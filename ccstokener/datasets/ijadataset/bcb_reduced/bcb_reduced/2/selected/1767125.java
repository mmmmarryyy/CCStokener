package kotan.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import kotan.AuthInfo;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

public class ProdEntityPersistentManager extends EntityPersistentManager {

    public ProdEntityPersistentManager(HttpClientManager clientManager) {
        super(clientManager);
    }

    private static final Logger logger = Logger.getLogger(ProdEntityPersistentManager.class.getName());

    @Override
    public void authorize(AuthInfo authInfo) throws AuthoricationRequiredException {
        String authToken = clientLogin(authInfo);
        login(authToken);
    }

    private String clientLogin(AuthInfo authInfo) throws AuthoricationRequiredException {
        logger.fine("clientLogin.");
        try {
            String url = "https://www.google.com/accounts/ClientLogin";
            HttpPost httpPost = new HttpPost(url);
            ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("accountType", "HOSTED_OR_GOOGLE"));
            params.add(new BasicNameValuePair("Email", authInfo.getEmail()));
            params.add(new BasicNameValuePair("Passwd", new String(authInfo.getPassword())));
            params.add(new BasicNameValuePair("service", "ah"));
            params.add(new BasicNameValuePair("source", "client.kotan-server.appspot.com"));
            httpPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
            HttpResponse response = clientManager.httpClient.execute(httpPost);
            HttpEntity entity = response.getEntity();
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                entity.consumeContent();
                throw new AuthoricationRequiredException(EntityUtils.toString(entity));
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()));
            while (true) {
                String line = reader.readLine();
                if (line == null) break;
                if (line.startsWith("Auth=")) {
                    return line.substring("Auth=".length());
                }
            }
            reader.close();
            throw new AuthoricationRequiredException("Login failure.");
        } catch (IOException e) {
            throw new AuthoricationRequiredException(e);
        }
    }

    private void login(String authToken) throws AuthoricationRequiredException {
        logger.fine("login.");
        try {
            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            nvps.add(new BasicNameValuePair("auth", authToken));
            HttpGet httpGet = new HttpGet("/_ah/login?" + URLEncodedUtils.format(nvps, HTTP.UTF_8));
            HttpResponse response = clientManager.httpClient.execute(getHttpsHost(), httpGet);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                entity.consumeContent();
            }
        } catch (IOException e) {
            throw new AuthoricationRequiredException(e);
        }
    }
}
