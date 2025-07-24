package Model.Dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import Model.Model.Pessoa;
import Util.PostgresConnection;


public class PessoaDAO {
	 public Pessoa validarUsuario(Pessoa pessoa) throws SQLException {
    	 PostgresConnection conn = new PostgresConnection();
         Connection conexao= conn.getConnection();
        	
            
       Pessoa pessoas = null;
         
               try {
            	String sql = "select idpessoa,nomepessoa ,usuariopessoa,nivelpessoa,situacaopessoa from pessoa"
            			+ " WHERE usuariopessoa = ? and senhapessoa= ? ";
            	PreparedStatement stmt = conexao.prepareStatement(sql);
            	 stmt.setString(1, pessoa.getUsuarioPessoa());
            	 stmt.setString(2, pessoa.getSenhaPessoa());
                ResultSet rs = stmt.executeQuery();
               if (rs.next()) {
                	/*int idPessoa, 
                	 * String nomePessoa,
                	
                	 *  String usuarioPessoa, 
                    String senhaPessoa,
                     String nivelPessoa, 
                     boolean situacaoPessoa,
                      String emailPessoa,
                       String telefonePessoa*/
                	 Pessoa pessoausuario = new Pessoa();
                		pessoausuario.setIdPessoa(rs.getInt("idclassificacao")); 
                		pessoausuario.setNomePessoa(rs.getString("nomepessoa"));
                		pessoausuario.setUsuarioPessoa(rs.getString("usuariopessoa"));
                		pessoausuario.setNivelPessoa( rs.getString("nivelpessoa"));
                		pessoausuario.setSituacaoPessoa(rs.getBoolean("situacaopessoa")); 
                		pessoas = pessoausuario;
                }
                conexao.close();
                } catch (Exception e) {
                	e.printStackTrace();
                    System.out.println("Erro no cadastro: "+e.getMessage());

			}
			 
		        return pessoas;
    }
}
