package Model.Model;

import java.math.BigDecimal;

/** Máquina/equipamento usado numa atividade do Diário (registro livre; sem
 *  cadastro próprio ainda — id_maquina reservado para uso futuro). */
public class DiarioMaquina {
    private int id;
    private int idDiario;
    private Integer idMaquina;          // cadastro futuro
    private String categoria;           // TRATOR | IMPLEMENTO | VEICULO
    private String nome;
    private BigDecimal horimetroInicial = BigDecimal.ZERO;
    private BigDecimal horimetroFinal = BigDecimal.ZERO;
    private BigDecimal horasTrabalhadas = BigDecimal.ZERO;
    private BigDecimal valorHora = BigDecimal.ZERO;
    private BigDecimal custo = BigDecimal.ZERO;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getIdDiario() { return idDiario; }
    public void setIdDiario(int idDiario) { this.idDiario = idDiario; }
    public Integer getIdMaquina() { return idMaquina; }
    public void setIdMaquina(Integer idMaquina) { this.idMaquina = idMaquina; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public BigDecimal getHorimetroInicial() { return horimetroInicial; }
    public void setHorimetroInicial(BigDecimal v) { this.horimetroInicial = v; }
    public BigDecimal getHorimetroFinal() { return horimetroFinal; }
    public void setHorimetroFinal(BigDecimal v) { this.horimetroFinal = v; }
    public BigDecimal getHorasTrabalhadas() { return horasTrabalhadas; }
    public void setHorasTrabalhadas(BigDecimal v) { this.horasTrabalhadas = v; }
    public BigDecimal getValorHora() { return valorHora; }
    public void setValorHora(BigDecimal v) { this.valorHora = v; }
    public BigDecimal getCusto() { return custo; }
    public void setCusto(BigDecimal custo) { this.custo = custo; }
}
