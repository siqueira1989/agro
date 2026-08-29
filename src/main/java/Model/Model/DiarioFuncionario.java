package Model.Model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Funcionário alocado numa atividade do Diário (N por diário, podendo haver vários dias de execução). */
public class DiarioFuncionario {
    private int id;
    private int idDiario;
    private int idPessoa;
    private String nomePessoa;          // exibição
    private String tipoFuncionario;     // CLT | DIARISTA | EMPREITA (snapshot)
    private String funcaoExercida;
    private BigDecimal horasTrabalhadas = BigDecimal.ZERO;
    private BigDecimal custoHora = BigDecimal.ZERO;        // snapshot
    private BigDecimal valorContratado = BigDecimal.ZERO;  // empreita
    private BigDecimal custo = BigDecimal.ZERO;            // snapshot no finalizar
    private LocalDate dataExecucao;     // dia em que esta leva foi trabalhada (permite múltiplas execuções por diário)

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getIdDiario() { return idDiario; }
    public void setIdDiario(int idDiario) { this.idDiario = idDiario; }
    public int getIdPessoa() { return idPessoa; }
    public void setIdPessoa(int idPessoa) { this.idPessoa = idPessoa; }
    public String getNomePessoa() { return nomePessoa; }
    public void setNomePessoa(String nomePessoa) { this.nomePessoa = nomePessoa; }
    public String getTipoFuncionario() { return tipoFuncionario; }
    public void setTipoFuncionario(String tipoFuncionario) { this.tipoFuncionario = tipoFuncionario; }
    public String getFuncaoExercida() { return funcaoExercida; }
    public void setFuncaoExercida(String funcaoExercida) { this.funcaoExercida = funcaoExercida; }
    public BigDecimal getHorasTrabalhadas() { return horasTrabalhadas; }
    public void setHorasTrabalhadas(BigDecimal horasTrabalhadas) { this.horasTrabalhadas = horasTrabalhadas; }
    public BigDecimal getCustoHora() { return custoHora; }
    public void setCustoHora(BigDecimal custoHora) { this.custoHora = custoHora; }
    public BigDecimal getValorContratado() { return valorContratado; }
    public void setValorContratado(BigDecimal valorContratado) { this.valorContratado = valorContratado; }
    public BigDecimal getCusto() { return custo; }
    public void setCusto(BigDecimal custo) { this.custo = custo; }
    public LocalDate getDataExecucao() { return dataExecucao; }
    public void setDataExecucao(LocalDate dataExecucao) { this.dataExecucao = dataExecucao; }
}
