package com.altova.io;

public class UrlInput extends StreamInput {

    String url;

    UrlInput(String url) throws Exception {
        super(new java.net.URL(url).openStream());
        this.url = url;
    }

    public String getFilename() {
        return url;
    }

    public void close() throws Exception {
        getStream().close();
    }
}
