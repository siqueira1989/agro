package Controller;

import Model.Dao.MaquinaDAO;
import Model.Model.Maquina;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * CRUD de maquinário. URL: /ControllerMaquina
 * GET (sem acao) → lista (?ativas=true opcional); ?id= → um; POST create|update|delete.
 */
public class ControllerMaquina extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private final Gson gson = new Gson();
    private MaquinaDAO dao;

    @Override public void init() { dao = new MaquinaDAO(); }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json"); resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        try (PrintWriter out = resp.getWriter()) {
            String id = req.getParameter("id");
            if (id != null && !id.isBlank()) {
                Maquina m = dao.buscarPorId(Integer.parseInt(id.trim()));
                if (m != null) out.print(gson.toJson(m)); else writeJson(resp, 404, false, "Maquinário não encontrado.");
                return;
            }
            boolean somenteAtivas = "true".equalsIgnoreCase(req.getParameter("ativas"));
            out.print(gson.toJson(dao.listar(somenteAtivas)));
        } catch (Exception e) {
            e.printStackTrace(); writeJson(resp, 500, false, "Erro ao carregar maquinário.");
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
                case "delete": desativar(jo, resp); break;
                default: writeJson(resp, 400, false, "Ação inválida.");
            }
        } catch (Exception e) {
            e.printStackTrace(); writeJson(resp, 500, false, "Erro interno: " + e.getMessage());
        }
    }

    private void salvar(JsonObject jo, HttpServletResponse resp, boolean update) throws Exception {
        String nome = getStr(jo, "nome");
        String tipo = getStr(jo, "tipo");
        if (nome == null || nome.isBlank() || tipo == null || !tipo.matches("TRATOR|IMPLEMENTO|VEICULO")) {
            writeJson(resp, 400, false, "Nome e tipo (Trator/Implemento/Veículo) são obrigatórios."); return;
        }
        Maquina m = new Maquina();
        m.setNome(nome.trim()); m.setTipo(tipo);
        m.setMarca(getStr(jo, "marca")); m.setIdentificacao(getStr(jo, "identificacao"));
        m.setCustoHora(getBD(jo, "custoHora")); m.setCustoKm(getBD(jo, "custoKm"));
        m.setCombustivelHora(getBD(jo, "combustivelHora")); m.setManutencaoHora(getBD(jo, "manutencaoHora"));
        m.setDepreciacaoHora(getBD(jo, "depreciacaoHora"));
        m.setSituacao(!jo.has("situacao") || jo.get("situacao").getAsBoolean());
        // Veículo usa custo/km; trator/implemento usa custo/hora
        if ("VEICULO".equals(tipo) && m.getCustoKm().signum() <= 0) {
            writeJson(resp, 400, false, "Informe o custo por KM do veículo."); return;
        }
        if (update) {
            m.setIdMaquina(getInt(jo, "idMaquina"));
            if (m.getIdMaquina() == 0) { writeJson(resp, 400, false, "ID do maquinário obrigatório."); return; }
            dao.atualizar(m); writeJson(resp, 200, true, "Maquinário atualizado com sucesso!");
        } else {
            dao.inserir(m); writeJson(resp, 200, true, "Maquinário cadastrado com sucesso!");
        }
    }

    private void desativar(JsonObject jo, HttpServletResponse resp) throws Exception {
        int id = getInt(jo, "idMaquina");
        if (id == 0) { writeJson(resp, 400, false, "ID obrigatório."); return; }
        boolean nova = jo.has("situacao") && !jo.get("situacao").getAsBoolean();
        dao.definirSituacao(id, nova);
        writeJson(resp, 200, true, nova ? "Maquinário ativado." : "Maquinário desativado.");
    }

    private void writeJson(HttpServletResponse resp, int status, boolean ok, String msg) throws IOException {
        resp.setContentType("application/json"); resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setStatus(status);
        Map<String, Object> m = new LinkedHashMap<>(); m.put("ok", ok); m.put("msg", msg);
        try (PrintWriter out = resp.getWriter()) { out.print(gson.toJson(m)); }
    }
    private String getStr(JsonObject jo, String k) { return jo.has(k) && !jo.get(k).isJsonNull() ? jo.get(k).getAsString() : null; }
    private int getInt(JsonObject jo, String k) { try { return jo.has(k) && !jo.get(k).isJsonNull() ? jo.get(k).getAsInt() : 0; } catch (Exception e) { return 0; } }
    private BigDecimal getBD(JsonObject jo, String k) { try { return jo.has(k) && !jo.get(k).isJsonNull() ? jo.get(k).getAsBigDecimal() : BigDecimal.ZERO; } catch (Exception e) { return BigDecimal.ZERO; } }
}
