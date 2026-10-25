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

    /* ===================== Caixas lançadas no Diário de Campo (SÓ LEITURA) =====================
     * O Colhedor (Colheita) e o Embalador (Processamento) também são lançados no Diário de Campo
     * em regime PRODUCAO (diario_funcionario). O pagamento é feito só aqui no perfil, então o
     * perfil lê essas linhas — nunca grava em tabela do Diário.
     * Regra de status: só CONCLUIDA entra no total a pagar (mesma regra do custo da safra: o
     * diário fechado não muda mais). EM_ANDAMENTO aparece na tabela, marcado "não entra no
     * total", porque ainda pode ser editado/receber execuções. PLANEJADA e CANCELADA ficam fora. */

    private static final String SQL_DIARIO_BASE =
          "SELECT d.iddiario, d.numero_diario, d.status, d.id_area, COALESCE(df.data_execucao, d.data) AS data_execucao, "
        + "       t.nome AS atividade, q.nome_quadra, "
        + "       COALESCE(a.propriedadeareaproducao, a.siglasareaproducao) AS area, "
        + "       df.quantidade_caixas, df.preco_por_caixa, df.valor_regime %s "
        + "FROM diario_funcionario df "
        + "JOIN diario_campo d ON d.iddiario = df.iddiario "
        + "LEFT JOIN diario_tipo_atividade t ON t.idtipo = d.id_tipo_atividade "
        + "LEFT JOIN quadra q ON q.idquadra = d.id_quadra "
        + "LEFT JOIN areaproducao a ON a.idareaproducao = d.id_area "
        + "WHERE df.idpessoa=? AND df.regime_trabalho='PRODUCAO' "
        + "AND d.status IN ('EM_ANDAMENTO','CONCLUIDA') ";

    /** Tipo(s) de embalagem do Processamento vêm da tabela da Colheita (V30), se ela já existir. */
    private static final String SQL_TIPO_EMBALAGEM =
          ", (SELECT string_agg(DISTINCT e.tipo_caixa, ', ') FROM colheita_processamento cp "
        + "   JOIN colheita_processamento_embalagem e ON e.idprocessamento = cp.idprocessamento "
        + "   WHERE cp.iddiario = d.iddiario AND e.idpessoa = df.idpessoa) AS tipo_embalagem ";

    private boolean temTabelaEmbalagem(Connection c) throws SQLException {
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT to_regclass('public.colheita_processamento_embalagem') IS NOT NULL")) {
            return rs.next() && rs.getBoolean(1);
        }
    }

    /**
     * Linhas do Diário de Campo (regime PRODUCAO) da pessoa no mês. Cada linha traz
     * {@code contaNoTotal} = diário CONCLUIDA.
     */
    public List<Map<String, Object>> listarDoDiario(int idpessoa, String periodo) throws SQLException {
        List<Map<String, Object>> lista = new ArrayList<>();
        try (Connection c = new PostgresConnection().getConnection()) {
            boolean emb = temTabelaEmbalagem(c);
            String sql = String.format(SQL_DIARIO_BASE, emb ? SQL_TIPO_EMBALAGEM : ", NULL AS tipo_embalagem ")
                    + "AND TO_CHAR(COALESCE(df.data_execucao, d.data),'YYYY-MM')=? ORDER BY 5, d.numero_diario";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, idpessoa);
                ps.setString(2, periodo);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> m = new LinkedHashMap<>();
                        String atividade = rs.getString("atividade");
                        String tipo = rs.getString("tipo_embalagem");
                        if (tipo == null && atividade != null && atividade.toLowerCase().contains("colh")) tipo = "CONTENTOR";
                        BigDecimal caixas = rs.getBigDecimal("quantidade_caixas");
                        BigDecimal preco  = rs.getBigDecimal("preco_por_caixa");
                        BigDecimal valor  = rs.getBigDecimal("valor_regime");
                        if (valor == null && caixas != null && preco != null) valor = caixas.multiply(preco);
                        Date dt = rs.getDate("data_execucao");
                        String status = rs.getString("status");
                        m.put("idDiario", rs.getInt("iddiario"));
                        m.put("numeroDiario", rs.getString("numero_diario"));
                        m.put("status", status);
                        m.put("contaNoTotal", "CONCLUIDA".equals(status));
                        m.put("idArea", rs.getObject("id_area"));
                        m.put("data", dt != null ? dt.toLocalDate().toString() : null);
                        m.put("atividade", atividade);
                        m.put("area", rs.getString("area"));
                        m.put("talhao", rs.getString("nome_quadra"));
                        m.put("tipoCaixa", tipo);
                        m.put("caixas", caixas != null ? caixas : BigDecimal.ZERO);
                        m.put("precoPorCaixa", preco != null ? preco : BigDecimal.ZERO);
                        m.put("valor", valor != null ? valor : BigDecimal.ZERO);
                        lista.add(m);
                    }
                }
            }
        }
        return lista;
    }

    /** Números dos diários (EM_ANDAMENTO/CONCLUIDA) com caixas da pessoa nesta data — aviso de duplicidade. */
    public List<String> diariosNaData(int idpessoa, LocalDate data) throws SQLException {
        String sql = "SELECT DISTINCT d.numero_diario FROM diario_funcionario df "
                + "JOIN diario_campo d ON d.iddiario = df.iddiario "
                + "WHERE df.idpessoa=? AND df.regime_trabalho='PRODUCAO' AND COALESCE(df.data_execucao, d.data)=? "
                + "AND d.status IN ('EM_ANDAMENTO','CONCLUIDA') ORDER BY 1";
        List<String> out = new ArrayList<>();
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idpessoa);
            ps.setDate(2, Date.valueOf(data));
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(rs.getString(1)); }
        }
        return out;
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
