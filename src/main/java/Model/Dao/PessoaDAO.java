package Model.Dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import Model.Model.Pessoa;
import Util.PostgresConnection;


public class PessoaDAO {

    private static final String SQL_VALIDAR =
        "SELECT idpessoa, nomepessoa, usuariopessoa, nivelpessoa, situacaopessoa " +
        "FROM pessoa WHERE usuariopessoa = ? AND senhapessoa = ?";

    public Pessoa validarUsuario(Pessoa pessoa) throws SQLException {
        try (Connection conexao = new PostgresConnection().getConnection();
             PreparedStatement stmt = conexao.prepareStatement(SQL_VALIDAR)) {

            stmt.setString(1, pessoa.getUsuarioPessoa());
            stmt.setString(2, pessoa.getSenhaPessoa());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Pessoa p = new Pessoa();
                    p.setIdPessoa(rs.getInt("idpessoa"));
                    p.setNomePessoa(rs.getString("nomepessoa"));
                    p.setUsuarioPessoa(rs.getString("usuariopessoa"));
                    p.setNivelPessoa(rs.getString("nivelpessoa"));
                    p.setSituacaoPessoa(rs.getBoolean("situacaopessoa"));
                    return p;
                }
            }
        }
        return null;
    }
}
