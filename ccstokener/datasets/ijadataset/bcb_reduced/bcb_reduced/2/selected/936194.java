package org.doit.muffin.filter;

import java.util.*;
import java.io.*;
import java.net.*;
import java.text.DateFormat;
import org.doit.muffin.*;

/** Filter to cache web content.
 *
 * Caching helps some, but it isn't very effective. Too much web content is dynamic, and too many
 * web hosts explicitly defeat caches. Ad revenue, tracking, etc.
 *
 * Cache file format is
 *
 *     <properties>
 *     BLANKLINE
 *     <http status line>
 *     <http headers>
 *     BLANKLINE
 *     <content>
 *
 * Read and write a cached message's properties using readProperties() and writeProperties().
 *
 * CacheFilter implements the RequestFilter, ReplyFilter, and HttpFilter interfaces. Note that it also
 * implements HttpRelay, a superinterface of HttpFilter.
 */
public class CacheFilter implements RequestFilter, ReplyFilter, HttpFilter {

    final int MaxCachedObjectSize = 1000000;

    /** Pragma attribute name.
     */
    final String PragmaAttribute = "Pragma";

    /** Cache-control attribute name.
     */
    final String CacheControlAttribute = "Cache-control";

    /** Authorization attribute name.
     */
    final String AuthorizationAttribute = "Authorization";

    /** Expires attribute name.
     */
    final String ExpiresAttribute = "Expires";

    /** Location attribute name.
     */
    final String LocationAttribute = "Location";

    /** Etag attribute name.
     */
    final String EtagAttribute = "Etag";

    /** Last-modified attribute name.
     */
    final String LastModifiedAttribute = "Last-Modified";

    /** If-Modified-Since attribute name.
     */
    final String IfModifiedSinceAttribute = "If-Modified-Since";

    /** Date attribute name.
     */
    final String DateAttribute = "Date";

    /** Content-length attribute name.
     */
    final String ContentLengthAttribute = "Content-length";

    /** X-Muffin-From-cache attribute
     */
    final String FromCacheAttribute = "X-Muffin-From-cache";

    /** No-cache value.
     */
    final String NoCache = "no-cache";

    /** Cache factory, i.e. generator for Cache instances.
     */
    Cache factory;

    /** Preferences for this filter.
     */
    Prefs prefs;

    /** Set of urls we've checked this session
     */
    HashSet checkedUrls = new HashSet();

    /** Create an instance of CacheFilter.
     */
    public CacheFilter(Cache factory) {
        this.factory = factory;
    }

    /** Set the preferences for this filter.
     */
    public void setPrefs(Prefs prefs) {
        this.prefs = prefs;
    }

    /** Filter a request.
     */
    public synchronized void filter(Request request) throws FilterException {
    }

    /** If the reply's not already cached, cache it.
     */
    public synchronized void filter(Reply reply) throws FilterException {
        if (reply != null) {
            if (reply.getRequest() != null) {
                if (isReplyCachable(reply)) {
                    class CacheInputStream extends BufferedInputStream {

                        Reply r;

                        public CacheInputStream(Reply r) {
                            super(r.getContent());
                            this.r = r;
                            r.setContent(this);
                            mark(MaxCachedObjectSize);
                        }

                        public void close() throws IOException {
                            factory.cacheReply(r);
                        }
                    }
                    reply.setContent(new CacheInputStream(reply));
                    String url = Cache.getURL(reply.getRequest());
                    checkedUrls.add(url);
                }
            } else {
                factory.report("null request in filter (Reply)");
            }
        } else {
            factory.report("null reply in filter");
        }
    }

    /** Returns whether this filter wants to provide a reply to this request.
     *  We do if we have a cached reply.
     */
    public synchronized boolean wantRequest(Request request) {
        String url = Cache.getURL(request);
        boolean weWantToProcessThisRequest;
        if (isGET(request)) {
            weWantToProcessThisRequest = isReplyCached(request);
            if (weWantToProcessThisRequest) {
                String pathname = factory.getCacheFile(url).getAbsolutePath();
                factory.report("from cache: " + url + ": " + pathname);
            } else {
                factory.report("from net: " + url);
            }
        } else {
            weWantToProcessThisRequest = false;
        }
        return weWantToProcessThisRequest;
    }

    /** Send a request
     *
     * Only called if wantRequest returns true.
     *
     * We don't need to send the request since the reply is in the cache.
     */
    public void sendRequest(Request request) {
    }

