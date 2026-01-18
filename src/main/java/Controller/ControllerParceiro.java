
package Controller;


import Model.Dao.ParceiroDAO;
import Model.Model.Parceiro;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 *
 * @author lucia
 */
public class ControllerParceiro extends HttpServlet {

  private static final long serialVersionUID = 1L;
  private ParceiroDAO parceiroDao;
   private final Gson gson = new Gson();
  
   public void init() {
        parceiroDao = new ParceiroDAO();
    }
   
       /* ============================================================
       Helper: resposta JSON padronizada
       ============================================================ */
    private void writeJson(HttpServletResponse resp, int status, 
            boolean ok, String msg, String target) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());

        Map<String, Object> payload = new HashMap<>();
        payload.put("ok", ok);
        payload.put("msg", msg);
        if (target != null) {
            payload.put("target", target);
        }

        resp.setStatus(status);
        try (PrintWriter out = resp.getWriter()) {
            out.print(gson.toJson(payload));
            out.flush();
        }
    }
   @Override
protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {

    response.setContentType("application/json");
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());

    String acao = request.getParameter("acao");
    String idParam = request.getParameter("id");
    System.out.println("estando variavel"+ idParam);
    String verificarCnpj = request.getParameter("cnpj");

    try (PrintWriter out = response.getWriter()) {

        // 1️⃣ RESUMO (dashboard)
        if ("dados".equalsIgnoreCase(acao)) {
            handleDados(null, response);
            return;
        }

        // 2️⃣ VERIFICAÇÃO DE CNPJ
        if (verificarCnpj != null && !verificarCnpj.isEmpty()) {
            boolean existe = parceiroDao.existeCNPJ(verificarCnpj);
            Map<String, Object> resultado = new HashMap<>();
            resultado.put("existe", existe);
            out.print(gson.toJson(resultado));
            return;
        }

        // 3️⃣ BUSCA POR ID (EDIÇÃO)
        if (idParam != null && !idParam.isEmpty()) {
            int id = Integer.parseInt(idParam);
            Parceiro parceiro = parceiroDao.getParceiroById(id);

            if (parceiro != null) {
                out.print(gson.toJson(parceiro));
            } else {
                writeJson(response, HttpServletResponse.SC_NOT_FOUND,
                        false, "Parceiro não encontrado.", null);
            }
            return;
        }

        // 4️⃣ LISTAGEM
        List<Parceiro> lista = parceiroDao.listAllParceiro();
        out.print(gson.toJson(lista));

    } catch (Exception e) {
        e.printStackTrace();
        writeJson(response,
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                false,
                "Erro ao carregar parceiros",
                null);
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
System.out.println("acao:"+ acao);
        switch (acao.toLowerCase()) {
            case "create":
                handleCreate(jsonObject, response);
                break;
            /* case "dados":
                handleDados(null, response);
                break;*/
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
         String cnpj=  jsonObject.get("cnpj").getAsString();
         
         // Verificação  existencia de  cnpj
             if (parceiroDao.existeCNPJ(cnpj)) {
                writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false,
                        "CNPJ já cadastrado.", "#alerta");
                return;
            }
         
        Parceiro parceiro = new Parceiro(
            0,
            jsonObject.get("nome").getAsString(),
            jsonObject.has("usuario") ? jsonObject.get("usuario").getAsString() : null,
            jsonObject.has("senha") ? jsonObject.get("senha").getAsString() : null,
            jsonObject.get("nivel").getAsString(),
            true, // ou use jsonObject.get("situacao").getAsBoolean() se enviar no JSON
            jsonObject.get("email").getAsString(),
            jsonObject.get("telefone").getAsString(), // telefonePessoa (adicione se enviar no JSON)
            jsonObject.get("cep").getAsString(),
            jsonObject.get("numero").getAsInt(),
            jsonObject.get("complemento").getAsString(),
            cnpj,
            jsonObject.get("razaosocial").getAsString(),
            jsonObject.get("inscricaoestadual").getAsString(),
            jsonObject.get("site").getAsString()
        );

        parceiroDao.addParceiro(parceiro);

        writeJson(response, HttpServletResponse.SC_OK, true, "Parceiro cadastrado com sucesso!", null);
    } catch (Exception e) {
        e.printStackTrace();
        System.out.println(" Erro gerencial do sistema" + e.getMessage());
        writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false, "Erro no cadastro",null);
    }
}
   /********************************** Update**************************************/
   private void handleUpdate(JsonObject jsonObject, HttpServletResponse response) throws IOException {
    try {
         String cnpj=  jsonObject.get("cnpj").getAsString();
        
         
        Parceiro parceiro = new Parceiro(
            jsonObject.get("idPessoa").getAsInt(),
            jsonObject.get("nome").getAsString(),
            jsonObject.has("usuario") ? jsonObject.get("usuario").getAsString() : null,
            jsonObject.has("senha") ? jsonObject.get("senha").getAsString() : null,
            jsonObject.get("nivel").getAsString(),
            jsonObject.get("situacao").getAsBoolean(), // ou use jsonObject.get("situacao").getAsBoolean() se enviar no JSON
            jsonObject.get("email").getAsString(),
            jsonObject.get("telefone").getAsString(), // telefonePessoa (adicione se enviar no JSON)
            jsonObject.get("cep").getAsString(),
            jsonObject.get("numero").getAsInt(),
            jsonObject.get("complemento").getAsString(),
            cnpj,
            jsonObject.get("razaosocial").getAsString(),
            jsonObject.get("inscricaoestadual").getAsString(),
            jsonObject.get("site").getAsString()
        );

        parceiroDao.updateParceiro(parceiro);

        writeJson(response, HttpServletResponse.SC_OK, true, "Parceiro atualizado com sucesso!", null);
    } catch (Exception e) {
        e.printStackTrace();
        System.out.println(" Erro gerencial do sistema" + e.getMessage());
        writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false, "Erro no cadastro",null);
    }
}
   /* -----------------------------  Desativar ----------------------------- */
   private void handleDelete(JsonObject jsonObject, HttpServletResponse response) throws IOException {
    try {
         int Idpessoa =  Integer.parseInt(jsonObject.get("idPessoa").getAsString());
         Boolean situacao = Boolean.valueOf(jsonObject.get("situacao").getAsString());
         
         // Analisando situacao
          System.out.println("id"+ Idpessoa +" situacao:" +situacao);
           situacao = !situacao;
         
        parceiroDao.deleteParceiro(Idpessoa, situacao);

        writeJson(response, HttpServletResponse.SC_OK, true, "Parceiro Desativado com sucesso!", null);
    } catch (Exception e) {
        e.printStackTrace();
        System.out.println(" Erro gerencial do sistema" + e.getMessage());
        writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false, "Erro no cadastro",null);
    }
}
      /* ----------------------------- Dados ----------------------------- */
  private void handleDados(JsonObject jsonObject, HttpServletResponse response) throws IOException {
    response.setContentType("application/json");
    PrintWriter out = response.getWriter();

    try {
        int total = parceiroDao.contarTodos();
        int ativos = parceiroDao.contarPorSituacao(true);
        int inativos = parceiroDao.contarPorSituacao(false);

        // Criando JSON de resposta
        JsonObject resultado = new JsonObject();
        resultado.addProperty("total", total);
        resultado.addProperty("ativos", ativos);
        resultado.addProperty("inativos", inativos);

        out.print(resultado.toString());

    } catch (Exception e) {
        e.printStackTrace();
        System.err.println("Erro ao obter dados de parceiros: " + e.getMessage());

        // Erro formatado
        JsonObject erro = new JsonObject();
        erro.addProperty("sucesso", false);
        erro.addProperty("msg", "Erro ao obter dados do sistema.");

        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        out.print(erro.toString());
    } finally {
        out.flush();
        out.close();
    }
}

    
}
