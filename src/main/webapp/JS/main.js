/******************************************************************************************************/
/* MÓDULO GERAL — funções utilitárias compartilhadas por todas as páginas                            */
/******************************************************************************************************/

/**
 * Valida formato de e-mail.
 * Usado em: todas as páginas que possuem input[type="email"] (ColaboCadastro.jsp,
 *           CadastroParceiro.jsp, Colaboradores.jsp, AtualizarParceiro.jsp, etc.)
 */
function isEmailValido(email) {
    if (!email) return false;
    email = String(email).trim();

    // regex simples e segura para sistemas web
    const re = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/i;
    return re.test(email);
}

/**
 * Aplica feedback visual Bootstrap (is-valid / is-invalid) em um campo de e-mail.
 * Usado em: todas as páginas com input[type="email"] — chamada por isEmailValido()
 */
function aplicarFeedbackEmail($el, valido, msg) {
    $el.removeClass('is-valid is-invalid')
       .addClass(valido ? 'is-valid' : 'is-invalid');

    if (!valido) {
        const $fb = $el.closest('.form-group').find('.invalid-feedback').first();
        if ($fb.length) {
            $fb.text(msg || 'Informe um e-mail válido.');
        }
    }
}

/**
 * Inicializa validação automática de e-mails via delegação de eventos.
 * Executada uma vez no carregamento — funciona para QUALQUER página sem ajuste no JSP.
 * Usado em: todas as páginas (chamada na configuração geral ao final deste arquivo)
 */
function initValidacaoEmailAutomatica() {

    // 1️⃣ BLUR: valida quando sai do campo
    $(document).off('blur.validacaoEmail', 'input[type="email"]')
      .on('blur.validacaoEmail', 'input[type="email"]', function () {

        const $el = $(this);
        const valor = ($el.val() || '').trim();

        // campo vazio → inválido se for required
        if ($el.prop('required') && valor === '') {
            aplicarFeedbackEmail($el, false, 'E-mail obrigatório.');
            return;
        }

        // se não estiver vazio, valida formato
        if (valor !== '' && !isEmailValido(valor)) {
            aplicarFeedbackEmail($el, false, 'Informe um e-mail válido.');
            return;
        }

        // ok
        aplicarFeedbackEmail($el, true);
    });


    // 2️⃣ SUBMIT: bloqueia envio se existir email inválido
    $(document).off('submit.validacaoEmail')
      .on('submit.validacaoEmail', 'form', function (e) {

        let ok = true;
        const $form = $(this);

        $form.find('input[type="email"]').each(function () {
            const $el = $(this);
            const valor = ($el.val() || '').trim();

            if ($el.prop('required') && valor === '') {
                aplicarFeedbackEmail($el, false, 'E-mail obrigatório.');
                ok = false;
                return;
            }

            if (valor !== '' && !isEmailValido(valor)) {
                aplicarFeedbackEmail($el, false, 'Informe um e-mail válido.');
                ok = false;
                return;
            }

            aplicarFeedbackEmail($el, true);
        });

        if (!ok) {
            e.preventDefault();
            e.stopImmediatePropagation();

            if (typeof mostrarAlerta === 'function') {
                mostrarAlerta('Revise os e-mails informados.', 'warning', '#alerta', 4000);
            }
        }
    });
}


/**
 * Exibe uma mensagem Bootstrap (success/danger/warning/info) em um container de alerta.
 * Usado em: todas as páginas — é a função padrão de feedback ao usuário do sistema.
 * @param {string} mensagem  Texto da mensagem
 * @param {string} tipo      Classe Bootstrap: 'success' | 'danger' | 'warning' | 'info'
 * @param {string} seletor   Seletor jQuery do elemento alerta (ex: '#alerta')
 * @param {number} autoHideMs Milissegundos para auto-ocultar (0 = não oculta)
 */
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


/**
 * Converte valor numérico em string formatada no padrão brasileiro (ex: "1.234,56").
 * Usado em: despesascustos.jsp — exibição e edição de valores monetários
 */
function formatarMoeda(valor) {
    valor = valor.replace(/\D/g, ''); // Remove tudo que não for dígito
    valor = (valor / 100).toFixed(2) + ''; // Divide por 100 e adiciona 2 casas decimais
    valor = valor.replace('.', ','); // Troca ponto decimal por vírgula
    valor = valor.replace(/\B(?=(\d{3})+(?!\d))/g, '.'); // Adiciona pontos nos milhares
    return valor;
}
/**
 * Formata um número para o padrão BR com separadores de milhar e vírgula decimal.
 * Usado em: despesascustos.jsp — preenchimento do modal de atualização de despesa
 */
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

/**
 * Consulta o ViaCEP e preenche os campos #logradouro, #bairro, #cidade, #uf.
 * Usado em: ColaboCadastro.jsp, CadastroParceiro.jsp, AtualizarParceiro.jsp
 */
function preencherEnderecoViaCep(cep) {
    if (!cep) return;

    const cepLimpo = String(cep).replace(/\D/g, '');
    if (cepLimpo.length !== 8) return;

    $.getJSON(`https://viacep.com.br/ws/${cepLimpo}/json/`)
        .done(function (data) {
            if (data.erro) {
                console.warn("CEP não encontrado no ViaCEP.");
                return;
            }
            $("#logradouro").val(data.logradouro || "");
            $("#bairro").val(data.bairro || "");
            $("#cidade").val(data.localidade || "");
            $("#uf").val(data.uf || "");
        })
        .fail(function () {
            console.warn("Erro ao consultar o ViaCEP.");
        });
}

/**
 * Exibe um alert nativo do navegador com título e mensagem.
 * Usado em: despesascustos.jsp — fallback de validação no modal de atualização
 */
function ExibirAlerta(titulo, mensagem) {
    alert(titulo + ": " + mensagem);
}

/* validarCPF — acessível globalmente para ColaboCadastro.jsp e futuras páginas */
function validarCPF(cpf) {
    cpf = cpf.replace(/[^\d]+/g, '');
    if (cpf.length !== 11) return false;
    if (/^(\d)\1+$/.test(cpf)) return false;
    let soma = 0;
    for (let i = 0; i < 9; i++) soma += parseInt(cpf.charAt(i)) * (10 - i);
    let d1 = 11 - (soma % 11);
    if (d1 >= 10) d1 = 0;
    if (d1 !== parseInt(cpf.charAt(9))) return false;
    soma = 0;
    for (let i = 0; i < 10; i++) soma += parseInt(cpf.charAt(i)) * (11 - i);
    let d2 = 11 - (soma % 11);
    if (d2 >= 10) d2 = 0;
    return d2 === parseInt(cpf.charAt(10));
}



/***********************************************************************************************************/
/* MÓDULO CLASSIFICAÇÃO — classificacao.jsp                                                               */
/***********************************************************************************************************/

/**
 * Carrega e inicializa o DataTable com as classificações cadastradas.
 * Usado em: classificacao.jsp
 */
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
                    return '<button class="btn-acao btn-acao-editar" title="Editar" onclick="abrirModalAtualizacaoClassificacao('
                            + row.idclassificacao + ', \''
                            + row.classificacao + '\')"><i class="fas fa-pen-to-square"></i></button> '
                            + '<button class="btn-acao btn-acao-excluir" title="Excluir" onclick="abrirModalExclusaoClassificacao(' + row.idclassificacao + ', \'' + row.classificacao + '\')"><i class="fas fa-trash-can"></i></button>';
                }
            }
        ],
        "language": {
            "url": 'https://cdn.datatables.net/plug-ins/2.1.6/i18n/pt-BR.json'
        }
    });
}

/** Abre o modal de edição preenchendo id e nome da classificação. Usado em: classificacao.jsp */
function abrirModalAtualizacaoClassificacao(id, classificacao) {
    $('#atualizacaoId').val(id);
    $('#atualizacaoClassificacao').val(classificacao);
    $('#modalAtualizacao').modal('show');
}

/** Abre o modal de exclusão com id e nome da classificação a remover. Usado em: classificacao.jsp */
function abrirModalExclusaoClassificacao(id, classificacao) {
    $('#exclusaoId').val(id);
    $('#exclusaoClassificacao').text(classificacao);
    $('#modalExclusao').modal('show');
}

/** Envia POST para criar nova classificação via ClassificacaoServlet. Usado em: classificacao.jsp */
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

/** Envia POST para atualizar nome de uma classificação existente. Usado em: classificacao.jsp */
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

/** Envia POST para excluir a classificação selecionada no modal. Usado em: classificacao.jsp */
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
/* MÓDULO DESPESAS E CUSTOS — despesascustos.jsp                                                      */
/******************************************************************************************************/

/** Abre o modal de edição preenchendo os campos com os dados da despesa selecionada. Usado em: despesascustos.jsp */
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

/** Abre o modal de exclusão com o id e nome da despesa/custo. Usado em: despesascustos.jsp */
function abrirModalExclusaoDespesasCustos(id, despesa) {
    // Define os valores necessários para a exclusão
    $('#exclusaoId').val(id);
    // Exibe o nome da despesa/custo no modal de exclusão (usando text() para um elemento <strong>)
    $('#exclusaoDespesa').text(despesa);
    // Exibe a modal de exclusão
    $('#modalExclusao').modal('show');
}

/** Inicializa/recarrega o DataTable de despesas e custos via DespesasCustosServlet. Usado em: despesascustos.jsp */
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
                    return '<button class="btn-acao btn-acao-editar" title="Editar" onclick="abrirModalAtualizacaoDespesasCustos('
                            + row.iddespesascusto + ', \''
                            + row.despesascusto + '\', \''
                            + row.unidadedespesascustos + '\', \''
                            + row.valordespesascustos + '\', \''
                            + row.tipodespesascustos + '\')"><i class="fas fa-pen-to-square"></i></button> '
                            + '<button class="btn-acao btn-acao-excluir" title="Excluir" onclick="abrirModalExclusaoDespesasCustos(' + row.iddespesascusto + ', \'' + row.despesascusto + '\')"><i class="fas fa-trash-can"></i></button>';
                }
            }
        ],
        "language": {
            "url": 'https://cdn.datatables.net/plug-ins/2.1.6/i18n/pt-BR.json' // Tradução para o Português
        }
    });
}

/** Envia POST para excluir a despesa/custo confirmado no modal. Usado em: despesascustos.jsp */
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

/** Envia POST para atualizar os dados de uma despesa/custo. Usado em: despesascustos.jsp */
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

/** Envia POST para criar nova despesa/custo via DespesasCustosServlet. Usado em: despesascustos.jsp */
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
/**
 * Limpa todos os campos de um modal Bootstrap e reseta o formulário interno.
 * Usado em: despesascustos.jsp — limpa o modal de cadastro ao abrir/fechar
 */
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

// Limpa quando o modal é ABERTO (garante estado inicial limpo) — despesascustos.jsp
$('#modalCadastro').on('show.bs.modal', function () {
    resetFormModal($(this));
});

// Limpa quando o modal é FECHADO (evita “herdar” dados na próxima abertura) — despesascustos.jsp
$('#modalCadastro').on('hidden.bs.modal', function () {
    resetFormModal($(this));
});


/******************************************************************************************************/
/* MÓDULO ALIMENTO — alimento.jsp e AtualizacaoAlimento.jsp                                          */
/******************************************************************************************************/

/** Envia POST para cadastrar novo alimento com classificações selecionadas. Usado em: alimento.jsp */
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
                $('#formAlimento').removeClass('was-ted');
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
/** Envia POST para atualizar nome e variedade do alimento e redireciona para alimento.jsp. Usado em: AtualizacaoAlimento.jsp */
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
/** Popula o select #SelectClassificacao com todas as classificações disponíveis. Usado em: alimento.jsp (modal de cadastro) */
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

/** Popula o select #classificacaoSelect com as classificações disponíveis para vinculação. Usado em: AtualizacaoAlimento.jsp */
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
/**
 * Busca os dados do alimento por id, salva no sessionStorage e redireciona para AtualizacaoAlimento.jsp.
 * Usado em: alimento.jsp (botão Editar na tabela)
 */
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

/** Inicializa/recarrega o DataTable de alimentos via ControllerAlimento. Usado em: alimento.jsp */
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
                    return data
                        ? '<span class="badge bg-success">Ativo</span>'
                        : '<span class="badge bg-danger">Inativo</span>';
                }

            },
            {"data": "tipoproduto"},
            {"data": "variedadealimento"},
            {
                "data": null,
                "title": "Ações",

                "render": function (data, type, row) {
                    return '<button class="btn-acao btn-acao-editar" title="Editar" onclick="editarAlimento(' + row.idproduto + ')"><i class="fas fa-pen-to-square"></i></button>';
                }
            }
        ],
        "language": {
            "url": 'https://cdn.datatables.net/plug-ins/2.1.6/i18n/pt-BR.json'
        }
    });
}

/** Carrega e inicializa o DataTable #AtualizarCarregamentoClassificacao com as classificações do alimento. Usado em: AtualizacaoAlimento.jsp */
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

/** Vincula as classificações selecionadas no modal ao alimento em edição. Usado em: AtualizacaoAlimento.jsp */
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

/******************************************************************************************************/
/* MÓDULO PARCEIRO — parceiro.jsp e CadastroParceiro.jsp                                              */
/******************************************************************************************************/

/** Inicializa/recarrega o DataTable de parceiros via ControllerParceiro. Usado em: parceiro.jsp */
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
                    return data
                        ? '<span class="badge bg-success">Ativo</span>'
                        : '<span class="badge bg-danger">Inativo</span>';
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
    const classeToggle = estaAtivo ? "btn-acao-desativar" : "btn-acao-ativar";
    const icone = estaAtivo ? "fa-ban" : "fa-circle-check";

    return `<button class="btn-acao btn-acao-editar" title="Editar"
        onclick="window.location.href='/agro/view/admin/CadastroParceiro.jsp?id=${row.idPessoa}'">
        <i class="fas fa-pen-to-square"></i>
    </button>
    <button class="btn-acao ${classeToggle}" title="${textoBotao}"
        onclick="abrirModalDesativar(${row.idPessoa}, '${row.nomePessoa}', ${row.situacaoPessoa})">
        <i class="fas ${icone}"></i>
    </button>`;
}



            }
        ],
        "language": {
            "url": 'https://cdn.datatables.net/plug-ins/2.1.6/i18n/pt-BR.json'
        }
    });
}



//** Atualizar de parceiro*/


/* Estado global para controle do modal desativar/ativar parceiro — parceiro.jsp */
if (typeof parceiroParaDesativar === "undefined") {
    var parceiroParaDesativar = null;
}
if (typeof situacaopessoa === "undefined") {
    var situacaopessoa = null;
}

/** Abre o modal de confirmação para ativar/desativar parceiro, ajustando texto e ícone. Usado em: parceiro.jsp */
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
// Confirma e executa ativar/desativar parceiro via ControllerParceiro. Usado em: parceiro.jsp
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


/** Carrega totais (total/ativos/inativos) e atualiza os badges de resumo na tela. Usado em: parceiro.jsp */
function carregarResumoParceiros() {
    $.ajax({
        url: "/agro/ControllerParceiro?acao=dados", // importante!
        method: "get",
        dataType: "json",
        success: function (dados) {
            $("#total").text(dados.total);
            $("#ativosParceiros").text(dados.ativos);
            $("#inativosParceiros").text(dados.inativos);
        },
        error: function () {
            console.warn("❌ Não foi possível carregar o resumo de parceiros.");
        }
    });
}

