package reports.utility;

import java.io.IOException;
import java.io.OutputStream;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRPrintElement;
import net.sf.jasperreports.engine.JRPrintFrame;
import net.sf.jasperreports.engine.JRPrintImage;
import net.sf.jasperreports.engine.JRPrintLine;
import net.sf.jasperreports.engine.JRPrintText;
import net.sf.jasperreports.engine.export.JRExporterGridCell;
import net.sf.jasperreports.engine.export.JRGridLayout.ExporterElements;

/**
 *
 * @author Administrator
 */
public class GenerateReport {

    javax.swing.table.DefaultTableModel dtm;

    /** Creates a new instance of GenerateReport */
    public GenerateReport(javax.swing.table.DefaultTableModel dtm) {
        this.dtm = dtm;
    }

    public void generateReport(String title) {
        try {
            System.out.println("in generate Report 27" + dtm.getRowCount());
            java.net.URL url = new java.net.URL(NewGenLibDesktopRoot.getInstance().getURLRoot() + "/NEWGEN_JR/AccessionRegister.xml");
            net.sf.jasperreports.engine.design.JasperDesign jd = net.sf.jasperreports.engine.xml.JRXmlLoader.load(url.openStream());
            net.sf.jasperreports.engine.JasperReport jr = net.sf.jasperreports.engine.JasperCompileManager.compileReport(jd);
            java.util.Map param = new java.util.HashMap();
            param.put("ReportTitle", title);
            param.put("MaxSalary", new Double(25000.00));
            Class.forName("org.postgresql.Driver");
            net.sf.jasperreports.engine.export.JRHtmlExporter exporter = new net.sf.jasperreports.engine.export.JRHtmlExporter();
            exporter.setParameter(net.sf.jasperreports.engine.JRExporterParameter.CHARACTER_ENCODING, "UTF-8");
            net.sf.jasperreports.engine.JasperPrint jp = net.sf.jasperreports.engine.JasperFillManager.fillReport(jr, param, new net.sf.jasperreports.engine.data.JRTableModelDataSource(dtm));
            if (jp.getPages().size() != 0) net.sf.jasperreports.view.JasperViewer.viewReport(jp, false); else javax.swing.JOptionPane.showMessageDialog(reports.DeskTopFrame.getInstance(), "There are no records in the selected report option.");
        } catch (Exception exp) {
            exp.printStackTrace();
        }
    }

    public void generateReport(String title, String fileLocation, String formatType) {
        try {
            System.out.println("in generate Report 27" + dtm.getRowCount());
            java.net.URL url = new java.net.URL(NewGenLibDesktopRoot.getInstance().getURLRoot() + "/NEWGEN_JR/AccessionRegister.xml");
            net.sf.jasperreports.engine.design.JasperDesign jd = net.sf.jasperreports.engine.xml.JRXmlLoader.load(url.openStream());
            System.out.println("in generate Report 30" + dtm.getRowCount());
            net.sf.jasperreports.engine.JasperReport jr = net.sf.jasperreports.engine.JasperCompileManager.compileReport(jd);
            System.out.println("in generate Report 32" + dtm.getRowCount());
            java.util.Map param = new java.util.HashMap();
            param.put("ReportTitle", "Accession Register");
            param.put("MaxSalary", new Double(25000.00));
            Class.forName("org.postgresql.Driver");
            System.out.println("in generate Report 37" + dtm.getRowCount());
            net.sf.jasperreports.engine.JasperPrint jp = net.sf.jasperreports.engine.JasperFillManager.fillReport(jr, param, new net.sf.jasperreports.engine.data.JRTableModelDataSource(dtm));
            System.out.println("in generate Report 39" + dtm.getRowCount());
            if (formatType.equals("xls")) {
                net.sf.jasperreports.engine.export.JRXlsExporter exporterXLS = new net.sf.jasperreports.engine.export.JRXlsExporter();
                exporterXLS.setParameter(net.sf.jasperreports.engine.export.JRXlsExporterParameter.JASPER_PRINT, jp);
                exporterXLS.setParameter(net.sf.jasperreports.engine.export.JRXlsExporterParameter.OUTPUT_FILE_NAME, fileLocation);
                exporterXLS.setParameter(net.sf.jasperreports.engine.export.JRXlsExporterParameter.IS_ONE_PAGE_PER_SHEET, Boolean.FALSE);
                exporterXLS.setParameter(net.sf.jasperreports.engine.export.JRXlsExporterParameter.IS_WHITE_PAGE_BACKGROUND, Boolean.FALSE);
                exporterXLS.setParameter(net.sf.jasperreports.engine.export.JRXlsExporterParameter.IS_REMOVE_EMPTY_SPACE_BETWEEN_ROWS, Boolean.TRUE);
                exporterXLS.exportReport();
            }
            if (formatType.equals("pdf")) {
                net.sf.jasperreports.engine.JasperExportManager.exportReportToPdfFile(jp, fileLocation);
            }
            System.out.println("in generate Report 43" + dtm.getRowCount());
        } catch (Exception exp) {
            exp.printStackTrace();
        }
    }

