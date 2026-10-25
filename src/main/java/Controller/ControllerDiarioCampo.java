package Controller;

import Model.Dao.DiarioCampoDAO;
import Model.Dao.EstoqueInsuficienteException;
import Model.Dao.SafraDAO;
import Model.Dao.TipoAtividadeDAO;
import Model.Model.DiarioCampo;
import Model.Model.DiarioInsumo;
import Model.Model.DiarioFuncionario;
import Model.Model.DiarioDespesa;
import com.google.gson.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Servlet do módulo Diário de Campo. URL: /ControllerDiarioCampo
 *
 * GET  (sem acao)                      → lista (filtros: area, quadra, cultura, tipo, status, de, ate)
 * GET  ?id=                            → um diário (com funcionários/máquinas/insumos)
 * GET  ?tiposatividade=1               → tipos de atividade ativos
 * GET  ?funcionarios=1                 → funcionários (idpessoa+nome+tipo) p/ selects
 * GET  ?despesascatalogo=1             → catálogo de despesas/custos (nome+classificação+valor ref.) p/ selects
 * GET  ?relatorio=talhao|cultura|atividade|area|responsavel &de=&ate=  → agregação de custo
 * POST acao=create|update|finalizar|addexecucao|removerfuncionario|removerdespesa|status|addtipoatividade|recalcularpessoa
 */
