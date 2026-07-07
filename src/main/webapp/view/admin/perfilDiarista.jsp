<!DOCTYPE html>
<%@ page contentType="text/html;charset=UTF-8" language="java"%>
<html lang="pt-br">
<head>
    <meta charset="UTF-8">
    <title>Agro - Perfil Diarista</title>
    <%@ include file="../../pagina/importacao.html"%>
</head>
<body>
<header>
    <%@ include file="../../pagina/menu.jsp"%>
</header>

<div class="container-fluid content mt-2">

    <div id="alerta" class="alert d-none" role="alert"></div>

    <!-- CABEÇALHO DO FUNCIONÁRIO -->
    <div class="card shadow-sm mb-3 d-none" id="headerFuncionario">
        <div class="card-body">
            <div class="d-flex justify-content-between align-items-start flex-wrap gap-2">
                <div>
                    <h4 class="mb-1"><i class="fas fa-user-clock me-2 text-warning"></i><span id="nomeFunc"></span></h4>
                    <div class="text-muted" id="cargoMatricula"></div>
                </div>
                <div class="d-flex gap-2 flex-wrap align-items-center">
                    <span id="badgeStatus" class="badge fs-6 px-3 py-2"></span>
                    <button class="btn btn-outline-primary btn-sm" data-bs-toggle="modal" data-bs-target="#modalDiaria">
                        <i class="fas fa-dollar-sign me-1"></i>Editar Diária
                    </button>
                </div>
            </div>
            <div class="row g-2 mt-2">
                <div class="col-auto">
                    <span class="text-muted small"><i class="fas fa-money-bill-wave me-1 text-success"></i>
                    Valor/dia: <strong id="infoValorDia" class="text-success"></strong></span>
                </div>
                <div class="col-auto">
                    <span class="text-muted small"><i class="fas fa-id-card-clip me-1 text-dark"></i>
                    Vínculo: <strong id="infoVinculoPeriodo" class="text-dark"></strong></span>
                </div>
            </div>
        </div>
    </div>

    <!-- FILTRO DE PERÍODO -->
    <div class="card shadow-sm mb-3">
        <div class="card-body py-2">
            <div class="d-flex align-items-center gap-3 flex-wrap">
                <label class="form-label mb-0 fw-semibold">
                    <i class="fas fa-calendar-alt me-1 text-success"></i>Período:
                </label>
                <input type="month" id="inputPeriodo" class="form-control" style="width:160px;">
                <button class="btn btn-success btn-sm" id="btnCarregarPeriodo">
                    <i class="fas fa-search me-1"></i>Carregar
                </button>
            </div>
        </div>
    </div>

    <!-- CARDS RESUMO -->
    <div class="row g-3 mb-3" id="cardsResumo" style="display:none!important">
        <div class="col-6 col-md-3">
            <div class="card shadow-sm border-0 bg-success text-white">
                <div class="card-body text-center py-3">
                    <div class="fs-3 fw-bold" id="cardDiasTrabalhados">-</div>
                    <div class="small">Dias Trabalhados</div>
                </div>
            </div>
        </div>
        <div class="col-6 col-md-3">
            <div class="card shadow-sm border-0 bg-primary text-white">
                <div class="card-body text-center py-3">
                    <div class="fs-3 fw-bold" id="cardBruto">-</div>
                    <div class="small">Valor Bruto</div>
                </div>
            </div>
        </div>
        <div class="col-6 col-md-3">
            <div class="card shadow-sm border-0 bg-warning text-dark">
                <div class="card-body text-center py-3">
                    <div class="fs-3 fw-bold" id="cardTotalVales">-</div>
                    <div class="small">Total Vales</div>
                </div>
            </div>
        </div>
        <div class="col-6 col-md-3">
            <div class="card shadow-sm border-0 bg-dark text-white">
                <div class="card-body text-center py-3">
                    <div class="fs-3 fw-bold" id="cardLiquido">-</div>
                    <div class="small">Valor Líquido</div>
                </div>
            </div>
        </div>
    </div>

    <div class="row g-3">
        <!-- PRESENÇA (DIAS TRABALHADOS) -->
        <div class="col-lg-7">
            <div class="card shadow-sm mb-3">
                <div class="card-header bg-white d-flex justify-content-between align-items-center">
                    <h6 class="text-success fw-bold mb-0">
                        <i class="fas fa-calendar-check me-2"></i>Dias Trabalhados
                    </h6>
                    <button class="btn btn-success btn-sm" data-bs-toggle="modal" data-bs-target="#modalPresenca">
                        <i class="fas fa-plus me-1"></i>Marcar Dia
                    </button>
                </div>
                <div class="card-body p-0">
                    <div class="table-responsive">
                        <table class="table table-sm table-hover mb-0">
                            <thead class="table-light">
                                <tr><th>Data</th><th class="text-end">Ações</th></tr>
                            </thead>
                            <tbody id="bodyPresenca">
                                <tr><td colspan="2" class="text-center text-muted py-3">Selecione um período.</td></tr>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        </div>

        <!-- VALES + FECHAMENTO -->
        <div class="col-lg-5">
            <div class="card shadow-sm mb-3">
                <div class="card-header bg-white d-flex justify-content-between align-items-center">
                    <h6 class="text-warning fw-bold mb-0"><i class="fas fa-hand-holding-usd me-2"></i>Vales</h6>
                    <button class="btn btn-warning btn-sm" data-bs-toggle="modal" data-bs-target="#modalVale">
                        <i class="fas fa-plus me-1"></i>Novo Vale
                    </button>
                </div>
                <div class="card-body p-0">
                    <div class="table-responsive">
                        <table class="table table-sm table-hover mb-0">
                            <thead class="table-light">
                                <tr><th>Data</th><th class="text-end">Valor</th><th>Status</th><th></th></tr>
                            </thead>
                            <tbody id="bodyVales">
                                <tr><td colspan="4" class="text-center text-muted py-3">Sem vales.</td></tr>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>

            <div class="card shadow-sm mb-3">
                <div class="card-header bg-success text-white">
                    <h6 class="mb-0"><i class="fas fa-file-invoice-dollar me-2"></i>Fechamento do Mês</h6>
                </div>
                <div class="card-body" id="painelFechamentoDiarista">
                    <p class="text-muted mb-0">Selecione um período.</p>
                </div>
            </div>
        </div>
    </div>
