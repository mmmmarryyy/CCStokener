package jriaffe.client.webservices;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HTTP;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @author preisler
 */
public class XMLWebService {

    public XMLWebService(String url, HttpMethod method, ServiceClient client) {
        this.url = url;
        this.method = method;
        this.client = client;
        HttpParams params = new BasicHttpParams();
        ConnManagerParams.setTimeout(params, 3000);
        httpClient = new DefaultHttpClient(params);
    }

    public XMLWebService(String url, HttpMethod method, ServiceClient client, List<String> nodes) {
        this(url, method, client);
        if (nodes != null) {
            this.nodes = new ArrayList<String>(nodes);
        }
    }

    public void execute(java.util.Map<String, String> parameters) {
        if (getUrl() == null) {
            return;
        }
        this.parameters = parameters;
        boolean streamToClient = false;
        if (getNodes() != null && !nodes.isEmpty() && getClient() != null) {
            streamToClient = true;
        }
        if (streamToClient == false && noBuffer == true) {
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    if (client != null) {
                        client.requestComplete(null);
                    }
                }
            });
        }
        Thread worker = null;
        if (method == HttpMethod.GET) {
            worker = new GetRequestWorker();
        } else if (method == HttpMethod.POST) {
            worker = new PostRequestWorker();
        }
        worker.start();
    }

    public byte[] getResult() {
        return buffer;
    }

    public String getResultAsString() {
        if (buffer != null) {
            return new String(buffer);
        }
        return null;
    }

    /**
     * @return the url
     */
    public String getUrl() {
        return url;
    }

    /**
     * @return the nodes
     */
    public List<String> getNodes() {
        List<String> rNodes = new ArrayList<String>(nodes);
        return rNodes;
    }

    /**
     * @return the client
     */
    public ServiceClient getClient() {
        return client;
    }

    /**
     * @return the method
     */
    public HttpMethod getMethod() {
        return method;
    }

    /**
     * @return the noBuffer
     */
    public boolean isNoBuffer() {
        return noBuffer;
    }

    /**
     * @param noBuffer the noBuffer to set
     */
    public void setNoBuffer(boolean noBuffer) {
        this.noBuffer = noBuffer;
    }

    public void cancelRequest() {
        cancel = true;
    }

    private List<NameValuePair> convertParamsToHttpPairs() {
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        Set<String> keys = parameters.keySet();
        for (String key : keys) {
            String value = parameters.get(key);
            nvps.add(new BasicNameValuePair(key, value));
        }
        return nvps;
    }

    private void encodeParameters() {
        if (parameters != null && !parameters.isEmpty()) {
            List<NameValuePair> nvps = convertParamsToHttpPairs();
            url = url + "?" + URLEncodedUtils.format(nvps, HTTP.UTF_8);
        }
    }

    private void encodeFormParameters(HttpPost httpost) throws UnsupportedEncodingException {
        if (parameters != null && !parameters.isEmpty()) {
            List<NameValuePair> nvps = convertParamsToHttpPairs();
            httpost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
        }
    }

    private void notifyClientTotalContentLength(final long contentLength) {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                client.totalBytesExpected(contentLength);
            }
        });
    }

    private void notifyClientProcessingError(final String message, final Exception ex) {
        Logger.getLogger(XMLWebService.class.getName()).log(Level.WARNING, "Error parsing XML for URL: " + url, ex);
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                client.processingError(message, ex);
                client.requestComplete(null);
            }
        });
    }

    class GetRequestWorker extends Thread {

        BasicHttpContext context = new BasicHttpContext();

        /**
         * Executes the GetMethod and prints some satus information.
         */
        @Override
        public void run() {
            InputStream stream = null;
            try {
                encodeParameters();
                HttpGet get = new HttpGet(url);
                HttpResponse response = httpClient.execute(get, context);
                HttpEntity entity = response.getEntity();
                long expected = entity.getContentLength();
                notifyClientTotalContentLength(expected);
                stream = entity.getContent();
                ResponseInputStream rs = new ResponseInputStream(stream);
                SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
                parser.parse(rs, new SAXHandler());
            } catch (SAXException ex) {
                if (cancel != true) {
                    notifyClientProcessingError(ex.getMessage(), ex);
                }
            } catch (IOException ex) {
                if (cancel != true) {
                    notifyClientProcessingError(ex.getMessage(), ex);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                if (client != null) {
                    notifyClientProcessingError(ex.getMessage(), ex);
                    client.requestComplete(null);
                }
            } finally {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException ex) {
                        Logger.getLogger(XMLWebService.class.getName()).log(Level.WARNING, "Error closing http request stream for URL:" + url, ex);
                    }
                }
            }
        }
    }

    class PostRequestWorker extends Thread {

        BasicHttpContext context = new BasicHttpContext();

        /**
         * Executes the GetMethod and prints some satus information.
         */
        @Override
        public void run() {
            InputStream stream = null;
            try {
                HttpPost post = new HttpPost(url);
                encodeFormParameters(post);
                HttpResponse response = httpClient.execute(post, context);
                HttpEntity entity = response.getEntity();
                long expected = entity.getContentLength();
                notifyClientTotalContentLength(expected);
                stream = entity.getContent();
                ResponseInputStream rs = new ResponseInputStream(stream);
                SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
                parser.parse(rs, new SAXHandler());
            } catch (SAXException ex) {
                if (cancel != true) {
                    notifyClientProcessingError(ex.getMessage(), ex);
                }
            } catch (IOException ex) {
                if (cancel != true) {
                    notifyClientProcessingError(ex.getMessage(), ex);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                if (client != null) {
                    notifyClientProcessingError(ex.getMessage(), ex);
                    client.requestComplete(null);
                }
            } finally {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException ex) {
                        Logger.getLogger(XMLWebService.class.getName()).log(Level.WARNING, "Error closing http request stream for URL:" + url, ex);
                    }
                }
            }
        }
    }

    class SAXHandler extends DefaultHandler {

        StringBuffer buffer = new StringBuffer();

        StringBuffer clientElement = null;

        String clientElementName = null;

        int nestedCount = 0;

        private static final String XML_TAG = "<?xml version='1.0' encoding='UTF-8'?>";

        @Override
        public void startDocument() throws SAXException {
            if (cancel == true) {
                throw new SAXException("User canceled request.");
            }
            if (isNoBuffer() == false) {
                buffer.append(XML_TAG);
            } else {
                buffer = null;
            }
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (cancel == true) {
                throw new SAXException("User canceled request.");
            }
            String name = "".equals(localName) ? qName : localName;
            StringBuffer tempElement = new StringBuffer();
            tempElement.append("<" + name);
            if (attributes != null) {
                for (int i = 0; i < attributes.getLength(); i++) {
                    String aName = attributes.getLocalName(i);
                    if ("".equals(aName)) {
                        aName = attributes.getQName(i);
                    }
                    tempElement.append(" ");
                    tempElement.append(aName + "=\"" + attributes.getValue(i) + "\"");
                }
            }
            tempElement.append(">");
            if (clientElement != null && name.equals(clientElementName)) {
                nestedCount++;
                clientElement.append(tempElement);
            } else if (clientElement == null && nodes.contains(name) && nestedCount == 0) {
                clientElement = new StringBuffer();
                clientElement.append(XML_TAG);
                clientElement.append(tempElement);
                clientElementName = name;
            } else if (clientElement != null) {
                clientElement.append(tempElement);
            }
            if (buffer != null) {
                buffer.append(tempElement);
            }
        }

        @Override
        public void characters(char ch[], int start, int length) throws SAXException {
            if (cancel == true) {
                throw new SAXException("User canceled request.");
            }
            String currentData = new String(ch, start, length).trim();
            if (!"".equals(currentData)) {
                if (buffer != null) {
                    buffer.append(ch, start, length);
                }
                if (clientElement != null) {
                    clientElement.append(ch, start, length);
                }
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (cancel == true) {
                throw new SAXException("User canceled request.");
            }
            String name = "".equals(localName) ? qName : localName;
            if (clientElement != null && name.equals(clientElementName) && nestedCount == 0) {
                clientElement.append("</" + clientElementName + ">");
                final String ce = new String(clientElement);
                final String ceN = new String(clientElementName);
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        if (client != null) {
                            client.dataRecieved(ceN, ce);
                        }
                    }
                });
                clientElement = null;
                clientElementName = null;
                nestedCount = 0;
            } else if (clientElement != null && name.equals(clientElementName) && nestedCount > 0) {
                nestedCount--;
                clientElement.append("</" + name + ">");
            } else if (clientElement != null) {
                clientElement.append("</" + name + ">");
            }
            if (buffer != null) {
                buffer.append("</" + name + ">");
            }
        }

        @Override
        public void endDocument() throws SAXException {
            if (cancel == true) {
                throw new SAXException("User canceled request.");
            }
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    if (client != null) {
                        if (buffer != null) {
                            client.requestComplete(buffer.toString());
                        } else {
                            client.requestComplete(null);
                        }
                    }
                    clientElement = null;
                    clientElementName = null;
                    buffer = null;
                    nestedCount = 0;
                }
            });
        }
    }

    class ResponseInputStream extends InputStream {

        long bytesRead = 0;

        long notifyCount = 0;

        InputStream wrappedInputStream = null;

        public ResponseInputStream(InputStream stream) throws Exception {
            if (stream == null) {
                throw new Exception("InputStream can not be null.");
            }
            this.wrappedInputStream = stream;
        }

        public long getBytesRead() {
            return bytesRead;
        }

        @Override
        public int available() throws IOException {
            checkCancel();
            return wrappedInputStream.available();
        }

        @Override
        public void close() throws IOException {
            checkCancel();
            wrappedInputStream.close();
        }

        @Override
        public void mark(int readLimit) {
            wrappedInputStream.mark(readLimit);
        }

        @Override
        public boolean markSupported() {
            return wrappedInputStream.markSupported();
        }

        @Override
        public int read() throws IOException {
            checkCancel();
            bytesRead++;
            checkClientNotify(1);
            return wrappedInputStream.read();
        }

        @Override
        public int read(byte b[]) throws IOException {
            checkCancel();
            int r = wrappedInputStream.read(b);
            bytesRead += r;
            checkClientNotify(r);
            return wrappedInputStream.read(b);
        }

        @Override
        public int read(byte b[], int off, int len) throws IOException {
            checkCancel();
            int r = wrappedInputStream.read(b, off, len);
            bytesRead += r;
            checkClientNotify(r);
            return r;
        }

        @Override
        public void reset() throws IOException {
            checkCancel();
            wrappedInputStream.reset();
        }

        @Override
        public long skip(long n) throws IOException {
            checkCancel();
            long r = wrappedInputStream.skip(n);
            bytesRead += r;
            checkClientNotify(r);
            return r;
        }

        private void checkCancel() throws IOException {
            if (cancel == true) {
                throw new IOException("User canceled download.");
            }
        }

        private void checkClientNotify(long currentRead) {
            notifyCount += currentRead;
            if (notifyCount >= notifyInterval) {
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        client.downloadProgress(bytesRead);
                    }
                });
                notifyCount = 0;
            }
        }
    }

    private String url;

    private List<String> nodes = new ArrayList<String>();

    private ServiceClient client = null;

    private byte[] buffer = null;

    private HttpMethod method = HttpMethod.GET;

    private HttpClient httpClient;

    private Map<String, String> parameters;

    private boolean cancel = false;

    /**
     * Set no buffer to true, and the parser will not buffer the entire document.
     * The requestComplete method will still be called but the parameter for XML
     * will be null.
     */
    private boolean noBuffer = false;

    /**
     * Number of bytes to read before notifying the client of the download progress.
     */
    private long notifyInterval = 100;
}
