package openvend.component;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import openvend.cache.OvCacheItem;
import openvend.cache.OvCacheKey;
import openvend.io.OvFileCache;
import openvend.main.I_OvRequestContext;
import openvend.main.OvLog;
import openvend.main.OvXmlModel;
import openvend.portlet.A_OvPortletRequestContext;
import org.apache.commons.collections.map.LRUMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Includes XML files from various sources into the XML model of a request.<p/>
 * 
 * @author Thomas Weckert
 * @version $Revision: 1.7 $
 * @since 1.0
 */
public class OvXmlIncludeComponent extends A_OvComponent {

    private static Log log = OvLog.getLog(OvXmlIncludeComponent.class);

    private static OvFileCache FILE_CACHE = null;

    private static Map HTTP_CACHE = null;

    private boolean fileCacheEnabled;

    private boolean httpCacheEnabled;

    private OvCacheKey cacheKey;

    private String httpCacheDirectives;

    /**
     * @see openvend.component.A_OvComponent#init(java.lang.String, java.util.Map)
     */
    public void init(String id, Map params) throws Exception {
        super.init(id, params);
        int fileMaxCacheSize = getParamAsInt("file-max-cache-size", 64);
        long fileMaxContentSize = getParamAsLong("file-max-content-size", 32000);
        this.fileCacheEnabled = "true".equalsIgnoreCase(getParam("file-cache-enabled", "true"));
        boolean fileCacheLastModifiedCheckEnabled = "true".equalsIgnoreCase(getParam("file-cache-last-modified-check-enabled", "true"));
        int httpMaxCacheSize = getParamAsInt("http-max-cache-size", 64);
        this.httpCacheEnabled = "true".equalsIgnoreCase(getParam("http-cache-enabled", "true"));
        this.httpCacheDirectives = getParam("http-cache-directives", null);
        if (StringUtils.isNotEmpty(this.httpCacheDirectives)) {
            this.cacheKey = new OvCacheKey(this.httpCacheDirectives);
        } else {
            this.cacheKey = new OvCacheKey("never;");
        }
        if (FILE_CACHE == null) {
            synchronized (OvXmlIncludeComponent.class) {
                if (FILE_CACHE == null) {
                    synchronized (OvXmlIncludeComponent.class) {
                        FILE_CACHE = new OvFileCache(fileMaxCacheSize, fileMaxContentSize, fileCacheLastModifiedCheckEnabled);
                    }
                }
            }
        }
        if (HTTP_CACHE == null) {
            synchronized (OvXmlIncludeComponent.class) {
                if (HTTP_CACHE == null) {
                    synchronized (OvXmlIncludeComponent.class) {
                        HTTP_CACHE = new LRUMap(httpMaxCacheSize);
                    }
                }
            }
        }
    }

    /**
     * @see openvend.component.A_OvComponent#handleRequest(I_OvRequestContext)
     */
    public void handleRequest(I_OvRequestContext requestContext) throws Exception {
        if (requestContext.isPortletRequest()) {
            A_OvPortletRequestContext portletRequestContext = (A_OvPortletRequestContext) requestContext;
            if (portletRequestContext.isPortletActionRequest()) {
                return;
            }
        }
        File contextPath = requestContext.getConfig().getContextPath();
        OvXmlModel xmlModel = requestContext.getXmlModel();
        Element xmlIncludeElement = xmlModel.appendElement(getId());
        String paramFile = getParam("file", null);
        String paramUrl = getParam("url", null);
        byte[] content = null;
        String source = null;
        if (StringUtils.isNotEmpty(paramFile) && paramFile.toLowerCase().endsWith(".xml")) {
            source = paramFile;
            content = getFileContent(requestContext, xmlIncludeElement, paramFile);
        } else if (StringUtils.isNotEmpty(paramUrl)) {
            source = paramUrl;
            content = getUrlContent(requestContext, xmlIncludeElement, paramUrl);
        }
        if (content != null) {
            Document includeDocument = requestContext.getXmlLoader().createDocument(source, content, contextPath);
            Document xmlModelDocument = xmlModel.getDocument();
            Node node = xmlModelDocument.importNode(includeDocument.getDocumentElement(), true);
            xmlIncludeElement.appendChild(node);
        }
    }

