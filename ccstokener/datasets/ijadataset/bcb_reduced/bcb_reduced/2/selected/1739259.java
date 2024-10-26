package org.foo.didl.test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class MyDidlUtils {

    public static String fetch(String reference) throws IOException {
        URL url = new URL(reference);
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        c.setRequestMethod("GET");
        c.setDoOutput(true);
        c.setRequestProperty("Content-Type", "text/xml; charset=UTF-8");
        return parseISToString(c.getInputStream());
    }

    public static String parseISToString(java.io.InputStream is) {
        java.io.DataInputStream din = new java.io.DataInputStream(is);
        StringBuffer sb = new StringBuffer();
        try {
            String line = null;
            while ((line = din.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (Exception ex) {
            ex.getMessage();
        } finally {
            try {
                is.close();
            } catch (Exception ex) {
            }
        }
        return stripXMLHeader(sb.toString());
    }

    public static String stripXMLHeader(String xml) {
        int x, y;
        if (xml.contains("<?")) {
            x = xml.indexOf("?");
            if (x == 1) {
                y = xml.lastIndexOf("?") + 2;
                xml = xml.substring(y, xml.length());
            }
        }
        return xml;
    }
}
