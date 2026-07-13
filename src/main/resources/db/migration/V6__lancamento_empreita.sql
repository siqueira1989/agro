-- =====================================================================
-- V6 — Lançamentos de empreita (dia + valor)
--
-- O funcionário de empreita recebe por serviço/dia. Cada lançamento é um
-- dia de empreita com um valor. O perfil soma os lançamentos do período,
-- gerencia vales e gera pagamentos.
-- Idempotente.
-- =====================================================================

BEGIN;

CREATE TABLE IF NOT EXISTS lancamento_empreita (
    idlancamento   SERIAL PRIMARY KEY,
    idpessoa       INTEGER NOT NULL,
    data_empreita  DATE NOT NULL,
    descricao      VARCHAR(200),
    valor          NUMERIC(12,2) NOT NULL DEFAULT 0,
    id_vinculo     INTEGER,
    pago           BOOLEAN NOT NULL DEFAULT false,
    criado_em      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_lanc_empreita_pessoa ON lancamento_empreita (idpessoa);

-- Validação de idpessoa contra a hierarquia (reutiliza função da V2)
DROP TRIGGER IF EXISTS chk_func_exist ON lancamento_empreita;
CREATE TRIGGER chk_func_exist BEFORE INSERT OR UPDATE ON lancamento_empreita
    FOR EACH ROW EXECUTE FUNCTION trg_valida_funcionario_existe('idpessoa');

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
    DELETE FROM vinculo_empregaticio   WHERE idpessoa      = OLD.idpessoa;
    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

COMMIT;
