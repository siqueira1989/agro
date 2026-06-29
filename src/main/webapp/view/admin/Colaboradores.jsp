<!DOCTYPE html>
<%@ page contentType="text/html;charset=UTF-8" language="java"%>
<html lang="pt-br">
<head>
    <title>Agro - Colaboradores</title>
    <%@ include file="../../pagina/importacao.html"%>

</head>
<body>
<header>
    <%@ include file="../../pagina/menu.jsp"%>
</header>

<div class="container-fluid content mt-3">

    <div class="bg-success text-white p-3 rounded mb-4">
        <h2 class="m-0"><i class="fas fa-users me-2"></i>Colaboradores</h2>
    </div>

    <div id="alerta" class="alert d-none" role="alert"></div>

    <!-- Filtros e botão novo -->
    <div class="row g-2 align-items-end mb-3">
        <div class="col-md-3 mb-0">
            <label for="filtroTipo">Tipo</label>
            <select id="filtroTipo" class="form-control">
                <option value="">Todos</option>
                <option value="CLT">CLT</option>
                <option value="DIARIA">Diária</option>
                <option value="EMPREITA">Empreita</option>
                <option value="PRODUCAO">Produção</option>
            </select>
        </div>
        <div class="col-md-3 mb-0">
            <label for="filtroSituacao">Situação</label>
            <select id="filtroSituacao" class="form-control">
                <option value="">Todos</option>
                <option value="Ativo">Ativo</option>
                <option value="Inativo">Inativo</option>
            </select>
        </div>
        <div class="col-md-auto ms-auto">
            <a href="${pageContext.request.contextPath}/view/admin/ColaboCadastro.jsp"
               class="btn btn-primary">
                <i class="fas fa-plus-circle me-1"></i> Novo Funcionário
            </a>
        </div>
    </div>

    <!-- Tabela -->
    <div class="table-responsive rounded shadow-custom m-2 p-2">
        <table id="tabelaFuncionarios" class="table table-bordered table-striped">
            <thead class="thead-green">
                <tr>
                    <th>ID</th>
                    <th>Nome</th>
                    <th>CPF</th>
                    <th>Cargo</th>
                    <th>Tipo</th>
                    <th>E-mail</th>
                    <th>Situação</th>
                    <th>Ações</th>
                </tr>
            </thead>
            <tbody></tbody>
        </table>
    </div>
</div>

