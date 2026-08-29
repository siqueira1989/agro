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
    // ── multi-modo (CLT que trabalhou em produção/empreita) ──
    private BigDecimal valorProducao        = BigDecimal.ZERO;
    private BigDecimal valorEmpreita        = BigDecimal.ZERO;

    // Encargos legais — P1-9 da auditoria de 29/08/2026. Antes o liquido era
    // simplesmente "bruto - vales": nao havia INSS, IRRF nem FGTS.
    private BigDecimal descontoInss         = BigDecimal.ZERO;
    private BigDecimal descontoIrrf         = BigDecimal.ZERO;
    /** FGTS e encargo do EMPREGADOR: nao desconta do liquido, mas compoe o custo. */
    private BigDecimal fgtsDeposito         = BigDecimal.ZERO;
    private int        dependentes;
    /** false quando a tabela de INSS/IRRF usada esta vencida (ver EncargosService). */
    private boolean    encargosConfiaveis   = true;

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

    public BigDecimal getValorProducao()                 { return valorProducao; }
    public void setValorProducao(BigDecimal v)           { this.valorProducao = v; }

    public BigDecimal getValorEmpreita()                 { return valorEmpreita; }
    public void setValorEmpreita(BigDecimal v)           { this.valorEmpreita = v; }

    public BigDecimal getDescontoInss()                  { return descontoInss; }
    public void setDescontoInss(BigDecimal v)            { this.descontoInss = v; }

    public BigDecimal getDescontoIrrf()                  { return descontoIrrf; }
    public void setDescontoIrrf(BigDecimal v)            { this.descontoIrrf = v; }

    public BigDecimal getFgtsDeposito()                  { return fgtsDeposito; }
    public void setFgtsDeposito(BigDecimal v)            { this.fgtsDeposito = v; }

    public int getDependentes()                          { return dependentes; }
    public void setDependentes(int v)                    { this.dependentes = v; }

    public boolean isEncargosConfiaveis()                { return encargosConfiaveis; }
    public void setEncargosConfiaveis(boolean v)         { this.encargosConfiaveis = v; }

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
