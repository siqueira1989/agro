package Service;

import Util.LogUtil;
import Util.PostgresConnection;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Encargos da folha: INSS, IRRF e FGTS (P1-9 da auditoria de 29/08/2026).
 *
 * <h3>O que estava errado</h3>
 * <p>O cálculo do líquido era, literalmente, {@code salarioBruto - totalVales}.
 * Não havia INSS, IRRF, FGTS nem DSR. O número apresentado como "líquido" não
 * correspondia ao que o trabalhador recebe nem ao que a empresa recolhe.</p>
 *
 * <h3>Como está agora</h3>
 * <p>As alíquotas <b>não estão no código</b>: vivem na tabela
 * {@code faixa_encargo}, com data de vigência. Quando a lei muda, a
 * contabilidade insere uma nova vigência e as competências antigas continuam
 * sendo recalculadas com a tabela que valia na época — sem alterar Java e sem
 * reimplantar o sistema.</p>
 *
 * <p><b>Aviso que precisa ser lido.</b> As faixas semeadas pela migração V22
 * refletem a tabela de maio/2025 e existem para que a estrutura nasça
 * funcionando. Elas mudam por lei, normalmente todo ano.
 * <b>Confira com a contabilidade antes de usar a folha em produção.</b>
 * {@link #tabelaDesatualizada(LocalDate)} avisa quando a vigência mais recente
 * é anterior à competência que está sendo calculada.</p>
 */
public final class EncargosService {

    private EncargosService() { }

    /** Uma faixa da tabela progressiva. */
    public record Faixa(BigDecimal limiteAte, BigDecimal aliquota, BigDecimal parcelaDeduzir) { }

    /** Resultado do cálculo de encargos de uma competência. */
    public record Encargos(BigDecimal inss, BigDecimal irrf, BigDecimal fgts,
                           BigDecimal baseIrrf, boolean tabelaConfiavel) {

        /** Total descontado do trabalhador (o FGTS não entra: é do empregador). */
        public BigDecimal totalDescontos() {
            return inss.add(irrf);
        }
    }

    /**
     * Apura INSS, IRRF e FGTS sobre a remuneração bruta da competência.
     *
     * @param bruto        remuneração bruta do mês
     * @param dependentes  número de dependentes declarados para o IRRF
     * @param competencia  qualquer dia do mês de referência (define a vigência)
     */
    public static Encargos calcular(BigDecimal bruto, int dependentes, LocalDate competencia)
            throws SQLException {

        BigDecimal base = bruto == null ? BigDecimal.ZERO : bruto.max(BigDecimal.ZERO);

        BigDecimal inss = calcularInss(base, competencia);
        BigDecimal baseIrrf = base.subtract(inss)
                .subtract(deducaoDependentes(dependentes, competencia))
                .max(BigDecimal.ZERO);
        BigDecimal irrf = calcularIrrf(baseIrrf, competencia);
        BigDecimal fgts = base.multiply(parametro("FGTS_ALIQUOTA", competencia, new BigDecimal("0.08")))
                              .setScale(2, RoundingMode.HALF_UP);

        return new Encargos(inss, irrf, fgts, baseIrrf, !tabelaDesatualizada(competencia));
    }

    /**
     * INSS progressivo: cada faixa incide apenas sobre a parcela do salário que
     * cai dentro dela — não é uma alíquota única sobre o total.
     */
    public static BigDecimal calcularInss(BigDecimal base, LocalDate competencia) throws SQLException {
        List<Faixa> faixas = faixas("INSS", competencia);
        if (faixas.isEmpty()) return BigDecimal.ZERO;

        BigDecimal teto = parametro("INSS_TETO", competencia, null);
        BigDecimal salario = (teto != null) ? base.min(teto) : base;

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal pisoFaixa = BigDecimal.ZERO;

        for (Faixa f : faixas) {
            BigDecimal tetoFaixa = f.limiteAte() != null ? f.limiteAte() : salario;
            if (salario.compareTo(pisoFaixa) <= 0) break;
            BigDecimal parcela = salario.min(tetoFaixa).subtract(pisoFaixa);
            if (parcela.signum() > 0) {
                total = total.add(parcela.multiply(f.aliquota()));
            }
            pisoFaixa = tetoFaixa;
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * IRRF pela tabela mensal: alíquota da faixa menos a parcela a deduzir.
     *
     * <p>Compara com o desconto simplificado e aplica o que for mais vantajoso
     * para o trabalhador, como manda a regra vigente.</p>
     */
    public static BigDecimal calcularIrrf(BigDecimal baseCalculo, LocalDate competencia) throws SQLException {
        List<Faixa> faixas = faixas("IRRF", competencia);
        if (faixas.isEmpty() || baseCalculo.signum() <= 0) return BigDecimal.ZERO;

        BigDecimal porFaixa = aplicarTabela(baseCalculo, faixas);

        BigDecimal simplificado = parametro("IRRF_DESCONTO_SIMPL", competencia, null);
        if (simplificado != null) {
            BigDecimal baseSimpl = baseCalculo.subtract(simplificado).max(BigDecimal.ZERO);
            porFaixa = porFaixa.min(aplicarTabela(baseSimpl, faixas));
        }
        return porFaixa.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal aplicarTabela(BigDecimal base, List<Faixa> faixas) {
        for (Faixa f : faixas) {
            if (f.limiteAte() == null || base.compareTo(f.limiteAte()) <= 0) {
                return base.multiply(f.aliquota()).subtract(f.parcelaDeduzir()).max(BigDecimal.ZERO);
            }
        }
        return BigDecimal.ZERO;
    }

    private static BigDecimal deducaoDependentes(int dependentes, LocalDate competencia) throws SQLException {
        if (dependentes <= 0) return BigDecimal.ZERO;
        BigDecimal porDependente = parametro("IRRF_DEDUCAO_DEPEND", competencia, BigDecimal.ZERO);
        return porDependente.multiply(BigDecimal.valueOf(dependentes));
    }

    /** A vigência mais recente é anterior à competência calculada? */
    public static boolean tabelaDesatualizada(LocalDate competencia) throws SQLException {
        String sql = "SELECT max(vigencia_inicio) FROM faixa_encargo";
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement st = c.prepareStatement(sql);
             ResultSet rs = st.executeQuery()) {
            if (!rs.next()) return true;
            Date d = rs.getDate(1);
            if (d == null) return true;
            LocalDate maisRecente = d.toLocalDate();
            boolean velha = maisRecente.plusYears(1).isBefore(competencia);
            if (velha) {
                LogUtil.aviso(EncargosService.class,
                        "Tabela de encargos com vigência de " + maisRecente
                      + " sendo usada para a competência " + competencia
                      + ". Confirme as faixas com a contabilidade.", null);
            }
            return velha;
        }
    }

    private static List<Faixa> faixas(String tributo, LocalDate competencia) throws SQLException {
        String sql = "SELECT limite_ate, aliquota, parcela_deduzir FROM faixa_encargo "
                   + "WHERE tributo = ? AND vigencia_inicio = ("
                   + "    SELECT max(vigencia_inicio) FROM faixa_encargo "
                   + "     WHERE tributo = ? AND vigencia_inicio <= ?) "
                   + "ORDER BY ordem";

        List<Faixa> lista = new ArrayList<>();
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement st = c.prepareStatement(sql)) {
            st.setString(1, tributo);
            st.setString(2, tributo);
            st.setDate(3, Date.valueOf(competencia));
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Faixa(rs.getBigDecimal(1), rs.getBigDecimal(2), rs.getBigDecimal(3)));
                }
            }
        }
        if (lista.isEmpty()) {
            LogUtil.aviso(EncargosService.class,
                    "Nenhuma faixa de " + tributo + " vigente em " + competencia
                  + ". O encargo será zero — aplique a migração V22 e confira a tabela.", null);
        }
        return lista;
    }

    private static BigDecimal parametro(String chave, LocalDate competencia, BigDecimal padrao)
            throws SQLException {
        String sql = "SELECT valor FROM parametro_encargo WHERE chave = ? AND vigencia_inicio <= ? "
                   + "ORDER BY vigencia_inicio DESC LIMIT 1";
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement st = c.prepareStatement(sql)) {
            st.setString(1, chave);
            st.setDate(2, Date.valueOf(competencia));
            try (ResultSet rs = st.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : padrao;
            }
        }
    }
}
