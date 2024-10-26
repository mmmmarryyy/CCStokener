package edu.ucsd.ncmir.WIBAnnotation;

import edu.ucsd.ncmir.spl.minixml.Document;
import edu.ucsd.ncmir.spl.minixml.Element;
import edu.ucsd.ncmir.spl.minixml.SAXBuilder;
import java.net.URL;

/**
 *
 * @author spl
 */
public class NIFHandler implements OntologyHandler {

    private static final String URL_BASE = "http://nif-services.neuinfo.org/ontoquest/concepts/term/";

    @Override
    public String getURL(String name) {
        String r;
        try {
            URL url = new URL(NIFHandler.URL_BASE + name.replaceAll(" ", "+"));
            Document d = new SAXBuilder().build(url.openStream());
            Element e = d.getRootElement().descendTo("url");
            r = e.getText().trim();
        } catch (Throwable t) {
            r = "unknown";
        }
        return r;
    }
}