</div>

<!-- Modal: Editar Diária -->
<div class="modal fade" id="modalDiaria" tabindex="-1">
    <div class="modal-dialog"><div class="modal-content">
        <div class="modal-header bg-primary text-white">
            <h5 class="modal-title"><i class="fas fa-dollar-sign me-2"></i>Editar Diária</h5>
            <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
        </div>
        <div class="modal-body">
            <div id="alertaDiaria" class="alert d-none"></div>
            <label class="form-label">Valor por Dia (R$) *</label>
            <input type="number" class="form-control" id="editValorDia" min="0" step="0.01">
        </div>
        <div class="modal-footer">
            <button class="btn btn-secondary" data-bs-dismiss="modal">Cancelar</button>
            <button class="btn btn-primary" id="btnSalvarDiaria"><i class="fas fa-save me-1"></i>Salvar</button>
        </div>
    </div></div>
</div>

<!-- Modal: Marcar Dia -->
<div class="modal fade" id="modalPresenca" tabindex="-1">
    <div class="modal-dialog"><div class="modal-content">
        <div class="modal-header bg-success text-white">
            <h5 class="modal-title"><i class="fas fa-calendar-check me-2"></i>Marcar Dia Trabalhado</h5>
            <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
        </div>
        <div class="modal-body">
            <div id="alertaPresenca" class="alert d-none"></div>
            <label class="form-label">Data *</label>
            <input type="date" class="form-control" id="presencaData">
        </div>
        <div class="modal-footer">
            <button class="btn btn-secondary" data-bs-dismiss="modal">Cancelar</button>
            <button class="btn btn-success" id="btnSalvarPresenca"><i class="fas fa-save me-1"></i>Salvar</button>
        </div>
    </div></div>
</div>

<!-- Modal: Vale -->
<div class="modal fade" id="modalVale" tabindex="-1">
    <div class="modal-dialog"><div class="modal-content">
        <div class="modal-header bg-warning text-dark">
            <h5 class="modal-title"><i class="fas fa-hand-holding-usd me-2"></i>Novo Vale</h5>
            <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
        </div>
        <div class="modal-body">
            <div id="alertaVale" class="alert d-none"></div>
            <div class="mb-2">
                <label class="form-label">Data *</label>
                <input type="date" class="form-control" id="valeData">
            </div>
            <div class="mb-2">
                <label class="form-label">Valor (R$) *</label>
                <input type="number" class="form-control" id="valeValor" min="0.01" step="0.01" placeholder="0,00">
            </div>
            <div class="mb-2">
                <label class="form-label">Tipo</label>
                <select class="form-select" id="valeTipo">
                    <option value="ADIANTAMENTO">Adiantamento</option>
                    <option value="ALIMENTACAO">Alimentação</option>
                    <option value="TRANSPORTE">Transporte</option>
                    <option value="OUTROS" selected>Outros</option>
                </select>
            </div>
            <div class="mb-2">
                <label class="form-label">Descrição</label>
                <input type="text" class="form-control" id="valeDescricao" maxlength="200">
            </div>
        </div>
        <div class="modal-footer">
            <button class="btn btn-secondary" data-bs-dismiss="modal">Cancelar</button>
            <button class="btn btn-warning" id="btnSalvarVale"><i class="fas fa-save me-1"></i>Salvar</button>
        </div>
    </div></div>
</div>

    <div class="container-fluid content mb-4">
        <div class="card shadow-sm">
            <div class="card-header bg-white d-flex justify-content-between align-items-center">
                <h6 class="text-success fw-bold mb-0"><i class="fas fa-money-check-dollar me-2"></i>Pagamentos</h6>
                <button class="btn btn-success btn-sm" id="btnGerarPagamento"><i class="fas fa-plus me-1"></i>Gerar Pagamento</button>
            </div>
            <div class="card-body p-0"><div class="table-responsive">
                <table class="table table-sm table-hover mb-0">
                    <thead class="table-light"><tr><th>Data</th><th class="text-end">Valor</th><th>Forma / Dados</th><th></th></tr></thead>
                    <tbody id="bodyPagamentos"><tr><td colspan="4" class="text-center text-muted py-2">Sem pagamentos no período.</td></tr></tbody>
                </table>
            </div></div>
        </div>
    </div>
<%@ include file="../../pagina/modalPagamento.jsp"%>
<%@ include file="../../pagina/footer.jsp"%>
</body>
</html>
