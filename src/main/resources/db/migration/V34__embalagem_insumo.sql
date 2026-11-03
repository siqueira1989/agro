-- Módulo Embalagem (módulo 21) — embalagem vira INSUMO com estoque (decisões do usuário de 02/11).
--
-- Cada embalagem tem um insumo da categoria EMBALAGEM (unidade CX, grandeza UNIDADE) que guarda o
-- saldo (quantidade_disponivel), o preço médio ponderado (preco_medio, atualizado a cada ENTRADA pelo
-- EstoqueMovimentoDAO), o estoque mínimo e o fornecedor. A tabela embalagem continua com o que é
-- específico: código, descrição, peso_kg, alimento, área e preco_embalador.
--
-- O custo da embalagem passa a ser CUSTO DE PRODUÇÃO: o Processar (Colheita) faz a SAÍDA do estoque
-- e lança a linha de insumo (baixado=true) no diário de Processamento.
--
-- preco_compra fica OBSOLETO: o preço de compra é o insumo.preco_medio. A coluna passa a aceitar
-- NULL e não é mais gravada; será removida quando nenhum leitor restar. A base não tinha embalagens
-- quando esta migração foi escrita, então não há dados a migrar. id_insumo fica NULLABLE só para não
-- quebrar a migração numa base que tenha embalagens antigas; o EmbalagemDAO sempre o preenche
-- (e cria o insumo de uma embalagem antiga na primeira edição).
ALTER TABLE embalagem
    ADD COLUMN id_insumo INTEGER REFERENCES insumo(idinsumo),
    ADD CONSTRAINT uk_embalagem_insumo UNIQUE (id_insumo),
    ALTER COLUMN preco_compra DROP NOT NULL,
    DROP CONSTRAINT ck_embalagem_precos;

ALTER TABLE embalagem
    ADD CONSTRAINT ck_embalagem_precos CHECK (preco_embalador > 0 AND (preco_compra IS NULL OR preco_compra > 0));

COMMENT ON COLUMN embalagem.id_insumo IS
  'Insumo (categoria EMBALAGEM, unidade CX) com saldo e preço médio desta embalagem. NULL só em registros anteriores à V34.';
COMMENT ON COLUMN embalagem.preco_compra IS
  'OBSOLETO desde a V34: o preço de compra é insumo.preco_medio (via id_insumo). Não é mais gravado.';
COMMENT ON TABLE embalagem IS
  'Embalagens por alimento + área (módulo Embalagem): preco_embalador (pago por caixa no Processamento); estoque e preço médio no insumo ligado (id_insumo).';