public class ControllerDiarioCampo extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private DiarioCampoDAO dao;
    private TipoAtividadeDAO tipoDAO;
    private SafraDAO safraDAO;

    private final Gson gson = new GsonBuilder()
        .registerTypeAdapter(LocalDate.class, (JsonSerializer<LocalDate>) (s, t, c) -> new JsonPrimitive(s.toString()))
        .registerTypeAdapter(LocalDate.class, (JsonDeserializer<LocalDate>) (j, t, c) ->
                j == null || j.isJsonNull() || j.getAsString().isBlank() ? null : LocalDate.parse(j.getAsString()))
        .registerTypeAdapter(LocalTime.class, (JsonSerializer<LocalTime>) (s, t, c) -> new JsonPrimitive(s.toString()))
        .registerTypeAdapter(LocalTime.class, (JsonDeserializer<LocalTime>) (j, t, c) -> {
            if (j == null || j.isJsonNull() || j.getAsString().isBlank()) return null;
            String v = j.getAsString();
            return LocalTime.parse(v.length() == 5 ? v + ":00" : v);
        })
        .create();

    @Override
    public void init() {
        dao = new DiarioCampoDAO();
        tipoDAO = new TipoAtividadeDAO();
        safraDAO = new SafraDAO();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        try (PrintWriter out = resp.getWriter()) {
            if (req.getParameter("tiposatividade") != null) { out.print(gson.toJson(tipoDAO.listar(true))); return; }
            if (req.getParameter("funcionarios") != null)  {
                String dataStr = req.getParameter("data");
                java.time.LocalDate dataFiltro = null;
                if (dataStr != null && !dataStr.isBlank()) {
                    try { dataFiltro = java.time.LocalDate.parse(dataStr); } catch (Exception ignore) {}
                }
                out.print(gson.toJson(dao.listarFuncionariosDisponiveis(dataFiltro))); return;
            }
            if (req.getParameter("despesascatalogo") != null) {
                out.print(gson.toJson(dao.listarDespesasDisponiveis())); return;
            }
            if (req.getParameter("relatorio") != null) {
                out.print(gson.toJson(dao.relatorioCusto(req.getParameter("relatorio"),
                        req.getParameter("de"), req.getParameter("ate")))); return;
            }
            String id = req.getParameter("id");
            if (id != null && !id.isBlank()) {
                DiarioCampo d = dao.buscarPorId(Integer.parseInt(id.trim()));
                if (d != null) out.print(gson.toJson(d));
                else writeJson(resp, 404, false, "Diário não encontrado.");
                return;
            }
            out.print(gson.toJson(dao.listar(
                    intOrNull(req.getParameter("area")), intOrNull(req.getParameter("quadra")),
                    intOrNull(req.getParameter("cultura")), intOrNull(req.getParameter("tipo")),
                    req.getParameter("status"), req.getParameter("de"), req.getParameter("ate"))));
        } catch (Exception e) {
            // P1-15/P2-20: pilha completa no log; ao usuário vai só o código.
            String cod = Util.LogUtil.erro(ControllerDiarioCampo.class, "Falha tratada em ControllerDiarioCampo.", e);
            writeJson(resp, 500, false, "Erro ao carregar diários: " + cod);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding(StandardCharsets.UTF_8.name());
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = req.getReader()) { String l; while ((l = r.readLine()) != null) sb.append(l); }
        JsonObject jo = gson.fromJson(sb.toString(), JsonObject.class);
        if (jo == null || !jo.has("acao")) { writeJson(resp, 400, false, "Ação não informada."); return; }
        try {
            switch (jo.get("acao").getAsString().toLowerCase()) {
                case "create":          handleSalvar(jo, resp, false); break;
                case "update":          handleSalvar(jo, resp, true); break;
                case "finalizar":       handleFinalizar(jo, resp); break;
                case "addexecucao":     handleAddExecucao(jo, resp); break;
                case "removerfuncionario": handleRemoverFuncionario(jo, resp); break;
                case "removerdespesa":  handleRemoverDespesa(jo, resp); break;
                case "status":          handleStatus(jo, resp); break;
                case "addtipoatividade":handleAddTipo(jo, resp); break;
                case "recalcularpessoa":handleRecalcularPessoa(jo, resp); break;
                default: writeJson(resp, 400, false, "Ação inválida.");
            }
        } catch (EstoqueInsuficienteException e) {
            writeJson(resp, 400, false, e.getMessage());
        } catch (IllegalArgumentException e) {
            // Violação de regra de negócio (transição de status inválida, diário
            // concluído, jornada implausível). A mensagem é escrita para o usuário
            // e precisa chegar até ele; antes caía no catch genérico e virava
            // "Erro interno" com um código opaco.
            writeJson(resp, 400, false, e.getMessage());
        } catch (Exception e) {
            // P1-15/P2-20: pilha completa no log; ao usuário vai só o código.
            String cod = Util.LogUtil.erro(ControllerDiarioCampo.class, "Falha tratada em ControllerDiarioCampo.", e);
            writeJson(resp, 500, false, "Erro interno: " + cod);
        }
    }

    private void handleSalvar(JsonObject jo, HttpServletResponse resp, boolean update) throws Exception {
        DiarioCampo d = gson.fromJson(jo, DiarioCampo.class);

        // Validações
        if (d.getIdTipoAtividade() == 0 || !tipoDAO.existe(d.getIdTipoAtividade())) {
            writeJson(resp, 400, false, "Tipo de atividade é obrigatório e deve ser válido."); return;
        }
        if (d.getIdArea() == null || d.getIdQuadra() == null) {
            writeJson(resp, 400, false, "Área e talhão são obrigatórios."); return;
        }
        if (d.getIdResponsavel() == null) {
            writeJson(resp, 400, false, "Responsável é obrigatório."); return;
        }
        if (!quadraPertenceArea(d.getIdQuadra(), d.getIdArea())) {
            writeJson(resp, 400, false, "O talhão selecionado não pertence à área informada."); return;
        }
        if (d.getHoraInicio() != null && d.getHoraFim() != null && d.getHoraFim().isBefore(d.getHoraInicio())) {
            writeJson(resp, 400, false, "Hora fim não pode ser anterior à hora início."); return;
        }
        for (DiarioFuncionario f : d.getFuncionarios()) {
            if (f.getHorasTrabalhadas() != null && f.getHorasTrabalhadas().signum() < 0) {
                writeJson(resp, 400, false, "Horas trabalhadas não podem ser negativas."); return;
            }
        }
        if (!validarRegimeCaixa(d.getIdTipoAtividade(), d.getFuncionarios(), resp)) return;
        // Nova regra: quantidade não é digitada — é calculada no servidor (DiarioCampoDAO.inserirInsumo),
        // de uma de duas formas mutuamente exclusivas: dosePorUnidade × doseTotalAplicada (DEFENSIVO,
        // aplicação por calda) OU quantidadeDireta (ADUBO, aplicação direta — ex.: nº de sacos × peso do
        // saco, já calculado no cliente). Aqui só valida que UMA das duas formas veio completa e positiva;
        // o valor de i.getQuantidade() enviado pelo cliente é sempre ignorado.
        for (DiarioInsumo i : d.getInsumos()) {
            if (i.getIdInsumo() == 0) {
                writeJson(resp, 400, false, "Cada insumo precisa de um produto selecionado."); return;
            }
            String forma = i.getFormaAplicacao();
            if (forma != null && !forma.isBlank()) {
                // Adubo por forma (V27): sacos de peso padrão, toneladas ou kg + nº de plantas.
                if (!java.util.Set.of("SACO", "TONELADA", "KG").contains(forma)) {
                    writeJson(resp, 400, false, "Forma de aplicação do adubo inválida."); return;
                }
                if ("SACO".equals(forma) && (i.getPesoSacoKg() == null
                        || i.getPesoSacoKg().stripTrailingZeros().scale() > 0
                        || !DiarioCampoDAO.PESOS_SACO_KG.contains(i.getPesoSacoKg().intValue()))) {
                    writeJson(resp, 400, false, "Escolha o tipo de saco do adubo: 5, 10, 25 ou 50 kg."); return;
                }
                if (i.getQtdInformada() == null || i.getQtdInformada().signum() <= 0) {
                    writeJson(resp, 400, false, "Informe a quantidade de adubo aplicada (nº de sacos, toneladas ou kg) maior que zero."); return;
                }
                if (i.getPlantasAplicadas() == null || i.getPlantasAplicadas() <= 0) {
                    writeJson(resp, 400, false, "Informe o nº de plantas que receberam o adubo (maior que zero)."); return;
                }
                continue;
            }
            boolean temQuantidadeDireta = i.getQuantidadeDireta() != null && i.getQuantidadeDireta().signum() > 0;
            boolean temDose = i.getDosePorUnidade() != null && i.getDosePorUnidade().signum() > 0
                    && i.getDoseTotalAplicada() != null && i.getDoseTotalAplicada().signum() > 0;
            if (!temQuantidadeDireta && !temDose) {
                writeJson(resp, 400, false,
                        "Informe a quantidade aplicada de cada insumo: dose por litro/kg + dose total aplicada (defensivo), "
                        + "ou a quantidade aplicada direta (adubo)."); return;
            }
        }
        for (DiarioDespesa dsp : d.getDespesas()) {
            if (dsp.getIdDespesaCusto() == 0 || dsp.getQuantidade() == null || dsp.getQuantidade().signum() <= 0) {
                writeJson(resp, 400, false, "Cada despesa precisa de um item do catálogo e quantidade maior que zero."); return;
            }
        }

        // Safra: OBRIGATÓRIA. Resolve pelo talhão + data; trava lançamentos em safra FINALIZADA e vincula a safra.
        java.time.LocalDate dataAtiv = d.getData() != null ? d.getData() : java.time.LocalDate.now();
        String[] sf = safraDAO.resolverSafra(d.getIdQuadra(), dataAtiv);
        if (sf == null) {
            writeJson(resp, 400, false, "É obrigatório ter uma Safra cadastrada que inclua este talhão e cubra a data da atividade. Cadastre/ajuste a safra antes de lançar."); return;
        }
        if ("FINALIZADA".equals(sf[2])) {
            writeJson(resp, 400, false, "A safra \"" + sf[1] + "\" está FINALIZADA — não é permitido lançar/editar atividades nela."); return;
        }
        d.setIdSafra(Integer.parseInt(sf[0]));

        // "Concluído" direto no cadastro: grava como EM_ANDAMENTO (satisfaz a pré-condição de
        // finalizar()) e encadeia finalizar() na mesma requisição — reaproveita o cálculo de custo
        // final e a baixa de estoque já existentes, sem duplicar lógica de negócio.
        boolean concluirDireto = "CONCLUIDA".equals(d.getStatus());
        if (concluirDireto) d.setStatus("EM_ANDAMENTO");

        if (update) {
            if (d.getIdDiario() == 0) { writeJson(resp, 400, false, "ID do diário obrigatório."); return; }
            dao.atualizarCompleto(d);   // substitui cabeçalho + filhas e recalcula custos
            if (concluirDireto) dao.finalizar(d.getIdDiario());
            writeJson(resp, 200, true, "Diário atualizado com sucesso!");
        } else {
            int id = dao.inserirCompleto(d);
            if (concluirDireto) dao.finalizar(id);
            Map<String, Object> m = okPayload("Diário cadastrado com sucesso!");
            m.put("iddiario", id);
            m.put("numeroDiario", d.getNumeroDiario());
            writeJsonRaw(resp, 200, m);
        }
    }

    /** Registra uma nova execução (dia + equipe) num diário EM_ANDAMENTO, somando ao custo total sem apagar as anteriores. */
    private void handleAddExecucao(JsonObject jo, HttpServletResponse resp) throws Exception {
        int id = getInt(jo, "iddiario");
        if (id == 0) { writeJson(resp, 400, false, "ID do diário obrigatório."); return; }
        String dataStr = getStr(jo, "dataExecucao");
        if (dataStr == null || dataStr.isBlank()) { writeJson(resp, 400, false, "Data da execução é obrigatória."); return; }
        java.time.LocalDate dataExec;
        try { dataExec = java.time.LocalDate.parse(dataStr); }
        catch (Exception e) { writeJson(resp, 400, false, "Data da execução inválida."); return; }

        List<DiarioFuncionario> funcionarios = new ArrayList<>();
        if (jo.has("funcionarios") && jo.get("funcionarios").isJsonArray()) {
            for (JsonElement je : jo.getAsJsonArray("funcionarios")) {
                DiarioFuncionario f = gson.fromJson(je, DiarioFuncionario.class);
                if (f.getHorasTrabalhadas() != null && f.getHorasTrabalhadas().signum() < 0) {
                    writeJson(resp, 400, false, "Horas trabalhadas não podem ser negativas."); return;
                }
                funcionarios.add(f);
            }
        }
        List<DiarioDespesa> despesas = new ArrayList<>();
        if (jo.has("despesas") && jo.get("despesas").isJsonArray()) {
            for (JsonElement je : jo.getAsJsonArray("despesas")) {
                DiarioDespesa d = gson.fromJson(je, DiarioDespesa.class);
                if (d.getIdDespesaCusto() == 0) {
                    writeJson(resp, 400, false, "Cada despesa precisa de um item do catálogo selecionado."); return;
                }
                if (d.getQuantidade() != null && d.getQuantidade().signum() <= 0) {
                    writeJson(resp, 400, false, "Quantidade da despesa deve ser maior que zero."); return;
                }
                if (d.getValorUnitario() != null && d.getValorUnitario().signum() < 0) {
                    writeJson(resp, 400, false, "Valor da despesa não pode ser negativo."); return;
                }
                despesas.add(d);
            }
        }
        if (funcionarios.isEmpty() && despesas.isEmpty()) {
            writeJson(resp, 400, false, "Informe ao menos um funcionário ou uma despesa para a execução."); return;
        }
        DiarioCampo atual = dao.buscarPorId(id);
        if (atual == null) { writeJson(resp, 400, false, "Diário não encontrado."); return; }
        if (!validarRegimeCaixa(atual.getIdTipoAtividade(), funcionarios, resp)) return;
        dao.adicionarExecucao(id, dataExec, funcionarios, despesas);
        writeJson(resp, 200, true, "Execução registrada com sucesso! Custo total recalculado.");
    }

    /** Remove um lançamento de mão de obra já salvo (funcionário/horas errados numa execução). */
    private void handleRemoverFuncionario(JsonObject jo, HttpServletResponse resp) throws Exception {
        int idDiario = getInt(jo, "iddiario");
        int idLinha = getInt(jo, "id");
        if (idDiario == 0 || idLinha == 0) { writeJson(resp, 400, false, "ID do diário e do lançamento são obrigatórios."); return; }
        dao.removerFuncionario(idDiario, idLinha);
        writeJson(resp, 200, true, "Lançamento removido. Custo recalculado.");
    }

    /** Remove uma despesa já salva (item/valor errado numa execução). */
    private void handleRemoverDespesa(JsonObject jo, HttpServletResponse resp) throws Exception {
        int idDiario = getInt(jo, "iddiario");
        int idLinha = getInt(jo, "id");
        if (idDiario == 0 || idLinha == 0) { writeJson(resp, 400, false, "ID do diário e da despesa são obrigatórios."); return; }
        dao.removerDespesa(idDiario, idLinha);
        writeJson(resp, 200, true, "Despesa removida. Custo recalculado.");
    }

    private void handleFinalizar(JsonObject jo, HttpServletResponse resp) throws Exception {
        int id = getInt(jo, "iddiario");
        if (id == 0) { writeJson(resp, 400, false, "ID do diário obrigatório."); return; }
        dao.finalizar(id);   // pode lançar EstoqueInsuficienteException → 400
        writeJson(resp, 200, true, "Atividade finalizada: custos calculados e baixa de estoque realizada.");
    }

    /**
     * Recalcula o custo de todos os diários (EM_ANDAMENTO/CONCLUIDA) que têm
     * lançamento de mão de obra desta pessoa. Uso administrativo: depois de
     * corrigir um valor cadastrado (salário, diária, empreitada) que estava
     * errado/zerado, os diários já fechados com esse funcionário continuam com
     * o custo antigo até serem recalculados manualmente — finalizar() só roda
     * uma vez, no momento da conclusão.
     */
    private void handleRecalcularPessoa(JsonObject jo, HttpServletResponse resp) throws Exception {
        int idpessoa = getInt(jo, "idpessoa");
        if (idpessoa == 0) { writeJson(resp, 400, false, "idpessoa é obrigatório."); return; }
        List<Integer> diarios = dao.listarDiariosComPessoa(idpessoa);
        for (int id : diarios) dao.recalcularCustos(id);
        writeJson(resp, 200, true, "Custo recalculado em " + diarios.size() + " diário(s).");
    }

    private void handleStatus(JsonObject jo, HttpServletResponse resp) throws Exception {
        int id = getInt(jo, "iddiario");
        String status = getStr(jo, "status");
        if (id == 0 || status == null) { writeJson(resp, 400, false, "ID e status são obrigatórios."); return; }
        if (!status.matches("PLANEJADA|EM_ANDAMENTO|CONCLUIDA|CANCELADA")) {
            writeJson(resp, 400, false, "Status inválido."); return;
        }
        dao.definirStatus(id, status);
        writeJson(resp, 200, true, "Status atualizado.");
    }

    private void handleAddTipo(JsonObject jo, HttpServletResponse resp) throws Exception {
        String nome = getStr(jo, "nome");
        if (nome == null || nome.isBlank()) { writeJson(resp, 400, false, "Nome do tipo é obrigatório."); return; }
        int idtipo = tipoDAO.inserir(nome.trim());
        Map<String, Object> m = okPayload("Tipo de atividade adicionado.");
        m.put("idtipo", idtipo);
        writeJsonRaw(resp, 200, m);
    }

    /* ------------------------- helpers ------------------------- */

    /**
     * Na Colheita, cada funcionário precisa indicar o regime de trabalho (sistema padrão do
     * contrato, ou produção por caixa) e a quantidade de caixas que colheu — isso vale pros DOIS
     * regimes, porque é volume físico colhido, não forma de pagamento: se só quem está em
     * PRODUCAO informasse caixa, o total do lote (Módulo Colheita) subcontaria a colheita de quem
     * ficou no regime normal do contrato. Preço por caixa continua exclusivo de PRODUCAO, já que
     * só ali o preço afeta o pagamento. Fora da Colheita, regime é NULO à força (nunca confia no
     * que o cliente mandou): o formulário reaproveita a mesma linha entre trocas de tipo de
     * atividade no navegador, e uma linha deixada preenchida antes de o usuário mudar o tipo não
     * pode vazar quantidade_caixas/valor_regime para fora da Colheita.
     *
     * Processamento (V29) usa o mesmo regime: CLT/Diarista/Empreita pelas horas (REGIME_SISTEMA,
     * caixas opcional) e Embalador por caixa (PRODUCAO, caixas + preço obrigatórios).
     *
     * Funcionário de Produção só existe pago por caixa: na Colheita só o cargo Colhedor, no
     * Processamento só o Embalador, sempre em PRODUCAO; nas demais atividades não entra (não tem
     * hora/diária para calcular). Tipo/cargo vêm do banco (DiarioCampoDAO.cargosProducao), nunca
     * do cliente.
     * Escreve o erro na resposta e devolve false quando a validação falha.
     */
    private boolean validarRegimeCaixa(int idTipoAtividade, List<DiarioFuncionario> funcionarios,
            HttpServletResponse resp) throws Exception {
        String nomeTipo = tipoDAO.buscarNome(idTipoAtividade);
        String nome = nomeTipo != null ? nomeTipo.toLowerCase() : "";
        boolean ehColheita = nome.contains("colh");
        boolean ehProcessamento = nome.contains("processa");
        List<Integer> ids = new ArrayList<>();
        for (DiarioFuncionario f : funcionarios) ids.add(f.getIdPessoa());
        Map<Integer, String> cargos = dao.cargosProducao(ids);
        for (DiarioFuncionario f : funcionarios) {
            if (!cargos.containsKey(f.getIdPessoa())) continue;
            String cargo = cargos.get(f.getIdPessoa());
            f.setTipoFuncionario("PRODUCAO");
            if (ehColheita && "Embalador".equalsIgnoreCase(cargo)) {
                writeJson(resp, 400, false, "Funcionário de Produção com cargo Embalador não pode ser lançado na Colheita.");
                return false;
            }
            if (ehProcessamento && "Colhedor".equalsIgnoreCase(cargo)) {
                writeJson(resp, 400, false, "Funcionário de Produção com cargo Colhedor não pode ser lançado no Processamento.");
                return false;
            }
            if (!ehColheita && !ehProcessamento) {
                writeJson(resp, 400, false, "Funcionário de Produção (pago por caixa) só pode ser lançado na Colheita ou no Processamento.");
                return false;
            }
            if (!"PRODUCAO".equals(f.getRegimeTrabalho())) {
                writeJson(resp, 400, false, "Funcionário de Produção deve ser lançado no regime Produção (por caixa).");
                return false;
            }
        }
        if (!ehColheita && !ehProcessamento) {
            for (DiarioFuncionario f : funcionarios) {
                f.setRegimeTrabalho(null); f.setQuantidadeCaixas(null); f.setPrecoPorCaixa(null);
            }
            return true;
        }
        for (DiarioFuncionario f : funcionarios) {
            String regime = f.getRegimeTrabalho();
            if (regime == null || regime.isBlank()) {
                writeJson(resp, 400, false, "Indique o regime de trabalho de cada funcionário (sistema ou produção por caixa).");
                return false;
            }
            if (!"REGIME_SISTEMA".equals(regime) && !"PRODUCAO".equals(regime)) {
                writeJson(resp, 400, false, "Regime de trabalho inválido: " + regime + "."); return false;
            }
            // Processamento: caixas só é obrigatório para quem é pago por caixa.
            boolean caixasObrigatorio = ehColheita || "PRODUCAO".equals(regime);
            if (caixasObrigatorio && (f.getQuantidadeCaixas() == null || f.getQuantidadeCaixas().signum() <= 0)) {
                writeJson(resp, 400, false, ehColheita
                        ? "Informe a quantidade de caixas colhidas (maior que zero) para cada funcionário."
                        : "Informe a quantidade de caixas (maior que zero) para quem é pago por caixa.");
                return false;
            }
            if ("PRODUCAO".equals(regime)) {
                if (f.getPrecoPorCaixa() == null || f.getPrecoPorCaixa().signum() <= 0) {
                    writeJson(resp, 400, false, "Informe o preço por caixa (maior que zero) para funcionários em regime de produção.");
                    return false;
                }
            }
        }
        return true;
    }

    private boolean quadraPertenceArea(int idQuadra, int idArea) throws Exception {
        try (Connection c = new Util.PostgresConnection().getConnection();
             PreparedStatement st = c.prepareStatement("SELECT 1 FROM quadra WHERE idquadra=? AND idareaproducao=?")) {
            st.setInt(1, idQuadra); st.setInt(2, idArea);
            try (ResultSet rs = st.executeQuery()) { return rs.next(); }
        }
    }

    private Integer intOrNull(String s) { try { return s == null || s.isBlank() ? null : Integer.parseInt(s.trim()); } catch (Exception e) { return null; } }
    private int getInt(JsonObject jo, String k) { try { return jo.has(k) && !jo.get(k).isJsonNull() ? jo.get(k).getAsInt() : 0; } catch (Exception e) { return 0; } }
    private String getStr(JsonObject jo, String k) { return jo.has(k) && !jo.get(k).isJsonNull() ? jo.get(k).getAsString() : null; }
    private Map<String, Object> okPayload(String msg) { Map<String, Object> m = new LinkedHashMap<>(); m.put("ok", true); m.put("msg", msg); return m; }

    private void writeJson(HttpServletResponse resp, int status, boolean ok, String msg) throws IOException {
        Map<String, Object> m = new LinkedHashMap<>(); m.put("ok", ok); m.put("msg", msg);
        writeJsonRaw(resp, status, m);
    }
    private void writeJsonRaw(HttpServletResponse resp, int status, Map<String, Object> payload) throws IOException {
        resp.setContentType("application/json"); resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setStatus(status);
        try (PrintWriter out = resp.getWriter()) { out.print(gson.toJson(payload)); }
    }
}
