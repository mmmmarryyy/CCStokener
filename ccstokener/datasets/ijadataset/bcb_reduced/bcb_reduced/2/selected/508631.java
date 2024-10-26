package com.prolix.editor.oics.get;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

public class OICSGetFile {

    private HttpClient client;

    private String url;

    private String path;

    public OICSGetFile(String file, String path) {
        this.url = file;
        this.path = path;
    }

    private InputStream execute() {
        client = new DefaultHttpClient();
        HttpGet get = new HttpGet(url);
        System.out.println("GET FILE: " + this.url);
        try {
            HttpResponse response = client.execute(get);
            HttpEntity entity = response.getEntity();
            return entity.getContent();
        } catch (ClientProtocolException e) {
        } catch (IOException e) {
        }
        return null;
    }

    private void close() {
        client.getConnectionManager().shutdown();
    }

    public File getFile() {
        InputStream ip = execute();
        try {
            FileOutputStream fos = new FileOutputStream(new File(this.path));
            int zeichen = 0;
            while ((zeichen = ip.read()) >= 0) {
                fos.write(zeichen);
            }
            fos.flush();
            fos.close();
            ip.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        close();
        return new File(this.path);
    }
}
