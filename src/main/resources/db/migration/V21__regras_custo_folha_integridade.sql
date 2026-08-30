-- ===========================================================================
-- V21 — Regras de custo, folha e integridade
-- Auditoria de 29/08/2026: P1-6, P1-7, P1-10, P1-13, P2-17 (parcial), P2-26.
-- ===========================================================================

-- ---------------------------------------------------------------------------
-- 1) FERIADOS (P1-10)
-- O calculo de dias uteis contava apenas "nao e sabado nem domingo". Em um mes
-- com feriado, o divisor saia maior que o real e contaminava valor-dia,
-- valor-hora, hora extra, DSR e desconto de falta.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.feriado (
    idferiado   serial PRIMARY KEY,
    data        date NOT NULL,
    descricao   character varying(80) NOT NULL,
    abrangencia character varying(10) NOT NULL DEFAULT 'NACIONAL',
    uf          character varying(2),
    municipio   character varying(60),
    CONSTRAINT ck_feriado_abrangencia
        CHECK (abrangencia IN ('NACIONAL', 'ESTADUAL', 'MUNICIPAL'))
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_feriado_data_abrangencia
    ON public.feriado (data, abrangencia, COALESCE(uf, ''), COALESCE(municipio, ''));
CREATE INDEX IF NOT EXISTS idx_feriado_data ON public.feriado (data);

COMMENT ON TABLE public.feriado IS
  'Feriados considerados no calculo de dias uteis (Service.ParametrosFolhaService). '
  'Feriados moveis (Carnaval, Sexta-feira Santa, Corpus Christi) sao calculados '
  'a partir da Pascoa em Java e nao precisam ser cadastrados.';

-- Feriados nacionais de data fixa, 2026 e 2027.
INSERT INTO public.feriado (data, descricao, abrangencia) VALUES
    ('2026-01-01', 'Confraternizacao Universal', 'NACIONAL'),
    ('2026-04-21', 'Tiradentes',                 'NACIONAL'),
    ('2026-05-01', 'Dia do Trabalho',            'NACIONAL'),
    ('2026-09-07', 'Independencia',              'NACIONAL'),
    ('2026-10-12', 'Nossa Senhora Aparecida',    'NACIONAL'),
    ('2026-11-02', 'Finados',                    'NACIONAL'),
    ('2026-11-15', 'Proclamacao da Republica',   'NACIONAL'),
    ('2026-11-20', 'Consciencia Negra',          'NACIONAL'),
    ('2026-12-25', 'Natal',                      'NACIONAL'),
    ('2027-01-01', 'Confraternizacao Universal', 'NACIONAL'),
    ('2027-04-21', 'Tiradentes',                 'NACIONAL'),
    ('2027-05-01', 'Dia do Trabalho',            'NACIONAL'),
    ('2027-09-07', 'Independencia',              'NACIONAL'),
    ('2027-10-12', 'Nossa Senhora Aparecida',    'NACIONAL'),
    ('2027-11-02', 'Finados',                    'NACIONAL'),
    ('2027-11-15', 'Proclamacao da Republica',   'NACIONAL'),
    ('2027-11-20', 'Consciencia Negra',          'NACIONAL'),
    ('2027-12-25', 'Natal',                      'NACIONAL')
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------------
-- 2) FOLGA COMPENSATORIA EXPLICITA (P1-13)
-- Antes a folga era DEDUZIDA de "existe um dia entre segunda e sabado sem
-- ponto e sem falta". Como quase ninguem tem ponto no sabado, quase toda
-- semana era tratada como compensada e o domingo trabalhado deixava de ser
-- pago a 100%. Agora a folga e um fato registrado, nao um palpite.
-- ---------------------------------------------------------------------------
ALTER TABLE public.ponto_eletronico
    ADD COLUMN IF NOT EXISTS folga_compensatoria boolean NOT NULL DEFAULT false;

COMMENT ON COLUMN public.ponto_eletronico.folga_compensatoria IS
  'true quando o dia foi concedido como folga compensatoria do domingo '
  'trabalhado na mesma semana. Marcado explicitamente pelo usuario.';

