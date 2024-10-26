package org.logitest;

import java.io.*;
import java.net.*;
import java.util.*;
import java.security.KeyStore;
import javax.net.ssl.*;
import javax.security.cert.X509Certificate;
import com.sun.net.ssl.*;
import org.apache.oro.text.perl.Perl5Util;
import org.apache.log4j.*;
import org.jdom.*;
import org.jdom.input.*;
import org.jdom.output.*;
import org.w3c.tidy.Tidy;
import org.logitest.util.*;
import org.logitest.event.*;

/** An abstract implementation of the Browser interface.

	@author Anthony Eden
*/
public abstract class AbstractBrowser implements Browser {

    /** Implementations must override this method to open the given URL.  This
		method is called when the setPage() method is invoked.
		
		@param url The URL
		@throws IOException
	*/
    protected abstract void openPage(URL url) throws IOException;

    /** Set the content type.
	
		@param contentType The content type (i.e. text/html).
	*/
    protected abstract void setContentType(String contentType);

    /** Set the current page.
	
		@param page The URL
		@throws IOException
	*/
    public void setPage(URL page) throws IOException {
        getBrowserListenerSupport().fireStartLoading(this, page);
        openPage(page);
        History history = getHistory();
        URL lastURL = history.last();
        if (history.isEmpty() || (lastURL != null && !page.sameFile(lastURL))) {
            history.add(page);
        }
        getBrowserListenerSupport().fireEndLoading(this, page);
    }

    /** Set the current page using the given resource's information.
	
		@param resource The Resource
		@param variables The variables currently set in the test
		@throws IOException
	*/
    public void setPage(Resource resource, Variables variables) throws IOException {
        this.resource = resource;
        String resourceURLString = resource.getFullURL().toString();
        String pageURLString = variables.replaceNames(resourceURLString);
        URL pageURL = new URL(pageURLString);
        setPage(pageURL);
    }

    /**	Set the current page to the given URL String.
	
		@param url The URL String
	*/
    public void setPage(String url) throws IOException {
        setPage(new URL(url));
    }

    /** Retrieve a document from a web server and return an input stream to read
		the documents contents.  The maximum number of redirects that will be
		followed is specified by <code>DEFAULT_MAXIMUM_REDIRECTS</code>.  Any
		specified resource post data will be included in the request.

		@param url The URL of the document to be retrieved.
		@return The input stream for reading the document's contents
		@throws IOException
	*/
    public InputStream getStream(URL url) throws IOException {
        return getStream(url, DEFAULT_MAXIMUM_REDIRECTS, true);
    }

