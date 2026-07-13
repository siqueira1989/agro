package Model.Dao;

import Model.Model.FechamentoFolha;
import Util.PostgresConnection;

import java.sql.*;

public class FechamentoFolhaDAO {

    private final VinculoDAO vinculoDAO = new VinculoDAO();

    public void salvar(FechamentoFolha f) throws SQLException {
        // Resolve o vínculo do período (primeiro dia do mês)
        Integer idVinculo = null;
        try {
            Model.Model.Vinculo vinc = vinculoDAO.buscarPorData(
                    f.getIdFuncionario(), java.time.LocalDate.parse(f.getPeriodo() + "-01"));
            if (vinc != null) idVinculo = vinc.getIdVinculo();
        } catch (Exception ignored) { }
        String sql = "INSERT INTO fechamento_folha_ponto "
                + "(idpessoa, periodo, salario_base, jornada_horas_dia, dias_uteis, "
                + " dias_trabalhados, faltas_injustificadas, total_minutos_extras, "
                + " valor_horas_extras, desconto_faltas, total_vales, salario_bruto, salario_liquido, "
                + " desconto_dsr, valor_adicional_noturno, valor_extra_100, "
                + " minutos_noturnos, minutos_extra_100, id_vinculo, "
                + " valor_producao, valor_empreita) "
                + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) "
                + "ON CONFLICT (idpessoa, periodo) DO UPDATE SET "
                + "salario_base=EXCLUDED.salario_base, "
                + "jornada_horas_dia=EXCLUDED.jornada_horas_dia, "
                + "dias_uteis=EXCLUDED.dias_uteis, "
                + "dias_trabalhados=EXCLUDED.dias_trabalhados, "
                + "faltas_injustificadas=EXCLUDED.faltas_injustificadas, "
                + "total_minutos_extras=EXCLUDED.total_minutos_extras, "
                + "valor_horas_extras=EXCLUDED.valor_horas_extras, "
                + "desconto_faltas=EXCLUDED.desconto_faltas, "
                + "total_vales=EXCLUDED.total_vales, "
                + "salario_bruto=EXCLUDED.salario_bruto, "
                + "salario_liquido=EXCLUDED.salario_liquido, "
                + "desconto_dsr=EXCLUDED.desconto_dsr, "
                + "valor_adicional_noturno=EXCLUDED.valor_adicional_noturno, "
                + "valor_extra_100=EXCLUDED.valor_extra_100, "
                + "minutos_noturnos=EXCLUDED.minutos_noturnos, "
                + "minutos_extra_100=EXCLUDED.minutos_extra_100, "
                + "id_vinculo=EXCLUDED.id_vinculo, "
                + "valor_producao=EXCLUDED.valor_producao, "
                + "valor_empreita=EXCLUDED.valor_empreita, "
                + "fechado_em=NOW()";
        PostgresConnection pc = new PostgresConnection();
        try (Connection c = pc.getConnection();
             PreparedStatement ps = c.prepareStatement(sql,
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, f.getIdFuncionario());
            ps.setString(2, f.getPeriodo());
            ps.setBigDecimal(3, f.getSalarioBase());
            ps.setInt(4, f.getJornadaHorasDia());
            ps.setInt(5, f.getDiasUteis());
            ps.setInt(6, f.getDiasTrabalhados());
            ps.setInt(7, f.getFaltasInjustificadas());
            ps.setInt(8, f.getTotalMinutosExtras());
            ps.setBigDecimal(9,  f.getValorHorasExtras());
            ps.setBigDecimal(10, f.getDescontoFaltas());
            ps.setBigDecimal(11, f.getTotalVales());
            ps.setBigDecimal(12, f.getSalarioBruto());
            ps.setBigDecimal(13, f.getSalarioLiquido());
            ps.setBigDecimal(14, f.getDescontoDsr());
            ps.setBigDecimal(15, f.getValorAdicionalNoturno());
            ps.setBigDecimal(16, f.getValorExtra100());
            ps.setInt(17, f.getMinutosNoturnos());
            ps.setInt(18, f.getMinutosExtra100());
            if (idVinculo != null) ps.setInt(19, idVinculo); else ps.setNull(19, java.sql.Types.INTEGER);
            ps.setBigDecimal(20, f.getValorProducao() != null ? f.getValorProducao() : java.math.BigDecimal.ZERO);
            ps.setBigDecimal(21, f.getValorEmpreita() != null ? f.getValorEmpreita() : java.math.BigDecimal.ZERO);
            ps.executeUpdate();
            try (ResultSet gk = ps.getGeneratedKeys()) {
                if (gk.next()) f.setIdFechamento(gk.getInt(1));
            }
        }
    }

    public FechamentoFolha buscarPorPeriodo(int idFuncionario, String periodo)
            throws SQLException {
        String sql = "SELECT ff.*, p.nomepessoa "
                + "FROM fechamento_folha_ponto ff "
                + "JOIN pessoa p ON p.idpessoa = ff.idpessoa "
                + "WHERE ff.idpessoa=? AND ff.periodo=?";
        PostgresConnection pc = new PostgresConnection();
        try (Connection c = pc.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idFuncionario);
            ps.setString(2, periodo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    private FechamentoFolha mapRow(ResultSet rs) throws SQLException {
        FechamentoFolha f = new FechamentoFolha();
        f.setIdFechamento(rs.getInt("idfechamento"));
        f.setIdFuncionario(rs.getInt("idpessoa"));          // coluna DB = idpessoa
        f.setNomeFuncionario(rs.getString("nomepessoa"));
        f.setPeriodo(rs.getString("periodo"));
        f.setSalarioBase(rs.getBigDecimal("salario_base"));
        f.setJornadaHorasDia(rs.getInt("jornada_horas_dia"));
        f.setDiasUteis(rs.getInt("dias_uteis"));
        f.setDiasTrabalhados(rs.getInt("dias_trabalhados"));
        f.setFaltasInjustificadas(rs.getInt("faltas_injustificadas"));
        f.setTotalMinutosExtras(rs.getInt("total_minutos_extras"));
        f.setValorHorasExtras(rs.getBigDecimal("valor_horas_extras"));
        f.setDescontoFaltas(rs.getBigDecimal("desconto_faltas"));
        f.setTotalVales(rs.getBigDecimal("total_vales"));
        f.setSalarioBruto(rs.getBigDecimal("salario_bruto"));
        f.setSalarioLiquido(rs.getBigDecimal("salario_liquido"));
        f.setDescontoDsr(rs.getBigDecimal("desconto_dsr"));
        f.setValorAdicionalNoturno(rs.getBigDecimal("valor_adicional_noturno"));
        f.setValorExtra100(rs.getBigDecimal("valor_extra_100"));
        f.setMinutosNoturnos(rs.getInt("minutos_noturnos"));
        f.setMinutosExtra100(rs.getInt("minutos_extra_100"));
        f.setValorProducao(rs.getBigDecimal("valor_producao"));
        f.setValorEmpreita(rs.getBigDecimal("valor_empreita"));
        return f;
    }
}
