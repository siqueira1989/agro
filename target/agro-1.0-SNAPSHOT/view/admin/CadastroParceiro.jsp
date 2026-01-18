<!DOCTYPE html>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html lang="pt-br">
    <head>
        <meta charset="UTF-8">
        <title>Agro- Cadastro de Parceiros</title>
        <!-- Bootstrap 4 CSS -->
        <link rel="stylesheet" href="https://stackpath.bootstrapcdn.com/bootstrap/4.5.2/css/bootstrap.min.css">
        <!-- Font Awesome -->
        <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0-beta3/css/all.min.css">
        <!-- CSS customizado -->
        <link href="../../CSS/estilo.css" rel="stylesheet">


        <style>
            .disabled-field {
                background-color: #e9ecef !important; /* cinza claro */
                transition: background-color 0.3s ease; /* animação suave */
            }
        </style>

    </head>
    <body>
        <header class="header">
            <%@ include file="../../pagina/menu.jsp" %>
        </header>

        <div class="container-fluid content mt-2">

            <!-- Título -->
            <div class="card shadow-sm mb-4">
                <div class="card-body bg-success text-white rounded">
                    <h4 id="tituloFormulario" class="titulo-pagina"><i class="fas fa-user-plus"></i> Cadastrar Parceiro</h4>
                    
                </div>
            </div>
            <div id="alerta" class="alert d-none" role="alert"></div>
            <!-- FORMULÁRIO COMPLETO -->
            <form id="formCadastro">

                <!-- DADOS EMPRESA -->
                <div class="card shadow-sm mb-4">
                    <div class="card-header bg-white">
                        <h5 class="text-success font-weight-bold">
                            <i class="fas fa-building"></i> Dados da Empresa
                        </h5>
                    </div>

                    <div class="card-body">
                        <div class="form-row">
                            <div class="form-group col-md-12">
                                <label>CNPJ *</label>
                                <div class="input-group">
                                    <input type="text" class="form-control required" id="cnpj" name="cnpj" placeholder="00.000.000/0001-00">
                                    <div class="input-group-append">
                                        <span class="input-group-text" id="icone-cnpj">
                                            <input type="hidden" id="idPessoa" name="idPessoa">
                                         
                                            <i class="fas fa-spinner fa-spin d-none text-secondary" id="spinner-cnpj"></i>
                                            <i class="fas fa-check text-success d-none" id="icone-ok"></i>
                                            <i class="fas fa-times text-danger d-none" id="icone-erro"></i>
                                        </span>
                                    </div>
                                </div>
                                <div class="invalid-feedback">Informe o CNPJ.</div>
                            </div>


                            <div class="form-group col-md-12">
                                <label>Razão Social *</label>
                                <input type="text" class="form-control required" id="razao" name="razaosocial">
                                <div class="invalid-feedback">Informe o razao social.</div>
                            </div>

                        </div>
                        <div class="form-row">


                            <div class="form-group  col-md-7">
                                <label>Site</label>
                                <input type="text" class="form-control" id="site" name="site" placeholder="https://www.exemplo.com.br">
                            </div>
                            <div class="form-group col-md-5">
                                <label>Inscrição Estadual</label>
                                <input type="text" class="form-control required" id="inscricaoestadual" name="inscricaoestadual">
                                <div class="invalid-feedback">Informe o Inscricao Estadual.</div>
                            </div>
                        </div>

                    </div>
                </div>

                <!-- DADOS COMERCIAIS -->
                <div class="card shadow-sm mb-4">
                    <div class="card-header bg-white">
                        <h5 class="text-success font-weight-bold">
                            <i class="fa-regular fa-address-book"></i> Dados Comerciais
                        </h5>
                    </div>

                    <div class="card-body">

                        <div class="form-group">
                            <label>Nome Completo *</label>
                            <input type="text" class="form-control required" id="nome" name="nome" required>
                            <div class="invalid-feedback">Informe o nome.</div>
                        </div>
                        <div class="form-row">
                            <div class="form-group col-md-6">
                                <label>Usuário *</label>
                                <input type="text" class="form-control required" id="usuario" name="usuario">

                            </div>

                            <div class="form-group col-md-6">
                                <label>Senha *</label>
                                <input type="password" class="form-control required" id="senha" name="senha">
                            </div>

                        </div>
                        <div class="form-row">
                            <div class="form-group col-md-6">
                                <label>E-mail *</label>
                                <input type="email" class="form-control required" id="email" name="email">
                                <div class="invalid-feedback">Informe o email.</div>
                            </div>
                            <div class="form-group col-md-6">
                                <label>Telefone</label>
                                <input type="telefone" class="form-control required" id="telefone" name="telefone">
                                <div class="invalid-feedback">Informe  telefone.</div>
                            </div>

                        </div>

                        <div class="form-row">

                            <div class="form-group col-md-6">
                                <label>Nível *</label>
                                <select class="form-control required" id="nivel" name="nivel">
                                    <option value="">Selecione</option>
                                    <option value="parceiro">Parceiro</option>
                                    <option value="insumo">Insumo</option>
                                    <option value="fornecedor">Fornecedor</option>
                                </select>
                                <div class="invalid-feedback">Informe o Nivel.</div>
                            </div>
                            <div class="form-group col-md-6">
                                <label>Situação</label>
                                <select class="form-control" id="situacao" name="situacao">
                                    <option value="true" selected>Ativo</option>
                                    <option value="false">Inativo</option>
                                </select>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- ENDEREÇO -->
                <div class="card shadow-sm mb-5">
                    <div class="card-header bg-white">
                        <h5 class="text-success font-weight-bold">
                            <i class="fas fa-map-marker-alt"></i> Endereço
                        </h5>
                    </div>

                    <div class="card-body">

                        <div class="form-row">
                            <div class="form-group col-md-4">
                                <label>CEP *</label>
                                <input type="text" class="form-control required" id="cep" name="cep">
                                <div class="invalid-feedback">Informe o CEP.</div>
                            </div>

                            <div class="form-group col-md-12">
                                <label>Endereço</label>
                                <input type="text" class="form-control" id="logradouro" readonly>
                            </div>
                        </div>

                        <div class="form-row">

                            <div class="form-group col-md-2">
                                <label>Número *</label>
                                <input type="text" class="form-control required" id="numero" name="numero">
                                <div class="invalid-feedback">Informe o numero.</div>
                            </div>

                            <div class="form-group col-md-4">
                                <label>Bairro</label>
                                <input type="text" class="form-control" id="bairro" readonly>
                            </div>

                            <div class="form-group col-md-4">
                                <label>Cidade</label>
                                <input type="text" class="form-control" id="cidade" readonly>
                            </div>

                            <div class="form-group col-md-2">
                                <label>UF</label>
                                <input type="text" class="form-control" id="uf" readonly>
                            </div>
                        </div>

                        <div class="form-group">
                            <label>Complemento</label>
                            <input type="text" class="form-control" id="complemento">

                        </div>
                    </div>
                </div>

                <!-- BOTÕES -->
                <div class="text-right mb-5">
                    <a href="http://localhost:8080/agro/view/admin/parceiro.jsp" class="btn btn-secondary">
                        <i class="fas fa-arrow-left"></i> Voltar
                    </a>

                  <button type="submit" class="btn btn-success" id="btnSalvar">
  <i class="fas fa-save"></i> Salvar Cadastro
