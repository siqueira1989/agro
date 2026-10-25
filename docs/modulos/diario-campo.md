# Módulo Diário de Campo

**Objetivo:** registro das atividades no talhão (agendamento, execução em vários dias, funcionários, máquinas, insumos, despesas) e cálculo do custo.
**Menu:** não tem item próprio — vive dentro do perfil da área (Produção → Área de Produção → Detalhe).

## Escopo
| Camada | Arquivo |
|---|---|
| Controller | `Controller/ControllerDiarioCampo.java` — URL `/ControllerDiarioCampo` |
| DAO | `Model/Dao/DiarioCampoDAO.java`, `Model/Dao/TipoAtividadeDAO.java` |
| Model | `DiarioCampo.java`, `DiarioDespesa.java`, `DiarioFuncionario.java`, `DiarioInsumo.java`, `DiarioMaquina.java`, `TipoAtividade.java` |
| View | `view/admin/DetalheAreaProducao.jsp`, **só a parte do Diário** |
| JS | `main.js` → seção `DIÁRIO DE CAMPO — dentro do Perfil da Área` (inclui AGENDAMENTO e EXECUÇÃO EM MÚLTIPLOS DIAS) |

**Ações:** `create`, `update`, `status`, `finalizar`, `addexecucao`, `addtipoatividade`, `recalcularpessoa`, `removerfuncionario`, `removerdespesa`.

## Banco
- Próprias: `diario_campo`, `diario_funcionario`, `diario_maquina`, `diario_insumo`, `diario_despesa`, `diario_tipo_atividade`.
- Migrações: V14, V17, V19, V23, V25, V26, V27, V29.
- Lê muitas tabelas de outros módulos: `safra`, `areaproducao`, `quadra`, `alimento`, `insumo`, `maquina`, `despesascusto`, `funcionario*`, `pessoa`, `vinculo_empregaticio`, `colheita_lote`.

## Dependências (módulo mais acoplado do sistema)
- **Safra** (`SafraDAO`), **Insumo/Estoque** (baixa de estoque, `EstoqueInsuficienteException`), **Maquinário** (custo/hora), **Despesas e Custos**, **Funcionário/CLT/Diarista/Empreita** (valor da diária/hora via `FolhaCalculoRuralService` e `ParametrosFolhaService`), **Colheita** (atividade "Colheita" gera lote).
- `DetalheAreaProducao.jsp` é dividida com **Área de Produção**.

## Processamento — como a Colheita cria o diário (onda 3)
Tipo de atividade "Processamento" (V29). Atividades com regime por caixa = Colheita e Processamento
(`dcEhAtividadeCaixa` no JS; `validarRegimeCaixa` no Controller).
- **Endpoint:** `POST /ControllerDiarioCampo` (JSON), `acao: "create"`:
  `{acao:"create", idArea, idQuadra, idCultura, idResponsavel, idTipoAtividade:<id do "Processamento">,
  status:"EM_ANDAMENTO", data:"aaaa-mm-dd", descricao:"Processamento do lote …", maquinas:[], insumos:[], despesas:[],
  funcionarios:[ {idPessoa, regimeTrabalho:"PRODUCAO", quantidadeCaixas, precoPorCaixa}   // embalador (por caixa)
               , {idPessoa, regimeTrabalho:"REGIME_SISTEMA", horasTrabalhadas} ]}`  // CLT/diarista/empreita (horas)
  Resposta `{ok,msg,iddiario,numeroDiario}`. Safra obrigatória (talhão + data), como em qualquer diário.
- Id do tipo: `GET /ControllerDiarioCampo?tiposatividade=1` e procurar `nome` "Processamento" (não fixar o id).
- Funcionários: `GET /ControllerDiarioCampo?funcionarios=1&data=aaaa-mm-dd` → `{idPessoa,nome,tipo,custoRef,custoRefExtra,cargo}`;
  Produção vem com `tipo:"PRODUCAO"`, `cargo` (Colhedor/Embalador) e `custoRef` = preço/caixa do cadastro.
- Regras no servidor: Produção só em PRODUCAO; Colhedor é recusado no Processamento; caixas obrigatórias só para
  PRODUCAO (horas para os demais). Mais dias: `acao:"addexecucao"`; fechar: `acao:"finalizar"` (não gera lote — só a Colheita gera).
- O vínculo diário de Processamento ↔ `colheita_lote` (quanto foi processado) é da Colheita; o Diário não guarda o lote.

## Pendências
- [ ] Relatórios/Safra não mostram ainda a dose por planta (`quantidade_por_planta_kg`); se quiserem, levar ao módulo do relatório.
- [ ] Onda 2: buscar o preço da caixa em Despesas (API da V28) ao lançar a Colheita e preencher o Preço/cx.
- [ ] Levar a Produção/Folha: funcionário de Produção lançado no Diário em regime PRODUCAO tem custo (valor_regime) no
  diário; conferir se o pagamento dele também sai por `lancamento_producao` (risco de pagar duas vezes a mesma caixa).
- [ ] Não há funcionário de Produção cadastrado no banco hoje: o filtro Colhedor/Embalador foi testado só na tela
  (opções simuladas) e pela validação do servidor; testar de novo com um cadastro real.

## Histórico
- 2026-08-25 — execução em múltiplos dias, valor de diarista/empreita automático.
- 2026-09-07 — despesas fixas/variáveis no diário.
- 2026-10-22 — Adubo por forma de aplicação (V27): a linha de Adubo tem "Forma / Quantidade" (Saco 5/10/25/50 kg, Tonelada ou kg, pré-selecionada pelo cadastro do insumo), "Plantas" (padrão: plantas do talhão, segue o talhão até ser editado) e "Dose por planta". O servidor (`DiarioCampoDAO.inserirInsumo`, `totalKgAdubo`, `kgParaUnidade`) calcula o total em kg, converte para a unidade do insumo (`quantidade_direta`/`quantidade`, debitada do estoque) e grava `quantidade_por_planta_kg`. Novas colunas em `diario_insumo`: `forma_aplicacao`, `peso_saco_kg`, `qtd_informada`, `plantas_aplicadas`, `quantidade_por_planta_kg`. O Controller valida o saco (só 5/10/25/50), a qtd > 0 e as plantas > 0. Lançamentos antigos reabrem como t/kg. Adubo que não é de massa (ex.: L) segue com a quantidade direta.
- 2026-10-25 — Melhorias da colheita, parte 1: (1) bug "não há safra" ao vir da página Colheita (`abrirColheita=1`):
  o `$.getJSON('/ControllerSafra')` não tinha promise e o modal abria antes de `dcTemSafra` ser preenchido — criados
  `dcSafraPromise` e `dcTiposPromise`, esperados no `abrirColheita` e em `dcAbrirNovo`. (2) Vindo da Colheita, depois
  de salvar volta para `colheitaestoque.jsp` (`dcVeioDaColheita`). (3) Colheita/Processamento: tabela de mão de obra
  em colunas Funcionário | Tipo | Regime | Preço | Caixas | Horas | Valor; demais tipos sem mudança. (4) Funcionário
  de Produção agora vem na lista (`tipo PRODUCAO` + `cargo`; antes vinha como "OUTRO" com custo 0): Colhedor só na
  Colheita, Embalador só no Processamento, sempre em regime Produção (travado) com o preço/caixa do cadastro
  sugerido; fora dessas atividades não aparece. Servidor valida (`DiarioCampoDAO.cargosProducao`). (5) V29: tipo
  "Processamento" — CLT/diarista/empreita por horas, embalador por caixa; não gera lote.
