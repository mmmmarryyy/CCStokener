package com.untilov.gb.se.http.auth;

import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Vector;
import javax.net.ssl.HttpsURLConnection;
import com.untilov.gb.http.auth.AuthenticatorIF;
import com.untilov.gb.http.cookies.CookiesManagerIF;
import com.untilov.gb.se.http.cookies.CookiesManagerSEImpl;

/**
 * HttpsURLConnection implementation of authentication. Tries to authenticates
 * over gmail, and populates cookies - they will be used in data retrival
 * process.
 * 
 * @author iuntilov
 */
public class AuthenticatorSEImpl implements AuthenticatorIF {

    private String url;

    CookiesManagerIF cookiesManager;

    public AuthenticatorSEImpl(String url) {
        HttpURLConnection.setFollowRedirects(false);
        this.url = url;
        setCookiesManager(new CookiesManagerSEImpl());
    }

    private HttpsURLConnection connect(String login, String password) throws Exception {
        String body = "Email=" + login + "&Passwd=" + password + "&service=bookmarks";
        URL url = new URL(this.url);
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        prepareConnection(body.length(), connection);
        OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
        writer.write(body);
        writer.flush();
        writer.close();
        return connection;
    }

    private void prepareConnection(int contentLength, HttpsURLConnection connection) throws ProtocolException {
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.addRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setAllowUserInteraction(false);
        connection.setRequestProperty("Content-length", contentLength + "");
    }

    public boolean authenticate(String login, String password) throws Exception {
        HttpsURLConnection connection = connect(login, password);
        if (connection.getResponseCode() != 302) {
            System.out.println("response code: " + connection.getResponseCode());
            return false;
        }
        cookiesManager.processCookies(connection);
        if (!cookiesManager.isCookiesValid()) {
            Enumeration e = cookiesManager.getCookies().elements();
            while (e.hasMoreElements()) {
                System.out.println("cookie: " + e.nextElement());
            }
            return false;
        }
        return true;
    }

    public void setCookiesManager(CookiesManagerIF cookiesManager) {
        this.cookiesManager = cookiesManager;
    }

    public CookiesManagerIF getCookiesManager() {
        return cookiesManager;
    }
}