/** Busca os dados de um parceiro por id e preenche o formulário para edição. Usado em: CadastroParceiro.jsp (modo update via ?id=) */
function carregarDadosParceiro(id) {
  $.ajax({
    url: `/agro/ControllerParceiro?id=${id}`,
    method: "GET",
    dataType: "json",
    success: function (parceiro) {
      $("#idPessoa").val(parceiro.idPessoa);
      $("#nome").val(parceiro.nomePessoa);
      $("#usuario").val(parceiro.usuarioPessoa);
      $("#senha").val(parceiro.senhaPessoa);
      $("#nivel").val(parceiro.nivelPessoa).trigger("change");
      $("#email").val(parceiro.emailPessoa);
      $("#telefone").val(parceiro.telefonePessoa);

      // Endereço
      $("#cep").val(parceiro.Cep || "");
      $("#numero").val(parceiro.numero|| "");
      $("#complemento").val(parceiro.complemento|| "");
       preencherEnderecoViaCep(parceiro.Cep || "" );
      $("#razao").val(parceiro.razaoSocialPessoaCnpj);
      $("#cnpj").val(parceiro.cnpjPessoaCnpj);
      $("#inscricaoestadual").val(parceiro.inscricaoEstadualPessoaCnpj);
      $("#site").val(parceiro.sitePessoaCnpj || "");

      // Situação
      const sit = (parceiro.situacaoPessoa === true || parceiro.situacaoPessoa === "true") ? "true" : "false";
      $("#situacao").val(sit);

      // Preenche logradouro, bairro, cidade, uf via cep
      if (parceiro.cepPessoa) preencherEnderecoViaCep(parceiro.cepPessoa);
    },
    error: function (xhr) {
      console.error("Erro GET parceiro:", xhr.responseText);
      mostrarAlerta("Erro ao carregar dados do parceiro!", "danger", "#alerta");
    }
  });
}
/**
 * Detecta se é cadastro (create) ou edição (update) pelo campo #idPessoa e envia POST.
 * Redireciona para parceiro.jsp após sucesso.
 * Usado em: CadastroParceiro.jsp
 */
function salvarParceiroCreateOuUpdate()
{
  const idPessoa = $("#idPessoa").val();
  const nivel = $("#nivel").val();

  // detecta se é create ou update
 const acao = idPessoa  !== "" ? "update" : "create";

 
  const data = {
    acao: acao,
    idPessoa: idPessoa,
    nome: $("#nome").val(),
    nivel: nivel,
    email: $("#email").val(),
    telefone: $("#telefone").val(),
    cep: $("#cep").val(),
    numero: $("#numero").val(),
    complemento: $("#complemento").val(),
    situacao: $("#situacao").val(),
    cnpj: $("#cnpj").val(),
    razaosocial: $("#razao").val(),
    inscricaoestadual: $("#inscricaoestadual").val(),
    site: $("#site").val()
  };

  if (nivel === "parceiro") {
    data.usuario = $("#usuario").val();
    data.senha = $("#senha").val();
  }

  $.ajax({
      url: "/agro/ControllerParceiro",
  method: "POST",
  contentType: "application/json; charset=utf-8",
  dataType: "json",
  data: JSON.stringify(data),
   
    success: function (resp) {
      // diferencia mensagens de sucesso
     
      if(  acao === "create"){
          let msgSucesso =resp.msg || "Parceiro cadastrado com sucesso!";
           sessionStorage.setItem("mensagemAlerta", msgSucesso);
      sessionStorage.setItem("tipoAlerta", "success");
      window.location.href = "/agro/view/admin/parceiro.jsp";
      }else{
          let msgSucesso =resp.msg || "Parceiro atualizado com sucesso!";
           sessionStorage.setItem("mensagemAlerta", msgSucesso);
      sessionStorage.setItem("tipoAlerta", "success");
      window.location.href = "/agro/view/admin/parceiro.jsp";
      }
    
     
    },
    error: function (xhr) {
      console.error("Erro POST parceiro:", xhr.responseText);
      // diferencia mensagens de erro
             if(  acao === "create"){
           let msgErro = "Erro ao cadastrar parceiro!";
           mostrarAlerta(msgErro, "danger", "#alerta", 4000);

      }else{
         let msgErro ="Erro ao atualizar parceiro!";
         mostrarAlerta(msgErro, "danger", "#alerta", 4000);

      }
      try {
        const json = JSON.parse(xhr.responseText);
        if (json.msg) msgErro = json.msg; 
        mostrarAlerta(msgErro, "danger", "#alerta", 4000);
        
      } catch (e) {
        // ignora parse
      }
      mostrarAlerta(msgErro, "danger", "#alerta", 4000);
    }
  });
}

/******************************************************************************************************/
/* MÓDULO COLABORADOR — ColaboCadastro.jsp e Colaboradores.jsp                                        */
/******************************************************************************************************/

/**
 * Reseta o formulário #formFuncionario para o estado inicial (campos vazios, sem validações).
 * Usado em: ColaboCadastro.jsp (botão Limpar e após salvar)
 */
function resetFuncionarioForm() {
  if (!$('#formFuncionario').length) return;

  const $form = $('#formFuncionario');

  $form[0].reset();
  $form.removeClass('was-validated');
  $form.find('.is-valid, .is-invalid').removeClass('is-valid is-invalid');

  $('#idPessoa').val('');
  $('#logradouro, #bairro, #cidade, #uf').val('');

  const $box = $('#boxTipoInfo');
  if ($box.length) {
    $box.attr('class', 'alert alert-info mb-0')
      .html('Selecione o <strong>Tipo Funcionário</strong> para ver dicas.');
  }

  if (typeof mostrarAlerta === 'function') {
    mostrarAlerta('Formulário limpo.', 'info', '#alerta', 2500);
  }

  $('#nomePessoa').focus();
}


/**
 * Converte dd/mm/yyyy → yyyy-MM-dd para envio ao backend.
 * Aceita yyyy-MM-dd como passthrough e retorna null se vazio.
 * Usado em: ColaboCadastro.jsp (submit do formulário de funcionário)
 */
function converterDataParaISO(valor) {
  if (!valor) return null;
  if (/^\d{4}-\d{2}-\d{2}$/.test(valor)) return valor; // já é ISO
  const partes = valor.split('/');
  if (partes.length === 3) return partes[2] + '-' + partes[1] + '-' + partes[0];
  return null;
}

/**
 * Converte yyyy-MM-dd → dd/mm/yyyy para exibição no datepicker.
 * Suporta também objetos {year, monthValue, dayOfMonth} retornados pelo Gson.
 * Usado em: ColaboCadastro.jsp (preenchimento do formulário no modo edição)
 */
function converterDataParaBR(isoDate) {
  if (!isoDate) return '';
  if (typeof isoDate === 'object') {
    // Gson pode enviar objeto {year,monthValue,dayOfMonth} em fallback
    var y = isoDate.year, m = String(isoDate.monthValue).padStart(2,'0'), d = String(isoDate.dayOfMonth).padStart(2,'0');
    return d + '/' + m + '/' + y;
  }
  var partes = String(isoDate).split('-');
  if (partes.length === 3) return partes[2] + '/' + partes[1] + '/' + partes[0];
  return isoDate;
}

/**
 * Preenche todos os campos do formulário #formFuncionario com os dados de um funcionário existente.
 * Usado em: ColaboCadastro.jsp (modo edição, após buscar dados via GET ?id=)
 */
function preencherFormularioEdicao(f) {
  $('#idPessoa').val(f.idPessoa || '');
  $('#nomePessoa').val(f.nomePessoa || '');
  $('#emailPessoa').val(f.emailPessoa || '');
  $('#telefonePessoa').val(f.telefonePessoa || '');
  $('#usuarioPessoa').val(f.usuarioPessoa || '');
  $('#senhaPessoa').val(f.senhaPessoa || '');
  $('#nivelPessoa').val((f.nivelPessoa || '').toLowerCase());
  $('#situacaoPessoa').val(String(f.situacaoPessoa));
  $('#cpfPf').val(f.cpfPf || '');
  $('#matricula').val(f.matricula || '');
  $('#tipoFuncionario').val(f.tipoFuncionario || '').trigger('change');
  $('#cargo').val(f.cargo || '');
  $('#cep').val(f.Cep || f.cep || '');
  $('#numero').val(f.numero || '');
  $('#complemento').val(f.complemento || '');

  // Data de nascimento: converter ISO → dd/mm/yyyy e setar no campo
  var dataNasc = converterDataParaBR(f.dataNascimentoPf);
  $('#dataNascimentoPf').val(dataNasc);
  if ($.fn.datepicker && dataNasc) {
    try { $('#dataNascimentoPf').datepicker('update', dataNasc); } catch (e) {}
  }

  // Datas de admissão e desligamento (ISO → dd/mm/yyyy)
  var dataIni = converterDataParaBR(f.dataInicio);
  var dataFim = converterDataParaBR(f.dataFim);
  $('#dataInicio').val(dataIni);
  $('#dataFim').val(dataFim);
  if ($.fn.datepicker) {
    try { if (dataIni) $('#dataInicio').datepicker('update', dataIni); } catch (e) {}
    try { if (dataFim) $('#dataFim').datepicker('update', dataFim); } catch (e) {}
  }

  // Ajustar título e botão para modo edição
  $('#tituloFormulario').html('<i class="fas fa-user-edit"></i> Editar Funcionário');
  $('#btnSalvar').html('<i class="fas fa-save"></i> Salvar Alterações');
}

/**
 * Inicializa toda a lógica da página de cadastro/edição de funcionário:
 * detecta modo (create/update via ?id=), aplica máscaras, datepicker,
 * toggle de senha, dica de tipo, CEP e submit com validação.
 * Chamada pelo guard $(document).ready em ColaboCadastro.jsp.
 * Usado em: ColaboCadastro.jsp
 */
function initFuncionarioCadastro() {
  if (!$('#formFuncionario').length) return;

  // ======== Máscaras ========
  if ($.fn.mask) {
    $('#cep').mask('00000-000');
    $('#cpfPf').mask('000.000.000-00');
    $('#dataNascimentoPf').mask('00/00/0000');
    $('#dataInicio').mask('00/00/0000');
    $('#dataFim').mask('00/00/0000');
    $('#telefonePessoa').mask('(00) 00000-0000').on('blur', function () {
      $(this).mask($(this).val().length === 14 ? '(00) 0000-0000' : '(00) 00000-0000');
    });
  }

  // Somente números no campo número do endereço
  $(document).off('input', '#numero').on('input', '#numero', function () {
    this.value = this.value.replace(/\D/g, '').slice(0, 6);
  });

  // ======== Modo Edição: detectar ?id= na URL ========
  var urlParams = new URLSearchParams(window.location.search);
  var editId = urlParams.get('id');
  if (editId) {
    $.ajax({
      url: (window.__ctxPath || '') + '/ControllerFuncionario?id=' + editId,
      method: 'GET',
      dataType: 'json',
      success: function (f) {
        if (f && f.idPessoa) {
          preencherFormularioEdicao(f);
        } else {
          mostrarAlerta('Funcionário não encontrado.', 'danger', '#alerta', 5000);
        }
      },
      error: function () {
        mostrarAlerta('Erro ao carregar dados do funcionário.', 'danger', '#alerta', 5000);
      }
    });
  }

  // ======== Calendário (Data Nascimento) ========
  // Usa bootstrap-datepicker se estiver carregado; senão, apenas mantém o input.
  if ($.fn.datepicker) {
    $('#dataNascimentoPf').datepicker({
      format: 'dd/mm/yyyy',
      language: 'pt-BR',
      autoclose: true,
      todayHighlight: true,
      endDate: new Date() // impede datas futuras
    });

    // botão que abre o calendário
    $(document).off('click', '#btnAbrirCalendario')
      .on('click', '#btnAbrirCalendario', function () {
        $('#dataNascimentoPf').datepicker('show');
      });

    // Datepickers de admissão e desligamento
    $('#dataInicio, #dataFim').datepicker({
      format: 'dd/mm/yyyy',
      language: 'pt-BR',
      autoclose: true,
      todayHighlight: true
    });
    $(document).off('click', '#btnAbrirCalInicio')
      .on('click', '#btnAbrirCalInicio', function () { $('#dataInicio').datepicker('show'); });
    $(document).off('click', '#btnAbrirCalFim')
      .on('click', '#btnAbrirCalFim', function () { $('#dataFim').datepicker('show'); });
  }

  // ======== Toggle senha ========
  $(document).off('click', '#btnToggleSenha')
    .on('click', '#btnToggleSenha', function () {
      const $inp = $('#senhaPessoa');
      const isPass = $inp.attr('type') === 'password';
      $inp.attr('type', isPass ? 'text' : 'password');
      $(this).find('i').toggleClass('fa-eye fa-eye-slash');
    });

  // ======== Dica por tipo ========
  function atualizarBoxTipoInfo() {
    const tipo = $('#tipoFuncionario').val();
    const $box = $('#boxTipoInfo');
    if (!$box.length) return;

    if (!tipo) {
      $box.attr('class', 'alert alert-info mb-0')
        .html('Selecione o <strong>Tipo Funcionário</strong> para ver dicas.');
      return;
    }

    const map = {
      CLT: { cls: 'alert alert-primary mb-0', html: 'CLT: cadastro padrão. Cálculos de horas/faltas/extra no módulo CLT.' },
      DIARISTA: { cls: 'alert alert-success mb-0', html: 'Diária: controle por dias trabalhados.' },
      EMPREITA: { cls: 'alert alert-warning mb-0', html: 'Empreita: pagamento por valor fixo do serviço.' },
      PRODUCAO: { cls: 'alert alert-secondary mb-0', html: 'Produção: pagamento por produção (ex.: caixas).' }
    };

    const cfg = map[tipo] || { cls: 'alert alert-info mb-0', html: 'Tipo selecionado.' };
    $box.attr('class', cfg.cls).html(cfg.html);
  }

  $(document).off('change', '#tipoFuncionario')
    .on('change', '#tipoFuncionario', atualizarBoxTipoInfo);

  atualizarBoxTipoInfo();

  // ======== Botão limpar ========
  $(document).off('click', '#btnLimpar')
    .on('click', '#btnLimpar', function () {
      resetFuncionarioForm();
    });

  // ======== CEP (se você já tem preencherEnderecoViaCep no main.js) ========
  $(document).off('blur', '#cep')
    .on('blur', '#cep', function () {
      const cep = $(this).val();
      if (typeof preencherEnderecoViaCep === 'function') {
        preencherEnderecoViaCep(cep);
      }
    });

  // ======== Submit (create/update) ========
  $(document).off('submit', '#formFuncionario')
    .on('submit', '#formFuncionario', function (e) {
      e.preventDefault();

      let ok = true;

      $(this).find('[required]').each(function () {
        if (!$(this).val() || !$(this).val().trim()) {
          $(this).addClass('is-invalid').removeClass('is-valid');
          ok = false;
        } else {
          $(this).removeClass('is-invalid').addClass('is-valid');
        }
      });

      const cpf = $('#cpfPf').val();
      if (cpf && typeof validarCPF === 'function' && !validarCPF(cpf)) {
        $('#cpfPf').addClass('is-invalid').removeClass('is-valid');
        ok = false;
      }

      // Data de nascimento: validar formato dd/mm/aaaa
      const dataNasc = $('#dataNascimentoPf').val().trim();
      if (dataNasc && !/^\d{2}\/\d{2}\/\d{4}$/.test(dataNasc)) {
        $('#dataNascimentoPf').addClass('is-invalid').removeClass('is-valid');
        ok = false;
      }

      if (!ok) {
        if (typeof mostrarAlerta === 'function') {
          mostrarAlerta('Revise os campos obrigatórios.', 'warning', '#alerta', 4000);
        } else {
          $('#alerta').removeClass('d-none').addClass('alert-warning').text('Revise os campos obrigatórios.');
        }
        return;
      }

      const idPessoaVal = $('#idPessoa').val();
      const payload = {
        acao: (idPessoaVal ? 'update' : 'create'),
        idpessoa: idPessoaVal ? parseInt(idPessoaVal) : null,
        nomepessoa: $('#nomePessoa').val().trim(),
        usuariopessoa: $('#usuarioPessoa').val().trim(),
        senhapessoa: $('#senhaPessoa').val(),
        nivelpessoa: $('#nivelPessoa').val(),
        situacaopessoa: $('#situacaoPessoa').val() === 'true',
        emailpessoa: $('#emailPessoa').val().trim(),
        telefonepessoa: $('#telefonePessoa').val().trim(),
        cep: $('#cep').val().trim(),
        numero: parseInt($('#numero').val(), 10) || 0,
        complemento: $('#complemento').val().trim(),
        cpfpf: $('#cpfPf').val().trim(),
        datanascimentopf: converterDataParaISO($('#dataNascimentoPf').val().trim()),
        matriculafuncionario: $('#matricula').val().trim(),
        tipofuncionario: $('#tipoFuncionario').val(),
        cargofuncionario: $('#cargo').val().trim(),
        datainiciofuncionario: converterDataParaISO($('#dataInicio').val().trim()),
        datafimfuncionario: converterDataParaISO($('#dataFim').val().trim())
      };

      $.ajax({
        url: (window.__ctxPath || '') + '/ControllerFuncionario',
        method: 'POST',
        contentType: 'application/json; charset=utf-8',
        dataType: 'json',
        data: JSON.stringify(payload),
        success: function (res) {
          const msg = (res && res.msg) ? res.msg : 'Funcionário salvo com sucesso!';
          if (res && res.ok) {
            sessionStorage.setItem('mensagemAlerta', msg);
            sessionStorage.setItem('tipoAlerta', 'success');
            window.location.href = (window.__ctxPath || '') + '/view/admin/Colaboradores.jsp';
          } else {
            if (typeof mostrarAlerta === 'function') {
              mostrarAlerta(msg, 'danger', '#alerta', 5000);
            } else {
              $('#alerta').removeClass('d-none alert-warning alert-success')
                .addClass('alert-danger').text(msg);
            }
          }
        },
        error: function () {
          if (typeof mostrarAlerta === 'function') {
            mostrarAlerta('Erro ao salvar funcionário!', 'danger', '#alerta', 5000);
          } else {
            $('#alerta').removeClass('d-none alert-warning alert-success')
              .addClass('alert-danger').text('Erro ao salvar funcionário!');
          }
        }
      });
    });
}
/***Configuração geral****/
$(function () {
    initValidacaoEmailAutomatica();
});

