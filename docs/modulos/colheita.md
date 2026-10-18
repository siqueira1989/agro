# Módulo Colheita e Estoque

**Objetivo:** lotes de colheita gerados a partir do Diário de Campo, com saldo em estoque, baixa parcial/total (`processar`), encerramento (`finalizar`) e indicadores por status.
**Menu:** Produção → Colheita e Estoque

## Escopo
| Camada | Arquivo |
|---|---|
| Controller | `Controller/ControllerColheita.java` — URL `/ControllerColheita` |
| DAO | `Model/Dao/ColheitaDAO.java` |
| Model | `ColheitaLote.java` |
| View | `view/admin/colheitaestoque.jsp` |
| JS | `main.js` → seção `MÓDULO COLHEITA E ESTOQUE` |

**Ações:** `processar`, `finalizar`.

## Banco
- `colheita_lote` (V24 — regime de produção); lê `diario_campo`, `areaproducao`, `quadra`, `alimento`.

## Dependências
- **Diário de Campo**: a atividade "Colheita" alimenta os lotes (`DiarioCampoDAO` também lê `colheita_lote`).
- **Área de Produção**, **Alimento**.

## Pendências
- [ ] Módulo inteiro ainda não commitado (controller, DAO, model, JSP e V24).

## Histórico
