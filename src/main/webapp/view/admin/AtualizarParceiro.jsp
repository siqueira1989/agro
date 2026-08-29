<!DOCTYPE html>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="Model.Model.Parceiro" %>
<%@ page import="Util.HtmlUtil" %>
<%
    Parceiro parceiro = (Parceiro) request.getAttribute("parceiro");
%>
<html lang="pt-br">
<head>
    <meta charset="UTF-8">
    <title>Agro - Atualização de Parceiros</title>
    <%@ include file="../../pagina/importacao.html"%>
</head>
<body>
    <header class="header">
        <%@ include file="../../pagina/menu.jsp" %>
    </header>
    
    <div class="container my-4">
        <div class="card shadow">
            <div class="card-header bg-success text-white">
                <h2 class="mb-0"><i class="fas fa-user-edit"></i> Atualização de Parceiros</h2>
            </div>
            <div class="card-body">
                <!-- Exibição de mensagem de alerta -->
                <%
                    String Mensagem = (String) request.getAttribute("Mensagem");
                    String Atributo = (String) request.getAttribute("Atributo");
                    if (Mensagem != null && !Mensagem.trim().isEmpty() && Atributo != null && !Atributo.trim().isEmpty()) {
                %>
                <div class="alert alert-<%= HtmlUtil.esc(Atributo) %> alert-dismissible fade show" role="alert">
                    <strong><i class="fas fa-exclamation-triangle"></i> Atenção!</strong> <%= HtmlUtil.esc(Mensagem) %>
                    <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Fechar"></button>
                </div>
                <%
                    }
                %>
                
                <form method="post" action="<%= request.getContextPath() %>/ControllerParceiro" class="needs-validation" novalidate>
                    <!-- Dados Pessoais -->
                    <div class="mb-4">
                        <h4 class="text-success mb-3"><i class="fas fa-user"></i> Dados Pessoais</h4>
                        <div class="row g-2">
                            <div class="col-md-12">
                                <label for="nomepessoa"><i class="fas fa-user"></i> Nome Completo</label>
                                <input type="text" class="form-control" id="nomepessoa" name="nomepessoa" value="<%= HtmlUtil.esc(parceiro.getNomePessoa()) %>" required>
                                <div class="invalid-feedback">O nome não pode ser vazio.</div>
                            </div>
                        </div>
                        <div class="row g-2">
                            <div class="col-md-4">
                                <label for="usuariopessoa"><i class="fas fa-user-tag"></i> Usuário</label>
                                <input type="text" class="form-control" id="usuariopessoa" name="usuariopessoa" value="<%= HtmlUtil.esc(parceiro.getUsuarioPessoa()) %>" required>
                                <div class="invalid-feedback">O nome de usuário não pode ser vazio.</div>
                            </div>
                            <div class="col-md-4">
                                <label for="senhapessoa"><i class="fas fa-lock"></i> Senha</label>
                                <input type="password" class="form-control" id="senhapessoa" name="senhapessoa"
                                       minlength="6" autocomplete="new-password"
                                       placeholder="Deixe em branco para manter a senha atual">
                                <div class="form-text">
                                    A senha nao e mais exibida aqui (auditoria P0-2). Preencha
                                    apenas se quiser troca-la; em branco, a atual e mantida.
                                </div>
                                <div class="invalid-feedback">A senha deve ter pelo menos 6 caracteres.</div>
                            </div>
                            <div class="col-md-4">
                                <label for="nivelpessoa"><i class="fas fa-user-cog"></i> Nível</label>
                                <input type="text" class="form-control" id="nivelpessoa" name="nivelpessoa" value="Parceiro" readonly>
                                <div class="invalid-feedback">O nível não pode ser vazio.</div>
                            </div>
                        </div>
                        <div class="row g-2">
                            <div class="col-md-12">
                                <label for="emailpessoa"><i class="fas fa-envelope"></i> Email</label>
                                <input type="email" class="form-control" id="emailpessoa" name="emailpessoa" value="<%= HtmlUtil.esc(parceiro.getEmailPessoa()) %>" required>
                                <div class="invalid-feedback">Informe um email válido.</div>
                            </div>
                        </div>
                        <div class="row g-2">
                            <div class="col-md-6">
                                <label for="situacaopessoa"><i class="fas fa-hourglass-half"></i> Situação</label>
                                <select class="form-control" id="situacaopessoa" name="situacaopessoa" required>
                                    <option value="true" <%= Boolean.TRUE.equals(parceiro.isSituacaoPessoa()) ? "selected" : "" %>>✔️ Ativo</option>
                                    <option value="false" <%= Boolean.FALSE.equals(parceiro.isSituacaoPessoa()) ? "selected" : "" %>>❌ Inativo</option>
                                </select>
                                <div class="invalid-feedback">A situação não pode ser vazia.</div>
                            </div>
                            <div class="col-md-6">
                                <label for="telefonepessoa"><i class="fas fa-phone"></i> Telefone</label>
                                <input type="text" class="form-control" id="telefonepessoa" name="telefonepessoa" value="<%= HtmlUtil.esc(parceiro.getTelefonePessoa()) %>" required>
                                <div class="invalid-feedback">O telefone não pode ser vazio.</div>
                            </div>
                        </div>
                    </div>
                    
                    <!-- Dados da Empresa Parceira -->
                    <div class="mb-4">
                        <h4 class="text-success mb-3"><i class="fas fa-building"></i> Dados da Empresa Parceira</h4>
                        <div class="row g-2">
                            <div class="col-md-12">
                                <label for="razaoSocialPessoaCnpj"><i class="fas fa-briefcase"></i> Razão Social</label>
                                <input type="text" class="form-control" id="razaoSocialPessoaCnpj" name="razaoSocialPessoaCnpj" value="<%= HtmlUtil.esc(parceiro.getRazaoSocialPessoaCnpj()) %>" required>
                                <div class="invalid-feedback">A razão social não pode ser vazia.</div>
                            </div>
                        </div>
                        <div class="row g-2">
                            <div class="col-md-6">
                                <label for="cnpjPessoaCnpj"><i class="fas fa-id-card"></i> CNPJ</label>
                                <input type="text" class="form-control" id="cnpjPessoaCnpj" name="cnpjPessoaCnpj" value="<%= HtmlUtil.esc(parceiro.getCnpjPessoaCnpj()) %>" required>
                                <div class="invalid-feedback">O CNPJ não pode ser vazio.</div>
                            </div>
                            <div class="col-md-6">
                                <label for="inscricaoEstadualPessoaCnpj"><i class="fas fa-file-alt"></i> Inscrição Estadual</label>
                                <input type="text" class="form-control" id="inscricaoEstadualPessoaCnpj" name="inscricaoEstadualPessoaCnpj" value="<%= HtmlUtil.esc(parceiro.getInscricaoEstadualPessoaCnpj()) %>" required>
                                <div class="invalid-feedback">A inscrição estadual não pode ser vazia.</div>
                            </div>
                        </div>
                        <div class="row g-2">
                            <div class="col-md-8">
                                <label for="siteparceiro"><i class="fas fa-globe"></i> Site do Parceiro</label>
                                <input type="text" class="form-control" id="siteparceiro" name="siteparceiro" value="<%= HtmlUtil.esc(parceiro.getSiteparceiro()) %>" required>
                                <div class="invalid-feedback">O site não pode ser vazio.</div>
                            </div>
                        </div>
                    </div>
                    
                    <!-- Endereço -->
                    <div class="mb-4">
                        <h4 class="text-success mb-3"><i class="fas fa-map-marker-alt"></i> Endereço</h4>
                        <div class="row g-2">
                            <div class="col-md-4">
                                <label for="cep"><i class="fas fa-map-pin"></i> CEP</label>
                                <input type="text" class="form-control" id="cep" name="cep" value="<%= HtmlUtil.esc(parceiro.getEndereco().getCep()) %>" maxlength="9" onblur="pesquisacep(this.value);" required>
                                <div class="invalid-feedback">O CEP não pode ser vazio.</div>
                            </div>
                        </div>
                        <div class="row g-2">
                            <div class="col-md-12">
                                <label for="endereco"><i class="fas fa-road"></i> Endereço</label>
                                <input type="text" class="form-control" id="endereco" name="endereco" value="<%= HtmlUtil.esc(parceiro.getEndereco().getEndereco()) %>" required>
                                <div class="invalid-feedback">O endereço não pode ser vazio.</div>
                            </div>
                        </div>
                        <div class="row g-2">
                            <div class="col-md-4">
                                <label for="numero"><i class="fas fa-thumbtack"></i> Número</label>
                                <input type="text" class="form-control" id="numero" name="numero" value="<%= HtmlUtil.esc(parceiro.getNumero()) %>" required>
                                <div class="invalid-feedback">O número não pode ser vazio.</div>
                            </div>
                            <div class="col-md-8">
                                <label for="complemento"><i class="fas fa-info-circle"></i> Complemento</label>
                                <input type="text" class="form-control" id="complemento" name="complemento" value="<%= HtmlUtil.esc(parceiro.getComplemento()) %>">
                            </div>
                        </div>
                        <div class="row g-2">
                            <div class="col-md-6">
                                <label for="bairro"><i class="fas fa-map-marker-alt"></i> Bairro</label>
                                <input type="text" class="form-control" id="bairro" name="bairro" value="<%= HtmlUtil.esc(parceiro.getEndereco().getBairro()) %>" required>
                                <div class="invalid-feedback">O bairro não pode ser vazio.</div>
                            </div>
                            <div class="col-md-6">
                                <label for="cidade"><i class="fas fa-city"></i> Cidade</label>
                                <input type="text" class="form-control" id="cidade" name="cidade" value="<%= HtmlUtil.esc(parceiro.getEndereco().getCidade()) %>" required>
                                <div class="invalid-feedback">A cidade não pode ser vazia.</div>
                            </div>
                        </div>
                        <div class="row g-2">
                            <div class="col-md-6">
                                <label for="estado"><i class="fas fa-flag"></i> Estado</label>
                                <input type="text" class="form-control" id="estado" name="estado" value="<%= HtmlUtil.esc(parceiro.getEndereco().getEstado()) %>" required>
                                <div class="invalid-feedback">O estado não pode ser vazio.</div>
                            </div>
                            <div class="col-md-6">
                                <label for="pais"><i class="fas fa-globe-americas"></i> País</label>
                                <input type="text" class="form-control" id="pais" name="pais" value="<%= HtmlUtil.esc(parceiro.getEndereco().getPais()) %>" required>
                                <div class="invalid-feedback">O país não pode ser vazio.</div>
                            </div>
                        </div>
                    </div>
                    
                    <input type="hidden" name="acao" value="Atualizar">
                    <input type="hidden" name="idpessoa" value="<%= HtmlUtil.esc(parceiro.getIdPessoa()) %>">
                    <div class="text-center">
                        <button type="submit" class="btn btn-warning">
                            <i class="fas fa-save"></i> Atualizar
                        </button>
                    </div>
                </form>
            </div>
        </div>
    </div>
    
    <%@ include file="../../pagina/footer.jsp" %>
</body>
</html>
