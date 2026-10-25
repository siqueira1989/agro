-- Etapa "Processamento" do Diário de Campo (embalagem/beneficiamento da colheita).
--
-- Será acionada pelo Módulo Colheita e Estoque (modal "Processar"), que cria um diário com este
-- tipo de atividade já preenchido. No Processamento a mão de obra segue o mesmo esquema de
-- horas (CLT/Diarista/Empreita) e os funcionários de Produção com cargo Embalador entram em
-- regime PRODUCAO (pagos por caixa) — ver ControllerDiarioCampo#validarRegimeCaixa.
-- Não gera lote de colheita (DiarioCampoDAO.criarLoteColheitaSeAplicavel só olha "Colheita").
INSERT INTO diario_tipo_atividade (nome, ativo)
SELECT 'Processamento', true
WHERE NOT EXISTS (SELECT 1 FROM diario_tipo_atividade WHERE lower(nome) = 'processamento');
