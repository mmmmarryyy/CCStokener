package org.apache.solr.client.impl;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.QueryResults;
import org.apache.solr.client.SolrClient;
import org.apache.solr.client.SolrDocument;
import org.apache.solr.client.SolrDocumentable;
import org.apache.solr.client.SolrDocumented;
import org.apache.solr.client.SolrQuery;
import org.apache.solr.client.exception.SolrClientException;
import org.apache.solr.client.exception.SolrServerException;
import org.apache.solr.client.util.XML;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

public class SolrClientImpl implements SolrClient {

    private static final Log LOG = LogFactory.getLog(SolrClientImpl.class);

    public static final String AGENT = "Solr[" + SolrClientImpl.class.getName() + "]";

    /**
	 * Factory for parsers for the XML we get back from the server.
	 */
    protected XmlPullParserFactory factory;

    protected ResultsParser parser;

    /**
	 * The URL of the Solr server.
	 */
    protected URL solrUpdateUrl;

    protected URL solrSearchUrl;

    /**
	 * This interface is for objects that write XML requests to the
	 * server.  Normally, these objects are created on the fly and
	 * passed to <code>serverCall(RequestWriter)</code>.
	 * All the public APIs that make calls to the server do so
	 * by constructing one of these and then passing it to <code>
	 * serverCall(RequestWriter)</code>.
	 * @see #postUpdateXml(RequestWriter)
	 */
    protected static interface RequestWriter {

        /**
		 * Write an XML request for a Solr server to the given writer.
		 * @param writer The writer to write the XML to.
		 * @throws IOException if writing fails.
		 */
        void writeRequest(Writer writer) throws Exception;
    }

    ;

    /**
	 * An implementation of the RequestWriter interface that writes
	 * a constant.  Useful for e.g. commit and optimize messages.
	 */
    protected static class ConstantRequestWriter implements RequestWriter {

        private final String constant;

        /**
		 * @param constant
		 */
        public ConstantRequestWriter(String constant) {
            this.constant = constant;
        }

        public void writeRequest(Writer writer) throws IOException {
            writer.write(constant);
        }
    }

    /**
	 * @param solrServerUrl The URL of the Solr server.  For 
	 * example, "<code>http://localhost:8983/solr/update</code>"
	 * if you are using the standard distribution Solr webapp 
	 * on your local machine.
	 * @throws SolrClientException If unable to construct, typically
	 * due to some problem with the XPP parser factory.
	 */
    public SolrClientImpl(URL baseURL) throws Exception {
        String base = baseURL.toExternalForm();
        if (!base.endsWith("/")) {
            base += "/";
        }
        this.solrUpdateUrl = new URL(base + "update/");
        this.solrSearchUrl = new URL(base + "select/");
        try {
            factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(false);
            parser = new ResultsParser(factory);
        } catch (XmlPullParserException e) {
            throw new SolrClientException("Unable to get XPP factory instance", e);
        }
        factory.setNamespaceAware(false);
    }

    /**
	 * This is the meat of any call to the server.  The specifics of what the call 
	 * does are controlled by the <code>RequestWriter</code> that is passed in.
	 * @param requestWriter
	 * @throws SolrClientException if there is a client-side problem.
	 * @throws SolrServerException if there is a server-side problem.
	 * @see RequestWriter
	 */
    protected void postUpdateXml(RequestWriter requestWriter) throws SolrClientException, SolrServerException {
        try {
            HttpURLConnection urlc = (HttpURLConnection) solrUpdateUrl.openConnection();
            urlc.setRequestMethod("POST");
            urlc.setDoOutput(true);
            urlc.setDoInput(true);
            urlc.setUseCaches(false);
            urlc.setAllowUserInteraction(false);
            urlc.setRequestProperty("Content-type", "text/xml; charset=utf-8");
            urlc.setRequestProperty("User-Agent", AGENT);
            OutputStream out = urlc.getOutputStream();
            try {
                Writer writer = new OutputStreamWriter(out, "utf-8");
                try {
                    requestWriter.writeRequest(writer);
                    out.flush();
                } catch (Exception e) {
                    throw new SolrClientException("RequestWriter failed to write request.", e);
                } finally {
                    writer.close();
                }
            } finally {
                out.close();
            }
            InputStream inputStream = urlc.getInputStream();
            try {
                Reader reader = new InputStreamReader(inputStream);
                try {
                    XmlPullParser xpp;
                    try {
                        xpp = factory.newPullParser();
                        xpp.setInput(reader);
                        xpp.nextTag();
                    } catch (XmlPullParserException e) {
                        throw new SolrClientException("XML parsing exception in solr client", e);
                    }
                    LOG.debug("xml element name: " + xpp.getName());
                    System.err.println("xml element name: " + xpp.getName());
                    if (!"response".equals(xpp.getName())) {
                        throw new SolrClientException("Result from server is not rooted with a <result> tag.");
                    }
                    try {
                        xpp.nextTag();
                        xpp.nextTag();
                    } catch (XmlPullParserException ex) {
                    }
                    String statusString = "0";
                    int status = Integer.parseInt(statusString);
                    if (status != 0) {
                        try {
                            StringWriter str = new StringWriter();
                            try {
                                requestWriter.writeRequest(str);
                            } catch (Exception ex) {
                            }
                            throw new SolrServerException("Server returned non-zero status: ", status, xpp.nextText(), str.toString());
                        } catch (XmlPullParserException e) {
                            throw new SolrClientException("XML parsing exception in solr client", e);
                        }
                    }
                } finally {
                    reader.close();
                }
            } finally {
                inputStream.close();
            }
        } catch (IOException e) {
            throw new SolrClientException("I/O exception in solr client", e);
        }
    }

