package sun.applet;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.applet.AudioClip;
import sun.audio.*;

/**
 * Applet audio clip;
 *
 * @version 	1.12, 07/01/98
 * @author Arthur van Hoff
 */
public class AppletAudioClip implements AudioClip {

    URL url;

    AudioData data;

    InputStream stream;

    /**
     * This should really fork a seperate thread to
     * load the data.
     */
    public AppletAudioClip(URL url) {
        InputStream in = null;
        try {
            try {
                URLConnection uc = url.openConnection();
                uc.setAllowUserInteraction(true);
                in = uc.getInputStream();
                data = new AudioStream(in).getData();
            } finally {
                if (in != null) {
                    in.close();
                }
            }
        } catch (IOException e) {
            data = null;
        }
    }

    public AppletAudioClip(byte[] data) {
        this.data = new AudioData(data);
    }

    public synchronized void play() {
        stop();
        if (data != null) {
            java.awt.Toolkit.getDefaultToolkit();
            stream = new AudioDataStream(data);
            AudioPlayer.player.start(stream);
        }
    }

    public synchronized void loop() {
        stop();
        if (data != null) {
            stream = new ContinuousAudioDataStream(data);
            AudioPlayer.player.start(stream);
        }
    }

    public synchronized void stop() {
        if (stream != null) {
            AudioPlayer.player.stop(stream);
            try {
                stream.close();
            } catch (IOException e) {
            }
        }
    }

    public String toString() {
        return getClass().toString() + "[" + url + "]";
    }
}
