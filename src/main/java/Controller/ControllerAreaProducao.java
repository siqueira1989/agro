package Controller;

import Model.Dao.AreaProducaoDAO;
import Model.Dao.QuadraDAO;
import Model.Model.AreaProducao;
import Model.Model.Quadra;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ControllerAreaProducao extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private AreaProducaoDAO dao;
    private QuadraDAO quadraDAO;
    private final Gson gson = new Gson();

    @Override
    public void init() {
        dao = new AreaProducaoDAO();
        quadraDAO = new QuadraDAO();
    }

    private void writeJson(HttpServletResponse resp, int status, boolean ok, String msg, String target) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        Map<String, Object> payload = new HashMap<>();
        payload.put("ok", ok);
        payload.put("msg", msg);
        if (target != null) payload.put("target", target);
        resp.setStatus(status);
        try (PrintWriter out = resp.getWriter()) {
            out.print(gson.toJson(payload));
            out.flush();
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        try (PrintWriter out = resp.getWriter()) {
            String id = req.getParameter("id");
            String quadras = req.getParameter("quadras");
            String alimentos = req.getParameter("alimentos");

            if (alimentos != null) {                       // select de tipo de planta
                out.print(gson.toJson(dao.listarAlimentos()));
            } else if (quadras != null && !quadras.isBlank()) {  // quadras de uma área
                boolean somenteAtivas = !"false".equalsIgnoreCase(req.getParameter("ativas"));  // default: só ativas
                out.print(gson.toJson(quadraDAO.listarPorArea(Integer.parseInt(quadras), somenteAtivas)));
            } else if (id != null && !id.isBlank()) {      // uma área
                AreaProducao area = dao.getById(Integer.parseInt(id));
                out.print(gson.toJson(area));
            } else {                                        // lista completa
                List<AreaProducao> lista = dao.listAll();
                out.print(gson.toJson(lista));
            }
            out.flush();
        } catch (SQLException e) {
            writeJson(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Erro ao buscar áreas: " + e.getMessage(), "page");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding(StandardCharsets.UTF_8.name());
        StringBuilder json = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String linha;
            while ((linha = reader.readLine()) != null) json.append(linha);
        }
        JsonObject jo = gson.fromJson(json.toString(), JsonObject.class);
        if (jo == null || !jo.has("acao")) {
            writeJson(resp, HttpServletResponse.SC_BAD_REQUEST, false, "Ação não informada.", "page");
            return;
        }
        switch (jo.get("acao").getAsString().toLowerCase()) {
            case "create": handleCreate(jo, resp); break;
            case "update": handleUpdate(jo, resp); break;
            case "desativar": handleDesativar(jo, resp); break;
            case "delete": handleDesativar(jo, resp); break;   // compat.: exclusão agora é soft delete
            case "addquadra": handleAddQuadra(jo, resp); break;
            case "desativarquadra": handleAtivarDesativarQuadra(jo, resp, false); break;
            case "ativarquadra": handleAtivarDesativarQuadra(jo, resp, true); break;
            default: writeJson(resp, HttpServletResponse.SC_BAD_REQUEST, false, "Ação inválida.", "page");
        }
    }

    private void handleCreate(JsonObject jo, HttpServletResponse resp) throws IOException {
        String propriedade = getStr(jo, "propriedadeareaproducao");
        String proprietario = getStr(jo, "proprietarioareaproducao");
        String sigla = getStr(jo, "siglasareaproducao");
        String cep = getStr(jo, "cep");
        String complemento = getStr(jo, "complemento");
        Integer qtd = getInt(jo, "quantidadetotalplantasareaproducao");
        Integer numero = getInt(jo, "numero");

        if (isBlank(propriedade) || isBlank(proprietario) || isBlank(sigla)) {
            writeJson(resp, HttpServletResponse.SC_BAD_REQUEST, false,
                    "Propriedade, proprietário e sigla são obrigatórios.", "modal");
            return;
        }
        if (sigla.length() > 4) {
            writeJson(resp, HttpServletResponse.SC_BAD_REQUEST, false,
                    "Sigla deve ter no máximo 4 caracteres.", "modal");
            return;
        }
        try {
            if (dao.existeSigla(sigla)) {
                writeJson(resp, HttpServletResponse.SC_BAD_REQUEST, false,
                        "Já existe uma área com essa sigla.", "page");
                return;
            }
            List<Quadra> quadras = parseQuadras(jo);
            if (quadras.isEmpty()) {
                writeJson(resp, HttpServletResponse.SC_BAD_REQUEST, false,
                        "Cadastre ao menos uma quadra de produção.", "page");
                return;
            }
            AreaProducao area = new AreaProducao();
            area.setPropriedadeAreaProducao(propriedade);
            area.setProprietarioAreaProducao(proprietario);
            area.setSiglasAreaProducao(sigla.toUpperCase());
            area.setCep(cep != null ? cep : "");
            area.setComplemento(complemento != null ? complemento : "");
            area.setNumero(numero != null ? numero : 0);
            dao.inserirComQuadras(area, quadras);   // qtd = soma das quadras
            writeJson(resp, HttpServletResponse.SC_OK, true, "Área cadastrada com sucesso!", "page");
        } catch (SQLException e) {
            writeJson(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Erro ao cadastrar: " + e.getMessage(), "page");
        }
    }

    /** Lê o array 'quadras' do JSON (nome, numeroPlantas, idAlimento). */
    private List<Quadra> parseQuadras(JsonObject jo) {
        List<Quadra> lista = new ArrayList<>();
        if (!jo.has("quadras") || !jo.get("quadras").isJsonArray()) return lista;
        JsonArray arr = jo.getAsJsonArray("quadras");
        for (JsonElement el : arr) {
            if (!el.isJsonObject()) continue;
            JsonObject q = el.getAsJsonObject();
            String nome = getStr(q, "nomeQuadra");
            if (isBlank(nome)) continue;
            Quadra quadra = new Quadra();
            quadra.setNomeQuadra(nome.trim());
            Integer plantas = getInt(q, "numeroPlantas");
            quadra.setNumeroPlantas(plantas != null ? plantas : 0);
            quadra.setAreaHa(getBD(q, "areaHa"));
            quadra.setIdAlimento(getInt(q, "idAlimento"));
            lista.add(quadra);
        }
        return lista;
    }

    private void handleUpdate(JsonObject jo, HttpServletResponse resp) throws IOException {
        Integer id = getInt(jo, "idareaproducao");
        String propriedade = getStr(jo, "propriedadeareaproducao");
        String proprietario = getStr(jo, "proprietarioareaproducao");
        String sigla = getStr(jo, "siglasareaproducao");
        String cep = getStr(jo, "cep");
        String complemento = getStr(jo, "complemento");
        Integer qtd = getInt(jo, "quantidadetotalplantasareaproducao");
        Integer numero = getInt(jo, "numero");

        if (id == null || isBlank(propriedade) || isBlank(proprietario) || isBlank(sigla)) {
            writeJson(resp, HttpServletResponse.SC_BAD_REQUEST, false,
                    "Dados obrigatórios não informados.", "page");
            return;
        }
        try {
            if (dao.existeSiglaOutro(sigla, id)) {
                writeJson(resp, HttpServletResponse.SC_BAD_REQUEST, false,
                        "Já existe outra área com essa sigla.", "page");
                return;
            }
            AreaProducao area = new AreaProducao();
            area.setIdAreaProducao(id);
            area.setPropriedadeAreaProducao(propriedade);
            area.setProprietarioAreaProducao(proprietario);
            area.setSiglasAreaProducao(sigla.toUpperCase());
            area.setCep(cep != null ? cep : "");
            area.setComplemento(complemento != null ? complemento : "");
            area.setNumero(numero != null ? numero : 0);
            area.setQuantidadeTotalPlantasAreaProducao(qtd != null ? qtd : 0);
            dao.update(area);
            dao.recomputarQtdPlantas(id);   // qtd sempre = soma das quadras ativas
            writeJson(resp, HttpServletResponse.SC_OK, true, "Área atualizada com sucesso!", "page");
        } catch (SQLException e) {
            writeJson(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Erro ao atualizar: " + e.getMessage(), "page");
        }
    }

    /** Ativa/desativa a área (soft delete). Recebe 'situacao' atual e inverte. */
    private void handleDesativar(JsonObject jo, HttpServletResponse resp) throws IOException {
        Integer id = getInt(jo, "idareaproducao");
        if (id == null) {
            writeJson(resp, HttpServletResponse.SC_BAD_REQUEST, false, "ID não informado.", "page");
            return;
        }
        try {
            boolean novaSituacao = jo.has("situacao") && !jo.get("situacao").getAsBoolean();
            dao.definirSituacao(id, novaSituacao);
            writeJson(resp, HttpServletResponse.SC_OK, true,
                    novaSituacao ? "Área ativada com sucesso!" : "Área desativada com sucesso!", "page");
        } catch (SQLException e) {
            writeJson(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Erro ao alterar situação: " + e.getMessage(), "page");
        }
    }

    /** Adiciona uma quadra a uma área existente e recomputa a soma de plantas. */
    private void handleAddQuadra(JsonObject jo, HttpServletResponse resp) throws IOException {
        Integer idArea = getInt(jo, "idareaproducao");
        String nome = getStr(jo, "nomeQuadra");
        Integer plantas = getInt(jo, "numeroPlantas");
        Integer idAlimento = getInt(jo, "idAlimento");
        if (idArea == null || isBlank(nome)) {
            writeJson(resp, HttpServletResponse.SC_BAD_REQUEST, false,
                    "Nome da quadra é obrigatório.", "page");
            return;
        }
        try {
            Quadra q = new Quadra();
            q.setIdAreaProducao(idArea);
            q.setNomeQuadra(nome.trim());
            q.setNumeroPlantas(plantas != null ? plantas : 0);
            q.setAreaHa(getBD(jo, "areaHa"));
            q.setIdAlimento(idAlimento);
            quadraDAO.inserir(q);
            dao.recomputarQtdPlantas(idArea);
            writeJson(resp, HttpServletResponse.SC_OK, true, "Quadra adicionada!", "page");
        } catch (SQLException e) {
            writeJson(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Erro ao adicionar quadra: " + e.getMessage(), "page");
        }
    }

    /**
     * Ativa/desativa uma quadra (soft delete: a quadra e seus dados são
     * preservados no banco) e recomputa a soma de plantas da área.
     */
    private void handleAtivarDesativarQuadra(JsonObject jo, HttpServletResponse resp, boolean ativar) throws IOException {
        Integer idQuadra = getInt(jo, "idquadra");
        if (idQuadra == null) {
            writeJson(resp, HttpServletResponse.SC_BAD_REQUEST, false, "ID da quadra não informado.", "page");
            return;
        }
        try {
            Integer idArea = quadraDAO.areaDaQuadra(idQuadra);
            quadraDAO.definirAtiva(idQuadra, ativar);
            if (idArea != null) dao.recomputarQtdPlantas(idArea);
            writeJson(resp, HttpServletResponse.SC_OK, true,
                    ativar ? "Quadra reativada!" : "Quadra desativada!", "page");
        } catch (SQLException e) {
            writeJson(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Erro ao alterar a quadra: " + e.getMessage(), "page");
        }
    }

    private boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }

    private String getStr(JsonObject jo, String campo) {
        return jo.has(campo) && !jo.get(campo).isJsonNull() ? jo.get(campo).getAsString() : null;
    }

    private java.math.BigDecimal getBD(JsonObject jo, String campo) {
        try {
            return jo.has(campo) && !jo.get(campo).isJsonNull() && !jo.get(campo).getAsString().trim().isEmpty()
                    ? jo.get(campo).getAsBigDecimal() : java.math.BigDecimal.ZERO;
        } catch (Exception e) { return java.math.BigDecimal.ZERO; }
    }

    private Integer getInt(JsonObject jo, String campo) {
        try {
            return jo.has(campo) && !jo.get(campo).isJsonNull()
                    && !jo.get(campo).getAsString().trim().isEmpty()
                    ? jo.get(campo).getAsInt() : null;
        } catch (Exception e) { return null; }
    }
}
