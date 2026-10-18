-- Colheita por regime (sistema x produção por caixa) + Módulo de Colheita e Estoque.
--
-- 'regime_trabalho' é independente do tipo de contrato (tipo_funcionario): CLT, DIARISTA
-- ou EMPREITA podem, num diário de Colheita específico, ser pagos por caixa colhida em vez
-- do cálculo padrão do contrato (diária/hora/valor-contratado) — a linha do diario_funcionario
-- carrega essa exceção pontual, sem alterar o cadastro da pessoa.
ALTER TABLE diario_funcionario
    ADD COLUMN regime_trabalho   VARCHAR(20),
    ADD COLUMN quantidade_caixas NUMERIC(10,2),
    ADD COLUMN preco_por_caixa   NUMERIC(12,2),
    ADD COLUMN valor_regime      NUMERIC(12,2);

ALTER TABLE diario_funcionario
    ADD CONSTRAINT ck_diariofunc_regime
        CHECK (regime_trabalho IS NULL OR regime_trabalho IN ('REGIME_SISTEMA', 'PRODUCAO'));

ALTER TABLE diario_funcionario
    ADD CONSTRAINT ck_diariofunc_regime_caixas
        CHECK (quantidade_caixas IS NULL OR quantidade_caixas >= 0);
ALTER TABLE diario_funcionario
    ADD CONSTRAINT ck_diariofunc_regime_preco
        CHECK (preco_por_caixa IS NULL OR preco_por_caixa >= 0);
ALTER TABLE diario_funcionario
    ADD CONSTRAINT ck_diariofunc_regime_valor
        CHECK (valor_regime IS NULL OR valor_regime >= 0);

COMMENT ON COLUMN diario_funcionario.regime_trabalho IS
  'NULL para atividades fora da Colheita. REGIME_SISTEMA = cálculo padrão do contrato da '
  'pessoa; PRODUCAO = pago só por quantidade_caixas*preco_por_caixa nesta atividade, '
  'anulando diária/hora/valor-contratado do dia para esta pessoa.';

-- Módulo de Colheita e Estoque: cada diário de Colheita finalizado com lançamentos em
-- regime PRODUCAO gera (ou atualiza) um lote. O volume não processado fica retido em
-- ESTOQUE até virar PROCESSADO ou ser baixado como FINALIZADO.
CREATE TABLE colheita_lote (
    idlote               SERIAL PRIMARY KEY,
    iddiario             INTEGER NOT NULL REFERENCES diario_campo(iddiario) ON DELETE CASCADE,
    id_quadra            INTEGER REFERENCES quadra(idquadra),
    id_cultura           INTEGER REFERENCES alimento(idproduto),
    data_colheita        DATE NOT NULL,
    quantidade_total     NUMERIC(12,2) NOT NULL DEFAULT 0,
    quantidade_processada NUMERIC(12,2) NOT NULL DEFAULT 0,
    unidade              VARCHAR(10) NOT NULL DEFAULT 'CX',
    status               VARCHAR(15) NOT NULL DEFAULT 'COLHEIDO',
    observacoes          VARCHAR(200),
    criado_em            TIMESTAMP NOT NULL DEFAULT now(),
    atualizado_em        TIMESTAMP
);

ALTER TABLE colheita_lote
    ADD CONSTRAINT ck_colheita_status
        CHECK (status IN ('COLHEIDO', 'ESTOQUE', 'PROCESSADO', 'FINALIZADO'));
ALTER TABLE colheita_lote
    ADD CONSTRAINT ck_colheita_quantidades
        CHECK (quantidade_total >= 0 AND quantidade_processada >= 0
               AND quantidade_processada <= quantidade_total);

-- Um diário de Colheita consolida um único lote (a soma de todos os funcionários em
-- regime PRODUCAO daquele diário); reabrir/editar recalcula o mesmo lote (upsert).
CREATE UNIQUE INDEX uk_colheita_lote_diario ON colheita_lote (iddiario);
CREATE INDEX idx_colheita_status ON colheita_lote (status);
CREATE INDEX idx_colheita_quadra ON colheita_lote (id_quadra);

COMMENT ON TABLE colheita_lote IS
  'Lote de produção colhida por regime PRODUCAO num diário de Colheita finalizado. '
  'Saldo em estoque = SUM(quantidade_total - quantidade_processada) WHERE status=''ESTOQUE''.';
