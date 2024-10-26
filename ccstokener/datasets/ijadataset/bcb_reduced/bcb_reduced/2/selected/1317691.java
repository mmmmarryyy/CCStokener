package tico.updateWizard;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.zip.Inflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import tico.configuration.TSetup;

public class TUpdateTask {

    JTextArea details;

    String urlString;

    public TUpdateTask(JTextArea details, String urlString) {
        this.details = details;
        this.urlString = urlString;
    }

    public int doTask() {
        try {
            URL url = new URL(urlString);
            URLConnection conn = url.openConnection();
            BufferedInputStream bin = new BufferedInputStream(conn.getInputStream());
            BufferedOutputStream bout = new BufferedOutputStream(new FileOutputStream("new.zip"));
            int i;
            details.append("Downloading file from: " + urlString + " ");
            while ((i = bin.read()) != -1) {
                details.append(".");
                bout.write(i);
            }
            bin.close();
            bout.close();
            details.append("DONE\n");
            ZipFile zipFile = new ZipFile("new.zip");
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                if (entry.isDirectory()) {
                    details.append("Extracting directory: " + entry.getName() + "\n");
                    (new File(entry.getName())).mkdir();
                    continue;
                }
                details.append("Extracting file: " + entry.getName() + "\n");
                copyInputStream(zipFile.getInputStream(entry), new BufferedOutputStream(new FileOutputStream(entry.getName())));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        details.append("FINISHED!!");
        return 0;
    }

    public static final void copyInputStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int len;
        while ((len = in.read(buffer)) >= 0) out.write(buffer, 0, len);
        in.close();
        out.close();
    }
}
