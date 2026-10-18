# Módulo Pagamento

**Objetivo:** registrar, listar e excluir pagamentos feitos aos funcionários; prazo de pagamento CLT.
**Menu:** sem item próprio — modal compartilhado incluído nos 4 perfis (CLT, Diarista, Empreita, Produção).

## Escopo
| Camada | Arquivo |
|---|---|
| Controller | `Controller/ControllerPagamento.java` — URL `/ControllerPagamento` |
| DAO | `Model/Dao/PagamentoDAO.java` |
| Model | — (trabalha com `Map`/JSON) |
| View | `src/main/webapp/pagina/modalPagamento.jsp` (fragmento: precisa de `pageEncoding="UTF-8"`) |
| JS | `main.js` → seção `PAGAMENTOS — compartilhado por todos os perfis` (contrato: cada perfil define `window.__pgCtx = {id, periodo, tipo, valor}` e chama `carregarPagamentos()`) |

**Ações:** `registrar`, `listar`, `excluir`, `prazoclt`.

## Banco
- `pagamento` (V9).

## Dependências
- Incluído por `perfilCLT.jsp`, `perfilDiarista.jsp`, `perfilEmpreita.jsp`, `perfilProducao.jsp` — mudar o contrato `__pgCtx` exige ajuste nos 4 perfis (avisar as conversas deles).

## Pendências
- [ ] —

## Histórico
