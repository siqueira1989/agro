# =============================================================================
# GerarDocBanco.ps1 — Engenharia Reversa: Banco agro → Documento Word
# =============================================================================

$outputPath = "C:\projeto\agro\Documentacao_Banco_Agro.docx"

# ---- Dados coletados via reverse-engineering --------------------------------
$schema = @{

    "pessoa" = @{
        descricao = "Entidade base (herança). Armazena dados comuns a todos os tipos de pessoa do sistema."
        colunas   = @(
            @{col="idpessoa";        tipo="INTEGER (PK, AUTO)"; nulo="NÃO"; desc="Identificador único"}
            @{col="nomepessoa";      tipo="VARCHAR(50)";        nulo="NÃO"; desc="Nome completo"}
            @{col="usuariopessoa";   tipo="VARCHAR(50)";        nulo="SIM"; desc="Login de acesso"}
            @{col="senhapessoa";     tipo="VARCHAR(50)";        nulo="SIM"; desc="Senha de acesso"}
            @{col="nivelpessoa";     tipo="VARCHAR(20)";        nulo="NÃO"; desc="Nível de acesso (ADMIN, USER...)"}
            @{col="situacaopessoa";  tipo="BOOLEAN";            nulo="SIM"; desc="Ativo (true) / Inativo (false)"}
            @{col="emailpessoa";     tipo="VARCHAR(50) UNIQUE"; nulo="NÃO"; desc="E-mail — único no sistema"}
            @{col="telefonepessoa";  tipo="VARCHAR(25)";        nulo="NÃO"; desc="Telefone de contato"}
            @{col="numero";          tipo="INTEGER";            nulo="NÃO"; desc="Número do endereço"}
            @{col="complemento";     tipo="VARCHAR(50)";        nulo="NÃO"; desc="Complemento do endereço"}
            @{col="cep";             tipo="VARCHAR(100)";       nulo="SIM"; desc="CEP do endereço"}
        )
        unique  = @("emailpessoa")
        fks     = @()
    }

    "pessoafisica" = @{
        descricao = "Extensão de PESSOA para pessoas físicas. Herda todos os campos de pessoa."
        colunas   = @(
            @{col="idpessoa";           tipo="INTEGER (FK→pessoa)"; nulo="NÃO"; desc="Herda PK de pessoa"}
            @{col="cpfpf";              tipo="VARCHAR(50) UNIQUE";  nulo="NÃO"; desc="CPF — único"}
            @{col="datanascimentopf";   tipo="DATE";                nulo="NÃO"; desc="Data de nascimento"}
            @{col="[+ campos pessoa]";  tipo="—";                   nulo="—";   desc="Todos os campos de pessoa herdados"}
        )
        unique = @("cpfpf")
        fks    = @()
    }

    "pessoacnpj" = @{
        descricao = "Extensão de PESSOA para pessoas jurídicas (empresas/parceiros PJ)."
        colunas   = @(
            @{col="idpessoa";                    tipo="INTEGER (FK→pessoa)";    nulo="NÃO"; desc="Herda PK de pessoa"}
            @{col="cnpjpessoacnpj";              tipo="VARCHAR(50) UNIQUE";     nulo="NÃO"; desc="CNPJ — único"}
            @{col="razaosocialpessoacnpj";        tipo="VARCHAR(50) UNIQUE";     nulo="NÃO"; desc="Razão social — única"}
            @{col="inscricaoestadualpessoacnpj"; tipo="VARCHAR(50) UNIQUE";     nulo="NÃO"; desc="Inscrição estadual — única"}
            @{col="[+ campos pessoa]";           tipo="—";                      nulo="—";   desc="Todos os campos de pessoa herdados"}
        )
        unique = @("cnpjpessoacnpj","razaosocialpessoacnpj","inscricaoestadualpessoacnpj")
        fks    = @()
    }

    "parceiro" = @{
        descricao = "Parceiros comerciais (fornecedores / compradores). Extensão de PESSOACNPJ."
        colunas   = @(
            @{col="idpessoa";     tipo="INTEGER (FK→pessoa)"; nulo="NÃO"; desc="Herda PK de pessoa"}
            @{col="siteparceiro"; tipo="VARCHAR(50) UNIQUE";  nulo="NÃO"; desc="Site do parceiro — único"}
            @{col="[+ campos pessoacnpj]"; tipo="—";          nulo="—";   desc="Campos CNPJ herdados"}
        )
        unique = @("siteparceiro")
        fks    = @()
    }

    "funcionario" = @{
        descricao = "Entidade base dos funcionários — herda de PESSOAFISICA e adiciona dados trabalhistas."
        colunas   = @(
            @{col="idpessoa";             tipo="INTEGER (PK, FK→pessoa)"; nulo="NÃO"; desc="PK herdada de pessoa"}
            @{col="matriculafuncionario"; tipo="VARCHAR(30) UNIQUE";      nulo="NÃO"; desc="Matrícula — única"}
            @{col="tipofuncionario";      tipo="ENUM";                    nulo="NÃO"; desc="CLT | DIARISTA | EMPREITA | PRODUCAO"}
            @{col="cargofuncionario";     tipo="VARCHAR(60)";             nulo="SIM"; desc="Cargo/função"}
            @{col="datainiciofuncionario";tipo="DATE";                    nulo="SIM"; desc="Data de admissão"}
            @{col="datafimfuncionario";   tipo="DATE";                    nulo="SIM"; desc="Data de demissão"}
            @{col="[+ campos pessoafisica]"; tipo="—";                   nulo="—";   desc="CPF, nascimento e demais campos"}
        )
        unique = @("matriculafuncionario")
        fks    = @()
    }

    "funcionarioclt" = @{
        descricao = "Funcionário com regime CLT. Extensão de FUNCIONARIO com controle de salário, horas extras e status de emprego."
        colunas   = @(
            @{col="idpessoa";           tipo="INTEGER (PK, FK→funcionario)"; nulo="NÃO"; desc="PK herdada"}
            @{col="salariomensal";      tipo="NUMERIC(12,2)";                nulo="NÃO"; desc="Salário mensal base"}
            @{col="valorhoraextra";     tipo="NUMERIC(12,2)";                nulo="NÃO"; desc="Valor por hora extra"}
            @{col="statusemprego";      tipo="VARCHAR(20)";                  nulo="NÃO"; desc="ATIVO | DESLIGADO | AFASTADO — default ATIVO"}
            @{col="cargahorariadiaria"; tipo="INTEGER";                      nulo="NÃO"; desc="Horas de trabalho por dia — default 8"}
            @{col="[+ campos funcionario]"; tipo="—";                        nulo="—";   desc="Todos os campos de funcionario herdados"}
        )
        unique = @()
        fks    = @("idpessoa → funcionario.idpessoa")
    }

    "funcionariodiarista" = @{
        descricao = "Funcionário diarista. Extensão de FUNCIONARIO com valor por dia trabalhado."
        colunas   = @(
            @{col="idpessoa";      tipo="INTEGER (PK, FK→funcionario)"; nulo="NÃO"; desc="PK herdada"}
            @{col="valorpordia";   tipo="NUMERIC(12,2)";                nulo="NÃO"; desc="Valor recebido por dia trabalhado"}
            @{col="[+ campos funcionario]"; tipo="—";                   nulo="—";   desc="Campos de funcionario herdados"}
        )
        unique = @()
        fks    = @("idpessoa → funcionario.idpessoa")
    }

    "funcionarioempreita" = @{
        descricao = "Funcionário por empreitada. Extensão de FUNCIONARIO com valor fixo acordado por serviço."
        colunas   = @(
            @{col="idpessoa";           tipo="INTEGER (PK, FK→funcionario)"; nulo="NÃO"; desc="PK herdada"}
            @{col="valorfixoacordado";  tipo="NUMERIC(12,2)";                nulo="NÃO"; desc="Valor acordado por empreitada"}
            @{col="[+ campos funcionario]"; tipo="—";                        nulo="—";   desc="Campos de funcionario herdados"}
        )
        unique = @()
        fks    = @("idpessoa → funcionario.idpessoa")
    }

    "funcionarioproducao" = @{
        descricao = "Funcionário por produção. Extensão de FUNCIONARIO com valor por unidade produzida."
        colunas   = @(
            @{col="idpessoa";       tipo="INTEGER (PK, FK→funcionario)"; nulo="NÃO"; desc="PK herdada"}
            @{col="valorporunidade";tipo="NUMERIC(12,2)";                nulo="NÃO"; desc="Valor por unidade produzida"}
            @{col="[+ campos funcionario]"; tipo="—";                    nulo="—";   desc="Campos de funcionario herdados"}
        )
        unique = @()
        fks    = @("idpessoa → funcionario.idpessoa")
    }

    "ponto_eletronico" = @{
        descricao = "Registro diário de ponto eletrônico de funcionários CLT. Armazena duas entradas/saídas por dia com cálculo automático de minutos trabalhados e extras."
        colunas   = @(
            @{col="idponto";       tipo="INTEGER (PK, AUTO)"; nulo="NÃO"; desc="Identificador"}
            @{col="idpessoa";      tipo="INTEGER (FK→funcionario)"; nulo="NÃO"; desc="FK para funcionário"}
            @{col="dataregistro";  tipo="DATE";               nulo="NÃO"; desc="Data do ponto"}
            @{col="entrada1";      tipo="TIME";               nulo="SIM"; desc="Primeiro horário de entrada"}
            @{col="saida1";        tipo="TIME";               nulo="SIM"; desc="Primeiro horário de saída"}
            @{col="entrada2";      tipo="TIME";               nulo="SIM"; desc="Segundo horário de entrada (após almoço)"}
            @{col="saida2";        tipo="TIME";               nulo="SIM"; desc="Segundo horário de saída"}
            @{col="total_minutos"; tipo="INTEGER";            nulo="NÃO"; desc="Total de minutos trabalhados — default 0"}
            @{col="extra_minutos"; tipo="INTEGER";            nulo="NÃO"; desc="Minutos extras além da jornada — default 0"}
            @{col="observacao";    tipo="VARCHAR(200)";       nulo="SIM"; desc="Observações do ponto"}
            @{col="criado_em";     tipo="TIMESTAMP";          nulo="NÃO"; desc="Data/hora de criação — default now()"}
        )
        unique = @("(idpessoa, dataregistro) — UK: um registro por funcionário por dia")
        fks    = @("idpessoa → funcionario.idpessoa")
    }

    "falta_funcionario" = @{
        descricao = "Registro de faltas de funcionários CLT, com flag de justificativa."
        colunas   = @(
            @{col="idfalta";       tipo="INTEGER (PK, AUTO)"; nulo="NÃO"; desc="Identificador"}
            @{col="idpessoa";      tipo="INTEGER (FK→funcionario)"; nulo="NÃO"; desc="FK para funcionário"}
            @{col="datafalta";     tipo="DATE";               nulo="NÃO"; desc="Data da falta"}
            @{col="justificada";   tipo="BOOLEAN";            nulo="NÃO"; desc="true = justificada, false = injustificada — default false"}
            @{col="motivo";        tipo="VARCHAR(200)";       nulo="SIM"; desc="Motivo/descrição da falta"}
            @{col="registrado_em"; tipo="TIMESTAMP";          nulo="NÃO"; desc="Data/hora do registro — default now()"}
        )
        unique = @("(idpessoa, datafalta) — UK: uma falta por funcionário por data")
        fks    = @("idpessoa → funcionario.idpessoa")
    }

    "vale_funcionario" = @{
        descricao = "Controle de vales/adiantamentos concedidos a funcionários CLT, com tipo e status de desconto."
        colunas   = @(
            @{col="idvale";        tipo="INTEGER (PK, AUTO)"; nulo="NÃO"; desc="Identificador"}
            @{col="idpessoa";      tipo="INTEGER (FK→funcionario)"; nulo="NÃO"; desc="FK para funcionário"}
            @{col="datavale";      tipo="DATE";               nulo="NÃO"; desc="Data do vale"}
            @{col="valor";         tipo="NUMERIC(10,2)";      nulo="NÃO"; desc="Valor do vale"}
            @{col="descricao";     tipo="VARCHAR(200)";       nulo="SIM"; desc="Descrição/observação"}
            @{col="tipovale";      tipo="VARCHAR(30)";        nulo="NÃO"; desc="ALIMENTACAO | TRANSPORTE | ADIANTAMENTO | OUTROS — default OUTROS"}
            @{col="statusvale";    tipo="VARCHAR(20)";        nulo="NÃO"; desc="PENDENTE | DESCONTADO — default PENDENTE"}
            @{col="registrado_em"; tipo="TIMESTAMP";          nulo="NÃO"; desc="Data/hora do registro — default now()"}
        )
        unique = @()
        fks    = @("idpessoa → funcionario.idpessoa")
    }

    "fechamento_folha_ponto" = @{
        descricao = "Fechamento mensal da folha de ponto CLT. Consolida salário base, extras, faltas e vales para gerar o salário líquido."
        colunas   = @(
            @{col="idfechamento";         tipo="INTEGER (PK, AUTO)"; nulo="NÃO"; desc="Identificador"}
            @{col="idpessoa";             tipo="INTEGER (FK→funcionario)"; nulo="NÃO"; desc="FK para funcionário CLT"}
            @{col="periodo";              tipo="VARCHAR(7)";          nulo="NÃO"; desc="Período no formato YYYY-MM"}
            @{col="salario_base";         tipo="NUMERIC(10,2)";       nulo="NÃO"; desc="Salário mensal base no período"}
            @{col="jornada_horas_dia";    tipo="INTEGER";             nulo="NÃO"; desc="Carga horária diária — default 8"}
            @{col="dias_uteis";           tipo="INTEGER";             nulo="NÃO"; desc="Quantidade de dias úteis no mês"}
            @{col="dias_trabalhados";     tipo="INTEGER";             nulo="NÃO"; desc="Dias com registro de ponto"}
            @{col="faltas_injustificadas";tipo="INTEGER";             nulo="NÃO"; desc="Número de faltas não justificadas"}
            @{col="total_minutos_extras"; tipo="INTEGER";             nulo="NÃO"; desc="Total de minutos extras acumulados"}
            @{col="valor_horas_extras";   tipo="NUMERIC(10,2)";       nulo="NÃO"; desc="Valor monetário das horas extras"}
            @{col="desconto_faltas";      tipo="NUMERIC(10,2)";       nulo="NÃO"; desc="Desconto proporcional por faltas"}
            @{col="total_vales";          tipo="NUMERIC(10,2)";       nulo="NÃO"; desc="Soma de vales descontados no período"}
            @{col="salario_bruto";        tipo="NUMERIC(10,2)";       nulo="NÃO"; desc="Salário base + extras"}
            @{col="salario_liquido";      tipo="NUMERIC(10,2)";       nulo="NÃO"; desc="Salário bruto − descontos − vales"}
            @{col="fechado_em";           tipo="TIMESTAMP";           nulo="NÃO"; desc="Data/hora do fechamento — default now()"}
        )
        unique = @("(idpessoa, periodo) — UK: um fechamento por funcionário por período")
        fks    = @("idpessoa → funcionario.idpessoa")
    }

    "folha_pagamento" = @{
        descricao = "Registro histórico de pagamentos mensais por tipo de funcionário."
        colunas   = @(
            @{col="idfolha";         tipo="INTEGER (PK, AUTO)"; nulo="NÃO"; desc="Identificador"}
            @{col="idfuncionario";   tipo="INTEGER (FK→funcionario)"; nulo="NÃO"; desc="FK para funcionário"}
            @{col="periodo";         tipo="VARCHAR(7)";          nulo="NÃO"; desc="Período YYYY-MM"}
            @{col="tipofuncionario"; tipo="ENUM";                nulo="NÃO"; desc="CLT | DIARISTA | EMPREITA | PRODUCAO"}
            @{col="valorcalculado";  tipo="NUMERIC(12,2)";       nulo="NÃO"; desc="Valor calculado para pagamento"}
            @{col="datageracao";     tipo="TIMESTAMP";           nulo="NÃO"; desc="Data de geração — default now()"}
        )
        unique = @()
        fks    = @("idfuncionario → funcionario.idpessoa")
    }

    "registroponto" = @{
        descricao = "Registro legado de presença diária de funcionários (versão simplificada do ponto_eletronico)."
        colunas   = @(
            @{col="idregistroponto"; tipo="INTEGER (PK, AUTO)"; nulo="NÃO"; desc="Identificador"}
            @{col="idfuncionario";   tipo="INTEGER (FK→funcionario)"; nulo="NÃO"; desc="FK para funcionário"}
            @{col="dataponto";       tipo="DATE";               nulo="NÃO"; desc="Data do registro"}
            @{col="presente";        tipo="BOOLEAN";            nulo="NÃO"; desc="Presença — default true"}
            @{col="horainicio";      tipo="TIME";               nulo="SIM"; desc="Hora de início"}
            @{col="horafim";         tipo="TIME";               nulo="SIM"; desc="Hora de fim"}
            @{col="horasextras";     tipo="NUMERIC(10,2)";      nulo="SIM"; desc="Horas extras — default 0"}
        )
        unique = @()
        fks    = @("idfuncionario → funcionario.idpessoa")
    }

    "lancamento_producao" = @{
        descricao = "Lançamento mensal de produção para funcionários do tipo PRODUCAO."
        colunas   = @(
            @{col="idlancamento";        tipo="INTEGER (PK, AUTO)"; nulo="NÃO"; desc="Identificador"}
            @{col="idfuncionario";       tipo="INTEGER (FK→funcionarioproducao)"; nulo="NÃO"; desc="FK para funcionário de produção"}
            @{col="periodo";             tipo="VARCHAR(7)";          nulo="NÃO"; desc="Período YYYY-MM"}
            @{col="quantidadeproduzida"; tipo="INTEGER";             nulo="NÃO"; desc="Quantidade de unidades produzidas"}
        )
        unique = @("(idfuncionario, periodo) — UK: um lançamento por funcionário por período")
        fks    = @("idfuncionario → funcionarioproducao.idpessoa")
    }

    "produto" = @{
        descricao = "Tabela base de produtos agrícolas do sistema."
        colunas   = @(
            @{col="idproduto";           tipo="INTEGER (PK, AUTO)"; nulo="NÃO"; desc="Identificador"}
            @{col="nomeproduto";         tipo="VARCHAR(50)";        nulo="NÃO"; desc="Nome do produto"}
            @{col="quantidadeproduto";   tipo="NUMERIC";            nulo="SIM"; desc="Quantidade em estoque"}
            @{col="valorunitarioproduto";tipo="NUMERIC";            nulo="SIM"; desc="Valor unitário de custo"}
            @{col="valorvendaproduto";   tipo="NUMERIC";            nulo="SIM"; desc="Valor de venda"}
            @{col="situacaoproduto";     tipo="BOOLEAN";            nulo="SIM"; desc="Ativo / Inativo"}
            @{col="tipoproduto";         tipo="VARCHAR(50)";        nulo="NÃO"; desc="Tipo/categoria do produto"}
        )
        unique = @()
        fks    = @()
    }

    "alimento" = @{
        descricao = "Extensão de PRODUTO para alimentos agrícolas, com campo adicional de variedade."
        colunas   = @(
            @{col="idproduto";       tipo="INTEGER (PK UNIQUE, FK→produto)"; nulo="NÃO"; desc="PK herdada de produto"}
            @{col="variedadealimento";tipo="VARCHAR(50)";                     nulo="NÃO"; desc="Variedade do alimento (ex: Gala, Fuji)"}
            @{col="[+ campos produto]";tipo="—";                             nulo="—";   desc="Campos de produto herdados"}
        )
        unique = @("idproduto")
        fks    = @()
    }

    "classificacao" = @{
        descricao = "Categorias de classificação aplicadas aos alimentos (ex: Orgânico, Premium, Descarte)."
        colunas   = @(
            @{col="idclassificacao"; tipo="INTEGER (PK, AUTO)"; nulo="NÃO"; desc="Identificador"}
            @{col="classificacao";   tipo="VARCHAR(50)";        nulo="NÃO"; desc="Nome da classificação"}
        )
        unique = @()
        fks    = @()
    }

    "alimentoclassificacao" = @{
        descricao = "Tabela associativa N:N entre ALIMENTO e CLASSIFICACAO."
        colunas   = @(
            @{col="idalimentoclassificacao"; tipo="INTEGER (PK, AUTO)"; nulo="NÃO"; desc="Identificador da associação"}
            @{col="idproduto";               tipo="INTEGER (FK→alimento)"; nulo="SIM"; desc="FK para alimento"}
            @{col="idclassificacao";         tipo="INTEGER (FK→classificacao)"; nulo="SIM"; desc="FK para classificação"}
        )
        unique = @()
        fks    = @("idproduto → alimento.idproduto", "idclassificacao → classificacao.idclassificacao")
    }

    "areaproducao" = @{
        descricao = "Áreas de produção agrícola (talhões, glebas) com dados de localização e capacidade."
        colunas   = @(
            @{col="idareaproducao";                tipo="INTEGER (PK, AUTO)"; nulo="NÃO"; desc="Identificador"}
            @{col="propriedadeareaproducao";       tipo="VARCHAR(50)";        nulo="NÃO"; desc="Nome da propriedade"}
            @{col="proprietarioareaproducao";      tipo="VARCHAR(50)";        nulo="NÃO"; desc="Nome do proprietário"}
            @{col="quantidadetotalplantasareaproducao";tipo="INTEGER";        nulo="NÃO"; desc="Capacidade total de plantas"}
            @{col="siglasareaproducao";            tipo="VARCHAR(4)";         nulo="NÃO"; desc="Sigla identificadora (ex: A1)"}
            @{col="cep";                           tipo="VARCHAR(10)";        nulo="NÃO"; desc="CEP da área"}
            @{col="numero";                        tipo="INTEGER";            nulo="NÃO"; desc="Número do endereço"}
            @{col="complemento";                   tipo="VARCHAR(50)";        nulo="NÃO"; desc="Complemento do endereço"}
        )
        unique = @()
        fks    = @()
    }

    "talao" = @{
        descricao = "Talão de produção — vincula uma área de produção a um produto/alimento colhido."
        colunas   = @(
            @{col="idtalao";                   tipo="INTEGER (PK, AUTO)"; nulo="NÃO"; desc="Identificador"}
            @{col="descricaotalao";            tipo="VARCHAR(50)";        nulo="NÃO"; desc="Descrição do talão"}
            @{col="idproduto";                 tipo="INTEGER (FK→alimento)"; nulo="SIM"; desc="FK para o alimento"}
            @{col="idareaproducao";            tipo="INTEGER (FK→areaproducao)"; nulo="SIM"; desc="FK para a área de produção"}
            @{col="quantidadeplantastalao";    tipo="INTEGER";            nulo="NÃO"; desc="Quantidade de plantas associadas ao talão"}
        )
        unique = @()
        fks    = @("idproduto → alimento.idproduto", "idareaproducao → areaproducao.idareaproducao")
    }

    "talaofinanceiro" = @{
        descricao = "Dados financeiros de uma safra vinculada a um talão — custos, despesas e receitas da safra."
        colunas   = @(
            @{col="idtalaofinanceiro";             tipo="INTEGER (PK, AUTO)"; nulo="NÃO"; desc="Identificador"}
            @{col="idtalao";                       tipo="INTEGER (FK→talao)"; nulo="NÃO"; desc="FK para talão"}
            @{col="safratalaofinanceiro";          tipo="VARCHAR(50)";        nulo="NÃO"; desc="Nome/identificação da safra"}
            @{col="iniciosafratalaofinanceiro";    tipo="DATE";               nulo="NÃO"; desc="Data de início da safra"}
            @{col="terminosafratalaofinanceiro";   tipo="DATE";               nulo="NÃO"; desc="Data de término da safra"}
            @{col="custosafratalaofinanceiro";     tipo="NUMERIC(10,2)";      nulo="NÃO"; desc="Custo total da safra"}
            @{col="despesassafratalaofinanceiro";  tipo="NUMERIC(10,2)";      nulo="NÃO"; desc="Despesas gerais da safra"}
            @{col="vendabrutasafratalaofinanceiro";tipo="NUMERIC(10,2)";      nulo="NÃO"; desc="Receita bruta de venda"}
            @{col="vendaliquidasafratalaofinanceiro";tipo="NUMERIC(10,2)";    nulo="NÃO"; desc="Receita líquida (bruta − deduções)"}
        )
        unique = @()
        fks    = @("idtalao → talao.idtalao")
    }

    "despesascusto" = @{
        descricao = "Catálogo de tipos de despesas e custos operacionais da fazenda."
        colunas   = @(
            @{col="iddespesascusto";        tipo="INTEGER (PK, AUTO)"; nulo="NÃO"; desc="Identificador"}
            @{col="despesascusto";          tipo="VARCHAR(50)";        nulo="NÃO"; desc="Descrição da despesa"}
            @{col="unidadedespesascustos";  tipo="VARCHAR(2)";         nulo="NÃO"; desc="Unidade de medida (kg, L, un...)"}
            @{col="valordespesascustos";    tipo="NUMERIC(10,2)";      nulo="NÃO"; desc="Valor unitário da despesa"}
            @{col="tipodespesascustos";     tipo="VARCHAR(20)";        nulo="NÃO"; desc="Tipo: CUSTO | DESPESA"}
        )
        unique = @()
        fks    = @()
    }

    "lancamentotalao" = @{
        descricao = "Lançamentos de despesas/custos vinculados a um talão (N:N entre talão e despesascusto)."
        colunas   = @(
            @{col="idlancalancamentodespesascustos"; tipo="INTEGER (PK, AUTO)"; nulo="NÃO"; desc="Identificador"}
            @{col="iddespesascusto"; tipo="INTEGER (FK→despesascusto)"; nulo="SIM"; desc="FK para tipo de despesa"}
            @{col="valorlancalancamentodespesascustos"; tipo="NUMERIC(10,2)"; nulo="SIM"; desc="Valor lançado"}
        )
        unique = @()
        fks    = @("iddespesascusto → despesascusto.iddespesascusto")
    }
}

