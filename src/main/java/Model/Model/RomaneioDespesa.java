package Model.Model;

import java.math.BigDecimal;

/**
 * Despesa de venda do romaneio (snapshot do momento). Forma: FIXO (valor digitado), PERCENTUAL_VENDA
 * (percentual × venda bruta) ou POR_CAIXA (quantidade × valor unitário). EMBALAGEM é legado: a linha
 * automática de custo de embalagem deixou de ser gerada em 02/11 (virou custo de produção) e é descartada ao salvar.
 */
public class RomaneioDespesa {
    public static final String FIXO = "FIXO", PERCENTUAL_VENDA = "PERCENTUAL_VENDA", POR_CAIXA = "POR_CAIXA", EMBALAGEM = "EMBALAGEM";

    private int id;
    private Integer idDespesasCusto;
    private String descricao;
    private String formaCalculo;
    private BigDecimal percentual;
    private BigDecimal quantidade;
    private BigDecimal valorUnitario;
    private BigDecimal valor = BigDecimal.ZERO;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public Integer getIdDespesasCusto() { return idDespesasCusto; }
    public void setIdDespesasCusto(Integer idDespesasCusto) { this.idDespesasCusto = idDespesasCusto; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getFormaCalculo() { return formaCalculo; }
    public void setFormaCalculo(String formaCalculo) { this.formaCalculo = formaCalculo; }
    public BigDecimal getPercentual() { return percentual; }
    public void setPercentual(BigDecimal percentual) { this.percentual = percentual; }
    public BigDecimal getQuantidade() { return quantidade; }
    public void setQuantidade(BigDecimal quantidade) { this.quantidade = quantidade; }
    public BigDecimal getValorUnitario() { return valorUnitario; }
    public void setValorUnitario(BigDecimal valorUnitario) { this.valorUnitario = valorUnitario; }
    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }
}
