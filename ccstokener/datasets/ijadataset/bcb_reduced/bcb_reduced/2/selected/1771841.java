package sk.sigp.tetras.findemail.crawl;

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
import org.apache.log4j.Logger;
import sk.sigp.tetras.service.PreferenceService;

/**
 * apache http client main object with customized properties cutom agent for
 * being invisible
 * 
 * @author mstafurik
 * 
 */
public class CrawlerHttpClient extends DefaultHttpClient {

    private static Logger LOG = Logger.getLogger(CrawlerHttpClient.class);

    private PreferenceService preferenceService;

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
        LOG.info("Loading URL: " + url);
        String pageDump = null;
        getParams().setParameter(ClientPNames.COOKIE_POLICY, org.apache.http.client.params.CookiePolicy.BROWSER_COMPATIBILITY);
        getParams().setParameter(HttpConnectionParams.SO_TIMEOUT, getSocketTimeout());
        HttpGet httpget = new HttpGet(url);
        httpget.getParams().setParameter(HttpConnectionParams.SO_TIMEOUT, getSocketTimeout());
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
        HttpProtocolParams.setUserAgent(params, getAgent());
        return params;
    }

    public CrawlerHttpClient() {
    }

    private String getAgent() {
        return getPreferenceService().getCrawlUserAgent();
    }

    private int getSocketTimeout() {
        return getPreferenceService().getSearchSocketTimeout();
    }

    public PreferenceService getPreferenceService() {
        return preferenceService;
    }

    public void setPreferenceService(PreferenceService preferenceService) {
        this.preferenceService = preferenceService;
    }
}
