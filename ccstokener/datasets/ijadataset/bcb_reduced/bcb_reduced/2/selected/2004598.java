package org.springframework.richclient.swing.image;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A URL protocol handler that resolves images from an ImageSource.
 * <p>
 * The syntax of an "image:" URL is: <code>image:{imageKey}</code>
 * 
 * @author oliverh
 */
public class Handler extends URLStreamHandler {

    private static final Log logger = LogFactory.getLog(Handler.class);

    private static ImageSource urlHandlerImageSource;

    /**
     * Installs this class as a handler for the "image:" protocol. Images will
     * be resolved from the provided image source.
     */
    public static void installImageUrlHandler(ImageSource urlHandlerImageSource) {
        Assert.notNull(urlHandlerImageSource);
        Handler.urlHandlerImageSource = urlHandlerImageSource;
        try {
            String packagePrefixList = System.getProperty("java.protocol.handler.pkgs");
            if (packagePrefixList != "") {
                packagePrefixList = packagePrefixList + "|";
            }
            packagePrefixList = packagePrefixList + "org.springframework.richclient";
            System.setProperty("java.protocol.handler.pkgs", packagePrefixList);
        } catch (SecurityException e) {
            logger.warn("Unable to install image URL handler", e);
            Handler.urlHandlerImageSource = null;
        }
    }

    /**
     * Creates an instance of <code>Handler</code>.
     */
    public Handler() {
    }

    protected URLConnection openConnection(URL url) throws IOException {
        if (!StringUtils.hasText(url.getPath())) {
            throw new MalformedURLException("must provide an image key.");
        } else if (StringUtils.hasText(url.getHost())) {
            throw new MalformedURLException("host part should be empty.");
        } else if (url.getPort() != -1) {
            throw new MalformedURLException("port part should be empty.");
        } else if (StringUtils.hasText(url.getQuery())) {
            throw new MalformedURLException("query part should be empty.");
        } else if (StringUtils.hasText(url.getRef())) {
            throw new MalformedURLException("ref part should be empty.");
        } else if (StringUtils.hasText(url.getUserInfo())) {
            throw new MalformedURLException("user info part should be empty.");
        }
        urlHandlerImageSource.getImage(url.getPath());
        Resource image = urlHandlerImageSource.getImageResource(url.getPath());
        if (image != null) {
            return image.getURL().openConnection();
        } else {
            throw new IOException("null image returned for key [" + url.getFile() + "].");
        }
    }
}
