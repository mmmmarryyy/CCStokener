package com.dukesoftware.utils.test;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import com.google.common.io.Closeables;

@Deprecated
public class SillyRawLevelMultiPartRequest {

    public static void main(String[] args) {
        SillyRawLevelMultiPartRequest task = new SillyRawLevelMultiPartRequest(600, "baka", "machinename");
        task.setFileString("test");
        connectExecuteDisconnect("http://localhost:1804/abc/", task);
    }

    public static final String CRLF = "\r\n";

    public static final String W_CRLF = CRLF + CRLF;

    public static final String CDFormDataName = "Content-Diposition: form-data; name=";

    /** Default boundary string */
    private static final String BOUNDARY = "yh9iweurp8wer9w743p";

    private static final String DM = "--";

    private final int timeout;

    private final String userName;

    private final String machineName;

    /** Header field */
    private String preHeader = "";

    /** Boundary separator string cache for http multipart request*/
    private String boundaryCacheMid;

    /** Boundary separator string cache for http multipart request*/
    private String boundaryCache;

    private String multiPartFormDataBoundary;

    /** Reference to file string */
    private String file;

    public SillyRawLevelMultiPartRequest(int timeout, String userName, String machineName) {
        this.timeout = timeout;
        this.userName = userName;
        this.machineName = machineName;
        setBoundary(BOUNDARY);
    }

    public void setBoundary(String boundary) {
        this.boundaryCache = new StringBuffer(CRLF).append(DM).append(boundary).toString();
        this.boundaryCacheMid = boundaryCache + CRLF;
        this.preHeader = createPreHeader(boundary);
        this.multiPartFormDataBoundary = "multipart/form-data; boundary=" + boundary;
    }

    private String createPreHeader(String boundary) {
        StringBuffer buf = new StringBuffer(DM);
        buf.append(boundary).append(CRLF).append(CDFormDataName).append("\"source\"").append(W_CRLF);
        buf.append(userName).append("@").append(machineName);
        buf.append(boundaryCacheMid);
        buf.append(CDFormDataName).append("\"FileField\"; filename=\"").append("FileName").append("\"").append(CRLF);
        buf.append("Content-Type: application/xml").append(W_CRLF);
        return buf.toString();
    }

    public void setFileString(String file) {
        this.file = file;
    }

    public void execute(HttpURLConnection urlCon) {
        try {
            urlCon.setRequestMethod("POST");
            urlCon.setDoOutput(true);
            urlCon.setReadTimeout(timeout);
            urlCon.setRequestProperty("Content-Type", multiPartFormDataBoundary);
            OutputStream os = urlCon.getOutputStream();
            DataOutputStream dos = new DataOutputStream(os);
            dos.writeBytes(preHeader);
            dos.writeBytes(file);
            dos.writeBytes(boundaryCache);
            dos.writeBytes(DM);
            dos.writeBytes(CRLF);
            dos.flush();
            dos.close();
            os.close();
            processResponceMessage(urlCon);
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void processResponceMessage(HttpURLConnection urlCon) throws IOException {
        InputStream is = urlCon.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String responseData = null;
        while ((responseData = reader.readLine()) != null) {
            System.out.println(responseData);
        }
        is.close();
    }

    public static final String createStringFromFile(File file) {
        StringBuffer buf = new StringBuffer();
        FileReader in = null;
        BufferedReader br = null;
        try {
            in = new FileReader(file);
            br = new BufferedReader(in);
            String line = null;
            while ((line = br.readLine()) != null) {
                buf.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            Closeables.closeQuietly(br);
            Closeables.closeQuietly(in);
        }
        return buf.toString();
    }

    public static final void connectExecuteDisconnect(String url, SillyRawLevelMultiPartRequest process) {
        URL urlObj = null;
        HttpURLConnection urlCon = null;
        try {
            urlObj = new URL(url);
            urlCon = (HttpURLConnection) urlObj.openConnection();
            process.execute(urlCon);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (urlCon != null) {
                urlCon.disconnect();
            }
        }
    }
}
