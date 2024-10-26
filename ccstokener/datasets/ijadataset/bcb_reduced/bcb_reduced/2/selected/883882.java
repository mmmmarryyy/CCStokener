package com.efsol.util;

import java.net.URL;
import java.util.Date;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;

public class BasicReloadableReference extends BasicIndirectReference implements ReloadableReference {

    private URL url;

    private long interval;

    private long timestamp;

    private int status;

    public BasicReloadableReference(URL url, long interval, Object value, long timestamp, int status) {
        super(value);
        this.url = url;
        this.interval = interval;
        this.timestamp = timestamp;
        this.status = status;
    }

    public BasicReloadableReference(URL url, long interval, Object value, long timestamp) {
        this(url, interval, value, timestamp, VALID);
    }

    public BasicReloadableReference(URL url, long interval, Object value) {
        this(url, interval, value, new Date().getTime(), VALID);
    }

    public BasicReloadableReference(URL url, long interval) {
        this(url, interval, null, 0, INVALID);
    }

    public URL getURL() {
        return url;
    }

    public long getInterval() {
        return interval;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getStatus() {
        return status;
    }

    public void setURL(URL url) {
        super.put(null);
        this.url = url;
        this.timestamp = 0;
        this.status = INVALID;
    }

    public void setInterval(long interval) {
        this.interval = interval;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public void put(Object value) {
        if (value != null) {
            super.put(value);
            timestamp = new Date().getTime();
            status = VALID;
        } else {
            status = INVALID;
        }
    }

    protected Object getRemoteContent() throws IOException {
        InputStream in = url.openStream();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        FileUtils.copyStream(in, out, true);
        return out.toString();
    }

    public synchronized void reload() {
        System.out.println("BRR: (" + new Date() + ")reloading url '" + url + "'");
        status = LOADING;
        Object content = null;
        try {
            content = getRemoteContent();
            put(content);
        } catch (IOException e) {
            status = INVALID;
        }
    }

    public void refresh() {
        if (status == INVALID || (status == VALID && isExpired())) {
            reload();
        }
    }

    public boolean isExpired() {
        return new Date().getTime() > timestamp + interval;
    }
}