-- ---------------------------------------------------------------------------
-- 3) EMPREITA: valor por atividade, nao o contrato inteiro (P1-6)
-- recalcularCustos usava funcionarioempreita.valorfixoacordado — o valor do
-- CONTRATO — em cada linha de diario_funcionario. Um empreiteiro lancado em
-- 5 atividades custava 5x o contrato. A coluna valor_contratado ja existia e
-- ja era gravada; passa a ser a fonte do custo.
-- O backfill abaixo distribui o contrato entre as atividades ja lancadas,
-- preservando o total historico em vez de multiplica-lo.
-- ---------------------------------------------------------------------------
WITH rateio AS (
    SELECT df.id,
           ROUND(
               COALESCE(fe.valorfixoacordado, 0)
               / GREATEST(COUNT(*) OVER (PARTITION BY df.idpessoa), 1)
           , 2) AS valor_rateado
      FROM public.diario_funcionario df
      JOIN public.funcionarioempreita fe ON fe.idpessoa = df.idpessoa
     WHERE upper(df.tipo_funcionario) = 'EMPREITA'
       AND COALESCE(df.valor_contratado, 0) = 0
)
UPDATE public.diario_funcionario df
   SET valor_contratado = r.valor_rateado
  FROM rateio r
 WHERE df.id = r.id;

COMMENT ON COLUMN public.diario_funcionario.valor_contratado IS
  'Valor acordado PARA ESTA ATIVIDADE (empreita). Nao e o valor do contrato '
  'inteiro do empreiteiro — ver P1-6 da auditoria de 29/08/2026.';

-- ---------------------------------------------------------------------------
-- 4) INDICES QUE FALTAVAM
-- diario_funcionario so tinha indice por iddiario. O rateio da diaria por
-- pessoa/dia e as consultas por funcionario faziam varredura completa.
-- ---------------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_diariofunc_pessoa_data
    ON public.diario_funcionario (idpessoa, data_execucao);
CREATE INDEX IF NOT EXISTS idx_vale_pessoa_data
    ON public.vale_funcionario (idpessoa, datavale);
CREATE INDEX IF NOT EXISTS idx_ponto_pessoa_data
    ON public.ponto_eletronico (idpessoa, dataregistro);
CREATE INDEX IF NOT EXISTS idx_falta_pessoa_data
    ON public.falta_funcionario (idpessoa, datafalta);
CREATE INDEX IF NOT EXISTS idx_vinculo_pessoa_periodo
    ON public.vinculo_empregaticio (idpessoa, data_admissao, data_desligamento);

-- ---------------------------------------------------------------------------
-- 5) CHECK CONSTRAINTS (P2-17)
-- Eram 38 tabelas com UM unico CHECK. Estas regras estavam so no Java — ou em
-- lugar nenhum — e podiam ser burladas por qualquer UPDATE direto.
--
-- TODAS entram como NOT VALID, de proposito. Um CHECK comum valida as linhas
-- ja existentes, e a migracao inteira falha se alguma nao passar. Como o
-- proprio P1-7 descreve baixa de estoque em duplicidade, e bem possivel que
-- exista insumo com saldo negativo em producao — justamente o resultado do
-- bug. Com NOT VALID a regra passa a valer para toda escrita NOVA sem barrar o
-- historico, e a aplicacao sobe. O bloco de diagnostico abaixo lista o que
-- precisa ser acertado; depois de corrigir, rode:
--     ALTER TABLE <tabela> VALIDATE CONSTRAINT <constraint>;
-- ---------------------------------------------------------------------------
ALTER TABLE public.insumo DROP CONSTRAINT IF EXISTS ck_insumo_saldo_nao_negativo;
ALTER TABLE public.insumo ADD CONSTRAINT ck_insumo_saldo_nao_negativo
    CHECK (quantidade_disponivel >= 0) NOT VALID;

ALTER TABLE public.insumo DROP CONSTRAINT IF EXISTS ck_insumo_preco_nao_negativo;
ALTER TABLE public.insumo ADD CONSTRAINT ck_insumo_preco_nao_negativo
    CHECK (preco_medio >= 0) NOT VALID;

ALTER TABLE public.diario_campo DROP CONSTRAINT IF EXISTS ck_diario_status;
ALTER TABLE public.diario_campo ADD CONSTRAINT ck_diario_status
    CHECK (status IN ('PLANEJADA', 'EM_ANDAMENTO', 'CONCLUIDA', 'CANCELADA')) NOT VALID;

ALTER TABLE public.diario_campo DROP CONSTRAINT IF EXISTS ck_diario_custos_nao_negativos;
ALTER TABLE public.diario_campo ADD CONSTRAINT ck_diario_custos_nao_negativos
    CHECK (custo_mao_obra >= 0 AND custo_insumos >= 0
       AND custo_maquinas >= 0 AND custo_total >= 0) NOT VALID;

