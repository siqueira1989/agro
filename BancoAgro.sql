/************************************************************* Banco de Dados  do Sistema ********************************************************************************/


/*Tabela Despesas e Custos*/

create table despesascusto(

	iddespesascusto serial primary key,
	despesascusto varchar(50) not null,
	unidadedespesascustos varchar(2) not null,
	valordespesascustos decimal(10,2) not  null,
	tipodespesascustos varchar(20) not null
	
	)

/*Tabela Classificacao*/

create	table	classificacao(

	idclassificacao serial primary key,
	classificacao varchar (50)not null
	)

/*Tabela Pessoa*/

create table pessoa(
	idpessoa serial primary key,
	nomepessoa varchar(50) not null,
	usuariopessoa varchar(50) not null,
	senhapessoa varchar(50) not null,
	nivelpessoa varchar(20) not null,
	situacaopessoa BOOLEAN,
	Cep varchar(15) not null,
	emailpessoa varchar(50) not null unique,
	telefonepessoa varchar(25) not null,
	numeropessoa int,
	complementopessoa varchar(50) not null 
	
);

/*tabela pessoa fisica*/

CREATE TABLE pessoafisica (
  cpfPf varchar(50) not null unique,
  dataNascimentoPf date not null
) INHERITS (pessoa);

/*tabela pessoa cnpjs*/

CREATE TABLE pessoacnpj (
 cnpjPessoaCnpj varchar(50) not null unique,
 razaoSocialPessoaCnpj varchar(50) not null unique,
inscricaoEstadualPessoaCnpj varchar(50) not null unique
) INHERITS (pessoa);

/*tabela pessoa parceiro*/

CREATE TABLE parceiro (
siteparceiro varchar(50) not null unique
 
) INHERITS (pessoacnpj);

/*Produto em geral*/

create table produto(
 idproduto serial primary key,
 nomeproduto varchar(50) not null,
 quantidadeproduto decimal,
 valorunitarioproduto decimal,
 valorvendaproduto decimal,
 situacaoproduto boolean,
 tipoproduto varchar(50) not null
)
 create table alimento(
    variedadealimento varchar(50) not null
 )INHERITS (produto);
 
ALTER TABLE alimento
ADD CONSTRAINT alimento_idproduto_unique UNIQUE (idproduto);

create table alimentoclassificacao(
	idalimentoclassificao serial primary key,
	idproduto int,
	idclassificacao int,
	CONSTRAINT fk_alimento  FOREIGN KEY (idproduto) REFERENCES alimento(idproduto),
	CONSTRAINT fk_classificacao  FOREIGN KEY (idclassificacao) REFERENCES classificacao(idclassificacao)
)