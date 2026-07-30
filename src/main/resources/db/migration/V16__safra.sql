-- =====================================================================
-- V16 — Safra (ciclo de produção) + rateio de custos + trava por período
--
-- Uma Safra agrupa talhões (quadras) num período, permitindo consolidar os
-- custos das atividades (Diário de Campo) do ciclo (COE — Custo Operacional
-- Efetivo, SEBRAE/CONAB), custo por planta/hectare, % por componente e custo
-- estimado por saca/tonelada. Estrutura preparada para futuro módulo de
-- Vendas/Receitas (que referenciará safra para apurar margem/lucro).
--
-- Relacionamento N:N Safra x Talhão via safra_talhao (com área destinada).
-- Idempotente.
-- =====================================================================

BEGIN;

-- Área real do talhão (ha) — necessária para validar a área destinada e para
-- métricas por hectare. (O talhão já tinha numero_plantas.)
ALTER TABLE quadra ADD COLUMN IF NOT EXISTS area_ha NUMERIC(10,2) NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS safra (
    idsafra              SERIAL PRIMARY KEY,
    nome                 VARCHAR(80) NOT NULL,               -- ex.: "Soja 2026/2027"
    id_cultura_principal INTEGER REFERENCES alimento(idproduto),
    data_inicial         DATE NOT NULL,
    data_final           DATE NOT NULL,
    status               VARCHAR(15) NOT NULL DEFAULT 'PLANEJADA', -- PLANEJADA|EM_ANDAMENTO|FINALIZADA
    estimativa_producao  NUMERIC(14,3) NOT NULL DEFAULT 0,   -- sacas/toneladas previstas
    unidade_producao     VARCHAR(10) NOT NULL DEFAULT 'SACA', -- SACA | TONELADA
    despesas_fixas       NUMERIC(12,2) NOT NULL DEFAULT 0,   -- custos fixos do ciclo (rateados)
    criado_em            TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_safra_periodo ON safra (data_inicial, data_final);

-- N:N Safra x Talhão
CREATE TABLE IF NOT EXISTS safra_talhao (
    id                SERIAL PRIMARY KEY,
    id_safra          INTEGER NOT NULL REFERENCES safra(idsafra) ON DELETE CASCADE,
    id_quadra         INTEGER NOT NULL REFERENCES quadra(idquadra) ON DELETE CASCADE,
    area_destinada_ha NUMERIC(10,2) NOT NULL DEFAULT 0,
    UNIQUE (id_safra, id_quadra)
);
CREATE INDEX IF NOT EXISTS idx_safratalhao_safra ON safra_talhao (id_safra);
CREATE INDEX IF NOT EXISTS idx_safratalhao_quadra ON safra_talhao (id_quadra);

-- Vínculo da atividade (Diário) com a safra (resolvido por talhão + data)
ALTER TABLE diario_campo ADD COLUMN IF NOT EXISTS id_safra INTEGER REFERENCES safra(idsafra) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS idx_diario_safra ON diario_campo (id_safra);

COMMIT;
