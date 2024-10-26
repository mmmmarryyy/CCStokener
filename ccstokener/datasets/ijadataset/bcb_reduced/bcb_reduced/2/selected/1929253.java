package uk.org.ogsadai.activity.delivery;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import uk.org.ogsadai.activity.ActivityProcessingException;
import uk.org.ogsadai.activity.ActivityTerminatedException;
import uk.org.ogsadai.activity.ActivityUserException;
import uk.org.ogsadai.activity.io.ActivityIOException;
import uk.org.ogsadai.common.msgs.DAILogger;

/**
 * Obtains data from HTTP.
 * 
 * @author The OGSA-DAI Project Team
 */
class ObtainFromHTTPCore implements ObtainFromCore {

    /** Copyright statement */
    private static final String COPYRIGHT_NOTICE = "Copyright (c) The University of Edinburgh, 2007.";

    /** Current host to use for deliveries. */
    private String mCurrentHost;

    /** Logger object for logging in this class */
    private static final DAILogger LOG = DAILogger.getLogger(ObtainFromHTTPCore.class);

    public void setHost(String host) {
        mCurrentHost = host;
    }

    public void setPassiveMode(boolean passiveMode) {
    }

    public InputStream obtain(String filename) throws ActivityProcessingException, ActivityTerminatedException, ActivityUserException {
        final URL url = DeliveryUtilities.createURL("http", mCurrentHost, filename);
        URLConnection connect;
        InputStream stream = null;
        try {
            connect = url.openConnection();
            connect.connect();
            stream = connect.getInputStream();
            return stream;
        } catch (IOException e) {
            throw new ActivityIOException(e);
        }
    }

    public void close() {
    }
}
