package hu.sztaki.lpds.information.service.alice;

import hu.sztaki.lpds.information.data.GuseServiceBean;
import hu.sztaki.lpds.information.inf.InitCommand;
import hu.sztaki.lpds.information.local.PropertyLoader;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;

/**
 * @author krisztian
 */
public class ServicesInitCommandImpl implements InitCommand {

    private HttpPost httpPost;

    private DefaultHttpClient httpclient;

    private BasicCookieStore cookieStore;

    private HttpContext localContext;

    public void run(ServletContext cx, HttpServletRequest request) {
        List<GuseServiceBean> tmp = DH.getI().getAllGuseService();
        for (GuseServiceBean t : tmp) {
            if (!t.getTyp().getSname().equals("information")) if (t.getIurl() != null) {
                open(t.getIurl());
                get(t);
            }
        }
    }

    /**
     * open http connection
     * @param pURL URL
     */
    public void open(String pURL) {
        httpclient = new DefaultHttpClient();
        cookieStore = new BasicCookieStore();
        localContext = new BasicHttpContext();
        httpPost = new HttpPost(pURL);
        localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
    }

    /**
     * send http post request
     * @param t Description of Service
     */
    public void get(GuseServiceBean t) {
        try {
            if (!"".equals(t.getIurl()) && !"null".equals(t.getIurl())) {
                List<NameValuePair> nvps = new ArrayList<NameValuePair>();
                nvps.add(new BasicNameValuePair("is.url", PropertyLoader.getInstance().getProperty("is.url")));
                nvps.add(new BasicNameValuePair("is.id", PropertyLoader.getInstance().getProperty("is.id")));
                nvps.add(new BasicNameValuePair("service.url", t.getUrl()));
                httpPost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
                HttpResponse response = httpclient.execute(httpPost);
                HttpEntity entity = response.getEntity();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
