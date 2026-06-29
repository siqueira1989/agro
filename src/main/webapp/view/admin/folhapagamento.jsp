<!DOCTYPE html>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
  <title>Agro - Folha de Pagamento</title>
  <%@ include file="../../pagina/importacao.html"%>
</head>
<body>
<%@ include file="../../pagina/menu.jsp"%>

<div class="container-fluid content mt-3">
  <div class="bg-success text-white p-3 rounded mb-4">
    <h2 class="m-0"><i class="fas fa-money-bill-wave me-2"></i>Folha de Pagamento</h2>
  </div>

  <div id="alertPage" class="alert d-none" role="alert"></div>

  <!-- Geração -->
  <div class="card shadow-sm mb-4">
    <div class="card-header bg-success text-white">
      <strong><i class="fas fa-cogs me-2"></i>Gerar Folha</strong>
    </div>
    <div class="card-body">
      <div class="row align-items-end">
        <div class="col-md-4 form-group mb-0">
          <label>Período (mês/ano) *</label>
          <input type="month" class="form-control" id="periodoGerar">
        </div>
        <div class="col-md-3">
          <button class="btn btn-success w-100" id="btnGerarFolha">
            <i class="fas fa-play me-1"></i>Gerar Folha
          </button>
        </div>
      </div>
      <small class="text-muted mt-2 d-block">
        Recalcula e substitui a folha do período selecionado com base nos registros de ponto e cadastros.
      </small>
    </div>
  </div>

  <!-- Visualização -->
  <div class="card shadow-sm">
    <div class="card-header">
      <div class="row align-items-center">
        <div class="col-md-4">
          <strong><i class="fas fa-list me-2"></i>Visualizar Folha</strong>
        </div>
        <div class="col-md-4">
          <input type="month" class="form-control form-control-sm" id="periodoVer">
        </div>
        <div class="col-md-2">
          <button class="btn btn-primary btn-sm w-100" id="btnVerFolha">
            <i class="fas fa-eye me-1"></i>Ver
          </button>
        </div>
        <div class="col-md-2">
          <div class="btn-group w-100">
            <button class="btn btn-success btn-sm" id="btnExcelFolha" title="Exportar Excel">
              <i class="fas fa-file-excel"></i>
            </button>
            <button class="btn btn-danger btn-sm" id="btnPdfFolha" title="Exportar PDF">
              <i class="fas fa-file-pdf"></i>
            </button>
          </div>
        </div>
      </div>
    </div>
    <div class="card-body">
      <table id="tabelaFolha" class="table table-bordered table-striped" style="width:100%">
        <thead class="thead-green">
          <tr>
            <th>Funcionário</th>
            <th>Tipo</th>
            <th>Período</th>
            <th>Valor (R$)</th>
            <th>Gerado em</th>
          </tr>
        </thead>
        <tbody id="corpoTabela"></tbody>
        <tfoot>
          <tr>
            <td colspan="3" class="text-end fw-bold">Total:</td>
            <td id="totalFolha" class="fw-bold text-success"></td>
            <td></td>
          </tr>
        </tfoot>
      </table>
    </div>
  </div>
</div>

<%@ include file="../../pagina/footer.jsp"%>

</body>
</html>
