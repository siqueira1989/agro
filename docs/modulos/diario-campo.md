# Módulo Diário de Campo

**Objetivo:** registro das atividades no talhão (agendamento, execução em vários dias, funcionários, máquinas, insumos, despesas) e cálculo do custo.
**Menu:** não tem item próprio — vive dentro do perfil da área (Produção → Área de Produção → Detalhe).

## Escopo
| Camada | Arquivo |
|---|---|
| Controller | `Controller/ControllerDiarioCampo.java` — URL `/ControllerDiarioCampo` |
| DAO | `Model/Dao/DiarioCampoDAO.java`, `Model/Dao/TipoAtividadeDAO.java` |
| Model | `DiarioCampo.java`, `DiarioDespesa.java`, `DiarioFuncionario.java`, `DiarioInsumo.java`, `DiarioMaquina.java`, `TipoAtividade.java` |
| View | `view/admin/DetalheAreaProducao.jsp`, **só a parte do Diário** |
| JS | `main.js` → seção `DIÁRIO DE CAMPO — dentro do Perfil da Área` (inclui AGENDAMENTO e EXECUÇÃO EM MÚLTIPLOS DIAS) |

**Ações:** `create`, `update`, `status`, `finalizar`, `addexecucao`, `addtipoatividade`, `recalcularpessoa`, `removerfuncionario`, `removerdespesa`.

## Banco
- Próprias: `diario_campo`, `diario_funcionario`, `diario_maquina`, `diario_insumo`, `diario_despesa`, `diario_tipo_atividade`.
- Migrações: V14, V17, V19, V23, V25.
- Lê muitas tabelas de outros módulos: `safra`, `areaproducao`, `quadra`, `alimento`, `insumo`, `maquina`, `despesascusto`, `funcionario*`, `pessoa`, `vinculo_empregaticio`, `colheita_lote`.

## Dependências (módulo mais acoplado do sistema)
- **Safra** (`SafraDAO`), **Insumo/Estoque** (baixa de estoque, `EstoqueInsuficienteException`), **Maquinário** (custo/hora), **Despesas e Custos**, **Funcionário/CLT/Diarista/Empreita** (valor da diária/hora via `FolhaCalculoRuralService` e `ParametrosFolhaService`), **Colheita** (atividade "Colheita" gera lote).
- `DetalheAreaProducao.jsp` é dividida com **Área de Produção**.

## Pendências
- [ ] —

## Histórico
- 2026-08-25 — execução em múltiplos dias, valor de diarista/empreita automático.
- 2026-09-07 — despesas fixas/variáveis no diário.
