create table areaproducao(
 idareaproducao serial primary key,
 propriedadeareaproducao varchar(50) not null,
 proprietarioareaproducao varchar(50) not null,
 quantidadetotalplantasareaproducao int not null,
 siglasAreaProducao varchar(4) not null,
 cep varchar(10) not null,
 complemento varchar (50) not null,
 numero int not null
)
create table talao(
	idtalao serial primary key,
	descricaotalhao varchar(50) not null,
    idproduto int,
    idareaproducao int,
	quantidadeplantastalao int not null,
	CONSTRAINT fk_alimento  FOREIGN KEY (idproduto) REFERENCES alimento(idproduto),
	CONSTRAINT fk_areaprducao  FOREIGN KEY (idareaproducao) REFERENCES areaproducao(idareaproducao)
	)

	create table talaofinanceiro(
   idtalaofinanceiro serial primary key,
   idtalao int  not null,
   safratalaofinanceiro varchar(50) not null,
   iniciosafratalaofinanceiro Date  not null,
   terminosafratalaofinanceiro Date not null,
   custosafratalaofinanceiro Numeric (10,2) not null,
   despesassafratalaofinanceiro Numeric (10,2) not null,
   vendabrutasafratalaofinanceiro Numeric (10,2) not null,
   vendaliquidasafratalaofinanceiro Numeric (10,2) not null,
   	CONSTRAINT fk_talao  FOREIGN KEY (idtalao) REFERENCES talao(idtalao)
)
create table lancamentotalao(
  IdlancaLancamentoDespesasCustos serial primary key,
   iddespesascusto int,
    valorlancaLancamentoDespesasCustos Numeric(10,2),
	CONSTRAINT fk_lancamentodespesa  FOREIGN KEY (iddespesascusto) REFERENCES despesascusto(iddespesascusto)
)
select*from talao
ALTER TABLE talao RENAME descricaotalhao TO descricaotalao