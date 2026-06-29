-- Table: public.funcionario

-- DROP TABLE IF EXISTS public.funcionario;

CREATE TABLE IF NOT EXISTS public.funcionario
(
    -- Inherited from table public.pessoafisica: idpessoa integer NOT NULL DEFAULT nextval('pessoa_idpessoa_seq'::regclass),
    -- Inherited from table public.pessoafisica: nomepessoa character varying(50) COLLATE pg_catalog."default" NOT NULL,
    -- Inherited from table public.pessoafisica: usuariopessoa character varying(50) COLLATE pg_catalog."default",
    -- Inherited from table public.pessoafisica: senhapessoa character varying(50) COLLATE pg_catalog."default",
    -- Inherited from table public.pessoafisica: nivelpessoa character varying(20) COLLATE pg_catalog."default" NOT NULL,
    -- Inherited from table public.pessoafisica: situacaopessoa boolean,
    -- Inherited from table public.pessoafisica: emailpessoa character varying(50) COLLATE pg_catalog."default" NOT NULL,
    -- Inherited from table public.pessoafisica: telefonepessoa character varying(25) COLLATE pg_catalog."default" NOT NULL,
    -- Inherited from table public.pessoafisica: cpfpf character varying(50) COLLATE pg_catalog."default" NOT NULL,
    -- Inherited from table public.pessoafisica: datanascimentopf date NOT NULL,
    -- Inherited from table public.pessoafisica: numero integer NOT NULL,
    -- Inherited from table public.pessoafisica: complemento character varying(50) COLLATE pg_catalog."default" NOT NULL,
    -- Inherited from table public.pessoafisica: cep character varying(100) COLLATE pg_catalog."default",
    matriculafuncionario character varying(30) COLLATE pg_catalog."default" NOT NULL,
    tipofuncionario tipo_funcionario NOT NULL,
    cargofuncionario character varying(60) COLLATE pg_catalog."default",
    datainiciofuncionario date,
    datafimfuncionario date,
    CONSTRAINT funcionario_pk PRIMARY KEY (idpessoa),
    CONSTRAINT funcionario_matriculafuncionario_key UNIQUE (matriculafuncionario)
)
    INHERITS (public.pessoafisica)

TABLESPACE pg_default;

ALTER TABLE IF EXISTS public.funcionario
    OWNER to postgres;
/******************************************************CLT***************************/

-- Table: public.funcionarioclt

-- DROP TABLE IF EXISTS public.funcionarioclt;

CREATE TABLE IF NOT EXISTS public.funcionarioclt
(
    -- Inherited from table public.funcionario: idpessoa integer NOT NULL DEFAULT nextval('pessoa_idpessoa_seq'::regclass),
    -- Inherited from table public.funcionario: nomepessoa character varying(50) COLLATE pg_catalog."default" NOT NULL,
    -- Inherited from table public.funcionario: usuariopessoa character varying(50) COLLATE pg_catalog."default",
    -- Inherited from table public.funcionario: senhapessoa character varying(50) COLLATE pg_catalog."default",
    -- Inherited from table public.funcionario: nivelpessoa character varying(20) COLLATE pg_catalog."default" NOT NULL,
    -- Inherited from table public.funcionario: situacaopessoa boolean,
    -- Inherited from table public.funcionario: emailpessoa character varying(50) COLLATE pg_catalog."default" NOT NULL,
    -- Inherited from table public.funcionario: telefonepessoa character varying(25) COLLATE pg_catalog."default" NOT NULL,
    -- Inherited from table public.funcionario: cpfpf character varying(50) COLLATE pg_catalog."default" NOT NULL,
    -- Inherited from table public.funcionario: datanascimentopf date NOT NULL,
    -- Inherited from table public.funcionario: numero integer NOT NULL,
    -- Inherited from table public.funcionario: complemento character varying(50) COLLATE pg_catalog."default" NOT NULL,
    -- Inherited from table public.funcionario: cep character varying(100) COLLATE pg_catalog."default",
    -- Inherited from table public.funcionario: matriculafuncionario character varying(30) COLLATE pg_catalog."default" NOT NULL,
    -- Inherited from table public.funcionario: tipofuncionario tipo_funcionario NOT NULL,
    -- Inherited from table public.funcionario: cargofuncionario character varying(60) COLLATE pg_catalog."default",
    -- Inherited from table public.funcionario: datainiciofuncionario date,
    -- Inherited from table public.funcionario: datafimfuncionario date,
    salariomensal numeric(12,2) NOT NULL DEFAULT 0,
    valorhoraextra numeric(12,2) NOT NULL DEFAULT 0,
    CONSTRAINT funcionarioclt_pk PRIMARY KEY (idpessoa)
)
    INHERITS (public.funcionario)

