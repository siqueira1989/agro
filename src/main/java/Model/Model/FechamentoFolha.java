package Model.Model;

import java.math.BigDecimal;

/** VO — resultado do cálculo financeiro mensal. */
public class FechamentoFolha {

    private int        idFechamento;
    private int        idFuncionario;
    private String     nomeFuncionario;
    private String     periodo;              // YYYY-MM
    private BigDecimal salarioBase;
    private int        jornadaHorasDia;
    private int        diasUteis;
    private int        diasTrabalhados;
    private int        faltasInjustificadas;
    private int        totalMinutosExtras;
    private BigDecimal valorHorasExtras;
    private BigDecimal descontoFaltas;
    private BigDecimal totalVales;
    private BigDecimal salarioBruto;
    private BigDecimal salarioLiquido;
    // ── rural (Lei 5.889/73) ──
    private BigDecimal descontoDsr          = BigDecimal.ZERO;
    private BigDecimal valorAdicionalNoturno = BigDecimal.ZERO;
    private BigDecimal valorExtra100        = BigDecimal.ZERO;
    private int        minutosNoturnos;
    private int        minutosExtra100;

    public int getIdFechamento()                         { return idFechamento; }
    public void setIdFechamento(int v)                   { this.idFechamento = v; }

    public int getIdFuncionario()                        { return idFuncionario; }
    public void setIdFuncionario(int v)                  { this.idFuncionario = v; }

    public String getNomeFuncionario()                   { return nomeFuncionario; }
    public void setNomeFuncionario(String v)             { this.nomeFuncionario = v; }

    public String getPeriodo()                           { return periodo; }
    public void setPeriodo(String v)                     { this.periodo = v; }

    public BigDecimal getSalarioBase()                   { return salarioBase; }
    public void setSalarioBase(BigDecimal v)             { this.salarioBase = v; }

    public int getJornadaHorasDia()                      { return jornadaHorasDia; }
    public void setJornadaHorasDia(int v)               { this.jornadaHorasDia = v; }

    public int getDiasUteis()                            { return diasUteis; }
    public void setDiasUteis(int v)                      { this.diasUteis = v; }

    public int getDiasTrabalhados()                      { return diasTrabalhados; }
    public void setDiasTrabalhados(int v)                { this.diasTrabalhados = v; }

    public int getFaltasInjustificadas()                 { return faltasInjustificadas; }
    public void setFaltasInjustificadas(int v)           { this.faltasInjustificadas = v; }

    public int getTotalMinutosExtras()                   { return totalMinutosExtras; }
    public void setTotalMinutosExtras(int v)             { this.totalMinutosExtras = v; }

    public BigDecimal getValorHorasExtras()              { return valorHorasExtras; }
    public void setValorHorasExtras(BigDecimal v)        { this.valorHorasExtras = v; }

    public BigDecimal getDescontoFaltas()                { return descontoFaltas; }
    public void setDescontoFaltas(BigDecimal v)          { this.descontoFaltas = v; }

    public BigDecimal getTotalVales()                    { return totalVales; }
    public void setTotalVales(BigDecimal v)              { this.totalVales = v; }

    public BigDecimal getSalarioBruto()                  { return salarioBruto; }
    public void setSalarioBruto(BigDecimal v)            { this.salarioBruto = v; }

    public BigDecimal getSalarioLiquido()                { return salarioLiquido; }
    public void setSalarioLiquido(BigDecimal v)          { this.salarioLiquido = v; }

    public BigDecimal getDescontoDsr()                   { return descontoDsr; }
    public void setDescontoDsr(BigDecimal v)             { this.descontoDsr = v; }

    public BigDecimal getValorAdicionalNoturno()         { return valorAdicionalNoturno; }
    public void setValorAdicionalNoturno(BigDecimal v)   { this.valorAdicionalNoturno = v; }

    public BigDecimal getValorExtra100()                 { return valorExtra100; }
    public void setValorExtra100(BigDecimal v)           { this.valorExtra100 = v; }

    public int getMinutosNoturnos()                      { return minutosNoturnos; }
    public void setMinutosNoturnos(int v)                { this.minutosNoturnos = v; }

    public int getMinutosExtra100()                      { return minutosExtra100; }
    public void setMinutosExtra100(int v)                { this.minutosExtra100 = v; }

    public String getNoturnoFormatado() {
        return String.format("%02d:%02d", minutosNoturnos / 60, minutosNoturnos % 60);
    }

    public String getExtra100Formatado() {
        return String.format("%02d:%02d", minutosExtra100 / 60, minutosExtra100 % 60);
    }

    public String getTotalExtrasFormatado() {
        int h = totalMinutosExtras / 60;
        int m = totalMinutosExtras % 60;
        return String.format("%02d:%02d", h, m);
    }
}
