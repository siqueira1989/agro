/******************************************************************************************************/
/* CONFIGURACAO GLOBAL — introduzida pela auditoria de 29/08/2026 (P2-23 e P2-25)                      */
/******************************************************************************************************/

/**
 * Caminho do contexto da aplicacao, descoberto em tempo de execucao.
 *
 * ANTES: 35 chamadas AJAX usavam a URL absoluta '/ControllerX'. Publicar o
 * sistema como ROOT ou com qualquer outro nome de contexto quebrava todas de
 * uma vez. Agora o caminho vem do proprio endereco da pagina.
 *
 * A pagina sempre esta em <contexto>/view/admin/... ou <contexto>/login.jsp,
 * entao o primeiro segmento do caminho e o contexto (ou vazio, se ROOT).
 */
const AGRO_CTX = (function () {
    // Preferencia: o valor injetado pelo servidor em pagina/importacao.html
    // (<%=request.getContextPath()%>), que e sempre exato.
    if (typeof window.CTX === 'string') return window.CTX;

    // Fallback para paginas que nao incluem importacao.html (login.jsp):
    // deduz o contexto do proprio endereco.
    var partes = window.location.pathname.split('/').filter(Boolean);
    var pastasDaApp = ['view', 'pagina', 'js', 'css'];
    if (partes.length === 0 || pastasDaApp.indexOf(partes[0].toLowerCase()) >= 0) return '';
    if (partes[0].toLowerCase().endsWith('.jsp')) return '';
    return '/' + partes[0];
})();

/**
 * Sessao expirada — tratamento unico para todas as chamadas AJAX (P0-1).
 *
 * Com o AuthFilter passando a cobrir /*, uma chamada sem sessao agora recebe
 * 401 com JSON em vez do HTML da tela de login. Sem este interceptador, o
 * jQuery tentaria interpretar HTML como dados e o usuario veria um erro de
 * parse incompreensivel. Aqui a resposta correta e clara: voltar ao login,
 * preservando a pagina em que a pessoa estava.
 */
$(document).ajaxError(function (evento, xhr) {
    if (xhr && xhr.status === 401) {
        var destino = encodeURIComponent(window.location.pathname + window.location.search);
        window.location.href = AGRO_CTX + '/login.jsp?redirect=' + destino;
    }
});

// Varias telas ainda usam a global CTX para montar links de paginas JSP. Ela e
// definida em pagina/importacao.html, que o login.jsp nao inclui — referenciar
// CTX ali daria ReferenceError. Este alias garante que as duas existam sempre e
// valham o mesmo.
window.CTX = (typeof window.CTX === 'string') ? window.CTX : AGRO_CTX;

/** Monta a URL de um endpoint do backend a partir do contexto atual. */
function agroUrl(caminho) {
    return AGRO_CTX + (caminho.charAt(0) === '/' ? caminho : '/' + caminho);
}

/**
 * Traducao do DataTables — uma unica origem (P2-25).
 *
 * ANTES conviviam tres URLs: 1.11.5/pt-BR.json, 2.1.6/pt-BR.json e
 * 1.11.5/pt_BR.json. Esta ultima, com underscore, NAO existe no CDN: as quatro
 * tabelas que a usavam (Areas, Maquinas, Safras e Diario) recebiam 404 e caiam
 * silenciosamente para os rotulos em ingles.
 */
const DT_IDIOMA_URL = 'https://cdn.datatables.net/plug-ins/2.1.6/i18n/pt-BR.json';

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
 * Máscara decimal para campos de QUANTIDADE (dose, peso, nº de sacos) — diferente da máscara
 * monetária reversa (`#.##0,00`) usada em salário/valor: aqui o usuário digita a vírgula explicitamente,
 * como escreveria no papel ("1,5"), em vez de "preencher da direita pra esquerda" como dinheiro.
 *
 * MOTIVO (bug real, reproduzido 2026-10-05): os campos de dose/quantidade do Insumo no Diário de
 * Campo eram <input type="number"> puro. A RENDERIZAÇÃO desse tipo de campo (vírgula ou ponto como
 * separador decimal) depende da configuração de localidade do navegador/SO do usuário — fora do
 * nosso controle e inconsistente entre máquinas. O resultado calculado 1,5 × 100 = 150 aparecia como
 * "150,000" numa máquina (vírgula — OK) e pode aparecer "150.000" (ponto) em outra; um usuário
 * brasileiro lendo "150.000" em notação BR entende "cento e cinquenta mil", não "150". Trocar para
 * <input type="text"> com esta máscara elimina a dependência da localidade do navegador: a exibição
 * é sempre em vírgula, controlada por nós, em qualquer máquina.
 */
/**
 * @param casas Não limita mais a digitação (ver bug abaixo) — mantido só por compatibilidade
 *              com quem já chama esta função; pode ser omitido.
 *
 * BUG REAL CONFIRMADO (2026-10-12): esta função costumava truncar o valor em `casas` dígitos
 * decimais DURANTE a digitação. Ao EDITAR um diário já salvo, o campo vinha pré-preenchido já
 * com o máximo de casas (ex.: "0,0300", 4 casas) — qualquer tecla nova que o usuário digitasse
 * no fim/meio do valor resultava num valor truncado de volta ao que já estava lá, então a tela
 * "não mudava nada" e parecia travada. O banco já arredonda pra escala da coluna
 * (NUMERIC(14,4)/NUMERIC(14,3)) ao gravar — não há necessidade nem motivo de truncar no cliente.
 */
function aplicarMascaraDecimal($el, casas) {
    $el.attr('type', 'text').attr('inputmode', 'decimal').attr('autocomplete', 'off');
    $el.off('input.decbr').on('input.decbr', function () {
        var el = this;
        var posAntes = el.selectionStart;
        var bruto = el.value;
        // Aceita "." como decimal também (teclado numérico de muitos notebooks/SOs manda ponto
        // mesmo em layout BR) — convertido pra vírgula antes de filtrar.
        var limpar = function (s) { return s.replace(/\./g, ',').replace(/[^\d,]/g, ''); };
        var limpo = limpar(bruto);
        var partes = limpo.split(',');
        if (partes.length > 2) limpo = partes[0] + ',' + partes.slice(1).join('');  // só 1 vírgula
        if (limpo === bruto) return;   // nada precisou mudar: não mexe no valor nem no cursor
        // Preserva a posição relativa do cursor (sem isso, toda tecla jogava o cursor pro fim do
        // campo, impedindo editar/corrigir no meio do valor já digitado).
        var novaPos = Math.min(limpar(bruto.slice(0, posAntes)).length, limpo.length);
        el.value = limpo;
        el.setSelectionRange(novaPos, novaPos);
    });
}

/** Lê um campo com máscara decimal BR ("1.234,5") como Number — nunca use parseFloat/Number direto nesses campos. */
function lerDecimalBR(v) {
    if (v == null || v === '') return 0;
    var n = parseFloat(String(v).replace(/\./g, '').replace(',', '.'));
    return isNaN(n) ? 0 : n;
}

/** Formata um Number para texto BR com casas fixas, pronto para preencher um campo com máscara decimal. */
function exibirDecimalBR(n, casas) {
    casas = casas || 3;
    return Number(n || 0).toLocaleString('pt-BR', { minimumFractionDigits: casas, maximumFractionDigits: casas });
}

/**
 * Como exibirDecimalBR, mas sem forçar zeros à direita — "0,03" em vez de "0,0300". Usada pra
 * PRÉ-PREENCHER campos editáveis ao abrir um diário pra edição: mostrar logo o valor "cheio" de
 * zeros deixava o campo visualmente no limite de casas decimais, mesmo sem precisar.
 */
