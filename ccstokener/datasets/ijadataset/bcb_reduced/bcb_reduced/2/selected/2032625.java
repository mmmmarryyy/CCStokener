package ontorama.ontotools.source.cgkb;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import ontorama.model.graph.EdgeType;
import ontorama.ontotools.CancelledQueryException;
import ontorama.ontotools.SourceException;
import ontorama.ontotools.query.Query;
import ontorama.ontotools.source.Source;
import ontorama.ontotools.source.SourceResult;
import ontorama.ontotools.source.UrlQueryStringConstructor;

public class CgKbSource implements Source {

    private static final int _defaultDepth = 1;

    public SourceResult getSourceResult(String uri, Query query) throws SourceException, CancelledQueryException {
        UrlQueryStringConstructor queryStringConstructor = new UrlQueryStringConstructor();
        List<EdgeType> relLinksList = query.getRelationLinksList();
        if (relLinksList.isEmpty()) {
            System.err.println("Query relation links list is empty!");
        }
        String allReadersString = "";
        for (EdgeType relDetails : relLinksList) {
            String readerString = "";
            Map<String, String> paramTable = new HashMap<String, String>();
            paramTable.put("node", query.getQueryTypeName());
            paramTable.put("rel", relDetails.getName());
            int depth = query.getDepth();
            if ((depth < 1) || (depth > 3)) {
                depth = _defaultDepth;
            }
            Integer depthInt = new Integer(depth);
            paramTable.put("depth", depthInt.toString());
            String queryString = queryStringConstructor.getQueryString(paramTable);
            String fullUrl = uri + queryString;
            URL url = null;
            try {
                Reader reader = doCgiFormPost(uri, paramTable);
                BufferedReader br = new BufferedReader(reader);
                String line;
                while ((line = br.readLine()) != null) {
                    readerString = readerString + line + "\n";
                }
            } catch (MalformedURLException mue) {
                throw new SourceException("Source Url " + url + " is malformed", mue);
            } catch (IOException ioe) {
                throw new SourceException("Couldn't retrieve data from source " + fullUrl, ioe);
            }
            allReadersString = allReadersString + readerString;
        }
        StringReader strReader = new StringReader(allReadersString);
        return new SourceResult(true, strReader, query);
    }

    private Reader doCgiFormPost(String urlLoc, Map<String, String> parameters) throws IOException {
        URL url = new URL(urlLoc);
        URLConnection connection = url.openConnection();
        connection.setDoOutput(true);
        PrintWriter out = new PrintWriter(connection.getOutputStream());
        String paramString = "";
        for (Entry<String, String> entry : parameters.entrySet()) {
            String paramName = entry.getKey();
            String paramValue = entry.getValue();
            paramString = paramString + paramName + "=" + paramValue + "&";
        }
        out.print(paramString);
        out.close();
        InputStreamReader in = new InputStreamReader(connection.getInputStream());
        return in;
    }
}
