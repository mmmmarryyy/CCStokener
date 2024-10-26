package hu.gbalage.httpserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/**
 * @author balage
 *
 */
public class HttpURLReaderHandler implements HttpHandler {

    private URL url;

    public HttpURLReaderHandler(URL url) {
        System.out.println(url.toString());
        this.url = url;
    }

    @Override
    public void handle(HttpExchange arg0) throws IOException {
        byte[] b;
        try {
            InputStream is = url.openStream();
            b = new byte[is.available()];
            is.read(b);
            is.close();
        } catch (Exception e) {
            arg0.sendResponseHeaders(500, 0);
            return;
        }
        arg0.sendResponseHeaders(200, b.length);
        OutputStream os = arg0.getResponseBody();
        os.write(b);
        os.close();
        arg0.close();
    }
}
