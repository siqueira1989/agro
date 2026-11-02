-- Módulo Romaneio — venda da produção processada (lote de colheita) a um parceiro comprador.
--
-- Um lote pode ter vários romaneios (compradores diferentes); por embalagem, a soma das caixas dos
-- romaneios não cancelados do lote não pode passar das caixas embaladas (colheita_processamento_embalagem).
-- Situações: ABERTO (falta preço em algum item) → PENDENTE (todos os itens com preço) →
--            FINALIZADO (valor líquido pago por inteiro). CANCELADO libera as caixas.
-- valor_bruto / total_despesas / valor_liquido / valor_pago ficam sempre recalculados e gravados:
-- a Safra soma esses campos por id_safra e id_quadra (status <> 'CANCELADO').

CREATE TABLE romaneio (
    idromaneio      SERIAL PRIMARY KEY,
    numero          VARCHAR(20)   NOT NULL UNIQUE,
    idlote          INTEGER       NOT NULL REFERENCES colheita_lote(idlote),
    id_area         INTEGER       NOT NULL REFERENCES areaproducao(idareaproducao),
    id_quadra       INTEGER       NOT NULL REFERENCES quadra(idquadra),
    id_alimento     INTEGER       NOT NULL REFERENCES alimento(idproduto),
    id_safra        INTEGER       REFERENCES safra(idsafra),
    -- Sem FK: parceiro herda de pessoa (pessoa → pessoacnpj → parceiro) e FK não enxerga as filhas.
    id_parceiro     INTEGER       NOT NULL,
    data_emissao    DATE          NOT NULL,
    motorista       VARCHAR(80),
    placa           VARCHAR(10),
    status          VARCHAR(12)   NOT NULL DEFAULT 'ABERTO',
    total_caixas    NUMERIC(12,2) NOT NULL DEFAULT 0,
    valor_bruto     NUMERIC(14,2) NOT NULL DEFAULT 0,
    total_despesas  NUMERIC(14,2) NOT NULL DEFAULT 0,
    valor_liquido   NUMERIC(14,2) NOT NULL DEFAULT 0,
    valor_pago      NUMERIC(14,2) NOT NULL DEFAULT 0,
    observacao      VARCHAR(300),
    criado_em       TIMESTAMP     NOT NULL DEFAULT now(),
    atualizado_em   TIMESTAMP,
    CONSTRAINT ck_romaneio_status CHECK (status IN ('ABERTO', 'PENDENTE', 'FINALIZADO', 'CANCELADO')),
    CONSTRAINT ck_romaneio_valores CHECK (total_caixas >= 0 AND valor_bruto >= 0 AND total_despesas >= 0
                                          AND valor_pago >= 0)
);

CREATE INDEX idx_romaneio_lote ON romaneio (idlote);
CREATE INDEX idx_romaneio_parceiro ON romaneio (id_parceiro, status);
CREATE INDEX idx_romaneio_safra_quadra ON romaneio (id_safra, id_quadra);

CREATE TABLE romaneio_item (
    id               SERIAL PRIMARY KEY,
    idromaneio       INTEGER       NOT NULL REFERENCES romaneio(idromaneio) ON DELETE CASCADE,
    idclassificacao  INTEGER       NOT NULL REFERENCES classificacao(idclassificacao),
    idembalagem      INTEGER       NOT NULL REFERENCES embalagem(idembalagem),
    caixas           NUMERIC(12,2) NOT NULL,
    preco_unitario   NUMERIC(12,2),
    valor            NUMERIC(14,2) NOT NULL DEFAULT 0,
    CONSTRAINT ck_romaneio_item_caixas CHECK (caixas > 0),
    CONSTRAINT ck_romaneio_item_preco CHECK (preco_unitario IS NULL OR preco_unitario >= 0),
    CONSTRAINT uk_romaneio_item UNIQUE (idromaneio, idclassificacao, idembalagem)
);

CREATE INDEX idx_romaneio_item_embalagem ON romaneio_item (idembalagem);

-- Despesas de venda: cópia fiel do momento (mudar o cadastro de despesas não altera romaneios antigos).
-- EMBALAGEM = linha automática "Custo de embalagem" (Σ caixas × embalagem.preco_compra), sem iddespesascusto.
CREATE TABLE romaneio_despesa (
    id               SERIAL PRIMARY KEY,
    idromaneio       INTEGER       NOT NULL REFERENCES romaneio(idromaneio) ON DELETE CASCADE,
    iddespesascusto  INTEGER       REFERENCES despesascusto(iddespesascusto) ON DELETE SET NULL,
    descricao        VARCHAR(100)  NOT NULL,
    forma_calculo    VARCHAR(20)   NOT NULL,
    percentual       NUMERIC(7,3),
    quantidade       NUMERIC(12,2),
    valor_unitario   NUMERIC(12,4),
    valor            NUMERIC(14,2) NOT NULL DEFAULT 0,
    CONSTRAINT ck_romaneio_despesa_forma CHECK (forma_calculo IN ('FIXO', 'PERCENTUAL_VENDA', 'POR_CAIXA', 'EMBALAGEM')),
    CONSTRAINT ck_romaneio_despesa_valor CHECK (valor >= 0)
);

CREATE INDEX idx_romaneio_despesa_romaneio ON romaneio_despesa (idromaneio);

-- Pagamento em grupo: um recebimento do parceiro distribuído entre os romaneios PENDENTES dele.
CREATE TABLE romaneio_pagamento (
    idpagamento      SERIAL PRIMARY KEY,
    id_parceiro      INTEGER       NOT NULL,
    data_pagamento   DATE          NOT NULL,
    forma            VARCHAR(20)   NOT NULL,
    valor            NUMERIC(14,2) NOT NULL,
    banco            VARCHAR(60),
    numero_cheque    VARCHAR(30),
    emitente         VARCHAR(80),
    bom_para         DATE,
    observacao       VARCHAR(300),
    criado_em        TIMESTAMP     NOT NULL DEFAULT now(),
    CONSTRAINT ck_romaneio_pagamento_forma CHECK (forma IN ('DINHEIRO', 'PIX', 'CHEQUE_PROPRIO', 'CHEQUE_TERCEIRO')),
    CONSTRAINT ck_romaneio_pagamento_valor CHECK (valor > 0)
);

CREATE INDEX idx_romaneio_pagamento_parceiro ON romaneio_pagamento (id_parceiro);

CREATE TABLE romaneio_pagamento_item (
    id           SERIAL PRIMARY KEY,
    idpagamento  INTEGER       NOT NULL REFERENCES romaneio_pagamento(idpagamento) ON DELETE CASCADE,
    idromaneio   INTEGER       NOT NULL REFERENCES romaneio(idromaneio),
    valor        NUMERIC(14,2) NOT NULL,
    CONSTRAINT ck_romaneio_pagamento_item_valor CHECK (valor > 0)
);

CREATE INDEX idx_romaneio_pagamento_item_romaneio ON romaneio_pagamento_item (idromaneio);

COMMENT ON TABLE romaneio IS
  'Venda de caixas de um lote de colheita a um parceiro (PARCEIRO). Valores sempre recalculados; lidos pela Safra.';
COMMENT ON TABLE romaneio_despesa IS
  'Despesas de venda do romaneio (snapshot): FIXO, PERCENTUAL_VENDA, POR_CAIXA e a linha automática EMBALAGEM.';
COMMENT ON TABLE romaneio_pagamento IS
  'Recebimento de um parceiro distribuído entre romaneios PENDENTES (romaneio_pagamento_item). Estorno = exclusão.';
