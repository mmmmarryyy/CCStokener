package pt.utl.ist.lucene.treceval.geotime.webservices;

import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;
import com.sun.tools.javac.util.Pair;
import com.vividsolutions.jts.geom.Geometry;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Logger;
import org.deegree.model.spatialschema.GMLGeometryAdapter;
import org.deegree.model.spatialschema.JTSAdapter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import pt.utl.ist.lucene.analyzer.LgteDiacriticFilter;
import pt.utl.ist.lucene.config.ConfigProperties;
import pt.utl.ist.lucene.utils.Dom4jUtil;
import pt.utl.ist.lucene.utils.placemaker.BelongTosDocument;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CallWebServices {

    static final String proxyHost = ConfigProperties.getProperty("proxy.host");

    static final int proxyPort = ConfigProperties.getIntProperty("proxy.port");

    private static final Logger logger = Logger.getLogger(CallWebServices.class);

    private static Proxy httpProxy = Proxy.NO_PROXY;

    protected static String yahooAppId = "AVNVvo3V34EOqIaAO7Uo.CrlQeGg8Ss43EhQfPm0HMZjqnkSUtA2MkhAiTkQ6T3XE6FWGg--";

    public static boolean test(byte[] b, String csnam) {
        CharsetDecoder cd = Charset.availableCharsets().get(csnam).newDecoder();
        try {
            cd.decode(ByteBuffer.wrap(b));
        } catch (CharacterCodingException e) {
            return false;
        }
        return true;
    }

    public static BelongTosDocument belongTos(String woeid) throws Exception {
        if (proxyHost != null && !proxyHost.equals("proxy.host")) {
            System.getProperties().setProperty("http.proxyHost", proxyHost);
            System.getProperties().setProperty("http.proxyPort", "" + proxyPort);
            InetSocketAddress addr = new InetSocketAddress(proxyHost, proxyPort);
            httpProxy = new Proxy(Proxy.Type.HTTP, addr);
        }
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        factory.setNamespaceAware(true);
        DocumentBuilder loader = factory.newDocumentBuilder();
        HttpClient client = new HttpClient();
        if (proxyHost != null && !proxyHost.equals("proxy.host")) client.getHostConfiguration().setProxy(proxyHost, proxyPort);
        try {
            URL url = new URL("http://where.yahooapis.com/v1/place/" + woeid + "/belongtos;count=0?appid=" + yahooAppId);
            org.dom4j.Document document = null;
            Node elmDest = null;
            try {
                document = Dom4jUtil.parse(url);
            } catch (Exception e) {
                logger.error("Trying this woeid");
                logger.error(woeid);
                throw e;
            }
            return new BelongTosDocument(woeid, document);
        } catch (Exception e) {
            logger.error(woeid + " " + e.toString(), e);
            throw e;
        }
    }

    public static org.w3c.dom.Document callServices(String data, String title, int year, int month, int day, String file, String id) throws Exception {
        return callServices(data, title, year, month, day, file, id, "en-EN");
    }

    public static org.w3c.dom.Document callServices(String data, String title, int year, int month, int day, String file, String id, String language) throws Exception {
        if (proxyHost != null && !proxyHost.equals("proxy.host")) {
            System.getProperties().setProperty("http.proxyHost", proxyHost);
            System.getProperties().setProperty("http.proxyPort", "" + proxyPort);
            InetSocketAddress addr = new InetSocketAddress(proxyHost, proxyPort);
            httpProxy = new Proxy(Proxy.Type.HTTP, addr);
        }
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        factory.setNamespaceAware(true);
        DocumentBuilder loader = factory.newDocumentBuilder();
        HttpClient client = new HttpClient();
        if (proxyHost != null && !proxyHost.equals("proxy.host")) client.getHostConfiguration().setProxy(proxyHost, proxyPort);
        try {
            String url = "http://wherein.yahooapis.com/v1/document";
            PostMethod post = new PostMethod(url);
            post.addParameter("documentType", "text/plain");
            post.addParameter("appid", yahooAppId);
            data = LgteDiacriticFilter.clean(data);
            post.addParameter("documentContent", data);
            if (title != null) post.addParameter("documentTitle", LgteDiacriticFilter.clean(title));
            post.addParameter("inputLanguage", language);
            post.setDoAuthentication(false);
            client.executeMethod(post);
            String response = post.getResponseBodyAsString();
            Document document = null;
            Node elmDest = null;
            try {
                document = loader.parse(new ByteArrayInputStream(response.getBytes("UTF-8")));
                elmDest = ((Element) (document.getFirstChild())).getElementsByTagName("document").item(0);
            } catch (NullPointerException ne) {
            } catch (Exception e) {
                logger.error("Trying this XML");
                logger.error(response);
                throw e;
            }
            if (elmDest == null) {
                BufferedReader reader = new BufferedReader(new StringReader(data));
                String line;
                StringBuilder dataBuilder = new StringBuilder();
                while ((line = reader.readLine()) != null && !line.toUpperCase().equals("</DOC>")) {
                    if (test(line.getBytes(), "UTF-8")) {
                        dataBuilder.append(line).append("\n");
                    } else {
                        StringBuilder lineBuilder = new StringBuilder();
                        for (int i = 0; i < line.length(); i++) {
                            char c = line.charAt(i);
                            if (!test(new String("" + c).getBytes(), "UTF-8")) {
                                lineBuilder.append("?");
                            } else {
                                lineBuilder.append(c);
                            }
                        }
                        logger.error("BAD character encoding at line: " + line + " using line: " + lineBuilder.toString());
                        dataBuilder.append(lineBuilder.toString());
                    }
                }
                data = dataBuilder.toString();
                post.releaseConnection();
                post = new PostMethod(url);
                post.addParameter("documentType", "text/plain");
                post.addParameter("appid", yahooAppId);
                data = LgteDiacriticFilter.clean(data);
                post.addParameter("documentContent", data);
                if (title != null) post.addParameter("documentTitle", LgteDiacriticFilter.clean(title));
                post.addParameter("inputLanguage", "en-EN");
                post.setDoAuthentication(false);
                client.executeMethod(post);
                response = post.getResponseBodyAsString();
                try {
                    document = loader.parse(new ByteArrayInputStream(response.getBytes("UTF-8")));
                    elmDest = ((Element) (document.getFirstChild())).getElementsByTagName("document").item(0);
                } catch (Exception e) {
                    logger.error("Trying this XML");
                    logger.error(response);
                    throw e;
                }
            }
            post.releaseConnection();
            String geo = getGeometryString(document);
            if (geo.indexOf("<gml:X>-180</gml:X>") >= 0) logger.warn("Document " + id + " have zero Places: scope: " + geo);
            String xml = geo.toString();
            logger.info(xml);
            Document docGmlBox = loader.parse(new ByteArrayInputStream(xml.getBytes()));
            if (elmDest == null) {
                logger.error("PlaceMaker dont did not bring doc tag >>>>>>");
                logger.error(xml);
            }
            Element box = (Element) document.importNode(docGmlBox.getFirstChild(), true);
            if (box == null) {
                logger.error("PlaceMaker geometry did not bring Geometry >>>>>>");
                logger.error(geo);
            }
            if (elmDest != null) elmDest.appendChild(box);
            return document;
        } catch (Exception e) {
            logger.error(id + " - @ " + file + ":" + e.toString(), e);
            throw e;
        }
    }

    public static org.w3c.dom.Document callTimextag(String url, String xml, String title, int year, int month, int day, String file, String id) throws Exception {
        if (xml.indexOf("-------------------------------") >= 0) xml.replaceAll("-------------------------------", "                               ");
        if (proxyHost != null && !proxyHost.equals("proxy.host")) {
            System.getProperties().setProperty("http.proxyHost", proxyHost);
            System.getProperties().setProperty("http.proxyPort", "" + proxyPort);
            InetSocketAddress addr = new InetSocketAddress(proxyHost, proxyPort);
            httpProxy = new Proxy(Proxy.Type.HTTP, addr);
        }
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        factory.setNamespaceAware(true);
        DocumentBuilder loader = factory.newDocumentBuilder();
        HttpClient client = new HttpClient();
        if (proxyHost != null && !proxyHost.equals("proxy.host")) client.getHostConfiguration().setProxy(proxyHost, proxyPort);
        try {
            PostMethod post = new PostMethod(url);
            xml = xml.replace("\\R", " ").replace("\\S", " ").replace("\\T", " ").replace("\\N", " ").replace("\\", "/").replace("Eastern", "east").replace("millennia ago", " ");
            post.addParameter("debug", "false");
            post.addParameter("service", "sgml2Timexes");
            post.addParameter("t", "1");
            post.addParameter("input", xml);
            post.setDoAuthentication(false);
            client.setTimeout(20000);
            client.executeMethod(post);
            String response = post.getResponseBodyAsString();
            Document document;
            try {
                document = loader.parse(new ByteArrayInputStream(response.getBytes("UTF-8")));
            } catch (Exception e) {
                logger.error("Parsing this XML:");
                logger.error(xml);
                logger.error("Response from tagger:");
                logger.error(response);
                throw e;
            }
            post.releaseConnection();
            return document;
        } catch (Exception e) {
            logger.error(id + " - @ " + file + ":" + e.toString(), e);
            throw e;
        }
    }

    protected static List<Pair<String, String>> proxy;

    public static void setRandomProxy() {
        if (proxy == null) {
            proxy = new ArrayList<Pair<String, String>>();
        }
        int aux = (int) (Math.random() * proxy.size());
        if (aux == proxy.size()) {
            System.getProperties().put("http.proxySet", "false");
        } else {
            Pair<String, String> p = proxy.get(aux);
            System.getProperties().put("http.proxySet", "true");
            System.setProperty("http.proxyHost", p.fst);
            System.setProperty("http.proxyPort", p.snd);
        }
    }

    private static Geometry getGeometry(Document doc) throws Exception {
        return JTSAdapter.export(GMLGeometryAdapter.wrap(getGeometryString(doc)));
    }

    private static Geometry getGeometry(String geometry) throws Exception {
        return JTSAdapter.export(GMLGeometryAdapter.wrap(geometry));
    }

    private static String getGeometryString(Document doc) throws Exception {
        NamespaceContext ctx = new NamespaceContext() {

            public String getNamespaceURI(String prefix) {
                String uri;
                if (prefix.equals("yahoo")) uri = "http://www.yahooapis.com/v1/base.rng"; else if (prefix.equals("ys")) uri = "http://wherein.yahooapis.com/v1/schema"; else if (prefix.equals("ys2")) uri = "http://where.yahooapis.com/v1/schema.rng"; else if (prefix.equals("gml")) uri = "http://www.opengis.net/gml"; else uri = null;
                return uri;
            }

            public Iterator getPrefixes(String val) {
                return null;
            }

            public String getPrefix(String uri) {
                return null;
            }
        };
        try {
            setRandomProxy();
            List<Geometry> list = new ArrayList<com.vividsolutions.jts.geom.Geometry>();
            NodeList lst = doc.getDocumentElement().getElementsByTagName("gml:Box");
            StringWriter sw = new StringWriter();
            ByteArrayOutputStream w = new ByteArrayOutputStream();
            if (lst.getLength() != 0) {
                printXML((Element) lst.item(0), new PrintStream(w));
                return new String(w.toByteArray());
            }
            javax.xml.xpath.XPathFactory factory = javax.xml.xpath.XPathFactory.newInstance();
            javax.xml.xpath.XPath xpath = factory.newXPath();
            xpath.setNamespaceContext(ctx);
            DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();
            dfactory.setValidating(false);
            dfactory.setNamespaceAware(true);
            DocumentBuilder loader = dfactory.newDocumentBuilder();
            printXML(doc.getDocumentElement(), new PrintStream(w));
            Document doc2 = loader.parse(new ByteArrayInputStream(w.toByteArray()));
            String name = xpath.compile("//ys:geographicScope/ys:name/text()").evaluate(doc2);
            String woeid = xpath.compile("//ys:geographicScope/ys:woeId/text()").evaluate(doc2);
            String swLat = xpath.compile("//ys:extents/ys:southWest/ys:latitude/text()").evaluate(doc2);
            String swLon = xpath.compile("//ys:extents/ys:southWest/ys:longitude/text()").evaluate(doc2);
            String neLat = xpath.compile("//ys:extents/ys:northEast/ys:latitude/text()").evaluate(doc2);
            String neLon = xpath.compile("//ys:extents/ys:northEast/ys:longitude/text()").evaluate(doc2);
            List<String> parents = new ArrayList<String>();
            if (woeid != null && woeid.trim().length() > 0 && !woeid.trim().equals("1")) {
                if (woeid.equals("2461607") || woeid.equals("55959673")) {
                    woeid = "55959673";
                    name = "North Sea";
                }
                try {
                    String url = "http://where.yahooapis.com/v1/place/" + woeid + "/belongtos;count=0?appid=" + yahooAppId;
                    HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection(httpProxy);
                    doc2 = loader.parse(conn.getInputStream());
                    NodeList auxp = (NodeList) xpath.compile("//ys2:place/ys2:woeid/text()").evaluate(doc2, XPathConstants.NODESET);
                    for (int i = auxp.getLength() - 1; i >= 0; i--) parents.add(auxp.item(i).getNodeValue().trim());
                    parents.add(woeid);
                    conn.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    String url = "http://where.yahooapis.com/v1/place/" + woeid + "?appid=" + yahooAppId;
                    HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection(httpProxy);
                    doc2 = loader.parse(conn.getInputStream());
                    swLat = xpath.compile("//ys2:boundingBox/ys2:southWest/ys2:latitude/text()").evaluate(doc2);
                    swLon = xpath.compile("//ys2:boundingBox/ys2:southWest/ys2:longitude/text()").evaluate(doc2);
                    neLat = xpath.compile("//ys2:boundingBox/ys2:northEast/ys2:latitude/text()").evaluate(doc2);
                    neLon = xpath.compile("//ys2:boundingBox/ys2:northEast/ys2:longitude/text()").evaluate(doc2);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (swLat == null || swLat.length() == 0) sw.write("<gml:Box xmlns:gml='http://www.opengis.net/gml'><gml:coord><gml:X>-180</gml:X><gml:Y>-90</gml:Y></gml:coord><gml:coord><gml:X>180</gml:X><gml:Y>90</gml:Y></gml:coord></gml:Box>"); else sw.write("<gml:Box xmlns:gml='http://www.opengis.net/gml'><gml:coord><gml:X>" + swLon + "</gml:X><gml:Y>" + swLat + "</gml:Y></gml:coord><gml:coord><gml:X>" + neLon + "</gml:X><gml:Y>" + neLat + "</gml:Y></gml:coord></gml:Box>");
            return sw.toString();
        } catch (Throwable e) {
            e.printStackTrace();
            StringWriter sw = new StringWriter();
            sw.write("<gml:Box xmlns:gml='http://www.opengis.net/gml'><gml:coord><gml:X>-180</gml:X><gml:Y>-90</gml:Y></gml:coord><gml:coord><gml:X>180</gml:X><gml:Y>90</gml:Y></gml:coord></gml:Box>");
            return sw.toString();
        }
    }

    public static void printXML(Element elm, PrintStream out) throws Exception {
        OutputFormat of = new OutputFormat("XML", "UTF-8", true);
        of.setIndent(1);
        of.setIndenting(true);
        XMLSerializer serializer = new XMLSerializer(out, of);
        serializer.asDOMSerializer();
        serializer.serialize(elm);
    }

    public static void main(String args[]) throws Exception {
        String data = "When and where were the Washington beltway snipers arrested";
        String title = "Test title";
        Document doc = callServices(data, title, 2009, 11, 11, "test", "test");
        serializeDoc(System.out, doc);
    }

    private static void serializeDoc(OutputStream stream, Document doc) throws IOException {
        OutputFormat of = new OutputFormat("XML", "UTF-8", true);
        of.setIndent(1);
        of.setIndenting(true);
        XMLSerializer serializer = new XMLSerializer(stream, of);
        serializer.asDOMSerializer();
        serializer.serialize(doc.getDocumentElement());
    }

    private static void serializeDoc(Writer writer, Document doc) throws IOException {
        OutputFormat of = new OutputFormat("XML", "UTF-8", true);
        of.setIndent(1);
        of.setIndenting(true);
        XMLSerializer serializer = new XMLSerializer(writer, of);
        serializer.asDOMSerializer();
        serializer.serialize(doc.getDocumentElement());
    }
}
