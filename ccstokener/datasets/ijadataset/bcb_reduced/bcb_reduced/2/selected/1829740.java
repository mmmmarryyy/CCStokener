package org.deri.xquery.saxon;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.sf.json.JSON;
import net.sf.json.JSONSerializer;
import net.sf.json.xml.XMLSerializer;
import org.deri.xquery.DatasetResults;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.QuerySolutionMap;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetRewindable;
import com.hp.hpl.jena.tdb.TDBFactory;

/**
 * Library of Java methods for usage from within XQuery queries when using Saxon
 * 
 * @author Stefan Bischof
 * @author Nuno Lopes
 * 
 */
class EvaluatorExternalFunctions {

    private static Map<String, DatasetResults> scopedDataset = new HashMap<String, DatasetResults>();

    private static String TDBLocation = System.getProperty("user.home") + "/.xsparql/TDB";

    /**
   * Saves string s to a local file.
   * 
   * @param prefix
   *          Turtle preamble
   * @param n3
   *          Turtle content
   * @return URI of local file containing string s
   */
    public static String turtleGraphToURI(String prefix, String n3) {
        String ret = "";
        try {
            File temp = File.createTempFile("sparqlGraph", ".n3");
            temp.deleteOnExit();
            BufferedWriter out = new BufferedWriter(new FileWriter(temp));
            out.write(prefix);
            out.write(n3.replace("\\", "\\\\"));
            out.close();
            ret = "file://" + temp.getAbsolutePath();
        } catch (IOException e) {
        }
        return ret;
    }

    public static Dataset getTDBDataset(String location) {
        if (!new File(location).exists()) {
            try {
                new File(location).mkdirs();
            } catch (Exception e) {
                System.err.println("Error retrieving the datasets: " + e.getMessage());
                System.exit(1);
            }
        }
        Dataset dataset = TDBFactory.createDataset(location);
        return dataset;
    }

    public static Dataset getTDBDataset() {
        String location = TDBLocation;
        return getTDBDataset(location);
    }

    public static String getDefaultTDBDatasetLocation() {
        return TDBLocation;
    }

    /**
   * Evaluates a SPARQL query, storing the bindings to be reused later. Used for
   * the ScopedDataset.
   * 
   * @param q
   *          query to be executed
   * @param id
   *          solution id
   * @return XML results of the query
   */
    public static ResultSet createScopedDataset(String q, String id) {
        if (scopedDataset.containsKey(id)) {
        }
        Query query = QueryFactory.create(q);
        Dataset dataset = DatasetFactory.create(query.getGraphURIs(), query.getNamedGraphURIs());
        QueryExecution qe = QueryExecutionFactory.create(query, dataset);
        ResultSet resultSet = qe.execSelect();
        DatasetResults ds = new DatasetResults(dataset);
        ResultSetRewindable results = ds.addResults(resultSet);
        scopedDataset.put(id, ds);
        return results;
    }

    /**
   * Evaluates a SPARQL query, using previously stored dataset and bindings.
   * Used for the ScopedDataset.
   * 
   * @param q
   *          query to be executed
   * @param id
   *          solution id
   * @param joinVars
   *          joining variables that will be put in the initialBinding
   * @param pos
   *          current iteration
   * @return XML results of the query
   */
    public static ResultSet sparqlScopedDataset(String q, String id, String joinVars, int pos) {
        if (!scopedDataset.containsKey(id)) {
        }
        Dataset dataset = scopedDataset.get(id).getDataset();
        ResultSetRewindable results = scopedDataset.get(id).getResults();
        results.reset();
        QuerySolutionMap initialBinding = createSolutionMap(results, joinVars, pos);
        QueryExecution qe = QueryExecutionFactory.create(q, dataset, initialBinding);
        ResultSet resultSet = qe.execSelect();
        ResultSetRewindable results2 = scopedDataset.get(id).addResults(resultSet);
        return results2;
    }

    /**
   * Creates an initialBinding from the previous solutions and the join vars.
   * 
   * @param results
   *          previous resultSet
   * @param joinVars
   *          joining variables that will be put in the initialBinding
   * @param pos
   *          current iteration
   * @return QuerySolutionMap to be used for filtering results
   */
    private static QuerySolutionMap createSolutionMap(ResultSetRewindable results, String joinVars, int pos) {
        QuerySolutionMap initialBinding = new QuerySolutionMap();
        String[] joinVarsArray = joinVars.split(",");
        QuerySolution s = new QuerySolutionMap();
        int it = 1;
        while (results.hasNext()) {
            QuerySolution qs = results.next();
            if (it == pos) {
                s = qs;
                break;
            }
            it++;
        }
        Iterator<String> iterator = Arrays.asList(joinVarsArray).iterator();
        while (iterator.hasNext()) {
            String st = iterator.next();
            if (s.contains(st)) {
                initialBinding.add(st, s.get(st));
            }
        }
        return initialBinding;
    }

    /**
   * Deletes stored dataset and solutions.
   * 
   * @param id
   *          solution id
   */
    public static void deleteScopedDataset(String id) {
        scopedDataset.remove(id);
    }

    /**
   * Deletes the last results from the stack.
   * 
   * @param id
   *          solution id
   */
    public static void scopedDatasetPopResults(String id) {
        if (scopedDataset.size() > 0) {
            scopedDataset.get(id).popResults();
        }
    }

    /**
   * Retrieves data from a url, Converts JSON data to XML
   * 
   * @param URL   location of the data
   * 
   */
    public static String jsonToXML(String loc) {
        String xml = "";
        String jsonData = "";
        try {
            URL url = new URL(loc);
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                jsonData += inputLine;
            }
            XMLSerializer serializer = new XMLSerializer();
            JSON json = JSONSerializer.toJSON(jsonData);
            serializer.setTypeHintsEnabled(false);
            xml = serializer.write(json);
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        return xml;
    }
}
