# Módulo Perfil Diarista

**Objetivo:** perfil do diarista: valor da diária, presenças e vales.
**Menu:** RH → Perfis e Fechamento (Diarista)

## Escopo
| Camada | Arquivo |
|---|---|
| Controller | `Controller/ControllerDiarista.java` — URL `/ControllerDiarista` |
| DAO | `Model/Dao/FuncionarioDiaristaDAO.java` |
| Model | `FuncionarioDiarista.java` |
| View | `view/admin/perfilDiarista.jsp` |
| JS | `main.js` → seção `MÓDULO DIARISTA` |

**Ações:** `atualizardiaria`, `marcarpresenca`, `removerpresenca`, `registrarvale`, `excluirvale`, `marcarvaledescontado`.

## Banco
- `funcionariodiarista`; usa `ponto_eletronico` (presença), `vale_funcionario`, `vinculo_empregaticio`.

## Dependências
- Usa `PontoEletronicoDAO`, `ValeFuncionarioDAO`, `VinculoDAO` (compartilhados com **CLT**, **Ponto**, **Empreita**, **Produção** — não alterar assinatura sem avisar).
- **Diário de Campo** lê o valor da diária para o custo.
- **Pagamento** (modal incluído no perfil).

## Pendências
- [ ] —

## Histórico
