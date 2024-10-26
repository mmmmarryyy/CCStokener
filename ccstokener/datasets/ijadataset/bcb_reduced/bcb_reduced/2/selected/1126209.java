package vqwiki.search;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.apache.commons.io.IOUtils;

public abstract class AbstractAttachmentIndexer implements AttachmentIndexer {

    public abstract String index(InputStream inputStream);

    public String index(File file) {
        InputStream is = null;
        try {
            is = new FileInputStream(file);
            return index(is);
        } catch (Throwable t) {
        } finally {
            IOUtils.closeQuietly(is);
        }
        return "";
    }

    public String index(URL url) {
        InputStream is = null;
        try {
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setInstanceFollowRedirects(true);
            urlConnection.setAllowUserInteraction(false);
            System.setProperty("sun.net.client.defaultConnectTimeout", "15000");
            System.setProperty("sun.net.client.defaultReadTimeout", "15000");
            urlConnection.connect();
            is = urlConnection.getInputStream();
            return index(is);
        } catch (Throwable t) {
        } finally {
            IOUtils.closeQuietly(is);
        }
        return "";
    }
}
