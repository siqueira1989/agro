# Módulo Parceiro

**Objetivo:** cadastro de parceiros (fornecedores, clientes) com endereço via ViaCEP.
**Menu:** Configuração → Parceiro

## Escopo
| Camada | Arquivo |
|---|---|
| Controller | `Controller/ControllerParceiro.java` — URL `/ControllerParceiro` |
| DAO | `Model/Dao/ParceiroDAO.java` |
| Model | `Parceiro.java`, `TipoParceiro.java` (bases `Pessoa`, `PessoaCnpj`, `PessoaFisica` são compartilhadas — ver núcleo) |
| View | `parceiro.jsp`, `CadastroParceiro.jsp`, `AtualizarParceiro.jsp` |
| JS | `main.js` → seção `MÓDULO PARCEIRO` + blocos `parceiro.jsp`, `CadastroParceiro.jsp`, `AtualizarParceiro.jsp` (ViaCEP) |

**Ações:** `create`, `dados`, `update`, `delete`.

## Banco
- `parceiro` (tipo de parceiro: V10).

## Dependências
- **Insumo**: fornecedor do insumo (`InsumoDAO` faz join com `parceiro`; `ControllerInsumo` usa `ParceiroDAO`).
- **Estoque**: movimento de entrada guarda o parceiro (`EstoqueMovimentoDAO`, `ControllerEstoque`).

## Pendências
- [ ] —

## Histórico
