# Módulo Colheita e Estoque

**Objetivo:** lotes de colheita gerados a partir do Diário de Campo, com saldo em estoque, baixa parcial/total (`processar`), encerramento (`finalizar`) e indicadores por status.
Desde a onda 3, o **Processar** abre um modal que registra a etapa de Processamento: embaladores por caixa, demais funcionários por horas, preço de cada embalagem em Despesas e Custos e um diário de Processamento no talhão do lote.
**Menu:** Produção → Colheita e Estoque

## Escopo
| Camada | Arquivo |
|---|---|
| Controller | `Controller/ControllerColheita.java` — URL `/ControllerColheita` |
| DAO | `Model/Dao/ColheitaDAO.java` |
| Model | `ColheitaLote.java` |
| View | `view/admin/colheitaestoque.jsp` (inclui o `#modalProcessar`) |
| JS | `main.js` → seção `MÓDULO COLHEITA E ESTOQUE` (funções `ce*` da lista e `cp*` do modal Processar) |

**Ações (POST JSON):** `processar` (legado: só quantidade, sem diário; a tela não usa mais), `validarprocessamento`, `processarcomdiario`, `finalizar`.
**GET:** lista (`?status=`), `?indicadores=1`, `?processamentos=<idLote>`.

## Banco
- `colheita_lote` (V24 — regime de produção); lê `diario_campo`, `areaproducao`, `quadra`, `alimento`.
- **V30** (`V30__colheita_processamento.sql`):
  - `colheita_processamento` (idprocessamento, idlote → `colheita_lote`, iddiario → `diario_campo` ON DELETE SET NULL, data_processamento, quantidade > 0, total_caixas, valor_embalagem). Índice único parcial em `iddiario`: um diário de Processamento corresponde a um único processamento.
  - `colheita_processamento_embalagem` (idprocessamento, idpessoa, iddespesascusto → `despesascusto`, tipo_caixa, quantidade_caixas, preco_caixa, valor). Guarda uma linha por embalador e tipo de embalagem, com o preço gravado no momento.
  - `idpessoa` **sem FK**: os funcionários ficam nas tabelas filhas (herança `pessoa → funcionario → funcionarioproducao/...`), e uma FK para `pessoa` não enxerga essas linhas. A primeira versão da V30 tinha essa FK. Ela foi aplicada só na base local em 25/10 e a constraint foi removida à mão; o arquivo já está corrigido.

## Modal "Processar" — fluxo
1. **Abrir** (`ceProcessar(idLote)`): carrega `ControllerDiarioCampo?tiposatividade=1` (procura "Processamento" pelo nome), `DespesasCustosServlet?embalagens=1&idAlimento=&idArea=` e `ControllerDiarioCampo?funcionarios=1&data=`. A lista de funcionários é recarregada quando a data muda.
   Embaladores = `tipo PRODUCAO` + `cargo` Embalador. Outros = CLT/DIARISTA/EMPREITA.
2. **Tabela** com as colunas Funcionário | Tipo | Regime | Embalagem | Preço | Caixas | Horas | Valor, e os totais calculados ao vivo.
   A embalagem preenche o preço cadastrado. Também aceita "+ Nova embalagem…" (nome livre, código gerado igual a `normalizarTipoCaixa`).
   O preço pode ser editado; se ficar diferente do cadastro, aparece o aviso "novo preço", e o novo valor é replicado nas linhas com a mesma embalagem.
   O valor por horas usa `dcValorFuncionario` (a mesma prévia do Diário).
   O responsável do diário é escolhido entre os funcionários lançados.
3. **Confirmar**:
   1. `POST /ControllerColheita {acao:"validarprocessamento", idlote, data, quantidade, embalagens[]}` só confere, sem gravar: lote aberto, data ≥ colheita, quantidade ≤ saldo, embalagens coerentes e **safra** do talhão na data. Mensagem: "Não há Safra que inclua o talhão X e cubra a data dd/mm/aaaa…".
   2. `POST /ControllerDiarioCampo {acao:"create", … idTipoAtividade:<Processamento>, status:"EM_ANDAMENTO", funcionarios:[PRODUCAO caixas×preço | REGIME_SISTEMA horas]}`, conforme o contrato da ficha do Diário. Um erro aqui aparece no modal como "O Diário de Campo recusou o lançamento: …".
   3. `POST /ControllerColheita {acao:"processarcomdiario", idlote, iddiario, data, quantidade, embalagens[{idPessoa, tipoCaixa, descricao?, caixas, preco}]}` faz **uma transação** com: `SELECT … FOR UPDATE` do lote, a mesma validação do passo 1, a verificação do diário (existe, é Processamento, é do talhão do lote e ainda não tem lote), `DespesasCustosDAO.registrarPrecoCaixa(c, "EMBALAGEM", alimento, área, tipo, descricao, preco)` por tipo (upsert), a baixa do lote e os INSERTs da V30. A resposta é `{ok, msg, idprocessamento}`.
   4. Se o passo 3 falhar, o JS manda `POST /ControllerDiarioCampo {acao:"status", iddiario, status:"CANCELADA"}` (o Diário não tem exclusão) e avisa no modal que o diário foi cancelado.
- A transação do passo 3 é manual (commit/rollback com `catch (SQLException | RuntimeException)`), porque o `TransacaoUtil` só faz rollback de `SQLException` e as regras lançam `IllegalArgumentException` no meio das gravações.

