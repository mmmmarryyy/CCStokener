package com.scholardesk.xml;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;
import org.w3c.dom.Document;
import org.w3c.tidy.Tidy;

public class XHTMLUtilities {

    public static Document getXHTMLDocument(URL _url) throws IOException {
        final Tidy tidy = new Tidy();
        tidy.setQuiet(true);
        tidy.setShowWarnings(false);
        tidy.setXmlOut(true);
        final BufferedInputStream input_stream = new BufferedInputStream(_url.openStream());
        return tidy.parseDOM(input_stream, null);
    }
}
