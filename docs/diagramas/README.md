# Diagramas do sistema

| Arquivo | Conteúdo | Como abrir |
|---|---|---|
| `classes-insumo-diario.drawio` | Classes de Insumo (adubo) e Diário de Campo, com o que mudou em 22/10/2026 (V27) destacado em amarelo | draw.io: Arquivo → Abrir |
| `classes-dominio.mmd` | Modelo de domínio completo: as 43 classes de `Model/Model`, herança, composição e associações | draw.io: Organizar → Inserir → Avançado → Mermaid, e colar o conteúdo |
| `camadas.mmd` | Visão em camadas: Filter → 22 Controllers → 5 Services → 28 DAOs → Util → PostgreSQL | idem (usa layout ELK) |

Gerados em 22/10/2026 a partir do código (`src/main/java`). Atualize quando surgir classe nova.

## Cores (visual corporativo, fonte Inter)

- **Domínio:** cinza-ardósia = pessoas e RH (Pessoa, Funcionário, ponto, folha) · azul = produção agrícola (Área, Quadra, Safra, Diário) · âmbar = Insumo e DiarioInsumo (alterados em 22/10/2026) · verde = estoque, máquinas e despesas · cinza-claro = enums.
- **Camadas:** cinza-ardósia = Controller/Filter · âmbar = Service · azul = DAO · cinza-claro = Util · azul-marinho = banco.
