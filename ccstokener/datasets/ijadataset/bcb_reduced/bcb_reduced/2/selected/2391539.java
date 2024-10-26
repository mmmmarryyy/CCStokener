package net.sf.leechget.service.megaupload;

import java.io.IOException;
import java.io.InputStream;
import net.sf.leechget.service.api.Logger;
import net.sf.leechget.util.IOUtil;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.StringBody;

/**
 * @author Rogiel
 * 
 */
public class MegaUploadLogger implements Logger {

    private final HttpClient client;

    private final String username;

    private final String password;

    public MegaUploadLogger(final HttpClient client, final String username, final String password) {
        this.client = client;
        this.username = username;
        this.password = password;
    }

    @Override
    public boolean login() throws ClientProtocolException, IOException {
        final HttpPost login = new HttpPost("http://www.megaupload.com/?c=login");
        final MultipartEntity entity = new MultipartEntity();
        login.setEntity(entity);
        entity.addPart("login", new StringBody("1"));
        entity.addPart("username", new StringBody(username));
        entity.addPart("password", new StringBody(password));
        final HttpResponse response = this.client.execute(login);
        final InputStream in = response.getEntity().getContent();
        final String body = IOUtil.getString(in);
        in.close();
        if (body.contains("Username and password do " + "not match. Please try again!")) {
            return false;
        }
        return true;
    }

    @Override
    public boolean logout() throws ClientProtocolException, IOException {
        final HttpPost login = new HttpPost("http://www.megaupload.com/");
        final MultipartEntity entity = new MultipartEntity();
        entity.addPart("logout", new StringBody("1"));
        login.setEntity(entity);
        final HttpResponse response = this.client.execute(login);
        response.getEntity().consumeContent();
        return true;
    }
}
