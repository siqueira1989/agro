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
    <div class="d-flex gap-2">
      <button class="btn btn-light btn-sm" id="btnEmbalagens"><i class="fas fa-box me-1"></i>Embalagens</button>
      <button class="btn btn-primary btn-sm" id="btnCadastrarColheita"><i class="fas fa-plus-circle me-1"></i>Cadastrar Colheita</button>
    </div>
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
        <button class="btn btn-secondary" data-bs-dismiss="modal"><i class="fas fa-times me-1"></i>Cancelar</button>
        <button class="btn btn-warning" id="btnIrParaColheita">Continuar</button>
      </div>
    </div></div>
  </div>

  <!-- Cadastro de embalagens (V31): por alimento + área, preço de compra e preço pago ao embalador. -->
  <div class="modal fade" id="modalEmbalagens" tabindex="-1">
    <div class="modal-dialog modal-xl modal-dialog-scrollable"><div class="modal-content">
      <div class="modal-header bg-success text-white">
        <h5 class="modal-title"><i class="fas fa-box me-2"></i>Embalagens</h5>
        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
      </div>
      <div class="modal-body bg-light">
        <div id="emAlerta" class="alert d-none" role="alert"></div>
        <div class="card shadow-sm border-0 mb-3"><div class="card-body py-2">
          <div class="small text-muted text-uppercase fw-semibold mb-1" id="emFormTitulo"><i class="fas fa-plus me-1"></i>Nova embalagem</div>
          <input type="hidden" id="emId" value="0">
          <div class="row g-2 align-items-end">
            <div class="col-6 col-md-3"><label class="form-label small mb-0" for="emAlimento">Alimento *</label>
              <select class="form-select form-select-sm" id="emAlimento"></select></div>
            <div class="col-6 col-md-3"><label class="form-label small mb-0" for="emArea">Área de produção *</label>
              <select class="form-select form-select-sm" id="emArea"></select></div>
            <div class="col-8 col-md-4"><label class="form-label small mb-0" for="emDescricao">Descrição *</label>
              <input type="text" class="form-control form-control-sm" id="emDescricao" maxlength="80" placeholder="Ex.: Papelão 7kg"></div>
            <div class="col-4 col-md-2"><label class="form-label small mb-0" for="emPeso">Peso (kg) *</label>
              <input type="number" class="form-control form-control-sm" id="emPeso" min="0" step="0.1"></div>
            <div class="col-6 col-md-3"><label class="form-label small mb-0" for="emCodigo">Código</label>
              <input type="text" class="form-control form-control-sm text-uppercase" id="emCodigo" maxlength="30" placeholder="gerado da descrição"></div>
            <div class="col-6 col-md-3"><label class="form-label small mb-0" for="emCompra">Preço de compra (R$) *</label>
              <input type="number" class="form-control form-control-sm" id="emCompra" min="0" step="0.01"></div>
            <div class="col-6 col-md-3"><label class="form-label small mb-0" for="emEmbalador">Pago ao embalador (R$/cx) *</label>
              <input type="number" class="form-control form-control-sm" id="emEmbalador" min="0" step="0.01"></div>
            <div class="col-6 col-md-3 d-flex gap-2 justify-content-end">
              <button type="button" class="btn btn-sm btn-outline-secondary" id="emLimpar">Limpar</button>
              <button type="button" class="btn btn-sm btn-success" id="emSalvar"><i class="fas fa-check me-1"></i>Salvar</button>
            </div>
          </div>
        </div></div>
        <div class="card shadow-sm border-0"><div class="card-body p-2">
          <div class="row g-2 mb-2">
            <div class="col-6 col-md-3"><select class="form-select form-select-sm" id="emFiltroAlimento"></select></div>
            <div class="col-6 col-md-3"><select class="form-select form-select-sm" id="emFiltroArea"></select></div>
            <div class="col-6 col-md-3"><select class="form-select form-select-sm" id="emFiltroAtivo">
              <option value="true">Ativas</option><option value="false">Desativadas</option><option value="">Todas</option>
            </select></div>
          </div>
          <div class="table-responsive">
            <table class="table table-sm table-striped align-middle mb-0">
              <thead class="table-light small"><tr>
                <th>Descrição</th><th>Código</th><th class="text-end">Peso</th><th>Alimento</th><th>Área</th>
                <th class="text-end">Compra</th><th class="text-end">Embalador</th><th>Situação</th><th class="text-end">Ações</th>
              </tr></thead>
              <tbody id="emTabela"></tbody>
            </table>
          </div>
        </div></div>
      </div>
    </div></div>
  </div>

  <!-- Processar lote: baixa do saldo + diário de Processamento (grade embalador × embalagem, pago
       por caixa com o preço do embalador do cadastro; demais funcionários por horas). -->
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

        <!-- Grade embalador × embalagem: cada célula é o nº de caixas; o preço é o do embalador (cadastro V31). -->
        <div class="card shadow-sm border-0 mb-3">
          <div class="card-header bg-white text-dark d-flex flex-wrap gap-2 justify-content-between align-items-center">
            <span class="fw-semibold"><i class="fas fa-box me-1"></i>Embaladores <small class="text-muted fw-normal">— caixas por embalagem (pago por caixa)</small></span>
            <div class="d-flex gap-2">
              <button type="button" class="btn btn-sm btn-primary" id="cpAddEmbalador"><i class="fas fa-user-plus me-1"></i>Embalador</button>
              <button type="button" class="btn btn-sm btn-outline-primary" id="cpAddEmbalagem"><i class="fas fa-plus me-1"></i>Embalagem</button>
            </div>
          </div>
          <div class="card-body p-0">
            <div class="border-bottom bg-light p-2 d-none" id="cpNovaEmbalagem">
              <div class="small fw-semibold mb-1"><i class="fas fa-box-open me-1"></i>Incluir embalagem no lote</div>
              <div class="row g-2 align-items-end">
                <div class="col-md-4">
                  <label class="form-label small mb-0" for="cpEmbExistente">Embalagem cadastrada</label>
                  <select class="form-select form-select-sm" id="cpEmbExistente"></select>
                </div>
                <div class="col-md-8 d-none" id="cpEmbNovaCampos">
                  <div class="row g-2">
                    <div class="col-6 col-md-4"><label class="form-label small mb-0" for="cpEmbDescricao">Descrição *</label>
                      <input type="text" class="form-control form-control-sm" id="cpEmbDescricao" maxlength="80" placeholder="Ex.: Papelão 7kg"></div>
                    <div class="col-6 col-md-2"><label class="form-label small mb-0" for="cpEmbPeso">Peso (kg) *</label>
                      <input type="number" class="form-control form-control-sm" id="cpEmbPeso" min="0" step="0.1"></div>
                    <div class="col-6 col-md-3"><label class="form-label small mb-0" for="cpEmbCompra">Preço de compra *</label>
                      <input type="number" class="form-control form-control-sm" id="cpEmbCompra" min="0" step="0.01"></div>
                    <div class="col-6 col-md-3"><label class="form-label small mb-0" for="cpEmbEmbalador">Pago ao embalador *</label>
                      <input type="number" class="form-control form-control-sm" id="cpEmbEmbalador" min="0" step="0.01"></div>
                  </div>
                </div>
                <div class="col-12 d-flex gap-2 justify-content-end">
                  <button type="button" class="btn btn-sm btn-outline-secondary" id="cpEmbCancelar">Cancelar</button>
                  <button type="button" class="btn btn-sm btn-primary" id="cpEmbIncluir"><i class="fas fa-check me-1"></i>Incluir coluna</button>
                </div>
              </div>
            </div>
            <div class="table-responsive">
              <table class="table table-sm table-bordered align-middle mb-0" id="cpGrade">
                <thead class="table-light small" id="cpGradeHead"></thead>
                <tbody id="cpGradeBody"></tbody>
                <tfoot class="table-light small fw-semibold" id="cpGradeFoot"></tfoot>
              </table>
            </div>
            <div class="text-center text-muted small py-3" id="cpGradeVazia"></div>
          </div>
        </div>

        <div class="card shadow-sm border-0">
          <div class="card-header bg-white text-dark d-flex flex-wrap gap-2 justify-content-between align-items-center">
            <span class="fw-semibold"><i class="fas fa-people-carry-box me-1"></i>Outros funcionários <small class="text-muted fw-normal">— CLT, diarista, empreita (por horas)</small></span>
            <button type="button" class="btn btn-sm btn-outline-primary" id="cpAddOutro"><i class="fas fa-plus me-1"></i>Funcionário</button>
          </div>
          <div class="card-body p-0">
            <div class="table-responsive">
              <table class="table table-sm align-middle mb-0">
                <thead class="table-light small"><tr>
                  <th style="min-width:180px">Funcionário</th><th>Tipo</th><th>Regime</th>
                  <th style="min-width:100px" class="text-end">Preço</th><th class="text-end">Caixas</th>
                  <th style="min-width:80px" class="text-end">Horas</th>
                  <th style="min-width:100px" class="text-end">Valor</th><th style="width:32px"></th>
                </tr></thead>
                <tbody id="cpFuncBody"></tbody>
              </table>
            </div>
            <div class="text-center text-muted small py-2" id="cpVazio">Nenhum funcionário por horas.</div>
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
            <small class="text-muted"><i class="fas fa-circle-info me-1"></i>Ao confirmar é criado um diário de <strong>Processamento</strong> no talhão do lote, com uma linha por embalador e embalagem (caixas × preço do embalador).</small>
          </div>
        </div>
      </div>
      <div class="modal-footer">
        <button class="btn btn-secondary" data-bs-dismiss="modal"><i class="fas fa-times me-1"></i>Cancelar</button>
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
