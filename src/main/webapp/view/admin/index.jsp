<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="pt-br">
<head>
    <title>Agro Tech One — Dashboard</title>
    <%@ include file="../../pagina/importacao.html" %>
</head>
<body>
<header><%@ include file="../../pagina/menu.jsp" %></header>

<div class="container-fluid content mt-3">

    <div class="bg-success text-white p-3 rounded mb-3">
        <h2 class="m-0"><i class="fas fa-tachometer-alt me-2"></i>Dashboard</h2>
    </div>

    <!-- KPI cards -->
    <div class="row g-3 mb-4">
        <div class="col-6 col-md-3">
            <div class="card shadow-sm border-start border-success border-4 h-100">
                <div class="card-body">
                    <div class="text-muted small">Funcionários Ativos</div>
                    <h3 class="fw-bold text-success mb-0" id="kpiFuncionarios">—</h3>
                    <i class="fas fa-users fa-lg text-success float-end mt-1"></i>
                </div>
            </div>
        </div>
        <div class="col-6 col-md-3">
            <div class="card shadow-sm border-start border-primary border-4 h-100">
                <div class="card-body">
                    <div class="text-muted small">Parceiros</div>
                    <h3 class="fw-bold text-primary mb-0" id="kpiParceiros">—</h3>
                    <i class="fas fa-handshake fa-lg text-primary float-end mt-1"></i>
                </div>
            </div>
        </div>
        <div class="col-6 col-md-3">
            <div class="card shadow-sm border-start border-warning border-4 h-100">
                <div class="card-body">
                    <div class="text-muted small">Alimentos Cadastrados</div>
                    <h3 class="fw-bold text-warning mb-0" id="kpiAlimentos">—</h3>
                    <i class="fas fa-apple-alt fa-lg text-warning float-end mt-1"></i>
                </div>
            </div>
        </div>
        <div class="col-6 col-md-3">
            <div class="card shadow-sm border-start border-info border-4 h-100">
                <div class="card-body">
                    <div class="text-muted small">Áreas Produtivas</div>
                    <h3 class="fw-bold text-info mb-0" id="kpiAreas">—</h3>
                    <i class="fas fa-map-marked-alt fa-lg text-info float-end mt-1"></i>
                </div>
            </div>
        </div>
    </div>

    <!-- Atalhos -->
    <div class="row g-3">
        <div class="col-md-4">
            <div class="card shadow-sm h-100">
                <div class="card-header bg-success text-white fw-semibold">
                    <i class="fas fa-users me-2"></i>RH
                </div>
                <div class="card-body d-flex flex-column gap-2">
                    <a href="<%=request.getContextPath()%>/view/admin/Colaboradores.jsp"
                       class="btn btn-outline-success btn-sm text-start">
                        <i class="fas fa-user-tie me-2"></i>Funcionários
                    </a>
                    <a href="<%=request.getContextPath()%>/view/admin/pontoeletronico.jsp"
                       class="btn btn-outline-success btn-sm text-start">
                        <i class="fas fa-fingerprint me-2"></i>Ponto Eletrônico
                    </a>
                    <a href="<%=request.getContextPath()%>/view/admin/folhapagamento.jsp"
                       class="btn btn-outline-success btn-sm text-start">
                        <i class="fas fa-money-bill-wave me-2"></i>Folha de Pagamento
                    </a>
                </div>
            </div>
        </div>
        <div class="col-md-4">
            <div class="card shadow-sm h-100">
                <div class="card-header bg-primary text-white fw-semibold">
                    <i class="fas fa-seedling me-2"></i>Produção
                </div>
                <div class="card-body d-flex flex-column gap-2">
                    <a href="<%=request.getContextPath()%>/view/admin/alimento.jsp"
                       class="btn btn-outline-primary btn-sm text-start">
                        <i class="fas fa-apple-alt me-2"></i>Alimentos
                    </a>
                    <a href="<%=request.getContextPath()%>/view/admin/areaproducao.jsp"
                       class="btn btn-outline-primary btn-sm text-start">
                        <i class="fas fa-map-marked-alt me-2"></i>Área de Produção
                    </a>
                </div>
            </div>
        </div>
        <div class="col-md-4">
            <div class="card shadow-sm h-100">
                <div class="card-header bg-secondary text-white fw-semibold">
                    <i class="fas fa-cog me-2"></i>Configurações
                </div>
                <div class="card-body d-flex flex-column gap-2">
                    <a href="<%=request.getContextPath()%>/view/admin/classificacao.jsp"
                       class="btn btn-outline-secondary btn-sm text-start">
                        <i class="fas fa-tags me-2"></i>Classificação
                    </a>
                    <a href="<%=request.getContextPath()%>/view/admin/despesascustos.jsp"
                       class="btn btn-outline-secondary btn-sm text-start">
                        <i class="fas fa-receipt me-2"></i>Custos e Despesas
                    </a>
                    <a href="<%=request.getContextPath()%>/view/admin/parceiro.jsp"
                       class="btn btn-outline-secondary btn-sm text-start">
                        <i class="fas fa-handshake me-2"></i>Parceiros
                    </a>
                </div>
            </div>
        </div>
    </div>

</div>

<%@ include file="../../pagina/footer.jsp" %>
</body>
</html>
