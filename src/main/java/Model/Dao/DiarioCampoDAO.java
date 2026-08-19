package Model.Dao;

import Model.Model.*;
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
 * Diário de Campo — persistência, cálculo de custos (mão de obra + insumos +
 * máquinas) e baixa de estoque ao finalizar. Os custos são recalculados a cada
 * criação/edição (snapshot no cabeçalho); a baixa de estoque só ocorre no
 * finalizar. Reutiliza VinculoDAO, EstoqueMovimentoDAO e o cadastro de máquina.
 */
public class DiarioCampoDAO {

    private final VinculoDAO vinculoDAO = new VinculoDAO();
    private final EstoqueMovimentoDAO estoqueDAO = new EstoqueMovimentoDAO();
    private static final BigDecimal DIAS_UTEIS_MES = new BigDecimal("22");

    /* ===================== CRIAÇÃO ===================== */

    public int inserirCompleto(DiarioCampo d) throws SQLException {
        try (Connection c = new PostgresConnection().getConnection()) {
            c.setAutoCommit(false);
            try {
                int id = proximoId(c);
                int ano = (d.getData() != null ? d.getData() : LocalDate.now()).getYear();
                String numero = ano + "-" + String.format("%06d", id);
                inserirCabecalho(c, id, numero, d);
                for (DiarioFuncionario f : d.getFuncionarios()) inserirFuncionario(c, id, f);
                for (DiarioMaquina m : d.getMaquinas()) inserirMaquina(c, id, m);
                for (DiarioInsumo i : d.getInsumos()) inserirInsumo(c, id, i);
                // PLANEJADA é apenas agendamento (previsão): não calcula custo. EM_ANDAMENTO/CONCLUIDA calculam.
                if (calculaCusto(d.getStatus())) recalcularCustos(c, id); else zerarCustos(c, id);
                c.commit();
                d.setIdDiario(id); d.setNumeroDiario(numero);
                return id;
            } catch (SQLException e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        }
    }

    /** Edita um diário não concluído: substitui cabeçalho e filhas, recalcula custos. */
    public void atualizarCompleto(DiarioCampo d) throws SQLException {
        try (Connection c = new PostgresConnection().getConnection()) {
            c.setAutoCommit(false);
            try {
                String status = statusAtual(c, d.getIdDiario());
                if ("CONCLUIDA".equals(status)) throw new SQLException("Diário concluído não pode ser editado.");
                atualizarCabecalho(c, d);
                for (String t : new String[]{"diario_funcionario", "diario_maquina", "diario_insumo"}) {
                    try (PreparedStatement st = c.prepareStatement("DELETE FROM " + t + " WHERE iddiario=?")) {
                        st.setInt(1, d.getIdDiario()); st.executeUpdate();
                    }
                }
                for (DiarioFuncionario f : d.getFuncionarios()) inserirFuncionario(c, d.getIdDiario(), f);
                for (DiarioMaquina m : d.getMaquinas()) inserirMaquina(c, d.getIdDiario(), m);
                for (DiarioInsumo i : d.getInsumos()) inserirInsumo(c, d.getIdDiario(), i);
                // PLANEJADA continua sem custo (previsão); EM_ANDAMENTO/CONCLUIDA calculam.
                if (calculaCusto(d.getStatus())) recalcularCustos(c, d.getIdDiario()); else zerarCustos(c, d.getIdDiario());
                c.commit();
            } catch (SQLException e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        }
    }

    /** Só calcula custos para atividades em Execução ou Concluídas; Planejada é previsão (custo 0). */
    private boolean calculaCusto(String status) {
        return "EM_ANDAMENTO".equals(status) || "CONCLUIDA".equals(status);
    }

    /** Zera os custos do diário (usado quando a atividade está apenas Planejada). */
    private void zerarCustos(Connection c, int idDiario) throws SQLException {
        try (PreparedStatement st = c.prepareStatement("UPDATE diario_campo SET custo_mao_obra=0, "
                + "custo_insumos=0, custo_maquinas=0, custo_total=0 WHERE iddiario=?")) {
            st.setInt(1, idDiario); st.executeUpdate();
        }
    }

    private void inserirCabecalho(Connection c, int id, String numero, DiarioCampo d) throws SQLException {
        String sql = "INSERT INTO diario_campo (iddiario, numero_diario, data, id_area, id_quadra, id_cultura, "
                + "id_responsavel, id_tipo_atividade, descricao, status, data_prevista, data_realizada, "
                + "hora_inicio, hora_fim, observacoes, id_safra) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement st = c.prepareStatement(sql)) {
            st.setInt(1, id); st.setString(2, numero);
            st.setDate(3, Date.valueOf(d.getData() != null ? d.getData() : LocalDate.now()));
            setIntOrNull(st, 4, d.getIdArea()); setIntOrNull(st, 5, d.getIdQuadra()); setIntOrNull(st, 6, d.getIdCultura());
            setIntOrNull(st, 7, d.getIdResponsavel()); st.setInt(8, d.getIdTipoAtividade());
            st.setString(9, d.getDescricao()); st.setString(10, d.getStatus() != null ? d.getStatus() : "PLANEJADA");
            setDateOrNull(st, 11, d.getDataPrevista()); setDateOrNull(st, 12, d.getDataRealizada());
            setTimeOrNull(st, 13, d.getHoraInicio()); setTimeOrNull(st, 14, d.getHoraFim());
            st.setString(15, d.getObservacoes()); setIntOrNull(st, 16, d.getIdSafra());
            st.executeUpdate();
        }
    }

    private void atualizarCabecalho(Connection c, DiarioCampo d) throws SQLException {
        // status só pode ser PLANEJADA ou EM_ANDAMENTO na edição (CONCLUIDA é via finalizar, e diário
        // concluído nem chega aqui). Se vier algo diferente, mantém PLANEJADA.
        String novoStatus = "EM_ANDAMENTO".equals(d.getStatus()) ? "EM_ANDAMENTO" : "PLANEJADA";
        String sql = "UPDATE diario_campo SET data=?, id_area=?, id_quadra=?, id_cultura=?, id_responsavel=?, "
                + "id_tipo_atividade=?, descricao=?, data_prevista=?, hora_inicio=?, hora_fim=?, observacoes=?, id_safra=?, status=? WHERE iddiario=?";
        try (PreparedStatement st = c.prepareStatement(sql)) {
            st.setDate(1, Date.valueOf(d.getData() != null ? d.getData() : LocalDate.now()));
            setIntOrNull(st, 2, d.getIdArea()); setIntOrNull(st, 3, d.getIdQuadra()); setIntOrNull(st, 4, d.getIdCultura());
            setIntOrNull(st, 5, d.getIdResponsavel()); st.setInt(6, d.getIdTipoAtividade());
            st.setString(7, d.getDescricao()); setDateOrNull(st, 8, d.getDataPrevista());
            setTimeOrNull(st, 9, d.getHoraInicio()); setTimeOrNull(st, 10, d.getHoraFim());
            st.setString(11, d.getObservacoes()); setIntOrNull(st, 12, d.getIdSafra());
            st.setString(13, novoStatus); st.setInt(14, d.getIdDiario());
            st.executeUpdate();
        }
    }

    private int proximoId(Connection c) throws SQLException {
        try (PreparedStatement st = c.prepareStatement("SELECT nextval('diario_campo_iddiario_seq')");
             ResultSet rs = st.executeQuery()) { rs.next(); return rs.getInt(1); }
    }

    private void inserirFuncionario(Connection c, int idDiario, DiarioFuncionario f) throws SQLException {
        String tipo = f.getTipoFuncionario() != null ? f.getTipoFuncionario() : resolverTipo(c, f.getIdPessoa());
        String sql = "INSERT INTO diario_funcionario (iddiario, idpessoa, tipo_funcionario, funcao_exercida, "
                + "horas_trabalhadas, custo_hora, valor_contratado, custo) VALUES (?,?,?,?,?,0,?,0)";
        try (PreparedStatement st = c.prepareStatement(sql)) {
            st.setInt(1, idDiario); st.setInt(2, f.getIdPessoa()); st.setString(3, tipo);
            st.setString(4, f.getFuncaoExercida()); st.setBigDecimal(5, nz(f.getHorasTrabalhadas()));
            st.setBigDecimal(6, nz(f.getValorContratado()));
            st.executeUpdate();
        }
    }

    private void inserirMaquina(Connection c, int idDiario, DiarioMaquina m) throws SQLException {
        String nome = m.getNome(); String categoria = m.getCategoria();
        if (m.getIdMaquina() != null) {   // snapshot do cadastro
            try (PreparedStatement q = c.prepareStatement("SELECT nome, tipo FROM maquina WHERE idmaquina=?")) {
                q.setInt(1, m.getIdMaquina());
                ResultSet rs = q.executeQuery();
                if (rs.next()) { nome = rs.getString(1); categoria = rs.getString(2); }
            }
        }
        String sql = "INSERT INTO diario_maquina (iddiario, id_maquina, categoria, nome, horimetro_inicial, "
                + "horimetro_final, horas_trabalhadas, km, valor_hora, custo) VALUES (?,?,?,?,?,?,?,?,0,0)";
        try (PreparedStatement st = c.prepareStatement(sql)) {
            st.setInt(1, idDiario); setIntOrNull(st, 2, m.getIdMaquina());
            st.setString(3, categoria); st.setString(4, nome);
            st.setBigDecimal(5, nz(m.getHorimetroInicial())); st.setBigDecimal(6, nz(m.getHorimetroFinal()));
            st.setBigDecimal(7, nz(m.getHorasTrabalhadas())); st.setBigDecimal(8, nz(m.getKm()));
            st.executeUpdate();
        }
    }

    private void inserirInsumo(Connection c, int idDiario, DiarioInsumo i) throws SQLException {
        String unidade = unidadeInsumo(c, i.getIdInsumo());
        String sql = "INSERT INTO diario_insumo (iddiario, id_insumo, quantidade, unidade, dose_aplicada, "
                + "custo_unitario, custo, baixado) VALUES (?,?,?,?,?,0,0,false)";
        try (PreparedStatement st = c.prepareStatement(sql)) {
            st.setInt(1, idDiario); st.setInt(2, i.getIdInsumo()); st.setBigDecimal(3, nz(i.getQuantidade()));
            st.setString(4, unidade); st.setString(5, i.getDoseAplicada());
            st.executeUpdate();
        }
    }

    /* ===================== CÁLCULO DE CUSTOS ===================== */

    /**
     * Recalcula custos de mão de obra, máquinas e insumos e grava os snapshots
     * no cabeçalho. NÃO mexe em estoque nem status (isso é do finalizar).
     */
    public void recalcularCustos(Connection c, int idDiario) throws SQLException {
        BigDecimal custoMO = BigDecimal.ZERO, custoMaq = BigDecimal.ZERO, custoIns = BigDecimal.ZERO;

        // Mão de obra
        for (Object[] f : carregarFuncBrutos(c, idDiario)) {
            int id = (int) f[0]; String tipo = (String) f[1]; BigDecimal horas = (BigDecimal) f[2];
            BigDecimal valorContratado = (BigDecimal) f[4]; int idpessoa = (int) f[5];
            BigDecimal custoHora = BigDecimal.ZERO, custo;
            if ("CLT".equalsIgnoreCase(tipo)) {
                custoHora = custoHoraCLT(c, idpessoa);
                custo = custoHora.multiply(nz(horas)).setScale(2, RoundingMode.HALF_UP);
            } else if ("DIARISTA".equalsIgnoreCase(tipo)) {
                custo = valorDiaria(c, idpessoa);
            } else {
                custo = nz(valorContratado);
            }
            try (PreparedStatement st = c.prepareStatement("UPDATE diario_funcionario SET custo_hora=?, custo=? WHERE id=?")) {
                st.setBigDecimal(1, custoHora); st.setBigDecimal(2, custo); st.setInt(3, id); st.executeUpdate();
            }
            custoMO = custoMO.add(custo);
        }

        // Máquinas (custo via cadastro: veículo=km×custo_km, demais=horas×custo_hora)
        for (Object[] m : carregarMaqBrutos(c, idDiario)) {
            int id = (int) m[0]; Integer idMaq = (Integer) m[1]; BigDecimal horas = (BigDecimal) m[2];
            BigDecimal km = (BigDecimal) m[3]; BigDecimal valorHoraLegado = (BigDecimal) m[4];
            BigDecimal unit = BigDecimal.ZERO, uso;
            if (idMaq != null) {
                String[] mq = maquinaTipoCustos(c, idMaq);   // [tipo, custo_hora, custo_km]
                if (mq != null && "VEICULO".equalsIgnoreCase(mq[0])) { unit = new BigDecimal(mq[2]); uso = nz(km); }
                else { unit = new BigDecimal(mq != null ? mq[1] : "0"); uso = nz(horas); }
            } else { unit = nz(valorHoraLegado); uso = nz(horas); }  // registro livre (legado)
            BigDecimal custo = unit.multiply(uso).setScale(2, RoundingMode.HALF_UP);
            try (PreparedStatement st = c.prepareStatement("UPDATE diario_maquina SET valor_hora=?, custo=? WHERE id=?")) {
                st.setBigDecimal(1, unit); st.setBigDecimal(2, custo); st.setInt(3, id); st.executeUpdate();
            }
            custoMaq = custoMaq.add(custo);
        }

        // Insumos (custo pelo preço médio corrente; baixa é no finalizar)
        for (Object[] i : carregarInsBrutos(c, idDiario)) {
            int id = (int) i[0]; int idInsumo = (int) i[1]; BigDecimal qtd = (BigDecimal) i[2];
            BigDecimal precoMedio = precoInsumo(c, idInsumo);
            BigDecimal custo = nz(qtd).multiply(precoMedio).setScale(2, RoundingMode.HALF_UP);
            try (PreparedStatement st = c.prepareStatement("UPDATE diario_insumo SET custo_unitario=?, custo=? WHERE id=?")) {
                st.setBigDecimal(1, precoMedio); st.setBigDecimal(2, custo); st.setInt(3, id); st.executeUpdate();
            }
            custoIns = custoIns.add(custo);
        }

        BigDecimal total = custoMO.add(custoMaq).add(custoIns);
        try (PreparedStatement st = c.prepareStatement("UPDATE diario_campo SET custo_mao_obra=?, custo_insumos=?, "
                + "custo_maquinas=?, custo_total=? WHERE iddiario=?")) {
            st.setBigDecimal(1, custoMO); st.setBigDecimal(2, custoIns); st.setBigDecimal(3, custoMaq);
            st.setBigDecimal(4, total); st.setInt(5, idDiario); st.executeUpdate();
        }
    }

    /** Finaliza: recalcula custos + baixa de estoque (bloqueia negativo, sem baixa dupla) + status CONCLUIDA. */
    public void finalizar(int idDiario) throws SQLException, EstoqueInsuficienteException {
        try (Connection c = new PostgresConnection().getConnection()) {
            c.setAutoCommit(false);
            try {
                String[] cab = statusNumeroData(c, idDiario);
                if (cab == null) throw new SQLException("Diário não encontrado.");
                if (!"PLANEJADA".equals(cab[0]) && !"EM_ANDAMENTO".equals(cab[0]))
                    throw new SQLException("Somente diários PLANEJADA/EM_ANDAMENTO podem ser finalizados.");
                LocalDate dataMov = cab[2] != null ? LocalDate.parse(cab[2]) : LocalDate.now();

                recalcularCustos(c, idDiario);

                for (Object[] i : carregarInsBrutos(c, idDiario)) {
                    int id = (int) i[0]; int idInsumo = (int) i[1]; BigDecimal qtd = (BigDecimal) i[2]; boolean baixado = (boolean) i[3];
                    if (!baixado && nz(qtd).signum() > 0) {
                        estoqueDAO.registrarSaida(c, idInsumo, qtd, dataMov, "Diário " + cab[1]);
                        try (PreparedStatement st = c.prepareStatement("UPDATE diario_insumo SET baixado=true WHERE id=?")) {
                            st.setInt(1, id); st.executeUpdate();
                        }
                    }
                }
                try (PreparedStatement st = c.prepareStatement("UPDATE diario_campo SET status='CONCLUIDA', "
                        + "data_realizada=COALESCE(data_realizada, CURRENT_DATE) WHERE iddiario=?")) {
                    st.setInt(1, idDiario); st.executeUpdate();
                }
                c.commit();
            } catch (SQLException | EstoqueInsuficienteException e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        }
    }

    public void definirStatus(int idDiario, String status) throws SQLException {
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement st = c.prepareStatement("UPDATE diario_campo SET status=? WHERE iddiario=?")) {
            st.setString(1, status); st.setInt(2, idDiario); st.executeUpdate();
        }
    }

    /* ===================== CONSULTAS ===================== */

    public DiarioCampo buscarPorId(int id) throws SQLException {
        String sql = "SELECT d.*, ar.propriedadeareaproducao AS area_nome, q.nome_quadra AS quadra_nome, "
                + "al.nomeproduto AS cultura_nome, p.nomepessoa AS resp_nome, t.nome AS tipo_nome, sf.nome AS safra_nome "
                + "FROM diario_campo d "
                + "LEFT JOIN areaproducao ar ON ar.idareaproducao=d.id_area "
                + "LEFT JOIN quadra q ON q.idquadra=d.id_quadra "
                + "LEFT JOIN alimento al ON al.idproduto=d.id_cultura "
                + "LEFT JOIN pessoa p ON p.idpessoa=d.id_responsavel "
                + "LEFT JOIN diario_tipo_atividade t ON t.idtipo=d.id_tipo_atividade "
                + "LEFT JOIN safra sf ON sf.idsafra=d.id_safra WHERE d.iddiario=?";
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement st = c.prepareStatement(sql)) {
            st.setInt(1, id);
            ResultSet rs = st.executeQuery();
            if (!rs.next()) return null;
            DiarioCampo d = mapCabecalho(rs);
            d.setFuncionarios(listarFuncionarios(c, id));
            d.setMaquinas(listarMaquinas(c, id));
            d.setInsumos(listarInsumos(c, id));
            return d;
        }
    }

