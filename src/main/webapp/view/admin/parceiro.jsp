<%@ page contentType="text/html;charset=UTF-8" language="java"%>
<html>
    <head>
        <title>Agro - Parceiros</title>

        <%@ include file="../../pagina/importacao.html"%>
    
    </head>
    <body>
        <header>
            <%@ include file="../../pagina/menu.jsp"%>
        </header>

        <div class="container-fluid content mt-2">
            <h2 class="mb-2 text-white border bg-success p-2 rounded shadow-custom">Parceiros</h2>
            <div id="alerta" class="alert d-none " role="alert"></div>
           

            <!-- CARD RESUMO -->
            <div class="row mb-4">

                <div class="col-lg-4 mb-3">
                    <div class="card shadow-sm border-left-green">
                        <div class="card-body">
                            <h6 class="text-muted">Total de Parceiros</h6>
                           <h3 id="total" class="fw-bold">0</h3>
                            <i class="fas fa-users fa-2x text-success float-end"></i>
                        </div>
                    </div>
                </div>

                <div class="col-lg-4 mb-3">
                    <div class="card shadow-sm border-left-green">
                        <div class="card-body">
                            <h6 class="text-muted">Ativos</h6>
                          <h3 id="ativosParceiros" class="fw-bold">0</h3>
                            <i class="fas fa-check-circle fa-2x text-success float-end"></i>
                        </div>
                    </div>
                </div>

                <div class="col-lg-4 mb-3">
                    <div class="card shadow-sm border-left-green">
                        <div class="card-body">
                            <h6 class="text-muted">Inativos</h6>
                         <h3 id="inativosParceiros" class="fw-bold">0</h3>
                            <i class="fas fa-user-slash fa-2x text-danger float-end"></i>
                        </div>
                    </div>
                </div>

            </div>

            <!-- CONTAGEM POR TIPO -->
            <div class="row mb-3">
                <div class="col-md-4 mb-2">
                    <div class="card shadow-sm border-start border-4 border-success">
                        <div class="card-body py-2 d-flex justify-content-between align-items-center">
                            <span class="text-muted"><i class="fas fa-handshake me-1 text-success"></i>Parceiros <small>(compra a produção)</small></span>
                            <h4 id="countParceiros" class="fw-bold mb-0">0</h4>
                        </div>
                    </div>
                </div>
                <div class="col-md-4 mb-2">
                    <div class="card shadow-sm border-start border-4 border-info">
                        <div class="card-body py-2 d-flex justify-content-between align-items-center">
                            <span class="text-muted"><i class="fas fa-truck me-1 text-info"></i>Fornecedores <small>(indiretos)</small></span>
                            <h4 id="countFornecedores" class="fw-bold mb-0">0</h4>
                        </div>
                    </div>
                </div>
                <div class="col-md-4 mb-2">
                    <div class="card shadow-sm border-start border-4 border-warning">
                        <div class="card-body py-2 d-flex justify-content-between align-items-center">
                            <span class="text-muted"><i class="fas fa-flask me-1 text-warning"></i>Insumos <small>(defensivos/adubos)</small></span>
                            <h4 id="countInsumos" class="fw-bold mb-0">0</h4>
                        </div>
                    </div>
                </div>
            </div>

            <!-- HEADER + BOTÃO -->
            <div class="row mb-3">
                <div class="col text-end">
                     <a href="<%=request.getContextPath()%>/view/admin/CadastroParceiro.jsp" class="btn btn-success">
                          <i class="fas fa-user-plus"></i> Cadastrar Parceiro
                    </a>
                </div>
            </div>

            <!-- ABAS DE FILTRO POR TIPO -->
            <ul class="nav nav-tabs mb-0">
                <li class="nav-item"><a class="nav-link aba-tipo-parceiro active" href="#" data-tipo="">Todos</a></li>
                <li class="nav-item"><a class="nav-link aba-tipo-parceiro" href="#" data-tipo="PARCEIRO">Parceiros</a></li>
                <li class="nav-item"><a class="nav-link aba-tipo-parceiro" href="#" data-tipo="FORNECEDOR">Fornecedores</a></li>
                <li class="nav-item"><a class="nav-link aba-tipo-parceiro" href="#" data-tipo="INSUMO">Insumos</a></li>
            </ul>

            <hr class="mt-0">
                <div class="card-body">
                    <div class="table-responsive rounded shadow-custom p-2">
                        <table id="tabelaParceiros" class="table table-bordered table-striped">
                            <thead class="thead-green">
                                <tr>
                                    <th>ID</th>
                                    <th>Nome</th>
                                     <th>Tipo</th>
                                    <th>Situação</th>
                                    <th>Telefone</th>
                                    <th>Razão Social</th>
                                    <th>Cnpj</th>
                                    <th>Ações</th>
                                </tr>
                            </thead>

                            <tbody>
                                
                            </tbody>

                        </table>
                    </div>
                </div>
            </div> 
        </div>

        <!-- MODAL EDITAR PARCEIRO -->
        <div class="modal fade" id="modalParceiro" tabindex="-1" aria-hidden="true">
            <div class="modal-dialog modal-lg">
                <div class="modal-content">
                    <div class="modal-header bg-success text-white">
                        <h5 class="modal-title"><i class="fas fa-user-edit me-2"></i>Editar Parceiro</h5>
                        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Fechar"></button>
                    </div>
                    <div class="modal-body">
                        <div id="alertaParceiro" class="alert d-none" role="alert"></div>
                        <input type="hidden" id="idPessoa">
                        <input type="hidden" id="usuario">
                        <input type="hidden" id="senha">

                        <div class="row g-2">
                            <div class="col-md-8">
                                <label class="form-label">Nome *</label>
                                <input type="text" class="form-control" id="nome">
                            </div>
                            <div class="col-md-4">
                                <label class="form-label">Tipo *</label>
                                <select class="form-select" id="nivel">
                                    <option value="PARCEIRO">Parceiro</option>
                                    <option value="FORNECEDOR">Fornecedor</option>
                                    <option value="INSUMO">Insumo</option>
                                </select>
                            </div>
                        </div>

                        <div class="row g-2 mt-1">
                            <div class="col-md-6">
                                <label class="form-label">Razão Social</label>
                                <input type="text" class="form-control" id="razao">
                            </div>
                            <div class="col-md-6">
                                <label class="form-label">CNPJ</label>
                                <input type="text" class="form-control" id="cnpj">
                            </div>
                        </div>

                        <div class="row g-2 mt-1">
                            <div class="col-md-4">
                                <label class="form-label">Inscrição Estadual</label>
                                <input type="text" class="form-control" id="inscricaoestadual">
                            </div>
                            <div class="col-md-5">
                                <label class="form-label">Site</label>
                                <input type="text" class="form-control" id="site" placeholder="https://...">
                            </div>
                            <div class="col-md-3">
                                <label class="form-label">Situação</label>
                                <select class="form-select" id="situacao">
                                    <option value="true">Ativo</option>
                                    <option value="false">Inativo</option>
                                </select>
                            </div>
                        </div>

                        <div class="row g-2 mt-1">
                            <div class="col-md-6">
                                <label class="form-label">E-mail</label>
                                <input type="email" class="form-control" id="email">
                            </div>
                            <div class="col-md-6">
                                <label class="form-label">Telefone</label>
                                <input type="text" class="form-control" id="telefone">
                            </div>
                        </div>

                        <hr>
                        <div class="row g-2">
                            <div class="col-md-3">
                                <label class="form-label">CEP</label>
                                <input type="text" class="form-control" id="cep">
                            </div>
                            <div class="col-md-7">
                                <label class="form-label">Endereço</label>
                                <input type="text" class="form-control" id="logradouro" readonly>
                            </div>
                            <div class="col-md-2">
                                <label class="form-label">Nº</label>
                                <input type="text" class="form-control" id="numero">
                            </div>
                        </div>
                        <div class="row g-2 mt-1">
                            <div class="col-md-4">
                                <label class="form-label">Bairro</label>
                                <input type="text" class="form-control" id="bairro" readonly>
                            </div>
                            <div class="col-md-4">
                                <label class="form-label">Cidade</label>
                                <input type="text" class="form-control" id="cidade" readonly>
                            </div>
                            <div class="col-md-2">
                                <label class="form-label">UF</label>
                                <input type="text" class="form-control" id="uf" readonly>
                            </div>
                            <div class="col-md-2">
                                <label class="form-label">Compl.</label>
                                <input type="text" class="form-control" id="complemento">
                            </div>
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancelar</button>
                        <button type="button" class="btn btn-success" id="btnSalvarParceiro">
                            <i class="fas fa-save me-1"></i>Salvar
                        </button>
                    </div>
                </div>
            </div>
        </div>

       <!-- MODAL ATIVAR / DESATIVAR -->
<div class="modal fade" id="modalDesativar" tabindex="-1" role="dialog" aria-labelledby="modalDesativarLabel" aria-hidden="true">
    <div class="modal-dialog" role="document">
        <div class="modal-content">
            <!-- Título do modal (dinâmico) -->
            <div class="modal-header bg-primary text-white">
                <h5 class="modal-title" id="modalDesativarLabel">Confirmar Ação</h5>
                <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Fechar"></button>
            </div>

            <!-- Corpo do modal com a mensagem -->
            <div class="modal-body">
                <p id="mensagemDesativar">Você deseja alterar a situação do parceiro?</p>
            </div>

            <!-- Rodapé com botões -->
            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancelar</button>
                <button type="button" class="btn btn-danger" id="btnConfirmarDesativar">
                    <i class="fas fa-user-slash me-1"></i> Desativar
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