package Model.Dao;

import Model.Model.Alimento;
import Util.PostgresConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Persistência de Alimento (cultura).
 *
 * <h3>Correções da auditoria de 29/08/2026</h3>
 *
 * <p><b>P0-4 — vazamento de conexão.</b> Os cinco métodos abriam a conexão fora
 * de {@code try-with-resources}. Três nunca a fechavam e dois fechavam dentro
 * do {@code try}, antes do {@code catch} — ou seja, qualquer exceção deixava a
 * conexão presa. Agora todos usam {@code try-with-resources}, que devolve a
 * conexão ao pool inclusive quando há erro.</p>
 *
 * <p><b>P0-5 — gravação silenciosa.</b> {@code listAll}, {@code AlimentoBuscaID}
 * e {@code VerificarDadosAlimento} capturavam a exceção, imprimiam no console e
 * retornavam normalmente. O servlet, sem exceção para tratar, respondia
 * "sucesso" ou uma lista vazia — o usuário via a confirmação e o registro não
 * existia. Pior: {@code VerificarDadosAlimento} devolvia {@code false} no erro,
 * fazendo o sistema concluir que o cadastro não era duplicado e seguir para o
 * INSERT. Nenhum método engole mais exceção; todos propagam
 * {@link SQLException} para o servlet decidir.</p>
 */
public class AlimentoDAO {

    private static final String COLUNAS =
        "idproduto, nomeproduto, situacaoproduto, tipoproduto, variedadealimento";

    public List<Alimento> listAll() throws SQLException {
        String sql = "SELECT " + COLUNAS + " FROM alimento ORDER BY nomeproduto";
        List<Alimento> alimentos = new ArrayList<>();

        try (Connection conexao = new PostgresConnection().getConnection();
             PreparedStatement stmt = conexao.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) alimentos.add(mapear(rs));
        }
        return alimentos;
    }

    public Alimento AlimentoBuscaID(int idalimento) throws SQLException {
        String sql = "SELECT " + COLUNAS + " FROM alimento WHERE idproduto = ?";

        try (Connection conexao = new PostgresConnection().getConnection();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {

            stmt.setInt(1, idalimento);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public void addAlimento(Alimento alimento) throws SQLException {
        String sql = "INSERT INTO alimento (nomeproduto, tipoproduto, situacaoproduto, "
                   + "variedadealimento) VALUES (?, ?, ?, ?)";

        try (Connection conexao = new PostgresConnection().getConnection();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {

            stmt.setString(1, alimento.getNomeproduto());
            stmt.setString(2, alimento.getTipoproduto());
            stmt.setBoolean(3, alimento.isSituacaoproduto());
            stmt.setString(4, alimento.getVariedadealimento());
            stmt.executeUpdate();
        }
    }

    public void updateAlimento(Alimento alimento) throws SQLException {
        String sql = "UPDATE alimento SET nomeproduto = ?, variedadealimento = ? WHERE idproduto = ?";

        try (Connection conexao = new PostgresConnection().getConnection();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {

            stmt.setString(1, alimento.getNomeproduto());
            stmt.setString(2, alimento.getVariedadealimento());
            stmt.setInt(3, alimento.getIdproduto());
            stmt.executeUpdate();
        }
    }

    /**
     * Indica se já existe um alimento com o mesmo nome, tipo e variedade.
     *
     * <p>Antes devolvia {@code false} quando o banco falhava — resposta perigosa,
     * porque "não sei" virava "não é duplicado" e o INSERT seguia adiante. Agora
     * a falha sobe como {@link SQLException} e o cadastro é interrompido.</p>
     */
    public boolean VerificarDadosAlimento(Alimento alimento) throws SQLException {
        String sql = "SELECT 1 FROM alimento WHERE nomeproduto = ? AND tipoproduto = ? "
                   + "AND variedadealimento = ? LIMIT 1";

        try (Connection conexao = new PostgresConnection().getConnection();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {

            stmt.setString(1, alimento.getNomeproduto());
            stmt.setString(2, alimento.getTipoproduto());
            stmt.setString(3, alimento.getVariedadealimento());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private Alimento mapear(ResultSet rs) throws SQLException {
        Alimento alimento = new Alimento();
        alimento.setIdproduto(rs.getInt("idproduto"));
        alimento.setNomeproduto(rs.getString("nomeproduto"));
        alimento.setSituacaoproduto(rs.getBoolean("situacaoproduto"));
        alimento.setTipoproduto(rs.getString("tipoproduto"));
        alimento.setVariedadealimento(rs.getString("variedadealimento"));
        return alimento;
    }
}
