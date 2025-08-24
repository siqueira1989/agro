/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Model.Dao;

import Model.Model.*;
import Util.PostgresConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AlimentoClassificacaoDao {
    
    //Adicionar o alimentoclassificacao da lista alimento
    
    public void addAlimentoClassificacao(AlimentoClassificacao alimentoclassificacao) throws SQLException {
    	 PostgresConnection conn = new PostgresConnection();
		 Connection conexao= conn.getConnection();
		 System.out.println("nivel 2");
        try {
        	String sql = "INSERT INTO alimentoclassificacao("
        		    + "idproduto, idclassificacao)"
        		    + "VALUES (?,?)";

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
    	 public Integer RetornoIdAlimento()throws SQLException {
		    PostgresConnection conn = new PostgresConnection();
		    Connection conexao = conn.getConnection();
		    
		    Integer numero = null;

		    try {
		        // Modificando a consulta SQL para retornar o 'idendereco' ao invés de contar os registros
		        String sql = "SELECT last_value as numero FROM public.produto_idproduto_seq";
		        PreparedStatement stmt = conexao.prepareStatement(sql);
		        
		        // Definindo o valor do parâmetro da consulta
		      
		        ResultSet rs = stmt.executeQuery();
		        
		        if (rs.next()) {
		            // Atribuindo o valor de 'idendereco' à variável
		            numero= rs.getInt("numero");
		        }

		    } catch (SQLException e) {
		        System.out.println("Erro no nivel DAO: " + e.getMessage()); // Tratar exceções de forma adequada na sua aplicação
		    } finally {
		        if (conexao != null) {
		            conexao.close(); // Certifique-se de fechar a conexão para evitar vazamento de recursos
		        }
		    }
		    
		  
		    return numero;
		}
}
