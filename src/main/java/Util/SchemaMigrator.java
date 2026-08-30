package Util;

import jakarta.servlet.ServletContext;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Aplicador de migrações escrito em JDBC puro (P2-18 da auditoria de 29/08/2026).
 *
 * <p><b>O problema.</b> A pasta {@code src/main/resources/db/migration} seguia a
 * convenção do Flyway (V2…V19) mas o Flyway não estava no {@code pom.xml} e o
 * {@code V1__} nem existia. Os scripts eram aplicados à mão, sem registro de
 * quais já haviam rodado — origem clássica do "funciona na minha máquina".</p>
 *
 * <p><b>A solução, sem framework.</b> Esta classe faz o mínimo que o Flyway faz
 * e que o projeto precisa: mantém a tabela {@code schema_version}, descobre os
 * arquivos {@code V<n>__*.sql} pelo {@link ServletContext}, ordena por número e
 * aplica os pendentes, cada um em sua própria transação. Um script que falha
 * é revertido e interrompe o processo, deixando os seguintes pendentes.</p>
 *
 * <p>O arquivo inteiro é enviado ao PostgreSQL numa única chamada, sem quebrar
 * por ponto e vírgula: é o que permite usar blocos {@code DO $$ ... $$} e
 * funções PL/pgSQL sem que um separador ingênuo corte o script no meio.</p>
 */
public final class SchemaMigrator {

    private static final String PASTA = "/WEB-INF/classes/db/migration/";
    private static final Pattern NOME = Pattern.compile("^V(\\d+)__(.+)\\.sql$");

    /**
     * Última versão que já havia sido aplicada à mão antes deste controlador
     * existir. Migrações novas começam em V20.
     */
    private static final int BASELINE_ATE = 19;

    /** Chave do pg_advisory_lock — qualquer bigint estável serve. */
    private static final long CHAVE_LOCK = 8_270_2026L;

    private final ServletContext ctx;

    public SchemaMigrator(ServletContext ctx) {
        this.ctx = ctx;
    }

    /** Aplica as migrações pendentes. Devolve quantas foram aplicadas. */
    public int migrar(boolean aplicar) {
        try (Connection c = new PostgresConnection().getConnection()) {
            // Lock consultivo: dois nós ou dois deploys simultâneos aplicariam a
            // mesma migração em paralelo — e a V21 tem um UPDATE de backfill que
            // não é idempotente sob concorrência. O segundo espera aqui e, ao
            // entrar, já encontra tudo registrado em schema_version.
            try (Statement st = c.createStatement()) {
                st.execute("SELECT pg_advisory_lock(" + CHAVE_LOCK + ")");
            }
            criarTabelaControle(c);
            baselineSeNecessario(c);
            Set<Integer> aplicadas = versoesAplicadas(c);
            List<Script> pendentes = new ArrayList<>();

            for (Script s : listarScripts()) {
                if (!aplicadas.contains(s.versao)) pendentes.add(s);
            }
            pendentes.sort(Comparator.comparingInt(s -> s.versao));

            if (pendentes.isEmpty()) {
                LogUtil.info(SchemaMigrator.class, "Esquema do banco em dia; nada a aplicar.");
                liberarLock(c);
                return 0;
            }

            if (!aplicar) {
                StringBuilder sb = new StringBuilder();
                for (Script s : pendentes) sb.append(s.nome).append(' ');
                LogUtil.aviso(SchemaMigrator.class,
                        "Migrações PENDENTES (db.migrate.onStartup=false): " + sb.toString().trim(), null);
                liberarLock(c);
                return 0;
            }

            int total = 0;
            for (Script s : pendentes) {
                aplicarScript(c, s);
                total++;
            }
            LogUtil.info(SchemaMigrator.class, total + " migração(ões) aplicada(s) com sucesso.");
            liberarLock(c);
            return total;

        } catch (SQLException e) {
            String cod = LogUtil.erro(SchemaMigrator.class,
                    "Falha ao aplicar as migrações de esquema.", e);
            throw new IllegalStateException(
                    "Banco de dados não pôde ser migrado (código " + cod + "). "
                  + "A aplicação não deve subir com o esquema desatualizado.", e);
        }
    }

    /** Libera o lock consultivo; a conexão volta ao pool logo em seguida. */
    private void liberarLock(Connection c) {
        try (Statement st = c.createStatement()) {
            st.execute("SELECT pg_advisory_unlock(" + CHAVE_LOCK + ")");
        } catch (SQLException e) {
            LogUtil.aviso(SchemaMigrator.class, "Falha ao liberar o lock de migração.", e);
        }
    }

    private void criarTabelaControle(Connection c) throws SQLException {
        try (Statement st = c.createStatement()) {
            st.execute(
                "CREATE TABLE IF NOT EXISTS public.schema_version ("
              + "  versao      integer PRIMARY KEY,"
              + "  nome        varchar(200) NOT NULL,"
              + "  aplicada_em timestamp NOT NULL DEFAULT now(),"
              + "  duracao_ms  integer NOT NULL,"
              + "  sucesso     boolean NOT NULL DEFAULT true)");
        }
    }

