package Model.Dao;

import Model.Model.FuncionarioCLT;
import Model.Model.FuncionarioCLT.StatusEmprego;
import Model.Model.TipoAtividadeRural;
import Model.Model.TipoFuncionario;
import Util.PostgresConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FuncionarioCLTDAO {

    private static final String COLS =
        // P0-2: senhapessoa foi retirada da lista — nenhuma consulta de leitura traz o hash.
        "idpessoa, nomepessoa, usuariopessoa, nivelpessoa, situacaopessoa, "
      + "emailpessoa, telefonepessoa, cpfpf, datanascimentopf, numero, complemento, cep, "
      + "matriculafuncionario, tipofuncionario, cargofuncionario, "
      + "datainiciofuncionario, datafimfuncionario, salariomensal, valorhoraextra, "
      + "statusemprego, cargahorariadiaria, tipo_atividade_rural";

    public FuncionarioCLT getCLTById(int id) throws SQLException {
        String sql = "SELECT " + COLS + " FROM funcionarioclt WHERE idpessoa=?";
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public List<FuncionarioCLT> listTodos() throws SQLException {
        return listarPorStatus(null);
    }

    public List<FuncionarioCLT> listarAtivos() throws SQLException {
        return listarPorStatus(StatusEmprego.ATIVO);
    }

    public List<FuncionarioCLT> listarPorStatus(StatusEmprego status) throws SQLException {
        String sql = "SELECT " + COLS + " FROM funcionarioclt"
            + (status != null ? " WHERE statusemprego=?" : "")
            + " ORDER BY nomepessoa";
        List<FuncionarioCLT> lista = new ArrayList<>();
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            if (status != null) ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        }
        return lista;
    }

    public void updateSalario(int idFuncionario, java.math.BigDecimal salario,
                              java.math.BigDecimal valorHoraExtra,
                              int cargaHorariaDiaria,
                              TipoAtividadeRural tipoAtividade) throws SQLException {
        String sql = "UPDATE funcionarioclt SET salariomensal=?, valorhoraextra=?, "
                   + "cargahorariadiaria=?, tipo_atividade_rural=CAST(? AS tipo_atividade_rural) "
                   + "WHERE idpessoa=?";
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBigDecimal(1, salario);
            ps.setBigDecimal(2, valorHoraExtra);
            ps.setInt(3, cargaHorariaDiaria);
            ps.setString(4, (tipoAtividade != null ? tipoAtividade : TipoAtividadeRural.LAVOURA).name());
            ps.setInt(5, idFuncionario);
            ps.executeUpdate();
        }
    }

    public void updateStatusEmprego(int idFuncionario, StatusEmprego status) throws SQLException {
        String sql = "UPDATE funcionarioclt SET statusemprego=? WHERE idpessoa=?";
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt(2, idFuncionario);
            ps.executeUpdate();
        }
    }

    private FuncionarioCLT mapear(ResultSet rs) throws SQLException {
        FuncionarioCLT f = new FuncionarioCLT(
            rs.getInt("idpessoa"),
            rs.getString("nomepessoa"),
            rs.getString("usuariopessoa"),
            null /* P0-2: a senha nunca sai do banco */,
            rs.getString("nivelpessoa"),
            rs.getBoolean("situacaopessoa"),
            rs.getString("emailpessoa"),
            rs.getString("telefonepessoa"),
            rs.getString("cep"),
            rs.getInt("numero"),
            rs.getString("complemento"),
            rs.getString("cpfpf"),
            rs.getDate("datanascimentopf") != null
                ? rs.getDate("datanascimentopf").toLocalDate() : null,
            rs.getString("matriculafuncionario"),
            TipoFuncionario.CLT,
            rs.getString("cargofuncionario"),
            rs.getDate("datafimfuncionario") != null
                ? rs.getDate("datafimfuncionario").toLocalDate() : null,
            rs.getDate("datainiciofuncionario") != null
                ? rs.getDate("datainiciofuncionario").toLocalDate() : null
        );
        f.setSalarioMensal(rs.getBigDecimal("salariomensal"));
        f.setValorHoraExtra(rs.getBigDecimal("valorhoraextra"));
        f.setCargaHorariaDiaria(rs.getInt("cargahorariadiaria"));
        String statusStr = rs.getString("statusemprego");
        try {
            f.setStatusEmprego(StatusEmprego.valueOf(statusStr));
        } catch (Exception e) {
            f.setStatusEmprego(StatusEmprego.ATIVO);
        }
        try {
            f.setTipoAtividadeRural(TipoAtividadeRural.valueOf(rs.getString("tipo_atividade_rural")));
        } catch (Exception e) {
            f.setTipoAtividadeRural(TipoAtividadeRural.LAVOURA);
        }
        return f;
    }
}
