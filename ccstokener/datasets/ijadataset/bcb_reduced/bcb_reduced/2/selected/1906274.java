package org.boticelli.plugin.twitter;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

public class ShortURLResolverImpl implements ShortURLResolver {

    private static Logger log = LoggerFactory.getLogger(ShortURLResolverImpl.class);

    private Pattern domainPattern;

    private DefaultHttpClient httpClient;

    @Required
    public void setHttpClientFactory(HttpClientFactory httpClientFactory) {
        this.httpClient = httpClientFactory.createHttpClient();
        HttpParams params = httpClient.getParams();
        HttpClientParams.setRedirecting(params, false);
    }

    /**
     * {@inheritDoc}
     */
    public String resolveShortURL(String text) {
        StringBuffer buf = new StringBuffer();
        Matcher m = domainPattern.matcher(text);
        while (m.find()) {
            log.debug("match: {}", m);
            String longURL = requestLongURL(m.group(1));
            m.appendReplacement(buf, longURL);
        }
        m.appendTail(buf);
        return buf.toString();
    }

    private String requestLongURL(String url) {
        log.debug("requestLongURL({})", url);
        HttpHead head = new HttpHead(url);
        try {
            HttpResponse response = httpClient.execute(head);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode < 200 || statusCode > 302) {
                log.error("URL service {} returned {}, returning unchanged value.", url, statusCode);
                return url;
            }
            Header header = response.getFirstHeader("Location");
            if (header != null) {
                String location = header.getValue();
                if (location != null) {
                    return location;
                }
            }
        } catch (IOException e) {
            log.error("io exception resolving url", e);
            head.abort();
        }
        return url;
    }

    public void setDomainRegex(String regex) {
        this.domainPattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
    }
}
