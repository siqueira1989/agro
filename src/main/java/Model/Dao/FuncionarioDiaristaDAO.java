package Model.Dao;

import Model.Model.FuncionarioDiarista;
import Model.Model.TipoFuncionario;
import Util.PostgresConnection;

import java.math.BigDecimal;
import java.sql.*;

/** Acesso a funcionariodiarista (valor fixo por dia). */
public class FuncionarioDiaristaDAO {

    public FuncionarioDiarista getById(int id) throws SQLException {
        String sql = "SELECT idpessoa, nomepessoa, usuariopessoa, senhapessoa, nivelpessoa, "
                + "situacaopessoa, emailpessoa, telefonepessoa, cpfpf, datanascimentopf, numero, "
                + "complemento, cep, matriculafuncionario, cargofuncionario, datainiciofuncionario, "
                + "datafimfuncionario, valorpordia FROM funcionariodiarista WHERE idpessoa=?";
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                FuncionarioDiarista f = new FuncionarioDiarista(
                        rs.getInt("idpessoa"), rs.getString("nomepessoa"), rs.getString("usuariopessoa"),
                        rs.getString("senhapessoa"), rs.getString("nivelpessoa"), rs.getBoolean("situacaopessoa"),
                        rs.getString("emailpessoa"), rs.getString("telefonepessoa"), rs.getString("cep"),
                        rs.getInt("numero"), rs.getString("complemento"), rs.getString("cpfpf"),
                        rs.getDate("datanascimentopf") != null ? rs.getDate("datanascimentopf").toLocalDate() : null,
                        rs.getString("matriculafuncionario"), TipoFuncionario.DIARISTA,
                        rs.getString("cargofuncionario"),
                        rs.getDate("datafimfuncionario") != null ? rs.getDate("datafimfuncionario").toLocalDate() : null,
                        rs.getDate("datainiciofuncionario") != null ? rs.getDate("datainiciofuncionario").toLocalDate() : null);
                f.setValorPorDia(rs.getBigDecimal("valorpordia"));
                return f;
            }
        }
    }

    public void updateValorPorDia(int id, BigDecimal valor) throws SQLException {
        String sql = "UPDATE funcionariodiarista SET valorpordia=? WHERE idpessoa=?";
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBigDecimal(1, valor != null ? valor : BigDecimal.ZERO);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }
}
