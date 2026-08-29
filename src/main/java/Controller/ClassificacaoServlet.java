package Controller;

import Model.Dao.ClassificacaoDAO;
import Model.Model.Classificacao;
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

public class ClassificacaoServlet extends HttpServlet {

    private ClassificacaoDAO classificacaoDAO;
    private final Gson gson = new Gson();

    @Override
    public void init() {
        classificacaoDAO = new ClassificacaoDAO();
    }

    /* ============================================================
       Helper: resposta JSON padronizada
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

    /* ============================================================
       GET: lista (DataTables consome array). Em erro, {ok:false,...}
       ============================================================ */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        try {
            List<Classificacao> classificacoes = classificacaoDAO.listAll();
            try (PrintWriter out = response.getWriter()) {
                out.print(gson.toJson(classificacoes));
                out.flush();
            }
        } catch (SQLException e) {
            // P1-15/P2-20: pilha completa no log; ao usuário vai só o código.
            String cod = Util.LogUtil.erro(ClassificacaoServlet.class, "Falha tratada em ClassificacaoServlet.", e);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Erro ao carregar classificações: " + cod, "page");
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
            String classificacaoNome = request.getParameter("classificacao");

            if (isBlank(classificacaoNome)) {
                writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false,
                        "Todos os campos são obrigatórios.", "modalClassificacaoCadastro");
                return;
            }
            
              if (classificacaoDAO.VerificacaoClassificacao(classificacaoNome)) {
                writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, "Classificação já cadastrada.", "modalClassificacaoCadastro");
                return;
            }


            Classificacao classificacao = new Classificacao();
            classificacao.setClassificacao(classificacaoNome);

            classificacaoDAO.saveClassificacao(classificacao);

            writeJson(response, HttpServletResponse.SC_OK, true,
                    "Classificação cadastrada com sucesso!", "page");
        } catch (Exception e) {
            // P1-15/P2-20: pilha completa no log; ao usuário vai só o código.
            String cod = Util.LogUtil.erro(ClassificacaoServlet.class, "Falha tratada em ClassificacaoServlet.", e);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Erro ao cadastrar: " + cod, "modalClassificacaoCadastro");
        }
    }

    /* ----------------------------- UPDATE ----------------------------- */
private void handleUpdate(HttpServletRequest request, HttpServletResponse response) throws IOException {
    try {
        String idParam = request.getParameter("id");
        String classificacaoNome = request.getParameter("classificacao");

        if (isBlank(idParam) || isBlank(classificacaoNome)) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false,
                    "ID e classificação são obrigatórios para atualizar.", "modalClassificacaoAtualizar");
            return;
        }

        int id = Integer.parseInt(idParam);

        // 1 - verificar se é o mesmo valor já salvo
        if (classificacaoDAO.DadoAtualClassificacao(id, classificacaoNome)) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false,
                    "O dado digitado já está gravado nesse registro. Não é permitido atualizar com o mesmo valor.", "modalClassificacaoAtualizar");
            return;
        }

        // 2 - verificar se existe em outro registro
        if (classificacaoDAO.existeEmOutroRegistroClassificacao(id, classificacaoNome)) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false,
                    "Já existe outra classificação com esse valor. Insira um valor diferente.", "modalClassificacaoAtualizar");
            return;
        }

        Classificacao classificacao = new Classificacao();
        classificacao.setIdclassificacao(id);
        classificacao.setClassificacao(classificacaoNome);

        classificacaoDAO.updateClassificacao(classificacao);

        writeJson(response, HttpServletResponse.SC_OK, true,
                "Classificação atualizada com sucesso!", "page");

    } catch (Exception e) {
            // P1-15/P2-20: pilha completa no log; ao usuário vai só o código.
            String cod = Util.LogUtil.erro(ClassificacaoServlet.class, "Falha tratada em ClassificacaoServlet.", e);
        writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                "Erro ao atualizar: " + cod, "modalClassificacaoAtualizar");
    }
}

    /* ----------------------------- DELETE ----------------------------- */
    private void handleDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String idParam = request.getParameter("id");

            if (isBlank(idParam)) {
                writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false,
                        "ID é obrigatório para excluir.", "modalClassificacaoExcluir");
                return;
            }

            int id = Integer.parseInt(idParam);
            
              if (classificacaoDAO.VerificacaoClassificacaoAlimento(id)) {
                writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, "Informação não pode ser excluida.", "modalClassificacaoExcluir");
                return;
            }
            Classificacao classificacao = new Classificacao();
            classificacao.setIdclassificacao(id);

            classificacaoDAO.deleteClassificacao(classificacao);

            writeJson(response, HttpServletResponse.SC_OK, true,
                    "Excluída com sucesso!", "page");
        } catch (Exception e) {
            // P1-15/P2-20: pilha completa no log; ao usuário vai só o código.
            String cod = Util.LogUtil.erro(ClassificacaoServlet.class, "Falha tratada em ClassificacaoServlet.", e);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Erro ao excluir: " + cod, "modalClassificacaoExcluir");
        }
    }

    /* ============================================================
       Util
       ============================================================ */
    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
