package in.raam.twsh.oauth;

import in.raam.twsh.comm.TwitterRequest;
import in.raam.twsh.util.Util;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map.Entry;
import oauth.signpost.OAuthConsumer;

/**
 * Adapter class that acts as a JSON Rest client interacting with the twitter REST API server, internally takes care of 
 * signing OAuth requests with the sign post consumer
 * @author raam
 *
 */
public class JsonRestClient {

    /**
     * Fire a HTTP request with provided url and return the JSON string response
     * @param urlStr
     *                  Rest URL to be accessed
     * @param consumer
     *                  OAuth Consumer implementation used to sign the requests 
     * @param params
     *                  Request parameters
     * @return
     * @throws Exception
     */
    public String getResponse(TwitterRequest twitterRequest, OAuthConsumer consumer) throws Exception {
        URL url = new URL(twitterRequest.url() + twitterRequest.paramString());
        HttpURLConnection req = (HttpURLConnection) url.openConnection();
        consumer.sign(req);
        req.connect();
        return Util.mkString(req.getInputStream());
    }

    /**
     * Post a request to the Twitter REST API server and return the response string as a JSON 
     * @param urlStr
     *                  Rest URL to be accessed
     * @param consumer
     *                  OAuth Consumer implementation used to sign the request 
     * @param params
     *                  Request param map
     * @return
     * @throws Exception
     */
    public String postRequest(TwitterRequest twitterRequest, OAuthConsumer consumer) throws Exception {
        URL url = new URL(twitterRequest.url() + twitterRequest.paramString());
        HttpURLConnection request = (HttpURLConnection) url.openConnection();
        request.setRequestMethod("POST");
        consumer.sign(request);
        request.connect();
        return Util.mkString(request.getInputStream());
    }
}
