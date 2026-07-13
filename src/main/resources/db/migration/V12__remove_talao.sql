-- =====================================================================
-- V12 — Remoção do módulo Talão
--
-- O talão duplicava a função da Área de Produção, que permanece no sistema.
-- Removem-se as tabelas do talão na ordem das dependências (FK):
--   talaofinanceiro → talao → lancamentotalao.
--
-- Seguro para a Área de Produção: 'talao' é que referenciava 'areaproducao'
-- (e 'alimento'); nenhuma dessas tabelas depende de 'talao'. 'lancamentotalao'
-- referenciava apenas 'despesascusto'. Portanto o DROP não afeta
-- areaproducao, alimento nem despesascusto.
-- Idempotente.
-- =====================================================================

BEGIN;

DROP TABLE IF EXISTS talaofinanceiro;   -- referencia talao
DROP TABLE IF EXISTS talao;             -- referencia areaproducao e alimento
DROP TABLE IF EXISTS lancamentotalao;   -- referencia despesascusto

COMMIT;
