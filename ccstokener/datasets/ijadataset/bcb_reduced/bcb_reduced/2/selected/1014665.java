package org.gldap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.StringTokenizer;

public class GoogleContacts {

    private String GC_CONTACT_URL_PREFIX = "http://www.google.com/m8/feeds/contacts/";

    private String GC_CONTACT_URL_SUFFIX = "/full";

    private GoogleSession googleSession = null;

    public GoogleContacts(GoogleSession _googleSession) {
        this.googleSession = _googleSession;
    }

    public String getContacts() throws IOException {
        String output = "";
        URL url = new URL(GC_CONTACT_URL_PREFIX + URLEncoder.encode(googleSession.getEmail(), "UTF-8") + GC_CONTACT_URL_SUFFIX);
        URLConnection conn = url.openConnection();
        conn.setRequestProperty("Authorization", "GoogleLogin auth=" + this.googleSession.google_token);
        conn.connect();
        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        while ((line = rd.readLine()) != null) {
            output = output + line;
        }
        return output;
    }
}
