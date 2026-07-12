package Model.Dao;

import Model.Model.Insumo;
import Util.PostgresConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Catálogo de insumos. Saldo (quantidade_disponivel) e preco_medio são
 * atualizados pelas movimentações (ver EstoqueMovimentoDAO); aqui ficam o
 * cadastro do insumo e as consultas de listagem/indicadores.
 */
public class InsumoDAO {

    /* ---------------------------- CRUD ---------------------------- */

    public void inserir(Insumo i) throws SQLException {
        String sql = "INSERT INTO insumo (nome, categoria, grandeza, unidade, "
                   + "estoque_minimo, id_fornecedor, situacao) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement st = c.prepareStatement(sql)) {
            st.setString(1, i.getNome());
            st.setString(2, i.getCategoria());
            st.setString(3, i.getGrandeza());
            st.setString(4, i.getUnidade());
            st.setBigDecimal(5, i.getEstoqueMinimo() != null ? i.getEstoqueMinimo() : BigDecimal.ZERO);
            if (i.getIdFornecedor() != null) st.setInt(6, i.getIdFornecedor()); else st.setNull(6, Types.INTEGER);
            st.setBoolean(7, i.isSituacao());
            st.executeUpdate();
        }
    }

    /** Atualiza apenas o cadastro (não mexe em saldo/preço médio). */
    public void atualizar(Insumo i) throws SQLException {
        String sql = "UPDATE insumo SET nome=?, categoria=?, grandeza=?, unidade=?, "
                   + "estoque_minimo=?, id_fornecedor=?, situacao=? WHERE idinsumo=?";
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement st = c.prepareStatement(sql)) {
            st.setString(1, i.getNome());
            st.setString(2, i.getCategoria());
            st.setString(3, i.getGrandeza());
            st.setString(4, i.getUnidade());
            st.setBigDecimal(5, i.getEstoqueMinimo() != null ? i.getEstoqueMinimo() : BigDecimal.ZERO);
            if (i.getIdFornecedor() != null) st.setInt(6, i.getIdFornecedor()); else st.setNull(6, Types.INTEGER);
            st.setBoolean(7, i.isSituacao());
            st.setInt(8, i.getIdInsumo());
            st.executeUpdate();
        }
    }

    /** Ativa/desativa (soft delete). */
    public void definirSituacao(int idInsumo, boolean situacao) throws SQLException {
        String sql = "UPDATE insumo SET situacao=? WHERE idinsumo=?";
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement st = c.prepareStatement(sql)) {
            st.setBoolean(1, situacao);
            st.setInt(2, idInsumo);
            st.executeUpdate();
        }
    }

    public Insumo buscarPorId(int idInsumo) throws SQLException {
        String sql = "SELECT * FROM insumo WHERE idinsumo=?";
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement st = c.prepareStatement(sql)) {
            st.setInt(1, idInsumo);
            ResultSet rs = st.executeQuery();
            if (rs.next()) return mapRow(rs);
        }
        return null;
    }

    /* ------------------------- CONSULTAS -------------------------- */

    /**
     * Lista insumos (com nome do fornecedor e flag de abaixo do mínimo) para a
     * tabela. Filtros opcionais por categoria e por situação.
     */
    public List<Map<String, Object>> listar(String categoria, Boolean situacao) throws SQLException {
        StringBuilder sql = new StringBuilder(
            "SELECT i.*, p.nomepessoa AS fornecedor_nome, "
          + "(i.quantidade_disponivel <= i.estoque_minimo) AS abaixo_minimo "
          + "FROM insumo i LEFT JOIN parceiro p ON p.idpessoa = i.id_fornecedor WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (categoria != null && !categoria.isBlank()) { sql.append(" AND i.categoria=?"); params.add(categoria.toUpperCase()); }
        if (situacao != null) { sql.append(" AND i.situacao=?"); params.add(situacao); }
        sql.append(" ORDER BY i.nome");

        List<Map<String, Object>> lista = new ArrayList<>();
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement st = c.prepareStatement(sql.toString())) {
            for (int k = 0; k < params.size(); k++) st.setObject(k + 1, params.get(k));
            ResultSet rs = st.executeQuery();
            while (rs.next()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("idInsumo", rs.getInt("idinsumo"));
                m.put("nome", rs.getString("nome"));
                m.put("categoria", rs.getString("categoria"));
                m.put("grandeza", rs.getString("grandeza"));
                m.put("unidade", rs.getString("unidade"));
                m.put("quantidadeDisponivel", rs.getBigDecimal("quantidade_disponivel"));
                m.put("precoMedio", rs.getBigDecimal("preco_medio"));
                m.put("estoqueMinimo", rs.getBigDecimal("estoque_minimo"));
                m.put("idFornecedor", rs.getObject("id_fornecedor"));
                m.put("fornecedorNome", rs.getString("fornecedor_nome"));
                m.put("situacao", rs.getBoolean("situacao"));
                m.put("abaixoMinimo", rs.getBoolean("abaixo_minimo"));
                lista.add(m);
            }
        }
        return lista;
    }

    public int contarTotal() throws SQLException {
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement st = c.prepareStatement("SELECT COUNT(*) FROM insumo WHERE situacao=true");
             ResultSet rs = st.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public int contarAbaixoMinimo() throws SQLException {
        String sql = "SELECT COUNT(*) FROM insumo WHERE situacao=true AND quantidade_disponivel <= estoque_minimo";
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement st = c.prepareStatement(sql);
             ResultSet rs = st.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /** Valor total em estoque = Σ (quantidade_disponivel × preco_medio). */
    public BigDecimal valorTotalEstoque() throws SQLException {
        String sql = "SELECT COALESCE(SUM(quantidade_disponivel * preco_medio),0) FROM insumo WHERE situacao=true";
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement st = c.prepareStatement(sql);
             ResultSet rs = st.executeQuery()) {
            return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
        }
    }

    private Insumo mapRow(ResultSet rs) throws SQLException {
        Insumo i = new Insumo();
        i.setIdInsumo(rs.getInt("idinsumo"));
        i.setNome(rs.getString("nome"));
        i.setCategoria(rs.getString("categoria"));
        i.setGrandeza(rs.getString("grandeza"));
        i.setUnidade(rs.getString("unidade"));
        i.setQuantidadeDisponivel(rs.getBigDecimal("quantidade_disponivel"));
        i.setPrecoMedio(rs.getBigDecimal("preco_medio"));
        i.setEstoqueMinimo(rs.getBigDecimal("estoque_minimo"));
        i.setIdFornecedor((Integer) rs.getObject("id_fornecedor"));
        i.setSituacao(rs.getBoolean("situacao"));
        return i;
    }
}
