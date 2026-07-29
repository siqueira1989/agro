-- =====================================================================
-- V15 — Cadastro de Maquinário (Tratores, Implementos e Veículos)
--
-- Boas práticas de custo agrícola:
--   * TRATOR/IMPLEMENTO → custo por HORA de operação, composto por
--       depreciação + combustível + manutenção/reparos (por hora).
--       (A mão de obra do operador é contabilizada à parte, via funcionário.)
--   * VEÍCULO → custo por KM rodado (combustível + manutenção + depreciação por km).
--
-- O custo_hora efetivo é a soma dos componentes quando informados; senão usa
-- o valor digitado diretamente. Ajusta diario_maquina para referenciar o
-- maquinário cadastrado e registrar KM (veículos) além de horas.
-- Idempotente.
-- =====================================================================

BEGIN;

CREATE TABLE IF NOT EXISTS maquina (
    idmaquina        SERIAL PRIMARY KEY,
    nome             VARCHAR(80) NOT NULL,
    tipo             VARCHAR(12) NOT NULL,          -- TRATOR | IMPLEMENTO | VEICULO
    marca            VARCHAR(60),
    identificacao    VARCHAR(40),                   -- placa / patrimônio
    custo_hora       NUMERIC(12,2) NOT NULL DEFAULT 0,  -- trator/implemento (efetivo)
    custo_km         NUMERIC(12,2) NOT NULL DEFAULT 0,  -- veículo
    combustivel_hora NUMERIC(12,2) NOT NULL DEFAULT 0,  -- componentes (boas práticas)
    manutencao_hora  NUMERIC(12,2) NOT NULL DEFAULT 0,
    depreciacao_hora NUMERIC(12,2) NOT NULL DEFAULT 0,
    situacao         BOOLEAN NOT NULL DEFAULT true,
    criado_em        TIMESTAMP NOT NULL DEFAULT now()
);

-- diario_maquina passa a referenciar o cadastro e registrar KM (veículos)
ALTER TABLE diario_maquina ADD COLUMN IF NOT EXISTS km NUMERIC(10,2) NOT NULL DEFAULT 0;

COMMIT;
