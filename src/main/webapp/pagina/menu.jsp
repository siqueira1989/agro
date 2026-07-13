<nav class="navbar navbar-expand-lg navbar-dark bg-success">
    <a class="navbar-brand" href="<%=request.getContextPath()%>/view/admin/index.jsp">
        <h3 class="fw-bold text-white mb-0">AgroFazenda</h3>
    </a>
    <button class="navbar-toggler" type="button" data-bs-toggle="collapse"
            data-bs-target="#navbarNav" aria-controls="navbarNav"
            aria-expanded="false" aria-label="Toggle navigation">
        <span class="navbar-toggler-icon"></span>
    </button>
    <div class="collapse navbar-collapse" id="navbarNav">
        <ul class="navbar-nav ms-auto">
            <li class="nav-item">
                <a class="nav-link" href="<%=request.getContextPath()%>/view/admin/index.jsp">
                    <i class="fas fa-home me-1"></i>Home
                </a>
            </li>

            <!-- Configuração -->
            <li class="nav-item dropdown">
                <a class="nav-link dropdown-toggle" href="#" role="button"
                   data-bs-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                    <i class="fas fa-cog me-1"></i>Configuração
                </a>
                <div class="dropdown-menu">
                    <a class="dropdown-item" href="<%=request.getContextPath()%>/view/admin/classificacao.jsp">
                        <i class="fas fa-tags me-1"></i>Classificação
                    </a>
                    <a class="dropdown-item" href="<%=request.getContextPath()%>/view/admin/despesascustos.jsp">
                        <i class="fas fa-receipt me-1"></i>Custos e Despesas
                    </a>
                    <a class="dropdown-item" href="<%=request.getContextPath()%>/view/admin/parceiro.jsp">
                        <i class="fas fa-handshake me-1"></i>Parceiro
                    </a>
                    <a class="dropdown-item" href="<%=request.getContextPath()%>/view/admin/estoque.jsp">
                        <i class="fas fa-warehouse me-1"></i>Estoque de Insumos
                    </a>
                </div>
            </li>

            <!-- Produção -->
            <li class="nav-item dropdown">
                <a class="nav-link dropdown-toggle" href="#" role="button"
                   data-bs-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                    <i class="fas fa-seedling me-1"></i>Produção
                </a>
                <div class="dropdown-menu">
                    <a class="dropdown-item" href="<%=request.getContextPath()%>/view/admin/alimento.jsp">
                        <i class="fas fa-apple-alt me-1"></i>Alimento
                    </a>
                    <a class="dropdown-item" href="<%=request.getContextPath()%>/view/admin/areaproducao.jsp">
                        <i class="fas fa-map-marked-alt me-1"></i>Área de Produção
                    </a>
                    <a class="dropdown-item" href="<%=request.getContextPath()%>/view/admin/talao.jsp">
                        <i class="fas fa-leaf me-1"></i>Talão
                    </a>
                </div>
            </li>

            <!-- RH -->
            <li class="nav-item dropdown">
                <a class="nav-link dropdown-toggle" href="#" role="button"
                   data-bs-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                    <i class="fas fa-users me-1"></i>RH
                </a>
                <div class="dropdown-menu">
                    <a class="dropdown-item" href="<%=request.getContextPath()%>/view/admin/Colaboradores.jsp">
                        <i class="fas fa-user-tie me-1"></i>Funcionários
                    </a>
                    <a class="dropdown-item" href="<%=request.getContextPath()%>/view/admin/folhapagamento.jsp">
                        <i class="fas fa-money-bill-wave me-1"></i>Folha de Pagamento
                    </a>
                    <div class="dropdown-divider"></div>
                    <a class="dropdown-item" href="<%=request.getContextPath()%>/view/admin/pontoeletronico.jsp">
                        <i class="fas fa-fingerprint me-1"></i>Ponto Eletrônico
                    </a>
                    <div class="dropdown-divider"></div>
                    <a class="dropdown-item fw-semibold text-success" href="<%=request.getContextPath()%>/view/admin/Colaboradores.jsp?tipo=CLT">
                        <i class="fas fa-id-badge me-1 text-success"></i>CLT — Perfis e Fechamento
                    </a>
                </div>
            </li>

            <!-- Relatórios -->
            <li class="nav-item dropdown">
                <a class="nav-link dropdown-toggle" href="#" role="button"
                   data-bs-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                    <i class="fas fa-chart-bar me-1"></i>Relatório
                </a>
                <div class="dropdown-menu">
                    <a class="dropdown-item" href="<%=request.getContextPath()%>/RelatorioExportServlet?tipo=excel&modulo=funcionario">
                        <i class="fas fa-file-excel me-1 text-success"></i>Funcionário (Excel)
                    </a>
                    <a class="dropdown-item" href="<%=request.getContextPath()%>/RelatorioExportServlet?tipo=pdf&modulo=funcionario">
                        <i class="fas fa-file-pdf me-1 text-danger"></i>Funcionário (PDF)
                    </a>
                    <div class="dropdown-divider"></div>
                    <a class="dropdown-item" href="<%=request.getContextPath()%>/RelatorioExportServlet?tipo=excel&modulo=areaproducao">
                        <i class="fas fa-file-excel me-1 text-success"></i>Área de Produção (Excel)
                    </a>
                    <a class="dropdown-item" href="<%=request.getContextPath()%>/RelatorioExportServlet?tipo=pdf&modulo=areaproducao">
                        <i class="fas fa-file-pdf me-1 text-danger"></i>Área de Produção (PDF)
                    </a>
                </div>
            </li>

            <!-- Usuário -->
            <li class="nav-item">
                <a class="nav-link" href="<%=request.getContextPath()%>/LogoutServlet">
                    <i class="fas fa-sign-out-alt me-1"></i>Sair
                </a>
            </li>
        </ul>
    </div>
</nav>
