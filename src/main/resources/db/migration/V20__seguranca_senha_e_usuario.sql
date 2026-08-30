-- ===========================================================================
-- V20 — Seguranca de credenciais
-- Auditoria de 29/08/2026: itens P0-2 (senha em texto puro) e P1-16 (login
-- sem indice nem unicidade).
--
-- ATENCAO — heranca de tabelas:
-- ALTER TABLE em "pessoa" propaga automaticamente para as filhas
-- (pessoafisica, funcionario, funcionarioclt, funcionariodiarista,
--  funcionarioempreita, funcionarioproducao, pessoacnpj, parceiro).
-- Ja os INDICES nao sao herdados: precisam ser criados um a um. E a
-- unicidade tambem nao atravessa a heranca, por isso ha um trigger global
-- alem dos indices por tabela.
-- ===========================================================================

-- 1) Espaco para o hash PBKDF2 ---------------------------------------------
-- Formato: pbkdf2$sha256$<iteracoes>$<salt b64>$<hash b64>  (~120 caracteres)
-- A coluna era varchar(50), suficiente so para texto puro.
ALTER TABLE public.pessoa ALTER COLUMN senhapessoa TYPE character varying(255);

COMMENT ON COLUMN public.pessoa.senhapessoa IS
  'Hash PBKDF2-HMAC-SHA256 no formato pbkdf2$sha256$<iter>$<salt>$<hash>. '
  'Nunca gravar texto puro. Gerado por Util.SenhaUtil.';

-- 2) Normalizar usuario em branco -----------------------------------------
-- ControllerFuncionario grava a string crua do formulario: quem cadastra sem
-- login fica com '' (string vazia), nao NULL. Como '' e um valor como outro
-- qualquer, dois cadastros sem login seriam vistos como usuario duplicado e
-- travariam a criacao do indice unico — e, depois, todo cadastro novo sem
-- login. Em SQL, varios NULL convivem num indice unico; varios '' nao.
UPDATE public.pessoa
   SET usuariopessoa = NULL
 WHERE usuariopessoa IS NOT NULL AND btrim(usuariopessoa) = '';

-- 3) Barrar login realmente duplicado antes de criar os indices ------------
-- Se sobrar duplicidade de verdade, o indice unico falharia com uma mensagem
-- opaca. Este bloco falha antes, dizendo exatamente quais sao.
DO $$
DECLARE
    duplicados text;
BEGIN
    SELECT string_agg(u, ', ')
      INTO duplicados
      FROM (
          SELECT usuariopessoa AS u
            FROM public.pessoa
           WHERE usuariopessoa IS NOT NULL AND btrim(usuariopessoa) <> ''
           GROUP BY usuariopessoa
          HAVING count(*) > 1
      ) d;

    IF duplicados IS NOT NULL THEN
        RAISE EXCEPTION
          'Existem usuarios duplicados que impedem a criacao do indice unico: %. '
          'Renomeie ou desative os cadastros repetidos e rode a migracao de novo.',
          duplicados;
    END IF;
END $$;

-- 4) Indice unico por tabela ------------------------------------------------
-- Resolve DOIS problemas de uma vez:
--   a) unicidade dentro de cada tabela;
--   b) o Seq Scan que o login provocava em 9 tabelas a cada tentativa.
CREATE UNIQUE INDEX IF NOT EXISTS uk_pessoa_usuario
    ON public.pessoa (usuariopessoa) WHERE usuariopessoa IS NOT NULL AND btrim(usuariopessoa) <> '';
CREATE UNIQUE INDEX IF NOT EXISTS uk_pessoafisica_usuario
    ON public.pessoafisica (usuariopessoa) WHERE usuariopessoa IS NOT NULL AND btrim(usuariopessoa) <> '';
CREATE UNIQUE INDEX IF NOT EXISTS uk_funcionario_usuario
    ON public.funcionario (usuariopessoa) WHERE usuariopessoa IS NOT NULL AND btrim(usuariopessoa) <> '';
CREATE UNIQUE INDEX IF NOT EXISTS uk_funcionarioclt_usuario
    ON public.funcionarioclt (usuariopessoa) WHERE usuariopessoa IS NOT NULL AND btrim(usuariopessoa) <> '';
CREATE UNIQUE INDEX IF NOT EXISTS uk_funcionariodiarista_usuario
    ON public.funcionariodiarista (usuariopessoa) WHERE usuariopessoa IS NOT NULL AND btrim(usuariopessoa) <> '';
CREATE UNIQUE INDEX IF NOT EXISTS uk_funcionarioempreita_usuario
    ON public.funcionarioempreita (usuariopessoa) WHERE usuariopessoa IS NOT NULL AND btrim(usuariopessoa) <> '';
CREATE UNIQUE INDEX IF NOT EXISTS uk_funcionarioproducao_usuario
    ON public.funcionarioproducao (usuariopessoa) WHERE usuariopessoa IS NOT NULL AND btrim(usuariopessoa) <> '';
