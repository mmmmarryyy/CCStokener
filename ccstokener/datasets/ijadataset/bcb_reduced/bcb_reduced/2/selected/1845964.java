package org.tockit.docco.documenthandler;

import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.rtf.RTFEditorKit;
import org.tockit.docco.filefilter.DoccoFileFilter;
import org.tockit.docco.filefilter.ExtensionFileFilterFactory;
import org.tockit.docco.gui.GuiMessages;
import org.tockit.docco.indexer.DocumentSummary;

public class RtfDocumentHandler implements DocumentHandler {

    public DocumentSummary parseDocument(URL url) throws IOException, DocumentHandlerException {
        RTFEditorKit kit = new RTFEditorKit();
        Document doc = kit.createDefaultDocument();
        try {
            kit.read(url.openStream(), doc, 0);
            String plainText = doc.getText(0, doc.getLength());
            DocumentSummary docSummary = new DocumentSummary();
            docSummary.contentReader = new StringReader(plainText);
            return docSummary;
        } catch (IOException e) {
            throw new DocumentHandlerException("Error reading RTF file", e);
        } catch (BadLocationException e) {
            throw new DocumentHandlerException("Internal error reading RTF file", e);
        }
    }

    public String getDisplayName() {
        return GuiMessages.getString("RtfDocumentHandler.name");
    }

    public DoccoFileFilter getDefaultFilter() {
        return new ExtensionFileFilterFactory().createNewFilter("rtf");
    }
}
