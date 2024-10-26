package ontorama.webkbtools.inputsource.cgkb;

import ontorama.webkbtools.inputsource.*;
import ontorama.webkbtools.query.Query;
import ontorama.webkbtools.util.SourceException;
import ontorama.webkbtools.util.CancelledQueryException;
import ontorama.ontologyConfig.RelationLinkDetails;
import ontorama.OntoramaConfig;
import java.util.*;
import java.io.*;
import java.net.*;

public class CgKbSource implements Source {

    private static final int _defaultDepth = 1;

    public SourceResult getSourceResult(String uri, Query query) throws SourceException, CancelledQueryException {
        UrlQueryStringConstructor queryStringConstructor = new UrlQueryStringConstructor();
        List relLinksList = query.getRelationLinksList();
        if (relLinksList.isEmpty()) {
            System.err.println("Query relation links list is empty!");
        }
        Iterator it = relLinksList.iterator();
        String allReadersString = "";
        while (it.hasNext()) {
            RelationLinkDetails relDetails = (RelationLinkDetails) it.next();
            String readerString = "";
            Hashtable paramTable = new Hashtable();
            paramTable.put("node", query.getQueryTypeName());
            paramTable.put("rel", relDetails.getLinkName());
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
                throw new SourceException("Source Url " + url + " is malformed");
            } catch (IOException ioe) {
                throw new SourceException("Couldn't retrieve data from source " + fullUrl);
            }
            allReadersString = allReadersString + readerString;
        }
        StringReader strReader = new StringReader(allReadersString);
        return new SourceResult(true, strReader, query);
    }

    private Reader doCgiFormPost(String urlLoc, Hashtable parameters) throws IOException {
        URL url = new URL(urlLoc);
        URLConnection connection = url.openConnection();
        connection.setDoOutput(true);
        PrintWriter out = new PrintWriter(connection.getOutputStream());
        Enumeration e = parameters.keys();
        String paramString = "";
        while (e.hasMoreElements()) {
            String paramName = (String) e.nextElement();
            String paramValue = (String) parameters.get(paramName);
            paramString = paramString + paramName + "=" + paramValue + "&";
        }
        out.print(paramString);
        System.out.println("POST param: " + paramString);
        out.close();
        InputStreamReader in = new InputStreamReader(connection.getInputStream());
        return in;
    }

    private Reader doCgiFormGet(String urlLoc) throws MalformedURLException, IOException {
        URL url = new URL(urlLoc);
        URLConnection connection = url.openConnection();
        Reader reader = new InputStreamReader(connection.getInputStream());
        return reader;
    }
}
