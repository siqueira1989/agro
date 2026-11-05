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

- **Romaneio** (bloco "Dados do destino") lê de `parceiro` as colunas **`logradouro varchar(120)`, `bairro varchar(60)`, `cidade varchar(60)`, `uf char(2)`** (V36, opcionais/nulas) e o JSON de `GET /ControllerParceiro?id=` traz as mesmas chaves (`logradouro`, `bairro`, `cidade`, `uf`). Não renomear/remover sem avisar o Romaneio.

## Pendências
- [x] Testado em 2026-11-04 (cadastro e edição com CEP gravam logradouro, bairro, cidade, UF).
- [ ] `AtualizarParceiro.jsp` não compila (HTTP 500: usa `parceiro.getEndereco()`, que não existe mais). Página órfã — nenhum menu/botão leva a ela; a edição é pelo modal de `parceiro.jsp`. Mantida por decisão do usuário em 2026-11-02.
- [ ] —

## Histórico

- 2026-11-04: endereço completo gravado (V36: logradouro, bairro, cidade, uf); campos do ViaCEP agora editáveis em `parceiro.jsp` e `CadastroParceiro.jsp`; create/update/GET incluem os campos. Edição só chama o ViaCEP se o cadastro não tem endereço gravado. Pedido do Gerente para o Romaneio. Corrigido junto: `carregarDadosParceiro` e o redirecionamento pós-cadastro não usavam `AGRO_CTX` (404 sob /agro).
- 2026-11-02: padronização visual do botão de novo registro (Gerente)
