package csiebug.web.webapp.example;

import java.io.IOException;
import java.util.ArrayList;
import org.apache.http.HttpResponse;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import csiebug.service.ServiceException;
import csiebug.util.AssertUtility;
import csiebug.web.webapp.BasicAction;

public class HttpClientAction extends BasicAction {

    private static final long serialVersionUID = 1L;

    public String main() throws Exception {
        makeControl();
        return FORMLOAD;
    }

    private void makeControl() throws ClientProtocolException, IOException {
        try {
            setRequestAttribute("FunctionName", getFunctionName());
        } catch (ServiceException se) {
            writeErrorLog(se);
            if (AssertUtility.isNotNullAndNotSpace(getFunctionId())) {
                setRequestAttribute("FunctionName", getFunctionId());
            } else {
                setRequestAttribute("FunctionName", "頁面標題");
            }
        }
        syncXplanner("TESTBU", "TESTDEPT", "TESTCUST", "TESTPJ0910", "C100", "測試", "garry_huang");
    }

    private boolean loginSyncServer(HttpContext localContext, DefaultHttpClient httpClient, HttpPost httpPost) throws ClientProtocolException, IOException {
        UsernamePasswordCredentials creds = new UsernamePasswordCredentials("pmpadmin", "admin");
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(new AuthScope("gsssvn", AuthScope.ANY_PORT), creds);
        httpClient.setCredentialsProvider(credsProvider);
        HttpResponse response = httpClient.execute(httpPost, localContext);
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            return true;
        } else {
            System.out.println("login失敗:" + response.getStatusLine());
            return false;
        }
    }

    private void syncXplanner(String buId, String deptId, String customerId, String projectId, String contractId, String note, String adminIds) throws ClientProtocolException, IOException {
        HttpParams params = new BasicHttpParams();
        ConnManagerParams.setMaxTotalConnections(params, 200);
        ConnPerRouteBean connPerRoute = new ConnPerRouteBean(20);
        HttpHost gsssvn = new HttpHost("gsssvn", 80);
        connPerRoute.setMaxForRoute(new HttpRoute(gsssvn), 50);
        ConnManagerParams.setMaxConnectionsPerRoute(params, connPerRoute);
        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        ThreadSafeClientConnManager cm = new ThreadSafeClientConnManager(params, schemeRegistry);
        HttpContext localContext = new BasicHttpContext();
        DefaultHttpClient httpClient = new DefaultHttpClient(cm, params);
        HttpPost httpPost = new HttpPost("http://gsssvn/cgi-bin/test/xp.pl");
        if (loginSyncServer(localContext, httpClient, httpPost)) {
            ArrayList<NameValuePair> pairList = new ArrayList<NameValuePair>();
            pairList.add(new BasicNameValuePair("action", "xplanner"));
            pairList.add(new BasicNameValuePair("bu", buId));
            pairList.add(new BasicNameValuePair("dept", deptId));
            pairList.add(new BasicNameValuePair("customer", customerId));
            pairList.add(new BasicNameValuePair("project", projectId));
            pairList.add(new BasicNameValuePair("contract", contractId));
            pairList.add(new BasicNameValuePair("note", note));
            pairList.add(new BasicNameValuePair("admin", adminIds));
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(pairList, "UTF-8");
            httpPost.setEntity(entity);
            HttpResponse response = httpClient.execute(httpPost);
            String responseString = EntityUtils.toString(response.getEntity());
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                System.out.println(responseString);
                if (responseString.indexOf("失敗") != -1) {
                    System.out.println(false);
                } else {
                    System.out.println(responseString.indexOf("不存在") == -1);
                }
            } else {
                System.out.println(response.getStatusLine());
            }
        }
    }
}
