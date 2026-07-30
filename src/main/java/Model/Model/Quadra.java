package Model.Model;

/**
 * Quadra de produção — subdivisão de uma propriedade (AreaProducao).
 * Cada quadra tem um nome, um número de plantas e um tipo de planta (alimento).
 * A soma das plantas das quadras ATIVAS é a quantidade total da área.
 */
public class Quadra {

    private int idQuadra;
    private int idAreaProducao;
    private String nomeQuadra;
    private int numeroPlantas;
    private java.math.BigDecimal areaHa = java.math.BigDecimal.ZERO;  // área real (ha)
    private Integer idAlimento;      // FK alimento(idproduto)
    private String alimentoNome;     // preenchido para exibição
    private boolean ativa = true;

    public int getIdQuadra() { return idQuadra; }
    public void setIdQuadra(int idQuadra) { this.idQuadra = idQuadra; }

    public int getIdAreaProducao() { return idAreaProducao; }
    public void setIdAreaProducao(int idAreaProducao) { this.idAreaProducao = idAreaProducao; }

    public String getNomeQuadra() { return nomeQuadra; }
    public void setNomeQuadra(String nomeQuadra) { this.nomeQuadra = nomeQuadra; }

    public int getNumeroPlantas() { return numeroPlantas; }
    public void setNumeroPlantas(int numeroPlantas) { this.numeroPlantas = numeroPlantas; }
    public java.math.BigDecimal getAreaHa() { return areaHa; }
    public void setAreaHa(java.math.BigDecimal areaHa) { this.areaHa = areaHa; }

    public Integer getIdAlimento() { return idAlimento; }
    public void setIdAlimento(Integer idAlimento) { this.idAlimento = idAlimento; }

    public String getAlimentoNome() { return alimentoNome; }
    public void setAlimentoNome(String alimentoNome) { this.alimentoNome = alimentoNome; }

    public boolean isAtiva() { return ativa; }
    public void setAtiva(boolean ativa) { this.ativa = ativa; }
}
