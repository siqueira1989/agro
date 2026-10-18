# Módulos do Agro Tech One

Cada módulo tem uma conversa dedicada com o Claude e uma ficha de contexto nesta pasta.
Um módulo = um controller de negócio (mais o DAO, os models, a tela e a seção do `main.js` dele).

## Como abrir a conversa de um módulo

No Claude Code (dentro de `C:\projeto\agro`), abra uma conversa nova e digite:

```
/modulo classificacao
```

Sem o comando, a primeira mensagem pode ser:

```
Esta conversa é o Módulo Classificação. Leia docs/modulos/classificacao.md e o CLAUDE.md, confirme o escopo e aguarde.
```

Dê à conversa o nome do módulo (ex.: "Módulo Classificação") para achá-la depois na barra lateral.

## Índice

| # | Módulo | Slug (`/modulo <slug>`) | Controller | Menu |
|---|---|---|---|---|
| 1 | Classificação | `classificacao` | ClassificacaoServlet | Configuração |
| 2 | Despesas e Custos | `despesas-custos` | DespesasCustosServlet | Configuração |
| 3 | Parceiro | `parceiro` | ControllerParceiro | Configuração |
| 4 | Insumo | `insumo` | ControllerInsumo | Configuração (Estoque de Insumos) |
| 5 | Estoque | `estoque` | ControllerEstoque | Configuração (Estoque de Insumos) |
| 6 | Maquinário | `maquina` | ControllerMaquina | Configuração |
| 7 | Alimento | `alimento` | ControllerAlimento | Produção |
| 8 | Área de Produção | `area-producao` | ControllerAreaProducao | Produção |
| 9 | Safra | `safra` | ControllerSafra | Produção |
| 10 | Diário de Campo | `diario-campo` | ControllerDiarioCampo | Produção (dentro da Área) |
| 11 | Colheita e Estoque | `colheita` | ControllerColheita | Produção |
| 12 | Folha de Pagamento | `folha-pagamento` | ControleColaborador | RH |
| 13 | Funcionário | `funcionario` | ControllerFuncionario | RH |
| 14 | Perfil CLT | `clt` | ControllerCLT | RH (Perfis) |
| 15 | Perfil Diarista | `diarista` | ControllerDiarista | RH (Perfis) |
| 16 | Perfil Empreita | `empreita` | ControllerEmpreita | RH (Perfis) |
| 17 | Perfil Produção (caixa) | `producao-caixa` | ControllerProducao | RH (Perfis) |
| 18 | Pagamento | `pagamento` | ControllerPagamento | RH (modal nos perfis) |
| 19 | Ponto Eletrônico | `ponto-eletronico` | ControllerPontoEletronico | RH |
| — | Núcleo (login, menu, relatórios, utilitários, build) | `nucleo` | Login, Logout, RelatorioExport | — |

## Pares que se tocam (coordenar entre conversas)

- **Insumo ↔ Estoque**: mesma tela (`estoque.jsp`) e mesma seção do JS.
- **Área de Produção ↔ Diário de Campo**: mesma página (`DetalheAreaProducao.jsp`).
- **CLT ↔ Ponto Eletrônico**: mesmas tabelas de ponto, falta, vale e fechamento.
- **Pagamento ↔ 4 perfis**: o modal é incluído em CLT, Diarista, Empreita e Produção.
- **Diário de Campo** lê dados de quase todos os módulos de Produção e RH.
- `Service/*` (cálculo de folha) é compartilhado: CLT, Ponto, Folha e Diário.

## Manutenção das fichas

Ao fim de cada entrega, a conversa do módulo atualiza "Pendências" e "Histórico" da própria ficha.
Módulo novo → criar a ficha aqui e uma linha neste índice.
