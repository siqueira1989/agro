package Controller;

import Model.Dao.RomaneioDAO;
import Model.Model.Romaneio;
import Model.Model.RomaneioDespesa;
import Model.Model.RomaneioItem;
import Model.Model.RomaneioPagamento;
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
import java.util.List;
import java.util.Map;

/**
 * Servlet do Módulo Romaneio (V33). URL: /ControllerRomaneio
 *
 * GET  (sem parâmetro)[?status=]      → lista de romaneios
 * GET  ?lotes=1                       → lotes PROCESSADO com caixas embaladas ainda não romaneadas (botão "Romaneio Cad.")
 * GET  ?contexto=1&lote=ID | &id=ID   → dados da tela MR1 (lote, safra, parceiros, classificações, embalagens com saldo,
 *                                       despesas de venda do cadastro e, na edição, o romaneio)
 * GET  ?id=ID                         → romaneio com itens e despesas
 * GET  ?parceiros=1                   → parceiros PARCEIRO com qtd/saldo de romaneios PENDENTES
 * GET  ?pendentes=ID_PARCEIRO         → romaneios PENDENTES do parceiro (mais antigo primeiro)
 * GET  ?pagamentos=ID_ROMANEIO        → histórico de pagamentos do romaneio
 * GET  ?pdf=ID_ROMANEIO               → documento do romaneio em PDF (attachment Romaneio_<numero>.pdf)
 * POST acao=salvar     → {idRomaneio?, idLote, idParceiro, dataEmissao, motorista?, placa?, observacao?,
 *                         itens[{idClassificacao, idEmbalagem, caixas, precoUnitario?}],
 *                         despesas[{idDespesasCusto?, descricao, formaCalculo, percentual?, valorUnitario?, valor?}]}
 *                         → {ok, msg, target, romaneio}
 * POST acao=cancelar   → {idRomaneio}
 * POST acao=pagar      → {idParceiro, data, forma, valor, banco?, numeroCheque?, emitente?, bomPara?, observacao?,
 *                         itens[{idRomaneio, valor}]} → {ok, msg, target, idPagamento}
 * POST acao=estornar   → {idPagamento}
 */
