package org.tdwg.tapir;

import java.io.InputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamException;

/**
 * <CODE>TAPIRClient</CODE> provides a utility to access to 
 * a TAPIR provider.  It is desgined to be compatible with
 * TpClient, a TAPIR client written in PHP
 *
 * @version 	25 Dec. 2008
 * @author 	Nozomi `James' Ytow
 */
public class TAPIRClient {

    public static String SEARCH_RESPONSE_BASE_CLASS = SearchResponse.class.getName();

    public static String LIBRARY_NAME = TAPIRClient.class.getName();

    public static String LIBRARY_VERSION = "0.1";

    public static String UTF8 = "UTF-8";

    protected URL url;

    protected String accessPoint;

    protected String agentName;

    protected String agentVersion;

    protected boolean shortNames;

    protected boolean shortValues;

    protected CapabilitiesResponse capabilitiesResponse;

    protected Object lastRequest;

    protected int lastResponseCode;

    protected Map<String, List<String>> lastResponseHeader;

    protected String lastResponseContent;

    protected HttpURLConnection connection;

    protected int encoding;

    protected static XMLInputFactory xmlInputFactory;

    public static boolean isPost(int type) {
        switch(type) {
            case Encoding.KVP_POST:
            case Encoding.XML_RAW_POST:
            case Encoding.XML_POST:
                return true;
        }
        return false;
    }

    public static boolean isGet(int type) {
        switch(type) {
            case Encoding.KVP_GET:
            case Encoding.XML_GET:
                return true;
        }
        return false;
    }

    public TAPIRClient(String accessPoint) throws TAPIRException {
        this(accessPoint, Encoding.KVP_GET);
    }

    public TAPIRClient(String accessPoint, int encoding) throws TAPIRException {
        this(accessPoint, encoding, LIBRARY_NAME, LIBRARY_VERSION);
    }

    public TAPIRClient(String accessPoint, int encoding, String agentName, String agentVersion) throws TAPIRException {
        this(accessPoint, encoding, agentName, agentVersion, true, true);
    }

    public TAPIRClient(String accessPoint, int encoding, String agentName, String agentVersion, boolean useShortNames, boolean useShortVlues) throws TAPIRException {
        if (accessPoint == null || accessPoint.length() == 0) {
            throw new TAPIRException("TAPIRClient: No accesspoint speficied");
        }
        this.accessPoint = accessPoint;
        this.agentName = agentName;
        this.agentVersion = agentVersion;
        setEncoding(encoding);
        this.shortNames = shortNames;
        this.shortValues = shortValues;
    }

    protected static URL getKVPGetURL(String accessPoint, Map<String, ? extends Object> query, String encoding) throws TAPIRException {
        int i = 0;
        StringBuffer url = new StringBuffer(accessPoint);
        url.append((url.charAt(url.length() - 1) == '?') ? '&' : '?');
        Iterator<String> keys = query.keySet().iterator();
        try {
            while (keys.hasNext()) {
                String key = keys.next();
                Object value = query.get(key);
                key = URLEncoder.encode(key, encoding);
                if (value != null && value instanceof Iterable) {
                    Iterator values = ((Iterable) value).iterator();
                    while (values.hasNext()) {
                        value = values.next();
                        if (i > 0) {
                            url.append('&');
                        }
                        url.append(key).append("=");
                        url.append(URLEncoder.encode(value.toString(), encoding));
                        i++;
                    }
                } else {
                    if (i > 0) {
                        url.append('&');
                    }
                    url.append(key).append("=");
                    url.append(URLEncoder.encode(value.toString(), encoding));
                    i++;
                }
            }
            return new URL(url.toString());
        } catch (Throwable t) {
            throw new TAPIRException("TAPIREClient#getKVPGetURL: " + t.getMessage(), t);
        }
    }

    public void setEncoding(int encoding) throws TAPIRException {
        switch(encoding) {
            case Encoding.KVP_GET:
            case Encoding.XML_GET:
            case Encoding.KVP_POST:
            case Encoding.XML_RAW_POST:
            case Encoding.XML_POST:
                break;
            default:
                throw new TAPIRException("Unknown TAPIR request encoding: " + encoding);
        }
        if (encoding > Encoding.XML_RAW_POST && capabilitiesResponse != null && !capabilitiesResponse.supportsXML()) {
            throw new TAPIRException("Unknown TAPIR request encoding: " + Encoding.getLabel(encoding));
        }
        this.encoding = encoding;
        switch(encoding) {
            case Encoding.KVP_GET:
            case Encoding.XML_GET:
                break;
            case Encoding.KVP_POST:
            case Encoding.XML_RAW_POST:
            case Encoding.XML_POST:
                break;
        }
    }

