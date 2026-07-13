-- =====================================================================
-- V2 — Integridade referencial para a hierarquia de herança "funcionario"
--
-- Problema: as FKs que apontam para funcionario(idpessoa) NÃO enxergam
-- linhas das tabelas filhas (funcionarioclt/diarista/empreita/producao),
-- porque chaves estrangeiras do PostgreSQL não respeitam herança.
-- Resultado: era impossível inserir ponto/vale/falta/folha para qualquer
-- funcionário, pois eles vivem fisicamente nas tabelas filhas.
--
-- Solução: substituir as 7 FKs por triggers que validam o idpessoa
-- contra a hierarquia completa (SELECT em funcionario enxerga as filhas)
-- e replicam o comportamento ON DELETE CASCADE.
-- =====================================================================

BEGIN;

-- 1) Remove as FKs incompatíveis com herança ----------------------------
ALTER TABLE registroponto          DROP CONSTRAINT IF EXISTS fk_regponto_func;
ALTER TABLE folha_pagamento        DROP CONSTRAINT IF EXISTS folha_pagamento_idfuncionario_fkey;
ALTER TABLE ponto_eletronico       DROP CONSTRAINT IF EXISTS ponto_eletronico_idpessoa_fkey;
ALTER TABLE falta_funcionario      DROP CONSTRAINT IF EXISTS falta_funcionario_idpessoa_fkey;
ALTER TABLE falta_funcionario      DROP CONSTRAINT IF EXISTS fk_falta_funcionario_idpessoa;
ALTER TABLE vale_funcionario       DROP CONSTRAINT IF EXISTS vale_funcionario_idpessoa_fkey;
ALTER TABLE fechamento_folha_ponto DROP CONSTRAINT IF EXISTS fechamento_folha_ponto_idpessoa_fkey;

-- 2) Função de validação ------------------------------------------------
-- Recebe o nome da coluna FK como argumento (TG_ARGV[0]) e garante que o
-- valor exista em funcionario (consulta que, por herança, vê as filhas).
CREATE OR REPLACE FUNCTION trg_valida_funcionario_existe()
RETURNS trigger AS $$
DECLARE
    id_val integer;
BEGIN
    id_val := (row_to_json(NEW) ->> TG_ARGV[0])::integer;
    IF id_val IS NOT NULL
       AND NOT EXISTS (SELECT 1 FROM funcionario WHERE idpessoa = id_val) THEN
        RAISE EXCEPTION 'Funcionário (idpessoa=%) não existe na hierarquia funcionario', id_val
            USING ERRCODE = 'foreign_key_violation';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 3) Triggers de validação nas tabelas dependentes ----------------------
DROP TRIGGER IF EXISTS chk_func_exist ON registroponto;
CREATE TRIGGER chk_func_exist BEFORE INSERT OR UPDATE ON registroponto
    FOR EACH ROW EXECUTE FUNCTION trg_valida_funcionario_existe('idfuncionario');

DROP TRIGGER IF EXISTS chk_func_exist ON folha_pagamento;
CREATE TRIGGER chk_func_exist BEFORE INSERT OR UPDATE ON folha_pagamento
    FOR EACH ROW EXECUTE FUNCTION trg_valida_funcionario_existe('idfuncionario');

DROP TRIGGER IF EXISTS chk_func_exist ON ponto_eletronico;
CREATE TRIGGER chk_func_exist BEFORE INSERT OR UPDATE ON ponto_eletronico
    FOR EACH ROW EXECUTE FUNCTION trg_valida_funcionario_existe('idpessoa');

DROP TRIGGER IF EXISTS chk_func_exist ON falta_funcionario;
CREATE TRIGGER chk_func_exist BEFORE INSERT OR UPDATE ON falta_funcionario
    FOR EACH ROW EXECUTE FUNCTION trg_valida_funcionario_existe('idpessoa');

DROP TRIGGER IF EXISTS chk_func_exist ON vale_funcionario;
CREATE TRIGGER chk_func_exist BEFORE INSERT OR UPDATE ON vale_funcionario
    FOR EACH ROW EXECUTE FUNCTION trg_valida_funcionario_existe('idpessoa');

DROP TRIGGER IF EXISTS chk_func_exist ON fechamento_folha_ponto;
CREATE TRIGGER chk_func_exist BEFORE INSERT OR UPDATE ON fechamento_folha_ponto
    FOR EACH ROW EXECUTE FUNCTION trg_valida_funcionario_existe('idpessoa');

-- 4) Função de cascade ao excluir funcionário ---------------------------
CREATE OR REPLACE FUNCTION trg_cascade_delete_funcionario()
RETURNS trigger AS $$
BEGIN
    DELETE FROM ponto_eletronico       WHERE idpessoa     = OLD.idpessoa;
    DELETE FROM falta_funcionario      WHERE idpessoa     = OLD.idpessoa;
    DELETE FROM vale_funcionario       WHERE idpessoa     = OLD.idpessoa;
    DELETE FROM fechamento_folha_ponto WHERE idpessoa     = OLD.idpessoa;
    DELETE FROM folha_pagamento        WHERE idfuncionario = OLD.idpessoa;
    DELETE FROM registroponto          WHERE idfuncionario = OLD.idpessoa;
    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

-- 5) Triggers de cascade em toda a hierarquia ---------------------------
-- Triggers não são herdados; o DELETE numa linha física dispara apenas o
-- trigger da tabela onde a linha reside. Por isso aplicamos nas 5 tabelas.
DROP TRIGGER IF EXISTS casc_del_func ON funcionario;
CREATE TRIGGER casc_del_func AFTER DELETE ON funcionario
    FOR EACH ROW EXECUTE FUNCTION trg_cascade_delete_funcionario();

DROP TRIGGER IF EXISTS casc_del_func ON funcionarioclt;
CREATE TRIGGER casc_del_func AFTER DELETE ON funcionarioclt
    FOR EACH ROW EXECUTE FUNCTION trg_cascade_delete_funcionario();

DROP TRIGGER IF EXISTS casc_del_func ON funcionariodiarista;
CREATE TRIGGER casc_del_func AFTER DELETE ON funcionariodiarista
    FOR EACH ROW EXECUTE FUNCTION trg_cascade_delete_funcionario();

DROP TRIGGER IF EXISTS casc_del_func ON funcionarioempreita;
CREATE TRIGGER casc_del_func AFTER DELETE ON funcionarioempreita
    FOR EACH ROW EXECUTE FUNCTION trg_cascade_delete_funcionario();

DROP TRIGGER IF EXISTS casc_del_func ON funcionarioproducao;
CREATE TRIGGER casc_del_func AFTER DELETE ON funcionarioproducao
    FOR EACH ROW EXECUTE FUNCTION trg_cascade_delete_funcionario();

COMMIT;
