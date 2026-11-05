# Módulo Área de Produção

**Objetivo:** áreas produtivas e suas quadras/talhões (com o alimento plantado); perfil da área.
**Menu:** Produção → Área de Produção

## Escopo
| Camada | Arquivo |
|---|---|
| Controller | `Controller/ControllerAreaProducao.java` — URL `/ControllerAreaProducao` |
| DAO | `Model/Dao/AreaProducaoDAO.java`, `Model/Dao/QuadraDAO.java` |
| Model | `AreaProducao.java`, `Quadra.java` |
| View | `areaproducao.jsp`, `CadastroAreaProducao.jsp`, `AtualizarAreaProducao.jsp`, `DetalheAreaProducao.jsp` (**só cabeçalho e quadras** — o Diário de Campo vive na mesma página) |
| JS | `main.js` → seção `MÓDULO ÁREA DE PRODUÇÃO` (LISTA, CADASTRO, ATUALIZAÇÃO, DETALHE) |

**Ações:** `create`, `update`, `delete`, `desativar`, `addquadra`, `ativarquadra`, `desativarquadra`.

## Banco
- `areaproducao`, `quadra` (V12 remove talão, V13 quadras).
- V35: dados do produtor e endereço gravados na área (ver Dependências).

## Dependências
- **Diário de Campo**: mesma página (`DetalheAreaProducao.jsp`).
- **Safra**: vincula área e quadras (`safra_talhao`, V18).
- **Romaneio** lê de `areaproducao` (V35, contrato fixo — não renomear): `proprietarioareaproducao` (nome do produtor), `cpf_cnpj_produtor varchar(18)`, `inscricao_estadual varchar(20)`, `logradouro varchar(120)`, `bairro varchar(60)`, `municipio varchar(60)`, `uf char(2)`, além de `cep`, `numero`, `complemento`. Todas opcionais (NULL). JSON do GET da área: `cpfCnpjProdutor`, `inscricaoEstadual`, `logradouro`, `bairro`, `municipio`, `uf`.
- **Colheita**.

## Pendências
- [ ] A mudança em `RelatorioExportServlet.java` (remoção do `case "areaproducao"`) só entra no próximo build Maven; até lá o servlet compilado no Tomcat ainda aceita a URL antiga.

## Histórico
- 2026-10-25 — Removida a exportação Excel/PDF da página: botões em `areaproducao.jsp` (antigas linhas 15–21); `main.js` não tinha handler dessa exportação; em `RelatorioExportServlet.java` (autorizado pelo Gerente) removidos `case "areaproducao"`, `exportarAreasExcel/Pdf`, campo/`init` do `AreaProducaoDAO` e imports. Folha e funcionário intactos. JSP copiado para `target`; build pendente.
- 2026-11-02: padronização visual do botão de novo registro (Gerente)
- 2026-11-04 — Dados do produtor para o Romaneio: migração `V35__areaproducao_dados_produtor.sql`; model/DAO/controller (create, update, GET) com `cpfCnpjProdutor`, `inscricaoEstadual`, `logradouro`, `bairro`, `municipio`, `uf`; Cadastro/Atualizar passam a gravar endereço (ViaCEP preenche, campos editáveis) + CPF/CNPJ (máscara, 11 ou 14 dígitos, também validado no servidor) e IE; Perfil da Área (`DetalheAreaProducao.jsp`, só o card Propriedade) exibe os novos dados. Aguarda build do Romaneio + teste no navegador (V35 aplicada pelo Flyway no start).
- 2026-11-04 — Teste no navegador (pós-build, V35 aplicada): cadastro com CEP 01310-100 (ViaCEP), CPF/CNPJ com máscara, IE; edição; Perfil da Área; validação do servidor (400 com 3 dígitos); sem erros no console. Área de teste `TV35` (id 4) ficou **inativa** — a ação `delete` do controller só desativa; excluir de vez exige SQL.
