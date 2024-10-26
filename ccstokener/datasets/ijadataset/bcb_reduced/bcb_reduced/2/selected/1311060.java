package com.androidcommons.webclient.json;

import java.io.IOException;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import com.androidcommons.webclient.http.HttpWebClient;

/**
 * @author Denis Migol
 * 
 */
public class JSONHttpWebClient extends HttpWebClient {

    /**
	 * @param endPoint
	 */
    public JSONHttpWebClient(final String endPoint) {
        super(endPoint);
    }

    /**
	 * 
	 * @param path
	 * @param jsRequest
	 * @return
	 * @throws IOException
	 * @throws HttpException
	 * @throws JSONException
	 */
    public JSONObject executeJSON(final String path, final JSONObject jsRequest) throws IOException, HttpException, JSONException {
        final HttpPost httpRequest = newHttpPost(path);
        httpRequest.setHeader("Content-Type", "application/json");
        final String request = jsRequest.toString();
        httpRequest.setEntity(new StringEntity(request));
        final HttpResponse httpResponse = executeHttp(httpRequest);
        final String response = EntityUtils.toString(httpResponse.getEntity());
        return new JSONObject(response);
    }
}
