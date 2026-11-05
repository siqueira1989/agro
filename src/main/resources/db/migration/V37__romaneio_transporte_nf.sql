-- Módulo Romaneio — transporte e nota fiscal, impressos no documento do romaneio (bloco 03 e linha da NF
-- do bloco 01 do modelo docs/modelos/Romaneio_Agricola_Moderno.docx). Todos opcionais.
ALTER TABLE romaneio
    ADD COLUMN IF NOT EXISTS nota_fiscal        varchar(20),
    ADD COLUMN IF NOT EXISTS serie_nf           varchar(5),
    ADD COLUMN IF NOT EXISTS transportadora     varchar(100),
    ADD COLUMN IF NOT EXISTS transportadora_doc varchar(18),
    ADD COLUMN IF NOT EXISTS motorista_cpf      varchar(14),
    ADD COLUMN IF NOT EXISTS motorista_cnh      varchar(20),
    ADD COLUMN IF NOT EXISTS placa_carreta      varchar(10),
    ADD COLUMN IF NOT EXISTS uf_veiculo         char(2),
    ADD COLUMN IF NOT EXISTS tipo_veiculo       varchar(30);
