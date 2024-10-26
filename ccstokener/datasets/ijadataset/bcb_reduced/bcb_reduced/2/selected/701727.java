package de.zeitfuchs.networkfinder.core.internal;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import de.zeitfuchs.networkfinder.core.ISeite;
import de.zeitfuchs.networkfinder.core.ISuchTools;
import de.zeitfuchs.networkfinder.core.ISuchmaschine;
import de.zeitfuchs.networkfinder.core.NetworkfinderCoreException;
import de.zeitfuchs.networkfinder.core.NetworkfinderUrlException;

public class SuchToolsImpl implements ISuchTools {

    int DEFAULT_READ_TIMEOUT = 10000;

    @Override
    public HttpURLConnection getHttpConnection(String sUrl) throws MalformedURLException, IOException {
        return getHttpConnection(sUrl, DEFAULT_READ_TIMEOUT);
    }

    @Override
    public HttpURLConnection getHttpConnection(String sUrl, int timeout) throws MalformedURLException, IOException {
        URL url = new URL(sUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(timeout);
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setRequestProperty("Accept", "text/*,application/xml,application/xhtml+xml");
        conn.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; .NET CLR 1.1.4322)");
        conn.connect();
        return conn;
    }

    @Override
    public ISeite getSeite(String seitenID, String url, ISuchmaschine suchmaschine) throws NetworkfinderCoreException, NetworkfinderUrlException {
        ISeite seite = null;
        try {
            HttpURLConnection conn = getHttpConnection(url);
            seite = new HtmlSeiteImpl(seitenID, url, conn.getInputStream(), conn.getContentType());
        } catch (MalformedURLException e) {
            throw new NetworkfinderUrlException(url, e);
        } catch (IOException e) {
            throw new NetworkfinderCoreException(e);
        }
        return seite;
    }

    @Override
    public List<ISeite> getSeiten(List<String> urls, ISuchmaschine suchmaschine) throws NetworkfinderCoreException, NetworkfinderUrlException {
        List<ISeite> seiten = new ArrayList<ISeite>();
        for (String link : urls) {
            try {
                HttpURLConnection conn = getHttpConnection(link);
                ISeite seite = new HtmlSeiteImpl(null, link, conn.getInputStream(), conn.getContentType());
                seiten.add(seite);
            } catch (MalformedURLException e) {
                throw new NetworkfinderUrlException(link, e);
            } catch (IOException e) {
                throw new NetworkfinderCoreException(e);
            }
        }
        return seiten;
    }

    @Override
    public void mergeTrefferListen(List<ISeite> quellTreffer, List<ISeite> zielTreffer) {
    }
}
