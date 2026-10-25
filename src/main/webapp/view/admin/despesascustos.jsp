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
    <div class="bg-success text-white p-3 rounded mb-3 d-flex justify-content-between align-items-center flex-wrap">
        <div>
            <h2 class="m-0"><i class="fas fa-pen-to-square me-2"></i>Despesas/Custos</h2>
            <small class="opacity-75">Cadastro dos itens de custo usados no Diário de Campo e na Safra</small>
        </div>
        <button id="btnCadastro" type="button" class="btn btn-primary btn-sm fw-semibold" data-bs-toggle="modal" data-bs-target="#modalCadastro">
            <i class="fas fa-plus me-1"></i> Novo Despesa/Custo
        </button>
    </div>

  <div id="alerta" class="alert d-none" role="alert"></div>

  <!-- KPIs -->
  <div class="row g-3 mb-3">
    <div class="col-6 col-md-4">
      <div class="card shadow-sm card-kpi h-100">
        <div class="card-body py-2">
          <div class="text-muted small">Cadastrados</div>
          <h3 id="kpiDcTotal" class="fw-bold mb-0">—</h3>
          <i class="fas fa-list fa-lg text-success float-end mt-1"></i>
        </div>
      </div>
    </div>
    <div class="col-6 col-md-4">
      <div class="card shadow-sm card-kpi-info h-100">
        <div class="card-body py-2">
          <div class="text-muted small">Fixas</div>
          <h3 id="kpiDcFixo" class="fw-bold mb-0">—</h3>
          <i class="fas fa-thumbtack fa-lg text-primary float-end mt-1"></i>
        </div>
      </div>
    </div>
    <div class="col-6 col-md-4">
      <div class="card shadow-sm card-kpi-warn h-100">
        <div class="card-body py-2">
          <div class="text-muted small">Variáveis</div>
          <h3 id="kpiDcVariavel" class="fw-bold mb-0">—</h3>
          <i class="fas fa-arrows-up-down fa-lg text-warning float-end mt-1"></i>
        </div>
      </div>
    </div>
  </div>

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
          <th>Categoria</th>
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
            <label for="dcCategoria"><i class="fas fa-box me-1 text-muted"></i>Categoria:</label>
            <select id="dcCategoria" name="categoria" class="form-select">
              <option value="GERAL">Geral</option>
              <option value="CAIXA_COLHEITA">Caixa de colheita</option>
              <option value="EMBALAGEM">Embalagem</option>
            </select>
            <small class="text-muted">Caixa de colheita / Embalagem: preço por alimento e área, usado no Diário de Campo e na Colheita.</small>
          </div>
          <div id="dcCamposCaixa" class="d-none">
            <div class="row">
              <div class="col-6 mb-3">
                <label for="dcAlimento"><i class="fas fa-apple-whole me-1 text-muted"></i>Alimento:</label>
                <select id="dcAlimento" name="idAlimento" class="form-select">
                  <option value="">Selecione...</option>
                </select>
              </div>
              <div class="col-6 mb-3">
                <label for="dcArea"><i class="fas fa-map-location-dot me-1 text-muted"></i>Área de Produção:</label>
                <select id="dcArea" name="idArea" class="form-select">
                  <option value="">Selecione...</option>
                </select>
              </div>
            </div>
            <div class="mb-3">
              <label for="dcTipoCaixa"><i class="fas fa-boxes-stacked me-1 text-muted"></i>Tipo de caixa:</label>
              <select id="dcTipoCaixa" class="form-select">
                <option value="">Selecione...</option>
              </select>
              <input type="text" id="dcTipoCaixaOutro" class="form-control mt-2 d-none" maxlength="30" placeholder="Ex.: PLASTICO_20">
            </div>
          </div>
          <div class="mb-3">
            <label for="despesascustos"><i class="fas fa-tag me-1 text-muted"></i>Despesa/Custo:</label>
            <input type="text" id="despesascustos" name="despesascustos" class="form-control" maxlength="50" placeholder="Ex.: Aluguel">
          </div>
          <div class="row">
            <div class="col-6 mb-3">
              <label for="unidade"><i class="fas fa-ruler me-1 text-muted"></i>Unidade:</label>
              <input type="text" id="unidade" name="unidade" class="form-control" maxlength="2" placeholder="Ex.: un">
            </div>
            <div class="col-6 mb-3">
              <label for="valor"><i class="fas fa-sack-dollar me-1 text-muted"></i>Valor:</label>
              <div class="input-group">
                <span class="input-group-text">R$</span>
                <input type="text" id="valor" name="valor" class="form-control" placeholder="0,00">
              </div>
            </div>
          </div>
          <div class="mb-3">
            <label for="tipo"><i class="fas fa-layer-group me-1 text-muted"></i>Tipo:</label>
            <input type="text" id="tipo" name="tipo" class="form-control" maxlength="20" placeholder="Ex.: Venda">
          </div>
          <div class="mb-3">
            <label for="classificacao"><i class="fas fa-arrows-up-down me-1 text-muted"></i>Classificação:</label>
            <select id="classificacao" name="classificacao" class="form-select">
              <option value="">Selecione...</option>
              <option value="FIXO">Fixo</option>
              <option value="VARIAVEL">Variável</option>
            </select>
            <small class="text-muted">Variável: o valor pode mudar a cada compra (ex.: marmita); o valor acima vira apenas referência.</small>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancelar</button>
            <button type="submit" class="btn btn-primary"><i class="fas fa-check me-1"></i>Salvar</button>
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
            <label for="atualizacaoDespesa"><i class="fas fa-tag me-1 text-muted"></i>Despesa/Custo:</label>
            <input type="text" id="atualizacaoDespesa" name="despesascusto" class="form-control" maxlength="50">
          </div>
          <div class="row">
            <div class="col-6 mb-3">
              <label for="atualizacaoUnidade"><i class="fas fa-ruler me-1 text-muted"></i>Unidade:</label>
              <input type="text" id="atualizacaoUnidade" name="unidadedespesacusto" class="form-control" maxlength="2">
            </div>
            <div class="col-6 mb-3">
              <label for="atualizacaoValor"><i class="fas fa-sack-dollar me-1 text-muted"></i>Valor:</label>
              <div class="input-group">
                <span class="input-group-text">R$</span>
                <input type="text" id="atualizacaoValor" name="valordespesacusto" class="form-control" placeholder="0,00">
              </div>
            </div>
          </div>
          <div class="mb-3">
            <label for="atualizacaoTipo"><i class="fas fa-layer-group me-1 text-muted"></i>Tipo:</label>
            <input type="text" id="atualizacaoTipo" name="tipodespesacusto" class="form-control" maxlength="20">
          </div>
          <div class="mb-3">
            <label for="atualizacaoClassificacao"><i class="fas fa-arrows-up-down me-1 text-muted"></i>Classificação:</label>
            <select id="atualizacaoClassificacao" name="classificacao" class="form-select">
              <option value="">Selecione...</option>
              <option value="FIXO">Fixo</option>
              <option value="VARIAVEL">Variável</option>
            </select>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancelar</button>
            <button type="submit" class="btn btn-info text-white"><i class="fas fa-check me-1"></i>Atualizar</button>
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