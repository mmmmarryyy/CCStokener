package de.campussource.cse.mapper.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import com.sun.jersey.api.container.httpserver.HttpServerFactory;
import com.sun.net.httpserver.HttpServer;

public class RestIdentityMapperIntegrationTest {

    private static final String LOCALHOST = "http://localhost:8000/";

    private static HttpServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        System.out.println("Creating Server...");
        server = HttpServerFactory.create(LOCALHOST);
        server.start();
        System.out.println("HTTP Server started.");
        System.out.println("Running tests...");
    }

    private static MappedObject sendHttpRequestToUrl(URL url) throws Exception {
        return sendHttpRequestToUrl(url, "GET");
    }

    private static MappedObject sendHttpRequestToUrl(URL url, String method) throws Exception {
        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method);
            connection.connect();
            InputStream is = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder buffer = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }
            System.out.println("Read: " + buffer.toString());
            connection.disconnect();
            JAXBContext context = JAXBContext.newInstance(MappedObject.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            MappedObject mapped = (MappedObject) unmarshaller.unmarshal(new StringReader(buffer.toString()));
            return mapped;
        } catch (IOException e) {
            e.printStackTrace();
        }
        throw new Exception("Could not establish connection to " + url.toExternalForm());
    }

    @Test
    public void testMapper() throws Exception {
        MappedObject mapped = sendHttpRequestToUrl(new URL(LOCALHOST + "mapped/12345/objectid"), "PUT");
        assertNotNull(mapped);
        assertNotNull(mapped.getBusId());
        assertEquals(Long.valueOf(12345L), mapped.getClientInstanceId());
        assertEquals("objectid", mapped.getClientObjectId());
        MappedObject loaded = sendHttpRequestToUrl(new URL(LOCALHOST + "mapped/" + mapped.getBusId() + "/12345"));
        assertEquals(mapped, loaded);
    }

    @AfterClass
    public static void tearDown() throws IOException {
        if (server != null) {
            System.out.println("Stopping server...");
            server.stop(0);
            System.out.println("Server stopped");
        }
    }
}
