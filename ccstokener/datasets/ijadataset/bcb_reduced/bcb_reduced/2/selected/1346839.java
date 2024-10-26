package free.util.audio;

import free.util.IOUtilities;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;

/**
 * This class allows you to play sounds in an application in JDK 1.1 - it uses
 * a variety of hacks (implementations of AudioPlayer) to try and play the sound
 * and is thus not platform independent, but it simply won't do anything if it
 * fails. The "au" format is most likely to be supported, but others may work
 * too.
 */
public class AudioClip {

    /**
   * The AudioPlayer that succeeded in playing sounds. This one, if not null is
   * tried first.
   */
    private static AudioPlayer successfulPlayer = null;

    /**
   * A list of classnames of AudioPlayer implementations which we will try to
   * see if they succeed.
   */
    private static final String[] PLAYER_CLASSNAMES;

    /**
   * Loads the AudioPlayer classnames.
   */
    static {
        String[] classnames = new String[0];
        try {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(AudioClip.class.getResourceAsStream("players.txt")));
                ArrayList tempVec = new ArrayList();
                String line;
                while ((line = reader.readLine()) != null) tempVec.add(line);
                reader.close();
                classnames = new String[tempVec.size()];
                for (int i = 0; i < classnames.length; i++) classnames[i] = (String) tempVec.get(i);
            } catch (IOException e) {
                e.printStackTrace();
                classnames = new String[0];
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        PLAYER_CLASSNAMES = classnames;
    }

    /**
   * The URL of the audio clip.
   */
    private final URL url;

    /**
   * The audio clip data.
   */
    private final byte[] data;

    /**
   * Creates a new AudioClip from the given URL. This constructor blocks until
   * all the sound data is read from the URL. 
   */
    public AudioClip(URL url) throws IOException {
        this.url = url;
        InputStream in = url.openStream();
        this.data = IOUtilities.readToEnd(in);
        in.close();
    }

    /**
   * Attempts to play this AudioClip, the method returns immediately and never
   * throws exceptions. If playing fails, it fails silently.
   */
    public synchronized void play() {
        if (successfulPlayer != null) {
            try {
                successfulPlayer.play(this);
                return;
            } catch (Exception e) {
            }
        }
        String cmdSpecifiedPlayer;
        try {
            cmdSpecifiedPlayer = System.getProperty("free.util.audio.player");
        } catch (SecurityException e) {
            cmdSpecifiedPlayer = null;
        }
        if (cmdSpecifiedPlayer != null) {
            try {
                Class playerClass = Class.forName(cmdSpecifiedPlayer);
                AudioPlayer player = (AudioPlayer) playerClass.newInstance();
                if (!player.isSupported()) {
                    System.err.println(cmdSpecifiedPlayer + " is not supported on your system - audio disabled.");
                    successfulPlayer = new NullAudioPlayer();
                    return;
                }
                player.play(this);
                successfulPlayer = player;
            } catch (Throwable e) {
                if (e instanceof ThreadDeath) throw (ThreadDeath) e;
                System.err.println(cmdSpecifiedPlayer + " failed - audio disabled.");
                successfulPlayer = new NullAudioPlayer();
            }
        } else {
            for (int i = 0; i < PLAYER_CLASSNAMES.length; i++) {
                String classname = PLAYER_CLASSNAMES[i];
                try {
                    Class playerClass = Class.forName(classname);
                    AudioPlayer player = (AudioPlayer) playerClass.newInstance();
                    if (!player.isSupported()) continue;
                    player.play(this);
                    System.err.println("Will now use " + classname + " to play audio clips.");
                    successfulPlayer = player;
                } catch (Throwable e) {
                    if (e instanceof ThreadDeath) throw (ThreadDeath) e;
                    continue;
                }
                break;
            }
            if (successfulPlayer == null) {
                System.err.println("All supported players failed - audio disabled");
                successfulPlayer = new NullAudioPlayer();
            }
        }
    }

    /**
   * Returns the URL of the audio clip.
   */
    public URL getURL() {
        return url;
    }

    /**
   * Returns the data of the audio clip.
   */
    public byte[] getData() {
        return (byte[]) data.clone();
    }
}
