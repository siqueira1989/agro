-- Diário de Campo — nova regra de negócio para Insumos (defensivo/adubo aplicado em campo):
-- o valor lançado deixa de ser uma quantidade digitada direto (sem cálculo, sem relação real com
-- o que foi aplicado) e passa a ser DOSE POR UNIDADE (litro ou kg de calda) × DOSE TOTAL APLICADA
-- (quantos litros/kg de calda foram de fato aplicados) = QUANTIDADE TOTAL APLICADA — é essa
-- quantidade calculada que é debitada do estoque. O antigo `dose_aplicada` (texto livre, decorativo,
-- sem entrar em nenhuma conta) sai de cena.
ALTER TABLE diario_insumo ADD COLUMN dose_por_unidade NUMERIC(14,4);
ALTER TABLE diario_insumo ADD COLUMN dose_total_aplicada NUMERIC(14,3);
ALTER TABLE diario_insumo DROP COLUMN IF EXISTS dose_aplicada;

ALTER TABLE diario_insumo ADD CONSTRAINT ck_diarioins_dose_positiva
    CHECK (dose_por_unidade IS NULL OR dose_por_unidade > 0);
ALTER TABLE diario_insumo ADD CONSTRAINT ck_diarioins_dosetotal_positiva
    CHECK (dose_total_aplicada IS NULL OR dose_total_aplicada > 0);

COMMENT ON COLUMN diario_insumo.dose_por_unidade IS
  'Quantidade do produto por litro ou kg de calda (na unidade cadastrada do insumo: g/mL/kg/L).';
COMMENT ON COLUMN diario_insumo.dose_total_aplicada IS
  'Total de calda (litros ou kg) efetivamente aplicada na atividade.';
COMMENT ON COLUMN diario_insumo.quantidade IS
  'Quantidade TOTAL do produto consumido = dose_por_unidade * dose_total_aplicada (calculado no '
  'servidor, nunca aceito direto do cliente) — é o valor debitado do estoque ao finalizar.';

-- Embalagem em saco: adubo e defensivo muitas vezes vêm fracionados em sacos de peso fixo (25kg,
-- 50kg...) mesmo quando o saldo do estoque é controlado em kg/tonelada. Campo opcional no cadastro
-- do insumo; quando preenchido, a tela de Entrada de estoque oferece lançar "nº de sacos" em vez da
-- quantidade direta, convertendo automaticamente (nº sacos * peso_embalagem).
ALTER TABLE insumo ADD COLUMN peso_embalagem NUMERIC(14,3);
ALTER TABLE insumo ADD CONSTRAINT ck_insumo_peso_embalagem CHECK (peso_embalagem IS NULL OR peso_embalagem > 0);

COMMENT ON COLUMN insumo.peso_embalagem IS
  'Peso/volume de 1 saco ou embalagem padrão deste insumo, na mesma unidade do insumo (ex.: 50 = '
  'saco de 50kg). Opcional — usado só como atalho de conversão na Entrada de estoque.';
