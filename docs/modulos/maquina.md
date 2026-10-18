# Módulo Maquinário

**Objetivo:** cadastro de máquinas e do custo/hora (combustível + manutenção + depreciação).
**Menu:** Configuração → Maquinário

## Escopo
| Camada | Arquivo |
|---|---|
| Controller | `Controller/ControllerMaquina.java` — URL `/ControllerMaquina` |
| DAO | `Model/Dao/MaquinaDAO.java` |
| Model | `Maquina.java` |
| View | `view/admin/maquina.jsp` |
| JS | `main.js` → seção `MÓDULO MAQUINÁRIO` |

**Ações:** `create`, `update`, `delete`.

## Banco
- `maquina` (V15).

## Dependências
- **Diário de Campo**: `diario_maquina` usa o custo/hora; o diário lista máquinas via `/ControllerMaquina`.

## Pendências
- [ ] —

## Histórico
