package Model.Model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Recebimento de um parceiro distribuído entre romaneios PENDENTES (romaneio_pagamento + _item). */
public class RomaneioPagamento {
    public static final String[] FORMAS = { "DINHEIRO", "PIX", "CHEQUE_PROPRIO", "CHEQUE_TERCEIRO" };

    private int idPagamento;
    private int idParceiro;        private String nomeParceiro;
    private LocalDate dataPagamento;
    private String forma;
    private BigDecimal valor;
    private String banco;
    private String numeroCheque;
    private String emitente;
    private LocalDate bomPara;
    private String observacao;
    /** No histórico de UM romaneio: quanto deste pagamento foi para ele. */
    private BigDecimal valorNoRomaneio;
    private List<Item> itens = new ArrayList<>();

    public static class Item {
        public int idRomaneio;
        public String numero;
        public BigDecimal valor;
        public Item() { }
        public Item(int idRomaneio, BigDecimal valor) { this.idRomaneio = idRomaneio; this.valor = valor; }
    }

    public static boolean isFormaValida(String f) {
        if (f == null) return false;
        for (String x : FORMAS) if (x.equals(f)) return true;
        return false;
    }

    public int getIdPagamento() { return idPagamento; }
    public void setIdPagamento(int idPagamento) { this.idPagamento = idPagamento; }
    public int getIdParceiro() { return idParceiro; }
    public void setIdParceiro(int idParceiro) { this.idParceiro = idParceiro; }
    public String getNomeParceiro() { return nomeParceiro; }
    public void setNomeParceiro(String nomeParceiro) { this.nomeParceiro = nomeParceiro; }
    public LocalDate getDataPagamento() { return dataPagamento; }
    public void setDataPagamento(LocalDate dataPagamento) { this.dataPagamento = dataPagamento; }
    public String getForma() { return forma; }
    public void setForma(String forma) { this.forma = forma; }
    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }
    public String getBanco() { return banco; }
    public void setBanco(String banco) { this.banco = banco; }
    public String getNumeroCheque() { return numeroCheque; }
    public void setNumeroCheque(String numeroCheque) { this.numeroCheque = numeroCheque; }
    public String getEmitente() { return emitente; }
    public void setEmitente(String emitente) { this.emitente = emitente; }
    public LocalDate getBomPara() { return bomPara; }
    public void setBomPara(LocalDate bomPara) { this.bomPara = bomPara; }
    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }
    public BigDecimal getValorNoRomaneio() { return valorNoRomaneio; }
    public void setValorNoRomaneio(BigDecimal valorNoRomaneio) { this.valorNoRomaneio = valorNoRomaneio; }
    public List<Item> getItens() { return itens; }
    public void setItens(List<Item> itens) { this.itens = itens; }
}
