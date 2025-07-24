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