function exibirDecimalNatural(n, casasMax) {
    return Number(n || 0).toLocaleString('pt-BR', { maximumFractionDigits: casasMax || 4 });
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

    // Já inicializada: recarrega os dados sem destruir (preserva busca/ordenação/página).
    if ($.fn.DataTable.isDataTable('#tabelaClassificacao')) {
        $('#tabelaClassificacao').DataTable().ajax.reload(null, false);
        return;
    }
    const tabela = $('#tabelaClassificacao').DataTable({
        "processing": false,
        "serverSide": false,

        "ajax": {
            "url": AGRO_CTX + "/ClassificacaoServlet",
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
            "url": DT_IDIOMA_URL
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
        url: AGRO_CTX + '/ClassificacaoServlet',
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
        url: AGRO_CTX + '/ClassificacaoServlet',
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
        url: AGRO_CTX + '/ClassificacaoServlet',
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
function abrirModalAtualizacaoDespesasCustos(id, despesa, unidade, valor, tipo, classificacao) {
    // Atribui os valores aos campos do formulário de atualização
    $('#atualizacaoId').val(id);
    $('#atualizacaoDespesa').val(despesa);
    $('#atualizacaoUnidade').val(unidade);

    // ====> Aqui faz o ajuste no valor
    const valorNumerico = parseFloat(valor);
    const valorFormatado = formatarValorParaBR(valorNumerico);
    $('#atualizacaoValor').val(valorFormatado);

    $('#atualizacaoTipo').val(tipo);
    $('#atualizacaoClassificacao').val(classificacao);
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

/** Linhas da última carga da tabela, por iddespesascusto (os botões passam só o id). */
let _dcCache = {};

/** Escapa texto vindo do banco antes de montar HTML. Usado em: despesascustos.jsp */
function dcEsc(v) {
    return String(v == null ? '' : v).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}

/** Rótulo da categoria (V28). Usado em: despesascustos.jsp */
function dcRotuloCategoria(cat) {
    return cat === 'CAIXA_COLHEITA' ? 'Caixa de colheita' : (cat === 'EMBALAGEM' ? 'Embalagem' : 'Geral');
}

/** Editar a partir do cache da tabela. Usado em: despesascustos.jsp */
function abrirModalAtualizacaoDespesasCustosPorId(id) {
    const r = _dcCache[id];
    if (!r) return;
    abrirModalAtualizacaoDespesasCustos(r.iddespesascusto, r.despesascusto, r.unidadedespesascustos,
        r.valordespesascustos, r.tipodespesascustos, r.classificacao);
}

/** Excluir a partir do cache da tabela. Usado em: despesascustos.jsp */
function abrirModalExclusaoDespesasCustosPorId(id) {
    const r = _dcCache[id];
    if (!r) return;
    abrirModalExclusaoDespesasCustos(r.iddespesascusto, r.despesascusto);
}

/**
 * Preenche os selects de Alimento, Área e Tipo de caixa do modal de cadastro (uma vez por página).
 * Tipos: sugestões do enum TipoCaixa (GET ?tiposCaixa=1) + "Outro (digitar)".
 * Usado em: despesascustos.jsp
 */
function carregarSelectsCaixaDespesasCustos() {
    if ($('#dcAlimento').data('carregado')) return;
    $('#dcAlimento').data('carregado', true);

    $.getJSON(AGRO_CTX + '/ControllerAlimento', function (lista) {
        const $s = $('#dcAlimento');
        (Array.isArray(lista) ? lista : []).forEach(function (a) {
            $s.append($('<option>').val(a.idproduto).text(a.nomeproduto));
        });
    });
    $.getJSON(AGRO_CTX + '/ControllerAreaProducao', function (lista) {
        const $s = $('#dcArea');
        (Array.isArray(lista) ? lista : []).forEach(function (a) {
            $s.append($('<option>').val(a.idAreaProducao)
                .text(a.PropriedadeAreaProducao + (a.SiglasAreaProducao ? ' (' + a.SiglasAreaProducao + ')' : '')));
        });
    });
    $.getJSON(AGRO_CTX + '/DespesasCustosServlet', {tiposCaixa: 1}, function (resp) {
        const $s = $('#dcTipoCaixa');
        ((resp && resp.tipos) || []).forEach(function (t) {
            $s.append($('<option>').val(t.codigo).text(t.rotulo + ' (' + t.codigo + ')').attr('data-cargo', t.cargo));
        });
        $s.append($('<option>').val('__OUTRO__').text('Outro (digitar)...'));
        alternarCamposCaixaDespesasCustos();
    });
}

/**
 * Mostra/oculta Alimento/Área/Tipo conforme a Categoria. Caixa de colheita fixa CONTENTOR;
 * Embalagem esconde o CONTENTOR. Caixas/embalagens sugerem unidade "un" e classificação Variável.
 * Usado em: despesascustos.jsp
 */
function alternarCamposCaixaDespesasCustos() {
    const cat = $('#dcCategoria').val() || 'GERAL';
    const caixa = cat !== 'GERAL';
    $('#dcCamposCaixa').toggleClass('d-none', !caixa);
    if (!caixa) return;

    const $tipo = $('#dcTipoCaixa');
    $tipo.find('option[value="CONTENTOR"]').prop('hidden', cat === 'EMBALAGEM');
    if (cat === 'CAIXA_COLHEITA') {
        $tipo.val('CONTENTOR').prop('disabled', true);
    } else {
        $tipo.prop('disabled', false);
        if ($tipo.val() === 'CONTENTOR') $tipo.val('');
    }
    $('#dcTipoCaixaOutro').toggleClass('d-none', $tipo.val() !== '__OUTRO__');

    if (!$('#unidade').val()) $('#unidade').val('un');
    if (!$('#tipo').val()) $('#tipo').val(cat === 'CAIXA_COLHEITA' ? 'Caixa colheita' : 'Embalagem');
    $('#classificacao').val('VARIAVEL');
}

/** Atualiza os cards de KPI (total/fixas/variáveis). Usado em: despesascustos.jsp */
function atualizarKpisDespesasCustos(lista) {
    if (!$('#kpiDcTotal').length) return;
    let total = lista.length;
    let fixo = 0, variavel = 0;
    lista.forEach(function (item) {
        if (item.classificacao === 'VARIAVEL') variavel++; else fixo++;
    });
    $('#kpiDcTotal').text(total);
    $('#kpiDcFixo').text(fixo);
    $('#kpiDcVariavel').text(variavel);
}

/** Inicializa/recarrega o DataTable de despesas e custos via DespesasCustosServlet. Usado em: despesascustos.jsp */
function CarregarDadosDespesasCustos() {
    // Já inicializada: recarrega sem destruir (preserva busca/ordenação/página).
    if ($.fn.DataTable.isDataTable('#tabelaDespesasCustos')) {
        $('#tabelaDespesasCustos').DataTable().ajax.reload(null, false);
        return;
    }

    const tabela = $('#tabelaDespesasCustos').DataTable({
        "processing": false,
        "serverSide": false,
        "ajax": {
            "url": AGRO_CTX + "/DespesasCustosServlet", // URL da servlet para buscar os dados
            "method": "GET",
            "dataSrc": function (json) {
                if (!Array.isArray(json)) return [];
                _dcCache = {};
                json.forEach(function (d) { _dcCache[d.iddespesascusto] = d; });
                atualizarKpisDespesasCustos(json);
                return json;
            }
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
                "data": "classificacao",
                "render": function (data, type, row) {
                    const variavel = data === 'VARIAVEL';
                    const rotulo = variavel ? 'Variável' : 'Fixo';
                    const cor = variavel ? 'bg-warning text-dark' : 'bg-secondary';
                    return '<span class="badge ' + cor + '">' + rotulo + '</span>';
                }
            },
            {
                "data": "categoria",
                "render": function (data, type, row) {
                    const cat = data || 'GERAL';
                    if (type !== 'display') {
                        return cat + ' ' + (row.nomeAlimento || '') + ' ' + (row.nomeArea || '') + ' ' + (row.tipoCaixa || '');
                    }
                    if (cat === 'GERAL') return '<span class="badge bg-light text-dark border">Geral</span>';
                    const cor = cat === 'CAIXA_COLHEITA' ? 'bg-success' : 'bg-info text-dark';
                    return '<span class="badge ' + cor + '">' + dcEsc(dcRotuloCategoria(cat)) + '</span>'
                         + '<div class="small text-muted mt-1">' + dcEsc(row.nomeAlimento || '?') + ' — '
                         + dcEsc(row.nomeArea || '?') + ' — ' + dcEsc(row.tipoCaixa || '?') + '</div>';
                }
            },
            {
                "data": null,
                "orderable": false,
                "render": function (data, type, row) {
                    // Só o id vai para o onclick; os textos saem do cache (evita quebra/injeção com aspas vindas do banco).
                    const id = parseInt(row.iddespesascusto, 10);
                    return '<button class="btn-acao btn-acao-editar" title="Editar" onclick="abrirModalAtualizacaoDespesasCustosPorId(' + id + ')"><i class="fas fa-pen-to-square"></i></button> '
                         + '<button class="btn-acao btn-acao-excluir" title="Excluir" onclick="abrirModalExclusaoDespesasCustosPorId(' + id + ')"><i class="fas fa-trash-can"></i></button>';
                }
            }
        ],
        "language": {
            "url": DT_IDIOMA_URL // Tradução para o Português
        }
    });
}

/** Envia POST para excluir a despesa/custo confirmado no modal. Usado em: despesascustos.jsp */
function excluirDespesasCustos() {
    var id = $('#exclusaoId').val();
    $.ajax({
        url: AGRO_CTX + '/DespesasCustosServlet',
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
function atualizarDespesasCustos(id, despesa, unidade, valor, tipo, classificacao) {
    $.ajax({
        url: AGRO_CTX + '/DespesasCustosServlet',
        method: 'POST',
        data: {
            acao: 'update',
            iddespesascustos: id,
            despesascustos: despesa,
            unidadedespesacustos: unidade,
            valordespesacustos: valor,
            tipodespesacustos: tipo,
            classificacao: classificacao
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
function salvarDespesasCustos(despesa, unidade, valor, tipo, classificacao, caixa) {
    $.ajax({
        url: AGRO_CTX + '/DespesasCustosServlet',
        method: 'POST',
        data: $.extend({
            acao: 'create',
            despesascusto: despesa,
            unidadedespesacusto: unidade,
            valordespesacusto: valor,
            tipodespesacusto: tipo,
            classificacao: classificacao
        }, caixa || {}),  // caixa: {categoria, idAlimento, idArea, tipoCaixa} (V28)
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
        url: AGRO_CTX + '/ControllerAlimento',
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
        url: AGRO_CTX + '/ControllerAlimento',
        method: 'POST',
        contentType: 'application/json',
        data: JSON.stringify(data),

        success: function (resp) {
            mostrarAlerta(resp.msg || 'Atualizada atualizada com sucesso!', 'success', '#alerta');
            sessionStorage.setItem("mensagemAlerta", resp.msg || 'Alimento atualizado com sucesso!');
            sessionStorage.setItem("tipoAlerta", "success");
            window.location.href = AGRO_CTX + "/view/admin/alimento.jsp";  // P2-23: era URL absoluta com localhost
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
        url: AGRO_CTX + '/ClassificacaoServlet',
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
        url: AGRO_CTX + '/ControllerAlimento',
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
        url: AGRO_CTX + '/ControllerAlimento',
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

    // Já inicializada: recarrega sem destruir (preserva busca/ordenação/página).
    if ($.fn.DataTable.isDataTable('#tabelaAlimento')) {
        $('#tabelaAlimento').DataTable().ajax.reload(null, false);
        return;
    }
    const tabela = $('#tabelaAlimento').DataTable({
        "processing": false,
        "serverSide": false,

        "ajax": {
            "url": AGRO_CTX + "/ControllerAlimento",
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
            "url": DT_IDIOMA_URL
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
        url: AGRO_CTX + '/ControllerAlimento',
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
                    url: DT_IDIOMA_URL
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
        url: AGRO_CTX + '/ControllerAlimento',
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

/* Filtro de tipo atualmente selecionado nas abas de parceiro.jsp ("" = todos). */
if (typeof filtroTipoParceiro === "undefined") { var filtroTipoParceiro = ""; }

/** Traduz o código do tipo de parceiro num badge colorido. */
function badgeTipoParceiro(tipo) {
    const t = (tipo || "").toUpperCase();
    if (t === "FORNECEDOR") return '<span class="badge bg-info text-dark">Fornecedor</span>';
    if (t === "INSUMO")     return '<span class="badge bg-warning text-dark">Insumo</span>';
    return '<span class="badge bg-success">Parceiro</span>';
}

/** Inicializa/recarrega o DataTable de parceiros via ControllerParceiro. Usado em: parceiro.jsp */
function CarregarParceiros() {

    const url = AGRO_CTX + "/ControllerParceiro" + (filtroTipoParceiro ? "?tipo=" + filtroTipoParceiro : "");
    // Já inicializada: atualiza a URL (filtro por tipo) e recarrega sem destruir.
    if ($.fn.DataTable.isDataTable('#tabelaParceiros')) {
        $('#tabelaParceiros').DataTable().ajax.url(url).load(null, false);
        return;
    }
    const tabela = $('#tabelaParceiros').DataTable({
        "processing": false,
        "serverSide": false,

        "ajax": {
            "url": url,
            "method": "GET",
            "dataSrc": ""
        },
        "columns": [
            {"data": "idPessoa"},
            {"data": "nomePessoa"},
            {
                "data": "tipoParceiro",
                "render": function (data) { return badgeTipoParceiro(data); }
            },
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
        onclick="abrirModalEditarParceiro(${row.idPessoa})">
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
            "url": DT_IDIOMA_URL
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
// Delegado no document: o main.js carrega no <head>, antes do botão existir no DOM.
$(document).on("click", "#btnConfirmarDesativar", function () {
    if (!parceiroParaDesativar)
        return;

    $.ajax({
        url: `/ControllerParceiro`,
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
        url: AGRO_CTX + "/ControllerParceiro?acao=dados", // importante!
        method: "get",
        dataType: "json",
        success: function (dados) {
            $("#total").text(dados.total);
            $("#ativosParceiros").text(dados.ativos);
            $("#inativosParceiros").text(dados.inativos);
            // Contagens por tipo (se os elementos existirem)
            $("#countParceiros").text(dados.parceiros ?? 0);
            $("#countFornecedores").text(dados.fornecedores ?? 0);
            $("#countInsumos").text(dados.insumos ?? 0);
        },
        error: function () {
            console.warn("❌ Não foi possível carregar o resumo de parceiros.");
        }
    });
}

/** Aplica o filtro de tipo selecionado nas abas e recarrega a tabela. Usado em: parceiro.jsp */
function filtrarParceirosPorTipo(tipo) {
    filtroTipoParceiro = tipo || "";
    $(".aba-tipo-parceiro").removeClass("active");
    $('.aba-tipo-parceiro[data-tipo="' + filtroTipoParceiro + '"]').addClass("active");
    CarregarParceiros();
}

/** Abre o modal de edição de parceiro e carrega os dados. Usado em: parceiro.jsp */
function abrirModalEditarParceiro(id) {
    $("#alertaParceiro").addClass("d-none").removeClass("alert-danger alert-success").text("");
    carregarDadosParceiro(id);
    $("#modalParceiro").modal("show");
}

/** Salva a edição do parceiro (modal), sem sair da listagem. Usado em: parceiro.jsp */
function salvarParceiroModal() {
    const tipo = $("#nivel").val();
    const data = {
        acao: "update",
        idPessoa: $("#idPessoa").val(),
        nome: $("#nome").val(),
        nivel: (tipo || "").toLowerCase(),
        tipo: tipo,
        email: $("#email").val(),
        telefone: $("#telefone").val(),
        cep: $("#cep").val(),
        numero: $("#numero").val(),
        complemento: $("#complemento").val(),
        situacao: $("#situacao").val(),
        cnpj: $("#cnpj").val(),
        razaosocial: $("#razao").val(),
        inscricaoestadual: $("#inscricaoestadual").val(),
        site: $("#site").val(),
        usuario: $("#usuario").val(),
        senha: $("#senha").val()
    };

    if (!data.nome || !tipo) {
        $("#alertaParceiro").removeClass("d-none").addClass("alert-danger").text("Nome e tipo são obrigatórios.");
        return;
    }

    $.ajax({
        url: AGRO_CTX + "/ControllerParceiro",
        method: "POST",
        contentType: "application/json; charset=utf-8",
        dataType: "json",
        data: JSON.stringify(data),
        success: function (resp) {
            $("#modalParceiro").modal("hide");
            mostrarAlerta(resp.msg || "Parceiro atualizado com sucesso!", "success", "#alerta");
            CarregarParceiros();
            carregarResumoParceiros();
        },
        error: function (xhr) {
            let msg = "Erro ao atualizar parceiro!";
            try { const j = JSON.parse(xhr.responseText); if (j.msg) msg = j.msg; } catch (e) {}
            $("#alertaParceiro").removeClass("d-none").addClass("alert-danger").text(msg);
        }
    });
}

/** Busca os dados de um parceiro por id e preenche o formulário para edição. Usado em: CadastroParceiro.jsp (modo update via ?id=) */
function carregarDadosParceiro(id) {
  $.ajax({
    url: `/ControllerParceiro?id=${id}`,
    method: "GET",
    dataType: "json",
    success: function (parceiro) {
      $("#idPessoa").val(parceiro.idPessoa);
      $("#nome").val(parceiro.nomePessoa);
      $("#usuario").val(parceiro.usuarioPessoa);
      $("#senha").val(parceiro.senhaPessoa);
      // Tipo de negócio (tipoParceiro é a fonte da verdade; nivelPessoa é fallback)
      $("#nivel").val(parceiro.tipoParceiro || (parceiro.nivelPessoa || "").toUpperCase()).trigger("change");
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
      $("#site").val(parceiro.siteparceiro || "");

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
  const tipo = $("#nivel").val();   // PARCEIRO | FORNECEDOR | INSUMO

  // detecta se é create ou update
 const acao = idPessoa  !== "" ? "update" : "create";


  const data = {
    acao: acao,
    idPessoa: idPessoa,
    nome: $("#nome").val(),
    nivel: (tipo || "").toLowerCase(),  // mantém nivelpessoa (cadastro/login)
    tipo: tipo,                          // classificação de negócio (tipo_parceiro)
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

  if (tipo === "PARCEIRO") {
    data.usuario = $("#usuario").val();
    data.senha = $("#senha").val();
  }

  $.ajax({
      url: AGRO_CTX + "/ControllerParceiro",
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
      window.location.href = "/view/admin/parceiro.jsp";
      }else{
          let msgSucesso =resp.msg || "Parceiro atualizado com sucesso!";
           sessionStorage.setItem("mensagemAlerta", msgSucesso);
      sessionStorage.setItem("tipoAlerta", "success");
      window.location.href = "/view/admin/parceiro.jsp";
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

/** Converte "1.500,00" (máscara BRL) → número 1500.00. */
function parseBRL(s) {
  if (s === null || s === undefined) return 0;
  s = String(s).trim();
  if (!s) return 0;
  s = s.replace(/\./g, '').replace(',', '.').replace(/[^\d.]/g, '');
  var n = parseFloat(s);
  return isNaN(n) ? 0 : n;
}

/** Converte número → "1.500,00" para exibir na máscara. */
function formatBRLnum(n) {
  return Number(n || 0).toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
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
  // cargo: select para produção, texto para os demais
  if (f.tipoFuncionario === 'PRODUCAO') { $('#cargoProducao').val(f.cargo || ''); }
  else { $('#cargo').val(f.cargo || ''); }
  // valor específico (diária/empreita/unidade)
  if (f.valorEspecifico != null && Number(f.valorEspecifico) > 0) {
    $('#valorEspecifico').val(formatBRLnum(f.valorEspecifico));
  }
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
    $('#valorEspecifico').mask('#.##0,00', { reverse: true });
    $('#salario').mask('#.##0,00', { reverse: true });
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
      url: AGRO_CTX + '/ControllerFuncionario?id=' + editId,
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

  // Mostra/esconde campos conforme o tipo de funcionário
  function ajustarCamposPorTipo() {
    var tipo = $('#tipoFuncionario').val();
    // reset padrão (CLT): cargo texto, datas visíveis, valor escondido
    $('#cargo').removeClass('d-none');
    $('#cargoProducao').addClass('d-none');
    $('#rowDatas').removeClass('d-none');
    $('#rowValor').addClass('d-none');

    if (tipo === 'DIARISTA' || tipo === 'EMPREITA') {
      $('#rowDatas').addClass('d-none');
      $('#rowValor').removeClass('d-none');
      $('#labelValor').text(tipo === 'DIARISTA' ? 'Valor da Diária *' : 'Valor da Empreita *');
    } else if (tipo === 'PRODUCAO') {
      $('#rowDatas').addClass('d-none');
      $('#cargo').addClass('d-none');
      $('#cargoProducao').removeClass('d-none');
    }
  }

  $(document).off('change', '#tipoFuncionario')
    .on('change', '#tipoFuncionario', function () { atualizarBoxTipoInfo(); ajustarCamposPorTipo(); });

  atualizarBoxTipoInfo();
  ajustarCamposPorTipo();

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

  // ======== Verificação em tempo real de duplicidade ========
  window.__existeDup = { cpf: false, matricula: false, usuario: false };

  function atualizarBotaoSalvar() {
    var d = window.__existeDup;
    var bloqueado = d.cpf || d.matricula || d.usuario;
    $('#btnSalvar').prop('disabled', bloqueado)
      .attr('title', bloqueado ? 'Há dados já cadastrados — corrija para continuar' : '');
  }

  function verificarDup(campo, seletor, rotulo) {
    var $el = $(seletor);
    var valor = ($el.val() || '').trim();
    var editId = new URLSearchParams(window.location.search).get('id') || '';
    if (!valor) {
      window.__existeDup[campo] = false;
      $el.removeClass('is-invalid');
      atualizarBotaoSalvar();
      return;
    }
    $.getJSON(AGRO_CTX + '/ControllerFuncionario',
      { existe: campo, valor: valor, excluir: editId })
      .done(function (r) {
        var existe = !!(r && r.existe);
        window.__existeDup[campo] = existe;
        if (existe) {
          $el.addClass('is-invalid').removeClass('is-valid');
          $el.siblings('.invalid-feedback').text(rotulo + ' já cadastrado no sistema.');
        } else {
          $el.removeClass('is-invalid').addClass('is-valid');
        }
        atualizarBotaoSalvar();
      });
  }

  // Checa ENQUANTO digita (evento input) com debounce; e também ao sair do campo (blur)
  var dupTimers = {};
  function agendarDup(campo, seletor, rotulo) {
    clearTimeout(dupTimers[campo]);
    dupTimers[campo] = setTimeout(function () { verificarDup(campo, seletor, rotulo); }, 450);
  }
  [['cpf', '#cpfPf', 'CPF'],
   ['matricula', '#matricula', 'Matrícula'],
   ['usuario', '#usuarioPessoa', 'Usuário']].forEach(function (c) {
    $(document).off('input.dup blur.dup', c[1])
      .on('input.dup', c[1], function () { agendarDup(c[0], c[1], c[2]); })
      .on('blur.dup',  c[1], function () { verificarDup(c[0], c[1], c[2]); });
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

      // Bloqueia se CPF / matrícula / usuário já existirem
      var dup = window.__existeDup || {};
      if (dup.cpf || dup.matricula || dup.usuario) {
        var quais = [];
        if (dup.cpf) quais.push('CPF');
        if (dup.matricula) quais.push('matrícula');
        if (dup.usuario) quais.push('usuário');
        mostrarAlerta('Já cadastrado no sistema: ' + quais.join(', ') + '. Corrija para continuar.',
                      'danger', '#alerta', 5000);
        return;
      }

      if (!ok) {
        if (typeof mostrarAlerta === 'function') {
          mostrarAlerta('Revise os campos obrigatórios.', 'warning', '#alerta', 4000);
        } else {
          $('#alerta').removeClass('d-none').addClass('alert-warning').text('Revise os campos obrigatórios.');
        }
        return;
      }

      // Cargo e valor específico dependem do tipo
      var tipoSel = $('#tipoFuncionario').val();
      var cargoVal = (tipoSel === 'PRODUCAO') ? $('#cargoProducao').val() : $('#cargo').val().trim();
      var valorEsp = parseBRL($('#valorEspecifico').val());

      // Validações específicas por tipo (frontend)
      if (!cargoVal) {
        (tipoSel === 'PRODUCAO' ? $('#cargoProducao') : $('#cargo')).addClass('is-invalid');
        mostrarAlerta(tipoSel === 'PRODUCAO' ? 'Selecione o cargo (Colhedor ou Embalador).' : 'Informe o cargo.',
                      'warning', '#alerta', 4000);
        return;
      }
      if ((tipoSel === 'DIARISTA' || tipoSel === 'EMPREITA') && (!valorEsp || valorEsp <= 0)) {
        $('#valorEspecifico').addClass('is-invalid');
        mostrarAlerta('Informe o valor da ' + (tipoSel === 'DIARISTA' ? 'diária' : 'empreita') + '.',
                      'warning', '#alerta', 4000);
        return;
      }
      // CLT: salário é obrigatório (base para o custo/hora nas atividades do diário)
      if (tipoSel === 'CLT' && (!parseBRL($('#salario').val()) || parseBRL($('#salario').val()) <= 0)) {
        $('#salario').addClass('is-invalid');
        mostrarAlerta('Informe o salário mensal do funcionário CLT.', 'warning', '#alerta', 4000);
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
        tipofuncionario: tipoSel,
        cargofuncionario: cargoVal,
        datainiciofuncionario: (tipoSel === 'CLT') ? converterDataParaISO($('#dataInicio').val().trim()) : null,
        datafimfuncionario: null,
        salariofuncionario: (tipoSel === 'CLT') ? parseBRL($('#salario').val()) : null,
        valorespecifico: valorEsp
      };

      $.ajax({
        url: AGRO_CTX + '/ControllerFuncionario',
        method: 'POST',
        contentType: 'application/json; charset=utf-8',
        dataType: 'json',
        data: JSON.stringify(payload),
        success: function (res) {
          const msg = (res && res.msg) ? res.msg : 'Funcionário salvo com sucesso!';
          if (res && res.ok) {
            sessionStorage.setItem('mensagemAlerta', msg);
            sessionStorage.setItem('tipoAlerta', 'success');
            window.location.href = AGRO_CTX + '/view/admin/Colaboradores.jsp';
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

/******************************************************************************************************/
/* $(document).ready() — blocos movidos dos JSPs com guardas de página                               */
/******************************************************************************************************/

/* ====================================================================================
   Auto-refresh SUAVE de tabelas (padrão do sistema)
   - Nunca usa DataTable().destroy(): preserva busca, ordenação e página do usuário.
   - Pausa quando a aba está em background (document.hidden) ou quando há um modal
     aberto (.modal.show), para não recarregar a lista enquanto o usuário edita.
   - Intervalo padrão: 15s. Use a mesma "chave" por tabela (evita timers duplicados).
   ==================================================================================== */
window.__tabelaTimers = window.__tabelaTimers || {};
function autoRefreshTabela(chave, recarregarFn, intervalMs) {
    if (window.__tabelaTimers[chave]) clearInterval(window.__tabelaTimers[chave]);
    window.__tabelaTimers[chave] = setInterval(function () {
        if (document.hidden) return;                        // aba inativa
        if (document.querySelector('.modal.show')) return;  // modal aberto: não atrapalha edição
        try { recarregarFn(); } catch (e) { /* silencioso */ }
    }, intervalMs || 15000);
}

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

/* ========== view/admin/index.jsp (dashboard admin) — KPIs ========== */
$(document).ready(function () {
    if (!$('#kpiFuncionarios').length) return;

    $.getJSON(CTX + '/ControllerFuncionario?ativos=true', function (lista) {
        $('#kpiFuncionarios').text(Array.isArray(lista) ? lista.length : 0);
    });

    $.getJSON(CTX + '/ControllerParceiro?acao=dados', function (dados) {
        $('#kpiParceiros').text(dados && dados.total != null ? dados.total : 0);
    });

    $.getJSON(CTX + '/ControllerAlimento', function (lista) {
        $('#kpiAlimentos').text(Array.isArray(lista) ? lista.length : 0);
    });

    $.getJSON(CTX + '/ControllerAreaProducao', function (lista) {
        $('#kpiAreas').text(Array.isArray(lista) ? lista.length : 0);
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
    autoRefreshTabela('classificacao', CarregarClassificacao, 15000);
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
    autoRefreshTabela('despesascustos', CarregarDadosDespesasCustos, 15000);

    $('#formCadastro').on('submit', function (e) {
        e.preventDefault();
        let despesa = $('#despesascustos').val().trim();
        let unidade = $('#unidade').val().trim();
        let valor   = $('#valor').val().trim();
        let tipo    = $('#tipo').val().trim();
        let classificacao = $('#classificacao').val();
        if (!despesa || !unidade || !valor || !tipo) {
            mostrarAlerta('Todos os campos são obrigatórios.', 'danger', '#alertModalCadastro');
            return;
        }
        if (!classificacao) {
            mostrarAlerta('Selecione a classificação (Fixo ou Variável).', 'danger', '#alertModalCadastro');
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
        // V28: caixa de colheita / embalagem por alimento e área
        let categoria = $('#dcCategoria').val() || 'GERAL';
        let caixa = {categoria: categoria};
        if (categoria !== 'GERAL') {
            let tipoCaixa = $('#dcTipoCaixa').val();
            if (tipoCaixa === '__OUTRO__') tipoCaixa = $('#dcTipoCaixaOutro').val().trim();
            if (!$('#dcAlimento').val() || !$('#dcArea').val()) {
                mostrarAlerta('Selecione o alimento e a área de produção.', 'danger', '#alertModalCadastro');
                return;
            }
            if (!tipoCaixa) {
                mostrarAlerta('Informe o tipo de caixa/embalagem.', 'danger', '#alertModalCadastro');
                return;
            }
            caixa.idAlimento = $('#dcAlimento').val();
            caixa.idArea = $('#dcArea').val();
            caixa.tipoCaixa = tipoCaixa;
        }
        salvarDespesasCustos(despesa, unidade, valor, tipo, classificacao, caixa);
    });

    carregarSelectsCaixaDespesasCustos();
    $('#dcCategoria, #dcTipoCaixa').on('change', alternarCamposCaixaDespesasCustos);

    $('#formAtualizacao').on('submit', function (e) {
        e.preventDefault();
        let id      = $('#atualizacaoId').val();
        let despesa = $('#atualizacaoDespesa').val().trim();
        let unidade = $('#atualizacaoUnidade').val().trim();
        let valor   = $('#atualizacaoValor').val().trim();
        let tipo    = $('#atualizacaoTipo').val().trim();
        let classificacao = $('#atualizacaoClassificacao').val();
        if (!despesa || !unidade || !valor || !tipo) {
            ExibirAlerta('Erro', 'Todos os campos são obrigatórios.');
            return;
        }
        if (!classificacao) {
            ExibirAlerta('Erro', 'Selecione a classificação (Fixo ou Variável).');
            return;
        }
        atualizarDespesasCustos(id, despesa, unidade, valor, tipo, classificacao);
    });

    $('#modalCadastro').on('show.bs.modal hidden.bs.modal', function () {
        $('#formCadastro')[0].reset();
        $('#alertModalCadastro').removeClass().addClass('alert d-none').empty();
        alternarCamposCaixaDespesasCustos();
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
    autoRefreshTabela('alimento', CarregarAlimento, 15000);

    // Pré-requisito: alimento precisa de ao menos uma Classificação cadastrada.
    window.__classifCount = null;
    $.getJSON(AGRO_CTX + '/ClassificacaoServlet', function (cs) { window.__classifCount = (cs || []).length; });

    $('#modalFormulario').on('show.bs.modal', function (e) {
        if (window.__classifCount === 0) {
            e.preventDefault();
            preReqAlerta('#alerta', 'Para cadastrar um Alimento é preciso ter ao menos uma <strong>Classificação</strong> cadastrada.',
                CTX + '/view/admin/classificacao.jsp', 'Cadastrar Classificação');
            return;
        }
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
    autoRefreshTabela('parceiros', function () { CarregarParceiros(); carregarResumoParceiros(); }, 15000);

    // Abas de filtro por tipo
    $(document).on("click", ".aba-tipo-parceiro", function (e) {
        e.preventDefault();
        filtrarParceirosPorTipo($(this).data("tipo"));
    });

    // Salvar edição do modal
    $("#btnSalvarParceiro").on("click", salvarParceiroModal);
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
            url: AGRO_CTX + '/ControllerParceiro?cnpj=' + cnpj,
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
    autoRefreshTabela('funcionarios', CarregarFuncionarios, 15000);

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
            url: AGRO_CTX + '/ControllerFuncionario',
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
        url: AGRO_CTX + '/ControllerFuncionario',
        method: 'GET', dataType: 'json',
        success: function (lista) {
            dadosFuncionarios = Array.isArray(lista) ? lista : [];
            if (tabelaFuncionarios) {
                tabelaFuncionarios.clear().rows.add(dadosFuncionarios).draw(false);
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
                    language: { url: DT_IDIOMA_URL },
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
    var btnPerfil = '';
    if (tipo === 'CLT') {
        btnPerfil = ' <a href="' + CTX + '/view/admin/perfilCLT.jsp?id=' + id + '"'
          + ' class="btn-acao" style="background:#198754;color:#fff;" title="Perfil CLT">'
          + '<i class="fas fa-id-badge"></i></a>';
    } else if (tipo === 'DIARISTA') {
        btnPerfil = ' <a href="' + CTX + '/view/admin/perfilDiarista.jsp?id=' + id + '"'
          + ' class="btn-acao" style="background:#fd7e14;color:#fff;" title="Perfil Diarista">'
          + '<i class="fas fa-user-clock"></i></a>';
    } else if (tipo === 'EMPREITA') {
        btnPerfil = ' <a href="' + CTX + '/view/admin/perfilEmpreita.jsp?id=' + id + '"'
          + ' class="btn-acao" style="background:#6f42c1;color:#fff;" title="Perfil Empreita">'
          + '<i class="fas fa-hard-hat"></i></a>';
    } else if (tipo === 'PRODUCAO') {
        btnPerfil = ' <a href="' + CTX + '/view/admin/perfilProducao.jsp?id=' + id + '"'
          + ' class="btn-acao" style="background:#0dcaf0;color:#fff;" title="Perfil Produção">'
          + '<i class="fas fa-boxes-stacked"></i></a>';
    }
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
        url: AGRO_CTX + '/ControllerFuncionario',
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
    // P2-22: o nome vem do banco. Antes era concatenado dentro de .html(), o que
    // executava qualquer marcacao gravada no cadastro (XSS armazenado). Agora a
    // marcacao e fixa e o dado entra por .text(), que nunca interpreta HTML.
    $('#textoConfirmacaoSituacao')
        .html('Deseja <strong class="js-acao"></strong> o funcionário <strong class="js-nome"></strong>?')
        .find('.js-acao').text(acao).end()
        .find('.js-nome').text(nome);
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

/******************************************************************************************************/
/* MÓDULO DIARISTA — perfilDiarista.jsp                                                              */
/******************************************************************************************************/

$(document).ready(function () {
    if (!$('#painelFechamentoDiarista').length) return;
    initPerfilDiarista();
});

function initPerfilDiarista() {
    var id = new URLSearchParams(window.location.search).get('id');
    if (!id) { mostrarAlerta('ID do funcionário não informado na URL.', 'danger', '#alerta'); return; }

    var hoje = new Date();
    $('#inputPeriodo').val(hoje.getFullYear() + '-' + String(hoje.getMonth() + 1).padStart(2, '0'));
    $('#presencaData, #valeData').val(hoje.toISOString().substring(0, 10));

    carregarPerfilDiarista(id, $('#inputPeriodo').val());

    $('#btnCarregarPeriodo').on('click', function () { carregarPerfilDiarista(id, $('#inputPeriodo').val()); });
    $('#btnSalvarDiaria').on('click', function () { salvarDiaria(id); });
    $('#btnSalvarPresenca').on('click', function () { salvarPresenca(id); });
    $('#btnSalvarVale').on('click', function () { salvarValeDiarista(id); });
}

function carregarPerfilDiarista(id, periodo) {
    $.ajax({
        url: AGRO_CTX + '/ControllerDiarista?acao=perfil&id=' + id + '&periodo=' + periodo,
        method: 'GET', dataType: 'json',
        success: function (data) {
            renderHeaderDiarista(data);
            renderCardsDiarista(data);
            renderTabelaPresenca(data.presencas || []);
            renderTabelaValesDiarista(data.vales || []);
            renderFechamentoDiarista(data);
            window.__pgCtx = { id: parseInt(id), periodo: periodo, tipo: 'DIARISTA', valor: data.valorLiquido };
            carregarPagamentos();
        },
        error: function () { mostrarAlerta('Erro ao carregar perfil do diarista.', 'danger', '#alerta', 5000); }
    });
}

function renderHeaderDiarista(data) {
    var f = data.funcionario; if (!f) return;
    var v = data.vinculo;
    $('#nomeFunc').text(f.nomePessoa || '');
    $('#cargoMatricula').text(((v ? v.cargo : f.cargo) || '') + ' · Matrícula: ' + (f.matricula || ''));
    $('#infoValorDia').text(formatBRL(data.valorPorDia) + '/dia');

    var totalVinc = (data.vinculos || []).length;
    var periodoTxt = '—';
    if (v) {
        var ini = v.dataAdmissao ? formatDataBR(v.dataAdmissao) : '?';
        var fim = v.dataDesligamento ? formatDataBR(v.dataDesligamento) : 'atual';
        periodoTxt = ini + ' – ' + fim + (totalVinc > 1 ? ' (' + totalVinc + ' vínculos)' : '');
    }
    $('#infoVinculoPeriodo').text(periodoTxt);

    var ativo = (data.vinculos || []).filter(function (x) { return !x.dataDesligamento; })[0];
    var st = ativo ? (ativo.status || 'ATIVO').toUpperCase() : 'DESLIGADO';
    $('#badgeStatus').text(st).attr('class', 'badge fs-6 px-3 py-2 ' +
        (st === 'ATIVO' ? 'bg-success' : st === 'AFASTADO' ? 'bg-warning text-dark' : 'bg-danger'));

    $('#editValorDia').val(data.valorPorDia || 0);
    $('#headerFuncionario').removeClass('d-none');
    window.__idDiarista = f.idPessoa;
}

function renderCardsDiarista(data) {
    $('#cardDiasTrabalhados').text(data.diasTrabalhados || 0);
    $('#cardBruto').text(formatBRL(data.valorBruto || 0));
    $('#cardTotalVales').text(formatBRL(data.totalVales || 0));
    $('#cardLiquido').text(formatBRL(data.valorLiquido || 0));
    $('#cardsResumo').css('display', '').removeClass('d-none');
}

function renderTabelaPresenca(presencas) {
    var tbody = $('#bodyPresenca').empty();
    if (!presencas.length) {
        tbody.append('<tr><td colspan="2" class="text-center text-muted py-3">Nenhum dia trabalhado no período.</td></tr>');
        return;
    }
    presencas.forEach(function (p) {
        tbody.append('<tr><td>' + formatDataBR(p.dataRegistro) + '</td>'
            + '<td class="text-end"><button class="btn btn-outline-danger btn-sm py-0" title="Remover" '
            + 'onclick="excluirPresenca(' + p.idPonto + ')"><i class="fas fa-trash"></i></button></td></tr>');
    });
}

function renderTabelaValesDiarista(vales) {
    var tbody = $('#bodyVales').empty();
    if (!vales.length) {
        tbody.append('<tr><td colspan="4" class="text-center text-muted py-3">Sem vales.</td></tr>');
        return;
    }
    vales.forEach(function (v) {
        var status = v.statusValeStr === 'DESCONTADO'
            ? '<span class="badge bg-secondary">Descontado</span>'
            : '<span class="badge bg-warning text-dark">Pendente</span>';
        var btnDesc = v.statusValeStr === 'PENDENTE'
            ? '<button class="btn btn-outline-success btn-sm py-0 me-1" title="Marcar descontado" '
              + 'onclick="marcarValeDescontadoDiarista(' + v.idVale + ')"><i class="fas fa-check"></i></button>'
            : '';
        tbody.append('<tr><td>' + formatDataBR(v.dataVale) + '</td>'
            + '<td class="text-end fw-bold">' + formatBRL(v.valor) + '</td>'
            + '<td>' + status + '</td>'
            + '<td class="text-nowrap">' + btnDesc
            + '<button class="btn btn-outline-danger btn-sm py-0" title="Excluir" '
            + 'onclick="excluirValeDiarista(' + v.idVale + ')"><i class="fas fa-trash"></i></button></td></tr>');
    });
}

function renderFechamentoDiarista(data) {
    $('#painelFechamentoDiarista').html(
        '<table class="table table-sm mb-0">'
        + '<tr><td>Dias trabalhados</td><td class="text-end fw-bold">' + (data.diasTrabalhados || 0) + '</td></tr>'
        + '<tr><td>Valor por dia</td><td class="text-end">' + formatBRL(data.valorPorDia) + '</td></tr>'
        + '<tr><td>Valor Bruto</td><td class="text-end text-success fw-bold">' + formatBRL(data.valorBruto) + '</td></tr>'
        + '<tr><td>Total Vales</td><td class="text-end text-warning">- ' + formatBRL(data.totalVales) + '</td></tr>'
        + '<tr class="table-success"><td class="fw-bold">Valor Líquido</td>'
        + '<td class="text-end fw-bold fs-5">' + formatBRL(data.valorLiquido) + '</td></tr>'
        + '</table>'
        + '<small class="text-muted">Período: ' + (data.periodo || '') + '</small>'
    );
}

function salvarDiaria(id) {
    var valor = parseFloat($('#editValorDia').val());
    if (isNaN(valor) || valor < 0) { mostrarAlerta('Informe um valor válido.', 'warning', '#alertaDiaria', 3000); return; }
    postDiarista({ acao: 'atualizardiaria', idpessoa: parseInt(id), valorpordia: valor },
        function () { $('#modalDiaria').modal('hide'); carregarPerfilDiarista(id, $('#inputPeriodo').val()); },
        '#alertaDiaria');
}

function salvarPresenca(id) {
    var data = $('#presencaData').val();
    if (!data) { mostrarAlerta('Informe a data.', 'warning', '#alertaPresenca', 3000); return; }
    postDiarista({ acao: 'marcarpresenca', idpessoa: parseInt(id), data: data },
        function () { $('#modalPresenca').modal('hide'); carregarPerfilDiarista(id, $('#inputPeriodo').val()); },
        '#alertaPresenca');
}

function excluirPresenca(idPonto) {
    if (!confirm('Remover este dia trabalhado?')) return;
    postDiarista({ acao: 'removerpresenca', idponto: idPonto },
        function () { carregarPerfilDiarista(window.__idDiarista, $('#inputPeriodo').val()); }, '#alerta');
}

function salvarValeDiarista(id) {
    var data = $('#valeData').val();
    var valor = parseFloat($('#valeValor').val());
    if (!data || isNaN(valor) || valor <= 0) { mostrarAlerta('Informe data e valor válidos.', 'warning', '#alertaVale', 3000); return; }
    postDiarista({ acao: 'registrarvale', idpessoa: parseInt(id), datavale: data, valor: valor,
                   tipovale: $('#valeTipo').val(), descricao: $('#valeDescricao').val() },
        function () { $('#modalVale').modal('hide'); $('#valeValor,#valeDescricao').val('');
                      carregarPerfilDiarista(id, $('#inputPeriodo').val()); },
        '#alertaVale');
}

function excluirValeDiarista(idVale) {
    if (!confirm('Excluir este vale?')) return;
    postDiarista({ acao: 'excluirvale', idvale: idVale },
        function () { carregarPerfilDiarista(window.__idDiarista, $('#inputPeriodo').val()); }, '#alerta');
}

function marcarValeDescontadoDiarista(idVale) {
    postDiarista({ acao: 'marcarvaledescontado', idvale: idVale },
        function () { carregarPerfilDiarista(window.__idDiarista, $('#inputPeriodo').val()); }, '#alerta');
}

function postDiarista(payload, onOk, alertaSel) {
    $.ajax({
        url: AGRO_CTX + '/ControllerDiarista',
        method: 'POST', contentType: 'application/json; charset=utf-8',
        data: JSON.stringify(payload), dataType: 'json',
        success: function (r) {
            if (r.ok) { if (r.msg) mostrarAlerta(r.msg, 'success', '#alerta', 3000); if (onOk) onOk(); }
            else mostrarAlerta(r.msg || 'Falha na operação.', 'danger', alertaSel || '#alerta', 4000);
        },
        error: function () { mostrarAlerta('Erro de conexão.', 'danger', alertaSel || '#alerta', 4000); }
    });
}

/******************************************************************************************************/
/* MÓDULO EMPREITA — perfilEmpreita.jsp                                                              */
/******************************************************************************************************/

$(document).ready(function () {
    if (!$('#painelFechamentoEmpreita').length) return;
    initPerfilEmpreita();
});

function initPerfilEmpreita() {
    var id = new URLSearchParams(window.location.search).get('id');
    if (!id) { mostrarAlerta('ID do funcionário não informado na URL.', 'danger', '#alerta'); return; }

    var hoje = new Date();
    $('#inputPeriodo').val(hoje.getFullYear() + '-' + String(hoje.getMonth() + 1).padStart(2, '0'));
    $('#empreitaData, #valeData').val(hoje.toISOString().substring(0, 10));
    if ($.fn.mask) $('#empreitaValor').mask('#.##0,00', { reverse: true });

    carregarPerfilEmpreita(id, $('#inputPeriodo').val());

    $('#btnCarregarPeriodo').on('click', function () { carregarPerfilEmpreita(id, $('#inputPeriodo').val()); });
    $('#btnNovaEmpreita').on('click', function () { resetModalEmpreita(); });
    $('#btnSalvarEmpreita').on('click', function () { salvarEmpreita(id); });
    $('#btnSalvarVale').on('click', function () { salvarValeEmpreita(id); });
}

function carregarPerfilEmpreita(id, periodo) {
    $.ajax({
        url: AGRO_CTX + '/ControllerEmpreita?acao=perfil&id=' + id + '&periodo=' + periodo,
        method: 'GET', dataType: 'json',
        success: function (data) {
            renderHeaderEmpreita(data);
            renderCardsEmpreita(data);
            renderTabelaEmpreita(data.lancamentos || []);
            renderTabelaValesEmpreita(data.vales || []);
            renderFechamentoEmpreita(data);
            window.__pgCtx = { id: parseInt(id), periodo: periodo, tipo: 'EMPREITA', valor: data.valorLiquido };
            carregarPagamentos();
        },
        error: function () { mostrarAlerta('Erro ao carregar perfil do empreita.', 'danger', '#alerta', 5000); }
    });
}

function renderHeaderEmpreita(data) {
    var f = data.funcionario; if (!f) return;
    var v = data.vinculo;
    $('#nomeFunc').text(f.nomePessoa || '');
    $('#cargoMatricula').text(((v ? v.cargo : f.cargo) || '') + ' · Matrícula: ' + (f.matricula || ''));
    $('#infoValorAcordado').text(formatBRL(data.valorAcordado));

    var totalVinc = (data.vinculos || []).length;
    var periodoTxt = '—';
    if (v) {
        var ini = v.dataAdmissao ? formatDataBR(v.dataAdmissao) : '?';
        var fim = v.dataDesligamento ? formatDataBR(v.dataDesligamento) : 'atual';
        periodoTxt = ini + ' – ' + fim + (totalVinc > 1 ? ' (' + totalVinc + ' vínculos)' : '');
    }
    $('#infoVinculoPeriodo').text(periodoTxt);

    var ativo = (data.vinculos || []).filter(function (x) { return !x.dataDesligamento; })[0];
    var st = ativo ? (ativo.status || 'ATIVO').toUpperCase() : 'DESLIGADO';
    $('#badgeStatus').text(st).attr('class', 'badge fs-6 px-3 py-2 ' +
        (st === 'ATIVO' ? 'bg-success' : st === 'AFASTADO' ? 'bg-warning text-dark' : 'bg-danger'));
    $('#headerFuncionario').removeClass('d-none');
    window.__idEmpreita = f.idPessoa;
}

function renderCardsEmpreita(data) {
    $('#cardQtd').text(data.qtdLancamentos || 0);
    $('#cardBruto').text(formatBRL(data.valorBruto || 0));
    $('#cardTotalVales').text(formatBRL(data.totalVales || 0));
    $('#cardLiquido').text(formatBRL(data.valorLiquido || 0));
    $('#cardsResumo').css('display', '').removeClass('d-none');
}

function renderTabelaEmpreita(lancs) {
    window.__empreitas = lancs || [];
    var tbody = $('#bodyEmpreita').empty();
    if (!lancs.length) {
        tbody.append('<tr><td colspan="4" class="text-center text-muted py-3">Nenhuma empreita no período.</td></tr>');
        return;
    }
    lancs.forEach(function (l) {
        tbody.append('<tr>'
            + '<td>' + formatDataBR(l.dataEmpreita) + '</td>'
            + '<td>' + (l.descricao || '-') + '</td>'
            + '<td class="text-end fw-bold">' + formatBRL(l.valor) + '</td>'
            + '<td class="text-end text-nowrap">'
            + '<button class="btn btn-outline-primary btn-sm py-0 me-1" title="Editar" '
            + 'onclick="editarEmpreita(' + l.idLancamento + ')"><i class="fas fa-pen"></i></button>'
            + '<button class="btn btn-outline-danger btn-sm py-0" title="Remover" '
            + 'onclick="excluirEmpreita(' + l.idLancamento + ')"><i class="fas fa-trash"></i></button>'
            + '</td></tr>');
    });
}

function renderTabelaValesEmpreita(vales) {
    var tbody = $('#bodyVales').empty();
    if (!vales.length) {
        tbody.append('<tr><td colspan="4" class="text-center text-muted py-3">Sem vales.</td></tr>');
        return;
    }
    vales.forEach(function (v) {
        var status = v.statusValeStr === 'DESCONTADO'
            ? '<span class="badge bg-secondary">Descontado</span>'
            : '<span class="badge bg-warning text-dark">Pendente</span>';
        var btnDesc = v.statusValeStr === 'PENDENTE'
            ? '<button class="btn btn-outline-success btn-sm py-0 me-1" title="Marcar descontado" '
              + 'onclick="marcarValeDescontadoEmpreita(' + v.idVale + ')"><i class="fas fa-check"></i></button>'
            : '';
        tbody.append('<tr><td>' + formatDataBR(v.dataVale) + '</td>'
            + '<td class="text-end fw-bold">' + formatBRL(v.valor) + '</td>'
            + '<td>' + status + '</td>'
            + '<td class="text-nowrap">' + btnDesc
            + '<button class="btn btn-outline-danger btn-sm py-0" title="Excluir" '
            + 'onclick="excluirValeEmpreita(' + v.idVale + ')"><i class="fas fa-trash"></i></button></td></tr>');
    });
}

function renderFechamentoEmpreita(data) {
    $('#painelFechamentoEmpreita').html(
        '<table class="table table-sm mb-0">'
        + '<tr><td>Empreitas no mês</td><td class="text-end fw-bold">' + (data.qtdLancamentos || 0) + '</td></tr>'
        + '<tr><td>Valor Bruto</td><td class="text-end text-success fw-bold">' + formatBRL(data.valorBruto) + '</td></tr>'
        + '<tr><td>Total Vales</td><td class="text-end text-warning">- ' + formatBRL(data.totalVales) + '</td></tr>'
        + '<tr class="table-success"><td class="fw-bold">Valor Líquido</td>'
        + '<td class="text-end fw-bold fs-5">' + formatBRL(data.valorLiquido) + '</td></tr>'
        + '</table><small class="text-muted">Período: ' + (data.periodo || '') + '</small>'
    );
}

function resetModalEmpreita() {
    window.__empreitaEditId = null;
    $('#modalEmpreitaTitulo').html('<i class="fas fa-list-check me-2"></i>Nova Empreita');
    $('#empreitaData').val(new Date().toISOString().substring(0, 10));
    $('#empreitaDescricao, #empreitaValor').val('');
    $('#alertaEmpreita').addClass('d-none');
}

function editarEmpreita(idLanc) {
    var l = (window.__empreitas || []).find(function (x) { return x.idLancamento === idLanc; });
    if (!l) { mostrarAlerta('Lançamento não encontrado.', 'warning', '#alerta', 3000); return; }
    window.__empreitaEditId = idLanc;
    $('#modalEmpreitaTitulo').html('<i class="fas fa-pen me-2"></i>Editar Empreita');
    $('#empreitaData').val(l.dataEmpreita);
    $('#empreitaDescricao').val(l.descricao || '');
    $('#empreitaValor').val(formatBRLnum(l.valor));
    $('#alertaEmpreita').addClass('d-none');
    bootstrap.Modal.getOrCreateInstance(document.getElementById('modalEmpreita')).show();
}

function salvarEmpreita(id) {
    var data = $('#empreitaData').val();
    var valor = parseBRL($('#empreitaValor').val());
    if (!data || !valor || valor <= 0) { mostrarAlerta('Informe data e valor válidos.', 'warning', '#alertaEmpreita', 3000); return; }
    var editId = window.__empreitaEditId || null;
    var payload = { acao: editId ? 'editlancamento' : 'addlancamento', idpessoa: parseInt(id),
                    data: data, descricao: $('#empreitaDescricao').val(), valor: valor };
    if (editId) payload.idlancamento = editId;
    postEmpreita(payload, function () { window.__empreitaEditId = null; $('#modalEmpreita').modal('hide');
        carregarPerfilEmpreita(id, $('#inputPeriodo').val()); }, '#alertaEmpreita');
}

function excluirEmpreita(idLanc) {
    if (!confirm('Remover esta empreita?')) return;
    postEmpreita({ acao: 'removelancamento', idlancamento: idLanc },
        function () { carregarPerfilEmpreita(window.__idEmpreita, $('#inputPeriodo').val()); }, '#alerta');
}

function salvarValeEmpreita(id) {
    var data = $('#valeData').val();
    var valor = parseFloat($('#valeValor').val());
    if (!data || isNaN(valor) || valor <= 0) { mostrarAlerta('Informe data e valor válidos.', 'warning', '#alertaVale', 3000); return; }
    postEmpreita({ acao: 'registrarvale', idpessoa: parseInt(id), datavale: data, valor: valor,
                   tipovale: $('#valeTipo').val(), descricao: $('#valeDescricao').val() },
        function () { $('#modalVale').modal('hide'); $('#valeValor,#valeDescricao').val('');
            carregarPerfilEmpreita(id, $('#inputPeriodo').val()); }, '#alertaVale');
}

function excluirValeEmpreita(idVale) {
    if (!confirm('Excluir este vale?')) return;
    postEmpreita({ acao: 'excluirvale', idvale: idVale },
        function () { carregarPerfilEmpreita(window.__idEmpreita, $('#inputPeriodo').val()); }, '#alerta');
}

function marcarValeDescontadoEmpreita(idVale) {
    postEmpreita({ acao: 'marcarvaledescontado', idvale: idVale },
        function () { carregarPerfilEmpreita(window.__idEmpreita, $('#inputPeriodo').val()); }, '#alerta');
}

function postEmpreita(payload, onOk, alertaSel) {
    $.ajax({
        url: AGRO_CTX + '/ControllerEmpreita',
        method: 'POST', contentType: 'application/json; charset=utf-8',
        data: JSON.stringify(payload), dataType: 'json',
        success: function (r) {
            if (r.ok) { if (r.msg) mostrarAlerta(r.msg, 'success', '#alerta', 3000); if (onOk) onOk(); }
            else mostrarAlerta(r.msg || 'Falha na operação.', 'danger', alertaSel || '#alerta', 4000);
        },
        error: function () { mostrarAlerta('Erro de conexão.', 'danger', alertaSel || '#alerta', 4000); }
    });
}

/******************************************************************************************************/
/* MÓDULO PRODUÇÃO — perfilProducao.jsp                                                              */
/******************************************************************************************************/

$(document).ready(function () {
    if (!$('#painelFechamentoProducao').length) return;
    initPerfilProducao();
});

function initPerfilProducao() {
    var id = new URLSearchParams(window.location.search).get('id');
    if (!id) { mostrarAlerta('ID do funcionário não informado na URL.', 'danger', '#alerta'); return; }
    var hoje = new Date();
    $('#inputPeriodo').val(hoje.getFullYear() + '-' + String(hoje.getMonth() + 1).padStart(2, '0'));
    $('#caixaData, #valeData').val(hoje.toISOString().substring(0, 10));

    carregarPerfilProducao(id, $('#inputPeriodo').val());
    $('#btnCarregarPeriodo').on('click', function () { carregarPerfilProducao(id, $('#inputPeriodo').val()); });
    $('#btnNovaCaixa').on('click', function () { resetModalCaixa(); });
    $('#btnSalvarCaixa').on('click', function () { salvarCaixa(id); });
    $('#btnSalvarVale').on('click', function () { salvarValeProducao(id); });
    $(document).on('change', '#caixaTipo', atualizarPrecoCaixaModal)
               .on('input', '#caixaQtd', atualizarPrecoCaixaModal);
}

function carregarPerfilProducao(id, periodo) {
    $.ajax({
        url: AGRO_CTX + '/ControllerProducao?acao=perfil&id=' + id + '&periodo=' + periodo,
        method: 'GET', dataType: 'json',
        success: function (data) {
            window.__idProducao = data.funcionario ? data.funcionario.idPessoa : id;
            window.__ano = data.ano;
            window.__tiposCaixa = data.tiposCaixa || [];
            renderHeaderProducao(data);
            renderCardsProducao(data);
            popularTipoCaixaSelect();
            renderTabelaCaixas(data.lancamentos || []);
            renderTabelaCaixasDiario(data.lancamentosDiario || []);
            renderPrecos(data);
            renderTabelaValesProducao(data.vales || []);
            renderFechamentoProducao(data);
            // valor = (manual + Diário concluído) − vales; composicao é só informativa (mostrada no modal).
            window.__pgCtx = { id: parseInt(id), periodo: periodo, tipo: 'PRODUCAO', valor: data.valorLiquido,
                               composicao: composicaoProducaoTxt(data) };
            carregarPagamentos();
        },
        error: function () { mostrarAlerta('Erro ao carregar perfil de produção.', 'danger', '#alerta', 5000); }
    });
}

function rotuloCaixa(nome) {
    var t = (window.__tiposCaixa || []).find(function (x) { return x.nome === nome; });
    return t ? t.rotulo : nome;
}
function precoCaixa(nome) {
    var t = (window.__tiposCaixa || []).find(function (x) { return x.nome === nome; });
    return t ? Number(t.preco) : 0;
}

function renderHeaderProducao(data) {
    var f = data.funcionario; if (!f) return;
    var v = data.vinculo;
    $('#nomeFunc').text(f.nomePessoa || '');
    $('#cargoMatricula').text('Matrícula: ' + (f.matricula || ''));
    $('#badgeCargo').text(data.cargo || '-');

    var totalVinc = (data.vinculos || []).length;
    var periodoTxt = '—';
    if (v) {
        var ini = v.dataAdmissao ? formatDataBR(v.dataAdmissao) : '?';
        var fim = v.dataDesligamento ? formatDataBR(v.dataDesligamento) : 'atual';
        periodoTxt = ini + ' – ' + fim + (totalVinc > 1 ? ' (' + totalVinc + ' vínculos)' : '');
    }
    $('#infoVinculoPeriodo').text(periodoTxt);

    var ativo = (data.vinculos || []).filter(function (x) { return !x.dataDesligamento; })[0];
    var st = ativo ? (ativo.status || 'ATIVO').toUpperCase() : 'DESLIGADO';
    $('#badgeStatus').text(st).attr('class', 'badge fs-6 px-3 py-2 ' +
        (st === 'ATIVO' ? 'bg-success' : st === 'AFASTADO' ? 'bg-warning text-dark' : 'bg-danger'));
    $('#headerFuncionario').removeClass('d-none');
    $('#precoAno').text('(' + (data.ano || '') + ')');
}

/* Caixas do Diário de Campo (regime PRODUCAO) — só leitura; só diário CONCLUIDA entra no total. */
function totalCaixasProducao(data) {
    return (Number(data.totalCaixas) || 0) + (Number(data.caixasDiario) || 0);
}
function composicaoProducaoTxt(data) {
    var txt = 'Manual ' + formatBRL(data.valorManual || 0) + ' + Diário ' + formatBRL(data.valorDiario || 0)
            + ' = ' + formatBRL(data.valorProducao || 0);
    if (Number(data.totalVales) > 0) txt += ' − Vales ' + formatBRL(data.totalVales) + ' = ' + formatBRL(data.valorLiquido || 0);
    return txt;
}
function escProducao(v) {
    return String(v == null ? '' : v).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}
function renderTabelaCaixasDiario(lancs) {
    var tbody = $('#bodyCaixasDiario').empty();
    if (!lancs.length) {
        tbody.append('<tr><td colspan="8" class="text-center text-muted py-3">Nenhuma caixa lançada no Diário no período.</td></tr>');
        return;
    }
    lancs.forEach(function (l) {
        var areaTalhao = [l.area, l.talhao].filter(Boolean).map(escProducao).join(' / ') || '—';
        var tipo = l.tipoCaixa ? escProducao(l.tipoCaixa.split(', ').map(rotuloCaixa).join(', ')) : '—';
        var num = escProducao(l.numeroDiario || l.idDiario);
        var link = l.idArea
            ? '<a href="' + AGRO_CTX + '/view/admin/DetalheAreaProducao.jsp?id=' + parseInt(l.idArea, 10)
              + '" title="Abrir a área (lista de diários)">' + num + '</a>'
            : num;
        var badge = l.contaNoTotal ? ''
            : ' <span class="badge bg-warning text-dark" title="Diário ainda em andamento: entra no total quando for concluído">'
              + 'Em andamento — fora do total</span>';
        tbody.append('<tr' + (l.contaNoTotal ? '' : ' class="text-muted"') + '>'
            + '<td>' + formatDataBR(l.data) + '</td>'
            + '<td>' + escProducao(l.atividade || '—') + '</td>'
            + '<td>' + areaTalhao + '</td>'
            + '<td>' + tipo + '</td>'
            + '<td class="text-end">' + Number(l.caixas || 0).toLocaleString('pt-BR') + '</td>'
            + '<td class="text-end">' + formatBRL(l.precoPorCaixa) + '</td>'
            + '<td class="text-end fw-bold">' + formatBRL(l.valor) + '</td>'
            + '<td class="text-nowrap">' + link + badge + '</td></tr>');
    });
}
// Mostra a composição do total (Manual + Diário) no modal de Pagamento compartilhado, só no perfil Produção.
$(document).on('shown.bs.modal', '#modalPagamento', function () {
    var ctx = window.__pgCtx || {};
    if (ctx.tipo !== 'PRODUCAO' || !ctx.composicao) return;
    $('#pgPrazoInfo').text(ctx.composicao).removeClass('d-none');
});

function renderCardsProducao(data) {
    $('#cardTotalCaixas').text(totalCaixasProducao(data).toLocaleString('pt-BR'));
    $('#cardValorProducao').text(formatBRL(data.valorProducao || 0));
    $('#cardTotalVales').text(formatBRL(data.totalVales || 0));
    $('#cardLiquido').text(formatBRL(data.valorLiquido || 0));
    $('#cardsResumo').css('display', '').removeClass('d-none');
}

function popularTipoCaixaSelect() {
    var $sel = $('#caixaTipo').empty();
    (window.__tiposCaixa || []).forEach(function (t) {
        $sel.append('<option value="' + t.nome + '">' + t.rotulo + '</option>');
    });
    atualizarPrecoCaixaModal();
}

function atualizarPrecoCaixaModal() {
    var preco = precoCaixa($('#caixaTipo').val());
    var qtd = parseInt($('#caixaQtd').val(), 10) || 0;
    $('#caixaPrecoInfo').text(formatBRL(preco));
    $('#caixaSubtotalInfo').text(formatBRL(preco * qtd));
}

function renderTabelaCaixas(lancs) {
    window.__caixas = lancs || [];
    var tbody = $('#bodyCaixas').empty();
    if (!lancs.length) {
        tbody.append('<tr><td colspan="6" class="text-center text-muted py-3">Nenhuma caixa no período.</td></tr>');
        return;
    }
    lancs.forEach(function (l) {
        tbody.append('<tr>'
            + '<td>' + formatDataBR(l.dataProducao) + '</td>'
            + '<td>' + rotuloCaixa(l.tipoCaixa) + '</td>'
            + '<td class="text-end">' + l.quantidade + '</td>'
            + '<td class="text-end">' + formatBRL(l.precoUnitario) + '</td>'
            + '<td class="text-end fw-bold">' + formatBRL(l.subtotal) + '</td>'
            + '<td class="text-end text-nowrap">'
            + '<button class="btn btn-outline-primary btn-sm py-0 me-1" title="Editar" '
            + 'onclick="editarCaixa(' + l.idProducao + ')"><i class="fas fa-pen"></i></button>'
            + '<button class="btn btn-outline-danger btn-sm py-0" title="Remover" '
            + 'onclick="excluirCaixa(' + l.idProducao + ')"><i class="fas fa-trash"></i></button>'
            + '</td></tr>');
    });
}

function renderPrecos(data) {
    var tbody = $('#bodyPrecos').empty();
    (data.tiposCaixa || []).forEach(function (t) {
        tbody.append('<tr>'
            + '<td>' + t.rotulo + '</td>'
            + '<td><input type="number" class="form-control form-control-sm precoInput" '
            + 'data-tipo="' + t.nome + '" min="0" step="0.01" value="' + Number(t.preco).toFixed(2) + '"></td>'
            + '<td><select class="form-select form-select-sm escopoSel">'
            + '<option value="geral">Geral</option>'
            + '<option value="func">Só este func.</option></select></td>'
            + '<td><button class="btn btn-primary btn-sm py-0" onclick="salvarPreco(this)">Salvar</button></td>'
            + '</tr>');
    });
}

function salvarPreco(btn) {
    var $tr = $(btn).closest('tr');
    var tipo = $tr.find('.precoInput').data('tipo');
    var preco = parseFloat($tr.find('.precoInput').val()) || 0;
    var escopo = $tr.find('.escopoSel').val();
    var payload = { acao: 'setpreco', tipocaixa: tipo, ano: window.__ano, preco: preco };
    if (escopo === 'func') payload.idpessoa = window.__idProducao;
    postProducao(payload, function () {
        carregarPerfilProducao(window.__idProducao, $('#inputPeriodo').val());
    }, '#alertaPreco');
}

function resetModalCaixa() {
    window.__caixaEditId = null;
    $('#modalCaixaTitulo').html('<i class="fas fa-box me-2"></i>Registrar Caixas');
    $('#caixaData').val(new Date().toISOString().substring(0, 10));
    $('#caixaQtd').val('');
    $('#alertaCaixa').addClass('d-none');
    atualizarPrecoCaixaModal();
}

function editarCaixa(idProd) {
    var l = (window.__caixas || []).find(function (x) { return x.idProducao === idProd; });
    if (!l) return;
    window.__caixaEditId = idProd;
    $('#modalCaixaTitulo').html('<i class="fas fa-pen me-2"></i>Editar Caixas');
    $('#caixaData').val(l.dataProducao);
    $('#caixaTipo').val(l.tipoCaixa);
    $('#caixaQtd').val(l.quantidade);
    atualizarPrecoCaixaModal();
    $('#alertaCaixa').addClass('d-none');
    bootstrap.Modal.getOrCreateInstance(document.getElementById('modalCaixa')).show();
}

function salvarCaixa(id) {
    var data = $('#caixaData').val();
    var tipo = $('#caixaTipo').val();
    var qtd = parseInt($('#caixaQtd').val(), 10);
    if (!data || !tipo || !qtd || qtd <= 0) { mostrarAlerta('Informe data, tipo e quantidade.', 'warning', '#alertaCaixa', 3000); return; }
    var editId = window.__caixaEditId || null;
    var payload = { acao: editId ? 'editcaixa' : 'addcaixa', idpessoa: parseInt(id),
                    data: data, tipocaixa: tipo, quantidade: qtd };
    if (editId) payload.idproducao = editId;
    postProducao(payload, function () { window.__caixaEditId = null; $('#modalCaixa').modal('hide');
        carregarPerfilProducao(id, $('#inputPeriodo').val()); }, '#alertaCaixa');
}

function excluirCaixa(idProd) {
    if (!confirm('Remover este lançamento de caixas?')) return;
    postProducao({ acao: 'removecaixa', idproducao: idProd },
        function () { carregarPerfilProducao(window.__idProducao, $('#inputPeriodo').val()); }, '#alerta');
}

function renderTabelaValesProducao(vales) {
    var tbody = $('#bodyVales').empty();
    if (!vales.length) { tbody.append('<tr><td colspan="4" class="text-center text-muted py-3">Sem vales.</td></tr>'); return; }
    vales.forEach(function (v) {
        var status = v.statusValeStr === 'DESCONTADO'
            ? '<span class="badge bg-secondary">Descontado</span>'
            : '<span class="badge bg-warning text-dark">Pendente</span>';
        var btnDesc = v.statusValeStr === 'PENDENTE'
            ? '<button class="btn btn-outline-success btn-sm py-0 me-1" title="Marcar descontado" '
              + 'onclick="marcarValeDescontadoProducao(' + v.idVale + ')"><i class="fas fa-check"></i></button>' : '';
        tbody.append('<tr><td>' + formatDataBR(v.dataVale) + '</td>'
            + '<td class="text-end fw-bold">' + formatBRL(v.valor) + '</td>'
            + '<td>' + status + '</td>'
            + '<td class="text-nowrap">' + btnDesc
            + '<button class="btn btn-outline-danger btn-sm py-0" title="Excluir" '
            + 'onclick="excluirValeProducao(' + v.idVale + ')"><i class="fas fa-trash"></i></button></td></tr>');
    });
}

function renderFechamentoProducao(data) {
    $('#painelFechamentoProducao').html(
        '<table class="table table-sm mb-0">'
        + '<tr><td>Total de caixas</td><td class="text-end fw-bold">' + totalCaixasProducao(data).toLocaleString('pt-BR') + '</td></tr>'
        + '<tr><td class="ps-3 small text-muted">Manual</td><td class="text-end small">' + formatBRL(data.valorManual) + '</td></tr>'
        + '<tr><td class="ps-3 small text-muted">Diário de Campo (concluído)</td><td class="text-end small">' + formatBRL(data.valorDiario) + '</td></tr>'
        + '<tr><td>Valor Produção</td><td class="text-end text-success fw-bold">' + formatBRL(data.valorProducao) + '</td></tr>'
        + '<tr><td>Total Vales</td><td class="text-end text-warning">- ' + formatBRL(data.totalVales) + '</td></tr>'
        + '<tr class="table-success"><td class="fw-bold">Valor Líquido</td>'
        + '<td class="text-end fw-bold fs-5">' + formatBRL(data.valorLiquido) + '</td></tr>'
        + '</table><small class="text-muted">Período: ' + (data.periodo || '') + '</small>'
    );
}

function salvarValeProducao(id) {
    var data = $('#valeData').val();
    var valor = parseFloat($('#valeValor').val());
    if (!data || isNaN(valor) || valor <= 0) { mostrarAlerta('Informe data e valor válidos.', 'warning', '#alertaVale', 3000); return; }
    postProducao({ acao: 'registrarvale', idpessoa: parseInt(id), datavale: data, valor: valor,
                   tipovale: $('#valeTipo').val(), descricao: $('#valeDescricao').val() },
        function () { $('#modalVale').modal('hide'); $('#valeValor,#valeDescricao').val('');
            carregarPerfilProducao(id, $('#inputPeriodo').val()); }, '#alertaVale');
}
function excluirValeProducao(idVale) {
    if (!confirm('Excluir este vale?')) return;
    postProducao({ acao: 'excluirvale', idvale: idVale },
        function () { carregarPerfilProducao(window.__idProducao, $('#inputPeriodo').val()); }, '#alerta');
}
function marcarValeDescontadoProducao(idVale) {
    postProducao({ acao: 'marcarvaledescontado', idvale: idVale },
        function () { carregarPerfilProducao(window.__idProducao, $('#inputPeriodo').val()); }, '#alerta');
}

function postProducao(payload, onOk, alertaSel) {
    $.ajax({
        url: AGRO_CTX + '/ControllerProducao',
        method: 'POST', contentType: 'application/json; charset=utf-8',
        data: JSON.stringify(payload), dataType: 'json',
        success: function (r) {
            if (r.ok) { if (r.msg) mostrarAlerta(r.msg, 'success', '#alerta', 3000); if (onOk) onOk(); }
            else if (r.confirmar) {
                // Já há caixas do Diário de Campo nesta data: só grava se o usuário confirmar.
                if (confirm(r.msg + '\n\nLançar mesmo assim?'))
                    postProducao($.extend({}, payload, { confirmardiario: true }), onOk, alertaSel);
            }
            else mostrarAlerta(r.msg || 'Falha na operação.', 'danger', alertaSel || '#alerta', 4000);
        },
        error: function () { mostrarAlerta('Erro de conexão.', 'danger', alertaSel || '#alerta', 4000); }
    });
}

/******************************************************************************************************/
/* PAGAMENTOS — compartilhado por todos os perfis (modalPagamento.jsp)                                */
/******************************************************************************************************/

/* Cada perfil define window.__pgCtx = {id, periodo, tipo, valor} e chama carregarPagamentos(). */

$(document).on('change', '#pgForma', ajustarCamposPagamento);
$(document).on('click', '#btnSalvarPagamento', salvarPagamento);
$(document).on('click', '#btnGerarPagamento', abrirModalPagamento);

function ajustarCamposPagamento() {
    var f = $('#pgForma').val();
    $('#pgRowPix').toggleClass('d-none', f !== 'PIX');
    $('#pgRowBanco').toggleClass('d-none', f !== 'TRANSFERENCIA');
}

function abrirModalPagamento() {
    var ctx = window.__pgCtx || {};
    if (!ctx.id) { mostrarAlerta('Carregue um período antes.', 'warning', '#alerta', 3000); return; }
    $('#pgValor').val(formatBRLnum(ctx.valor || 0));
    $('#pgData').val(new Date().toISOString().substring(0, 10));
    $('#pgForma').val('PIX'); ajustarCamposPagamento();
    $('#pgBanco,#pgAgencia,#pgConta,#pgChavePix,#pgObs').val('');
    $('#alertaPagamento').addClass('d-none');
    if (ctx.tipo === 'CLT' && ctx.periodo) {
        $.getJSON(AGRO_CTX + '/ControllerPagamento', { acao: 'prazoclt', periodo: ctx.periodo })
            .done(function (r) {
                if (r && r.ok) $('#pgPrazoInfo').text('Prazo CLT: pagar até ' + formatDataBR(r.prazo) +
                    ' (5º dia útil do mês seguinte).').removeClass('d-none');
            });
    } else { $('#pgPrazoInfo').addClass('d-none'); }
    bootstrap.Modal.getOrCreateInstance(document.getElementById('modalPagamento')).show();
}

function salvarPagamento() {
    var ctx = window.__pgCtx || {};
    var valor = parseBRL($('#pgValor').val());
    var forma = $('#pgForma').val();
    if (!valor || valor <= 0) { mostrarAlerta('Informe o valor.', 'warning', '#alertaPagamento', 3000); return; }
    if (forma === 'TRANSFERENCIA' && (!$('#pgBanco').val() || !$('#pgAgencia').val() || !$('#pgConta').val())) {
        mostrarAlerta('Para transferência informe banco, agência e conta.', 'warning', '#alertaPagamento', 4000); return;
    }
    if (forma === 'PIX' && !$('#pgChavePix').val()) {
        mostrarAlerta('Para PIX informe a chave.', 'warning', '#alertaPagamento', 4000); return;
    }
    var payload = { acao: 'registrar', idpessoa: ctx.id, periodo: ctx.periodo, tipofuncionario: ctx.tipo,
        valor: valor, forma: forma, datapagamento: $('#pgData').val(), observacao: $('#pgObs').val(),
        banco: $('#pgBanco').val(), agencia: $('#pgAgencia').val(), conta: $('#pgConta').val(),
        chavepix: $('#pgChavePix').val() };
    $.ajax({
        url: AGRO_CTX + '/ControllerPagamento', method: 'POST',
        contentType: 'application/json; charset=utf-8', data: JSON.stringify(payload), dataType: 'json',
        success: function (r) {
            if (r.ok) { $('#modalPagamento').modal('hide'); mostrarAlerta(r.msg, 'success', '#alerta', 3000); carregarPagamentos(); }
            else mostrarAlerta(r.msg, 'danger', '#alertaPagamento', 4000);
        },
        error: function () { mostrarAlerta('Erro de conexão.', 'danger', '#alertaPagamento', 4000); }
    });
}

function carregarPagamentos() {
    var ctx = window.__pgCtx || {};
    if (!ctx.id || !$('#bodyPagamentos').length) return;
    $.getJSON(AGRO_CTX + '/ControllerPagamento', { acao: 'listar', id: ctx.id, periodo: ctx.periodo })
        .done(function (lista) {
            var tbody = $('#bodyPagamentos').empty();
            if (!lista || !lista.length) {
                tbody.append('<tr><td colspan="4" class="text-center text-muted py-2">Sem pagamentos no período.</td></tr>');
                return;
            }
            lista.forEach(function (p) {
                var det = p.formaPagamento === 'PIX' ? ('PIX · ' + (p.chavePix || ''))
                    : p.formaPagamento === 'TRANSFERENCIA'
                        ? ((p.banco || '') + ' · Ag ' + (p.agencia || '') + ' · C/C ' + (p.conta || ''))
                        : (p.formaPagamento === 'DINHEIRO' ? 'Dinheiro' : 'Cheque');
                tbody.append('<tr><td>' + formatDataBR(p.dataPagamento) + '</td>'
                    + '<td class="text-end fw-bold">' + formatBRL(p.valor) + '</td>'
                    + '<td class="small">' + det + '</td>'
                    + '<td><button class="btn btn-outline-danger btn-sm py-0" title="Excluir" '
                    + 'onclick="excluirPagamento(' + p.idPagamento + ')"><i class="fas fa-trash"></i></button></td></tr>');
            });
        });
}

function excluirPagamento(id) {
    if (!confirm('Excluir este pagamento?')) return;
    $.ajax({
        url: AGRO_CTX + '/ControllerPagamento', method: 'POST',
        contentType: 'application/json; charset=utf-8',
        data: JSON.stringify({ acao: 'excluir', idpagamento: id }), dataType: 'json',
        success: function () { carregarPagamentos(); }
    });
}

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
        url: AGRO_CTX + '/ControllerCLT?acao=perfil&id=' + idFuncionario + '&periodo=' + periodo,
        method: 'GET',
        dataType: 'json',
        success: function (data) {
            renderHeaderCLT(data);
            renderCardsCLT(data);
            renderTabelaPonto(data.pontos || []);
            renderTabelaFaltas(data.faltas || []);
            renderTabelaVales(data.vales || []);
            if (data.fechamento) renderFechamento(data.fechamento);
            window.__pgCtx = { id: parseInt(idFuncionario), periodo: periodo, tipo: 'CLT',
                valor: data.fechamento ? data.fechamento.salarioLiquido : 0 };
            carregarPagamentos();
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
        + ((fc.valorProducao && fc.valorProducao > 0)
            ? '<tr><td>Produção (dias em modo produção)</td><td class="text-end text-info">+ ' + formatBRL(fc.valorProducao) + '</td></tr>' : '')
        + ((fc.valorEmpreita && fc.valorEmpreita > 0)
            ? '<tr><td>Empreita (dias em modo empreita)</td><td class="text-end text-info">+ ' + formatBRL(fc.valorEmpreita) + '</td></tr>' : '')
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
        url: AGRO_CTX + '/ControllerPontoEletronico',
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
        url: AGRO_CTX + '/ControllerPontoEletronico',
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
        url: AGRO_CTX + '/ControllerCLT',
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
        url: AGRO_CTX + '/ControllerCLT',
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
        url: AGRO_CTX + '/ControllerCLT',
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
        url: AGRO_CTX + '/ControllerCLT',
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
        url: AGRO_CTX + '/ControllerCLT',
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
        url: AGRO_CTX + '/ControllerCLT',
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
        url: AGRO_CTX + '/ControllerCLT',
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
        url: AGRO_CTX + '/ControllerCLT',
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
        url: AGRO_CTX + '/ControllerCLT',
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
        url: AGRO_CTX + '/ControllerCLT',
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
/* MÓDULO ÁREA DE PRODUÇÃO — lista, cadastro (com quadras), atualização e detalhe                     */
/******************************************************************************************************/

/** Lê um parâmetro da query string da URL. */
function apGetParam(nome) { return new URLSearchParams(window.location.search).get(nome); }

/** Cache dos alimentos (tipo de planta) para os selects de quadra. */
if (typeof apAlimentosCache === 'undefined') { var apAlimentosCache = []; }
function apOptionsAlimento() {
    var s = '<option value="">Selecione</option>';
    apAlimentosCache.forEach(function (a) { s += '<option value="' + a.id + '">' + a.nome + '</option>'; });
    return s;
}

/* ---------------- LISTA (areaproducao.jsp) ---------------- */
if (typeof areaParaDesativar === 'undefined') { var areaParaDesativar = null; var areaSituacaoAtual = null; }

$(document).ready(function () {
    if (!$('#tabelaAreas').length) return;

    var msg = sessionStorage.getItem('mensagemAlerta');
    if (msg) {
        mostrarAlerta(msg, sessionStorage.getItem('tipoAlerta') || 'success', '#alertPage');
        sessionStorage.removeItem('mensagemAlerta'); sessionStorage.removeItem('tipoAlerta');
    }
    $('#tabelaAreas').DataTable({ language: { url: DT_IDIOMA_URL } });
    apCarregarLista();
    autoRefreshTabela('areas', apCarregarLista, 15000);
});

function apCarregarLista() {
    var tabela = $('#tabelaAreas').DataTable();
    $.get(AGRO_CTX + '/ControllerAreaProducao', function (dados) {
        tabela.clear();  // limpa só quando os dados chegam (sem flash de tabela vazia)
        dados.forEach(function (a) {
            var badge = a.situacao ? '<span class="badge bg-success">Ativa</span>' : '<span class="badge bg-secondary">Inativa</span>';
            var nome = (a.PropriedadeAreaProducao || '').replace(/'/g, "\\'");
            var acoes =
                '<a class="btn-acao btn-acao-editar" title="Atualizar" href="' + CTX + '/view/admin/AtualizarAreaProducao.jsp?id=' + a.idAreaProducao + '"><i class="fas fa-pen-to-square"></i></a> ' +
                '<button class="btn-acao ' + (a.situacao ? 'btn-acao-desativar' : 'btn-acao-ativar') + '" title="' + (a.situacao ? 'Desativar' : 'Ativar') + '" onclick="apAbrirDesativar(' + a.idAreaProducao + ',' + a.situacao + ',\'' + nome + '\')"><i class="fas ' + (a.situacao ? 'fa-ban' : 'fa-circle-check') + '"></i></button> ' +
                '<a class="btn-acao" title="Área (detalhe)" href="' + CTX + '/view/admin/DetalheAreaProducao.jsp?id=' + a.idAreaProducao + '"><i class="fas fa-map-location-dot"></i></a>';
            tabela.row.add([
                a.PropriedadeAreaProducao, a.ProprietarioAreaProducao, a.SiglasAreaProducao,
                a.QuantidadeTotalPlantasAreaProducao, badge, a.cep || '', acoes
            ]).draw(false);
        });
    });
}

function apAbrirDesativar(id, situacao, nome) {
    areaParaDesativar = id; areaSituacaoAtual = situacao;
    var desativar = (situacao === true || situacao === 'true');
    $('#modalDesativarAreaLabel').text(desativar ? 'Confirmar Desativação' : 'Confirmar Ativação');
    $('#mensagemDesativarArea').text((desativar ? 'Desativar' : 'Ativar') + ' a área "' + nome + '"?');
    $('#btnConfirmarDesativarArea').removeClass('btn-danger btn-success').addClass(desativar ? 'btn-danger' : 'btn-success').text(desativar ? 'Desativar' : 'Ativar');
    $('#modalDesativarArea').modal('show');
}

$(document).on('click', '#btnConfirmarDesativarArea', function () {
    if (areaParaDesativar == null) return;
    $.ajax({
        url: AGRO_CTX + '/ControllerAreaProducao', method: 'POST', contentType: 'application/json',
        data: JSON.stringify({ acao: 'desativar', idareaproducao: areaParaDesativar, situacao: areaSituacaoAtual }),
        success: function (res) {
            $('#modalDesativarArea').modal('hide');
            mostrarAlerta(res.msg, res.ok ? 'info' : 'danger', '#alertPage');
            apCarregarLista();
        },
        error: function () { $('#modalDesativarArea').modal('hide'); mostrarAlerta('Erro ao alterar situação.', 'danger', '#alertPage'); }
    });
});

/* ---------------- CADASTRO (CadastroAreaProducao.jsp) ---------------- */
$(document).ready(function () {
    if (!$('#formCadastroArea').length) return;
    $('#cepArea').mask('00000-000');
    $('#cepArea').on('blur', function () { preencherEnderecoViaCep($(this).val()); });

    $.getJSON(AGRO_CTX + '/ControllerAreaProducao?alimentos=1', function (lista) {
        apAlimentosCache = lista;
        apAddQuadraRow();  // começa com uma linha
    });
    $('#btnAddQuadraRow').on('click', apAddQuadraRow);
    $('#btnSalvarArea').on('click', apSalvarCadastro);
    $('#quadrasBody').on('input', '.q-plantas', apAtualizarSoma);
    $('#quadrasBody').on('click', '.q-remove', function () { $(this).closest('tr').remove(); apAtualizarSoma(); });
});

function apAddQuadraRow() {
    var tr = '<tr>' +
        '<td><input type="text" class="form-control form-control-sm q-nome" maxlength="60" placeholder="Ex.: Quadra 1"></td>' +
        '<td><input type="number" class="form-control form-control-sm q-plantas" min="0" value="0"></td>' +
        '<td><input type="number" class="form-control form-control-sm q-area" min="0" step="0.01" value="0"></td>' +
        '<td><select class="form-select form-select-sm q-alimento">' + apOptionsAlimento() + '</select></td>' +
        '<td class="text-center"><button type="button" class="btn btn-sm btn-outline-danger q-remove"><i class="fas fa-trash"></i></button></td>' +
        '</tr>';
    $('#quadrasBody').append(tr);
}
function apAtualizarSoma() {
    var soma = 0;
    $('#quadrasBody .q-plantas').each(function () { soma += parseInt($(this).val()) || 0; });
    $('#quantidadetotalplantasareaproducao').val(soma);
}
function apSalvarCadastro() {
    var quadras = [];
    $('#quadrasBody tr').each(function () {
        var nome = $(this).find('.q-nome').val().trim();
        if (!nome) return;
        quadras.push({
            nomeQuadra: nome,
            numeroPlantas: parseInt($(this).find('.q-plantas').val()) || 0,
            areaHa: parseFloat($(this).find('.q-area').val()) || 0,
            idAlimento: parseInt($(this).find('.q-alimento').val()) || 0
        });
    });
    var payload = {
        acao: 'create',
        propriedadeareaproducao: $('#propriedadeareaproducao').val().trim(),
        proprietarioareaproducao: $('#proprietarioareaproducao').val().trim(),
        siglasareaproducao: $('#siglasareaproducao').val().trim(),
        cep: $('#cepArea').val().trim(),
        numero: parseInt($('#numeroArea').val()) || 0,
        complemento: $('#complementoArea').val().trim(),
        quadras: quadras
    };
    if (!payload.propriedadeareaproducao || !payload.proprietarioareaproducao || !payload.siglasareaproducao) {
        mostrarAlerta('Propriedade, proprietário e sigla são obrigatórios.', 'danger', '#alertFormArea'); return;
    }
    if (quadras.length === 0) { mostrarAlerta('Cadastre ao menos uma quadra (com nome).', 'danger', '#alertFormArea'); return; }
    $.ajax({
        url: AGRO_CTX + '/ControllerAreaProducao', method: 'POST', contentType: 'application/json',
        data: JSON.stringify(payload),
        success: function (res) {
            if (res.ok) {
                sessionStorage.setItem('mensagemAlerta', res.msg); sessionStorage.setItem('tipoAlerta', 'success');
                window.location.href = CTX + '/view/admin/areaproducao.jsp';
            } else { mostrarAlerta(res.msg, 'danger', '#alertFormArea'); }
        },
        error: function (xhr) { var m = 'Erro ao cadastrar.'; try { m = JSON.parse(xhr.responseText).msg || m; } catch (e) {} mostrarAlerta(m, 'danger', '#alertFormArea'); }
    });
}

/* ---------------- ATUALIZAÇÃO (AtualizarAreaProducao.jsp) ---------------- */
$(document).ready(function () {
    if (!$('#formAtualizarArea').length) return;
    var id = apGetParam('id');
    $('#areaId').val(id);
    $('#cepAreaEdit').mask('00000-000');
    $('#cepAreaEdit').on('blur', function () { preencherEnderecoViaCep($(this).val()); });

    $.getJSON(AGRO_CTX + '/ControllerAreaProducao?id=' + id, function (a) {
        $('#propEdit').val(a.PropriedadeAreaProducao);
        $('#proprietEdit').val(a.ProprietarioAreaProducao);
        $('#siglaEdit').val(a.SiglasAreaProducao);
        $('#qtdEdit').val(a.QuantidadeTotalPlantasAreaProducao);
        $('#cepAreaEdit').val(a.cep);
        preencherEnderecoViaCep(a.cep);
        $('#numeroAreaEdit').val(a.numero);
        $('#complementoAreaEdit').val(a.complemento);
    });
    apCarregarAlimentos($('#addQuadraAlimento'));
    apCarregarQuadrasEdit(id);

    $('#btnAtualizarArea').on('click', apAtualizarArea);
    $('#btnAddQuadraEdit').on('click', apAddQuadraEdit);
});

/** Popula um select de tipo de planta (mantém a 1ª opção). */
function apCarregarAlimentos($sel) {
    $.getJSON(AGRO_CTX + '/ControllerAreaProducao?alimentos=1', function (lista) {
        $sel.find('option:not(:first)').remove();
        lista.forEach(function (a) { $sel.append('<option value="' + a.id + '">' + a.nome + '</option>'); });
    });
}
function apCarregarQuadrasEdit(id) {
    // Carrega TODAS as quadras (ativas e inativas) — a quadra desativada não é
    // excluída; permanece visível com o status e pode ser reativada.
    $.getJSON(AGRO_CTX + '/ControllerAreaProducao?quadras=' + id + '&ativas=false', function (lista) {
        var $b = $('#quadrasEditBody').empty();
        if (!lista.length) { $b.append('<tr><td colspan="6" class="text-center text-muted py-2">Sem quadras.</td></tr>'); return; }
        lista.forEach(function (q) {
            var badge = q.ativa ? '<span class="badge bg-success">Ativa</span>' : '<span class="badge bg-secondary">Inativa</span>';
            var acao = q.ativa
                ? '<button class="btn btn-sm btn-outline-danger" onclick="apDesativarQuadra(' + q.idQuadra + ')"><i class="fas fa-ban me-1"></i>Desativar</button>'
                : '<button class="btn btn-sm btn-outline-success" onclick="apAtivarQuadra(' + q.idQuadra + ')"><i class="fas fa-circle-check me-1"></i>Reativar</button>';
            $b.append('<tr class="' + (q.ativa ? '' : 'table-light text-muted') + '"><td>' + q.nomeQuadra +
                '</td><td class="text-end">' + q.numeroPlantas + '</td><td class="text-end">' + (Number(q.areaHa || 0).toLocaleString('pt-BR')) +
                '</td><td>' + (q.alimentoNome || '—') + '</td><td>' + badge + '</td><td class="text-center">' + acao + '</td></tr>');
        });
    });
}
function apAtualizarArea() {
    var payload = {
        acao: 'update', idareaproducao: parseInt($('#areaId').val()),
        propriedadeareaproducao: $('#propEdit').val().trim(),
        proprietarioareaproducao: $('#proprietEdit').val().trim(),
        siglasareaproducao: $('#siglaEdit').val().trim(),
        cep: $('#cepAreaEdit').val().trim(),
        numero: parseInt($('#numeroAreaEdit').val()) || 0,
        complemento: $('#complementoAreaEdit').val().trim()
    };
    if (!payload.propriedadeareaproducao || !payload.proprietarioareaproducao || !payload.siglasareaproducao) {
        mostrarAlerta('Propriedade, proprietário e sigla são obrigatórios.', 'danger', '#alertFormAreaEdit'); return;
    }
    $.ajax({
        url: AGRO_CTX + '/ControllerAreaProducao', method: 'POST', contentType: 'application/json', data: JSON.stringify(payload),
        success: function (res) {
            if (res.ok) {
                sessionStorage.setItem('mensagemAlerta', res.msg); sessionStorage.setItem('tipoAlerta', 'success');
                window.location.href = CTX + '/view/admin/areaproducao.jsp';
            } else { mostrarAlerta(res.msg, 'danger', '#alertFormAreaEdit'); }
        },
        error: function (xhr) { var m = 'Erro ao atualizar.'; try { m = JSON.parse(xhr.responseText).msg || m; } catch (e) {} mostrarAlerta(m, 'danger', '#alertFormAreaEdit'); }
    });
}
function apAddQuadraEdit() {
    var id = parseInt($('#areaId').val());
    var nome = $('#addQuadraNome').val().trim();
    if (!nome) { mostrarAlerta('Informe o nome da quadra.', 'danger', '#alertQuadra'); return; }
    var payload = { acao: 'addquadra', idareaproducao: id, nomeQuadra: nome, numeroPlantas: parseInt($('#addQuadraPlantas').val()) || 0, areaHa: parseFloat($('#addQuadraArea').val()) || 0, idAlimento: parseInt($('#addQuadraAlimento').val()) || 0 };
    $.ajax({
        url: AGRO_CTX + '/ControllerAreaProducao', method: 'POST', contentType: 'application/json', data: JSON.stringify(payload),
        success: function (res) { if (res.ok) { window.location.reload(); } else { mostrarAlerta(res.msg, 'danger', '#alertQuadra'); } },
        error: function () { mostrarAlerta('Erro ao adicionar quadra.', 'danger', '#alertQuadra'); }
    });
}
function apDesativarQuadra(idQuadra) {
    if (!confirm('Desativar esta quadra? Os dados são preservados e a quadra pode ser reativada.')) return;
    $.ajax({
        url: AGRO_CTX + '/ControllerAreaProducao', method: 'POST', contentType: 'application/json',
        data: JSON.stringify({ acao: 'desativarquadra', idquadra: idQuadra }),
        success: function () { window.location.reload(); },
        error: function () { mostrarAlerta('Erro ao desativar quadra.', 'danger', '#alertQuadra'); }
    });
}
function apAtivarQuadra(idQuadra) {
    $.ajax({
        url: AGRO_CTX + '/ControllerAreaProducao', method: 'POST', contentType: 'application/json',
        data: JSON.stringify({ acao: 'ativarquadra', idquadra: idQuadra }),
        success: function () { window.location.reload(); },
        error: function () { mostrarAlerta('Erro ao reativar quadra.', 'danger', '#alertQuadra'); }
    });
}

/* ---------------- DETALHE (DetalheAreaProducao.jsp) ---------------- */
$(document).ready(function () {
    if (!$('#detalheArea').length) return;
    var id = apGetParam('id');
    $.getJSON(AGRO_CTX + '/ControllerAreaProducao?id=' + id, function (a) {
        $('#detProp').text(a.PropriedadeAreaProducao);
        $('#detProp2').text(a.PropriedadeAreaProducao || '');
        $('#detPropriet').text(a.ProprietarioAreaProducao);
        $('#detSigla').text(a.SiglasAreaProducao);
        $('#detQtd').text(a.QuantidadeTotalPlantasAreaProducao);
        $('#detCep').text(a.cep || '—');
        $('#detNumero').text(a.numero || '—');
        $('#detComplemento').text(a.complemento || '—');
        $('#detSituacao').html(a.situacao ? '<span class="badge bg-success">Ativa</span>' : '<span class="badge bg-secondary">Inativa</span>');
    });
    $.getJSON(AGRO_CTX + '/ControllerAreaProducao?quadras=' + id + '&ativas=false', function (lista) {
        var $b = $('#detQuadrasBody').empty();
        if (!lista.length) { $b.append('<tr><td colspan="4" class="text-center text-muted py-2">Sem quadras.</td></tr>'); return; }
        lista.forEach(function (q) {
            $b.append('<tr><td>' + q.nomeQuadra + '</td><td class="text-end">' + q.numeroPlantas + '</td><td>' + (q.alimentoNome || '—') + '</td><td>' +
                (q.ativa ? '<span class="badge bg-success">Ativa</span>' : '<span class="badge bg-secondary">Inativa</span>') + '</td></tr>');
        });
    });
    dcInit(id);   // Diário de Campo escopado a esta área
    // Vindo do Módulo Colheita ("Cadastrar Colheita" escolhe a área e redireciona pra cá) — abre o
    // "Novo Diário" já com Colheita pré-selecionada (é o mesmo cadastro, só evita o clique extra).
    if (apGetParam('abrirColheita') === '1') {
        dcVeioDaColheita = true;   // ao salvar, volta para a página Colheita (dcSalvar)
        // Espera também safra e tipos: sem isso, se /ControllerSafra ainda não voltou, dcTemSafra
        // fica false e aparecia o pré-requisito "obrigatório ter uma Safra" mesmo com safra válida.
        $.when(dcFuncPromise, dcTalhaoPromise, dcDespPromise, dcSafraPromise, dcTiposPromise).always(function () {
            dcAbrirNovoInterno();
            var tipoColheita = dcCacheTipos.filter(function (t) { return t.nome.toLowerCase().indexOf('colh') > -1; })[0];
            if (tipoColheita) $('#dcTipo').val(String(tipoColheita.idTipo)).trigger('change');
        });
    }
});

/******************************************************************************************************/
/* DIÁRIO DE CAMPO — dentro do Perfil da Área (DetalheAreaProducao.jsp)                               */
/******************************************************************************************************/
var dcAreaId = null, dcCacheFunc = [], dcCacheIns = [], dcCacheMaq = [], dcCacheTalhao = [], dcCacheTipos = [], dcCacheDesp = [], dcCulturaMap = {};
var dcOptTalhao = '', dcOptTipo = '', dcOptFunc = '', dcOptDesp = '';
var dcSafraResolvida = false, dcTemSafra = false;
var dcFuncPromise = null, dcTalhaoPromise = null, dcDespPromise = null, dcSafraPromise = null, dcTiposPromise = null;
var dcVeioDaColheita = false;   // aberto pela página Colheita (?abrirColheita=1)
var dcPrecoCaixaRef = 0, dcPrecoCaixaChave = '';   // preço da caixa (Despesas/Custos) do alimento+área atuais

function dcMoney(v) { return 'R$ ' + Number(v || 0).toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 }); }
/** Escapa texto vindo do banco antes de montar HTML (seção Diário). */
function dcHtml(v) {
    return String(v == null ? '' : v).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}
function dcBadgeStatus(s) {
    var map = { PLANEJADA: 'bg-secondary', EM_ANDAMENTO: 'bg-info text-dark', CONCLUIDA: 'bg-success', CANCELADA: 'bg-danger' };
    var rot = { PLANEJADA: 'Planejado', EM_ANDAMENTO: 'Execução', CONCLUIDA: 'Concluído', CANCELADA: 'Cancelado' };
    return '<span class="badge ' + (map[s] || 'bg-secondary') + '">' + (rot[s] || s || '') + '</span>';
}

/** Converte texto flexível de horas para horas decimais. Ex.: "2h30m"→2.5, "30min"→0.5, "2:30"→2.5, "1,5"→1.5, "90m"→1.5. */
function dcParseHoras(txt) {
    if (txt == null) return 0;
    var s = String(txt).trim().toLowerCase().replace(',', '.');
    if (s === '') return 0;
    // formato "h:mm"
    var mColon = s.match(/^(\d+):([0-5]?\d)$/);
    if (mColon) return Math.round((parseInt(mColon[1]) + parseInt(mColon[2]) / 60) * 1000) / 1000;
    // formato com h e/ou m (ex.: 2h30m, 2h, 30m, 45min, 1h5)
    var mHM = s.match(/^(?:(\d+(?:\.\d+)?)\s*h)?\s*(?:(\d+(?:\.\d+)?)\s*(?:m|min)?)?$/);
    if (mHM && (mHM[1] || mHM[2]) && /[hm:]/.test(s)) {
        var h = parseFloat(mHM[1] || 0);
        var mnt = parseFloat(mHM[2] || 0);
        return Math.round((h + mnt / 60) * 1000) / 1000;
    }
    // número puro = horas decimais
    var n = parseFloat(s);
    return isNaN(n) ? 0 : n;
}

/** Formata horas decimais para exibição amigável. Ex.: 2.5→"2h30", 0.5→"0h30", 3→"3h". */
function dcFormatHoras(dec) {
    var v = Number(dec) || 0;
    var h = Math.floor(v);
    var m = Math.round((v - h) * 60);
    if (m === 60) { h += 1; m = 0; }
    return h + 'h' + (m > 0 ? (m < 10 ? '0' + m : m) : '');
}

/** Rebuild do select Responsável com base APENAS nos funcionários adicionados na atividade. */
function dcAtualizarResponsavel() {
    var atual = $('#dcResponsavel').val();
    var vistos = {}, opts = '<option value="">Selecione</option>';
    $('#dcFuncBody tr').each(function () {
        var $sel = $(this).find('.dc-fpessoa');
        var id = $sel.val(); if (!id || vistos[id]) return;
        vistos[id] = true;
        var nome = $sel.find(':selected').text() || ('#' + id);
        opts += '<option value="' + id + '">' + nome + '</option>';
    });
    $('#dcResponsavel').html(opts);
    if (atual && vistos[atual]) $('#dcResponsavel').val(atual);
}

/** Recarrega a lista de funcionários conforme a data (só vínculos ativos na data) e atualiza os selects. */
function dcRecarregarFuncsPorData() {
    var data = $('#dcData').val();
    var url = AGRO_CTX + '/ControllerDiarioCampo?funcionarios=1' + (data ? '&data=' + data : '');
    $.getJSON(url, function (fs) {
        dcCacheFunc = fs; dcOptFunc = dcMontarOptFunc(fs);
        // Reaplica nas linhas existentes. Preserva a pessoa já escolhida mesmo que ela não esteja
        // na lista filtrada da data (ex.: edição), acrescentando a opção original de volta.
        $('#dcFuncBody tr').each(function () {
            var $sel = $(this).find('.dc-fpessoa'); var v = $sel.val();
            var optAntigo = v ? $sel.find('option[value="' + v + '"]').prop('outerHTML') : '';
            $sel.html(dcOptFunc);
            if (v) {
                if (!$sel.find('option[value="' + v + '"]').length && optAntigo) $sel.append(optAntigo);
                $sel.val(v);
            }
        });
        dcFiltrarFuncsAtividade();
        dcAtualizarResponsavel();
        dcRecalcPreview();
    });
}
/** Categoria de insumo esperada pelo tipo de atividade selecionado (filtro dinâmico). */
function dcCategoriaPorTipo() {
    var idt = parseInt($('#dcTipo').val()) || 0;
    var t = dcCacheTipos.filter(function (x) { return x.idTipo === idt; })[0];
    var nome = (t ? t.nome : '').toLowerCase();
    if (nome.indexOf('aduba') === 0 || nome.indexOf('aduba') > -1) return 'ADUBO';        // Adubação
    if (nome.indexOf('pulveriz') > -1) return 'DEFENSIVO';                                 // Pulverização
    return null;   // demais: todos os insumos
}

/** O tipo de atividade selecionado é "Colheita"? Mesmo padrão de dcCategoriaPorTipo (busca por nome no cache). */
function dcEhColheita() {
    var idt = parseInt($('#dcTipo').val()) || 0;
    var t = dcCacheTipos.filter(function (x) { return x.idTipo === idt; })[0];
    var nome = (t ? t.nome : '').toLowerCase();
    return nome.indexOf('colh') > -1;
}

/** O tipo de atividade selecionado é "Processamento" (V29 — embalagem da colheita)? */
function dcEhProcessamento() {
    var idt = parseInt($('#dcTipo').val()) || 0;
    var t = dcCacheTipos.filter(function (x) { return x.idTipo === idt; })[0];
    return (t ? t.nome : '').toLowerCase().indexOf('processa') > -1;
}

/** Atividades com regime por caixa (Sistema x Produção): Colheita e Processamento. */
function dcEhAtividadeCaixa() { return dcEhColheita() || dcEhProcessamento(); }

/**
 * Funcionário de Produção só é pago por caixa: na Colheita entra todo mundo menos o Embalador; no
 * Processamento, todo mundo menos o Colhedor; nas demais atividades, nenhum de Produção. Demais
 * tipos (CLT/Diarista/Empreita) sempre. Mesma regra do servidor (ControllerDiarioCampo#validarRegimeCaixa).
 */
function dcFuncApto(tipo, cargo) {
    if (tipo !== 'PRODUCAO') return true;
    var c = String(cargo || '').toLowerCase();
    if (dcEhColheita()) return c !== 'embalador';
    if (dcEhProcessamento()) return c !== 'colhedor';
    return false;
}

/** Monta as options do select de funcionários (data-tipo/ref/refextra/cargo). */
function dcMontarOptFunc(fs) {
    var opt = '<option value="">Selecione</option>';
    fs.forEach(function (f) {
        var rot = f.tipo === 'PRODUCAO' ? 'Produção' + (f.cargo ? ' – ' + f.cargo : '') : f.tipo;
        opt += '<option value="' + f.idPessoa + '" data-tipo="' + f.tipo + '" data-ref="' + (f.custoRef || 0) + '" data-refextra="' + (f.custoRefExtra || 0) +
            '" data-cargo="' + dcHtml(f.cargo || '') + '">' + dcHtml(f.nome) + ' (' + dcHtml(rot) + ')</option>';
    });
    return opt;
}

/**
 * Esconde, nos selects editáveis de funcionário, quem não pode ser lançado na atividade atual
 * (dcFuncApto) e limpa a linha que tinha escolhido alguém que deixou de ser apto.
 */
function dcFiltrarFuncsAtividade() {
    $('#dcFuncBody .dc-fpessoa:not([disabled]), #exFuncBody .ex-fpessoa').each(function () {
        var $sel = $(this);
        $sel.find('option[value!=""]').each(function () {
            var apto = dcFuncApto($(this).attr('data-tipo'), $(this).attr('data-cargo'));
            $(this).prop('hidden', !apto).prop('disabled', !apto);
        });
        // .val() devolve null quando a opção selecionada está desabilitada — checa pela própria opção.
        var $opt = $sel.find('option:selected');
        if ($opt.attr('value') && $opt.prop('disabled')) {
            $sel.val('');
            $sel.closest('tr').find('.dc-ftipo,.ex-ftipo').val('');
        }
    });
}

/**
 * Mostra/esconde as colunas de regime (Regime | Preço | Caixas) nas tabelas de Funcionários
 * (principal e "Registrar Nova Execução") conforme o tipo de atividade selecionado — regime só
 * faz sentido na Colheita e no Processamento. Recalcula a prévia de custo em seguida, pois trocar
 * de tipo muda como cada linha existente deve ser exibida (horas x caixas).
 */
function dcAtualizarVisibilidadeColheita() {
    var colheita = dcEhAtividadeCaixa();
    $('.dc-col-regime').toggleClass('d-none', !colheita);
    $('.dc-col-padrao').toggleClass('d-none', colheita);
    dcFiltrarFuncsAtividade();
    if (!colheita) {
        // Fora da Colheita o regime não existe: reseta para Sistema para não deixar uma linha
        // "presa" em Produção (caixa) de quando o tipo de atividade era Colheita há pouco — o
        // servidor também nunca confiaria nesse valor fora da Colheita, mas aqui evita a UI
        // mostrar Horas=0/valor calculado por caixa de forma incoerente com a atividade atual.
        $('#dcFuncBody .dc-fregime, #exFuncBody .ex-fregime').val('REGIME_SISTEMA');
    }
    $('#dcFuncBody tr').each(function () { dcAtualizarCampoHoras($(this), dcFuncEditavel); });
    $('#exFuncBody tr').each(function () { dcAtualizarCampoHorasExec($(this)); });
    dcRecalcPreview();
    dcExecRecalcPreview();
}

function dcInit(areaId) {
    dcAreaId = areaId;
    $('#dcData').val(new Date().toISOString().slice(0, 10));

    dcTalhaoPromise = $.getJSON(AGRO_CTX + '/ControllerAreaProducao?quadras=' + areaId, function (qs) {
        dcCacheTalhao = qs; dcOptTalhao = '';
        qs.forEach(function (q) { dcOptTalhao += '<option value="' + q.idQuadra + '">' + q.nomeQuadra + '</option>'; });
        $('#dcTalhao').html(dcOptTalhao || '<option value="">(sem talhões)</option>');
    });
    $.getJSON(AGRO_CTX + '/ControllerAreaProducao?alimentos=1', function (as) {
        dcCulturaMap = {}; as.forEach(function (a) { dcCulturaMap[a.id] = a.nome; });
    });
    dcCarregarTipos();
    dcFuncPromise = $.getJSON(AGRO_CTX + '/ControllerDiarioCampo?funcionarios=1', function (fs) {
        dcCacheFunc = fs; dcOptFunc = dcMontarOptFunc(fs);
        $('#dcResponsavel').html(dcOptFunc);
    });
    dcDespPromise = $.getJSON(AGRO_CTX + '/ControllerDiarioCampo?despesascatalogo=1', function (ds) {
        dcCacheDesp = ds; dcOptDesp = '<option value="">Selecione</option>';
        ds.forEach(function (x) { dcOptDesp += '<option value="' + x.idDespesaCusto + '" data-classificacao="' + x.classificacao + '" data-ref="' + (x.valorRef || 0) + '">' + x.nome + '</option>'; });
    });
    $.getJSON(AGRO_CTX + '/ControllerInsumo', function (is) { dcCacheIns = is.filter(function (i) { return i.situacao; }); });
    $.getJSON(AGRO_CTX + '/ControllerMaquina?ativas=true', function (ms) { dcCacheMaq = ms; });
    // Existe ao menos uma safra? (pré-requisito para lançar atividades)
    dcSafraPromise = $.getJSON(AGRO_CTX + '/ControllerSafra', function (ls) { dcTemSafra = Array.isArray(ls) && ls.length > 0; });

    dcCarregarLista();
    autoRefreshTabela('diario', dcCarregarLista, 15000);

    $('#dcFiltroStatus').off('change').on('change', dcCarregarLista);
    $('#btnNovoDiario').off('click').on('click', dcAbrirNovo);
    $('#btnAgendarDiario').off('click').on('click', dcAbrirAgendar);
    $('#btnSalvarAgendar').off('click').on('click', dcSalvarAgendar);
    $('#btnAddFunc').off('click').on('click', function () { dcAddFuncRow(); });
    $('#btnAddMaq').off('click').on('click', function () { dcAddMaqRow(); });
    $('#btnAddInsDef').off('click').on('click', function () { dcAddInsRowDef(); });
    $('#btnAddInsAdu').off('click').on('click', function () { dcAddInsRowAdu(); });
    $('#btnAddDesp').off('click').on('click', function () { dcAddDespRow(); });
    $('#btnAddTipo').off('click').on('click', dcAddTipo);
    $('#btnSalvarDiario').off('click').on('click', dcSalvar);

    // Talhão -> preenche cultura automaticamente (do cadastro do talhão) + resolve safra
    $('#dcTalhao').off('change.dc').on('change.dc', dcAplicarCulturaDoTalhao);
    // Data -> resolve a safra (talhão + data) e recarrega os funcionários ativos na data
    $('#dcData').off('change.dc').on('change.dc', function () { dcResolverSafra(); dcRecarregarFuncsPorData(); });
    // Tipo de atividade -> mostra só a seção de Insumos correspondente (Adubação=Adubo,
    // Pulverização=Defensivo, demais=as duas) e mostra/esconde a coluna Regime (só existe na Colheita)
    $('#dcTipo').off('change.dc').on('change.dc', function () { dcAtualizarVisibilidadeInsumos(); dcAtualizarVisibilidadeColheita(); dcBuscarPrecoCaixa(); });
    // Preço/caixa digitado pelo usuário deixa de seguir a sugestão (Despesas/Custos ou cadastro).
    $('#modalNovoDiario').off('input.dcpc', '.dc-fprecocaixa,.ex-fprecocaixa').on('input.dcpc', '.dc-fprecocaixa,.ex-fprecocaixa', function () { $(this).attr('data-auto', '0'); });

    // recálculo do custo estimado ao vivo
    var body = '#modalNovoDiario';
    $(body).off('input.dc change.dc', '.dc-fpessoa,.dc-fhoras,.dc-fvalor,.dc-fcaixas,.dc-fprecocaixa,.dc-mmaq,.dc-muso,.dc-ipro,.dc-idosepu,.dc-idosetotal,.dc-iforma,.dc-iqtdinf,.dc-iplantas')
        .on('input.dc change.dc', '.dc-fpessoa,.dc-fhoras,.dc-fvalor,.dc-fcaixas,.dc-fprecocaixa,.dc-mmaq,.dc-muso,.dc-ipro,.dc-idosepu,.dc-idosetotal,.dc-iforma,.dc-iqtdinf,.dc-iplantas', dcRecalcPreview);
    // Plantas digitadas pelo usuário deixam de seguir o talhão.
    $(body).off('input.dcpl', '.dc-iplantas').on('input.dcpl', '.dc-iplantas', function () { $(this).attr('data-auto', '0'); });
    $('#dcFuncBody').off('change.dc', '.dc-fpessoa').on('change.dc', '.dc-fpessoa', function () {
        var opt = $(this).find(':selected');
        var $tr = $(this).closest('tr');
        $tr.find('.dc-ftipo').val(opt.data('tipo') || '');
        dcAtualizarCampoHoras($tr, dcFuncEditavel);
        dcAtualizarResponsavel();   // responsável só entre os funcionários adicionados
    });
    $('#dcFuncBody').off('change.dc', '.dc-fregime').on('change.dc', '.dc-fregime', function () {
        dcAtualizarCampoHoras($(this).closest('tr'), dcFuncEditavel);
        dcRecalcPreview();
    });
    $('#dcMaqBody').off('change.dc', '.dc-mmaq').on('change.dc', '.dc-mmaq', function () { dcAtualizarLinhaMaquina($(this).closest('tr')); });
    $('#dcMaqBody').off('input.dc', '.dc-muso').on('input.dc', '.dc-muso', function () { dcAtualizarLinhaMaquina($(this).closest('tr')); });
    $('#dcDespBody').off('change.dcd', '.dc-ditem').on('change.dcd', '.dc-ditem', function () {
        dcAplicarDespesaSelecionada($(this).closest('tr'), 'dc'); dcRecalcPreview();
    });
    $('#dcDespBody').off('input.dcd', '.dc-dqtd,.dc-dvalor').on('input.dcd', '.dc-dqtd,.dc-dvalor', function () {
        dcRecalcLinhaDespesa($(this).closest('tr'), 'dc'); dcRecalcPreview();
    });
    $('#modalNovoDiario').off('click.dc', '.dc-rm').on('click.dc', '.dc-rm', function () {
        var eraFunc = $(this).closest('#dcFuncBody').length > 0;
        $(this).closest('tr').remove();
        if (eraFunc) dcAtualizarResponsavel();
        dcRecalcPreview();
    });
    // Remove um lançamento JÁ SALVO (funcionário/despesa de uma execução anterior) — diferente do
    // .dc-rm acima, que só tira uma linha ainda não salva do formulário: aqui é preciso apagar de
    // verdade no banco e recalcular o custo, por isso vai ao servidor em vez de só manipular o DOM.
    $('#modalNovoDiario').off('click.dcrmhist', '.dc-rm-hist').on('click.dcrmhist', '.dc-rm-hist', function () {
        var tipo = $(this).data('tipo'), idLinha = $(this).data('id');
        var iddiario = parseInt($('#dcId').val()) || 0;
        if (!iddiario || !idLinha) return;
        if (!confirm('Remover este lançamento? O custo do diário será recalculado.')) return;
        var acao = tipo === 'funcionario' ? 'removerfuncionario' : 'removerdespesa';
        $.ajax({
            url: AGRO_CTX + '/ControllerDiarioCampo', method: 'POST', contentType: 'application/json; charset=utf-8',
            data: JSON.stringify({ acao: acao, iddiario: iddiario, id: idLinha }),
            success: function (res) {
                if (res.ok) { mostrarAlerta(res.msg, 'success', '#alertPerfil'); dcCarregarLista(); dcEditar(iddiario); }
                else { $('#alertDiario').removeClass('d-none').addClass('alert-danger').text(res.msg); }
            },
            error: function (xhr) { var m = 'Erro ao remover lançamento.'; try { m = JSON.parse(xhr.responseText).msg || m; } catch (e) {} $('#alertDiario').removeClass('d-none').addClass('alert-danger').text(m); }
        });
    });

    // Bloco "Registrar Nova Execução" (diário EM_ANDAMENTO, múltiplos dias/equipes)
    $('#btnAddExecFunc').off('click').on('click', function () { dcAddExecFuncRow(); });
    $('#btnSalvarExecucao').off('click').on('click', dcSalvarExecucao);
    $('#dcBlocoExecucao').off('input.ex change.ex', '.ex-fpessoa,.ex-fhoras,.ex-fcaixas,.ex-fprecocaixa')
        .on('input.ex change.ex', '.ex-fpessoa,.ex-fhoras,.ex-fcaixas,.ex-fprecocaixa', dcExecRecalcPreview);
    $('#exFuncBody').off('change.ex', '.ex-fpessoa').on('change.ex', '.ex-fpessoa', function () {
        var $tr = $(this).closest('tr');
        var tipo = $(this).find(':selected').data('tipo') || '';
        $tr.find('.ex-ftipo').val(tipo);
        dcAtualizarCampoHorasExec($tr);
        dcExecRecalcPreview();
    });
    $('#exFuncBody').off('change.ex', '.ex-fregime').on('change.ex', '.ex-fregime', function () {
        dcAtualizarCampoHorasExec($(this).closest('tr'));
        dcExecRecalcPreview();
    });
    $('#dcBlocoExecucao').off('click.ex', '.ex-rm').on('click.ex', '.ex-rm', function () { $(this).closest('tr').remove(); });
    $('#btnAddExecDesp').off('click').on('click', function () { dcAddExecDespRow(); });
    $('#exDespBody').off('change.exd', '.ex-ditem').on('change.exd', '.ex-ditem', function () {
        dcAplicarDespesaSelecionada($(this).closest('tr'), 'ex');
    });
    $('#exDespBody').off('input.exd', '.ex-dqtd,.ex-dvalor').on('input.exd', '.ex-dqtd,.ex-dvalor', function () {
        dcRecalcLinhaDespesa($(this).closest('tr'), 'ex');
    });
}

function dcCarregarTipos() {
    dcTiposPromise = $.getJSON(AGRO_CTX + '/ControllerDiarioCampo?tiposatividade=1', function (ts) {
        dcCacheTipos = ts; dcOptTipo = '';
        ts.forEach(function (t) { dcOptTipo += '<option value="' + t.idTipo + '">' + t.nome + '</option>'; });
        $('#dcTipo').html(dcOptTipo);
    });
}

/** Preenche a Cultura (input desabilitado + id oculto) com a cultura cadastrada no talhão. */
function dcAplicarCulturaDoTalhao() {
    var idq = parseInt($('#dcTalhao').val()) || 0;
    var q = dcCacheTalhao.filter(function (x) { return x.idQuadra === idq; })[0];
    if (q && q.idAlimento) {
        $('#dcCultura').val(String(q.idAlimento));
        $('#dcCulturaNome').val(q.alimentoNome || dcCulturaMap[q.idAlimento] || '');
    } else {
        $('#dcCultura').val('');
        $('#dcCulturaNome').val(q ? '(talhão sem cultura cadastrada)' : '');
    }
    dcAtualizarPlantasAdubo();
    dcResolverSafra();
    dcBuscarPrecoCaixa();   // preço da caixa é por alimento do talhão + área
}

/** Resolve e exibe a safra do talhão na data (trava/vínculo — validado no backend). */
function dcResolverSafra() {
    var idq = parseInt($('#dcTalhao').val()) || 0;
    var data = $('#dcData').val();
    dcSafraResolvida = false;
    if (!idq || !data) { $('#dcSafraNome').text('—'); return; }
    $.getJSON(AGRO_CTX + '/ControllerSafra?resolver=' + idq + '&data=' + data, function (r) {
        if (r && r.idSafra) {
            // Safra válida só se NÃO estiver finalizada (finalizada bloqueia lançamento).
            dcSafraResolvida = (r.status !== 'FINALIZADA');
            $('#dcSafraNome').html(r.nome + ' [' + r.status + ']' + (r.status === 'FINALIZADA' ? ' — <span class="text-danger fw-bold">FINALIZADA: bloqueia lançamento</span>' : ''));
        } else {
            dcSafraResolvida = false;
            $('#dcSafraNome').html('<span class="text-danger">nenhuma safra cobre este talhão nesta data (obrigatório)</span>');
        }
    });
}

/** Options do select de insumo, restritas a uma categoria fixa (DEFENSIVO ou ADUBO) — cada uma
 * das duas tabelas de Insumos só lista a sua categoria, então não depende mais do tipo de atividade. */
function dcInsumoOptsCategoria(cat) {
    var opt = '<option value="">Selecione</option>';
    dcCacheIns.filter(function (i) { return i.categoria === cat; })
        .forEach(function (i) { opt += '<option value="' + i.idInsumo + '" data-preco="' + (i.precoMedio || 0) + '" data-unidade="' + (i.unidade || '') +
            '" data-embalagem="' + (i.pesoEmbalagem || '') + '">' + i.nome + ' (' + i.unidade + ')</option>'; });
    return opt;
}

/**
 * Mostra só a seção de Insumos (Defensivo/Adubo) correspondente ao tipo de atividade escolhido —
 * Adubação só precisa de Adubo, Pulverização só de Defensivo; nos demais tipos não há restrição,
 * então mostra as duas. Linhas já lançadas numa seção que fica escondida continuam no formulário
 * e são enviadas normalmente ao salvar — só a exibição muda.
 */
function dcAtualizarVisibilidadeInsumos() {
    var cat = dcCategoriaPorTipo();
    $('#dcInsDefHeader,#dcInsDefAjuda,#dcInsDefTableWrap').toggleClass('d-none', cat === 'ADUBO');
    $('#dcInsAduHeader,#dcInsAduAjuda,#dcInsAduTableWrap').toggleClass('d-none', cat === 'DEFENSIVO');
}

function dcCarregarLista() {
    var st = $('#dcFiltroStatus').val();
    var url = AGRO_CTX + '/ControllerDiarioCampo?area=' + dcAreaId + (st ? '&status=' + st : '');
    $.getJSON(url, function (lista) {
        var $b = $('#dcTabelaBody').empty();
        var pend = 0, concl = 0, custo = 0, planejadas = [];
        if (!lista.length) $b.append('<tr><td colspan="8" class="text-center text-muted py-3">Nenhum diário nesta área.</td></tr>');
        lista.forEach(function (d) {
            if (d.status === 'CONCLUIDA') { concl++; custo += Number(d.custoTotal || 0); }
            else if (d.status === 'PLANEJADA' || d.status === 'EM_ANDAMENTO') pend++;
            if (d.status === 'PLANEJADA') planejadas.push(d);
            var acoes = '<button class="btn btn-sm btn-outline-dark" title="Ver" onclick="dcVerDetalhe(' + d.idDiario + ')"><i class="fas fa-eye"></i></button>';
            if (d.status === 'PLANEJADA') acoes += ' <button class="btn btn-sm btn-outline-success" title="Iniciar execução" onclick="dcIniciarExecucao(' + d.idDiario + ')"><i class="fas fa-play"></i></button>';
            else if (d.status !== 'CONCLUIDA') acoes += ' <button class="btn btn-sm btn-outline-primary" title="Editar" onclick="dcEditar(' + d.idDiario + ')"><i class="fas fa-pen-to-square"></i></button>';
            $b.append('<tr><td>' + (d.numeroDiario || '') + '</td><td>' + formatarData(d.data) + '</td><td>' + (d.quadraNome || '—') +
                '</td><td>' + (d.culturaNome || '—') + '</td><td>' + (d.tipoAtividadeNome || '—') + '</td><td>' + dcBadgeStatus(d.status) +
                '</td><td class="text-end">' + dcMoney(d.custoTotal) + '</td><td class="text-end">' + acoes + '</td></tr>');
        });
        $('#dcCardAtividades').text(lista.length); $('#dcCardPendentes').text(pend);
        $('#dcCardConcluidas').text(concl); $('#dcCardCusto').text(dcMoney(custo));
        // Ícone de notificação de atividades planejadas (agendamentos aguardando execução)
        var $badge = $('#dcBadgeAlertas'), $lista = $('#dcListaAlertas').empty();
        if (planejadas.length > 0) {
            $badge.removeClass('d-none').text(planejadas.length);
            planejadas.forEach(function (d) {
                $lista.append('<li><a class="dropdown-item small" href="#" onclick="dcVerDetalhe(' + d.idDiario + ');return false;">' +
                    '<i class="fas fa-calendar-check me-1 text-warning"></i>' + (d.tipoAtividadeNome || 'Atividade') +
                    ' — ' + (d.quadraNome || '') + ' <span class="text-muted">(' + formatarData(d.dataPrevista || d.data) + ')</span></a></li>');
            });
        } else {
            $badge.addClass('d-none').text('0');
            $lista.append('<li class="text-muted small px-2">Nenhuma atividade planejada.</li>');
        }
    });
}

function dcResetForm() {
    $('#alertDiario').addClass('d-none').text('');
    $('#dcTalhao').html(dcOptTalhao);
    $('#dcResponsavel').html('<option value="">Selecione</option>'); $('#dcTipo').html(dcOptTipo);
    $('#dcStatusSel').val('EM_ANDAMENTO');
    $('#dcId,#dcStatus,#dcNumero,#dcCultura,#dcCulturaNome').val('');
    $('#dcDescricao,#dcObs').val('');
    $('#dcData').val(new Date().toISOString().slice(0, 10));
    $('#dcFuncBody,#dcMaqBody,#dcInsDefBody,#dcInsAduBody,#dcDespBody,#exFuncBody,#exDespBody').empty();
    $('#dcTotalPreview').text(dcMoney(0));
    dcSafraResolvida = false;
    dcFuncEditavel = true;
    dcDespEditavel = true;
    $('#btnAddFunc,#btnAddDesp,#dcFuncHeader,#dcFuncTableWrap,#dcDespHeader,#dcDespTableWrap').removeClass('d-none');
    $('#dcFuncSoNaExecucao,#dcDespSoNaExecucao,#dcBlocoExecucao').addClass('d-none');
    dcAtualizarVisibilidadeInsumos();
}
function dcAbrirNovo() {
    // Aguarda os carregamentos iniciais (funcionários/talhões/despesas) antes de checar pré-requisitos,
    // evitando falso alerta de "sem funcionário" quando o usuário clica antes do fetch terminar.
    var $btn = $('#btnNovoDiario').prop('disabled', true);
    $.when(dcFuncPromise, dcTalhaoPromise, dcDespPromise, dcSafraPromise, dcTiposPromise).always(function () {
        $btn.prop('disabled', false);
        dcAbrirNovoInterno();
    });
}

function dcAbrirNovoInterno() {
    // Pré-requisitos: a área precisa de talhões, funcionários e uma safra cadastrada.
    if (!dcCacheTalhao.length) {
        preReqAlerta('#alertPerfil', 'Esta área não tem <strong>talhões</strong> cadastrados. Cadastre um talhão antes de lançar atividades.',
            CTX + '/view/admin/AtualizarAreaProducao.jsp?id=' + dcAreaId, 'Adicionar Talhão');
        return;
    }
    if (!dcCacheFunc.length) {
        preReqAlerta('#alertPerfil', 'Para lançar uma atividade é preciso ter ao menos um <strong>Funcionário</strong> cadastrado (responsável).',
            CTX + '/view/admin/ColaboCadastro.jsp', 'Cadastrar Funcionário');
        return;
    }
    if (!dcTemSafra) {
        preReqAlerta('#alertPerfil', 'Para lançar uma atividade é obrigatório ter uma <strong>Safra</strong> cadastrada que inclua o talhão e cubra a data.',
            CTX + '/view/admin/safra.jsp', 'Cadastrar Safra');
        return;
    }
    dcResetForm();
    $('#dcModalTitulo').text('Novo Diário de Campo');
    dcAddFuncRow();
    dcRecarregarFuncsPorData();    // funcionários ativos na data de hoje
    dcAplicarCulturaDoTalhao();    // preenche a cultura do talhão pré-selecionado + resolve safra
    $('#modalNovoDiario').modal('show');
}

/* ===================== AGENDAMENTO (modal enxuto, sem funcionários/máquinas/insumos) ===================== */

function dcAbrirAgendar() {
    var $btn = $('#btnAgendarDiario').prop('disabled', true);
    $.when(dcFuncPromise, dcTalhaoPromise, dcDespPromise).always(function () {
        $btn.prop('disabled', false);
        if (!dcCacheTalhao.length) {
            preReqAlerta('#alertPerfil', 'Esta área não tem <strong>talhões</strong> cadastrados. Cadastre um talhão antes de agendar atividades.',
                CTX + '/view/admin/AtualizarAreaProducao.jsp?id=' + dcAreaId, 'Adicionar Talhão');
            return;
        }
        if (!dcCacheFunc.length) {
            preReqAlerta('#alertPerfil', 'Para agendar uma atividade é preciso ter ao menos um <strong>Funcionário</strong> cadastrado (responsável).',
                CTX + '/view/admin/ColaboCadastro.jsp', 'Cadastrar Funcionário');
            return;
        }
        $('#alertAgendar').addClass('d-none').text('');
        $('#agTalhao').html(dcOptTalhao);
        $('#agTipo').html(dcOptTipo);
        $('#agResponsavel').html(dcOptFunc);
        $('#agData').val(new Date().toISOString().slice(0, 10));
        $('#agDescricao, #agObs').val('');
        $('#modalAgendarDiario').modal('show');
    });
}

function dcSalvarAgendar() {
    var idQuadra = parseInt($('#agTalhao').val()) || null;
    var idResponsavel = parseInt($('#agResponsavel').val()) || null;
    var idTipoAtividade = parseInt($('#agTipo').val()) || 0;
    var data = $('#agData').val();
    if (!idQuadra || !idResponsavel || !idTipoAtividade || !data) {
        $('#alertAgendar').removeClass('d-none').addClass('alert-danger').text('Talhão, responsável, tipo de atividade e data prevista são obrigatórios.');
        return;
    }
    var q = dcCacheTalhao.filter(function (x) { return x.idQuadra === idQuadra; })[0];
    var payload = {
        acao: 'create', idArea: dcAreaId, idQuadra: idQuadra,
        idCultura: q && q.idAlimento ? q.idAlimento : null,
        idResponsavel: idResponsavel, idTipoAtividade: idTipoAtividade,
        status: 'PLANEJADA', data: data, dataPrevista: data,
        descricao: $('#agDescricao').val(), observacoes: $('#agObs').val(),
        funcionarios: [], maquinas: [], insumos: []
    };
    $.ajax({
        url: AGRO_CTX + '/ControllerDiarioCampo', method: 'POST', contentType: 'application/json; charset=utf-8',
        data: JSON.stringify(payload),
        success: function (res) {
            if (res.ok) { $('#modalAgendarDiario').modal('hide'); mostrarAlerta(res.msg + (res.numeroDiario ? ' (' + res.numeroDiario + ')' : ''), 'success', '#alertPerfil'); dcCarregarLista(); }
            else $('#alertAgendar').removeClass('d-none').addClass('alert-danger').text(res.msg);
        },
        error: function (xhr) { var m = 'Erro ao agendar atividade.'; try { m = JSON.parse(xhr.responseText).msg || m; } catch (e) {} $('#alertAgendar').removeClass('d-none').addClass('alert-danger').text(m); }
    });
}

var dcFuncEditavel = true;   // false quando editando um diário já existente (funcionários viram somente leitura; use "Registrar execução")
var dcDespEditavel = true;   // false quando editando um diário já existente (despesas viram somente leitura; use "Registrar execução")

/**
 * Empreita não trabalha por hora: esconde/desabilita o campo Horas e mantém o Valor(auto) = valor
 * da empreitada. Regime PRODUCAO (Colheita por caixa) troca o campo Horas por Qtd caixas + Preço/
 * caixa, independente do tipo de contrato — CLT/DIARISTA/EMPREITA podem estar em regime PRODUCAO.
 */
/**
 * Caixas colhidas é sempre pedido na Colheita (é volume físico, vale pros dois regimes); Preço/
 * caixa é exclusivo de PRODUCAO (só ali o preço afeta o pagamento); Horas some quando PRODUCAO
 * (não é pago por hora) e quando EMPREITA (valor fixo, não depende de horas).
 */
function dcAtualizarCampoHoras($tr, funcEditavel) {
    var tipo = $tr.find('.dc-ftipo').val();
    var colheita = dcEhAtividadeCaixa();
    var $horas = $tr.find('.dc-fhoras');
    var $caixas = $tr.find('.dc-fcaixas');
    var $preco = $tr.find('.dc-fprecocaixa');
    var regime = dcAplicarRegimeProducao($tr, 'dc', colheita, funcEditavel);

    if (!colheita) {
        $caixas.addClass('d-none'); $preco.addClass('d-none'); $horas.removeClass('d-none');
        if (tipo === 'EMPREITA') { $horas.val('').prop('disabled', true).attr('placeholder', '—'); }
        else { $horas.prop('disabled', !funcEditavel).attr('placeholder', '2h30 / 45min / 1,5'); }
        return;
    }
    $caixas.removeClass('d-none').prop('disabled', !funcEditavel);
    if (regime === 'PRODUCAO') {
        $horas.addClass('d-none').prop('disabled', true);
        $preco.removeClass('d-none').prop('disabled', !funcEditavel);
    } else {
        $preco.addClass('d-none');
        $horas.removeClass('d-none');
        if (tipo === 'EMPREITA') { $horas.val('').prop('disabled', true).attr('placeholder', '—'); }
        else { $horas.prop('disabled', !funcEditavel).attr('placeholder', '2h30 / 45min / 1,5'); }
    }
}

/**
 * Funcionário de Produção (Colhedor/Embalador) só é pago por caixa: trava o Regime em Produção e
 * sugere o Preço/caixa do cadastro (valorporunidade) se ainda estiver vazio. Demais tipos: regime
 * livre. Devolve o regime efetivo da linha. prefix = 'dc' (cadastro) ou 'ex' (nova execução).
 */
function dcAplicarRegimeProducao($tr, prefix, atividadeCaixa, funcEditavel) {
    var $reg = $tr.find('.' + prefix + '-fregime');
    if (atividadeCaixa && $tr.find('.' + prefix + '-ftipo').val() === 'PRODUCAO') {
        $reg.val('PRODUCAO').prop('disabled', true);
    } else {
        $reg.prop('disabled', !funcEditavel);
    }
    if (funcEditavel && atividadeCaixa && $reg.val() === 'PRODUCAO') dcSugerirPrecoCaixa($tr, prefix);
    return $reg.val();
}

/**
 * Preço/caixa sugerido numa linha em regime Produção: na Colheita, o preço da caixa cadastrado em
 * Despesas/Custos para o alimento do talhão + área (dcPrecoCaixaRef) tem prioridade; senão, para
 * funcionário de Produção, o preço do cadastro dele (data-ref). Só preenche campo vazio ou que foi
 * preenchido automaticamente (data-auto="1") — o que o usuário digitou fica.
 */
function dcSugerirPrecoCaixa($tr, prefix) {
    var $p = $tr.find('.' + prefix + '-fprecocaixa');
    if ($p.val() && $p.attr('data-auto') !== '1') return;
    var v = 0;
    if (dcEhColheita() && dcPrecoCaixaRef > 0) v = dcPrecoCaixaRef;
    else if ($tr.find('.' + prefix + '-ftipo').val() === 'PRODUCAO') v = Number($tr.find('.' + prefix + '-fpessoa :selected').attr('data-ref')) || 0;
    if (v > 0) $p.val(v).attr('data-auto', '1');
    else if ($p.attr('data-auto') === '1') $p.val('').removeAttr('data-auto');   // sugestão de outro talhão/tipo
}

/**
 * Colheita: busca em Despesas/Custos (V28) o preço da caixa CAIXA_COLHEITA/CONTENTOR do alimento do
 * talhão + área. Achou: preenche o Preço/cx das linhas em Produção. Não achou: aviso discreto — ao
 * salvar, o servidor registra o preço da primeira linha (DiarioCampoDAO.registrarPrecoCaixaSeAusente).
 * Chamada ao trocar tipo/talhão e ao abrir um diário existente.
 */
function dcBuscarPrecoCaixa() {
    var idAlimento = parseInt($('#dcCultura').val()) || 0;
    var chave = idAlimento + '/' + dcAreaId;
    dcPrecoCaixaRef = 0; dcPrecoCaixaChave = chave;
    $('#dcAvisoPrecoCaixa').addClass('d-none').text('');
    if (!dcEhColheita() || !idAlimento || !dcAreaId) return;
    $.getJSON(AGRO_CTX + '/DespesasCustosServlet', { precoCaixa: 1, categoria: 'CAIXA_COLHEITA', idAlimento: idAlimento, idArea: dcAreaId, tipoCaixa: 'CONTENTOR' }, function (r) {
        if (dcPrecoCaixaChave !== chave) return;   // talhão/tipo trocou enquanto buscava
        if (r && r.encontrado && r.registro) {
            dcPrecoCaixaRef = Number(r.registro.valordespesascustos) || 0;
        } else {
            $('#dcAvisoPrecoCaixa').removeClass('d-none').html('<i class="fas fa-circle-info me-1"></i>Preço da caixa não cadastrado para <strong>' +
                dcHtml($('#dcCulturaNome').val()) + '</strong> — <strong>' + dcHtml($('#detProp').text()) +
                '</strong>; o valor informado será registrado em Despesas/Custos.');
        }
        $('#dcFuncBody tr').each(function () { dcAtualizarCampoHoras($(this), dcFuncEditavel); });
        $('#exFuncBody tr').each(function () { dcAtualizarCampoHorasExec($(this)); });
        dcRecalcPreview();
        dcExecRecalcPreview();
    });
}

/** Mesma lógica de dcAtualizarCampoHoras, para as linhas do bloco "Registrar Nova Execução" (prefixo ex-). */
function dcAtualizarCampoHorasExec($tr) {
    var tipo = $tr.find('.ex-ftipo').val();
    var colheita = dcEhAtividadeCaixa();
    var regime = dcAplicarRegimeProducao($tr, 'ex', colheita, true);
    var $horas = $tr.find('.ex-fhoras');
    var $caixas = $tr.find('.ex-fcaixas');
    var $preco = $tr.find('.ex-fprecocaixa');

    if (!colheita) {
        $caixas.addClass('d-none'); $preco.addClass('d-none'); $horas.removeClass('d-none');
        if (tipo === 'EMPREITA') { $horas.val('').prop('disabled', true); } else { $horas.prop('disabled', false); }
        return;
    }
    $caixas.removeClass('d-none');
    if (regime === 'PRODUCAO') {
        $horas.addClass('d-none').prop('disabled', true);
        $preco.removeClass('d-none');
    } else {
        $preco.addClass('d-none');
        $horas.removeClass('d-none');
        if (tipo === 'EMPREITA') { $horas.val('').prop('disabled', true); } else { $horas.prop('disabled', false); }
    }
}
function dcAddFuncRow(f) {
    var dis = dcFuncEditavel ? '' : 'disabled';
    // Linha nova (ainda não salva): remove só do formulário (.dc-rm). Linha histórica (já persistida,
    // f.id vindo do servidor): remove de verdade via API (.dc-rm-hist), pois foi lançada errada e
    // precisa recalcular o custo — não dá pra simplesmente tirar da tela, como faz uma linha nova.
    var btnCell = dcFuncEditavel
        ? '<button type="button" class="btn btn-sm btn-outline-danger dc-rm" title="Remover"><i class="fas fa-trash"></i></button>'
        : (f && f.id ? '<button type="button" class="btn btn-sm btn-outline-danger dc-rm-hist" data-tipo="funcionario" data-id="' + f.id + '" title="Remover este lançamento (recalcula o custo)"><i class="fas fa-trash"></i></button>' : '');
    $('#dcFuncBody').append('<tr>' +
        '<td><select class="form-select form-select-sm dc-fpessoa" ' + dis + '>' + dcOptFunc + '</select></td>' +
        '<td><input class="form-control form-control-sm dc-ftipo" readonly placeholder="—" style="width:90px"></td>' +
        '<td class="dc-col-regime d-none"><select class="form-select form-select-sm dc-fregime" ' + dis + '>' +
            '<option value="REGIME_SISTEMA">Sistema</option><option value="PRODUCAO">Produção (caixa)</option></select></td>' +
        // Colheita/Processamento: Regime | Preço | Caixas | Horas | Valor (colunas .dc-col-regime);
        // demais atividades: só Horas | Valor.
        '<td class="dc-col-regime d-none"><input type="number" class="form-control form-control-sm dc-fprecocaixa d-none" placeholder="Preço/cx" title="Preço por caixa (regime Produção)" min="0.01" step="0.01" ' + dis + '></td>' +
        '<td class="dc-col-regime d-none"><input type="number" class="form-control form-control-sm dc-fcaixas d-none" placeholder="Caixas" min="0.01" step="0.01" ' + dis + '></td>' +
        '<td><input type="text" class="form-control form-control-sm dc-fhoras" placeholder="2h30 / 45min / 1,5" title="Aceita horas e minutos: 2h30, 45min, 2:30 ou 1,5" value="" ' + dis + '></td>' +
        '<td><input type="number" class="form-control form-control-sm dc-fvalor bg-light" readonly title="Calculado automaticamente conforme o tipo de funcionário" value="0"></td>' +
        '<td class="text-center">' + btnCell + '</td></tr>');
    if (f) {
        var $tr = $('#dcFuncBody tr:last'); var $sel = $tr.find('.dc-fpessoa');
        $sel.val(String(f.idPessoa));
        // Somente leitura (execução de outro dia) pode não estar na lista filtrada atual do select —
        // garante que o nome apareça mesmo assim, adicionando a opção de volta.
        if ($sel.val() !== String(f.idPessoa)) {
            $sel.append('<option value="' + f.idPessoa + '">' + (f.nomePessoa || ('#' + f.idPessoa)) + '</option>');
            $sel.val(String(f.idPessoa));
        }
        $tr.find('.dc-ftipo').val(f.tipoFuncionario || '');
        $tr.find('.dc-fregime').val(f.regimeTrabalho || 'REGIME_SISTEMA');
        $tr.find('.dc-fhoras').val(Number(f.horasTrabalhadas) > 0 ? dcFormatHoras(f.horasTrabalhadas) : '');
        if (f.quantidadeCaixas != null) $tr.find('.dc-fcaixas').val(f.quantidadeCaixas);
        if (f.regimeTrabalho === 'PRODUCAO') {
            $tr.find('.dc-fprecocaixa').val(f.precoPorCaixa || 0);
            $tr.find('.dc-fvalor').val(f.custo || f.valorRegime || 0);
        } else {
            $tr.find('.dc-fvalor').val(f.custo || f.valorContratado || 0);
        }
        dcAtualizarCampoHoras($tr, dcFuncEditavel);
    }
    dcAtualizarResponsavel();
    dcAtualizarVisibilidadeColheita();
}
/** @param bodySel corpo da tabela de destino — permite reuso no modal dedicado de Colheita (#chMaqBody). */
function dcAddMaqRow(m, bodySel) {
    bodySel = bodySel || '#dcMaqBody';
    if (!m && !dcCacheMaq.length) {
        preReqAlerta('#alertDiario', 'Não há <strong>Maquinário</strong> cadastrado. Cadastre em Configuração → Maquinário para usar aqui.',
            CTX + '/view/admin/maquina.jsp', 'Cadastrar Maquinário');
        return;
    }
    var opt = '<option value="">Selecione</option>';
    dcCacheMaq.forEach(function (mq) { opt += '<option value="' + mq.idMaquina + '" data-tipo="' + mq.tipo + '" data-ch="' + (mq.custoHora || 0) + '" data-ck="' + (mq.custoKm || 0) + '">' + mq.nome + ' (' + mq.tipo + ')</option>'; });
    $(bodySel).append('<tr>' +
        '<td><select class="form-select form-select-sm dc-mmaq">' + opt + '</select></td>' +
        '<td class="d-flex align-items-center gap-1"><input type="number" class="form-control form-control-sm dc-muso" min="0" step="0.1" value="0"><span class="dc-mun small text-muted">h</span></td>' +
        '<td class="text-end dc-mcusto">R$ 0,00</td>' +
        '<td class="text-center"><button type="button" class="btn btn-sm btn-outline-danger dc-rm"><i class="fas fa-trash"></i></button></td></tr>');
    if (m) {
        var $tr = $(bodySel + ' tr:last');
        if (m.idMaquina) $tr.find('.dc-mmaq').val(String(m.idMaquina));
        var opt2 = $tr.find('.dc-mmaq :selected');
        var isVeic = (opt2.data('tipo') === 'VEICULO');
        $tr.find('.dc-muso').val(isVeic ? (m.km || 0) : (m.horasTrabalhadas || 0));
        dcAtualizarLinhaMaquina($tr);
    }
}
function dcAtualizarLinhaMaquina($tr) {
    var opt = $tr.find('.dc-mmaq :selected');
    var isVeic = (opt.data('tipo') === 'VEICULO');
    var unit = isVeic ? Number(opt.data('ck') || 0) : Number(opt.data('ch') || 0);
    $tr.find('.dc-mun').text(isVeic ? 'km' : 'h');
    var uso = Number($tr.find('.dc-muso').val()) || 0;
    $tr.find('.dc-mcusto').text(dcMoney(unit * uso));
    dcRecalcPreview();
}
function dcAddInsRowDef(i) {
    if (!i && !dcCacheIns.some(function (x) { return x.categoria === 'DEFENSIVO'; })) {
        preReqAlerta('#alertDiario', 'Não há <strong>Insumo Defensivo</strong> cadastrado. Cadastre no Estoque de Insumos para usar aqui.',
            CTX + '/view/admin/estoque.jsp', 'Cadastrar Insumo');
        return;
    }
    $('#dcInsDefBody').append('<tr>' +
        '<td><select class="form-select form-select-sm dc-ipro">' + dcInsumoOptsCategoria('DEFENSIVO') + '</select></td>' +
        '<td><div class="input-group input-group-sm">' +
            '<input type="text" inputmode="decimal" class="form-control dc-idosepu" value="" placeholder="0,0000" title="Quantidade do produto, na unidade do cadastro, por litro ou kg de calda">' +
            '<span class="input-group-text dc-iunid">—</span>' +
        '</div></td>' +
        '<td><input type="text" inputmode="decimal" class="form-control dc-idosetotal" value="" placeholder="0,000" title="Total de calda efetivamente aplicada"></td>' +
        '<td><div class="input-group input-group-sm"><input type="text" class="form-control dc-iqtdtotal bg-light" readonly value="0" title="Quantidade total consumida, debitada do estoque"><span class="input-group-text dc-iunid2">—</span></div></td>' +
        '<td class="text-center"><button type="button" class="btn btn-sm btn-outline-danger dc-rm"><i class="fas fa-trash"></i></button></td></tr>');
    var $tr = $('#dcInsDefBody tr:last');
    aplicarMascaraDecimal($tr.find('.dc-idosepu'), 4);
    aplicarMascaraDecimal($tr.find('.dc-idosetotal'), 3);
    if (i) {
        $tr.find('.dc-ipro').val(String(i.idInsumo));
        if (i.dosePorUnidade != null) $tr.find('.dc-idosepu').val(exibirDecimalNatural(i.dosePorUnidade, 4));
        if (i.doseTotalAplicada != null) $tr.find('.dc-idosetotal').val(exibirDecimalNatural(i.doseTotalAplicada, 3));
    }
    dcAtualizarLinhaInsumoDef($tr);
}

function dcAddInsRowAdu(i) {
    if (!i && !dcCacheIns.some(function (x) { return x.categoria === 'ADUBO'; })) {
        preReqAlerta('#alertDiario', 'Não há <strong>Insumo Adubo</strong> cadastrado. Cadastre no Estoque de Insumos para usar aqui.',
            CTX + '/view/admin/estoque.jsp', 'Cadastrar Insumo');
        return;
    }
    $('#dcInsAduBody').append('<tr>' +
        '<td><select class="form-select form-select-sm dc-ipro">' + dcInsumoOptsCategoria('ADUBO') + '</select></td>' +
        '<td><div class="input-group input-group-sm">' +
            '<select class="form-select form-select-sm dc-iforma" style="max-width:9.5rem" title="Como o adubo foi aplicado: em sacos (escolha o peso do saco), em toneladas ou em kg">' +
                '<option value="S5">Saco 5 kg</option><option value="S10">Saco 10 kg</option><option value="S25">Saco 25 kg</option><option value="S50">Saco 50 kg</option>' +
                '<option value="T">Tonelada</option><option value="KG">Quilo (kg)</option></select>' +
            '<input type="text" inputmode="decimal" class="form-control dc-iqtdinf" value="" placeholder="0" title="Quantidade aplicada na forma escolhida">' +
            '<span class="input-group-text dc-iqtdlbl">sacos</span>' +
        '</div></td>' +
        '<td><input type="text" inputmode="numeric" class="form-control form-control-sm dc-iplantas" value="" data-auto="1" title="Plantas do talhão que receberam o adubo (padrão: todas as plantas do talhão)"></td>' +
        '<td><input type="text" class="form-control form-control-sm dc-iporplanta bg-light" readonly value="—" title="Dose por planta = total aplicado ÷ nº de plantas"></td>' +
        '<td><div class="input-group input-group-sm"><input type="text" class="form-control dc-iqtdtotal bg-light" readonly value="0" title="Quantidade total consumida, debitada do estoque"><span class="input-group-text dc-iunid2">—</span></div></td>' +
        '<td class="text-center"><button type="button" class="btn btn-sm btn-outline-danger dc-rm"><i class="fas fa-trash"></i></button></td></tr>');
    var $tr = $('#dcInsAduBody tr:last');
    aplicarMascaraDecimal($tr.find('.dc-iqtdinf'), 3);
    $tr.find('.dc-iplantas').val(dcPlantasDoTalhao() || '');
    if (i) {
        $tr.find('.dc-ipro').val(String(i.idInsumo));
        var un = $tr.find('.dc-ipro :selected').data('unidade');
        if (i.formaAplicacao) {
            $tr.find('.dc-iforma').val(i.formaAplicacao === 'SACO' ? 'S' + Number(i.pesoSacoKg) : (i.formaAplicacao === 'TONELADA' ? 'T' : 'KG'));
            $tr.find('.dc-iqtdinf').val(exibirDecimalNatural(i.qtdInformada, 3));
            if (i.plantasAplicadas) $tr.find('.dc-iplantas').val(i.plantasAplicadas).attr('data-auto', '0');
        } else if (i.quantidadeDireta != null) {
            // Lançamento anterior à V27 (só a quantidade total): reabre como t ou kg.
            var q = Number(i.quantidadeDireta);
            $tr.find('.dc-iforma').val(un === 'T' ? 'T' : 'KG');
            $tr.find('.dc-iqtdinf').val(exibirDecimalNatural(un === 'G' ? q / 1000 : q, 3));
        }
        $tr.attr('data-ins', String(i.idInsumo));
    }
    dcAtualizarLinhaInsumoAdu($tr);
}

/** Nº de plantas do talhão selecionado no Diário (0 se não houver). */
function dcPlantasDoTalhao() {
    var idq = parseInt($('#dcTalhao').val()) || 0;
    var q = dcCacheTalhao.filter(function (x) { return x.idQuadra === idq; })[0];
    return q ? (parseInt(q.numeroPlantas) || 0) : 0;
}

/** Ao trocar o talhão, atualiza as plantas das linhas de adubo que o usuário não editou. */
function dcAtualizarPlantasAdubo() {
    var p = dcPlantasDoTalhao();
    $('#dcInsAduBody tr').each(function () {
        var $p = $(this).find('.dc-iplantas');
        if ($p.attr('data-auto') === '1') $p.val(p || '');
        dcAtualizarLinhaInsumoAdu($(this));
    });
}

var DC_PESOS_SACO = [5, 10, 25, 50];
function dcEhMassa(un) { return un === 'G' || un === 'KG' || un === 'T'; }

/** Lê a forma de aplicação da linha de adubo: {forma: SACO|TONELADA|KG, pesoSacoKg}. */
function dcFormaAdubo($tr) {
    var v = $tr.find('.dc-iforma').val() || 'KG';
    if (v.charAt(0) === 'S') return { forma: 'SACO', pesoSacoKg: Number(v.slice(1)) };
    return { forma: v === 'T' ? 'TONELADA' : 'KG', pesoSacoKg: null };
}

/** Mostra a unidade do insumo escolhido e recalcula a quantidade total consumida (linha Defensivo: dose do produto × total de calda aplicada). */
function dcAtualizarLinhaInsumoDef($tr) {
    var opt = $tr.find('.dc-ipro :selected');
    var unidade = opt.data('unidade') || '—';
    $tr.find('.dc-iunid,.dc-iunid2').text(unidade);
    var dosePu = lerDecimalBR($tr.find('.dc-idosepu').val());
    var doseTotal = lerDecimalBR($tr.find('.dc-idosetotal').val());
    $tr.find('.dc-iqtdtotal').val(exibirDecimalBR(dosePu * doseTotal, 3));
}

/**
 * Linha Adubo: identifica pelo cadastro do insumo como ele chega (saco de 5/10/25/50 kg ou
 * tonelada) e pré-seleciona a forma; calcula o total (sacos × peso, t × 1000 ou kg), converte para
 * a unidade do insumo (debitada do estoque) e a dose por planta. Ex.: 10 sacos de 50 kg em 50
 * plantas = 500 kg, 10 kg/planta; 5 t em 500 plantas = 10 kg/planta. O servidor refaz a conta
 * (DiarioCampoDAO.inserirInsumo) — aqui é só a prévia.
 */
function dcAtualizarLinhaInsumoAdu($tr) {
    var opt = $tr.find('.dc-ipro :selected');
    var unidade = opt.data('unidade') || '';
    var massa = dcEhMassa(unidade);
    $tr.find('.dc-iunid2').text(unidade || '—');
    // Insumo trocado: pré-seleciona a forma conforme o cadastro (saco padrão → saco; unidade T → tonelada).
    if (opt.val() && $tr.attr('data-ins') !== String(opt.val())) {
        $tr.attr('data-ins', String(opt.val()));
        var embKg = Math.round(insUnidadeParaKg(parseFloat(opt.data('embalagem')) || 0, unidade) * 1000) / 1000;
        $tr.find('.dc-iforma').val(DC_PESOS_SACO.indexOf(embKg) >= 0 ? 'S' + embKg : (unidade === 'T' ? 'T' : 'KG'));
    }
    // Adubo que não é de massa (ex.: foliar em litros): quantidade direto na unidade do insumo.
    $tr.find('.dc-iforma').toggleClass('d-none', !massa);
    $tr.find('.dc-iplantas').prop('disabled', !massa);
    var qtd = lerDecimalBR($tr.find('.dc-iqtdinf').val());
    var plantas = parseInt($tr.find('.dc-iplantas').val()) || 0;
    if (!massa) {
        $tr.find('.dc-iqtdlbl').text(unidade || '—');
        $tr.find('.dc-iporplanta').val('—');
        $tr.find('.dc-iqtdtotal').val(exibirDecimalBR(qtd, 3));
        return;
    }
    var f = dcFormaAdubo($tr);
    $tr.find('.dc-iqtdlbl').text(f.forma === 'SACO' ? 'sacos' : (f.forma === 'TONELADA' ? 't' : 'kg'));
    var totalKg = f.forma === 'SACO' ? qtd * f.pesoSacoKg : (f.forma === 'TONELADA' ? qtd * 1000 : qtd);
    $tr.find('.dc-iqtdtotal').val(exibirDecimalBR(insKgParaUnidade(totalKg, unidade), 3));
    var txt = '—';
    if (plantas > 0 && totalKg > 0) {
        var kgPl = totalKg / plantas;
        txt = f.forma === 'TONELADA'
            ? exibirDecimalNatural(kgPl / 1000, 4) + ' t (' + exibirDecimalNatural(kgPl, 3) + ' kg)/planta'
            : exibirDecimalNatural(kgPl, 3) + ' kg/planta';
    }
    $tr.find('.dc-iporplanta').val(txt);
}

/**
 * Aplica classificação/valor de referência da despesa selecionada numa linha (Fixo trava o
 * valor unitário no valor do catálogo; Variável libera edição, sugerindo o valor de referência
 * — cada linha pode ter seu próprio preço, sem forçar média). Usado tanto no formulário
 * principal (prefix 'dc') quanto no bloco de execução (prefix 'ex').
 */
function dcAplicarDespesaSelecionada($tr, prefix) {
    var opt = $tr.find('.' + prefix + '-ditem :selected');
    var classificacao = opt.data('classificacao') || '';
    var ref = Number(opt.data('ref') || 0);
    var variavel = classificacao === 'VARIAVEL';
    $tr.find('.' + prefix + '-dclass').text(classificacao ? (variavel ? 'Variável' : 'Fixo') : '—')
        .toggleClass('bg-warning text-dark', variavel).toggleClass('bg-secondary', !variavel);
    $tr.find('.' + prefix + '-dvalor').prop('readonly', !variavel).val(ref.toFixed(2));
    dcRecalcLinhaDespesa($tr, prefix);
}
function dcRecalcLinhaDespesa($tr, prefix) {
    var qtd = Number($tr.find('.' + prefix + '-dqtd').val()) || 0;
    var valor = Number($tr.find('.' + prefix + '-dvalor').val()) || 0;
    $tr.find('.' + prefix + '-dsub').val(dcMoney(qtd * valor));
}

function dcAddDespRow(d) {
    if (!d && !dcCacheDesp.length) {
        preReqAlerta('#alertDiario', 'Não há <strong>Despesa/Custo</strong> cadastrada. Cadastre em Configuração → Custos e Despesas para usar aqui.',
            CTX + '/view/admin/despesascustos.jsp', 'Cadastrar Despesa/Custo');
        return;
    }
    var dis = dcDespEditavel ? '' : 'disabled';
    var btnCell = dcDespEditavel
        ? '<button type="button" class="btn btn-sm btn-outline-danger dc-rm" title="Remover"><i class="fas fa-trash"></i></button>'
        : (d && d.id ? '<button type="button" class="btn btn-sm btn-outline-danger dc-rm-hist" data-tipo="despesa" data-id="' + d.id + '" title="Remover esta despesa (recalcula o custo)"><i class="fas fa-trash"></i></button>' : '');
    $('#dcDespBody').append('<tr>' +
        '<td><select class="form-select form-select-sm dc-ditem" ' + dis + '>' + dcOptDesp + '</select></td>' +
        '<td><span class="badge dc-dclass bg-secondary">—</span></td>' +
        '<td><input type="number" class="form-control form-control-sm dc-dqtd" min="0.01" step="0.01" value="1" ' + dis + '></td>' +
        '<td><input type="number" class="form-control form-control-sm dc-dvalor" min="0" step="0.01" value="0" readonly></td>' +
        '<td><input type="text" class="form-control form-control-sm dc-dsub bg-light" readonly value="R$ 0,00"></td>' +
        '<td class="text-center">' + btnCell + '</td></tr>');
    if (d) {
        var $tr = $('#dcDespBody tr:last');
        var $sel = $tr.find('.dc-ditem');
        $sel.val(String(d.idDespesaCusto));
        // Histórico de outra execução pode não estar na lista filtrada atual do select —
        // garante que o nome apareça mesmo assim, adicionando a opção de volta.
        if ($sel.val() !== String(d.idDespesaCusto)) {
            $sel.append('<option value="' + d.idDespesaCusto + '">' + (d.descricao || ('#' + d.idDespesaCusto)) + '</option>');
            $sel.val(String(d.idDespesaCusto));
        }
        var variavel = d.classificacao === 'VARIAVEL';
        $tr.find('.dc-dclass').text(variavel ? 'Variável' : 'Fixo').toggleClass('bg-warning text-dark', variavel).toggleClass('bg-secondary', !variavel);
        $tr.find('.dc-dqtd').val(d.quantidade != null ? d.quantidade : 1);
        $tr.find('.dc-dvalor').val(Number(d.valorUnitario || 0).toFixed(2));
        $tr.find('.dc-dsub').val(dcMoney(d.valor));
    }
}

function dcAddExecDespRow() {
    if (!dcCacheDesp.length) {
        preReqAlerta('#alertDiario', 'Não há <strong>Despesa/Custo</strong> cadastrada. Cadastre em Configuração → Custos e Despesas para usar aqui.',
            CTX + '/view/admin/despesascustos.jsp', 'Cadastrar Despesa/Custo');
        return;
    }
    $('#exDespBody').append('<tr>' +
        '<td><select class="form-select form-select-sm ex-ditem">' + dcOptDesp + '</select></td>' +
        '<td><span class="badge ex-dclass bg-secondary">—</span></td>' +
        '<td><input type="number" class="form-control form-control-sm ex-dqtd" min="0.01" step="0.01" value="1"></td>' +
        '<td><input type="number" class="form-control form-control-sm ex-dvalor" min="0" step="0.01" value="0" readonly></td>' +
        '<td><input type="text" class="form-control form-control-sm ex-dsub bg-light" readonly value="R$ 0,00"></td>' +
        '<td class="text-center"><button type="button" class="btn btn-sm btn-outline-danger ex-rm"><i class="fas fa-trash"></i></button></td></tr>');
}

/** Custo estimado ao vivo (Funcionários + Máquinas + Insumos). */
/**
 * Valor(auto) de um lançamento de mão de obra, conforme o tipo do funcionário:
 *  - CLT: até 8h, horas × valor da hora normal; acima de 8h, 8h à hora normal
 *    + as horas excedentes à hora extra (refExtra).
 *  - DIARISTA: até 8h, prorateado (horas × diária/8); acima de 8h, a diária cheia.
 *  - EMPREITA (e demais): valor fixo acordado, não depende de horas.
 * Espelha o cálculo autoritativo do servidor em DiarioCampoDAO.recalcularCustos —
 * aqui é só a prévia exibida antes de salvar.
 */
function dcValorFuncionario(tipo, ref, refExtra, horas) {
    ref = Number(ref) || 0; refExtra = Number(refExtra) || 0; horas = Number(horas) || 0;
    if (tipo === 'CLT') {
        if (horas > 8) return (8 * ref) + ((horas - 8) * refExtra);
        return horas * ref;
    }
    if (tipo === 'DIARISTA') {
        if (horas > 8) return ref;
        return horas * (ref / 8);
    }
    return ref;   // EMPREITA
}
function dcRecalcPreview() {
    var total = 0;
    $('#dcFuncBody tr').each(function () {
        if (!dcFuncEditavel) {
            // Somente leitura (diário já existente): soma o custo já calculado pelo servidor,
            // não recalcula a partir do select (que pode não ter a opção do funcionário daquele dia).
            total += Number($(this).find('.dc-fvalor').val()) || 0;
            return;
        }
        var opt = $(this).find('.dc-fpessoa :selected'); if (!opt.val()) return;
        var valor;
        if ($(this).find('.dc-fregime').val() === 'PRODUCAO') {
            // Colheita por caixa: independe do cadastro do funcionário (CLT/diária/empreita) —
            // anula o cálculo padrão do contrato para esta linha.
            var caixas = Number($(this).find('.dc-fcaixas').val()) || 0;
            var precoCx = Number($(this).find('.dc-fprecocaixa').val()) || 0;
            valor = caixas * precoCx;
        } else {
            var tipo = opt.data('tipo'); var ref = Number(opt.data('ref') || 0); var refExtra = Number(opt.data('refextra') || 0);
            var horas = dcParseHoras($(this).find('.dc-fhoras').val());
            // Valor é sempre calculado a partir do cadastro do funcionário (custo/hora CLT, diária ou empreita fixa).
            valor = dcValorFuncionario(tipo, ref, refExtra, horas);
        }
        $(this).find('.dc-fvalor').val(valor.toFixed(2));
        total += valor;
    });
    $('#dcMaqBody tr').each(function () {
        var opt = $(this).find('.dc-mmaq :selected'); if (!opt.val()) return;
        var isVeic = (opt.data('tipo') === 'VEICULO');
        var unit = isVeic ? Number(opt.data('ck') || 0) : Number(opt.data('ch') || 0);
        total += unit * (Number($(this).find('.dc-muso').val()) || 0);
    });
    $('#dcInsDefBody tr').each(function () {
        var opt = $(this).find('.dc-ipro :selected'); if (!opt.val()) return;
        dcAtualizarLinhaInsumoDef($(this));
        var qtdTotal = lerDecimalBR($(this).find('.dc-iqtdtotal').val());
        total += Number(opt.data('preco') || 0) * qtdTotal;
    });
    $('#dcInsAduBody tr').each(function () {
        var opt = $(this).find('.dc-ipro :selected'); if (!opt.val()) return;
        dcAtualizarLinhaInsumoAdu($(this));
        var qtdTotal = lerDecimalBR($(this).find('.dc-iqtdtotal').val());
        total += Number(opt.data('preco') || 0) * qtdTotal;
    });
    $('#dcDespBody tr').each(function () {
        var opt = $(this).find('.dc-ditem :selected'); if (!opt.val()) return;
        var qtd = Number($(this).find('.dc-dqtd').val()) || 0;
        var valor = Number($(this).find('.dc-dvalor').val()) || 0;
        total += qtd * valor;
    });
    $('#dcTotalPreview').text(dcMoney(total));
}

function dcSalvar() {
    var ehColheita = dcEhColheita();
    var ehCaixa = dcEhAtividadeCaixa();   // Colheita ou Processamento
    var funcionarios = [], caixasInvalidas = false, precoInvalido = false;
    $('#dcFuncBody tr').each(function () {
        var id = $(this).find('.dc-fpessoa').val(); if (!id) return;
        var regime = ehCaixa ? ($(this).find('.dc-fregime').val() || null) : null;
        var caixas = null, precoCx = null;
        if (ehCaixa) {
            // Caixas colhidas vale pros dois regimes na Colheita (é volume físico, não forma de
            // pagamento); no Processamento só é obrigatório para quem é pago por caixa.
            // Preço/caixa só importa em PRODUCAO, que é quem tem o pagamento calculado por ele.
            caixas = parseFloat($(this).find('.dc-fcaixas').val()) || 0;
            if (caixas <= 0 && (ehColheita || regime === 'PRODUCAO')) caixasInvalidas = true;
            if (caixas <= 0) caixas = null;
            if (regime === 'PRODUCAO') {
                precoCx = parseFloat($(this).find('.dc-fprecocaixa').val()) || 0;
                if (precoCx <= 0) precoInvalido = true;
            }
        }
        funcionarios.push({ idPessoa: parseInt(id), tipoFuncionario: $(this).find('.dc-ftipo').val(),
            horasTrabalhadas: dcParseHoras($(this).find('.dc-fhoras').val()),
            valorContratado: parseFloat($(this).find('.dc-fvalor').val()) || 0,
            regimeTrabalho: regime, quantidadeCaixas: caixas, precoPorCaixa: precoCx });
    });
    if (caixasInvalidas) {
        $('#alertDiario').removeClass('d-none').addClass('alert-danger')
            .text(ehColheita ? 'Informe a quantidade de caixas colhidas (maior que zero) para cada funcionário.' : 'Informe a quantidade de caixas (maior que zero) para quem é pago por caixa.');
        return;
    }
    if (precoInvalido) {
        $('#alertDiario').removeClass('d-none').addClass('alert-danger')
            .text('Para funcionários em regime de produção, informe o preço por caixa maior que zero.');
        return;
    }
    var maquinas = [];
    $('#dcMaqBody tr').each(function () {
        var opt = $(this).find('.dc-mmaq :selected'); var idMaq = opt.val(); if (!idMaq) return;
        var isVeic = (opt.data('tipo') === 'VEICULO'); var uso = parseFloat($(this).find('.dc-muso').val()) || 0;
        maquinas.push({ idMaquina: parseInt(idMaq), horasTrabalhadas: isVeic ? 0 : uso, km: isVeic ? uso : 0 });
    });
    // quantidade não é mais enviada: o servidor calcula dosePorUnidade × doseTotalAplicada (defensivo)
    // ou usa quantidadeDireta direto (adubo) — ver DiarioCampoDAO.inserirInsumo. Qual das duas formas
    // vale é determinado por qual das duas tabelas (Defensivo/Adubo) a linha está, não mais por um
    // atributo de categoria no próprio insumo.
    var insumos = [], insumoInvalido = false;
    $('#dcInsDefBody tr').each(function () {
        var id = $(this).find('.dc-ipro').val();
        var dosePu = lerDecimalBR($(this).find('.dc-idosepu').val());
        var doseTotal = lerDecimalBR($(this).find('.dc-idosetotal').val());
        if (!id && dosePu === 0 && doseTotal === 0) return;
        if (!id || dosePu <= 0 || doseTotal <= 0) { insumoInvalido = true; return; }
        insumos.push({ idInsumo: parseInt(id), dosePorUnidade: dosePu, doseTotalAplicada: doseTotal });
    });
    // Adubo de massa vai pela forma (sacos/t/kg) + plantas — o servidor calcula o total e a dose por
    // planta; adubo que não é de massa continua com a quantidade direta na unidade do insumo.
    $('#dcInsAduBody tr').each(function () {
        var $tr = $(this);
        var id = $tr.find('.dc-ipro').val();
        var qtd = lerDecimalBR($tr.find('.dc-iqtdinf').val());
        if (!id && qtd === 0) return;
        if (!id || qtd <= 0) { insumoInvalido = true; return; }
        if (!dcEhMassa($tr.find('.dc-ipro :selected').data('unidade'))) {
            insumos.push({ idInsumo: parseInt(id), quantidadeDireta: qtd });
            return;
        }
        var plantas = parseInt($tr.find('.dc-iplantas').val()) || 0;
        if (plantas <= 0) { insumoInvalido = true; return; }
        var f = dcFormaAdubo($tr);
        insumos.push({ idInsumo: parseInt(id), formaAplicacao: f.forma, pesoSacoKg: f.pesoSacoKg, qtdInformada: qtd, plantasAplicadas: plantas });
    });
    if (insumoInvalido) { $('#alertDiario').removeClass('d-none').addClass('alert-danger').text('Cada insumo precisa de produto e quantidade aplicada: dose por litro/kg + dose total aplicada (defensivo), ou quantidade e nº de plantas (adubo), todos maiores que zero.'); return; }
    var despesas = [], despesaInvalida = false;
    $('#dcDespBody tr').each(function () {
        var id = $(this).find('.dc-ditem').val();
        var qtd = parseFloat($(this).find('.dc-dqtd').val()) || 0;
        if (!id && qtd === 1) return;
        if (!id || qtd <= 0) { despesaInvalida = true; return; }
        despesas.push({ idDespesaCusto: parseInt(id), quantidade: qtd, valorUnitario: parseFloat($(this).find('.dc-dvalor').val()) || 0 });
    });
    if (despesaInvalida) { $('#alertDiario').removeClass('d-none').addClass('alert-danger').text('Cada despesa precisa de um item e quantidade > 0.'); return; }

    var editId = $('#dcId').val();
    var payload = {
        acao: editId ? 'update' : 'create', iddiario: editId ? parseInt(editId) : 0,
        idDiario: editId ? parseInt(editId) : 0,
        data: $('#dcData').val(), idArea: dcAreaId,
        idQuadra: parseInt($('#dcTalhao').val()) || null,
        idCultura: $('#dcCultura').val() ? parseInt($('#dcCultura').val()) : null,
        idResponsavel: parseInt($('#dcResponsavel').val()) || null,
        idTipoAtividade: parseInt($('#dcTipo').val()) || 0,
        status: ($('#dcStatusSel').val() || 'EM_ANDAMENTO'),
        descricao: $('#dcDescricao').val(),
        observacoes: $('#dcObs').val(), funcionarios: funcionarios, maquinas: maquinas, insumos: insumos, despesas: despesas
    };
    if (!payload.idQuadra || !payload.idResponsavel || !payload.idTipoAtividade) {
        $('#alertDiario').removeClass('d-none').addClass('alert-danger').text('Talhão, responsável e tipo de atividade são obrigatórios.'); return;
    }
    // Safra é obrigatória: bloqueia no cliente antes de enviar (o backend também valida).
    if (!dcSafraResolvida) {
        $('#alertDiario').removeClass('d-none').addClass('alert-danger')
            .html('É obrigatório uma <strong>Safra</strong> cadastrada que inclua este talhão e cubra a data. Cadastre/ajuste a safra antes de salvar.');
        return;
    }
    $.ajax({
        url: AGRO_CTX + '/ControllerDiarioCampo', method: 'POST', contentType: 'application/json; charset=utf-8',
        data: JSON.stringify(payload),
        success: function (res) {
            if (res.ok) {
                // Cadastro aberto pela página Colheita ("Cadastrar Colheita"): volta para ela.
                if (dcVeioDaColheita && !editId) {
                    window.location.href = AGRO_CTX + '/view/admin/colheitaestoque.jsp';
                    return;
                }
                mostrarAlerta(res.msg + (res.numeroDiario ? ' (' + res.numeroDiario + ')' : ''), 'success', '#alertPerfil');
                dcCarregarLista();
                // Atividade recém-passada para Execução: mantém o modal aberto e mostra o bloco de
                // "Registrar Nova Execução" para já lançar a primeira equipe/dia, sem precisar reabrir.
                if (editId && payload.status === 'EM_ANDAMENTO') dcEditar(parseInt(editId));
                else $('#modalNovoDiario').modal('hide');
            } else $('#alertDiario').removeClass('d-none').addClass('alert-danger').text(res.msg);
        },
        error: function (xhr) { var m = 'Erro ao salvar diário.'; try { m = JSON.parse(xhr.responseText).msg || m; } catch (e) {} $('#alertDiario').removeClass('d-none').addClass('alert-danger').text(m); }
    });
}

/**
 * Transição PLANEJADA→EM_ANDAMENTO (mesma regra de negócio do endpoint "status")
 * antes de abrir o modal de edição. Sem isso, "Iniciar execução" chamava dcEditar
 * direto com o diário ainda PLANEJADA — a seção "Registrar Nova Execução" só
 * aparece quando o status já é EM_ANDAMENTO, então o usuário via a mensagem
 * "use Registrar nova execução abaixo" apontando para uma seção escondida, sem
 * nenhum jeito de lançar funcionário/despesa na primeira abertura.
 */
function dcIniciarExecucao(id) {
    $.ajax({
        url: AGRO_CTX + '/ControllerDiarioCampo', method: 'POST', contentType: 'application/json; charset=utf-8',
        data: JSON.stringify({ acao: 'status', iddiario: id, status: 'EM_ANDAMENTO' }),
        success: function (res) {
            if (res.ok) { dcCarregarLista(); dcEditar(id); }
            else mostrarAlerta(res.msg, 'danger', '#alertPerfil');
        },
        error: function (xhr) { var m = 'Erro ao iniciar execução.'; try { m = JSON.parse(xhr.responseText).msg || m; } catch (e) {} mostrarAlerta(m, 'danger', '#alertPerfil'); }
    });
}

/** Carrega um diário existente no formulário para edição. */
function dcEditar(id) {
    $.getJSON(AGRO_CTX + '/ControllerDiarioCampo?id=' + id, function (d) {
        dcResetForm();
        dcFuncEditavel = false;
        dcDespEditavel = false;
        $('#btnAddFunc,#btnAddDesp').addClass('d-none');
        // Sem lançamento anterior (ex.: diário recém-passado de PLANEJADA para Execução), a tabela
        // de cima ficaria vazia e somente-leitura ao lado da mesma coisa em "Registrar Nova
        // Execução" — redundante. Só mostra a tabela/aviso de cima quando há histórico para revisar.
        var temFunc = (d.funcionarios || []).length > 0;
        var temDesp = (d.despesas || []).length > 0;
        $('#dcFuncHeader,#dcFuncTableWrap,#dcFuncSoNaExecucao').toggleClass('d-none', !temFunc);
        $('#dcDespHeader,#dcDespTableWrap,#dcDespSoNaExecucao').toggleClass('d-none', !temDesp);
        $('#dcModalTitulo').text('Editar Diário ' + (d.numeroDiario || ''));
        $('#dcId').val(d.idDiario); $('#dcStatus').val(d.status); $('#dcNumero').val(d.numeroDiario);
        // PLANEJADA (agendamento) não tem opção neste modal — "Iniciar execução" já entra como Execução.
        $('#dcStatusSel').val(d.status === 'CONCLUIDA' ? 'CONCLUIDA' : 'EM_ANDAMENTO');
        $('#dcData').val(d.data || ''); $('#dcTalhao').val(d.idQuadra ? String(d.idQuadra) : '');
        $('#dcCultura').val(d.idCultura ? String(d.idCultura) : '');
        $('#dcCulturaNome').val(d.culturaNome || '');
        $('#dcTipo').val(String(d.idTipoAtividade));
        dcAtualizarVisibilidadeInsumos();
        $('#dcDescricao').val(d.descricao || ''); $('#dcObs').val(d.observacoes || '');
        (d.funcionarios || []).forEach(function (f) { dcAddFuncRow(f); });
        (d.maquinas || []).forEach(function (m) { dcAddMaqRow(m); });
        // quantidadeDireta preenchido = foi lançado como Adubo; senão, Defensivo (dose×calda).
        (d.insumos || []).forEach(function (i) { if (i.quantidadeDireta != null) dcAddInsRowAdu(i); else dcAddInsRowDef(i); });
        (d.despesas || []).forEach(function (x) { dcAddDespRow(x); });
        // Responsável definido após montar as linhas (as opções vêm dos funcionários adicionados)
        dcAtualizarResponsavel();
        $('#dcResponsavel').val(d.idResponsavel ? String(d.idResponsavel) : '');
        dcRecalcPreview();
        dcResolverSafra();
        if (d.status === 'EM_ANDAMENTO') dcAbrirExecucaoForm();
        dcBuscarPrecoCaixa();   // execução em mais dias de uma Colheita usa o mesmo preço
        $('#modalNovoDiario').modal('show');
    });
}

/* ===================== EXECUÇÃO EM MÚLTIPLOS DIAS (diário já EM_ANDAMENTO) ===================== */

function dcAbrirExecucaoForm() {
    $('#dcBlocoExecucao').removeClass('d-none');
    $('#exFuncBody,#exDespBody').empty();
    $('#exData').val(new Date().toISOString().slice(0, 10));
    dcAddExecFuncRow();
}

function dcAddExecFuncRow() {
    $('#exFuncBody').append('<tr>' +
        '<td><select class="form-select form-select-sm ex-fpessoa">' + dcOptFunc + '</select></td>' +
        '<td><input class="form-control form-control-sm ex-ftipo" readonly placeholder="—" style="width:90px"></td>' +
        '<td class="dc-col-regime d-none"><select class="form-select form-select-sm ex-fregime">' +
            '<option value="REGIME_SISTEMA">Sistema</option><option value="PRODUCAO">Produção (caixa)</option></select></td>' +
        '<td class="dc-col-regime d-none"><input type="number" class="form-control form-control-sm ex-fprecocaixa d-none" placeholder="Preço/cx" title="Preço por caixa (regime Produção)" min="0.01" step="0.01"></td>' +
        '<td class="dc-col-regime d-none"><input type="number" class="form-control form-control-sm ex-fcaixas d-none" placeholder="Caixas" min="0.01" step="0.01"></td>' +
        '<td><input type="text" class="form-control form-control-sm ex-fhoras" placeholder="2h30 / 45min / 1,5" value=""></td>' +
        '<td><input type="number" class="form-control form-control-sm ex-fvalor bg-light" readonly value="0"></td>' +
        '<td class="text-center"><button type="button" class="btn btn-sm btn-outline-danger ex-rm"><i class="fas fa-trash"></i></button></td></tr>');
    dcAtualizarVisibilidadeColheita();
}

function dcExecRecalcPreview() {
    $('#exFuncBody tr').each(function () {
        var opt = $(this).find('.ex-fpessoa :selected'); if (!opt.val()) return;
        var valor;
        if ($(this).find('.ex-fregime').val() === 'PRODUCAO') {
            var caixas = Number($(this).find('.ex-fcaixas').val()) || 0;
            var precoCx = Number($(this).find('.ex-fprecocaixa').val()) || 0;
            valor = caixas * precoCx;
        } else {
            var tipo = opt.data('tipo'); var ref = Number(opt.data('ref') || 0); var refExtra = Number(opt.data('refextra') || 0);
            var horas = dcParseHoras($(this).find('.ex-fhoras').val());
            valor = dcValorFuncionario(tipo, ref, refExtra, horas);
        }
        $(this).find('.ex-fvalor').val(valor.toFixed(2));
    });
}

function dcSalvarExecucao() {
    var iddiario = parseInt($('#dcId').val()) || 0;
    var dataExec = $('#exData').val();
    if (!iddiario || !dataExec) { $('#alertDiario').removeClass('d-none').addClass('alert-danger').text('Data da execução é obrigatória.'); return; }
    var ehColheita = dcEhColheita();
    var ehCaixa = dcEhAtividadeCaixa();   // Colheita ou Processamento
    var funcionarios = [], caixasInvalidas = false, precoInvalido = false;
    $('#exFuncBody tr').each(function () {
        var id = $(this).find('.ex-fpessoa').val(); if (!id) return;
        var regime = ehCaixa ? ($(this).find('.ex-fregime').val() || null) : null;
        var caixas = null, precoCx = null;
        if (ehCaixa) {
            caixas = parseFloat($(this).find('.ex-fcaixas').val()) || 0;
            if (caixas <= 0 && (ehColheita || regime === 'PRODUCAO')) caixasInvalidas = true;
            if (caixas <= 0) caixas = null;
            if (regime === 'PRODUCAO') {
                precoCx = parseFloat($(this).find('.ex-fprecocaixa').val()) || 0;
                if (precoCx <= 0) precoInvalido = true;
            }
        }
        funcionarios.push({ idPessoa: parseInt(id), tipoFuncionario: $(this).find('.ex-ftipo').val(),
            horasTrabalhadas: dcParseHoras($(this).find('.ex-fhoras').val()),
            valorContratado: parseFloat($(this).find('.ex-fvalor').val()) || 0,
            regimeTrabalho: regime, quantidadeCaixas: caixas, precoPorCaixa: precoCx });
    });
    if (caixasInvalidas) {
        $('#alertDiario').removeClass('d-none').addClass('alert-danger')
            .text(ehColheita ? 'Informe a quantidade de caixas colhidas (maior que zero) para cada funcionário.' : 'Informe a quantidade de caixas (maior que zero) para quem é pago por caixa.');
        return;
    }
    if (precoInvalido) {
        $('#alertDiario').removeClass('d-none').addClass('alert-danger')
            .text('Para funcionários em regime de produção, informe o preço por caixa maior que zero.');
        return;
    }
    var despesas = [];
    $('#exDespBody tr').each(function () {
        var id = $(this).find('.ex-ditem').val(); if (!id) return;
        despesas.push({ idDespesaCusto: parseInt(id), quantidade: parseFloat($(this).find('.ex-dqtd').val()) || 1,
            valorUnitario: parseFloat($(this).find('.ex-dvalor').val()) || 0 });
    });
    if (!funcionarios.length && !despesas.length) { $('#alertDiario').removeClass('d-none').addClass('alert-danger').text('Adicione ao menos um funcionário ou uma despesa para a execução.'); return; }
    $.ajax({
        url: AGRO_CTX + '/ControllerDiarioCampo', method: 'POST', contentType: 'application/json; charset=utf-8',
        data: JSON.stringify({ acao: 'addexecucao', iddiario: iddiario, dataExecucao: dataExec, funcionarios: funcionarios, despesas: despesas }),
        success: function (res) {
            if (res.ok) { mostrarAlerta(res.msg, 'success', '#alertPerfil'); dcCarregarLista(); dcEditar(iddiario); }
            else $('#alertDiario').removeClass('d-none').addClass('alert-danger').text(res.msg);
        },
        error: function (xhr) { var m = 'Erro ao registrar execução.'; try { m = JSON.parse(xhr.responseText).msg || m; } catch (e) {} $('#alertDiario').removeClass('d-none').addClass('alert-danger').text(m); }
    });
}

function dcVerDetalhe(id) {
    $.getJSON(AGRO_CTX + '/ControllerDiarioCampo?id=' + id, function (d) {
        $('#ddNumero').text(d.numeroDiario || '');
        var h = '<div class="row g-2 mb-2">' +
            '<div class="col-4"><small class="text-muted">Data</small><div>' + formatarData(d.data) + '</div></div>' +
            '<div class="col-4"><small class="text-muted">Talhão</small><div>' + (d.quadraNome || '—') + '</div></div>' +
            '<div class="col-4"><small class="text-muted">Cultura</small><div>' + (d.culturaNome || '—') + '</div></div>' +
            '<div class="col-4"><small class="text-muted">Atividade</small><div>' + (d.tipoAtividadeNome || '—') + '</div></div>' +
            '<div class="col-4"><small class="text-muted">Responsável</small><div>' + (d.responsavelNome || '—') + '</div></div>' +
            '<div class="col-4"><small class="text-muted">Status</small><div>' + dcBadgeStatus(d.status) + '</div></div></div>';
        h += '<table class="table table-sm"><tr><th>Mão de obra</th><td class="text-end">' + dcMoney(d.custoMaoObra) + '</td>' +
            '<th>Insumos</th><td class="text-end">' + dcMoney(d.custoInsumos) + '</td></tr>' +
            '<tr><th>Máquinas</th><td class="text-end">' + dcMoney(d.custoMaquinas) + '</td>' +
            '<th>Despesas</th><td class="text-end">' + dcMoney(d.custoDespesas) + '</td></tr>' +
            '<tr><th>Custo Total</th><td class="text-end fw-bold" colspan="3">' + dcMoney(d.custoTotal) + '</td></tr></table>';
        function bloco(t, arr, cols) {
            if (!arr || !arr.length) return '';
            var s = '<h6 class="fw-bold mt-2">' + t + '</h6><table class="table table-sm"><thead><tr>';
            cols.forEach(function (c) { s += '<th>' + c[0] + '</th>'; }); s += '</tr></thead><tbody>';
            arr.forEach(function (o) { s += '<tr>'; cols.forEach(function (c) { s += '<td>' + c[1](o) + '</td>'; }); s += '</tr>'; });
            return s + '</tbody></table>';
        }
        h += bloco('Funcionários', d.funcionarios, [['Nome', function (o) { return o.nomePessoa || o.idPessoa; }], ['Tipo', function (o) { return o.tipoFuncionario || ''; }], ['Execução', function (o) { return o.dataExecucao ? formatarData(o.dataExecucao) : ''; }], ['Regime', function (o) { return o.regimeTrabalho === 'PRODUCAO' ? 'Produção' : (o.regimeTrabalho === 'REGIME_SISTEMA' ? 'Sistema' : ''); }], ['Horas', function (o) { return o.horasTrabalhadas; }], ['Caixas', function (o) { return o.quantidadeCaixas != null ? o.quantidadeCaixas : '—'; }], ['Custo', function (o) { return dcMoney(o.custo); }]]);
        h += bloco('Máquinas', d.maquinas, [['Maquinário', function (o) { return o.nome || ''; }], ['Categoria', function (o) { return o.categoria || ''; }], ['Uso', function (o) { return (o.categoria === 'VEICULO' ? o.km + ' km' : o.horasTrabalhadas + ' h'); }], ['Custo', function (o) { return dcMoney(o.custo); }]]);
        h += bloco('Insumos', d.insumos, [['Insumo', function (o) { return o.insumoNome || o.idInsumo; }], ['Dose (por L/kg)', function (o) { return o.dosePorUnidade != null ? o.dosePorUnidade : '—'; }], ['Dose Total Aplicada', function (o) { return o.doseTotalAplicada != null ? o.doseTotalAplicada : '—'; }], ['Aplicação (adubo)', function (o) {
            if (!o.formaAplicacao) return '—';
            var q = exibirDecimalNatural(o.qtdInformada, 3);
            var forma = o.formaAplicacao === 'SACO' ? q + ' saco(s) de ' + Number(o.pesoSacoKg) + ' kg' : (o.formaAplicacao === 'TONELADA' ? q + ' t' : q + ' kg');
            return forma + ' em ' + o.plantasAplicadas + ' plantas = ' + exibirDecimalNatural(o.quantidadePorPlantaKg, 3) + ' kg/planta';
        }], ['Qtd. Total Aplicada', function (o) { return o.quantidade + ' ' + (o.unidade || ''); }], ['Custo', function (o) { return dcMoney(o.custo); }], ['Baixado', function (o) { return o.baixado ? 'Sim' : 'Não'; }]]);
        h += bloco('Despesas', d.despesas, [['Despesa', function (o) { return o.descricao || o.idDespesaCusto; }], ['Classif.', function (o) { return o.classificacao === 'VARIAVEL' ? 'Variável' : 'Fixo'; }], ['Execução', function (o) { return o.dataExecucao ? formatarData(o.dataExecucao) : ''; }], ['Qtd', function (o) { return o.quantidade; }], ['Valor unit.', function (o) { return dcMoney(o.valorUnitario); }], ['Subtotal', function (o) { return dcMoney(o.valor); }]]);
        $('#ddCorpo').html(h);
        var podeFinalizar = (d.status === 'PLANEJADA' || d.status === 'EM_ANDAMENTO');
        $('#btnFinalizarDiario').toggleClass('d-none', !podeFinalizar).off('click').on('click', function () { dcFinalizar(d.idDiario); });
        $('#modalDetalheDiario').modal('show');
    });
}

function dcFinalizar(id) {
    if (!confirm('Finalizar a atividade? Isso calcula os custos e dá baixa no estoque dos insumos.')) return;
    $.ajax({
        url: AGRO_CTX + '/ControllerDiarioCampo', method: 'POST', contentType: 'application/json',
        data: JSON.stringify({ acao: 'finalizar', iddiario: id }),
        success: function (res) { $('#modalDetalheDiario').modal('hide'); mostrarAlerta(res.msg, res.ok ? 'success' : 'danger', '#alertPerfil'); dcCarregarLista(); },
        error: function (xhr) { var m = 'Erro ao finalizar.'; try { m = JSON.parse(xhr.responseText).msg || m; } catch (e) {} mostrarAlerta(m, 'danger', '#alertPerfil'); $('#modalDetalheDiario').modal('hide'); }
    });
}

function dcAddTipo() {
    var nome = prompt('Nome do novo tipo de atividade:');
    if (!nome || !nome.trim()) return;
    $.ajax({
        url: AGRO_CTX + '/ControllerDiarioCampo', method: 'POST', contentType: 'application/json; charset=utf-8',
        data: JSON.stringify({ acao: 'addtipoatividade', nome: nome.trim() }),
        success: function () { dcCarregarTipos(); }
    });
}

/******************************************************************************************************/
/* MÓDULO MAQUINÁRIO — maquina.jsp                                                                     */
/******************************************************************************************************/
$(document).ready(function () {
    if (!$('#tabelaMaquinas').length) return;
    $('#tabelaMaquinas').DataTable({ language: { url: DT_IDIOMA_URL } });
    mqCarregar();
    autoRefreshTabela('maquinas', mqCarregar, 15000);
    $('#mqTipo').on('change', mqToggleBlocos);
    // Custo/Hora = combustível + manutenção + depreciação (soma automática ao digitar os componentes)
    $('#mqComb, #mqManut, #mqDeprec').on('input', mqRecalcularCustoHora);
    $('#btnNovoMaquina').on('click', mqNovo);
    $('#btnSalvarMaquina').on('click', mqSalvar);
});
/** Soma os componentes (combustível + manutenção + depreciação) no Custo/Hora. */
function mqRecalcularCustoHora() {
    var comb = parseFloat($('#mqComb').val()) || 0;
    var manut = parseFloat($('#mqManut').val()) || 0;
    var deprec = parseFloat($('#mqDeprec').val()) || 0;
    $('#mqCustoHora').val((comb + manut + deprec).toFixed(2));
}
function mqToggleBlocos() {
    var tipo = $('#mqTipo').val();
    var veic = tipo === 'VEICULO';
    var implemento = tipo === 'IMPLEMENTO';
    $('#mqBlocoKm').toggleClass('d-none', !veic);
    $('#mqBlocoHora').toggleClass('d-none', veic);
    // Implemento não tem combustível próprio (a energia vem do trator) nem operador próprio
    // (o operador do trator já é contabilizado). Custo/hora = depreciação + manutenção.
    $('#mqCombWrap').toggleClass('d-none', implemento);
    if (implemento) {
        $('#mqComb').val('0');
        mqRecalcularCustoHora();
        $('#mqTituloHora').text('Custo por Hora (Implemento)');
        $('#mqAjudaHora').text('Implemento é tracionado pelo trator: sem combustível e sem operador próprios. Custo/Hora = manutenção + depreciação. O operador (do trator) é lançado à parte, via funcionário.');
    } else {
        $('#mqTituloHora').text('Custo por Hora (Trator)');
        $('#mqAjudaHora').text('Se preencher os componentes, o Custo/Hora é a soma deles (combustível + manutenção + depreciação). A mão de obra do operador é lançada à parte, via funcionário.');
    }
}
function mqCarregar() {
    var t = $('#tabelaMaquinas').DataTable();
    $.get(AGRO_CTX + '/ControllerMaquina', function (lista) {
        t.clear();  // limpa só quando os dados chegam (sem flash de tabela vazia)
        lista.forEach(function (m) {
            t.row.add([m.nome, m.tipo, m.marca || '—', m.identificacao || '—',
                'R$ ' + Number(m.custoHora || 0).toLocaleString('pt-BR', {minimumFractionDigits:2}),
                'R$ ' + Number(m.custoKm || 0).toLocaleString('pt-BR', {minimumFractionDigits:2}),
                m.situacao ? '<span class="badge bg-success">Ativo</span>' : '<span class="badge bg-secondary">Inativo</span>',
                '<button class="btn-acao btn-acao-editar" title="Editar" onclick="mqEditar(' + m.idMaquina + ')"><i class="fas fa-pen-to-square"></i></button> ' +
                '<button class="btn-acao ' + (m.situacao ? 'btn-acao-desativar' : 'btn-acao-ativar') + '" title="' + (m.situacao ? 'Desativar' : 'Ativar') + '" onclick="mqToggle(' + m.idMaquina + ',' + m.situacao + ')"><i class="fas ' + (m.situacao ? 'fa-ban' : 'fa-circle-check') + '"></i></button>'
            ]).draw(false);
        });
    });
}
function mqNovo() {
    $('#alertaMaquina').addClass('d-none').text(''); $('#modalMaquinaTitulo').html('<i class="fas fa-tractor me-2"></i>Novo Maquinário');
    $('#mqId,#mqNome,#mqMarca,#mqIdent').val(''); $('#mqTipo').val('TRATOR');
    $('#mqComb,#mqManut,#mqDeprec,#mqCustoHora,#mqCustoKm').val('0');
    mqToggleBlocos(); $('#modalMaquina').modal('show');
}
function mqEditar(id) {
    $.getJSON(AGRO_CTX + '/ControllerMaquina?id=' + id, function (m) {
        $('#alertaMaquina').addClass('d-none').text(''); $('#modalMaquinaTitulo').html('<i class="fas fa-pen-to-square me-2"></i>Editar Maquinário');
        $('#mqId').val(m.idMaquina); $('#mqNome').val(m.nome); $('#mqTipo').val(m.tipo);
        $('#mqMarca').val(m.marca || ''); $('#mqIdent').val(m.identificacao || '');
        $('#mqComb').val(m.combustivelHora || 0); $('#mqManut').val(m.manutencaoHora || 0); $('#mqDeprec').val(m.depreciacaoHora || 0);
        $('#mqCustoHora').val(m.custoHora || 0); $('#mqCustoKm').val(m.custoKm || 0);
        mqToggleBlocos(); $('#modalMaquina').modal('show');
    });
}
function mqSalvar() {
    var id = $('#mqId').val();
    var data = { acao: id ? 'update' : 'create', idMaquina: id ? parseInt(id) : 0,
        nome: $('#mqNome').val().trim(), tipo: $('#mqTipo').val(), marca: $('#mqMarca').val(), identificacao: $('#mqIdent').val(),
        combustivelHora: parseFloat($('#mqComb').val()) || 0, manutencaoHora: parseFloat($('#mqManut').val()) || 0,
        depreciacaoHora: parseFloat($('#mqDeprec').val()) || 0, custoHora: parseFloat($('#mqCustoHora').val()) || 0,
        custoKm: parseFloat($('#mqCustoKm').val()) || 0, situacao: true };
    if (!data.nome) { $('#alertaMaquina').removeClass('d-none').addClass('alert-danger').text('Informe o nome.'); return; }
    $.ajax({ url: AGRO_CTX + '/ControllerMaquina', method: 'POST', contentType: 'application/json; charset=utf-8', data: JSON.stringify(data),
        success: function (res) { if (res.ok) { $('#modalMaquina').modal('hide'); mostrarAlerta(res.msg, 'success', '#alerta'); mqCarregar(); } else $('#alertaMaquina').removeClass('d-none').addClass('alert-danger').text(res.msg); },
        error: function (xhr) { var m = 'Erro ao salvar.'; try { m = JSON.parse(xhr.responseText).msg || m; } catch (e) {} $('#alertaMaquina').removeClass('d-none').addClass('alert-danger').text(m); } });
}
function mqToggle(id, situacao) {
    $.ajax({ url: AGRO_CTX + '/ControllerMaquina', method: 'POST', contentType: 'application/json', data: JSON.stringify({ acao: 'delete', idMaquina: id, situacao: situacao }),
        success: function (res) { mostrarAlerta(res.msg, 'info', '#alerta'); mqCarregar(); },
        error: function () { mostrarAlerta('Erro ao alterar situação.', 'danger', '#alerta'); } });
}

/******************************************************************************************************/
/* MÓDULO SAFRA — safra.jsp (CRUD + vínculo de talhões)                                               */
/******************************************************************************************************/
var sfCacheTalhoes = [], sfCacheAreas = [], sfOptCultura = '';
function sfMoney(v) { return 'R$ ' + Number(v || 0).toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 }); }
function sfBadge(s) {
    var m = { PLANEJADA: 'bg-secondary', EM_ANDAMENTO: 'bg-info text-dark', FINALIZADA: 'bg-success' };
    return '<span class="badge ' + (m[s] || 'bg-secondary') + '">' + (s || '') + '</span>';
}

$(document).ready(function () {
    if (!$('#tabelaSafras').length) return;
    $('#tabelaSafras').DataTable({ language: { url: DT_IDIOMA_URL } });
    $.getJSON(AGRO_CTX + '/ControllerSafra?culturas=1', function (cs) {
        sfOptCultura = '<option value="">—</option>';
        cs.forEach(function (c) { sfOptCultura += '<option value="' + c.id + '">' + c.nome + '</option>'; });
    });
    $.getJSON(AGRO_CTX + '/ControllerSafra?talhoes=1', function (ts) { sfCacheTalhoes = ts; });
    $.getJSON(AGRO_CTX + '/ControllerAreaProducao', function (as) { sfCacheAreas = as; });
    sfCarregar();
    autoRefreshTabela('safras', sfCarregar, 15000);
    $('#btnNovaSafra').on('click', sfNovo);
    $('#btnSalvarSafra').on('click', sfSalvar);
    $('#sfArea').on('change', function () { sfRenderTalhoesPorArea(parseInt($(this).val()) || 0); });
});

function sfCarregar() {
    var t = $('#tabelaSafras').DataTable();
    $.get(AGRO_CTX + '/ControllerSafra', function (lista) {
        t.clear();  // limpa só quando os dados chegam (sem flash de tabela vazia)
        lista.forEach(function (s) {
            var periodo = (s.dataInicial ? formatarData(s.dataInicial) : '') + ' a ' + (s.dataFinal ? formatarData(s.dataFinal) : '');
            var acoes = '<a class="btn-acao btn-acao-editar" title="Editar" href="#" onclick="sfEditar(' + s.idSafra + ');return false;"><i class="fas fa-pen-to-square"></i></a> ' +
                '<a class="btn-acao" title="Resumo financeiro" href="' + CTX + '/view/admin/resumoSafra.jsp?id=' + s.idSafra + '"><i class="fas fa-chart-pie"></i></a>';
            t.row.add([s.nome, s.culturaNome || '—', periodo, sfBadge(s.status),
                Number(s.estimativaProducao || 0).toLocaleString('pt-BR') + ' ' + (s.unidadeProducao || ''), acoes]).draw(false);
        });
    });
}

function sfOptAreas() {
    var o = '<option value="">Selecione</option>';
    sfCacheAreas.forEach(function (a) { o += '<option value="' + a.idAreaProducao + '">' + a.PropriedadeAreaProducao + '</option>'; });
    return o;
}
/** Preenche a tabela de talhões com TODAS as quadras da área escolhida (auto), sem seleção manual.
 *  existentes: mapa idQuadra -> areaDestinadaHa, para pré-preencher em modo edição. */
function sfRenderTalhoesPorArea(idArea, existentes) {
    existentes = existentes || {};
    var qs = sfCacheTalhoes.filter(function (q) { return q.idAreaProducao === idArea; });
    var $b = $('#sfTalhaoBody').empty();
    if (!idArea) { $b.append('<tr><td colspan="2" class="text-center text-muted py-2">Selecione uma Área de Produção acima.</td></tr>'); return; }
    if (!qs.length) { $b.append('<tr><td colspan="2" class="text-center text-muted py-2">Esta área não tem talhões cadastrados.</td></tr>'); return; }
    qs.forEach(function (q) {
        var areaDest = existentes[q.idQuadra] != null ? existentes[q.idQuadra] : (q.areaHa || 0);
        $b.append('<tr><td><input type="hidden" class="sf-tq" value="' + q.idQuadra + '">' + q.nome +
            ' <span class="text-muted small">(' + q.numeroPlantas + ' pl, ' + Number(q.areaHa || 0) + ' ha reais)</span></td>' +
            '<td><input type="number" step="0.01" min="0" class="form-control form-control-sm sf-tarea" value="' + areaDest + '"></td></tr>');
    });
}

function sfNovo() {
    if (!sfCacheAreas.length) {
        preReqAlerta('#alerta', 'Para cadastrar uma Safra é preciso ter <strong>Área de Produção</strong> cadastrada.',
            CTX + '/view/admin/areaproducao.jsp', 'Ir para Área de Produção');
        return;
    }
    $('#alertaSafra').addClass('d-none').text(''); $('#modalSafraTitulo').html('<i class="fas fa-seedling me-2"></i>Nova Safra');
    $('#sfId,#sfNome').val(''); $('#sfCultura').html(sfOptCultura); $('#sfArea').html(sfOptAreas());
    $('#sfStatus').val('PLANEJADA'); $('#sfUnidade').val('SACA');
    $('#sfDataIni,#sfDataFim').val(''); $('#sfEstimativa,#sfDespFixas').val('0');
    sfRenderTalhoesPorArea(0);
    $('#modalSafra').modal('show');
}
function sfEditar(id) {
    $.getJSON(AGRO_CTX + '/ControllerSafra?id=' + id, function (s) {
        $('#alertaSafra').addClass('d-none').text(''); $('#modalSafraTitulo').html('<i class="fas fa-pen-to-square me-2"></i>Editar Safra');
        $('#sfCultura').html(sfOptCultura); $('#sfArea').html(sfOptAreas());
        $('#sfId').val(s.idSafra); $('#sfNome').val(s.nome);
        $('#sfCultura').val(s.idCulturaPrincipal ? String(s.idCulturaPrincipal) : '');
        $('#sfArea').val(s.idAreaProducao ? String(s.idAreaProducao) : '');
        $('#sfStatus').val(s.status); $('#sfUnidade').val(s.unidadeProducao || 'SACA');
        $('#sfDataIni').val(s.dataInicial || ''); $('#sfDataFim').val(s.dataFinal || '');
        $('#sfEstimativa').val(s.estimativaProducao || 0); $('#sfDespFixas').val(s.despesasFixas || 0);
        var existentes = {};
        (s.talhoes || []).forEach(function (t) { existentes[t.idQuadra] = t.areaDestinadaHa || 0; });
        sfRenderTalhoesPorArea(s.idAreaProducao || 0, existentes);
        $('#modalSafra').modal('show');
    });
}
function sfSalvar() {
    var talhoes = [], invalido = false;
    $('#sfTalhaoBody tr').each(function () {
        var idq = $(this).find('.sf-tq').val(); if (!idq) return;
        var q = sfCacheTalhoes.filter(function (x) { return String(x.idQuadra) === String(idq); })[0];
        var areaMax = q ? Number(q.areaHa || 0) : 0;
        var area = parseFloat($(this).find('.sf-tarea').val()) || 0;
        if (areaMax > 0 && area > areaMax) invalido = true;
        talhoes.push({ idQuadra: parseInt(idq), areaDestinadaHa: area });
    });
    if (invalido) { $('#alertaSafra').removeClass('d-none').addClass('alert-danger').text('Há talhão com área destinada maior que a área real. Corrija antes de salvar.'); return; }
    var id = $('#sfId').val();
    var payload = {
        acao: id ? 'update' : 'create', idSafra: id ? parseInt(id) : 0,
        nome: $('#sfNome').val().trim(),
        idAreaProducao: parseInt($('#sfArea').val()) || null,
        idCulturaPrincipal: $('#sfCultura').val() ? parseInt($('#sfCultura').val()) : null,
        dataInicial: $('#sfDataIni').val() || null, dataFinal: $('#sfDataFim').val() || null,
        status: $('#sfStatus').val(), estimativaProducao: parseFloat($('#sfEstimativa').val()) || 0,
        unidadeProducao: $('#sfUnidade').val(), despesasFixas: parseFloat($('#sfDespFixas').val()) || 0,
        talhoes: talhoes
    };
    if (!payload.nome || !payload.idAreaProducao || !payload.dataInicial || !payload.dataFinal) {
        $('#alertaSafra').removeClass('d-none').addClass('alert-danger').text('Nome, Área de Produção e datas são obrigatórios.'); return;
    }
    $.ajax({ url: AGRO_CTX + '/ControllerSafra', method: 'POST', contentType: 'application/json; charset=utf-8', data: JSON.stringify(payload),
        success: function (res) { if (res.ok) { $('#modalSafra').modal('hide'); mostrarAlerta(res.msg, 'success', '#alerta'); sfCarregar(); } else $('#alertaSafra').removeClass('d-none').addClass('alert-danger').text(res.msg); },
        error: function (xhr) { var m = 'Erro ao salvar safra.'; try { m = JSON.parse(xhr.responseText).msg || m; } catch (e) {} $('#alertaSafra').removeClass('d-none').addClass('alert-danger').text(m); } });
}

/******************************************************************************************************/
/* RESUMO FINANCEIRO DA SAFRA — resumoSafra.jsp                                                       */
/******************************************************************************************************/
$(document).ready(function () {
    if (!$('#resumoSafra').length) return;
    var id = new URLSearchParams(window.location.search).get('id');
    $.getJSON(AGRO_CTX + '/ControllerSafra?resumo=' + id, function (r) {
        if (!r || !r.safra) return;
        var s = r.safra;
        $('#rsNome').text(s.nome || ''); $('#rsStatus').html(sfBadge(s.status));
        $('#rsCultura').text(s.culturaNome || '—');
        $('#rsPeriodo').text((s.dataInicial ? formatarData(s.dataInicial) : '') + ' a ' + (s.dataFinal ? formatarData(s.dataFinal) : ''));
        $('#rsUnidade').text(s.unidadeProducao === 'TONELADA' ? 'Tonelada' : 'Saca');
        $('#rsAtiv').text(r.qtdAtividades); $('#rsPlantas').text(Number(r.totalPlantas || 0).toLocaleString('pt-BR'));
        $('#rsArea').text(Number(r.totalAreaHa || 0).toLocaleString('pt-BR'));
        $('#rsCustoTotal').text(sfMoney(r.custoTotal));
        $('#rsPorPlanta').text('R$ ' + Number(r.custoPorPlanta || 0).toLocaleString('pt-BR', {minimumFractionDigits:4, maximumFractionDigits:4}));
        $('#rsPorHa').text(sfMoney(r.custoPorHa)); $('#rsPorSaca').text(sfMoney(r.custoPorUnidade));
        $('#rsIns').text(sfMoney(r.custoInsumos)); $('#rsMaq').text(sfMoney(r.custoMaquinas));
        $('#rsMo').text(sfMoney(r.custoMaoObra)); $('#rsFix').text(sfMoney(r.despesasFixas));
        $('#rsDesp').text(sfMoney(r.custoDespesas));
        $('#rsPctIns').text((r.pctInsumos || 0) + '%'); $('#rsPctMaq').text((r.pctMaquinas || 0) + '%');
        $('#rsPctMo').text((r.pctMaoObra || 0) + '%'); $('#rsPctFix').text((r.pctDespesasFixas || 0) + '%');
        $('#rsPctDesp').text((r.pctDespesas || 0) + '%');
        $('#rsTotalFoot').text(sfMoney(r.custoTotal));
        var barra = '';
        barra += '<div class="progress-bar bg-primary" style="width:' + (r.pctInsumos || 0) + '%" title="Insumos"></div>';
        barra += '<div class="progress-bar bg-secondary" style="width:' + (r.pctMaquinas || 0) + '%" title="Maquinário"></div>';
        barra += '<div class="progress-bar bg-success" style="width:' + (r.pctMaoObra || 0) + '%" title="Mão de obra"></div>';
        barra += '<div class="progress-bar bg-info" style="width:' + (r.pctDespesas || 0) + '%" title="Despesas de execução"></div>';
        barra += '<div class="progress-bar bg-warning" style="width:' + (r.pctDespesasFixas || 0) + '%" title="Despesas fixas"></div>';
        $('#rsBarra').html(barra);
        rsCacheRateio = r.rateio || [];
        var alimentos = [], vistos = {};
        rsCacheRateio.forEach(function (x) { if (x.alimento && !vistos[x.alimento]) { vistos[x.alimento] = true; alimentos.push(x.alimento); } });
        var optsAlim = '<option value="">Todos</option>';
        alimentos.sort().forEach(function (a) { optsAlim += '<option value="' + rsEsc(a) + '">' + rsEsc(a) + '</option>'; });
        $('#rsFiltroAlimento').html(optsAlim);
        // Sem despesas fixas os totais das duas colunas coincidem — a frase sobre a diferença só aparece quando há fixas.
        $('#rsRateioNota').text((Number(r.despesasFixas) > 0
            ? 'O custo dividido inclui as Despesas Fixas da safra (' + sfMoney(r.despesasFixas) +
              '), que não são lançadas por quadra; por isso os totais das colunas não coincidem. '
            : '') + 'Só atividades concluídas entram no custo.');
        rsRenderRateio();
    });
    $('#rsFiltroAlimento').off('change').on('change', rsRenderRateio);
});

var rsCacheRateio = [];

/** Renderiza a tabela de rateio por talhão, filtrando por alimento quando selecionado. */
function rsRenderRateio() {
    var filtro = $('#rsFiltroAlimento').val();
    var lista = filtro ? rsCacheRateio.filter(function (x) { return x.alimento === filtro; }) : rsCacheRateio;
    var $b = $('#rsRateioBody').empty();
    if (!lista.length) $b.append('<tr><td colspan="7" class="text-center text-muted py-2">Sem talhões.</td></tr>');
    var totalRateado = 0, totalReal = 0, totalDif = 0, totalPendQtd = 0, totalPendValor = 0;
    lista.forEach(function (x) {
        var rateado = Number(x.custoRateado) || 0, real = Number(x.custoReal) || 0;
        var qtdPend = Number(x.qtdPendentes) || 0, valPend = Number(x.valorPendente) || 0;
        totalRateado += rateado; totalReal += real; totalPendQtd += qtdPend; totalPendValor += valPend;
        var badge = qtdPend > 0
            ? ' <span class="badge bg-warning text-dark" title="Atividades planejadas/em andamento: só entram no custo depois de finalizadas no Diário de Campo.">' +
              qtdPend + ' em andamento (' + sfMoney(valPend) + ')</span>'
            : '';
        var semLanc = real === 0 && qtdPend === 0;
        var celReal = semLanc ? '<span class="text-muted fst-italic small">Sem lançamentos</span>' : sfMoney(real) + badge;
        var celDif = '';
        if (real === 0 && qtdPend > 0) {
            // Nada concluído ainda, só atividades em andamento: a diferença não diz nada (não é economia).
            celDif = '<span class="text-muted" title="Ainda não há atividade concluída: a diferença só faz sentido depois de finalizar as atividades no Diário.">—</span>';
        } else if (!semLanc) {
            var dif = real - rateado; totalDif += dif;
            celDif = '<span class="' + rsClasseDif(dif) + '">' + sfMoney(dif) + '</span>';
        }
        $b.append('<tr><td>' + rsEsc(x.talhao) + '</td><td>' + rsEsc(x.alimento || '—') + '</td><td class="text-end">' + Number(x.numeroPlantas).toLocaleString('pt-BR') +
            '</td><td class="text-end">' + rsEsc(x.participacaoPct) + '%</td><td class="text-end">' + sfMoney(rateado) +
            '</td><td class="text-end">' + celReal + '</td><td class="text-end">' + celDif + '</td></tr>');
    });
    $('#rsFiltroRateadoTotal').text(sfMoney(totalRateado));
    $('#rsFiltroRealTotal').text(sfMoney(totalReal));
    $('#rsFiltroDifTotal').html('<span class="' + rsClasseDif(totalDif) + '">' + sfMoney(totalDif) + '</span>')
        .attr('title', 'Soma das diferenças dos talhões com custo lançado (exclui "Sem lançamentos" e os que só têm atividades em andamento).');
    $('#rsFiltroPendRow').toggleClass('d-none', totalPendQtd === 0);
    $('#rsFiltroPendTotal').text('Pendente (fora do custo): ' + totalPendQtd + ' atividade(s) em andamento/planejada(s), ' + sfMoney(totalPendValor));
}

/** Verde quando gastou menos que a sua parte (negativo), vermelho quando mais (positivo). */
function rsClasseDif(v) { return v < 0 ? 'text-success' : (v > 0 ? 'text-danger' : ''); }

/** Escapa texto vindo do banco antes de montar HTML. */
function rsEsc(v) {
    return String(v == null ? '' : v).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}

/**
 * Exibe um aviso de pré-requisito (módulo dependente sem dados) num container de
 * alerta, com link para o cadastro que falta. Usado pelos guards de cadastro.
 */
function preReqAlerta(sel, msg, href, txt) {
    var link = href ? ' <a href="' + href + '" class="alert-link fw-bold">' + (txt || 'Cadastrar agora') + ' <i class="fas fa-arrow-right ms-1"></i></a>' : '';
    $(sel).removeClass('d-none alert-success alert-info alert-danger').addClass('alert alert-warning')
          .html('<i class="fas fa-triangle-exclamation me-1"></i>' + msg + link);
    try { $('html,body').animate({ scrollTop: 0 }, 200); } catch (e) {}
}

/** Oculta e limpa o conteúdo de um container de alerta. Usado em: areaproducao.jsp */
function limparAlerta(sel) { $(sel).removeClass().addClass('alert d-none').empty(); }

/* ========== folhapagamento.jsp ========== */
var tabelaFolha;

$(document).ready(function () {
    if (!$('#tabelaFolha').length) return;

    tabelaFolha = $('#tabelaFolha').DataTable({
        language: { url: DT_IDIOMA_URL },
        paging: false
    });

    let mesAtual = new Date().toISOString().slice(0, 7);
    $('#periodoGerar').val(mesAtual);
    $('#periodoVer').val(mesAtual);

    // Pré-requisito: a Folha depende de Funcionários cadastrados.
    $.getJSON(AGRO_CTX + '/ControllerFuncionario', function (fs) {
        if (!(fs || []).length) {
            preReqAlerta('#alertPage', 'Para gerar a Folha de Pagamento é preciso ter <strong>Funcionários</strong> cadastrados.',
                CTX + '/view/admin/ColaboCadastro.jsp', 'Cadastrar Funcionário');
            $('#btnGerarFolha, #btnVerFolha, #btnExcelFolha, #btnPdfFolha').prop('disabled', true);
        }
    });

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
        url: AGRO_CTX + '/ControleColaborador',
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
    $.get(AGRO_CTX + '/ControleColaborador?periodo=' + periodo, function (dados) {
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
    window.location.href = AGRO_CTX + '/RelatorioExportServlet?tipo=' + tipo + '&modulo=folhapagamento&periodo=' + periodo;
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
    $.get(AGRO_CTX + '/ControllerPontoEletronico?tipo=funcionarios', function (lista) {
        if (!(lista || []).length) {
            preReqAlerta('#alerta', 'Para usar o Ponto Eletrônico é preciso ter ao menos um <strong>Funcionário</strong> cadastrado.',
                CTX + '/view/admin/ColaboCadastro.jsp', 'Cadastrar Funcionário');
            $('#btnBuscar').prop('disabled', true);
            return;
        }
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
    $.get(AGRO_CTX + '/ControllerPontoEletronico?tipo=pontos&idfuncionario=' + id + '&periodo=' + periodo, function (lista) {
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
    $.get(AGRO_CTX + '/ControllerPontoEletronico?tipo=pontos&idfuncionario=' + id + '&periodo=' + getPeriodo(), function (lista) {
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
    $.get(AGRO_CTX + '/ControllerPontoEletronico?tipo=faltas&idfuncionario=' + id + '&periodo=' + periodo, function (lista) {
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
    $.get(AGRO_CTX + '/ControllerPontoEletronico?tipo=vales&idfuncionario=' + id + '&periodo=' + periodo, function (lista) {
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
        url: AGRO_CTX + '/ControllerPontoEletronico',
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

/******************************************************************************************************/
/* MÓDULO ESTOQUE DE INSUMOS — estoque.jsp                                                            */
/******************************************************************************************************/

/* Cache dos insumos carregados, usado para preencher selects e saldo da saída. */
if (typeof insumosCache === "undefined") { var insumosCache = []; }

/** Unidades disponíveis por grandeza. */
const UNIDADES_POR_GRANDEZA = {
    MASSA:      [["G", "Grama (g)"], ["KG", "Quilograma (kg)"], ["T", "Tonelada (t)"]],
    CAPACIDADE: [["ML", "Mililitro (ml)"], ["L", "Litro (L)"]],
    UNIDADE:    [["UN", "Unidade (un)"]]
};

/** Formata quantidade (até 3 casas, sem zeros à toa). */
function fmtQtd(v) {
    return Number(v || 0).toLocaleString('pt-BR', { maximumFractionDigits: 3 });
}
function badgeCategoriaInsumo(cat) {
    return (cat === "ADUBO")
        ? '<span class="badge bg-success">Adubo</span>'
        : '<span class="badge bg-primary">Defensivo</span>';
}

/** Recarrega a DataTable de insumos aplicando o filtro de categoria. */
function CarregarInsumos() {
    const cat = $("#filtroCategoria").val() || "";
    const url = AGRO_CTX + "/ControllerInsumo" + (cat ? "?categoria=" + cat : "");
    // Já inicializada: atualiza a URL (filtro categoria) e recarrega sem destruir.
    if ($.fn.DataTable.isDataTable('#tabelaInsumos')) {
        $('#tabelaInsumos').DataTable().ajax.url(url).load(null, false);
        return;
    }
    $('#tabelaInsumos').DataTable({
        "ajax": { "url": url, "method": "GET", "dataSrc": function (json) { insumosCache = json; return json; } },
        "columns": [
            { "data": "nome" },
            { "data": "categoria", "render": badgeCategoriaInsumo },
            {
                "data": null, "className": "text-end",
                "render": function (row) {
                    const txt = fmtQtd(row.quantidadeDisponivel) + " " + (row.unidade || "");
                    return row.abaixoMinimo
                        ? '<span class="text-danger fw-bold">' + txt + ' <i class="fas fa-triangle-exclamation" title="Abaixo do mínimo"></i></span>'
                        : txt;
                }
            },
            { "data": "precoMedio", "className": "text-end", "render": function (v) { return "R$ " + formatBRLnum(v); } },
            { "data": "valorTotal", "className": "text-end", "render": function (v) { return "R$ " + formatBRLnum(v); } },
            { "data": null, "className": "text-end", "render": function (row) { return fmtQtd(row.estoqueMinimo) + " " + (row.unidade || ""); } },
            {
                "data": "situacao", "render": function (v) {
                    return v ? '<span class="badge bg-success">Ativo</span>' : '<span class="badge bg-secondary">Inativo</span>';
                }
            },
            {
                "data": null, "className": "text-end", "orderable": false,
                "render": function (row) {
                    return `<button class="btn-acao btn-acao-editar" title="Editar" onclick="abrirModalEditarInsumo(${row.idInsumo})"><i class="fas fa-pen-to-square"></i></button>
                        <button class="btn-acao" title="Movimentações" onclick="verMovimentos(${row.idInsumo}, '${(row.nome||'').replace(/'/g,"\\'")}')"><i class="fas fa-clock-rotate-left"></i></button>
                        <button class="btn-acao ${row.situacao ? 'btn-acao-desativar' : 'btn-acao-ativar'}" title="${row.situacao ? 'Desativar' : 'Ativar'}" onclick="toggleInsumo(${row.idInsumo}, ${row.situacao})"><i class="fas ${row.situacao ? 'fa-ban' : 'fa-circle-check'}"></i></button>`;
                }
            }
        ],
        "language": { "url": DT_IDIOMA_URL }
    });
}

/** Cards de resumo do estoque. */
function carregarResumoEstoque() {
    $.getJSON(AGRO_CTX + "/ControllerInsumo?acao=dados", function (d) {
        $("#cardTotalInsumos").text(d.total);
        $("#cardAbaixoMinimo").text(d.abaixoMinimo);
        $("#cardValorEstoque").text("R$ " + formatBRLnum(d.valorEstoque));
    });
}

/** Adubo de massa usa o select de saco padrão (5/10/25/50 kg) no lugar do peso livre. */
function insUsaSacoAdubo() { return $("#insCategoria").val() === "ADUBO" && $("#insGrandeza").val() === "MASSA"; }

function insAtualizarCampoSaco() {
    const adubo = insUsaSacoAdubo();
    $("#insSacoAdubo,#insSacoAduboHint").toggleClass("d-none", !adubo);
    $("#insPesoEmbalagem").toggleClass("d-none", adubo);
}

/** Seleciona o saco (em kg); um peso fora do padrão de cadastro antigo vira uma opção extra. */
function insSetSacoAdubo(kg) {
    const $s = $("#insSacoAdubo");
    $s.find("option.ins-saco-legado").remove();
    if (!kg) { $s.val(""); return; }
    const v = String(Math.round(kg * 1000) / 1000);
    if (!$s.find('option[value="' + v + '"]').length)
        $s.append('<option class="ins-saco-legado" value="' + v + '">Saco de ' + exibirDecimalNatural(kg, 3) + ' kg (cadastro antigo)</option>');
    $s.val(v);
}

/** Conversões de massa entre kg e a unidade do insumo (G/KG/T). */
function insKgParaUnidade(kg, un) { return un === "T" ? kg / 1000 : (un === "G" ? kg * 1000 : kg); }
function insUnidadeParaKg(v, un) { return un === "T" ? v * 1000 : (un === "G" ? v / 1000 : v); }

/** Preenche o select de unidades conforme a grandeza. */
function popularUnidades(grandeza, selecionada) {
    const $u = $("#insUnidade").empty();
    (UNIDADES_POR_GRANDEZA[grandeza] || []).forEach(function (par) {
        $u.append(`<option value="${par[0]}">${par[1]}</option>`);
    });
    if (selecionada) $u.val(selecionada);
}

/** Carrega os parceiros tipo insumo num select. cb(lista) é chamado após carregar. */
function carregarFornecedoresInsumo(selectId, incluirVazio, cb) {
    $.getJSON(AGRO_CTX + "/ControllerInsumo?acao=fornecedores", function (lista) {
        lista = lista || [];
        const $s = $("#" + selectId).empty();
        if (incluirVazio) $s.append('<option value="">— nenhum —</option>');
        lista.forEach(function (f) { $s.append(`<option value="${f.idPessoa}">${f.nome}</option>`); });
        if (cb) cb(lista);
    });
}

function abrirModalNovoInsumo() {
    $("#alertaInsumo").addClass("d-none").text("");
    $("#modalInsumoTitulo").html('<i class="fas fa-flask me-2"></i>Novo Insumo');
    $("#insId").val("");
    $("#insNome").val("");
    $("#insCategoria").val("DEFENSIVO");
    $("#insGrandeza").val("MASSA");
    popularUnidades("MASSA");
    $("#insMinimo").val("0");
    $("#insPesoEmbalagem").val("");
    insSetSacoAdubo(null);
    insAtualizarCampoSaco();
    carregarFornecedoresInsumo("insFornecedor", true, dcAtualizarHintFornecedor);
    $("#modalInsumo").modal("show");
}

/** Mostra um aviso no modal do insumo quando não há Parceiro do tipo Insumo. */
function dcAtualizarHintFornecedor(lista) {
    $("#insFornecedorHint").toggleClass("d-none", (lista && lista.length) > 0);
}

function abrirModalEditarInsumo(id) {
    $("#alertaInsumo").addClass("d-none").text("");
    $("#modalInsumoTitulo").html('<i class="fas fa-pen-to-square me-2"></i>Editar Insumo');
    $.getJSON(AGRO_CTX + "/ControllerInsumo?id=" + id, function (i) {
        $("#insId").val(i.idInsumo);
        $("#insNome").val(i.nome);
        $("#insCategoria").val(i.categoria);
        $("#insGrandeza").val(i.grandeza);
        popularUnidades(i.grandeza, i.unidade);
        $("#insMinimo").val(exibirDecimalNatural(i.estoqueMinimo, 3));
        $("#insPesoEmbalagem").val(i.pesoEmbalagem != null ? exibirDecimalNatural(i.pesoEmbalagem, 3) : "");
        insSetSacoAdubo(i.pesoEmbalagem != null ? insUnidadeParaKg(Number(i.pesoEmbalagem), i.unidade) : null);
        insAtualizarCampoSaco();
        // seta o fornecedor só APÓS carregar as opções (evita corrida)
        carregarFornecedoresInsumo("insFornecedor", true, function (lista) {
            dcAtualizarHintFornecedor(lista);
            $("#insFornecedor").val(i.idFornecedor ? String(i.idFornecedor) : "");
        });
        $("#modalInsumo").modal("show");
    });
}

function salvarInsumo() {
    const id = $("#insId").val();
    const data = {
        acao: id ? "update" : "create",
        idInsumo: id ? parseInt(id) : 0,
        nome: $("#insNome").val().trim(),
        categoria: $("#insCategoria").val(),
        grandeza: $("#insGrandeza").val(),
        unidade: $("#insUnidade").val(),
        estoqueMinimo: lerDecimalBR($("#insMinimo").val()),
        pesoEmbalagem: insUsaSacoAdubo()
            ? ($("#insSacoAdubo").val() ? insKgParaUnidade(Number($("#insSacoAdubo").val()), $("#insUnidade").val()) : null)
            : lerDecimalBR($("#insPesoEmbalagem").val()),
        idFornecedor: $("#insFornecedor").val() ? parseInt($("#insFornecedor").val()) : 0,
        situacao: true
    };
    if (!data.nome) { $("#alertaInsumo").removeClass("d-none").addClass("alert-danger").text("Informe o nome."); return; }
    $.ajax({
        url: AGRO_CTX + "/ControllerInsumo", method: "POST", contentType: "application/json; charset=utf-8",
        data: JSON.stringify(data),
        success: function (resp) {
            $("#modalInsumo").modal("hide");
            mostrarAlerta(resp.msg || "Insumo salvo!", "success", "#alerta");
            CarregarInsumos(); carregarResumoEstoque();
        },
        error: function (xhr) {
            let msg = "Erro ao salvar insumo!";
            try { msg = JSON.parse(xhr.responseText).msg || msg; } catch (e) {}
            $("#alertaInsumo").removeClass("d-none").addClass("alert-danger").text(msg);
        }
    });
}

function toggleInsumo(id, situacaoAtual) {
    $.ajax({
        url: AGRO_CTX + "/ControllerInsumo", method: "POST", contentType: "application/json; charset=utf-8",
        data: JSON.stringify({ acao: "delete", idInsumo: id, situacao: situacaoAtual }),
        success: function (resp) {
            mostrarAlerta(resp.msg || "Situação alterada.", "info", "#alerta");
            CarregarInsumos(); carregarResumoEstoque();
        },
        error: function () { mostrarAlerta("Erro ao alterar situação do insumo.", "danger", "#alerta"); }
    });
}

/** Preenche um select com os insumos ativos do cache. */
function preencherSelectInsumos(selectId) {
    const $s = $("#" + selectId).empty();
    insumosCache.filter(i => i.situacao).forEach(function (i) {
        $s.append(`<option value="${i.idInsumo}" data-disp="${i.quantidadeDisponivel}" data-un="${i.unidade}" data-embalagem="${i.pesoEmbalagem || ''}">${i.nome} (${i.unidade})</option>`);
    });
}

/** Mostra/esconde o atalho "nº de sacos" na Entrada conforme o insumo escolhido tenha peso de embalagem cadastrado. */
function atualizarBlocoSacosEntrada() {
    const opt = $("#entInsumo").find(":selected");
    const peso = parseFloat(opt.data("embalagem")) || 0;
    $("#entSacos,#entPrecoSaco").val("");
    if (peso > 0) {
        $("#entPesoSaco").text(fmtQtd(peso) + " " + (opt.data("un") || ""));
        $("#entBlocoSacos").removeClass("d-none");
    } else {
        $("#entBlocoSacos").addClass("d-none");
    }
}

/**
 * Nº de sacos × peso do saco preenche a Quantidade automaticamente, e preço por saco ÷ peso do
 * saco preenche o Preço unitário — ambos só um atalho; os dois campos continuam editáveis.
 */
function calcularQtdPorSacos() {
    const opt = $("#entInsumo").find(":selected");
    const peso = parseFloat(opt.data("embalagem")) || 0;
    const sacos = lerDecimalBR($("#entSacos").val());
    const precoSaco = lerDecimalBR($("#entPrecoSaco").val());
    if (peso > 0 && sacos > 0) $("#entQtd").val(exibirDecimalNatural(peso * sacos, 3));
    if (peso > 0 && precoSaco > 0) $("#entPreco").val(exibirDecimalBR(precoSaco / peso, 2));
}

function abrirEntrada() {
    // Pré-requisitos: precisa de insumo cadastrado e de fornecedor tipo Insumo.
    if (!insumosCache.filter(function (i) { return i.situacao; }).length) {
        preReqAlerta('#alerta', 'Para registrar uma Entrada é preciso ter ao menos um <strong>Insumo</strong> cadastrado.', null);
        return;
    }
    $.getJSON(AGRO_CTX + "/ControllerInsumo?acao=fornecedores", function (forns) {
        if (!(forns || []).length) {
            preReqAlerta('#alerta', 'Para registrar uma Entrada é preciso de ao menos um <strong>Parceiro do tipo Insumo</strong> (fornecedor).',
                CTX + '/view/admin/CadastroParceiro.jsp', 'Cadastrar Parceiro');
            return;
        }
        $("#alertaEntrada").addClass("d-none").text("");
        preencherSelectInsumos("entInsumo");
        var $s = $("#entFornecedor").empty();
        forns.forEach(function (f) { $s.append('<option value="' + f.idPessoa + '">' + f.nome + '</option>'); });
        $("#entQtd").val(""); $("#entPreco").val(""); $("#entObs").val("");
        $("#entData").val(new Date().toISOString().slice(0, 10));
        atualizarBlocoSacosEntrada();
        $("#modalEntrada").modal("show");
    });
}

function salvarEntrada() {
    const data = {
        acao: "entrada",
        idInsumo: parseInt($("#entInsumo").val()),
        idParceiro: $("#entFornecedor").val() ? parseInt($("#entFornecedor").val()) : 0,
        quantidade: lerDecimalBR($("#entQtd").val()),
        precoUnitario: lerDecimalBR($("#entPreco").val()),
        data: $("#entData").val(),
        observacao: $("#entObs").val()
    };
    if (!data.idInsumo || data.quantidade <= 0) { $("#alertaEntrada").removeClass("d-none").addClass("alert-danger").text("Insumo e quantidade (>0) obrigatórios."); return; }
    $.ajax({
        url: AGRO_CTX + "/ControllerEstoque", method: "POST", contentType: "application/json; charset=utf-8",
        data: JSON.stringify(data),
        success: function (resp) {
            $("#modalEntrada").modal("hide");
            mostrarAlerta(resp.msg || "Entrada registrada!", "success", "#alerta");
            CarregarInsumos(); carregarResumoEstoque();
        },
        error: function (xhr) {
            let msg = "Erro ao registrar entrada!";
            try { msg = JSON.parse(xhr.responseText).msg || msg; } catch (e) {}
            $("#alertaEntrada").removeClass("d-none").addClass("alert-danger").text(msg);
        }
    });
}

function atualizarDisponivelSaida() {
    const opt = $("#saiInsumo").find(":selected");
    $("#saiDisponivel").text(opt.length ? fmtQtd(opt.data("disp")) + " " + (opt.data("un") || "") : "-");
}

function abrirSaida() {
    if (!insumosCache.filter(function (i) { return i.situacao; }).length) {
        preReqAlerta('#alerta', 'Para registrar uma Saída é preciso ter ao menos um <strong>Insumo</strong> cadastrado (com estoque).', null);
        return;
    }
    $("#alertaSaida").addClass("d-none").text("");
    preencherSelectInsumos("saiInsumo");
    $("#saiQtd").val(""); $("#saiObs").val("");
    $("#saiData").val(new Date().toISOString().slice(0, 10));
    atualizarDisponivelSaida();
    $("#modalSaida").modal("show");
}

function salvarSaida() {
    const data = {
        acao: "saida",
        idInsumo: parseInt($("#saiInsumo").val()),
        quantidade: lerDecimalBR($("#saiQtd").val()),
        data: $("#saiData").val(),
        observacao: $("#saiObs").val()
    };
    if (!data.idInsumo || data.quantidade <= 0) { $("#alertaSaida").removeClass("d-none").addClass("alert-danger").text("Insumo e quantidade (>0) obrigatórios."); return; }
    $.ajax({
        url: AGRO_CTX + "/ControllerEstoque", method: "POST", contentType: "application/json; charset=utf-8",
        data: JSON.stringify(data),
        success: function (resp) {
            $("#modalSaida").modal("hide");
            mostrarAlerta(resp.msg || "Saída registrada!", "success", "#alerta");
            CarregarInsumos(); carregarResumoEstoque();
        },
        error: function (xhr) {
            let msg = "Erro ao registrar saída!";
            try { msg = JSON.parse(xhr.responseText).msg || msg; } catch (e) {}
            $("#alertaSaida").removeClass("d-none").addClass("alert-danger").text(msg);
        }
    });
}

function verMovimentos(id, nome) {
    $("#movInsumoNome").text(nome || "");
    const $b = $("#bodyMovimentos").html('<tr><td colspan="7" class="text-center text-muted py-3">Carregando...</td></tr>');
    $.getJSON(AGRO_CTX + "/ControllerEstoque?acao=movimentos&idinsumo=" + id, function (lista) {
        if (!lista.length) { $b.html('<tr><td colspan="7" class="text-center text-muted py-3">Sem movimentações.</td></tr>'); return; }
        $b.empty();
        lista.forEach(function (m) {
            const badge = m.tipo === "ENTRADA"
                ? '<span class="badge bg-info text-dark">Entrada</span>'
                : '<span class="badge bg-warning text-dark">Saída</span>';
            $b.append(`<tr>
                <td>${formatarData(m.dataMov)}</td>
                <td>${badge}</td>
                <td class="text-end">${fmtQtd(m.quantidade)}</td>
                <td class="text-end">${m.precoUnitario != null ? "R$ " + formatBRLnum(m.precoUnitario) : "—"}</td>
                <td>${m.parceiroNome || "—"}</td>
                <td class="text-end">${fmtQtd(m.saldoApos)}</td>
                <td>${m.observacao || ""}</td>
            </tr>`);
        });
    });
    $("#modalMovimentos").modal("show");
}

/* ========== estoque.jsp init ========== */
$(document).ready(function () {
    if (!$('#tabelaInsumos').length) return;

    CarregarInsumos();
    carregarResumoEstoque();
    autoRefreshTabela('insumos', function () { CarregarInsumos(); carregarResumoEstoque(); }, 15000);

    // Máscara decimal BR nos campos de quantidade/peso (nunca <input type="number"> puro — ver
    // aplicarMascaraDecimal). Precisa ser aplicada ANTES dos handlers de input abaixo, pra quem lê o
    // valor (calcularQtdPorSacos) sempre receber o texto já higienizado.
    aplicarMascaraDecimal($('#insMinimo'), 3);
    aplicarMascaraDecimal($('#insPesoEmbalagem'), 3);
    aplicarMascaraDecimal($('#entQtd'), 3);
    aplicarMascaraDecimal($('#entSacos'), 2);
    aplicarMascaraDecimal($('#entPrecoSaco'), 2);
    aplicarMascaraDecimal($('#entPreco'), 2);
    aplicarMascaraDecimal($('#saiQtd'), 3);

    $("#filtroCategoria").on("change", CarregarInsumos);
    $("#insGrandeza").on("change", function () { popularUnidades($(this).val()); insAtualizarCampoSaco(); });
    $("#insCategoria,#insUnidade").on("change", insAtualizarCampoSaco);
    $("#btnNovoInsumo").on("click", abrirModalNovoInsumo);
    $("#btnSalvarInsumo").on("click", salvarInsumo);
    $("#btnEntrada").on("click", abrirEntrada);
    $("#btnSalvarEntrada").on("click", salvarEntrada);
    $("#btnSaida").on("click", abrirSaida);
    $("#btnSalvarSaida").on("click", salvarSaida);
    $("#saiInsumo").on("change", atualizarDisponivelSaida);
    $("#entInsumo").on("change", atualizarBlocoSacosEntrada);
    $("#entSacos,#entPrecoSaco").on("input", calcularQtdPorSacos);
});

/* ========================================================================================== */
/* MÓDULO COLHEITA E ESTOQUE — colheitaestoque.jsp                                            */
/* ========================================================================================== */

function ceBadgeStatus(s) {
    var map = { COLHEIDO: 'bg-info text-dark', ESTOQUE: 'bg-warning text-dark', PROCESSADO: 'bg-primary', FINALIZADO: 'bg-secondary' };
    var rot = { COLHEIDO: 'Colhido', ESTOQUE: 'Em estoque', PROCESSADO: 'Processado', FINALIZADO: 'Finalizado' };
    return '<span class="badge ' + (map[s] || 'bg-secondary') + '">' + (rot[s] || s || '') + '</span>';
}

function ceCarregarIndicadores() {
    $.getJSON(AGRO_CTX + '/ControllerColheita?indicadores=1', function (ind) {
        $('#ceCardTotal').text(Number(ind.totalGeral || 0).toLocaleString('pt-BR', { minimumFractionDigits: 2 }));
        var porStatus = ind.porStatus || {};
        function saldo(st) { return porStatus[st] ? Number(porStatus[st].qtdTotal || 0).toLocaleString('pt-BR', { minimumFractionDigits: 2 }) : '0,00'; }
        $('#ceCardEstoque').text(saldo('ESTOQUE'));
        $('#ceCardProcessado').text(saldo('PROCESSADO'));
        $('#ceCardFinalizado').text(saldo('FINALIZADO'));
    });
}

function ceCarregarLista() {
    var status = $('#ceFiltroStatus').val();
    var url = AGRO_CTX + '/ControllerColheita' + (status ? '?status=' + status : '');
    $.getJSON(url, function (lista) {
        var $b = $('#ceTabelaBody').empty();
        ceLotes = {};
        if (!lista.length) { $b.append('<tr><td colspan="10" class="text-center text-muted py-3">Nenhum lote de colheita.</td></tr>'); return; }
        lista.forEach(function (l) {
            ceLotes[l.idLote] = l;
            var saldoQtd = (Number(l.quantidadeTotal || 0) - Number(l.quantidadeProcessada || 0));
            var acoes = '';
            if (l.status !== 'FINALIZADO') {
                if (saldoQtd > 0) acoes += '<button class="btn btn-sm btn-outline-primary ms-1" onclick="ceProcessar(' + Number(l.idLote) + ')"><i class="fas fa-box-open me-1"></i>Processar</button>';
                acoes += '<button class="btn btn-sm btn-outline-secondary ms-1" onclick="ceFinalizar(' + l.idLote + ')">Finalizar</button>';
            }
            $b.append('<tr><td>' + ceEsc(l.numeroDiario || '') + '</td><td>' + formatarData(l.dataColheita) + '</td>' +
                '<td>' + ceEsc(l.quadraNome || '—') + '</td><td>' + ceEsc(l.siglaArea || '—') + '</td><td>' + ceEsc(l.culturaNome || '—') + '</td>' +
                '<td class="text-end">' + Number(l.quantidadeTotal || 0).toFixed(2) + ' ' + (l.unidade || 'CX') + '</td>' +
                '<td class="text-end">' + Number(l.quantidadeProcessada || 0).toFixed(2) + '</td>' +
                '<td class="text-end">' + saldoQtd.toFixed(2) + '</td>' +
                '<td>' + ceBadgeStatus(l.status) + '</td>' +
                '<td class="text-end">' + acoes + '</td></tr>');
        });
    });
}

/* ---------- Modal "Processar" (onda 3) ----------
 * Processa uma QUANTIDADE do saldo (não precisa ser tudo: o resto continua em ESTOQUE) e registra a
 * mão de obra da etapa como um diário de Processamento no talhão do lote:
 *   embaladores (Produção, cargo Embalador) → regime PRODUCAO, caixas × preço da embalagem;
 *   demais (CLT/diarista/empreita)           → REGIME_SISTEMA, por horas (mesmo cálculo do Diário).
 * O preço de cada embalagem vem de Despesas e Custos (EMBALAGEM por alimento + área) e é
 * gravado/atualizado ao confirmar. Fluxo do Confirmar:
 *   1) Colheita valida (lote, saldo, safra, embalagens) sem gravar;
 *   2) Diário cria o diário de Processamento (contrato documentado em docs/modulos/diario-campo.md);
 *   3) Colheita grava baixa + preços + vínculo lote↔diário numa transação;
 *   se o passo 3 falhar, o diário do passo 2 é CANCELADO (o Diário não tem exclusão). */
var ceLotes = {};                 // idLote → lote da última listagem
var cpLote = null;                // lote aberto no modal
var cpFuncs = [];                 // funcionários ativos na data (ControllerDiarioCampo?funcionarios=1)
var cpEmbalagens = [];            // embalagens cadastradas p/ alimento + área (DespesasCustosServlet?embalagens=1)
var cpIdTipoProc = null;          // id do tipo de atividade "Processamento" (buscado por nome)
var CP_NOVA = '__NOVA__';

function ceEsc(v) {
    return String(v == null ? '' : v).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}
function ceMoeda(v) { return 'R$ ' + Number(v || 0).toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 }); }
function ceQtd(v) { return Number(v || 0).toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 }); }
function ceMsgXhr(xhr, padrao) { try { return JSON.parse(xhr.responseText).msg || padrao; } catch (e) { return padrao; } }
function cpHoje() { return new Date(Date.now() - new Date().getTimezoneOffset() * 60000).toISOString().slice(0, 10); }
/** Mesmo código que DespesasCustosDAO.normalizarTipoCaixa gera ("Papelão 7kg" → PAPELAO_7KG). */
function cpNormTipo(s) {
    return String(s || '').normalize('NFD').replace(/[̀-ͯ]/g, '').toUpperCase().trim()
        .replace(/[\s\-]+/g, '_').replace(/[^A-Z0-9_]/g, '');
}
function cpSaldo() { return cpLote ? Number(cpLote.quantidadeTotal || 0) - Number(cpLote.quantidadeProcessada || 0) : 0; }
function cpAlerta(msg, tipo) {
    $('#cpAlerta').removeClass('d-none alert-success alert-danger alert-warning alert-info').addClass('alert-' + (tipo || 'danger')).html(msg);
    var corpo = $('#modalProcessar .modal-body')[0]; if (corpo) corpo.scrollTop = 0;
}

function ceProcessar(idLote) {
    var l = ceLotes[idLote]; if (!l) return;
    cpLote = l;
    var saldo = cpSaldo();
    $('#cpTituloLote').text('— diário ' + (l.numeroDiario || '#' + l.idLote));
    $('#cpTalhao').text(l.quadraNome || '—');
    $('#cpAlimento').text(l.culturaNome || '—');
    $('#cpArea').text((l.areaNome || '—') + (l.siglaArea ? ' (' + l.siglaArea + ')' : ''));
    $('#cpColheita').text(formatarData(l.dataColheita));
    $('#cpSaldo').text(ceQtd(saldo) + ' ' + (l.unidade || 'CX'));
    var hoje = cpHoje();
    $('#cpData').val(l.dataColheita && l.dataColheita > hoje ? l.dataColheita : hoje).attr('min', l.dataColheita || '');
    $('#cpQuantidade').val(saldo.toFixed(2)).attr('max', saldo.toFixed(2));
    $('#cpFuncBody').empty();
    $('#cpResponsavel').html('<option value="">Selecione</option>');
    $('#cpAlerta').addClass('d-none');
    $('#cpHistorico').empty();
    $('#cpConfirmar').prop('disabled', false).html('<i class="fas fa-check me-1"></i>Confirmar processamento');
    cpAtualizarQuantidade(); cpRecalcular();

    var pTipos = cpIdTipoProc ? $.Deferred().resolve().promise()
        : $.getJSON(AGRO_CTX + '/ControllerDiarioCampo?tiposatividade=1').then(function (tipos) {
            (tipos || []).forEach(function (t) { if (String(t.nome || '').trim().toLowerCase() === 'processamento') cpIdTipoProc = t.idtipo || t.idTipo || t.id; });
        });
    var pEmb = cpCarregarEmbalagens();
    var pFunc = cpCarregarFuncionarios();
    $.getJSON(AGRO_CTX + '/ControllerColheita?processamentos=' + Number(l.idLote), function (hist) {
        if (!hist || !hist.length) return;
        var itens = hist.slice(0, 3).map(function (h) {
            return formatarData(h.data) + ': ' + ceQtd(h.quantidade) + (h.numeroDiario ? ' (diário ' + ceEsc(h.numeroDiario) + ')' : '');
        });
        $('#cpHistorico').html('<span class="text-muted"><i class="fas fa-clock-rotate-left me-1"></i>Processamentos anteriores:</span> ' + itens.join(' · '));
    });
    $.when(pTipos, pEmb, pFunc).always(function () {
        if (!cpIdTipoProc) cpAlerta('Tipo de atividade <strong>Processamento</strong> não encontrado no Diário de Campo — não é possível registrar a etapa.', 'warning');
        $('#modalProcessar').modal('show');
        if (cpOpcoesPessoa('EMB').length) cpNovaLinha('EMB');
    });
}

function cpCarregarEmbalagens() {
    if (!cpLote || !cpLote.idCultura || !cpLote.idArea) { cpEmbalagens = []; return $.Deferred().resolve().promise(); }
    return $.getJSON(AGRO_CTX + '/DespesasCustosServlet?embalagens=1&idAlimento=' + Number(cpLote.idCultura) + '&idArea=' + Number(cpLote.idArea))
        .then(function (r) { cpEmbalagens = (r && r.embalagens) || []; }, function () { cpEmbalagens = []; });
}

function cpCarregarFuncionarios() {
    return $.getJSON(AGRO_CTX + '/ControllerDiarioCampo?funcionarios=1&data=' + encodeURIComponent($('#cpData').val() || ''))
        .then(function (lista) { cpFuncs = lista || []; }, function () { cpFuncs = []; });
}

function cpEhEmbalador(f) { return f.tipo === 'PRODUCAO' && /embalador/i.test(f.cargo || ''); }
function cpOpcoesPessoa(modo) {
    return cpFuncs.filter(function (f) {
        return modo === 'EMB' ? cpEhEmbalador(f) : (f.tipo === 'CLT' || f.tipo === 'DIARISTA' || f.tipo === 'EMPREITA');
    });
}
function cpHtmlPessoas(modo, selecionado) {
    var lista = cpOpcoesPessoa(modo);
    if (!lista.length) {
        return '<option value="">' + (modo === 'EMB' ? 'Nenhum embalador ativo na data' : 'Nenhum funcionário ativo na data') + '</option>';
    }
    var h = '<option value="">Selecione</option>';
    lista.forEach(function (f) {
        h += '<option value="' + Number(f.idPessoa) + '" data-tipo="' + ceEsc(f.tipo) + '" data-ref="' + Number(f.custoRef || 0) +
            '" data-refextra="' + Number(f.custoRefExtra || 0) + '"' + (String(f.idPessoa) === String(selecionado) ? ' selected' : '') + '>' + ceEsc(f.nome) + '</option>';
    });
    return h;
}
function cpHtmlEmbalagens(selecionado) {
    var h = '<option value="">Selecione</option>';
    cpEmbalagens.forEach(function (e) {
        h += '<option value="' + ceEsc(e.tipoCaixa) + '" data-preco="' + Number(e.valordespesascustos || 0) + '"' +
            (e.tipoCaixa === selecionado ? ' selected' : '') + '>' + ceEsc(e.despesascusto || e.tipoCaixa) + ' — ' + ceMoeda(e.valordespesascustos) + '</option>';
    });
    return h + '<option value="' + CP_NOVA + '"' + (selecionado === CP_NOVA ? ' selected' : '') + '>+ Nova embalagem…</option>';
}

function cpNovaLinha(modo) {
    var emb = modo === 'EMB';
    var $tr = $('<tr data-modo="' + modo + '">' +
        '<td><select class="form-select form-select-sm cp-pessoa">' + cpHtmlPessoas(modo) + '</select></td>' +
        '<td class="small cp-tipo text-nowrap">—</td>' +
        '<td>' + (emb ? '<span class="badge bg-primary-subtle text-primary border border-primary-subtle">Produção</span>'
                      : '<span class="badge bg-secondary-subtle text-secondary border">Horas</span>') + '</td>' +
        '<td>' + (emb ? '<select class="form-select form-select-sm cp-emb">' + cpHtmlEmbalagens('') + '</select>' +
                        '<input type="text" class="form-control form-control-sm mt-1 d-none cp-nova-nome" maxlength="30" placeholder="Nome da embalagem (ex.: Papelão 7kg)">'
                      : '<span class="text-muted">—</span>') + '</td>' +
        '<td class="text-end">' + (emb ? '<input type="number" class="form-control form-control-sm text-end cp-preco" min="0" step="0.01" placeholder="0,00">' +
                                         '<div class="small text-warning d-none cp-preco-alt">novo preço</div>'
                                       : '<span class="small text-muted cp-ref"></span>') + '</td>' +
        '<td>' + (emb ? '<input type="number" class="form-control form-control-sm text-end cp-caixas" min="0" step="1" placeholder="0">' : '<span class="text-muted d-block text-center">—</span>') + '</td>' +
        '<td>' + (emb ? '<span class="text-muted d-block text-center">—</span>' : '<input type="number" class="form-control form-control-sm text-end cp-horas" min="0" step="0.5" placeholder="0">') + '</td>' +
        '<td class="text-end fw-semibold cp-valor">R$ 0,00</td>' +
        '<td class="text-end"><button type="button" class="btn btn-sm btn-link text-danger p-0 cp-remover" title="Remover"><i class="fas fa-xmark"></i></button></td>' +
        '</tr>');
    $('#cpFuncBody').append($tr);
    cpRecalcular();
    return $tr;
}

/** Preço da embalagem escolhida na linha: o cadastrado em Despesas ou, se já editado em outra linha, o mesmo. */
function cpCodigoEmbalagem($tr) {
    var v = $tr.find('.cp-emb').val();
    return v === CP_NOVA ? cpNormTipo($tr.find('.cp-nova-nome').val()) : (v || '');
}
function cpAoEscolherEmbalagem($tr) {
    var v = $tr.find('.cp-emb').val();
    $tr.find('.cp-nova-nome').toggleClass('d-none', v !== CP_NOVA);
    if (v === CP_NOVA) { $tr.find('.cp-preco').val(''); $tr.find('.cp-nova-nome').trigger('focus'); }
    else {
        var codigo = cpCodigoEmbalagem($tr), outro = null;
        $('#cpFuncBody tr[data-modo="EMB"]').not($tr).each(function () { if (codigo && cpCodigoEmbalagem($(this)) === codigo && $(this).find('.cp-preco').val()) outro = $(this).find('.cp-preco').val(); });
        $tr.find('.cp-preco').val(outro || (v ? Number($tr.find('.cp-emb :selected').data('preco') || 0).toFixed(2) : ''));
    }
    cpRecalcular();
}
/** Mesmo tipo de embalagem = mesmo preço: editar numa linha replica nas outras. */
function cpReplicarPreco($tr) {
    var codigo = cpCodigoEmbalagem($tr), preco = $tr.find('.cp-preco').val();
    if (!codigo) return;
    $('#cpFuncBody tr[data-modo="EMB"]').not($tr).each(function () { if (cpCodigoEmbalagem($(this)) === codigo) $(this).find('.cp-preco').val(preco); });
}

function cpAtualizarQuantidade() {
    var saldo = cpSaldo(), q = Number($('#cpQuantidade').val()) || 0;
    var pct = saldo > 0 ? Math.min(100, Math.max(0, q / saldo * 100)) : 0;
    $('#cpBarra').css('width', pct + '%').toggleClass('bg-danger', q > saldo).toggleClass('bg-success', q <= saldo);
    $('#cpRestante').html(q > saldo ? '<span class="text-danger">Maior que o saldo disponível.</span>'
        : 'Fica em estoque: <strong>' + ceQtd(saldo - q) + '</strong> ' + ceEsc(cpLote ? cpLote.unidade || 'CX' : 'CX'));
}

function cpRecalcular() {
    var totCx = 0, totEmb = 0, totHoras = 0;
    $('#cpFuncBody tr').each(function () {
        var $tr = $(this), opt = $tr.find('.cp-pessoa :selected'), valor = 0;
        var tipo = opt.val() ? opt.data('tipo') : '';
        $tr.find('.cp-tipo').text(tipo ? (tipo === 'PRODUCAO' ? 'Produção' : tipo.charAt(0) + tipo.slice(1).toLowerCase()) : '—');
        if ($tr.data('modo') === 'EMB') {
            var cx = Number($tr.find('.cp-caixas').val()) || 0, preco = Number($tr.find('.cp-preco').val()) || 0;
            valor = cx * preco; totCx += cx; totEmb += valor;
            var $sel = $tr.find('.cp-emb :selected'), cadastrado = $sel.val() && $sel.val() !== CP_NOVA ? Number($sel.data('preco') || 0) : null;
            $tr.find('.cp-preco-alt').toggleClass('d-none', !(cadastrado !== null && preco > 0 && Math.abs(preco - cadastrado) > 0.004));
        } else {
            var ref = Number(opt.data('ref') || 0), refExtra = Number(opt.data('refextra') || 0), horas = Number($tr.find('.cp-horas').val()) || 0;
            valor = opt.val() && typeof dcValorFuncionario === 'function' ? dcValorFuncionario(tipo, ref, refExtra, horas) : 0;
            $tr.find('.cp-ref').text(opt.val() ? (tipo === 'CLT' ? ceMoeda(ref) + '/h' : (tipo === 'DIARISTA' ? ceMoeda(ref) + '/dia' : ceMoeda(ref) + ' fixo')) : '');
            totHoras += valor;
        }
        $tr.find('.cp-valor').text(ceMoeda(valor));
    });
    $('#cpTotCaixas').text(totCx.toLocaleString('pt-BR'));
    $('#cpTotEmb').text(ceMoeda(totEmb)); $('#cpTotHoras').text(ceMoeda(totHoras)); $('#cpTotGeral').text(ceMoeda(totEmb + totHoras));
    $('#cpVazio').toggle(!$('#cpFuncBody tr').length);
    cpAtualizarResponsavel();
}

/** Responsável do diário: escolhido entre os funcionários lançados (igual ao Diário). */
function cpAtualizarResponsavel() {
    var atual = $('#cpResponsavel').val(), vistos = {}, primeiro = null, h = '<option value="">Selecione</option>';
    $('#cpFuncBody .cp-pessoa').each(function () {
        var id = $(this).val(); if (!id || vistos[id]) return;
        vistos[id] = true; primeiro = primeiro || id;
        h += '<option value="' + Number(id) + '">' + ceEsc($(this).find(':selected').text()) + '</option>';
    });
    $('#cpResponsavel').html(h).val(atual && vistos[atual] ? atual : (primeiro || ''));
}

function cpAoMudarData() {
    cpCarregarFuncionarios().always(function () {
        $('#cpFuncBody tr').each(function () {
            var $sel = $(this).find('.cp-pessoa'), atual = $sel.val();
            $sel.html(cpHtmlPessoas($(this).data('modo'), atual));
        });
        cpRecalcular();
    });
}

/** Lê o modal; devolve {erro} ou os dados para os 3 passos do Confirmar. */
function cpColetar() {
    var data = $('#cpData').val(), qtd = Number($('#cpQuantidade').val()) || 0, saldo = cpSaldo();
    if (!data) return { erro: 'Informe a data do processamento.' };
    if (qtd <= 0) return { erro: 'Informe a quantidade processada (maior que zero).' };
    if (qtd > saldo + 0.0001) return { erro: 'A quantidade processada (' + ceQtd(qtd) + ') é maior que o saldo disponível (' + ceQtd(saldo) + ').' };
    if (!$('#cpFuncBody tr').length) return { erro: 'Adicione ao menos um funcionário (embalador ou por horas).' };
    var embalagens = [], funcionarios = [], erro = null, precos = {};
    $('#cpFuncBody tr').each(function (i) {
        if (erro) return;
        var $tr = $(this), linha = 'Linha ' + (i + 1) + ': ', idPessoa = parseInt($tr.find('.cp-pessoa').val()) || 0;
        if (!idPessoa) { erro = linha + 'escolha o funcionário.'; return; }
        if ($tr.data('modo') === 'EMB') {
            var v = $tr.find('.cp-emb').val(), nova = v === CP_NOVA, nome = $.trim($tr.find('.cp-nova-nome').val());
            var codigo = cpCodigoEmbalagem($tr), preco = Number($tr.find('.cp-preco').val()) || 0, cx = Number($tr.find('.cp-caixas').val()) || 0;
            if (!v || (nova && !codigo)) { erro = linha + (nova ? 'informe o nome da nova embalagem.' : 'escolha o tipo de embalagem.'); return; }
            if (preco <= 0) { erro = linha + 'informe o preço da embalagem (maior que zero).'; return; }
            if (cx <= 0) { erro = linha + 'informe o nº de caixas (maior que zero).'; return; }
            if (precos[codigo] != null && Math.abs(precos[codigo] - preco) > 0.004) { erro = linha + 'a mesma embalagem está com preços diferentes.'; return; }
            precos[codigo] = preco;
            embalagens.push({ idPessoa: idPessoa, tipoCaixa: codigo, descricao: nova ? nome : null, caixas: cx, preco: preco });
            funcionarios.push({ idPessoa: idPessoa, regimeTrabalho: 'PRODUCAO', quantidadeCaixas: cx, precoPorCaixa: preco,
                funcaoExercida: 'Embalador' });
        } else {
            var horas = Number($tr.find('.cp-horas').val()) || 0;
            if (horas <= 0) { erro = linha + 'informe as horas trabalhadas.'; return; }
            funcionarios.push({ idPessoa: idPessoa, regimeTrabalho: 'REGIME_SISTEMA', horasTrabalhadas: horas });
        }
    });
    if (erro) return { erro: erro };
    var idResp = parseInt($('#cpResponsavel').val()) || 0;
    if (!idResp) return { erro: 'Escolha o responsável pelo diário de Processamento.' };
    if (!cpIdTipoProc) return { erro: 'Tipo de atividade Processamento não encontrado no Diário de Campo.' };
    return { data: data, quantidade: Number(qtd.toFixed(2)), embalagens: embalagens, funcionarios: funcionarios, idResponsavel: idResp };
}

function cpPost(url, corpo) {
    return $.ajax({ url: AGRO_CTX + url, method: 'POST', contentType: 'application/json; charset=utf-8', data: JSON.stringify(corpo) });
}

function cpConfirmar() {
    var d = cpColetar();
    if (d.erro) { cpAlerta(d.erro, 'danger'); return; }
    var l = cpLote, $btn = $('#cpConfirmar');
    $btn.prop('disabled', true).html('<span class="spinner-border spinner-border-sm me-1"></span>Registrando…');
    $('#cpAlerta').addClass('d-none');
    var base = { idlote: l.idLote, data: d.data, quantidade: d.quantidade, embalagens: d.embalagens };
    var falhar = function (msg) {
        cpAlerta(msg, 'danger');
        $btn.prop('disabled', false).html('<i class="fas fa-check me-1"></i>Confirmar processamento');
    };
    // 1) valida na Colheita (nada é gravado)
    cpPost('/ControllerColheita', $.extend({ acao: 'validarprocessamento' }, base)).then(function () {
        // 2) diário de Processamento (contrato do Diário de Campo)
        return cpPost('/ControllerDiarioCampo', {
            acao: 'create', idArea: l.idArea, idQuadra: l.idQuadra, idCultura: l.idCultura, idResponsavel: d.idResponsavel,
            idTipoAtividade: cpIdTipoProc, status: 'EM_ANDAMENTO', data: d.data,
            descricao: 'Processamento do lote da colheita ' + (l.numeroDiario || '#' + l.idLote) + ' — ' + ceQtd(d.quantidade) + ' ' + (l.unidade || 'CX'),
            maquinas: [], insumos: [], despesas: [], funcionarios: d.funcionarios
        }).then(null, function (xhr) {
            return $.Deferred().reject({ msg: 'O Diário de Campo recusou o lançamento: ' + ceMsgXhr(xhr, 'erro ao criar o diário de Processamento.') }).promise();
        });
    }, function (xhr) {
        return $.Deferred().reject({ msg: ceMsgXhr(xhr, 'Erro ao validar o processamento.') }).promise();
    }).then(function (diario) {
        // 3) baixa + preços + vínculo (transação da Colheita); se falhar, cancela o diário criado
        return cpPost('/ControllerColheita', $.extend({ acao: 'processarcomdiario', iddiario: diario.iddiario }, base))
            .then(function () { return diario; }, function (xhr) {
                var msg = ceMsgXhr(xhr, 'Erro ao registrar o processamento.');
                return cpPost('/ControllerDiarioCampo', { acao: 'status', iddiario: diario.iddiario, status: 'CANCELADA' })
                    .then(function () { return $.Deferred().reject({ msg: msg + ' O diário de Processamento ' + ceEsc(diario.numeroDiario || '') + ' foi cancelado.' }).promise(); },
                          function () { return $.Deferred().reject({ msg: msg + ' Atenção: o diário de Processamento ' + ceEsc(diario.numeroDiario || '') + ' foi criado e não pôde ser cancelado — cancele-o no Diário de Campo.' }).promise(); });
            });
    }).then(function (diario) {
        $('#modalProcessar').modal('hide');
        mostrarAlerta('Processamento registrado. Diário de Processamento <strong>' + ceEsc(diario.numeroDiario || '') + '</strong> criado no talhão ' + ceEsc(l.quadraNome || '') + '.', 'success', '#alertaColheita', 6000);
        ceCarregarLista(); ceCarregarIndicadores();
    }, function (e) {
        falhar(e && e.msg ? e.msg : 'Erro ao registrar o processamento.');
    });
}

function cpInitModal() {
    var $m = $('#modalProcessar');
    $('#cpAddEmbalador').off('click').on('click', function () { cpNovaLinha('EMB'); });
    $('#cpAddOutro').off('click').on('click', function () { cpNovaLinha('HORAS'); });
    $('#cpUsarSaldo').off('click').on('click', function () { $('#cpQuantidade').val(cpSaldo().toFixed(2)); cpAtualizarQuantidade(); });
    $('#cpQuantidade').off('input').on('input', cpAtualizarQuantidade);
    $('#cpData').off('change').on('change', cpAoMudarData);
    $('#cpConfirmar').off('click').on('click', cpConfirmar);
    $m.off('.cp')
      .on('change.cp', '.cp-pessoa', cpRecalcular)
      .on('change.cp', '.cp-emb', function () { cpAoEscolherEmbalagem($(this).closest('tr')); })
      .on('input.cp', '.cp-nova-nome', function () { cpRecalcular(); })
      .on('input.cp', '.cp-preco', function () { cpReplicarPreco($(this).closest('tr')); cpRecalcular(); })
      .on('input.cp', '.cp-caixas, .cp-horas', cpRecalcular)
      .on('click.cp', '.cp-remover', function () { $(this).closest('tr').remove(); cpRecalcular(); });
}

function ceFinalizar(idLote) {
    if (!confirm('Finalizar este lote? Não será possível processar mais nada nele depois.')) return;
    $.ajax({
        url: AGRO_CTX + '/ControllerColheita', method: 'POST', contentType: 'application/json; charset=utf-8',
        data: JSON.stringify({ acao: 'finalizar', idlote: idLote }),
        success: function (res) {
            mostrarAlerta(res.msg, res.ok ? 'success' : 'danger', '#alertaColheita');
            ceCarregarLista(); ceCarregarIndicadores();
        },
        error: function (xhr) { var m = 'Erro ao finalizar lote.'; try { m = JSON.parse(xhr.responseText).msg || m; } catch (e) {} mostrarAlerta(m, 'danger', '#alertaColheita'); }
    });
}

/** Botão "Cadastrar Colheita": escolhe a área e redireciona pro Diário de Campo dela (modal abre sozinho). */
function ceCarregarAreas() {
    $.getJSON(AGRO_CTX + '/ControllerAreaProducao', function (areas) {
        var opt = '<option value="">Selecione</option>';
        areas.filter(function (a) { return a.situacao; }).forEach(function (a) {
            opt += '<option value="' + a.idAreaProducao + '">' + a.PropriedadeAreaProducao + ' (' + a.SiglasAreaProducao + ')</option>';
        });
        $('#ceSelecionarArea').html(opt);
    });
}

function initColheitaEstoque() {
    ceCarregarIndicadores();
    ceCarregarLista();
    cpInitModal();
    $('#ceFiltroStatus').off('change').on('change', ceCarregarLista);
    $('#btnCadastrarColheita').off('click').on('click', function () {
        ceCarregarAreas();
        $('#modalEscolherArea').modal('show');
    });
    $('#btnIrParaColheita').off('click').on('click', function () {
        var idArea = $('#ceSelecionarArea').val();
        if (!idArea) { mostrarAlerta('Selecione uma área.', 'danger', '#alertaColheita'); return; }
        window.location.href = AGRO_CTX + '/view/admin/DetalheAreaProducao.jsp?id=' + idArea + '&abrirColheita=1';
    });
}

/* ========== colheitaestoque.jsp init ========== */
$(document).ready(function () {
    if (!$('#ceTabelaBody').length) return;
    initColheitaEstoque();
});