<!DOCTYPE html>
<%@ page contentType="text/html;charset=UTF-8" language="java"%>
<html lang="pt-br">
<head>
    <meta charset="UTF-8">
    <title>Agro - Perfil CLT</title>
    <%@ include file="../../pagina/importacao.html"%>
</head>
<body>
<header><%@ include file="../../pagina/menu.jsp"%></header>

<div class="container-fluid content mt-2">

    <!-- ALERTA GLOBAL -->
    <div id="alerta" class="alert d-none" role="alert"></div>

    <!-- CABEÇALHO DO FUNCIONÁRIO -->
    <div id="headerFuncionario" class="card shadow-sm mb-3 d-none">
        <div class="card-body py-3">
            <div class="d-flex align-items-center gap-3 flex-wrap">
                <div class="rounded-circle bg-success text-white d-flex align-items-center justify-content-center"
                     style="width:56px;height:56px;font-size:1.6rem;flex-shrink:0;">
                    <i class="fas fa-user-tie"></i>
                </div>
                <div class="flex-grow-1">
                    <h5 id="nomeFunc" class="mb-0 fw-bold"></h5>
                    <small class="text-muted" id="cargoMatricula"></small>
                </div>
                <div class="d-flex gap-2 flex-wrap align-items-center">
                    <span id="badgeStatus" class="badge fs-6 px-3 py-2"></span>
                    <div class="dropdown">
                        <button class="btn btn-outline-secondary btn-sm dropdown-toggle" data-bs-toggle="dropdown">
                            Alterar Status
                        </button>
                        <ul class="dropdown-menu">
                            <li><a class="dropdown-item" href="#" onclick="alterarStatus('ATIVO')">
                                <i class="fas fa-check-circle text-success me-1"></i>Ativo</a></li>
                            <li><a class="dropdown-item" href="#" onclick="alterarStatus('AFASTADO')">
                                <i class="fas fa-pause-circle text-warning me-1"></i>Afastado</a></li>
                            <li><a class="dropdown-item" href="#" onclick="alterarStatus('DESLIGADO')">
                                <i class="fas fa-times-circle text-danger me-1"></i>Desligado</a></li>
                        </ul>
                    </div>
                    <button class="btn btn-outline-primary btn-sm" data-bs-toggle="modal" data-bs-target="#modalSalario">
                        <i class="fas fa-dollar-sign me-1"></i>Editar Salário
                    </button>
                    <button class="btn btn-outline-danger btn-sm d-none" id="btnDesligarVinculo">
                        <i class="fas fa-user-slash me-1"></i>Desligar
                    </button>
                    <button class="btn btn-outline-success btn-sm d-none" id="btnReadmitirVinculo">
                        <i class="fas fa-user-plus me-1"></i>Readmitir
                    </button>
                </div>
            </div>
            <div class="row g-2 mt-2">
                <div class="col-auto">
                    <span class="text-muted small"><i class="fas fa-money-bill-wave me-1 text-success"></i>
                    Salário: <strong id="infoSalario" class="text-success"></strong></span>
                </div>
                <div class="col-auto">
                    <span class="text-muted small"><i class="fas fa-clock me-1 text-primary"></i>
                    H.Extra/h: <strong id="infoHoraExtra" class="text-primary"></strong></span>
                </div>
                <div class="col-auto">
                    <span class="text-muted small"><i class="fas fa-calendar-day me-1 text-secondary"></i>
                    Jornada: <strong id="infoJornada" class="text-secondary"></strong></span>
                </div>
                <div class="col-auto">
                    <span class="text-muted small"><i class="fas fa-tractor me-1 text-success"></i>
                    Atividade: <strong id="infoAtividade" class="text-success"></strong></span>
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
                <span class="text-muted small ms-auto" id="infoDiasUteis"></span>
            </div>
        </div>
    </div>

    <!-- CARDS RESUMO -->
    <div class="row g-3 mb-3" id="cardsResumo" style="display:none!important">
        <div class="col-6 col-md-3">
            <div class="card shadow-sm border-0 bg-success text-white">
                <div class="card-body text-center py-3">
                    <div class="fs-3 fw-bold" id="cardDiasTrabalhados">-</div>
                    <div class="small">Dias c/ Ponto</div>
                </div>
            </div>
        </div>
        <div class="col-6 col-md-3">
            <div class="card shadow-sm border-0 bg-danger text-white">
                <div class="card-body text-center py-3">
                    <div class="fs-3 fw-bold" id="cardFaltasInjust">-</div>
                    <div class="small">Faltas Injustificadas</div>
                </div>
            </div>
        </div>
        <div class="col-6 col-md-3">
            <div class="card shadow-sm border-0 bg-primary text-white">
                <div class="card-body text-center py-3">
                    <div class="fs-3 fw-bold" id="cardHorasExtras">-</div>
                    <div class="small">Horas Extras</div>
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
    </div>

    <div class="row g-3">

        <!-- COLUNA ESQUERDA: Ponto e Faltas -->
        <div class="col-lg-7">

            <!-- REGISTRO DE PONTO -->
            <div class="card shadow-sm mb-3">
                <div class="card-header bg-white d-flex justify-content-between align-items-center">
                    <h6 class="text-success fw-bold mb-0">
                        <i class="fas fa-fingerprint me-2"></i>Registro de Ponto
                    </h6>
                    <button class="btn btn-success btn-sm" id="btnNovoPonto" data-bs-toggle="modal" data-bs-target="#modalPonto">
                        <i class="fas fa-plus me-1"></i>Lançar Ponto
                    </button>
                </div>
                <div class="card-body p-0">
                    <div class="table-responsive">
                        <table class="table table-sm table-hover mb-0" id="tabelaPonto">
                            <thead class="table-light">
                                <tr>
                                    <th>Data</th>
                                    <th>Entrada 1</th>
                                    <th>Saída 1</th>
                                    <th>Entrada 2</th>
                                    <th>Saída 2</th>
                                    <th>Total</th>
                                    <th>Extra</th>
                                    <th>Noturno</th>
                                    <th class="text-end">Ações</th>
                                </tr>
                            </thead>
                            <tbody id="bodyPonto">
                                <tr><td colspan="9" class="text-center text-muted py-3">Selecione um período.</td></tr>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>

            <!-- FALTAS -->
            <div class="card shadow-sm mb-3">
                <div class="card-header bg-white d-flex justify-content-between align-items-center">
                    <h6 class="text-danger fw-bold mb-0">
                        <i class="fas fa-calendar-times me-2"></i>Faltas
                    </h6>
                    <button class="btn btn-danger btn-sm" data-bs-toggle="modal" data-bs-target="#modalFalta">
                        <i class="fas fa-plus me-1"></i>Registrar Falta
                    </button>
                </div>
                <div class="card-body p-0">
                    <table class="table table-sm table-hover mb-0" id="tabelaFaltas">
                        <thead class="table-light">
                            <tr>
                                <th>Data</th>
                                <th>Tipo</th>
                                <th>Motivo</th>
                                <th></th>
                            </tr>
                        </thead>
                        <tbody id="bodyFaltas">
                            <tr><td colspan="4" class="text-center text-muted py-3">Nenhuma falta no período.</td></tr>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>

        <!-- COLUNA DIREITA: Vales e Fechamento -->
        <div class="col-lg-5">

            <!-- VALES -->
            <div class="card shadow-sm mb-3">
                <div class="card-header bg-white d-flex justify-content-between align-items-center">
                    <h6 class="text-warning fw-bold mb-0">
                        <i class="fas fa-hand-holding-usd me-2"></i>Vales
                    </h6>
                    <button class="btn btn-warning btn-sm" data-bs-toggle="modal" data-bs-target="#modalVale">
                        <i class="fas fa-plus me-1"></i>Novo Vale
                    </button>
                </div>
                <div class="card-body p-0">
                    <table class="table table-sm table-hover mb-0">
                        <thead class="table-light">
                            <tr>
                                <th>Data</th>
                                <th>Tipo</th>
                                <th class="text-end">Valor</th>
                                <th>Status</th>
                                <th></th>
                            </tr>
                        </thead>
                        <tbody id="bodyVales">
                            <tr><td colspan="5" class="text-center text-muted py-3">Nenhum vale no período.</td></tr>
                        </tbody>
                    </table>
                </div>
            </div>

            <!-- FECHAMENTO SALARIAL -->
            <div class="card shadow-sm border-success mb-3">
                <div class="card-header bg-success text-white d-flex justify-content-between align-items-center">
                    <h6 class="fw-bold mb-0">
                        <i class="fas fa-calculator me-2"></i>Fechamento do Mês
                    </h6>
                    <button class="btn btn-light btn-sm fw-bold" id="btnCalcular">
                        <i class="fas fa-sync-alt me-1"></i>Calcular
                    </button>
                </div>
                <div class="card-body" id="painelFechamento">
                    <p class="text-muted text-center mb-0">Clique em "Calcular" para gerar o fechamento.</p>
                </div>
            </div>

        </div>
    </div><!-- /row -->
