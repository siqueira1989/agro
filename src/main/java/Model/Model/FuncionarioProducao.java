package Model.Model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class FuncionarioProducao extends Funcionario {

    private int quantidadeProduzidaNoPeriodo;
    private BigDecimal valorPorUnidade = BigDecimal.ZERO;

public FuncionarioProducao(int idPessoa, String nomePessoa, String usuarioPessoa,
        String senhaPessoa, String nivelPessoa, boolean situacaoPessoa, String emailPessoa,
        String telefonePessoa, String cep, int numero, String complemento, String cpfPf,
        LocalDate dataNascimentoPf, String matricula, TipoFuncionario tipoFuncionario,
        String cargo, LocalDate dataFim, LocalDate dataInicio) {

    super(idPessoa, nomePessoa, usuarioPessoa,
          senhaPessoa, nivelPessoa, situacaoPessoa, emailPessoa,
          telefonePessoa, cep, numero, complemento, cpfPf,
          dataNascimentoPf,
          matricula, cargo, tipoFuncionario,
          dataInicio, dataFim);

    setTipoFuncionario(TipoFuncionario.PRODUCAO);
}

    public BigDecimal calcularPagamento(LocalDate inicio, LocalDate fim) {
        return valorPorUnidade.multiply(
                BigDecimal.valueOf(Math.max(0, quantidadeProduzidaNoPeriodo))
        );
    }

    public int getQuantidadeProduzidaNoPeriodo() {
        return quantidadeProduzidaNoPeriodo;
    }

    public void setQuantidadeProduzidaNoPeriodo(int quantidadeProduzidaNoPeriodo) {
        this.quantidadeProduzidaNoPeriodo = Math.max(0, quantidadeProduzidaNoPeriodo);
    }

    public BigDecimal getValorPorUnidade() {
        return valorPorUnidade;
    }

    public void setValorPorUnidade(BigDecimal valorPorUnidade) {
        this.valorPorUnidade = valorPorUnidade != null ? valorPorUnidade : BigDecimal.ZERO;
    }
}
