package org.xith3d.loaders.sound.impl.ogg;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import org.xith3d.loaders.sound.SoundLoader;

/**
 * This is a SoundLoader implementation for Oggle Vorbis sounds (.ogg).
 * 
 * @author Marvin Froehlich (aka Qudus)
 */
public class OggLoader extends SoundLoader {

    /**
     * The default extension to assume for Oggle Vorbis files.
     */
    public static final String DEFAULT_EXTENSION = "ogg";

    private static OggLoader singletonInstance = null;

    /**
     * @return the OggLoader instance to use as singleton.
     */
    public static OggLoader getInstance() {
        if (singletonInstance == null) singletonInstance = new OggLoader();
        return (singletonInstance);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OggSoundContainer loadSound(InputStream in) throws IOException {
        if (!(in instanceof BufferedInputStream)) in = new BufferedInputStream(in);
        OggSoundContainer osc = new OggSoundContainer((BufferedInputStream) in);
        return (osc);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OggSoundContainer loadSound(URL url) throws IOException {
        return (loadSound(url.openStream()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OggSoundContainer loadSound(String filename) throws IOException {
        return (loadSound(new BufferedInputStream(new FileInputStream(filename))));
    }
}
