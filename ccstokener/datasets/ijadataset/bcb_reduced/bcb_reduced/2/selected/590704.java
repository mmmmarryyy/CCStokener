package org.progeeks.mapview.wms;

import java.io.*;
import java.net.*;
import java.util.*;
import org.progeeks.util.*;
import org.progeeks.util.log.*;

/**
 *
 *  @version   $Revision: 4149 $
 *  @author    Paul Speed
 */
public class WmsService {

    public static final String TYPE_SERVICE_EXCEPTION = "application/vnd.ogc.se_xml";

    static Log log = Log.getLog();

    private String baseUrl;

    private String separator = "";

    private WmsCapabilities caps;

    /**
     *  Creates a new WMS service for the specified base URL.  The URL
     *  should include the SERVICE=WMS if it is required in the URL.
     */
    public WmsService(String baseUrl) {
        this.baseUrl = baseUrl;
        if (!baseUrl.endsWith("&") && !baseUrl.endsWith("?")) separator = "&";
    }

    /**
     *  Retrieves and caches the server capabilities for
     *  the configured base URL.
     */
    public WmsCapabilities initialize() throws IOException {
        GetCapsRequest req = new GetCapsRequest();
        caps = doRequest(req);
        return caps;
    }

    protected URL makeUrl(WmsRequest<?> req) throws IOException {
        String u = baseUrl + separator + req.getRequest();
        return new URL(u);
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public WmsCapabilities getCapabilities() {
        return caps;
    }

    public <T> T doRequest(WmsRequest<T> req) throws IOException {
        URL url = makeUrl(req);
        if (log.isDebugEnabled()) log.debug("doRequest(" + url + ")");
        System.out.println("doRequest(" + url + ")");
        URLConnection uConn = url.openConnection();
        System.out.println("Default read timeout:" + uConn.getReadTimeout() + "  Connect timeout:" + uConn.getConnectTimeout());
        uConn.setReadTimeout(30000);
        uConn.setConnectTimeout(30000);
        uConn.connect();
        InputStream in = uConn.getInputStream();
        try {
            Map<String, List<String>> header = uConn.getHeaderFields();
            if (log.isDebugEnabled()) log.debug("Header:" + header);
            String contentType = uConn.getContentType();
            if (contentType == null) {
                log.warn("Could not retrieve content type from response for:" + url);
                log.warn("Headers:" + header);
                String test = URLConnection.guessContentTypeFromStream(in);
                log.warn("Content guess:" + test);
            }
            String charset = "UTF-8";
            int split = contentType == null ? -1 : contentType.indexOf(";");
            if (split > 0) {
                String extra = contentType.substring(split + 1);
                contentType = contentType.substring(0, split);
                if (extra.startsWith("charset=")) charset = extra.substring("charset=".length());
            }
            if (log.isDebugEnabled()) log.debug("Content type:" + contentType);
            if (TYPE_SERVICE_EXCEPTION.equals(contentType)) {
                String error = StringUtils.readString(new InputStreamReader(in, charset));
                throw new RuntimeException(error);
            }
            T response = req.readResponse(in);
            return response;
        } finally {
            in.close();
        }
    }

    public String toString() {
        return getClass().getSimpleName() + "[ baseUrl=" + baseUrl + ", caps=" + caps + "]";
    }

    public static void main(String[] args) throws IOException {
        WmsService wms = new WmsService(args[0]);
        WmsCapabilities wmsCaps = wms.initialize();
        System.out.println(wmsCaps);
        System.out.println("Layers:");
        WmsLayerInfo first = null;
        for (WmsLayerInfo l : wmsCaps.getLayers()) {
            if (first == null && l.getName() != null && l.getName().indexOf(' ') < 0) first = l;
            System.out.println(l);
        }
        System.out.println("Trying a map request...");
        System.out.println("Trying to grab layer [" + first.getName() + "]");
        GetMapImageRequest req = new GetMapImageRequest(first.getName());
        req.setBounds(new org.progeeks.mapview.GeoRectangle(-180, -90, 180, 180));
        req.setSize(360, 180);
        Object result = wms.doRequest(req);
        System.out.println("Result:" + result);
    }
}
