package net.httpconn;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public abstract class HttpCommand {

    public abstract String execute() throws IOException;

    protected String url;

    protected String content;

    protected String method;

    protected HttpURLConnection connection;

    protected String c_type;

    protected int timeout;

    public void initializeConnection() throws MalformedURLException, IOException {
        URL urlLocal = new URL(this.getUrl());
        connection = (HttpURLConnection) urlLocal.openConnection();
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setUseCaches(false);
        connection.setRequestMethod(this.getMethod());
        if (this.getTimeout() <= 0) {
            this.setTimeout(50000);
        }
        connection.setConnectTimeout(this.getTimeout());
        if (c_type != null) {
            connection.addRequestProperty("Content-Type", c_type);
        }
    }

    public String getUrl() {
        return url;
    }

    public void finalizeConnection() {
        if (connection != null) {
            connection.disconnect();
            connection = null;
        }
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getC_type() {
        return c_type;
    }

    public void setC_type(String c_type) {
        this.c_type = c_type;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
}
