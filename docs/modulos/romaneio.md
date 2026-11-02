# Módulo Romaneio

**Objetivo:** venda da produção processada. As caixas embaladas de um lote de colheita são vendidas a um parceiro comprador, com as despesas de venda e os recebimentos. É onde entram as **receitas** do sistema: venda bruta e líquida.
**Menu:** Produção → Romaneio

## Escopo
| Camada | Arquivo |
|---|---|
| Controller | `Controller/ControllerRomaneio.java`, URL `/ControllerRomaneio` |
| DAO | `Model/Dao/RomaneioDAO.java` |
| Model | `Romaneio.java`, `RomaneioItem.java`, `RomaneioDespesa.java`, `RomaneioPagamento.java` |
| View | `view/admin/romaneio.jsp` (lista, cards, modais "Romaneio Cad.", "Registrar pagamento" e histórico), `view/admin/cadastroRomaneio.jsp` (passo MR1) |
| JS | `main.js` → seção `MÓDULO ROMANEIO` (funções `rm*` da lista, `rp*` do pagamento, `cr*` do cadastro) + bloco `romaneio.jsp / cadastroRomaneio.jsp init` |
| Núcleo (autorizado) | `WEB-INF/web.xml` (servlet + mapping), `pagina/menu.jsp` (item "Romaneio" em Produção) |

**GET:** lista (`[?status=]`), `?lotes=1`, `?contexto=1&lote=<idLote>` ou `&id=<idromaneio>`, `?id=<idromaneio>`, `?parceiros=1`, `?pendentes=<idParceiro>`, `?pagamentos=<idromaneio>`.
**POST (JSON, `acao`):** `salvar`, `cancelar`, `pagar`, `estornar`. Resposta `{ok, msg, target}` (+ `romaneio` / `idPagamento`). Regra violada ⇒ HTTP 400 com a mensagem; erro técnico ⇒ 500 com o código do `LogUtil`.

## Banco: V33 (`V33__romaneio.sql`)
- `romaneio`: `idromaneio`, `numero` UNIQUE (`AAAA-NNNNNN`, sequência por ano do `data_emissao`, gerada com `pg_advisory_xact_lock`), `idlote` → `colheita_lote`, `id_area`, `id_quadra`, `id_alimento`, `id_safra` (resolvida por `SafraDAO.resolverSafra(talhão, data da colheita do lote)`; sem safra o salvamento é recusado), `id_parceiro` (sem FK, por causa da herança de pessoa), `data_emissao`, `motorista`, `placa` (maiúsculas, sem espaços), `status` CHECK (`ABERTO|PENDENTE|FINALIZADO|CANCELADO`), `total_caixas`, `valor_bruto`, `total_despesas`, `valor_liquido`, `valor_pago` (padrão 0), `observacao`, `criado_em`, `atualizado_em`.
- `romaneio_item`: `idromaneio` (cascade), `idclassificacao`, `idembalagem`, `caixas` > 0, `preco_unitario` (NULL até informar), `valor`. UNIQUE (romaneio, classificação, embalagem).
- `romaneio_despesa`: `iddespesascusto` (NULL na linha automática), `descricao`, `forma_calculo` (`FIXO|PERCENTUAL_VENDA|POR_CAIXA|EMBALAGEM`), `percentual`, `quantidade`, `valor_unitario`, `valor`. É snapshot: mudar o cadastro de despesas não altera romaneios gravados.
- `romaneio_pagamento`: `id_parceiro`, `data_pagamento`, `forma` (`DINHEIRO|PIX|CHEQUE_PROPRIO|CHEQUE_TERCEIRO`), `valor` > 0, `banco`, `numero_cheque`, `emitente`, `bom_para`, `observacao`, `criado_em`.
- `romaneio_pagamento_item`: `idpagamento` (cascade), `idromaneio`, `valor` > 0.

### Contrato para a Safra
Somar por safra e por talhão `valor_bruto`, `total_despesas` e `valor_liquido` de `romaneio` com `status <> 'CANCELADO'`, usando `romaneio.id_safra` e `romaneio.id_quadra`. Os quatro valores (`valor_bruto`, `total_despesas`, `valor_liquido`, `valor_pago`) são recalculados e gravados pelo `RomaneioDAO` em todo `salvar`, `pagar` e `estornar`. Nenhum outro caminho grava esses campos.
Observação: romaneios ABERTO entram com a venda parcial, porque os itens sem preço valem 0. Se a Safra quiser só a receita "confirmada", deve filtrar `status IN ('PENDENTE','FINALIZADO')`.

