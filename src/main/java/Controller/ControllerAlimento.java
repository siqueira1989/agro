package Controller;

import Model.Dao.AlimentoClassificacaoDao;
import Model.Dao.AlimentoDAO;
import Model.Dao.ClassificacaoDAO;
import Model.Model.Alimento;
import Model.Model.AlimentoClassificacao;
import Model.Model.Classificacao;
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

public class ControllerAlimento extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private AlimentoDAO alimentodao;
    private AlimentoClassificacaoDao alimentoclassificacaodao;
    private ClassificacaoDAO classificacaoDAO;
    private final Gson gson = new Gson();

    public void init() {
        alimentodao = new AlimentoDAO();
        alimentoclassificacaodao = new AlimentoClassificacaoDao();
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
       POST: create | update | delete com JSON padronizado
       ============================================================ */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());

        StringBuilder json = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String linha;
            while ((linha = reader.readLine()) != null) {
                json.append(linha);
            }
        }

        JsonObject jsonObject = gson.fromJson(json.toString(), JsonObject.class);
        String acao = jsonObject.get("acao").getAsString();

        switch (acao.toLowerCase()) {
            case "create":
                handleCreate(jsonObject, response);
                break;
            case "update":
                handleUpdate(jsonObject, response);
                break;
            case "delete":
                handleDelete(jsonObject, response);
                break;
            default:
                writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, "Ação inválida.", "page");
        }
    }

    /* ----------------------------- CREATE ----------------------------- */
    private void handleCreate(JsonObject jsonObject, HttpServletResponse response) throws IOException {
        try {
            String alimento = jsonObject.get("alimento").getAsString();
            String variedade = jsonObject.get("variedade").getAsString();
            String tipo = jsonObject.get("tipo").getAsString();
            List<String> classificacoes = gson.fromJson(jsonObject.get("classificacoes"), List.class);

            if (isBlank(alimento) || isBlank(variedade) || isBlank(tipo) || isBlank(classificacoes)) {
                writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false,
                        "Todos os campos são obrigatórios para cadastrar.", "modalClassificacaoCadastro");
                return;
            }

            // Gera o próximo ID
            int numero = alimentoclassificacaodao.RetornoIdAlimento();
            System.out.println("Número gerado: " + numero);

            // Cadastra o alimento
            Alimento alimentoObj = new Alimento();
            alimentoObj.setNomeproduto(alimento);
            alimentoObj.setTipoproduto(tipo);
            alimentoObj.setVariedadealimento(variedade);
            alimentoObj.setSituacaoproduto(true);
               if (alimentodao.VerificarDadosAlimento(alimentoObj)) {
                writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, "Alimento ja cadastrado.", "ModalCadastroAlimento");
                return;
            }
            
            alimentodao.addAlimento(alimentoObj);

            // Associa as classificações ao alimento
            for (String classificacaoId : classificacoes) {
                AlimentoClassificacao alimentoClassificacao = new AlimentoClassificacao();
                alimentoClassificacao.setClassificacao(new Classificacao());
                alimentoClassificacao.getClassificacao().setIdclassificacao(Integer.parseInt(classificacaoId));

                alimentoClassificacao.setAlimento(new Alimento());
                alimentoClassificacao.getAlimento().setIdproduto(numero + 1); // cuidado aqui

                alimentoclassificacaodao.addAlimentoClassificacao(alimentoClassificacao);
            }

            writeJson(response, HttpServletResponse.SC_OK, true, "Alimento cadastrado com sucesso!", "page");
        } catch (Exception e) {
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Erro ao cadastrar: " + e.getMessage(), "ModalCadastroAlimento");
        }
    }

    /* ----------------------------- UPDATE ----------------------------- */
    private void handleUpdate(JsonObject jsonObject, HttpServletResponse response) throws IOException {
        try {
            String id = jsonObject.get("idproduto").getAsString();
            String alimento = jsonObject.get("alimento").getAsString();
            String variedade = jsonObject.get("variedade").getAsString();

            if (isBlank(id) || isBlank(alimento) || isBlank(variedade)) {
                writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false,
                        "ID e campos obrigatórios para atualizar.", "modalClassificacaoAtualizar");
                return;
            }

            // Atualização do alimento pode ser feita aqui se for implementada
            writeJson(response, HttpServletResponse.SC_OK, true,
                    "Atualizado com sucesso!", "page");
        } catch (Exception e) {
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Erro ao atualizar: " + e.getMessage(), "modalClassificacaoAtualizar");
        }
    }

    /* ----------------------------- DELETE ----------------------------- */
    private void handleDelete(JsonObject jsonObject, HttpServletResponse response) throws IOException {
        try {
            String idParam = jsonObject.get("id").getAsString();

            if (isBlank(idParam)) {
                writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false,
                        "ID é obrigatório para excluir.", "modalClassificacaoExcluir");
                return;
            }

            int id = Integer.parseInt(idParam);

            if (classificacaoDAO.VerificacaoClassificacaoAlimento(id)) {
                writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false,
                        "Informação não pode ser excluída.", "modalClassificacaoExcluir");
                return;
            }

            Classificacao classificacao = new Classificacao();
            classificacao.setIdclassificacao(id);
            classificacaoDAO.deleteClassificacao(classificacao);

            writeJson(response, HttpServletResponse.SC_OK, true,
                    "Excluída com sucesso!", "page");
        } catch (IOException | NumberFormatException | SQLException e) {
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Erro ao excluir: " + e.getMessage(), "modalClassificacaoExcluir");
        }
    }

    /* ----------------------------- GET: Lista ----------------------------- */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
         PrintWriter out = response.getWriter();
        try {
            String id =request.getParameter("id");
            if (id != null && !id.isEmpty()){
                 int idalimento = Integer.parseInt(id);
                Alimento alimento = alimentodao.AlimentoBuscaID(idalimento);
                if (alimento != null) {
                    System.out.println("alimento" + alimento);
                    out.print(gson.toJson(alimento));
                } else {
                    writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false,
                        "Informação não encontrada.", "page");
                }
            }else{
            List<Alimento> alimentos = alimentodao.listAll();
             out.print(gson.toJson(alimentos));
                out.flush();
            }
        } catch (SQLException e) {
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Erro ao carregar alimentos: " + e.getMessage(), "page");
        }
    }
    


    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private boolean isBlank(List<String> list) {
        return list == null || list.isEmpty();
    }
}
