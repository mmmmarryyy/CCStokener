package de.huxhorn.whistler.services;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JmpUrlShortener implements UrlShortener {

    private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(JmpUrlShortener.class);

    private String login;

    private String apiKey;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String shorten(String url) {
        List<NameValuePair> qparams = new ArrayList<NameValuePair>();
        qparams.add(new BasicNameValuePair("version", "2.0.1"));
        qparams.add(new BasicNameValuePair("longUrl", url));
        if (login != null) {
            qparams.add(new BasicNameValuePair("login", login));
            qparams.add(new BasicNameValuePair("apiKey", apiKey));
            qparams.add(new BasicNameValuePair("history", "1"));
        }
        try {
            BasicHttpParams params = new BasicHttpParams();
            DefaultHttpClient httpclient = new DefaultHttpClient(params);
            URI uri = URIUtils.createURI("http", "api.j.mp", -1, "/shorten", URLEncodedUtils.format(qparams, "UTF-8"), null);
            HttpGet httpget = new HttpGet(uri);
            if (logger.isDebugEnabled()) logger.debug("HttpGet.uri={}", httpget.getURI());
            HttpResponse response = httpclient.execute(httpget);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream instream = entity.getContent();
                JsonFactory f = new JsonFactory();
                JsonParser jp = f.createJsonParser(instream);
                JmpShortenResponse responseObj = new JmpShortenResponse();
                for (; ; ) {
                    JsonToken token = jp.nextToken();
                    String fieldname = jp.getCurrentName();
                    if (logger.isDebugEnabled()) logger.debug("Token={}, currentName={}", token, fieldname);
                    if (token == JsonToken.START_OBJECT) {
                        continue;
                    }
                    if (token == JsonToken.END_OBJECT) {
                        break;
                    }
                    if ("errorCode".equals(fieldname)) {
                        token = jp.nextToken();
                        responseObj.setErrorCode(jp.getIntValue());
                    } else if ("errorMessage".equals(fieldname)) {
                        token = jp.nextToken();
                        responseObj.setErrorMessage(jp.getText());
                    } else if ("statusCode".equals(fieldname)) {
                        token = jp.nextToken();
                        responseObj.setStatusCode(jp.getText());
                    } else if ("results".equals(fieldname)) {
                        Map<String, ShortenedUrl> results = parseResults(jp);
                        responseObj.setResults(results);
                    } else {
                        throw new IllegalStateException("Unrecognized field '" + fieldname + "'!");
                    }
                }
                Map<String, ShortenedUrl> results = responseObj.getResults();
                if (results == null) {
                    return null;
                }
                ShortenedUrl shortened = results.get(url);
                if (shortened == null) {
                    return null;
                }
                if (logger.isDebugEnabled()) logger.debug("JmpShortenResponse: {}", responseObj);
                if ("OK".equals(responseObj.getStatusCode())) {
                    return shortened.getShortUrl();
                }
                if (logger.isWarnEnabled()) logger.warn("JmpShortenResponse: {}", responseObj);
            }
        } catch (IOException ex) {
            if (logger.isWarnEnabled()) logger.warn("Exception!", ex);
        } catch (URISyntaxException ex) {
            if (logger.isWarnEnabled()) logger.warn("Exception!", ex);
        }
        return null;
    }

    private Map<String, ShortenedUrl> parseResults(JsonParser jp) throws IOException {
        Map<String, ShortenedUrl> results = new HashMap<String, ShortenedUrl>();
        for (; ; ) {
            JsonToken token = jp.nextToken();
            String fieldname = jp.getCurrentName();
            if (logger.isDebugEnabled()) logger.debug("Results - Token={}, currentName={}", token, fieldname);
            if (token == JsonToken.START_OBJECT) {
                continue;
            }
            if (token == JsonToken.END_OBJECT) {
                break;
            }
            jp.nextToken();
            ShortenedUrl shortened = new ShortenedUrl();
            shortened.setOriginal(fieldname);
            for (; ; ) {
                token = jp.nextToken();
                fieldname = jp.getCurrentName();
                if (logger.isDebugEnabled()) logger.debug("ResultsInner - Token={}, currentName={}", token, fieldname);
                if (token == JsonToken.START_OBJECT) {
                    continue;
                }
                if (token == JsonToken.END_OBJECT) {
                    break;
                }
                if ("hash".equals(fieldname)) {
                    token = jp.nextToken();
                    shortened.setHash(jp.getText());
                } else if ("shortKeywordUrl".equals(fieldname)) {
                    token = jp.nextToken();
                    shortened.setShortKeywordUrl(jp.getText());
                } else if ("shortUrl".equals(fieldname)) {
                    token = jp.nextToken();
                    shortened.setShortUrl(jp.getText());
                } else if ("userHash".equals(fieldname)) {
                    token = jp.nextToken();
                    shortened.setUserHash(jp.getText());
                } else {
                    if (logger.isWarnEnabled()) logger.warn("Unknown field in results: '{}'!", fieldname);
                }
            }
            results.put(shortened.getOriginal(), shortened);
        }
        return results;
    }

    private static class JmpShortenResponse {

        private int errorCode;

        private String errorMessage;

        private Map<String, ShortenedUrl> results;

        private String statusCode;

        public int getErrorCode() {
            return errorCode;
        }

        public void setErrorCode(int errorCode) {
            this.errorCode = errorCode;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        public Map<String, ShortenedUrl> getResults() {
            return results;
        }

        public void setResults(Map<String, ShortenedUrl> results) {
            this.results = results;
        }

        public void setStatusCode(String statusCode) {
            this.statusCode = statusCode;
        }

        public String getStatusCode() {
            return statusCode;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            JmpShortenResponse that = (JmpShortenResponse) o;
            if (errorCode != that.errorCode) return false;
            if (errorMessage != null ? !errorMessage.equals(that.errorMessage) : that.errorMessage != null) {
                return false;
            }
            if (results != null ? !results.equals(that.results) : that.results != null) return false;
            if (statusCode != null ? !statusCode.equals(that.statusCode) : that.statusCode != null) return false;
            return true;
        }

        @Override
        public int hashCode() {
            int result = errorCode;
            result = 31 * result + (errorMessage != null ? errorMessage.hashCode() : 0);
            result = 31 * result + (results != null ? results.hashCode() : 0);
            result = 31 * result + (statusCode != null ? statusCode.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "JmpShortenResponse{" + "errorCode=" + errorCode + ", errorMessage='" + errorMessage + '\'' + ", results=" + results + ", statusCode='" + statusCode + '\'' + '}';
        }
    }

    private static class ShortenedUrl {

        private String original;

        private String hash;

        private String shortKeywordUrl;

        private String shortUrl;

        private String userHash;

        public String getHash() {
            return hash;
        }

        public void setHash(String hash) {
            this.hash = hash;
        }

        public String getOriginal() {
            return original;
        }

        public void setOriginal(String original) {
            this.original = original;
        }

        public String getShortKeywordUrl() {
            return shortKeywordUrl;
        }

        public void setShortKeywordUrl(String shortKeywordUrl) {
            this.shortKeywordUrl = shortKeywordUrl;
        }

        public String getShortUrl() {
            return shortUrl;
        }

        public void setShortUrl(String shortUrl) {
            this.shortUrl = shortUrl;
        }

        public String getUserHash() {
            return userHash;
        }

        public void setUserHash(String userHash) {
            this.userHash = userHash;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ShortenedUrl that = (ShortenedUrl) o;
            if (hash != null ? !hash.equals(that.hash) : that.hash != null) return false;
            if (original != null ? !original.equals(that.original) : that.original != null) return false;
            if (shortKeywordUrl != null ? !shortKeywordUrl.equals(that.shortKeywordUrl) : that.shortKeywordUrl != null) {
                return false;
            }
            if (shortUrl != null ? !shortUrl.equals(that.shortUrl) : that.shortUrl != null) return false;
            if (userHash != null ? !userHash.equals(that.userHash) : that.userHash != null) return false;
            return true;
        }

        @Override
        public int hashCode() {
            int result = original != null ? original.hashCode() : 0;
            result = 31 * result + (hash != null ? hash.hashCode() : 0);
            result = 31 * result + (shortKeywordUrl != null ? shortKeywordUrl.hashCode() : 0);
            result = 31 * result + (shortUrl != null ? shortUrl.hashCode() : 0);
            result = 31 * result + (userHash != null ? userHash.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "ShortenedUrl{" + "hash='" + hash + '\'' + ", original='" + original + '\'' + ", shortKeywordUrl='" + shortKeywordUrl + '\'' + ", shortUrl='" + shortUrl + '\'' + ", userHash='" + userHash + '\'' + '}';
        }
    }
}
