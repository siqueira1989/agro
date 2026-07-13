-- ============================================================
--  Módulo: Controle de Ponto Eletrônico e Cálculo de Folha
--  Banco  : PostgreSQL
--  Data   : 2026-06
-- ============================================================

-- Tabela principal de registros de ponto (4 marcações diárias)
CREATE TABLE IF NOT EXISTS ponto_eletronico (
    idponto         SERIAL PRIMARY KEY,
    idpessoa        INT NOT NULL REFERENCES funcionario(idpessoa) ON DELETE CASCADE,
    dataregistro    DATE NOT NULL,
    entrada1        TIME,
    saida1          TIME,
    entrada2        TIME,
    saida2          TIME,
    total_minutos   INT NOT NULL DEFAULT 0,
    extra_minutos   INT NOT NULL DEFAULT 0,
    observacao      VARCHAR(200),
    criado_em       TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_ponto_funcionario_dia UNIQUE (idpessoa, dataregistro)
);

CREATE INDEX IF NOT EXISTS idx_ponto_funcionario ON ponto_eletronico(idpessoa);
CREATE INDEX IF NOT EXISTS idx_ponto_data       ON ponto_eletronico(dataregistro);

-- Faltas (justificadas ou não)
CREATE TABLE IF NOT EXISTS falta_funcionario (
    idfalta         SERIAL PRIMARY KEY,
    idpessoa        INT NOT NULL REFERENCES funcionario(idpessoa) ON DELETE CASCADE,
    datafalta       DATE NOT NULL,
    justificada     BOOLEAN NOT NULL DEFAULT FALSE,
    motivo          VARCHAR(200),
    registrado_em   TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_falta_funcionario_dia UNIQUE (idpessoa, datafalta)
);

CREATE INDEX IF NOT EXISTS idx_falta_funcionario ON falta_funcionario(idpessoa);

-- Vales / Adiantamentos
CREATE TABLE IF NOT EXISTS vale_funcionario (
    idvale          SERIAL PRIMARY KEY,
    idpessoa        INT NOT NULL REFERENCES funcionario(idpessoa) ON DELETE CASCADE,
    datavale        DATE NOT NULL,
    valor           NUMERIC(10,2) NOT NULL CHECK (valor > 0),
    descricao       VARCHAR(200),
    registrado_em   TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_vale_funcionario ON vale_funcionario(idpessoa);

-- Fechamento mensal (resultado financeiro calculado)
CREATE TABLE IF NOT EXISTS fechamento_folha_ponto (
    idfechamento            SERIAL PRIMARY KEY,
    idpessoa                INT NOT NULL REFERENCES funcionario(idpessoa) ON DELETE CASCADE,
    periodo                 VARCHAR(7) NOT NULL,          -- formato YYYY-MM
    salario_base            NUMERIC(10,2) NOT NULL,
    jornada_horas_dia       INT NOT NULL DEFAULT 8,
    dias_uteis              INT NOT NULL DEFAULT 0,
    dias_trabalhados        INT NOT NULL DEFAULT 0,
    faltas_injustificadas   INT NOT NULL DEFAULT 0,
    total_minutos_extras    INT NOT NULL DEFAULT 0,
    valor_horas_extras      NUMERIC(10,2) NOT NULL DEFAULT 0.00,
    desconto_faltas         NUMERIC(10,2) NOT NULL DEFAULT 0.00,
    total_vales             NUMERIC(10,2) NOT NULL DEFAULT 0.00,
    salario_bruto           NUMERIC(10,2) NOT NULL DEFAULT 0.00,
    salario_liquido         NUMERIC(10,2) NOT NULL DEFAULT 0.00,
    fechado_em              TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_fechamento_func_periodo UNIQUE (idpessoa, periodo)
);

COMMENT ON TABLE ponto_eletronico IS 'Registro diário de ponto: entrada1/saida1 (manhã) e entrada2/saida2 (tarde)';
COMMENT ON TABLE falta_funcionario IS 'Faltas registradas; justificada=true não desconta salário';
COMMENT ON TABLE vale_funcionario  IS 'Adiantamentos concedidos ao funcionário no mês';
COMMENT ON TABLE fechamento_folha_ponto IS 'Resultado financeiro mensal calculado pelo sistema';
