package Service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Documento do romaneio em PDF, no layout do modelo do usuário (docs/modelos/Romaneio_Agricola_Moderno.docx):
 * A4 retrato, cabeçalho com a caixa verde do nº/safra, blocos 01 Origem, 02 Destino, 03 Transporte e
 * 04 Mercadorias (mínimo de 7 linhas, cresce se houver mais) e o rodapé das três vias. Sem preços.
 *
 * Usa o OpenPDF que já vem com o JasperReports (sem dependência nova). Medidas tiradas do .docx: grade de
 * 12 colunas de 870 twips (= 43,5 pt), margens de 733/480 twips, cores #1F5130 / #6E9A4E / #BFC8BA.
 * A página de impressão (romaneioDocumento.jsp) segue as mesmas medidas em CSS.
 *
 * Entrada: o mapa de {@code RomaneioDAO.documento(id)} (todos os textos já formatados, nunca null).
 */
public final class RomaneioDocumentoPdf {

    private static final Color VERDE = new Color(0x1F, 0x51, 0x30);
    private static final Color OLIVA = new Color(0x6E, 0x9A, 0x4E);
    private static final Color VERDE_CLARO = new Color(0xB9, 0xD8, 0x9C);
    private static final Color BORDA = new Color(0xBF, 0xC8, 0xBA);
    private static final Color TEXTO = new Color(0x2B, 0x2B, 0x2B);
    private static final Color CINZA = new Color(0x6B, 0x74, 0x66);
    private static final Color CINZA_CLARO = new Color(0x9A, 0xA3, 0x94);
    private static final Color FUNDO_CAB = new Color(0xEE, 0xF4, 0xE8);
    private static final Color FUNDO_ZEBRA = new Color(0xF8, 0xFA, 0xF6);

    private static final float LARGURA = 522f;            // 10440 twips
    private static final int LINHAS_MINIMAS = 7;

    private RomaneioDocumentoPdf() { }

    private static final class Fontes {
        final BaseFont normal, negrito, italico;
        Fontes() throws Exception {
            BaseFont n = carregar("calibri.ttf"), b = carregar("calibrib.ttf"), i = carregar("calibrii.ttf");
            if (n == null || b == null || i == null) {     // fora do Windows: Helvetica (WinAnsi cobre os acentos)
                n = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
                b = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
                i = BaseFont.createFont(BaseFont.HELVETICA_OBLIQUE, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
            }
            normal = n; negrito = b; italico = i;
        }
        private static BaseFont carregar(String arquivo) {
            String win = System.getenv("WINDIR");
            File f = new File((win == null ? "C:\\Windows" : win) + "\\Fonts\\" + arquivo);
            if (!f.isFile()) return null;
            try { return BaseFont.createFont(f.getAbsolutePath(), BaseFont.IDENTITY_H, BaseFont.EMBEDDED); }
            catch (Exception e) { return null; }
        }
        Font f(BaseFont b, float tam, Color cor) { Font x = new Font(b, tam); x.setColor(cor); return x; }
    }

    @SuppressWarnings("unchecked")
    public static byte[] gerar(Map<String, Object> d) throws Exception {
        Fontes fx = new Fontes();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 36.65f, 36.65f, 24f, 40f);
        PdfWriter w = PdfWriter.getInstance(doc, out);
        w.setPageEvent(new Rodape(fx));
        doc.addTitle("Romaneio " + v(d, "numero"));
        doc.addCreator("Agro Tech One");
        doc.open();

        doc.add(cabecalho(d, fx));
        espaco(doc, 12);

        PdfPTable t = secao("01", "DADOS DA PROPRIEDADE (ORIGEM)", fx);
        campos(t, fx, new Object[][]{{"NOME DO PRODUTOR", v(d, "prodNome"), 8}, {"CPF / CNPJ", v(d, "prodDoc"), 4}});
        campos(t, fx, new Object[][]{{"NOME DA PROPRIEDADE", v(d, "propriedade"), 7}, {"SIGLA", v(d, "sigla"), 2}, {"INSCRIÇÃO ESTADUAL", v(d, "prodIe"), 3}});
        campos(t, fx, new Object[][]{{"ENDEREÇO", v(d, "prodEndereco"), 8}, {"MUNICÍPIO / UF", v(d, "prodMunicipioUf"), 4}});
        campos(t, fx, new Object[][]{{"DATA DE EMISSÃO", v(d, "dataEmissao"), 3}, {"NOTA FISCAL", v(d, "notaFiscal"), 3},
                {"SÉRIE", v(d, "serieNf"), 2}, {"TALHÃO / LOTE", v(d, "talhaoLote"), 4}});
        doc.add(t);
        espaco(doc, 12);

