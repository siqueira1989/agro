# Módulo Área de Produção

**Objetivo:** áreas produtivas e suas quadras/talhões (com o alimento plantado); perfil da área.
**Menu:** Produção → Área de Produção

## Escopo
| Camada | Arquivo |
|---|---|
| Controller | `Controller/ControllerAreaProducao.java` — URL `/ControllerAreaProducao` |
| DAO | `Model/Dao/AreaProducaoDAO.java`, `Model/Dao/QuadraDAO.java` |
| Model | `AreaProducao.java`, `Quadra.java` |
| View | `areaproducao.jsp`, `CadastroAreaProducao.jsp`, `AtualizarAreaProducao.jsp`, `DetalheAreaProducao.jsp` (**só cabeçalho e quadras** — o Diário de Campo vive na mesma página) |
| JS | `main.js` → seção `MÓDULO ÁREA DE PRODUÇÃO` (LISTA, CADASTRO, ATUALIZAÇÃO, DETALHE) |

**Ações:** `create`, `update`, `delete`, `desativar`, `addquadra`, `ativarquadra`, `desativarquadra`.

## Banco
- `areaproducao`, `quadra` (V12 remove talão, V13 quadras).

## Dependências
- **Diário de Campo**: mesma página (`DetalheAreaProducao.jsp`).
- **Safra**: vincula área e quadras (`safra_talhao`, V18).
- **Colheita** e **Relatórios** (export Excel/PDF da área em `RelatorioExportServlet`, que é do núcleo).

## Pendências
- [ ] —

## Histórico