$ordemTabelas = @(
    "pessoa","pessoafisica","pessoacnpj","parceiro",
    "funcionario","funcionarioclt","funcionariodiarista","funcionarioempreita","funcionarioproducao",
    "ponto_eletronico","registroponto","falta_funcionario","vale_funcionario",
    "fechamento_folha_ponto","folha_pagamento","lancamento_producao",
    "produto","alimento","classificacao","alimentoclassificacao",
    "areaproducao","talao","talaofinanceiro",
    "despesascusto","lancamentotalao"
)

# =============================================================================
# CRIAR DOCUMENTO WORD VIA COM
# =============================================================================
Write-Host "Iniciando geração do documento Word..."

$word = New-Object -ComObject Word.Application
$word.Visible = $false
$doc = $word.Documents.Add()
$sel = $word.Selection

# ---- Estilos de cor ----
$corVerde       = 0x1D6B3E  # verde escuro RGB invertido para OLE
$corVerdeClaro  = 0x4CAF50
$corBranco      = 0xFFFFFF
$corCinza       = 0xF5F5F5
$corCinzaEscuro = 0x616161
$corTexto       = 0x212121

function Set-FontStyle($range, $size, $bold, $color, $name = "Calibri") {
    $range.Font.Name  = $name
    $range.Font.Size  = $size
    $range.Font.Bold  = $bold
    $range.Font.Color = $color
}

