package org.sharp.android.autils;

import java.io.InputStream;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.sharp.intf.Response;

public class HttpUtils {

    public static Response newResponse(final Response.StatusCode statusCode, final String content) {
        return new Response() {

            @Override
            public Response.StatusCode statusCode() {
                return statusCode;
            }

            @Override
            public String content() {
                return content;
            }
        };
    }

    public static Response get(String httpUrl) {
        Response.StatusCode statusCode = Response.StatusCode.NORMAL;
        String content = null;
        DefaultHttpClient dhc = new DefaultHttpClient();
        HttpUriRequest request = new HttpGet(httpUrl);
        try {
            HttpResponse response = dhc.execute(request);
            response.getStatusLine().getStatusCode();
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream instream = entity.getContent();
                content = IOUtils.toString(instream, EntityUtils.getContentCharSet(entity));
            }
        } catch (Exception e) {
            e.printStackTrace();
            statusCode = Response.StatusCode.EXCEPTION;
        }
        return newResponse(statusCode, content);
    }
}
