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
import java.time.YearMonth;
import java.util.*;

/**
 * Servlet do módulo EMPREITA.
 * URL: /ControllerEmpreita
 *
 * O empreiteiro recebe por serviço/dia (cada lançamento = dia + valor).
 * O perfil soma os lançamentos do período, gerencia vales e mostra o líquido.
 *
 * GET  ?acao=perfil&id={idpessoa}&periodo={YYYY-MM}
 * POST acao=addlancamento | editlancamento | removelancamento |
 *           registrarvale | excluirvale | marcarvaledescontado
 */
public class ControllerEmpreita extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private FuncionarioDAO          funcDAO;
    private LancamentoEmpreitaDAO   empreitaDAO;
    private ValeFuncionarioDAO      valeDAO;
    private VinculoDAO              vinculoDAO;

    private final Gson gson = new GsonBuilder()
        .registerTypeAdapter(LocalDate.class,
            (JsonSerializer<LocalDate>) (s, t, c) -> new JsonPrimitive(s.toString()))
        .registerTypeAdapter(java.time.LocalTime.class,
            (JsonSerializer<java.time.LocalTime>) (s, t, c) -> new JsonPrimitive(s.toString()))
        .create();

    @Override
    public void init() {
        funcDAO     = new FuncionarioDAO();
        empreitaDAO = new LancamentoEmpreitaDAO();
        valeDAO     = new ValeFuncionarioDAO();
        vinculoDAO  = new VinculoDAO();
    }

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
            String cod = Util.LogUtil.erro(ControllerEmpreita.class, "Falha tratada em ControllerEmpreita.", e);
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

        Funcionario f = funcDAO.getFuncionarioById(id);
        if (f == null) { writeJson(resp, 404, false, "Funcionário não encontrado."); return; }

        BigDecimal valorAcordado = funcDAO.getValorEspecifico(id, TipoFuncionario.EMPREITA);
        Vinculo vinc = vinculoDAO.buscarPorData(id, YearMonth.parse(periodo).atDay(1));
        if (vinc == null) vinc = vinculoDAO.buscarAtivo(id);

        List<LancamentoEmpreita> lancamentos = empreitaDAO.listarPorMes(id, periodo);
        BigDecimal totalEmpreitas = empreitaDAO.somarPorMes(id, periodo);
        List<ValeFuncionario> vales = valeDAO.listarPorMes(id, periodo);
        BigDecimal totalVales = valeDAO.somarPorMes(id, periodo);
        if (totalVales == null) totalVales = BigDecimal.ZERO;

        BigDecimal bruto = totalEmpreitas.setScale(2, RoundingMode.HALF_UP);
        BigDecimal liquido = bruto.subtract(totalVales).setScale(2, RoundingMode.HALF_UP);
        if (liquido.compareTo(BigDecimal.ZERO) < 0) liquido = BigDecimal.ZERO;

        Map<String, Object> perfil = new LinkedHashMap<>();
        perfil.put("funcionario", f);
        perfil.put("valorAcordado", valorAcordado);
        perfil.put("vinculo", vinc);
        perfil.put("vinculos", vinculoDAO.listarPorPessoa(id));
        perfil.put("periodo", periodo);
        perfil.put("lancamentos", lancamentos);
        perfil.put("qtdLancamentos", lancamentos.size());
        perfil.put("vales", vales);
        perfil.put("totalVales", totalVales);
        perfil.put("valorBruto", bruto);
        perfil.put("valorLiquido", liquido);
        out.print(gson.toJson(perfil));
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding(StandardCharsets.UTF_8.name());
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = req.getReader()) { String l; while ((l = r.readLine()) != null) sb.append(l); }
        JsonObject jo = gson.fromJson(sb.toString(), JsonObject.class);
        if (jo == null || !jo.has("acao")) { writeJson(resp, 400, false, "Ação não informada."); return; }
        try {
            switch (jo.get("acao").getAsString().toLowerCase()) {
                case "addlancamento"        -> handleAddLancamento(jo, resp);
                case "editlancamento"       -> handleEditLancamento(jo, resp);
                case "removelancamento"     -> handleRemoveLancamento(jo, resp);
                case "registrarvale"        -> handleRegistrarVale(jo, resp);
                case "excluirvale"          -> handleExcluirVale(jo, resp);
                case "marcarvaledescontado" -> handleMarcarValeDescontado(jo, resp);
                default -> writeJson(resp, 400, false, "Ação inválida.");
            }
        } catch (Exception e) {
            // P1-15/P2-20: pilha completa no log; ao usuário vai só o código.
            String cod = Util.LogUtil.erro(ControllerEmpreita.class, "Falha tratada em ControllerEmpreita.", e);
            writeJson(resp, 500, false, "Erro interno: " + cod);
        }
    }

    private void handleAddLancamento(JsonObject jo, HttpServletResponse resp)
            throws IOException, SQLException {
        int id = getInt(jo, "idpessoa");
        String data = getString(jo, "data");
        BigDecimal valor = getBD(jo, "valor");
        if (id == 0 || data == null || valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            writeJson(resp, 400, false, "Data e valor (maior que zero) são obrigatórios."); return;
        }
        LancamentoEmpreita l = new LancamentoEmpreita();
        l.setIdPessoa(id);
        l.setDataEmpreita(LocalDate.parse(data));
        l.setDescricao(getString(jo, "descricao"));
        l.setValor(valor);
        empreitaDAO.inserir(l);
        writeJson(resp, 200, true, "Empreita registrada.");
    }

    private void handleEditLancamento(JsonObject jo, HttpServletResponse resp)
            throws IOException, SQLException {
        int idLanc = getInt(jo, "idlancamento");
        int id = getInt(jo, "idpessoa");
        String data = getString(jo, "data");
        BigDecimal valor = getBD(jo, "valor");
        if (idLanc == 0 || id == 0 || data == null || valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            writeJson(resp, 400, false, "Dados obrigatórios não informados."); return;
        }
        LancamentoEmpreita l = new LancamentoEmpreita();
        l.setIdLancamento(idLanc);
        l.setIdPessoa(id);
        l.setDataEmpreita(LocalDate.parse(data));
        l.setDescricao(getString(jo, "descricao"));
        l.setValor(valor);
        empreitaDAO.atualizar(l);
        writeJson(resp, 200, true, "Empreita atualizada.");
    }

    private void handleRemoveLancamento(JsonObject jo, HttpServletResponse resp)
            throws IOException, SQLException {
        int idLanc = getInt(jo, "idlancamento");
        if (idLanc == 0) { writeJson(resp, 400, false, "ID do lançamento obrigatório."); return; }
        empreitaDAO.excluir(idLanc);
        writeJson(resp, 200, true, "Empreita removida.");
    }

    private void handleRegistrarVale(JsonObject jo, HttpServletResponse resp)
            throws IOException, SQLException {
        int id = getInt(jo, "idpessoa");
        BigDecimal valor = getBD(jo, "valor");
        String data = getString(jo, "datavale");
        if (id == 0 || valor == null || valor.compareTo(BigDecimal.ZERO) <= 0 || data == null) {
            writeJson(resp, 400, false, "Funcionário, valor e data são obrigatórios."); return;
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

    /* helpers */
    private void writeJson(HttpServletResponse resp, int status, boolean ok, String msg) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setStatus(status);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ok", ok); m.put("msg", msg);
        try (PrintWriter out = resp.getWriter()) { out.print(gson.toJson(m)); }
    }
    private int getInt(JsonObject jo, String k) {
        try { return jo.has(k) && !jo.get(k).isJsonNull() ? jo.get(k).getAsInt() : 0; } catch (Exception e) { return 0; }
    }
    private BigDecimal getBD(JsonObject jo, String k) {
        try { return jo.has(k) && !jo.get(k).isJsonNull() ? jo.get(k).getAsBigDecimal() : null; } catch (Exception e) { return null; }
    }
    private String getString(JsonObject jo, String k) {
        return jo.has(k) && !jo.get(k).isJsonNull() ? jo.get(k).getAsString() : null;
    }
}