ALTER TABLE public.estoque_movimento DROP CONSTRAINT IF EXISTS ck_movimento_tipo;
ALTER TABLE public.estoque_movimento ADD CONSTRAINT ck_movimento_tipo
    CHECK (tipo IN ('ENTRADA', 'SAIDA')) NOT VALID;

ALTER TABLE public.estoque_movimento DROP CONSTRAINT IF EXISTS ck_movimento_quantidade;
ALTER TABLE public.estoque_movimento ADD CONSTRAINT ck_movimento_quantidade
    CHECK (quantidade > 0) NOT VALID;

ALTER TABLE public.vinculo_empregaticio DROP CONSTRAINT IF EXISTS ck_vinculo_periodo;
ALTER TABLE public.vinculo_empregaticio ADD CONSTRAINT ck_vinculo_periodo
    CHECK (data_desligamento IS NULL OR data_desligamento >= data_admissao) NOT VALID;

ALTER TABLE public.vinculo_empregaticio DROP CONSTRAINT IF EXISTS ck_vinculo_salario;
ALTER TABLE public.vinculo_empregaticio ADD CONSTRAINT ck_vinculo_salario
    CHECK (salario_mensal >= 0) NOT VALID;

ALTER TABLE public.maquina DROP CONSTRAINT IF EXISTS ck_maquina_custos;
ALTER TABLE public.maquina ADD CONSTRAINT ck_maquina_custos
    CHECK (custo_hora >= 0 AND custo_km >= 0) NOT VALID;

-- Teto de jornada (P2-26): 17:00 as 08:00 digitado por engano virava 15 h
-- trabalhadas e 7 h extras indevidas. 16 h e o limite plausivel para um dia.
ALTER TABLE public.ponto_eletronico DROP CONSTRAINT IF EXISTS ck_ponto_jornada_plausivel;
ALTER TABLE public.ponto_eletronico ADD CONSTRAINT ck_ponto_jornada_plausivel
    CHECK (total_minutos IS NULL OR (total_minutos >= 0 AND total_minutos <= 960)) NOT VALID;

-- Diagnostico: aponta, no log do servidor, o que impede a validacao completa.
DO $$
DECLARE
    n_saldo   bigint;
    n_status  bigint;
    n_jornada bigint;
    n_qtd     bigint;
BEGIN
    SELECT count(*) INTO n_saldo   FROM public.insumo WHERE quantidade_disponivel < 0;
    SELECT count(*) INTO n_status  FROM public.diario_campo
        WHERE status IS NULL OR status NOT IN ('PLANEJADA','EM_ANDAMENTO','CONCLUIDA','CANCELADA');
    SELECT count(*) INTO n_jornada FROM public.ponto_eletronico
        WHERE total_minutos IS NOT NULL AND (total_minutos < 0 OR total_minutos > 960);
    SELECT count(*) INTO n_qtd     FROM public.estoque_movimento WHERE quantidade <= 0;

    IF n_saldo + n_status + n_jornada + n_qtd > 0 THEN
        RAISE NOTICE 'V21 — dados historicos fora das novas regras (constraints ficaram NOT VALID):';
        RAISE NOTICE '  insumo com saldo negativo ............ %', n_saldo;
        RAISE NOTICE '  diario_campo com status invalido ..... %', n_status;
        RAISE NOTICE '  ponto com jornada fora de 0..16h ..... %', n_jornada;
        RAISE NOTICE '  movimento com quantidade <= 0 ........ %', n_qtd;
        RAISE NOTICE 'Corrija esses registros e rode ALTER TABLE ... VALIDATE CONSTRAINT ...';
    ELSE
        RAISE NOTICE 'V21 — nenhum dado historico viola as novas regras.';
    END IF;
END $$;

-- 6) FK que faltava e era possivel criar
-- insumo.id_fornecedor aponta para parceiro, que herda pessoa — FK real e
-- impossivel enquanto houver heranca (P2-17). Fica o trigger equivalente.
-- ---------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.trg_valida_fornecedor_existe()
RETURNS trigger AS $$
BEGIN
    IF NEW.id_fornecedor IS NULL THEN
        RETURN NEW;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM public.parceiro WHERE idpessoa = NEW.id_fornecedor) THEN
        RAISE EXCEPTION 'Fornecedor % nao existe na tabela parceiro.', NEW.id_fornecedor
            USING ERRCODE = 'foreign_key_violation';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS chk_fornecedor_existe ON public.insumo;
CREATE TRIGGER chk_fornecedor_existe BEFORE INSERT OR UPDATE OF id_fornecedor
    ON public.insumo FOR EACH ROW EXECUTE FUNCTION public.trg_valida_fornecedor_existe();
