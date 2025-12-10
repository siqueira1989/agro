/******************************************************************************************************/
/*Modulo Geral*/
/******************************************************************************************************/
function mostrarAlerta(mensagem, tipo = 'info', seletor = '#alerta', autoHideMs = 4000) {
    const $box = $(seletor);
    if ($box.length === 0) {
        console.warn('Elemento de alerta não encontrado:', seletor);
        return;
    }
    console.log("Mostrando alerta:", mensagem, "->", seletor);

    $box
            .removeClass('d-none alert-success alert-danger alert-info alert-warning')
            .addClass(`alert alert-${tipo}`)
            .html(mensagem)
            .fadeIn();

    if (autoHideMs > 0) {
        setTimeout(() => {
            $box.fadeOut(() => $box.addClass('d-none'));
        }, autoHideMs);
}
}


function formatarMoeda(valor) {
    valor = valor.replace(/\D/g, ''); // Remove tudo que não for dígito
    valor = (valor / 100).toFixed(2) + ''; // Divide por 100 e adiciona 2 casas decimais
    valor = valor.replace('.', ','); // Troca ponto decimal por vírgula
    valor = valor.replace(/\B(?=(\d{3})+(?!\d))/g, '.'); // Adiciona pontos nos milhares
    return valor;
}
// Formato brasileiro
function formatarValorParaBR(valor) {
    if (typeof valor === 'number') {
        valor = valor.toFixed(2); // garante 2 casas
    }

    valor = valor.toString().replace('.', ','); // ponto para vírgula

    let partes = valor.split(',');
    let inteiro = partes[0];
    let decimal = partes[1] || '00';

    // Adiciona separador de milhar
    inteiro = inteiro.replace(/\B(?=(\d{3})+(?!\d))/g, '.');

    return `${inteiro},${decimal}`;
}


function ExibirAlerta(titulo, mensagem) {
    alert(titulo + ": " + mensagem);
}

/***********************************************************************************************************/
/*Modulo Classificação*/
/******************************************************************************************************/

