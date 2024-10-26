package sound.spi;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.net.URL;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.spi.AudioFileReader;
import jflac.Constants;
import jflac.FLACDecoder;
import io.BitInputStream;
import io.BitOutputStream;
import metadata.StreamInfo;

/**
 * Provider for Flac audio file reading services. This implementation can parse
 * the format information from Flac audio file, and can produce audio input
 * streams from files of this type.
 * 
 * @author Marc Gimpel, Wimba S.A. (marc@wimba.com)
 * @version $Revision: 1.8 $
 */
public class FlacAudioFileReader extends AudioFileReader {

    private static final boolean DEBUG = false;

    private FLACDecoder decoder;

    private StreamInfo streamInfo;

    /**
     * Obtains the audio file format of the File provided. The File must point
     * to valid audio file data.
     * 
     * @param file
     *            the File from which file format information should be
     *            extracted.
     * @return an AudioFileFormat object describing the audio file format.
     * @exception UnsupportedAudioFileException
     *                if the File does not point to a valid audio file data
     *                recognized by the system.
     * @exception IOException
     *                if an I/O exception occurs.
     */
    public AudioFileFormat getAudioFileFormat(File file) throws UnsupportedAudioFileException, IOException {
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
            return getAudioFileFormat(inputStream, (int) file.length());
        } finally {
            inputStream.close();
        }
    }

    /**
     * Obtains an audio input stream from the URL provided. The URL must point
     * to valid audio file data.
     * 
     * @param url
     *            the URL for which the AudioInputStream should be constructed.
     * @return an AudioInputStream object based on the audio file data pointed
     *         to by the URL.
     * @exception UnsupportedAudioFileException
     *                if the File does not point to a valid audio file data
     *                recognized by the system.
     * @exception IOException
     *                if an I/O exception occurs.
     */
    public AudioFileFormat getAudioFileFormat(URL url) throws UnsupportedAudioFileException, IOException {
        InputStream inputStream = url.openStream();
        try {
            return getAudioFileFormat(inputStream);
        } finally {
            inputStream.close();
        }
    }

    /**
     * Obtains an audio input stream from the input stream provided.
     * 
     * @param stream
     *            the input stream from which the AudioInputStream should be
     *            constructed.
     * @return an AudioInputStream object based on the audio file data contained
     *         in the input stream.
     * @exception UnsupportedAudioFileException
     *                if the File does not point to a valid audio file data
     *                recognized by the system.
     * @exception IOException
     *                if an I/O exception occurs.
     */
    public AudioFileFormat getAudioFileFormat(InputStream stream) throws UnsupportedAudioFileException, IOException {
        return getAudioFileFormat(stream, AudioSystem.NOT_SPECIFIED);
    }

    /**
     * Return the AudioFileFormat from the given InputStream. Implementation.
     * 
     * @param bitStream
     * @param baos
     * @param mediaLength
     * @return an AudioInputStream object based on the audio file data contained
     *         in the input stream.
     * @exception UnsupportedAudioFileException
     *                if the File does not point to a valid audio file data
     *                recognized by the system.
     * @exception IOException
     *                if an I/O exception occurs.
     */
    protected AudioFileFormat getAudioFileFormat(InputStream bitStream, int mediaLength) throws UnsupportedAudioFileException, IOException {
        AudioFormat format;
        try {
            decoder = new FLACDecoder(bitStream);
            streamInfo = decoder.readStreamInfo();
            if (streamInfo == null) {
                if (DEBUG) {
                    System.out.println("FLAC file reader: no stream info found");
                }
                throw new UnsupportedAudioFileException("No StreamInfo found");
            }
            format = new FlacAudioFormat(streamInfo);
        } catch (IOException ioe) {
            if (DEBUG) {
                System.out.println("FLAC file reader: not a FLAC stream");
            }
            throw new UnsupportedAudioFileException(ioe.getMessage());
        }
        if (DEBUG) {
            System.out.println("FLAC file reader: got stream with format " + format);
        }
        return new AudioFileFormat(FlacFileFormatType.FLAC, format, AudioSystem.NOT_SPECIFIED);
    }

    /**
     * Obtains an audio input stream from the File provided. The File must point
     * to valid audio file data.
     * 
     * @param file
     *            the File for which the AudioInputStream should be constructed.
     * @return an AudioInputStream object based on the audio file data pointed
     *         to by the File.
     * @exception UnsupportedAudioFileException
     *                if the File does not point to a valid audio file data
     *                recognized by the system.
     * @exception IOException
     *                if an I/O exception occurs.
     */
    public AudioInputStream getAudioInputStream(File file) throws UnsupportedAudioFileException, IOException {
        InputStream inputStream = new FileInputStream(file);
        try {
            return getAudioInputStream(inputStream, (int) file.length());
        } catch (UnsupportedAudioFileException e) {
            inputStream.close();
            throw e;
        } catch (IOException e) {
            inputStream.close();
            throw e;
        }
    }

    /**
     * Obtains an audio input stream from the URL provided. The URL must point
     * to valid audio file data.
     * 
     * @param url
     *            the URL for which the AudioInputStream should be constructed.
     * @return an AudioInputStream object based on the audio file data pointed
     *         to by the URL.
     * @exception UnsupportedAudioFileException
     *                if the File does not point to a valid audio file data
     *                recognized by the system.
     * @exception IOException
     *                if an I/O exception occurs.
     */
    public AudioInputStream getAudioInputStream(URL url) throws UnsupportedAudioFileException, IOException {
        InputStream inputStream = url.openStream();
        try {
            return getAudioInputStream(inputStream);
        } catch (UnsupportedAudioFileException e) {
            inputStream.close();
            throw e;
        } catch (IOException e) {
            inputStream.close();
            throw e;
        }
    }

    /**
     * Obtains an audio input stream from the input stream provided. The stream
     * must point to valid audio file data.
     * 
     * @param stream
     *            the input stream from which the AudioInputStream should be
     *            constructed.
     * @return an AudioInputStream object based on the audio file data contained
     *         in the input stream.
     * @exception UnsupportedAudioFileException
     *                if the File does not point to a valid audio file data
     *                recognized by the system.
     * @exception IOException
     *                if an I/O exception occurs.
     */
    public AudioInputStream getAudioInputStream(InputStream stream) throws UnsupportedAudioFileException, IOException {
        return getAudioInputStream(stream, AudioSystem.NOT_SPECIFIED);
    }

    /**
     * Obtains an audio input stream from the input stream provided. The stream
     * must point to valid audio file data.
     * 
     * @param inputStream
     *            the input stream from which the AudioInputStream should be
     *            constructed.
     * @param medialength
     * @return an AudioInputStream object based on the audio file data contained
     *         in the input stream.
     * @exception UnsupportedAudioFileException
     *                if the File does not point to a valid audio file data
     *                recognized by the system.
     * @exception IOException
     *                if an I/O exception occurs.
     */
    protected AudioInputStream getAudioInputStream(InputStream inputStream, int medialength) throws UnsupportedAudioFileException, IOException {
        AudioFileFormat audioFileFormat = getAudioFileFormat(inputStream, medialength);
        ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
        BitOutputStream bitOutStream = new BitOutputStream(byteOutStream);
        bitOutStream.writeByteBlock(Constants.STREAM_SYNC_STRING, Constants.STREAM_SYNC_STRING.length);
        streamInfo.write(bitOutStream, false);
        BitInputStream bis = decoder.getBitInputStream();
        int bytesLeft = bis.getInputBytesUnconsumed();
        byte[] b = new byte[bytesLeft];
        bis.readByteBlockAlignedNoCRC(b, bytesLeft);
        byteOutStream.write(b);
        ByteArrayInputStream byteInStream = new ByteArrayInputStream(byteOutStream.toByteArray());
        SequenceInputStream sequenceInputStream = new SequenceInputStream(byteInStream, inputStream);
        return new AudioInputStream(sequenceInputStream, audioFileFormat.getFormat(), audioFileFormat.getFrameLength());
    }
}
