package nl.weeaboo.ogg.vorbis;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.spi.AudioFileReader;
import nl.weeaboo.ogg.InputStreamView;
import nl.weeaboo.ogg.OggReader;
import nl.weeaboo.ogg.StreamUtil;

public class VorbisAudioFileReader extends AudioFileReader {

    public static final AudioFileFormat.Type VORBIS_TYPE = new AudioFileFormat.Type("Vorbis", "ogg");

    private static final int HEADER_READ_LEN = 64 << 10;

    public VorbisAudioFileReader() {
    }

    @Override
    public AudioFileFormat getAudioFileFormat(File file) throws UnsupportedAudioFileException, IOException {
        InputStream in = new FileInputStream(file);
        try {
            return getAudioFileFormat(in);
        } finally {
            if (in != null) in.close();
        }
    }

    @Override
    public AudioFileFormat getAudioFileFormat(URL url) throws UnsupportedAudioFileException, IOException {
        InputStream in = url.openStream();
        try {
            return getAudioFileFormat(in);
        } finally {
            if (in != null) in.close();
        }
    }

    @Override
    public AudioFileFormat getAudioFileFormat(InputStream in) throws UnsupportedAudioFileException, IOException {
        in = (in.markSupported() ? in : new BufferedInputStream(in));
        InputStreamView inv = new InputStreamView(in, HEADER_READ_LEN);
        inv.mark(HEADER_READ_LEN + 1);
        VorbisDecoder vorbisd = new VorbisDecoder();
        OggReader reader = new OggReader();
        try {
            reader.setInput(StreamUtil.getOggInput(inv));
            if (reader.addStreamHandler(vorbisd) == null) {
                throw new UnsupportedAudioFileException();
            }
            reader.readStreamHeaders();
        } finally {
            inv.reset();
        }
        if (!reader.hasReadHeaders()) {
            throw new UnsupportedAudioFileException();
        }
        AudioFormat format = vorbisd.getAudioFormat();
        return new AudioFileFormat(VORBIS_TYPE, format, AudioSystem.NOT_SPECIFIED);
    }

    @Override
    public AudioInputStream getAudioInputStream(File file) throws UnsupportedAudioFileException, IOException {
        InputStream in = new FileInputStream(file);
        try {
            return getAudioInputStream(in);
        } finally {
            if (in != null) in.close();
        }
    }

    @Override
    public AudioInputStream getAudioInputStream(URL url) throws UnsupportedAudioFileException, IOException {
        InputStream in = url.openStream();
        try {
            return getAudioInputStream(in);
        } finally {
            if (in != null) in.close();
        }
    }

    @Override
    public VorbisAudioInputStream getAudioInputStream(InputStream in) throws UnsupportedAudioFileException, IOException {
        in = (in.markSupported() ? in : new BufferedInputStream(in));
        AudioFileFormat aff = getAudioFileFormat(in);
        if (aff == null) {
            return null;
        }
        VorbisDecoder vorbisd = new VorbisDecoder();
        OggReader reader = new OggReader();
        reader.setInput(StreamUtil.getOggInput(in));
        reader.addStreamHandler(vorbisd);
        reader.readStreamHeaders();
        VorbisDecoderInputStream vin = new VorbisDecoderInputStream(reader, vorbisd);
        return new VorbisAudioInputStream(vin, aff.getFormat(), AudioSystem.NOT_SPECIFIED);
    }
}
