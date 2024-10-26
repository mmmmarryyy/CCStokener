package net.sf.ngrease.core.ast;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class ElementSourceUrlImpl implements ElementSource {

    private final URL url;

    private final ElementParser elementParser;

    public ElementSourceUrlImpl(URL url, ElementParser elementParser) {
        this.url = url;
        this.elementParser = elementParser;
    }

    public Element getElement() {
        try {
            InputStream stream = url.openStream();
            Element element = elementParser.parse(stream);
            return element;
        } catch (IOException e) {
            throw new NgreaseException(e);
        }
    }

    public URL getUrl() {
        return url;
    }

    public ElementParser getElementParser() {
        return elementParser;
    }

    public String toString() {
        return "" + url;
    }
}
