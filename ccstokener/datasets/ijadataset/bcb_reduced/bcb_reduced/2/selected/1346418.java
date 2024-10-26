package marquee.xmlrpc.connections;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import marquee.xmlrpc.XmlRpcClientConnection;
import marquee.xmlrpc.XmlRpcClientConnectionFactory;

/**
 *  A factory for connections that use the <code>java.net.URL</code>
 *  infrastructure.  These connections are useful in an applet because
 *  any proxy and security settings in the browser are used.
 *
 *  @author <a href="mailto:tobya@peace.com">Toby Allsopp</a>
 *  @version $Revision: 1.1 $
 */
public class URLConnectionFactory implements XmlRpcClientConnectionFactory {

    /**
     *  Initializes the factory to create connections to the XML-RPC server at
     *  the specified URL.
     */
    public URLConnectionFactory(URL url) {
        this.url = url;
    }

    public XmlRpcClientConnection createConnection() throws IOException {
        return new URLConnectionImpl();
    }

    /**
     *  The Connection implementation this factory is based on.
     */
    private class URLConnectionImpl implements XmlRpcClientConnection {

        /**
         *  Opens and initializes the internal URLConnection.
         */
        private URLConnectionImpl() throws IOException {
            urlConnection = url.openConnection();
            urlConnection.setDoOutput(true);
            urlConnection.setDoInput(true);
            urlConnection.setRequestProperty("Content-Type", "text/xml");
        }

        public void setOutputContentLength(int contentLength) {
            urlConnection.setRequestProperty("Content-Length", String.valueOf(contentLength));
        }

        public void setKeepAlive(boolean keepAlive) {
            urlConnection.setRequestProperty("Connection", keepAlive ? "keep-alive" : "close");
        }

        public void setGzip(boolean gzip) {
            if (gzip) {
                urlConnection.setRequestProperty("Accept-Encoding", "gzip");
            }
        }

        public OutputStream getOutputStream() throws IOException {
            output = urlConnection.getOutputStream();
            return output;
        }

        public InputStream getInputStream() throws IOException {
            output.close();
            return urlConnection.getInputStream();
        }

        public int getInputContentLength() {
            return urlConnection.getContentLength();
        }

        public boolean isServerKeepAlive() {
            String connectionHeader = urlConnection.getHeaderField("connection");
            return (connectionHeader != null) && connectionHeader.equals("keep-alive");
        }

        public boolean isInputGzipped() {
            String encodingHeader = urlConnection.getContentEncoding();
            return (encodingHeader != null) && encodingHeader.equals("gzip");
        }

        public void close() throws IOException {
            if (urlConnection instanceof HttpURLConnection) {
                ((HttpURLConnection) urlConnection).disconnect();
            }
        }

        /** The connection to the XML-RPC server */
        private URLConnection urlConnection;

        /** OutputStream to server */
        private OutputStream output;
    }

    /** The URL this factory creates connections for */
    private final URL url;
}
