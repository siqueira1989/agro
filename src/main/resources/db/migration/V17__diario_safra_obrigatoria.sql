-- V17 — Toda atividade agrícola (Diário de Campo) deve estar vinculada a uma Safra
-- Regra de negócio: não se registra atividade sem safra. Reforço no nível do banco.
-- Idempotente: pode ser reaplicada sem efeitos colaterais.

BEGIN;

-- 1) Remove atividades órfãs (sem safra vinculada). As tabelas filhas
--    (diario_funcionario / diario_maquina / diario_insumo) são apagadas em cascata
--    pelas FKs ON DELETE CASCADE definidas na V14.
DELETE FROM diario_campo WHERE id_safra IS NULL;

-- 2) Torna o vínculo com a safra OBRIGATÓRIO no nível do banco.
--    (SET NOT NULL é no-op se a coluna já for NOT NULL.)
ALTER TABLE diario_campo ALTER COLUMN id_safra SET NOT NULL;

-- 3) A FK deixa de "anular" (ON DELETE SET NULL) — incompatível com NOT NULL.
--    Passa a RESTRICT: excluir uma safra que ainda tem atividades é BLOQUEADO,
--    com erro claro, em vez de violar o NOT NULL.
ALTER TABLE diario_campo DROP CONSTRAINT IF EXISTS diario_campo_id_safra_fkey;
ALTER TABLE diario_campo ADD CONSTRAINT diario_campo_id_safra_fkey
    FOREIGN KEY (id_safra) REFERENCES safra(idsafra) ON DELETE RESTRICT;

COMMIT;
