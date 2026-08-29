package Controller;

import Model.Dao.PagamentoDAO;
import Util.DataUtil;
import com.google.gson.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

/**
 * Servlet de PAGAMENTOS (comum a todos os tipos de funcionário).
 * URL: /ControllerPagamento
 *
 * GET  ?acao=listar&id={idpessoa}&periodo={YYYY-MM}
 * GET  ?acao=prazoclt&periodo={YYYY-MM}  → data-limite (5º dia útil do mês seguinte)
 * POST acao=registrar | excluir
 */
public class ControllerPagamento extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private PagamentoDAO dao;
    private final Gson gson = new Gson();

    @Override public void init() { dao = new PagamentoDAO(); }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        try (PrintWriter out = resp.getWriter()) {
            String acao = req.getParameter("acao");
            if ("prazoclt".equals(acao)) {
                String periodo = req.getParameter("periodo");
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("ok", true);
                m.put("prazo", DataUtil.quintoDiaUtilProximoMes(periodo).toString());
                out.print(gson.toJson(m));
                return;
            }
            if ("listar".equals(acao)) {
                int id = Integer.parseInt(req.getParameter("id"));
                String periodo = req.getParameter("periodo");
                out.print(gson.toJson(dao.listar(id, periodo)));
                return;
            }
            writeJson(resp, 400, false, "Ação inválida.");
        } catch (Exception e) {
            // P1-15/P2-20: pilha completa no log; ao usuário vai só o código.
            String cod = Util.LogUtil.erro(ControllerPagamento.class, "Falha tratada em ControllerPagamento.", e);
            writeJson(resp, 500, false, "Erro: " + cod);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding(StandardCharsets.UTF_8.name());
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = req.getReader()) { String l; while ((l = r.readLine()) != null) sb.append(l); }
        JsonObject jo = gson.fromJson(sb.toString(), JsonObject.class);
        if (jo == null || !jo.has("acao")) { writeJson(resp, 400, false, "Ação não informada."); return; }
        try {
            switch (jo.get("acao").getAsString().toLowerCase()) {
                case "registrar" -> handleRegistrar(jo, resp);
                case "excluir"   -> handleExcluir(jo, resp);
                default -> writeJson(resp, 400, false, "Ação inválida.");
            }
        } catch (Exception e) {
            // P1-15/P2-20: pilha completa no log; ao usuário vai só o código.
            String cod = Util.LogUtil.erro(ControllerPagamento.class, "Falha tratada em ControllerPagamento.", e);
            writeJson(resp, 500, false, "Erro interno: " + cod);
        }
    }

    private void handleRegistrar(JsonObject jo, HttpServletResponse resp) throws IOException, SQLException {
        int id = getInt(jo, "idpessoa");
        String periodo = getString(jo, "periodo");
        BigDecimal valor = getBD(jo, "valor");
        String forma = getString(jo, "forma");
        if (id == 0 || periodo == null || valor == null || valor.compareTo(BigDecimal.ZERO) <= 0 || forma == null) {
            writeJson(resp, 400, false, "Funcionário, período, valor e forma de pagamento são obrigatórios.");
            return;
        }
        forma = forma.toUpperCase();
        // Validação de dados bancários por forma
        if ("TRANSFERENCIA".equals(forma)) {
            if (isBlank(getString(jo, "banco")) || isBlank(getString(jo, "agencia")) || isBlank(getString(jo, "conta"))) {
                writeJson(resp, 400, false, "Para transferência informe banco, agência e conta.");
                return;
            }
        }
        if ("PIX".equals(forma) && isBlank(getString(jo, "chavepix"))) {
            writeJson(resp, 400, false, "Para PIX informe a chave.");
            return;
        }
        LocalDate dataPg = null;
        String dp = getString(jo, "datapagamento");
        if (dp != null && !dp.isBlank()) dataPg = LocalDate.parse(dp);

        dao.registrar(id, periodo, getString(jo, "tipofuncionario"), valor, forma,
                getString(jo, "banco"), getString(jo, "agencia"), getString(jo, "conta"),
                getString(jo, "chavepix"), dataPg, getString(jo, "observacao"));
        writeJson(resp, 200, true, "Pagamento registrado.");
    }

    private void handleExcluir(JsonObject jo, HttpServletResponse resp) throws IOException, SQLException {
        int id = getInt(jo, "idpagamento");
        if (id == 0) { writeJson(resp, 400, false, "ID obrigatório."); return; }
        dao.excluir(id);
        writeJson(resp, 200, true, "Pagamento excluído.");
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
    private boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
    private int getInt(JsonObject jo, String k) {
        try { return jo.has(k) && !jo.get(k).isJsonNull() ? jo.get(k).getAsInt() : 0; } catch (Exception e) { return 0; }
    }
    private BigDecimal getBD(JsonObject jo, String k) {
        try { return jo.has(k) && !jo.get(k).isJsonNull() ? jo.get(k).getAsBigDecimal() : null; } catch (Exception e) { return null; }
    }
    private String getString(JsonObject jo, String k) {
        return jo.has(k) && !jo.get(k).isJsonNull() ? jo.get(k).getAsString() : null;
    }
}