    public void generateCompleteAccessionReport(String fileLocation, String formatType, String title) {
        try {
            System.out.println("in generate Report 27" + dtm.getRowCount());
            java.net.URL url = new java.net.URL(NewGenLibDesktopRoot.getInstance().getURLRoot() + "/NEWGEN_JR/AccessionRegister.xml");
            net.sf.jasperreports.engine.design.JasperDesign jd = net.sf.jasperreports.engine.xml.JRXmlLoader.load(url.openStream());
            System.out.println("in generate Report 30" + dtm.getRowCount());
            net.sf.jasperreports.engine.JasperReport jr = net.sf.jasperreports.engine.JasperCompileManager.compileReport(jd);
            System.out.println("in generate Report 32" + dtm.getRowCount());
            java.util.Map param = new java.util.HashMap();
            param.put("ReportTitle", title);
            param.put("MaxSalary", new Double(25000.00));
            Class.forName("org.postgresql.Driver");
            System.out.println("in generate Report 37" + dtm.getRowCount());
            net.sf.jasperreports.engine.JasperPrint jp = net.sf.jasperreports.engine.JasperFillManager.fillReport(jr, param, new net.sf.jasperreports.engine.data.JRTableModelDataSource(dtm));
            System.out.println("in generate Report 39" + dtm.getRowCount());
            if (formatType.equals("xls")) {
                net.sf.jasperreports.engine.export.JRXlsExporter exporterXLS = new net.sf.jasperreports.engine.export.JRXlsExporter();
                exporterXLS.setParameter(net.sf.jasperreports.engine.export.JRXlsExporterParameter.JASPER_PRINT, jp);
                exporterXLS.setParameter(net.sf.jasperreports.engine.export.JRXlsExporterParameter.OUTPUT_FILE_NAME, fileLocation);
                exporterXLS.setParameter(net.sf.jasperreports.engine.export.JRXlsExporterParameter.IS_ONE_PAGE_PER_SHEET, Boolean.FALSE);
                exporterXLS.setParameter(net.sf.jasperreports.engine.export.JRXlsExporterParameter.IS_WHITE_PAGE_BACKGROUND, Boolean.FALSE);
                exporterXLS.setParameter(net.sf.jasperreports.engine.export.JRXlsExporterParameter.IS_REMOVE_EMPTY_SPACE_BETWEEN_ROWS, Boolean.TRUE);
                exporterXLS.exportReport();
            }
            if (formatType.equals("pdf")) {
                net.sf.jasperreports.engine.JasperExportManager.exportReportToPdfFile(jp, fileLocation);
            }
            System.out.println("in generate Report 43" + dtm.getRowCount());
        } catch (Exception exp) {
            exp.printStackTrace();
        }
    }

    public void generateCompleteSerieswiseAccessionReport(String fileLocation, String formatType, String title) {
        try {
            System.out.println("in generate Report 27" + dtm.getRowCount());
            java.net.URL url = new java.net.URL(NewGenLibDesktopRoot.getInstance().getURLRoot() + "/NEWGEN_JR/AccessionRegister.xml");
            net.sf.jasperreports.engine.design.JasperDesign jd = net.sf.jasperreports.engine.xml.JRXmlLoader.load(url.openStream());
            System.out.println("in generate Report 30" + dtm.getRowCount());
            net.sf.jasperreports.engine.JasperReport jr = net.sf.jasperreports.engine.JasperCompileManager.compileReport(jd);
            System.out.println("in generate Report 32" + dtm.getRowCount());
            java.util.Map param = new java.util.HashMap();
            param.put("ReportTitle", title);
            param.put("MaxSalary", new Double(25000.00));
            Class.forName("org.postgresql.Driver");
            System.out.println("in generate Report 37" + dtm.getRowCount());
            net.sf.jasperreports.engine.JasperPrint jp = net.sf.jasperreports.engine.JasperFillManager.fillReport(jr, param, new net.sf.jasperreports.engine.data.JRTableModelDataSource(dtm));
            System.out.println("in generate Report 39" + dtm.getRowCount());
            if (formatType.equals("xls")) {
                net.sf.jasperreports.engine.export.JRXlsExporter exporterXLS = new net.sf.jasperreports.engine.export.JRXlsExporter();
                exporterXLS.setParameter(net.sf.jasperreports.engine.export.JRXlsExporterParameter.JASPER_PRINT, jp);
                exporterXLS.setParameter(net.sf.jasperreports.engine.export.JRXlsExporterParameter.OUTPUT_FILE_NAME, fileLocation);
                exporterXLS.setParameter(net.sf.jasperreports.engine.export.JRXlsExporterParameter.IS_ONE_PAGE_PER_SHEET, Boolean.FALSE);
                exporterXLS.setParameter(net.sf.jasperreports.engine.export.JRXlsExporterParameter.IS_WHITE_PAGE_BACKGROUND, Boolean.FALSE);
                exporterXLS.setParameter(net.sf.jasperreports.engine.export.JRXlsExporterParameter.IS_REMOVE_EMPTY_SPACE_BETWEEN_ROWS, Boolean.TRUE);
                exporterXLS.exportReport();
            }
            if (formatType.equals("pdf")) {
                net.sf.jasperreports.engine.JasperExportManager.exportReportToPdfFile(jp, fileLocation);
            }
            System.out.println("in generate Report 43" + dtm.getRowCount());
        } catch (Exception exp) {
            exp.printStackTrace();
        }
    }

