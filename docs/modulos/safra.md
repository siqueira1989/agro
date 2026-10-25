# Módulo Safra

**Objetivo:** safras por área de produção (talhões vinculados, período, status) e resumo financeiro com rateio de custo por talhão.
**Menu:** Produção → Safra

## Escopo
| Camada | Arquivo |
|---|---|
| Controller | `Controller/ControllerSafra.java` — URL `/ControllerSafra` |
| DAO | `Model/Dao/SafraDAO.java` |
| Model | `Safra.java`, `SafraTalhao.java` |
| View | `safra.jsp`, `resumoSafra.jsp` |
| JS | `main.js` → seções `MÓDULO SAFRA` e `RESUMO FINANCEIRO DA SAFRA` |

**Ações:** `create`, `update`, `status`.

## Banco
- `safra`, `safra_talhao` (V16 safra, V17 diário com safra obrigatória, V18 `safra.id_area_producao`).
- Lê `areaproducao`, `quadra`, `alimento`, `diario_campo` (custos).

## Dependências
- **Diário de Campo**: todo diário pertence a uma safra; `ControllerDiarioCampo` usa `SafraDAO`. Mudança em período/status/trava afeta o lançamento do diário.
- **Área de Produção**, **Alimento**.

## Pendências
- [x] Coluna "Diferença" neutra quando o lançado é R$ 0,00 e só há atividades em andamento (feito em 2026-10-25).
- [ ] Dados da Safra id 2: quadras 3 e 5 têm atividades EM_ANDAMENTO a finalizar no Diário de Campo (ação do usuário, não de código).
- [ ] **Sem validação de sobreposição:** é possível cadastrar duas safras com o mesmo talhão no mesmo período. Nesse caso `resolverSafra` escolhe pela ordem de status (EM_ANDAMENTO > PLANEJADA > FINALIZADA) e, no empate, uma qualquer (sem ORDER BY de desempate), o que pode mandar diários para a safra errada. Sugestão: bloquear no create/update. É regra nova, então precisa da decisão do Gerente.
- [ ] Safra 4: o talhão "Silas Sabia SS" tem área 0 ha, por isso o "Custo por ha" aparece R$ 0,00. É dado do módulo Área de Produção.
- [ ] Detalhe visual: depois de um erro de validação no modal, a mensagem continua visível quando o salvamento seguinte dá certo (o modal fecha, então quase não aparece).

## Histórico
- 2026-08-25 — cadastro por área (todas as quadras), resumo com filtro por alimento e datas em formato BR.
- 2026-10-22 — Resumo: tabela de rateio autoexplicativa sem mudar a regra de cálculo. Colunas renomeadas ("Custo dividido (por nº de plantas)", "Custo lançado no Diário") com tooltip; nova coluna "Diferença" (lançado − dividido, verde/vermelho, total no rodapé); badge "N em andamento (R$ Y)" e "Sem lançamentos"; linha de total pendente do filtro; nota sobre Despesas Fixas (só quando > 0). JSON `?resumo=`: cada item de `rateio` ganhou `qtdPendentes` e `valorPendente` (PLANEJADA/EM_ANDAMENTO no período; `SafraDAO.pendenciasTalhao`). Textos do banco escapados (`rsEsc`).
- 2026-10-25 — Verificação de ponta a ponta (pedido do Gerente). Passou: listagem; criar/editar (safra de teste id 5, em 2020, depois apagada); validação de campos obrigatórios e de data final < inicial; ação `status` (válido e inválido); `resolver` (quadra 9 → safra 4, quadra 1 → safra 2, data fora do período → vazio); resumo das safras 2 e 4 (cards, composição = 100%, soma do rateio = custo total, Diferença, badges, filtro por alimento, nota de Despesas Fixas quando > 0). Corrigido: Diferença neutra ("—", cinza, fora do total) quando o lançado é 0 e há atividades em andamento.
