-- Tabela de Registro de Ponto
CREATE TABLE IF NOT EXISTS registroponto (
    idregistroponto SERIAL PRIMARY KEY,
    idfuncionario   INT     NOT NULL REFERENCES pessoafisica(idpessoa) ON DELETE CASCADE,
    dataregistro    DATE    NOT NULL,
    presente        BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (idfuncionario, dataregistro)
);

-- Tabela de Folha de Pagamento
CREATE TABLE IF NOT EXISTS folha_pagamento (
    idfolha          SERIAL PRIMARY KEY,
    idfuncionario    INT                 NOT NULL REFERENCES funcionario(idpessoa) ON DELETE CASCADE,
    periodo          VARCHAR(7)          NOT NULL,   -- formato YYYY-MM
    tipofuncionario  tipo_funcionario    NOT NULL,
    valorcalculado   NUMERIC(12,2)       NOT NULL DEFAULT 0,
    datageracao      TIMESTAMP           NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_folha_periodo ON folha_pagamento(periodo);
CREATE INDEX IF NOT EXISTS idx_folha_funcionario ON folha_pagamento(idfuncionario);

-- Tabela de Lançamento de Produção (para funcionários do tipo PRODUCAO)
CREATE TABLE IF NOT EXISTS lancamento_producao (
    idlancamento        SERIAL PRIMARY KEY,
    idfuncionario       INT        NOT NULL REFERENCES funcionarioproducao(idpessoa) ON DELETE CASCADE,
    periodo             VARCHAR(7) NOT NULL,   -- formato YYYY-MM
    quantidadeproduzida INT        NOT NULL DEFAULT 0,
    UNIQUE (idfuncionario, periodo)
);
