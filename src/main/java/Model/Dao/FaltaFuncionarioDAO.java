package Model.Dao;

import Model.Model.FaltaFuncionario;
import Util.PostgresConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FaltaFuncionarioDAO {

    private final VinculoDAO vinculoDAO = new VinculoDAO();

    public void registrar(FaltaFuncionario f) throws SQLException {
        Model.Model.Vinculo vinc = vinculoDAO.buscarPorData(f.getIdFuncionario(), f.getDataFalta());
        Integer idVinculo = vinc != null ? vinc.getIdVinculo() : null;
        String sql = "INSERT INTO falta_funcionario (idpessoa, datafalta, justificada, motivo, id_vinculo) "
                + "VALUES (?,?,?,?,?) "
                + "ON CONFLICT (idpessoa, datafalta) DO UPDATE SET "
                + "justificada=EXCLUDED.justificada, motivo=EXCLUDED.motivo, id_vinculo=EXCLUDED.id_vinculo";
        PostgresConnection pc = new PostgresConnection();
        try (Connection c = pc.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, f.getIdFuncionario());
            ps.setDate(2, Date.valueOf(f.getDataFalta()));
            ps.setBoolean(3, f.isJustificada());
            ps.setString(4, f.getMotivo());
            if (idVinculo != null) ps.setInt(5, idVinculo); else ps.setNull(5, Types.INTEGER);
            ps.executeUpdate();
        }
    }

    public List<FaltaFuncionario> listarPorMes(int idFuncionario, String periodo)
            throws SQLException {
        String sql = "SELECT ff.*, p.nomepessoa "
                + "FROM falta_funcionario ff "
                + "JOIN pessoa p ON p.idpessoa = ff.idpessoa "
                + "WHERE ff.idpessoa = ? AND TO_CHAR(ff.datafalta,'YYYY-MM') = ? "
                + "ORDER BY ff.datafalta";
        List<FaltaFuncionario> lista = new ArrayList<>();
        PostgresConnection pc = new PostgresConnection();
        try (Connection c = pc.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idFuncionario);
            ps.setString(2, periodo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    FaltaFuncionario f = new FaltaFuncionario();
                    f.setIdFalta(rs.getInt("idfalta"));
                    f.setIdFuncionario(rs.getInt("idpessoa"));      // coluna DB = idpessoa
                    f.setNomeFuncionario(rs.getString("nomepessoa"));
                    f.setDataFalta(rs.getDate("datafalta").toLocalDate());
                    f.setJustificada(rs.getBoolean("justificada"));
                    f.setMotivo(rs.getString("motivo"));
                    lista.add(f);
                }
            }
        }
        return lista;
    }

    /**
     * Faltas injustificadas na competência.
     *
     * <p>O filtro era {@code TO_CHAR(datafalta,'YYYY-MM') = ?}. Aplicar função
     * sobre a coluna impede o uso de índice: o PostgreSQL precisava ler a tabela
     * inteira e converter cada data. Trocado por um intervalo de datas, que usa
     * o índice {@code idx_falta_pessoa_data} criado na migração V21.</p>
     */
    public int contarInjustificadas(int idFuncionario, String periodo) throws SQLException {
        java.time.YearMonth ym = java.time.YearMonth.parse(periodo);
        String sql = "SELECT COUNT(*) FROM falta_funcionario "
                + "WHERE idpessoa=? AND datafalta >= ? AND datafalta <= ? AND justificada=false";
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idFuncionario);
            ps.setDate(2, java.sql.Date.valueOf(ym.atDay(1)));
            ps.setDate(3, java.sql.Date.valueOf(ym.atEndOfMonth()));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    /**
     * Quantas semanas da competência tiveram pelo menos uma falta injustificada.
     *
     * <p>Cada uma dessas semanas faz perder o descanso semanal remunerado. O
     * cálculo do CLT urbano não descontava DSR nenhum (P1-9); o rural já
     * descontava, mas com a contagem feita em Java. A regra passa a ser a mesma
     * nos dois, resolvida aqui pelo banco com {@code date_trunc('week', ...)},
     * que agrupa de segunda a domingo.</p>
     */
    public int contarSemanasComFaltaInjustificada(int idFuncionario, String periodo) throws SQLException {
        java.time.YearMonth ym = java.time.YearMonth.parse(periodo);
        String sql = "SELECT COUNT(DISTINCT date_trunc('week', datafalta)) "
                   + "FROM falta_funcionario "
                   + "WHERE idpessoa=? AND datafalta >= ? AND datafalta <= ? AND justificada=false";
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idFuncionario);
            ps.setDate(2, java.sql.Date.valueOf(ym.atDay(1)));
            ps.setDate(3, java.sql.Date.valueOf(ym.atEndOfMonth()));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    public void excluir(int idFalta) throws SQLException {
        PostgresConnection pc = new PostgresConnection();
        try (Connection c = pc.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "DELETE FROM falta_funcionario WHERE idfalta = ?")) {
            ps.setInt(1, idFalta);
            ps.executeUpdate();
        }
    }
}
