package org.dengues.test.components;

import java.io.IOException;

/**
 * Qiang.Zhang.Adolf@gmail.com class global comment. Detailled comment <br/>
 * 
 * $Id: Dengues.epf 1 2006-09-29 17:06:40Z qiang.zhang $
 * 
 */
public class LibrariesTestAll {

    /**
     * Comment method "main".
     * 
     * @param args
     */
    public static void main(String[] args) throws Exception {
        testMail();
    }

    /**
     * Qiang.Zhang.Adolf@gmail.com Comment method "testMail".
     */
    private static void testMail() {
        String host = "smtp.126.com";
        String from = "zhqi_3094@126.com";
        String to = "qiang.zhang.adolf@gmail.com";
        String username = "zhqi_3094";
        String password = "221231zhqi";
        java.util.Properties props = new java.util.Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.auth", "true");
        final javax.mail.PasswordAuthentication authentication = new javax.mail.PasswordAuthentication(username, password);
        javax.mail.Authenticator authenticator = new javax.mail.Authenticator() {

            @Override
            protected javax.mail.PasswordAuthentication getPasswordAuthentication() {
                return authentication;
            }
        };
        javax.mail.Session session = javax.mail.Session.getDefaultInstance(props);
        javax.mail.internet.MimeMessage message = new javax.mail.internet.MimeMessage(session);
        try {
            message.setFrom(new javax.mail.internet.InternetAddress(from));
            message.addRecipient(javax.mail.Message.RecipientType.TO, new javax.mail.internet.InternetAddress(to));
            message.setSubject("Hello JavaMail");
            javax.mail.internet.MimeBodyPart mbp1 = new javax.mail.internet.MimeBodyPart();
            mbp1.setText("Welcome to JavaMail");
            javax.mail.internet.MimeBodyPart mbp2 = new javax.mail.internet.MimeBodyPart();
            mbp2.attachFile("c:/cmd.txt");
            javax.mail.Multipart mp = new javax.mail.internet.MimeMultipart();
            mp.addBodyPart(mbp1);
            mp.addBodyPart(mbp2);
            message.setContent(mp);
            message.setSentDate(new java.util.Date());
            javax.mail.Transport transport = session.getTransport("smtp");
            transport.connect(host, username, password);
            transport.sendMessage(message, message.getAllRecipients());
            transport.close();
        } catch (javax.mail.internet.AddressException e) {
            e.printStackTrace();
        } catch (javax.mail.MessagingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Qiang.Zhang.Adolf@gmail.com Comment method "testXML".
     */
    private static void testXML() throws Exception {
        dengues.system.ComplexXPathUsedXalan xalan = new dengues.system.ComplexXPathUsedXalan("c:/xml_out.xml");
        org.w3c.dom.NodeList nodes = xalan.parseXPathNodes("/root/row");
        Object parseXPath = xalan.parseXPath("/root/row/price", javax.xml.xpath.XPathConstants.NUMBER);
        System.out.println(parseXPath);
        for (int i = 0; i < nodes.getLength(); i++) {
            org.w3c.dom.Node item = nodes.item(i);
            org.w3c.dom.NodeList childNodes = item.getChildNodes();
            for (int j = 0; j < childNodes.getLength(); j++) {
                org.w3c.dom.Node citem = childNodes.item(j);
            }
        }
    }

    private static void testFileList(String dirPath, String listMode, String fileMask, Boolean includeSubDir, Boolean caseSensitive, java.util.List<String> fileList) {
        java.io.File dir = new java.io.File(dirPath);
        if (dir.isDirectory()) {
            java.io.File[] listFiles = dir.listFiles(dengues.system.FileHelper.createFilenameFilter(fileMask, caseSensitive));
            for (java.io.File file : listFiles) {
                if (listMode.equals("Both")) {
                    fileList.add(file.getAbsolutePath());
                } else if (listMode.equals("Files")) {
                    if (file.isFile()) {
                        fileList.add(file.getAbsolutePath());
                    }
                } else if (listMode.equals("Directories")) {
                    if (file.isDirectory()) {
                        fileList.add(file.getAbsolutePath());
                    }
                }
                if (includeSubDir) {
                    testFileList(file.getAbsolutePath(), listMode, fileMask, includeSubDir, caseSensitive, fileList);
                }
            }
        }
    }

    /**
     * Qiang.Zhang.Adolf@gmail.com Comment method "testTidy".
     */
    private static void testTidy() {
        try {
            String url = "http://groups.google.com/group/dengues/files";
            java.io.InputStream is = new java.net.URL(url).openStream();
            org.w3c.dom.Document doc = dengues.system.HTMLWebHelper.parseDOM(is);
            org.w3c.dom.NodeList list = doc.getElementsByTagName("td");
            org.w3c.dom.Element stockTypeElement = null;
            for (int i = 0; i < list.getLength(); i++) {
                org.w3c.dom.Node item = list.item(i);
                String content = dengues.system.HTMLWebHelper.getContent(item);
                String convert = dengues.system.HTMLWebHelper.convert(content);
                if (convert.equals("zDevil")) {
                    stockTypeElement = (org.w3c.dom.Element) item.getParentNode().getParentNode();
                    break;
                }
            }
            if (stockTypeElement != null) {
                org.w3c.dom.NodeList trList = stockTypeElement.getElementsByTagName("tr");
                for (int i = 0; i < trList.getLength(); i++) {
                    org.w3c.dom.NodeList trListChildren = trList.item(i).getChildNodes();
                    if (trListChildren.getLength() > 2) {
                        org.w3c.dom.Node node_0 = trListChildren.item(0);
                        org.w3c.dom.Node node_1 = trListChildren.item(1);
                        String content = dengues.system.HTMLWebHelper.getContent(node_0);
                        String convert_0 = dengues.system.HTMLWebHelper.convert(content);
                        content = dengues.system.HTMLWebHelper.getContent(node_1);
                        String convert_1 = dengues.system.HTMLWebHelper.convert(content);
                        if (!"".equals(convert_0)) {
                            System.out.println(convert_0 + " => " + convert_1);
                        }
                    }
                }
            }
            is.close();
        } catch (java.net.MalformedURLException ex) {
            ex.printStackTrace();
        } catch (java.io.IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Qiang.Zhang.Adolf@gmail.com Comment method "testHSQLDB".
     */
    private static void testHSQLDB() {
        try {
            java.sql.PreparedStatement pstmt = null;
            pstmt.setBoolean(0, false);
            short x = 0;
            float f = 0;
            pstmt.setShort(0, x);
            pstmt.setObject(0, new Object());
            pstmt.setFloat(0, f);
            pstmt.setDouble(0, 1.0);
            pstmt.setInt(0, 1);
            pstmt.setBytes(0, new byte[0]);
            java.util.Date date = new java.util.Date();
            pstmt.setDate(0, new java.sql.Date(date.getTime()));
        } catch (java.sql.SQLException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Qiang.Zhang.Adolf@gmail.com Comment method "testJXL". look for
     * http://www.ibm.com/developerworks/cn/java/l-javaExcel/index.html.
     */
    private static void testJXL() {
        try {
            String sheetName = "new Sheet";
            String filename = "d:/myfile.xls";
            java.io.File file = new java.io.File(filename);
            java.io.File target = file;
            jxl.write.WritableWorkbook wwb;
            jxl.WorkbookSettings settings = new jxl.WorkbookSettings();
            settings.setEncoding("UTF-8");
            if (file.exists()) {
                String oldName = "old" + file.getName();
                target = new java.io.File(file.getAbsolutePath().replace(file.getName(), oldName));
                if (target.exists()) {
                    target.delete();
                }
                file.renameTo(target);
                jxl.Workbook workbook = jxl.Workbook.getWorkbook(target);
                wwb = jxl.Workbook.createWorkbook(file, workbook, settings);
            } else {
                wwb = jxl.Workbook.createWorkbook(file, settings);
            }
            String[] sheetNames = wwb.getSheetNames();
            jxl.write.WritableSheet ws;
            if (java.util.Arrays.asList(sheetNames).contains(sheetName)) {
                ws = wwb.createSheet(sheetName + "(1)", 0);
            } else {
                ws = wwb.createSheet(sheetName, 0);
            }
            jxl.write.Label labelC = new jxl.write.Label(0, 0, "This is a Label cell");
            ws.addCell(labelC);
            jxl.write.WritableFont wf = new jxl.write.WritableFont(jxl.write.WritableFont.TIMES, 18, jxl.write.WritableFont.BOLD, true);
            jxl.write.WritableCellFormat wcfF = new jxl.write.WritableCellFormat(wf);
            jxl.write.Label labelCF = new jxl.write.Label(1, 0, "This is a Label Cell", wcfF);
            ws.addCell(labelCF);
            jxl.write.WritableFont wfc = new jxl.write.WritableFont(jxl.write.WritableFont.ARIAL, 10, jxl.write.WritableFont.NO_BOLD, false, jxl.format.UnderlineStyle.NO_UNDERLINE, jxl.format.Colour.RED);
            jxl.write.WritableCellFormat wcfFC = new jxl.write.WritableCellFormat(wfc);
            jxl.write.Label labelCFC = new jxl.write.Label(1, 0, "This is a Label Cell", wcfFC);
            ws.addCell(labelCFC);
            jxl.write.Number labelN = new jxl.write.Number(0, 1, 3.1415926);
            ws.addCell(labelN);
            jxl.write.NumberFormat nf = new jxl.write.NumberFormat("#.##");
            jxl.write.WritableCellFormat wcfN = new jxl.write.WritableCellFormat(nf);
            jxl.write.Number labelNF = new jxl.write.Number(1, 1, 3.1415926, wcfN);
            ws.addCell(labelNF);
            jxl.write.Boolean labelB = new jxl.write.Boolean(0, 2, false);
            ws.addCell(labelB);
            jxl.write.DateTime labelDT = new jxl.write.DateTime(0, 3, new java.util.Date());
            ws.addCell(labelDT);
            jxl.write.DateFormat df = new jxl.write.DateFormat("dd MM yyyy hh:mm:ss");
            jxl.write.WritableCellFormat wcfDF = new jxl.write.WritableCellFormat(df);
            jxl.write.DateTime labelDTF = new jxl.write.DateTime(1, 3, new java.util.Date(), wcfDF);
            ws.addCell(labelDTF);
            wwb.write();
            wwb.close();
            filename = "d:/myfile2.xls";
            jxl.Workbook rwb = jxl.Workbook.getWorkbook(new java.io.File(filename), settings);
            jxl.Sheet sheet = rwb.getSheet(sheetName);
            if (sheet == null) {
                throw new RuntimeException(sheetName + " don't found in the workbook:" + file.getName());
            }
            int rows = sheet.getRows();
            int columns = sheet.getColumns();
            Integer.parseInt("1");
            for (int i = 0; i < rows; i++) {
                jxl.Cell[] row = sheet.getRow(i);
                for (jxl.Cell cell : row) {
                    boolean isLabel = cell.getType().equals(jxl.CellType.LABEL);
                    if (isLabel) {
                        break;
                    }
                    if (cell.getType().equals(jxl.CellType.DATE)) {
                        jxl.DateCell dateCell = (jxl.DateCell) cell;
                        dateCell.getDate();
                    }
                    System.out.println(cell.getContents());
                }
            }
            rwb.close();
        } catch (jxl.read.biff.BiffException ex) {
            ex.printStackTrace();
        } catch (java.io.IOException ex) {
            ex.printStackTrace();
        } catch (jxl.write.WriteException ex) {
            ex.printStackTrace();
        }
    }
}
