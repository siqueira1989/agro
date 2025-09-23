/*
package Controller;

import Model.Dao.EnderecoDAO;
import Model.Dao.ParceiroDAO;
import Model.Model.Endereco;
import Model.Model.Parceiro;
import com.google.gson.Gson;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.List;


public class ControllerParceiro extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	private EnderecoDAO enderecoDAO;
	private ParceiroDAO parceiroDao;
    String Caminho= "view/admin/";
    String Mensagem ="";
	String Atributo = "";


	public void init() {
		 enderecoDAO = new EnderecoDAO();
		 parceiroDao = new ParceiroDAO();
	}
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	    response.setContentType("application/json");
	    response.setCharacterEncoding("UTF-8");

	    // Verifica se foi passado um ID para buscar os detalhes de um parceiro
	   
	    String idParceiro = request.getParameter("id");
	    PrintWriter out = response.getWriter();
	    
	    try {
	        if (idParceiro != null && !idParceiro.isEmpty()) {
	           
	        	// Busca o parceiro pelo ID
	            
	        	int id = Integer.parseInt(idParceiro);
	            Parceiro parceiro = parceiroDao.getParceiroById(id);

	            if (parceiro != null) {
	               
	            	// Retorna os dados do parceiro como JSON
	                
	            	String json = new Gson().toJson(parceiro);
	            	out.print(json);
	            	
	            } else {
	                
	            	// Retorna uma mensagem de erro se o parceiro não for encontrado
	               
	            	response.setStatus(HttpServletResponse.SC_NOT_FOUND);
	                out.print("{\"message\":\"Parceiro não encontrado\"}");
	            }
	        } else {
	            // Caso não tenha sido passado um ID, retorna a lista de todos os parceiros
	            List<Parceiro> parceiroList = parceiroDao.listAllParceiro();
	            String json = new Gson().toJson(parceiroList);
	            out.print(json);
	        }
	    } catch (Exception e) {
	    	
	        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
	        out.print("{\"message\":\"Erro ao processar a solicitação\"}");
	    } finally {
	        out.flush();
	    }
	}


	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// Declaração de json e acao
		
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		
		// Declareando a dado!
		String acao = request.getParameter("acao");
		 int idendereco = 0;
	
		 if("create".equals(acao)) {
			 try {
				 Endereco endereco = new Endereco(); 
				 endereco.setCep(request.getParameter("cep"));
				
				 // Cadastro de CEP no banco de dados!
				 
				 boolean CepTeste = enderecoDAO.existeCep(endereco);
				 
				 if (CepTeste == true) {
					
					 idendereco = enderecoDAO.obterIdEnderecoPorCep(endereco);
					
				 } else {
					
					 //Endereco; Cep;Bairro;Cidade; Estado;Pais;
					 endereco.setEndereco(request.getParameter("endereco"));
					 endereco.setBairro(request.getParameter("bairro"));
					 endereco.setCidade(request.getParameter("cidade"));
					 endereco.setEstado(request.getParameter("estado"));
					 endereco.setPais(request.getParameter("pais"));
					  enderecoDAO.create(endereco);
					  idendereco = enderecoDAO.obterIdEnderecoPorCep(endereco);
				 }
				 
				
				 String cnpj= request.getParameter("cnpjPessoaCnpj");
				 Boolean TesteCNPJ = parceiroDao.existeCNPJ(cnpj);
				 	if(TesteCNPJ == true)
				 			{
				 		Mensagem = "CNPJ Já cadastrado, Cadastrar novo parceiro!";
				 		Atributo= "danger";
				        request.setAttribute("Mensagem", Mensagem);
				 		 request.setAttribute("Atributo", Atributo);
				        request.getRequestDispatcher(Caminho +"CadastroParceiro.jsp").forward(request, response);
				 		
				 			}else {
				 				 System.out.println("nivel 1");
				 				 Endereco endereco2 = new Endereco();
				 				endereco2.setIdendereco(idendereco);
				 			String nome = request.getParameter("nomepessoa"); 
		 					String usuario =	request.getParameter("usuariopessoa");
		 					String senha = request.getParameter("senhapessoa");
		 					String nivel =	request.getParameter("nivelpessoa"); 
		 					Boolean situacao = Boolean.valueOf(request.getParameter("situacaopessoa")); 
		 					String email = request.getParameter("emailpessoa");
		 					int numero=  Integer.parseInt(request.getParameter("numero"));
		 					String complemento = request.getParameter("complemento");
		 					String telefone = request.getParameter("telefonepessoa");
		 					String cnpjpessoa =	cnpj; 
		 					String razaoSocial = request.getParameter("razaoSocialPessoaCnpj"); 
		 					String inscricaoEstadual = request.getParameter("inscricaoEstadualPessoaCnpj"); 
		 					String site=	request.getParameter("siteparceiro");
		 				                                     System.out.println("site"+ nome);
		 				//Declaração de classe 
				 				 Parceiro parceiro = new Parceiro(
				 						 0, nome, usuario,
				 						 senha, nivel, situacao,
				 						 email, telefone, endereco2,
				 						 numero, complemento, cnpjpessoa, 
				 						 razaoSocial, inscricaoEstadual, site);
				 				 
				 				 // Cadastro endereco e pessoa table Enderecopessoa
				 		parceiroDao.addParceiro(parceiro);
				 		
				 		Mensagem = "Cadastro com Sucesso!";
				 		Atributo= "success";
				        request.setAttribute("Mensagem", Mensagem);
				 		request.setAttribute("Atributo", Atributo);
				        request.getRequestDispatcher(Caminho +"parceiro.jsp").forward(request, response);
		 					
				 			}
			} catch (Exception e) {
				Mensagem = "Erro no cadastro!";
		 		Atributo= "danger";
		        request.setAttribute("Mensagem", Mensagem);
		 		request.setAttribute("Atributo", Atributo);
		        request.getRequestDispatcher(Caminho +"CadastroParceiro.jsp").forward(request, response);
			}
			
			
		 }else if("atualizarenvio".equals(acao)){
			 int id = Integer.parseInt(request.getParameter("idPessoa"));
			  try {
				Parceiro parceiro = parceiroDao.getParceiroById(id);
				
				 request.setAttribute("parceiro", parceiro);
			        RequestDispatcher dispatcher = request.getRequestDispatcher( Caminho+ "AtualizarParceiro.jsp");
			        dispatcher.forward(request, response);
			} catch (SQLException e) {
				Mensagem = "Erro de Buscar dados!!!!!";
		 		Atributo= "danger";
		        request.setAttribute("Mensagem", Mensagem);
		 		request.setAttribute("Atributo", Atributo);
		        request.getRequestDispatcher(Caminho +"parceiro.jsp").forward(request, response);
			}
			
		 }else if("Atualizar".equals(acao)) {
			 try {
				 System.out.println("Testei");
				 Endereco endereco = new Endereco(); 
				 endereco.setCep(request.getParameter("cep"));
				 // Cadastro de CEP no banco de dados!
				 boolean CepTeste = enderecoDAO.existeCep(endereco);
				 
				 if (CepTeste == true) {
					
					 idendereco = enderecoDAO.obterIdEnderecoPorCep(endereco);
					
				 } else {
					
					 // Declaração de variaveis para classe Endereço
					 
					 endereco.setEndereco(request.getParameter("endereco"));
					 endereco.setBairro(request.getParameter("bairro"));
					 endereco.setCidade(request.getParameter("cidade"));
					 endereco.setEstado(request.getParameter("estado"));
					 endereco.setPais(request.getParameter("pais"));
					  
					 	//Execução da DaoEndereco
					 	
					  enderecoDAO.create(endereco);
					  
					//Buscando id do endereço
					 	
					  idendereco = enderecoDAO.obterIdEnderecoPorCep(endereco);
				 }
				 
				 // Declaração de variaveis para classe Endereço para Classe Parceiro	
				 
 				 Endereco endereco2 = new Endereco();
 				endereco2.setIdendereco(idendereco);
 				
 		
 			
 				
 				int idpessoa =Integer.parseInt(request.getParameter("idpessoa"));
 				String nome = request.getParameter("nomepessoa"); 
				String usuario =	request.getParameter("usuariopessoa");
				String senha = request.getParameter("senhapessoa");
				String nivel =	request.getParameter("nivelpessoa"); 
				Boolean situacao = Boolean.valueOf(request.getParameter("situacaopessoa")); 
				String email = request.getParameter("emailpessoa");
				int numero=  Integer.parseInt(request.getParameter("numero"));
				String complemento = request.getParameter("complemento");
				String telefone = request.getParameter("telefonepessoa");
				String cnpjpessoa =	request.getParameter("cnpjPessoaCnpj");
				String razaoSocial = request.getParameter("razaoSocialPessoaCnpj"); 
				String inscricaoEstadual = request.getParameter("inscricaoEstadualPessoaCnpj"); 
				String site=	request.getParameter("siteparceiro");
				
				
				
 				 Parceiro parceiro = new Parceiro(
 						 idpessoa, nome, usuario,
 						 senha, nivel, situacao,
 						 email, telefone, endereco2,
 						 numero, complemento, cnpjpessoa, 
 						 razaoSocial, inscricaoEstadual, site);
 				 
 				 parceiroDao.updateParceiro(parceiro);
 		
 				 Enviando mensagem redirecionado para Parceiro.jsp
 				 	
 				 	Mensagem = "Atualizado com Sucesso!";
 				 	Atributo= "success";
 				 	request.setAttribute("Mensagem", Mensagem);
 				 	request.setAttribute("Atributo", Atributo);
 				 	request.getRequestDispatcher(Caminho +"parceiro.jsp").forward(request, response);
			} catch (Exception e) {
				
			
				
				Mensagem = "Erro em atualizar";
		 		Atributo= "danger";
		        request.setAttribute("Mensagem", Mensagem);
		 		request.setAttribute("Atributo", Atributo);
		        request.getRequestDispatcher(Caminho +"parceiro.jsp").forward(request, response);
			}
		 }else if("desativar".equals(acao)) {// excluir o dado
			  try {
				// Obtém o parâmetro 'situacao' do formulário
			 		String situacaoParam = request.getParameter("situacaopessoa");
			 		int id = Integer.parseInt(request.getParameter("idpessoa"));
		       
			 		// Converte o parâmetro para booleano
		       
			 		boolean situacao = Boolean.parseBoolean(situacaoParam);

		        // Inverte a situação
		        situacao = !situacao;
		        // Instanciando
		       
		       parceiroDao.deleteParceiro(id, situacao);
		       String mensagem = "Parceiro desativado com sucesso!";
		        request.setAttribute("Mensagem", mensagem);
		        request.setAttribute("Atributo", "success");
		        request.getRequestDispatcher(Caminho +"parceiro.jsp").forward(request, response);
		    
			} catch (Exception e) {
				Mensagem = "Erro em desaativar os dados";
		 		Atributo= "danger";
		        request.setAttribute("Mensagem", Mensagem);
		 		request.setAttribute("Atributo", Atributo);
		        request.getRequestDispatcher(Caminho +"parceiro.jsp").forward(request, response);
			}
			 
		 }
		
	}

}
*/
package Controller;

