package gnu.javax.sound.sampled.WAV;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.net.URL;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.spi.AudioFileReader;

/**
 * A WAV file reader.
 * 
 * This code reads WAV files.
 *
 * There are many decent documents on the web describing the WAV file
 * format.  I didn't bother looking for the official document.  If it
 * exists, I'm not even sure if it is freely available.  We should
 * update this comment if we find out anything helpful here.  I used
 * http://www.sonicspot.com/guide/wavefiles.html
 *
 * @author Anthony Green (green@redhat.com)
 *
 */
public class WAVReader extends AudioFileReader {

    private static long readUnsignedIntLE(DataInputStream is) throws IOException {
        byte[] buf = new byte[4];
        is.readFully(buf);
        return (buf[0] & 0xFF | ((buf[1] & 0xFF) << 8) | ((buf[2] & 0xFF) << 16) | ((buf[3] & 0xFF) << 24));
    }

    private static short readUnsignedShortLE(DataInputStream is) throws IOException {
        byte[] buf = new byte[2];
        is.readFully(buf);
        return (short) (buf[0] & 0xFF | ((buf[1] & 0xFF) << 8));
    }

    public AudioFileFormat getAudioFileFormat(File file) throws UnsupportedAudioFileException, IOException {
        InputStream is = new FileInputStream(file);
        try {
            return getAudioFileFormat(is);
        } finally {
            is.close();
        }
    }

    public AudioFileFormat getAudioFileFormat(InputStream in) throws UnsupportedAudioFileException, IOException {
        DataInputStream din;
        if (in instanceof DataInputStream) din = (DataInputStream) in; else din = new DataInputStream(in);
        if (din.readInt() != 0x52494646) throw new UnsupportedAudioFileException("Invalid WAV chunk header.");
        readUnsignedIntLE(din);
        if (din.readInt() != 0x57415645) throw new UnsupportedAudioFileException("Invalid WAV chunk header.");
        boolean foundFmt = false;
        boolean foundData = false;
        short compressionCode = 0, numberChannels = 0, blockAlign = 0, bitsPerSample = 0;
        long sampleRate = 0, bytesPerSecond = 0;
        long chunkLength = 0;
        while (!foundData) {
            int chunkId = din.readInt();
            chunkLength = readUnsignedIntLE(din);
            switch(chunkId) {
                case 0x666D7420:
                    foundFmt = true;
                    compressionCode = readUnsignedShortLE(din);
                    numberChannels = readUnsignedShortLE(din);
                    sampleRate = readUnsignedIntLE(din);
                    bytesPerSecond = readUnsignedIntLE(din);
                    blockAlign = readUnsignedShortLE(din);
                    bitsPerSample = readUnsignedShortLE(din);
                    din.skip(chunkLength - 16);
                    break;
                case 0x66616374:
                    din.skip(chunkLength);
                    break;
                case 0x64617461:
                    if (!foundFmt) throw new UnsupportedAudioFileException("This implementation requires WAV fmt chunks precede data chunks.");
                    foundData = true;
                    break;
                default:
                    din.skip(chunkLength);
            }
        }
        AudioFormat.Encoding encoding;
        switch(compressionCode) {
            case 1:
                if (bitsPerSample <= 8) encoding = AudioFormat.Encoding.PCM_UNSIGNED; else encoding = AudioFormat.Encoding.PCM_SIGNED;
                break;
            default:
                throw new UnsupportedAudioFileException("Unrecognized WAV compression code: 0x" + Integer.toHexString(compressionCode));
        }
        return new AudioFileFormat(AudioFileFormat.Type.WAVE, new AudioFormat(encoding, (float) sampleRate, bitsPerSample, numberChannels, ((bitsPerSample + 7) / 8) * numberChannels, (float) bytesPerSecond, false), (int) chunkLength);
    }

    public AudioFileFormat getAudioFileFormat(URL url) throws UnsupportedAudioFileException, IOException {
        InputStream is = url.openStream();
        try {
            return getAudioFileFormat(is);
        } finally {
            is.close();
        }
    }

    public AudioInputStream getAudioInputStream(File file) throws UnsupportedAudioFileException, IOException {
        return getAudioInputStream(new FileInputStream(file));
    }

    public AudioInputStream getAudioInputStream(InputStream stream) throws UnsupportedAudioFileException, IOException {
        AudioFileFormat aff = getAudioFileFormat(stream);
        return new AudioInputStream(stream, aff.getFormat(), (long) aff.getFrameLength());
    }

    public AudioInputStream getAudioInputStream(URL url) throws UnsupportedAudioFileException, IOException {
        return getAudioInputStream(url.openStream());
    }
}
