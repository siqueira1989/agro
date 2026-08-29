package Model.Dao;

import Model.Model.DespesasCustos;
import Util.PostgresConnection;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Persistência de Despesas e Custos.
 *
 * <h3>Correções da auditoria de 29/08/2026</h3>
 *
 * <p><b>P0-4 — vazamento de conexão.</b> Era o pior caso do sistema: os sete
 * métodos abriam a conexão e <b>nenhum</b> a fechava. Todos passaram a
 * {@code try-with-resources}.</p>
 *
 * <p><b>P0-5 — gravação silenciosa.</b> {@code create}, {@code update} e
 * {@code delete} capturavam a {@link SQLException} e retornavam normalmente; o
 * servlet respondia "sucesso" sem que nada tivesse sido gravado. As exceções
 * agora sobem.</p>
 *
 * <p><b>Novo achado durante a correção — tabela errada.</b>
 * {@code VerificarDadosUpdate} e {@code ExisteEmOutroRegistroDespesasCustos}
 * consultavam {@code FROM classificacao} — resquício de copiar e colar do
 * {@code ClassificacaoDAO}. Como {@code classificacao} não tem a coluna
 * {@code iddespesascusto}, as duas consultas <b>sempre falhavam</b>. Uma
 * escondia o erro e devolvia {@code false} (o sistema concluía que não havia
 * duplicidade e seguia); a outra estourava erro 500 na tela. Corrigido para
 * {@code FROM despesascusto}.</p>
 */
public class DespesasCustosDAO {

    private static final String COLUNAS =
        "iddespesascusto, despesascusto, unidadedespesascustos, "
      + "valordespesascustos, tipodespesascustos";

