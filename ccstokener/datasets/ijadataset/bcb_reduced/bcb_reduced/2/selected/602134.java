package com.frameworkset.orm.engine.transform;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import org.apache.log4j.Logger;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * A resolver to get the database.dtd file for the XML parser from the jar.
 *
 * @author <a href="mailto:mpoeschl@marmot.at">Martin Poeschl</a>
 * @author <a href="mailto:kschrader@karmalab.org">Kurt Schrader</a>
 * @author <a href="mailto:quintonm@bellsouth.net">Quinton McCombs</a>
 * @version $Id: DTDResolver.java,v 1.12 2004/10/12 22:02:01 dlr Exp $
 */
public class DTDResolver implements EntityResolver, Serializable {

    /** Where the DTD is located on the web. */
    public static final String WEB_SITE_DTD = "http://db.apache.org/torque/dtd/database_3_2.dtd";

    /** InputSource for <code>database.dtd</code>. */
    private InputSource databaseDTD = null;

    /** Logging class from commons.logging */
    private static Logger log = Logger.getLogger(DTDResolver.class);

    /**
     * constructor
     */
    public DTDResolver() throws SAXException {
        try {
            InputStream dtdStream = getClass().getResourceAsStream("database.dtd");
            if (dtdStream != null) {
                databaseDTD = new InputSource(dtdStream);
            } else {
                log.warn("Could not locate database.dtd");
            }
        } catch (Exception ex) {
            throw new SAXException("Could not get stream for database.dtd", ex);
        }
    }

    /**
     * An implementation of the SAX <code>EntityResolver</code>
     * interface to be called by the XML parser.
     *
     * @param publicId The public identifier of the external entity
     * @param systemId The system identifier of the external entity
     * @return An <code>InputSource</code> for the
     * <code>database.dtd</code> file.
     */
    public InputSource resolveEntity(String publicId, String systemId) throws IOException {
        if (databaseDTD != null && WEB_SITE_DTD.equals(systemId)) {
            String pkg = getClass().getName().substring(0, getClass().getName().lastIndexOf('.'));
            log.info("Resolver: used database.dtd from '" + pkg + "' package");
            return databaseDTD;
        } else if (systemId == null || "".equals(systemId.trim())) {
            log.info("Resolver: used '" + WEB_SITE_DTD + '\'');
            return getInputSource(WEB_SITE_DTD);
        } else {
            log.info("Resolver: used '" + systemId + '\'');
            return getInputSource(systemId);
        }
    }

    /**
     * Retrieves a XML input source for the specified URL.
     *
     * @param urlString The URL of the input source.
     * @return <code>InputSource</code> for the URL.
     */
    private InputSource getInputSource(String urlString) throws IOException {
        URL url = new URL(urlString);
        return new InputSource(url.openStream());
    }
}