        t = secao("02", "DADOS DO DESTINO", fx);
        campos(t, fx, new Object[][]{{"EMPRESA", v(d, "destEmpresa"), 8}, {"CNPJ", v(d, "destCnpj"), 4}});
        campos(t, fx, new Object[][]{{"ENDEREÇO", v(d, "destEndereco"), 7}, {"CEP", v(d, "destCep"), 2}, {"INSCRIÇÃO ESTADUAL (IE)", v(d, "destIe"), 3}});
        campos(t, fx, new Object[][]{{"CIDADE", v(d, "destCidade"), 6}, {"ESTADO", v(d, "destUf"), 2}, {"TELEFONE / CONTATO", v(d, "destTelefone"), 4}});
        doc.add(t);
        espaco(doc, 12);

        t = secao("03", "TRANSPORTE", fx);
        campos(t, fx, new Object[][]{{"TRANSPORTADORA", v(d, "transportadora"), 8}, {"CNPJ / CPF", v(d, "transportadoraDoc"), 4}});
        campos(t, fx, new Object[][]{{"MOTORISTA", v(d, "motorista"), 6}, {"CPF DO MOTORISTA", v(d, "motoristaCpf"), 3}, {"CNH", v(d, "motoristaCnh"), 3}});
        campos(t, fx, new Object[][]{{"PLACA DO VEÍCULO", v(d, "placa"), 3}, {"PLACA DA CARRETA", v(d, "placaCarreta"), 3},
                {"UF", v(d, "ufVeiculo"), 2}, {"TIPO DE VEÍCULO", v(d, "tipoVeiculo"), 4}});
        doc.add(t);
        espaco(doc, 12);

        doc.add(mercadorias((List<Map<String, String>>) d.get("itens"), v(d, "totalQuantidade"), v(d, "totalPeso"), fx));
        doc.close();
        return out.toByteArray();
    }

    /* ---------- cabeçalho: título à esquerda (barra oliva) e caixa verde do nº / safra ---------- */
    private static PdfPTable cabecalho(Map<String, Object> d, Fontes fx) throws DocumentException {
        PdfPTable t = new PdfPTable(new float[]{357f, 165f});
        t.setTotalWidth(LARGURA); t.setLockedWidth(true);

        PdfPCell esq = new PdfPCell();
        esq.setBorder(Rectangle.LEFT); esq.setBorderWidthLeft(4.5f); esq.setBorderColorLeft(OLIVA);
        esq.setPaddingLeft(10f); esq.setPaddingTop(2f); esq.setPaddingBottom(2f);
        esq.setVerticalAlignment(Element.ALIGN_MIDDLE);
        esq.addElement(espacado("ROMANEIO DE", fx.f(fx.negrito, 10.5f, OLIVA), 2.2f, Element.ALIGN_LEFT, 0f));
        Paragraph p = new Paragraph("PRODUTOS AGRÍCOLAS", fx.f(fx.negrito, 22f, VERDE)); p.setLeading(25f);
        esq.addElement(p);
        p = new Paragraph("Controle de carga, pesagem e expedição de produção agrícola", fx.f(fx.italico, 9f, CINZA)); p.setLeading(12f);
        esq.addElement(p);
        t.addCell(esq);

        PdfPCell dir = new PdfPCell();
        dir.setBorder(Rectangle.NO_BORDER); dir.setBackgroundColor(VERDE);
        dir.setPadding(8f); dir.setVerticalAlignment(Element.ALIGN_MIDDLE);
        dir.addElement(espacado("ROMANEIO Nº", fx.f(fx.negrito, 7f, VERDE_CLARO), 1.6f, Element.ALIGN_CENTER, 0f));
        dir.addElement(espacado(v(d, "numero"), fx.f(fx.negrito, 18f, Color.WHITE), 1.2f, Element.ALIGN_CENTER, 22f));
        Paragraph s = new Paragraph();
        s.setAlignment(Element.ALIGN_CENTER); s.setLeading(11f);
        s.add(new Chunk("SAFRA  ", fx.f(fx.negrito, 7f, VERDE_CLARO)));
        String ini = v(d, "safraInicio"), fim = v(d, "safraFim");
        s.add(new Chunk((ini.isEmpty() ? "_____" : ini) + " / " + (fim.isEmpty() ? "_____" : fim), fx.f(fx.negrito, 8.5f, Color.WHITE)));
        dir.addElement(s);
        t.addCell(dir);
        return t;
    }

