package com.dddforandroid.c2dm;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import javax.servlet.http.HttpServletResponse;
import com.dddforandroid.util.Util;

/**
 * 
 * Handle the process of sending an c2dm message to an Android device.  
 *
 */
public class MessageUtil {

    @SuppressWarnings("unused")
    private static final String AUTH = "authentication";

    private static final String UPDATE_CLIENT_AUTH = "Update-Client-Auth";

    public static final String PARAM_REGISTRATION_ID = "registration_id";

    public static final String PARAM_DELAY_WHILE_IDLE = "delay_while_idle";

    public static final String PARAM_COLLAPSE_KEY = "collapse_key";

    public static final String C2DM_SEND_ENDPOINT = "https://android.clients.google.com/c2dm/send";

    private static final String UTF8 = "UTF-8";

    public static int sendMessage(String auth_token, String registrationId, String message) throws IOException {
        StringBuilder postDataBuilder = new StringBuilder();
        postDataBuilder.append(PARAM_REGISTRATION_ID).append("=").append(registrationId);
        postDataBuilder.append("&").append(PARAM_COLLAPSE_KEY).append("=").append("0");
        postDataBuilder.append("&").append("data.payload").append("=").append(URLEncoder.encode(message, UTF8));
        byte[] postData = postDataBuilder.toString().getBytes(UTF8);
        URL url = new URL(C2DM_SEND_ENDPOINT);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setUseCaches(false);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
        conn.setRequestProperty("Content-Length", Integer.toString(postData.length));
        conn.setRequestProperty("Authorization", "GoogleLogin auth=" + auth_token);
        OutputStream out = conn.getOutputStream();
        out.write(postData);
        out.close();
        int responseCode = conn.getResponseCode();
        if (responseCode == HttpServletResponse.SC_UNAUTHORIZED || responseCode == HttpServletResponse.SC_FORBIDDEN) {
            AuthenticationUtil.getTokenFromServer(Util.USER, Util.PASSWORD);
            sendMessage(auth_token, registrationId, message);
        }
        String updatedAuthToken = conn.getHeaderField(UPDATE_CLIENT_AUTH);
        if (updatedAuthToken != null && !auth_token.equals(updatedAuthToken)) {
            Util.updateToken(updatedAuthToken);
        }
        return responseCode;
    }
}
