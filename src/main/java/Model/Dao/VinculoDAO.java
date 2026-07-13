package Model.Dao;

import Model.Model.TipoAtividadeRural;
import Model.Model.TipoFuncionario;
import Model.Model.Vinculo;
import Util.PostgresConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Acesso a `vinculo_empregaticio`. Um vínculo é um período de trabalho de uma
 * pessoa; uma pessoa pode ter vários (não sobrepostos). Há no máximo um
 * vínculo ativo por pessoa (índice único parcial no banco).
 */
public class VinculoDAO {

    private static final String COLS =
        "id_vinculo, idpessoa, tipo_funcionario, cargo, tipo_atividade_rural, "
      + "data_admissao, data_desligamento, salario_mensal, valor_hora_extra, "
      + "carga_horaria_diaria, status, motivo_desligamento";

    public int inserir(Vinculo v) throws SQLException {
        String sql = "INSERT INTO vinculo_empregaticio "
            + "(idpessoa, tipo_funcionario, cargo, tipo_atividade_rural, data_admissao, "
            + " data_desligamento, salario_mensal, valor_hora_extra, carga_horaria_diaria, "
            + " status, motivo_desligamento) "
            + "VALUES (?, CAST(? AS tipo_funcionario), ?, CAST(? AS tipo_atividade_rural), "
            + " ?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, v);
            ps.executeUpdate();
            try (ResultSet gk = ps.getGeneratedKeys()) {
                if (gk.next()) { v.setIdVinculo(gk.getInt(1)); return gk.getInt(1); }
            }
        }
        return 0;
    }

    private void bind(PreparedStatement ps, Vinculo v) throws SQLException {
        ps.setInt(1, v.getIdPessoa());
        ps.setString(2, v.getTipoFuncionario() != null ? v.getTipoFuncionario().name() : "CLT");
        ps.setString(3, v.getCargo());
        ps.setString(4, v.getTipoAtividadeRural() != null
                ? v.getTipoAtividadeRural().name() : "LAVOURA");
        if (v.getDataAdmissao() != null) ps.setDate(5, Date.valueOf(v.getDataAdmissao()));
        else ps.setNull(5, Types.DATE);
        if (v.getDataDesligamento() != null) ps.setDate(6, Date.valueOf(v.getDataDesligamento()));
        else ps.setNull(6, Types.DATE);
        ps.setBigDecimal(7, v.getSalarioMensal());
        ps.setBigDecimal(8, v.getValorHoraExtra());
        ps.setInt(9, v.getCargaHorariaDiaria());
        ps.setString(10, v.getStatus());
        ps.setString(11, v.getMotivoDesligamento());
    }

    public List<Vinculo> listarPorPessoa(int idPessoa) throws SQLException {
        String sql = "SELECT " + COLS + " FROM vinculo_empregaticio "
                   + "WHERE idpessoa=? ORDER BY data_admissao DESC NULLS LAST, id_vinculo DESC";
        List<Vinculo> lista = new ArrayList<>();
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idPessoa);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        }
        return lista;
    }

    /** Vínculo ativo (sem desligamento) da pessoa, ou null. */
    public Vinculo buscarAtivo(int idPessoa) throws SQLException {
        String sql = "SELECT " + COLS + " FROM vinculo_empregaticio "
                   + "WHERE idpessoa=? AND data_desligamento IS NULL "
                   + "ORDER BY id_vinculo DESC LIMIT 1";
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idPessoa);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public Vinculo buscarPorId(int idVinculo) throws SQLException {
        String sql = "SELECT " + COLS + " FROM vinculo_empregaticio WHERE id_vinculo=?";
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idVinculo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    /**
     * Vínculo ao qual uma data pertence: admissao &lt;= data &lt;= coalesce(deslig, hoje).
     * Como os períodos não se sobrepõem, resolve um único vínculo.
     */
    public Vinculo buscarPorData(int idPessoa, LocalDate data) throws SQLException {
        String sql = "SELECT " + COLS + " FROM vinculo_empregaticio "
                   + "WHERE idpessoa=? "
                   + "AND (data_admissao IS NULL OR data_admissao <= ?) "
                   + "AND (data_desligamento IS NULL OR data_desligamento >= ?) "
                   + "ORDER BY data_admissao DESC NULLS LAST, id_vinculo DESC LIMIT 1";
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idPessoa);
            ps.setDate(2, Date.valueOf(data));
            ps.setDate(3, Date.valueOf(data));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public void atualizarTermos(Vinculo v) throws SQLException {
        String sql = "UPDATE vinculo_empregaticio SET "
                   + "cargo=?, tipo_atividade_rural=CAST(? AS tipo_atividade_rural), "
                   + "salario_mensal=?, valor_hora_extra=?, carga_horaria_diaria=?, "
                   + "data_admissao=?, status=? WHERE id_vinculo=?";
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, v.getCargo());
            ps.setString(2, v.getTipoAtividadeRural() != null ? v.getTipoAtividadeRural().name() : "LAVOURA");
            ps.setBigDecimal(3, v.getSalarioMensal());
            ps.setBigDecimal(4, v.getValorHoraExtra());
            ps.setInt(5, v.getCargaHorariaDiaria());
            if (v.getDataAdmissao() != null) ps.setDate(6, Date.valueOf(v.getDataAdmissao()));
            else ps.setNull(6, Types.DATE);
            ps.setString(7, v.getStatus());
            ps.setInt(8, v.getIdVinculo());
            ps.executeUpdate();
        }
    }

    /** Encerra o vínculo (desligamento). */
    public void desligar(int idVinculo, LocalDate dataFim, String motivo) throws SQLException {
        String sql = "UPDATE vinculo_empregaticio SET "
                   + "data_desligamento=?, status='DESLIGADO', motivo_desligamento=? "
                   + "WHERE id_vinculo=?";
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(dataFim != null ? dataFim : LocalDate.now()));
            ps.setString(2, motivo);
            ps.setInt(3, idVinculo);
            ps.executeUpdate();
        }
    }

    private Vinculo mapear(ResultSet rs) throws SQLException {
        Vinculo v = new Vinculo();
        v.setIdVinculo(rs.getInt("id_vinculo"));
        v.setIdPessoa(rs.getInt("idpessoa"));
        try { v.setTipoFuncionario(TipoFuncionario.valueOf(rs.getString("tipo_funcionario"))); }
        catch (Exception ignored) { }
        v.setCargo(rs.getString("cargo"));
        try { v.setTipoAtividadeRural(TipoAtividadeRural.valueOf(rs.getString("tipo_atividade_rural"))); }
        catch (Exception ignored) { }
        Date adm = rs.getDate("data_admissao");
        v.setDataAdmissao(adm != null ? adm.toLocalDate() : null);
        Date des = rs.getDate("data_desligamento");
        v.setDataDesligamento(des != null ? des.toLocalDate() : null);
        v.setSalarioMensal(rs.getBigDecimal("salario_mensal"));
        v.setValorHoraExtra(rs.getBigDecimal("valor_hora_extra"));
        v.setCargaHorariaDiaria(rs.getInt("carga_horaria_diaria"));
        v.setStatus(rs.getString("status"));
        v.setMotivoDesligamento(rs.getString("motivo_desligamento"));
        return v;
    }
}
