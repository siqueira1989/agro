<!DOCTYPE html>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html lang="pt-br">
<head>
  <meta charset="UTF-8">
  <title>Agro - Atualizar Área de Produção</title>
  <%@ include file="../../pagina/importacao.html"%>
</head>
<body>
<header><%@ include file="../../pagina/menu.jsp"%></header>

<div class="container-fluid content mt-2">
  <div class="card shadow-sm mb-3">
    <div class="card-body bg-primary text-white rounded">
      <h4 class="mb-0"><i class="fas fa-pen-to-square me-2"></i>Atualizar Área de Produção</h4>
    </div>
  </div>

  <div id="alertFormAreaEdit" class="alert d-none" role="alert"></div>
  <input type="hidden" id="areaId">

  <!-- FORMULÁRIO -->
  <form id="formAtualizarArea">
    <div class="card shadow-sm mb-3">
      <div class="card-header bg-white"><h5 class="text-primary fw-bold mb-0"><i class="fas fa-map-marked-alt me-2"></i>Propriedade</h5></div>
      <div class="card-body">
        <div class="row g-2">
          <div class="col-md-6"><label class="form-label">Proprietário *</label>
            <input type="text" class="form-control" id="proprietEdit" maxlength="50"></div>
          <div class="col-md-6"><label class="form-label">Propriedade *</label>
            <input type="text" class="form-control" id="propEdit" maxlength="50"></div>
        </div>
        <div class="row g-2 mt-1">
          <div class="col-md-3"><label class="form-label">CEP</label>
            <input type="text" class="form-control" id="cepAreaEdit" placeholder="00000-000"></div>
          <div class="col-md-5"><label class="form-label">Endereço</label>
            <input type="text" class="form-control" id="logradouro" readonly></div>
          <div class="col-md-2"><label class="form-label">Número</label>
            <input type="number" class="form-control" id="numeroAreaEdit" min="0"></div>
          <div class="col-md-2"><label class="form-label">UF</label>
            <input type="text" class="form-control" id="uf" readonly></div>
        </div>
        <div class="row g-2 mt-1">
          <div class="col-md-4"><label class="form-label">Bairro</label>
            <input type="text" class="form-control" id="bairro" readonly></div>
          <div class="col-md-4"><label class="form-label">Cidade</label>
            <input type="text" class="form-control" id="cidade" readonly></div>
          <div class="col-md-4"><label class="form-label">Complemento</label>
            <input type="text" class="form-control" id="complementoAreaEdit" maxlength="50"></div>
        </div>
        <div class="row g-2 mt-1">
          <div class="col-md-3"><label class="form-label">Siglas * (máx. 4)</label>
            <input type="text" class="form-control" id="siglaEdit" maxlength="4"></div>
          <div class="col-md-3"><label class="form-label">Qtd. Plantas (das quadras)</label>
            <input type="number" class="form-control bg-light" id="qtdEdit" readonly></div>
        </div>
      </div>
    </div>
    <div class="text-end mb-3">
      <a href="${pageContext.request.contextPath}/view/admin/areaproducao.jsp" class="btn btn-secondary"><i class="fas fa-arrow-left me-1"></i>Voltar</a>
      <button type="button" class="btn btn-primary" id="btnAtualizarArea"><i class="fas fa-save me-1"></i>Atualizar</button>
    </div>
  </form>

  <!-- QUADRAS DA ÁREA -->
  <div class="card shadow-sm mb-3">
    <div class="card-header bg-white"><h5 class="text-primary fw-bold mb-0"><i class="fas fa-th-large me-2"></i>Quadras</h5></div>
    <div class="card-body">
      <div id="alertQuadra" class="alert d-none" role="alert"></div>
      <div class="table-responsive mb-3">
        <table class="table table-sm table-hover align-middle mb-0">
          <thead class="table-light"><tr><th>Nome</th><th class="text-end">Nº Plantas</th><th class="text-end">Área (ha)</th><th>Tipo de Planta</th><th>Situação</th><th style="width:130px;"></th></tr></thead>
          <tbody id="quadrasEditBody"></tbody>
        </table>
      </div>
      <!-- Adicionar nova quadra -->
      <div class="border-top pt-3">
        <h6 class="fw-bold text-success"><i class="fas fa-plus me-1"></i>Adicionar Quadra</h6>
        <div class="row g-2 align-items-end">
          <div class="col-md-3"><label class="form-label">Nome da quadra</label>
            <input type="text" class="form-control" id="addQuadraNome" maxlength="60"></div>
          <div class="col-md-2"><label class="form-label">Nº de plantas</label>
            <input type="number" class="form-control" id="addQuadraPlantas" min="0" value="0"></div>
          <div class="col-md-2"><label class="form-label">Área (ha)</label>
            <input type="number" class="form-control" id="addQuadraArea" min="0" step="0.01" value="0"></div>
          <div class="col-md-3"><label class="form-label">Tipo de planta</label>
            <select class="form-select" id="addQuadraAlimento"><option value="">Selecione</option></select></div>
          <div class="col-md-2">
            <button type="button" class="btn btn-success w-100" id="btnAddQuadraEdit"><i class="fas fa-plus me-1"></i>Adicionar</button></div>
        </div>
      </div>
    </div>
  </div>
</div>

<%@ include file="../../pagina/footer.jsp"%>
</body>
</html>
