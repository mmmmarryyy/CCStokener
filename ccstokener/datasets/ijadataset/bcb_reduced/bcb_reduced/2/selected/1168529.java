package org.jdas.transformer;

import java.io.IOException;
import java.net.URL;
import org.jdas.model.GenericDocument;
import org.jdas.model.implementation.Dom4j;
import org.jdas.model.implementation.Implementation;

abstract class GenericDocumentTransformerFactory<D extends GenericDocument, R extends GenericDocumentTransformer<D>> {

    private static Class<? extends Implementation> implementation;

    public static GenericDocumentTransformer<? extends GenericDocument> getTranformer(URL url) throws IOException {
        setDefaultImplementation();
        if ("text/xml".equals(url.openConnection().getContentType()) || "application/xml".equals(url.openConnection().getContentType())) {
            return null;
        } else if ("text/html".equals(url.openConnection().getContentType())) {
            return null;
        }
        return null;
    }

    public static <I extends Implementation> void setImplementation(Class<I> implementation) {
        GenericDocumentTransformerFactory.implementation = implementation;
    }

    private static void setDefaultImplementation() {
        if (implementation == null) {
            GenericDocumentTransformerFactory.setImplementation(Dom4j.class);
        }
    }
}
