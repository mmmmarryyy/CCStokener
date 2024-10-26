package org.boticelli.plugin.twitter;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.boticelli.dao.TwitterDAO;
import org.boticelli.model.TwitterAuth;
import org.boticelli.model.TwitterEntry;
import org.boticelli.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;
import org.svenson.JSONParser;
import org.svenson.tokenize.InputStreamSource;
import org.svenson.util.ExceptionWrapper;

public class TwitterAPIImpl implements TwitterAPI, InitializingBean {

    private static final int LOOKUP_LIMIT = 100;

    private static Logger log = LoggerFactory.getLogger(TwitterAPIImpl.class);

    private BasicHttpContext context = new BasicHttpContext();

    private DefaultHttpClient httpClient;

    private HttpClientFactory httpClientFactory;

    private String token;

    private String authorizeURL;

    private String accessTokenURL;

    private String requestTokenURL;

    private String secret;

    private TwitterDAO twitterDAO;

    private String botNick;

    @Required
    public void setBotNick(String botNick) {
        this.botNick = botNick;
    }

    @Required
    public void setTwitterDAO(TwitterDAO twitterDAO) {
        this.twitterDAO = twitterDAO;
    }

    @Required
    public void setAuthorizeURL(String authorizeURL) {
        this.authorizeURL = authorizeURL;
    }

    @Required
    public void setAccessTokenURL(String accessTokenURL) {
        this.accessTokenURL = accessTokenURL;
    }

    @Required
    public void setRequestTokenURL(String requestTokenURL) {
        this.requestTokenURL = requestTokenURL;
    }

    @Required
    public void setToken(String token) {
        this.token = token;
    }

    @Required
    public void setSecret(String secret) {
        this.secret = secret;
    }

    @Required
    public void setHttpClientFactory(HttpClientFactory httpClientFactory) {
        this.httpClientFactory = httpClientFactory;
    }

