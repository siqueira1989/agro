package Model.Dao;

import Model.Model.CategoriaInsumo;
import Model.Model.Embalagem;
import Util.PostgresConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Cadastro de embalagens (módulo Embalagem, V31 + V34). Por alimento + área de produção, com o preço
 * pago ao embalador (Processar da Colheita). Desde a V34 cada embalagem tem um INSUMO da categoria
 * EMBALAGEM (unidade CX) com o estoque: saldo, preço médio ponderado (atualizado nas ENTRADAS do
 * EstoqueMovimentoDAO), estoque mínimo e fornecedor. Este DAO cria e mantém esse insumo junto com a
 * embalagem, na mesma transação. Não há exclusão: embalagem usada num processamento/romaneio fica
 * referenciada — só desativa (e o insumo junto).
 * Regras violadas ⇒ IllegalArgumentException com mensagem para o usuário; SQLException propaga.
 */
public class EmbalagemDAO {

    private static final String SELECT =
            "SELECT e.*, al.nomeproduto AS nome_alimento, ar.propriedadeareaproducao AS nome_area, "
          + "ar.siglasareaproducao AS sigla_area, i.quantidade_disponivel, i.preco_medio, i.estoque_minimo, "
          + "i.id_fornecedor, p.nomepessoa AS fornecedor_nome, "
          + "COALESCE(i.quantidade_disponivel < i.estoque_minimo, false) AS abaixo_minimo FROM embalagem e "
          + "JOIN alimento al ON al.idproduto = e.id_alimento "
          + "JOIN areaproducao ar ON ar.idareaproducao = e.id_area_producao "
          + "LEFT JOIN insumo i ON i.idinsumo = e.id_insumo "
          + "LEFT JOIN parceiro p ON p.idpessoa = i.id_fornecedor";

    /** Lista com filtros opcionais (null = todos). Ordem: alimento, área, peso, descrição. */
    public List<Embalagem> listar(Integer idAlimento, Integer idArea, Boolean ativo) throws SQLException {
        try (Connection c = new PostgresConnection().getConnection()) {
            return listar(c, idAlimento, idArea, ativo);
        }
    }

