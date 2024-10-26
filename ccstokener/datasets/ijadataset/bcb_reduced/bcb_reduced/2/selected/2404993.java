package demo.restful.client;

import java.io.InputStream;
import java.net.URL;
import org.apache.cxf.helpers.IOUtils;

public final class Client {

    private Client() {
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Sent HTTP GET request to query customer info");
        URL url = new URL("http://localhost:8080/xml/customers");
        InputStream in = url.openStream();
        System.out.println(getStringFromInputStream(in));
    }

    private static String getStringFromInputStream(InputStream in) throws Exception {
        return IOUtils.toString(in);
    }
}
