# Módulo Alimento

**Objetivo:** cadastro dos alimentos/culturas (tipo de planta) e das classificações aceitas por cada um.
**Menu:** Produção → Alimento

## Escopo
| Camada | Arquivo |
|---|---|
| Controller | `Controller/ControllerAlimento.java` — URL `/ControllerAlimento` |
| DAO | `Model/Dao/AlimentoDAO.java`, `Model/Dao/AlimentoClassificacaoDao.java` |
| Model | `Alimento.java`, `AlimentoClassificacao.java` (`Produto.java` existe, sem uso em controller) |
| View | `alimento.jsp`, `AtualizacaoAlimento.jsp` |
| JS | `main.js` → seção `MÓDULO ALIMENTO` + blocos `alimento.jsp`, `AtualizacaoAlimento.jsp` |

**Ações:** `create`, `update`, `delete`, `atualizacaoalimentoclassificacao`, `atualizacaoalimentoclassificacaomodal`, `salvarclassificacoesalimentomodal`.

## Banco
- `alimento`, `alimentoclassificacao` (FK para `classificacao`).

## Dependências
- **Classificação**: lê `ClassificacaoDAO` (não editar aqui — é do módulo Classificação).
- **Área de Produção**: `quadra` referencia o alimento plantado.
- **Safra, Diário de Campo, Colheita**: fazem join com `alimento`.

## Pendências
- [ ] —

## Histórico
