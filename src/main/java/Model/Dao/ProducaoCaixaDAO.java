package Model.Dao;

import Model.Model.Vinculo;
import Util.PostgresConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Lançamentos de caixas produzidas (por dia e tipo). */
public class ProducaoCaixaDAO {

    private final VinculoDAO vinculoDAO = new VinculoDAO();

    private Integer resolverVinculo(int idpessoa, LocalDate data) throws SQLException {
        Vinculo v = vinculoDAO.buscarPorData(idpessoa, data);
        return v != null ? v.getIdVinculo() : null;
    }

    /** Insere um lançamento com o preço unitário (snapshot) já resolvido. */
    public void inserir(int idpessoa, LocalDate data, String tipoCaixa, int quantidade,
                        BigDecimal precoUnitario) throws SQLException {
        Integer idVinc = resolverVinculo(idpessoa, data);
        String sql = "INSERT INTO producao_caixa "
                + "(idpessoa, data_producao, tipo_caixa, quantidade, preco_unitario, id_vinculo) "
                + "VALUES (?,?,?,?,?,?)";
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idpessoa);
            ps.setDate(2, Date.valueOf(data));
            ps.setString(3, tipoCaixa);
            ps.setInt(4, quantidade);
            ps.setBigDecimal(5, precoUnitario);
            if (idVinc != null) ps.setInt(6, idVinc); else ps.setNull(6, Types.INTEGER);
            ps.executeUpdate();
        }
    }

    public void atualizar(int idProducao, int idpessoa, LocalDate data, String tipoCaixa,
                          int quantidade, BigDecimal precoUnitario) throws SQLException {
        Integer idVinc = resolverVinculo(idpessoa, data);
        String sql = "UPDATE producao_caixa SET data_producao=?, tipo_caixa=?, quantidade=?, "
                + "preco_unitario=?, id_vinculo=? WHERE idproducao=?";
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(data));
            ps.setString(2, tipoCaixa);
            ps.setInt(3, quantidade);
            ps.setBigDecimal(4, precoUnitario);
            if (idVinc != null) ps.setInt(5, idVinc); else ps.setNull(5, Types.INTEGER);
            ps.setInt(6, idProducao);
            ps.executeUpdate();
        }
    }

    public void excluir(int idProducao) throws SQLException {
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM producao_caixa WHERE idproducao=?")) {
            ps.setInt(1, idProducao);
            ps.executeUpdate();
        }
    }

    /** Lançamentos do mês (cada um com subtotal quantidade × preço). */
    public List<Map<String, Object>> listarPorMes(int idpessoa, String periodo) throws SQLException {
        String sql = "SELECT idproducao, data_producao, tipo_caixa, quantidade, preco_unitario "
                + "FROM producao_caixa WHERE idpessoa=? AND TO_CHAR(data_producao,'YYYY-MM')=? "
                + "ORDER BY data_producao, tipo_caixa";
        List<Map<String, Object>> lista = new ArrayList<>();
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idpessoa);
            ps.setString(2, periodo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    int qtd = rs.getInt("quantidade");
                    BigDecimal preco = rs.getBigDecimal("preco_unitario");
                    m.put("idProducao", rs.getInt("idproducao"));
                    m.put("dataProducao", rs.getDate("data_producao").toLocalDate().toString());
                    m.put("tipoCaixa", rs.getString("tipo_caixa"));
                    m.put("quantidade", qtd);
                    m.put("precoUnitario", preco);
                    m.put("subtotal", preco.multiply(BigDecimal.valueOf(qtd)));
                    lista.add(m);
                }
            }
        }
        return lista;
    }

    /** Resumo do mês: total de caixas e valor total (Σ quantidade × preço). */
    public int[] totalCaixas(int idpessoa, String periodo) throws SQLException {
        // retorna [totalQuantidade] via inteiro; valor vem em somarValor
        String sql = "SELECT COALESCE(SUM(quantidade),0) FROM producao_caixa "
                + "WHERE idpessoa=? AND TO_CHAR(data_producao,'YYYY-MM')=?";
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idpessoa);
            ps.setString(2, periodo);
            try (ResultSet rs = ps.executeQuery()) {
                return new int[]{ rs.next() ? rs.getInt(1) : 0 };
            }
        }
    }

    public BigDecimal somarValor(int idpessoa, String periodo) throws SQLException {
        String sql = "SELECT COALESCE(SUM(quantidade * preco_unitario),0) FROM producao_caixa "
                + "WHERE idpessoa=? AND TO_CHAR(data_producao,'YYYY-MM')=?";
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idpessoa);
            ps.setString(2, periodo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
            }
        }
    }
}
