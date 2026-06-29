-- =====================================================================
-- V3 — Módulo CLT Rural (Lei 5.889/73 + Portaria 671 MTE)
--
-- Acrescenta ao módulo CLT o que a legislação do trabalho rural exige:
--   * tipo de atividade (lavoura/pecuária) → janela do adicional noturno
--   * contadores de minutos noturnos reais no ponto
--   * auditoria da ação do gestor que registra o ponto (Portaria 671)
--   * colunas financeiras do fechamento: DSR, adicional noturno, extra 100%
-- Idempotente (IF NOT EXISTS / DO $$).
-- =====================================================================

BEGIN;

-- 1) ENUM do tipo de atividade rural ------------------------------------
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'tipo_atividade_rural') THEN
        CREATE TYPE tipo_atividade_rural AS ENUM ('LAVOURA', 'PECUARIA');
    END IF;
END$$;

-- 2) funcionarioclt: tipo de atividade ----------------------------------
ALTER TABLE funcionarioclt
    ADD COLUMN IF NOT EXISTS tipo_atividade_rural tipo_atividade_rural NOT NULL DEFAULT 'LAVOURA';

-- 3) ponto_eletronico: minutos noturnos + auditoria ---------------------
ALTER TABLE ponto_eletronico
    ADD COLUMN IF NOT EXISTS minutos_noturnos    INT            NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS latitude            NUMERIC(10,7),
    ADD COLUMN IF NOT EXISTS longitude           NUMERIC(10,7),
    ADD COLUMN IF NOT EXISTS ip_origem           VARCHAR(45),
    ADD COLUMN IF NOT EXISTS user_agent          VARCHAR(255),
    ADD COLUMN IF NOT EXISTS hash_autenticidade  VARCHAR(64),
    ADD COLUMN IF NOT EXISTS registrado_por      INT,
    ADD COLUMN IF NOT EXISTS atualizado_em       TIMESTAMP;

-- 4) fechamento_folha_ponto: colunas financeiras rurais -----------------
ALTER TABLE fechamento_folha_ponto
    ADD COLUMN IF NOT EXISTS desconto_dsr            NUMERIC(10,2) NOT NULL DEFAULT 0.00,
    ADD COLUMN IF NOT EXISTS valor_adicional_noturno NUMERIC(10,2) NOT NULL DEFAULT 0.00,
    ADD COLUMN IF NOT EXISTS valor_extra_100         NUMERIC(10,2) NOT NULL DEFAULT 0.00,
    ADD COLUMN IF NOT EXISTS minutos_noturnos        INT           NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS minutos_extra_100       INT           NOT NULL DEFAULT 0;

COMMIT;
