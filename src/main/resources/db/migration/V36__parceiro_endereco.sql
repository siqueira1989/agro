-- Endereço completo do parceiro (preenchido pelo ViaCEP, editável), lido pelo Romaneio (bloco "Dados do destino").
ALTER TABLE parceiro
    ADD COLUMN IF NOT EXISTS logradouro varchar(120),
    ADD COLUMN IF NOT EXISTS bairro     varchar(60),
    ADD COLUMN IF NOT EXISTS cidade     varchar(60),
    ADD COLUMN IF NOT EXISTS uf         char(2);