TABLESPACE pg_default;

ALTER TABLE IF EXISTS public.funcionarioclt
    OWNER to postgres;

/******************** diarista******************/

-- Table: public.funcionariodiarista

-- DROP TABLE IF EXISTS public.funcionariodiarista;

CREATE TABLE IF NOT EXISTS public.funcionariodiarista
(
    -- Inherited from table public.funcionario: idpessoa integer NOT NULL DEFAULT nextval('pessoa_idpessoa_seq'::regclass),
    -- Inherited from table public.funcionario: nomepessoa character varying(50) COLLATE pg_catalog."default" NOT NULL,
    -- Inherited from table public.funcionario: usuariopessoa character varying(50) COLLATE pg_catalog."default",
    -- Inherited from table public.funcionario: senhapessoa character varying(50) COLLATE pg_catalog."default",
    -- Inherited from table public.funcionario: nivelpessoa character varying(20) COLLATE pg_catalog."default" NOT NULL,
    -- Inherited from table public.funcionario: situacaopessoa boolean,
    -- Inherited from table public.funcionario: emailpessoa character varying(50) COLLATE pg_catalog."default" NOT NULL,
    -- Inherited from table public.funcionario: telefonepessoa character varying(25) COLLATE pg_catalog."default" NOT NULL,
    -- Inherited from table public.funcionario: cpfpf character varying(50) COLLATE pg_catalog."default" NOT NULL,
    -- Inherited from table public.funcionario: datanascimentopf date NOT NULL,
    -- Inherited from table public.funcionario: numero integer NOT NULL,
    -- Inherited from table public.funcionario: complemento character varying(50) COLLATE pg_catalog."default" NOT NULL,
    -- Inherited from table public.funcionario: cep character varying(100) COLLATE pg_catalog."default",
    -- Inherited from table public.funcionario: matriculafuncionario character varying(30) COLLATE pg_catalog."default" NOT NULL,
    -- Inherited from table public.funcionario: tipofuncionario tipo_funcionario NOT NULL,
    -- Inherited from table public.funcionario: cargofuncionario character varying(60) COLLATE pg_catalog."default",
    -- Inherited from table public.funcionario: datainiciofuncionario date,
    -- Inherited from table public.funcionario: datafimfuncionario date,
    valorpordia numeric(12,2) NOT NULL DEFAULT 0,
    CONSTRAINT funcionariodiarista_pk PRIMARY KEY (idpessoa)
)
    INHERITS (public.funcionario)

TABLESPACE pg_default;

ALTER TABLE IF EXISTS public.funcionariodiarista
    OWNER to postgres;
/****** empreita***********************/
-- Table: public.funcionarioempreita

-- DROP TABLE IF EXISTS public.funcionarioempreita;