</div><!-- /container -->

<!-- ===== MODAIS ===== -->

<!-- Modal: Lançar Ponto -->
<div class="modal fade" id="modalPonto" tabindex="-1">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header bg-success text-white">
                <h5 class="modal-title" id="modalPontoTitulo"><i class="fas fa-fingerprint me-2"></i>Lançar Ponto</h5>
                <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
            </div>
            <div class="modal-body">
                <div id="alertaPonto" class="alert d-none"></div>
                <div class="mb-3">
                    <label class="form-label">Data *</label>
                    <input type="date" class="form-control" id="pontoData">
                </div>
                <div class="row g-2">
                    <div class="col-6">
                        <label class="form-label">Entrada 1</label>
                        <input type="time" class="form-control" id="pontoEntrada1">
                    </div>
                    <div class="col-6">
                        <label class="form-label">Saída 1</label>
                        <input type="time" class="form-control" id="pontoSaida1">
                    </div>
                    <div class="col-6">
                        <label class="form-label">Entrada 2</label>
                        <input type="time" class="form-control" id="pontoEntrada2">
                    </div>
                    <div class="col-6">
                        <label class="form-label">Saída 2</label>
                        <input type="time" class="form-control" id="pontoSaida2">
                    </div>
                </div>
                <div class="mt-2">
                    <label class="form-label">Observação</label>
                    <input type="text" class="form-control" id="pontoObs" maxlength="200">
                </div>
            </div>
            <div class="modal-footer">
                <button class="btn btn-secondary" data-bs-dismiss="modal">Cancelar</button>
                <button class="btn btn-success" id="btnSalvarPonto">
                    <i class="fas fa-save me-1"></i>Salvar
                </button>
            </div>
        </div>
    </div>
