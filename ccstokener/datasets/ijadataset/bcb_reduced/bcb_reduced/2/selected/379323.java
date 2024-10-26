package gnu.xml.transform;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import gnu.xml.dom.ls.ReaderInputStream;

/**
 * URI resolver for XSLT.
 * This resolver parses external entities into DOMSources. It
 * maintains a cache of URIs to DOMSources to avoid expensive re-parsing.
 *
 * @author <a href='mailto:dog@gnu.org'>Chris Burdess</a>
 */
class XSLURIResolver implements URIResolver {

    Map lastModifiedCache = new HashMap();

    Map nodeCache = new HashMap();

    DocumentBuilder builder;

    URIResolver userResolver;

    ErrorListener userListener;

    void setUserResolver(URIResolver userResolver) {
        this.userResolver = userResolver;
    }

    void setUserListener(ErrorListener userListener) {
        this.userListener = userListener;
    }

    /**
   * Clear the cache.
   */
    void flush() {
        lastModifiedCache.clear();
        nodeCache.clear();
    }

    public Source resolve(String href, String base) throws TransformerException {
        Source source = null;
        if (userResolver != null) {
            source = userResolver.resolve(base, href);
        }
        return resolveDOM(source, href, base);
    }

    DOMSource resolveDOM(Source source, String base, String href) throws TransformerException {
        if (source != null && source instanceof DOMSource) {
            return (DOMSource) source;
        }
        String systemId = (source == null) ? null : source.getSystemId();
        long lastModified = 0L, lastLastModified = 0L;
        try {
            URL url = resolveURL(systemId, base, href);
            Node node = null;
            InputStream in = null;
            if (source instanceof StreamSource) {
                StreamSource ss = (StreamSource) source;
                in = ss.getInputStream();
                if (in == null) {
                    Reader reader = ss.getReader();
                    if (reader != null) {
                        in = new ReaderInputStream(reader);
                    }
                }
            }
            if (in == null) {
                if (url != null) {
                    systemId = url.toString();
                    node = (Node) nodeCache.get(systemId);
                    URLConnection conn = url.openConnection();
                    Long llm = (Long) lastModifiedCache.get(systemId);
                    if (llm != null) {
                        lastLastModified = llm.longValue();
                        conn.setIfModifiedSince(lastLastModified);
                    }
                    conn.connect();
                    lastModified = conn.getLastModified();
                    if (node != null && lastModified > 0L && lastModified <= lastLastModified) {
                        return new DOMSource(node, systemId);
                    } else {
                        in = conn.getInputStream();
                        nodeCache.put(systemId, node);
                        lastModifiedCache.put(systemId, new Long(lastModified));
                    }
                } else {
                    throw new TransformerException("can't resolve URL: " + systemId);
                }
            }
            InputSource input = new InputSource(in);
            input.setSystemId(systemId);
            DocumentBuilder builder = getDocumentBuilder();
            node = builder.parse(input);
            return new DOMSource(node, systemId);
        } catch (IOException e) {
            throw new TransformerException(e);
        } catch (SAXException e) {
            throw new TransformerException(e);
        }
    }

    URL resolveURL(String systemId, String base, String href) throws IOException {
        URL url = null;
        try {
            if (systemId != null) {
                try {
                    url = new URL(systemId);
                } catch (MalformedURLException e) {
                }
            }
            if (url == null) {
                if (base != null) {
                    URL baseURL = new URL(base);
                    url = new URL(baseURL, href);
                } else if (href != null) {
                    url = new URL(href);
                } else {
                    throw new MalformedURLException(systemId);
                }
            }
            return url;
        } catch (MalformedURLException e) {
            File file = null;
            if (href == null) {
                href = systemId;
            }
            if (base != null) {
                int lsi = base.lastIndexOf(File.separatorChar);
                if (lsi != -1 && lsi < base.length() - 1) {
                    base = base.substring(0, lsi);
                }
                File baseFile = new File(base);
                file = new File(baseFile, href);
            } else if (href != null) {
                file = new File(href);
            }
            return (file == null) ? null : file.toURL();
        }
    }

    DocumentBuilder getDocumentBuilder() throws TransformerException {
        try {
            if (builder == null) {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setNamespaceAware(true);
                factory.setExpandEntityReferences(true);
                builder = factory.newDocumentBuilder();
            }
            if (userResolver != null) {
                builder.setEntityResolver(new URIResolverEntityResolver(userResolver));
            }
            if (userListener != null) {
                builder.setErrorHandler(new ErrorListenerErrorHandler(userListener));
            }
            return builder;
        } catch (Exception e) {
            throw new TransformerException(e);
        }
    }
}
