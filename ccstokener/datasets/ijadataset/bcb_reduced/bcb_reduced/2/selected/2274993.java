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
 * WAVE file reader.
 *
 * @author Kara Kytle
 * @author Jan Borgersen
 * @author Florian Bomers
 */
public class WaveFileReader extends SunFileReader {

    private static final int MAX_READ_LENGTH = 12;

    /**
     * WAVE reader type
     */
    public static final AudioFileFormat.Type types[] = { AudioFileFormat.Type.WAVE };

    /**
     * Constructs a new WaveFileReader object.
     */
    public WaveFileReader() {
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
        AudioFileFormat aff = getFMT(stream, true);
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
        InputStream urlStream = url.openStream();
        AudioFileFormat fileFormat = null;
        try {
            fileFormat = getFMT(urlStream, false);
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
            fileFormat = getFMT(fis, false);
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
        AudioFileFormat fileFormat = getFMT(stream, true);
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
            fileFormat = getFMT(urlStream, false);
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
            fileFormat = getFMT(fis, false);
        } finally {
            if (fileFormat == null) {
                fis.close();
            }
        }
        return new AudioInputStream(fis, fileFormat.getFormat(), fileFormat.getFrameLength());
    }

    private AudioFileFormat getFMT(InputStream stream, boolean doReset) throws UnsupportedAudioFileException, IOException {
        int bytesRead;
        int nread = 0;
        int fmt;
        int length = 0;
        int wav_type = 0;
        short channels;
        long sampleRate;
        long avgBytesPerSec;
        short blockAlign;
        int sampleSizeInBits;
        AudioFormat.Encoding encoding = null;
        DataInputStream dis = new DataInputStream(stream);
        if (doReset) {
            dis.mark(MAX_READ_LENGTH);
        }
        int magic = dis.readInt();
        int fileLength = rllong(dis);
        int waveMagic = dis.readInt();
        int totallength;
        if (fileLength <= 0) {
            fileLength = AudioSystem.NOT_SPECIFIED;
            totallength = AudioSystem.NOT_SPECIFIED;
        } else {
            totallength = fileLength + 8;
        }
        if ((magic != WaveFileFormat.RIFF_MAGIC) || (waveMagic != WaveFileFormat.WAVE_MAGIC)) {
            if (doReset) {
                dis.reset();
            }
            throw new UnsupportedAudioFileException("not a WAVE file");
        }
        while (true) {
            try {
                fmt = dis.readInt();
                nread += 4;
                if (fmt == WaveFileFormat.FMT_MAGIC) {
                    break;
                } else {
                    length = rllong(dis);
                    nread += 4;
                    if (length % 2 > 0) length++;
                    nread += dis.skipBytes(length);
                }
            } catch (EOFException eof) {
                throw new UnsupportedAudioFileException("Not a valid WAV file");
            }
        }
        length = rllong(dis);
        nread += 4;
        int endLength = nread + length;
        wav_type = rlshort(dis);
        nread += 2;
        if (wav_type == WaveFileFormat.WAVE_FORMAT_PCM) encoding = AudioFormat.Encoding.PCM_SIGNED; else if (wav_type == WaveFileFormat.WAVE_FORMAT_ALAW) encoding = AudioFormat.Encoding.ALAW; else if (wav_type == WaveFileFormat.WAVE_FORMAT_MULAW) encoding = AudioFormat.Encoding.ULAW; else {
            throw new UnsupportedAudioFileException("Not a supported WAV file");
        }
        channels = rlshort(dis);
        nread += 2;
        sampleRate = rllong(dis);
        nread += 4;
        avgBytesPerSec = rllong(dis);
        nread += 4;
        blockAlign = rlshort(dis);
        nread += 2;
        sampleSizeInBits = (int) rlshort(dis);
        nread += 2;
        if ((sampleSizeInBits == 8) && encoding.equals(AudioFormat.Encoding.PCM_SIGNED)) encoding = AudioFormat.Encoding.PCM_UNSIGNED;
        if (length % 2 != 0) length += 1;
        if (endLength > nread) nread += dis.skipBytes(endLength - nread);
        nread = 0;
        while (true) {
            try {
                int datahdr = dis.readInt();
                nread += 4;
                if (datahdr == WaveFileFormat.DATA_MAGIC) {
                    break;
                } else {
                    int thisLength = rllong(dis);
                    nread += 4;
                    if (thisLength % 2 > 0) thisLength++;
                    nread += dis.skipBytes(thisLength);
                }
            } catch (EOFException eof) {
                throw new UnsupportedAudioFileException("Not a valid WAV file");
            }
        }
        int dataLength = rllong(dis);
        nread += 4;
        AudioFormat format = new AudioFormat(encoding, (float) sampleRate, sampleSizeInBits, channels, calculatePCMFrameSize(sampleSizeInBits, channels), (float) sampleRate, false);
        return new WaveFileFormat(AudioFileFormat.Type.WAVE, totallength, format, dataLength / format.getFrameSize());
    }
}