    /** Retrieve a document from a web server and return an input stream to read
		the documents contents.

		@param url The URL of the document to be retrieved.
		@param maximumRedirects The maximum number of redirects that should be
			followed.
		@param sendPostData If true and a resource has been provided previously,
			then any post parameters of the resource are included with the 
			request.  This parameter should be set to false when following 
			redirects.
		@return The input stream for reading the document's contents
		@throws IOException
	*/
    public InputStream getStream(URL url, int maximumRedirects, boolean sendPostData) throws IOException {
        log.debug("Opening stream to " + url);
        if (url.getProtocol().equals("https")) {
            SSLSocketFactory factory = HttpsURLConnection.getDefaultSSLSocketFactory();
            if (factory != ourSSLSocketFactory) {
                if (ourSSLSocketFactory == null) {
                    log.debug("Creating a new SSLSocketFactory");
                    try {
                        Preferences preferences = Preferences.getInstance();
                        SSLContext ctx = SSLContext.getInstance("TLS");
                        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
                        KeyStore ks = KeyStore.getInstance(preferences.getKeyStoreType());
                        char[] passphrase = preferences.getKeyStorePassPhrase().toCharArray();
                        char[] keypassphrase = preferences.getKeyPassPhrase().toCharArray();
                        ks.load(new FileInputStream(preferences.getKeyStoreLocation()), passphrase);
                        kmf.init(ks, keypassphrase);
                        ctx.init(kmf.getKeyManagers(), null, null);
                        ourSSLSocketFactory = ctx.getSocketFactory();
                    } catch (Exception e) {
                        throw new IOException(e.getMessage());
                    }
                }
                HttpsURLConnection.setDefaultSSLSocketFactory(ourSSLSocketFactory);
            }
        }
        URLConnection connection = url.openConnection();
        connection.setUseCaches(false);
        connection.setDoOutput(true);
        connection.setDoInput(true);
        if (connection instanceof HttpURLConnection) {
            ((HttpURLConnection) connection).setInstanceFollowRedirects(false);
        }
        String postString = getPOSTString();
        if (resource != null) {
            if (connection instanceof HttpURLConnection) {
                ((HttpURLConnection) connection).setRequestMethod(resource.getMethod());
            }
            log.debug("Sending request headers.");
            Iterator requestHeaders = resource.getRequestHeaders().iterator();
            while (requestHeaders.hasNext()) {
                NameValuePair requestHeader = (NameValuePair) requestHeaders.next();
                connection.setRequestProperty(requestHeader.getName(), requestHeader.getValue());
            }
            log.debug("Setting request method to " + resource.getMethod());
            if (resource.getMethod().equals(Resource.POST) && sendPostData) {
                String contentLength = Integer.toString(postString.length());
                connection.setRequestProperty("Content-Length", contentLength);
                System.out.println("Content length set to " + contentLength);
            }
        } else {
            if (connection instanceof HttpURLConnection) {
                ((HttpURLConnection) connection).setRequestMethod("GET");
            }
        }
        Iterator cookies = getCookies().getApplicableCookies(url);
        if (cookies.hasNext()) {
            log.debug("Setting cookies.");
        }
        while (cookies.hasNext()) {
            Cookie cookie = (Cookie) cookies.next();
            connection.setRequestProperty("Cookie", cookie.toString());
        }
        log.debug("Connecting.");
        connection.connect();
        log.debug("Connected.");
        if (resource != null && resource.getMethod().equals(Resource.POST) && sendPostData) {
            log.debug("Posting data.");
            OutputStream out = new BufferedOutputStream(connection.getOutputStream());
            byte[] bytes = postString.getBytes();
            for (int i = 0; i < bytes.length; i++) {
                out.write(bytes[i]);
            }
            out.flush();
            out.close();
        }
        log.debug("Storing cookies from server.");
        String cookieField = connection.getHeaderField("Set-Cookie");
        if (cookieField != null) {
            setCookies(url, cookieField);
        }
        log.debug("Storing headers.");
        storeHeaders(connection);
        if (connection instanceof HttpURLConnection) {
            HttpURLConnection httpConnection = (HttpURLConnection) connection;
            int response = httpConnection.getResponseCode();
            log.debug("HTTP response code: " + response);
            if (response >= 300 && response <= 399) {
                String loc = connection.getHeaderField("Location");
                if (loc.startsWith("http", 0)) {
                    url = new URL(loc);
                } else {
                    url = new URL(url, loc);
                }
                log.debug("Redirect: " + url);
                if (maximumRedirects > 0) {
                    return getStream(url, maximumRedirects - 1, false);
                } else {
                    throw new IOException("Maximum number of redirects exceeded");
                }
            } else if (response == 401) {
                log.debug("Authorization required.");
            }
        }
        String contentType = connection.getContentType();
        log.debug("Connection believes content type is: " + contentType);
        if (contentType == null) {
            contentType = "text/html";
        }
        log.debug("Setting the content type to: " + contentType);
        setContentType(contentType);
        log.debug("Getting the connection's InputStream.");
        InputStream in = connection.getInputStream();
        ByteArrayOutputStream byteArrayOut = new ByteArrayOutputStream();
        log.debug("Parsing the input stream.");
        DOMBuilder builder = new DOMBuilder();
        document = builder.build(getTidy().parseDOM(in, byteArrayOut));
        log.debug("Parsing complete.");
        return new ByteArrayInputStream(byteArrayOut.toByteArray());
    }

    /**	Get the Document currently displayed in the browser.
	
		@return The JDOM Document currently displayed
	*/
    public Document getJDOMDocument() {
        return document;
    }

    /** Go forward.
	
		@throws IOException
	*/
    public synchronized void forward() throws IOException {
        History history = getHistory();
        if (history.canGoForward()) {
            URL url = history.forward();
            getBrowserListenerSupport().fireStartLoading(this, url);
            openPage(url);
            getBrowserListenerSupport().fireEndLoading(this, url);
        }
    }

    /** Go back.
	
		@throws IOException
	*/
    public synchronized void back() throws IOException {
        History history = getHistory();
        if (history.canGoBack()) {
            URL url = history.back();
            getBrowserListenerSupport().fireStartLoading(this, url);
            openPage(url);
            getBrowserListenerSupport().fireEndLoading(this, url);
        }
    }

    /**	Get the browser's History.
	
		@return The browser's History
	*/
    public History getHistory() {
        if (history == null) {
            history = new History();
        }
        return history;
    }

    /**	Get the cookies currently in the browser's memory.
	
		@return The browser's CookieList
	*/
    public CookieList getCookies() {
        if (cookies == null) {
            cookies = new CookieList();
        }
        return cookies;
    }

    /** Get the headers of the current page.
	
		@return A NameValuePairSet of name/value pairs
	*/
    public NameValuePairSet getResponseHeaders() {
        if (responseHeaders == null) {
            responseHeaders = new NameValuePairSet();
        }
        return responseHeaders;
    }

