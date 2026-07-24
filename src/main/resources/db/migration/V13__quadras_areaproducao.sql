-- =====================================================================
-- V13 — Quadras da Área de Produção
--
-- Uma propriedade (areaproducao) passa a ter várias QUADRAS de produção,
-- cada uma com nome, número de plantas e tipo de planta (alimento).
-- A quantidade total de plantas da área é a soma das quadras ativas.
--
-- Também adiciona 'situacao' na areaproducao para desativação (soft delete),
-- substituindo a exclusão física.
-- Idempotente.
-- =====================================================================

BEGIN;

-- Soft delete da área
ALTER TABLE areaproducao ADD COLUMN IF NOT EXISTS situacao BOOLEAN NOT NULL DEFAULT true;

-- Quadras de produção
CREATE TABLE IF NOT EXISTS quadra (
    idquadra        SERIAL PRIMARY KEY,
    idareaproducao  INTEGER NOT NULL REFERENCES areaproducao(idareaproducao) ON DELETE CASCADE,
    nome_quadra     VARCHAR(60) NOT NULL,
    numero_plantas  INTEGER NOT NULL DEFAULT 0,
    id_alimento     INTEGER REFERENCES alimento(idproduto),  -- alimento tem UNIQUE em idproduto
    ativa           BOOLEAN NOT NULL DEFAULT true,
    criado_em       TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_quadra_area ON quadra (idareaproducao);

COMMIT;