    public void generateCustomizedListReport(String xmlPath, String title) {
        try {
            System.out.println("in generate Report 27" + dtm.getRowCount());
            java.net.URL url = new java.net.URL(NewGenLibDesktopRoot.getInstance().getURLRoot() + "/NEWGEN_JR/" + xmlPath);
            net.sf.jasperreports.engine.design.JasperDesign jd = net.sf.jasperreports.engine.xml.JRXmlLoader.load(url.openStream());
            System.out.println("in generate Report 30" + dtm.getRowCount());
            net.sf.jasperreports.engine.JasperReport jr = net.sf.jasperreports.engine.JasperCompileManager.compileReport(jd);
            System.out.println("in generate Report 32" + dtm.getRowCount());
            java.util.Map param = new java.util.HashMap();
            param.put("ReportTitle", title);
            param.put("MaxSalary", new Double(25000.00));
            Class.forName("org.postgresql.Driver");
            System.out.println("in generate Report 37" + dtm.getRowCount());
            net.sf.jasperreports.engine.JasperPrint jp = net.sf.jasperreports.engine.JasperFillManager.fillReport(jr, param, new net.sf.jasperreports.engine.data.JRTableModelDataSource(dtm));
            System.out.println("in generate Report 39" + dtm.getRowCount());
            java.sql.Timestamp currentTime = new java.sql.Timestamp(java.util.Calendar.getInstance().getTimeInMillis());
            System.out.println("JP GETPAGES" + jp.getPages().size());
            if (jp.getPages().size() != 0) net.sf.jasperreports.view.JasperViewer.viewReport(jp, false); else javax.swing.JOptionPane.showMessageDialog(reports.DeskTopFrame.getInstance(), "There are no records in the selected report option.");
            System.out.println("in generate Report 43" + dtm.getRowCount());
        } catch (Exception exp) {
            exp.printStackTrace();
        }
    }

    public void generateStatisticalListReport(String xmlFileName, String title) {
        try {
            System.out.println("in generate Report 27" + dtm.getRowCount());
            java.net.URL url = new java.net.URL(NewGenLibDesktopRoot.getInstance().getURLRoot() + "/NEWGEN_JR/" + xmlFileName);
            net.sf.jasperreports.engine.design.JasperDesign jd = net.sf.jasperreports.engine.xml.JRXmlLoader.load(url.openStream());
            System.out.println("in generate Report 30" + dtm.getRowCount());
            net.sf.jasperreports.engine.JasperReport jr = net.sf.jasperreports.engine.JasperCompileManager.compileReport(jd);
            System.out.println("in generate Report 32" + dtm.getRowCount());
            java.util.Map param = new java.util.HashMap();
            param.put("ReportTitle", title);
            param.put("MaxSalary", new Double(25000.00));
            Class.forName("org.postgresql.Driver");
            System.out.println("in generate Report 37" + dtm.getRowCount());
            net.sf.jasperreports.engine.JasperPrint jp = net.sf.jasperreports.engine.JasperFillManager.fillReport(jr, param, new net.sf.jasperreports.engine.data.JRTableModelDataSource(dtm));
            System.out.println("in generate Report 39" + dtm.getRowCount());
            java.sql.Timestamp currentTime = new java.sql.Timestamp(java.util.Calendar.getInstance().getTimeInMillis());
            if (jp.getPages().size() != 0) net.sf.jasperreports.view.JasperViewer.viewReport(jp, false); else javax.swing.JOptionPane.showMessageDialog(reports.DeskTopFrame.getInstance(), "There are no records in the selected report option.");
            System.out.println("in generate Report 43" + dtm.getRowCount());
        } catch (Exception exp) {
            exp.printStackTrace();
        }
    }

