# Módulo Classificação

**Objetivo:** cadastro dos padrões de classificação de qualidade dos alimentos (ex.: Extra, Primeira).
**Menu:** Configuração → Classificação

## Escopo (arquivos que esta conversa pode editar)
| Camada | Arquivo |
|---|---|
| Controller | `src/main/java/Controller/ClassificacaoServlet.java` — URL `/ClassificacaoServlet` |
| DAO | `src/main/java/Model/Dao/ClassificacaoDAO.java` |
| Model | `src/main/java/Model/Model/Classificacao.java` |
| View | `src/main/webapp/view/admin/classificacao.jsp` |
| JS | `main.js` → seção `MÓDULO CLASSIFICAÇÃO` + bloco `/* ========== classificacao.jsp ========== */` do `$(document).ready` |

**Ações (POST `acao`):** `create`, `update`, `delete`. GET = lista.

## Banco
- `classificacao` (`idclassificacao` PK, `classificacao varchar(50) NOT NULL`) — sem UNIQUE.
- Lê `alimentoclassificacao` para bloquear exclusão de classificação em uso.
- Não confundir com a coluna `despesascusto.classificacao` (FIXO/VARIAVEL), que é do módulo Despesas e Custos.

## Dependências
- **Alimento** usa esta tabela: `ControllerAlimento` chama `ClassificacaoDAO`; `AlimentoClassificacaoDao` faz join com `classificacao`. Mudou o JSON ou a tabela → avisar a conversa do Alimento.

## Pendências
- [ ] Nome com apóstrofo quebra os botões e permite XSS (`onclick` montado sem escape no DataTable).
- [ ] Duplicidade: sem `trim`, comparação sensível a maiúsculas, sem UNIQUE no banco.
- [ ] Sem validação de tamanho (> 50 caracteres vira erro SQL genérico).
- [ ] `id` não numérico → 500 em vez de 400; mensagem de exclusão vaga e sem acento.

## Histórico
- 2026-08-29 — auditoria: conexões em try-with-resources e exceções propagadas no DAO.
