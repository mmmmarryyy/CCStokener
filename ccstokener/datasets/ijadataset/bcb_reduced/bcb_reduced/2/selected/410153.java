package org.mobicents.cloud.scaler.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Handle HTTP requests between a client and a host.
 * 
 * @author Thibault Leruitte
 */
public class HTTPClient {

    /**
	 * HTTP requests supported.
	 * 
	 * @author Thibault Leruitte
	 */
    public enum Method {

        GET, POST
    }

    /** URL contacted by this client. */
    private String url;

    /**
	 * Construct a new HTTP client for a specific host and port.
	 * @param host
	 * @param port
	 */
    public HTTPClient(String host, int port) {
        this.url = "http://" + host + ":" + port;
    }

    /**
	 * Construct a new HTTP client for a specific URL.
	 * @param host
	 * @param port
	 */
    public HTTPClient(String url) {
        this.url = url;
    }

    /**
	 * Execute the GET request.
	 * @param action
	 * @return
	 */
    public String get() {
        return get(null);
    }

    /**
	 * Execute the GET request on a subpath of the URL.
	 * @param action
	 * @return
	 */
    public String get(String action) {
        return doRequest(Method.GET, action);
    }

    /**
	 * Execute the POST request.
	 * @param action
	 * @param params
	 * @return
	 */
    public String post(PostParameter... params) {
        return post(null, params);
    }

    /**
	 * Execute the POST request on a subpath of the URL.
	 * @param action
	 * @param params
	 * @return
	 */
    public String post(String action, PostParameter... params) {
        return doRequest(Method.POST, action, params);
    }

    /**
	 * Do a request.
	 * @param method
	 * @param action
	 * @param params
	 * @return
	 */
    private String doRequest(Method method, String action, PostParameter... params) {
        StringBuilder output = new StringBuilder();
        BufferedReader reader = null;
        OutputStreamWriter writer = null;
        try {
            String target = (action == null) ? url : url + action;
            URL url = new URL(target);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method.toString());
            connection.setRequestProperty("Accept", "text/xml");
            if (method == Method.POST) {
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                connection.setRequestProperty("X-Requested-With", "XMLHttpRequest");
            }
            connection.setDoOutput(method == Method.POST);
            connection.setDoInput(true);
            connection.setReadTimeout(30 * 1000);
            connection.connect();
            if (method == Method.POST) {
                assert params != null;
                writer = new OutputStreamWriter(connection.getOutputStream());
                for (PostParameter param : params) {
                    writer.write(param.encode());
                }
                writer.close();
                writer = null;
            }
            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line = null;
            while ((line = reader.readLine()) != null) {
                output.append(line + "\n");
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return output.toString();
    }
}
