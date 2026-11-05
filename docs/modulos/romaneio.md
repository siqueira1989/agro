# Módulo Romaneio

**Objetivo:** venda da produção processada. As caixas embaladas de um lote de colheita são vendidas a um parceiro comprador, com as despesas de venda e os recebimentos. É onde entram as **receitas** do sistema: venda bruta e líquida.
**Menu:** Produção → Romaneio

## Escopo
| Camada | Arquivo |
|---|---|
| Controller | `Controller/ControllerRomaneio.java`, URL `/ControllerRomaneio` |
| DAO | `Model/Dao/RomaneioDAO.java` |
| Model | `Romaneio.java`, `RomaneioItem.java`, `RomaneioDespesa.java`, `RomaneioPagamento.java` |
| View | `view/admin/romaneio.jsp` (lista, cards, modais "Romaneio Cad.", "Registrar pagamento" e histórico), `view/admin/cadastroRomaneio.jsp` (passo MR1), `view/admin/romaneioDocumento.jsp` (documento para visualizar/imprimir) |
| Documento PDF | `Service/RomaneioDocumentoPdf.java` (OpenPDF que vem com o JasperReports). Modelo de referência: `docs/modelos/Romaneio_Agricola_Moderno.docx`; exemplo gerado: `docs/modelos/Romaneio_exemplo_gerado.pdf` |
| JS | `main.js` → seção `MÓDULO ROMANEIO` (funções `rm*` da lista, `rp*` do pagamento, `cr*` do cadastro) + bloco `romaneio.jsp / cadastroRomaneio.jsp init` |
| Núcleo (autorizado) | `WEB-INF/web.xml` (servlet + mapping), `pagina/menu.jsp` (item "Romaneio" em Produção) |

**GET:** lista (`[?status=]`), `?lotes=1`, `?contexto=1&lote=<idLote>` ou `&id=<idromaneio>`, `?id=<idromaneio>`, `?parceiros=1`, `?pendentes=<idParceiro>`, `?pagamentos=<idromaneio>`, `?pdf=<idromaneio>` (PDF, `Content-Disposition: attachment; filename="Romaneio_<numero>.pdf"`; 404/400 em JSON).
**POST (JSON, `acao`):** `salvar`, `cancelar`, `pagar`, `estornar`. Resposta `{ok, msg, target}` (+ `romaneio` / `idPagamento`). Regra violada ⇒ HTTP 400 com a mensagem; erro técnico ⇒ 500 com o código do `LogUtil`.

## Banco: V33 (`V33__romaneio.sql`)
- `romaneio`: `idromaneio`, `numero` UNIQUE (`AAAA-NNNNNN`, sequência por ano do `data_emissao`, gerada com `pg_advisory_xact_lock`), `idlote` → `colheita_lote`, `id_area`, `id_quadra`, `id_alimento`, `id_safra` (resolvida por `SafraDAO.resolverSafra(talhão, data da colheita do lote)`; sem safra o salvamento é recusado), `id_parceiro` (sem FK, por causa da herança de pessoa), `data_emissao`, `motorista`, `placa` (maiúsculas, sem espaços), `status` CHECK (`ABERTO|PENDENTE|FINALIZADO|CANCELADO`), `total_caixas`, `valor_bruto`, `total_despesas`, `valor_liquido`, `valor_pago` (padrão 0), `observacao`, `criado_em`, `atualizado_em`.
- `romaneio_item`: `idromaneio` (cascade), `idclassificacao`, `idembalagem`, `caixas` > 0, `preco_unitario` (NULL até informar), `valor`. UNIQUE (romaneio, classificação, embalagem).
- `romaneio_despesa`: `iddespesascusto`, `descricao`, `forma_calculo` (`FIXO|PERCENTUAL_VENDA|POR_CAIXA`; `EMBALAGEM` continua no CHECK só por compatibilidade, porque a linha automática de embalagem não é mais gerada), `percentual`, `quantidade`, `valor_unitario`, `valor`. É snapshot: mudar o cadastro de despesas não altera romaneios gravados.
- `romaneio_pagamento`: `id_parceiro`, `data_pagamento`, `forma` (`DINHEIRO|PIX|CHEQUE_PROPRIO|CHEQUE_TERCEIRO`), `valor` > 0, `banco`, `numero_cheque`, `emitente`, `bom_para`, `observacao`, `criado_em`.
- `romaneio_pagamento_item`: `idpagamento` (cascade), `idromaneio`, `valor` > 0.
- **V37** (`V37__romaneio_transporte_nf.sql`), em `romaneio`, todos opcionais: `nota_fiscal` (20), `serie_nf` (5), `transportadora` (100), `transportadora_doc` (18, gravado formatado como CNPJ ou CPF), `motorista_cpf` (14, gravado `000.000.000-00`), `motorista_cnh` (20), `placa_carreta` (10), `uf_veiculo` char(2), `tipo_veiculo` (30).

