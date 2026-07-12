<!DOCTYPE html>
<%@ page contentType="text/html;charset=UTF-8" language="java"%>
<html lang="pt-br">
<head>
    <meta charset="UTF-8">
    <title>Agro - Estoque de Insumos</title>
    <%@ include file="../../pagina/importacao.html"%>
</head>
<body>
<header>
    <%@ include file="../../pagina/menu.jsp"%>
</header>

<div class="container-fluid content mt-2">
    <h2 class="mb-3 text-white bg-success p-2 rounded shadow-custom">
        <i class="fas fa-warehouse me-2"></i>Estoque de Insumos
    </h2>
    <div id="alerta" class="alert d-none" role="alert"></div>

    <!-- CARDS RESUMO -->
    <div class="row g-3 mb-3">
        <div class="col-md-4">
            <div class="card shadow-sm border-0 bg-success text-white"><div class="card-body text-center py-3">
                <div class="fs-3 fw-bold" id="cardTotalInsumos">0</div><div class="small">Insumos Ativos</div>
            </div></div>
        </div>
        <div class="col-md-4">
            <div class="card shadow-sm border-0 bg-danger text-white"><div class="card-body text-center py-3">
                <div class="fs-3 fw-bold" id="cardAbaixoMinimo">0</div><div class="small">Abaixo do Mínimo</div>
            </div></div>
        </div>
        <div class="col-md-4">
            <div class="card shadow-sm border-0 bg-primary text-white"><div class="card-body text-center py-3">
                <div class="fs-3 fw-bold" id="cardValorEstoque">R$ 0,00</div><div class="small">Valor em Estoque</div>
            </div></div>
        </div>
    </div>

    <!-- AÇÕES -->
    <div class="d-flex justify-content-between align-items-center mb-2 flex-wrap gap-2">
        <div class="d-flex align-items-center gap-2">
            <label class="form-label mb-0 fw-semibold">Categoria:</label>
            <select id="filtroCategoria" class="form-select form-select-sm" style="width:180px;">
                <option value="">Todas</option>
                <option value="DEFENSIVO">Defensivos</option>
                <option value="ADUBO">Adubos</option>
            </select>
        </div>
        <div class="d-flex gap-2">
            <button class="btn btn-info text-white btn-sm" id="btnEntrada"><i class="fas fa-arrow-down me-1"></i>Entrada</button>
            <button class="btn btn-warning btn-sm" id="btnSaida"><i class="fas fa-arrow-up me-1"></i>Saída</button>
            <button class="btn btn-success btn-sm" id="btnNovoInsumo"><i class="fas fa-plus me-1"></i>Novo Insumo</button>
        </div>
    </div>

    <!-- TABELA -->
    <div class="card shadow-sm">
        <div class="card-body p-2">
            <div class="table-responsive">
                <table id="tabelaInsumos" class="table table-bordered table-striped table-hover mb-0">
                    <thead class="table-light">
                        <tr>
                            <th>Insumo</th><th>Categoria</th><th class="text-end">Qtd. Disponível</th>
                            <th class="text-end">Preço Médio</th><th class="text-end">Mínimo</th>
                            <th>Situação</th><th class="text-end">Ações</th>
                        </tr>
                    </thead>
                    <tbody></tbody>
                </table>
            </div>
        </div>
    </div>
</div>

<!-- MODAL: Novo/Editar Insumo -->
<div class="modal fade" id="modalInsumo" tabindex="-1">
    <div class="modal-dialog"><div class="modal-content">
        <div class="modal-header bg-success text-white">
            <h5 class="modal-title" id="modalInsumoTitulo"><i class="fas fa-flask me-2"></i>Novo Insumo</h5>
            <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
        </div>
        <div class="modal-body">
            <div id="alertaInsumo" class="alert d-none"></div>
            <input type="hidden" id="insId">
            <div class="mb-2">
                <label class="form-label">Nome *</label>
                <input type="text" class="form-control" id="insNome" maxlength="80">
            </div>
            <div class="row g-2">
                <div class="col-md-6">
                    <label class="form-label">Categoria *</label>
                    <select class="form-select" id="insCategoria">
                        <option value="DEFENSIVO">Defensivo</option>
                        <option value="ADUBO">Adubo</option>
                    </select>
                </div>
                <div class="col-md-3">
                    <label class="form-label">Grandeza *</label>
                    <select class="form-select" id="insGrandeza">
                        <option value="MASSA">Massa</option>
                        <option value="CAPACIDADE">Capacidade</option>
                        <option value="UNIDADE">Unidade</option>
                    </select>
                </div>
                <div class="col-md-3">
                    <label class="form-label">Unidade *</label>
                    <select class="form-select" id="insUnidade"></select>
                </div>
            </div>
            <div class="row g-2 mt-1">
                <div class="col-md-6">
                    <label class="form-label">Estoque Mínimo</label>
                    <input type="number" class="form-control" id="insMinimo" min="0" step="0.001" value="0">
                </div>
                <div class="col-md-6">
                    <label class="form-label">Fornecedor padrão (insumo)</label>
                    <select class="form-select" id="insFornecedor"><option value="">— nenhum —</option></select>
                </div>
            </div>
            <div class="mt-2 small text-muted">Quantidade e preço médio são geridos pelas movimentações (Entrada/Saída).</div>
        </div>
        <div class="modal-footer">
            <button class="btn btn-secondary" data-bs-dismiss="modal">Cancelar</button>
            <button class="btn btn-success" id="btnSalvarInsumo"><i class="fas fa-save me-1"></i>Salvar</button>
        </div>
    </div></div>
