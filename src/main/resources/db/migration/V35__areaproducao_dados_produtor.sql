-- Dados do produtor/endereço da área, lidos pelo Romaneio (bloco "Dados da propriedade (origem)").
ALTER TABLE areaproducao
    ADD COLUMN IF NOT EXISTS cpf_cnpj_produtor  varchar(18),
    ADD COLUMN IF NOT EXISTS inscricao_estadual varchar(20),
    ADD COLUMN IF NOT EXISTS logradouro         varchar(120),
    ADD COLUMN IF NOT EXISTS bairro             varchar(60),
    ADD COLUMN IF NOT EXISTS municipio          varchar(60),
    ADD COLUMN IF NOT EXISTS uf                 char(2);
