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
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Origem única dos parâmetros de tempo e de divisores da folha
 * (P1-10 e P1-11 da auditoria de 29/08/2026).
 *
 * <h3>O que estava errado</h3>
 *
 * <p><b>Feriados ignorados.</b> {@code FolhaCalculoService} e
 * {@code FolhaCalculoRuralService} contavam dias úteis como "não é sábado nem
 * domingo" — o comentário do código admitia <i>"sem feriados"</i>. Em setembro
 * de 2026, com o feriado do dia 7, o cálculo usava 22 dias úteis em vez de 21, e
 * o erro contaminava valor-dia, valor-hora, hora extra, DSR e desconto de
 * falta.</p>
 *
 * <p><b>Três divisores diferentes para a mesma coisa.</b>
 * {@code DiarioCampoDAO} usava a constante fixa 22 em Java, o SQL da mesma
 * classe repetia {@code * 22.0}, e a folha usava os dias úteis reais (20 a 23).
 * A hora do mesmo funcionário CLT custava um valor no Diário de Campo e outro
 * na folha — em fevereiro a diferença chegava a 10%.</p>
 *
 * <h3>Dois divisores, de propósito</h3>
 *
 * <p>Ao unificar ficou claro que existem <b>duas perguntas diferentes</b>, e
 * confundi-las era parte do problema:</p>
 *
 * <ul>
 *   <li><b>Folha (o que a lei manda pagar e descontar).</b> Para salário mensal,
 *       o valor do dia é {@code salário / 30} — CLT, art. 64. Descontar uma
 *       falta por {@code salário / diasÚteis} tirava ≈4,5% do salário no lugar
 *       dos ≈3,3% devidos: desconto a maior em toda falta injustificada.</li>
 *   <li><b>Custeio da atividade (quanto a hora trabalhada custa à fazenda).</b>
 *       Aí o salário se dilui nas horas efetivamente produtivas do mês:
 *       {@code salário / (jornada × diasÚteis do mês)}. Dividir por 30 aqui
 *       subestimaria o custo, porque incluiria os dias de descanso.</li>
 * </ul>
 *
 * <p>Os dois métodos existem, têm nomes que dizem para que servem, e os feriados
 * entram no cálculo dos dias úteis em ambos os casos.</p>
 */
public final class ParametrosFolhaService {

    /** Divisor legal do salário mensal para obter o valor do dia (CLT, art. 64). */
    public static final BigDecimal DIVISOR_MENSAL_LEGAL = new BigDecimal("30");

    /** Cache de feriados por ano — evita ida ao banco a cada dia avaliado. */
    private static final Map<Integer, Set<LocalDate>> CACHE_FERIADOS = new ConcurrentHashMap<>();

    private ParametrosFolhaService() { }

    // ───────────────────────── dias úteis ─────────────────────────

    /** Dias úteis do mês: exclui sábados, domingos e feriados. */
    public static int diasUteis(YearMonth mes) {
        Set<LocalDate> feriados = feriadosDoAno(mes.getYear());
        int total = 0;
        for (int d = 1; d <= mes.lengthOfMonth(); d++) {
            LocalDate dia = mes.atDay(d);
            if (ehDiaUtil(dia, feriados)) total++;
        }
        return total;
    }

    /** Aceita o período no formato "YYYY-MM" usado pelos serviços de folha. */
    public static int diasUteis(String periodo) {
        return diasUteis(YearMonth.parse(periodo));
    }

    public static boolean ehFeriado(LocalDate dia) {
        return feriadosDoAno(dia.getYear()).contains(dia);
    }

