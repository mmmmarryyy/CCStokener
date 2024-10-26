package com.tensegrity.palo.xmla;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import com.tensegrity.palojava.PaloException;

public class XMLAClient {

    public static final boolean IGNORE_VARIABLE_CUBES = Boolean.getBoolean("xmla_ignoreVariableCubes");

    URL url;

    boolean connOpen;

    HttpURLConnection urlConnection;

    private String requestType;

    DocumentBuilder builder;

    private static boolean verbose = false;

    private static boolean shortVerbose = false;

    private static boolean debug = false;

    private static boolean outputToFile = false;

    private static boolean shortOutputToFile = false;

    private static boolean disallowDebug = false;

    private static BufferedWriter writer = null;

    private String username;

    private String password;

    private String server;

    private String service;

    private XMLAServerInfo[] connections;

    private boolean globalIsSAP = false;

    private boolean globalIsSAPSet = false;

    public XMLAClient(String server, String service, String user, String pass) throws ParserConfigurationException {
        if (writer == null && (outputToFile || shortOutputToFile)) {
            try {
                writer = new BufferedWriter(new FileWriter("C:\\Users\\PhilippBouillon\\Data\\SoapRequests.txt"));
            } catch (IOException e) {
            }
        }
        StringBuffer urlString = new StringBuffer(server);
        if (!server.endsWith("/")) {
            urlString.append("/");
        }
        if (service.startsWith("/")) {
            urlString.append(service.substring(1));
        } else {
            urlString.append(service);
        }
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            connOpen = false;
            this.username = user;
            this.password = pass;
            this.server = server;
            this.service = service;
            url = new URL(urlString.toString());
            builder = factory.newDocumentBuilder();
            connections = loadConnections();
        } catch (MalformedURLException e) {
            String temp = urlString.toString().trim().toLowerCase();
            int pos = 0;
            if (temp.startsWith("http") && temp.length() > 4) {
                temp = temp.substring(4);
            }
            do {
                char c = temp.charAt(pos);
                if (c == ':' || c == '/' || c == '\\') {
                    pos++;
                } else {
                    break;
                }
            } while (pos < temp.length());
            if (pos < temp.length()) {
                temp = temp.substring(pos);
            }
            temp = "http://" + temp;
            try {
                url = new URL(temp);
                builder = factory.newDocumentBuilder();
                connections = loadConnections();
            } catch (MalformedURLException e2) {
            }
        }
    }

    public static String getTextFromDOMElement(org.w3c.dom.Node node) {
        String retVal = "";
        if (node.getNodeType() != Node.ELEMENT_NODE) return "";
        org.w3c.dom.NodeList nodeList = node.getChildNodes();
        int i;
        int n = nodeList.getLength();
        if (n > 1) {
            for (i = 0; i < n; i++) {
                if (nodeList.item(i).getNodeType() == Node.TEXT_NODE) {
                    retVal += nodeList.item(i).getNodeValue();
                }
            }
        } else {
            if (n > 0) {
                retVal = nodeList.item(0).getNodeValue();
            } else {
                retVal = "";
            }
        }
        return retVal;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getServer() {
        return server;
    }

    public String getService() {
        return service;
    }

    private String getRequestType() {
        return requestType == null ? "DISCOVER_DATASOURCES" : requestType;
    }

    private void setRequestType(String _requestType) {
        requestType = _requestType;
    }

    private void setDiscoverMimeHeaders() {
        urlConnection.setRequestProperty("SOAPAction", "\"urn:schemas-microsoft-com:xml-analysis:Discover\"");
        urlConnection.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
        urlConnection.setRequestProperty("Accept", "application/soap+xml, application/dime, multipart/related, text/*");
    }

    private void setExecuteMimeHeaders() {
        urlConnection.setRequestProperty("SOAPAction", "\"urn:schemas-microsoft-com:xml-analysis:Execute\"");
        urlConnection.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
        urlConnection.setRequestProperty("Accept", "application/soap+xml, application/dime, multipart/related, text/*");
    }

    private boolean init() {
        if (connOpen) return true;
        try {
            urlConnection = (HttpURLConnection) url.openConnection();
            String encoding = new sun.misc.BASE64Encoder().encode((username + ":" + password).getBytes());
            urlConnection.setRequestProperty("Authorization", "Basic " + encoding);
            urlConnection.setRequestMethod("POST");
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(true);
            connOpen = true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public synchronized void disconnect() {
        if (urlConnection != null) {
            try {
                urlConnection.disconnect();
            } catch (Exception e) {
            }
        }
        connOpen = false;
    }

    public static String xmlEncodeString(String orig) {
        if (orig == null) {
            return "";
        }
        char[] chars = orig.toCharArray();
        boolean needsEncoding = false;
        search: for (int i = 0; i < chars.length; i++) {
            switch(chars[i]) {
                case '&':
                case '"':
                case '\'':
                case '<':
                case '>':
                    needsEncoding = true;
                    break search;
            }
        }
        if (!needsEncoding) return orig;
        StringBuffer strBuf = new StringBuffer();
        for (int i = 0; i < chars.length; i++) {
            switch(chars[i]) {
                case '&':
                    strBuf.append("&amp;");
                    break;
                case '\"':
                    strBuf.append("&quot;");
                    break;
                case '\'':
                    strBuf.append("&apos;");
                    break;
                case '<':
                    strBuf.append("&lt;");
                    break;
                case '\r':
                    strBuf.append("&#xd;");
                    break;
                case '>':
                    strBuf.append("&gt;");
                    break;
                default:
                    if (((int) chars[i]) > 127) {
                        strBuf.append("&#");
                        strBuf.append((int) chars[i]);
                        strBuf.append(";");
                    } else {
                        strBuf.append(chars[i]);
                    }
            }
        }
        return strBuf.toString();
    }

    public synchronized Document execute(String mdx, XMLAExecuteProperties properties) throws IOException {
        if (!init()) {
            disconnect();
            return null;
        }
        StringBuffer request = new StringBuffer("");
        request.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n<SOAP-ENV:Envelope" + "\n  xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\"" + "\n  SOAP-ENV:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">" + "\n  <SOAP-ENV:Body>" + "\n    <Execute  xmlns=\"urn:schemas-microsoft-com:xml-analysis\"" + "\n              SOAP-ENV:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">");
        request.append("\n   <Command>" + "\n      <Statement>" + "\n         " + xmlEncodeString(mdx) + "\n      </Statement>" + "\n   </Command>");
        request.append(properties.getXML("    "));
        request.append("\n    </Execute>");
        request.append("\n  </SOAP-ENV:Body>");
        request.append("\n</SOAP-ENV:Envelope>");
        setExecuteMimeHeaders();
        Document d = null;
        try {
            byte[] b = request.toString().getBytes("UTF8");
            if (shortVerbose && !disallowDebug) {
                System.out.println("MDX: " + mdx);
            }
            if (verbose && !disallowDebug) {
                System.out.println("\nSending request [" + GregorianCalendar.getInstance().getTime() + "]: " + mdx);
                String catalog = properties.getCatalog();
                String dataSource = properties.getDataSourceInfo();
                StringBuffer props = new StringBuffer();
                if (dataSource.trim().length() != 0) {
                    props.append(dataSource.trim());
                }
                if (catalog.trim().length() != 0) {
                    if (props.length() != 0) {
                        props.append(", ");
                    }
                    props.append(catalog.trim());
                }
                System.out.println(request);
            }
            if (outputToFile && !disallowDebug) {
                writer.write("\n\nSending SOAP Message (" + GregorianCalendar.getInstance().getTime() + "):\n" + request + "\n");
            }
            if (debug && !disallowDebug) {
                System.out.println("Request length == " + request.length());
            }
            writeToStream(b, urlConnection.getOutputStream());
            if (urlConnection.getErrorStream() != null && verbose && !disallowDebug) {
                BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getErrorStream()));
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    System.out.println(inputLine);
                }
                in.close();
            } else {
                if (debug && !disallowDebug) {
                    System.out.println("No error stream.");
                }
            }
            try {
                d = builder.parse(urlConnection.getInputStream());
            } catch (Exception e) {
                if (urlConnection.getErrorStream() != null && verbose && !disallowDebug) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getErrorStream()));
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        System.out.println(inputLine);
                    }
                    in.close();
                } else {
                    if (debug) {
                        System.out.println("No error stream.");
                    }
                }
            }
            NodeList faults = d.getElementsByTagName("SOAP-ENV:Fault");
            if (verbose && !disallowDebug) {
                PrintWriter out = new PrintWriter(System.out);
                XMLDocumentWriter dw = new XMLDocumentWriter(out);
                System.out.println("\nRECEIVED SOAP MESSAGE (" + GregorianCalendar.getInstance().getTime() + "):\n");
                dw.write(d.getChildNodes().item(0));
                out.flush();
            }
            if (outputToFile && !disallowDebug) {
                writer.write("\n\nReceived SOAP message (" + GregorianCalendar.getInstance().getTime() + "):\n");
                XMLDocumentWriter dw = new XMLDocumentWriter(null);
                writer.write(dw.write(d.getChildNodes().item(0)));
                writer.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            disconnect();
        }
        return d;
    }

    private synchronized Document discover(XMLARestrictions restrictions, XMLAProperties properties) throws IOException {
        if (!init()) {
            disconnect();
            return null;
        }
        StringBuffer request = new StringBuffer("");
        request.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n<SOAP-ENV:Envelope" + "\n  xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\"" + "\n  SOAP-ENV:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">" + "\n  <SOAP-ENV:Body>" + "\n    <Discover xmlns=\"urn:schemas-microsoft-com:xml-analysis\"" + "\n              SOAP-ENV:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">");
        request.append("\n    <RequestType>" + getRequestType() + "</RequestType>");
        request.append(restrictions.getXML("    "));
        request.append(properties.getXML("    "));
        request.append("\n    </Discover>");
        request.append("\n  </SOAP-ENV:Body>");
        request.append("\n</SOAP-ENV:Envelope>");
        setDiscoverMimeHeaders();
        Document d = null;
        try {
            byte[] b = request.toString().getBytes("UTF8");
            if (shortOutputToFile && !disallowDebug) {
                String outputString = "Send [" + getTimeString() + "]: " + getRequestType() + " with " + formatPropsAndRests(restrictions, properties);
                System.out.println(outputString);
                writer.write(outputString + "\n");
                writer.flush();
            }
            if (shortVerbose && !disallowDebug) {
                System.out.println("Request: " + getRequestType() + " - " + formatPropsAndRests(restrictions, properties));
            }
            if (verbose && !disallowDebug) {
                System.out.println("\nSENDING SOAP MESSAGE (" + GregorianCalendar.getInstance().getTime() + "):\n" + request);
            }
            if (outputToFile && !disallowDebug) {
                writer.write("\n\nSending SOAP message (" + GregorianCalendar.getInstance().getTime() + "):\n" + request);
            }
            try {
                writeToStream(b, urlConnection.getOutputStream());
            } catch (Exception e) {
                if (urlConnection.getErrorStream() != null && verbose && !disallowDebug) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getErrorStream()));
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        if (outputToFile) {
                            writer.write(inputLine + "\n");
                            writer.flush();
                        }
                        System.out.println(inputLine);
                    }
                    in.close();
                }
            }
            try {
                d = builder.parse(urlConnection.getInputStream());
            } catch (Exception e) {
                if (urlConnection.getErrorStream() != null && verbose && !disallowDebug) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getErrorStream()));
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        if (outputToFile) {
                            writer.write(inputLine + "\n");
                            writer.flush();
                        }
                        System.out.println(inputLine);
                    }
                    in.close();
                }
            }
            if (d != null) {
                NodeList faults = d.getElementsByTagName("SOAP-ENV:Fault");
                if (verbose && !disallowDebug) {
                    PrintWriter out = new PrintWriter(System.out);
                    XMLDocumentWriter dw = new XMLDocumentWriter(out);
                    System.out.println("\nRECEIVED SOAP MESSAGE (" + GregorianCalendar.getInstance().getTime() + "):\n");
                    dw.write(d.getChildNodes().item(0));
                    out.flush();
                }
                if (requestType.equals("DBSCHEMA_CATALOGS") && faults != null && faults.getLength() != 0) {
                    throw new PaloException("Could not list databases. Reason: " + getErrorString(faults));
                }
            }
            if (verbose && !disallowDebug) {
                PrintWriter out = new PrintWriter(System.out);
                XMLDocumentWriter dw = new XMLDocumentWriter(out);
                System.out.println("\nRECEIVED SOAP MESSAGE (" + GregorianCalendar.getInstance().getTime() + "):\n");
                dw.write(d.getChildNodes().item(0));
                out.flush();
            }
            if (outputToFile && !disallowDebug) {
                writer.write("\n\nReceived SOAP message (" + GregorianCalendar.getInstance().getTime() + "):\n");
                XMLDocumentWriter dw = new XMLDocumentWriter(null);
                if (d != null) {
                    writer.write(dw.write(d.getChildNodes().item(0)));
                    writer.flush();
                }
            }
        } catch (PaloException e) {
            disconnect();
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            disconnect();
        }
        return d;
    }

    private final void parseServerData(XMLAServerInfo server, Node node) {
        String nodeName = node.getNodeName();
        if (nodeName.equals("DataSourceDescription")) {
            server.setDescription(getTextFromDOMElement(node));
        } else if (nodeName.equals("URL")) {
            server.setUrl(getTextFromDOMElement(node));
        } else if (nodeName.equals("DataSourceInfo")) {
            if (debug) {
                System.out.println("DataSourceInfo == " + getTextFromDOMElement(node));
            }
        } else if (nodeName.equals("ProviderName")) {
            if (debug) {
                System.out.println("ProviderName == " + getTextFromDOMElement(node));
            }
        } else if (nodeName.equals("ProviderType")) {
            if (debug) {
                System.out.println("ProviderType == " + getTextFromDOMElement(node));
            }
        } else if (nodeName.equals("AuthenticationMode")) {
            server.setAuthentication(getTextFromDOMElement(node));
            if (debug) {
                System.out.println("AuthenticationMode == " + getTextFromDOMElement(node));
            }
        } else {
            if (debug) {
                System.out.println("Unknown node name: " + nodeName);
            }
        }
    }

    private XMLAServerInfo[] loadConnections() {
        Set serverSet = new HashSet();
        try {
            Document doc = discoverDataSources(new XMLARestrictions(), new XMLAProperties());
            if (doc == null) {
                return null;
            }
            NodeList rows = doc.getElementsByTagName("row");
            for (int i = 0, n = rows.getLength(); i < n; i++) {
                NodeList elements = rows.item(i).getChildNodes();
                XMLAServerInfo server = null;
                boolean repeatLoop = false;
                for (int j = 0, m = elements.getLength(); j < m; j++) {
                    if (elements.item(j).getNodeType() == Node.ELEMENT_NODE) {
                        if (elements.item(j).getNodeName().equals("DataSourceName")) {
                            String text = getTextFromDOMElement(elements.item(j));
                            server = new XMLAServerInfo(text);
                            serverSet.add(server);
                        } else {
                            if (server == null) {
                                if (debug) {
                                    System.out.println("This should not happen!");
                                }
                                repeatLoop = true;
                                continue;
                            }
                            parseServerData(server, elements.item(j));
                        }
                    }
                }
                if (repeatLoop) {
                    for (int j = 0, m = elements.getLength(); j < m; j++) {
                        if (elements.item(j).getNodeType() == Node.ELEMENT_NODE) {
                            if (!elements.item(j).getNodeName().equals("DataSourceName")) {
                                parseServerData(server, elements.item(j));
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        XMLAServerInfo[] servers = new XMLAServerInfo[serverSet.size()];
        servers = (XMLAServerInfo[]) serverSet.toArray(servers);
        return servers;
    }

    public boolean isSAP(XMLAServerInfo server) {
        String[] rowsetNames = discoverRowsets();
        boolean sap = false;
        for (String rowset : rowsetNames) {
            if (rowset.equalsIgnoreCase("SAP_VARIABLES")) {
                sap = true;
                break;
            }
        }
        return sap;
    }

    public boolean isSAP() {
        if (!globalIsSAPSet) {
            XMLAServerInfo[] servers = getConnections();
            if (servers != null && servers.length > 0) {
                globalIsSAP = isSAP(getConnections()[0]);
                globalIsSAPSet = true;
            }
        }
        return globalIsSAP;
    }

    private String[] discoverRowsets() {
        try {
            XMLAProperties prop = new XMLAProperties();
            XMLARestrictions rest = new XMLARestrictions();
            Document doc = discoverSchemaRowsets(rest, prop);
            if (doc == null) {
                return new String[0];
            }
            NodeList nl = doc.getElementsByTagName("row");
            if (nl == null || nl.getLength() == 0) {
                return new String[0];
            }
            ArrayList<String> rowsetNames = new ArrayList<String>();
            for (int i = 0, n = nl.getLength(); i < n; i++) {
                NodeList nlRow = nl.item(i).getChildNodes();
                for (int j = 0; j < nlRow.getLength(); j++) {
                    if (nlRow.item(j).getNodeType() == Node.ELEMENT_NODE) {
                        String nodeName = nlRow.item(j).getNodeName();
                        if (nodeName.equals("SchemaName")) {
                            rowsetNames.add(XMLAClient.getTextFromDOMElement(nlRow.item(j)));
                        }
                    }
                }
            }
            return rowsetNames.toArray(new String[0]);
        } catch (IOException e) {
        }
        return new String[0];
    }

    public Document discoverDataSources(XMLARestrictions restrictions, XMLAProperties properties) throws IOException {
        setRequestType("DISCOVER_DATASOURCES");
        return discover(restrictions, properties);
    }

    public Document discoverSchemaRowsets(XMLARestrictions restrictions, XMLAProperties properties) throws IOException {
        setRequestType("DISCOVER_SCHEMA_ROWSETS");
        return discover(restrictions, properties);
    }

    public Document getCatalogList(XMLARestrictions restrictions, XMLAProperties properties) throws IOException {
        setRequestType("DBSCHEMA_CATALOGS");
        return discover(restrictions, properties);
    }

    public Document getCubeList(XMLARestrictions restrictions, XMLAProperties properties) throws IOException {
        setRequestType("MDSCHEMA_CUBES");
        return discover(restrictions, properties);
    }

    public Document getSAPVariableList(XMLARestrictions restrictions, XMLAProperties properties) throws IOException {
        setRequestType("SAP_VARIABLES");
        return discover(restrictions, properties);
    }

    public Document getDimensionList(XMLARestrictions restrictions, XMLAProperties properties) throws IOException {
        setRequestType("MDSCHEMA_DIMENSIONS");
        return discover(restrictions, properties);
    }

    public Document getFunctionList(XMLARestrictions restrictions, XMLAProperties properties) throws IOException {
        setRequestType("MDSCHEMA_FUNCTIONS");
        return discover(restrictions, properties);
    }

    public Document getHierarchyList(XMLARestrictions restrictions, XMLAProperties properties) throws IOException {
        setRequestType("MDSCHEMA_HIERARCHIES");
        return discover(restrictions, properties);
    }

    public Document getLevelList(XMLARestrictions restrictions, XMLAProperties properties) throws IOException {
        setRequestType("MDSCHEMA_LEVELS");
        return discover(restrictions, properties);
    }

    public Document getMeasureList(XMLARestrictions restrictions, XMLAProperties properties) throws IOException {
        setRequestType("MDSCHEMA_MEASURES");
        return discover(restrictions, properties);
    }

    public Document getMemberList(XMLARestrictions restrictions, XMLAProperties properties) throws IOException {
        setRequestType("MDSCHEMA_MEMBERS");
        return discover(restrictions, properties);
    }

    public Document getPropertyList(XMLARestrictions restrictions, XMLAProperties properties) throws IOException {
        setRequestType("MDSCHEMA_PROPERTIES");
        return discover(restrictions, properties);
    }

    private void writeToStream(byte[] data, OutputStream urlOutputStream) throws IOException {
        BufferedOutputStream os = null;
        try {
            os = new BufferedOutputStream(urlOutputStream);
            os.write(data);
            os.flush();
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
                if (urlOutputStream != null) {
                    urlOutputStream.close();
                }
            } catch (IOException e) {
            }
        }
    }

    public static boolean isVerbose() {
        return verbose;
    }

    public static boolean isDebug() {
        return debug;
    }

    public XMLAServerInfo[] getConnections() {
        return connections;
    }

    public static void setVerbose(boolean newVerbose) {
        verbose = newVerbose;
    }

    public static void setDebug(boolean newDebug) {
        debug = newDebug;
    }

    private String getTimeString() {
        int hours = GregorianCalendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int minutes = GregorianCalendar.getInstance().get(Calendar.MINUTE);
        int seconds = GregorianCalendar.getInstance().get(Calendar.SECOND);
        int ms = GregorianCalendar.getInstance().get(Calendar.MILLISECOND);
        String msString = "";
        if (ms < 10) {
            msString = "00" + ms;
        } else if (ms < 100) {
            msString = "0" + ms;
        } else {
            msString = "" + ms;
        }
        return hours + ":" + (minutes < 10 ? "0" + minutes : minutes) + ":" + (seconds < 10 ? "0" + seconds : seconds) + "." + msString;
    }

    private String formatPropsAndRests(XMLARestrictions restrictions, XMLAProperties properties) {
        String catalog = properties.getCatalog();
        String dataSource = properties.getDataSourceInfo();
        StringBuffer props = new StringBuffer();
        if (dataSource != null) {
            if (dataSource.trim().length() != 0) {
                props.append(dataSource.trim());
            }
        }
        if (catalog != null) {
            if (catalog.trim().length() != 0) {
                if (props.length() != 0) {
                    props.append(", ");
                }
                props.append(catalog.trim());
            }
        }
        StringBuffer rests = new StringBuffer();
        if (restrictions.getCatalog() != null && restrictions.getCatalog().trim().length() != 0) {
            rests.append(restrictions.getCatalog().trim() + ", ");
        }
        if (restrictions.getCubeName() != null && restrictions.getCubeName().trim().length() != 0) {
            rests.append(restrictions.getCubeName().trim() + ", ");
        }
        if (restrictions.getDimensionUniqueName() != null && restrictions.getDimensionUniqueName().trim().length() != 0) {
            rests.append(restrictions.getDimensionUniqueName().trim() + ", ");
        }
        if (restrictions.getHierarchyUniqueName() != null && restrictions.getHierarchyUniqueName().trim().length() != 0) {
            rests.append(restrictions.getHierarchyUniqueName().trim() + ", ");
        }
        if (restrictions.getMemberUniqueName() != null && restrictions.getMemberUniqueName().trim().length() != 0) {
            rests.append(restrictions.getMemberUniqueName().trim());
        }
        String end = rests.toString().trim();
        if (end.endsWith(",")) {
            end = end.substring(0, end.length() - 1).trim();
        }
        return props.toString() + " [" + end + "]";
    }

    public static void printStackTrace(StackTraceElement[] trace, PrintStream s) {
    }

    public static String getErrorString(NodeList faults) {
        String faultCode = "<Unknown fault code>";
        String faultString = "<No fault string>";
        String faultActor = "<No fault actor>";
        String description = "<No description>";
        faults = faults.item(0).getChildNodes();
        int length = faults.getLength();
        for (int i = 0; i < length; i++) {
            Node faultNode = faults.item(i);
            if (faultNode.getNodeName().equals("faultcode")) {
                faultCode = XMLAClient.getTextFromDOMElement(faultNode);
            } else if (faultNode.getNodeName().equals("faultstring")) {
                faultString = XMLAClient.getTextFromDOMElement(faultNode);
            } else if (faultNode.getNodeName().equals("faultactor")) {
                faultActor = XMLAClient.getTextFromDOMElement(faultNode);
            } else if (faultNode.getNodeName().equals("detail")) {
                NodeList details = faultNode.getChildNodes();
                if (details != null && details.getLength() > 0) {
                    for (int j = 0, n = details.getLength(); j < n; j++) {
                        Node detailNode = details.item(j);
                        if (detailNode.getNodeName().equals("Error")) {
                            NamedNodeMap errorMap = detailNode.getAttributes();
                            Node dNode = errorMap.getNamedItem("Description");
                            description = "";
                            if (dNode != null) {
                                description += "Description:  " + dNode.getNodeValue() + " \n";
                            }
                            dNode = errorMap.getNamedItem("ErrorCode");
                            if (dNode != null) {
                                description += "ErrorCode:    " + dNode.getNodeValue() + " \n";
                            }
                            dNode = errorMap.getNamedItem("HelpFile");
                            if (dNode != null) {
                                description += "HelpFile:     " + dNode.getNodeValue() + " \n";
                            }
                            dNode = errorMap.getNamedItem("Source");
                            if (dNode != null) {
                                description += "Source:       " + dNode.getNodeValue() + " \n";
                            }
                        } else if (detailNode.getNodeName().startsWith("XA:error")) {
                            NodeList dcNodes = detailNode.getChildNodes();
                            for (int k = 0, m = dcNodes.getLength(); k < m; k++) {
                                Node dcNode = dcNodes.item(k);
                                if (dcNode.getNodeName().equals("desc")) {
                                    description = XMLAClient.getTextFromDOMElement(dcNode);
                                }
                            }
                        }
                    }
                }
            }
        }
        StringBuffer errorMessage = new StringBuffer("Error!   \n");
        errorMessage.append("Fault code:   " + faultCode + " \n");
        errorMessage.append("Fault string: " + faultString + " \n");
        errorMessage.append("Fault actor:  " + faultActor + " \n");
        errorMessage.append(description);
        return errorMessage.toString();
    }
}
