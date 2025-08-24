<!DOCTYPE html>
<%@ page contentType="text/html;charset=UTF-8" language="java"%>
<html>
    <head>
        <title>Info4Cloud - Alimento</title>
        <!-- Incluindo o CSS do Bootstrap 4 -->
        <link rel="stylesheet" href="https://stackpath.bootstrapcdn.com/bootstrap/4.5.2/css/bootstrap.min.css">
        <!-- jQuery -->
        <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
        <!-- JavaScript do Bootstrap 4 -->
        <script src="https://stackpath.bootstrapcdn.com/bootstrap/4.5.2/js/bootstrap.bundle.min.js"></script>
        <!-- DataTables CSS e JS -->
        <link rel="stylesheet" href="https://cdn.datatables.net/1.11.5/css/jquery.dataTables.min.css">
        <script src="https://cdn.datatables.net/1.11.5/js/jquery.dataTables.min.js"></script>
        <!-- Font Awesome -->
        <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0-beta3/css/all.min.css">
        <!-- CSS customizado -->
        <link href="../../CSS/estilo.css" rel="stylesheet">

        <script>
            $(document).ready(function () {
                // Verificando mensagem
                var mensagem = '<%= request.getAttribute("Mensagem") != null ? request.getAttribute("Mensagem") : ""%>';
                var atributo = '<%= request.getAttribute("Atributo") != null ? request.getAttribute("Atributo") : ""%>';

                if (mensagem && atributo) {
                    // Chama a função mostrarAlerta
                    mostrarAlerta(mensagem, atributo);
                }
                CarregarClassificacao();
            }); // <- Parêntese final adicionado aqui



            // Excluir
            function excluirAlimento() {
                var id = $('#exclusaoId').val();

                $.ajax({
                    url: '/agro/ControllerAlimento',
                    method: 'POST',
                    data: {
                        acao: 'delete',
                        idalimento: id
                    },
                    success: function (response) {
                        mostrarAlerta('Alimento excluída com sucesso!', 'info');
                        $('#modalExclusao').modal('hide');
                        $('#tabelaalimentos').DataTable().ajax.reload();
                    },
                    error: function (xhr) {
                        alert('Atenção: ' + xhr.responseText);
                        mostrarAlerta('Alimento não excluída com sucesso!', 'danger');
                    }
                });
            }

            // Atualizar
            function atualizarAlimento(id, alimento, variedade) { // <- Removida vírgula extra aqui
                $.ajax({
                    url: '/agro/ControllerAlimento',
                    method: 'POST',
                    data: {
                        acao: 'update',
                        idalimentos: id,
                        alimentos: alimento,
                        variedade: variedade // <- Corrigido "Variedade" para "variedade"
                    },
                    success: function (response) {
                        mostrarAlerta('Alimento atualizada com sucesso', 'info');
                        $('#modalAtualizacao').modal('hide');
                        $('#formAtualizacao')[0].reset();
                        $('#tabelaalimentos').DataTable().ajax.reload();
                    },
                    error: function (xhr) {
                        console.log('Atenção: ' + xhr.responseText);
                        mostrarAlerta('Alimento não atualizada!', 'danger');
                    }
                });
            }

            /*Cadastro*/
            function salvarAlimento() {
                const form = document.getElementById('formCadastro');

                // Verifica se o formulário é válido
                if (!form.checkValidity()) {
                    form.classList.add('was-validated'); // Ativa os estilos de validação do Bootstrap
                    return; // Impede o envio
                }


                const alimento = $('#alimento').val();
                const variedade = $('#variedade').val();
                const classificacoesSelecionadas = $('#SelectClassificacao').val();

                // Criação do objeto JSON com a ação separada
                const data = {
                    acao: 'create', // Ação incluída no corpo do JSON
                    alimento: alimento,
                    variedade: variedade,
                    classificacoes: classificacoesSelecionadas // Array de valores selecionados
                };

                console.log("Dados enviados:", data); // Log para depuração

                $.ajax({
                    url: '/agro/ControllerAlimento', // Sem parâmetros na URL
                    method: 'POST',
                    contentType: 'application/json', // Definindo que o conteúdo será JSON
                    data: JSON.stringify(data), // Convertendo o objeto em uma string JSON
                    success: function (response) {
                        console.log("Alimento cadastrado com sucesso:", response);
                        $('#modalCadastro').modal('hide');
                        $('#formCadastro')[0].reset();
                        $('#tabelaalimentos').DataTable().ajax.reload();
                    },
                    error: function (xhr) {
                        console.error("Erro ao cadastrar alimento:", xhr.responseText);
                        mostrarAlerta("Erro ao cadastrar alimento", 'danger');
                    }
                });
            }
            // Abertura do Modal
            function abrirModalAtualizacao(id, alimento, variedade) {
                $('#alimento').val(id);
                $('#alimento').val(alimento);
                $('#variedade').val(variedade);
                $('#modalAtualizacao').modal('show');
            }

            // Mostrar mensagem
            function mostrarAlerta(mensagem, tipo) {
                const alerta = $('#alerta');
                alerta.removeClass('d-none alert-success alert-danger alert-info alert-warning');
                alerta.addClass('alert alert-' + tipo);
                alerta.text(mensagem);
                alerta.fadeIn();
                setTimeout(() => {
                    alerta.fadeOut(() => {
                        alerta.addClass('d-none');
                    });
                }, 5000);
            }

            // Carregar Classificação
            function CarregarClassificacao() {
                $.ajax({
                    url: '/agro/ClassificacaoServlet',
                    method: 'GET',
                    dataType: 'json',
                    success: function (classificacoes) {
                        let selectClassificacao = $('#SelectClassificacao');
                        selectClassificacao.empty(); // Limpa as opções anteriores

                        console.log("Classificações carregadas:", classificacoes);  // Verifique no console se as classificações estão chegando

                        classificacoes.forEach(function (classificacao) {// Variavel  esta dando erro por causa do id
                            selectClassificacao.append('<option value="' + classificacao.idclassificacao + '">' + classificacao.classificacao + '</option>');
                        });
                    },
                    error: function () {
                        console.error('Erro ao carregar as classificações.');
                    }
                });
            }

        </script>

    </head>
    <body>
        <header>
            <%@ include file="../../pagina/menu.jsp"%>
        </header>

        <div class="container-fluid content mt-2">
            <h2 class="mb-2 text-white border bg-success p-2 rounded shadow-custom">Alimento</h2>
            <div id="alerta" class="alert d-none " role="alert"></div>
            <button id="btnCadastro" type="button" class="btn btn-primary mt-4 mb-3" data-toggle="modal" data-target="#modalCadastro">
                <i class="fas fa-plus"></i> <span class="texto-botao">Cadastrar Alimento</span>
            </button>

            <div class="table-responsive rounded shadow-custom m-2 p-2">
                <table id="tabelaalimentos" class="table table-bordered table-striped">
                    <thead class="thead-green">
                        <tr>
                            <th>ID</th>
                            <th>Alimento</th>
                            <th>Variedade</th>
                            <th>Ações</th>
                        </tr>
                    </thead>
                    <tbody>
                    </tbody>
                </table>
            </div>
        </div>

        <!-- Modal de cadastro -->
        <div id="modalCadastro" class="modal fade" tabindex="-1" role="dialog">
            <div class="modal-dialog modal-lg modal-dialog-centered" role="document">
                <div class="modal-content rounded-lg shadow-lg">
                    <div class="modal-header border-0">
                        <h5 class="modal-title text-primary font-weight-bold">
                            <i class="fas fa-plus-circle"></i> Cadastrar Alimento
                        </h5>
                        <button type="button" class="close text-muted" data-dismiss="modal" aria-label="Close">
                            <span aria-hidden="true">&times;</span>
                        </button>
                    </div>
                    <div class="modal-body">
                        <form id="formCadastro" novalidate>
                            <!-- Alimento Input with Icon -->
                            <div class="form-group">
                                <label for="alimento" class="font-weight-semibold">Alimento:</label>
                                <div class="input-group">
                                    <div class="input-group-prepend">
                                        <span class="input-group-text"><i class="fas fa-apple-alt text-success"></i></span>
                                    </div>
                                    <input type="text" id="alimento" name="alimento" class="form-control" placeholder="Ex: Batata" required maxlength="50" pattern="[A-Za-zÀ-ÿ\s]+" title="Apenas letras são permitidas">
                                </div>
                                <div class="invalid-feedback">Por favor, insira o nome do alimento apenas com letras.</div>
                            </div>

                            <!-- Variedade Input with Icon -->
                            <div class="form-group">
                                <label for="variedade" class="font-weight-semibold">Variedade:</label>
                                <div class="input-group">
                                    <div class="input-group-prepend">
                                        <span class="input-group-text"><i class="fas fa-seedling text-warning"></i></span>
                                    </div>
                                    <input type="text" id="variedade" name="variedade" class="form-control" placeholder="Ex: A1" required maxlength="2" pattern="[A-Za-z0-9]+" title="Use letras e números. Máximo de 2 caracteres.">
                                </div>
                                <div class="invalid-feedback">Insira a variedade com até 2 caracteres alfanuméricos.</div>
                            </div>


                            <!-- Classificação Select with Multiple Selection -->
                            <div class="form-group">
                                <label for="SelectClassificacao" class="font-weight-semibold">Classificação:</label>
                                <div class="input-group">
                                    <div class="input-group-prepend">
                                        <span class="input-group-text"><i class="fas fa-list-alt text-info"></i></span>
                                    </div>
                                    <select id="SelectClassificacao" name="classificacoes" class="form-control" multiple required>
                                        <option disabled value="">Selecione uma ou mais classificações...</option>
                                        <!-- As opções devem ser carregadas dinamicamente -->
                                    </select>

                                </div>
                                <div class="invalid-feedback">Por favor, selecione pelo menos uma classificação.</div>
                            </div>
                        </form>
                    </div>
                    <div class="modal-footer border-0">
                        <button type="button" class="btn btn-outline-secondary" data-dismiss="modal">
                            <i class="fas fa-times"></i> Cancelar
                        </button>
                        <button type="button" onclick="salvarAlimento()" class="btn btn-primary">
                            <i class="fas fa-save"></i> Salvar
                        </button>

                    </div>
                </div>
            </div>
        </div>


        <!-- Modal de atualização -->
        <div id="modalAtualizacao" class="modal fade" tabindex="-1" role="dialog">
            <div class="modal-dialog modal-lg modal-dialog-centered" role="document">
                <div class="modal-content rounded-lg shadow-lg">
                    <div class="modal-header border-0">
                        <h5 class="modal-title text-primary font-weight-bold">
                            <i class="fas fa-edit"></i> Atualizar Alimento
                        </h5>
                        <button type="button" class="close text-muted" data-dismiss="modal" aria-label="Close">
                            <span aria-hidden="true">&times;</span>
                        </button>
                    </div>
                    <div class="modal-body">
                        <form id="formAtualizacao" novalidate>
                            <!-- Campo Oculto para ID -->
                            <input type="hidden" id="idalimento" name="id">

                            <!-- Campo Alimento com Icone -->
                            <div class="form-group">
                                <label for="alimento" class="font-weight-semibold">Alimento:</label>
                                <div class="input-group">
                                    <div class="input-group-prepend">
                                        <span class="input-group-text"><i class="fas fa-apple-alt text-success"></i></span>
                                    </div>
                                    <input type="text" id="alimento" name="alimento" class="form-control" placeholder="Ex: Cenoura" required maxlength="50" pattern="[A-Za-zÀ-ÿ\s]+" title="Apenas letras são permitidas">
                                </div>
                                <div class="invalid-feedback">Por favor, insira o nome do alimento apenas com letras.</div>
                            </div>

                            <!-- Campo Variedade com Icone -->
                            <div class="form-group">
                                <label for="variedade" class="font-weight-semibold">Variedade:</label>
                                <div class="input-group">
                                    <div class="input-group-prepend">
                                        <span class="input-group-text"><i class="fas fa-seedling text-warning"></i></span>
                                    </div>
                                    <input type="text" id="variedade" name="variedade" class="form-control" placeholder="Ex: B1" required maxlength="2" pattern="[A-Za-z0-9]+" title="Use letras e números. Máximo de 2 caracteres.">
                                </div>
                                <div class="invalid-feedback">Insira a variedade com até 2 caracteres alfanuméricos.</div>
                            </div>
                            <!-- Classificação Select with Multiple Selection -->
                            <div class="form-group">
                                <label for="SelectClassificacao" class="font-weight-semibold">Classificação:</label>
                                <div class="input-group">
                                    <div class="input-group-prepend">
                                        <span class="input-group-text"><i class="fas fa-list-alt text-info"></i></span>
                                    </div>
                                    <select id="SelectClassificacao" name="classificacoes" class="form-control custom-select" multiple required>
                                        <option disabled value="">Selecione uma ou mais classificações...</option>
                                        <!-- As opções devem ser carregadas dinamicamente -->
                                    </select>
                                </div>
                                <div class="invalid-feedback">Por favor, selecione pelo menos uma classificação.</div>
                            </div>
                        </form>
                    </div>
                    <div class="modal-footer border-0">
                        <button type="button" class="btn btn-outline-secondary" data-dismiss="modal">
                            <i class="fas fa-times"></i> Cancelar
                        </button>
                        <button type="submit" form="formAtualizacao" class="btn btn-primary">
                            <i class="fas fa-save"></i> Atualizar
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
