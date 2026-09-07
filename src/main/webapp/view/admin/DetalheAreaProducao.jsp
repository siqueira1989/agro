<!DOCTYPE html>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html lang="pt-br">
<head>
  <meta charset="UTF-8">
  <title>Agro - Perfil da Área</title>
  <%@ include file="../../pagina/importacao.html"%>
</head>
<body>
<header><%@ include file="../../pagina/menu.jsp"%></header>

<div class="container-fluid content mt-2" id="detalheArea">
  <div class="card shadow-sm mb-3">
    <div class="card-body bg-dark text-white rounded d-flex justify-content-between align-items-center">
      <h4 class="mb-0"><i class="fas fa-map-location-dot me-2"></i>Perfil da Área — <span id="detProp2"></span></h4>
      <span id="detSituacao"></span>
    </div>
  </div>

  <div id="alertPerfil" class="alert d-none" role="alert"></div>

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

  <!-- INDICADORES DO DIÁRIO (desta área) -->
  <div class="row g-3 mb-3">
    <div class="col-md-3"><div class="card shadow-sm border-0 bg-primary text-white"><div class="card-body text-center py-3">
      <div class="fs-4 fw-bold" id="dcCardAtividades">0</div><div class="small">Atividades</div></div></div></div>
    <div class="col-md-3"><div class="card shadow-sm border-0 bg-warning text-dark"><div class="card-body text-center py-3">
      <div class="fs-4 fw-bold" id="dcCardPendentes">0</div><div class="small">Pendentes</div></div></div></div>
    <div class="col-md-3"><div class="card shadow-sm border-0 bg-success text-white"><div class="card-body text-center py-3">
      <div class="fs-4 fw-bold" id="dcCardConcluidas">0</div><div class="small">Concluídas</div></div></div></div>
    <div class="col-md-3"><div class="card shadow-sm border-0 bg-dark text-white"><div class="card-body text-center py-3">
      <div class="fs-4 fw-bold" id="dcCardCusto">R$ 0,00</div><div class="small">Custo Total</div></div></div></div>
  </div>

  <!-- QUADRAS -->
  <div class="card shadow-sm mb-3">
    <div class="card-header bg-white"><h5 class="text-dark fw-bold mb-0"><i class="fas fa-th-large me-2"></i>Talhões (Quadras)</h5></div>
    <div class="card-body p-0">
      <div class="table-responsive">
        <table class="table table-sm table-striped mb-0 align-middle">
          <thead class="table-light"><tr><th>Nome</th><th class="text-end">Nº Plantas</th><th>Tipo de Planta</th><th>Situação</th></tr></thead>
          <tbody id="detQuadrasBody"></tbody>
        </table>
      </div>
    </div>
  </div>

  <!-- DIÁRIO DE CAMPO (desta área) -->
  <div class="card shadow-sm mb-4">
    <div class="card-header bg-white d-flex justify-content-between align-items-center">
      <h5 class="text-success fw-bold mb-0"><i class="fas fa-book me-2"></i>Diário de Campo</h5>
      <div class="d-flex gap-2 align-items-center">
        <select id="dcFiltroStatus" class="form-select form-select-sm" style="width:160px;">
          <option value="">Todos status</option>
          <option value="PLANEJADA">Planejado</option>
          <option value="EM_ANDAMENTO">Execução</option>
          <option value="CONCLUIDA">Concluído</option>
        </select>
        <div class="dropdown">
          <button class="btn btn-outline-warning btn-sm position-relative" id="dcBtnAlertas" type="button" data-bs-toggle="dropdown" aria-expanded="false" title="Atividades planejadas">
            <i class="fas fa-bell"></i>
            <span class="badge rounded-pill bg-danger d-none" id="dcBadgeAlertas" style="font-size:.6rem;">0</span>
          </button>
          <ul class="dropdown-menu dropdown-menu-end p-2" id="dcListaAlertas" style="min-width:280px;max-height:300px;overflow:auto;">
            <li class="text-muted small px-2">Nenhuma atividade planejada.</li>
          </ul>
        </div>
        <button class="btn btn-outline-success btn-sm" id="btnAgendarDiario"><i class="fas fa-calendar-plus me-1"></i>Agendar</button>
        <button class="btn btn-success btn-sm" id="btnNovoDiario"><i class="fas fa-plus me-1"></i>Novo Diário</button>
      </div>
    </div>
    <div class="card-body p-0">
      <div class="table-responsive">
        <table class="table table-sm table-hover mb-0 align-middle">
          <thead class="table-light"><tr>
            <th>Número</th><th>Data</th><th>Talhão</th><th>Cultura</th><th>Atividade</th>
            <th>Status</th><th class="text-end">Custo Total</th><th class="text-end">Ações</th>
          </tr></thead>
          <tbody id="dcTabelaBody"><tr><td colspan="8" class="text-center text-muted py-3">Carregando...</td></tr></tbody>
        </table>
      </div>
    </div>
  </div>

  <div class="mb-5">
    <a href="${pageContext.request.contextPath}/view/admin/areaproducao.jsp" class="btn btn-secondary"><i class="fas fa-arrow-left me-1"></i>Voltar</a>
  </div>
