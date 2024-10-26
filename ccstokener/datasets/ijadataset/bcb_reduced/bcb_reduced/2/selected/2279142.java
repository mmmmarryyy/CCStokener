package com.bitgate.util.soap;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import com.bitgate.server.Server;
import com.bitgate.util.cookie.CookieStore;
import com.bitgate.util.debug.Debug;
import com.bitgate.util.node.NodeUtil;
import com.bitgate.util.services.engine.DocumentFactory;
import com.bitgate.util.services.engine.Encoder;
import com.bitgate.util.services.engine.RenderEngine;
import com.sun.org.apache.xpath.internal.XPathAPI;

/**
 * This class connects to a SOAP server, makes a request, parses its information, and stores it so other classes can access
 * the data that was parsed.
 *
 * @author Kenji Hollis &lt;kenji@nuklees.com&gt;
 * @version $Id: //depot/nuklees/util/soapclient/SoapClient.java#8 $
 */
public class SoapClient {

    /**
     * Constructor.
     */
    public SoapClient() {
    }

    /**
     * Public function to walk the tree of a <code>Node</code> with an XPath expression.
     *
     * @param node The <code>Node</code> to traverse.
     * @param path The XPath expression to perform.
     * @return <code>String</code> containing the result.
     */
    public String walkNodeTree(Node node, String path) {
        String str = null;
        try {
            str = XPathAPI.eval(node, path).str();
        } catch (Exception e) {
            Debug.log("Unable to retrieve path '" + path + "' from supplied XML document: " + e);
        }
        Debug.log("Path '" + path + "' returns '" + str + "'");
        return str;
    }

    /**
     * Strips off the result of a string, cleaning off any URLEncoded data.
     *
     * @param request The request to strip.
     * @return <code>String</code> containing the stripped string.
     */
    public String stripResult(String request) {
        StringBuffer requestBuffer = new StringBuffer();
        String returnString = null;
        int requestLength = request.length();
        for (int i = 0; i < requestLength; i++) {
            if (request.charAt(i) == '\r') {
                requestBuffer.append("%0d");
            } else if (request.charAt(i) == '\n') {
                requestBuffer.append("%0a");
            } else {
                requestBuffer.append(request.charAt(i));
            }
        }
        returnString = requestBuffer.toString();
        requestBuffer = null;
        boolean found = true;
        returnString = returnString.replaceAll("%0d", "\r");
        returnString = returnString.replaceAll("%0a", "\n");
        return returnString;
    }

    private void loadCookies(String location, URLConnection conn, RenderEngine c) {
        String locationFile = location;
        if (locationFile.startsWith("http://")) {
            locationFile = locationFile.substring(7);
        } else if (locationFile.startsWith("https://")) {
            locationFile = locationFile.substring(8);
        }
        if (locationFile.indexOf("/") != -1) {
            locationFile = locationFile.substring(locationFile.indexOf("/") + 1);
        }
        ArrayList list = CookieStore.getDefault().getCookieList(locationFile);
        if (list == null) {
            Debug.log("Unable to retrieve cookie store from memory; using client cookies.");
            if (c.getClientContext().getRequestHeader("cookie") != null) {
                conn.setRequestProperty("Cookie", c.getClientContext().getRequestHeader("cookie"));
            }
        } else {
            for (int i = 0; i < list.size(); i++) {
                conn.setRequestProperty("Cookie", (String) list.get(i));
                Debug.inform("Setting soap cookie '" + (String) list.get(i) + "'");
            }
        }
        list = null;
    }

    private void handleHeaders(String location, Map headers) {
        if (headers == null) {
            Debug.inform("Client sent no headers, returning.");
        }
        String locationFile = location;
        if (locationFile.startsWith("http://")) {
            locationFile = locationFile.substring(7);
        } else if (locationFile.startsWith("https://")) {
            locationFile = locationFile.substring(8);
        }
        if (locationFile.indexOf("/") != -1) {
            locationFile = locationFile.substring(locationFile.indexOf("/") + 1);
        }
        Iterator it = headers.keySet().iterator();
        while (it.hasNext()) {
            String header = (String) it.next();
            Debug.inform("Soap header '" + header + "' = '" + (String) headers.get(header));
            if (header.equalsIgnoreCase("set-cookie")) {
                CookieStore.getDefault().addCookie(locationFile, (String) headers.get(header));
            }
        }
        it = null;
    }