</div>

<!-- Modal: Registrar Falta -->
<div class="modal fade" id="modalFalta" tabindex="-1">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header bg-danger text-white">
                <h5 class="modal-title"><i class="fas fa-calendar-times me-2"></i>Registrar Falta</h5>
                <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
            </div>
            <div class="modal-body">
                <div id="alertaFalta" class="alert d-none"></div>
                <div class="mb-3">
                    <label class="form-label">Data da Falta *</label>
                    <input type="date" class="form-control" id="faltaData">
                </div>
                <div class="mb-3">
                    <label class="form-label">Tipo</label>
                    <select class="form-select" id="faltaJustificada">
                        <option value="false">Injustificada</option>
                        <option value="true">Justificada</option>
                    </select>
                </div>
                <div class="mb-3">
                    <label class="form-label">Motivo</label>
                    <input type="text" class="form-control" id="faltaMotivo" maxlength="200">
                </div>
            </div>
            <div class="modal-footer">
                <button class="btn btn-secondary" data-bs-dismiss="modal">Cancelar</button>
                <button class="btn btn-danger" id="btnSalvarFalta">
                    <i class="fas fa-save me-1"></i>Salvar
                </button>
            </div>
        </div>
    </div>
</div>

<!-- Modal: Novo Vale -->
<div class="modal fade" id="modalVale" tabindex="-1">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header bg-warning">
                <h5 class="modal-title fw-bold"><i class="fas fa-hand-holding-usd me-2"></i>Novo Vale</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <div class="modal-body">
                <div id="alertaVale" class="alert d-none"></div>
                <div class="mb-3">
                    <label class="form-label">Tipo de Vale *</label>
                    <select class="form-select" id="valeTipo">
                        <option value="ALIMENTACAO">Alimentação</option>
                        <option value="TRANSPORTE">Transporte</option>
                        <option value="ADIANTAMENTO">Adiantamento de Salário</option>
                        <option value="OUTROS">Outros</option>
                    </select>
                </div>
                <div class="mb-3">
                    <label class="form-label">Valor (R$) *</label>
                    <input type="number" class="form-control" id="valeValor"
                           min="0.01" step="0.01" placeholder="0,00">
                </div>
                <div class="mb-3">
                    <label class="form-label">Data *</label>
                    <input type="date" class="form-control" id="valeData">
                </div>
                <div class="mb-3">
                    <label class="form-label">Descrição</label>
                    <input type="text" class="form-control" id="valeDescricao" maxlength="200">
                </div>
            </div>
            <div class="modal-footer">
                <button class="btn btn-secondary" data-bs-dismiss="modal">Cancelar</button>
                <button class="btn btn-warning fw-bold" id="btnSalvarVale">
                    <i class="fas fa-save me-1"></i>Salvar
                </button>
            </div>
        </div>
    </div>
