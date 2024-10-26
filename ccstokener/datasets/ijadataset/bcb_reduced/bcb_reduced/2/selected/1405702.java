package httptester;

import httptester.Configuration.MethodOfProxyAuthentication;
import httptester.Configuration.MethodOfProxySelection;
import java.io.IOException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.util.List;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.ProxySelectorRoutePlanner;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.ibex.util.IOUtil;
import com.btr.proxy.search.ProxySearch;

public class HttpTester {

    final Configuration conf;

    MessageParams message;

    public HttpTester(Configuration conf) {
        this.conf = conf;
    }

    public void run() throws ConfProblem {
        try {
            message = conf.constructRequest();
            doApacheHttpRequest(conf.getMethodOfProxySelection(), conf.getMethodOfProxyAuthentication());
        } catch (ConfProblem p) {
            p.print();
            return;
        }
    }

    private void log(String s) {
        System.err.println(s);
    }

    public void doApacheHttpRequest(MethodOfProxySelection proxySelect, MethodOfProxyAuthentication proxyAuthenticate) throws ConfProblem {
        log("-------------------- Request ------------------------------");
        log("proxy selection method:        " + proxySelect);
        log("proxy authentication method:   " + proxyAuthenticate);
        DefaultHttpClient httpclient = new DefaultHttpClient();
        switch(proxySelect) {
            case manual:
                HttpHost proxyHost = null;
                try {
                    proxyHost = conf.constructManualProxy();
                } catch (ConfProblem p) {
                    p.print();
                    return;
                }
                httpclient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxyHost);
                break;
            case detect_proxyvole:
                ProxySearch proxySearch = ProxySearch.getDefaultProxySearch();
                ProxySelector myProxySelector = proxySearch.getProxySelector();
                ProxySelector.setDefault(myProxySelector);
            case detect_builtin:
                ProxySelectorRoutePlanner routePlanner = new ProxySelectorRoutePlanner(httpclient.getConnectionManager().getSchemeRegistry(), ProxySelector.getDefault());
                httpclient.setRoutePlanner(routePlanner);
                List<Proxy> proxies = ProxySelector.getDefault().select(message.uri);
                log(proxies.size() + " discovered proxies:- ");
                for (Proxy p : proxies) {
                    log("    " + p);
                }
                break;
            default:
        }
        switch(proxyAuthenticate) {
            case ntlm:
                {
                    NTCredentials creds = conf.constructNtlmCredentials();
                    httpclient.getCredentialsProvider().setCredentials(AuthScope.ANY, creds);
                    break;
                }
            case basic:
                {
                    UsernamePasswordCredentials credentials = conf.constructBasicCredentials();
                    httpclient.getCredentialsProvider().setCredentials(AuthScope.ANY, credentials);
                    break;
                }
            case none:
            default:
        }
        HttpParams params = new BasicHttpParams();
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(params, "UTF-8");
        HttpProtocolParams.setUserAgent(params, "Vexi");
        HttpProtocolParams.setUseExpectContinue(params, true);
        BasicHttpEntityEnclosingRequest request = new BasicHttpEntityEnclosingRequest("POST", message.uriString);
        ByteArrayEntity entity = new ByteArrayEntity(message.message.getBytes());
        entity.setContentType("text/xml");
        request.setEntity(entity);
        HttpHost host = new HttpHost(message.host, message.port);
        request.setParams(params);
        try {
            log("-------------------- Response ------------------------------");
            HttpResponse response = httpclient.execute(host, request);
            byte[] bytes = IOUtil.toByteArray(response.getEntity().getContent());
            int statusCode = response.getStatusLine().getStatusCode();
            log("Status Code: " + statusCode);
            log(new String(bytes));
        } catch (IOException e) {
            System.err.println("[Problem] Exception thrown performing request");
            e.printStackTrace();
        }
    }
}
