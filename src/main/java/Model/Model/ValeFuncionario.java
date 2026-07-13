package Model.Model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ValeFuncionario {

    public enum StatusVale { PENDENTE, DESCONTADO }
    public enum TipoVale   { ALIMENTACAO, TRANSPORTE, ADIANTAMENTO, OUTROS }

    private int        idVale;
    private int        idFuncionario;
    private String     nomeFuncionario;
    private LocalDate  dataVale;
    private BigDecimal valor;
    private String     descricao;
    private TipoVale   tipoVale   = TipoVale.OUTROS;
    private StatusVale statusVale = StatusVale.PENDENTE;

    public int getIdVale()                       { return idVale; }
    public void setIdVale(int v)                 { this.idVale = v; }

    public int getIdFuncionario()                { return idFuncionario; }
    public void setIdFuncionario(int v)          { this.idFuncionario = v; }

    public String getNomeFuncionario()           { return nomeFuncionario; }
    public void setNomeFuncionario(String v)     { this.nomeFuncionario = v; }

    public LocalDate getDataVale()               { return dataVale; }
    public void setDataVale(LocalDate v)         { this.dataVale = v; }

    public BigDecimal getValor()                 { return valor; }
    public void setValor(BigDecimal v)           { this.valor = v; }

    public String getDescricao()                 { return descricao; }
    public void setDescricao(String v)           { this.descricao = v; }

    public TipoVale getTipoVale()                { return tipoVale; }
    public void setTipoVale(TipoVale v)          { this.tipoVale = v != null ? v : TipoVale.OUTROS; }
    public String getTipoValeStr()               { return tipoVale != null ? tipoVale.name() : "OUTROS"; }

    public StatusVale getStatusVale()            { return statusVale; }
    public void setStatusVale(StatusVale v)      { this.statusVale = v != null ? v : StatusVale.PENDENTE; }
    public String getStatusValeStr()             { return statusVale != null ? statusVale.name() : "PENDENTE"; }
}
