package sk.sigp.aobot.client;

import java.io.IOException;
import java.net.URISyntaxException;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;

/**
 * apache http client main object with customized properties custom agent for
 * being invisible
 * 
 * @author Oest
 * 
 */
public class CrawlerHttpClient extends DefaultHttpClient {

    /**
	 * fetches page from url (stupid version)
	 * 
	 * @param url
	 * @return
	 * @throws IllegalStateException
	 * @throws IOException
	 * @throws HttpException
	 * @throws InterruptedException
	 * @throws URISyntaxException
	 */
    public String httpToStringStupid(String url) throws IllegalStateException, IOException, HttpException, InterruptedException, URISyntaxException {
        String pageDump = null;
        getParams().setParameter(ClientPNames.COOKIE_POLICY, org.apache.http.client.params.CookiePolicy.BROWSER_COMPATIBILITY);
        HttpGet httpget = new HttpGet(url);
        httpget.getParams().setParameter(HttpConnectionParams.CONNECTION_TIMEOUT, 10000);
        HttpResponse response = execute(httpget);
        HttpEntity entity = response.getEntity();
        pageDump = IOUtils.toString(entity.getContent(), "UTF-8");
        return pageDump;
    }

    protected HttpParams createHttpParams() {
        HttpParams params = new BasicHttpParams();
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);
        HttpProtocolParams.setUseExpectContinue(params, true);
        HttpProtocolParams.setUserAgent(params, "OestRC");
        return params;
    }
}
