<%@ page contentType="text/html;charset=UTF-8" language="java"%>
<html>
    <head>
        <title>Agro - Parceiros</title>

        <%@ include file="../../pagina/importacao.html"%>
    
        <script>
            $(document).ready(function () {
                
                
                // Carregando mensagens
                   const msg = sessionStorage.getItem("mensagemAlerta");
    const tipo = sessionStorage.getItem("tipoAlerta");

    if (msg && window.location.pathname.includes("parceiro.jsp")) {
        mostrarAlerta(msg, tipo || 'info', '#alerta', 4000);
                               sessionStorage.removeItem("mensagemAlerta");
                               sessionStorage.removeItem("tipoAlerta");
                           }
                                     
                        /** Carregamento de  Dados na tabela do sistemaa*/            
                CarregarParceiros();
                carregarResumoParceiros()
              
                setInterval(CarregarParceiros, 20000);
                  
            });
        </script>
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
                           <h3 id="total" class="font-weight-bold">0</h3>
                            <i class="fas fa-users fa-2x text-success float-right"></i>
                        </div>
                    </div>
                </div>

                <div class="col-lg-4 mb-3">
                    <div class="card shadow-sm border-left-green">
                        <div class="card-body">
                            <h6 class="text-muted">Ativos</h6>
                          <h3 id="ativosParceiros" class="font-weight-bold">0</h3>
                            <i class="fas fa-check-circle fa-2x text-success float-right"></i>
                        </div>
                    </div>
                </div>

                <div class="col-lg-4 mb-3">
                    <div class="card shadow-sm border-left-green">
                        <div class="card-body">
                            <h6 class="text-muted">Inativos</h6>
                         <h3 id="inativosParceiros" class="font-weight-bold">0</h3>
                            <i class="fas fa-user-slash fa-2x text-danger float-right"></i>
                        </div>
                    </div>
                </div>

            </div>
            <!-- HEADER + BOTÃO -->
            <div class="row mb-4">
                <div class="col text-right">
                     <a href="http://localhost:8080/agro/view/admin/CadastroParceiro.jsp" class="btn btn-success">
                          <i class="fas fa-user-plus"></i> Cadastrar Parceiro
                    </a>
                </div>
            </div>
            <!-- DATATABLE -->
           
                    <h5 class="">
                        </i> Lista de Parceiros
                    </h5>
          
            <hr>
                <div class="card-body">
                    <div class="table-responsive">
                        <table id="tabelaParceiros" class="table table-striped table-bordered">
                            <thead class="thead-light">
                                <tr>
                                    <th>ID</th>
                                    <th>Parceiro</th>
                                     <th>Nivel</th>
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

       <!-- MODAL ATIVAR / DESATIVAR -->
<div class="modal fade" id="modalDesativar" tabindex="-1" role="dialog" aria-labelledby="modalDesativarLabel" aria-hidden="true">
    <div class="modal-dialog" role="document">
        <div class="modal-content">
            <!-- Título do modal (dinâmico) -->
            <div class="modal-header bg-primary text-white">
                <h5 class="modal-title" id="modalDesativarLabel">Confirmar Ação</h5>
                <button type="button" class="close text-white" data-dismiss="modal" aria-label="Fechar">
                    <span aria-hidden="true">&times;</span>
                </button>
            </div>

            <!-- Corpo do modal com a mensagem -->
            <div class="modal-body">
                <p id="mensagemDesativar">Você deseja alterar a situação do parceiro?</p>
            </div>

            <!-- Rodapé com botões -->
            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" data-dismiss="modal">Cancelar</button>
                <button type="button" class="btn btn-danger" id="btnConfirmarDesativar">
                    <i class="fas fa-user-slash mr-1"></i> Desativar
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