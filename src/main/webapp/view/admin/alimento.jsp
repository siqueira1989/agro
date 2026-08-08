<!DOCTYPE html>
<!-- Pagina Alimento.jsp -->
<%@ page contentType="text/html;charset=UTF-8" language="java"%>
<html>
    <head>
        <title>Agro - Alimento</title>
        <!-- Bootstrap 4 CSS -->
<%@ include file="../../pagina/importacao.html"%>


    </head>
    <body>
        <header>
            <%@ include file="../../pagina/menu.jsp"%>
        </header>

        <div class="container-fluid content mt-3">
            <div class="bg-success text-white p-3 rounded mb-4">
                <h2 class="m-0"> <i class="fa-solid fa-leaf me-1"></i>Alimento</h2>  
            </div>
          
            <div id="alerta" class="alert d-none" role="alert"></div>

            <!-- Botão para abrir o modal -->
            <button type="button" class="btn btn-primary mt-4 mb-3" data-bs-toggle="modal" data-bs-target="#modalFormulario">
                <i class="fas fa-plus-circle me-1"></i> Novo registro
            </button>
            <!-- Tabela de alimentos -->
            <div class="table-responsive rounded shadow-custom m-2 p-2">
                <table id="tabelaAlimento" class="table table-bordered table-striped">
                    <thead class="thead-green">
                        <tr>
                            <th>ID Alimento</th>
                            <th>Alimento</th>
                            <th>Situação</th>
                            <th>Tipo Alimento</th>
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
                        <h5 class="modal-title" id="tituloModal"><i class="fas fa-leaf me-1"></i> Cadastro de Alimento</h5>
                        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Fechar"></button>
                    </div>

                    <div class="modal-body">
                        
                           <!-- NOVO: contêiner de alerta do modal -->
                            <div id="ModalCadastroAlimento"></div>
                        
                        <div class="mb-3">
                            <label for="alimento" class="form-label">Alimento</label>
                            <div class="input-group">
                                <span class="input-group-text"><i class="fas fa-carrot"></i></span>
                                <input type="text" class="form-control" id="alimento" name="alimento" required>
                                <div class="invalid-feedback">Informe o alimento.</div>
                            </div>
                        </div>

                        <div class="mb-3">
                            <label for="variedade" class="form-label">Variedade</label>
                            <div class="input-group">
                                <span class="input-group-text"><i class="fas fa-seedling"></i></span>
                                <input type="text" class="form-control" id="variedade" name="variedade" required>
                                <input type="hidden" class="form-control" id="tipo" name="tipo" value="Alimento">
                                <div class="invalid-feedback">Informe a variedade.</div>
                            </div>
                        </div>

                        <div class="mb-3">
                            <label for="SelectClassificacao" class="form-label">Classificação</label>
                            <div class="input-group">
                                <span class="input-group-text"><i class="fas fa-tags"></i></span>
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
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal"><i class="fas fa-times"></i> Cancelar</button>
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
