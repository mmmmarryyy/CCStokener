package qurtext.factory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.logging.Logger;
import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.CacheFactory;
import javax.cache.CacheManager;
import javax.jdo.PersistenceManager;
import qurtext.domain.Source;

public class SourceFactory {

    private static final Logger log = Logger.getLogger(SourceFactory.class.getName());

    Cache cache;

    public SourceFactory() {
        try {
            CacheFactory cacheFactory = CacheManager.getInstance().getCacheFactory();
            cache = cacheFactory.createCache(Collections.emptyMap());
        } catch (CacheException e) {
            log.severe(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Source getSource(String urlPath, String params) {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        try {
            String query = "select from " + Source.class.getName() + " where urlPath=='" + urlPath + "' && params==\"" + params + "\"";
            Source result = ((Collection<Source>) pm.newQuery(query).execute()).iterator().next();
            return result;
        } catch (NoSuchElementException e) {
            return null;
        } finally {
            pm.close();
        }
    }

    @SuppressWarnings("unchecked")
    public String getSourceContent(String urlPath, String params, boolean isPost, String cacheKey) {
        if (null != cacheKey) {
            String content = (String) cache.get(cacheKey);
            if (null != content) return content;
        }
        Source source = getSource(urlPath, params);
        if (null != source) {
            if (null != cacheKey) {
                String content = source.getContent();
                if (null != content) cache.put(cacheKey, content);
            }
            return source.getContent();
        }
        HttpURLConnection connection = null;
        try {
            String result = "";
            if (isPost) {
                URL url = new URL(urlPath);
                connection = (HttpURLConnection) url.openConnection();
                connection.setDoOutput(true);
                connection.setRequestMethod("POST");
                OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
                writer.write(params);
                writer.close();
            } else {
                URL url = new URL(urlPath + "?" + params);
                connection = (HttpURLConnection) url.openConnection();
            }
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8") {
                });
                String line;
                while ((line = reader.readLine()) != null) {
                    result += line;
                }
                reader.close();
                source = new Source(urlPath, params, result);
                saveSource(source);
                if (null != cacheKey) {
                    String content = source.getContent();
                    if (null != content) cache.put(cacheKey, content);
                }
                return source.getContent();
            } else {
                result = connection.getResponseMessage();
            }
        } catch (MalformedURLException e) {
            return e.getMessage();
        } catch (IOException e) {
            return e.getMessage();
        } finally {
            if (null != connection) connection.disconnect();
        }
        return "ERROR: cannot reach source";
    }

    private void saveSource(Source source) {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        try {
            pm.makePersistent(source);
        } finally {
            pm.close();
        }
    }
}
