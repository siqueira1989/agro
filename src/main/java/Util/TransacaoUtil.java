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
                // P2-20: antes so imprimia a pilha e seguia. Um rollback que falha
                // significa dados possivelmente inconsistentes: precisa ficar no log
                // com severidade e codigo de ocorrencia.
                Util.LogUtil.erro(TransacaoUtil.class,
                        "ROLLBACK FALHOU — verifique a consistencia dos dados.", ex);
            }
            throw e;
        } finally {
            try {
                conn.setAutoCommit(autoCommitOriginal);
            } catch (SQLException ex) {
                Util.LogUtil.aviso(TransacaoUtil.class,
                        "Falha ao restaurar o autoCommit da conexao.", ex);
            }
        }
    }
}
