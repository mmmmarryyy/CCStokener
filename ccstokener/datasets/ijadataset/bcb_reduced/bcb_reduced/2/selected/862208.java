package net.sf.ahtutils.controller.factory.xml.cloud.facebook;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;
import java.util.GregorianCalendar;
import net.sf.ahtutils.controller.servlet.facebook.FbRedirector;
import net.sf.ahtutils.xml.cloud.facebook.App;
import net.sf.ahtutils.xml.cloud.facebook.Token;
import net.sf.exlp.util.DateUtil;
import net.sf.exlp.util.exception.ExlpXpathNotFoundException;
import net.sf.exlp.util.exception.ExlpXpathNotUniqueException;
import net.sf.exlp.xml.xpath.NetXpath;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccessTokenFactory {

    static final Logger logger = LoggerFactory.getLogger(AccessTokenFactory.class);

    private String appId;

    private String appSecret;

    private String appURL;

    private static final String keyToken = "access_token=";

    private static final String keyExpires = "&expires=";

    public AccessTokenFactory(String appId, String appSecret, String appURL) {
        this.appId = appId;
        this.appSecret = appSecret;
        this.appURL = appURL;
    }

    public AccessTokenFactory(App app) {
        try {
            this.appId = app.getAppId();
            this.appURL = NetXpath.getUrl(app.getRedirect().getUrl(), FbRedirector.Code.fbauth.toString()).getValue();
            this.appSecret = app.getSecret();
        } catch (ExlpXpathNotFoundException e) {
            logger.error("", e);
        } catch (ExlpXpathNotUniqueException e) {
            logger.error("", e);
        }
    }

    public Token request(String code) {
        Token token = null;
        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpGet httpGet = createGet(code);
        try {
            HttpResponse response = httpclient.execute(httpGet);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                token = toXml(EntityUtils.toString(entity));
                System.out.println();
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return token;
    }

    public Token toXml(String httpToken) {
        int indexTstart = keyToken.length();
        int indexTend = httpToken.indexOf(keyExpires);
        int seconds = Integer.valueOf(httpToken.substring(indexTend + keyExpires.length(), httpToken.length()).trim());
        GregorianCalendar gc = new GregorianCalendar();
        gc.setTime(new Date());
        gc.add(GregorianCalendar.SECOND, seconds);
        Token token = new Token();
        token.setExpiresIn(seconds);
        token.setValue(httpToken.substring(indexTstart, indexTend));
        token.setExpires(DateUtil.getXmlGc4D(gc.getTime()));
        return token;
    }

    private HttpGet createGet(String code) {
        StringBuffer sb = new StringBuffer();
        sb.append("https://graph.facebook.com/oauth/access_token?");
        sb.append("client_id=" + appId);
        sb.append("&redirect_uri=" + appURL);
        sb.append("&client_secret=" + appSecret);
        sb.append("&code=" + code);
        URI uri = null;
        try {
            URL url = new URL(sb.toString());
            uri = new URI(url.getProtocol(), url.getHost(), url.getPath(), url.getQuery(), null);
        } catch (URISyntaxException e) {
            logger.error("", e);
        } catch (MalformedURLException e) {
            logger.error("", e);
        }
        return new HttpGet(uri);
    }
}
