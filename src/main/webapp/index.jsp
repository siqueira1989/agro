<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    // Raiz da aplicacao.
    //
    // Este arquivo era uma COPIA IDENTICA de view/admin/index.jsp — 124 linhas do
    // Dashboard duplicadas —, so que com os includes apontando para
    // "../../pagina/...", caminho que resolve ACIMA do context root e nao existe.
    // Na pratica a raiz respondia erro de traducao de JSP; e, como "/" estava
    // liberado no filtro, servia a estrutura do painel a quem nao tinha login.
    //
    // A raiz nao precisa renderizar nada: quem esta autenticado vai ao painel,
    // quem nao esta vai ao login. Uma tela so, em um lugar so.
    if (session.getAttribute("usuarioLogado") != null) {
        response.sendRedirect(request.getContextPath() + "/view/admin/index.jsp");
    } else {
        response.sendRedirect(request.getContextPath() + "/login.jsp");
    }
%>
