package sk.tonyb.library.web.downloader;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import org.apache.log4j.Logger;
import sk.tonyb.library.codebook.Encoding;

/**
 * Class, which provides getting a web file with proper encoding. <br>
 * All methods of this class are synchronized. <br>
 * <br>
 * Source code is based on {@link http://stackoverflow.com/questions/5131358/access-denied-when-trying-to-get-webpage-from-a-website}. <br>
 * 
 * @since december 2010
 * @last_modified 29.09.2011, Anton Balucha
 */
public final class WebDocumentDownloader {

    /** log4j logger */
    private static Logger logger = Logger.getLogger(WebDocumentDownloader.class);

    private String urlString = null;

    private Encoding encoding = null;

    private java.util.Map<String, java.util.List<String>> responseHeader = null;

    private java.net.URL responseUrl = null;

    private int responseCode = -1;

    private String mimeType = null;

    private String charset = null;

    private Object content = null;

    private static final int connectionTimeout = 10000;

    private static final int readTimeout = 10000;

    private static final String EXCEPTION_MESSAGE = "URL protocol must be HTTP.";

    /** User-agent */
    private static final String PROPERTY_USER_AGENT_NAME = "User-agent";

    /** Opera/9.80 (Windows NT 5.1; U; en-GB) Presto/2.2.15 Version/10.00 */
    private static final String PROPERTY_USER_AGENT_VALUE = "Opera/9.80 (Windows NT 5.1; U; en-GB) Presto/2.2.15 Version/10.00";

    /**
	 * Constructor, which open web file. Default encoding is UTF-8.
	 * 
	 * @param urlString - URL of page, which will be downloaded.
	 */
    public WebDocumentDownloader(String urlString) {
        this(urlString, Encoding.UTF_8);
    }

    /**
	 * Constructor, which open web file.
	 * 
	 * @param urlString - URL of page, which will be downloaded.
	 * @param encoding - encoding of downloaded web page.
	 */
    public WebDocumentDownloader(String urlString, Encoding encoding) {
        this.urlString = urlString;
        this.encoding = encoding;
        this.init();
    }

    /** Method, which initialize variables and download page. */
    private synchronized void init() {
        java.net.URL url = null;
        java.net.URLConnection urlConnection = null;
        java.net.HttpURLConnection httpUrlConnection = null;
        int length = 0;
        String type = null;
        String[] parts = null;
        String t = null;
        int index = 0;
        java.io.InputStream inputStream = null;
        try {
            url = new java.net.URL(this.urlString);
            urlConnection = url.openConnection();
            if (!(urlConnection instanceof java.net.HttpURLConnection)) {
                throw new java.lang.IllegalArgumentException(WebDocumentDownloader.EXCEPTION_MESSAGE);
            }
            httpUrlConnection = (java.net.HttpURLConnection) urlConnection;
            httpUrlConnection.setConnectTimeout(WebDocumentDownloader.connectionTimeout);
            httpUrlConnection.setReadTimeout(WebDocumentDownloader.readTimeout);
            httpUrlConnection.setInstanceFollowRedirects(true);
            httpUrlConnection.setRequestProperty("User-Agent", "Opera/9.80 (Windows NT 5.1; U; en-GB) Presto/2.2.15 Version/10.00");
            httpUrlConnection.setRequestProperty("Accept", "text/html, application/xml;q=0.9, application/xhtml+xml, image/png, image/jpeg, image/gif, image/x-xbitmap, */*;q=0.1");
            httpUrlConnection.setRequestProperty("Accept-Language", "sk-SK,sk;q=0.9,en;q=0.8");
            httpUrlConnection.setRequestProperty("Accept-Charset", "iso-8859-1, utf-8, utf-16, *;q=0.1");
            httpUrlConnection.setRequestProperty("Connection", "Keep-Alive, TE");
            httpUrlConnection.setRequestProperty("TE", "deflate, gzip, chunked, identity, trailers");
            httpUrlConnection.connect();
            this.responseHeader = httpUrlConnection.getHeaderFields();
            this.responseCode = httpUrlConnection.getResponseCode();
            this.responseUrl = httpUrlConnection.getURL();
            length = httpUrlConnection.getContentLength();
            type = httpUrlConnection.getContentType();
            if (type != null) {
                parts = type.split(";");
                this.mimeType = parts[0].trim();
                for (int i = 1; i < parts.length && this.charset == null; i++) {
                    t = parts[i].trim();
                    index = t.toLowerCase().indexOf("charset=");
                    if (index != -1) {
                        this.charset = t.substring(index + 8);
                    }
                }
            }
            inputStream = httpUrlConnection.getErrorStream();
            if (inputStream != null) {
                this.content = this.readStream(length, inputStream);
            } else if ((this.content = httpUrlConnection.getContent()) != null && this.content instanceof java.io.InputStream) {
                this.content = this.readStream(length, (java.io.InputStream) this.content);
            }
            httpUrlConnection.disconnect();
        } catch (IllegalArgumentException e) {
            logger.error("IllegalArgumentException", e);
        } catch (IOException e) {
            logger.error("IOException", e);
        } catch (Exception e) {
            logger.error("Exception", e);
        }
    }

