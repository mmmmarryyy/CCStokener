package org.dcm4chee.xero.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.util.HashMap;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import org.dcm4chee.xero.metadata.MetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * An XmlModel object is a simple representation of an XML data structure,
 * intended to be used easily by templating views. This isn't really intended to
 * be the originating structure, but rather is intended to be a very fast
 * parsing/usage structure that has enough extensibility to allow using existing
 * data sources efficiently.
 * 
 * @author bwallace
 * 
 */
@SuppressWarnings("serial")
public class XmlModel extends HashMap<String, Object> {

    private static final Logger log = LoggerFactory.getLogger(XmlModel.class);

    protected String resource;

    /** The URL for a fixed resource */
    URL url;

    /**
    * Contains defaults from the parent object.
    */
    protected XmlModel parent;

    /**
    * The pattern for the name for this object
    */
    protected String templatePattern;

    public XmlModel() {
        log.debug("Creating a new XmlModel object.");
    }

    /**
    * Create an XmlModel object with the given initial capacity.
    * @param initialCapacity
    */
    public XmlModel(int initialCapacity) {
        super(initialCapacity);
    }

    /**
    * The XML model can have the initial data created from a resource - this is
    * done statically and allows the same object to be re-used many times.
    * Throws an IllegalArgumentException if the file isn't valid XML or can't be read/found.
    * 
    * @param resource
    */
    @MetaData(required = false)
    public synchronized void setResource(String resource) {
        this.resource = resource;
        log.debug("Setting XmlModel resource to " + resource);
        clear();
        url = Thread.currentThread().getContextClassLoader().getResource(resource);
        if (url == null) {
            throw new IllegalArgumentException("Resource " + resource + " not found.");
        }
        try {
            parseURL(url);
        } catch (SAXException e) {
            throw new IllegalArgumentException("Resource " + resource + " can't be parsed, reason:" + e, e);
        } catch (IOException e) {
            throw new IllegalArgumentException("Resource " + resource + " can't be read, reason:" + e, e);
        }
    }

    /**
    * Sets the template pattern to use to generate the URL that defines the contents of this object.
    * Assume the URL is a partial one and refers to the local host to retrieve the full value.
    */
    public void setUrlTemplate(String templatePattern) {
        this.templatePattern = templatePattern;
    }

    /**
    * Parses the given URL into this object. Assumed to start with an empty
    * object, or else extend the given object.
    * 
    * @param url
    * @throws SAXException
    * @throws ParserConfigurationException
    */
    public void parseURL(URL url) throws SAXException, IOException {
        assert url != null;
        parseInputSource(new InputSource(url.openStream()));
    }

    public synchronized void parseInputSource(InputSource is) throws IOException, SAXException {
        XmlModelHandler handler = new XmlModelHandler(this);
        SAXParserFactory spf = SAXParserFactory.newInstance();
        try {
            spf.setFeature("http://xml.org/sax/features/namespaces", true);
            SAXParser sp = spf.newSAXParser();
            sp.parse(is, handler);
        } catch (ParserConfigurationException e) {
            throw new RuntimeException("Parser configuration error:" + e, e);
        }
    }

    /** Sets the parent value to inherit from. */
    public void setParent(XmlModel parent) {
        this.parent = parent;
    }

    /**
    * Return the value from this object, or from a parent if this object doesn't have the value.
    */
    @Override
    public Object get(Object key) {
        Object ret = super.get(key);
        if (ret == null && parent != null) {
            ret = parent.get(key);
            if (log.isDebugEnabled()) log.debug("Object not found, using parent value " + key + "=" + ret);
            return ret;
        }
        return ret;
    }

    /**
    * Indicates if the given key is present in this map.
    */
    @Override
    public boolean containsKey(Object key) {
        if (super.containsKey(key)) {
            return true;
        }
        if (parent != null) {
            Object value = parent.get(key);
            if (value != null) {
                if (log.isDebugEnabled()) log.debug("Contains key not found - copying it local " + key + "=" + value);
                put((String) key, value);
                return true;
            }
        }
        return false;
    }

    /**
    * Sets an XML source to use.
    * Currently only supports StreamSource, but other sources could be supported in the future.
    * @param ret
    * @throws SAXException 
    * @throws IOException 
    */
    public void setSource(Source source) throws IOException, SAXException {
        StreamSource ss = (StreamSource) source;
        Reader r = ss.getReader();
        if (r != null) {
            parseInputSource(new InputSource(r));
        } else {
            InputStream is = ss.getInputStream();
            parseInputSource(new InputSource(is));
        }
    }
}