    public void generateBudgetExpenditureReport(String title) {
        try {
            java.net.URL url = new java.net.URL(NewGenLibDesktopRoot.getInstance().getURLRoot() + "/NEWGEN_JR/BudgetExpenditure.xml");
            net.sf.jasperreports.engine.design.JasperDesign jd = net.sf.jasperreports.engine.xml.JRXmlLoader.load(url.openStream());
            net.sf.jasperreports.engine.JasperReport jr = net.sf.jasperreports.engine.JasperCompileManager.compileReport(jd);
            java.util.Map param = new java.util.HashMap();
            param.put("ReportTitle", title);
            param.put("MaxSalary", new Double(25000.00));
            Class.forName("org.postgresql.Driver");
            net.sf.jasperreports.engine.JasperPrint jp = net.sf.jasperreports.engine.JasperFillManager.fillReport(jr, param, new net.sf.jasperreports.engine.data.JRTableModelDataSource(dtm));
            dtm.setRowCount(0);
            if (jp.getPages().size() != 0) net.sf.jasperreports.view.JasperViewer.viewReport(jp, false); else javax.swing.JOptionPane.showMessageDialog(reports.DeskTopFrame.getInstance(), "There are no records in the selected report option.");
        } catch (Exception exp) {
            exp.printStackTrace();
        }
    }

    public void generateCurrentReservationReport() {
        try {
            java.net.URL url = new java.net.URL(NewGenLibDesktopRoot.getInstance().getURLRoot() + "/NEWGEN_JR/CurrentReservationReport.xml");
            net.sf.jasperreports.engine.design.JasperDesign jd = net.sf.jasperreports.engine.xml.JRXmlLoader.load(url.openStream());
            net.sf.jasperreports.engine.JasperReport jr = net.sf.jasperreports.engine.JasperCompileManager.compileReport(jd);
            java.util.Map param = new java.util.HashMap();
            String UTC = "";
            java.util.Calendar cl = java.util.Calendar.getInstance();
            String month = String.valueOf((cl.get(java.util.Calendar.MONTH) + 1));
            if (month.trim().length() == 1) month = "0" + month.trim();
            String day = String.valueOf(cl.get(java.util.Calendar.DATE));
            if (day.trim().length() == 1) day = "0" + day.trim();
            UTC = cl.get(java.util.Calendar.YEAR) + "-" + month + "-" + day;
            param.put("ReportTitle", "Report of Reservations for items as on '" + UTC + "'");
            param.put("MaxSalary", new Double(25000.00));
            Class.forName("org.postgresql.Driver");
            net.sf.jasperreports.engine.JasperPrint jp = net.sf.jasperreports.engine.JasperFillManager.fillReport(jr, param, new net.sf.jasperreports.engine.data.JRTableModelDataSource(dtm));
            dtm.setRowCount(0);
            if (jp.getPages().size() != 0) net.sf.jasperreports.view.JasperViewer.viewReport(jp, false); else javax.swing.JOptionPane.showMessageDialog(reports.DeskTopFrame.getInstance(), "There are no records in the selected report option.");
        } catch (Exception exp) {
            exp.printStackTrace();
        }
    }

    public void generateListofOverDueMaterial(String title) {
        try {
            java.net.URL url = new java.net.URL(NewGenLibDesktopRoot.getInstance().getURLRoot() + "/NEWGEN_JR/ListOfOverDueMaterial.xml");
            net.sf.jasperreports.engine.design.JasperDesign jd = net.sf.jasperreports.engine.xml.JRXmlLoader.load(url.openStream());
            net.sf.jasperreports.engine.JasperReport jr = net.sf.jasperreports.engine.JasperCompileManager.compileReport(jd);
            java.util.Map param = new java.util.HashMap();
            param.put("ReportTitle", title);
            param.put("MaxSalary", new Double(25000.00));
            Class.forName("org.postgresql.Driver");
            net.sf.jasperreports.engine.JasperPrint jp = net.sf.jasperreports.engine.JasperFillManager.fillReport(jr, param, new net.sf.jasperreports.engine.data.JRTableModelDataSource(dtm));
            dtm.setRowCount(0);
            if (jp.getPages().size() != 0) net.sf.jasperreports.view.JasperViewer.viewReport(jp, false); else javax.swing.JOptionPane.showMessageDialog(reports.DeskTopFrame.getInstance(), "There are no records in the selected report option.");
        } catch (Exception exp) {
            exp.printStackTrace();
        }
    }

    public void generatePatronReport(String title) {
        try {
            java.net.URL url = new java.net.URL(NewGenLibDesktopRoot.getInstance().getURLRoot() + "/NEWGEN_JR/PatronReport.xml");
            net.sf.jasperreports.engine.design.JasperDesign jd = net.sf.jasperreports.engine.xml.JRXmlLoader.load(url.openStream());
            net.sf.jasperreports.engine.JasperReport jr = net.sf.jasperreports.engine.JasperCompileManager.compileReport(jd);
            java.util.Map param = new java.util.HashMap();
            param.put("ReportTitle", title);
            param.put("MaxSalary", new Double(25000.00));
            Class.forName("org.postgresql.Driver");
            net.sf.jasperreports.engine.JasperPrint jp = net.sf.jasperreports.engine.JasperFillManager.fillReport(jr, param, new net.sf.jasperreports.engine.data.JRTableModelDataSource(dtm));
            dtm.setRowCount(0);
            if (jp.getPages().size() != 0) net.sf.jasperreports.view.JasperViewer.viewReport(jp, false); else javax.swing.JOptionPane.showMessageDialog(reports.DeskTopFrame.getInstance(), "There are no records in the selected report option.");
        } catch (Exception exp) {
            exp.printStackTrace();
        }
    }

