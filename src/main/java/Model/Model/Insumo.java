package Model.Model;

import java.math.BigDecimal;

/**
 * Insumo agrícola (defensivo ou adubo) do controle de estoque.
 * quantidade_disponivel e preco_medio são o saldo corrente (atualizados a cada
 * movimentação); estoque_minimo dispara o alerta de reposição.
 */
public class Insumo {

    private int idInsumo;
    private String nome;
    private String categoria;             // DEFENSIVO | ADUBO
    private String grandeza;              // MASSA | CAPACIDADE | UNIDADE
    private String unidade;               // G | KG | T | ML | L | UN
    private BigDecimal quantidadeDisponivel = BigDecimal.ZERO;
    private BigDecimal precoMedio = BigDecimal.ZERO;
    private BigDecimal estoqueMinimo = BigDecimal.ZERO;
    private Integer idFornecedor;         // parceiro tipo INSUMO (opcional/padrão)
    private String fornecedorNome;        // preenchido só para exibição
    private boolean situacao = true;

    public int getIdInsumo() { return idInsumo; }
    public void setIdInsumo(int idInsumo) { this.idInsumo = idInsumo; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public String getGrandeza() { return grandeza; }
    public void setGrandeza(String grandeza) { this.grandeza = grandeza; }

    public String getUnidade() { return unidade; }
    public void setUnidade(String unidade) { this.unidade = unidade; }

    public BigDecimal getQuantidadeDisponivel() { return quantidadeDisponivel; }
    public void setQuantidadeDisponivel(BigDecimal q) { this.quantidadeDisponivel = q; }

    public BigDecimal getPrecoMedio() { return precoMedio; }
    public void setPrecoMedio(BigDecimal precoMedio) { this.precoMedio = precoMedio; }

    public BigDecimal getEstoqueMinimo() { return estoqueMinimo; }
    public void setEstoqueMinimo(BigDecimal estoqueMinimo) { this.estoqueMinimo = estoqueMinimo; }

    public Integer getIdFornecedor() { return idFornecedor; }
    public void setIdFornecedor(Integer idFornecedor) { this.idFornecedor = idFornecedor; }

    public String getFornecedorNome() { return fornecedorNome; }
    public void setFornecedorNome(String fornecedorNome) { this.fornecedorNome = fornecedorNome; }

    public boolean isSituacao() { return situacao; }
    public void setSituacao(boolean situacao) { this.situacao = situacao; }
}
