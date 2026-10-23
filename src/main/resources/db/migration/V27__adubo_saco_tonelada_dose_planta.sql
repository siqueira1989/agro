-- Diário de Campo — Adubo lançado pela FORMA em que foi aplicado: nº de sacos de um tipo padrão
-- (5/10/25/50 kg), toneladas ou quilos, mais o nº de plantas que receberam. O servidor converte
-- para a unidade do insumo (quantidade_direta = quantidade debitada do estoque) e grava a dose por
-- planta em kg. Ex.: 10 sacos de 50 kg em 50 plantas → 500 kg, 10 kg/planta; 5 t em 500 plantas →
-- 5000 kg, 10 kg/planta. Linhas antigas (só quantidade_direta) continuam válidas: forma_aplicacao NULL.
ALTER TABLE diario_insumo ADD COLUMN forma_aplicacao VARCHAR(10);
ALTER TABLE diario_insumo ADD COLUMN peso_saco_kg NUMERIC(8,3);
ALTER TABLE diario_insumo ADD COLUMN qtd_informada NUMERIC(14,3);
ALTER TABLE diario_insumo ADD COLUMN plantas_aplicadas INTEGER;
ALTER TABLE diario_insumo ADD COLUMN quantidade_por_planta_kg NUMERIC(14,4);

ALTER TABLE diario_insumo ADD CONSTRAINT ck_diarioins_forma
    CHECK (forma_aplicacao IS NULL OR forma_aplicacao IN ('SACO','TONELADA','KG'));
ALTER TABLE diario_insumo ADD CONSTRAINT ck_diarioins_peso_saco
    CHECK (peso_saco_kg IS NULL OR peso_saco_kg IN (5, 10, 25, 50));
ALTER TABLE diario_insumo ADD CONSTRAINT ck_diarioins_saco_completo
    CHECK (forma_aplicacao IS DISTINCT FROM 'SACO' OR peso_saco_kg IS NOT NULL);
ALTER TABLE diario_insumo ADD CONSTRAINT ck_diarioins_qtd_informada
    CHECK (qtd_informada IS NULL OR qtd_informada > 0);
ALTER TABLE diario_insumo ADD CONSTRAINT ck_diarioins_plantas
    CHECK (plantas_aplicadas IS NULL OR plantas_aplicadas > 0);

COMMENT ON COLUMN diario_insumo.forma_aplicacao IS 'ADUBO: SACO | TONELADA | KG — como a quantidade foi informada. NULL = lançamento antigo (só quantidade_direta).';
COMMENT ON COLUMN diario_insumo.peso_saco_kg IS 'Peso de 1 saco (5/10/25/50 kg) quando forma_aplicacao = SACO.';
COMMENT ON COLUMN diario_insumo.qtd_informada IS 'Valor digitado: nº de sacos, toneladas ou kg, conforme forma_aplicacao.';
COMMENT ON COLUMN diario_insumo.plantas_aplicadas IS 'Nº de plantas do talhão que receberam o adubo (padrão: plantas do talhão).';
COMMENT ON COLUMN diario_insumo.quantidade_por_planta_kg IS 'Dose por planta em kg = total em kg / plantas_aplicadas (calculado no servidor).';
