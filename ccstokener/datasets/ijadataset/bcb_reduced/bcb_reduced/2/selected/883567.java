package de.fuberlin.wiwiss.marbles;

import static org.junit.Assert.*;
import info.aduna.iteration.CloseableIteration;
import info.aduna.iteration.Iterations;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.openrdf.sail.SailException;
import org.openrdf.sail.inferencer.InferencerConnection;
import org.openrdf.sail.nativerdf.NativeStore;
import org.openrdf.sail.nativerdf.model.NativeLiteral;
import org.openrdf.sail.rdbms.mysql.MySqlStore;

/**
 * Testcases for the SameAsInferencer
 * 
 * @author Christian Becker
 */
public class SameAsInferencerTest {

    /**
	 * Path to an empty directory to use for the native store
	 */
    final String dbPath = "/Users/Christian/Desktop/Diplomarbeit/data/inferencerTest";

    final String geoNamesNS = "http://www.geonames.org/ontology#";

    final String dbpediaNS = "http://dbpedia.org/property/";

    SameAsInferencer inferencer;

    Repository dataRepo;

    RepositoryConnection repoConn;

    ValueFactory valueFactory;

    URI DBpediaMet, DBpediaImage, geoNamesMet;

    @BeforeClass
    public static void classSetup() throws RepositoryException {
        org.apache.log4j.BasicConfigurator.configure();
    }

    /**
	 * Add data, then sameAs (with delayed autoCommit)
	 * @throws RepositoryException
	 */
    @Test
    public void testInference1() throws RepositoryException {
        repoConn.setAutoCommit(false);
        String metName = "Metropolitan Museum of Art";
        assertFalse(repoConn.hasStatement((Resource) null, null, null, false));
        repoConn.add(geoNamesMet, valueFactory.createURI(geoNamesNS, "name"), valueFactory.createLiteral(metName));
        repoConn.add(DBpediaImage, valueFactory.createURI(dbpediaNS, "museum"), DBpediaMet);
        repoConn.add(DBpediaMet, OWL.SAMEAS, geoNamesMet);
        repoConn.setAutoCommit(true);
        List<Statement> statements = repoConn.getStatements(DBpediaMet, valueFactory.createURI(geoNamesNS, "name"), null, true).asList();
        assertEquals(statements.size(), 1);
        Statement st = statements.get(0);
        Literal obj = (Literal) st.getObject();
        assertEquals(obj.getLabel(), metName);
        statements = repoConn.getStatements(null, valueFactory.createURI(dbpediaNS, "museum"), geoNamesMet, true).asList();
        assertEquals(statements.size(), 1);
        st = statements.get(0);
        assertEquals(st.getSubject(), DBpediaImage);
    }

    /**
	 * Add sameAs, then data
	 * @throws RepositoryException
	 */
    @Test
    public void testInference2() throws RepositoryException {
        String metName = "Metropolitan Museum of Art";
        assertFalse(repoConn.hasStatement((Resource) null, null, null, false));
        repoConn.add(DBpediaMet, OWL.SAMEAS, geoNamesMet);
        repoConn.add(geoNamesMet, valueFactory.createURI(geoNamesNS, "name"), valueFactory.createLiteral(metName));
        repoConn.add(DBpediaImage, valueFactory.createURI(dbpediaNS, "museum"), DBpediaMet);
        List<Statement> statements = repoConn.getStatements(DBpediaMet, valueFactory.createURI(geoNamesNS, "name"), null, true).asList();
        assertEquals(statements.size(), 1);
        Statement st = statements.get(0);
        Literal obj = (Literal) st.getObject();
        assertEquals(obj.getLabel(), metName);
        statements = repoConn.getStatements(null, valueFactory.createURI(dbpediaNS, "museum"), geoNamesMet, true).asList();
        assertEquals(statements.size(), 1);
        st = statements.get(0);
        assertEquals(st.getSubject(), DBpediaImage);
    }

