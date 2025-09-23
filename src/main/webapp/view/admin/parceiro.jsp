<%@ page contentType="text/html;charset=UTF-8" language="java"%>
<html>
<head>
    <title>Agro - Parceiros</title>
   
     <%@ include file="../../pagina/importacao.html"%>

    <script>
        $(document).ready(function() {
        	 //Verificando mensagem
            		var mensagem = '<%= request.getAttribute("Mensagem") != null ? request.getAttribute("Mensagem") : "" %>';
           			 var atributo = '<%= request.getAttribute("Atributo") != null ? request.getAttribute("Atributo") : "" %>';

            				if (mensagem && atributo) {
              					 // Chama a função mostrarAlerta
               					 mostrarAlerta(mensagem, atributo);
           			 }
            // Inicializa o DataTable
            const tabela = $('#tabelaParceiros').DataTable({
                "processing": true,
                "serverSide": false,
                "ajax": {
                    "url": "/agro/ControllerParceiro", // URL da servlet para buscar os dados 
                    "method": "GET",
                    "dataSrc": "" // Define que os dados virão no formato de array
                },
                "columns": [
                    { "data": "nomePessoa" },
                    { 
                        "data": "situacaoPessoa", 
                        "render": function(data) {
                            // Badge para situação
                            if (data) {
                                return '<span class="badge badge-success">Ativo</span>';
                            } else {
                                return '<span class="badge badge-danger">Inativo</span>';
                            }
                        }
                    },
                    { "data": "emailPessoa" },
                    { "data": "telefonePessoa" },
                    { "data": "razaoSocialPessoaCnpj" },
                    {
                        "data": null,
                        "render": function(data, type, row) {
                        	
                        	 return '<button class="btn btn-sm btn-warning"  onclick="enviarParaAtualizacao('+row.idPessoa+')"><i class="fas fa-sync"></i> <span class="texto-botao">Atualizar</span> </button> ' +
                             '<button class="btn btn-sm btn-dark" onclick="abrirModalExclusao(' 
                              + row.idPessoa + ', \'' + row.nomePessoa + '\', '
                              + row.situacaoPessoa + ')"><i class="fas fa-toggle-on"></i><span class="texto-botao">Alterar</span> </button> ' +
                             '<button class="btn btn-sm btn-info" onclick="abrirModalDetalhes(' + row.idPessoa + ')"><i class="fas fa-info-circle"></i><span class="texto-botao">Detalhes</span></button>';
                 			 }
                            
                    }
                ],
                "language": {
                    "url": 'https://cdn.datatables.net/plug-ins/2.1.6/i18n/pt-BR.json' // Tradução para o Português
                }
            });

           //Meettodo abrir modal de exclusao!
            window.abrirModalExclusao = function(id, nome, situacao) {
                $('#exclusaoId').val(id);
                $('#exclusaoNome').text(nome);  // Use .text() para elementos span
                $('#exclusaoSituacao').text(situacao ? 'true' : 'false');  // Define o texto com base na situação
                $('#modalExclusao').modal('show');
            };;

            // Metodo de Detalhes
            window.abrirModalDetalhes = function(id) {
                $.ajax({
                    url: '/agro/ControllerParceiro', // URL da servlet para buscar os detalhes
                    method: 'GET',
                    data: { id: id }, // Envia o ID do parceiro para obter detalhes
                    success: function(response) {
                        // Preencher os dados do parceiro no modal
                        $('#detalhesNome').text(response.nomePessoa);
                        $('#detalhesUsuario').text(response.usuarioPessoa);
                        $('#detalhesSenha').text(response.senhaPessoa);
                        $('#detalhesNivel').text(response.nivelPessoa);
                        $('#detalhesEmail').text(response.emailPessoa);
                        $('#detalhesTelefone').text(response.telefonePessoa);
                        $('#detalhesCnpj').text(response.cnpjPessoaCnpj);
                        $('#detalhesRazaoSocial').text(response.razaoSocialPessoaCnpj);
                        $('#detalhesInscricaoEstadual').text(response.inscricaoEstadualPessoaCnpj);
                        $('#detalhesSite').text(response.siteparceiro);
                        $('#detalhesEndereco').text(response.endereco.Endereco + ', ' + response.numero);
                        $('#detalhesCep').text(response.endereco.Cep);
                        $('#detalhesBairro').text(response.endereco.Bairro);
                        $('#detalhesCidade').text(response.endereco.Cidade);
                        $('#detalhesEstado').text(response.endereco.Estado);
                        $('#detalhesPais').text(response.endereco.Pais);

                        // Adiciona a badge de Situação no modal
                        if (response.situacaoPessoa) {
                            $('#detalhesSituacao').html('<span class="badge badge-success">Ativo</span>');
                        } else {
                            $('#detalhesSituacao').html('<span class="badge badge-danger">Inativo</span>');
                        }

                        $('#modalDetalhes').modal('show');
                    },
                    error: function(xhr) {
                        alert('Erro ao buscar detalhes do parceiro.');
                    }
                });
            };
            //Mostrar mensagem
      				function mostrarAlerta(mensagem, tipo) {
   								 const alerta = $('#alerta');

  										  // Remove todas as classes de alerta anteriores
  										  alerta.removeClass('d-none alert-success alert-danger alert-info alert-warning');

  										  // Adiciona a classe apropriada ao alerta
  											  alerta.addClass('alert alert-' + tipo);

   											 // Define a mensagem do alerta
   												 alerta.text(mensagem);

  												  // Exibe o alerta
  													  alerta.fadeIn();

    												// Oculta o alerta após 5 segundos
   													 setTimeout(() => {
   	    												 alerta.fadeOut(() => {
          											     alerta.addClass('d-none'); // Reaplica 'd-none' para esconder
       												 });
    											}, 5000);
										}

        
            // Função para exclusão de parceiro com redirecionamento e mensagem dinâmica
  
        window.excluirDespesa = function() {
            let id = $('#exclusaoId').val();
            const situacao = $('#exclusaoSituacao').text();
            const nome = $('#exclusaoNome').text();

            const situacaoFormatada = situacao === 'true' ? 'true' : 'false';
            $.ajax({
                url: '/agro/ControllerParceiro',
                method: 'POST',
                data: {
                    idpessoa: id,
                    situacaopessoa: situacaoFormatada,
                    acao: 'desativar' // Certifique-se de adicionar a ação aqui
                },
                success: function(response) {
                    // Atualiza a tabela ou recarrega a página
                    mostrarAlerta('Parceiro: ' + nome + ' desativado com sucesso!', 'info');
                    $('#modalExclusao').modal('hide');
                    // Recarregar a DataTable após a exclusão
                    $('#tabelaParceiros').DataTable().ajax.reload(); // Isso atualiza a tabela

                    // Se você deseja redirecionar para a página após a exclusão:
                    // window.location.href = "caminho/para/sua/pagina.jsp"; 
                },
                error: function(xhr) {
                    mostrarAlerta('Erro em desativar', 'danger');
                    alert('Atenção: ' + xhr.responseText);
                }
            });
        };

        });
        // criando meodo para buscar id enviar para atualizaçao
        function enviarParaAtualizacao(id) {
            const form = document.createElement('form');
            form.method = 'POST';
            form.action = '/agro/ControllerParceiro';

            const input = document.createElement('input');
            input.type = 'hidden';
            input.name = 'idPessoa';
            input.value = id;

            const action = document.createElement('input');
            action.type = 'hidden';
            action.name = 'acao';
            action.value = 'atualizarenvio';

            form.appendChild(input);
            form.appendChild(action);
            document.body.appendChild(form);

            form.submit(); // Submete o formulário
        }


    </script>