<!-- ===== Modal Editar ===== -->
<div class="modal fade" id="modalEditar" tabindex="-1" role="dialog" aria-hidden="true">
    <div class="modal-dialog modal-lg" role="document">
        <form id="formEditar" class="modal-content needs-validation" novalidate>
            <div class="modal-header bg-primary text-white">
                <h5 class="modal-title"><i class="fas fa-user-edit me-1"></i> Editar Funcionário</h5>
                <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Fechar"></button>
            </div>

            <div class="modal-body">
                <div id="alertaModalEditar"></div>

                <input type="hidden" id="editIdPessoa">
                <input type="hidden" id="editCpfPf">
                <input type="hidden" id="editMatricula">
                <input type="hidden" id="editSenhaPessoa">
                <input type="hidden" id="editDataNasc">
                <input type="hidden" id="editDataInicio">
                <input type="hidden" id="editDataFim">
                <input type="hidden" id="editCep">
                <input type="hidden" id="editNumero">
                <input type="hidden" id="editComplemento">

                <div class="row g-2">
                    <div class="col-md-8">
                        <label>Nome *</label>
                        <div class="input-group">
                            <span class="input-group-text"><i class="fas fa-user"></i></span>
                            <input type="text" class="form-control" id="editNome" minlength="3" maxlength="120" required>
                            <div class="invalid-feedback">Informe o nome.</div>
                        </div>
                    </div>
                    <div class="col-md-4">
                        <label>Nível *</label>
                        <div class="input-group">
                            <span class="input-group-text"><i class="fas fa-layer-group"></i></span>
                            <select class="form-select" id="editNivel" required>
                                <option value="">Selecione</option>
                                <option value="admin">Admin</option>
                                <option value="gerente">Gerente</option>
                                <option value="operador">Operador</option>
                            </select>
                            <div class="invalid-feedback">Selecione o nível.</div>
                        </div>
                    </div>
                </div>

                <div class="row g-2">
                    <div class="col-md-6">
                        <label>E-mail *</label>
                        <div class="input-group">
                            <span class="input-group-text"><i class="fas fa-envelope"></i></span>
                            <input type="email" class="form-control" id="editEmail" maxlength="120" required>
                            <div class="invalid-feedback">Informe um e-mail válido.</div>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <label>Telefone</label>
                        <div class="input-group">
                            <span class="input-group-text"><i class="fas fa-phone"></i></span>
                            <input type="text" class="form-control" id="editTelefone" maxlength="20">
                        </div>
                    </div>
                </div>

                <div class="row g-2">
                    <div class="col-md-4">
                        <label>Cargo *</label>
                        <div class="input-group">
                            <span class="input-group-text"><i class="fas fa-user-tie"></i></span>
                            <input type="text" class="form-control" id="editCargo" minlength="2" maxlength="80" required>
                            <div class="invalid-feedback">Informe o cargo.</div>
                        </div>
                    </div>
                    <div class="col-md-4">
                        <label>Tipo *</label>
                        <div class="input-group">
                            <span class="input-group-text"><i class="fas fa-users-cog"></i></span>
                            <select class="form-select" id="editTipo" required>
                                <option value="">Selecione</option>
                                <option value="CLT">CLT</option>
                                <option value="DIARIA">Diária</option>
                                <option value="EMPREITA">Empreita</option>
                                <option value="PRODUCAO">Produção</option>
                            </select>
                            <div class="invalid-feedback">Selecione o tipo.</div>
                        </div>
                    </div>
                    <div class="col-md-4">
                        <label>Situação</label>
                        <div class="input-group">
                            <span class="input-group-text"><i class="fas fa-toggle-on"></i></span>
                            <select class="form-select" id="editSituacao">
                                <option value="true">Ativo</option>
                                <option value="false">Inativo</option>
                            </select>
                        </div>
                    </div>
                </div>

                <div class="row g-2">
                    <div class="col-md-6">
                        <label>Usuário *</label>
                        <div class="input-group">
                            <span class="input-group-text"><i class="fas fa-user-circle"></i></span>
                            <input type="text" class="form-control" id="editUsuario" minlength="3" maxlength="40" required>
                            <div class="invalid-feedback">Informe o usuário.</div>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <label>Nova Senha <small class="text-muted">(deixe em branco para manter)</small></label>
                        <div class="input-group">
                            <span class="input-group-text"><i class="fas fa-lock"></i></span>
                            <input type="password" class="form-control" id="editSenhaNova" minlength="4" maxlength="60">
                        </div>
                    </div>
                </div>
            </div>

            <div class="modal-footer">
                <button type="submit" class="btn btn-success"><i class="fas fa-save me-1"></i> Salvar</button>
                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">
                    <i class="fas fa-times me-1"></i> Cancelar
                </button>
            </div>
        </form>
    </div>
</div>
<!-- Fim modal editar -->

<!-- ===== Modal Confirmar Desativar/Reativar ===== -->
<div class="modal fade" id="modalSituacao" tabindex="-1" role="dialog" aria-hidden="true">
    <div class="modal-dialog modal-sm" role="document">
        <div class="modal-content">
            <div class="modal-header bg-warning">
                <h5 class="modal-title"><i class="fas fa-exclamation-triangle me-1"></i> Confirmar</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Fechar"></button>
            </div>
            <div class="modal-body">
                <p id="textoConfirmacaoSituacao"></p>
                <input type="hidden" id="situacaoId">
                <input type="hidden" id="situacaoNova">
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-warning" id="btnConfirmarSituacao">
                    <i class="fas fa-check me-1"></i> Confirmar
                </button>
                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancelar</button>
            </div>
        </div>
    </div>
</div>
<!-- Fim modal situação -->

<footer>
    <%@ include file="../../pagina/footer.jsp"%>
</footer>

</body>
</html>
