package org.projectopen.rest;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringBufferInputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.TreeMap;
import java.util.regex.Pattern;
import org.projectopen.util.DomPrinter;
import org.projectopen.util.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import com.sun.org.apache.xerces.internal.parsers.DOMParser;
import sun.misc.BASE64Encoder;

@SuppressWarnings({ "unchecked", "deprecation" })
public class RESTClient implements Logger {

    private int userId = 0;

    private Properties properties = null;

    private static RESTClient defaultInstance = null;

    private Logger parent = null;

    private static String clientRESTVersion = "1.4";

    private String serverRESTVersion = null;

    public RESTClient(Logger starterApp) {
        super();
        parent = starterApp;
        RESTClient.defaultInstance = this;
        serverRESTVersion = restReadServerRESTVersion();
        this.checkRESTVersionCompatibility(serverRESTVersion);
    }

    /**
	 * Check if we can deal with the REST version on
	 * the server.
	 */
    private void checkRESTVersionCompatibility(String serverRESTVersion) {
        Pattern p = Pattern.compile("\\.");
        String serverPieces[] = p.split(serverRESTVersion);
        int len = serverPieces.length;
        String clientPieces[] = p.split(clientRESTVersion);
        int clientPiecesLen = clientPieces.length;
        if (clientPiecesLen < len) {
            len = clientPiecesLen;
        }
        boolean ok = true;
        int serverVer = Integer.parseInt(serverPieces[0]);
        int clientVer = Integer.parseInt(clientPieces[0]);
        if (clientVer != serverVer) {
            ok = false;
        }
        serverVer = Integer.parseInt(serverPieces[1]);
        clientVer = Integer.parseInt(clientPieces[1]);
        if (serverVer < clientVer) {
            ok = false;
        }
        if (!ok) {
            this.logMessage(Logger.FATAL, "REST Protocol Mismatch", "The server doesn't provide the required version of the REST interface.", "REST V" + serverRESTVersion + " incompatible with REST V" + clientRESTVersion);
            System.exit(0);
        } else {
            this.logMessage(Logger.INFO, "REST Protocol Match", "The client and server REST protocol versions are compatible. Server: " + serverRESTVersion + ", Client: " + clientRESTVersion, "");
        }
    }

    /**
	 * Return a default RESTClient.
	 * Most applications in this world will only need one connection
	 * to a REST server...
	 * @return	A default RESTClient
	 */
    public static RESTClient defaultInstance() {
        if (null == defaultInstance) {
            System.out.println("ERROR: default instance not yet initialized, so we can't write out error. Please call RESTClient.setupDefaultInstance(..) first.");
            defaultInstance = new RESTClient(null);
        }
        return defaultInstance;
    }

    /**
	 * Determine the parameters necessary to connect to the
	 * server. Properties are initialized with the a default
	 * login to the ]po[ main demo server.
	 * User can modify these properties and store them in the
	 * local application folder.
	 * 
	 * @return A properties object with the configuration. 
	 */
    public Properties getProperties() {
        if (properties == null) {
            properties = new Properties();
            properties.put("server", "http://demo.project-open.net");
            properties.put("email", "bbigboss@tigerpond.com");
            properties.put("password", "ben");
            try {
                FileInputStream in = new FileInputStream("config.properties");
                properties.load(in);
                in.close();
            } catch (Exception e) {
            }
            logMessage(Logger.INFO, "RESTClient.getProperties()", "Loaded properties from 'config.properties'", properties.toString());
        }
        return properties;
    }

    /**
	 * Store modified properties to the local application directory.
	 */
    public void storeProperties() {
        logMessage(Logger.INFO, "RESTClient.storeProperties()", "Storing properties to 'config.properties'", properties.toString());
        FileOutputStream out;
        try {
            out = new FileOutputStream("config.properties");
            properties.store(out, "--- Parameters to access a ]project-open[ server ---");
            out.close();
        } catch (Exception e) {
            logMessage(Logger.ERROR, "RESTClient.storeProperties()", "Error storing properties to 'config.properties'", e.toString());
        }
    }

    /**
	 * Delete all cached information.
	 * This is necessary when changing the configuration 
	 * or when the next day has started if the application
	 * is running at night.
	 */
    public void flushCache() {
        userId = 0;
        properties = null;
    }

