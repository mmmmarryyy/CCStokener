package org.gldap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.StringTokenizer;
import org.apache.log4j.Logger;

public class GoogleSession {

    private static Logger log4j = Logger.getLogger(GoogleSession.class);

    private String GC_AUTHENTICATION_URL = "https://www.google.com/accounts/ClientLogin";

    private String GC_ACCOUNT_TYPE = "GOOGLE";

    private String GC_SOURCE = "gldap_0.01";

    private String GC_SERVICE = "cp";

    public String google_token = null;

    private String email;

    private String password;

    public GoogleSession(String _email, String _password) throws Exception {
        log4j.debug("GoogleSession objected instantiated with email: " + _email);
        this.email = _email;
        this.password = _password;
        getAuthenticationToken();
    }

    private void getAuthenticationToken() throws Exception {
        log4j.debug("Attempting to retrieve Google auth ticket");
        String data = URLEncoder.encode("accountType", "UTF-8") + "=" + URLEncoder.encode(GC_ACCOUNT_TYPE, "UTF-8");
        data += "&" + URLEncoder.encode("Email", "UTF-8") + "=" + URLEncoder.encode(this.email, "UTF-8");
        data += "&" + URLEncoder.encode("Passwd", "UTF-8") + "=" + URLEncoder.encode(this.password, "UTF-8");
        data += "&" + URLEncoder.encode("service", "UTF-8") + "=" + URLEncoder.encode(GC_SERVICE, "UTF-8");
        data += "&" + URLEncoder.encode("source", "UTF-8") + "=" + URLEncoder.encode(GC_SOURCE, "UTF-8");
        URL url = new URL(GC_AUTHENTICATION_URL);
        URLConnection conn = url.openConnection();
        conn.setDoOutput(true);
        OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
        wr.write(data);
        wr.flush();
        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        while ((line = rd.readLine()) != null) {
            StringTokenizer st = new StringTokenizer(line, "=");
            while (st.hasMoreTokens()) {
                String s = st.nextToken();
                if (s.equals("Auth")) {
                    log4j.debug("google session retrieved successfully");
                    this.google_token = st.nextToken();
                    break;
                }
            }
        }
        wr.close();
        rd.close();
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
