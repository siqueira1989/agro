# Módulo Perfil Produção (por caixa)

**Objetivo:** perfil do funcionário pago por produção: caixas colhidas, preço por tipo de caixa e vales.
**Menu:** RH → Perfis e Fechamento (Produção). Não confundir com o menu "Produção" (área, safra, colheita).

## Escopo
| Camada | Arquivo |
|---|---|
| Controller | `Controller/ControllerProducao.java` — URL `/ControllerProducao` |
| DAO | `Model/Dao/ProducaoCaixaDAO.java`, `Model/Dao/PrecoCaixaDAO.java` |
| Model | `FuncionarioProducao.java`, `TipoCaixa.java` |
| View | `view/admin/perfilProducao.jsp` |
| JS | `main.js` → seção `MÓDULO PRODUÇÃO — perfilProducao.jsp` |

**Ações:** `addcaixa`, `editcaixa`, `removecaixa`, `setpreco`, `registrarvale`, `excluirvale`, `marcarvaledescontado`.

## Banco
- `producao_caixa`, `preco_caixa` (V7); usa `vale_funcionario`, `vinculo_empregaticio`.
- **Só leitura** (tabelas do Diário/Colheita): `diario_funcionario`, `diario_campo`, `diario_tipo_atividade`,
  `quadra`, `areaproducao`; e, se existir (V30), `colheita_processamento` + `colheita_processamento_embalagem`
  (tipo de embalagem do Embalador). O perfil nunca grava nessas tabelas.

## Pagamento do funcionário de Produção (decisão do usuário, 2026-10-25)
O Colhedor/Embalador é pago **só aqui no perfil** (modal de Pagamento). O Diário de Campo registra o custo da
safra, mas não paga ninguém. O perfil soma:
- **Manual** — `producao_caixa` do mês (lançado no próprio perfil);
- **Diário** — linhas de `diario_funcionario` com `regime_trabalho='PRODUCAO'` da pessoa, pela data da execução
  (`COALESCE(data_execucao, diario_campo.data)`), valor = `valor_regime` (snapshot do diário).
- **Regra de status:** só diário **CONCLUIDA** entra no total (mesma regra do custo da safra: diário fechado não
  muda mais). **EM_ANDAMENTO** aparece na tabela, apagado e marcado "Em andamento — fora do total", porque ainda
  pode ser editado ou receber execuções; entra no total quando for concluído. PLANEJADA e CANCELADA não aparecem.
- Total: `valorProducao = valorManual + valorDiario`; `valorLiquido = valorProducao − vales` → `__pgCtx.valor`.
  `__pgCtx.composicao` (campo extra, só texto) é mostrado no modal de Pagamento em `#pgPrazoInfo`
  ("Manual R$ X + Diário R$ Y = R$ Z − Vales …"), por um listener `shown.bs.modal` na seção deste módulo — o modal
  e a seção PAGAMENTOS não foram alterados.
- **Duplicidade:** `addcaixa`/`editcaixa` numa data em que já há caixas do Diário (EM_ANDAMENTO ou CONCLUIDA)
  para a pessoa respondem `{ok:false, confirmar:true, msg}`; o front pede confirmação e reenvia com
  `confirmardiario:true`. **Avisa em vez de bloquear**: no mesmo dia a pessoa pode ter caixas legítimas fora do
  diário (outro tipo de caixa, outro lote ou ajuste); o lançamento manual não substitui nem altera o diário.
