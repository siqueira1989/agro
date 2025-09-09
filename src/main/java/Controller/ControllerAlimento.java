package Controller;


import Model.Dao.AlimentoClassificacaoDao;
import Model.Dao.AlimentoDAO;
import Model.Dao.ClassificacaoDAO;
import Model.Model.Alimento;
import Model.Model.AlimentoClassificacao;
import Model.Model.Classificacao;
import com.google.gson.Gson;
import com.google.gson.JsonObject;



import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import static jakarta.ws.rs.client.Entity.json;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.HashMap;
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

    public ControllerAlimento() {
        super();
    }/*

    @SuppressWarnings("unchecked")
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Lendo o corpo da requisição JSON
       

        if ("create".equals(acao)) {
            try {
              
                // Resposta de sucesso
                response.setContentType("application/json");
                response.getWriter().write("{\"message\": \"Alimento cadastrado com sucesso!\"}");
            } catch (Exception e) {
                // Tratamento de erros
                e.printStackTrace();
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("{\"message\": \"Erro ao processar a solicitação\"}");
            }
        } else {
            // Resposta para ações inválidas
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"message\": \"Ação não suportada!\"}");
        }
    }*/
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
            List<Alimento> alimentos = alimentodao.listAll();
            try (PrintWriter out = response.getWriter()) {
                out.print(gson.toJson(alimentos));
                out.flush();
            }
        } catch (SQLException e) {
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Erro ao carregar classificações: " + e.getMessage(), "page");
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

        // Convertendo o JSON em um objeto
    
        JsonObject jsonObject = gson.fromJson(json.toString(), JsonObject.class);

        // Extraindo a ação do JSON
        String acao = jsonObject.get("acao").getAsString();
       
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
            StringBuilder json = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String linha;
            while ((linha = reader.readLine()) != null) {
                json.append(linha);
            }
        }
             JsonObject jsonObject = gson.fromJson(json.toString(), JsonObject.class);
             // Extraindo os demais parâmetros do JSON
                String alimento = jsonObject.get("alimento").getAsString();
                String variedade = jsonObject.get("variedade").getAsString();
                String tipo = jsonObject.get("tipo").getAsString();
                List<String> classificacoes = gson.fromJson(jsonObject.get("classificacoes"), List.class);

               if (isBlank(alimento) || isBlank(variedade)|| isBlank(tipo) || isBlank(classificacoes)) {
                writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false,
                        "ID e classificação são obrigatórios para atualizar.", "modalClassificacaoAtualizar");
                return;
            }
                
                // Obtém o número antes do loop
                int numero = alimentoclassificacaodao.RetornoIdAlimento();
               System.out.println("Número gerado: " + numero);
 Alimento alimentoObj= new Alimento();
                alimentoObj.setNomeproduto(alimento);
                alimentoObj.setTipoproduto(tipo);
                alimentoObj.setVariedadealimento(variedade);
                alimentoObj.setSituacaoproduto(true);
                 alimentodao.addAlimento(alimentoObj);
              
                // Loop para cadastrar cada classificação no banco de dados
               for (String classificacaoId : classificacoes) {
                    AlimentoClassificacao alimentoClassificacao = new AlimentoClassificacao();
                    // Instanciando  dentro da  setClassificacao
                     alimentoClassificacao.setClassificacao(new Classificacao());
                    alimentoClassificacao.getClassificacao().setIdclassificacao(Integer.parseInt(classificacaoId));
                   alimentoClassificacao.setAlimento(new Alimento());
                    alimentoClassificacao.getAlimento().setIdproduto(numero+1); // Associando ao alimento gerado
                    // Inserindo no banco de dados
                  alimentoclassificacaodao.addAlimentoClassificacao(alimentoClassificacao);
                }
                 writeJson(response, HttpServletResponse.SC_OK, true,
                    "Alimento cadastrado com sucesso!", "page");
        } catch (Exception e) {
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Erro ao cadastrar: " + e.getMessage(), "modalClassificacaoCadastro");
        }
    }

    /* ----------------------------- UPDATE ----------------------------- */
    private void handleUpdate(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
              StringBuilder json = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String linha;
            while ((linha = reader.readLine()) != null) {
                json.append(linha);
            }
        }
             JsonObject jsonObject = gson.fromJson(json.toString(), JsonObject.class);
             // Extraindo os demais parâmetros do JSON
              String id = jsonObject.get("idproduto").getAsString();
                String alimento = jsonObject.get("alimento").getAsString();
                String variedade = jsonObject.get("variedade").getAsString();

            if (isBlank(alimento) || isBlank(variedade)|| isBlank(id)) {
                writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false,
                        "ID e classificação são obrigatórios para atualizar.", "modalClassificacaoAtualizar");
                return;
            }

           // int id = Integer.parseInt(idParam);

           /* Classificacao classificacao = new Classificacao();
            classificacao.setIdclassificacao(id);
            classificacao.setClassificacao(classificacaoNome);*/

           // classificacaoDAO.updateClassificacao(classificacao);

            writeJson(response, HttpServletResponse.SC_OK, true,
                    "Classificação atualizada com sucesso!", "page");
        } catch (Exception e) {
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Erro ao atualizar: " + e.getMessage(), "modalClassificacaoAtualizar");
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
        } catch (IOException | NumberFormatException | SQLException e) {
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Erro ao excluir: " + e.getMessage(), "modalClassificacaoExcluir");
        }
    }

    /* ============================================================
       Util
       ============================================================ */
    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
  
    private boolean isBlank(List<String> classificacoes) {
       return classificacoes == null || classificacoes.isEmpty();
    }
}
 