    public int getEncoding() {
        return encoding;
    }

    public void setAgentName(String name) {
        this.agentName = name;
    }

    public String getAgentName() {
        return agentName;
    }

    public void setAgentVersion(String version) {
        this.agentVersion = version;
    }

    public String getAgentVersion() {
        return agentVersion;
    }

    public void setShortNames(boolean useShort) {
        this.shortNames = useShort;
    }

    public boolean getShortNames() {
        return shortNames;
    }

    public void setShortValues(boolean useShort) {
        this.shortValues = useShort;
    }

    public boolean getShortValues() {
        return shortValues;
    }

    public String getEndpoint() {
        return getAccessPoint();
    }

    public String getAccessPoint() {
        if (url == null) return accessPoint;
        StringBuffer ap = new StringBuffer(url.getProtocol());
        ap.append("://").append(url.getHost());
        int port = url.getPort();
        if (port != -1) {
            ap.append(':').append(port);
        }
        return ap.toString();
    }

    public boolean supportsXML() throws TAPIRException {
        loadCapabilities();
        return capabilitiesResponse.supportsXML();
    }

    public boolean supportsInventory() throws TAPIRException {
        loadCapabilities();
        return capabilitiesResponse.supportsInventory();
    }

    public boolean supportsInventoryOnAnyConcepts() throws TAPIRException {
        loadCapabilities();
        return capabilitiesResponse.supportsInventoryOnAnyConcepts();
    }

    public Iterator<QueryTemplate> getInventoryTemplates() throws TAPIRException {
        loadCapabilities();
        return capabilitiesResponse.getInventoryTemplates();
    }

    public boolean supportsSearch() throws TAPIRException {
        loadCapabilities();
        return capabilitiesResponse.supportsSearch();
    }

    public boolean supportsSearchWithAnyOutputModels() throws TAPIRException {
        loadCapabilities();
        return capabilitiesResponse.supportsSearchWithAnyOutputModels();
    }

    public Iterator<OutputModel> getKnownOutputModels() throws TAPIRException {
        loadCapabilities();
        return capabilitiesResponse.getKnownOutputModels();
    }

    public Iterator<QueryTemplate> getSearchTemplates() throws TAPIRException {
        loadCapabilities();
        return capabilitiesResponse.getSearchTemplates();
    }

    public boolean supportsFilter() throws TAPIRException {
        loadCapabilities();
        return capabilitiesResponse.supportsFilter();
    }

    public boolean mappedSchema(String namespace) throws TAPIRException {
        loadCapabilities();
        return capabilitiesResponse.mappedSchema(namespace);
    }

    public Iterator<MappedSchema> getMappedSchemata() throws TAPIRException {
        loadCapabilities();
        return capabilitiesResponse.getMappedSchemata();
    }

    public boolean mappedConcept(String conceptId) throws TAPIRException {
        loadCapabilities();
        return capabilitiesResponse.mappedConcept(conceptId);
    }

    public Iterator<String> getMappedConcepts() throws TAPIRException {
        loadCapabilities();
        return capabilitiesResponse.getMappedConcepts();
    }

    public Iterator<String> getSupportedVariables() throws TAPIRException {
        loadCapabilities();
        return capabilitiesResponse.getSupportedVariables();
    }

    public String getSetting(String name) throws TAPIRException {
        loadCapabilities();
        return capabilitiesResponse.getSetting(name);
    }

    public Object getLastRequest() {
        return lastRequest;
    }

    public int getLastResponseCode() {
        return lastResponseCode;
    }

    public Map<String, List<String>> getLastResponseHeader() {
        return lastResponseHeader;
    }

    public String getLastResponseContent() {
        return lastResponseContent;
    }

    protected InputStream getQueryResult(String query) throws TAPIRException {
        InputStream xml;
        if (encoding > Encoding.KVP_POST) {
            xml = sendXMLRequest("<" + query + "/>");
        } else {
            Hashtable<String, Object> request = new Hashtable<String, Object>(1);
            request.put("op", (shortValues) ? query.substring(0, 1) : query);
            xml = sendKVPRequest(request);
        }
        return xml;
    }

