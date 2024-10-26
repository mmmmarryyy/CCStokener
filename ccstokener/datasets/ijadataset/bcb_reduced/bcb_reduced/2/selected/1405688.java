package soapdust.urlhandler.servlet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import junit.framework.TestCase;

public class HandlerTest extends TestCase {

    static final byte[] TEST_DATA = new byte[] { 0, 1, 2, 3 };

    @Override
    protected void setUp() throws Exception {
        Class.forName("soapdust.urlhandler.servlet.Handler");
        Handler.clearRegister();
    }

    public void testServletProtocolIsSUpportedByURL() throws MalformedURLException, ClassNotFoundException {
        new URL("servlet:");
    }

    public void testOpeningAServletURLReturnsAnHttpConnection() throws IOException {
        URLConnection connection = new URL("servlet:soapdust.urlhandler.servlet.NoopServlet/").openConnection();
        assertTrue(connection instanceof HttpURLConnection);
    }

    public void testServletUrlRequestStatusCodeDefaultsTo200() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL("servlet:soapdust.urlhandler.servlet.NoopServlet").openConnection();
        assertEquals(200, connection.getResponseCode());
    }

    public void testServletUrlRequestStatusCodeIsTheResquestStatusCodeOfTheServlet() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL("servlet:soapdust.urlhandler.servlet.Status500Servlet/").openConnection();
        assertEquals(500, connection.getResponseCode());
    }

    public void testServletUrlResponseIsEmptyWhenNoDataReturnedByServlet() throws IOException {
        assertUrlContent(new URL("servlet:soapdust.urlhandler.servlet.NoopServlet/"), new byte[0]);
    }

    public void testServletUrlResponseContentIsReturnedFromServlet() throws IOException {
        assertUrlContent(new URL("servlet:soapdust.urlhandler.servlet.TestData/"), TEST_DATA);
    }

    public void test5xxStatusThrowsIOExceptionwhenTryingToRead() throws MalformedURLException, IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL("servlet:soapdust.urlhandler.servlet.Status500Servlet/").openConnection();
        try {
            connection.getInputStream();
            fail();
        } catch (IOException e) {
        }
    }

    public void testCanReadResponseFromErrorStreamWhen5xxStatus() throws MalformedURLException, IOException {
        assertUrlErrorStreamContent(new URL("servlet:soapdust.urlhandler.servlet.Status500Servlet/"), TEST_DATA);
    }

    public void testCanNotReadResponseFromErrorStreamWhenNot5xxStatus() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL("servlet:soapdust.urlhandler.servlet.NoopServlet/").openConnection();
        assertNull(connection.getErrorStream());
    }

    public void testOneCanWriteInAServletUrl() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL("servlet:soapdust.urlhandler.servlet.NoopServlet/").openConnection();
        connection.getOutputStream();
    }

    public void testWrittenDataIsSentToServlet() throws MalformedURLException, IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL("servlet:soapdust.urlhandler.servlet.EchoServlet/").openConnection();
        OutputStream out = connection.getOutputStream();
        out.write(HandlerTest.TEST_DATA);
        out.flush();
        out.close();
        assertStreamContent(TEST_DATA, connection.getInputStream());
    }

    public void testMethodPostDelegatesToDoPost() throws MalformedURLException, IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL("servlet:soapdust.urlhandler.servlet.PostStatus201/").openConnection();
        connection.setRequestMethod("POST");
        assertEquals(201, connection.getResponseCode());
    }

    public void testManageHeaders() throws MalformedURLException, IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL("servlet:soapdust.urlhandler.servlet.ExpectSoapActionHeaderWithValueTest/").openConnection();
        connection.addRequestProperty("SOAPAction", "Test");
        assertEquals(200, connection.getResponseCode());
    }

    public void testManageHeaderWithMultipleValues() throws MalformedURLException, IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL("servlet:soapdust.urlhandler.servlet.ExpectTestHeaderWithValuesTest1AndTest2/").openConnection();
        connection.addRequestProperty("Test", "Test1");
        connection.addRequestProperty("Test", "Test2");
        assertEquals(200, connection.getResponseCode());
    }

    public void testOneCanExplicitlyRegisterAnInstanceOfAServletObject() throws IOException {
        Handler.register("NoopServlet", new NoopServlet());
        HttpURLConnection connection = (HttpURLConnection) new URL("servlet:reg:NoopServlet/").openConnection();
        assertEquals(200, connection.getResponseCode());
    }

    private void assertUrlContent(URL url, byte[] expectedContent) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        InputStream stream = connection.getInputStream();
        assertStreamContent(expectedContent, stream);
    }

    private void assertStreamContent(byte[] expectedContent, InputStream stream) throws IOException {
        assertTrue(Arrays.equals(expectedContent, readFully(stream)));
    }

    private byte[] readFully(InputStream in) throws IOException {
        ByteArrayOutputStream content = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        for (int read = in.read(buffer, 0, buffer.length); read != -1; read = in.read(buffer, 0, buffer.length)) {
            content.write(buffer, 0, read);
        }
        return content.toByteArray();
    }

    private void assertUrlErrorStreamContent(URL url, byte[] expectedContent) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        InputStream stream = connection.getErrorStream();
        assertStreamContent(expectedContent, stream);
    }
}

class NoopServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        return;
    }
}

class Status500Servlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ServletOutputStream out = resp.getOutputStream();
        out.write(HandlerTest.TEST_DATA);
        out.flush();
        out.close();
        resp.setStatus(500);
        return;
    }
}

class TestData extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ServletOutputStream out = resp.getOutputStream();
        out.write(HandlerTest.TEST_DATA);
        out.flush();
        out.close();
        return;
    }
}

class EchoServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ServletOutputStream out = resp.getOutputStream();
        ServletInputStream in = req.getInputStream();
        byte[] buffer = new byte[1024];
        for (int read = in.read(buffer); read != -1; read = in.read(buffer)) {
            out.write(buffer, 0, read);
        }
        out.flush();
        out.close();
        in.close();
        return;
    }
}

class PostStatus201 extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setStatus(201);
    }
}

class ExpectSoapActionHeaderWithValueTest extends HttpServlet {

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getHeader("SOAPAction") != "Test") {
            resp.setStatus(400);
        }
    }
}

class ExpectTestHeaderWithValuesTest1AndTest2 extends HttpServlet {

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Enumeration headerNames = req.getHeaderNames();
        if (!headerNames.hasMoreElements()) resp.setStatus(400); else {
            String name = (String) headerNames.nextElement();
            Enumeration headerValues = req.getHeaders(name);
            if (!"Test".equals(name)) resp.setStatus(401); else if (!headerValues.hasMoreElements()) resp.setStatus(402); else if (!"Test1".equals((String) headerValues.nextElement())) resp.setStatus(403); else if (!headerValues.hasMoreElements()) resp.setStatus(404); else if (!"Test2".equals((String) headerValues.nextElement())) resp.setStatus(405); else if (headerNames.hasMoreElements()) resp.setStatus(406);
        }
    }
}
