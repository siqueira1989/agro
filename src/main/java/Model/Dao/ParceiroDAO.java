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
        // P0-4: a conexao era aberta fora do try e NUNCA fechada.
        // P0-5: a SQLException era engolida e o servlet respondia "sucesso".
        String sql = "INSERT INTO parceiro ("
            + "nomepessoa, usuariopessoa, senhapessoa, nivelpessoa, "
            + "situacaopessoa, emailpessoa,cep ,numero, complemento, "
            + "telefonepessoa, cnpjPessoaCnpj, razaosocialpessoacnpj, "
            + "inscricaoestadualpessoacnpj, siteparceiro, tipo_parceiro) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conexao = new PostgresConnection().getConnection();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {

            stmt.setString(1, parceiro.getNomePessoa());
            stmt.setString(2, parceiro.getUsuarioPessoa());
            stmt.setString(3, Util.SenhaUtil.hashOuVazio(parceiro.getSenhaPessoa()));  // P0-2
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
        }
    }
    /*Lista por id*/
    public Parceiro getParceiroById(int id) throws SQLException {
        Parceiro parceiro = null;
        String sql = "SELECT * FROM parceiro where idPessoa = ?";

        // P0-4: fechava so no finally do metodo; agora try-with-resources cobre
        // tambem o PreparedStatement e o ResultSet.
        try (Connection conexao = new PostgresConnection().getConnection();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                // Criar o objeto Parceiro e Endereço a partir do ResultSet
              

                parceiro = new Parceiro(
                    rs.getInt("idPessoa"),
                    rs.getString("nomePessoa"),
                    rs.getString("usuarioPessoa"),
                    null /* P0-2: a senha nunca sai do banco */,
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
        }
        return parceiro;
    }
    /*Listar tudo*/
    public List<Parceiro> listAllParceiro() throws SQLException {
        
        List<Parceiro> parceiroList = new ArrayList<>();
        String sql = "SELECT * FROM parceiro ORDER BY nomepessoa";

        try (Connection conexao = new PostgresConnection().getConnection();
             PreparedStatement stmt = conexao.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
              
                
                // Construindo o objeto Parceiro
                Parceiro parceiro = new Parceiro(
                    rs.getInt("idpessoa"),                // ID da pessoa
                    rs.getString("nomepessoa"),           // Nome da pessoa
                    rs.getString("usuariopessoa"),        // Usuário da pessoa
                    null /* P0-2: a senha nunca sai do banco */,          // Senha da pessoa
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
        }
        return parceiroList;
    }

    /**
     * O CNPJ ja esta cadastrado?
     *
     * <p>P0-4/P0-5: a conexao nunca era fechada e a excecao era engolida,
     * devolvendo {@code false}. Ou seja, uma falha do banco fazia o sistema
     * concluir que o CNPJ era inedito e seguir para o INSERT, criando parceiro
     * duplicado. Agora a excecao sobe e o cadastro e interrompido.</p>
     */
    public boolean existeCNPJ(String cnpj) throws SQLException {
        String sql = "SELECT 1 FROM parceiro WHERE cnpjpessoacnpj = ? LIMIT 1";

        try (Connection conexao = new PostgresConnection().getConnection();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {

            stmt.setString(1, cnpj);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
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
        String sql = "SELECT idpessoa FROM parceiro WHERE cnpjpessoacnpj = ?";

        try (Connection conexao = new PostgresConnection().getConnection();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {

            stmt.setString(1, cnpj);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt("idpessoa") : null;
            }
        }
    }

    public void updateParceiro(Parceiro parceiro) throws SQLException {
                String sql = "UPDATE parceiro SET "+
       // P0-2: senha em branco no formulario mantem a atual.
       "nomepessoa = ?, usuariopessoa = ?, "+
       "senhapessoa = COALESCE(NULLIF(?, ''), senhapessoa), "+
       "nivelpessoa = ?, situacaopessoa = ?, emailpessoa = ?,"+
       "numero = ?, complemento = ?, cep = ?, "+
       "telefonepessoa = ?, cnpjPessoaCnpj = ?, razaosocialpessoacnpj = ?,"+
       "inscricaoestadualpessoacnpj = ?, siteparceiro = ?, tipo_parceiro = ? WHERE idpessoa = ?";

        try (Connection conexao = new PostgresConnection().getConnection();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {

            stmt.setString(1, parceiro.getNomePessoa());
            stmt.setString(2, parceiro.getUsuarioPessoa());
            stmt.setString(3, Util.SenhaUtil.hashOuVazio(parceiro.getSenhaPessoa()));  // P0-2
            
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
                    null /* P0-2: a senha nunca sai do banco */,
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

    /** Desativa o parceiro (exclusao logica). */
    public void deleteParceiro(int id, Boolean situacao) throws SQLException {
        String sql = "UPDATE parceiro SET situacaopessoa = ? WHERE idpessoa = ?";

        try (Connection conexao = new PostgresConnection().getConnection();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {

            stmt.setBoolean(1, situacao != null && situacao);
            stmt.setInt(2, id);
            stmt.executeUpdate();
        }
    }

    /*
     * Removidas nesta revisao duas versoes antigas de getParceiroById e
     * getAllParceiros que estavam comentadas no fim do arquivo (cerca de 70
     * linhas). Referenciavam EnderecoDAO/Endereco, que nao existem mais, e um
     * PostgresConnection.getConnection() estatico que tambem nao existe.
     * Codigo morto so atrapalha a leitura — o historico esta no Git.
     */
}
