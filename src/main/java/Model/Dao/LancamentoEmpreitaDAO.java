package Model.Dao;

import Model.Model.LancamentoEmpreita;
import Model.Model.Vinculo;
import Util.PostgresConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Acesso a lancamento_empreita (dias de empreita com valor). */
public class LancamentoEmpreitaDAO {

    private final VinculoDAO vinculoDAO = new VinculoDAO();

    private Integer resolverVinculo(int idpessoa, LocalDate data) throws SQLException {
        Vinculo v = vinculoDAO.buscarPorData(idpessoa, data);
        return v != null ? v.getIdVinculo() : null;
    }

    public void inserir(LancamentoEmpreita l) throws SQLException {
        Integer idVinc = resolverVinculo(l.getIdPessoa(), l.getDataEmpreita());
        String sql = "INSERT INTO lancamento_empreita (idpessoa, data_empreita, descricao, valor, id_vinculo) "
                + "VALUES (?,?,?,?,?)";
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, l.getIdPessoa());
            ps.setDate(2, Date.valueOf(l.getDataEmpreita()));
            ps.setString(3, l.getDescricao());
            ps.setBigDecimal(4, l.getValor());
            if (idVinc != null) ps.setInt(5, idVinc); else ps.setNull(5, Types.INTEGER);
            ps.executeUpdate();
        }
    }

    public void atualizar(LancamentoEmpreita l) throws SQLException {
        Integer idVinc = resolverVinculo(l.getIdPessoa(), l.getDataEmpreita());
        String sql = "UPDATE lancamento_empreita SET data_empreita=?, descricao=?, valor=?, id_vinculo=? "
                + "WHERE idlancamento=?";
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(l.getDataEmpreita()));
            ps.setString(2, l.getDescricao());
            ps.setBigDecimal(3, l.getValor());
            if (idVinc != null) ps.setInt(4, idVinc); else ps.setNull(4, Types.INTEGER);
            ps.setInt(5, l.getIdLancamento());
            ps.executeUpdate();
        }
    }

    public void excluir(int idLancamento) throws SQLException {
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM lancamento_empreita WHERE idlancamento=?")) {
            ps.setInt(1, idLancamento);
            ps.executeUpdate();
        }
    }

    public LancamentoEmpreita buscarPorId(int idpessoa, int idLancamento) throws SQLException {
        // idpessoa não é usado no filtro (idlancamento é único), mantido por simetria
        String sql = "SELECT * FROM lancamento_empreita WHERE idlancamento=?";
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idLancamento);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public List<LancamentoEmpreita> listarPorMes(int idpessoa, String periodo) throws SQLException {
        String sql = "SELECT * FROM lancamento_empreita "
                + "WHERE idpessoa=? AND TO_CHAR(data_empreita,'YYYY-MM')=? ORDER BY data_empreita";
        List<LancamentoEmpreita> lista = new ArrayList<>();
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idpessoa);
            ps.setString(2, periodo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        }
        return lista;
    }

    public BigDecimal somarPorMes(int idpessoa, String periodo) throws SQLException {
        String sql = "SELECT COALESCE(SUM(valor),0) FROM lancamento_empreita "
                + "WHERE idpessoa=? AND TO_CHAR(data_empreita,'YYYY-MM')=?";
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idpessoa);
            ps.setString(2, periodo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
            }
        }
    }

    private LancamentoEmpreita mapear(ResultSet rs) throws SQLException {
        LancamentoEmpreita l = new LancamentoEmpreita();
        l.setIdLancamento(rs.getInt("idlancamento"));
        l.setIdPessoa(rs.getInt("idpessoa"));
        l.setDataEmpreita(rs.getDate("data_empreita").toLocalDate());
        l.setDescricao(rs.getString("descricao"));
        l.setValor(rs.getBigDecimal("valor"));
        l.setPago(rs.getBoolean("pago"));
        return l;
    }
}
