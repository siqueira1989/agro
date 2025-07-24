package Model.Dao;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import Model.Model.Endereco;
import Model.Model.Parceiro;
import Util.PostgresConnection;

public class ParceiroDAO {

	/*Cadastrar de parceiro*/
	
    public void addParceiro(Parceiro parceiro) throws SQLException {
    	 PostgresConnection conn = new PostgresConnection();
		 Connection conexao= conn.getConnection();
		 System.out.println("nivel 2");
        try {
        	 String sql = "INSERT INTO parceiro ("
            + "nomepessoa, usuariopessoa, senhapessoa, nivelpessoa, "
            + "situacaopessoa, emailpessoa, numero, complemento, "
            + "idendereco, telefonepessoa, cnpjPessoaCnpj, razaosocialpessoacnpj, "
            + "inscricaoestadualpessoacnpj, siteparceiro) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        	PreparedStatement stmt = conexao.prepareStatement(sql);
           stmt.setString(1, parceiro.getNomePessoa());
            stmt.setString(2, parceiro.getUsuarioPessoa());
            stmt.setString(3, parceiro.getSenhaPessoa());
            stmt.setString(4, parceiro.getNivelPessoa());
            stmt.setBoolean(5, parceiro.isSituacaoPessoa());
            stmt.setString(6, parceiro.getEmailPessoa());
            stmt.setInt(7, parceiro.getNumero());
            stmt.setString(8, parceiro.getComplemento());
            stmt.setInt(9, parceiro.getEndereco().getIdendereco());
            stmt.setString(10, parceiro.getTelefonePessoa());
            stmt.setString(11, parceiro.getCnpjPessoaCnpj());
            stmt.setString(12, parceiro.getRazaoSocialPessoaCnpj());
            stmt.setString(13, parceiro.getInscricaoEstadualPessoaCnpj());
            stmt.setString(14, parceiro.getSiteparceiro());
            stmt.executeUpdate();
        
            
        } catch (SQLException e) {
             e.printStackTrace();
           System.out.println("Erro ao listar os parceiros: " + e.getMessage());
        }
    
    }
    /*Lista por id*/
    public Parceiro getParceiroById(int id) throws SQLException {
        Parceiro parceiro = null;
        PostgresConnection conn = new PostgresConnection();
        Connection conexao = conn.getConnection();
    

        try  {
        	 String sql = "SELECT * FROM parceiro INNER JOIN endereco "
        	 		+ "ON parceiro.idendereco = endereco.idendereco WHERE parceiro.idPessoa = ?";
        	  PreparedStatement stmt = conexao.prepareStatement(sql);
        	 stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                // Criar o objeto Parceiro e Endereço a partir do ResultSet
                Endereco endereco = new Endereco();
                endereco.setIdendereco(rs.getInt("idendereco"));
                endereco.setEndereco(rs.getString("endereco"));
                endereco.setCep(rs.getString("cep"));
                endereco.setBairro(rs.getString("bairro"));
                endereco.setCidade(rs.getString("cidade"));
                endereco.setEstado(rs.getString("estado"));
                endereco.setPais(rs.getString("pais"));

                parceiro = new Parceiro(
                    rs.getInt("idPessoa"),
                    rs.getString("nomePessoa"),
                    rs.getString("usuarioPessoa"),
                    rs.getString("senhaPessoa"),
                    rs.getString("nivelPessoa"),
                    rs.getBoolean("situacaoPessoa"),
                    rs.getString("emailPessoa"),
                    rs.getString("telefonePessoa"),
                    endereco,
                    rs.getInt("numero"),
                    rs.getString("complemento"),
                    rs.getString("cnpjPessoaCnpj"),
                    rs.getString("razaoSocialPessoaCnpj"),
                    rs.getString("inscricaoEstadualPessoaCnpj"),
                    rs.getString("siteParceiro")
                );
            }
            }catch (SQLException e) {
                e.printStackTrace();
                System.out.println("Erro ao listar os parceiros: " + e.getMessage());
            } finally {
                if (conexao != null) {
                    conexao.close();
                }
            }
        
