package com.javapda.c328r;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import junit.framework.TestCase;
import com.javapda.camera.CameraInfo;

public class C328rCameraInfoTest extends TestCase {

    public void testBasic() {
        CameraInfo ci = C328rCameraInfo.getInstance();
        assertNotNull(ci);
        assertNotNull(ci.getCapabilities());
        assertFalse(ci.getCapabilities().isEmpty());
        System.out.println(ci.getUrl());
        URL url = ci.getUrl();
        try {
            URLConnection conn = url.openConnection();
            conn.connect();
            InputStream is = conn.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line = null;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