/****** empreita*****/
CREATE TABLE IF NOT EXISTS public.funcionarioempreita
(
    -- Inherited from table public.funcionario: idpessoa integer NOT NULL DEFAULT nextval('pessoa_idpessoa_seq'::regclass),
    -- Inherited from table public.funcionario: nomepessoa character varying(50) COLLATE pg_catalog."default" NOT NULL,
    -- Inherited from table public.funcionario: usuariopessoa character varying(50) COLLATE pg_catalog."default",
    -- Inherited from table public.funcionario: senhapessoa character varying(50) COLLATE pg_catalog."default",
    -- Inherited from table public.funcionario: nivelpessoa character varying(20) COLLATE pg_catalog."default" NOT NULL,
    -- Inherited from table public.funcionario: situacaopessoa boolean,
    -- Inherited from table public.funcionario: emailpessoa character varying(50) COLLATE pg_catalog."default" NOT NULL,
    -- Inherited from table public.funcionario: telefonepessoa character varying(25) COLLATE pg_catalog."default" NOT NULL,
    -- Inherited from table public.funcionario: cpfpf character varying(50) COLLATE pg_catalog."default" NOT NULL,
    -- Inherited from table public.funcionario: datanascimentopf date NOT NULL,
    -- Inherited from table public.funcionario: numero integer NOT NULL,
    -- Inherited from table public.funcionario: complemento character varying(50) COLLATE pg_catalog."default" NOT NULL,
    -- Inherited from table public.funcionario: cep character varying(100) COLLATE pg_catalog."default",
    -- Inherited from table public.funcionario: matriculafuncionario character varying(30) COLLATE pg_catalog."default" NOT NULL,
    -- Inherited from table public.funcionario: tipofuncionario tipo_funcionario NOT NULL,
    -- Inherited from table public.funcionario: cargofuncionario character varying(60) COLLATE pg_catalog."default",
    -- Inherited from table public.funcionario: datainiciofuncionario date,
    -- Inherited from table public.funcionario: datafimfuncionario date,
    valorfixoacordado numeric(12,2) NOT NULL DEFAULT 0,
    CONSTRAINT funcionarioempreita_pk PRIMARY KEY (idpessoa)
)
    INHERITS (public.funcionario)

TABLESPACE pg_default;

ALTER TABLE IF EXISTS public.funcionarioempreita
    OWNER to postgres;

-- Table: public.funcionarioproducao

-- DROP TABLE IF EXISTS public.funcionarioproducao;

CREATE TABLE IF NOT EXISTS public.funcionarioproducao
(
    -- Inherited from table public.funcionario: idpessoa integer NOT NULL DEFAULT nextval('pessoa_idpessoa_seq'::regclass),
    -- Inherited from table public.funcionario: nomepessoa character varying(50) COLLATE pg_catalog."default" NOT NULL,
    -- Inherited from table public.funcionario: usuariopessoa character varying(50) COLLATE pg_catalog."default",
    -- Inherited from table public.funcionario: senhapessoa character varying(50) COLLATE pg_catalog."default",
    -- Inherited from table public.funcionario: nivelpessoa character varying(20) COLLATE pg_catalog."default" NOT NULL,
    -- Inherited from table public.funcionario: situacaopessoa boolean,
    -- Inherited from table public.funcionario: emailpessoa character varying(50) COLLATE pg_catalog."default" NOT NULL,
    -- Inherited from table public.funcionario: telefonepessoa character varying(25) COLLATE pg_catalog."default" NOT NULL,
    -- Inherited from table public.funcionario: cpfpf character varying(50) COLLATE pg_catalog."default" NOT NULL,
    -- Inherited from table public.funcionario: datanascimentopf date NOT NULL,
    -- Inherited from table public.funcionario: numero integer NOT NULL,
    -- Inherited from table public.funcionario: complemento character varying(50) COLLATE pg_catalog."default" NOT NULL,
    -- Inherited from table public.funcionario: cep character varying(100) COLLATE pg_catalog."default",
    -- Inherited from table public.funcionario: matriculafuncionario character varying(30) COLLATE pg_catalog."default" NOT NULL,
    -- Inherited from table public.funcionario: tipofuncionario tipo_funcionario NOT NULL,
    -- Inherited from table public.funcionario: cargofuncionario character varying(60) COLLATE pg_catalog."default",
    -- Inherited from table public.funcionario: datainiciofuncionario date,
    -- Inherited from table public.funcionario: datafimfuncionario date,
    valorporunidade numeric(12,2) NOT NULL DEFAULT 0,
    CONSTRAINT funcionarioproducao_pk PRIMARY KEY (idpessoa)
)
    INHERITS (public.funcionario)

TABLESPACE pg_default;

ALTER TABLE IF EXISTS public.funcionarioproducao
    OWNER to postgres;
