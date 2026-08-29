package Controller;

import Model.Dao.*;
import Model.Model.*;
import Model.Model.FuncionarioCLT.StatusEmprego;
import Model.Model.ValeFuncionario.TipoVale;
import Service.FolhaCalculoRuralService;
import com.google.gson.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

/**
 * Servlet unificado do módulo CLT.
 * URL: /ControllerCLT
 *
 * GET  ?acao=listar[&status=ATIVO|DESLIGADO|AFASTADO]
 * GET  ?acao=perfil&id={idFuncionario}&periodo={YYYY-MM}
 * POST acao=atualizarSalario | atualizarStatus | registrarVale |
 *           excluirVale | marcarValeDescontado | registrarFalta |
 *           excluirFalta | calcularFechamento
 */
public class ControllerCLT extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private FuncionarioCLTDAO  cltDAO;
    private ValeFuncionarioDAO valeDAO;
    private FaltaFuncionarioDAO faltaDAO;
    private PontoEletronicoDAO  pontoDAO;
    private FechamentoFolhaDAO  fechamentoDAO;
    private VinculoDAO          vinculoDAO;
    private FuncionarioDAO      funcDAO;

    private final Gson gson = new GsonBuilder()
        .registerTypeAdapter(LocalDate.class,
            (JsonSerializer<LocalDate>) (src, t, ctx) -> new JsonPrimitive(src.toString()))
        .registerTypeAdapter(java.time.LocalTime.class,
            (JsonSerializer<java.time.LocalTime>) (src, t, ctx) ->
                new JsonPrimitive(src.toString())) // "HH:mm" / "HH:mm:ss"
        .create();

    @Override
    public void init() {
        cltDAO       = new FuncionarioCLTDAO();
        valeDAO      = new ValeFuncionarioDAO();
        faltaDAO     = new FaltaFuncionarioDAO();
        pontoDAO     = new PontoEletronicoDAO();
        fechamentoDAO = new FechamentoFolhaDAO();
        vinculoDAO   = new VinculoDAO();
        funcDAO      = new FuncionarioDAO();
    }

    /* ── GET ─────────────────────────────────────────────────────────────── */

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());

        String acao = req.getParameter("acao");
        try (PrintWriter out = resp.getWriter()) {
            if ("perfil".equals(acao)) {
                handleGetPerfil(req, resp, out);
            } else {
                // listar (padrão)
                String statusParam = req.getParameter("status");
                List<FuncionarioCLT> lista;
                if (statusParam != null && !statusParam.isBlank()) {
                    StatusEmprego st = StatusEmprego.valueOf(statusParam.toUpperCase());
                    lista = cltDAO.listarPorStatus(st);
                } else {
                    lista = cltDAO.listTodos();
                }
                out.print(gson.toJson(lista));
            }
        } catch (Exception e) {
            // P1-15/P2-20: pilha completa no log; ao usuário vai só o código.
            String cod = Util.LogUtil.erro(ControllerCLT.class, "Falha tratada em ControllerCLT.", e);
            writeJson(resp, 500, false, "Erro: " + cod);
        }
    }

    private void handleGetPerfil(HttpServletRequest req, HttpServletResponse resp,
                                  PrintWriter out) throws Exception {
        String idStr     = req.getParameter("id");
        String periodo   = req.getParameter("periodo");

        if (idStr == null || idStr.isBlank()) {
            writeJson(resp, 400, false, "ID do funcionário obrigatório.");
            return;
        }
        int id = Integer.parseInt(idStr.trim());
        if (periodo == null || periodo.isBlank()) {
            periodo = LocalDate.now().toString().substring(0, 7); // YYYY-MM
        }

        FuncionarioCLT f = cltDAO.getCLTById(id);
        if (f == null) {
            writeJson(resp, 404, false, "Funcionário CLT não encontrado.");
            return;
        }

        List<PontoEletronico> pontos  = pontoDAO.listarPorMes(id, periodo);
        List<FaltaFuncionario> faltas = faltaDAO.listarPorMes(id, periodo);
        List<ValeFuncionario>  vales  = valeDAO.listarPorMes(id, periodo);

        int totalMinExtras = pontoDAO.somarMinutosExtras(id, periodo);
        int diasUteisPeriodo = contarDiasUteis(YearMonth.parse(periodo));

        // Vínculo (período de trabalho) que cobre o mês selecionado; se não
        // houver, usa o vínculo ativo. Os termos do header vêm dele.
        Vinculo vinc = vinculoDAO.buscarPorData(id, YearMonth.parse(periodo).atDay(1));
        if (vinc == null) vinc = vinculoDAO.buscarAtivo(id);

        BigDecimal salarioRef = vinc != null ? vinc.getSalarioMensal() : f.getSalarioMensal();
        BigDecimal vheRef      = vinc != null ? vinc.getValorHoraExtra() : f.getValorHoraExtra();
        int jornadaRef         = vinc != null ? vinc.getCargaHorariaDiaria() : f.getCargaHorariaDiaria();
        BigDecimal vheEfetivo  = valorHoraExtraEfetivo(salarioRef, jornadaRef, vheRef, diasUteisPeriodo);

        Map<String, Object> perfil = new LinkedHashMap<>();
        perfil.put("funcionario", f);
        perfil.put("vinculo", vinc);
        perfil.put("vinculos", vinculoDAO.listarPorPessoa(id));
        perfil.put("periodo", periodo);
        perfil.put("valorHoraExtraEfetivo", vheEfetivo);
        perfil.put("pontos", pontos);
        perfil.put("faltas", faltas);
        perfil.put("vales", vales);
        perfil.put("totalMinutosExtras", totalMinExtras);
        perfil.put("horasExtrasFormatado", formatarMinutos(totalMinExtras));
        perfil.put("totalVales", valeDAO.somarPorMes(id, periodo));
        perfil.put("faltasInjustificadas", faltaDAO.contarInjustificadas(id, periodo));
        perfil.put("diasComPonto", pontos.size());

        // fechamento já salvo para o período (se existir)
        perfil.put("fechamento", fechamentoDAO.buscarPorPeriodo(id, periodo));

        out.print(gson.toJson(perfil));
    }

    /* ── POST ────────────────────────────────────────────────────────────── */

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding(StandardCharsets.UTF_8.name());
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String linha;
            while ((linha = reader.readLine()) != null) sb.append(linha);
        }
        JsonObject jo = gson.fromJson(sb.toString(), JsonObject.class);
        if (jo == null || !jo.has("acao")) {
            writeJson(resp, 400, false, "Ação não informada.");
            return;
        }

        String acao = jo.get("acao").getAsString();
        try {
            switch (acao.toLowerCase()) {
                case "atualizarsalario"       -> handleAtualizarSalario(jo, resp);
                case "atualizarstatus"        -> handleAtualizarStatus(jo, resp);
                case "registrarvale"          -> handleRegistrarVale(jo, resp);
                case "excluirvale"            -> handleExcluirVale(jo, resp);
                case "marcarvaledescontado"   -> handleMarcarValeDescontado(jo, resp);
                case "registrarfalta"         -> handleRegistrarFalta(jo, resp);
                case "excluirfalta"           -> handleExcluirFalta(jo, resp);
                case "calcularfechamento"     -> handleCalcularFechamento(jo, resp);
                case "desligarvinculo"        -> handleDesligarVinculo(jo, resp);
                case "readmitir"              -> handleReadmitir(jo, resp);
                default -> writeJson(resp, 400, false, "Ação inválida: " + acao);
            }
        } catch (Exception e) {
            // P1-15/P2-20: pilha completa no log; ao usuário vai só o código.
            String cod = Util.LogUtil.erro(ControllerCLT.class, "Falha tratada em ControllerCLT.", e);
            writeJson(resp, 500, false, "Erro interno: " + cod);
        }
    }

    private void handleAtualizarSalario(JsonObject jo, HttpServletResponse resp)
            throws IOException, SQLException {
        int    id              = getInt(jo, "idpessoa");
        BigDecimal salario     = getBD(jo, "salariomensal");
        BigDecimal horaExtra   = getBD(jo, "valorhoraextra");
        int    cargaHoraria    = jo.has("cargahorariadiaria")
                                 ? jo.get("cargahorariadiaria").getAsInt() : 8;
        String tipoAtivStr     = getString(jo, "tipoatividaderural");
        Model.Model.TipoAtividadeRural tipoAtividade = Model.Model.TipoAtividadeRural.LAVOURA;
        if (tipoAtivStr != null) {
            try { tipoAtividade = Model.Model.TipoAtividadeRural.valueOf(tipoAtivStr.toUpperCase()); }
            catch (IllegalArgumentException ignored) { }
        }

        if (id == 0 || salario == null || salario.compareTo(BigDecimal.ZERO) <= 0) {
            writeJson(resp, 400, false, "ID e salário válido são obrigatórios.");
            return;
        }
        // Grava no VÍNCULO ativo (fonte da verdade). Permite também editar um
        // vínculo específico se "idvinculo" for informado.
        int idVinculo = getInt(jo, "idvinculo");
        Vinculo alvo = idVinculo != 0 ? vinculoDAO.buscarPorId(idVinculo) : vinculoDAO.buscarAtivo(id);
        if (alvo != null) {
            alvo.setSalarioMensal(salario);
            alvo.setValorHoraExtra(horaExtra != null ? horaExtra : BigDecimal.ZERO);
            alvo.setCargaHorariaDiaria(cargaHoraria);
            alvo.setTipoAtividadeRural(tipoAtividade);
            vinculoDAO.atualizarTermos(alvo);
        }
        // Mantém funcionarioclt sincronizado durante a transição
        cltDAO.updateSalario(id, salario,
                horaExtra != null ? horaExtra : BigDecimal.ZERO, cargaHoraria, tipoAtividade);
        writeJson(resp, 200, true, "Salário atualizado com sucesso!");
    }

    private void handleAtualizarStatus(JsonObject jo, HttpServletResponse resp)
            throws IOException, SQLException {
        int    id     = getInt(jo, "idpessoa");
        String status = getString(jo, "statusemprego");
        if (id == 0 || status == null) {
            writeJson(resp, 400, false, "ID e status são obrigatórios.");
            return;
        }
        StatusEmprego st;
        try { st = StatusEmprego.valueOf(status.toUpperCase()); }
        catch (IllegalArgumentException e) {
            writeJson(resp, 400, false, "Status inválido: " + status);
            return;
        }
        cltDAO.updateStatusEmprego(id, st);
        writeJson(resp, 200, true, "Status atualizado para " + st.name() + ".");
    }

    private void handleRegistrarVale(JsonObject jo, HttpServletResponse resp)
            throws IOException, SQLException {
        int        id    = getInt(jo, "idpessoa");
        BigDecimal valor = getBD(jo, "valor");
        String     data  = getString(jo, "datavale");
        String     desc  = getString(jo, "descricao");
        String     tipo  = getString(jo, "tipovale");

        if (id == 0 || valor == null || valor.compareTo(BigDecimal.ZERO) <= 0 || data == null) {
            writeJson(resp, 400, false, "Funcionário, valor e data são obrigatórios.");
            return;
        }
        ValeFuncionario v = new ValeFuncionario();
        v.setIdFuncionario(id);
        v.setValor(valor);
        v.setDataVale(LocalDate.parse(data));
        v.setDescricao(desc != null ? desc : "");
        try { v.setTipoVale(TipoVale.valueOf(tipo.toUpperCase())); }
        catch (Exception e) { v.setTipoVale(TipoVale.OUTROS); }
        valeDAO.registrar(v);
        writeJson(resp, 200, true, "Vale registrado com sucesso!");
    }

    private void handleExcluirVale(JsonObject jo, HttpServletResponse resp)
            throws IOException, SQLException {
        int id = getInt(jo, "idvale");
        if (id == 0) { writeJson(resp, 400, false, "ID do vale obrigatório."); return; }
        valeDAO.excluir(id);
        writeJson(resp, 200, true, "Vale excluído.");
    }

    private void handleMarcarValeDescontado(JsonObject jo, HttpServletResponse resp)
            throws IOException, SQLException {
        int id = getInt(jo, "idvale");
        if (id == 0) { writeJson(resp, 400, false, "ID do vale obrigatório."); return; }
        valeDAO.marcarDescontado(id);
        writeJson(resp, 200, true, "Vale marcado como descontado.");
    }

    private void handleRegistrarFalta(JsonObject jo, HttpServletResponse resp)
            throws IOException, SQLException {
        int    id          = getInt(jo, "idpessoa");
        String dataStr     = getString(jo, "datafalta");
        boolean justificada = jo.has("justificada") && jo.get("justificada").getAsBoolean();
        String motivo      = getString(jo, "motivo");

        if (id == 0 || dataStr == null) {
            writeJson(resp, 400, false, "Funcionário e data são obrigatórios.");
            return;
        }
        FaltaFuncionario f = new FaltaFuncionario();
        f.setIdFuncionario(id);
        f.setDataFalta(LocalDate.parse(dataStr));
        f.setJustificada(justificada);
        f.setMotivo(motivo != null ? motivo : "");
        faltaDAO.registrar(f);
        writeJson(resp, 200, true, "Falta registrada.");
    }

    private void handleExcluirFalta(JsonObject jo, HttpServletResponse resp)
            throws IOException, SQLException {
        int id = getInt(jo, "idfalta");
        if (id == 0) { writeJson(resp, 400, false, "ID da falta obrigatório."); return; }
        faltaDAO.excluir(id);
        writeJson(resp, 200, true, "Falta excluída.");
    }

    private void handleCalcularFechamento(JsonObject jo, HttpServletResponse resp)
            throws IOException, SQLException {
        int    id      = getInt(jo, "idpessoa");
        String periodo = getString(jo, "periodo");

        if (id == 0 || periodo == null) {
            writeJson(resp, 400, false, "ID e período (YYYY-MM) são obrigatórios.");
            return;
        }

        FuncionarioCLT f = cltDAO.getCLTById(id);
        if (f == null) {
            writeJson(resp, 404, false, "Funcionário CLT não encontrado.");
            return;
        }

        // Cálculo rural (DSR, domingo 100%, adicional noturno, tolerância)
        FechamentoFolha fechamento = new FolhaCalculoRuralService().calcularFechamento(f, periodo);

        fechamentoDAO.salvar(fechamento);

        // Marca os vales do mês como descontados
        valeDAO.marcarDescontadosPorMes(id, periodo);

        resp.setContentType("application/json");
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setStatus(200);
        try (PrintWriter out = resp.getWriter()) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("ok", true);
            payload.put("msg", "Fechamento calculado com sucesso!");
            payload.put("fechamento", fechamento);
            out.print(gson.toJson(payload));
        }
    }

    /** Desliga o vínculo ativo da pessoa (encerra o período de trabalho). */
    private void handleDesligarVinculo(JsonObject jo, HttpServletResponse resp)
            throws IOException, SQLException {
        int idPessoa = getInt(jo, "idpessoa");
        if (idPessoa == 0) { writeJson(resp, 400, false, "ID obrigatório."); return; }
        Vinculo ativo = vinculoDAO.buscarAtivo(idPessoa);
        if (ativo == null) {
            writeJson(resp, 400, false, "Não há vínculo ativo para desligar.");
            return;
        }
        String dataFimStr = getString(jo, "datafim");
        LocalDate dataFim = (dataFimStr != null && !dataFimStr.isBlank())
                ? LocalDate.parse(dataFimStr) : LocalDate.now();
        vinculoDAO.desligar(ativo.getIdVinculo(), dataFim, getString(jo, "motivo"));
        cltDAO.updateStatusEmprego(idPessoa, StatusEmprego.DESLIGADO);
        funcDAO.deleteFuncionario(idPessoa, false); // situação inativa
        writeJson(resp, 200, true, "Vínculo desligado em " + dataFim + ".");
    }

    /** Readmite a pessoa: abre um novo vínculo, se não houver vínculo ativo. */
    private void handleReadmitir(JsonObject jo, HttpServletResponse resp)
            throws IOException, SQLException {
        int idPessoa = getInt(jo, "idpessoa");
        if (idPessoa == 0) { writeJson(resp, 400, false, "ID obrigatório."); return; }
        if (vinculoDAO.buscarAtivo(idPessoa) != null) {
            writeJson(resp, 400, false, "Já existe um vínculo ativo. Desligue-o antes de readmitir.");
            return;
        }
        String dataAdmStr = getString(jo, "dataadmissao");
        LocalDate dataAdm = (dataAdmStr != null && !dataAdmStr.isBlank())
                ? LocalDate.parse(dataAdmStr) : LocalDate.now();
        Vinculo v = new Vinculo();
        v.setIdPessoa(idPessoa);
        v.setTipoFuncionario(TipoFuncionario.CLT);
        v.setCargo(getString(jo, "cargo"));
        v.setDataAdmissao(dataAdm);
        v.setStatus("ATIVO");
        vinculoDAO.inserir(v);
        cltDAO.updateStatusEmprego(idPessoa, StatusEmprego.ATIVO);
        funcDAO.deleteFuncionario(idPessoa, true); // reativa situação
        writeJson(resp, 200, true, "Funcionário readmitido. Novo vínculo aberto em " + dataAdm + ".");
    }

    /* ── helpers ─────────────────────────────────────────────────────────── */

    /**
     * Valor efetivo da hora extra. Se o funcionário tem um valor cadastrado
     * (&gt; 0), usa-o; caso contrário calcula a partir do salário:
     *   valorHora = salário / (diasUteis × jornada);  extra = valorHora × 1,50
     */
    private BigDecimal valorHoraExtraEfetivo(FuncionarioCLT f, int diasUteis) {
        return valorHoraExtraEfetivo(f.getSalarioMensal(), f.getCargaHorariaDiaria(),
                f.getValorHoraExtra(), diasUteis);
    }

    private BigDecimal valorHoraExtraEfetivo(BigDecimal salario, int jornada,
                                             BigDecimal manual, int diasUteis) {
        if (manual != null && manual.compareTo(BigDecimal.ZERO) > 0) {
            return manual;
        }
        if (salario == null || salario.compareTo(BigDecimal.ZERO) <= 0
                || diasUteis <= 0 || jornada <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal horasMes = BigDecimal.valueOf((long) diasUteis * jornada);
        return salario.divide(horasMes, 4, RoundingMode.HALF_UP)
                      .multiply(new BigDecimal("1.50"))
                      .setScale(2, RoundingMode.HALF_UP);
    }

    private int contarDiasUteis(YearMonth ym) {
        int count = 0;
        LocalDate d = ym.atDay(1);
        LocalDate fim = ym.atEndOfMonth();
        while (!d.isAfter(fim)) {
            int dow = d.getDayOfWeek().getValue(); // 1=Seg … 7=Dom
            if (dow >= 1 && dow <= 5) count++;
            d = d.plusDays(1);
        }
        return count;
    }

    private String formatarMinutos(int minutos) {
        return String.format("%02d:%02d", minutos / 60, minutos % 60);
    }

    private void writeJson(HttpServletResponse resp, int status, boolean ok, String msg)
            throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setStatus(status);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ok", ok);
        m.put("msg", msg);
        try (PrintWriter out = resp.getWriter()) {
            out.print(gson.toJson(m));
        }
    }

    private int getString_asInt(JsonObject jo, String key) {
        return jo.has(key) && !jo.get(key).isJsonNull() ? jo.get(key).getAsInt() : 0;
    }
    private int getInt(JsonObject jo, String key) {
        try { return jo.has(key) && !jo.get(key).isJsonNull() ? jo.get(key).getAsInt() : 0; }
        catch (Exception e) { return 0; }
    }
    private BigDecimal getBD(JsonObject jo, String key) {
        try { return jo.has(key) && !jo.get(key).isJsonNull()
                ? jo.get(key).getAsBigDecimal() : null; }
        catch (Exception e) { return null; }
    }
    private String getString(JsonObject jo, String key) {
        return jo.has(key) && !jo.get(key).isJsonNull()
                ? jo.get(key).getAsString() : null;
    }
}