    protected byte[] getFileContent(I_OvRequestContext requestContext, Element xmlIncludeElement, String filename) throws Exception {
        byte[] content = null;
        xmlIncludeElement.setAttribute("file", filename);
        File file = requestContext.getConfig().resolveFile(filename);
        if (fileCacheEnabled) {
            content = FILE_CACHE.getFile(file);
        } else {
            content = FileUtils.readFileToByteArray(file);
        }
        return content;
    }

    protected byte[] getUrlContent(I_OvRequestContext requestContext, Element xmlIncludeElement, String urlStr) throws Exception {
        byte[] content = null;
        xmlIncludeElement.setAttribute("url", urlStr);
        URL url = new URL(urlStr);
        if (!httpCacheEnabled || cacheKey.cacheNever()) {
            if (log.isDebugEnabled()) {
                log.debug("Caching of URL '" + url.toString() + "' is explicitly denied or HTTP cache is disabled!");
            }
            content = readUrlContent(url);
        } else {
            Map variants = null;
            OvCacheItem cacheItem = null;
            String requestCacheKey = cacheKey.createCacheKey(requestContext);
            if (StringUtils.isEmpty(requestCacheKey)) {
                if (log.isDebugEnabled()) {
                    log.debug("Request for URL '" + url.toString() + "' is not cacheable!");
                }
            } else {
                synchronized (HTTP_CACHE) {
                    URI cacheURI = new URI(url.toString());
                    if ((variants = (Map) HTTP_CACHE.get(cacheURI)) == null || (cacheItem = (OvCacheItem) variants.get(requestCacheKey)) == null) {
                        if (variants == null) {
                            variants = new HashMap();
                            HTTP_CACHE.put(cacheURI, variants);
                        }
                        content = readUrlContent(url);
                        cacheItem = new OvCacheItem(httpCacheDirectives, requestCacheKey, content);
                        if (cacheKey.cacheTimeout()) {
                            cacheItem.setDateExpires(cacheKey.getTimeout());
                        }
                        variants.put(requestCacheKey, cacheItem);
                        if (log.isDebugEnabled()) {
                            log.debug("Added new variant '" + requestCacheKey + "'for URL '" + url.toString() + "' to the HTTP cache!");
                        } else {
                            content = cacheItem.getContent();
                            if (log.isDebugEnabled()) {
                                log.debug("Returning cached variant '" + requestCacheKey + "' for URL '" + url.toString() + "' from the HTTP cache!");
                            }
                        }
                        HTTP_CACHE.notifyAll();
                    }
                }
            }
        }
        return content;
    }

    protected byte[] readUrlContent(URL url) {
        byte[] content = null;
        ByteArrayOutputStream output = null;
        InputStream input = null;
        try {
            URLConnection conn = url.openConnection();
            conn.connect();
            if (conn instanceof HttpURLConnection) {
                HttpURLConnection httpConn = (HttpURLConnection) conn;
                int httpResponseCode = httpConn.getResponseCode();
                String httpResponseMessage = httpConn.getResponseMessage();
                if (httpResponseCode != HttpURLConnection.HTTP_OK) {
                    if (log.isErrorEnabled()) {
                        log.error("Error opening URL connection for '" + url.toString() + "', HTTP status: " + httpResponseCode + ", HTTP response message: " + httpResponseMessage);
                    }
                    return null;
                }
            }
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Error opening URL connection for '" + url.toString() + "'");
            }
            return null;
        }
        try {
            output = new ByteArrayOutputStream();
            input = url.openStream();
            byte[] buf = new byte[512];
            for (int len = 0; (len = input.read(buf)) != -1; output.write(buf, 0, len)) ;
            content = output.toByteArray();
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Error reading contents of URL *" + url.toString() + "'");
            }
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (Throwable t) {
                } finally {
                    output = null;
                }
            }
            if (input != null) {
                try {
                    input.close();
                } catch (Throwable t) {
                } finally {
                    input = null;
                }
            }
        }
        return content;
    }

    protected String getUrlParams(I_OvRequestContext requestContext) {
        return StringUtils.EMPTY;
    }
}
