package ontorama.ontotools.source;

import java.awt.Frame;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessControlException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import ontorama.OntoramaConfig;
import ontorama.model.graph.Edge;
import ontorama.model.graph.Node;
import ontorama.ui.OntoRamaApp;
import ontorama.ontotools.source.webkb.AmbiguousChoiceDialog;
import ontorama.ontotools.source.webkb.WebkbQueryStringConstructor;
import ontorama.ontotools.query.Query;
import ontorama.ontotools.parser.ParserResult;
import ontorama.ontotools.parser.rdf.RdfWebkbParser;
import ontorama.ontotools.CancelledQueryException;
import ontorama.ontotools.ParserException;
import ontorama.ontotools.SourceException;

public class WebKB2Source implements Source {

    /**
     * query we want to post to webkb
     */
    private Query query;

    /**
     * List used to hold multi RDF document.
     */
    private List docs = new LinkedList();

    /**
     * list of types extracted from the multiple readers
     */
    private List typesList = new LinkedList();

    /**
     * holds string representing all reader data
     * returned from webkb query
     */
    private String readerString = "";

    /**
     * Patterns to look  for if webkb query was unsuccessfull
     */
    private String webkbErorrStartPattern = "<br><b>";

    private String webkbErrorEndPattern = "</b><br>";

    /**
     * name of property 'Synonym'
     *
     * @todo  shouldn't hard code synonym property name, because if someone changes
     * it in the config.xml file - the whole thing will crash without reasonable
     * explanation. find a better way to do this!
     */
    private String synPropName = "synonym";

    /**
     *  Get a SourceResult from given uri. First, get a reader and check ir.
     *  If result is ambiguous - propmpt user
     *  to make a choice and return new formulated query. If result is not
     *  ambiguous - return reader.
     *
     *  To check if webkb returned error, we check if there were RDF end of
     *  element tags in the returned data (to see if we got RDF document back).
     *  If not - we check for error patterns trying to extract the error message.
     *
     *  @param  uri - base uri for the WebKB cgi script
     *  @param  query - object Query holding details of a query we are executing
     *  @return sourceResult
     *  @throws SourceException
     *  @throws CancelledQueryException
     *
     *  @todo mechanism for stopping interrupted queries seems hacky. at the moment
     *  we only check in the one method if thread is interrupted (because this loop is most time consuming)
     *  what if tread is interrupted somewhere else? it won't work untill process is finised! does this
     *  mean we should check in each method if thread is interrupted? then it seems even more hacky!
     */
    public SourceResult getSourceResult(String uri, Query query) throws SourceException, CancelledQueryException {
        this.query = query;
        this.docs = new LinkedList();
        this.typesList = new LinkedList();
        this.readerString = "";
        int queryDepth = query.getDepth();
        Query testQuery = query;
        testQuery.setDepth(1);
        String fullUri = constructQueryUrl(uri, testQuery);
        query.setDepth(queryDepth);
        Reader resultReader = null;
        BufferedReader br = null;
        try {
            Reader reader = executeWebkbQuery(fullUri);
            br = new BufferedReader(reader);
            checkForMultiRdfDocuments(br);
            System.out.println("docs size = " + docs.size());
            if (docs.size() == 0) {
                String webkbError = checkForWebkbErrors(readerString);
                throw new SourceException("WebKB Error: " + webkbError);
            }
            if (resultIsAmbiguous()) {
                Query newQuery = processAmbiguousResultSet();
                return (new SourceResult(false, null, newQuery));
            }
            reader.close();
            resultReader = executeWebkbQuery(constructQueryUrl(uri, query));
        } catch (IOException ioExc) {
            throw new SourceException("Couldn't read input data source for " + fullUri + ", error: " + ioExc.getMessage());
        } catch (ParserException parserExc) {
            throw new SourceException("Error parsing returned RDF data, here is error provided by parser: " + parserExc.getMessage());
        } catch (InterruptedException intExc) {
            throw new CancelledQueryException();
        }
        System.out.println("resultReader = " + resultReader);
        return (new SourceResult(true, resultReader, null));
    }

    /**
     * Get a reader from given url
     */
    private InputStreamReader getInputStreamReader(String uri) throws MalformedURLException, IOException {
        URL url = new URL(uri);
        URLConnection connection = url.openConnection();
        return new InputStreamReader(connection.getInputStream());
    }

    /**
     * construct query string ready to use with webkb
     */
    private String constructQueryUrl(String uri, Query query) {
        WebkbQueryStringConstructor queryConstructor = new WebkbQueryStringConstructor();
        String resultUrl = uri + queryConstructor.getQueryString(query, OntoramaConfig.queryOutputFormat);
        return resultUrl;
    }

    /**
     * execute webkb query
     */
    private Reader executeWebkbQuery(String fullUrl) throws IOException {
        if (OntoramaConfig.DEBUG) {
            System.out.println("fullUrl = " + fullUrl);
        }
        System.out.println("class WebKB2Source, fullUrl = " + fullUrl);
        InputStreamReader reader = getInputStreamReader(fullUrl);
        return reader;
    }

    /**
     * check for errors returned by webkb
     *
     * Check if EITHER of error patterns appear in the document,
     * rather then if both of them have to appear. This is more
     * flexible - if webkb is changes or some other error returns
     * some slightly different patterns - we should still be able to
     * catch it.
     */
    private String checkForWebkbErrors(String doc) {
        String extractedErrorStr = doc;
        int startPatternInd = doc.indexOf(webkbErorrStartPattern);
        int endPatternInd = doc.indexOf(webkbErrorEndPattern);
        if (endPatternInd != -1) {
            extractedErrorStr = extractedErrorStr.substring(0, endPatternInd);
        }
        if (startPatternInd != -1) {
            extractedErrorStr = extractedErrorStr.substring(webkbErorrStartPattern.length());
        }
        return extractedErrorStr;
    }