    public void generateReportedLostItems(String title) {
        try {
            java.net.URL url = new java.net.URL(NewGenLibDesktopRoot.getInstance().getURLRoot() + "/NEWGEN_JR/ListOfReportedLost.xml");
            net.sf.jasperreports.engine.design.JasperDesign jd = net.sf.jasperreports.engine.xml.JRXmlLoader.load(url.openStream());
            net.sf.jasperreports.engine.JasperReport jr = net.sf.jasperreports.engine.JasperCompileManager.compileReport(jd);
            java.util.Map param = new java.util.HashMap();
            param.put("ReportTitle", title);
            param.put("MaxSalary", new Double(25000.00));
            Class.forName("org.postgresql.Driver");
            net.sf.jasperreports.engine.JasperPrint jp = net.sf.jasperreports.engine.JasperFillManager.fillReport(jr, param, new net.sf.jasperreports.engine.data.JRTableModelDataSource(dtm));
            dtm.setRowCount(0);
            if (jp.getPages().size() != 0) net.sf.jasperreports.view.JasperViewer.viewReport(jp, false); else javax.swing.JOptionPane.showMessageDialog(reports.DeskTopFrame.getInstance(), "There are no records in the selected report option.");
        } catch (Exception exp) {
            exp.printStackTrace();
        }
    }

    public void generateStockReport(String title) {
        try {
            java.net.URL url = new java.net.URL(NewGenLibDesktopRoot.getInstance().getURLRoot() + "/NEWGEN_JR/StockVerificationReport.xml");
            net.sf.jasperreports.engine.design.JasperDesign jd = net.sf.jasperreports.engine.xml.JRXmlLoader.load(url.openStream());
            net.sf.jasperreports.engine.JasperReport jr = net.sf.jasperreports.engine.JasperCompileManager.compileReport(jd);
            java.util.Map param = new java.util.HashMap();
            param.put("ReportTitle", title);
            Class.forName("org.postgresql.Driver");
            net.sf.jasperreports.engine.JasperPrint jp = net.sf.jasperreports.engine.JasperFillManager.fillReport(jr, param, new net.sf.jasperreports.engine.data.JRTableModelDataSource(dtm));
            if (jp.getPages().size() != 0) net.sf.jasperreports.view.JasperViewer.viewReport(jp, false); else javax.swing.JOptionPane.showMessageDialog(reports.DeskTopFrame.getInstance(), "There are no records in the selected report option.");
        } catch (Exception exp) {
            exp.printStackTrace();
        }
    }

    public void generateXyzReport(String fileLocation1, String reportTitle, String fileLocation2) {
        try {
            java.net.URL url = new java.net.URL(NewGenLibDesktopRoot.getInstance().getURLRoot() + "/NEWGEN_JR/" + fileLocation1);
            net.sf.jasperreports.engine.design.JasperDesign jd = net.sf.jasperreports.engine.xml.JRXmlLoader.load(url.openStream());
            net.sf.jasperreports.engine.JasperReport jr = net.sf.jasperreports.engine.JasperCompileManager.compileReport(jd);
            java.util.Map param = new java.util.HashMap();
            param.put("ReportTitle", reportTitle);
            Class.forName("org.postgresql.Driver");
            net.sf.jasperreports.engine.JasperPrint jp = net.sf.jasperreports.engine.JasperFillManager.fillReport(jr, param, new net.sf.jasperreports.engine.data.JRTableModelDataSource(dtm));
            java.sql.Timestamp currentTime = new java.sql.Timestamp(java.util.Calendar.getInstance().getTimeInMillis());
            if (jp.getPages().size() != 0) net.sf.jasperreports.view.JasperViewer.viewReport(jp, false); else javax.swing.JOptionPane.showMessageDialog(reports.DeskTopFrame.getInstance(), "There are no records in the selected report option.");
        } catch (Exception exp) {
            exp.printStackTrace();
        }
    }

