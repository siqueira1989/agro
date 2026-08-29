package Model.Dao;

import Model.Model.Alimento;
import Model.Model.AlimentoClassificacao;
import Model.Model.Classificacao;
import Util.PostgresConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Associação entre Alimento (cultura) e Classificação.
 *
 * <h3>Correções da auditoria de 29/08/2026</h3>
 *
 * <p><b>P0-4 — vazamento de conexão.</b> Seis métodos abriam a conexão fora de
 * {@code try-with-resources}. Todos foram convertidos.</p>
 *
 * <p><b>P0-5 — {@code ExisteAssociacaoClassificacao} engolia a exceção</b> e
 * devolvia {@code false}, o que o chamador entendia como "não existe a
 * associação" — abrindo caminho para gravar duplicata.</p>
 *
 * <p><b>Novo achado durante a correção — ID adivinhado.</b> O cadastro de
 * alimento com classificações fazia:</p>
 * <pre>
 * int numero = dao.RetornoIdAlimento();       // SELECT last_value FROM ..._seq
 * alimentodao.addAlimento(alimentoObj);       // INSERT
 * ...setIdproduto(numero + 1);                // "cuidado aqui"
 * </pre>
 * <p>Ou seja: lia o último valor da sequência e <b>chutava</b> que o próximo id
 * seria ele mais um. Isso quebra em dois casos reais. Se dois usuários cadastram
 * ao mesmo tempo, os dois calculam o mesmo número e as classificações de um vão
 * parar no alimento do outro. E se a sequência tiver buracos — qualquer INSERT
 * que falhou já consome um número —, o palpite aponta para um id inexistente e
 * as classificações se perdem ou violam a chave estrangeira. Além disso, o
 * alimento e suas classificações eram gravados sem transação: um erro no meio
 * deixava o alimento cadastrado sem nenhuma classificação.</p>
 *
 * <p>{@link #cadastrarComClassificacoes} substitui tudo isso: um único INSERT
 * com {@code RETURNING idproduto} devolve o id <b>real</b> gerado, e o alimento
 * mais as associações são gravados numa só transação — ou entra tudo, ou não
 * entra nada.</p>
 */
public class AlimentoClassificacaoDao {

    /**
     * Cadastra o alimento e suas classificações atomicamente.
     *
     * @return o id realmente gerado pelo banco para o alimento
     */
    public int cadastrarComClassificacoes(Alimento alimento, List<Integer> idsClassificacao)
            throws SQLException {

        String sqlAlimento = "INSERT INTO alimento (nomeproduto, tipoproduto, situacaoproduto, "
                           + "variedadealimento) VALUES (?, ?, ?, ?) RETURNING idproduto";
        String sqlAssoc = "INSERT INTO alimentoclassificacao (idproduto, idclassificacao) VALUES (?, ?)";

        try (Connection c = new PostgresConnection().getConnection()) {
            boolean autoCommitOriginal = c.getAutoCommit();
            c.setAutoCommit(false);
            try {
                int idProduto;
                try (PreparedStatement st = c.prepareStatement(sqlAlimento)) {
                    st.setString(1, alimento.getNomeproduto());
                    st.setString(2, alimento.getTipoproduto());
                    st.setBoolean(3, alimento.isSituacaoproduto());
                    st.setString(4, alimento.getVariedadealimento());
                    try (ResultSet rs = st.executeQuery()) {
                        if (!rs.next()) throw new SQLException("O banco não devolveu o id do alimento.");
                        idProduto = rs.getInt(1);
                    }
                }

                if (idsClassificacao != null && !idsClassificacao.isEmpty()) {
                    try (PreparedStatement st = c.prepareStatement(sqlAssoc)) {
                        for (Integer idClass : idsClassificacao) {
                            if (idClass == null) continue;
                            st.setInt(1, idProduto);
                            st.setInt(2, idClass);
                            st.addBatch();
                        }
                        st.executeBatch();
                    }
                }

                c.commit();
                alimento.setIdproduto(idProduto);
                return idProduto;

            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(autoCommitOriginal);
            }
        }
    }

    public void addAlimentoClassificacao(AlimentoClassificacao alimentoclassificacao) throws SQLException {
        String sql = "INSERT INTO alimentoclassificacao (idproduto, idclassificacao) VALUES (?, ?)";

        try (Connection conexao = new PostgresConnection().getConnection();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {

            stmt.setInt(1, alimentoclassificacao.getAlimento().getIdproduto());
            stmt.setInt(2, alimentoclassificacao.getClassificacao().getIdclassificacao());
            stmt.executeUpdate();
        }
    }

    /**
     * Último valor da sequência de alimento.
     *
     * @deprecated Nunca use para prever o próximo id: a leitura não é atômica em
     *             relação ao INSERT e a sequência tem buracos. Use
     *             {@link #cadastrarComClassificacoes}, que obtém o id real com
     *             {@code RETURNING}.
     */
    @Deprecated
    public Integer RetornoIdAlimento() throws SQLException {
        String sql = "SELECT last_value AS numero FROM public.produto_idproduto_seq";

        try (Connection conexao = new PostgresConnection().getConnection();
             PreparedStatement stmt = conexao.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            return rs.next() ? rs.getInt("numero") : null;
        }
    }

    /** Classificações já associadas a um alimento. */
    public List<AlimentoClassificacao> ClassificacaoAlimentoBuscaID(int idproduto) throws SQLException {
        String sql = "SELECT a.idproduto, c.idclassificacao, a.nomeproduto, c.classificacao "
                   + "FROM public.alimentoclassificacao ac "
                   + "JOIN public.alimento a ON ac.idproduto = a.idproduto "
                   + "JOIN public.classificacao c ON ac.idclassificacao = c.idclassificacao "
                   + "WHERE a.idproduto = ? "
                   + "ORDER BY a.idproduto, c.idclassificacao";

        List<AlimentoClassificacao> lista = new ArrayList<>();

        try (Connection conexao = new PostgresConnection().getConnection();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {

            stmt.setInt(1, idproduto);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    AlimentoClassificacao ac = novo();
                    ac.getAlimento().setIdproduto(rs.getInt("idproduto"));
                    ac.getAlimento().setNomeproduto(rs.getString("nomeproduto"));
                    ac.getClassificacao().setIdclassificacao(rs.getInt("idclassificacao"));
                    ac.getClassificacao().setClassificacao(rs.getString("classificacao"));
                    lista.add(ac);
                }
            }
        }
        return lista;
    }

    /** Classificações AINDA NÃO associadas ao alimento — alimenta o modal. */
    public List<AlimentoClassificacao> ClassificacaoAlimentoBuscaModalID(int idproduto) throws SQLException {
        String sql = "SELECT c.idclassificacao, c.classificacao FROM classificacao c "
                   + "WHERE NOT EXISTS (SELECT 1 FROM alimentoclassificacao ac "
                   + "                  WHERE ac.idproduto = ? AND ac.idclassificacao = c.idclassificacao) "
                   + "ORDER BY c.classificacao";

        List<AlimentoClassificacao> lista = new ArrayList<>();

        try (Connection conexao = new PostgresConnection().getConnection();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {

            stmt.setInt(1, idproduto);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    AlimentoClassificacao ac = novo();
                    ac.getClassificacao().setIdclassificacao(rs.getInt("idclassificacao"));
                    ac.getClassificacao().setClassificacao(rs.getString("classificacao"));
                    lista.add(ac);
                }
            }
        }
        return lista;
    }

    /** A associação já existe? Antes devolvia false quando o banco falhava. */
    public boolean ExisteAssociacaoClassificacao(AlimentoClassificacao alimentoclassificacao)
            throws SQLException {

        String sql = "SELECT 1 FROM alimentoclassificacao WHERE idproduto = ? "
                   + "AND idclassificacao = ? LIMIT 1";

        try (Connection conexao = new PostgresConnection().getConnection();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {

            stmt.setInt(1, alimentoclassificacao.getAlimento().getIdproduto());
            stmt.setInt(2, alimentoclassificacao.getClassificacao().getIdclassificacao());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private AlimentoClassificacao novo() {
        AlimentoClassificacao ac = new AlimentoClassificacao();
        ac.setAlimento(new Alimento());
        ac.setClassificacao(new Classificacao());
        return ac;
    }
}
