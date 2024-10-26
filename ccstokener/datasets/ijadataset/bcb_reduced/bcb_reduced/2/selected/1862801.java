package org.blueoxygen.cimande.httpclient;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

public class HTTPPostPerson {

    public HTTPPostPerson() {
    }

    public static void main(String[] args) {
        String scheme = "http";
        String host = "localhost";
        int port = 8081;
        String contextPath = "Cimande";
        List<NameValuePair> formparams = new ArrayList<NameValuePair>();
        formparams.add(new BasicNameValuePair("username", "Vi2"));
        formparams.add(new BasicNameValuePair("password", "Vi2"));
        formparams.add(new BasicNameValuePair("firstName", "Vi2"));
        formparams.add(new BasicNameValuePair("lastName", "Ad2"));
        formparams.add(new BasicNameValuePair("gender", "true"));
        formparams.add(new BasicNameValuePair("address", "oia"));
        formparams.add(new BasicNameValuePair("status", "single"));
        formparams.add(new BasicNameValuePair("birthDate", "01/01/2000"));
        try {
            URI uri = URIUtils.createURI(scheme, host, port, contextPath + "/person.json", null, null);
            HttpPost httpPost = new HttpPost(uri);
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams);
            httpPost.setEntity(entity);
            System.out.println(uri);
            HttpClient client = new DefaultHttpClient();
            HttpResponse response = client.execute(httpPost);
            System.out.println(EntityUtils.toString(response.getEntity()));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }
}
