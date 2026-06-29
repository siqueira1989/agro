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
      <button class="btn btn-light btn-sm" data-bs-toggle="modal" data-bs-target="#modalCadastro">
        <i class="fas fa-plus me-1"></i>Nova Área
      </button>
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
            <th>CEP</th>
            <th>Ações</th>
          </tr>
        </thead>
        <tbody id="corpoTabela"></tbody>
      </table>
  </div>
</div>

<!-- Modal Cadastro -->
<div class="modal fade" id="modalCadastro" tabindex="-1">
  <div class="modal-dialog modal-lg">
    <div class="modal-content">
      <div class="modal-header bg-success text-white">
        <h5 class="modal-title"><i class="fas fa-plus me-2"></i>Nova Área de Produção</h5>
        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Fechar"></button>
      </div>
      <div class="modal-body">
        <div id="alertModalCadastro" class="alert d-none" role="alert"></div>
        <form id="formCadastro">
          <div class="row">
            <div class="col-md-6 form-group">
              <label>Propriedade *</label>
              <input type="text" class="form-control" id="propriedadeareaproducao" maxlength="50" required>
            </div>
            <div class="col-md-6 form-group">
              <label>Proprietário *</label>
              <input type="text" class="form-control" id="proprietarioareaproducao" maxlength="50" required>
            </div>
          </div>
          <div class="row">
            <div class="col-md-3 form-group">
              <label>Sigla * (máx. 4 chars)</label>
              <input type="text" class="form-control" id="siglasareaproducao" maxlength="4" required>
            </div>
            <div class="col-md-3 form-group">
              <label>Qtd. Total Plantas</label>
              <input type="number" class="form-control" id="quantidadetotalplantasareaproducao" min="0" value="0">
            </div>
            <div class="col-md-3 form-group">
              <label>CEP</label>
              <input type="text" class="form-control" id="cep" placeholder="00000-000">
            </div>
            <div class="col-md-3 form-group">
              <label>Número</label>
              <input type="number" class="form-control" id="numero" min="0" value="0">
            </div>
          </div>
          <div class="mb-3">
            <label>Complemento</label>
            <input type="text" class="form-control" id="complemento" maxlength="50">
          </div>
        </form>
      </div>
      <div class="modal-footer">
        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancelar</button>
        <button type="button" class="btn btn-success" id="btnSalvar">
          <i class="fas fa-save me-1"></i>Salvar
        </button>
      </div>
    </div>
  </div>
</div>

<!-- Modal Edição -->
<div class="modal fade" id="modalEdicao" tabindex="-1">
  <div class="modal-dialog modal-lg">
    <div class="modal-content">
      <div class="modal-header bg-primary text-white">
        <h5 class="modal-title"><i class="fas fa-edit me-2"></i>Editar Área de Produção</h5>
        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Fechar"></button>
      </div>
      <div class="modal-body">
        <div id="alertModalEdicao" class="alert d-none" role="alert"></div>
        <input type="hidden" id="editId">
        <form id="formEdicao">
          <div class="row">
            <div class="col-md-6 form-group">
              <label>Propriedade *</label>
              <input type="text" class="form-control" id="editPropriedade" maxlength="50" required>
            </div>
            <div class="col-md-6 form-group">
              <label>Proprietário *</label>
              <input type="text" class="form-control" id="editProprietario" maxlength="50" required>
            </div>
          </div>
          <div class="row">
            <div class="col-md-3 form-group">
              <label>Sigla *</label>
              <input type="text" class="form-control" id="editSigla" maxlength="4" required>
            </div>
            <div class="col-md-3 form-group">
              <label>Qtd. Plantas</label>
              <input type="number" class="form-control" id="editQtdPlantas" min="0">
            </div>
            <div class="col-md-3 form-group">
              <label>CEP</label>
              <input type="text" class="form-control" id="editCep">
            </div>
            <div class="col-md-3 form-group">
              <label>Número</label>
              <input type="number" class="form-control" id="editNumero" min="0">
            </div>
          </div>
          <div class="mb-3">
            <label>Complemento</label>
            <input type="text" class="form-control" id="editComplemento" maxlength="50">
          </div>
        </form>
      </div>
      <div class="modal-footer">
        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancelar</button>
        <button type="button" class="btn btn-primary" id="btnAtualizar">
          <i class="fas fa-save me-1"></i>Atualizar
        </button>
      </div>
    </div>
  </div>
</div>

<%@ include file="../../pagina/footer.jsp"%>

</body>
</html>
