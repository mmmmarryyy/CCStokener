package jp.joogoo.web.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import org.slim3.datastore.Datastore;
import com.google.api.client.http.HttpParser;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonHttpParser;
import com.google.api.client.util.DateTime;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.repackaged.org.json.JSONArray;
import com.google.appengine.repackaged.org.json.JSONException;
import com.google.appengine.repackaged.org.json.JSONObject;
import jp.joogoo.web.model.T_new;

public class YouroomFetcher implements FetcherService {

    private static final Logger logger = Logger.getLogger(YouroomFetcher.class.getName());

    public void setUpTransport(HttpTransport transport) {
        HttpParser parser = new JsonHttpParser();
        transport.addParser(parser);
    }

    public List<T_new> executeGet(HttpTransport transport, String targetUrl) throws HttpResponseException, IOException {
        HttpRequest req = transport.buildGetRequest();
        req.setUrl(targetUrl);
        HttpResponse response = req.execute();
        JSONArray entries;
        try {
            entries = new JSONArray(response.parseAsString());
        } catch (JSONException e) {
            throw new IOException(e.getMessage());
        }
        List<T_new> results = new ArrayList<T_new>();
        for (int i = 0, len = entries.length(); i < len; i++) {
            T_new news = new T_new();
            try {
                JSONObject r = entries.getJSONObject(i);
                JSONObject entry = r.getJSONObject("entry");
                news.setNewText(entry.getString("content"));
                DateTime d = DateTime.parseRfc3339(entry.getString("updated_at"));
                news.setCreatedAt(new Date(d.value));
                StringBuilder buffer = new StringBuilder();
                int id = entry.getInt("id");
                buffer.append(id);
                int root = entry.optInt("root_id");
                if (root > 0) {
                    buffer.append("@");
                    buffer.append(root);
                }
                int parent = entry.optInt("parent_id");
                if (parent > 0) {
                    buffer.append("@");
                    buffer.append(parent);
                }
                Key key = Datastore.createKey(T_new.class, buffer.toString());
                news.setKey(key);
                JSONObject author = entry.getJSONObject("participation");
                if (author != null) {
                    news.setAuthor(author.getString("name"));
                    JSONObject group = author.getJSONObject("group");
                    news.setTitle(group.getString("name"));
                    String groupId = group.getString("to_param");
                    String url = String.format("https://www.youroom.in/r/%s/entries/%d", groupId, id);
                    news.setContentUrl(url);
                }
            } catch (JSONException e) {
                logger.warning(e.getMessage());
                continue;
            }
            results.add(news);
        }
        return results;
    }
}
