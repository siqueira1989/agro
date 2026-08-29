-- =====================================================================
-- V18 — Safra passa a ser cadastrada por Área de Produção (não mais por
-- talhão avulso). Adiciona id_area_producao à safra; o rateio de custo
-- continua por quadra (safra_talhao), só a seleção na tela muda: ao
-- escolher a área, todas as quadras dela entram automaticamente.
--
-- Backfill: para safras já existentes, resolve a área a partir da primeira
-- quadra vinculada em safra_talhao.
-- Idempotente.
-- =====================================================================

BEGIN;

ALTER TABLE safra ADD COLUMN IF NOT EXISTS id_area_producao INTEGER REFERENCES areaproducao(idareaproducao);

UPDATE safra s
SET id_area_producao = sub.idareaproducao
FROM (
    SELECT DISTINCT ON (st.id_safra) st.id_safra, q.idareaproducao
    FROM safra_talhao st JOIN quadra q ON q.idquadra = st.id_quadra
    ORDER BY st.id_safra, st.id
) sub
WHERE sub.id_safra = s.idsafra AND s.id_area_producao IS NULL;

CREATE INDEX IF NOT EXISTS idx_safra_area ON safra (id_area_producao);

COMMIT;
