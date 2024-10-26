package provider;

import java.io.BufferedInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class WebPageDownloader {

    public static String get(String url) throws IOException {
        return get(new URL(url));
    }

    public static String get(URL url) throws IOException {
        if (url == null) {
            throw new IllegalArgumentException("URI cannot be null.");
        }
        InputStream is = url.openStream();
        DataInputStream dis = new DataInputStream(new BufferedInputStream(is));
        return getString(dis);
    }

    private static String getString(DataInput input) throws IOException {
        String result = null;
        if (input != null) {
            StringBuilder strBuilder = new StringBuilder();
            String line = null;
            line = input.readLine();
            while (line != null) {
                strBuilder.append(line);
                line = input.readLine();
            }
            result = strBuilder.toString();
        }
        return result;
    }
}
