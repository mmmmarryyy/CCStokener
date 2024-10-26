package com.skillworld.webapp.model.facebookservice;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public class FacebookServiceImpl implements FacebookService {

    public boolean isValidAuthorizationToken(String uid, String token) {
        boolean resp = false;
        StringBuilder url = new StringBuilder();
        String rest = null;
        url.append("https://graph.facebook.com/" + uid);
        url.append("?access_token=" + token);
        try {
            rest = fazHttpRequest(url.toString());
            if (rest != null) {
                resp = true;
            }
        } catch (Exception e) {
            System.out.println("[isAuthorized]" + e);
        }
        return resp;
    }

    private String fazHttpRequest(String u) {
        StringBuilder str = new StringBuilder();
        URL url = null;
        URLConnection urlC = null;
        try {
            url = new URL(u.toString());
            urlC = url.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(urlC.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                str.append(inputLine);
            }
            in.close();
        } catch (Exception e) {
            System.out.println("[fazHttpRequest]" + e);
        }
        return (str.length() > 0) ? str.toString() : null;
    }
}
