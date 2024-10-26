package com.novocode.naf.resource.xml;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import org.w3c.dom.Node;
import com.novocode.naf.app.NAFException;

/**
 * Manages XSLT transformations.
 *
 * @author Stefan Zeiger (szeiger@novocode.com)
 * @since Oct 27, 2004
 * @version $Id: StyleManager.java 397 2008-04-10 12:50:40Z szeiger $
 */
final class StyleManager {

    private static final TransformerFactory tFactory = TransformerFactory.newInstance();

    public StyleManager() {
    }

    public Transformer createTransformer(URL url) throws NAFException {
        InputStream in = null;
        try {
            in = url.openStream();
            Transformer t = tFactory.newTransformer(new StreamSource(in));
            return t;
        } catch (IOException ex) {
            throw new NAFException("Error loading style sheet URL \"" + url + "\"", ex);
        } catch (TransformerConfigurationException ex) {
            throw new NAFException("Error configuring XML transformer", ex);
        } finally {
            if (in != null) try {
                in.close();
            } catch (IOException ignored) {
            }
        }
    }

    public Node transform(Transformer t, Node n) throws NAFException {
        DOMSource s = new DOMSource(n);
        DOMResult r = new DOMResult();
        try {
            t.transform(s, r);
            return r.getNode();
        } catch (TransformerException ex) {
            throw new NAFException("Error performing XSLT transformation", ex);
        }
    }
}