</div>

<!-- MODAL: Entrada -->
<div class="modal fade" id="modalEntrada" tabindex="-1">
    <div class="modal-dialog"><div class="modal-content">
        <div class="modal-header bg-info text-white">
            <h5 class="modal-title"><i class="fas fa-arrow-down me-2"></i>Entrada de Insumo</h5>
            <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
        </div>
        <div class="modal-body">
            <div id="alertaEntrada" class="alert d-none"></div>
            <div class="mb-2">
                <label class="form-label">Insumo *</label>
                <select class="form-select" id="entInsumo"></select>
            </div>
            <div class="mb-2">
                <label class="form-label">Fornecedor (tipo insumo) *</label>
                <select class="form-select" id="entFornecedor"></select>
            </div>
            <div class="row g-2">
                <div class="col-md-6">
                    <label class="form-label">Quantidade *</label>
                    <input type="number" class="form-control" id="entQtd" min="0.001" step="0.001">
                </div>
                <div class="col-md-6">
                    <label class="form-label">Preço unitário (R$) *</label>
                    <input type="number" class="form-control" id="entPreco" min="0" step="0.01">
                </div>
            </div>
            <div class="mb-2 mt-2">
                <label class="form-label">Data</label>
                <input type="date" class="form-control" id="entData">
            </div>
            <div class="mb-2">
                <label class="form-label">Observação</label>
                <input type="text" class="form-control" id="entObs" maxlength="200">
            </div>
        </div>
        <div class="modal-footer">
            <button class="btn btn-secondary" data-bs-dismiss="modal">Cancelar</button>
            <button class="btn btn-info text-white" id="btnSalvarEntrada"><i class="fas fa-save me-1"></i>Registrar Entrada</button>
        </div>
    </div></div>
</div>

<!-- MODAL: Saída -->
<div class="modal fade" id="modalSaida" tabindex="-1">
    <div class="modal-dialog"><div class="modal-content">
        <div class="modal-header bg-warning text-dark">
            <h5 class="modal-title"><i class="fas fa-arrow-up me-2"></i>Saída de Insumo</h5>
            <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
        </div>
        <div class="modal-body">
            <div id="alertaSaida" class="alert d-none"></div>
            <div class="mb-2">
                <label class="form-label">Insumo *</label>
                <select class="form-select" id="saiInsumo"></select>
            </div>
            <div class="alert alert-light border py-1 small mb-2">
                Disponível: <strong id="saiDisponivel">-</strong>
            </div>
            <div class="mb-2">
                <label class="form-label">Quantidade *</label>
                <input type="number" class="form-control" id="saiQtd" min="0.001" step="0.001">
            </div>
            <div class="mb-2">
                <label class="form-label">Data</label>
                <input type="date" class="form-control" id="saiData">
            </div>
            <div class="mb-2">
                <label class="form-label">Observação</label>
                <input type="text" class="form-control" id="saiObs" maxlength="200">
            </div>
        </div>
        <div class="modal-footer">
            <button class="btn btn-secondary" data-bs-dismiss="modal">Cancelar</button>
            <button class="btn btn-warning" id="btnSalvarSaida"><i class="fas fa-save me-1"></i>Registrar Saída</button>
        </div>
    </div></div>
</div>

<!-- MODAL: Movimentações -->
<div class="modal fade" id="modalMovimentos" tabindex="-1">
    <div class="modal-dialog modal-lg"><div class="modal-content">
        <div class="modal-header bg-dark text-white">
            <h5 class="modal-title"><i class="fas fa-clock-rotate-left me-2"></i>Movimentações — <span id="movInsumoNome"></span></h5>
            <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
        </div>
        <div class="modal-body">
            <div class="table-responsive">
                <table class="table table-sm table-hover mb-0">
                    <thead class="table-light"><tr><th>Data</th><th>Tipo</th><th class="text-end">Qtd</th>
                        <th class="text-end">Preço</th><th>Fornecedor</th><th class="text-end">Saldo</th><th>Obs</th></tr></thead>
                    <tbody id="bodyMovimentos"><tr><td colspan="7" class="text-center text-muted py-3">Sem movimentações.</td></tr></tbody>
                </table>
            </div>
        </div>
    </div></div>
</div>

<%@ include file="../../pagina/footer.jsp"%>
</body>
</html>
