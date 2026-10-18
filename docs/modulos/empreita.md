# Módulo Perfil Empreita

**Objetivo:** perfil do funcionário por empreita: lançamentos de serviço por empreitada e vales.
**Menu:** RH → Perfis e Fechamento (Empreita)

## Escopo
| Camada | Arquivo |
|---|---|
| Controller | `Controller/ControllerEmpreita.java` — URL `/ControllerEmpreita` |
| DAO | `Model/Dao/LancamentoEmpreitaDAO.java` |
| Model | `FuncionarioEmpreita.java`, `LancamentoEmpreita.java` |
| View | `view/admin/perfilEmpreita.jsp` |
| JS | `main.js` → seção `MÓDULO EMPREITA` |

**Ações:** `addlancamento`, `editlancamento`, `removelancamento`, `registrarvale`, `excluirvale`, `marcarvaledescontado`.

## Banco
- `lancamento_empreita` (V6); usa `vale_funcionario`, `vinculo_empregaticio`.

## Dependências
- `ValeFuncionarioDAO`, `VinculoDAO`, `FuncionarioDAO` são compartilhados.
- **Diário de Campo** lê o valor da empreita para o custo.
- **Pagamento** (modal incluído no perfil).

## Pendências
- [ ] —

## Histórico
