package Model.Dao;

import Model.Model.Maquina;
import Util.PostgresConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** CRUD de maquinário (trator/implemento/veículo) com custos. */
public class MaquinaDAO {

    /** custo_hora efetivo = soma dos componentes quando informados; senão o valor direto. */
    private BigDecimal custoHoraEfetivo(Maquina m) {
        BigDecimal soma = nz(m.getCombustivelHora()).add(nz(m.getManutencaoHora())).add(nz(m.getDepreciacaoHora()));
        return soma.signum() > 0 ? soma : nz(m.getCustoHora());
    }

    public void inserir(Maquina m) throws SQLException {
        String sql = "INSERT INTO maquina (nome, tipo, marca, identificacao, custo_hora, custo_km, "
                + "combustivel_hora, manutencao_hora, depreciacao_hora, situacao) VALUES (?,?,?,?,?,?,?,?,?,?)";
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement st = c.prepareStatement(sql)) {
            preencher(st, m);
            st.executeUpdate();
        }
    }

    public void atualizar(Maquina m) throws SQLException {
        String sql = "UPDATE maquina SET nome=?, tipo=?, marca=?, identificacao=?, custo_hora=?, custo_km=?, "
                + "combustivel_hora=?, manutencao_hora=?, depreciacao_hora=?, situacao=? WHERE idmaquina=?";
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement st = c.prepareStatement(sql)) {
            preencher(st, m);
            st.setInt(11, m.getIdMaquina());
            st.executeUpdate();
        }
    }

    private void preencher(PreparedStatement st, Maquina m) throws SQLException {
        st.setString(1, m.getNome());
        st.setString(2, m.getTipo());
        st.setString(3, m.getMarca());
        st.setString(4, m.getIdentificacao());
        st.setBigDecimal(5, custoHoraEfetivo(m));
        st.setBigDecimal(6, nz(m.getCustoKm()));
        st.setBigDecimal(7, nz(m.getCombustivelHora()));
        st.setBigDecimal(8, nz(m.getManutencaoHora()));
        st.setBigDecimal(9, nz(m.getDepreciacaoHora()));
        st.setBoolean(10, m.isSituacao());
    }

    public void definirSituacao(int idMaquina, boolean situacao) throws SQLException {
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement st = c.prepareStatement("UPDATE maquina SET situacao=? WHERE idmaquina=?")) {
            st.setBoolean(1, situacao);
            st.setInt(2, idMaquina);
            st.executeUpdate();
        }
    }

    public Maquina buscarPorId(int id) throws SQLException {
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement st = c.prepareStatement("SELECT * FROM maquina WHERE idmaquina=?")) {
            st.setInt(1, id);
            ResultSet rs = st.executeQuery();
            return rs.next() ? map(rs) : null;
        }
    }

    public List<Maquina> listar(boolean somenteAtivas) throws SQLException {
        String sql = "SELECT * FROM maquina" + (somenteAtivas ? " WHERE situacao=true" : "") + " ORDER BY nome";
        List<Maquina> l = new ArrayList<>();
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement st = c.prepareStatement(sql);
             ResultSet rs = st.executeQuery()) {
            while (rs.next()) l.add(map(rs));
        }
        return l;
    }

    private Maquina map(ResultSet rs) throws SQLException {
        Maquina m = new Maquina();
        m.setIdMaquina(rs.getInt("idmaquina"));
        m.setNome(rs.getString("nome"));
        m.setTipo(rs.getString("tipo"));
        m.setMarca(rs.getString("marca"));
        m.setIdentificacao(rs.getString("identificacao"));
        m.setCustoHora(rs.getBigDecimal("custo_hora"));
        m.setCustoKm(rs.getBigDecimal("custo_km"));
        m.setCombustivelHora(rs.getBigDecimal("combustivel_hora"));
        m.setManutencaoHora(rs.getBigDecimal("manutencao_hora"));
        m.setDepreciacaoHora(rs.getBigDecimal("depreciacao_hora"));
        m.setSituacao(rs.getBoolean("situacao"));
        return m;
    }

    private static BigDecimal nz(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }
}
