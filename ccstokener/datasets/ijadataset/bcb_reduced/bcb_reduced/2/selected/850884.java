package de.guidoludwig.jtrade.expimp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Authenticator;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

/**
 * Updates the WEB interface with the current data
 * 
 *
 */
public class WebConnection {

    private String sql;

    public WebConnection(String sql) {
        this.sql = sql;
    }

    /**
	 * FIXME : exception handling
	 * @throws Exception
	 */
    public void update() {
        Authenticator.setDefault(new MyAuthenticator());
        URL url = null;
        try {
            url = new URL("http://trade.gigabass.de/update/update.php");
        } catch (MalformedURLException e) {
            handleException(e);
            return;
        }
        URLConnection conn;
        try {
            conn = url.openConnection();
        } catch (IOException e) {
            handleException(e);
            return;
        }
        conn.setDoOutput(true);
        OutputStreamWriter wr = null;
        try {
            wr = new OutputStreamWriter(conn.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            wr.write("sql=" + URLEncoder.encode(sql, "UTF-8") + "\n");
            wr.flush();
        } catch (IOException e) {
            handleException(e);
        }
        BufferedReader rd = null;
        try {
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = rd.readLine()) != null) {
            }
        } catch (IOException e) {
            handleException(e);
        }
        try {
            wr.close();
        } catch (IOException e) {
            handleException(e);
        }
        try {
            rd.close();
        } catch (IOException e) {
            handleException(e);
        }
    }

    private void handleException(Exception e) {
    }

    /**
	 * FIXME : password / username ...
	 */
    private static class MyAuthenticator extends Authenticator {

        protected PasswordAuthentication getPasswordAuthentication() {
            String promptString = getRequestingPrompt();
            String hostname = getRequestingHost();
            InetAddress ipaddr = getRequestingSite();
            int port = getRequestingPort();
            String username = "myusername";
            String password = "mypassword";
            return new PasswordAuthentication(username, password.toCharArray());
        }
    }
}
