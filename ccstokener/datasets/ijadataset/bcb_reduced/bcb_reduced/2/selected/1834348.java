package org.itemscript.standard;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;
import org.itemscript.core.JsonSystem;
import org.itemscript.core.connectors.ConnectorBase;
import org.itemscript.core.connectors.SyncGetConnector;
import org.itemscript.core.connectors.SyncPostConnector;
import org.itemscript.core.connectors.SyncPutConnector;
import org.itemscript.core.connectors.SyncQueryConnector;
import org.itemscript.core.exceptions.ItemscriptError;
import org.itemscript.core.url.Url;
import org.itemscript.core.util.StaticJsonUtil;
import org.itemscript.core.values.ItemscriptPutResponse;
import org.itemscript.core.values.ItemscriptRemoveResponse;
import org.itemscript.core.values.JsonArray;
import org.itemscript.core.values.JsonObject;
import org.itemscript.core.values.JsonValue;
import org.itemscript.core.values.PutResponse;
import org.itemscript.core.values.RemoveResponse;

/**
 * HTTP Connector for the standard-Java configuration.
 * <p>
 * Associated with the <code>http:</code> and <code>https:</code> schemes in the standard-Java configuration.
 * 
 * @author Jacob Davies<br/><a href="mailto:jacob@itemscript.org">jacob@itemscript.org</a>
 */
public final class HttpConnector extends ConnectorBase implements SyncGetConnector, SyncPutConnector, SyncPostConnector, SyncQueryConnector {

    private static final String CONTENT_TYPE = "Content-Type";

    private static final String APPLICATION_JSON = "application/json";

    /**
     * Create a new HttpConnector.
     * 
     * @param system The associated JsonSystem.
     */
    public HttpConnector(JsonSystem system) {
        super(system);
    }

    private JsonObject createMeta(URLConnection connection) {
        JsonObject meta = system().createObject();
        Map<String, List<String>> headers = connection.getHeaderFields();
        for (String key : headers.keySet()) {
            List<String> values = headers.get(key);
            if (values.size() == 1) {
                meta.put(key, values.get(0));
            } else {
                JsonArray headerList = meta.createArray(key);
                for (int i = 0; i < values.size(); ++i) {
                    headerList.add(values.get(i));
                }
            }
        }
        return meta;
    }

    public JsonValue get(Url url) {
        try {
            URLConnection connection = new URL(url + "").openConnection();
            return createItemFromResponse(url, connection);
        } catch (IOException e) {
            throw ItemscriptError.internalError(this, "get.IOException", e);
        }
    }

    private JsonValue createItemFromResponse(Url url, URLConnection connection) throws IOException {
        String contentType = connection.getContentType();
        if (StaticJsonUtil.looksLikeJson(url, contentType)) {
            return system().createItem(url + "", createMeta(connection), StandardUtil.readJson(system(), new InputStreamReader(connection.getInputStream()))).value();
        } else if (contentType.startsWith("text")) {
            return system().createItem(url + "", createMeta(connection), StandardUtil.readText(system(), new BufferedReader(new InputStreamReader(connection.getInputStream())))).value();
        } else {
            return system().createItem(url + "", createMeta(connection), StandardUtil.readBinary(system(), connection.getInputStream())).value();
        }
    }

    public PutResponse post(Url url, JsonValue value) {
        try {
            URL javaUrl = new URL(url + "");
            HttpURLConnection connection = (HttpURLConnection) javaUrl.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty(CONTENT_TYPE, APPLICATION_JSON);
            connection.connect();
            Writer w = new OutputStreamWriter(connection.getOutputStream());
            w.write(value.toCompactJsonString());
            w.close();
            JsonValue retValue = null;
            if (connection.getContentLength() > 0) {
                retValue = createItemFromResponse(url, connection);
            }
            return new ItemscriptPutResponse(url + "", createMeta(connection), retValue);
        } catch (IOException e) {
            throw ItemscriptError.internalError(this, "post.IOException", e);
        }
    }

    public PutResponse put(Url url, JsonValue value) {
        try {
            URL javaUrl = new URL(url + "");
            HttpURLConnection connection = (HttpURLConnection) javaUrl.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("PUT");
            connection.setRequestProperty(CONTENT_TYPE, APPLICATION_JSON);
            connection.connect();
            Writer w = new OutputStreamWriter(connection.getOutputStream());
            w.write(value.toCompactJsonString());
            w.close();
            JsonValue retValue = null;
            if (connection.getContentLength() > 0) {
                retValue = createItemFromResponse(url, connection);
            }
            return new ItemscriptPutResponse(url + "", createMeta(connection), retValue);
        } catch (IOException e) {
            throw ItemscriptError.internalError(this, "put.IOException", e);
        }
    }

    public JsonValue query(Url url) {
        return get(url);
    }

    public RemoveResponse remove(Url url) {
        try {
            URL javaUrl = new URL(url + "");
            HttpURLConnection connection = (HttpURLConnection) javaUrl.openConnection();
            connection.setRequestMethod("DELETE");
            connection.connect();
            int response = connection.getResponseCode();
            String responseMessage = connection.getResponseMessage();
            return new ItemscriptRemoveResponse(createMeta(connection));
        } catch (IOException e) {
            throw ItemscriptError.internalError(this, "remove.IOException", e);
        }
    }
}
