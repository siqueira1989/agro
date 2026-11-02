package Model.Model;

import java.math.BigDecimal;

/**
 * Embalagem do Processamento por alimento + área (V31). Dois preços: o pago ao embalador por
 * caixa (Processar) e o de compra pela empresa (custo da embalagem no Romaneio).
 */
public class Embalagem {
    private int idEmbalagem;
    private String codigo;          // [A-Z0-9_]{1,30}, normalizado (DespesasCustosDAO.normalizarTipoCaixa)
    private String descricao;
    private BigDecimal pesoKg;
    private Integer idAlimento;     private String nomeAlimento;   // nome só leitura (JOIN)
    private Integer idArea;         private String nomeArea;       private String siglaArea;
    private BigDecimal precoCompra;
    private BigDecimal precoEmbalador;
    private boolean ativo = true;

    public int getIdEmbalagem() { return idEmbalagem; }
    public void setIdEmbalagem(int idEmbalagem) { this.idEmbalagem = idEmbalagem; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public BigDecimal getPesoKg() { return pesoKg; }
    public void setPesoKg(BigDecimal pesoKg) { this.pesoKg = pesoKg; }
    public Integer getIdAlimento() { return idAlimento; }
    public void setIdAlimento(Integer idAlimento) { this.idAlimento = idAlimento; }
    public String getNomeAlimento() { return nomeAlimento; }
    public void setNomeAlimento(String nomeAlimento) { this.nomeAlimento = nomeAlimento; }
    public Integer getIdArea() { return idArea; }
    public void setIdArea(Integer idArea) { this.idArea = idArea; }
    public String getNomeArea() { return nomeArea; }
    public void setNomeArea(String nomeArea) { this.nomeArea = nomeArea; }
    public String getSiglaArea() { return siglaArea; }
    public void setSiglaArea(String siglaArea) { this.siglaArea = siglaArea; }
    public BigDecimal getPrecoCompra() { return precoCompra; }
    public void setPrecoCompra(BigDecimal precoCompra) { this.precoCompra = precoCompra; }
    public BigDecimal getPrecoEmbalador() { return precoEmbalador; }
    public void setPrecoEmbalador(BigDecimal precoEmbalador) { this.precoEmbalador = precoEmbalador; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
}