import Model.Dao.EnderecoDAO;
import Model.Dao.ParceiroDAO;
import Model.Model.Endereco;
import Model.Model.Parceiro;
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

public class ControllerParceiro extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private EnderecoDAO enderecoDAO;
    private ParceiroDAO parceiroDao;
    private final Gson gson = new Gson();

    @Override
    public void init() {
        enderecoDAO = new EnderecoDAO();
        parceiroDao = new ParceiroDAO();
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
       GET: lista ou parceiro por ID
       ============================================================ */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        String idParam = request.getParameter("id");

        try (PrintWriter out = response.getWriter()) {
            if (idParam != null && !idParam.isEmpty()) {
                int id = Integer.parseInt(idParam);
                Parceiro parceiro = parceiroDao.getParceiroById(id);

                if (parceiro != null) {
                    out.print(gson.toJson(parceiro));
                } else {
                    writeJson(response, HttpServletResponse.SC_NOT_FOUND, false,
                            "Parceiro não encontrado.", "page");
                }
            } else {
                List<Parceiro> lista = parceiroDao.listAllParceiro();
                out.print(gson.toJson(lista));
            }
            out.flush();
        } catch (Exception e) {
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Erro ao carregar parceiros: " + e.getMessage(), "page");
        }
    }

    /* ============================================================
       POST: create | update | delete (JSON no corpo)
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
                writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false,
                        "Ação inválida.", "page");
        }
    }

    /* ----------------------------- CREATE ----------------------------- */
    private void handleCreate(JsonObject json, HttpServletResponse response) throws IOException {
        try {
            // Endereço
            Endereco endereco = new Endereco();
            endereco.setCep(json.get("cep").getAsString());

            int idEndereco;
            if (enderecoDAO.existeCep(endereco)) {
                idEndereco = enderecoDAO.obterIdEnderecoPorCep(endereco);
            } else {
                endereco.setEndereco(json.get("endereco").getAsString());
                endereco.setBairro(json.get("bairro").getAsString());
                endereco.setCidade(json.get("cidade").getAsString());
                endereco.setEstado(json.get("estado").getAsString());
                endereco.setPais(json.get("pais").getAsString());
                enderecoDAO.create(endereco);
                idEndereco = enderecoDAO.obterIdEnderecoPorCep(endereco);
            }

            String cnpj = json.get("cnpjPessoaCnpj").getAsString();
            if (parceiroDao.existeCNPJ(cnpj)) {
                writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false,
                        "CNPJ já cadastrado.", "modalCadastroParceiro");
                return;
            }

            Endereco endereco2 = new Endereco();
            endereco2.setIdendereco(idEndereco);

            Parceiro parceiro = new Parceiro(
                    0,
                    json.get("nomepessoa").getAsString(),
                    json.get("usuariopessoa").getAsString(),
                    json.get("senhapessoa").getAsString(),
                    json.get("nivelpessoa").getAsString(),
                    json.get("situacaopessoa").getAsBoolean(),
                    json.get("emailpessoa").getAsString(),
                    json.get("telefonepessoa").getAsString(),
                    endereco2,
                    json.get("numero").getAsInt(),
                    json.get("complemento").getAsString(),
                    cnpj,
                    json.get("razaoSocialPessoaCnpj").getAsString(),
                    json.get("inscricaoEstadualPessoaCnpj").getAsString(),
                    json.get("siteparceiro").getAsString()
            );

            parceiroDao.addParceiro(parceiro);
            writeJson(response, HttpServletResponse.SC_OK, true,
                    "Parceiro cadastrado com sucesso!", "page");

        } catch (Exception e) {
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Erro ao cadastrar: " + e.getMessage(), "modalCadastroParceiro");
        }
    }

    /* ----------------------------- UPDATE ----------------------------- */
    private void handleUpdate(JsonObject json, HttpServletResponse response) throws IOException {
        try {
            int idEndereco;
            Endereco endereco = new Endereco();
            endereco.setCep(json.get("cep").getAsString());

            if (enderecoDAO.existeCep(endereco)) {
                idEndereco = enderecoDAO.obterIdEnderecoPorCep(endereco);
            } else {
                endereco.setEndereco(json.get("endereco").getAsString());
                endereco.setBairro(json.get("bairro").getAsString());
                endereco.setCidade(json.get("cidade").getAsString());
                endereco.setEstado(json.get("estado").getAsString());
                endereco.setPais(json.get("pais").getAsString());
                enderecoDAO.create(endereco);
                idEndereco = enderecoDAO.obterIdEnderecoPorCep(endereco);
            }

            Endereco endereco2 = new Endereco();
            endereco2.setIdendereco(idEndereco);

            Parceiro parceiro = new Parceiro(
                    json.get("idpessoa").getAsInt(),
                    json.get("nomepessoa").getAsString(),
                    json.get("usuariopessoa").getAsString(),
                    json.get("senhapessoa").getAsString(),
                    json.get("nivelpessoa").getAsString(),
                    json.get("situacaopessoa").getAsBoolean(),
                    json.get("emailpessoa").getAsString(),
                    json.get("telefonepessoa").getAsString(),
                    endereco2,
                    json.get("numero").getAsInt(),
                    json.get("complemento").getAsString(),
                    json.get("cnpjPessoaCnpj").getAsString(),
                    json.get("razaoSocialPessoaCnpj").getAsString(),
                    json.get("inscricaoEstadualPessoaCnpj").getAsString(),
                    json.get("siteparceiro").getAsString()
            );

            parceiroDao.updateParceiro(parceiro);
            writeJson(response, HttpServletResponse.SC_OK, true,
                    "Parceiro atualizado com sucesso!", "page");

        } catch (Exception e) {
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Erro ao atualizar: " + e.getMessage(), "modalAtualizarParceiro");
        }
    }

    /* ----------------------------- DELETE ----------------------------- */
    private void handleDelete(JsonObject json, HttpServletResponse response) throws IOException {
        try {
            int id = json.get("idpessoa").getAsInt();
            boolean situacao = json.get("situacaopessoa").getAsBoolean();

            parceiroDao.deleteParceiro(id, !situacao);
            writeJson(response, HttpServletResponse.SC_OK, true,
                    "Parceiro desativado com sucesso!", "page");

        } catch (Exception e) {
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Erro ao desativar: " + e.getMessage(), "modalExcluirParceiro");
        }
    }
}
