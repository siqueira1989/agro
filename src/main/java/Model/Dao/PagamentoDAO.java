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

/** Registro de pagamentos (forma + dados bancários). */
public class PagamentoDAO {

    private final VinculoDAO vinculoDAO = new VinculoDAO();

    public void registrar(int idpessoa, String periodo, String tipoFunc, BigDecimal valor,
                          String forma, String banco, String agencia, String conta,
                          String chavePix, LocalDate dataPagamento, String obs) throws SQLException {
        Integer idVinc = null;
        try {
            Vinculo v = vinculoDAO.buscarPorData(idpessoa, LocalDate.parse(periodo + "-01"));
            if (v != null) idVinc = v.getIdVinculo();
        } catch (Exception ignored) { }

        String sql = "INSERT INTO pagamento (idpessoa, id_vinculo, periodo, tipo_funcionario, valor, "
                + "forma_pagamento, banco, agencia, conta, chave_pix, data_pagamento, observacao) "
                + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idpessoa);
            if (idVinc != null) ps.setInt(2, idVinc); else ps.setNull(2, Types.INTEGER);
            ps.setString(3, periodo);
            ps.setString(4, tipoFunc);
            ps.setBigDecimal(5, valor != null ? valor : BigDecimal.ZERO);
            ps.setString(6, forma);
            ps.setString(7, banco);
            ps.setString(8, agencia);
            ps.setString(9, conta);
            ps.setString(10, chavePix);
            ps.setDate(11, Date.valueOf(dataPagamento != null ? dataPagamento : LocalDate.now()));
            ps.setString(12, obs);
            ps.executeUpdate();
        }
    }

    public List<Map<String, Object>> listar(int idpessoa, String periodo) throws SQLException {
        String sql = "SELECT * FROM pagamento WHERE idpessoa=?"
                + (periodo != null && !periodo.isBlank() ? " AND periodo=?" : "")
                + " ORDER BY data_pagamento DESC, idpagamento DESC";
        List<Map<String, Object>> lista = new ArrayList<>();
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idpessoa);
            if (periodo != null && !periodo.isBlank()) ps.setString(2, periodo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("idPagamento", rs.getInt("idpagamento"));
                    m.put("periodo", rs.getString("periodo"));
                    m.put("valor", rs.getBigDecimal("valor"));
                    m.put("formaPagamento", rs.getString("forma_pagamento"));
                    m.put("banco", rs.getString("banco"));
                    m.put("agencia", rs.getString("agencia"));
                    m.put("conta", rs.getString("conta"));
                    m.put("chavePix", rs.getString("chave_pix"));
                    m.put("dataPagamento", rs.getDate("data_pagamento").toLocalDate().toString());
                    m.put("observacao", rs.getString("observacao"));
                    lista.add(m);
                }
            }
        }
        return lista;
    }

    public void excluir(int idPagamento) throws SQLException {
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM pagamento WHERE idpagamento=?")) {
            ps.setInt(1, idPagamento);
            ps.executeUpdate();
        }
    }
}
