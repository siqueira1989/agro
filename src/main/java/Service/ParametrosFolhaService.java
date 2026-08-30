package Service;

import Util.ConfigUtil;
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
 * Origem única dos parâmetros de tempo e dos divisores da folha
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
 *       dos ≈3,3% devidos. Já a hora normal usa o divisor <b>220</b> da jornada
 *       de 44 h semanais, e não 240.</li>
 *   <li><b>Custeio da atividade (quanto a hora trabalhada custa à fazenda).</b>
 *       Aí o salário se dilui nas horas efetivamente produtivas do mês:
 *       {@code salário / (jornada × diasÚteis do mês)}. Dividir por 30 aqui
 *       subestimaria o custo, porque incluiria os dias de descanso.</li>
 * </ul>
 */
public final class ParametrosFolhaService {

    /** Divisor legal do salário mensal para obter o valor do dia (CLT, art. 64). */
    public static final BigDecimal DIVISOR_MENSAL_LEGAL = new BigDecimal("30");

    /**
     * Horas mensais por hora de jornada diária: 27,5.
     *
     * <p>Uma jornada de 8 h corresponde a 44 h semanais e ao divisor legal de
     * <b>220 horas mensais</b> (8 × 27,5 = 220). O código anterior derivava a
     * hora de {@code salário / 30 / jornada} — divisor 240 —, o que pagava toda
     * hora extra e todo adicional noturno ≈8,3% <b>a menos</b> do que o devido.</p>
     */
    private static final BigDecimal HORAS_MENSAIS_POR_HORA_DIARIA = new BigDecimal("27.5");

    /** Validade do cache de feriados: um cadastro novo vale sem reiniciar o Tomcat. */
    private static final long TTL_CACHE_MS = 60L * 60L * 1000L;

    private record Cache(Set<LocalDate> dias, long expiraEm) { }

    private static final Map<Integer, Cache> CACHE_FERIADOS = new ConcurrentHashMap<>();

    private ParametrosFolhaService() { }

    // ───────────────────────── dias úteis ─────────────────────────

