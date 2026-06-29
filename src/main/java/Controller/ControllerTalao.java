package Controller;

import Model.Dao.AlimentoDAO;
import Model.Dao.AreaProducaoDAO;
import Model.Dao.TalaoDAO;
import Model.Model.Alimento;
import Model.Model.AreaProducao;
import Model.Model.Talao;
import Model.Model.TalaoFinanceiro;
import Util.PostgresConnection;
import Util.TransacaoUtil;
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
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ControllerTalao extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private TalaoDAO talaoDAO;
    private AreaProducaoDAO areaDAO;
    private final Gson gson = new Gson();

    @Override
    public void init() {
        talaoDAO = new TalaoDAO();
        areaDAO = new AreaProducaoDAO();
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
            String idArea = req.getParameter("idarea");
            String financeiro = req.getParameter("financeiro");

            if (financeiro != null && id != null) {
                TalaoFinanceiro tf = talaoDAO.getTalaoFinanceiroByTalao(Integer.parseInt(id));
                out.print(gson.toJson(tf));
            } else if (id != null && !id.isBlank()) {
                Talao talao = talaoDAO.getById(Integer.parseInt(id));
                out.print(gson.toJson(talao));
            } else if (idArea != null && !idArea.isBlank()) {
                List<Talao> lista = talaoDAO.listByAreaProducao(Integer.parseInt(idArea));
                out.print(gson.toJson(lista));
            } else {
                List<Talao> lista = talaoDAO.listAll();
                out.print(gson.toJson(lista));
            }
            out.flush();
        } catch (SQLException e) {
            writeJson(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Erro ao buscar talões: " + e.getMessage(), "page");
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
            case "criarfinanceiro": handleCriarFinanceiro(jo, resp); break;
            case "atualizarfinanceiro": handleAtualizarFinanceiro(jo, resp); break;
            default: writeJson(resp, HttpServletResponse.SC_BAD_REQUEST, false, "Ação inválida.", "page");
        }
    }

    private void handleCreate(JsonObject jo, HttpServletResponse resp) throws IOException {
        String descricao = getStr(jo, "descricaotalao");
        Integer idProduto = getInt(jo, "idproduto");
        Integer idArea = getInt(jo, "idareaproducao");
        Integer qtd = getInt(jo, "quantidadeplantastalao");

        if (isBlank(descricao) || idProduto == null || idArea == null) {
            writeJson(resp, HttpServletResponse.SC_BAD_REQUEST, false,
                    "Descrição, alimento e área são obrigatórios.", "modal");
            return;
        }
        try {
            Talao talao = new Talao();
            talao.setDescricaoTalao(descricao);
            Alimento alimento = new Alimento();
            alimento.setIdproduto(idProduto);
            talao.setAlimentoTalao(alimento);
            AreaProducao area = new AreaProducao();
            area.setIdAreaProducao(idArea);
            talao.setAreaproducaoTalao(area);
            talao.setQuantidadeplantasTalao(qtd != null ? qtd : 0);
            talaoDAO.insert(talao);
            writeJson(resp, HttpServletResponse.SC_OK, true, "Talão cadastrado com sucesso!", "page");
        } catch (SQLException e) {
            writeJson(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Erro ao cadastrar talão: " + e.getMessage(), "modal");
        }
    }

    private void handleUpdate(JsonObject jo, HttpServletResponse resp) throws IOException {
        Integer id = getInt(jo, "idtalao");
        String descricao = getStr(jo, "descricaotalao");
        Integer idProduto = getInt(jo, "idproduto");
        Integer idArea = getInt(jo, "idareaproducao");
        Integer qtd = getInt(jo, "quantidadeplantastalao");

        if (id == null || isBlank(descricao) || idProduto == null || idArea == null) {
            writeJson(resp, HttpServletResponse.SC_BAD_REQUEST, false,
                    "Dados obrigatórios não informados.", "modal");
            return;
        }
        try {
            Talao talao = new Talao();
            talao.setIdTalao(id);
            talao.setDescricaoTalao(descricao);
            Alimento alimento = new Alimento();
            alimento.setIdproduto(idProduto);
            talao.setAlimentoTalao(alimento);
            AreaProducao area = new AreaProducao();
            area.setIdAreaProducao(idArea);
            talao.setAreaproducaoTalao(area);
            talao.setQuantidadeplantasTalao(qtd != null ? qtd : 0);
            talaoDAO.update(talao);
            writeJson(resp, HttpServletResponse.SC_OK, true, "Talão atualizado com sucesso!", "page");
        } catch (SQLException e) {
            writeJson(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Erro ao atualizar talão: " + e.getMessage(), "modal");
        }
    }

    private void handleDelete(JsonObject jo, HttpServletResponse resp) throws IOException {
        Integer id = getInt(jo, "idtalao");
        if (id == null) {
            writeJson(resp, HttpServletResponse.SC_BAD_REQUEST, false, "ID não informado.", "page");
            return;
        }
        try {
            talaoDAO.delete(id);
            writeJson(resp, HttpServletResponse.SC_OK, true, "Talão excluído com sucesso!", "page");
        } catch (SQLException e) {
            writeJson(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Erro ao excluir talão: " + e.getMessage(), "page");
        }
    }

    private void handleCriarFinanceiro(JsonObject jo, HttpServletResponse resp) throws IOException {
        Integer idTalao = getInt(jo, "idtalao");
        String safra = getStr(jo, "safratalaofinanceiro");
        if (idTalao == null || isBlank(safra)) {
            writeJson(resp, HttpServletResponse.SC_BAD_REQUEST, false,
                    "Talão e safra são obrigatórios.", "modal");
            return;
        }
        try {
            TalaoFinanceiro tf = new TalaoFinanceiro();
            tf.setIdTalao(idTalao);
            tf.setSafraTalaoFinanceiro(safra);
            tf.setCustosafraTalaoFinanceiro(getDbl(jo, "custosafratalaofinanceiro"));
            tf.setDespesassafraTalaoFinanceiro(getDbl(jo, "despesassafratalaofinanceiro"));
            tf.setVendabrutassafraTalaoFinanceiro(getDbl(jo, "vendabrutasafratalaofinanceiro"));
            tf.setVendasliquidassafraTalaoFinanceiro(getDbl(jo, "vendaliquidasafratalaofinanceiro"));
            String inicio = getStr(jo, "iniciosafratalaofinanceiro");
            String termino = getStr(jo, "terminosafratalaofinanceiro");
            if (inicio != null && !inicio.isBlank())
                tf.setIniciosafraTalaoFinanceiro(java.sql.Date.valueOf(inicio));
            if (termino != null && !termino.isBlank())
                tf.setTerminosafraTalaoFinanceiro(java.sql.Date.valueOf(termino));

            try (Connection conn = new PostgresConnection().getConnection()) {
                TransacaoUtil.executar(conn, c -> talaoDAO.insertTalaoFinanceiro(tf));
            }
            writeJson(resp, HttpServletResponse.SC_OK, true, "Dados financeiros cadastrados!", "page");
        } catch (SQLException e) {
            writeJson(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Erro ao cadastrar financeiro: " + e.getMessage(), "modal");
        }
    }

    private void handleAtualizarFinanceiro(JsonObject jo, HttpServletResponse resp) throws IOException {
        Integer idTf = getInt(jo, "idtalaofinanceiro");
        Integer idTalao = getInt(jo, "idtalao");
        String safra = getStr(jo, "safratalaofinanceiro");
        if (idTf == null || idTalao == null || isBlank(safra)) {
            writeJson(resp, HttpServletResponse.SC_BAD_REQUEST, false,
                    "Dados obrigatórios não informados.", "modal");
            return;
        }
        try {
            TalaoFinanceiro tf = new TalaoFinanceiro();
            tf.setIdTalaoFinanceiro(idTf);
            tf.setIdTalao(idTalao);
            tf.setSafraTalaoFinanceiro(safra);
            tf.setCustosafraTalaoFinanceiro(getDbl(jo, "custosafratalaofinanceiro"));
            tf.setDespesassafraTalaoFinanceiro(getDbl(jo, "despesassafratalaofinanceiro"));
            tf.setVendabrutassafraTalaoFinanceiro(getDbl(jo, "vendabrutasafratalaofinanceiro"));
            tf.setVendasliquidassafraTalaoFinanceiro(getDbl(jo, "vendaliquidasafratalaofinanceiro"));
            String inicio = getStr(jo, "iniciosafratalaofinanceiro");
            String termino = getStr(jo, "terminosafratalaofinanceiro");
            if (inicio != null && !inicio.isBlank())
                tf.setIniciosafraTalaoFinanceiro(java.sql.Date.valueOf(inicio));
            if (termino != null && !termino.isBlank())
                tf.setTerminosafraTalaoFinanceiro(java.sql.Date.valueOf(termino));

            try (Connection conn = new PostgresConnection().getConnection()) {
                TransacaoUtil.executar(conn, c -> talaoDAO.updateTalaoFinanceiro(tf));
            }
            writeJson(resp, HttpServletResponse.SC_OK, true, "Dados financeiros atualizados!", "page");
        } catch (SQLException e) {
            writeJson(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Erro ao atualizar financeiro: " + e.getMessage(), "modal");
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

    private Double getDbl(JsonObject jo, String campo) {
        try {
            return jo.has(campo) && !jo.get(campo).isJsonNull() ? jo.get(campo).getAsDouble() : 0.0;
        } catch (Exception e) { return 0.0; }
    }
}
