package org.ikross.twitter.task;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.HashMap;
import java.util.Set;
import org.ikross.twitter.connector.parameter.GETParameter;
import org.ikross.twitter.connector.parameter.IParameter;
import org.ikross.twitter.connector.parameter.POSTParameter;
import org.ikross.twitter.exception.AuthenticationException;
import org.ikross.twitter.exception.BadGatewayException;
import org.ikross.twitter.exception.BadRequestException;
import org.ikross.twitter.exception.InternalServerException;
import org.ikross.twitter.exception.ServiceUnavailableException;
import org.ikross.twitter.exception.TaskException;
import org.ikross.twitter.exception.URLNotFoundException;

public class DefaultConnector extends Connector {

    private URL url;

    private HashMap<String, GETParameter> parametersGET;

    private HashMap<String, POSTParameter> parametersPOST;

    private final String HTTP_BAD_REQUEST_ERROR = "Bad request. The limit request may be exceeded";

    private final String HTTP_UNAUTHORIZED_ERROR = "Wrong user or password. Verify the credentials with the task";

    private final String HTTP_FORBIDDEN_ERROR = "Right request but something is wrong";

    private final String HTTP_NOT_FOUND_ERROR = "The request has an invalid URI or the resource in question doesn't exist";

    private final String HTTP_INTERNAL_ERROR = "Internal error in the remote server";

    private final String HTTP_BAD_GATEWAY_ERROR = "Service may be down or being updated";

    private final String HTTP_SERVICE_UNAVAILABLE_ERROR = "The servers are up, but are overloaded with requests.";

    public DefaultConnector(URL url) {
        if (url == null) throw new IllegalArgumentException("Arguments must not be null");
        this.url = url;
        this.parametersGET = new HashMap<String, GETParameter>();
        this.parametersPOST = new HashMap<String, POSTParameter>();
    }

    public void addParameter(String key, IParameter parameter) {
        if (parameter instanceof GETParameter) {
            this.parametersGET.put(key, (GETParameter) parameter);
        } else if (parameter instanceof POSTParameter) {
            this.parametersPOST.put(key, (POSTParameter) parameter);
        } else {
            throw new IllegalArgumentException("Supplied parameter is not valid. Only allowes POST and GET pararmeters.");
        }
    }

    public void addParameter(String key, GETParameter parameter) {
        this.parametersGET.put(key, parameter);
    }

    public void addParameter(String key, POSTParameter parameter) {
        this.parametersPOST.put(key, parameter);
    }

    public Set<String> getKeyParameters() {
        Set<String> keySet = this.parametersGET.keySet();
        keySet.addAll(this.parametersPOST.keySet());
        return keySet;
    }

    public IParameter getParameter(String key) {
        IParameter parameter = this.parametersGET.get(key);
        if (parameter == null) parameter = this.parametersPOST.get(key);
        return parameter;
    }

    public boolean checkParameters() {
        return true;
    }

    private URL getURL() {
        return this.url;
    }

    private String getErrorResponseText(HttpURLConnection connection) {
        if (connection == null) return null;
        String result = "";
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
            result = reader.readLine();
            String tmp = "";
            while ((tmp = reader.readLine()) != null) {
                result += tmp;
            }
        } catch (IOException except) {
            result = null;
        }
        result = (result.equals("")) ? null : result;
        return result;
    }

    private String createErrorResponseText(final String errorMessage) {
        StringBuffer sb = new StringBuffer("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n").append("<hash>\n").append("<request>").append(this.getURL().getPath().substring(this.getURL().getPath().indexOf("/"))).append("</request>\n").append("<error>").append(errorMessage).append("</error>\n").append("</hash>");
        return sb.toString();
    }

    public InputStream execute() throws TaskException {
        try {
            StringBuffer sUrl = new StringBuffer(this.url.toString());
            if (this.parametersGET.size() > 0) {
                sUrl.append("?");
                for (String key : this.parametersGET.keySet()) {
                    sUrl.append(key);
                    sUrl.append("=");
                    sUrl.append(this.parametersGET.get(key).getValue());
                    sUrl.append("&");
                }
                sUrl.deleteCharAt(sUrl.length() - 1);
            }
            this.url = new URL(sUrl.toString());
            HttpURLConnection connection = (HttpURLConnection) this.url.openConnection();
            if (this.parametersPOST.size() > 0) {
                connection.setRequestMethod("POST");
                for (POSTParameter parameter : this.parametersPOST.values()) {
                    connection.addRequestProperty(parameter.getKey(), parameter.getValue());
                }
            }
            try {
                return connection.getInputStream();
            } catch (IOException except) {
                int response = connection.getResponseCode();
                String rText = this.getErrorResponseText(connection);
                switch(response) {
                    case HttpURLConnection.HTTP_BAD_REQUEST:
                        rText = (rText == null) ? this.createErrorResponseText(this.HTTP_BAD_REQUEST_ERROR) : rText;
                        throw new BadRequestException(rText);
                    case HttpURLConnection.HTTP_UNAUTHORIZED:
                        rText = (rText == null) ? this.createErrorResponseText(this.HTTP_UNAUTHORIZED_ERROR) : rText;
                        throw new AuthenticationException(rText);
                    case HttpURLConnection.HTTP_FORBIDDEN:
                        rText = (rText == null) ? this.createErrorResponseText(this.HTTP_FORBIDDEN_ERROR) : rText;
                        throw new TaskException(rText);
                    case HttpURLConnection.HTTP_NOT_FOUND:
                        rText = (rText == null) ? this.createErrorResponseText(this.HTTP_NOT_FOUND_ERROR) : rText;
                        throw new URLNotFoundException(rText);
                    case HttpURLConnection.HTTP_INTERNAL_ERROR:
                        rText = (rText == null) ? this.createErrorResponseText(this.HTTP_INTERNAL_ERROR) : rText;
                        throw new InternalServerException(rText);
                    case HttpURLConnection.HTTP_BAD_GATEWAY:
                        rText = (rText == null) ? this.createErrorResponseText(this.HTTP_BAD_GATEWAY_ERROR) : rText;
                        throw new BadGatewayException(rText);
                    case HttpURLConnection.HTTP_UNAVAILABLE:
                        rText = (rText == null) ? this.createErrorResponseText(this.HTTP_SERVICE_UNAVAILABLE_ERROR) : rText;
                        throw new ServiceUnavailableException(rText);
                    default:
                        throw new TaskException("Something went wrong");
                }
            }
        } catch (ProtocolException e) {
            throw new TaskException(e);
        } catch (MalformedURLException e) {
            throw new TaskException(e);
        } catch (IOException e) {
            throw new TaskException(e);
        }
    }

    public IConnector clone() {
        IConnector task = new DefaultConnector(this.url);
        for (GETParameter parameter : this.parametersGET.values()) {
            task.addParameter(parameter.getKey(), parameter);
        }
        for (POSTParameter parameter : this.parametersPOST.values()) {
            task.addParameter(parameter.getKey(), parameter);
        }
        return task;
    }
}
