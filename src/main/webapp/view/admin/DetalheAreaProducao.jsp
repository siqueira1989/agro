<!DOCTYPE html>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html lang="pt-br">
<head>
  <meta charset="UTF-8">
  <title>Agro - Detalhe da Área</title>
  <%@ include file="../../pagina/importacao.html"%>
</head>
<body>
<header><%@ include file="../../pagina/menu.jsp"%></header>

<div class="container-fluid content mt-2" id="detalheArea">
  <div class="card shadow-sm mb-3">
    <div class="card-body bg-dark text-white rounded d-flex justify-content-between align-items-center">
      <h4 class="mb-0"><i class="fas fa-map-location-dot me-2"></i>Detalhe da Área de Produção</h4>
      <span id="detSituacao"></span>
    </div>
  </div>

  <!-- DADOS -->
  <div class="card shadow-sm mb-3">
    <div class="card-header bg-white"><h5 class="text-dark fw-bold mb-0">Propriedade</h5></div>
    <div class="card-body">
      <div class="row g-3">
        <div class="col-md-4"><small class="text-muted">Proprietário</small><div class="fw-semibold" id="detPropriet">—</div></div>
        <div class="col-md-4"><small class="text-muted">Propriedade</small><div class="fw-semibold" id="detProp">—</div></div>
        <div class="col-md-2"><small class="text-muted">Sigla</small><div class="fw-semibold" id="detSigla">—</div></div>
        <div class="col-md-2"><small class="text-muted">Qtd. Plantas</small><div class="fw-semibold" id="detQtd">—</div></div>
        <div class="col-md-3"><small class="text-muted">CEP</small><div class="fw-semibold" id="detCep">—</div></div>
        <div class="col-md-2"><small class="text-muted">Número</small><div class="fw-semibold" id="detNumero">—</div></div>
        <div class="col-md-7"><small class="text-muted">Complemento</small><div class="fw-semibold" id="detComplemento">—</div></div>
      </div>
    </div>
  </div>

  <!-- QUADRAS -->
  <div class="card shadow-sm mb-4">
    <div class="card-header bg-white"><h5 class="text-dark fw-bold mb-0"><i class="fas fa-th-large me-2"></i>Quadras</h5></div>
    <div class="card-body p-0">
      <div class="table-responsive">
        <table class="table table-sm table-striped mb-0 align-middle">
          <thead class="table-light"><tr><th>Nome</th><th class="text-end">Nº Plantas</th><th>Tipo de Planta</th><th>Situação</th></tr></thead>
          <tbody id="detQuadrasBody"></tbody>
        </table>
      </div>
    </div>
  </div>

  <div class="mb-5">
    <a href="${pageContext.request.contextPath}/view/admin/areaproducao.jsp" class="btn btn-secondary"><i class="fas fa-arrow-left me-1"></i>Voltar</a>
  </div>
</div>

<%@ include file="../../pagina/footer.jsp"%>
</body>
</html>
