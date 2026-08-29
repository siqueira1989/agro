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
 * Servlet do módulo PRODUÇÃO.
 * URL: /ControllerProducao
 *
 * O funcionário de produção ganha por caixa (quantidade × preço). O preço vem
 * do histórico (por ano, podendo variar por funcionário). Cada lançamento
 * grava o preço aplicado (snapshot).
 *
 * GET  ?acao=perfil&id={idpessoa}&periodo={YYYY-MM}
 * POST acao=addcaixa | editcaixa | removecaixa | setpreco |
 *           registrarvale | excluirvale | marcarvaledescontado
 */
public class ControllerProducao extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private FuncionarioDAO     funcDAO;
    private ProducaoCaixaDAO   producaoDAO;
    private PrecoCaixaDAO      precoDAO;
    private ValeFuncionarioDAO valeDAO;
    private VinculoDAO         vinculoDAO;

    private final Gson gson = new GsonBuilder()
        .registerTypeAdapter(LocalDate.class,
            (JsonSerializer<LocalDate>) (s, t, c) -> new JsonPrimitive(s.toString()))
        .registerTypeAdapter(java.time.LocalTime.class,
            (JsonSerializer<java.time.LocalTime>) (s, t, c) -> new JsonPrimitive(s.toString()))
        .create();

    @Override
    public void init() {
        funcDAO     = new FuncionarioDAO();
        producaoDAO = new ProducaoCaixaDAO();
        precoDAO    = new PrecoCaixaDAO();
        valeDAO     = new ValeFuncionarioDAO();
        vinculoDAO  = new VinculoDAO();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        try (PrintWriter out = resp.getWriter()) {
            if (!"perfil".equals(req.getParameter("acao"))) { writeJson(resp, 400, false, "Ação inválida."); return; }
            handlePerfil(req, resp, out);
        } catch (Exception e) {
            // P1-15/P2-20: pilha completa no log; ao usuário vai só o código.
            String cod = Util.LogUtil.erro(ControllerProducao.class, "Falha tratada em ControllerProducao.", e);
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
        int ano = Integer.parseInt(periodo.substring(0, 4));

        Funcionario f = funcDAO.getFuncionarioById(id);
        if (f == null) { writeJson(resp, 404, false, "Funcionário não encontrado."); return; }

        String cargo = f.getCargo() != null ? f.getCargo() : "";
        Vinculo vinc = vinculoDAO.buscarPorData(id, YearMonth.parse(periodo).atDay(1));
        if (vinc == null) vinc = vinculoDAO.buscarAtivo(id);

        // Tipos de caixa permitidos ao cargo + preço vigente (para o ano) de cada
        List<Map<String, Object>> tiposCaixa = new ArrayList<>();
        for (TipoCaixa t : TipoCaixa.values()) {
            if (!t.getCargo().equalsIgnoreCase(cargo)) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("nome", t.name());
            m.put("rotulo", t.getRotulo());
            m.put("preco", precoDAO.resolverPreco(t.name(), ano, id));
            tiposCaixa.add(m);
        }

        List<Map<String, Object>> lancamentos = producaoDAO.listarPorMes(id, periodo);
        int totalCaixas = producaoDAO.totalCaixas(id, periodo)[0];
        BigDecimal valorProducao = producaoDAO.somarValor(id, periodo).setScale(2, RoundingMode.HALF_UP);

        List<ValeFuncionario> vales = valeDAO.listarPorMes(id, periodo);
        BigDecimal totalVales = valeDAO.somarPorMes(id, periodo);
        if (totalVales == null) totalVales = BigDecimal.ZERO;

        BigDecimal liquido = valorProducao.subtract(totalVales).setScale(2, RoundingMode.HALF_UP);
        if (liquido.compareTo(BigDecimal.ZERO) < 0) liquido = BigDecimal.ZERO;

        Map<String, Object> perfil = new LinkedHashMap<>();
        perfil.put("funcionario", f);
        perfil.put("cargo", cargo);
        perfil.put("ano", ano);
        perfil.put("vinculo", vinc);
        perfil.put("vinculos", vinculoDAO.listarPorPessoa(id));
        perfil.put("periodo", periodo);
        perfil.put("tiposCaixa", tiposCaixa);
        perfil.put("lancamentos", lancamentos);
        perfil.put("totalCaixas", totalCaixas);
        perfil.put("valorProducao", valorProducao);
        perfil.put("vales", vales);
        perfil.put("totalVales", totalVales);
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
                case "addcaixa"             -> handleAddCaixa(jo, resp);
                case "editcaixa"            -> handleEditCaixa(jo, resp);
                case "removecaixa"          -> handleRemoveCaixa(jo, resp);
                case "setpreco"             -> handleSetPreco(jo, resp);
                case "registrarvale"        -> handleRegistrarVale(jo, resp);
                case "excluirvale"          -> handleExcluirVale(jo, resp);
                case "marcarvaledescontado" -> handleMarcarValeDescontado(jo, resp);
                default -> writeJson(resp, 400, false, "Ação inválida.");
            }
        } catch (Exception e) {
            // P1-15/P2-20: pilha completa no log; ao usuário vai só o código.
            String cod = Util.LogUtil.erro(ControllerProducao.class, "Falha tratada em ControllerProducao.", e);
            writeJson(resp, 500, false, "Erro interno: " + cod);
        }
    }

    private void handleAddCaixa(JsonObject jo, HttpServletResponse resp) throws IOException, SQLException {
        int id = getInt(jo, "idpessoa");
        String data = getString(jo, "data");
        String tipo = getString(jo, "tipocaixa");
        int qtd = getInt(jo, "quantidade");
        if (id == 0 || data == null || !TipoCaixa.isValido(tipo) || qtd <= 0) {
            writeJson(resp, 400, false, "Data, tipo de caixa e quantidade (>0) são obrigatórios."); return;
        }
        LocalDate d = LocalDate.parse(data);
        BigDecimal preco = precoDAO.resolverPreco(tipo, d.getYear(), id);
        producaoDAO.inserir(id, d, tipo, qtd, preco);
        writeJson(resp, 200, true, "Produção registrada.");
    }

    private void handleEditCaixa(JsonObject jo, HttpServletResponse resp) throws IOException, SQLException {
        int idProd = getInt(jo, "idproducao");
        int id = getInt(jo, "idpessoa");
        String data = getString(jo, "data");
        String tipo = getString(jo, "tipocaixa");
        int qtd = getInt(jo, "quantidade");
        if (idProd == 0 || id == 0 || data == null || !TipoCaixa.isValido(tipo) || qtd <= 0) {
            writeJson(resp, 400, false, "Dados obrigatórios não informados."); return;
        }
        LocalDate d = LocalDate.parse(data);
        BigDecimal preco = precoDAO.resolverPreco(tipo, d.getYear(), id);
        producaoDAO.atualizar(idProd, id, d, tipo, qtd, preco);
        writeJson(resp, 200, true, "Produção atualizada.");
    }

    private void handleRemoveCaixa(JsonObject jo, HttpServletResponse resp) throws IOException, SQLException {
        int idProd = getInt(jo, "idproducao");
        if (idProd == 0) { writeJson(resp, 400, false, "ID obrigatório."); return; }
        producaoDAO.excluir(idProd);
        writeJson(resp, 200, true, "Produção removida.");
    }

    /** Define o preço de um tipo de caixa (geral do ano, ou específico do funcionário). */
    private void handleSetPreco(JsonObject jo, HttpServletResponse resp) throws IOException, SQLException {
        String tipo = getString(jo, "tipocaixa");
        int ano = getInt(jo, "ano");
        BigDecimal preco = getBD(jo, "preco");
        Integer idpessoa = jo.has("idpessoa") && !jo.get("idpessoa").isJsonNull()
                && jo.get("idpessoa").getAsInt() != 0 ? jo.get("idpessoa").getAsInt() : null;
        if (!TipoCaixa.isValido(tipo) || ano == 0 || preco == null || preco.compareTo(BigDecimal.ZERO) < 0) {
            writeJson(resp, 400, false, "Tipo de caixa, ano e preço válidos são obrigatórios."); return;
        }
        precoDAO.upsert(tipo, ano, idpessoa, preco);
        writeJson(resp, 200, true, "Preço atualizado.");
    }

    private void handleRegistrarVale(JsonObject jo, HttpServletResponse resp) throws IOException, SQLException {
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

    private void handleExcluirVale(JsonObject jo, HttpServletResponse resp) throws IOException, SQLException {
        int idVale = getInt(jo, "idvale");
        if (idVale == 0) { writeJson(resp, 400, false, "ID do vale obrigatório."); return; }
        valeDAO.excluir(idVale);
        writeJson(resp, 200, true, "Vale excluído.");
    }

    private void handleMarcarValeDescontado(JsonObject jo, HttpServletResponse resp) throws IOException, SQLException {
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