    /** Já existe uma despesa com esta descrição? */
    public boolean existeDespesaCusto(String despesascustos) throws SQLException {
        String sql = "SELECT 1 FROM despesascusto WHERE despesascusto = ? LIMIT 1";

        try (Connection conexao = new PostgresConnection().getConnection();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {

            stmt.setString(1, despesascustos);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void create(DespesasCustos despesaCusto) throws SQLException {
        String sql = "INSERT INTO despesascusto (despesascusto, unidadedespesascustos, "
                   + "valordespesascustos, tipodespesascustos) VALUES (?, ?, ?, ?)";

        try (Connection conexao = new PostgresConnection().getConnection();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {

            stmt.setString(1, despesaCusto.getDespesascusto());
            stmt.setString(2, despesaCusto.getUnidadedespesascustos());
            stmt.setBigDecimal(3, valor(despesaCusto));
            stmt.setString(4, despesaCusto.getTipodespesascustos());
            stmt.executeUpdate();
        }
    }

    public DespesasCustos read(int id) throws SQLException {
        String sql = "SELECT " + COLUNAS + " FROM despesascusto WHERE iddespesascusto = ?";

        try (Connection conexao = new PostgresConnection().getConnection();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {

            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public void update(DespesasCustos despesaCusto) throws SQLException {
        String sql = "UPDATE despesascusto SET despesascusto = ?, unidadedespesascustos = ?, "
                   + "valordespesascustos = ?, tipodespesascustos = ? WHERE iddespesascusto = ?";

        try (Connection conexao = new PostgresConnection().getConnection();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {

            stmt.setString(1, despesaCusto.getDespesascusto());
            stmt.setString(2, despesaCusto.getUnidadedespesascustos());
            stmt.setBigDecimal(3, valor(despesaCusto));
            stmt.setString(4, despesaCusto.getTipodespesascustos());
            stmt.setInt(5, despesaCusto.getIddespesascusto());
            stmt.executeUpdate();
        }
    }

    public void delete(DespesasCustos despesaCusto) throws SQLException {
        String sql = "DELETE FROM despesascusto WHERE iddespesascusto = ?";

        try (Connection conexao = new PostgresConnection().getConnection();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {

            stmt.setInt(1, despesaCusto.getIddespesascusto());
            stmt.executeUpdate();
        }
    }

    public List<DespesasCustos> listAll() throws SQLException {
        String sql = "SELECT " + COLUNAS + " FROM despesascusto ORDER BY despesascusto";
        List<DespesasCustos> lista = new ArrayList<>();

        try (Connection conexao = new PostgresConnection().getConnection();
             PreparedStatement stmt = conexao.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    /**
     * O registro enviado é idêntico ao que já está gravado? Evita UPDATE inútil.
     *
     * <p>Consultava {@code FROM classificacao} — tabela errada, sem a coluna
     * {@code iddespesascusto}. A consulta sempre falhava e o {@code catch}
     * devolvia {@code false}, o que o chamador entendia como "mudou alguma
     * coisa". Corrigido para {@code despesascusto}.</p>
     */
    public boolean VerificarDadosUpdate(DespesasCustos despesaCusto) throws SQLException {
        String sql = "SELECT 1 FROM despesascusto WHERE iddespesascusto = ? AND despesascusto = ? "
                   + "AND unidadedespesascustos = ? AND valordespesascustos = ? "
                   + "AND tipodespesascustos = ? LIMIT 1";

        try (Connection conexao = new PostgresConnection().getConnection();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {

            stmt.setInt(1, despesaCusto.getIddespesascusto());
            stmt.setString(2, despesaCusto.getDespesascusto());
            stmt.setString(3, despesaCusto.getUnidadedespesascustos());
            stmt.setBigDecimal(4, valor(despesaCusto));
            stmt.setString(5, despesaCusto.getTipodespesascustos());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Os mesmos dados já pertencem a OUTRO registro? (também consultava a tabela errada) */
    public boolean ExisteEmOutroRegistroDespesasCustos(DespesasCustos despesaCusto) throws SQLException {
        String sql = "SELECT 1 FROM despesascusto WHERE iddespesascusto <> ? AND despesascusto = ? "
                   + "AND unidadedespesascustos = ? AND valordespesascustos = ? "
                   + "AND tipodespesascustos = ? LIMIT 1";

        try (Connection conexao = new PostgresConnection().getConnection();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {

            stmt.setInt(1, despesaCusto.getIddespesascusto());
            stmt.setString(2, despesaCusto.getDespesascusto());
            stmt.setString(3, despesaCusto.getUnidadedespesascustos());
            stmt.setBigDecimal(4, valor(despesaCusto));
            stmt.setString(5, despesaCusto.getTipodespesascustos());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    @SuppressWarnings("deprecation")
    public static String converterParaMoedaBrasileira(Double valor) {
        NumberFormat formatoBrasileiro = NumberFormat.getNumberInstance(new Locale("pt", "BR"));  // Locale.of() so existe a partir do Java 19
        formatoBrasileiro.setMinimumFractionDigits(2);
        formatoBrasileiro.setMaximumFractionDigits(2);
        return formatoBrasileiro.format(valor == null ? 0d : valor);
    }

    private DespesasCustos mapear(ResultSet rs) throws SQLException {
        DespesasCustos d = new DespesasCustos();
        d.setIddespesascusto(rs.getInt("iddespesascusto"));
        d.setDespesascusto(rs.getString("despesascusto"));
        d.setUnidadedespesascustos(rs.getString("unidadedespesascustos"));
        BigDecimal v = rs.getBigDecimal("valordespesascustos");
        d.setValordespesascustos(v != null ? v.doubleValue() : 0d);
        d.setTipodespesascustos(rs.getString("tipodespesascustos"));
        return d;
    }

    /**
     * Converte o valor do modelo (ainda {@code double}) para {@link BigDecimal}
     * com duas casas, que é o tipo da coluna {@code numeric(10,2)}.
     *
     * <p>Dívida remanescente registrada no relatório: o campo do modelo deveria
     * ser {@code BigDecimal} de ponta a ponta. O arredondamento explícito aqui
     * elimina o efeito prático (centavos com dízima binária), mas não a causa.</p>
     */
    private BigDecimal valor(DespesasCustos d) {
        return BigDecimal.valueOf(d.getValordespesascustos())
                         .setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
