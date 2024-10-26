package org.apache.harmony.applet;

import java.applet.AudioClip;
import java.io.IOException;
import java.net.URL;

/**
 * Implementation of AudioClip interface
 */
public class AudioClipImpl implements AudioClip {

    @SuppressWarnings("unused")
    private final URL url;

    public AudioClipImpl(URL url) {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkConnect(url.getHost(), url.getPort());
            try {
                sm.checkPermission(url.openConnection().getPermission());
            } catch (IOException e) {
            }
        }
        this.url = url;
    }

    public void stop() {
        throw new UnsupportedOperationException();
    }

    public void loop() {
        throw new UnsupportedOperationException();
    }

    public void play() {
        throw new UnsupportedOperationException();
    }
}
