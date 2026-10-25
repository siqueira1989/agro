package Controller;

import Model.Dao.ColheitaDAO;
import com.google.gson.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Servlet do Módulo de Colheita e Estoque. URL: /ControllerColheita
 *
 * GET  (sem acao)          → lista de lotes (filtro opcional: status)
 * GET  ?indicadores=1      → indicadores agregados (quantidade total/saldo por status)
 * POST acao=processar      → {idlote, quantidade} — baixa parcial ou total do saldo em estoque
 * POST acao=finalizar      → {idlote} — encerra o lote (terminal)
 * GET  ?processamentos=ID  → processamentos do lote (V30) com o nº do diário de Processamento
 * POST acao=validarprocessamento → {idlote, data, quantidade, embalagens[]} — só confere (não grava)
 * POST acao=processarcomdiario   → {idlote, iddiario, data, quantidade, embalagens[{idPessoa, tipoCaixa,
 *                                   descricao?, caixas, preco}]} — baixa + preços EMBALAGEM + vínculo, numa transação
 */
public class ControllerColheita extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private ColheitaDAO dao;

    private final Gson gson = new GsonBuilder()
        .registerTypeAdapter(LocalDate.class, (JsonSerializer<LocalDate>) (s, t, c) -> new JsonPrimitive(s.toString()))
        .create();

    @Override
    public void init() { dao = new ColheitaDAO(); }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        try (PrintWriter out = resp.getWriter()) {
            if (req.getParameter("indicadores") != null) { out.print(gson.toJson(dao.indicadores())); return; }
            if (req.getParameter("processamentos") != null) {
                out.print(gson.toJson(dao.listarProcessamentos(Integer.parseInt(req.getParameter("processamentos").trim())))); return;
            }
            out.print(gson.toJson(dao.listar(req.getParameter("status"))));
        } catch (Exception e) {
            String cod = Util.LogUtil.erro(ControllerColheita.class, "Falha tratada em ControllerColheita.", e);
            writeJson(resp, 500, false, "Erro ao carregar colheita: " + cod);
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
                case "processar": handleProcessar(jo, resp); break;
                case "validarprocessamento": handleValidarProcessamento(jo, resp); break;
                case "processarcomdiario": handleProcessarComDiario(jo, resp); break;
                case "finalizar": handleFinalizar(jo, resp); break;
                default: writeJson(resp, 400, false, "Ação inválida.");
            }
        } catch (IllegalArgumentException e) {
            writeJson(resp, 400, false, e.getMessage());
        } catch (Exception e) {
            String cod = Util.LogUtil.erro(ControllerColheita.class, "Falha tratada em ControllerColheita.", e);
            writeJson(resp, 500, false, "Erro interno: " + cod);
        }
    }

    private void handleProcessar(JsonObject jo, HttpServletResponse resp) throws Exception {
        int idLote = jo.has("idlote") && !jo.get("idlote").isJsonNull() ? jo.get("idlote").getAsInt() : 0;
        BigDecimal quantidade = jo.has("quantidade") && !jo.get("quantidade").isJsonNull()
                ? jo.get("quantidade").getAsBigDecimal() : null;
        if (idLote == 0) { writeJson(resp, 400, false, "ID do lote é obrigatório."); return; }
        dao.registrarProcessamento(idLote, quantidade);
        writeJson(resp, 200, true, "Processamento registrado.");
    }

    /** Modal Processar, passo 1: confere lote/saldo/safra/embalagens sem gravar (antes de criar o diário). */
    private void handleValidarProcessamento(JsonObject jo, HttpServletResponse resp) throws Exception {
        int idLote = getInt(jo, "idlote");
        if (idLote == 0) { writeJson(resp, 400, false, "ID do lote é obrigatório."); return; }
        dao.validarProcessamento(idLote, getData(jo), getDecimal(jo, "quantidade"), lerEmbalagens(jo));
        writeJson(resp, 200, true, "Processamento válido.");
    }

    /**
     * Modal Processar, passo 3 (o passo 2 é o diário de Processamento criado no Diário de Campo):
     * baixa do lote + preços das embalagens + vínculo lote ↔ diário, numa transação.
     */
    private void handleProcessarComDiario(JsonObject jo, HttpServletResponse resp) throws Exception {
        int idLote = getInt(jo, "idlote");
        int idDiario = getInt(jo, "iddiario");
        if (idLote == 0 || idDiario == 0) { writeJson(resp, 400, false, "Lote e diário de Processamento são obrigatórios."); return; }
        int idProc = dao.registrarProcessamentoComDiario(idLote, idDiario, getData(jo), getDecimal(jo, "quantidade"), lerEmbalagens(jo));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ok", true); m.put("msg", "Processamento registrado."); m.put("idprocessamento", idProc);
        resp.setContentType("application/json"); resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        try (PrintWriter out = resp.getWriter()) { out.print(gson.toJson(m)); }
    }

    private List<ColheitaDAO.LinhaEmbalagem> lerEmbalagens(JsonObject jo) {
        List<ColheitaDAO.LinhaEmbalagem> out = new ArrayList<>();
        if (!jo.has("embalagens") || !jo.get("embalagens").isJsonArray()) return out;
        for (JsonElement el : jo.getAsJsonArray("embalagens")) {
            JsonObject o = el.getAsJsonObject();
            ColheitaDAO.LinhaEmbalagem e = new ColheitaDAO.LinhaEmbalagem();
            e.idPessoa = getInt(o, "idPessoa");
            e.tipoCaixa = getStr(o, "tipoCaixa");
            e.descricao = getStr(o, "descricao");
            e.caixas = getDecimal(o, "caixas");
            e.preco = getDecimal(o, "preco");
            out.add(e);
        }
        return out;
    }

    private LocalDate getData(JsonObject jo) {
        String s = getStr(jo, "data");
        if (s == null || s.isBlank()) return null;
        try { return LocalDate.parse(s.trim()); } catch (Exception e) { throw new IllegalArgumentException("Data do processamento inválida."); }
    }
    private int getInt(JsonObject jo, String k) { try { return jo.has(k) && !jo.get(k).isJsonNull() ? jo.get(k).getAsInt() : 0; } catch (Exception e) { return 0; } }
    private String getStr(JsonObject jo, String k) { return jo.has(k) && !jo.get(k).isJsonNull() ? jo.get(k).getAsString() : null; }
    private BigDecimal getDecimal(JsonObject jo, String k) {
        try { return jo.has(k) && !jo.get(k).isJsonNull() ? jo.get(k).getAsBigDecimal() : null; }
        catch (Exception e) { throw new IllegalArgumentException("Valor numérico inválido em " + k + "."); }
    }

    private void handleFinalizar(JsonObject jo, HttpServletResponse resp) throws Exception {
        int idLote = jo.has("idlote") && !jo.get("idlote").isJsonNull() ? jo.get("idlote").getAsInt() : 0;
        if (idLote == 0) { writeJson(resp, 400, false, "ID do lote é obrigatório."); return; }
        dao.finalizarLote(idLote);
        writeJson(resp, 200, true, "Lote finalizado.");
    }

    private void writeJson(HttpServletResponse resp, int status, boolean ok, String msg) throws IOException {
        Map<String, Object> m = new LinkedHashMap<>(); m.put("ok", ok); m.put("msg", msg);
        resp.setContentType("application/json"); resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setStatus(status);
        try (PrintWriter out = resp.getWriter()) { out.print(gson.toJson(m)); }
    }
}
