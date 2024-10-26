package org.ministone.portal.helper;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @author sunwj
 * @version $Id: HttpGenerateFileHelper.java,v 1.1 2007/06/08 15:31:45 swenker Exp $version $date $time $commiter Exp $
 * @since 2007-1-2
 *        <p/>
 *        generate via http connection
 */
public class HttpGenerateFileHelper {

    public static HttpGenerateFileHelper instance() {
        return new HttpGenerateFileHelper();
    }

    private HttpGenerateFileHelper() {
    }

    /**
     * generate file using the content supplied by the given uri and saved as target file.
     * @param uri
     * @param target
     * */
    public void generate(String urlString, String target) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        BufferedInputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());
        BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(target));
        byte[] buf = new byte[10 * 1024];
        int len;
        while ((len = inputStream.read(buf, 0, buf.length)) != -1) {
            outputStream.write(buf, 0, len);
        }
        inputStream.close();
        outputStream.close();
        urlConnection.disconnect();
    }
}
