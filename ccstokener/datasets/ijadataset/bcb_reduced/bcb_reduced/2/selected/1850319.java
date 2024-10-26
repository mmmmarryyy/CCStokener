package ar.com.jkohen.applet;

import java.applet.*;
import java.net.*;
import java.io.*;
import java.lang.reflect.*;

public class NewAudioClip implements AudioClip {

    private AudioClip au;

    private URL url;

    private Object as, cas, play;

    private Constructor constructor;

    private Method start;

    private Method stop;

    public NewAudioClip(URL u) throws Exception {
        if (u == null) throw new IllegalArgumentException("Invalid audioclip URL"); else this.url = u;
        try {
            audioClip();
        } catch (Exception ex1) {
            try {
                sunClip();
            } catch (Exception ex2) {
            }
            ;
        }
    }

    private void audioClip() throws Exception {
        Class AudioClass = Applet.class;
        Class args[] = { URL.class };
        Method newclip = AudioClass.getMethod("newAudioClip", args);
        Object params[] = { url };
        Object obj = newclip.invoke(null, params);
        au = (AudioClip) obj;
    }

    private void sunClip() throws Exception {
        Class AudioClass = Class.forName("sun.audio.AudioStream");
        constructor = AudioClass.getConstructor(new Class[] { InputStream.class });
        Class PlayerClass = Class.forName("sun.audio.AudioPlayer");
        Field player = PlayerClass.getField("player");
        play = player.get(null);
        start = PlayerClass.getMethod("start", new Class[] { InputStream.class });
        stop = PlayerClass.getMethod("stop", new Class[] { InputStream.class });
    }

    public void play() {
        try {
            if (play != null && start != null) {
                as = constructor.newInstance(new Object[] { url.openStream() });
                start.invoke(play, new Object[] { as });
                return;
            }
            if (au != null) au.play();
        } catch (Exception ex) {
        }
    }

    public void stop() {
        try {
            if (play != null && stop != null) {
                if (as != null) {
                    stop.invoke(play, new Object[] { as });
                    as = null;
                }
                if (cas != null) {
                    stop.invoke(play, new Object[] { cas });
                    cas = null;
                }
                return;
            }
            if (au != null) au.stop();
        } catch (Exception ex) {
        }
    }

    public void loop() {
        try {
            if (play != null && start != null) {
                as = constructor.newInstance(new Object[] { url.openStream() });
                Class AudioClass = Class.forName("sun.audio.AudioStream");
                Method getData = AudioClass.getMethod("getData", new Class[] {});
                Object data = getData.invoke(as, new Object[] {});
                Class CASClass = Class.forName("sun.audio.ContinuousAudioDataStream");
                Constructor constructor = CASClass.getConstructor(new Class[] { Class.forName("sun.audio.AudioData") });
                cas = constructor.newInstance(new Object[] { data });
                start.invoke(play, new Object[] { cas });
                return;
            }
            if (au != null) au.loop();
        } catch (Exception ex) {
        }
    }
}
