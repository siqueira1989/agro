# Relatório de Modificações — Agro Tech One
**Data:** 18/08/2026
**Responsável técnico:** Claude (assistente) — commits no branch `main`

---

## Resumo

Foram entregues **4 conjuntos de mudanças** cobrindo correção de bug de interface,
padronização de UX, uma correção de regra financeira e um lote de melhorias em
três módulos (Funcionário, Maquinário e Diário de Campo). Tudo compilado,
implantado no Tomcat, testado ponta a ponta e enviado ao GitHub.

| # | Commit | Tipo | Módulo(s) | Descrição curta |
|---|--------|------|-----------|-----------------|
| 1 | `3bb7aac` | fix | Classificação / Alimento | Modal quebrado + IDs duplicados corrigidos |
| 2 | `b9c0a08` | feat | Todo o sistema (tabelas) | Carregamento automático suave das tabelas |
| 3 | `b6e5637` | fix | Safra | Custo consolidado só de atividades concluídas |
| 4 | `e0dd927` | feat | Funcionário / Maquinário / Diário | Salário CLT, auto-soma custo/hora e regras do Diário |

---

## 1. Correção do modal de Classificação (e Alimento) — `3bb7aac`

### Problema
O cadastro de Classificação não salvava e o modal aparecia "desconfigurado".

### Causa
HTML malformado: o `<div class="input-group">` fechava **antes** do `<input>`,
jogando o campo para fora do grupo, e um `</div>` extra fechava o corpo do modal
cedo demais, desmontando o layout. Havia também `id` de alerta duplicados.

### Solução
- Colocado o `<input>`/`<select>` dentro do `input-group` e removido o `</div>` sobrando.
- Removidos os `id` duplicados dos alertas.
- Mesma correção aplicada aos 3 blocos equivalentes de `alimento.jsp`.

### Arquivos
- `src/main/webapp/view/admin/classificacao.jsp`
- `src/main/webapp/view/admin/alimento.jsp`

### Teste
Cadastro de classificação salvando e aparecendo na lista; input dentro do grupo;
sem `id` duplicado. **OK.**

---

## 2. Carregamento automático padronizado das tabelas — `b9c0a08`

### Objetivo
Padronizar o recarregamento automático de **todas** as listagens do sistema,
de forma **suave** (sem resetar busca, ordenação e página do usuário).

### O que mudou
- Antes só 5 tabelas recarregavam, e faziam `destroy()` + recriação a cada
  10–20s (perdendo o estado do usuário). Agora um único helper
  `autoRefreshTabela(chave, fn, 15s)` cuida de **10 tabelas**: Classificação,
  Despesas/Custos, Alimento, Parceiros, Funcionários, Estoque, Áreas, Máquinas,
  Safras e Diário de Campo.
- Refresh suave: `ajax.reload(null,false)` / `ajax.url().load(null,false)` nas
  DataTables com AJAX, e `draw(false)` nas tabelas de dados.
- Filtros dinâmicos (categoria de insumo, tipo de parceiro) preservados no reload.
- **Pausa automática** quando a aba está em segundo plano ou há um modal aberto
  (não recarrega a lista enquanto o usuário edita).
- Loaders manuais (Áreas/Máquinas/Safras) limpam a tabela só quando os dados
  chegam, eliminando o "flash" de tabela vazia.

### Arquivo
- `src/main/webapp/JS/main.js`

### Teste
Busca digitada preservada após o reload (prova de que não há `destroy`); timers
registrados por página; sem erros de console. **OK.**

---

## 3. Custo da Safra só de atividades concluídas — `b6e5637`

### Problema
O Resumo Financeiro da Safra somava o custo de **todas** as atividades (exceto
canceladas), incluindo as **Planejadas** — que são apenas agendamentos ainda não
executados. Isso inflava o custo por planta/hectare/saca e o rateio por talhão.

### Solução
Troca do filtro `status <> 'CANCELADA'` por `status = 'CONCLUIDA'` nos dois
pontos de consolidação (COE por componente e custo real por talhão), alinhando
com o card de Custo Total da Área e com os relatórios, que já contavam apenas
atividades concluídas.

