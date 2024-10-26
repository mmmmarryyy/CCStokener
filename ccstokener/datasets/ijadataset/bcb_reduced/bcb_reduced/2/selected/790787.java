package com.giews.report.utils;

import java.util.Map;
import java.util.*;
import java.io.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.DriverManager;
import java.nio.channels.FileChannel;
import java.net.URL;
import java.net.URLConnection;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRCsvDataSource;
import net.sf.jasperreports.engine.data.JRMapArrayDataSource;
import net.sf.jasperreports.engine.base.JRBaseStaticText;
import net.sf.jasperreports.engine.base.JRBaseImage;
import net.sf.jasperreports.engine.design.*;
import net.sf.jasperreports.engine.util.JRLoader;
import com.giews.report.component.Title;
import com.giews.report.component.Text;
import com.giews.report.component.Table;
import com.giews.report.component.Chart;
import com.giews.report.utils.GIEWSDataBuilder;
import com.giews.report.utils.StyledTextConvertor;
import org.fao.waicent.kids.giews.table.TableObject;

public class JasperGenerator {

    public static final String MAKE_JRPTINT = "compile";

    public static final String WRITE_XML = "writeXml";

    public static final String MAKE_PDF = "pdf";

    public static final String MAKE_DATASOURCE = "datasource";

    public static final int TITLE_HEIGHT = 40;

    private Collection titles = new ArrayList();

    private Collection maps = new ArrayList();

    private Collection charts = new ArrayList();

    private Table table;

    private Collection texts = new ArrayList();

    private JasperDesign jasperDesign;

    private Map expressions = new HashMap();

    private TableObject tableObject;

    String templateStorePath;

    String templateStoreURL;

    private String name;

    private int pageWidth;

    private int pageHeight;

    private String sessionKey;

    private String[] columnes;

    private ArrayList filesToCopy = new ArrayList();

    public String getSessionKey() {
        return sessionKey;
    }