    /**
	 * Manual initialization
	 * @throws RepositoryException
	 * @throws SailException 
	 */
    @Test
    public void testInference3() throws RepositoryException, SailException {
        String metName = "Metropolitan Museum of Art";
        assertFalse(repoConn.hasStatement((Resource) null, null, null, false));
        inferencer.setAutoInference(false);
        repoConn.add(DBpediaMet, OWL.SAMEAS, geoNamesMet);
        repoConn.add(geoNamesMet, valueFactory.createURI(geoNamesNS, "name"), valueFactory.createLiteral(metName));
        repoConn.add(DBpediaImage, valueFactory.createURI(dbpediaNS, "museum"), DBpediaMet);
        inferencer.addInferredForResource(DBpediaMet);
        List<Statement> statements = repoConn.getStatements(DBpediaMet, valueFactory.createURI(geoNamesNS, "name"), null, true).asList();
        assertEquals(statements.size(), 1);
        Statement st = statements.get(0);
        Literal obj = (Literal) st.getObject();
        assertEquals(obj.getLabel(), metName);
        statements = repoConn.getStatements(null, valueFactory.createURI(dbpediaNS, "museum"), geoNamesMet, true).asList();
        assertEquals(statements.size(), 1);
        st = statements.get(0);
        assertEquals(st.getSubject(), DBpediaImage);
    }

    /**
	 * Test with real data
	 * @throws RepositoryException
	 * @throws SailException 
	 * @throws RDFParseException 
	 * @throws MalformedURLException 
	 */
    @Test
    public void testInference4() throws RepositoryException, SailException, RDFParseException, MalformedURLException {
        loadRDFURL(new URL("http://dbpedia.org/data/Brandenburg_Gate"));
        loadRDFURL(new URL("http://revyu.com/sparql?query=PREFIX+rev%3A+%3Chttp%3A%2F%2Fpurl.org%2Fstuff%2Frev%23%3E%0D%0APREFIX+rdfs%3A+%3Chttp%3A%2F%2Fwww.w3.org%2F2000%2F01%2Frdf-schema%23%3E%0D%0APREFIX+owl%3A+%3Chttp%3A%2F%2Fwww.w3.org%2F2002%2F07%2Fowl%23%3E+%0D%0A%0D%0ACONSTRUCT+%7B%0D%0A+%3Frev+%3Fp+%3Fo+.%0D%0A+%3Fthing+%3Fq+%3Fr%0D%0A%7D%0D%0AWHERE%0D%0A%7B%0D%0A++%3Fthing+owl%3AsameAs+%3Chttp%3A%2F%2Fdbpedia.org%2Fresource%2FBrandenburg_Gate%3E+.%0D%0A++%3Fthing+rev%3AhasReview+%3Frev+.%0D%0A++%3Frev+%3Fp+%3Fo+.%0D%0A++%3Fthing+%3Fq+%3Fr%0D%0A%7D"));
        Resource bbDBpedia = valueFactory.createURI("http://dbpedia.org/resource/Brandenburg_Gate");
        Resource bbRevyu = valueFactory.createURI("http://revyu.com/things/brandenburg-gate-berlin-tourism");
        assertTrue(repoConn.hasStatement(bbRevyu, valueFactory.createURI("http://purl.org/stuff/rev#hasReview"), null, false));
        assertTrue(repoConn.hasStatement(bbDBpedia, null, null, false));
        assertTrue(repoConn.hasStatement(bbDBpedia, valueFactory.createURI("http://purl.org/stuff/rev#hasReview"), null, true));
    }

    @Before
    public void setUp() throws Exception {
        inferencer = new SameAsInferencer(new NativeStore(new File(dbPath)));
        dataRepo = new SailRepository(inferencer);
        dataRepo.initialize();
        repoConn = dataRepo.getConnection();
        valueFactory = dataRepo.getValueFactory();
        DBpediaMet = valueFactory.createURI("http://dbpedia.org/resource/Metropolitan_Museum_of_Art");
        DBpediaImage = valueFactory.createURI("http://dbpedia.org/resource/A_Girl_Asleep_%28Vermeer%29");
        geoNamesMet = valueFactory.createURI("http://sws.geonames.org/5126698/");
        repoConn.remove((Resource) null, null, null);
        repoConn.commit();
        System.out.println(repoConn.hasStatement((Resource) null, null, null, false));
        inferencer.setAutoInference(true);
    }

    @After
    public void tearDown() throws Exception {
        System.out.println("shutdown");
        repoConn.commit();
        repoConn.close();
        dataRepo.shutDown();
    }

    private void loadRDFURL(URL url) throws RDFParseException, RepositoryException {
        URI urlContext = valueFactory.createURI(url.toString());
        try {
            URLConnection urlConn = url.openConnection();
            urlConn.setRequestProperty("Accept", "application/rdf+xml");
            InputStream is = urlConn.getInputStream();
            repoConn.add(is, url.toString(), RDFFormat.RDFXML, urlContext);
            is.close();
            repoConn.commit();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