/* Atualiza referência de ctxPath legada para o CTX global */
$(function () {
    if (typeof window.__ctxPath === 'undefined') {
        window.__ctxPath = window.CTX || '';
    }
});

/******************************************************************************************************/
/* $(document).ready() — blocos movidos dos JSPs com guardas de página                               */
/******************************************************************************************************/

/* ========== login.jsp ========== */
$(document).ready(function () {
    if (!$('#formLogin').length) return;

    $('#btnToggleSenha').on('click', function () {
        var input = $('#senha');
        var icon  = $('#iconeSenha');
        if (input.attr('type') === 'password') {
            input.attr('type', 'text');
            icon.removeClass('fa-eye').addClass('fa-eye-slash');
        } else {
            input.attr('type', 'password');
            icon.removeClass('fa-eye-slash').addClass('fa-eye');
        }
    });

    $('#formLogin').on('submit', function (e) {
        e.preventDefault();
        var usuario = $('#usuario').val().trim();
        var senha   = $('#senha').val();
        if (!usuario || !senha) { exibirAlertaLogin('Preencha usuário e senha.', 'danger'); return; }
        var btn = $('#btnLogin');
        btn.prop('disabled', true).html('<i class="fas fa-spinner fa-spin me-1"></i>Entrando...');
        $.ajax({
            url: $('#formLogin').data('action') || 'LoginServlet',
            method: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({ usuario: usuario, senha: senha }),
            success: function (res) {
                if (res.ok) {
                    window.location.href = window.LOGIN_REDIRECT || '/';
                } else {
                    exibirAlertaLogin(res.msg, 'danger');
                    btn.prop('disabled', false).html('<i class="fas fa-sign-in-alt me-1"></i>Entrar');
                }
            },
            error: function () {
                exibirAlertaLogin('Erro de conexão. Tente novamente.', 'danger');
                btn.prop('disabled', false).html('<i class="fas fa-sign-in-alt me-1"></i>Entrar');
            }
        });
    });

    function exibirAlertaLogin(msg, tipo) {
        var alerta = $('#alertLogin');
        alerta.removeClass().addClass('alert alert-' + tipo).html(msg).fadeIn(200);
        setTimeout(function () { alerta.fadeOut(400, function () { $(this).addClass('d-none'); }); }, 5000);
    }
});

/* ========== index.jsp (dashboard raiz) ========== */
$(document).ready(function () {
    if (!$('#graficoResumo').length) return;
    var ctx = document.getElementById('graficoResumo').getContext('2d');
    new Chart(ctx, {
        type: 'pie',
        data: {
            labels: ['Pagas', 'Pendentes'],
            datasets: [{ data: [70, 30], backgroundColor: ['#28a745', '#ffc107'], borderColor: ['#fff', '#fff'] }]
        },
        options: { responsive: true, maintainAspectRatio: false }
    });
});

/* ========== AtualizarParceiro.jsp ========== */
$(document).ready(function () {
    if (!$('#cnpjPessoaCnpj').length) return;

    $('#telefonepessoa').mask('(00) 00000-0000');
    $('#cnpjPessoaCnpj').mask('00.000.000/0000-00');
    $('#inscricaoEstadualPessoaCnpj').mask('000.000.000.000');
    $('#cep').mask('00000-000');
});

/** Limpa os campos de endereço do formulário quando o CEP não é encontrado. Usado em: AtualizarParceiro.jsp */
function limpa_formulario_cep_atualizarParceiro() {
    $('#endereco').val('');
    $('#bairro').val('');
    $('#cidade').val('');
    $('#estado').val('');
}

/** Callback JSONP do ViaCEP; preenche os campos de endereço ou exibe alerta se CEP inválido. Usado em: AtualizarParceiro.jsp */
function meu_callback(conteudo) {
    if (!('erro' in conteudo)) {
        $('#endereco').val(conteudo.logradouro);
        $('#bairro').val(conteudo.bairro);
        $('#cidade').val(conteudo.localidade);
        $('#estado').val(conteudo.uf);
    } else {
        limpa_formulario_cep_atualizarParceiro();
        alert('CEP não encontrado.');
    }
}

/** Valida o CEP digitado e injeta script JSONP do ViaCEP para preenchimento automático do endereço. Chamado via onblur no campo CEP. Usado em: AtualizarParceiro.jsp */
function pesquisacep(valor) {
    var cep = valor.replace(/\D/g, '');
    if (cep !== '') {
        if (/^[0-9]{8}$/.test(cep)) {
            $('#endereco').val('...');
            $('#bairro').val('...');
            $('#cidade').val('...');
            $('#estado').val('...');
            var script = document.createElement('script');
            script.src = 'https://viacep.com.br/ws/' + cep + '/json/?callback=meu_callback';
            document.body.appendChild(script);
        } else {
            limpa_formulario_cep_atualizarParceiro();
            alert('Formato de CEP inválido.');
        }
    } else {
        limpa_formulario_cep_atualizarParceiro();
    }
}

/* ========== classificacao.jsp ========== */
$(document).ready(function () {
    if (!$('#tabelaClassificacao').length) return;
    CarregarClassificacao();
    setInterval(CarregarClassificacao, 20000);
});

/* ========== despesascustos.jsp ========== */
$(document).ready(function () {
    if (!$('#tabelaDespesasCustos').length) return;

    $('#valor').mask('000.000.000,00', { reverse: true });

    $('#modalAtualizacao').on('shown.bs.modal', function () {
        let campo = $('#atualizacaoValor');
        campo.unmask();
        campo.mask('000.000.000,00', { reverse: true });
    });

    CarregarDadosDespesasCustos();
    setInterval(CarregarDadosDespesasCustos, 10000);

    $('#formCadastro').on('submit', function (e) {
        e.preventDefault();
        let despesa = $('#despesascustos').val().trim();
        let unidade = $('#unidade').val().trim();
        let valor   = $('#valor').val().trim();
        let tipo    = $('#tipo').val().trim();
        if (!despesa || !unidade || !valor || !tipo) {
            mostrarAlerta('Todos os campos são obrigatórios.', 'danger', '#alertModalCadastro');
            return;
        }
        if (despesa.length > 50 || unidade.length > 2 || tipo.length > 20) {
            mostrarAlerta('Limite de caracteres excedido.', 'warning', '#alertModalCadastro', 4000);
            return;
        }
        if (!/^\d{1,3}(\.\d{3})*,\d{2}$/.test(valor)) {
            mostrarAlerta('Formato inválido de valor.', 'warning', '#alertModalCadastro', 4000);
            return;
        }
        salvarDespesasCustos(despesa, unidade, valor, tipo);
    });

    $('#formAtualizacao').on('submit', function (e) {
        e.preventDefault();
        let id      = $('#atualizacaoId').val();
        let despesa = $('#atualizacaoDespesa').val().trim();
        let unidade = $('#atualizacaoUnidade').val().trim();
        let valor   = $('#atualizacaoValor').val().trim();
        let tipo    = $('#atualizacaoTipo').val().trim();
        if (!despesa || !unidade || !valor || !tipo) {
            ExibirAlerta('Erro', 'Todos os campos são obrigatórios.');
            return;
        }
        atualizarDespesasCustos(id, despesa, unidade, valor, tipo);
    });

    $('#modalCadastro').on('show.bs.modal hidden.bs.modal', function () {
        $('#formCadastro')[0].reset();
        $('#alertModalCadastro').removeClass().addClass('alert d-none').empty();
    });
});

/* ========== alimento.jsp ========== */
$(document).ready(function () {
    if (!$('#tabelaAlimento').length) return;

    const msg  = sessionStorage.getItem('mensagemAlerta');
    const tipo = sessionStorage.getItem('tipoAlerta');
    if (msg && window.location.pathname.includes('alimento.jsp')) {
        mostrarAlerta(msg, tipo || 'info', '#alerta', 4000);
        sessionStorage.removeItem('mensagemAlerta');
        sessionStorage.removeItem('tipoAlerta');
    }

    CarregarAlimento();
    setInterval(CarregarAlimento, 10000);

    $('#modalFormulario').on('show.bs.modal', function () {
        CarregarClassificacaoModal();
    });

    $('#formAlimento').on('submit', function (event) {
        event.preventDefault();
        if (!this.checkValidity()) {
            event.stopPropagation();
            this.classList.add('was-validated');
            return;
        }
        salvarAlimento();
    });
});

/* ========== parceiro.jsp ========== */
$(document).ready(function () {
    if (!$('#tabelaParceiros').length) return;

    const msg  = sessionStorage.getItem('mensagemAlerta');
    const tipo = sessionStorage.getItem('tipoAlerta');
    if (msg && window.location.pathname.includes('parceiro.jsp')) {
        mostrarAlerta(msg, tipo || 'info', '#alerta', 4000);
        sessionStorage.removeItem('mensagemAlerta');
        sessionStorage.removeItem('tipoAlerta');
    }

    CarregarParceiros();
    carregarResumoParceiros();
    setInterval(CarregarParceiros, 20000);
});

/* ========== AtualizacaoAlimento.jsp ========== */
$(document).ready(function () {
    if (!$('#AtualizarCarregamentoClassificacao').length) return;

    const alimentoJson = sessionStorage.getItem('alimentoParaAtualizar');
    if (alimentoJson) {
        const alimento  = JSON.parse(alimentoJson);
        const idproduto = alimento.idproduto;
        $('#idproduto').val(idproduto);
        $('#alimento').val(alimento.nomeproduto);
        $('#variedade').val(alimento.variedadealimento);
        setTimeout(() => CarregarClassificacaoAtualizarAlimento(idproduto), 200);
        CarregarClassificacaoModalAtualizar(idproduto);
    }

    $('#modalClassificacao').on('show.bs.modal', function () {
        $('#valorAlimento').text($('#alimento').val().trim());
        $('#valorVariedade').text($('#variedade').val().trim());
    });
});

