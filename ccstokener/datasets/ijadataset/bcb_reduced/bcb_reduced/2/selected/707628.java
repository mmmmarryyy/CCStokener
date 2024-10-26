package jp.joogoo.web.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.slim3.datastore.Datastore;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.xml.atom.AtomParser;
import com.google.appengine.api.datastore.Key;
import jp.joogoo.web.model.T_new;
import jp.joogoo.web.schema.cybozulive.NotifyEntry;
import jp.joogoo.web.schema.cybozulive.NotifyFeed;

public class CybozuliveFetcher implements FetcherService {

    public void setUpTransport(HttpTransport transport) {
        AtomParser parser = new AtomParser();
        parser.namespaceDictionary = NotifyFeed.NAMESPACE_DICTIONARY;
        transport.addParser(parser);
    }

    public List<T_new> executeGet(HttpTransport transport, String targetUrl) throws HttpResponseException, IOException {
        HttpRequest req = transport.buildGetRequest();
        req.setUrl(targetUrl);
        NotifyFeed feed = req.execute().parseAs(NotifyFeed.class);
        if (feed.entry == null) {
            return Collections.emptyList();
        }
        List<T_new> results = new ArrayList<T_new>();
        for (NotifyEntry e : feed.entry) {
            StringBuilder buffer = new StringBuilder();
            if (e.id != null) {
                buffer.append(e.id);
            }
            buffer.append("@");
            if (e.updated != null) {
                buffer.append(e.updated.toStringRfc3339().substring(0, 19) + "Z");
            }
            Key key = Datastore.createKey(T_new.class, buffer.toString());
            T_new news = new T_new();
            news.setTitle(e.title);
            if (e.content != null) {
                news.setNewText(e.content.substring(0, Math.min(e.content.length(), 500)));
            }
            if (e.status != null && e.content == null) {
                news.setNewText(e.status);
            }
            if (e.updated != null) {
                news.setCreatedAt(new Date(e.updated.value));
            }
            news.setContentUrl(e.getAlternate());
            if (e.author != null) {
                news.setAuthor(e.author.name);
            }
            news.setKey(key);
            results.add(news);
        }
        return results;
    }
}
