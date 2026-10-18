-- Diário de Campo — Adubo (e qualquer insumo aplicado SEM o conceito de calda) ganha um segundo
-- jeito de lançar quantidade: direto na unidade do insumo (kg/tonelada), tipicamente calculado no
-- cliente a partir do nº de sacos × peso do saco (insumo.peso_embalagem, já existente desde a
-- V25). Defensivo continua usando dose_por_unidade × dose_total_aplicada (aplicação por calda).
-- As duas formas são mutuamente exclusivas por linha — nunca as duas ao mesmo tempo.
ALTER TABLE diario_insumo ADD COLUMN quantidade_direta NUMERIC(14,3);
ALTER TABLE diario_insumo ADD CONSTRAINT ck_diarioins_qtddireta_positiva
    CHECK (quantidade_direta IS NULL OR quantidade_direta > 0);

COMMENT ON COLUMN diario_insumo.quantidade_direta IS
  'Quantidade aplicada diretamente (ADUBO), já na unidade do insumo (kg/tonelada) — alternativa a '
  'dose_por_unidade*dose_total_aplicada (DEFENSIVO, aplicação por calda). O número de sacos usado '
  'para chegar nesse valor (quando o insumo tem peso_embalagem cadastrado) é só um atalho de '
  'cálculo no cliente; não é gravado separadamente.';
