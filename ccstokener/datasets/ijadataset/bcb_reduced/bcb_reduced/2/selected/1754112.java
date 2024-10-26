package experiments;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.deegree.model.spatialschema.GMLGeometryAdapter;
import org.deegree.model.spatialschema.JTSAdapter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;
import com.sun.tools.javac.util.Pair;
import com.vividsolutions.jts.algorithm.distance.DiscreteHausdorffDistance;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Coordinate;
import com.wcohen.ss.BasicStringWrapper;
import com.wcohen.ss.BasicStringWrapperIterator;
import com.wcohen.ss.api.StringWrapper;
import com.wcohen.ss.api.StringWrapperIterator;
import com.wcohen.ss.tokens.NGramTokenizer;
import com.wcohen.ss.tokens.SimpleTokenizer;

public class ProcessGeoCLEFCollections {

    protected static String yahooAppId = "AVNVvo3V34EOqIaAO7Uo.CrlQeGg8Ss43EhQfPm0HMZjqnkSUtA2MkhAiTkQ6T3XE6FWGg--";

    protected static List<Pair<String, String>> proxy;

    protected static List<Pair<String, Pair<String, Geometry>>> topics;

    protected Map<String, Map<String, Boolean>> relevanceTopics = new HashMap<String, Map<String, Boolean>>();

    protected Map<String, Map<String, Boolean>> relevanceDocuments = new HashMap<String, Map<String, Boolean>>();

    protected Map<String, Set<String>> judgedDocuments = new HashMap<String, Set<String>>();

    protected TextSimilarityScorer tfidf;

    protected TextSimilarityScorer tfidfh;

    protected int numDocumentsTest = -1;

    public ProcessGeoCLEFCollections() throws Exception {
        tfidf = new TextSimilarityScorer(new SimpleTokenizer(true, true));
        tfidfh = new TextSimilarityScorer(new SimpleTokenizer(true, true));
        proxy = new ArrayList<Pair<String, String>>();
        topics = new ArrayList<Pair<String, Pair<String, Geometry>>>();
        File dtd = new File("geomarkup.dtd");
        dtd.createNewFile();
        dtd.deleteOnExit();
    }

    public static void printXML(Element elm, PrintStream out) throws Exception {
        OutputFormat of = new OutputFormat("XML", "UTF-8", true);
        of.setIndent(1);
        of.setIndenting(true);
        XMLSerializer serializer = new XMLSerializer(out, of);
        serializer.asDOMSerializer();
        serializer.serialize(elm);
    }

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