    public void generateCheckOutList(String fileLocation1, String reportTitle) {
        try {
            java.net.URL url = new java.net.URL(NewGenLibDesktopRoot.getInstance().getURLRoot() + "/NEWGEN_JR/" + fileLocation1);
            net.sf.jasperreports.engine.design.JasperDesign jd = net.sf.jasperreports.engine.xml.JRXmlLoader.load(url.openStream());
            net.sf.jasperreports.engine.JasperReport jr = net.sf.jasperreports.engine.JasperCompileManager.compileReport(jd);
            java.util.Map param = new java.util.HashMap();
            param.put("ReportTitle", reportTitle);
            Class.forName("org.postgresql.Driver");
            net.sf.jasperreports.engine.JasperPrint jp = net.sf.jasperreports.engine.JasperFillManager.fillReport(jr, param, new net.sf.jasperreports.engine.data.JRTableModelDataSource(dtm));
            java.sql.Timestamp currentTime = new java.sql.Timestamp(java.util.Calendar.getInstance().getTimeInMillis());
            if (jp.getPages().size() != 0) net.sf.jasperreports.view.JasperViewer.viewReport(jp, false); else javax.swing.JOptionPane.showMessageDialog(reports.DeskTopFrame.getInstance(), "There are no records in the selected report option.");
        } catch (Exception exp) {
            exp.printStackTrace();
        }
    }

    public void generateVendorReport(String title) {
        try {
            java.net.URL url = new java.net.URL(NewGenLibDesktopRoot.getInstance().getURLRoot() + "/NEWGEN_JR/VendorReport.xml");
            net.sf.jasperreports.engine.design.JasperDesign jd = net.sf.jasperreports.engine.xml.JRXmlLoader.load(url.openStream());
            net.sf.jasperreports.engine.JasperReport jr = net.sf.jasperreports.engine.JasperCompileManager.compileReport(jd);
            java.util.Map param = new java.util.HashMap();
            param.put("ReportTitle", title);
            Class.forName("org.postgresql.Driver");
            net.sf.jasperreports.engine.JasperPrint jp = net.sf.jasperreports.engine.JasperFillManager.fillReport(jr, param, new net.sf.jasperreports.engine.data.JRTableModelDataSource(dtm));
            java.sql.Timestamp currentTime = new java.sql.Timestamp(java.util.Calendar.getInstance().getTimeInMillis());
            if (jp.getPages().size() != 0) net.sf.jasperreports.view.JasperViewer.viewReport(jp, false); else javax.swing.JOptionPane.showMessageDialog(reports.DeskTopFrame.getInstance(), "There are no records in the selected report option.");
        } catch (Exception exp) {
            exp.printStackTrace();
        }
    }

    public void generateCirculationReportForWholeDay(String title) {
        try {
            java.net.URL url = new java.net.URL(NewGenLibDesktopRoot.getInstance().getURLRoot() + "/NEWGEN_JR/ForWholeDay.xml");
            System.out.println("in generate Report 27" + dtm.getRowCount());
            net.sf.jasperreports.engine.design.JasperDesign jd = net.sf.jasperreports.engine.xml.JRXmlLoader.load(url.openStream());
            System.out.println("in generate Report 30" + dtm.getRowCount());
            net.sf.jasperreports.engine.JasperReport jr = net.sf.jasperreports.engine.JasperCompileManager.compileReport(jd);
            System.out.println("in generate Report 32" + dtm.getRowCount());
            java.util.Map param = new java.util.HashMap();
            param.put("ReportTitle", title);
            Class.forName("org.postgresql.Driver");
            System.out.println("in generate Report 37" + dtm.getRowCount());
            net.sf.jasperreports.engine.JasperPrint jp = net.sf.jasperreports.engine.JasperFillManager.fillReport(jr, param, new net.sf.jasperreports.engine.data.JRTableModelDataSource(dtm));
            System.out.println("in generate Report 39" + dtm.getRowCount());
            java.sql.Timestamp currentTime = new java.sql.Timestamp(java.util.Calendar.getInstance().getTimeInMillis());
            if (jp.getPages().size() != 0) net.sf.jasperreports.view.JasperViewer.viewReport(jp, false); else javax.swing.JOptionPane.showMessageDialog(reports.DeskTopFrame.getInstance(), "There are no records in the selected report option.");
            System.out.println("in generate Report 43" + dtm.getRowCount());
        } catch (Exception exp) {
            exp.printStackTrace();
        }
    }

