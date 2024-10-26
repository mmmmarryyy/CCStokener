package eulergui.inputs.dispatcher;

import java.net.URL;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;
import deductions.Namespaces;

public class FormatRecognizer {

    private boolean rdfRoot;

    private boolean owlRoot;

    public FormatRecognizer(URL url) {
        FormatRecognizerContentHandler handler = new FormatRecognizerContentHandler();
        try {
            XMLReader saxReader = XMLReaderFactory.createXMLReader();
            saxReader.setContentHandler(handler);
            saxReader.parse(new InputSource(url.openStream()));
        } catch (StopProcessingException e) {
            if (Namespaces.prefixToId.get("rdf").equals(handler.startURI)) {
                System.out.println("FormatRecognizer: " + url + ": root XML element: " + handler.startLocalName);
                rdfRoot = true;
            } else if (Namespaces.prefixToId.get("owl").equals(handler.startURI)) {
                System.out.println("FormatRecognizer: " + url + ": root XML element: " + handler.startLocalName);
                owlRoot = true;
            }
        } catch (Exception e) {
            throw new RuntimeException("N3SourceFromXML_Gloze.extractXMLNamespaces():\n" + "Could not load from URL " + url + "\n reason: " + e.getLocalizedMessage() + "\n cause: " + e.getCause(), e);
        }
    }

    public boolean isRDF() {
        return rdfRoot;
    }

    public boolean isPlainXML() {
        return !rdfRoot && !owlRoot;
    }

    /** cf http://www.w3.org/TR/2009/REC-owl2-xml-serialization-20091027/ */
    public boolean isOWLXML() {
        return owlRoot;
    }
}