    public void addStatusListener(StatusListener l) {
        getStatusListenerSupport().addStatusListener(l);
    }

    public void removeStatusListener(StatusListener l) {
        getStatusListenerSupport().removeStatusListener(l);
    }

    protected StatusListenerSupport getStatusListenerSupport() {
        if (statusListenerSupport == null) {
            statusListenerSupport = new StatusListenerSupport();
        }
        return statusListenerSupport;
    }

    public void addLocationListener(LocationListener l) {
        getLocationListenerSupport().addLocationListener(l);
    }

    public void removeLocationListener(LocationListener l) {
        getLocationListenerSupport().removeLocationListener(l);
    }

    protected LocationListenerSupport getLocationListenerSupport() {
        if (locationListenerSupport == null) {
            locationListenerSupport = new LocationListenerSupport();
        }
        return locationListenerSupport;
    }

    public void addBrowserListener(BrowserListener l) {
        getBrowserListenerSupport().addBrowserListener(l);
    }

    public void removeBrowserListener(BrowserListener l) {
        getBrowserListenerSupport().removeBrowserListener(l);
    }

    protected BrowserListenerSupport getBrowserListenerSupport() {
        if (browserListenerSupport == null) {
            browserListenerSupport = new BrowserListenerSupport();
        }
        return browserListenerSupport;
    }

    /** Store all headers into a NameValuePairSet.
	
		@param connection The URLConnection
	*/
    protected void storeHeaders(URLConnection connection) {
        NameValuePairSet responseHeaders = getResponseHeaders();
        responseHeaders.clear();
        int i = 0;
        String headerName = connection.getHeaderFieldKey(i);
        if (headerName != null) {
            responseHeaders.add(new NameValuePair(headerName, connection.getHeaderField(i)));
        }
        i++;
        while ((headerName = connection.getHeaderFieldKey(i)) != null) {
            String headerValue = connection.getHeaderField(i);
            responseHeaders.add(new NameValuePair(headerName, headerValue));
            i++;
        }
    }

    protected void parseDocument() throws IOException {
        InputStream in = new ByteArrayInputStream(getText().getBytes());
        DOMBuilder builder = new DOMBuilder();
        document = builder.build(getTidy().parseDOM(in, null));
    }

    private String getPOSTString() {
        if (resource == null) {
            return "";
        }
        StringBuffer buffer = new StringBuffer();
        Iterator i = resource.getParameters().iterator();
        while (i.hasNext()) {
            NameValuePair parameter = (NameValuePair) i.next();
            buffer.append(URLEncoder.encode(parameter.getName()));
            buffer.append("=");
            buffer.append(URLEncoder.encode(parameter.getValue()));
            if (i.hasNext()) {
                buffer.append("&");
            }
        }
        return buffer.toString();
    }

    protected void setCookies(URL url, String cookieString) {
        List newCookies = Cookie.parse(url, cookieString);
        log.debug("New Cookies:");
        Iterator cookieIterator = newCookies.iterator();
        while (cookieIterator.hasNext()) {
            log.debug(cookieIterator.next().toString());
        }
        Iterator iter = newCookies.iterator();
        while (iter.hasNext()) {
            Cookie cookie = (Cookie) iter.next();
            log.debug("Checking cookie " + cookie.getName());
            log.debug("URL Path: " + url.getFile());
            log.debug("Cookie Path: " + cookie.getPath());
            if (!url.getFile().startsWith(cookie.getPath())) {
                log.debug("Cookie is not valid.");
                continue;
            }
            log.debug("Adding cookie.");
            getCookies().add(cookie);
        }
        log.debug("Cookies After addAll():");
        cookieIterator = getCookies().iterator();
        while (cookieIterator.hasNext()) {
            log.debug(cookieIterator.next().toString());
        }
    }

    private Tidy getTidy() {
        if (tidy == null) {
            tidy = new Tidy();
            tidy.setQuiet(true);
            tidy.setShowWarnings(false);
            tidy.setTidyMark(false);
        }
        return tidy;
    }

    private static final int DEFAULT_MAXIMUM_REDIRECTS = 5;

    private static Category log = Category.getInstance(AbstractBrowser.class.getName());

    private static SSLSocketFactory ourSSLSocketFactory = null;

    protected Document document;

    protected History history;

    protected CookieList cookies;

    protected Resource resource;

    protected NameValuePairSet responseHeaders;

    protected StatusListenerSupport statusListenerSupport;

    protected LocationListenerSupport locationListenerSupport;

    protected BrowserListenerSupport browserListenerSupport;

    private Tidy tidy;
}
