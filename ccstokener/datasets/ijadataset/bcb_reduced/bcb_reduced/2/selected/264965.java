package wabclient;

import javax.swing.*;
import java.awt.image.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import org.mozilla.javascript.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import javax.xml.parsers.*;

public class AppHandler extends ParserHandler {

    public AppHandler(WABClient client, String url) {
        super(client, url);
    }

    public void startElement(String uri, String tag, String qName, org.xml.sax.Attributes attributes) throws SAXException {
        wabclient.Attributes prop = new wabclient.Attributes(attributes);
        try {
            if (tag.equals("app")) {
                if (prop == null) {
                    System.err.println("app without properties");
                    return;
                }
                String appname = prop.getValue("name", "");
                String lookandfeel = prop.getValue("lookandfeel", "");
                global.setAppName(appname);
                if (lookandfeel.length() > 0) {
                    if (lookandfeel.equalsIgnoreCase("Windows")) lookandfeel = "com.sun.java.swing.plaf.windows.WindowsLookAndFeel"; else if (lookandfeel.equalsIgnoreCase("Motif")) lookandfeel = "com.sun.java.swing.plaf.motif.MotifLookAndFeel"; else if (lookandfeel.equalsIgnoreCase("Mac")) lookandfeel = "com.sun.java.swing.plaf.mac.MacLookAndFeel";
                    UIManager.setLookAndFeel(lookandfeel);
                }
            } else if (tag.equals("script")) {
                WABClient c = (WABClient) global;
                c.beginScript();
                String url = prop.getValue("src");
                if (url.length() > 0) {
                    try {
                        BufferedReader r = new BufferedReader(new InputStreamReader(new URL(url).openStream()));
                        String buffer;
                        while (true) {
                            buffer = r.readLine();
                            if (buffer == null) break;
                            c.script += buffer + "\n";
                        }
                        r.close();
                        c.endScript();
                    } catch (IOException ioe) {
                        System.err.println("[IOError] " + ioe.getMessage());
                        System.exit(0);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    public void endElement(String uri, String tag, String qName) throws SAXException {
        if (tag.equals("script")) {
            WABClient c = (WABClient) global;
            c.endScript();
        } else if (tag.equals("app")) {
            WABClient c = (WABClient) global;
            c.initContext();
            c.fireEvent("onload");
            if (global.windowCount == 0) System.exit(0);
        }
    }

    public void characters(char[] ch, int start, int length) throws SAXException {
        WABClient c = (WABClient) global;
        c.characters(new String(ch, start, length));
    }

    public void ioexception(Exception ex) {
        JOptionPane.showMessageDialog(null, "The required server does not respond.\n" + ex.getMessage(), global.appname, JOptionPane.ERROR_MESSAGE);
        System.exit(1);
    }
}
