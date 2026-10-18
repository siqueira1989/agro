# Núcleo (infraestrutura compartilhada — não é módulo de negócio)

Use esta ficha para mudanças que atravessam módulos: login, layout, menu, relatórios, utilitários, build.

## Escopo
| Área | Arquivo |
|---|---|
| Autenticação | `Controller/LoginServlet.java`, `Controller/LogoutServlet.java`, `Filter/AuthFilter.java`, `login.jsp`, `Util/HashUtil.java`, `Util/SenhaUtil.java`, `Util/MigracaoSenhas.java` |
| Relatórios | `Controller/RelatorioExportServlet.java` (Excel/PDF de funcionário, área de produção e folha) |
| Layout | `pagina/menu.jsp`, `pagina/footer.jsp`, `pagina/base.jsp`, `pagina/importacao.html`, `CSS/estilo.css`, `view/admin/index.jsp` (dashboard) |
| JS geral | `main.js` → `MÓDULO GERAL` (utilitários como `mostrarAlerta`) e blocos `login.jsp`, `index.jsp` do `$(document).ready` |
| Infra | `Util/PostgresConnection.java`, `ConnectionPool.java`, `TransacaoUtil.java`, `LogUtil.java`, `HtmlUtil.java`, `DataUtil.java`, `ConfigUtil.java`, `EmailUtil.java`, `Bootstrap.java`, `SchemaMigrator.java` |
| Config | `WEB-INF/web.xml`, `pom.xml`, `src/main/resources/db/migration/` (numeração), `sql_sistema/agro_banco_completo.sql` (esquema base) |
| Cálculo de folha | `Service/*` — usado por CLT, Ponto, Folha e Diário; tratar aqui quando a mudança for de regra geral |

## Regras
- Mudanças aqui afetam todos os módulos: listar o impacto antes de editar.
- Ao criar módulo novo: registrar o servlet no `web.xml`, adicionar link no `menu.jsp` e criar a ficha em `docs/modulos/`.
