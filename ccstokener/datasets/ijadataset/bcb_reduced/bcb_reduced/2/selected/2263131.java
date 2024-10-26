package com.meschbach.psi.util;

import com.meschbach.psi.PSIException;
import com.meschbach.psi.util.rest.GetRequest;
import com.meschbach.psi.util.rest.PostRequest;
import com.meschbach.psi.util.rest.PutRequest;
import com.meschbach.psi.util.rest.RequestBuilder;
import com.meschbach.psi.util.rest.ResponseEntityAssertion;
import com.meschbach.psi.util.rest.ResponseHandler;
import com.meschbach.psi.util.rest.StatusAssertionResponseHandler;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;

/**
 * A <code>RESTClient</code> is a class for constructing simple RESTful client
 * against a remote host.  The primary intention of the instances is to provide
 * a DRYer implementation of the DSL class methods attached to this class.
 * <p>
 * If you are searching for the HttpStatusCode enumeration then it has moved
 * into the package in version 1.2.0.
 *
 * @author "Mark Eschbach" meschbach@gmail.com
 * @since 1.1.0
 * @version 1.1.0
 */
public class RESTClient {

    /**
     * The <code>url</code> is the URL to request.  This is the
     * plain text URL, without being encoded.
     * @since 1.2.0
     */
    protected String url;

    /**
     * The <code>builder</code> is responsible for constructing an HTTPClient
     * request to be issued.
     * @since 1.2.0
     */
    protected RequestBuilder builder;

    /**
     * The <code>handlers</code> is a list of handlers to deal with the
     * response in an application specific method.
     * @since 1.2.0
     */
    protected List<ResponseHandler> handlers;

    /**
     * Constructs a new RESTful client to contact the named remote resource
     * <code>url</code> with the method specified by the given <code>builder</code>
     * 
     * @param url is the URL to the resource to be contacted
     * @param builder is the builder for the method to be used
     * @since 1.2.0
     */
    public RESTClient(String url, RequestBuilder builder) {
        this.url = url;
        this.builder = builder;
        handlers = new LinkedList<ResponseHandler>();
    }

    /**
     * Issues the request represented by this client and notifies the response
     * handlers of th results of the request.
     *
     * @throws PSIException if a problem occurs issuing or processing the request
     * @throws AssertionError maybe thrown depending on the implementation of the response handlers
     */
    public void doRequest() throws PSIException {
        HttpClient client = new DefaultHttpClient();
        try {
            URI uri = new URI(url);
            HttpResponse response = client.execute(builder.buildRequest(uri));
            HttpStatusCode responseCode = HttpStatusCode.getCode(response.getStatusLine().getStatusCode());
            Iterator<ResponseHandler> rhit = handlers.iterator();
            while (rhit.hasNext()) {
                rhit.next().handleResponse(response, responseCode);
            }
        } catch (URISyntaxException use) {
            throw new PSIException(use);
        } catch (IOException ioe) {
            throw new PSIException(ioe);
        } finally {
            client.getConnectionManager().shutdown();
        }
    }

    /**
     * Adds a response handler to the set of handlers to be notified when our
     * request has completed.
     *
     * @param rs is the response handler to be notified
     */
    public void addHandler(ResponseHandler rs) {
        handlers.add(rs);
    }

    /**
     * Issues a GET request to the given remote resource and asserts we will
     * receive the specified HTTP response code
     *
     * @param url is a string in URL format (http://host/resource/path) of the resource to be contacted
     * @param expectedCode is the HTTP status code we expect to receive
     * @throws PSIException if a problem attempting to issue the request
     */
    public static void assertGetStatus(String url, HttpStatusCode expectedCode) {
        try {
            RESTClient rc = new RESTClient(url, new GetRequest());
            rc.addHandler(new StatusAssertionResponseHandler(expectedCode));
            rc.doRequest();
        } catch (PSIException pe) {
            throw new IllegalStateException(pe);
        }
    }

    /**
     * Attempts to issue an HTTP <code>PUT</code> request against with the
     * given HTTP <code>entity</code> to the named resource at <code>url<code>,
     * asserting the resulting status cde will be <code>expectedCode</code>.
     * 
     * @param url is a string in URL format of the resource to put
     * @param entity is the entity body to put
     * @param expectedCode is the status code expected to be returned
     * @throws PSIException if there is a problem contacting the remote service for the request
     */
    public static void assertPutStatus(String url, String entity, HttpStatusCode expectedCode) {
        try {
            RESTClient rc = new RESTClient(url, new PutRequest(entity));
            rc.addHandler(new StatusAssertionResponseHandler(expectedCode));
            rc.doRequest();
        } catch (PSIException pe) {
            throw new IllegalStateException(pe);
        }
    }

    /**
     * Issues an HTTP GET request to the named resource at <code>url</code>
     * expecting an HTTP 200 Ok response with the given <code>expectedEntity</code>
     * as the response entity.
     *
     * @param url is the resource to GET
     * @param expectedEntity is the entity we are expecting in return
     * @throws PSIException if we are unable to contact the remote service.
     */
    public static void assertGet(String url, String expectedEntity) {
        assertGet(url, HttpStatusCode.Ok, expectedEntity);
    }

    /**
     * Issues the given GET request with to the specified <code>url/code>,
     * asserting we recieve a response with HTTP response code <code>expectedCode</code>
     * and an HTTP entity of <code>expectedEntity</code>.
     *
     * @param url is the URL of the resource to get
     * @param expectedCode is the code we expect to be returned
     * @param expectedEntity is the entity we expect to receive
     * @throws PSIException if we are unable to communicate with the remote HTTP service
     */
    public static void assertGet(String url, HttpStatusCode expectedCode, String expectedEntity) {
        try {
            RESTClient rc = new RESTClient(url, new GetRequest());
            rc.addHandler(new StatusAssertionResponseHandler(expectedCode));
            rc.addHandler(new ResponseEntityAssertion(expectedEntity));
            rc.doRequest();
        } catch (PSIException pe) {
            throw new IllegalStateException(pe);
        }
    }

    /**
     * Issues an HTTP POST request with the specified content within the HTTP
     * entity.  This function will then assert that the given
     * <code>expectedCode</code> and <code>expectedEntity</code> is received
     * in response.
     * 
     * @param url is the URL to POST to
     * @param entity is the entity to POST
     * @param expectedCode is the expected response code
     * @param expectedEntity is the expected entity
     */
    public static void assertPost(String url, String entity, HttpStatusCode expectedCode, String expectedEntity) {
        try {
            RESTClient rc = new RESTClient(url, new PostRequest(entity));
            rc.addHandler(new StatusAssertionResponseHandler(expectedCode));
            rc.addHandler(new ResponseEntityAssertion(expectedEntity));
            rc.doRequest();
        } catch (PSIException pe) {
            throw new IllegalStateException(pe);
        }
    }

    /**
     * Issues an HTTP POST request with the specified content within the HTTP
     * entity.  This function will then assert that the given
     * <code>expectedCode</code> is received
     * in response.
     *
     * @param url is the URL to POST to
     * @param entity is the entity to POST
     * @param expectedCode is the expected response code
     * @param expectedEntity is the expected entity
     */
    public static void assertPost(String url, String entity, HttpStatusCode expectedCode) {
        try {
            RESTClient rc = new RESTClient(url, new PostRequest(entity));
            rc.addHandler(new StatusAssertionResponseHandler(expectedCode));
            rc.doRequest();
        } catch (PSIException pe) {
            throw new IllegalStateException(pe);
        }
    }
}
