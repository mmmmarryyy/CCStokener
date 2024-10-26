package com.jettmarks.openid;

import org.apache.log4j.Logger;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Takes token and turns it into the user information matching the token.
 * 
 * This class comes from sample code provided by http://rpxwiki.com/.
 * 
 * @author jett
 */
public class RPX {

    /**
   * Logger for this class
   */
    private static final Logger logger = Logger.getLogger(RPX.class);

    private String apiKey;

    private String baseUrl;

    public static final String openIDapiKey = System.getProperty("org.rpxnow.appid.bkthn", "1eaaf21cf6e07deff1e57e11f6689c991f897e34");

    public RPX(String baseUrl) {
        this(openIDapiKey, baseUrl);
    }

    public RPX(String apiKey, String baseUrl) {
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        logger.debug("APIKey: " + this.apiKey);
        logger.debug("baseURL: " + this.baseUrl);
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public Element authInfo(String token) {
        Map<String, Object> query = new HashMap<String, Object>();
        query.put("token", token);
        return apiCall("auth_info", query);
    }

    public HashMap<String, List<String>> allMappings() {
        Element rsp = apiCall("all_mappings", null);
        HashMap<String, List<String>> result = new HashMap<String, List<String>>();
        NodeList mappings = getNodeList("/rsp/mappings/mapping", rsp);
        for (int i = 0; i < mappings.getLength(); i++) {
            Element mapping = (Element) mappings.item(i);
            List<String> identifiers = new ArrayList<String>();
            NodeList rk_list = getNodeList("primaryKey", mapping);
            NodeList id_list = getNodeList("identifiers/identifier", mapping);
            String remote_key = ((Element) rk_list.item(0)).getTextContent();
            for (int j = 0; j < id_list.getLength(); j++) {
                Element ident = (Element) id_list.item(j);
                identifiers.add(ident.getTextContent());
            }
            result.put(remote_key, identifiers);
        }
        return result;
    }

    private NodeList getNodeList(String xpath_expr, Element root) {
        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();
        try {
            return (NodeList) xpath.evaluate(xpath_expr, root, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            return null;
        }
    }

    public List<String> mappings(Object primaryKey) {
        Map<String, Object> query = new HashMap<String, Object>();
        query.put("primaryKey", primaryKey);
        Element rsp = apiCall("mappings", query);
        Element oids = (Element) rsp.getFirstChild();
        List<String> result = new ArrayList<String>();
        NodeList nl = oids.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Element e = (Element) nl.item(i);
            result.add(e.getTextContent());
        }
        return result;
    }

    public void map(String identifier, Object primaryKey) {
        Map<String, Object> query = new HashMap<String, Object>();
        query.put("identifier", identifier);
        query.put("primaryKey", primaryKey);
        apiCall("map", query);
    }

    public void unmap(String identifier, Object primaryKey) {
        Map<String, Object> query = new HashMap<String, Object>();
        query.put("identifier", identifier);
        query.put("primaryKey", primaryKey);
        apiCall("unmap", query);
    }

    @SuppressWarnings("unchecked")
    private Element apiCall(String methodName, Map<String, Object> partialQuery) {
        Map<String, Object> query = null;
        if (partialQuery == null) {
            query = new HashMap<String, Object>();
        } else {
            query = new HashMap<String, Object>(partialQuery);
        }
        query.put("format", "xml");
        query.put("apiKey", apiKey);
        StringBuffer sb = new StringBuffer();
        for (Iterator<?> it = query.entrySet().iterator(); it.hasNext(); ) {
            if (sb.length() > 0) {
                sb.append('&');
            }
            try {
                Map.Entry e = (Map.Entry) it.next();
                if (e.getValue() != null) {
                    sb.append(URLEncoder.encode(e.getKey().toString(), "UTF-8"));
                    sb.append('=');
                    sb.append(URLEncoder.encode(e.getValue().toString(), "UTF-8"));
                }
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("Unexpected encoding error", e);
            }
        }
        String data = sb.toString();
        try {
            URL url = new URL(baseUrl + "/api/v2/" + methodName);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.connect();
            OutputStreamWriter osw = new OutputStreamWriter(conn.getOutputStream(), "UTF-8");
            osw.write(data);
            osw.close();
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setIgnoringElementContentWhitespace(true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(conn.getInputStream());
            Element response = (Element) doc.getFirstChild();
            if (!response.getAttribute("stat").equals("ok")) {
                throw new RuntimeException("Unexpected API error");
            }
            return response;
        } catch (MalformedURLException e) {
            throw new RuntimeException("Unexpected URL error", e);
        } catch (IOException e) {
            throw new RuntimeException("Unexpected IO error", e);
        } catch (ParserConfigurationException e) {
            throw new RuntimeException("Unexpected XML error", e);
        } catch (SAXException e) {
            throw new RuntimeException("Unexpected XML error", e);
        }
    }

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: Rpx <api key> <RPX service URL> <map|unmap|mappings|all_mappings> [...]");
            System.exit(1);
        }
        RPX r = new RPX(args[0], args[1]);
        if (args[2].equals("mappings")) {
            System.out.println("Mappings for " + args[3] + ":");
            System.out.println(r.mappings(args[3]));
        }
        if (args[2].equals("map")) {
            System.out.println(args[3] + " mapped to " + args[4]);
            r.map(args[3], args[4]);
        }
        if (args[2].equals("unmap")) {
            System.out.println(args[3] + " unmapped from " + args[4]);
            r.unmap(args[3], args[4]);
        }
        if (args[2].equals("all_mappings")) {
            System.out.println("All mappings:");
            System.out.println(r.allMappings().toString());
        }
    }
}