### Arquivo
- `src/main/java/Model/Dao/SafraDAO.java`

### Teste
Compilação e classe implantada confirmadas com o novo filtro. **OK.**

---

## 4. Funcionário, Maquinário e Diário de Campo — `e0dd927`

Lote de melhorias solicitadas nos três módulos.

### 4.1 Funcionário (CLT)
- **Removido** o campo "Data de Desligamento" do cadastro (é uma admissão nova).
- **Adicionado "Salário Mensal"**, gravado em `vinculo_empregaticio.salario_mensal`
  — base do custo/hora usado nas atividades do Diário.

### 4.2 Maquinário
- O campo **Custo/Hora agora soma automaticamente** combustível + manutenção +
  depreciação enquanto se digita. Ao marcar **Implemento** (que não tem
  combustível próprio), o valor é recalculado.

### 4.3 Diário de Campo
- **3 status**: Planejado (agendamento), **Execução** (antes "Em andamento") e
  Concluído. Seletor de status no modal e filtro sem "Cancelada".
- **Planejado é previsão → não calcula custo** (custo 0). Execução e Concluído
  calculam normalmente.
- **Safra obrigatória** para criar/editar atividade: o backend bloqueia quando
  não há safra cobrindo o talhão na data, e o frontend avisa/guarda.
- **Responsável** passa a listar **apenas os funcionários adicionados** na
  atividade (quem está executando).
- **Funcionários lançáveis** = apenas os com **vínculo ativo na data** do
  lançamento (admitido até a data e não desligado); recarrega ao mudar a data.
- **Campo Horas** aceita horas e minutos: `2h30`, `45min`, `2:30`, `1,5` —
  convertidos para horas decimais no cálculo.
- **Atividade multi-dia**: fica em Execução e pode receber novos
  colaboradores/horas em edições posteriores; o custo soma o total lançado.
- **Notificação de atividades planejadas** na página do perfil da área.

### Arquivos
- `src/main/webapp/view/admin/ColaboCadastro.jsp`
- `src/main/webapp/view/admin/DetalheAreaProducao.jsp`
- `src/main/webapp/JS/main.js`
- `src/main/java/Controller/ControllerFuncionario.java`
- `src/main/java/Controller/ControllerDiarioCampo.java`
- `src/main/java/Model/Dao/DiarioCampoDAO.java`

### Testes ponta a ponta
| Item | Resultado |
|------|-----------|
| Salário CLT → vínculo (3.300) e custo/hora derivado | **18,75** = 3300 / (8h × 22) ✅ |
| Auto-soma custo/hora da máquina | Trator 50+30+20 = **100,00**; Implemento **50,00** ✅ |
| Funcionários filtrados pela data (vínculo ativo) | Admitido em 01/01/2026 não aparece antes; aparece depois ✅ |
| Safra obrigatória | Criação sem safra **bloqueada** (HTTP 400) ✅ |
| Planejado sem custo × Execução com custo | Planejado **0,00**; Execução **37,50** (18,75 × 2h) ✅ |
| Responsável só entre os adicionados | Só "Selecione" → aparecem os adicionados; some ao remover ✅ |
| Notificação de planejadas | "2 atividade(s) planejada(s) aguardando execução" ✅ |
| Parser de horas | `2h30`→2.5, `45min`→0.75, `2:30`→2.5, `1,5`→1.5 ✅ |

Os dados criados para teste (funcionário, safra e diários) foram **removidos** ao final.

---

## Observações e pontos em aberto

- **Intervalo do auto-refresh** das tabelas ficou em **15 segundos** para todas;
  pode ser ajustado facilmente (é um único parâmetro no helper).
- **Multi-dia do Diário** foi resolvido como "mesma atividade em Execução, soma
  tudo" (opção escolhida). Um registro dia-a-dia mais detalhado (quem trabalhou
  em cada data) pode ser evoluído no futuro com "sessões por dia"; hoje o custo
  total já fica correto.
- Todos os commits acima estão em `main` e enviados ao GitHub
  (`siqueira1989/agro`).