</div>

<!-- MODAL: Novo Diário -->
<div class="modal fade" id="modalNovoDiario" tabindex="-1">
  <div class="modal-dialog modal-xl"><div class="modal-content">
    <div class="modal-header bg-success text-white">
      <h5 class="modal-title"><i class="fas fa-book me-2"></i><span id="dcModalTitulo">Novo Diário de Campo</span></h5>
      <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
    </div>
    <div class="modal-body">
      <div id="alertDiario" class="alert d-none"></div>
      <input type="hidden" id="dcId">
      <input type="hidden" id="dcStatus">
      <input type="hidden" id="dcNumero">

      <div class="row g-2">
        <div class="col-md-3"><label class="form-label">Data *</label><input type="date" class="form-control" id="dcData"></div>
        <div class="col-md-3"><label class="form-label">Talhão *</label><select class="form-select" id="dcTalhao"></select></div>
        <div class="col-md-3"><label class="form-label">Cultura (do talhão)</label>
          <input type="text" class="form-control bg-light" id="dcCulturaNome" readonly placeholder="—">
          <input type="hidden" id="dcCultura"></div>
        <div class="col-md-3"><label class="form-label">Responsável *</label><select class="form-select" id="dcResponsavel"></select></div>
      </div>
      <div class="row g-2 mt-1">
        <div class="col-md-4"><label class="form-label">Tipo de atividade *</label>
          <div class="input-group">
            <select class="form-select" id="dcTipo"></select>
            <button class="btn btn-outline-secondary" type="button" id="btnAddTipo" title="Novo tipo"><i class="fas fa-plus"></i></button>
          </div>
        </div>
        <div class="col-md-3"><label class="form-label">Situação *</label>
          <select class="form-select" id="dcStatusSel">
            <option value="EM_ANDAMENTO">Execução</option>
            <option value="CONCLUIDA">Concluído</option>
          </select>
          <small class="text-muted">Concluído calcula o custo final e já baixa o estoque dos insumos.</small>
        </div>
        <div class="col-md-5"><label class="form-label">Descrição</label><input type="text" class="form-control" id="dcDescricao" maxlength="200"></div>
      </div>
      <div class="row g-2 mt-1">
        <div class="col-md-12"><label class="form-label">Observações</label><input type="text" class="form-control" id="dcObs" maxlength="200"></div>
      </div>
      <div class="mt-2"><span class="badge bg-light text-dark border"><i class="fas fa-seedling me-1 text-success"></i>Safra vinculada: <strong id="dcSafraNome">—</strong></span></div>

      <!-- Funcionários -->
      <div class="d-flex justify-content-between align-items-center mt-3 mb-1">
        <h6 class="fw-bold text-secondary mb-0"><i class="fas fa-users me-1"></i>Funcionários</h6>
        <button class="btn btn-outline-secondary btn-sm" type="button" id="btnAddFunc"><i class="fas fa-plus me-1"></i>Adicionar</button>
      </div>
      <div class="table-responsive"><table class="table table-sm align-middle mb-2">
        <thead class="table-light"><tr><th style="width:30%">Funcionário</th><th>Tipo</th><th style="width:14%">Horas</th><th style="width:18%">Valor (auto)</th><th></th></tr></thead>
        <tbody id="dcFuncBody"></tbody></table></div>
      <div id="dcFuncSoNaExecucao" class="alert alert-info py-2 small d-none">
        <i class="fas fa-circle-info me-1"></i>Ao editar um diário já criado, os funcionários ficam somente leitura — use
        <strong>"Registrar nova execução"</strong> abaixo para adicionar colaboradores de outro dia (o custo soma tudo).
      </div>
      <div id="dcBlocoExecucao" class="border rounded p-2 mb-2 d-none">
        <div class="d-flex justify-content-between align-items-center mb-2">
          <h6 class="fw-bold text-secondary mb-0"><i class="fas fa-calendar-day me-1"></i>Registrar Nova Execução</h6>
        </div>
        <div class="row g-2">
          <div class="col-md-4"><label class="form-label small mb-0">Data da execução *</label><input type="date" class="form-control form-control-sm" id="exData"></div>
        </div>
        <div class="table-responsive mt-2"><table class="table table-sm align-middle mb-2">
          <thead class="table-light"><tr><th style="width:30%">Funcionário</th><th>Tipo</th><th style="width:16%">Horas</th><th style="width:18%">Valor (auto)</th><th></th></tr></thead>
          <tbody id="exFuncBody"></tbody></table></div>
        <button type="button" class="btn btn-outline-secondary btn-sm" id="btnAddExecFunc"><i class="fas fa-plus me-1"></i>Adicionar colaborador</button>

        <h6 class="fw-bold text-secondary mb-0 mt-3"><i class="fas fa-receipt me-1"></i>Despesas desta execução</h6>
        <div class="table-responsive mt-2"><table class="table table-sm align-middle mb-2">
          <thead class="table-light"><tr><th style="width:26%">Despesa</th><th>Classif.</th><th style="width:12%">Qtd</th><th style="width:16%">Valor unit.</th><th style="width:16%">Subtotal</th><th></th></tr></thead>
          <tbody id="exDespBody"></tbody></table></div>
        <button type="button" class="btn btn-outline-secondary btn-sm" id="btnAddExecDesp"><i class="fas fa-plus me-1"></i>Adicionar despesa</button>

        <button type="button" class="btn btn-success btn-sm float-end" id="btnSalvarExecucao"><i class="fas fa-save me-1"></i>Salvar Execução</button>
      </div>

      <!-- Máquinas -->
      <div class="d-flex justify-content-between align-items-center mt-2 mb-1">
        <h6 class="fw-bold text-secondary mb-0"><i class="fas fa-tractor me-1"></i>Máquinas</h6>
        <button class="btn btn-outline-secondary btn-sm" type="button" id="btnAddMaq"><i class="fas fa-plus me-1"></i>Adicionar</button>
      </div>
      <div class="table-responsive"><table class="table table-sm align-middle mb-2">
        <thead class="table-light"><tr><th style="width:40%">Maquinário</th><th>Uso (Horas/KM)</th><th class="text-end">Custo</th><th></th></tr></thead>
        <tbody id="dcMaqBody"></tbody></table></div>

      <!-- Insumos -->
      <div class="d-flex justify-content-between align-items-center mt-2 mb-1">
        <h6 class="fw-bold text-secondary mb-0"><i class="fas fa-flask me-1"></i>Insumos</h6>
        <button class="btn btn-outline-secondary btn-sm" type="button" id="btnAddIns"><i class="fas fa-plus me-1"></i>Adicionar</button>
      </div>
      <div class="table-responsive"><table class="table table-sm align-middle mb-0">
        <thead class="table-light"><tr><th style="width:34%">Insumo</th><th>Quantidade</th><th>Dose aplicada</th><th></th></tr></thead>
        <tbody id="dcInsBody"></tbody></table></div>

      <!-- Despesas -->
      <div class="d-flex justify-content-between align-items-center mt-2 mb-1">
        <h6 class="fw-bold text-secondary mb-0"><i class="fas fa-receipt me-1"></i>Despesas</h6>
        <button class="btn btn-outline-secondary btn-sm" type="button" id="btnAddDesp"><i class="fas fa-plus me-1"></i>Adicionar</button>
      </div>
      <div class="table-responsive"><table class="table table-sm align-middle mb-2">
        <thead class="table-light"><tr><th style="width:26%">Despesa</th><th>Classif.</th><th style="width:12%">Qtd</th><th style="width:16%">Valor unit.</th><th style="width:16%">Subtotal</th><th></th></tr></thead>
        <tbody id="dcDespBody"></tbody></table></div>
      <div id="dcDespSoNaExecucao" class="alert alert-info py-2 small d-none">
        <i class="fas fa-circle-info me-1"></i>Ao editar um diário já criado, as despesas ficam somente leitura — use
        <strong>"Registrar nova execução"</strong> acima para lançar novas compras (o custo soma tudo, nunca faz média).
      </div>
      <small class="text-muted">A baixa de estoque ocorre ao <strong>finalizar</strong> a atividade. O custo total é calculado e salvo automaticamente.</small>
    </div>
    <div class="modal-footer justify-content-between">
      <div class="fw-bold">Custo estimado: <span id="dcTotalPreview" class="text-success">R$ 0,00</span></div>
      <div>
        <button class="btn btn-secondary" data-bs-dismiss="modal">Cancelar</button>
        <button class="btn btn-success" id="btnSalvarDiario"><i class="fas fa-save me-1"></i>Salvar Diário</button>
      </div>
    </div>
  </div></div>