### Contrato para a Safra
Somar por safra e por talhão `valor_bruto`, `total_despesas` e `valor_liquido` de `romaneio` com `status <> 'CANCELADO'`, usando `romaneio.id_safra` e `romaneio.id_quadra`. Os quatro valores (`valor_bruto`, `total_despesas`, `valor_liquido`, `valor_pago`) são recalculados e gravados pelo `RomaneioDAO` em todo `salvar`, `pagar` e `estornar`. Nenhum outro caminho grava esses campos.
Observação: romaneios ABERTO entram com a venda parcial, porque os itens sem preço valem 0. Se a Safra quiser só a receita "confirmada", deve filtrar `status IN ('PENDENTE','FINALIZADO')`.

## Regras
1. **Lotes disponíveis** (`?lotes=1`, botão "Romaneio Cad."): `colheita_lote.status = 'PROCESSADO'`, com caixas embaladas (processamentos com `idembalagem`) e saldo > 0 em alguma embalagem. O MR1 aceita lotes PROCESSADO ou FINALIZADO.
2. **Caixas:** por embalagem, a soma dos itens dos romaneios não cancelados do lote não pode passar das caixas embaladas. O lote é travado com `FOR UPDATE` no `salvar`, e na edição o próprio romaneio é descontado. A tela mostra Embaladas / Outros romaneios / Este / Saldo ao vivo e avisa quando o lote fica totalmente romaneado.
3. **Itens:** a classificação precisa ser do alimento do lote (`alimentoclassificacao`) e a embalagem precisa ter caixas no lote. A data de emissão não pode ser anterior à colheita.
4. **Valores (servidor):** item = caixas × preço (2 casas). Bruto = Σ itens. Despesas:
   - `PERCENTUAL_VENDA` = bruto × % / 100;
   - `POR_CAIXA` = total de caixas × R$/cx;
   - `FIXO` = valor digitado.

   Líquido = bruto − despesas. As despesas iniciais vêm de `DespesasCustosDAO.listarRomaneio` (Comissão 18%, Frete por embalagem, Vale). O usuário ajusta %, R$/cx ou o valor, remove linhas ou inclui outra despesa do cadastro (`listAll`, categoria GERAL).

   **Embalagem não é despesa de venda** (decisão do usuário, 02/11): a embalagem virou insumo com estoque, e o custo dela é **custo de produção**, lançado no diário de Processamento pelo preço médio do estoque (Módulo Colheita) e somado na Safra. O Romaneio não gera mais a linha "Custo de embalagem"; se um romaneio antigo tiver uma linha `EMBALAGEM`, ela é descartada no próximo salvar (recálculo).