    /**
     * Marca como aplicadas as migrações que já rodaram à mão antes de existir
     * controle de versão.
     *
     * <p>V2 a V19 foram executadas manualmente ao longo do desenvolvimento e o
     * V1 nunca existiu. Sem esta linha de corte, o primeiro boot tentaria
     * reaplicar tudo — no melhor caso falhando, no pior duplicando dados. A
     * presença da tabela {@code pessoa} é o sinal de que o banco já está
     * formado; num banco vazio o baseline não acontece e os scripts rodam
     * normalmente desde o começo.</p>
     */
    private void baselineSeNecessario(Connection c) throws SQLException {
        int jaRegistradas;
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT count(*) FROM public.schema_version")) {
            rs.next();
            jaRegistradas = rs.getInt(1);
        }
        if (jaRegistradas > 0) return;

        boolean bancoJaFormado;
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(
                 "SELECT to_regclass('public.pessoa') IS NOT NULL")) {
            rs.next();
            bancoJaFormado = rs.getBoolean(1);
        }
        if (!bancoJaFormado) {
            LogUtil.info(SchemaMigrator.class, "Banco vazio: todas as migrações serão aplicadas.");
            return;
        }

        try (PreparedStatement st = c.prepareStatement(
                "INSERT INTO public.schema_version (versao, nome, duracao_ms, sucesso) "
              + "VALUES (?,?,0,true) ON CONFLICT (versao) DO NOTHING")) {
            for (int v = 1; v <= BASELINE_ATE; v++) {
                st.setInt(1, v);
                st.setString(2, "baseline (aplicada manualmente antes do controle de versão)");
                st.addBatch();
            }
            st.executeBatch();
        }
        LogUtil.info(SchemaMigrator.class,
                "Baseline registrado: V1 a V" + BASELINE_ATE + " marcadas como já aplicadas.");
    }

    private Set<Integer> versoesAplicadas(Connection c) throws SQLException {
        Set<Integer> v = new HashSet<>();
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(
                 "SELECT versao FROM public.schema_version WHERE sucesso = true")) {
            while (rs.next()) v.add(rs.getInt(1));
        }
        return v;
    }

    private List<Script> listarScripts() {
        List<Script> lista = new ArrayList<>();
        Set<String> caminhos = ctx.getResourcePaths(PASTA);
        if (caminhos == null) {
            LogUtil.aviso(SchemaMigrator.class,
                    "Pasta de migrações não encontrada em " + PASTA + ".", null);
            return lista;
        }
        for (String caminho : caminhos) {
            String arquivo = caminho.substring(caminho.lastIndexOf('/') + 1);
            Matcher m = NOME.matcher(arquivo);
            if (m.matches()) {
                lista.add(new Script(Integer.parseInt(m.group(1)), arquivo, caminho));
            }
        }
        return lista;
    }

    private void aplicarScript(Connection c, Script s) throws SQLException {
        String sql = ler(s.caminho);
        if (sql == null || sql.isBlank()) {
            // Registra assim mesmo: um script vazio que não é registrado volta a
            // ser lido e "ignorado" em todo boot, para sempre.
            LogUtil.aviso(SchemaMigrator.class, "Script vazio: " + s.nome + " (registrado como aplicado).", null);
            try (PreparedStatement st = c.prepareStatement(
                    "INSERT INTO public.schema_version (versao, nome, duracao_ms, sucesso) "
                  + "VALUES (?,?,0,true) ON CONFLICT (versao) DO NOTHING")) {
                st.setInt(1, s.versao); st.setString(2, s.nome); st.executeUpdate();
            }
            return;
        }

        LogUtil.info(SchemaMigrator.class, "Aplicando " + s.nome + "…");
        long inicio = System.currentTimeMillis();
        boolean autoCommitOriginal = c.getAutoCommit();
        c.setAutoCommit(false);
        try {
            // O arquivo inteiro vai numa única chamada: é o que permite blocos
            // DO $$ ... $$ e funções PL/pgSQL sem quebrar por ponto e vírgula.
            try (Statement st = c.createStatement()) {
                st.execute(sql);
            }
            try (PreparedStatement st = c.prepareStatement(
                    "INSERT INTO public.schema_version (versao, nome, duracao_ms, sucesso) "
                  + "VALUES (?,?,?,true)")) {
                st.setInt(1, s.versao);
                st.setString(2, s.nome);
                st.setInt(3, (int) (System.currentTimeMillis() - inicio));
                st.executeUpdate();
            }
            c.commit();
            LogUtil.info(SchemaMigrator.class,
                    s.nome + " aplicada em " + (System.currentTimeMillis() - inicio) + " ms.");
        } catch (SQLException e) {
            c.rollback();
            LogUtil.erro(SchemaMigrator.class, "Migração " + s.nome + " falhou e foi revertida.", e);
            throw e;
        } finally {
            c.setAutoCommit(autoCommitOriginal);
        }
    }

    private String ler(String caminho) {
        try (InputStream in = ctx.getResourceAsStream(caminho)) {
            if (in == null) return null;
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
            return bos.toString(StandardCharsets.UTF_8);
        } catch (Exception e) {
            LogUtil.erro(SchemaMigrator.class, "Falha ao ler " + caminho, e);
            return null;
        }
    }

    private static final class Script {
        final int versao;
        final String nome;
        final String caminho;
        Script(int versao, String nome, String caminho) {
            this.versao = versao; this.nome = nome; this.caminho = caminho;
        }
    }
}
