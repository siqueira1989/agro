<!DOCTYPE html>
<%@ page contentType="text/html;charset=UTF-8" language="java"%>
<html lang="pt-br">
<head>
  <meta charset="UTF-8">
  <title>Agro - Resumo Financeiro da Safra</title>
  <%@ include file="../../pagina/importacao.html"%>
</head>
<body>
<header><%@ include file="../../pagina/menu.jsp"%></header>

<div class="container-fluid content mt-3" id="resumoSafra">
  <div class="bg-dark text-white p-3 rounded mb-3 d-flex justify-content-between align-items-center">
    <h2 class="m-0"><i class="fas fa-chart-pie me-2"></i>Resumo Financeiro — <span id="rsNome"></span></h2>
    <span id="rsStatus"></span>
  </div>

  <div class="row g-2 mb-1 text-muted small">
    <div class="col-md-3">Cultura: <strong id="rsCultura">—</strong></div>
    <div class="col-md-3">Período: <strong id="rsPeriodo">—</strong></div>
    <div class="col-md-2">Atividades: <strong id="rsAtiv">0</strong></div>
    <div class="col-md-2">Plantas: <strong id="rsPlantas">0</strong></div>
    <div class="col-md-2">Área: <strong id="rsArea">0</strong> ha</div>
  </div>

  <!-- CARDS -->
  <div class="row g-3 my-1">
    <div class="col-md-3"><div class="card shadow-sm border-0 bg-dark text-white"><div class="card-body text-center py-3">
      <div class="fs-4 fw-bold" id="rsCustoTotal">R$ 0,00</div><div class="small">Custo Total Acumulado</div></div></div></div>
    <div class="col-md-3"><div class="card shadow-sm border-0 bg-primary text-white"><div class="card-body text-center py-3">
      <div class="fs-4 fw-bold" id="rsPorPlanta">R$ 0,00</div><div class="small">Custo por Planta</div></div></div></div>
    <div class="col-md-3"><div class="card shadow-sm border-0 bg-info text-white"><div class="card-body text-center py-3">
      <div class="fs-4 fw-bold" id="rsPorHa">R$ 0,00</div><div class="small">Custo por Hectare</div></div></div></div>
    <div class="col-md-3"><div class="card shadow-sm border-0 bg-success text-white"><div class="card-body text-center py-3">
      <div class="fs-4 fw-bold" id="rsPorSaca">R$ 0,00</div><div class="small">Custo Estimado por <span id="rsUnidade">Saca</span></div></div></div></div>
  </div>

  <div class="row g-3">
    <!-- Composição de custos (COE) -->
    <div class="col-12"><div class="card shadow-sm">
      <div class="card-header bg-white fw-bold"><i class="fas fa-layer-group me-2"></i>Composição do Custo (COE + Fixas)</div>
      <div class="card-body">
        <table class="table table-sm">
          <thead class="table-light"><tr><th>Componente</th><th class="text-end">Valor</th><th class="text-end">%</th></tr></thead>
          <tbody>
            <tr><td><i class="fas fa-flask text-primary me-1"></i>Insumos</td><td class="text-end" id="rsIns">R$ 0,00</td><td class="text-end" id="rsPctIns">0%</td></tr>
            <tr><td><i class="fas fa-tractor text-secondary me-1"></i>Maquinário/Combustível</td><td class="text-end" id="rsMaq">R$ 0,00</td><td class="text-end" id="rsPctMaq">0%</td></tr>
            <tr><td><i class="fas fa-users text-success me-1"></i>Mão de Obra</td><td class="text-end" id="rsMo">R$ 0,00</td><td class="text-end" id="rsPctMo">0%</td></tr>
            <tr><td><i class="fas fa-file-invoice-dollar text-warning me-1"></i>Despesas Fixas</td><td class="text-end" id="rsFix">R$ 0,00</td><td class="text-end" id="rsPctFix">0%</td></tr>
          </tbody>
          <tfoot><tr class="fw-bold"><td>Custo Total</td><td class="text-end" id="rsTotalFoot">R$ 0,00</td><td class="text-end">100%</td></tr></tfoot>
        </table>
        <div class="progress" style="height:22px;" id="rsBarra"></div>
      </div>
    </div></div>

    <!-- Rateio por talhão -->
    <div class="col-12"><div class="card shadow-sm">
      <div class="card-header bg-white d-flex justify-content-between align-items-center flex-wrap gap-2">
        <span class="fw-bold"><i class="fas fa-balance-scale me-2"></i>Rateio Proporcional por Talhão</span>
        <div class="d-flex align-items-center gap-2">
          <label class="form-label small mb-0 text-muted">Alimento:</label>
          <select class="form-select form-select-sm" id="rsFiltroAlimento" style="width:180px;">
            <option value="">Todos</option>
          </select>
        </div>
      </div>
      <div class="card-body p-0"><div class="table-responsive">
        <table class="table table-sm table-striped mb-0">
          <thead class="table-light"><tr><th>Talhão</th><th>Alimento</th><th class="text-end">Plantas</th><th class="text-end">Part.%</th><th class="text-end">Custo Rateado</th><th class="text-end">Custo Real</th></tr></thead>
          <tbody id="rsRateioBody"></tbody>
          <tfoot><tr class="fw-bold"><td colspan="4">Total (filtro atual)</td><td class="text-end" id="rsFiltroRateadoTotal">R$ 0,00</td><td class="text-end" id="rsFiltroRealTotal">R$ 0,00</td></tr></tfoot>
        </table>
      </div></div>
    </div></div>
  </div>

  <div class="my-4">
    <a href="${pageContext.request.contextPath}/view/admin/safra.jsp" class="btn btn-secondary"><i class="fas fa-arrow-left me-1"></i>Voltar</a>
  </div>
</div>

<%@ include file="../../pagina/footer.jsp"%>
</body>
</html>
