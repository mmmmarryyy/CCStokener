package qurtext.factory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
import qurtext.domain.Media;

public class MediaFactory {

    private static final Logger log = Logger.getLogger(MediaFactory.class.getName());

    Cache cache;

    public MediaFactory() {
        try {
            CacheFactory cacheFactory = CacheManager.getInstance().getCacheFactory();
            cache = cacheFactory.createCache(Collections.emptyMap());
        } catch (CacheException e) {
            log.severe(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Media getMedia(String urlPath, String params) {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        try {
            String query = "select from " + Media.class.getName() + " where urlPath=='" + urlPath + "' && filename=='" + params + "'";
            Media result = ((Collection<Media>) pm.newQuery(query).execute()).iterator().next();
            return result;
        } catch (NoSuchElementException e) {
            return null;
        } finally {
            pm.close();
        }
    }

    @SuppressWarnings("unchecked")
    public byte[] getMediaContent(String urlPath, String params, boolean isPost, String cacheKey) {
        if (null != cacheKey) {
            byte[] content = (byte[]) cache.get(cacheKey);
            if (null != content) return content;
        }
        Media source = getMedia(urlPath, params);
        if (null != source) {
            if (null != cacheKey) {
                byte[] content = source.getContent();
                if (null != content) cache.put(cacheKey, content);
            }
            return source.getContent();
        }
        HttpURLConnection connection = null;
        try {
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            if (isPost) {
                URL url = new URL(urlPath);
                connection = (HttpURLConnection) url.openConnection();
                connection.setDoOutput(true);
                connection.setRequestMethod("POST");
                OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
                writer.write(params);
                writer.close();
            } else if (params.indexOf("=") <= 0) {
                URL url = new URL(urlPath + params);
                connection = (HttpURLConnection) url.openConnection();
            } else {
                URL url = new URL(urlPath + "?" + params);
                connection = (HttpURLConnection) url.openConnection();
            }
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                InputStream reader = connection.getInputStream();
                byte[] buffer = new byte[102400];
                int byteRead;
                while ((byteRead = reader.read(buffer)) > 0) {
                    result.write(buffer, 0, byteRead);
                }
                reader.close();
                source = new Media(urlPath, params, result.toByteArray());
                saveMedia(source);
                if (null != cacheKey) {
                    byte[] content = source.getContent();
                    if (null != content) cache.put(cacheKey, content);
                }
                return source.getContent();
            } else {
                return connection.getResponseMessage().getBytes();
            }
        } catch (MalformedURLException e) {
            return e.getMessage().getBytes();
        } catch (IOException e) {
            return e.getMessage().getBytes();
        } finally {
            if (null != connection) connection.disconnect();
        }
    }

    private void saveMedia(Media media) {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        try {
            pm.makePersistent(media);
        } finally {
            pm.close();
        }
    }

    public String getMediaText(String urlPath, String params, boolean isPost, String cacheKey) {
        return new String(getMediaContent(urlPath, params, isPost, cacheKey));
    }
}
