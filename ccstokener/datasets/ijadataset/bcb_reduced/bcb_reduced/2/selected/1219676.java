package org.swingerproject.xml.css;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Locale;
import org.w3c.css.sac.CSSException;
import org.w3c.css.sac.DocumentHandler;
import org.w3c.css.sac.ErrorHandler;
import org.w3c.css.sac.SelectorFactory;

public class CSSParser {

    private DocumentHandler handler;

    public String getParserVersion() {
        return "0.1";
    }

    public void parseStyleSheet(InputStream arg0) throws CSSException, IOException {
    }

    public void parseStyleSheet(File arg0) throws CSSException, IOException {
        parseStyleSheet(new FileInputStream(arg0));
    }

    public void parseStyleSheet(URL url) throws CSSException, IOException {
        parseStyleSheet(url.openStream());
    }

    public void setDocumentHandler(DocumentHandler arg0) {
        handler = arg0;
    }

    public void setErrorHandler(ErrorHandler arg0) {
    }

    public void setSelectorFactory(SelectorFactory arg0) {
    }
}
