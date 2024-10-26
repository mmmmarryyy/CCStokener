package com.qspin.qtaste.ui.tools;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.util.StringTokenizer;
import javax.swing.text.BadLocationException;
import javax.swing.text.ChangedCharSetException;
import javax.swing.text.Document;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

/**
 *
 * @author vdubois
 */
public class HTMLDocumentLoader {

    public HTMLDocument loadDocument(HTMLDocument doc, URL url, String charSet) throws IOException {
        doc.putProperty(Document.StreamDescriptionProperty, url);
        InputStream in = null;
        boolean ignoreCharSet = false;
        for (; ; ) {
            try {
                doc.remove(0, doc.getLength());
                URLConnection urlc = url.openConnection();
                in = urlc.getInputStream();
                Reader reader = (charSet == null) ? new InputStreamReader(in) : new InputStreamReader(in, charSet);
                HTMLEditorKit.Parser parser = getParser();
                HTMLEditorKit.ParserCallback htmlReader = getParserCallback(doc);
                parser.parse(reader, htmlReader, ignoreCharSet);
                htmlReader.flush();
                break;
            } catch (BadLocationException ex) {
                throw new IOException(ex.getMessage());
            } catch (ChangedCharSetException e) {
                charSet = getNewCharSet(e);
                ignoreCharSet = true;
                in.close();
            }
        }
        return doc;
    }

    public HTMLDocument loadDocument(URL url, String charSet) throws IOException {
        return loadDocument((HTMLDocument) kit.createDefaultDocument(), url, charSet);
    }

    public HTMLDocument loadDocument(URL url) throws IOException {
        return loadDocument(url, null);
    }

    public synchronized HTMLEditorKit.Parser getParser() {
        if (parser == null) {
            try {
                Class<?> c = Class.forName("javax.swing.text.html.parser.ParserDelegator");
                parser = (HTMLEditorKit.Parser) c.newInstance();
            } catch (Throwable e) {
            }
        }
        return parser;
    }

    public synchronized HTMLEditorKit.ParserCallback getParserCallback(HTMLDocument doc) {
        return doc.getReader(0);
    }

    protected String getNewCharSet(ChangedCharSetException e) {
        String spec = e.getCharSetSpec();
        if (e.keyEqualsCharSet()) {
            return spec;
        }
        int index = spec.indexOf(";");
        if (index != -1) {
            spec = spec.substring(index + 1);
        }
        spec = spec.toLowerCase();
        StringTokenizer st = new StringTokenizer(spec, " \t=", true);
        boolean foundCharSet = false;
        boolean foundEquals = false;
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            if (token.equals(" ") || token.equals("\t")) {
                continue;
            }
            if (foundCharSet == false && foundEquals == false && token.equals("charset")) {
                foundCharSet = true;
                continue;
            } else if (foundEquals == false && token.equals("=")) {
                foundEquals = true;
                continue;
            } else if (foundEquals == true && foundCharSet == true) {
                return token;
            }
            foundCharSet = false;
            foundEquals = false;
        }
        return "8859_1";
    }

    protected static HTMLEditorKit kit;

    protected static HTMLEditorKit.Parser parser;

    static {
        kit = new HTMLEditorKit();
    }
}