    /**
     * Requests data from a URL and returns its information.
     *
     * @param urlLocation The URL to request.
     * @param headers The <code>ArrayList</code> containing headers to send to the client.
     * @param c The currently active <code>RenderEngine</code> object.
     * @return <code>String</code> containing the parsed data.
     * @throws Exception on any errors.
     */
    public String readURL(String urlLocation, ArrayList headers, RenderEngine c) throws Exception {
        URL url = null;
        HttpURLConnection conn = null;
        InputStream istream = null;
        try {
            url = new URL(urlLocation);
            conn = (HttpURLConnection) url.openConnection();
        } catch (Exception e) {
            throw new Exception("Soap is unable to retrieve URL for '" + urlLocation + "': " + e.getMessage());
        }
        loadCookies(urlLocation, conn, c);
        if (headers != null) {
            for (int i = 0; i < headers.size(); i++) {
                String header = (String) headers.get(i);
                String key = header.substring(0, header.indexOf(":"));
                String value = header.substring(header.indexOf(":") + 2);
                Debug.log("Adding new request header '" + key + "'='" + value + "'");
                conn.setRequestProperty(key, value);
            }
        }
        Debug.debug("Set to use GET, URL=" + urlLocation);
        try {
            istream = conn.getInputStream();
        } catch (Exception e) {
            Debug.debug("Unable to capture input stream: " + e.getMessage());
            throw new Exception("Unable to capture input stream from URL '" + urlLocation + "': " + e.getMessage());
        }
        Debug.debug("'GET' - Got input stream.");
        if (conn.getContentLength() == -1) {
            Debug.debug("Content length = unknown");
        } else {
            Debug.debug("Content length = " + conn.getContentLength());
        }
        byte data[] = null;
        int curPos = 0, contentLength = conn.getContentLength();
        if (conn.getContentLength() == -1) {
            String byteSize = NodeUtil.walkNodeTree(Server.getConfig(), "//configuration/object[@type='engine.tunable']/property[@type='engine.unknowncontentsize']/@value");
            if (byteSize == null) {
                contentLength = 4096;
            } else {
                contentLength = Integer.parseInt(byteSize);
            }
            Debug.debug("Content length unknown.  Allowing fuzz of " + contentLength + " bytes.");
        }
        data = new byte[contentLength];
        try {
            int dataRead = 0;
            while ((dataRead = istream.read(data, curPos, contentLength - curPos)) != -1) {
                if (dataRead == 0) {
                    break;
                }
                curPos += dataRead;
            }
        } catch (Exception e) {
            throw new Exception("Soap is unable to read data from HTTP connection: " + e.getMessage());
        }
        try {
            istream.close();
            conn.disconnect();
        } catch (MalformedURLException e) {
            throw new Exception("Soap request to site '" + urlLocation + "' is invalid: " + e.getMessage());
        } catch (IOException e) {
            throw new Exception("Soap request to site '" + urlLocation + "' failed to connect.");
        }
        String dataOut = new String(data);
        int counter = 0;
        data = null;
        istream = null;
        conn = null;
        url = null;
        return dataOut.trim();
    }

    /**
     * Makes a POST request to a client.
     *
     * @param urlLocation The URL to request.
     * @param headers The <code>ArrayList</code> object containing a list of headers to send.
     * @param content The content to send.
     * @param postVariables A <code>HashMap</code> of the variables to send to the requested URL.
     * @param c The currently active <code>RenderEngine</code> object.
     */
    public String postURL(String urlLocation, ArrayList headers, String content, HashMap postVariables, RenderEngine c) throws Exception {
        String postContent = null;
        if (postVariables != null) {
            boolean firstElement = true;
            postContent = new String();
            Iterator elements = postVariables.keySet().iterator();
            while (elements.hasNext()) {
                String key = (String) elements.next();
                String val = (String) postVariables.get(key);
                if (firstElement) {
                    postContent += Encoder.URLEncode(key) + "=" + Encoder.URLEncode(val);
                    firstElement = false;
                } else {
                    postContent += "&" + Encoder.URLEncode(key) + "=" + Encoder.URLEncode(val);
                }
            }
            elements = null;
        } else {
            postContent = content;
        }
        Debug.log("Connecting to URL '" + urlLocation + "', content '" + postContent + "'");
        URL url = null;
        try {
            url = new URL(urlLocation);
        } catch (MalformedURLException e) {
            Debug.log("Unable to retrieve URL '" + urlLocation + "': " + e.getMessage());
            return null;
        }
        StringBuffer lines = new StringBuffer();
        HttpURLConnection conn = null;
        boolean contentLengthFound = false;
        try {
            conn = (HttpURLConnection) url.openConnection();
            loadCookies(urlLocation, conn, c);
            if (headers != null) {
                for (int i = 0; i < headers.size(); i++) {
                    String header = (String) headers.get(i);
                    String key = header.substring(0, header.indexOf(":"));
                    String value = header.substring(header.indexOf(":") + 2);
                    if (key != null && key.equalsIgnoreCase("content-length")) {
                        contentLengthFound = true;
                    }
                    Debug.log("Adding new request header '" + key + "'='" + value + "'");
                    conn.setRequestProperty(key, value);
                }
            }
            if (!contentLengthFound) {
                Debug.log("Adding new request header 'Content-Length'='" + postContent.length() + "'");
                conn.setRequestProperty("Content-Length", Integer.toString(postContent.length()));
            }
            conn.setDoOutput(true);
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(postContent);
            wr.flush();
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line = null;
            while ((line = rd.readLine()) != null) {
                lines.append(line);
                lines.append("\r\n");
            }
            handleHeaders(urlLocation, conn.getHeaderFields());
            wr.close();
            rd.close();
            wr = null;
            rd = null;
        } catch (IOException e) {
            if (conn != null) {
                lines = new StringBuffer();
                try {
                    throw new Exception("Server returned error code '" + conn.getResponseCode() + "': " + conn.getResponseMessage());
                } catch (IOException ee) {
                    throw new Exception("Unable to report error codes: " + ee.getMessage());
                }
            }
            Debug.log("I/O Exception occurred while communicating with endpoint: " + e.getMessage());
            return lines.toString().trim();
        } catch (Exception e) {
        }
        url = null;
        conn = null;
        return lines.toString().trim();
    }