## Regras
1. **Lotes disponíveis** (`?lotes=1`, botão "Romaneio Cad."): `colheita_lote.status = 'PROCESSADO'`, com caixas embaladas (processamentos com `idembalagem`) e saldo > 0 em alguma embalagem. O MR1 aceita lotes PROCESSADO ou FINALIZADO.
2. **Caixas:** por embalagem, a soma dos itens dos romaneios não cancelados do lote não pode passar das caixas embaladas. O lote é travado com `FOR UPDATE` no `salvar`, e na edição o próprio romaneio é descontado. A tela mostra Embaladas / Outros romaneios / Este / Saldo ao vivo e avisa quando o lote fica totalmente romaneado.
3. **Itens:** a classificação precisa ser do alimento do lote (`alimentoclassificacao`) e a embalagem precisa ter caixas no lote. A data de emissão não pode ser anterior à colheita.
4. **Valores (servidor):** item = caixas × preço (2 casas). Bruto = Σ itens. Despesas:
   - linha automática **Custo de embalagem** = Σ caixas × `embalagem.preco_compra` (sempre regerada);
   - `PERCENTUAL_VENDA` = bruto × % / 100;
   - `POR_CAIXA` = total de caixas × R$/cx;
   - `FIXO` = valor digitado.

   Líquido = bruto − despesas. As despesas iniciais vêm de `DespesasCustosDAO.listarRomaneio` (Comissão 18%, Frete por embalagem, Vale). O usuário ajusta %, R$/cx ou o valor, remove linhas ou inclui outra despesa do cadastro (`listAll`, categoria GERAL).
5. **Situação:** ABERTO (algum item sem preço) → PENDENTE (todos com preço). Com todos os preços, o líquido precisa ser > 0. FINALIZADO quando `valor_pago ≥ valor_liquido`. FINALIZADO e CANCELADO não podem ser editados (abrem só para leitura).
6. **Edição com pagamento:** todos os itens precisam continuar com preço, o líquido não pode ficar menor que o já pago e o parceiro não pode ser trocado. Se o líquido ficar igual ao pago, o romaneio vira FINALIZADO.
7. **Pagamento em grupo** (`pagar`): parceiro, data, forma e valor. Cheque exige banco e número; cheque de terceiro exige o emitente; "bom para" ≥ data. A tela distribui o valor do romaneio mais antigo para o mais novo, até o saldo de cada um, e as partes são editáveis. No servidor: cada parte > 0 e ≤ saldo, o romaneio precisa ser do parceiro e estar PENDENTE, e Σ partes = valor.
8. **Estorno** (`estornar`): exclui o pagamento inteiro (todas as partes) e recalcula `valor_pago`/situação de cada romaneio afetado (FINALIZADO → PENDENTE). O modal avisa quando o pagamento foi dividido com outros romaneios.
9. **Cancelar:** só ABERTO/PENDENTE sem pagamento. Libera as caixas do lote.

## Dependências
- **Colheita e Estoque:** lê `colheita_lote`, `colheita_processamento(_embalagem)` (soma por `idembalagem`, a mesma regra de `?caixasembaladas=`) e `embalagem` (`EmbalagemDAO.buscar`: descrição, peso, `preco_compra`). Contrato de URL: `cadastroRomaneio.jsp?lote=<idLote>` (redirecionamento do "Finalizar lote", onda C).
- **Despesas e Custos:** `DespesasCustosDAO.listarRomaneio(Connection)` e `listAll()` (campos `iddespesascusto`, `despesascusto`, `valordespesascustos`, `formaCalculo`, `categoria`).
- **Safra:** `SafraDAO.resolverSafra` (leitura). A Safra **consome** `romaneio` (contrato acima).
- **Parceiro:** lê `parceiro` (`tipo_parceiro = 'PARCEIRO'`, `situacaopessoa`, `nomepessoa`).
- **Alimento / Classificação:** lê `alimentoclassificacao` e `classificacao`.

## Pendências
- [x] Conferência visual feita pelo Gerente Geral em 02/11 no teste integrado (navegador do app, tela visível): lista e cards, "Romaneio Cad.", MR1 com grade classificação × embalagem, despesas, caixas embaladas e resumo. Tudo ok.
- [ ] Estorno é exclusão (sem trilha de auditoria). Se o usuário quiser histórico de estornos, acrescentar `estornado_em`/motivo em vez de apagar.
- [ ] Ao reeditar, o custo de embalagem usa o `preco_compra` **atual** do cadastro (é recalculado a cada salvar). Os demais parâmetros ficam como gravados no romaneio.
- [ ] Processamentos anteriores à V31 (sem `idembalagem`) não entram no saldo do romaneio.
- [ ] Impressão/PDF do romaneio (não pedido ainda).

## Histórico
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