    /** Method, which read stream bytes and transcode. */
    private Object readStream(int length, java.io.InputStream inputStream) throws java.io.IOException {
        int bufferLength = 0;
        byte[] buffer = null;
        byte[] bytes = null;
        byte[] newBytes = null;
        bufferLength = Math.max(1024, Math.max(length, inputStream.available()));
        buffer = new byte[bufferLength];
        ;
        for (int nRead = inputStream.read(buffer); nRead != -1; nRead = inputStream.read(buffer)) {
            if (bytes == null) {
                bytes = buffer;
                buffer = new byte[bufferLength];
                continue;
            }
            newBytes = new byte[bytes.length + nRead];
            System.arraycopy(bytes, 0, newBytes, 0, bytes.length);
            System.arraycopy(buffer, 0, newBytes, bytes.length, nRead);
            bytes = newBytes;
        }
        if (this.charset == null) {
            return bytes;
        }
        try {
            return new String(bytes, this.charset);
        } catch (java.io.UnsupportedEncodingException e) {
            logger.error("UnsupportedEncodingException", e);
        } catch (Exception e) {
            logger.error("Exception", e);
        }
        return bytes;
    }

    /** Method, which return generic content. */
    public Object getContentGeneric() {
        return this.content;
    }

    /** Method, which return HTML file. First, method check, if downloaded file is text/html, then convert generic object to text value as object of String. */
    public String getContentHtml() {
        String page = null;
        String s = null;
        byte[] b = null;
        try {
            if (this.content != null && this.mimeType.equals("text/html") && this.charset == null) {
                b = (byte[]) this.content;
                page = new String(b, this.encoding.getValue());
            } else if (this.content != null && this.mimeType.equals("text/html") && this.charset != null) {
                s = (String) this.content;
                page = new String(s.getBytes(this.charset), this.encoding.getValue());
            }
        } catch (UnsupportedEncodingException e) {
            logger.error("UnsupportedEncodingException", e);
        } catch (Exception e) {
            logger.error("Exception", e);
        }
        return page;
    }

    /** Method, which return response code. */
    public int getResponseCode() {
        return this.responseCode;
    }

    /** Method, which return response header. */
    public java.util.Map<String, java.util.List<String>> getHeaderFields() {
        return this.responseHeader;
    }

    /** Method, which return URL of the received page. */
    public java.net.URL getURL() {
        return this.responseUrl;
    }

    /** Method, which return MIME type. */
    public String getMimeType() {
        return this.mimeType;
    }

    /** Method, which return charset. */
    public String getCharset() {
        return this.charset;
    }

    /** 
	 * Method, which return short information about downloaded page. <br> 
	 * Information contains:
	 * <ul>
	 * <li>response code</li>
	 * <li>response header</li>
	 * <li>response url</li>
	 * <li>mime type</li>
	 * <li>charset</li>
	 * </ul>
	 */
    @Override
    public String toString() {
        return "[responseCode = " + this.responseCode + "] [responseHeader = " + this.responseHeader.toString() + "] [url = " + this.responseUrl + "] [mimeType = " + this.mimeType + "] [charset = " + this.charset + "]";
    }
}
