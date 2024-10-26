package n3_project;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Logger;
import eulergui.inputs.N3SourceFromXMI;
import eulergui.inputs.N3SourceFromXML_Gloze;
import eulergui.inputs.dispatcher.FormatRecognizer;
import eulergui.project.N3Source;
import eulergui.project.Project;
import eulergui.util.URLHelper;

/**
 * attempts to find the right source format (N3, RDF, XMI, plain XML, ...)
 * downloadable from given URL; delegate within {@link ProjectGUI}; used for
 * opening an URL with the 2nd button in the toolbar, and
 * {@link DropURLTranferHandler} ;
 * 
 * TODO move to eulergui/inputs/dispatcher TODO consider use a rule set here
 * instead of imperative programming
 * 
 */
public class SourceFactory {

    /** factory for N3Source: guesse the exact format of given URL, content type, extension, first line, and if XML root element
	 * TODO RDFa */
    public N3Source addSource(URL url, Project project) throws IOException, URISyntaxException {
        N3Source n3 = null;
        if (URLHelper.isLocal(url) && !(new File(url.toURI())).exists()) {
            return new N3Source(url);
        }
        final URLConnection openConnection = makeN3URLConnection(url);
        final String contentType = openConnection.getContentType();
        final InputStream is = openConnection.getInputStream();
        final BufferedInputStream bis = new BufferedInputStream(is);
        final BufferedReader br = new BufferedReader(new InputStreamReader(bis, "UTF8"));
        String firstLine = br.readLine();
        Logger.getLogger("theDefault").info(url.toString() + " : contentType: \"" + contentType + "\"" + " first line: \"" + firstLine + "\"");
        final String file = url.getFile();
        if (file.endsWith(".n3") || file.endsWith(".ttl") || file.endsWith(".nt") || (contentType == null && !looksLikeXMLStart(firstLine))) {
            n3 = new N3Source(url);
        } else if (contentType != null && (contentType.contains("n3") || (contentType.equals("text/plain") && !looksLikeXMLStart(firstLine)))) {
            n3 = new N3Source(url);
        } else if (isXMIURL(file)) {
            n3 = new N3SourceFromXMI(url.toURI(), project);
            n3.prepare(project);
        } else if (contentType != null && contentType.contains("application/rdf+xml") || file.endsWith(".rdf") || file.endsWith(".rdfs")) {
            n3 = new N3SourceFromRDF(url, project);
        } else {
            final FormatRecognizer recognizer = new FormatRecognizer(url);
            if (recognizer.isRDF()) {
                n3 = new N3SourceFromRDF(url, project);
            } else if (recognizer.isOWLXML()) {
                n3 = new N3SourceFromOWL(url, project);
            } else if (recognizer.isPlainXML()) {
                final N3Source n3Gloze = new N3SourceFromXML_Gloze();
                n3Gloze.setURI(url.toString());
                n3Gloze.prepare(project);
                n3 = n3Gloze;
            }
        }
        if (project != null) {
            project.addN3Source(n3);
        }
        return n3;
    }

    /** applies the right "accept" headers for N3 */
    public URLConnection makeN3URLConnection(URL url) throws IOException {
        final URLConnection openConnection = url.openConnection();
        openConnection.setRequestProperty("accept", "text/n3" + ", application/n3" + ", text/rdf+n3" + ", text/turtle" + ", application/rdf+xml, text/xml" + ", text/plain" + ", text/xhtml" + ", text/html");
        openConnection.setConnectTimeout(2000);
        return openConnection;
    }

    /** simple detection of XMI, UML, etc by extension */
    public static boolean isXMIURL(String file) {
        return file.endsWith(".ecore") || file.endsWith(".uml") || file.endsWith(".xmi") || file.endsWith(".mof") || file.endsWith(".emof") || file.endsWith(".cmof");
    }

    private boolean looksLikeXMLStart(String firstLine) {
        return firstLine.startsWith("<?xml ") || firstLine.startsWith("<!--");
    }
}
