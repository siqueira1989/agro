package Model.Model;

import java.math.BigDecimal;

/**
 * Insumo consumido numa atividade do Diário (N por diário). Ao finalizar, dá baixa no estoque
 * (baixado=true).
 *
 * <p>Regra de negócio: {@code quantidade} (o que é debitado do estoque) NÃO é digitada direto —
 * é calculada no servidor, de duas formas possíveis (uma ou outra, nunca as duas):</p>
 * <ul>
 *   <li><b>DEFENSIVO</b> (aplicação por calda): {@code dosePorUnidade * doseTotalAplicada} — dose
 *       do produto por litro/kg de calda, vezes o total de calda aplicada.</li>
 *   <li><b>ADUBO</b> (aplicação direta, sem calda): {@code quantidadeDireta} — a quantidade já na
 *       unidade do insumo (kg/tonelada), normalmente calculada no cliente a partir do nº de sacos
 *       × peso do saco (cadastro do insumo), mas o servidor só confia no valor final informado
 *       aqui — não reconstrói a partir de "nº de sacos" (esse nº não é enviado nem gravado).</li>
 * </ul>
 */
public class DiarioInsumo {
    private int id;
    private int idDiario;
    private int idInsumo;
    private String insumoNome;          // exibição
    private BigDecimal quantidade = BigDecimal.ZERO;   // calculado: ver regra de negócio acima
    private String unidade;             // snapshot (g/mL/kg/L do cadastro do insumo)
    private BigDecimal dosePorUnidade;      // produto por litro/kg de calda (DEFENSIVO)
    private BigDecimal doseTotalAplicada;   // total de calda (litros/kg) aplicada (DEFENSIVO)
    private BigDecimal quantidadeDireta;    // quantidade aplicada direto, na unidade do insumo (ADUBO)
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
    public BigDecimal getDosePorUnidade() { return dosePorUnidade; }
    public void setDosePorUnidade(BigDecimal dosePorUnidade) { this.dosePorUnidade = dosePorUnidade; }
    public BigDecimal getDoseTotalAplicada() { return doseTotalAplicada; }
    public void setDoseTotalAplicada(BigDecimal doseTotalAplicada) { this.doseTotalAplicada = doseTotalAplicada; }
    public BigDecimal getQuantidadeDireta() { return quantidadeDireta; }
    public void setQuantidadeDireta(BigDecimal quantidadeDireta) { this.quantidadeDireta = quantidadeDireta; }
    public BigDecimal getCustoUnitario() { return custoUnitario; }
    public void setCustoUnitario(BigDecimal custoUnitario) { this.custoUnitario = custoUnitario; }
    public BigDecimal getCusto() { return custo; }
    public void setCusto(BigDecimal custo) { this.custo = custo; }
    public boolean isBaixado() { return baixado; }
    public void setBaixado(boolean baixado) { this.baixado = baixado; }
}
