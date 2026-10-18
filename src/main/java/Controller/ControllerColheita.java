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
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Servlet do Módulo de Colheita e Estoque. URL: /ControllerColheita
 *
 * GET  (sem acao)          → lista de lotes (filtro opcional: status)
 * GET  ?indicadores=1      → indicadores agregados (quantidade total/saldo por status)
 * POST acao=processar      → {idlote, quantidade} — baixa parcial ou total do saldo em estoque
 * POST acao=finalizar      → {idlote} — encerra o lote (terminal)
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
