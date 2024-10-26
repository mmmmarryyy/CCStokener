package uk.org.ogsadai.activity.delivery;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import org.globus.ftp.Session;
import org.globus.ftp.exception.FTPException;
import org.globus.io.streams.FTPOutputStream;
import uk.org.ogsadai.activity.ActivityProcessingException;
import uk.org.ogsadai.activity.ActivityTerminatedException;
import uk.org.ogsadai.activity.ActivityUserException;
import uk.org.ogsadai.activity.io.ActivityIOException;
import uk.org.ogsadai.common.msgs.DAILogger;
import uk.org.ogsadai.util.IOUtilities;

/**
 * Delivers data to FTP.
 * 
 * @author The OGSA-DAI Project Team
 */
class DeliverToFTPCore implements DeliverToCore {

    /** Copyright statement */
    private static final String COPYRIGHT_NOTICE = "Copyright (c) The University of Edinburgh, 2007";

    /** Logger object for logging in this class */
    private static final DAILogger LOG = DAILogger.getLogger(DeliverToFTPCore.class);

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

    public void setAppend(boolean append) {
    }

    public void deliver(final String name, final InputStream data) throws ActivityProcessingException, ActivityTerminatedException, ActivityUserException {
        final URL url = DeliveryUtilities.createURL("ftp", mCurrentHost, name);
        OutputStream output = null;
        try {
            if (!mPassiveMode) {
                output = openOutputStream(url);
            } else {
                output = createFTPOutputStream(url, name);
            }
            IOUtilities.streamData(data, output);
        } catch (IOException e) {
            throw new ActivityIOException(e);
        } finally {
            try {
                if (output != null) {
                    output.close();
                }
                data.close();
            } catch (IOException e) {
                LOG.warn(new ActivityIOException(e));
            }
        }
    }

    public void close() {
    }

    /**
     * Auxiliary method to set up an output stream in case the passive mode is
     * true.
     * 
     * @param url
     *            The url of the server where data will be delivered to.
     * @param filename
     *            The filename where data will be delivered to.
     * @return the produced FTPOutputStream
     * @throws ActivityUserException
     *             if an error occurs whilst interacting with the FTP server
     */
    private FTPOutputStream createFTPOutputStream(URL url, String filename) throws ActivityUserException {
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
        FTPOutputStream fous = null;
        try {
            fous = new FTPOutputStream(hostFromURL, portFromURL, username, password, filename, false, true, Session.TYPE_IMAGE);
        } catch (IOException e) {
            throw new ActivityUserException(e);
        } catch (FTPException e) {
            throw new ActivityFTPException(e);
        }
        return fous;
    }

    /**
     * Opens an output stream to write to the specified FTP URL. It is used in
     * case the passive mode is false.
     * 
     * @param url
     *            URL to write to
     * @return open output stream
     * @throws ActivityUserException
     *             if an error occurs accessing the URL
     */
    private OutputStream openOutputStream(final URL url) throws ActivityUserException {
        OutputStream output = null;
        try {
            final URLConnection connection = url.openConnection();
            connection.setDoInput(false);
            connection.setDoOutput(true);
            output = connection.getOutputStream();
        } catch (IOException e) {
            throw new ActivityUserException(e);
        }
        return output;
    }
}
