package org.exist.http.servlets;

import org.apache.log4j.Logger;
import org.exist.EXistException;
import org.exist.collections.Collection;
import org.exist.dom.DocumentImpl;
import org.exist.security.AuthenticationException;
import org.exist.security.Permission;
import org.exist.security.PermissionDeniedException;
import org.exist.security.Subject;
import org.exist.security.internal.AccountImpl;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.storage.lock.Lock;
import org.exist.storage.serializers.Serializer;
import org.exist.storage.serializers.XIncludeFilter;
import org.exist.util.serializer.Receiver;
import org.exist.util.serializer.ReceiverToSAX;
import org.exist.util.serializer.SAXSerializer;
import org.exist.util.serializer.SAXToReceiver;
import org.exist.util.serializer.SerializerPool;
import org.exist.xmldb.XmldbURI;
import org.exist.xquery.Constants;
import org.exist.xquery.XPathException;
import org.exist.xquery.value.Item;
import org.exist.xquery.value.NodeValue;
import org.exist.xquery.value.Type;
import org.exist.xquery.value.ValueSequence;
import org.exist.xslt.TransformerFactoryAllocator;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TemplatesHandler;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamSource;
import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.zip.GZIPInputStream;

/**
 * eXist-db servlet for XSLT transformations.
 *
 * @author Wolfgang
 */
public class XSLTServlet extends HttpServlet {

    private static final long serialVersionUID = -7258405385386062151L;

    private static final String REQ_ATTRIBUTE_PREFIX = "xslt.";

    private static final String REQ_ATTRIBUTE_STYLESHEET = "xslt.stylesheet";

    private static final String REQ_ATTRIBUTE_INPUT = "xslt.input";

    private static final String REQ_ATTRIBUTE_OUTPUT = "xslt.output.";

    private static final String REQ_ATTRIBUTE_BASE = "xslt.base";

    private static final Logger LOG = Logger.getLogger(XSLTServlet.class);

    private BrokerPool pool;

    private final Map<String, CachedStylesheet> cache = new HashMap<String, CachedStylesheet>();

    private Boolean caching = null;

