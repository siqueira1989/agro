package Model.Dao;

import Model.Model.PontoEletronico;
import Model.Model.TipoAtividadeRural;
import Model.Model.Vinculo;
import Service.CalculoPontoRural;
import Util.HashUtil;
import Util.PostgresConnection;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class PontoEletronicoDAO {

    private static final int JORNADA_MIN = 480; // 8h × 60

    private final VinculoDAO vinculoDAO = new VinculoDAO();

    /** Resolve o id_vinculo a que uma data pertence (ou null). */
    private Integer resolverVinculoId(int idpessoa, LocalDate data) throws SQLException {
        Vinculo v = vinculoDAO.buscarPorData(idpessoa, data);
        return v != null ? v.getIdVinculo() : null;
    }

    /** Configuração rural do funcionário usada no cálculo do ponto. */
    private static final class CfgRural {
        TipoAtividadeRural atividade = TipoAtividadeRural.LAVOURA;
        int jornadaMin = JORNADA_MIN;
    }

    /** Carrega tipo de atividade e jornada (em minutos) do funcionário CLT. */
    private CfgRural carregarCfg(Connection c, int idpessoa) throws SQLException {
        CfgRural cfg = new CfgRural();
        String sql = "SELECT tipo_atividade_rural, cargahorariadiaria "
                   + "FROM funcionarioclt WHERE idpessoa=?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idpessoa);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    try { cfg.atividade = TipoAtividadeRural.valueOf(rs.getString("tipo_atividade_rural")); }
                    catch (Exception ignored) { }
                    int jh = rs.getInt("cargahorariadiaria");
                    if (jh > 0) cfg.jornadaMin = jh * 60;
                }
            }
        }
        return cfg;
    }

    /** Hash SHA-256 de autenticidade do registro (Portaria 671). */
    private String gerarHash(PontoEletronico p) {
        String canonical = String.join("|",
                String.valueOf(p.getIdFuncionario()),
                String.valueOf(p.getDataRegistro()),
                String.valueOf(p.getEntrada1()), String.valueOf(p.getSaida1()),
                String.valueOf(p.getEntrada2()), String.valueOf(p.getSaida2()),
                String.valueOf(p.getTotalMinutos()), String.valueOf(p.getMinutosNoturnos()),
                String.valueOf(p.getRegistradoPor()));
        return HashUtil.sha256(canonical);
    }

    public PontoEletronico registrarMarcacao(int idFuncionario, LocalDate data,
                                             String campo, LocalTime hora)
            throws SQLException {

        PostgresConnection pc = new PostgresConnection();
        try (Connection c = pc.getConnection()) {

            // garante que o registro do dia existe
            String upsert = "INSERT INTO ponto_eletronico (idpessoa, dataregistro) "
                    + "VALUES (?, ?) ON CONFLICT (idpessoa, dataregistro) DO NOTHING";
            try (PreparedStatement ps = c.prepareStatement(upsert)) {
                ps.setInt(1, idFuncionario);
                ps.setDate(2, Date.valueOf(data));
                ps.executeUpdate();
            }

            // atualiza somente o campo solicitado (whitelist explícita)
            String coluna = switch (campo) {
                case "entrada1" -> "entrada1";
                case "saida1"   -> "saida1";
                case "entrada2" -> "entrada2";
                case "saida2"   -> "saida2";
                default -> throw new IllegalArgumentException("Campo inválido: " + campo);
            };
            String upd = "UPDATE ponto_eletronico SET " + coluna + " = ? "
                    + "WHERE idpessoa = ? AND dataregistro = ?";
            try (PreparedStatement ps = c.prepareStatement(upd)) {
                ps.setTime(1, Time.valueOf(hora));
                ps.setInt(2, idFuncionario);
                ps.setDate(3, Date.valueOf(data));
                ps.executeUpdate();
            }

            recalcularTotais(c, idFuncionario, data);
            return buscarPorDia(idFuncionario, data);
        }
    }

    public void salvarRegistroCompleto(PontoEletronico p) throws SQLException {
        PostgresConnection pc = new PostgresConnection();
        try (Connection c = pc.getConnection()) {
            CfgRural cfg = carregarCfg(c, p.getIdFuncionario());
            CalculoPontoRural.calcular(p, cfg.atividade, cfg.jornadaMin);
            p.setHashAutenticidade(gerarHash(p));
            Integer idVinculo = resolverVinculoId(p.getIdFuncionario(), p.getDataRegistro());
            String sql = "INSERT INTO ponto_eletronico "
                    + "(idpessoa, dataregistro, entrada1, saida1, entrada2, saida2, "
                    + " total_minutos, extra_minutos, minutos_noturnos, observacao, "
                    + " ip_origem, user_agent, registrado_por, hash_autenticidade, id_vinculo) "
                    + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) "
                    + "ON CONFLICT (idpessoa, dataregistro) DO UPDATE SET "
                    + "entrada1=EXCLUDED.entrada1, saida1=EXCLUDED.saida1, "
                    + "entrada2=EXCLUDED.entrada2, saida2=EXCLUDED.saida2, "
                    + "total_minutos=EXCLUDED.total_minutos, "
                    + "extra_minutos=EXCLUDED.extra_minutos, "
                    + "minutos_noturnos=EXCLUDED.minutos_noturnos, "
                    + "observacao=EXCLUDED.observacao, "
                    + "ip_origem=EXCLUDED.ip_origem, user_agent=EXCLUDED.user_agent, "
                    + "registrado_por=EXCLUDED.registrado_por, "
                    + "hash_autenticidade=EXCLUDED.hash_autenticidade, "
                    + "id_vinculo=EXCLUDED.id_vinculo, "
                    + "atualizado_em=NOW()";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, p.getIdFuncionario());
                ps.setDate(2, Date.valueOf(p.getDataRegistro()));
                ps.setTime(3, p.getEntrada1() != null ? Time.valueOf(p.getEntrada1()) : null);
                ps.setTime(4, p.getSaida1()   != null ? Time.valueOf(p.getSaida1())   : null);
                ps.setTime(5, p.getEntrada2() != null ? Time.valueOf(p.getEntrada2()) : null);
                ps.setTime(6, p.getSaida2()   != null ? Time.valueOf(p.getSaida2())   : null);
                ps.setInt(7, p.getTotalMinutos());
                ps.setInt(8, p.getExtraMinutos());
                ps.setInt(9, p.getMinutosNoturnos());
                ps.setString(10, p.getObservacao());
                ps.setString(11, p.getIpOrigem());
                ps.setString(12, p.getUserAgent());
                if (p.getRegistradoPor() != null) ps.setInt(13, p.getRegistradoPor());
                else ps.setNull(13, Types.INTEGER);
                ps.setString(14, p.getHashAutenticidade());
                if (idVinculo != null) ps.setInt(15, idVinculo); else ps.setNull(15, Types.INTEGER);
                ps.executeUpdate();
            }
        }
    }

    public List<PontoEletronico> listarPorMes(int idFuncionario, String periodo)
            throws SQLException {
        String sql = "SELECT pe.*, p.nomepessoa, p.emailpessoa "
                + "FROM ponto_eletronico pe "
                + "JOIN pessoa p ON p.idpessoa = pe.idpessoa "
                + "WHERE pe.idpessoa = ? "
                + "AND TO_CHAR(pe.dataregistro,'YYYY-MM') = ? "
                + "ORDER BY pe.dataregistro";
        List<PontoEletronico> lista = new ArrayList<>();
        PostgresConnection pc = new PostgresConnection();
        try (Connection c = pc.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idFuncionario);
            ps.setString(2, periodo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapRow(rs));
            }
        }
        return lista;
    }

    public PontoEletronico buscarPorDia(int idFuncionario, LocalDate data)
            throws SQLException {
        String sql = "SELECT pe.*, p.nomepessoa, p.emailpessoa "
                + "FROM ponto_eletronico pe "
                + "JOIN pessoa p ON p.idpessoa = pe.idpessoa "
                + "WHERE pe.idpessoa = ? AND pe.dataregistro = ?";
        PostgresConnection pc = new PostgresConnection();
        try (Connection c = pc.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idFuncionario);
            ps.setDate(2, Date.valueOf(data));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    public int somarMinutosExtras(int idFuncionario, String periodo) throws SQLException {
        String sql = "SELECT COALESCE(SUM(extra_minutos),0) FROM ponto_eletronico "
                + "WHERE idpessoa = ? AND TO_CHAR(dataregistro,'YYYY-MM') = ?";
        PostgresConnection pc = new PostgresConnection();
        try (Connection c = pc.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idFuncionario);
            ps.setString(2, periodo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    public int contarDiasPresentes(int idFuncionario, java.time.LocalDate inicio,
                                    java.time.LocalDate fim) throws SQLException {
        String sql = "SELECT COUNT(*) FROM ponto_eletronico "
                + "WHERE idpessoa = ? AND dataregistro BETWEEN ? AND ?";
        PostgresConnection pc = new PostgresConnection();
        try (Connection c = pc.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idFuncionario);
            ps.setDate(2, Date.valueOf(inicio));
            ps.setDate(3, Date.valueOf(fim));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    /**
     * Atualiza um registro de ponto existente pelo idponto, recalculando
     * total/extra. Permite corrigir horários e até a data (respeitando a
     * UNIQUE idpessoa+dataregistro).
     */
    public void atualizarPorId(PontoEletronico p) throws SQLException {
        PostgresConnection pc = new PostgresConnection();
        try (Connection c = pc.getConnection()) {
            CfgRural cfg = carregarCfg(c, p.getIdFuncionario());
            CalculoPontoRural.calcular(p, cfg.atividade, cfg.jornadaMin);
            p.setHashAutenticidade(gerarHash(p));
            Integer idVinculo = resolverVinculoId(p.getIdFuncionario(), p.getDataRegistro());
            String sql = "UPDATE ponto_eletronico SET "
                    + "dataregistro=?, entrada1=?, saida1=?, entrada2=?, saida2=?, "
                    + "total_minutos=?, extra_minutos=?, minutos_noturnos=?, observacao=?, "
                    + "ip_origem=?, user_agent=?, registrado_por=?, hash_autenticidade=?, "
                    + "id_vinculo=?, atualizado_em=NOW() "
                    + "WHERE idponto=?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setDate(1, Date.valueOf(p.getDataRegistro()));
                ps.setTime(2, p.getEntrada1() != null ? Time.valueOf(p.getEntrada1()) : null);
                ps.setTime(3, p.getSaida1()   != null ? Time.valueOf(p.getSaida1())   : null);
                ps.setTime(4, p.getEntrada2() != null ? Time.valueOf(p.getEntrada2()) : null);
                ps.setTime(5, p.getSaida2()   != null ? Time.valueOf(p.getSaida2())   : null);
                ps.setInt(6, p.getTotalMinutos());
                ps.setInt(7, p.getExtraMinutos());
                ps.setInt(8, p.getMinutosNoturnos());
                ps.setString(9, p.getObservacao());
                ps.setString(10, p.getIpOrigem());
                ps.setString(11, p.getUserAgent());
                if (p.getRegistradoPor() != null) ps.setInt(12, p.getRegistradoPor());
                else ps.setNull(12, Types.INTEGER);
                ps.setString(13, p.getHashAutenticidade());
                if (idVinculo != null) ps.setInt(14, idVinculo); else ps.setNull(14, Types.INTEGER);
                ps.setInt(15, p.getIdPonto());
                ps.executeUpdate();
            }
        }
    }

    public void excluir(int idPonto) throws SQLException {
        PostgresConnection pc = new PostgresConnection();
        try (Connection c = pc.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "DELETE FROM ponto_eletronico WHERE idponto = ?")) {
            ps.setInt(1, idPonto);
            ps.executeUpdate();
        }
    }

    // ── helpers privados ──────────────────────────────────────────────────────

    private void recalcularTotais(Connection c, int idFuncionario, LocalDate data)
            throws SQLException {
        String sel = "SELECT entrada1, saida1, entrada2, saida2 "
                + "FROM ponto_eletronico WHERE idpessoa=? AND dataregistro=?";
        try (PreparedStatement ps = c.prepareStatement(sel)) {
            ps.setInt(1, idFuncionario);
            ps.setDate(2, Date.valueOf(data));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    PontoEletronico p = new PontoEletronico();
                    p.setIdFuncionario(idFuncionario);
                    p.setEntrada1(toLocalTime(rs.getTime("entrada1")));
                    p.setSaida1(toLocalTime(rs.getTime("saida1")));
                    p.setEntrada2(toLocalTime(rs.getTime("entrada2")));
                    p.setSaida2(toLocalTime(rs.getTime("saida2")));
                    CfgRural cfg = carregarCfg(c, idFuncionario);
                    CalculoPontoRural.calcular(p, cfg.atividade, cfg.jornadaMin);

                    String upd = "UPDATE ponto_eletronico SET total_minutos=?, extra_minutos=?, "
                            + "minutos_noturnos=? WHERE idpessoa=? AND dataregistro=?";
                    try (PreparedStatement u = c.prepareStatement(upd)) {
                        u.setInt(1, p.getTotalMinutos());
                        u.setInt(2, p.getExtraMinutos());
                        u.setInt(3, p.getMinutosNoturnos());
                        u.setInt(4, idFuncionario);
                        u.setDate(5, Date.valueOf(data));
                        u.executeUpdate();
                    }
                }
            }
        }
    }

    private PontoEletronico mapRow(ResultSet rs) throws SQLException {
        PontoEletronico p = new PontoEletronico();
        p.setIdPonto(rs.getInt("idponto"));
        p.setIdFuncionario(rs.getInt("idpessoa"));          // coluna DB = idpessoa
        p.setNomeFuncionario(rs.getString("nomepessoa"));
        p.setEmailFuncionario(rs.getString("emailpessoa"));
        p.setDataRegistro(rs.getDate("dataregistro").toLocalDate());
        p.setEntrada1(toLocalTime(rs.getTime("entrada1")));
        p.setSaida1(toLocalTime(rs.getTime("saida1")));
        p.setEntrada2(toLocalTime(rs.getTime("entrada2")));
        p.setSaida2(toLocalTime(rs.getTime("saida2")));
        p.setTotalMinutos(rs.getInt("total_minutos"));
        p.setExtraMinutos(rs.getInt("extra_minutos"));
        p.setMinutosNoturnos(rs.getInt("minutos_noturnos"));
        p.setObservacao(rs.getString("observacao"));
        p.setLatitude(rs.getBigDecimal("latitude"));
        p.setLongitude(rs.getBigDecimal("longitude"));
        p.setIpOrigem(rs.getString("ip_origem"));
        p.setUserAgent(rs.getString("user_agent"));
        p.setHashAutenticidade(rs.getString("hash_autenticidade"));
        int rp = rs.getInt("registrado_por");
        p.setRegistradoPor(rs.wasNull() ? null : rp);
        return p;
    }

    private LocalTime toLocalTime(Time t) {
        return t != null ? t.toLocalTime() : null;
    }
}
