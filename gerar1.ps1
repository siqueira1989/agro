$outputPath = "C:\projeto\agro\Documentacao_Banco_Agro.docx"
$word = New-Object -ComObject Word.Application
$word.Visible = $false
$doc = $word.Documents.Add()
$sel = $word.Selection
function Par($t){$sel.Font.Name="Calibri";$sel.Font.Size=11;$sel.Font.Bold=$false;$sel.Font.Color=0x000000;$sel.TypeText($t);$sel.TypeParagraph()}
function H($t,$sz,$cor){$sel.Font.Name="Calibri";$sel.Font.Size=$sz;$sel.Font.Bold=$true;$sel.Font.Color=$cor;$sel.ParagraphFormat.Alignment=0;$sel.TypeText($t);$sel.TypeParagraph();$sel.Font.Bold=$false;$sel.Font.Size=11;$sel.Font.Color=0x000000}
function Pg(){$sel.InsertBreak(7)}
function MkTbl($headers,$rows,$cor){$nc=$headers.Count;$tw=$doc.Tables.Add($sel.Range,1,$nc);try{$tw.Style="Table Grid"}catch{};for($c=1;$c-le$nc;$c++){$tw.Cell(1,$c).Range.Text=$headers[$c-1];$tw.Cell(1,$c).Range.Font.Bold=$true;$tw.Cell(1,$c).Range.Font.Size=8;$tw.Cell(1,$c).Range.Font.Color=0xFFFFFF;$tw.Cell(1,$c).Shading.BackgroundPatternColor=$cor};$ri=1;foreach($row in $rows){$ri++;$rw=$tw.Rows.Add();for($c=1;$c-le$nc;$c++){$v=if($c-le$row.Count){$row[$c-1]}else{""};$tw.Cell($ri,$c).Range.Text=$v;$tw.Cell($ri,$c).Range.Font.Size=8};if($ri%2-eq0){for($c=1;$c-le$nc;$c++){$tw.Cell($ri,$c).Shading.BackgroundPatternColor=0xF1F8E9}}};$sel.MoveEnd();$sel.Collapse(0);$sel.TypeParagraph()}
# CAPA
$sel.ParagraphFormat.Alignment=1
$sel.Font.Name="Calibri";$sel.Font.Size=36;$sel.Font.Bold=$true;$sel.Font.Color=0x1E6B3D;$sel.TypeText("AGRO TECH ONE");$sel.TypeParagraph()
$sel.Font.Size=18;$sel.Font.Bold=$false;$sel.Font.Color=0x424242;$sel.TypeText("Sistema de Gestao Agricola");$sel.TypeParagraph();$sel.TypeParagraph()
$sel.Font.Size=22;$sel.Font.Bold=$true;$sel.Font.Color=0x1E6B3D;$sel.TypeText("DOCUMENTACAO DO BANCO DE DADOS");$sel.TypeParagraph()
$sel.Font.Size=14;$sel.Font.Bold=$false;$sel.Font.Color=0x616161;$sel.TypeText("Engenharia Reversa - Modelagem e Diagrama ER");$sel.TypeParagraph();$sel.TypeParagraph()
$sel.Font.Size=11;$sel.Font.Color=0x424242;$sel.TypeText("PostgreSQL 17  |  Schema: public  |  25 tabelas");$sel.TypeParagraph()
$sel.TypeText("Gerado em: "+(Get-Date -Format "dd/MM/yyyy HH:mm"));$sel.TypeParagraph()
$sel.ParagraphFormat.Alignment=0
Pg
# SEC 1
H "1. VISAO GERAL DO BANCO DE DADOS" 16 0x1E6B3D
@("Banco    : agro","SGBD     : PostgreSQL 17","Host     : localhost:5432","Schema   : public","Tabelas  : 25") | ForEach-Object {Par $_}
$sel.TypeParagraph()
Par "O sistema Agro Tech One cobre os seguintes dominios funcionais:"
@("* Cadastro de Pessoas - clientes, parceiros, funcionarios e usuarios","* Recursos Humanos - CLT, Diarista, Empreita, Producao","* Ponto Eletronico - entrada/saida com calculo de horas extras","* Folha de Pagamento - fechamento mensal com vales, faltas e extras","* Producao Agricola - areas de producao, talaos e safras","* Estoque de Alimentos - catalogo com classificacoes","* Financeiro - despesas, custos e receitas por safra") | ForEach-Object {Par $_}
Pg
# SEC 2
H "2. ARQUITETURA E HERANCA DE TABELAS (TABLE-PER-TYPE)" 16 0x1E6B3D
Par "O banco utiliza heranca de tabelas. Todas as entidades filhas compartilham a sequencia pessoa_idpessoa_seq como identificador unico."
$sel.TypeParagraph()
$sel.Font.Name="Courier New";$sel.Font.Size=9
@("pessoa  (idpessoa, nome, email, telefone, endereco)","  +-- pessoafisica       (+ CPF, data nascimento)","  |     +-- funcionario  (+ matricula, tipo ENUM, cargo, datas)","  |           +-- funcionarioclt       (+ salario, hora extra, statusemprego)","  |           +-- funcionariodiarista  (+ valorpordia)","  |           +-- funcionarioempreita  (+ valorfixoacordado)","  |           +-- funcionarioproducao  (+ valorporunidade)","  +-- pessoacnpj          (+ CNPJ, razaosocial, inscricaoestadual)","        +-- parceiro      (+ siteparceiro)","","produto (idproduto, nome, quantidade, valores, tipo)","  +-- alimento  (+ variedadealimento)") | ForEach-Object {$sel.TypeText($_);$sel.TypeParagraph()}
$sel.Font.Name="Calibri";$sel.Font.Size=11;$sel.TypeParagraph()
Pg
