package crawling;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;
import java.util.TreeMap;
import org.apache.log4j.Logger;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.OWL2;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.RDFSyntax;
import com.hp.hpl.jena.vocabulary.XSD;

/**
 * A recursive RDF resource crawler.
 * 
 * @author Christoph
 *
 */
public class ResourceCrawler {

    private static final String RDF_TYPE = "application/rdf+xml";

    private String root;

    private Model model;

    private int depth;

    private Logger log;

    private Map<String, String> nspfmap = new TreeMap<String, String>();

    private long lastRequest = System.currentTimeMillis();

    /**
	 * @param root the (remote) rdf file from where crawling is started
	 * @param depth number of levels that will be crawled (root being level 0)
	 */
    public ResourceCrawler(String root, int depth) {
        this.root = root;
        this.depth = depth;
        this.model = ModelFactory.createDefaultModel();
        this.model.getReader().setErrorHandler(null);
        this.log = Logger.getLogger("crawler");
    }

    /**
	 * do it
	 */
    public void crawl() {
        Model model2parse = ModelFactory.createDefaultModel();
        log.info("Parsing root resource.");
        try {
            model2parse.read(configureConnection(root), root);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoRDFException e) {
            e.printStackTrace();
        }
        log.info("Model size: " + model2parse.size());
        Model model_current = null;
        for (int i = 1; i <= this.depth; i++) {
            model_current = ModelFactory.createDefaultModel();
            log.info("Parsing level " + i + " resources.");
            ExceptionCounter ec = new ExceptionCounter();
            for (NodeIterator it = model2parse.listObjects(); it.hasNext(); ) {
                RDFNode n = it.nextNode();
                if (!this.model.containsResource(n) && n.isURIResource() && !isPseudoURI(n.asResource()) && !isSchematic(n.asResource())) {
                    String uri = n.asResource().getURI();
                    try {
                        model_current.read(configureConnection(uri), uri);
                    } catch (Exception e) {
                        ec.add(e);
                    }
                }
            }
            this.nspfmap.putAll(model2parse.getNsPrefixMap());
            this.model.add(model2parse);
            model2parse = model_current;
            log.info("Exception count: " + ec.toString());
            log.info("Model size: " + (int) (this.model.size() + model2parse.size()));
        }
        this.nspfmap.putAll(model_current.getNsPrefixMap());
        this.model.add(model_current);
        this.model.setNsPrefixes(this.nspfmap);
    }

    /**
	 * @param url the URL to be configured
	 * @return the input stream of a valid remote rdf file
	 * @throws IOException if no URL connection could be established
	 */
    private InputStream configureConnection(String url) throws IOException, NoRDFException {
        while (this.lastRequest > System.currentTimeMillis() - 1000) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        URL uurl = new URL(url);
        HttpURLConnection con = (HttpURLConnection) uurl.openConnection();
        this.lastRequest = System.currentTimeMillis();
        con.setRequestProperty("Accept", RDF_TYPE);
        con.connect();
        String type = con.getContentType();
        int split = type.indexOf(";");
        if (type.equalsIgnoreCase(RDF_TYPE) || (split >= 0 && type.substring(0, split).equalsIgnoreCase(RDF_TYPE))) {
            return con.getInputStream();
        } else {
            throw new NoRDFException(type);
        }
    }

    /**
	 * Checks whether this resource represents an RDF description that is already known to jena.
	 * 
	 * @param r an RDF ressource to be tested
	 * @return true if r is from a RDF/RDFS/OWL/OWL2/XSD namespace, false otherwise
	 */
    private boolean isSchematic(Resource r) {
        String nsr = r.getNameSpace();
        return RDF.getURI().equals(nsr) || RDFS.getURI().equals(nsr) || RDFSyntax.getURI().equals(nsr) || OWL.getURI().equals(nsr) || OWL2.getURI().equals(nsr) || XSD.getURI().equals(nsr);
    }

    /**
	 * @param r the resource 2b checked
	 * @return true if the resource uri does not start with true protocol prefix
	 */
    private boolean isPseudoURI(Resource r) {
        return r.getURI().startsWith("mailto:") || r.getURI().startsWith("tel:") || r.getURI().startsWith("urn:");
    }

    private boolean checkURI(Resource r) {
        try {
            URI uri = new URI(r.getURI());
            uri.toString();
            return true;
        } catch (URISyntaxException e) {
            return false;
        }
    }

    /**
	 * Write the model to file. {@link #crawl()}} should be invoked first.
	 * 
	 * @param file
	 */
    public void write(File file) {
        Model modelCleaned = ModelFactory.createDefaultModel();
        StmtIterator it = model.listStatements();
        for (Statement s : it.toSet()) {
            Resource r = s.getSubject();
            if (checkURI(r)) {
                modelCleaned.add(s);
            }
        }
        try {
            modelCleaned.write(new FileWriter(file), "RDF/XML");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
	 * hula hoop
	 */
    public static void main(String[] args) {
        ResourceCrawler c = new ResourceCrawler("http://data.linkedmdb.org/resource/film/14838", 2);
        c.crawl();
        c.write(new File("desert_victory_crawl2.rdf"));
    }
}
