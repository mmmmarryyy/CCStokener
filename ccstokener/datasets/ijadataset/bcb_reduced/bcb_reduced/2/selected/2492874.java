package codekeeper;

import java.awt.Font;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.tree.DefaultTreeModel;
import org.fife.ui.rsyntaxtextarea.*;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

/**
 *
 * @author tattooedpierre
 *
 * Contains helper methods and constants.
 */
public class Constants {

    public static final String DefaultDelimeter = "#";

    public static final String DatabaseFilename = "CodeKeeperDB.xml";

    public static final String Newline = System.getProperty("line.separator");

    public enum SnippetTypes {

        CodeCSharp, CodeJavaScript, CodeJava, CodeVisualBasic, CodeDelphi, CodePascal, CodeCSS, CodeHTML, CodeXML, SQLMisc, TextMisc
    }

    public static ArrayList<SnippetCategory> DefaultCategoryList = new ArrayList<SnippetCategory>() {

        {
            add(new SnippetCategory("C#", Constants.SnippetTypes.CodeCSharp));
            add(new SnippetCategory("Java", Constants.SnippetTypes.CodeJava));
            add(new SnippetCategory("JavaScript", Constants.SnippetTypes.CodeJavaScript));
            add(new SnippetCategory("Delphi", Constants.SnippetTypes.CodeDelphi));
            add(new SnippetCategory("Visual Basic", Constants.SnippetTypes.CodeVisualBasic));
            add(new SnippetCategory("Pascal", Constants.SnippetTypes.CodePascal));
            add(new SnippetCategory("SQL", Constants.SnippetTypes.SQLMisc));
            add(new SnippetCategory("Misc", Constants.SnippetTypes.TextMisc));
            add(new SnippetCategory("CSS", Constants.SnippetTypes.CodeCSS));
            add(new SnippetCategory("HTML", Constants.SnippetTypes.CodeHTML));
            add(new SnippetCategory("XML", Constants.SnippetTypes.CodeXML));
        }
    };

    /**
     * Set the font for all token types.
     *
     * @param textArea The text area to modify.
     * @param font The font to use.
     */
    public static void setFont(RSyntaxTextArea textArea, Font font) {
        if (font != null) {
            SyntaxScheme ss = textArea.getSyntaxScheme();
            ss = (SyntaxScheme) ss.clone();
            for (int i = 0; i < ss.styles.length; i++) {
                if (ss.styles[i] != null) {
                    ss.styles[i].font = font;
                }
            }
            textArea.setSyntaxScheme(ss);
            textArea.setFont(font);
        }
    }

    public static void DownloadFromUrl(String url, String path) {
        File outputFile = new File("CodeKeeperDB.xml");
        java.io.BufferedInputStream in = null;
        try {
            if (outputFile.exists()) {
                outputFile.delete();
            }
            in = new java.io.BufferedInputStream(new URL(url + path).openStream());
            FileOutputStream fos = new FileOutputStream(outputFile.toString());
            BufferedOutputStream bout = new BufferedOutputStream(fos, 1024);
            byte[] data = new byte[1024];
            while (in.read(data, 0, 1024) >= 0) {
                bout.write(data);
            }
            bout.close();
            in.close();
        } catch (IOException ex) {
            Logger.getLogger(CodeKeeperView.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void UploadToUrl(String url, String path) {
        try {
            String xmldata = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
            String line;
            String hostname = url.replace("http://www.", "");
            int port = 80;
            InetAddress addr = InetAddress.getByName(hostname);
            Socket sock = new Socket(addr, port);
            BufferedWriter wr = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream(), "UTF-8"));
            wr.write("POST " + path + " HTTP/1.0\r\n");
            wr.write("Host: " + url + "\r\n");
            wr.write("Content-Length: " + xmldata.length() + "\r\n");
            wr.write("Content-Type: text/xml; charset=\"utf-8\"\r\n");
            wr.write("\r\n");
            BufferedReader rd = new BufferedReader(new FileReader("output.xml"));
            while ((line = rd.readLine()) != null) wr.write(line);
            wr.flush();
            rd = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            while ((line = rd.readLine()) != null) System.out.println(line);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static DefaultTreeModel ReadXml(String filename) {
        XMLDecoder decoder = null;
        try {
            decoder = new XMLDecoder(new BufferedInputStream(new FileInputStream(filename)));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        DefaultTreeModel model = (DefaultTreeModel) decoder.readObject();
        decoder.close();
        return model;
    }

    public static String SaveObjectToXml(Object o, String path) {
        File f = new File(path);
        String success = "CodeKeeper database saved OK.";
        try {
            if (f.exists()) f.delete();
            FileOutputStream fstream = new FileOutputStream(path);
            try {
                XMLEncoder ostream = new XMLEncoder(fstream);
                try {
                    ostream.writeObject(o);
                    ostream.flush();
                } finally {
                    ostream.close();
                }
            } finally {
                fstream.close();
            }
        } catch (Exception ex) {
            success = ex.getLocalizedMessage();
        }
        return success;
    }
}
