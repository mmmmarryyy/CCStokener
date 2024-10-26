package com.corratech.opensuite.zimbra;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class HttpUpload {

    public static final String CRLF = "\r\n";

    public static final String CONTENT_TYPE_TEXT_PLAIN = "text/plain";

    public static final String CONTENT_TYPE_APPLICATION_OCTET_STREAM = "application/octet-stream";

    public static final String CONTENT_TYPE_IMAGE_JPEG = "image/jpeg";

    public static final String CONTENT_TYPE_APPLICATION_ZIP = "application/zip";

    protected ByteArrayOutputStream bos = null;

    protected URL url = null;

    private boolean bundleLocked = false;

    protected String BOUNDARY_SEPARATOR_SUFFIX = "-----------------7d2221111300b8";

    protected HttpURLConnection httpCon;

    public HttpUpload(String url) throws MalformedURLException {
        this.url = new URL(url);
        bos = new ByteArrayOutputStream();
        httpCon = null;
    }

    public int attachFile(String file, String paramName, String fileName, String contentType) throws FileNotFoundException, IOException {
        if (bundleLocked) throw new IOException("Attempting to add File to multi-part upload after lock");
        String contentEncoding = "binary";
        FileInputStream fis = new FileInputStream(file);
        String str = CRLF + "--" + BOUNDARY_SEPARATOR_SUFFIX + CRLF;
        str += "Content-Disposition: form-data; name=\"" + paramName + "\"; filename=\"" + fileName + "\"" + CRLF;
        str += "Content-Type: " + contentType + CRLF;
        str += "Content-Transfer-Encoding: " + contentEncoding + CRLF;
        str += CRLF;
        bos.write(str.getBytes());
        int bytesRead = 0;
        int datum = -1;
        while ((datum = fis.read()) != -1) {
            bos.write(datum);
            bytesRead++;
        }
        fis.close();
        return bytesRead;
    }

    public void attachParam(String paramName, String paramValue) throws IOException {
        if (bundleLocked) throw new IOException("Attempting to add param to multi-part upload after lock");
        String str = "--" + BOUNDARY_SEPARATOR_SUFFIX + CRLF;
        str += "Content-Disposition: form-data; name=\"" + paramName + "\"" + CRLF;
        str += CRLF;
        str += paramValue + CRLF;
        bos.write(str.getBytes());
    }

    protected void closeBundle() throws IOException {
        String str = CRLF + "--" + BOUNDARY_SEPARATOR_SUFFIX + "--";
        bos.write(str.getBytes());
    }

    public HttpURLConnection openConnection() throws IOException {
        httpCon = (HttpURLConnection) url.openConnection();
        httpCon.setDoOutput(true);
        httpCon.setDoInput(true);
        httpCon.setRequestProperty("connection", "Keep-Alive");
        return httpCon;
    }

    public HttpURLConnection connect() throws IOException {
        if (httpCon == null) throw new NullPointerException("HttpUpload (RTFM): You must call openConnection() before calling connect().");
        closeBundle();
        httpCon.setRequestProperty("content-length", String.valueOf(bos.size()));
        httpCon.setRequestProperty("content-type", "multipart/form-data; boundary=" + BOUNDARY_SEPARATOR_SUFFIX);
        OutputStream os = httpCon.getOutputStream();
        bos.writeTo(os);
        return httpCon;
    }
}
