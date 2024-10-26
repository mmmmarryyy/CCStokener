package org.las.crawler;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParamBean;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.las.tools.Md5;
import org.las.tools.URLCanonicalizer;
import org.las.tools.MIMEFormater.MIMEFormater;

public final class Fetcher {

    private final Logger logger = Logger.getLogger(Fetcher.class);

    private ThreadSafeClientConnManager connectionManager;

    private DefaultHttpClient httpclient;

    private MIMEFormater mimeFormater;

    private final int MAX_DOWNLOAD_SIZE = Config.getIntProperty("fetcher.max_download_size", 1048576);

    public Fetcher() {
        HttpParams params = new BasicHttpParams();
        HttpProtocolParamBean paramsBean = new HttpProtocolParamBean(params);
        paramsBean.setVersion(HttpVersion.HTTP_1_1);
        paramsBean.setContentCharset("UTF-8");
        paramsBean.setUseExpectContinue(true);
        params.setParameter("http.useragent", Config.getStringProperty("fetcher.user_agent", "mycrawler (http://www.las.ac.cn/)"));
        params.setIntParameter("http.socket.timeout", Config.getIntProperty("fetcher.socket_timeout", 20000));
        params.setIntParameter("http.connection.timeout", Config.getIntProperty("fetcher.connection_timeout", 30000));
        params.setBooleanParameter("http.protocol.handle-redirects", Config.getBooleanProperty("fetcher.follow_redirects", true));
        params.setBooleanParameter("http.protocol.allow-circular-redirects", Config.getBooleanProperty("fetcher.allow-circular_redirects", true));
        ConnPerRouteBean connPerRouteBean = new ConnPerRouteBean();
        connPerRouteBean.setDefaultMaxPerRoute(Config.getIntProperty("fetcher.max_connections_per_host", 100));
        ConnManagerParams.setMaxConnectionsPerRoute(params, connPerRouteBean);
        ConnManagerParams.setMaxTotalConnections(params, Config.getIntProperty("fetcher.max_total_connections", 100));
        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        if (Config.getBooleanProperty("fetcher.crawl_https", false)) {
            schemeRegistry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));
        }
        connectionManager = new ThreadSafeClientConnManager(params, schemeRegistry);
        logger.setLevel(Level.INFO);
        httpclient = new DefaultHttpClient(connectionManager, params);
        DefaultHttpRequestRetryHandler handler = new DefaultHttpRequestRetryHandler(3, true);
        httpclient.setHttpRequestRetryHandler(handler);
        mimeFormater = new MIMEFormater();
    }

    public int fetch(URLEntity urlEntity, PageEntity page) {
        String toFetchURL = urlEntity.getUrl();
        toFetchURL.replaceAll(" ", "%20");
        HttpGet get = new HttpGet(toFetchURL);
        HttpEntity entity = null;
        try {
            HttpResponse response = httpclient.execute(get);
            entity = response.getEntity();
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                if (response.getStatusLine().getStatusCode() != HttpStatus.SC_NOT_FOUND) {
                    logger.info("Failed: " + response.getStatusLine().toString() + ", while fetching " + toFetchURL);
                }
                return response.getStatusLine().getStatusCode();
            }
            String url = get.getURI().toString();
            if (entity != null) {
                long size = getContentSize(response);
                if (size > MAX_DOWNLOAD_SIZE) {
                    entity.consumeContent();
                    return Fetcher.PageTooBig;
                }
                String encode = getContentEncode(response);
                String type = getContentType(urlEntity, response);
                if (type.indexOf(';') >= 0) {
                    String[] str = type.split(";");
                    type = str[0];
                    if (encode == null && str.length > 1) {
                        encode = str[1].trim().replaceAll("charset=", "");
                    }
                }
                final byte[] content = downloadContent(response);
                final String md5 = Md5.getDigest("mycrawler", content);
                final String format = mimeFormater.JudgeFormat(url, type);
                if (content != null) {
                    size = content.length;
                    page.setUrl(url);
                    page.setContent(content);
                    page.setSize(size);
                    page.setEncode(encode);
                    page.setType(type);
                    page.setDownloadDate(new Date());
                    page.setAnchorText(urlEntity.getAnchor_text());
                    page.setDigest(md5);
                    page.setFormat(format);
                    if (urlEntity.getTitle() != null) {
                        page.setTitle(urlEntity.getTitle());
                    }
                    if (urlEntity.getDiscription() != null) {
                        page.setDiscription(urlEntity.getDiscription());
                    }
                    if (urlEntity.getPublishData() != null) {
                        page.setPublishData(urlEntity.getPublishData());
                    }
                    return Fetcher.OK;
                } else {
                    return Fetcher.PageLoadError;
                }
            } else {
                get.abort();
            }
        } catch (Exception e) {
            logger.error(e.getMessage() + " while fetching " + toFetchURL);
        } finally {
            try {
                if (entity != null) {
                    entity.consumeContent();
                }
                if (get != null) {
                    get.abort();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return UnknownError;
    }

    private byte[] downloadContent(HttpResponse response) {
        try {
            InputStream is = response.getEntity().getContent();
            String contentType = getContentEncode(response);
            if (contentType != null && contentType.indexOf("gzip") >= 0) {
                is = new GZIPInputStream(is);
            }
            ByteArrayOutputStream bytestream = new ByteArrayOutputStream();
            int b = 0;
            while ((b = is.read()) != -1) {
                bytestream.write(b);
            }
            byte[] content = bytestream.toByteArray();
            bytestream.close();
            return content;
        } catch (Exception e) {
            return null;
        }
    }

    private long getContentSize(HttpResponse response) {
        long size = response.getEntity().getContentLength();
        if (size == -1) {
            Header length = response.getLastHeader("Content-Length");
            if (length == null) {
                length = response.getLastHeader("Content-length");
            }
            if (length != null) {
                size = Integer.parseInt(length.getValue());
            } else {
                size = -1;
            }
        }
        return size;
    }

    private String getContentEncode(HttpResponse response) {
        String encode = null;
        Header contentEncode = response.getEntity().getContentEncoding();
        if (contentEncode != null) {
            encode = contentEncode.getValue();
        }
        return encode;
    }

    private String getContentType(URLEntity urlEntity, HttpResponse response) {
        Header contentType = response.getEntity().getContentType();
        if (contentType != null) {
            return contentType.getValue();
        } else {
            return null;
        }
    }

    public void setProxy(String proxyHost, int proxyPort) {
        HttpHost proxy = new HttpHost(proxyHost, proxyPort);
        httpclient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
    }

    public void setProxy(String proxyHost, int proxyPort, String username, String password) {
        httpclient.getCredentialsProvider().setCredentials(new AuthScope(proxyHost, proxyPort), new UsernamePasswordCredentials(username, password));
        setProxy(proxyHost, proxyPort);
    }

    public static final int OK = 1000;

    public static final int PageTooBig = 1001;

    public static final int UnknownDocument = 1002;

    public static final int URLnotAllowedByURLFilters = 1003;

    public static final int FatalProtocolViolation = 1004;

    public static final int FatalTransportError = 1005;

    public static final int UnknownError = 1006;

    public static final int PageLoadError = 1007;

    public static final int RequestForTermination = 1008;

    public static final int RedirectedPageIsSeen = 1010;

    public static boolean is2XXSuccess(int code) {
        return code >= 200 && code < 300;
    }

    public static String statusCodesToString(int code) {
        switch(code) {
            case 100:
                return "HTTP-100-Info-Continue";
            case 101:
                return "HTTP-101-Info-Switching Protocols";
            case 200:
                return "HTTP-200-Success-OK";
            case 201:
                return "HTTP-201-Success-Created";
            case 202:
                return "HTTP-202-Success-Accepted";
            case 203:
                return "HTTP-203-Success-Non-Authoritative";
            case 204:
                return "HTTP-204-Success-No Content ";
            case 205:
                return "HTTP-205-Success-Reset Content";
            case 206:
                return "HTTP-206-Success-Partial Content";
            case 300:
                return "HTTP-300-Redirect-Multiple Choices";
            case 301:
                return "HTTP-301-Redirect-Moved Permanently";
            case 302:
                return "HTTP-302-Redirect-Found";
            case 303:
                return "HTTP-303-Redirect-See Other";
            case 304:
                return "HTTP-304-Redirect-Not Modified";
            case 305:
                return "HTTP-305-Redirect-Use Proxy";
            case 307:
                return "HTTP-307-Redirect-Temporary Redirect";
            case 400:
                return "HTTP-400-ClientErr-Bad Request";
            case 401:
                return "HTTP-401-ClientErr-Unauthorized";
            case 402:
                return "HTTP-402-ClientErr-Payment Required";
            case 403:
                return "HTTP-403-ClientErr-Forbidden";
            case 404:
                return "HTTP-404-ClientErr-Not Found";
            case 405:
                return "HTTP-405-ClientErr-Method Not Allowed";
            case 407:
                return "HTTP-406-ClientErr-Not Acceptable";
            case 408:
                return "HTTP-407-ClientErr-Proxy Authentication Required";
            case 409:
                return "HTTP-408-ClientErr-Request Timeout";
            case 410:
                return "HTTP-409-ClientErr-Conflict";
            case 406:
                return "HTTP-410-ClientErr-Gone";
            case 411:
                return "HTTP-411-ClientErr-Length Required";
            case 412:
                return "HTTP-412-ClientErr-Precondition Failed";
            case 413:
                return "HTTP-413-ClientErr-Request Entity Too Large";
            case 414:
                return "HTTP-414-ClientErr-Request-URI Too Long";
            case 415:
                return "HTTP-415-ClientErr-Unsupported Media Type";
            case 416:
                return "HTTP-416-ClientErr-Requested Range Not Satisfiable";
            case 417:
                return "HTTP-417-ClientErr-Expectation Failed";
            case 500:
                return "HTTP-500-ServerErr-Internal Server Error";
            case 501:
                return "HTTP-501-ServerErr-Not Implemented";
            case 502:
                return "HTTP-502-ServerErr-Bad Gateway";
            case 503:
                return "HTTP-503-ServerErr-Service Unavailable";
            case 504:
                return "HTTP-504-ServerErr-Gateway Timeout";
            case 505:
                return "HTTP-505-ServerErr-HTTP Version Not Supported";
            case OK:
                return "OK";
            case PageTooBig:
                return "Page too big";
            case UnknownDocument:
                return "Unknown Document Type";
            case URLnotAllowedByURLFilters:
                return "URL not allowed by filters";
            case FatalProtocolViolation:
                return "Fatal Protocol Violation";
            case FatalTransportError:
                return "Fatal Transport Error";
            case RequestForTermination:
                return "Request for Termination";
            case UnknownError:
                return "Unknown Error";
            default:
                return Integer.toString(code);
        }
    }

    public HttpResponse manualFollowRedirect(HttpResponse response, HttpGet get) {
        Header location_header = response.getLastHeader("Location");
        if (response.getStatusLine().getStatusCode() == 302 && location_header != null) {
            System.out.println();
            System.out.println(response.getStatusLine());
            for (Header header : response.getAllHeaders()) {
                System.out.println(header.getName() + '=' + header.getValue());
            }
            URL redirectURL = URLCanonicalizer.getCanonicalURL(location_header.getValue(), get.getURI().toString());
            logger.info("Redirect to: " + redirectURL);
            try {
                get.setURI(redirectURL.toURI());
                response = httpclient.execute(get);
            } catch (Exception e) {
                logger.error("Error in Redirect!");
            }
            response = manualFollowRedirect(response, get);
        }
        return response;
    }

    public static void main(String[] args) {
        Fetcher fetcher = new Fetcher();
        URLEntity urlEntity = new URLEntity();
        String url = "https://www.llnl.gov/news/newsreleases/";
        urlEntity.setUrl(url);
        PageEntity page = new PageEntity();
        int status_code = fetcher.fetch(urlEntity, page);
        if (status_code == Fetcher.OK) {
            page.print();
        } else {
            System.out.println(Fetcher.statusCodesToString(status_code));
        }
    }
}
