# Módulo Colheita e Estoque

**Objetivo:** lotes de colheita gerados a partir do Diário de Campo, com saldo em estoque, baixa parcial/total (`processar`), encerramento (`finalizar`) e indicadores por status.
O **Processar** abre um modal que registra a etapa de Processamento: grade de embaladores × embalagens (pagos por caixa), demais funcionários por horas e um diário de Processamento no talhão do lote.
Desde a onda A do pacote Romaneio (02/11), o módulo também tem o **cadastro de embalagens** (V31), por alimento e área, com o preço pago ao embalador e o preço de compra.
**Menu:** Produção → Colheita e Estoque (botão "Embalagens" no topo da página).

## Escopo
| Camada | Arquivo |
|---|---|
| Controller | `Controller/ControllerColheita.java` — URL `/ControllerColheita` (lotes, processamento e embalagens) |
| DAO | `Model/Dao/ColheitaDAO.java`, `Model/Dao/EmbalagemDAO.java` |
| Model | `ColheitaLote.java`, `Embalagem.java` |
| View | `view/admin/colheitaestoque.jsp` (inclui `#modalProcessar` e `#modalEmbalagens`) |
| JS | `main.js` → seção `MÓDULO COLHEITA E ESTOQUE` (funções `ce*` da lista, `cp*` do modal Processar, `em*` do cadastro de embalagens) |

**Ações (POST JSON):** `processar` (legado: só quantidade, sem diário; a tela não usa mais), `validarprocessamento`, `processarcomdiario`, `finalizar`, `salvarembalagem`, `ativarembalagem`.
**GET:** lista (`?status=`), `?indicadores=1`, `?processamentos=<idLote>`, `?caixasembaladas=<idLote>`, `?embalagens=1[&idAlimento=&idArea=&ativo=true|false]`.

## Banco
- `colheita_lote` (V24 — regime de produção); lê `diario_campo`, `areaproducao`, `quadra`, `alimento`.
- **V30** (`V30__colheita_processamento.sql`):
  - `colheita_processamento` (idprocessamento, idlote → `colheita_lote`, iddiario → `diario_campo` ON DELETE SET NULL, data_processamento, quantidade > 0, total_caixas, valor_embalagem). Índice único parcial em `iddiario`: um diário de Processamento corresponde a um único processamento.
  - `colheita_processamento_embalagem` (idprocessamento, idpessoa, iddespesascusto, **idembalagem** (V31), tipo_caixa, quantidade_caixas, preco_caixa, valor): uma linha por embalador × embalagem, com o preço do embalador gravado no momento (snapshot).
  - `idpessoa` **sem FK**: os funcionários ficam nas tabelas filhas (herança `pessoa → funcionario → funcionarioproducao/...`), e uma FK para `pessoa` não enxerga essas linhas.
  - `iddespesascusto` só existe nos registros da onda 3 (categoria EMBALAGEM de Despesas). Desde a V31 fica NULL; o Processar não usa mais Despesas.
- **V31** (`V31__embalagem.sql`): ver o contrato abaixo. Acrescenta `colheita_processamento_embalagem.idembalagem` (FK, NULL nos registros antigos). Não houve migração de dados (não havia EMBALAGEM em `despesascusto`).

