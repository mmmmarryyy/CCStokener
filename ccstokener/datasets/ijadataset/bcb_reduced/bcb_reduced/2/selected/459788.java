package eu.soa4all.reasoner.util.ontologies;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.deri.wsmo4j.io.parser.rdf.RDFParser;
import org.deri.wsmo4j.io.parser.rdf.RDFParser.Syntax;
import org.omwg.ontology.Ontology;
import org.wsmo.common.TopEntity;
import org.wsmo.common.exception.InvalidModelException;
import org.wsmo.wsml.Parser;
import org.wsmo.wsml.ParserException;
import com.ontotext.wsmo4j.parser.wsml.WsmlParser;
import eu.soa4all.wp6.common.utils.jar.JarResources;

/**
 * Utility class to load ontologies from the Web.
 */
public class OntologyLoader {

    public OntologyLoader() {
    }

    public Parser createWsmlParser() {
        return new WsmlParser();
    }

    public Parser createRdfNTriplesParser() {
        return new RDFParser(Syntax.N_TRIPLES, false);
    }

    public Parser createRdfN3Parser() {
        return new RDFParser(Syntax.N3, false);
    }

    public Parser createRdfXmlParser() {
        return new RDFParser(Syntax.RDF_XML, false);
    }

    public Parser createRdfTurtleParser() {
        return new RDFParser(Syntax.TURTLE, false);
    }

    public byte[] loadRaw(String ontologyUrl) throws IOException {
        URL url = new URL(ontologyUrl);
        InputStream is = url.openStream();
        final int BUFFER_SIZE = 1024;
        byte[] buffer = new byte[BUFFER_SIZE];
        ByteArrayOutputStream ontologyBytes = new ByteArrayOutputStream();
        for (; ; ) {
            int bytesRead = is.read(buffer);
            if (bytesRead <= 0) break;
            ontologyBytes.write(buffer, 0, bytesRead);
        }
        return ontologyBytes.toByteArray();
    }

    private Ontology loadOntology(String url, Parser parser) throws IOException, ParserException, InvalidModelException {
        assert url != null;
        byte[] raw = loadRaw(url);
        return parseOntology(new InputStreamReader(new ByteArrayInputStream(raw)), parser);
    }

    private Ontology loadOntologyFromJar(String url, Parser parser, String jar) throws IOException, ParserException, InvalidModelException {
        assert url != null;
        byte[] raw = loadFileInJar(jar, url);
        return parseOntology(new InputStreamReader(new ByteArrayInputStream(raw)), parser);
    }

    private byte[] loadFileInJar(String jarfile, String file) {
        JarResources jr = new JarResources(jarfile);
        byte[] r = jr.getResource(file);
        return r;
    }

    public Ontology parseOntology(Reader input, Parser parser) throws IOException, ParserException, InvalidModelException {
        TopEntity[] identifiable = parser.parse(input);
        for (TopEntity te : identifiable) {
            if (te instanceof Ontology) {
                return (Ontology) te;
            }
        }
        return null;
    }

    /**
	 * Load an ontology of unknown format. Try each of the known parsers one after the other.
	 * @param ontologyUri
	 */
    public Ontology loadUnknownOntology(String ontologyUri) {
        List<Parser> parsers = new ArrayList<Parser>();
        parsers.add(createRdfXmlParser());
        parsers.add(createRdfNTriplesParser());
        parsers.add(createRdfTurtleParser());
        parsers.add(createWsmlParser());
        try {
            byte[] rawOntology = loadRaw(ontologyUri);
            for (Parser p : parsers) {
                try {
                    Ontology ontology = parseOntology(new InputStreamReader(new ByteArrayInputStream(rawOntology)), p);
                    if (ontology != null) return ontology;
                } catch (Exception e) {
                }
            }
        } catch (Exception e) {
        }
        return null;
    }

    public Ontology loadWSMLOntology(Reader reader) throws IOException, ParserException, InvalidModelException {
        return parseOntology(reader, createWsmlParser());
    }

    public Ontology loadWSMLOntology(String ontologyIri) throws IOException, ParserException, InvalidModelException {
        assert ontologyIri != null;
        return loadOntology(ontologyIri, createWsmlParser());
    }

    public Ontology loadWSMLOntologyFromJar(String ontologyIri, String jar) throws IOException, ParserException, InvalidModelException {
        assert ontologyIri != null;
        return loadOntologyFromJar(ontologyIri, createWsmlParser(), jar);
    }

    public Ontology loadWSMLOntologyFromRdfXml(Reader reader) throws IOException, ParserException, InvalidModelException {
        return parseOntology(reader, createRdfXmlParser());
    }

    public Ontology loadWSMLOntologyFromRdfXml(String ontologyIri) throws IOException, ParserException, InvalidModelException {
        assert ontologyIri != null;
        return loadOntology(ontologyIri, createRdfXmlParser());
    }

    public Ontology loadWSMLOntologyFromRdfXmlFromJar(String ontologyIri, String jar) throws IOException, ParserException, InvalidModelException {
        assert ontologyIri != null;
        return loadOntologyFromJar(ontologyIri, createRdfXmlParser(), jar);
    }

    public Ontology loadWSMLOntologyFromRdfNTriples(Reader reader) throws IOException, ParserException, InvalidModelException {
        return parseOntology(reader, createRdfNTriplesParser());
    }

    public Ontology loadWSMLOntologyFromRdfNTriples(String ontologyIri) throws IOException, ParserException, InvalidModelException {
        assert ontologyIri != null;
        return loadOntology(ontologyIri, createRdfNTriplesParser());
    }

    public Ontology loadWSMLOntologyFromRdfN3(Reader reader) throws IOException, ParserException, InvalidModelException {
        return parseOntology(reader, createRdfN3Parser());
    }

    public Ontology loadWSMLOntologyFromRdfN3(String ontologyIri) throws IOException, ParserException, InvalidModelException {
        assert ontologyIri != null;
        return loadOntology(ontologyIri, createRdfN3Parser());
    }

    public Ontology loadWSMLOntologyFromRdfN3FromJar(String ontologyIri, String jar) throws IOException, ParserException, InvalidModelException {
        assert ontologyIri != null;
        return loadOntologyFromJar(ontologyIri, createRdfN3Parser(), jar);
    }
}
