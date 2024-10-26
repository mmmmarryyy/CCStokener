package org.tockit.docco.documenthandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import org.apache.poi.POIOLE2TextExtractor;
import org.apache.poi.hpsf.SummaryInformation;
import org.tockit.docco.indexer.DocumentSummary;

public abstract class POIExtractorDocumentHandler {

    public POIExtractorDocumentHandler() {
        super();
    }

    protected abstract POIOLE2TextExtractor createExtractor(InputStream inputStream) throws IOException;

    public DocumentSummary parseDocument(URL url) throws IOException, DocumentHandlerException {
        InputStream inputStream = null;
        try {
            inputStream = url.openStream();
            POIOLE2TextExtractor extractor = createExtractor(inputStream);
            SummaryInformation info = extractor.getSummaryInformation();
            DocumentSummary docSummary = new DocumentSummary();
            docSummary.authors = DocSummaryPOIFSReaderListener.getAuthors(info);
            docSummary.contentReader = new StringReader(extractor.getText());
            docSummary.creationDate = info.getCreateDateTime();
            docSummary.keywords = new ArrayList();
            docSummary.keywords.add(info.getKeywords());
            docSummary.modificationDate = new Date(info.getEditTime());
            docSummary.title = info.getTitle();
            return docSummary;
        } catch (IOException e) {
            if (e.getMessage().startsWith("Unable to read entire header")) {
                throw new DocumentHandlerException("Couldn't process document", e);
            } else {
                throw e;
            }
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }
}