- **Tipo de caixa por linha:** `diario_funcionario` não guarda a embalagem. Colheita → sempre CONTENTOR ("Caixa
  Contentor"). Processamento → `ProducaoCaixaDAO.embalagemDaLinha` procura em `colheita_processamento_embalagem` do
  mesmo diário e pessoa: 1º preço/caixa + nº de caixas iguais, 2º só preço, 3º o diário inteiro; aceita só se der
  **um único tipo** — ambíguo ou sem registro mostra "—" (nunca junta tipos). Rótulo: nome do cadastro em Despesas
  (`despesascusto.despesascusto`), senão o rótulo do enum `TipoCaixa`, senão o código formatado ("PAPELAO_7KG" →
  "Caixa Papelão 7kg").
- Resposta do GET `acao=perfil` ganhou: `lancamentosDiario[]` ({idDiario, numeroDiario, status, contaNoTotal,
  idArea, data, atividade, area, talhao, tipoCaixa, tipoCaixaRotulo, caixas, precoPorCaixa, valor}), `caixasDiario`, `valorManual`,
  `valorDiario`. `valorProducao`/`valorLiquido` agora incluem o Diário.

## Dependências
- `ValeFuncionarioDAO`, `VinculoDAO`, `FuncionarioDAO` são compartilhados.
- **Diário de Campo / Colheita**: desde a V24, CLT, diarista ou empreita podem ser pagos por caixa num diário de Colheita (`diario_funcionario.regime_trabalho`) — mudança no preço/tipo de caixa afeta esse cálculo.
- **Diário de Campo (leitura)**: `ProducaoCaixaDAO.listarDoDiario` e `diariosNaData` leem `diario_funcionario`
  (`idpessoa`, `regime_trabalho`, `quantidade_caixas`, `preco_por_caixa`, `valor_regime`, `data_execucao`) e
  `diario_campo` (`status`, `data`, `numero_diario`, `id_area`, `id_quadra`, `id_tipo_atividade`). Renomear/remover
  essas colunas ou mudar os status quebra o perfil. Não usa `DiarioCampoDAO.somarValorRegimeProducao`/
  `listarDiasRegimeProducao` (folha CLT): aqueles contam tudo que não é CANCELADA e não trazem o detalhe.
- **Colheita (leitura, opcional)**: tipo de embalagem vem de `colheita_processamento_embalagem` (V30) se a tabela
  existir (checado com `to_regclass`); na Colheita o tipo é sempre CONTENTOR.
- **Pagamento** (modal incluído no perfil). Usa o campo extra `__pgCtx.composicao` e escreve em `#pgPrazoInfo`
  só quando `__pgCtx.tipo === 'PRODUCAO'`.

## Pendências
- [ ] Link "Diário" abre a área (`DetalheAreaProducao.jsp?id=`), não o diário específico — se o Diário ganhar um
  parâmetro para abrir um diário pelo id, trocar o link.
- [ ] Pagamento não é vinculado às caixas (é por período): se o usuário pagar o mês e depois concluir um diário
  daquele mês, o total sobe e a diferença precisa de outro pagamento — não há controle de "já pago" por linha.

## Histórico
- 2026-10-25 — Perfil Produção passa a ler as caixas do Diário de Campo (Colhedor na Colheita, Embalador no
  Processamento): tabela "Lançadas no Diário de Campo" (só leitura), total = Manual + Diário (só CONCLUIDA), composição
  no Fechamento e no modal de Pagamento, aviso com confirmação ao lançar caixa manual numa data com caixas do Diário.
  Verificado no navegador com dados de teste criados por SQL (Colhedor "TESTE Colhedor PerfilProd" id 67 com login de
  teste; diários TESTE-PP-1 CONCLUIDA 30 cx × R$ 4 em 24/10 e TESTE-PP-2 EM_ANDAMENTO 10 cx em 25/10): tabela mostrou
  as 2 linhas (a 2ª "fora do total"), total R$ 120; caixa manual em 24/10 pediu confirmação (cancelar não gravou,
  confirmar gravou); com preço R$ 3,50 → "Manual R$ 17,50 + Diário R$ 120,00 = R$ 137,50" no Fechamento e no modal,
  `pgValor` 137,50. Dados de teste (pessoa 67, diários 91/92, `producao_caixa` e `preco_caixa` dela) apagados.
- 2026-10-25 — Correção (teste integrado do Gerente): a coluna "Tipo de Caixa" juntava todos os tipos do diário em
  cada linha ("Caixa Papelão 10kg, PAPELAO_7KG"). Agora cada linha mostra só a sua embalagem (regra acima) com rótulo
  legível. Verificado com embalador de teste (id 69): diário A com 40 cx × R$ 2,30 (PAPELAO_10) e 20 cx × R$ 1,80
  (PAPELAO_7KG) → "Caixa Papelão 10kg" e "Caixa Papelão 7kg" em linhas separadas; diário B com 2 linhas iguais
  (10 cx × R$ 2,00) e tipos diferentes → "—" nas duas. Dados de teste (pessoa 69, diários TESTE-PP-A/B, lote e
  processamentos) apagados.
