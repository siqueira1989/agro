<!DOCTYPE html>
<%@ page contentType="text/html;charset=UTF-8" language="java"%>
<html lang="pt-br">
<head>
  <meta charset="UTF-8">
  <title>Agro - Safras</title>
  <%@ include file="../../pagina/importacao.html"%>
</head>
<body>
<header><%@ include file="../../pagina/menu.jsp"%></header>

<div class="container-fluid content mt-3">
  <div class="bg-success text-white p-3 rounded mb-3 d-flex justify-content-between align-items-center">
    <h2 class="m-0"><i class="fas fa-seedling me-2"></i>Safras</h2>
    <button class="btn btn-light btn-sm" id="btnNovaSafra"><i class="fas fa-plus me-1"></i>Nova Safra</button>
  </div>
  <div id="alerta" class="alert d-none" role="alert"></div>

  <div class="card shadow-sm"><div class="card-body p-2">
    <div class="table-responsive">
      <table id="tabelaSafras" class="table table-bordered table-striped" style="width:100%">
        <thead class="thead-green"><tr>
          <th>Safra</th><th>Cultura</th><th>Período</th><th>Status</th>
          <th class="text-end">Estimativa</th><th class="text-end">Ações</th>
        </tr></thead>
        <tbody></tbody>
      </table>
    </div>
  </div></div>
</div>

<!-- MODAL Cadastro/Edição -->
<div class="modal fade" id="modalSafra" tabindex="-1"><div class="modal-dialog modal-xl"><div class="modal-content">
  <div class="modal-header bg-success text-white">
    <h5 class="modal-title" id="modalSafraTitulo"><i class="fas fa-seedling me-2"></i>Nova Safra</h5>
    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
  </div>
  <div class="modal-body">
    <div id="alertaSafra" class="alert d-none"></div>
    <input type="hidden" id="sfId">
    <div class="row g-2">
      <div class="col-md-5"><label class="form-label">Nome *</label><input class="form-control" id="sfNome" maxlength="80" placeholder="Ex.: Soja 2026/2027"></div>
      <div class="col-md-4"><label class="form-label">Cultura Principal</label><select class="form-select" id="sfCultura"><option value="">—</option></select></div>
      <div class="col-md-3"><label class="form-label">Status</label>
        <select class="form-select" id="sfStatus">
          <option value="PLANEJADA">Planejada</option><option value="EM_ANDAMENTO">Em andamento</option><option value="FINALIZADA">Finalizada</option>
        </select></div>
    </div>
    <div class="row g-2 mt-1">
      <div class="col-md-3"><label class="form-label">Data Inicial *</label><input type="date" class="form-control" id="sfDataIni"></div>
      <div class="col-md-3"><label class="form-label">Data Final *</label><input type="date" class="form-control" id="sfDataFim"></div>
      <div class="col-md-3"><label class="form-label">Estimativa Produção</label><input type="number" step="0.001" class="form-control" id="sfEstimativa" value="0"></div>
      <div class="col-md-3"><label class="form-label">Unidade</label>
        <select class="form-select" id="sfUnidade"><option value="SACA">Saca</option><option value="TONELADA">Tonelada</option></select></div>
    </div>
    <div class="row g-2 mt-1">
      <div class="col-md-3"><label class="form-label">Despesas Fixas (R$)</label><input type="number" step="0.01" class="form-control" id="sfDespFixas" value="0"></div>
    </div>

    <div class="d-flex justify-content-between align-items-center mt-3 mb-1">
      <h6 class="fw-bold text-secondary mb-0"><i class="fas fa-th-large me-1"></i>Talhões da Safra</h6>
      <button type="button" class="btn btn-outline-success btn-sm" id="btnAddTalhaoSafra"><i class="fas fa-plus me-1"></i>Adicionar talhão</button>
    </div>
    <div class="table-responsive"><table class="table table-sm align-middle mb-0">
      <thead class="table-light"><tr><th style="width:55%">Talhão</th><th>Área destinada (ha)</th><th></th></tr></thead>
      <tbody id="sfTalhaoBody"></tbody></table></div>
    <small class="text-muted">A área destinada não pode exceder a área real (ha) cadastrada no talhão.</small>
  </div>
  <div class="modal-footer">
    <button class="btn btn-secondary" data-bs-dismiss="modal">Cancelar</button>
    <button class="btn btn-success" id="btnSalvarSafra"><i class="fas fa-save me-1"></i>Salvar</button>
  </div>
</div></div></div>

<%@ include file="../../pagina/footer.jsp"%>
</body>
</html>