public class ControllerRomaneio extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private final RomaneioDAO dao = new RomaneioDAO();

    private final Gson gson = new GsonBuilder()
        .registerTypeAdapter(LocalDate.class, (JsonSerializer<LocalDate>) (s, t, c) -> new JsonPrimitive(s.toString()))
        .create();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getParameter("pdf") != null) { handlePdf(req, resp); return; }
        resp.setContentType("application/json");
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        try {
            Object res;
            if (req.getParameter("lotes") != null) res = dao.lotesDisponiveis();
            else if (req.getParameter("contexto") != null) res = dao.contexto(intOrNull(req.getParameter("lote")), intOrNull(req.getParameter("id")));
            else if (req.getParameter("parceiros") != null) res = dao.listarParceiros();
            else if (req.getParameter("pendentes") != null) res = dao.pendentesDoParceiro(intParam(req, "pendentes"));
            else if (req.getParameter("pagamentos") != null) res = dao.pagamentosDoRomaneio(intParam(req, "pagamentos"));
            else if (req.getParameter("id") != null) {
                res = dao.buscar(intParam(req, "id"));
                if (res == null) { writeJson(resp, 404, false, "Romaneio não encontrado.", "page"); return; }
            }
            else res = dao.listar(req.getParameter("status"));
            try (PrintWriter out = resp.getWriter()) { out.print(gson.toJson(res)); }
        } catch (IllegalArgumentException e) {
            writeJson(resp, 400, false, e.getMessage(), "page");
        } catch (Exception e) {
            String cod = Util.LogUtil.erro(ControllerRomaneio.class, "Falha tratada em ControllerRomaneio (GET).", e);
            writeJson(resp, 500, false, Util.LogUtil.mensagemUsuario("carregar os romaneios", cod), "page");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding(StandardCharsets.UTF_8.name());
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = req.getReader()) { String l; while ((l = r.readLine()) != null) sb.append(l); }
        JsonObject jo;
        try { jo = gson.fromJson(sb.toString(), JsonObject.class); } catch (Exception e) { jo = null; }
        if (jo == null || !jo.has("acao")) { writeJson(resp, 400, false, "Ação não informada.", "page"); return; }
        String target = getStr(jo, "target");
        if (target == null || target.isBlank()) target = "page";
        try {
            switch (jo.get("acao").getAsString().toLowerCase()) {
                case "salvar": handleSalvar(jo, resp, target); break;
                case "cancelar": {
                    int id = getInt(jo, "idRomaneio");
                    if (id == 0) { writeJson(resp, 400, false, "Romaneio não informado.", target); return; }
                    dao.cancelar(id);
                    writeJson(resp, 200, true, "Romaneio cancelado. As caixas voltaram para o saldo do lote.", target);
                    break;
                }
                case "pagar": handlePagar(jo, resp, target); break;
                case "estornar": {
                    int id = getInt(jo, "idPagamento");
                    if (id == 0) { writeJson(resp, 400, false, "Pagamento não informado.", target); return; }
                    List<String> numeros = dao.estornarPagamento(id);
                    writeJson(resp, 200, true, "Pagamento estornado (romaneio(s) " + String.join(", ", numeros) + ").", target);
                    break;
                }
                default: writeJson(resp, 400, false, "Ação inválida.", target);
            }
        } catch (IllegalArgumentException e) {
            writeJson(resp, 400, false, e.getMessage(), target);
        } catch (Exception e) {
            String cod = Util.LogUtil.erro(ControllerRomaneio.class, "Falha tratada em ControllerRomaneio (POST).", e);
            writeJson(resp, 500, false, Util.LogUtil.mensagemUsuario("gravar o romaneio", cod), target);
        }
    }

    private void handleSalvar(JsonObject jo, HttpServletResponse resp, String target) throws Exception {
        Romaneio r = new Romaneio();
        r.setIdRomaneio(getInt(jo, "idRomaneio"));
        r.setIdLote(getInt(jo, "idLote"));
        if (r.getIdLote() == 0) { writeJson(resp, 400, false, "Lote não informado.", target); return; }
        r.setIdParceiro(getInt(jo, "idParceiro"));
        r.setDataEmissao(getData(jo, "dataEmissao", "Data de emissão inválida."));
        r.setMotorista(getStr(jo, "motorista"));
        r.setPlaca(getStr(jo, "placa"));
        r.setObservacao(getStr(jo, "observacao"));
        r.setNotaFiscal(getStr(jo, "notaFiscal"));
        r.setSerieNf(getStr(jo, "serieNf"));
        r.setTransportadora(getStr(jo, "transportadora"));
        r.setTransportadoraDoc(getStr(jo, "transportadoraDoc"));
        r.setMotoristaCpf(getStr(jo, "motoristaCpf"));
        r.setMotoristaCnh(getStr(jo, "motoristaCnh"));
        r.setPlacaCarreta(getStr(jo, "placaCarreta"));
        r.setUfVeiculo(getStr(jo, "ufVeiculo"));
        r.setTipoVeiculo(getStr(jo, "tipoVeiculo"));
        if (jo.has("itens") && jo.get("itens").isJsonArray()) {
            for (JsonElement el : jo.getAsJsonArray("itens")) {
                JsonObject o = el.getAsJsonObject();
                RomaneioItem i = new RomaneioItem();
                i.setIdClassificacao(getInt(o, "idClassificacao"));
                i.setIdEmbalagem(getInt(o, "idEmbalagem"));
                i.setCaixas(getDecimal(o, "caixas"));
                i.setPrecoUnitario(getDecimal(o, "precoUnitario"));
                r.getItens().add(i);
            }
        }
        if (jo.has("despesas") && jo.get("despesas").isJsonArray()) {
            for (JsonElement el : jo.getAsJsonArray("despesas")) {
                JsonObject o = el.getAsJsonObject();
                RomaneioDespesa d = new RomaneioDespesa();
                int idd = getInt(o, "idDespesasCusto");
                d.setIdDespesasCusto(idd == 0 ? null : idd);
                d.setDescricao(getStr(o, "descricao"));
                d.setFormaCalculo(getStr(o, "formaCalculo"));
                d.setPercentual(getDecimal(o, "percentual"));
                d.setValorUnitario(getDecimal(o, "valorUnitario"));
                d.setValor(getDecimal(o, "valor"));
                r.getDespesas().add(d);
            }
        }
        boolean novo = r.getIdRomaneio() == 0;
        Romaneio salvo = dao.salvar(r);
        String msg = (novo ? "Romaneio " + salvo.getNumero() + " emitido" : "Romaneio " + salvo.getNumero() + " atualizado")
                + " — situação " + salvo.getStatus() + ".";
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ok", true); m.put("msg", msg); m.put("target", target); m.put("romaneio", salvo);
        writeJsonRaw(resp, 200, m);
    }

    private void handlePagar(JsonObject jo, HttpServletResponse resp, String target) throws Exception {
        RomaneioPagamento p = new RomaneioPagamento();
        p.setIdParceiro(getInt(jo, "idParceiro"));
        p.setDataPagamento(getData(jo, "data", "Data do pagamento inválida."));
        String forma = getStr(jo, "forma");
        p.setForma(forma == null ? null : forma.trim().toUpperCase());
        p.setValor(getDecimal(jo, "valor"));
        p.setBanco(getStr(jo, "banco"));
        p.setNumeroCheque(getStr(jo, "numeroCheque"));
        p.setEmitente(getStr(jo, "emitente"));
        p.setBomPara(getData(jo, "bomPara", "Data \"bom para\" inválida."));
        p.setObservacao(getStr(jo, "observacao"));
        if (jo.has("itens") && jo.get("itens").isJsonArray()) {
            for (JsonElement el : jo.getAsJsonArray("itens")) {
                JsonObject o = el.getAsJsonObject();
                BigDecimal v = getDecimal(o, "valor");
                if (v == null || v.signum() == 0) continue;   // romaneio da lista sem valor distribuído
                p.getItens().add(new RomaneioPagamento.Item(getInt(o, "idRomaneio"), v));
            }
        }
        int id = dao.registrarPagamento(p);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ok", true); m.put("msg", "Pagamento registrado."); m.put("target", target); m.put("idPagamento", id);
        writeJsonRaw(resp, 200, m);
    }

    /** Documento do romaneio em PDF (modelo do usuário), para baixar: Romaneio_<numero>.pdf. */
    private void handlePdf(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Integer id = intOrNull(req.getParameter("pdf"));
        if (id == null) {
            resp.setContentType("application/json"); resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
            writeJson(resp, 400, false, "Romaneio inválido.", "page"); return;
        }
        try {
            Map<String, Object> doc = dao.documento(id);
            if (doc == null) {
                resp.setContentType("application/json"); resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
                writeJson(resp, 404, false, "Romaneio não encontrado.", "page"); return;
            }
            byte[] pdf = Service.RomaneioDocumentoPdf.gerar(doc);
            String nome = "Romaneio_" + String.valueOf(doc.get("numero")).replaceAll("[^A-Za-z0-9_-]", "") + ".pdf";
            resp.setContentType("application/pdf");
            resp.setHeader("Content-Disposition", "attachment; filename=\"" + nome + "\"");
            resp.setContentLength(pdf.length);
            resp.getOutputStream().write(pdf);
        } catch (Exception e) {
            String cod = Util.LogUtil.erro(ControllerRomaneio.class, "Falha ao gerar o PDF do romaneio " + id + ".", e);
            resp.reset();
            writeJson(resp, 500, false, Util.LogUtil.mensagemUsuario("gerar o PDF do romaneio", cod), "page");
        }
    }

    private int intParam(HttpServletRequest req, String k) {
        Integer v = intOrNull(req.getParameter(k));
        if (v == null) throw new IllegalArgumentException("Parâmetro " + k + " inválido.");
        return v;
    }
    private Integer intOrNull(String s) { try { return s == null || s.isBlank() ? null : Integer.parseInt(s.trim()); } catch (Exception e) { return null; } }
    private int getInt(JsonObject jo, String k) { try { return jo.has(k) && !jo.get(k).isJsonNull() ? jo.get(k).getAsInt() : 0; } catch (Exception e) { return 0; } }
    private String getStr(JsonObject jo, String k) { return jo.has(k) && !jo.get(k).isJsonNull() ? jo.get(k).getAsString() : null; }
    private BigDecimal getDecimal(JsonObject jo, String k) {
        if (!jo.has(k) || jo.get(k).isJsonNull()) return null;
        String s = jo.get(k).getAsString().trim();
        if (s.isEmpty()) return null;
        try { return new BigDecimal(s.contains(",") ? s.replace(".", "").replace(',', '.') : s); }
        catch (Exception e) { throw new IllegalArgumentException("Valor numérico inválido em " + k + "."); }
    }
    private LocalDate getData(JsonObject jo, String k, String erro) {
        String s = getStr(jo, k);
        if (s == null || s.isBlank()) return null;
        try { return LocalDate.parse(s.trim()); } catch (Exception e) { throw new IllegalArgumentException(erro); }
    }

    private void writeJsonRaw(HttpServletResponse resp, int status, Map<String, Object> m) throws IOException {
        resp.setContentType("application/json"); resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setStatus(status);
        try (PrintWriter out = resp.getWriter()) { out.print(gson.toJson(m)); }
    }

    private void writeJson(HttpServletResponse resp, int status, boolean ok, String msg, String target) throws IOException {
        Map<String, Object> m = new LinkedHashMap<>(); m.put("ok", ok); m.put("msg", msg); m.put("target", target);
        writeJsonRaw(resp, status, m);
    }
}
