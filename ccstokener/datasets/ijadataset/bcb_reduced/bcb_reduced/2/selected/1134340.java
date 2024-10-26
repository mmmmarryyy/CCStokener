package uk.org.ogsadai.activity.delivery;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import org.globus.ftp.Session;
import org.globus.ftp.exception.FTPException;
import org.globus.io.streams.FTPInputStream;
import uk.org.ogsadai.activity.ActivityProcessingException;
import uk.org.ogsadai.activity.ActivityTerminatedException;
import uk.org.ogsadai.activity.ActivityUserException;
import uk.org.ogsadai.activity.io.ActivityIOException;
import uk.org.ogsadai.common.msgs.DAILogger;

/**
 * Delivers data to FTP.
 * 
 * @author The OGSA-DAI Project Team
 */
class ObtainFromFTPCore implements ObtainFromCore {

    /** Copyright statement */
    private static final String COPYRIGHT_NOTICE = "Copyright (c) The University of Edinburgh, 2007";

    /** Logger object for logging in this class */
    private static final DAILogger LOG = DAILogger.getLogger(ObtainFromFTPCore.class);

    /** Current host to use for deliveries. */
    private String mCurrentHost;

    /** Passive mode in case of FTP and GFTP delivery*/
    private boolean mPassiveMode;

    public void setHost(String host) {
        mCurrentHost = host;
    }

    public void setPassiveMode(boolean passiveMode) {
        mPassiveMode = passiveMode;
    }

    public InputStream obtain(String name) throws ActivityProcessingException, ActivityTerminatedException, ActivityUserException {
        final URL url = DeliveryUtilities.createURL("ftp", mCurrentHost, name);
        InputStream input = null;
        if (!mPassiveMode) {
            input = openInputStream(url);
        } else {
            input = createFTPInputStream(url, name);
        }
        return input;
    }

    public void close() {
    }

    /**
     * Opens an stream to read from the specified FTP URL if passive mode
     * is set to false
     * 
     * @param url
     *            URL to write to
     * @return open input stream
     * @throws ActivityUserException 
     *             if an error occurs accessing the URL
     */
    private InputStream openInputStream(final URL url) throws ActivityUserException {
        InputStream input = null;
        try {
            final URLConnection connection = url.openConnection();
            connection.setDoInput(true);
            connection.setDoOutput(false);
            input = connection.getInputStream();
            return input;
        } catch (IOException e) {
            throw new ActivityUserException(new ActivityIOException(e));
        }
    }

    /**
     * Auxiliary mehtod that establishes the FTPInputStream if passive mode is
     * set to true.
     * 
     * 
     * @param url
     *            The url whose parts will be used to create the FTPInputStream.
     * @param filename
     *            The filename whose contents should be retrieved and streamed.
     * @return The produced FTPInputStream
     * @throws ActivityUserException
     *             if an error occurs due to something specified by an end user
     */
    private FTPInputStream createFTPInputStream(URL url, String filename) throws ActivityUserException {
        String hostFromURL = url.getHost();
        int portFromURL = url.getPort();
        if (portFromURL == -1) {
            portFromURL = 21;
        }
        String userInfoFromURL = url.getUserInfo();
        String username;
        String password;
        if (userInfoFromURL == null) {
            username = null;
            password = null;
        } else {
            username = userInfoFromURL.split(":")[0];
            password = userInfoFromURL.split(":")[1];
        }
        FTPInputStream fis = null;
        try {
            fis = new FTPInputStream(hostFromURL, portFromURL, username, password, filename, true, Session.TYPE_IMAGE);
        } catch (IOException e) {
            throw new ActivityUserException(e);
        } catch (FTPException e) {
            throw new ActivityFTPException(e);
        }
        return fis;
    }
}
