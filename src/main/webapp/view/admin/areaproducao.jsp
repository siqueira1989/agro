<!DOCTYPE html>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
  <title>Agro - Áreas de Produção</title>
  <%@ include file="../../pagina/importacao.html"%>
</head>
<body>
<%@ include file="../../pagina/menu.jsp"%>

<div class="container-fluid content mt-3">
  <div class="bg-success text-white p-3 rounded mb-4 d-flex justify-content-between align-items-center">
    <h2 class="m-0"><i class="fas fa-map-marked-alt me-2"></i>Áreas de Produção</h2>
    <div>
      <a href="${pageContext.request.contextPath}/RelatorioExportServlet?tipo=excel&modulo=areaproducao"
         class="btn btn-light btn-sm me-1">
        <i class="fas fa-file-excel me-1 text-success"></i>Excel
      </a>
      <a href="${pageContext.request.contextPath}/RelatorioExportServlet?tipo=pdf&modulo=areaproducao"
         class="btn btn-light btn-sm me-2">
        <i class="fas fa-file-pdf me-1 text-danger"></i>PDF
      </a>
      <a href="${pageContext.request.contextPath}/view/admin/CadastroAreaProducao.jsp" class="btn btn-light btn-sm">
        <i class="fas fa-plus me-1"></i>Nova Área
      </a>
    </div>
  </div>

  <div id="alertPage" class="alert d-none" role="alert"></div>

  <div class="table-responsive rounded shadow-custom m-2 p-2">
      <table id="tabelaAreas" class="table table-bordered table-striped" style="width:100%">
        <thead class="thead-green">
          <tr>
            <th>Propriedade</th>
            <th>Proprietário</th>
            <th>Sigla</th>
            <th>Qtd. Plantas</th>
            <th>Situação</th>
            <th>CEP</th>
            <th>Ações</th>
          </tr>
        </thead>
        <tbody></tbody>
      </table>
  </div>
</div>

<!-- Modal Desativar/Ativar -->
<div class="modal fade" id="modalDesativarArea" tabindex="-1">
  <div class="modal-dialog">
    <div class="modal-content">
      <div class="modal-header bg-primary text-white">
        <h5 class="modal-title" id="modalDesativarAreaLabel">Confirmar Ação</h5>
        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
      </div>
      <div class="modal-body">
        <p id="mensagemDesativarArea" class="mb-0">Alterar a situação desta área?</p>
      </div>
      <div class="modal-footer">
        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancelar</button>
        <button type="button" class="btn btn-danger" id="btnConfirmarDesativarArea">Confirmar</button>
      </div>
    </div>
  </div>
</div>

<%@ include file="../../pagina/footer.jsp"%>
</body>
</html>
