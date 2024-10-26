package org.jgetfile.util;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import javolution.util.FastList;
import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jgetfile.crawler.link.LinkManager;
import org.jgetfile.crawler.util.JGetFileUtils;

/**
 * 
 * @author Samuel Mendenhall
 */
public final class NetworkUtils {

    private static Logger logger = Logger.getLogger(NetworkUtils.class.getName());

    private static LinkManager linkManager = LinkManager.getInstance();

    private static HttpClient client = new HttpClient();

    private static GetMethod method = null;

    private static DefaultHttpMethodRetryHandler defaultHandler = new DefaultHttpMethodRetryHandler(1, false);

    private static DefaultHttpMethodRetryHandler zeroRetry = new DefaultHttpMethodRetryHandler(0, false);

    public static String getHtml(String address) {
        String html = null;
        method = new GetMethod(address);
        method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, defaultHandler);
        try {
            int statusCode = client.executeMethod(method);
            if (statusCode != HttpStatus.SC_OK) {
                logger.error("Method failed: " + method.getStatusLine());
            }
            html = method.getResponseBodyAsString();
        } catch (Exception e) {
            logger.error("Last Address Tried: " + address);
            logger.error("Fatal protocol violation: " + e.getMessage());
        } finally {
            method.releaseConnection();
        }
        return html;
    }

    public static boolean siteExists(String address) {
        boolean exists = true;
        method = new GetMethod(address);
        method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, zeroRetry);
        try {
            int statusCode = client.executeMethod(method);
            if (statusCode != HttpStatus.SC_OK) {
                exists = false;
                method.abort();
                return exists;
            }
            method.abort();
        } catch (Exception e) {
            e.printStackTrace();
            exists = false;
        } finally {
            method.releaseConnection();
        }
        return exists;
    }

    @SuppressWarnings("static-access")
    public static boolean isValid(String address, String... mimeTypes) {
        if (mimeTypes.length == 0) {
            mimeTypes = linkManager.getMimeTypes();
        }
        boolean isValid = true;
        method = new GetMethod(address);
        method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, defaultHandler);
        try {
            int statusCode = client.executeMethod(method);
            logger.debug("Status Code of: " + address + " = " + statusCode);
            if (statusCode != HttpStatus.SC_OK) {
                isValid = false;
                method.abort();
                return isValid;
            }
            String contentType = method.getResponseHeader("Content-Type").getValue();
            boolean containsMimeType = false;
            for (String mimeT : mimeTypes) {
                if (JGetFileUtils.containsIgnoreCase(contentType, mimeT)) {
                    containsMimeType = true;
                }
            }
            if (!(mimeTypes.length == 0) && !containsMimeType) {
                isValid = false;
            }
            method.abort();
        } catch (Exception e) {
            logger.error("Last Address Tried: " + address);
            logger.error("Error Message: " + e.getMessage());
        } finally {
            method.releaseConnection();
        }
        return isValid;
    }

    public static boolean isLinkHtmlContent(String address) {
        boolean isHtml = false;
        URLConnection conn = null;
        try {
            if (!address.startsWith("http://")) {
                address = "http://" + address;
            }
            URL url = new URL(address);
            conn = url.openConnection();
            if (conn.getContentType().equals("text/html") && !conn.getHeaderField(0).contains("404")) {
                isHtml = true;
            }
        } catch (Exception e) {
            logger.error("Address attempted: " + conn.getURL());
            logger.error("Error Message: " + e.getMessage());
        }
        return isHtml;
    }

    public static FastList<String> getHrefs(String html, String... extensions) {
        FastList<String> hrefs = new FastList<String>();
        if (html == null) {
            html = "";
        }
        StringTokenizer st = new StringTokenizer(html, "\n");
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            if (!JGetFileUtils.containsIgnoreCase(token, "href")) {
                continue;
            }
            FastList<String> hrefsInToken = parseHrefs(token);
            for (FastList.Node<String> n = hrefsInToken.head(), end = hrefsInToken.tail(); (n = n.getNext()) != end; ) {
                String value = n.getValue();
                if (!value.equals("")) {
                    for (String e : extensions) {
                        if (value.endsWith(e)) {
                            hrefs.add(value);
                        }
                    }
                }
            }
        }
        return hrefs;
    }

    private static FastList<String> parseHrefs(String text) {
        FastList<String> hrefs = new FastList<String>();
        while (JGetFileUtils.containsIgnoreCase(text, "href")) {
            StringBuilder leftOvers = new StringBuilder();
            int firstHrefIndex = StringUtils.indexOf(text, "href");
            if (firstHrefIndex == -1) {
                firstHrefIndex = StringUtils.indexOf(text, "HREF");
            }
            leftOvers.append(StringUtils.substring(text, 0, firstHrefIndex));
            text = StringUtils.substring(text, firstHrefIndex, text.length());
            int firstQuoteAfterHref = StringUtils.indexOf(text, "\"");
            text = StringUtils.substring(text, firstQuoteAfterHref + 1, text.length());
            int nextQuote = StringUtils.indexOf(text, "\"");
            String href = StringUtils.substring(text, 0, nextQuote);
            hrefs.add(href);
            text = StringUtils.substring(text, nextQuote + 1, text.length());
        }
        return hrefs;
    }

    public static FastList<String> getRoots(String address) {
        FastList<String> roots = new FastList<String>();
        if (!StringUtils.contains(address, "http://")) {
            return roots;
        }
        address = StringUtils.substringAfterLast(address, "http://");
        address = StringUtils.substringBefore(address, "?");
        if (address.endsWith("/")) {
            address = StringUtils.removeEnd(address, "/");
        }
        roots.add("http://" + address);
        while (StringUtils.contains(address, "/")) {
            address = StringUtils.substringBeforeLast(address, "/");
            roots.add("http://" + address);
        }
        return roots;
    }

    public static void printResponseHeaders(String address) {
        logger.info("Address: " + address);
        try {
            URL url = new URL(address);
            URLConnection conn = url.openConnection();
            for (int i = 0; ; i++) {
                String headerName = conn.getHeaderFieldKey(i);
                String headerValue = conn.getHeaderField(i);
                if (headerName == null && headerValue == null) {
                    break;
                }
                if (headerName == null) {
                    logger.info(headerValue);
                    continue;
                }
                logger.info(headerName + " " + headerValue);
            }
        } catch (Exception e) {
            logger.error("Exception Message: " + e.getMessage());
        }
    }

    public static Map<String, List<String>> getResponseHeader(String address) {
        System.out.println(address);
        URLConnection conn = null;
        Map<String, List<String>> responseHeader = null;
        try {
            URL url = new URL(address);
            conn = url.openConnection();
            responseHeader = conn.getHeaderFields();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return responseHeader;
    }

    public static int getContentLength(String address) {
        URLConnection conn = null;
        int contentLength = 0;
        try {
            URL url = new URL(address);
            conn = url.openConnection();
            contentLength = conn.getContentLength();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return contentLength;
    }

    public static String formatWebAddress(String address) {
        if (address.contains("#")) {
            address = StringUtils.substringAfter(address, "#");
        }
        if (!address.startsWith("http://") && JGetFileUtils.containsIgnoreCase(address, "www")) {
            address = "http://" + address;
        }
        if (address.endsWith("/")) {
            address = address.substring(0, address.length() - 1);
        }
        return address;
    }

    public static String getRealLocation(String address, int depthToLocate) {
        String realAddress = address;
        if (depthToLocate == 0) {
            return realAddress;
        }
        try {
            URL url = new URL(address);
            URLConnection conn = url.openConnection();
            String location = conn.getHeaderField("Location");
            if (location == null) {
                return realAddress;
            } else {
                return getRealLocation(location, depthToLocate - 1);
            }
        } catch (MalformedURLException e) {
            logger.error(e.getMessage());
            logger.debug(e.getStackTrace());
        } catch (IOException e) {
            logger.error(e.getMessage());
            logger.debug(e.getStackTrace());
        }
        return realAddress;
    }
}
