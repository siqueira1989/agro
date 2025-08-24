<!DOCTYPE html>
<%@ page contentType="text/html;charset=UTF-8" language="java"%>
<html>
<head>
    <title>Info4Cloud - Alimento</title>
    <!-- Bootstrap 4 CSS -->
    <link rel="stylesheet" href="https://stackpath.bootstrapcdn.com/bootstrap/4.5.2/css/bootstrap.min.css">
    <!-- jQuery -->
    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <!-- Bootstrap 4 JS -->
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
        // Carrega opções do select de classificação sempre que abrir o modal
        $('#modalFormulario').on('show.bs.modal', function () {
            CarregarClassificacaoModal();
        });

        // Submit do formulário com AJAX
        $('#formAlimento').on('submit', function (event) {
            event.preventDefault();

            // Bootstrap validation
            if (!this.checkValidity()) {
                event.stopPropagation();
                this.classList.add('was-validated');
                return;
            }

            salvarAlimento();
        });
    });

    // Função AJAX para cadastrar alimento
    function salvarAlimento() {
         const alimento = $('#alimento').val();
         const variedade = $('#variedade').val();
         const tipo = $('#tipo').val();
        const classificacoesSelecionadas = $('#SelectClassificacao').val();

        const data = {
            acao: 'create',
            alimento: alimento,
            tipo: tipo,
            variedade: variedade,
            classificacoes: classificacoesSelecionadas
        };

        $.ajax({
            url: '/agro/ControllerAlimento',
            method: 'POST',
            contentType: 'application/json',
            data: JSON.stringify(data),
            success: function (response) {
                $('#modalFormulario').modal('hide');
                $('#formAlimento')[0].reset(); // Limpa campos
                $('#formAlimento').removeClass('was-validated'); // Remove validação do Bootstrap
                $('#tabelaalimentos').DataTable().ajax.reload();
                mostrarAlerta("Alimento cadastrado com sucesso!", 'success');
            },
            error: function (xhr) {
                mostrarAlerta("Erro ao cadastrar alimento", 'danger');
            }
        });
    }

    // Exibe alerta bonito
    function mostrarAlerta(msg, tipo) {
        let alerta = $('#alerta');
        alerta.removeClass('d-none alert-success alert-danger').addClass('alert-' + tipo).text(msg).fadeIn();

        setTimeout(function () {
            alerta.fadeOut(function () {
                alerta.addClass('d-none');
            });
        }, 2500);
    }

    // Função para carregar classificações no select
    function CarregarClassificacaoModal() {
        $.ajax({
            url: '/agro/ClassificacaoServlet',
            method: 'GET',
            dataType: 'json',
            success: function (classificacoes) {
                let select = $('#SelectClassificacao');
                select.empty();
                select.append('<option disabled value="">Selecione uma ou mais classificações...</option>');
                classificacoes.forEach(function (item) {
                    select.append('<option value="' + item.idclassificacao + '">' + item.classificacao + '</option>');
                });
            },
            error: function () {
                mostrarAlerta('Erro ao carregar classificações.', 'danger');
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
        <div id="alerta" class="alert d-none" role="alert"></div>
        <!-- Botão para abrir o modal -->
        <button type="button" class="btn btn-primary mt-4 mb-3" data-toggle="modal" data-target="#modalFormulario">
            <i class="fas fa-plus-circle"></i> Novo registro
        </button>
        <!-- Tabela de alimentos -->
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
    <div class="modal fade" id="modalFormulario" tabindex="-1" role="dialog" aria-labelledby="tituloModal" aria-hidden="true">
        <div class="modal-dialog" role="document">
            <form id="formAlimento" class="modal-content needs-validation" novalidate>
                <div class="modal-header bg-primary text-white">
                    <h5 class="modal-title" id="tituloModal"><i class="fas fa-leaf"></i> Cadastro de Alimento</h5>
                    <button type="button" class="close text-white" data-dismiss="modal" aria-label="Fechar">
                        <span aria-hidden="true">&times;</span>
                    </button>
                </div>

                <div class="modal-body">
                    <div class="form-group">
                        <label for="alimento">Alimento</label>
                        <div class="input-group">
                            <div class="input-group-prepend">
                                <span class="input-group-text"><i class="fas fa-carrot"></i></span>
                            </div>
                            <input type="text" class="form-control" id="alimento" name="alimento" required>
                            <div class="invalid-feedback">Informe o alimento.</div>
                        </div>
                    </div>

                    <div class="form-group">
                        <label for="variedade">Variedade</label>
                        <div class="input-group">
                            <div class="input-group-prepend">
                                <span class="input-group-text"><i class="fas fa-seedling"></i></span>
                            </div>
                            <input type="text" class="form-control" id="variedade" name="variedade" required>
                           <input type="hidden" class="form-control" id="tipo" name="tipo" value="Alimento">
                            <div class="invalid-feedback">Informe a variedade.</div>
                        </div>
                    </div>

                    <div class="form-group">
                        <label for="classificacao">Classificação</label>
                        <div class="input-group">
                            <div class="input-group-prepend">
                                <span class="input-group-text"><i class="fas fa-tags"></i></span>
                            </div>
                            <select id="SelectClassificacao" name="classificacoes" class="form-control" multiple required>
                                <option disabled value="">Selecione uma ou mais classificações...</option>
                                <!-- Opções dinâmicas -->
                            </select>
                            <div class="invalid-feedback">Selecione pelo menos uma classificação.</div>
                        </div>
                    </div>
                </div>

                <div class="modal-footer">
                    <button type="submit" class="btn btn-success"><i class="fas fa-save"></i> Salvar</button>
                    <button type="button" class="btn btn-secondary" data-dismiss="modal"><i class="fas fa-times"></i> Cancelar</button>
                </div>
            </form>
        </div>
    </div>
    <!-- Fim do modal cadastro -->

    <footer>
        <%@ include file="../../pagina/footer.jsp"%>
    </footer>
</body>
</html>
