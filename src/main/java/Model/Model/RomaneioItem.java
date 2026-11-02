package Model.Model;

import java.math.BigDecimal;

/** Item do romaneio: caixas de uma classificação numa embalagem, com o preço de venda (NULL até informar). */
public class RomaneioItem {
    private int id;
    private int idClassificacao;   private String nomeClassificacao;
    private int idEmbalagem;       private String descricaoEmbalagem;
    private BigDecimal caixas;
    private BigDecimal precoUnitario;
    private BigDecimal valor = BigDecimal.ZERO;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getIdClassificacao() { return idClassificacao; }
    public void setIdClassificacao(int idClassificacao) { this.idClassificacao = idClassificacao; }
    public String getNomeClassificacao() { return nomeClassificacao; }
    public void setNomeClassificacao(String nomeClassificacao) { this.nomeClassificacao = nomeClassificacao; }
    public int getIdEmbalagem() { return idEmbalagem; }
    public void setIdEmbalagem(int idEmbalagem) { this.idEmbalagem = idEmbalagem; }
    public String getDescricaoEmbalagem() { return descricaoEmbalagem; }
    public void setDescricaoEmbalagem(String descricaoEmbalagem) { this.descricaoEmbalagem = descricaoEmbalagem; }
    public BigDecimal getCaixas() { return caixas; }
    public void setCaixas(BigDecimal caixas) { this.caixas = caixas; }
    public BigDecimal getPrecoUnitario() { return precoUnitario; }
    public void setPrecoUnitario(BigDecimal precoUnitario) { this.precoUnitario = precoUnitario; }
    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }
}
