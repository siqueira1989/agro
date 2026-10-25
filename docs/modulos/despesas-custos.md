# Módulo Despesas e Custos

**Objetivo:** cadastro das despesas/custos (fixos e variáveis) usados nos lançamentos do Diário de Campo e no custo da safra.
Desde a V28 também guarda o **preço da caixa de colheita e das embalagens, por alimento e por área de produção**.
**Menu:** Configuração → Custos e Despesas

## Escopo
| Camada | Arquivo |
|---|---|
| Controller | `Controller/DespesasCustosServlet.java` — URL `/DespesasCustosServlet` |
| DAO | `Model/Dao/DespesasCustosDAO.java` |
| Model | `Model/Model/DespesasCustos.java` (`LancamentoDespesasCustos.java` existe, mas nenhum controller usa); lê `Model/Model/TipoCaixa.java` (sugestões de código) |
| View | `view/admin/despesascustos.jsp` |
| JS | `main.js` → seção `MÓDULO DESPESAS E CUSTOS` + bloco `despesascustos.jsp` do `$(document).ready` |

**Ações:** `create`, `update`, `delete`, `registrarPrecoCaixa`. GET = lista; GET `precoCaixa`, `embalagens`, `tiposCaixa`.

## Banco
- `despesascusto`: coluna `classificacao` FIXO/VARIAVEL (V23, CHECK `ck_despesascusto_classificacao`).
- **V28** (`V28__despesascusto_preco_caixa.sql`):
  - `categoria varchar(20) NOT NULL DEFAULT 'GERAL'` — CHECK `ck_despesascusto_categoria` em `GERAL | CAIXA_COLHEITA | EMBALAGEM`. Registros antigos = `GERAL`.
  - `id_alimento` → `alimento(idproduto)`, `id_area_producao` → `areaproducao(idareaproducao)`, `tipo_caixa varchar(30)` — todos NULL em `GERAL`.
  - CHECK `ck_despesascusto_caixa_completa`: categoria ≠ GERAL ⇒ os três obrigatórios (tipo não vazio).
  - Índice único parcial `ux_despesascusto_preco_caixa (categoria, id_alimento, id_area_producao, tipo_caixa) WHERE categoria <> 'GERAL'` — é a chave do upsert.
  - `tipo_caixa`: `CONTENTOR` para `CAIXA_COLHEITA`; para `EMBALAGEM`, código livre `[A-Z0-9_]{1,30}`. Sugestões = enum `TipoCaixa` (PAPELAO_7, PAPELAO_10, PLASTICO_7, PLASTICO_25, MADEIRA_7, MADEIRA_15).
  - Caixas/embalagens gravadas pelo `registrarPrecoCaixa` saem com unidade `un`, classificação `VARIAVEL`, tipo "Caixa colheita"/"Embalagem".

## Dependências
- **Diário de Campo**: `diario_despesa` referencia `despesascusto`; `DiarioCampoDAO` lê esta tabela. A lista (GET sem parâmetros) **só ganhou campos** (ver abaixo) — nada foi renomeado/removido.
- **Safra (resumo financeiro)** soma essas despesas via Diário.
- **Diário de Campo (onda 2)** — preço da caixa de colheita (`CAIXA_COLHEITA`/`CONTENTOR`) e **Colheita e Estoque (onda 3)** — embalagens do embalador (`EMBALAGEM`) consomem o contrato abaixo.

### Contrato V28 — preço de caixa/embalagem

**Fluxo esperado:** buscar → se não encontrou, pedir o valor ao usuário → registrar (upsert) → usar o `iddespesascusto` devolvido.

**DAO (`Model.Dao.DespesasCustosDAO`)** — cada método tem versão com `Connection` como 1º parâmetro, que **não** abre/comita/fecha (roda na transação do chamador, ex.: `TransacaoUtil`):

| Método | Retorno |
|---|---|
| `buscarPrecoCaixa([Connection c,] String categoria, int idAlimento, int idArea, String tipoCaixa)` | `DespesasCustos` ou `null` |
| `registrarPrecoCaixa([Connection c,] String categoria, int idAlimento, int idArea, String tipoCaixa, String descricao, BigDecimal valor)` | `int iddespesascusto` (insere ou atualiza o valor) |
| `listarEmbalagens([Connection c,] int idAlimento, int idArea)` | `List<DespesasCustos>` (só `EMBALAGEM`, ordem por `tipo_caixa`) |
| `static normalizarTipoCaixa(String)` | código normalizado ("Plástico 20" → `PLASTICO_20`), `null` se vazio |

