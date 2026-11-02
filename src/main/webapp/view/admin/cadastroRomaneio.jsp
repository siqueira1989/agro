<!DOCTYPE html>
<%@ page contentType="text/html;charset=UTF-8" language="java"%>
<html lang="pt-br">
<head>
  <meta charset="UTF-8">
  <title>Agro - Romaneio</title>
  <%@ include file="../../pagina/importacao.html"%>
</head>
<body>
<header><%@ include file="../../pagina/menu.jsp"%></header>

<!-- Passo MR1 do Romaneio. Contrato de URL: ?lote=<idLote> (novo; a Colheita redireciona para cá ao
     finalizar um lote) ou ?id=<idromaneio> (edição). Os parâmetros são lidos pelo main.js. -->
<div class="container-fluid content mt-3" id="crPagina">
  <div class="bg-success text-white p-3 rounded mb-3 d-flex flex-wrap gap-2 justify-content-between align-items-center">
    <h2 class="m-0"><i class="fas fa-truck me-2"></i><span id="crTitulo">Novo romaneio</span></h2>
    <div class="d-flex gap-2 align-items-center">
      <span id="crStatusBadge"></span>
      <a class="btn btn-light btn-sm" href="<%=request.getContextPath()%>/view/admin/romaneio.jsp"><i class="fas fa-arrow-left me-1"></i>Romaneios</a>
    </div>
  </div>
  <div id="alertaCadRomaneio" class="alert d-none" role="alert"></div>

  <div class="row g-3">
    <div class="col-12 col-xl-8">
      <!-- Dados do lote (somente leitura) -->
      <div class="card shadow-sm border-0 mb-3"><div class="card-body py-2">
        <div class="small text-muted text-uppercase fw-semibold mb-1"><i class="fas fa-seedling me-1"></i>Colheita</div>
        <div class="row g-2 small">
          <div class="col-6 col-md-2"><div class="text-muted">Diário</div><div class="fw-semibold" id="crDiario">—</div></div>
          <div class="col-6 col-md-2"><div class="text-muted">Colheita</div><div class="fw-semibold" id="crDataColheita">—</div></div>
          <div class="col-6 col-md-3"><div class="text-muted">Área de produção</div><div class="fw-semibold" id="crArea">—</div></div>
          <div class="col-6 col-md-2"><div class="text-muted">Talhão</div><div class="fw-semibold" id="crTalhao">—</div></div>
          <div class="col-6 col-md-3"><div class="text-muted">Alimento</div><div class="fw-semibold" id="crAlimento">—</div></div>
          <div class="col-12"><div class="text-muted d-inline">Safra:</div> <span class="fw-semibold" id="crSafra">—</span></div>
        </div>
      </div></div>

      <!-- Venda -->
      <div class="card shadow-sm border-0 mb-3"><div class="card-body py-2">
        <div class="small text-muted text-uppercase fw-semibold mb-1"><i class="fas fa-handshake me-1"></i>Venda</div>
        <div class="row g-2 align-items-end">
          <div class="col-12 col-md-5"><label class="form-label small mb-0" for="crParceiro">Parceiro comprador *</label>
            <select class="form-select form-select-sm" id="crParceiro"></select></div>
          <div class="col-6 col-md-3"><label class="form-label small mb-0" for="crDataEmissao">Data de emissão *</label>
            <input type="date" class="form-control form-control-sm" id="crDataEmissao"></div>
          <div class="col-6 col-md-4"></div>
          <div class="col-8 col-md-5"><label class="form-label small mb-0" for="crMotorista">Motorista</label>
            <input type="text" class="form-control form-control-sm" id="crMotorista" maxlength="80"></div>
          <div class="col-4 col-md-3"><label class="form-label small mb-0" for="crPlaca">Placa do caminhão</label>
            <input type="text" class="form-control form-control-sm text-uppercase" id="crPlaca" maxlength="10" placeholder="ABC1D23"></div>
          <div class="col-12 col-md-4"><label class="form-label small mb-0" for="crObservacao">Observação</label>
            <input type="text" class="form-control form-control-sm" id="crObservacao" maxlength="300"></div>
        </div>
      </div></div>

      <!-- Itens: classificação × embalagem -->
      <div class="card shadow-sm border-0 mb-3">
        <div class="card-header bg-white text-dark d-flex flex-wrap gap-2 justify-content-between align-items-center">
          <span class="small text-muted text-uppercase fw-semibold"><i class="fas fa-boxes-stacked me-1"></i>Caixas por classificação e embalagem</span>
          <span class="small text-muted">Preço vazio = romaneio fica <b>Aberto</b>; com todos os preços fica <b>Pendente</b>.</span>
        </div>
        <div class="card-body p-0">
          <div class="table-responsive">
            <table class="table table-sm align-middle mb-0">
              <thead class="table-light small"><tr>
                <th>Classificação</th><th>Embalagem</th><th class="text-end" style="width:130px">Caixas</th>
                <th class="text-end" style="width:150px">Preço unit. (R$/cx)</th><th class="text-end" style="width:140px">Valor</th>
              </tr></thead>
              <tbody id="crItensBody"></tbody>
              <tfoot class="table-light small fw-semibold"><tr>
                <td colspan="2" class="text-end">Total</td>
                <td class="text-end" id="crTotalCaixas">0</td><td></td>
                <td class="text-end" id="crTotalBruto">R$ 0,00</td>
              </tr></tfoot>
            </table>
          </div>
        </div>
      </div>

      <!-- Despesas de venda -->
      <div class="card shadow-sm border-0 mb-3">
        <div class="card-header bg-white text-dark d-flex flex-wrap gap-2 justify-content-between align-items-center">
          <span class="small text-muted text-uppercase fw-semibold"><i class="fas fa-receipt me-1"></i>Despesas de venda</span>
          <div class="d-flex gap-2">
            <select class="form-select form-select-sm" id="crDespesaIncluir" style="min-width:220px"></select>
            <button type="button" class="btn btn-primary btn-sm text-nowrap" id="crDespesaIncluirBtn"><i class="fas fa-plus-circle me-1"></i>Incluir</button>
          </div>
        </div>
        <div class="card-body p-0">
          <div class="table-responsive">
            <table class="table table-sm align-middle mb-0">
              <thead class="table-light small"><tr>
                <th>Despesa</th><th>Cálculo</th><th class="text-end" style="width:170px">Parâmetro</th>
                <th class="text-end" style="width:150px">Valor</th><th style="width:40px"></th>
              </tr></thead>
              <tbody id="crDespesasBody"></tbody>
              <tfoot class="table-light small fw-semibold"><tr>
                <td colspan="3" class="text-end">Total de despesas</td>
                <td class="text-end" id="crTotalDespesas">R$ 0,00</td><td></td>
              </tr></tfoot>
            </table>
          </div>
        </div>
      </div>
    </div>

    <div class="col-12 col-xl-4">
      <!-- Saldo de caixas por embalagem -->
      <div class="card shadow-sm border-0 mb-3">
        <div class="card-header bg-white text-dark small text-muted text-uppercase fw-semibold"><i class="fas fa-scale-balanced me-1"></i>Caixas embaladas do lote</div>
        <div class="card-body p-0">
          <table class="table table-sm align-middle mb-0 small">
            <thead class="table-light"><tr>
              <th>Embalagem</th><th class="text-end">Embaladas</th><th class="text-end">Outros rom.</th>
              <th class="text-end">Este</th><th class="text-end">Saldo</th>
            </tr></thead>
            <tbody id="crSaldoBody"></tbody>
          </table>
          <div class="p-2 small" id="crSaldoInfo"></div>
        </div>
      </div>

      <!-- Resumo -->
      <div class="card shadow-sm border-0 mb-3"><div class="card-body">
        <div class="small text-muted text-uppercase fw-semibold mb-2"><i class="fas fa-calculator me-1"></i>Resumo</div>
        <div class="d-flex justify-content-between"><span>Venda bruta</span><span class="fw-semibold" id="crResBruto">R$ 0,00</span></div>
        <div class="d-flex justify-content-between text-danger"><span>(−) Despesas de venda</span><span class="fw-semibold" id="crResDespesas">R$ 0,00</span></div>
        <hr class="my-2">
        <div class="d-flex justify-content-between fs-5"><span>Venda líquida</span><span class="fw-bold" id="crResLiquido">R$ 0,00</span></div>
        <div class="d-flex justify-content-between small text-muted mt-1"><span>Já pago</span><span id="crResPago">R$ 0,00</span></div>
        <div class="d-flex justify-content-between small mt-1"><span>Situação ao salvar</span><span id="crResStatus"></span></div>
        <button type="button" class="btn btn-success w-100 mt-3" id="crSalvar"><i class="fas fa-check me-1"></i>Salvar romaneio</button>
      </div></div>
    </div>
  </div>
</div>

<%@ include file="../../pagina/footer.jsp"%>
</body>
</html>
