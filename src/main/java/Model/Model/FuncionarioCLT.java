package Model.Model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class FuncionarioCLT extends Funcionario {

    public enum StatusEmprego { ATIVO, DESLIGADO, AFASTADO }

    /** Adicional noturno rural (Lei 5.889/73): 25% sobre a hora noturna. */
    public static final BigDecimal ADICIONAL_NOTURNO_PCT = new BigDecimal("0.25");

    private BigDecimal salarioMensal    = BigDecimal.ZERO;
    private BigDecimal valorHoraExtra   = BigDecimal.ZERO;
    private int        cargaHorariaDiaria = 8;
    private StatusEmprego statusEmprego = StatusEmprego.ATIVO;
    private TipoAtividadeRural tipoAtividadeRural = TipoAtividadeRural.LAVOURA;

    public FuncionarioCLT(int idPessoa, String nomePessoa, String usuarioPessoa,
            String senhaPessoa, String nivelPessoa, boolean situacaoPessoa, String emailPessoa,
            String telefonePessoa, String cep, int numero, String complemento, String cpfPf,
            LocalDate dataNascimentoPf, String matricula, TipoFuncionario tipoFuncionario,
            String cargo, LocalDate dataFim, LocalDate dataInicio) {
        super(idPessoa, nomePessoa, usuarioPessoa, senhaPessoa, nivelPessoa, situacaoPessoa,
              emailPessoa, telefonePessoa, cep, numero, complemento, cpfPf,
              dataNascimentoPf, matricula, cargo, tipoFuncionario, dataInicio, dataFim);
        setTipoFuncionario(TipoFuncionario.CLT);
    }

    public FuncionarioCLT() {}

    // ── getters / setters ──────────────────────────────────────

    public BigDecimal getSalarioMensal() { return salarioMensal; }
    public void setSalarioMensal(BigDecimal v) {
        this.salarioMensal = v != null ? v : BigDecimal.ZERO;
    }

    public BigDecimal getValorHoraExtra() { return valorHoraExtra; }
    public void setValorHoraExtra(BigDecimal v) {
        this.valorHoraExtra = v != null ? v : BigDecimal.ZERO;
    }

    public int getCargaHorariaDiaria() { return cargaHorariaDiaria; }
    public void setCargaHorariaDiaria(int v) {
        this.cargaHorariaDiaria = v > 0 ? v : 8;
    }

    public StatusEmprego getStatusEmprego() { return statusEmprego; }
    public void setStatusEmprego(StatusEmprego v) {
        this.statusEmprego = v != null ? v : StatusEmprego.ATIVO;
    }

    public TipoAtividadeRural getTipoAtividadeRural() { return tipoAtividadeRural; }
    public void setTipoAtividadeRural(TipoAtividadeRural v) {
        this.tipoAtividadeRural = v != null ? v : TipoAtividadeRural.LAVOURA;
    }

    public String getTipoAtividadeRuralStr() {
        return tipoAtividadeRural != null ? tipoAtividadeRural.name() : "LAVOURA";
    }

    public String getStatusEmpregoStr() {
        return statusEmprego != null ? statusEmprego.name() : "ATIVO";
    }

    /** Valor de um minuto de trabalho baseado no salário e jornada mensal. */
    public BigDecimal getValorMinuto(int diasUteisNoMes) {
        if (salarioMensal.compareTo(BigDecimal.ZERO) == 0 || diasUteisNoMes <= 0) {
            return BigDecimal.ZERO;
        }
        int minutosMensais = diasUteisNoMes * cargaHorariaDiaria * 60;
        return salarioMensal.divide(BigDecimal.valueOf(minutosMensais), 6,
                java.math.RoundingMode.HALF_UP);
    }
}
