package org.jdas.reader;

import java.io.IOException;
import java.net.URL;
import org.jdas.model.GenericDocument;
import org.jdas.model.implementation.Dom4j;
import org.jdas.model.implementation.Implementation;

abstract class GenericDocumentReaderFactory<D extends GenericDocument, R extends GenericDocumentReader<D>> {

    private static Class<? extends Implementation> implementation = Dom4j.class;

    static GenericDocumentReader<? extends GenericDocument> getReader(URL url) throws IOException {
        if ("text/xml".equals(url.openConnection().getContentType()) || "application/xml".equals(url.openConnection().getContentType())) {
            return DOMDocumentReaderFactory.getReader(implementation);
        } else if ("text/html".equals(url.openConnection().getContentType())) {
            return HTMLDocumentReaderFactory.getReader(implementation);
        }
        return null;
    }

    static <I extends Implementation> void setImplementation(Class<I> implementation) {
        GenericDocumentReaderFactory.implementation = implementation;
    }

    static Class<? extends Implementation> getImplementation() {
        return implementation;
    }
}