    /** Provide a cached reply to a request.
     *
     * Only called if wantRequest returns true.
     */
    public synchronized Reply recvReply(Request request) {
        factory.report("receiving a cached reply: " + Cache.getURL(request));
        String url = Cache.getURL(request);
        Reply reply = factory.getCachedReply(url);
        reply.setRequest(request);
        reply.setHeaderField(FromCacheAttribute, Boolean.TRUE.toString());
        return reply;
    }

    /** Close the http relay.
     */
    public void close() {
    }

    private boolean isRequestCachable(Request request) {
        String pragmaValue = request.getHeaderField(PragmaAttribute);
        String cacheControlValue = request.getHeaderField(CacheControlAttribute);
        String authorizationValue = request.getHeaderField(AuthorizationAttribute);
        String url = Cache.getURL(request);
        boolean cachable = true;
        String reason = "";
        if (NoCache.equalsIgnoreCase(pragmaValue)) {
            cachable = false;
            reason = PragmaAttribute + " is " + pragmaValue;
        } else if (NoCache.equalsIgnoreCase(cacheControlValue)) {
            cachable = false;
            reason = CacheControlAttribute + " is " + cacheControlValue;
        } else if (authorizationValue != null) {
            cachable = false;
            reason = AuthorizationAttribute + " supplied";
        } else if (!isGET(request)) {
            cachable = false;
            reason = "not GET (" + request + ")";
        } else if (url == null) {
            cachable = false;
            reason = "url is null";
        } else if (url.indexOf('?') >= 0) {
            cachable = false;
            reason = "url contains '?'";
        } else if (url.toLowerCase().indexOf("cgi") >= 0) {
            cachable = false;
            reason = "url contains \"cgi\"";
        } else if (url.toLowerCase().indexOf("servlet") >= 0) {
            cachable = false;
            reason = "url contains \"servlet\"";
        }
        if (cachable) {
        } else {
            factory.report("request not cachable: " + url + ": " + reason);
        }
        return cachable;
    }

    private boolean isReplyCachable(Reply reply) {
        boolean cachable = false;
        String reason = null;
        String alreadyCachedValue = reply.getHeaderField(FromCacheAttribute);
        if (alreadyCachedValue == null || !Boolean.getBoolean(alreadyCachedValue)) {
            int statusCode = reply.getStatusCode();
            if (statusCode == HttpURLConnection.HTTP_OK) {
                if (isGET(reply.getRequest())) {
                    cachable = true;
                } else {
                    reason = "not GET";
                }
            } else {
                reason = "status code is " + String.valueOf(statusCode);
            }
        } else {
            reason = "already cached";
        }
        if (!cachable) {
            String url = Cache.getURL(reply.getRequest());
            factory.report("reply not cachable: " + url + ": " + reason);
        }
        return cachable;
    }

    /** Returns whether we have a cached reply to this request.
     */
    public boolean isReplyCached(Request request) {
        String url = Cache.getURL(request);
        return isRequestCachable(request) && factory.isCached(url) && !needRefresh(url);
    }

    private boolean needRefresh(String url) {
        boolean refresh = false;
        String reason = "";
        if (factory.checkForUpdatesAlways() || (factory.checkForUpdatesOncePerSession() && !checkedUrls.contains(url))) {
            try {
                Reply cachedCopy = factory.getCachedReply(url);
                Date now = new Date();
                Date expirationDate = parseDate(cachedCopy.getHeaderField(ExpiresAttribute));
                if (expirationDate != null && now.after(expirationDate)) {
                    refresh = true;
                    reason = "url expired";
                } else {
                    try {
                        int responseCode = 0;
                        HttpURLConnection netCopy = getNetHead(url);
                        if (netCopy != null) {
                            responseCode = netCopy.getResponseCode();
                        }
                        if (responseCode == HttpURLConnection.HTTP_NOT_MODIFIED) {
                            reason = "not modified";
                            refresh = false;
                        } else if (responseCode == HttpURLConnection.HTTP_OK) {
                            if (fieldChanged(cachedCopy, netCopy, EtagAttribute)) {
                                refresh = true;
                                reason = EtagAttribute + " changed";
                            } else if (fieldChanged(cachedCopy, netCopy, LastModifiedAttribute)) {
                                refresh = true;
                                reason = LastModifiedAttribute + " changed";
                            } else if (fieldChanged(cachedCopy, netCopy, ContentLengthAttribute)) {
                                refresh = true;
                                reason = ContentLengthAttribute + " changed";
                            } else {
                                if (netCopy.getHeaderField(EtagAttribute) == null && netCopy.getHeaderField(LastModifiedAttribute) == null && fieldChanged(cachedCopy, netCopy, DateAttribute)) {
                                    refresh = true;
                                    reason = DateAttribute + " changed";
                                }
                            }
                        } else {
                            if (reason == null) {
                                reason = "bad HEAD result code: " + String.valueOf(responseCode);
                            }
                            refresh = false;
                        }
                    } catch (Exception e) {
                        reason = "need refresh: exception: " + e;
                        e.printStackTrace();
                        refresh = false;
                    }
                }
            } catch (Exception e) {
                reason = "need refresh: " + e;
                e.printStackTrace();
                refresh = true;
            }
            if (Cache.Debugging) {
                if (refresh) {
                    String message = "need refresh: " + url + ": " + reason;
                    factory.report(message);
                }
            }
            checkedUrls.add(url);
        }
        return refresh;
    }

