package org.ivoa.dm.semantics;

import org.w3c.dom.Document;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamSource;

/**
 * Load ontologies/semantic vocabularies etc to be used by data models.<br>
 *
 * @author Laurent Bourges (voparis) / Gerard Lemson (mpe)
 */
public class OntologyFactory {

    /**
   * TODO : Method Description
   *
   * @param uri
   *
   * @return value TODO : Value Description
   */
    public Ontology loadOntology(final String uri) {
        Ontology ontology = null;
        Document doc = parse(uri);
        doc.getElementsByTagName("skos");
        return ontology;
    }

    /**
   * TODO : Method Description
   *
   * @param uri
   *
   * @return value TODO : Value Description
   */
    private Document parse(final String uri) {
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            URL url = new URL(uri);
            URLConnection con = url.openConnection();
            con.addRequestProperty("Accept", "application/rdf+xml");
            Templates template = factory.newTemplates(new StreamSource(new FileInputStream("skos.xsl")));
            Transformer xformer = template.newTransformer();
            Source source = new StreamSource(url.openStream());
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = builder.newDocument();
            Result result = new DOMResult(doc);
            xformer.transform(source, result);
            return doc;
        } catch (final ParserConfigurationException e) {
        } catch (final FileNotFoundException e) {
            e.printStackTrace();
        } catch (final TransformerConfigurationException e) {
        } catch (final TransformerException e) {
        } catch (final MalformedURLException mex) {
        } catch (final IOException ioe) {
        }
        return null;
    }

    /**
   * TODO : Method Description
   *
   * @param args
   */
    public static void main(final String[] args) {
        OntologyFactory f = new OntologyFactory();
        String uri = "http://volute.googlecode.com/svn/trunk/projects/theory/snapdm/input/vocabularies-1.0/IAUT93.rdf";
        Ontology o = f.loadOntology(uri);
    }
}
