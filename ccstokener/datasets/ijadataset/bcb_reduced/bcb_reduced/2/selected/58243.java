package com.ibm.hamlet;

import java.io.*;
import java.net.*;
import org.xml.sax.*;

public class HamletHandler implements ContentHandler {

    private Hamlet hamlet;

    public HamletHandler(Hamlet hamlet) {
        this.hamlet = hamlet;
    }

    public int getElementRepeatCount(String id, String name, Attributes atts) throws Exception {
        return 0;
    }

    public String getElementReplacement(String id, String name, Attributes atts) throws Exception {
        return "";
    }

    public Attributes getElementAttributes(String id, String name, Attributes atts) throws Exception {
        return atts;
    }

    public InputStream getElementIncludeSource(String id, String name, Attributes atts) throws Exception {
        URL url = hamlet.getIncludeURL(atts.getValue("SRC"));
        return url.openStream();
    }
}