### Contrato V31 — tabela `embalagem` (lida pelo Romaneio)
| Coluna | Regra |
|---|---|
| `idembalagem` | SERIAL PK |
| `codigo` | `[A-Z0-9_]{1,30}`, gerado da descrição por `DespesasCustosDAO.normalizarTipoCaixa` quando vazio ("Papelão 7kg" → `PAPELAO_7KG`) |
| `descricao` | obrigatória, até 80 caracteres (rótulo exibido) |
| `peso_kg` | NUMERIC(8,3) > 0, obrigatório |
| `id_alimento` → `alimento(idproduto)`, `id_area_producao` → `areaproducao(idareaproducao)` | obrigatórios |
| `preco_compra` | > 0. Custo da embalagem para a empresa (**Romaneio**) |
| `preco_embalador` | > 0. Valor pago por caixa ao embalador (**Processar** → diário PRODUCAO) |
| `ativo` | sem exclusão; desativar tira a embalagem da grade do Processar |
| `criado_em`, `atualizado_em` | |
- UNIQUE `uk_embalagem (codigo, id_alimento, id_area_producao)`.
- DAO `EmbalagemDAO`: `listar([Connection,] idAlimento?, idArea?, ativo?)`, `buscar(Connection, id)`, `salvar([Connection,] Embalagem)` (insere ou atualiza e devolve o registro), `definirAtivo(id, ativo)`. Regras ⇒ `IllegalArgumentException` com mensagem para o usuário.
- JSON (Gson, bean `Embalagem`): `{idEmbalagem, codigo, descricao, pesoKg, idAlimento, nomeAlimento, idArea, nomeArea, siglaArea, precoCompra, precoEmbalador, ativo}`.
- `POST acao=salvarembalagem {idEmbalagem? (0/ausente = novo), codigo?, descricao, pesoKg, idAlimento, idArea, precoCompra, precoEmbalador, ativo?, target?}` → `{ok, msg, target, embalagem}`. Erros: HTTP 400 `{ok:false, msg}` (ex.: "Já existe a embalagem PAPELAO_7KG para este alimento e área.").
- `POST acao=ativarembalagem {idEmbalagem, ativo}` → `{ok, msg}`.

### Caixas embaladas por lote e por tipo (para o Romaneio)
- `GET /ControllerColheita?caixasembaladas=<idLote>` (`ColheitaDAO.caixasEmbaladas`) devolve as caixas somadas de todos os processamentos do lote, agrupadas por embalagem:
  `[{idEmbalagem, codigo, descricao, pesoKg, precoCompra, caixas, valorEmbalador}]`, ordenado por peso. Registros anteriores à V31 saem agrupados pelo código, com `idEmbalagem`/`pesoKg`/`precoCompra` nulos.
- SQL equivalente: `SUM(pe.quantidade_caixas)` de `colheita_processamento p JOIN colheita_processamento_embalagem pe ON pe.idprocessamento=p.idprocessamento LEFT JOIN embalagem e ON e.idembalagem=pe.idembalagem WHERE p.idlote=?`, agrupado por `pe.idembalagem`.
- Total de caixas embaladas de **um** processamento: `colheita_processamento.total_caixas` (= soma da grade). Por processamento e tipo: `colheita_processamento_embalagem` filtrado por `idprocessamento`.
- Atenção: os processamentos contam mesmo que alguém cancele depois o diário de Processamento no Diário de Campo (a baixa do lote também não é desfeita). Ver Pendências.

## Modal "Processar" — fluxo
1. **Abrir** (`ceProcessar(idLote)`): carrega `ControllerDiarioCampo?tiposatividade=1` (procura "Processamento" pelo nome), `ControllerColheita?embalagens=1&ativo=true&idAlimento=&idArea=` (do lote) e `ControllerDiarioCampo?funcionarios=1&data=`. A lista de funcionários é recarregada quando a data muda.
   Embaladores = `tipo PRODUCAO` + `cargo` Embalador. Outros = CLT/DIARISTA/EMPREITA.
2. **Grade de embaladores:** cada linha é um embalador (sem repetir) e cada coluna é uma embalagem ativa do alimento e da área do lote, com descrição, peso e preço do embalador. A célula recebe o nº de caixas.
   - Os totais são calculados ao vivo: por linha (caixas e R$), por coluna (rodapé "Total por embalagem") e geral.
   - **+ Embalador** acrescenta uma linha. **+ Embalagem** abre um painel para incluir uma embalagem cadastrada que esteja fora da grade, ou para cadastrar uma nova ali mesmo (descrição, peso, preço de compra, preço do embalador). O cadastro novo usa `salvarembalagem` e a coluna entra na grade sem perder o que já foi digitado.
   - O preço não é editável na grade: muda-se no cadastro.
