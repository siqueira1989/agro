package Service;

import Model.Dao.FaltaFuncionarioDAO;
import Model.Dao.PontoEletronicoDAO;
import Model.Dao.ValeFuncionarioDAO;
import Model.Model.FaltaFuncionario;
import Model.Model.FechamentoFolha;
import Model.Model.FuncionarioCLT;
import Model.Model.PontoEletronico;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fechamento mensal para o trabalhador RURAL (Lei 5.889/73).
 *
 * Diferenças em relação ao CLT urbano:
 *  - DSR: cada semana (seg–dom) com falta injustificada desconta 1 DSR.
 *  - Domingo trabalhado SEM folga compensatória na semana → 100% extra
 *    (todas as horas do domingo). COM folga → domingo conta como dia normal.
 *  - Adicional noturno de 25% sobre as horas noturnas (já contadas em
 *    minutos reais no ponto).
 *
 * Base de rateio mantida consistente com o sistema: valorDia = salário/diasUteis.
 */
public class FolhaCalculoRuralService {

    private final PontoEletronicoDAO  pontoDAO = new PontoEletronicoDAO();
    private final FaltaFuncionarioDAO faltaDAO = new FaltaFuncionarioDAO();
    private final ValeFuncionarioDAO  valeDAO  = new ValeFuncionarioDAO();
    private final Model.Dao.VinculoDAO vinculoDAO = new Model.Dao.VinculoDAO();
    private final Model.Dao.ProducaoCaixaDAO    producaoDAO = new Model.Dao.ProducaoCaixaDAO();
    private final Model.Dao.LancamentoEmpreitaDAO empreitaDAO = new Model.Dao.LancamentoEmpreitaDAO();

    private static final BigDecimal C60 = BigDecimal.valueOf(60);