function Add-Heading1($sel, $texto) {
    $sel.Style = $sel.Document.Styles["Título 1"]
    $sel.TypeText($texto)
    $sel.TypeParagraph()
}

function Add-Heading2($sel, $texto) {
    $sel.Style = $sel.Document.Styles["Título 2"]
    $sel.TypeText($texto)
    $sel.TypeParagraph()
}

function Add-Paragraph($sel, $texto) {
    $sel.Style = $sel.Document.Styles["Normal"]
    $sel.TypeText($texto)
    $sel.TypeParagraph()
}

# ============== CAPA ==============
$sel.Style = $doc.Styles["Normal"]

# Logo / título principal
$r = $sel.Range
$r.Text = "AGRO TECH ONE"
$r.Font.Name = "Calibri"
$r.Font.Size = 36
$r.Font.Bold = $true
$r.Font.Color = 0x1E6B3D
$r.ParagraphFormat.Alignment = 1  # Centro
$sel.MoveEnd()
$sel.Collapse(0)
$sel.TypeParagraph()

$sel.TypeText("Sistema de Gestão Agrícola")
$sel.Font.Size = 18
$sel.Font.Bold = $false
$sel.Font.Color = 0x424242
$sel.ParagraphFormat.Alignment = 1
$sel.TypeParagraph()
$sel.TypeParagraph()

