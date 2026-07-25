package Model.Dao;

import Model.Model.Quadra;
import Util.PostgresConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Quadras de produção. O insert transacional (junto da área) recebe a conexão
 * de fora; as demais operações abrem a própria conexão.
 */
public class QuadraDAO {

    /** Insere uma quadra usando uma conexão já aberta (transação com a área). */
    public void inserir(Connection conn, Quadra q) throws SQLException {
        String sql = "INSERT INTO quadra (idareaproducao, nome_quadra, numero_plantas, id_alimento, ativa) "
                   + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement st = conn.prepareStatement(sql)) {
            st.setInt(1, q.getIdAreaProducao());
            st.setString(2, q.getNomeQuadra());
            st.setInt(3, q.getNumeroPlantas());
            if (q.getIdAlimento() != null) st.setInt(4, q.getIdAlimento()); else st.setNull(4, Types.INTEGER);
            st.setBoolean(5, q.isAtiva());
            st.executeUpdate();
        }
    }

    /** Insere uma quadra numa área já existente (conexão própria). */
    public void inserir(Quadra q) throws SQLException {
        try (Connection conn = new PostgresConnection().getConnection()) {
            inserir(conn, q);
        }
    }

    /** Lista as quadras de uma área (todas ou só ativas), com o nome do alimento. */
    public List<Quadra> listarPorArea(int idArea, boolean somenteAtivas) throws SQLException {
        String sql = "SELECT q.*, a.nomeproduto AS alimento_nome "
                   + "FROM quadra q LEFT JOIN alimento a ON a.idproduto = q.id_alimento "
                   + "WHERE q.idareaproducao = ?" + (somenteAtivas ? " AND q.ativa = true" : "")
                   + " ORDER BY q.idquadra";
        List<Quadra> lista = new ArrayList<>();
        try (Connection conn = new PostgresConnection().getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setInt(1, idArea);
            ResultSet rs = st.executeQuery();
            while (rs.next()) {
                Quadra q = new Quadra();
                q.setIdQuadra(rs.getInt("idquadra"));
                q.setIdAreaProducao(rs.getInt("idareaproducao"));
                q.setNomeQuadra(rs.getString("nome_quadra"));
                q.setNumeroPlantas(rs.getInt("numero_plantas"));
                q.setIdAlimento((Integer) rs.getObject("id_alimento"));
                q.setAlimentoNome(rs.getString("alimento_nome"));
                q.setAtiva(rs.getBoolean("ativa"));
                lista.add(q);
            }
        }
        return lista;
    }

    /** Ativa/desativa uma quadra (soft delete — a linha e seus dados são preservados). */
    public void definirAtiva(int idQuadra, boolean ativa) throws SQLException {
        try (Connection conn = new PostgresConnection().getConnection();
             PreparedStatement st = conn.prepareStatement("UPDATE quadra SET ativa=? WHERE idquadra=?")) {
            st.setBoolean(1, ativa);
            st.setInt(2, idQuadra);
            st.executeUpdate();
        }
    }

    /** Retorna a área dona de uma quadra (para recomputar a soma depois). */
    public Integer areaDaQuadra(int idQuadra) throws SQLException {
        try (Connection conn = new PostgresConnection().getConnection();
             PreparedStatement st = conn.prepareStatement("SELECT idareaproducao FROM quadra WHERE idquadra=?")) {
            st.setInt(1, idQuadra);
            ResultSet rs = st.executeQuery();
            if (rs.next()) return rs.getInt(1);
        }
        return null;
    }
}