5. **Situação:** ABERTO (algum item sem preço) → PENDENTE (todos com preço). Com todos os preços, o líquido precisa ser > 0. FINALIZADO quando `valor_pago ≥ valor_liquido`. FINALIZADO e CANCELADO não podem ser editados (abrem só para leitura).
6. **Edição com pagamento:** todos os itens precisam continuar com preço, o líquido não pode ficar menor que o já pago e o parceiro não pode ser trocado. Se o líquido ficar igual ao pago, o romaneio vira FINALIZADO.
7. **Pagamento em grupo** (`pagar`): parceiro, data, forma e valor. Cheque exige banco e número; cheque de terceiro exige o emitente; "bom para" ≥ data. A tela distribui o valor do romaneio mais antigo para o mais novo, até o saldo de cada um, e as partes são editáveis. No servidor: cada parte > 0 e ≤ saldo, o romaneio precisa ser do parceiro e estar PENDENTE, e Σ partes = valor.
8. **Estorno** (`estornar`): exclui o pagamento inteiro (todas as partes) e recalcula `valor_pago`/situação de cada romaneio afetado (FINALIZADO → PENDENTE). O modal avisa quando o pagamento foi dividido com outros romaneios.
9. **Cancelar:** só ABERTO/PENDENTE sem pagamento. Libera as caixas do lote.
10. **Transporte e nota fiscal** (bloco no MR1, V37): todos opcionais.
    - CPF do motorista com 11 dígitos; CNPJ/CPF da transportadora com 14 ou 11 dígitos.
    - UF com 2 letras.
    - Placa do veículo e da carreta no formato antigo (`ABC1234`) ou Mercosul (`ABC1D23`); aceita hífen e espaço e grava em maiúsculas, sem eles.
11. **Documento do romaneio** (modelo do usuário, A4): `RomaneioDAO.documento(id)` monta os textos e a página e o PDF usam os mesmos dados.
    - **Cabeçalho:** Nº = `numero`; SAFRA = ano inicial / ano final da safra do romaneio.
    - **01 Origem:** área do lote (V35: `proprietarioareaproducao`, `cpf_cnpj_produtor`, `propriedadeareaproducao`, `siglasareaproducao`, `inscricao_estadual`, endereço com `logradouro`, `numero`, `complemento`, `bairro`, `cep` e `municipio / uf`), mais emissão, NF, série e talhão/lote ("quadra · Lote N (diário X)").
    - **02 Destino:** o parceiro (V36). Empresa = razão social (ou o nome), CNPJ, endereço, CEP, IE, cidade, UF e telefone.
    - **03 Transporte:** campos da V37, mais motorista e placa.
    - **04 Mercadorias:** uma linha por item. Descrição = alimento; embalagem; classificação; quantidade = caixas; unid. CX; peso = caixas × `embalagem.peso_kg`; observação vazia. Linha TOTAIS com caixas e peso. Mínimo de 7 linhas (completa com vazias) e cresce se houver mais.
    - **Sem preços.** Campo sem dado sai vazio. Na página, todo texto do banco passa por `HtmlUtil.esc`.
    - **Botões:** Visualizar (nova aba) e Baixar PDF na lista (exceto cancelados) e no MR1 depois de salvo. A página tem Imprimir (CSS `@page A4`), Baixar PDF, Abrir romaneio e Voltar.

## Dependências
- **Colheita e Estoque:** lê `colheita_lote`, `colheita_processamento(_embalagem)` (soma por `idembalagem`, a mesma regra de `?caixasembaladas=`) e `embalagem` (`EmbalagemDAO.buscar`: só descrição e peso; desde 02/11 o Romaneio **não lê** `preco_compra`). Contrato de URL: `cadastroRomaneio.jsp?lote=<idLote>` (redirecionamento do "Finalizar lote", onda C).
- **Despesas e Custos:** `DespesasCustosDAO.listarRomaneio(Connection)` e `listAll()` (campos `iddespesascusto`, `despesascusto`, `valordespesascustos`, `formaCalculo`, `categoria`).
- **Safra:** `SafraDAO.resolverSafra` (leitura). A Safra **consome** `romaneio` (contrato acima).
- **Parceiro:** lê `parceiro` (`tipo_parceiro = 'PARCEIRO'`, `situacaopessoa`, `nomepessoa`). No documento também lê `razaosocialpessoacnpj`, `cnpjpessoacnpj`, `inscricaoestadualpessoacnpj`, `telefonepessoa`, `cep`, `numero`, `complemento` e as colunas da **V36** (`logradouro`, `bairro`, `cidade`, `uf`).
- **Área de Produção:** no documento, lê `areaproducao` (proprietário, propriedade, sigla, `cep`, `numero`, `complemento`) e as colunas da **V35** (`cpf_cnpj_produtor`, `inscricao_estadual`, `logradouro`, `bairro`, `municipio`, `uf`). Renomear essas colunas quebra o documento.
- **Alimento / Classificação:** lê `alimentoclassificacao` e `classificacao`.

