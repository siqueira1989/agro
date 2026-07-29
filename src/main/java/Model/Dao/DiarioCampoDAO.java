package Model.Dao;

import Model.Model.*;
import Util.PostgresConnection;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Diário de Campo — persistência do cabeçalho e das coleções (funcionários,
 * máquinas, insumos), cálculo de custos e baixa automática de estoque ao
 * finalizar. Reutiliza VinculoDAO e EstoqueMovimentoDAO.
 */
public class DiarioCampoDAO {

    private final VinculoDAO vinculoDAO = new VinculoDAO();
    private final EstoqueMovimentoDAO estoqueDAO = new EstoqueMovimentoDAO();

    private static final BigDecimal DIAS_UTEIS_MES = new BigDecimal("22");

    /* ===================== CRIAÇÃO (transacional) ===================== */

    public int inserirCompleto(DiarioCampo d) throws SQLException {
        try (Connection c = new PostgresConnection().getConnection()) {
            c.setAutoCommit(false);
            try {
                int id = proximoId(c);
                int ano = (d.getData() != null ? d.getData() : LocalDate.now()).getYear();
                String numero = ano + "-" + String.format("%06d", id);

                String sql = "INSERT INTO diario_campo (iddiario, numero_diario, data, id_area, id_quadra, "
                        + "id_cultura, id_responsavel, id_tipo_atividade, descricao, status, data_prevista, "
                        + "data_realizada, hora_inicio, hora_fim, observacoes) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
                try (PreparedStatement st = c.prepareStatement(sql)) {
                    st.setInt(1, id);
                    st.setString(2, numero);
                    st.setDate(3, dateOrNow(d.getData()));
                    setIntOrNull(st, 4, d.getIdArea());
                    setIntOrNull(st, 5, d.getIdQuadra());
                    setIntOrNull(st, 6, d.getIdCultura());
                    setIntOrNull(st, 7, d.getIdResponsavel());
                    st.setInt(8, d.getIdTipoAtividade());
                    st.setString(9, d.getDescricao());
                    st.setString(10, d.getStatus() != null ? d.getStatus() : "PLANEJADA");
                    setDateOrNull(st, 11, d.getDataPrevista());
                    setDateOrNull(st, 12, d.getDataRealizada());
                    setTimeOrNull(st, 13, d.getHoraInicio());
                    setTimeOrNull(st, 14, d.getHoraFim());
                    st.setString(15, d.getObservacoes());
                    st.executeUpdate();
                }

                for (DiarioFuncionario f : d.getFuncionarios()) inserirFuncionario(c, id, f);
                for (DiarioMaquina m : d.getMaquinas()) inserirMaquina(c, id, m);
                for (DiarioInsumo i : d.getInsumos()) inserirInsumo(c, id, i);

                c.commit();
                d.setIdDiario(id);
                d.setNumeroDiario(numero);
                return id;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    private int proximoId(Connection c) throws SQLException {
        try (PreparedStatement st = c.prepareStatement("SELECT nextval('diario_campo_iddiario_seq')");
             ResultSet rs = st.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private void inserirFuncionario(Connection c, int idDiario, DiarioFuncionario f) throws SQLException {
        String tipo = f.getTipoFuncionario() != null ? f.getTipoFuncionario() : resolverTipo(c, f.getIdPessoa());
        f.setTipoFuncionario(tipo);
        BigDecimal custoHora = "CLT".equalsIgnoreCase(tipo) ? custoHoraCLT(c, f.getIdPessoa()) : BigDecimal.ZERO;
        f.setCustoHora(custoHora);
        String sql = "INSERT INTO diario_funcionario (iddiario, idpessoa, tipo_funcionario, funcao_exercida, "
                + "horas_trabalhadas, custo_hora, valor_contratado, custo) VALUES (?,?,?,?,?,?,?,0)";
        try (PreparedStatement st = c.prepareStatement(sql)) {
            st.setInt(1, idDiario);
            st.setInt(2, f.getIdPessoa());
            st.setString(3, tipo);
            st.setString(4, f.getFuncaoExercida());
            st.setBigDecimal(5, nz(f.getHorasTrabalhadas()));
            st.setBigDecimal(6, custoHora);
            st.setBigDecimal(7, nz(f.getValorContratado()));
            st.executeUpdate();
        }
    }

    private void inserirMaquina(Connection c, int idDiario, DiarioMaquina m) throws SQLException {
        String sql = "INSERT INTO diario_maquina (iddiario, id_maquina, categoria, nome, horimetro_inicial, "
                + "horimetro_final, horas_trabalhadas, valor_hora, custo) VALUES (?,?,?,?,?,?,?,?,0)";
        try (PreparedStatement st = c.prepareStatement(sql)) {
            st.setInt(1, idDiario);
            setIntOrNull(st, 2, m.getIdMaquina());
            st.setString(3, m.getCategoria());
            st.setString(4, m.getNome());
            st.setBigDecimal(5, nz(m.getHorimetroInicial()));
            st.setBigDecimal(6, nz(m.getHorimetroFinal()));
            st.setBigDecimal(7, nz(m.getHorasTrabalhadas()));
            st.setBigDecimal(8, nz(m.getValorHora()));
            st.executeUpdate();
        }
    }

    private void inserirInsumo(Connection c, int idDiario, DiarioInsumo i) throws SQLException {
        BigDecimal[] pu = precoUnidadeInsumo(c, i.getIdInsumo());  // [preco_medio, (unidade via out param? não)]
        String unidade = unidadeInsumo(c, i.getIdInsumo());
        i.setCustoUnitario(pu[0]);
        i.setUnidade(unidade);
        String sql = "INSERT INTO diario_insumo (iddiario, id_insumo, quantidade, unidade, dose_aplicada, "
                + "custo_unitario, custo, baixado) VALUES (?,?,?,?,?,?,0,false)";
        try (PreparedStatement st = c.prepareStatement(sql)) {
            st.setInt(1, idDiario);
            st.setInt(2, i.getIdInsumo());
            st.setBigDecimal(3, nz(i.getQuantidade()));
            st.setString(4, unidade);
            st.setString(5, i.getDoseAplicada());
            st.setBigDecimal(6, pu[0]);
            st.executeUpdate();
        }
    }

    /* ===================== FINALIZAR (custo + baixa) ===================== */

    /**
     * Finaliza a atividade: calcula os custos (mão de obra, insumos, máquinas),
     * dá baixa no estoque dos insumos ainda não baixados (bloqueia negativo) e
     * grava os snapshots. Tudo numa única transação.
     */
    public void finalizar(int idDiario) throws SQLException, EstoqueInsuficienteException {
        try (Connection c = new PostgresConnection().getConnection()) {
            c.setAutoCommit(false);
            try {
                String[] cab = statusNumeroData(c, idDiario);   // [status, numero, dataISO]
                if (cab == null) throw new SQLException("Diário não encontrado.");
                String status = cab[0];
                if (!"PLANEJADA".equals(status) && !"EM_ANDAMENTO".equals(status)) {
                    throw new SQLException("Somente diários PLANEJADA/EM_ANDAMENTO podem ser finalizados.");
                }
                LocalDate dataMov = cab[2] != null ? LocalDate.parse(cab[2]) : LocalDate.now();

                BigDecimal custoMO = BigDecimal.ZERO, custoIns = BigDecimal.ZERO, custoMaq = BigDecimal.ZERO;

                // Mão de obra
                for (Object[] f : carregarFuncBrutos(c, idDiario)) {
                    int id = (int) f[0]; String tipo = (String) f[1];
                    BigDecimal horas = (BigDecimal) f[2]; BigDecimal custoHora = (BigDecimal) f[3];
                    BigDecimal valorContratado = (BigDecimal) f[4]; int idpessoa = (int) f[5];
                    BigDecimal custo;
                    if ("CLT".equalsIgnoreCase(tipo)) {
                        if (custoHora == null || custoHora.signum() == 0) custoHora = custoHoraCLT(c, idpessoa);
                        custo = custoHora.multiply(nz(horas)).setScale(2, RoundingMode.HALF_UP);
                    } else if ("DIARISTA".equalsIgnoreCase(tipo)) {
                        custo = valorDiaria(c, idpessoa);
                    } else { // EMPREITA
                        custo = nz(valorContratado);
                    }
                    atualizarCustoFilho(c, "diario_funcionario", id, custoHora, custo);
                    custoMO = custoMO.add(custo);
                }

                // Máquinas
                for (Object[] m : carregarMaqBrutos(c, idDiario)) {
                    int id = (int) m[0]; BigDecimal horas = (BigDecimal) m[1]; BigDecimal valorHora = (BigDecimal) m[2];
                    BigDecimal custo = nz(horas).multiply(nz(valorHora)).setScale(2, RoundingMode.HALF_UP);
                    atualizarCustoFilho(c, "diario_maquina", id, null, custo);
                    custoMaq = custoMaq.add(custo);
                }

                // Insumos (+ baixa de estoque)
                for (Object[] i : carregarInsBrutos(c, idDiario)) {
                    int id = (int) i[0]; int idInsumo = (int) i[1]; BigDecimal qtd = (BigDecimal) i[2];
                    boolean baixado = (boolean) i[3];
                    BigDecimal precoMedio = precoUnidadeInsumo(c, idInsumo)[0];
                    BigDecimal custo = nz(qtd).multiply(precoMedio).setScale(2, RoundingMode.HALF_UP);
                    if (!baixado && qtd.signum() > 0) {
                        estoqueDAO.registrarSaida(c, idInsumo, qtd, dataMov, "Diário " + cab[1]);
                    }
                    try (PreparedStatement st = c.prepareStatement(
                            "UPDATE diario_insumo SET custo_unitario=?, custo=?, baixado=true WHERE id=?")) {
                        st.setBigDecimal(1, precoMedio);
                        st.setBigDecimal(2, custo);
                        st.setInt(3, id);
                        st.executeUpdate();
                    }
                    custoIns = custoIns.add(custo);
                }

                BigDecimal total = custoMO.add(custoIns).add(custoMaq);
                try (PreparedStatement st = c.prepareStatement(
                        "UPDATE diario_campo SET custo_mao_obra=?, custo_insumos=?, custo_maquinas=?, custo_total=?, "
                        + "status='CONCLUIDA', data_realizada=COALESCE(data_realizada, CURRENT_DATE) WHERE iddiario=?")) {
                    st.setBigDecimal(1, custoMO);
                    st.setBigDecimal(2, custoIns);
                    st.setBigDecimal(3, custoMaq);
                    st.setBigDecimal(4, total);
                    st.setInt(5, idDiario);
                    st.executeUpdate();
                }
                c.commit();
            } catch (SQLException | EstoqueInsuficienteException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    public void definirStatus(int idDiario, String status) throws SQLException {
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement st = c.prepareStatement("UPDATE diario_campo SET status=? WHERE iddiario=?")) {
            st.setString(1, status);
            st.setInt(2, idDiario);
            st.executeUpdate();
        }
    }

    /* ===================== CONSULTAS ===================== */

    public DiarioCampo buscarPorId(int id) throws SQLException {
        String sql = "SELECT d.*, ar.propriedadeareaproducao AS area_nome, q.nome_quadra AS quadra_nome, "
                + "al.nomeproduto AS cultura_nome, p.nomepessoa AS resp_nome, t.nome AS tipo_nome "
                + "FROM diario_campo d "
                + "LEFT JOIN areaproducao ar ON ar.idareaproducao=d.id_area "
                + "LEFT JOIN quadra q ON q.idquadra=d.id_quadra "
                + "LEFT JOIN alimento al ON al.idproduto=d.id_cultura "
                + "LEFT JOIN pessoa p ON p.idpessoa=d.id_responsavel "
                + "LEFT JOIN diario_tipo_atividade t ON t.idtipo=d.id_tipo_atividade "
                + "WHERE d.iddiario=?";
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
          + "al.nomeproduto AS cultura_nome, p.nomepessoa AS resp_nome, t.nome AS tipo_nome "
          + "FROM diario_campo d "
          + "LEFT JOIN areaproducao ar ON ar.idareaproducao=d.id_area "
          + "LEFT JOIN quadra q ON q.idquadra=d.id_quadra "
          + "LEFT JOIN alimento al ON al.idproduto=d.id_cultura "
          + "LEFT JOIN pessoa p ON p.idpessoa=d.id_responsavel "
          + "LEFT JOIN diario_tipo_atividade t ON t.idtipo=d.id_tipo_atividade WHERE 1=1");
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

    /** Agregação de custo por dimensão (talhao|cultura|atividade|area|responsavel). */
    public List<Map<String, Object>> relatorioCusto(String dimensao, String de, String ate) throws SQLException {
        String grupo, rotulo, join;
        switch (dimensao == null ? "" : dimensao.toLowerCase()) {
            case "cultura":     grupo = "al.nomeproduto"; rotulo = "al.nomeproduto"; join = "LEFT JOIN alimento al ON al.idproduto=d.id_cultura"; break;
            case "atividade":   grupo = "t.nome"; rotulo = "t.nome"; join = "LEFT JOIN diario_tipo_atividade t ON t.idtipo=d.id_tipo_atividade"; break;
            case "area":        grupo = "ar.propriedadeareaproducao"; rotulo = "ar.propriedadeareaproducao"; join = "LEFT JOIN areaproducao ar ON ar.idareaproducao=d.id_area"; break;
            case "responsavel": grupo = "p.nomepessoa"; rotulo = "p.nomepessoa"; join = "LEFT JOIN pessoa p ON p.idpessoa=d.id_responsavel"; break;
            case "talhao":
            default:            grupo = "q.nome_quadra"; rotulo = "q.nome_quadra"; join = "LEFT JOIN quadra q ON q.idquadra=d.id_quadra"; break;
        }
        StringBuilder sql = new StringBuilder("SELECT COALESCE(" + rotulo + ",'—') AS rotulo, "
                + "COUNT(*) AS qtd_atividades, COALESCE(SUM(d.custo_total),0) AS custo_total, "
                + "COALESCE(SUM(d.custo_mao_obra),0) AS custo_mao_obra, COALESCE(SUM(d.custo_insumos),0) AS custo_insumos, "
                + "COALESCE(SUM(d.custo_maquinas),0) AS custo_maquinas "
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
                m.put("rotulo", rs.getString("rotulo"));
                m.put("qtdAtividades", rs.getInt("qtd_atividades"));
                m.put("custoTotal", rs.getBigDecimal("custo_total"));
                m.put("custoMaoObra", rs.getBigDecimal("custo_mao_obra"));
                m.put("custoInsumos", rs.getBigDecimal("custo_insumos"));
                m.put("custoMaquinas", rs.getBigDecimal("custo_maquinas"));
                out.add(m);
            }
        }
        return out;
    }

    /** Funcionários disponíveis (idpessoa + nome + tipo) para os selects. */
    public List<Map<String, Object>> listarFuncionariosDisponiveis() throws SQLException {
        String sql = "SELECT f.idpessoa, pe.nomepessoa, "
                + "CASE WHEN c.idpessoa IS NOT NULL THEN 'CLT' "
                + "     WHEN di.idpessoa IS NOT NULL THEN 'DIARISTA' "
                + "     WHEN e.idpessoa IS NOT NULL THEN 'EMPREITA' ELSE 'OUTRO' END AS tipo "
                + "FROM funcionario f "
                + "JOIN pessoa pe ON pe.idpessoa=f.idpessoa "
                + "LEFT JOIN funcionarioclt c ON c.idpessoa=f.idpessoa "
                + "LEFT JOIN funcionariodiarista di ON di.idpessoa=f.idpessoa "
                + "LEFT JOIN funcionarioempreita e ON e.idpessoa=f.idpessoa "
                + "ORDER BY pe.nomepessoa";
        List<Map<String, Object>> out = new ArrayList<>();
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement st = c.prepareStatement(sql);
             ResultSet rs = st.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("idPessoa", rs.getInt("idpessoa"));
                m.put("nome", rs.getString("nomepessoa"));
                m.put("tipo", rs.getString("tipo"));
                out.add(m);
            }
        }
        return out;
    }

    /* ===================== helpers de custo ===================== */

    private String resolverTipo(Connection c, int idpessoa) throws SQLException {
        String sql = "SELECT CASE WHEN c.idpessoa IS NOT NULL THEN 'CLT' "
                + "WHEN di.idpessoa IS NOT NULL THEN 'DIARISTA' "
                + "WHEN e.idpessoa IS NOT NULL THEN 'EMPREITA' ELSE 'OUTRO' END "
                + "FROM funcionario f "
                + "LEFT JOIN funcionarioclt c ON c.idpessoa=f.idpessoa "
                + "LEFT JOIN funcionariodiarista di ON di.idpessoa=f.idpessoa "
                + "LEFT JOIN funcionarioempreita e ON e.idpessoa=f.idpessoa WHERE f.idpessoa=?";
        try (PreparedStatement st = c.prepareStatement(sql)) {
            st.setInt(1, idpessoa);
            ResultSet rs = st.executeQuery();
            return rs.next() ? rs.getString(1) : "OUTRO";
        }
    }

    private BigDecimal custoHoraCLT(Connection c, int idpessoa) throws SQLException {
        BigDecimal salario = BigDecimal.ZERO; int carga = 8;
        try {
            Model.Model.Vinculo v = vinculoDAO.buscarAtivo(idpessoa);
            if (v != null) { salario = nz(v.getSalarioMensal()); carga = v.getCargaHorariaDiaria() > 0 ? v.getCargaHorariaDiaria() : 8; }
        } catch (SQLException ignore) {}
        if (salario.signum() == 0) {  // fallback funcionarioclt
            try (PreparedStatement st = c.prepareStatement("SELECT salariomensal, cargahorariadiaria FROM funcionarioclt WHERE idpessoa=?")) {
                st.setInt(1, idpessoa);
                ResultSet rs = st.executeQuery();
                if (rs.next()) { salario = nz(rs.getBigDecimal(1)); int cg = rs.getInt(2); if (cg > 0) carga = cg; }
            }
        }
        if (salario.signum() == 0) return BigDecimal.ZERO;
        BigDecimal horasMes = new BigDecimal(carga).multiply(DIAS_UTEIS_MES);
        return salario.divide(horasMes, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal valorDiaria(Connection c, int idpessoa) throws SQLException {
        try (PreparedStatement st = c.prepareStatement("SELECT valorpordia FROM funcionariodiarista WHERE idpessoa=?")) {
            st.setInt(1, idpessoa);
            ResultSet rs = st.executeQuery();
            return rs.next() ? nz(rs.getBigDecimal(1)) : BigDecimal.ZERO;
        }
    }

    private BigDecimal[] precoUnidadeInsumo(Connection c, int idInsumo) throws SQLException {
        try (PreparedStatement st = c.prepareStatement("SELECT preco_medio FROM insumo WHERE idinsumo=?")) {
            st.setInt(1, idInsumo);
            ResultSet rs = st.executeQuery();
            return new BigDecimal[]{ rs.next() ? nz(rs.getBigDecimal(1)) : BigDecimal.ZERO };
        }
    }

    private String unidadeInsumo(Connection c, int idInsumo) throws SQLException {
        try (PreparedStatement st = c.prepareStatement("SELECT unidade FROM insumo WHERE idinsumo=?")) {
            st.setInt(1, idInsumo);
            ResultSet rs = st.executeQuery();
            return rs.next() ? rs.getString(1) : null;
        }
    }

    private String[] statusNumeroData(Connection c, int idDiario) throws SQLException {
        try (PreparedStatement st = c.prepareStatement("SELECT status, numero_diario, data FROM diario_campo WHERE iddiario=?")) {
            st.setInt(1, idDiario);
            ResultSet rs = st.executeQuery();
            if (!rs.next()) return null;
            Date dt = rs.getDate("data");
            return new String[]{ rs.getString("status"), rs.getString("numero_diario"), dt != null ? dt.toLocalDate().toString() : null };
        }
    }

    private List<Object[]> carregarFuncBrutos(Connection c, int idDiario) throws SQLException {
        List<Object[]> l = new ArrayList<>();
        try (PreparedStatement st = c.prepareStatement("SELECT id, tipo_funcionario, horas_trabalhadas, custo_hora, valor_contratado, idpessoa FROM diario_funcionario WHERE iddiario=?")) {
            st.setInt(1, idDiario);
            ResultSet rs = st.executeQuery();
            while (rs.next()) l.add(new Object[]{ rs.getInt(1), rs.getString(2), rs.getBigDecimal(3), rs.getBigDecimal(4), rs.getBigDecimal(5), rs.getInt(6) });
        }
        return l;
    }
    private List<Object[]> carregarMaqBrutos(Connection c, int idDiario) throws SQLException {
        List<Object[]> l = new ArrayList<>();
        try (PreparedStatement st = c.prepareStatement("SELECT id, horas_trabalhadas, valor_hora FROM diario_maquina WHERE iddiario=?")) {
            st.setInt(1, idDiario);
            ResultSet rs = st.executeQuery();
            while (rs.next()) l.add(new Object[]{ rs.getInt(1), rs.getBigDecimal(2), rs.getBigDecimal(3) });
        }
        return l;
    }
    private List<Object[]> carregarInsBrutos(Connection c, int idDiario) throws SQLException {
        List<Object[]> l = new ArrayList<>();
        try (PreparedStatement st = c.prepareStatement("SELECT id, id_insumo, quantidade, baixado FROM diario_insumo WHERE iddiario=?")) {
            st.setInt(1, idDiario);
            ResultSet rs = st.executeQuery();
            while (rs.next()) l.add(new Object[]{ rs.getInt(1), rs.getInt(2), rs.getBigDecimal(3), rs.getBoolean(4) });
        }
        return l;
    }
    private void atualizarCustoFilho(Connection c, String tabela, int id, BigDecimal custoHora, BigDecimal custo) throws SQLException {
        String sql = custoHora != null
                ? "UPDATE " + tabela + " SET custo_hora=?, custo=? WHERE id=?"
                : "UPDATE " + tabela + " SET custo=? WHERE id=?";
        try (PreparedStatement st = c.prepareStatement(sql)) {
            if (custoHora != null) { st.setBigDecimal(1, custoHora); st.setBigDecimal(2, custo); st.setInt(3, id); }
            else { st.setBigDecimal(1, custo); st.setInt(2, id); }
            st.executeUpdate();
        }
    }

    /* ===================== mapeamento de filhas ===================== */

    private List<DiarioFuncionario> listarFuncionarios(Connection c, int idDiario) throws SQLException {
        List<DiarioFuncionario> l = new ArrayList<>();
        String sql = "SELECT df.*, pe.nomepessoa FROM diario_funcionario df "
                + "LEFT JOIN pessoa pe ON pe.idpessoa=df.idpessoa WHERE df.iddiario=? ORDER BY df.id";
        try (PreparedStatement st = c.prepareStatement(sql)) {
            st.setInt(1, idDiario);
            ResultSet rs = st.executeQuery();
            while (rs.next()) {
                DiarioFuncionario f = new DiarioFuncionario();
                f.setId(rs.getInt("id")); f.setIdDiario(idDiario); f.setIdPessoa(rs.getInt("idpessoa"));
                f.setNomePessoa(rs.getString("nomepessoa")); f.setTipoFuncionario(rs.getString("tipo_funcionario"));
                f.setFuncaoExercida(rs.getString("funcao_exercida")); f.setHorasTrabalhadas(rs.getBigDecimal("horas_trabalhadas"));
                f.setCustoHora(rs.getBigDecimal("custo_hora")); f.setValorContratado(rs.getBigDecimal("valor_contratado"));
                f.setCusto(rs.getBigDecimal("custo"));
                l.add(f);
            }
        }
        return l;
    }
    private List<DiarioMaquina> listarMaquinas(Connection c, int idDiario) throws SQLException {
        List<DiarioMaquina> l = new ArrayList<>();
        try (PreparedStatement st = c.prepareStatement("SELECT * FROM diario_maquina WHERE iddiario=? ORDER BY id")) {
            st.setInt(1, idDiario);
            ResultSet rs = st.executeQuery();
            while (rs.next()) {
                DiarioMaquina m = new DiarioMaquina();
                m.setId(rs.getInt("id")); m.setIdDiario(idDiario); m.setIdMaquina((Integer) rs.getObject("id_maquina"));
                m.setCategoria(rs.getString("categoria")); m.setNome(rs.getString("nome"));
                m.setHorimetroInicial(rs.getBigDecimal("horimetro_inicial")); m.setHorimetroFinal(rs.getBigDecimal("horimetro_final"));
                m.setHorasTrabalhadas(rs.getBigDecimal("horas_trabalhadas")); m.setValorHora(rs.getBigDecimal("valor_hora"));
                m.setCusto(rs.getBigDecimal("custo"));
                l.add(m);
            }
        }
        return l;
    }
    private List<DiarioInsumo> listarInsumos(Connection c, int idDiario) throws SQLException {
        List<DiarioInsumo> l = new ArrayList<>();
        String sql = "SELECT di.*, i.nome AS insumo_nome FROM diario_insumo di "
                + "LEFT JOIN insumo i ON i.idinsumo=di.id_insumo WHERE di.iddiario=? ORDER BY di.id";
        try (PreparedStatement st = c.prepareStatement(sql)) {
            st.setInt(1, idDiario);
            ResultSet rs = st.executeQuery();
            while (rs.next()) {
                DiarioInsumo i = new DiarioInsumo();
                i.setId(rs.getInt("id")); i.setIdDiario(idDiario); i.setIdInsumo(rs.getInt("id_insumo"));
                i.setInsumoNome(rs.getString("insumo_nome")); i.setQuantidade(rs.getBigDecimal("quantidade"));
                i.setUnidade(rs.getString("unidade")); i.setDoseAplicada(rs.getString("dose_aplicada"));
                i.setCustoUnitario(rs.getBigDecimal("custo_unitario")); i.setCusto(rs.getBigDecimal("custo"));
                i.setBaixado(rs.getBoolean("baixado"));
                l.add(i);
            }
        }
        return l;
    }

    private DiarioCampo mapCabecalho(ResultSet rs) throws SQLException {
        DiarioCampo d = new DiarioCampo();
        d.setIdDiario(rs.getInt("iddiario"));
        d.setNumeroDiario(rs.getString("numero_diario"));
        Date data = rs.getDate("data"); d.setData(data != null ? data.toLocalDate() : null);
        d.setIdArea((Integer) rs.getObject("id_area")); d.setAreaNome(rs.getString("area_nome"));
        d.setIdQuadra((Integer) rs.getObject("id_quadra")); d.setQuadraNome(rs.getString("quadra_nome"));
        d.setIdCultura((Integer) rs.getObject("id_cultura")); d.setCulturaNome(rs.getString("cultura_nome"));
        d.setIdResponsavel((Integer) rs.getObject("id_responsavel")); d.setResponsavelNome(rs.getString("resp_nome"));
        d.setIdTipoAtividade(rs.getInt("id_tipo_atividade")); d.setTipoAtividadeNome(rs.getString("tipo_nome"));
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
    private static Date dateOrNow(LocalDate d) { return Date.valueOf(d != null ? d : LocalDate.now()); }
    private static void setIntOrNull(PreparedStatement st, int i, Integer v) throws SQLException {
        if (v != null) st.setInt(i, v); else st.setNull(i, Types.INTEGER);
    }
    private static void setDateOrNull(PreparedStatement st, int i, LocalDate d) throws SQLException {
        if (d != null) st.setDate(i, Date.valueOf(d)); else st.setNull(i, Types.DATE);
    }
    private static void setTimeOrNull(PreparedStatement st, int i, LocalTime t) throws SQLException {
        if (t != null) st.setTime(i, Time.valueOf(t)); else st.setNull(i, Types.TIME);
    }
}
