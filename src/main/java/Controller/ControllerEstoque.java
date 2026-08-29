package Controller;

import Model.Dao.EstoqueInsuficienteException;
import Model.Dao.EstoqueMovimentoDAO;
import Model.Dao.ParceiroDAO;
import Model.Model.Parceiro;
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
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Servlet das movimentações de estoque. URL: /ControllerEstoque
 *
 * GET  ?acao=movimentos&idinsumo={id} → histórico do insumo
 * POST acao=entrada  → soma ao saldo (recalcula preço médio); fornecedor tipo INSUMO
 * POST acao=saida    → baixa do saldo; bloqueia se saldo insuficiente
 */
public class ControllerEstoque extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private final Gson gson = new Gson();
    private EstoqueMovimentoDAO movDAO;
    private ParceiroDAO parceiroDAO;

    @Override
    public void init() {
        movDAO = new EstoqueMovimentoDAO();
        parceiroDAO = new ParceiroDAO();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        try (PrintWriter out = resp.getWriter()) {
            if ("movimentos".equalsIgnoreCase(req.getParameter("acao"))) {
                String idParam = req.getParameter("idinsumo");
                if (idParam == null || idParam.isBlank()) { writeJson(resp, 400, false, "Insumo obrigatório."); return; }
                out.print(gson.toJson(movDAO.listarPorInsumo(Integer.parseInt(idParam.trim()))));
                return;
            }
            writeJson(resp, 400, false, "Ação inválida.");
        } catch (Exception e) {
            // P1-15/P2-20: pilha completa no log; ao usuário vai só o código.
            String cod = Util.LogUtil.erro(ControllerEstoque.class, "Falha tratada em ControllerEstoque.", e);
            writeJson(resp, 500, false, "Erro ao carregar movimentações.");
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
                case "entrada" -> entrada(jo, resp);
                case "saida"   -> saida(jo, resp);
                default -> writeJson(resp, 400, false, "Ação inválida.");
            }
        } catch (EstoqueInsuficienteException e) {
            writeJson(resp, 400, false, e.getMessage());
        } catch (Exception e) {
            // P1-15/P2-20: pilha completa no log; ao usuário vai só o código.
            String cod = Util.LogUtil.erro(ControllerEstoque.class, "Falha tratada em ControllerEstoque.", e);
            writeJson(resp, 500, false, "Erro interno: " + cod);
        }
    }

    private void entrada(JsonObject jo, HttpServletResponse resp) throws Exception {
        int idInsumo = getInt(jo, "idInsumo");
        BigDecimal qtd = getBD(jo, "quantidade");
        BigDecimal preco = getBD(jo, "precoUnitario");
        Integer idParceiro = getIntObj(jo, "idParceiro");
        LocalDate data = getData(jo, "data");
        String obs = getStr(jo, "observacao");

        if (idInsumo == 0 || qtd == null || qtd.compareTo(BigDecimal.ZERO) <= 0
                || preco == null || preco.compareTo(BigDecimal.ZERO) < 0) {
            writeJson(resp, 400, false, "Insumo, quantidade (>0) e preço são obrigatórios."); return;
        }
        // Entrada exige fornecedor do tipo INSUMO
        if (idParceiro == null) { writeJson(resp, 400, false, "Selecione o fornecedor (tipo insumo)."); return; }
        Parceiro p = parceiroDAO.getParceiroById(idParceiro);
        if (p == null || !"INSUMO".equalsIgnoreCase(p.getTipoParceiro())) {
            writeJson(resp, 400, false, "Fornecedor inválido: deve ser um parceiro do tipo Insumo."); return;
        }

        movDAO.registrarEntrada(idInsumo, qtd, preco, idParceiro, data, obs);
        writeJson(resp, 200, true, "Entrada registrada com sucesso!");
    }

    private void saida(JsonObject jo, HttpServletResponse resp) throws Exception {
        int idInsumo = getInt(jo, "idInsumo");
        BigDecimal qtd = getBD(jo, "quantidade");
        LocalDate data = getData(jo, "data");
        String obs = getStr(jo, "observacao");

        if (idInsumo == 0 || qtd == null || qtd.compareTo(BigDecimal.ZERO) <= 0) {
            writeJson(resp, 400, false, "Insumo e quantidade (>0) são obrigatórios."); return;
        }
        movDAO.registrarSaida(idInsumo, qtd, data, obs);
        writeJson(resp, 200, true, "Saída registrada com sucesso!");
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
    private LocalDate getData(JsonObject jo, String k) {
        String s = getStr(jo, k);
        return (s == null || s.isBlank()) ? LocalDate.now() : LocalDate.parse(s);
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
        try { return jo.has(k) && !jo.get(k).isJsonNull() ? jo.get(k).getAsBigDecimal() : null; }
        catch (Exception e) { return null; }
    }
}