    public void setSessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
    }

    public String getTemplateStoreURL() {
        return templateStoreURL;
    }

    public void setTemplateStoreURL(String templateStoreURL) {
        this.templateStoreURL = templateStoreURL;
    }

    public String getTemplateStorePath() {
        return templateStorePath;
    }

    public void setTemplateStorePath(String templateStorePath) {
        this.templateStorePath = templateStorePath;
    }

    public int getLeftMargin() {
        return leftMargin;
    }

    public void setLeftMargin(int leftMargin) {
        this.leftMargin = leftMargin;
    }

    public int getRightMargin() {
        return rightMargin;
    }

    public void setRightMargin(int rightMargin) {
        this.rightMargin = rightMargin;
    }

    public int getTopMargin() {
        return topMargin;
    }

    public void setTopMargin(int topMargin) {
        this.topMargin = topMargin;
    }

    public int getBottomMargin() {
        return bottomMargin;
    }

    public void setBottomMargin(int bottomMargin) {
        this.bottomMargin = bottomMargin;
    }

    int leftMargin = 40;

    int rightMargin = 40;

    int topMargin = 50;

    int bottomMargin = 50;

    int titleHeight = 300;

    public JasperGenerator() {
        jasperDesign = new JasperDesign();
    }

    public JasperDesign getJasperDesign() {
        return jasperDesign;
    }

    private TreeMap ArrangeComponents() throws JRException {
        TreeMap tree = new TreeMap();
        for (Iterator it = texts.iterator(); it.hasNext(); ) {
            Text text = (Text) it.next();
            tree.put(new Integer(text.getY() + text.getHeight()), text);
        }
        for (Iterator it = maps.iterator(); it.hasNext(); ) {
            com.giews.report.component.Map newMap = (com.giews.report.component.Map) it.next();
            tree.put(new Integer(newMap.getY() + newMap.getHeight()), newMap);
        }
        for (Iterator it = charts.iterator(); it.hasNext(); ) {
            com.giews.report.component.Chart newChart = (com.giews.report.component.Chart) it.next();
            tree.put(new Integer(newChart.getY() + newChart.getHeight()), newChart);
        }
        if (table != null) {
            tree.put(new Integer(table.getY() + table.getHeight()), table);
        }
        return tree;
    }

    private void buildJasperDesign(boolean saveFiles) throws JRException, IOException {
        jasperDesign.setName(name);
        jasperDesign.setPageWidth(pageWidth);
        jasperDesign.setPageHeight(pageHeight);
        jasperDesign.setColumnWidth(1);
        jasperDesign.setLeftMargin(leftMargin);
        jasperDesign.setRightMargin(rightMargin);
        jasperDesign.setTopMargin(topMargin);
        jasperDesign.setBottomMargin(bottomMargin);
        byte b1 = 3;
        jasperDesign.setWhenNoDataType(b1);
        JRDesignStyle normalStyle = new JRDesignStyle();
        normalStyle.setName("Arial_Normal");
        normalStyle.setDefault(true);
        normalStyle.setFontName("Arial");
        normalStyle.setFontSize(12);
        normalStyle.setPdfFontName("Helvetica");
        normalStyle.setPdfEncoding("Cp1252");
        normalStyle.setPdfEmbedded(false);
        jasperDesign.addStyle(normalStyle);
        JRDesignStyle boldStyle = new JRDesignStyle();
        boldStyle.setName("Arial_Bold");
        boldStyle.setFontName("Arial");
        boldStyle.setFontSize(12);
        boldStyle.setBold(true);
        boldStyle.setPdfFontName("Helvetica-Bold");
        boldStyle.setPdfEncoding("Cp1252");
        boldStyle.setPdfEmbedded(false);
        jasperDesign.addStyle(boldStyle);
        JRDesignStyle italicStyle = new JRDesignStyle();
        italicStyle.setName("Arial_Italic");
        italicStyle.setFontName("Arial");
        italicStyle.setFontSize(12);
        italicStyle.setItalic(true);
        italicStyle.setPdfFontName("Helvetica-Oblique");
        italicStyle.setPdfEncoding("Cp1252");
        italicStyle.setPdfEmbedded(false);
        jasperDesign.addStyle(italicStyle);
        JRDesignBand band = new JRDesignBand();
        for (Iterator it = titles.iterator(); it.hasNext(); ) {
            Title title = (Title) it.next();
            title.setX(pageWidth / 6);
            title.setY(0);
            title.setWidth(2 * pageWidth / 3);
            title.setHeight(19);
            title.setHorizontalAlignment((byte) 2);
            title.setVerticalAlignment((byte) 2);
            band.addElement(title);
            title.setKey("TITLE");
        }
        jasperDesign.setTitle(band);
        int counter = 0;
        if (table == null) {
            band.setHeight(pageHeight - jasperDesign.getTopMargin() - jasperDesign.getBottomMargin());
            for (Iterator it = texts.iterator(); it.hasNext(); ) {
                addObjectToBind((Text) it.next(), band, counter++, 0);
            }
            for (Iterator it = maps.iterator(); it.hasNext(); counter++) {
                addObjectToBind((com.giews.report.component.Map) it.next(), band, counter++, 0);
            }
            for (Iterator it = charts.iterator(); it.hasNext(); ) {
                addObjectToBind((com.giews.report.component.Chart) it.next(), band, counter++, 0);
            }
            jasperDesign.setTitle(band);
        } else {
            TreeMap components = ArrangeComponents();
            int currentBandHeight = TITLE_HEIGHT;
            int tableOffset = 0;
            Iterator it = components.keySet().iterator();
            boolean table_processed = false;
            while (it.hasNext()) {
                Integer key = (Integer) it.next();
                Object obj = components.get(key);
                if (obj instanceof Table) {
                    if (table != null) {
                        band.setHeight(table.getY());
                        jasperDesign.setTitle(band);
                        if (saveFiles) table.prepareXMLAndMakeDataSource(tableObject, jasperDesign, columnes, getSavePath(name + "_table.xml"));
                        tableOffset = table.getY() + table.getHeight();
                        band = new JRDesignBand();
                        table_processed = true;
                    }
                } else if (obj instanceof com.giews.report.component.Map) {
                    currentBandHeight = addObjectToBind(obj, band, counter++, tableOffset);
                } else if (obj instanceof Chart) {
                    currentBandHeight = addObjectToBind(obj, band, counter++, tableOffset);
                } else if (obj instanceof Text) {
                    currentBandHeight = addObjectToBind(obj, band, counter++, tableOffset);
                }
            }
            if (table_processed) {
                band.setHeight(currentBandHeight);
                jasperDesign.setSummary(band);
            }
        }
        copyFiles();
    }

    protected String getSavePath(String fileName) {
        StringBuffer buff = new StringBuffer(templateStorePath).append(name);
        if (!(new File(buff.toString()).isDirectory())) {
            (new File(buff.toString())).mkdirs();
        }
        return buff.append(File.separator).append(fileName).toString();
    }

    protected int addObjectToBind(Object obj, JRDesignBand band, int counter, int tableOffset) throws JRException {
        if (obj instanceof com.giews.report.component.Map) {
            com.giews.report.component.Map newMap = (com.giews.report.component.Map) obj;
            newMap.setScaleImage(JRDesignImage.SCALE_IMAGE_FILL_FRAME);
            newMap.setX(newMap.getX());
            newMap.setY(newMap.getY() - tableOffset);
            String fileName = "panel_map_" + counter;
            if (newMap.getSourceType() == 1) {
                fileName += "_dyn";
            }
            JRDesignParameter parameter = new JRDesignParameter();
            parameter.setName(fileName);
            parameter.setValueClass(java.lang.String.class);
            jasperDesign.addParameter(parameter);
            JRDesignExpression expression = new JRDesignExpression();
            expression.setValueClass(java.lang.String.class);
            expression.setText("$P{" + fileName + "}");
            newMap.setExpression(expression);
            newMap.setKey(fileName);
            if (newMap.getSourceType() == 2) {
                expressions.put(fileName, getSavePath(fileName));
                filesToCopy.add(new String[] { newMap.getMapPath(), getSavePath(fileName) });
            } else {
                expressions.put(fileName, newMap.getMapPath().replaceAll(" ", "%20"));
            }
            band.addElement(newMap);
            return newMap.getY() + newMap.getHeight();
        } else if (obj instanceof Chart) {
            com.giews.report.component.Chart newChart = (com.giews.report.component.Chart) obj;
            newChart.setScaleImage(JRDesignImage.SCALE_IMAGE_FILL_FRAME);
            newChart.setX(newChart.getX());
            newChart.setY(newChart.getY() - tableOffset);
            String fileName = "panel_chart_" + counter;
            if (newChart.getSourceType() == 1) {
                fileName += "_dyn";
            }
            JRDesignParameter parameter = new JRDesignParameter();
            parameter.setName(fileName);
            parameter.setValueClass(java.lang.String.class);
            jasperDesign.addParameter(parameter);
            JRDesignExpression expression = new JRDesignExpression();
            expression.setValueClass(java.lang.String.class);
            expression.setText("$P{" + fileName + "}");
            newChart.setExpression(expression);
            newChart.setKey(fileName);
            if (newChart.getSourceType() == 2) {
                expressions.put(fileName, getSavePath(fileName));
                filesToCopy.add(new String[] { newChart.getChartPath(), getSavePath(fileName) });
            } else {
                expressions.put(fileName, newChart.getChartPath().replaceAll(" ", "%20"));
            }
            band.addElement(newChart);
            return newChart.getY() + newChart.getHeight();
        } else if (obj instanceof Text) {
            Text text = (Text) obj;
            text.setX(text.getX());
            text.setY(text.getY() - tableOffset);
            text.setKey("panel_text_" + counter);
            band.addElement(text);
            return text.getY() + text.getHeight();
        }
        return -1;
    }

    public int getPageWidth() {
        return pageWidth;
    }

    public void setPageWidth(int pageWidth) {
        this.pageWidth = pageWidth;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPageHeight() {
        return pageHeight;
    }

    public void setPageHeight(int pageHeight) {
        this.pageHeight = pageHeight;
    }

    /**
     * method append Title
     *
     * @param title
     */
    public void addTitle(Title title) {
        titles.add(title);
    }

    /**
     * read jrxmlfile and return Titles collection
     *
     * @return Collection
     */
    public Collection getTitles() {
        return titles;
    }

    public String getReportTitle() {
        if (titles.size() > 0) {
            Title title = (Title) ((ArrayList) titles).get(0);
            return title.getText();
        }
        return "";
    }

    /**
     * method append Texts
     *
     * @param text
     */
    public void addText(Text text) {
        texts.add(text);
    }

    public Collection getTexts() {
        return texts;
    }

    /**
     * method append mapPath
     *
     * @param newMap
     */
    public void addMap(com.giews.report.component.Map newMap) {
        maps.add(newMap);
    }

    public void addChart(com.giews.report.component.Chart newChart) {
        charts.add(newChart);
    }

    public Collection getMaps() {
        return maps;
    }

    public Collection getCharts() {
        return charts;
    }

    public void addChart(String sql, Map columns) {
        charts.add(sql);
    }

    public void setTable(Table table) {
        this.table = table;
    }

    public Table getTable() {
        return table;
    }

    private JRCsvDataSource getDataSource() throws IOException {
        String[] columnNames = new String[] { "city", "id", "name", "address", "state" };
        JRCsvDataSource ds = new JRCsvDataSource(new File("c://temp/CsvDataSource.txt"));
        ds.setRecordDelimiter("\r\n");
        ds.setColumnNames(columnNames);
        return ds;
    }

    public String getTableName() {
        return "table1";
    }

    public String[] getTableColumns(String tableName) {
        String[] columnNames = new String[] { "city", "id", "name", "address", "state" };
        return columnNames;
    }

    private Connection getConnection() throws ClassNotFoundException, SQLException {
        String driver = "com.mysql.jdbc.Driver";
        String connectString = "jdbc:mysql://localhost:3306/myhibtest";
        String user = "root";
        String password = "";
        Class.forName(driver);
        Connection conn = DriverManager.getConnection(connectString, user, password);
        return conn;
    }

    /**
     * dependis taskName method generate jrprint or jsxml files.
     *
     * @param taskName
     * @param fileName
     */
    public void generate(String taskName, String fileName) throws JRException, IOException {
        if (MAKE_JRPTINT.equals(taskName)) {
            String tempFile;
            buildJasperDesign(true);
            JasperCompileManager.compileReportToFile(jasperDesign, fileName);
            if (table != null && table.getDataSource() != null) {
                tempFile = JasperFillManager.fillReportToFile(fileName, expressions, table.getDataSource());
            } else {
                tempFile = JasperFillManager.fillReportToFile(fileName, expressions);
            }
            JasperCompileManager.writeReportToXmlFile(fileName);
            JasperExportManager.exportReportToPdfFile(tempFile);
        }
        if (MAKE_PDF.equals(taskName)) {
            String tempFile;
            buildJasperDesign(false);
            if (table != null && table.getDataSource() != null) {
                table.setColumns(table.getSlectedColumns(), jasperDesign);
                JasperCompileManager.compileReportToFile(jasperDesign, fileName);
                tempFile = JasperFillManager.fillReportToFile(fileName, expressions, table.getDataSource());
            } else {
                JasperCompileManager.compileReportToFile(jasperDesign, fileName);
                tempFile = JasperFillManager.fillReportToFile(fileName, expressions);
            }
            JasperExportManager.exportReportToPdfFile(tempFile);
        } else if (MAKE_DATASOURCE.equals(taskName)) {
            JasperCompileManager.compileReportToFile(jasperDesign, fileName);
        }
    }

    public void readJRXML(String fileName, GIEWSDataBuilder dataBuilder) {
        try {
            JasperReport subreport = (JasperReport) JRLoader.loadObject(fileName);
            bottomMargin = subreport.getBottomMargin();
            leftMargin = subreport.getLeftMargin();
            topMargin = subreport.getTopMargin();
            rightMargin = subreport.getRightMargin();
            pageHeight = subreport.getPageHeight();
            pageWidth = subreport.getPageWidth();
            name = subreport.getName();
            JRBand b = (JRBand) subreport.getTitle();
            int offset = 0;
            if (b != null) {
                offset = b.getHeight();
                readBand(b, 0, dataBuilder);
            }
            b = (JRBand) subreport.getDetail();
            String tablePath = getSavePath(name + "_table.xml");
            if (b != null && (new File(tablePath)).exists()) {
                table = new Table();
                table.compileTableFromSavedXMLFile(tablePath);
                offset += table.getHeight();
            }
            b = (JRBand) subreport.getSummary();
            if (b != null) readBand(b, offset, dataBuilder);
        } catch (JRException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void readBand(JRBand b, int offset, GIEWSDataBuilder dataBuilder) {
        for (int i = 0; i < b.getElements().length; i++) {
            if (b.getElements()[i] instanceof JRBaseStaticText) {
                JRBaseStaticText txtOrTlt = (JRBaseStaticText) b.getElements()[i];
                if (txtOrTlt.getKey().equals("TITLE")) {
                    Title title = new Title();
                    title.setX(txtOrTlt.getX());
                    title.setY(txtOrTlt.getY());
                    title.setWidth(txtOrTlt.getWidth());
                    title.setHeight(txtOrTlt.getHeight());
                    title.setText(txtOrTlt.getText());
                    title.setKey(txtOrTlt.getKey());
                    titles.add(title);
                } else if (txtOrTlt.getKey().startsWith("panel_text")) {
                    Text text = new Text();
                    text.setX(txtOrTlt.getX());
                    text.setY(txtOrTlt.getY() + offset);
                    text.setWidth(txtOrTlt.getWidth());
                    text.setHeight(txtOrTlt.getHeight());
                    text.setText(txtOrTlt.getText());
                    text.setStyledText(true);
                    text.setKey(txtOrTlt.getKey());
                    texts.add(text);
                }
            } else if (b.getElements()[i] instanceof JRBaseImage) {
                JRBaseImage mapOrChart = (JRBaseImage) b.getElements()[i];
                if (mapOrChart.getKey().startsWith("panel_map")) {
                    com.giews.report.component.Map m = new com.giews.report.component.Map(jasperDesign);
                    if (mapOrChart.getKey().indexOf("_dyn") > -1) {
                        m.setSourceType(2);
                    }
                    m.setX(mapOrChart.getX());
                    m.setY(mapOrChart.getY() + offset);
                    m.setHeight(mapOrChart.getHeight());
                    m.setWidth(mapOrChart.getWidth());
                    if (dataBuilder != null && m.getSourceType() == 2) {
                        com.giews.report.beans.Component component = dataBuilder.getMap();
                        if (component == null) {
                        } else {
                            m.setMapPath(component.getUrl());
                        }
                    } else {
                        m.setMapPath(templateStoreURL + name + "/" + mapOrChart.getKey());
                    }
                    m.setKey(mapOrChart.getKey());
                    maps.add(m);
                } else {
                    com.giews.report.component.Chart m = new com.giews.report.component.Chart(jasperDesign);
                    if (mapOrChart.getKey().indexOf("_dyn") > -1) {
                        m.setSourceType(2);
                    }
                    m.setX(mapOrChart.getX());
                    m.setY(mapOrChart.getY() + offset);
                    m.setHeight(mapOrChart.getHeight());
                    m.setWidth(mapOrChart.getWidth());
                    if (dataBuilder != null && m.getSourceType() == 2) {
                        com.giews.report.beans.Component component = dataBuilder.getChart();
                        if (component == null) {
                        } else {
                            m.setChartPath(component.getUrl());
                        }
                    } else {
                        m.setChartPath(templateStoreURL + name + "/" + mapOrChart.getKey());
                    }
                    m.setKey(mapOrChart.getKey());
                    charts.add(m);
                }
            }
        }
    }

    public StringBuffer generateComponentsJS(GIEWSDataBuilder dataBuilder) {
        ArrayList mapsJS = new ArrayList();
        ArrayList chartsJS = new ArrayList();
        StringBuffer jsCode = new StringBuffer("");
        com.giews.report.beans.Component map = dataBuilder.getMap();
        jsCode.append("{type:'map', name:'");
        jsCode.append("panel_map");
        jsCode.append("', xy:[");
        jsCode.append(String.valueOf(map.getCoordinateX()));
        jsCode.append(",");
        jsCode.append(String.valueOf(map.getCoordinateY()));
        jsCode.append("], sizes:[");
        jsCode.append(String.valueOf(map.getWidth()));
        jsCode.append(",");
        jsCode.append(String.valueOf(map.getHeight()));
        jsCode.append("], value:{src:\"");
        jsCode.append(map.getUrl());
        jsCode.append("\" ,caption:'");
        jsCode.append(map.getKey());
        jsCode.append("'}}");
        mapsJS.add(jsCode);
        jsCode = new StringBuffer("");
        com.giews.report.beans.Component chart = dataBuilder.getChart();
        jsCode.append("{type:'chart', name:'");
        jsCode.append("panel_chart");
        jsCode.append("', xy:[");
        jsCode.append(String.valueOf(chart.getCoordinateX()));
        jsCode.append(",");
        jsCode.append(String.valueOf(chart.getCoordinateY()));
        jsCode.append("], sizes:[");
        jsCode.append(String.valueOf(chart.getWidth()));
        jsCode.append(",");
        jsCode.append(String.valueOf(chart.getHeight()));
        jsCode.append("], value:{src:\"");
        jsCode.append(chart.getUrl());
        jsCode.append("\" ,caption:'");
        jsCode.append(chart.getKey());
        jsCode.append("'}}");
        chartsJS.add(jsCode);
        StringBuffer componentsJS = new StringBuffer("");
        if (mapsJS.size() > 0 || chartsJS.size() > 0) {
            componentsJS.append(",components:[");
            if (mapsJS.size() > 0) {
                if (",components:[".equals(componentsJS.toString())) {
                    componentsJS.append(mapsJS.get(0));
                } else {
                    componentsJS.append(",");
                    componentsJS.append(mapsJS.get(0));
                }
                for (int i = 1; i < mapsJS.size(); i++) {
                    componentsJS.append(",");
                    componentsJS.append(mapsJS.get(i));
                }
            }
            if (chartsJS.size() > 0) {
                if (",components:[".equals(componentsJS.toString())) {
                    componentsJS.append(chartsJS.get(0));
                } else {
                    componentsJS.append(",");
                    componentsJS.append(chartsJS.get(0));
                }
                for (int i = 1; i < chartsJS.size(); i++) {
                    componentsJS.append(",");
                    componentsJS.append(chartsJS.get(i));
                }
            }
            componentsJS.append("]");
        }
        return componentsJS;
    }

    public StringBuffer generateComponentsJS() {
        StringBuffer jsCode = new StringBuffer("");
        ArrayList mapsJS = new ArrayList();
        ArrayList textsJS = new ArrayList();
        ArrayList chartsJS = new ArrayList();
        for (Iterator it = texts.iterator(); it.hasNext(); ) {
            jsCode = new StringBuffer("");
            Text text = (Text) it.next();
            jsCode.append("{type:'text', name:'");
            jsCode.append(text.getKey());
            jsCode.append("', xy:[");
            jsCode.append(String.valueOf(text.getX()));
            jsCode.append(",");
            jsCode.append(String.valueOf(text.getY()));
            jsCode.append("], sizes:[");
            jsCode.append(String.valueOf(text.getWidth()));
            jsCode.append(",");
            jsCode.append(String.valueOf(text.getHeight()));
            jsCode.append("], value:'");
            jsCode.append(StyledTextConvertor.reconvert(new StringBuffer(text.getText())));
            jsCode.append("'}");
            textsJS.add(jsCode);
        }
        for (Iterator it = maps.iterator(); it.hasNext(); ) {
            jsCode = new StringBuffer("");
            com.giews.report.component.Map map = (com.giews.report.component.Map) it.next();
            jsCode.append("{type:'map', name:'");
            jsCode.append(map.getKey());
            jsCode.append("', xy:[");
            jsCode.append(String.valueOf(map.getX()));
            jsCode.append(",");
            jsCode.append(String.valueOf(map.getY()));
            jsCode.append("], sizes:[");
            jsCode.append(String.valueOf(map.getWidth()));
            jsCode.append(",");
            jsCode.append(String.valueOf(map.getHeight()));
            jsCode.append("], value:{src:'");
            jsCode.append(map.getMapPath());
            jsCode.append("' ,caption:'");
            jsCode.append(map.getKey());
            jsCode.append("'}}");
            mapsJS.add(jsCode);
        }
        for (Iterator it = charts.iterator(); it.hasNext(); ) {
            jsCode = new StringBuffer("");
            com.giews.report.component.Chart chart = (com.giews.report.component.Chart) it.next();
            jsCode.append("{type:'chart', name:'");
            jsCode.append(chart.getKey());
            jsCode.append("', xy:[");
            jsCode.append(String.valueOf(chart.getX()));
            jsCode.append(",");
            jsCode.append(String.valueOf(chart.getY()));
            jsCode.append("], sizes:[");
            jsCode.append(String.valueOf(chart.getWidth()));
            jsCode.append(",");
            jsCode.append(String.valueOf(chart.getHeight()));
            jsCode.append("], value:{src:'");
            jsCode.append(chart.getChartPath());
            jsCode.append("' ,caption:'");
            jsCode.append(chart.getKey());
            jsCode.append("'}}");
            chartsJS.add(jsCode);
        }
        StringBuffer tableJs = new StringBuffer("");
        if (table != null) {
            tableJs.append("{type:'table', name:'panel_table', xy:[");
            tableJs.append(String.valueOf(table.getX()));
            tableJs.append(",");
            tableJs.append(String.valueOf(table.getY()));
            tableJs.append("], sizes:[");
            tableJs.append(String.valueOf(table.getWidth()));
            tableJs.append(",");
            tableJs.append(String.valueOf(table.getHeight()));
            tableJs.append("], value:'table/tablewizardview.jsp'}");
        }
        StringBuffer componentsJS = new StringBuffer("");
        if (textsJS.size() > 0 || mapsJS.size() > 0 || chartsJS.size() > 0 || tableJs.length() > 0) {
            componentsJS.append(",components:[");
            if (textsJS.size() > 0) {
                componentsJS.append(textsJS.get(0));
                for (int i = 1; i < textsJS.size(); i++) {
                    componentsJS.append(",");
                    componentsJS.append(textsJS.get(i));
                }
            }
            if (mapsJS.size() > 0) {
                if (",components:[".equals(componentsJS.toString())) {
                    componentsJS.append(mapsJS.get(0));
                } else {
                    componentsJS.append(",");
                    componentsJS.append(mapsJS.get(0));
                }
                for (int i = 1; i < mapsJS.size(); i++) {
                    componentsJS.append(",");
                    componentsJS.append(mapsJS.get(i));
                }
            }
            if (chartsJS.size() > 0) {
                if (",components:[".equals(componentsJS.toString())) {
                    componentsJS.append(chartsJS.get(0));
                } else {
                    componentsJS.append(",");
                    componentsJS.append(chartsJS.get(0));
                }
                for (int i = 1; i < chartsJS.size(); i++) {
                    componentsJS.append(",");
                    componentsJS.append(chartsJS.get(i));
                }
            }
            if (tableJs.length() > 0) {
                if (",components:[".equals(componentsJS.toString())) {
                    componentsJS.append(tableJs);
                } else {
                    componentsJS.append(",");
                    componentsJS.append(tableJs);
                }
            }
            componentsJS.append("]");
        }
        return componentsJS;
    }

    private void copyFiles() throws IOException {
        for (Iterator it = filesToCopy.iterator(); it.hasNext(); ) {
            String[] files = (String[]) it.next();
            if (files[0].startsWith("file:///")) {
                files[0] = files[0].substring("file:///".length());
            }
            URL url = new URL(files[0]);
            URLConnection urlConnection = url.openConnection();
            if (urlConnection != null) {
                InputStream reader = urlConnection.getInputStream();
                FileOutputStream fs = new FileOutputStream(files[1]);
                try {
                    byte[] cbuf = new byte[4096];
                    int cnt = 0;
                    while ((cnt = reader.read(cbuf)) != -1) fs.write(cbuf, 0, cnt);
                } finally {
                    reader.close();
                    fs.close();
                }
            }
        }
    }

    public TableObject getTableObject() {
        return tableObject;
    }

    public void setTableObject(TableObject tableObject) {
        this.tableObject = tableObject;
    }

    public void setColumnes(String[] columnes) {
        this.columnes = columnes;
    }
}
