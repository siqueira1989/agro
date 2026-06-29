<!DOCTYPE html>
<%@ page contentType="text/html;charset=UTF-8" language="java"%>
<html lang="pt-br">
<head>
    <meta charset="UTF-8">
    <title>Agro - Cadastro de Funcionário</title>
    <%@ include file="../../pagina/importacao.html"%>
</head>
<body>
<header>
    <%@ include file="../../pagina/menu.jsp"%>
</header>

<div class="container-fluid content mt-2">

    <!-- TÍTULO -->
    <div class="card shadow-sm mb-4">
        <div class="card-body bg-success text-white rounded">
            <h4 id="tituloFormulario" class="mb-0">
                <i class="fas fa-user-plus me-2"></i>Cadastrar Funcionário
            </h4>
        </div>
    </div>

    <!-- ALERTA -->
    <div id="alerta" class="alert d-none" role="alert"></div>

    <form id="formFuncionario" novalidate>
        <input type="hidden" id="idPessoa" name="idPessoa">

        <!-- DADOS PESSOAIS / ACESSO -->
        <div class="card shadow-sm mb-4">
            <div class="card-header bg-white">
                <h5 class="text-success fw-bold mb-0">
                    <i class="fa-regular fa-address-card me-2"></i>Dados Pessoais e Acesso
                </h5>
            </div>
            <div class="card-body">
                <div class="row g-2">
                    <div class="col-md-12">
                        <label class="form-label">Nome *</label>
                        <div class="input-group">
                            <span class="input-group-text"><i class="fas fa-user"></i></span>
                            <input type="text" class="form-control" id="nomePessoa" name="nomePessoa"
                                   minlength="3" maxlength="120" required>
                            <div class="invalid-feedback">Informe o nome (mín. 3 caracteres).</div>
                        </div>
                    </div>
                </div>

                <div class="row g-2 mt-1">
                    <div class="col-md-6">
                        <label class="form-label">E-mail *</label>
                        <div class="input-group">
                            <span class="input-group-text"><i class="fas fa-envelope"></i></span>
                            <input type="email" class="form-control" id="emailPessoa" name="emailPessoa"
                                   maxlength="120" required>
                            <div class="invalid-feedback">Informe um e-mail válido.</div>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <label class="form-label">Telefone *</label>
                        <div class="input-group">
                            <span class="input-group-text"><i class="fas fa-phone"></i></span>
                            <input type="text" class="form-control" id="telefonePessoa" name="telefonePessoa" required>
                            <div class="invalid-feedback">Informe um telefone válido.</div>
                        </div>
                    </div>
                </div>

                <div class="row g-2 mt-1">
                    <div class="col-md-4">
                        <label class="form-label">Usuário *</label>
                        <div class="input-group">
                            <span class="input-group-text"><i class="fas fa-user-circle"></i></span>
                            <input type="text" class="form-control" id="usuarioPessoa" name="usuarioPessoa"
                                   minlength="3" maxlength="40" required>
                            <div class="invalid-feedback">Informe o usuário (3 a 40 chars).</div>
                        </div>
                    </div>
                    <div class="col-md-4">
                        <label class="form-label">Senha *</label>
                        <div class="input-group">
                            <span class="input-group-text"><i class="fas fa-lock"></i></span>
                            <input type="password" class="form-control" id="senhaPessoa" name="senhaPessoa"
                                   minlength="4" maxlength="60" required>
                            <button class="btn btn-outline-secondary" type="button" id="btnToggleSenha" title="Mostrar/ocultar">
                                <i class="fa-regular fa-eye"></i>
                            </button>
                            <div class="invalid-feedback">Informe uma senha (mín. 4).</div>
                        </div>
                    </div>
                    <div class="col-md-2">
                        <label class="form-label">Nível *</label>
                        <div class="input-group">
                            <span class="input-group-text"><i class="fas fa-layer-group"></i></span>
                            <select class="form-select" id="nivelPessoa" name="nivelPessoa" required>
                                <option value="">Selecione</option>
                                <option value="admin">Admin</option>
                                <option value="gerente">Gerente</option>
                                <option value="operador">Operador</option>
                            </select>
                            <div class="invalid-feedback">Selecione o nível.</div>
                        </div>
                    </div>
                    <div class="col-md-2">
                        <label class="form-label">Situação</label>
                        <div class="input-group">
                            <span class="input-group-text"><i class="fas fa-toggle-on"></i></span>
                            <select class="form-select" id="situacaoPessoa" name="situacaoPessoa">
                                <option value="true" selected>Ativo</option>
                                <option value="false">Inativo</option>
                            </select>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- DADOS PROFISSIONAIS -->
        <div class="card shadow-sm mb-4">
            <div class="card-header bg-white">
                <h5 class="text-success fw-bold mb-0">
                    <i class="fas fa-briefcase me-2"></i>Dados Profissionais
                </h5>
            </div>
            <div class="card-body">
                <div class="row g-2">
                    <div class="col-md-4">
                        <label class="form-label">CPF *</label>
                        <div class="input-group">
                            <span class="input-group-text"><i class="fas fa-id-card"></i></span>
                            <input type="text" class="form-control" id="cpfPf" name="cpfPf" required>
                            <div class="invalid-feedback">Informe um CPF válido.</div>
                        </div>
                    </div>
                    <div class="col-md-4">
                        <label class="form-label">Data Nascimento *</label>
                        <div class="input-group date" id="grpDataNasc">
                            <span class="input-group-text"><i class="fas fa-calendar-alt"></i></span>
                            <input type="text" class="form-control" id="dataNascimentoPf" name="dataNascimentoPf"
                                   placeholder="dd/mm/aaaa" autocomplete="off" required>
                            <button class="btn btn-outline-secondary" type="button" id="btnAbrirCalendario" title="Selecionar data">
                                <i class="fas fa-calendar"></i>
                            </button>
                            <div class="invalid-feedback">Informe uma data válida (dd/mm/aaaa).</div>
                        </div>
                    </div>
                    <div class="col-md-4">
                        <label class="form-label">Matrícula *</label>
                        <div class="input-group">
                            <span class="input-group-text"><i class="fas fa-hashtag"></i></span>
                            <input type="text" class="form-control" id="matricula" name="matricula" maxlength="30" required>
                            <div class="invalid-feedback">Informe a matrícula.</div>
                        </div>
                    </div>
                </div>

                <div class="row g-2 mt-1">
                    <div class="col-md-4">
                        <label class="form-label">Tipo Funcionário *</label>
                        <div class="input-group">
                            <span class="input-group-text"><i class="fas fa-users-cog"></i></span>
                            <select class="form-select" id="tipoFuncionario" name="tipoFuncionario" required>
                                <option value="">Selecione</option>
                                <option value="CLT">CLT</option>
                                <option value="DIARISTA">Diária</option>
                                <option value="EMPREITA">Empreita</option>
                                <option value="PRODUCAO">Produção</option>
                            </select>
                            <div class="invalid-feedback">Selecione o tipo.</div>
                        </div>
                    </div>
                    <div class="col-md-8">
                        <label class="form-label">Cargo *</label>
                        <div class="input-group">
                            <span class="input-group-text"><i class="fas fa-user-tie"></i></span>
                            <input type="text" class="form-control" id="cargo" name="cargo"
                                   minlength="2" maxlength="80" required>
                            <div class="invalid-feedback">Informe o cargo (mín. 2 caracteres).</div>
                        </div>
                    </div>
                </div>

                <div class="row g-2 mt-1">
                    <div class="col-md-4">
                        <label class="form-label">Data de Admissão</label>
                        <div class="input-group date" id="grpDataInicio">
                            <span class="input-group-text"><i class="fas fa-calendar-plus"></i></span>
                            <input type="text" class="form-control" id="dataInicio" name="dataInicio"
                                   placeholder="dd/mm/aaaa" autocomplete="off">
                            <button class="btn btn-outline-secondary" type="button" id="btnAbrirCalInicio" title="Selecionar data">
                                <i class="fas fa-calendar"></i>
                            </button>
                        </div>
                        <small class="text-muted">Quando o funcionário entrou na empresa.</small>
                    </div>
                    <div class="col-md-4">
                        <label class="form-label">Data de Desligamento</label>
                        <div class="input-group date" id="grpDataFim">
                            <span class="input-group-text"><i class="fas fa-calendar-minus"></i></span>
                            <input type="text" class="form-control" id="dataFim" name="dataFim"
                                   placeholder="dd/mm/aaaa" autocomplete="off">
                            <button class="btn btn-outline-secondary" type="button" id="btnAbrirCalFim" title="Selecionar data">
                                <i class="fas fa-calendar"></i>
                            </button>
                        </div>
                        <small class="text-muted">Preencher apenas ao desligar o funcionário.</small>
                    </div>
                </div>

                <div id="boxTipoInfo" class="alert alert-info mt-2 mb-0">
                    Selecione o <strong>Tipo Funcionário</strong> para ver dicas.
                </div>
            </div>
        </div>

        <!-- ENDEREÇO -->
        <div class="card shadow-sm mb-5">
            <div class="card-header bg-white">
                <h5 class="text-success fw-bold mb-0">
                    <i class="fas fa-map-marker-alt me-2"></i>Endereço
                </h5>
            </div>
            <div class="card-body">
                <div class="row g-2">
                    <div class="col-md-4">
                        <label class="form-label">CEP *</label>
                        <div class="input-group">
                            <span class="input-group-text"><i class="fas fa-map-pin"></i></span>
                            <input type="text" class="form-control" id="cep" name="cep" required>
                            <div class="invalid-feedback">Informe o CEP.</div>
                        </div>
                    </div>
                    <div class="col-md-8">
                        <label class="form-label">Endereço</label>
                        <div class="input-group">
                            <span class="input-group-text"><i class="fas fa-road"></i></span>
                            <input type="text" class="form-control" id="logradouro" name="logradouro" readonly>
                        </div>
                    </div>
                </div>

                <div class="row g-2 mt-1">
                    <div class="col-md-2">
                        <label class="form-label">Número *</label>
                        <div class="input-group">
                            <span class="input-group-text"><i class="fas fa-sort-numeric-up"></i></span>
                            <input type="text" class="form-control" id="numero" name="numero" required>
                            <div class="invalid-feedback">Informe o número.</div>
                        </div>
                    </div>
                    <div class="col-md-4">
                        <label class="form-label">Bairro</label>
                        <div class="input-group">
                            <span class="input-group-text"><i class="fas fa-city"></i></span>
                            <input type="text" class="form-control" id="bairro" name="bairro" readonly>
                        </div>
                    </div>
                    <div class="col-md-4">
                        <label class="form-label">Cidade</label>
                        <div class="input-group">
                            <span class="input-group-text"><i class="fas fa-building"></i></span>
                            <input type="text" class="form-control" id="cidade" name="cidade" readonly>
                        </div>
                    </div>
                    <div class="col-md-2">
                        <label class="form-label">UF</label>
                        <div class="input-group">
                            <span class="input-group-text"><i class="fas fa-flag"></i></span>
                            <input type="text" class="form-control" id="uf" name="uf" readonly>
                        </div>
                    </div>
                </div>

                <div class="mt-2">
                    <label class="form-label">Complemento</label>
                    <div class="input-group">
                        <span class="input-group-text"><i class="fas fa-align-left"></i></span>
                        <input type="text" class="form-control" id="complemento" name="complemento" maxlength="80">
                    </div>
                </div>
            </div>
        </div>

        <!-- BOTÕES -->
        <div class="text-end mb-5">
            <a href="${pageContext.request.contextPath}/view/admin/Colaboradores.jsp" class="btn btn-secondary">
                <i class="fas fa-arrow-left me-1"></i>Voltar
            </a>
            <button type="button" class="btn btn-outline-secondary ms-2" id="btnLimpar">
                <i class="fas fa-eraser me-1"></i>Limpar
            </button>
            <button type="submit" class="btn btn-success ms-2" id="btnSalvar">
                <i class="fas fa-save me-1"></i>Salvar Cadastro
            </button>
        </div>
    </form>
</div>

<%@ include file="../../pagina/footer.jsp"%>

</body>
</html>
