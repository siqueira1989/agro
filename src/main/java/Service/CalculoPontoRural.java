package Service;

import Model.Model.PontoEletronico;
import Model.Model.TipoAtividadeRural;

import java.time.Duration;
import java.time.LocalTime;

/**
 * Cálculo de um registro de ponto (1 dia) segundo as regras rurais:
 *
 *  - Total trabalhado = soma dos pares entrada/saída (trata virada de dia).
 *  - Tolerância (Art. 58 CLT): variação líquida de até 10 min/dia não gera
 *    extra nem atraso. (A nuance "5 min por batida" é absorvida pela banda
 *    diária de 10 min; documentada aqui para refinamento futuro.)
 *  - Minutos noturnos REAIS na janela da atividade (60 min, sem redução).
 *
 * O split de "domingo 100%" NÃO é decidido aqui: depende da escala da semana
 * (folga compensatória) e é resolvido no fechamento mensal.
 */
public final class CalculoPontoRural {

    /** Banda de tolerância diária do Art. 58 §1º (10 minutos). */
    public static final int TOLERANCIA_DIARIA_MIN = 10;

    /**
     * Teto de jornada plausível para um dia: 16 horas (P2-26 da auditoria de
     * 29/08/2026).
     *
     * <p>O tratamento de virada de meia-noite somava 24 h sempre que a saída era
     * menor que a entrada. Um erro de digitação — entrada 17:00, saída 08:00 —
     * virava <b>15 horas trabalhadas</b> em silêncio, gerando 7 horas extras
     * indevidas na folha. Acima deste teto o registro é rejeitado, porque é
     * muito mais provável ser engano de digitação do que jornada real.</p>
     */
    public static final int JORNADA_MAXIMA_MIN = 16 * 60;

    private CalculoPontoRural() { }

    /**
     * Recalcula totalMinutos, extraMinutos e minutosNoturnos do ponto.
     *
     * @param p           registro de ponto (com horários preenchidos)
     * @param atividade   tipo de atividade rural (define a janela noturna)
     * @param jornadaMin  jornada diária em minutos (tipicamente 480)
     */
    public static void calcular(PontoEletronico p, TipoAtividadeRural atividade, int jornadaMin) {
        if (atividade == null) atividade = TipoAtividadeRural.LAVOURA;

        validarSequencia(p);

        int total = 0;
        int noturno = 0;

        total   += minutosIntervalo(p.getEntrada1(), p.getSaida1());
        noturno += atividade.minutosNoturnos(p.getEntrada1(), p.getSaida1());

        total   += minutosIntervalo(p.getEntrada2(), p.getSaida2());
        noturno += atividade.minutosNoturnos(p.getEntrada2(), p.getSaida2());

        total = Math.max(0, total);

        // P2-26: barra jornada implausivel em vez de aceitar em silencio.
        if (total > JORNADA_MAXIMA_MIN) {
            throw new IllegalArgumentException(
                "Jornada de " + (total / 60) + "h" + String.format("%02d", total % 60)
              + " no dia " + p.getDataRegistro() + " excede o limite de 16 horas. "
              + "Verifique os horários — provavelmente a entrada e a saída estão trocadas.");
        }

        // Extra bruto = trabalho além da jornada. Tolerância: se a diferença
        // (para mais ou para menos) couber na banda de 10 min, desconsidera.
        int extra = total - jornadaMin;
        if (Math.abs(extra) <= TOLERANCIA_DIARIA_MIN) {
            extra = 0;
        } else if (extra < 0) {
            extra = 0;  // atraso não vira extra; o desconto fica a cargo da falta/fechamento
        }

        p.setTotalMinutos(total);
        p.setExtraMinutos(extra);
        p.setMinutosNoturnos(noturno);
    }

    /**
     * Confere a ordem das batidas antes de calcular (P2-26).
     *
     * <p>Sem esta checagem, um segundo período que comece antes do fim do
     * primeiro passava despercebido e o total do dia saía inflado.</p>
     */
    private static void validarSequencia(PontoEletronico p) {
        if (p.getSaida1() != null && p.getEntrada2() != null
                && p.getEntrada2().isBefore(p.getSaida1())) {
            throw new IllegalArgumentException(
                "O segundo período começa antes do fim do primeiro (saída "
              + p.getSaida1() + ", entrada " + p.getEntrada2() + "). Corrija os horários.");
        }
        if (p.getEntrada1() == null && p.getSaida1() != null) {
            throw new IllegalArgumentException("Há saída sem entrada correspondente no primeiro período.");
        }
        if (p.getEntrada2() == null && p.getSaida2() != null) {
            throw new IllegalArgumentException("Há saída sem entrada correspondente no segundo período.");
        }
    }

    /** Minutos de um intervalo, tratando saída &lt; entrada como virada de dia. */
    private static int minutosIntervalo(LocalTime entrada, LocalTime saida) {
        if (entrada == null || saida == null) return 0;
        long min = Duration.between(entrada, saida).toMinutes();
        if (min < 0) min += 24 * 60;   // cruzou a meia-noite
        return (int) min;
    }
}
