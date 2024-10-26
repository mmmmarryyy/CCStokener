package jaco.mp3.player;

import jaco.mp3.player.resources.Decoder;
import jaco.mp3.player.resources.Frame;
import jaco.mp3.player.resources.SampleBuffer;
import jaco.mp3.player.resources.SoundStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.BooleanControl;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.Line;
import javax.sound.sampled.SourceDataLine;

/**
 * Java MP3
 * 
 * @version 1.00, April 15, 2011
 * @author Cristian Sulea ( http://cristiansulea.entrust.ro )
 */
public class MP3 {

    public static void main(String[] args) {
        new MP3("E:/Mp3/mp3/Mungo Jerry - In the summertime.mp3").play();
    }

    private static final Logger LOGGER = Logger.getLogger(MP3.class.getName());

    private File file;

    private URL url;

    private int volume = 100;

    private boolean isPaused = false;

    private boolean isStopped = true;

    private SourceDataLine source;

    public MP3(String file) {
        this(new File(file));
    }

    public MP3(File file) {
        this.file = file;
    }

    public MP3(URL url) {
        this.url = url;
    }

    public void setVolume(int volume) {
        this.volume = volume;
        if (source != null) {
            try {
                FloatControl gainControl = (FloatControl) source.getControl(FloatControl.Type.MASTER_GAIN);
                BooleanControl muteControl = (BooleanControl) source.getControl(BooleanControl.Type.MUTE);
                if (volume == 0) {
                    muteControl.setValue(true);
                } else {
                    muteControl.setValue(false);
                    gainControl.setValue((float) (Math.log(volume / 100d) / Math.log(10.0) * 20.0));
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "unable to set the volume to the source", e);
            }
        }
    }

    public int getVolume() {
        return volume;
    }

    public void play() {
        if (isPaused) {
            isPaused = false;
            return;
        }
        isStopped = false;
        Decoder decoder = new Decoder();
        SoundStream stream = null;
        try {
            if (file != null) {
                stream = new SoundStream(new FileInputStream(file));
            } else if (url != null) {
                stream = new SoundStream(url.openStream());
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "unable to open the sound stream", e);
        }
        if (stream != null) {
            while (true) {
                if (isStopped) {
                    break;
                }
                if (isPaused) {
                    if (source != null) {
                        source.flush();
                    }
                    try {
                        Thread.sleep(10);
                    } catch (Exception e) {
                    }
                    continue;
                }
                try {
                    Frame frame = stream.readFrame();
                    if (frame == null) {
                        break;
                    }
                    if (source == null) {
                        int frequency = frame.frequency();
                        int channels = frame.mode() == Frame.SINGLE_CHANNEL ? 1 : 2;
                        AudioFormat format = new AudioFormat(frequency, 16, channels, true, false);
                        Line line = AudioSystem.getLine(new DataLine.Info(SourceDataLine.class, format));
                        source = (SourceDataLine) line;
                        source.open(format);
                        source.start();
                        setVolume(volume);
                    }
                    SampleBuffer output = (SampleBuffer) decoder.decodeFrame(frame, stream);
                    short[] buffer = output.getBuffer();
                    int offs = 0;
                    int len = output.getBufferLength();
                    source.write(toByteArray(buffer, offs, len), 0, len * 2);
                    stream.closeFrame();
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "unexpected problems while playing " + toString(), e);
                    break;
                }
            }
            if (!isStopped) {
                source.drain();
            } else {
                source.flush();
            }
            source.stop();
            source.close();
            source = null;
            try {
                stream.close();
            } catch (Exception e) {
            }
        }
        isStopped = true;
    }

    public void pause() {
        isPaused = true;
    }

    public void stop() {
        isStopped = true;
    }

    /**
	 * Retrieves the position in milliseconds of the current audio sample being
	 * played. This method delegates to the <code>
	 * AudioDevice</code> that is used by this player to sound the decoded audio
	 * samples.
	 * 
	 * TODO: might not be correct - what if i try to play 2 files in the same
	 * time?
	 */
    public int getPosition() {
        int pos = 0;
        if (source != null) {
            pos = (int) (source.getMicrosecondPosition() / 1000);
        }
        return pos;
    }

    @Override
    public String toString() {
        if (file != null) {
            return file.getAbsolutePath();
        } else if (url != null) {
            return url.toString();
        }
        return super.toString();
    }

    private byte[] toByteArray(short[] ss, int offs, int len) {
        byte[] bb = new byte[len * 2];
        int idx = 0;
        short s;
        while (len-- > 0) {
            s = ss[offs++];
            bb[idx++] = (byte) s;
            bb[idx++] = (byte) (s >>> 8);
        }
        return bb;
    }
}