    /**
     * @return Value of TransformerFactoryAllocator.PROPERTY_CACHING_ATTRIBUTE or TRUE if not present.
     */
    private boolean isCaching() {
        if (caching == null) {
            Object property = pool.getConfiguration().getProperty(TransformerFactoryAllocator.PROPERTY_CACHING_ATTRIBUTE);
            if (property != null) {
                caching = (Boolean) property;
            } else {
                caching = true;
            }
        }
        return caching;
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String stylesheet = (String) request.getAttribute(REQ_ATTRIBUTE_STYLESHEET);
        if (stylesheet == null) {
            throw new ServletException("No stylesheet source specified!");
        }
        Item inputNode = null;
        String sourceAttrib = (String) request.getAttribute(REQ_ATTRIBUTE_INPUT);
        if (sourceAttrib != null) {
            Object sourceObj = request.getAttribute(sourceAttrib);
            if (sourceObj != null) {
                if (sourceObj instanceof ValueSequence) {
                    ValueSequence seq = (ValueSequence) sourceObj;
                    if (seq.size() == 1) {
                        sourceObj = seq.itemAt(0);
                    }
                }
                if (sourceObj instanceof Item) {
                    inputNode = (Item) sourceObj;
                    if (!Type.subTypeOf(inputNode.getType(), Type.NODE)) {
                        throw new ServletException("Input for XSLT servlet is not a node. Read from attribute " + sourceAttrib);
                    }
                    LOG.debug("Taking XSLT input from request attribute " + sourceAttrib);
                } else throw new ServletException("Input for XSLT servlet is not a node. Read from attribute " + sourceAttrib);
            }
        }
        try {
            pool = BrokerPool.getInstance();
        } catch (EXistException e) {
            throw new ServletException(e.getMessage(), e);
        }
        Subject user = pool.getSecurityManager().getGuestSubject();
        Subject requestUser = AccountImpl.getUserFromServletRequest(request);
        if (requestUser != null) user = requestUser;
        String userParam = (String) request.getAttribute("xslt.user");
        String passwd = (String) request.getAttribute("xslt.password");
        if (userParam != null) {
            try {
                user = pool.getSecurityManager().authenticate(userParam, passwd);
            } catch (AuthenticationException e1) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Wrong password or user");
                return;
            }
        }
        SAXTransformerFactory factory = TransformerFactoryAllocator.getTransformerFactory(pool);
        Templates templates = getSource(user, request, response, factory, stylesheet);
        if (templates == null) {
            return;
        }
        DBBroker broker = null;
        try {
            broker = pool.get(user);
            TransformerHandler handler = factory.newTransformerHandler(templates);
            setTransformerParameters(request, handler.getTransformer());
            Properties properties = handler.getTransformer().getOutputProperties();
            setOutputProperties(request, properties);
            String encoding = properties.getProperty("encoding");
            if (encoding == null) {
                encoding = "UTF-8";
            }
            response.setCharacterEncoding(encoding);
            String mediaType = properties.getProperty("media-type");
            if (mediaType != null) {
                if (encoding == null) response.setContentType(mediaType); else if (mediaType.indexOf("charset") == -1) response.setContentType(mediaType + "; charset=" + encoding); else response.setContentType(mediaType);
            }
            SAXSerializer sax = (SAXSerializer) SerializerPool.getInstance().borrowObject(SAXSerializer.class);
            Writer writer = new BufferedWriter(response.getWriter());
            sax.setOutput(writer, properties);
            SAXResult result = new SAXResult(sax);
            handler.setResult(result);
            Serializer serializer = broker.getSerializer();
            serializer.reset();
            Receiver receiver = new ReceiverToSAX(handler);
            try {
                XIncludeFilter xinclude = new XIncludeFilter(serializer, receiver);
                receiver = xinclude;
                String moduleLoadPath;
                String base = (String) request.getAttribute(REQ_ATTRIBUTE_BASE);
                if (base != null) {
                    moduleLoadPath = getServletContext().getRealPath(base);
                } else if (stylesheet.startsWith("xmldb:exist://")) {
                    moduleLoadPath = XmldbURI.xmldbUriFor(stylesheet).getCollectionPath();
                } else {
                    moduleLoadPath = getCurrentDir(request).getAbsolutePath();
                }
                xinclude.setModuleLoadPath(moduleLoadPath);
                serializer.setReceiver(receiver);
                if (inputNode != null) {
                    serializer.toSAX((NodeValue) inputNode);
                } else {
                    SAXToReceiver saxreceiver = new SAXToReceiver(receiver);
                    XMLReader reader = pool.getParserPool().borrowXMLReader();
                    reader.setContentHandler(saxreceiver);
                    InputStream stream;
                    InputStream inStream = new BufferedInputStream(request.getInputStream());
                    inStream.mark(10);
                    try {
                        stream = new GZIPInputStream(inStream);
                    } catch (IOException e) {
                        inStream.reset();
                        stream = inStream;
                    }
                    reader.parse(new InputSource(stream));
                }
            } catch (SAXParseException e) {
                LOG.error(e.getMessage());
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            } catch (SAXException e) {
                throw new ServletException("SAX exception while transforming node: " + e.getMessage(), e);
            } finally {
                SerializerPool.getInstance().returnObject(sax);
            }
            writer.flush();
            response.flushBuffer();
        } catch (IOException e) {
            throw new ServletException("IO exception while transforming node: " + e.getMessage(), e);
        } catch (TransformerException e) {
            throw new ServletException("Exception while transforming node: " + e.getMessage(), e);
        } catch (Throwable e) {
            LOG.error(e);
            throw new ServletException("An error occurred: " + e.getMessage(), e);
        } finally {
            pool.release(broker);
        }
    }

    private Templates getSource(Subject user, HttpServletRequest request, HttpServletResponse response, SAXTransformerFactory factory, String stylesheet) throws ServletException, IOException {
        if (stylesheet.indexOf(':') == Constants.STRING_NOT_FOUND) {
            File f = new File(stylesheet);
            if (f.canRead()) {
                stylesheet = f.toURI().toASCIIString();
            } else {
                if (f.isAbsolute()) {
                    if (stylesheet.startsWith("//")) {
                        stylesheet = stylesheet.replaceFirst("//", "/");
                    }
                    String url = getServletContext().getRealPath(stylesheet);
                    if (url == null) {
                        response.sendError(HttpServletResponse.SC_NOT_FOUND, "Stylesheet not found (URL: " + stylesheet + ")");
                        return null;
                    }
                    f = new File(url);
                    stylesheet = f.toURI().toASCIIString();
                } else {
                    f = new File(getCurrentDir(request), stylesheet);
                    stylesheet = f.toURI().toASCIIString();
                }
                if (!f.canRead()) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND, "Stylesheet not found (URL: " + stylesheet + ")");
                    return null;
                }
            }
        }
        String base;
        int p = stylesheet.lastIndexOf("/");
        if (p != Constants.STRING_NOT_FOUND) {
            base = stylesheet.substring(0, p);
        } else {
            base = stylesheet;
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Loading stylesheet from " + stylesheet);
        }
        CachedStylesheet cached = cache.get(stylesheet);
        if (cached == null) {
            cached = new CachedStylesheet(factory, user, stylesheet, base);
            cache.put(stylesheet, cached);
        }
        return cached.getTemplates(user);
    }

    private File getCurrentDir(HttpServletRequest request) {
        String path = request.getPathTranslated();
        if (path == null) {
            path = request.getRequestURI().substring(request.getContextPath().length());
            int p = path.lastIndexOf('/');
            if (p != Constants.STRING_NOT_FOUND) {
                path = path.substring(0, p);
            }
            path = getServletContext().getRealPath(path);
        }
        File file = new File(path);
        if (file.isDirectory()) {
            return file;
        } else {
            return file.getParentFile();
        }
    }

    /**
     *  Copy "xslt." attributes from HTTP request to transformer. Does not copy 'input', 'output'
     * and 'styleheet' attributes.
     */
    private void setTransformerParameters(HttpServletRequest request, Transformer transformer) throws XPathException {
        for (Enumeration<String> e = request.getAttributeNames(); e.hasMoreElements(); ) {
            String name = e.nextElement();
            if (name.startsWith(REQ_ATTRIBUTE_PREFIX) && !(name.startsWith(REQ_ATTRIBUTE_OUTPUT) || REQ_ATTRIBUTE_INPUT.equals(name) || REQ_ATTRIBUTE_STYLESHEET.equals(name))) {
                Object value = request.getAttribute(name);
                if (value instanceof NodeValue) {
                    NodeValue nv = (NodeValue) value;
                    if (nv.getImplementationType() == NodeValue.IN_MEMORY_NODE) {
                        value = nv.toMemNodeSet();
                    }
                }
                transformer.setParameter(name, value);
                transformer.setParameter(name.substring(REQ_ATTRIBUTE_PREFIX.length()), value);
            }
        }
    }

    /**
     * Copies 'output' attributes to properties object.
     */
    private void setOutputProperties(HttpServletRequest request, Properties properties) {
        for (Enumeration<String> e = request.getAttributeNames(); e.hasMoreElements(); ) {
            String name = e.nextElement();
            if (name.startsWith(REQ_ATTRIBUTE_OUTPUT)) {
                Object value = request.getAttribute(name);
                if (value != null) {
                    properties.setProperty(name.substring(REQ_ATTRIBUTE_OUTPUT.length()), value.toString());
                }
            }
        }
    }

    private class CachedStylesheet {

        SAXTransformerFactory factory;

        long lastModified = -1;

        Templates templates = null;

        String uri;

        public CachedStylesheet(SAXTransformerFactory factory, Subject user, String uri, String baseURI) throws ServletException {
            this.factory = factory;
            this.uri = uri;
            if (!baseURI.startsWith("xmldb:exist://")) {
                factory.setURIResolver(new ExternalResolver(baseURI));
            }
            getTemplates(user);
        }

        public Templates getTemplates(Subject user) throws ServletException {
            if (uri.startsWith("xmldb:exist://")) {
                String docPath = uri.substring("xmldb:exist://".length());
                DocumentImpl doc = null;
                DBBroker broker = null;
                try {
                    broker = pool.get(user);
                    doc = broker.getXMLResource(XmldbURI.create(docPath), Lock.READ_LOCK);
                    if (doc == null) {
                        throw new ServletException("Stylesheet not found: " + docPath);
                    }
                    if (!isCaching() || (doc != null && (templates == null || doc.getMetadata().getLastModified() > lastModified))) {
                        templates = getSource(broker, doc);
                    }
                    lastModified = doc.getMetadata().getLastModified();
                } catch (PermissionDeniedException e) {
                    throw new ServletException("Permission denied to read stylesheet: " + uri, e);
                } catch (EXistException e) {
                    throw new ServletException("Error while reading stylesheet source from db: " + e.getMessage(), e);
                } finally {
                    pool.release(broker);
                    if (doc != null) {
                        doc.getUpdateLock().release(Lock.READ_LOCK);
                    }
                }
            } else {
                try {
                    URL url = new URL(uri);
                    URLConnection connection = url.openConnection();
                    long modified = connection.getLastModified();
                    if (!isCaching() || (templates == null || modified > lastModified || modified == 0)) {
                        LOG.debug("compiling stylesheet " + url.toString());
                        templates = factory.newTemplates(new StreamSource(connection.getInputStream()));
                    }
                    lastModified = modified;
                } catch (IOException e) {
                    throw new ServletException("Error while reading stylesheet source from uri: " + uri + ": " + e.getMessage(), e);
                } catch (TransformerConfigurationException e) {
                    throw new ServletException("Error while reading stylesheet source from uri: " + uri + ": " + e.getMessage(), e);
                }
            }
            return templates;
        }

        private Templates getSource(DBBroker broker, DocumentImpl stylesheet) throws ServletException {
            factory.setURIResolver(new DatabaseResolver(broker, stylesheet));
            try {
                TemplatesHandler handler = factory.newTemplatesHandler();
                handler.startDocument();
                Serializer serializer = broker.getSerializer();
                serializer.reset();
                serializer.setSAXHandlers(handler, null);
                serializer.toSAX(stylesheet);
                handler.endDocument();
                return handler.getTemplates();
            } catch (SAXException e) {
                throw new ServletException("A SAX exception occurred while compiling the stylesheet: " + e.getMessage(), e);
            } catch (TransformerConfigurationException e) {
                throw new ServletException("A configuration exception occurred while " + "compiling the stylesheet: " + e.getMessage(), e);
            }
        }
    }

    private class ExternalResolver implements URIResolver {

        private String baseURI;

        public ExternalResolver(String base) {
            this.baseURI = base;
        }

        @Override
        public Source resolve(String href, String base) throws TransformerException {
            URL url;
            try {
                url = new URL(baseURI + "/" + href);
                URLConnection connection = url.openConnection();
                return new StreamSource(connection.getInputStream());
            } catch (MalformedURLException e) {
                return null;
            } catch (IOException e) {
                return null;
            }
        }
    }

    private class DatabaseResolver implements URIResolver {

        DocumentImpl doc;

        DBBroker broker;

        public DatabaseResolver(DBBroker broker, DocumentImpl myDoc) {
            this.broker = broker;
            this.doc = myDoc;
        }

        @Override
        public Source resolve(String href, String base) throws TransformerException {
            Collection collection = doc.getCollection();
            String path;
            if (href.startsWith("/")) {
                path = href;
            } else {
                path = collection.getURI() + "/" + href;
            }
            DocumentImpl xslDoc;
            try {
                xslDoc = (DocumentImpl) broker.getXMLResource(XmldbURI.create(path));
            } catch (PermissionDeniedException e) {
                throw new TransformerException(e.getMessage(), e);
            }
            if (xslDoc == null) {
                LOG.debug("Document " + href + " not found in collection " + collection.getURI());
                return null;
            }
            if (!xslDoc.getPermissions().validate(broker.getSubject(), Permission.READ)) {
                throw new TransformerException("Insufficient privileges to read resource " + path);
            }
            DOMSource source = new DOMSource(xslDoc);
            return source;
        }
    }
}
