package com.realtime.crossfire.jxclient.metaserver;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * Queries Crossfire's metaserver to learn about existing servers.
 * @author Lauwenmark
 * @author Andreas Kirschbaum
 */
public class Metaserver {

    /**
     * The time (in seconds) to forget about old metaserver entries.
     */
    private static final long EXPIRE_INTERVAL = 60L * 60 * 24 * 2;

    /**
     * The metaserver URL.
     */
    @NotNull
    private static final String METASERVER_URL = "http://crossfire.real-time.com/metaserver2/meta_client.php";

    /**
     * The cached metaserver entries.
     */
    @NotNull
    private final ServerCache serverCache;

    /**
     * The {@link MetaserverModel} instance to update.
     */
    @NotNull
    private final MetaserverModel metaserverModel;

    /**
     * Creates a new instance.
     * @param metaserverCacheFile the metaserver cache file
     * @param metaserverModel the metaserver model instance to update
     */
    public Metaserver(@NotNull final File metaserverCacheFile, @NotNull final MetaserverModel metaserverModel) {
        serverCache = new ServerCache(metaserverCacheFile);
        this.metaserverModel = metaserverModel;
        metaserverModel.begin();
        for (final MetaserverEntry metaserverEntry : serverCache.getAll().values()) {
            metaserverModel.add(metaserverEntry);
        }
        metaserverModel.commit();
    }

    /**
     * Updates the contents of {@link #metaserverModel}.
     */
    public void updateMetaList() {
        metaserverModel.begin();
        serverCache.expire(EXPIRE_INTERVAL * 1000);
        final Map<String, MetaserverEntry> oldEntries = serverCache.getAll();
        final MetaserverEntry localhostMetaserverEntry = MetaserverEntryParser.parseEntry(ServerCache.DEFAULT_ENTRY_LOCALHOST);
        assert localhostMetaserverEntry != null;
        metaserverModel.add(localhostMetaserverEntry);
        oldEntries.remove(ServerCache.makeKey(localhostMetaserverEntry));
        serverCache.put(localhostMetaserverEntry);
        try {
            final URL url = new URL(METASERVER_URL);
            final String httpProxy = System.getenv("http_proxy");
            if (httpProxy != null && httpProxy.length() > 0) {
                if (httpProxy.regionMatches(true, 0, "http://", 0, 7)) {
                    final String[] tmp = httpProxy.substring(7).replaceAll("/.*", "").split(":", 2);
                    System.setProperty("http.proxyHost", tmp[0]);
                    System.setProperty("http.proxyPort", tmp.length >= 2 ? tmp[1] : "80");
                } else {
                    System.err.println("Warning: unsupported http_proxy protocol: " + httpProxy);
                }
            }
            final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            try {
                conn.setRequestMethod("GET");
                conn.setUseCaches(false);
                conn.connect();
                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    final InputStream in = conn.getInputStream();
                    final InputStreamReader isr = new InputStreamReader(in, "ISO-8859-1");
                    try {
                        final BufferedReader br = new BufferedReader(isr);
                        try {
                            final MetaserverEntryParser metaserverEntryParser = new MetaserverEntryParser();
                            while (true) {
                                final String line = br.readLine();
                                if (line == null) {
                                    break;
                                }
                                final MetaserverEntry metaserverEntry = metaserverEntryParser.parseLine(line);
                                if (metaserverEntry != null) {
                                    metaserverModel.add(metaserverEntry);
                                    oldEntries.remove(ServerCache.makeKey(metaserverEntry));
                                    serverCache.put(metaserverEntry);
                                }
                            }
                        } finally {
                            br.close();
                        }
                    } finally {
                        isr.close();
                    }
                }
            } finally {
                conn.disconnect();
            }
        } catch (final IOException ignored) {
        }
        for (final MetaserverEntry metaserverEntry : oldEntries.values()) {
            metaserverModel.add(metaserverEntry);
        }
        metaserverModel.commit();
        serverCache.save();
    }
}
