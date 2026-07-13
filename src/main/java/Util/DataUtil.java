package Util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;

/** Utilidades de data para pagamentos. */
public final class DataUtil {

    private DataUtil() { }

    /**
     * Data-limite de pagamento do CLT: até o 5º dia útil do mês SEGUINTE ao
     * período calculado (ex.: janeiro → pago até o 5º dia útil de fevereiro).
     * Considera apenas sábados/domingos como não úteis (sem feriados).
     *
     * @param periodo período calculado no formato YYYY-MM
     */
    public static LocalDate quintoDiaUtilProximoMes(String periodo) {
        YearMonth ym = YearMonth.parse(periodo).plusMonths(1);
        LocalDate d = ym.atDay(1);
        int uteis = 0;
        while (true) {
            DayOfWeek dow = d.getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                uteis++;
                if (uteis == 5) return d;
            }
            d = d.plusDays(1);
        }
    }
}
