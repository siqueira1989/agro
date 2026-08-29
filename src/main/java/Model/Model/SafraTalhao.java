package Model.Model;

import java.math.BigDecimal;

/** Vínculo N:N entre Safra e Talhão (quadra), com a área destinada na safra. */
public class SafraTalhao {
    private int id;
    private int idSafra;
    private int idQuadra;
    private String quadraNome;                 // exibição
    private String alimentoNome;                // exibição (cultura plantada na quadra)
    private int numeroPlantas;                  // exibição (base do rateio)
    private BigDecimal areaHa = BigDecimal.ZERO; // área real do talhão (exibição/validação)
    private BigDecimal areaDestinadaHa = BigDecimal.ZERO;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getIdSafra() { return idSafra; }
    public void setIdSafra(int idSafra) { this.idSafra = idSafra; }
    public int getIdQuadra() { return idQuadra; }
    public void setIdQuadra(int idQuadra) { this.idQuadra = idQuadra; }
    public String getQuadraNome() { return quadraNome; }
    public void setQuadraNome(String quadraNome) { this.quadraNome = quadraNome; }
    public String getAlimentoNome() { return alimentoNome; }
    public void setAlimentoNome(String alimentoNome) { this.alimentoNome = alimentoNome; }
    public int getNumeroPlantas() { return numeroPlantas; }
    public void setNumeroPlantas(int numeroPlantas) { this.numeroPlantas = numeroPlantas; }
    public BigDecimal getAreaHa() { return areaHa; }
    public void setAreaHa(BigDecimal areaHa) { this.areaHa = areaHa; }
    public BigDecimal getAreaDestinadaHa() { return areaDestinadaHa; }
    public void setAreaDestinadaHa(BigDecimal v) { this.areaDestinadaHa = v; }
}