    /**
     * Read RDF documents into list and build a string that
     * will represent the whole document's data.
     * If the list contains more then one document, the query
     * is ambugious. i.e "cat" can be (big_cat, Caterpillar, true_cat, etc).
     *
     * Result: docs - list of RDF documents
     *         readerString - string holding all data from this reader
     *
     * @todo remove count and debugging print statement
     */
    private void checkForMultiRdfDocuments(BufferedReader br) throws IOException, InterruptedException {
        String token;
        String buf = "";
        String line = br.readLine();
        StringTokenizer st;
        while (line != null) {
            System.out.print(".");
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("Query was cancelled");
            }
            readerString = readerString + line;
            st = new StringTokenizer(line, "<", true);
            while (st.hasMoreTokens()) {
                token = st.nextToken();
                buf = buf + token;
                if (token.equals("/rdf:RDF>")) {
                    docs.add(new String(buf));
                    buf = "";
                }
            }
            buf = buf + "\n";
            line = br.readLine();
        }
    }

    /**
     * Deal with case when result is ambiguous: extract list of choices
     * from the list of received documents and popup dialog box prompting
     * user to make a choice.
     */
    private Query processAmbiguousResultSet() throws ParserException {
        getRootTypesFromStreams();
        Frame frame = OntoRamaApp.getMainFrame();
        String selectedType = ((ontorama.model.graph.Node) typesList.get(0)).getName();
        AmbiguousChoiceDialog dialog = new AmbiguousChoiceDialog(typesList, frame);
        selectedType = dialog.getSelected();
        System.out.println("\n\n\nselectedType = " + selectedType);
        String newTermName = selectedType;
        Query newQuery = new Query(newTermName, this.query.getRelationLinksList());
        newQuery.setDepth(this.query.getDepth());
        return newQuery;
    }

    /**
     * Build list of top/root types extracted from the multiple documents,
     * and build a mapping between types and documents themselfs;
     *
     * The way we do this is: iterate through streams and extract list of types
     * for each stream, than add contents of each list to the global  list of
     * possible query candidates.
     *
     */
    private void getRootTypesFromStreams() throws ParserException {
        Iterator it = docs.iterator();
        while (it.hasNext()) {
            String nextDocStr = (String) it.next();
            StringReader curReader = new StringReader(nextDocStr);
            List curTypesList = getTypesListFromRdfStream(curReader, query.getQueryTypeName());
            for (int i = 0; i < curTypesList.size(); i++) {
                ontorama.model.graph.Node node = (ontorama.model.graph.Node) curTypesList.get(i);
                if (!typesList.contains(node)) {
                    typesList.add(node);
                }
            }
        }
    }

    /**
     * Get list of types that we think user may have meant to search for
     * from the given reader.
     *
     * The way we do this: we parse each reader into iterator of ontology types
     * using corresponding webkb parser, then we go through this iterator and
     * look for types with synonym equals to 'termName' (term name that user
     * searched for).
     *
     * Another way to do this: use rdf parser and do pretty much the same:
     * go through rdf statements that have 'label' propertyr value that
     * equals 'termName'. We use 'label' property because it is describing
     * synonyms.
     *
     * Assumption: we assume that in WebKB2 each ambuguous result has
     * an original search term as a synonym. Otherwise, it is not clear
     * how to extract these 'wanted' terms from the list of ontology terms
     * returned from webkb for each ambuguous choice.
     *
     * @todo  check if this assumption (above) is fair
     *
     */
    private List getTypesListFromRdfStream(Reader reader, String termName) throws ParserException, AccessControlException {
        List typeNamesList = new LinkedList();
        RdfWebkbParser parser = new RdfWebkbParser();
        ParserResult parserResult = parser.getResult(reader);
        List nodesList = parserResult.getNodesList();
        Iterator typesIt = nodesList.iterator();
        while (typesIt.hasNext()) {
            ontorama.model.graph.Node curNode = (ontorama.model.graph.Node) typesIt.next();
            List synonyms = getSynonyms(curNode, parserResult.getEdgesList());
            if (synonyms.contains(termName)) {
                typeNamesList.add(curNode);
            }
        }
        return typeNamesList;
    }

    private List getSynonyms(ontorama.model.graph.Node node, List edgesList) {
        List result = new LinkedList();
        Iterator it = edgesList.iterator();
        while (it.hasNext()) {
            ontorama.model.graph.Edge edge = (ontorama.model.graph.Edge) it.next();
            if (edge.getFromNode().equals(node)) {
                if (edge.getEdgeType().getName().equals(synPropName)) {
                    ontorama.model.graph.Node synonymNode = edge.getToNode();
                    result.add(synonymNode.getName());
                }
            }
        }
        return result;
    }

    /**
     * used for tests
     */
    protected boolean resultIsAmbiguous() {
        if (docs.size() > 1) {
            System.out.println("docs.size = " + docs.size());
            return true;
        }
        return false;
    }

    /**
     * used for tests
     */
    protected int getNumOfChoices() {
        return typesList.size();
    }

    /**
     * used for tests
     */
    protected List getChoicesList() {
        return typesList;
    }
}
