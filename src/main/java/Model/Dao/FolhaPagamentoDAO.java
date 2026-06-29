package Model.Dao;

import Model.Model.FolhaPagamento;
import Model.Model.TipoFuncionario;
import Util.PostgresConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class FolhaPagamentoDAO {

    public void inserirLancamento(FolhaPagamento fp, Connection conn) throws SQLException {
        Integer idVinculo = resolverVinculo(conn, fp.getIdFuncionario(), fp.getPeriodo());
        String sql = "INSERT INTO folha_pagamento (idfuncionario, periodo, tipofuncionario, valorcalculado, id_vinculo) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, fp.getIdFuncionario());
            stmt.setString(2, fp.getPeriodo());
            stmt.setObject(3, fp.getTipoFuncionario().name(), java.sql.Types.OTHER);
            stmt.setBigDecimal(4, fp.getValorCalculado());
            if (idVinculo != null) stmt.setInt(5, idVinculo); else stmt.setNull(5, java.sql.Types.INTEGER);
            stmt.executeUpdate();
        }
    }

    /** Resolve o id_vinculo do período (primeiro dia do mês), na mesma conexão. */
    private Integer resolverVinculo(Connection conn, int idpessoa, String periodo) throws SQLException {
        String sql = "SELECT id_vinculo FROM vinculo_empregaticio WHERE idpessoa=? "
                + "AND (data_admissao IS NULL OR data_admissao <= to_date(?,'YYYY-MM-DD')) "
                + "AND (data_desligamento IS NULL OR data_desligamento >= to_date(?,'YYYY-MM-DD')) "
                + "ORDER BY data_admissao DESC NULLS LAST, id_vinculo DESC LIMIT 1";
        try (PreparedStatement vs = conn.prepareStatement(sql)) {
            vs.setInt(1, idpessoa);
            vs.setString(2, periodo + "-01");
            vs.setString(3, periodo + "-01");
            try (ResultSet rs = vs.executeQuery()) {
                if (rs.next()) { int id = rs.getInt(1); return rs.wasNull() ? null : id; }
            }
        }
        return null;
    }

    public List<FolhaPagamento> listByPeriodo(String periodo) throws SQLException {
        List<FolhaPagamento> lista = new ArrayList<>();
        String sql = "SELECT fp.idfolha, fp.idfuncionario, fp.periodo, fp.tipofuncionario, "
                + "fp.valorcalculado, fp.datageracao, p.nomepessoa "
                + "FROM folha_pagamento fp "
                + "JOIN pessoa p ON p.idpessoa = fp.idfuncionario "
                + "WHERE fp.periodo=? ORDER BY p.nomepessoa";
        try (Connection conn = new PostgresConnection().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, periodo);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        }
        return lista;
    }

    public List<FolhaPagamento> listByFuncionario(int idFuncionario) throws SQLException {
        List<FolhaPagamento> lista = new ArrayList<>();
        String sql = "SELECT fp.idfolha, fp.idfuncionario, fp.periodo, fp.tipofuncionario, "
                + "fp.valorcalculado, fp.datageracao, p.nomepessoa "
                + "FROM folha_pagamento fp "
                + "JOIN pessoa p ON p.idpessoa = fp.idfuncionario "
                + "WHERE fp.idfuncionario=? ORDER BY fp.periodo DESC";
        try (Connection conn = new PostgresConnection().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idFuncionario);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        }
        return lista;
    }

    public boolean existeFolhaPeriodo(int idFuncionario, String periodo) throws SQLException {
        String sql = "SELECT 1 FROM folha_pagamento WHERE idfuncionario=? AND periodo=?";
        try (Connection conn = new PostgresConnection().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idFuncionario);
            stmt.setString(2, periodo);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void deletarPorPeriodo(String periodo, Connection conn) throws SQLException {
        String sql = "DELETE FROM folha_pagamento WHERE periodo=?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, periodo);
            stmt.executeUpdate();
        }
    }

    private FolhaPagamento mapear(ResultSet rs) throws SQLException {
        FolhaPagamento fp = new FolhaPagamento();
        fp.setIdFolha(rs.getInt("idfolha"));
        fp.setIdFuncionario(rs.getInt("idfuncionario"));
        fp.setNomeFuncionario(rs.getString("nomepessoa"));
        fp.setPeriodo(rs.getString("periodo"));
        fp.setTipoFuncionario(TipoFuncionario.valueOf(rs.getString("tipofuncionario")));
        fp.setValorCalculado(rs.getBigDecimal("valorcalculado"));
        Timestamp ts = rs.getTimestamp("datageracao");
        if (ts != null) fp.setDataGeracao(ts.toLocalDateTime());
        return fp;
    }
}
