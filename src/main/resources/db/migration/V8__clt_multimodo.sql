-- =====================================================================
-- V8 — CLT multi-modo (produção / empreita)
--
-- Um CLT pode trabalhar um dia em modo produção ou empreita, sem perder o
-- vínculo. Nesses dias não conta falta nem hora extra, e ganha por
-- produção/empreita (somado ao fechamento CLT). Guarda os ganhos no
-- fechamento para consulta/histórico.
-- Idempotente.
-- =====================================================================

BEGIN;

ALTER TABLE fechamento_folha_ponto
    ADD COLUMN IF NOT EXISTS valor_producao NUMERIC(10,2) NOT NULL DEFAULT 0.00,
    ADD COLUMN IF NOT EXISTS valor_empreita NUMERIC(10,2) NOT NULL DEFAULT 0.00;

COMMIT;
