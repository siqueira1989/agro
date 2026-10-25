package Model.Dao;

import Model.Model.DespesasCustos;
import Util.PostgresConnection;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

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
        "d.iddespesascusto, d.despesascusto, d.unidadedespesascustos, "
      + "d.valordespesascustos, d.tipodespesascustos, d.classificacao, "
      + "d.categoria, d.id_alimento, d.id_area_producao, d.tipo_caixa, "
      + "a.nomeproduto AS nome_alimento, ap.propriedadeareaproducao AS nome_area, "
      + "ap.siglasareaproducao AS sigla_area";

    /** FROM com os JOINs de exibição (nome do alimento e da área). */
    private static final String FROM =
        " FROM despesascusto d"
      + " LEFT JOIN alimento a ON a.idproduto = d.id_alimento"
      + " LEFT JOIN areaproducao ap ON ap.idareaproducao = d.id_area_producao";

    /** Código de caixa/embalagem: maiúsculas, dígitos e '_' (ex.: CONTENTOR, PLASTICO_20). */
    private static final Pattern TIPO_CAIXA = Pattern.compile("^[A-Z0-9_]{1,30}$");

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
                   + "valordespesascustos, tipodespesascustos, classificacao, "
                   + "categoria, id_alimento, id_area_producao, tipo_caixa) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conexao = new PostgresConnection().getConnection();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {

            String categoria = despesaCusto.getCategoria() == null
                    ? DespesasCustos.CATEGORIA_GERAL : despesaCusto.getCategoria();
            boolean geral = DespesasCustos.CATEGORIA_GERAL.equals(categoria);
            stmt.setString(1, despesaCusto.getDespesascusto());
            stmt.setString(2, despesaCusto.getUnidadedespesascustos());
            stmt.setBigDecimal(3, valor(despesaCusto));
            stmt.setString(4, despesaCusto.getTipodespesascustos());
            stmt.setString(5, despesaCusto.getClassificacao());
            stmt.setString(6, categoria);
            stmt.setObject(7, geral ? null : despesaCusto.getIdAlimento(), Types.INTEGER);
            stmt.setObject(8, geral ? null : despesaCusto.getIdAreaProducao(), Types.INTEGER);
            stmt.setString(9, geral ? null : normalizarTipoCaixa(despesaCusto.getTipoCaixa()));
            stmt.executeUpdate();
        }
    }

    public DespesasCustos read(int id) throws SQLException {
        String sql = "SELECT " + COLUNAS + FROM + " WHERE d.iddespesascusto = ?";

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
                   + "valordespesascustos = ?, tipodespesascustos = ?, classificacao = ? WHERE iddespesascusto = ?";

        try (Connection conexao = new PostgresConnection().getConnection();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {

            stmt.setString(1, despesaCusto.getDespesascusto());
            stmt.setString(2, despesaCusto.getUnidadedespesascustos());
            stmt.setBigDecimal(3, valor(despesaCusto));
            stmt.setString(4, despesaCusto.getTipodespesascustos());
            stmt.setString(5, despesaCusto.getClassificacao());
            stmt.setInt(6, despesaCusto.getIddespesascusto());
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
        String sql = "SELECT " + COLUNAS + FROM + " ORDER BY d.despesascusto";
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
                   + "AND tipodespesascustos = ? AND classificacao = ? LIMIT 1";

        try (Connection conexao = new PostgresConnection().getConnection();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {

            stmt.setInt(1, despesaCusto.getIddespesascusto());
            stmt.setString(2, despesaCusto.getDespesascusto());
            stmt.setString(3, despesaCusto.getUnidadedespesascustos());
            stmt.setBigDecimal(4, valor(despesaCusto));
            stmt.setString(5, despesaCusto.getTipodespesascustos());
            stmt.setString(6, despesaCusto.getClassificacao());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Os mesmos dados já pertencem a OUTRO registro? (também consultava a tabela errada) */
    public boolean ExisteEmOutroRegistroDespesasCustos(DespesasCustos despesaCusto) throws SQLException {
        String sql = "SELECT 1 FROM despesascusto WHERE iddespesascusto <> ? AND despesascusto = ? "
                   + "AND unidadedespesascustos = ? AND valordespesascustos = ? "
                   + "AND tipodespesascustos = ? AND classificacao = ? LIMIT 1";

        try (Connection conexao = new PostgresConnection().getConnection();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {

            stmt.setInt(1, despesaCusto.getIddespesascusto());
            stmt.setString(2, despesaCusto.getDespesascusto());
            stmt.setString(3, despesaCusto.getUnidadedespesascustos());
            stmt.setBigDecimal(4, valor(despesaCusto));
            stmt.setString(5, despesaCusto.getTipodespesascustos());
            stmt.setString(6, despesaCusto.getClassificacao());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /* ============================================================
       V28 — Preço de caixa (colheita) e de embalagem (processamento),
       por alimento e área. Contrato usado pelo Diário de Campo e pela
       Colheita e Estoque: buscar → se null, pedir o valor ao usuário →
       registrar (upsert). As versões com Connection não abrem, não
       comitam e não fecham a conexão: rodam na transação do chamador.
       Regras inválidas (categoria, código, valor, alimento/área
       inexistentes) sobem como IllegalArgumentException com mensagem
       pronta para o usuário.
       ============================================================ */

    /** Preço cadastrado para (categoria, alimento, área, tipo de caixa), ou {@code null}. */
    public DespesasCustos buscarPrecoCaixa(String categoria, int idAlimento, int idArea, String tipoCaixa) throws SQLException {
        try (Connection c = new PostgresConnection().getConnection()) {
            return buscarPrecoCaixa(c, categoria, idAlimento, idArea, tipoCaixa);
        }
    }

    public DespesasCustos buscarPrecoCaixa(Connection c, String categoria, int idAlimento, int idArea, String tipoCaixa) throws SQLException {
        String cat = validarCategoriaCaixa(categoria);
        String tipo = tipoCaixaPadrao(cat, tipoCaixa);
        String sql = "SELECT " + COLUNAS + FROM
                   + " WHERE d.categoria = ? AND d.id_alimento = ? AND d.id_area_producao = ? AND d.tipo_caixa = ?";
        try (PreparedStatement stmt = c.prepareStatement(sql)) {
            stmt.setString(1, cat);
            stmt.setInt(2, idAlimento);
            stmt.setInt(3, idArea);
            stmt.setString(4, tipo);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    /**
     * Insere o preço ou, se já existir para a mesma chave, atualiza o valor.
     * {@code descricao} vazia gera uma automática (ex.: "Caixa Contentor - CAQUI RAMA FORTE - SS")
     * e, num registro já existente, preserva a descrição atual.
     * Grava unidade "un", classificação VARIAVEL. Devolve o {@code iddespesascusto}.
     */
    public int registrarPrecoCaixa(String categoria, int idAlimento, int idArea, String tipoCaixa,
                                   String descricao, BigDecimal valor) throws SQLException {
        try (Connection c = new PostgresConnection().getConnection()) {
            return registrarPrecoCaixa(c, categoria, idAlimento, idArea, tipoCaixa, descricao, valor);
        }
    }

    public int registrarPrecoCaixa(Connection c, String categoria, int idAlimento, int idArea, String tipoCaixa,
                                   String descricao, BigDecimal valor) throws SQLException {
        String cat = validarCategoriaCaixa(categoria);
        String tipo = tipoCaixaPadrao(cat, tipoCaixa);
        if (valor == null || valor.signum() <= 0 || valor.compareTo(new BigDecimal("99999999.99")) > 0) {
            throw new IllegalArgumentException("Informe um valor maior que zero para a caixa/embalagem.");
        }
        String nomeAlimento = nome(c, "SELECT nomeproduto FROM alimento WHERE idproduto = ?", idAlimento);
        if (nomeAlimento == null) throw new IllegalArgumentException("Alimento não encontrado.");
        String siglaArea = nome(c, "SELECT siglasareaproducao FROM areaproducao WHERE idareaproducao = ?", idArea);
        if (siglaArea == null) throw new IllegalArgumentException("Área de produção não encontrada.");

        boolean descricaoInformada = descricao != null && !descricao.trim().isEmpty();
        String desc = descricaoInformada ? descricao.trim()
                    : rotuloTipoCaixa(tipo) + " - " + nomeAlimento.trim() + " - " + siglaArea.trim();
        if (desc.length() > 50) desc = desc.substring(0, 50);

        String sql = "INSERT INTO despesascusto (despesascusto, unidadedespesascustos, valordespesascustos, "
                   + "tipodespesascustos, classificacao, categoria, id_alimento, id_area_producao, tipo_caixa) "
                   + "VALUES (?, 'un', ?, ?, 'VARIAVEL', ?, ?, ?, ?) "
                   + "ON CONFLICT (categoria, id_alimento, id_area_producao, tipo_caixa) WHERE categoria <> 'GERAL' "
                   + "DO UPDATE SET valordespesascustos = EXCLUDED.valordespesascustos, "
                   + "despesascusto = CASE WHEN ? THEN EXCLUDED.despesascusto ELSE despesascusto.despesascusto END "
                   + "RETURNING iddespesascusto";
        try (PreparedStatement stmt = c.prepareStatement(sql)) {
            stmt.setString(1, desc);
            stmt.setBigDecimal(2, valor.setScale(2, java.math.RoundingMode.HALF_UP));
            stmt.setString(3, DespesasCustos.CATEGORIA_CAIXA_COLHEITA.equals(cat) ? "Caixa colheita" : "Embalagem");
            stmt.setString(4, cat);
            stmt.setInt(5, idAlimento);
            stmt.setInt(6, idArea);
            stmt.setString(7, tipo);
            stmt.setBoolean(8, descricaoInformada);
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /** Embalagens (categoria EMBALAGEM) cadastradas para o alimento/área, por código. */
    public List<DespesasCustos> listarEmbalagens(int idAlimento, int idArea) throws SQLException {
        try (Connection c = new PostgresConnection().getConnection()) {
            return listarEmbalagens(c, idAlimento, idArea);
        }
    }

    public List<DespesasCustos> listarEmbalagens(Connection c, int idAlimento, int idArea) throws SQLException {
        String sql = "SELECT " + COLUNAS + FROM
                   + " WHERE d.categoria = 'EMBALAGEM' AND d.id_alimento = ? AND d.id_area_producao = ?"
                   + " ORDER BY d.tipo_caixa";
        List<DespesasCustos> lista = new ArrayList<>();
        try (PreparedStatement stmt = c.prepareStatement(sql)) {
            stmt.setInt(1, idAlimento);
            stmt.setInt(2, idArea);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        }
        return lista;
    }

    /**
     * Normaliza o código digitado: sem acentos, maiúsculas, espaços/hífens viram '_'
     * ("Plástico 20" → "PLASTICO_20"). {@code null}/vazio → {@code null}.
     * Código fora de {@code [A-Z0-9_]{1,30}} → IllegalArgumentException.
     */
    public static String normalizarTipoCaixa(String tipoCaixa) {
        if (tipoCaixa == null || tipoCaixa.trim().isEmpty()) return null;
        String t = Normalizer.normalize(tipoCaixa.trim(), Normalizer.Form.NFD)
                             .replaceAll("\\p{M}", "")
                             .toUpperCase(Locale.ROOT)
                             .replaceAll("[\\s\\-]+", "_");
        if (!TIPO_CAIXA.matcher(t).matches()) {
            throw new IllegalArgumentException("Tipo de caixa inválido: use letras, números e '_' (até 30), ex.: PLASTICO_20.");
        }
        return t;
    }

    private static String validarCategoriaCaixa(String categoria) {
        String cat = categoria == null ? "" : categoria.trim().toUpperCase(Locale.ROOT);
        if (!DespesasCustos.CATEGORIA_CAIXA_COLHEITA.equals(cat) && !DespesasCustos.CATEGORIA_EMBALAGEM.equals(cat)) {
            throw new IllegalArgumentException("Categoria deve ser CAIXA_COLHEITA ou EMBALAGEM.");
        }
        return cat;
    }

    /** CAIXA_COLHEITA sem código assume CONTENTOR; EMBALAGEM exige o código. */
    private static String tipoCaixaPadrao(String categoria, String tipoCaixa) {
        String t = normalizarTipoCaixa(tipoCaixa);
        if (t == null && DespesasCustos.CATEGORIA_CAIXA_COLHEITA.equals(categoria)) return Model.Model.TipoCaixa.CONTENTOR.name();
        if (t == null) throw new IllegalArgumentException("Informe o tipo de embalagem.");
        return t;
    }

    /** Rótulo do enum TipoCaixa quando o código é conhecido; senão o próprio código legível. */
    private static String rotuloTipoCaixa(String tipo) {
        if (Model.Model.TipoCaixa.isValido(tipo)) return Model.Model.TipoCaixa.valueOf(tipo).getRotulo();
        return "Embalagem " + tipo.replace('_', ' ');
    }

    private static String nome(Connection c, String sql, int id) throws SQLException {
        try (PreparedStatement stmt = c.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
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
        d.setClassificacao(rs.getString("classificacao"));
        d.setCategoria(rs.getString("categoria"));
        d.setIdAlimento((Integer) rs.getObject("id_alimento"));
        d.setIdAreaProducao((Integer) rs.getObject("id_area_producao"));
        d.setTipoCaixa(rs.getString("tipo_caixa"));
        d.setNomeAlimento(rs.getString("nome_alimento"));
        d.setNomeArea(rs.getString("nome_area"));
        d.setSiglaArea(rs.getString("sigla_area"));
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
