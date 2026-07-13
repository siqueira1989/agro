package Model.Model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Vínculo empregatício — um período de trabalho de uma pessoa na empresa.
 * Uma pessoa pode ter vários vínculos (não sobrepostos) ao longo do tempo,
 * cada um com seus próprios termos (salário, hora extra, jornada, atividade,
 * cargo, datas e status). A matrícula permanece na pessoa (constante).
 */
public class Vinculo {

    private int        idVinculo;
    private int        idPessoa;
    private String     nomePessoa;          // conveniência (join), não persiste aqui
    private TipoFuncionario tipoFuncionario;
    private String     cargo;
    private TipoAtividadeRural tipoAtividadeRural = TipoAtividadeRural.LAVOURA;
    private LocalDate  dataAdmissao;
    private LocalDate  dataDesligamento;     // null = vínculo ativo
    private BigDecimal salarioMensal     = BigDecimal.ZERO;
    private BigDecimal valorHoraExtra    = BigDecimal.ZERO;
    private int        cargaHorariaDiaria = 8;
    private String     status            = "ATIVO";  // ATIVO | DESLIGADO | AFASTADO
    private String     motivoDesligamento;

    public Vinculo() {}

    public boolean isAtivo() { return dataDesligamento == null; }

    // ── getters / setters ──────────────────────────────────────
    public int getIdVinculo()                  { return idVinculo; }
    public void setIdVinculo(int v)            { this.idVinculo = v; }

    public int getIdPessoa()                   { return idPessoa; }
    public void setIdPessoa(int v)             { this.idPessoa = v; }

    public String getNomePessoa()              { return nomePessoa; }
    public void setNomePessoa(String v)        { this.nomePessoa = v; }

    public TipoFuncionario getTipoFuncionario() { return tipoFuncionario; }
    public void setTipoFuncionario(TipoFuncionario v) { this.tipoFuncionario = v; }

    public String getCargo()                   { return cargo; }
    public void setCargo(String v)             { this.cargo = v; }

    public TipoAtividadeRural getTipoAtividadeRural() { return tipoAtividadeRural; }
    public void setTipoAtividadeRural(TipoAtividadeRural v) {
        this.tipoAtividadeRural = v != null ? v : TipoAtividadeRural.LAVOURA;
    }

    public LocalDate getDataAdmissao()         { return dataAdmissao; }
    public void setDataAdmissao(LocalDate v)   { this.dataAdmissao = v; }

    public LocalDate getDataDesligamento()     { return dataDesligamento; }
    public void setDataDesligamento(LocalDate v) { this.dataDesligamento = v; }

    public BigDecimal getSalarioMensal()       { return salarioMensal; }
    public void setSalarioMensal(BigDecimal v) { this.salarioMensal = v != null ? v : BigDecimal.ZERO; }

    public BigDecimal getValorHoraExtra()      { return valorHoraExtra; }
    public void setValorHoraExtra(BigDecimal v){ this.valorHoraExtra = v != null ? v : BigDecimal.ZERO; }

    public int getCargaHorariaDiaria()         { return cargaHorariaDiaria; }
    public void setCargaHorariaDiaria(int v)   { this.cargaHorariaDiaria = v > 0 ? v : 8; }

    public String getStatus()                  { return status; }
    public void setStatus(String v)            { this.status = v != null ? v : "ATIVO"; }

    public String getMotivoDesligamento()      { return motivoDesligamento; }
    public void setMotivoDesligamento(String v){ this.motivoDesligamento = v; }
}
