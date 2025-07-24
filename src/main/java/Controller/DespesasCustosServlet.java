/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package Controller;

import Model.Dao.DespesasCustosDAO;
import Model.Model.DespesasCustos;
import com.google.gson.Gson;
import java.io.IOException;
import java.io.PrintWriter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.sql.SQLException;
import java.util.List;

/**
 *
 * @author lucia
 */
public class DespesasCustosServlet extends HttpServlet {
private static final long serialVersionUID = 1L;
	  
	    private DespesasCustosDAO despesasCustosDAO;
	    @Override
		public void init() {
			despesasCustosDAO = new DespesasCustosDAO();
		}
   protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		 response.setContentType("application/json");
	     response.setCharacterEncoding("UTF-8"); 
	     try {
	    	 List<DespesasCustos> despesasCustosList = despesasCustosDAO.listAll();
	    	 String json = new Gson().toJson(despesasCustosList);
	    	 PrintWriter out = response.getWriter();
	         out.print(json);
	         out.flush();
		} catch (IOException | SQLException e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getWriter().write("{\"error\":\"Atenção: " + e.getMessage() + "\"}");	
			}
	}
   	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		String acao = request.getParameter("acao");
		 if("create".equals(acao)) {
			 try {
				 // Buscando as informação de jsp para servlet
				 
				 String despesascustos = request.getParameter("despesascusto");
				 String unidadedespesascustos =  request.getParameter("unidadedespesacusto");
				 String valordespesascustos =  request.getParameter("valordespesacusto");
				 String tipodespesascustos =  request.getParameter("tipodespesacusto");
				 
				// Converter o valor de moeda brasileira para o formato americano
				    String valorUS = valordespesascustos.replace(".", "").replace(",", ".");
				 
				 System.out.println(valorUS);
				 /* despesascusto, unidadedespesascustos, valordespesascustos, tipodespesascustos*/
				// Cria uma nova instância
				 Boolean testedespesa= despesasCustosDAO.existeDespesaCusto(despesascustos);
				  if (testedespesa == true) { 
					 // System.out.println("tem");
					  response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					  response.getWriter().write("\"Despesas já existe registro!\"");
						return;
						
				  }
				  /*declarando para model despesas e custo*/
				  
				  DespesasCustos despesascustoclass= new DespesasCustos();
					despesascustoclass.setDespesascusto(despesascustos);
					despesascustoclass.setUnidadedespesascustos(unidadedespesascustos);
					despesascustoclass.setValordespesascustos(Double.parseDouble(valorUS));
					despesascustoclass.setTipodespesascustos(tipodespesascustos);
					/* despesascusto, unidadedespesascustos, valordespesascustos, tipodespesascustos*/
					
					/*Conectando Dao*/
					// Chama o método de atualização no DAO
					despesasCustosDAO.create(despesascustoclass);
					
					 // Envia resposta de sucesso
					response.setStatus(HttpServletResponse.SC_OK);
					response.getWriter().write("{\"message\":\"Despesas cadastro  com sucesso!\"}");


			} catch (Exception e) {
				e.printStackTrace();
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				response.getWriter()
						.write("{\"error\":\"Atenção: " + e.getMessage() + "\"}");

			}
		 } else if("update".equals(acao)) { 
			 try {
				 String iddespesascustos = request.getParameter("iddespesascustos");
				 String despesascustos = request.getParameter("despesascustos");
				 String unidadedespesascustos= request.getParameter("unidadedespesacustos");
				 String valordespesascustos = request.getParameter("valordespesacustos");
				 String tipodespesascustos = request.getParameter("tipodespesacustos");
				// Converter o valor de moeda brasileira para o formato americano
				    String valorUS = valordespesascustos.replace(".", "").replace(",", ".");
					/*
					 * System.out.println(iddespesascustos); System.out.println(despesascustos);
					 * System.out.println("unidade: "+unidadedespesascustos);
					 * System.out.println("unidade2:" + valordespesascustos);
					 * System.out.println(tipodespesascustos);
					 */
		     //Variaveis do sistema
		     DespesasCustos despesascustosclass = new DespesasCustos();
		     despesascustosclass.setIddespesascusto(Integer.parseInt(iddespesascustos));
		     despesascustosclass.setDespesascusto(despesascustos);
		     despesascustosclass.setValordespesascustos(Double.parseDouble(valorUS));
		     despesascustosclass.setUnidadedespesascustos(unidadedespesascustos);
		     despesascustosclass.setTipodespesascustos(tipodespesascustos);
		     
		     despesasCustosDAO.update(despesascustosclass);
		      
		   // Envia resposta de sucesso
				response.setStatus(HttpServletResponse.SC_OK);
				response.getWriter().write("{\"message\":\"Despesas atualizada com sucesso!\"}");
		} catch (Exception e) {
			e.printStackTrace();
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getWriter()
					.write("{\"error\":\"Atenção: " + e.getMessage() + "\"}");

		}
			 
		
			 
		 } else { 
			 try {
				 String iddespesascustos = request.getParameter("iddespesascusto");
				 //Variaveis do sistema
				 System.out.println(iddespesascustos);
			      DespesasCustos despesascustosclass = new DespesasCustos();
			      despesascustosclass.setIddespesascusto(Integer.parseInt(iddespesascustos));
			      
			   // Atualizando
			      despesasCustosDAO.delete(despesascustosclass);
			      
			   // Envia resposta de sucesso
					response.setStatus(HttpServletResponse.SC_OK);
					response.getWriter().write("{\"message\":\"Excluido com sucesso!\"}");
			} catch (Exception e) {
				e.printStackTrace();
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				response.getWriter()
						.write("{\"error\":\"Atenção: " + e.getMessage() + "\"}");
			}
			 
		 }
		 
	}
}
