package Model.Dao;

import Model.Model.ColheitaLote;
import Util.PostgresConnection;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Módulo de Colheita e Estoque: lotes gerados por {@link DiarioCampoDAO#finalizar(int)} quando
 * um diário de Colheita tem lançamentos em regime PRODUCAO. Nem toda a colheita é processada de
 * uma vez — o saldo (quantidade_total - quantidade_processada) fica retido como estoque de
 * produção, e cada processamento pode dar baixa numa PARTE desse saldo, não necessariamente tudo.
 */
public class ColheitaDAO {

    public List<ColheitaLote> listar(String status) throws SQLException {
        StringBuilder sql = new StringBuilder(
            "SELECT cl.*, d.numero_diario, q.nome_quadra AS quadra_nome, al.nomeproduto AS cultura_nome, "
          + "ar.siglasareaproducao AS sigla_area "
          + "FROM colheita_lote cl "
          + "JOIN diario_campo d ON d.iddiario = cl.iddiario "
          + "LEFT JOIN quadra q ON q.idquadra = cl.id_quadra "
          + "LEFT JOIN areaproducao ar ON ar.idareaproducao = q.idareaproducao "
          + "LEFT JOIN alimento al ON al.idproduto = cl.id_cultura WHERE 1=1");
        List<Object> ps = new ArrayList<>();
        if (status != null && !status.isBlank()) { sql.append(" AND cl.status=?"); ps.add(status); }
        sql.append(" ORDER BY cl.data_colheita DESC, cl.idlote DESC");
        List<ColheitaLote> lista = new ArrayList<>();
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement st = c.prepareStatement(sql.toString())) {
            for (int k = 0; k < ps.size(); k++) st.setObject(k + 1, ps.get(k));
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        }
        return lista;
    }

    /** Indicadores agregados: quantidade total geral e por status, nº de lotes/saldo. */
    public Map<String, Object> indicadores() throws SQLException {
        String sql = "SELECT status, COUNT(*) AS qtd_lotes, "
                + "COALESCE(SUM(quantidade_total),0) AS qtd_total, "
                + "COALESCE(SUM(quantidade_total - quantidade_processada),0) AS qtd_saldo "
                + "FROM colheita_lote GROUP BY status";
        Map<String, Object> porStatus = new LinkedHashMap<>();
        BigDecimal totalGeral = BigDecimal.ZERO;
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement st = c.prepareStatement(sql);
             ResultSet rs = st.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("qtdLotes", rs.getInt("qtd_lotes"));
                m.put("qtdTotal", rs.getBigDecimal("qtd_total"));
                m.put("qtdSaldo", rs.getBigDecimal("qtd_saldo"));
                porStatus.put(rs.getString("status"), m);
                totalGeral = totalGeral.add(rs.getBigDecimal("qtd_total"));
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("totalGeral", totalGeral);
        out.put("porStatus", porStatus);
        return out;
    }

    /**
     * Registra o processamento de uma QUANTIDADE (não necessariamente o saldo inteiro) do lote —
     * a forma dinâmica de lidar com "nem sempre vai ser processado tudo": o que não for
     * processado agora continua retido como estoque de produção (status ESTOQUE), e o lote pode
     * receber vários processamentos parciais ao longo do tempo até esgotar o saldo (status vira
     * PROCESSADO só quando quantidade_processada atinge quantidade_total). Lote FINALIZADO não
     * aceita mais processamento — o ciclo dele já foi encerrado.
     */
    public void registrarProcessamento(int idLote, BigDecimal quantidade) throws SQLException {
        if (quantidade == null || quantidade.signum() <= 0) {
            throw new IllegalArgumentException("Quantidade a processar deve ser maior que zero.");
        }
        try (Connection c = new PostgresConnection().getConnection()) {
            String status; BigDecimal total, processada;
            try (PreparedStatement st = c.prepareStatement(
                    "SELECT status, quantidade_total, quantidade_processada FROM colheita_lote WHERE idlote=?")) {
                st.setInt(1, idLote);
                try (ResultSet rs = st.executeQuery()) {
                    if (!rs.next()) throw new IllegalArgumentException("Lote não encontrado.");
                    status = rs.getString(1); total = rs.getBigDecimal(2); processada = rs.getBigDecimal(3);
                }
            }
            if ("FINALIZADO".equals(status)) {
                throw new IllegalArgumentException("Lote finalizado não pode receber processamento.");
            }
            BigDecimal saldo = total.subtract(processada);
            if (quantidade.compareTo(saldo) > 0) {
                throw new IllegalArgumentException(
                    "Quantidade maior que o saldo em estoque (" + saldo.setScale(2, RoundingMode.HALF_UP) + ").");
            }
            BigDecimal novaProcessada = processada.add(quantidade);
            String novoStatus = novaProcessada.compareTo(total) >= 0 ? "PROCESSADO" : "ESTOQUE";
            try (PreparedStatement st = c.prepareStatement(
                    "UPDATE colheita_lote SET quantidade_processada=?, status=?, atualizado_em=now() WHERE idlote=?")) {
                st.setBigDecimal(1, novaProcessada); st.setString(2, novoStatus); st.setInt(3, idLote);
                st.executeUpdate();
            }
        }
    }

    /** Encerra o lote (vendido, descartado, ou já processado) — terminal, sem mexer na quantidade. */
    public void finalizarLote(int idLote) throws SQLException {
        try (Connection c = new PostgresConnection().getConnection()) {
            String status;
            try (PreparedStatement st = c.prepareStatement("SELECT status FROM colheita_lote WHERE idlote=?")) {
                st.setInt(1, idLote);
                try (ResultSet rs = st.executeQuery()) {
                    if (!rs.next()) throw new IllegalArgumentException("Lote não encontrado.");
                    status = rs.getString(1);
                }
            }
            if ("FINALIZADO".equals(status)) return;
            try (PreparedStatement st = c.prepareStatement(
                    "UPDATE colheita_lote SET status='FINALIZADO', atualizado_em=now() WHERE idlote=?")) {
                st.setInt(1, idLote); st.executeUpdate();
            }
        }
    }

    private ColheitaLote mapear(ResultSet rs) throws SQLException {
        ColheitaLote l = new ColheitaLote();
        l.setIdLote(rs.getInt("idlote")); l.setIdDiario(rs.getInt("iddiario"));
        l.setNumeroDiario(rs.getString("numero_diario"));
        l.setIdQuadra((Integer) rs.getObject("id_quadra")); l.setQuadraNome(rs.getString("quadra_nome"));
        l.setIdCultura((Integer) rs.getObject("id_cultura")); l.setCulturaNome(rs.getString("cultura_nome"));
        l.setSiglaArea(rs.getString("sigla_area"));
        Date d = rs.getDate("data_colheita"); l.setDataColheita(d != null ? d.toLocalDate() : null);
        l.setQuantidadeTotal(rs.getBigDecimal("quantidade_total"));
        l.setQuantidadeProcessada(rs.getBigDecimal("quantidade_processada"));
        l.setUnidade(rs.getString("unidade")); l.setStatus(rs.getString("status"));
        l.setObservacoes(rs.getString("observacoes"));
        return l;
    }
}
