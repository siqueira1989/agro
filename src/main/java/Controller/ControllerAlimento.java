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
import java.util.ArrayList;
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
        if (target != null) {
            payload.put("target", target);
        }

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
            case "atualizacaoalimentoclassificacaomodal":
                handleAtualizacaoAlimentoClassificacaoModal(jsonObject, response);
                break;
            case "update":
                handleUpdate(jsonObject, response);
                break;
            case "atualizacaoalimentoclassificacao":
                handleAtualizacaoAlimentoClassificacao(jsonObject, response);
                break;
            case "salvarclassificacoesalimentomodal":
             handleSalvarClassificacoesAlimentoModal(jsonObject, response);
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

            Alimento alimentoObj = new Alimento();
            alimentoObj.setNomeproduto(alimento);
            alimentoObj.setTipoproduto(tipo);
            alimentoObj.setVariedadealimento(variedade);
            alimentoObj.setSituacaoproduto(true);

            if (alimentodao.VerificarDadosAlimento(alimentoObj)) {
                writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, "Alimento ja cadastrado.", "ModalCadastroAlimento");
                return;
            }

            // Antes: lia last_value da sequencia e assumia que o proximo id seria
            // "numero + 1" (havia ate um comentario "cuidado aqui"). Dois cadastros
            // simultaneos calculavam o mesmo numero e as classificacoes de um iam
            // parar no alimento do outro; e qualquer buraco na sequencia apontava
            // para um id inexistente. Agora o INSERT devolve o id real com
            // RETURNING, e alimento + classificacoes entram na mesma transacao.
            java.util.List<Integer> idsClassificacao = new java.util.ArrayList<>();
            for (String classificacaoId : classificacoes) {
                try {
                    idsClassificacao.add(Integer.valueOf(classificacaoId.trim()));
                } catch (NumberFormatException ex) {
                    writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false,
                            "Classificação inválida informada.", "ModalCadastroAlimento");
                    return;
                }
            }

            alimentoclassificacaodao.cadastrarComClassificacoes(alimentoObj, idsClassificacao);

            writeJson(response, HttpServletResponse.SC_OK, true, "Alimento cadastrado com sucesso!", "page");
        } catch (Exception e) {
            // P1-15: o detalhe tecnico (nome de tabela, coluna, constraint) fica so
            // no log; o usuario recebe um codigo para citar ao suporte.
            String cod = Util.LogUtil.erro(ControllerAlimento.class, "Falha ao cadastrar alimento.", e);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    Util.LogUtil.mensagemUsuario("cadastrar o alimento", cod), "ModalCadastroAlimento");
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
                        "ID e campos obrigatórios para atualizar.", "page");
                return;
            }
            
            /*Instanciando novo objeto*/
            
             Alimento alimentoObj = new Alimento();
             
             /*Atribuindo objeto*/
             
                alimentoObj.setIdproduto(Integer.parseInt(id));
                alimentoObj.setNomeproduto(alimento);
                alimentoObj.setVariedadealimento(variedade);
                System.err.println("paseei controle");
                alimentodao.updateAlimento(alimentoObj);
             
            // Atualização do alimento pode ser feita aqui se for implementada
            writeJson(response, HttpServletResponse.SC_OK, true,
                    "Atualizado com sucesso!", "page");
        } catch (Exception e) {
            // P1-15/P2-20: pilha completa no log; ao usuário vai só o código.
            String cod = Util.LogUtil.erro(ControllerAlimento.class, "Falha tratada em ControllerAlimento.", e);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Erro ao atualizar: " + cod, "page");
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
            // P1-15: detalhe tecnico so no log.
            String cod = Util.LogUtil.erro(ControllerAlimento.class, "Falha ao excluir classificação.", e);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    Util.LogUtil.mensagemUsuario("excluir a classificação", cod), "modalClassificacaoExcluir");
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
            String id = request.getParameter("id");
            if (id != null && !id.isEmpty()) {
                int idalimento = Integer.parseInt(id);
                Alimento alimento = alimentodao.AlimentoBuscaID(idalimento);
                if (alimento != null) {

                    out.print(gson.toJson(alimento));
                } else {
                    writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false,
                            "Informação não encontrada.", "page");
                }
            } else {
                List<Alimento> alimentos = alimentodao.listAll();
                out.print(gson.toJson(alimentos));
                out.flush();
            }
        } catch (SQLException e) {
            // P1-15/P2-20: pilha completa no log; ao usuário vai só o código.
            String cod = Util.LogUtil.erro(ControllerAlimento.class, "Falha tratada em ControllerAlimento.", e);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Erro ao carregar alimentos: " + cod, "page");
        }
    }

    /* ----------------------------- DELETE ----------------------------- */
    private void handleAtualizacaoAlimentoClassificacao(JsonObject jsonObject, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        try (PrintWriter out = response.getWriter()) {
            String id = jsonObject.get("idproduto").getAsString();

            if (id == null || id.isEmpty()) {
                writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false,
                        "ID não informado.", "page");
                return;
            }

            int idproduto = Integer.parseInt(id);
            System.out.println("Buscando classificações para alimento ID: " + idproduto);

            List<AlimentoClassificacao> lista = alimentoclassificacaodao.ClassificacaoAlimentoBuscaID(idproduto);

            if (lista != null && !lista.isEmpty()) {
                out.print(gson.toJson(lista));
                out.flush();
            } else {
                writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false,
                        "Nenhuma classificação encontrada para o alimento.", "page");
            }

        } catch (SQLException e) {
            // P1-15/P2-20: pilha completa no log; ao usuário vai só o código.
            String cod = Util.LogUtil.erro(ControllerAlimento.class, "Falha tratada em ControllerAlimento.", e);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Erro ao carregar classificação do alimento: " + cod, "page");
        }
    }



