package Model.Model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Lote de produção colhida por regime PRODUCAO, consolidado de um diário de Colheita
 * finalizado (1 lote por diário — ver {@code uk_colheita_lote_diario}).
 */
public class ColheitaLote {
    private int idLote;
    private int idDiario;
    private String numeroDiario;   // exibição
    private Integer idQuadra;      private String quadraNome;
    private Integer idCultura;     private String culturaNome;
    private String siglaArea;      // sigla da Área de Produção dona do talhão (AreaProducao.SiglasAreaProducao)
    private Integer idArea;        private String areaNome;   // Área de Produção do talhão (modal Processar)
    private LocalDate dataColheita;
    private BigDecimal quantidadeTotal = BigDecimal.ZERO;
    private BigDecimal quantidadeProcessada = BigDecimal.ZERO;
    private String unidade = "CX";
    private String status = "COLHEIDO";
    private String observacoes;

    public int getIdLote() { return idLote; }
    public void setIdLote(int idLote) { this.idLote = idLote; }
    public int getIdDiario() { return idDiario; }
    public void setIdDiario(int idDiario) { this.idDiario = idDiario; }
    public String getNumeroDiario() { return numeroDiario; }
    public void setNumeroDiario(String numeroDiario) { this.numeroDiario = numeroDiario; }
    public Integer getIdQuadra() { return idQuadra; }
    public void setIdQuadra(Integer idQuadra) { this.idQuadra = idQuadra; }
    public String getQuadraNome() { return quadraNome; }
    public void setQuadraNome(String quadraNome) { this.quadraNome = quadraNome; }
    public Integer getIdCultura() { return idCultura; }
    public void setIdCultura(Integer idCultura) { this.idCultura = idCultura; }
    public String getCulturaNome() { return culturaNome; }
    public void setCulturaNome(String culturaNome) { this.culturaNome = culturaNome; }
    public String getSiglaArea() { return siglaArea; }
    public void setSiglaArea(String siglaArea) { this.siglaArea = siglaArea; }
    public Integer getIdArea() { return idArea; }
    public void setIdArea(Integer idArea) { this.idArea = idArea; }
    public String getAreaNome() { return areaNome; }
    public void setAreaNome(String areaNome) { this.areaNome = areaNome; }
    public LocalDate getDataColheita() { return dataColheita; }
    public void setDataColheita(LocalDate dataColheita) { this.dataColheita = dataColheita; }
    public BigDecimal getQuantidadeTotal() { return quantidadeTotal; }
    public void setQuantidadeTotal(BigDecimal quantidadeTotal) { this.quantidadeTotal = quantidadeTotal; }
    public BigDecimal getQuantidadeProcessada() { return quantidadeProcessada; }
    public void setQuantidadeProcessada(BigDecimal quantidadeProcessada) { this.quantidadeProcessada = quantidadeProcessada; }
    public String getUnidade() { return unidade; }
    public void setUnidade(String unidade) { this.unidade = unidade; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
}
