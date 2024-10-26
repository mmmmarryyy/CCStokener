package es.eucm.eadventure.common.auxiliar.zipurl;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.io.FileInputStream;

/**
 * @author Ca�izal, G., Del Blanco, A., Torrente, F.J. (alphabetical order) *
 * @author L�pez Ma�as, E., P�rez Padilla, F., Sollet, E., Torijano, B. (former
 *         developers by alphabetical order)
 * 
 */
public class ZipURLConnection extends URLConnection {

    private String assetPath;

    private String zipFile;

    /**
     * @param url
     * @throws MalformedURLException
     */
    public ZipURLConnection(URL assetURL, String zipFile, String assetPath) {
        super(assetURL);
        this.assetPath = assetPath;
        this.zipFile = zipFile;
    }

    /**
     * @param url
     * @throws MalformedURLException
     */
    public ZipURLConnection(URL assetURL, String assetPath) {
        super(assetURL);
        this.assetPath = assetPath;
        zipFile = null;
    }

    @Override
    public void connect() throws IOException {
    }

    @Override
    public InputStream getInputStream() {
        if (assetPath != null) {
            return buildInputStream();
        } else {
            try {
                return url.openStream();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    private InputStream buildInputStream() {
        try {
            if (zipFile != null) {
                return new FileInputStream(zipFile + "/" + assetPath);
            } else {
                return new FileInputStream(assetPath);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }
}
