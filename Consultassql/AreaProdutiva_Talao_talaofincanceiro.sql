Talaocreate table areaproducao(
	idareaproducao serial primary key,
	nomeresponsavel varchar(50) not null,
	propriedadeproducao varchar(50) not null,
    cep varchar(50) not null,
    numero int,
    complemento varchar(100),
    SiglasProducao varchar(100)
	/*CONSTRAINT fk_alimento  FOREIGN KEY (idproduto) REFERENCES alimento(idproduto),
	CONSTRAINT fk_classificacao  FOREIGN KEY (idclassificacao) REFERENCES classificacao(idclassificacao)*/
)
create table talao(
	idtalao serial primary key,
	descricaotalhao varchar(50) not null,
    idproduto int,
    Siglatalao varchar(5),
	CONSTRAINT fk_alimento  FOREIGN KEY (idproduto) REFERENCES alimento(idproduto)
	)
create table talaofinanceiro(
   idtalaofinanceiro int,
   idtalao int,
   safratalaofinanceiro,
   iniciosafratalaofinanceiro Date,
   terminosafratalaofinanceiro Date,
   custosafratalaofinanceiro Numeric (10,2),
   despesassafratalaofinanceiro Numeric (10,2),
   vendabrutasafratalaofinanceiro Numeric (10,2),
   vendaliquidasafratalaofinanceiro Numeric (10,2),
   	CONSTRAINT fk_alimento  FOREIGN KEY (idtalao) REFERENCES talao(idtalao)
)