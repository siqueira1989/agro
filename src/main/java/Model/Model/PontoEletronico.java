package Model.Model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public class PontoEletronico {

    private int     idPonto;
    private int     idFuncionario;
    private String  nomeFuncionario;
    private String  emailFuncionario;
    private LocalDate dataRegistro;
    private LocalTime entrada1;
    private LocalTime saida1;
    private LocalTime entrada2;
    private LocalTime saida2;
    private int     totalMinutos;
    private int     extraMinutos;
    private int     minutosNoturnos;     // horas noturnas rurais (60min reais)
    private String  observacao;
    /**
     * Dia concedido como folga compensatoria do domingo trabalhado na mesma
     * semana (P1-13 da auditoria de 29/08/2026).
     *
     * <p>Antes a folga era DEDUZIDA da ausencia de registro: "existe um dia
     * entre segunda e sabado sem ponto e sem falta". Como quase ninguem tem
     * ponto no sabado, quase toda semana era classificada como compensada e o
     * domingo trabalhado deixava de ser pago a 100%. Agora e um fato marcado
     * explicitamente, nao um palpite.</p>
     */
    private boolean folgaCompensatoria;

    // ── auditoria (Portaria 671) — ação do gestor que registra ──
    private BigDecimal latitude;          // reservado p/ app móvel futuro
    private BigDecimal longitude;
    private String  ipOrigem;
    private String  userAgent;
    private String  hashAutenticidade;
    private Integer registradoPor;        // idpessoa do gestor/admin

    // ── getters / setters ─────────────────────────────────────

    public int getIdPonto()                        { return idPonto; }
    public void setIdPonto(int idPonto)            { this.idPonto = idPonto; }

    public int getIdFuncionario()                  { return idFuncionario; }
    public void setIdFuncionario(int v)            { this.idFuncionario = v; }

    public String getNomeFuncionario()             { return nomeFuncionario; }
    public void setNomeFuncionario(String v)       { this.nomeFuncionario = v; }

    public String getEmailFuncionario()            { return emailFuncionario; }
    public void setEmailFuncionario(String v)      { this.emailFuncionario = v; }

    public LocalDate getDataRegistro()             { return dataRegistro; }
    public void setDataRegistro(LocalDate v)       { this.dataRegistro = v; }

    public LocalTime getEntrada1()                 { return entrada1; }
    public void setEntrada1(LocalTime v)           { this.entrada1 = v; }

    public LocalTime getSaida1()                   { return saida1; }
    public void setSaida1(LocalTime v)             { this.saida1 = v; }

    public LocalTime getEntrada2()                 { return entrada2; }
    public void setEntrada2(LocalTime v)           { this.entrada2 = v; }

    public LocalTime getSaida2()                   { return saida2; }
    public void setSaida2(LocalTime v)             { this.saida2 = v; }

    public int getTotalMinutos()                   { return totalMinutos; }
    public void setTotalMinutos(int v)             { this.totalMinutos = v; }

    public int getExtraMinutos()                   { return extraMinutos; }
    public void setExtraMinutos(int v)             { this.extraMinutos = v; }

    public String getObservacao()                  { return observacao; }
    public void setObservacao(String v)            { this.observacao = v; }

    public int getMinutosNoturnos()                { return minutosNoturnos; }
    public void setMinutosNoturnos(int v)          { this.minutosNoturnos = v; }

    public BigDecimal getLatitude()                { return latitude; }
    public void setLatitude(BigDecimal v)          { this.latitude = v; }

    public BigDecimal getLongitude()               { return longitude; }
    public void setLongitude(BigDecimal v)         { this.longitude = v; }

    public String getIpOrigem()                    { return ipOrigem; }
    public void setIpOrigem(String v)              { this.ipOrigem = v; }

    public String getUserAgent()                   { return userAgent; }
    public void setUserAgent(String v)             { this.userAgent = v; }

    public String getHashAutenticidade()           { return hashAutenticidade; }
    public void setHashAutenticidade(String v)     { this.hashAutenticidade = v; }

    public Integer getRegistradoPor()              { return registradoPor; }
    public void setRegistradoPor(Integer v)        { this.registradoPor = v; }

    public String getNoturnoFormatado() {
        int h = minutosNoturnos / 60;
        int m = minutosNoturnos % 60;
        return String.format("%02d:%02d", h, m);
    }

    // ── helpers ──────────────────────────────────────────────

    /** Recalcula total_minutos e extra_minutos a partir dos horários. */
    public void calcularMinutos(int jornadaMinutos) {
        int total = 0;
        if (entrada1 != null && saida1 != null)
            total += (int) java.time.Duration.between(entrada1, saida1).toMinutes();
        if (entrada2 != null && saida2 != null)
            total += (int) java.time.Duration.between(entrada2, saida2).toMinutes();
        this.totalMinutos = Math.max(0, total);
        this.extraMinutos = Math.max(0, total - jornadaMinutos);
    }

    public String getTotalFormatado() {
        int h = totalMinutos / 60;
        int m = totalMinutos % 60;
        return String.format("%02d:%02d", h, m);
    }

    public String getExtraFormatado() {
        int h = extraMinutos / 60;
        int m = extraMinutos % 60;
        return String.format("%02d:%02d", h, m);
    }

    public boolean isFolgaCompensatoria()          { return folgaCompensatoria; }
    public void setFolgaCompensatoria(boolean v)   { this.folgaCompensatoria = v; }
}
