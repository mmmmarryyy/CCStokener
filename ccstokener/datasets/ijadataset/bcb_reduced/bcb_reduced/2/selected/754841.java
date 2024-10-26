package multisms;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.TimerTask;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;
import provider.Proxy;

public class CheckTask extends TimerTask {

    private int proxyMaxDelay = 5000;

    private int port;

    private String host;

    private MultiSMS ref;

    public CheckTask(MultiSMS ref, int port, String host) {
        this.port = port;
        this.host = host;
        this.ref = ref;
    }

    @Override
    public void run() {
        long time1 = System.currentTimeMillis();
        try {
            DefaultHttpClient client = new DefaultHttpClient();
            HttpHost prox = new HttpHost(host, port);
            client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, prox);
            String target = "http://www.google.de/intl/en_com/images/srpr/logo1w.png";
            HttpGet httpget = new HttpGet(target);
            HttpResponse response = client.execute(httpget);
            HttpEntity e = response.getEntity();
            BufferedReader rd = new BufferedReader(new InputStreamReader(e.getContent()));
            ;
            while (rd.readLine() != null) {
            }
            long time2 = System.currentTimeMillis();
            if ((time2 - time1) < proxyMaxDelay) {
                System.out.println("Added " + host + ":" + port + " with " + (time2 - time1) + "ms");
                ref.proxyList.add(new Proxy(host, port));
            }
        } catch (ClientProtocolException e) {
        } catch (IOException e) {
        }
    }
}
