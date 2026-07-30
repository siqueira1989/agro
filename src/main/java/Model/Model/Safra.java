package Model.Model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Safra (ciclo de produção) que agrupa talhões num período. */
public class Safra {
    private int idSafra;
    private String nome;
    private Integer idCulturaPrincipal;   private String culturaNome;
    private LocalDate dataInicial;
    private LocalDate dataFinal;
    private String status = "PLANEJADA";   // PLANEJADA | EM_ANDAMENTO | FINALIZADA
    private BigDecimal estimativaProducao = BigDecimal.ZERO;
    private String unidadeProducao = "SACA";  // SACA | TONELADA
    private BigDecimal despesasFixas = BigDecimal.ZERO;
    private List<SafraTalhao> talhoes = new ArrayList<>();

    public int getIdSafra() { return idSafra; }
    public void setIdSafra(int idSafra) { this.idSafra = idSafra; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public Integer getIdCulturaPrincipal() { return idCulturaPrincipal; }
    public void setIdCulturaPrincipal(Integer v) { this.idCulturaPrincipal = v; }
    public String getCulturaNome() { return culturaNome; }
    public void setCulturaNome(String culturaNome) { this.culturaNome = culturaNome; }
    public LocalDate getDataInicial() { return dataInicial; }
    public void setDataInicial(LocalDate dataInicial) { this.dataInicial = dataInicial; }
    public LocalDate getDataFinal() { return dataFinal; }
    public void setDataFinal(LocalDate dataFinal) { this.dataFinal = dataFinal; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getEstimativaProducao() { return estimativaProducao; }
    public void setEstimativaProducao(BigDecimal v) { this.estimativaProducao = v; }
    public String getUnidadeProducao() { return unidadeProducao; }
    public void setUnidadeProducao(String unidadeProducao) { this.unidadeProducao = unidadeProducao; }
    public BigDecimal getDespesasFixas() { return despesasFixas; }
    public void setDespesasFixas(BigDecimal despesasFixas) { this.despesasFixas = despesasFixas; }
    public List<SafraTalhao> getTalhoes() { return talhoes; }
    public void setTalhoes(List<SafraTalhao> talhoes) { this.talhoes = talhoes; }
}
