package Model.Dao;

import Model.Model.AreaProducao;
import Util.PostgresConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AreaProducaoDAO {

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
        return a;
    }
}
