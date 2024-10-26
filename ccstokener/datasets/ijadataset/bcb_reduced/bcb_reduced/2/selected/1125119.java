package com.synchrona.communication;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import org.apache.commons.codec.binary.Base64;

public class Twitter {

    public void updateTwitter(String userSendMessage, String sender) {
        String username = null;
        if (sender.contains("harini")) {
            username = "harini_sync";
            setTwitter(username, "pass123", userSendMessage);
        } else if (sender.contains("kapila")) {
            username = "kapila_sync";
            setTwitter(username, "pass123", userSendMessage);
        } else if (sender.contains("nilufa")) {
            username = "nilufa_sync";
            setTwitter(username, "pass123", "@harini_sync " + userSendMessage);
        } else if (sender.contains("aruna")) {
            username = "aruna_sync";
            setTwitter(username, "pass123", "@harini_sync " + userSendMessage);
        } else {
            username = "synchrona";
            setTwitter(username, "pass123", userSendMessage);
        }
    }

    private void setTwitter(String username, String password, String message) {
        URL url = null;
        try {
            url = new URL("https://twitter.com/statuses/update.xml");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        URLConnection connection = null;
        try {
            connection = url.openConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setUseCaches(false);
        String authorization = username + ":" + password;
        Base64 en = new Base64();
        String encoded = new String(en.encode(authorization.getBytes())).trim();
        connection.setRequestProperty("Authorization", "Basic " + encoded);
        OutputStreamWriter out = null;
        try {
            out = new OutputStreamWriter(connection.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            out.write("status=" + URLEncoder.encode(message, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        String response;
        try {
            while ((response = in.readLine()) != null) {
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
