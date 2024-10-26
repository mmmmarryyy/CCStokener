package org.scopemvc.view.servlet.xml;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Properties;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.scopemvc.util.Debug;
import org.scopemvc.util.ScopeConfig;
import org.xml.sax.ContentHandler;
import org.scopemvc.view.servlet.Page;

/**
 * <P>
 *
 * A ServletView that references an XSLT URI used to transform an XML
 * representation of the View's bound model objects. </P> <P>
 *
 * The XSLT is assumed to describe the entire view, not just a part of the
 * overall page, even when this view is a subview or a parent of subviews. </P>
 * <P>
 *
 * Model objects are turned to a SAX source in a concrete subclass, which is
 * transformed with the XSLT to a SAX stream that gets fed to a SAX
 * ContentHandler (eg an HTML serializer) that writes to an OutputStream (eg the
 * HTTPResponse's output stream). </P> <P>
 *
 * This abstract base class does some generic XSLT handling, including caching
 * compiled stylesheets. The concrete Scope impl is in {@link XSLPage}. </P>
 *
 * @author <A HREF="mailto:smeyfroi@users.sourceforge.net">Steve Meyfroidt</A>
 * @version $Revision: 1.11 $ $Date: 2002/11/20 00:19:57 $
 * @created 05 September 2002
 */
public abstract class AbstractXSLPage extends Page {

    /**
     * A cache for compiled stylesheets.
     */
    protected static HashMap templateCache = new HashMap();

    private static final Log LOG = LogFactory.getLog(AbstractXSLPage.class);

    /**
     * Base URI to reference XSL URIs from.
     */
    private static String systemID;

    /**
     * Do we want to cache the stylesheets? Yes for production, no for
     * development because you want to see changes to the XSLTs as soon as you
     * make them rather than after restarting the web application.
     */
    protected boolean shouldCacheTemplates;

    /**
     * URI of the XSLT for this View relative to the {@link #setSystemID}. The
     * XSLT is assumed to be for the whole page, even if this view is a subview
     * of a parent.
     */
    protected String xslURI;

    /**
     * Filesystem directory to dump all XML before transforming. Useful for
     * debug and development but disable in production!
     */
    protected String debugXMLDirectory;

    /**
     * Specify the XSLT to use when showing this View. The XSLT is assumed to be
     * for the whole page, even if this view is a subview or a parent of some
     * subviews.
     *
     * @param inXslURI a URI to the XSLT relative to what has been set in {@link
     *      #setSystemID}
     * @param inViewID TODO: Describe the Parameter
     */
    public AbstractXSLPage(String inViewID, String inXslURI) {
        super(inViewID);
        if (LOG.isDebugEnabled()) {
            LOG.debug("XslURI: " + inXslURI);
        }
        setXslURI(inXslURI);
        if (ScopeConfig.getString("org.scopemvc.view.servlet.xml.AbstractXSLPage.shouldCacheTemplates").equals("1")) {
            shouldCacheTemplates = true;
        } else {
            shouldCacheTemplates = false;
        }
        debugXMLDirectory = ScopeConfig.getString("org.scopemvc.view.servlet.xml.AbstractXSLPage.debugXMLDirectory");
        if (debugXMLDirectory.length() < 1) {
            debugXMLDirectory = null;
        }
    }

    /**
     * Where XSLT URIs are referenced relative to. See
     * javax.xml.transform.sax.TransformerHandler
     *
     * @return The systemID value
     */
    public static String getSystemID() {
        if (systemID == null) {
            return "";
        }
        return systemID;
    }

    /**
     * Where XSLT URIs are referenced relative to. See
     * javax.xml.transform.sax.TransformerHandler
     *
     * @param inSystemID The new systemID value
     */
    public static void setSystemID(String inSystemID) {
        systemID = inSystemID;
    }

    /**
     * The XSLT that will be shown for this view. (If null then the default
     * "pass-through" XSL is used.)
     *
     * @return The xslURI value
     */
    public String getXslURI() {
        return xslURI;
    }

    /**
     * Allow subclasses to deliver different content types. Here "text/html"
     * unless no stylesheet in which case "text/xml".
     *
     * @return The contentType value
     */
    public String getContentType() {
        if (getXslURI() == null) {
            return "text/xml";
        } else {
            return "text/html";
        }
    }

    /**
     * The XSLT that will be shown for this view. (If null then the default
     * "pass-through" XSL is used.)
     *
     * @param inURI The new xslURI value
     */
    public void setXslURI(String inURI) {
        xslURI = inURI;
    }

    /**
     * For debug.
     *
     * @return TODO: Describe the Return Value
     */
    public String toString() {
        String result = "(" + getClass().getName() + ":" + getID() + "," + getSystemID() + "," + getXslURI();
        result += ")";
        return result;
    }

    /**
     * <P>
     *
     * Stream the view by calling {@link #generateXMLDocument} and processing
     * the result with the XSLT. </P>
     *
     * @param inOutputStream Stream the result of the XSLT processing into here.
     * @throws Exception TODO: Describe the Exception
     */
    public void streamView(OutputStream inOutputStream) throws Exception {
        if (debugXMLDirectory != null) {
            try {
                File file = new File(debugXMLDirectory + getXslURI() + ".xml");
                File directory = file.getParentFile();
                directory.mkdirs();
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                generateXMLDocument(makeSerializer(fileOutputStream, getXMLOutputProperties()));
                fileOutputStream.flush();
                fileOutputStream.close();
            } catch (IOException e) {
                LOG.warn("streamView: can't write XML to file: ", e);
            }
        }
        TransformerHandler transformerHandler = getTransformerHandler(inOutputStream);
        long time0 = System.currentTimeMillis();
        generateXMLDocument(transformerHandler);
        if (LOG.isInfoEnabled()) {
            LOG.info("streamView: process XML: time: " + (System.currentTimeMillis() - time0));
        }
    }