    /**
	 * Accepts log messages from the REST interactions 
	 * and GUI elements and logs them 1) to the DebugPanel
	 * and 2) to the TrayIconStarter, where ERROR and FATAL
	 * messages are displayed to the user.
	 */
    public void logMessage(int level, String domain, String message, String details) {
        if (null != parent) {
            parent.logMessage(level, domain, message, details);
        }
        String levelString = "undefined";
        switch(level) {
            case Logger.DEBUG:
                levelString = "DEBUG";
                break;
            case Logger.INFO:
                levelString = "INFO";
                break;
            case Logger.WARNING:
                levelString = "WARNING";
                break;
            case Logger.ERROR:
                levelString = "ERROR";
                break;
            case Logger.FATAL:
                levelString = "FATAL";
                break;
        }
        System.out.println(levelString + ": " + domain + ": " + message + ": " + details);
    }

    /**
	 * Determine the ]po[ user_id of the current user.
	 * Cache the result because we need the user_id frequently.
	 * @return	The ]po[ user_id of the current user.
	 */
    public int getMyUserId() {
        if (0 == userId) {
            String email = properties.getProperty("email");
            ProjopObject user = restReadUserFromEmail(email);
            if (null != user) {
                userId = Integer.parseInt(user.get("user_id"));
            }
        }
        return userId;
    }

    /**
	 * Launch a Web browser in the user's desktop with a 
	 * URL including his password for automatic login.
	 */
    public void restBrowserLogin() {
        logMessage(Logger.INFO, "RESTClient.restBrowserLogin()", "Launch a Web browser.", "");
        String server = getProperties().getProperty("server");
        String email = getProperties().getProperty("email");
        String password = getProperties().getProperty("password");
        try {
            URI uri = new java.net.URI(server + "/intranet/auto-login?email=" + email + "&password=" + password + "&url=/intranet-timesheet2/hours/index");
            java.awt.Desktop.getDesktop().browse(uri);
        } catch (Exception e1) {
            System.err.println(e1.getMessage());
        }
    }

    ;

    /**
	 * Perform a REST HTTP request.
	 * 
	 * @param method	Either "GET" or "POST"
	 * @param urlPath	The path part of the URL including a leading slash, 
	 * 					for example "/intranet-rest/index"
	 * @param body		Input stream for the XML data to send to the server 
	 * 					for POST operations. Ignored for GET operations.
	 * @return			A XML document with the parsed REST response.
	 * 					Returns NULL in case of an error.
	 */
    public Document httpRequest(String method, String urlPath, InputStream body) {
        String server = getProperties().getProperty("server");
        String email = getProperties().getProperty("email");
        String password = getProperties().getProperty("password");
        HttpURLConnection connection;
        int httpResponseCode = 0;
        URL url = null;
        try {
            url = new URL(server + urlPath);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        logMessage(Logger.DEBUG, "HTTP Interaction", "-> " + method + ": " + url, "");
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method);
        } catch (IOException e1) {
            logMessage(Logger.ERROR, " Connection Error", "Unable to connect to server " + server, "");
            return null;
        }
        BASE64Encoder encoder = new BASE64Encoder();
        String encodedCredential = encoder.encode((email + ":" + password).getBytes());
        connection.setRequestProperty("Authorization", "BASIC " + encodedCredential);
        byte buffer[] = new byte[8192];
        int read = 0;
        if (body != null && method != "DELETE") {
            try {
                connection.setDoOutput(true);
                OutputStream output = connection.getOutputStream();
                while ((read = body.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                }
            } catch (IOException e2) {
                logMessage(Logger.ERROR, "Connection Error", "Unable to write to connection to server " + server, "");
                return null;
            }
        }
        try {
            connection.connect();
            httpResponseCode = connection.getResponseCode();
        } catch (IOException e2) {
            logMessage(Logger.ERROR, "Connection Error", "Unable to connect to server " + server, "");
            return null;
        }
        logMessage(Logger.DEBUG, "HTTP Interaction", "<- httpResponseCode=" + httpResponseCode + " from " + url, "");
        InputStream responseBodyStream = null;
        if (200 == httpResponseCode) {
            try {
                responseBodyStream = connection.getInputStream();
            } catch (IOException e) {
                logMessage(Logger.ERROR, "Connection Error", "Unable to read from server", "");
                return null;
            }
        } else {
            try {
                responseBodyStream = connection.getErrorStream();
                char charBuffer[] = new char[8192];
                StringBuilder errorXmlString = new StringBuilder();
                Reader in = new InputStreamReader(responseBodyStream, "UTF-8");
                do {
                    read = in.read(charBuffer, 0, buffer.length);
                    if (read > 0) {
                        errorXmlString.append(charBuffer, 0, read);
                    }
                } while (read >= 0);
                logMessage(Logger.ERROR, "Application Error", "The REST server reports an application error.", errorXmlString.toString());
                responseBodyStream = new StringBufferInputStream(errorXmlString.toString());
            } catch (IOException e) {
                logMessage(Logger.ERROR, "Connection Error", "Unable to read from server " + server, "");
                return null;
            }
        }
        DOMParser parser = new DOMParser();
        try {
            parser.setFeature("http://xml.org/sax/features/validation", false);
            parser.setFeature("http://apache.org/xml/features/validation/schema", false);
            InputSource source = new InputSource(responseBodyStream);
            parser.parse(source);
        } catch (Exception e1) {
            logMessage(Logger.ERROR, "Application Error", "Unable to parse the reply from server " + server, "");
            return null;
        }
        Document dom = parser.getDocument();
        if (httpResponseCode != 200) {
            Element docEle = dom.getDocumentElement();
            DomPrinter.walk(docEle);
            httpError(httpResponseCode, url, docEle);
            dom = null;
        }
        return dom;
    }

