package com.almilli.catnip.server;

import com.almilli.catnip.logging.Log;
import com.almilli.catnip.logging.LogFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import org.apache.catalina.startup.ContextConfig;
import org.apache.catalina.startup.DigesterFactory;
import org.apache.tomcat.util.digester.Digester;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

/**
 * The <code>HmeConfig</code> class extends a ContextConfig and will also load the hme.xml file
 * when loading application config files.
 * @author s2kdave
 */
public class HmeConfig extends ContextConfig {

    private static final Log log = LogFactory.getLog(HmeConfig.class);

    private static final String APPLICATION_HME_XML = "/WEB-INF/hme.xml";

    /**
     * The <code>Digester</code> we will use to process web application
     * deployment descriptor files.
     */
    protected static Digester hmeDigester = null;

    protected static HmeRuleSet hmeRuleSet = new HmeRuleSet();

    /**
     * Create (if necessary) and return a Digester configured to process the
     * web application deployment descriptor (web.xml).
     */
    protected static Digester createHmeDigester(boolean xmlValidation, boolean xmlNamespaceAware) {
        Digester hmeDigester = DigesterFactory.newDigester(xmlValidation, xmlNamespaceAware, hmeRuleSet);
        return hmeDigester;
    }

    @Override
    protected void applicationWebConfig() {
        super.applicationWebConfig();
        applicationHmeConfig();
    }

    protected void applicationHmeConfig() {
        URL url = null;
        InputStream stream = null;
        try {
            url = context.getServletContext().getResource(APPLICATION_HME_XML);
            if (url != null) {
                stream = url.openStream();
            }
        } catch (MalformedURLException e1) {
        } catch (IOException e) {
        }
        if (stream != null) {
            if (log.isInfoEnabled()) {
                log.info("Adding HME Applications: " + url);
            }
            if (hmeDigester == null) {
                hmeDigester = createHmeDigester(xmlValidation, xmlNamespaceAware);
            }
            synchronized (hmeDigester) {
                try {
                    InputSource is = new InputSource(url.toExternalForm());
                    is.setByteStream(stream);
                    hmeDigester.push(context);
                    ContextErrorHandler errorHandler = new ContextErrorHandler();
                    hmeDigester.setErrorHandler(errorHandler);
                    if (log.isDebugEnabled()) {
                        log.debug("Parsing application web.xml file at " + url.toExternalForm());
                    }
                    hmeDigester.parse(is);
                    if (errorHandler.parseException != null) {
                        ok = false;
                    }
                } catch (SAXParseException e) {
                    log.error(sm.getString("contextConfig.applicationParse", url.toExternalForm()), e);
                    log.error(sm.getString("contextConfig.applicationPosition", "" + e.getLineNumber(), "" + e.getColumnNumber()));
                    ok = false;
                } catch (Exception e) {
                    log.error(sm.getString("contextConfig.applicationParse", url.toExternalForm()), e);
                    ok = false;
                } finally {
                    hmeDigester.reset();
                    try {
                        if (stream != null) {
                            stream.close();
                        }
                    } catch (IOException e) {
                        log.error(sm.getString("contextConfig.applicationClose"), e);
                    }
                }
            }
        }
    }

    protected class ContextErrorHandler implements ErrorHandler {

        protected SAXParseException parseException;

        public void error(SAXParseException exception) {
            parseException = exception;
        }

        public void fatalError(SAXParseException exception) {
            parseException = exception;
        }

        public void warning(SAXParseException exception) {
            parseException = exception;
        }
    }
}