    private boolean isGET(Request request) {
        final String HttpGet = "GET";
        boolean is = false;
        if (request != null) {
            if (request.getRequest() != null) {
                is = request.getRequest().toUpperCase().startsWith(HttpGet);
            } else {
                factory.report("null request type in isGet");
            }
        } else {
            factory.report("null request in isGet");
        }
        return is;
    }

    /** 
     * @returns parsed Date, or null if not parseable.
     */
    private Date parseDate(String dateString) {
        Date date;
        try {
            DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL, Locale.ENGLISH);
            dateFormat.setTimeZone(Cache.GMT);
            dateFormat.setLenient(true);
            date = dateFormat.parse(dateString);
        } catch (Exception e) {
            date = null;
        }
        return date;
    }

    private boolean fieldChanged(Reply cachedCopy, HttpURLConnection netCopy, String attribute) {
        String cachedValue = cachedCopy.getHeaderField(attribute);
        String netValue = netCopy.getHeaderField(attribute);
        if (attribute.equalsIgnoreCase(ContentLengthAttribute)) {
            String gzipLength = cachedCopy.getHeaderField(Reply.GzipContentLengthAttribute);
            if (gzipLength != null) {
                cachedValue = gzipLength;
            }
        }
        boolean changed = (cachedValue == null && netValue != null) || (cachedValue != null && netValue == null) || (cachedValue != null && netValue != null && (!cachedValue.equals(netValue)));
        if (changed) {
            factory.report("need refresh: " + netCopy.getURL() + ": field " + attribute + " changed from '" + cachedValue + "' to '" + netValue + "'");
        }
        return changed;
    }

    public static synchronized void reportHeaders(Cache factory, Message message) {
        final String Indent = "    ";
        if (message instanceof Request) {
            Request r = (Request) message;
            factory.report("request: " + r.getRequest());
        } else if (message instanceof Reply) {
            Reply r = (Reply) message;
            Request request = r.getRequest();
            if (request != null) {
                factory.report("reply: " + Cache.getURL(request));
            } else {
                factory.report("cache:");
            }
            factory.report(Indent + r.getStatusLine());
        } else {
            factory.report("unknown message type:");
        }
        Enumeration keys = message.getHeaders();
        while (keys.hasMoreElements()) {
            String name = keys.nextElement().toString();
            String value = message.getHeaderField(name);
            String line = Indent + name + ":" + value;
            factory.report(line);
        }
    }

    public static synchronized void reportURLConnectionHeaders(Cache factory, HttpURLConnection http) {
        final String Indent = "    ";
        factory.report("net: " + http.getRequestMethod() + " " + http.getURL());
        try {
            factory.report(Indent + String.valueOf(http.getResponseCode()) + " " + http.getResponseMessage());
        } catch (IOException ioe) {
        }
        int i = 1;
        String key = http.getHeaderFieldKey(i);
        while (key != null) {
            String value = http.getHeaderField(key);
            String line = Indent + key + ":" + value;
            factory.report(line);
            ++i;
            key = http.getHeaderFieldKey(i);
        }
    }

    private HttpURLConnection getNetHead(String url) throws IOException {
        HttpURLConnection netCopy = null;
        HttpURLConnection.setFollowRedirects(true);
        final String HeadRequestMethod = "HEAD";
        try {
            netCopy = (HttpURLConnection) new URL(url).openConnection();
            if (Cache.Debugging) {
                factory.report("got net head for " + url);
            }
        } catch (Exception e) {
            factory.report("unable to get net head for " + url + ": " + e);
        }
        netCopy.setRequestMethod(HeadRequestMethod);
        netCopy.connect();
        if (Cache.Debugging) {
        }
        return netCopy;
    }
}
