package com.sun.media.sound;

import java.util.Vector;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.EOFException;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.SequenceInputStream;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * AIFF file reader and writer.
 *
 * @author Kara Kytle
 * @author Jan Borgersen
 * @author Florian Bomers
 */
public class AiffFileReader extends SunFileReader {

    private static final int MAX_READ_LENGTH = 8;

    /**
     * AIFF parser type
     */
    public static final AudioFileFormat.Type types[] = { AudioFileFormat.Type.AIFF };

    /**
     * Constructs a new AiffParser object.
     */
    public AiffFileReader() {
    }

    /**
     * Obtains the audio file format of the input stream provided.  The stream must
     * point to valid audio file data.  In general, audio file providers may
     * need to read some data from the stream before determining whether they
     * support it.  These parsers must
     * be able to mark the stream, read enough data to determine whether they
     * support the stream, and, if not, reset the stream's read pointer to its original
     * position.  If the input stream does not support this, this method may fail
     * with an IOException.
     * @param stream the input stream from which file format information should be
     * extracted
     * @return an <code>AudioFileFormat</code> object describing the audio file format
     * @throws UnsupportedAudioFileException if the stream does not point to valid audio
     * file data recognized by the system
     * @throws IOException if an I/O exception occurs
     * @see InputStream#markSupported
     * @see InputStream#mark
     */
    public AudioFileFormat getAudioFileFormat(InputStream stream) throws UnsupportedAudioFileException, IOException {
        AudioFileFormat aff = getCOMM(stream, true);
        stream.reset();
        return aff;
    }

    /**
     * Obtains the audio file format of the URL provided.  The URL must
     * point to valid audio file data.
     * @param url the URL from which file format information should be
     * extracted
     * @return an <code>AudioFileFormat</code> object describing the audio file format
     * @throws UnsupportedAudioFileException if the URL does not point to valid audio
     * file data recognized by the system
     * @throws IOException if an I/O exception occurs
     */
    public AudioFileFormat getAudioFileFormat(URL url) throws UnsupportedAudioFileException, IOException {
        AudioFileFormat fileFormat = null;
        InputStream urlStream = url.openStream();
        try {
            fileFormat = getCOMM(urlStream, false);
        } finally {
            urlStream.close();
        }
        return fileFormat;
    }

    /**
     * Obtains the audio file format of the File provided.  The File must
     * point to valid audio file data.
     * @param file the File from which file format information should be
     * extracted
     * @return an <code>AudioFileFormat</code> object describing the audio file format
     * @throws UnsupportedAudioFileException if the File does not point to valid audio
     * file data recognized by the system
     * @throws IOException if an I/O exception occurs
     */
    public AudioFileFormat getAudioFileFormat(File file) throws UnsupportedAudioFileException, IOException {
        AudioFileFormat fileFormat = null;
        FileInputStream fis = new FileInputStream(file);
        try {
            fileFormat = getCOMM(fis, false);
        } finally {
            fis.close();
        }
        return fileFormat;
    }

    /**
     * Obtains an audio stream from the input stream provided.  The stream must
     * point to valid audio file data.  In general, audio file providers may
     * need to read some data from the stream before determining whether they
     * support it.  These parsers must
     * be able to mark the stream, read enough data to determine whether they
     * support the stream, and, if not, reset the stream's read pointer to its original
     * position.  If the input stream does not support this, this method may fail
     * with an IOException.
     * @param stream the input stream from which the <code>AudioInputStream</code> should be
     * constructed
     * @return an <code>AudioInputStream</code> object based on the audio file data contained
     * in the input stream.
     * @throws UnsupportedAudioFileException if the stream does not point to valid audio
     * file data recognized by the system
     * @throws IOException if an I/O exception occurs
     * @see InputStream#markSupported
     * @see InputStream#mark
     */
    public AudioInputStream getAudioInputStream(InputStream stream) throws UnsupportedAudioFileException, IOException {
        AudioFileFormat fileFormat = getCOMM(stream, true);
        return new AudioInputStream(stream, fileFormat.getFormat(), fileFormat.getFrameLength());
    }

    /**
     * Obtains an audio stream from the URL provided.  The URL must
     * point to valid audio file data.
     * @param url the URL for which the <code>AudioInputStream</code> should be
     * constructed
     * @return an <code>AudioInputStream</code> object based on the audio file data pointed
     * to by the URL
     * @throws UnsupportedAudioFileException if the URL does not point to valid audio
     * file data recognized by the system
     * @throws IOException if an I/O exception occurs
     */
    public AudioInputStream getAudioInputStream(URL url) throws UnsupportedAudioFileException, IOException {
        InputStream urlStream = url.openStream();
        AudioFileFormat fileFormat = null;
        try {
            fileFormat = getCOMM(urlStream, false);
        } finally {
            if (fileFormat == null) {
                urlStream.close();
            }
        }
        return new AudioInputStream(urlStream, fileFormat.getFormat(), fileFormat.getFrameLength());
    }

    /**
     * Obtains an audio stream from the File provided.  The File must
     * point to valid audio file data.
     * @param file the File for which the <code>AudioInputStream</code> should be
     * constructed
     * @return an <code>AudioInputStream</code> object based on the audio file data pointed
     * to by the File
     * @throws UnsupportedAudioFileException if the File does not point to valid audio
     * file data recognized by the system
     * @throws IOException if an I/O exception occurs
     */
    public AudioInputStream getAudioInputStream(File file) throws UnsupportedAudioFileException, IOException {
        FileInputStream fis = new FileInputStream(file);
        AudioFileFormat fileFormat = null;
        try {
            fileFormat = getCOMM(fis, false);
        } finally {
            if (fileFormat == null) {
                fis.close();
            }
        }
        return new AudioInputStream(fis, fileFormat.getFormat(), fileFormat.getFrameLength());
    }

