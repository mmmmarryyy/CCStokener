package doors.util;

import java.io.InputStream;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

/**
 * The doors.dtd entity resolver.
 * 
 * @author Adam Buckley
 */
public class DtdResolver implements EntityResolver {

    private final Logger logger = Logger.getLogger("DtdResolver");

    /**
	 * This class resolves systemId <code>http://doors.sourceforge.net/doors.dtd</code>
	 * as <code>/doors.dtd</code>, where / is this.class's root directory or
	 * jar.
	 * 
	 * @return null if /doors.dtd cannot be found or an error occurs.
	 */
    public InputSource resolveEntity(String publicId, String systemId) {
        InputSource r = null;
        logger.fine("Resolver: resolveEntity(\"" + publicId + "\", \"" + systemId + "\")");
        if (systemId.equals("http://doors.sourceforge.net/doors.dtd")) {
            String resourceName = DtdResolver.class.getResource("") + "doors.dtd";
            URL url = DtdResolver.class.getResource("/doors.dtd");
            logger.fine("Resolver: Trying to resolve " + systemId);
            logger.fine("Resolver: Local URL is " + url);
            if (url == null) {
                logger.severe("Resolver: Cannot find doors.dtd as " + resourceName);
            } else {
                try {
                    InputStream is = url.openStream();
                    r = new InputSource(is);
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "When trying to open '" + resourceName + "'", e);
                }
            }
        }
        return r;
    }
}