3. **Outros funcionários (horas):** colunas Funcionário | Tipo | Regime | Preço | Caixas | Horas | Valor. O valor usa `dcValorFuncionario` (a mesma prévia do Diário). O responsável do diário é escolhido entre os funcionários lançados.
4. **Confirmar**:
   1. `POST {acao:"validarprocessamento", idlote, data, quantidade, embalagens[{idPessoa, idEmbalagem, caixas, preco}]}` só confere, sem gravar: lote aberto, data ≥ colheita, quantidade ≤ saldo, **safra** do talhão na data, e em cada célula embalagem ativa, do alimento e da área do lote, sem par embalador×embalagem repetido e com `preco` igual ao `preco_embalador` atual. Se o preço mudou com o modal aberto, recusa com "O preço do embalador da embalagem X mudou para R$ Y. Feche e abra o modal de novo.".
   2. `POST /ControllerDiarioCampo {acao:"create", … idTipoAtividade:<Processamento>, status:"EM_ANDAMENTO", funcionarios:[...]}`: **uma linha PRODUCAO por célula preenchida** (`quantidadeCaixas`, `precoPorCaixa` = preço do embalador, `funcaoExercida` "Embalador — <descrição>") e as linhas REGIME_SISTEMA por horas. Um erro aqui aparece no modal como "O Diário de Campo recusou o lançamento: …".
   3. `POST {acao:"processarcomdiario", idlote, iddiario, data, quantidade, embalagens[...]}` faz **uma transação** com: `SELECT … FOR UPDATE` do lote, a mesma validação, a verificação do diário (existe, é Processamento, é do talhão do lote e ainda não tem lote), a baixa do lote e os INSERTs de `colheita_processamento` (`total_caixas` = soma da grade, `valor_embalagem` = soma caixas × preço do embalador) e de `colheita_processamento_embalagem` (idembalagem, `tipo_caixa` = código, caixas, preço do embalador). A resposta é `{ok, msg, idprocessamento}`.
   4. Se o passo 3 falhar, o JS manda `POST /ControllerDiarioCampo {acao:"status", iddiario, status:"CANCELADA"}` (o Diário não tem exclusão) e avisa no modal.
- A transação do passo 3 é manual (commit/rollback com `catch (SQLException | RuntimeException)`), porque o `TransacaoUtil` só faz rollback de `SQLException`.

## Dependências
- **Diário de Campo**: a atividade "Colheita" alimenta os lotes (`DiarioCampoDAO` também lê `colheita_lote`).
  O modal Processar **consome** do Diário: `GET ?tiposatividade=1`, `GET ?funcionarios=1&data=`, `POST acao=create` (Processamento), `POST acao=status` (CANCELADA, como compensação) e a função JS `dcValorFuncionario`. Se o Diário mudar algum desses formatos, o modal quebra.
- **Perfil Produção** (lê `colheita_processamento_embalagem`): mostra o tipo de embalagem por linha do diário, casando preço + caixas da linha com o detalhe. Conferido com a grade em 02/11: cada célula vira uma linha do perfil com o tipo certo. Rótulo hoje vem do código formatado ("Caixa Papelão 7kg"), porque o perfil procura o nome em Despesas (`iddespesascusto`, NULL desde a V31). Renomear/remover `tipo_caixa`, `quantidade_caixas`, `preco_caixa`, `idpessoa` quebra o perfil.
- **Romaneio** (onda B): lê a tabela `embalagem` (`preco_compra`, `peso_kg`) e as caixas embaladas por lote (`GET /ControllerColheita?caixasembaladas=<idLote>`, contrato acima).
  A Colheita **depende** do Romaneio no fluxo "Finalizar → Romaneio" (onda C): depois de finalizar um lote com caixas embaladas > 0, pergunta "Lote finalizado. Seguir para o cadastro do romaneio?" e redireciona para `view/admin/cadastroRomaneio.jsp?lote=<idLote>` (URL é contrato fixo; se o Romaneio mudar a URL ou o parâmetro `lote`, o botão quebra).