    public void add(final SolrDocumentable doc) throws SolrClientException, SolrServerException {
        ArrayList<SolrDocumentable> docs = new ArrayList<SolrDocumentable>(1);
        docs.add(doc);
        this.add(docs);
    }

    public void add(final Collection<SolrDocumentable> documents) throws SolrClientException, SolrServerException {
        RequestWriter requestWriter = new RequestWriter() {

            private void writeFieldValue(Writer writer, String fieldName, Object fieldValue) throws IOException {
                if (fieldValue instanceof Date) {
                    fieldValue = SolrClient.ISO8601_UTC.format((Date) fieldValue);
                }
                XML.writeXML(writer, "field", fieldValue.toString(), "name", fieldName);
            }

            public void writeRequest(Writer writer) throws IOException {
                writer.write("<add>");
                for (SolrDocumentable document : documents) {
                    writer.write("<doc>");
                    Map<String, Object> fields = null;
                    if (document instanceof SolrDocument) {
                        fields = ((SolrDocument) document).getSolrDocumentFields();
                    } else if (document instanceof SolrDocumented) {
                        fields = ((SolrDocumented) document).getSolrDocument().getSolrDocumentFields();
                    } else {
                        throw new RuntimeException("don't know about SolrDocumentable type: " + document.getClass().getName());
                    }
                    for (Map.Entry<String, Object> field : fields.entrySet()) {
                        String fieldName = field.getKey();
                        Object fieldValue = field.getValue();
                        if (fieldValue != null) {
                            if (fieldValue instanceof Collection) {
                                for (Object individualFieldValue : (Collection) fieldValue) {
                                    writeFieldValue(writer, fieldName, individualFieldValue);
                                }
                            } else if (fieldValue.getClass().isArray()) {
                                for (Object individualFieldValue : (Object[]) fieldValue) {
                                    writeFieldValue(writer, fieldName, individualFieldValue);
                                }
                            } else {
                                writeFieldValue(writer, fieldName, fieldValue);
                            }
                        }
                    }
                    writer.write("</doc>");
                }
                writer.write("</add>");
            }
        };
        postUpdateXml(requestWriter);
    }

    public void delete(final String id) throws SolrClientException, SolrServerException {
        RequestWriter requestWriter = new RequestWriter() {

            public void writeRequest(Writer writer) throws IOException {
                writer.write("<delete><id>");
                XML.escapeCharData(id, writer);
                writer.write("</id></delete>");
            }
        };
        postUpdateXml(requestWriter);
    }

    public void deleteByQuery(final String query) throws SolrClientException, SolrServerException {
        RequestWriter requestWriter = new RequestWriter() {

            public void writeRequest(Writer writer) throws IOException {
                writer.write("<delete><query>");
                XML.escapeCharData(query, writer);
                writer.write("</query></delete>");
            }
        };
        postUpdateXml(requestWriter);
    }

    public void commit(boolean waitFlush, boolean waitSearcher) throws SolrClientException, SolrServerException {
        StringBuilder xml = new StringBuilder();
        xml.append("<commit ");
        xml.append(" waitFlush=\"").append(waitFlush).append("\" ");
        xml.append(" waitSearcher=\"").append(waitSearcher).append("\" ");
        xml.append("/>");
        postUpdateXml(new ConstantRequestWriter(xml.toString()));
    }

    public void optimize(boolean waitFlush, boolean waitSearcher) throws SolrClientException, SolrServerException {
        StringBuilder xml = new StringBuilder();
        xml.append("<optimize ");
        xml.append(" waitFlush=\"").append(waitFlush).append("\" ");
        xml.append(" waitSearcher=\"").append(waitSearcher).append("\" ");
        xml.append("/>");
        postUpdateXml(new ConstantRequestWriter(xml.toString()));
    }

    /**
     * perhaps this should use: http://jakarta.apache.org/commons/httpclient/
     */
    public QueryResults query(SolrQuery query) throws SolrClientException, SolrServerException {
        try {
            URL qurl = new URL(solrSearchUrl.toExternalForm() + "?" + query.getQueryString());
            HttpURLConnection urlc = (HttpURLConnection) qurl.openConnection();
            urlc.setRequestMethod("GET");
            urlc.setDoOutput(true);
            urlc.setDoInput(true);
            urlc.setUseCaches(false);
            urlc.setAllowUserInteraction(false);
            urlc.setRequestProperty("User-Agent", AGENT);
            if (urlc.getResponseCode() != 200) {
                InputStream input = urlc.getErrorStream();
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                long count = 0;
                int n = 0;
                while (-1 != (n = input.read(buffer))) {
                    output.write(buffer, 0, n);
                    count += n;
                }
                throw new SolrServerException("non XML result", urlc.getResponseCode(), output.toString(), qurl.toExternalForm());
            }
            InputStream inputStream = urlc.getInputStream();
            try {
                Reader reader = new InputStreamReader(inputStream);
                QueryResults res = parser.process(reader);
                res.setSolrURL(qurl);
                res.setQuery(query);
                return res;
            } catch (XmlPullParserException e) {
                throw new SolrClientException("error parsing XML", e);
            } finally {
                inputStream.close();
            }
        } catch (IOException e) {
            throw new SolrClientException("I/O exception in solr client", e);
        }
    }
}
