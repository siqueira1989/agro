-- Despesas/Custos — despesas de VENDA usadas pelo Romaneio (venda da produção ao parceiro comprador).
--
-- forma_calculo: como o Romaneio calcula o valor da despesa
--   FIXO             → valordespesascustos é o valor em R$ (padrão; comportamento de sempre)
--   PERCENTUAL_VENDA → valordespesascustos guarda o PERCENTUAL sobre a venda bruta (18 = 18%)
--   POR_CAIXA        → valordespesascustos é o R$ por caixa/embalagem vendida
-- aplica_romaneio: a despesa aparece na lista de despesas do Romaneio (GET ?romaneio=1).
--
-- Registros existentes ficam FIXO / false (nada muda para eles).

ALTER TABLE despesascusto
    ADD COLUMN forma_calculo VARCHAR(20) NOT NULL DEFAULT 'FIXO';
ALTER TABLE despesascusto
    ADD CONSTRAINT ck_despesascusto_forma_calculo
    CHECK (forma_calculo IN ('FIXO', 'PERCENTUAL_VENDA', 'POR_CAIXA'));
ALTER TABLE despesascusto
    ADD COLUMN aplica_romaneio BOOLEAN NOT NULL DEFAULT false;

COMMENT ON COLUMN despesascusto.forma_calculo IS 'FIXO (R$) | PERCENTUAL_VENDA (valordespesascustos = % sobre a venda bruta, 18 = 18%) | POR_CAIXA (R$ por caixa).';
COMMENT ON COLUMN despesascusto.aplica_romaneio IS 'true = despesa de venda oferecida no Romaneio.';

-- Dados iniciais (só se ainda não existir despesa com a mesma descrição).
INSERT INTO despesascusto (despesascusto, unidadedespesascustos, valordespesascustos, tipodespesascustos,
                           classificacao, categoria, forma_calculo, aplica_romaneio)
SELECT v.descricao, v.unidade, v.valor, 'Venda', 'VARIAVEL', 'GERAL', v.forma, true
  FROM (VALUES ('Comissão de venda',                   '%',  18.00, 'PERCENTUAL_VENDA'),
               ('Frete por embalagem',                 'cx',  0.00, 'POR_CAIXA'),
               ('Vale (adiantamento motorista/frete)', 'un',  0.00, 'FIXO')
       ) AS v(descricao, unidade, valor, forma)
 WHERE NOT EXISTS (SELECT 1 FROM despesascusto d
                    WHERE lower(btrim(d.despesascusto)) = lower(v.descricao));
