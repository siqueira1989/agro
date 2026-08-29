package Controller;

import Model.Dao.*;
import Model.Model.*;
import com.google.gson.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.*;

/**
 * Servlet do módulo DIARISTA.
 * URL: /ControllerDiarista
 *
 * O diarista recebe um VALOR FIXO POR DIA trabalhado, não tem horas extras
 * e pode ter vales (descontados no pagamento).
 *
 * GET  ?acao=perfil&id={idpessoa}&periodo={YYYY-MM}
 * POST acao=atualizardiaria | marcarpresenca | removerpresenca |
 *           registrarvale | excluirvale | marcarvaledescontado
 */
public class ControllerDiarista extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private FuncionarioDiaristaDAO diaristaDAO;
    private PontoEletronicoDAO     pontoDAO;
    private ValeFuncionarioDAO     valeDAO;
    private VinculoDAO             vinculoDAO;

    private final Gson gson = new GsonBuilder()
        .registerTypeAdapter(LocalDate.class,
            (JsonSerializer<LocalDate>) (s, t, c) -> new JsonPrimitive(s.toString()))
        .registerTypeAdapter(LocalTime.class,
            (JsonSerializer<LocalTime>) (s, t, c) -> new JsonPrimitive(s.toString()))
        .create();

    @Override
    public void init() {
        diaristaDAO = new FuncionarioDiaristaDAO();
        pontoDAO    = new PontoEletronicoDAO();
        valeDAO     = new ValeFuncionarioDAO();
        vinculoDAO  = new VinculoDAO();
    }

    /* ── GET ─────────────────────────────────────────────────────── */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        try (PrintWriter out = resp.getWriter()) {
            if (!"perfil".equals(req.getParameter("acao"))) {
                writeJson(resp, 400, false, "Ação inválida.");
                return;
            }
            handlePerfil(req, resp, out);
        } catch (Exception e) {
            // P1-15/P2-20: pilha completa no log; ao usuário vai só o código.
            String cod = Util.LogUtil.erro(ControllerDiarista.class, "Falha tratada em ControllerDiarista.", e);
            writeJson(resp, 500, false, "Erro: " + cod);
        }
    }

    private void handlePerfil(HttpServletRequest req, HttpServletResponse resp, PrintWriter out)
            throws Exception {
        String idStr   = req.getParameter("id");
        String periodo = req.getParameter("periodo");
        if (idStr == null || idStr.isBlank()) { writeJson(resp, 400, false, "ID obrigatório."); return; }
        int id = Integer.parseInt(idStr.trim());
        if (periodo == null || periodo.isBlank()) periodo = YearMonth.now().toString();

        FuncionarioDiarista f = diaristaDAO.getById(id);
        if (f == null) { writeJson(resp, 404, false, "Diarista não encontrado."); return; }

        Vinculo vinc = vinculoDAO.buscarPorData(id, YearMonth.parse(periodo).atDay(1));
        if (vinc == null) vinc = vinculoDAO.buscarAtivo(id);

        List<PontoEletronico> presencas = pontoDAO.listarPorMes(id, periodo);
        int diasTrabalhados = presencas.size();
        List<ValeFuncionario> vales = valeDAO.listarPorMes(id, periodo);
        BigDecimal totalVales = valeDAO.somarPorMes(id, periodo);
        if (totalVales == null) totalVales = BigDecimal.ZERO;

        BigDecimal valorDia = f.getValorPorDia() != null ? f.getValorPorDia() : BigDecimal.ZERO;
        BigDecimal bruto   = valorDia.multiply(BigDecimal.valueOf(diasTrabalhados)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal liquido = bruto.subtract(totalVales).setScale(2, RoundingMode.HALF_UP);
        if (liquido.compareTo(BigDecimal.ZERO) < 0) liquido = BigDecimal.ZERO;

        Map<String, Object> perfil = new LinkedHashMap<>();
        perfil.put("funcionario", f);
        perfil.put("vinculo", vinc);
        perfil.put("vinculos", vinculoDAO.listarPorPessoa(id));
        perfil.put("periodo", periodo);
        perfil.put("valorPorDia", valorDia);
        perfil.put("presencas", presencas);
        perfil.put("diasTrabalhados", diasTrabalhados);
        perfil.put("vales", vales);
        perfil.put("totalVales", totalVales);
        perfil.put("valorBruto", bruto);
        perfil.put("valorLiquido", liquido);
        out.print(gson.toJson(perfil));
    }

    /* ── POST ────────────────────────────────────────────────────── */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding(StandardCharsets.UTF_8.name());
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = req.getReader()) {
            String l; while ((l = r.readLine()) != null) sb.append(l);
        }
        JsonObject jo = gson.fromJson(sb.toString(), JsonObject.class);
        if (jo == null || !jo.has("acao")) { writeJson(resp, 400, false, "Ação não informada."); return; }
        try {
            switch (jo.get("acao").getAsString().toLowerCase()) {
                case "atualizardiaria"      -> handleAtualizarDiaria(jo, resp);
                case "marcarpresenca"       -> handleMarcarPresenca(jo, req, resp);
                case "removerpresenca"      -> handleRemoverPresenca(jo, resp);
                case "registrarvale"        -> handleRegistrarVale(jo, resp);
                case "excluirvale"          -> handleExcluirVale(jo, resp);
                case "marcarvaledescontado" -> handleMarcarValeDescontado(jo, resp);
                default -> writeJson(resp, 400, false, "Ação inválida.");
            }
        } catch (Exception e) {
            // P1-15/P2-20: pilha completa no log; ao usuário vai só o código.
            String cod = Util.LogUtil.erro(ControllerDiarista.class, "Falha tratada em ControllerDiarista.", e);
            writeJson(resp, 500, false, "Erro interno: " + cod);
        }
    }

    private void handleAtualizarDiaria(JsonObject jo, HttpServletResponse resp)
            throws IOException, SQLException {
        int id = getInt(jo, "idpessoa");
        BigDecimal valor = getBD(jo, "valorpordia");
        if (id == 0 || valor == null || valor.compareTo(BigDecimal.ZERO) < 0) {
            writeJson(resp, 400, false, "ID e valor da diária válidos são obrigatórios.");
            return;
        }
        diaristaDAO.updateValorPorDia(id, valor);
        writeJson(resp, 200, true, "Valor da diária atualizado!");
    }

    private void handleMarcarPresenca(JsonObject jo, HttpServletRequest req, HttpServletResponse resp)
            throws IOException, SQLException {
        int id = getInt(jo, "idpessoa");
        String data = getString(jo, "data");
        if (id == 0 || data == null) { writeJson(resp, 400, false, "ID e data são obrigatórios."); return; }
        Integer registradoPor = null;
        HttpSession s = req.getSession(false);
        if (s != null && s.getAttribute("usuarioLogado") instanceof Pessoa p) registradoPor = p.getIdPessoa();
        pontoDAO.marcarPresenca(id, LocalDate.parse(data), registradoPor);
        writeJson(resp, 200, true, "Dia registrado.");
    }

    private void handleRemoverPresenca(JsonObject jo, HttpServletResponse resp)
            throws IOException, SQLException {
        int idPonto = getInt(jo, "idponto");
        if (idPonto == 0) { writeJson(resp, 400, false, "ID do registro obrigatório."); return; }
        pontoDAO.excluir(idPonto);
        writeJson(resp, 200, true, "Dia removido.");
    }

    private void handleRegistrarVale(JsonObject jo, HttpServletResponse resp)
            throws IOException, SQLException {
        int id = getInt(jo, "idpessoa");
        BigDecimal valor = getBD(jo, "valor");
        String data = getString(jo, "datavale");
        if (id == 0 || valor == null || valor.compareTo(BigDecimal.ZERO) <= 0 || data == null) {
            writeJson(resp, 400, false, "Funcionário, valor e data são obrigatórios.");
            return;
        }
        ValeFuncionario v = new ValeFuncionario();
        v.setIdFuncionario(id);
        v.setValor(valor);
        v.setDataVale(LocalDate.parse(data));
        v.setDescricao(getString(jo, "descricao"));
        String tipo = getString(jo, "tipovale");
        if (tipo != null) {
            try { v.setTipoVale(ValeFuncionario.TipoVale.valueOf(tipo.toUpperCase())); }
            catch (IllegalArgumentException ignored) { }
        }
        valeDAO.registrar(v);
        writeJson(resp, 200, true, "Vale registrado.");
    }

    private void handleExcluirVale(JsonObject jo, HttpServletResponse resp)
            throws IOException, SQLException {
        int idVale = getInt(jo, "idvale");
        if (idVale == 0) { writeJson(resp, 400, false, "ID do vale obrigatório."); return; }
        valeDAO.excluir(idVale);
        writeJson(resp, 200, true, "Vale excluído.");
    }

    private void handleMarcarValeDescontado(JsonObject jo, HttpServletResponse resp)
            throws IOException, SQLException {
        int idVale = getInt(jo, "idvale");
        if (idVale == 0) { writeJson(resp, 400, false, "ID do vale obrigatório."); return; }
        valeDAO.marcarDescontado(idVale);
        writeJson(resp, 200, true, "Vale marcado como descontado.");
    }

    /* ── helpers ─────────────────────────────────────────────────── */
    private void writeJson(HttpServletResponse resp, int status, boolean ok, String msg) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setStatus(status);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ok", ok); m.put("msg", msg);
        try (PrintWriter out = resp.getWriter()) { out.print(gson.toJson(m)); }
    }
    private int getInt(JsonObject jo, String k) {
        try { return jo.has(k) && !jo.get(k).isJsonNull() ? jo.get(k).getAsInt() : 0; }
        catch (Exception e) { return 0; }
    }
    private BigDecimal getBD(JsonObject jo, String k) {
        try { return jo.has(k) && !jo.get(k).isJsonNull() ? jo.get(k).getAsBigDecimal() : null; }
        catch (Exception e) { return null; }
    }
    private String getString(JsonObject jo, String k) {
        return jo.has(k) && !jo.get(k).isJsonNull() ? jo.get(k).getAsString() : null;
    }
}
