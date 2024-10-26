package org.yawlfoundation.yawl.editor.thirdparty.engine;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Author: Michael Adams
 * Creation Date: 18/02/2009
 */
public class ServerLookup {

    public static boolean isReachable(String serviceURI) throws IOException {
        URL url = new URL(serviceURI);
        HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
        httpConnection.setRequestMethod("HEAD");
        httpConnection.setConnectTimeout(2000);
        httpConnection.getResponseCode();
        return true;
    }
}
