package newgen.presentation.cataloguing;

import javax.naming.*;

/**
 *
 * @author  administrator
 */
public class UploadCataloguePanel extends javax.swing.JPanel {

    private static UploadCataloguePanel instance = null;

    private newgen.presentation.component.ServletConnector servletConnector = null;

    public static UploadCataloguePanel getInstance() {
        instance = new UploadCataloguePanel();
        return instance;
    }

    /** Creates new form UploadCataloguePanel */
    public UploadCataloguePanel() {
        initComponents();
    }

    public void getDetails() {
        String fr = "";
        try {
            boolean resExists = true;
            String resT = null;
            String folPath = "";
            int o = 0;
            org.jdom.Element op = new org.jdom.Element("OperationId");
            op.setAttribute("no", "34");
            org.jdom.output.XMLOutputter out = new org.jdom.output.XMLOutputter();
            String xml34 = out.outputString(op);
            System.out.println("xml is" + xml34);
            String samp = newgen.presentation.component.ServletConnector.getInstance().sendRequest("NewGenServlet", xml34);
            System.out.println("xml from servlet is" + samp);
            String from = "";
            org.jdom.input.SAXBuilder sab1 = new org.jdom.input.SAXBuilder();
            org.jdom.Document doc1 = sab1.build(new java.io.StringReader(samp));
            String un = doc1.getRootElement().getText();
            java.util.Calendar cal = java.util.Calendar.getInstance();
            java.util.Calendar cal1 = java.util.Calendar.getInstance();
            java.util.StringTokenizer st = new java.util.StringTokenizer(un, "-");
            String t[] = new String[3];
            for (int i = 0; st.hasMoreElements(); i++) {
                t[i] = st.nextToken();
                System.out.println("token r" + t[i]);
            }
            Integer y = new Integer(t[0]);
            Integer m = new Integer(t[1]);
            Integer dt = new Integer(t[2]);
            cal1.set(y.intValue(), m.intValue() - 1, dt.intValue());
            if (cal1.get(cal1.DATE) < cal.get(cal.DATE)) {
                dateField1.setText(cal1.get(cal1.YEAR) + "-" + (cal1.get(cal1.MONTH) + 1) + "-" + cal1.get(cal1.DATE) + 1);
            } else {
                dateField1.setText(cal1.get(cal1.YEAR) + "-" + (cal1.get(cal1.MONTH) + 1) + "-" + cal1.get(cal1.DATE));
            }
            dateField2.setText(cal.get(cal.YEAR) + "-" + (cal.get(cal.MONTH) + 1) + "-" + cal.get(cal.DATE));
            fr = dateField1.getText();
            String unt = dateField2.getText();
            String urlstr = "http://203.197.20.2:8080/newgenlibctxt/oai2.0?verb=ListRecords&metadataPrefix=marc21&from=" + fr + "&until=" + unt;
            String path = "C:" + java.io.File.separator + "NewGenLibFiles" + java.io.File.separator + "OAIPMH_RECORDS" + java.io.File.separator;
            String path1 = path;
            String path2 = path;
            java.io.File file = new java.io.File(path);
            if (file.isDirectory() == false) {
                file.mkdirs();
            }
            for (int i = 0; resExists == false; i++) {
                try {
                    path = "C:" + java.io.File.separator + "NewGenLibFiles" + java.io.File.separator + "OAIPMH_RECORDS" + java.io.File.separator;
                    java.net.URL url = new java.net.URL(urlstr);
                    java.io.InputStream is = url.openStream();
                    java.io.InputStreamReader isr = new java.io.InputStreamReader(is);
                    StringBuffer a = new StringBuffer(10);
                    java.io.File f = new java.io.File("c:" + java.io.File.separator + "NewGenLibFiles" + java.io.File.separator + "OAI_REC" + java.io.File.separator + resT + ".xml");
                    int ch;
                    while ((ch = is.read()) != -1) {
                        a.append((char) ch);
                    }
                    path = path + resT + ".xml";
                    java.io.FileWriter w = new java.io.FileWriter(path);
                    String cont = a.substring(0);
                    w.write(cont);
                    w.close();
                    is.close();
                    org.jdom.input.SAXBuilder sb = new org.jdom.input.SAXBuilder();
                    org.jdom.Document doc = sb.build(path);
                    try {
                        resT = doc.getRootElement().getChild("ListRecords", doc.getRootElement().getNamespace()).getChildText("resumptionToken", doc.getRootElement().getNamespace());
                    } catch (Exception e) {
                    }
                    System.out.println("res is" + resT);
                    if (!(resT == null)) {
                        urlstr = "http://203.197.20.2:8080/newgenlibctxt/oai2.0?verb=ListRecords&from=" + fr + "&resumptionToken=" + resT + "&until=" + unt;
                    } else {
                        resExists = false;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            path2 = path2 + "library.xml";
            java.io.FileWriter fw = new java.io.FileWriter(path2);
            org.jdom.Element libName = new org.jdom.Element("LibraryName");
            libName.setText(newgen.presentation.NewGenMain.getAppletInstance().getLibraryName(newgen.presentation.NewGenMain.getAppletInstance().getLibraryID()));
            org.jdom.output.XMLOutputter outl = new org.jdom.output.XMLOutputter();
            fw.write(outl.outputString(libName));
            fw.flush();
            System.out.println("lib xml" + outl.outputString(libName));
            unicodeTextField2.setText(zipCompress(path1));
            org.jdom.Element op1 = new org.jdom.Element("OperationId");
            op1.setAttribute("no", "35");
            op1.setAttribute("id", doc1.getRootElement().getAttributeValue("id"));
            op1.setText(fr);
            org.jdom.output.XMLOutputter out1 = new org.jdom.output.XMLOutputter();
            String xml35 = out1.outputString(op1);
            String success = newgen.presentation.component.ServletConnector.getInstance().sendRequest("NewGenServlet", xml35);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String zipCompress(String path) {
        String pathJar = "";
        try {
            java.io.BufferedInputStream origin = null;
            java.io.File fCom = null;
            pathJar = "C:" + java.io.File.separator + "NewGenLibFiles" + java.io.File.separator + "COMPRESSED_OAIPMH" + java.io.File.separator;
            fCom = new java.io.File(pathJar);
            if (fCom.isDirectory() == false) {
                fCom.mkdirs();
                System.out.println("inside 1111");
            }
            pathJar = pathJar + "marc.jar";
            java.io.FileOutputStream dest1 = new java.io.FileOutputStream(pathJar);
            StringBuffer sbuf = new StringBuffer();
            sbuf.append("Class-Path :" + "SDA" + newgen.presentation.NewGenMain.getAppletInstance().getLibraryName(newgen.presentation.NewGenMain.getAppletInstance().getLibraryID()));
            java.io.InputStream is = new java.io.ByteArrayInputStream(sbuf.toString().getBytes("UTF-8"));
            java.util.jar.Manifest man = new java.util.jar.Manifest(is);
            java.util.jar.JarOutputStream out = new java.util.jar.JarOutputStream(new java.io.BufferedOutputStream(dest1), man);
            java.io.File f = new java.io.File(path);
            String files[] = f.list();
            System.out.println("files r" + files.length);
            for (int i = 0; i < files.length; i++) {
                java.io.FileInputStream fis = new java.io.FileInputStream(path + files[i]);
                java.io.BufferedInputStream bin = new java.io.BufferedInputStream(fis, 1024);
                java.util.jar.JarEntry entryx = new java.util.jar.JarEntry(files[i]);
                out.putNextEntry(entryx);
                byte data[] = new byte[1024];
                int count;
                while ((count = bin.read(data, 0, 1024)) != -1) {
                    out.write(data, 0, count);
                }
                bin.close();
            }
            out.close();
            for (int g = 0; g < files.length; g++) {
                java.io.File f1 = new java.io.File(path + files[g]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return pathJar;
    }

    public void reloadLocales() {
        jLabel1.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("HostlibraryServerName"));
        jLabel2.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("From"));
        jLabel3.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("Until"));
        jLabel4.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("FileLocationatLocalDrive"));
        jButton1.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("Upload"));
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        unicodeTextField1 = new newgen.presentation.UnicodeTextField();
        jLabel2 = new javax.swing.JLabel();
        dateField1 = new newgen.presentation.component.DateField();
        jLabel3 = new javax.swing.JLabel();
        dateField2 = new newgen.presentation.component.DateField();
        jLabel4 = new javax.swing.JLabel();
        unicodeTextField2 = new newgen.presentation.UnicodeTextField();
        jButton1 = new javax.swing.JButton();
        setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS));
        jPanel1.setLayout(new java.awt.GridBagLayout());
        jLabel1.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("HostlibraryServerName"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        jPanel1.add(jLabel1, gridBagConstraints);
        unicodeTextField1.setColumns(25);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        jPanel1.add(unicodeTextField1, gridBagConstraints);
        jLabel2.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("From"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        jPanel1.add(jLabel2, gridBagConstraints);
        dateField1.setText("");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        jPanel1.add(dateField1, gridBagConstraints);
        jLabel3.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("Until"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        jPanel1.add(jLabel3, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        jPanel1.add(dateField2, gridBagConstraints);
        jLabel4.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("FileLocationatLocalDrive"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        jPanel1.add(jLabel4, gridBagConstraints);
        unicodeTextField2.setColumns(25);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        jPanel1.add(unicodeTextField2, gridBagConstraints);
        jButton1.setText(newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("Upload"));
        jButton1.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 7;
        jPanel1.add(jButton1, gridBagConstraints);
        add(jPanel1);
    }

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            Object[] object = new Object[3];
            object[0] = "UPLOAD";
            object[1] = "CatalogueRecords";
            java.util.Vector vector = new java.util.Vector();
            java.io.File zipFile = new java.io.File(unicodeTextField2.getText());
            System.out.println("path is" + zipFile.getAbsolutePath());
            System.out.println("file exists" + zipFile.exists());
            System.out.println("file length" + zipFile.length());
            java.nio.channels.FileChannel fileChannel = null;
            fileChannel = (new java.io.FileInputStream(zipFile)).getChannel();
            int fileLength = (int) fileChannel.size();
            System.out.println("fileLength : " + fileLength);
            java.nio.MappedByteBuffer mappedByteBuffer = fileChannel.map(java.nio.channels.FileChannel.MapMode.READ_ONLY, 0, fileLength);
            byte[] byteArray = new byte[mappedByteBuffer.capacity()];
            System.out.println(byteArray.length);
            System.out.println(mappedByteBuffer.hasArray());
            fileChannel.close();
            mappedByteBuffer.get(byteArray);
            vector.addElement(unicodeTextField1.getText());
            vector.addElement(byteArray);
            object[2] = vector;
            Object retObject = newgen.presentation.component.ServletConnector.getInstance().sendObjectRequestToSpecifiedServer(unicodeTextField1.getText(), "FileUploadDownloadServlet", object);
            vector = (java.util.Vector) retObject;
            System.out.println("Return value is: " + vector.elementAt(0));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private newgen.presentation.component.DateField dateField1;

    private newgen.presentation.component.DateField dateField2;

    private javax.swing.JButton jButton1;

    private javax.swing.JLabel jLabel1;

    private javax.swing.JLabel jLabel2;

    private javax.swing.JLabel jLabel3;

    private javax.swing.JLabel jLabel4;

    private javax.swing.JPanel jPanel1;

    private newgen.presentation.UnicodeTextField unicodeTextField1;

    private newgen.presentation.UnicodeTextField unicodeTextField2;
}
