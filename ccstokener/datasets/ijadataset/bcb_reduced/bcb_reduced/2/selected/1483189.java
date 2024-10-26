package de.eversync.netComm;

import java.io.IOException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import de.eversync.logging.Logger;

public class Request {

    private boolean mRequestStarted;

    private HttpClient mHttpClient;

    private HttpUriRequest mRequest;

    public Request(HttpClient aHttpClient) {
        this.setHttpClient(aHttpClient);
    }

    public void addRequest(HttpUriRequest aRequest) {
        if (!this.isRequestStarted()) {
            this.setRequest(aRequest);
        }
    }

    public HttpEntity performRequest() throws ClientProtocolException, IOException, WrongLoginException {
        if (this.getRequest() != null) {
            this.setRequestStarted(true);
            this.getHttpClient().getConnectionManager().closeExpiredConnections();
            Logger.getInstance().add("Execute: " + this.getRequest().getURI());
            HttpResponse response = this.getHttpClient().execute(this.getRequest());
            if (response.getFirstHeader("eversync-denied") == null) {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    return entity;
                }
            } else {
                throw new WrongLoginException("Username or password are wrong");
            }
        } else {
            throw new NullPointerException("No request set");
        }
        return null;
    }

    private void setRequestStarted(boolean aVal) {
        this.mRequestStarted = aVal;
    }

    public boolean isRequestStarted() {
        return mRequestStarted;
    }

    private void setHttpClient(HttpClient aC) {
        this.mHttpClient = aC;
    }

    private HttpClient getHttpClient() {
        return this.mHttpClient;
    }

    private void setRequest(HttpUriRequest aR) {
        this.mRequest = aR;
    }

    private HttpUriRequest getRequest() {
        return this.mRequest;
    }
}
