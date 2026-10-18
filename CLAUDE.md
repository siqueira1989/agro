# Agro Tech One — instruções para o Claude

Sistema de gestão agrícola (fruticultura): Jakarta EE 10 (Servlets + JSP), PostgreSQL 17,
jQuery + Bootstrap + DataTables. Camadas: `Controller/` (servlets) → `Model/Dao/` (JDBC) →
`Model/Model/` (beans); regras de cálculo em `Service/`; front-end em `view/admin/*.jsp` +
`JS/main.js` (arquivo único, dividido em seções `MÓDULO ...`).

## Trabalho por módulo (uma conversa por módulo)

O desenvolvimento é organizado em **conversas dedicadas por módulo**. O índice e as fichas
ficam em [docs/modulos/](docs/modulos/README.md). Uma conversa de módulo começa com
`/modulo <slug>` (ex.: `/modulo classificacao`) ou com a frase
"Esta conversa é o Módulo X — leia docs/modulos/<slug>.md".

Regras quando a conversa é de um módulo:

1. **Ler a ficha do módulo antes de qualquer coisa** e confirmar o escopo em poucas linhas.
2. **Editar só os arquivos listados na ficha.** Arquivos compartilhados (`main.js`, `web.xml`,
   `menu.jsp`, `estilo.css`, `importacao.html`) só na seção/trecho do módulo.
3. **Fora do escopo = avisar, não editar.** Se a mudança exigir alterar outro módulo (ex.: uma
   coluna nova usada pelo Diário de Campo), parar, descrever o impacto e sugerir levar o
   pedido à conversa daquele módulo.
4. **Dependências:** antes de mudar tabela, DAO ou JSON de resposta, conferir a seção
   "Dependências" da ficha — outros módulos podem ler os mesmos dados.
5. **Atualizar a ficha ao final** de cada entrega (seção "Histórico" e "Pendências").
6. Migrações: próxima versão livre em `src/main/resources/db/migration/` (checar o maior
   `V<n>` antes — outras conversas podem ter criado uma). Uma migração por mudança.

## Convenções do código

- Servlets: GET devolve JSON (lista para DataTables); POST com parâmetro `acao`
  (create/update/delete/...) responde `{ok, msg, target}` — `target` é o id do alerta na tela.
- Erros: `Util.LogUtil.erro(...)` grava a pilha no log e devolve um código; o usuário vê só o código.
- DAO: `try-with-resources` sempre; exceções SQL propagam (nunca engolir).
- Conexões via `Util.PostgresConnection` (pool em `Util.ConnectionPool`); transações com `Util.TransacaoUtil`.
- HTML vindo do banco deve ser escapado (`Util.HtmlUtil` no Java; escapar no JS antes de montar `onclick`/HTML).
- Fragmentos JSP incluídos (`menu.jsp`, `footer.jsp`, `modalPagamento.jsp`) precisam de
  `<%@ page pageEncoding="UTF-8" %>` própria.

## Build e deploy local

- Build: `mvnw.cmd clean package -DskipTests` (usar o wrapper, não o `mvn` global).
- Tomcat serve `target/agro-1.0-SNAPSHOT` (exploded). Mudança só em JS/JSP: copiar o arquivo
  para `target/agro-1.0-SNAPSHOT/...` sem rebuild. Mudança em Java: parar o Tomcat antes do
  `clean` (Windows trava os jars), rebuild e religar.
- Trabalhando em worktree (`.claude/worktrees/*`): espelhar a edição no checkout principal
  `C:\projeto\agro` — o Tomcat não enxerga o worktree.
- Verificar no navegador em `http://localhost:8080/agro/...` antes de dar como concluído.
