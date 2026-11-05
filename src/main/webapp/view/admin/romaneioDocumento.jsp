<!DOCTYPE html>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java"
         import="java.util.*, Model.Dao.RomaneioDAO, Util.HtmlUtil"%>
<%--
  Documento do romaneio para visualizar/imprimir (A4), no layout do modelo do usuário
  (docs/modelos/Romaneio_Agricola_Moderno.docx). As mesmas medidas do PDF (Service.RomaneioDocumentoPdf):
  grade de 12 colunas, cores #1F5130/#6E9A4E/#BFC8BA, mínimo de 7 linhas de mercadorias. Sem preços.
  Todo texto do banco passa por HtmlUtil.esc.
--%>
<%
    Map<String, Object> d = null;
    String erro = null;
    try {
        String p = request.getParameter("id");
        int id = p == null ? 0 : Integer.parseInt(p.trim());
        d = id > 0 ? new RomaneioDAO().documento(id) : null;
        if (d == null) erro = "Romaneio não encontrado.";
    } catch (NumberFormatException e) {
        erro = "Romaneio inválido.";
    } catch (Exception e) {
        erro = Util.LogUtil.mensagemUsuario("abrir o documento do romaneio",
                Util.LogUtil.erro(RomaneioDAO.class, "Falha em romaneioDocumento.jsp", e));
    }
    final Map<String, Object> doc = d;
    java.util.function.Function<String, String> v = k -> doc == null || doc.get(k) == null ? "" : HtmlUtil.esc(String.valueOf(doc.get(k)));
    String ctx = request.getContextPath();