    private AudioFileFormat getCOMM(InputStream is, boolean doReset) throws UnsupportedAudioFileException, IOException {
        DataInputStream dis = new DataInputStream(is);
        if (doReset) {
            dis.mark(MAX_READ_LENGTH);
        }
        int fileRead = 0;
        int dataLength = 0;
        AudioFormat format = null;
        int magic = dis.readInt();
        if (magic != AiffFileFormat.AIFF_MAGIC) {
            if (doReset) {
                dis.reset();
            }
            throw new UnsupportedAudioFileException("not an AIFF file");
        }
        int length = dis.readInt();
        int iffType = dis.readInt();
        fileRead += 12;
        int totallength;
        if (length <= 0) {
            length = AudioSystem.NOT_SPECIFIED;
            totallength = AudioSystem.NOT_SPECIFIED;
        } else {
            totallength = length + 8;
        }
        boolean aifc = false;
        if (iffType == AiffFileFormat.AIFC_MAGIC) {
            aifc = true;
        }
        boolean ssndFound = false;
        while (!ssndFound) {
            int chunkName = dis.readInt();
            int chunkLen = dis.readInt();
            fileRead += 8;
            int chunkRead = 0;
            switch(chunkName) {
                case AiffFileFormat.FVER_MAGIC:
                    break;
                case AiffFileFormat.COMM_MAGIC:
                    if ((!aifc && chunkLen < 18) || (aifc && chunkLen < 22)) {
                        throw new UnsupportedAudioFileException("Invalid AIFF/COMM chunksize");
                    }
                    int channels = dis.readShort();
                    dis.readInt();
                    int sampleSizeInBits = dis.readShort();
                    float sampleRate = (float) read_ieee_extended(dis);
                    chunkRead += (2 + 4 + 2 + 10);
                    AudioFormat.Encoding encoding = AudioFormat.Encoding.PCM_SIGNED;
                    if (aifc) {
                        int enc = dis.readInt();
                        chunkRead += 4;
                        switch(enc) {
                            case AiffFileFormat.AIFC_PCM:
                                encoding = AudioFormat.Encoding.PCM_SIGNED;
                                break;
                            case AiffFileFormat.AIFC_ULAW:
                                encoding = AudioFormat.Encoding.ULAW;
                                sampleSizeInBits = 8;
                                break;
                            default:
                                throw new UnsupportedAudioFileException("Invalid AIFF encoding");
                        }
                    }
                    int frameSize = calculatePCMFrameSize(sampleSizeInBits, channels);
                    format = new AudioFormat(encoding, sampleRate, sampleSizeInBits, channels, frameSize, sampleRate, true);
                    break;
                case AiffFileFormat.SSND_MAGIC:
                    int dataOffset = dis.readInt();
                    int blocksize = dis.readInt();
                    chunkRead += 8;
                    if (chunkLen < length) {
                        dataLength = chunkLen - chunkRead;
                    } else {
                        dataLength = length - (fileRead + chunkRead);
                    }
                    ssndFound = true;
                    break;
            }
            fileRead += chunkRead;
            if (!ssndFound) {
                int toSkip = chunkLen - chunkRead;
                if (toSkip > 0) {
                    fileRead += dis.skipBytes(toSkip);
                }
            }
        }
        if (format == null) {
            throw new UnsupportedAudioFileException("missing COMM chunk");
        }
        AudioFileFormat.Type type = aifc ? AudioFileFormat.Type.AIFC : AudioFileFormat.Type.AIFF;
        return new AiffFileFormat(type, totallength, format, dataLength / format.getFrameSize());
    }

    /** write_ieee_extended(DataOutputStream dos, double f) throws IOException {
     * Extended precision IEEE floating-point conversion routine.
     * @argument DataOutputStream
     * @argument double
     * @return void
     * @exception IOException
     */
    private void write_ieee_extended(DataOutputStream dos, double f) throws IOException {
        int exponent = 16398;
        double highMantissa = f;
        while (highMantissa < 44000) {
            highMantissa *= 2;
            exponent--;
        }
        dos.writeShort(exponent);
        dos.writeInt(((int) highMantissa) << 16);
        dos.writeInt(0);
    }

    /**
     * read_ieee_extended
     * Extended precision IEEE floating-point conversion routine.
     * @argument DataInputStream
     * @return double
     * @exception IOException
     */
    private double read_ieee_extended(DataInputStream dis) throws IOException {
        double f = 0;
        int expon = 0;
        long hiMant = 0, loMant = 0;
        long t1, t2;
        double HUGE = ((double) 3.40282346638528860e+38);
        expon = dis.readUnsignedShort();
        t1 = (long) dis.readUnsignedShort();
        t2 = (long) dis.readUnsignedShort();
        hiMant = t1 << 16 | t2;
        t1 = (long) dis.readUnsignedShort();
        t2 = (long) dis.readUnsignedShort();
        loMant = t1 << 16 | t2;
        if (expon == 0 && hiMant == 0 && loMant == 0) {
            f = 0;
        } else {
            if (expon == 0x7FFF) f = HUGE; else {
                expon -= 16383;
                expon -= 31;
                f = (hiMant * Math.pow(2, expon));
                expon -= 32;
                f += (loMant * Math.pow(2, expon));
            }
        }
        return f;
    }
}