    private static Paragraph espacado(String txt, Font f, float espaco, int alinhamento, float leading) {
        Chunk c = new Chunk(txt, f); c.setCharacterSpacing(espaco);
        Paragraph p = new Paragraph(c); p.setAlignment(alinhamento);
        p.setLeading(leading > 0 ? leading : f.getSize() * 1.3f);
        return p;
    }

    /* ---------- blocos 01–03: grade de 12 colunas ---------- */
    private static PdfPTable secao(String num, String titulo, Fontes fx) {
        float[] col = new float[12];
        java.util.Arrays.fill(col, LARGURA / 12f);
        PdfPTable t = new PdfPTable(col);
        t.setTotalWidth(LARGURA); t.setLockedWidth(true);
        t.addCell(faixa(num, titulo, 12, fx));
        return t;
    }

    private static PdfPCell faixa(String num, String titulo, int colspan, Fontes fx) {
        Phrase ph = new Phrase();
        ph.add(new Chunk(num + "   ", fx.f(fx.negrito, 9f, VERDE_CLARO)));
        ph.add(new Chunk(titulo, fx.f(fx.negrito, 9f, Color.WHITE)));
        PdfPCell c = new PdfPCell(ph);
        c.setColspan(colspan);
        c.setBackgroundColor(VERDE); c.setBorderColor(VERDE); c.setBorderWidth(0.5f);
        c.setPaddingLeft(6f); c.setPaddingTop(3f); c.setPaddingBottom(5f);
        return c;
    }

    /** Uma linha de campos: {rótulo, valor, colunas}. Rótulo pequeno oliva em cima, valor embaixo. */
    private static void campos(PdfPTable t, Fontes fx, Object[][] linha) {
        for (Object[] campo : linha) {
            PdfPCell c = new PdfPCell();
            c.setColspan((Integer) campo[2]);
            c.setBorderColor(BORDA); c.setBorderWidth(0.5f);
            c.setPaddingLeft(4f); c.setPaddingRight(4f); c.setPaddingTop(1f); c.setPaddingBottom(4f);
            c.setMinimumHeight(27f);
            Paragraph rot = new Paragraph((String) campo[0], fx.f(fx.negrito, 6.5f, OLIVA)); rot.setLeading(9f);
            Paragraph val = new Paragraph((String) campo[1], fx.f(fx.normal, 8.5f, TEXTO)); val.setLeading(10.5f);
            c.addElement(rot); c.addElement(val);
            t.addCell(c);
        }
    }

