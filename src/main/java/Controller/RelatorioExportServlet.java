package Controller;

import Model.Dao.AreaProducaoDAO;
import Model.Dao.FolhaPagamentoDAO;
import Model.Dao.FuncionarioDAO;
import Model.Model.AreaProducao;
import Model.Model.FolhaPagamento;
import Model.Model.Funcionario;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.design.*;
import net.sf.jasperreports.engine.type.HorizontalTextAlignEnum;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RelatorioExportServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private FolhaPagamentoDAO folhaDAO;
    private FuncionarioDAO funcionarioDAO;
    private AreaProducaoDAO areaDAO;

    @Override
    public void init() {
        folhaDAO = new FolhaPagamentoDAO();
        funcionarioDAO = new FuncionarioDAO();
        areaDAO = new AreaProducaoDAO();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String tipo = req.getParameter("tipo");
        String modulo = req.getParameter("modulo");
        String periodo = req.getParameter("periodo");

        if (tipo == null || modulo == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Parâmetros 'tipo' e 'modulo' são obrigatórios.");
            return;
        }

        try {
            switch (modulo.toLowerCase()) {
                case "folhapagamento":
                    if (periodo == null || periodo.isBlank()) {
                        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Parâmetro 'periodo' é obrigatório para folha.");
                        return;
                    }
                    List<FolhaPagamento> folha = folhaDAO.listByPeriodo(periodo);
                    if ("excel".equalsIgnoreCase(tipo)) exportarFolhaExcel(resp, folha, periodo);
                    else exportarFolhaPdf(resp, folha, periodo);
                    break;

                case "funcionario":
                    List<Funcionario> funcionarios = funcionarioDAO.listAllFuncionario();
                    if ("excel".equalsIgnoreCase(tipo)) exportarFuncionariosExcel(resp, funcionarios);
                    else exportarFuncionariosPdf(resp, funcionarios);
                    break;

                case "areaproducao":
                    List<AreaProducao> areas = areaDAO.listAll();
                    if ("excel".equalsIgnoreCase(tipo)) exportarAreasExcel(resp, areas);
                    else exportarAreasPdf(resp, areas);
                    break;

                default:
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Módulo desconhecido: " + modulo);
            }
        } catch (SQLException e) {
            // P1-15/P2-20: pilha completa no log; ao usuário vai só o código.
            String cod = Util.LogUtil.erro(RelatorioExportServlet.class, "Falha tratada em RelatorioExportServlet.", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Erro ao gerar relatório: " + cod);
        }
    }

    // ===================== FOLHA DE PAGAMENTO =====================

    private void exportarFolhaExcel(HttpServletResponse resp, List<FolhaPagamento> lista, String periodo) throws IOException {
        String filename = "folha_" + periodo + ".xlsx";
        resp.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        resp.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        try (XSSFWorkbook wb = new XSSFWorkbook(); OutputStream os = resp.getOutputStream()) {
            Sheet sheet = wb.createSheet("Folha " + periodo);

            CellStyle headerStyle = criarEstiloCabecalho(wb);
            CellStyle moedaStyle = criarEstiloMoeda(wb);

            // Título
            Row titulo = sheet.createRow(0);
            Cell cellTitulo = titulo.createCell(0);
            cellTitulo.setCellValue("FOLHA DE PAGAMENTO — " + periodo);
            cellTitulo.setCellStyle(headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));

            // Cabeçalhos
            Row cabecalho = sheet.createRow(1);
            String[] cols = {"Funcionário", "Tipo", "Período", "Valor (R$)"};
            for (int i = 0; i < cols.length; i++) {
                Cell c = cabecalho.createCell(i);
                c.setCellValue(cols[i]);
                c.setCellStyle(headerStyle);
            }

            // Dados
            int rowNum = 2;
            BigDecimal total = BigDecimal.ZERO;
            for (FolhaPagamento fp : lista) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(fp.getNomeFuncionario());
                row.createCell(1).setCellValue(fp.getTipoFuncionario().name());
                row.createCell(2).setCellValue(fp.getPeriodo());
                Cell valorCell = row.createCell(3);
                valorCell.setCellValue(fp.getValorCalculado().doubleValue());
                valorCell.setCellStyle(moedaStyle);
                total = total.add(fp.getValorCalculado());
            }

            // Total
            Row totalRow = sheet.createRow(rowNum);
            totalRow.createCell(2).setCellValue("TOTAL:");
            Cell totalCell = totalRow.createCell(3);
            totalCell.setCellValue(total.doubleValue());
            totalCell.setCellStyle(moedaStyle);

            // Gerado em
            sheet.createRow(rowNum + 2).createCell(0)
                    .setCellValue("Gerado em: " + LocalDateTime.now().format(FORMATTER));

            for (int i = 0; i < 4; i++) sheet.autoSizeColumn(i);
            wb.write(os);
        }
    }

    private void exportarFolhaPdf(HttpServletResponse resp, List<FolhaPagamento> lista, String periodo) throws IOException {
        resp.setContentType("application/pdf");
        resp.setHeader("Content-Disposition", "attachment; filename=\"folha_" + periodo + ".pdf\"");

        try (OutputStream os = resp.getOutputStream()) {
            JasperPrint print = gerarJasperPrint(
                    "FOLHA DE PAGAMENTO — " + periodo,
                    new String[]{"nomeFuncionario", "tipoFuncionario", "periodo", "valorCalculado"},
                    new String[]{"Funcionário", "Tipo", "Período", "Valor (R$)"},
                    new int[]{200, 80, 80, 100},
                    lista, FolhaPagamento.class
            );
            JasperExportManager.exportReportToPdfStream(print, os);
        } catch (JRException e) {
            // P1-15/P2-20: pilha completa no log; ao usuário vai só o código.
            String cod = Util.LogUtil.erro(RelatorioExportServlet.class, "Falha tratada em RelatorioExportServlet.", e);
            throw new IOException("Erro ao gerar PDF: " + cod, e);
        }
    }

    // ===================== FUNCIONÁRIOS =====================

    private void exportarFuncionariosExcel(HttpServletResponse resp, List<Funcionario> lista) throws IOException {
        resp.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        resp.setHeader("Content-Disposition", "attachment; filename=\"funcionarios.xlsx\"");

        try (XSSFWorkbook wb = new XSSFWorkbook(); OutputStream os = resp.getOutputStream()) {
            Sheet sheet = wb.createSheet("Funcionários");
            CellStyle headerStyle = criarEstiloCabecalho(wb);

            Row titulo = sheet.createRow(0);
            Cell cellTitulo = titulo.createCell(0);
            cellTitulo.setCellValue("RELATÓRIO DE FUNCIONÁRIOS");
            cellTitulo.setCellStyle(headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 4));

            Row cab = sheet.createRow(1);
            String[] cols = {"Nome", "CPF", "Matrícula", "Cargo", "Tipo"};
            for (int i = 0; i < cols.length; i++) {
                Cell c = cab.createCell(i);
                c.setCellValue(cols[i]);
                c.setCellStyle(headerStyle);
            }

            int rowNum = 2;
            for (Funcionario f : lista) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(f.getNomePessoa());
                row.createCell(1).setCellValue(f.getCpfPf() != null ? f.getCpfPf() : "");
                row.createCell(2).setCellValue(f.getMatricula() != null ? f.getMatricula() : "");
                row.createCell(3).setCellValue(f.getCargo() != null ? f.getCargo() : "");
                row.createCell(4).setCellValue(f.getTipoFuncionario() != null ? f.getTipoFuncionario().name() : "");
            }

            sheet.createRow(rowNum + 1).createCell(0)
                    .setCellValue("Gerado em: " + LocalDateTime.now().format(FORMATTER));

            for (int i = 0; i < 5; i++) sheet.autoSizeColumn(i);
            wb.write(os);
        }
    }

    private void exportarFuncionariosPdf(HttpServletResponse resp, List<Funcionario> lista) throws IOException {
        resp.setContentType("application/pdf");
        resp.setHeader("Content-Disposition", "attachment; filename=\"funcionarios.pdf\"");

        try (OutputStream os = resp.getOutputStream()) {
            JasperPrint print = gerarJasperPrint(
                    "RELATÓRIO DE FUNCIONÁRIOS",
                    new String[]{"nomePessoa", "cpfPf", "matricula", "cargo", "tipoFuncionario"},
                    new String[]{"Nome", "CPF", "Matrícula", "Cargo", "Tipo"},
                    new int[]{180, 100, 80, 100, 80},
                    lista, Funcionario.class
            );
            JasperExportManager.exportReportToPdfStream(print, os);
        } catch (JRException e) {
            // P1-15/P2-20: pilha completa no log; ao usuário vai só o código.
            String cod = Util.LogUtil.erro(RelatorioExportServlet.class, "Falha tratada em RelatorioExportServlet.", e);
            throw new IOException("Erro ao gerar PDF: " + cod, e);
        }
    }

    // ===================== ÁREAS DE PRODUÇÃO =====================

    private void exportarAreasExcel(HttpServletResponse resp, List<AreaProducao> lista) throws IOException {
        resp.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        resp.setHeader("Content-Disposition", "attachment; filename=\"areas_producao.xlsx\"");

        try (XSSFWorkbook wb = new XSSFWorkbook(); OutputStream os = resp.getOutputStream()) {
            Sheet sheet = wb.createSheet("Áreas de Produção");
            CellStyle headerStyle = criarEstiloCabecalho(wb);

            Row titulo = sheet.createRow(0);
            Cell cellTitulo = titulo.createCell(0);
            cellTitulo.setCellValue("ÁREAS DE PRODUÇÃO");
            cellTitulo.setCellStyle(headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 4));

            Row cab = sheet.createRow(1);
            String[] cols = {"Propriedade", "Proprietário", "Sigla", "Qtd. Plantas", "CEP"};
            for (int i = 0; i < cols.length; i++) {
                Cell c = cab.createCell(i);
                c.setCellValue(cols[i]);
                c.setCellStyle(headerStyle);
            }

            int rowNum = 2;
            for (AreaProducao a : lista) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(a.getPropriedadeAreaProducao());
                row.createCell(1).setCellValue(a.getProprietarioAreaProducao());
                row.createCell(2).setCellValue(a.getSiglasAreaProducao());
                row.createCell(3).setCellValue(a.getQuantidadeTotalPlantasAreaProducao());
                row.createCell(4).setCellValue(a.getCep() != null ? a.getCep() : "");
            }

            sheet.createRow(rowNum + 1).createCell(0)
                    .setCellValue("Gerado em: " + LocalDateTime.now().format(FORMATTER));

            for (int i = 0; i < 5; i++) sheet.autoSizeColumn(i);
            wb.write(os);
        }
    }

    private void exportarAreasPdf(HttpServletResponse resp, List<AreaProducao> lista) throws IOException {
        resp.setContentType("application/pdf");
        resp.setHeader("Content-Disposition", "attachment; filename=\"areas_producao.pdf\"");

        try (OutputStream os = resp.getOutputStream()) {
            JasperPrint print = gerarJasperPrint(
                    "ÁREAS DE PRODUÇÃO",
                    new String[]{"propriedadeAreaProducao", "proprietarioAreaProducao",
                            "siglasAreaProducao", "quantidadeTotalPlantasAreaProducao", "cep"},
                    new String[]{"Propriedade", "Proprietário", "Sigla", "Qtd. Plantas", "CEP"},
                    new int[]{180, 140, 60, 80, 100},
                    lista, AreaProducao.class
            );
            JasperExportManager.exportReportToPdfStream(print, os);
        } catch (JRException e) {
            // P1-15/P2-20: pilha completa no log; ao usuário vai só o código.
            String cod = Util.LogUtil.erro(RelatorioExportServlet.class, "Falha tratada em RelatorioExportServlet.", e);
            throw new IOException("Erro ao gerar PDF: " + cod, e);
        }
    }

    // ===================== JASPER HELPER =====================

    private JasperPrint gerarJasperPrint(String titulo, String[] campos, String[] cabecalhos,
                                          int[] larguras, List<?> dados, Class<?> beanClass) throws JRException {
        JasperDesign design = new JasperDesign();
        design.setName("relatorio");
        design.setPageWidth(595);
        design.setPageHeight(842);
        design.setLeftMargin(20);
        design.setRightMargin(20);
        design.setTopMargin(20);
        design.setBottomMargin(20);

        // Parâmetros
        JRDesignParameter paramTitulo = new JRDesignParameter();
        paramTitulo.setName("TITULO");
        paramTitulo.setValueClass(String.class);
        design.addParameter(paramTitulo);

        // Fields
        for (String campo : campos) {
            JRDesignField field = new JRDesignField();
            field.setName(campo);
            field.setValueClass(Object.class);
            design.addField(field);
        }

        // Sections
        JRDesignBand titleBand = new JRDesignBand();
        titleBand.setHeight(30);
        JRDesignStaticText tituloText = new JRDesignStaticText();
        tituloText.setText(titulo);
        tituloText.setX(0);
        tituloText.setY(5);
        tituloText.setWidth(555);
        tituloText.setHeight(20);
        tituloText.setHorizontalTextAlign(HorizontalTextAlignEnum.CENTER);
        tituloText.setFontSize(14f);
        tituloText.setBold(true);
        titleBand.addElement(tituloText);
        design.setTitle(titleBand);

        // Column header
        JRDesignBand colHeader = new JRDesignBand();
        colHeader.setHeight(20);
        int x = 0;
        for (int i = 0; i < cabecalhos.length; i++) {
            JRDesignStaticText st = new JRDesignStaticText();
            st.setText(cabecalhos[i]);
            st.setX(x);
            st.setY(0);
            st.setWidth(larguras[i]);
            st.setHeight(20);
            st.setBold(true);
            colHeader.addElement(st);
            x += larguras[i];
        }
        design.setColumnHeader(colHeader);

        // Detail
        JRDesignBand detail = new JRDesignBand();
        detail.setHeight(15);
        x = 0;
        for (int i = 0; i < campos.length; i++) {
            JRDesignTextField tf = new JRDesignTextField();
            JRDesignExpression exp = new JRDesignExpression();
            exp.setValueClass(String.class);
            exp.setText("String.valueOf($F{" + campos[i] + "})");
            tf.setExpression(exp);
            tf.setX(x);
            tf.setY(0);
            tf.setWidth(larguras[i]);
            tf.setHeight(15);
            detail.addElement(tf);
            x += larguras[i];
        }
        ((JRDesignSection) design.getDetailSection()).addBand(detail);

        // Footer
        JRDesignBand pageFoot = new JRDesignBand();
        pageFoot.setHeight(20);
        JRDesignStaticText footText = new JRDesignStaticText();
        footText.setText("Gerado em: " + LocalDateTime.now().format(FORMATTER) + "  |  Agro Tech One");
        footText.setX(0);
        footText.setY(0);
        footText.setWidth(555);
        footText.setHeight(20);
        footText.setHorizontalTextAlign(HorizontalTextAlignEnum.RIGHT);
        footText.setFontSize(8f);
        pageFoot.addElement(footText);
        design.setPageFooter(pageFoot);

        JasperReport report = JasperCompileManager.compileReport(design);
        Map<String, Object> params = new HashMap<>();
        params.put("TITULO", titulo);
        JRBeanCollectionDataSource ds = new JRBeanCollectionDataSource(dados);
        return JasperFillManager.fillReport(report, params, ds);
    }

    // ===================== ESTILOS EXCEL =====================

    private CellStyle criarEstiloCabecalho(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREEN.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle criarEstiloMoeda(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        DataFormat format = wb.createDataFormat();
        style.setDataFormat(format.getFormat("\"R$\" #,##0.00"));
        return style;
    }
}
