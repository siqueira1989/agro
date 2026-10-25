-- Módulo Colheita e Estoque — modal "Processar" (onda 3).
--
-- Cada processamento de um lote (baixa de uma parte do saldo) passa a ser registrado com o
-- diário de Processamento que o Diário de Campo gerou para ele (mão de obra: embaladores por
-- caixa e demais funcionários por horas) e com o detalhe das embalagens usadas por embalador
-- (tipo de embalagem, nº de caixas e preço/caixa vindo de Despesas e Custos — categoria EMBALAGEM).
--
-- O Diário NÃO guarda o lote: o vínculo lote ↔ diário de Processamento é desta tabela.
CREATE TABLE colheita_processamento (
    idprocessamento      SERIAL PRIMARY KEY,
    idlote               INTEGER NOT NULL REFERENCES colheita_lote(idlote) ON DELETE CASCADE,
    iddiario             INTEGER REFERENCES diario_campo(iddiario) ON DELETE SET NULL,
    data_processamento   DATE NOT NULL,
    quantidade           NUMERIC(12,2) NOT NULL,
    total_caixas         NUMERIC(12,2) NOT NULL DEFAULT 0,
    valor_embalagem      NUMERIC(14,2) NOT NULL DEFAULT 0,
    criado_em            TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT ck_colheita_proc_quantidade CHECK (quantidade > 0),
    CONSTRAINT ck_colheita_proc_totais CHECK (total_caixas >= 0 AND valor_embalagem >= 0)
);

-- Um diário de Processamento corresponde a um único processamento de lote.
CREATE UNIQUE INDEX uk_colheita_proc_diario ON colheita_processamento (iddiario) WHERE iddiario IS NOT NULL;
CREATE INDEX idx_colheita_proc_lote ON colheita_processamento (idlote);

CREATE TABLE colheita_processamento_embalagem (
    id                   SERIAL PRIMARY KEY,
    idprocessamento      INTEGER NOT NULL REFERENCES colheita_processamento(idprocessamento) ON DELETE CASCADE,
    idpessoa             INTEGER NOT NULL REFERENCES pessoa(idpessoa),
    iddespesascusto      INTEGER REFERENCES despesascusto(iddespesascusto) ON DELETE SET NULL,
    tipo_caixa           VARCHAR(30) NOT NULL,
    quantidade_caixas    NUMERIC(12,2) NOT NULL,
    preco_caixa          NUMERIC(12,2) NOT NULL,
    valor                NUMERIC(14,2) NOT NULL,
    CONSTRAINT ck_colheita_proc_emb_caixas CHECK (quantidade_caixas > 0),
    CONSTRAINT ck_colheita_proc_emb_preco CHECK (preco_caixa > 0 AND valor >= 0)
);

CREATE INDEX idx_colheita_proc_emb_proc ON colheita_processamento_embalagem (idprocessamento);

COMMENT ON TABLE colheita_processamento IS
  'Baixa de uma parte do saldo de um colheita_lote, ligada ao diário de Processamento gerado no modal Processar.';
COMMENT ON TABLE colheita_processamento_embalagem IS
  'Embalagens por embalador num processamento: tipo (despesascusto EMBALAGEM), caixas e preço/caixa (snapshot).';