    /**
     * Returns the available operations from a specified SOAP URL.
     *
     * @param url The URL to parse.
     * @param c The currently active <code>RenderEngine</code> object.
     * @return <code>OperationsStore</code> object.
     */
    public OperationsStore getOperations(String url, RenderEngine c) {
        OperationsStore ops = OperationsStoreCache.getDefault().get(url);
        ArrayList headers = new ArrayList();
        String wsdlData = null;
        if (ops != null) {
            return ops;
        }
        ops = new OperationsStore();
        if (c.getClientContext().getRequestHeader("cookie") != null) {
            headers.add("Cookie: " + c.getClientContext().getRequestHeader("cookie"));
            Debug.inform("Adding cookies to requested soap site.");
        }
        if (SoapCache.getDefault().getCache(url) == null) {
            try {
                wsdlData = readURL(url, headers, c);
            } catch (Exception e) {
                Debug.debug("Unable to contact site '" + url + "': " + e + " (" + e.getMessage() + ")");
                return null;
            }
        }
        Node node = null;
        if (SoapCache.getDefault().getCache(url) == null) {
            ByteArrayInputStream bais = new ByteArrayInputStream(wsdlData.getBytes());
            try {
                Document doc = DocumentFactory.getDefault().getFactory().newDocumentBuilder().parse(new InputSource(bais));
                node = (Node) doc.getDocumentElement();
            } catch (Exception e) {
                Debug.debug("Unable to parse: " + e);
                return null;
            }
            Debug.log("Soap document loaded: " + wsdlData.length() + " byte(s)");
            SoapCache.getDefault().addCache(url, node);
        } else {
            node = SoapCache.getDefault().getCache(url);
        }
        SoapParser soapParser = new SoapParser((Node) node, null, null, null, null);
        Vector operations = null;
        HashMap operationDocumentation = null;
        try {
            operations = soapParser.getOperations();
            operationDocumentation = soapParser.getOperationDocumentation();
            Debug.debug("Retrieved operations: Size ops=" + operations.size() + " doc=" + operationDocumentation.size());
        } catch (SoapParserException e) {
            Debug.debug("Unable to retrieved operations: " + e.getMessage());
            return null;
        }
        ops.setOperations(operations);
        ops.setOperationDocumentation(operationDocumentation);
        OperationsStoreCache.getDefault().add(url, ops);
        headers = null;
        node = null;
        soapParser = null;
        operations = null;
        operationDocumentation = null;
        return ops;
    }

    /**
     * Returns the currently known Ports from a SOAP URL.
     *
     * @param url The URL to request.
     * @param portname The port name to look up.
     * @param c The <code>RenderEngine</code> object.
     * @return <code>PortStore</code> object.
     */
    public PortStore getPort(String url, String portname, RenderEngine c) {
        String wsdlData = null;
        Node node = null;
        try {
            ArrayList headers = new ArrayList();
            if (c.getClientContext().getRequestHeader("cookie") != null) {
                headers.add("Cookie: " + c.getClientContext().getRequestHeader("cookie"));
                Debug.inform("Adding cookies to requested soap site.");
            }
            if (SoapCache.getDefault().getCache(url) == null) {
                wsdlData = readURL(url, headers, c);
            }
        } catch (Exception e) {
            Debug.log("Unable to contact site '" + url + ": " + e.getMessage());
            return null;
        }
        if (SoapCache.getDefault().getCache(url) == null) {
            ByteArrayInputStream bais = new ByteArrayInputStream(wsdlData.toString().getBytes());
            try {
                Document doc = DocumentFactory.getDefault().getFactory().newDocumentBuilder().parse(new InputSource(bais));
                node = (Node) doc.getDocumentElement();
            } catch (Exception e) {
                Debug.log("Unable to parse: " + e);
            }
            Debug.log("Soap document loaded: " + url.length() + " byte(s)");
            SoapCache.getDefault().addCache(url, node);
        } else {
            node = SoapCache.getDefault().getCache(url);
        }
        SoapParser soapParser = new SoapParser((Node) node, portname, null, null, null);
        PortStore pStore = new PortStore();
        try {
            pStore.addPorts(soapParser.getPorts());
            pStore.addDocumentation(soapParser.getDocumentation());
        } catch (SoapParserException e) {
            Debug.debug("Unable to parse ports: " + e.getMessage());
            return null;
        }
        soapParser = null;
        node = null;
        return pStore;
    }
}