</button>

                </div>

            </form>

        </div>

        <!-- Scripts -->

        <!-- jQuery -->
        <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>

        <!-- jQuery Mask -->
        <script src="https://cdnjs.cloudflare.com/ajax/libs/jquery.mask/1.14.16/jquery.mask.min.js"></script>

        <!-- Bootstrap -->
        <script src="https://stackpath.bootstrapcdn.com/bootstrap/4.5.2/js/bootstrap.bundle.min.js"></script>
<%@ include file="../../pagina/footer.jsp" %>

<script>
$(document).ready(function () {

  // ========= MÁSCARAS =========
  $('#cep').mask('00000-000');
  $('#cnpj').mask('00.000.000/0001-00');

  $('#telefone')
    .mask('(00) 00000-0000')
    .on('blur', function () {
      if ($(this).val().length === 14) {
        $(this).mask('(00) 0000-0000');
      } else {
        $(this).mask('(00) 00000-0000');
      }
    });

  // ========= LÊ ID DA URL (update) =========
  const urlParams = new URLSearchParams(window.location.search);
  const id = urlParams.get("id");

  if (id) {
    $("#tituloFormulario").html('<i class="fas fa-user-edit"></i> Atualizar Parceiro');
    $("#btnSalvar").html('<i class="fas fa-save"></i> Atualizar');

    carregarDadosParceiro(id);

    // dica: em update, normalmente não deixa trocar CNPJ
    $("#cnpj").prop("readonly", true).addClass("disabled-field");
  } else {
    $("#tituloFormulario").html('<i class="fas fa-user-plus"></i> Cadastrar Parceiro');
    $("#btnSalvar").html('<i class="fas fa-save"></i> Salvar Cadastro');
  }

  // ========= DESABILITAR USUÁRIO / SENHA SE NÃO FOR PARCEIRO =========
  $("#nivel").on("change", function () {
    let valor = $(this).val();
    if (valor !== "parceiro") {
      $("#usuario, #senha")
        .prop("disabled", true)
        .addClass("disabled-field")
        .fadeTo(200, 0.6)
        .removeClass("is-invalid is-valid")
        .val("");
    } else {
      $("#usuario, #senha")
        .prop("disabled", false)
        .removeClass("disabled-field")
        .fadeTo(200, 1);
    }
  });

  // ========= VIA CEP =========
  $("#cep").blur(function () {
    let cep = $(this).val().replace(/\D/g, '');
    if (cep.length !== 8) return;

    $.getJSON("https://viacep.com.br/ws/" + cep + "/json/", function (data) {
      if (data.erro) {
        alert("❌ CEP não encontrado.");
        return;
      }
      $("#logradouro").val(data.logradouro);
      $("#bairro").val(data.bairro);
      $("#cidade").val(data.localidade);
      $("#uf").val(data.uf);
    });
  });

  // ========= VALIDAR CNPJ (só no CREATE) =========
  function validarCNPJ(cnpj) {
    cnpj = cnpj.replace(/[^\d]+/g, '');
    if (cnpj.length !== 14) return false;
    if (/^(\d)\1{13}$/.test(cnpj)) return false;

    let tamanho = cnpj.length - 2;
    let numeros = cnpj.substring(0, tamanho);
    let digitos = cnpj.substring(tamanho);
    let soma = 0;
    let pos = tamanho - 7;

    for (let i = tamanho; i >= 1; i--) {
      soma += numeros.charAt(tamanho - i) * pos--;
      if (pos < 2) pos = 9;
    }

    let resultado = soma % 11 < 2 ? 0 : 11 - (soma % 11);
    if (resultado != digitos.charAt(0)) return false;

    tamanho += 1;
    numeros = cnpj.substring(0, tamanho);
    soma = 0;
    pos = tamanho - 7;

    for (let i = tamanho; i >= 1; i--) {
      soma += numeros.charAt(tamanho - i) * pos--;
      if (pos < 2) pos = 9;
    }

    resultado = soma % 11 < 2 ? 0 : 11 - (soma % 11);
    return resultado == digitos.charAt(1);
  }

  $("#cnpj").on("input", function () {
    $("#icone-ok, #icone-erro").addClass("d-none");
    $("#spinner-cnpj").addClass("d-none");
    $("#cnpj").removeClass("is-valid is-invalid");
    $("#btnSalvar").prop("disabled", false);
  });

  $("#cnpj").on("blur", function () {

    // se for UPDATE, não valida existência de CNPJ (já é dele)
    if ($("#idPessoa").val()) return;

    const cnpj = $(this).val().replace(/\D/g, '');
    $("#icone-ok, #icone-erro").addClass("d-none");
    $("#spinner-cnpj").removeClass("d-none");

    if (cnpj.length !== 14 || !validarCNPJ(cnpj)) {
      $("#spinner-cnpj").addClass("d-none");
      $("#icone-erro").removeClass("d-none");
      $("#cnpj").addClass("is-invalid");
      $("#btnSalvar").prop("disabled", true);
      return;
    }

    $.ajax({
      url: "/agro/ControllerParceiro?cnpj=" + cnpj,
      method: "GET",
      dataType: "json",
      success: function (res) {
        $("#spinner-cnpj").addClass("d-none");
        const existe = res.existe === true || res.existe === "true";

        if (existe) {
          $("#icone-erro").removeClass("d-none");
          $("#cnpj").addClass("is-invalid");
          $("#btnSalvar").prop("disabled", true);
        } else {
          $("#icone-ok").removeClass("d-none");
          $("#cnpj").removeClass("is-invalid").addClass("is-valid");
          $("#btnSalvar").prop("disabled", false);
        }
      },
      error: function () {
        $("#spinner-cnpj").addClass("d-none");
        $("#icone-erro").removeClass("d-none");
        $("#btnSalvar").prop("disabled", true);
        alert("❌ Erro ao verificar CNPJ.");
      }
    });
  });

  // ========= SUBMIT (create/update) =========
  $("#formCadastro").on("submit", function (e) {
    e.preventDefault();
    salvarParceiroCreateOuUpdate();
  });

});

</script>
        
</body>
</html>
