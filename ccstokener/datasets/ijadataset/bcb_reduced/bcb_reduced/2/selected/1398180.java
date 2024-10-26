package org.xhtmlrenderer.demo.aboutbox;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import org.xhtmlrenderer.extend.UserAgentCallback;
import org.xhtmlrenderer.resource.CSSResource;
import org.xhtmlrenderer.resource.ImageResource;
import org.xhtmlrenderer.resource.XMLResource;
import org.xhtmlrenderer.swing.AWTFSImage;
import org.xhtmlrenderer.util.Uu;
import org.xhtmlrenderer.util.XRLog;

/**
 * Created by IntelliJ IDEA.
 * User: tobe
 * Date: 2005-jun-15
 * Time: 07:38:59
 * To change this template use File | Settings | File Templates.
 */
public class DemoUserAgent implements UserAgentCallback {

    private String baseUrl;

    private int index = -1;

    private ArrayList history = new ArrayList();

    /**
     * an LRU cache
     */
    private int imageCacheCapacity = 16;

    private java.util.LinkedHashMap imageCache = new java.util.LinkedHashMap(imageCacheCapacity, 0.75f, true) {

        protected boolean removeEldestEntry(java.util.Map.Entry eldest) {
            return size() > imageCacheCapacity;
        }
    };

    public CSSResource getCSSResource(String uri) {
        InputStream is = null;
        uri = resolveURI(uri);
        try {
            URLConnection uc = new URL(uri).openConnection();
            uc.connect();
            is = uc.getInputStream();
        } catch (MalformedURLException e) {
            XRLog.exception("bad URL given: " + uri, e);
        } catch (IOException e) {
            XRLog.exception("IO problem for " + uri, e);
        }
        return new CSSResource(is);
    }

    public ImageResource getImageResource(String uri) {
        ImageResource ir = null;
        uri = resolveURI(uri);
        ir = (ImageResource) imageCache.get(uri);
        if (ir == null) {
            InputStream is = null;
            try {
                URLConnection uc = new URL(uri).openConnection();
                uc.connect();
                is = uc.getInputStream();
            } catch (MalformedURLException e1) {
                XRLog.exception("bad URL given: " + uri, e1);
            } catch (IOException e11) {
                XRLog.exception("IO problem for " + uri, e11);
            }
            if (is != null) {
                try {
                    BufferedImage img = ImageIO.read(is);
                    ir = new ImageResource(AWTFSImage.createLegacyImage(img));
                    imageCache.put(uri, ir);
                } catch (IOException e) {
                    XRLog.exception("Can't read image file; unexpected problem for URI '" + uri + "'", e);
                }
            }
        }
        if (ir == null) ir = new ImageResource(null);
        return ir;
    }

    public byte[] getBinaryResource(String uri) {
        InputStream is = null;
        try {
            URL url = new URL(uri);
            URLConnection conn = url.openConnection();
            is = conn.getInputStream();
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buf = new byte[10240];
            int i;
            while ((i = is.read(buf)) != -1) {
                result.write(buf, 0, i);
            }
            is.close();
            is = null;
            return result.toByteArray();
        } catch (IOException e) {
            return null;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public XMLResource getXMLResource(String uri) {
        uri = resolveURI(uri);
        if (uri != null && uri.startsWith("file:")) {
            File file = null;
            try {
                file = new File(new URI(uri));
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }
        XMLResource xr = null;
        InputStream inputStream = null;
        try {
            URLConnection uc = new URL(uri).openConnection();
            uc.connect();
            String contentType = uc.getContentType();
            inputStream = uc.getInputStream();
            xr = XMLResource.load(inputStream);
        } catch (MalformedURLException e) {
            XRLog.exception("bad URL given: " + uri, e);
        } catch (IOException e) {
            XRLog.exception("IO problem for " + uri, e);
        } finally {
            if (inputStream != null) try {
                inputStream.close();
            } catch (IOException e) {
            }
        }
        if (xr == null) {
            String notFound = "<h1>Document not found</h1>";
            xr = XMLResource.load(new StringReader(notFound));
        }
        return xr;
    }

    public boolean isVisited(String uri) {
        if (uri == null) return false;
        uri = resolveURI(uri);
        return history.contains(uri);
    }

    public void setBaseURL(String url) {
        baseUrl = resolveURI(url);
        if (baseUrl == null) baseUrl = "error:FileNotFound";
        if (index >= 0) {
            String historic = (String) history.get(index);
            if (historic.equals(baseUrl)) return;
        }
        index++;
        for (int i = index; i < history.size(); history.remove(i)) ;
        history.add(index, baseUrl);
    }

    public String resolveURI(String uri) {
        URL ref = null;
        if (uri == null) return baseUrl;
        if (uri.trim().equals("")) return baseUrl;
        if (uri.startsWith("demo:")) {
            DemoMarker marker = new DemoMarker();
            String short_url = uri.substring(5);
            if (!short_url.startsWith("/")) {
                short_url = "/" + short_url;
            }
            ref = marker.getClass().getResource(short_url);
            Uu.p("ref = " + ref);
        } else {
            try {
                URL base;
                if (baseUrl == null || baseUrl.length() == 0) {
                    ref = new URL(uri);
                } else {
                    base = new URL(baseUrl);
                    ref = new URL(base, uri);
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
        if (ref == null) return null; else return ref.toExternalForm();
    }

    public String getBaseURL() {
        return baseUrl;
    }

    public String getForward() {
        index++;
        return (String) history.get(index);
    }

    public String getBack() {
        index--;
        return (String) history.get(index);
    }

    public boolean hasForward() {
        if (index + 1 < history.size() && index >= 0) {
            return true;
        } else {
            return false;
        }
    }

    public boolean hasBack() {
        if (index >= 0) {
            return true;
        } else {
            return false;
        }
    }
}
