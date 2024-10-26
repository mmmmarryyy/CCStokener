package org.deri.xquery;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.deri.xsparql.Configuration;
import org.deri.xsparql.DatasetResults;
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
public class EvaluatorExternalFunctions {

    private static Map<String, DatasetResults> scopedDataset = new HashMap<String, DatasetResults>();

    /**
     * Saves string s to a local file.
     * 
     * @param prefix
     *            Turtle preamble
     * @param n3
     *            Turtle content
     * @return URI of local file containing string s
     */
    public static String turtleGraphToURI(String prefix, String n3) {
        String ret = "";
        try {
            File temp = File.createTempFile("sparqlGraph", ".n3");
            temp.deleteOnExit();
            BufferedWriter out = new BufferedWriter(new FileWriter(temp));
            out.write(prefix);
            out.write(n3);
            out.close();
            ret = "file://" + temp.getAbsolutePath();
        } catch (IOException e) {
        }
        return ret;
    }

    /**
     * Performs a POST query to a url. Called from the rewritten query for the
     * Named graphs optimisation.
     * 
     * @param endpoint
     *            endpoint to POST the query
     * @param data
     *            data to be POSTed
     */
    public static void doPostQuery(String endpoint, String data) {
        String ret = "";
        try {
            URL url = new URL(endpoint);
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write("request=" + data);
            wr.flush();
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = rd.readLine()) != null) {
                ret += line;
            }
            wr.close();
            rd.close();
        } catch (Exception e) {
        }
    }

    public static Dataset getTDBDataset() {
        String location = Configuration.getTDBLocation();
        if (!new File(location).exists()) {
            try {
                new File(location).mkdirs();
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        }
        Dataset dataset = TDBFactory.createDataset(location);
        return dataset;
    }

    /**
     * Evaluates a SPARQL query, storing the bindings to be reused later. Used
     * for the ScopedDataset.
     * 
     * @param q
     *            query to be executed
     * @param id
     *            solution id
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
     *            query to be executed
     * @param id
     *            solution id
     * @param joinVars
     *            joining variables that will be put in the initialBinding
     * @param pos
     *            current iteration
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
     *            previous resultSet
     * @param joinVars
     *            joining variables that will be put in the initialBinding
     * @param pos
     *            current iteration
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
     *            solution id
     */
    public static void deleteScopedDataset(String id) {
        scopedDataset.remove(id);
    }

    /**
     * Deletes the last results from the stack.
     * 
     * @param id
     *            solution id
     */
    public static void scopedDatasetPopResults(String id) {
        if (scopedDataset.size() > 0) {
            scopedDataset.get(id).popResults();
        }
    }
}
