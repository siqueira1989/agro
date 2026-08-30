package Util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
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
 * fechá-la. Consequência importante: todo o código existente que usa
 * {@code try (Connection c = new PostgresConnection().getConnection())} continua
 * idêntico e passa a devolver a conexão automaticamente — inclusive quando
 * ocorre exceção.</p>
 *
 * <h3>Contabilidade das vagas</h3>
 * <p>O número de conexões físicas é controlado por uma <b>reserva de vaga</b>
 * ({@link #reservarVaga()}), feita por <i>compare-and-set</i> antes de abrir a
 * conexão e sempre desfeita ({@link #liberarVaga()}) quando a abertura falha.
 * Sem esse cuidado, um banco temporariamente fora do ar deixava o contador
 * inflado: as conexões mortas eram descartadas mas as vagas não voltavam, e o
 * pool parava de criar conexões <b>para sempre</b>, mesmo depois de o banco
 * voltar — reproduzindo a indisponibilidade que este pool existe para evitar.</p>
 *
 * <p>A abertura da conexão acontece <b>fora</b> de qualquer bloco
 * {@code synchronized}: com o banco inacessível, o {@code connect} fica preso
 * até o timeout de TCP, e segurar o monitor durante esse tempo travaria as
 * demais threads, inclusive o encerramento do contexto.</p>
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
    /** Conexões físicas vivas — usado no descarte e no encerramento. */
    private final Set<Connection> todas = Collections.synchronizedSet(new HashSet<>());
    /** Vagas ocupadas: conexões existentes mais as que estão sendo abertas. */
    private final AtomicInteger criadas = new AtomicInteger(0);
    private final AtomicInteger emUso = new AtomicInteger(0);
    private volatile boolean encerrado = false;

    private ConnectionPool() {
        this.url      = ConfigUtil.obrigatorio("db.url");
        this.usuario  = ConfigUtil.obrigatorio("db.user");
        // A senha pode ser legitimamente vazia (autenticacao trust ou peer, ou
        // credencial em .pgpass). Exigi-la impediria essas configuracoes; o que
        // cabe e avisar, nao bloquear.
        this.senha    = ConfigUtil.get("db.password", "");
        if (this.senha.isEmpty()) {
            LogUtil.aviso(ConnectionPool.class,
                    "db.password vazio: o banco esta sendo acessado sem senha. "
                  + "Confirme se e mesmo autenticacao trust/peer.", null);
        }
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
     * @throws SQLException se o pool estiver esgotado além do tempo limite ou
     *                      se o banco estiver inacessível
     */
    public Connection emprestar() throws SQLException {
        if (encerrado) throw new SQLException("O pool de conexões já foi encerrado.");

        Connection fisica = disponiveis.poll();
        boolean vindaDaFila = fisica != null;

        if (fisica == null && reservarVaga()) {
            fisica = abrirComVagaReservada();   // devolve a vaga sozinha se falhar
        }

        if (fisica == null) {
            fisica = aguardarDaFila();
            vindaDaFila = true;
        }

        // Só faz sentido validar o que estava parado na fila: uma conexão
        // recém-aberta acabou de responder ao handshake. Validar sempre custaria
        // uma ida extra ao banco em cada chamada de DAO.
        if (vindaDaFila && !valida(fisica)) {
            fisica = substituir(fisica);
        }

        emUso.incrementAndGet();
        return envolver(fisica);
    }

    private Connection aguardarDaFila() throws SQLException {
        Connection c;
        try {
            c = disponiveis.poll(emprestimoTimeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("Interrompido enquanto aguardava uma conexão livre.", e);
        }
        if (c == null) {
            throw new SQLException(
                "Nenhuma conexão disponível após " + emprestimoTimeoutMs + " ms ("
              + estatisticas() + "). Provável conexão não devolvida por algum DAO.");
        }
        return c;
    }

    /** Reserva uma vaga antes de abrir. Devolve false se o pool está cheio. */
    private boolean reservarVaga() {
        while (true) {
            int atual = criadas.get();
            if (atual >= tamanhoMaximo) return false;
            if (criadas.compareAndSet(atual, atual + 1)) return true;
        }
    }

    private void liberarVaga() {
        criadas.updateAndGet(n -> n > 0 ? n - 1 : 0);
    }

    /**
     * Abre a conexão com a vaga já reservada. Em caso de falha devolve a vaga —
     * é isto que impede o pool de "morrer" depois de uma indisponibilidade do
     * banco.
     */
    private Connection abrirComVagaReservada() throws SQLException {
        try {
            Connection nova = abrirFisica();
            todas.add(nova);
            return nova;
        } catch (SQLException e) {
            liberarVaga();
            throw e;
        }
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

    /** Descarta a conexão morta (liberando a vaga) e abre outra no lugar. */
    private Connection substituir(Connection morta) throws SQLException {
        descartar(morta);
        LogUtil.aviso(ConnectionPool.class, "Conexão inválida descartada; abrindo outra.", null);

        if (reservarVaga()) {
            return abrirComVagaReservada();
        }
        // Corrida rara: outra thread tomou a vaga recém-liberada. Espera pela fila.
        return aguardarDaFila();
    }

    /** Fecha a conexão física, tira do inventário e devolve a vaga. */
    private void descartar(Connection c) {
        fecharSilencioso(c);
        if (todas.remove(c)) liberarVaga();
    }

    /**
     * Devolve a conexão física ao pool, restaurando o estado padrão. Um
     * {@code rollback} preventivo evita que uma transação deixada aberta por
     * um chamador contamine o próximo que pegar a mesma conexão.
     */
    private void devolver(Connection fisica) {
        emUso.updateAndGet(n -> n > 0 ? n - 1 : 0);

        if (encerrado) { descartar(fisica); return; }

        try {
            if (!fisica.getAutoCommit()) {
                fisica.rollback();
                fisica.setAutoCommit(true);
            }
            fisica.clearWarnings();
            if (!disponiveis.offer(fisica)) descartar(fisica);
        } catch (SQLException e) {
            LogUtil.aviso(ConnectionPool.class, "Conexão descartada ao ser devolvida.", e);
            descartar(fisica);
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
    public void encerrar() {
        encerrado = true;
        disponiveis.clear();
        // Cópia defensiva: descartar() mexe no próprio conjunto.
        for (Connection c : todas.toArray(new Connection[0])) {
            descartar(c);
        }
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

    /** Diagnóstico: "3 em uso, 7 livres, 10 abertas de 10". */
    public String estatisticas() {
        return emUso.get() + " em uso, " + disponiveis.size() + " livres, "
             + criadas.get() + " abertas de " + tamanhoMaximo;
    }
}
