package Model.Dao;

import Util.PostgresConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;


public class EnderecoPessoaDAO {
	public void createEnderecoPessoa(int idpessoa, int idendereco, String  numero, String complemento) throws SQLException {
	    
	    PostgresConnection conn = new PostgresConnection();
	    Connection conexao = conn.getConnection();
	    
	    try {
	        // Query para inserir os dados na tabela endereco_pessoa
	    	
	        String sql = "INSERT INTO enderecopessoa (idpessoa, idendereco, numero, complemento) VALUES (?, ?, ?, ?)";
	        PreparedStatement stmt = conexao.prepareStatement(sql);
	        
	        // Definindo os valores dos parâmetros
	        stmt.setInt(1, idpessoa); // idpessoa
	        stmt.setInt(2, idendereco); // idendereco
	        stmt.setString(3, numero); // Número
	        stmt.setString(4,complemento); // Complemento
	        
	        // Executando a operação de inserção
	        stmt.executeUpdate();
	        
	    } catch (SQLException e) {
	        System.out.println("Erro no nível EnderecoPessoaDAO Create: " + e.getMessage());
	    } finally {
	        if (conexao != null) {
	            conexao.close(); // Fechando a conexão após o uso
	        }
	    }
	}

}
