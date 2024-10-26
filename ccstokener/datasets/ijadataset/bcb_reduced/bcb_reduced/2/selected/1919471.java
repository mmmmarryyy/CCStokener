package com.pallas.unicore.connection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;

/**
 * Class reads file contents as a String from an URL. If a proxy server is used
 * it may ask for authorization with a dialog.
 * 
 * @author Ralf Ratering
 * @version $Id: URLReader.java,v 1.1 2004/05/25 14:58:51 rmenday Exp $
 */
public class URLReader {

    private static Logger logger = Logger.getLogger("com.pallas.unicore.connection");

    public static void main(String args[]) {
        try {
            URLReader reader = new URLReader();
            ProxyManager.useProxy("webcheck.pallas.com", 8080, "");
            System.err.println(">>>>>>>>>>>>>>>>>>>>>>HTML-PAGE:");
            URL url = new URL("http://www.unicorepro.com/unicoreSites.xml");
            logger.info(reader.readURL(url));
        } catch (Exception e) {
            logger.log(Level.WARNING, "Could not read URL.", e);
        }
    }

    public URLReader() {
        this(null);
    }

    public URLReader(JFrame parent) {
        ProxyManager.setParent(parent);
    }

    public String readURL(URL url) throws IOException {
        URLConnection uc = url.openConnection();
        InputStream content = (InputStream) uc.getInputStream();
        BufferedReader in = new BufferedReader(new InputStreamReader(content));
        String line;
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        while ((line = in.readLine()) != null) {
            pw.println(line);
        }
        return sw.toString();
    }
}
