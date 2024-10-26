package org.xith3d.loaders.sound.impl.midi;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import org.xith3d.loaders.sound.SoundLoader;

/**
 * This is a SoundLoader implementation for Midi sounds (.mid).
 * 
 * @author Marvin Froehlich (aka Qudus)
 */
public class MidiLoader extends SoundLoader {

    /**
     * The default extension to assume for Wave files.
     */
    public static final String DEFAULT_EXTENSION = "mid";

    private static MidiLoader singletonInstance = null;

    /**
     * @return the MidiLoader instance to use as singleton.
     */
    public static MidiLoader getInstance() {
        if (singletonInstance == null) singletonInstance = new MidiLoader();
        return (singletonInstance);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MidiSoundContainer loadSound(InputStream in) throws IOException {
        try {
            return (MidiSoundContainer.load(in));
        } catch (Exception e) {
            IOException e2 = new IOException(e.getMessage());
            e2.initCause(e);
            throw e2;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MidiSoundContainer loadSound(URL url) throws IOException {
        return (loadSound(url.openStream()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MidiSoundContainer loadSound(String filename) throws IOException {
        return (loadSound(new BufferedInputStream(new FileInputStream(filename))));
    }
}
