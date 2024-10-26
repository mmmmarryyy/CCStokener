package org.vardb;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import org.junit.Test;
import org.vardb.util.CWebHelper;

public class TestScratch {

    @Test
    public void testSomething() {
        String feed = "http://groups.google.com/group/vardb-test/feed/rss_v2_0_msgs.xml";
        System.out.println(CWebHelper.readRss(feed, 1));
    }

    public static String getUrl(String url) {
        BufferedInputStream in = null;
        BufferedOutputStream out = null;
        try {
            ByteArrayOutputStream bytearray = new ByteArrayOutputStream();
            in = new BufferedInputStream(new URL(url).openStream());
            out = new BufferedOutputStream(bytearray, 1024);
            byte[] data = new byte[1024];
            int x = 0;
            while ((x = in.read(data, 0, 1024)) >= 0) {
                out.write(data, 0, x);
            }
            return bytearray.toString();
        } catch (Exception e) {
            throw new CVardbException(e);
        } finally {
            try {
                if (out != null) out.close();
                if (in != null) in.close();
            } catch (Exception e) {
            }
        }
    }
}