function CarregarClassificacao() {

    // Inicializa o DataTable
    if ($.fn.DataTable.isDataTable('#tabelaClassificacao')) {
        $('#tabelaClassificacao').DataTable().destroy();
    }
    const tabela = $('#tabelaClassificacao').DataTable({
        "processing": false,
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

                            + row.classificacao + '\')"><i class="fas fa-sync mr-1"></i><span class="texto-botao"><texto-botao>Atualizar</texto-botao></span></button> ' +
                            '<button class="btn btn-sm btn-danger btn-responsivo" onclick="abrirModalExclusaoClassificacao(' + row.idclassificacao + ', \'' + row.classificacao + '\')"><i class="fas fa-trash-alt mr-1"></i><span class="texto-botao">Excluir</span></button>';
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

    $.ajax({
        url: '/agro/ClassificacaoServlet',
        method: 'POST',
        data: {
            classificacao: classificacao,
            acao: 'create'
        },
        success: function (resp) {
            mostrarAlerta(resp.msg || 'Classificação com sucesso!', 'success', '#alerta');
            $('#modalCadastro').modal('hide');
            $('#classificacao').val("");
            $('#tabelaClassificacao').DataTable().ajax.reload();
        },
        error: function (xhr) {
            let msg = 'Despesa não atualizada!';
            try {
                msg = JSON.parse(xhr.responseText).msg || msg;
            } catch (e) {
            }
            mostrarAlerta(msg, 'danger', '#modalClassificacaoCadastro', 4000);
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
        data: {
            id: id,
            classificacao: classificacao,
            acao: 'update'
        },
        success: function (resp) {

            mostrarAlerta(resp.msg || 'Despesa atualizada com sucesso!', 'info', '#alerta');
            $('#modalAtualizacao').modal('hide');
            $('#formAtualizacao')[0].reset();
            $('#tabelaClassificacao').DataTable().ajax.reload();
        },
        error: function (xhr) {
            let msg = 'Despesa não atualizada!';
            try {
                msg = JSON.parse(xhr.responseText).msg || msg;
            } catch (e) {
            }
            mostrarAlerta(msg, 'danger', '#modalClassificacaoAtualizar', 4000);
        }

    });
}

function excluirClassificacao() {
    const id = $('#exclusaoId').val();
    $.ajax({
        url: '/agro/ClassificacaoServlet',
        method: 'POST',
        data: {id: id,
            acao: 'delete'
        },
        success: function (resp) {
            mostrarAlerta(resp.msg || 'excluída com sucesso!', 'warning', '#alerta');
            $('#modalExclusao').modal('hide');
            $('#tabelaClassificacao').DataTable().ajax.reload();
        },
        error: function (xhr) {
            let msg = 'Classificação  não excluída!';
            try {
                msg = JSON.parse(xhr.responseText).msg || msg;
            } catch (e) {

            }
            mostrarAlerta(msg, 'danger', '#modalClassificacaoExcluir', 4000);


        }
    });
}

/******************************************************************************************************/
/* Modulo Despesas Custos*/
/******************************************************************************************************/

function abrirModalAtualizacaoDespesasCustos(id, despesa, unidade, valor, tipo) {
    // Atribui os valores aos campos do formulário de atualização
    $('#atualizacaoId').val(id);
    $('#atualizacaoDespesa').val(despesa);
    $('#atualizacaoUnidade').val(unidade);

    // ====> Aqui faz o ajuste no valor
    const valorNumerico = parseFloat(valor);
    const valorFormatado = formatarValorParaBR(valorNumerico);
    $('#atualizacaoValor').val(valorFormatado);

    $('#atualizacaoTipo').val(tipo);
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

function CarregarDadosDespesasCustos() {
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
            {"data": "iddespesascusto"},
            {"data": "despesascusto"},
            {"data": "unidadedespesascustos"},
            {
                "data": "valordespesascustos",
                "render": function (data, type, row) {
                    const valor = parseFloat(data);
                    return isNaN(valor) ? '-' : 'R$ ' + valor.toLocaleString('pt-BR', {minimumFractionDigits: 2, maximumFractionDigits: 2});
                }
            },
            {"data": "tipodespesascustos"},
            {
                "data": null,
                "render": function (data, type, row) {
                    return '<button class="btn btn-sm btn-warning btn-responsivo" onclick="abrirModalAtualizacaoDespesasCustos('
                            + row.iddespesascusto + ', \''
                            + row.despesascusto + '\', \''
                            + row.unidadedespesascustos + '\', \''
                            + row.valordespesascustos + '\', \''
                            + row.tipodespesascustos + '\')"><i class="fas fa-sync mr-1"></i><span class="texto-botao">Atualizar</span></button> ' +
                            '<button class="btn btn-sm btn-danger btn-responsivo" onclick="abrirModalExclusaoDespesasCustos(' + row.iddespesascusto + ', \'' + row.despesascusto + '\')"><i class="fas fa-trash-alt mr-1"></i><span class="texto-botao">Excluir</span></button>';
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
        success: function (resp) {
            mostrarAlerta(resp.msg || 'Despesa excluída com sucesso!', 'warning', '#alerta');
            $('#modalExclusao').modal('hide');
            $('#tabelaDespesasCustos').DataTable().ajax.reload();
        },
        error: function (xhr) {
            let msg = 'Despesa não excluída!';
            try {
                msg = JSON.parse(xhr.responseText).msg || msg;
            } catch (e) {
            }
            mostrarAlerta(msg, 'danger', '#alerta');
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
        success: function (resp) {
            mostrarAlerta(resp.msg || 'Despesa atualizada com sucesso!', 'success', '#alerta');
            $('#modalAtualizacao').modal('hide');
            $('#formAtualizacao')[0].reset();
            $('#tabelaDespesasCustos').DataTable().ajax.reload();
        },
        error: function (xhr) {
            let msg = 'Despesa não atualizada!';
            try {
                msg = JSON.parse(xhr.responseText).msg || msg;
            } catch (e) {
            }
            mostrarAlerta(msg, 'danger', '#alertModalAtualizacao', 4000);
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
        success: function (resp) {
            // resp: { ok:true, msg:"..." }
            mostrarAlerta(resp.msg || 'Despesa cadastrada com sucesso!', 'success', '#alerta');
            $('#modalCadastro').modal('hide');
            $('#formCadastro')[0].reset();
            $('#tabelaDespesasCustos').DataTable().ajax.reload();
        },
        error: function (xhr) {
            // espera { ok:false, msg:"...", target:"modalCadastro" }
            let msg = 'Erro ao cadastrar!';
            try {
                msg = JSON.parse(xhr.responseText).msg || msg;
            } catch (e) {
            }
            // chama alerta no modal de cadastro e fecha em 4 segundos
            mostrarAlerta(msg, 'danger', '#alertModalCadastro', 4000);
        }
    });
}
function resetFormModal($modal, formSelector = 'form') {
    const $form = $modal.find(formSelector);

    if ($form.length) {
        // reset nativo
        if ($form[0])
            $form[0].reset();

        // força limpeza (cobre inputs já setados via JS/máscaras/autofill)
        $form.find('input, textarea').not('[type=hidden]').val('');
        $form.find('select').prop('selectedIndex', 0).trigger('change');

        // se tiver máscaras, re-dispare o handler para reformatar vazio
        $form.find('input').trigger('input');
    }

    // limpa alertas do modal (se você usa contêiner de alerta)
    $modal.find('#alertModalCadastro').empty();
}

// Limpa quando o modal é ABERTO (garante estado inicial limpo)
$('#modalCadastro').on('show.bs.modal', function () {
    resetFormModal($(this));
});

// Limpa quando o modal é FECHADO (evita “herdar” dados na próxima abertura)
$('#modalCadastro').on('hidden.bs.modal', function () {
    resetFormModal($(this));
});


/******************************************************************************************************/
/* Alimento*/
/******************************************************************************************************/

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
        success: function (resp) {
            // Fecha o modal primeiro
            $('#modalFormulario').modal('hide');

            // Espera o modal ser escondido para mostrar o alerta
            $('#modalFormulario').one('hidden.bs.modal', function () {
                $('#formAlimento')[0].reset();
                $('#formAlimento').removeClass('was-validated');
                $('#tabelaalimentos').DataTable().ajax.reload();

                // 🔥 Aqui o alerta será visível, pois o modal já está fechado
                mostrarAlerta(resp.msg || 'Alimento cadastrado com sucesso!', 'success', '#alerta', 4000);
            });
        },
        error: function (xhr) {
            let msg = 'Erro ao cadastrar alimento.';
            let target = '#alerta';

            try {
                const json = JSON.parse(xhr.responseText);
                msg = json.msg || msg;
                if (json.target) {
                    target = '#' + json.target;
                }
            } catch (e) {
                console.error('Erro ao tratar JSON de erro:', e);
            }

            mostrarAlerta(msg, 'danger', target, 5000);
        }
    });
}
/* Atualizar alimento*/
function atualizarAlimento() {
    const alimento = $('#alimento').val();
    const variedade = $('#variedade').val();
    const id = $('#idproduto').val();

    const data = {
        acao: 'update',
        idproduto: id,
        alimento: alimento,
        variedade: variedade

    };
    $.ajax({
        url: '/agro/ControllerAlimento',
        method: 'POST',
        contentType: 'application/json',
        data: JSON.stringify(data),

        success: function (resp) {
            mostrarAlerta(resp.msg || 'Atualizada atualizada com sucesso!', 'success', '#alerta');
            sessionStorage.setItem("mensagemAlerta", resp.msg || 'Alimento atualizado com sucesso!');
            sessionStorage.setItem("tipoAlerta", "success");
            window.location.href = "http://localhost:8080/agro/view/admin/alimento.jsp";
            //$('#tabelaalimentos').DataTable().ajax.reload();
        },
        error: function (xhr) {
            let msg = 'Despesa não atualizada!';
            try {
                msg = JSON.parse(xhr.responseText).msg || msg;
                console.log("erro");
            } catch (e) {
            }
            mostrarAlerta(msg, 'danger', '#alerta', 4000);
        }
    });
}
/* Função para carregar classificações no select no cadastro modal Alimento da Pagina Gerenciamento de Alimento*/

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

/* Modulo para carregar  classificaçoes na pagina Atualização*/

function CarregarClassificacaoModalAtualizar(idproduto) {
    // Faz a requisição AJAX separada antes e injeta os dados manualmente
    $.ajax({
        url: '/agro/ControllerAlimento',
        method: 'POST',
        contentType: 'application/json; charset=utf-8',
        dataType: 'json',
        data: JSON.stringify({
            acao: 'atualizacaoalimentoclassificacaomodal',
            idproduto: idproduto
        }),
        success: function (classificacoes) {
            console.log("📦 Dados recebidos:", classificacoes);
            const select = $('#classificacaoSelect');
            select.empty();
            select.append('<option disabled value="">Selecione uma ou mais classificações...</option>');

            classificacoes.forEach(function (item) {
                const c = item.classificacao || item;
                select.append('<option value="' + c.idclassificacao + '">' + c.classificacao + '</option>');
            });
        },
        error: function (xhr) {
            console.error("❌ Erro no carregamento:", xhr.responseText);
            mostrarAlerta('Erro ao carregar classificações vinculadas.', 'danger', '#alerta');
        }
    });
}
/* Abertura de Modal de  Atualização*/
function editarAlimento(id) {
    $.ajax({
        url: '/agro/ControllerAlimento',
        method: 'GET',
        data: {id: id},
        dataType: 'json',
        success: function (data) {
            // Armazena os dados no sessionStorage para usar na próxima página
            sessionStorage.setItem('alimentoParaAtualizar', JSON.stringify(data));
            // Redireciona para página de atualização
            window.location.href = 'AtualizacaoAlimento.jsp';

        },
        error: function (xhr) {
            if (xhr.status === 404) {
                mostrarAlerta("Alimento não encontrado.", "danger");
            } else {
                mostrarAlerta("Erro ao buscar dados do alimento.", "danger");
            }
        }
    });
}

/* Carregamento de  tabela Alimento*/
function CarregarAlimento() {

    // Inicializa o DataTable
    if ($.fn.DataTable.isDataTable('#tabelaAlimento')) {
        $('#tabelaAlimento').DataTable().destroy();
    }
    const tabela = $('#tabelaAlimento').DataTable({
        "processing": false,
        "serverSide": false,

        "ajax": {
            "url": "/agro/ControllerAlimento",
            "method": "GET",
            "dataSrc": ""
        },
        "columns": [
            {"data": "idproduto"},
            {"data": "nomeproduto"},
            {
                "data": "situacaoproduto",

                "render": function (data) {
                    // Badge para situação
                    if (data) {
                        return '<span class="badge badge-success">Ativo</span>';
                    } else {
                        return '<span class="badge badge-danger">Inativo</span>';
                    }
                }

            },
            {"data": "tipoproduto"},
            {"data": "variedadealimento"},
            {
                "data": null,
                "title": "Ações",

                "render": function (data, type, row) {
                    return '<button class="btn btn-sm btn-warning btn-responsivo mr-3 ml-3" onclick="editarAlimento(' + row.idproduto + ')">' +
                            '<i class="fas fa-sync mr-1"></i><span class="texto-botao">Atualizar</span></button>';
                }
            }
        ],
        "language": {
            "url": 'https://cdn.datatables.net/plug-ins/2.1.6/i18n/pt-BR.json'
        }
    });
}

/*Carregar os dados da classificação para tabela atulizar*/

function CarregarClassificacaoAtualizarAlimento(idproduto) {
    console.log("🔍 Carregando classificações para o alimento ID:", idproduto);

    // Destroi o DataTable anterior, se existir
    if ($.fn.DataTable.isDataTable('#AtualizarCarregamentoClassificacao')) {
        $('#AtualizarCarregamentoClassificacao').DataTable().destroy();
    }

    // Faz a requisição AJAX separada antes e injeta os dados manualmente
    $.ajax({
        url: '/agro/ControllerAlimento',
        method: 'POST',
        contentType: 'application/json; charset=utf-8',
        dataType: 'json',
        data: JSON.stringify({
            acao: 'atualizacaoalimentoclassificacao',
            idproduto: idproduto
        }),
        success: function (json) {
            console.log("✅ Dados recebidos:", json);

            // Inicializa DataTable com os dados retornados
            $('#AtualizarCarregamentoClassificacao').DataTable({
                data: json.map(item => ({
                        idclassificacao: item.classificacao.idclassificacao,
                        classificacao: item.classificacao.classificacao,
                        idproduto: item.alimento.idproduto
                    })),
                columns: [
                    {data: "idclassificacao"},
                    {data: "classificacao"},
                    {data: "idproduto", visible: false}
                ],
                language: {
                    url: 'https://cdn.datatables.net/plug-ins/2.1.6/i18n/pt-BR.json'
                }
            });
        },
        error: function (xhr) {
            console.error("❌ Erro no carregamento:", xhr.responseText);
            mostrarAlerta('Erro ao carregar classificações vinculadas.', 'danger', '#alerta');
        }
    });
}

/* Salvar cadastro novo  classificação*/
function salvarClassificacaoModalAtualizacaoAlimento() {
    const idproduto = $('#idproduto').val();
    const classificacoesSelecionadas = $('#classificacaoSelect').val(); // array dos IDs selecionados

    if (!idproduto) {
        mostrarAlerta('ID do alimento não encontrado.', 'danger', '#alerta');
        return;
    }

    if (!classificacoesSelecionadas || classificacoesSelecionadas.length === 0) {
        mostrarAlerta('Selecione ao menos uma classificação.', 'warning', '#ModalAlimentoClassificacao');
        return;
    }

    // Monta o objeto para envio
    const data = {
        acao: 'salvarclassificacoesalimentomodal',
        idproduto: idproduto,
        classificacoes: classificacoesSelecionadas
    };

    console.log("📦 Enviando dados do modal:", data);

    $.ajax({
        url: '/agro/ControllerAlimento',
        method: 'POST',
        contentType: 'application/json; charset=utf-8',
        dataType: 'json',
        data: JSON.stringify(data),
        success: function (resp) {
            console.log("✅ Resposta:", resp);
            $('#modalClassificacao').modal('hide');
            mostrarAlerta(resp.msg || 'Classificações adicionadas com sucesso!', 'success', '#alerta');
            // Recarrega a tabela de classificações do alimento
            CarregarClassificacaoAtualizarAlimento(idproduto);
            CarregarClassificacaoModalAtualizar(idproduto);
        },
        error: function (xhr) {
            let msg = 'Classificação ja cadastrado';
            console.error("❌ Erro ao salvar:", xhr.responseText);
            mostrarAlerta(msg, 'danger', '#ModalAlimentoClassificacao');
        }
    });
}

/*>>>>>>>>>>>>>>>>>>>>>>Parceiro<<<<<<<<<<<<<<<<<<<<<<<<<< */
/*Carregamento de parceiro*/
function CarregarParceiros() {

    // Inicializa o DataTable
    if ($.fn.DataTable.isDataTable('#tabelaParceiros')) {
        $('#tabelaParceiros').DataTable().destroy();
    }
    const tabela = $('#tabelaParceiros').DataTable({
        "processing": false,
        "serverSide": false,

        "ajax": {
            "url": "/agro/ControllerParceiro",
            "method": "GET",
            "dataSrc": ""
        },
        "columns": [
            {"data": "idPessoa"},
            {"data": "nomePessoa"},
            {"data": "nivelPessoa"},
            {
                "data": "situacaoPessoa",

                "render": function (data) {
                    // Badge para situação
                    if (data) {
                        return '<span class="badge badge-success">Ativo</span>';
                    } else {
                        return '<span class="badge badge-danger">Inativo</span>';
                    }
                }

            },
            {"data": "telefonePessoa"},
            {"data": "razaoSocialPessoaCnpj"},
            {"data": "cnpjPessoaCnpj"},
            {
                "data": null,
                "title": "Ações",
"render": function (data, type, row) {
    const estaAtivo = row.situacaoPessoa === true || row.situacaoPessoa === "true";
    const textoBotao = estaAtivo ? "Desativar" : "Ativar";
    const classeBotao = estaAtivo ? "btn-danger" : "btn-success";
    const icone = estaAtivo ? "fa-user-slash" : "fa-user-check";

    return `
        <button class="btn btn-sm btn-warning btn-responsivo mr-2" onclick="editarAlimento(${row.idPessoa})">
            <i class="fas fa-sync mr-1"></i><span class="texto-botao">Atualizar</span>
        </button>
        <button class="btn btn-sm ${classeBotao} btn-responsivo" onclick="abrirModalDesativar(${row.idPessoa}, '${row.nomePessoa}', ${row.situacaoPessoa})">
            <i class="fas ${icone} mr-1"></i><span class="texto-botao">${textoBotao}</span>
        </button>
    `;
}



            }
        ],
        "language": {
            "url": 'https://cdn.datatables.net/plug-ins/2.1.6/i18n/pt-BR.json'
        }
    });
}
/* Salvamento de Parceiro*/

function salvarParceiro() {
    const nivel = $('#nivel').val();
    const data = {
        acao: "create", // ou "update"
        cnpj: $('#cnpj').val(),
        razaosocial: $('#razao').val(),
        site: $('#site').val(),
        inscricaoestadual: $('#inscricaoestadual').val(),
        nome: $('#nome').val(),
        email: $('#email').val(),
        telefone: $('#telefone').val(),
        nivel: nivel,
        cep: $('#cep').val(),
        numero: $('#numero').val(),
        complemento: $('#complemento').val(),
        situacao: "Ativo"
    };

    // Só adiciona usuário e senha se for "parceiro"
    if (nivel === 'parceiro') {
        data.usuario = $('#usuario').val();
        data.senha = $('#senha').val();
    }

    $.ajax({
        url: '/agro/ControllerParceiro', // ajuste conforme necessário
        method: 'POST',
        contentType: 'application/json',
        data: JSON.stringify(data),
        success: function (resp) {
            mostrarAlerta(resp.msg || 'Parceiro cadastrado com sucesso!', 'success', '#alerta');
            sessionStorage.setItem("mensagemAlerta", resp.msg || 'Parceiro cadastrado com sucesso!');
            sessionStorage.setItem("tipoAlerta", "success");
            window.location.href = "http://localhost:8080/agro/view/admin/parceiro.jsp";
        },
        error: function (xhr) {
            let msg = 'Erro ao cadastrar parceiro!';
            try {
                msg = JSON.parse(xhr.responseText).msg || msg;
            } catch (e) {
            }
            mostrarAlerta(msg, 'danger', '#alerta', 4000);
        }
    });
}
// Atualizar de parceiro
function updateParceiro() {
    const nivel = $('#nivel').val();

    const data = {
        acao: "update", // ou "update"
        idpessoa: $('#idpessoa').val(),
        cnpj: $('#cnpj').val(),
        razaosocial: $('#razao').val(),
        site: $('#site').val(),
        inscricaoestadual: $('#inscricaoestadual').val(),
        nome: $('#nome').val(),
        email: $('#email').val(),
        telefone: $('#telefone').val(),
        nivel: nivel,
        cep: $('#cep').val(),
        numero: $('#numero').val(),
        complemento: $('#complemento').val(),
        situacao: "Ativo"
    };

    // Só adiciona usuário e senha se for "parceiro"
    if (nivel === 'parceiro') {
        data.usuario = $('#usuario').val();
        data.senha = $('#senha').val();
    }

    $.ajax({
        url: '/agro/ControllerParceiro', // ajuste conforme necessário
        method: 'POST',
        contentType: 'application/json',
        data: JSON.stringify(data),
        success: function (resp) {
            mostrarAlerta(resp.msg || 'Parceiro cadastrado com sucesso!', 'success', '#alerta');
            sessionStorage.setItem("mensagemAlerta", resp.msg || 'Parceiro cadastrado com sucesso!');
            sessionStorage.setItem("tipoAlerta", "success");
            carregarResumoParceiros();
            window.location.href = "http://localhost:8080/agro/view/admin/parceiro.jsp";
        },
        error: function (xhr) {
            let msg = 'Erro ao cadastrar parceiro!';
            try {
                msg = JSON.parse(xhr.responseText).msg || msg;
            } catch (e) {
            }
            mostrarAlerta(msg, 'danger', '#alerta', 4000);
        }
    });
}

/** Modulo de Desativação do  parceiro*/
if (typeof parceiroParaDesativar === "undefined") {
    var parceiroParaDesativar = null;
}
if (typeof situacaopessoa === "undefined") {
    var situacaopessoa = null;
}

function abrirModalDesativar(id, nome, situacaoAtual) {
    parceiroParaDesativar = id;
    situacaopessoa = situacaoAtual;

    // Define se a ação será ativar ou desativar
    const vaiDesativar = situacaoAtual === true || situacaoAtual === "true";

    const titulo = vaiDesativar ? "Confirmar Desativação" : "Confirmar Ativação";
    const mensagem = vaiDesativar
        ? `Você deseja desativar o parceiro "${nome}"?`
        : `Você deseja ativar o parceiro "${nome}"?`;

    const classeBotao = vaiDesativar ? "btn-danger" : "btn-success";
    const textoBotao = vaiDesativar ? "Desativar" : "Ativar";
    const iconeBotao = vaiDesativar ? "fa-user-slash" : "fa-user-check";

    $("#modalDesativarLabel").text(titulo);
    $("#mensagemDesativar").text(mensagem);

    const $btn = $("#btnConfirmarDesativar");
    $btn.removeClass("btn-danger btn-success").addClass(classeBotao);
    $btn.html(`<i class="fas ${iconeBotao} mr-1"></i>${textoBotao}`);

    $("#modalDesativar").modal("show");
}
// Ao clicar no botão "Desativar" dentro do modal
$("#btnConfirmarDesativar").on("click", function () {
    if (!parceiroParaDesativar)
        return;

    $.ajax({
        url: `/agro/ControllerParceiro`,
        method: "POST",
        contentType: "application/json",
        data: JSON.stringify({
            acao: "delete",
            situacao: situacaopessoa,
            idPessoa: parceiroParaDesativar
        }),
        success: function (resp) {
            sessionStorage.setItem("mensagemAlerta", resp.msg || 'Parceiro alterado com sucesso!');
            sessionStorage.setItem("tipoAlerta", "info");
            CarregarParceiros();
            carregarResumoParceiros();
            $("#modalDesativar").modal("hide");
        },
        error: function () {
            $("#modalDesativar").modal("hide");
            mostrarAlerta("Erro ao alterar parceiro!", "danger", "#alerta");
        }
    });
});


/** Carregaremento do dados*/
function carregarResumoParceiros() {
    $.ajax({
        url: "/agro/ControllerParceiro?acao=dados", // importante!
        method: "get",
        dataType: "json",
        success: function (dados) {
            $("#total").text(dados.total);
            $("#ativosParceiros").text(dados.ativos);
            $("#inativosParceiros").text(dados.inativos);
            console.log('total:' + dados.total);
        },
        error: function () {
            console.warn("❌ Não foi possível carregar o resumo de parceiros.");
        }
    });
}





           