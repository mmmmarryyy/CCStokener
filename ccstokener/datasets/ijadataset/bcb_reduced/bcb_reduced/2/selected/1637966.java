package org.jomper.pluto;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.jomper.pluto.ConnectionDigestHandlerDefaultImpl;
import org.jomper.pluto.ConnectionDigestHandler;

public class ConnectionDigestHandlerTest {

    private ConnectionDigestHandler connectionDigestHandler = null;

    private URLConnection uc = null;

    @Before
    public void setUp() throws Exception {
        connectionDigestHandler = new ConnectionDigestHandlerDefaultImpl();
        URL url = null;
        try {
            url = new URL("http://dev2dev.bea.com.cn/bbs/servlet/D2DServlet/download/64104-35000-204984-2890/webwork2guide.pdf");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        try {
            uc = url.openConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testSetConnectionDigest() {
        connectionDigestHandler.setConnectionDigest(uc, 3);
    }

    @Test
    public void testGetConnectionDigest() {
    }
}
