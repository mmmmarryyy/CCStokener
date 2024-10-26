package com.tscribble.bitleech.plugins.http.net.impl.basichttp;

import static java.lang.System.out;
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;
import com.tscribble.bitleech.core.download.auth.AuthManager;
import com.tscribble.bitleech.core.download.auth.Authentication;
import com.tscribble.bitleech.core.download.auth.IAuthProvider;
import com.tscribble.bitleech.core.download.protocol.AbstractProtocolClient;
import com.tscribble.bitleech.plugins.http.auth.HTTPAuthProvider;

public class Basic_HttpClient extends AbstractProtocolClient {

    private HttpURLConnection con;

    private HTTPAuthProvider aprovider;

    private Authentication auth;

    public Basic_HttpClient() {
        Authenticator.setDefault(new MyAuthenticator());
    }

    public void getDownloadInfo() throws Exception {
        URL url = new URL(getURL());
        con = (HttpURLConnection) url.openConnection();
        con.setRequestProperty("User-Agent", "test");
        con.setRequestProperty("Accept", "*/*");
        con.setRequestProperty("Range", "bytes=0-");
        con.setRequestMethod("HEAD");
        con.setUseCaches(false);
        con.connect();
        con.disconnect();
        if (mustRedirect()) secureRedirect();
        AuthManager.putAuth(getSite(), auth);
        url = con.getURL();
        setURL(url.toString());
        setSize(Long.parseLong(con.getHeaderField("Content-Length")));
        setResumable(con.getResponseCode() == 206);
        setLastModified(con.getLastModified());
        setRangeEnd(getSize() - 1);
    }

    public void setAuthProvider(IAuthProvider authProvider) {
        this.aprovider = (HTTPAuthProvider) authProvider;
    }

    @Override
    public IAuthProvider getAuthProvider() {
        return aprovider;
    }

    public void initGet() throws Exception {
        URL url = new URL(getURL());
        con = (HttpURLConnection) url.openConnection();
        con.setRequestProperty("Accept", "*/*");
        con.setRequestProperty("Range", "bytes=" + getPosition() + "-" + getRangeEnd());
        con.setUseCaches(false);
        con.connect();
        setInputStream(con.getInputStream());
    }

    private boolean mustRedirect() throws IOException {
        int code = con.getResponseCode();
        if (code == HttpURLConnection.HTTP_MOVED_PERM || code == HttpURLConnection.HTTP_MOVED_TEMP) {
            return true;
        } else return false;
    }

    private void secureRedirect() throws IOException {
        URL url = new URL(con.getHeaderField("Location"));
        out.println("Secure Redirect to: " + url);
        con = (HttpsURLConnection) url.openConnection();
    }

    private class MyAuthenticator extends Authenticator {

        protected PasswordAuthentication getPasswordAuthentication() {
            String realm = con.getHeaderField("WWW-Authenticate");
            aprovider.setSite(getSite());
            aprovider.setRealm(realm);
            auth = aprovider.promptAuthentication();
            return new PasswordAuthentication(auth.getUser(), auth.getPassword().toCharArray());
        }
    }

    public void disconnect() {
        try {
            con.disconnect();
        } catch (Exception e) {
        } finally {
        }
    }
}
