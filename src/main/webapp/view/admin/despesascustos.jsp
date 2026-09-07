<!DOCTYPE html>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
  <title>Agro - Despesas/Custos</title>
  <%@ include file="../../pagina/importacao.html"%>
</head>
<body>
<header>
  <%@ include file="../../pagina/menu.jsp" %>
</header>

<div class="container-fluid content mt-2">
    <div class="bg-success text-white p-3 rounded mb-4">
        <h2 class="m-0"> <i class="fas fa-pen-to-square me-2"></i>Despesas/Custos</h2>  
    </div>

  <div id="alerta" class="alert d-none" role="alert"></div>

  <button id="btnCadastro" type="button" class="btn btn-primary mt-4 mb-3" data-bs-toggle="modal" data-bs-target="#modalCadastro">
    <i class="fas fa-plus"></i> Cadastrar Despesa/Custo
  </button>

  <div class="table-responsive rounded shadow-custom m-2 p-2">
    <table id="tabelaDespesasCustos" class="table table-bordered table-striped">
      <thead class="thead-green">
        <tr>
          <th>ID</th>
          <th>Despesa/Custo</th>
          <th>Unidade</th>
          <th>Valor</th>
          <th>Tipo</th>
          <th>Classificação</th>
          <th>Ações</th>
        </tr>
      </thead>
      <tbody></tbody>
    </table>
  </div>
</div>

<!-- Modal Cadastro -->
<div class="modal fade" id="modalCadastro" tabindex="-1" role="dialog">
  <div class="modal-dialog modal-dialog-centered" role="document">
    <div class="modal-content">
      <div class="modal-header bg-primary text-white">
        <h5 class="modal-title"><i class="fas fa-plus-circle me-2"></i>Cadastrar Despesa/Custo</h5>
        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Fechar"></button>
      </div>
      <div class="modal-body">
        <!--  Alert Modal Cadastro -->
        
        <div id="alertModalCadastro" class="alert d-none" role="alert"></div>
        
        <form id="formCadastro">
          <div class="mb-3">
            <label for="despesascustos">Despesa/Custo:</label>
            <input type="text" id="despesascustos" name="despesascustos" class="form-control" maxlength="50" placeholder="Ex.: Aluguel">
          </div>
          <div class="mb-3">
            <label for="unidade">Unidade:</label>
            <input type="text" id="unidade" name="unidade" class="form-control" maxlength="2" placeholder="Ex.: un">
          </div>
          <div class="mb-3">
            <label for="valor">Valor:</label>
            <input type="text" id="valor" name="valor" class="form-control" placeholder="0,00">
          </div>
          <div class="mb-3">
            <label for="tipo">Tipo:</label>
            <input type="text" id="tipo" name="tipo" class="form-control" maxlength="20" placeholder="Ex.: Venda">
          </div>
          <div class="mb-3">
            <label for="classificacao">Classificação:</label>
            <select id="classificacao" name="classificacao" class="form-select">
              <option value="">Selecione...</option>
              <option value="FIXO">Fixo</option>
              <option value="VARIAVEL">Variável</option>
            </select>
            <small class="text-muted">Variável: o valor pode mudar a cada compra (ex.: marmita); o valor acima vira apenas referência.</small>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancelar</button>
            <button type="submit" class="btn btn-primary">Salvar</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</div>

<!-- Modal Atualização -->
<div class="modal fade" id="modalAtualizacao" tabindex="-1" role="dialog">
  <div class="modal-dialog modal-dialog-centered" role="document">
    <div class="modal-content">
      <div class="modal-header bg-warning text-white">
        <h5 class="modal-title"><i class="fas fa-edit me-2"></i>Atualizar Despesa/Custo</h5>
        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Fechar"></button>
      </div>
      <div class="modal-body">
        <div id="alertModalAtualizacao" class="alert d-none" role="alert"></div>
        <form id="formAtualizacao">
          <input type="hidden" id="atualizacaoId" name="id">
          <div class="mb-3">
            <label for="atualizacaoDespesa">Despesa/Custo:</label>
            <input type="text" id="atualizacaoDespesa" name="despesascusto" class="form-control" maxlength="50">
          </div>
          <div class="mb-3">
            <label for="atualizacaoUnidade">Unidade:</label>
            <input type="text" id="atualizacaoUnidade" name="unidadedespesacusto" class="form-control" maxlength="2">
          </div>
          <div class="mb-3">
            <label for="atualizacaoValor">Valor:</label>
            <input type="text" id="atualizacaoValor" name="valordespesacusto" class="form-control" placeholder="0,00">
          </div>
          <div class="mb-3">
            <label for="atualizacaoTipo">Tipo:</label>
            <input type="text" id="atualizacaoTipo" name="tipodespesacusto" class="form-control" maxlength="20">
          </div>
          <div class="mb-3">
            <label for="atualizacaoClassificacao">Classificação:</label>
            <select id="atualizacaoClassificacao" name="classificacao" class="form-select">
              <option value="">Selecione...</option>
              <option value="FIXO">Fixo</option>
              <option value="VARIAVEL">Variável</option>
            </select>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancelar</button>
            <button type="submit" class="btn btn-info">Atualizar</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</div>

<!-- Modal Exclusao -->
<div class="modal fade" id="modalExclusao" tabindex="-1" role="dialog">
  <div class="modal-dialog modal-dialog-centered" role="document">
    <div class="modal-content">
      <div class="modal-header bg-danger text-white">
        <h5 class="modal-title"><i class="fas fa-trash-alt me-2"></i>Excluir Despesa/Custo</h5>
        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Fechar"></button>
      </div>
      <div class="modal-body">
        <p>Tem certeza de que deseja excluir a despesa/custo <strong id="exclusaoDespesa"></strong>?</p>
        <input type="hidden" id="exclusaoId" name="id">
      </div>
      <div class="modal-footer">
        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancelar</button>
        <button type="button" class="btn btn-danger" onclick="excluirDespesasCustos()">Excluir</button>
      </div>
    </div>
  </div>
</div>

<footer>
  <%@ include file="../../pagina/footer.jsp" %>
</footer>
</body>
</html>