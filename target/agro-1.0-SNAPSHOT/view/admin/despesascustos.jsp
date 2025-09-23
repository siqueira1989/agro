<!DOCTYPE html>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
  <title>Agro - Despesas/Custos</title>
  <%@ include file="../../pagina/importacao.html"%>
  <script>
    $(document).ready(function () {
      $('#valor').mask('000.000.000,00', { reverse: true });

      $('#modalAtualizacao').on('shown.bs.modal', function () {
        let campo = $('#atualizacaoValor');
        campo.unmask();
        campo.mask('000.000.000,00', { reverse: true });
      });

      CarregarDadosDespesasCustos();
      setInterval(CarregarDadosDespesasCustos, 10000);

      $('#formCadastro').on('submit', function (e) {
        e.preventDefault();
        let despesa = $('#despesascustos').val().trim();
        let unidade = $('#unidade').val().trim();
        let valor = $('#valor').val().trim();
        let tipo = $('#tipo').val().trim();

        if (!despesa || !unidade || !valor || !tipo) {
          mostrarAlerta('Todos os campos são obrigatórios.', 'danger', '#alertModalCadastro');
          return;
        }
        if (despesa.length > 50 || unidade.length > 2 || tipo.length > 20) {
          mostrarAlerta('Limite de caracteres excedido.', 'warning', '#alertModalCadastro', 4000);
          return;
        }
        if (!/^\d{1,3}(\.\d{3})*,\d{2}$/.test(valor)) {
          mostrarAlerta('Formato inválido de valor.', 'warning', '#alertModalCadastro', 4000);
          return;
        }
        salvarDespesasCustos(despesa, unidade, valor, tipo);
      });

      $('#formAtualizacao').on('submit', function (e) {
        e.preventDefault();
        let id = $('#atualizacaoId').val();
        let despesa = $('#atualizacaoDespesa').val().trim();
        let unidade = $('#atualizacaoUnidade').val().trim();
        let valor = $('#atualizacaoValor').val().trim();
        let tipo = $('#atualizacaoTipo').val().trim();

        if (!despesa || !unidade || !valor || !tipo) {
          ExibirAlerta('Erro', 'Todos os campos são obrigatórios.');
          return;
        }
        atualizarDespesasCustos(id, despesa, unidade, valor, tipo);
      });

      $('#modalCadastro').on('show.bs.modal hidden.bs.modal', function () {
        $('#formCadastro')[0].reset();
        $('#alertModalCadastro').removeClass().addClass('alert d-none').empty();
      });
    });
  </script>
</head>
<body>
<header>
  <%@ include file="../../pagina/menu.jsp" %>
</header>

<div class="container-fluid content mt-2">
    <div class="bg-success text-white p-3 rounded mb-4">
        <h2 class="m-0"> <i class="fas fa-pen-to-square mr-2"></i>Despesas/Custos</h2>  
    </div>

  <div id="alerta" class="alert d-none" role="alert"></div>

  <button id="btnCadastro" type="button" class="btn btn-primary mt-4 mb-3" data-toggle="modal" data-target="#modalCadastro">
    <i class="fas fa-plus"></i> Cadastrar Despesa/Custo
  </button>

  <div class="table-responsive rounded shadow-sm m-2 p-2">
    <table id="tabelaDespesasCustos" class="table table-bordered table-striped table-hover">
      <thead class="thead-dark">
        <tr>
          <th>ID</th>
          <th>Despesa/Custo</th>
          <th>Unidade</th>
          <th>Valor</th>
          <th>Tipo</th>
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
        <h5 class="modal-title"><i class="fas fa-plus-circle mr-2"></i>Cadastrar Despesa/Custo</h5>
        <button type="button" class="close text-white" data-dismiss="modal">
          <span>&times;</span>
        </button>
      </div>
      <div class="modal-body">
        <!--  Alert Modal Cadastro -->
        
        <div id="alertModalCadastro" class="alert d-none" role="alert"></div>
        
        <form id="formCadastro">
          <div class="form-group">
            <label for="despesascustos">Despesa/Custo:</label>
            <input type="text" id="despesascustos" name="despesascustos" class="form-control" maxlength="50" placeholder="Ex.: Aluguel">
          </div>
          <div class="form-group">
            <label for="unidade">Unidade:</label>
            <input type="text" id="unidade" name="unidade" class="form-control" maxlength="2" placeholder="Ex.: un">
          </div>
          <div class="form-group">
            <label for="valor">Valor:</label>
            <input type="text" id="valor" name="valor" class="form-control" placeholder="0,00">
          </div>
          <div class="form-group">
            <label for="tipo">Tipo:</label>
            <input type="text" id="tipo" name="tipo" class="form-control" maxlength="20" placeholder="Ex.: Fixo">
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" data-dismiss="modal">Cancelar</button>
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
        <h5 class="modal-title"><i class="fas fa-edit mr-2"></i>Atualizar Despesa/Custo</h5>
        <button type="button" class="close text-white" data-dismiss="modal">
          <span>&times;</span>
        </button>
      </div>
      <div class="modal-body">
        <div id="alertModalAtualizacao" class="alert d-none" role="alert"></div>
        <form id="formAtualizacao">
          <input type="hidden" id="atualizacaoId" name="id">
          <div class="form-group">
            <label for="atualizacaoDespesa">Despesa/Custo:</label>
            <input type="text" id="atualizacaoDespesa" name="despesascusto" class="form-control" maxlength="50">
          </div>
          <div class="form-group">
            <label for="atualizacaoUnidade">Unidade:</label>
            <input type="text" id="atualizacaoUnidade" name="unidadedespesacusto" class="form-control" maxlength="2">
          </div>
          <div class="form-group">
            <label for="atualizacaoValor">Valor:</label>
            <input type="text" id="atualizacaoValor" name="valordespesacusto" class="form-control" placeholder="0,00">
          </div>
          <div class="form-group">
            <label for="atualizacaoTipo">Tipo:</label>
            <input type="text" id="atualizacaoTipo" name="tipodespesacusto" class="form-control" maxlength="20">
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" data-dismiss="modal">Cancelar</button>
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
        <h5 class="modal-title"><i class="fas fa-trash-alt mr-2"></i>Excluir Despesa/Custo</h5>
        <button type="button" class="close text-white" data-dismiss="modal">
          <span>&times;</span>
        </button>
      </div>
      <div class="modal-body">
        <p>Tem certeza de que deseja excluir a despesa/custo <strong id="exclusaoDespesa"></strong>?</p>
        <input type="hidden" id="exclusaoId" name="id">
      </div>
      <div class="modal-footer">
        <button type="button" class="btn btn-secondary" data-dismiss="modal">Cancelar</button>
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
