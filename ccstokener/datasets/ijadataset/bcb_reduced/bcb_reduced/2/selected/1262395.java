package seismosurfer.http;

import java.applet.Applet;
import java.applet.AppletContext;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import seismosurfer.data.QuakeClientData;
import seismosurfer.data.QuakeMaxMinData;
import seismosurfer.data.constants.ParameterNames;
import seismosurfer.data.constants.QueryNames;
import seismosurfer.util.Assert;
import seismosurfer.util.SeismoException;
import com.bbn.openmap.util.Debug;

/**
 * A "gateway" for HTTP that encapsulates communication with the server.
 * The communication is done by serializing Java objects 
 * which transfer data through the client and server processes into
 * HTTP packets. This class then acts as an intermediate
 * between the applet classes and the server classes.
 *
 */
public class HttpGateway implements ParameterNames, QueryNames {

    protected static HttpGateway gateway;

    private Applet applet;

    private AppletContext appletCtx;

    /**
     * Constructor is made protected to disallow
     * free instantiation of this class.
     *
     */
    protected HttpGateway() {
    }

    /**
     * Controls the instantiation of this class.
     * 
     * @param applet the applet itself
     */
    public static void init(Applet applet) {
        if (gateway == null) {
            gateway = new HttpGateway();
            Assert.notNull(applet);
            gateway.applet = applet;
            gateway.appletCtx = gateway.applet.getAppletContext();
            Assert.notNull(gateway.appletCtx);
            URLUtil.init(gateway.applet.getCodeBase());
        }
    }

    /**
     * Retrieves a Map with the values/frequencies 
     * given the data source and the query type.
     * 
     * @param queryName the query type
     * @param source the name of the data source
     * @return a Map with the values/frequencies
     */
    public static Map getFrequencies(String queryName, String source) {
        Map freq = null;
        Map parameter = new HashMap(1);
        parameter.put(SOURCE, source);
        Object obj = sendParameters(queryName, parameter);
        if (obj instanceof Map) {
            freq = (Map) obj;
        } else {
            Debug.error("Data returned are not in the expected form.");
            throw new SeismoException("Data returned are not in the expected form.");
        }
        return freq;
    }

    /**
     * Retrieves a list with the data sources.
     * 
     * @return a list with the data sources
     */
    public static List getCatalogSources() {
        List sources = null;
        Object obj = sendParameters(CATALOG_SOURCES, null, URLUtil.getURL(URLUtil.QUERY));
        if (obj instanceof List) {
            sources = (List) obj;
        } else {
            Debug.error("Data returned are not in the expected form.");
            throw new SeismoException("Data returned are not in the expected form.");
        }
        return sources;
    }

    /**
     * Retrieves a list with the catalogs of 
     * seismic data from which the db is updated.
     * 
     * @return a list with the catalogs
     */
    public static List getCatalogs() {
        List catalogs = null;
        Object obj = sendParameters(CATALOGS, null, URLUtil.getURL(URLUtil.QUERY));
        if (obj instanceof List) {
            catalogs = (List) obj;
        } else {
            Debug.error("Data returned are not in the expected form.");
            throw new SeismoException("Data returned are not in the expected form.");
        }
        return catalogs;
    }

