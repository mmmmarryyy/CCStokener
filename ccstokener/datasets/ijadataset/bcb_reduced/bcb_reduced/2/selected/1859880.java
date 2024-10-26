package ao.dd.web.alexo;

import ao.dd.common.WebUtils;
import ao.dd.web.alexo.response.ImageResponse;
import ao.dd.web.alexo.response.Response;
import ao.dd.web.alexo.response.TextResponse;
import javax.imageio.ImageIO;
import java.awt.*;
import java.io.*;
import java.net.Proxy;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * A browser session.
 * Should always be accessed via the UserAgent interface.
 */
public class UserAgentImpl implements UserAgent {

    private static final int TIMOUT_SECONDS = 30;

    private String USER_AGENT;

    private CookieManager COOKIES;

    private Proxy PROXY;

    private volatile URL previouseUrl = null;

    public UserAgentImpl() {
        this("Mozilla/5.0 (Windows; U; Windows NT 5.1; nl; rv:1.8)");
    }

    public UserAgentImpl(Proxy proxy) {
        this();
        PROXY = proxy;
    }

    public UserAgentImpl(String userAgentHeader) {
        COOKIES = new CookieManagerImpl();
        USER_AGENT = userAgentHeader;
    }

    public Response load(URL url) {
        return load(url, previouseUrl, (String) null);
    }

    public Response load(URL url, String postData) {
        return load(url, previouseUrl, postData);
    }

    public Response load(URL url, Map<String, String> postData) {
        return load(url, WebUtils.urlEncode(postData));
    }

    public Response load(URL url, Map<String, String> data, boolean isPost) {
        return isPost ? load(url, data) : load(WebUtils.asUrl(url.toString() + "?" + WebUtils.urlEncode(data)));
    }

    public Response load(URL url, URL referer) {
        return load(url, referer, (String) null);
    }

    public Response load(URL url, URL referer, Map<String, String> postData) {
        return load(url, referer, WebUtils.urlEncode(postData));
    }

    public Response load(URL url, URL referrer, Map<String, String> data, boolean isPost) {
        return isPost ? load(url, referrer, data) : load(WebUtils.asUrl(url.toString() + "?" + WebUtils.urlEncode(data)), referrer);
    }

    public Response load(final URL url, final URL referer, final String postData) {
        final CountDownLatch done = new CountDownLatch(1);
        final Throwable[] error = new Throwable[1];
        final Response[] response = new Response[1];
        new Thread() {

            public void run() {
                try {
                    response[0] = doLoad(url, referer, postData);
                    response[0].get();
                } catch (Throwable err) {
                    error[0] = err;
                }
                done.countDown();
            }
        }.start();
        try {
            done.await(TIMOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            error[0] = new SocketTimeoutException("didn't finish in " + TIMOUT_SECONDS + " seconds");
        }
        if (error[0] != null) {
            throw new Error(error[0]);
        }
        return response[0];
    }

    private Response doLoad(URL url, URL referer, String postData) throws IOException {
        URLConnection connection = PROXY == null ? url.openConnection() : url.openConnection(PROXY);
        COOKIES.writeCookies(connection);
        connection.setRequestProperty("User-Agent", USER_AGENT);
        if (referer != null) {
            connection.setRequestProperty("Referer", referer.toString());
        }
        if (postData != null) {
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setRequestProperty("CONTENT_LENGTH", "" + postData.length());
            OutputStream os = connection.getOutputStream();
            OutputStreamWriter osw = new OutputStreamWriter(os);
            osw.write(postData);
            osw.flush();
            osw.close();
        }
        connection.connect();
        COOKIES.readCookies(connection);
        previouseUrl = url;
        return responceInstance(url, connection.getInputStream(), connection.getContentType());
    }

    private Response responceInstance(URL url, InputStream input, String type) {
        if (type.toLowerCase().startsWith("text/")) {
            return new TextResponseImpl(url, input);
        } else if (type.toLowerCase().startsWith("image/")) {
            return new ImageResponseImpl(url, input);
        } else {
            return new ResponseImpl(url, input);
        }
    }

    private class ResponseImpl implements Response {

        private byte[] bytes;

        private final BufferedInputStream inputStream;

        private final URL address;

        private ResponseImpl(URL address, InputStream input) {
            this.address = address;
            this.inputStream = new BufferedInputStream(input);
            this.bytes = null;
        }

        public URL address() {
            return address;
        }

        public InputStream get() {
            return new ByteArrayInputStream(getBytes());
        }

        public synchronized byte[] getBytes() {
            if (bytes != null) return bytes;
            List<Byte> list = new ArrayList<Byte>();
            try {
                for (int chr = inputStream.read(); chr != -1; chr = inputStream.read()) {
                    list.add((byte) chr);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            bytes = new byte[list.size()];
            int byteIndex = 0;
            for (Byte b : list) {
                bytes[byteIndex++] = b;
            }
            return bytes;
        }

        public void setCookie(String name, String key, String value) {
            COOKIES.setCookie(address, name, key, value);
        }

        public String getCookie(String name, String key) {
            return COOKIES.getCookie(address, name, key);
        }

        public Map<String, Map<String, String>> getCookies() {
            return COOKIES.getCookies(address);
        }
    }

    public class TextResponseImpl extends ResponseImpl implements TextResponse {

        private String cache = null;

        private TextResponseImpl(URL address, InputStream input) {
            super(address, input);
        }

        public String getText() {
            if (cache != null) return cache;
            byte[] bytes = getBytes();
            StringBuilder text = new StringBuilder(bytes.length);
            for (byte aByte : bytes) {
                text.append((char) aByte);
            }
            cache = text.toString();
            return cache;
        }

        public String toString() {
            return getText();
        }
    }

    public class ImageResponseImpl extends ResponseImpl implements ImageResponse {

        private Image cache = null;

        private ImageResponseImpl(URL address, InputStream input) {
            super(address, input);
        }

        public Image getImage() {
            if (cache != null) return cache;
            try {
                cache = ImageIO.read(get());
                return cache;
            } catch (IOException e) {
                throw new Error(e);
            }
        }
    }
}
