<!DOCTYPE html>
<%@ page contentType="text/html;charset=UTF-8" language="java"%>
<html lang="pt-br">
<head>
  <meta charset="UTF-8">
  <title>Agro - Maquinário</title>
  <%@ include file="../../pagina/importacao.html"%>
</head>
<body>
<header><%@ include file="../../pagina/menu.jsp"%></header>

<div class="container-fluid content mt-3">
  <div class="bg-success text-white p-3 rounded mb-3 d-flex justify-content-between align-items-center">
    <h2 class="m-0"><i class="fas fa-tractor me-2"></i>Maquinário</h2>
    <button class="btn btn-light btn-sm" id="btnNovoMaquina"><i class="fas fa-plus me-1"></i>Novo Maquinário</button>
  </div>
  <div id="alerta" class="alert d-none" role="alert"></div>

  <div class="card shadow-sm"><div class="card-body p-2">
    <div class="table-responsive">
      <table id="tabelaMaquinas" class="table table-bordered table-striped" style="width:100%">
        <thead class="thead-green"><tr>
          <th>Nome</th><th>Tipo</th><th>Marca</th><th>Identificação</th>
          <th class="text-end">Custo/Hora</th><th class="text-end">Custo/KM</th><th>Situação</th><th class="text-end">Ações</th>
        </tr></thead>
        <tbody></tbody>
      </table>
    </div>
  </div></div>
</div>

<!-- MODAL Cadastro/Edição -->
<div class="modal fade" id="modalMaquina" tabindex="-1"><div class="modal-dialog modal-lg"><div class="modal-content">
  <div class="modal-header bg-success text-white">
    <h5 class="modal-title" id="modalMaquinaTitulo"><i class="fas fa-tractor me-2"></i>Novo Maquinário</h5>
    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
  </div>
  <div class="modal-body">
    <div id="alertaMaquina" class="alert d-none"></div>
    <input type="hidden" id="mqId">
    <div class="row g-2">
      <div class="col-md-6"><label class="form-label">Nome *</label><input class="form-control" id="mqNome" maxlength="80"></div>
      <div class="col-md-3"><label class="form-label">Tipo *</label>
        <select class="form-select" id="mqTipo">
          <option value="TRATOR">Trator</option><option value="IMPLEMENTO">Implemento</option><option value="VEICULO">Veículo</option>
        </select></div>
      <div class="col-md-3"><label class="form-label">Marca</label><input class="form-control" id="mqMarca" maxlength="60"></div>
    </div>
    <div class="row g-2 mt-1">
      <div class="col-md-6"><label class="form-label">Identificação (placa/patrimônio)</label><input class="form-control" id="mqIdent" maxlength="40"></div>
    </div>

    <!-- Custo por HORA (trator/implemento) -->
    <div id="mqBlocoHora" class="mt-2">
      <h6 class="fw-bold text-secondary" id="mqTituloHora">Custo por Hora</h6>
      <div class="row g-2">
        <div class="col-md-3" id="mqCombWrap"><label class="form-label">Combustível/h</label><input type="number" step="0.01" class="form-control mq-comp" id="mqComb" value="0"></div>
        <div class="col-md-3"><label class="form-label">Manutenção/h</label><input type="number" step="0.01" class="form-control mq-comp" id="mqManut" value="0"></div>
        <div class="col-md-3"><label class="form-label">Depreciação/h</label><input type="number" step="0.01" class="form-control mq-comp" id="mqDeprec" value="0"></div>
        <div class="col-md-3"><label class="form-label fw-bold">Custo/Hora (R$) *</label><input type="number" step="0.01" class="form-control" id="mqCustoHora" value="0"></div>
      </div>
      <small class="text-muted" id="mqAjudaHora"></small>
    </div>

    <!-- Custo por KM (veículo) -->
    <div id="mqBlocoKm" class="mt-2 d-none">
      <h6 class="fw-bold text-secondary">Custo por KM (Veículo)</h6>
      <div class="row g-2">
        <div class="col-md-4"><label class="form-label fw-bold">Custo/KM (R$) *</label><input type="number" step="0.01" class="form-control" id="mqCustoKm" value="0"></div>
      </div>
    </div>
  </div>
  <div class="modal-footer">
    <button class="btn btn-secondary" data-bs-dismiss="modal">Cancelar</button>
    <button class="btn btn-success" id="btnSalvarMaquina"><i class="fas fa-save me-1"></i>Salvar</button>
  </div>
</div></div></div>

<%@ include file="../../pagina/footer.jsp"%>
</body>
</html>
