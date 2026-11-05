package Model.Dao;

import Model.Model.DespesasCustos;
import Model.Model.Romaneio;
import Model.Model.RomaneioDespesa;
import Model.Model.RomaneioItem;
import Model.Model.RomaneioPagamento;
import Util.PostgresConnection;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Módulo Romaneio (V33): venda das caixas embaladas de um lote de colheita a um parceiro comprador,
 * despesas de venda (snapshot) e recebimentos em grupo por parceiro.
 *
 * Regras principais:
 *   - um lote pode ter vários romaneios; por embalagem, Σ caixas dos romaneios não cancelados ≤ caixas
 *     embaladas do lote (colheita_processamento_embalagem). O lote fica travado (FOR UPDATE) na gravação;
 *   - situação: ABERTO (falta preço) → PENDENTE (todos com preço) → FINALIZADO (líquido pago);
 *   - valor_bruto, total_despesas, valor_liquido e valor_pago são sempre recalculados aqui e gravados
 *     (a Safra soma esses campos).
 * As regras lançam IllegalArgumentException com a mensagem para o usuário; por isso as transações são
 * manuais (TransacaoUtil só faz rollback de SQLException).
 */
public class RomaneioDAO {

    private static final BigDecimal CEM = new BigDecimal("100");
    private static final DateTimeFormatter BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final long LOCK_NUMERO = 733001L;

    private static final String SELECT_ROMANEIO =
            "SELECT r.*, p.nomepessoa AS nome_parceiro, ar.propriedadeareaproducao AS nome_area, "
          + "ar.siglasareaproducao AS sigla_area, q.nome_quadra, al.nomeproduto AS nome_alimento, "
          + "s.nome AS nome_safra, d.numero_diario, cl.data_colheita "
          + "FROM romaneio r "
          + "LEFT JOIN parceiro p ON p.idpessoa = r.id_parceiro "
          + "LEFT JOIN areaproducao ar ON ar.idareaproducao = r.id_area "
          + "LEFT JOIN quadra q ON q.idquadra = r.id_quadra "
          + "LEFT JOIN alimento al ON al.idproduto = r.id_alimento "
          + "LEFT JOIN safra s ON s.idsafra = r.id_safra "
          + "JOIN colheita_lote cl ON cl.idlote = r.idlote "
          + "LEFT JOIN diario_campo d ON d.iddiario = cl.iddiario";

    /* ============================== CONSULTAS ============================== */

