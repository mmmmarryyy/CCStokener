package uk.co.altv.simpledb.connector;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import uk.co.altv.simpledb.IReply;
import uk.co.altv.simpledb.RawReply;

/**
 * @author niki
 *
 */
public class SimpleURLConnector implements IConnector {

    private int maxRetries = 3;

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public boolean canDoGet() {
        return true;
    }

    public boolean canDoPost() {
        return true;
    }

    public IReply doGet(URL url) throws IOException {
        return recurseGet(url, 0);
    }

    private IReply recurseGet(URL url, int retry) throws IOException {
        RawReply rep = (RawReply) processReply((HttpURLConnection) url.openConnection());
        if (rep.getSuccess() && (rep.getHTTPStatusCode() == HttpURLConnection.HTTP_OK)) {
            return rep;
        } else if ((rep.getHTTPStatusCode() == HttpURLConnection.HTTP_UNAVAILABLE) || (rep.getHTTPStatusCode() == HttpURLConnection.HTTP_INTERNAL_ERROR)) {
            if (retry < this.maxRetries) {
                try {
                    Thread.sleep(((retry ^ 2) * 75) + 50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return recurseGet(url, retry++);
            }
        }
        return rep;
    }

    public IReply doPost(URL url, String postBody) throws IOException {
        return recursePost(url, postBody, 0);
    }

    private IReply recursePost(URL url, String postBody, int retry) throws IOException {
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setDoOutput(true);
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        OutputStreamWriter writer = new OutputStreamWriter(new BufferedOutputStream(con.getOutputStream(), 8192));
        writer.write(postBody);
        writer.close();
        RawReply rep = (RawReply) processReply(con);
        if (rep.getSuccess() && (rep.getHTTPStatusCode() == HttpURLConnection.HTTP_OK)) {
            return rep;
        } else if ((rep.getHTTPStatusCode() == HttpURLConnection.HTTP_UNAVAILABLE) || (rep.getHTTPStatusCode() == HttpURLConnection.HTTP_INTERNAL_ERROR)) {
            if (retry < this.maxRetries) {
                try {
                    Thread.sleep(((retry ^ 2) * 75) + 50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return recursePost(url, postBody, retry++);
            }
        }
        return rep;
    }

    protected IReply processReply(HttpURLConnection con) throws IOException {
        IReply reply = new RawReply();
        reply.setHTTPContentType(con.getContentType());
        reply.setHTTPContentEncoding(con.getContentEncoding());
        reply.setHTTPContentLength(con.getContentLength());
        boolean success = false;
        InputStream in = con.getErrorStream();
        if (in == null) {
            in = con.getInputStream();
            success = true;
        }
        reply.setSuccess(success);
        reply.setHTTPStatusMessage(con.getResponseMessage());
        reply.setHTTPStatusCode(con.getResponseCode());
        reply.setHTTPHeaderFields(con.getHeaderFields());
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, "utf-8"), 8192);
        char[] buffer = new char[512];
        StringBuilder bu = new StringBuilder(4096);
        int len = 0;
        while ((len = reader.read(buffer)) != -1) {
            bu.append(buffer, 0, len);
        }
        reader.close();
        reply.setPayload(bu.toString());
        return reply;
    }
}
