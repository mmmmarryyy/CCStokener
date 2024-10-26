package newgen.presentation.component;

import java.util.zip.*;
import java.io.*;
import org.apache.commons.mail.*;

/**
 *
 * @author Administrator
 */
public class GenerateLogFile {

    /** Creates a new instance of GenerateLogFile */
    public static GenerateLogFile thisInstance = null;

    public GenerateLogFile() {
    }

    public static GenerateLogFile getInstance() {
        if (thisInstance == null) thisInstance = new GenerateLogFile();
        return thisInstance;
    }

    public boolean storeLogFile(String filePath) {
        try {
            String serverIp = java.util.prefs.Preferences.systemRoot().get("serverurl", "localhost");
            String portno = java.util.prefs.Preferences.systemRoot().get("portno", "8080");
            java.net.URL url = new java.net.URL("http://" + serverIp + ":" + portno + "/newgenlibctxt/LogFileHandler");
            System.out.println("http://" + serverIp + ":" + portno + "/newgenlibctxt/LogFileHandler");
            url.openConnection();
            java.io.InputStream ins = url.openStream();
            System.out.println("size = " + ins.available());
            int size = ins.available();
            BufferedOutputStream bos = null;
            BufferedInputStream bis = new BufferedInputStream(ins);
            ZipInputStream zis = new ZipInputStream(bis);
            ZipEntry entry;
            try {
                while ((entry = zis.getNextEntry()) != null) {
                    System.out.println("Extraction : " + entry);
                    int count;
                    byte data[] = new byte[2048];
                    FileOutputStream fos = new FileOutputStream("server.txt");
                    bos = new BufferedOutputStream(fos, 2048);
                    while ((count = zis.read(data, 0, 2048)) != -1) {
                        bos.write(data, 0, count);
                    }
                }
                bos.flush();
                bos.close();
            } catch (java.io.EOFException e) {
                System.out.println("unexpeted end of ZLIB input stream");
            }
            System.out.println("**************");
            ins.close();
            bis.close();
            zis.close();
            ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(filePath + "/server.zip")));
            byte data[] = new byte[2048];
            int count;
            entry = new ZipEntry("server.txt");
            zos.putNextEntry(entry);
            BufferedInputStream bis1 = new BufferedInputStream(new FileInputStream("server.txt"));
            while ((count = bis1.read(data, 0, 2048)) != -1) {
                zos.write(data, 0, count);
            }
            bis1.close();
            zos.closeEntry();
            zos.close();
            System.out.println("zip file created at + " + filePath + "/server.zip");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean sendLogThroughMail(String filePath) {
        try {
            String serverIp = newgen.presentation.NewGenMain.getAppletInstance().getCodeBase().getHost();
            java.net.URL url = new java.net.URL("http://" + serverIp + ":8080/newgenlibctxt/DBFileHandler");
            url.openConnection();
            java.io.InputStream ins = url.openStream();
            org.jdom.input.SAXBuilder saxBuild = new org.jdom.input.SAXBuilder();
            org.jdom.Document doc = saxBuild.build(ins);
            java.util.List l2 = doc.getRootElement().getChild("mbean").getChildren();
            String user = null;
            String password = null;
            String server = null;
            String fromEmail = null;
            for (int i = 0; i < l2.size(); i++) {
                org.jdom.Element e = (org.jdom.Element) l2.get(i);
                if (e.getName().equals("attribute")) {
                    if (e.getAttributeValue("name").equals("User")) {
                        user = e.getText();
                    }
                }
                if (e.getName().equals("attribute")) {
                    if (e.getAttributeValue("name").equals("Password")) {
                        password = e.getText();
                    }
                }
                java.util.List l3 = e.getChildren();
                if (l3.size() != 0) {
                    org.jdom.Element e1 = (org.jdom.Element) l3.get(0);
                    java.util.List l4 = e1.getChildren();
                    for (int j = 0; j < l4.size(); j++) {
                        org.jdom.Element e2 = (org.jdom.Element) l4.get(j);
                        if (e2.getAttributeValue("name").equals("mail.smtp.host")) server = e2.getAttributeValue("value");
                        if (e2.getAttributeValue("name").equals("mail.from")) fromEmail = e2.getAttributeValue("value");
                    }
                }
            }
            HtmlEmail htmlEmail = new HtmlEmail();
            htmlEmail.setAuthentication(user, password);
            htmlEmail.setHostName(server);
            htmlEmail.addTo("poorna@verussolutions.biz", "NewGenLib");
            htmlEmail.setFrom(fromEmail, "Library");
            htmlEmail.setSubject("Log File");
            EmailAttachment emailAttach = new EmailAttachment();
            System.out.println(filePath + "/server.zip");
            emailAttach.setPath(filePath + "/server.zip");
            String text = "Here is the zipped log file which is attached to this mail";
            htmlEmail.setTextMsg("<HTML><BODY>" + text + "<HTML><BODY>");
            try {
                htmlEmail.send();
                System.out.println("mail successfully sent");
                return true;
            } catch (org.apache.commons.mail.EmailException eme) {
                eme.printStackTrace();
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
