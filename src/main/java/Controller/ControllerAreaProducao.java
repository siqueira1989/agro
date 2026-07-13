package Controller;

import Model.Dao.AreaProducaoDAO;
import Model.Model.AreaProducao;
import com.google.gson.Gson;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ControllerAreaProducao extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private AreaProducaoDAO dao;
    private final Gson gson = new Gson();

    @Override
    public void init() {
        dao = new AreaProducaoDAO();
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
            if (id != null && !id.isBlank()) {
                AreaProducao area = dao.getById(Integer.parseInt(id));
                out.print(gson.toJson(area));
            } else {
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
            case "delete": handleDelete(jo, resp); break;
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
                        "Já existe uma área com essa sigla.", "modal");
                return;
            }
            AreaProducao area = new AreaProducao();
            area.setPropriedadeAreaProducao(propriedade);
            area.setProprietarioAreaProducao(proprietario);
            area.setSiglasAreaProducao(sigla.toUpperCase());
            area.setCep(cep != null ? cep : "");
            area.setComplemento(complemento != null ? complemento : "");
            area.setNumero(numero != null ? numero : 0);
            area.setQuantidadeTotalPlantasAreaProducao(qtd != null ? qtd : 0);
            dao.insert(area);
            writeJson(resp, HttpServletResponse.SC_OK, true, "Área cadastrada com sucesso!", "page");
        } catch (SQLException e) {
            writeJson(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Erro ao cadastrar: " + e.getMessage(), "modal");
        }
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
                    "Dados obrigatórios não informados.", "modal");
            return;
        }
        try {
            if (dao.existeSiglaOutro(sigla, id)) {
                writeJson(resp, HttpServletResponse.SC_BAD_REQUEST, false,
                        "Já existe outra área com essa sigla.", "modal");
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
            writeJson(resp, HttpServletResponse.SC_OK, true, "Área atualizada com sucesso!", "page");
        } catch (SQLException e) {
            writeJson(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Erro ao atualizar: " + e.getMessage(), "modal");
        }
    }

    private void handleDelete(JsonObject jo, HttpServletResponse resp) throws IOException {
        Integer id = getInt(jo, "idareaproducao");
        if (id == null) {
            writeJson(resp, HttpServletResponse.SC_BAD_REQUEST, false, "ID não informado.", "page");
            return;
        }
        try {
            dao.delete(id);
            writeJson(resp, HttpServletResponse.SC_OK, true, "Área excluída com sucesso!", "page");
        } catch (SQLException e) {
            writeJson(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Erro ao excluir (verifique se há talões vinculados): " + e.getMessage(), "page");
        }
    }

    private boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }

    private String getStr(JsonObject jo, String campo) {
        return jo.has(campo) && !jo.get(campo).isJsonNull() ? jo.get(campo).getAsString() : null;
    }

    private Integer getInt(JsonObject jo, String campo) {
        try {
            return jo.has(campo) && !jo.get(campo).isJsonNull()
                    && !jo.get(campo).getAsString().trim().isEmpty()
                    ? jo.get(campo).getAsInt() : null;
        } catch (Exception e) { return null; }
    }
}