CREATE UNIQUE INDEX IF NOT EXISTS uk_pessoacnpj_usuario
    ON public.pessoacnpj (usuariopessoa) WHERE usuariopessoa IS NOT NULL AND btrim(usuariopessoa) <> '';
CREATE UNIQUE INDEX IF NOT EXISTS uk_parceiro_usuario
    ON public.parceiro (usuariopessoa) WHERE usuariopessoa IS NOT NULL AND btrim(usuariopessoa) <> '';

-- 5) Unicidade GLOBAL na hierarquia ----------------------------------------
-- Os indices acima nao impedem que um parceiro e um funcionario usem o mesmo
-- login, porque cada um vive em uma tabela. Um SELECT na tabela-mae enxerga
-- as filhas, entao o trigger cobre a hierarquia inteira.
CREATE OR REPLACE FUNCTION public.trg_usuario_unico_hierarquia()
RETURNS trigger AS $$
BEGIN
    -- Sem login informado nao ha o que verificar. Trata '' como ausencia,
    -- senao o segundo cadastro sem login seria recusado como duplicado.
    IF NEW.usuariopessoa IS NULL OR btrim(NEW.usuariopessoa) = '' THEN
        NEW.usuariopessoa := NULL;
        RETURN NEW;
    END IF;

    IF EXISTS (
        SELECT 1 FROM public.pessoa
         WHERE usuariopessoa = NEW.usuariopessoa
           AND idpessoa IS DISTINCT FROM NEW.idpessoa
    ) THEN
        RAISE EXCEPTION 'O usuario "%" ja esta em uso por outro cadastro.', NEW.usuariopessoa
            USING ERRCODE = 'unique_violation';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS chk_usuario_unico ON public.pessoa;
CREATE TRIGGER chk_usuario_unico BEFORE INSERT OR UPDATE OF usuariopessoa
    ON public.pessoa FOR EACH ROW EXECUTE FUNCTION public.trg_usuario_unico_hierarquia();

DROP TRIGGER IF EXISTS chk_usuario_unico ON public.pessoafisica;
CREATE TRIGGER chk_usuario_unico BEFORE INSERT OR UPDATE OF usuariopessoa
    ON public.pessoafisica FOR EACH ROW EXECUTE FUNCTION public.trg_usuario_unico_hierarquia();

DROP TRIGGER IF EXISTS chk_usuario_unico ON public.funcionario;
CREATE TRIGGER chk_usuario_unico BEFORE INSERT OR UPDATE OF usuariopessoa
    ON public.funcionario FOR EACH ROW EXECUTE FUNCTION public.trg_usuario_unico_hierarquia();

DROP TRIGGER IF EXISTS chk_usuario_unico ON public.funcionarioclt;
CREATE TRIGGER chk_usuario_unico BEFORE INSERT OR UPDATE OF usuariopessoa
    ON public.funcionarioclt FOR EACH ROW EXECUTE FUNCTION public.trg_usuario_unico_hierarquia();

DROP TRIGGER IF EXISTS chk_usuario_unico ON public.funcionariodiarista;
CREATE TRIGGER chk_usuario_unico BEFORE INSERT OR UPDATE OF usuariopessoa
    ON public.funcionariodiarista FOR EACH ROW EXECUTE FUNCTION public.trg_usuario_unico_hierarquia();

DROP TRIGGER IF EXISTS chk_usuario_unico ON public.funcionarioempreita;
CREATE TRIGGER chk_usuario_unico BEFORE INSERT OR UPDATE OF usuariopessoa
    ON public.funcionarioempreita FOR EACH ROW EXECUTE FUNCTION public.trg_usuario_unico_hierarquia();

DROP TRIGGER IF EXISTS chk_usuario_unico ON public.funcionarioproducao;
CREATE TRIGGER chk_usuario_unico BEFORE INSERT OR UPDATE OF usuariopessoa
    ON public.funcionarioproducao FOR EACH ROW EXECUTE FUNCTION public.trg_usuario_unico_hierarquia();

DROP TRIGGER IF EXISTS chk_usuario_unico ON public.pessoacnpj;
CREATE TRIGGER chk_usuario_unico BEFORE INSERT OR UPDATE OF usuariopessoa
    ON public.pessoacnpj FOR EACH ROW EXECUTE FUNCTION public.trg_usuario_unico_hierarquia();

DROP TRIGGER IF EXISTS chk_usuario_unico ON public.parceiro;
CREATE TRIGGER chk_usuario_unico BEFORE INSERT OR UPDATE OF usuariopessoa
    ON public.parceiro FOR EACH ROW EXECUTE FUNCTION public.trg_usuario_unico_hierarquia();

-- 6) A conversao das senhas existentes para PBKDF2 nao pode ser feita em SQL
--    (o PostgreSQL nao calcula PBKDF2 sem pgcrypto). Ela roda no boot da
--    aplicacao, em Util.MigracaoSenhas, disparada por Util.Bootstrap.
