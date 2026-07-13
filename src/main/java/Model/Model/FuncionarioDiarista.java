package Model.Model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class FuncionarioDiarista extends Funcionario {

    private static final Logger LOGGER =
            Logger.getLogger(FuncionarioDiarista.class.getName());

    private BigDecimal valorPorDia = BigDecimal.ZERO;

public FuncionarioDiarista(
        int idPessoa, String nomePessoa, String usuarioPessoa,
        String senhaPessoa, String nivelPessoa, boolean situacaoPessoa,
        String emailPessoa, String telefonePessoa, String cep,
        int numero, String complemento, String cpfPf,
        LocalDate dataNascimentoPf, String matricula, TipoFuncionario tipoFuncionario,
        String cargo, LocalDate dataFim, LocalDate dataInicio) {

    super(idPessoa, nomePessoa, usuarioPessoa,
          senhaPessoa, nivelPessoa, situacaoPessoa, emailPessoa,
          telefonePessoa, cep, numero, complemento, cpfPf,
          dataNascimentoPf,
          matricula, cargo, tipoFuncionario,
          dataInicio, dataFim);

    setTipoFuncionario(TipoFuncionario.DIARISTA);
}


    public BigDecimal getValorPorDia() {
        return valorPorDia;
    }

    public void setValorPorDia(BigDecimal valorPorDia) {
        this.valorPorDia = valorPorDia != null ? valorPorDia : BigDecimal.ZERO;
    }
}
