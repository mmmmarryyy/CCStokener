package org.bresearch.websec.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class HttpConnect implements IHttpConnect {

    private final ConnectSettingsBean connectSettings;

    private final SystemSettingsBean systemSettings;

    private ConnectResult lastResult = new ConnectResult(-1, "");

    public HttpConnect(final ConnectSettingsBean settings, final SystemSettingsBean sysSettings) {
        this.connectSettings = settings;
        this.systemSettings = sysSettings;
    }

    public void connect(final URLConnectAdapter urlAdapter) {
        if (this.connectSettings == null) {
            throw new IllegalStateException("Invalid Connect Settings (is null)");
        }
        final HttpURLConnection httpConnection = (HttpURLConnection) urlAdapter.openConnection();
        BufferedReader in;
        try {
            in = new BufferedReader(new InputStreamReader(httpConnection.getInputStream()));
            final StringBuilder buf = new StringBuilder(200);
            String str;
            while ((str = in.readLine()) != null) {
                buf.append(str);
                buf.append('\n');
            }
            final ConnectResult result = new ConnectResult(httpConnection.getResponseCode(), buf.toString());
            final Map<String, List<String>> headerFields = httpConnection.getHeaderFields();
            for (Map.Entry<String, List<String>> entry : headerFields.entrySet()) {
                final String key = entry.getKey();
                final List<String> val = entry.getValue();
                if ((val != null) && (val.size() > 1)) {
                    System.out.println("WARN: Invalid header value : " + key + " url=" + this.connectSettings.getUrl());
                }
                if (key != null) {
                    result.addHeader(key, val.get(0), val);
                } else {
                    result.addHeader("Status", val.get(0), val);
                }
            }
            this.lastResult = result;
        } catch (IOException e) {
            throw new ConnectException(e);
        }
    }

    /**
     * @return the connectSettings
     */
    public ConnectSettingsBean getConnectSettings() {
        return connectSettings;
    }

    /**
     * @return the lastResult
     */
    public ConnectResult getLastResult() {
        return lastResult;
    }

    public URLConnectAdapter buildURL() {
        if (this.connectSettings == null) {
            throw new IllegalStateException("Invalid Connect Settings (is null)");
        }
        URLConnectAdapter adapter;
        try {
            adapter = new URLConnectAdapter(new URL(this.connectSettings.getUrl()));
        } catch (MalformedURLException e) {
            throw new ConnectException(e);
        }
        return adapter;
    }

    public void buildConnectProperties() {
        if ((this.systemSettings != null) && this.systemSettings.getProxySet()) {
            System.getProperties().put("http.agent", "" + this.systemSettings.getHttpAgent());
            System.getProperties().put("proxySet", "" + this.systemSettings.getProxySet());
            System.getProperties().put("proxyHost", "" + this.systemSettings.getProxyHost());
            System.getProperties().put("proxyPort", "" + this.systemSettings.getProxySet());
        }
    }
}
