# Módulo Embalagem

**Objetivo:** cadastro das embalagens (caixas) usadas no Processamento da colheita, por alimento + área de produção, com o **estoque** de cada uma. Cada embalagem é um **insumo da categoria EMBALAGEM** (unidade `CX`): as compras entram por Entrada (preço médio ponderado) e o Processar da Colheita dá baixa e lança o custo no diário de Processamento (custo de produção).
**Menu:** Configuração → Embalagens (perto de Estoque de Insumos)

## Escopo
| Camada | Arquivo |
|---|---|
| Controller | `Controller/ControllerEmbalagem.java`, URL `/ControllerEmbalagem` |
| DAO | `Model/Dao/EmbalagemDAO.java` (cria e mantém o insumo ligado, na mesma transação) |
| Model | `Model/Model/Embalagem.java` |
| View | `view/admin/embalagem.jsp` (lista + modais Nova/Editar, Entrada (compra), Movimentos) |
| JS | `main.js` → seção `MÓDULO EMBALAGEM` (funções `eb*`) + bloco `embalagem.jsp init` |
| Banco | `V31__embalagem.sql` (tabela, criada pela Colheita), `V34__embalagem_insumo.sql` |
| Núcleo (autorizado) | `WEB-INF/web.xml` (servlet + mapping), `pagina/menu.jsp` (item em Configuração) |

**GET:** lista `[?idAlimento=&idArea=&ativo=true|false]`, `?id=<idEmbalagem>`.
**POST (JSON, `acao`):** `salvar` `{idEmbalagem?, codigo?, descricao, pesoKg, idAlimento, idArea, precoEmbalador, estoqueMinimo?, idFornecedor?, ativo?}` → `{ok, msg, target, embalagem}`; `ativar` `{idEmbalagem, ativo}`. Regra violada ⇒ 400 com a mensagem; erro técnico ⇒ 500 com o código do `LogUtil`.
**Entrada (compra) e Movimentos** usam o Estoque de Insumos: `POST /ControllerEstoque {acao:"entrada", idInsumo:<embalagem.idInsumo>, quantidade, precoUnitario, idParceiro (tipo Insumo, obrigatório), data, observacao}` e `GET /ControllerEstoque?acao=movimentos&idinsumo=`.

## Banco (contrato)
`embalagem` (V31 + V34):
| Coluna | Regra |
|---|---|
| `idembalagem` | PK |
| `codigo` | `^[A-Z0-9_]{1,30}$`, único por (alimento, área). Gerado da descrição se vazio (`EmbalagemDAO.normalizarCodigo`: sem acento, maiúsculas, qualquer caractere fora de `[A-Z0-9]` vira `_`, `_` repetidos viram um, sem `_` nas pontas, até 30). Ex.: "Papelao caixa 6,5" → `PAPELAO_CAIXA_6_5`; "PAPELAO_6.5" → `PAPELAO_6_5`. |
| `descricao` | até 80, obrigatória |
| `peso_kg` | > 0 |
| `id_alimento`, `id_area_producao` | FK, obrigatórios |
| `preco_embalador` | > 0. Pago por caixa no Processar (diário de Processamento, regime PRODUCAO) |
| `ativo` | sem exclusão: desativar também desativa o insumo |
| `id_insumo` | **V34**, FK `insumo`, UNIQUE. Saldo, preço médio, mínimo e fornecedor ficam no insumo. NULL só em embalagem anterior à V34 (a primeira edição cria o insumo). |
| `preco_compra` | **OBSOLETO desde a V34** (NULL, não é mais gravado). O preço de compra é `insumo.preco_medio`. Remover quando nenhum leitor restar. |

Insumo ligado: `categoria='EMBALAGEM'`, `grandeza='UNIDADE'`, `unidade='CX'`, `nome` = "descrição (ALIMENTO · SIGLA)" (até 80), `estoque_minimo`, `id_fornecedor`, `situacao` = `ativo`. `quantidade_disponivel` e `preco_medio` mudam só pelas movimentações (`EstoqueMovimentoDAO`).

## Como os outros módulos usam
- **Colheita (Processar):** `GET /ControllerColheita?embalagens=1&ativo=true` (colunas da grade, com `saldo`). Ao confirmar, `ColheitaDAO.registrarProcessamentoComDiario` faz, na mesma transação da baixa do lote: SAÍDA do estoque de cada embalagem (soma das caixas da grade; saldo insuficiente ⇒ recusa com "faltam N cx") e `DiarioCampoDAO.inserirInsumoBaixado` (linha `diario_insumo` com quantidade = caixas, `custo_unitario` = preço médio no momento, `baixado=true`). O `validarprocessamento` recusa caixas acima do saldo antes de criar o diário.
- **Estoque de Insumos:** as embalagens aparecem com a categoria Embalagem (filtro e rótulo). Entradas/saídas de lá valem igual. Criar insumo EMBALAGEM ou trocar a categoria por lá é recusado (`ControllerInsumo`): o cadastro é aqui.
- **Romaneio:** lê `embalagem` (código, descrição, peso) e `GET /ControllerColheita?caixasembaladas=<idLote>`; `precoCompra` desse JSON agora é o preço médio atual do insumo (só informativo). A linha automática "Custo de embalagem" saiu do Romaneio.
- **Diário de Campo:** os selects de insumo do Diário filtram DEFENSIVO/ADUBO, então embalagens não aparecem para lançar à mão.

## Dependências
- `Model/Model/CategoriaInsumo` (EMBALAGEM), `UnidadeMedida` (CX), `EstoqueMovimentoDAO`, `ControllerEstoque`, `ControllerInsumo?acao=fornecedores` (fornecedores tipo Insumo), `ControllerAlimento`, `ControllerAreaProducao`.

## Pendências
- [ ] Custo no diário de Processamento x preço médio: o `DiarioCampoDAO.recalcularCustos` reprecifica todas as linhas de insumo pelo preço médio corrente, inclusive as `baixado=true`. Se houver compra nova entre o Processar e o Finalizar do diário, o custo da embalagem no diário muda (a baixa foi a outro preço). Proposta enviada ao Gerente: congelar `custo_unitario` das linhas baixadas (mudança no Diário).
- [ ] Cancelar o diário de Processamento não estorna o estoque das embalagens (nem a baixa do lote). Mesma regra das demais baixas; definir estorno explícito se o usuário quiser.
- [ ] Remover a coluna `embalagem.preco_compra` quando confirmado que ninguém mais a lê.

## Histórico
- 2026-11-02 — **Criação do módulo** (decisões do usuário de 02/11: embalagem é insumo com estoque; custo = preço médio ponderado; custo de produção no Processamento; baixa ao confirmar o Processar; sem saldo, o Processar é bloqueado). V34 (`id_insumo`, `preco_compra` obsoleto), `ControllerEmbalagem`, `embalagem.jsp`, item no menu Configuração. O cadastro saiu da página Colheita e Estoque (botão e modal removidos; o "+ Embalagem" do Processar só inclui embalagem já cadastrada e mostra o link para esta página).
  - Correção do erro reportado ("Tipo de caixa inválido" com "6,5"/"6.5"): normalização própria (`normalizarCodigo`), mensagem "Código da embalagem…", validação dos campos antes do código. A normalização da Despesas (`normalizarTipoCaixa`) não mudou. O campo "preço de compra" deixou de existir no cadastro (vem das entradas).
