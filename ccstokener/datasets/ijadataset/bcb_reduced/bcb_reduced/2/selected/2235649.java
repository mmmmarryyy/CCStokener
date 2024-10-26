package gnu.javax.sound.sampled.AU;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.spi.AudioFileReader;
import java.io.File;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.net.URL;
import java.nio.ByteBuffer;

public class AUReader extends AudioFileReader {

    private static class AUHeader {

        private static final int MAGIC = 0x2e736e64;

        public static final int SIZE = 24;

        public static final int ULAW = 1;

        public static final int PCM8 = 2;

        public static final int PCM16 = 3;

        public static final int PCM24 = 4;

        public static final int PCM32 = 5;

        public static final int IEEE32 = 6;

        public static final int IEEE64 = 7;

        public static final int G721 = 23;

        public static final int G722 = 24;

        public static final int G723 = 25;

        public static final int G723_5BIT = 26;

        public static final int ALAW = 27;

        public int headerSize;

        public int fileSize;

        public int encoding;

        public int sampleRate;

        public int channels;

        public int sampleSizeInBits;

        public AUHeader(InputStream stream) throws IOException, UnsupportedAudioFileException {
            byte[] hdr = new byte[24];
            stream.read(hdr);
            ByteBuffer buf = ByteBuffer.wrap(hdr);
            if (buf.getInt() != MAGIC) throw new UnsupportedAudioFileException("Not an AU format audio file.");
            headerSize = buf.getInt();
            fileSize = buf.getInt();
            encoding = buf.getInt();
            sampleRate = buf.getInt();
            channels = buf.getInt();
            switch(encoding) {
                case ULAW:
                case PCM8:
                case ALAW:
                    sampleSizeInBits = 8;
                    break;
                case PCM16:
                    sampleSizeInBits = 16;
                    break;
                case PCM24:
                    sampleSizeInBits = 24;
                    break;
                case PCM32:
                    sampleSizeInBits = 32;
                    break;
                default:
                    throw new UnsupportedAudioFileException("Unsupported encoding.");
            }
        }

        public AudioFormat getAudioFormat() {
            AudioFormat.Encoding encType = AudioFormat.Encoding.PCM_SIGNED;
            if (encoding == 1) encType = AudioFormat.Encoding.ULAW;
            if (encoding == 27) encType = AudioFormat.Encoding.ALAW;
            return new AudioFormat(encType, (float) sampleRate, sampleSizeInBits, channels, (sampleSizeInBits >> 3) * channels, (float) sampleRate, true);
        }

        public AudioFileFormat getAudioFileFormat() {
            return new AudioFileFormat(new AUFormatType(), getAudioFormat(), AudioSystem.NOT_SPECIFIED);
        }
    }

    public static class AUFormatType extends AudioFileFormat.Type {

        public AUFormatType() {
            super("AU", ".au");
        }
    }

    public AudioFileFormat getAudioFileFormat(File file) throws IOException, UnsupportedAudioFileException {
        return getAudioFileFormat(new FileInputStream(file));
    }

    public AudioFileFormat getAudioFileFormat(InputStream stream) throws IOException, UnsupportedAudioFileException {
        if (!stream.markSupported()) throw new IOException("Stream must support marking.");
        stream.mark(25);
        AUHeader header = new AUHeader(stream);
        stream.reset();
        return header.getAudioFileFormat();
    }

    public AudioFileFormat getAudioFileFormat(URL url) throws IOException, UnsupportedAudioFileException {
        return getAudioFileFormat(new BufferedInputStream(url.openStream()));
    }

    public AudioInputStream getAudioInputStream(File file) throws IOException, UnsupportedAudioFileException {
        InputStream stream = new FileInputStream(file);
        long length = file.length();
        AUHeader header = new AUHeader(stream);
        if (header.headerSize > AUHeader.SIZE) stream.skip(header.headerSize - AUHeader.SIZE);
        length -= header.headerSize;
        return new AudioInputStream(stream, header.getAudioFormat(), length);
    }

    public AudioInputStream getAudioInputStream(InputStream stream) throws IOException, UnsupportedAudioFileException {
        AUHeader header = new AUHeader(stream);
        if (header.headerSize > AUHeader.SIZE) stream.skip(header.headerSize - AUHeader.SIZE);
        return new AudioInputStream(stream, header.getAudioFormat(), AudioSystem.NOT_SPECIFIED);
    }

    public AudioInputStream getAudioInputStream(URL url) throws IOException, UnsupportedAudioFileException {
        return getAudioInputStream(new BufferedInputStream(url.openStream()));
    }
}
