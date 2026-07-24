package Model.Dao;

import Model.Model.AreaProducao;
import Model.Model.Quadra;
import Util.PostgresConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AreaProducaoDAO {

    private final QuadraDAO quadraDAO = new QuadraDAO();

    public void insert(AreaProducao area) throws SQLException {
        String sql = "INSERT INTO areaproducao (propriedadeareaproducao, proprietarioareaproducao, "
                + "quantidadetotalplantasareaproducao, siglasAreaProducao, cep, numero, complemento) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = new PostgresConnection().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, area.getPropriedadeAreaProducao());
            stmt.setString(2, area.getProprietarioAreaProducao());
            stmt.setInt(3, area.getQuantidadeTotalPlantasAreaProducao());
            stmt.setString(4, area.getSiglasAreaProducao());
            stmt.setString(5, area.getCep());
            stmt.setInt(6, area.getNumero());
            stmt.setString(7, area.getComplemento());
            stmt.executeUpdate();
        }
    }

    public void update(AreaProducao area) throws SQLException {
        String sql = "UPDATE areaproducao SET propriedadeareaproducao=?, proprietarioareaproducao=?, "
                + "quantidadetotalplantasareaproducao=?, siglasAreaProducao=?, cep=?, numero=?, complemento=? "
                + "WHERE idareaproducao=?";
        try (Connection conn = new PostgresConnection().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, area.getPropriedadeAreaProducao());
            stmt.setString(2, area.getProprietarioAreaProducao());
            stmt.setInt(3, area.getQuantidadeTotalPlantasAreaProducao());
            stmt.setString(4, area.getSiglasAreaProducao());
            stmt.setString(5, area.getCep());
            stmt.setInt(6, area.getNumero());
            stmt.setString(7, area.getComplemento());
            stmt.setInt(8, area.getIdAreaProducao());
            stmt.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM areaproducao WHERE idareaproducao=?";
        try (Connection conn = new PostgresConnection().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    /**
     * Cria a área e suas quadras numa transação; a quantidade total de plantas
     * é gravada como a soma das plantas das quadras informadas.
     */
    public int inserirComQuadras(AreaProducao area, List<Quadra> quadras) throws SQLException {
        int soma = 0;
        for (Quadra q : quadras) soma += q.getNumeroPlantas();

        String sql = "INSERT INTO areaproducao (propriedadeareaproducao, proprietarioareaproducao, "
                + "quantidadetotalplantasareaproducao, siglasAreaProducao, cep, numero, complemento, situacao) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, true)";
        try (Connection conn = new PostgresConnection().getConnection()) {
            conn.setAutoCommit(false);
            try {
                int idArea;
                try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    stmt.setString(1, area.getPropriedadeAreaProducao());
                    stmt.setString(2, area.getProprietarioAreaProducao());
                    stmt.setInt(3, soma);
                    stmt.setString(4, area.getSiglasAreaProducao());
                    stmt.setString(5, area.getCep());
                    stmt.setInt(6, area.getNumero());
                    stmt.setString(7, area.getComplemento());
                    stmt.executeUpdate();
                    try (ResultSet keys = stmt.getGeneratedKeys()) {
                        keys.next();
                        idArea = keys.getInt(1);
                    }
                }
                for (Quadra q : quadras) {
                    q.setIdAreaProducao(idArea);
                    quadraDAO.inserir(conn, q);
                }
                conn.commit();
                return idArea;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /** Ativa/desativa a área (soft delete). */
    public void definirSituacao(int id, boolean situacao) throws SQLException {
        try (Connection conn = new PostgresConnection().getConnection();
             PreparedStatement stmt = conn.prepareStatement("UPDATE areaproducao SET situacao=? WHERE idareaproducao=?")) {
            stmt.setBoolean(1, situacao);
            stmt.setInt(2, id);
            stmt.executeUpdate();
        }
    }

    /** Recalcula a quantidade total de plantas da área a partir das quadras ativas. */
    public void recomputarQtdPlantas(int idArea) throws SQLException {
        String sql = "UPDATE areaproducao SET quantidadetotalplantasareaproducao = "
                + "(SELECT COALESCE(SUM(numero_plantas),0) FROM quadra WHERE idareaproducao=? AND ativa=true) "
                + "WHERE idareaproducao=?";
        try (Connection conn = new PostgresConnection().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idArea);
            stmt.setInt(2, idArea);
            stmt.executeUpdate();
        }
    }

    /** Lista os alimentos (id + nome) para o select de tipo de planta. */
    public List<Map<String, Object>> listarAlimentos() throws SQLException {
        List<Map<String, Object>> lista = new ArrayList<>();
        String sql = "SELECT idproduto, nomeproduto FROM alimento ORDER BY nomeproduto";
        try (Connection conn = new PostgresConnection().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", rs.getInt("idproduto"));
                m.put("nome", rs.getString("nomeproduto"));
                lista.add(m);
            }
        }
        return lista;
    }

    public List<AreaProducao> listAll() throws SQLException {
        List<AreaProducao> lista = new ArrayList<>();
        String sql = "SELECT * FROM areaproducao ORDER BY propriedadeareaproducao";
        try (Connection conn = new PostgresConnection().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        }
        return lista;
    }

    public AreaProducao getById(int id) throws SQLException {
        String sql = "SELECT * FROM areaproducao WHERE idareaproducao=?";
        try (Connection conn = new PostgresConnection().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }
        }
        return null;
    }

    public boolean existeSigla(String sigla) throws SQLException {
        String sql = "SELECT 1 FROM areaproducao WHERE UPPER(siglasAreaProducao)=UPPER(?)";
        try (Connection conn = new PostgresConnection().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, sigla);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean existeSiglaOutro(String sigla, int idAtual) throws SQLException {
        String sql = "SELECT 1 FROM areaproducao WHERE UPPER(siglasAreaProducao)=UPPER(?) AND idareaproducao<>?";
        try (Connection conn = new PostgresConnection().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, sigla);
            stmt.setInt(2, idAtual);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public int contarTodos() throws SQLException {
        String sql = "SELECT COUNT(*) FROM areaproducao";
        try (Connection conn = new PostgresConnection().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    private AreaProducao mapear(ResultSet rs) throws SQLException {
        AreaProducao a = new AreaProducao();
        a.setIdAreaProducao(rs.getInt("idareaproducao"));
        a.setPropriedadeAreaProducao(rs.getString("propriedadeareaproducao"));
        a.setProprietarioAreaProducao(rs.getString("proprietarioareaproducao"));
        a.setQuantidadeTotalPlantasAreaProducao(rs.getInt("quantidadetotalplantasareaproducao"));
        a.setSiglasAreaProducao(rs.getString("siglasAreaProducao"));
        a.setCep(rs.getString("cep"));
        a.setNumero(rs.getInt("numero"));
        a.setComplemento(rs.getString("complemento"));
        a.setSituacao(rs.getBoolean("situacao"));
        return a;
    }
}