    public CapabilitiesResponse capabilities() throws TAPIRException {
        return capabilities(true);
    }

    public CapabilitiesResponse capabilities(boolean overwriteInternalReference) throws TAPIRException {
        CapabilitiesResponse c = new CapabilitiesResponse(getQueryResult("capabilities"), TAPIR.NAMESPACE);
        if (overwriteInternalReference || capabilitiesResponse == null) capabilitiesResponse = c;
        return c;
    }

    public MetadataResponse getMetadata() throws TAPIRException {
        return new MetadataResponse(getQueryResult("metadata"), TAPIR.NAMESPACE);
    }

    protected String getAbbreviation(boolean shorter, String key) {
        return (shorter) ? key.substring(0, 1) : key;
    }

    public InventoryResponse getCustomInventory(Map<String, String> targets, Boolean count, int start, int limit, Filter filter) throws TAPIRException {
        if (targets == null || targets.isEmpty()) {
            throw new TAPIRException("First parameter to TAPIRClient#getCustomInventory must be non-empty Map");
        }
        InventoryResponse response;
        if (encoding > Encoding.KVP_POST) {
            StringBuffer body = new StringBuffer("<inventory");
            body.append(getXMLPagingParameters(count, start, limit));
            body.append("><concepts>");
            for (Iterator<String> i = targets.keySet().iterator(); i.hasNext(); ) {
                String key = i.next();
                body.append("<concept id=\"").append(key).append("\"");
                String value = targets.get(key);
                if (value != null && key != value) {
                    body.append(" tagName=\"").append(value).append("\"");
                }
                body.append("/>");
            }
            body.append("</concepts>");
            if (filter != null) {
                body.append(filter.getXML());
            }
            body.append("</inventory>");
            System.out.println(body.toString());
            response = new InventoryResponse(sendXMLRequest(body.toString()), TAPIR.NAMESPACE);
        } else {
            Hashtable<String, Object> parameters = new Hashtable<String, Object>();
            parameters.put("op", (shortValues) ? "i" : "inventory");
            parameters.put((shortNames) ? "c" : "concept", targets.keySet());
            Collection values = targets.values();
            if (values != null) {
                parameters.put((shortNames) ? "n" : "tagname", values);
            }
            Map<String, String> query = getKVPPagingParameters(count, start, limit);
            for (Iterator<String> keys = query.keySet().iterator(); keys.hasNext(); ) {
                String key = keys.next();
                parameters.put(key, query.get(key));
            }
            query.clear();
            if (filter != null) {
                parameters.put((shortNames) ? "f" : "filter", filter.getKVP());
            }
            response = new InventoryResponse(sendKVPRequest(parameters), TAPIR.NAMESPACE);
            parameters.clear();
        }
        return response;
    }

    public InventoryResponse getTemplateInventory(String template, Boolean count, int start, int limit) throws TAPIRException {
        InventoryResponse response;
        if (Encoding.isKVP(encoding)) {
            Hashtable<String, Object> parameters = new Hashtable<String, Object>();
            parameters.put("op", getAbbreviation(shortValues, "inventory"));
            parameters.put(getAbbreviation(shortNames, "template"), template);
            Map<String, String> pagingParameters = getKVPPagingParameters(count, start, limit);
            for (Iterator<String> i = pagingParameters.keySet().iterator(); i.hasNext(); ) {
                String key = i.next();
                parameters.put(key, pagingParameters.get(key));
            }
            pagingParameters.clear();
            response = new InventoryResponse(sendKVPRequest(parameters), TAPIR.NAMESPACE);
            parameters.clear();
        } else if (Encoding.isXML(encoding)) {
            StringBuffer body = new StringBuffer("<inventory");
            body.append(getXMLPagingParameters(count, start, limit));
            body.append(">");
            body.append("<template location=\"");
            body.append(template).append("\">");
            body.append("</inventory>");
            response = new InventoryResponse(sendXMLRequest(body.toString()), TAPIR.NAMESPACE);
        } else {
            response = null;
        }
        return response;
    }

    public SearchResponse customSearch(String model, String className, Boolean count, int start, int limit, Boolean envelope, Boolean omitNamespace, Filter filter, Map<String, Boolean> orderBy) throws TAPIRException {
        return search("model", "externalOutputModel", model, className, count, start, limit, envelope, omitNamespace, filter, orderBy);
    }

