package Util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Pool de conexões escrito só com o JDK (P0-4 da auditoria de 29/08/2026).
 *
 * <p><b>Por que existir.</b> Antes, cada chamada de DAO fazia
 * {@code DriverManager.getConnection(...)}, que abre um processo real no
 * PostgreSQL. Como 20 métodos nunca fechavam a conexão e outros 12 só fechavam
 * no caminho feliz, o servidor esgotava o {@code max_connections} (padrão 100)
 * e a aplicação parava com {@code FATAL: sorry, too many clients already} até
 * o Tomcat ser reiniciado.</p>
 *
 * <p><b>Como resolve.</b> As conexões físicas são criadas uma única vez e
 * reaproveitadas. Cada chamador recebe um <i>proxy dinâmico</i>
 * ({@link java.lang.reflect.Proxy}) que se comporta como uma {@link Connection}
 * normal, mas cujo {@code close()} <b>devolve a conexão ao pool</b> em vez de
 * fechá-la. Isso tem uma consequência importante: todo o código existente que
 * usa {@code try (Connection c = new PostgresConnection().getConnection())}
 * continua idêntico e passa a devolver a conexão automaticamente — inclusive
 * quando ocorre exceção.</p>
 *
 * <p>Se um chamador esquecer o {@code close()}, a conexão fica retida como
 * antes; a diferença é que agora o pool tem um teto conhecido e o
 * {@link #emprestimoTimeoutMs} faz a falha aparecer como erro tratável em vez
 * de derrubar o banco.</p>
 *
 * <p>Nenhuma dependência foi adicionada ao {@code pom.xml}: só
 * {@code java.sql}, {@code java.lang.reflect} e {@code java.util.concurrent}.</p>
 */
public final class ConnectionPool {

    private static volatile ConnectionPool instancia;

    private final String url;
    private final String usuario;
    private final String senha;
    private final int tamanhoMaximo;
    private final long emprestimoTimeoutMs;
    private final int validacaoTimeoutS;

    private final BlockingQueue<Connection> disponiveis;
    private final List<Connection> todas = new ArrayList<>();
    private final AtomicInteger criadas = new AtomicInteger(0);
    private final AtomicInteger emUso = new AtomicInteger(0);
    private volatile boolean encerrado = false;

    private ConnectionPool() {
        this.url      = ConfigUtil.obrigatorio("db.url");
        this.usuario  = ConfigUtil.obrigatorio("db.user");
        this.senha    = ConfigUtil.obrigatorio("db.password");
        this.tamanhoMaximo       = Math.max(1, ConfigUtil.getInt("db.pool.size", 10));
        this.emprestimoTimeoutMs = ConfigUtil.getInt("db.pool.timeoutMs", 10000);
        this.validacaoTimeoutS   = ConfigUtil.getInt("db.pool.validationTimeoutSeconds", 2);
        this.disponiveis = new ArrayBlockingQueue<>(tamanhoMaximo);

        try {
            Class.forName(ConfigUtil.get("db.driver", "org.postgresql.Driver"));
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Driver JDBC não encontrado no classpath.", e);
        }

        LogUtil.info(ConnectionPool.class,
                "Pool inicializado: " + tamanhoMaximo + " conexões no máximo, "
              + "configuração lida de " + ConfigUtil.origem() + ".");
    }

    public static ConnectionPool getInstance() {
        ConnectionPool local = instancia;
        if (local == null) {
            synchronized (ConnectionPool.class) {
                local = instancia;
                if (local == null) {
                    local = new ConnectionPool();
                    instancia = local;
                }
            }
        }
        return local;
    }

    /**
     * Empresta uma conexão. O objeto devolvido é um proxy: chamar
     * {@code close()} nele devolve a conexão ao pool.
     *
     * @throws SQLException se o pool estiver esgotado além do tempo limite
     */
    public Connection emprestar() throws SQLException {
        if (encerrado) throw new SQLException("O pool de conexões já foi encerrado.");

        Connection fisica = disponiveis.poll();

        if (fisica == null && criadas.get() < tamanhoMaximo) {
            synchronized (this) {
                if (criadas.get() < tamanhoMaximo) {
                    fisica = abrirFisica();
                    criadas.incrementAndGet();
                    todas.add(fisica);
                }
            }
        }

        if (fisica == null) {
            try {
                fisica = disponiveis.poll(emprestimoTimeoutMs, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new SQLException("Interrompido enquanto aguardava uma conexão livre.", e);
            }
        }

        if (fisica == null) {
            throw new SQLException(
                "Nenhuma conexão disponível após " + emprestimoTimeoutMs + " ms "
              + "(" + emUso.get() + " de " + tamanhoMaximo + " em uso). "
              + "Provável conexão não devolvida por algum DAO.");
        }

        if (!valida(fisica)) {
            fisica = substituir(fisica);
        }

        emUso.incrementAndGet();
        return envolver(fisica);
    }

    private Connection abrirFisica() throws SQLException {
        Connection c = DriverManager.getConnection(url, usuario, senha);
        c.setAutoCommit(true);
        return c;
    }

    private boolean valida(Connection c) {
        try {
            return !c.isClosed() && c.isValid(validacaoTimeoutS);
        } catch (SQLException e) {
            return false;
        }
    }

    private synchronized Connection substituir(Connection morta) throws SQLException {
        fecharSilencioso(morta);
        todas.remove(morta);
        Connection nova = abrirFisica();
        todas.add(nova);
        LogUtil.aviso(ConnectionPool.class, "Conexão inválida descartada e recriada.", null);
        return nova;
    }

    /**
     * Devolve a conexão física ao pool, restaurando o estado padrão. Um
     * {@code rollback} preventivo evita que uma transação deixada aberta por
     * um chamador contamine o próximo que pegar a mesma conexão.
     */
    private void devolver(Connection fisica) {
        emUso.decrementAndGet();
        if (encerrado) { fecharSilencioso(fisica); return; }
        try {
            if (!fisica.getAutoCommit()) {
                fisica.rollback();
                fisica.setAutoCommit(true);
            }
            fisica.clearWarnings();
            if (!disponiveis.offer(fisica)) fecharSilencioso(fisica);
        } catch (SQLException e) {
            LogUtil.aviso(ConnectionPool.class, "Conexão descartada ao ser devolvida.", e);
            fecharSilencioso(fisica);
            synchronized (this) {
                todas.remove(fisica);
                criadas.decrementAndGet();
            }
        }
    }

    /** Proxy que intercepta {@code close}, {@code isClosed} e {@code unwrap}. */
    private Connection envolver(final Connection fisica) {
        return (Connection) Proxy.newProxyInstance(
            ConnectionPool.class.getClassLoader(),
            new Class<?>[]{ Connection.class },
            new InvocationHandler() {
                private boolean devolvida = false;

                @Override
                public Object invoke(Object proxy, Method metodo, Object[] args) throws Throwable {
                    String nome = metodo.getName();

                    if ("close".equals(nome)) {
                        if (!devolvida) { devolvida = true; devolver(fisica); }
                        return null;
                    }
                    if ("isClosed".equals(nome)) {
                        return devolvida || fisica.isClosed();
                    }
                    if ("unwrap".equals(nome)) {
                        return fisica;
                    }
                    if (devolvida) {
                        throw new SQLException("Conexão já devolvida ao pool: " + nome + "() não é permitido.");
                    }
                    try {
                        return metodo.invoke(fisica, args);
                    } catch (InvocationTargetException e) {
                        throw e.getCause();
                    }
                }
            });
    }

    /** Fecha todas as conexões físicas. Chamado pelo listener de shutdown. */
    public synchronized void encerrar() {
        encerrado = true;
        for (Connection c : todas) fecharSilencioso(c);
        todas.clear();
        disponiveis.clear();
        criadas.set(0);
        emUso.set(0);
        LogUtil.info(ConnectionPool.class, "Pool encerrado; todas as conexões foram fechadas.");
    }

    private static void fecharSilencioso(Connection c) {
        if (c == null) return;
        try {
            c.close();
        } catch (SQLException e) {
            LogUtil.aviso(ConnectionPool.class, "Falha ao fechar conexão física.", e);
        }
    }

    /** Diagnóstico: "3 em uso de 10 (7 livres)". */
    public String estatisticas() {
        return emUso.get() + " em uso de " + tamanhoMaximo + " (" + disponiveis.size() + " livres)";
    }
}
