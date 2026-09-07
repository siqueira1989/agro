package Model.Dao;

import Model.Model.Safra;
import Model.Model.SafraTalhao;
import Util.PostgresConnection;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Safra: persistência (com vínculo N:N de talhões), resolução da safra ativa
 * por talhão+data (trava do Diário) e apuração de custos (COE — SEBRAE/CONAB):
 * consolidação por componente, rateio proporcional por talhão e indicadores.
 */
public class SafraDAO {

    /** Erro de negócio na validação de área. */
    public static class AreaInvalidaException extends Exception {
        public AreaInvalidaException(String m) { super(m); }
    }

    /* ======================= CRUD ======================= */

    public int inserirCompleto(Safra s) throws SQLException, AreaInvalidaException {
        try (Connection c = new PostgresConnection().getConnection()) {
            c.setAutoCommit(false);
            try {
                int id;
                String sql = "INSERT INTO safra (nome, id_cultura_principal, data_inicial, data_final, status, "
                        + "estimativa_producao, unidade_producao, despesas_fixas, id_area_producao) VALUES (?,?,?,?,?,?,?,?,?) RETURNING idsafra";
                try (PreparedStatement st = c.prepareStatement(sql)) {
                    preencherSafra(st, s);
                    ResultSet rs = st.executeQuery(); rs.next(); id = rs.getInt(1);
                }
                for (SafraTalhao t : s.getTalhoes()) inserirTalhao(c, id, t);
                c.commit();
                s.setIdSafra(id);
                return id;
            } catch (SQLException | AreaInvalidaException e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        }
    }

    public void atualizarCompleto(Safra s) throws SQLException, AreaInvalidaException {
        try (Connection c = new PostgresConnection().getConnection()) {
            c.setAutoCommit(false);
            try {
                String sql = "UPDATE safra SET nome=?, id_cultura_principal=?, data_inicial=?, data_final=?, status=?, "
                        + "estimativa_producao=?, unidade_producao=?, despesas_fixas=?, id_area_producao=? WHERE idsafra=?";
                try (PreparedStatement st = c.prepareStatement(sql)) {
                    preencherSafra(st, s);
                    st.setInt(10, s.getIdSafra());
                    st.executeUpdate();
                }
                try (PreparedStatement d = c.prepareStatement("DELETE FROM safra_talhao WHERE id_safra=?")) {
                    d.setInt(1, s.getIdSafra()); d.executeUpdate();
                }
                for (SafraTalhao t : s.getTalhoes()) inserirTalhao(c, s.getIdSafra(), t);
                c.commit();
            } catch (SQLException | AreaInvalidaException e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        }
    }

    private void preencherSafra(PreparedStatement st, Safra s) throws SQLException {
        st.setString(1, s.getNome());
        if (s.getIdCulturaPrincipal() != null) st.setInt(2, s.getIdCulturaPrincipal()); else st.setNull(2, Types.INTEGER);
        st.setDate(3, Date.valueOf(s.getDataInicial()));
        st.setDate(4, Date.valueOf(s.getDataFinal()));
        st.setString(5, s.getStatus() != null ? s.getStatus() : "PLANEJADA");
        st.setBigDecimal(6, nz(s.getEstimativaProducao()));
        st.setString(7, s.getUnidadeProducao() != null ? s.getUnidadeProducao() : "SACA");
        st.setBigDecimal(8, nz(s.getDespesasFixas()));
        if (s.getIdAreaProducao() != null) st.setInt(9, s.getIdAreaProducao()); else st.setNull(9, Types.INTEGER);
    }

    /** Insere um vínculo talhão, validando a área destinada contra a área real do talhão. */
    private void inserirTalhao(Connection c, int idSafra, SafraTalhao t) throws SQLException, AreaInvalidaException {
        BigDecimal areaReal = BigDecimal.ZERO; String nomeQ = "talhão";
        try (PreparedStatement q = c.prepareStatement("SELECT nome_quadra, area_ha FROM quadra WHERE idquadra=?")) {
            q.setInt(1, t.getIdQuadra());
            ResultSet rs = q.executeQuery();
            if (rs.next()) { nomeQ = rs.getString(1); areaReal = nz(rs.getBigDecimal(2)); }
        }
        if (areaReal.signum() > 0 && nz(t.getAreaDestinadaHa()).compareTo(areaReal) > 0) {
            throw new AreaInvalidaException("Área destinada (" + t.getAreaDestinadaHa().toPlainString()
                    + " ha) excede a área real do talhão \"" + nomeQ + "\" (" + areaReal.toPlainString() + " ha).");
        }
        try (PreparedStatement st = c.prepareStatement(
                "INSERT INTO safra_talhao (id_safra, id_quadra, area_destinada_ha) VALUES (?,?,?)")) {
            st.setInt(1, idSafra); st.setInt(2, t.getIdQuadra()); st.setBigDecimal(3, nz(t.getAreaDestinadaHa()));
            st.executeUpdate();
        }
    }

    public void definirStatus(int idSafra, String status) throws SQLException {
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement st = c.prepareStatement("UPDATE safra SET status=? WHERE idsafra=?")) {
            st.setString(1, status); st.setInt(2, idSafra); st.executeUpdate();
        }
    }

    public Safra buscarPorId(int id) throws SQLException {
        String sql = "SELECT s.*, al.nomeproduto AS cultura_nome, ar.propriedadeareaproducao AS area_nome FROM safra s "
                + "LEFT JOIN alimento al ON al.idproduto=s.id_cultura_principal "
                + "LEFT JOIN areaproducao ar ON ar.idareaproducao=s.id_area_producao WHERE s.idsafra=?";
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement st = c.prepareStatement(sql)) {
            st.setInt(1, id);
            ResultSet rs = st.executeQuery();
            if (!rs.next()) return null;
            Safra s = map(rs);
            s.setTalhoes(listarTalhoes(c, id));
            return s;
        }
    }

