package org.musicbrainz.webservice.impl;

import java.io.IOException;
import java.io.InputStream;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.musicbrainz.webservice.AuthorizationException;
import org.musicbrainz.webservice.DefaultWebService;
import org.musicbrainz.webservice.RequestException;
import org.musicbrainz.webservice.ResourceNotFoundException;
import org.musicbrainz.webservice.WebServiceException;
import org.musicbrainz.wsxml.MbXMLException;
import org.musicbrainz.wsxml.element.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple http client using Apache Commons HttpClient.
 * 
 * @author Patrick Ruhkopf
 */
public class HttpClientWebService extends DefaultWebService {

    /**
	 * A logger
	 */
    private Logger log = LoggerFactory.getLogger(HttpClientWebService.class);

    /**
	 * A {@link HttpClient} instance
	 */
    private HttpClient httpClient;

    /**
	 * Default constructor creates a httpClient with default properties. 
	 */
    public HttpClientWebService() {
        this.httpClient = new DefaultHttpClient();
    }

    /**
	 * Use this constructor to inject a configured {@link HttpClient}.
	 * 
	 * @param httpClient A configured {@link HttpClient}.
	 */
    public HttpClientWebService(final HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    protected Metadata doGet(final String url) throws WebServiceException, MbXMLException {
        final HttpGet method = new HttpGet(url);
        this.log.debug(url);
        Metadata metadata = null;
        try {
            final HttpResponse response = this.httpClient.execute(method);
            final int statusCode = response.getStatusLine().getStatusCode();
            if (HttpStatus.SC_OK == statusCode) {
                final InputStream responseStream = response.getEntity().getContent();
                metadata = this.getParser().parse(responseStream);
            } else {
                final String responseString = response.getEntity() != null ? EntityUtils.toString(response.getEntity()) : "";
                switch(statusCode) {
                    case HttpStatus.SC_NOT_FOUND:
                        throw new ResourceNotFoundException(responseString);
                    case HttpStatus.SC_BAD_REQUEST:
                        throw new RequestException(responseString);
                    case HttpStatus.SC_FORBIDDEN:
                        throw new AuthorizationException(responseString);
                    case HttpStatus.SC_UNAUTHORIZED:
                        throw new AuthorizationException(responseString);
                    default:
                        String em = "web service returned unknown status '" + statusCode + "', response was: " + responseString;
                        this.log.error(em);
                        throw new WebServiceException(em);
                }
            }
        } catch (IOException e) {
            this.log.error("Fatal transport error: " + e.getMessage());
            throw new WebServiceException(e.getMessage(), e);
        }
        return metadata;
    }

    @Override
    protected void doPost(final String url, final InputStream data) throws WebServiceException {
        final HttpPost method = new HttpPost(url);
        method.setEntity(new InputStreamEntity(data, -1));
        try {
            final HttpResponse response = this.httpClient.execute(method);
            final String responseString = response.getEntity() != null ? EntityUtils.toString(response.getEntity()) : "";
            final int statusCode = response.getStatusLine().getStatusCode();
            switch(statusCode) {
                case HttpStatus.SC_OK:
                    return;
                case HttpStatus.SC_NOT_FOUND:
                    throw new ResourceNotFoundException(responseString);
                case HttpStatus.SC_BAD_REQUEST:
                    throw new RequestException(responseString);
                case HttpStatus.SC_FORBIDDEN:
                    throw new AuthorizationException(responseString);
                case HttpStatus.SC_UNAUTHORIZED:
                    throw new AuthorizationException(responseString);
                default:
                    String em = "web service returned unknown status '" + statusCode + "', response was: " + responseString;
                    this.log.error(em);
                    throw new WebServiceException(em);
            }
        } catch (IOException e) {
            this.log.error("Fatal transport error: " + e.getMessage());
            throw new WebServiceException(e.getMessage(), e);
        }
    }
}
