package xbrowser.renderer.custom;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.text.*;
import java.util.*;
import javax.swing.text.*;
import javax.swing.text.html.*;
import xbrowser.cookie.XCookie;
import xbrowser.renderer.XRendererContext;

public final class XRendererSupport {

    public static void setContext(XRendererContext cntx) {
        context = cntx;
    }

    public static XRendererContext getContext() {
        return context;
    }

    /** This is Workaround for JDK bug on setIfModified(), up to merlin-beta. */
    public static void setIfModified(URLConnection conn, long lastMod) {
        Date date = new Date(lastMod);
        SimpleDateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.ENGLISH);
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        conn.setRequestProperty("If-Modified-Since", df.format(date));
    }

    public static boolean isCacheable(HttpURLConnection c, int response) {
        long expiration = c.getExpiration();
        long now = System.currentTimeMillis();
        boolean ok = (response >= HttpURLConnection.HTTP_OK && response < HttpURLConnection.HTTP_MOVED_PERM) && (c.getHeaderField("Expires") == null || expiration > now) && (!"no-cache".equals(c.getHeaderField("Cache-Control")) && !"no-cache".equals(c.getHeaderField("Pragma")) && c.getLastModified() < now);
        return ok;
    }

    /** Set the cookies */
    public static void setCookies(URL page, URLConnection conn) {
        Iterator cookies = getContext().getCookieManager().getApplicableCookies(page);
        String sCookie = null;
        while (cookies.hasNext()) {
            XCookie cookie = (XCookie) cookies.next();
            if (sCookie == null) sCookie += cookie.toSendString(); else sCookie += "; " + cookie.toSendString();
            getContext().getLogger().message(_this, "Set cookie: " + cookie);
        }
        if (sCookie != null) conn.setRequestProperty("Cookie", sCookie);
    }

    /** Storing cookies from server */
    public static void getCookies(URL page, URLConnection conn) {
        int i = 0;
        do {
            String fieldName = conn.getHeaderFieldKey(i);
            String cookie_field = conn.getHeaderField(i);
            if (fieldName == null && cookie_field == null) break;
            if (fieldName != null && fieldName.equalsIgnoreCase("Set-Cookie")) {
                if (cookie_field != null) {
                    getContext().getLogger().message(_this, "Got cookie: " + cookie_field);
                    getContext().getCookieManager().setCookies(page, cookie_field);
                }
            }
            i++;
        } while (true);
    }

    /** When saving, images/applets can be saved along.
	 *  @param doc The HTMLDocument which will be changed to refer to local parts.
	 *  @param dir Directory to save all images relative to.
	 *  @param mode Defines how to map images src url to file name.
	 */
    public static void makeDocLocal(HTMLDocument doc, String dir, int mode) throws IOException {
        ElementIterator i = new ElementIterator(doc);
        Element el;
        SavingCache cache = new SavingCache(doc);
        while ((el = i.next()) != null) {
            HTML.Tag eltag = (HTML.Tag) el.getAttributes().getAttribute(StyleConstants.NameAttribute);
            if (HTML.Tag.IMG.equals(eltag)) {
                makeImgLocal(doc, dir, cache, el);
            }
        }
    }

    private static void makeImgLocal(HTMLDocument doc, String dir, SavingCache cache, Element el) throws IOException {
        String src = (String) el.getAttributes().getAttribute(HTML.Attribute.SRC);
        if (src == null) {
            getContext().getLogger().warning(_this, "SRC not found!");
            return;
        }
        long saveElapsed = 0;
        String newsrc = cache.get(src);
        if (newsrc == null) {
            File imageDir = new File(dir);
            newsrc = getLocalImagePath(src, imageDir);
            boolean saved = false;
            if (!src.toLowerCase().startsWith("file:")) {
                try {
                    URL imgURL = new URL(doc.getBase(), src);
                    String url = imgURL.toString();
                    if (getContext().getCacheManager().isCached(url)) {
                        File cacheFile = getContext().getCacheManager().getFileToCache(url);
                        long now = System.currentTimeMillis();
                        copyFile(cacheFile, newsrc);
                        long elapsed = System.currentTimeMillis() - now;
                        saveElapsed += elapsed;
                        saved = true;
                    } else {
                        getContext().getLogger().error(_this, "Image is not cached???");
                    }
                } catch (MalformedURLException mfe) {
                    getContext().getLogger().error(_this, "Not saved because of Malformed: " + src);
                }
            }
            newsrc = newsrc.replace('\\', '/');
            if (!saved) saveComponentFile(doc.getBase(), src, newsrc);
            newsrc = "file:///" + newsrc;
            cache.put(src, newsrc);
        } else {
        }
        String newimg = changeImgSrc(el, newsrc);
        try {
            setOuterHTML(doc, el, newimg);
        } catch (BadLocationException ex) {
            getContext().getLogger().error(_this, ex);
        }
    }

    public static String changeImgSrc(Element img, String newsrc) {
        String result = "<IMG ";
        AttributeSet as = img.getAttributes();
        Enumeration attr = as.getAttributeNames();
        while (attr.hasMoreElements()) {
            Object cur = attr.nextElement();
            Object value;
            if (cur.equals(StyleConstants.NameAttribute)) continue;
            if (!cur.equals(HTML.Attribute.SRC)) {
                value = as.getAttribute(cur);
            } else value = newsrc;
            result += cur + "=\"" + value + "\" ";
        }
        result += ">";
        return result;
    }

    /** Strategies to map Inet file name to local file name.
	 *  Currently local file name is "<UNIQ_NUM>.<ext from src>".
	 *  Requires that src is not <code>null</code>.
	 */
    private static String getLocalImagePath(String src, File dir) {
        int last_dot = src.lastIndexOf('.');
        String ext = last_dot > 0 ? src.substring(last_dot) : "";
        if (ext.indexOf('?') > 0) ext = ".gif";
        File local_file = null;
        try {
            local_file = File.createTempFile("XBL", ext, dir);
        } catch (IOException ex) {
            getContext().getLogger().error(_this, "Attempt failed: " + src);
            getContext().getLogger().error(_this, ex);
            local_file = new File(dir, "XB_Bad." + ext);
        }
        return local_file.getAbsolutePath();
    }

    private static void copyFile(File from, String to) throws IOException {
        FileInputStream fis = new FileInputStream(from);
        FileOutputStream fos = new FileOutputStream(to);
        copyStreams(fis, fos, 0);
    }

    /** @deprecated Because does not use cache facilities. */
    private static void saveComponentFile(URL base_url, String link_src, String new_src) throws MalformedURLException, IOException {
        getContext().getLogger().message(_this, "BAD: saveComponent()! " + link_src);
        URL itemUrl = new URL(base_url, link_src);
        URLConnection conn = itemUrl.openConnection();
        InputStream is = conn.getInputStream();
        FileOutputStream fos = new FileOutputStream(new_src);
        copyStreams(is, fos, 0);
    }

    /** Utility method to copy whole content of streams. */
    public static void copyStreams(InputStream is, OutputStream fos, int size) throws IOException {
        int thesize = size > 0 ? size : 4096;
        byte[] buffer = new byte[thesize];
        try {
            int actual, total = 0;
            do {
                actual = is.read(buffer, 0, thesize);
                if (actual <= 0) break;
                total += actual;
                fos.write(buffer, 0, actual);
            } while (true);
        } finally {
            fos.close();
            is.close();
        }
    }

    public static void setOuterHTML(HTMLDocument doc, Element elem, String html_text) throws BadLocationException, IOException {
        try {
            invokeMethod(doc, "setOuterHTML", new Object[] { elem, html_text });
        } catch (BadLocationException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            getContext().getLogger().error(_this, e);
        }
    }

    public static void insertAfterEnd(HTMLDocument doc, Element elem, String html_text) throws BadLocationException, IOException {
        try {
            invokeMethod(doc, "insertAfterEnd", new Object[] { elem, html_text });
        } catch (BadLocationException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            getContext().getLogger().error(_this, e);
        }
    }

    public static void addStyleSheet(StyleSheet ss, StyleSheet ss_param) {
        try {
            invokeMethod(ss, "addStyleSheet", new Object[] { ss_param });
        } catch (Exception e) {
            getContext().getLogger().error(_this, e);
        }
    }

    public static void setParser(HTMLDocument doc, HTMLEditorKit.Parser parser) {
        try {
            invokeMethod(doc, "setParser", new Class[] { HTMLEditorKit.Parser.class }, new Object[] { parser });
        } catch (Exception e) {
            getContext().getLogger().error(_this, e);
        }
    }

    private static Object invokeMethod(Object method_owner, String method_name, Object[] method_params) throws Exception {
        Class[] params_classes = new Class[method_params.length];
        for (int i = 0; i < method_params.length; i++) params_classes[i] = method_params[i].getClass();
        return invokeMethod(method_owner, method_name, params_classes, method_params);
    }

    private static Object invokeMethod(Object method_owner, String method_name, Class[] param_types, Object[] method_params) throws Exception {
        Method method;
        try {
            method = method_owner.getClass().getMethod(method_name, param_types);
            return method.invoke(method_owner, method_params);
        } catch (NoSuchMethodException e) {
            getContext().getLogger().warning(_this, "'" + method_name + "' method not found in '" + method_owner.getClass().getName() + "' to invoke!!");
            getContext().getLogger().error(_this, e);
        } catch (SecurityException e) {
            getContext().getLogger().error(_this, e);
        } catch (IllegalAccessException e) {
            getContext().getLogger().error(_this, e);
        } catch (IllegalArgumentException e) {
            getContext().getLogger().error(_this, e);
        } catch (InvocationTargetException e) {
            getContext().getLogger().error(_this, e);
        }
        return null;
    }

    static class SavingCache {

        java.util.Map cache;

        Document document;

        public SavingCache(Document doc) {
            document = doc;
            Object objCache = doc.getProperty(SAVE_CACHE);
            if ((objCache == null) || !(objCache instanceof java.util.Map)) {
                cache = new HashMap();
                doc.putProperty(SAVE_CACHE, cache);
            } else {
                cache = (java.util.Map) objCache;
            }
        }

        public String get(String key) {
            return (String) cache.get(key);
        }

        public void put(String key, String value) {
            cache.put(key, value);
        }
    }

    public static class SavingThread extends Thread {

        public SavingThread(URL url, File cacheFile, List owner) {
            setName("Saving image " + url);
            setPriority(Thread.MIN_PRIORITY);
            this.url = url;
            this.cacheFile = cacheFile;
            this.owner = owner;
        }

        public void run() {
            boolean ok = true;
            try {
                getContext().getLogger().message(this, "Started saving: " + url + " to " + cacheFile.getName());
                URLConnection conn = url.openConnection();
                InputStream is = conn.getInputStream();
                String contentType = conn.getContentType();
                if (contentType != null) getContext().getCacheManager().setContentType(url.toString(), contentType);
                FileOutputStream fos = new FileOutputStream(cacheFile);
                copyStreams(is, fos, 0);
            } catch (IOException ioe) {
                getContext().getLogger().error(this, ioe);
                getContext().getCacheManager().removeFromCache(url.toString());
                ok = false;
            }
            if (owner != null) owner.remove(this);
            getContext().getLogger().message(this, "Saving: " + url + (ok ? " complete!" : " aborted."));
        }

        URL url;

        File cacheFile;

        List owner;
    }

    public static class XInputSave {

        public XInputSave(File file) {
            this.file = file;
            isFileBased = true;
        }

        public XInputSave() {
            bos = new ByteArrayOutputStream();
            isFileBased = false;
        }

        public InputStream getSavingStream(InputStream is) {
            if (isFileBased) return is;
            XTeeInputStream newer = new XTeeInputStream(is);
            newer.setFileOutputStream(bos);
            return newer;
        }

        public InputStream getSavedStream() throws FileNotFoundException {
            if (isFileBased) return new FileInputStream(file); else return new ByteArrayInputStream(bos.toByteArray());
        }

        private final boolean isFileBased;

        private File file = null;

        private ByteArrayOutputStream bos = null;
    }

    private static XRendererSupport _this = new XRendererSupport();

    private static XRendererContext context = null;

    public static final int MODE_IMAGES_TEMP = 0;

    public static final int MODE_IMAGES_ALONG = 1;

    static final String IMAGE_CACHE_PROPERTY = "imageCache";

    static final String SAVE_CACHE = "XBrowser Document Save Cache";
}
