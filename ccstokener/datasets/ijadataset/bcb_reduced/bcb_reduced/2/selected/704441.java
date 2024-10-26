package se.hogdahls.httpd;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

/** Access Resources stored in .jar files
 * @author Johan
 */
public class ResourceLoader {

    URL url;

    String error;

    /** Read resources with all error handling built in
	 * @see exists()
	 * @param urlPath
	 */
    ResourceLoader(String urlPath) {
        url = getClass().getResource(urlPath);
        System.err.println(url.toString());
    }

    /** get a ResourceLoader
	 * @param urlPath path to open
	 * @return ResourceLoader or null
	 */
    static ResourceLoader getResourceLoader(String urlPath) {
        ResourceLoader loader = new ResourceLoader(urlPath);
        return loader.exists() ? loader : null;
    }

    /** Result of creation
	 * @return True if we got an URL
	 */
    Boolean exists() {
        return url != null;
    }

    /** Read resource
	 * @return Contents or null
	 */
    ByteBuffer getResourceAsByteBuffer() {
        if (url != null) {
            URLConnection conn;
            InputStream input;
            try {
                input = url.openStream();
                conn = url.openConnection();
            } catch (IOException e2) {
                System.err.println(error = "getResourceAsByteArray:openConnection Failed " + url.getFile());
                return null;
            }
            int size = conn.getContentLength();
            int len;
            if (input != null) {
                byte bytes[] = new byte[size];
                try {
                    len = input.read(bytes);
                    input.close();
                    if (len != size) {
                        System.err.println(error = "getResourceAsByteArray:Short read " + url.getFile() + " Expected " + size + ">" + len);
                    }
                    return ByteBuffer.wrap(bytes);
                } catch (IOException e) {
                    try {
                        input.close();
                    } catch (IOException e1) {
                    }
                    System.err.println(error = "getResourceAsByteArray:Failed reading " + url.getFile());
                }
            }
        }
        return null;
    }

    /** Read resource
	 * @return Contents or null
	 */
    CharBuffer getResourceAsCharBuffer() {
        URLConnection conn;
        InputStream input;
        try {
            input = url.openStream();
            conn = url.openConnection();
        } catch (IOException e2) {
            System.err.println(error = "getResourceAsCharBuffer:openConnection Failed " + url.getFile());
            return null;
        }
        int size = conn.getContentLength();
        if (size == -1) size = 10000;
        if (input != null) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            CharBuffer cbuf = CharBuffer.allocate(size);
            try {
                int len = reader.read(cbuf);
                reader.close();
                cbuf.limit(len);
                return cbuf;
            } catch (IOException e) {
                try {
                    reader.close();
                } catch (IOException e1) {
                }
                System.err.println(error = "getResourceAsCharBuffer:reader Failed" + url.getFile());
            }
        }
        return null;
    }

    /** Last error as string
	 * @return error or empty string
	 */
    String lastError() {
        return error != null ? error : "";
    }
}
