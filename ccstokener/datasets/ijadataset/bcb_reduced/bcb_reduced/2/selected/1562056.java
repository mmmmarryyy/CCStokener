package kotan.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import kotan.AuthInfo;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

public class DevEntityPersistentManager extends EntityPersistentManager {

    public DevEntityPersistentManager(HttpClientManager clientManager) {
        super(clientManager);
    }

    @Override
    public void authorize(AuthInfo authInfo) throws AuthoricationRequiredException {
        try {
            HttpPost httpPost = new HttpPost("/_ah/login");
            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            nvps.add(new BasicNameValuePair("email", authInfo.getEmail()));
            nvps.add(new BasicNameValuePair("isAdmin", "on"));
            nvps.add(new BasicNameValuePair("continue", "null"));
            nvps.add(new BasicNameValuePair("action", "Log In"));
            httpPost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
            HttpResponse response = clientManager.httpClient.execute(getHttpHost(), httpPost);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                entity.consumeContent();
            }
        } catch (IOException e) {
            throw new AuthoricationRequiredException(e);
        }
    }
}
