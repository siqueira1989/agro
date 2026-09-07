-- Classificação Fixo/Variável no catálogo de Despesas/Custos, e lançamento de
-- despesas por execução do Diário de Campo (ex.: marmita comprada para os
-- funcionários no dia, cada unidade com seu próprio preço — soma, não média).

ALTER TABLE despesascusto
    ADD COLUMN classificacao VARCHAR(10) NOT NULL DEFAULT 'FIXO';
ALTER TABLE despesascusto
    ADD CONSTRAINT ck_despesascusto_classificacao CHECK (classificacao IN ('FIXO', 'VARIAVEL'));

CREATE TABLE diario_despesa (
    id SERIAL PRIMARY KEY,
    iddiario INTEGER NOT NULL REFERENCES diario_campo(iddiario) ON DELETE CASCADE,
    iddespesascusto INTEGER NOT NULL REFERENCES despesascusto(iddespesascusto) ON DELETE RESTRICT,
    descricao VARCHAR(50) NOT NULL,
    quantidade NUMERIC(10,2) NOT NULL DEFAULT 1 CHECK (quantidade > 0),
    valor_unitario NUMERIC(12,2) NOT NULL CHECK (valor_unitario >= 0),
    valor NUMERIC(12,2) NOT NULL CHECK (valor >= 0),
    data_execucao DATE NOT NULL
);
CREATE INDEX idx_diariodesp_diario ON diario_despesa(iddiario);
CREATE INDEX idx_diariodesp_execucao ON diario_despesa(iddiario, data_execucao);

ALTER TABLE diario_campo ADD COLUMN custo_despesas NUMERIC(12,2) NOT NULL DEFAULT 0;

ALTER TABLE diario_campo DROP CONSTRAINT IF EXISTS ck_diario_custos_nao_negativos;
ALTER TABLE diario_campo ADD CONSTRAINT ck_diario_custos_nao_negativos
    CHECK (custo_mao_obra >= 0 AND custo_insumos >= 0 AND custo_maquinas >= 0
           AND custo_despesas >= 0 AND custo_total >= 0) NOT VALID;