    private static boolean ehDiaUtil(LocalDate dia, Set<LocalDate> feriados) {
        DayOfWeek dow = dia.getDayOfWeek();
        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) return false;
        return !feriados.contains(dia);
    }

    // ───────────────────────── folha (regra legal) ─────────────────────────

    /**
     * Valor de um dia de salário mensal: {@code salário / 30}.
     *
     * <p>Base do desconto de falta injustificada e do DSR perdido.</p>
     */
    public static BigDecimal valorDiaFolha(BigDecimal salarioMensal) {
        if (salarioMensal == null || salarioMensal.signum() <= 0) return BigDecimal.ZERO;
        return salarioMensal.divide(DIVISOR_MENSAL_LEGAL, 4, RoundingMode.HALF_UP);
    }

    /**
     * Valor da hora normal para fins de folha: {@code salário / (jornada × 30)}.
     *
     * <p>É a base sobre a qual incidem o adicional de hora extra e o adicional
     * noturno.</p>
     */
    public static BigDecimal valorHoraFolha(BigDecimal salarioMensal, int jornadaDiaria) {
        int jornada = jornadaDiaria > 0 ? jornadaDiaria : 8;
        return valorDiaFolha(salarioMensal)
                .divide(BigDecimal.valueOf(jornada), 4, RoundingMode.HALF_UP);
    }

    // ───────────────────── custeio de atividade ─────────────────────

    /**
     * Custo da hora produtiva: {@code salário / (jornada × dias úteis do mês)}.
     *
     * <p>Usado pelo Diário de Campo para ratear o salário do CLT nas atividades.
     * Substitui a constante fixa 22 que estava duplicada em Java e em SQL — o
     * número agora acompanha o mês real, feriados incluídos.</p>
     */
    public static BigDecimal custoHoraProdutiva(BigDecimal salarioMensal, int jornadaDiaria, YearMonth mes) {
        if (salarioMensal == null || salarioMensal.signum() <= 0) return BigDecimal.ZERO;
        int jornada = jornadaDiaria > 0 ? jornadaDiaria : 8;
        int uteis = Math.max(diasUteis(mes), 1);
        return salarioMensal.divide(
                BigDecimal.valueOf((long) jornada * uteis), 2, RoundingMode.HALF_UP);
    }

    // ───────────────────────── feriados ─────────────────────────

    /**
     * Feriados do ano: os de data fixa vêm da tabela {@code feriado}; os móveis
     * (Carnaval, Sexta-feira Santa e Corpus Christi) são derivados da Páscoa e
     * não precisam ser cadastrados a cada ano.
     */
    private static Set<LocalDate> feriadosDoAno(int ano) {
        return CACHE_FERIADOS.computeIfAbsent(ano, a -> {
            Set<LocalDate> dias = new HashSet<>(feriadosMoveis(a));
            String sql = "SELECT data FROM feriado WHERE EXTRACT(YEAR FROM data) = ?";
            try (Connection c = new PostgresConnection().getConnection();
                 PreparedStatement st = c.prepareStatement(sql)) {
                st.setInt(1, a);
                try (ResultSet rs = st.executeQuery()) {
                    while (rs.next()) {
                        Date d = rs.getDate(1);
                        if (d != null) dias.add(d.toLocalDate());
                    }
                }
            } catch (SQLException e) {
                // Sem a tabela (migração V21 ainda não aplicada) o cálculo segue
                // apenas com os feriados móveis — melhor do que interromper a
                // folha, mas o fato precisa ficar registrado.
                LogUtil.aviso(ParametrosFolhaService.class,
                        "Não foi possível ler a tabela de feriados do ano " + a
                      + "; considerando apenas os feriados móveis.", e);
            }
            return dias;
        });
    }

    /** Limpa o cache — chame após cadastrar ou remover feriados. */
    public static void limparCacheFeriados() {
        CACHE_FERIADOS.clear();
    }

    private static Set<LocalDate> feriadosMoveis(int ano) {
        LocalDate pascoa = domingoDePascoa(ano);
        Set<LocalDate> dias = new HashSet<>();
        dias.add(pascoa.minusDays(48));  // segunda de Carnaval
        dias.add(pascoa.minusDays(47));  // terça de Carnaval
        dias.add(pascoa.minusDays(2));   // Sexta-feira Santa
        dias.add(pascoa.plusDays(60));   // Corpus Christi
        return dias;
    }

    /** Domingo de Páscoa pelo algoritmo de Meeus/Jones/Butcher (calendário gregoriano). */
    private static LocalDate domingoDePascoa(int ano) {
        int a = ano % 19;
        int b = ano / 100;
        int c = ano % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int mes = (h + l - 7 * m + 114) / 31;
        int dia = ((h + l - 7 * m + 114) % 31) + 1;
        return LocalDate.of(ano, mes, dia);
    }
}
