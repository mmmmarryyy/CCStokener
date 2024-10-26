package org.jdas.reader;

import java.io.IOException;
import java.net.URL;
import org.dom4j.Document;
import org.dom4j.io.DOMReader;
import org.jdas.model.DOMDocument;
import org.jdas.model.GenericDocument;
import org.jdas.model.dom.DOMDocumentDom4jImpl;
import org.jdas.reader.exception.DocumentReaderException;
import org.w3c.tidy.Configuration;
import org.w3c.tidy.Tidy;

class HTMLDocumentReaderDom4jImpl extends HTMLDocumentReader<DOMDocument<Document>> {

    private static GenericDocumentReader<? extends GenericDocument> domReader = null;

    private HTMLDocumentReaderDom4jImpl() {
        super();
    }

    static GenericDocumentReader<? extends GenericDocument> getInstance() {
        if (domReader == null) {
            domReader = new HTMLDocumentReaderDom4jImpl();
        }
        return domReader;
    }

    @Override
    public DOMDocument<Document> getDocument(URL url) throws DocumentReaderException {
        Tidy tidy = new Tidy();
        setupTidy(tidy);
        try {
            org.w3c.dom.Document document = tidy.parseDOM(url.openStream(), null);
            if (tidy.getParseErrors() > 0) {
                throw new DocumentReaderException("JDAS invalid XML: error while parsing");
            }
            return new DOMDocumentDom4jImpl(new DOMReader().read(document));
        } catch (IOException ioEx) {
            throw new DocumentReaderException(ioEx.getCause());
        }
    }

    private void setupTidy(Tidy tidy) {
        tidy.setXHTML(false);
        tidy.setCharEncoding(Configuration.UTF8);
        tidy.setShowWarnings(false);
        tidy.setQuiet(true);
    }
}
