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
        return calcularFechamento(f, periodo, 0);
    }

    /**
     * @param dependentes numero de dependentes para a deducao do IRRF (P1-9)
     */
    public FechamentoFolha calcularFechamento(FuncionarioCLT f, String periodo, int dependentes)
            throws java.sql.SQLException {

        int id = f.getIdPessoa();
        YearMonth ym = YearMonth.parse(periodo);
        LocalDate primeiroDia = ym.atDay(1);
        LocalDate ultimoDia    = ym.atEndOfMonth();

        // Termos do VÍNCULO que cobre o período (fonte da verdade); se não
        // houver, usa os dados de funcionarioclt como transição.
        // P1-12: era buscarPorData(id, primeiroDia). Funcionario admitido no dia
        // 10 nao tinha vinculo valido no dia 1, entao vinc ficava null, o calculo
        // caia no fallback de funcionarioclt ou em ZERO e o mes da admissao saia
        // com FOLHA ZERADA. Agora basta o vinculo se sobrepor ao periodo.
        Model.Model.Vinculo vinc = vinculoDAO.buscarPorPeriodo(id, primeiroDia, ultimoDia);
        // P1-10/P1-11: dias uteis vem do servico unico e ja descontam feriados.
        int diasUteis  = ParametrosFolhaService.diasUteis(ym);
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

            // P1-13: a folga compensatoria era DEDUZIDA de "existe um dia entre
            // segunda e sabado sem ponto e sem falta". Como quase ninguem tem
            // ponto no sabado, praticamente toda semana era dada como compensada
            // e o domingo trabalhado deixava de ser pago a 100%. Agora a folga e
            // um registro explicito no ponto do dia.
            boolean folga = false;
            for (int i = 0; i < 6; i++) {                 // seg..sáb
                LocalDate dia = seg.plusDays(i);
                if (dia.isBefore(primeiroDia) || dia.isAfter(ultimoDia)) continue;
                PontoEletronico pd = pontoPorDia.get(dia);
                if (pd != null && pd.isFolgaCompensatoria()) { folga = true; break; }
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

        // P1-9: para salario mensal o valor-dia legal e salario/30 (CLT art. 64),
        // nao salario/diasUteis. Com o divisor antigo, cada falta descontava
        // ~4,5% do salario em vez dos ~3,3% devidos.
        BigDecimal valorDia  = ParametrosFolhaService.valorDiaFolha(salario);
        BigDecimal valorHora = ParametrosFolhaService.valorHoraFolha(salario, jornadaHrs);

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

        // PENDENCIA ABERTA (P1-14 da auditoria, mantida por decisao do cliente):
        // nos dias em modo alternativo o codigo deixa de contar falta e hora
        // extra, mas NAO abate o dia do salario mensal — o funcionario recebe o
        // dia pelo salario e novamente pela producao/empreita. Se for premio de
        // produtividade, esta correto e basta documentar; se nao for, e pagamento
        // em duplicidade. A ser validado com o RH/contabilidade.
        BigDecimal bruto = salario.add(valorExtraNormal).add(valorExtra100).add(valorAdicNoturno)
                .add(valorProducao).add(valorEmpreita)
                .setScale(2, RoundingMode.HALF_UP);

        // Remuneracao efetiva do mes: e sobre ela que INSS e IRRF incidem.
        BigDecimal remuneracao = bruto.subtract(descontoFaltas).subtract(descontoDsr)
                .max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        // P1-9: nao havia INSS, IRRF nem FGTS — o "liquido" era so bruto - vales.
        EncargosService.Encargos enc =
                EncargosService.calcular(remuneracao, dependentes, primeiroDia);

        BigDecimal liquido = remuneracao
                .subtract(enc.inss()).subtract(enc.irrf()).subtract(totalVales)
                .max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

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
        fc.setDescontoInss(enc.inss());
        fc.setDescontoIrrf(enc.irrf());
        fc.setFgtsDeposito(enc.fgts());
        fc.setDependentes(dependentes);
        fc.setEncargosConfiaveis(enc.tabelaConfiavel());
        return fc;
    }

    private BigDecimal horas(int minutos) {
        return BigDecimal.valueOf(minutos).divide(C60, 4, RoundingMode.HALF_UP);
    }

    /**
     * @deprecated P1-11: era uma copia da mesma regra que existia em
     *             {@code FolhaCalculoService}, e ambas ignoravam feriados.
     *             Use {@link ParametrosFolhaService#diasUteis(YearMonth)}.
     */
    @Deprecated
    private int contarDiasUteis(YearMonth ym) {
        return ParametrosFolhaService.diasUteis(ym);
    }
}
