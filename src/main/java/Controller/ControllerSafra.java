package Controller;

import Model.Dao.SafraDAO;
import Model.Model.Safra;
import Model.Model.SafraTalhao;
import Util.PostgresConnection;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Safra. URL: /ControllerSafra
 * GET  (sem acao) lista | ?id= uma safra | ?resumo=ID painel de custos |
 *      ?resolver=IDQUADRA&data=YYYY-MM-DD | ?talhoes=1 (quadras) | ?culturas=1
 * POST acao=create | update | status
 */
public class ControllerSafra extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private SafraDAO dao;
    private final Gson gson = new GsonBuilder()
        .registerTypeAdapter(LocalDate.class, (JsonSerializer<LocalDate>) (s, t, c) -> new JsonPrimitive(s.toString()))
        .registerTypeAdapter(LocalDate.class, (JsonDeserializer<LocalDate>) (j, t, c) ->
                j == null || j.isJsonNull() || j.getAsString().isBlank() ? null : LocalDate.parse(j.getAsString()))
        .create();

    @Override public void init() { dao = new SafraDAO(); }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json"); resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        try (PrintWriter out = resp.getWriter()) {
            if (req.getParameter("culturas") != null) { out.print(gson.toJson(listarCulturas())); return; }
            if (req.getParameter("talhoes") != null)  { out.print(gson.toJson(listarQuadras())); return; }
            if (req.getParameter("resumo") != null)   { out.print(gson.toJson(dao.resumoFinanceiro(Integer.parseInt(req.getParameter("resumo"))))); return; }
            if (req.getParameter("resolver") != null) {
                String data = req.getParameter("data");
                LocalDate d = (data == null || data.isBlank()) ? LocalDate.now() : LocalDate.parse(data);
                String[] sf = dao.resolverSafra(Integer.parseInt(req.getParameter("resolver")), d);
                Map<String, Object> m = new LinkedHashMap<>();
                if (sf != null) { m.put("idSafra", Integer.parseInt(sf[0])); m.put("nome", sf[1]); m.put("status", sf[2]); }
                out.print(gson.toJson(m));
                return;
            }
            String id = req.getParameter("id");
            if (id != null && !id.isBlank()) {
                Safra s = dao.buscarPorId(Integer.parseInt(id.trim()));
                if (s != null) out.print(gson.toJson(s)); else writeJson(resp, 404, false, "Safra não encontrada.");
                return;
            }
            out.print(gson.toJson(dao.listar()));
        } catch (Exception e) {
            e.printStackTrace(); writeJson(resp, 500, false, "Erro ao carregar safra: " + e.getMessage());
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
                case "create": salvar(jo, resp, false); break;
                case "update": salvar(jo, resp, true); break;
                case "status": {
                    int id = jo.get("idSafra").getAsInt(); String st = jo.get("status").getAsString();
                    if (!st.matches("PLANEJADA|EM_ANDAMENTO|FINALIZADA")) { writeJson(resp, 400, false, "Status inválido."); return; }
                    dao.definirStatus(id, st); writeJson(resp, 200, true, "Status da safra atualizado.");
                    break;
                }
                default: writeJson(resp, 400, false, "Ação inválida.");
            }
        } catch (SafraDAO.AreaInvalidaException e) {
            writeJson(resp, 400, false, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace(); writeJson(resp, 500, false, "Erro interno: " + e.getMessage());
        }
    }

    private void salvar(JsonObject jo, HttpServletResponse resp, boolean update) throws Exception {
        Safra s = gson.fromJson(jo, Safra.class);
        if (s.getNome() == null || s.getNome().isBlank()) { writeJson(resp, 400, false, "Nome da safra é obrigatório."); return; }
        if (s.getDataInicial() == null || s.getDataFinal() == null) { writeJson(resp, 400, false, "Datas inicial e final são obrigatórias."); return; }
        if (s.getDataFinal().isBefore(s.getDataInicial())) { writeJson(resp, 400, false, "Data final não pode ser anterior à inicial."); return; }
        if (s.getStatus() == null || !s.getStatus().matches("PLANEJADA|EM_ANDAMENTO|FINALIZADA")) s.setStatus("PLANEJADA");
        if (update) {
            if (s.getIdSafra() == 0) { writeJson(resp, 400, false, "ID da safra obrigatório."); return; }
            dao.atualizarCompleto(s); writeJson(resp, 200, true, "Safra atualizada com sucesso!");
        } else {
            int id = dao.inserirCompleto(s);
            Map<String, Object> m = new LinkedHashMap<>(); m.put("ok", true); m.put("msg", "Safra cadastrada com sucesso!"); m.put("idSafra", id);
            writeRaw(resp, 200, m);
        }
    }

    /* selects */
    private List<Map<String, Object>> listarCulturas() throws Exception {
        List<Map<String, Object>> l = new ArrayList<>();
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement st = c.prepareStatement("SELECT idproduto, nomeproduto FROM alimento ORDER BY nomeproduto");
             ResultSet rs = st.executeQuery()) {
            while (rs.next()) { Map<String, Object> m = new LinkedHashMap<>(); m.put("id", rs.getInt(1)); m.put("nome", rs.getString(2)); l.add(m); }
        }
        return l;
    }
    private List<Map<String, Object>> listarQuadras() throws Exception {
        List<Map<String, Object>> l = new ArrayList<>();
        String sql = "SELECT q.idquadra, q.nome_quadra, q.numero_plantas, q.area_ha, a.propriedadeareaproducao "
                + "FROM quadra q JOIN areaproducao a ON a.idareaproducao=q.idareaproducao WHERE q.ativa=true ORDER BY a.propriedadeareaproducao, q.nome_quadra";
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement st = c.prepareStatement(sql); ResultSet rs = st.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("idQuadra", rs.getInt("idquadra")); m.put("nome", rs.getString("nome_quadra"));
                m.put("numeroPlantas", rs.getInt("numero_plantas")); m.put("areaHa", rs.getBigDecimal("area_ha"));
                m.put("area", rs.getString("propriedadeareaproducao"));
                l.add(m);
            }
        }
        return l;
    }

    private void writeJson(HttpServletResponse resp, int status, boolean ok, String msg) throws IOException {
        Map<String, Object> m = new LinkedHashMap<>(); m.put("ok", ok); m.put("msg", msg); writeRaw(resp, status, m);
    }
    private void writeRaw(HttpServletResponse resp, int status, Map<String, Object> payload) throws IOException {
        resp.setContentType("application/json"); resp.setCharacterEncoding(StandardCharsets.UTF_8.name()); resp.setStatus(status);
        try (PrintWriter out = resp.getWriter()) { out.print(gson.toJson(payload)); }
    }
}
