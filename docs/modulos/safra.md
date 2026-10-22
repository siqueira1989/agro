# Módulo Safra

**Objetivo:** safras por área de produção (talhões vinculados, período, status) e resumo financeiro com rateio de custo por talhão.
**Menu:** Produção → Safra

## Escopo
| Camada | Arquivo |
|---|---|
| Controller | `Controller/ControllerSafra.java` — URL `/ControllerSafra` |
| DAO | `Model/Dao/SafraDAO.java` |
| Model | `Safra.java`, `SafraTalhao.java` |
| View | `safra.jsp`, `resumoSafra.jsp` |
| JS | `main.js` → seções `MÓDULO SAFRA` e `RESUMO FINANCEIRO DA SAFRA` |

**Ações:** `create`, `update`, `status`.

## Banco
- `safra`, `safra_talhao` (V16 safra, V17 diário com safra obrigatória, V18 `safra.id_area_producao`).
- Lê `areaproducao`, `quadra`, `alimento`, `diario_campo` (custos).

## Dependências
- **Diário de Campo**: todo diário pertence a uma safra; `ControllerDiarioCampo` usa `SafraDAO`. Mudança em período/status/trava afeta o lançamento do diário.
- **Área de Produção**, **Alimento**.

## Pendências
- [ ] Coluna "Diferença" em talhão com R$ 0,00 lançado + atividades em andamento fica verde (−custo dividido), o que pode ler como "economia". Avaliar se deve ficar neutra/oculta nesse caso (o pedido de 2026-10-22 mandou exibir).
- [ ] Dados da Safra id 2: quadras 3 e 5 têm atividades EM_ANDAMENTO a finalizar no Diário de Campo (ação do usuário, não de código).

## Histórico
- 2026-08-25 — cadastro por área (todas as quadras), resumo com filtro por alimento e datas em formato BR.
- 2026-10-22 — Resumo: tabela de rateio autoexplicativa sem mudar a regra de cálculo. Colunas renomeadas ("Custo dividido (por nº de plantas)", "Custo lançado no Diário") com tooltip; nova coluna "Diferença" (lançado − dividido, verde/vermelho, total no rodapé); badge "N em andamento (R$ Y)" e "Sem lançamentos"; linha de total pendente do filtro; nota sobre Despesas Fixas (só quando > 0). JSON `?resumo=`: cada item de `rateio` ganhou `qtdPendentes` e `valorPendente` (PLANEJADA/EM_ANDAMENTO no período; `SafraDAO.pendenciasTalhao`). Textos do banco escapados (`rsEsc`).