</head>
<body>
    <header>
        <%@ include file="../../pagina/menu.jsp"%>
    </header>

    <div class="container-fluid content mt-2">
        <h2 class="mb-2 text-white border bg-success p-2 rounded shadow-custom">Parceiros</h2>
    <div id="alerta" class="alert d-none " role="alert"></div>
      
        <a class="btn btn-primary mt-4 mb-3" href="http://localhost:8080/agro/view/admin/CadastroParceiro.jsp" role="button">
            <i class="fas fa-plus"></i> Cadastrar Parceiros
        </a>
        <div class="table-responsive rounded shadow-custom m-2 p-2">
            <table id="tabelaParceiros" class="table table-bordered table-striped">
                <thead class="thead-green">
                    <tr>
                        <th>Nome</th>
                        <th>Situação</th>
                        <th>Email</th>
                        <th>Telefone</th>
                        <th>Razão Social</th>
                        <th>Ações</th>
                    </tr>
                </thead>
                <tbody></tbody>
            </table>
        </div>
    </div>

 <!-- Modal de detalhes personalizado -->
<div id="modalDetalhes" class="modal fade" tabindex="-1" role="dialog">
    <div class="modal-dialog modal-lg" role="document">
        <div class="modal-content">
            <div class="modal-header bg-primary text-white">
                <h5 class="modal-title"><i class="fas fa-info-circle"></i> Detalhes do Parceiro</h5>
                <button type="button" class="close text-white" data-dismiss="modal" aria-label="Fechar">
                    <span aria-hidden="true">&times;</span>
                </button>
            </div>
            <div class="modal-body">
                <!-- Sessão de Dados da Empresa -->
                <div class="row">
                    <div class="col-12">
                        <h4 class="text-center"><i class="fas fa-building"></i> Dados da Empresa</h4>
                        <hr>
                    </div>
                </div>
                <div class="row mb-3">
                    <div class="col-md-6">
                        <p><strong><i class="fas fa-user"></i> Nome:</strong> <span id="detalhesNome"></span></p>
                    </div>
                    <div class="col-md-6">
                        <p><strong><i class="fas fa-user-shield"></i> Usuário:</strong> <span id="detalhesUsuario"></span></p>
                    </div>
                </div>
                <div class="row mb-3">
                    <div class="col-md-4">
                        <p><strong><i class="fas fa-lock"></i> Nível:</strong> <span id="detalhesNivel"></span></p>
                    </div>
                     <div class="col-md-4">
       						 <p><strong><i class="fas fa-toggle-on"></i> Situação:</strong>
            				<span id="detalhesSituacao"></span>
       					 </p>
    				</div>
                    <div class="col-md-4">
                        <p><strong><i class="fas fa-envelope"></i> Email:</strong> <span id="detalhesEmail"></span></p>
                    </div>
                </div>
                <div class="row mb-3">
                    <div class="col-md-6">
                        <p><strong><i class="fas fa-phone"></i> Telefone:</strong> <span id="detalhesTelefone"></span></p>
                    </div>
                    <div class="col-md-6">
                        <p><strong><i class="fas fa-building"></i> Razão Social:</strong> <span id="detalhesRazaoSocial"></span></p>
                    </div>
                </div>

                <!-- Sessão de Dados do CNPJ -->
                <div class="row">
                    <div class="col-12">
                        <h4 class="text-center"><i class="fas fa-id-card"></i> Dados do CNPJ</h4>
                        <hr>
                    </div>
                </div>
                <div class="row mb-3">
                    <div class="col-md-6">
                        <p><strong><i class="fas fa-id-card-alt"></i> CNPJ:</strong> <span id="detalhesCnpj"></span></p>
                    </div>
                    <div class="col-md-6">
                        <p><strong><i class="fas fa-file-alt"></i> Inscrição Estadual:</strong> <span id="detalhesInscricaoEstadual"></span></p>
                    </div>
                </div>
                <div class="row mb-3">
                    <div class="col-md-12">
                        <p><strong><i class="fas fa-globe"></i> Site:</strong> <span id="detalhesSite"></span></p>
                    </div>
                </div>

                <!-- Sessão de Endereço -->
                <div class="row">
                    <div class="col-12">
                        <h4 class="text-center"><i class="fas fa-map-marker-alt"></i> Endereço do Parceiro</h4>
                        <hr>
                    </div>
                </div>
                <div class="row mb-3">
                    <div class="col-md-6">
                        <p><strong><i class="fas fa-home"></i> Endereço:</strong> <span id="detalhesEndereco"></span></p>
                    </div>
                    <div class="col-md-3">
                        <p><strong><i class="fas fa-map-pin"></i> CEP:</strong> <span id="detalhesCep"></span></p>
                    </div>
                    <div class="col-md-3">
                        <p><strong><i class="fas fa-map"></i> Bairro:</strong> <span id="detalhesBairro"></span></p>
                    </div>
                </div>
                <div class="row mb-3">
                    <div class="col-md-6">
                        <p><strong><i class="fas fa-city"></i> Cidade:</strong> <span id="detalhesCidade"></span></p>
                    </div>
                    <div class="col-md-3">
                        <p><strong><i class="fas fa-flag"></i> Estado:</strong> <span id="detalhesEstado"></span></p>
                    </div>
                    <div class="col-md-3">
                        <p><strong><i class="fas fa-globe-americas"></i> País:</strong> <span id="detalhesPais"></span></p>
                    </div>
                </div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" data-dismiss="modal"><i class="fas fa-times"></i> Fechar</button>
            </div>
        </div>
    </div>
