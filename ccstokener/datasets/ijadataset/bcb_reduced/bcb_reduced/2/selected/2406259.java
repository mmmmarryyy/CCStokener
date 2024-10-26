package audio.player;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import audio.decoder.JavaLayerException;

/**
 * 
 * @author B.P.
 */
public class jlp {

    private String fFilename = null;

    private boolean remote = false;

    private boolean loop = false;

    private boolean runs = false;

    private Player player;

    public jlp() {
    }

    private void play() throws JavaLayerException {
        if (runs) {
            this.stop();
        }
        try {
            InputStream in = null;
            if (remote == true) in = getURLInputStream(); else in = getInputStream();
            AudioDevice dev = getAudioDevice();
            player = new Player(in, dev);
            player.play();
        } catch (Exception ex) {
            throw new JavaLayerException("Problem playing file " + fFilename, ex);
        }
    }

    public void playMp3(String file, boolean rem) {
        remote = rem;
        fFilename = file;
        (new Thread() {

            public void run() {
                try {
                    play();
                } catch (Exception e) {
                }
            }
        }).start();
    }

    public void loopMp3(String file, boolean rem) {
        remote = rem;
        fFilename = file;
        loop = true;
        (new Thread() {

            public void run() {
                try {
                    while (loop) play();
                } catch (Exception e) {
                }
            }
        }).start();
    }

    public void stop() {
        loop = false;
        try {
            player.close();
        } catch (Exception e) {
        }
    }

    /**
	 * Playing file from URL (Streaming).
	 */
    protected InputStream getURLInputStream() throws Exception {
        URL url = new URL(fFilename);
        InputStream fin = url.openStream();
        BufferedInputStream bin = new BufferedInputStream(fin);
        return bin;
    }

    /**
	 * Playing file from FileInputStream.
	 */
    protected InputStream getInputStream() throws IOException {
        InputStream fin;
        try {
            fin = new FileInputStream(fFilename);
        } catch (Exception e) {
            fin = getClass().getResourceAsStream("/" + fFilename);
        }
        BufferedInputStream bin = new BufferedInputStream(fin);
        return bin;
    }

    protected AudioDevice getAudioDevice() throws JavaLayerException {
        return FactoryRegistry.systemRegistry().createAudioDevice();
    }

    /**
	 * For Midi Playback
	 */
    private Sequencer sequencer;

    public void playMidi(String afile, boolean rem) {
        try {
            Sequence sequence;
            if (rem) {
                InputStream in = getClass().getResourceAsStream("/" + afile);
                sequence = MidiSystem.getSequence(in);
            } else {
                sequence = MidiSystem.getSequence(new URL(afile));
            }
            sequencer = MidiSystem.getSequencer();
            sequencer.open();
            sequencer.setSequence(sequence);
            sequencer.start();
        } catch (MalformedURLException e) {
        } catch (IOException e) {
        } catch (MidiUnavailableException e) {
        } catch (InvalidMidiDataException e) {
        }
    }

    /**
	 * stops Midi Playback
	 * 
	 */
    public void stopMidi() {
        sequencer.stop();
    }

    boolean bloed = false;

    public void playWav(String path, boolean loop) {
        try {
            InputStream in = getClass().getResourceAsStream("/" + path);
            AudioInputStream stream = AudioSystem.getAudioInputStream(in);
            AudioFormat format = stream.getFormat();
            DataLine.Info info = new DataLine.Info(Clip.class, stream.getFormat(), ((int) stream.getFrameLength() * format.getFrameSize()));
            Clip clip = (Clip) AudioSystem.getLine(info);
            bloed = true;
            clip.open(stream);
            if (loop) {
                clip.loop(Clip.LOOP_CONTINUOUSLY);
            } else {
                clip.start();
            }
            if (!clip.isRunning()) {
                bloed = false;
            }
        } catch (MalformedURLException e) {
        } catch (IOException e) {
        } catch (LineUnavailableException e) {
        } catch (UnsupportedAudioFileException e) {
            e.printStackTrace();
        }
    }
}
