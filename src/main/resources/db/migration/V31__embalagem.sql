-- Módulo Colheita e Estoque — cadastro de embalagens (pacote Romaneio, onda A).
--
-- Decisão do usuário (02/11): o preço PAGO AO EMBALADOR por caixa é diferente do preço de COMPRA
-- da embalagem pela empresa. Os dois ficam aqui, por alimento + área de produção:
--   preco_embalador → modal Processar (diário de Processamento, regime PRODUCAO);
--   preco_compra    → Romaneio (custo da embalagem).
-- A categoria EMBALAGEM de despesascusto (V28) deixa de ser usada pelo Processar; não havia
-- registros EMBALAGEM na base, então não há migração de dados.
CREATE TABLE embalagem (
    idembalagem        SERIAL PRIMARY KEY,
    codigo             VARCHAR(30)   NOT NULL,
    descricao          VARCHAR(80)   NOT NULL,
    peso_kg            NUMERIC(8,3)  NOT NULL,
    id_alimento        INTEGER       NOT NULL REFERENCES alimento(idproduto),
    id_area_producao   INTEGER       NOT NULL REFERENCES areaproducao(idareaproducao),
    preco_compra       NUMERIC(12,2) NOT NULL,
    preco_embalador    NUMERIC(12,2) NOT NULL,
    ativo              BOOLEAN       NOT NULL DEFAULT true,
    criado_em          TIMESTAMP     NOT NULL DEFAULT now(),
    atualizado_em      TIMESTAMP,
    CONSTRAINT ck_embalagem_codigo CHECK (codigo ~ '^[A-Z0-9_]{1,30}$'),
    CONSTRAINT ck_embalagem_descricao CHECK (btrim(descricao) <> ''),
    CONSTRAINT ck_embalagem_peso CHECK (peso_kg > 0),
    CONSTRAINT ck_embalagem_precos CHECK (preco_compra > 0 AND preco_embalador > 0),
    CONSTRAINT uk_embalagem UNIQUE (codigo, id_alimento, id_area_producao)
);

CREATE INDEX idx_embalagem_alimento_area ON embalagem (id_alimento, id_area_producao) WHERE ativo;

COMMENT ON TABLE embalagem IS
  'Embalagens por alimento + área: preco_embalador (pago por caixa no Processamento) e preco_compra (custo no Romaneio).';

-- Detalhe do processamento passa a apontar para o cadastro. Registros anteriores ficam com NULL
-- (usavam despesascusto/EMBALAGEM). Sem exclusão de embalagem: só desativar (ativo=false).
ALTER TABLE colheita_processamento_embalagem
    ADD COLUMN idembalagem INTEGER REFERENCES embalagem(idembalagem);

CREATE INDEX idx_colheita_proc_emb_embalagem ON colheita_processamento_embalagem (idembalagem);
