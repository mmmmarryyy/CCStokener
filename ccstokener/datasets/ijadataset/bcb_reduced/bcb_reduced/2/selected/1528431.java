package com.google.code.sapwcrawler.download.procedure;

import java.io.*;
import java.net.*;
import com.google.code.sapwcrawler.data.BinaryData;
import com.google.code.sapwcrawler.datahandler.DataHandler;
import java.util.*;

public class URLDownloader implements DownloadProc {

    private static class ByteArray {

        private byte[] data;

        private int index = 0;

        public ByteArray(int capacity) {
            data = new byte[capacity];
        }

        public void put(byte b) {
            index++;
            if (index >= data.length) data = Arrays.copyOf(data, data.length * 2);
            data[index] = b;
        }

        public byte[] toArray() {
            return data;
        }
    }

    private Map<String, String> headers = Collections.synchronizedMap(new HashMap<String, String>());

    private DataHandler<BinaryData> ddHandler;

    private boolean followRedirects = false;

    public void setFollowRedirects(boolean v) {
        this.followRedirects = v;
    }

    public void setDataHandler(DataHandler<BinaryData> v) {
        this.ddHandler = v;
    }

    public void setDefaultHeaders(Map<String, String> headers) {
        this.headers.putAll(headers);
    }

    private byte[] readData(HttpURLConnection url) throws IOException {
        InputStream is = url.getInputStream();
        int size = url.getContentLength() > 0 ? url.getContentLength() : 25000;
        ByteArray array = new ByteArray(size);
        int b;
        while ((b = is.read()) > 0) array.put((byte) b);
        return array.toArray();
    }

    @Override
    public void download(URL url) throws Exception {
        HttpURLConnection conn = openConnection(url);
        conn.setInstanceFollowRedirects(this.followRedirects);
        conn.connect();
        if (conn.getResponseCode() == 200) {
            ddHandler.processData(new BinaryData(readData(conn), conn.getHeaderFields(), url));
        } else {
            throw new IOException("Error fetching " + url.toString());
        }
    }

    private HttpURLConnection openConnection(URL url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        for (Map.Entry<String, String> ent : headers.entrySet()) conn.setRequestProperty(ent.getKey(), ent.getValue());
        return conn;
    }
}
