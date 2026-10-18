# Módulo Perfil CLT

**Objetivo:** perfil do funcionário CLT rural: salário, faltas, vales, ponto, fechamento mensal, desligamento e readmissão.
**Menu:** RH → Perfis e Fechamento (CLT)

## Escopo
| Camada | Arquivo |
|---|---|
| Controller | `Controller/ControllerCLT.java` — URL `/ControllerCLT` |
| DAO | `Model/Dao/FuncionarioCLTDAO.java`, `Model/Dao/FaltaFuncionarioDAO.java`, `Model/Dao/FechamentoFolhaDAO.java`, `Model/Dao/ValeFuncionarioDAO.java` |
| Model | `FuncionarioCLT.java`, `FaltaFuncionario.java`, `FechamentoFolha.java`, `ValeFuncionario.java`, `TipoAtividadeRural.java` |
| Service | `Service/FolhaCalculoRuralService.java`, `Service/CalculoPontoRural.java` (compartilhados — ver Dependências) |
| View | `view/admin/perfilCLT.jsp` |
| JS | `main.js` → `initPerfilCLT()` / `carregarPerfilCLT()` e o bloco até `helpers de formatação usados no módulo CLT` |

**Ações:** `perfil`, `calcularfechamento`, `atualizarsalario`, `atualizarstatus`, `registrarfalta`, `excluirfalta`, `registrarvale`, `excluirvale`, `marcarvaledescontado`, `desligarvinculo`, `readmitir`.

## Banco
- `funcionarioclt`, `falta_funcionario`, `fechamento_folha_ponto`, `vale_funcionario`; lê `ponto_eletronico`, `vinculo_empregaticio`.
- Migrações: V3 (CLT rural), V8 (multimodo), V21 (regras de custo/folha), V22 (encargos).

## Dependências
- **Ponto Eletrônico**: mesmas tabelas de ponto, falta, vale e fechamento — coordenar.
- **Pagamento**: o modal de pagamentos é incluído no perfil.
- `FolhaCalculoRuralService` também é usado por **Diário de Campo** (custo da hora) e `VinculoDAO`.
- **Funcionário** (cadastro base e `VinculoDAO`).

## Pendências
- [ ] —

## Histórico
