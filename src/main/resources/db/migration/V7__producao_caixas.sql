-- =====================================================================
-- V7 — Produção por caixas + histórico de preços
--
-- Funcionário de produção ganha por caixa produzida (quantidade × preço).
--   - Colhedor produz "Caixa Contentor".
--   - Embalador produz caixas de papelão/plástico/madeira.
-- Preços têm HISTÓRICO por ano e podem variar por funcionário (competência).
-- Cada lançamento guarda o preço unitário aplicado (snapshot), preservando
-- o valor histórico mesmo que o preço mude depois.
-- Idempotente.
-- =====================================================================

BEGIN;

-- Tabela de preços por tipo de caixa, com histórico (ano) e opcional por funcionário
CREATE TABLE IF NOT EXISTS preco_caixa (
    idpreco     SERIAL PRIMARY KEY,
    tipo_caixa  VARCHAR(30) NOT NULL,
    ano         INTEGER NOT NULL,
    idpessoa    INTEGER,          -- NULL = preço geral do ano; preenchido = preço específico do funcionário
    preco       NUMERIC(12,2) NOT NULL DEFAULT 0,
    criado_em   TIMESTAMP NOT NULL DEFAULT now()
);
-- 1 preço geral por (tipo, ano) e 1 preço por (tipo, ano, funcionário)
CREATE UNIQUE INDEX IF NOT EXISTS uk_preco_geral
    ON preco_caixa (tipo_caixa, ano) WHERE idpessoa IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_preco_func
    ON preco_caixa (tipo_caixa, ano, idpessoa) WHERE idpessoa IS NOT NULL;

-- Lançamentos de caixas produzidas (por dia e tipo)
CREATE TABLE IF NOT EXISTS producao_caixa (
    idproducao     SERIAL PRIMARY KEY,
    idpessoa       INTEGER NOT NULL,
    data_producao  DATE NOT NULL,
    tipo_caixa     VARCHAR(30) NOT NULL,
    quantidade     INTEGER NOT NULL DEFAULT 0,
    preco_unitario NUMERIC(12,2) NOT NULL DEFAULT 0,
    id_vinculo     INTEGER,
    criado_em      TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_producao_caixa_pessoa ON producao_caixa (idpessoa);

-- Validação de idpessoa (reutiliza função da V2)
DROP TRIGGER IF EXISTS chk_func_exist ON producao_caixa;
CREATE TRIGGER chk_func_exist BEFORE INSERT OR UPDATE ON producao_caixa
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
    DELETE FROM producao_caixa         WHERE idpessoa      = OLD.idpessoa;
    DELETE FROM preco_caixa            WHERE idpessoa      = OLD.idpessoa;
    DELETE FROM vinculo_empregaticio   WHERE idpessoa      = OLD.idpessoa;
    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

COMMIT;
