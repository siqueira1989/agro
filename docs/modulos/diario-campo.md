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
- **Ponto Eletrônico / CLT / Diarista** (04/11, **só leitura**): `ponto_eletronico` (presença = linha na data com
  `folga_compensatoria=false`; diarista marca sem horários) e `falta_funcionario` (qualquer falta no dia exclui).
  Usadas em `listarFuncionariosDisponiveis` (com data) e `semPresencaNoPonto` (`SQL_PRESENCA_PONTO`). Mudar o jeito
  de gravar presença/falta nesses módulos muda quem pode ser lançado no Diário.
- **Colheita/Embalagem** (V34): o Processar da Colheita chama `inserirInsumoBaixado(Connection…)` na transação dela (linha de embalagem já baixada). `recalcularCustos` mantém o `custo_unitario` das linhas `baixado=true` — mudar isso altera o custo de embalagem dos diários de Processamento e o custo de insumos de diários fechados.

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
- [ ] Levar a Produção/Folha: funcionário de Produção lançado no Diário em regime PRODUCAO tem custo (valor_regime) no
  diário; conferir se o pagamento dele também sai por `lancamento_producao` (risco de pagar duas vezes a mesma caixa).
- [ ] Duplicata antiga (04/11, não alterada): diário 2026-000079 (Adubação, CONCLUIDA) tem carla costa duas vezes em
  12/10/2026 (duas linhas em regime normal). É a única no banco.
- [ ] Sugestão à Colheita (Processar): conferir no cliente a mesma pessoa repetida entre as linhas "por horas" (hoje
  só a grade de embaladores confere); o servidor já recusa e a Colheita mostra "O Diário de Campo recusou…".

## Histórico
- 2026-11-04 — **Ajustes de 04/11** (Ajuste04_11_2026.docx):
  1. **Mesma pessoa uma vez por dia na atividade.** Servidor: `ControllerDiarioCampo#validarMaoDeObra` no `create` e no
     `addexecucao` (junta as novas com as já gravadas na data via `DiarioCampoDAO.funcionariosNaData`) → 400. Única
     exceção: várias linhas PRODUCAO da mesma pessoa com `funcao_exercida` diferentes (a grade do Processar grava uma
     linha por embalagem: "Embalador — <embalagem>"). Produção + Sistema da mesma pessoa (caso do print) é recusado.
     O `update` não valida: `atualizarCompleto` não grava `diario_funcionario` (mão de obra nova só entra por
     `addexecucao`) e as linhas históricas reenviadas têm datas misturadas. Tela: quem já está numa linha some das
     outras (`dcPessoasUsadas` em `dcFiltrarFuncsAtividade`); no bloco de nova execução, também quem já foi lançado
     naquela data (`tr[data-exec]`); duplicata forçada mostra aviso (`dcAvisoDuplicados`) antes de enviar.
  2. **Presença pelo Ponto (só CLT e diarista).** `GET ?funcionarios=1&data=` só traz CLT/diarista com ponto na data e
     sem falta no dia; Empreita/Produção como antes; sem `data` não filtra (agendamento usa `dcOptFuncSemData`).
     Servidor recusa no `create` (exceto PLANEJADA) e no `addexecucao`: "Fulano não tem presença registrada no ponto
     em dd/mm/aaaa…" (`DiarioCampoDAO.semPresencaNoPonto`). A nova execução carrega a lista pela data dela
     (`dcRecarregarFuncsExec`, ao abrir e ao mudar `#exData`). Dica fixa sob as duas tabelas. Linhas antigas não mudam.
     O Processar da Colheita usa o mesmo GET/POST, então segue a regra (inclusive o select de responsável dele).
  3. **Pergunta ao concluir Colheita** (`dcPerguntarIrColheita`, modal criado no JS): ao salvar Colheita como Concluído
     e ao Finalizar uma Colheita pelo detalhe. Sim → `colheitaestoque.jsp`; Não → fica com a lista atualizada. Não
     aparece no fluxo `abrirColheita=1` (que já volta sozinho).
  4. **`abrirColheita=1` só Colheita:** `dcTravarTipoColheita` seleciona e desabilita o tipo e esconde o "+" (também
     no agendamento); editar destrava. Payload `origem:"colheita"` → o servidor recusa outro tipo (400).
  Verificado no Chrome com pontos/falta de teste (04/11: carla CLT e clara diarista; 03/11: carla com falta; 05/11 sem
  ponto) e diários de teste 120/121 + lotes 21/22 — tudo apagado no fim.
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
- 2026-10-25 — Onda 2, preço da caixa vindo de Despesas/Custos (contrato V28): na Colheita, ao escolher o tipo,
  trocar o talhão (cultura) ou abrir um diário existente, `dcBuscarPrecoCaixa` faz
  `GET /DespesasCustosServlet?precoCaixa=1&categoria=CAIXA_COLHEITA&idAlimento=<cultura do talhão>&idArea=<área>&tipoCaixa=CONTENTOR`.
  Encontrou: o Preço/cx das linhas em Produção (cadastro e nova execução) vem com esse valor, que tem prioridade
  sobre o preço do cadastro do funcionário de Produção; o usuário pode editar (editou = não é mais sobrescrito,
  `data-auto="0"`). Não encontrou: aviso discreto `#dcAvisoPrecoCaixa` e, ao salvar (`create` e `addexecucao`),
  `DiarioCampoDAO.registrarPrecoCaixaSeAusente` registra, na mesma transação, o preço da **primeira** linha em regime
  Produção via `DespesasCustosDAO.registrarPrecoCaixa(c, …)`; linhas com outros preços não mudam o cadastro, e
  preço já cadastrado nunca é sobrescrito pelo Diário. `inserirCompleto`/`adicionarExecucao` passaram a dar rollback
  também em `RuntimeException` (o registro pode lançar `IllegalArgumentException`). Processamento: nada (onda 3).
