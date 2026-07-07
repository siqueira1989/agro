package Model.Dao;

import Util.PostgresConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Preços das caixas de produção, com histórico por ano e opcionalmente por
 * funcionário (competência). Resolução: preço específico do funcionário no
 * ano tem prioridade; senão o preço geral do ano.
 */
public class PrecoCaixaDAO {

    /** Insere ou atualiza o preço de um tipo de caixa (geral ou por funcionário). */
    public void upsert(String tipoCaixa, int ano, Integer idpessoa, BigDecimal preco) throws SQLException {
        try (Connection c = new PostgresConnection().getConnection()) {
            String where = "tipo_caixa=? AND ano=? AND "
                    + (idpessoa != null ? "idpessoa=?" : "idpessoa IS NULL");
            // tenta UPDATE
            try (PreparedStatement up = c.prepareStatement(
                    "UPDATE preco_caixa SET preco=? WHERE " + where)) {
                up.setBigDecimal(1, preco);
                up.setString(2, tipoCaixa);
                up.setInt(3, ano);
                if (idpessoa != null) up.setInt(4, idpessoa);
                if (up.executeUpdate() > 0) return;
            }
            // senão INSERT
            try (PreparedStatement in = c.prepareStatement(
                    "INSERT INTO preco_caixa (tipo_caixa, ano, idpessoa, preco) VALUES (?,?,?,?)")) {
                in.setString(1, tipoCaixa);
                in.setInt(2, ano);
                if (idpessoa != null) in.setInt(3, idpessoa); else in.setNull(3, Types.INTEGER);
                in.setBigDecimal(4, preco);
                in.executeUpdate();
            }
        }
    }

    /** Preço aplicável: específico do funcionário no ano, senão o geral do ano. */
    public BigDecimal resolverPreco(String tipoCaixa, int ano, int idpessoa) throws SQLException {
        String sql = "SELECT preco FROM preco_caixa "
                + "WHERE tipo_caixa=? AND ano=? AND (idpessoa=? OR idpessoa IS NULL) "
                + "ORDER BY idpessoa NULLS LAST LIMIT 1";
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tipoCaixa);
            ps.setInt(2, ano);
            ps.setInt(3, idpessoa);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    BigDecimal v = rs.getBigDecimal(1);
                    return v != null ? v : BigDecimal.ZERO;
                }
            }
        }
        return BigDecimal.ZERO;
    }

    /** Preços gerais de um ano (idpessoa NULL) — para o gestor configurar. */
    public List<Map<String, Object>> listarGeraisDoAno(int ano) throws SQLException {
        String sql = "SELECT tipo_caixa, preco FROM preco_caixa "
                + "WHERE ano=? AND idpessoa IS NULL ORDER BY tipo_caixa";
        List<Map<String, Object>> lista = new ArrayList<>();
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, ano);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("tipoCaixa", rs.getString("tipo_caixa"));
                    m.put("preco", rs.getBigDecimal("preco"));
                    lista.add(m);
                }
            }
        }
        return lista;
    }

    /** Preços específicos de um funcionário num ano (overrides). */
    public Map<String, BigDecimal> mapaPrecosFuncionario(int idpessoa, int ano) throws SQLException {
        String sql = "SELECT tipo_caixa, preco FROM preco_caixa WHERE ano=? AND idpessoa=?";
        Map<String, BigDecimal> mapa = new LinkedHashMap<>();
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, ano);
            ps.setInt(2, idpessoa);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) mapa.put(rs.getString("tipo_caixa"), rs.getBigDecimal("preco"));
            }
        }
        return mapa;
    }
}