/* ========== CadastroParceiro.jsp ========== */
$(document).ready(function () {
    if (!$('#formCadastro').length || !$('#cnpj').length) return;

    $('#cep').mask('00000-000');
    $('#cnpj').mask('00.000.000/0001-00');

    $('#telefone').mask('(00) 00000-0000').on('blur', function () {
        $(this).mask($(this).val().length === 14 ? '(00) 0000-0000' : '(00) 00000-0000');
    });

    const urlParams = new URLSearchParams(window.location.search);
    const id = urlParams.get('id');

    if (id) {
        $('#tituloFormulario').html('<i class="fas fa-user-edit"></i> Atualizar Parceiro');
        $('#btnSalvar').html('<i class="fas fa-save"></i> Atualizar');
        carregarDadosParceiro(id);
        $('#cnpj').prop('readonly', true).addClass('disabled-field');
    } else {
        $('#tituloFormulario').html('<i class="fas fa-user-plus"></i> Cadastrar Parceiro');
        $('#btnSalvar').html('<i class="fas fa-save"></i> Salvar Cadastro');
    }

    $('#nivel').on('change', function () {
        if ($(this).val() !== 'parceiro') {
            $('#usuario, #senha').prop('disabled', true).addClass('disabled-field').fadeTo(200, 0.6).val('');
        } else {
            $('#usuario, #senha').prop('disabled', false).removeClass('disabled-field').fadeTo(200, 1);
        }
    });

    $('#cep').blur(function () {
        let cep = $(this).val().replace(/\D/g, '');
        if (cep.length !== 8) return;
        $.getJSON('https://viacep.com.br/ws/' + cep + '/json/', function (data) {
            if (data.erro) { alert('CEP não encontrado.'); return; }
            $('#logradouro').val(data.logradouro);
            $('#bairro').val(data.bairro);
            $('#cidade').val(data.localidade);
            $('#uf').val(data.uf);
        });
    });

    function validarCNPJ(cnpj) {
        cnpj = cnpj.replace(/[^\d]+/g, '');
        if (cnpj.length !== 14 || /^(\d)\1{13}$/.test(cnpj)) return false;
        let t = cnpj.length - 2, n = cnpj.substring(0, t), d = cnpj.substring(t), s = 0, p = t - 7;
        for (let i = t; i >= 1; i--) { s += n.charAt(t - i) * p--; if (p < 2) p = 9; }
        let r = s % 11 < 2 ? 0 : 11 - (s % 11);
        if (r != d.charAt(0)) return false;
        t += 1; n = cnpj.substring(0, t); s = 0; p = t - 7;
        for (let i = t; i >= 1; i--) { s += n.charAt(t - i) * p--; if (p < 2) p = 9; }
        r = s % 11 < 2 ? 0 : 11 - (s % 11);
        return r == d.charAt(1);
    }

    $('#cnpj').on('input', function () {
        $('#icone-ok, #icone-erro').addClass('d-none');
        $('#spinner-cnpj').addClass('d-none');
        $('#cnpj').removeClass('is-valid is-invalid');
        $('#btnSalvar').prop('disabled', false);
    });

    $('#cnpj').on('blur', function () {
        if ($('#idPessoa').val()) return;
        const cnpj = $(this).val().replace(/\D/g, '');
        $('#icone-ok, #icone-erro').addClass('d-none');
        $('#spinner-cnpj').removeClass('d-none');
        if (cnpj.length !== 14 || !validarCNPJ(cnpj)) {
            $('#spinner-cnpj').addClass('d-none');
            $('#icone-erro').removeClass('d-none');
            $('#cnpj').addClass('is-invalid');
            $('#btnSalvar').prop('disabled', true);
            return;
        }
        $.ajax({
            url: CTX + '/ControllerParceiro?cnpj=' + cnpj,
            method: 'GET', dataType: 'json',
            success: function (res) {
                $('#spinner-cnpj').addClass('d-none');
                if (res.existe === true || res.existe === 'true') {
                    $('#icone-erro').removeClass('d-none');
                    $('#cnpj').addClass('is-invalid');
                    $('#btnSalvar').prop('disabled', true);
                } else {
                    $('#icone-ok').removeClass('d-none');
                    $('#cnpj').removeClass('is-invalid').addClass('is-valid');
                    $('#btnSalvar').prop('disabled', false);
                }
            },
            error: function () {
                $('#spinner-cnpj').addClass('d-none');
                $('#icone-erro').removeClass('d-none');
                $('#btnSalvar').prop('disabled', true);
                alert('Erro ao verificar CNPJ.');
            }
        });
    });

    $('#formCadastro').on('submit', function (e) {
        e.preventDefault();
        salvarParceiroCreateOuUpdate();
    });
});

/* ========== Colaboradores.jsp ========== */
$(document).ready(function () {
    if (!$('#tabelaFuncionarios').length) return;

    const msg  = sessionStorage.getItem('mensagemAlerta');
    const tipo = sessionStorage.getItem('tipoAlerta');
    if (msg && window.location.pathname.includes('Colaboradores')) {
        mostrarAlerta(msg, tipo || 'info', '#alerta', 4000);
        sessionStorage.removeItem('mensagemAlerta');
        sessionStorage.removeItem('tipoAlerta');
    }

    CarregarFuncionarios();
    setInterval(CarregarFuncionarios, 10000);

    $('#filtroTipo, #filtroSituacao').on('change', function () {
        if (tabelaFuncionarios) tabelaFuncionarios.draw();
    });

    $('#formEditar').on('submit', function (event) {
        event.preventDefault();
        if (!this.checkValidity()) { event.stopPropagation(); this.classList.add('was-validated'); return; }
        salvarEdicao();
    });

    $('#btnConfirmarSituacao').on('click', function () {
        const id      = parseInt($('#situacaoId').val());
        const novaSit = $('#situacaoNova').val() === 'true';
        $.ajax({
            url: CTX + '/ControllerFuncionario',
            method: 'POST', contentType: 'application/json',
            data: JSON.stringify({ acao: 'delete', id: id, situacao: novaSit }),
            success: function (res) {
                $('#modalSituacao').modal('hide');
                mostrarAlerta(res.msg, res.ok ? 'success' : 'danger', '#alerta', 4000);
                if (res.ok) CarregarFuncionarios();
            },
            error: function () {
                $('#modalSituacao').modal('hide');
                mostrarAlerta('Erro de conexão.', 'danger', '#alerta', 5000);
            }
        });
    });
});

let tabelaFuncionarios = null;
let dadosFuncionarios  = [];

/**
 * Carrega a lista de funcionários via GET e inicializa/atualiza o DataTable #tabelaFuncionarios.
 * Inclui filtros personalizados por tipo e situação.
 * Usado em: Colaboradores.jsp
 */
function CarregarFuncionarios() {
    $.ajax({
        url: CTX + '/ControllerFuncionario',
        method: 'GET', dataType: 'json',
        success: function (lista) {
            dadosFuncionarios = Array.isArray(lista) ? lista : [];
            if (tabelaFuncionarios) {
                tabelaFuncionarios.clear().rows.add(dadosFuncionarios).draw();
            } else {
                tabelaFuncionarios = $('#tabelaFuncionarios').DataTable({
                    data: dadosFuncionarios,
                    columns: [
                        { data: 'idPessoa' },
                        { data: 'nomePessoa' },
                        { data: 'cpfPf',  defaultContent: '-' },
                        { data: 'cargo',  defaultContent: '-' },
                        { data: 'tipoFuncionario', defaultContent: '-',
                          render: function (v) { return labelTipo(v); } },
                        { data: 'emailPessoa', defaultContent: '-' },
                        { data: 'situacaoPessoa', defaultContent: false,
                          render: function (v) {
                              return v ? '<span class="badge text-bg-success">Ativo</span>'
                                       : '<span class="badge text-bg-secondary">Inativo</span>';
                          }
                        },
                        { data: null, orderable: false, searchable: false,
                          render: function (_, __, row) { return botoesAcaoColaborador(row); }
                        }
                    ],
                    language: { url: 'https://cdn.datatables.net/plug-ins/1.11.5/i18n/pt-BR.json' },
                    order: [[1, 'asc']],
                    columnDefs: [{ targets: -1, className: 'text-center' }]
                });

                $.fn.dataTable.ext.search.push(function (settings, data) {
                    if (settings.nTable !== document.getElementById('tabelaFuncionarios')) return true;
                    const filtroTipo = $('#filtroTipo').val().toLowerCase();
                    const filtroSit  = $('#filtroSituacao').val().toLowerCase();
                    const tipoCell = (data[4] || '').toLowerCase();
                    const sitCell  = (data[6] || '').toLowerCase();
                    if (filtroTipo && !tipoCell.includes(filtroTipo)) return false;
                    if (filtroSit  && !sitCell.includes(filtroSit))   return false;
                    return true;
                });
            }
        },
        error: function () { mostrarAlerta('Erro ao carregar funcionários.', 'danger', '#alerta', 5000); }
    });
}

/** Converte o código interno do tipo de funcionário para rótulo legível na tabela. Usado em: Colaboradores.jsp */
function labelTipo(v) {
    const map = { CLT: 'CLT', DIARIA: 'Diária', EMPREITA: 'Empreita', PRODUCAO: 'Produção' };
    return map[v] || (v || '-');
}

