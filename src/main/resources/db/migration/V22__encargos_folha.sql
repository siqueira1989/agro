-- ===========================================================================
-- V22 — Encargos da folha (P1-9 da auditoria de 29/08/2026)
--
-- O calculo era literalmente:  liquido = bruto - vales
-- Sem INSS, sem IRRF, sem FGTS, sem DSR. O valor apresentado como "liquido"
-- nao correspondia nem ao que o trabalhador recebe nem ao que a empresa
-- recolhe.
--
-- !!! ATENCAO — CONFERIR COM A CONTABILIDADE ANTES DE USAR EM PRODUCAO !!!
-- As faixas abaixo refletem a tabela vigente a partir de maio/2025 e estao
-- aqui para que a estrutura nasca funcionando. Elas MUDAM por lei, em geral
-- todo ano. Confira os valores com o escritorio contabil e, se necessario,
-- insira uma nova vigencia (as faixas antigas ficam preservadas para
-- recalcular competencias passadas).
-- ===========================================================================

CREATE TABLE IF NOT EXISTS public.faixa_encargo (
    idfaixa         serial PRIMARY KEY,
    tributo         character varying(10) NOT NULL,   -- INSS | IRRF
    vigencia_inicio date NOT NULL,
    limite_ate      numeric(12,2),                    -- NULL = ultima faixa
    aliquota        numeric(6,4) NOT NULL,            -- 0.0750 = 7,5%
    parcela_deduzir numeric(12,2) NOT NULL DEFAULT 0,
    ordem           integer NOT NULL,
    CONSTRAINT ck_faixa_tributo CHECK (tributo IN ('INSS', 'IRRF')),
    CONSTRAINT ck_faixa_aliquota CHECK (aliquota >= 0 AND aliquota <= 1)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_faixa_encargo
    ON public.faixa_encargo (tributo, vigencia_inicio, ordem);

COMMENT ON TABLE public.faixa_encargo IS
  'Faixas progressivas de INSS e IRRF por vigencia. Lidas por Service.EncargosService. '
  'CONFERIR COM A CONTABILIDADE a cada mudanca de tabela legal.';

-- A chave sozinha NAO pode ser a PK: EncargosService.parametro() consulta
-- "WHERE chave = ? AND vigencia_inicio <= ? ORDER BY vigencia_inicio DESC",
-- ou seja, foi escrita para varias vigencias por chave. Com PK so em "chave",
-- cadastrar a tabela de 2027 violaria a PK e a saida seria um UPDATE — que
-- reescreveria o passado e faria toda competencia antiga ser recalculada com
-- o valor novo, em silencio. Exatamente o oposto do que este arquivo promete.
CREATE TABLE IF NOT EXISTS public.parametro_encargo (
    chave           character varying(40) NOT NULL,
    valor           numeric(12,4) NOT NULL,
    vigencia_inicio date NOT NULL,
    descricao       character varying(120),
    CONSTRAINT pk_parametro_encargo PRIMARY KEY (chave, vigencia_inicio)
);

-- INSS — tabela progressiva vigente desde 01/05/2025 (CONFERIR)
INSERT INTO public.faixa_encargo (tributo, vigencia_inicio, limite_ate, aliquota, parcela_deduzir, ordem) VALUES
    ('INSS', '2025-05-01', 1518.00,  0.0750, 0, 1),
    ('INSS', '2025-05-01', 2793.88,  0.0900, 0, 2),
    ('INSS', '2025-05-01', 4190.83,  0.1200, 0, 3),
    ('INSS', '2025-05-01', 8157.41,  0.1400, 0, 4)
ON CONFLICT DO NOTHING;

-- IRRF — tabela mensal vigente desde 01/05/2025 (CONFERIR)
INSERT INTO public.faixa_encargo (tributo, vigencia_inicio, limite_ate, aliquota, parcela_deduzir, ordem) VALUES
    ('IRRF', '2025-05-01', 2428.80, 0.0000,   0.00, 1),
    ('IRRF', '2025-05-01', 2826.65, 0.0750, 182.16, 2),
    ('IRRF', '2025-05-01', 3751.05, 0.1500, 394.16, 3),
    ('IRRF', '2025-05-01', 4664.68, 0.2250, 675.49, 4),
    ('IRRF', '2025-05-01', NULL,    0.2750, 908.73, 5)
ON CONFLICT DO NOTHING;

INSERT INTO public.parametro_encargo (chave, valor, vigencia_inicio, descricao) VALUES
    ('INSS_TETO',            8157.41, '2025-05-01', 'Teto do salario de contribuicao'),
    ('IRRF_DEDUCAO_DEPEND',   189.59, '2025-05-01', 'Deducao mensal por dependente'),
    ('IRRF_DESCONTO_SIMPL',   607.20, '2025-05-01', 'Desconto simplificado mensal'),
    ('FGTS_ALIQUOTA',          0.0800, '1990-01-01', 'FGTS: 8% sobre a remuneracao (encargo do empregador)')
ON CONFLICT (chave, vigencia_inicio) DO NOTHING;

-- Colunas para guardar os encargos apurados no fechamento
ALTER TABLE public.fechamento_folha_ponto
    ADD COLUMN IF NOT EXISTS desconto_inss numeric(10,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS desconto_irrf numeric(10,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS fgts_deposito numeric(10,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS dependentes   integer      NOT NULL DEFAULT 0;

COMMENT ON COLUMN public.fechamento_folha_ponto.fgts_deposito IS
  'FGTS e encargo do EMPREGADOR: nao desconta do liquido, mas compoe o custo.';
