package com.csam.browser.scripting;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 *
 * @author Nathan Crause <ncrause at clarkesolomou.com>
 */
public class IOUtil {

    public static String readStream(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int count;
        while ((count = in.read(buffer)) != -1) {
            out.write(buffer, 0, count);
        }
        return out.toString();
    }

    public static String readResource(String resourceName) throws IOException {
        InputStream in = IOUtil.class.getResourceAsStream(resourceName);
        assert in != null : "Unable to locate resource '" + resourceName + "'";
        return readStream(in);
    }

    public static String readURL(URL url) throws IOException {
        return readStream(url.openStream());
    }
}
