<!DOCTYPE html>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html lang="pt-br">
<head>
  <meta charset="UTF-8">
  <title>Agro - Cadastro de Área de Produção</title>
  <%@ include file="../../pagina/importacao.html"%>
</head>
<body>
<header><%@ include file="../../pagina/menu.jsp"%></header>

<div class="container-fluid content mt-2">
  <div class="card shadow-sm mb-3">
    <div class="card-body bg-success text-white rounded">
      <h4 class="mb-0"><i class="fas fa-plus me-2"></i>Cadastrar Área de Produção</h4>
    </div>
  </div>

  <div id="alertFormArea" class="alert d-none" role="alert"></div>

  <form id="formCadastroArea">
    <!-- DADOS DA PROPRIEDADE -->
    <div class="card shadow-sm mb-3">
      <div class="card-header bg-white"><h5 class="text-success fw-bold mb-0"><i class="fas fa-map-marked-alt me-2"></i>Propriedade</h5></div>
      <div class="card-body">
        <div class="row g-2">
          <div class="col-md-6"><label class="form-label">Proprietário *</label>
            <input type="text" class="form-control" id="proprietarioareaproducao" maxlength="50"></div>
          <div class="col-md-6"><label class="form-label">Propriedade *</label>
            <input type="text" class="form-control" id="propriedadeareaproducao" maxlength="50"></div>
        </div>
        <div class="row g-2 mt-1">
          <div class="col-md-3"><label class="form-label">CEP</label>
            <input type="text" class="form-control" id="cepArea" placeholder="00000-000"></div>
          <div class="col-md-5"><label class="form-label">Endereço</label>
            <input type="text" class="form-control" id="logradouro" readonly></div>
          <div class="col-md-2"><label class="form-label">Número</label>
            <input type="number" class="form-control" id="numeroArea" min="0" value="0"></div>
          <div class="col-md-2"><label class="form-label">UF</label>
            <input type="text" class="form-control" id="uf" readonly></div>
        </div>
        <div class="row g-2 mt-1">
          <div class="col-md-4"><label class="form-label">Bairro</label>
            <input type="text" class="form-control" id="bairro" readonly></div>
          <div class="col-md-4"><label class="form-label">Cidade</label>
            <input type="text" class="form-control" id="cidade" readonly></div>
          <div class="col-md-4"><label class="form-label">Complemento</label>
            <input type="text" class="form-control" id="complementoArea" maxlength="50"></div>
        </div>
        <div class="row g-2 mt-1">
          <div class="col-md-3"><label class="form-label">Siglas * (máx. 4)</label>
            <input type="text" class="form-control" id="siglasareaproducao" maxlength="4"></div>
          <div class="col-md-3"><label class="form-label">Qtd. Plantas (automático)</label>
            <input type="number" class="form-control bg-light" id="quantidadetotalplantasareaproducao" value="0" readonly></div>
        </div>
      </div>
    </div>

    <!-- QUADRAS -->
    <div class="card shadow-sm mb-3">
      <div class="card-header bg-white d-flex justify-content-between align-items-center">
        <h5 class="text-success fw-bold mb-0"><i class="fas fa-th-large me-2"></i>Quadras de Produção</h5>
        <button type="button" class="btn btn-outline-success btn-sm" id="btnAddQuadraRow"><i class="fas fa-plus me-1"></i>Adicionar Quadra</button>
      </div>
      <div class="card-body p-0">
        <div class="table-responsive">
          <table class="table table-sm mb-0 align-middle">
            <thead class="table-light">
              <tr><th>Nome da Quadra</th><th style="width:150px;">Nº de Plantas</th><th style="width:250px;">Tipo de Planta</th><th style="width:60px;"></th></tr>
            </thead>
            <tbody id="quadrasBody"></tbody>
          </table>
        </div>
      </div>
    </div>

    <div class="text-end mb-5">
      <a href="${pageContext.request.contextPath}/view/admin/areaproducao.jsp" class="btn btn-secondary"><i class="fas fa-arrow-left me-1"></i>Voltar</a>
      <button type="button" class="btn btn-success" id="btnSalvarArea"><i class="fas fa-save me-1"></i>Salvar Cadastro</button>
    </div>
  </form>
</div>

<%@ include file="../../pagina/footer.jsp"%>
</body>
</html>