    public SearchResponse templateSearch(String template, String className, Boolean count, int start, int limit, Boolean envelope, Boolean omitNamespace, Filter filter, Map<String, Boolean> orderBy) throws TAPIRException {
        return search("template", "template", template, className, count, start, limit, envelope, omitNamespace, filter, orderBy);
    }

    protected SearchResponse search(String tag, String xmlTag, String modelTemplate, String className, Boolean count, int start, int limit, Boolean envelope, Boolean omitNamespace, Filter filter, Map<String, Boolean> orderBy) throws TAPIRException {
        InputStream xml = null;
        if (Encoding.isKVP(encoding)) {
            Hashtable<String, Object> parameters = new Hashtable<String, Object>();
            parameters.put("op", getAbbreviation(shortValues, "search"));
            parameters.put(getAbbreviation(shortNames, "tag"), modelTemplate);
            Map<String, String> map = getKVPPagingParameters(count, start, limit);
            for (Iterator<String> keys = map.keySet().iterator(); keys.hasNext(); ) {
                String key = keys.next();
                parameters.put(key, map.get(key));
            }
            map.clear();
            map = getKVPExtraSearchParameters(envelope, omitNamespace);
            for (Iterator<String> keys = map.keySet().iterator(); keys.hasNext(); ) {
                String key = keys.next();
                parameters.put(key, map.get(key));
            }
            map.clear();
            if (filter != null) {
                parameters.put(getAbbreviation(shortNames, "filter"), filter);
            }
            if (orderBy != null) {
                parameters.put(getAbbreviation(shortNames, "orderby"), orderBy);
            }
            xml = sendKVPRequest(parameters);
            parameters.clear();
        } else if (Encoding.isXML(encoding)) {
            StringBuffer body = new StringBuffer("<search");
            body.append(getXMLPagingParameters(count, start, limit));
            body.append(getXMLExtraSearchParameters(envelope, omitNamespace));
            body.append(">");
            body.append("<").append(xmlTag).append(" location=\"");
            body.append(modelTemplate).append("\"/>");
            if (filter != null) {
                body.append(filter.getXML());
            }
            if (orderBy != null) {
                body.append("<orderBy>");
                for (Iterator<String> i = orderBy.keySet().iterator(); i.hasNext(); ) {
                    String key = i.next();
                    body.append("<concept id=\"").append(key).append("\"");
                    Object value = orderBy.get(key);
                    if (value == Boolean.TRUE || Boolean.TRUE.equals(value)) body.append(" descend=\"true\"");
                    body.append("/>");
                }
                body.append("</orderBy>");
            }
            body.append("</search>");
            xml = sendXMLRequest(body.toString());
        }
        try {
            if (className == null) className = SEARCH_RESPONSE_BASE_CLASS;
            Class<? extends SearchResponse> classFor = Class.forName(className).asSubclass(SearchResponse.class);
            Constructor<? extends SearchResponse> constructor = classFor.getConstructor(InputStream.class, String.class);
            SearchResponse response = constructor.newInstance(xml, TAPIR.NAMESPACE);
            return response;
        } catch (Throwable t) {
            throw new TAPIRException(this, "search", t);
        }
    }

    public PingResponse ping() throws TAPIRException {
        return new PingResponse(getQueryResult("ping"), TAPIR.NAMESPACE);
    }

    private void clearLastData() {
        lastRequest = null;
        lastResponseCode = 0;
        lastResponseHeader = null;
        lastResponseContent = null;
    }

