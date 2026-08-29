package Service;

import Model.Dao.FaltaFuncionarioDAO;
import Model.Dao.FechamentoFolhaDAO;
import Model.Dao.PontoEletronicoDAO;
import Model.Dao.ValeFuncionarioDAO;
import Model.Model.FechamentoFolha;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.YearMonth;

/**
 * Fechamento mensal do CLT urbano.
 *
 * <h3>Fórmula anterior e o que havia de errado</h3>
 * <pre>
 * valorDia       = salarioBase / diasUteis      &lt;-- P1-9: a lei manda dividir por 30
 * valorHora      = valorDia / jornadaHorasDia
 * descontoFaltas = faltasInjustificadas x valorDia
 * valorExtras    = (minutosExtras / 60) x valorHora x 1,50
 * salarioBruto   = salarioBase - descontoFaltas + valorExtras
 * salarioLiquido = salarioBruto - totalVales    &lt;-- P1-9: sem INSS, IRRF, FGTS nem DSR
 * </pre>
 *
 * <p>Duas consequências concretas:</p>
 * <ul>
 *   <li><b>Desconto de falta a maior.</b> Com {@code salário / diasÚteis}, uma
 *       falta tirava ≈4,5% do salário; pela CLT (art. 64, divisor 30) o devido é
 *       ≈3,3%. Diferença cobrada do trabalhador em toda falta injustificada,
 *       com risco trabalhista.</li>
 *   <li><b>Líquido que não é líquido.</b> Sem INSS e IRRF, o valor exibido não
 *       correspondia ao que o trabalhador recebe; sem FGTS, o custo da empresa
 *       ficava subestimado.</li>
 * </ul>
 *
 * <h3>Fórmula atual</h3>
 * <pre>
 * diasUteis      = dias do mês sem sábado, domingo E FERIADO   (P1-10)
 * valorDia       = salarioBase / 30                            (CLT art. 64)
 * valorHora      = salarioBase / (jornada x 30)
 * descontoFaltas = faltas x valorDia
 * descontoDsr    = semanas com falta x valorDia                (P1-9: DSR perdido)
 * valorExtras    = horasExtras x valorHora x 1,50
 * remuneracao    = salarioBase + valorExtras - descontoFaltas - descontoDsr
 * INSS/IRRF/FGTS = EncargosService (tabelas por vigência, no banco)
 * liquido        = remuneracao - INSS - IRRF - vales
 * </pre>
 */
public class FolhaCalculoService {

    private final PontoEletronicoDAO pontoDAO   = new PontoEletronicoDAO();
    private final FaltaFuncionarioDAO faltaDAO  = new FaltaFuncionarioDAO();
    private final ValeFuncionarioDAO  valeDAO   = new ValeFuncionarioDAO();
    private final FechamentoFolhaDAO  fechDAO   = new FechamentoFolhaDAO();

    private static final BigDecimal ADICIONAL_HORA_EXTRA = new BigDecimal("1.50");
    private static final BigDecimal SESSENTA = BigDecimal.valueOf(60);

    public FechamentoFolha calcularEPersistir(int idFuncionario, String periodo,
                                              BigDecimal salarioBase, int jornadaHoras)
            throws SQLException {
        return calcularEPersistir(idFuncionario, periodo, salarioBase, jornadaHoras, 0);
    }

    /**
     * Calcula e persiste o fechamento do mês.
     *
     * @param dependentes número de dependentes para a dedução do IRRF
     */
    public FechamentoFolha calcularEPersistir(int idFuncionario, String periodo,
                                              BigDecimal salarioBase, int jornadaHoras,
                                              int dependentes) throws SQLException {

        YearMonth mes = YearMonth.parse(periodo);
        BigDecimal salario = salarioBase != null ? salarioBase : BigDecimal.ZERO;
        int jornada = jornadaHoras > 0 ? jornadaHoras : 8;

        // P1-10: dias úteis agora descontam feriados (fixos e móveis).
        int diasUteis      = ParametrosFolhaService.diasUteis(mes);
        int faltasInj      = faltaDAO.contarInjustificadas(idFuncionario, periodo);
        int minutosExtras  = pontoDAO.somarMinutosExtras(idFuncionario, periodo);
        BigDecimal totalVales = valeDAO.somarPorMes(idFuncionario, periodo);
        if (totalVales == null) totalVales = BigDecimal.ZERO;

        // P1-9: divisor legal 30, não os dias úteis.
        BigDecimal valorDia  = ParametrosFolhaService.valorDiaFolha(salario);
        BigDecimal valorHora = ParametrosFolhaService.valorHoraFolha(salario, jornada);

        BigDecimal descontoFaltas = valorDia.multiply(BigDecimal.valueOf(faltasInj))
                .setScale(2, RoundingMode.HALF_UP);

        // P1-9: cada semana com falta injustificada faz perder o descanso semanal
        // remunerado. Não existia no cálculo anterior.
        int semanasComFalta = faltaDAO.contarSemanasComFaltaInjustificada(idFuncionario, periodo);
        BigDecimal descontoDsr = valorDia.multiply(BigDecimal.valueOf(semanasComFalta))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal horasExtras = BigDecimal.valueOf(minutosExtras)
                .divide(SESSENTA, 4, RoundingMode.HALF_UP);
        BigDecimal valorExtras = horasExtras.multiply(valorHora).multiply(ADICIONAL_HORA_EXTRA)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal remuneracao = salario.add(valorExtras)
                .subtract(descontoFaltas).subtract(descontoDsr)
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        EncargosService.Encargos enc =
                EncargosService.calcular(remuneracao, dependentes, mes.atDay(1));

        BigDecimal liquido = remuneracao
                .subtract(enc.inss()).subtract(enc.irrf()).subtract(totalVales)
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        FechamentoFolha f = new FechamentoFolha();
        f.setIdFuncionario(idFuncionario);
        f.setPeriodo(periodo);
        f.setSalarioBase(salario.setScale(2, RoundingMode.HALF_UP));
        f.setJornadaHorasDia(jornada);
        f.setDiasUteis(diasUteis);
        f.setDiasTrabalhados(diasUteis - faltasInj);
        f.setFaltasInjustificadas(faltasInj);
        f.setTotalMinutosExtras(minutosExtras);
        f.setValorHorasExtras(valorExtras);
        f.setDescontoFaltas(descontoFaltas);
        f.setDescontoDsr(descontoDsr);
        f.setTotalVales(totalVales.setScale(2, RoundingMode.HALF_UP));
        f.setSalarioBruto(remuneracao);
        f.setDescontoInss(enc.inss());
        f.setDescontoIrrf(enc.irrf());
        f.setFgtsDeposito(enc.fgts());
        f.setDependentes(dependentes);
        f.setEncargosConfiaveis(enc.tabelaConfiavel());
        f.setSalarioLiquido(liquido);

        fechDAO.salvar(f);
        return f;
    }

    public FechamentoFolha buscar(int idFuncionario, String periodo) throws SQLException {
        return fechDAO.buscarPorPeriodo(idFuncionario, periodo);
    }

    /**
     * Dias úteis do mês.
     *
     * @deprecated Duplicava a mesma regra que existia em
     *             {@code FolhaCalculoRuralService} e ignorava feriados (P1-10 e
     *             P1-11). Use {@link ParametrosFolhaService#diasUteis(String)}.
     */
    @Deprecated
    public static int calcularDiasUteis(String periodo) {
        return ParametrosFolhaService.diasUteis(periodo);
    }
}