    public List<Safra> listar() throws SQLException {
        String sql = "SELECT s.*, al.nomeproduto AS cultura_nome, ar.propriedadeareaproducao AS area_nome FROM safra s "
                + "LEFT JOIN alimento al ON al.idproduto=s.id_cultura_principal "
                + "LEFT JOIN areaproducao ar ON ar.idareaproducao=s.id_area_producao ORDER BY s.data_inicial DESC";
        List<Safra> l = new ArrayList<>();
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement st = c.prepareStatement(sql); ResultSet rs = st.executeQuery()) {
            while (rs.next()) l.add(map(rs));
        }
        return l;
    }

    private List<SafraTalhao> listarTalhoes(Connection c, int idSafra) throws SQLException {
        List<SafraTalhao> l = new ArrayList<>();
        String sql = "SELECT st.*, q.nome_quadra, q.numero_plantas, q.area_ha, a.nomeproduto AS alimento_nome "
                + "FROM safra_talhao st JOIN quadra q ON q.idquadra=st.id_quadra "
                + "LEFT JOIN alimento a ON a.idproduto=q.id_alimento WHERE st.id_safra=? ORDER BY st.id";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idSafra);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                SafraTalhao t = new SafraTalhao();
                t.setId(rs.getInt("id")); t.setIdSafra(idSafra); t.setIdQuadra(rs.getInt("id_quadra"));
                t.setQuadraNome(rs.getString("nome_quadra")); t.setNumeroPlantas(rs.getInt("numero_plantas"));
                t.setAreaHa(rs.getBigDecimal("area_ha")); t.setAreaDestinadaHa(rs.getBigDecimal("area_destinada_ha"));
                t.setAlimentoNome(rs.getString("alimento_nome"));
                l.add(t);
            }
        }
        return l;
    }

    /* ======================= TRAVA / RESOLUÇÃO ======================= */

    /**
     * Resolve a safra de um talhão numa data (talhão ∈ safra_talhao e data no
     * período). Prioriza EM_ANDAMENTO; retorna [idsafra, nome, status] ou null.
     */
    public String[] resolverSafra(int idQuadra, LocalDate data) throws SQLException {
        String sql = "SELECT s.idsafra, s.nome, s.status FROM safra s "
                + "JOIN safra_talhao st ON st.id_safra=s.idsafra "
                + "WHERE st.id_quadra=? AND ? BETWEEN s.data_inicial AND s.data_final "
                + "ORDER BY CASE s.status WHEN 'EM_ANDAMENTO' THEN 0 WHEN 'PLANEJADA' THEN 1 ELSE 2 END LIMIT 1";
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement st = c.prepareStatement(sql)) {
            st.setInt(1, idQuadra); st.setDate(2, Date.valueOf(data));
            ResultSet rs = st.executeQuery();
            if (rs.next()) return new String[]{ String.valueOf(rs.getInt(1)), rs.getString(2), rs.getString(3) };
        }
        return null;
    }

    /* ======================= APURAÇÃO DE CUSTOS ======================= */

    /**
     * Painel de custos da safra (COE + rateio + indicadores). Considera apenas
     * as atividades CONCLUÍDAS dos talhões da safra dentro do período — ou seja,
     * o custo efetivamente realizado. Atividades PLANEJADA/EM_ANDAMENTO são
     * pré-agendamentos e não entram no consolidado (a baixa de estoque também só
     * ocorre ao finalizar). Coerente com o card da Área e os relatórios.
     */
    public Map<String, Object> resumoFinanceiro(int idSafra) throws SQLException {
        Map<String, Object> out = new LinkedHashMap<>();
        try (Connection c = new PostgresConnection().getConnection()) {
            Safra s = buscarPorId2(c, idSafra);
            if (s == null) return out;

            // COE por componente (só atividades CONCLUÍDAS: custo efetivamente realizado.
            // PLANEJADA/EM_ANDAMENTO são pré-agendamentos e não entram no custo consolidado —
            // coerente com o card da Área e com os relatórios de agregação.)
            BigDecimal cInsumos = BigDecimal.ZERO, cMaq = BigDecimal.ZERO, cMo = BigDecimal.ZERO, cDesp = BigDecimal.ZERO;
            String sqlCoe = "SELECT COALESCE(SUM(d.custo_insumos),0) i, COALESCE(SUM(d.custo_maquinas),0) m, "
                    + "COALESCE(SUM(d.custo_mao_obra),0) mo, COALESCE(SUM(d.custo_despesas),0) desp, COUNT(*) n FROM diario_campo d "
                    + "JOIN safra_talhao st ON st.id_quadra=d.id_quadra AND st.id_safra=? "
                    + "WHERE d.data BETWEEN ? AND ? AND d.status = 'CONCLUIDA'";
            int nAtiv = 0;
            try (PreparedStatement ps = c.prepareStatement(sqlCoe)) {
                ps.setInt(1, idSafra); ps.setDate(2, Date.valueOf(s.getDataInicial())); ps.setDate(3, Date.valueOf(s.getDataFinal()));
                ResultSet rs = ps.executeQuery();
                if (rs.next()) { cInsumos = rs.getBigDecimal("i"); cMaq = rs.getBigDecimal("m"); cMo = rs.getBigDecimal("mo"); cDesp = rs.getBigDecimal("desp"); nAtiv = rs.getInt("n"); }
            }
            // Despesas lançadas por execução no Diário (ex.: marmita comprada no dia) — distintas
            // das "despesas fixas" da própria Safra (custo administrativo fixo por safra).
            BigDecimal coe = cInsumos.add(cMaq).add(cMo).add(cDesp);
            BigDecimal despFixas = nz(s.getDespesasFixas());
            BigDecimal custoTotal = coe.add(despFixas);

            // Bases: total de plantas e área destinada
            int totalPlantas = 0; BigDecimal totalArea = BigDecimal.ZERO;
            List<SafraTalhao> talhoes = listarTalhoes(c, idSafra);
            for (SafraTalhao t : talhoes) { totalPlantas += t.getNumeroPlantas(); totalArea = totalArea.add(nz(t.getAreaDestinadaHa())); }

            BigDecimal custoPorPlanta = totalPlantas > 0 ? custoTotal.divide(new BigDecimal(totalPlantas), 4, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            BigDecimal custoPorHa = totalArea.signum() > 0 ? custoTotal.divide(totalArea, 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            BigDecimal custoPorSaca = nz(s.getEstimativaProducao()).signum() > 0 ? custoTotal.divide(s.getEstimativaProducao(), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

            // Rateio proporcional por talhão (base: numero de plantas)
            List<Map<String, Object>> rateio = new ArrayList<>();
            for (SafraTalhao t : talhoes) {
                BigDecimal part = totalPlantas > 0 ? new BigDecimal(t.getNumeroPlantas()).divide(new BigDecimal(totalPlantas), 6, RoundingMode.HALF_UP) : BigDecimal.ZERO;
                BigDecimal custoRateado = custoTotal.multiply(part).setScale(2, RoundingMode.HALF_UP);
                BigDecimal custoReal = custoRealTalhao(c, idSafra, t.getIdQuadra(), s.getDataInicial(), s.getDataFinal());
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("talhao", t.getQuadraNome()); r.put("alimento", t.getAlimentoNome());
                r.put("numeroPlantas", t.getNumeroPlantas());
                r.put("areaDestinadaHa", t.getAreaDestinadaHa());
                r.put("participacaoPct", part.multiply(new BigDecimal(100)).setScale(2, RoundingMode.HALF_UP));
                r.put("custoRateado", custoRateado); r.put("custoReal", custoReal);
                rateio.add(r);
            }

            out.put("safra", s);
            out.put("qtdAtividades", nAtiv);
            out.put("custoInsumos", cInsumos); out.put("custoMaquinas", cMaq); out.put("custoMaoObra", cMo);
            out.put("custoDespesas", cDesp);
            out.put("coe", coe); out.put("despesasFixas", despFixas); out.put("custoTotal", custoTotal);
            out.put("totalPlantas", totalPlantas); out.put("totalAreaHa", totalArea);
            out.put("custoPorPlanta", custoPorPlanta); out.put("custoPorHa", custoPorHa);
            out.put("custoPorUnidade", custoPorSaca);
            out.put("pctInsumos", pct(cInsumos, custoTotal)); out.put("pctMaquinas", pct(cMaq, custoTotal));
            out.put("pctMaoObra", pct(cMo, custoTotal)); out.put("pctDespesasFixas", pct(despFixas, custoTotal));
            out.put("pctDespesas", pct(cDesp, custoTotal));
            out.put("rateio", rateio);
        }
        return out;
    }

    private BigDecimal custoRealTalhao(Connection c, int idSafra, int idQuadra, LocalDate ini, LocalDate fim) throws SQLException {
        String sql = "SELECT COALESCE(SUM(custo_total),0) FROM diario_campo WHERE id_quadra=? AND data BETWEEN ? AND ? AND status = 'CONCLUIDA'";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idQuadra); ps.setDate(2, Date.valueOf(ini)); ps.setDate(3, Date.valueOf(fim));
            ResultSet rs = ps.executeQuery();
            return rs.next() ? nz(rs.getBigDecimal(1)) : BigDecimal.ZERO;
        }
    }

    private Safra buscarPorId2(Connection c, int id) throws SQLException {
        try (PreparedStatement st = c.prepareStatement(
                "SELECT s.*, al.nomeproduto AS cultura_nome, ar.propriedadeareaproducao AS area_nome FROM safra s "
                + "LEFT JOIN alimento al ON al.idproduto=s.id_cultura_principal "
                + "LEFT JOIN areaproducao ar ON ar.idareaproducao=s.id_area_producao WHERE s.idsafra=?")) {
            st.setInt(1, id);
            ResultSet rs = st.executeQuery();
            return rs.next() ? map(rs) : null;
        }
    }

    private static BigDecimal pct(BigDecimal parte, BigDecimal total) {
        if (total == null || total.signum() == 0) return BigDecimal.ZERO;
        return parte.multiply(new BigDecimal(100)).divide(total, 2, RoundingMode.HALF_UP);
    }

    private Safra map(ResultSet rs) throws SQLException {
        Safra s = new Safra();
        s.setIdSafra(rs.getInt("idsafra")); s.setNome(rs.getString("nome"));
        s.setIdCulturaPrincipal((Integer) rs.getObject("id_cultura_principal")); s.setCulturaNome(rs.getString("cultura_nome"));
        Date di = rs.getDate("data_inicial"); s.setDataInicial(di != null ? di.toLocalDate() : null);
        Date df = rs.getDate("data_final"); s.setDataFinal(df != null ? df.toLocalDate() : null);
        s.setStatus(rs.getString("status")); s.setEstimativaProducao(rs.getBigDecimal("estimativa_producao"));
        s.setUnidadeProducao(rs.getString("unidade_producao")); s.setDespesasFixas(rs.getBigDecimal("despesas_fixas"));
        s.setIdAreaProducao((Integer) rs.getObject("id_area_producao")); s.setAreaProducaoNome(rs.getString("area_nome"));
        return s;
    }

    private static BigDecimal nz(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }
}
