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
- [ ] —

## Histórico
- 2026-08-25 — cadastro por área (todas as quadras), resumo com filtro por alimento e datas em formato BR.
