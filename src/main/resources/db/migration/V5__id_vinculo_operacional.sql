-- =====================================================================
-- V5 — id_vinculo nas tabelas operacionais
--
-- Liga cada lançamento (ponto, vale, falta, fechamento, folha, produção,
-- registroponto) ao VÍNCULO (período de trabalho) a que pertence, para que
-- os dados de cada passagem da pessoa pela empresa fiquem corretamente
-- atribuídos — mesmo após readmissão.
--
-- A coluna é nullable; o backfill resolve o vínculo pela DATA do lançamento
-- (admissao <= data <= coalesce(desligamento, data)). Como os períodos não
-- se sobrepõem, sempre resolve um único vínculo.
-- Idempotente.
-- =====================================================================

BEGIN;

-- 1) Colunas ------------------------------------------------------------
ALTER TABLE ponto_eletronico       ADD COLUMN IF NOT EXISTS id_vinculo INTEGER;
ALTER TABLE vale_funcionario       ADD COLUMN IF NOT EXISTS id_vinculo INTEGER;
ALTER TABLE falta_funcionario      ADD COLUMN IF NOT EXISTS id_vinculo INTEGER;
ALTER TABLE fechamento_folha_ponto ADD COLUMN IF NOT EXISTS id_vinculo INTEGER;
ALTER TABLE folha_pagamento        ADD COLUMN IF NOT EXISTS id_vinculo INTEGER;
ALTER TABLE lancamento_producao    ADD COLUMN IF NOT EXISTS id_vinculo INTEGER;
ALTER TABLE registroponto          ADD COLUMN IF NOT EXISTS id_vinculo INTEGER;

CREATE INDEX IF NOT EXISTS idx_ponto_vinculo      ON ponto_eletronico (id_vinculo);
CREATE INDEX IF NOT EXISTS idx_fechamento_vinculo ON fechamento_folha_ponto (id_vinculo);

-- 2) Backfill — tabelas com coluna DATE ---------------------------------
UPDATE ponto_eletronico t SET id_vinculo = (
    SELECT v.id_vinculo FROM vinculo_empregaticio v
    WHERE v.idpessoa = t.idpessoa
      AND (v.data_admissao IS NULL OR v.data_admissao <= t.dataregistro)
      AND (v.data_desligamento IS NULL OR v.data_desligamento >= t.dataregistro)
    ORDER BY v.data_admissao DESC NULLS LAST, v.id_vinculo DESC LIMIT 1
) WHERE t.id_vinculo IS NULL;

UPDATE vale_funcionario t SET id_vinculo = (
    SELECT v.id_vinculo FROM vinculo_empregaticio v
    WHERE v.idpessoa = t.idpessoa
      AND (v.data_admissao IS NULL OR v.data_admissao <= t.datavale)
      AND (v.data_desligamento IS NULL OR v.data_desligamento >= t.datavale)
    ORDER BY v.data_admissao DESC NULLS LAST, v.id_vinculo DESC LIMIT 1
) WHERE t.id_vinculo IS NULL;

UPDATE falta_funcionario t SET id_vinculo = (
    SELECT v.id_vinculo FROM vinculo_empregaticio v
    WHERE v.idpessoa = t.idpessoa
      AND (v.data_admissao IS NULL OR v.data_admissao <= t.datafalta)
      AND (v.data_desligamento IS NULL OR v.data_desligamento >= t.datafalta)
    ORDER BY v.data_admissao DESC NULLS LAST, v.id_vinculo DESC LIMIT 1
) WHERE t.id_vinculo IS NULL;

UPDATE registroponto t SET id_vinculo = (
    SELECT v.id_vinculo FROM vinculo_empregaticio v
    WHERE v.idpessoa = t.idfuncionario
      AND (v.data_admissao IS NULL OR v.data_admissao <= t.dataponto)
      AND (v.data_desligamento IS NULL OR v.data_desligamento >= t.dataponto)
    ORDER BY v.data_admissao DESC NULLS LAST, v.id_vinculo DESC LIMIT 1
) WHERE t.id_vinculo IS NULL;

-- 3) Backfill — tabelas com PERÍODO (YYYY-MM) ---------------------------
UPDATE fechamento_folha_ponto t SET id_vinculo = (
    SELECT v.id_vinculo FROM vinculo_empregaticio v
    WHERE v.idpessoa = t.idpessoa
      AND (v.data_admissao IS NULL OR v.data_admissao <= to_date(t.periodo || '-01','YYYY-MM-DD'))
      AND (v.data_desligamento IS NULL OR v.data_desligamento >= to_date(t.periodo || '-01','YYYY-MM-DD'))
    ORDER BY v.data_admissao DESC NULLS LAST, v.id_vinculo DESC LIMIT 1
) WHERE t.id_vinculo IS NULL;

UPDATE folha_pagamento t SET id_vinculo = (
    SELECT v.id_vinculo FROM vinculo_empregaticio v
    WHERE v.idpessoa = t.idfuncionario
      AND (v.data_admissao IS NULL OR v.data_admissao <= to_date(t.periodo || '-01','YYYY-MM-DD'))
      AND (v.data_desligamento IS NULL OR v.data_desligamento >= to_date(t.periodo || '-01','YYYY-MM-DD'))
    ORDER BY v.data_admissao DESC NULLS LAST, v.id_vinculo DESC LIMIT 1
) WHERE t.id_vinculo IS NULL;

UPDATE lancamento_producao t SET id_vinculo = (
    SELECT v.id_vinculo FROM vinculo_empregaticio v
    WHERE v.idpessoa = t.idfuncionario
      AND (v.data_admissao IS NULL OR v.data_admissao <= to_date(t.periodo || '-01','YYYY-MM-DD'))
      AND (v.data_desligamento IS NULL OR v.data_desligamento >= to_date(t.periodo || '-01','YYYY-MM-DD'))
    ORDER BY v.data_admissao DESC NULLS LAST, v.id_vinculo DESC LIMIT 1
) WHERE t.id_vinculo IS NULL;

COMMIT;
