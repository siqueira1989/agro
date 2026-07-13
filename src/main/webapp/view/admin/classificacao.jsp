<%@ page contentType="text/html;charset=UTF-8" language="java"%>
<html>
    <head>
        <title>Agro - Classificações</title>
        <meta name="viewport" content="width=device-width, initial-scale=1">
         <%@ include file="../../pagina/importacao.html"%>

    </head>
    <body>
        <header>
            <%@ include file="../../pagina/menu.jsp"%>
        </header>

        <div class="container-fluid content mt-3">
            <div class="bg-success text-white p-3 rounded mb-4">
                <h2 class="m-0"><i class="fas fa-tags"></i> Classificações</h2>
            </div>

            <!--  Alert principal -->

            <div id="alerta" class="alert d-none" role="alert"></div>

            <div class="mb-3">
                <button id="btnCadastro" class="btn btn-primary" data-bs-toggle="modal" data-bs-target="#modalCadastro">
                    <i class="fas fa-plus"></i> Nova Classificação
                </button>
            </div>

            <div class="table-responsive rounded shadow-custom m-2 p-2">
                <table id="tabelaClassificacao" class="table table-bordered table-striped">
                    <thead class="thead-green">
                        <tr>
                            <th>ID</th>
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
            <div class="modal-dialog" role="document">
                <div class="modal-content">
                    <div class="modal-header bg-primary text-white">
                        <h5 class="modal-title"> <i class="fas fa-plus-circle me-2"></i>Cadastrar Classificação</h5>
                        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Fechar"></button>
                    </div>
                    <form id="formCadastro" class="needs-validation" novalidate>
                        <div class="modal-body">

                            <!--  Alert Modal Cadastro -->

                            <div id="modalClassificacaoCadastro" class="alert d-none" role="alert"></div>

                            <div class="mb-3">
                                <label for="classificacao">Classificação</label>
                                <div class="input-group">
                                    <span class="input-group-text"><i class="fas fa-list-alt text-info"></i></span>
                                    </div>
                                    <input type="text" id="classificacao" name="classificacao" class="form-control" required>
                                    <div class="invalid-feedback">Informe a classificação.</div>
                                </div>
                            </div>
                            <div id="modalClassificacaoCadastro" class="mt-2"></div>
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-primary me-2" onclick="salvarClassificacao()">
                                <i class="fas fa-save"></i> Salvar
                            </button>
                            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">
                                <i class="fas fa-times"></i> Cancelar
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        </div>

        <!-- Modal Atualização -->
        <div class="modal fade" id="modalAtualizacao" tabindex="-1" role="dialog">
            <div class="modal-dialog" role="document">
                <div class="modal-content">
                    <div class="modal-header bg-warning text-white">
                        <h5 class="modal-title"><i class="fas fa-edit me-2"></i>Atualizar Classificação</h5>
                        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Fechar"></button>
                    </div>
                    <form id="formAtualizacao" class="needs-validation" novalidate>
                        <div class="modal-body">
                            
                             <!--  Alert Modal Cadastro -->

                            <div id="modalClassificacaoAtualizar" class="alert d-none" role="alert"></div>
                            
                            <input type="hidden" id="atualizacaoId" name="atualizacaoId">
                            <div class="mb-3">
                                <label for="atualizacaoClassificacao">Classificação</label>
                                <input type="text" id="atualizacaoClassificacao" name="atualizacaoClassificacao" class="form-control" required>
                                <div class="invalid-feedback">Informe a classificação.</div>
                            </div>
                            <div id="modalClassificacaoAtualizar" class="mt-2"></div>
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-primary me-2" onclick="atualizarClassificacao()">
                                <i class="fas fa-sync-alt"></i> Atualizar
                            </button>
                            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">
                                <i class="fas fa-times"></i> Cancelar
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        </div>

        <!-- Modal Exclusão -->
        <div class="modal fade" id="modalExclusao" tabindex="-1" role="dialog">
            <div class="modal-dialog" role="document">
                <div class="modal-content">
                    
                    <div class="modal-header bg-danger text-white">
                           
                        <h5 class="modal-title">Confirmar Exclusão</h5>
                        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Fechar"></button>
                    </div>
                    <div class="modal-body p-3">
                          <!--  Alert Modal Cadastro -->

                            <div id="modalClassificacaoExcluir" class="alert d-none" role="alert"></div>
                        <p>Tem certeza de que deseja excluir esta classificação?</p>
                        <input type="hidden" id="exclusaoId" name="exclusaoId">
                        <input type="hidden" id="deletar" value="Delete">
                        <strong id="exclusaoClassificacao"></strong>
                        <div id="modalClassificacaoExcluir" class="mt-2"></div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-danger me-2" onclick="excluirClassificacao()">
                            <i class="fas fa-trash"></i> Excluir
                        </button>
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">
                            <i class="fas fa-times"></i> Cancelar
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