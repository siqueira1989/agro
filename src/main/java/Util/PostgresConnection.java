package Util;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Ponto único de obtenção de conexões com o PostgreSQL.
 *
 * <p><b>O que mudou (P0-3 e P0-4 da auditoria de 29/08/2026).</b></p>
 * <ul>
 *   <li>A URL, o usuário e a senha <b>saíram do código-fonte</b>. Antes estavam
 *       escritos aqui e versionados no Git; agora vêm de {@link ConfigUtil}
 *       (arquivo externo ou propriedade de sistema).</li>
 *   <li>{@link #getConnection()} não abre mais uma conexão nova a cada chamada:
 *       pega uma emprestada do {@link ConnectionPool}. Como o objeto devolvido é
 *       um proxy cujo {@code close()} devolve a conexão ao pool, <b>todo o
 *       código de DAO existente continua válido sem alteração</b> — e passa a
 *       liberar o recurso corretamente quando envolvido em
 *       {@code try-with-resources}.</li>
 * </ul>
 *
 * <p>A classe permanece instanciável com {@code new PostgresConnection()} para
 * não quebrar os 28 DAOs que já a usam dessa forma.</p>
 */
public class PostgresConnection {

    /**
     * Empresta uma conexão do pool.
     *
     * <p>Use sempre dentro de {@code try-with-resources}:</p>
     * <pre>{@code
     * try (Connection c = new PostgresConnection().getConnection();
     *      PreparedStatement st = c.prepareStatement(SQL)) {
     *     ...
     * }
     * }</pre>
     *
     * @throws SQLException se o pool estiver esgotado ou o banco inacessível
     */
    public Connection getConnection() throws SQLException {
        return ConnectionPool.getInstance().emprestar();
    }

    /**
     * Devolve a conexão ao pool.
     *
     * @deprecated Prefira {@code try-with-resources}, que já faz isso mesmo
     *             quando ocorre exceção. Mantido apenas para compatibilidade.
     */
    @Deprecated
    public static void closeConnection(Connection connection) {
        if (connection == null) return;
        try {
            connection.close();
        } catch (SQLException e) {
            LogUtil.aviso(PostgresConnection.class, "Falha ao devolver a conexão ao pool.", e);
        }
    }
}
