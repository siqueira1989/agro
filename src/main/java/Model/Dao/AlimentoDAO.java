package Model.Dao;

import Model.Model.Alimento;
import Util.PostgresConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AlimentoDAO {

    public List<Alimento> listAll() throws SQLException {
        PostgresConnection conn = new PostgresConnection();
        Connection conexao = conn.getConnection();

        List<Alimento> alimentos = new ArrayList<>();
        try {
            String sql = "SELECT idproduto,nomeproduto, situacaoproduto, "
                    + "tipoproduto, variedadealimento FROM alimento";
            PreparedStatement stmt = conexao.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Alimento alimento = new Alimento();

                alimento.setIdproduto(rs.getInt("idproduto"));
                alimento.setNomeproduto(rs.getString("nomeproduto"));
                alimento.setSituacaoproduto(rs.getBoolean("situacaoproduto"));
                alimento.setTipoproduto(rs.getString("tipoproduto"));
                alimento.setVariedadealimento(rs.getString("variedadealimento"));

                alimentos.add(alimento);
            }
            conexao.close();
        } catch (SQLException e) {
            System.out.println("Erro na camada Dao: " + e.getMessage());

        }

        return alimentos;
    }

    public Alimento AlimentoBuscaID(int idalimento) throws SQLException {
        PostgresConnection conn = new PostgresConnection();
        Connection conexao = conn.getConnection();
        Alimento alimento = null;

        try {
            String sql = "SELECT idproduto,nomeproduto, situacaoproduto, "
                    + "tipoproduto, variedadealimento FROM alimento where idproduto=? ";
            PreparedStatement stmt = conexao.prepareStatement(sql);
            stmt.setInt(1, idalimento);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                alimento = new Alimento();

                alimento.setIdproduto(rs.getInt("idproduto"));
                alimento.setNomeproduto(rs.getString("nomeproduto"));
                alimento.setSituacaoproduto(rs.getBoolean("situacaoproduto"));
                alimento.setTipoproduto(rs.getString("tipoproduto"));
                alimento.setVariedadealimento(rs.getString("variedadealimento"));
            }
            conexao.close();
        } catch (SQLException e) {
            System.out.println("Erro na camada Dao: " + e.getMessage());

        }

        return alimento;
    }

    public void addAlimento(Alimento alimento) throws SQLException {
        PostgresConnection conn = new PostgresConnection();
        Connection conexao = conn.getConnection();
        System.out.println("nivel 2");
        try {
            String sql = "INSERT INTO alimento ("
                    + "nomeproduto, tipoproduto, "
                    + "situacaoproduto ,variedadealimento)"
                    + "VALUES ( ?,?,?,?)";

            PreparedStatement stmt = conexao.prepareStatement(sql);
            stmt.setString(1, alimento.getNomeproduto());
            stmt.setString(2, alimento.getTipoproduto());
            stmt.setBoolean(3, alimento.isSituacaoproduto());
            stmt.setString(4, alimento.getVariedadealimento());

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao adicionar o parceiro: " + e.getMessage(), e);
        }
    }

    public void updateAlimento(Alimento alimento) throws SQLException {
        PostgresConnection conn = new PostgresConnection();
        Connection conexao = conn.getConnection();
        System.out.println("nivel 2");
        try {
            String sql = "UPDATE classificacao SET nomeproduto = ?, tipoproduto = ?,"
                    + "situacaoproduto = ?, variedadealimento= ? WHERE idproduto = ?";

            PreparedStatement stmt = conexao.prepareStatement(sql);
            stmt.setString(1, alimento.getNomeproduto());
            stmt.setString(2, alimento.getTipoproduto());
            stmt.setBoolean(3, alimento.isSituacaoproduto());
            stmt.setString(4, alimento.getVariedadealimento());
            stmt.setInt(4, alimento.getIdproduto());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao adicionar o parceiro: " + e.getMessage(), e);
        }
    }

    /*Metodo de veriticação*/
    public boolean VerificarDadosAlimento(Alimento alimento) throws SQLException {
        PostgresConnection conn = new PostgresConnection();
        Connection conexao = conn.getConnection();

        try {

            String sql = "SELECT COUNT(*) FROM alimento WHERE nomeproduto = ?"
                    + " AND tipoproduto = ? AND variedadealimento = ?";

            PreparedStatement stmt = conexao.prepareStatement(sql);
            stmt.setString(1, alimento.getNomeproduto());
            stmt.setString(2, alimento.getTipoproduto());
            stmt.setString(3, alimento.getVariedadealimento());
            ResultSet rs = stmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (Exception e) {
            System.out.println("Erro sistema VerificarDadosUpdate " + e.getMessage());
        }
        return false;
    }
    
}
