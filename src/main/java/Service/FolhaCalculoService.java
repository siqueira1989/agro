package Service;

import Model.Dao.FaltaFuncionarioDAO;
import Model.Dao.FechamentoFolhaDAO;
import Model.Dao.PontoEletronicoDAO;
import Model.Dao.ValeFuncionarioDAO;
import Model.Model.FechamentoFolha;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Regras de negócio para fechamento mensal:
 *
 *  valorDia       = salarioBase / diasUteis
 *  valorHora      = valorDia / jornadaHorasDia
 *  descontoFaltas = faltasInjustificadas × valorDia
 *  valorExtras    = (totalMinutosExtras / 60) × valorHora × 1,50
 *  salarioBruto   = salarioBase - descontoFaltas + valorExtras
 *  salarioLiquido = salarioBruto - totalVales
 */
public class FolhaCalculoService {

    private final PontoEletronicoDAO pontoDAO   = new PontoEletronicoDAO();
    private final FaltaFuncionarioDAO faltaDAO  = new FaltaFuncionarioDAO();
    private final ValeFuncionarioDAO  valeDAO   = new ValeFuncionarioDAO();
    private final FechamentoFolhaDAO  fechDAO   = new FechamentoFolhaDAO();

    /**
     * Calcula e persiste o fechamento do mês para o funcionário.
     *
     * @param idFuncionario  ID do funcionário
     * @param periodo        Formato YYYY-MM
     * @param salarioBase    Salário de referência informado pelo usuário
     * @param jornadaHoras   Jornada diária padrão (tipicamente 8)
     * @return               FechamentoFolha com todos os valores calculados
     */
    public FechamentoFolha calcularEPersistir(int idFuncionario, String periodo,
                                              BigDecimal salarioBase, int jornadaHoras)
            throws SQLException {

        // --- dias úteis do mês (seg a sex, sem feriados) ---
        int diasUteis = calcularDiasUteis(periodo);

        // --- dados do período ---
        int faltasInj       = faltaDAO.contarInjustificadas(idFuncionario, periodo);
        int minutosExtras   = pontoDAO.somarMinutosExtras(idFuncionario, periodo);
        BigDecimal totalVales = valeDAO.somarPorMes(idFuncionario, periodo);

        // --- cálculos ---
        BigDecimal valorDia  = salarioBase.divide(
                BigDecimal.valueOf(diasUteis > 0 ? diasUteis : 1), 4, RoundingMode.HALF_UP);
        BigDecimal valorHora = valorDia.divide(
                BigDecimal.valueOf(jornadaHoras > 0 ? jornadaHoras : 8), 4, RoundingMode.HALF_UP);

        BigDecimal descontoFaltas = valorDia
                .multiply(BigDecimal.valueOf(faltasInj))
                .setScale(2, RoundingMode.HALF_UP);

        // hora extra = valor hora × 1,50 (adicional de 50 %)
        BigDecimal horasExtras = BigDecimal.valueOf(minutosExtras).divide(
                BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);
        BigDecimal valorExtras = horasExtras
                .multiply(valorHora)
                .multiply(new BigDecimal("1.50"))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal salarioBruto   = salarioBase.subtract(descontoFaltas).add(valorExtras)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal salarioLiquido = salarioBruto.subtract(totalVales)
                .setScale(2, RoundingMode.HALF_UP);

        // --- monta VO ---
        FechamentoFolha f = new FechamentoFolha();
        f.setIdFuncionario(idFuncionario);
        f.setPeriodo(periodo);
        f.setSalarioBase(salarioBase.setScale(2, RoundingMode.HALF_UP));
        f.setJornadaHorasDia(jornadaHoras);
        f.setDiasUteis(diasUteis);
        f.setDiasTrabalhados(diasUteis - faltasInj);
        f.setFaltasInjustificadas(faltasInj);
        f.setTotalMinutosExtras(minutosExtras);
        f.setValorHorasExtras(valorExtras);
        f.setDescontoFaltas(descontoFaltas);
        f.setTotalVales(totalVales.setScale(2, RoundingMode.HALF_UP));
        f.setSalarioBruto(salarioBruto);
        f.setSalarioLiquido(salarioLiquido);

        // --- persiste (upsert) ---
        fechDAO.salvar(f);
        return f;
    }

    /** Busca fechamento já salvo para um período. */
    public FechamentoFolha buscar(int idFuncionario, String periodo) throws SQLException {
        return fechDAO.buscarPorPeriodo(idFuncionario, periodo);
    }

    // ── helper: contar dias úteis (seg–sex) no mês ──────────────────────────
    public static int calcularDiasUteis(String periodo) {
        // periodo = "YYYY-MM"
        String[] partes = periodo.split("-");
        YearMonth ym = YearMonth.of(Integer.parseInt(partes[0]), Integer.parseInt(partes[1]));
        int count = 0;
        for (int d = 1; d <= ym.lengthOfMonth(); d++) {
            DayOfWeek dow = LocalDate.of(ym.getYear(), ym.getMonth(), d).getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) count++;
        }
        return count;
    }
}