/*------------------------------------atualizacaoalimentoclassificacaomodal-----------------------------*/
    

    private void handleAtualizacaoAlimentoClassificacaoModal(JsonObject jsonObject, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        try (PrintWriter out = response.getWriter()) {
            String id = jsonObject.get("idproduto").getAsString();

            if (id == null || id.isEmpty()) {
                writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false,
                        "ID não informado.", "page");
                return;
            }

            int idproduto = Integer.parseInt(id);
            System.out.println("Buscando classificações para alimento ID: " + idproduto);

            List<AlimentoClassificacao> lista = alimentoclassificacaodao.ClassificacaoAlimentoBuscaModalID(idproduto);
            System.out.println("teste"+lista);
            if (lista != null && !lista.isEmpty()) {
                out.print(gson.toJson(lista));
                out.flush();
            } else {
                writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false,
                        "Nenhuma classificação encontrada para o alimento.", "page");
            }

        } catch (SQLException e) {
            // P1-15/P2-20: pilha completa no log; ao usuário vai só o código.
            String cod = Util.LogUtil.erro(ControllerAlimento.class, "Falha tratada em ControllerAlimento.", e);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Erro ao carregar classificação do alimento: " + cod, "page");
        }
    }
    
    /*Cadastrar novos classificação no pagina atualização*/
    
    private void handleSalvarClassificacoesAlimentoModal(JsonObject jsonObject, HttpServletResponse response) throws IOException {
    response.setContentType("application/json");
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());

    try {
        int idproduto = jsonObject.get("idproduto").getAsInt();
         List<String> classificacoes = gson.fromJson(jsonObject.get("classificacoes"), List.class);

        

        if (classificacoes.isEmpty()) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, "Nenhuma classificação selecionada.", "page");
            return;
        }

        // Grava as classificações no banco
        for (String idClassificacao : classificacoes) {
            
            
            
            AlimentoClassificacao ac = new AlimentoClassificacao();

            ac.setAlimento(new Alimento());
            ac.getAlimento().setIdproduto(idproduto);

            ac.setClassificacao(new Classificacao());
            ac.getClassificacao().setIdclassificacao(Integer.parseInt(idClassificacao));
            
            if(alimentoclassificacaodao.ExisteAssociacaoClassificacao(ac)){
                   writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, "Dados já cadastrados!", "#alerta");
            return;
            }
            
            alimentoclassificacaodao.addAlimentoClassificacao(ac);
        }

        writeJson(response, HttpServletResponse.SC_OK, true, "Classificações associadas com sucesso!", "page");

    } catch (Exception e) {
            // P1-15/P2-20: pilha completa no log; ao usuário vai só o código.
            String cod = Util.LogUtil.erro(ControllerAlimento.class, "Falha tratada em ControllerAlimento.", e);
        writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                "Erro ao associar classificações: " + cod, "page");
    }
}


    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private boolean isBlank(List<String> list) {
        return list == null || list.isEmpty();
    }
}
