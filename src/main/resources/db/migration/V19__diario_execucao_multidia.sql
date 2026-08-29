-- =====================================================================
-- V19 — Diário de Campo em execução (EM_ANDAMENTO) passa a aceitar mais de
-- uma "leva" de mão de obra em datas diferentes (dia 1 com equipe A, dia 2
-- com equipe B etc.), com o custo total somando todas.
--
-- data_execucao identifica a que dia cada lançamento de diario_funcionario
-- pertence. Backfill: registros existentes recebem a data do diário (data
-- da atividade), preservando o comportamento atual (1 diário = 1 dia).
-- Idempotente.
-- =====================================================================

BEGIN;

ALTER TABLE diario_funcionario ADD COLUMN IF NOT EXISTS data_execucao DATE;

UPDATE diario_funcionario df
SET data_execucao = dc.data
FROM diario_campo dc
WHERE dc.iddiario = df.iddiario AND df.data_execucao IS NULL;

CREATE INDEX IF NOT EXISTS idx_diariofunc_execucao ON diario_funcionario (iddiario, data_execucao);

COMMIT;
