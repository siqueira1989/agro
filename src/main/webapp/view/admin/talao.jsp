<!DOCTYPE html>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
  <title>Agro - Talões</title>
  <%@ include file="../../pagina/importacao.html"%>
</head>
<body>
<%@ include file="../../pagina/menu.jsp"%>

<div class="container-fluid content mt-3">
  <div class="bg-success text-white p-3 rounded mb-4 d-flex justify-content-between align-items-center">
    <h2 class="m-0"><i class="fas fa-seedling me-2"></i>Talões</h2>
    <button class="btn btn-light btn-sm" data-bs-toggle="modal" data-bs-target="#modalCadastro">
      <i class="fas fa-plus me-1"></i>Novo Talão
    </button>
  </div>

  <div id="alertPage" class="alert d-none" role="alert"></div>

  <div class="table-responsive rounded shadow-custom m-2 p-2">
      <table id="tabelaTalao" class="table table-bordered table-striped" style="width:100%">
        <thead class="thead-green">
          <tr>
            <th>Descrição</th>
            <th>Alimento</th>
            <th>Área</th>
            <th>Sigla</th>
            <th>Qtd. Plantas</th>
            <th>Ações</th>
          </tr>
        </thead>
        <tbody id="corpoTabela"></tbody>
      </table>
  </div>
</div>

<!-- Modal Cadastro -->
<div class="modal fade" id="modalCadastro" tabindex="-1">
  <div class="modal-dialog">
    <div class="modal-content">
      <div class="modal-header bg-success text-white">
        <h5 class="modal-title"><i class="fas fa-plus me-2"></i>Novo Talão</h5>
        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Fechar"></button>
      </div>
      <div class="modal-body">
        <div id="alertModalCadastro" class="alert d-none" role="alert"></div>
        <form id="formCadastro">
          <div class="mb-3">
            <label>Descrição *</label>
            <input type="text" class="form-control" id="descricaotalao" maxlength="50" required>
          </div>
          <div class="mb-3">
            <label>Alimento *</label>
            <select class="form-control" id="idproduto" required>
              <option value="">Selecione...</option>
            </select>
          </div>
          <div class="mb-3">
            <label>Área de Produção *</label>
            <select class="form-control" id="idareaproducao" required>
              <option value="">Selecione...</option>
            </select>
          </div>
          <div class="mb-3">
            <label>Qtd. Plantas</label>
            <input type="number" class="form-control" id="quantidadeplantastalao" min="0" value="0">
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
  <div class="modal-dialog">
    <div class="modal-content">
      <div class="modal-header bg-primary text-white">
        <h5 class="modal-title"><i class="fas fa-edit me-2"></i>Editar Talão</h5>
        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Fechar"></button>
      </div>
      <div class="modal-body">
        <div id="alertModalEdicao" class="alert d-none" role="alert"></div>
        <input type="hidden" id="editId">
        <form id="formEdicao">
          <div class="mb-3">
            <label>Descrição *</label>
            <input type="text" class="form-control" id="editDescricao" maxlength="50" required>
          </div>
          <div class="mb-3">
            <label>Alimento *</label>
            <select class="form-control" id="editIdProduto" required>
              <option value="">Selecione...</option>
            </select>
          </div>
          <div class="mb-3">
            <label>Área de Produção *</label>
            <select class="form-control" id="editIdArea" required>
              <option value="">Selecione...</option>
            </select>
          </div>
          <div class="mb-3">
            <label>Qtd. Plantas</label>
            <input type="number" class="form-control" id="editQtdPlantas" min="0">
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

<!-- Modal Financeiro -->
<div class="modal fade" id="modalFinanceiro" tabindex="-1">
  <div class="modal-dialog modal-lg">
    <div class="modal-content">
      <div class="modal-header bg-warning">
        <h5 class="modal-title"><i class="fas fa-dollar-sign me-2"></i>Dados Financeiros</h5>
        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Fechar"></button>
      </div>
      <div class="modal-body">
        <div id="alertModalFinanceiro" class="alert d-none" role="alert"></div>
        <input type="hidden" id="finIdTalao">
        <input type="hidden" id="finIdTalaoFinanceiro">
        <form id="formFinanceiro">
          <div class="row">
            <div class="col-md-4 form-group">
              <label>Safra *</label>
              <input type="text" class="form-control" id="safratalaofinanceiro" maxlength="50">
            </div>
            <div class="col-md-4 form-group">
              <label>Início Safra</label>
              <input type="date" class="form-control" id="iniciosafratalaofinanceiro">
            </div>
            <div class="col-md-4 form-group">
              <label>Término Safra</label>
              <input type="date" class="form-control" id="terminosafratalaofinanceiro">
            </div>
          </div>
          <div class="row">
            <div class="col-md-3 form-group">
              <label>Custos (R$)</label>
              <input type="number" step="0.01" class="form-control" id="custosafratalaofinanceiro" value="0">
            </div>
            <div class="col-md-3 form-group">
              <label>Despesas (R$)</label>
              <input type="number" step="0.01" class="form-control" id="despesassafratalaofinanceiro" value="0">
            </div>
            <div class="col-md-3 form-group">
              <label>Venda Bruta (R$)</label>
              <input type="number" step="0.01" class="form-control" id="vendabrutasafratalaofinanceiro" value="0">
            </div>
            <div class="col-md-3 form-group">
              <label>Venda Líquida (R$)</label>
              <input type="number" step="0.01" class="form-control" id="vendaliquidasafratalaofinanceiro" value="0">
            </div>
          </div>
        </form>
      </div>
      <div class="modal-footer">
        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancelar</button>
        <button type="button" class="btn btn-warning" id="btnSalvarFinanceiro">
          <i class="fas fa-save me-1"></i>Salvar Financeiro
        </button>
      </div>
    </div>
  </div>
</div>

<%@ include file="../../pagina/footer.jsp"%>

</body>
</html>