## Pendências
- [x] Conferência visual feita pelo Gerente Geral em 02/11 no teste integrado (navegador do app, tela visível): lista e cards, "Romaneio Cad.", MR1 com grade classificação × embalagem, despesas, caixas embaladas e resumo. Tudo ok.
- [ ] Estorno é exclusão (sem trilha de auditoria). Se o usuário quiser histórico de estornos, acrescentar `estornado_em`/motivo em vez de apagar.
- [ ] Processamentos anteriores à V31 (sem `idembalagem`) não entram no saldo do romaneio.
- [x] Impressão/PDF do romaneio (feito em 04/11, ver Histórico).
- [ ] O PDF usa a Calibri do Windows (`C:\Windows\Fonts`). Em servidor sem essa fonte, cai para Helvetica (o layout se mantém, mas a fonte muda).
- [ ] O modelo tem "variedade" junto da descrição, e o cadastro de alimento não tem variedade separada: sai só o nome do alimento.

## Histórico
- 2026-11-04: **documento do romaneio (visualizar, imprimir e baixar PDF)**, no modelo do usuário, a pedido do Gerente.
  - **V37:** transporte e NF em `romaneio`. Bean, DAO (validação e gravação) e controller com os 9 campos. Bloco "Transporte e nota fiscal" no MR1, com motorista e placa movidos para ele e uma lista de sugestões de tipo de veículo.
  - **Documento:** `RomaneioDAO.documento(id)`, `romaneioDocumento.jsp` (A4, CSS de impressão), `Service/RomaneioDocumentoPdf` e `GET ?pdf=`. Botões na lista e no MR1.
  - **Medidas do modelo:** convertido com LibreOffice e lido no XML. Grade de 12 colunas de 43,5 pt, margens 36,65/24 pt e as cores do modelo.
  - **Build único** (com a V35 da Área e a V36 do Parceiro); V35–V37 aplicadas no startup.
  - **Verificado:**
    - pelo DAO: recusas de CPF curto, UF inválida, placa e carreta inválidas e documento da transportadora com nº de dígitos errado; normalização de placa, CPF e CNPJ; mapeamento completo;
    - PDF comparado com o modelo bloco a bloco; com 10 itens, a tabela cresce;
    - no Chrome logado:
      - botões na lista e no MR1;
      - o MR1 carrega e salva os campos e recusa CPF e UF inválidos;
      - `?pdf=` devolve `application/pdf` e `attachment` com `Romaneio_2026-000002.pdf` (`%PDF-`), e 404 para id inexistente;
      - a página do documento é igual ao modelo (screenshot) e o `<img onerror>` gravado no banco aparece como texto (escape ok).
  - **Console sem erros de JavaScript** em `areaproducao.jsp`, `CadastroAreaProducao.jsp` e `parceiro.jsp` (pedido do Gerente).
  - **Dados de teste apagados:** área 3, talhão 10, safra 6, parceiro 85, embalagens 17/18, diário 122, lote 23, romaneio 10. O romaneio real 2026-000001 não foi tocado.
- 2026-11-02: **"Custo de embalagem" sai das despesas de venda** (pedido do Gerente, decisão do usuário). A embalagem passa a ser custo de produção (estoque → diário de Processamento → Safra); no Romaneio contaria duas vezes.
  - `RomaneioDAO.salvar` não gera mais a linha `EMBALAGEM` e descarta as que vierem; o `contexto` não devolve mais `precoCompra` nas embalagens.
  - `main.js` (`crRenderDespesas`/`crRecalcular`) sem a linha automática. O quadro "Caixas embaladas do lote" foi mantido.
  - Não havia romaneios na base. Sem build nem restart: o Java entra no build único da Colheita. O `main.js` foi copiado para o target e conferido no Chrome (`?lote=3`): só Comissão, Frete e Vale; total de despesas e líquido corretos; sintaxe OK.
