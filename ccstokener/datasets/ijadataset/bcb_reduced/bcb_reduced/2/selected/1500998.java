package org.openremote.android.console;

import java.io.IOException;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

/**
 * Does the HTTP stuff, anything related to HttpClient should go here.
 * 
 * @author Andrew C. Oliver <acoliver at osintegrators.com>
 */
public class HTTPUtil {

    public static final String CLICK = "click";

    public static final String PRESS = "press";

    public static final String RELEASE = "release";

    /**
     * Sends a button action of some kind
     * 
     * @param url
     * @param id
     * @param command
     * @return http_status_code
     * @throws ClientProtocolException
     * @throws IOException
     */
    public static int sendButton(String url, String id, String command) throws ClientProtocolException, IOException {
        String connectString = url + "/rest/button/" + id + "/" + command;
        HttpClient client = new DefaultHttpClient();
        HttpPost post = new HttpPost(connectString);
        HttpResponse response = client.execute(post);
        int code = response.getStatusLine().getStatusCode();
        return code;
    }
}
