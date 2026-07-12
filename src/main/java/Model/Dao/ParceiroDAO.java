package Model.Dao;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
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
            + "situacaopessoa, emailpessoa,cep ,numero, complemento, "
            + "telefonepessoa, cnpjPessoaCnpj, razaosocialpessoacnpj, "
            + "inscricaoestadualpessoacnpj, siteparceiro, tipo_parceiro) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        	PreparedStatement stmt = conexao.prepareStatement(sql);
           stmt.setString(1, parceiro.getNomePessoa());
            stmt.setString(2, parceiro.getUsuarioPessoa());
            stmt.setString(3, parceiro.getSenhaPessoa());
            stmt.setString(4, parceiro.getNivelPessoa());
            stmt.setBoolean(5, parceiro.isSituacaoPessoa());
            stmt.setString(6, parceiro.getEmailPessoa());
            stmt.setString(7, parceiro.getCep());
            stmt.setInt(8, parceiro.getNumero());
            stmt.setString(9, parceiro.getComplemento());
            stmt.setString(10, parceiro.getTelefonePessoa());
            stmt.setString(11, parceiro.getCnpjPessoaCnpj());
            stmt.setString(12, parceiro.getRazaoSocialPessoaCnpj());
            stmt.setString(13, parceiro.getInscricaoEstadualPessoaCnpj());
            stmt.setString(14, parceiro.getSiteparceiro());
            stmt.setString(15, Model.Model.TipoParceiro.normalizar(parceiro.getTipoParceiro()));
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
        	 String sql = "SELECT * FROM parceiro where idPessoa = ?";
        	  PreparedStatement stmt = conexao.prepareStatement(sql);
        	 stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                // Criar o objeto Parceiro e Endereço a partir do ResultSet
              

                parceiro = new Parceiro(
                    rs.getInt("idPessoa"),
                    rs.getString("nomePessoa"),
                    rs.getString("usuarioPessoa"),
                    rs.getString("senhaPessoa"),
                    rs.getString("nivelPessoa"),
                    rs.getBoolean("situacaoPessoa"),
                    rs.getString("emailPessoa"),
                    rs.getString("telefonePessoa"),
                    rs.getString("cep"),
                    rs.getInt("numero"),
                    rs.getString("complemento"),
                    rs.getString("cnpjPessoaCnpj"),
                    rs.getString("razaoSocialPessoaCnpj"),
                    rs.getString("inscricaoEstadualPessoaCnpj"),
                    rs.getString("siteParceiro")
                );
                parceiro.setTipoParceiro(rs.getString("tipo_parceiro"));
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
            String sql = "SELECT * FROM parceiro ";
                       
            PreparedStatement stmt = conexao.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
              
                
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
                    rs.getString("cep"),                             // Objeto Endereço
                    rs.getInt("numero"),            // Número do endereço
                    rs.getString("complemento"),    // Complemento do endereço
                    rs.getString("cnpjPessoaCnpj"),       // CNPJ do parceiro
                    rs.getString("razaoSocialPessoaCnpj"),// Razão social
                    rs.getString("inscricaoEstadualPessoaCnpj"), // Inscrição estadual
                    rs.getString("siteparceiro")          // Site do parceiro
                );
                parceiro.setTipoParceiro(rs.getString("tipo_parceiro"));

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
    /*Contagenm do total ativos*/
 public int contarPorSituacao(boolean ativo) throws SQLException {
    int numero = 0;

    String sql = "SELECT COUNT(*) FROM parceiro WHERE situacaopessoa = ?";

    try (Connection conexao = new PostgresConnection().getConnection();
         PreparedStatement stmt = conexao.prepareStatement(sql)) {

        stmt.setBoolean(1, ativo);
        ResultSet rs = stmt.executeQuery();

        if (rs.next()) {
            numero = rs.getInt(1); // <- retorna o número corretamente
        }

    } catch (SQLException e) {
        System.err.println("Erro ao contar parceiros por situação: " + e.getMessage());
        throw e; // ou trate conforme sua regra
    }

    return numero;
}
/*Contagem de todo*/
 public int contarTodos() throws SQLException {
    int numero = 0;

    String sql = "SELECT COUNT(*) FROM parceiro";

    try (Connection conexao = new PostgresConnection().getConnection();
         PreparedStatement stmt = conexao.prepareStatement(sql);
         ResultSet rs = stmt.executeQuery()) {

        if (rs.next()) {
            numero = rs.getInt(1);
        }

    } catch (SQLException e) {
        System.err.println("Erro ao contar todos os parceiros: " + e.getMessage());
        throw e;
    }

    return numero;
}
/*Verificação id para perceiro*/
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
       "numero = ?, complemento = ?, cep = ?, "+
       "telefonepessoa = ?, cnpjPessoaCnpj = ?, razaosocialpessoacnpj = ?,"+
       "inscricaoestadualpessoacnpj = ?, siteparceiro = ?, tipo_parceiro = ? WHERE idpessoa = ?";
                PreparedStatement stmt = conexao.prepareStatement(sql);
            stmt.setString(1, parceiro.getNomePessoa());
            stmt.setString(2, parceiro.getUsuarioPessoa());
            stmt.setString(3, parceiro.getSenhaPessoa());
            
            stmt.setString(4, parceiro.getNivelPessoa());
            stmt.setBoolean(5, parceiro.isSituacaoPessoa());
            stmt.setString(6, parceiro.getEmailPessoa());
            
            stmt.setInt(7, parceiro.getNumero());
            stmt.setString(8, parceiro.getComplemento());
            stmt.setString(9, parceiro.getCep());
            
            stmt.setString(10, parceiro.getTelefonePessoa());
            stmt.setString(11, parceiro.getCnpjPessoaCnpj());
            stmt.setString(12, parceiro.getRazaoSocialPessoaCnpj());
          
            stmt.setString(13, parceiro.getInscricaoEstadualPessoaCnpj());
            stmt.setString(14, parceiro.getSiteparceiro());
            stmt.setString(15, Model.Model.TipoParceiro.normalizar(parceiro.getTipoParceiro()));
            stmt.setInt(16, parceiro.getIdPessoa());

            stmt.executeUpdate();
        } catch (SQLException e) {
        	e.printStackTrace();
            System.out.println("Erro no nivel dao: "+e.getMessage());
        }
    }

    /* Lista parceiros por tipo de negócio (PARCEIRO | FORNECEDOR | INSUMO).
       Passar null/"" retorna todos. */
    public List<Parceiro> listarPorTipo(String tipo) throws SQLException {
        List<Parceiro> lista = new ArrayList<>();
        boolean filtrar = tipo != null && !tipo.isBlank();
        String sql = "SELECT * FROM parceiro"
                   + (filtrar ? " WHERE tipo_parceiro = ?" : "")
                   + " ORDER BY nomepessoa";

        try (Connection conexao = new PostgresConnection().getConnection();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {

            if (filtrar) stmt.setString(1, tipo.trim().toUpperCase());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Parceiro parceiro = new Parceiro(
                    rs.getInt("idpessoa"),
                    rs.getString("nomepessoa"),
                    rs.getString("usuariopessoa"),
                    rs.getString("senhapessoa"),
                    rs.getString("nivelpessoa"),
                    rs.getBoolean("situacaopessoa"),
                    rs.getString("emailpessoa"),
                    rs.getString("telefonepessoa"),
                    rs.getString("cep"),
                    rs.getInt("numero"),
                    rs.getString("complemento"),
                    rs.getString("cnpjPessoaCnpj"),
                    rs.getString("razaoSocialPessoaCnpj"),
                    rs.getString("inscricaoEstadualPessoaCnpj"),
                    rs.getString("siteparceiro")
                );
                parceiro.setTipoParceiro(rs.getString("tipo_parceiro"));
                lista.add(parceiro);
            }
        }
        return lista;
    }

    /* Conta parceiros por tipo de negócio. */
    public int contarPorTipo(String tipo) throws SQLException {
        int numero = 0;
        String sql = "SELECT COUNT(*) FROM parceiro WHERE tipo_parceiro = ?";
        try (Connection conexao = new PostgresConnection().getConnection();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setString(1, tipo == null ? "" : tipo.trim().toUpperCase());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) numero = rs.getInt(1);
        }
        return numero;
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
