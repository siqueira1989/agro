-- =====================================================================
-- V14 — Diário de Campo (centro operacional)
--
-- Registra cada operação por talhão (quadra) com rastreabilidade técnica,
-- operacional e financeira: mão de obra + insumos + máquinas → custo total,
-- e baixa automática de estoque ao finalizar.
--
-- Reutiliza os cadastros existentes (NÃO duplica):
--   Área    → areaproducao        Talhão  → quadra
--   Cultura → alimento(idproduto)  Insumo  → insumo
--   Funcionário → hierarquia funcionario (idpessoa validado por trigger)
--
-- Idempotente.
-- =====================================================================

BEGIN;

-- Tipos de atividade (extensível: novos tipos podem ser adicionados) -----
CREATE TABLE IF NOT EXISTS diario_tipo_atividade (
    idtipo SERIAL PRIMARY KEY,
    nome   VARCHAR(40) NOT NULL UNIQUE,
    ativo  BOOLEAN NOT NULL DEFAULT true
);

INSERT INTO diario_tipo_atividade (nome)
SELECT x FROM (VALUES
    ('Plantio'), ('Pulverização'), ('Adubação'), ('Colheita'), ('Irrigação'),
    ('Capina'), ('Roçada'), ('Poda'), ('Inspeção'), ('Outro')
) AS v(x)
WHERE NOT EXISTS (SELECT 1 FROM diario_tipo_atividade d WHERE d.nome = v.x);

-- Cabeçalho do diário ----------------------------------------------------
CREATE TABLE IF NOT EXISTS diario_campo (
    iddiario          SERIAL PRIMARY KEY,
    numero_diario     VARCHAR(20) NOT NULL UNIQUE,          -- gerado (YYYY-000123)
    data              DATE NOT NULL DEFAULT CURRENT_DATE,
    id_area           INTEGER REFERENCES areaproducao(idareaproducao),
    id_quadra         INTEGER REFERENCES quadra(idquadra),
    id_cultura        INTEGER REFERENCES alimento(idproduto),
    id_responsavel    INTEGER,                              -- idpessoa (trigger valida)
    id_tipo_atividade INTEGER NOT NULL REFERENCES diario_tipo_atividade(idtipo),
    descricao         VARCHAR(200),
    status            VARCHAR(15) NOT NULL DEFAULT 'PLANEJADA', -- PLANEJADA|EM_ANDAMENTO|CONCLUIDA|CANCELADA
    data_prevista     DATE,
    data_realizada    DATE,
    hora_inicio       TIME,
    hora_fim          TIME,
    observacoes       TEXT,
    custo_mao_obra    NUMERIC(12,2) NOT NULL DEFAULT 0,
    custo_insumos     NUMERIC(12,2) NOT NULL DEFAULT 0,
    custo_maquinas    NUMERIC(12,2) NOT NULL DEFAULT 0,
    custo_total       NUMERIC(12,2) NOT NULL DEFAULT 0,
    criado_em         TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_diario_data     ON diario_campo (data);
CREATE INDEX IF NOT EXISTS idx_diario_area     ON diario_campo (id_area);
CREATE INDEX IF NOT EXISTS idx_diario_quadra   ON diario_campo (id_quadra);
CREATE INDEX IF NOT EXISTS idx_diario_cultura  ON diario_campo (id_cultura);
CREATE INDEX IF NOT EXISTS idx_diario_tipo     ON diario_campo (id_tipo_atividade);
CREATE INDEX IF NOT EXISTS idx_diario_status   ON diario_campo (status);

DROP TRIGGER IF EXISTS chk_func_exist ON diario_campo;
CREATE TRIGGER chk_func_exist BEFORE INSERT OR UPDATE ON diario_campo
    FOR EACH ROW EXECUTE FUNCTION trg_valida_funcionario_existe('id_responsavel');

-- Funcionários do diário (N) --------------------------------------------
CREATE TABLE IF NOT EXISTS diario_funcionario (
    id                SERIAL PRIMARY KEY,
    iddiario          INTEGER NOT NULL REFERENCES diario_campo(iddiario) ON DELETE CASCADE,
    idpessoa          INTEGER NOT NULL,                     -- trigger valida
    tipo_funcionario  VARCHAR(12),                          -- snapshot CLT|DIARISTA|EMPREITA
    funcao_exercida   VARCHAR(60),
    horas_trabalhadas NUMERIC(6,2) NOT NULL DEFAULT 0,
    custo_hora        NUMERIC(12,2) NOT NULL DEFAULT 0,     -- snapshot
    valor_contratado  NUMERIC(12,2) NOT NULL DEFAULT 0,     -- empreita
    custo             NUMERIC(12,2) NOT NULL DEFAULT 0      -- snapshot no finalizar
);
CREATE INDEX IF NOT EXISTS idx_diariofunc_diario ON diario_funcionario (iddiario);

DROP TRIGGER IF EXISTS chk_func_exist ON diario_funcionario;
CREATE TRIGGER chk_func_exist BEFORE INSERT OR UPDATE ON diario_funcionario
    FOR EACH ROW EXECUTE FUNCTION trg_valida_funcionario_existe('idpessoa');

-- Máquinas do diário (N) — sem cadastro próprio ainda (id_maquina futuro) -
CREATE TABLE IF NOT EXISTS diario_maquina (
    id                 SERIAL PRIMARY KEY,
    iddiario           INTEGER NOT NULL REFERENCES diario_campo(iddiario) ON DELETE CASCADE,
    id_maquina         INTEGER,                             -- para cadastro futuro
    categoria          VARCHAR(12),                         -- TRATOR|IMPLEMENTO|VEICULO
    nome               VARCHAR(80),
    horimetro_inicial  NUMERIC(10,2) NOT NULL DEFAULT 0,
    horimetro_final    NUMERIC(10,2) NOT NULL DEFAULT 0,
    horas_trabalhadas  NUMERIC(10,2) NOT NULL DEFAULT 0,
    valor_hora         NUMERIC(12,2) NOT NULL DEFAULT 0,
    custo              NUMERIC(12,2) NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_diariomaq_diario ON diario_maquina (iddiario);

-- Insumos do diário (N) --------------------------------------------------
CREATE TABLE IF NOT EXISTS diario_insumo (
    id             SERIAL PRIMARY KEY,
    iddiario       INTEGER NOT NULL REFERENCES diario_campo(iddiario) ON DELETE CASCADE,
    id_insumo      INTEGER NOT NULL REFERENCES insumo(idinsumo),
    quantidade     NUMERIC(14,3) NOT NULL DEFAULT 0,
    unidade        VARCHAR(5),                              -- snapshot
    dose_aplicada  VARCHAR(60),
    custo_unitario NUMERIC(12,2) NOT NULL DEFAULT 0,        -- snapshot preco_medio
    custo          NUMERIC(12,2) NOT NULL DEFAULT 0,
    baixado        BOOLEAN NOT NULL DEFAULT false           -- já deu baixa no estoque?
);
CREATE INDEX IF NOT EXISTS idx_diarioins_diario ON diario_insumo (iddiario);

COMMIT;
