package com.medcentrex.bridge.interfaces;

import java.io.InputStream;
import java.net.URL;
import org.pdfbox.pdmodel.PDDocument;

public class HCFA1500Util {

    public static PDDocument newDocument() throws Exception {
        return new PDDocument();
    }

    public static PDDocument newDocument(InputStream is) throws Exception {
        return PDDocument.load(is);
    }

    public static PDDocument newDocument(URL url) throws Exception {
        return newDocument(url.openStream());
    }
}
