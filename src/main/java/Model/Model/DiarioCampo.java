package Model.Model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Diário de Campo — cabeçalho de uma operação realizada num talhão, com suas
 * coleções de funcionários, máquinas e insumos, e os custos consolidados.
 */
public class DiarioCampo {
    private int idDiario;
    private String numeroDiario;
    private LocalDate data;
    private Integer idArea;         private String areaNome;
    private Integer idQuadra;       private String quadraNome;
    private Integer idCultura;      private String culturaNome;
    private Integer idResponsavel;  private String responsavelNome;
    private int idTipoAtividade;    private String tipoAtividadeNome;
    private Integer idSafra;        private String safraNome;
    private String descricao;
    private String status = "PLANEJADA";
    private LocalDate dataPrevista;
    private LocalDate dataRealizada;
    private LocalTime horaInicio;
    private LocalTime horaFim;
    private String observacoes;
    private BigDecimal custoMaoObra = BigDecimal.ZERO;
    private BigDecimal custoInsumos = BigDecimal.ZERO;
    private BigDecimal custoMaquinas = BigDecimal.ZERO;
    private BigDecimal custoTotal = BigDecimal.ZERO;

    private List<DiarioFuncionario> funcionarios = new ArrayList<>();
    private List<DiarioMaquina> maquinas = new ArrayList<>();
    private List<DiarioInsumo> insumos = new ArrayList<>();

    public int getIdDiario() { return idDiario; }
    public void setIdDiario(int idDiario) { this.idDiario = idDiario; }
    public String getNumeroDiario() { return numeroDiario; }
    public void setNumeroDiario(String numeroDiario) { this.numeroDiario = numeroDiario; }
    public LocalDate getData() { return data; }
    public void setData(LocalDate data) { this.data = data; }
    public Integer getIdArea() { return idArea; }
    public void setIdArea(Integer idArea) { this.idArea = idArea; }
    public String getAreaNome() { return areaNome; }
    public void setAreaNome(String areaNome) { this.areaNome = areaNome; }
    public Integer getIdQuadra() { return idQuadra; }
    public void setIdQuadra(Integer idQuadra) { this.idQuadra = idQuadra; }
    public String getQuadraNome() { return quadraNome; }
    public void setQuadraNome(String quadraNome) { this.quadraNome = quadraNome; }
    public Integer getIdCultura() { return idCultura; }
    public void setIdCultura(Integer idCultura) { this.idCultura = idCultura; }
    public String getCulturaNome() { return culturaNome; }
    public void setCulturaNome(String culturaNome) { this.culturaNome = culturaNome; }
    public Integer getIdResponsavel() { return idResponsavel; }
    public void setIdResponsavel(Integer idResponsavel) { this.idResponsavel = idResponsavel; }
    public String getResponsavelNome() { return responsavelNome; }
    public void setResponsavelNome(String responsavelNome) { this.responsavelNome = responsavelNome; }
    public int getIdTipoAtividade() { return idTipoAtividade; }
    public void setIdTipoAtividade(int idTipoAtividade) { this.idTipoAtividade = idTipoAtividade; }
    public String getTipoAtividadeNome() { return tipoAtividadeNome; }
    public void setTipoAtividadeNome(String tipoAtividadeNome) { this.tipoAtividadeNome = tipoAtividadeNome; }
    public Integer getIdSafra() { return idSafra; }
    public void setIdSafra(Integer idSafra) { this.idSafra = idSafra; }
    public String getSafraNome() { return safraNome; }
    public void setSafraNome(String safraNome) { this.safraNome = safraNome; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDate getDataPrevista() { return dataPrevista; }
    public void setDataPrevista(LocalDate dataPrevista) { this.dataPrevista = dataPrevista; }
    public LocalDate getDataRealizada() { return dataRealizada; }
    public void setDataRealizada(LocalDate dataRealizada) { this.dataRealizada = dataRealizada; }
    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }
    public LocalTime getHoraFim() { return horaFim; }
    public void setHoraFim(LocalTime horaFim) { this.horaFim = horaFim; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
    public BigDecimal getCustoMaoObra() { return custoMaoObra; }
    public void setCustoMaoObra(BigDecimal v) { this.custoMaoObra = v; }
    public BigDecimal getCustoInsumos() { return custoInsumos; }
    public void setCustoInsumos(BigDecimal v) { this.custoInsumos = v; }
    public BigDecimal getCustoMaquinas() { return custoMaquinas; }
    public void setCustoMaquinas(BigDecimal v) { this.custoMaquinas = v; }
    public BigDecimal getCustoTotal() { return custoTotal; }
    public void setCustoTotal(BigDecimal v) { this.custoTotal = v; }
    public List<DiarioFuncionario> getFuncionarios() { return funcionarios; }
    public void setFuncionarios(List<DiarioFuncionario> funcionarios) { this.funcionarios = funcionarios; }
    public List<DiarioMaquina> getMaquinas() { return maquinas; }
    public void setMaquinas(List<DiarioMaquina> maquinas) { this.maquinas = maquinas; }
    public List<DiarioInsumo> getInsumos() { return insumos; }
    public void setInsumos(List<DiarioInsumo> insumos) { this.insumos = insumos; }
}
