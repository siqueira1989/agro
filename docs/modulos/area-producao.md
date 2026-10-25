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
- [ ] A mudança em `RelatorioExportServlet.java` (remoção do `case "areaproducao"`) só entra no próximo build Maven; até lá o servlet compilado no Tomcat ainda aceita a URL antiga.

## Histórico
- 2026-10-25 — Removida a exportação Excel/PDF da página: botões em `areaproducao.jsp` (antigas linhas 15–21); `main.js` não tinha handler dessa exportação; em `RelatorioExportServlet.java` (autorizado pelo Gerente) removidos `case "areaproducao"`, `exportarAreasExcel/Pdf`, campo/`init` do `AreaProducaoDAO` e imports. Folha e funcionário intactos. JSP copiado para `target`; build pendente.
