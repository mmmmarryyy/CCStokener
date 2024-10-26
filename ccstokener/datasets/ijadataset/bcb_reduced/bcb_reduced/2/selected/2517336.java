package dev.cwxstat.com;

import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class MyHttps {

    public static int getUrl(final String s) {
        try {
            final URL url = new URL(s);
            final BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            int count = 0;
            String data = null;
            while ((data = reader.readLine()) != null) {
                System.out.printf("Results(%3d) of data: %s\n", count, data);
                ++count;
            }
            return count;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
