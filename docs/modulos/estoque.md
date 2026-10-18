# Módulo Estoque (movimentação de insumos)

**Objetivo:** entradas e saídas de insumos, saldo e trava de estoque insuficiente.
**Menu:** Configuração → Estoque de Insumos

## Escopo
| Camada | Arquivo |
|---|---|
| Controller | `Controller/ControllerEstoque.java` — URL `/ControllerEstoque` |
| DAO | `Model/Dao/EstoqueMovimentoDAO.java`, `Model/Dao/EstoqueInsuficienteException.java` |
| Model | `EstoqueMovimento.java` (usa também `Insumo` e `Parceiro`) |
| View | `view/admin/estoque.jsp`, **parte de movimentação e saldo** |
| JS | `main.js` → seção `MÓDULO ESTOQUE DE INSUMOS`, funções de entrada/saída (chamadas a `/ControllerEstoque`) |

**Ações:** `entrada`, `saida`.

## Banco
- `estoque_movimento` (V11); lê `insumo` e `parceiro`.

## Dependências
- **Insumo**: mesma tela e mesmo trecho do JS.
- **Diário de Campo**: dá baixa de estoque ao lançar insumos e usa `EstoqueInsuficienteException`.
- **Colheita**: o estoque de produto colhido é outro conceito (`colheita_lote`) — não misturar.

## Pendências
- [ ] —

## Histórico
