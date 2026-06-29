<%@ page contentType="text/html;charset=UTF-8" language="java" isELIgnored="true" %>
<!DOCTYPE html>
<html lang="pt-br">
<head>
    <title>Agro — Ponto Eletrônico</title>
    <%@ include file="../../pagina/importacao.html" %>
</head>
<body>
<header><%@ include file="../../pagina/menu.jsp" %></header>

<div class="container-fluid content mt-3">

    <!-- Cabeçalho -->
    <div class="bg-success text-white p-3 rounded mb-3">
        <h2 class="m-0"><i class="fas fa-fingerprint me-2"></i>Ponto Eletrônico</h2>
    </div>

    <div id="alerta" class="alert d-none mb-3" role="alert"></div>

    <!-- Filtros -->
    <div class="card shadow-sm mb-3">
        <div class="card-body py-2">
            <div class="row g-2 align-items-end">
                <div class="col-md-4">
                    <label class="form-label mb-1 fw-semibold">Funcionário</label>
                    <select id="selFuncionario" class="form-select form-select-sm">
                        <option value="">Selecione...</option>
                    </select>
                </div>
                <div class="col-md-3">
                    <label class="form-label mb-1 fw-semibold">Período</label>
                    <input type="month" id="selPeriodo" class="form-control form-control-sm">
                </div>
                <div class="col-md-auto">
                    <button class="btn btn-success btn-sm" id="btnBuscar">
                        <i class="fas fa-search me-1"></i>Buscar
                    </button>
                </div>
                <div class="col-md-auto ms-auto text-center">
                    <div id="relogio">00:00:00</div>
                    <small class="text-muted" id="dataHoje"></small>
                </div>
            </div>
        </div>
    </div>

    <!-- KPIs -->
    <div class="row g-3 mb-3" id="kpiRow">
        <div class="col-6 col-md-3">
            <div class="card shadow-sm card-kpi h-100">
                <div class="card-body py-2">
                    <div class="text-muted small">Dias Trabalhados</div>
                    <h3 id="kpiDias" class="fw-bold mb-0">—</h3>
                    <i class="fas fa-calendar-check fa-lg text-success float-end mt-1"></i>
                </div>
            </div>
        </div>
        <div class="col-6 col-md-3">
            <div class="card shadow-sm card-kpi-info h-100">
                <div class="card-body py-2">
                    <div class="text-muted small">Horas Extras</div>
                    <h3 id="kpiExtras" class="fw-bold mb-0">—</h3>
                    <i class="fas fa-clock fa-lg text-primary float-end mt-1"></i>
                </div>
            </div>
        </div>
        <div class="col-6 col-md-3">
            <div class="card shadow-sm card-kpi-danger h-100">
                <div class="card-body py-2">
                    <div class="text-muted small">Faltas</div>
                    <h3 id="kpiFaltas" class="fw-bold mb-0">—</h3>
                    <i class="fas fa-user-times fa-lg text-danger float-end mt-1"></i>
                </div>
            </div>
        </div>
        <div class="col-6 col-md-3">
            <div class="card shadow-sm card-kpi-warn h-100">
                <div class="card-body py-2">
                    <div class="text-muted small">Vales</div>
                    <h3 id="kpiVales" class="fw-bold mb-0">—</h3>
                    <i class="fas fa-dollar-sign fa-lg text-warning float-end mt-1"></i>
                </div>
            </div>
        </div>
    </div>

    <!-- TABS -->
    <ul class="nav nav-tabs mb-3" id="abas">
        <li class="nav-item">
            <button class="nav-link active" data-bs-toggle="tab" data-bs-target="#tabPonto">
                <i class="fas fa-hand-point-up me-1"></i>Bater Ponto
            </button>
        </li>
        <li class="nav-item">
            <button class="nav-link" data-bs-toggle="tab" data-bs-target="#tabEspelho">
                <i class="fas fa-table me-1"></i>Espelho do Mês
            </button>
        </li>
        <li class="nav-item">
            <button class="nav-link" data-bs-toggle="tab" data-bs-target="#tabFaltasVales">
                <i class="fas fa-clipboard-list me-1"></i>Faltas &amp; Vales
            </button>
        </li>
        <li class="nav-item">
            <button class="nav-link" data-bs-toggle="tab" data-bs-target="#tabFechamento">
                <i class="fas fa-file-invoice-dollar me-1"></i>Fechamento
            </button>
        </li>
    </ul>

    <div class="tab-content">

        <!-- ===== ABA 1: BATER PONTO ===== -->
        <div class="tab-pane fade show active" id="tabPonto">
            <div class="row justify-content-center">
                <div class="col-md-7">
                    <div class="card shadow-sm">
                        <div class="card-header bg-white fw-semibold">
                            <i class="fas fa-fingerprint text-success me-2"></i>Registrar Marcação
                        </div>
                        <div class="card-body">
                            <div class="row g-2 mb-3">
                                <div class="col-md-6">
                                    <label class="form-label">Data</label>
                                    <input type="date" id="pontoData" class="form-control">
                                </div>
                                <div class="col-md-6">
                                    <label class="form-label">Hora (deixe em branco = agora)</label>
                                    <input type="time" id="pontoHora" class="form-control"
                                           placeholder="HH:MM">
                                </div>
                            </div>
                            <div class="row g-2">
                                <div class="col-6">
                                    <button class="btn btn-success btn-ponto w-100"
                                            onclick="baterPonto('entrada1')">
                                        <i class="fas fa-sign-in-alt me-2"></i>Entrada 1
                                    </button>
                                </div>
                                <div class="col-6">
                                    <button class="btn btn-outline-success btn-ponto w-100"
                                            onclick="baterPonto('saida1')">
                                        <i class="fas fa-sign-out-alt me-2"></i>Saída 1
                                    </button>
                                </div>
                                <div class="col-6">
                                    <button class="btn btn-primary btn-ponto w-100"
                                            onclick="baterPonto('entrada2')">
                                        <i class="fas fa-sign-in-alt me-2"></i>Entrada 2
                                    </button>
                                </div>
                                <div class="col-6">
                                    <button class="btn btn-outline-primary btn-ponto w-100"
                                            onclick="baterPonto('saida2')">
                                        <i class="fas fa-sign-out-alt me-2"></i>Saída 2
                                    </button>
                                </div>
                            </div>
                            <hr>
                            <!-- Registro de hoje -->
                            <h6 class="fw-semibold"><i class="fas fa-calendar-day text-success me-1"></i>Registro de Hoje</h6>
                            <div id="pontoHoje" class="text-muted small">Selecione um funcionário.</div>
                        </div>
                    </div>
                </div>

                <!-- Modal edição manual -->
                <div class="col-md-5">
                    <div class="card shadow-sm">
                        <div class="card-header bg-white fw-semibold">
                            <i class="fas fa-edit text-warning me-2"></i>Edição Manual (Admin)
                        </div>
                        <div class="card-body">
                            <div class="mb-2">
                                <label class="form-label">Data</label>
                                <input type="date" id="manualData" class="form-control form-control-sm">
                            </div>
                            <div class="row g-2">
                                <div class="col-6">
                                    <label class="form-label">Entrada 1</label>
                                    <input type="time" id="manualE1" class="form-control form-control-sm">
                                </div>
                                <div class="col-6">
                                    <label class="form-label">Saída 1</label>
                                    <input type="time" id="manualS1" class="form-control form-control-sm">
                                </div>
                                <div class="col-6">
                                    <label class="form-label">Entrada 2</label>
                                    <input type="time" id="manualE2" class="form-control form-control-sm">
                                </div>
                                <div class="col-6">
                                    <label class="form-label">Saída 2</label>
                                    <input type="time" id="manualS2" class="form-control form-control-sm">
                                </div>
                            </div>
                            <div class="mb-2 mt-2">
                                <label class="form-label">Observação</label>
                                <input type="text" id="manualObs" class="form-control form-control-sm"
                                       maxlength="200">
                            </div>
                            <button class="btn btn-warning btn-sm w-100 mt-1"
                                    onclick="salvarManual()">
                                <i class="fas fa-save me-1"></i>Salvar Registro Manual
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- ===== ABA 2: ESPELHO DO MÊS ===== -->
        <div class="tab-pane fade" id="tabEspelho">
            <div class="table-responsive rounded shadow-custom p-2">
                <table class="table table-bordered table-striped table-espelho" id="tabelaEspelho">
                    <thead class="thead-green">
                        <tr>
                            <th>Data</th>
                            <th>Dia</th>
                            <th>Entrada 1</th>
                            <th>Saída 1</th>
                            <th>Entrada 2</th>
                            <th>Saída 2</th>
                            <th>Total</th>
                            <th>Extras</th>
                            <th>Obs</th>
                            <th></th>
                        </tr>
                    </thead>
                    <tbody id="corpoEspelho">
                        <tr><td colspan="10" class="text-center text-muted">Selecione funcionário e período.</td></tr>
                    </tbody>
                    <tfoot>
                        <tr class="fw-bold">
                            <td colspan="6" class="text-end">Total do mês:</td>
                            <td id="totalHoras" class="text-success">—</td>
                            <td id="totalExtras" class="text-primary">—</td>
                            <td colspan="2"></td>
                        </tr>
                    </tfoot>
                </table>
            </div>
        </div>

        <!-- ===== ABA 3: FALTAS & VALES ===== -->
        <div class="tab-pane fade" id="tabFaltasVales">
            <div class="row g-3">

                <!-- FALTAS -->
                <div class="col-md-6">
                    <div class="card shadow-sm">
                        <div class="card-header bg-danger text-white fw-semibold">
                            <i class="fas fa-user-times me-2"></i>Registrar Falta
                        </div>
                        <div class="card-body">
                            <div class="mb-2">
                                <label class="form-label">Data da Falta</label>
                                <input type="date" id="faltaData" class="form-control form-control-sm">
                            </div>
                            <div class="mb-2">
                                <div class="form-check form-switch">
                                    <input class="form-check-input" type="checkbox"
                                           id="faltaJustificada" role="switch">
                                    <label class="form-check-label" for="faltaJustificada">
                                        Justificada (não desconta)
                                    </label>
                                </div>
                            </div>
                            <div class="mb-3">
                                <label class="form-label">Motivo</label>
                                <input type="text" id="faltaMotivo" class="form-control form-control-sm"
                                       maxlength="200">
                            </div>
                            <button class="btn btn-danger btn-sm w-100"
                                    onclick="registrarFalta()">
                                <i class="fas fa-plus me-1"></i>Registrar Falta
                            </button>
                        </div>
                    </div>

                    <div class="table-responsive rounded shadow-custom mt-3 p-2">
                        <table class="table table-sm table-bordered table-striped">
                            <thead class="thead-green">
                                <tr><th>Data</th><th>Tipo</th><th>Motivo</th><th></th></tr>
                            </thead>
                            <tbody id="corpoFaltas">
                                <tr><td colspan="4" class="text-center text-muted">—</td></tr>
                            </tbody>
                        </table>
                    </div>
                </div>

                <!-- VALES -->
                <div class="col-md-6">
                    <div class="card shadow-sm">
                        <div class="card-header bg-warning fw-semibold">
                            <i class="fas fa-money-bill-wave me-2"></i>Registrar Vale
                        </div>
                        <div class="card-body">
                            <div class="mb-2">
                                <label class="form-label">Data</label>
                                <input type="date" id="valeData" class="form-control form-control-sm">
                            </div>
                            <div class="mb-2">
                                <label class="form-label">Valor (R$)</label>
                                <input type="number" id="valeValor" step="0.01" min="0.01"
                                       class="form-control form-control-sm" placeholder="0,00">
                            </div>
                            <div class="mb-3">
                                <label class="form-label">Descrição</label>
                                <input type="text" id="valeDesc" class="form-control form-control-sm"
                                       maxlength="200">
                            </div>
                            <button class="btn btn-warning btn-sm w-100"
                                    onclick="registrarVale()">
                                <i class="fas fa-plus me-1"></i>Registrar Vale
                            </button>
                        </div>
                    </div>

                    <div class="table-responsive rounded shadow-custom mt-3 p-2">
                        <table class="table table-sm table-bordered table-striped">
                            <thead class="thead-green">
                                <tr><th>Data</th><th>Valor</th><th>Descrição</th><th></th></tr>
                            </thead>
                            <tbody id="corpoVales">
                                <tr><td colspan="4" class="text-center text-muted">—</td></tr>
                            </tbody>
                            <tfoot>
                                <tr class="fw-bold">
                                    <td>Total</td>
                                    <td id="totalValesRodape" colspan="3">—</td>
                                </tr>
                            </tfoot>
                        </table>
                    </div>
                </div>
            </div>
        </div>

        <!-- ===== ABA 4: FECHAMENTO ===== -->
        <div class="tab-pane fade" id="tabFechamento">
            <div class="row g-3">
                <!-- FORMULÁRIO -->
                <div class="col-md-5">
                    <div class="card shadow-sm">
                        <div class="card-header bg-success text-white fw-semibold">
                            <i class="fas fa-calculator me-2"></i>Calcular Fechamento
                        </div>
                        <div class="card-body">
                            <div class="mb-3">
                                <label class="form-label">Salário Base (R$) <span class="text-danger">*</span></label>
                                <div class="input-group">
                                    <span class="input-group-text">R$</span>
                                    <input type="number" id="fechSalario" step="0.01" min="0"
                                           class="form-control" placeholder="2000,00">
                                </div>
                            </div>
                            <div class="mb-3">
                                <label class="form-label">Jornada Diária (horas)</label>
                                <input type="number" id="fechJornada" value="8" min="1" max="12"
                                       class="form-control">
                            </div>
                            <div class="alert alert-info small mb-3">
                                <strong>Fórmula aplicada:</strong><br>
                                Salário Líquido = (Base − Faltas + Extras) − Vales<br>
                                Extra = hora × 150%
                            </div>
                            <button class="btn btn-success w-100" id="btnFechar"
                                    onclick="fecharFolha()">
                                <i class="fas fa-file-invoice-dollar me-1"></i>Gerar Fechamento
                            </button>
                        </div>
                    </div>
                </div>

                <!-- RESULTADO -->
                <div class="col-md-7" id="painelFechamento" style="display:none">
                    <div class="card shadow-sm border-success">
                        <div class="card-header bg-success text-white fw-semibold">
                            <i class="fas fa-receipt me-2"></i>Resumo Financeiro
                            <span id="fechNome" class="float-end small opacity-75"></span>
                        </div>
                        <div class="card-body p-0">
                            <table class="table table-borderless mb-0">
                                <tbody>
                                    <tr><td class="text-muted ps-3">Período</td>
                                        <td class="fw-semibold" id="fechPeriodo">—</td></tr>
                                    <tr><td class="text-muted ps-3">Dias Úteis no Mês</td>
                                        <td id="fechDiasUteis">—</td></tr>
                                    <tr><td class="text-muted ps-3">Dias Trabalhados</td>
                                        <td id="fechDiasTrab">—</td></tr>
                                    <tr><td class="text-muted ps-3">Faltas Injustificadas</td>
                                        <td class="text-danger" id="fechFaltas">—</td></tr>
                                    <tr><td class="text-muted ps-3">Horas Extras</td>
                                        <td class="text-primary" id="fechExtrasHoras">—</td></tr>
                                    <tr class="border-top">
                                        <td class="text-muted ps-3">Salário Base</td>
                                        <td id="fechBase">—</td></tr>
                                    <tr><td class="text-muted ps-3">+ Valor Horas Extras</td>
                                        <td class="text-success" id="fechValorExtras">—</td></tr>
                                    <tr><td class="text-muted ps-3">− Desconto Faltas</td>
                                        <td class="text-danger" id="fechDescFaltas">—</td></tr>
                                    <tr><td class="text-muted ps-3">Salário Bruto</td>
                                        <td class="fw-semibold" id="fechBruto">—</td></tr>
                                    <tr><td class="text-muted ps-3">− Vales/Adiantamentos</td>
                                        <td class="text-danger" id="fechVales">—</td></tr>
                                </tbody>
                            </table>
                            <div class="bg-success text-white p-3 mt-0 rounded-bottom">
                                <div class="d-flex justify-content-between align-items-center">
                                    <strong class="fs-5">Salário Líquido</strong>
                                    <strong class="fs-4" id="fechLiquido">R$ 0,00</strong>
                                </div>
                            </div>
                        </div>
                    </div>
                    <div class="alert alert-success mt-2 small">
                        <i class="fas fa-envelope me-1"></i>
                        Resumo enviado por e-mail ao funcionário.
                    </div>
                </div>
            </div>
        </div>
    </div><!-- /tab-content -->

</div><!-- /container -->

<%@ include file="../../pagina/footer.jsp" %>

</body>
</html>