    public void generateCirculationReportForHourOfDay(String title) {
        try {
            java.net.URL url = new java.net.URL(NewGenLibDesktopRoot.getInstance().getURLRoot() + "/NEWGEN_JR/ForHourOfDay.xml");
            System.out.println("in generate Report 27" + dtm.getRowCount());
            net.sf.jasperreports.engine.design.JasperDesign jd = net.sf.jasperreports.engine.xml.JRXmlLoader.load(url.openStream());
            System.out.println("in generate Report 30" + dtm.getRowCount());
            net.sf.jasperreports.engine.JasperReport jr = net.sf.jasperreports.engine.JasperCompileManager.compileReport(jd);
            System.out.println("in generate Report 32" + dtm.getRowCount());
            java.util.Map param = new java.util.HashMap();
            param.put("ReportTitle", title);
            Class.forName("org.postgresql.Driver");
            System.out.println("in generate Report 37" + dtm.getRowCount());
            net.sf.jasperreports.engine.JasperPrint jp = net.sf.jasperreports.engine.JasperFillManager.fillReport(jr, param, new net.sf.jasperreports.engine.data.JRTableModelDataSource(dtm));
            System.out.println("in generate Report 39" + dtm.getRowCount());
            java.sql.Timestamp currentTime = new java.sql.Timestamp(java.util.Calendar.getInstance().getTimeInMillis());
            if (jp.getPages().size() != 0) net.sf.jasperreports.view.JasperViewer.viewReport(jp, false); else javax.swing.JOptionPane.showMessageDialog(reports.DeskTopFrame.getInstance(), "There are no records in the selected report option.");
            System.out.println("in generate Report 43" + dtm.getRowCount());
        } catch (Exception exp) {
            exp.printStackTrace();
        }
    }

    public void generateMissingIssueReport() {
        try {
            java.net.URL url = new java.net.URL(NewGenLibDesktopRoot.getInstance().getURLRoot() + "/NEWGEN_JR/ListMissingIssue.xml");
            System.out.println("in generate Report 27" + dtm.getRowCount());
            net.sf.jasperreports.engine.design.JasperDesign jd = net.sf.jasperreports.engine.xml.JRXmlLoader.load(url.openStream());
            System.out.println("in generate Report 30" + dtm.getRowCount());
            net.sf.jasperreports.engine.JasperReport jr = net.sf.jasperreports.engine.JasperCompileManager.compileReport(jd);
            System.out.println("in generate Report 32" + dtm.getRowCount());
            java.util.Map param = new java.util.HashMap();
            param.put("ReportTitle", "Missing issue report");
            Class.forName("org.postgresql.Driver");
            System.out.println("in generate Report 37" + dtm.getRowCount());
            net.sf.jasperreports.engine.JasperPrint jp = net.sf.jasperreports.engine.JasperFillManager.fillReport(jr, param, new net.sf.jasperreports.engine.data.JRTableModelDataSource(dtm));
            System.out.println("in generate Report 39" + dtm.getRowCount());
            java.sql.Timestamp currentTime = new java.sql.Timestamp(java.util.Calendar.getInstance().getTimeInMillis());
            if (jp.getPages().size() != 0) net.sf.jasperreports.view.JasperViewer.viewReport(jp, false); else javax.swing.JOptionPane.showMessageDialog(reports.DeskTopFrame.getInstance(), "There are no records in the selected report option.");
            System.out.println("in generate Report 43" + dtm.getRowCount());
        } catch (Exception exp) {
            exp.printStackTrace();
        }
    }

    public void generateUnfullFilledSubscriptionReport() {
        try {
            java.net.URL url = new java.net.URL(NewGenLibDesktopRoot.getInstance().getURLRoot() + "/NEWGEN_JR/UnfullfilledSubscriptionReport.xml");
            System.out.println("in generate Report 27" + dtm.getRowCount());
            net.sf.jasperreports.engine.design.JasperDesign jd = net.sf.jasperreports.engine.xml.JRXmlLoader.load(url.openStream());
            System.out.println("in generate Report 30" + dtm.getRowCount());
            net.sf.jasperreports.engine.JasperReport jr = net.sf.jasperreports.engine.JasperCompileManager.compileReport(jd);
            System.out.println("in generate Report 32" + dtm.getRowCount());
            java.util.Map param = new java.util.HashMap();
            param.put("ReportTitle", "Unfull filled subscription report");
            Class.forName("org.postgresql.Driver");
            System.out.println("in generate Report 37" + dtm.getRowCount());
            net.sf.jasperreports.engine.JasperPrint jp = net.sf.jasperreports.engine.JasperFillManager.fillReport(jr, param, new net.sf.jasperreports.engine.data.JRTableModelDataSource(dtm));
            System.out.println("in generate Report 39" + dtm.getRowCount());
            java.sql.Timestamp currentTime = new java.sql.Timestamp(java.util.Calendar.getInstance().getTimeInMillis());
            if (jp.getPages().size() != 0) net.sf.jasperreports.view.JasperViewer.viewReport(jp, false); else javax.swing.JOptionPane.showMessageDialog(reports.DeskTopFrame.getInstance(), "There are no records in the selected report option.");
            System.out.println("in generate Report 43" + dtm.getRowCount());
        } catch (Exception exp) {
            exp.printStackTrace();
        }
    }

