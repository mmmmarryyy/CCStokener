package org.lindenb.freebase;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Properties;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.lindenb.io.IOUtils;
import org.lindenb.json.JSONParser;
import org.lindenb.sw.vocabulary.XHTML;
import org.lindenb.sw.vocabulary.XSD;
import org.lindenb.sw.vocabulary.XSI;
import org.lindenb.util.Compilation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class FreebaseCall {

    private static final String COOKIE = "metaweb-user";

    private String apiService = "mqlread";

    private boolean useTestServer = false;

    private String cookie = null;

    private FreebaseCall() {
        setUsingTestServer(true);
        setAPIService("mqlread");
    }

    public void setUsingTestServer(boolean useTestServer) {
        this.useTestServer = useTestServer;
    }

    public boolean isUsingTestServer() {
        return useTestServer;
    }

    public void setAPIService(String apiService) {
        this.apiService = apiService;
    }

    public String getAPIService() {
        return apiService;
    }

    public void setCookie(String cookie) {
        this.cookie = cookie;
    }

    public String getCookie() {
        return cookie;
    }

    private static Document convertJson2Xml(Object json) {
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setCoalescing(true);
            f.setExpandEntityReferences(true);
            f.setIgnoringComments(true);
            f.setIgnoringElementContentWhitespace(true);
            f.setValidating(false);
            f.setNamespaceAware(true);
            DocumentBuilder b = f.newDocumentBuilder();
            Document dom = b.newDocument();
            Element n = json2xml(dom, json);
            n.setAttribute("xmlns", XHTML.NS);
            n.setAttribute("xmlns:xsi", XSI.NS);
            n.setAttribute("xmlns:xsd", XSD.NS);
            dom.appendChild(n);
            return dom;
        } catch (FactoryConfigurationError e) {
            throw new RuntimeException(e);
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    private static Element json2xml(Document dom, Object json) {
        if (json == null) {
            return dom.createElementNS(XHTML.NS, "span");
        } else if ((json instanceof Map<?, ?>)) {
            Element dl = dom.createElementNS(XHTML.NS, "dl");
            Map<?, ?> map = Map.class.cast(json);
            for (Object key : map.keySet()) {
                Element dt = dom.createElementNS(XHTML.NS, "dt");
                dl.appendChild(dt);
                dt.appendChild(dom.createTextNode(key.toString()));
                Element dd = dom.createElementNS(XHTML.NS, "dd");
                dl.appendChild(dd);
                dd.appendChild(json2xml(dom, map.get(key)));
            }
            return dl;
        } else if ((json instanceof Iterable<?>)) {
            Element ul = dom.createElementNS(XHTML.NS, "ul");
            Iterable<?> array = Iterable.class.cast(json);
            for (Object o : array) {
                Element li = dom.createElementNS(XHTML.NS, "li");
                ul.appendChild(li);
                li.appendChild(json2xml(dom, o));
            }
            return ul;
        }
        String xsi = null;
        if ((json instanceof Boolean)) {
            xsi = "xsd:boolean";
        } else if ((json instanceof Short)) {
            xsi = "xsd:short";
        } else if ((json instanceof Integer)) {
            xsi = "xsd:integer";
        } else if ((json instanceof Long)) {
            xsi = "xsd:long";
        } else if ((json instanceof Float)) {
            xsi = "xsd:float";
        } else if ((json instanceof Double)) {
            xsi = "xsd:double";
        } else if ((json instanceof String) || (json instanceof Character)) {
            xsi = null;
        } else {
            throw new IllegalArgumentException("Cannot convert " + json.getClass() + " to XML");
        }
        Element span = dom.createElementNS(XHTML.NS, "span");
        if (xsi != null) span.setAttributeNS(XSI.NS, "xsi:type", xsi);
        span.appendChild(dom.createTextNode(json.toString()));
        return span;
    }

    /** performs a query over www.rebase.com */
    public String send(String json) throws IOException {
        String urlStr = "http://" + (isUsingTestServer() ? "test" : "www") + ".freebase.com/api/service/" + getAPIService();
        URL url = new URL(urlStr);
        URLConnection connection = url.openConnection();
        connection.setRequestProperty("Cookie", COOKIE + "=" + "\"" + getCookie() + "\"");
        connection.setRequestProperty("X-Metaweb-Request", "HelloMetaweb");
        connection.setDoOutput(true);
        OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
        out.write("queries=" + URLEncoder.encode(json, "UTF-8"));
        out.close();
        InputStream in = connection.getInputStream();
        String result = IOUtils.getReaderContent(new InputStreamReader(in));
        in.close();
        return result;
    }

    public Document send2xml(String json) throws IOException {
        return convertJson2Xml(new JSONParser(send(json)));
    }

    public static void main(String[] args) {
        Properties preferences = new Properties();
        FreebaseCall app = new FreebaseCall();
        File prefFile = new File(System.getProperty("user.home"), ".metaweb.xml");
        String envelope = null;
        boolean xmlOuput = false;
        boolean sideBySide = false;
        try {
            if (!prefFile.exists()) {
                System.err.println("Cannot get " + prefFile + "\nThis file should look like this:\n" + "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<!DOCTYPE properties SYSTEM \"http://java.sun.com/dtd/properties.dtd\">\n" + "<properties>\n" + "       <entry key=\"" + COOKIE + "\">xxxxx</entry>\n" + "</properties>\n");
            } else {
                InputStream in = new FileInputStream(prefFile);
                preferences.loadFromXML(in);
                in.close();
            }
            int optind = 0;
            while (optind < args.length) {
                if (args[optind].equals("-h")) {
                    System.err.println(Compilation.getLabel());
                    System.err.println(" -c <string> metaweb cookie (or content of ${home}/.metaweb  will be used)");
                    System.err.println(" -e <enveloppe: qualified name> add an envelope {\"qname\":{\"query\":... }} around your query");
                    System.err.println(" -E same as -e but qname is random");
                    System.err.println(" -x xml output");
                    System.err.println(" -s <service> default: read");
                    System.err.println(" -w same as s but use mqlwrite");
                    System.err.println(" -t use test server");
                    System.err.println(" -y display side by side query/result");
                    return;
                } else if (args[optind].equals("-t")) {
                    app.setUsingTestServer(true);
                } else if (args[optind].equals("-y")) {
                    sideBySide = true;
                } else if (args[optind].equals("-c")) {
                    preferences.setProperty(COOKIE, args[++optind]);
                } else if (args[optind].equals("-e")) {
                    envelope = args[++optind];
                } else if (args[optind].equals("-E")) {
                    envelope = "qname" + System.currentTimeMillis();
                } else if (args[optind].equals("-x")) {
                    xmlOuput = true;
                } else if (args[optind].equals("-s")) {
                    app.setAPIService(args[++optind].trim());
                } else if (args[optind].equals("-w")) {
                    app.setAPIService("mqlwrite");
                } else if (args[optind].equals("--")) {
                    optind++;
                    break;
                } else if (args[optind].startsWith("-")) {
                    System.err.println("Unknown option " + args[optind]);
                } else {
                    break;
                }
                ++optind;
            }
            if (!preferences.containsKey(COOKIE)) {
                System.err.println("Value of Cookie \"" + COOKIE + "\" was not specified");
                return;
            } else {
                app.setCookie(preferences.getProperty(COOKIE));
            }
            String json = null;
            if (optind == args.length) {
                json = IOUtils.getReaderContent(new InputStreamReader(System.in));
            } else if (optind + 1 == args.length) {
                json = IOUtils.getFileContent(new File(args[optind]));
            } else {
                System.err.println("Illegal number of arguments.");
                return;
            }
            if (envelope != null) {
                json = "{\"" + envelope + "\":{\"query\":" + json + "}}";
            }
            String result = app.send(json);
            if (xmlOuput) {
                Document doc = convertJson2Xml(new JSONParser(result));
                Transformer xformer = TransformerFactory.newInstance().newTransformer();
                xformer.setOutputProperty(OutputKeys.METHOD, "xml");
                xformer.setOutputProperty(OutputKeys.INDENT, "yes");
                xformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                xformer.transform(new DOMSource(doc), new StreamResult(System.out));
            } else {
                if (!sideBySide) {
                    System.out.print(result);
                } else {
                    String array1[] = json.split("[\n]");
                    String array2[] = result.split("[\n]");
                    int len = 2;
                    for (String s : array1) len = Math.max(len, s.length());
                    for (int i = 0; i < Math.max(array1.length, array2.length); ++i) {
                        String s1 = (i < array1.length ? array1[i] : "");
                        while (s1.length() < len) s1 += " ";
                        String s2 = (i < array2.length ? array2[i] : "");
                        System.out.println(s1 + " | " + s2);
                    }
                }
            }
        } catch (Throwable err) {
            err.printStackTrace();
        }
    }
}
