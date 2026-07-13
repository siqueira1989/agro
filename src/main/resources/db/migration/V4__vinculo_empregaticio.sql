-- =====================================================================
-- V4 — Vínculo empregatício (múltiplos períodos de trabalho por pessoa)
--
-- Separa a PESSOA (identidade) do VÍNCULO (período de trabalho). Uma pessoa
-- passa a poder ter N vínculos (não sobrepostos), cada um com seus próprios
-- cargo, salário, hora extra, jornada, atividade, admissão/desligamento e
-- status — preservando o histórico de cada passagem pela empresa.
--
-- Matrícula permanece em `funcionario` (constante por pessoa).
-- Idempotente.
-- =====================================================================

BEGIN;

-- 1) Tabela de vínculos --------------------------------------------------
CREATE TABLE IF NOT EXISTS vinculo_empregaticio (
    id_vinculo            SERIAL PRIMARY KEY,
    idpessoa              INTEGER NOT NULL,
    tipo_funcionario      tipo_funcionario NOT NULL,
    cargo                 VARCHAR(60),
    tipo_atividade_rural  tipo_atividade_rural NOT NULL DEFAULT 'LAVOURA',
    data_admissao         DATE,
    data_desligamento     DATE,
    salario_mensal        NUMERIC(12,2) NOT NULL DEFAULT 0,
    valor_hora_extra      NUMERIC(12,2) NOT NULL DEFAULT 0,
    carga_horaria_diaria  INTEGER NOT NULL DEFAULT 8,
    status                VARCHAR(20) NOT NULL DEFAULT 'ATIVO',
    motivo_desligamento   VARCHAR(200),
    criado_em             TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_vinculo_pessoa ON vinculo_empregaticio (idpessoa);

-- 2) Validação de idpessoa contra a hierarquia (reutiliza função da V2) ---
DROP TRIGGER IF EXISTS chk_func_exist ON vinculo_empregaticio;
CREATE TRIGGER chk_func_exist BEFORE INSERT OR UPDATE ON vinculo_empregaticio
    FOR EACH ROW EXECUTE FUNCTION trg_valida_funcionario_existe('idpessoa');

-- 3) Cascade: ao excluir o funcionário, remove seus vínculos -------------
CREATE OR REPLACE FUNCTION trg_cascade_delete_funcionario()
RETURNS trigger AS $$
BEGIN
    DELETE FROM ponto_eletronico       WHERE idpessoa      = OLD.idpessoa;
    DELETE FROM falta_funcionario      WHERE idpessoa      = OLD.idpessoa;
    DELETE FROM vale_funcionario       WHERE idpessoa      = OLD.idpessoa;
    DELETE FROM fechamento_folha_ponto WHERE idpessoa      = OLD.idpessoa;
    DELETE FROM folha_pagamento        WHERE idfuncionario = OLD.idpessoa;
    DELETE FROM registroponto          WHERE idfuncionario = OLD.idpessoa;
    DELETE FROM vinculo_empregaticio   WHERE idpessoa      = OLD.idpessoa;
    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

-- 4) Migração: 1 vínculo para cada funcionário existente ------------------
-- Campos comuns vêm da hierarquia `funcionario`; financeiro/atividade/status
-- específicos do CLT são copiados de `funcionarioclt` logo abaixo.
INSERT INTO vinculo_empregaticio
    (idpessoa, tipo_funcionario, cargo, data_admissao, data_desligamento, status)
SELECT f.idpessoa, f.tipofuncionario, f.cargofuncionario,
       f.datainiciofuncionario, f.datafimfuncionario,
       CASE WHEN f.situacaopessoa THEN 'ATIVO' ELSE 'DESLIGADO' END
FROM funcionario f
WHERE NOT EXISTS (
    SELECT 1 FROM vinculo_empregaticio v WHERE v.idpessoa = f.idpessoa
);

UPDATE vinculo_empregaticio v
SET salario_mensal       = c.salariomensal,
    valor_hora_extra     = c.valorhoraextra,
    carga_horaria_diaria = c.cargahorariadiaria,
    tipo_atividade_rural = c.tipo_atividade_rural,
    status               = COALESCE(c.statusemprego, v.status)
FROM funcionarioclt c
WHERE v.idpessoa = c.idpessoa;

-- 5) No máximo 1 vínculo ATIVO (sem data de desligamento) por pessoa ------
CREATE UNIQUE INDEX IF NOT EXISTS uk_vinculo_ativo
    ON vinculo_empregaticio (idpessoa) WHERE data_desligamento IS NULL;

COMMIT;
