package mipt.common;

import java.io.File;
import java.io.IOException;
import java.net.ContentHandler;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownServiceException;
import java.util.HashMap;
import java.util.Map;

/**
 * By default works just like ClassLoader.getSystemResource().getContent() but 
 *  if ClassLoader.getSystemResource() failed, the second option is retrieving URL
 *  from "/"+path via purpose.getClass().getClassLoader().
 * For the third option you should call setRelativePath(true), root (that is typically
 *  null in this case) will be considerer to be in this.getClass() package directory.
 * If none if the above succeed, new URL(path) is tried (for external resources).
 * Other logic is delegated to any Transformer<URL,Object>.
 * As usual, this class must be subclassed with not-content-type-based getContentType()
 *  implementation: see mipt.io package. In this case, handler's map of the Transformer delegate
 *  (that otherwise would be filled externally) plays the role of handler's cache (i.e. the map
 *  is filled internally to save memory).
 * @author Evdokimov
 */
public class DefaultResourceFactory extends AbstractResourceFactory {

    private Transformer<URL, Object> transformer;

    private boolean relativePath = false;

    /**
	 * One more level of delegation is introduced here - java.net.ContentHandler.
	 * The transformer itself remain responsible only for high-level logic (including check
	 *  whether content type is standard or it's handler's class name) and for choosing
	 *  handler according to the Purpose (some handlers can be set manually, some are created here).
	 */
    public class DefaultTransformer implements Transformer<URL, Object> {

        private Map<Purpose, ContentHandler> handlers;

        /**
		 * You can't put arbitrary handler to this map: it must be instanceof class which name is
		 *  defined by getContentType() (otherwise the handler will be replaced: getContentType() has priority).
		 */
        public final Map<Purpose, ContentHandler> getHandlers() {
            if (handlers == null) handlers = initHandlers();
            return handlers;
        }

        /**
		 * May be overridden to fill all necessary handlers.
		 */
        protected Map<Purpose, ContentHandler> initHandlers() {
            return new HashMap<Purpose, ContentHandler>(8);
        }

        public void setHandlers(Map<Purpose, ContentHandler> handlers) {
            this.handlers = handlers;
        }

        /**
		 * Loads object content from the valid URL.
		 * @param name - may be used by overriders only
		 * @throws UnknownServiceException - if getContentType() returns null or us unsupported.
		 * @throws IOException - if can't open connection or error during reading.
		 * @see mipt.common.AbstractResourceFactory.Transformer#get(java.lang.Object, java.lang.String, mipt.common.ResourceFactory.Purpose)
		 */
        public Object get(URL url, String name, Purpose purpose) throws IOException {
            URLConnection conn = url.openConnection();
            String type = getContentType(conn, purpose);
            if (type == null) throw new UnknownServiceException("You must either load resource with content-type attribute or override DefaultResourceFactory.getContentType()");
            if (type.indexOf('.') < 0) return conn.getContent();
            return getContentHandler(type, conn, purpose).getContent(conn);
        }

        /**
		 * Returns non-standard handler in the case when content type contains full class name.
		 * Is not called if {@link #getContentType(URLConnection,Purpose)} is not overridden.
		 */
        public ContentHandler getContentHandler(String handlerClassName, URLConnection conn, Purpose purpose) throws UnknownServiceException {
            ContentHandler handler = purpose == null ? null : getHandlers().get(purpose);
            if (handler == null && purpose != Purpose.ROOT) handler = getHandlers().get(Purpose.ROOT);
            try {
                Class cls = Class.forName(handlerClassName);
                if (handler == null || !cls.isInstance(handler)) {
                    handler = (ContentHandler) cls.newInstance();
                    getHandlers().put(purpose, handler);
                }
                return handler;
            } catch (Exception e) {
                if (handler != null) return handler;
                throw new UnknownServiceException("Can't instantiate: " + e.getMessage());
            }
        }
    }

    /**
	 * 
	 */
    public DefaultResourceFactory() {
    }

    /**
	 * @param root - sets the root for ROOT purpose (for loading "main" objects; also used as default root).
	 */
    public DefaultResourceFactory(String root) {
        super(root);
    }

    public final Transformer<URL, Object> getTransformer() {
        if (transformer == null) transformer = initTransformer();
        return transformer;
    }

    protected Transformer<URL, Object> initTransformer() {
        return new DefaultTransformer();
    }

    public void setTransformer(Transformer<URL, Object> transformer) {
        this.transformer = transformer;
    }

    /**
	 * Use only of you didn't set transformer that does not inherit DefaultTransformer.
	 */
    public final Map<Purpose, ContentHandler> getHandlers() {
        return ((DefaultTransformer) getTransformer()).getHandlers();
    }

    public final boolean isRelativePath() {
        return relativePath;
    }

    public void setRelativePath(boolean relativePath) {
        this.relativePath = relativePath;
    }

    /**
	 * By default, uses the package dir of getClass() as a point to load the resource from.
	 * @see mipt.common.ResourceFactory#get(java.lang.String, mipt.common.ResourceFactory.Purpose)
	 */
    public Object get(String name, Purpose purpose) throws IOException {
        name = Utils.getPathNormal(name, getResourcePoint(name, purpose));
        String path = getPath(name, purpose);
        URL url = isRelativePath() ? getClass().getResource(path) : Utils.getResource(path, getClass());
        if (url == null) url = getExternalResource(path);
        return getTransformer().get(url, name, purpose);
    }

    /**
	 * Returns the argument for {@link Utils#getPathNormal(String, Object)}.
	 * The overrider could use {@link #getCurrentResourceName()}
	 * @param name To use in condition of overrider
	 * @param purpose To use in condition of overrider
	 */
    protected Object getResourcePoint(String name, Purpose purpose) {
        return getClass();
    }

    /**
	 * Creates non-system (and non-classloader-based) resource URL.
	 * This does not usually occur (but often occurs in IDE where classpath does not have user.dir).
	 * @return non null if no MalformedURLException arose
	 * @throws IOException
	 */
    protected URL getExternalResource(String path) throws IOException {
        try {
            if (!path.contains("://")) {
                if (new File(path).isAbsolute()) path = "file:///" + path; else path = "file:///" + Const.userDir + Const.fileSep + path;
            }
            return new URL(path.replace('\\', '/'));
        } catch (MalformedURLException e) {
            throw new IOException(path + " is not found");
        }
    }

    /**
	 * Called by DefaultTransformer only!
	 * Can return real content type or java.net.ContentHandler subclass' full name
	 *  (if content type contains '.', is assumed to be such class name).
	 * By default extracts content type from the content itself (it must be set explicitly).
	 */
    protected String getContentType(URLConnection conn, Purpose purpose) {
        return conn.getContentType();
    }
}