    protected String wrapXMLRequestBody(String body, String agentName, String agentVersion) {
        StringBuffer xml = new StringBuffer("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
        xml.append("<request xmlns=\"").append(TAPIR.NAMESPACE);
        xml.append("\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        xml.append(" xsi:schemaLocation=\"");
        xml.append(TAPIR.NAMESPACE).append(" ").append(TAPIR.SCHEMA_LOCATION).append("\">");
        xml.append("<header><source sendtime=\"");
        xml.append(new SimpleDateFormat("yyyy-MM-ddTHH:mm:ss zz").format(new Date()));
        xml.append("\"><software name=\"").append(agentName);
        xml.append("\" version=\"").append(agentVersion);
        xml.append("\"/></source></header>");
        xml.append(body).append("</request>");
        return xml.toString();
    }

    public InputStream sendHTTPRequest(HttpURLConnection connection) throws TAPIRException {
        try {
            connection.connect();
            lastResponseCode = connection.getResponseCode();
            if (lastResponseCode != 200) {
                throw new TAPIRException(this, "sendHTTPRequest: Unexpected HTTP status code " + String.valueOf(lastResponseCode));
            }
            String contentType = connection.getContentType();
            if (contentType == null) {
                throw new TAPIRException(this, "sendHTTPRequest: No content-type returned");
            }
            if (contentType.length() < 8 || !contentType.startsWith("text/xml")) {
                throw new TAPIRException(this, "sendHTTPRequest: Unexpected content-type: " + contentType);
            }
            lastResponseHeader = connection.getHeaderFields();
            return connection.getInputStream();
        } catch (IOException ioe) {
            throw new TAPIRException(this, "sendHTTPRequest", ioe);
        }
    }

    public InputStream sendKVPRequest(Map<String, ? extends Object> query) throws TAPIRException {
        if (!Encoding.isKVP(encoding)) {
            throw new TAPIRException(this, "sendKVPRequest: invalid request");
        }
        clearLastData();
        HttpURLConnection connection = null;
        if (encoding == Encoding.KVP_GET) {
            try {
                connection = (HttpURLConnection) getKVPGetURL(accessPoint, query, UTF8).openConnection();
                connection.setRequestMethod("GET");
                connection.setDoOutput(true);
                connection.connect();
            } catch (Throwable t) {
                throw new TAPIRException(this, "sendKVPRequest [GET]", t);
            }
        } else if (encoding == Encoding.KVP_POST) {
            try {
                if (url == null) {
                    url = new URL(accessPoint);
                }
                connection = (HttpURLConnection) url.openConnection();
                connection.setDoOutput(true);
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                if (agentName != null) {
                    connection.setRequestProperty("User-Agent", agentName);
                }
                StringBuffer postData = new StringBuffer();
                for (Iterator<String> keys = query.keySet().iterator(); keys.hasNext(); ) {
                    String key = keys.next();
                    Object value = query.get(key);
                    if (value instanceof Collection) {
                        Iterator i = ((Collection) value).iterator();
                        while (i.hasNext()) {
                            Object v = i.next();
                            if (v != null) {
                                if (postData.length() > 0) {
                                    postData.append("&");
                                }
                                postData.append(key).append("=").append(v.toString());
                            }
                        }
                    } else {
                        if (postData.length() > 0) {
                            postData.append("&");
                        }
                        postData.append(key).append("=").append(value.toString());
                    }
                }
                PrintStream out = new PrintStream(connection.getOutputStream());
                out.print(postData.toString());
                out.close();
            } catch (Throwable t) {
                throw new TAPIRException(this, "sendKVPRequest [POST]", t);
            }
        } else {
            throw new TAPIRException(this, "sendKVPRequest: Incompatible TAPIR encoding to use with KVP requests");
        }
        return sendHTTPRequest(connection);
    }

    public InputStream sendXMLRequest(String body) throws TAPIRException {
        return sendXMLRequest(body, true);
    }

    public InputStream sendXMLRequest(String body, boolean wrapBody) throws TAPIRException {
        if (!Encoding.isXML(encoding)) {
            throw new TAPIRException(this, "sendXMLRequest: invalid request");
        }
        String xml = (wrapBody) ? wrapXMLRequestBody(body, agentName, agentVersion) : body;
        lastRequest = xml;
        if (encoding == Encoding.XML_GET) {
            try {
                StringBuffer query = new StringBuffer(accessPoint);
                query.append("?request=");
                query.append(URLEncoder.encode(xml, UTF8));
                connection = (HttpURLConnection) (new URL(query.toString())).openConnection();
                if (agentName != null) {
                    connection.setRequestProperty("User-Agent", agentName);
                }
                connection.setDoOutput(true);
                connection.setRequestMethod("GET");
                connection.connect();
            } catch (Throwable t) {
                throw new TAPIRException(this, "sendXMLRequest [GET]", t);
            }
        } else {
            try {
                if (url == null) {
                    url = new URL(accessPoint);
                }
                connection = (HttpURLConnection) url.openConnection();
                if (agentName != null) {
                    connection.setRequestProperty("User-Agent", agentName);
                }
                connection.setDoOutput(true);
                connection.setRequestMethod("POST");
                String request = null;
                if (encoding == Encoding.XML_RAW_POST) {
                    connection.setRequestProperty("Content-Type", "text/xml");
                } else if (encoding == Encoding.XML_POST) {
                    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    request = "request=";
                } else {
                    throw new TAPIRException(this, "sendXMLRequest: Incompatible TAPIR encoding to use with XML requests");
                }
                PrintStream out = new PrintStream(connection.getOutputStream());
                out.print((request == null) ? xml : request + xml);
                out.close();
            } catch (Throwable t) {
                throw new TAPIRException(this, "sendXMLRequest [POST]", t);
            }
        }
        return sendHTTPRequest(connection);
    }

    protected void loadCapabilities() throws TAPIRException {
        if (capabilitiesResponse == null) {
            capabilitiesResponse = capabilities();
        }
    }

    protected Object getPagingParameters(Boolean count, int start, int limit) {
        return (encoding > Encoding.KVP_POST) ? getXMLPagingParameters(count, start, limit) : getKVPPagingParameters(count, start, limit);
    }

    protected String getXMLPagingParameters(Boolean count, int start, int limit) {
        StringBuffer attributes = new StringBuffer();
        if (count) {
            attributes.append(" count=\"").append(count).append("\"");
        }
        if (start > 0) {
            attributes.append(" start=\"").append(start).append("\"");
        }
        if (limit > 0) {
            attributes.append(" limit=\"").append(limit).append("\"");
        }
        return attributes.toString();
    }

    protected Map<String, String> getKVPPagingParameters(Boolean count, int start, int limit) {
        Hashtable<String, String> parameters = new Hashtable<String, String>();
        if (count != null && count) {
            parameters.put(shortNames ? "cnt" : "count", count.booleanValue() ? "1" : "0");
        }
        if (start > 0) {
            parameters.put(shortNames ? "s" : "start", String.valueOf(start));
        }
        if (limit > 0) {
            parameters.put(shortNames ? "l" : "limit", String.valueOf(limit));
        }
        return parameters;
    }

    protected Object getExtraSearchParameters(Boolean envelope, Boolean omitNamespace) {
        return (encoding > Encoding.KVP_POST) ? getXMLExtraSearchParameters(envelope, omitNamespace) : getKVPExtraSearchParameters(envelope, omitNamespace);
    }

    protected String getXMLExtraSearchParameters(Boolean envelope, Boolean omitNamespace) {
        StringBuffer attributes = new StringBuffer();
        if (envelope != null) {
            attributes.append(" envelope=\"").append(envelope).append("\"");
        }
        if (omitNamespace != null) {
            attributes.append(" omit-Ns=\"").append(omitNamespace).append("\"");
        }
        return attributes.toString();
    }

    protected Map<String, String> getKVPExtraSearchParameters(Boolean envelope, Boolean omitNamespace) {
        Hashtable<String, String> parameters = new Hashtable<String, String>();
        if (envelope != null) {
            parameters.put(shortNames ? "e" : "envelope", envelope.booleanValue() ? "1" : "0");
        }
        if (omitNamespace != null) {
            parameters.put("omit-ns", omitNamespace.booleanValue() ? "1" : "0");
        }
        return parameters;
    }

    protected static XMLInputFactory getXMLInputFactory() {
        if (xmlInputFactory == null) {
            xmlInputFactory = XMLInputFactory.newInstance();
        }
        return xmlInputFactory;
    }

    protected static XMLStreamReader getXMLStreamReader(InputStream xml) {
        try {
            return getXMLInputFactory().createXMLStreamReader(xml);
        } catch (Throwable t) {
        }
        return null;
    }

    protected static String getText(XMLStreamReader xml) {
        try {
            return new String(xml.getTextCharacters(), xml.getTextStart(), xml.getTextLength());
        } catch (Throwable t) {
        }
        return null;
    }

    protected static void dumpXML(InputStream inputStream) {
        XMLStreamReader xml = getXMLStreamReader(inputStream);
        int attributeCount = 0;
        try {
            while (xml.hasNext()) {
                StringBuffer buffer = new StringBuffer();
                switch(xml.next()) {
                    case XMLStreamReader.START_ELEMENT:
                        buffer.append("START_ELEMENT: ");
                        buffer.append(xml.getName());
                        buffer.append(" has ");
                        int attrs = xml.getAttributeCount();
                        attributeCount = 0;
                        switch(attrs) {
                            case 0:
                                buffer.append(" no attribute");
                                break;
                            case 1:
                                buffer.append(" an attribute");
                                break;
                            default:
                                buffer.append(" ");
                                buffer.append(attrs);
                                buffer.append(" attributes");
                                break;
                        }
                        if (attrs > 0) {
                            for (int i = 0; i < attrs; i++) {
                                buffer.append("\n attribute[");
                                buffer.append(i);
                                buffer.append("]: ");
                                buffer.append(xml.getAttributeName(attributeCount++));
                            }
                        }
                        System.out.println(buffer.toString());
                        break;
                    case XMLStreamReader.ATTRIBUTE:
                        buffer.append("ATTRIBUTE: ");
                        buffer.append(xml.getAttributeName(attributeCount++));
                        System.out.println(buffer.toString());
                        break;
                    case XMLStreamReader.NAMESPACE:
                        buffer.append("NAMESPACE: ");
                        System.out.println(buffer.toString());
                        break;
                    case XMLStreamReader.END_ELEMENT:
                        buffer.append("END_ELEMENT: ");
                        buffer.append(xml.getName());
                        System.out.println(buffer.toString());
                        break;
                    case XMLStreamReader.CHARACTERS:
                        buffer.append("CHARACTERS: ");
                        buffer.append(getText(xml));
                        System.out.println(buffer.toString());
                        break;
                    case XMLStreamReader.CDATA:
                        buffer.append("CDATA: ");
                        buffer.append(getText(xml));
                        System.out.println(buffer.toString());
                        break;
                    case XMLStreamReader.COMMENT:
                        buffer.append("COMMENT: ");
                        buffer.append(getText(xml));
                        System.out.println(buffer.toString());
                        break;
                    case XMLStreamReader.SPACE:
                        buffer.append("SPACE: ");
                        buffer.append(getText(xml));
                        System.out.println(buffer.toString());
                        break;
                    case XMLStreamReader.START_DOCUMENT:
                        buffer.append("START_DOCUMENT: ");
                        buffer.append(xml.getEncoding());
                        buffer.append(" ");
                        buffer.append(xml.getVersion());
                        System.out.println(buffer.toString());
                        break;
                    case XMLStreamReader.END_DOCUMENT:
                        buffer.append("END_DOCUMENT");
                        System.out.println(buffer.toString());
                        break;
                    case XMLStreamReader.PROCESSING_INSTRUCTION:
                        buffer.append("PROCESSING_INSTRUCTION");
                        System.out.println(buffer.toString());
                        break;
                    case XMLStreamReader.ENTITY_REFERENCE:
                        buffer.append("ENTITY_REFERENCE: ");
                        buffer.append(getText(xml));
                        System.out.println(buffer.toString());
                        break;
                    case XMLStreamReader.DTD:
                        buffer.append("DTD: ");
                        break;
                }
            }
        } catch (Throwable e) {
        }
    }

    public static String capabilityString(boolean result, String capability) {
        StringBuffer buffer = new StringBuffer();
        if (result) buffer.append("supprots "); else buffer.append("does not supprot ");
        buffer.append(capability);
        return buffer.toString();
    }

    public static void main(String[] args) {
        if (args == null || args.length == 0) {
            System.out.println("Specify an endpoint URL");
            System.exit(1);
        }
        try {
            String endpoint = args[0];
            TAPIRClient client = new TAPIRClient(endpoint);
            if (client.ping().ok()) {
                System.out.println("TAPIR service on " + endpoint + " seems OK");
            } else {
                System.out.println("TAPIR service on " + endpoint + " does not response to a ping");
                System.exit(2);
            }
            MetadataResponse metadata = client.getMetadata();
            InputStream metadataXML = metadata.getXML();
            String conceptID = "http://www.tdwg.org/schemas/abcd/2.06/DataSets/DataSet/Units/Unit/Identifications/Identification/Result/TaxonIdentified/ScientificName/FullScientificNameString";
            if (!client.supportsInventoryOnAnyConcepts() || !client.mappedConcept(conceptID) || !client.supportsSearchWithAnyOutputModels()) {
                System.out.println("TAPIR provider " + endpoint + " does not support all capabilities necessary");
            }
            System.out.println("TAPIR provider " + endpoint + " supppots/unsupoors capablities as follows:");
            System.out.println("\t" + capabilityString(client.supportsXML(), "XML"));
            System.out.println("\t" + capabilityString(client.supportsInventory(), "Inventory"));
            System.out.println("\t" + capabilityString(client.supportsInventoryOnAnyConcepts(), "InventoryOnAnyConcepts"));
            Iterator<QueryTemplate> i = client.getInventoryTemplates();
            if (i == null || !i.hasNext()) {
                System.out.println("\t" + capabilityString(false, "InventoryTemplate"));
            } else {
                System.out.println("\tsupports following inventory template(s)");
                while (i.hasNext()) {
                    QueryTemplate template = i.next();
                    if (template != null) {
                        System.out.println("\t\t" + template.getLocation());
                    }
                }
            }
            System.out.println("\t" + capabilityString(client.supportsSearch(), "Search"));
            System.out.println("\t" + capabilityString(client.supportsSearchWithAnyOutputModels(), "SearchWithAnyOutputModels"));
            Iterator<OutputModel> knownModels = client.getKnownOutputModels();
            if (knownModels == null || !knownModels.hasNext()) {
                System.out.println("\t" + capabilityString(false, "any known output model"));
            } else {
                System.out.println("\tsupports following known model(s)");
                while (knownModels.hasNext()) {
                    OutputModel model = knownModels.next();
                    if (model != null) {
                        System.out.println("\t\t" + model.getLocation());
                    }
                }
            }
            i = client.getSearchTemplates();
            if (i == null || !i.hasNext()) {
                System.out.println("\t" + capabilityString(false, "SearchTemplate"));
            } else {
                System.out.println("\tsupports following search template(s)");
                while (i.hasNext()) {
                    QueryTemplate template = i.next();
                    if (template != null) {
                        System.out.println("\t\t" + template.getLocation());
                    }
                }
            }
            System.out.println("\t" + capabilityString(client.supportsFilter(), "Filter"));
            Iterator<MappedSchema> mapped = client.getMappedSchemata();
            if (mapped == null || !mapped.hasNext()) {
                System.out.println("\t" + capabilityString(false, "MappedSchemata"));
            } else {
                System.out.println("\tsupports following mapped schemata(s)");
                while (mapped.hasNext()) {
                    MappedSchema schema = mapped.next();
                    if (schema != null) {
                        System.out.println("\t\t" + schema.getLocation());
                    }
                }
            }
            Iterator<String> concepts = client.getMappedConcepts();
            if (concepts == null || !concepts.hasNext()) {
                System.out.println("\t" + capabilityString(false, "MappedConcept"));
            } else {
                System.out.println("\tsupports following mapped concept(s)");
                while (concepts.hasNext()) {
                    String concept = concepts.next();
                    if (concept != null) {
                        System.out.println("\t\t" + concept);
                    }
                }
            }
            Iterator<String> variables = client.getSupportedVariables();
            if (variables == null || !variables.hasNext()) {
                System.out.println("\t supports no variable");
            } else {
                System.out.println("\tsupports following variable(s)");
                while (variables.hasNext()) {
                    String variable = variables.next();
                    if (variable != null) {
                        System.out.println("\t\t" + variable);
                    }
                }
            }
            System.out.println("end of capablity section for TAPIR provider " + endpoint);
            ComparisonOperator comp = new ComparisonOperator("http://www.tdwg.org/schemas/abcd/2.06/DataSets/DataSet/Units/Unit/Identifications/Identification/Result/TaxonIdentified/ScientificName/FullScientificNameString", ComparisonOperator.COP_LIKE, Operand.EXP_LITERAL, "\"Luzu*\"");
            Filter filter = new Filter(comp);
            Hashtable<String, String> inventoryQuery = new Hashtable<String, String>();
            inventoryQuery.put("http://digir.net/schema/conceptual/darwin/2003/1.0#/Country", "http://digir.net/schema/conceptual/darwin/2003/1.0#/Country");
            inventoryQuery.put("http://digir.net/schema/conceptual/darwin/2003/1.0#/Genus", "http://digir.net/schema/conceptual/darwin/2003/1.0#/Genus");
            InventoryResponse inventory = client.getCustomInventory(inventoryQuery, null, 0, 10, filter);
            System.out.println(inventory.getTotalReturned() + " records found");
            Hashtable<String, Boolean> orderBy = new Hashtable<String, Boolean>();
            orderBy.put("http://www.tdwg.org/schemas/abcd/2.06/DataSets/DataSet/Units/Unit/Identifications/Identification/Result/TaxonIdentified/ScientificName/FullScientificNameString", Boolean.FALSE);
            SearchResponse search = client.customSearch("http://res.tdwg.org/tapir/models/abcd206.xml", null, null, 0, 10, null, null, filter, orderBy);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
