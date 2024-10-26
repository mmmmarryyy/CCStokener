package br.net.woodstock.rockframework.net.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Map.Entry;
import br.net.woodstock.rockframework.util.Assert;
import br.net.woodstock.rockframework.utils.ConditionUtils;

public class HttpClient implements Serializable {

    private static final long serialVersionUID = -4741892532905721566L;

    private static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

    private static final String GET_METHOD = "GET";

    private static final String POST_METHOD = "POST";

    private static final String POST_CONTENT_TYPE_PROPERTY = "Content-Type";

    private static final String POST_CONTENT_TYPE_VALUE = "application/x-www-form-urlencoded";

    private Proxy proxy;

    public HttpClient() {
        super();
    }

    public HttpClient(final Proxy proxy) {
        super();
        this.proxy = proxy;
    }

    public byte[] doGet(final String url) {
        Assert.notEmpty(url, "url");
        try {
            HttpURLConnection c = this.getGetConnection(url);
            c.connect();
            InputStream inputStream = c.getInputStream();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            int i = -1;
            do {
                i = inputStream.read();
                if (i != -1) {
                    outputStream.write(i);
                }
            } while (i != -1);
            inputStream.close();
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new HttpException(e);
        }
    }

    public byte[] doPost(final String url, final Map<String, Object> params) {
        return this.doPost(url, params, HttpClient.DEFAULT_CHARSET);
    }

    public byte[] doPost(final String url, final Map<String, Object> params, final Charset charset) {
        Assert.notEmpty(url, "url");
        Assert.notNull(charset, "charset");
        try {
            HttpURLConnection c = this.getPostConnection(url, params, charset);
            InputStream inputStream = c.getInputStream();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            int i = -1;
            do {
                i = inputStream.read();
                if (i != -1) {
                    outputStream.write(i);
                }
            } while (i != -1);
            inputStream.close();
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new HttpException(e);
        }
    }

    private HttpURLConnection getPostConnection(final String url, final Map<String, Object> params, final Charset charset) throws IOException {
        URL u = new URL(url);
        HttpURLConnection c = this.getConnection(u);
        c.setAllowUserInteraction(true);
        c.setDoInput(true);
        c.setDoOutput(true);
        c.setRequestMethod(HttpClient.POST_METHOD);
        c.setRequestProperty(HttpClient.POST_CONTENT_TYPE_PROPERTY, HttpClient.POST_CONTENT_TYPE_VALUE);
        if (ConditionUtils.isNotEmpty(params)) {
            OutputStream outputStream = c.getOutputStream();
            Writer writer = new OutputStreamWriter(outputStream);
            StringBuilder builder = new StringBuilder();
            boolean first = true;
            for (Entry<String, Object> entry : params.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (!first) {
                    builder.append("&");
                }
                builder.append(key);
                builder.append("=");
                if (value != null) {
                    String valueStr = URLEncoder.encode(value.toString(), charset.displayName());
                    builder.append(valueStr);
                }
                if (first) {
                    first = false;
                }
            }
            writer.write(builder.toString());
            writer.flush();
            writer.close();
        }
        return c;
    }

    private HttpURLConnection getGetConnection(final String url) throws IOException {
        URL u = new URL(url);
        HttpURLConnection c = this.getConnection(u);
        c.setDefaultUseCaches(false);
        c.setDoInput(true);
        c.setRequestMethod(HttpClient.GET_METHOD);
        c.setUseCaches(false);
        return c;
    }

    private HttpURLConnection getConnection(final URL url) throws IOException {
        if (this.proxy != null) {
            HttpURLConnection c = (HttpURLConnection) url.openConnection(this.proxy);
            return c;
        }
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        return c;
    }
}
