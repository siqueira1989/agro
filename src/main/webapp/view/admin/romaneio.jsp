<!DOCTYPE html>
<%@ page contentType="text/html;charset=UTF-8" language="java"%>
<html lang="pt-br">
<head>
  <meta charset="UTF-8">
  <title>Agro - Romaneios</title>
  <%@ include file="../../pagina/importacao.html"%>
</head>
<body>
<header><%@ include file="../../pagina/menu.jsp"%></header>

<div class="container-fluid content mt-3">
  <div class="bg-success text-white p-3 rounded mb-3 d-flex flex-wrap gap-2 justify-content-between align-items-center">
    <h2 class="m-0"><i class="fas fa-truck me-2"></i>Romaneios</h2>
    <div class="d-flex gap-2">
      <button class="btn btn-light btn-sm" id="btnRmPagamento"><i class="fas fa-hand-holding-dollar me-1"></i>Registrar pagamento</button>
      <button class="btn btn-primary btn-sm" id="btnRmCadastrar"><i class="fas fa-plus-circle me-1"></i>Romaneio Cad.</button>
    </div>
  </div>
  <div id="alertaRomaneio" class="alert d-none" role="alert"></div>

  <div class="row g-3 mb-3">
    <div class="col-12 col-md-4"><div class="card shadow-sm border-0 h-100"><div class="card-body">
      <div class="small text-muted text-uppercase fw-semibold">Total de romaneios</div>
      <div class="fs-3 fw-bold" id="rmCardTotal">0</div>
      <div class="small text-muted" id="rmCardTotalSub">Venda bruta R$ 0,00 · líquida R$ 0,00</div>
    </div></div></div>
    <div class="col-12 col-md-4"><div class="card shadow-sm border-0 h-100 border-start border-warning border-4"><div class="card-body">
      <div class="small text-muted text-uppercase fw-semibold">Pendentes</div>
      <div class="fs-3 fw-bold text-warning" id="rmCardPendentes">0</div>
      <div class="small text-muted" id="rmCardPendentesSub">A receber R$ 0,00</div>
    </div></div></div>
    <div class="col-12 col-md-4"><div class="card shadow-sm border-0 h-100 border-start border-success border-4"><div class="card-body">
      <div class="small text-muted text-uppercase fw-semibold">Finalizados</div>
      <div class="fs-3 fw-bold text-success" id="rmCardFinalizados">0</div>
      <div class="small text-muted" id="rmCardFinalizadosSub">Recebido R$ 0,00</div>
    </div></div></div>
  </div>

  <div class="card shadow-sm border-0"><div class="card-body p-2">
    <div class="d-flex flex-wrap gap-2 mb-2 align-items-center">
      <label class="small text-muted" for="rmFiltroStatus">Situação</label>
      <select class="form-select form-select-sm w-auto" id="rmFiltroStatus">
        <option value="">Todas</option><option value="ABERTO">Aberto</option><option value="PENDENTE">Pendente</option>
        <option value="FINALIZADO">Finalizado</option><option value="CANCELADO">Cancelado</option>
      </select>
    </div>
    <div class="table-responsive">
      <table class="table table-sm table-bordered table-striped align-middle mb-0">
        <thead class="table-light small"><tr>
          <th>Número</th><th>Emissão</th><th>Parceiro</th><th>Área / Talhão</th><th>Alimento</th>
          <th class="text-end">Caixas</th><th class="text-end">Bruto</th><th class="text-end">Líquido</th>
          <th class="text-end">Pago</th><th>Situação</th><th class="text-end">Ações</th>
        </tr></thead>
        <tbody id="rmTabelaBody"></tbody>
      </table>
    </div>
  </div></div>
</div>

<!-- "Romaneio Cad.": escolha do lote (colheita Processada com caixas embaladas ainda não romaneadas) -->
<div class="modal fade" id="modalRmLote" tabindex="-1">
  <div class="modal-dialog"><div class="modal-content">
    <div class="modal-header bg-warning">
      <h5 class="modal-title"><i class="fas fa-truck me-2"></i>Novo romaneio</h5>
      <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
    </div>
    <div class="modal-body">
      <div id="rmLoteAlerta" class="alert d-none" role="alert"></div>
      <label class="form-label" for="rmSelLote">Colheita (lote processado) *</label>
      <select class="form-select" id="rmSelLote"></select>
      <small class="text-muted d-block mt-2" id="rmSelLoteInfo">Só aparecem os lotes Processados que ainda têm caixas embaladas sem romaneio.</small>
    </div>
    <div class="modal-footer">
      <button class="btn btn-secondary" data-bs-dismiss="modal"><i class="fas fa-times me-1"></i>Cancelar</button>
      <button class="btn btn-warning" id="btnRmContinuar">Continuar</button>
    </div>
  </div></div>
</div>

