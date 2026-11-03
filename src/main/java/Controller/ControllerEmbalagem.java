package Controller;

import Model.Dao.EmbalagemDAO;
import Model.Model.Embalagem;
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
 * Servlet do módulo Embalagem (módulo 21, Configuração). URL: /ControllerEmbalagem
 *
 * GET  [?idAlimento=&idArea=&ativo=true|false] → lista com estoque (saldo, preço médio, mínimo, fornecedor)
 * GET  ?id=ID → uma embalagem (edição)
 * POST acao=salvar → {idEmbalagem?, codigo?, descricao, pesoKg, idAlimento, idArea, precoEmbalador,
 *                     estoqueMinimo?, idFornecedor?, ativo?} → {ok, msg, target, embalagem}
 * POST acao=ativar → {idEmbalagem, ativo}
 *
 * Entradas (compras) e o histórico usam o Estoque de Insumos: POST /ControllerEstoque acao=entrada
 * {idInsumo = embalagem.idInsumo, ...} e GET /ControllerEstoque?acao=movimentos&idinsumo=.
 */
public class ControllerEmbalagem extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private final Gson gson = new Gson();
    private EmbalagemDAO dao;

    @Override
    public void init() { dao = new EmbalagemDAO(); }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        try (PrintWriter out = resp.getWriter()) {
            Integer id = intOrNull(req.getParameter("id"));
            if (id != null) {
                Embalagem e = dao.buscar(id);
                if (e != null) out.print(gson.toJson(e)); else writeJson(resp, 404, false, "Embalagem não encontrada.", null);
                return;
            }
            String at = req.getParameter("ativo");
            out.print(gson.toJson(dao.listar(intOrNull(req.getParameter("idAlimento")), intOrNull(req.getParameter("idArea")),
                    at == null || at.isBlank() ? null : Boolean.valueOf(at))));
        } catch (Exception e) {
            String cod = Util.LogUtil.erro(ControllerEmbalagem.class, "Falha tratada em ControllerEmbalagem.", e);
            writeJson(resp, 500, false, "Erro ao carregar embalagens: " + cod, null);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding(StandardCharsets.UTF_8.name());
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = req.getReader()) { String l; while ((l = r.readLine()) != null) sb.append(l); }
        JsonObject jo = gson.fromJson(sb.toString(), JsonObject.class);
        if (jo == null || !jo.has("acao")) { writeJson(resp, 400, false, "Ação não informada.", null); return; }
        String target = getStr(jo, "target");
        if (target == null || target.isBlank()) target = "page";
        try {
            switch (jo.get("acao").getAsString().toLowerCase()) {
                case "salvar" -> salvar(jo, resp, target);
                case "ativar" -> ativar(jo, resp, target);
                default -> writeJson(resp, 400, false, "Ação inválida.", null);
            }
        } catch (IllegalArgumentException e) {
            writeJson(resp, 400, false, e.getMessage(), Map.of("target", target));
        } catch (Exception e) {
            String cod = Util.LogUtil.erro(ControllerEmbalagem.class, "Falha tratada em ControllerEmbalagem.", e);
            writeJson(resp, 500, false, "Erro interno: " + cod, Map.of("target", target));
        }
    }

    private void salvar(JsonObject jo, HttpServletResponse resp, String target) throws Exception {
        Embalagem e = new Embalagem();
        e.setIdEmbalagem(getInt(jo, "idEmbalagem"));
        e.setCodigo(getStr(jo, "codigo"));
        e.setDescricao(getStr(jo, "descricao"));
        e.setPesoKg(getDecimal(jo, "pesoKg", "o peso (kg)"));
        int idAlimento = getInt(jo, "idAlimento"), idArea = getInt(jo, "idArea"), idForn = getInt(jo, "idFornecedor");
        e.setIdAlimento(idAlimento == 0 ? null : idAlimento);
        e.setIdArea(idArea == 0 ? null : idArea);
        e.setIdFornecedor(idForn == 0 ? null : idForn);
        e.setPrecoEmbalador(getDecimal(jo, "precoEmbalador", "o preço pago ao embalador"));
        e.setEstoqueMinimo(getDecimal(jo, "estoqueMinimo", "o estoque mínimo"));
        e.setAtivo(!jo.has("ativo") || jo.get("ativo").isJsonNull() || jo.get("ativo").getAsBoolean());
        boolean novo = e.getIdEmbalagem() == 0;
        Embalagem salva = dao.salvar(e);
        writeJson(resp, 200, true, novo ? "Embalagem cadastrada." : "Embalagem atualizada.",
                Map.of("target", target, "embalagem", salva));
    }

    private void ativar(JsonObject jo, HttpServletResponse resp, String target) throws Exception {
        int id = getInt(jo, "idEmbalagem");
        if (id == 0 || !jo.has("ativo")) { writeJson(resp, 400, false, "Embalagem e situação são obrigatórias.", Map.of("target", target)); return; }
        boolean ativo = jo.get("ativo").getAsBoolean();
        dao.definirAtivo(id, ativo);
        writeJson(resp, 200, true, ativo ? "Embalagem ativada." : "Embalagem desativada.", Map.of("target", target));
    }

    /* helpers */
    private void writeJson(HttpServletResponse resp, int status, boolean ok, String msg, Map<String, Object> extra) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setStatus(status);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ok", ok); m.put("msg", msg);
        if (extra != null) m.putAll(extra);
        try (PrintWriter out = resp.getWriter()) { out.print(gson.toJson(m)); }
    }
    private Integer intOrNull(String s) { try { return s == null || s.isBlank() ? null : Integer.parseInt(s.trim()); } catch (Exception e) { return null; } }
    private String getStr(JsonObject jo, String k) { return jo.has(k) && !jo.get(k).isJsonNull() ? jo.get(k).getAsString() : null; }
    private int getInt(JsonObject jo, String k) { try { return jo.has(k) && !jo.get(k).isJsonNull() ? jo.get(k).getAsInt() : 0; } catch (Exception e) { return 0; } }
    private BigDecimal getDecimal(JsonObject jo, String k, String campo) {
        try { return jo.has(k) && !jo.get(k).isJsonNull() && !jo.get(k).getAsString().isBlank() ? jo.get(k).getAsBigDecimal() : null; }
        catch (Exception e) { throw new IllegalArgumentException("Valor inválido em " + campo + "."); }
    }
}
