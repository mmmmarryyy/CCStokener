package net.sunji.spring.plus.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import javax.servlet.http.HttpServletRequest;

/**
 * @author seyoung
 *
 */
public class URLUtils {

    /**
	 * Creates a new instance of URLUtil
	 */
    public URLUtils() {
        super();
    }

    public static String getAbsoluteServerName(HttpServletRequest request) throws MalformedURLException {
        StringBuffer path = new StringBuffer();
        URL url = new URL(request.getRequestURL().toString());
        path.append(url.getProtocol() + "://");
        path.append(url.getHost());
        path.append(("80".equals(url.getPort()) ? "" : ":" + url.getPort()));
        return path.toString();
    }

    public static String getAbsoluteContextPath(HttpServletRequest request) throws MalformedURLException {
        StringBuffer path = new StringBuffer();
        path.append(getAbsoluteServerName(request));
        path.append(request.getContextPath());
        return path.toString();
    }

    public static boolean isURL(String str) {
        return str.contains("/") || str.contains("\\");
    }

    public static boolean isBroken(String str) {
        File file = new File(str);
        boolean ret = (file.isFile() || file.isDirectory());
        file = null;
        return !ret;
    }

    /**
	 * Test(s) if the URI is broken or not<br>
	 * If the String is already read use the isBroken(String) method because all
	 * this method does is decodes the<br>
	 * uri path and calls isBroken(String)<br>
	 */
    public static boolean isBroken(URI uri) {
        return isBroken(URLDecoder.decode(uri.getPath()));
    }

    public static boolean isBroken(File file) {
        return isBroken(file.getPath());
    }

    /**
	 * Method description
	 *
	 *
	 * @param url
	 * @param file
	 */
    public static void copy(URL url, File file) throws IOException {
        InputStream inputStream = new BufferedInputStream(url.openStream());
        int count;
        OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file));
        byte[] b = new byte[1024];
        try {
            while ((count = inputStream.read(b, 0, 1024)) > 0) {
                outputStream.write(b, 0, count);
            }
        } finally {
            inputStream.close();
            outputStream.close();
        }
    }

    /**
	 * Create a temp file for opying the contents of the URL into. This temp file
	 * will have the same extension as the URL
	 */
    public static File urlToTempFile(URL url) throws IOException {
        String externalForm = url.toExternalForm();
        String[] pathParts = externalForm.split("/");
        String name = pathParts[pathParts.length - 1];
        String[] nameParts = name.split("\\.");
        String ext = nameParts[nameParts.length - 1];
        if (nameParts.length <= 1) {
            ext = "";
        }
        return File.createTempFile("temp", ext);
    }

    public static File copyToTempFile(URL url) throws IOException {
        File tmpFile = urlToTempFile(url);
        copy(url, tmpFile);
        return tmpFile;
    }

    /**
	 * Method description
	 *
	 *
	 * @param in
	 * @param chunkSize
	 *
	 * @return
	 *
	 * @throws java.io.IOException
	 */
    public static byte[] loadBytesFromStream(InputStream in, int chunkSize) throws IOException {
        if (chunkSize < 1) {
            chunkSize = 1024;
        }
        int count;
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] b = new byte[chunkSize];
        byte[] byteArray = null;
        try {
            while ((count = in.read(b, 0, chunkSize)) > 0) {
                bo.write(b, 0, count);
            }
            byteArray = bo.toByteArray();
        } finally {
            bo.close();
        }
        return byteArray;
    }
}
