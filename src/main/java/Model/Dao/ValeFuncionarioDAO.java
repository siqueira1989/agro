package Model.Dao;

import Model.Model.ValeFuncionario;
import Model.Model.ValeFuncionario.StatusVale;
import Model.Model.ValeFuncionario.TipoVale;
import Util.PostgresConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ValeFuncionarioDAO {

    private final VinculoDAO vinculoDAO = new VinculoDAO();

    public void registrar(ValeFuncionario v) throws SQLException {
        Model.Model.Vinculo vinc = vinculoDAO.buscarPorData(v.getIdFuncionario(), v.getDataVale());
        Integer idVinculo = vinc != null ? vinc.getIdVinculo() : null;
        String sql = "INSERT INTO vale_funcionario "
                + "(idpessoa, datavale, valor, descricao, tipovale, statusvale, id_vinculo) "
                + "VALUES (?,?,?,?,?,?,?)";
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, v.getIdFuncionario());
            ps.setDate(2, Date.valueOf(v.getDataVale()));
            ps.setBigDecimal(3, v.getValor());
            ps.setString(4, v.getDescricao());
            ps.setString(5, v.getTipoValeStr());
            ps.setString(6, v.getStatusValeStr());
            if (idVinculo != null) ps.setInt(7, idVinculo); else ps.setNull(7, Types.INTEGER);
            ps.executeUpdate();
        }
    }

    public List<ValeFuncionario> listarPorMes(int idFuncionario, String periodo)
            throws SQLException {
        String sql = "SELECT vf.*, p.nomepessoa "
                + "FROM vale_funcionario vf "
                + "JOIN pessoa p ON p.idpessoa = vf.idpessoa "
                + "WHERE vf.idpessoa=? AND TO_CHAR(vf.datavale,'YYYY-MM')=? "
                + "ORDER BY vf.datavale";
        return executarLista(sql, idFuncionario, periodo);
    }

    public List<ValeFuncionario> listarPendentes(int idFuncionario) throws SQLException {
        String sql = "SELECT vf.*, p.nomepessoa "
                + "FROM vale_funcionario vf "
                + "JOIN pessoa p ON p.idpessoa = vf.idpessoa "
                + "WHERE vf.idpessoa=? AND vf.statusvale='PENDENTE' "
                + "ORDER BY vf.datavale";
        List<ValeFuncionario> lista = new ArrayList<>();
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idFuncionario);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapRow(rs));
            }
        }
        return lista;
    }

    public BigDecimal somarPorMes(int idFuncionario, String periodo) throws SQLException {
        String sql = "SELECT COALESCE(SUM(valor),0) FROM vale_funcionario "
                + "WHERE idpessoa=? AND TO_CHAR(datavale,'YYYY-MM')=?";
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idFuncionario);
            ps.setString(2, periodo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
            }
        }
    }

    public BigDecimal somarPendentes(int idFuncionario) throws SQLException {
        String sql = "SELECT COALESCE(SUM(valor),0) FROM vale_funcionario "
                + "WHERE idpessoa=? AND statusvale='PENDENTE'";
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idFuncionario);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
            }
        }
    }

    public void marcarDescontado(int idVale) throws SQLException {
        String sql = "UPDATE vale_funcionario SET statusvale='DESCONTADO' WHERE idvale=?";
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idVale);
            ps.executeUpdate();
        }
    }

    public void marcarDescontadosPorMes(int idFuncionario, String periodo) throws SQLException {
        String sql = "UPDATE vale_funcionario SET statusvale='DESCONTADO' "
                + "WHERE idpessoa=? AND TO_CHAR(datavale,'YYYY-MM')=? AND statusvale='PENDENTE'";
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idFuncionario);
            ps.setString(2, periodo);
            ps.executeUpdate();
        }
    }

    public void excluir(int idVale) throws SQLException {
        String sql = "DELETE FROM vale_funcionario WHERE idvale=?";
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idVale);
            ps.executeUpdate();
        }
    }

    private List<ValeFuncionario> executarLista(String sql, int idFuncionario, String periodo)
            throws SQLException {
        List<ValeFuncionario> lista = new ArrayList<>();
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idFuncionario);
            ps.setString(2, periodo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapRow(rs));
            }
        }
        return lista;
    }

    private ValeFuncionario mapRow(ResultSet rs) throws SQLException {
        ValeFuncionario v = new ValeFuncionario();
        v.setIdVale(rs.getInt("idvale"));
        v.setIdFuncionario(rs.getInt("idpessoa"));
        v.setNomeFuncionario(rs.getString("nomepessoa"));
        v.setDataVale(rs.getDate("datavale").toLocalDate());
        v.setValor(rs.getBigDecimal("valor"));
        v.setDescricao(rs.getString("descricao"));
        try { v.setTipoVale(TipoVale.valueOf(rs.getString("tipovale"))); }
        catch (Exception e) { v.setTipoVale(TipoVale.OUTROS); }
        try { v.setStatusVale(StatusVale.valueOf(rs.getString("statusvale"))); }
        catch (Exception e) { v.setStatusVale(StatusVale.PENDENTE); }
        return v;
    }
}
