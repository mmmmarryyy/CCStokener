package org.tockit.docco.documenthandler;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import org.tockit.docco.filefilter.DoccoFileFilter;
import org.tockit.docco.filefilter.ExtensionFileFilterFactory;
import org.tockit.docco.gui.GuiMessages;
import org.tockit.docco.indexer.DocumentSummary;

public class PlainTextDocumentHandler implements DocumentHandler {

    public DocumentSummary parseDocument(URL url) throws IOException, DocumentHandlerException {
        Reader reader = new InputStreamReader(url.openStream());
        DocumentSummary docSummary = new DocumentSummary();
        docSummary.contentReader = reader;
        return docSummary;
    }

    public String getDisplayName() {
        return GuiMessages.getString("PlainTextDocumentHandler.name");
    }

    public DoccoFileFilter getDefaultFilter() {
        return new ExtensionFileFilterFactory().createNewFilter("txt;log");
    }
}
