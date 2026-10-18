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
- [ ] V25 e V26 ainda não commitadas (ver `git status`).

## Histórico
