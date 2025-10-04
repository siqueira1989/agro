package Model.Dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import Model.Model.Endereco;
import Util.PostgresConnection;


/**
 *
 * @author Luciano Siqueira
 */
public class EnderecoDAO {
	 public boolean existeCep(Endereco endereco) throws SQLException {
		 PostgresConnection conn = new PostgresConnection();
		 Connection conexao= conn.getConnection();
		 
	        boolean existe = false;

	        // Substitua pelo seu método de obter conexão com o banco de dados
	        try {
	        	 String sql = "SELECT COUNT(*) FROM endereco WHERE cep = ?";
	             PreparedStatement stmt = conexao.prepareStatement(sql);
	             // Definindo o valor do parâmetro da consulta
	             stmt.setString(1, endereco.getCep());
	             ResultSet rs = stmt.executeQuery();
	             if (rs.next()) {
	                    // Verificando se a contagem é maior que zero
	                    existe = rs.getInt(1)>0;
	                }


	        } catch (SQLException e) {
	            System.out.println("Erro no nivel dao: "+e.getMessage()); // Tratar exceções de forma adequada na sua aplicação
	        }
  
	        return existe;
	    }
	 
	 public Integer obterIdEnderecoPorCep(Endereco endereco) throws SQLException {
		    PostgresConnection conn = new PostgresConnection();
		    Connection conexao = conn.getConnection();
		    
		    Integer idEndereco = null;

		    try {
		        // Modificando a consulta SQL para retornar o 'idendereco' ao invés de contar os registros
		        String sql = "SELECT idendereco FROM endereco WHERE cep = ?";
		        PreparedStatement stmt = conexao.prepareStatement(sql);
		        
		        // Definindo o valor do parâmetro da consulta
		        stmt.setString(1, endereco.getCep());
		        ResultSet rs = stmt.executeQuery();
		        
		        if (rs.next()) {
		            // Atribuindo o valor de 'idendereco' à variável
		            idEndereco = rs.getInt("idendereco");
		        }

		    } catch (SQLException e) {
		        System.out.println("Erro no nivel DAO: " + e.getMessage()); // Tratar exceções de forma adequada na sua aplicação
		    } finally {
		        if (conexao != null) {
		            conexao.close(); // Certifique-se de fechar a conexão para evitar vazamento de recursos
		        }
		    }
		    
		  
		    return idEndereco;
		}
	 public void create(Endereco endereco) throws SQLException {
	     
	        PostgresConnection conn = new PostgresConnection();
		    Connection conexao = conn.getConnection();
	        try {
	        	   String sql = "INSERT INTO endereco (endereco, cep, bairro, cidade, estado, pais) VALUES (?, ?, ?, ?, ?, ?)";
	        	   PreparedStatement stmt = conexao.prepareStatement(sql);
	            stmt.setString(1, endereco.getEndereco());
	            stmt.setString(2, endereco.getCep());
	            stmt.setString(3, endereco.getBairro());
	            stmt.setString(4, endereco.getCidade());
	            stmt.setString(5, endereco.getEstado());
	            stmt.setString(6, endereco.getPais());
	           
	            stmt.executeUpdate();
	        }catch (SQLException e) {
			     System.out.println("Erro no nivel DAO: " + e.getMessage());
			}
	    }
	
}
