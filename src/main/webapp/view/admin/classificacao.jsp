<%@ page contentType="text/html;charset=UTF-8" language="java"%>
<html>
    <head>
        <title>Info4Cloud</title>
        
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
        <script type="text/javascript" src="../../JS/main.js?<%= System.currentTimeMillis() %>"></script>
 


        <script>
            $(document).ready(function () {
                // Verificando mensagem
                var mensagem = '<%= request.getAttribute("Mensagem") != null ? request.getAttribute("Mensagem") : ""%>';
                var atributo = '<%= request.getAttribute("Atributo") != null ? request.getAttribute("Atributo") : ""%>';

                if (mensagem && atributo) {
                    mostrarAlerta(mensagem, atributo);
                }
                // Chama a função para carregar a classificação após 3 segundos
              // Carregar rapido  
             CarregarClassificacao();
                 setInterval(function () {
            CarregarClassificacao();
        }, 10000);
            });
           
        </script>
    </head>
    <body>
        <header>
            <%@ include file="../../pagina/menu.jsp"%>
        </header>
        <div class="container-fluid content mt-2">
            <h2 class="mb-2 text-white border bg-success p-2 rounded shadow-custom">Classificações</h2>
            <div id="alerta" class="alert d-none " role="alert"></div>
             <button id="btnCadastro" type="button" class="btn btn-primary mt-4" data-toggle="modal" data-target="#modalCadastro">
                <i class="fas fa-plus"></i> Classificação
            </button>
            <div class="table-responsive rounded shadow-custom m-2 p-2">
                
                <table id="tabelaClassificacao" class="table table-bordered table-striped">
                    <thead class="thead-green">
                        <tr>
                            <th>ID</th>
                            <th>Classificação</th>
                            <th>Ações</th>
                        </tr>
                    </thead>
                    <tbody>
                    </tbody>
                </table>
                 
            </div>
           
        </div>



        <!-- Modal de Cadastro de Classificação -->

        <div class="modal fade" id="modalCadastro" tabindex="-1" role="dialog"
             aria-labelledby="modalCadastroLabel" aria-hidden="true">
            <div class="modal-dialog" role="document">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title" id="modalCadastroLabel">Cadastrar Classificação</h5>
                        <button type="button" class="close" data-dismiss="modal"aria-label="Close">
                            <span aria-hidden="true">&times;</span>
                        </button>
                    </div>
                    <div class="modal-body">
                        <form id="formCadastro">
                            <div class="form-group">
                                <label for="classificacao">Classificação</label> 
                                <div class="input-group">
                                    <div class="input-group-prepend">
                                        <span class="input-group-text"><i class=" ml-1 fas fa-list-alt text-info"></i></span>

                                        <input type="text" id="classificacao"  name="classificacao" class="form-control ml-1">
                                    </div>
                                </div>

                            </div>
                        </form>
                    </div>
                    <div class="modal-footer">
                       <button type="button" class="btn btn-primary" onclick="salvarClassificacao()">Salvar</button>
                       <button type="button" class="btn btn-secondary" data-dismiss="modal">Cancelar</button>
                    </div>
                </div>
            </div>
        </div>

        <!-- Modal de Atualização de Classificação -->
        <div class="modal fade" id="modalAtualizacao" tabindex="-1" role="dialog" aria-labelledby="modalAtualizacaoLabel" aria-hidden="true">
            <div class="modal-dialog" role="document">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title" id="modalAtualizacaoLabel">Atualizar Classificação</h5>
                        <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                            <span aria-hidden="true">&times;</span>
                        </button>
                    </div>
                    <div class="modal-body">
                        <form id="formAtualizacao">
                            <input type="hidden" id="atualizacaoId"  name="atualizacaoId">
                            <div class="form-group">
                                <label for="atualizacaoClassificacao">Classificação</label> 
                                <input type="text" class="form-control" id="atualizacaoClassificacao" name="atualizacaoClassificacao" required>
                            </div>
                        </form>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-primary" onclick="atualizarClassificacao()">Atualizar</button>
                        <button type="button" class="btn btn-secondary" data-dismiss="modal">Cancelar</button>
                    </div>
                </div>
            </div>
        </div>

        <!-- Modal de Confirmação de Exclusão -->
        <div class="modal fade" id="modalExclusao" tabindex="-1" role="dialog" aria-labelledby="modalExclusaoLabel" aria-hidden="true">
            <div class="modal-dialog" role="document">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title" id="modalExclusaoLabel">Confirmar Exclusão</h5>
                        <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                            <span aria-hidden="true">&times;</span>
                        </button>
                    </div>
                    <div class="modal-body">
                        <form>
                            <p>Tem certeza de que deseja excluir esta classificação?</p>
                            <input type="hidden" id="exclusaoId" name= "exclusaoId">
                            <input type="hidden" id="deletar" value="Delete">
                         <strong id="exclusaoClassificacao"></strong>
                            <strong id="exclusaoClassificacao"></strong>
                        </form>	
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-danger" onclick="excluirClassificacao()">Excluir</button>
                        <button type="button" class="btn btn-secondary" data-dismiss="modal">Cancelar</button>
                    </div>


                </div>
            </div>
        </div>
    </div>
    <footer>
        <%@ include file="../../pagina/footer.jsp"%>
    </footer>
</body>
</html>
