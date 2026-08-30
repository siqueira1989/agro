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

    /* ===================== CRIAÇÃO ===================== */

    public int inserirCompleto(DiarioCampo d) throws SQLException {
        try (Connection c = new PostgresConnection().getConnection()) {
            c.setAutoCommit(false);
            try {
                int id = proximoId(c);
                int ano = (d.getData() != null ? d.getData() : LocalDate.now()).getYear();
                String numero = ano + "-" + String.format("%06d", id);
                inserirCabecalho(c, id, numero, d);
                LocalDate dataExec = d.getData() != null ? d.getData() : LocalDate.now();
                for (DiarioFuncionario f : d.getFuncionarios()) inserirFuncionario(c, id, f, dataExec);
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

    /**
     * Edita um diário não concluído: substitui cabeçalho, máquinas e insumos, recalcula custos.
     * NÃO mexe em diario_funcionario — mão de obra é gerenciada por {@link #adicionarExecucao}
     * (permite múltiplos dias/equipes por diário, somando o custo total); editar aqui não apaga
     * o histórico de execuções já registradas.
     */
    public void atualizarCompleto(DiarioCampo d) throws SQLException {
        try (Connection c = new PostgresConnection().getConnection()) {
            c.setAutoCommit(false);
            try {
                String status = statusAtual(c, d.getIdDiario());
                if ("CONCLUIDA".equals(status)) {
                    throw new IllegalArgumentException("Diário concluído não pode ser editado.");
                }
                String statusFinal = atualizarCabecalho(c, d, status);

                try (PreparedStatement st = c.prepareStatement(
                        "DELETE FROM diario_maquina WHERE iddiario=?")) {
                    st.setInt(1, d.getIdDiario()); st.executeUpdate();
                }

                // P1-7: o DELETE apagava TODAS as linhas de diario_insumo, inclusive
                // as marcadas baixado=true. A movimentação de estoque continuava no
                // extrato, mas o vínculo com o diário sumia — e uma nova finalização
                // baixava o mesmo insumo outra vez. Linhas já baixadas ficam.
                try (PreparedStatement st = c.prepareStatement(
                        "DELETE FROM diario_insumo WHERE iddiario=? AND baixado = false")) {
                    st.setInt(1, d.getIdDiario()); st.executeUpdate();
                }
                for (DiarioMaquina m : d.getMaquinas()) inserirMaquina(c, d.getIdDiario(), m);
                for (DiarioInsumo i : d.getInsumos()) {
                    // Não reinserir um insumo cuja baixa já foi feita: a linha
                    // preservada acima continua valendo (P1-7).
                    if (!insumoJaBaixado(c, d.getIdDiario(), i.getIdInsumo())) {
                        inserirInsumo(c, d.getIdDiario(), i);
                    }
                }
                // PLANEJADA continua sem custo (previsão); EM_ANDAMENTO/CONCLUIDA calculam.
                // Usa o status EFETIVAMENTE gravado, não o que veio no payload: uma
                // requisição sem o campo "status" rebaixava um diário EM_ANDAMENTO
                // para PLANEJADA e zerava custos já apurados, respondendo "sucesso".
                if (calculaCusto(statusFinal)) recalcularCustos(c, d.getIdDiario());
                else zerarCustos(c, d.getIdDiario());
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback(); throw e;
            } finally { c.setAutoCommit(true); }
        }
    }

    /**
     * Registra uma nova execução (dia + equipe) num diário já em EM_ANDAMENTO, sem apagar as
     * execuções anteriores — o custo total soma automaticamente todas (recalcularCustos agrega
     * todas as linhas de diario_funcionario do diário).
     */
    public void adicionarExecucao(int idDiario, LocalDate dataExecucao, List<DiarioFuncionario> funcionarios) throws SQLException {
        try (Connection c = new PostgresConnection().getConnection()) {
            c.setAutoCommit(false);
            try {
                String status = statusAtual(c, idDiario);
                if (!"EM_ANDAMENTO".equals(status)) throw new SQLException("Só é possível registrar execução em diários EM_ANDAMENTO.");
                for (DiarioFuncionario f : funcionarios) inserirFuncionario(c, idDiario, f, dataExecucao);
                recalcularCustos(c, idDiario);
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

    /**
     * Grava o cabeçalho e devolve o status que ficou efetivamente no banco.
     *
     * @param statusAtualDoBanco status antes da edição, lido pelo chamador
     */
    private String atualizarCabecalho(Connection c, DiarioCampo d, String statusAtualDoBanco)
            throws SQLException {
        // O status so muda se o payload trouxer um valor VALIDO e a transicao for
        // permitida; caso contrario o atual e preservado. Antes, qualquer valor
        // diferente de EM_ANDAMENTO virava PLANEJADA — inclusive a ausencia do
        // campo —, o que rebaixava o diario e, na sequencia, zerava seus custos.
        // Era tambem um desvio da maquina de estados: CANCELADA voltava a
        // PLANEJADA por "acao=update", sem passar por definirStatus.
        String novoStatus = statusAtualDoBanco;
        String pedido = d.getStatus() == null ? null : d.getStatus().trim().toUpperCase();
        if ("EM_ANDAMENTO".equals(pedido) || "PLANEJADA".equals(pedido)) {
            if (pedido.equals(statusAtualDoBanco)
                    || !STATUS_CONHECIDOS.contains(statusAtualDoBanco)
                    || TRANSICOES_PERMITIDAS.get(pedido).contains(statusAtualDoBanco)) {
                novoStatus = pedido;
            } else {
                throw new IllegalArgumentException(
                    "Transição de status inválida: " + statusAtualDoBanco + " para " + pedido + ".");
            }
        }
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
        return novoStatus;
    }

    private int proximoId(Connection c) throws SQLException {
        try (PreparedStatement st = c.prepareStatement("SELECT nextval('diario_campo_iddiario_seq')");
             ResultSet rs = st.executeQuery()) { rs.next(); return rs.getInt(1); }
    }

    private void inserirFuncionario(Connection c, int idDiario, DiarioFuncionario f, LocalDate dataExecucao) throws SQLException {
        String tipo = f.getTipoFuncionario() != null ? f.getTipoFuncionario() : resolverTipo(c, f.getIdPessoa());
        String sql = "INSERT INTO diario_funcionario (iddiario, idpessoa, tipo_funcionario, funcao_exercida, "
                + "horas_trabalhadas, custo_hora, valor_contratado, custo, data_execucao) VALUES (?,?,?,?,?,0,?,0,?)";
        try (PreparedStatement st = c.prepareStatement(sql)) {
            st.setInt(1, idDiario); st.setInt(2, f.getIdPessoa()); st.setString(3, tipo);
            st.setString(4, f.getFuncaoExercida()); st.setBigDecimal(5, nz(f.getHorasTrabalhadas()));
            st.setBigDecimal(6, nz(f.getValorContratado()));
            st.setDate(7, Date.valueOf(f.getDataExecucao() != null ? f.getDataExecucao() : dataExecucao));
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

        // Pares (pessoa, dia) de diaristas tocados aqui: ao fim, os OUTROS diários
        // do mesmo dia precisam ser reajustados, senão o rateio fica pela metade
        // (um diário com a diária cheia e o outro com a metade).
        List<int[]> diaristasParaRatear = new ArrayList<>();
        List<LocalDate> diasParaRatear = new ArrayList<>();

        // Mão de obra
        for (Object[] f : carregarFuncBrutos(c, idDiario)) {
            int id = (int) f[0]; String tipo = (String) f[1]; BigDecimal horas = (BigDecimal) f[2];
            BigDecimal valorContratado = (BigDecimal) f[4];
            int idpessoa = (int) f[5];
            LocalDate dataExec = (LocalDate) f[6];
            BigDecimal custoHora = BigDecimal.ZERO, custo;

            if ("CLT".equalsIgnoreCase(tipo)) {
                custoHora = custoHoraCLT(c, idpessoa, dataExec);
                custo = custoHora.multiply(nz(horas)).setScale(2, RoundingMode.HALF_UP);

            } else if ("DIARISTA".equalsIgnoreCase(tipo)) {
                // P1-6: a diária é por PESSOA e por DIA, não por lançamento. Se o
                // diarista trabalhou em três atividades no mesmo dia, antes eram
                // cobradas três diárias. Agora a diária é rateada entre elas.
                custo = custoDiariaRateada(c, idpessoa, dataExec);
                diaristasParaRatear.add(new int[]{ idpessoa });
                diasParaRatear.add(dataExec);

            } else if ("EMPREITA".equalsIgnoreCase(tipo)) {
                // P1-6: usava funcionarioempreita.valorfixoacordado — o valor do
                // CONTRATO INTEIRO — em cada atividade. Um empreiteiro lançado em
                // cinco atividades custava cinco vezes o contrato, inflando custo
                // por hectare, custo por planta e o rateio por talhão da Safra.
                // Passa a usar o valor acordado PARA ESTA ATIVIDADE, que a coluna
                // valor_contratado já gravava e ninguém lia.
                custo = nz(valorContratado).setScale(2, RoundingMode.HALF_UP);
                if (custo.signum() == 0) {
                    Util.LogUtil.aviso(DiarioCampoDAO.class,
                            "Empreita sem valor_contratado no diário " + idDiario
                          + " (pessoa " + idpessoa + "): custo lançado como zero. "
                          + "Informe o valor acordado para a atividade.", null);
                }

            } else {
                custo = BigDecimal.ZERO;
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

        for (int k = 0; k < diaristasParaRatear.size(); k++) {
            ratearDiariaNosDemaisDiarios(c, diaristasParaRatear.get(k)[0], diasParaRatear.get(k), idDiario);
        }
    }

    /**
     * Valor que cabe a UM lançamento de diária: a diária dividida pelo número de
     * lançamentos daquela pessoa naquele dia.
     */
    private BigDecimal custoDiariaRateada(Connection c, int idpessoa, LocalDate data) throws SQLException {
        BigDecimal diaria = valorDiaria(c, idpessoa);
        int lancamentos = contarLancamentosNoDia(c, idpessoa, data, "DIARISTA");
        return diaria.divide(BigDecimal.valueOf(Math.max(lancamentos, 1)), 2, RoundingMode.HALF_UP);
    }

    /**
     * Propaga o rateio da diária para os OUTROS diários do mesmo dia.
     *
     * <p>Sem isto, o rateio só funcionava no caso de um lançamento por dia — que
     * já estava certo antes. Com dois diários: o primeiro gravava a diária cheia
     * (só existia um lançamento na hora do cálculo) e o segundo gravava metade;
     * ninguém voltava para corrigir o primeiro, e o dia somava 1,5 diária. Com
     * três, somava 1,83.</p>
     *
     * <p>Diários já CONCLUIDA não são alterados — o custo deles é um retrato
     * fechado —, mas continuam contando no divisor, e o fato vai para o log para
     * que a diferença não passe despercebida.</p>
     */
    private void ratearDiariaNosDemaisDiarios(Connection c, int idpessoa, LocalDate data, int idDiarioOrigem)
            throws SQLException {
        if (data == null) return;

        BigDecimal valor = custoDiariaRateada(c, idpessoa, data);
        List<Integer> afetados = new ArrayList<>();

        String sel = "SELECT df.id, df.iddiario, d.status FROM diario_funcionario df "
                   + "JOIN diario_campo d ON d.iddiario = df.iddiario "
                   + "WHERE df.idpessoa=? AND df.data_execucao=? "
                   + "AND upper(df.tipo_funcionario)='DIARISTA' "
                   + "AND COALESCE(d.status,'') <> 'CANCELADA' AND df.iddiario <> ?";
        try (PreparedStatement st = c.prepareStatement(sel)) {
            st.setInt(1, idpessoa); st.setDate(2, Date.valueOf(data)); st.setInt(3, idDiarioOrigem);
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    int idLinha = rs.getInt(1);
                    int idDiario = rs.getInt(2);
                    String status = rs.getString(3);
                    if ("CONCLUIDA".equals(status)) {
                        Util.LogUtil.aviso(DiarioCampoDAO.class,
                                "Diário " + idDiario + " está CONCLUIDA e mantém a diária antiga "
                              + "do diarista " + idpessoa + " em " + data
                              + "; o rateio correto agora seria " + valor + ".", null);
                        continue;
                    }
                    try (PreparedStatement up = c.prepareStatement(
                            "UPDATE diario_funcionario SET custo=? WHERE id=?")) {
                        up.setBigDecimal(1, valor); up.setInt(2, idLinha); up.executeUpdate();
                    }
                    if (!afetados.contains(idDiario)) afetados.add(idDiario);
                }
            }
        }

        for (int idDiario : afetados) atualizarTotaisCabecalho(c, idDiario);
    }

    /** Recalcula custo_mao_obra e custo_total do cabeçalho a partir das linhas. */
    private void atualizarTotaisCabecalho(Connection c, int idDiario) throws SQLException {
        String sql = "UPDATE diario_campo d SET "
                   + "  custo_mao_obra = COALESCE((SELECT SUM(custo) FROM diario_funcionario "
                   + "                              WHERE iddiario = d.iddiario), 0), "
                   + "  custo_total = COALESCE((SELECT SUM(custo) FROM diario_funcionario "
                   + "                           WHERE iddiario = d.iddiario), 0) "
                   + "              + d.custo_insumos + d.custo_maquinas "
                   + "WHERE d.iddiario = ?";
        try (PreparedStatement st = c.prepareStatement(sql)) {
            st.setInt(1, idDiario); st.executeUpdate();
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

    /**
     * Altera o status do diário respeitando as transições válidas
     * (P1-7 da auditoria de 29/08/2026).
     *
     * <h4>O que este método fazia antes</h4>
     * <p>Um {@code UPDATE} cru, sem validar nada. Isso abria dois buracos reais:</p>
     *
     * <p><b>1. Concluir sem custo e sem baixa.</b> Ir de PLANEJADA direto para
     * CONCLUIDA por aqui mantinha o custo zerado (a criação chamou
     * {@code zerarCustos}) e <b>não baixava o insumo</b>. A consolidação da
     * Safra, que soma apenas {@code status='CONCLUIDA'}, incorporava a atividade
     * valendo zero.</p>
     *
     * <p><b>2. Baixa de estoque em duplicidade.</b> Voltar de CONCLUIDA para
     * EM_ANDAMENTO reabria o diário; ao editá-lo, {@code atualizarCompleto}
     * apagava as linhas de {@code diario_insumo} — inclusive as marcadas
     * {@code baixado=true} — e, ao finalizar de novo, o estoque era baixado
     * outra vez pelas mesmas quantidades.</p>
     *
     * <h4>Regras agora</h4>
     * <ul>
     *   <li>CONCLUIDA só é atingida por {@link #finalizar(int)}, que recalcula
     *       custos e faz a baixa dentro de uma transação;</li>
     *   <li>diário CONCLUIDA não volta atrás: estornar exige decisão explícita,
     *       não um clique de mudança de status;</li>
     *   <li>demais transições continuam livres.</li>
     * </ul>
     */
    public void definirStatus(int idDiario, String status) throws SQLException {
        String novo = status == null ? "" : status.trim().toUpperCase();

        // Estas sao violacoes de REGRA DE NEGOCIO, nao falhas tecnicas: por isso
        // IllegalArgumentException, que o ControllerDiarioCampo traduz em HTTP 400
        // com a mensagem visivel. Como SQLException, caiam no catch generico e o
        // usuario recebia "Erro interno" com um codigo opaco.
        if ("CONCLUIDA".equals(novo)) {
            throw new IllegalArgumentException(
                "Para concluir a atividade use a ação Finalizar: é ela que recalcula "
              + "os custos e dá baixa nos insumos.");
        }
        if (!TRANSICOES_PERMITIDAS.containsKey(novo)) {
            throw new IllegalArgumentException("Status inválido: " + status + ".");
        }

        try (Connection c = new PostgresConnection().getConnection()) {
            String atual = statusAtual(c, idDiario);
            if (atual == null) throw new IllegalArgumentException("Diário não encontrado.");
            if (atual.equals(novo)) return;

            if ("CONCLUIDA".equals(atual)) {
                throw new IllegalArgumentException(
                    "Diário concluído não pode voltar para " + novo + ". O estoque já foi "
                  + "baixado; reabrir exigiria estorno das movimentações.");
            }
            // Diario com status legado (nulo, minusculo, valor antigo) nao pode
            // ficar travado: registra o fato e deixa passar.
            if (!STATUS_CONHECIDOS.contains(atual)) {
                Util.LogUtil.aviso(DiarioCampoDAO.class,
                        "Diário " + idDiario + " com status legado '" + atual
                      + "'; transição para " + novo + " permitida.", null);
            } else if (!TRANSICOES_PERMITIDAS.get(novo).contains(atual)) {
                throw new IllegalArgumentException(
                    "Transição de status inválida: " + atual + " para " + novo + ".");
            }

            try (PreparedStatement st = c.prepareStatement(
                    "UPDATE diario_campo SET status=? WHERE iddiario=?")) {
                st.setString(1, novo);
                st.setInt(2, idDiario);
                st.executeUpdate();
            }
        }
    }

    /** Status de origem aceitos para cada status de destino. */
    private static final Map<String, java.util.Set<String>> TRANSICOES_PERMITIDAS = Map.of(
        // CANCELADA e reversivel de proposito: cancelar por engano nao pode
        // obrigar a recriar o diario do zero, com novo numero e sem o historico
        // de execucoes. O que continua irreversivel e CONCLUIDA, porque ali o
        // estoque ja foi baixado.
        "PLANEJADA",    java.util.Set.of("EM_ANDAMENTO", "CANCELADA"),
        "EM_ANDAMENTO", java.util.Set.of("PLANEJADA", "CANCELADA"),
        "CANCELADA",    java.util.Set.of("PLANEJADA", "EM_ANDAMENTO")
    );

    private static final java.util.Set<String> STATUS_CONHECIDOS =
        java.util.Set.of("PLANEJADA", "EM_ANDAMENTO", "CONCLUIDA", "CANCELADA");

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
                // P1-11: era "* 22.0" fixo. O divisor passa a ser o numero real de
                // dias uteis do mes de referencia, calculado em Java e passado como
                // parametro — mesma fonte usada por custoHoraCLT.
                + "CASE WHEN c.idpessoa IS NOT NULL THEN round(COALESCE(v.salario_mensal, c.salariomensal, 0) / "
                + "        (GREATEST(COALESCE(v.carga_horaria_diaria, c.cargahorariadiaria, 8),1) * CAST(? AS numeric)), 2) "
                + "     WHEN di.idpessoa IS NOT NULL THEN COALESCE(di.valorpordia, 0) "
                + "     WHEN e.idpessoa IS NOT NULL THEN COALESCE(e.valorfixoacordado, 0) ELSE 0 END AS custo_ref "
                + "FROM funcionario f JOIN pessoa pe ON pe.idpessoa=f.idpessoa "
                + "LEFT JOIN funcionarioclt c ON c.idpessoa=f.idpessoa "
                + "LEFT JOIN funcionariodiarista di ON di.idpessoa=f.idpessoa "
                + "LEFT JOIN funcionarioempreita e ON e.idpessoa=f.idpessoa "
                + joinVinc
                // CLT depende do vínculo ativo na data (salário/carga horária vêm dele); diarista/empreita
                // não dependem de vínculo — só precisam estar ativos (situacaopessoa), senão sumiam da
                // lista quando nunca tiveram um registro em vinculo_empregaticio.
                + (data != null ? "WHERE (c.idpessoa IS NOT NULL AND v.id_vinculo IS NOT NULL) "
                        + "OR (c.idpessoa IS NULL AND f.situacaopessoa = true) " : "")
                + "ORDER BY pe.nomepessoa";
        List<Map<String, Object>> out = new ArrayList<>();
        // O "?" dos dias uteis aparece na lista do SELECT, antes dos parametros do
        // JOIN — a ordem dos indices segue a posicao no texto do SQL.
        int diasUteis = Service.ParametrosFolhaService.diasUteisDoMesDe(data);
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement st = c.prepareStatement(sql)) {
            st.setBigDecimal(1, BigDecimal.valueOf(Math.max(diasUteis, 1)));
            if (data != null) { st.setDate(2, Date.valueOf(data)); st.setDate(3, Date.valueOf(data)); }
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

    /**
     * Custo da hora de um CLT para efeito de custeio da atividade.
     *
     * <p>P1-11: dividia por uma constante fixa de 22 dias úteis, enquanto a
     * folha usava os dias úteis reais do mês. A hora do mesmo funcionário
     * custava um valor aqui e outro na folha — em fevereiro, 10% de diferença.
     * O divisor agora vem de {@link Service.ParametrosFolhaService}, que conta
     * os dias úteis do mês <b>da atividade</b>, feriados descontados.</p>
     */
    private BigDecimal custoHoraCLT(Connection c, int idpessoa, LocalDate dataExec) throws SQLException {
        BigDecimal salario = BigDecimal.ZERO; int carga = 8;
        try {
            // P1-8: era buscarAtivo(idpessoa), que abria OUTRA conexão dentro
            // desta transação — uma por funcionário da atividade — e lia fora
            // dela. Agora reaproveita a conexão corrente.
            Vinculo v = vinculoDAO.buscarAtivo(c, idpessoa);
            if (v != null) { salario = nz(v.getSalarioMensal()); carga = v.getCargaHorariaDiaria() > 0 ? v.getCargaHorariaDiaria() : 8; }
        } catch (SQLException ignore) {}
        if (salario.signum() == 0) {
            try (PreparedStatement st = c.prepareStatement("SELECT salariomensal, cargahorariadiaria FROM funcionarioclt WHERE idpessoa=?")) {
                st.setInt(1, idpessoa); ResultSet rs = st.executeQuery();
                if (rs.next()) { salario = nz(rs.getBigDecimal(1)); int cg = rs.getInt(2); if (cg > 0) carga = cg; }
            }
        }
        if (salario.signum() == 0) return BigDecimal.ZERO;
        return Service.ParametrosFolhaService.custoHoraProdutiva(
                salario, carga, java.time.YearMonth.from(dataExec != null ? dataExec : LocalDate.now()));
    }

    /**
     * Quantos lançamentos deste tipo a pessoa tem na mesma data, em diários não
     * cancelados. É o divisor do rateio da diária (P1-6).
     */
    private int contarLancamentosNoDia(Connection c, int idpessoa, LocalDate data, String tipo)
            throws SQLException {
        if (data == null) return 1;
        String sql = "SELECT COUNT(*) FROM diario_funcionario df "
                   + "JOIN diario_campo d ON d.iddiario = df.iddiario "
                   + "WHERE df.idpessoa = ? AND df.data_execucao = ? "
                   // COALESCE: comparar NULL com 'CANCELADA' devolve NULL, nao TRUE,
                   // e a linha sumia do divisor.
                   + "AND upper(df.tipo_funcionario) = ? AND COALESCE(d.status,'') <> 'CANCELADA'";
        try (PreparedStatement st = c.prepareStatement(sql)) {
            st.setInt(1, idpessoa);
            st.setDate(2, Date.valueOf(data));
            st.setString(3, tipo);
            try (ResultSet rs = st.executeQuery()) {
                return rs.next() ? Math.max(rs.getInt(1), 1) : 1;
            }
        }
    }

    private BigDecimal valorDiaria(Connection c, int idpessoa) throws SQLException {
        try (PreparedStatement st = c.prepareStatement("SELECT valorpordia FROM funcionariodiarista WHERE idpessoa=?")) {
            st.setInt(1, idpessoa); ResultSet rs = st.executeQuery();
            return rs.next() ? nz(rs.getBigDecimal(1)) : BigDecimal.ZERO;
        }
    }

    private BigDecimal valorFixoEmpreita(Connection c, int idpessoa) throws SQLException {
        try (PreparedStatement st = c.prepareStatement("SELECT valorfixoacordado FROM funcionarioempreita WHERE idpessoa=?")) {
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

    /** A baixa deste insumo neste diário já foi efetivada? (P1-7) */
    private boolean insumoJaBaixado(Connection c, int idDiario, int idInsumo) throws SQLException {
        try (PreparedStatement st = c.prepareStatement(
                "SELECT 1 FROM diario_insumo WHERE iddiario=? AND id_insumo=? AND baixado=true LIMIT 1")) {
            st.setInt(1, idDiario); st.setInt(2, idInsumo);
            try (ResultSet rs = st.executeQuery()) {
                return rs.next();
            }
        }
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
        try (PreparedStatement st = c.prepareStatement(
                "SELECT id, tipo_funcionario, horas_trabalhadas, custo_hora, valor_contratado, "
              + "idpessoa, data_execucao FROM diario_funcionario WHERE iddiario=?")) {
            st.setInt(1, idDiario);
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) l.add(new Object[]{
                    rs.getInt(1), rs.getString(2), rs.getBigDecimal(3), rs.getBigDecimal(4),
                    rs.getBigDecimal(5), rs.getInt(6),
                    rs.getDate(7) != null ? rs.getDate(7).toLocalDate() : null });
            }
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
                f.setCusto(rs.getBigDecimal("custo"));
                Date de = rs.getDate("data_execucao"); f.setDataExecucao(de != null ? de.toLocalDate() : null);
                l.add(f);
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
