/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Model.Dao;

import Util.PostgresConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class AlimentoClassificacaoDao {
    
    //Adicionar o alimentoclassificacao da lista alimento
    
    public void addAlimentoClassificacao(AlimentoClassificacao alimentoclassificacao) throws SQLException {
    	 PostgresConnection conn = new PostgresConnection();
		 Connection conexao= conn.getConnection();
		 System.out.println("nivel 2");
        try {
        	String sql = "INSERT INTO alimentoclassificacao ("
        		    + "idproduto, idclasssificacao)"
        		    + "VALUES ( ?,?)";

            PreparedStatement stmt = conexao.prepareStatement(sql);
            stmt.setInt(1, alimentoclassificacao.getAlimento().getIdproduto());
            stmt.setInt(2, alimentoclassificacao.getClassificacao().getIdclassificacao());
         
            stmt.executeUpdate(); 
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao adicionar o parceiro: " + e.getMessage(), e);
        }
    }
    
    //Deletar o alimentoclassificacao da lista alimento
    
     public void deletarAlimento(AlimentoClassificacao alimentoclassificacao) throws SQLException {
                 PostgresConnection conn = new PostgresConnection();
		 Connection conexao= conn.getConnection();
		 System.out.println("nivel 2");
        try {
        	String sql = "delete from alimentoclassificacao where idalimentoclassificacao=?";
        	PreparedStatement stmt = conexao.prepareStatement(sql);
                stmt.setInt(1, alimentoclassificacao.getIdalimentoclassificao());
                stmt.executeUpdate(); 
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao adicionar o parceiro: " + e.getMessage(), e);
        }
    }
}
