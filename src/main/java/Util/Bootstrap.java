package Util;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

/**
 * Inicialização e encerramento da aplicação.
 *
 * <p>Concentra o que precisa acontecer uma única vez, na ordem certa, quando o
 * Tomcat publica o contexto:</p>
 *
 * <ol>
 *   <li>aquece o {@link ConnectionPool} e falha cedo se a configuração externa
 *       estiver faltando — melhor do que descobrir no primeiro clique;</li>
 *   <li>aplica as migrações de esquema pendentes ({@link SchemaMigrator});</li>
 *   <li>converte as senhas em texto puro que ainda restarem
 *       ({@link MigracaoSenhas}).</li>
 * </ol>
 *
 * <p>No {@code contextDestroyed}, fecha todas as conexões físicas — sem isso o
 * Tomcat acusa vazamento de recursos a cada <i>redeploy</i>.</p>
 *
 * <p>É um {@code ServletContextListener} da própria Jakarta EE, que já é a
 * plataforma do projeto; nenhum framework foi introduzido.</p>
 */
@WebListener
public class Bootstrap implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        LogUtil.info(Bootstrap.class, "Iniciando Agro Tech One…");

        try {
            LogUtil.info(Bootstrap.class,
                    "Configuração: " + ConfigUtil.origem()
                  + " | pool: " + ConnectionPool.getInstance().estatisticas());

            boolean aplicar = ConfigUtil.getBoolean("db.migrate.onStartup", true);
            new SchemaMigrator(sce.getServletContext()).migrar(aplicar);

            if (ConfigUtil.getBoolean("seguranca.migrarSenhasNoBoot", true)) {
                MigracaoSenhas.executar();
            }

            LogUtil.info(Bootstrap.class, "Agro Tech One pronto.");

        } catch (RuntimeException e) {
            // Subir com o banco desatualizado ou com senha em texto puro é pior
            // do que não subir: o erro apareceria depois, disfarçado.
            LogUtil.erro(Bootstrap.class, "Falha na inicialização da aplicação.", e);
            throw e;
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        try {
            ConnectionPool.getInstance().encerrar();
        } catch (RuntimeException e) {
            LogUtil.aviso(Bootstrap.class, "Falha ao encerrar o pool de conexões.", e);
        }
        LogUtil.info(Bootstrap.class, "Agro Tech One encerrado.");
    }
}
