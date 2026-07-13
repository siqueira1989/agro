package Model.Model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Movimentação de estoque (histórico/auditoria). Cada linha grava o saldo
 * resultante (saldoApos) para rastreabilidade.
 */
public class EstoqueMovimento {

    private int idMov;
    private int idInsumo;
    private String tipo;                  // ENTRADA | SAIDA
    private BigDecimal quantidade;
    private BigDecimal precoUnitario;     // só nas entradas
    private Integer idParceiro;           // fornecedor tipo INSUMO (entrada)
    private String parceiroNome;          // exibição
    private LocalDate dataMov;
    private String observacao;
    private BigDecimal saldoApos;

    public int getIdMov() { return idMov; }
    public void setIdMov(int idMov) { this.idMov = idMov; }

    public int getIdInsumo() { return idInsumo; }
    public void setIdInsumo(int idInsumo) { this.idInsumo = idInsumo; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public BigDecimal getQuantidade() { return quantidade; }
    public void setQuantidade(BigDecimal quantidade) { this.quantidade = quantidade; }

    public BigDecimal getPrecoUnitario() { return precoUnitario; }
    public void setPrecoUnitario(BigDecimal precoUnitario) { this.precoUnitario = precoUnitario; }

    public Integer getIdParceiro() { return idParceiro; }
    public void setIdParceiro(Integer idParceiro) { this.idParceiro = idParceiro; }

    public String getParceiroNome() { return parceiroNome; }
    public void setParceiroNome(String parceiroNome) { this.parceiroNome = parceiroNome; }

    public LocalDate getDataMov() { return dataMov; }
    public void setDataMov(LocalDate dataMov) { this.dataMov = dataMov; }

    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }

    public BigDecimal getSaldoApos() { return saldoApos; }
    public void setSaldoApos(BigDecimal saldoApos) { this.saldoApos = saldoApos; }
}
