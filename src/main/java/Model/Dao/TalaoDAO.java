package Model.Dao;

import Model.Model.Alimento;
import Model.Model.AreaProducao;
import Model.Model.Talao;
import Model.Model.TalaoFinanceiro;
import Util.PostgresConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TalaoDAO {

    public void insert(Talao talao) throws SQLException {
        String sql = "INSERT INTO talao (descricaotalao, idproduto, idareaproducao, quantidadeplantastalao) "
                + "VALUES (?, ?, ?, ?)";
        try (Connection conn = new PostgresConnection().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, talao.getDescricaoTalao());
            stmt.setInt(2, talao.getAlimentoTalao().getIdproduto());
            stmt.setInt(3, talao.getAreaproducaoTalao().getIdAreaProducao());
            stmt.setInt(4, talao.getQuantidadeplantasTalao());
            stmt.executeUpdate();
        }
    }

    public void update(Talao talao) throws SQLException {
        String sql = "UPDATE talao SET descricaotalao=?, idproduto=?, idareaproducao=?, quantidadeplantastalao=? "
                + "WHERE idtalao=?";
        try (Connection conn = new PostgresConnection().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, talao.getDescricaoTalao());
            stmt.setInt(2, talao.getAlimentoTalao().getIdproduto());
            stmt.setInt(3, talao.getAreaproducaoTalao().getIdAreaProducao());
            stmt.setInt(4, talao.getQuantidadeplantasTalao());
            stmt.setInt(5, talao.getIdTalao());
            stmt.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM talao WHERE idtalao=?";
        try (Connection conn = new PostgresConnection().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    public List<Talao> listAll() throws SQLException {
        List<Talao> lista = new ArrayList<>();
        String sql = "SELECT t.idtalao, t.descricaotalao, t.quantidadeplantastalao, "
                + "t.idproduto, t.idareaproducao, "
                + "a.nomeproduto, ap.propriedadeareaproducao, ap.siglasAreaProducao "
                + "FROM talao t "
                + "JOIN alimento al ON al.idproduto = t.idproduto "
                + "JOIN produto a ON a.idproduto = t.idproduto "
                + "JOIN areaproducao ap ON ap.idareaproducao = t.idareaproducao "
                + "ORDER BY t.descricaotalao";
        try (Connection conn = new PostgresConnection().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        }
        return lista;
    }

    public List<Talao> listByAreaProducao(int idAreaProducao) throws SQLException {
        List<Talao> lista = new ArrayList<>();
        String sql = "SELECT t.idtalao, t.descricaotalao, t.quantidadeplantastalao, "
                + "t.idproduto, t.idareaproducao, "
                + "a.nomeproduto, ap.propriedadeareaproducao, ap.siglasAreaProducao "
                + "FROM talao t "
                + "JOIN produto a ON a.idproduto = t.idproduto "
                + "JOIN areaproducao ap ON ap.idareaproducao = t.idareaproducao "
                + "WHERE t.idareaproducao=? ORDER BY t.descricaotalao";
        try (Connection conn = new PostgresConnection().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idAreaProducao);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        }
        return lista;
    }

    public Talao getById(int id) throws SQLException {
        String sql = "SELECT t.idtalao, t.descricaotalao, t.quantidadeplantastalao, "
                + "t.idproduto, t.idareaproducao, "
                + "a.nomeproduto, ap.propriedadeareaproducao, ap.siglasAreaProducao "
                + "FROM talao t "
                + "JOIN produto a ON a.idproduto = t.idproduto "
                + "JOIN areaproducao ap ON ap.idareaproducao = t.idareaproducao "
                + "WHERE t.idtalao=?";
        try (Connection conn = new PostgresConnection().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }
        }
        return null;
    }

    // --- TalaoFinanceiro ---

    public void insertTalaoFinanceiro(TalaoFinanceiro tf) throws SQLException {
        String sql = "INSERT INTO talaofinanceiro (idtalao, safratalaofinanceiro, "
                + "iniciosafratalaofinanceiro, terminosafratalaofinanceiro, "
                + "custosafratalaofinanceiro, despesassafratalaofinanceiro, "
                + "vendabrutasafratalaofinanceiro, vendaliquidasafratalaofinanceiro) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = new PostgresConnection().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, tf.getIdTalao());
            stmt.setString(2, tf.getSafraTalaoFinanceiro());
            stmt.setDate(3, tf.getIniciosafraTalaoFinanceiro() != null
                    ? new Date(tf.getIniciosafraTalaoFinanceiro().getTime()) : null);
            stmt.setDate(4, tf.getTerminosafraTalaoFinanceiro() != null
                    ? new Date(tf.getTerminosafraTalaoFinanceiro().getTime()) : null);
            stmt.setDouble(5, tf.getCustosafraTalaoFinanceiro() != null ? tf.getCustosafraTalaoFinanceiro() : 0.0);
            stmt.setDouble(6, tf.getDespesassafraTalaoFinanceiro() != null ? tf.getDespesassafraTalaoFinanceiro() : 0.0);
            stmt.setDouble(7, tf.getVendabrutassafraTalaoFinanceiro() != null ? tf.getVendabrutassafraTalaoFinanceiro() : 0.0);
            stmt.setDouble(8, tf.getVendasliquidassafraTalaoFinanceiro() != null ? tf.getVendasliquidassafraTalaoFinanceiro() : 0.0);
            stmt.executeUpdate();
        }
    }

    public void updateTalaoFinanceiro(TalaoFinanceiro tf) throws SQLException {
        String sql = "UPDATE talaofinanceiro SET safratalaofinanceiro=?, "
                + "iniciosafratalaofinanceiro=?, terminosafratalaofinanceiro=?, "
                + "custosafratalaofinanceiro=?, despesassafratalaofinanceiro=?, "
                + "vendabrutasafratalaofinanceiro=?, vendaliquidasafratalaofinanceiro=? "
                + "WHERE idtalaofinanceiro=?";
        try (Connection conn = new PostgresConnection().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, tf.getSafraTalaoFinanceiro());
            stmt.setDate(2, tf.getIniciosafraTalaoFinanceiro() != null
                    ? new Date(tf.getIniciosafraTalaoFinanceiro().getTime()) : null);
            stmt.setDate(3, tf.getTerminosafraTalaoFinanceiro() != null
                    ? new Date(tf.getTerminosafraTalaoFinanceiro().getTime()) : null);
            stmt.setDouble(4, tf.getCustosafraTalaoFinanceiro() != null ? tf.getCustosafraTalaoFinanceiro() : 0.0);
            stmt.setDouble(5, tf.getDespesassafraTalaoFinanceiro() != null ? tf.getDespesassafraTalaoFinanceiro() : 0.0);
            stmt.setDouble(6, tf.getVendabrutassafraTalaoFinanceiro() != null ? tf.getVendabrutassafraTalaoFinanceiro() : 0.0);
            stmt.setDouble(7, tf.getVendasliquidassafraTalaoFinanceiro() != null ? tf.getVendasliquidassafraTalaoFinanceiro() : 0.0);
            stmt.setInt(8, tf.getIdTalaoFinanceiro());
            stmt.executeUpdate();
        }
    }

    public TalaoFinanceiro getTalaoFinanceiroByTalao(int idTalao) throws SQLException {
        String sql = "SELECT * FROM talaofinanceiro WHERE idtalao=? ORDER BY idtalaofinanceiro DESC LIMIT 1";
        try (Connection conn = new PostgresConnection().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idTalao);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapearFinanceiro(rs);
            }
        }
        return null;
    }

    private Talao mapear(ResultSet rs) throws SQLException {
        Talao t = new Talao();
        t.setIdTalao(rs.getInt("idtalao"));
        t.setDescricaoTalao(rs.getString("descricaotalao"));
        t.setQuantidadeplantasTalao(rs.getInt("quantidadeplantastalao"));

        Alimento alimento = new Alimento();
        alimento.setIdproduto(rs.getInt("idproduto"));
        alimento.setNomeproduto(rs.getString("nomeproduto"));
        t.setAlimentoTalao(alimento);

        AreaProducao area = new AreaProducao();
        area.setIdAreaProducao(rs.getInt("idareaproducao"));
        area.setPropriedadeAreaProducao(rs.getString("propriedadeareaproducao"));
        area.setSiglasAreaProducao(rs.getString("siglasAreaProducao"));
        t.setAreaproducaoTalao(area);

        return t;
    }

    private TalaoFinanceiro mapearFinanceiro(ResultSet rs) throws SQLException {
        TalaoFinanceiro tf = new TalaoFinanceiro();
        tf.setIdTalaoFinanceiro(rs.getInt("idtalaofinanceiro"));
        tf.setIdTalao(rs.getInt("idtalao"));
        tf.setSafraTalaoFinanceiro(rs.getString("safratalaofinanceiro"));
        tf.setIniciosafraTalaoFinanceiro(rs.getDate("iniciosafratalaofinanceiro"));
        tf.setTerminosafraTalaoFinanceiro(rs.getDate("terminosafratalaofinanceiro"));
        tf.setCustosafraTalaoFinanceiro(rs.getDouble("custosafratalaofinanceiro"));
        tf.setDespesassafraTalaoFinanceiro(rs.getDouble("despesassafratalaofinanceiro"));
        tf.setVendabrutassafraTalaoFinanceiro(rs.getDouble("vendabrutasafratalaofinanceiro"));
        tf.setVendasliquidassafraTalaoFinanceiro(rs.getDouble("vendaliquidasafratalaofinanceiro"));
        return tf;
    }
}
