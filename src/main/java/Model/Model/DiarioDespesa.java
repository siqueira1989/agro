package Model.Model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Despesa lançada numa execução do Diário (N por diário). Cada compra vira sua
 * própria linha — uma despesa variável comprada 3 vezes em preços diferentes
 * (ex.: marmita a R$10, R$15 e R$20) gera 3 linhas somadas, nunca uma média.
 */
public class DiarioDespesa {
    private int id;
    private int idDiario;
    private int idDespesaCusto;
    private String descricao;           // snapshot do nome no lançamento
    private String classificacao;       // FIXO | VARIAVEL — exibição
    private BigDecimal quantidade = BigDecimal.ONE;
    private BigDecimal valorUnitario = BigDecimal.ZERO;
    private BigDecimal valor = BigDecimal.ZERO;   // quantidade * valorUnitario, gravado no insert
    private LocalDate dataExecucao;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getIdDiario() { return idDiario; }
    public void setIdDiario(int idDiario) { this.idDiario = idDiario; }
    public int getIdDespesaCusto() { return idDespesaCusto; }
    public void setIdDespesaCusto(int idDespesaCusto) { this.idDespesaCusto = idDespesaCusto; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getClassificacao() { return classificacao; }
    public void setClassificacao(String classificacao) { this.classificacao = classificacao; }
    public BigDecimal getQuantidade() { return quantidade; }
    public void setQuantidade(BigDecimal quantidade) { this.quantidade = quantidade; }
    public BigDecimal getValorUnitario() { return valorUnitario; }
    public void setValorUnitario(BigDecimal valorUnitario) { this.valorUnitario = valorUnitario; }
    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }
    public LocalDate getDataExecucao() { return dataExecucao; }
    public void setDataExecucao(LocalDate dataExecucao) { this.dataExecucao = dataExecucao; }
}
