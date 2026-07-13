<%@ page contentType="text/html;charset=UTF-8" language="java"%>
<html>
<head>
    <title>AgroFazenda Admin</title>
    <%@ include file="../../pagina/importacao.html"%>

</head>
<body>
<header>
    <%@ include file="../../pagina/menu.jsp"%>
</header>

<div class="container-fluid content mt-2">
    <div class="bg-success text-white p-3 rounded mb-4">
        <h2 class="m-0"><i class="fas fa-sync me-1"></i>Atualização de Alimento</h2>
    </div>

    <div id="alerta" class="alert d-none" role="alert"></div>

    <!-- Formulário -->
    <div class="card mb-4">
        <div class="card-header">Formulário de Atualização</div>
        <div class="card-body">
            <form>
                <div class="row g-2">
                    <div class="col-md-6">
                        <label for="id">Id Alimento:</label>
                        <input type="text" class="form-control" readonly id="idproduto" name="idproduto">
                    </div>
                    <div class="col-md-12">
                        <label for="alimento">Nome do Alimento:</label>
                        <input type="text" class="form-control" id="alimento" name="alimento">
                    </div>
                    <div class="col-md-12">
                        <label for="variedade">Variedade:</label>
                        <input type="text" class="form-control" id="variedade" name="variedade">
                    </div>
                </div>
                <div class="d-flex justify-content-end">
                    <button type="button" class="btn btn-warning me-2" onclick="atualizarAlimento()">
                        <i class="fas fa-sync-alt"></i> Atualizar
                    </button>
                    <button type="reset" class="btn btn-secondary">Cancelar</button>
                </div>
            </form>
        </div>
    </div>

    <!-- Tabela -->
    <div class="card">
        <div class="card-header">Classificações Cadastradas</div>
        <div class="card-body">
            <div class="mb-3 text-end">
                <button class="btn btn-primary" data-bs-toggle="modal" data-bs-target="#modalClassificacao">
                    <i class="fas fa-plus"></i> Adicionar Classificação
                </button>
            </div>

            <div class="table-responsive rounded shadow-custom p-2">
                <table id="AtualizarCarregamentoClassificacao" class="table table-bordered table-striped">
                    <thead class="thead-green">
                        <tr>
                            <th>Código</th>
                            <th>Classificação</th>
                            <th class="d-none">ID Produto</th>
                        </tr>
                    </thead>
                </table>
            </div>
        </div>
    </div>
</div>

<!-- Modal -->
<div class="modal fade" id="modalClassificacao" tabindex="-1" role="dialog" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered modal-md" role="document">
        <div class="modal-content border-0 shadow">
            <div class="modal-header bg-success text-white">
                <h5 class="modal-title"><i class="fas fa-seedling me-2"></i>Adicionar Classificação</h5>
                <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Fechar"></button>
            </div>
            <div class="modal-body pb-0">
                  <div id="ModalAlimentoClassificacao" class="alert d-none" role="alert"></div>
                <form>
                    <div class="mb-3">
                        <p><strong>Alimento:</strong> <span id="valorAlimento"></span></p>
                        <p><strong>Variedade:</strong> <span id="valorVariedade"></span></p>
                    </div>
                    <div class="mb-3">
                        <label for="classificacaoSelect" class="fw-bold">Classificações disponíveis</label>
                        <select id="classificacaoSelect" class="form-control" multiple></select>
                        <small class="form-text text-muted">Segure <strong>Ctrl</strong> (ou <strong>Cmd</strong> no Mac) para selecionar mais de uma.</small>
                    </div>
                </form>
            </div>
            <div class="modal-footer border-0 pt-0 pb-3 pe-3">
                <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">
                    <i class="fas fa-times me-1"></i>Cancelar
                </button>
                <button type="button" class="btn btn-success" onclick="salvarClassificacaoModalAtualizacaoAlimento()">
                    <i class="fas fa-check me-1"></i>Salvar
                </button>
            </div>
        </div>
    </div>
</div>

<footer>
    <%@ include file="../../pagina/footer.jsp"%>
</footer>
</body>
</html>
