package Model.Model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class FuncionarioEmpreita extends Funcionario {

    private BigDecimal valorFixoAcordado = BigDecimal.ZERO;

 public FuncionarioEmpreita(
        int idPessoa, String nomePessoa, String usuarioPessoa,
        String senhaPessoa, String nivelPessoa, boolean situacaoPessoa,
        String emailPessoa, String telefonePessoa, String cep,
        int numero, String complemento, String cpfPf,
        LocalDate dataNascimentoPf, String matricula,
        TipoFuncionario tipoFuncionario, String cargo,
        LocalDate dataFim, LocalDate dataInicio) {

    super(idPessoa, nomePessoa, usuarioPessoa,
          senhaPessoa, nivelPessoa, situacaoPessoa, emailPessoa,
          telefonePessoa, cep, numero, complemento, cpfPf,
          dataNascimentoPf,
          matricula, cargo, tipoFuncionario,
          dataInicio, dataFim);

    setTipoFuncionario(TipoFuncionario.EMPREITA);
}

    public BigDecimal calcularPagamento(LocalDate inicio, LocalDate fim) {
        return valorFixoAcordado;
    }

    public BigDecimal getValorFixoAcordado() {
        return valorFixoAcordado;
    }

    public void setValorFixoAcordado(BigDecimal valorFixoAcordado) {
        this.valorFixoAcordado =
                valorFixoAcordado != null ? valorFixoAcordado : BigDecimal.ZERO;
    }
}