    public List<Embalagem> listar(Connection c, Integer idAlimento, Integer idArea, Boolean ativo) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT + " WHERE 1=1");
        List<Object> ps = new ArrayList<>();
        if (idAlimento != null) { sql.append(" AND e.id_alimento=?"); ps.add(idAlimento); }
        if (idArea != null) { sql.append(" AND e.id_area_producao=?"); ps.add(idArea); }
        if (ativo != null) { sql.append(" AND e.ativo=?"); ps.add(ativo); }
        sql.append(" ORDER BY al.nomeproduto, ar.propriedadeareaproducao, e.peso_kg, e.descricao");
        List<Embalagem> out = new ArrayList<>();
        try (PreparedStatement st = c.prepareStatement(sql.toString())) {
            for (int k = 0; k < ps.size(); k++) st.setObject(k + 1, ps.get(k));
            try (ResultSet rs = st.executeQuery()) { while (rs.next()) out.add(mapear(rs)); }
        }
        return out;
    }

    public Embalagem buscar(int id) throws SQLException {
        try (Connection c = new PostgresConnection().getConnection()) {
            return buscar(c, id);
        }
    }

    public Embalagem buscar(Connection c, int id) throws SQLException {
        try (PreparedStatement st = c.prepareStatement(SELECT + " WHERE e.idembalagem=?")) {
            st.setInt(1, id);
            try (ResultSet rs = st.executeQuery()) { return rs.next() ? mapear(rs) : null; }
        }
    }

    /**
     * Insere (idEmbalagem = 0) ou atualiza, numa transação com o insumo ligado. O código vazio é gerado
     * da descrição ("Papelão caixa 6,5" → PAPELAO_CAIXA_6_5). Código repetido no mesmo alimento + área
     * é recusado. Saldo e preço médio NÃO são editados aqui (só pelas movimentações de estoque).
     * Devolve o registro gravado.
     */
    public Embalagem salvar(Embalagem e) throws SQLException {
        try (Connection c = new PostgresConnection().getConnection()) {
            c.setAutoCommit(false);
            try {
                Embalagem gravada = salvar(c, e);
                c.commit();
                return gravada;
            } catch (SQLException | RuntimeException ex) { c.rollback(); throw ex; }
            finally { c.setAutoCommit(true); }
        }
    }

    /** Mesmo que {@link #salvar(Embalagem)}, na transação do chamador (não faz commit/rollback). */
    public Embalagem salvar(Connection c, Embalagem e) throws SQLException {
        String[] nomes = validar(c, e);   // [alimento, sigla da área]
        try (PreparedStatement st = c.prepareStatement(
                "SELECT idembalagem FROM embalagem WHERE codigo=? AND id_alimento=? AND id_area_producao=? AND idembalagem<>?")) {
            st.setString(1, e.getCodigo()); st.setInt(2, e.getIdAlimento()); st.setInt(3, e.getIdArea());
            st.setInt(4, e.getIdEmbalagem());
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) throw new IllegalArgumentException(
                        "Já existe a embalagem " + e.getCodigo() + " para este alimento e área.");
            }
        }
        String nomeInsumo = nomeInsumo(e.getDescricao(), nomes[0], nomes[1]);
        int id = e.getIdEmbalagem();
        if (id == 0) {
            int idInsumo = inserirInsumo(c, e, nomeInsumo);
            try (PreparedStatement st = c.prepareStatement(
                    "INSERT INTO embalagem (codigo, descricao, peso_kg, id_alimento, id_area_producao, "
                  + "preco_embalador, ativo, id_insumo) VALUES (?,?,?,?,?,?,?,?) RETURNING idembalagem")) {
                preencher(st, e);
                st.setInt(8, idInsumo);
                try (ResultSet rs = st.executeQuery()) { rs.next(); id = rs.getInt(1); }
            }
        } else {
            Embalagem atual = buscar(c, id);
            if (atual == null) throw new IllegalArgumentException("Embalagem não encontrada.");
            Integer idInsumo = atual.getIdInsumo();
            if (idInsumo == null) idInsumo = inserirInsumo(c, e, nomeInsumo);   // embalagem anterior à V34
            else atualizarInsumo(c, idInsumo, e, nomeInsumo);
            try (PreparedStatement st = c.prepareStatement(
                    "UPDATE embalagem SET codigo=?, descricao=?, peso_kg=?, id_alimento=?, id_area_producao=?, "
                  + "preco_embalador=?, ativo=?, id_insumo=?, atualizado_em=now() WHERE idembalagem=?")) {
                preencher(st, e);
                st.setInt(8, idInsumo);
                st.setInt(9, id);
                st.executeUpdate();
            }
        }
        return buscar(c, id);
    }

    /** Ativa/desativa a embalagem e o insumo ligado (a embalagem some/volta no Processar e no Estoque). */
    public void definirAtivo(int id, boolean ativo) throws SQLException {
        try (Connection c = new PostgresConnection().getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement st = c.prepareStatement(
                        "UPDATE embalagem SET ativo=?, atualizado_em=now() WHERE idembalagem=?")) {
                    st.setBoolean(1, ativo); st.setInt(2, id);
                    if (st.executeUpdate() == 0) throw new IllegalArgumentException("Embalagem não encontrada.");
                }
                try (PreparedStatement st = c.prepareStatement(
                        "UPDATE insumo SET situacao=? WHERE idinsumo=(SELECT id_insumo FROM embalagem WHERE idembalagem=?)")) {
                    st.setBoolean(1, ativo); st.setInt(2, id); st.executeUpdate();
                }
                c.commit();
            } catch (SQLException | RuntimeException ex) { c.rollback(); throw ex; }
            finally { c.setAutoCommit(true); }
        }
    }

    /**
     * Código da embalagem: sem acento, maiúsculas, QUALQUER caractere fora de [A-Z0-9] vira "_",
     * "_" repetidos viram um só, sem "_" nas pontas, até 30 caracteres.
     * Ex.: "PAPELAO_6.5" → PAPELAO_6_5; "Papelao caixa 6,5" → PAPELAO_CAIXA_6_5.
     * (Própria do módulo Embalagem; a normalização do tipo de caixa da Despesas não muda.)
     */
    public static String normalizarCodigo(String texto) {
        String t = texto == null ? "" : Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        if (t.length() > 30) t = t.substring(0, 30).replaceAll("_+$", "");
        if (t.isEmpty()) {
            throw new IllegalArgumentException("Código da embalagem inválido: use letras e números (ex.: PAPELAO_6_5).");
        }
        return t;
    }

    /* --------------------------- helpers --------------------------- */

    /** Valida os campos na ordem do formulário e devolve [nome do alimento, sigla da área]. */
    private String[] validar(Connection c, Embalagem e) throws SQLException {
        if (e.getIdAlimento() == null || e.getIdArea() == null) throw new IllegalArgumentException("Escolha o alimento e a área de produção.");
        if (e.getDescricao() == null || e.getDescricao().isBlank()) throw new IllegalArgumentException("Informe a descrição da embalagem.");
        e.setDescricao(e.getDescricao().trim());
        if (e.getDescricao().length() > 80) throw new IllegalArgumentException("Descrição com no máximo 80 caracteres.");
        positivo(e.getPesoKg(), "o peso (kg)");
        positivo(e.getPrecoEmbalador(), "o preço pago ao embalador");
        if (e.getEstoqueMinimo() == null) e.setEstoqueMinimo(BigDecimal.ZERO);
        if (e.getEstoqueMinimo().signum() < 0) throw new IllegalArgumentException("O estoque mínimo não pode ser negativo.");
        String cod = e.getCodigo() == null || e.getCodigo().isBlank() ? e.getDescricao() : e.getCodigo();
        e.setCodigo(normalizarCodigo(cod));
        String alimento = nome(c, "SELECT nomeproduto FROM alimento WHERE idproduto=?", e.getIdAlimento());
        if (alimento == null) throw new IllegalArgumentException("Alimento não encontrado.");
        String sigla = nome(c, "SELECT COALESCE(siglasareaproducao, propriedadeareaproducao) FROM areaproducao WHERE idareaproducao=?", e.getIdArea());
        if (sigla == null) throw new IllegalArgumentException("Área de produção não encontrada.");
        if (e.getIdFornecedor() != null
                && nome(c, "SELECT tipo_parceiro FROM parceiro WHERE idpessoa=? AND upper(tipo_parceiro)='INSUMO'", e.getIdFornecedor()) == null) {
            throw new IllegalArgumentException("Fornecedor inválido: deve ser um parceiro do tipo Insumo.");
        }
        return new String[]{ alimento, sigla };
    }

    /** Nome do insumo no Estoque: "Papelão 7kg (CAQUI RAMA FORTE · SS)", até 80 caracteres. */
    private static String nomeInsumo(String descricao, String alimento, String sigla) {
        String n = descricao + " (" + alimento + " · " + sigla + ")";
        return n.length() > 80 ? n.substring(0, 80) : n;
    }

    private static int inserirInsumo(Connection c, Embalagem e, String nome) throws SQLException {
        try (PreparedStatement st = c.prepareStatement(
                "INSERT INTO insumo (nome, categoria, grandeza, unidade, estoque_minimo, id_fornecedor, situacao) "
              + "VALUES (?, ?, 'UNIDADE', 'CX', ?, ?, ?) RETURNING idinsumo")) {
            st.setString(1, nome); st.setString(2, CategoriaInsumo.EMBALAGEM.name());
            st.setBigDecimal(3, e.getEstoqueMinimo());
            if (e.getIdFornecedor() != null) st.setInt(4, e.getIdFornecedor()); else st.setNull(4, Types.INTEGER);
            st.setBoolean(5, e.isAtivo());
            try (ResultSet rs = st.executeQuery()) { rs.next(); return rs.getInt(1); }
        }
    }

    /** Atualiza só o cadastro do insumo (nome, mínimo, fornecedor, situação); saldo/preço médio ficam. */
    private static void atualizarInsumo(Connection c, int idInsumo, Embalagem e, String nome) throws SQLException {
        try (PreparedStatement st = c.prepareStatement(
                "UPDATE insumo SET nome=?, categoria=?, grandeza='UNIDADE', unidade='CX', estoque_minimo=?, "
              + "id_fornecedor=?, situacao=? WHERE idinsumo=?")) {
            st.setString(1, nome); st.setString(2, CategoriaInsumo.EMBALAGEM.name());
            st.setBigDecimal(3, e.getEstoqueMinimo());
            if (e.getIdFornecedor() != null) st.setInt(4, e.getIdFornecedor()); else st.setNull(4, Types.INTEGER);
            st.setBoolean(5, e.isAtivo()); st.setInt(6, idInsumo);
            st.executeUpdate();
        }
    }

    private static void positivo(BigDecimal v, String campo) {
        if (v == null || v.signum() <= 0) throw new IllegalArgumentException("Informe " + campo + " maior que zero.");
        if (v.compareTo(new BigDecimal("99999999.99")) > 0) throw new IllegalArgumentException("Valor muito alto em " + campo + ".");
    }

    private static String nome(Connection c, String sql, int id) throws SQLException {
        try (PreparedStatement st = c.prepareStatement(sql)) {
            st.setInt(1, id);
            try (ResultSet rs = st.executeQuery()) { return rs.next() ? rs.getString(1) : null; }
        }
    }

    private static void preencher(PreparedStatement st, Embalagem e) throws SQLException {
        st.setString(1, e.getCodigo()); st.setString(2, e.getDescricao()); st.setBigDecimal(3, e.getPesoKg());
        st.setInt(4, e.getIdAlimento()); st.setInt(5, e.getIdArea());
        st.setBigDecimal(6, e.getPrecoEmbalador()); st.setBoolean(7, e.isAtivo());
    }

    private static Embalagem mapear(ResultSet rs) throws SQLException {
        Embalagem e = new Embalagem();
        e.setIdEmbalagem(rs.getInt("idembalagem")); e.setCodigo(rs.getString("codigo"));
        e.setDescricao(rs.getString("descricao")); e.setPesoKg(rs.getBigDecimal("peso_kg"));
        e.setIdAlimento(rs.getInt("id_alimento")); e.setNomeAlimento(rs.getString("nome_alimento"));
        e.setIdArea(rs.getInt("id_area_producao")); e.setNomeArea(rs.getString("nome_area"));
        e.setSiglaArea(rs.getString("sigla_area"));
        e.setPrecoEmbalador(rs.getBigDecimal("preco_embalador"));
        e.setAtivo(rs.getBoolean("ativo"));
        e.setIdInsumo((Integer) rs.getObject("id_insumo"));
        e.setPrecoCompra(rs.getBigDecimal("preco_medio"));
        e.setSaldo(rs.getBigDecimal("quantidade_disponivel"));
        e.setEstoqueMinimo(rs.getBigDecimal("estoque_minimo"));
        e.setIdFornecedor((Integer) rs.getObject("id_fornecedor"));
        e.setFornecedorNome(rs.getString("fornecedor_nome"));
        e.setAbaixoMinimo(rs.getBoolean("abaixo_minimo"));
        return e;
    }
}
