/*Modulo Geral*/

 
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
            
                    function formatarMoeda(valor) {
            valor = valor.replace(/\D/g, "")
                         .replace(/(\d{1,2})$/, ',$1')
                         .replace(/(?=(\d{3})+(\D))\B/g, ".");
            return valor;
        }
        
         function ExibirAlerta(titulo, mensagem) {
                alert(titulo + ": " + mensagem);
            }
         
/***********************************************************************************************************/

/*Modulo Classificação*/
 function CarregarClassificacao() {

                // Inicializa o DataTable
                if ($.fn.DataTable.isDataTable('#tabelaClassificacao')) {
                         $('#tabelaClassificacao').DataTable().destroy();
                      }
                const tabela = $('#tabelaClassificacao').DataTable({
                    "processing": true,
                    "serverSide": false,
                  
                    "ajax": {
                        "url": "/agro/ClassificacaoServlet",
                        "method": "GET",
                        "dataSrc": ""
                    },
                    "columns": [
                        {"data": "idclassificacao"},
                        {"data": "classificacao"},
                        {
                            "data": null,
                            "title": "Ações",

                            "render": function (data, type, row) {
                                return '<button class="btn btn-sm btn-warning btn-responsivo mr-3 ml-3" onclick="abrirModalAtualizacaoClassificacao('
                                        + row.idclassificacao + ', \''

                                        + row.classificacao + '\')"><i class="fas fa-sync"></i><span class="texto-botao">Atualizar</span></button> ' +
                                        '<button class="btn btn-sm btn-danger btn-responsivo" onclick="abrirModalExclusaoClassificacao(' + row.idclassificacao + ', \'' + row.classificacao + '\')"><i class="fas fa-trash-alt"></i><span class="texto-botao">Excluir</span></button>';
                            }
                        }
                    ],
                    "language": {
                        "url": 'https://cdn.datatables.net/plug-ins/2.1.6/i18n/pt-BR.json'
                    }
                });
            }

           function abrirModalAtualizacaoClassificacao(id, classificacao) {
            $('#atualizacaoId').val(id);
            $('#atualizacaoClassificacao').val(classificacao);
            $('#modalAtualizacao').modal('show');
        }

        function abrirModalExclusaoClassificacao(id, classificacao) {
            $('#exclusaoId').val(id);
            $('#exclusaoClassificacao').text(classificacao);
            $('#modalExclusao').modal('show');
        }

            function salvarClassificacao() {
                const classificacao = $('#classificacao').val();
                if (classificacao.trim() === '') {
                    alert('Por favor, preencha a classificação.');
                    return;
                }
                $.ajax({
                    url: '/agro/ClassificacaoServlet',
                    method: 'POST',
                    data: {classificacao: classificacao},
                    success: function (response) {
                        mostrarAlerta(response.message, 'success');
                        $('#modalCadastro').modal('hide');
                        $('#classificacao').val("");
                        $('#tabelaClassificacao').DataTable().ajax.reload();
                    },
                    error: function (xhr) {
                        mostrarAlerta("Erro em cadastrar", 'danger');
                        alert('Erro ao cadastrar classificação: ' + xhr.responseJSON.error);
                    }
                });
            }

            function atualizarClassificacao() {
                const id = $('#atualizacaoId').val();
                const classificacao = $('#atualizacaoClassificacao').val();
                if (classificacao.trim() === '') {
                    alert('Por favor, preencha a classificação.');
                    return;
                }
                $.ajax({
                    url: '/agro/ClassificacaoServlet',
                    method: 'POST',
                    data: {id: id, classificacao: classificacao},
                    success: function (response) {
                        mostrarAlerta(response.message, 'success');

                        $('#modalAtualizacao').modal('hide');
                        $('#tabelaClassificacao').DataTable().ajax.reload();
                    },
                    error: function (xhr) {
                        mostrarAlerta('Erro em atualizar a Classificação', 'danger');
                        console.log('Erro ao atualizar classificação: ' + (xhr.responseJSON ? xhr.responseJSON.error : 'Erro desconhecido'));
                    }
                });
            }

            function excluirClassificacao() {
                const id = $('#exclusaoId').val();
                $.ajax({
                    url: '/agro/ClassificacaoServlet',
                    method: 'POST',
                    data: {id: id, acao: 'Delete'},
                    success: function (response) {
                        mostrarAlerta(response.message, 'success');
                        $('#modalExclusao').modal('hide');
                        $('#tabelaClassificacao').DataTable().ajax.reload();
                    },
                    error: function (xhr) {
                        mostrarAlerta('Erro em excluir o dado!', 'danger');
                        console.log('Erro ao excluir classificação: ' + (xhr.responseJSON ? xhr.responseJSON.error : 'Erro desconhecido'));
                    }
                });
            }
            
