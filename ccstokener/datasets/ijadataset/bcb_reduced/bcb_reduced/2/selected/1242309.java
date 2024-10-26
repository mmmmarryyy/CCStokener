package org.twdata.pipeline.stage;

import java.io.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.HashSet;
import java.util.Set;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.sax.*;
import javax.xml.transform.stream.*;
import org.apache.commons.logging.*;
import org.apache.commons.logging.*;
import org.twdata.pipeline.*;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;

/**
 *  Transforms XML SAX events either with a locally configured XSL file, or by
 *  processing the client-side XSL processing instruction on the XML stream.
 */
public class TraxTransformer extends AbstractTransformer {

    protected static final Log log = LogFactory.getLog(TraxTransformer.class);

    protected SAXTransformerFactory factory;

    protected TemplatesManager manager;

    protected TransformerHandler handler;

    protected XMLConsumer consumer;

    protected PipelineContext ctx;

    protected String xslPath;

    protected URIResolver uriResolver;

    /**
     *  Sets the templates manager to retrieve compiled XSL files
     *
     *@param  manager  The new templatesManager
     */
    public void setTemplatesManager(TemplatesManager manager) {
        this.manager = manager;
    }

    /**
     *  Sets the xslPath attribute of the TraxTransformer object
     *
     *@param  path  The new xslPath value
     */
    public void setXslPath(String path) {
        this.xslPath = path;
    }

    /**  Recycles instance */
    public void recycle() {
        consumer = null;
        ctx = null;
        handler = null;
        xslPath = null;
        uriResolver = null;
    }

    /**
     *  Sets up the local xsl if configured
     *
     *@param  ctx                    The context
     *@exception  PipelineException  If anything goes wrong
     */
    public void setup(PipelineContext ctx) throws PipelineException {
        super.setup(ctx);
        if (factory == null) {
            try {
                factory = (SAXTransformerFactory) TransformerFactory.newInstance();
            } catch (Exception ex) {
                throw new PipelineException(ex);
            }
        }
        this.ctx = ctx;
        final SourceResolver sourceResolver = ctx.getSourceResolver();
        if (sourceResolver != null) {
            uriResolver = new URIResolver() {

                public Source resolve(String base, String href) {
                    try {
                        URL url = sourceResolver.resolve(href);
                        if (url != null) {
                            return new StreamSource(url.openStream(), url.toExternalForm());
                        }
                    } catch (Exception ex) {
                        log.warn("Unable to resolve url " + href);
                    }
                    return null;
                }
            };
            if (manager.getSourceResolver() == null) {
                manager.setSourceResolver(sourceResolver);
            }
        }
        if (xslPath != null) {
            handler = getHandler(xslPath);
            javax.xml.transform.Transformer trans = handler.getTransformer();
            if (uriResolver != null) {
                trans.setURIResolver(uriResolver);
            }
            trans.setErrorListener(new TraxErrorListener());
            String output = trans.getOutputProperty("method");
            if (!"xml".equals(output)) {
                throw new IllegalArgumentException("The output property of the local xsl stylesheet must be xml");
            }
            contentHandler = handler;
            lexicalHandler = handler;
        }
    }

    /**
     *  Sets the consumer to publish SAX events to
     *
     *@param  consumer               The new consumer value
     *@exception  PipelineException  If anything goes wrong
     */
    public void setConsumer(XMLConsumer consumer) throws PipelineException {
        if (handler != null) {
            SAXResult result = new SAXResult(consumer);
            result.setLexicalHandler(consumer);
            handler.setResult(result);
        } else {
            super.setConsumer(consumer);
            this.consumer = consumer;
        }
    }

    /**
     *  Receive notification of a processing instruction. If it is <code>xml-styleshset</code>
     *  , then a transformer is setup and the transformation is inserted into
     *  the pipeline.
     *
     *@param  target            The processing instruction target.
     *@param  data              The processing instruction data, or null if none
     *      was supplied.
     *@exception  SAXException  If anything goes wrong
     */
    public void processingInstruction(String target, String data) throws SAXException {
        if (target != null && "xml-stylesheet".equals(target)) {
            if (data.indexOf("text/xsl") > -1) {
                int start = data.indexOf("href=\"") + 6;
                String xsl = data.substring(start, data.indexOf("\"", start));
                try {
                    URL url = new URL(ctx.getSource().getSystemId());
                    URL xslUrl = new URL(url, xsl);
                    handler = getHandler(xslUrl.toString());
                } catch (Exception ex) {
                    log.error(ex, ex);
                }
                javax.xml.transform.Transformer trans = handler.getTransformer();
                trans.setErrorListener(new TraxErrorListener());
                if (uriResolver != null) {
                    trans.setURIResolver(uriResolver);
                }
                handler.startDocument();
                String output = trans.getOutputProperty("method");
                SAXResult result = new SAXResult(contentHandler);
                result.setLexicalHandler(lexicalHandler);
                handler.setResult(result);
                contentHandler = handler;
                lexicalHandler = handler;
                return;
            }
        }
        if (contentHandler != null) {
            contentHandler.processingInstruction(target, data);
        }
    }

    protected TransformerHandler getHandler(String path) throws PipelineException {
        Templates templates = manager.getTemplates(factory, path);
        try {
            if (templates == null) {
                throw new TransformerException("Unable to retrieve template " + path);
            }
            return factory.newTransformerHandler(templates);
        } catch (Exception ex) {
            throw new PipelineException(ex);
        }
    }
}
