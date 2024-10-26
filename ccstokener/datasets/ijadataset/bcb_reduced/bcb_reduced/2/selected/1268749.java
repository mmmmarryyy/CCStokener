package org.zkoss.web.util.resource;

import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.net.URL;
import org.zkoss.lang.D;
import org.zkoss.util.resource.Loader;
import org.zkoss.util.logging.Log;

/**
 * A semi-implemented loader to used with {@link ResourceCaches#get}
 * to retrieve servlet resources.
 *
 * @author tomyeh
 */
public abstract class ResourceLoader implements Loader {

    private static final Log log = Log.lookup(ResourceLoader.class);

    protected ResourceLoader() {
    }

    /** Parses the specified file and returns the result which
	 * will be stored into the cache ({@link ResourceCaches#get}).
	 *
	 * <p>Deriving must override this method.
	 *
	 * @param extra the extra paramter passed from {@link ResourceCaches#get}.
	 */
    protected abstract Object parse(String path, File file, Object extra) throws Exception;

    /** Parses the specified URL and returns the result which
	 * will be stored into the cache ({@link ResourceCaches#get}).
	 *
	 * <p>Deriving must override this method.
	 *
	 * @param extra the extra paramter passed from {@link ResourceCaches#get}.
	 */
    protected abstract Object parse(String path, URL url, Object extra) throws Exception;

    public boolean shallCheck(Object src, long expiredMillis) {
        return expiredMillis > 0;
    }

    public long getLastModified(Object src) {
        final ResourceInfo si = (ResourceInfo) src;
        if (si.url != null) {
            final String protocol = si.url.getProtocol().toLowerCase();
            if (!"http".equals(protocol) && !"https".equals(protocol) && !"ftp".equals(protocol)) {
                try {
                    return si.url.openConnection().getLastModified();
                } catch (Throwable ex) {
                    return -1;
                }
            }
            return -1;
        }
        return si.file.lastModified();
    }

    public Object load(Object src) throws Exception {
        final ResourceInfo si = (ResourceInfo) src;
        if (si.url != null) return parse(si.path, si.url, si.extra);
        if (!si.file.exists()) {
            if (D.ON && log.debugable()) log.debug("Not found: " + si.file);
            return null;
        }
        if (D.ON && log.debugable()) log.debug("Loading " + si.file);
        try {
            return parse(si.path, si.file, si.extra);
        } catch (FileNotFoundException ex) {
            return null;
        }
    }
}
