create table alimento(
 idalimento serial primary key,
 nomealimento varchar(50) not null,
 variedadealimento varchar(50) not null
)

create table alimentoclassificacao(
	idalimentoclassificao serial primary key,
	idalimento int,
	idclassificacao int,
	CONSTRAINT fk_alimento  FOREIGN KEY (idalimento) REFERENCES alimento(idalimento),
	CONSTRAINT fk_classificacao  FOREIGN KEY (idclassificacao) REFERENCES classificacao(idclassificacao)
)