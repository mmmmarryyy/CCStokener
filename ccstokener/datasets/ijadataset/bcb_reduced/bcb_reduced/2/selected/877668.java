package org.openremote.controller.agent;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.openremote.controller.Constants;
import org.openremote.controller.utils.Logger;

/**
 * Cheap and dirty HTTP client for the agent
 * 
 * @author Stéphane Épardaud <stef@epardaud.fr>
 */
public class RESTCall {

    private Logger log = Logger.getLogger(Constants.AGENT_LOG_CATEGORY);

    private HttpURLConnection connection;

    protected RESTCall() {
    }

    public RESTCall(String path, String user, String password) throws AgentException {
        this("GET", path, user, password);
    }

    public RESTCall(String method, String path, String user, String password) throws AgentException {
        URL url;
        try {
            url = new URL(path);
        } catch (MalformedURLException e) {
            throw new AgentException("Malformed URL: " + path, e);
        }
        try {
            connection = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            throw new AgentException("Failed to open URL connection to " + path, e);
        }
        try {
            connection.setRequestMethod(method);
        } catch (ProtocolException e) {
            throw new AgentException("Failed to use " + method + " method", e);
        }
        addAuth(user, password);
    }

    public void invoke() throws AgentException {
        try {
            connection.connect();
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new AgentException("Failed to execute REST call at " + connection.getURL() + ": " + connection.getResponseCode() + " " + connection.getResponseMessage());
            }
        } catch (ConnectException e) {
            throw new AgentException("Failed to connect to beehive at " + connection.getURL());
        } catch (IOException e) {
            throw new AgentException("Failed to connect to beehive", e);
        }
    }

    public void invoke(InputStream is) throws AgentException {
        try {
            addHeader("Content-Type", "application/zip");
            addHeader("Content-Length", String.valueOf(is.available()));
            connection.setDoOutput(true);
            connection.connect();
            OutputStream os = connection.getOutputStream();
            boolean success = false;
            try {
                IOUtils.copy(is, os);
                success = true;
            } finally {
                try {
                    os.flush();
                    os.close();
                } catch (IOException x) {
                    if (success) throw x;
                }
            }
            connection.disconnect();
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new AgentException("Failed to execute REST call at " + connection.getURL() + ": " + connection.getResponseCode() + " " + connection.getResponseMessage());
            }
        } catch (ConnectException e) {
            throw new AgentException("Failed to connect to beehive at " + connection.getURL());
        } catch (IOException e) {
            throw new AgentException("Failed to connect to beehive", e);
        }
    }

    public String getResponse() throws AgentException {
        InputStream is;
        try {
            is = connection.getInputStream();
        } catch (IOException e) {
            throw new AgentException("Failed to read data from connection", e);
        }
        try {
            return IOUtils.toString(is);
        } catch (IOException e) {
            throw new AgentException("Failed to read data from connection", e);
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                log.error("Failed to close input stream", e);
            }
            connection.disconnect();
        }
    }

    private void addAuth(String user, String password) {
        String userpass = user + ":" + password;
        String basicAuth = "Basic " + new String(Base64.encodeBase64(userpass.getBytes()));
        addHeader("Authorization", basicAuth);
    }

    public void addHeader(String header, String value) {
        connection.setRequestProperty(header, value);
    }

    public void disconnect() {
        connection.disconnect();
    }

    public InputStream getInputStream() throws IOException {
        return connection.getInputStream();
    }

    public OutputStream getOutputStream() throws IOException {
        return connection.getOutputStream();
    }
}