    /**
     * Make Properties for <code>Transformer.setOutputProperties</code> suitable
     * for debug XML output.
     *
     * @return The xMLOutputProperties value
     * @see javax.xml.transform.OutputKeys
     */
    protected Properties getXMLOutputProperties() {
        Properties oprops = new Properties();
        oprops.put(OutputKeys.METHOD, "xml");
        oprops.put(OutputKeys.INDENT, "2");
        return oprops;
    }

    /**
     * Make Properties for <code>Transformer.setOutputProperties</code> suitable
     * for final HTML output.
     *
     * @return The hTMLOutputProperties value
     * @see javax.xml.transform.OutputKeys
     */
    protected Properties getHTMLOutputProperties() {
        Properties oprops = new Properties();
        oprops.put(OutputKeys.METHOD, "html");
        oprops.put(OutputKeys.INDENT, "2");
        return oprops;
    }

    /**
     * Thread-safe Templates are cached for reuse to avoid parsing and compiling
     * stylesheets repeatedly.
     *
     * @param inOutputStream TODO: Describe the Parameter
     * @return The transformerHandler value
     * @throws Exception TODO: Describe the Exception
     */
    protected TransformerHandler getTransformerHandler(OutputStream inOutputStream) throws Exception {
        long time0 = System.currentTimeMillis();
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        if (!transformerFactory.getFeature(javax.xml.transform.sax.SAXResult.FEATURE)) {
            throw new UnsupportedOperationException("Can't use AbstractXSLPage with an XSL Processor that can't output to SAX.");
        }
        if (!transformerFactory.getFeature(javax.xml.transform.sax.SAXSource.FEATURE)) {
            throw new UnsupportedOperationException("Can't use AbstractXSLPage with an XSL Processor that can't take SAX input.");
        }
        SAXTransformerFactory saxTransformerFactory = (SAXTransformerFactory) transformerFactory;
        TransformerHandler transformerHandler = null;
        if (getXslURI() == null) {
            transformerHandler = saxTransformerFactory.newTransformerHandler();
        } else {
            Object o;
            synchronized (templateCache) {
                o = templateCache.get(getXslURI());
            }
            Templates template = null;
            if (o != null) {
                if (Debug.ON) {
                    Debug.assertTrue(o instanceof Templates);
                }
                template = (Templates) o;
            } else {
                String urlPath = getSystemID() + getXslURI();
                URL url = new URL(urlPath);
                if (LOG.isInfoEnabled()) {
                    LOG.info("XSL URL: " + url);
                }
                InputStream stream = url.openStream();
                int fileIndex = urlPath.lastIndexOf('/') + 1;
                String baseURI = "";
                if (fileIndex >= 0) {
                    baseURI = urlPath.substring(0, fileIndex);
                }
                if (LOG.isDebugEnabled()) {
                    LOG.debug("getTransformerHandler: baseURI: " + baseURI + "urlPath: " + urlPath);
                }
                StreamSource source = new StreamSource(stream, baseURI);
                template = saxTransformerFactory.newTemplates(source);
                if (shouldCacheTemplates) {
                    synchronized (templateCache) {
                        templateCache.put(getXslURI(), template);
                    }
                }
            }
            if (LOG.isInfoEnabled()) {
                LOG.info("getTransformerHandler: get compiled XSL: time: " + (System.currentTimeMillis() - time0));
            }
            if (Debug.ON) {
                Debug.assertTrue(template != null, "null template");
            }
            transformerHandler = saxTransformerFactory.newTransformerHandler(template);
        }
        if (Debug.ON) {
            Debug.assertTrue(inOutputStream != null, "null output stream");
        }
        ContentHandler contentHandler;
        if (getXslURI() == null) {
            contentHandler = makeSerializer(inOutputStream, getXMLOutputProperties());
        } else {
            contentHandler = makeSerializer(inOutputStream, getHTMLOutputProperties());
        }
        Result result = new SAXResult(contentHandler);
        transformerHandler.setResult(result);
        return transformerHandler;
    }

    /**
     * Make a null transformer to take SAX input and stream it to an
     * OutputStream using the passed OutputProperties.
     *
     * @param inStream TODO: Describe the Parameter
     * @param inOutputProperties TODO: Describe the Parameter
     * @return ContentHandler around a null Transformer
     * @see #getXMLOutputProperties()
     * @see #getHTMLOutputProperties()
     */
    protected ContentHandler makeSerializer(OutputStream inStream, Properties inOutputProperties) {
        try {
            TransformerFactory tfactory = TransformerFactory.newInstance();
            if (tfactory.getFeature(SAXSource.FEATURE)) {
                SAXTransformerFactory stfactory = ((SAXTransformerFactory) tfactory);
                TransformerHandler handler = stfactory.newTransformerHandler();
                Transformer serializer = handler.getTransformer();
                serializer.setOutputProperties(inOutputProperties);
                Result result = new StreamResult(inStream);
                handler.setResult(result);
                return handler;
            } else {
                LOG.fatal("TransformerFactory doesn't support SAXSource");
                throw new UnsupportedOperationException("TransformerFactory doesn't support SAXSource");
            }
        } catch (TransformerConfigurationException e) {
            LOG.fatal("TransformerConfigurationException", e);
            throw new UnsupportedOperationException("TransformerConfigurationException: " + e);
        }
    }

    /**
     * Override to implement model to SAX conversion into the passed
     * ContentHandler.
     *
     * @param inContentHandler TODO: Describe the Parameter
     * @throws Exception TODO: Describe the Exception
     */
    protected abstract void generateXMLDocument(ContentHandler inContentHandler) throws Exception;
}
