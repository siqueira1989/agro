<%@ page contentType="text/html;charset=UTF-8" language="java"%>
<html>
<head>
    <title>Info4Cloud - Alimento</title>
    <%@ include file="../../pagina/importacao.html"%>

    <script>
        $(document).ready(function () {
            const alimentoJson = sessionStorage.getItem('alimentoParaAtualizar');
            console.log("Recebido:", alimentoJson);

            if (alimentoJson) {
                const alimento = JSON.parse(alimentoJson);
                const idproduto = alimento.idproduto;

                // Preenche os campos do formulário
                $('#idproduto').val(idproduto);
                $('#alimento').val(alimento.nomeproduto);
                $('#variedade').val(alimento.variedadealimento);

                console.log("Carregando classificações do produto ID:", idproduto);
                // Garante execução após DOM pronto
                setTimeout(() => CarregarClassificacaoAtualizarAlimento(idproduto), 200);
                CarregarClassificacaoModalAtualizar(idproduto);
            }

            $('#modalClassificacao').on('show.bs.modal', function () {
                $('#valorAlimento').text($('#alimento').val().trim());
                $('#valorVariedade').text($('#variedade').val().trim());
            });
        });
    </script>
</head>
<body>
<header>
    <%@ include file="../../pagina/menu.jsp"%>
</header>

<div class="container-fluid content mt-2">
    <div class="bg-success text-white p-3 rounded mb-4">
        <h2 class="m-0"><i class="fas fa-sync mr-1"></i>Painel Administrativo</h2>
    </div>

    <div id="alerta" class="alert d-none" role="alert"></div>

    <!-- Formulário -->
    <div class="card mb-4">
        <div class="card-header">Formulário de Atualização</div>
        <div class="card-body">
            <form>
                <div class="form-row">
                    <div class="form-group col-md-6">
                        <label for="id">Id Alimento:</label>
                        <input type="text" class="form-control" readonly id="idproduto" name="idproduto">
                    </div>
                    <div class="form-group col-md-12">
                        <label for="alimento">Nome do Alimento:</label>
                        <input type="text" class="form-control" id="alimento" name="alimento">
                    </div>
                    <div class="form-group col-md-12">
                        <label for="variedade">Variedade:</label>
                        <input type="text" class="form-control" id="variedade" name="variedade">
                    </div>
                </div>
                <div class="d-flex justify-content-end">
                    <button type="submit" class="btn btn-warning mr-2">
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
            <div class="mb-3 text-right">
                <button class="btn btn-primary" data-toggle="modal" data-target="#modalClassificacao">
                    <i class="fas fa-plus"></i> Adicionar Classificação
                </button>
            </div>

            <div class="table-responsive">
                <table id="AtualizarCarregamentoClassificacao" class="table table-bordered table-hover">
                    <thead class="thead-light">
                        <tr>
                            <th>Código</th>
                            <th>Classificação</th>
                            <th class="d-none">ID Produto</th>
                            <th class="text-center">Ações</th>
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
                <h5 class="modal-title"><i class="fas fa-seedling mr-2"></i>Adicionar Classificação</h5>
                <button type="button" class="close text-white" data-dismiss="modal" aria-label="Fechar">
                    <span aria-hidden="true">&times;</span>
                </button>
            </div>
            <div class="modal-body pb-0">
                <form>
                    <div class="form-group">
                        <p><strong>Alimento:</strong> <span id="valorAlimento"></span></p>
                        <p><strong>Variedade:</strong> <span id="valorVariedade"></span></p>
                    </div>
                    <div class="form-group">
                        <label for="classificacaoSelect" class="font-weight-bold">Classificações disponíveis</label>
                        <select id="classificacaoSelect" class="form-control" multiple></select>
                        <small class="form-text text-muted">Segure <strong>Ctrl</strong> (ou <strong>Cmd</strong> no Mac) para selecionar mais de uma.</small>
                    </div>
                </form>
            </div>
            <div class="modal-footer border-0 pt-0 pb-3 pr-3">
                <button type="button" class="btn btn-outline-secondary" data-dismiss="modal">
                    <i class="fas fa-times mr-1"></i>Cancelar
                </button>
                <button type="button" class="btn btn-success" onclick="salvarClassificacaoModal()">
                    <i class="fas fa-check mr-1"></i>Salvar
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