<!-- Pagamento em grupo: um recebimento do parceiro distribuído entre os romaneios PENDENTES dele -->
<div class="modal fade" id="modalRmPagamento" tabindex="-1">
  <div class="modal-dialog modal-xl modal-dialog-scrollable"><div class="modal-content">
    <div class="modal-header bg-success text-white">
      <h5 class="modal-title"><i class="fas fa-hand-holding-dollar me-2"></i>Registrar pagamento</h5>
      <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
    </div>
    <div class="modal-body bg-light">
      <div id="rpAlerta" class="alert d-none" role="alert"></div>
      <div class="card shadow-sm border-0 mb-3"><div class="card-body py-2">
        <div class="row g-2 align-items-end">
          <div class="col-12 col-md-4"><label class="form-label small mb-0" for="rpParceiro">Parceiro *</label>
            <select class="form-select form-select-sm" id="rpParceiro"></select></div>
          <div class="col-6 col-md-2"><label class="form-label small mb-0" for="rpData">Data *</label>
            <input type="date" class="form-control form-control-sm" id="rpData"></div>
          <div class="col-6 col-md-3"><label class="form-label small mb-0" for="rpForma">Forma *</label>
            <select class="form-select form-select-sm" id="rpForma">
              <option value="DINHEIRO">Dinheiro</option><option value="PIX">Pix</option>
              <option value="CHEQUE_PROPRIO">Cheque próprio</option><option value="CHEQUE_TERCEIRO">Cheque de terceiros</option>
            </select></div>
          <div class="col-6 col-md-3"><label class="form-label small mb-0" for="rpValor">Valor (R$) *</label>
            <input type="number" class="form-control form-control-sm" id="rpValor" min="0" step="0.01"></div>
        </div>
        <div class="row g-2 align-items-end mt-1 d-none" id="rpChequeCampos">
          <div class="col-6 col-md-3"><label class="form-label small mb-0" for="rpBanco">Banco *</label>
            <input type="text" class="form-control form-control-sm" id="rpBanco" maxlength="60"></div>
          <div class="col-6 col-md-3"><label class="form-label small mb-0" for="rpNumeroCheque">Nº do cheque *</label>
            <input type="text" class="form-control form-control-sm" id="rpNumeroCheque" maxlength="30"></div>
          <div class="col-6 col-md-4" id="rpEmitenteCol"><label class="form-label small mb-0" for="rpEmitente">Emitente *</label>
            <input type="text" class="form-control form-control-sm" id="rpEmitente" maxlength="80"></div>
          <div class="col-6 col-md-2"><label class="form-label small mb-0" for="rpBomPara">Bom para</label>
            <input type="date" class="form-control form-control-sm" id="rpBomPara"></div>
        </div>
        <div class="row g-2 mt-1">
          <div class="col-12"><label class="form-label small mb-0" for="rpObservacao">Observação</label>
            <input type="text" class="form-control form-control-sm" id="rpObservacao" maxlength="300"></div>
        </div>
      </div></div>
      <div class="card shadow-sm border-0"><div class="card-body p-2">
        <div class="d-flex flex-wrap justify-content-between align-items-center mb-2 gap-2">
          <div class="small text-muted text-uppercase fw-semibold">Romaneios pendentes do parceiro (do mais antigo ao mais novo)</div>
          <button type="button" class="btn btn-sm btn-outline-success" id="rpDistribuir"><i class="fas fa-arrows-down-to-line me-1"></i>Distribuir o valor</button>
        </div>
        <div class="table-responsive">
          <table class="table table-sm align-middle mb-0">
            <thead class="table-light small"><tr>
              <th>Número</th><th>Emissão</th><th>Talhão</th><th class="text-end">Líquido</th><th class="text-end">Pago</th>
              <th class="text-end">Saldo</th><th class="text-end" style="width:160px">Aplicar (R$)</th>
            </tr></thead>
            <tbody id="rpBody"></tbody>
            <tfoot class="table-light small fw-semibold"><tr>
              <td colspan="5" class="text-end">Distribuído</td>
              <td class="text-end" id="rpTotalSaldo">R$ 0,00</td>
              <td class="text-end" id="rpTotalDistribuido">R$ 0,00</td>
            </tr></tfoot>
          </table>
        </div>
        <div class="small mt-2" id="rpDiferenca"></div>
      </div></div>
    </div>
    <div class="modal-footer">
      <button class="btn btn-secondary" data-bs-dismiss="modal">Fechar</button>
      <button class="btn btn-success" id="rpConfirmar"><i class="fas fa-check me-1"></i>Confirmar pagamento</button>
    </div>
  </div></div>
</div>

<!-- Histórico de pagamentos de um romaneio (com estorno) -->
<div class="modal fade" id="modalRmHistorico" tabindex="-1">
  <div class="modal-dialog modal-lg modal-dialog-scrollable"><div class="modal-content">
    <div class="modal-header bg-success text-white">
      <h5 class="modal-title"><i class="fas fa-receipt me-2"></i>Pagamentos do romaneio <span id="rhNumero"></span></h5>
      <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
    </div>
    <div class="modal-body">
      <div id="rhAlerta" class="alert d-none" role="alert"></div>
      <div class="row g-2 mb-2 small" id="rhResumo"></div>
      <div class="table-responsive">
        <table class="table table-sm align-middle mb-0">
          <thead class="table-light small"><tr>
            <th>Data</th><th>Forma</th><th>Detalhe</th><th class="text-end">Valor total</th>
            <th class="text-end">Neste romaneio</th><th class="text-end">Ações</th>
          </tr></thead>
          <tbody id="rhBody"></tbody>
        </table>
      </div>
    </div>
    <div class="modal-footer"><button class="btn btn-secondary" data-bs-dismiss="modal">Fechar</button></div>
  </div></div>
</div>

<%@ include file="../../pagina/footer.jsp"%>
</body>
</html>