$sel.TypeText("DOCUMENTAÇÃO DO BANCO DE DADOS")
$sel.Font.Size = 22
$sel.Font.Bold = $true
$sel.Font.Color = 0x1E6B3D
$sel.TypeParagraph()

$sel.TypeText("Engenharia Reversa — Modelagem e Diagrama ER")
$sel.Font.Size = 14
$sel.Font.Bold = $false
$sel.Font.Color = 0x616161
$sel.TypeParagraph()
$sel.TypeParagraph()

$sel.TypeText("Banco de Dados: PostgreSQL 17  •  Schema: public")
$sel.Font.Size = 11
$sel.Font.Color = 0x424242
$sel.TypeParagraph()

$sel.TypeText("Gerado em: " + (Get-Date -Format "dd/MM/yyyy 'às' HH:mm"))
$sel.Font.Size = 11
$sel.TypeParagraph()
$sel.TypeParagraph()

# Linha divisória
$brd = $sel.Borders(0)  # wdBorderBottom não disponível facilmente, usamos parágrafo
$sel.TypeParagraph()

# Reset alinhamento
$sel.ParagraphFormat.Alignment = 0
$sel.Font.Color = 0x000000
$sel.Font.Bold = $false
$sel.Font.Size = 11

$sel.InsertBreak(7)  # Quebra de página

# ============== ÍNDICE ==============
$sel.Style = $doc.Styles["Normal"]
$sel.Font.Name  = "Calibri"
$sel.Font.Size  = 16
$sel.Font.Bold  = $true
$sel.Font.Color = 0x1E6B3D
$sel.ParagraphFormat.Alignment = 0
$sel.TypeText("SUMÁRIO")
$sel.TypeParagraph()
$sel.Font.Bold  = $false
$sel.Font.Size  = 11
$sel.Font.Color = 0x000000