    private com.vividsolutions.jts.geom.Geometry getGeometry(Document doc) throws Exception {
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
            List<com.vividsolutions.jts.geom.Geometry> list = new ArrayList<com.vividsolutions.jts.geom.Geometry>();
            NodeList lst = doc.getDocumentElement().getElementsByTagName("gml:Box");
            StringWriter sw = new StringWriter();
            ByteArrayOutputStream w = new ByteArrayOutputStream();
            if (lst.getLength() != 0) return JTSAdapter.export(GMLGeometryAdapter.wrap((Element) (lst.item(0))));
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
                    HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
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
                    HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                    doc2 = loader.parse(conn.getInputStream());
                    swLat = xpath.compile("//ys2:boundingBox/ys2:southWest/ys2:latitude/text()").evaluate(doc2);
                    swLon = xpath.compile("//ys2:boundingBox/ys2:southWest/ys2:longitude/text()").evaluate(doc2);
                    neLat = xpath.compile("//ys2:boundingBox/ys2:northEast/ys2:latitude/text()").evaluate(doc2);
                    neLon = xpath.compile("//ys2:boundingBox/ys2:northEast/ys2:longitude/text()").evaluate(doc2);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (swLat == null || swLat.length() == 0 || sw.toString().trim().length() == 0) sw.write("<gml:Box xmlns:gml='http://www.opengis.net/gml'><gml:coord><gml:X>-180</gml:X><gml:Y>-90</gml:Y></gml:coord><gml:coord><gml:X>180</gml:X><gml:Y>90</gml:Y></gml:coord></gml:Box>"); else sw.write("<gml:Box xmlns:gml='http://www.opengis.net/gml'><gml:coord><gml:X>" + swLon + "</gml:X><gml:Y>" + swLat + "</gml:Y></gml:coord><gml:coord><gml:X>" + neLon + "</gml:X><gml:Y>" + neLat + "</gml:Y></gml:coord></gml:Box>");
            Geometry geo = JTSAdapter.export(GMLGeometryAdapter.wrap(sw.toString()));
            geo.setUserData(new Pair<String, List<String>>(name, parents));
            return geo;
        } catch (Throwable e) {
            e.printStackTrace();
            StringWriter sw = new StringWriter();
            sw.write("<gml:Box xmlns:gml='http://www.opengis.net/gml'><gml:coord><gml:X>-180</gml:X><gml:Y>-90</gml:Y></gml:coord><gml:coord><gml:X>180</gml:X><gml:Y>90</gml:Y></gml:coord></gml:Box>");
            Geometry geo = JTSAdapter.export(GMLGeometryAdapter.wrap(sw.toString()));
            List<String> parents = new ArrayList<String>();
            parents.add("1");
            geo.setUserData(new Pair<String, List<String>>("World", parents));
            return geo;
        }
    }

    public double geometrySimilarity(Geometry g1, Geometry g2, int formula) {
        Geometry overlap = g1.difference(g1.difference(g2));
        Geometry union = g1.union(g2);
        double a1 = g1.getArea();
        double a2 = g2.getArea();
        double ao = overlap.getArea();
        double au = union.getArea();
        switch(formula) {
            case 1:
                return 2.0 * ao / (a1 + a2);
            case 2:
                return Math.min(ao / a1, ao / a2);
            case 3:
                if (g1.contains(g2)) return a2 / a1; else if (g2.contains(g1)) return a1 / a2; else if (ao > 0) return ((ao / a1) * 100.0) / (((1 - ao / a2) * 100.0) + 100.0); else return 0;
            case 4:
                return ao / au;
            case 5:
                return 1.0 / (1.0 + Math.exp(0.5 * (ao / a1) + 0.5 * (ao / a2)));
            case 6:
                if (g2.contains(g1)) return 1.0; else {
                    Envelope e = g2.getEnvelopeInternal();
                    double d = Math.sqrt(e.getHeight() * e.getHeight() + e.getWidth() * e.getWidth());
                    double D = g1.getCentroid().distance(g2.getCentroid()) - d;
                    return 1 - ((1 + Math.signum(D) * (1.0 - Math.exp(0 - Math.pow(D / (d * 0.5), 2.0)))) / 2.0);
                }
            case 7:
                return new DiscreteHausdorffDistance(g1, g2).distance();
            case 8:
                List<String> t1 = (List<String>) (((Pair) (g1.getUserData())).snd);
                List<String> t2 = (List<String>) (((Pair) (g2.getUserData())).snd);
                double score = 0;
                score += t1 == null || t1.size() == 0 ? 1.0 : 1.0 / t1.size();
                score += t2 == null || t2.size() == 0 ? 1.0 : 1.0 / t2.size();
                if (t1 != null) for (int i = 0; i < t1.size(); i++) if (t2 == null || !t2.contains(t1.get(i))) score += 1.0 / (i + 1);
                if (t2 != null) for (int i = 0; i < t2.size(); i++) if (t1 == null || !t1.contains(t2.get(i))) score += 1.0 / (i + 1);
                return score;
            default:
                return g1.distance(g2);
        }
    }

    public void addTopics(String file) throws Exception {
        String lang = file.toLowerCase().endsWith("pt") ? "pt-PT" : "en-EN";
        addTopics(file, lang);
    }

    public void addTopics(String file, String lang) throws Exception {
        if (new File(file).isDirectory()) {
            File files[] = new File(file).listFiles();
            for (int i = 0; i < files.length; i++) if (files[i].getName().endsWith(".xml")) addTopics(files[i].getAbsolutePath(), lang);
        } else {
            System.out.println("Adding topics from file " + file);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder loader = factory.newDocumentBuilder();
            Document document = loader.parse(new File(file));
            Element tree = document.getDocumentElement();
            NodeList nodes = tree.getElementsByTagName("top");
            for (int j = 0; j < nodes.getLength() && (((Element) (nodes.item(j))).getElementsByTagName("title")).getLength() > 0; j++) {
                String topic = ((Element) (nodes.item(j))).getElementsByTagName("title").item(0).getFirstChild().toString();
                String id = ((Element) (nodes.item(j))).getElementsByTagName("num").item(0).getFirstChild().toString().trim();
                String description = ((Element) (nodes.item(j))).getElementsByTagName("desc").item(0).getFirstChild().toString() + ". " + ((Element) (nodes.item(j))).getElementsByTagName("narr").item(0).getFirstChild().toString();
                Geometry geo = getGeometry(yahooGeocode(description, topic, lang));
                if (geo == null) getGeometry(yahooGeocode(topic, topic, lang));
                topics.add(new Pair<String, Pair<String, Geometry>>(id.trim(), new Pair<String, Geometry>(topic.toLowerCase().trim(), geo)));
            }
            nodes = tree.getElementsByTagName("topic");
            for (int j = 0; j < nodes.getLength(); j++) {
                String topic = ((Element) ((Element) (nodes.item(j))).getElementsByTagName("title").item(0)).getFirstChild().toString();
                String id = ((Element) (nodes.item(j))).getElementsByTagName("identifier").item(0).getFirstChild().toString().trim();
                String description = ((Element) (nodes.item(j))).getElementsByTagName("description").item(0).getFirstChild().toString() + ". " + ((Element) (nodes.item(j))).getElementsByTagName("narrative").item(0).getFirstChild().toString();
                Geometry geo = getGeometry(yahooGeocode(description, topic, lang));
                if (geo == null) getGeometry(yahooGeocode(topic, topic, lang));
                topics.add(new Pair<String, Pair<String, Geometry>>(id.trim(), new Pair<String, Geometry>(topic.toLowerCase().trim(), geo)));
            }
            nodes = tree.getElementsByTagName("top");
            String lang2 = lang.substring(lang.indexOf("-") + 1);
            for (int j = 0; j < nodes.getLength() && (((Element) (nodes.item(j))).getElementsByTagName(lang2.toUpperCase() + "-title")).getLength() > 0; j++) {
                String topic = ((Element) (nodes.item(j))).getElementsByTagName(lang2.toUpperCase() + "-title").item(0).getFirstChild().toString();
                String id = ((Element) (nodes.item(j))).getElementsByTagName("num").item(0).getFirstChild().toString().trim();
                String description = ((Element) (nodes.item(j))).getElementsByTagName(lang2.toUpperCase() + "-desc").item(0).getFirstChild().toString() + ". " + ((Element) (nodes.item(j))).getElementsByTagName(lang2.toUpperCase() + "-narr").item(0).getFirstChild().toString();
                Geometry geo = getGeometry(yahooGeocode(description, topic, lang));
                if (geo == null) getGeometry(yahooGeocode(topic, topic, lang));
                topics.add(new Pair<String, Pair<String, Geometry>>(id.trim(), new Pair<String, Geometry>(topic.toLowerCase().trim(), geo)));
            }
        }
    }

    public void addRelevance(String relevance) throws Exception {
        if (new File(relevance).isDirectory()) {
            File files[] = new File(relevance).listFiles();
            for (int i = 0; i < files.length; i++) if (files[i].getName().endsWith(".txt")) addRelevance(files[i].getAbsolutePath());
        } else {
            System.out.println("Adding relevance judgemnts from file " + relevance);
            BufferedReader rel = new BufferedReader(new FileReader(relevance));
            String j;
            while ((j = rel.readLine()) != null) {
                String aux[] = j.split(" ");
                if (aux[0].startsWith("0")) aux[0] = "GC" + aux[0];
                System.out.println(aux[0] + "\t" + aux[1] + "\t" + aux[2] + "\t" + aux[3]);
                Set<String> tops = judgedDocuments.get(aux[2].trim());
                if (tops == null) tops = new HashSet<String>();
                tops.add(aux[0].trim());
                judgedDocuments.put(aux[2].trim(), tops);
                if (aux[3].trim().equals("1")) {
                    Map m1 = relevanceTopics.get(aux[0].trim());
                    if (m1 == null) m1 = new HashMap<String, Boolean>();
                    m1.put(aux[2].trim(), true);
                    relevanceTopics.put(aux[0].trim(), m1);
                    Map m2 = relevanceDocuments.get(aux[2].trim());
                    if (m2 == null) m2 = new HashMap<String, Boolean>();
                    m2.put(aux[0].trim(), true);
                    relevanceDocuments.put(aux[2].trim(), m2);
                }
            }
        }
    }

    public void trainTextMetrics(File path) throws Exception {
        class MyInputStream extends InputStream {

            InputStream in = null, r1 = null, r2 = null;

            boolean closed = false;

            public MyInputStream(File f) throws FileNotFoundException {
                this(new BufferedInputStream(new FileInputStream(f)));
            }

            public MyInputStream(InputStream r) {
                r1 = new ByteArrayInputStream("<aux>".getBytes());
                in = new BufferedInputStream(r);
                r2 = new ByteArrayInputStream("</aux>".getBytes());
                closed = false;
            }

            public int read() throws IOException {
                if (closed) return -1;
                int read = r1.read();
                if (read != -1) return read;
                read = in.read();
                if (read == '&') read = ' ';
                if (read != -1) return read;
                read = r2.read();
                return read;
            }

            public void close() throws IOException {
                in.close();
                closed = true;
            }
        }
        class XMLFilenameFilter implements FilenameFilter {

            public boolean accept(File dir, String name) {
                String ext = name.substring(name.lastIndexOf(".") + 1, name.length());
                if (name.indexOf(".") != -1 && ext.compareToIgnoreCase("sgml") == 0) return (true);
                if (name.indexOf(".") == -1 && name.startsWith("la")) return (true); else return (false);
            }
        }
        class MyStringWrapperIterator extends BasicStringWrapperIterator {

            boolean onlyHeadline;

            public MyStringWrapperIterator(Iterator it, boolean h) {
                super(it);
                this.onlyHeadline = h;
            }

            public StringWrapper nextStringWrapper() {
                String obj = ((StringWrapper) super.next()).unwrap();
                String file = obj.substring(0, obj.indexOf(" : ")).trim();
                String mid = obj.substring(obj.indexOf(" :") + 3).trim();
                ;
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                Document document = null;
                try {
                    document = factory.newDocumentBuilder().parse(new MyInputStream(new File(file)));
                } catch (Exception e) {
                }
                Element tree = document.getDocumentElement();
                NodeList nodes = tree.getElementsByTagName("DOC");
                for (int j = 0; j < nodes.getLength(); j++) try {
                    String id = ((Element) (nodes.item(j))).getElementsByTagName("DOCID").item(0).getFirstChild().toString().trim();
                    if (!id.equals(mid)) continue;
                    String text = ((Element) (nodes.item(j))).getElementsByTagName("TEXT").item(0).getFirstChild().toString().trim();
                    String headline = text.substring(0, text.indexOf("\n") == -1 ? 0 : text.indexOf("\n"));
                    try {
                        headline = ((Element) (nodes.item(j))).getElementsByTagName("HEADLINE").item(0).getFirstChild().toString().trim();
                    } catch (Exception e) {
                    }
                    if (onlyHeadline) return new BasicStringWrapper(headline.toLowerCase()); else return new BasicStringWrapper(text.toLowerCase() + ". " + headline.toLowerCase());
                } catch (Exception e) {
                    return new BasicStringWrapper("");
                }
                return new BasicStringWrapper("");
            }
        }
        File files[] = path.listFiles(new XMLFilenameFilter());
        Set<StringWrapper> wrappers = new HashSet<StringWrapper>();
        for (int i = 0, numDocuments = 0; i < files.length && (numDocumentsTest == -1 || numDocuments < numDocumentsTest); i++) {
            System.out.println("Loading " + files[i]);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder loader = factory.newDocumentBuilder();
            Document document = loader.parse(new MyInputStream(files[i]));
            Element tree = document.getDocumentElement();
            NodeList nodes = tree.getElementsByTagName("DOC");
            for (int j = 0; j < nodes.getLength() && (numDocumentsTest == -1 || numDocuments < numDocumentsTest); j++) {
                String id = ((Element) (nodes.item(j))).getElementsByTagName("DOCID").item(0).getFirstChild().toString().trim();
                System.out.println("Adding " + files[i] + " " + id + " to the TF/IDF training examples.");
                wrappers.add(new BasicStringWrapper(files[i].getAbsolutePath() + " : " + id));
                numDocuments++;
            }
        }
        StringWrapperIterator it = new MyStringWrapperIterator(wrappers.iterator(), false);
        tfidf.train(it);
        it = new MyStringWrapperIterator(wrappers.iterator(), true);
        tfidfh.train(it);
    }

    public Document yahooGeocode(String data, String title, String lang) throws Exception {
        setRandomProxy();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        factory.setNamespaceAware(true);
        DocumentBuilder loader = factory.newDocumentBuilder();
        HttpClient client = new HttpClient();
        String url = "http://wherein.yahooapis.com/v1/document";
        PostMethod post = new PostMethod(url);
        post.addParameter("documentType", "text/plain");
        post.addParameter("appid", yahooAppId);
        post.addParameter("documentContent", data);
        post.addParameter("documentTitle", title);
        post.addParameter("inputLanguage", lang);
        post.setDoAuthentication(false);
        client.executeMethod(post);
        String response = post.getResponseBodyAsString();
        Document document = loader.parse(new ByteArrayInputStream(response.getBytes("UTF-8")));
        post.releaseConnection();
        return document;
    }

    public Document computeMetrics(String id, String no, String headline, String data2, Document doc) throws Exception {
        String data = data2 + "\n" + headline;
        NodeList lst = doc.getDocumentElement().getElementsByTagName("topicFeatures");
        while (lst.getLength() != 0) {
            doc.getDocumentElement().removeChild(lst.item(0));
            lst = doc.getDocumentElement().getElementsByTagName("topicFeatures");
        }
        com.vividsolutions.jts.geom.Geometry dataScope = getGeometry(doc);
        for (int i = 0; i < topics.size(); i++) {
            String topic = topics.get(i).fst;
            String topicText = topics.get(i).snd.fst;
            com.vividsolutions.jts.geom.Geometry topicScope = topics.get(i).snd.snd;
            Element elm1 = doc.createElement("topicFeatures");
            elm1.setAttribute("topic", topic);
            elm1.setAttribute("docid", no);
            elm1.setAttribute("relevance", relevanceDocuments.get(id) == null || relevanceDocuments.get(id).get(topic) == null || relevanceDocuments.get(id).get(topic) == false ? "0.0" : "1.0");
            Element elmt = doc.createElement("textFeatures");
            elmt.setAttribute("length", "" + tfidf.length(data));
            elmt.setAttribute("headline-length", "" + tfidfh.length(headline));
            elmt.setAttribute("topic-length", "" + tfidf.length(topicText));
            elmt.setAttribute("tfidf", "" + tfidf.tfidf(topicText, data));
            elmt.setAttribute("bm25", "" + tfidf.bm25(topicText, data));
            elmt.setAttribute("tf", "" + tfidf.tf(topicText, data));
            elmt.setAttribute("idf", "" + tfidf.idf(topicText, data));
            elmt.setAttribute("headline-tfidf", "" + tfidfh.tfidf(topicText, headline));
            elmt.setAttribute("headline-bm25", "" + tfidfh.bm25(topicText, headline));
            elmt.setAttribute("headline-tf", "" + tfidfh.tf(topicText, headline));
            elmt.setAttribute("headline-idf", "" + tfidfh.idf(topicText, headline));
            Element elmg = doc.createElement("geoFeatures");
            elmg.setAttribute("distance", topicScope == null || dataScope == null ? "-1.0" : "" + geometrySimilarity(topicScope, dataScope, 0));
            elmg.setAttribute("topic-area", topicScope == null ? "0.0" : "" + topicScope.getArea());
            elmg.setAttribute("doc-area", dataScope == null ? "0.0" : "" + dataScope.getArea());
            elmg.setAttribute("overlap1", topicScope == null || dataScope == null ? "0.0" : "" + geometrySimilarity(topicScope, dataScope, 1));
            elmg.setAttribute("overlap2", topicScope == null || dataScope == null ? "0.0" : "" + geometrySimilarity(topicScope, dataScope, 2));
            elmg.setAttribute("overlap3", topicScope == null || dataScope == null ? "0.0" : "" + geometrySimilarity(topicScope, dataScope, 3));
            elmg.setAttribute("overlap4", topicScope == null || dataScope == null ? "0.0" : "" + geometrySimilarity(topicScope, dataScope, 4));
            elmg.setAttribute("overlap5", topicScope == null || dataScope == null ? "0.0" : "" + geometrySimilarity(topicScope, dataScope, 5));
            elmg.setAttribute("distance-norm", topicScope == null || dataScope == null ? "0.0" : "" + geometrySimilarity(topicScope, dataScope, 6));
            elmg.setAttribute("distance-hausdorff", topicScope == null || dataScope == null ? "-1.0" : "" + geometrySimilarity(topicScope, dataScope, 7));
            elmg.setAttribute("taxonomy1", "" + geometrySimilarity(topicScope, dataScope, 8));
            elm1.appendChild(elmt);
            elm1.appendChild(elmg);
            doc.getDocumentElement().appendChild(elm1);
        }
        return doc;
    }

    public void processGeoCLEF(File in, File out, boolean reprocess) throws Exception {
        class MyInputStream extends InputStream {

            InputStream in = null, r1 = null, r2 = null;

            boolean closed = false;

            public MyInputStream(File f) throws FileNotFoundException {
                this(new BufferedInputStream(new FileInputStream(f)));
            }

            public MyInputStream(InputStream r) {
                r1 = new ByteArrayInputStream("<aux>".getBytes());
                in = new BufferedInputStream(r);
                r2 = new ByteArrayInputStream("</aux>".getBytes());
                closed = false;
            }

            public int read() throws IOException {
                if (closed) return -1;
                int read = r1.read();
                if (read != -1) return read;
                read = in.read();
                if (read == '&') read = ' ';
                if (read != -1) return read;
                read = r2.read();
                return read;
            }

            public void close() throws IOException {
                in.close();
                closed = true;
            }
        }
        class XMLFilenameFilter implements FilenameFilter {

            public boolean accept(File dir, String name) {
                String ext = name.substring(name.lastIndexOf(".") + 1, name.length());
                if (name.indexOf(".") != -1 && ext.compareToIgnoreCase("sgml") == 0) return (true);
                if (name.indexOf(".") == -1 && name.startsWith("la")) return (true); else return (false);
            }
        }
        String lang = in.getName().toLowerCase().endsWith("pt") ? "pt-PT" : "en-EN";
        File files[] = in.listFiles(new XMLFilenameFilter());
        for (int i = 0, numDocuments = 0; i < files.length && (numDocumentsTest == -1 || numDocuments < numDocumentsTest); i++) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder loader = factory.newDocumentBuilder();
            Document document = loader.parse(new MyInputStream(files[i]));
            Element tree = document.getDocumentElement();
            NodeList nodes = tree.getElementsByTagName("DOC");
            for (int j = 0; j < nodes.getLength() && (numDocumentsTest == -1 || numDocuments < numDocumentsTest); j++) try {
                String id = ((Element) (nodes.item(j))).getElementsByTagName("DOCID").item(0).getFirstChild().toString().trim();
                String no = ((Element) (nodes.item(j))).getElementsByTagName("DOCNO").item(0).getFirstChild().toString().trim();
                System.out.println("** Processing document " + new File(new File(out, "" + Math.abs(id.hashCode()) % 21), id + ".xml").getAbsolutePath() + " " + no + " to compute features. ");
                if (!judgedDocuments.containsKey(no)) continue;
                String text = ((Element) (nodes.item(j))).getElementsByTagName("TEXT").item(0).getFirstChild().toString().trim();
                String headline = text.substring(0, text.indexOf("\n") == -1 ? 0 : text.indexOf("\n"));
                try {
                    headline = ((Element) (nodes.item(j))).getElementsByTagName("HEADLINE").item(0).getFirstChild().toString().trim();
                } catch (Exception e) {
                }
                File outdir = new File(out, "" + Math.abs(id.hashCode()) % 21);
                Document doc;
                if (reprocess) {
                    try {
                        doc = factory.newDocumentBuilder().parse(new FileInputStream(new File(outdir, id + ".xml")));
                        doc = computeMetrics(id, no, text.toLowerCase(), headline.toLowerCase(), doc);
                    } catch (Exception ex) {
                        doc = computeMetrics(id, no, text.toLowerCase(), headline.toLowerCase(), yahooGeocode(text, headline, lang));
                    }
                } else doc = computeMetrics(id, no, text.toLowerCase(), headline.toLowerCase(), yahooGeocode(text, headline, lang));
                outdir.mkdir();
                PrintStream output = new PrintStream(new FileOutputStream(new File(outdir, id + ".xml")));
                printXML(doc.getDocumentElement(), output);
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
        }
    }

    public void getL2RFeatureVectors(File data, String outFileName) throws Exception {
        PrintStream out = new PrintStream(new FileOutputStream(new File(outFileName)));
        out.println("@RELATION geoclef-learn-to-rank");
        out.println("@ATTRIBUTE topic                    STRING");
        out.println("@ATTRIBUTE topic-length			 NUMERIC");
        out.println("@ATTRIBUTE doclength				 NUMERIC");
        out.println("@ATTRIBUTE tf						 NUMERIC");
        out.println("@ATTRIBUTE idf				         NUMERIC");
        out.println("@ATTRIBUTE tfidf				     NUMERIC");
        out.println("@ATTRIBUTE bm25				     NUMERIC");
        out.println("@ATTRIBUTE headline-length			 NUMERIC");
        out.println("@ATTRIBUTE headline-tf				 NUMERIC");
        out.println("@ATTRIBUTE headline-idf			 NUMERIC");
        out.println("@ATTRIBUTE headline-tfidf			 NUMERIC");
        out.println("@ATTRIBUTE headline-bm25			 NUMERIC");
        out.println("@ATTRIBUTE distance				 NUMERIC");
        out.println("@ATTRIBUTE topicarea				 NUMERIC");
        out.println("@ATTRIBUTE docarea				     NUMERIC");
        out.println("@ATTRIBUTE overlap1				 NUMERIC");
        out.println("@ATTRIBUTE overlap2				 NUMERIC");
        out.println("@ATTRIBUTE overlap3				 NUMERIC");
        out.println("@ATTRIBUTE overlap4				 NUMERIC");
        out.println("@ATTRIBUTE overlap5				 NUMERIC");
        out.println("@ATTRIBUTE normalized-dist			 NUMERIC");
        out.println("@ATTRIBUTE dist-hausdorff			 NUMERIC");
        out.println("@ATTRIBUTE taxonomy1				 NUMERIC");
        out.println("@ATTRIBUTE relevance				 NUMERIC");
        out.println("@DATA");
        class XMLFilenameFilter implements FilenameFilter {

            public boolean accept(File dir, String name) {
                String ext = name.substring(name.lastIndexOf(".") + 1, name.length());
                if (name.indexOf(".") != -1 && ext.compareToIgnoreCase("xml") == 0) return (true); else return (false);
            }
        }
        File dirs[] = data.listFiles();
        for (int k = 0; k < dirs.length; k++) if (dirs[k].isDirectory()) {
            File files[] = dirs[k].listFiles(new XMLFilenameFilter());
            for (int i = 0; i < files.length; i++) try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder loader = factory.newDocumentBuilder();
                Document document = loader.parse(new FileInputStream(files[i]));
                Element tree = document.getDocumentElement();
                NodeList nodes = tree.getElementsByTagName("topicFeatures");
                for (int j = 0; j < nodes.getLength(); j++) try {
                    Element text = (Element) (((Element) (nodes.item(j))).getElementsByTagName("textFeatures").item(0));
                    Element geo = (Element) (((Element) (nodes.item(j))).getElementsByTagName("geoFeatures").item(0));
                    String docid = ((Element) (nodes.item(j))).getAttributes().getNamedItem("docid").getNodeValue().trim();
                    String topic = ((Element) (nodes.item(j))).getAttributes().getNamedItem("topic").getNodeValue().trim();
                    String id = docid + "_" + topic;
                    boolean rel1 = relevanceDocuments.get(docid) == null ? false : relevanceDocuments.get(docid).get(topic) == null ? false : relevanceDocuments.get(docid).get(topic);
                    boolean rel2 = relevanceTopics.get(topic) == null ? false : relevanceTopics.get(topic).get(docid) == null ? false : relevanceTopics.get(topic).get(docid);
                    String relevance = ((Element) (nodes.item(j))).getAttributes().getNamedItem("relevance").getNodeValue().trim();
                    relevance = (rel1 || rel2) ? "1.0" : "0.0";
                    String length = text.getAttributes().getNamedItem("length").getNodeValue();
                    String hlength = text.getAttributes().getNamedItem("headline-length").getNodeValue();
                    String tlength = text.getAttributes().getNamedItem("topic-length").getNodeValue();
                    String tf = text.getAttributes().getNamedItem("tf").getNodeValue();
                    String idf = text.getAttributes().getNamedItem("idf").getNodeValue();
                    String tfidf = text.getAttributes().getNamedItem("tfidf").getNodeValue();
                    String bm25 = text.getAttributes().getNamedItem("bm25").getNodeValue();
                    String htf = text.getAttributes().getNamedItem("headline-tf").getNodeValue();
                    String hidf = text.getAttributes().getNamedItem("headline-idf").getNodeValue();
                    String htfidf = text.getAttributes().getNamedItem("headline-tfidf").getNodeValue();
                    String hbm25 = text.getAttributes().getNamedItem("headline-bm25").getNodeValue();
                    String distance = geo.getAttributes().getNamedItem("distance").getNodeValue();
                    String topicarea = geo.getAttributes().getNamedItem("topic-area").getNodeValue();
                    String docarea = geo.getAttributes().getNamedItem("doc-area").getNodeValue();
                    String overlap1 = geo.getAttributes().getNamedItem("overlap1").getNodeValue();
                    String overlap2 = geo.getAttributes().getNamedItem("overlap2").getNodeValue();
                    String overlap3 = geo.getAttributes().getNamedItem("overlap3").getNodeValue();
                    String overlap4 = geo.getAttributes().getNamedItem("overlap4").getNodeValue();
                    String overlap5 = geo.getAttributes().getNamedItem("overlap5").getNodeValue();
                    String distanceNorm = geo.getAttributes().getNamedItem("distance-norm").getNodeValue();
                    String distanceHausdorff = geo.getAttributes().getNamedItem("distance-hausdorff").getNodeValue();
                    String taxonomy1 = geo.getAttributes().getNamedItem("taxonomy1").getNodeValue();
                    if (taxonomy1.equals("NaN")) taxonomy1 = "0.0";
                    out.println('"' + id + '"' + "," + tlength + "," + length + "," + tf + "," + idf + "," + tfidf + "," + bm25 + "," + hlength + "," + htf + "," + hidf + "," + htfidf + "," + hbm25 + "," + distance + "," + topicarea + "," + docarea + "," + overlap1 + "," + overlap2 + "," + overlap3 + "," + overlap4 + "," + overlap5 + "," + distanceNorm + "," + distanceHausdorff + "," + taxonomy1 + "," + relevance);
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
        }
    }

    public void weka2SVMMAP(String path, boolean filterRelevanceJudgements) throws Exception {
        File input = new File(path);
        if (path.indexOf(".") != -1) path = path.substring(0, path.lastIndexOf(".")) + ".svmmap";
        File output = new File(path);
        PrintStream out = new PrintStream(new FileOutputStream(output));
        BufferedReader reader = new BufferedReader(new FileReader(input));
        String aux;
        Map<Integer, Pair<Double, Double>> normalization = new HashMap<Integer, Pair<Double, Double>>();
        while ((aux = reader.readLine()) != null && !aux.equals("@DATA")) {
        }
        ;
        while ((aux = reader.readLine()) != null) {
            String data[] = aux.split(",");
            String t = data[0].substring(data[0].indexOf("_") + 1, 1 + data[0].substring(1).indexOf("\""));
            String t2 = t.indexOf("/") != -1 ? t.substring(t.indexOf("/") + 1, t.lastIndexOf("-")).replaceAll("[A-Z]*", "") : t.replaceAll("[A-Z]*", "");
            int qid = new Integer(t2) - 1;
            Double maxScore = normalization.get(qid) == null ? null : normalization.get(qid).fst;
            Double minScore = normalization.get(qid) == null ? null : normalization.get(qid).snd;
            if (maxScore == null) maxScore = new Double(0);
            if (minScore == null) minScore = new Double(1.0);
            for (int i = 1; i < data.length - 1; i++) {
                Double nscore = new Double(data[i]);
                if (nscore > maxScore) maxScore = nscore;
                if (nscore < minScore && nscore >= 0.0) minScore = nscore;
            }
            normalization.put(qid, new Pair<Double, Double>(maxScore, minScore));
        }
        reader = new BufferedReader(new FileReader(input));
        while ((aux = reader.readLine()) != null && !aux.equals("@DATA")) {
        }
        ;
        while ((aux = reader.readLine()) != null) {
            String data[] = aux.split(",");
            String t = data[0].substring(data[0].indexOf("_") + 1, 1 + data[0].substring(1).indexOf("\""));
            String did = data[0].substring(1, data[0].indexOf("_")).trim();
            String t2 = t.indexOf("/") != -1 ? t.substring(t.indexOf("/") + 1, t.lastIndexOf("-")).replaceAll("[A-Z]*", "") : t.replaceAll("[A-Z]*", "");
            int qid = new Integer(t2) - 1;
            if (!filterRelevanceJudgements || (judgedDocuments.containsKey(did) && judgedDocuments.get(did).contains(t))) {
                out.print(data[data.length - 1] + " qid:" + qid);
                double max = normalization.get(qid).fst;
                double min = normalization.get(qid).snd;
                double mm = max - min;
                double w = 0.0;
                double w2 = 1.0 - (1.0 / Math.log(Math.E + new Double(data[18])));
                for (int i = 1; i < data.length - 1; i++) {
                    w = mm == 0.0 || new Double(data[i]) <= 0.0 ? new Double(data[i]) : (new Double(data[i]) - min) / mm;
                    out.print(" " + i + ":" + w);
                }
                double distance = mm == 0.0 ? new Double(data[12]) : (new Double(data[12]) - min) / mm;
                double overlap = mm == 0.0 ? new Double(data[17]) : (new Double(data[17]) - min) / mm;
                double tfidf = mm == 0.0 ? new Double(data[5]) : (new Double(data[3]) - min) / mm;
                double bm25 = mm == 0.0 ? new Double(data[6]) : (new Double(data[4]) - min) / mm;
                w = (distance + tfidf) * 0.5;
                out.print(" " + (data.length - 1 + 0) + ":" + w);
                w = (distance + bm25) * 0.5;
                out.print(" " + (data.length - 1 + 1) + ":" + w);
                w = (overlap + tfidf) * 0.5;
                out.print(" " + (data.length - 1 + 2) + ":" + w);
                w = (overlap + bm25) * 0.5;
                out.print(" " + (data.length - 1 + 3) + ":" + w);
                w = (((1 - w2) * bm25) + (w2 * overlap));
                out.print(" " + (data.length - 1 + 4) + ":" + w);
                out.println(" #docid = " + did);
            }
        }
        out.close();
        reader.close();
    }

    public void topicInfo() {
        System.out.println("*** General topic information ***");
        int termSize[] = new int[4];
        double area[][] = new double[4][25];
        for (int i = 0, j = -1; i < topics.size(); i++) {
            if (i % 25 == 0) {
                j++;
            }
            ;
            System.out.println(topics.get(i).fst.toString() + " " + topics.get(i).snd.fst + " " + (topics.get(i).snd.snd == null ? 0 : topics.get(i).snd.snd.getArea()) + " " + (topics.get(i).snd.snd == null ? "none" : topics.get(i).snd.snd.getUserData()));
            if (topics.get(i).snd.fst == null) System.out.println(" *** " + topics.get(i).fst); else termSize[j] += topics.get(i).snd.fst.split(" ").length;
            area[j][i % 25] = topics.get(i).snd.snd == null ? 0 : topics.get(i).snd.snd.getArea();
        }
        for (int i = 0, j = -1; i < topics.size(); i++) {
            if (i % 25 == 0) {
                j++;
            }
            ;
            String col = "0.0";
            if (area[j][i % 25] == 0) col = "2.0";
            System.out.println(i + "\t" + col + " # " + topics.get(i).snd.fst + " : " + (topics.get(i).snd.snd == null ? "" : topics.get(i).snd.snd.getUserData()));
        }
        for (int i = 0; i < 4; i++) {
            System.out.println("===============================");
            System.out.println("Average Term size colection " + i + " = " + (termSize[i] / 25.0));
            int sum1 = 0, sum2 = 0, sum3 = 0, sum4 = 0;
            for (int j = 0; j < 25; j++) {
                sum1 += area[i][j] == 0 ? 1 : 0;
                sum2 += area[i][j] < 100 ? 1 : 0;
                sum3 += area[i][j] >= 100 && area[i][j] <= 1000 ? 1 : 0;
                sum4 += area[i][j] > 1000 ? 1 : 0;
            }
            System.out.println("Area 0 for collection " + i + " = " + sum1);
            System.out.println("Area <100 for collection " + i + " = " + sum2);
            System.out.println("Area >100 for collection " + i + " = " + sum3);
            System.out.println("Area >1000 for collection " + i + " = " + sum4);
        }
    }

    public static void main(String args[]) throws Exception {
        System.setProperty("http.proxyHost", "proxy1.ipp.pt");
        System.setProperty("http.proxyPort", "3128");
        Logger logger = Logger.getRootLogger();
        logger.setLevel(Level.ERROR);
        ProcessGeoCLEFCollections aux = new ProcessGeoCLEFCollections();
        String data = "d:\\Servidores\\data\\geoclef\\gh-95";
        if (args.length == 1) data = args[0];
        aux.addTopics(data + File.separator + "topics");
        aux.addRelevance(data + File.separator + "topics");
        aux.topicInfo();
        aux.trainTextMetrics(new File(data));
        aux.processGeoCLEF(new File(data), new File(data + File.separator + "output"), false);
        aux.getL2RFeatureVectors(new File(data + File.separator + "output"), data + File.separator + "output" + File.separator + "results.arff");
        aux.weka2SVMMAP(data + File.separator + "output" + File.separator + "results.arff", true);
    }
}
