package Model.Model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class FolhaPagamento {

    private int idFolha;
    private int idFuncionario;
    private String nomeFuncionario;
    private String periodo;
    private TipoFuncionario tipoFuncionario;
    private BigDecimal valorCalculado = BigDecimal.ZERO;
    private LocalDateTime dataGeracao;

    public FolhaPagamento() {}

    public int getIdFolha() { return idFolha; }
    public void setIdFolha(int idFolha) { this.idFolha = idFolha; }

    public int getIdFuncionario() { return idFuncionario; }
    public void setIdFuncionario(int idFuncionario) { this.idFuncionario = idFuncionario; }

    public String getNomeFuncionario() { return nomeFuncionario; }
    public void setNomeFuncionario(String nomeFuncionario) { this.nomeFuncionario = nomeFuncionario; }

    public String getPeriodo() { return periodo; }
    public void setPeriodo(String periodo) { this.periodo = periodo; }

    public TipoFuncionario getTipoFuncionario() { return tipoFuncionario; }
    public void setTipoFuncionario(TipoFuncionario tipoFuncionario) { this.tipoFuncionario = tipoFuncionario; }

    public BigDecimal getValorCalculado() { return valorCalculado; }
    public void setValorCalculado(BigDecimal valorCalculado) { this.valorCalculado = valorCalculado != null ? valorCalculado : BigDecimal.ZERO; }

    public LocalDateTime getDataGeracao() { return dataGeracao; }
    public void setDataGeracao(LocalDateTime dataGeracao) { this.dataGeracao = dataGeracao; }
}
