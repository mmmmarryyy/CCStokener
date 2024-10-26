package org.mbari.vars.annotation.tools;

import org.mbari.vars.annotation.model.CameraData;
import org.mbari.vars.annotation.model.dao.CameraDataDAO;
import org.mbari.vars.dao.DAOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.ResourceBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author brian
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class UpdateStillImageUrlTool {

    private static final Logger log = LoggerFactory.getLogger(UpdateStillImageUrlTool.class);

    /**
     * VARS stores images in a directory that contains this as part of it's path. The
     * tail end of the local URL and the remote URL are the same, but the starting
     * portions are different. This key is used to locate the parts of the path that
     * are the same.
     */
    public static final String SEARCH_KEY = "VARS/data";

    /** <!-- Field description --> */
    public static final byte[] PNG_KEY = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF };

    /** <!-- Field description --> */
    public static final byte[] JPG_KEY = { (byte) 0x89, (byte) 0x50, (byte) 0x4E };

    /**
     * THis is the string that gets prepended onto fiel urls to create web URLs
     */
    public static final String HTTP_PREFIX = ResourceBundle.getBundle("vars").getString("image.archive.url");

    /** <!-- Field description --> */
    public static final byte[] GIF_KEY = { (byte) 0x47, (byte) 0x49, (byte) 0x46 };

    /**
     * This is the key that is used to locate file URLS in the database.
     */
    public static final String FILE_PREFIX = "file:";

    /**
     *
     */
    private UpdateStillImageUrlTool() {
        super();
    }

    /**
     * Converts a file URL stored in a database to the coresponding http url.
     *
     * TODO gernate unit test for this method
     *
     *
     * @param fileUrl
     * @return A http URL. null if the String provided should not be converted to a
     *      URL.
     *
     * @throws MalformedURLException
     */
    public static URL fileUrlToHttpUrl(final String fileUrl) throws MalformedURLException {
        URL httpUrl = null;
        if ((fileUrl != null) && fileUrl.toLowerCase().startsWith(FILE_PREFIX)) {
            int idx = fileUrl.indexOf(SEARCH_KEY);
            if (idx > -1) {
                idx = idx + SEARCH_KEY.length() + 1;
                String httpString = HTTP_PREFIX + fileUrl.substring(idx);
                httpString = httpString.replaceAll(" ", "%20");
                httpUrl = new URL(httpString);
            }
        }
        return httpUrl;
    }

    /**
     * Searchs the VARS database for all file URLs
     *
     * @return A collection of CameraData objects whose stillImageUrl field is a
     *      file URL.
     *
     * @throws DAOException
     */
    public static Collection findFileUrls() throws DAOException {
        return CameraDataDAO.getInstance().findByStillImageUrlPrefix(FILE_PREFIX);
    }

    /**
     * Checks the web server to see if the image exists. IT does this by opening a
     * stream and reading the first 3 bytes. It checks these bytes to see if its a
     * jpg, gif or png.
     *
     * @param url
     * @return true if the image exists.
     */
    public static boolean isImageOnWebServer(final URL url) {
        boolean onServer = false;
        if (url != null) {
            byte[] b = new byte[3];
            try {
                InputStream in = url.openStream();
                in.read(b);
                in.close();
            } catch (Exception e) {
                if (log.isInfoEnabled()) {
                    log.info("Unable to open the URL, " + url, e);
                }
            }
            if (Arrays.equals(b, PNG_KEY) || Arrays.equals(b, GIF_KEY) || Arrays.equals(b, JPG_KEY)) {
                onServer = true;
            }
        }
        return onServer;
    }

    /**
     * <p><!-- Method description --></p>
     *
     *
     * @param args
     */
    public static void main(String[] args) {
        try {
            updateStillImageUrls();
        } catch (Exception e) {
            log.error("Unable to update the still image URLS.", e);
        }
    }

    /**
     * <p><!-- Method description --></p>
     *
     *
     * @throws DAOException
     */
    public static void updateStillImageUrls() throws DAOException {
        Collection cameraDatums = findFileUrls();
        for (Iterator i = cameraDatums.iterator(); i.hasNext(); ) {
            CameraData cd = (CameraData) i.next();
            try {
                updateUrl(cd);
            } catch (MalformedURLException e) {
                log.warn("Failed to update " + cd, e);
            }
        }
    }

    /**
     *
     * @param cameraData
     * @throws DAOException
     * @throws MalformedURLException
     */
    public static void updateUrl(CameraData cameraData) throws DAOException, MalformedURLException {
        if (cameraData != null) {
            URL newUrl = fileUrlToHttpUrl(cameraData.getStillImage());
            if (log.isDebugEnabled()) {
                log.debug("Attempting to update " + cameraData.getStillImage() + " to " + newUrl);
            }
            if (isImageOnWebServer(newUrl)) {
                cameraData.setStillImage(newUrl.toExternalForm());
                CameraDataDAO.getInstance().updateStillImage(cameraData);
            }
        }
    }
}
