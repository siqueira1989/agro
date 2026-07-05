-- =====================================================================
-- V9 — Pagamentos
--
-- Registra pagamentos de qualquer tipo de funcionário, com a forma de
-- pagamento (PIX / transferência bancária / dinheiro / cheque) e os dados
-- bancários (banco, agência, conta). Vincula ao período pago.
-- Idempotente.
-- =====================================================================

BEGIN;

CREATE TABLE IF NOT EXISTS pagamento (
    idpagamento      SERIAL PRIMARY KEY,
    idpessoa         INTEGER NOT NULL,
    id_vinculo       INTEGER,
    periodo          VARCHAR(7) NOT NULL,      -- YYYY-MM referente ao cálculo
    tipo_funcionario VARCHAR(20),
    valor            NUMERIC(12,2) NOT NULL DEFAULT 0,
    forma_pagamento  VARCHAR(20) NOT NULL,     -- PIX | TRANSFERENCIA | DINHEIRO | CHEQUE
    banco            VARCHAR(60),
    agencia          VARCHAR(20),
    conta            VARCHAR(30),
    chave_pix        VARCHAR(120),
    data_pagamento   DATE NOT NULL DEFAULT CURRENT_DATE,
    observacao       VARCHAR(200),
    criado_em        TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_pagamento_pessoa ON pagamento (idpessoa, periodo);

-- Validação de idpessoa (reutiliza função da V2)
DROP TRIGGER IF EXISTS chk_func_exist ON pagamento;
CREATE TRIGGER chk_func_exist BEFORE INSERT OR UPDATE ON pagamento
    FOR EACH ROW EXECUTE FUNCTION trg_valida_funcionario_existe('idpessoa');

-- Marca lançamentos de produção como pagos (empreita já tem 'pago')
ALTER TABLE producao_caixa ADD COLUMN IF NOT EXISTS pago BOOLEAN NOT NULL DEFAULT false;

-- Cascade ao excluir o funcionário
CREATE OR REPLACE FUNCTION trg_cascade_delete_funcionario()
RETURNS trigger AS $$
BEGIN
    DELETE FROM ponto_eletronico       WHERE idpessoa      = OLD.idpessoa;
    DELETE FROM falta_funcionario      WHERE idpessoa      = OLD.idpessoa;
    DELETE FROM vale_funcionario       WHERE idpessoa      = OLD.idpessoa;
    DELETE FROM fechamento_folha_ponto WHERE idpessoa      = OLD.idpessoa;
    DELETE FROM folha_pagamento        WHERE idfuncionario = OLD.idpessoa;
    DELETE FROM registroponto          WHERE idfuncionario = OLD.idpessoa;
    DELETE FROM lancamento_empreita    WHERE idpessoa      = OLD.idpessoa;
    DELETE FROM producao_caixa         WHERE idpessoa      = OLD.idpessoa;
    DELETE FROM preco_caixa            WHERE idpessoa      = OLD.idpessoa;
    DELETE FROM pagamento              WHERE idpessoa      = OLD.idpessoa;
    DELETE FROM vinculo_empregaticio   WHERE idpessoa      = OLD.idpessoa;
    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

COMMIT;
