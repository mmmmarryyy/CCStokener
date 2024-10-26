package safe.docservice.test;

import gate.Gate;
import java.net.*;
import java.io.*;
import junit.framework.Test;
import junit.framework.TestSuite;

public class TestDocservice {

    public static Test suite() throws Exception {
        Gate.init();
        TestSuite suite = new TestSuite();
        try {
            suite.addTest(TestDocserviceAPI.suite());
        } catch (Exception e) {
            System.out.println("Can't add tests! exception = " + e);
            throw (e);
        }
        return suite;
    }

    public static byte[] getFileContent(URL url) throws IOException {
        System.out.println("Opening url: " + url);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedInputStream in = new BufferedInputStream(url.openStream());
        byte[] buf = new byte[4096];
        int numRead = 0;
        while ((numRead = in.read(buf)) >= 0) {
            baos.write(buf, 0, numRead);
        }
        in.close();
        return baos.toByteArray();
    }
}
