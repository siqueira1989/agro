-- =====================================================================
-- V11 — Estoque de Insumos Agrícolas
--
-- Controle de insumos usados direto na produção (defensivos e adubos):
--   * catálogo (insumo) com saldo corrente e preço médio ponderado;
--   * movimentações (estoque_movimento) para histórico/auditoria, cada
--     linha gravando o saldo resultante (saldo_apos).
--
-- Regras de negócio ficam no app (DAO em transação):
--   entrada → recalcula preço médio ponderado e soma ao saldo;
--   saída   → bloqueia se saldo insuficiente (não deixa estoque negativo).
--
-- 'insumo' não faz parte da hierarquia de 'pessoa', então usamos FK real
-- entre estoque_movimento e insumo. Já id_parceiro (fornecedor tipo INSUMO)
-- aponta para parceiro (herança) e é validado no app, sem FK.
-- Idempotente.
-- =====================================================================

BEGIN;

-- Catálogo de insumos ------------------------------------------------
CREATE TABLE IF NOT EXISTS insumo (
    idinsumo              SERIAL PRIMARY KEY,
    nome                  VARCHAR(80)  NOT NULL,
    categoria             VARCHAR(20)  NOT NULL,             -- DEFENSIVO | ADUBO
    grandeza              VARCHAR(15)  NOT NULL,             -- MASSA | CAPACIDADE | UNIDADE
    unidade               VARCHAR(5)   NOT NULL,             -- G | KG | T | ML | L | UN
    quantidade_disponivel NUMERIC(14,3) NOT NULL DEFAULT 0,
    preco_medio           NUMERIC(12,2) NOT NULL DEFAULT 0,
    estoque_minimo        NUMERIC(14,3) NOT NULL DEFAULT 0,
    id_fornecedor         INTEGER,                           -- parceiro tipo INSUMO (opcional/padrão)
    situacao              BOOLEAN NOT NULL DEFAULT true,
    criado_em             TIMESTAMP NOT NULL DEFAULT now()
);

-- Movimentações de estoque -------------------------------------------
CREATE TABLE IF NOT EXISTS estoque_movimento (
    idmov          SERIAL PRIMARY KEY,
    idinsumo       INTEGER NOT NULL REFERENCES insumo(idinsumo) ON DELETE CASCADE,
    tipo           VARCHAR(10) NOT NULL,                     -- ENTRADA | SAIDA
    quantidade     NUMERIC(14,3) NOT NULL,
    preco_unitario NUMERIC(12,2),                            -- preenchido nas entradas
    id_parceiro    INTEGER,                                  -- fornecedor tipo INSUMO (entrada)
    data_mov       DATE NOT NULL DEFAULT CURRENT_DATE,
    observacao     VARCHAR(200),
    saldo_apos     NUMERIC(14,3) NOT NULL,
    criado_em      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_estoque_mov_insumo ON estoque_movimento (idinsumo, data_mov);

COMMIT;
