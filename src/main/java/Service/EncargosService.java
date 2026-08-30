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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
 * {@link Encargos#tabelaConfiavel()} vem {@code false} quando a vigência
 * encontrada não cobre bem a competência calculada.</p>
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

    // ─────────────── cache das tabelas legais ───────────────
    // As faixas mudam uma ou duas vezes por ano e têm 5 a 9 linhas. Sem cache,
    // cada funcionário custava ~7 empréstimos de conexão do pool só para reler
    // as mesmas linhas — numa folha de 80 pessoas, ~560 idas a um pool de 10.

    private static final long TTL_CACHE_MS = 60L * 60L * 1000L;

    private record CacheFaixas(List<Faixa> faixas, long expiraEm) { }
    private record CacheValor(BigDecimal valor, long expiraEm) { }

    private static final Map<String, CacheFaixas> CACHE_FAIXAS = new ConcurrentHashMap<>();
    private static final Map<String, CacheValor>  CACHE_PARAMS = new ConcurrentHashMap<>();

    /** Descarta o cache — chame depois de cadastrar uma nova vigência. */
    public static void limparCache() {
        CACHE_FAIXAS.clear();
        CACHE_PARAMS.clear();
        LogUtil.info(EncargosService.class, "Cache de tabelas de encargos limpo.");
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
        BigDecimal baseLegal = base.subtract(inss)
                .subtract(deducaoDependentes(dependentes, competencia))
                .max(BigDecimal.ZERO);

        BigDecimal irrf = calcularIrrf(base, inss, dependentes, competencia);

        BigDecimal fgts = base.multiply(parametro("FGTS_ALIQUOTA", competencia, new BigDecimal("0.08")))
                              .setScale(2, RoundingMode.HALF_UP);

        return new Encargos(inss, irrf, fgts, baseLegal, tabelaConfiavel(competencia));
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
     * IRRF pela tabela mensal, escolhendo o modelo mais vantajoso.
     *
     * <p>O desconto simplificado é uma <b>alternativa</b> às deduções legais, não
     * um acréscimo a elas. A comparação correta é entre:</p>
     * <ul>
     *   <li><b>completo:</b> {@code bruto − INSS − dependentes};</li>
     *   <li><b>simplificado:</b> {@code bruto − desconto simplificado}.</li>
     * </ul>
     *
     * <p>A versão anterior subtraía o desconto simplificado de uma base que
     * <b>já havia perdido o INSS e os dependentes</b>, e o {@code min()} sempre
     * escolhia esse ramo. Num salário de R$ 5.000 sem dependentes isso retinha
     * R$ 196,45 em vez de R$ 312,89 — <b>R$ 116 a menos por mês por
     * funcionário</b>, que vira passivo fiscal.</p>
     */
    public static BigDecimal calcularIrrf(BigDecimal bruto, BigDecimal inss,
                                          int dependentes, LocalDate competencia)
            throws SQLException {

        List<Faixa> faixas = faixas("IRRF", competencia);
        if (faixas.isEmpty() || bruto == null || bruto.signum() <= 0) return BigDecimal.ZERO;

        BigDecimal baseCompleta = bruto
                .subtract(inss == null ? BigDecimal.ZERO : inss)
                .subtract(deducaoDependentes(dependentes, competencia))
                .max(BigDecimal.ZERO);
        BigDecimal impostoCompleto = aplicarTabela(baseCompleta, faixas);

        BigDecimal simplificado = parametro("IRRF_DESCONTO_SIMPL", competencia, null);
        if (simplificado == null) {
            return impostoCompleto.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal baseSimplificada = bruto.subtract(simplificado).max(BigDecimal.ZERO);
        BigDecimal impostoSimplificado = aplicarTabela(baseSimplificada, faixas);

        return impostoCompleto.min(impostoSimplificado)
                .max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
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

    /**
     * A vigência encontrada cobre bem a competência calculada?
     *
     * <p>Devolve {@code false} em dois casos, e os dois importam:</p>
     * <ul>
     *   <li>a tabela mais recente é anterior em mais de um ano — provavelmente
     *       a lei mudou e ninguém cadastrou;</li>
     *   <li>a competência é <b>anterior</b> à vigência mais antiga cadastrada —
     *       é o caso de um recálculo retroativo. Antes, isso devolvia INSS e
     *       IRRF zerados e ainda marcava o resultado como confiável.</li>
     * </ul>
     */
    public static boolean tabelaConfiavel(LocalDate competencia) throws SQLException {
        String sql = "SELECT min(vigencia_inicio), max(vigencia_inicio) FROM faixa_encargo";
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement st = c.prepareStatement(sql);
             ResultSet rs = st.executeQuery()) {

            if (!rs.next()) return false;
            Date min = rs.getDate(1);
            Date max = rs.getDate(2);
            if (min == null || max == null) return false;

            if (competencia.isBefore(min.toLocalDate())) {
                LogUtil.aviso(EncargosService.class,
                        "Competência " + competencia + " é anterior à vigência mais antiga cadastrada ("
                      + min.toLocalDate() + "). Usando a tabela mais antiga; confira o recálculo "
                      + "retroativo com a contabilidade.", null);
                return false;
            }
            if (max.toLocalDate().plusYears(1).isBefore(competencia)) {
                LogUtil.aviso(EncargosService.class,
                        "Tabela de encargos com vigência de " + max.toLocalDate()
                      + " sendo usada para a competência " + competencia
                      + ". Confirme as faixas com a contabilidade.", null);
                return false;
            }
            return true;
        }
    }

    /** @deprecated Nome invertido; use {@link #tabelaConfiavel(LocalDate)}. */
    @Deprecated
    public static boolean tabelaDesatualizada(LocalDate competencia) throws SQLException {
        return !tabelaConfiavel(competencia);
    }

    /**
     * Faixas vigentes na competência.
     *
     * <p>Quando a competência é anterior a tudo que está cadastrado, cai para a
     * vigência mais antiga em vez de devolver lista vazia. Devolver vazia fazia
     * o recálculo retroativo produzir INSS = 0 e IRRF = 0 silenciosamente — um
     * líquido inflado apresentado como correto.</p>
     */
    private static List<Faixa> faixas(String tributo, LocalDate competencia) throws SQLException {
        String chave = tributo + "@" + competencia.withDayOfMonth(1);
        long agora = System.currentTimeMillis();

        CacheFaixas cache = CACHE_FAIXAS.get(chave);
        if (cache != null && cache.expiraEm() > agora) return cache.faixas();

        String sql = "SELECT limite_ate, aliquota, parcela_deduzir FROM faixa_encargo "
                   + "WHERE tributo = ? AND vigencia_inicio = ("
                   + "    SELECT COALESCE("
                   + "        (SELECT max(vigencia_inicio) FROM faixa_encargo "
                   + "          WHERE tributo = ? AND vigencia_inicio <= ?),"
                   + "        (SELECT min(vigencia_inicio) FROM faixa_encargo WHERE tributo = ?))) "
                   + "ORDER BY ordem";

        List<Faixa> lista = new ArrayList<>();
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement st = c.prepareStatement(sql)) {
            st.setString(1, tributo);
            st.setString(2, tributo);
            st.setDate(3, Date.valueOf(competencia));
            st.setString(4, tributo);
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Faixa(rs.getBigDecimal(1), rs.getBigDecimal(2), rs.getBigDecimal(3)));
                }
            }
        }

        if (lista.isEmpty()) {
            LogUtil.aviso(EncargosService.class,
                    "Nenhuma faixa de " + tributo + " cadastrada. O encargo será zero — "
                  + "aplique a migração V22 e confira a tabela com a contabilidade.", null);
            return lista;   // não cacheia a ausência
        }

        CACHE_FAIXAS.put(chave, new CacheFaixas(List.copyOf(lista), agora + TTL_CACHE_MS));
        return lista;
    }

    private static BigDecimal parametro(String chave, LocalDate competencia, BigDecimal padrao)
            throws SQLException {

        String chaveCache = chave + "@" + competencia.withDayOfMonth(1);
        long agora = System.currentTimeMillis();

        CacheValor cache = CACHE_PARAMS.get(chaveCache);
        if (cache != null && cache.expiraEm() > agora) return cache.valor();

        String sql = "SELECT valor FROM parametro_encargo WHERE chave = ? AND vigencia_inicio <= ? "
                   + "ORDER BY vigencia_inicio DESC LIMIT 1";
        BigDecimal valor = padrao;
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement st = c.prepareStatement(sql)) {
            st.setString(1, chave);
            st.setDate(2, Date.valueOf(competencia));
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) valor = rs.getBigDecimal(1);
            }
        }

        if (valor != null) {
            CACHE_PARAMS.put(chaveCache, new CacheValor(valor, agora + TTL_CACHE_MS));
        }
        return valor;
    }
}