$sections = @(
    "1. Visão Geral do Banco de Dados",
    "2. Arquitetura e Herança de Tabelas",
    "3. Módulos do Sistema",
    "   3.1 Módulo Pessoa / Parceiro",
    "   3.2 Módulo Funcionários (RH)",
    "   3.3 Módulo Ponto e Folha",
    "   3.4 Módulo Produção Agrícola",
    "   3.5 Módulo Financeiro",
    "4. Dicionário de Dados — Tabelas",
    "5. Relacionamentos (Foreign Keys)",
    "6. Restrições e Índices",
    "7. Enumerações (ENUMs)",
    "8. Diagrama ER Textual"
)
foreach ($s in $sections) {
    $sel.TypeText($s)
    $sel.TypeParagraph()
}

$sel.InsertBreak(7)

# ============== SEÇÃO 1: VISÃO GERAL ==============
$sel.Font.Name  = "Calibri"
$sel.Font.Size  = 16
$sel.Font.Bold  = $true
$sel.Font.Color = 0x1E6B3D
$sel.TypeText("1. VISÃO GERAL DO BANCO DE DADOS")
$sel.TypeParagraph()
$sel.Font.Bold  = $false
$sel.Font.Size  = 11
$sel.Font.Color = 0x000000

$sel.TypeText("Nome do banco: agro")
$sel.TypeParagraph()
$sel.TypeText("SGBD: PostgreSQL 17")
$sel.TypeParagraph()
$sel.TypeText("Host: localhost:5432")
$sel.TypeParagraph()
$sel.TypeText("Schema: public")
$sel.TypeParagraph()
$sel.TypeText("Total de tabelas: 25")
$sel.TypeParagraph()
$sel.TypeParagraph()

$sel.TypeText("O banco de dados do sistema Agro Tech One foi projetado para suportar a gestão completa de uma propriedade agrícola, contemplando os seguintes domínios funcionais:")
$sel.TypeParagraph()
$sel.TypeParagraph()

$dominios = @(
    "• Cadastro de Pessoas: clientes, parceiros, funcionários e usuários",
    "• Recursos Humanos: controle de funcionários CLT, diaristas, empreiteiros e produção",
    "• Ponto Eletrônico: registro de entrada/saída com cálculo automático de horas extras",
    "• Folha de Pagamento: fechamento mensal com vales, faltas e horas extras",
    "• Produção Agrícola: áreas de produção, talões de colheita e safras",
    "• Estoque/Produtos: catálogo de alimentos com classificações",
    "• Financeiro: despesas, custos e receitas por safra"
)
foreach ($d in $dominios) {
    $sel.TypeText($d)
    $sel.TypeParagraph()
}

$sel.TypeParagraph()
$sel.InsertBreak(7)

# ============== SEÇÃO 2: ARQUITETURA ==============
$sel.Font.Size  = 16
$sel.Font.Bold  = $true
$sel.Font.Color = 0x1E6B3D
$sel.TypeText("2. ARQUITETURA E HERANÇA DE TABELAS")
$sel.TypeParagraph()
$sel.Font.Bold  = $false
$sel.Font.Size  = 11
$sel.Font.Color = 0x000000

$sel.TypeText("O banco utiliza uma estratégia de herança de tabelas (Table-Per-Type) inspirada no modelo de herança do PostgreSQL. As entidades principais herdam campos das tabelas-pai através de colunas duplicadas (desnormalização intencional para performance), com a identidade compartilhada pela sequência pessoa_idpessoa_seq.")
$sel.TypeParagraph()
$sel.TypeParagraph()

$hierarquia = @(
    "HIERARQUIA DE HERANÇA:",
    "",
    "pessoa  (base — idpessoa, nome, email, telefone, endereço)",
    "   ├── pessoafisica    (+ CPF, data nascimento)",
    "   │       └── funcionario   (+ matrícula, tipo, cargo, datas)",
    "   │               ├── funcionarioclt       (+ salário, hora extra, status)",
    "   │               ├── funcionariodiarista  (+ valor/dia)",
    "   │               ├── funcionarioempreita  (+ valor fixo acordado)",
    "   │               └── funcionarioproducao  (+ valor/unidade)",
    "   └── pessoacnpj      (+ CNPJ, razão social, inscrição estadual)",
    "           └── parceiro        (+ site parceiro)",
    "",
    "produto (base — nome, quantidade, valores, tipo)",
    "   └── alimento  (+ variedade)"
)
foreach ($h in $hierarquia) {
    if ($h -eq "") { $sel.TypeParagraph(); continue }
    $sel.Font.Name = "Courier New"
    $sel.Font.Size = 9
    $sel.TypeText($h)
    $sel.TypeParagraph()
}
$sel.Font.Name = "Calibri"
$sel.Font.Size = 11
$sel.TypeParagraph()
$sel.InsertBreak(7)

# ============== SEÇÃO 3: MÓDULOS ==============
$sel.Font.Size  = 16
$sel.Font.Bold  = $true
$sel.Font.Color = 0x1E6B3D
$sel.TypeText("3. MÓDULOS DO SISTEMA")
$sel.TypeParagraph()
$sel.Font.Bold  = $false
$sel.Font.Size  = 11
$sel.Font.Color = 0x000000

$modulos = @(
    @{ titulo="3.1 Módulo Pessoa / Parceiro"; desc="Gerencia todos os cadastros de pessoas do sistema. Utiliza herança para diferenciar pessoa física (pessoafisica) de pessoa jurídica (pessoacnpj). Os parceiros comerciais estendem pessoacnpj adicionando o campo de site. Esta estrutura permite que o sistema mantenha um identificador único (idpessoa) para todos os cadastros, independente do tipo."; tabelas="pessoa, pessoafisica, pessoacnpj, parceiro" }
    @{ titulo="3.2 Módulo Funcionários (RH)"; desc="Controla o quadro de funcionários dividido em 4 regimes: CLT (salário fixo + extras), Diarista (valor por dia), Empreita (valor fixo por serviço) e Produção (valor por unidade produzida). Todos herdam de funcionario que por sua vez herda de pessoafisica. O tipo é discriminado pelo campo ENUM tipofuncionario."; tabelas="funcionario, funcionarioclt, funcionariodiarista, funcionarioempreita, funcionarioproducao" }
    @{ titulo="3.3 Módulo Ponto e Folha"; desc="Registra o ponto eletrônico diário (duas entradas e duas saídas), calcula minutos trabalhados e extras automaticamente. O fechamento mensal consolida salário base + horas extras - desconto de faltas - vales para gerar o salário líquido. Vales e faltas são controlados independentemente por funcionário."; tabelas="ponto_eletronico, registroponto, falta_funcionario, vale_funcionario, fechamento_folha_ponto, folha_pagamento, lancamento_producao" }
    @{ titulo="3.4 Módulo Produção Agrícola"; desc="Gerencia as áreas de produção (talhões), os talões de colheita e os alimentos produzidos. Cada talão vincula uma área de produção a um alimento específico, com controle de quantidade de plantas. Os alimentos podem ter múltiplas classificações (Orgânico, Premium, etc.) através da tabela associativa alimentoclassificacao."; tabelas="areaproducao, talao, produto, alimento, classificacao, alimentoclassificacao" }
    @{ titulo="3.5 Módulo Financeiro"; desc="Registra os resultados financeiros de cada safra (custo, despesas, venda bruta e líquida) vinculada a um talão. O catálogo de despesascusto permite lançar custos operacionais através da tabela lancamentotalao."; tabelas="talaofinanceiro, despesascusto, lancamentotalao" }
)