    public List<DiarioCampo> listar(Integer idArea, Integer idQuadra, Integer idCultura,
                                    Integer idTipo, String status, String de, String ate) throws SQLException {
        StringBuilder sql = new StringBuilder(
            "SELECT d.*, ar.propriedadeareaproducao AS area_nome, q.nome_quadra AS quadra_nome, "
          + "al.nomeproduto AS cultura_nome, p.nomepessoa AS resp_nome, t.nome AS tipo_nome, sf.nome AS safra_nome "
          + "FROM diario_campo d "
          + "LEFT JOIN areaproducao ar ON ar.idareaproducao=d.id_area "
          + "LEFT JOIN quadra q ON q.idquadra=d.id_quadra "
          + "LEFT JOIN alimento al ON al.idproduto=d.id_cultura "
          + "LEFT JOIN pessoa p ON p.idpessoa=d.id_responsavel "
          + "LEFT JOIN diario_tipo_atividade t ON t.idtipo=d.id_tipo_atividade "
          + "LEFT JOIN safra sf ON sf.idsafra=d.id_safra WHERE 1=1");
        List<Object> ps = new ArrayList<>();
        if (idArea != null)   { sql.append(" AND d.id_area=?"); ps.add(idArea); }
        if (idQuadra != null) { sql.append(" AND d.id_quadra=?"); ps.add(idQuadra); }
        if (idCultura != null){ sql.append(" AND d.id_cultura=?"); ps.add(idCultura); }
        if (idTipo != null)   { sql.append(" AND d.id_tipo_atividade=?"); ps.add(idTipo); }
        if (status != null && !status.isBlank()) { sql.append(" AND d.status=?"); ps.add(status); }
        if (de != null && !de.isBlank())   { sql.append(" AND d.data>=?"); ps.add(Date.valueOf(de)); }
        if (ate != null && !ate.isBlank()) { sql.append(" AND d.data<=?"); ps.add(Date.valueOf(ate)); }
        sql.append(" ORDER BY d.data DESC, d.iddiario DESC");
        List<DiarioCampo> lista = new ArrayList<>();
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement st = c.prepareStatement(sql.toString())) {
            for (int k = 0; k < ps.size(); k++) st.setObject(k + 1, ps.get(k));
            ResultSet rs = st.executeQuery();
            while (rs.next()) lista.add(mapCabecalho(rs));
        }
        return lista;
    }

    public List<Map<String, Object>> relatorioCusto(String dimensao, String de, String ate) throws SQLException {
        String grupo, join;
        switch (dimensao == null ? "" : dimensao.toLowerCase()) {
            case "cultura":     grupo = "al.nomeproduto"; join = "LEFT JOIN alimento al ON al.idproduto=d.id_cultura"; break;
            case "atividade":   grupo = "t.nome"; join = "LEFT JOIN diario_tipo_atividade t ON t.idtipo=d.id_tipo_atividade"; break;
            case "area":        grupo = "ar.propriedadeareaproducao"; join = "LEFT JOIN areaproducao ar ON ar.idareaproducao=d.id_area"; break;
            case "responsavel": grupo = "p.nomepessoa"; join = "LEFT JOIN pessoa p ON p.idpessoa=d.id_responsavel"; break;
            default:            grupo = "q.nome_quadra"; join = "LEFT JOIN quadra q ON q.idquadra=d.id_quadra"; break;
        }
        StringBuilder sql = new StringBuilder("SELECT COALESCE(" + grupo + ",'—') AS rotulo, COUNT(*) AS qtd_atividades, "
                + "COALESCE(SUM(d.custo_total),0) AS custo_total, COALESCE(SUM(d.custo_mao_obra),0) AS custo_mao_obra, "
                + "COALESCE(SUM(d.custo_insumos),0) AS custo_insumos, COALESCE(SUM(d.custo_maquinas),0) AS custo_maquinas "
                + "FROM diario_campo d " + join + " WHERE d.status='CONCLUIDA'");
        List<Object> ps = new ArrayList<>();
        if (de != null && !de.isBlank())  { sql.append(" AND d.data>=?"); ps.add(Date.valueOf(de)); }
        if (ate != null && !ate.isBlank()){ sql.append(" AND d.data<=?"); ps.add(Date.valueOf(ate)); }
        sql.append(" GROUP BY ").append(grupo).append(" ORDER BY custo_total DESC");
        List<Map<String, Object>> out = new ArrayList<>();
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement st = c.prepareStatement(sql.toString())) {
            for (int k = 0; k < ps.size(); k++) st.setObject(k + 1, ps.get(k));
            ResultSet rs = st.executeQuery();
            while (rs.next()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("rotulo", rs.getString("rotulo")); m.put("qtdAtividades", rs.getInt("qtd_atividades"));
                m.put("custoTotal", rs.getBigDecimal("custo_total")); m.put("custoMaoObra", rs.getBigDecimal("custo_mao_obra"));
                m.put("custoInsumos", rs.getBigDecimal("custo_insumos")); m.put("custoMaquinas", rs.getBigDecimal("custo_maquinas"));
                out.add(m);
            }
        }
        return out;
    }

    /** Funcionários com custo de referência (CLT=custo/hora, Diarista=valor/dia) p/ os selects e preview. */
    public List<Map<String, Object>> listarFuncionariosDisponiveis() throws SQLException {
        return listarFuncionariosDisponiveis(null);
    }

    /**
     * Lista funcionários para os selects do diário. Quando {@code data} é informada,
     * retorna apenas os que possuem vínculo ATIVO nessa data (admitido até a data e
     * não desligado até ela) — ou seja, quem estava presente/vinculado no dia do lançamento.
     */
    public List<Map<String, Object>> listarFuncionariosDisponiveis(LocalDate data) throws SQLException {
        // Vínculo usado para salário/custo e para o filtro por data (quando houver).
        String joinVinc = (data != null)
                ? "LEFT JOIN vinculo_empregaticio v ON v.idpessoa=f.idpessoa "
                  + "AND v.data_admissao <= ? AND (v.data_desligamento IS NULL OR v.data_desligamento >= ?) "
                : "LEFT JOIN vinculo_empregaticio v ON v.idpessoa=f.idpessoa AND v.data_desligamento IS NULL ";
        String sql = "SELECT f.idpessoa, pe.nomepessoa, "
                + "CASE WHEN c.idpessoa IS NOT NULL THEN 'CLT' WHEN di.idpessoa IS NOT NULL THEN 'DIARISTA' "
                + "     WHEN e.idpessoa IS NOT NULL THEN 'EMPREITA' ELSE 'OUTRO' END AS tipo, "
                + "CASE WHEN c.idpessoa IS NOT NULL THEN round(COALESCE(v.salario_mensal, c.salariomensal, 0) / "
                + "        (GREATEST(COALESCE(v.carga_horaria_diaria, c.cargahorariadiaria, 8),1) * 22.0), 2) "
                + "     WHEN di.idpessoa IS NOT NULL THEN COALESCE(di.valorpordia, 0) ELSE 0 END AS custo_ref "
                + "FROM funcionario f JOIN pessoa pe ON pe.idpessoa=f.idpessoa "
                + "LEFT JOIN funcionarioclt c ON c.idpessoa=f.idpessoa "
                + "LEFT JOIN funcionariodiarista di ON di.idpessoa=f.idpessoa "
                + "LEFT JOIN funcionarioempreita e ON e.idpessoa=f.idpessoa "
                + joinVinc
                + (data != null ? "WHERE v.id_vinculo IS NOT NULL " : "")
                + "ORDER BY pe.nomepessoa";
        List<Map<String, Object>> out = new ArrayList<>();
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement st = c.prepareStatement(sql)) {
            if (data != null) { st.setDate(1, Date.valueOf(data)); st.setDate(2, Date.valueOf(data)); }
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("idPessoa", rs.getInt("idpessoa")); m.put("nome", rs.getString("nomepessoa"));
                    m.put("tipo", rs.getString("tipo")); m.put("custoRef", rs.getBigDecimal("custo_ref"));
                    out.add(m);
                }
            }
        }
        return out;
    }

    /* ===================== helpers de custo ===================== */

    private String resolverTipo(Connection c, int idpessoa) throws SQLException {
        String sql = "SELECT CASE WHEN c.idpessoa IS NOT NULL THEN 'CLT' WHEN di.idpessoa IS NOT NULL THEN 'DIARISTA' "
                + "WHEN e.idpessoa IS NOT NULL THEN 'EMPREITA' ELSE 'OUTRO' END FROM funcionario f "
                + "LEFT JOIN funcionarioclt c ON c.idpessoa=f.idpessoa LEFT JOIN funcionariodiarista di ON di.idpessoa=f.idpessoa "
                + "LEFT JOIN funcionarioempreita e ON e.idpessoa=f.idpessoa WHERE f.idpessoa=?";
        try (PreparedStatement st = c.prepareStatement(sql)) {
            st.setInt(1, idpessoa); ResultSet rs = st.executeQuery();
            return rs.next() ? rs.getString(1) : "OUTRO";
        }
    }

    private BigDecimal custoHoraCLT(Connection c, int idpessoa) throws SQLException {
        BigDecimal salario = BigDecimal.ZERO; int carga = 8;
        try {
            Vinculo v = vinculoDAO.buscarAtivo(idpessoa);
            if (v != null) { salario = nz(v.getSalarioMensal()); carga = v.getCargaHorariaDiaria() > 0 ? v.getCargaHorariaDiaria() : 8; }
        } catch (SQLException ignore) {}
        if (salario.signum() == 0) {
            try (PreparedStatement st = c.prepareStatement("SELECT salariomensal, cargahorariadiaria FROM funcionarioclt WHERE idpessoa=?")) {
                st.setInt(1, idpessoa); ResultSet rs = st.executeQuery();
                if (rs.next()) { salario = nz(rs.getBigDecimal(1)); int cg = rs.getInt(2); if (cg > 0) carga = cg; }
            }
        }
        if (salario.signum() == 0) return BigDecimal.ZERO;
        return salario.divide(new BigDecimal(carga).multiply(DIAS_UTEIS_MES), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal valorDiaria(Connection c, int idpessoa) throws SQLException {
        try (PreparedStatement st = c.prepareStatement("SELECT valorpordia FROM funcionariodiarista WHERE idpessoa=?")) {
            st.setInt(1, idpessoa); ResultSet rs = st.executeQuery();
            return rs.next() ? nz(rs.getBigDecimal(1)) : BigDecimal.ZERO;
        }
    }

    private BigDecimal precoInsumo(Connection c, int idInsumo) throws SQLException {
        try (PreparedStatement st = c.prepareStatement("SELECT preco_medio FROM insumo WHERE idinsumo=?")) {
            st.setInt(1, idInsumo); ResultSet rs = st.executeQuery();
            return rs.next() ? nz(rs.getBigDecimal(1)) : BigDecimal.ZERO;
        }
    }

    private String unidadeInsumo(Connection c, int idInsumo) throws SQLException {
        try (PreparedStatement st = c.prepareStatement("SELECT unidade FROM insumo WHERE idinsumo=?")) {
            st.setInt(1, idInsumo); ResultSet rs = st.executeQuery();
            return rs.next() ? rs.getString(1) : null;
        }
    }

    /** [tipo, custo_hora, custo_km] da máquina, ou null. */
    private String[] maquinaTipoCustos(Connection c, int idMaquina) throws SQLException {
        try (PreparedStatement st = c.prepareStatement("SELECT tipo, custo_hora, custo_km FROM maquina WHERE idmaquina=?")) {
            st.setInt(1, idMaquina); ResultSet rs = st.executeQuery();
            if (rs.next()) return new String[]{ rs.getString(1), nz(rs.getBigDecimal(2)).toPlainString(), nz(rs.getBigDecimal(3)).toPlainString() };
        }
        return null;
    }

    private String statusAtual(Connection c, int idDiario) throws SQLException {
        try (PreparedStatement st = c.prepareStatement("SELECT status FROM diario_campo WHERE iddiario=?")) {
            st.setInt(1, idDiario); ResultSet rs = st.executeQuery();
            return rs.next() ? rs.getString(1) : null;
        }
    }
    private String[] statusNumeroData(Connection c, int idDiario) throws SQLException {
        try (PreparedStatement st = c.prepareStatement("SELECT status, numero_diario, data FROM diario_campo WHERE iddiario=?")) {
            st.setInt(1, idDiario); ResultSet rs = st.executeQuery();
            if (!rs.next()) return null;
            Date dt = rs.getDate("data");
            return new String[]{ rs.getString("status"), rs.getString("numero_diario"), dt != null ? dt.toLocalDate().toString() : null };
        }
    }

    private List<Object[]> carregarFuncBrutos(Connection c, int idDiario) throws SQLException {
        List<Object[]> l = new ArrayList<>();
        try (PreparedStatement st = c.prepareStatement("SELECT id, tipo_funcionario, horas_trabalhadas, custo_hora, valor_contratado, idpessoa FROM diario_funcionario WHERE iddiario=?")) {
            st.setInt(1, idDiario); ResultSet rs = st.executeQuery();
            while (rs.next()) l.add(new Object[]{ rs.getInt(1), rs.getString(2), rs.getBigDecimal(3), rs.getBigDecimal(4), rs.getBigDecimal(5), rs.getInt(6) });
        }
        return l;
    }
    private List<Object[]> carregarMaqBrutos(Connection c, int idDiario) throws SQLException {
        List<Object[]> l = new ArrayList<>();
        try (PreparedStatement st = c.prepareStatement("SELECT id, id_maquina, horas_trabalhadas, km, valor_hora FROM diario_maquina WHERE iddiario=?")) {
            st.setInt(1, idDiario); ResultSet rs = st.executeQuery();
            while (rs.next()) l.add(new Object[]{ rs.getInt(1), (Integer) rs.getObject(2), rs.getBigDecimal(3), rs.getBigDecimal(4), rs.getBigDecimal(5) });
        }
        return l;
    }
    private List<Object[]> carregarInsBrutos(Connection c, int idDiario) throws SQLException {
        List<Object[]> l = new ArrayList<>();
        try (PreparedStatement st = c.prepareStatement("SELECT id, id_insumo, quantidade, baixado FROM diario_insumo WHERE iddiario=?")) {
            st.setInt(1, idDiario); ResultSet rs = st.executeQuery();
            while (rs.next()) l.add(new Object[]{ rs.getInt(1), rs.getInt(2), rs.getBigDecimal(3), rs.getBoolean(4) });
        }
        return l;
    }

    /* ===================== mapeamento de filhas ===================== */

    private List<DiarioFuncionario> listarFuncionarios(Connection c, int idDiario) throws SQLException {
        List<DiarioFuncionario> l = new ArrayList<>();
        try (PreparedStatement st = c.prepareStatement("SELECT df.*, pe.nomepessoa FROM diario_funcionario df "
                + "LEFT JOIN pessoa pe ON pe.idpessoa=df.idpessoa WHERE df.iddiario=? ORDER BY df.id")) {
            st.setInt(1, idDiario); ResultSet rs = st.executeQuery();
            while (rs.next()) {
                DiarioFuncionario f = new DiarioFuncionario();
                f.setId(rs.getInt("id")); f.setIdDiario(idDiario); f.setIdPessoa(rs.getInt("idpessoa"));
                f.setNomePessoa(rs.getString("nomepessoa")); f.setTipoFuncionario(rs.getString("tipo_funcionario"));
                f.setFuncaoExercida(rs.getString("funcao_exercida")); f.setHorasTrabalhadas(rs.getBigDecimal("horas_trabalhadas"));
                f.setCustoHora(rs.getBigDecimal("custo_hora")); f.setValorContratado(rs.getBigDecimal("valor_contratado"));
                f.setCusto(rs.getBigDecimal("custo")); l.add(f);
            }
        }
        return l;
    }
    private List<DiarioMaquina> listarMaquinas(Connection c, int idDiario) throws SQLException {
        List<DiarioMaquina> l = new ArrayList<>();
        try (PreparedStatement st = c.prepareStatement("SELECT * FROM diario_maquina WHERE iddiario=? ORDER BY id")) {
            st.setInt(1, idDiario); ResultSet rs = st.executeQuery();
            while (rs.next()) {
                DiarioMaquina m = new DiarioMaquina();
                m.setId(rs.getInt("id")); m.setIdDiario(idDiario); m.setIdMaquina((Integer) rs.getObject("id_maquina"));
                m.setCategoria(rs.getString("categoria")); m.setNome(rs.getString("nome"));
                m.setHorimetroInicial(rs.getBigDecimal("horimetro_inicial")); m.setHorimetroFinal(rs.getBigDecimal("horimetro_final"));
                m.setHorasTrabalhadas(rs.getBigDecimal("horas_trabalhadas")); m.setKm(rs.getBigDecimal("km"));
                m.setValorHora(rs.getBigDecimal("valor_hora")); m.setCusto(rs.getBigDecimal("custo")); l.add(m);
            }
        }
        return l;
    }
    private List<DiarioInsumo> listarInsumos(Connection c, int idDiario) throws SQLException {
        List<DiarioInsumo> l = new ArrayList<>();
        try (PreparedStatement st = c.prepareStatement("SELECT di.*, i.nome AS insumo_nome FROM diario_insumo di "
                + "LEFT JOIN insumo i ON i.idinsumo=di.id_insumo WHERE di.iddiario=? ORDER BY di.id")) {
            st.setInt(1, idDiario); ResultSet rs = st.executeQuery();
            while (rs.next()) {
                DiarioInsumo i = new DiarioInsumo();
                i.setId(rs.getInt("id")); i.setIdDiario(idDiario); i.setIdInsumo(rs.getInt("id_insumo"));
                i.setInsumoNome(rs.getString("insumo_nome")); i.setQuantidade(rs.getBigDecimal("quantidade"));
                i.setUnidade(rs.getString("unidade")); i.setDoseAplicada(rs.getString("dose_aplicada"));
                i.setCustoUnitario(rs.getBigDecimal("custo_unitario")); i.setCusto(rs.getBigDecimal("custo"));
                i.setBaixado(rs.getBoolean("baixado")); l.add(i);
            }
        }
        return l;
    }

    private DiarioCampo mapCabecalho(ResultSet rs) throws SQLException {
        DiarioCampo d = new DiarioCampo();
        d.setIdDiario(rs.getInt("iddiario")); d.setNumeroDiario(rs.getString("numero_diario"));
        Date data = rs.getDate("data"); d.setData(data != null ? data.toLocalDate() : null);
        d.setIdArea((Integer) rs.getObject("id_area")); d.setAreaNome(rs.getString("area_nome"));
        d.setIdQuadra((Integer) rs.getObject("id_quadra")); d.setQuadraNome(rs.getString("quadra_nome"));
        d.setIdCultura((Integer) rs.getObject("id_cultura")); d.setCulturaNome(rs.getString("cultura_nome"));
        d.setIdResponsavel((Integer) rs.getObject("id_responsavel")); d.setResponsavelNome(rs.getString("resp_nome"));
        d.setIdTipoAtividade(rs.getInt("id_tipo_atividade")); d.setTipoAtividadeNome(rs.getString("tipo_nome"));
        d.setIdSafra((Integer) rs.getObject("id_safra")); d.setSafraNome(rs.getString("safra_nome"));
        d.setDescricao(rs.getString("descricao")); d.setStatus(rs.getString("status"));
        Date dp = rs.getDate("data_prevista"); d.setDataPrevista(dp != null ? dp.toLocalDate() : null);
        Date dr = rs.getDate("data_realizada"); d.setDataRealizada(dr != null ? dr.toLocalDate() : null);
        Time hi = rs.getTime("hora_inicio"); d.setHoraInicio(hi != null ? hi.toLocalTime() : null);
        Time hf = rs.getTime("hora_fim"); d.setHoraFim(hf != null ? hf.toLocalTime() : null);
        d.setObservacoes(rs.getString("observacoes"));
        d.setCustoMaoObra(rs.getBigDecimal("custo_mao_obra")); d.setCustoInsumos(rs.getBigDecimal("custo_insumos"));
        d.setCustoMaquinas(rs.getBigDecimal("custo_maquinas")); d.setCustoTotal(rs.getBigDecimal("custo_total"));
        return d;
    }

    /* ===================== util JDBC ===================== */
    private static BigDecimal nz(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }
    private static void setIntOrNull(PreparedStatement st, int i, Integer v) throws SQLException {
        if (v != null) st.setInt(i, v); else st.setNull(i, Types.INTEGER);
    }
    private static void setDateOrNull(PreparedStatement st, int i, LocalDate d) throws SQLException {
        if (d != null) st.setDate(i, Date.valueOf(d)); else st.setNull(i, Types.DATE);
    }
    private static void setTimeOrNull(PreparedStatement st, int i, java.time.LocalTime t) throws SQLException {
        if (t != null) st.setTime(i, Time.valueOf(t)); else st.setNull(i, Types.TIME);
    }
}
