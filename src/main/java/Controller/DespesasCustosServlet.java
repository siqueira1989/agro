package Controller;

import Model.Dao.DespesasCustosDAO;
import Model.Model.DespesasCustos;
import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DespesasCustosServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private DespesasCustosDAO despesasCustosDAO;
    private final Gson gson = new Gson();

    @Override
    public void init() {
        despesasCustosDAO = new DespesasCustosDAO();
    }

    /* ============================================================
       Helpers: resposta JSON padronizada e parse de moeda BRL
       ============================================================ */
    private void writeJson(HttpServletResponse resp, int status, boolean ok, String msg, String target) throws IOException {
        writeJson(resp, status, ok, msg, target, null);
    }

    /** Mesmo padrão {ok,msg,target} com campos extras (ex.: encontrado, registro, embalagens). */
    private void writeJson(HttpServletResponse resp, int status, boolean ok, String msg, String target,
                           Map<String, Object> extras) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());

        Map<String, Object> payload = new HashMap<>();
        if (extras != null) payload.putAll(extras);
        payload.put("ok", ok);
        payload.put("msg", msg);
        if (target != null) payload.put("target", target);

        resp.setStatus(status);
        try (PrintWriter out = resp.getWriter()) {
            out.print(gson.toJson(payload));
            out.flush();
        }
    }

    private double parseBRL(String valorBRL) {
        if (valorBRL == null) return 0.0;
        String normalized = valorBRL.replace(".", "").replace(",", ".");
        return Double.parseDouble(normalized);
    }

    /* ============================================================
       GET: lista para DataTables (array). Em erro, {ok:false,...}
       ============================================================ */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        if (request.getParameter("precoCaixa") != null) { handleBuscarPrecoCaixa(request, response); return; }
        if (request.getParameter("embalagens") != null) { handleListarEmbalagens(request, response); return; }
        if (request.getParameter("tiposCaixa") != null) { handleTiposCaixa(response); return; }
        try {
            List<DespesasCustos> despesasCustosList = despesasCustosDAO.listAll();
            try (PrintWriter out = response.getWriter()) {
                out.print(gson.toJson(despesasCustosList));
                out.flush();
            }
        } catch (SQLException e) {
            // P1-15/P2-20: pilha completa no log; ao usuário vai só o código.
            String cod = Util.LogUtil.erro(DespesasCustosServlet.class, "Falha tratada em DespesasCustosServlet.", e);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false, "Erro ao listar: " + cod, "page");
        }
    }

    /* ============================================================
       POST: create | update | delete com JSON padronizado
       ============================================================ */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());

        String acao = request.getParameter("acao");
        if ("create".equalsIgnoreCase(acao)) {
            handleCreate(request, response);
        } else if ("update".equalsIgnoreCase(acao)) {
            handleUpdate(request, response);
        } else if ("delete".equalsIgnoreCase(acao)) {
            handleDelete(request, response);
        } else if ("registrarPrecoCaixa".equalsIgnoreCase(acao)) {
            handleRegistrarPrecoCaixa(request, response);
        } else {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, "Ação inválida.", "page");
        }
    }

    /* ----------------------------- CREATE ----------------------------- */
    private void handleCreate(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            // Nomes de parâmetros iguais aos do seu JS:
            String despesascusto        = request.getParameter("despesascusto");
            String unidadedespesacusto  = request.getParameter("unidadedespesacusto");
            String valordespesacusto    = request.getParameter("valordespesacusto");
            String tipodespesacusto     = request.getParameter("tipodespesacusto");
            String classificacao        = request.getParameter("classificacao");

            // Regras simples de validação (opcional, mas útil):
            if (isBlank(despesascusto) || isBlank(unidadedespesacusto) || isBlank(valordespesacusto) || isBlank(tipodespesacusto)) {
                writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, "Todos os campos são obrigatórios.", "modalCadastro");
                return;
            }
            if (!"FIXO".equalsIgnoreCase(classificacao) && !"VARIAVEL".equalsIgnoreCase(classificacao)) {
                writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, "Classificação deve ser Fixo ou Variável.", "modalCadastro");
                return;
            }

            // V28: categoria (GERAL padrão). Caixa/embalagem exige alimento, área e tipo.
            String categoria = request.getParameter("categoria");
            categoria = isBlank(categoria) ? DespesasCustos.CATEGORIA_GERAL : categoria.trim().toUpperCase();
            if (!DespesasCustos.isCategoriaValida(categoria)) {
                writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, "Categoria inválida.", "modalCadastro");
                return;
            }
            Integer idAlimento = null, idArea = null;
            String tipoCaixa = null;
            if (!DespesasCustos.CATEGORIA_GERAL.equals(categoria)) {
                idAlimento = parseIntOuNull(request.getParameter("idAlimento"));
                idArea = parseIntOuNull(request.getParameter("idArea"));
                tipoCaixa = request.getParameter("tipoCaixa");
                if (idAlimento == null || idArea == null) {
                    writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, "Selecione o alimento e a área de produção.", "modalCadastro");
                    return;
                }
                if (DespesasCustos.CATEGORIA_EMBALAGEM.equals(categoria) && isBlank(tipoCaixa)) {
                    writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, "Informe o tipo de embalagem.", "modalCadastro");
                    return;
                }
                tipoCaixa = isBlank(tipoCaixa) ? "CONTENTOR" : DespesasCustosDAO.normalizarTipoCaixa(tipoCaixa);
                DespesasCustos existente = despesasCustosDAO.buscarPrecoCaixa(categoria, idAlimento, idArea, tipoCaixa);
                if (existente != null) {
                    writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false,
                            "Já existe preço para este alimento/área/tipo (ID " + existente.getIddespesascusto() + "). Edite o registro.",
                            "modalCadastro");
                    return;
                }
            }

            // Anti-duplicidade
            if (despesasCustosDAO.existeDespesaCusto(despesascusto)) {
                writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, "Despesa/Custo já cadastrada.", "ModalCadastro");
                return;
            }

            double valor = parseBRL(valordespesacusto);

            DespesasCustos bean = new DespesasCustos();
            bean.setDespesascusto(despesascusto);
            bean.setUnidadedespesascustos(unidadedespesacusto);
            bean.setValordespesascustos(valor);
            bean.setTipodespesascustos(tipodespesacusto);
            bean.setClassificacao(classificacao.toUpperCase());
            bean.setCategoria(categoria);
            bean.setIdAlimento(idAlimento);
            bean.setIdAreaProducao(idArea);
            bean.setTipoCaixa(tipoCaixa);

            despesasCustosDAO.create(bean);

            writeJson(response, HttpServletResponse.SC_OK, true, "Despesa cadastrada com sucesso!", "page");
        } catch (IllegalArgumentException e) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, e.getMessage(), "modalCadastro");
        } catch (Exception e) {
            // P1-15/P2-20: pilha completa no log; ao usuário vai só o código.
            String cod = Util.LogUtil.erro(DespesasCustosServlet.class, "Falha tratada em DespesasCustosServlet.", e);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false, "Erro ao cadastrar: " + cod, "modalCadastro");
        }
    }

    /* ----------------------------- UPDATE ----------------------------- */
    private void handleUpdate(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            // Nomes de parâmetros iguais aos do seu JS:
            String iddespesascustos     = request.getParameter("iddespesascustos");
            String despesascustos       = request.getParameter("despesascustos");
            String unidadedespesacustos = request.getParameter("unidadedespesacustos");
            String valordespesacustos   = request.getParameter("valordespesacustos");
            String tipodespesacustos    = request.getParameter("tipodespesacustos");
            String classificacao        = request.getParameter("classificacao");

            if (isBlank(iddespesascustos) || isBlank(despesascustos) || isBlank(unidadedespesacustos) ||
                isBlank(valordespesacustos) || isBlank(tipodespesacustos)) {
                writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, "Todos os campos são obrigatórios.", "modalAtualizacao");
                return;
            }
            if (!"FIXO".equalsIgnoreCase(classificacao) && !"VARIAVEL".equalsIgnoreCase(classificacao)) {
                writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, "Classificação deve ser Fixo ou Variável.", "modalAtualizacao");
                return;
            }

            double valor = parseBRL(valordespesacustos);

            DespesasCustos bean = new DespesasCustos();
            bean.setIddespesascusto(Integer.parseInt(iddespesascustos));
            bean.setDespesascusto(despesascustos);
            bean.setUnidadedespesascustos(unidadedespesacustos);
            bean.setValordespesascustos(valor);
            bean.setTipodespesascustos(tipodespesacustos);
            bean.setClassificacao(classificacao.toUpperCase());
            if (despesasCustosDAO.VerificarDadosUpdate(bean)) {
                writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, "Despesa/Custo está igual ao cadastro anterior.", "modalAtualizacao");
                return;
            }
            if (despesasCustosDAO.ExisteEmOutroRegistroDespesasCustos(bean)) {
                writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, "Já existe uma Despesa/Custo cadastrada com esses dados.", "modalAtualizacao");
                return;
            }
            despesasCustosDAO.update(bean);

            writeJson(response, HttpServletResponse.SC_OK, true, "Despesa atualizada com sucesso!", "page");
        } catch (Exception e) {
            // P1-15/P2-20: pilha completa no log; ao usuário vai só o código.
            String cod = Util.LogUtil.erro(DespesasCustosServlet.class, "Falha tratada em DespesasCustosServlet.", e);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false, "Erro ao atualizar: " + cod, "modalAtualizacao");
        }
    }

    /* ----------------------------- DELETE ----------------------------- */
    private void handleDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String id = request.getParameter("iddespesascusto");
            if (isBlank(id)) {
                writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, "ID é obrigatório para excluir.", "page");
                return;
            }

            DespesasCustos bean = new DespesasCustos();
            bean.setIddespesascusto(Integer.parseInt(id));

            despesasCustosDAO.delete(bean);

            writeJson(response, HttpServletResponse.SC_OK, true, "Excluída com sucesso!", "page");
        } catch (Exception e) {
            // P1-15/P2-20: pilha completa no log; ao usuário vai só o código.
            String cod = Util.LogUtil.erro(DespesasCustosServlet.class, "Falha tratada em DespesasCustosServlet.", e);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false, "Erro ao excluir: " + cod, "page");
        }
    }

    /* ============================================================
       V28 — Preço de caixa/embalagem por alimento e área.
       Contrato documentado em docs/modulos/despesas-custos.md.
       ============================================================ */

    /** GET ?precoCaixa=1&categoria=&idAlimento=&idArea=&tipoCaixa= → {ok,msg,target,encontrado,registro?} */
    private void handleBuscarPrecoCaixa(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            Integer idAlimento = parseIntOuNull(request.getParameter("idAlimento"));
            Integer idArea = parseIntOuNull(request.getParameter("idArea"));
            if (idAlimento == null || idArea == null) {
                writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, "idAlimento e idArea são obrigatórios.", "page");
                return;
            }
            DespesasCustos d = despesasCustosDAO.buscarPrecoCaixa(request.getParameter("categoria"),
                    idAlimento, idArea, request.getParameter("tipoCaixa"));
            Map<String, Object> extras = new HashMap<>();
            extras.put("encontrado", d != null);
            if (d != null) extras.put("registro", d);
            writeJson(response, HttpServletResponse.SC_OK, true,
                    d != null ? "Preço encontrado." : "Preço não cadastrado para este alimento/área/tipo.", "page", extras);
        } catch (IllegalArgumentException e) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, e.getMessage(), "page");
        } catch (SQLException e) {
            String cod = Util.LogUtil.erro(DespesasCustosServlet.class, "Falha ao buscar preço de caixa.", e);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false, "Erro ao buscar preço: " + cod, "page");
        }
    }

    /** GET ?embalagens=1&idAlimento=&idArea= → {ok,msg,target,embalagens:[...]} */
    private void handleListarEmbalagens(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            Integer idAlimento = parseIntOuNull(request.getParameter("idAlimento"));
            Integer idArea = parseIntOuNull(request.getParameter("idArea"));
            if (idAlimento == null || idArea == null) {
                writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, "idAlimento e idArea são obrigatórios.", "page");
                return;
            }
            List<DespesasCustos> lista = despesasCustosDAO.listarEmbalagens(idAlimento, idArea);
            Map<String, Object> extras = new HashMap<>();
            extras.put("embalagens", lista);
            writeJson(response, HttpServletResponse.SC_OK, true,
                    lista.isEmpty() ? "Nenhuma embalagem cadastrada para este alimento/área." : "", "page", extras);
        } catch (SQLException e) {
            String cod = Util.LogUtil.erro(DespesasCustosServlet.class, "Falha ao listar embalagens.", e);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false, "Erro ao listar embalagens: " + cod, "page");
        }
    }

    /** GET ?tiposCaixa=1 → {ok,msg,target,tipos:[{codigo,rotulo,cargo}]} — sugestões do enum TipoCaixa. */
    private void handleTiposCaixa(HttpServletResponse response) throws IOException {
        List<Map<String, String>> tipos = new java.util.ArrayList<>();
        for (Model.Model.TipoCaixa t : Model.Model.TipoCaixa.values()) {
            Map<String, String> m = new HashMap<>();
            m.put("codigo", t.name());
            m.put("rotulo", t.getRotulo());
            m.put("cargo", t.getCargo());
            tipos.add(m);
        }
        Map<String, Object> extras = new HashMap<>();
        extras.put("tipos", tipos);
        writeJson(response, HttpServletResponse.SC_OK, true, "", "page", extras);
    }

    /**
     * POST acao=registrarPrecoCaixa&categoria=&idAlimento=&idArea=&tipoCaixa=&descricao=&valor=
     * → upsert; {ok,msg,target,id,registro}. {@code valor} aceita "3,50", "1.234,56" ou "3.50".
     */
    private void handleRegistrarPrecoCaixa(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String target = isBlank(request.getParameter("target")) ? "page" : request.getParameter("target");
        try {
            Integer idAlimento = parseIntOuNull(request.getParameter("idAlimento"));
            Integer idArea = parseIntOuNull(request.getParameter("idArea"));
            if (idAlimento == null || idArea == null) {
                writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, "Selecione o alimento e a área de produção.", target);
                return;
            }
            java.math.BigDecimal valor = parseValor(request.getParameter("valor"));
            String categoria = request.getParameter("categoria");
            boolean existia = despesasCustosDAO.buscarPrecoCaixa(categoria, idAlimento, idArea, request.getParameter("tipoCaixa")) != null;
            int id = despesasCustosDAO.registrarPrecoCaixa(categoria, idAlimento, idArea,
                    request.getParameter("tipoCaixa"), request.getParameter("descricao"), valor);
            Map<String, Object> extras = new HashMap<>();
            extras.put("id", id);
            extras.put("registro", despesasCustosDAO.read(id));
            writeJson(response, HttpServletResponse.SC_OK, true,
                    existia ? "Preço atualizado com sucesso!" : "Preço registrado com sucesso!", target, extras);
        } catch (IllegalArgumentException e) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, e.getMessage(), target);
        } catch (SQLException e) {
            String cod = Util.LogUtil.erro(DespesasCustosServlet.class, "Falha ao registrar preço de caixa.", e);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false, "Erro ao registrar preço: " + cod, target);
        }
    }

    /** "3,50" / "1.234,56" (BRL) ou "3.50" (ponto decimal). Inválido → IllegalArgumentException. */
    private java.math.BigDecimal parseValor(String s) {
        if (isBlank(s)) throw new IllegalArgumentException("Informe o valor.");
        String t = s.trim().replace("R$", "").trim();
        if (t.contains(",")) t = t.replace(".", "").replace(",", ".");
        try {
            return new java.math.BigDecimal(t);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Formato inválido de valor.");
        }
    }

    private Integer parseIntOuNull(String s) {
        if (isBlank(s)) return null;
        try {
            return Integer.valueOf(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /* Util */
    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
