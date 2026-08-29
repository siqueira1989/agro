<!DOCTYPE html>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html lang="pt-br">
    <head>
        <meta charset="UTF-8">
        <title>Agro- Cadastro de Parceiros</title>
        <%@ include file="../../pagina/importacao.html"%>

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
                        <h5 class="text-success fw-bold">
                            <i class="fas fa-building"></i> Dados da Empresa
                        </h5>
                    </div>

                    <div class="card-body">
                        <div class="row g-2">
                            <div class="col-md-12">
                                <label>CNPJ *</label>
                                <div class="input-group">
                                    <input type="text" class="form-control required" id="cnpj" name="cnpj" placeholder="00.000.000/0001-00">
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


                            <div class="col-md-12">
                                <label>Razão Social *</label>
                                <input type="text" class="form-control required" id="razao" name="razaosocial">
                                <div class="invalid-feedback">Informe o razao social.</div>
                            </div>

                        </div>
                        <div class="row g-2">


                            <div class="form-group  col-md-7">
                                <label>Site</label>
                                <input type="text" class="form-control" id="site" name="site" placeholder="https://www.exemplo.com.br">
                            </div>
                            <div class="col-md-5">
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
                        <h5 class="text-success fw-bold">
                            <i class="fa-regular fa-address-book"></i> Dados Comerciais
                        </h5>
                    </div>

                    <div class="card-body">

                        <div class="mb-3">
                            <label>Nome Completo *</label>
                            <input type="text" class="form-control required" id="nome" name="nome" required>
                            <div class="invalid-feedback">Informe o nome.</div>
                        </div>
                        <div class="row g-2">
                            <div class="col-md-6">
                                <label>Usuário *</label>
                                <input type="text" class="form-control required" id="usuario" name="usuario">

                            </div>

                            <div class="col-md-6">
                                <label>Senha *</label>
                                <input type="password" class="form-control required" id="senha" name="senha">
                            </div>

                        </div>
                        <div class="row g-2">
                            <div class="col-md-6">
                                <label>E-mail *</label>
                                <input type="email" class="form-control required" id="email" name="email">
                                <div class="invalid-feedback">Informe o email.</div>
                            </div>
                            <div class="col-md-6">
                                <label>Telefone</label>
                                <input type="telefone" class="form-control required" id="telefone" name="telefone">
                                <div class="invalid-feedback">Informe  telefone.</div>
                            </div>

                        </div>

                        <div class="row g-2">

                            <div class="col-md-6">
                                <label>Tipo *</label>
                                <select class="form-control required" id="nivel" name="nivel">
                                    <option value="">Selecione</option>
                                    <option value="PARCEIRO">Parceiro — compra a produção</option>
                                    <option value="FORNECEDOR">Fornecedor — vende indiretamente (caixas, gasolina)</option>
                                    <option value="INSUMO">Insumo — defensivos e adubos</option>
                                </select>
                                <div class="invalid-feedback">Informe o Tipo.</div>
                            </div>
                            <div class="col-md-6">
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
                        <h5 class="text-success fw-bold">
                            <i class="fas fa-map-marker-alt"></i> Endereço
                        </h5>
                    </div>

                    <div class="card-body">

                        <div class="row g-2">
                            <div class="col-md-4">
                                <label>CEP *</label>
                                <input type="text" class="form-control required" id="cep" name="cep">
                                <div class="invalid-feedback">Informe o CEP.</div>
                            </div>

                            <div class="col-md-12">
                                <label>Endereço</label>
                                <input type="text" class="form-control" id="logradouro" readonly>
                            </div>
                        </div>

                        <div class="row g-2">

                            <div class="col-md-2">
                                <label>Número *</label>
                                <input type="text" class="form-control required" id="numero" name="numero">
                                <div class="invalid-feedback">Informe o numero.</div>
                            </div>

                            <div class="col-md-4">
                                <label>Bairro</label>
                                <input type="text" class="form-control" id="bairro" readonly>
                            </div>

                            <div class="col-md-4">
                                <label>Cidade</label>
                                <input type="text" class="form-control" id="cidade" readonly>
                            </div>

                            <div class="col-md-2">
                                <label>UF</label>
                                <input type="text" class="form-control" id="uf" readonly>
                            </div>
                        </div>

                        <div class="mb-3">
                            <label>Complemento</label>
                            <input type="text" class="form-control" id="complemento">

                        </div>
                    </div>
                </div>

                <!-- BOTÕES -->
                <div class="text-end mb-5">
                    <a href="<%=request.getContextPath()%>/view/admin/parceiro.jsp" class="btn btn-secondary">
                        <i class="fas fa-arrow-left"></i> Voltar
                    </a>

                  <button type="submit" class="btn btn-success" id="btnSalvar">
  <i class="fas fa-save"></i> Salvar Cadastro
</button>

                </div>

            </form>

        </div>

<%@ include file="../../pagina/footer.jsp" %>

</body>
</html>