    /** Calcula o fechamento mensal rural (sem persistir). */
    public FechamentoFolha calcularFechamento(FuncionarioCLT f, String periodo)
            throws java.sql.SQLException {

        int id = f.getIdPessoa();
        YearMonth ym = YearMonth.parse(periodo);
        LocalDate primeiroDia = ym.atDay(1);
        LocalDate ultimoDia    = ym.atEndOfMonth();

        // Termos do VÍNCULO que cobre o período (fonte da verdade); se não
        // houver, usa os dados de funcionarioclt como transição.
        Model.Model.Vinculo vinc = vinculoDAO.buscarPorData(id, primeiroDia);
        int diasUteis  = contarDiasUteis(ym);
        int jornadaHrs = (vinc != null ? vinc.getCargaHorariaDiaria() : f.getCargaHorariaDiaria());
        if (jornadaHrs <= 0) jornadaHrs = 8;

        List<PontoEletronico>  pontos = pontoDAO.listarPorMes(id, periodo);
        List<FaltaFuncionario> faltas = faltaDAO.listarPorMes(id, periodo);
        BigDecimal totalVales = valeDAO.somarPorMes(id, periodo);
        if (totalVales == null) totalVales = BigDecimal.ZERO;

        // Multi-modo: dias em que o CLT trabalhou em produção/empreita.
        // Nesses dias não conta falta nem hora extra; ganha por produção/empreita.
        java.util.Set<LocalDate> diasModoAlt = new java.util.HashSet<>();
        for (Map<String, Object> lp : producaoDAO.listarPorMes(id, periodo)) {
            diasModoAlt.add(LocalDate.parse((String) lp.get("dataProducao")));
        }
        for (Model.Model.LancamentoEmpreita le : empreitaDAO.listarPorMes(id, periodo)) {
            diasModoAlt.add(le.getDataEmpreita());
        }
        BigDecimal valorProducao = producaoDAO.somarValor(id, periodo);
        BigDecimal valorEmpreita = empreitaDAO.somarPorMes(id, periodo);
        if (valorProducao == null) valorProducao = BigDecimal.ZERO;
        if (valorEmpreita == null) valorEmpreita = BigDecimal.ZERO;

        // Índices por dia
        Map<LocalDate, PontoEletronico> pontoPorDia = new HashMap<>();
        for (PontoEletronico p : pontos) pontoPorDia.put(p.getDataRegistro(), p);

        Map<LocalDate, Boolean> faltaInjustPorDia = new HashMap<>();
        int faltasInjust = 0;
        for (FaltaFuncionario fa : faltas) {
            // Ignora falta em dia de modo alternativo (produção/empreita)
            if (!fa.isJustificada() && !diasModoAlt.contains(fa.getDataFalta())) {
                faltaInjustPorDia.put(fa.getDataFalta(), true);
                faltasInjust++;
            }
        }

        // Agrupa por semana (segunda-feira como chave)
        Map<LocalDate, List<LocalDate>> semanas = new HashMap<>();
        for (PontoEletronico p : pontos) {
            LocalDate seg = p.getDataRegistro().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            semanas.computeIfAbsent(seg, k -> new ArrayList<>()).add(p.getDataRegistro());
        }
        // Garante semanas que só têm falta (sem ponto) também entrem para o DSR
        for (LocalDate dFalta : faltaInjustPorDia.keySet()) {
            LocalDate seg = dFalta.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            semanas.computeIfAbsent(seg, k -> new ArrayList<>());
        }

        int minutosExtraNormal = 0;
        int minutosExtra100    = 0;
        int minutosNoturnos    = 0;
        int semanasComFalta    = 0;

        for (Map.Entry<LocalDate, List<LocalDate>> e : semanas.entrySet()) {
            LocalDate seg = e.getKey();

            // Folga compensatória: algum dia útil (seg–sáb) dentro do mês sem ponto e sem falta
            boolean folga = false;
            for (int i = 0; i < 6; i++) {                 // seg..sáb
                LocalDate dia = seg.plusDays(i);
                if (dia.isBefore(primeiroDia) || dia.isAfter(ultimoDia)) continue;
                boolean temPonto = pontoPorDia.containsKey(dia);
                boolean temFalta = faltaInjustPorDia.containsKey(dia);
                if (!temPonto && !temFalta) { folga = true; break; }
            }

            // DSR: semana com falta injustificada
            boolean semanaTemFalta = false;
            for (int i = 0; i < 7; i++) {
                LocalDate dia = seg.plusDays(i);
                if (faltaInjustPorDia.containsKey(dia)) { semanaTemFalta = true; break; }
            }
            if (semanaTemFalta) semanasComFalta++;

            // Percorre os dias com ponto da semana
            for (LocalDate dia : e.getValue()) {
                PontoEletronico p = pontoPorDia.get(dia);
                if (p == null) continue;
                if (diasModoAlt.contains(dia)) continue;  // dia de produção/empreita: sem extra/noturno CLT
                minutosNoturnos += p.getMinutosNoturnos();

                boolean isDomingo = dia.getDayOfWeek() == DayOfWeek.SUNDAY;
                if (isDomingo && !folga) {
                    // Domingo sem compensação → todas as horas a 100%
                    minutosExtra100 += p.getTotalMinutos();
                } else {
                    // Dia normal (ou domingo compensado) → só o excedente é extra normal
                    minutosExtraNormal += p.getExtraMinutos();
                }
            }
        }

        // ── valores financeiros (salário/hora-extra vêm do VÍNCULO do período) ──
        BigDecimal salario = (vinc != null && vinc.getSalarioMensal() != null)
                ? vinc.getSalarioMensal()
                : (f.getSalarioMensal() != null ? f.getSalarioMensal() : BigDecimal.ZERO);
        BigDecimal vheCadastrado = (vinc != null) ? vinc.getValorHoraExtra() : f.getValorHoraExtra();

        BigDecimal valorDia = diasUteis > 0
                ? salario.divide(BigDecimal.valueOf(diasUteis), 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal valorHora = jornadaHrs > 0
                ? valorDia.divide(BigDecimal.valueOf(jornadaHrs), 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Hora extra normal: valor cadastrado se houver; senão hora × 1,5
        BigDecimal valorHoraExtra = (vheCadastrado != null
                && vheCadastrado.compareTo(BigDecimal.ZERO) > 0)
                ? vheCadastrado
                : valorHora.multiply(new BigDecimal("1.50"));

        BigDecimal valorExtraNormal = horas(minutosExtraNormal).multiply(valorHoraExtra)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal valorExtra100 = horas(minutosExtra100).multiply(valorHora)
                .multiply(new BigDecimal("2.00")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal valorAdicNoturno = horas(minutosNoturnos).multiply(valorHora)
                .multiply(FuncionarioCLT.ADICIONAL_NOTURNO_PCT).setScale(2, RoundingMode.HALF_UP);

        BigDecimal descontoFaltas = valorDia.multiply(BigDecimal.valueOf(faltasInjust))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal descontoDsr = valorDia.multiply(BigDecimal.valueOf(semanasComFalta))
                .setScale(2, RoundingMode.HALF_UP);

        // Ganhos de produção/empreita (dias em modo alternativo) somam ao bruto
        BigDecimal bruto = salario.add(valorExtraNormal).add(valorExtra100).add(valorAdicNoturno)
                .add(valorProducao).add(valorEmpreita)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal liquido = bruto.subtract(descontoFaltas).subtract(descontoDsr)
                .subtract(totalVales).setScale(2, RoundingMode.HALF_UP);
        if (liquido.compareTo(BigDecimal.ZERO) < 0) liquido = BigDecimal.ZERO;

        // ── monta VO ──
        FechamentoFolha fc = new FechamentoFolha();
        fc.setIdFuncionario(id);
        fc.setNomeFuncionario(f.getNomePessoa());
        fc.setPeriodo(periodo);
        fc.setSalarioBase(salario.setScale(2, RoundingMode.HALF_UP));
        fc.setJornadaHorasDia(jornadaHrs);
        fc.setDiasUteis(diasUteis);
        fc.setDiasTrabalhados(pontos.size());
        fc.setFaltasInjustificadas(faltasInjust);
        fc.setTotalMinutosExtras(minutosExtraNormal);
        fc.setValorHorasExtras(valorExtraNormal);
        fc.setDescontoFaltas(descontoFaltas);
        fc.setTotalVales(totalVales.setScale(2, RoundingMode.HALF_UP));
        fc.setSalarioBruto(bruto);
        fc.setSalarioLiquido(liquido);
        fc.setDescontoDsr(descontoDsr);
        fc.setValorAdicionalNoturno(valorAdicNoturno);
        fc.setValorExtra100(valorExtra100);
        fc.setMinutosNoturnos(minutosNoturnos);
        fc.setMinutosExtra100(minutosExtra100);
        fc.setValorProducao(valorProducao.setScale(2, RoundingMode.HALF_UP));
        fc.setValorEmpreita(valorEmpreita.setScale(2, RoundingMode.HALF_UP));
        return fc;
    }

    private BigDecimal horas(int minutos) {
        return BigDecimal.valueOf(minutos).divide(C60, 4, RoundingMode.HALF_UP);
    }

    private int contarDiasUteis(YearMonth ym) {
        int count = 0;
        for (int d = 1; d <= ym.lengthOfMonth(); d++) {
            DayOfWeek dow = LocalDate.of(ym.getYear(), ym.getMonthValue(), d).getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) count++;
        }
        return count;
    }
}
