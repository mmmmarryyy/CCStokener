package migool.util;

import static migool.util.StringUtil.cutPrefix;
import static migool.util.StringUtil.isNotNullOrEmpty;
import static migool.util.StringUtil.isNullOrEmpty;
import java.net.URL;

/**
 * 
 * @author Denis Migol
 * 
 */
public final class LinkUtil {

    /**
	 * Don't let anyone instantiate this class.
	 */
    private LinkUtil() {
    }

    public static final String SLASH = "/";

    public static final String HTTP_PREFIX = "http://";

    public static final String HTTPS_PREFIX = "https://";

    public static final String FTP_PREFIX = "ftp://";

    public static final String WWW_PREFIX = "www.";

    /**
	 * 
	 * @param link
	 * @return
	 */
    public static String cutLinkToHost(final String link) {
        if (isNullOrEmpty(link)) return link;
        String result = link;
        if (link.startsWith(HTTP_PREFIX) || link.startsWith(HTTPS_PREFIX) || link.startsWith(FTP_PREFIX)) {
            if (link.startsWith(HTTP_PREFIX)) {
                result = link.substring(HTTP_PREFIX.length());
            } else if (link.startsWith(HTTPS_PREFIX)) {
                result = link.substring(HTTPS_PREFIX.length());
            } else {
                result = link.substring(FTP_PREFIX.length());
            }
        }
        if (result.indexOf("/") > -1) {
            result = result.substring(0, result.indexOf("/"));
        }
        if (result.startsWith(WWW_PREFIX)) {
            result = result.substring(WWW_PREFIX.length());
        }
        return result;
    }

    /**
	 * 
	 * @param link
	 * @return
	 */
    public static String cutLinkToPath(final String link) {
        if (isNullOrEmpty(link)) {
            return link;
        }
        final String ret;
        if (link.charAt(0) == '/') {
            ret = link;
        } else {
            final String host = cutLinkToHost(link);
            ret = link.substring(link.indexOf(host) + host.length());
        }
        return ret;
    }

    /**
	 * 
	 * @param link
	 * @return
	 */
    public static String getHost(final String link) {
        String host = cutLinkToHost(link);
        if (isNotNullOrEmpty(host)) {
            host = host.toLowerCase();
            while (host.startsWith(WWW_PREFIX)) {
                host = host.substring(WWW_PREFIX.length());
            }
            if (host.endsWith(".")) {
                host = host.substring(0, host.length() - 1);
            }
            if (host.indexOf(".") == 1) {
                return "";
            }
            try {
                final URL url = new URL(HTTP_PREFIX + host);
                url.openConnection();
                return url.getHost();
            } catch (final Exception e) {
                return "";
            }
        }
        return host;
    }

    /**
	 * 
	 * @param link
	 * @return
	 */
    public static String createHttpRoot(final String link) {
        final String host = cutLinkToHost(link);
        if (isNotNullOrEmpty(host)) {
            return HTTP_PREFIX + host;
        } else {
            return null;
        }
    }

    /**
	 * 
	 * @param link
	 * @param path
	 * @return
	 */
    public static String createHttpLink(final String link, final String path) {
        final String httpRoot = createHttpRoot(link);
        final String correctPath = cutPrefix(cutLinkToPath(path), SLASH);
        if (isNotNullOrEmpty(httpRoot) && isNotNullOrEmpty(correctPath)) {
            return httpRoot + SLASH + correctPath;
        } else {
            return null;
        }
    }

    /**
	 * 
	 * @param link
	 * @return
	 */
    public static boolean isRootLink(final String link) {
        if (isNullOrEmpty(link) || !RegexUtil.isUrl(link)) {
            return false;
        }
        final String host = getHost(link);
        return isNotNullOrEmpty(host) && (link.endsWith(host) || link.endsWith(host + "/"));
    }

    public static String getFileName(final String url) {
        if (isNullOrEmpty(url)) {
            return url;
        }
        String result = url;
        int lastIndex = 0;
        if (result.contains("/")) {
            lastIndex = result.lastIndexOf("/");
        } else if (result.contains("\\")) {
            lastIndex = result.lastIndexOf("\\");
        }
        result = result.substring(lastIndex + 1);
        if (isNullOrEmpty(result)) {
            return result;
        }
        while (!RegexUtil.isFileNameUrl(result) && isNotNullOrEmpty(result)) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    /**
	 * 
	 * @param url1
	 * @param url2
	 * @return
	 */
    public static boolean equals(final String url1, final String url2) {
        if (isNotNullOrEmpty(url1) && isNotNullOrEmpty(url2)) {
            return cutLinkToHost(url1).equalsIgnoreCase(cutLinkToHost(url2)) && cutLinkToPath(url1).equalsIgnoreCase(cutLinkToPath(url2));
        }
        return false;
    }

    /**
	 * 
	 * @param url
	 * @return
	 */
    public static String cutDomain(final String url) {
        return null;
    }

    /**
	 * 
	 * @param url
	 * @return
	 */
    public static boolean isDomain2(final String url) {
        return false;
    }

    /**
	 * 
	 * @param url
	 * @return
	 */
    public static String cutDomain2(final String url) {
        return null;
    }

    /**
	 * 
	 * @param url1
	 * @param url2
	 * @return
	 */
    public static boolean equalsDomains2(final String url1, final String url2) {
        final String domain1 = cutDomain2(url1);
        final String domain2 = cutDomain2(url2);
        return (isNotNullOrEmpty(domain1) && isNotNullOrEmpty(domain2)) ? domain1.equalsIgnoreCase(domain2) : false;
    }
}