</div>

<!-- Modal: Editar Salário -->
<div class="modal fade" id="modalSalario" tabindex="-1">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header bg-primary text-white">
                <h5 class="modal-title"><i class="fas fa-dollar-sign me-2"></i>Editar Salário</h5>
                <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
            </div>
            <div class="modal-body">
                <div id="alertaSalario" class="alert d-none"></div>
                <div class="mb-3">
                    <label class="form-label">Salário Mensal (R$) *</label>
                    <input type="number" class="form-control" id="editSalario"
                           min="0.01" step="0.01">
                </div>
                <div class="mb-3">
                    <label class="form-label">Valor Hora Extra (R$)</label>
                    <input type="number" class="form-control" id="editHoraExtra"
                           min="0" step="0.01">
                </div>
                <div class="mb-3">
                    <label class="form-label">Carga Horária Diária (horas)</label>
                    <input type="number" class="form-control" id="editCargaHoraria"
                           min="1" max="12" value="8">
                </div>
                <div class="mb-3">
                    <label class="form-label">Tipo de Atividade Rural</label>
                    <select class="form-select" id="editTipoAtividade">
                        <option value="LAVOURA">Lavoura (noturno 21h–05h)</option>
                        <option value="PECUARIA">Pecuária (noturno 20h–04h)</option>
                    </select>
                    <small class="text-muted">Define a janela do adicional noturno (25%, hora de 60 min).</small>
                </div>
            </div>
            <div class="modal-footer">
                <button class="btn btn-secondary" data-bs-dismiss="modal">Cancelar</button>
                <button class="btn btn-primary" id="btnSalvarSalario">
                    <i class="fas fa-save me-1"></i>Salvar
                </button>
            </div>
        </div>
    </div>
</div>

<%@ include file="../../pagina/footer.jsp"%>
</body>
</html>