- **Despesas e Custos**: o Processar não usa mais a categoria EMBALAGEM (V28). Usa só `DespesasCustosDAO.normalizarTipoCaixa` (static) para gerar o código da embalagem.
- **Safra**: `SafraDAO.resolverSafra` (só leitura) na validação.
- **Área de Produção** (`GET /ControllerAreaProducao`), **Alimento** (`GET /ControllerAlimento`): selects do cadastro de embalagens.
- A lista de lotes (GET) tem `idArea` e `areaNome` (desde a onda 3).

## Pendências
- [ ] **Onda C — reteste final:** o redirecionamento "Finalizar → Romaneio" está implementado (02/11), mas `cadastroRomaneio.jsp` ainda não existia no teste (404). Retestar quando a onda B do Romaneio estiver no ar: a página deve abrir com o lote já escolhido.
- [ ] **Perfil Produção** (levar à conversa do módulo): (1) mostrar `embalagem.descricao` (LEFT JOIN por `idembalagem`) em vez do código formatado; (2) com a grade, um embalador pode ter duas embalagens com o mesmo preço e o mesmo nº de caixas no mesmo diário, e aí o perfil mostra "—" (ambíguo), porque `diario_funcionario` não guarda a embalagem. Uma solução seria o Diário guardar `idembalagem` na linha, o que é mudança no Diário.
- [ ] **Atomicidade total** (baixa + diário numa única transação de banco) depende do Diário. Hoje são duas transações com compensação. Para ter uma só, o Diário precisaria expor `inserirCompleto(Connection c, DiarioCampo d)` e as validações do `handleSalvar`/`validarRegimeCaixa` num método reutilizável.
- [ ] Cancelar depois, no Diário, um diário de Processamento já ligado a um lote não desfaz a baixa nem as caixas embaladas (o Romaneio vai contar essas caixas). Definir a regra (bloquear o cancelamento no Diário ou estornar na Colheita).
- [ ] Mensagem de preço alterado mostra o valor com ponto ("R$ 0.95"). Trocar para vírgula no próximo build (`ColheitaDAO.validar`).
- [ ] Cards de indicadores: "Em Estoque/Processado/Finalizado" somam `quantidade_total` **por status do lote**, e não o saldo nem o processado. Comportamento anterior à onda 3; ajustar se o usuário quiser.
- [ ] Ação legada `processar` (sem diário) continua no Controller por compatibilidade. Remover quando ninguém mais a usar.
- [ ] Os dados de teste da onda 3 (pessoa 68, diários 93/94/95, preços em Despesas) foram apagados na limpeza do banco de 25/10 (informado pelo Gerente).

