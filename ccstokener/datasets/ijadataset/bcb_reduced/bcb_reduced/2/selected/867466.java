package com.weespers.usage;

import java.net.URL;
import java.net.URLConnection;

public class UsageReporter {

    public static UsageReporter INSTANCE = new UsageReporter();

    public void reportUsage() {
        try {
            URL url = new URL("http://c.statcounter.com/6457148/0/748ad9dc/1/");
            URLConnection connection = url.openConnection();
            connection.setConnectTimeout(5000);
            connection.getInputStream();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