    /* ---------- bloco 04: mercadorias ---------- */
    private static PdfPTable mercadorias(List<Map<String, String>> itens, String totQtd, String totPeso, Fontes fx) throws DocumentException {
        PdfPTable t = new PdfPTable(new float[]{26f, 131f, 65f, 65f, 55f, 35f, 60f, 85f});
        t.setTotalWidth(LARGURA); t.setLockedWidth(true);
        t.setHeaderRows(2);
        t.addCell(faixa("04", "DISCRIMINAÇÃO DAS MERCADORIAS / PRODUTOS", 8, fx));
        String[] cab = {"ITEM", "DESCRIÇÃO", "EMBALAGEM", "CLASSIFICAÇÃO", "QUANTIDADE", "UNID.", "PESO (KG)", "OBSERVAÇÃO"};
        for (String h : cab) {
            PdfPCell c = celula(h, fx.f(fx.negrito, 7f, VERDE), Element.ALIGN_CENTER, FUNDO_CAB);
            c.setMinimumHeight(17f);
            t.addCell(c);
        }
        int n = Math.max(LINHAS_MINIMAS, itens == null ? 0 : itens.size());
        Font fItem = fx.f(fx.normal, 8f, CINZA_CLARO), fVal = fx.f(fx.normal, 8f, TEXTO);
        for (int i = 0; i < n; i++) {
            Map<String, String> it = itens != null && i < itens.size() ? itens.get(i) : null;
            Color fundo = i % 2 == 1 ? FUNDO_ZEBRA : null;
            t.addCell(linhaItem(String.format("%02d", i + 1), fItem, Element.ALIGN_LEFT, fundo));
            t.addCell(linhaItem(g(it, "descricao"), fVal, Element.ALIGN_LEFT, fundo));
            t.addCell(linhaItem(g(it, "embalagem"), fVal, Element.ALIGN_LEFT, fundo));
            t.addCell(linhaItem(g(it, "classificacao"), fVal, Element.ALIGN_CENTER, fundo));
            t.addCell(linhaItem(g(it, "quantidade"), fVal, Element.ALIGN_RIGHT, fundo));
            t.addCell(linhaItem(g(it, "unid"), fVal, Element.ALIGN_CENTER, fundo));
            t.addCell(linhaItem(g(it, "peso"), fVal, Element.ALIGN_RIGHT, fundo));
            t.addCell(linhaItem(g(it, "observacao"), fVal, Element.ALIGN_LEFT, fundo));
        }
        Font fTot = fx.f(fx.negrito, 8f, VERDE);
        PdfPCell tot = celula("TOTAIS", fTot, Element.ALIGN_RIGHT, FUNDO_CAB);
        tot.setColspan(4); tot.setMinimumHeight(17f);
        t.addCell(tot);
        t.addCell(celula(totQtd, fTot, Element.ALIGN_RIGHT, FUNDO_CAB));
        t.addCell(celula(itens == null || itens.isEmpty() ? "" : "CX", fTot, Element.ALIGN_CENTER, FUNDO_CAB));
        t.addCell(celula(totPeso, fTot, Element.ALIGN_RIGHT, FUNDO_CAB));
        t.addCell(celula("", fTot, Element.ALIGN_LEFT, FUNDO_CAB));
        return t;
    }

    private static PdfPCell linhaItem(String txt, Font f, int alinhamento, Color fundo) {
        PdfPCell c = celula(txt, f, alinhamento, fundo);
        c.setMinimumHeight(15.5f);
        return c;
    }

    private static PdfPCell celula(String txt, Font f, int alinhamento, Color fundo) {
        PdfPCell c = new PdfPCell(new Phrase(txt == null ? "" : txt, f));
        c.setHorizontalAlignment(alinhamento); c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setBorderColor(BORDA); c.setBorderWidth(0.5f);
        c.setPaddingLeft(4f); c.setPaddingRight(4f); c.setPaddingTop(2f); c.setPaddingBottom(4f);
        if (fundo != null) c.setBackgroundColor(fundo);
        return c;
    }

    /** Rodapé de toda página: linha fina e "1ª via – Produtor • 2ª via – Transportador • 3ª via – Destino". */
    private static final class Rodape extends PdfPageEventHelper {
        private final Fontes fx;
        Rodape(Fontes fx) { this.fx = fx; }
        @Override public void onEndPage(PdfWriter w, Document d) {
            PdfContentByte cb = w.getDirectContent();
            float y = d.bottomMargin() - 18f;
            cb.saveState();
            cb.setColorStroke(BORDA); cb.setLineWidth(0.5f);
            cb.moveTo(d.left(), y + 10f); cb.lineTo(d.right(), y + 10f); cb.stroke();
            cb.restoreState();
            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                    new Phrase("1ª via – Produtor   •   2ª via – Transportador   •   3ª via – Destino", fx.f(fx.normal, 7f, CINZA)),
                    (d.left() + d.right()) / 2f, y, 0);
        }
    }

    private static void espaco(Document doc, float pt) throws DocumentException {
        Paragraph p = new Paragraph(" "); p.setLeading(pt); doc.add(p);
    }

    private static String v(Map<String, Object> d, String k) { Object o = d.get(k); return o == null ? "" : String.valueOf(o); }
    private static String g(Map<String, String> m, String k) { if (m == null) return ""; String s = m.get(k); return s == null ? "" : s; }
}
