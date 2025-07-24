/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
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


	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
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
				 
				/*Processo de verificacao*/
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
 				
 		
 				/*Declaração de variaveis*/
 				
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
				
				/*Declarando para classe*/ 
				
 				 Parceiro parceiro = new Parceiro(
 						 idpessoa, nome, usuario,
 						 senha, nivel, situacao,
 						 email, telefone, endereco2,
 						 numero, complemento, cnpjpessoa, 
 						 razaoSocial, inscricaoEstadual, site);
 				
 				 /*Executando DAO*/
 				 
 				 parceiroDao.updateParceiro(parceiro);
 		
 				 /*Enviando mensagem redirecionado para Parceiro.jsp*/
 				 	
 				 	Mensagem = "Atualizado com Sucesso!";
 				 	Atributo= "success";
 				 	request.setAttribute("Mensagem", Mensagem);
 				 	request.setAttribute("Atributo", Atributo);
 				 	request.getRequestDispatcher(Caminho +"parceiro.jsp").forward(request, response);
			} catch (Exception e) {
				
				/*Enviando mensagem redirecionado para Parceiro.jsp*-> Erro não foi atualizado*/
				
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
