-- =====================================================================
-- V10 — Tipo de Parceiro
--
-- Formaliza a classificação de negócio do parceiro numa coluna dedicada
-- (tipo_parceiro), separando-a do nivelpessoa (que segue sendo dado de
-- cadastro/login). Três papéis:
--   PARCEIRO   = compra a produção agrícola
--   FORNECEDOR = vende produto indiretamente (caixas, gasolina, etc.)
--   INSUMO     = vende produto usado direto na produção (defensivos e adubos)
--
-- Também corrige siteparceiro (site é opcional): remove NOT NULL e UNIQUE.
-- Idempotente.
-- =====================================================================

BEGIN;

-- 1) Nova coluna de classificação de negócio (só na tabela filha parceiro)
ALTER TABLE parceiro ADD COLUMN IF NOT EXISTS tipo_parceiro VARCHAR(20);

-- 2) Backfill a partir do nivelpessoa existente
UPDATE parceiro
   SET tipo_parceiro = CASE lower(coalesce(nivelpessoa, ''))
                           WHEN 'fornecedor' THEN 'FORNECEDOR'
                           WHEN 'insumo'     THEN 'INSUMO'
                           ELSE 'PARCEIRO'
                       END
 WHERE tipo_parceiro IS NULL;

-- 3) Site do parceiro passa a ser opcional (remove UNIQUE e NOT NULL)
ALTER TABLE parceiro DROP CONSTRAINT IF EXISTS parceiro_siteparceiro_key;
ALTER TABLE parceiro ALTER COLUMN siteparceiro DROP NOT NULL;

COMMIT;