    /**
	 * Generic "list" request towards the REST server that will 
	 * return a list of objects that match the specified SQL query string.
	 * @param objectType	]po[ object type ("im_project", "im_hour", "user", ...)
	 * @param query			A SQL where clause with fields matching the object_type.
	 * 						For example, the "user" object defines the fields "email",
	 * 						"first_names" etc.
	 * @return				A list of ProjopObject who have matched the SQL query.
	 */
    public List restListObjectsFromQuery(String objectType, String query, boolean readFullObjectP) {
        String urlPath;
        Document dom = null;
        String encodedQuery = null;
        List myObjects = new ArrayList();
        logMessage(Logger.INFO, "RESTClient.restListObjectsFromQuery(" + objectType + "," + query + ")", "Query REST server for objects matching the SQL where clause", "");
        try {
            encodedQuery = URLEncoder.encode(query, "UTF-8");
            urlPath = "/intranet-rest/" + objectType + "?format=xml&query=" + encodedQuery;
            dom = httpRequest("GET", urlPath, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (dom == null) {
            return null;
        }
        DomPrinter.walk(dom);
        Element docEle = dom.getDocumentElement();
        NodeList nl = docEle.getElementsByTagName("object_id");
        if (nl != null && nl.getLength() > 0) {
            for (int i = 0; i < nl.getLength(); i++) {
                Element el = (Element) nl.item(i);
                String objectIdString = el.getAttribute("id");
                int objectId = Integer.parseInt(objectIdString);
                ProjopObject o;
                if (readFullObjectP) {
                    o = this.restReadObjectFromId(objectType, objectId);
                } else {
                    o = new ProjopObject(objectType);
                    o.setObjectId(objectId);
                }
                myObjects.add(o);
            }
        }
        logMessage(Logger.INFO, "RESTClient.restListObjectsFromQuery(" + objectType + "," + query + ")", "Query REST server for objects matching the SQL where clause", myObjects.toString());
        return myObjects;
    }

    /**
	 * The server has returned a reply like this one:
	 * <error>
	 *	<http_status>403</http_status>
	 *	<http_status_message>Forbidden: The request is understood, but it has been refused.  An accompanying error message will explain why.</http_status_message>
	 *	<request>/intranet-reporting/view?format=xml&report%5fcode=rest%5fmy%5ftimesheet%5fprojects</request>
	 *	<message>The current user doesn't have the right to see this report.'</message>
	 * </error>
	 */
    protected void httpError(int httpResponseCode, URL url, Element docEle) {
        Integer httpStatus = ProjopObject.getIntValue(docEle, "http_status");
        String httpStatusMessage = ProjopObject.getTextValue(docEle, "http_status_message");
        String httpMessage = ProjopObject.getTextValue(docEle, "message");
        String urlQuery = url.getQuery();
        if (urlQuery == null) {
            urlQuery = "";
        } else {
            urlQuery = "/" + urlQuery;
        }
        String details = "" + "Status: " + httpStatus + "\nURL: " + url.getPath() + urlQuery;
        logMessage(Logger.ERROR, "Application Error", httpStatusMessage + "\n" + httpMessage, details);
    }

    /**
	 * Query the REST server for the values of a particular object
	 * identified by an objectType and an objectId.
	 * @param objectType	]po[ object type ("im_project", "user", "im_hour", ...)
	 * @param objectId		]po[ object ID
	 * @return				ProjopObject filled with the object's information
	 * 						or NULL in case of an error.
	 */
    public ProjopObject restReadObjectFromId(String objectType, int objectId) {
        Document dom = null;
        ProjopObject o = new ProjopObject(objectType);
        o.setObjectId(objectId);
        logMessage(Logger.INFO, "RESTClient.restReadObjectFromId(" + objectType + "," + objectId + ")", "Retreive a specific object", "");
        try {
            String urlPath = "/intranet-rest/" + objectType + "/" + objectId + "?format=xml";
            dom = this.httpRequest("GET", urlPath, null);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        Element docEle = dom.getDocumentElement();
        NodeList nl = docEle.getChildNodes();
        if (nl != null && nl.getLength() > 0) {
            for (int i = 0; i < nl.getLength(); i++) {
                Node el = (Node) nl.item(i);
                int type = el.getNodeType();
                if (type != Node.ELEMENT_NODE) {
                    continue;
                }
                String value = el.getTextContent();
                String name = el.getNodeName();
                o.set(name, value);
            }
        }
        ProjopObjectType.getObjectType(objectType).getInstances().put(new Integer(objectId), o);
        logMessage(Logger.INFO, "RESTClient.restReadObjectFromId(" + objectType + "," + objectId + ")", "Retreive a specific object", o.toString());
        return o;
    }

    /**
	 * This procedure accepts a newly setup ProjopObject that
	 * doesn't yet contain an objectId.
	 * It uses the generic "Create" POST operation to create 
	 * a new object on the ]po[ server and stores the returned
	 * objectId in the ProjopObject.
	 * Creating new ]po[ objects requires a number of parameters
	 * that depends on the object type. Please consult the ]po[
	 * documentation for details on these required fields.
	 * 
	 * @param objectType	The type of the new object to be created
	 * @param vars			A Hashtable with variable-value pairs to
	 * 						be sent to the server
	 * @return				Returns the objectId of the new ]po[ object.
	 */
    public int restCreateObject(ProjopObject object) {
        Document dom = null;
        String xmlString = object.toXMLString();
        String objectType = object.getObjectType();
        String urlPath = "/intranet-rest/" + objectType;
        try {
            StringBuffer buf = new StringBuffer(xmlString);
            ByteArrayInputStream bis = new ByteArrayInputStream(buf.toString().getBytes("UTF-8"));
            dom = httpRequest("POST", urlPath, bis);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (dom != null) {
            Element docEle = dom.getDocumentElement();
            DomPrinter.walk(docEle);
            int type = docEle.getNodeType();
            if (type != Node.ELEMENT_NODE) {
                RESTClient.defaultInstance().logMessage(Logger.ERROR, "HTTP Interaction", "Received bad HTTP response", docEle.toString());
                return 0;
            }
            String value = docEle.getTextContent();
            String name = docEle.getNodeName();
            if ("object_id" == name) {
                object.setObjectId(Integer.parseInt(value));
            }
        }
        return 0;
    }

    /**
	 * Retrieve all information about a ]po[ specific user, identified
	 * by the user's email (which is unique in ]po[).
	 *   
	 * @param email The email of a ]po[ user
	 * @return a ProjopObject of type "user" with everything about the user.
	 */
    public ProjopObject restReadUserFromEmail(String email) {
        logMessage(Logger.INFO, "RESTClient.restReadUserFromEmail(" + email + ")", "Query the REST server for a user with the specified email", "");
        List objects = restListObjectsFromQuery("user", "email = '" + email + "'", true);
        if (objects == null) {
            return null;
        }
        if (objects.isEmpty()) {
            return null;
        }
        ProjopObject o = ((ProjopObject) objects.get(0));
        logMessage(Logger.INFO, "RESTClient.restReadUserFromEmail(" + email + ")", "Query the REST server for a user with the specified email", o.toString());
        return o;
    }

    /**
	 * Determines the server's REST version.
	 * Major changes in the version digit indicate incompatibilities.
	 * Minor changes indicated protocol additions.
	 * 
	 * @return The server's REST protocol version
	 */
    public String restReadServerRESTVersion() {
        Document dom = null;
        logMessage(Logger.INFO, "RESTClient.restReadVersion", "Get the server's REST version", "");
        try {
            String urlPath = "/intranet-rest/version?format=xml";
            dom = this.httpRequest("GET", urlPath, null);
        } catch (Exception e) {
            logMessage(Logger.ERROR, "RESTClient.restReadVersion", "Error connecting to server.", e.toString());
            return "";
        }
        Element docEle = dom.getDocumentElement();
        NodeList nl = docEle.getChildNodes();
        if (nl != null && nl.getLength() > 0) {
            for (int i = 0; i < nl.getLength(); i++) {
                Node el = (Node) nl.item(i);
                int type = el.getNodeType();
                if (type != Node.TEXT_NODE) {
                    continue;
                }
                String value = el.getTextContent().trim();
                if (null != value && "" != value) {
                    return value;
                }
            }
        }
        return "";
    }

    /**
	 * Query the REST server for the list of available groups in the system.
	 * @return	List of ProjopObjects representing the current ]po[ groups.
	 */
    public List restListGroups() {
        Document dom = null;
        List groups = new ArrayList();
        try {
            String urlPath = "/intranet-reporting/view?format=xml&report_code=groups";
            dom = this.httpRequest("GET", urlPath, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Element docEle = dom.getDocumentElement();
        NodeList nl = docEle.getElementsByTagName("value");
        if (nl != null && nl.getLength() > 0) {
            for (int i = 0; i < nl.getLength(); i++) {
                Element el = (Element) nl.item(i);
                ProjopObject g = ProjopObject.objectFromXML(el);
                groups.add(g);
            }
        }
        logMessage(Logger.INFO, "RESTClient.restListGroups()", "List all groups in the ]po[ system", groups.toString());
        return groups;
    }

    /**
	 * Retreive the values of a DynField widget.
	 * @param widgetName	The name of a DynField Widget
	 * @return				A List of ComboBoxValues representing
	 * 						the options for the widget.
	 */
    public List restListDynfieldWidgetValues(String widgetName) {
        TreeMap widgets = ProjopObjectType.getObjectType("im_dynfield_widget").getInstances();
        Iterator iter = widgets.values().iterator();
        ProjopObject widget = null;
        while (iter.hasNext()) {
            ProjopObject attr = (ProjopObject) iter.next();
            if (widgetName.equals(attr.get("widget_name"))) {
                widget = attr;
            }
        }
        if (null == widget) {
            this.logMessage(Logger.ERROR, "Dynfield Widgets", "Didn't find widget with name " + widgetName, "");
            return null;
        }
        int widgetId = Integer.parseInt(widget.get("widget_id"));
        Document dom = null;
        List groups = new ArrayList();
        try {
            String urlPath = "/intranet-rest/dynfield-widget-values?widget_id=" + widgetId + "&format=xml";
            dom = this.httpRequest("GET", urlPath, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Element docEle = dom.getDocumentElement();
        NodeList nl = docEle.getElementsByTagName("value");
        if (nl != null && nl.getLength() > 0) {
            for (int i = 0; i < nl.getLength(); i++) {
                Element el = (Element) nl.item(i);
                int type = el.getNodeType();
                DomPrinter.walk(el);
                if (type != Node.ELEMENT_NODE) {
                    continue;
                }
                String value = el.getTextContent();
                String key = el.getAttribute("key");
                int keyInt = Integer.parseInt(key);
                ComboBoxValue v = new ComboBoxValue(value, keyInt);
                groups.add(v);
            }
        }
        logMessage(Logger.INFO, "RESTClient.restListGroups()", "List all groups in the ]po[ system", groups.toString());
        return groups;
    }

    /**
	 * Query the REST server for the list of categories of the 
	 * given categoy type.
	 * @param	categoryType	A ]po[ category, for example
	 * 			"Intranet Project Status".
	 * @return	List of Category ProjopObjects representing the 
	 * 			category.
	 */
    public List restListCategoriesForType(String categoryType) {
        Document dom = null;
        List categories = new ArrayList();
        try {
            String categoryTypeEncoded = URLEncoder.encode("'" + categoryType + "'", "UTF-8");
            String urlPath = "/intranet-reporting/view?format=xml&report_code=rest_category_type&category_type=" + categoryTypeEncoded;
            dom = this.httpRequest("GET", urlPath, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Element docEle = dom.getDocumentElement();
        ProjopObjectType t = (ProjopObjectType) ProjopObjectType.getObjectType("im_category");
        NodeList nl = docEle.getElementsByTagName("row");
        if (nl != null && nl.getLength() > 0) {
            for (int i = 0; i < nl.getLength(); i++) {
                Element el = (Element) nl.item(i);
                DomPrinter.walk(el);
                ProjopObject g = ProjopCategory.categoryFromCategoryTypesXML(el);
                t.getInstances().put(g.getObjectId(), g);
                categories.add(g);
            }
        }
        logMessage(Logger.INFO, "RESTClient.restListCategories()", "List a category type", categories.toString());
        return categories;
    }

    /**
	 * Query the REST server for a list of currently logged hours.
	 * 
	 * @return	A list of ProjopHour objects representing the hours
	 * 			logged today.
	 */
    public List restListMyTimesheetProjects() {
        String urlPath;
        Document dom = null;
        List myProjects = new ArrayList();
        try {
            urlPath = "/intranet-reporting/view?format=xml&report_code=rest_my_timesheet_projects";
            dom = httpRequest("GET", urlPath, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Element docEle = dom.getDocumentElement();
        NodeList nl = docEle.getElementsByTagName("row");
        if (nl != null && nl.getLength() > 0) {
            for (int i = 0; i < nl.getLength(); i++) {
                Element el = (Element) nl.item(i);
                ProjopProject p = ProjopProject.projectFromMyTimesheetProjectsXML(el);
                myProjects.add(p);
            }
        }
        logMessage(Logger.INFO, "RESTClient.restListMyTimesheetProjects()", "List all project to which the current user can log hours.", myProjects.toString());
        return myProjects;
    }

    public void restUpdateHour(ProjopHour h) {
        Document dom = null;
        boolean createNewObject = false;
        String httpOperation = "POST";
        String hourIdString = h.get("hour_id");
        String xmlString = h.toXMLString();
        String urlPath = null;
        if (hourIdString == null) {
            urlPath = "/intranet-rest/im_hour";
            createNewObject = true;
            logMessage(Logger.INFO, "RESTClient.restUpdateHour(hour)", "Create a new Hour object.", h.toString());
        } else {
            urlPath = "/intranet-rest/im_hour/" + hourIdString;
            createNewObject = false;
            logMessage(Logger.INFO, "RESTClient.restUpdateHour(hour)", "Update an existing Hour object.", h.toString());
        }
        try {
            StringBuffer buf = new StringBuffer(xmlString);
            ByteArrayInputStream bis = new ByteArrayInputStream(buf.toString().getBytes("UTF-8"));
            dom = httpRequest(httpOperation, urlPath, bis);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (createNewObject && dom != null) {
            Element docEle = dom.getDocumentElement();
            DomPrinter.walk(docEle);
            int type = docEle.getNodeType();
            if (type != Node.ELEMENT_NODE) {
                return;
            }
            String value = docEle.getTextContent();
            String name = docEle.getNodeName();
            if ("object_id" == name) {
                h.set("hour_id", value);
            }
        }
        return;
    }

    /**
	 * Query the REST server the hours logged by the current
	 * user today.  
	 * @return	A list of ProjectHour objects
	 */
    public List restListHours() {
        Document dom = null;
        List list = new ArrayList();
        try {
            String urlPath = "/intranet-reporting/view?format=xml&report_code=rest_my_hours";
            dom = httpRequest("GET", urlPath, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (null == dom) {
            return list;
        }
        Element docEle = dom.getDocumentElement();
        NodeList nl = docEle.getElementsByTagName("row");
        if (nl != null && nl.getLength() > 0) {
            for (int i = 0; i < nl.getLength(); i++) {
                Element el = (Element) nl.item(i);
                ProjopHour h = ProjopHour.hourFromMyHoursXML(el);
                list.add(h);
            }
        }
        logMessage(Logger.INFO, "RESTClient.restListHours()", "Query the hours logged today by the current user.", list.toString());
        return list;
    }
}
