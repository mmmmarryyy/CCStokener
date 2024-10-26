package com.face.api.client;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import com.face.api.client.exception.FaceClientException;

/**
 * Default implementation of {@link Responder} interface. It seems as though
 * face.com always returns an HTTP status code of 200 even for 404 not founds.
 * This is why there is no need to check the status line.
 * 
 * @author Marlon Hendred
 *
 */
class DefaultResponder implements Responder {

    /**
	 * {@link HttpClient} for executing requests
	 */
    private final HttpClient httpClient;

    /**
	 * {@link HttpPost} method for {@code POST}s
	 */
    private final HttpPost postMethod;

    /**
	 * {@link HttpGet} method for {@code GET}s
	 */
    private final HttpGet getMethod;

    /**
	 * Logger
	 */
    private final Log logger;

    public DefaultResponder() {
        this.logger = LogFactory.getLog(DefaultResponder.class);
        this.httpClient = new DefaultHttpClient();
        this.postMethod = new HttpPost();
        this.getMethod = new HttpGet();
    }

    /**
	 * @see {@link Responder#doPost(URI, List)}
	 */
    public String doPost(final URI uri, final List<NameValuePair> params) throws FaceClientException {
        try {
            final HttpEntity entity = new UrlEncodedFormEntity(params, "UTF-8");
            postMethod.setURI(uri);
            postMethod.setEntity(entity);
            final HttpResponse response = httpClient.execute(postMethod);
            return EntityUtils.toString(response.getEntity());
        } catch (ClientProtocolException cpe) {
            logger.error("Protocol error while POSTing to " + uri, cpe);
            throw new FaceClientException(cpe);
        } catch (IOException ioe) {
            logger.error("Error while POSTing to " + uri, ioe);
            throw new FaceClientException(ioe);
        }
    }

    /**
	 * @see {@link Responder#doPost(File, URI, List)}
	 */
    public String doPost(final File file, final URI uri, final List<NameValuePair> params) throws FaceClientException {
        try {
            final MultipartEntity entity = new MultipartEntity();
            entity.addPart("image", new FileBody(file));
            try {
                for (NameValuePair nvp : params) {
                    entity.addPart(nvp.getName(), new StringBody(nvp.getValue()));
                }
            } catch (UnsupportedEncodingException uee) {
                logger.error("Error adding entity", uee);
                throw new FaceClientException(uee);
            }
            postMethod.setURI(uri);
            postMethod.setEntity(entity);
            final HttpResponse response = httpClient.execute(postMethod);
            return EntityUtils.toString(response.getEntity());
        } catch (ClientProtocolException cpe) {
            logger.error("Protocol error while POSTing to " + uri, cpe);
            throw new FaceClientException(cpe);
        } catch (IOException ioe) {
            logger.error("Error while POSTing to " + uri, ioe);
            throw new FaceClientException(ioe);
        }
    }

    /**
	 * @see {@code Responder#doGet(URI)}
	 */
    public String doGet(final URI uri) throws FaceClientException {
        getMethod.setURI(uri);
        try {
            HttpResponse response = httpClient.execute(getMethod);
            return EntityUtils.toString(response.getEntity());
        } catch (ClientProtocolException cpe) {
            logger.error("Protocol error while POSTing to " + uri, cpe);
            throw new FaceClientException(cpe);
        } catch (IOException ioe) {
            logger.error("Error while POSTing to " + uri, ioe);
            throw new FaceClientException(ioe);
        }
    }
}
