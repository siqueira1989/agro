package Model.Dao;

import Model.Model.ColheitaLote;
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
 * Módulo de Colheita e Estoque: lotes gerados por {@link DiarioCampoDAO#finalizar(int)} quando
 * um diário de Colheita tem lançamentos em regime PRODUCAO. Nem toda a colheita é processada de
 * uma vez — o saldo (quantidade_total - quantidade_processada) fica retido como estoque de
 * produção, e cada processamento pode dar baixa numa PARTE desse saldo, não necessariamente tudo.
 */
public class ColheitaDAO {

    private static final String SELECT_LOTE =
            "SELECT cl.*, d.numero_diario, q.nome_quadra AS quadra_nome, al.nomeproduto AS cultura_nome, "
          + "ar.siglasareaproducao AS sigla_area, ar.idareaproducao AS id_area, ar.propriedadeareaproducao AS area_nome "
          + "FROM colheita_lote cl "
          + "JOIN diario_campo d ON d.iddiario = cl.iddiario "
          + "LEFT JOIN quadra q ON q.idquadra = cl.id_quadra "
          + "LEFT JOIN areaproducao ar ON ar.idareaproducao = q.idareaproducao "
          + "LEFT JOIN alimento al ON al.idproduto = cl.id_cultura";

    public List<ColheitaLote> listar(String status) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT_LOTE + " WHERE 1=1");
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

    /* ============ PROCESSAMENTO COM DIÁRIO (modal "Processar", V30) ============ */

    /** Uma linha de embalagem do modal Processar: embalador + tipo de embalagem + caixas × preço/caixa. */
    public static class LinhaEmbalagem {
        public int idPessoa;
        public String tipoCaixa;     // código da embalagem (normalizado por DespesasCustosDAO)
        public String descricao;     // rótulo de embalagem nova ("Papelão 7kg"); vazio preserva o cadastro
        public BigDecimal caixas;
        public BigDecimal preco;
    }

    /**
     * Validação sem gravar, chamada pelo modal ANTES de criar o diário de Processamento: lote
     * aberto, quantidade ≤ saldo, safra cobrindo talhão + data e linhas de embalagem coerentes.
     * Assim o diário só é criado quando a baixa do lote vai passar. Regras ⇒ IllegalArgumentException.
     */
    public ColheitaLote validarProcessamento(int idLote, LocalDate data, BigDecimal quantidade,
                                             List<LinhaEmbalagem> embalagens) throws SQLException {
        ColheitaLote l;
        try (Connection c = new PostgresConnection().getConnection()) {
            l = carregarLote(c, idLote, false);
        }
        validar(l, data, quantidade, embalagens);
        String[] sf = new SafraDAO().resolverSafra(l.getIdQuadra(), data);
        if (sf == null) {
            throw new IllegalArgumentException("Não há Safra que inclua o talhão " + l.getQuadraNome()
                + " e cubra a data " + data.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                + ". Cadastre ou ajuste a safra (Produção → Safra) antes de processar.");
        }
        if ("FINALIZADA".equals(sf[2])) {
            throw new IllegalArgumentException("A safra \"" + sf[1] + "\" está FINALIZADA — não é possível lançar o processamento nela.");
        }
        return l;
    }

    /**
     * Grava o processamento numa transação só: baixa do saldo do lote, preço de cada embalagem em
     * Despesas e Custos (EMBALAGEM por alimento + área; insere ou atualiza o preço) e o vínculo
     * lote ↔ diário de Processamento com o detalhe por embalador (V30). O diário já foi criado pelo
     * Diário de Campo; aqui só se confere que é um Processamento do mesmo talhão e ainda sem lote.
     *
     * Transação manual (e não TransacaoUtil) porque as regras lançam IllegalArgumentException no
     * meio das gravações, e TransacaoUtil só faz rollback de SQLException.
     */
    public int registrarProcessamentoComDiario(int idLote, int idDiario, LocalDate data, BigDecimal quantidade,
                                               List<LinhaEmbalagem> embalagens) throws SQLException {
        try (Connection c = new PostgresConnection().getConnection()) {
            c.setAutoCommit(false);
            try {
                ColheitaLote l = carregarLote(c, idLote, true);
                validar(l, data, quantidade, embalagens);
                validarDiarioProcessamento(c, idDiario, l);

                DespesasCustosDAO despDAO = new DespesasCustosDAO();
                Map<String, Integer> idDespesaPorTipo = new LinkedHashMap<>();
                for (LinhaEmbalagem e : embalagens) {
                    String tipo = DespesasCustosDAO.normalizarTipoCaixa(e.tipoCaixa);
                    if (idDespesaPorTipo.containsKey(tipo)) continue;
                    String desc = e.descricao == null || e.descricao.isBlank() ? null : e.descricao.trim();
                    idDespesaPorTipo.put(tipo, despDAO.registrarPrecoCaixa(c, "EMBALAGEM",
                            l.getIdCultura(), l.getIdArea(), tipo, desc, e.preco));
                }

                aplicarBaixa(c, l, quantidade);

                BigDecimal totalCaixas = BigDecimal.ZERO, totalValor = BigDecimal.ZERO;
                for (LinhaEmbalagem e : embalagens) {
                    totalCaixas = totalCaixas.add(e.caixas);
                    totalValor = totalValor.add(e.caixas.multiply(e.preco).setScale(2, RoundingMode.HALF_UP));
                }
                int idProc;
                try (PreparedStatement st = c.prepareStatement(
                        "INSERT INTO colheita_processamento (idlote, iddiario, data_processamento, quantidade, "
                      + "total_caixas, valor_embalagem) VALUES (?,?,?,?,?,?) RETURNING idprocessamento")) {
                    st.setInt(1, idLote); st.setInt(2, idDiario); st.setDate(3, Date.valueOf(data));
                    st.setBigDecimal(4, quantidade); st.setBigDecimal(5, totalCaixas); st.setBigDecimal(6, totalValor);
                    try (ResultSet rs = st.executeQuery()) { rs.next(); idProc = rs.getInt(1); }
                }
                try (PreparedStatement st = c.prepareStatement(
                        "INSERT INTO colheita_processamento_embalagem (idprocessamento, idpessoa, iddespesascusto, "
                      + "tipo_caixa, quantidade_caixas, preco_caixa, valor) VALUES (?,?,?,?,?,?,?)")) {
                    for (LinhaEmbalagem e : embalagens) {
                        String tipo = DespesasCustosDAO.normalizarTipoCaixa(e.tipoCaixa);
                        st.setInt(1, idProc); st.setInt(2, e.idPessoa); st.setInt(3, idDespesaPorTipo.get(tipo));
                        st.setString(4, tipo); st.setBigDecimal(5, e.caixas); st.setBigDecimal(6, e.preco);
                        st.setBigDecimal(7, e.caixas.multiply(e.preco).setScale(2, RoundingMode.HALF_UP));
                        st.addBatch();
                    }
                    st.executeBatch();
                }
                c.commit();
                return idProc;
            } catch (SQLException | RuntimeException e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        }
    }

    /** Processamentos de um lote (com o nº do diário de Processamento), mais recentes primeiro. */
    public List<Map<String, Object>> listarProcessamentos(int idLote) throws SQLException {
        String sql = "SELECT p.idprocessamento, p.iddiario, d.numero_diario, d.status AS status_diario, "
                + "p.data_processamento, p.quantidade, p.total_caixas, p.valor_embalagem "
                + "FROM colheita_processamento p LEFT JOIN diario_campo d ON d.iddiario = p.iddiario "
                + "WHERE p.idlote=? ORDER BY p.data_processamento DESC, p.idprocessamento DESC";
        List<Map<String, Object>> out = new ArrayList<>();
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement st = c.prepareStatement(sql)) {
            st.setInt(1, idLote);
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("idProcessamento", rs.getInt("idprocessamento"));
                    m.put("idDiario", rs.getObject("iddiario"));
                    m.put("numeroDiario", rs.getString("numero_diario"));
                    m.put("statusDiario", rs.getString("status_diario"));
                    m.put("data", rs.getDate("data_processamento").toLocalDate().toString());
                    m.put("quantidade", rs.getBigDecimal("quantidade"));
                    m.put("totalCaixas", rs.getBigDecimal("total_caixas"));
                    m.put("valorEmbalagem", rs.getBigDecimal("valor_embalagem"));
                    out.add(m);
                }
            }
        }
        return out;
    }

    private ColheitaLote carregarLote(Connection c, int idLote, boolean travar) throws SQLException {
        try (PreparedStatement st = c.prepareStatement(SELECT_LOTE + " WHERE cl.idlote=?" + (travar ? " FOR UPDATE OF cl" : ""))) {
            st.setInt(1, idLote);
            try (ResultSet rs = st.executeQuery()) {
                if (!rs.next()) throw new IllegalArgumentException("Lote não encontrado.");
                return mapear(rs);
            }
        }
    }

    private void validar(ColheitaLote l, LocalDate data, BigDecimal quantidade, List<LinhaEmbalagem> embalagens) {
        if ("FINALIZADO".equals(l.getStatus())) {
            throw new IllegalArgumentException("Lote finalizado não pode receber processamento.");
        }
        if (data == null) throw new IllegalArgumentException("Informe a data do processamento.");
        if (l.getDataColheita() != null && data.isBefore(l.getDataColheita())) {
            throw new IllegalArgumentException("A data do processamento não pode ser anterior à colheita do lote.");
        }
        if (quantidade == null || quantidade.signum() <= 0) {
            throw new IllegalArgumentException("Quantidade a processar deve ser maior que zero.");
        }
        BigDecimal saldo = l.getQuantidadeTotal().subtract(l.getQuantidadeProcessada());
        if (quantidade.compareTo(saldo) > 0) {
            throw new IllegalArgumentException(
                "Quantidade maior que o saldo em estoque (" + saldo.setScale(2, RoundingMode.HALF_UP) + ").");
        }
        if (l.getIdArea() == null || l.getIdCultura() == null) {
            throw new IllegalArgumentException("O lote não tem talhão/área ou alimento definidos — não é possível processar.");
        }
        Map<String, BigDecimal> precoPorTipo = new LinkedHashMap<>();
        for (LinhaEmbalagem e : embalagens) {
            if (e.idPessoa <= 0) throw new IllegalArgumentException("Escolha o embalador de cada linha de embalagem.");
            String tipo = DespesasCustosDAO.normalizarTipoCaixa(e.tipoCaixa);
            if (tipo == null) throw new IllegalArgumentException("Escolha o tipo de embalagem de cada embalador.");
            if (e.caixas == null || e.caixas.signum() <= 0) {
                throw new IllegalArgumentException("Informe o nº de caixas (maior que zero) de cada embalador.");
            }
            if (e.preco == null || e.preco.signum() <= 0) {
                throw new IllegalArgumentException("Informe o preço da embalagem (maior que zero).");
            }
            BigDecimal anterior = precoPorTipo.putIfAbsent(tipo, e.preco);
            if (anterior != null && anterior.compareTo(e.preco) != 0) {
                throw new IllegalArgumentException("A embalagem " + tipo + " está com preços diferentes em duas linhas — use o mesmo preço.");
            }
        }
    }

    private void validarDiarioProcessamento(Connection c, int idDiario, ColheitaLote l) throws SQLException {
        try (PreparedStatement st = c.prepareStatement("SELECT d.id_quadra, t.nome FROM diario_campo d "
                + "LEFT JOIN diario_tipo_atividade t ON t.idtipo = d.id_tipo_atividade WHERE d.iddiario=?")) {
            st.setInt(1, idDiario);
            try (ResultSet rs = st.executeQuery()) {
                if (!rs.next()) throw new IllegalArgumentException("Diário de Processamento não encontrado.");
                Integer quadra = (Integer) rs.getObject(1);
                String nome = rs.getString(2);
                if (nome == null || !nome.toLowerCase().contains("processa")) {
                    throw new IllegalArgumentException("O diário informado não é de Processamento.");
                }
                if (quadra == null || !quadra.equals(l.getIdQuadra())) {
                    throw new IllegalArgumentException("O diário de Processamento não é do talhão do lote.");
                }
            }
        }
        try (PreparedStatement st = c.prepareStatement("SELECT 1 FROM colheita_processamento WHERE iddiario=?")) {
            st.setInt(1, idDiario);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) throw new IllegalArgumentException("Este diário de Processamento já está ligado a um lote.");
            }
        }
    }

    /** Soma a quantidade ao processado e ajusta o status (PROCESSADO ao esgotar o saldo, senão ESTOQUE). */
    private void aplicarBaixa(Connection c, ColheitaLote l, BigDecimal quantidade) throws SQLException {
        BigDecimal novaProcessada = l.getQuantidadeProcessada().add(quantidade);
        String novoStatus = novaProcessada.compareTo(l.getQuantidadeTotal()) >= 0 ? "PROCESSADO" : "ESTOQUE";
        try (PreparedStatement st = c.prepareStatement(
                "UPDATE colheita_lote SET quantidade_processada=?, status=?, atualizado_em=now() WHERE idlote=?")) {
            st.setBigDecimal(1, novaProcessada); st.setString(2, novoStatus); st.setInt(3, l.getIdLote());
            st.executeUpdate();
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
        l.setIdArea((Integer) rs.getObject("id_area")); l.setAreaNome(rs.getString("area_nome"));
        Date d = rs.getDate("data_colheita"); l.setDataColheita(d != null ? d.toLocalDate() : null);
        l.setQuantidadeTotal(rs.getBigDecimal("quantidade_total"));
        l.setQuantidadeProcessada(rs.getBigDecimal("quantidade_processada"));
        l.setUnidade(rs.getString("unidade")); l.setStatus(rs.getString("status"));
        l.setObservacoes(rs.getString("observacoes"));
        return l;
    }
}
