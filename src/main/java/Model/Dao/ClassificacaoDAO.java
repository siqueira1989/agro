package Model.Dao;

import Model.Model.Classificacao;
import Util.PostgresConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Persistência de Classificação (padrão de qualidade do alimento).
 *
 * <h3>Correções da auditoria de 29/08/2026</h3>
 *
 * <p><b>P0-4 — vazamento de conexão.</b> Sete métodos abriam a conexão fora de
 * {@code try-with-resources}; cinco nunca a fechavam. Como não havia pool, cada
 * chamada dessas prendia um processo do PostgreSQL até o Tomcat reiniciar.</p>
 *
 * <p><b>P0-5 — gravação silenciosa.</b> {@code saveClassificacao},
 * {@code updateClassificacao} e {@code deleteClassificacao} declaravam
 * {@code throws SQLException} mas capturavam tudo e retornavam normalmente: o
 * servlet respondia "Cadastrado com sucesso!" sem que nada tivesse sido
 * gravado. {@code DadoAtualClassificacao} tinha até um {@code catch} vazio.
 * Todos agora propagam a exceção.</p>
 *
 * <p>As três verificações de duplicidade usavam {@code SELECT COUNT(*)} onde
 * bastava saber se existe alguma linha; passaram a {@code SELECT 1 ... LIMIT 1},
 * que o banco resolve parando no primeiro acerto.</p>
 */
public class ClassificacaoDAO {

    public List<Classificacao> listAll() throws SQLException {
        String sql = "SELECT idclassificacao, classificacao FROM classificacao ORDER BY classificacao";
        List<Classificacao> classificacoes = new ArrayList<>();

        try (Connection conexao = new PostgresConnection().getConnection();
             PreparedStatement stmt = conexao.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Classificacao c = new Classificacao();
                c.setIdclassificacao(rs.getInt("idclassificacao"));
                c.setClassificacao(rs.getString("classificacao"));
                classificacoes.add(c);
            }
        }
        return classificacoes;
    }

    public void saveClassificacao(Classificacao classificacao) throws SQLException {
        String sql = "INSERT INTO classificacao (classificacao) VALUES (?)";

        try (Connection conexao = new PostgresConnection().getConnection();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {

            stmt.setString(1, classificacao.getClassificacao());
            stmt.executeUpdate();
        }
    }

    public void updateClassificacao(Classificacao classificacao) throws SQLException {
        String sql = "UPDATE classificacao SET classificacao = ? WHERE idclassificacao = ?";

        try (Connection conexao = new PostgresConnection().getConnection();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {

            stmt.setString(1, classificacao.getClassificacao());
            stmt.setInt(2, classificacao.getIdclassificacao());
            stmt.executeUpdate();
        }
    }

    public void deleteClassificacao(Classificacao classificacao) throws SQLException {
        String sql = "DELETE FROM classificacao WHERE idclassificacao = ?";

        try (Connection conexao = new PostgresConnection().getConnection();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {

            stmt.setInt(1, classificacao.getIdclassificacao());
            stmt.executeUpdate();
        }
    }

    /** Já existe uma classificação com este nome? */
    public boolean VerificacaoClassificacao(String classificacao) throws SQLException {
        String sql = "SELECT 1 FROM classificacao WHERE classificacao = ? LIMIT 1";

        try (Connection conexao = new PostgresConnection().getConnection();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {

            stmt.setString(1, classificacao);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** A classificação está em uso por algum alimento? Bloqueia a exclusão. */
    public boolean VerificacaoClassificacaoAlimento(int idclassificacao) throws SQLException {
        String sql = "SELECT 1 FROM alimentoclassificacao WHERE idclassificacao = ? LIMIT 1";

        try (Connection conexao = new PostgresConnection().getConnection();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {

            stmt.setInt(1, idclassificacao);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** O valor enviado é igual ao que já está gravado? Evita UPDATE inútil. */
    public boolean DadoAtualClassificacao(int id, String classificacao) throws SQLException {
        String sql = "SELECT 1 FROM classificacao WHERE idclassificacao = ? AND classificacao = ? LIMIT 1";

        try (Connection conexao = new PostgresConnection().getConnection();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.setString(2, classificacao);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** O nome já pertence a OUTRO registro? Valida a alteração. */
    public boolean existeEmOutroRegistroClassificacao(int id, String classificacao) throws SQLException {
        String sql = "SELECT 1 FROM classificacao WHERE classificacao = ? AND idclassificacao <> ? LIMIT 1";

        try (Connection conexao = new PostgresConnection().getConnection();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {

            stmt.setString(1, classificacao);
            stmt.setInt(2, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }
}
