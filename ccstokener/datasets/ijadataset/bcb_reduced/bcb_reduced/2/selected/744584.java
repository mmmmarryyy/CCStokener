package org.kisst.cordys.caas.soap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import org.kisst.cordys.caas.main.Environment;

public class NativeCaller extends BaseCaller {

    private static class MyAuthenticator extends Authenticator {

        private String username = null;

        private String password = null;

        private MyAuthenticator() {
            Authenticator.setDefault(this);
        }

        public void setCredentials(String username, String password) {
            this.username = username;
            this.password = password;
        }

        @Override
        public PasswordAuthentication getPasswordAuthentication() {
            Environment.get().debug("getPasswordAuthentication" + "\n\t" + this.getRequestingHost() + "\n\t" + this.getRequestingPort() + "\n\t" + this.getRequestingPrompt() + "\n\t" + this.getRequestingProtocol() + "\n\t" + this.getRequestingScheme() + "\n\t" + this.getRequestingSite() + "\n\t" + this.getRequestingURL() + "\n\t" + this.getRequestorType());
            if (username != null) return new PasswordAuthentication(username, password.toCharArray()); else return super.getPasswordAuthentication();
        }
    }

    protected static final MyAuthenticator myAuthenticator = new MyAuthenticator();

    public NativeCaller(String name) {
        super(name);
    }

    @Override
    public String httpCall(String urlstr, String input) {
        try {
            URL url = new URL(urlstr);
            HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
            byte[] b = input.getBytes();
            httpConn.setRequestProperty("Content-Length", "" + b.length);
            httpConn.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
            httpConn.setRequestMethod("POST");
            httpConn.setDoOutput(true);
            httpConn.setDoInput(true);
            myAuthenticator.setCredentials(username, password);
            OutputStream out = httpConn.getOutputStream();
            out.write(b);
            out.close();
            InputStreamReader isreader = new InputStreamReader(httpConn.getInputStream());
            BufferedReader in = new BufferedReader(isreader);
            StringBuilder result = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) result.append(inputLine);
            in.close();
            return result.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
