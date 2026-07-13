-- =====================================================================
-- AGRO TECH ONE — BANCO DE DADOS COMPLETO (schema consolidado)
--
-- Arquivo único com TODAS as alterações/versões já aplicadas ao banco.
-- Reflete o estado atual (todas as migrações V2..V12 aplicadas e o
-- módulo Talão removido). Serve para recriar o banco do zero em outro PC.
--
-- Como usar (PostgreSQL):
--   createdb agro
--   psql -U postgres -d agro -f agro_banco_completo.sql
--
-- ---------------------------------------------------------------------
-- HISTÓRICO DE VERSÕES (migrações em src/main/resources/db/migration)
--   V1  (base)  BancoAgro.sql — tabelas iniciais (pessoa, funcionário,
--               parceiro, produto, alimento, área de produção, etc.)
--   V2  triggers de integridade de funcionário (herança sem FK)
--   V3  CLT rural (atividade, jornada, ponto eletrônico, fechamento)
--   V4  vínculo empregatício (múltiplos períodos por pessoa)
--   V5  id_vinculo nas tabelas operacionais
--   V6  lançamento de empreita (dia + valor)
--   V7  produção por caixas + histórico de preços (preco_caixa)
--   V8  CLT multi-modo (valor_producao/valor_empreita no fechamento)
--   V9  pagamentos (forma PIX/transferência + dados bancários)
--   V10 tipo_parceiro (Parceiro/Fornecedor/Insumo) + site opcional
--   V11 estoque de insumos (insumo + estoque_movimento)
--   V12 remoção do módulo Talão (duplicava a Área de Produção)
--
-- Observação: este arquivo é o SCHEMA (estrutura). Não inclui dados.
-- Gerado a partir do banco vivo via pg_dump --schema-only.
-- =====================================================================


--
-- PostgreSQL database dump
--

-- Dumped from database version 17.4
-- Dumped by pg_dump version 17.4

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: tipo_atividade_rural; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.tipo_atividade_rural AS ENUM (
    'LAVOURA',
    'PECUARIA'
);


--
-- Name: tipo_f; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.tipo_f AS ENUM (
    'CLT',
    'DIARISTA',
    'PRODUCAO',
    'EMPREITA'
);


--
-- Name: tipo_funcionario; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.tipo_funcionario AS ENUM (
    'CLT',
    'DIARISTA',
    'EMPREITA',
    'PRODUCAO'
);


