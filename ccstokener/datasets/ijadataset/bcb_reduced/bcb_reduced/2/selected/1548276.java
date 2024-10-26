package com.ssg.tools.jsonxml.common.tools;

import com.ssg.tools.jsonxml.common.Utilities;
import com.ssg.tools.jsonxml.json.schema.JSONSchemaException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;

/**
 * Returns reader stream for provided URI/URL assuming specified or default (if null, "UTF-8") encoding.
 * Override to dynamically substitute or cache URI contents.
 * 
 * @author ssg
 */
public abstract class URLResolver implements URIResolver {

    public abstract StreamSource resolveURL(URL url, String encoding) throws JSONSchemaException;

    public StreamSource resolveURI(URI uri, String encoding) throws JSONSchemaException {
        try {
            return resolveURL(uri.toURL(), encoding);
        } catch (Throwable th) {
            throw new JSONSchemaException("Failed to resolve schema URI. Error: " + th, th);
        }
    }

    public String getEncoding(String encoding) {
        if (encoding == null) {
            return "UTF-8";
        } else {
            return encoding;
        }
    }

    /**
     * Shortcut method to get reader-based source stream.
     * 
     * @param source
     * @return
     * @throws JSONSchemaException 
     */
    public StreamSource toStreamSource(Object source, String encoding) throws JSONSchemaException {
        return toStreamSource(source, encoding, true);
    }

    /**
     * Tries to interprets source as URI/URL, File, or resource and returns appropriately created StreamSource object.
     * 
     * @param source
     * @return
     * @throws JSONSchemaException 
     */
    public StreamSource toStreamSource(Object source, String encoding, boolean asReader) throws JSONSchemaException {
        encoding = getEncoding(encoding);
        if (source instanceof URL || source instanceof URI) {
            try {
                URL url = (source instanceof URL) ? (URL) source : ((URI) source).toURL();
                if (asReader) {
                    return new StreamSource(new InputStreamReader(url.openStream(), encoding));
                } else {
                    return new StreamSource(url.openStream());
                }
            } catch (MalformedURLException muex) {
                throw new JSONSchemaException("Failed to create stream source for URI '" + source + "'. Error: " + muex, muex);
            } catch (IOException ioex) {
                throw new JSONSchemaException("Failed to create stream source for URI/URL '" + source + "'. Error: " + ioex, ioex);
            }
        } else if (source instanceof String) {
            try {
                URL url = new URL((String) source);
                if (asReader) {
                    return new StreamSource(new InputStreamReader(url.openStream(), encoding));
                } else {
                    return new StreamSource(url.openStream());
                }
            } catch (MalformedURLException muex) {
                try {
                    if (asReader) {
                        return new StreamSource(Utilities.getReaderForResource((String) source), encoding);
                    } else {
                        return new StreamSource(Thread.currentThread().getContextClassLoader().getResourceAsStream((String) source));
                    }
                } catch (Throwable th) {
                    throw new JSONSchemaException("Failed to create stream source for resource '" + source + "'. Error: " + th, th);
                }
            } catch (IOException ioex) {
                throw new JSONSchemaException("Failed to create stream source for string '" + source + "'. Error: " + ioex, ioex);
            }
        } else if (source instanceof File && ((File) source).isFile()) {
            try {
                if (asReader) {
                    return new StreamSource(new InputStreamReader(new FileInputStream((File) source), encoding));
                } else {
                    return new StreamSource(new FileInputStream((File) source));
                }
            } catch (IOException ioex) {
                throw new JSONSchemaException("Failed to create stream source for file '" + source + "'. Error: " + ioex, ioex);
            }
        } else {
            throw new JSONSchemaException("Failed to create stream source for " + source);
        }
    }

    public static class CacheingURLResolver extends URLResolver {

        private static Map<URL, Object> _globalCache = new LinkedHashMap<URL, Object>();

        Map<URL, Object> _cache = new LinkedHashMap<URL, Object>();

        public CacheingURLResolver() throws JSONSchemaException {
            init();
        }

        @Override
        public StreamSource resolveURL(URL url, String encoding) throws JSONSchemaException {
            if (_cache.containsKey(url)) {
                return toStreamSource(_cache.get(url), encoding);
            } else if (_globalCache.containsKey(url)) {
                return toStreamSource(_globalCache.get(url), encoding);
            } else {
                return toStreamSource(url, encoding);
            }
        }

        public synchronized void register(URL url, Object object) {
            register(url, object, false);
        }

        public synchronized void register(URL url, Object object, boolean asGlobal) {
            if (url == null) {
                return;
            }
            if (object == null) {
                unregister(url, asGlobal);
            } else {
                if (asGlobal) {
                    _globalCache.put(url, object);
                } else {
                    _cache.put(url, object);
                }
            }
        }

        public synchronized void unregister(URL url) {
            unregister(url, false);
        }

        public synchronized void unregister(URL url, boolean asGlobal) {
            if (url == null) {
                return;
            }
            if (asGlobal) {
                if (_globalCache.containsKey(url)) {
                    _globalCache.remove(url);
                }
            } else if (_cache.containsKey(url)) {
                _cache.remove(url);
            }
        }

        public void clear() {
            _cache.clear();
        }

        public void init() throws JSONSchemaException {
        }

        /**
         * Registers defaults
         */
        static {
            try {
                _globalCache.put(new URL("http://json-schema.org/schema"), "schemas/http___json-schema.org_schema.schema.json");
                _globalCache.put(new URL("http://json-schema.org/draft-03/schema#"), "schemas/http___json-schema.org_schema.schema.json");
                _globalCache.put(new URL("http://json-schema.org/hyper-schema"), "schemas/http___json-schema.org_hyper-schema.schema.json");
                _globalCache.put(new URL("http://json-schema.org/draft-03/hyper-schema#"), "schemas/http___json-schema.org_hyper-schema.schema.json");
            } catch (Throwable th) {
            }
        }

        /**
         * @param href
         * @param base
         * @return
         * @throws TransformerException 
         */
        public Source resolve(String href, String base) throws TransformerException {
            return null;
        }
    }
}
