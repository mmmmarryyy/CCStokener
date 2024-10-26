package com.google.gdt.eclipse.mobile.android.wizards.helpers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

/**
 * User authentication for C2DM service
 */
public class C2dmAuthentication {

    public static String authToken = "";

    /**
   * Get auth token from server for C2DM service for the account specified by
   * user
   */
    public static void getAuth(String email, String passwd) {
        OutputStreamWriter wr = null;
        BufferedReader rd = null;
        try {
            URL url = new URL("https://www.google.com/accounts/ClientLogin");
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            wr = new OutputStreamWriter(conn.getOutputStream());
            String data = "Email=" + URLEncoder.encode(email, "UTF-8") + "&Passwd=" + URLEncoder.encode(passwd, "UTF-8") + "&accountType=GOOGLE&service=ac2dm";
            wr.write(data);
            wr.flush();
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = rd.readLine()) != null) {
                if (line.startsWith("Auth=")) {
                    authToken = line.substring(5);
                }
            }
        } catch (IOException e) {
        } finally {
            if (wr != null) {
                try {
                    wr.close();
                } catch (IOException e) {
                }
            }
            if (rd != null) {
                try {
                    rd.close();
                } catch (IOException e) {
                }
            }
        }
    }
}
