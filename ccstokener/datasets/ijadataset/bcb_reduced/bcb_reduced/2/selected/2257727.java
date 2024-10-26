package org.idspace.aau.iwis.recommend;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Iterator;
import org.idspace.aau.iwis.recommend.model.CreativityStatement;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class TripleFinder {

    public static String SUBCLASS_QUERY = "SUBCLASS_QUERY";

    public static String SUBCLASS_DBPEDIA_QUERY = "SUBCLASS_DBPEDIA_QUERY";

    public static String RANGE_QUERY = "RANGE_QUERY";

    public static String TRIPLE_QUERY = "TRIPLE_QUERY";

    public static String PURL_SUBJECT = "PURL_SUBJECT";

    public static String DOMAIN_QUERY = "DOMAIN_QUERY";

    public static String ALL_QUERY = "ALL_QUERY";

    public static String DBPEDIA = "<http://dbpedia.org/ontology/";

    public static String TERM_SUBJECT = "<http://purl.org/dc/terms/subject>";

    public static String PREFIX = "   PREFIX owl: <http://www.w3.org/2002/07/owl#> " + " 	PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>" + " 	PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>" + " 	PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>" + " 	PREFIX foaf: <http://xmlns.com/foaf/0.1/>" + " 	PREFIX dc: <http://purl.org/dc/elements/1.1/>" + " 	PREFIX : <http://dbpedia.org/resource/>" + " 	PREFIX dbpedia2: <http://dbpedia.org/property/>" + " 	PREFIX dbpedia: <http://dbpedia.org/>" + " 	PREFIX skos: <http://www.w3.org/2004/02/skos/core#>";

    /**
	 * Empty Constructor
	 */
    public TripleFinder() {
        super();
    }

    /**
	 * Sets up a query with the defined arguments
	 * @return the built query
	 */
    public String buildQuery(String prefix, CreativityStatement statement, String baseWord) {
        StringBuilder builtQuery = new StringBuilder(prefix);
        builtQuery.append(" SELECT distinct ?s ?p ?o WHERE { ");
        for (Iterator<String> iterator = statement.getProperties().iterator(); iterator.hasNext(); ) {
            String prop = (String) iterator.next();
            builtQuery.append("{ ?s ?p ?o . ?s rdfs:label \"" + baseWord + "\"@en . FILTER regex(?p, \"" + prop + "\"). }");
            if (iterator.hasNext()) {
                builtQuery.append(" UNION ");
            }
        }
        builtQuery.append(" } ");
        return builtQuery.toString();
    }

    /**
	 * A simple test method...
	 */
    @Deprecated
    public void test() {
        try {
            String query = "* <http://xmlns.com/foaf/0.1/workplaceHomepage> <http://www.deri.ie/>" + "* <http://xmlns.com/foaf/0.1/knows> *";
            String url = "http://sindice.com/api/v2/search?qt=advanced&q=" + URLEncoder.encode(query, "utf-8") + "&qt=advanced";
            URL urlObj = new URL(url);
            URLConnection con = urlObj.openConnection();
            if (con != null) {
                Model model = ModelFactory.createDefaultModel();
                model.read(con.getInputStream(), null);
            }
            System.out.println(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
