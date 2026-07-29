package Model.Dao;

import Util.PostgresConnection;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Movimentações de estoque. As duas operações rodam em transação e atualizam,
 * de forma atômica, o saldo e o preço médio do insumo:
 *   ENTRADA → preço médio ponderado + soma ao saldo;
 *   SAÍDA   → bloqueia se saldo insuficiente (estoque nunca fica negativo).
 */
public class EstoqueMovimentoDAO {

    /**
     * Registra entrada: recalcula o preço médio ponderado e incrementa o saldo.
     *   novo_medio = (qtd_atual*medio_atual + qtd_entrada*preco_entrada) / (qtd_atual+qtd_entrada)
     */
    public void registrarEntrada(int idInsumo, BigDecimal qtd, BigDecimal precoUnit,
                                 Integer idParceiro, LocalDate data, String obs) throws SQLException {
        try (Connection c = new PostgresConnection().getConnection()) {
            c.setAutoCommit(false);
            try {
                BigDecimal[] atual = lerSaldoPreco(c, idInsumo);   // [saldo, precoMedio]
                BigDecimal saldoAtual = atual[0];
                BigDecimal medioAtual = atual[1];

                BigDecimal novoSaldo = saldoAtual.add(qtd);
                BigDecimal novoMedio;
                if (novoSaldo.compareTo(BigDecimal.ZERO) == 0) {
                    novoMedio = precoUnit;
                } else {
                    novoMedio = saldoAtual.multiply(medioAtual)
                            .add(qtd.multiply(precoUnit))
                            .divide(novoSaldo, 2, RoundingMode.HALF_UP);
                }

                atualizarSaldoPreco(c, idInsumo, novoSaldo, novoMedio);
                inserirMovimento(c, idInsumo, "ENTRADA", qtd, precoUnit, idParceiro, data, obs, novoSaldo);

                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    /**
     * Registra saída: bloqueia se a quantidade solicitada exceder o saldo.
     * O preço médio não muda numa saída.
     */
    public void registrarSaida(int idInsumo, BigDecimal qtd, LocalDate data, String obs)
            throws SQLException, EstoqueInsuficienteException {
        try (Connection c = new PostgresConnection().getConnection()) {
            c.setAutoCommit(false);
            try {
                BigDecimal[] atual = lerSaldoPreco(c, idInsumo);
                BigDecimal saldoAtual = atual[0];

                if (saldoAtual.compareTo(qtd) < 0) {
                    throw new EstoqueInsuficienteException(
                        "Estoque insuficiente: disponível " + saldoAtual.stripTrailingZeros().toPlainString()
                        + ", solicitado " + qtd.stripTrailingZeros().toPlainString() + ".");
                }

                BigDecimal novoSaldo = saldoAtual.subtract(qtd);
                atualizarSaldoPreco(c, idInsumo, novoSaldo, atual[1]);
                inserirMovimento(c, idInsumo, "SAIDA", qtd, null, null, data, obs, novoSaldo);

                c.commit();
            } catch (SQLException | EstoqueInsuficienteException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    /**
     * Saída usando uma conexão já aberta (participa da transação do chamador —
     * NÃO faz commit/rollback). Útil para operações atômicas como a baixa de
     * vários insumos ao finalizar um Diário de Campo.
     */
    public void registrarSaida(Connection c, int idInsumo, BigDecimal qtd, LocalDate data, String obs)
            throws SQLException, EstoqueInsuficienteException {
        BigDecimal[] atual = lerSaldoPreco(c, idInsumo);
        BigDecimal saldoAtual = atual[0];
        if (saldoAtual.compareTo(qtd) < 0) {
            throw new EstoqueInsuficienteException(
                "Estoque insuficiente: disponível " + saldoAtual.stripTrailingZeros().toPlainString()
                + ", solicitado " + qtd.stripTrailingZeros().toPlainString() + ".");
        }
        BigDecimal novoSaldo = saldoAtual.subtract(qtd);
        atualizarSaldoPreco(c, idInsumo, novoSaldo, atual[1]);
        inserirMovimento(c, idInsumo, "SAIDA", qtd, null, null, data, obs, novoSaldo);
    }

    /** Histórico de movimentações de um insumo (mais recentes primeiro). */
    public List<Map<String, Object>> listarPorInsumo(int idInsumo) throws SQLException {
        String sql = "SELECT m.*, p.nomepessoa AS parceiro_nome "
                   + "FROM estoque_movimento m LEFT JOIN parceiro p ON p.idpessoa = m.id_parceiro "
                   + "WHERE m.idinsumo=? ORDER BY m.data_mov DESC, m.idmov DESC";
        List<Map<String, Object>> lista = new ArrayList<>();
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement st = c.prepareStatement(sql)) {
            st.setInt(1, idInsumo);
            ResultSet rs = st.executeQuery();
            while (rs.next()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("idMov", rs.getInt("idmov"));
                m.put("tipo", rs.getString("tipo"));
                m.put("quantidade", rs.getBigDecimal("quantidade"));
                m.put("precoUnitario", rs.getBigDecimal("preco_unitario"));
                m.put("parceiroNome", rs.getString("parceiro_nome"));
                Date d = rs.getDate("data_mov");
                m.put("dataMov", d != null ? d.toLocalDate().toString() : null);
                m.put("observacao", rs.getString("observacao"));
                m.put("saldoApos", rs.getBigDecimal("saldo_apos"));
                lista.add(m);
            }
        }
        return lista;
    }

    /* --------------------------- helpers --------------------------- */

    private BigDecimal[] lerSaldoPreco(Connection c, int idInsumo) throws SQLException {
        // FOR UPDATE trava a linha do insumo durante a transação
        try (PreparedStatement st = c.prepareStatement(
                "SELECT quantidade_disponivel, preco_medio FROM insumo WHERE idinsumo=? FOR UPDATE")) {
            st.setInt(1, idInsumo);
            ResultSet rs = st.executeQuery();
            if (!rs.next()) throw new SQLException("Insumo não encontrado: " + idInsumo);
            return new BigDecimal[]{ rs.getBigDecimal(1), rs.getBigDecimal(2) };
        }
    }

    private void atualizarSaldoPreco(Connection c, int idInsumo, BigDecimal saldo, BigDecimal medio) throws SQLException {
        try (PreparedStatement st = c.prepareStatement(
                "UPDATE insumo SET quantidade_disponivel=?, preco_medio=? WHERE idinsumo=?")) {
            st.setBigDecimal(1, saldo);
            st.setBigDecimal(2, medio);
            st.setInt(3, idInsumo);
            st.executeUpdate();
        }
    }

    private void inserirMovimento(Connection c, int idInsumo, String tipo, BigDecimal qtd,
                                  BigDecimal precoUnit, Integer idParceiro, LocalDate data,
                                  String obs, BigDecimal saldoApos) throws SQLException {
        String sql = "INSERT INTO estoque_movimento (idinsumo, tipo, quantidade, preco_unitario, "
                   + "id_parceiro, data_mov, observacao, saldo_apos) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement st = c.prepareStatement(sql)) {
            st.setInt(1, idInsumo);
            st.setString(2, tipo);
            st.setBigDecimal(3, qtd);
            if (precoUnit != null) st.setBigDecimal(4, precoUnit); else st.setNull(4, Types.NUMERIC);
            if (idParceiro != null) st.setInt(5, idParceiro); else st.setNull(5, Types.INTEGER);
            st.setDate(6, Date.valueOf(data));
            st.setString(7, obs);
            st.setBigDecimal(8, saldoApos);
            st.executeUpdate();
        }
    }
}
