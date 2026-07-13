package Model.Model;

import java.time.LocalTime;

/**
 * Tipo de atividade rural (Lei 5.889/73). Define a janela do horário noturno,
 * que difere da urbana e varia conforme a atividade:
 *   - LAVOURA:  21:00 às 05:00
 *   - PECUÁRIA: 20:00 às 04:00
 *
 * No meio rural a hora noturna tem 60 minutos REAIS (não se aplica a redução
 * urbana de 52min30s). Estes minutos servem de base para o adicional de 25%.
 */
public enum TipoAtividadeRural {

    LAVOURA(LocalTime.of(21, 0), LocalTime.of(5, 0)),
    PECUARIA(LocalTime.of(20, 0), LocalTime.of(4, 0));

    private final LocalTime inicioNoturno;
    private final LocalTime fimNoturno;

    TipoAtividadeRural(LocalTime inicioNoturno, LocalTime fimNoturno) {
        this.inicioNoturno = inicioNoturno;
        this.fimNoturno = fimNoturno;
    }

    public LocalTime getInicioNoturno() { return inicioNoturno; }
    public LocalTime getFimNoturno()    { return fimNoturno; }

    /**
     * Minutos noturnos REAIS de um intervalo trabalhado [entrada, saida].
     * A janela noturna cruza a meia-noite (ex.: 21:00→05:00); por isso o
     * cálculo é feito em minutos-desde-meia-noite, contando a interseção
     * tanto da parte noite (21:00→24:00) quanto da madrugada (00:00→05:00).
     *
     * @param entrada início do intervalo trabalhado
     * @param saida   fim do intervalo; se for menor que a entrada, considera-se
     *                virada de dia (a soma de minutos já trata isso via 1440).
     */
    public int minutosNoturnos(LocalTime entrada, LocalTime saida) {
        if (entrada == null || saida == null) return 0;

        int ini = entrada.getHour() * 60 + entrada.getMinute();
        int fim = saida.getHour() * 60 + saida.getMinute();
        if (fim <= ini) fim += 24 * 60;            // virada de dia

        int janIni = inicioNoturno.getHour() * 60 + inicioNoturno.getMinute(); // ex.: 1260
        int janFim = fimNoturno.getHour() * 60 + fimNoturno.getMinute();       // ex.: 300

        // A janela noturna [janIni, janFim+1440) cobre da noite à madrugada do
        // dia seguinte. Como o intervalo trabalhado pode começar em qualquer
        // ponto, projetamos a janela em dois "dias" e somamos as interseções.
        int total = 0;
        for (int desloc = -24 * 60; desloc <= 24 * 60; desloc += 24 * 60) {
            int wIni = janIni + desloc;
            int wFim = janFim + desloc + 24 * 60;  // janela sempre cruza meia-noite
            total += intersecao(ini, fim, wIni, wFim);
        }
        return total;
    }

    private int intersecao(int aIni, int aFim, int bIni, int bFim) {
        int ini = Math.max(aIni, bIni);
        int fim = Math.min(aFim, bFim);
        return Math.max(0, fim - ini);
    }
}
