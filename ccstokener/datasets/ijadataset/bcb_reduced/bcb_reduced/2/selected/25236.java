package juploader.shortener.plugin.tnij;

import juploader.httpclient.FormSubmitRequest;
import juploader.httpclient.HttpClient;
import juploader.httpclient.HttpResponse;
import juploader.httpclient.exceptions.RequestCancelledException;
import juploader.httpclient.exceptions.TimeoutException;
import juploader.parsing.Parser;
import juploader.plugin.Plugin;
import juploader.shortener.AbstractShortenerProvider;
import java.io.IOException;
import java.io.InputStream;

/**
 * Wtyczka obsługująca skracanie adresów przy pomocy serwisu Tnij.org.
 *
 * @author Adam Pawelec
 */
@Plugin(author = "Adam Pawelec", authorEmail = "proktor86@gmail.com", name = "tnij.org", version = 1)
public class Tnij extends AbstractShortenerProvider {

    public String shorten(String url) throws IOException {
        HttpClient httpClient = HttpClient.createInstance();
        FormSubmitRequest request = httpClient.createFormSubmitRequest();
        request.setUrl("http://tnij.org/skracaj.php");
        request.addParameter("url_do_skrocenia", url);
        request.addParameter("maskowanie", "0");
        request.addParameter("ukrywanie", "1");
        try {
            HttpResponse response = httpClient.execute(request);
            String shortenLink = parseResponseBody(response.getResponseBody());
            response.close();
            return shortenLink;
        } catch (RequestCancelledException ex) {
            return null;
        } catch (TimeoutException ex) {
            return null;
        }
    }

    public boolean canBeShorter(String value) {
        return value.startsWith("http://") || value.startsWith("https://") || value.startsWith("ftp://");
    }

    private String parseResponseBody(InputStream responseBody) throws IOException {
        Parser parser = new Parser(responseBody);
        return parser.parseOne("class=\"inp\" size=\"80\" value=\"(.*)\" style=\"width:460px;\" /></p>");
    }
}
