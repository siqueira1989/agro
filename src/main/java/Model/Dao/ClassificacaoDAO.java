
package Model.Dao;

import Model.Model.Classificacao;
import Util.PostgresConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author lucia
 */
public class ClassificacaoDAO {
   
	 // Método para listar todas as classificações do banco de dados
    public List<Classificacao> listAll() throws SQLException {
    	 PostgresConnection conn = new PostgresConnection();
         Connection conexao= conn.getConnection();
        	
            
            
            List<Classificacao> classificacoes = new ArrayList<>();
                       try {
            	String sql = "SELECT * FROM classificacao";
            	PreparedStatement stmt = conexao.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                	 Classificacao classificacao = new Classificacao();

                    classificacao.setIdclassificacao(rs.getInt("idclassificacao"));
                    classificacao.setClassificacao(rs.getString("classificacao"));

                    classificacoes.add(classificacao);
                }
                conexao.close();
                } catch (Exception e) {
                	e.printStackTrace();
                    System.out.println("Erro no cadastro: "+e.getMessage());

			}
			 
		        return classificacoes;
    }
    //Método para criar uma nova classificação no banco de dados
    public void saveClassificacao(Classificacao classificacao) throws SQLException {
        
        PostgresConnection conn = new PostgresConnection();
        Connection conexao= conn.getConnection();
        try  {
        	String sql = "INSERT INTO classificacao (classificacao) VALUES (?)";
        	 PreparedStatement ptmt= conexao.prepareStatement(sql);
            ptmt.setString(1, classificacao.getClassificacao());
            ptmt.executeUpdate();
        }catch (Exception e) {
        	e.printStackTrace();
            System.out.println("Erro no cadastro: "+e.getMessage());
		}
        conexao.close();
    }
    public void updateClassificacao(Classificacao classificacao) throws SQLException {
      
    	 PostgresConnection conn = new PostgresConnection();
         Connection conexao= conn.getConnection();
        try {
        	 String sql = "UPDATE classificacao SET classificacao = ? WHERE idclassificacao = ?";
        	 PreparedStatement ptmt = conexao.prepareStatement(sql);
        	
        	 ptmt.setString(1, classificacao.getClassificacao());
        	 ptmt.setInt(2, classificacao.getIdclassificacao());
        	 ptmt.executeUpdate();
        }catch (Exception e) {
        	e.printStackTrace();
            System.out.println("Erro no cadastro: "+e.getMessage());
		}
    }
    public void deleteClassificacao(Classificacao classificacao) throws SQLException {
        
        PostgresConnection conn = new PostgresConnection();
        Connection conexao= conn.getConnection();
        try  {
        	 String sql = "DELETE FROM classificacao WHERE idclassificacao = ?";
        	 PreparedStatement ptmt = conexao.prepareStatement(sql);
        	 ptmt.setInt(1, classificacao.getIdclassificacao());
        	 ptmt.executeUpdate();
        }catch (Exception e) {
        	e.printStackTrace();
            System.out.println("Erro no cadastro: "+e.getMessage());
		}
    }
// Metodo de Criação de busca existente
     public boolean VerificacaoClassificacao (String classificacao) throws SQLException {
		 PostgresConnection conn = new PostgresConnection();
		 Connection conexao= conn.getConnection();
		 
	        boolean verificacaoclassificacao = false;

	        // Substitua pelo seu método de obter conexão com o banco de dados
	        try {
	        	 String sql = "SELECT COUNT(*) FROM classificacao WHERE classificacao = ?";
	             PreparedStatement stmt = conexao.prepareStatement(sql);
	             // Definindo o valor do parâmetro da consulta
	             stmt.setString(1, classificacao);
	             ResultSet rs = stmt.executeQuery();
	             if (rs.next()) {
	                    // Verificando se a contagem é maior que zero
	                    verificacaoclassificacao = rs.getInt(1)>0;
	                }


	        } catch (SQLException e) {
	            System.out.println("Erro no nivel dao: "+e.getMessage()); // Tratar exceções de forma adequada na sua aplicação
	        }
   
	        return verificacaoclassificacao;
	    }
          public boolean VerificacaoClassificacaoAlimento (int idclassificacao) throws SQLException {
		 PostgresConnection conn = new PostgresConnection();
		 Connection conexao= conn.getConnection();
		 
	        boolean verificacaoclassificacao = false;

	        // Substitua pelo seu método de obter conexão com o banco de dados
	        try {
	        	 String sql = "SELECT COUNT(*) FROM alimentoclassificacao ac inner join classificacao c on ac.idclassificacao = c.idclassificacao where ac.idclassificacao=?";
	             PreparedStatement stmt = conexao.prepareStatement(sql);
	             // Definindo o valor do parâmetro da consulta
	             stmt.setInt(1, idclassificacao);
	             ResultSet rs = stmt.executeQuery();
	             if (rs.next()) {
	                    // Verificando se a contagem é maior que zero
	                    verificacaoclassificacao = rs.getInt(1)>0;
	                }


	        } catch (SQLException e) {
	            System.out.println("Erro no nivel dao: "+e.getMessage()); // Tratar exceções de forma adequada na sua aplicação
	        }
   
	        return verificacaoclassificacao;
	    }
}

