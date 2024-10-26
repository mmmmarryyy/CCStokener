package org.stellarium.data;

import org.stellarium.StellariumException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * User: freds
 * Date: Apr 5, 2008
 * Time: 3:02:49 PM
 */
public class WebFileLoaderImpl extends AbstractFileLoader {

    private final String homeServerUrl;

    public WebFileLoaderImpl(ResourceLocatorUtil locatorUtil, String homeServerUrl) {
        super(locatorUtil);
        this.homeServerUrl = homeServerUrl;
    }

    public URL getOrLoadFile(File file) {
        if (!file.exists()) {
            File folder = file.getParentFile();
            if (!folder.exists()) {
                if (!folder.mkdirs()) {
                    throw new StellariumException("Loading file " + file + " cannot be done since folder " + folder + " cannot be created.");
                }
            }
            String extraPath = null;
            while (folder != null) {
                if (folder.equals(locatorUtil.getStellariumHome())) {
                    extraPath = file.getPath().substring(folder.getPath().length()).replace('\\', '/');
                    break;
                }
                folder = folder.getParentFile();
            }
            if (extraPath != null) {
                loadDynamically(file, extraPath);
            }
        }
        try {
            return new URL(file.toURI().toURL(), "");
        } catch (MalformedURLException e) {
            throw new StellariumException(e);
        }
    }

    private void loadDynamically(File result, String extraPath) {
        URL url = null;
        InputStream is = null;
        FileOutputStream fos = null;
        try {
            url = new URL(homeServerUrl + extraPath);
            is = url.openStream();
            fos = new FileOutputStream(result);
            byte[] buff = new byte[8192];
            int nbRead;
            while ((nbRead = is.read(buff)) > 0) fos.write(buff, 0, nbRead);
        } catch (IOException e) {
            throw new StellariumException("Cannot dynamically load " + result + " from " + url);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace(System.out);
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace(System.out);
                }
            }
        }
    }
}