foreach ($m in $modulos) {
    $sel.Font.Size  = 13
    $sel.Font.Bold  = $true
    $sel.Font.Color = 0x2E7D32
    $sel.TypeText($m.titulo)
    $sel.TypeParagraph()
    $sel.Font.Bold  = $false
    $sel.Font.Size  = 11
    $sel.Font.Color = 0x000000
    $sel.TypeText($m.desc)
    $sel.TypeParagraph()
    $sel.Font.Italic = $true
    $sel.Font.Color  = 0x616161
    $sel.TypeText("Tabelas: " + $m.tabelas)
    $sel.Font.Italic = $false
    $sel.Font.Color  = 0x000000
    $sel.TypeParagraph()
    $sel.TypeParagraph()
}

$sel.InsertBreak(7)

# ============== SEÇÃO 4: DICIONÁRIO DE DADOS ==============
$sel.Font.Size  = 16
$sel.Font.Bold  = $true
$sel.Font.Color = 0x1E6B3D
$sel.TypeText("4. DICIONÁRIO DE DADOS")
$sel.TypeParagraph()
$sel.Font.Bold  = $false
$sel.Font.Size  = 11
$sel.Font.Color = 0x000000
$sel.TypeText("Documentação detalhada de todas as 25 tabelas do banco de dados.")
$sel.TypeParagraph()
$sel.TypeParagraph()

$i = 1
foreach ($nomeTabela in $ordemTabelas) {
    if (-not $schema.ContainsKey($nomeTabela)) { $i++; continue }
    $t = $schema[$nomeTabela]

    # Cabeçalho da tabela
    $sel.Font.Size  = 13
    $sel.Font.Bold  = $true
    $sel.Font.Color = 0x1B5E20
    $sel.TypeText("4.$i — $($nomeTabela.ToUpper())")
    $sel.TypeParagraph()
    $sel.Font.Bold  = $false
    $sel.Font.Size  = 11
    $sel.Font.Color = 0x424242
    $sel.TypeText($t.descricao)
    $sel.TypeParagraph()
    $sel.TypeParagraph()

    # Tabela Word com colunas
    $tabWord = $doc.Tables.Add($sel.Range, 1, 4)
    $tabWord.Style = "Tabela com grade"
    try { $tabWord.Style = "Table Grid" } catch {}

    # Larguras
    $tabWord.Columns(1).Width = $word.CentimetersToPoints(4.0)
    $tabWord.Columns(2).Width = $word.CentimetersToPoints(4.5)
    $tabWord.Columns(3).Width = $word.CentimetersToPoints(2.0)
    $tabWord.Columns(4).Width = $word.CentimetersToPoints(6.0)

    # Cabeçalho
    $headers = @("Coluna","Tipo de Dado","Nulo?","Descrição")
    for ($c = 1; $c -le 4; $c++) {
        $cell = $tabWord.Cell(1, $c)
        $cell.Range.Text = $headers[$c - 1]
        $cell.Range.Font.Bold = $true
        $cell.Range.Font.Size = 9
        $cell.Range.Font.Color = 0xFFFFFF
        $cell.Shading.BackgroundPatternColor = 0x2E7D32
    }

    # Linhas de dados
    foreach ($col in $t.colunas) {
        $row = $tabWord.Rows.Add()
        $rowIdx = $tabWord.Rows.Count
        $tabWord.Cell($rowIdx, 1).Range.Text = $col.col
        $tabWord.Cell($rowIdx, 2).Range.Text = $col.tipo
        $tabWord.Cell($rowIdx, 3).Range.Text = $col.nulo
        $tabWord.Cell($rowIdx, 4).Range.Text = $col.desc

        # Fonte das células
        for ($c = 1; $c -le 4; $c++) {
            $tabWord.Cell($rowIdx, $c).Range.Font.Size = 9
            $tabWord.Cell($rowIdx, $c).Range.Font.Bold = $false
        }
        $tabWord.Cell($rowIdx, 1).Range.Font.Bold = $true

        # Linhas alternadas
        if ($rowIdx % 2 -eq 0) {
            for ($c = 1; $c -le 4; $c++) {
                $tabWord.Cell($rowIdx, $c).Shading.BackgroundPatternColor = 0xF1F8E9
            }
        }
    }

    # Mover cursor para após a tabela
    $sel.MoveEnd()
    $sel.Collapse(0)
    $sel.TypeParagraph()

    # FKs
    if ($t.fks.Count -gt 0) {
        $sel.Font.Size   = 9
        $sel.Font.Bold   = $true
        $sel.Font.Color  = 0x1565C0
        $sel.TypeText("Foreign Keys: ")
        $sel.Font.Bold   = $false
        $sel.TypeText(($t.fks -join " | "))
        $sel.TypeParagraph()
        $sel.Font.Color = 0x000000
    }

    # UNIQUEs
    if ($t.unique.Count -gt 0) {
        $sel.Font.Size  = 9
        $sel.Font.Bold  = $true
        $sel.Font.Color = 0xE65100
        $sel.TypeText("Restrições UNIQUE: ")
        $sel.Font.Bold  = $false
        $sel.TypeText(($t.unique -join " | "))
        $sel.TypeParagraph()
        $sel.Font.Color = 0x000000
    }

    $sel.Font.Size = 11
    $sel.TypeParagraph()
    $i++
}

$sel.InsertBreak(7)

# ============== SEÇÃO 5: RELACIONAMENTOS ==============
$sel.Font.Size  = 16
$sel.Font.Bold  = $true
$sel.Font.Color = 0x1E6B3D
$sel.TypeText("5. RELACIONAMENTOS (FOREIGN KEYS)")
$sel.TypeParagraph()
$sel.Font.Bold  = $false
$sel.Font.Size  = 11
$sel.Font.Color = 0x000000

$fkList = @(
    @{ origem="alimentoclassificacao"; col="idproduto"; dest="alimento"; dcol="idproduto"; card="N:1"; desc="Cada classificação pertence a um alimento" }
    @{ origem="alimentoclassificacao"; col="idclassificacao"; dest="classificacao"; dcol="idclassificacao"; card="N:1"; desc="Cada registro tem uma classificação" }
    @{ origem="falta_funcionario"; col="idpessoa"; dest="funcionario"; dcol="idpessoa"; card="N:1"; desc="Faltas pertencem a um funcionário" }
    @{ origem="fechamento_folha_ponto"; col="idpessoa"; dest="funcionario"; dcol="idpessoa"; card="N:1"; desc="Fechamentos pertencem a um funcionário" }
    @{ origem="folha_pagamento"; col="idfuncionario"; dest="funcionario"; dcol="idpessoa"; card="N:1"; desc="Pagamentos pertencem a um funcionário" }
    @{ origem="lancamento_producao"; col="idfuncionario"; dest="funcionarioproducao"; dcol="idpessoa"; card="N:1"; desc="Lançamentos pertencem a funcionário de produção" }
    @{ origem="lancamentotalao"; col="iddespesascusto"; dest="despesascusto"; dcol="iddespesascusto"; card="N:1"; desc="Lançamentos referenciam tipo de despesa" }
    @{ origem="ponto_eletronico"; col="idpessoa"; dest="funcionario"; dcol="idpessoa"; card="N:1"; desc="Pontos pertencem a um funcionário" }
    @{ origem="registroponto"; col="idfuncionario"; dest="funcionario"; dcol="idpessoa"; card="N:1"; desc="Registros de ponto pertencem a um funcionário" }
    @{ origem="talao"; col="idproduto"; dest="alimento"; dcol="idproduto"; card="N:1"; desc="Talão referencia um alimento colhido" }
    @{ origem="talao"; col="idareaproducao"; dest="areaproducao"; dcol="idareaproducao"; card="N:1"; desc="Talão pertence a uma área de produção" }
    @{ origem="talaofinanceiro"; col="idtalao"; dest="talao"; dcol="idtalao"; card="N:1"; desc="Dados financeiros pertencem a um talão" }
    @{ origem="vale_funcionario"; col="idpessoa"; dest="funcionario"; dcol="idpessoa"; card="N:1"; desc="Vales pertencem a um funcionário" }
)

