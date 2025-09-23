
<%@ page contentType="text/html;charset=UTF-8" language="java"%>
<html>
    <head>
        <title>Info4Cloud - Alimento</title>
    <%@ include file="../../pagina/importacao.html"%>
    <script>
    $(function () {
        const alimentoJson = sessionStorage.getItem('alimentoParaAtualizar');
        console.log("Recebido:", alimentoJson);

        if (alimentoJson) {
            const alimento = JSON.parse(alimentoJson);
            
            $('#alimento').val(alimento.nomeproduto);
            $('#variedade').val(alimento.variedadealimento);

            // Limpa o sessionStorage
            sessionStorage.removeItem('alimentoParaAtualizar');
        }
    });
</script>

    

    </head>
    <body>
        <header>
            <%@ include file="../../pagina/menu.jsp"%>
        </header>

        <div class="container-fluid content mt-2">
             <div class="bg-success text-white p-3 rounded mb-4">
            <h2 class="m-0"><i class="fas fa-sync mr-1"></i>Painel Administrativo</h2> 
             </div>

            <!-- Formulário de Alimento -->
            <div class="card mb-4">
                <div class="card-header">
                    Formulário de Atualização
                </div>
                <div class="card-body">
                    <form>
                        <div class="form-row">
                            <div class="form-group col-md-6">
                                <label for="alimento">Nome do Alimento</label>
                                <input type="text" class="form-control" id="alimento" name="alimento" placeholder="Ex: Tomate">
                            </div>
                            <div class="form-group col-md-6">
                                <label for="variedade">Variedade</label>
                                <input type="text" class="form-control" id="variedade"  name="variedade" placeholder="Ex: Italiano">
                            </div>
                        </div>
                        <div class="d-flex justify-content-end">
                            <button type="submit" class="btn btn-warning mr-2">
                               <i class="far fa-edit"></i> Atualizar
                            </button>
                            <button type="reset" class="btn btn-secondary">
                                Cancelar
                            </button>
                        </div>
                    </form>
                </div>
            </div>

            <!-- Tabela de Classificações -->
            <div class="card">
                <div class="card-header">
                    Classificações Cadastradas
                </div>
                <div class="card-body">

                    <!-- Botão logo abaixo do título -->
                    <div class="mb-3 text-right">
                        <button class="btn btn-primary" data-toggle="modal" data-target="#modalClassificacao">
                            <i class="fas fa-plus"></i> Adicionar Classificação
                        </button>
                    </div>

                    <div class="table-responsive">
                        <table class="table table-bordered table-hover">
                            <thead class="thead-light">
                                <tr>
                                    <th>Código</th>
                                    <th>Classificação</th>
                                    <th class="text-center">Ações</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr>
                                    <td>001</td>
                                    <td>Classe A</td>
                                    <td class="text-center">
                                        <button class="btn btn-danger btn-sm">
                                            <i class="fas fa-trash"></i> Excluir
                                        </button>
                                    </td>
                                </tr>
                                <tr>
                                    <td>002</td>
                                    <td>Classe B</td>
                                    <td class="text-center">
                                        <button class="btn btn-danger btn-sm">
                                            <i class="fas fa-trash"></i> Excluir
                                        </button>
                                    </td>
                                </tr>
                                <tr>
                                    <td>003</td>
                                    <td>Classe Especial</td>
                                    <td class="text-center">
                                        <button class="btn btn-danger btn-sm">
                                            <i class="fas fa-trash"></i> Excluir
                                        </button>
                                    </td>
                                </tr>
                            </tbody>
                        </table>
                    </div>

                </div>
            </div>
        </div>
        <!-- Modal Classificação -->
        <div class="modal fade" id="modalClassificacao" tabindex="-1" role="dialog" aria-hidden="true">
            <div class="modal-dialog modal-md" role="document">
                <div class="modal-content">

                    <div class="modal-header bg-primary text-white">
                        <h5 class="modal-title">Adicionar Classificação</h5>
                        <button type="button" class="close text-white" data-dismiss="modal" aria-label="Fechar">
                            <span aria-hidden="true">&times;</span>
                        </button>
                    </div>

                    <div class="modal-body">
                        <form>
                            <div class="form-group">
                                <label for="AlimentoText">Alimento</label>
                                <input type="text" readonly class="form-control-plaintext font-weight-bold" id="AlimentoText" value="Tomate - Italiano">
                            </div>

                            <div class="form-group">
                                <label for="classificacaoSelect">Classificações disponíveis</label>
                                <select id="classificacaoSelect" class="custom-select" multiple>
                                    <option value="1">Classe A</option>
                                    <option value="2">Classe B</option>
                                    <option value="3">Classe Especial</option>
                                </select>
                                <small class="form-text text-muted">
                                    Segure Ctrl (ou Cmd no Mac) para selecionar mais de uma.
                                </small>
                            </div>
                        </form>
                    </div>

                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-dismiss="modal">Cancelar</button>
                        <button type="button" class="btn btn-primary">Salvar</button>
                    </div>

                </div>
            </div>
        </div>
        <footer>
            <%@ include file="../../pagina/footer.jsp"%>
        </footer>
    </body>
</html>
