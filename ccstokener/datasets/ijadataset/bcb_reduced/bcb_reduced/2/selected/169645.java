package org.twdata.pipeline;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.Iterator;
import java.util.List;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamSource;
import org.apache.commons.logging.*;
import org.twdata.pipeline.cache.*;

/**
 *  Manages XSL Templates by caching compiled XSL files for a certain amount of
 *  time.
 */
public class TemplatesManagerImpl implements TemplatesManager {

    private static Log log = LogFactory.getLog(TemplatesManager.class);

    /**  If stylesheets will be automatically reloaded */
    private boolean autoReloadTemplates = false;

    private SourceResolver resolver;

    private Cache cache;

    private long expiryTime;

    /**
     *  Sets the cache
     *
     *@param  cache  The new cache value
     */
    public void setCache(Cache cache) {
        this.cache = cache;
    }

    /**
     *  Sets whether the templates will be auto-reloaded
     *
     *@param  reload  True to auto-reload local templates
     */
    public void setAutoReloadLocalTemplates(boolean reload) {
        autoReloadTemplates = reload;
    }

    /**
     *  Sets the object to use to resolve paths
     *
     *@param resolver The source resolver 
     */
    public void setSourceResolver(SourceResolver resolver) {
        this.resolver = resolver;
    }

    /**
     *  Gets the object to use to resolve paths
     *
     *@return The source resolver 
     */
    public SourceResolver getSourceResolver() {
        return this.resolver;
    }

    /**
     *  Sets the expiration time
     *
     *@param  time  The time in seconds when to expire from creation, 0 to never
     *      expire
     */
    public void setExpiryTime(long time) {
        this.expiryTime = time;
    }

    /**
     *  Gets a compiled stylesheet for the given path. It first tries to get a
     *  cached Templates. If one cannot be found, it creates a new Templates,
     *  stores it in cache, and returns it.
     *
     *@param  factory  The SAX transformer factory
     *@param  path     The URL to the template
     *@return          The found templates
     */
    public Templates getTemplates(SAXTransformerFactory factory, String path) {
        try {
            Templates templates = getTemplatesFromCache(path);
            if (templates == null) {
                if (log.isDebugEnabled()) {
                    log.debug("Creating new Templates for " + path);
                }
                long trace = 0;
                if (log.isDebugEnabled()) {
                    trace = System.currentTimeMillis();
                }
                Source source = null;
                long lastModified = 0;
                URL url = null;
                try {
                    if (resolver != null) {
                        url = resolver.resolve(path);
                    } else {
                        url = new URL(path);
                    }
                } catch (MalformedURLException ex) {
                    url = new File(path).toURL();
                }
                if ("file".equalsIgnoreCase(url.getProtocol()) && autoReloadTemplates) {
                    if (log.isDebugEnabled()) {
                        log.debug("Loading template from filesystem if changed");
                    }
                    File file = new File(url.getPath());
                    if (!file.exists()) {
                        log.error("Stylesheet " + path + " cannot be found");
                        return null;
                    }
                    source = new StreamSource(new FileInputStream(file));
                    source.setSystemId(file.getAbsolutePath());
                    lastModified = file.lastModified();
                } else {
                    source = new StreamSource(url.openStream());
                    source.setSystemId(url.toString());
                    if (log.isDebugEnabled()) {
                        log.debug("Loading template from url");
                    }
                }
                templates = factory.newTemplates(source);
                if (log.isDebugEnabled()) {
                    log.debug("Template compilation time:" + (System.currentTimeMillis() - trace));
                }
                putTemplates(templates, path, lastModified);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Reusing Templates for " + path);
                }
            }
            return templates;
        } catch (Exception e) {
            log.error("Exception in creating Transform Handler", e);
            return null;
        }
    }

    /**
     *  Gets a Templates from cache. If one cannot be found, returns null.
     *
     *@param  path  The path to the template file
     *@return       The templates value
     */
    protected Templates getTemplatesFromCache(String path) {
        Templates templates = null;
        if (cache.getCacheable(path) != null) {
            CacheWrapper cw = (CacheWrapper) cache.getCacheable(path);
            templates = (Templates) cw.getWrapped();
            if (autoReloadTemplates) {
                File file = new File(path);
                if (!file.exists()) {
                    log.error("Unable to find stylesheet:" + path);
                    templates = null;
                    cache.removeCacheable(path);
                } else {
                    long lastmod = file.lastModified();
                    if (lastmod != cw.getLastModifiedTime()) {
                        templates = null;
                        cache.removeCacheable(path);
                        if (log.isDebugEnabled()) {
                            log.debug("Stylesheet " + path + " has been recently modified, removing cached");
                        }
                    }
                }
            }
            if (templates != null && log.isDebugEnabled()) {
                log.debug("Pulled template " + path + "from cache");
            }
        }
        return templates;
    }

    /**
     *  Puts a Templates in cache.
     *
     *@param  templates  The Templates to store in cache
     *@param  id         The unique identifier of the stylesheet
     *@param  lastmod    The last modified timestamp
     */
    protected void putTemplates(Templates templates, String id, long lastmod) {
        long exp = 0;
        if (expiryTime == 0) {
            exp = Long.MAX_VALUE;
        } else {
            exp = System.currentTimeMillis() + (expiryTime * 1000);
        }
        CacheWrapper cw = new CacheWrapper(id, exp, lastmod, templates);
        cache.addCacheable(cw);
        if (log.isDebugEnabled()) {
            log.debug("Adding template " + id + " to cache");
        }
    }
}
