package twilio.client.urlconnection;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.net.*;
import org.apache.commons.codec.binary.Base64;

public class TwilioClient extends twilio.client.TwilioClient {

    private static final String ENCODING = "UTF-8";

    public TwilioClient(String accountSid, String authToken) {
        super(accountSid, authToken);
    }

    @Override
    public void setUserAgent(String ua) {
    }

    @Override
    public void setConnectionTimeout(int milliseconds) {
    }

    @Override
    public void setSocketTimeout(int milliseconds) {
    }

    @Override
    protected String sendHttpRequest(String httpMethod, CharSequence url, Map<String, String> params) {
        if (params == null) {
            params = new HashMap<String, String>();
        }
        URL u = null;
        boolean isPost = "POST".equalsIgnoreCase(httpMethod);
        try {
            if (isPost) {
                u = new URL(url.toString());
            } else {
                u = new URL(buildUrl(url, params));
            }
            System.out.println("URL: " + u);
            HttpURLConnection urlconn = (HttpURLConnection) u.openConnection();
            urlconn.setDoInput(true);
            urlconn.setDoOutput(isPost);
            try {
                urlconn.setAllowUserInteraction(false);
            } catch (Throwable ignore) {
            }
            urlconn.setDefaultUseCaches(false);
            urlconn.setInstanceFollowRedirects(true);
            urlconn.setRequestMethod(httpMethod);
            String userAndPassword = this.getAccountSid() + ":" + this.getAuthToken();
            byte[] basicAuth = Base64.encodeBase64(userAndPassword.getBytes(ENCODING));
            urlconn.setRequestProperty("Authorization", "Basic " + new String(basicAuth, ENCODING));
            if (isPost) {
                urlconn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            }
            OutputStream out = null;
            if ((isPost) && (params.size() > 0)) {
                try {
                    out = urlconn.getOutputStream();
                    StringBuilder sb = new StringBuilder();
                    for (String key : params.keySet()) {
                        sb.append(encode(key));
                        sb.append("=");
                        sb.append(encode(params.get(key)));
                        sb.append("&");
                    }
                    if ((sb.length() > 0) && (sb.charAt(sb.length() - 1) == '&')) {
                        sb.deleteCharAt(sb.length() - 1);
                    }
                    OutputStreamWriter writer = new OutputStreamWriter(out);
                    writer.append(sb);
                    writer.flush();
                    writer.close();
                    out.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            int code = urlconn.getResponseCode();
            if ((code < 200) || (code > 299)) {
                throw new RuntimeException("HTTP response status code = " + code);
            }
            String response = readResponse(urlconn);
            return response;
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    protected String readResponse(HttpURLConnection urlconn) {
        InputStream input = null;
        try {
            input = urlconn.getInputStream();
            byte[] buffer = new byte[4096];
            int n = -1;
            ByteArrayOutputStream baos = new ByteArrayOutputStream(4096);
            while ((n = input.read(buffer)) != -1) {
                if (n > 0) {
                    baos.write(buffer, 0, n);
                }
            }
            return new String(baos.toByteArray(), "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                input.close();
            } catch (Exception ignore) {
            }
        }
    }

    protected String encode(String s) {
        try {
            return URLEncoder.encode(s, ENCODING);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
