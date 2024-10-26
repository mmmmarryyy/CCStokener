package foursquare4j;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import foursquare4j.exception.AuthenticationException;
import foursquare4j.exception.FoursquareException;
import foursquare4j.exception.RateLimitingException;
import foursquare4j.types.Error;
import foursquare4j.xml.handler.ErrorHandler;
import foursquare4j.xml.handler.Handler;

public class FoursquareBasicAuthenticationImpl extends FoursquareBase {

    protected final String username;

    protected final String password;

    public FoursquareBasicAuthenticationImpl(final String username, final String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    protected <T> T execute(final HttpMethod method, final String url, Parameters parameters, final Handler<T> handler) throws FoursquareException {
        HttpURLConnection connection = null;
        try {
            switch(method) {
                case GET:
                    connection = openConnection(url.concat("?").concat(formEncode(parameters)));
                    connection.setRequestMethod("GET");
                    connection.connect();
                    break;
                case POST:
                    connection = openConnection(url);
                    connection.setRequestMethod("POST");
                    connection.setDoOutput(true);
                    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    connection.connect();
                    final OutputStream out = connection.getOutputStream();
                    out.write(formEncode(parameters).getBytes());
                    out.flush();
                    out.close();
                    break;
            }
            final int statusCode = connection.getResponseCode();
            if (statusCode / 100 != 2) {
                final Error error = parseBody(connection.getErrorStream(), new ErrorHandler());
                if (error == null) throw new FoursquareException(connection.getResponseMessage()); else if ("error".equals(error.getType())) throw new FoursquareException(error.getMessage()); else if ("unauthorized".equals(error.getType())) throw new AuthenticationException(error.getMessage()); else if ("ratelimited".equals(error.getType())) throw new RateLimitingException(error.getMessage()); else throw new FoursquareException(connection.getResponseMessage());
            }
            return parseBody(connection.getInputStream(), handler);
        } catch (final IOException e) {
            throw new FoursquareException(e);
        } catch (final ParserConfigurationException e) {
            throw new FoursquareException(e);
        } catch (final SAXException e) {
            throw new FoursquareException(e);
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    protected HttpURLConnection openConnection(final String url) throws IOException {
        final HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("User-Agent", userAgent);
        connection.setRequestProperty("Accept", "application/xhtml+xml,application/xml,text/xml;q=0.9,*/*;q=0.8");
        connection.setRequestProperty("Accept-Language", "ja,en-us;q=0.7,en;q=0.3");
        connection.setRequestProperty("Accept-Encoding", "deflate");
        connection.setRequestProperty("Accept-Charset", "utf-8");
        connection.setRequestProperty("Authorization", "Basic ".concat(base64Encode((username.concat(":").concat(password)).getBytes("UTF-8"))));
        return connection;
    }

    private String formEncode(final Map<String, String> parameters) {
        final StringBuilder builder = new StringBuilder();
        boolean isFirst = true;
        for (final Map.Entry<String, String> parameter : parameters.entrySet()) {
            if (isFirst) isFirst = false; else builder.append("&");
            final String key = parameter.getKey();
            if (key == null) continue;
            builder.append(urlEncode(key));
            builder.append("=");
            builder.append(urlEncode(parameter.getValue()));
        }
        return builder.toString();
    }

    private String urlEncode(final String s) {
        if (s == null) return "";
        try {
            return URLEncoder.encode(s, "UTF-8").replace("+", "%20").replace("*", "%2A").replace("%7E", "~");
        } catch (final UnsupportedEncodingException e) {
        }
        return "";
    }

    private String base64Encode(final byte[] value) {
        final char[] table = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();
        final StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < value.length; ++i) {
            String b = Integer.toBinaryString(value[i] & 0xff);
            while (b.length() < 8) b = "0".concat(b);
            buffer.append(b);
        }
        while (buffer.length() % 6 != 0) buffer.append("0");
        final StringBuilder encoded = new StringBuilder();
        for (int i = 0; i < buffer.length(); i += 6) encoded.append(table[Integer.parseInt(buffer.substring(i, i + 6), 2)]);
        while (encoded.length() % 4 != 0) encoded.append("=");
        return encoded.toString();
    }
}