        return parceiro;
    }
    /*Listar tudo*/
    public List<Parceiro> listAllParceiro() throws SQLException {
        
        List<Parceiro> parceiroList = new ArrayList<>();
        PostgresConnection conn = new PostgresConnection();
        Connection conexao = conn.getConnection();
        
        try {  
            // Consulta SQL para buscar parceiros com seus respectivos endereços
            String sql = "SELECT * FROM parceiro "
                       + "INNER JOIN endereco ON parceiro.idendereco = endereco.idendereco";
                       
            PreparedStatement stmt = conexao.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                // Construindo o objeto Endereco
                Endereco endereco = new Endereco(
                    rs.getInt("idendereco"),        // ID do endereço
                    rs.getString("endereco"),       // Endereço
                    rs.getString("cep"),            // CEP
                    rs.getString("bairro"),         // Bairro
                    rs.getString("cidade"),         // Cidade
                    rs.getString("estado"),         // Estado
                    rs.getString("pais")            // País
                );
                
                // Construindo o objeto Parceiro
                Parceiro parceiro = new Parceiro(
                    rs.getInt("idpessoa"),                // ID da pessoa
                    rs.getString("nomepessoa"),           // Nome da pessoa
                    rs.getString("usuariopessoa"),        // Usuário da pessoa
                    rs.getString("senhapessoa"),          // Senha da pessoa
                    rs.getString("nivelpessoa"),          // Nível da pessoa
                    rs.getBoolean("situacaopessoa"),      // Situação da pessoa
                    rs.getString("emailpessoa"),          // Email da pessoa
                    rs.getString("telefonepessoa"),       // Telefone da pessoa
                    endereco,                             // Objeto Endereço
                    rs.getInt("numero"),            // Número do endereço
                    rs.getString("complemento"),    // Complemento do endereço
                    rs.getString("cnpjPessoaCnpj"),       // CNPJ do parceiro
                    rs.getString("razaoSocialPessoaCnpj"),// Razão social
                    rs.getString("inscricaoEstadualPessoaCnpj"), // Inscrição estadual
                    rs.getString("siteparceiro")          // Site do parceiro
                );

                parceiroList.add(parceiro);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Erro ao listar os parceiros: " + e.getMessage());
        } finally {
            if (conexao != null) {
                conexao.close();
            }
        }

        return parceiroList;
    }

    public boolean existeCNPJ(String cnpj) throws SQLException {
		 PostgresConnection conn = new PostgresConnection();
		 Connection conexao= conn.getConnection();
		 
	        boolean existe = false;

	        // Substitua pelo seu método de obter conexão com o banco de dados
	        try {
	        	 String sql = "SELECT COUNT(*) FROM parceiro WHERE cnpjpessoacnpj = ?";
	             PreparedStatement stmt = conexao.prepareStatement(sql);
	             // Definindo o valor do parâmetro da consulta
	             stmt.setString(1, cnpj);
	             ResultSet rs = stmt.executeQuery();
	             if (rs.next()) {
	                    // Verificando se a contagem é maior que zero
	                    existe = rs.getInt(1)>0;
	                }


	        } catch (SQLException e) {
	        	e.printStackTrace();
	            System.out.println("Erro no nivel dao: "+e.getMessage()); // Tratar exceções de forma adequada na sua aplicação
	        }
 
	        return existe;
	    }
    public Integer obterIdPArceiroPorCNPJ(String cnpj) throws SQLException {
	    PostgresConnection conn = new PostgresConnection();
	    Connection conexao = conn.getConnection();
	    
	    Integer idpessoa = null;

	    try {
	        // Modificando a consulta SQL para retornar o 'idendereco' ao invés de contar os registros
	        String sql = "SELECT idpessoa FROM parceiro WHERE cnpjpessoacnpj = ?";
	        PreparedStatement stmt = conexao.prepareStatement(sql);
	        
	        // Definindo o valor do parâmetro da consulta
	        stmt.setString(1, cnpj);
	        ResultSet rs = stmt.executeQuery();
	        
	        if (rs.next()) {
	            // Atribuindo o valor de 'idendereco' à variável
	            idpessoa = rs.getInt("idpessoa");
	        }

	    } catch (SQLException e) {
	        e.printStackTrace();
	        System.out.println("Erro no nivel DAO: " + e.getMessage()); // Tratar exceções de forma adequada na sua aplicação
	    } finally {
	        if (conexao != null) {
	            conexao.close(); // Certifique-se de fechar a conexão para evitar vazamento de recursos
	        }
	    }
	    
	  
	    return idpessoa;
	}

    public void updateParceiro(Parceiro parceiro) throws SQLException {
    	  PostgresConnection conn = new PostgresConnection();
  	    Connection conexao = conn.getConnection();
        try  {
                String sql = "UPDATE parceiro SET "+
       "nomepessoa = ?, usuariopessoa = ?, senhapessoa = ?, "+
       "nivelpessoa = ?, situacaopessoa = ?, emailpessoa = ?,"+
       "numero = ?, complemento = ?, idendereco = ?, "+
       "telefonepessoa = ?, cnpjPessoaCnpj = ?, razaosocialpessoacnpj = ?,"+ 
       "inscricaoestadualpessoacnpj = ?, siteparceiro = ? WHERE idpessoa = ?";
                PreparedStatement stmt = conexao.prepareStatement(sql);
            stmt.setString(1, parceiro.getNomePessoa());
            stmt.setString(2, parceiro.getUsuarioPessoa());
            stmt.setString(3, parceiro.getSenhaPessoa());
            
            stmt.setString(4, parceiro.getNivelPessoa());
            stmt.setBoolean(5, parceiro.isSituacaoPessoa());
            stmt.setString(6, parceiro.getEmailPessoa());
            
            stmt.setInt(7, parceiro.getNumero());
            stmt.setString(8, parceiro.getComplemento());
            stmt.setInt(9, parceiro.getEndereco().getIdendereco());
            
            stmt.setString(10, parceiro.getTelefonePessoa());
            stmt.setString(11, parceiro.getCnpjPessoaCnpj());
            stmt.setString(12, parceiro.getRazaoSocialPessoaCnpj());
          
            stmt.setString(13, parceiro.getInscricaoEstadualPessoaCnpj());
            stmt.setString(14, parceiro.getSiteparceiro());
            stmt.setInt(15, parceiro.getIdPessoa());

            stmt.executeUpdate();
        } catch (SQLException e) {
        	e.printStackTrace();
            System.out.println("Erro no nivel dao: "+e.getMessage());
        }
    }

    public void deleteParceiro(int id, Boolean situacao) throws SQLException {
    	  PostgresConnection conn = new PostgresConnection();
    	    Connection conexao = conn.getConnection();
        try  {
        	 String sql = "UPDATE parceiro SET situacaopessoa = ? WHERE idpessoa = ?";
        	 PreparedStatement stmt = conexao.prepareStatement(sql);
        	 System.out.println("passei daao "+situacao);
        	 stmt.setBoolean(1, situacao);
        	 stmt.setInt(2, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
        	e.printStackTrace();
            System.out.println("Erro no nivel dao: "+e.getMessage());
        }
    }
/*
    public Parceiro getParceiroById(int id) {
        String sql = "SELECT * FROM parceiro WHERE idpessoa = ?";
        Parceiro parceiro = null;

        try (Connection conn = PostgresConnection.getConnection(); 
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Endereco endereco = new EnderecoDAO().setEnderecoById(rs.getInt("idendereco"));  // Presumindo que há uma relação com a tabela de endereços

                parceiro = new Parceiro(
                        rs.getInt("idpessoa"),
                        rs.getString("nomepessoa"),
                        endereco,
                        rs.getString("usuariopessoa"),
                        rs.getString("senhapessoa"),
                        rs.getString("nivelpessoa"),
                        rs.getBoolean("situacaopessoa"),
                        rs.getString("emailpessoa"),
                        rs.getString("telefonepessoa"),
                        rs.getString("cnpjPessoaCnpj"),
                        rs.getString("razaoSocialPessoaCnpj"),
                        rs.getString("inscricaoEstadualPessoaCnpj"),
                        rs.getString("siteparceiro")
                );
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar o parceiro: " + e.getMessage(), e);
        }

        return parceiro;
    }

    public List<Parceiro> getAllParceiros() {
        String sql = "SELECT * FROM parceiro";
        List<Parceiro> parceiros = new ArrayList<>();

        try (Connection conn = PostgresConnection.getConnection(); 
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Endereco endereco = new EnderecoDAO().getIdendereco(rs.getInt("idendereco"));  // Presumindo que há uma relação com a tabela de endereços

                Parceiro parceiro = new Parceiro(
                        rs.getInt("idpessoa"),
                        rs.getString("nomepessoa"),
                        endereco,
                        rs.getString("usuariopessoa"),
                        rs.getString("senhapessoa"),
                        rs.getString("nivelpessoa"),
                        rs.getBoolean("situacaopessoa"),
                        rs.getString("emailpessoa"),
                        rs.getString("telefonepessoa"),
                        rs.getString("cnpjPessoaCnpj"),
                        rs.getString("razaoSocialPessoaCnpj"),
                        rs.getString("inscricaoEstadualPessoaCnpj"),
                        rs.getString("siteparceiro")
                );
                parceiros.add(parceiro);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar os parceiros: " + e.getMessage(), e);
        }

        return parceiros;
    }*/
}
