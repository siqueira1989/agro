<!DOCTYPE html>
<!-- Página embalagem.jsp — Módulo Embalagem (módulo 21, Configuração) -->
<%@ page contentType="text/html;charset=UTF-8" language="java"%>
<html lang="pt-br">
<head>
    <meta charset="UTF-8">
    <title>Agro - Embalagens</title>
    <%@ include file="../../pagina/importacao.html"%>
</head>
<body>
<header>
    <%@ include file="../../pagina/menu.jsp"%>
</header>

<div class="container-fluid content mt-3">
    <div class="bg-success text-white p-3 rounded mb-3 d-flex justify-content-between align-items-center flex-wrap gap-2">
        <h2 class="m-0"><i class="fas fa-box me-1"></i>Embalagens</h2>
        <button type="button" class="btn btn-primary btn-sm" id="ebNovo">
            <i class="fas fa-plus-circle me-1"></i>Nova embalagem
        </button>
    </div>

    <div id="alerta" class="alert d-none" role="alert"></div>

    <p class="text-muted small mb-2">
        Cada embalagem é um insumo da categoria <strong>Embalagem</strong> (unidade cx) com estoque próprio. As compras entram
        por <strong>Entrada</strong> e atualizam o preço médio; o Processar da Colheita dá baixa no estoque e lança o custo
        no diário de Processamento.
    </p>

    <!-- FILTROS -->
    <div class="d-flex flex-wrap gap-2 align-items-center mb-2">
        <select id="ebFiltroAlimento" class="form-select form-select-sm" style="width:220px;"><option value="">Todos os alimentos</option></select>
        <select id="ebFiltroArea" class="form-select form-select-sm" style="width:220px;"><option value="">Todas as áreas</option></select>
        <select id="ebFiltroAtivo" class="form-select form-select-sm" style="width:150px;">
            <option value="true">Ativas</option>
            <option value="false">Desativadas</option>
            <option value="">Todas</option>
        </select>
    </div>

    <!-- TABELA -->
    <div class="table-responsive rounded shadow-custom p-2 bg-white">
        <table class="table table-bordered table-striped table-hover align-middle mb-0">
            <thead class="thead-green">
                <tr>
                    <th>Código</th><th>Descrição</th><th class="text-end">Peso</th><th>Alimento</th><th>Área</th>
                    <th class="text-end">Saldo</th><th class="text-end">Preço médio de compra</th>
                    <th class="text-end">Preço do embalador</th><th>Situação</th><th class="text-end">Ações</th>
                </tr>
            </thead>
            <tbody id="ebTabela"><tr><td colspan="10" class="text-center text-muted py-3">Carregando…</td></tr></tbody>
        </table>
    </div>
</div>

<!-- MODAL: Nova / Editar embalagem -->
<div class="modal fade" id="modalEmbalagem" tabindex="-1">
    <div class="modal-dialog modal-lg"><div class="modal-content">
        <div class="modal-header bg-primary text-white">
            <h5 class="modal-title" id="ebFormTitulo"><i class="fas fa-box me-1"></i>Nova embalagem</h5>
            <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
        </div>
        <div class="modal-body">
            <div id="ebAlertaForm" class="alert d-none"></div>
            <input type="hidden" id="ebId" value="0">
            <div class="row g-2">
                <div class="col-md-6">
                    <label class="form-label" for="ebAlimento">Alimento *</label>
                    <select class="form-select" id="ebAlimento"></select>
                </div>
                <div class="col-md-6">
                    <label class="form-label" for="ebArea">Área de produção *</label>
                    <select class="form-select" id="ebArea"></select>
                </div>
                <div class="col-md-8">
                    <label class="form-label" for="ebDescricao">Descrição *</label>
                    <input type="text" class="form-control" id="ebDescricao" maxlength="80" placeholder="ex.: Papelão 6,5 kg">
                </div>
                <div class="col-md-4">
                    <label class="form-label" for="ebCodigo">Código <span class="text-muted">(opcional)</span></label>
                    <input type="text" class="form-control" id="ebCodigo" maxlength="40" placeholder="gerado da descrição">
                </div>
                <div class="col-md-4">
                    <label class="form-label" for="ebPeso">Peso da caixa (kg) *</label>
                    <input type="text" class="form-control" id="ebPeso" placeholder="ex.: 6,5">
                </div>
                <div class="col-md-4">
                    <label class="form-label" for="ebEmbalador">Preço pago ao embalador (R$/cx) *</label>
                    <input type="text" class="form-control" id="ebEmbalador" placeholder="ex.: 0,40">
                </div>
                <div class="col-md-4">
                    <label class="form-label" for="ebMinimo">Estoque mínimo (cx)</label>
                    <input type="text" class="form-control" id="ebMinimo" placeholder="0">
                </div>
                <div class="col-md-12">
                    <label class="form-label" for="ebFornecedor">Fornecedor padrão <span class="text-muted">(parceiro do tipo Insumo)</span></label>
                    <select class="form-select" id="ebFornecedor"><option value="">— nenhum —</option></select>
                </div>
            </div>
            <div class="mt-2 small text-muted">O preço de compra não é digitado aqui: ele é o <strong>preço médio</strong> das entradas (compras) registradas em <strong>Entrada</strong>.</div>
        </div>
        <div class="modal-footer">
            <button class="btn btn-secondary" data-bs-dismiss="modal"><i class="fas fa-times me-1"></i>Cancelar</button>
            <button class="btn btn-success" id="ebSalvar"><i class="fas fa-save me-1"></i>Salvar</button>
        </div>
    </div></div>
