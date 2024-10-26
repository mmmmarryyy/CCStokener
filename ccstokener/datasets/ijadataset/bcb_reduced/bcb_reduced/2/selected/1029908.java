package de.fuberlin.wiwiss.ng4j.trix;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import javax.xml.transform.TransformerException;
import org.xml.sax.SAXException;
import com.hp.hpl.jena.rdf.model.AnonId;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFErrorHandler;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.RDFReader;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.shared.JenaException;

/**
 * Simple RDFReader that adds support for the TriX syntax (see
 * <a href="http://www.hpl.hp.com/techreports/2004/HPL-2004-56">TriX
 * specification</a>) to the Jena framework. Does not support
 * TriX's named graph features. It adds all statements from
 * the first graph to a Jena model, ignoring its name if present,
 * and ignoring all further graphs if present.
 *
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class JenaRDFReaderWithExtensions implements RDFReader, ParserCallback {

    private Model targetModel;

    private boolean done = false;

    private Resource subject;

    private Property predicate;

    private RDFNode object;

    public void read(Model model, Reader r, String base) {
        this.targetModel = model;
        try {
            new TriXParserWithExtensions().parse(r, new URI(base), this);
        } catch (IOException e) {
            throw new JenaException(e);
        } catch (SAXException e) {
            throw new JenaException(e);
        } catch (URISyntaxException e) {
            throw new JenaException(e);
        } catch (TransformerException e) {
            throw new JenaException(e);
        }
    }

    public void read(Model model, InputStream r, String base) {
        this.targetModel = model;
        try {
            new TriXParserWithExtensions().parse(r, new URI(base), this);
        } catch (IOException e) {
            throw new JenaException(e);
        } catch (SAXException e) {
            throw new JenaException(e);
        } catch (URISyntaxException e) {
            throw new JenaException(e);
        } catch (TransformerException e) {
            throw new JenaException(e);
        }
    }

    public void read(Model model, String url) {
        try {
            URLConnection conn = new URL(url).openConnection();
            String encoding = conn.getContentEncoding();
            if (encoding == null) {
                read(model, conn.getInputStream(), url);
            } else {
                read(model, new InputStreamReader(conn.getInputStream(), encoding), url);
            }
        } catch (IOException e) {
            throw new JenaException(e);
        }
    }

    public Object setProperty(String propName, Object propValue) {
        return null;
    }

    public RDFErrorHandler setErrorHandler(RDFErrorHandler errHandler) {
        return null;
    }

    public void startGraph(List<String> uris) {
    }

    public void endGraph() {
        this.done = true;
    }

    public void subjectURI(String uri) {
        this.subject = this.targetModel.createResource(uri);
    }

    public void subjectBNode(String id) {
        this.subject = this.targetModel.createResource(new AnonId(id));
    }

    public void subjectPlainLiteral(String value, String lang) {
        throw new JenaException("Literals are not allowed as subjects in RDF");
    }

    public void subjectTypedLiteral(String value, String datatypeURI) {
        throw new JenaException("Literals are not allowed as subjects in RDF");
    }

    public void predicate(String uri) {
        this.predicate = this.targetModel.createProperty(uri);
    }

    public void objectURI(String uri) {
        this.object = this.targetModel.createResource(uri);
        addStatement();
    }

    public void objectBNode(String id) {
        this.object = this.targetModel.createResource(new AnonId(id));
        addStatement();
    }

    public void objectPlainLiteral(String value, String lang) {
        this.object = this.targetModel.createLiteral(value, lang);
        addStatement();
    }

    public void objectTypedLiteral(String value, String datatypeURI) {
        this.object = this.targetModel.createTypedLiteral(value, datatypeURI);
        addStatement();
    }

    private void addStatement() {
        if (this.done) {
            return;
        }
        this.targetModel.add(this.targetModel.createStatement(this.subject, this.predicate, this.object));
    }
}
