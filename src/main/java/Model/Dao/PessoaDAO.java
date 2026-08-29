package Model.Dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import Model.Model.Pessoa;
import Util.LogUtil;
import Util.PostgresConnection;
import Util.SenhaUtil;

/**
 * Autenticação de usuário.
 *
 * <p><b>O que mudou (P0-2 e P1-16 da auditoria de 29/08/2026).</b></p>
 *
 * <p>A consulta antiga era:</p>
 * <pre>WHERE usuariopessoa = ? AND senhapessoa = ?</pre>
 * <p>Ou seja, a senha em texto puro ia no {@code WHERE} e era comparada pelo
 * banco. Isso obrigava a guardá-la legível e ainda fazia a comparação em tempo
 * variável.</p>
 *
 * <p>Agora a consulta busca <b>só pelo usuário</b>, traz o hash e a conferência
 * acontece em {@link SenhaUtil#verificar}, com PBKDF2 e comparação em tempo
 * constante. Com o índice único criado na V20, essa busca deixou de varrer as
 * nove tabelas da hierarquia de herança.</p>
 */
public class PessoaDAO {

    private static final String SQL_BUSCAR_POR_USUARIO =
        "SELECT idpessoa, nomepessoa, usuariopessoa, senhapessoa, nivelpessoa, situacaopessoa " +
        "FROM pessoa WHERE usuariopessoa = ?";

    /**
     * Valida as credenciais.
     *
     * @return a pessoa autenticada, ou {@code null} se usuário ou senha não
     *         conferirem. A distinção entre "usuário inexistente" e "senha
     *         errada" é deliberadamente não exposta, para não permitir
     *         descobrir quais logins existem.
     */
    public Pessoa validarUsuario(Pessoa credenciais) throws SQLException {
        try (Connection conexao = new PostgresConnection().getConnection();
             PreparedStatement stmt = conexao.prepareStatement(SQL_BUSCAR_POR_USUARIO)) {

            stmt.setString(1, credenciais.getUsuarioPessoa());

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    // Mesmo sem encontrar o usuário, gastamos o tempo de um
                    // hash: sem isso, a resposta instantânea denunciaria que o
                    // login não existe (enumeração de usuários).
                    SenhaUtil.verificar(credenciais.getSenhaPessoa(), SenhaUtil.gerarHash("nao-existe"));
                    return null;
                }

                String armazenada = rs.getString("senhapessoa");
                if (!SenhaUtil.verificar(credenciais.getSenhaPessoa(), armazenada)) {
                    LogUtil.aviso(PessoaDAO.class,
                            "Tentativa de login inválida para o usuário informado.", null);
                    return null;
                }

                Pessoa p = new Pessoa();
                p.setIdPessoa(rs.getInt("idpessoa"));
                p.setNomePessoa(rs.getString("nomepessoa"));
                p.setUsuarioPessoa(rs.getString("usuariopessoa"));
                p.setNivelPessoa(rs.getString("nivelpessoa"));
                p.setSituacaoPessoa(rs.getBoolean("situacaopessoa"));
                // A senha NAO e copiada para o objeto de sessao.
                return p;
            }
        }
    }
}