## Dependências
- **Diário de Campo**: a atividade "Colheita" alimenta os lotes (`DiarioCampoDAO` também lê `colheita_lote`).
  O modal Processar **consome** do Diário: `GET ?tiposatividade=1`, `GET ?funcionarios=1&data=`, `POST acao=create` (Processamento), `POST acao=status` (CANCELADA, como compensação) e a função JS `dcValorFuncionario`.
  Se o Diário mudar algum desses formatos, o modal quebra.
- **Despesas e Custos** (contrato V28): `GET ?embalagens=1`, `DespesasCustosDAO.registrarPrecoCaixa(Connection, …)` e `normalizarTipoCaixa`. Os preços de `EMBALAGEM` gravados pelo modal aparecem na tela de Despesas.
- **Safra**: `SafraDAO.resolverSafra` (só leitura) na validação.
- **Área de Produção**, **Alimento**.
- A lista de lotes (GET) **ganhou** `idArea` e `areaNome`; nenhum campo foi removido.

## Pendências
- [ ] **Atomicidade total** (baixa + diário + preços numa única transação de banco) depende do Diário. Hoje são duas transações com compensação (o diário é cancelado se a baixa falhar).
  Para ter uma transação só, o Diário precisaria expor `inserirCompleto(Connection c, DiarioCampo d)`, que hoje abre conexão própria, e as validações do `handleSalvar`/`validarRegimeCaixa` num método reutilizável. Isso fica para a conversa do Diário; a Colheita passaria a chamar o DAO dentro da própria transação.
- [ ] **Pagamento do embalador** (decisão em aberto com o usuário): o modal usa como preço/caixa do embalador o **preço da embalagem** cadastrado em Despesas (`EMBALAGEM`), conforme o pedido da onda 3. Se a decisão separar "custo do material da embalagem" de "valor pago por caixa ao embalador", o preço enviado ao Diário (`precoPorCaixa`) muda aqui.
- [ ] Cards de indicadores: "Em Estoque/Processado/Finalizado" somam `quantidade_total` **por status do lote**, e não o saldo nem o processado. Exemplo: um lote em ESTOQUE com 160 de 200 já processados conta 200 em "Em Estoque" e 0 em "Processado". Comportamento anterior à onda 3; ajustar se o usuário quiser.
- [ ] Ação legada `processar` (sem diário) continua no Controller por compatibilidade. Remover quando ninguém mais a usar.
- [ ] Módulo ainda não commitado por inteiro (controller, DAO, model, JSP, V24, V30). O commit automático do repositório pega as mudanças.
- [ ] Dados de teste na base local (onda 3):
  - funcionário de Produção **"TESTE Embalador Onda3"** (idpessoa 68, matrícula `TESTE-EMB-ONDA3`, sem usuário/senha), **desativado** (`situacaopessoa=false`). Foi criado por SQL e não pela tela, porque o cadastro pela tela exige criar login com senha;
  - processamento 2 do lote 7 (60 CX, diário de Processamento **2026-000094**, EM_ANDAMENTO, R$ 203,00);
  - diário 2026-000093 (Processamento, CANCELADA: teste da compensação);
  - diário 2026-000095 (Colheita, CANCELADA: teste do redirecionamento);
  - embalagens: PAPELAO_10 passou de R$ 2,10 para R$ 2,30 e foi criada PAPELAO_7KG (id 16, R$ 1,80), ambas para alimento 1 / área 2.

  Podem ser apagados ou mantidos.

## Histórico
- 2026-10-25 — **Onda 3**:
  - **3.1** O botão Processar abre o `#modalProcessar` (Bootstrap 5, cards, linhas dinâmicas, totais ao vivo): cabeçalho do lote (talhão, alimento, área, saldo), data, quantidade com barra de saldo restante e "Tudo", embaladores com embalagem/preço/caixas, outros funcionários por horas e responsável.
  - **3.2** Ao confirmar, é criado o diário de Processamento e são gravados a baixa, os preços de embalagem e o vínculo (V30). Ver o fluxo acima.
  - **3.3** Não se aplica à Colheita: nenhuma tela do módulo mostra valor ou custo da caixa colhida. A lista mostra só quantidades. O preço da caixa de colheita (CAIXA_COLHEITA/CONTENTOR) fica no Diário (onda 2).
  - **3.4** O layout Tipo | Regime | Preço | Caixas | Horas | Valor foi usado na tabela de funcionários do modal, com a coluna Embalagem a mais. A lista de lotes não segue esse layout porque é uma linha por lote, não por funcionário.
  - **3.5** Confirmado no navegador: Cadastrar Colheita → Sitio Silas → Salvar Diário volta para `colheitaestoque.jsp`. No Diário, o Embalador aparece oculto/desabilitado na Colheita.
  - Também: a lista passou a escapar textos do banco (`ceEsc`), e o botão Processar passa só o id.
  - Verificado no navegador:
    - safra fora do período mostra a mensagem no modal e não grava nada;
    - falha no passo 3 (FK da V30) desfez a transação (lote, preços e vínculo intactos) e cancelou o diário 2026-000093;
    - fluxo completo: lote 7 foi de 100 para 160 processados; diário 94 com 2 linhas PRODUCAO (40×2,30 e 20×1,80) e diarista 4 h = R$ 75, custo R$ 203 igual à prévia; PAPELAO_10 atualizado e PAPELAO_7KG criado em Despesas.