/** Gera HTML dos botões Editar e Ativar/Desativar para cada linha da tabela. Usado em: Colaboradores.jsp */
function botoesAcaoColaborador(row) {
    const ativo = row.situacaoPessoa;
    const id    = row.idPessoa;
    const tipo  = row.tipoFuncionario || '';
    const nome  = (row.nomePessoa || '').replace(/'/g, "\\'");
    var btnEditar = '<a href="' + CTX + '/view/admin/ColaboCadastro.jsp?id=' + id + '"'
        + ' class="btn-acao btn-acao-editar" title="Editar"><i class="fas fa-pen-to-square"></i></a>';
    var btnSit = '<button class="btn-acao ' + (ativo ? 'btn-acao-desativar' : 'btn-acao-ativar') + '"'
        + ' title="' + (ativo ? 'Desativar' : 'Reativar') + '"'
        + ' onclick="abrirModalSituacao(' + id + ', \'' + nome + '\', ' + ativo + ')">'
        + '<i class="fas fa-' + (ativo ? 'ban' : 'circle-check') + '"></i></button>';
    var btnPerfil = tipo === 'CLT'
        ? ' <a href="' + CTX + '/view/admin/perfilCLT.jsp?id=' + id + '"'
          + ' class="btn-acao" style="background:#198754;color:#fff;" title="Perfil CLT">'
          + '<i class="fas fa-id-badge"></i></a>'
        : '';
    return btnEditar + ' ' + btnSit + btnPerfil;
}

/** Busca o funcionário no array local e preenche o modal #modalEditar para edição inline. Usado em: Colaboradores.jsp */
function abrirEditar(id) {
    const f = dadosFuncionarios.find(x => x.idPessoa === id);
    if (!f) return;
    $('#editIdPessoa').val(f.idPessoa);
    $('#editCpfPf').val(f.cpfPf || '');
    $('#editMatricula').val(f.matricula || '');
    $('#editSenhaPessoa').val(f.senhaPessoa || '');
    $('#editDataNasc').val(f.dataNascimentoPf || '');
    $('#editDataInicio').val(f.dataInicio || '');
    $('#editDataFim').val(f.dataFim || '');
    $('#editCep').val(f.Cep || '');
    $('#editNumero').val(f.numero || '');
    $('#editComplemento').val(f.complemento || '');
    $('#editNome').val(f.nomePessoa || '');
    $('#editEmail').val(f.emailPessoa || '');
    $('#editTelefone').val(f.telefonePessoa || '');
    $('#editCargo').val(f.cargo || '');
    $('#editNivel').val((f.nivelPessoa || '').toLowerCase());
    $('#editTipo').val(f.tipoFuncionario || '');
    $('#editSituacao').val(String(f.situacaoPessoa));
    $('#editUsuario').val(f.usuarioPessoa || '');
    $('#editSenhaNova').val('');
    $('#formEditar')[0].classList.remove('was-validated');
    $('#alertaModalEditar').html('');
    $('#modalEditar').modal('show');
}

/** Coleta os dados do modal #modalEditar e envia POST para atualizar o funcionário. Usado em: Colaboradores.jsp */
function salvarEdicao() {
    const novaSenha = $('#editSenhaNova').val().trim();
    const payload = {
        acao: 'update',
        idpessoa: parseInt($('#editIdPessoa').val()),
        nomepessoa: $('#editNome').val().trim(),
        emailpessoa: $('#editEmail').val().trim(),
        telefonepessoa: $('#editTelefone').val().trim(),
        usuariopessoa: $('#editUsuario').val().trim(),
        senhapessoa: novaSenha || $('#editSenhaPessoa').val(),
        nivelpessoa: $('#editNivel').val(),
        situacaopessoa: $('#editSituacao').val() === 'true',
        cpfpf: $('#editCpfPf').val(),
        matriculafuncionario: $('#editMatricula').val(),
        cargofuncionario: $('#editCargo').val().trim(),
        tipofuncionario: $('#editTipo').val(),
        datanascimentopf: $('#editDataNasc').val() || null,
        datainiciofuncionario: $('#editDataInicio').val() || null,
        datafimfuncionario: $('#editDataFim').val() || null,
        cep: $('#editCep').val(),
        numero: parseInt($('#editNumero').val()) || 0,
        complemento: $('#editComplemento').val()
    };
    $.ajax({
        url: CTX + '/ControllerFuncionario',
        method: 'POST', contentType: 'application/json',
        data: JSON.stringify(payload),
        success: function (res) {
            if (res.ok) {
                $('#modalEditar').modal('hide');
                mostrarAlerta(res.msg, 'success', '#alerta', 4000);
                CarregarFuncionarios();
            } else {
                mostrarAlerta(res.msg, 'danger', '#alertaModalEditar', 6000);
            }
        },
        error: function () { mostrarAlerta('Erro de conexão ao salvar.', 'danger', '#alertaModalEditar', 5000); }
    });
}

/** Abre o modal #modalSituacao para confirmar ativação ou desativação do funcionário. Usado em: Colaboradores.jsp */
function abrirModalSituacao(id, nome, situacaoAtual) {
    $('#situacaoId').val(id);
    const novaSit = !situacaoAtual;
    $('#situacaoNova').val(novaSit);
    const acao = novaSit ? 'reativar' : 'desativar';
    $('#textoConfirmacaoSituacao').html('Deseja <strong>' + acao + '</strong> o funcionário <strong>' + nome + '</strong>?');
    $('#modalSituacao').modal('show');
}

/* ========== ColaboCadastro.jsp ========== */
$(document).ready(function () {
    if (!$('#formFuncionario').length) return;
    initFuncionarioCadastro();
});

/******************************************************************************************************/
/* MÓDULO CLT — perfilCLT.jsp                                                                         */
/******************************************************************************************************/

$(document).ready(function () {
    if (!$('#painelFechamento').length) return;
    initPerfilCLT();
});

function initPerfilCLT() {
    var idFuncionario = new URLSearchParams(window.location.search).get('id');
    if (!idFuncionario) {
        mostrarAlerta('ID do funcionário não informado na URL.', 'danger', '#alerta');
        return;
    }

    var periodoInput = $('#inputPeriodo');
    var hoje = new Date();
    periodoInput.val(hoje.getFullYear() + '-' + String(hoje.getMonth() + 1).padStart(2, '0'));

    carregarPerfilCLT(idFuncionario, periodoInput.val());

    $('#btnCarregarPeriodo').on('click', function () {
        carregarPerfilCLT(idFuncionario, periodoInput.val());
    });

    $('#btnCalcular').on('click', function () {
        calcularFechamentoCLT(idFuncionario, periodoInput.val());
    });

    $('#btnSalvarPonto').on('click', function () {
        salvarPontoCLT(idFuncionario);
    });

    // "Lançar Ponto" sempre abre em modo novo (limpa o modo edição)
    $('#btnNovoPonto').on('click', function () {
        resetModalPonto();
    });

    $('#btnSalvarFalta').on('click', function () {
        salvarFaltaCLT(idFuncionario);
    });

    $('#btnSalvarVale').on('click', function () {
        salvarValeCLT(idFuncionario);
    });

    $('#btnSalvarSalario').on('click', function () {
        salvarSalarioCLT(idFuncionario);
    });

    $('#btnDesligarVinculo').on('click', function () {
        desligarVinculoCLT(idFuncionario);
    });

    $('#btnReadmitirVinculo').on('click', function () {
        readmitirVinculoCLT(idFuncionario);
    });

    // pré-preencher data dos modais com hoje
    var hoje_iso = hoje.toISOString().substring(0, 10);
    $('#pontoData, #faltaData, #valeData').val(hoje_iso);
}

function carregarPerfilCLT(idFuncionario, periodo) {
    $.ajax({
        url: (window.__ctxPath || '') + '/ControllerCLT?acao=perfil&id=' + idFuncionario + '&periodo=' + periodo,
        method: 'GET',
        dataType: 'json',
        success: function (data) {
            renderHeaderCLT(data);
            renderCardsCLT(data);
            renderTabelaPonto(data.pontos || []);
            renderTabelaFaltas(data.faltas || []);
            renderTabelaVales(data.vales || []);
            if (data.fechamento) renderFechamento(data.fechamento);
        },
        error: function () {
            mostrarAlerta('Erro ao carregar perfil do funcionário.', 'danger', '#alerta', 5000);
        }
    });
}

function renderHeaderCLT(data) {
    var f = data.funcionario;
    if (!f) return;
    var v = data.vinculo;   // termos do período de trabalho (fonte da verdade)

    // Termos vêm do vínculo, com fallback para funcionarioclt (transição)
    var salario   = v ? v.salarioMensal : f.salarioMensal;
    var jornada   = v ? v.cargaHorariaDiaria : f.cargaHorariaDiaria;
    var cargo     = (v ? v.cargo : f.cargo) || '';
    var atividade = v ? (v.tipoAtividadeRural || 'LAVOURA') : (f.tipoAtividadeRuralStr || 'LAVOURA');
    var status    = (v ? v.status : f.statusEmpregoStr) || 'ATIVO';
    var vheCad    = v ? v.valorHoraExtra : f.valorHoraExtra;

    $('#nomeFunc').text(f.nomePessoa || '');
    $('#cargoMatricula').text(cargo + ' · Matrícula: ' + (f.matricula || ''));
    $('#infoSalario').text(formatBRL(salario));

    var vhe = (data.valorHoraExtraEfetivo != null && data.valorHoraExtraEfetivo > 0)
        ? data.valorHoraExtraEfetivo : vheCad;
    var autoTag = (!vheCad || vheCad <= 0) && vhe > 0 ? ' (auto)' : '';
    $('#infoHoraExtra').text(formatBRL(vhe) + '/h' + autoTag);
    $('#infoJornada').text((jornada || 8) + 'h/dia');
    $('#infoAtividade').text(atividade === 'PECUARIA' ? 'Pecuária' : 'Lavoura');

    // Período do vínculo + contagem
    var totalVinc = (data.vinculos || []).length;
    var periodoTxt = '—';
    if (v) {
        var ini = v.dataAdmissao ? formatDataBR(v.dataAdmissao) : '?';
        var fim = v.dataDesligamento ? formatDataBR(v.dataDesligamento) : 'atual';
        periodoTxt = ini + ' – ' + fim + (totalVinc > 1 ? ' (' + totalVinc + ' vínculos)' : '');
    }
    $('#infoVinculoPeriodo').text(periodoTxt);

    // Estado ATUAL da pessoa: existe algum vínculo sem desligamento?
    var ativo = (data.vinculos || []).filter(function (x) { return !x.dataDesligamento; })[0];
    var stAtual = ativo ? (ativo.status || 'ATIVO').toUpperCase() : 'DESLIGADO';
    $('#badgeStatus').text(stAtual)
         .attr('class', 'badge fs-6 px-3 py-2 ' +
            (stAtual === 'ATIVO' ? 'bg-success' : stAtual === 'AFASTADO' ? 'bg-warning text-dark' : 'bg-danger'));
    $('#headerFuncionario').removeClass('d-none');

    // Botões Desligar / Readmitir conforme o estado atual da pessoa
    var temAtivo = !!ativo;
    $('#btnDesligarVinculo').toggleClass('d-none', !temAtivo);
    $('#btnReadmitirVinculo').toggleClass('d-none', temAtivo);

    // pré-preencher modal de salário com os termos do vínculo
    $('#editSalario').val(salario || 0);
    $('#editHoraExtra').val(vheCad || 0);
    $('#editCargaHoraria').val(jornada || 8);
    $('#editTipoAtividade').val(atividade);

    window.__idCLT = f.idPessoa;
    window.__idVinculo = v ? v.idVinculo : null;
    window.__vinculoAtivo = vinculoAtivo;
}

function renderCardsCLT(data) {
    $('#cardDiasTrabalhados').text(data.diasComPonto || 0);
    $('#cardFaltasInjust').text(data.faltasInjustificadas || 0);
    $('#cardHorasExtras').text(data.horasExtrasFormatado || '00:00');
    $('#cardTotalVales').text(formatBRL(data.totalVales || 0));
    $('#cardsResumo').css('display', '');
    $('#cardsResumo').removeClass('d-none');
    var diasUteis = diasUteisDoMes($('#inputPeriodo').val());
    $('#infoDiasUteis').text(diasUteis + ' dias úteis no mês');
}

function renderTabelaPonto(pontos) {
    window.__pontosCLT = pontos || [];   // cache para a edição
    var tbody = $('#bodyPonto').empty();
    if (!pontos.length) {
        tbody.append('<tr><td colspan="9" class="text-center text-muted py-3">Nenhum ponto lançado.</td></tr>');
        return;
    }
    pontos.forEach(function (p) {
        var extra = p.extraMinutos > 0
            ? '<span class="badge bg-primary">' + formatMin(p.extraMinutos) + '</span>'
            : '-';
        var noturno = p.minutosNoturnos > 0
            ? '<span class="badge bg-dark"><i class="fas fa-moon me-1"></i>' + formatMin(p.minutosNoturnos) + '</span>'
            : '-';
        var acoes = '<div class="text-nowrap text-end">'
            + '<button class="btn btn-outline-primary btn-sm py-0 me-1" title="Editar" '
            + 'onclick="editarPontoCLT(' + p.idPonto + ')"><i class="fas fa-pen"></i></button>'
            + '<button class="btn btn-outline-danger btn-sm py-0" title="Excluir" '
            + 'onclick="excluirPontoCLT(' + p.idPonto + ')"><i class="fas fa-trash"></i></button>'
            + '</div>';
        tbody.append('<tr>'
            + '<td>' + formatDataBR(p.dataRegistro) + '</td>'
            + '<td>' + (p.entrada1 || '-') + '</td>'
            + '<td>' + (p.saida1   || '-') + '</td>'
            + '<td>' + (p.entrada2 || '-') + '</td>'
            + '<td>' + (p.saida2   || '-') + '</td>'
            + '<td><strong>' + formatMin(p.totalMinutos) + '</strong></td>'
            + '<td>' + extra + '</td>'
            + '<td>' + noturno + '</td>'
            + '<td>' + acoes + '</td>'
            + '</tr>');
    });
}

function renderTabelaFaltas(faltas) {
    var tbody = $('#bodyFaltas').empty();
    if (!faltas.length) {
        tbody.append('<tr><td colspan="4" class="text-center text-muted py-3">Nenhuma falta no período.</td></tr>');
        return;
    }
    faltas.forEach(function (f) {
        var tipo = f.justificada
            ? '<span class="badge bg-secondary">Justificada</span>'
            : '<span class="badge bg-danger">Injustificada</span>';
        tbody.append('<tr>'
            + '<td>' + formatDataBR(f.dataFalta) + '</td>'
            + '<td>' + tipo + '</td>'
            + '<td>' + (f.motivo || '-') + '</td>'
            + '<td><button class="btn btn-outline-danger btn-sm py-0" '
            + 'onclick="excluirFaltaCLT(' + f.idFalta + ')"><i class="fas fa-trash"></i></button></td>'
            + '</tr>');
    });
}

function renderTabelaVales(vales) {
    var tbody = $('#bodyVales').empty();
    if (!vales.length) {
        tbody.append('<tr><td colspan="5" class="text-center text-muted py-3">Nenhum vale no período.</td></tr>');
        return;
    }
    var tipoLabel = { ALIMENTACAO: 'Alimentação', TRANSPORTE: 'Transporte',
                      ADIANTAMENTO: 'Adiantamento', OUTROS: 'Outros' };
    vales.forEach(function (v) {
        var status = v.statusValeStr === 'DESCONTADO'
            ? '<span class="badge bg-secondary">Descontado</span>'
            : '<span class="badge bg-warning text-dark">Pendente</span>';
        var btnDesc = v.statusValeStr === 'PENDENTE'
            ? '<button class="btn btn-outline-success btn-sm py-0 me-1" title="Marcar descontado" '
              + 'onclick="marcarValeDescontadoCLT(' + v.idVale + ')"><i class="fas fa-check"></i></button>'
            : '';
        tbody.append('<tr>'
            + '<td>' + formatDataBR(v.dataVale) + '</td>'
            + '<td>' + (tipoLabel[v.tipoValeStr] || v.tipoValeStr) + '</td>'
            + '<td class="text-end fw-bold">' + formatBRL(v.valor) + '</td>'
            + '<td>' + status + '</td>'
            + '<td class="text-nowrap">' + btnDesc
            + '<button class="btn btn-outline-danger btn-sm py-0" '
            + 'onclick="excluirValeCLT(' + v.idVale + ')"><i class="fas fa-trash"></i></button></td>'
            + '</tr>');
    });
}

function renderFechamento(fc) {
    if (!fc) return;
    $('#painelFechamento').html(
        '<div class="table-responsive">'
        + '<table class="table table-sm mb-0">'
        + '<tr><td>Salário Base</td><td class="text-end fw-bold text-success">' + formatBRL(fc.salarioBase) + '</td></tr>'
        + '<tr><td>Horas Extras (' + (fc.totalMinutosExtras ? formatMin(fc.totalMinutosExtras) : '00:00') + ')</td>'
        + '<td class="text-end text-primary">+ ' + formatBRL(fc.valorHorasExtras) + '</td></tr>'
        + '<tr><td>Extras 100% — Domingo (' + (fc.minutosExtra100 ? formatMin(fc.minutosExtra100) : '00:00') + ')</td>'
        + '<td class="text-end text-primary">+ ' + formatBRL(fc.valorExtra100) + '</td></tr>'
        + '<tr><td>Adicional Noturno (' + (fc.minutosNoturnos ? formatMin(fc.minutosNoturnos) : '00:00') + ')</td>'
        + '<td class="text-end text-primary">+ ' + formatBRL(fc.valorAdicionalNoturno) + '</td></tr>'
        + '<tr><td>Desconto Faltas (' + fc.faltasInjustificadas + ' dia(s))</td>'
        + '<td class="text-end text-danger">- ' + formatBRL(fc.descontoFaltas) + '</td></tr>'
        + '<tr><td>Desconto DSR</td>'
        + '<td class="text-end text-danger">- ' + formatBRL(fc.descontoDsr) + '</td></tr>'
        + '<tr><td>Total Vales</td>'
        + '<td class="text-end text-warning">- ' + formatBRL(fc.totalVales) + '</td></tr>'
        + '<tr class="table-success"><td class="fw-bold">Salário Líquido</td>'
        + '<td class="text-end fw-bold fs-5">' + formatBRL(fc.salarioLiquido) + '</td></tr>'
        + '</table></div>'
        + '<small class="text-muted">Período: ' + fc.periodo + ' · '
        + fc.diasTrabalhados + ' dias trabalhados / ' + fc.diasUteis + ' úteis</small>'
    );
}

function salvarPontoCLT(idFuncionario) {
    var data = $('#pontoData').val();
    if (!data) { mostrarAlerta('Informe a data.', 'warning', '#alertaPonto', 3000); return; }

    var editId = window.__pontoEditId || null;
    var payload = {
        acao: editId ? 'editar_ponto' : 'salvar_ponto_manual',
        idfuncionario: parseInt(idFuncionario),
        dataregistro: data,
        entrada1: $('#pontoEntrada1').val() || null, saida1: $('#pontoSaida1').val() || null,
        entrada2: $('#pontoEntrada2').val() || null, saida2: $('#pontoSaida2').val() || null,
        observacao: $('#pontoObs').val()
    };
    if (editId) payload.idponto = editId;

    $.ajax({
        url: (window.__ctxPath || '') + '/ControllerPontoEletronico',
        method: 'POST', contentType: 'application/json; charset=utf-8',
        data: JSON.stringify(payload), dataType: 'json',
        success: function (r) {
            if (r.ok) {
                window.__pontoEditId = null;
                $('#modalPonto').modal('hide');
                carregarPerfilCLT(idFuncionario, $('#inputPeriodo').val());
            } else {
                mostrarAlerta(r.msg, 'danger', '#alertaPonto', 4000);
            }
        },
        error: function () { mostrarAlerta('Erro ao salvar ponto.', 'danger', '#alertaPonto', 4000); }
    });
}

/** Abre o modal preenchido para corrigir um registro de ponto existente. */
function editarPontoCLT(idPonto) {
    var p = (window.__pontosCLT || []).find(function (x) { return x.idPonto === idPonto; });
    if (!p) { mostrarAlerta('Registro não encontrado.', 'warning', '#alerta', 3000); return; }

    window.__pontoEditId = idPonto;
    $('#modalPontoTitulo').html('<i class="fas fa-pen me-2"></i>Editar Ponto');
    $('#pontoData').val(p.dataRegistro);
    $('#pontoEntrada1').val(p.entrada1 || '');
    $('#pontoSaida1').val(p.saida1 || '');
    $('#pontoEntrada2').val(p.entrada2 || '');
    $('#pontoSaida2').val(p.saida2 || '');
    $('#pontoObs').val(p.observacao || '');
    $('#alertaPonto').addClass('d-none');
    bootstrap.Modal.getOrCreateInstance(document.getElementById('modalPonto')).show();
}

/** Exclui um registro de ponto após confirmação. */
function excluirPontoCLT(idPonto) {
    if (!confirm('Excluir este registro de ponto?')) return;
    $.ajax({
        url: (window.__ctxPath || '') + '/ControllerPontoEletronico',
        method: 'POST', contentType: 'application/json; charset=utf-8',
        data: JSON.stringify({ acao: 'excluir_ponto', idponto: idPonto }), dataType: 'json',
        success: function (r) {
            if (r.ok) carregarPerfilCLT(window.__idCLT, $('#inputPeriodo').val());
            else mostrarAlerta(r.msg, 'danger', '#alerta', 4000);
        },
        error: function () { mostrarAlerta('Erro ao excluir ponto.', 'danger', '#alerta', 4000); }
    });
}

/** Restaura o modal de ponto para o modo "novo lançamento". */
function resetModalPonto() {
    window.__pontoEditId = null;
    $('#modalPontoTitulo').html('<i class="fas fa-fingerprint me-2"></i>Lançar Ponto');
    var hojeIso = new Date().toISOString().substring(0, 10);
    $('#pontoData').val(hojeIso);
    $('#pontoEntrada1, #pontoSaida1, #pontoEntrada2, #pontoSaida2, #pontoObs').val('');
    $('#alertaPonto').addClass('d-none');
}

function salvarFaltaCLT(idFuncionario) {
    var data = $('#faltaData').val();
    if (!data) { mostrarAlerta('Informe a data.', 'warning', '#alertaFalta', 3000); return; }
    $.ajax({
        url: (window.__ctxPath || '') + '/ControllerCLT',
        method: 'POST', contentType: 'application/json; charset=utf-8',
        data: JSON.stringify({
            acao: 'registrarFalta', idpessoa: parseInt(idFuncionario),
            datafalta: data, justificada: $('#faltaJustificada').val() === 'true',
            motivo: $('#faltaMotivo').val()
        }), dataType: 'json',
        success: function (r) {
            if (r.ok) {
                $('#modalFalta').modal('hide');
                carregarPerfilCLT(idFuncionario, $('#inputPeriodo').val());
            } else {
                mostrarAlerta(r.msg, 'danger', '#alertaFalta', 4000);
            }
        },
        error: function () { mostrarAlerta('Erro ao registrar falta.', 'danger', '#alertaFalta', 4000); }
    });
}

function excluirFaltaCLT(idFalta) {
    if (!confirm('Remover esta falta?')) return;
    $.ajax({
        url: (window.__ctxPath || '') + '/ControllerCLT',
        method: 'POST', contentType: 'application/json; charset=utf-8',
        data: JSON.stringify({ acao: 'excluirFalta', idfalta: idFalta }), dataType: 'json',
        success: function (r) {
            if (r.ok) carregarPerfilCLT(window.__idCLT, $('#inputPeriodo').val());
            else mostrarAlerta(r.msg, 'danger', '#alerta', 4000);
        }
    });
}

function salvarValeCLT(idFuncionario) {
    var valor = parseFloat($('#valeValor').val());
    var data  = $('#valeData').val();
    if (!valor || valor <= 0 || !data) {
        mostrarAlerta('Informe valor e data.', 'warning', '#alertaVale', 3000); return;
    }
    $.ajax({
        url: (window.__ctxPath || '') + '/ControllerCLT',
        method: 'POST', contentType: 'application/json; charset=utf-8',
        data: JSON.stringify({
            acao: 'registrarVale', idpessoa: parseInt(idFuncionario),
            valor: valor, datavale: data,
            tipovale: $('#valeTipo').val(), descricao: $('#valeDescricao').val()
        }), dataType: 'json',
        success: function (r) {
            if (r.ok) {
                $('#modalVale').modal('hide');
                carregarPerfilCLT(idFuncionario, $('#inputPeriodo').val());
            } else {
                mostrarAlerta(r.msg, 'danger', '#alertaVale', 4000);
            }
        },
        error: function () { mostrarAlerta('Erro ao registrar vale.', 'danger', '#alertaVale', 4000); }
    });
}

function excluirValeCLT(idVale) {
    if (!confirm('Excluir este vale?')) return;
    $.ajax({
        url: (window.__ctxPath || '') + '/ControllerCLT',
        method: 'POST', contentType: 'application/json; charset=utf-8',
        data: JSON.stringify({ acao: 'excluirVale', idvale: idVale }), dataType: 'json',
        success: function (r) {
            if (r.ok) carregarPerfilCLT(window.__idCLT, $('#inputPeriodo').val());
            else mostrarAlerta(r.msg, 'danger', '#alerta', 4000);
        }
    });
}

function marcarValeDescontadoCLT(idVale) {
    if (!confirm('Marcar este vale como descontado?')) return;
    $.ajax({
        url: (window.__ctxPath || '') + '/ControllerCLT',
        method: 'POST', contentType: 'application/json; charset=utf-8',
        data: JSON.stringify({ acao: 'marcarValeDescontado', idvale: idVale }), dataType: 'json',
        success: function (r) {
            if (r.ok) carregarPerfilCLT(window.__idCLT, $('#inputPeriodo').val());
            else mostrarAlerta(r.msg, 'danger', '#alerta', 4000);
        }
    });
}

function salvarSalarioCLT(idFuncionario) {
    var salario = parseFloat($('#editSalario').val());
    if (!salario || salario <= 0) {
        mostrarAlerta('Informe um salário válido.', 'warning', '#alertaSalario', 3000); return;
    }
    $.ajax({
        url: (window.__ctxPath || '') + '/ControllerCLT',
        method: 'POST', contentType: 'application/json; charset=utf-8',
        data: JSON.stringify({
            acao: 'atualizarSalario', idpessoa: parseInt(idFuncionario),
            idvinculo: window.__idVinculo || 0,
            salariomensal: salario,
            valorhoraextra: parseFloat($('#editHoraExtra').val()) || 0,
            cargahorariadiaria: parseInt($('#editCargaHoraria').val()) || 8,
            tipoatividaderural: $('#editTipoAtividade').val() || 'LAVOURA'
        }), dataType: 'json',
        success: function (r) {
            if (r.ok) {
                $('#modalSalario').modal('hide');
                carregarPerfilCLT(idFuncionario, $('#inputPeriodo').val());
                mostrarAlerta(r.msg, 'success', '#alerta', 3000);
            } else {
                mostrarAlerta(r.msg, 'danger', '#alertaSalario', 4000);
            }
        },
        error: function () { mostrarAlerta('Erro ao atualizar salário.', 'danger', '#alertaSalario', 4000); }
    });
}

function desligarVinculoCLT(idFuncionario) {
    var data = prompt('Data de desligamento (dd/mm/aaaa):', '');
    if (data === null) return;
    var iso = converterDataParaISO((data || '').trim());
    var motivo = prompt('Motivo do desligamento (opcional):', '') || '';
    $.ajax({
        url: (window.__ctxPath || '') + '/ControllerCLT',
        method: 'POST', contentType: 'application/json; charset=utf-8',
        data: JSON.stringify({ acao: 'desligarvinculo', idpessoa: parseInt(idFuncionario),
                               datafim: iso, motivo: motivo }),
        dataType: 'json',
        success: function (r) {
            if (r.ok) { mostrarAlerta(r.msg, 'success', '#alerta', 4000);
                        carregarPerfilCLT(idFuncionario, $('#inputPeriodo').val()); }
            else mostrarAlerta(r.msg, 'danger', '#alerta', 5000);
        },
        error: function () { mostrarAlerta('Erro ao desligar vínculo.', 'danger', '#alerta', 5000); }
    });
}

function readmitirVinculoCLT(idFuncionario) {
    var data = prompt('Data de admissão do novo vínculo (dd/mm/aaaa):', '');
    if (data === null) return;
    var iso = converterDataParaISO((data || '').trim());
    var cargo = prompt('Cargo no novo vínculo:', $('#cargoMatricula').text().split(' · ')[0] || '') || '';
    $.ajax({
        url: (window.__ctxPath || '') + '/ControllerCLT',
        method: 'POST', contentType: 'application/json; charset=utf-8',
        data: JSON.stringify({ acao: 'readmitir', idpessoa: parseInt(idFuncionario),
                               dataadmissao: iso, cargo: cargo }),
        dataType: 'json',
        success: function (r) {
            if (r.ok) { mostrarAlerta(r.msg, 'success', '#alerta', 4000);
                        carregarPerfilCLT(idFuncionario, $('#inputPeriodo').val()); }
            else mostrarAlerta(r.msg, 'danger', '#alerta', 5000);
        },
        error: function () { mostrarAlerta('Erro ao readmitir.', 'danger', '#alerta', 5000); }
    });
}

function alterarStatus(status) {
    if (!window.__idCLT) return;
    $.ajax({
        url: (window.__ctxPath || '') + '/ControllerCLT',
        method: 'POST', contentType: 'application/json; charset=utf-8',
        data: JSON.stringify({ acao: 'atualizarStatus', idpessoa: window.__idCLT, statusemprego: status }),
        dataType: 'json',
        success: function (r) {
            if (r.ok) {
                carregarPerfilCLT(window.__idCLT, $('#inputPeriodo').val());
                mostrarAlerta(r.msg, 'success', '#alerta', 3000);
            } else {
                mostrarAlerta(r.msg, 'danger', '#alerta', 4000);
            }
        }
    });
}

function calcularFechamentoCLT(idFuncionario, periodo) {
    if (!confirm('Calcular o fechamento de ' + periodo + '? Os vales pendentes do mês serão marcados como descontados.')) return;
    $('#btnCalcular').prop('disabled', true).html('<i class="fas fa-spinner fa-spin me-1"></i>Calculando...');
    $.ajax({
        url: (window.__ctxPath || '') + '/ControllerCLT',
        method: 'POST', contentType: 'application/json; charset=utf-8',
        data: JSON.stringify({ acao: 'calcularFechamento', idpessoa: parseInt(idFuncionario), periodo: periodo }),
        dataType: 'json',
        success: function (r) {
            $('#btnCalcular').prop('disabled', false).html('<i class="fas fa-sync-alt me-1"></i>Calcular');
            if (r.ok) {
                renderFechamento(r.fechamento);
                carregarPerfilCLT(idFuncionario, periodo);
                mostrarAlerta(r.msg, 'success', '#alerta', 4000);
            } else {
                mostrarAlerta(r.msg, 'danger', '#alerta', 4000);
            }
        },
        error: function () {
            $('#btnCalcular').prop('disabled', false).html('<i class="fas fa-sync-alt me-1"></i>Calcular');
            mostrarAlerta('Erro ao calcular fechamento.', 'danger', '#alerta', 4000);
        }
    });
}

/* helpers de formatação usados no módulo CLT */
function formatBRL(v) {
    if (v == null) return 'R$ 0,00';
    return 'R$ ' + parseFloat(v).toFixed(2).replace('.', ',').replace(/\B(?=(\d{3})+(?!\d))/g, '.');
}
function formatMin(min) {
    var h = Math.floor(min / 60), m = min % 60;
    return String(h).padStart(2, '0') + ':' + String(m).padStart(2, '0');
}
function formatDataBR(iso) {
    if (!iso) return '-';
    var p = String(iso).substring(0, 10).split('-');
    return p.length === 3 ? p[2] + '/' + p[1] + '/' + p[0] : iso;
}
function diasUteisDoMes(periodo) {
    if (!periodo) return 0;
    var p = periodo.split('-'), ano = parseInt(p[0]), mes = parseInt(p[1]) - 1;
    var d = new Date(ano, mes, 1), count = 0;
    while (d.getMonth() === mes) {
        var dow = d.getDay();
        if (dow >= 1 && dow <= 5) count++;
        d.setDate(d.getDate() + 1);
    }
    return count;
}

/******************************************************************************************************/
/* MÓDULO ÁREA DE PRODUÇÃO — areaproducao.jsp                                                         */
/******************************************************************************************************/
var tabelaAreas; // DataTable da página areaproducao.jsp

$(document).ready(function () {
    if (!$('#tabelaAreas').length) return;

    $('#cep').mask('00000-000');
    tabelaAreas = $('#tabelaAreas').DataTable({ language: { url: 'https://cdn.datatables.net/plug-ins/1.11.5/i18n/pt_BR.json' } });
    areaProducaoCarregarTabela();

    $('#btnSalvar').on('click', areaProducaoSalvar);
    $('#btnAtualizar').on('click', areaProducaoAtualizar);
    $('#modalCadastro').on('hidden.bs.modal', function () {
        $('#formCadastro')[0].reset();
        limparAlerta('#alertModalCadastro');
    });
    $('#modalEdicao').on('hidden.bs.modal', function () { limparAlerta('#alertModalEdicao'); });
});

/** Busca todas as áreas de produção e repopula o DataTable #tabelaAreas. Usado em: areaproducao.jsp */
function areaProducaoCarregarTabela() {
    $.get(CTX + '/ControllerAreaProducao', function (dados) {
        tabelaAreas.clear();
        dados.forEach(function (a) {
            tabelaAreas.row.add([
                a.PropriedadeAreaProducao,
                a.ProprietarioAreaProducao,
                a.SiglasAreaProducao,
                a.QuantidadeTotalPlantasAreaProducao,
                a.cep || '',
                '<button class="btn-acao btn-acao-editar" title="Editar" onclick="areaProducaoAbrirEdicao(' + JSON.stringify(a).replace(/"/g, '&quot;') + ')"><i class="fas fa-pen-to-square"></i></button> ' +
                '<button class="btn-acao btn-acao-excluir" title="Excluir" onclick="areaProducaoExcluir(' + a.idAreaProducao + ')"><i class="fas fa-trash"></i></button>'
            ]).draw(false);
        });
    });
}

/** Coleta os dados do modal de cadastro e chama areaProducaoEnviar com ação 'create'. Usado em: areaproducao.jsp */
function areaProducaoSalvar() {
    let payload = {
        acao: 'create',
        propriedadeareaproducao: $('#propriedadeareaproducao').val().trim(),
        proprietarioareaproducao: $('#proprietarioareaproducao').val().trim(),
        siglasareaproducao: $('#siglasareaproducao').val().trim(),
        quantidadetotalplantasareaproducao: parseInt($('#quantidadetotalplantasareaproducao').val()) || 0,
        cep: $('#cep').val().trim(),
        numero: parseInt($('#numero').val()) || 0,
        complemento: $('#complemento').val().trim()
    };
    areaProducaoEnviar(payload, '#alertModalCadastro', '#modalCadastro');
}

/** Preenche e abre o modal #modalEdicao com os dados da área selecionada. Usado em: areaproducao.jsp */
function areaProducaoAbrirEdicao(a) {
    $('#editId').val(a.idAreaProducao);
    $('#editPropriedade').val(a.PropriedadeAreaProducao);
    $('#editProprietario').val(a.ProprietarioAreaProducao);
    $('#editSigla').val(a.SiglasAreaProducao);
    $('#editQtdPlantas').val(a.QuantidadeTotalPlantasAreaProducao);
    $('#editCep').val(a.cep);
    $('#editNumero').val(a.numero);
    $('#editComplemento').val(a.complemento);
    $('#modalEdicao').modal('show');
}

/** Coleta os dados do modal de edição e chama areaProducaoEnviar com ação 'update'. Usado em: areaproducao.jsp */
function areaProducaoAtualizar() {
    let payload = {
        acao: 'update',
        idareaproducao: parseInt($('#editId').val()),
        propriedadeareaproducao: $('#editPropriedade').val().trim(),
        proprietarioareaproducao: $('#editProprietario').val().trim(),
        siglasareaproducao: $('#editSigla').val().trim(),
        quantidadetotalplantasareaproducao: parseInt($('#editQtdPlantas').val()) || 0,
        cep: $('#editCep').val().trim(),
        numero: parseInt($('#editNumero').val()) || 0,
        complemento: $('#editComplemento').val().trim()
    };
    areaProducaoEnviar(payload, '#alertModalEdicao', '#modalEdicao');
}

/** Solicita confirmação e envia POST para excluir a área de produção. Usado em: areaproducao.jsp */
function areaProducaoExcluir(id) {
    if (!confirm('Excluir esta área? Talões vinculados serão afetados.')) return;
    areaProducaoEnviar({ acao: 'delete', idareaproducao: id }, '#alertPage', null);
}

/**
 * Função base AJAX para criar, editar e excluir áreas de produção.
 * Fecha o modal, exibe alerta e recarrega a tabela após sucesso.
 * Usado em: areaproducao.jsp — chamada por areaProducaoSalvar, areaProducaoAtualizar, areaProducaoExcluir
 */
function areaProducaoEnviar(payload, alertSelector, modalId) {
    $.ajax({
        url: CTX + '/ControllerAreaProducao',
        method: 'POST', contentType: 'application/json',
        data: JSON.stringify(payload),
        success: function (res) {
            if (res.ok) {
                if (modalId) $(modalId).modal('hide');
                mostrarAlerta(res.msg, 'success', '#alertPage');
                areaProducaoCarregarTabela();
            } else {
                mostrarAlerta(res.msg, 'danger', alertSelector);
            }
        },
        error: function () { mostrarAlerta('Erro na requisição.', 'danger', alertSelector); }
    });
}

/** Oculta e limpa o conteúdo de um container de alerta. Usado em: areaproducao.jsp, talao.jsp */
function limparAlerta(sel) { $(sel).removeClass().addClass('alert d-none').empty(); }

/******************************************************************************************************/
/* MÓDULO TALÃO — talao.jsp                                                                           */
/******************************************************************************************************/
var tabelaTalao; // DataTable da página talao.jsp

$(document).ready(function () {
    if (!$('#tabelaTalao').length) return;

    tabelaTalao = $('#tabelaTalao').DataTable({ language: { url: 'https://cdn.datatables.net/plug-ins/1.11.5/i18n/pt_BR.json' } });
    talaoCarregarAreas($('#idareaproducao'));
    talaoCarregarAreas($('#editIdArea'));
    talaoCarregarAlimentos($('#idproduto'));
    talaoCarregarAlimentos($('#editIdProduto'));
    carregarTaloes();

    $('#btnSalvar').on('click', talaoSalvar);
    $('#btnAtualizar').on('click', talaoAtualizar);
    $('#btnSalvarFinanceiro').on('click', salvarFinanceiro);
    $('#modalCadastro').on('hidden.bs.modal', function () { $('#formCadastro')[0].reset(); });
});

/** Popula um select com as áreas de produção disponíveis. Usado em: talao.jsp (selects de cadastro e edição) */
function talaoCarregarAreas(sel) {
    $.get(CTX + '/ControllerAreaProducao', function (dados) {
        sel.find('option:not(:first)').remove();
        dados.forEach(function (a) {
            sel.append('<option value="' + a.idAreaProducao + '">' + a.siglasAreaProducao + ' — ' + a.propriedadeAreaProducao + '</option>');
        });
    });
}

/** Popula um select com os alimentos cadastrados. Usado em: talao.jsp (selects de cadastro e edição) */
function talaoCarregarAlimentos(sel) {
    $.get(CTX + '/ControllerAlimento', function (dados) {
        sel.find('option:not(:first)').remove();
        dados.forEach(function (a) {
            sel.append('<option value="' + a.idproduto + '">' + a.nomeproduto + '</option>');
        });
    });
}

/** Busca todos os talões e repopula o DataTable #tabelaTalao com botões de ação. Usado em: talao.jsp */
function carregarTaloes() {
    $.get(CTX + '/ControllerTalao', function (dados) {
        tabelaTalao.clear();
        dados.forEach(function (t) {
            tabelaTalao.row.add([
                t.descricaoTalao,
                t.alimentoTalao ? t.alimentoTalao.nomeproduto : '',
                t.areaproducaoTalao ? t.areaproducaoTalao.propriedadeAreaProducao : '',
                t.areaproducaoTalao ? t.areaproducaoTalao.siglasAreaProducao : '',
                t.quantidadeplantasTalao,
                '<button class="btn-acao btn-acao-editar" title="Editar" onclick="talaoAbrirEdicao(' + JSON.stringify(t).replace(/"/g,'&quot;') + ')"><i class="fas fa-pen-to-square"></i></button> ' +
                '<button class="btn-acao btn-acao-financeiro" title="Financeiro" onclick="abrirFinanceiro(' + t.idTalao + ')"><i class="fas fa-dollar-sign"></i></button> ' +
                '<button class="btn-acao btn-acao-excluir" title="Excluir" onclick="talaoExcluir(' + t.idTalao + ')"><i class="fas fa-trash"></i></button>'
            ]).draw(false);
        });
    });
}

/** Coleta os dados do modal de cadastro e chama talaoEnviar com ação 'create'. Usado em: talao.jsp */
function talaoSalvar() {
    let payload = {
        acao: 'create',
        descricaotalao: $('#descricaotalao').val().trim(),
        idproduto: parseInt($('#idproduto').val()),
        idareaproducao: parseInt($('#idareaproducao').val()),
        quantidadeplantastalao: parseInt($('#quantidadeplantastalao').val()) || 0
    };
    talaoEnviar(payload, '#alertModalCadastro', '#modalCadastro');
}

/** Preenche e abre o modal #modalEdicao com os dados do talão selecionado. Usado em: talao.jsp */
function talaoAbrirEdicao(t) {
    $('#editId').val(t.idTalao);
    $('#editDescricao').val(t.descricaoTalao);
    if (t.alimentoTalao) $('#editIdProduto').val(t.alimentoTalao.idproduto);
    if (t.areaproducaoTalao) $('#editIdArea').val(t.areaproducaoTalao.idAreaProducao);
    $('#editQtdPlantas').val(t.quantidadeplantasTalao);
    $('#modalEdicao').modal('show');
}

/** Coleta os dados do modal de edição e chama talaoEnviar com ação 'update'. Usado em: talao.jsp */
function talaoAtualizar() {
    let payload = {
        acao: 'update',
        idtalao: parseInt($('#editId').val()),
        descricaotalao: $('#editDescricao').val().trim(),
        idproduto: parseInt($('#editIdProduto').val()),
        idareaproducao: parseInt($('#editIdArea').val()),
        quantidadeplantastalao: parseInt($('#editQtdPlantas').val()) || 0
    };
    talaoEnviar(payload, '#alertModalEdicao', '#modalEdicao');
}

/** Solicita confirmação e envia POST para excluir o talão. Usado em: talao.jsp */
function talaoExcluir(id) {
    if (!confirm('Excluir este talão?')) return;
    talaoEnviar({ acao: 'delete', idtalao: id }, '#alertPage', null);
}

/** Busca o talão financeiro associado e abre o modal #modalFinanceiro para criação ou edição. Usado em: talao.jsp */
function abrirFinanceiro(idTalao) {
    $('#finIdTalao').val(idTalao);
    $('#formFinanceiro')[0].reset();
    $.get(CTX + '/ControllerTalao?financeiro=1&id=' + idTalao, function (tf) {
        if (tf && tf.idTalaoFinanceiro) {
            $('#finIdTalaoFinanceiro').val(tf.idTalaoFinanceiro);
            $('#safratalaofinanceiro').val(tf.safraTalaoFinanceiro);
            if (tf.iniciosafraTalaoFinanceiro) $('#iniciosafratalaofinanceiro').val(tf.iniciosafraTalaoFinanceiro.split('T')[0]);
            if (tf.terminosafraTalaoFinanceiro) $('#terminosafratalaofinanceiro').val(tf.terminosafraTalaoFinanceiro.split('T')[0]);
            $('#custosafratalaofinanceiro').val(tf.custosafraTalaoFinanceiro || 0);
            $('#despesassafratalaofinanceiro').val(tf.despesassafraTalaoFinanceiro || 0);
            $('#vendabrutasafratalaofinanceiro').val(tf.vendabrutassafraTalaoFinanceiro || 0);
            $('#vendaliquidasafratalaofinanceiro').val(tf.vendasliquidassafraTalaoFinanceiro || 0);
        }
    });
    $('#modalFinanceiro').modal('show');
}

/** Coleta os dados do formulário financeiro e envia POST para criar ou atualizar o talão financeiro. Usado em: talao.jsp */
function salvarFinanceiro() {
    let idTF = parseInt($('#finIdTalaoFinanceiro').val());
    let payload = {
        acao: idTF ? 'atualizarfinanceiro' : 'criarfinanceiro',
        idtalao: parseInt($('#finIdTalao').val()),
        idtalaofinanceiro: idTF || undefined,
        safratalaofinanceiro: $('#safratalaofinanceiro').val().trim(),
        iniciosafratalaofinanceiro: $('#iniciosafratalaofinanceiro').val(),
        terminosafratalaofinanceiro: $('#terminosafratalaofinanceiro').val(),
        custosafratalaofinanceiro: parseFloat($('#custosafratalaofinanceiro').val()) || 0,
        despesassafratalaofinanceiro: parseFloat($('#despesassafratalaofinanceiro').val()) || 0,
        vendabrutasafratalaofinanceiro: parseFloat($('#vendabrutasafratalaofinanceiro').val()) || 0,
        vendaliquidasafratalaofinanceiro: parseFloat($('#vendaliquidasafratalaofinanceiro').val()) || 0
    };
    talaoEnviar(payload, '#alertModalFinanceiro', '#modalFinanceiro');
}

/** Função central de envio AJAX para ControllerTalao; oculta modal e recarrega tabela em caso de sucesso. Usado em: talao.jsp */
function talaoEnviar(payload, alertSelector, modalId) {
    $.ajax({
        url: CTX + '/ControllerTalao',
        method: 'POST', contentType: 'application/json',
        data: JSON.stringify(payload),
        success: function (res) {
            if (res.ok) {
                if (modalId) $(modalId).modal('hide');
                mostrarAlerta(res.msg, 'success', '#alertPage');
                carregarTaloes();
            } else {
                mostrarAlerta(res.msg, 'danger', alertSelector);
            }
        },
        error: function () { mostrarAlerta('Erro na requisição.', 'danger', alertSelector); }
    });
}

/* ========== folhapagamento.jsp ========== */
var tabelaFolha;

$(document).ready(function () {
    if (!$('#tabelaFolha').length) return;

    tabelaFolha = $('#tabelaFolha').DataTable({
        language: { url: 'https://cdn.datatables.net/plug-ins/1.11.5/i18n/pt_BR.json' },
        paging: false
    });

    let mesAtual = new Date().toISOString().slice(0, 7);
    $('#periodoGerar').val(mesAtual);
    $('#periodoVer').val(mesAtual);

    $('#btnGerarFolha').on('click', gerarFolha);
    $('#btnVerFolha').on('click', function () { verFolha($('#periodoVer').val()); });
    $('#btnExcelFolha').on('click', function () { folhaExportar('excel'); });
    $('#btnPdfFolha').on('click', function () { folhaExportar('pdf'); });

    verFolha(mesAtual);
});

/** Envia POST para ControleColaborador calculando e persistindo a folha do período informado. Usado em: folhapagamento.jsp */
function gerarFolha() {
    let periodo = $('#periodoGerar').val();
    if (!periodo) { mostrarAlerta('Selecione o período.', 'warning', '#alertPage'); return; }
    let btn = $('#btnGerarFolha');
    btn.prop('disabled', true).html('<i class="fas fa-spinner fa-spin me-1"></i>Gerando...');
    $.ajax({
        url: CTX + '/ControleColaborador',
        method: 'POST', contentType: 'application/json',
        data: JSON.stringify({ acao: 'gerarfolha', periodo: periodo }),
        success: function (res) {
            mostrarAlerta(res.msg, res.ok ? 'success' : 'danger', '#alertPage');
            if (res.ok) { $('#periodoVer').val(periodo); verFolha(periodo); }
            btn.prop('disabled', false).html('<i class="fas fa-play me-1"></i>Gerar Folha');
        },
        error: function () {
            mostrarAlerta('Erro ao gerar folha.', 'danger', '#alertPage');
            btn.prop('disabled', false).html('<i class="fas fa-play me-1"></i>Gerar Folha');
        }
    });
}

/** Consulta o ControleColaborador e popula o DataTable #tabelaFolha com os lançamentos do período. Usado em: folhapagamento.jsp */
function verFolha(periodo) {
    if (!periodo) return;
    $.get(CTX + '/ControleColaborador?periodo=' + periodo, function (dados) {
        tabelaFolha.clear();
        let total = 0;
        dados.forEach(function (fp) {
            let tipo = { CLT: 'CLT', DIARISTA: 'Diarista', PRODUCAO: 'Produção', EMPREITA: 'Empreita' }[fp.tipoFuncionario] || fp.tipoFuncionario;
            tabelaFolha.row.add([
                fp.nomeFuncionario,
                tipo,
                fp.periodo,
                'R$ ' + parseFloat(fp.valorCalculado).toFixed(2).replace('.', ','),
                fp.dataGeracao ? fp.dataGeracao.replace('T', ' ').substring(0, 16) : ''
            ]).draw(false);
            total += parseFloat(fp.valorCalculado) || 0;
        });
        $('#totalFolha').text('R$ ' + total.toFixed(2).replace('.', ','));
    });
}

/** Redireciona para RelatorioExportServlet para baixar a folha do período como Excel ou PDF. Usado em: folhapagamento.jsp */
function folhaExportar(tipo) {
    let periodo = $('#periodoVer').val();
    if (!periodo) { mostrarAlerta('Selecione o período para exportar.', 'warning', '#alertPage'); return; }
    window.location.href = CTX + '/RelatorioExportServlet?tipo=' + tipo + '&modulo=folhapagamento&periodo=' + periodo;
}

/* ========== pontoeletronico.jsp ========== */
$(document).ready(function () {
    if (!$('#selFuncionario').length) return;

    const hoje = new Date();
    const mesAtual = hoje.getFullYear() + '-' + String(hoje.getMonth() + 1).padStart(2, '0');
    $('#selPeriodo').val(mesAtual);
    $('#pontoData').val(hoje.toISOString().slice(0, 10));
    $('#manualData').val(hoje.toISOString().slice(0, 10));
    $('#faltaData').val(hoje.toISOString().slice(0, 10));
    $('#valeData').val(hoje.toISOString().slice(0, 10));

    pontoCFuncionarios();
    $('#btnBuscar').on('click', buscar);

    // relógio
    (function tick() {
        const agora = new Date();
        const el = document.getElementById('relogio');
        if (el) el.textContent =
            String(agora.getHours()).padStart(2, '0') + ':' +
            String(agora.getMinutes()).padStart(2, '0') + ':' +
            String(agora.getSeconds()).padStart(2, '0');
        const dd = document.getElementById('dataHoje');
        if (dd) dd.textContent = agora.toLocaleDateString('pt-BR', { weekday: 'long', day: '2-digit', month: 'long' });
        setTimeout(tick, 1000);
    })();
});

/** Nomes curtos dos dias da semana usados na montagem do espelho de ponto. Usado em: pontoeletronico.jsp */
const DIAS_SEMANA = ['Dom', 'Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb'];

/** Popula o select #selFuncionario com todos os funcionários ativos. Usado em: pontoeletronico.jsp */
function pontoCFuncionarios() {
    $.get(CTX + '/ControllerPontoEletronico?tipo=funcionarios', function (lista) {
        const sel = $('#selFuncionario');
        lista.forEach(f => sel.append(
            '<option value="' + f.idPessoa + '">' + f.nomePessoa + ' (' + (f.tipoFuncionario || '—') + ')</option>'
        ));
    }).fail(() => mostrarAlerta('Erro ao carregar funcionários.', 'danger', '#alerta'));
}

/** Retorna o id do funcionário selecionado no filtro. Usado em: pontoeletronico.jsp */
function getIdFunc()  { return $('#selFuncionario').val(); }
/** Retorna o período (YYYY-MM) selecionado no filtro. Usado em: pontoeletronico.jsp */
function getPeriodo() { return $('#selPeriodo').val(); }

/** Dispara carregamento de espelho, faltas, vales e ponto do dia para o funcionário e período selecionados. Usado em: pontoeletronico.jsp */
function buscar() {
    const id = getIdFunc();
    if (!id) { mostrarAlerta('Selecione um funcionário.', 'warning', '#alerta', 3000); return; }
    const p = getPeriodo();
    carregarEspelho(id, p);
    carregarFaltas(id, p);
    carregarVales(id, p);
    carregarPontoHoje(id);
}

/** Busca o espelho de ponto do funcionário no período e popula a tabela #corpoEspelho. Usado em: pontoeletronico.jsp */
function carregarEspelho(id, periodo) {
    $.get(CTX + '/ControllerPontoEletronico?tipo=pontos&idfuncionario=' + id + '&periodo=' + periodo, function (lista) {
        const tbody = $('#corpoEspelho').empty();
        let totMin = 0, totExt = 0;
        if (!lista.length) {
            tbody.html('<tr><td colspan="10" class="text-center text-muted">Nenhum registro.</td></tr>');
            $('#totalHoras,#totalExtras').text('—');
            atualizarKpiDias(0, 0);
            return;
        }
        lista.forEach(r => {
            const dt  = new Date(r.dataRegistro + 'T00:00');
            const dsm = DIAS_SEMANA[dt.getDay()];
            const ext = r.extraMinutos > 0
                ? '<span class="badge text-bg-primary badge-ponto">' + minParaHora(r.extraMinutos) + '</span>'
                : '<span class="text-muted">—</span>';
            totMin += r.totalMinutos;
            totExt += r.extraMinutos;
            tbody.append('<tr>'
                + '<td>' + formatarData(r.dataRegistro) + '</td>'
                + '<td>' + dsm + '</td>'
                + '<td class="hora-cell text-success">' + (r.entrada1 || '—') + '</td>'
                + '<td class="hora-cell text-danger">' + (r.saida1 || '—') + '</td>'
                + '<td class="hora-cell text-success">' + (r.entrada2 || '—') + '</td>'
                + '<td class="hora-cell text-danger">' + (r.saida2 || '—') + '</td>'
                + '<td class="hora-cell fw-semibold">' + minParaHora(r.totalMinutos) + '</td>'
                + '<td>' + ext + '</td>'
                + '<td><small class="text-muted">' + (r.observacao || '') + '</small></td>'
                + '<td><button class="btn-acao btn-acao-excluir" onclick="excluirPonto(' + r.idPonto + ')" title="Excluir"><i class="fas fa-trash-can"></i></button></td>'
                + '</tr>');
        });
        $('#totalHoras').text(minParaHora(totMin));
        $('#totalExtras').text(totExt > 0 ? minParaHora(totExt) : '—');
        atualizarKpiDias(lista.length, totExt);
    }).fail(() => mostrarAlerta('Erro ao carregar espelho.', 'danger', '#alerta'));
}

/** Busca e exibe as marcações do funcionário no dia de hoje no painel #pontoHoje. Usado em: pontoeletronico.jsp */
function carregarPontoHoje(id) {
    $.get(CTX + '/ControllerPontoEletronico?tipo=pontos&idfuncionario=' + id + '&periodo=' + getPeriodo(), function (lista) {
        const hoje = new Date().toISOString().slice(0, 10);
        const reg  = lista.find(r => r.dataRegistro === hoje);
        const div  = $('#pontoHoje');
        if (!reg) { div.html('<span class="text-muted">Nenhuma marcação hoje.</span>'); return; }
        div.html(
            '<div class="row g-2 text-center">'
            + '<div class="col-3"><div class="border rounded p-2 ' + (reg.entrada1 ? 'bg-success text-white' : 'bg-light text-muted') + '"><div class="small">Entrada 1</div><strong>' + (reg.entrada1 || '—') + '</strong></div></div>'
            + '<div class="col-3"><div class="border rounded p-2 ' + (reg.saida1 ? 'bg-danger text-white' : 'bg-light text-muted') + '"><div class="small">Saída 1</div><strong>' + (reg.saida1 || '—') + '</strong></div></div>'
            + '<div class="col-3"><div class="border rounded p-2 ' + (reg.entrada2 ? 'bg-primary text-white' : 'bg-light text-muted') + '"><div class="small">Entrada 2</div><strong>' + (reg.entrada2 || '—') + '</strong></div></div>'
            + '<div class="col-3"><div class="border rounded p-2 ' + (reg.saida2 ? 'bg-secondary text-white' : 'bg-light text-muted') + '"><div class="small">Saída 2</div><strong>' + (reg.saida2 || '—') + '</strong></div></div>'
            + '</div><div class="text-center mt-2">Total: <strong>' + minParaHora(reg.totalMinutos) + '</strong>'
            + (reg.extraMinutos > 0 ? ' · Extras: <strong class="text-primary">' + minParaHora(reg.extraMinutos) + '</strong>' : '')
            + '</div>'
        );
    });
}

/** Busca as faltas do funcionário no período e popula a tabela #corpoFaltas. Usado em: pontoeletronico.jsp */
function carregarFaltas(id, periodo) {
    $.get(CTX + '/ControllerPontoEletronico?tipo=faltas&idfuncionario=' + id + '&periodo=' + periodo, function (lista) {
        const tbody = $('#corpoFaltas').empty();
        if (!lista.length) {
            tbody.html('<tr><td colspan="4" class="text-center text-muted">Nenhuma falta.</td></tr>');
            $('#kpiFaltas').text('0');
            return;
        }
        let inj = 0;
        lista.forEach(f => {
            if (!f.justificada) inj++;
            const badge = f.justificada
                ? '<span class="badge text-bg-warning">Justificada</span>'
                : '<span class="badge text-bg-danger">Injustificada</span>';
            tbody.append('<tr>'
                + '<td>' + formatarData(f.dataFalta) + '</td>'
                + '<td>' + badge + '</td>'
                + '<td><small>' + (f.motivo || '—') + '</small></td>'
                + '<td><button class="btn-acao btn-acao-excluir" onclick="excluirFalta(' + f.idFalta + ')" title="Excluir"><i class="fas fa-trash-can"></i></button></td>'
                + '</tr>');
        });
        $('#kpiFaltas').text(lista.length + ' (' + inj + ' inj.)');
    });
}

/** Busca os vales do funcionário no período e popula a tabela #corpoVales com total acumulado. Usado em: pontoeletronico.jsp */
function carregarVales(id, periodo) {
    $.get(CTX + '/ControllerPontoEletronico?tipo=vales&idfuncionario=' + id + '&periodo=' + periodo, function (lista) {
        const tbody = $('#corpoVales').empty();
        if (!lista.length) {
            tbody.html('<tr><td colspan="4" class="text-center text-muted">Nenhum vale.</td></tr>');
            $('#kpiVales').text('R$ 0,00');
            $('#totalValesRodape').text('R$ 0,00');
            return;
        }
        let soma = 0;
        lista.forEach(v => {
            soma += parseFloat(v.valor);
            tbody.append('<tr>'
                + '<td>' + formatarData(v.dataVale) + '</td>'
                + '<td class="text-danger fw-semibold">R$ ' + fmtMoeda(v.valor) + '</td>'
                + '<td><small>' + (v.descricao || '—') + '</small></td>'
                + '<td><button class="btn-acao btn-acao-excluir" onclick="excluirVale(' + v.idVale + ')" title="Excluir"><i class="fas fa-trash-can"></i></button></td>'
                + '</tr>');
        });
        const total = 'R$ ' + fmtMoeda(soma);
        $('#kpiVales').text(total);
        $('#totalValesRodape').text(total);
    });
}

/** Atualiza os KPI cards #kpiDias e #kpiExtras com os totais do espelho carregado. Usado em: pontoeletronico.jsp */
function atualizarKpiDias(dias, extMin) {
    $('#kpiDias').text(dias + ' dia' + (dias !== 1 ? 's' : ''));
    $('#kpiExtras').text(extMin > 0 ? minParaHora(extMin) : '00:00');
}

/** Envia registro de uma marcação de ponto (entrada1, saida1, entrada2 ou saida2) via pontoPost. Usado em: pontoeletronico.jsp */
function baterPonto(campo) {
    const id = getIdFunc();
    if (!id) { mostrarAlerta('Selecione um funcionário.', 'warning', '#alerta', 3000); return; }
    pontoPost({ acao: 'registrar_ponto', idfuncionario: parseInt(id),
        dataregistro: $('#pontoData').val(), campo, hora: $('#pontoHora').val() || null },
        res => { mostrarAlerta(res.msg, 'success', '#alerta', 4000); buscar(); });
}

/** Envia um lançamento manual de ponto com as quatro marcações para o funcionário selecionado. Usado em: pontoeletronico.jsp */
function salvarManual() {
    const id = getIdFunc();
    if (!id) { mostrarAlerta('Selecione um funcionário.', 'warning', '#alerta', 3000); return; }
    pontoPost({ acao: 'salvar_ponto_manual', idfuncionario: parseInt(id),
        dataregistro: $('#manualData').val(),
        entrada1: $('#manualE1').val() || null, saida1: $('#manualS1').val() || null,
        entrada2: $('#manualE2').val() || null, saida2: $('#manualS2').val() || null,
        observacao: $('#manualObs').val() },
        res => { mostrarAlerta(res.msg, 'success', '#alerta', 4000); buscar(); });
}

/** Solicita confirmação e envia DELETE do registro de ponto pelo id. Usado em: pontoeletronico.jsp */
function excluirPonto(id) {
    if (!confirm('Excluir este registro?')) return;
    pontoPost({ acao: 'excluir_ponto', idponto: id },
        res => { mostrarAlerta(res.msg, 'success', '#alerta', 3000); buscar(); });
}

/** Envia registro de falta (justificada ou não) para o funcionário e data selecionados. Usado em: pontoeletronico.jsp */
function registrarFalta() {
    const id = getIdFunc();
    if (!id) { mostrarAlerta('Selecione um funcionário.', 'warning', '#alerta', 3000); return; }
    pontoPost({ acao: 'registrar_falta', idfuncionario: parseInt(id),
        datafalta: $('#faltaData').val(),
        justificada: $('#faltaJustificada').is(':checked'),
        motivo: $('#faltaMotivo').val() },
        res => { mostrarAlerta(res.msg, 'success', '#alerta', 4000); buscar(); });
}

/** Solicita confirmação e envia DELETE da falta pelo id. Usado em: pontoeletronico.jsp */
function excluirFalta(id) {
    if (!confirm('Remover esta falta?')) return;
    pontoPost({ acao: 'excluir_falta', idfalta: id },
        res => { mostrarAlerta(res.msg, 'success', '#alerta', 3000); buscar(); });
}

/** Envia registro de vale/adiantamento com valor e descrição para o funcionário selecionado. Usado em: pontoeletronico.jsp */
function registrarVale() {
    const id = getIdFunc();
    if (!id) { mostrarAlerta('Selecione um funcionário.', 'warning', '#alerta', 3000); return; }
    const valor = parseFloat($('#valeValor').val());
    if (!valor || valor <= 0) { mostrarAlerta('Informe um valor válido.', 'warning', '#alerta', 3000); return; }
    pontoPost({ acao: 'registrar_vale', idfuncionario: parseInt(id),
        datavale: $('#valeData').val(), valor, descricao: $('#valeDesc').val() },
        res => { mostrarAlerta(res.msg, 'success', '#alerta', 4000); buscar(); });
}

/** Solicita confirmação e envia DELETE do vale pelo id. Usado em: pontoeletronico.jsp */
function excluirVale(id) {
    if (!confirm('Remover este vale?')) return;
    pontoPost({ acao: 'excluir_vale', idvale: id },
        res => { mostrarAlerta(res.msg, 'success', '#alerta', 3000); buscar(); });
}

/** Envia ação 'fechar_folha' calculando salário bruto, descontos e líquido do funcionário no período. Usado em: pontoeletronico.jsp */
function fecharFolha() {
    const id = getIdFunc();
    if (!id) { mostrarAlerta('Selecione um funcionário.', 'warning', '#alerta', 3000); return; }
    const sal = parseFloat($('#fechSalario').val());
    if (!sal || sal <= 0) { mostrarAlerta('Informe o salário base.', 'warning', '#alerta', 3000); return; }
    const btn = $('#btnFechar').prop('disabled', true).html('<i class="fas fa-spinner fa-spin me-1"></i>Calculando...');
    pontoPost({ acao: 'fechar_folha', idfuncionario: parseInt(id),
        periodo: getPeriodo(), salario_base: sal, jornada_horas: parseInt($('#fechJornada').val()) || 8 },
        res => {
            btn.prop('disabled', false).html('<i class="fas fa-file-invoice-dollar me-1"></i>Gerar Fechamento');
            mostrarAlerta(res.msg, 'success', '#alerta', 5000);
            preencherFechamento(res.fechamento);
        },
        () => btn.prop('disabled', false).html('<i class="fas fa-file-invoice-dollar me-1"></i>Gerar Fechamento'));
}

/** Preenche o painel #painelFechamento com os dados calculados do fechamento de folha. Usado em: pontoeletronico.jsp */
function preencherFechamento(f) {
    $('#fechNome').text(f.nomeFuncionario || '');
    $('#fechPeriodo').text(f.periodo);
    $('#fechDiasUteis').text(f.diasUteis);
    $('#fechDiasTrab').text(f.diasTrabalhados);
    $('#fechFaltas').text(f.faltasInjustificadas);
    $('#fechExtrasHoras').text(minParaHora(f.totalMinutosExtras));
    $('#fechBase').text('R$ ' + fmtMoeda(f.salarioBase));
    $('#fechValorExtras').text('+ R$ ' + fmtMoeda(f.valorHorasExtras));
    $('#fechDescFaltas').text('− R$ ' + fmtMoeda(f.descontoFaltas));
    $('#fechBruto').text('R$ ' + fmtMoeda(f.salarioBruto));
    $('#fechVales').text('− R$ ' + fmtMoeda(f.totalVales));
    $('#fechLiquido').text('R$ ' + fmtMoeda(f.salarioLiquido));
    $('#painelFechamento').show();
}

/** Função central de envio AJAX para ControllerPontoEletronico; chama onOk em sucesso ou onFail em erro de rede. Usado em: pontoeletronico.jsp */
function pontoPost(payload, onOk, onFail) {
    $.ajax({
        url: CTX + '/ControllerPontoEletronico',
        method: 'POST', contentType: 'application/json',
        data: JSON.stringify(payload),
        success: function (res) {
            if (res.ok) { if (onOk) onOk(res); }
            else mostrarAlerta(res.msg, 'danger', '#alerta', 6000);
        },
        error: function () {
            mostrarAlerta('Erro de conexão.', 'danger', '#alerta', 5000);
            if (onFail) onFail();
        }
    });
}

/** Converte minutos inteiros para string no formato HH:MM. Usado em: pontoeletronico.jsp */
function minParaHora(min) {
    if (!min) return '00:00';
    return String(Math.floor(min / 60)).padStart(2, '0') + ':' + String(min % 60).padStart(2, '0');
}

/** Converte data no formato ISO (YYYY-MM-DD) para o padrão BR (DD/MM/YYYY). Usado em: pontoeletronico.jsp */
function formatarData(iso) {
    if (!iso) return '—';
    const p = iso.split('-');
    return p[2] + '/' + p[1] + '/' + p[0];
}

function fmtMoeda(v) {
    return parseFloat(v || 0).toFixed(2).replace('.', ',');
}