/******************************************************************************************************/
/* Modulo Despesas Custos*/
function abrirModalAtualizacaoDespesasCustos(id, despesa, unidade, valor, tipo) {
    // Atribui os valores aos campos do formulário de atualização
    $('#atualizacaoId').val(id);
    $('#atualizacaoDespesa').val(despesa);
    $('#atualizacaoUnidade').val(unidade);
    $('#atualizacaoValor').val(valor);
    $('#atualizacaoTipo').val(tipo);
    // Exibe a modal de atualização
    $('#modalAtualizacao').modal('show');
}

function abrirModalExclusaoDespesasCustos(id, despesa) {
    // Define os valores necessários para a exclusão
    $('#exclusaoId').val(id);
    // Exibe o nome da despesa/custo no modal de exclusão (usando text() para um elemento <strong>)
    $('#exclusaoDespesa').text(despesa);
    // Exibe a modal de exclusão
    $('#modalExclusao').modal('show');
}

  function CarregarDadosDespesasCustos(){
                        // Inicializa o DataTable
                         // Inicializa o DataTable
                if ($.fn.DataTable.isDataTable('#tabelaDespesasCustos')) {
                         $('#tabelaDespesasCustos').DataTable().destroy();
                     }

            const tabela = $('#tabelaDespesasCustos').DataTable({
                "processing": false,
                "serverSide": false,
                "ajax": {
                    "url": "/agro/DespesasCustosServlet", // URL da servlet para buscar os dados
                    "method": "GET",
                    "dataSrc": "" // Define que os dados virão no formato de array
                },
                "columns": [
                    { "data": "iddespesascusto" },
                    { "data": "despesascusto" },
                    { "data": "unidadedespesascustos" },
                    { "data": "valordespesascustos" },
                    { "data": "tipodespesascustos" },
                    {
                        "data": null,
                        "render": function(data, type, row) {
                            return '<button class="btn btn-sm btn-warning btn-responsivo" onclick="abrirModalAtualizacaoDespesasCustos('
                                    + row.iddespesascusto +', \'' 
                                    + row.despesascusto +'\', \'' 
                                    + row.unidadedespesascustos +'\', \'' 
                                    + row.valordespesascustos +'\', \''
                                    + row.tipodespesascustos + '\')"><i class="fas fa-sync"></i><span class="texto-botao">Atualizar</span></button> ' +
                                   '<button class="btn btn-sm btn-danger btn-responsivo" onclick="abrirModalExclusaoDespesasCustos(' + row.iddespesascusto + ', \'' + row.despesascusto + '\')"><i class="fas fa-trash-alt"></i><span class="texto-botao">Excluir</span></button>';
                        }
                    }
                ],
                "language": {
                	"url": 'https://cdn.datatables.net/plug-ins/2.1.6/i18n/pt-BR.json' // Tradução para o Português
                }
            });
        }
         function excluirDespesasCustos() {
            var id = $('#exclusaoId').val();

            $.ajax({
                url: '/agro/DespesasCustosServlet',
                method: 'POST',
                data: {
                    acao: 'delete',
                    iddespesascusto: id
                },
                success: function(response) {
                	 mostrarAlerta('Despesa excluída com sucesso!','info');
                    $('#modalExclusao').modal('hide');
                    $('#tabelaDespesasCustos').DataTable().ajax.reload();
                },
                error: function(xhr) {
                    alert('Atenção: ' + xhr.responseText);
                    mostrarAlerta('Despesa não excluída com sucesso!','danger');
                }
            });
        }
          
                  function atualizarDespesasCustos(id, despesa, unidade, valor, tipo) {
            $.ajax({
                url: '/agro/DespesasCustosServlet',
                method: 'POST',
                data: {
                    acao: 'update',
                    iddespesascustos: id,
                    despesascustos: despesa,
                    unidadedespesacustos: unidade,
                    valordespesacustos: valor,
                    tipodespesacustos: tipo
                },
                success: function(response) {
                	 mostrarAlerta('Despesa atualizada:   com sucesso','info');
                    $('#modalAtualizacao').modal('hide');
                    $('#formAtualizacao')[0].reset();
                    $('#tabelaDespesasCustos').DataTable().ajax.reload();
                },
                error: function(xhr) {
                    console.log('Atenção: ' + xhr.responseText);
                    mostrarAlerta('Despesa não atualizada!','danger');
                }
            });
        }
        function salvarDespesasCustos(despesa, unidade, valor, tipo) {
            $.ajax({
                url: '/agro/DespesasCustosServlet',
                method: 'POST',
                data: {
                    acao: 'create',
                    despesascusto: despesa,
                    unidadedespesacusto: unidade,
                    valordespesacusto: valor,
                    tipodespesacusto: tipo
                },
                success: function(response) {
                	 mostrarAlerta('Despesa cadastrada com sucesso!','success');
                    $('#modalCadastro').modal('hide');
                    $('#formCadastro')[0].reset();
                    $('#tabelaDespesasCustos').DataTable().ajax.reload();
                },
                error: function(xhr) {
                    console.log( xhr.responseText);
                    mostrarAlerta('Despesa não cadastrada!','danger');
                }
            });
        }