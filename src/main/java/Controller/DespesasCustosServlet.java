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
        try {
            List<DespesasCustos> despesasCustosList = despesasCustosDAO.listAll();
            try (PrintWriter out = response.getWriter()) {
                out.print(gson.toJson(despesasCustosList));
                out.flush();
            }
        } catch (SQLException e) {
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false, "Erro ao listar: " + e.getMessage(), "page");
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

            // Regras simples de validação (opcional, mas útil):
            if (isBlank(despesascusto) || isBlank(unidadedespesacusto) || isBlank(valordespesacusto) || isBlank(tipodespesacusto)) {
                writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, "Todos os campos são obrigatórios.", "modalCadastro");
                return;
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

            despesasCustosDAO.create(bean);

            writeJson(response, HttpServletResponse.SC_OK, true, "Despesa cadastrada com sucesso!", "page");
        } catch (Exception e) {
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false, "Erro ao cadastrar: " + e.getMessage(), "modalCadastro");
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

            if (isBlank(iddespesascustos) || isBlank(despesascustos) || isBlank(unidadedespesacustos) ||
                isBlank(valordespesacustos) || isBlank(tipodespesacustos)) {
                writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, "Todos os campos são obrigatórios.", "modalAtualizacao");
                return;
            }

            double valor = parseBRL(valordespesacustos);

            DespesasCustos bean = new DespesasCustos();
            bean.setIddespesascusto(Integer.parseInt(iddespesascustos));
            bean.setDespesascusto(despesascustos);
            bean.setUnidadedespesascustos(unidadedespesacustos);
            bean.setValordespesascustos(valor);
            bean.setTipodespesascustos(tipodespesacustos);
             if (despesasCustosDAO.VerificarDadosUpdate(bean)) {
                 System.out.println("ok");
                writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, "Despesa/Custo está igual ao cadastro anterior.", "modalAtualizacao");
                return;
            }
              if (despesasCustosDAO.existeDespesaCusto(despesascustos)) {
                 System.out.println("ok");
                writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, "Despesa/Custo está igual ao cadastro  de outro registro.", "modalAtualizacao");
                return;
            }
            despesasCustosDAO.update(bean);

            writeJson(response, HttpServletResponse.SC_OK, true, "Despesa atualizada com sucesso!", "page");
        } catch (Exception e) {
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false, "Erro ao atualizar: " + e.getMessage(), "modalAtualizacao");
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
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false, "Erro ao excluir: " + e.getMessage(), "page");
        }
    }

    /* Util */
    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
