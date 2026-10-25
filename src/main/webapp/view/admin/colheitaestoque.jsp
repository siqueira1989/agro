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

  <!-- Processar lote (onda 3): baixa do saldo + diário de Processamento (embaladores por caixa e
       demais funcionários por horas) + preço de cada embalagem gravado em Despesas e Custos. -->
  <div class="modal fade" id="modalProcessar" tabindex="-1" data-bs-backdrop="static">
    <div class="modal-dialog modal-xl modal-dialog-scrollable"><div class="modal-content">
      <div class="modal-header bg-primary text-white">
        <h5 class="modal-title"><i class="fas fa-box-open me-2"></i>Processar lote <span id="cpTituloLote"></span></h5>
        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
      </div>
      <div class="modal-body bg-light">
        <div id="cpAlerta" class="alert d-none" role="alert"></div>

        <div class="row g-2 mb-3">
          <div class="col-md-7">
            <div class="card shadow-sm h-100 border-0"><div class="card-body py-2">
              <div class="small text-muted text-uppercase fw-semibold mb-1"><i class="fas fa-seedling me-1"></i>Lote</div>
              <div class="row g-2 small">
                <div class="col-6 col-lg-3"><div class="text-muted">Talhão</div><div class="fw-semibold" id="cpTalhao">—</div></div>
                <div class="col-6 col-lg-3"><div class="text-muted">Alimento</div><div class="fw-semibold" id="cpAlimento">—</div></div>
                <div class="col-6 col-lg-3"><div class="text-muted">Área</div><div class="fw-semibold" id="cpArea">—</div></div>
                <div class="col-6 col-lg-3"><div class="text-muted">Colheita</div><div class="fw-semibold" id="cpColheita">—</div></div>
              </div>
              <div class="small mt-2" id="cpHistorico"></div>
            </div></div>
          </div>
          <div class="col-md-5">
            <div class="card shadow-sm h-100 border-0"><div class="card-body py-2">
              <div class="d-flex justify-content-between align-items-center mb-1">
                <span class="small text-muted text-uppercase fw-semibold"><i class="fas fa-warehouse me-1"></i>Saldo disponível</span>
                <span class="badge bg-warning text-dark fs-6" id="cpSaldo">0</span>
              </div>
              <div class="row g-2">
                <div class="col-6">
                  <label class="form-label small mb-0" for="cpData">Data *</label>
                  <input type="date" class="form-control form-control-sm" id="cpData">
                </div>
                <div class="col-6">
                  <label class="form-label small mb-0" for="cpQuantidade">Quantidade processada *</label>
                  <div class="input-group input-group-sm">
                    <input type="number" class="form-control" id="cpQuantidade" min="0" step="0.01">
                    <button class="btn btn-outline-secondary" type="button" id="cpUsarSaldo" title="Processar todo o saldo">Tudo</button>
                  </div>
                </div>
              </div>
              <div class="progress mt-2" style="height:6px" title="Saldo que fica em estoque"><div class="progress-bar bg-success" id="cpBarra" style="width:0%"></div></div>
              <div class="small text-muted mt-1" id="cpRestante"></div>
            </div></div>
          </div>
        </div>

        <div class="card shadow-sm border-0">
          <div class="card-header bg-white d-flex flex-wrap gap-2 justify-content-between align-items-center">
            <span class="fw-semibold"><i class="fas fa-people-carry-box me-1"></i>Funcionários da etapa</span>
            <div class="d-flex gap-2">
              <button type="button" class="btn btn-sm btn-primary" id="cpAddEmbalador"><i class="fas fa-plus me-1"></i>Embalador</button>
              <button type="button" class="btn btn-sm btn-outline-primary" id="cpAddOutro"><i class="fas fa-plus me-1"></i>Outro funcionário (horas)</button>
            </div>
          </div>
          <div class="card-body p-0">
            <div class="table-responsive">
              <table class="table table-sm align-middle mb-0">
                <thead class="table-light small"><tr>
                  <th style="min-width:180px">Funcionário</th><th>Tipo</th><th>Regime</th>
                  <th style="min-width:200px">Embalagem</th><th style="width:110px" class="text-end">Preço</th>
                  <th style="width:90px" class="text-end">Caixas</th><th style="width:90px" class="text-end">Horas</th>
                  <th style="width:110px" class="text-end">Valor</th><th style="width:40px"></th>
                </tr></thead>
                <tbody id="cpFuncBody"></tbody>
              </table>
            </div>
            <div class="text-center text-muted small py-3" id="cpVazio">Adicione os embaladores (pagos por caixa) e, se houver, os outros funcionários (por horas).</div>
          </div>
          <div class="card-footer bg-white">
            <div class="row g-2 text-center small">
              <div class="col-6 col-md-3"><div class="text-muted">Caixas embaladas</div><div class="fw-bold fs-5" id="cpTotCaixas">0</div></div>
              <div class="col-6 col-md-3"><div class="text-muted">Embaladores</div><div class="fw-bold fs-5" id="cpTotEmb">R$ 0,00</div></div>
              <div class="col-6 col-md-3"><div class="text-muted">Por horas</div><div class="fw-bold fs-5" id="cpTotHoras">R$ 0,00</div></div>
              <div class="col-6 col-md-3"><div class="text-muted">Total da etapa</div><div class="fw-bold fs-5 text-primary" id="cpTotGeral">R$ 0,00</div></div>
            </div>
          </div>
        </div>

        <div class="row g-2 mt-2">
          <div class="col-md-5">
            <label class="form-label small mb-0" for="cpResponsavel">Responsável pelo diário *</label>
            <select class="form-select form-select-sm" id="cpResponsavel"><option value="">Selecione</option></select>
          </div>
          <div class="col-md-7 d-flex align-items-end">
            <small class="text-muted"><i class="fas fa-circle-info me-1"></i>Ao confirmar é criado um diário de <strong>Processamento</strong> no talhão do lote e os preços das embalagens ficam gravados em Despesas e Custos.</small>
          </div>
        </div>
      </div>
      <div class="modal-footer">
        <button class="btn btn-secondary" data-bs-dismiss="modal">Cancelar</button>
        <button class="btn btn-primary" id="cpConfirmar"><i class="fas fa-check me-1"></i>Confirmar processamento</button>
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
