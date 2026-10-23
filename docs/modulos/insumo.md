# Módulo Insumo

**Objetivo:** cadastro de insumos (defensivos, adubos etc.) com categoria, unidade, embalagem e fornecedor.
**Menu:** Configuração → Estoque de Insumos (tela compartilhada com o módulo Estoque)

## Escopo
| Camada | Arquivo |
|---|---|
| Controller | `Controller/ControllerInsumo.java` — URL `/ControllerInsumo` |
| DAO | `Model/Dao/InsumoDAO.java` |
| Model | `Insumo.java`, `CategoriaInsumo.java`, `UnidadeMedida.java` |
| View | `view/admin/estoque.jsp`, **só a parte de cadastro de insumos** |
| JS | `main.js` → seção `MÓDULO ESTOQUE DE INSUMOS`, funções do cadastro de insumo (chamadas a `/ControllerInsumo`) |

**Ações:** `create`, `update`, `delete`, `dados`, `fornecedores`.

## Banco
- `insumo`: migrações V11 (estoque), V25 (dose/embalagem), V26 (quantidade direta de adubo).

## Dependências
- **Estoque** divide a mesma tela e o mesmo trecho do JS — coordenar mudanças em `estoque.jsp`.
- **Parceiro**: fornecedor (`ParceiroDAO`).
- **Diário de Campo**: `diario_insumo` consome insumos (dose, unidade) — mudança em unidade/dose afeta o cálculo do diário.

## Pendências
- [x] V25 e V26 commitadas (9f04351).
- [ ] Entrada de estoque (módulo Estoque) ainda usa só o saco do cadastro como atalho; se o mesmo adubo chegar em sacos de pesos diferentes, avaliar um seletor de saco na Entrada.

## Histórico
- 2026-10-22 — Adubo (grandeza Massa): o campo livre "Peso do saco" vira o seletor "A granel / Saco 5, 10, 25 ou 50 kg" (`#insSacoAdubo`). O valor é em kg e é convertido para a unidade do insumo ao salvar (saco de 50 kg num adubo em T → `peso_embalagem` 0,05). Adubo a granel usa a unidade Tonelada. Defensivo continua com o campo livre. Sem mudança de banco no `insumo`.