</div>

<!-- MODAL: Agendar Atividade (agendamento enxuto, sem funcionários/máquinas/insumos) -->
<div class="modal fade" id="modalAgendarDiario" tabindex="-1">
  <div class="modal-dialog"><div class="modal-content">
    <div class="modal-header bg-warning">
      <h5 class="modal-title"><i class="fas fa-calendar-plus me-2"></i>Agendar Atividade</h5>
      <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
    </div>
    <div class="modal-body">
      <div id="alertAgendar" class="alert d-none"></div>
      <div class="row g-2">
        <div class="col-md-6"><label class="form-label">Talhão *</label><select class="form-select" id="agTalhao"></select></div>
        <div class="col-md-6"><label class="form-label">Tipo de atividade *</label><select class="form-select" id="agTipo"></select></div>
      </div>
      <div class="row g-2 mt-1">
        <div class="col-md-6"><label class="form-label">Responsável *</label><select class="form-select" id="agResponsavel"></select></div>
        <div class="col-md-6"><label class="form-label">Data prevista *</label><input type="date" class="form-control" id="agData"></div>
      </div>
      <div class="row g-2 mt-1">
        <div class="col-md-12"><label class="form-label">Descrição</label><input type="text" class="form-control" id="agDescricao" maxlength="200"></div>
      </div>
      <div class="row g-2 mt-1">
        <div class="col-md-12"><label class="form-label">Observações</label><input type="text" class="form-control" id="agObs" maxlength="200"></div>
      </div>
      <small class="text-muted d-block mt-2">Agendamento não calcula custo — é só previsão. Depois, use "Iniciar execução" na lista para registrar funcionários, máquinas e insumos.</small>
    </div>
    <div class="modal-footer">
      <button class="btn btn-secondary" data-bs-dismiss="modal">Cancelar</button>
      <button class="btn btn-warning" id="btnSalvarAgendar"><i class="fas fa-save me-1"></i>Agendar</button>
    </div>
  </div></div>
</div>

<!-- MODAL: Detalhe/Finalizar Diário -->
<div class="modal fade" id="modalDetalheDiario" tabindex="-1">
  <div class="modal-dialog modal-lg"><div class="modal-content">
    <div class="modal-header bg-dark text-white">
      <h5 class="modal-title"><i class="fas fa-book me-2"></i>Diário <span id="ddNumero"></span></h5>
      <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
    </div>
    <div class="modal-body" id="ddCorpo"></div>
    <div class="modal-footer">
      <button class="btn btn-secondary" data-bs-dismiss="modal">Fechar</button>
      <button class="btn btn-success d-none" id="btnFinalizarDiario"><i class="fas fa-check me-1"></i>Finalizar (calcular custo + baixa estoque)</button>
    </div>
  </div></div>
</div>

<%@ include file="../../pagina/footer.jsp"%>
</body>
</html>
