
package Model.Dao;

import Model.Model.Alimento;
import Util.PostgresConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class AlimentoDAO {
     public void addAlimento(Alimento alimento) throws SQLException {
    	 PostgresConnection conn = new PostgresConnection();
		 Connection conexao= conn.getConnection();
		 System.out.println("nivel 2");
        try {
        	String sql = "INSERT INTO alimento ("
        		    + "nomeproduto, tipoproduto, "
        		    + "situacaoproduto ,variedadealimento)"
        		    + "VALUES ( ?,?,?,?)";

        	PreparedStatement stmt = conexao.prepareStatement(sql);
           stmt.setString(1, alimento.getNomeproduto());
            stmt.setString(2, alimento.getTipoproduto());
            stmt.setBoolean(3, alimento.isSituacaoproduto());
            stmt.setString(4, alimento.getVariedadealimento());
          
            stmt.executeUpdate(); 
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao adicionar o parceiro: " + e.getMessage(), e);
        }
    }
       public void updateAlimento(Alimento alimento) throws SQLException {
    	 PostgresConnection conn = new PostgresConnection();
		 Connection conexao= conn.getConnection();
		 System.out.println("nivel 2");
        try { 
            String sql = "UPDATE classificacao SET nomeproduto = ?, tipoproduto = ?,"+
                     "situacaoproduto = ?, variedadealimento= ? WHERE idproduto = ?";
        	

        	PreparedStatement stmt = conexao.prepareStatement(sql);
           stmt.setString(1, alimento.getNomeproduto());
            stmt.setString(2, alimento.getTipoproduto());
            stmt.setBoolean(3, alimento.isSituacaoproduto());
            stmt.setString(4, alimento.getVariedadealimento());
           stmt.setInt(4, alimento.getIdproduto());
            stmt.executeUpdate(); 
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao adicionar o parceiro: " + e.getMessage(), e);
        }
    }
}