    /**
     * {@inheritDoc}
     */
    public List<Map<String, Object>> getFriendsTimeLine(TwitterEntry entry, String since) {
        String uri = "http://api.twitter.com/1/statuses/home_timeline.json";
        if (since != null) {
            uri += "?since_id=" + urlEscape(since);
        }
        List<Map<String, Object>> json = getJSON(List.class, uri, getConsumerForEntry(entry));
        return json;
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, Object> tweet(TwitterEntry entry, String message) {
        CommonsHttpOAuthConsumer consumer = getConsumerForEntry(entry);
        return postAndReturnJSON(Map.class, "http://api.twitter.com/1/statuses/update.json?status=" + urlEscape(message.trim()) + "&source=Boticelli", consumer);
    }

    private CommonsHttpOAuthConsumer getConsumerForEntry(TwitterEntry sendFor) {
        CommonsHttpOAuthConsumer consumer = createOAuthConsumer();
        consumer.setTokenWithSecret(sendFor.getToken(), sendFor.getSecret());
        return consumer;
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, Object> deleteTweet(TwitterEntry entry, String id) {
        return postAndReturnJSON(Map.class, "http://api.twitter.com/1/statuses/destroy/" + urlEscape(id) + ".json", getConsumerForEntry(entry));
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, Object> unfriend(TwitterEntry entry, String user) {
        return postAndReturnJSON(Map.class, "http://api.twitter.com/1/friendships/destroy.json?screen_name=" + urlEscape(user), getConsumerForEntry(entry));
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, Object> befriend(TwitterEntry entry, String user) {
        return postAndReturnJSON(Map.class, "http://api.twitter.com/1/friendships/create.json?screen_name=" + urlEscape(user), getConsumerForEntry(entry));
    }

    @Override
    public List<String> showFriends(TwitterEntry entry) {
        HashMap result = getJSON(HashMap.class, "http://api.twitter.com/1/friends/ids.json?cursor=-1", getConsumerForEntry(entry));
        List<Long> ids = (List<Long>) result.get("ids");
        return lookupUserScreenNames(entry, ids);
    }

    private String urlEscape(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw ExceptionWrapper.wrap(e);
        }
    }

    private <T> T getJSON(Class<T> cls, String uri, CommonsHttpOAuthConsumer consumer) {
        try {
            HttpGet httpGet = new HttpGet(uri);
            consumer.sign(httpGet);
            return executeMethod(cls, httpGet);
        } catch (OAuthMessageSignerException e) {
            throw new TwitterException(e.getMessage());
        } catch (OAuthExpectationFailedException e) {
            throw new TwitterException(e.getMessage());
        } catch (OAuthCommunicationException e) {
            throw new TwitterException(e.getMessage());
        }
    }

    private <T> T postAndReturnJSON(Class<T> cls, String uri, CommonsHttpOAuthConsumer consumer) {
        try {
            HttpPost httpPost = new HttpPost(uri);
            consumer.sign(httpPost);
            return executeMethod(cls, httpPost);
        } catch (OAuthMessageSignerException e) {
            throw new TwitterException(e.getMessage());
        } catch (OAuthExpectationFailedException e) {
            throw new TwitterException(e.getMessage());
        } catch (OAuthCommunicationException e) {
            throw new TwitterException(e.getMessage());
        }
    }

    private <T> T deleteAndReturnJSON(Class<T> cls, String uri, CommonsHttpOAuthConsumer consumer) {
        try {
            HttpDelete httpDelete = new HttpDelete(uri);
            consumer.sign(httpDelete);
            return executeMethod(cls, httpDelete);
        } catch (OAuthMessageSignerException e) {
            throw new TwitterException(e.getMessage());
        } catch (OAuthExpectationFailedException e) {
            throw new TwitterException(e.getMessage());
        } catch (OAuthCommunicationException e) {
            throw new TwitterException(e.getMessage());
        }
    }

    private <T> T executeMethod(Class<T> cls, HttpRequestBase httpReq) {
        log.debug("executing {} {}, converting response to {}", new Object[] { httpReq.getMethod(), httpReq.getURI(), cls });
        InputStream is = null;
        try {
            HttpResponse resp = httpClient.execute(httpReq, context);
            int statusCode = resp.getStatusLine().getStatusCode();
            if (statusCode < 200 || statusCode > 299) {
                log.error("{} executing {} {}", new Object[] { statusCode, httpReq.getMethod(), httpReq.getURI() });
                throw new TwitterException("Twitter answered with " + statusCode);
            }
            HttpEntity entity = resp.getEntity();
            if (entity != null) {
                is = entity.getContent();
                T result = JSONParser.defaultJSONParser().parse(cls, new InputStreamSource(is, false));
                log.debug("result = {}", result);
                return result;
            }
        } catch (TwitterException e) {
            httpReq.abort();
            throw e;
        } catch (Exception e) {
            log.error("error executing {} {}: {}", new Object[] { httpReq.getMethod(), httpReq.getURI(), e });
            httpReq.abort();
            throw new TwitterException("error on " + httpReq.getMethod() + " " + httpReq.getURI(), e);
        } finally {
            if (is != null) {
                IOUtils.closeQuietly(is);
            }
        }
        return null;
    }

    public boolean isConfigured() {
        return !token.equals("NONE");
    }

    public DefaultHttpClient getHttpClient() {
        return httpClient;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.httpClient = httpClientFactory.createHttpClient();
    }

    @Override
    public CommonsHttpOAuthProvider createOAuthProvider() {
        CommonsHttpOAuthProvider provider = new CommonsHttpOAuthProvider(requestTokenURL, accessTokenURL, authorizeURL, httpClient);
        return provider;
    }

    @Override
    public CommonsHttpOAuthConsumer createOAuthConsumer() {
        return new CommonsHttpOAuthConsumer(token, secret);
    }

    private List<String> lookupUserScreenNames(TwitterEntry entry, List<Long> ids) {
        List<String> screenNames = new ArrayList<String>();
        boolean tooLarge = false;
        do {
            tooLarge = ids.size() > LOOKUP_LIMIT;
            List<Long> idsToRequest;
            if (tooLarge) {
                idsToRequest = ids.subList(0, LOOKUP_LIMIT);
                ids = ids.subList(LOOKUP_LIMIT, ids.size());
            } else {
                idsToRequest = ids;
            }
            List<Map<String, Object>> userInfos = getJSON(List.class, "http://api.twitter.com/1/users/lookup.json?user_id=" + urlEscape(Util.join(idsToRequest, ",")) + "&include_entities=false", getConsumerForEntry(entry));
            for (Map<String, Object> info : userInfos) {
                screenNames.add((String) info.get("screen_name"));
            }
        } while (tooLarge);
        return screenNames;
    }
}