    /**
     * Shows a document in a new browser window, 
     * given its url.
     * 
     * @param url the url where the document is located
     * @param target the HTML frame in which the document
     *        will be displayed
     * @see  java.applet.AppletContext#showDocument

     */
    public static void showDocument(String url, String target) {
        try {
            gateway.appletCtx.showDocument(new URL(url), target);
        } catch (MalformedURLException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Shows in a new browser window a web page that 
     * is located on this application`s server.
     * 
     * @param relativeURL the relative to the applet url 
     */
    public static void showInternalWebPage(String relativeURL) {
        showDocument(URLUtil.getAppletURL() + relativeURL, "_blank");
    }

    /**
     * Shows in a new browser window a web page that 
     * is not located on this application`s server.
     * 
     * @param url the url of the web page
     */
    public static void showExternalWebPage(String url) {
        showDocument(url, "_blank");
    }

    /**
     * Shows a web page, to upload a document or url related to an earthquake, in
     * a browser window.
     * 
     * @param quakeid the id of the earthquake
     * @param type DOC or URL
     */
    public static void showDocument(long quakeid, int type) {
        String queryString = "?type=" + type + "&quakeid=" + quakeid;
        if (type == DOC) {
            showDocument(URLUtil.getURL(URLUtil.UPLOADFORM) + queryString, "Upload Document");
        } else if (type == URL) {
            showDocument(URLUtil.getURL(URLUtil.UPLOADFORM) + queryString, "Upload URL");
        }
    }

    /**
     * Shows a web page with a list of all the documents
     * related to an earthquake.
     * 
     * @param quakeid the id of the earthquake
     */
    public static void showDocument(long quakeid) {
        String queryString = "?quakeid=" + quakeid;
        showDocument(URLUtil.getURL(URLUtil.QUAKE_DOCS) + queryString, "Related Documents");
    }

    /**
     * Handles the actual communication with the
     * server.
     * 
     * @param queryName the name of the query
     * @param parameters the parameters of the query
     * @param url the url of the server program
     * @return an Object with the results
     */
    protected static Object sendParameters(String queryName, Map parameters, URL url) {
        Object obj = null;
        try {
            URLConnection con = getURLConnection(url);
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream(512);
            ObjectOutputStream out = new ObjectOutputStream(byteStream);
            out.writeObject(queryName);
            out.writeObject(parameters);
            out.flush();
            con.setRequestProperty("Content-Length", String.valueOf(byteStream.size()));
            con.setRequestProperty("Content-Type", "application/x-java-serialized-object");
            byteStream.writeTo(con.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(con.getInputStream());
            try {
                obj = in.readObject();
            } catch (ClassNotFoundException e) {
                Debug.error(e.getMessage());
                throw new SeismoException(e);
            }
            in.close();
        } catch (IOException e) {
            Debug.error(e.getMessage());
            throw new SeismoException(e);
        }
        return obj;
    }

    protected static Object sendParameters(String queryName, Map parameters) {
        return sendParameters(queryName, parameters, URLUtil.getURL(URLUtil.QUERY));
    }

    /**
     * Retrieves a list with earthquake related data.
     * 
     * @param quakeID the id of the earthquake
     * @param queryName the name of the query.
     * @return a list with earthquake related data
     */
    protected static List getListForQuake(long quakeID, String queryName) {
        List list = null;
        Map parameter = new HashMap(1);
        parameter.put(QUAKEID, new Long(quakeID));
        Object obj = sendParameters(queryName, parameter);
        if (obj instanceof List) {
            System.out.println("\n" + "List size :" + ((List) obj).size() + "\n");
            list = (List) obj;
        } else {
            System.out.println("Server returned wrong Type!!!");
        }
        return list;
    }

    /**
     * Retrieves a list of magnitude objects
     * given the related earthquake.
     * 
     * @param quakeID the id of the earthquake
     * @return a list of magnitude objects
     */
    public static List getMagnitudesForQuake(long quakeID) {
        return getListForQuake(quakeID, QueryNames.MAG);
    }

    /**
     * Retrieves a list of macroseismic objects
     * given the related earthquake.
     * 
     * @param quakeID the id of the earthquake
     * @return a list of macroseismic objects
     */
    public static List getMacroseismicForQuake(long quakeID) {
        return getListForQuake(quakeID, QueryNames.MACRO);
    }

    /**
     * Retrieves a list of sites based on the given criteria
     * (parameters).
     * 
     * @param parameters the criteria used to retrieve subset
     *        of the sites
     * @return a list of sites
     */
    public static List getSites(Map parameters) {
        List sites = null;
        Object obj = sendParameters(QueryNames.SITES, parameters);
        if (obj instanceof List) {
            sites = (List) obj;
        } else {
            System.out.println("Server returned wrong Type!!!");
        }
        System.out.println("\n" + sites.size() + "\n");
        return sites;
    }

    /**
     * Retrieves a list of earthquake data and loads them
     * in the QuakeClientData for later reference.
     * 
     * @param queryName the name of the query
     * @param parameters the name/value pairs of the parameters
     */
    public static void loadQuakeClientData(String queryName, Map parameters) {
        QuakeClientData.clear();
        Object obj = sendParameters(queryName, parameters);
        if (obj instanceof List) {
            QuakeClientData.setQuakeResults((List) obj);
        } else {
            System.out.println("Server returned wrong Type!!!");
        }
        int size = QuakeClientData.getQuakeResults().size();
        System.out.println("\n" + size + "\n");
    }

    /**
     * Makes a connection with a web server to a specified
     * url to read and send data. 
     * 
     * @param url
     * @return a URLConnection with the web server
     */
    protected static URLConnection getURLConnection(URL url) {
        URLConnection connection = null;
        try {
            connection = url.openConnection();
            connection.setUseCaches(false);
            connection.setDefaultUseCaches(true);
            connection.setDoInput(true);
            connection.setDoOutput(true);
            return connection;
        } catch (IOException e) {
            throw new SeismoException(e);
        }
    }

    /**
     * Gets a list of the component names 
     * that should be deactivated in UI.
     * 
     * @return a list of component names
     */
    public static List getConstraints() {
        List constraints = null;
        Object obj = sendParameters(QueryNames.CONSTRAINTS, null);
        if (obj instanceof List) {
            constraints = (List) obj;
        } else {
            System.out.println("Server returned wrong Type!!!");
        }
        for (Iterator i = constraints.iterator(); i.hasNext(); ) {
            System.out.println("Constraints: " + (String) i.next());
        }
        return constraints;
    }

    /**
     * Gets an array of the component names 
     * that should be deactivated in UI.
     * 
     * @return an array of component names
     */
    public static String[] getConstraintsNames() {
        List constr = getConstraints();
        int constrCount = constr.size();
        Object[] constrObj = constr.toArray();
        String[] constrNames = new String[constrCount];
        for (int i = 0; i < constrCount; i++) {
            constrNames[i] = (String) constrObj[i];
        }
        return constrNames;
    }

    /**
     * Sets the min/max values in the QuakeMaxMinData object.
     *
     */
    public static void loadQuakeMaxMinData() {
        Map minMaxValues = null;
        Object obj = sendParameters(QueryNames.MINMAX, null);
        if (obj instanceof Map) {
            minMaxValues = (Map) obj;
        } else {
            System.out.println("Server returned wrong Type!!!");
        }
        QuakeMaxMinData.setMinMaxValues(minMaxValues);
    }
}
