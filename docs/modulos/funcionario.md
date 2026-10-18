# Módulo Funcionário

**Objetivo:** cadastro de funcionários (CLT, diarista, empreita, produção), vínculo empregatício, ativar/desativar e busca por CPF.
**Menu:** RH → Funcionários

## Escopo
| Camada | Arquivo |
|---|---|
| Controller | `Controller/ControllerFuncionario.java` — URL `/ControllerFuncionario` |
| DAO | `Model/Dao/FuncionarioDAO.java`, `Model/Dao/VinculoDAO.java`, `Model/Dao/PessoaDAO.java` (compartilhado com o login) |
| Model | `Funcionario.java`, `FuncionarioCLT.java`, `FuncionarioDiarista.java`, `FuncionarioEmpreita.java`, `FuncionarioProducao.java`, `TipoFuncionario.java`, `Vinculo.java`, `Pessoa.java`, `PessoaFisica.java` |
| View | `Colaboradores.jsp`, `ColaboCadastro.jsp` |
| JS | `main.js` → seção `MÓDULO COLABORADOR` + blocos `Colaboradores.jsp`, `ColaboCadastro.jsp` do `$(document).ready` |

**Ações:** `create`, `update`, `delete`, `buscarcpf`, `obteridcpf`, `contagem`.

## Banco
- `pessoa`, `funcionario` (pai) e filhas `funcionarioclt`, `funcionariodiarista`, `funcionarioempreita`, `funcionarioproducao`; `vinculo_empregaticio`.
- Migrações: V2 (triggers de integridade), V4/V5 (vínculo), V20 (senha/usuário).

## Dependências
- Base de todos os perfis: **CLT**, **Diarista**, **Empreita**, **Produção (caixas)**, **Ponto**, **Folha**, **Diário de Campo** leem `funcionario`/`vinculo_empregaticio`. Mudança de coluna ou tipo → avisar essas conversas.
- `PessoaDAO` também é usado pelo **Login** (núcleo).

## Pendências
- [ ] —

## Histórico
- 2026-06-23 — corrigidos DIARIA→DIARISTA e insert sempre na tabela pai.
