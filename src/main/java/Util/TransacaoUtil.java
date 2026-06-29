package Util;

import java.sql.Connection;
import java.sql.SQLException;

public class TransacaoUtil {

    @FunctionalInterface
    public interface TransacaoBlock {
        void executar(Connection conn) throws SQLException;
    }

    public static void executar(Connection conn, TransacaoBlock bloco) throws SQLException {
        boolean autoCommitOriginal = conn.getAutoCommit();
        try {
            conn.setAutoCommit(false);
            bloco.executar(conn);
            conn.commit();
        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            throw e;
        } finally {
            try {
                conn.setAutoCommit(autoCommitOriginal);
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }
}
