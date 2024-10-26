package org.magnos.asset.source;

import java.io.InputStream;
import java.net.URL;
import org.magnos.asset.base.BaseAssetSource;

/**
 * A source that reads assets from a JAR file.
 * 
 * @author Philip Diffenderfer
 *
 */
public class JarSource extends BaseAssetSource {

    /**
	 * Instantiates a new JarSource.
	 * 
	 * @param url
	 * 		The URL/URI of the JAR file.
	 */
    public JarSource(String url) {
        super(null, "jar:" + url + "!/", "");
    }

    @Override
    public InputStream getStream(String request) throws Exception {
        URL url = new URL(getAbsolute(request));
        return url.openStream();
    }

    /**
	 * Instantiates a JarSource referencing a JAR file on the file-system.
	 * 
	 * @param file
	 * 		The path to the JAR file.
	 * @return
	 * 		The reference to the new JarSource.
	 */
    public static JarSource fromFile(String file) {
        return new JarSource("file:" + file);
    }

    /**
	 * Instantiates a JarSource referencing a JAR file accessed through HTTP.
	 * 
	 * @param host
	 * 		The host and path to the JAR file. This should not include the
	 * 		protocol string (http://).
	 * @return
	 * 		The reference to the new JarSource.
	 */
    public static JarSource fromHttp(String host) {
        return new JarSource("http://" + host);
    }

    /**
	 * Instantiates a JarSource referencing a JAR file accessed through HTTPS.
	 * 
	 * @param host
	 * 		The host and path to the JAR file. This should not include the
	 * 		protocol string (https://).
	 * @return
	 * 		The reference to the new JarSource.
	 */
    public static JarSource fromHttps(String host) {
        return new JarSource("https://" + host);
    }
}