    /** Dias úteis do mês: exclui sábados, domingos e feriados. */
    public static int diasUteis(YearMonth mes) {
        Set<LocalDate> feriados = feriadosDoAno(mes.getYear());
        int total = 0;
        for (int d = 1; d <= mes.lengthOfMonth(); d++) {
            LocalDate dia = mes.atDay(d);
            DayOfWeek dow = dia.getDayOfWeek();
            if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) continue;
            if (feriados.contains(dia)) continue;
            total++;
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
     * Valor da hora normal: {@code salário / (jornada × 27,5)} — divisor 220
     * para a jornada padrão de 8 h.
     *
     * <p>É a base sobre a qual incidem o adicional de hora extra e o adicional
     * noturno.</p>
     */
    public static BigDecimal valorHoraFolha(BigDecimal salarioMensal, int jornadaDiaria) {
        if (salarioMensal == null || salarioMensal.signum() <= 0) return BigDecimal.ZERO;
        int jornada = jornadaDiaria > 0 ? jornadaDiaria : 8;
        BigDecimal horasMensais = HORAS_MENSAIS_POR_HORA_DIARIA.multiply(BigDecimal.valueOf(jornada));
        return salarioMensal.divide(horasMensais, 4, RoundingMode.HALF_UP);
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

    /** Dias úteis do mês da data informada — usado pelas consultas do Diário. */
    public static int diasUteisDoMesDe(LocalDate data) {
        return diasUteis(YearMonth.from(data != null ? data : LocalDate.now()));
    }

    // ───────────────────────── feriados ─────────────────────────

    /**
     * Feriados do ano: os de data fixa vêm da tabela {@code feriado}; os móveis
     * (Carnaval, Sexta-feira Santa e Corpus Christi) são derivados da Páscoa e
     * não precisam ser cadastrados a cada ano.
     *
     * <p>A abrangência é respeitada: feriados estaduais só valem para a UF
     * configurada em {@code folha.uf} e os municipais para o município em
     * {@code folha.municipio}. Sem esse filtro, um feriado municipal de outra
     * cidade reduziria o divisor de todo mundo.</p>
     *
     * <p><b>Falha de leitura não é cacheada.</b> Se o banco estiver
     * indisponível no primeiro cálculo do ano, o conjunto incompleto seria
     * gravado no cache e, pelo resto da vida da JVM, 7 de setembro e Natal
     * deixariam de existir para o cálculo — sem nenhum erro visível.</p>
     */
    private static Set<LocalDate> feriadosDoAno(int ano) {
        long agora = System.currentTimeMillis();
        Cache atual = CACHE_FERIADOS.get(ano);
        if (atual != null && atual.expiraEm() > agora) return atual.dias();

        Set<LocalDate> dias = new HashSet<>(feriadosMoveis(ano));
        try {
            dias.addAll(feriadosCadastrados(ano));
            CACHE_FERIADOS.put(ano, new Cache(Set.copyOf(dias), agora + TTL_CACHE_MS));
        } catch (SQLException e) {
            // Não cacheia: a próxima chamada tenta de novo em vez de congelar
            // um calendário incompleto.
            LogUtil.aviso(ParametrosFolhaService.class,
                    "Não foi possível ler a tabela de feriados do ano " + ano
                  + "; considerando apenas os feriados móveis nesta chamada.", e);
        }
        return dias;
    }

    private static Set<LocalDate> feriadosCadastrados(int ano) throws SQLException {
        String uf        = ConfigUtil.get("folha.uf", null);
        String municipio = ConfigUtil.get("folha.municipio", null);

        // Intervalo de datas em vez de EXTRACT(YEAR FROM data): função sobre a
        // coluna impede o uso do índice idx_feriado_data.
        String sql = "SELECT data FROM feriado "
                   + "WHERE data >= ? AND data <= ? "
                   + "AND (abrangencia = 'NACIONAL' "
                   + "  OR (abrangencia = 'ESTADUAL'  AND uf = ?) "
                   + "  OR (abrangencia = 'MUNICIPAL' AND municipio = ?))";

        Set<LocalDate> dias = new HashSet<>();
        try (Connection c = new PostgresConnection().getConnection();
             PreparedStatement st = c.prepareStatement(sql)) {
            st.setDate(1, Date.valueOf(LocalDate.of(ano, 1, 1)));
            st.setDate(2, Date.valueOf(LocalDate.of(ano, 12, 31)));
            st.setString(3, uf);
            st.setString(4, municipio);
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    Date d = rs.getDate(1);
                    if (d != null) dias.add(d.toLocalDate());
                }
            }
        }
        return dias;
    }

    /** Limpa o cache — chame após cadastrar, alterar ou remover feriados. */
    public static void limparCacheFeriados() {
        CACHE_FERIADOS.clear();
        LogUtil.info(ParametrosFolhaService.class, "Cache de feriados limpo.");
    }

    /**
     * Feriados móveis derivados da Páscoa.
     *
     * <p><b>Só a Sexta-feira Santa entra por padrão.</b> Carnaval (segunda e
     * terça) e Corpus Christi <b>não são feriados nacionais</b> — são ponto
     * facultativo, e cada empresa decide se para. Contá-los como feriado
     * reduziria os dias úteis de todo mundo: em fevereiro de 2026, de 20 para
     * 18, o que infla o custo da hora produtiva em ~11% para uma fazenda que
     * trabalha no Carnaval.</p>
     *
     * <p>Quem não trabalha nessas datas liga em {@code agro.properties}:</p>
     * <pre>
     * folha.feriado.carnaval=true
     * folha.feriado.corpusChristi=true
     * </pre>
     * <p>Feriados estaduais e municipais continuam vindo da tabela
     * {@code feriado}, com a abrangência respeitada.</p>
     */
    private static Set<LocalDate> feriadosMoveis(int ano) {
        LocalDate pascoa = domingoDePascoa(ano);
        Set<LocalDate> dias = new HashSet<>();

        dias.add(pascoa.minusDays(2));   // Sexta-feira Santa — feriado nacional

        if (ConfigUtil.getBoolean("folha.feriado.carnaval", false)) {
            dias.add(pascoa.minusDays(48));  // segunda de Carnaval
            dias.add(pascoa.minusDays(47));  // terça de Carnaval
        }
        if (ConfigUtil.getBoolean("folha.feriado.corpusChristi", false)) {
            dias.add(pascoa.plusDays(60));
        }
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