$tabFk = $doc.Tables.Add($sel.Range, 1, 5)
try { $tabFk.Style = "Table Grid" } catch {}
$tabFk.Columns(1).Width = $word.CentimetersToPoints(3.5)
$tabFk.Columns(2).Width = $word.CentimetersToPoints(3.0)
$tabFk.Columns(3).Width = $word.CentimetersToPoints(3.5)
$tabFk.Columns(4).Width = $word.CentimetersToPoints(1.5)
$tabFk.Columns(5).Width = $word.CentimetersToPoints(5.0)

$hdrsFk = @("Tabela Origem","Coluna FK","Tabela Destino","Cardinalidade","Descrição")
for ($c = 1; $c -le 5; $c++) {
    $tabFk.Cell(1,$c).Range.Text  = $hdrsFk[$c-1]
    $tabFk.Cell(1,$c).Range.Font.Bold = $true
    $tabFk.Cell(1,$c).Range.Font.Size = 9
    $tabFk.Cell(1,$c).Range.Font.Color = 0xFFFFFF
    $tabFk.Cell(1,$c).Shading.BackgroundPatternColor = 0x1565C0
}

$ri = 1
foreach ($fk in $fkList) {
    $ri++
    $row = $tabFk.Rows.Add()
    $tabFk.Cell($ri,1).Range.Text = $fk.origem
    $tabFk.Cell($ri,2).Range.Text = "$($fk.col) → $($fk.dcol)"
    $tabFk.Cell($ri,3).Range.Text = $fk.dest
    $tabFk.Cell($ri,4).Range.Text = $fk.card
    $tabFk.Cell($ri,5).Range.Text = $fk.desc
    for ($c = 1; $c -le 5; $c++) { $tabFk.Cell($ri,$c).Range.Font.Size = 9 }
    if ($ri % 2 -eq 0) {
        for ($c = 1; $c -le 5; $c++) {
            $tabFk.Cell($ri,$c).Shading.BackgroundPatternColor = 0xE3F2FD
        }
    }
}

$sel.MoveEnd()
$sel.Collapse(0)
$sel.TypeParagraph()
$sel.InsertBreak(7)

# ============== SEÇÃO 6: ÍNDICES ==============
$sel.Font.Size  = 16
$sel.Font.Bold  = $true
$sel.Font.Color = 0x1E6B3D
$sel.TypeText("6. RESTRIÇÕES E ÍNDICES")
$sel.TypeParagraph()
$sel.Font.Bold  = $false
$sel.Font.Size  = 11
$sel.Font.Color = 0x000000

$indices = @(
    @{tab="pessoa";              idx="pessoa_emailpessoa_key";                  tipo="UNIQUE"; cols="emailpessoa"}
    @{tab="pessoafisica";        idx="pessoafisica_cpfpf_key";                  tipo="UNIQUE"; cols="cpfpf"}
    @{tab="pessoacnpj";          idx="pessoacnpj_cnpjpessoacnpj_key";           tipo="UNIQUE"; cols="cnpjpessoacnpj"}
    @{tab="pessoacnpj";          idx="pessoacnpj_razaosocialpessoacnpj_key";    tipo="UNIQUE"; cols="razaosocialpessoacnpj"}
    @{tab="pessoacnpj";          idx="pessoacnpj_inscricaoestadualpessoacnpj_key"; tipo="UNIQUE"; cols="inscricaoestadualpessoacnpj"}
    @{tab="parceiro";            idx="parceiro_siteparceiro_key";               tipo="UNIQUE"; cols="siteparceiro"}
    @{tab="funcionario";         idx="funcionario_matriculafuncionario_key";    tipo="UNIQUE"; cols="matriculafuncionario"}
    @{tab="alimento";            idx="alimento_idproduto_unique";               tipo="UNIQUE"; cols="idproduto"}
    @{tab="ponto_eletronico";    idx="uk_ponto_funcionario_dia";                tipo="UNIQUE COMPOSTO"; cols="(idpessoa, dataregistro)"}
    @{tab="falta_funcionario";   idx="uk_falta_funcionario_dia";                tipo="UNIQUE COMPOSTO"; cols="(idpessoa, datafalta)"}
    @{tab="fechamento_folha_ponto"; idx="uk_fechamento_func_periodo";           tipo="UNIQUE COMPOSTO"; cols="(idpessoa, periodo)"}
    @{tab="lancamento_producao"; idx="lancamento_producao_idfuncionario_periodo_key"; tipo="UNIQUE COMPOSTO"; cols="(idfuncionario, periodo)"}
)

$tabIdx = $doc.Tables.Add($sel.Range, 1, 4)
try { $tabIdx.Style = "Table Grid" } catch {}
$tabIdx.Columns(1).Width = $word.CentimetersToPoints(3.5)
$tabIdx.Columns(2).Width = $word.CentimetersToPoints(5.5)
$tabIdx.Columns(3).Width = $word.CentimetersToPoints(3.5)
$tabIdx.Columns(4).Width = $word.CentimetersToPoints(4.0)

$hdrsIdx = @("Tabela","Nome do Índice","Tipo","Colunas")
for ($c = 1; $c -le 4; $c++) {
    $tabIdx.Cell(1,$c).Range.Text  = $hdrsIdx[$c-1]
    $tabIdx.Cell(1,$c).Range.Font.Bold = $true
    $tabIdx.Cell(1,$c).Range.Font.Size = 9
    $tabIdx.Cell(1,$c).Range.Font.Color = 0xFFFFFF
    $tabIdx.Cell(1,$c).Shading.BackgroundPatternColor = 0xE65100
}

$ri = 1
foreach ($idx in $indices) {
    $ri++
    $r2 = $tabIdx.Rows.Add()
    $tabIdx.Cell($ri,1).Range.Text = $idx.tab
    $tabIdx.Cell($ri,2).Range.Text = $idx.idx
    $tabIdx.Cell($ri,3).Range.Text = $idx.tipo
    $tabIdx.Cell($ri,4).Range.Text = $idx.cols
    for ($c = 1; $c -le 4; $c++) { $tabIdx.Cell($ri,$c).Range.Font.Size = 9 }
    if ($ri % 2 -eq 0) {
        for ($c = 1; $c -le 4; $c++) {
            $tabIdx.Cell($ri,$c).Shading.BackgroundPatternColor = 0xFFF3E0
        }
    }
}

$sel.MoveEnd()
$sel.Collapse(0)
$sel.TypeParagraph()
$sel.InsertBreak(7)

# ============== SEÇÃO 7: ENUMs ==============
$sel.Font.Size  = 16
$sel.Font.Bold  = $true
$sel.Font.Color = 0x1E6B3D
$sel.TypeText("7. ENUMERAÇÕES (ENUMs)")
$sel.TypeParagraph()
$sel.Font.Bold  = $false
$sel.Font.Size  = 11
$sel.Font.Color = 0x000000

