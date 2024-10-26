package net.allblog.testbed;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.URL;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.w3c.tidy.Configuration;
import org.w3c.tidy.Tidy;

public class TidyParser {

    InputStream is;

    Tidy tidy;

    Element rawDoc;

    public TidyParser(String url) throws IOException {
        this(new URL(url));
    }

    public TidyParser(URL url) throws IOException {
        this.is = url.openStream();
        this.tidy = new Tidy();
        init();
    }

    private void init() {
        this.tidy.setCharEncoding(Configuration.UTF8);
        this.tidy.setQuiet(true);
        this.tidy.setShowWarnings(false);
        org.w3c.dom.Document root = this.tidy.parseDOM(this.is, null);
        this.rawDoc = root.getDocumentElement();
    }

    public String getBody() {
        if (this.rawDoc == null) {
            return null;
        }
        String body = "";
        NodeList children = this.rawDoc.getElementsByTagName("body");
        if (children.getLength() > 0) {
            body = getText(children.item(0));
        }
        return body;
    }

    public String getFull() throws IOException {
        byte[] buffer = new byte[4096];
        OutputStream outputStream = new ByteArrayOutputStream();
        while (true) {
            int read = this.is.read(buffer);
            if (read == -1) {
                break;
            }
            outputStream.write(buffer, 0, read);
        }
        outputStream.close();
        String s = outputStream.toString();
        return s;
    }

    public String getText(Node node) {
        NodeList children = node.getChildNodes();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            switch(child.getNodeType()) {
                case Node.ELEMENT_NODE:
                    sb.append(getText(child));
                    sb.append(" ");
                    break;
                case Node.TEXT_NODE:
                    sb.append(((Text) child).getData());
                    break;
            }
        }
        return sb.toString();
    }

    public static void main(String[] args) throws IOException {
        TidyParser tp = new TidyParser(args[0]);
        System.out.println(tp.getBody());
    }
}