</div>


    <!-- Modal de atualização -->
    <div id="modalAtualizacao" class="modal fade" tabindex="-1" role="dialog">
        <div class="modal-dialog" role="document">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title">Atualizar Parceiro</h5>
                    <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                        <span aria-hidden="true">&times;</span>
                    </button>
                </div>
                <div class="modal-body">
                    <form id="formAtualizacao">
                        <input type="hidden" id="atualizacaoId" name="id">
                        <div class="form-group">
                            <label for="atualizacaoNome">Nome:</label>
                            <input type="text" id="atualizacaoNome" name="nomePessoa" class="form-control" required maxlength="50">
                        </div>
                        <div class="form-group">
                            <label for="atualizacaoEmail">Email:</label>
                            <input type="email" id="atualizacaoEmail" name="emailPessoa" class="form-control" required maxlength="100">
                        </div>
                        <div class="form-group">
                            <label for="atualizacaoTelefone">Telefone:</label>
                            <input type="text" id="atualizacaoTelefone" name="telefonePessoa" class="form-control" required maxlength="15">
                        </div>
                        <div class="form-group">
                            <label for="atualizacaoRazaoSocial">Razão Social:</label>
                            <input type="text" id="atualizacaoRazaoSocial" name="razaoSocialPessoaCnpj" class="form-control" required maxlength="100">
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-secondary" data-dismiss="modal">Cancelar</button>
                            <button type="submit" class="btn btn-primary">Atualizar</button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    </div>

    <!-- Modal de exclusão -->
    <div id="modalExclusao" class="modal fade" tabindex="-1" role="dialog">
        <div class="modal-dialog" role="document">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title">Excluir Parceiro</h5>
                    <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                        <span aria-hidden="true">&times;</span>
                    </button>
                </div>
                <div class="modal-body">
   							 <p>Tem certeza de que deseja excluir o parceiro <strong id="exclusaoNome"></strong>?</p>
    							<input type="hidden" id="exclusaoId" name="id">
    							<input type="hidden" id="exclusaoNome" name="nome">
    							<input type="hidden" id="exclusaoSituacao" name="situacao">
    							<span id="exclusaoSituacao" class="d-none"></span> <!-- Usando span para a situação -->
				</div>

                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-dismiss="modal">Cancelar</button>
                    <button type="button" class="btn btn-danger" onclick="excluirDespesa()">Excluir</button>
                </div>
            </div>
        </div>
    </div>

    <footer>
        <%@ include file="../../pagina/footer.jsp"%>
    </footer>
</body>
</html>