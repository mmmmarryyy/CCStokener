package org.softnetwork.xml.sax;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.softnetwork.io.RewindableInputStream;
import org.softnetwork.log.LightConsole;
import org.softnetwork.xml.XMLException;
import org.softnetwork.xml.dom.DOMDocumentFactory;
import org.softnetwork.xml.dom.NodeWrapper;
import org.softnetwork.xml.dom.OutputNode;
import org.softnetwork.xml.sax.helpers.SAXReader;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLFilter;

/**
 * @author $Author: smanciot $
 * 
 * @version $Revision: 130 $
 */
public final class InputNodeService extends NodeWrapper implements InputNode {

    private URL url;

    /**
	 * entityResolver
	 */
    private EntityResolver entityResolver;

    /**
	 * errorHandler
	 */
    private ErrorHandler errorHandler;

    private InputStream is;

    private boolean read = false;

    private XMLFilter[] filters;

    private InputNode node;

    /**
	 * @param node
	 */
    protected InputNodeService(InputNode node) {
        super(node);
        this.node = node;
    }

    public final void clear() {
    }

    public final boolean isRead() {
        return read;
    }

    public final void read() throws XMLException {
        read(false, null, null);
    }

    public final void read(boolean validating, Map features, Map properties) throws XMLException {
        if (!read) {
            long before = System.currentTimeMillis();
            if (is == null && node instanceof OutputNode) reset();
            Document document = null;
            if (node instanceof Document) document = (Document) node; else document = getOwnerDocument();
            if (document == null) document = DOMDocumentFactory.newInstance();
            SAXReader reader = new SAXReader(document, node, filters, features, properties);
            reader.setEntityResolver(entityResolver);
            reader.setErrorHandler(errorHandler);
            try {
                reader.setFeature(SAX_VALIDATION_FEATURE_ID, validating);
            } catch (SAXNotRecognizedException e) {
            } catch (SAXNotSupportedException e) {
            }
            try {
                InputSource source = new InputSource(is);
                if (url != null) source.setSystemId(url.getPath());
                reader.parse(source);
            } catch (IOException e) {
                throw new XMLException(e);
            } catch (SAXException e) {
                throw new XMLException(e);
            }
            read = true;
            LightConsole.info("Reading within " + (System.currentTimeMillis() - before) + " ms");
        }
    }

    public final void read(boolean validating) throws XMLException {
        read(validating, null, null);
    }

    public final void read(InputStream is) throws XMLException {
        setIs(is);
        read();
    }

    public final void reset() {
        if (is == null && node instanceof OutputNode) setIs(new ByteArrayInputStream(((OutputNode) node).getBytes()));
        if (read) if (is != null && is instanceof RewindableInputStream) try {
            ((RewindableInputStream) is).rewind();
        } catch (IOException e) {
            LightConsole.error(e);
        } else if (node instanceof OutputNode) setIs(new ByteArrayInputStream(((OutputNode) node).getBytes()));
        NodeList list = getChildNodes();
        int len = list.getLength();
        List copy = new ArrayList();
        for (int i = 0; i < len; i++) copy.add(list.item(i));
        Iterator it = copy.iterator();
        while (it.hasNext()) {
            Node child = (Node) it.next();
            removeChild(child);
            child = null;
        }
        copy.clear();
        copy = null;
        clear();
        read = false;
    }

    public final void setBytes(byte[] bytes) {
        setIs(new ByteArrayInputStream(bytes));
    }

    public final void setFilters(XMLFilter[] filters) {
        this.filters = filters;
    }

    public final void setIs(InputStream is) {
        if (is != null) {
            this.is = is;
            NodeList list = getChildNodes();
            int len = list.getLength();
            List copy = new ArrayList();
            for (int i = 0; i < len; i++) copy.add(list.item(i));
            Iterator it = copy.iterator();
            while (it.hasNext()) {
                Node child = (Node) it.next();
                removeChild(child);
                child = null;
            }
            copy.clear();
            copy = null;
            clear();
            read = false;
        }
    }

    public final void setIs(URL url) throws IOException {
        if (url != null) {
            this.url = url;
            setIs(url.openStream());
        }
    }

    /**
	 * @param entityResolver
	 *            The entityResolver to set.
	 */
    public void setEntityResolver(EntityResolver entityResolver) {
        this.entityResolver = entityResolver;
    }

    /**
	 * @param errorHandler
	 *            The errorHandler to set.
	 */
    public void setErrorHandler(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }
}
