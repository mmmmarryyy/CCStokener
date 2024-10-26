package org.ujac.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * Name: HttpResourceLoader<br>
 * Description: A resource loader which loads resources from network using the http protocol.
 * <br>Log: $Log$
 * <br>Log: Revision 1.4  2004/12/07 19:17:17  lauerc
 * <br>Log: At method loadResource: Enhanced URL detection.
 * <br>Log:
 * <br>Log: Revision 1.3  2004/12/07 18:32:59  lauerc
 * <br>Log: At method loadResource: Fixed stream handling.
 * <br>Log:
 * <br>Log: Revision 1.2  2004/12/07 18:27:10  lauerc
 * <br>Log: At method loadResource: Improved connection handling.
 * <br>Log:
 * <br>Log: Revision 1.1  2004/12/06 23:23:24  lauerc
 * <br>Log: Initial revision.
 * <br>Log:
 * @author $Author: lauerc $
 * @version $Revision: 1944 $
 */
public class HttpResourceLoader implements ResourceLoader {

    /** The URL root. */
    private String urlRoot = null;

    /**
   * Constructs a HttpResourceLoader instance with no specific attributes.
   */
    public HttpResourceLoader() {
    }

    /**
   * Constructs a HttpResourceLoader instance with specific attributes.
   * @param urlRoot The root location.
   */
    public HttpResourceLoader(String urlRoot) {
        this.urlRoot = urlRoot;
    }

    /**
   * @see org.ujac.util.ResourceLoader#loadResource(java.lang.String)
   */
    public byte[] loadResource(String location) throws IOException {
        if ((location == null) || (location.length() == 0)) {
            throw new IOException("The given resource location must not be null and non empty.");
        }
        URL url = null;
        if (urlRoot == null) {
            url = new URL(location);
        } else {
            int firstColonIdx = location.indexOf(':');
            int firstSlashIdx = location.indexOf('/');
            if ((firstColonIdx > 0) && ((firstSlashIdx < 0) || (firstColonIdx < firstSlashIdx))) {
                url = new URL(location);
            } else {
                url = new URL(urlRoot + location);
            }
        }
        URLConnection cxn = url.openConnection();
        InputStream is = null;
        try {
            byte[] byteBuffer = new byte[2048];
            ByteArrayOutputStream bos = new ByteArrayOutputStream(2048);
            is = cxn.getInputStream();
            int bytesRead = 0;
            while ((bytesRead = is.read(byteBuffer, 0, 2048)) >= 0) {
                bos.write(byteBuffer, 0, bytesRead);
            }
            return bos.toByteArray();
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }
}