    public List<Romaneio> listar(String status) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT_ROMANEIO + " WHERE 1=1");
        if (status != null && !status.isBlank()) sql.append(" AND r.status=?");
        sql.append(" ORDER BY r.data_emissao DESC, r.idromaneio DESC");
        List<Romaneio> out = new ArrayList<>();
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement st = c.prepareStatement(sql.toString())) {
            if (status != null && !status.isBlank()) st.setString(1, status.trim().toUpperCase());
            try (ResultSet rs = st.executeQuery()) { while (rs.next()) out.add(mapear(rs)); }
        }
        return out;
    }

    /** Romaneio com itens e despesas, ou null. */
    public Romaneio buscar(int id) throws SQLException {
        try (Connection c = new PostgresConnection().getConnection()) { return buscar(c, id, false); }
    }

    private Romaneio buscar(Connection c, int id, boolean travar) throws SQLException {
        Romaneio r;
        try (PreparedStatement st = c.prepareStatement(SELECT_ROMANEIO + " WHERE r.idromaneio=?" + (travar ? " FOR UPDATE OF r" : ""))) {
            st.setInt(1, id);
            try (ResultSet rs = st.executeQuery()) { if (!rs.next()) return null; r = mapear(rs); }
        }
        try (PreparedStatement st = c.prepareStatement(
                "SELECT ri.*, cf.classificacao AS nome_classificacao, e.descricao AS descricao_embalagem "
              + "FROM romaneio_item ri JOIN classificacao cf ON cf.idclassificacao = ri.idclassificacao "
              + "JOIN embalagem e ON e.idembalagem = ri.idembalagem WHERE ri.idromaneio=? ORDER BY ri.id")) {
            st.setInt(1, id);
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    RomaneioItem i = new RomaneioItem();
                    i.setId(rs.getInt("id"));
                    i.setIdClassificacao(rs.getInt("idclassificacao")); i.setNomeClassificacao(rs.getString("nome_classificacao"));
                    i.setIdEmbalagem(rs.getInt("idembalagem")); i.setDescricaoEmbalagem(rs.getString("descricao_embalagem"));
                    i.setCaixas(rs.getBigDecimal("caixas")); i.setPrecoUnitario(rs.getBigDecimal("preco_unitario"));
                    i.setValor(rs.getBigDecimal("valor"));
                    r.getItens().add(i);
                }
            }
        }
        try (PreparedStatement st = c.prepareStatement("SELECT * FROM romaneio_despesa WHERE idromaneio=? ORDER BY id")) {
            st.setInt(1, id);
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    RomaneioDespesa d = new RomaneioDespesa();
                    d.setId(rs.getInt("id")); d.setIdDespesasCusto((Integer) rs.getObject("iddespesascusto"));
                    d.setDescricao(rs.getString("descricao")); d.setFormaCalculo(rs.getString("forma_calculo"));
                    d.setPercentual(rs.getBigDecimal("percentual")); d.setQuantidade(rs.getBigDecimal("quantidade"));
                    d.setValorUnitario(rs.getBigDecimal("valor_unitario")); d.setValor(rs.getBigDecimal("valor"));
                    r.getDespesas().add(d);
                }
            }
        }
        return r;
    }

    /**
     * Lotes que ainda podem receber romaneio (botão "Romaneio Cad."): PROCESSADO, com caixas embaladas
     * e com caixas ainda não romaneadas em alguma embalagem.
     */
    public List<Map<String, Object>> lotesDisponiveis() throws SQLException {
        String sql = "WITH emb AS (" + SQL_EMBALADAS_POR_LOTE + "), rom AS (" + SQL_ROMANEADAS_POR_LOTE + ") "
                + "SELECT cl.idlote, d.numero_diario, cl.data_colheita, q.nome_quadra, al.nomeproduto, "
                + "ar.propriedadeareaproducao, ar.siglasareaproducao, SUM(emb.caixas) AS embaladas, "
                + "SUM(COALESCE(rom.caixas,0)) AS romaneadas "
                + "FROM colheita_lote cl JOIN emb ON emb.idlote = cl.idlote "
                + "LEFT JOIN rom ON rom.idlote = emb.idlote AND rom.idembalagem = emb.idembalagem "
                + "JOIN diario_campo d ON d.iddiario = cl.iddiario "
                + "LEFT JOIN quadra q ON q.idquadra = cl.id_quadra "
                + "LEFT JOIN areaproducao ar ON ar.idareaproducao = q.idareaproducao "
                + "LEFT JOIN alimento al ON al.idproduto = cl.id_cultura "
                + "WHERE cl.status = 'PROCESSADO' "
                + "GROUP BY cl.idlote, d.numero_diario, cl.data_colheita, q.nome_quadra, al.nomeproduto, "
                + "ar.propriedadeareaproducao, ar.siglasareaproducao "
                + "HAVING SUM(GREATEST(emb.caixas - COALESCE(rom.caixas,0), 0)) > 0 "
                + "ORDER BY cl.data_colheita DESC, cl.idlote DESC";
        List<Map<String, Object>> out = new ArrayList<>();
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement st = c.prepareStatement(sql);
             ResultSet rs = st.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("idLote", rs.getInt("idlote"));
                m.put("numeroDiario", rs.getString("numero_diario"));
                m.put("dataColheita", rs.getDate("data_colheita").toLocalDate().toString());
                m.put("nomeQuadra", rs.getString("nome_quadra"));
                m.put("nomeAlimento", rs.getString("nomeproduto"));
                m.put("nomeArea", rs.getString("propriedadeareaproducao"));
                m.put("siglaArea", rs.getString("siglasareaproducao"));
                m.put("embaladas", rs.getBigDecimal("embaladas"));
                m.put("romaneadas", rs.getBigDecimal("romaneadas"));
                out.add(m);
            }
        }
        return out;
    }

    private static final String SQL_EMBALADAS_POR_LOTE =
            "SELECT p.idlote, pe.idembalagem, SUM(pe.quantidade_caixas) AS caixas "
          + "FROM colheita_processamento p JOIN colheita_processamento_embalagem pe ON pe.idprocessamento = p.idprocessamento "
          + "WHERE pe.idembalagem IS NOT NULL GROUP BY p.idlote, pe.idembalagem";
    private static final String SQL_ROMANEADAS_POR_LOTE =
            "SELECT r.idlote, ri.idembalagem, SUM(ri.caixas) AS caixas "
          + "FROM romaneio r JOIN romaneio_item ri ON ri.idromaneio = r.idromaneio "
          + "WHERE r.status <> 'CANCELADO' GROUP BY r.idlote, ri.idembalagem";

    /**
     * Tudo que a tela MR1 (cadastroRomaneio.jsp) precisa: lote (área, talhão, alimento), safra resolvida,
     * parceiros PARCEIRO, classificações do alimento, embalagens embaladas do lote com o saldo (descontando
     * o próprio romaneio na edição), despesas de venda do cadastro e, na edição, o romaneio.
     */
    public Map<String, Object> contexto(Integer idLote, Integer idRomaneio) throws SQLException {
        Map<String, Object> out = new LinkedHashMap<>();
        try (Connection c = new PostgresConnection().getConnection()) {
            Romaneio r = null;
            if (idRomaneio != null) {
                r = buscar(c, idRomaneio, false);
                if (r == null) throw new IllegalArgumentException("Romaneio não encontrado.");
                idLote = r.getIdLote();
            }
            if (idLote == null) throw new IllegalArgumentException("Informe o lote ou o romaneio.");
            Map<String, Object> lote = carregarLote(c, idLote, false);
            out.put("lote", lote);
            String[] sf = new SafraDAO().resolverSafra((Integer) lote.get("idQuadra"), (LocalDate) lote.get("dataColheitaData"));
            lote.remove("dataColheitaData");
            if (sf != null) {
                Map<String, Object> s = new LinkedHashMap<>();
                s.put("idSafra", Integer.parseInt(sf[0])); s.put("nome", sf[1]); s.put("status", sf[2]);
                out.put("safra", s);
            }
            out.put("parceiros", listarParceiros(c));
            out.put("classificacoes", classificacoes(c, (Integer) lote.get("idAlimento")));
            List<Map<String, Object>> embs = new ArrayList<>();
            Map<Integer, BigDecimal> embaladas = caixasEmbaladas(c, idLote);
            Map<Integer, BigDecimal> outros = caixasRomaneadas(c, idLote, idRomaneio == null ? 0 : idRomaneio);
            EmbalagemDAO embDAO = new EmbalagemDAO();
            for (Map.Entry<Integer, BigDecimal> e : embaladas.entrySet()) {
                Model.Model.Embalagem emb = embDAO.buscar(c, e.getKey());
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("idEmbalagem", e.getKey());
                m.put("descricao", emb.getDescricao());
                m.put("pesoKg", emb.getPesoKg());
                m.put("embaladas", e.getValue());
                m.put("outrosRomaneios", outros.getOrDefault(e.getKey(), BigDecimal.ZERO));
                embs.add(m);
            }
            embs.sort(Comparator.comparing(m -> (BigDecimal) m.get("pesoKg")));
            out.put("embalagens", embs);
            DespesasCustosDAO dDAO = new DespesasCustosDAO();
            out.put("despesasRomaneio", dDAO.listarRomaneio(c));
            List<DespesasCustos> todas = new ArrayList<>();
            for (DespesasCustos d : dDAO.listAll()) if (d.getCategoria() == null || "GERAL".equals(d.getCategoria())) todas.add(d);
            out.put("despesasCadastro", todas);
            if (r != null) out.put("romaneio", r);
        }
        return out;
    }

    /** Parceiros compradores (tipo PARCEIRO, ativos) com a quantidade e o saldo dos romaneios PENDENTES. */
    public List<Map<String, Object>> listarParceiros() throws SQLException {
        try (Connection c = new PostgresConnection().getConnection()) { return listarParceiros(c); }
    }

    private List<Map<String, Object>> listarParceiros(Connection c) throws SQLException {
        String sql = "SELECT p.idpessoa, p.nomepessoa, p.razaosocialpessoacnpj, "
                + "COUNT(r.idromaneio) AS qtd, COALESCE(SUM(r.valor_liquido - r.valor_pago),0) AS saldo "
                + "FROM parceiro p LEFT JOIN romaneio r ON r.id_parceiro = p.idpessoa AND r.status = 'PENDENTE' "
                + "WHERE p.tipo_parceiro = 'PARCEIRO' AND COALESCE(p.situacaopessoa, true) "
                + "GROUP BY p.idpessoa, p.nomepessoa, p.razaosocialpessoacnpj ORDER BY p.nomepessoa";
        List<Map<String, Object>> out = new ArrayList<>();
        try (PreparedStatement st = c.prepareStatement(sql); ResultSet rs = st.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("idParceiro", rs.getInt("idpessoa"));
                m.put("nome", rs.getString("nomepessoa"));
                m.put("razaoSocial", rs.getString("razaosocialpessoacnpj"));
                m.put("qtdPendentes", rs.getInt("qtd"));
                m.put("saldoPendente", rs.getBigDecimal("saldo"));
                out.add(m);
            }
        }
        return out;
    }

    /** Romaneios PENDENTES do parceiro, do mais antigo para o mais novo (ordem da distribuição do pagamento). */
    public List<Romaneio> pendentesDoParceiro(int idParceiro) throws SQLException {
        List<Romaneio> out = new ArrayList<>();
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement st = c.prepareStatement(SELECT_ROMANEIO
                     + " WHERE r.id_parceiro=? AND r.status='PENDENTE' ORDER BY r.data_emissao, r.idromaneio")) {
            st.setInt(1, idParceiro);
            try (ResultSet rs = st.executeQuery()) { while (rs.next()) out.add(mapear(rs)); }
        }
        return out;
    }

    /** Histórico de pagamentos de um romaneio (com o quanto de cada pagamento foi para ele). */
    public List<RomaneioPagamento> pagamentosDoRomaneio(int idRomaneio) throws SQLException {
        String sql = "SELECT pg.*, pa.nomepessoa, pi.valor AS valor_no_romaneio FROM romaneio_pagamento_item pi "
                + "JOIN romaneio_pagamento pg ON pg.idpagamento = pi.idpagamento "
                + "LEFT JOIN parceiro pa ON pa.idpessoa = pg.id_parceiro "
                + "WHERE pi.idromaneio=? ORDER BY pg.data_pagamento, pg.idpagamento";
        List<RomaneioPagamento> out = new ArrayList<>();
        try (Connection c = new PostgresConnection().getConnection()) {
            try (PreparedStatement st = c.prepareStatement(sql)) {
                st.setInt(1, idRomaneio);
                try (ResultSet rs = st.executeQuery()) {
                    while (rs.next()) {
                        RomaneioPagamento p = new RomaneioPagamento();
                        p.setIdPagamento(rs.getInt("idpagamento")); p.setIdParceiro(rs.getInt("id_parceiro"));
                        p.setNomeParceiro(rs.getString("nomepessoa"));
                        p.setDataPagamento(rs.getDate("data_pagamento").toLocalDate());
                        p.setForma(rs.getString("forma")); p.setValor(rs.getBigDecimal("valor"));
                        p.setBanco(rs.getString("banco")); p.setNumeroCheque(rs.getString("numero_cheque"));
                        p.setEmitente(rs.getString("emitente"));
                        Date bp = rs.getDate("bom_para"); p.setBomPara(bp == null ? null : bp.toLocalDate());
                        p.setObservacao(rs.getString("observacao"));
                        p.setValorNoRomaneio(rs.getBigDecimal("valor_no_romaneio"));
                        out.add(p);
                    }
                }
            }
            // Romaneios que dividiram cada pagamento (mostra que o estorno desfaz todos).
            try (PreparedStatement st = c.prepareStatement("SELECT pi.idromaneio, r.numero, pi.valor FROM romaneio_pagamento_item pi "
                    + "JOIN romaneio r ON r.idromaneio = pi.idromaneio WHERE pi.idpagamento=? ORDER BY r.numero")) {
                for (RomaneioPagamento p : out) {
                    st.setInt(1, p.getIdPagamento());
                    try (ResultSet rs = st.executeQuery()) {
                        while (rs.next()) {
                            RomaneioPagamento.Item i = new RomaneioPagamento.Item(rs.getInt(1), rs.getBigDecimal(3));
                            i.numero = rs.getString(2);
                            p.getItens().add(i);
                        }
                    }
                }
            }
        }
        return out;
    }

    /* ============================== GRAVAÇÃO ============================== */

    /**
     * Cria (idRomaneio = 0) ou atualiza o romaneio com itens e despesas, recalculando tudo no servidor:
     * itens (caixas × preço), despesas por forma de cálculo, bruto,
     * líquido e situação. Devolve o romaneio gravado.
     */
    public Romaneio salvar(Romaneio in) throws SQLException {
        try (Connection c = new PostgresConnection().getConnection()) {
            c.setAutoCommit(false);
            try {
                Map<String, Object> lote = carregarLote(c, in.getIdLote(), true);
                String stLote = (String) lote.get("status");
                if (!"PROCESSADO".equals(stLote) && !"FINALIZADO".equals(stLote)) {
                    throw new IllegalArgumentException("Só é possível emitir romaneio de lote Processado ou Finalizado.");
                }
                Romaneio atual = null;
                if (in.getIdRomaneio() > 0) {
                    atual = buscar(c, in.getIdRomaneio(), true);
                    if (atual == null) throw new IllegalArgumentException("Romaneio não encontrado.");
                    if (atual.getIdLote() != in.getIdLote()) throw new IllegalArgumentException("O romaneio não é deste lote.");
                    if (Romaneio.FINALIZADO.equals(atual.getStatus())) throw new IllegalArgumentException("Romaneio finalizado não pode ser alterado.");
                    if (Romaneio.CANCELADO.equals(atual.getStatus())) throw new IllegalArgumentException("Romaneio cancelado não pode ser alterado.");
                }
                BigDecimal pago = atual == null ? BigDecimal.ZERO : atual.getValorPago();

                validarParceiro(c, in.getIdParceiro());
                if (atual != null && pago.signum() > 0 && atual.getIdParceiro() != in.getIdParceiro()) {
                    throw new IllegalArgumentException("O romaneio já tem pagamento: não é possível trocar o parceiro.");
                }
                LocalDate dataColheita = (LocalDate) lote.get("dataColheitaData");
                if (in.getDataEmissao() == null) throw new IllegalArgumentException("Informe a data de emissão.");
                if (dataColheita != null && in.getDataEmissao().isBefore(dataColheita)) {
                    throw new IllegalArgumentException("A data de emissão não pode ser anterior à colheita do lote ("
                            + dataColheita.format(BR) + ").");
                }
                in.setMotorista(limpar(in.getMotorista(), 80, "Motorista"));
                in.setPlaca(placa(in.getPlaca(), "Placa do veículo"));
                in.setObservacao(limpar(in.getObservacao(), 300, "Observação"));
                validarTransporte(in);

                // Itens: classificação do alimento, embalagem embalada no lote, caixas > 0, sem repetir o par.
                Integer idAlimento = (Integer) lote.get("idAlimento");
                Set<Integer> classesOk = new HashSet<>();
                for (Map<String, Object> m : classificacoes(c, idAlimento)) classesOk.add((Integer) m.get("idClassificacao"));
                Map<Integer, BigDecimal> embaladas = caixasEmbaladas(c, in.getIdLote());
                Map<Integer, BigDecimal> outros = caixasRomaneadas(c, in.getIdLote(), in.getIdRomaneio());
                Map<Integer, BigDecimal> deste = new LinkedHashMap<>();
                Set<String> pares = new HashSet<>();
                if (in.getItens().isEmpty()) throw new IllegalArgumentException("Informe as caixas de pelo menos uma classificação.");
                boolean todosComPreco = true;
                BigDecimal totalCaixas = BigDecimal.ZERO, bruto = BigDecimal.ZERO;
                for (RomaneioItem i : in.getItens()) {
                    if (!classesOk.contains(i.getIdClassificacao())) throw new IllegalArgumentException("Classificação inválida para o alimento do lote.");
                    if (!embaladas.containsKey(i.getIdEmbalagem())) throw new IllegalArgumentException("Embalagem sem caixas embaladas neste lote.");
                    if (i.getCaixas() == null || i.getCaixas().signum() <= 0) throw new IllegalArgumentException("O nº de caixas de cada item deve ser maior que zero.");
                    if (!pares.add(i.getIdClassificacao() + ":" + i.getIdEmbalagem())) throw new IllegalArgumentException("A mesma classificação e embalagem aparecem duas vezes.");
                    if (i.getPrecoUnitario() != null && i.getPrecoUnitario().signum() < 0) throw new IllegalArgumentException("O preço unitário não pode ser negativo.");
                    if (i.getPrecoUnitario() == null) todosComPreco = false;
                    i.setValor(i.getPrecoUnitario() == null ? BigDecimal.ZERO : i.getCaixas().multiply(i.getPrecoUnitario()).setScale(2, RoundingMode.HALF_UP));
                    deste.merge(i.getIdEmbalagem(), i.getCaixas(), BigDecimal::add);
                    totalCaixas = totalCaixas.add(i.getCaixas());
                    bruto = bruto.add(i.getValor());
                }
                EmbalagemDAO embDAO = new EmbalagemDAO();
                for (Map.Entry<Integer, BigDecimal> e : deste.entrySet()) {
                    Model.Model.Embalagem emb = embDAO.buscar(c, e.getKey());
                    BigDecimal saldo = embaladas.get(e.getKey()).subtract(outros.getOrDefault(e.getKey(), BigDecimal.ZERO));
                    if (e.getValue().compareTo(saldo) > 0) {
                        throw new IllegalArgumentException("A embalagem " + emb.getDescricao() + " tem só " + fmt(saldo.max(BigDecimal.ZERO))
                                + " caixa(s) disponível(is) neste lote (informado: " + fmt(e.getValue()) + ").");
                    }
                }

                // Despesas: recalculadas pela forma. Desde 02/11 o custo da embalagem é custo de PRODUÇÃO
                // (insumo com estoque, lançado no diário de Processamento): a antiga linha automática
                // EMBALAGEM não é mais gerada e, se vier de um romaneio antigo, é descartada aqui.
                List<RomaneioDespesa> despesas = new ArrayList<>();
                BigDecimal totalDesp = BigDecimal.ZERO;
                for (RomaneioDespesa d : in.getDespesas()) {
                    if (RomaneioDespesa.EMBALAGEM.equals(d.getFormaCalculo())) continue;
                    String desc = d.getDescricao() == null ? "" : d.getDescricao().trim();
                    if (desc.isEmpty()) throw new IllegalArgumentException("Informe a descrição de cada despesa.");
                    if (desc.length() > 100) desc = desc.substring(0, 100);
                    d.setDescricao(desc);
                    String f = d.getFormaCalculo() == null ? RomaneioDespesa.FIXO : d.getFormaCalculo();
                    d.setFormaCalculo(f);
                    switch (f) {
                        case RomaneioDespesa.PERCENTUAL_VENDA: {
                            BigDecimal pct = d.getPercentual() == null ? BigDecimal.ZERO : d.getPercentual();
                            if (pct.signum() < 0 || pct.compareTo(CEM) > 0) throw new IllegalArgumentException("O percentual de " + desc + " deve estar entre 0 e 100.");
                            d.setPercentual(pct); d.setQuantidade(null); d.setValorUnitario(null);
                            d.setValor(bruto.multiply(pct).divide(CEM, 2, RoundingMode.HALF_UP));
                            break;
                        }
                        case RomaneioDespesa.POR_CAIXA: {
                            BigDecimal vu = d.getValorUnitario() == null ? BigDecimal.ZERO : d.getValorUnitario();
                            if (vu.signum() < 0) throw new IllegalArgumentException("O valor por caixa de " + desc + " não pode ser negativo.");
                            d.setValorUnitario(vu); d.setQuantidade(totalCaixas); d.setPercentual(null);
                            d.setValor(totalCaixas.multiply(vu).setScale(2, RoundingMode.HALF_UP));
                            break;
                        }
                        case RomaneioDespesa.FIXO: {
                            BigDecimal v = d.getValor() == null ? BigDecimal.ZERO : d.getValor();
                            if (v.signum() < 0) throw new IllegalArgumentException("O valor de " + desc + " não pode ser negativo.");
                            d.setValor(v.setScale(2, RoundingMode.HALF_UP)); d.setPercentual(null); d.setQuantidade(null); d.setValorUnitario(null);
                            break;
                        }
                        default: throw new IllegalArgumentException("Forma de cálculo inválida na despesa " + desc + ".");
                    }
                    despesas.add(d);
                    totalDesp = totalDesp.add(d.getValor());
                }
                BigDecimal liquido = bruto.subtract(totalDesp);

                String status = todosComPreco ? Romaneio.PENDENTE : Romaneio.ABERTO;
                if (todosComPreco && liquido.signum() <= 0) {
                    throw new IllegalArgumentException("O valor líquido (" + fmtMoeda(liquido) + ") precisa ser maior que zero: as despesas passam da venda bruta.");
                }
                if (pago.signum() > 0) {
                    if (!todosComPreco) throw new IllegalArgumentException("O romaneio já tem pagamento: todos os itens precisam de preço.");
                    if (liquido.compareTo(pago) < 0) {
                        throw new IllegalArgumentException("O valor líquido (" + fmtMoeda(liquido) + ") ficaria menor que o já pago ("
                                + fmtMoeda(pago) + "). Estorne o pagamento antes de reduzir o romaneio.");
                    }
                    if (liquido.compareTo(pago) == 0) status = Romaneio.FINALIZADO;
                }

                String[] sf = new SafraDAO().resolverSafra((Integer) lote.get("idQuadra"), dataColheita);
                if (sf == null) {
                    throw new IllegalArgumentException("Não há Safra que inclua o talhão " + lote.get("nomeQuadra") + " na data da colheita ("
                            + dataColheita.format(BR) + "). Cadastre ou ajuste a safra antes de emitir o romaneio.");
                }

                int id;
                if (atual == null) {
                    String numero = proximoNumero(c, in.getDataEmissao().getYear());
                    try (PreparedStatement st = c.prepareStatement("INSERT INTO romaneio (numero, idlote, id_area, id_quadra, id_alimento, "
                            + "id_safra, id_parceiro, data_emissao, motorista, placa, status, total_caixas, valor_bruto, total_despesas, "
                            + "valor_liquido, valor_pago, observacao) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,0,?) RETURNING idromaneio")) {
                        st.setString(1, numero); st.setInt(2, in.getIdLote());
                        st.setInt(3, (Integer) lote.get("idArea")); st.setInt(4, (Integer) lote.get("idQuadra"));
                        st.setInt(5, idAlimento); st.setInt(6, Integer.parseInt(sf[0])); st.setInt(7, in.getIdParceiro());
                        st.setDate(8, Date.valueOf(in.getDataEmissao())); st.setString(9, in.getMotorista()); st.setString(10, in.getPlaca());
                        st.setString(11, status); st.setBigDecimal(12, totalCaixas); st.setBigDecimal(13, bruto);
                        st.setBigDecimal(14, totalDesp); st.setBigDecimal(15, liquido); st.setString(16, in.getObservacao());
                        try (ResultSet rs = st.executeQuery()) { rs.next(); id = rs.getInt(1); }
                    }
                } else {
                    id = atual.getIdRomaneio();
                    try (PreparedStatement st = c.prepareStatement("UPDATE romaneio SET id_safra=?, id_parceiro=?, data_emissao=?, motorista=?, "
                            + "placa=?, status=?, total_caixas=?, valor_bruto=?, total_despesas=?, valor_liquido=?, observacao=?, "
                            + "atualizado_em=now() WHERE idromaneio=?")) {
                        st.setInt(1, Integer.parseInt(sf[0])); st.setInt(2, in.getIdParceiro());
                        st.setDate(3, Date.valueOf(in.getDataEmissao())); st.setString(4, in.getMotorista()); st.setString(5, in.getPlaca());
                        st.setString(6, status); st.setBigDecimal(7, totalCaixas); st.setBigDecimal(8, bruto);
                        st.setBigDecimal(9, totalDesp); st.setBigDecimal(10, liquido); st.setString(11, in.getObservacao());
                        st.setInt(12, id);
                        st.executeUpdate();
                    }
                    try (PreparedStatement st = c.prepareStatement("DELETE FROM romaneio_item WHERE idromaneio=?")) { st.setInt(1, id); st.executeUpdate(); }
                    try (PreparedStatement st = c.prepareStatement("DELETE FROM romaneio_despesa WHERE idromaneio=?")) { st.setInt(1, id); st.executeUpdate(); }
                }
                try (PreparedStatement st = c.prepareStatement("UPDATE romaneio SET nota_fiscal=?, serie_nf=?, transportadora=?, "
                        + "transportadora_doc=?, motorista_cpf=?, motorista_cnh=?, placa_carreta=?, uf_veiculo=?, tipo_veiculo=? WHERE idromaneio=?")) {
                    st.setString(1, in.getNotaFiscal()); st.setString(2, in.getSerieNf()); st.setString(3, in.getTransportadora());
                    st.setString(4, in.getTransportadoraDoc()); st.setString(5, in.getMotoristaCpf()); st.setString(6, in.getMotoristaCnh());
                    st.setString(7, in.getPlacaCarreta()); st.setString(8, in.getUfVeiculo()); st.setString(9, in.getTipoVeiculo());
                    st.setInt(10, id);
                    st.executeUpdate();
                }
                try (PreparedStatement st = c.prepareStatement("INSERT INTO romaneio_item (idromaneio, idclassificacao, idembalagem, caixas, "
                        + "preco_unitario, valor) VALUES (?,?,?,?,?,?)")) {
                    for (RomaneioItem i : in.getItens()) {
                        st.setInt(1, id); st.setInt(2, i.getIdClassificacao()); st.setInt(3, i.getIdEmbalagem());
                        st.setBigDecimal(4, i.getCaixas()); st.setBigDecimal(5, i.getPrecoUnitario()); st.setBigDecimal(6, i.getValor());
                        st.addBatch();
                    }
                    st.executeBatch();
                }
                try (PreparedStatement st = c.prepareStatement("INSERT INTO romaneio_despesa (idromaneio, iddespesascusto, descricao, "
                        + "forma_calculo, percentual, quantidade, valor_unitario, valor) VALUES (?,?,?,?,?,?,?,?)")) {
                    for (RomaneioDespesa d : despesas) {
                        st.setInt(1, id); st.setObject(2, d.getIdDespesasCusto(), Types.INTEGER); st.setString(3, d.getDescricao());
                        st.setString(4, d.getFormaCalculo()); st.setBigDecimal(5, d.getPercentual()); st.setBigDecimal(6, d.getQuantidade());
                        st.setBigDecimal(7, d.getValorUnitario()); st.setBigDecimal(8, d.getValor());
                        st.addBatch();
                    }
                    st.executeBatch();
                }
                Romaneio salvo = buscar(c, id, false);
                c.commit();
                return salvo;
            } catch (SQLException | RuntimeException e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        }
    }

    /** Cancela (libera as caixas do lote). Só ABERTO/PENDENTE e sem pagamento. */
    public void cancelar(int id) throws SQLException {
        try (Connection c = new PostgresConnection().getConnection()) {
            c.setAutoCommit(false);
            try {
                Romaneio r = buscar(c, id, true);
                if (r == null) throw new IllegalArgumentException("Romaneio não encontrado.");
                if (Romaneio.CANCELADO.equals(r.getStatus())) throw new IllegalArgumentException("O romaneio já está cancelado.");
                if (Romaneio.FINALIZADO.equals(r.getStatus())) throw new IllegalArgumentException("Romaneio finalizado não pode ser cancelado.");
                if (r.getValorPago().signum() > 0) throw new IllegalArgumentException("O romaneio tem pagamento: estorne os pagamentos antes de cancelar.");
                try (PreparedStatement st = c.prepareStatement("UPDATE romaneio SET status='CANCELADO', atualizado_em=now() WHERE idromaneio=?")) {
                    st.setInt(1, id); st.executeUpdate();
                }
                c.commit();
            } catch (SQLException | RuntimeException e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        }
    }

    /**
     * Pagamento em grupo: grava o recebimento e a distribuição entre romaneios PENDENTES do parceiro
     * (cada parte ≤ saldo do romaneio; Σ partes = valor). Romaneio com saldo zerado vira FINALIZADO.
     */
    public int registrarPagamento(RomaneioPagamento p) throws SQLException {
        if (p.getIdParceiro() <= 0) throw new IllegalArgumentException("Escolha o parceiro.");
        if (p.getDataPagamento() == null) throw new IllegalArgumentException("Informe a data do pagamento.");
        if (!RomaneioPagamento.isFormaValida(p.getForma())) throw new IllegalArgumentException("Forma de pagamento inválida.");
        if (p.getValor() == null || p.getValor().signum() <= 0) throw new IllegalArgumentException("O valor do pagamento deve ser maior que zero.");
        p.setValor(p.getValor().setScale(2, RoundingMode.HALF_UP));
        boolean cheque = p.getForma().startsWith("CHEQUE");
        if (cheque) {
            p.setBanco(limpar(p.getBanco(), 60, "Banco"));
            p.setNumeroCheque(limpar(p.getNumeroCheque(), 30, "Nº do cheque"));
            if (p.getBanco() == null || p.getNumeroCheque() == null) throw new IllegalArgumentException("Informe o banco e o número do cheque.");
            p.setEmitente(limpar(p.getEmitente(), 80, "Emitente"));
            if ("CHEQUE_TERCEIRO".equals(p.getForma()) && p.getEmitente() == null) throw new IllegalArgumentException("Informe o emitente do cheque de terceiro.");
            if (p.getBomPara() != null && p.getBomPara().isBefore(p.getDataPagamento())) {
                throw new IllegalArgumentException("A data \"bom para\" não pode ser anterior à data do pagamento.");
            }
        } else { p.setBanco(null); p.setNumeroCheque(null); p.setEmitente(null); p.setBomPara(null); }
        p.setObservacao(limpar(p.getObservacao(), 300, "Observação"));
        if (p.getItens().isEmpty()) throw new IllegalArgumentException("Distribua o valor entre pelo menos um romaneio.");

        try (Connection c = new PostgresConnection().getConnection()) {
            c.setAutoCommit(false);
            try {
                validarParceiro(c, p.getIdParceiro());
                BigDecimal soma = BigDecimal.ZERO;
                Set<Integer> vistos = new HashSet<>();
                for (RomaneioPagamento.Item i : p.getItens()) {
                    if (i.valor == null || i.valor.signum() <= 0) throw new IllegalArgumentException("Cada parte da distribuição deve ser maior que zero.");
                    i.valor = i.valor.setScale(2, RoundingMode.HALF_UP);
                    if (!vistos.add(i.idRomaneio)) throw new IllegalArgumentException("O mesmo romaneio aparece duas vezes na distribuição.");
                    Romaneio r = buscar(c, i.idRomaneio, true);
                    if (r == null) throw new IllegalArgumentException("Romaneio não encontrado.");
                    if (r.getIdParceiro() != p.getIdParceiro()) throw new IllegalArgumentException("O romaneio " + r.getNumero() + " não é deste parceiro.");
                    if (!Romaneio.PENDENTE.equals(r.getStatus())) throw new IllegalArgumentException("O romaneio " + r.getNumero() + " não está pendente.");
                    if (i.valor.compareTo(r.getSaldo()) > 0) {
                        throw new IllegalArgumentException("O valor para o romaneio " + r.getNumero() + " (" + fmtMoeda(i.valor)
                                + ") passa do saldo dele (" + fmtMoeda(r.getSaldo()) + ").");
                    }
                    soma = soma.add(i.valor);
                }
                if (soma.compareTo(p.getValor()) != 0) {
                    throw new IllegalArgumentException("A soma distribuída (" + fmtMoeda(soma) + ") deve ser igual ao valor do pagamento ("
                            + fmtMoeda(p.getValor()) + ").");
                }
                int idPag;
                try (PreparedStatement st = c.prepareStatement("INSERT INTO romaneio_pagamento (id_parceiro, data_pagamento, forma, valor, "
                        + "banco, numero_cheque, emitente, bom_para, observacao) VALUES (?,?,?,?,?,?,?,?,?) RETURNING idpagamento")) {
                    st.setInt(1, p.getIdParceiro()); st.setDate(2, Date.valueOf(p.getDataPagamento())); st.setString(3, p.getForma());
                    st.setBigDecimal(4, p.getValor()); st.setString(5, p.getBanco()); st.setString(6, p.getNumeroCheque());
                    st.setString(7, p.getEmitente()); st.setDate(8, p.getBomPara() == null ? null : Date.valueOf(p.getBomPara()));
                    st.setString(9, p.getObservacao());
                    try (ResultSet rs = st.executeQuery()) { rs.next(); idPag = rs.getInt(1); }
                }
                try (PreparedStatement st = c.prepareStatement("INSERT INTO romaneio_pagamento_item (idpagamento, idromaneio, valor) VALUES (?,?,?)")) {
                    for (RomaneioPagamento.Item i : p.getItens()) {
                        st.setInt(1, idPag); st.setInt(2, i.idRomaneio); st.setBigDecimal(3, i.valor); st.addBatch();
                    }
                    st.executeBatch();
                }
                for (RomaneioPagamento.Item i : p.getItens()) recalcularPago(c, i.idRomaneio);
                c.commit();
                return idPag;
            } catch (SQLException | RuntimeException e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        }
    }

    /** Estorna (exclui) o pagamento inteiro; os romaneios dele voltam a PENDENTE com o saldo de volta. */
    public List<String> estornarPagamento(int idPagamento) throws SQLException {
        try (Connection c = new PostgresConnection().getConnection()) {
            c.setAutoCommit(false);
            try {
                List<Integer> ids = new ArrayList<>();
                try (PreparedStatement st = c.prepareStatement("SELECT 1 FROM romaneio_pagamento WHERE idpagamento=? FOR UPDATE")) {
                    st.setInt(1, idPagamento);
                    try (ResultSet rs = st.executeQuery()) { if (!rs.next()) throw new IllegalArgumentException("Pagamento não encontrado."); }
                }
                try (PreparedStatement st = c.prepareStatement("SELECT r.idromaneio FROM romaneio r JOIN romaneio_pagamento_item pi "
                        + "ON pi.idromaneio = r.idromaneio WHERE pi.idpagamento=? ORDER BY r.idromaneio FOR UPDATE OF r")) {
                    st.setInt(1, idPagamento);
                    try (ResultSet rs = st.executeQuery()) { while (rs.next()) ids.add(rs.getInt(1)); }
                }
                try (PreparedStatement st = c.prepareStatement("DELETE FROM romaneio_pagamento WHERE idpagamento=?")) {
                    st.setInt(1, idPagamento); st.executeUpdate();
                }
                List<String> numeros = new ArrayList<>();
                for (Integer id : ids) { recalcularPago(c, id); numeros.add(buscar(c, id, false).getNumero()); }
                c.commit();
                return numeros;
            } catch (SQLException | RuntimeException e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        }
    }

    /* ============================== AUXILIARES ============================== */

    /** valor_pago = Σ partes dos pagamentos; FINALIZADO quando cobre o líquido, senão PENDENTE. */
    private void recalcularPago(Connection c, int idRomaneio) throws SQLException {
        try (PreparedStatement st = c.prepareStatement("UPDATE romaneio r SET valor_pago = x.pago, "
                + "status = CASE WHEN r.status IN ('PENDENTE','FINALIZADO') THEN "
                + "  CASE WHEN x.pago >= r.valor_liquido AND r.valor_liquido > 0 THEN 'FINALIZADO' ELSE 'PENDENTE' END "
                + "  ELSE r.status END, atualizado_em = now() "
                + "FROM (SELECT COALESCE(SUM(valor),0) AS pago FROM romaneio_pagamento_item WHERE idromaneio=?) x "
                + "WHERE r.idromaneio=?")) {
            st.setInt(1, idRomaneio); st.setInt(2, idRomaneio); st.executeUpdate();
        }
    }

    private Map<String, Object> carregarLote(Connection c, int idLote, boolean travar) throws SQLException {
        String sql = "SELECT cl.idlote, cl.status, cl.data_colheita, cl.id_quadra, cl.id_cultura, d.numero_diario, q.nome_quadra, "
                + "q.idareaproducao, ar.propriedadeareaproducao, ar.siglasareaproducao, al.nomeproduto "
                + "FROM colheita_lote cl JOIN diario_campo d ON d.iddiario = cl.iddiario "
                + "LEFT JOIN quadra q ON q.idquadra = cl.id_quadra "
                + "LEFT JOIN areaproducao ar ON ar.idareaproducao = q.idareaproducao "
                + "LEFT JOIN alimento al ON al.idproduto = cl.id_cultura WHERE cl.idlote=?" + (travar ? " FOR UPDATE OF cl" : "");
        try (PreparedStatement st = c.prepareStatement(sql)) {
            st.setInt(1, idLote);
            try (ResultSet rs = st.executeQuery()) {
                if (!rs.next()) throw new IllegalArgumentException("Lote de colheita não encontrado.");
                if (rs.getObject("id_quadra") == null || rs.getObject("id_cultura") == null || rs.getObject("idareaproducao") == null) {
                    throw new IllegalArgumentException("O lote não tem talhão, área ou alimento definidos.");
                }
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("idLote", rs.getInt("idlote"));
                m.put("status", rs.getString("status"));
                LocalDate dc = rs.getDate("data_colheita").toLocalDate();
                m.put("dataColheita", dc.toString());
                m.put("dataColheitaData", dc);
                m.put("numeroDiario", rs.getString("numero_diario"));
                m.put("idQuadra", rs.getInt("id_quadra")); m.put("nomeQuadra", rs.getString("nome_quadra"));
                m.put("idArea", rs.getInt("idareaproducao")); m.put("nomeArea", rs.getString("propriedadeareaproducao"));
                m.put("siglaArea", rs.getString("siglasareaproducao"));
                m.put("idAlimento", rs.getInt("id_cultura")); m.put("nomeAlimento", rs.getString("nomeproduto"));
                return m;
            }
        }
    }

    private List<Map<String, Object>> classificacoes(Connection c, int idAlimento) throws SQLException {
        List<Map<String, Object>> out = new ArrayList<>();
        try (PreparedStatement st = c.prepareStatement("SELECT DISTINCT cf.idclassificacao, cf.classificacao FROM alimentoclassificacao ac "
                + "JOIN classificacao cf ON cf.idclassificacao = ac.idclassificacao WHERE ac.idproduto=? ORDER BY cf.classificacao")) {
            st.setInt(1, idAlimento);
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("idClassificacao", rs.getInt(1)); m.put("nome", rs.getString(2));
                    out.add(m);
                }
            }
        }
        return out;
    }

    /** Caixas embaladas do lote por embalagem (mesma soma de ColheitaDAO.caixasEmbaladas; só registros com idembalagem). */
    private Map<Integer, BigDecimal> caixasEmbaladas(Connection c, int idLote) throws SQLException {
        Map<Integer, BigDecimal> out = new LinkedHashMap<>();
        try (PreparedStatement st = c.prepareStatement("SELECT idembalagem, caixas FROM (" + SQL_EMBALADAS_POR_LOTE + ") x WHERE idlote=?")) {
            st.setInt(1, idLote);
            try (ResultSet rs = st.executeQuery()) { while (rs.next()) out.put(rs.getInt(1), rs.getBigDecimal(2)); }
        }
        return out;
    }

    /** Caixas já romaneadas do lote por embalagem (não cancelados), sem contar o romaneio {@code exceto}. */
    private Map<Integer, BigDecimal> caixasRomaneadas(Connection c, int idLote, int exceto) throws SQLException {
        Map<Integer, BigDecimal> out = new LinkedHashMap<>();
        try (PreparedStatement st = c.prepareStatement("SELECT ri.idembalagem, SUM(ri.caixas) FROM romaneio r "
                + "JOIN romaneio_item ri ON ri.idromaneio = r.idromaneio WHERE r.idlote=? AND r.status <> 'CANCELADO' "
                + "AND r.idromaneio <> ? GROUP BY ri.idembalagem")) {
            st.setInt(1, idLote); st.setInt(2, exceto);
            try (ResultSet rs = st.executeQuery()) { while (rs.next()) out.put(rs.getInt(1), rs.getBigDecimal(2)); }
        }
        return out;
    }

    private void validarParceiro(Connection c, int idParceiro) throws SQLException {
        if (idParceiro <= 0) throw new IllegalArgumentException("Escolha o parceiro comprador.");
        try (PreparedStatement st = c.prepareStatement("SELECT 1 FROM parceiro WHERE idpessoa=? AND tipo_parceiro='PARCEIRO'")) {
            st.setInt(1, idParceiro);
            try (ResultSet rs = st.executeQuery()) {
                if (!rs.next()) throw new IllegalArgumentException("O parceiro escolhido não é um comprador (tipo Parceiro).");
            }
        }
    }

    /** Próximo número AAAA-NNNNNN do ano (lock transacional para dois cadastros simultâneos não colidirem). */
    private String proximoNumero(Connection c, int ano) throws SQLException {
        try (PreparedStatement st = c.prepareStatement("SELECT pg_advisory_xact_lock(?)")) { st.setLong(1, LOCK_NUMERO); st.execute(); }
        int seq;
        try (PreparedStatement st = c.prepareStatement("SELECT COALESCE(MAX(CAST(substring(numero FROM 6) AS integer)),0) + 1 "
                + "FROM romaneio WHERE numero LIKE ?")) {
            st.setString(1, ano + "-%");
            try (ResultSet rs = st.executeQuery()) { rs.next(); seq = rs.getInt(1); }
        }
        return String.format("%d-%06d", ano, seq);
    }

    private static String limpar(String s, int max, String campo) {
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) return null;
        if (s.length() > max) throw new IllegalArgumentException(campo + " aceita no máximo " + max + " caracteres.");
        return s;
    }

    private static String fmt(BigDecimal v) {
        return v.stripTrailingZeros().scale() <= 0 ? v.setScale(0, RoundingMode.HALF_UP).toPlainString()
                : v.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }

    private static String fmtMoeda(BigDecimal v) {
        java.text.NumberFormat nf = java.text.NumberFormat.getNumberInstance(Locale.forLanguageTag("pt-BR"));
        nf.setMinimumFractionDigits(2); nf.setMaximumFractionDigits(2);
        return "R$ " + nf.format(v);
    }

    private Romaneio mapear(ResultSet rs) throws SQLException {
        Romaneio r = new Romaneio();
        r.setIdRomaneio(rs.getInt("idromaneio")); r.setNumero(rs.getString("numero"));
        r.setIdLote(rs.getInt("idlote")); r.setNumeroDiario(rs.getString("numero_diario"));
        r.setIdArea(rs.getInt("id_area")); r.setNomeArea(rs.getString("nome_area")); r.setSiglaArea(rs.getString("sigla_area"));
        r.setIdQuadra(rs.getInt("id_quadra")); r.setNomeQuadra(rs.getString("nome_quadra"));
        r.setIdAlimento(rs.getInt("id_alimento")); r.setNomeAlimento(rs.getString("nome_alimento"));
        r.setIdSafra((Integer) rs.getObject("id_safra")); r.setNomeSafra(rs.getString("nome_safra"));
        r.setIdParceiro(rs.getInt("id_parceiro")); r.setNomeParceiro(rs.getString("nome_parceiro"));
        r.setDataEmissao(rs.getDate("data_emissao").toLocalDate());
        Date dc = rs.getDate("data_colheita"); r.setDataColheita(dc == null ? null : dc.toLocalDate());
        r.setMotorista(rs.getString("motorista")); r.setPlaca(rs.getString("placa"));
        r.setStatus(rs.getString("status"));
        r.setTotalCaixas(rs.getBigDecimal("total_caixas")); r.setValorBruto(rs.getBigDecimal("valor_bruto"));
        r.setTotalDespesas(rs.getBigDecimal("total_despesas")); r.setValorLiquido(rs.getBigDecimal("valor_liquido"));
        r.setValorPago(rs.getBigDecimal("valor_pago"));
        r.setObservacao(rs.getString("observacao"));
        r.setNotaFiscal(rs.getString("nota_fiscal")); r.setSerieNf(rs.getString("serie_nf"));
        r.setTransportadora(rs.getString("transportadora")); r.setTransportadoraDoc(rs.getString("transportadora_doc"));
        r.setMotoristaCpf(rs.getString("motorista_cpf")); r.setMotoristaCnh(rs.getString("motorista_cnh"));
        r.setPlacaCarreta(rs.getString("placa_carreta"));
        String uf = rs.getString("uf_veiculo"); r.setUfVeiculo(uf == null ? null : uf.trim());
        r.setTipoVeiculo(rs.getString("tipo_veiculo"));
        return r;
    }
}
