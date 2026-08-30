<!DOCTYPE html>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    String _redirect = request.getParameter("redirect");
    String _ctx      = request.getContextPath();

    // Validacao do destino pos-login.
    //
    // Duas exigencias, nao uma:
    //   1) tem de ser uma URL interna do sistema (o startsWith de antes);
    //   2) tem de conter APENAS caracteres seguros de URL.
    //
    // So o startsWith nao bastava: o valor e impresso dentro de uma string
    // JavaScript logo abaixo, e um destino como
    //     /agro/view/admin/x';alert(document.cookie);//
    // passava na verificacao e era injetado cru entre as aspas — XSS refletido.
    // O AuthFilter agora GERA links com ?redirect= a cada acesso sem sessao, o
    // que tornava o ataque bem plausivel. A lista branca de caracteres fecha o
    // vetor na origem: aspas, barra invertida e sinal de menor nao entram.
    boolean _destinoValido =
            _redirect != null
         && !_redirect.isEmpty()
         && _redirect.startsWith(_ctx + "/view/admin/")
         && _redirect.matches("[A-Za-z0-9/_.\\-]*(\\?[A-Za-z0-9/_.\\-=&%]*)?");

    if (!_destinoValido) {
        _redirect = _ctx + "/view/admin/index.jsp";
    }
    // Já logado: vai direto ao destino
    if (session.getAttribute("usuarioLogado") != null) {
        response.sendRedirect(_redirect);
        return;
    }
%>
<html>
<head>
    <title>Agro Tech One — Login</title>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.0/css/all.min.css">
    <link href="CSS/estilo.css" rel="stylesheet">
</head>
<body>
<div class="login-wrapper">
    <div class="login-card">
        <div class="login-header">
            <h4><i class="fas fa-leaf me-2"></i>Agro Tech One</h4>
            <p class="mb-0 small">Sistema de Gestão Agrícola</p>
        </div>
        <div class="card shadow-sm" style="border-top: none; border-radius: 0 0 4px 4px;">
            <div class="card-body p-4">
                <div id="alertLogin" class="alert d-none" role="alert"></div>
                <form id="formLogin" data-action="<%=_ctx%>/LoginServlet">
                    <div class="mb-3">
                        <label for="usuario" class="form-label"><i class="fas fa-user me-1"></i>Usuário</label>
                        <input type="text" class="form-control" id="usuario" name="usuario"
                               placeholder="Seu usuário" required autofocus>
                    </div>
                    <div class="mb-3">
                        <label for="senha" class="form-label"><i class="fas fa-lock me-1"></i>Senha</label>
                        <div class="input-group">
                            <input type="password" class="form-control" id="senha" name="senha"
                                   placeholder="Sua senha" required>
                            <button class="btn btn-outline-secondary" type="button" id="btnToggleSenha">
                                <i class="fas fa-eye" id="iconeSenha"></i>
                            </button>
                        </div>
                    </div>
                    <button type="submit" class="btn btn-login w-100 mt-3" id="btnLogin">
                        <i class="fas fa-sign-in-alt me-1"></i>Entrar
                    </button>
                </form>
            </div>
        </div>
    </div>
</div>

<script src="https://code.jquery.com/jquery-3.7.1.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<!-- URL de destino após login — injeta valor de servidor para main.js -->
<script>window.LOGIN_REDIRECT = '<%=_redirect%>';</script>
<script src="JS/main.js"></script>
</body>
</html>
