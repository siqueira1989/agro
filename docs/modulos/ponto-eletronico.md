# Módulo Ponto Eletrônico

**Objetivo:** marcações de ponto, espelho mensal, faltas, vales e fechamento do ponto por funcionário.
**Menu:** RH → Ponto Eletrônico

## Escopo
| Camada | Arquivo |
|---|---|
| Controller | `Controller/ControllerPontoEletronico.java` — `@WebServlet("/ControllerPontoEletronico")` (único servlet por anotação; os outros estão no `web.xml`) |
| DAO | `Model/Dao/PontoEletronicoDAO.java` |
| Model | `PontoEletronico.java` |
| Service | `Service/FolhaCalculoService.java`, `Service/CalculoPontoRural.java` (compartilhados) |
| View | `view/admin/pontoeletronico.jsp` |
| JS | `main.js` → bloco `/* ========== pontoeletronico.jsp ========== */` |

**Ações:** `pontos`, `registrar_ponto`, `salvar_ponto_manual`, `editar_ponto`, `excluir_ponto`, `faltas`, `registrar_falta`, `excluir_falta`, `vales`, `registrar_vale`, `excluir_vale`, `fechamento`, `fechar_folha`.

## Banco
- `ponto_eletronico`; também escreve em `falta_funcionario`, `vale_funcionario`, `fechamento_folha_ponto`.

## Dependências
- **CLT** usa as mesmas tabelas e DAOs (`FaltaFuncionarioDAO`, `ValeFuncionarioDAO`, `FechamentoFolhaDAO`) — coordenar.
- **Diarista** grava presença em `ponto_eletronico`.
- **Folha de Pagamento** lê o ponto (`PontoEletronicoDAO`).

## Pendências
- [ ] —

## Histórico
