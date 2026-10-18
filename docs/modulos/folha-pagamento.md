# Módulo Folha de Pagamento

**Objetivo:** gerar e listar a folha do período para todos os funcionários; exportar em Excel/PDF.
**Menu:** RH → Folha de Pagamento

## Escopo
| Camada | Arquivo |
|---|---|
| Controller | `Controller/ControleColaborador.java` — URL `/ControleColaborador` (o nome é histórico: hoje só trata a folha) |
| DAO | `Model/Dao/FolhaPagamentoDAO.java` |
| Model | `FolhaPagamento.java` |
| View | `view/admin/folhapagamento.jsp` |
| JS | `main.js` → bloco `/* ========== folhapagamento.jsp ========== */` |

**Ações:** `gerarfolha`, `listarfolha`.

## Banco
- `folha_pagamento`; lê `pessoa`, `vinculo_empregaticio`.

## Dependências
- Lê `FuncionarioDAO`, `FuncionarioCLTDAO`, `PontoEletronicoDAO` (dados de **Funcionário**, **CLT** e **Ponto Eletrônico** — não editar esses DAOs aqui).
- Export Excel/PDF: `RelatorioExportServlet?modulo=folhapagamento` (núcleo).
- Regras de cálculo compartilhadas em `Service/` (`FolhaCalculoService`, `FolhaCalculoRuralService`, `EncargosService`, `ParametrosFolhaService`) — mudanças ali afetam CLT, Ponto e Diário.

## Pendências
- [ ] —

## Histórico
