package Util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Converte, uma única vez, as senhas em texto puro para PBKDF2
 * (P0-2 da auditoria de 29/08/2026).
 *
 * <p><b>Por que em Java e não em SQL.</b> O PostgreSQL não calcula PBKDF2 sem a
 * extensão {@code pgcrypto}, que nem sempre está instalada. A conversão roda no
 * boot da aplicação, logo depois das migrações de esquema, usando o mesmo
 * {@link SenhaUtil} que o login usa.</p>
 *
 * <p><b>Efeito para o usuário: nenhum.</b> Ninguém precisa redefinir senha —
 * cada senha atual é lida, transformada em hash e regravada. No login seguinte
 * a comparação passa a ser feita pelo hash. Depois desta rotina, não existe
 * mais senha legível no banco.</p>
 *
 * <p>A operação é idempotente: linhas que já estão em PBKDF2 são ignoradas, de
 * modo que rodar de novo não custa nada nem estraga nada.</p>
 */
public final class MigracaoSenhas {

    private MigracaoSenhas() { }

    /** Converte as senhas pendentes. Devolve quantas foram convertidas. */
    public static int executar() {
        try (Connection c = new PostgresConnection().getConnection()) {

            if (!colunaComporta(c)) {
                LogUtil.aviso(MigracaoSenhas.class,
                        "A coluna pessoa.senhapessoa ainda não comporta o hash. "
                      + "Aplique a migração V20 antes de converter as senhas.", null);
                return 0;
            }

            // SELECT na tabela-mãe enxerga as filhas (herança), então esta única
            // consulta cobre pessoa, funcionário, parceiro e todas as demais.
            Map<Integer, String> pendentes = new LinkedHashMap<>();
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery(
                     "SELECT idpessoa, senhapessoa FROM public.pessoa "
                   + "WHERE senhapessoa IS NOT NULL AND senhapessoa <> '' "
                   + "AND senhapessoa NOT LIKE 'pbkdf2$%'")) {
                while (rs.next()) pendentes.put(rs.getInt(1), rs.getString(2));
            }

            if (pendentes.isEmpty()) {
                LogUtil.info(MigracaoSenhas.class, "Nenhuma senha em texto puro encontrada.");
                return 0;
            }

            LogUtil.info(MigracaoSenhas.class,
                    pendentes.size() + " senha(s) em texto puro; convertendo para PBKDF2…");

            boolean autoCommitOriginal = c.getAutoCommit();
            c.setAutoCommit(false);
            int convertidas = 0;
            try (PreparedStatement st = c.prepareStatement(
                    "UPDATE public.pessoa SET senhapessoa = ? WHERE idpessoa = ?")) {
                for (Map.Entry<Integer, String> e : pendentes.entrySet()) {
                    st.setString(1, SenhaUtil.gerarHash(e.getValue()));
                    st.setInt(2, e.getKey());
                    st.addBatch();
                    convertidas++;
                }
                st.executeBatch();
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(autoCommitOriginal);
            }

            LogUtil.info(MigracaoSenhas.class,
                    convertidas + " senha(s) convertida(s). Não há mais senha legível no banco.");
            return convertidas;

        } catch (SQLException e) {
            String cod = LogUtil.erro(MigracaoSenhas.class,
                    "Falha ao converter as senhas para hash.", e);
            throw new IllegalStateException(
                    "Migração de senhas falhou (código " + cod + ").", e);
        }
    }

    /** Confere se a coluna já foi ampliada pela V20 (era varchar(50)). */
    private static boolean colunaComporta(Connection c) throws SQLException {
        try (PreparedStatement st = c.prepareStatement(
                "SELECT character_maximum_length FROM information_schema.columns "
              + "WHERE table_schema='public' AND table_name='pessoa' AND column_name='senhapessoa'");
             ResultSet rs = st.executeQuery()) {
            if (!rs.next()) return false;
            int tamanho = rs.getInt(1);
            return rs.wasNull() || tamanho >= 200;
        }
    }
}