</div>

<!-- MODAL: Entrada (compra) -->
<div class="modal fade" id="modalEbEntrada" tabindex="-1">
    <div class="modal-dialog"><div class="modal-content">
        <div class="modal-header bg-info text-white">
            <h5 class="modal-title"><i class="fas fa-arrow-down me-2"></i>Entrada (compra) — <span id="ebEntNome"></span></h5>
            <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
        </div>
        <div class="modal-body">
            <div id="ebAlertaEntrada" class="alert d-none"></div>
            <input type="hidden" id="ebEntInsumo">
            <div class="alert alert-light border py-1 small mb-2">
                Saldo atual: <strong id="ebEntSaldo">-</strong> · preço médio: <strong id="ebEntMedio">-</strong>
            </div>
            <div class="mb-2">
                <label class="form-label" for="ebEntFornecedor">Fornecedor (tipo insumo) *</label>
                <select class="form-select" id="ebEntFornecedor"></select>
            </div>
            <div class="row g-2">
                <div class="col-md-6">
                    <label class="form-label" for="ebEntQtd">Quantidade (cx) *</label>
                    <input type="text" class="form-control" id="ebEntQtd" placeholder="ex.: 100">
                </div>
                <div class="col-md-6">
                    <label class="form-label" for="ebEntPreco">Preço unitário (R$/cx) *</label>
                    <input type="text" class="form-control" id="ebEntPreco" placeholder="ex.: 1,25">
                </div>
            </div>
            <div class="mb-2 mt-2">
                <label class="form-label" for="ebEntData">Data</label>
                <input type="date" class="form-control" id="ebEntData">
            </div>
            <div class="mb-2">
                <label class="form-label" for="ebEntObs">Observação</label>
                <input type="text" class="form-control" id="ebEntObs" maxlength="200">
            </div>
            <div class="small text-muted" id="ebEntPrevisao"></div>
        </div>
        <div class="modal-footer">
            <button class="btn btn-secondary" data-bs-dismiss="modal"><i class="fas fa-times me-1"></i>Cancelar</button>
            <button class="btn btn-info text-white" id="ebEntSalvar"><i class="fas fa-save me-1"></i>Registrar entrada</button>
        </div>
    </div></div>
</div>

<!-- MODAL: Movimentos (histórico) -->
<div class="modal fade" id="modalEbMovimentos" tabindex="-1">
    <div class="modal-dialog modal-lg modal-fullscreen-sm-down"><div class="modal-content">
        <div class="modal-header bg-dark text-white">
            <h5 class="modal-title"><i class="fas fa-clock-rotate-left me-2"></i>Movimentos — <span id="ebMovNome"></span></h5>
            <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
        </div>
        <div class="modal-body">
            <div class="table-responsive">
                <table class="table table-sm table-hover mb-0">
                    <thead class="table-light"><tr><th>Data</th><th>Tipo</th><th class="text-end">Qtd (cx)</th>
                        <th class="text-end">Preço</th><th>Fornecedor</th><th class="text-end">Saldo</th><th>Obs</th></tr></thead>
                    <tbody id="ebMovBody"></tbody>
                </table>
            </div>
        </div>
    </div></div>
</div>

<%@ include file="../../pagina/footer.jsp"%>
</body>
</html>
