package com.dddforandroid.database.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;

public class SyncServerDatabase {

    private String responseMessage;

    public String createString(String content) {
        String output = null;
        String begin = "{\"notes\":";
        String end = "}";
        output = begin + content + end;
        return output;
    }

    public String getData(DefaultHttpClient httpclient) {
        try {
            HttpGet get = new HttpGet("http://3dforandroid.appspot.com/api/v1/note");
            get.setHeader("Content-Type", "application/json");
            get.setHeader("Accept", "*/*");
            HttpResponse response = httpclient.execute(get);
            HttpEntity entity = response.getEntity();
            InputStream instream = entity.getContent();
            responseMessage = read(instream);
            if (instream != null) instream.close();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return responseMessage;
    }

    public String deleteData(String id, DefaultHttpClient httpclient) {
        try {
            HttpDelete del = new HttpDelete("http://3dforandroid.appspot.com/api/v1/note/delete/" + id);
            del.setHeader("Content-Type", "application/json");
            del.setHeader("Accept", "*/*");
            HttpResponse response = httpclient.execute(del);
            HttpEntity entity = response.getEntity();
            InputStream instream;
            instream = entity.getContent();
            responseMessage = read(instream);
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return responseMessage;
    }

    public String putData(String id, String updatedNote, DefaultHttpClient httpclient) {
        try {
            HttpPut put = new HttpPut("http://3dforandroid.appspot.com/api/v1/note/update/" + id);
            StringEntity se = new StringEntity(updatedNote);
            se.setContentEncoding(HTTP.UTF_8);
            se.setContentType("application/json");
            put.setEntity(se);
            put.setHeader("Content-Type", "application/json");
            put.setHeader("Accept", "*/*");
            HttpResponse response = httpclient.execute(put);
            HttpEntity entity = response.getEntity();
            InputStream instream;
            instream = entity.getContent();
            responseMessage = read(instream);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return responseMessage;
    }

    public String postData(String result, DefaultHttpClient httpclient) {
        try {
            HttpPost post = new HttpPost("http://3dforandroid.appspot.com/api/v1/note/create");
            StringEntity se = new StringEntity(result);
            se.setContentEncoding(HTTP.UTF_8);
            se.setContentType("application/json");
            post.setEntity(se);
            post.setHeader("Content-Type", "application/json");
            post.setHeader("Accept", "*/*");
            HttpResponse response = httpclient.execute(post);
            HttpEntity entity = response.getEntity();
            InputStream instream;
            instream = entity.getContent();
            responseMessage = read(instream);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return responseMessage;
    }

    private String read(InputStream instream) {
        StringBuilder sb = null;
        try {
            sb = new StringBuilder();
            BufferedReader r = new BufferedReader(new InputStreamReader(instream));
            for (String line = r.readLine(); line != null; line = r.readLine()) {
                sb.append(line);
            }
            instream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }
}
