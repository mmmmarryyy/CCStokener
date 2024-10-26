package org.gbif.ecatws.servlet;

import org.gbif.ecat.lucene.analysis.sciname.SciName;
import org.gbif.ecat.lucene.analysis.sciname.SciNameAnalyzer;
import org.gbif.ecat.lucene.analysis.sciname.SciNameIterator;
import org.gbif.ecat.lucene.utils.FilterXmlReader;
import org.gbif.ecat.lucene.utils.LuceneUtils;
import org.gbif.ecatws.servlet.result.JsonWriter;
import org.gbif.ecatws.servlet.util.WsException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.google.inject.Singleton;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.lucene.analysis.TokenStream;

@Singleton
public class NameIndexerServlet extends BasicServlet {

    private static final String INPUT = "input";

    private static final String TYPE = "type";

    private static final String FORMAT = "format";

    private static final byte THRESHOLD = 25;

    private HttpClient client;

    protected SciNameAnalyzer analyzer;

    @Override
    public void destroy() {
        super.destroy();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws WsException {
        String callback = para(req, JsonWriter.CALLBACK, null);
        String input = para(req, INPUT, null);
        String type = para(req, TYPE, "url");
        String format = para(req, FORMAT, null);
        PrintWriter out = null;
        Reader contentReader = null;
        try {
            out = resp.getWriter();
            if (StringUtils.trimToNull(input) == null) {
                resp.setContentType("text/html");
                printHelp(out);
            } else {
                if (type.equalsIgnoreCase("url")) {
                    HttpGet httpget = new HttpGet(input);
                    try {
                        HttpResponse response = client.execute(httpget);
                        HttpEntity entity = response.getEntity();
                        if (entity != null) {
                            String charset = "UTF-8";
                            contentReader = new InputStreamReader(entity.getContent(), charset);
                            if (false) {
                                contentReader = new FilterXmlReader(contentReader);
                            } else {
                                contentReader = new BufferedReader(contentReader);
                            }
                        }
                    } catch (RuntimeException ex) {
                        httpget.abort();
                        throw ex;
                    }
                } else {
                    contentReader = new StringReader(input);
                }
                long time = System.currentTimeMillis();
                TokenStream stream = nameTokenStream(contentReader);
                SciNameIterator iter = new SciNameIterator(stream);
                if (format != null && format.equalsIgnoreCase("json")) {
                    resp.setContentType("application/json");
                    streamAsJSON(iter, out, callback);
                } else if (format != null && format.equalsIgnoreCase("xml")) {
                    resp.setContentType("text/xml");
                    streamAsXML(iter, out);
                } else {
                    resp.setContentType("text/plain");
                    streamAsText(iter, out);
                }
                log.info("Indexing finished in " + (System.currentTimeMillis() - time) + " msecs");
            }
        } catch (IOException e1) {
            log.error("IOException", e1);
            e1.printStackTrace();
        } finally {
            if (contentReader != null) {
                try {
                    contentReader.close();
                } catch (IOException e) {
                    log.error("IOException", e);
                }
            }
            out.flush();
            out.close();
        }
    }

    @Override
    public void init() throws ServletException {
        super.init();
        try {
            HttpParams params = new BasicHttpParams();
            SchemeRegistry schemeRegistry = new SchemeRegistry();
            schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
            ClientConnectionManager cm = new ThreadSafeClientConnManager(params, schemeRegistry);
            client = new DefaultHttpClient(cm, params);
            analyzer = new SciNameAnalyzer();
            analyzer.setThreshold(THRESHOLD);
        } catch (Exception e) {
            throw new ServletException("Cannot create IndexingManager. Please verify dictionaries in data dir: " + cfg.dataDir(), e);
        }
    }

    private TokenStream nameTokenStream(Reader reader) throws IOException {
        return analyzer.reusableTokenStream(LuceneUtils.FIELD_BODY, reader);
    }

    private void printHelp(PrintWriter out) {
        out.write("<html><body><h2>Name Finder API</h2>" + "<dl>" + "<dt>" + INPUT + "</dt>" + "<dd>The text to be searched, either supplied directly as plain text or indirectly by supplying an accessible" + "url pointing to a document to be indexed. Depending on the service the document can be text, html, xml, pdf, doc, xls, ppt or more." + "Some formats are hard to extract though and beware of pdfs which only contains scan images, but no text." + "</dd>" + "<dt>" + TYPE + "</dt>" + "<dd>The type of input, currently either plain text or a url to some public online document. Can be any of <span class='param'>text, url</span>." + "The service will try to derive the type if none is given." + "</dd>" + "<dt>" + FORMAT + "</dt>" + "<dd>response format. Can be any of <span class='param'>xml, json, text</span></dd>" + "<dt>" + JsonWriter.CALLBACK + "</dt>" + "<dd>optional javascript callback handler to support json-p</dd>" + "</dl>" + "</body></html>");
    }

    private void streamAsJSON(SciNameIterator iter, PrintWriter out, String callback) throws IOException {
        JSONObject jsonObj = new JSONObject();
        if (callback != null && callback.length() > 1) {
            out.write(callback + "({\"names\":[\n");
        } else {
            out.write("{\"names\":[\n");
        }
        boolean first = true;
        for (SciName n : iter) {
            if (first) {
                first = false;
            } else {
                out.write(",");
            }
            jsonObj.put("scientificName", n.scientificName);
            jsonObj.put("verbatim", n.verbatimName);
            jsonObj.put("score", n.score);
            jsonObj.put("offsetStart", n.offsetStart);
            jsonObj.put("offsetEnd", n.offsetEnd);
            out.write(jsonObj.toString(0) + "\n");
        }
        if (callback != null && callback.length() > 1) {
            out.print("]})");
        } else {
            out.print("]}");
        }
    }

    private void streamAsText(SciNameIterator iter, PrintWriter out) throws IOException {
        out.write("\n#ScientificName\tVerbatimString\tScore\tOffsetStart\tOffsetEnd\tNovum");
        for (SciName n : iter) {
            if (n == null) {
                continue;
            }
            out.write("\n" + n.scientificName.replaceAll("[\\t\\r\\n]", " ") + "\t" + n.verbatimName.replaceAll("[\\t\\r\\n]", " ") + "\t" + n.score + "\t" + n.offsetStart + "\t" + n.offsetEnd + "\t" + n.novum);
        }
    }

    private void streamAsXML(SciNameIterator iter, PrintWriter out) throws IOException {
        out.print("<names xmlns='http://globalnames.org/namefinder' xmlns:dwc='http://rs.tdwg.org/dwc/terms/'>");
        for (SciName n : iter) {
            if (n == null) {
                continue;
            }
            out.write("<name>\n");
            out.write(" <verbatimString>" + StringEscapeUtils.escapeXml(n.verbatimName) + "</verbatimString>\n");
            out.write(" <score>" + n.score + "</score>\n");
            out.write(" <dwc:scientificName>" + StringEscapeUtils.escapeXml(n.scientificName) + "</dwc:scientificName>\n");
            out.write(" <offset start='" + n.offsetStart + "' end='" + n.offsetEnd + "'/>\n");
            out.write("</name>\n");
        }
        out.write("</names>");
    }
}
