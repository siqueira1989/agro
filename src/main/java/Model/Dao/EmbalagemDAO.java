package Model.Dao;

import Model.Model.Embalagem;
import Util.PostgresConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Cadastro de embalagens (V31), do Módulo Colheita e Estoque. Por alimento + área de produção, com
 * preço pago ao embalador (Processar) e preço de compra (Romaneio). Não há exclusão: embalagem já
 * usada num processamento fica referenciada em colheita_processamento_embalagem — só desativa.
 * Regras violadas ⇒ IllegalArgumentException com mensagem para o usuário; SQLException propaga.
 */
public class EmbalagemDAO {

    private static final String SELECT =
            "SELECT e.*, al.nomeproduto AS nome_alimento, ar.propriedadeareaproducao AS nome_area, "
          + "ar.siglasareaproducao AS sigla_area FROM embalagem e "
          + "JOIN alimento al ON al.idproduto = e.id_alimento "
          + "JOIN areaproducao ar ON ar.idareaproducao = e.id_area_producao";

    /** Lista com filtros opcionais (null = todos). Ordem: alimento, área, descrição. */
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

    public Embalagem buscar(Connection c, int id) throws SQLException {
        try (PreparedStatement st = c.prepareStatement(SELECT + " WHERE e.idembalagem=?")) {
            st.setInt(1, id);
            try (ResultSet rs = st.executeQuery()) { return rs.next() ? mapear(rs) : null; }
        }
    }

    /**
     * Insere (idEmbalagem = 0) ou atualiza. O código vazio é gerado da descrição ("Papelão 7kg" →
     * PAPELAO_7KG). Código repetido no mesmo alimento + área é recusado. Devolve o registro gravado.
     */
    public Embalagem salvar(Embalagem e) throws SQLException {
        try (Connection c = new PostgresConnection().getConnection()) {
            return salvar(c, e);
        }
    }

    public Embalagem salvar(Connection c, Embalagem e) throws SQLException {
        validar(c, e);
        try (PreparedStatement st = c.prepareStatement(
                "SELECT idembalagem FROM embalagem WHERE codigo=? AND id_alimento=? AND id_area_producao=? AND idembalagem<>?")) {
            st.setString(1, e.getCodigo()); st.setInt(2, e.getIdAlimento()); st.setInt(3, e.getIdArea());
            st.setInt(4, e.getIdEmbalagem());
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) throw new IllegalArgumentException(
                        "Já existe a embalagem " + e.getCodigo() + " para este alimento e área.");
            }
        }
        int id = e.getIdEmbalagem();
        if (id == 0) {
            try (PreparedStatement st = c.prepareStatement(
                    "INSERT INTO embalagem (codigo, descricao, peso_kg, id_alimento, id_area_producao, preco_compra, "
                  + "preco_embalador, ativo) VALUES (?,?,?,?,?,?,?,?) RETURNING idembalagem")) {
                preencher(st, e);
                try (ResultSet rs = st.executeQuery()) { rs.next(); id = rs.getInt(1); }
            }
        } else {
            try (PreparedStatement st = c.prepareStatement(
                    "UPDATE embalagem SET codigo=?, descricao=?, peso_kg=?, id_alimento=?, id_area_producao=?, "
                  + "preco_compra=?, preco_embalador=?, ativo=?, atualizado_em=now() WHERE idembalagem=?")) {
                preencher(st, e);
                st.setInt(9, id);
                if (st.executeUpdate() == 0) throw new IllegalArgumentException("Embalagem não encontrada.");
            }
        }
        return buscar(c, id);
    }

    public void definirAtivo(int id, boolean ativo) throws SQLException {
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement st = c.prepareStatement(
                     "UPDATE embalagem SET ativo=?, atualizado_em=now() WHERE idembalagem=?")) {
            st.setBoolean(1, ativo); st.setInt(2, id);
            if (st.executeUpdate() == 0) throw new IllegalArgumentException("Embalagem não encontrada.");
        }
    }

    private void validar(Connection c, Embalagem e) throws SQLException {
        if (e.getDescricao() == null || e.getDescricao().isBlank()) throw new IllegalArgumentException("Informe a descrição da embalagem.");
        e.setDescricao(e.getDescricao().trim());
        if (e.getDescricao().length() > 80) throw new IllegalArgumentException("Descrição com no máximo 80 caracteres.");
        String cod = e.getCodigo() == null || e.getCodigo().isBlank() ? e.getDescricao() : e.getCodigo();
        e.setCodigo(DespesasCustosDAO.normalizarTipoCaixa(cod));   // IAE se inválido
        if (e.getIdAlimento() == null || e.getIdArea() == null) throw new IllegalArgumentException("Escolha o alimento e a área de produção.");
        positivo(e.getPesoKg(), "o peso (kg)");
        positivo(e.getPrecoCompra(), "o preço de compra");
        positivo(e.getPrecoEmbalador(), "o preço pago ao embalador");
        if (!existe(c, "SELECT 1 FROM alimento WHERE idproduto=?", e.getIdAlimento())) throw new IllegalArgumentException("Alimento não encontrado.");
        if (!existe(c, "SELECT 1 FROM areaproducao WHERE idareaproducao=?", e.getIdArea())) throw new IllegalArgumentException("Área de produção não encontrada.");
    }

    private static void positivo(BigDecimal v, String campo) {
        if (v == null || v.signum() <= 0) throw new IllegalArgumentException("Informe " + campo + " maior que zero.");
        if (v.compareTo(new BigDecimal("99999999.99")) > 0) throw new IllegalArgumentException("Valor muito alto em " + campo + ".");
    }

    private static boolean existe(Connection c, String sql, int id) throws SQLException {
        try (PreparedStatement st = c.prepareStatement(sql)) {
            st.setInt(1, id);
            try (ResultSet rs = st.executeQuery()) { return rs.next(); }
        }
    }

    private static void preencher(PreparedStatement st, Embalagem e) throws SQLException {
        st.setString(1, e.getCodigo()); st.setString(2, e.getDescricao()); st.setBigDecimal(3, e.getPesoKg());
        st.setInt(4, e.getIdAlimento()); st.setInt(5, e.getIdArea());
        st.setBigDecimal(6, e.getPrecoCompra()); st.setBigDecimal(7, e.getPrecoEmbalador()); st.setBoolean(8, e.isAtivo());
    }

    private static Embalagem mapear(ResultSet rs) throws SQLException {
        Embalagem e = new Embalagem();
        e.setIdEmbalagem(rs.getInt("idembalagem")); e.setCodigo(rs.getString("codigo"));
        e.setDescricao(rs.getString("descricao")); e.setPesoKg(rs.getBigDecimal("peso_kg"));
        e.setIdAlimento(rs.getInt("id_alimento")); e.setNomeAlimento(rs.getString("nome_alimento"));
        e.setIdArea(rs.getInt("id_area_producao")); e.setNomeArea(rs.getString("nome_area"));
        e.setSiglaArea(rs.getString("sigla_area"));
        e.setPrecoCompra(rs.getBigDecimal("preco_compra")); e.setPrecoEmbalador(rs.getBigDecimal("preco_embalador"));
        e.setAtivo(rs.getBoolean("ativo"));
        return e;
    }
}
