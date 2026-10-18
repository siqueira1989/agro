<!DOCTYPE html>
<%@ page contentType="text/html;charset=UTF-8" language="java"%>
<html lang="pt-br">
<head>
  <meta charset="UTF-8">
  <title>Agro - Colheita e Estoque</title>
  <%@ include file="../../pagina/importacao.html"%>
</head>
<body>
<header><%@ include file="../../pagina/menu.jsp"%></header>

<div class="container-fluid content mt-3">
  <div class="bg-success text-white p-3 rounded mb-3 d-flex justify-content-between align-items-center">
    <h2 class="m-0"><i class="fas fa-boxes-stacked me-2"></i>Colheita e Estoque</h2>
    <button class="btn btn-warning btn-sm" id="btnCadastrarColheita"><i class="fas fa-basket-shopping me-1"></i>Cadastrar Colheita</button>
  </div>
  <div id="alertaColheita" class="alert d-none" role="alert"></div>

  <!-- Escolha da área: o cadastro em si acontece no Diário de Campo daquela área (evita duplicar
       toda a lógica de talhão/funcionário/máquina/safra que já existe em DetalheAreaProducao.jsp). -->
  <div class="modal fade" id="modalEscolherArea" tabindex="-1">
    <div class="modal-dialog"><div class="modal-content">
      <div class="modal-header bg-warning">
        <h5 class="modal-title"><i class="fas fa-map-location-dot me-2"></i>Cadastrar Colheita</h5>
        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
      </div>
      <div class="modal-body">
        <label class="form-label">Área de Produção *</label>
        <select class="form-select" id="ceSelecionarArea"></select>
        <small class="text-muted d-block mt-2">O cadastro da colheita (funcionários, regime, máquinas) é feito no Diário de Campo da área escolhida.</small>
      </div>
      <div class="modal-footer">
        <button class="btn btn-secondary" data-bs-dismiss="modal">Cancelar</button>
        <button class="btn btn-warning" id="btnIrParaColheita">Continuar</button>
      </div>
    </div></div>
  </div>

  <div class="row g-2 mb-3">
    <div class="col-6 col-md-3">
      <div class="card shadow-sm h-100"><div class="card-body text-center">
        <small class="text-muted">Total Colhido (geral)</small>
        <div class="fs-4 fw-bold text-success" id="ceCardTotal">0</div>
      </div></div>
    </div>
    <div class="col-6 col-md-3">
      <div class="card shadow-sm h-100"><div class="card-body text-center">
        <small class="text-muted">Em Estoque</small>
        <div class="fs-4 fw-bold text-warning" id="ceCardEstoque">0</div>
      </div></div>
    </div>
    <div class="col-6 col-md-3">
      <div class="card shadow-sm h-100"><div class="card-body text-center">
        <small class="text-muted">Processado</small>
        <div class="fs-4 fw-bold text-primary" id="ceCardProcessado">0</div>
      </div></div>
    </div>
    <div class="col-6 col-md-3">
      <div class="card shadow-sm h-100"><div class="card-body text-center">
        <small class="text-muted">Finalizado</small>
        <div class="fs-4 fw-bold text-secondary" id="ceCardFinalizado">0</div>
      </div></div>
    </div>
  </div>

  <div class="card shadow-sm"><div class="card-body p-2">
    <div class="d-flex justify-content-between align-items-center mb-2">
      <div class="input-group" style="max-width:260px">
        <span class="input-group-text"><i class="fas fa-filter"></i></span>
        <select class="form-select" id="ceFiltroStatus">
          <option value="">Todos os status</option>
          <option value="COLHEIDO">Colhido</option>
          <option value="ESTOQUE">Em estoque</option>
          <option value="PROCESSADO">Processado</option>
          <option value="FINALIZADO">Finalizado</option>
        </select>
      </div>
    </div>
    <div class="table-responsive">
      <table class="table table-sm table-bordered table-striped align-middle">
        <thead class="table-light"><tr>
          <th>Diário</th><th>Data</th><th>Talhão</th><th>Siglas</th><th>Cultura</th>
          <th class="text-end">Qtd. Total</th><th class="text-end">Processado</th><th class="text-end">Saldo</th>
          <th>Status</th><th class="text-end">Ações</th>
        </tr></thead>
        <tbody id="ceTabelaBody"></tbody>
      </table>
    </div>
  </div></div>
</div>

<%@ include file="../../pagina/footer.jsp"%>
</body>
</html>
