package test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

public class ReadVersion {

    public static void main(String[] args) {
        System.out.println(readVersionFromSocket("http://www.jforum.net/latest_version.txt"));
    }

    private static String readVersionFromSocket(String strUrl) {
        InputStream is = null;
        OutputStream os = null;
        String data = null;
        try {
            URL url = new URL(strUrl);
            URLConnection conn = url.openConnection();
            is = conn.getInputStream();
            os = new ByteArrayOutputStream();
            int available = is.available();
            while (available > 0) {
                byte[] b = new byte[available];
                is.read(b);
                os.write(b);
                available = is.available();
            }
            data = os.toString();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e) {
                }
            }
            if (os != null) {
                try {
                    os.close();
                } catch (Exception e) {
                }
            }
        }
        return data;
    }
}