--
-- Name: trg_cascade_delete_funcionario(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.trg_cascade_delete_funcionario() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    DELETE FROM ponto_eletronico       WHERE idpessoa      = OLD.idpessoa;
    DELETE FROM falta_funcionario      WHERE idpessoa      = OLD.idpessoa;
    DELETE FROM vale_funcionario       WHERE idpessoa      = OLD.idpessoa;
    DELETE FROM fechamento_folha_ponto WHERE idpessoa      = OLD.idpessoa;
    DELETE FROM folha_pagamento        WHERE idfuncionario = OLD.idpessoa;
    DELETE FROM registroponto          WHERE idfuncionario = OLD.idpessoa;
    DELETE FROM lancamento_empreita    WHERE idpessoa      = OLD.idpessoa;
    DELETE FROM producao_caixa         WHERE idpessoa      = OLD.idpessoa;
    DELETE FROM preco_caixa            WHERE idpessoa      = OLD.idpessoa;
    DELETE FROM pagamento              WHERE idpessoa      = OLD.idpessoa;
    DELETE FROM vinculo_empregaticio   WHERE idpessoa      = OLD.idpessoa;
    RETURN OLD;
END;
$$;


--
-- Name: trg_valida_funcionario_existe(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.trg_valida_funcionario_existe() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
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
$$;


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: produto; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.produto (
    idproduto integer NOT NULL,
    nomeproduto character varying(50) NOT NULL,
    quantidadeproduto numeric,
    valorunitarioproduto numeric,
    valorvendaproduto numeric,
    situacaoproduto boolean,
    tipoproduto character varying(50) NOT NULL
);


--
-- Name: alimento; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.alimento (
    variedadealimento character varying(50) NOT NULL
)
INHERITS (public.produto);


--
-- Name: alimentoclassificacao; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.alimentoclassificacao (
    idalimentoclassificacao integer NOT NULL,
    idproduto integer,
    idclassificacao integer
);


--
-- Name: alimentoclassificacao_idalimentoclassificacao_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.alimentoclassificacao_idalimentoclassificacao_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: alimentoclassificacao_idalimentoclassificacao_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.alimentoclassificacao_idalimentoclassificacao_seq OWNED BY public.alimentoclassificacao.idalimentoclassificacao;


--
-- Name: areaproducao; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.areaproducao (
    idareaproducao integer NOT NULL,
    propriedadeareaproducao character varying(50) NOT NULL,
    proprietarioareaproducao character varying(50) NOT NULL,
    quantidadetotalplantasareaproducao integer NOT NULL,
    siglasareaproducao character varying(4) NOT NULL,
    cep character varying(10) NOT NULL,
    complemento character varying(50) NOT NULL,
    numero integer NOT NULL
);


--
-- Name: areaproducao_idareaproducao_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.areaproducao_idareaproducao_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: areaproducao_idareaproducao_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.areaproducao_idareaproducao_seq OWNED BY public.areaproducao.idareaproducao;


--
-- Name: classificacao; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.classificacao (
    idclassificacao integer NOT NULL,
    classificacao character varying(50) NOT NULL
);


--
-- Name: classificacao_idclassificacao_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.classificacao_idclassificacao_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: classificacao_idclassificacao_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.classificacao_idclassificacao_seq OWNED BY public.classificacao.idclassificacao;


--
-- Name: despesascusto; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.despesascusto (
    iddespesascusto integer NOT NULL,
    despesascusto character varying(50) NOT NULL,
    unidadedespesascustos character varying(2) NOT NULL,
    valordespesascustos numeric(10,2) NOT NULL,
    tipodespesascustos character varying(20) NOT NULL
);


--
-- Name: despesascusto_iddespesascusto_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.despesascusto_iddespesascusto_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: despesascusto_iddespesascusto_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.despesascusto_iddespesascusto_seq OWNED BY public.despesascusto.iddespesascusto;


--
-- Name: estoque_movimento; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.estoque_movimento (
    idmov integer NOT NULL,
    idinsumo integer NOT NULL,
    tipo character varying(10) NOT NULL,
    quantidade numeric(14,3) NOT NULL,
    preco_unitario numeric(12,2),
    id_parceiro integer,
    data_mov date DEFAULT CURRENT_DATE NOT NULL,
    observacao character varying(200),
    saldo_apos numeric(14,3) NOT NULL,
    criado_em timestamp without time zone DEFAULT now() NOT NULL
);


--
-- Name: estoque_movimento_idmov_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.estoque_movimento_idmov_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: estoque_movimento_idmov_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.estoque_movimento_idmov_seq OWNED BY public.estoque_movimento.idmov;


--
-- Name: falta_funcionario; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.falta_funcionario (
    idfalta integer NOT NULL,
    idpessoa integer NOT NULL,
    datafalta date NOT NULL,
    justificada boolean DEFAULT false NOT NULL,
    motivo character varying(200),
    registrado_em timestamp without time zone DEFAULT now() NOT NULL,
    id_vinculo integer
);


--
-- Name: TABLE falta_funcionario; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.falta_funcionario IS 'Faltas registradas; justificada=true não desconta salário';


--
-- Name: falta_funcionario_idfalta_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.falta_funcionario_idfalta_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: falta_funcionario_idfalta_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.falta_funcionario_idfalta_seq OWNED BY public.falta_funcionario.idfalta;


--
-- Name: fechamento_folha_ponto; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.fechamento_folha_ponto (
    idfechamento integer NOT NULL,
    idpessoa integer NOT NULL,
    periodo character varying(7) NOT NULL,
    salario_base numeric(10,2) NOT NULL,
    jornada_horas_dia integer DEFAULT 8 NOT NULL,
    dias_uteis integer DEFAULT 0 NOT NULL,
    dias_trabalhados integer DEFAULT 0 NOT NULL,
    faltas_injustificadas integer DEFAULT 0 NOT NULL,
    total_minutos_extras integer DEFAULT 0 NOT NULL,
    valor_horas_extras numeric(10,2) DEFAULT 0.00 NOT NULL,
    desconto_faltas numeric(10,2) DEFAULT 0.00 NOT NULL,
    total_vales numeric(10,2) DEFAULT 0.00 NOT NULL,
    salario_bruto numeric(10,2) DEFAULT 0.00 NOT NULL,
    salario_liquido numeric(10,2) DEFAULT 0.00 NOT NULL,
    fechado_em timestamp without time zone DEFAULT now() NOT NULL,
    desconto_dsr numeric(10,2) DEFAULT 0.00 NOT NULL,
    valor_adicional_noturno numeric(10,2) DEFAULT 0.00 NOT NULL,
    valor_extra_100 numeric(10,2) DEFAULT 0.00 NOT NULL,
    minutos_noturnos integer DEFAULT 0 NOT NULL,
    minutos_extra_100 integer DEFAULT 0 NOT NULL,
    id_vinculo integer,
    valor_producao numeric(10,2) DEFAULT 0.00 NOT NULL,
    valor_empreita numeric(10,2) DEFAULT 0.00 NOT NULL
);


--
-- Name: TABLE fechamento_folha_ponto; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.fechamento_folha_ponto IS 'Resultado financeiro mensal calculado pelo sistema';


--
-- Name: fechamento_folha_ponto_idfechamento_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.fechamento_folha_ponto_idfechamento_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: fechamento_folha_ponto_idfechamento_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.fechamento_folha_ponto_idfechamento_seq OWNED BY public.fechamento_folha_ponto.idfechamento;


--
-- Name: folha_pagamento; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.folha_pagamento (
    idfolha integer NOT NULL,
    idfuncionario integer NOT NULL,
    periodo character varying(7) NOT NULL,
    tipofuncionario public.tipo_funcionario NOT NULL,
    valorcalculado numeric(12,2) DEFAULT 0 NOT NULL,
    datageracao timestamp without time zone DEFAULT now() NOT NULL,
    id_vinculo integer
);


--
-- Name: folha_pagamento_idfolha_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.folha_pagamento_idfolha_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: folha_pagamento_idfolha_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.folha_pagamento_idfolha_seq OWNED BY public.folha_pagamento.idfolha;


--
-- Name: pessoa; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.pessoa (
    idpessoa integer NOT NULL,
    nomepessoa character varying(50) NOT NULL,
    usuariopessoa character varying(50),
    senhapessoa character varying(50),
    nivelpessoa character varying(20) NOT NULL,
    situacaopessoa boolean,
    emailpessoa character varying(50) NOT NULL,
    telefonepessoa character varying(25) NOT NULL,
    numero integer NOT NULL,
    complemento character varying(50) NOT NULL,
    cep character varying(100)
);


--
-- Name: pessoafisica; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.pessoafisica (
    cpfpf character varying(50) NOT NULL,
    datanascimentopf date NOT NULL
)
INHERITS (public.pessoa);


--
-- Name: funcionario; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.funcionario (
    matriculafuncionario character varying(30) NOT NULL,
    tipofuncionario public.tipo_funcionario NOT NULL,
    cargofuncionario character varying(60),
    datainiciofuncionario date,
    datafimfuncionario date
)
INHERITS (public.pessoafisica);


--
-- Name: funcionarioclt; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.funcionarioclt (
    salariomensal numeric(12,2) DEFAULT 0 NOT NULL,
    valorhoraextra numeric(12,2) DEFAULT 0 NOT NULL,
    statusemprego character varying(20) DEFAULT 'ATIVO'::character varying NOT NULL,
    cargahorariadiaria integer DEFAULT 8 NOT NULL,
    tipo_atividade_rural public.tipo_atividade_rural DEFAULT 'LAVOURA'::public.tipo_atividade_rural NOT NULL
)
INHERITS (public.funcionario);


--
-- Name: funcionariodiarista; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.funcionariodiarista (
    valorpordia numeric(12,2) DEFAULT 0 NOT NULL
)
INHERITS (public.funcionario);


--
-- Name: funcionarioempreita; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.funcionarioempreita (
    valorfixoacordado numeric(12,2) DEFAULT 0 NOT NULL
)
INHERITS (public.funcionario);


--
-- Name: funcionarioproducao; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.funcionarioproducao (
    valorporunidade numeric(12,2) DEFAULT 0 NOT NULL
)
INHERITS (public.funcionario);


--
-- Name: insumo; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.insumo (
    idinsumo integer NOT NULL,
    nome character varying(80) NOT NULL,
    categoria character varying(20) NOT NULL,
    grandeza character varying(15) NOT NULL,
    unidade character varying(5) NOT NULL,
    quantidade_disponivel numeric(14,3) DEFAULT 0 NOT NULL,
    preco_medio numeric(12,2) DEFAULT 0 NOT NULL,
    estoque_minimo numeric(14,3) DEFAULT 0 NOT NULL,
    id_fornecedor integer,
    situacao boolean DEFAULT true NOT NULL,
    criado_em timestamp without time zone DEFAULT now() NOT NULL
);


--
-- Name: insumo_idinsumo_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.insumo_idinsumo_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: insumo_idinsumo_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.insumo_idinsumo_seq OWNED BY public.insumo.idinsumo;


--
-- Name: lancamento_empreita; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.lancamento_empreita (
    idlancamento integer NOT NULL,
    idpessoa integer NOT NULL,
    data_empreita date NOT NULL,
    descricao character varying(200),
    valor numeric(12,2) DEFAULT 0 NOT NULL,
    id_vinculo integer,
    pago boolean DEFAULT false NOT NULL,
    criado_em timestamp without time zone DEFAULT now() NOT NULL
);


--
-- Name: lancamento_empreita_idlancamento_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.lancamento_empreita_idlancamento_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: lancamento_empreita_idlancamento_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.lancamento_empreita_idlancamento_seq OWNED BY public.lancamento_empreita.idlancamento;


--
-- Name: lancamento_producao; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.lancamento_producao (
    idlancamento integer NOT NULL,
    idfuncionario integer NOT NULL,
    periodo character varying(7) NOT NULL,
    quantidadeproduzida integer DEFAULT 0 NOT NULL,
    id_vinculo integer
);


--
-- Name: lancamento_producao_idlancamento_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.lancamento_producao_idlancamento_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: lancamento_producao_idlancamento_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.lancamento_producao_idlancamento_seq OWNED BY public.lancamento_producao.idlancamento;


--
-- Name: pagamento; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.pagamento (
    idpagamento integer NOT NULL,
    idpessoa integer NOT NULL,
    id_vinculo integer,
    periodo character varying(7) NOT NULL,
    tipo_funcionario character varying(20),
    valor numeric(12,2) DEFAULT 0 NOT NULL,
    forma_pagamento character varying(20) NOT NULL,
    banco character varying(60),
    agencia character varying(20),
    conta character varying(30),
    chave_pix character varying(120),
    data_pagamento date DEFAULT CURRENT_DATE NOT NULL,
    observacao character varying(200),
    criado_em timestamp without time zone DEFAULT now() NOT NULL
);


--
-- Name: pagamento_idpagamento_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.pagamento_idpagamento_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: pagamento_idpagamento_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.pagamento_idpagamento_seq OWNED BY public.pagamento.idpagamento;


--
-- Name: pessoacnpj; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.pessoacnpj (
    cnpjpessoacnpj character varying(50) NOT NULL,
    razaosocialpessoacnpj character varying(50) NOT NULL,
    inscricaoestadualpessoacnpj character varying(50) NOT NULL
)
INHERITS (public.pessoa);


--
-- Name: parceiro; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.parceiro (
    siteparceiro character varying(50),
    tipo_parceiro character varying(20)
)
INHERITS (public.pessoacnpj);


--
-- Name: pessoa_idpessoa_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.pessoa_idpessoa_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: pessoa_idpessoa_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.pessoa_idpessoa_seq OWNED BY public.pessoa.idpessoa;


--
-- Name: ponto_eletronico; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.ponto_eletronico (
    idponto integer NOT NULL,
    idpessoa integer NOT NULL,
    dataregistro date NOT NULL,
    entrada1 time without time zone,
    saida1 time without time zone,
    entrada2 time without time zone,
    saida2 time without time zone,
    total_minutos integer DEFAULT 0 NOT NULL,
    extra_minutos integer DEFAULT 0 NOT NULL,
    observacao character varying(200),
    criado_em timestamp without time zone DEFAULT now() NOT NULL,
    minutos_noturnos integer DEFAULT 0 NOT NULL,
    latitude numeric(10,7),
    longitude numeric(10,7),
    ip_origem character varying(45),
    user_agent character varying(255),
    hash_autenticidade character varying(64),
    registrado_por integer,
    atualizado_em timestamp without time zone,
    id_vinculo integer
);


--
-- Name: TABLE ponto_eletronico; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.ponto_eletronico IS 'Registro diário de ponto: entrada1/saida1 (manhã) e entrada2/saida2 (tarde)';


--
-- Name: ponto_eletronico_idponto_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.ponto_eletronico_idponto_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: ponto_eletronico_idponto_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.ponto_eletronico_idponto_seq OWNED BY public.ponto_eletronico.idponto;


--
-- Name: preco_caixa; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.preco_caixa (
    idpreco integer NOT NULL,
    tipo_caixa character varying(30) NOT NULL,
    ano integer NOT NULL,
    idpessoa integer,
    preco numeric(12,2) DEFAULT 0 NOT NULL,
    criado_em timestamp without time zone DEFAULT now() NOT NULL
);


--
-- Name: preco_caixa_idpreco_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.preco_caixa_idpreco_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: preco_caixa_idpreco_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.preco_caixa_idpreco_seq OWNED BY public.preco_caixa.idpreco;


--
-- Name: producao_caixa; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.producao_caixa (
    idproducao integer NOT NULL,
    idpessoa integer NOT NULL,
    data_producao date NOT NULL,
    tipo_caixa character varying(30) NOT NULL,
    quantidade integer DEFAULT 0 NOT NULL,
    preco_unitario numeric(12,2) DEFAULT 0 NOT NULL,
    id_vinculo integer,
    criado_em timestamp without time zone DEFAULT now() NOT NULL,
    pago boolean DEFAULT false NOT NULL
);


--
-- Name: producao_caixa_idproducao_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.producao_caixa_idproducao_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: producao_caixa_idproducao_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.producao_caixa_idproducao_seq OWNED BY public.producao_caixa.idproducao;


--
-- Name: produto_idproduto_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.produto_idproduto_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: produto_idproduto_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.produto_idproduto_seq OWNED BY public.produto.idproduto;


--
-- Name: registroponto; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.registroponto (
    idregistroponto integer NOT NULL,
    idfuncionario integer NOT NULL,
    dataponto date NOT NULL,
    presente boolean DEFAULT true NOT NULL,
    horainicio time without time zone,
    horafim time without time zone,
    horasextras numeric(10,2) DEFAULT 0,
    id_vinculo integer
);


--
-- Name: registroponto_idregistroponto_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.registroponto_idregistroponto_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: registroponto_idregistroponto_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.registroponto_idregistroponto_seq OWNED BY public.registroponto.idregistroponto;


--
-- Name: vale_funcionario; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.vale_funcionario (
    idvale integer NOT NULL,
    idpessoa integer NOT NULL,
    datavale date NOT NULL,
    valor numeric(10,2) NOT NULL,
    descricao character varying(200),
    registrado_em timestamp without time zone DEFAULT now() NOT NULL,
    tipovale character varying(30) DEFAULT 'OUTROS'::character varying NOT NULL,
    statusvale character varying(20) DEFAULT 'PENDENTE'::character varying NOT NULL,
    id_vinculo integer,
    CONSTRAINT vale_funcionario_valor_check CHECK ((valor > (0)::numeric))
);


--
-- Name: TABLE vale_funcionario; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.vale_funcionario IS 'Adiantamentos concedidos ao funcionário no mês';


--
-- Name: vale_funcionario_idvale_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.vale_funcionario_idvale_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: vale_funcionario_idvale_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.vale_funcionario_idvale_seq OWNED BY public.vale_funcionario.idvale;


--
-- Name: vinculo_empregaticio; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.vinculo_empregaticio (
    id_vinculo integer NOT NULL,
    idpessoa integer NOT NULL,
    tipo_funcionario public.tipo_funcionario NOT NULL,
    cargo character varying(60),
    tipo_atividade_rural public.tipo_atividade_rural DEFAULT 'LAVOURA'::public.tipo_atividade_rural NOT NULL,
    data_admissao date,
    data_desligamento date,
    salario_mensal numeric(12,2) DEFAULT 0 NOT NULL,
    valor_hora_extra numeric(12,2) DEFAULT 0 NOT NULL,
    carga_horaria_diaria integer DEFAULT 8 NOT NULL,
    status character varying(20) DEFAULT 'ATIVO'::character varying NOT NULL,
    motivo_desligamento character varying(200),
    criado_em timestamp without time zone DEFAULT now() NOT NULL
);


--
-- Name: vinculo_empregaticio_id_vinculo_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.vinculo_empregaticio_id_vinculo_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: vinculo_empregaticio_id_vinculo_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.vinculo_empregaticio_id_vinculo_seq OWNED BY public.vinculo_empregaticio.id_vinculo;


--
-- Name: alimento idproduto; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.alimento ALTER COLUMN idproduto SET DEFAULT nextval('public.produto_idproduto_seq'::regclass);


--
-- Name: alimentoclassificacao idalimentoclassificacao; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.alimentoclassificacao ALTER COLUMN idalimentoclassificacao SET DEFAULT nextval('public.alimentoclassificacao_idalimentoclassificacao_seq'::regclass);


--
-- Name: areaproducao idareaproducao; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.areaproducao ALTER COLUMN idareaproducao SET DEFAULT nextval('public.areaproducao_idareaproducao_seq'::regclass);


--
-- Name: classificacao idclassificacao; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.classificacao ALTER COLUMN idclassificacao SET DEFAULT nextval('public.classificacao_idclassificacao_seq'::regclass);


--
-- Name: despesascusto iddespesascusto; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.despesascusto ALTER COLUMN iddespesascusto SET DEFAULT nextval('public.despesascusto_iddespesascusto_seq'::regclass);


--
-- Name: estoque_movimento idmov; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estoque_movimento ALTER COLUMN idmov SET DEFAULT nextval('public.estoque_movimento_idmov_seq'::regclass);


--
-- Name: falta_funcionario idfalta; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.falta_funcionario ALTER COLUMN idfalta SET DEFAULT nextval('public.falta_funcionario_idfalta_seq'::regclass);


--
-- Name: fechamento_folha_ponto idfechamento; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fechamento_folha_ponto ALTER COLUMN idfechamento SET DEFAULT nextval('public.fechamento_folha_ponto_idfechamento_seq'::regclass);


--
-- Name: folha_pagamento idfolha; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.folha_pagamento ALTER COLUMN idfolha SET DEFAULT nextval('public.folha_pagamento_idfolha_seq'::regclass);


--
-- Name: funcionario idpessoa; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.funcionario ALTER COLUMN idpessoa SET DEFAULT nextval('public.pessoa_idpessoa_seq'::regclass);


--
-- Name: funcionarioclt idpessoa; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.funcionarioclt ALTER COLUMN idpessoa SET DEFAULT nextval('public.pessoa_idpessoa_seq'::regclass);


--
-- Name: funcionariodiarista idpessoa; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.funcionariodiarista ALTER COLUMN idpessoa SET DEFAULT nextval('public.pessoa_idpessoa_seq'::regclass);


--
-- Name: funcionarioempreita idpessoa; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.funcionarioempreita ALTER COLUMN idpessoa SET DEFAULT nextval('public.pessoa_idpessoa_seq'::regclass);


--
-- Name: funcionarioproducao idpessoa; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.funcionarioproducao ALTER COLUMN idpessoa SET DEFAULT nextval('public.pessoa_idpessoa_seq'::regclass);


--
-- Name: insumo idinsumo; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.insumo ALTER COLUMN idinsumo SET DEFAULT nextval('public.insumo_idinsumo_seq'::regclass);


--
-- Name: lancamento_empreita idlancamento; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.lancamento_empreita ALTER COLUMN idlancamento SET DEFAULT nextval('public.lancamento_empreita_idlancamento_seq'::regclass);


--
-- Name: lancamento_producao idlancamento; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.lancamento_producao ALTER COLUMN idlancamento SET DEFAULT nextval('public.lancamento_producao_idlancamento_seq'::regclass);


--
-- Name: pagamento idpagamento; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pagamento ALTER COLUMN idpagamento SET DEFAULT nextval('public.pagamento_idpagamento_seq'::regclass);


--
-- Name: parceiro idpessoa; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.parceiro ALTER COLUMN idpessoa SET DEFAULT nextval('public.pessoa_idpessoa_seq'::regclass);


--
-- Name: pessoa idpessoa; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pessoa ALTER COLUMN idpessoa SET DEFAULT nextval('public.pessoa_idpessoa_seq'::regclass);


--
-- Name: pessoacnpj idpessoa; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pessoacnpj ALTER COLUMN idpessoa SET DEFAULT nextval('public.pessoa_idpessoa_seq'::regclass);


--
-- Name: pessoafisica idpessoa; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pessoafisica ALTER COLUMN idpessoa SET DEFAULT nextval('public.pessoa_idpessoa_seq'::regclass);


--
-- Name: ponto_eletronico idponto; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ponto_eletronico ALTER COLUMN idponto SET DEFAULT nextval('public.ponto_eletronico_idponto_seq'::regclass);


--
-- Name: preco_caixa idpreco; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.preco_caixa ALTER COLUMN idpreco SET DEFAULT nextval('public.preco_caixa_idpreco_seq'::regclass);


--
-- Name: producao_caixa idproducao; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.producao_caixa ALTER COLUMN idproducao SET DEFAULT nextval('public.producao_caixa_idproducao_seq'::regclass);


--
-- Name: produto idproduto; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.produto ALTER COLUMN idproduto SET DEFAULT nextval('public.produto_idproduto_seq'::regclass);


--
-- Name: registroponto idregistroponto; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.registroponto ALTER COLUMN idregistroponto SET DEFAULT nextval('public.registroponto_idregistroponto_seq'::regclass);


--
-- Name: vale_funcionario idvale; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vale_funcionario ALTER COLUMN idvale SET DEFAULT nextval('public.vale_funcionario_idvale_seq'::regclass);


--
-- Name: vinculo_empregaticio id_vinculo; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vinculo_empregaticio ALTER COLUMN id_vinculo SET DEFAULT nextval('public.vinculo_empregaticio_id_vinculo_seq'::regclass);


--
-- Name: alimento alimento_idproduto_unique; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.alimento
    ADD CONSTRAINT alimento_idproduto_unique UNIQUE (idproduto);


--
-- Name: alimentoclassificacao alimentoclassificacao_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.alimentoclassificacao
    ADD CONSTRAINT alimentoclassificacao_pkey PRIMARY KEY (idalimentoclassificacao);


--
-- Name: areaproducao areaproducao_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.areaproducao
    ADD CONSTRAINT areaproducao_pkey PRIMARY KEY (idareaproducao);


--
-- Name: classificacao classificacao_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.classificacao
    ADD CONSTRAINT classificacao_pkey PRIMARY KEY (idclassificacao);


--
-- Name: despesascusto despesascusto_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.despesascusto
    ADD CONSTRAINT despesascusto_pkey PRIMARY KEY (iddespesascusto);


--
-- Name: estoque_movimento estoque_movimento_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estoque_movimento
    ADD CONSTRAINT estoque_movimento_pkey PRIMARY KEY (idmov);


--
-- Name: falta_funcionario falta_funcionario_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.falta_funcionario
    ADD CONSTRAINT falta_funcionario_pkey PRIMARY KEY (idfalta);


--
-- Name: fechamento_folha_ponto fechamento_folha_ponto_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fechamento_folha_ponto
    ADD CONSTRAINT fechamento_folha_ponto_pkey PRIMARY KEY (idfechamento);


--
-- Name: folha_pagamento folha_pagamento_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.folha_pagamento
    ADD CONSTRAINT folha_pagamento_pkey PRIMARY KEY (idfolha);


--
-- Name: funcionario funcionario_matriculafuncionario_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.funcionario
    ADD CONSTRAINT funcionario_matriculafuncionario_key UNIQUE (matriculafuncionario);


--
-- Name: funcionario funcionario_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.funcionario
    ADD CONSTRAINT funcionario_pk PRIMARY KEY (idpessoa);


--
-- Name: funcionarioclt funcionarioclt_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.funcionarioclt
    ADD CONSTRAINT funcionarioclt_pk PRIMARY KEY (idpessoa);


--
-- Name: funcionariodiarista funcionariodiarista_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.funcionariodiarista
    ADD CONSTRAINT funcionariodiarista_pk PRIMARY KEY (idpessoa);


--
-- Name: funcionarioempreita funcionarioempreita_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.funcionarioempreita
    ADD CONSTRAINT funcionarioempreita_pk PRIMARY KEY (idpessoa);


--
-- Name: funcionarioproducao funcionarioproducao_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.funcionarioproducao
    ADD CONSTRAINT funcionarioproducao_pk PRIMARY KEY (idpessoa);


--
-- Name: insumo insumo_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.insumo
    ADD CONSTRAINT insumo_pkey PRIMARY KEY (idinsumo);


--
-- Name: lancamento_empreita lancamento_empreita_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.lancamento_empreita
    ADD CONSTRAINT lancamento_empreita_pkey PRIMARY KEY (idlancamento);


--
-- Name: lancamento_producao lancamento_producao_idfuncionario_periodo_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.lancamento_producao
    ADD CONSTRAINT lancamento_producao_idfuncionario_periodo_key UNIQUE (idfuncionario, periodo);


--
-- Name: lancamento_producao lancamento_producao_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.lancamento_producao
    ADD CONSTRAINT lancamento_producao_pkey PRIMARY KEY (idlancamento);


--
-- Name: pagamento pagamento_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pagamento
    ADD CONSTRAINT pagamento_pkey PRIMARY KEY (idpagamento);


--
-- Name: pessoa pessoa_emailpessoa_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pessoa
    ADD CONSTRAINT pessoa_emailpessoa_key UNIQUE (emailpessoa);


--
-- Name: pessoa pessoa_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pessoa
    ADD CONSTRAINT pessoa_pkey PRIMARY KEY (idpessoa);


--
-- Name: pessoacnpj pessoacnpj_cnpjpessoacnpj_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pessoacnpj
    ADD CONSTRAINT pessoacnpj_cnpjpessoacnpj_key UNIQUE (cnpjpessoacnpj);


--
-- Name: pessoacnpj pessoacnpj_inscricaoestadualpessoacnpj_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pessoacnpj
    ADD CONSTRAINT pessoacnpj_inscricaoestadualpessoacnpj_key UNIQUE (inscricaoestadualpessoacnpj);


--
-- Name: pessoacnpj pessoacnpj_razaosocialpessoacnpj_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pessoacnpj
    ADD CONSTRAINT pessoacnpj_razaosocialpessoacnpj_key UNIQUE (razaosocialpessoacnpj);


--
-- Name: pessoafisica pessoafisica_cpfpf_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pessoafisica
    ADD CONSTRAINT pessoafisica_cpfpf_key UNIQUE (cpfpf);


--
-- Name: ponto_eletronico ponto_eletronico_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ponto_eletronico
    ADD CONSTRAINT ponto_eletronico_pkey PRIMARY KEY (idponto);


--
-- Name: preco_caixa preco_caixa_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.preco_caixa
    ADD CONSTRAINT preco_caixa_pkey PRIMARY KEY (idpreco);


--
-- Name: producao_caixa producao_caixa_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.producao_caixa
    ADD CONSTRAINT producao_caixa_pkey PRIMARY KEY (idproducao);


--
-- Name: produto produto_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.produto
    ADD CONSTRAINT produto_pkey PRIMARY KEY (idproduto);


--
-- Name: registroponto registroponto_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.registroponto
    ADD CONSTRAINT registroponto_pkey PRIMARY KEY (idregistroponto);


--
-- Name: falta_funcionario uk_falta_funcionario_dia; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.falta_funcionario
    ADD CONSTRAINT uk_falta_funcionario_dia UNIQUE (idpessoa, datafalta);


--
-- Name: fechamento_folha_ponto uk_fechamento_func_periodo; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fechamento_folha_ponto
    ADD CONSTRAINT uk_fechamento_func_periodo UNIQUE (idpessoa, periodo);


--
-- Name: ponto_eletronico uk_ponto_funcionario_dia; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ponto_eletronico
    ADD CONSTRAINT uk_ponto_funcionario_dia UNIQUE (idpessoa, dataregistro);


--
-- Name: vale_funcionario vale_funcionario_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vale_funcionario
    ADD CONSTRAINT vale_funcionario_pkey PRIMARY KEY (idvale);


--
-- Name: vinculo_empregaticio vinculo_empregaticio_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vinculo_empregaticio
    ADD CONSTRAINT vinculo_empregaticio_pkey PRIMARY KEY (id_vinculo);


--
-- Name: idx_estoque_mov_insumo; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_estoque_mov_insumo ON public.estoque_movimento USING btree (idinsumo, data_mov);


--
-- Name: idx_falta_funcionario; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_falta_funcionario ON public.falta_funcionario USING btree (idpessoa);


--
-- Name: idx_fechamento_vinculo; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_fechamento_vinculo ON public.fechamento_folha_ponto USING btree (id_vinculo);


--
-- Name: idx_folha_funcionario; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_folha_funcionario ON public.folha_pagamento USING btree (idfuncionario);


--
-- Name: idx_folha_periodo; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_folha_periodo ON public.folha_pagamento USING btree (periodo);


--
-- Name: idx_lanc_empreita_pessoa; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_lanc_empreita_pessoa ON public.lancamento_empreita USING btree (idpessoa);


--
-- Name: idx_pagamento_pessoa; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_pagamento_pessoa ON public.pagamento USING btree (idpessoa, periodo);


--
-- Name: idx_ponto_data; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ponto_data ON public.ponto_eletronico USING btree (dataregistro);


--
-- Name: idx_ponto_funcionario; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ponto_funcionario ON public.ponto_eletronico USING btree (idpessoa);


--
-- Name: idx_ponto_vinculo; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ponto_vinculo ON public.ponto_eletronico USING btree (id_vinculo);


--
-- Name: idx_producao_caixa_pessoa; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_producao_caixa_pessoa ON public.producao_caixa USING btree (idpessoa);


--
-- Name: idx_regponto_func_data; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_regponto_func_data ON public.registroponto USING btree (idfuncionario, dataponto);


--
-- Name: idx_vale_funcionario; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_vale_funcionario ON public.vale_funcionario USING btree (idpessoa);


--
-- Name: idx_vale_idpessoa_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_vale_idpessoa_status ON public.vale_funcionario USING btree (idpessoa, statusvale);


--
-- Name: idx_vinculo_pessoa; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_vinculo_pessoa ON public.vinculo_empregaticio USING btree (idpessoa);


--
-- Name: uk_preco_func; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uk_preco_func ON public.preco_caixa USING btree (tipo_caixa, ano, idpessoa) WHERE (idpessoa IS NOT NULL);


--
-- Name: uk_preco_geral; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uk_preco_geral ON public.preco_caixa USING btree (tipo_caixa, ano) WHERE (idpessoa IS NULL);


--
-- Name: uk_vinculo_ativo; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uk_vinculo_ativo ON public.vinculo_empregaticio USING btree (idpessoa) WHERE (data_desligamento IS NULL);


--
-- Name: funcionario casc_del_func; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER casc_del_func AFTER DELETE ON public.funcionario FOR EACH ROW EXECUTE FUNCTION public.trg_cascade_delete_funcionario();


--
-- Name: funcionarioclt casc_del_func; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER casc_del_func AFTER DELETE ON public.funcionarioclt FOR EACH ROW EXECUTE FUNCTION public.trg_cascade_delete_funcionario();


--
-- Name: funcionariodiarista casc_del_func; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER casc_del_func AFTER DELETE ON public.funcionariodiarista FOR EACH ROW EXECUTE FUNCTION public.trg_cascade_delete_funcionario();


--
-- Name: funcionarioempreita casc_del_func; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER casc_del_func AFTER DELETE ON public.funcionarioempreita FOR EACH ROW EXECUTE FUNCTION public.trg_cascade_delete_funcionario();


--
-- Name: funcionarioproducao casc_del_func; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER casc_del_func AFTER DELETE ON public.funcionarioproducao FOR EACH ROW EXECUTE FUNCTION public.trg_cascade_delete_funcionario();


--
-- Name: falta_funcionario chk_func_exist; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER chk_func_exist BEFORE INSERT OR UPDATE ON public.falta_funcionario FOR EACH ROW EXECUTE FUNCTION public.trg_valida_funcionario_existe('idpessoa');


--
-- Name: fechamento_folha_ponto chk_func_exist; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER chk_func_exist BEFORE INSERT OR UPDATE ON public.fechamento_folha_ponto FOR EACH ROW EXECUTE FUNCTION public.trg_valida_funcionario_existe('idpessoa');


--
-- Name: folha_pagamento chk_func_exist; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER chk_func_exist BEFORE INSERT OR UPDATE ON public.folha_pagamento FOR EACH ROW EXECUTE FUNCTION public.trg_valida_funcionario_existe('idfuncionario');


--
-- Name: lancamento_empreita chk_func_exist; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER chk_func_exist BEFORE INSERT OR UPDATE ON public.lancamento_empreita FOR EACH ROW EXECUTE FUNCTION public.trg_valida_funcionario_existe('idpessoa');


--
-- Name: pagamento chk_func_exist; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER chk_func_exist BEFORE INSERT OR UPDATE ON public.pagamento FOR EACH ROW EXECUTE FUNCTION public.trg_valida_funcionario_existe('idpessoa');


--
-- Name: ponto_eletronico chk_func_exist; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER chk_func_exist BEFORE INSERT OR UPDATE ON public.ponto_eletronico FOR EACH ROW EXECUTE FUNCTION public.trg_valida_funcionario_existe('idpessoa');


--
-- Name: producao_caixa chk_func_exist; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER chk_func_exist BEFORE INSERT OR UPDATE ON public.producao_caixa FOR EACH ROW EXECUTE FUNCTION public.trg_valida_funcionario_existe('idpessoa');


--
-- Name: registroponto chk_func_exist; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER chk_func_exist BEFORE INSERT OR UPDATE ON public.registroponto FOR EACH ROW EXECUTE FUNCTION public.trg_valida_funcionario_existe('idfuncionario');


--
-- Name: vale_funcionario chk_func_exist; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER chk_func_exist BEFORE INSERT OR UPDATE ON public.vale_funcionario FOR EACH ROW EXECUTE FUNCTION public.trg_valida_funcionario_existe('idpessoa');


--
-- Name: vinculo_empregaticio chk_func_exist; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER chk_func_exist BEFORE INSERT OR UPDATE ON public.vinculo_empregaticio FOR EACH ROW EXECUTE FUNCTION public.trg_valida_funcionario_existe('idpessoa');


--
-- Name: estoque_movimento estoque_movimento_idinsumo_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.estoque_movimento
    ADD CONSTRAINT estoque_movimento_idinsumo_fkey FOREIGN KEY (idinsumo) REFERENCES public.insumo(idinsumo) ON DELETE CASCADE;


--
-- Name: alimentoclassificacao fk_alimento; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.alimentoclassificacao
    ADD CONSTRAINT fk_alimento FOREIGN KEY (idproduto) REFERENCES public.alimento(idproduto);


--
-- Name: alimentoclassificacao fk_classificacao; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.alimentoclassificacao
    ADD CONSTRAINT fk_classificacao FOREIGN KEY (idclassificacao) REFERENCES public.classificacao(idclassificacao);


--
-- Name: lancamento_producao lancamento_producao_idfuncionario_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.lancamento_producao
    ADD CONSTRAINT lancamento_producao_idfuncionario_fkey FOREIGN KEY (idfuncionario) REFERENCES public.funcionarioproducao(idpessoa) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