%>
<html lang="pt-br">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Romaneio <%= v.apply("numero") %></title>
  <style>
    :root { --verde:#1F5130; --oliva:#6E9A4E; --verde-claro:#B9D89C; --borda:#BFC8BA; --texto:#2B2B2B;
            --cinza:#6B7466; --cinza-claro:#9AA394; --fundo-cab:#EEF4E8; --zebra:#F8FAF6; }
    * { box-sizing: border-box; }
    body { margin: 0; background: #e9ece6; font-family: Calibri, Carlito, "Segoe UI", Arial, sans-serif; color: var(--texto);
           -webkit-print-color-adjust: exact; print-color-adjust: exact; }
    .barra { position: sticky; top: 0; z-index: 2; display: flex; gap: 8px; justify-content: center; flex-wrap: wrap;
             padding: 10px 16px; background: var(--verde); }
    .barra a, .barra button { font: 600 14px/1 "Segoe UI", Arial, sans-serif; border: 0; border-radius: 6px; padding: 9px 14px;
             cursor: pointer; text-decoration: none; color: var(--verde); background: #fff; }
    .barra .principal { background: var(--verde-claro); }
    .folha { width: 210mm; min-height: 297mm; margin: 16px auto; background: #fff; padding: 8.5mm 12.9mm 14mm;
             box-shadow: 0 2px 10px rgba(0,0,0,.15); position: relative; }
    .cab { display: grid; grid-template-columns: 357fr 165fr; margin-bottom: 4.2mm; }
    .cab .titulo { border-left: 4.5pt solid var(--oliva); padding: 1mm 0 1mm 3.5mm; display: flex; flex-direction: column; justify-content: center; }
    .cab .sup { color: var(--oliva); font-weight: 700; font-size: 10.5pt; letter-spacing: 2.2pt; }
    .cab h1 { margin: 0; color: var(--verde); font-size: 22pt; line-height: 1.12; font-weight: 700; }
    .cab .sub { color: var(--cinza); font-style: italic; font-size: 9pt; }
    .cab .numero { background: var(--verde); color: #fff; text-align: center; padding: 2.8mm; display: flex; flex-direction: column; justify-content: center; }
    .cab .numero .rot { color: var(--verde-claro); font-weight: 700; font-size: 7pt; letter-spacing: 1.6pt; }
    .cab .numero .num { font-weight: 700; font-size: 18pt; letter-spacing: 1.2pt; line-height: 1.25; }
    .cab .numero .safra { font-size: 8.5pt; font-weight: 700; }
    .cab .numero .safra span { color: var(--verde-claro); font-size: 7pt; margin-right: 4px; }
    table { width: 100%; border-collapse: collapse; table-layout: fixed; margin-bottom: 4.2mm; }
    td, th { border: 0.5pt solid var(--borda); vertical-align: top; }
    .faixa { background: var(--verde); border-color: var(--verde); color: #fff; font-weight: 700; font-size: 9pt; padding: 1mm 2.1mm 1.7mm; text-align: left; }
    .faixa b { color: var(--verde-claro); margin-right: 3mm; }
    .campo { height: 9.5mm; padding: 0.3mm 1.4mm 1.4mm; }
    .campo .r { display: block; color: var(--oliva); font-weight: 700; font-size: 6.5pt; line-height: 1.4; }
    .campo .v { display: block; font-size: 8.5pt; line-height: 1.25; word-wrap: break-word; }
    .merc th.c { background: var(--fundo-cab); color: var(--verde); font-size: 7pt; font-weight: 700; height: 6mm; vertical-align: middle; }
    .merc td { font-size: 8pt; height: 5.5mm; padding: 0.7mm 1.4mm 1.4mm; vertical-align: middle; }
    .merc tbody tr:nth-child(even) td { background: var(--zebra); }
    .merc td.it { color: var(--cinza-claro); }
    .merc .tot td { background: var(--fundo-cab); color: var(--verde); font-weight: 700; height: 6mm; }
    .d { text-align: right; } .m { text-align: center; }
    .rodape { position: absolute; left: 12.9mm; right: 12.9mm; bottom: 6mm; border-top: 0.5pt solid var(--borda);
              text-align: center; color: var(--cinza); font-size: 7pt; padding-top: 1.4mm; }
    .erro { max-width: 640px; margin: 40px auto; padding: 16px; background: #fff; border-left: 4px solid #b02a37; font: 15px "Segoe UI", Arial, sans-serif; }
    @page { size: A4; margin: 0; }
    @media print {
      body { background: #fff; }
      .barra { display: none; }
      .folha { margin: 0; box-shadow: none; width: 210mm; min-height: 297mm; }
      .merc thead { display: table-header-group; }
      .merc tr { page-break-inside: avoid; }
    }
    @media screen and (max-width: 820px) {
      .folha { width: auto; min-height: 0; margin: 8px; padding: 12px 16px 40px; }
      .cab { grid-template-columns: 1fr; gap: 8px; }
      .merc { display: block; overflow-x: auto; }
    }
  </style>
</head>
<body>
<% if (erro != null) { %>
  <div class="erro"><%= HtmlUtil.esc(erro) %> <a href="<%= ctx %>/view/admin/romaneio.jsp">Voltar aos romaneios</a></div>
<% } else {
     @SuppressWarnings("unchecked") List<Map<String, String>> itens = (List<Map<String, String>>) doc.get("itens");
     int linhas = Math.max(7, itens.size());
     String ini = v.apply("safraInicio"), fim = v.apply("safraFim");
%>
  <div class="barra">
    <button type="button" class="principal" onclick="window.print()">&#128424; Imprimir</button>
    <a href="<%= ctx %>/ControllerRomaneio?pdf=<%= doc.get("idRomaneio") %>">&#11015; Baixar PDF</a>
    <a href="<%= ctx %>/view/admin/cadastroRomaneio.jsp?id=<%= doc.get("idRomaneio") %>">Abrir romaneio</a>
    <a href="<%= ctx %>/view/admin/romaneio.jsp">Voltar aos romaneios</a>
  </div>

  <div class="folha">
    <div class="cab">
      <div class="titulo">
        <span class="sup">ROMANEIO DE</span>
        <h1>PRODUTOS AGRÍCOLAS</h1>
        <span class="sub">Controle de carga, pesagem e expedição de produção agrícola</span>
      </div>
      <div class="numero">
        <span class="rot">ROMANEIO Nº</span>
        <span class="num"><%= v.apply("numero") %></span>
        <span class="safra"><span>SAFRA</span><%= ini.isEmpty() ? "_____" : ini %> / <%= fim.isEmpty() ? "_____" : fim %></span>
      </div>
    </div>

    <table>
      <colgroup><% for (int i = 0; i < 12; i++) { %><col><% } %></colgroup>
      <tr><th class="faixa" colspan="12"><b>01</b>DADOS DA PROPRIEDADE (ORIGEM)</th></tr>
      <tr><td class="campo" colspan="8"><span class="r">NOME DO PRODUTOR</span><span class="v"><%= v.apply("prodNome") %></span></td>
          <td class="campo" colspan="4"><span class="r">CPF / CNPJ</span><span class="v"><%= v.apply("prodDoc") %></span></td></tr>
      <tr><td class="campo" colspan="7"><span class="r">NOME DA PROPRIEDADE</span><span class="v"><%= v.apply("propriedade") %></span></td>
          <td class="campo" colspan="2"><span class="r">SIGLA</span><span class="v"><%= v.apply("sigla") %></span></td>
          <td class="campo" colspan="3"><span class="r">INSCRIÇÃO ESTADUAL</span><span class="v"><%= v.apply("prodIe") %></span></td></tr>
      <tr><td class="campo" colspan="8"><span class="r">ENDEREÇO</span><span class="v"><%= v.apply("prodEndereco") %></span></td>
          <td class="campo" colspan="4"><span class="r">MUNICÍPIO / UF</span><span class="v"><%= v.apply("prodMunicipioUf") %></span></td></tr>
      <tr><td class="campo" colspan="3"><span class="r">DATA DE EMISSÃO</span><span class="v"><%= v.apply("dataEmissao") %></span></td>
          <td class="campo" colspan="3"><span class="r">NOTA FISCAL</span><span class="v"><%= v.apply("notaFiscal") %></span></td>
          <td class="campo" colspan="2"><span class="r">SÉRIE</span><span class="v"><%= v.apply("serieNf") %></span></td>
          <td class="campo" colspan="4"><span class="r">TALHÃO / LOTE</span><span class="v"><%= v.apply("talhaoLote") %></span></td></tr>
    </table>

    <table>
      <colgroup><% for (int i = 0; i < 12; i++) { %><col><% } %></colgroup>
      <tr><th class="faixa" colspan="12"><b>02</b>DADOS DO DESTINO</th></tr>
      <tr><td class="campo" colspan="8"><span class="r">EMPRESA</span><span class="v"><%= v.apply("destEmpresa") %></span></td>
          <td class="campo" colspan="4"><span class="r">CNPJ</span><span class="v"><%= v.apply("destCnpj") %></span></td></tr>
      <tr><td class="campo" colspan="7"><span class="r">ENDEREÇO</span><span class="v"><%= v.apply("destEndereco") %></span></td>
          <td class="campo" colspan="2"><span class="r">CEP</span><span class="v"><%= v.apply("destCep") %></span></td>
          <td class="campo" colspan="3"><span class="r">INSCRIÇÃO ESTADUAL (IE)</span><span class="v"><%= v.apply("destIe") %></span></td></tr>
      <tr><td class="campo" colspan="6"><span class="r">CIDADE</span><span class="v"><%= v.apply("destCidade") %></span></td>
          <td class="campo" colspan="2"><span class="r">ESTADO</span><span class="v"><%= v.apply("destUf") %></span></td>
          <td class="campo" colspan="4"><span class="r">TELEFONE / CONTATO</span><span class="v"><%= v.apply("destTelefone") %></span></td></tr>
    </table>

    <table>
      <colgroup><% for (int i = 0; i < 12; i++) { %><col><% } %></colgroup>
      <tr><th class="faixa" colspan="12"><b>03</b>TRANSPORTE</th></tr>
      <tr><td class="campo" colspan="8"><span class="r">TRANSPORTADORA</span><span class="v"><%= v.apply("transportadora") %></span></td>
          <td class="campo" colspan="4"><span class="r">CNPJ / CPF</span><span class="v"><%= v.apply("transportadoraDoc") %></span></td></tr>
      <tr><td class="campo" colspan="6"><span class="r">MOTORISTA</span><span class="v"><%= v.apply("motorista") %></span></td>
          <td class="campo" colspan="3"><span class="r">CPF DO MOTORISTA</span><span class="v"><%= v.apply("motoristaCpf") %></span></td>
          <td class="campo" colspan="3"><span class="r">CNH</span><span class="v"><%= v.apply("motoristaCnh") %></span></td></tr>
      <tr><td class="campo" colspan="3"><span class="r">PLACA DO VEÍCULO</span><span class="v"><%= v.apply("placa") %></span></td>
          <td class="campo" colspan="3"><span class="r">PLACA DA CARRETA</span><span class="v"><%= v.apply("placaCarreta") %></span></td>
          <td class="campo" colspan="2"><span class="r">UF</span><span class="v"><%= v.apply("ufVeiculo") %></span></td>
          <td class="campo" colspan="4"><span class="r">TIPO DE VEÍCULO</span><span class="v"><%= v.apply("tipoVeiculo") %></span></td></tr>
    </table>

    <table class="merc">
      <colgroup><col style="width:5%"><col style="width:25.1%"><col style="width:12.45%"><col style="width:12.45%">
                <col style="width:10.55%"><col style="width:6.7%"><col style="width:11.5%"><col style="width:16.25%"></colgroup>
      <thead>
        <tr><th class="faixa" colspan="8"><b>04</b>DISCRIMINAÇÃO DAS MERCADORIAS / PRODUTOS</th></tr>
        <tr><th class="c">ITEM</th><th class="c">DESCRIÇÃO</th><th class="c">EMBALAGEM</th><th class="c">CLASSIFICAÇÃO</th>
            <th class="c">QUANTIDADE</th><th class="c">UNID.</th><th class="c">PESO (KG)</th><th class="c">OBSERVAÇÃO</th></tr>
      </thead>
      <tbody>
      <% for (int i = 0; i < linhas; i++) {
           Map<String, String> it = i < itens.size() ? itens.get(i) : Collections.emptyMap(); %>
        <tr><td class="it"><%= String.format("%02d", i + 1) %></td>
            <td><%= HtmlUtil.esc(it.get("descricao")) %></td>
            <td><%= HtmlUtil.esc(it.get("embalagem")) %></td>
            <td class="m"><%= HtmlUtil.esc(it.get("classificacao")) %></td>
            <td class="d"><%= HtmlUtil.esc(it.get("quantidade")) %></td>
            <td class="m"><%= HtmlUtil.esc(it.get("unid")) %></td>
            <td class="d"><%= HtmlUtil.esc(it.get("peso")) %></td>
            <td><%= HtmlUtil.esc(it.get("observacao")) %></td></tr>
      <% } %>
        <tr class="tot"><td colspan="4" class="d">TOTAIS</td><td class="d"><%= v.apply("totalQuantidade") %></td>
            <td class="m"><%= itens.isEmpty() ? "" : "CX" %></td><td class="d"><%= v.apply("totalPeso") %></td><td></td></tr>
      </tbody>
    </table>

    <div class="rodape">1ª via – Produtor &nbsp;•&nbsp; 2ª via – Transportador &nbsp;•&nbsp; 3ª via – Destino</div>
  </div>
<% } %>
</body>
</html>
