package org.salamandra.web.core.transformer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import javax.servlet.ServletContext;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.salamandra.web.core.resource.ResourceUtils;
import org.salamandra.web.core.transformer.node.tx.TrXO;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;
import com.sun.org.apache.xalan.internal.xsltc.trax.DOM2SAX;

/**
 * @author: Luigi Scarpato
 * 
 */
public abstract class SAXTransformer extends Transformer implements URIResolver {

    private ServletContext context;

    private static SAXTransformerFactory stf = null;

    protected Log LOG = LogFactory.getLog(SAXTransformer.class);

    ;

    public void init(ServletContext context) {
        this.context = context;
    }

    /**
     * Creates a new identity transformer.
     */
    public SAXTransformerFactory getSaxTransformerHandler() {
        synchronized (TrXO.class) {
            if (stf == null) {
                stf = (SAXTransformerFactory) TransformerFactory.newInstance();
                stf.setURIResolver(this);
            }
            return stf;
        }
    }

    public XMLReader creteXmlReaderBySource(Source source) throws SAXException {
        XMLReader reader;
        if (source instanceof DOMSource) {
            final DOMSource domsrc = (DOMSource) source;
            final org.w3c.dom.Node node = domsrc.getNode();
            reader = new DOM2SAX(node);
        } else {
            reader = XMLReaderFactory.createXMLReader();
        }
        return reader;
    }

    public ServletContext getServletContext() {
        return context;
    }

    public Source resolve(String href, String base) {
        assert href != null;
        URL url;
        try {
            url = ResourceUtils.getRealPath(href, context);
        } catch (FileNotFoundException e) {
            LOG.error("File not found '" + href + "'", e);
            return null;
        } catch (MalformedURLException e) {
            LOG.error("Error creating URL '" + href + "'", e);
            return null;
        }
        String urlPath = url.toString();
        String systemId = urlPath.substring(0, urlPath.lastIndexOf('/') + 1);
        StreamSource s = null;
        try {
            s = new StreamSource(url.openStream(), systemId);
        } catch (IOException e) {
            LOG.error("Can't load the resource from '" + href + "'", e);
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("href:" + href + " base:" + base + " resolved path:" + s.getSystemId());
        }
        return s;
    }

    public Object clone() throws CloneNotSupportedException {
        SAXTransformer clone = (SAXTransformer) super.clone();
        clone.context = context;
        clone.LOG = LOG;
        return clone;
    }
}