## Histórico
- 2026-10-25 — **Onda 3**: modal Processar (cabeçalho do lote, data, quantidade com barra de saldo, embaladores com embalagem/preço/caixas, outros por horas, responsável); diário de Processamento + baixa + vínculo (V30); preço da embalagem gravado em Despesas (EMBALAGEM). 3.3 não se aplica à Colheita (nenhuma tela mostra valor de caixa colhida). 3.4: layout Tipo | Regime | Preço | Caixas | Horas | Valor no modal. 3.5: Cadastrar Colheita → Sitio Silas → salvar volta para `colheitaestoque.jsp`. A lista passou a escapar textos do banco (`ceEsc`). Verificado no navegador (safra fora do período, falha no passo 3 com rollback e cancelamento do diário, fluxo completo).
- 2026-11-02 — **Pacote Romaneio, onda A** (decisões do usuário de 02/11: preço do embalador ≠ preço de compra; um embalador embala vários tipos no mesmo dia):
  - **V31**: tabela `embalagem` (contrato acima) + `colheita_processamento_embalagem.idembalagem`. A V31 foi aplicada pelo SchemaMigrator; a V32 da Despesas já estava na base.
  - **Cadastro de embalagens**: botão "Embalagens" → `#modalEmbalagens` com formulário (alimento, área, descrição, código opcional com sugestão ao vivo, peso, preço de compra, preço do embalador), lista filtrável (alimento, área, ativas/desativadas/todas), editar e ativar/desativar. `EmbalagemDAO` + ações `salvarembalagem`/`ativarembalagem` + GET `embalagens`.
  - **Modal Processar** redesenhado: grade embalador × embalagem, + Embalador, + Embalagem (inclusive cadastrar ali), totais ao vivo por linha, por coluna e gerais; outros funcionários por horas como antes. O Processar deixou de gravar preços em Despesas.
  - **Romaneio**: `GET ?caixasembaladas=<idLote>`.
  - Corrigido o título branco sobre branco dos cards do modal (`text-dark`; o CSS global pinta o texto de `.card-header`).
  - Verificado no Chrome (aba própria; dados de teste criados e **apagados**: embaladores 70/71 por SQL sem login, embalagens 1–4, diários 101/102, lote 11, processamento 6):
    - cadastro: criar ("Papelão 7kg" → `PAPELAO_7KG`), duplicado recusado, peso vazio recusado, editar preço, desativar (some da grade do Processar);
    - grade: 2 embaladores × 3 embalagens, sendo uma cadastrada pelo próprio modal sem perder o que já fora digitado. Linhas R$ 24,00 e R$ 15,00, rodapé 50/10/5 cx, diarista 2 h R$ 37,50, total R$ 76,50;
    - preço alterado com o modal aberto: recusado na validação, sem criar diário;
    - confirmação: diário 2026-000102 com 4 linhas PRODUCAO (uma por célula) + 1 por horas, custo R$ 76,50; lote 0 → 60; detalhe com idembalagem e snapshot do preço; `caixasembaladas` correto;
    - Perfil Produção: cada célula aparece como linha com o tipo certo ("Caixa Papelão 7kg", "Caixa Plástico 20kg", "Caixa Madeira 12kg").
- 2026-11-02 — **Pacote Romaneio, onda C** (só JS, `main.js` copiado para o target, sem build): `ceFinalizar` → se ok, `ceOferecerRomaneio(idLote)` soma `caixasembaladas`; com > 0 abre `#modalIrRomaneio` (criado no JS) "Lote finalizado. Seguir para o cadastro do romaneio?" com **Ficar aqui** / **Ir para o Romaneio** → `AGRO_CTX + '/view/admin/cadastroRomaneio.jsp?lote=' + idLote`. Sem caixas, só finaliza como antes.
  - Verificado no Chrome (aba própria; lotes de teste 12/13, diários `TESTE-ONDAC-*` e embalagem `TESTE_ONDAC` criados por SQL e **apagados**): lote 13 sem caixas → finaliza sem pergunta; lote 12 com 40 caixas → pergunta com "40 caixa(s) embalada(s)", "Ficar aqui" fecha e mantém a página, "Ir para o Romaneio" vai para `cadastroRomaneio.jsp?lote=12` (404 — a página da onda B ainda não existia; reteste pendente).
- 2026-11-02 — **Correção do teste integrado** (só JS): o select "Responsável pelo diário" (`#cpResponsavel`) do Processar listava só quem estava na grade. Agora `cpAtualizarResponsavel` lista todos os funcionários ativos na data (`cpFuncs`, de `GET /ControllerDiarioCampo?funcionarios=1&data=`) + os lançados no modal, ordenados por nome; mantém a escolha atual e sugere o primeiro lançado. Recarrega ao mudar a data (`cpAoMudarData`) e ao abrir o modal. O Diário não exige que o responsável esteja nas linhas (só obrigatório). Verificado no Chrome com lote de teste 17 (criado/apagado por SQL): opções = lista da API em duas datas; embalador na grade (simulado no JS) aparece e continua escolhível; escolha preservada.
- 2026-11-02: padronização visual do botão de novo registro (Gerente)
