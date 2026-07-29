package Model.Model;

import java.math.BigDecimal;

/** Insumo consumido numa atividade do Diário (N por diário). Ao finalizar,
 *  dá baixa no estoque (baixado=true). */
public class DiarioInsumo {
    private int id;
    private int idDiario;
    private int idInsumo;
    private String insumoNome;          // exibição
    private BigDecimal quantidade = BigDecimal.ZERO;
    private String unidade;             // snapshot
    private String doseAplicada;
    private BigDecimal custoUnitario = BigDecimal.ZERO;  // snapshot preco_medio
    private BigDecimal custo = BigDecimal.ZERO;
    private boolean baixado = false;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getIdDiario() { return idDiario; }
    public void setIdDiario(int idDiario) { this.idDiario = idDiario; }
    public int getIdInsumo() { return idInsumo; }
    public void setIdInsumo(int idInsumo) { this.idInsumo = idInsumo; }
    public String getInsumoNome() { return insumoNome; }
    public void setInsumoNome(String insumoNome) { this.insumoNome = insumoNome; }
    public BigDecimal getQuantidade() { return quantidade; }
    public void setQuantidade(BigDecimal quantidade) { this.quantidade = quantidade; }
    public String getUnidade() { return unidade; }
    public void setUnidade(String unidade) { this.unidade = unidade; }
    public String getDoseAplicada() { return doseAplicada; }
    public void setDoseAplicada(String doseAplicada) { this.doseAplicada = doseAplicada; }
    public BigDecimal getCustoUnitario() { return custoUnitario; }
    public void setCustoUnitario(BigDecimal custoUnitario) { this.custoUnitario = custoUnitario; }
    public BigDecimal getCusto() { return custo; }
    public void setCusto(BigDecimal custo) { this.custo = custo; }
    public boolean isBaixado() { return baixado; }
    public void setBaixado(boolean baixado) { this.baixado = baixado; }
}
