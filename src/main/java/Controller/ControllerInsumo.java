package Controller;

import Model.Dao.InsumoDAO;
import Model.Dao.ParceiroDAO;
import Model.Model.CategoriaInsumo;
import Model.Model.Insumo;
import Model.Model.Parceiro;
import Model.Model.UnidadeMedida;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Servlet do catálogo de insumos. URL: /ControllerInsumo
 *
 * GET  (sem acao) → lista insumos (filtros ?categoria= &situacao=)
 * GET  ?id=            → um insumo (edição)
 * GET  ?acao=dados     → indicadores (total, abaixo do mínimo, valor em estoque)
 * GET  ?acao=fornecedores → parceiros tipo INSUMO ativos (para modal de entrada)
 * POST acao=create | update | delete(desativar/ativar)
 */
public class ControllerInsumo extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private final Gson gson = new Gson();
    private InsumoDAO insumoDAO;
    private ParceiroDAO parceiroDAO;

    @Override
    public void init() {
        insumoDAO = new InsumoDAO();
        parceiroDAO = new ParceiroDAO();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String acao = req.getParameter("acao");
        String idParam = req.getParameter("id");
        try (PrintWriter out = resp.getWriter()) {

            if ("dados".equalsIgnoreCase(acao)) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("total", insumoDAO.contarTotal());
                m.put("abaixoMinimo", insumoDAO.contarAbaixoMinimo());
                m.put("valorEstoque", insumoDAO.valorTotalEstoque());
                out.print(gson.toJson(m));
                return;
            }

            if ("fornecedores".equalsIgnoreCase(acao)) {
                List<Map<String, Object>> forns = new ArrayList<>();
                for (Parceiro p : parceiroDAO.listarPorTipo("INSUMO")) {
                    if (!p.isSituacaoPessoa()) continue;
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("idPessoa", p.getIdPessoa());
                    m.put("nome", p.getNomePessoa());
                    forns.add(m);
                }
                out.print(gson.toJson(forns));
                return;
            }

            if (idParam != null && !idParam.isBlank()) {
                Insumo i = insumoDAO.buscarPorId(Integer.parseInt(idParam.trim()));
                if (i != null) out.print(gson.toJson(i));
                else writeJson(resp, 404, false, "Insumo não encontrado.");
                return;
            }

            String categoria = req.getParameter("categoria");
            String sitParam = req.getParameter("situacao");
            Boolean situacao = sitParam == null || sitParam.isBlank() ? null : Boolean.valueOf(sitParam);
            out.print(gson.toJson(insumoDAO.listar(categoria, situacao)));

        } catch (Exception e) {
            // P1-15/P2-20: pilha completa no log; ao usuário vai só o código.
            String cod = Util.LogUtil.erro(ControllerInsumo.class, "Falha tratada em ControllerInsumo.", e);
            writeJson(resp, 500, false, "Erro ao carregar insumos.");
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
                case "create" -> salvar(jo, resp, false);
                case "update" -> salvar(jo, resp, true);
                case "delete" -> desativar(jo, resp);
                default -> writeJson(resp, 400, false, "Ação inválida.");
            }
        } catch (Exception e) {
            // P1-15/P2-20: pilha completa no log; ao usuário vai só o código.
            String cod = Util.LogUtil.erro(ControllerInsumo.class, "Falha tratada em ControllerInsumo.", e);
            writeJson(resp, 500, false, "Erro interno: " + cod);
        }
    }

    private void salvar(JsonObject jo, HttpServletResponse resp, boolean update) throws Exception {
        String nome = getStr(jo, "nome");
        String categoria = getStr(jo, "categoria");
        String grandeza = getStr(jo, "grandeza");
        String unidade = getStr(jo, "unidade");

        if (nome == null || nome.isBlank() || !CategoriaInsumo.isValido(categoria) || !UnidadeMedida.isValido(unidade)) {
            writeJson(resp, 400, false, "Nome, categoria e unidade válidos são obrigatórios."); return;
        }
        if (!UnidadeMedida.pertence(unidade, grandeza)) {
            writeJson(resp, 400, false, "Unidade não corresponde à grandeza informada."); return;
        }

        Insumo i = new Insumo();
        i.setNome(nome.trim());
        i.setCategoria(categoria.toUpperCase());
        i.setGrandeza(grandeza.toUpperCase());
        i.setUnidade(unidade.toUpperCase());
        i.setEstoqueMinimo(getBD(jo, "estoqueMinimo"));
        i.setIdFornecedor(getIntObj(jo, "idFornecedor"));
        i.setSituacao(!jo.has("situacao") || jo.get("situacao").getAsBoolean());

        if (update) {
            i.setIdInsumo(getInt(jo, "idInsumo"));
            if (i.getIdInsumo() == 0) { writeJson(resp, 400, false, "ID do insumo obrigatório."); return; }
            insumoDAO.atualizar(i);
            writeJson(resp, 200, true, "Insumo atualizado com sucesso!");
        } else {
            insumoDAO.inserir(i);
            writeJson(resp, 200, true, "Insumo cadastrado com sucesso!");
        }
    }

    private void desativar(JsonObject jo, HttpServletResponse resp) throws Exception {
        int id = getInt(jo, "idInsumo");
        if (id == 0) { writeJson(resp, 400, false, "ID do insumo obrigatório."); return; }
        boolean novaSituacao = jo.has("situacao") && !jo.get("situacao").getAsBoolean();
        insumoDAO.definirSituacao(id, novaSituacao);
        writeJson(resp, 200, true, novaSituacao ? "Insumo ativado." : "Insumo desativado.");
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
    private String getStr(JsonObject jo, String k) {
        return jo.has(k) && !jo.get(k).isJsonNull() ? jo.get(k).getAsString() : null;
    }
    private int getInt(JsonObject jo, String k) {
        try { return jo.has(k) && !jo.get(k).isJsonNull() ? jo.get(k).getAsInt() : 0; } catch (Exception e) { return 0; }
    }
    private Integer getIntObj(JsonObject jo, String k) {
        try {
            if (!jo.has(k) || jo.get(k).isJsonNull()) return null;
            int v = jo.get(k).getAsInt();
            return v == 0 ? null : v;
        } catch (Exception e) { return null; }
    }
    private BigDecimal getBD(JsonObject jo, String k) {
        try { return jo.has(k) && !jo.get(k).isJsonNull() ? jo.get(k).getAsBigDecimal() : BigDecimal.ZERO; }
        catch (Exception e) { return BigDecimal.ZERO; }
    }
}