- 2026-11-02: **criação do módulo (pacote Romaneio, onda B).**
  - V33 com as 5 tabelas; `ControllerRomaneio`, `RomaneioDAO`, 4 beans; telas `romaneio.jsp` (3 cards: total, pendentes com o valor a receber, finalizados com o recebido; filtro por situação; ações Editar/Ver, Pagamentos, Cancelar) e `cadastroRomaneio.jsp` (MR1: dados do lote, venda, grade classificação × embalagem com caixas e preço, despesas de venda, saldo por embalagem e resumo com a situação prevista). Seção `MÓDULO ROMANEIO` no `main.js`, com textos do banco escapados (`rmEsc`). Registrado em `web.xml`, `menu.jsp` e no índice.
  - Build único com `mvnw.cmd clean package`; a V33 foi aplicada pelo SchemaMigrator no startup.
  - Verificado por um harness Java chamando o `RomaneioDAO` no banco local, com dados próprios **apagados no fim** (parceiros 72/73, embalagens 6/7, diário 106, lote 14, processamento 8, romaneios e pagamentos). Os 35 cenários passaram:
    - contexto (embalagens, classificações, safra, despesas);
    - ABERTO sem preço com bruto 1.200 / despesas 631 / líquido 569 conferidos à mão;
    - recusas: excesso de caixas ("tem só 40"), data anterior à colheita, parceiro inválido, líquido ≤ 0;
    - lote totalmente romaneado some da lista; edição desconta o próprio romaneio;
    - pagamento em grupo quitando R1 (FINALIZADO) e parcial no R2; recusas de soma ≠ valor, parte > saldo, cheque de terceiro sem emitente e romaneio de outro parceiro;
    - editar FINALIZADO recusado; líquido < pago recusado; cancelar com pagamento recusado;
    - estorno voltando os dois a PENDENTE; cancelar liberando o saldo do lote.
  - **Teste no Chrome, logado** (o Gerente refez o login), com dados próprios **apagados no fim**: parceiros 74/75, embalagens 8/9, diário 107, lote 15, romaneios 5–7.
    - "Romaneio Cad." lista só o lote com saldo ("150 de 150 cx") e leva a `?lote=15`;
    - MR1 com área, talhão, alimento, safra, parceiros PARCEIRO, grade 2 × 2 e 3 despesas do cadastro mais a linha de embalagem;
    - prévia ao vivo igual ao servidor: bruto R$ 1.200, despesas R$ 631, líquido R$ 569, situação Aberto;
    - salvar troca a URL para `?id=` e mostra o nº 2026-000001;
    - excesso de caixas mostrado em vermelho (saldo −10) e recusado pelo servidor ("tem só 40");
    - restante exato → "lote fica totalmente romaneado" e Pendente; o lote some do "Romaneio Cad.";
    - edição preservando frete, vale e motorista; preço incluído → Pendente (líquido R$ 1.553);
    - cards 2 / R$ 4.400 / R$ 3.013;
    - pagamento em grupo em cheque de terceiros (campos aparecem pela forma; sem banco/nº é recusado): R$ 2.053 distribuídos como 1.553 + 500, com o R1 Finalizado e o R2 parcial;
    - histórico com "Dividido com"; estorno voltando os dois a Pendente;
    - cancelar liberando 60 cx; o cancelado abre somente leitura;
    - **fluxo da Colheita:** Finalizar lote → modal "Ir para o Romaneio" → `cadastroRomaneio.jsp?lote=15` com o lote FINALIZADO aceito e o romaneio 2026-000003 salvo.
- 2026-11-02: padronização visual do botão de novo registro (Gerente)
