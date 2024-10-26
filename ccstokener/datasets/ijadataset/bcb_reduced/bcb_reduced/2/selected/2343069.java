package pl.szpadel.android.gadu;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.StringTokenizer;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.util.InetAddressUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import android.content.Context;

/** Stupid simple lib doing the HTPP magic to get the server addresses */
public class HttpServiceConnector {

    private static final String TAG = "HttpServiceConnector";

    static Collection<InetSocketAddress> getAddresses(Context ctx, long userId) throws Exception {
        AGLog.d(TAG, "Connecting to HTTP service to obtain IP addresses");
        String host = (String) ctx.getResources().getText(R.string.gg_webservice_addr);
        String ver = App.getInstance().getGGClientVersion();
        String url = host + "?fmnumber=" + Long.toString(userId) + "&lastmsg=0&version=" + ver;
        HttpClient httpClient = new DefaultHttpClient();
        AGLog.d(TAG, "connecting to http service at " + url);
        HttpGet request = new HttpGet(url);
        HttpResponse response = httpClient.execute(request);
        AGLog.d(TAG, "response status:" + response.getStatusLine().getReasonPhrase());
        HttpEntity ent = response.getEntity();
        if (ent == null) {
            AGLog.e(TAG, "No response entity");
            throw new ClientProtocolException("No response entity");
        }
        InputStream content = ent.getContent();
        BufferedReader reader = new BufferedReader(new InputStreamReader(content));
        String line = reader.readLine();
        AGLog.d(TAG, "response: " + line);
        StringTokenizer tokenizer = new StringTokenizer(line, " ");
        @SuppressWarnings("unused") String status = tokenizer.nextToken();
        @SuppressWarnings("unused") String unknown = tokenizer.nextToken();
        ArrayList<InetSocketAddress> result = new ArrayList<InetSocketAddress>();
        while (tokenizer.hasMoreTokens()) {
            StringTokenizer addrport = new StringTokenizer(tokenizer.nextToken(), ":");
            String addrStr = addrport.nextToken();
            if (InetAddressUtils.isIPv4Address(addrStr)) {
                AGLog.d(TAG, "Address decoded successfully: " + addrStr);
            } else {
                AGLog.w(TAG, "Failed to decode address: " + addrStr);
                continue;
            }
            String portStr;
            if (addrport.hasMoreTokens()) {
                portStr = addrport.nextToken();
            } else {
                portStr = (String) ctx.getResources().getText(R.string.gg_default_port);
            }
            AGLog.d(TAG, "Port decoded successfully: " + portStr);
            short port = Short.decode(portStr);
            result.add(new InetSocketAddress(addrStr, port));
        }
        return result;
    }
}