- 2026-11-02: padronização visual do botão de novo registro (Gerente)
- 2026-11-02 — **Módulo Embalagem (V34), autorizado pelo Gerente:** novo método `DiarioCampoDAO.inserirInsumoBaixado(Connection c, int idDiario, int idInsumo, BigDecimal quantidade, String unidade, BigDecimal custoUnitario)`, usado só pelo Processar da Colheita, na transação dela: grava uma linha `diario_insumo` já baixada (`quantidade` = `quantidade_direta` = caixas, `custo_unitario` = preço médio no momento da baixa, `custo`, `baixado=true`) e chama `recalcularCustos(c, id)` (entra em `custo_insumos`/`custo_total`). A saída do estoque é feita por quem chama (`EstoqueMovimentoDAO.registrarSaida(Connection…)`).
  - **Finalizar:** a linha tem `baixado=true`, então o `finalizar` não baixa de novo (só recalcula o custo).
  - **Editar (`atualizarCompleto`):** linhas baixadas são preservadas (regra P1-7).
  - **Cancelar:** muda só o status; o estoque das embalagens não é estornado (igual às demais baixas). A Safra soma só diários CONCLUIDA, então um diário cancelado não entra no custo da Safra.
- 2026-11-02 — **`recalcularCustos`: linha já baixada mantém o custo da baixa** (aprovado pelo Gerente, decisão do usuário: o custo é o do momento da baixa, pelo preço médio de quando o insumo foi usado). Linhas `baixado=true` usam o `custo_unitario` gravado e só recalculam `custo = quantidade × custo_unitario`; linhas não baixadas continuam reprecificadas pelo preço médio corrente (inclusive no `finalizar`, que recalcula antes de baixar). `carregarInsBrutos` passou a ler também `custo_unitario`.
  - Motivo: o diário de Processamento nasce EM_ANDAMENTO com a linha da embalagem já baixada; sem isso, uma compra entre o Processar e o Finalizar mudaria o custo dela.
  - Efeito colateral (correção): `removerFuncionario`/`removerDespesa` e outros recálculos num diário CONCLUIDA reprecificavam os insumos já baixados pelo preço médio do dia; agora o custo de insumos de diário fechado não muda mais.
