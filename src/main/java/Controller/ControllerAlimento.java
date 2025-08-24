package Controller;


import Model.Dao.AlimentoClassificacaoDao;
import Model.Dao.AlimentoDAO;
import Model.Model.Alimento;
import Model.Model.AlimentoClassificacao;
import Model.Model.Classificacao;
import com.google.gson.Gson;
import com.google.gson.JsonObject;



import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class ControllerAlimento extends HttpServlet {
    private static final long serialVersionUID = 1L;
    
    private AlimentoDAO alimentodao;
    private AlimentoClassificacaoDao alimentoclassificacaodao;

    String Caminho = "view/admin/";
    String Mensagem = "";
    String Atributo = "";

    public void init() {
        alimentodao = new AlimentoDAO();
        alimentoclassificacaodao = new AlimentoClassificacaoDao();
    }

    public ControllerAlimento() {
        super();
    }

    @SuppressWarnings("unchecked")
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Lendo o corpo da requisição JSON
        StringBuilder json = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String linha;
            while ((linha = reader.readLine()) != null) {
                json.append(linha);
            }
        }

        // Convertendo o JSON em um objeto
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(json.toString(), JsonObject.class);

        // Extraindo a ação do JSON
        String acao = jsonObject.get("acao").getAsString();

        if ("create".equals(acao)) {
            try {
                // Extraindo os demais parâmetros do JSON
                String alimento = jsonObject.get("alimento").getAsString();
                String variedade = jsonObject.get("variedade").getAsString();
                String tipo = jsonObject.get("tipo").getAsString();
                List<String> classificacoes = gson.fromJson(jsonObject.get("classificacoes"), List.class);

                // Logs para depuração
                System.out.println("Ação: " + acao);
                System.out.println("Alimento: " + alimento);
                System.out.println("Variedade: " + variedade);
                System.out.println("Classificações 2: " + classificacoes);
                System.out.println("tipo: " + tipo);
                
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
                // Cadastro produto
              
                
               
         
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
    }
}