Regras (valem para DAO e endpoints):
- `categoria` só `CAIXA_COLHEITA` ou `EMBALAGEM` (case-insensitive). `tipoCaixa` vazio em `CAIXA_COLHEITA` ⇒ `CONTENTOR`; em `EMBALAGEM` é obrigatório.
- `tipoCaixa` é normalizado (sem acento, maiúsculas, espaço/hífen → `_`) tanto na busca quanto no registro.
- `valor` > 0 (até 99.999.999,99). `descricao` vazia ⇒ gerada ("Caixa Contentor - CAQUI RAMA FORTE - SS"); num registro existente, descrição vazia **preserva** a atual.
- Violação de regra ⇒ `IllegalArgumentException` com mensagem pronta para o usuário (alimento/área inexistente, código inválido, valor ≤ 0). `SQLException` propaga.

**Bean `DespesasCustos` (campos novos no JSON — Gson omite os nulos, então registros `GERAL` só ganham `categoria`):**
`categoria`, `idAlimento`, `idAreaProducao`, `tipoCaixa`, e só-leitura (JOIN) `nomeAlimento` (`alimento.nomeproduto`), `nomeArea` (`propriedadeareaproducao`), `siglaArea`.

**Endpoints (`/DespesasCustosServlet`):**

`GET ?precoCaixa=1&categoria=CAIXA_COLHEITA&idAlimento=1&idArea=2&tipoCaixa=CONTENTOR`
```json
{"ok":true,"msg":"Preço encontrado.","target":"page","encontrado":true,
 "registro":{"iddespesascusto":6,"despesascusto":"Contentor caqui rama forte SS","unidadedespesascustos":"un",
   "valordespesascustos":3.5,"tipodespesascustos":"Caixa colheita","classificacao":"VARIAVEL",
   "categoria":"CAIXA_COLHEITA","idAlimento":1,"idAreaProducao":2,"tipoCaixa":"CONTENTOR",
   "nomeAlimento":"CAQUI RAMA FORTE","nomeArea":"Sitio Silas","siglaArea":"SS"}}
```
Não encontrado: `{"ok":true,"encontrado":false,"msg":"Preço não cadastrado para este alimento/área/tipo.","target":"page"}` (HTTP 200).

`GET ?embalagens=1&idAlimento=1&idArea=2` → `{"ok":true,"msg":"","target":"page","embalagens":[{...mesmo formato de registro...}]}` (lista vazia ⇒ `msg` "Nenhuma embalagem cadastrada…").

`GET ?tiposCaixa=1` → `{"ok":true,"tipos":[{"codigo":"CONTENTOR","rotulo":"Caixa Contentor","cargo":"Colhedor"}, ...]}` (sugestões do enum).

`POST acao=registrarPrecoCaixa&categoria=EMBALAGEM&idAlimento=1&idArea=2&tipoCaixa=PLASTICO_20&valor=4,75[&descricao=...][&target=idDoAlerta]`
```json
{"ok":true,"msg":"Preço atualizado com sucesso!","target":"page","id":7,"registro":{...}}
```
`msg` = "Preço registrado com sucesso!" quando inseriu. `valor` aceita `3,50`, `1.234,56` ou `3.50`. `target` opcional (padrão `page`) para o chamador indicar o alerta da sua tela.

Erros: HTTP 400 `{"ok":false,"msg":"<regra>","target":...}` (ex.: `idAlimento e idArea são obrigatórios.`, `Categoria deve ser CAIXA_COLHEITA ou EMBALAGEM.`, `Área de produção não encontrada.`); HTTP 500 com código do `LogUtil`.

`POST acao=create` aceita, opcionalmente, `categoria`, `idAlimento`, `idArea`, `tipoCaixa` (tela de cadastro); recusa (400) se a chave já existir — o upsert é só no `registrarPrecoCaixa`. `update` não altera categoria/alimento/área/tipo.

## Pendências
- [ ] Modal de **atualização** não permite trocar categoria/alimento/área/tipo (só descrição, unidade, valor, tipo, classificação). Para mudar a chave: excluir e recadastrar.
- [x] Dados de teste (ids 6, 7, 9, 10 e 16) apagados em 2026-10-25 pelo Gerente Geral, a pedido do usuário. Não há preço de caixa/embalagem cadastrado: o primeiro lançamento real de Colheita ou Processamento vai registrar.

## Histórico
- 2026-09-07 — corrigido update sempre bloqueado; adicionada classificação Fixo/Variável (V23).
- 2026-10-25 — **V28**: categoria GERAL/CAIXA_COLHEITA/EMBALAGEM + alimento/área/tipo de caixa com chave única parcial; DAO `buscarPrecoCaixa`/`registrarPrecoCaixa` (upsert)/`listarEmbalagens` (com e sem `Connection`); endpoints `precoCaixa`, `embalagens`, `tiposCaixa`, `registrarPrecoCaixa`; tela com campo Categoria (Alimento, Área, Tipo de caixa com "Outro (digitar)") e coluna Categoria. Botões Editar/Excluir passam só o id (antes embutiam textos do banco no `onclick`). Verificado no navegador e via fetch.
