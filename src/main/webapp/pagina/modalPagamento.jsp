<!-- Modal de Pagamento (compartilhado pelos perfis) -->
<div class="modal fade" id="modalPagamento" tabindex="-1">
    <div class="modal-dialog"><div class="modal-content">
        <div class="modal-header bg-success text-white">
            <h5 class="modal-title"><i class="fas fa-money-check-dollar me-2"></i>Gerar Pagamento</h5>
            <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
        </div>
        <div class="modal-body">
            <div id="alertaPagamento" class="alert d-none"></div>
            <div id="pgPrazoInfo" class="alert alert-info py-1 small d-none"></div>
            <div class="row g-2">
                <div class="col-6">
                    <label class="form-label">Valor (R$) *</label>
                    <div class="input-group"><span class="input-group-text">R$</span>
                        <input type="text" class="form-control" id="pgValor" placeholder="0,00"></div>
                </div>
                <div class="col-6">
                    <label class="form-label">Data do Pagamento</label>
                    <input type="date" class="form-control" id="pgData">
                </div>
            </div>
            <div class="mb-2 mt-2">
                <label class="form-label">Forma de Pagamento *</label>
                <select class="form-select" id="pgForma">
                    <option value="PIX">PIX</option>
                    <option value="TRANSFERENCIA">Transferência Bancária</option>
                    <option value="DINHEIRO">Dinheiro</option>
                    <option value="CHEQUE">Cheque</option>
                </select>
            </div>
            <!-- PIX -->
            <div class="mb-2 d-none" id="pgRowPix">
                <label class="form-label">Chave PIX *</label>
                <input type="text" class="form-control" id="pgChavePix" maxlength="120" placeholder="CPF, e-mail, telefone ou aleatória">
            </div>
            <!-- Transferência -->
            <div class="row g-2 d-none" id="pgRowBanco">
                <div class="col-md-6"><label class="form-label">Banco *</label>
                    <input type="text" class="form-control" id="pgBanco" maxlength="60"></div>
                <div class="col-md-3"><label class="form-label">Agência *</label>
                    <input type="text" class="form-control" id="pgAgencia" maxlength="20"></div>
                <div class="col-md-3"><label class="form-label">Conta *</label>
                    <input type="text" class="form-control" id="pgConta" maxlength="30"></div>
            </div>
            <div class="mt-2">
                <label class="form-label">Observação</label>
                <input type="text" class="form-control" id="pgObs" maxlength="200">
            </div>
        </div>
        <div class="modal-footer">
            <button class="btn btn-secondary" data-bs-dismiss="modal">Cancelar</button>
            <button class="btn btn-success" id="btnSalvarPagamento"><i class="fas fa-save me-1"></i>Registrar Pagamento</button>
        </div>
    </div></div>
</div>
