# Módulo Perfil Produção (por caixa)

**Objetivo:** perfil do funcionário pago por produção: caixas colhidas, preço por tipo de caixa e vales.
**Menu:** RH → Perfis e Fechamento (Produção). Não confundir com o menu "Produção" (área, safra, colheita).

## Escopo
| Camada | Arquivo |
|---|---|
| Controller | `Controller/ControllerProducao.java` — URL `/ControllerProducao` |
| DAO | `Model/Dao/ProducaoCaixaDAO.java`, `Model/Dao/PrecoCaixaDAO.java` |
| Model | `FuncionarioProducao.java`, `TipoCaixa.java` |
| View | `view/admin/perfilProducao.jsp` |
| JS | `main.js` → seção `MÓDULO PRODUÇÃO — perfilProducao.jsp` |

**Ações:** `addcaixa`, `editcaixa`, `removecaixa`, `setpreco`, `registrarvale`, `excluirvale`, `marcarvaledescontado`.

## Banco
- `producao_caixa`, `preco_caixa` (V7); usa `vale_funcionario`, `vinculo_empregaticio`.

## Dependências
- `ValeFuncionarioDAO`, `VinculoDAO`, `FuncionarioDAO` são compartilhados.
- **Diário de Campo / Colheita**: desde a V24, CLT, diarista ou empreita podem ser pagos por caixa num diário de Colheita (`diario_funcionario.regime_trabalho`) — mudança no preço/tipo de caixa afeta esse cálculo.
- **Pagamento** (modal incluído no perfil).

## Pendências
- [ ] —

## Histórico
