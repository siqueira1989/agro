# Módulo Despesas e Custos

**Objetivo:** cadastro das despesas/custos (fixos e variáveis) usados nos lançamentos do Diário de Campo e no custo da safra.
**Menu:** Configuração → Custos e Despesas

## Escopo
| Camada | Arquivo |
|---|---|
| Controller | `Controller/DespesasCustosServlet.java` — URL `/DespesasCustosServlet` |
| DAO | `Model/Dao/DespesasCustosDAO.java` |
| Model | `Model/Model/DespesasCustos.java` (`LancamentoDespesasCustos.java` existe, mas nenhum controller usa) |
| View | `view/admin/despesascustos.jsp` |
| JS | `main.js` → seção `MÓDULO DESPESAS E CUSTOS` + bloco `despesascustos.jsp` do `$(document).ready` |

**Ações:** `create`, `update`, `delete`. GET = lista.

## Banco
- `despesascusto`: coluna `classificacao` FIXO/VARIAVEL (V23, CHECK `ck_despesascusto_classificacao`).

## Dependências
- **Diário de Campo**: `diario_despesa` referencia `despesascusto`; `DiarioCampoDAO` lê esta tabela.
- **Safra (resumo financeiro)** soma essas despesas via Diário.

## Pendências
- [ ] —

## Histórico
- 2026-09-07 — corrigido update sempre bloqueado; adicionada classificação Fixo/Variável (V23).
