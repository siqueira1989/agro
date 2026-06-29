package Model.Model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


public class Funcionario extends PessoaFisica {

    private String matricula;
    private TipoFuncionario tipoFuncionario;
    private LocalDate dataInicio;
    private LocalDate dataFim;
    private String cargo;

public Funcionario() {}

public Funcionario(
        int idPessoa, String nomePessoa, String usuarioPessoa, String senhaPessoa, 
        String nivelPessoa, boolean situacaoPessoa, String emailPessoa, String telefonePessoa, String cep,
        int numero, String complemento, String cpfPf, LocalDate dataNascimentoPf, String matricula, String cargo, TipoFuncionario tipoFuncionario,
LocalDate dataInicio, LocalDate dataFim)
{
    super(idPessoa, nomePessoa, usuarioPessoa, senhaPessoa, nivelPessoa,
          situacaoPessoa, emailPessoa, telefonePessoa, cep, numero, complemento,
          cpfPf, dataNascimentoPf);

    this.matricula = matricula;
    this.tipoFuncionario = tipoFuncionario;
    this.cargo = cargo;
    this.dataInicio = dataInicio;
    this.dataFim = dataFim;
}

    

    // getters/setters
    public String getMatricula() { return matricula; }
    public void setMatricula(String matricula) { this.matricula = matricula; }

    public TipoFuncionario getTipoFuncionario() { return tipoFuncionario; }
    public void setTipoFuncionario(TipoFuncionario tipoFuncionario) { this.tipoFuncionario = tipoFuncionario; }

    public LocalDate getDataInicio() { return dataInicio; }
    public void setDataInicio(LocalDate dataInicio) { this.dataInicio = dataInicio; }

    public LocalDate getDataFim() { return dataFim; }
    public void setDataFim(LocalDate dataFim) { this.dataFim = dataFim; }

    public String getCargo() { return cargo; }
    public void setCargo(String cargo) { this.cargo = cargo; }
}