$enums = @(
    @{ nome="tipo_funcionario"; tabelas="funcionario, funcionarioclt, funcionariodiarista, funcionarioempreita, funcionarioproducao, folha_pagamento"; valores="CLT — Regime CLT com salário fixo mensal|DIARISTA — Pagamento por dia trabalhado|EMPREITA — Pagamento fixo por serviço contratado|PRODUCAO — Pagamento por unidade produzida" }
    @{ nome="statusemprego (VARCHAR)"; tabelas="funcionarioclt"; valores="ATIVO — Funcionário em atividade normal|DESLIGADO — Funcionário demitido (mantém histórico)|AFASTADO — Funcionário temporariamente afastado" }
    @{ nome="tipovale (VARCHAR)"; tabelas="vale_funcionario"; valores="ALIMENTACAO — Vale alimentação|TRANSPORTE — Vale transporte|ADIANTAMENTO — Adiantamento de salário|OUTROS — Outros tipos de vale" }
    @{ nome="statusvale (VARCHAR)"; tabelas="vale_funcionario"; valores="PENDENTE — Vale concedido aguardando desconto|DESCONTADO — Vale já descontado no fechamento" }
    @{ nome="tipodespesascustos (VARCHAR)"; tabelas="despesascusto"; valores="CUSTO — Custo direto de produção|DESPESA — Despesa operacional" }
)

foreach ($en in $enums) {
    $sel.Font.Size  = 12
    $sel.Font.Bold  = $true
    $sel.Font.Color = 0x4A148C
    $sel.TypeText($en.nome)
    $sel.TypeParagraph()
    $sel.Font.Bold  = $false
    $sel.Font.Size  = 10
    $sel.Font.Color = 0x616161
    $sel.TypeText("Usado em: " + $en.tabelas)
    $sel.TypeParagraph()
    $sel.Font.Color = 0x000000
    foreach ($v in $en.valores -split '\|') {
        $sel.TypeText("  • " + $v)
        $sel.TypeParagraph()
    }
    $sel.TypeParagraph()
}

$sel.InsertBreak(7)

# ============== SEÇÃO 8: DIAGRAMA ER TEXTUAL ==============
$sel.Font.Size  = 16
$sel.Font.Bold  = $true
$sel.Font.Color = 0x1E6B3D
$sel.TypeText("8. DIAGRAMA ER TEXTUAL")
$sel.TypeParagraph()
$sel.Font.Bold  = $false
$sel.Font.Size  = 10
$sel.Font.Color = 0x212121

$sel.TypeText("Representação textual das entidades e relacionamentos principais do banco de dados Agro Tech One.")
$sel.TypeParagraph()
$sel.TypeParagraph()

$er = @'
┌──────────────────────────────────────────────────────────────────────────────┐
│                        AGRO TECH ONE — DIAGRAMA ER                          │
└──────────────────────────────────────────────────────────────────────────────┘

╔══════════════╗     herda      ╔══════════════╗     herda      ╔══════════════╗
║    PESSOA    ║ ──────────────>║ PESSOA FÍSICA║──────────────>║ FUNCIONARIO  ║
║──────────────║                ║──────────────║                ║──────────────║
║ PK idpessoa ║                ║ cpfpf (UK)   ║                ║ matricula(UK)║
║ nomepessoa  ║                ║ datanasc     ║                ║ tipofunc ENUM║
║ email (UK)  ║                ╚══════════════╝                ║ cargo        ║
║ telefone    ║                                                 ╚══════╤═══════╝
║ cep/endereço║                ╔══════════════╗     herda             │
╚══════════════╝               ║ PESSOA CNPJ  ║──────────────>╔══════╧════════╗
        │   herda              ║──────────────║               ║   PARCEIRO    ║
        └─────────────────────>║ cnpj (UK)    ║               ╚═══════════════╝
                               ║ razaosocial  ║
                               ╚══════════════╝
                                                        ┌──── CLT (salário, extras, status)
                                                        ├──── DIARISTA (valor/dia)
                    FUNCIONARIO ────── especializa em ──┤
                                                        ├──── EMPREITA (valor fixo)
                                                        └──── PRODUCAO (valor/unidade)

═══════════════════════════════════════════════════════════════════════════════
MÓDULO PONTO E FOLHA
═══════════════════════════════════════════════════════════════════════════════

 FUNCIONARIO (1) ────< PONTO_ELETRONICO (N)    [UK: idpessoa+dataregistro]
 FUNCIONARIO (1) ────< FALTA_FUNCIONARIO (N)   [UK: idpessoa+datafalta]
 FUNCIONARIO (1) ────< VALE_FUNCIONARIO (N)    [statusvale: PENDENTE/DESCONTADO]
 FUNCIONARIO (1) ────< FECHAMENTO_FOLHA (N)    [UK: idpessoa+periodo]
 FUNCIONARIO (1) ────< FOLHA_PAGAMENTO (N)
 FUNCIONARIOPRODUCAO (1) ────< LANCAMENTO_PRODUCAO (N) [UK: idfunc+periodo]

═══════════════════════════════════════════════════════════════════════════════
MÓDULO PRODUÇÃO AGRÍCOLA
═══════════════════════════════════════════════════════════════════════════════

 ╔═══════════╗           ╔═══════════╗           ╔════════════════╗
 ║  PRODUTO  ║   herda   ║ ALIMENTO  ║    N:N    ║ CLASSIFICACAO  ║
 ║───────────║ ─────────>║───────────║<─────────>║────────────────║
 ║ idproduto ║           ║ variedade ║           ║ idclassificacao║
 ╚═══════════╝           ╚═════╤═════╝           ╚════════════════╝
                               │                 (via alimentoclassificacao)
                      referenciado por
                               │
 ╔═══════════════╗   N:1  ╔═══╧═════╗   1:N   ╔════════════════╗
 ║ AREAPRODUCAO  ║ ──────>║  TALAO  ║─────────║ TALAOFINANCEIRO║
 ║───────────────║        ║─────────║         ║────────────────║
 ║ propriedade   ║        ║ descricao│         ║ safra, período ║
 ║ plantas total ║        ║ plantas  ║         ║ custo/despesa  ║
 ╚═══════════════╝        ╚══════╤══╝         ║ venda bruta/liq║
                                 │            ╚════════════════╝
═══════════════════════════════════════════════════════════════════════════════
MÓDULO FINANCEIRO
═══════════════════════════════════════════════════════════════════════════════

 DESPESASCUSTO (1) ────< LANCAMENTOTALAO (N) — lançamentos de despesas por talão
'@

$sel.Font.Name = "Courier New"
$sel.Font.Size = 8
$sel.TypeText($er)
$sel.TypeParagraph()
$sel.Font.Name = "Calibri"
$sel.Font.Size = 11
$sel.TypeParagraph()

# ============== RODAPÉ ==============
$sel.InsertBreak(7)
$sel.Font.Size  = 14
$sel.Font.Bold  = $true
$sel.Font.Color = 0x1E6B3D
$sel.ParagraphFormat.Alignment = 1
$sel.TypeText("AGRO TECH ONE — Sistema de Gestão Agrícola")
$sel.TypeParagraph()
$sel.Font.Bold  = $false
$sel.Font.Size  = 10
$sel.Font.Color = 0x616161
$sel.TypeText("Documentação gerada por Engenharia Reversa em " + (Get-Date -Format "dd/MM/yyyy 'às' HH:mm"))
$sel.TypeParagraph()
$sel.TypeText("Banco: PostgreSQL 17 | 25 tabelas | Schema: public")
$sel.TypeParagraph()

# ============== SALVAR ==============
$doc.SaveAs([ref]$outputPath, [ref]16)  # 16 = wdFormatDocx
$doc.Close()
$word.Quit()

[System.Runtime.Interopservices.Marshal]::ReleaseComObject($doc) | Out-Null
[System.Runtime.Interopservices.Marshal]::ReleaseComObject($word) | Out-Null

Write-Host ""
Write-Host "✓ Documento gerado com sucesso: $outputPath"
