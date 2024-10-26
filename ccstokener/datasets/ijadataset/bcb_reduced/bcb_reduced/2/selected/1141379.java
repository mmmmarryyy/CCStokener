package soundlibrary;

import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.sound.sampled.*;
import javazoom.jl.player.Player;

/**
 * This class creates a thread that will stream audio from a URL. It can handle
 * all uncompressed streams that Java can natively handle (.wav, .aiff, .au,
 * etc.). It also uses the JavaZoom library to decode and play mp3 files as
 * well. When the thread should be stopped, a request must be sent in.
 * @author dan
 */
public class StreamThread extends Thread {

    private URL url = null;

    private Player mp3_player;

    private Clip clip;

    /**
     * This is an optional constructor that takes in the source URL for the
     * thread. 
     * @param source_url
     */
    public StreamThread(URL source_url) {
        url = source_url;
    }

    /**
     * The constructor can also be called with no arguments. If this is the
     * case, the method setStream() must be called to set the stream URL. If it
     * is not and the StreamThread is run, it will throw an exception.
     */
    public StreamThread() {
    }

    /**
     * This method is used to set the source URL of the StreamThread.
     * @param source_url
     */
    public void setStream(URL source_url) {
        url = source_url;
    }

    /**
     * Because there is no way of knowing how long certain file types may play,
     * the Thread uses a loop when the sound is beign streamed. This loop
     * continues until the sound is finished or a stop is requested. This is
     * intended to be used by the user to inplement a manual stop.
     */
    public void requestStop() {
        if (mp3_player != null) mp3_player.close();
        if (clip != null && clip.isRunning()) {
            clip.close();
            clip.drain();
        }
    }

    /**
     * This is the main guts of the thread. It first resets the stop_requested
     * variable. Then it checks to see if the thread has been given a URL
     * (throwing an exception if it has not). Then it decides if the URL given
     * corresponds to a supported file type. If it is, then it streams that
     * sound and waits until the sound is finished or a stop is requested.
     */
    public void run() {
        try {
            if (url == null) throw new Exception("URL uninitialized.");
            if (url.getFile().endsWith(".mp3")) {
                url.openConnection();
                BufferedInputStream input_stream = new BufferedInputStream(url.openStream());
                mp3_player = new Player(input_stream);
                mp3_player.play();
                while (!mp3_player.isComplete()) {
                    Thread.yield();
                }
            } else if (url.getFile().endsWith(".wav") || url.getFile().endsWith(".aiff") || url.getFile().endsWith(".au")) {
                url.openConnection();
                AudioInputStream soundIn = AudioSystem.getAudioInputStream(new BufferedInputStream(url.openStream()));
                AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, AudioSystem.NOT_SPECIFIED, 16, 2, 4, AudioSystem.NOT_SPECIFIED, true);
                DataLine.Info info = new DataLine.Info(Clip.class, format);
                clip = (Clip) AudioSystem.getLine(info);
                clip.open(soundIn);
                clip.start();
                while (clip.isRunning()) {
                    Thread.yield();
                }
            } else {
                throw new Exception("Unsupported File Type");
            }
        } catch (Exception e) {
            System.err.println(e);
        }
    }
}
