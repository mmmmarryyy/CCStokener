package com.evolution.player.jamendo.verifier;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedHashSet;
import com.evolution.player.jamendo.common.JamendoUtils;
import com.evolution.player.jamendo.verifier.HashInfoFailure.UnknowHashReason;

/**
 * @since 0.6
 */
public class RequestQueue {

    private final LinkedHashSet<String> fQueue;

    private final HashCache fCache;

    public RequestQueue(HashCache cache) {
        fCache = cache;
        fQueue = new LinkedHashSet<String>();
        new Thread("Verifier Queue") {

            @Override
            public void run() {
                while (true) {
                    String id;
                    synchronized (fQueue) {
                        while (fQueue.isEmpty()) {
                            try {
                                fQueue.wait();
                            } catch (InterruptedException e) {
                            }
                        }
                        id = fQueue.iterator().next();
                    }
                    HashInfo hashInfo = createHash(id);
                    fCache.put(id, hashInfo);
                    synchronized (fQueue) {
                        fQueue.remove(id);
                    }
                }
            }
        }.start();
    }

    public void addRequest(String id) {
        synchronized (fQueue) {
            if (!fQueue.contains(id)) {
                fQueue.add(id);
                fQueue.notifyAll();
            }
        }
    }

    private HashInfo createHash(String id) {
        try {
            URL url = new URL("http://api.jamendo.com/get2/stream/track/redirect/?id=" + id + "&streamencoding=mp31");
            URLConnection connection = url.openConnection();
            connection.setConnectTimeout(30 * 1000);
            connection.setReadTimeout(30 * 1000);
            connection.connect();
            int length = connection.getContentLength();
            if (length == 0) return new HashInfoFailure(id, UnknowHashReason.UNKNOWN_JAMENDO_ID);
            return new HashInfoSuccess(id, JamendoUtils.getHash(connection.getInputStream()));
        } catch (MalformedURLException e) {
            return new HashInfoFailure(id, UnknowHashReason.UNKNOWN_JAMENDO_ID);
        } catch (FileNotFoundException e) {
            return new HashInfoFailure(id, UnknowHashReason.UNKNOWN_JAMENDO_ID);
        } catch (IOException e) {
            return new HashInfoFailure(id, UnknowHashReason.UNKNOWN_JAMENDO_ID);
        }
    }
}
