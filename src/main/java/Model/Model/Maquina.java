package Model.Model;

import java.math.BigDecimal;

/**
 * Maquinário: Trator, Implemento (custo por HORA) ou Veículo (custo por KM).
 * Para trator/implemento o custo_hora efetivo pode ser composto por
 * combustível + manutenção + depreciação (boas práticas de custo agrícola).
 */
public class Maquina {
    private int idMaquina;
    private String nome;
    private String tipo;              // TRATOR | IMPLEMENTO | VEICULO
    private String marca;
    private String identificacao;     // placa / patrimônio
    private BigDecimal custoHora = BigDecimal.ZERO;
    private BigDecimal custoKm = BigDecimal.ZERO;
    private BigDecimal combustivelHora = BigDecimal.ZERO;
    private BigDecimal manutencaoHora = BigDecimal.ZERO;
    private BigDecimal depreciacaoHora = BigDecimal.ZERO;
    private boolean situacao = true;

    public int getIdMaquina() { return idMaquina; }
    public void setIdMaquina(int idMaquina) { this.idMaquina = idMaquina; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }
    public String getIdentificacao() { return identificacao; }
    public void setIdentificacao(String identificacao) { this.identificacao = identificacao; }
    public BigDecimal getCustoHora() { return custoHora; }
    public void setCustoHora(BigDecimal v) { this.custoHora = v; }
    public BigDecimal getCustoKm() { return custoKm; }
    public void setCustoKm(BigDecimal v) { this.custoKm = v; }
    public BigDecimal getCombustivelHora() { return combustivelHora; }
    public void setCombustivelHora(BigDecimal v) { this.combustivelHora = v; }
    public BigDecimal getManutencaoHora() { return manutencaoHora; }
    public void setManutencaoHora(BigDecimal v) { this.manutencaoHora = v; }
    public BigDecimal getDepreciacaoHora() { return depreciacaoHora; }
    public void setDepreciacaoHora(BigDecimal v) { this.depreciacaoHora = v; }
    public boolean isSituacao() { return situacao; }
    public void setSituacao(boolean situacao) { this.situacao = situacao; }
}