    public void generateBindingReport() {
        try {
            java.net.URL url = new java.net.URL(NewGenLibDesktopRoot.getInstance().getURLRoot() + "/NEWGEN_JR/ListBindingReport.xml");
            System.out.println("in generate Report 27" + dtm.getRowCount());
            net.sf.jasperreports.engine.design.JasperDesign jd = net.sf.jasperreports.engine.xml.JRXmlLoader.load(url.openStream());
            System.out.println("in generate Report 30" + dtm.getRowCount());
            net.sf.jasperreports.engine.JasperReport jr = net.sf.jasperreports.engine.JasperCompileManager.compileReport(jd);
            System.out.println("in generate Report 32" + dtm.getRowCount());
            java.util.Map param = new java.util.HashMap();
            param.put("ReportTitle", "List items under binding report");
            Class.forName("org.postgresql.Driver");
            System.out.println("in generate Report 37" + dtm.getRowCount());
            net.sf.jasperreports.engine.JasperPrint jp = net.sf.jasperreports.engine.JasperFillManager.fillReport(jr, param, new net.sf.jasperreports.engine.data.JRTableModelDataSource(dtm));
            System.out.println("in generate Report 39" + dtm.getRowCount());
            java.sql.Timestamp currentTime = new java.sql.Timestamp(java.util.Calendar.getInstance().getTimeInMillis());
            if (jp.getPages().size() != 0) net.sf.jasperreports.view.JasperViewer.viewReport(jp, false); else javax.swing.JOptionPane.showMessageDialog(reports.DeskTopFrame.getInstance(), "There are no records in the selected report option.");
            System.out.println("in generate Report 43" + dtm.getRowCount());
        } catch (Exception exp) {
            exp.printStackTrace();
        }
    }

    public void generateDocumentationListReport(String title) {
        try {
            java.net.URL url = new java.net.URL(NewGenLibDesktopRoot.getInstance().getURLRoot() + "/NEWGEN_JR/DocumentationList.xml");
            System.out.println("in generate Report 27" + dtm.getRowCount());
            net.sf.jasperreports.engine.design.JasperDesign jd = net.sf.jasperreports.engine.xml.JRXmlLoader.load(url.openStream());
            System.out.println("in generate Report 30" + dtm.getRowCount());
            net.sf.jasperreports.engine.JasperReport jr = net.sf.jasperreports.engine.JasperCompileManager.compileReport(jd);
            System.out.println("in generate Report 32" + dtm.getRowCount());
            java.util.Map param = new java.util.HashMap();
            param.put("ReportTitle", title);
            Class.forName("org.postgresql.Driver");
            System.out.println("in generate Report 37" + dtm.getRowCount());
            net.sf.jasperreports.engine.JasperPrint jp = net.sf.jasperreports.engine.JasperFillManager.fillReport(jr, param, new net.sf.jasperreports.engine.data.JRTableModelDataSource(dtm));
            System.out.println("in generate Report 39" + dtm.getRowCount());
            java.sql.Timestamp currentTime = new java.sql.Timestamp(java.util.Calendar.getInstance().getTimeInMillis());
            if (jp.getPages().size() != 0) net.sf.jasperreports.view.JasperViewer.viewReport(jp, false); else javax.swing.JOptionPane.showMessageDialog(reports.DeskTopFrame.getInstance(), "There are no records in the selected report option.");
            System.out.println("in generate Report 43" + dtm.getRowCount());
        } catch (Exception exp) {
            exp.printStackTrace();
        }
    }

    public void generateListOfSubscriptions() {
        try {
            java.net.URL url = new java.net.URL(NewGenLibDesktopRoot.getInstance().getURLRoot() + "/NEWGEN_JR/ListOfSubscriptions.xml");
            System.out.println(NewGenLibDesktopRoot.getRoot() + "/NEWGEN_JR/ListOfSubscriptions.xml");
            net.sf.jasperreports.engine.design.JasperDesign jd = net.sf.jasperreports.engine.xml.JRXmlLoader.load(url.openStream());
            System.out.println("in generate Report 30" + dtm.getRowCount());
            net.sf.jasperreports.engine.JasperReport jr = net.sf.jasperreports.engine.JasperCompileManager.compileReport(jd);
            System.out.println("in generate Report 32" + dtm.getRowCount());
            java.util.Map param = new java.util.HashMap();
            param.put("ReportTitle", "List of subscriptions");
            Class.forName("org.postgresql.Driver");
            System.out.println("in generate Report 37" + dtm.getRowCount());
            net.sf.jasperreports.engine.JasperPrint jp = net.sf.jasperreports.engine.JasperFillManager.fillReport(jr, param, new net.sf.jasperreports.engine.data.JRTableModelDataSource(dtm));
            System.out.println("in generate Report 39" + dtm.getRowCount());
            java.sql.Timestamp currentTime = new java.sql.Timestamp(java.util.Calendar.getInstance().getTimeInMillis());
            if (jp.getPages().size() != 0) net.sf.jasperreports.view.JasperViewer.viewReport(jp, false); else javax.swing.JOptionPane.showMessageDialog(reports.DeskTopFrame.getInstance(), "There are no records in the selected report option.");
            System.out.println("in generate Report 43" + dtm.getRowCount());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
