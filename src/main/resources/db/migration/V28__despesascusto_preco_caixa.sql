-- Despesas/Custos — preço de caixa de colheita (contentor) e de embalagem de processamento,
-- POR alimento e POR área de produção. O Diário de Campo (caixa de colheita) e a Colheita e
-- Estoque (embalagens do embalador) buscam o preço aqui; quando não encontram, registram o valor
-- informado pelo usuário (upsert pela chave única abaixo).
--
-- categoria:  GERAL (todos os registros existentes — nada muda para eles)
--             CAIXA_COLHEITA (tipo_caixa = 'CONTENTOR')
--             EMBALAGEM (tipo_caixa = código livre: PAPELAO_7, PLASTICO_20, MADEIRA_15...)
-- A classificação FIXO/VARIAVEL continua; caixas/embalagens são gravadas como VARIAVEL.

ALTER TABLE despesascusto
    ADD COLUMN categoria VARCHAR(20) NOT NULL DEFAULT 'GERAL';
ALTER TABLE despesascusto
    ADD CONSTRAINT ck_despesascusto_categoria
    CHECK (categoria IN ('GERAL', 'CAIXA_COLHEITA', 'EMBALAGEM'));

ALTER TABLE despesascusto
    ADD COLUMN id_alimento INTEGER NULL REFERENCES alimento(idproduto);
ALTER TABLE despesascusto
    ADD COLUMN id_area_producao INTEGER NULL REFERENCES areaproducao(idareaproducao);
ALTER TABLE despesascusto
    ADD COLUMN tipo_caixa VARCHAR(30) NULL;

ALTER TABLE despesascusto
    ADD CONSTRAINT ck_despesascusto_caixa_completa
    CHECK (categoria = 'GERAL'
           OR (id_alimento IS NOT NULL AND id_area_producao IS NOT NULL
               AND tipo_caixa IS NOT NULL AND btrim(tipo_caixa) <> ''));

CREATE UNIQUE INDEX ux_despesascusto_preco_caixa
    ON despesascusto (categoria, id_alimento, id_area_producao, tipo_caixa)
    WHERE categoria <> 'GERAL';

COMMENT ON COLUMN despesascusto.categoria IS 'GERAL | CAIXA_COLHEITA | EMBALAGEM. ≠ GERAL exige id_alimento, id_area_producao e tipo_caixa.';
COMMENT ON COLUMN despesascusto.id_alimento IS 'Alimento ao qual o preço de caixa/embalagem se aplica (categoria ≠ GERAL).';
COMMENT ON COLUMN despesascusto.id_area_producao IS 'Área de produção à qual o preço de caixa/embalagem se aplica (categoria ≠ GERAL).';
COMMENT ON COLUMN despesascusto.tipo_caixa IS 'Código da caixa/embalagem: CONTENTOR na colheita; texto livre em maiúsculas para embalagens (ex.: PAPELAO_7, PLASTICO_20).';
