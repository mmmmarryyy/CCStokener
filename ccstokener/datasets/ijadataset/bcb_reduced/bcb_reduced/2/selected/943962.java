package org.tritonus.applet;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * From JRE/JDK 1.3.0_01 on, applets can not use provided service
 * providers. Obviously, in these later releases of the Java 2 platform
 * the service providers are only searched on the system/boot classloader
 * and NOT on the classloader of the applet.
 * Workaround found by Tritonus Team.
 */
public class AppletMpegSPIWorkaround {

    public static boolean DEBUG = false;

    public static String useragent = null;

    public static AudioInputStream getAudioInputStream(AudioFormat targetFormat, AudioInputStream sourceStream) {
        try {
            return AudioSystem.getAudioInputStream(targetFormat, sourceStream);
        } catch (IllegalArgumentException iae) {
            if (DEBUG == true) {
                System.err.println("Using AppletMpegSPIWorkaround to get codec");
            }
            try {
                Class.forName("javazoom.spi.mpeg.sampled.convert.MpegFormatConversionProvider");
                return new javazoom.spi.mpeg.sampled.convert.MpegFormatConversionProvider().getAudioInputStream(targetFormat, sourceStream);
            } catch (ClassNotFoundException cnfe) {
                throw new IllegalArgumentException("Mpeg codec not properly installed");
            }
        }
    }

    public static AudioInputStream getAudioInputStream(File file) throws UnsupportedAudioFileException, IOException {
        InputStream inputStream = new BufferedInputStream(new FileInputStream(file));
        try {
            if (DEBUG == true) {
                System.err.println("Using AppletMpegSPIWorkaround to get codec (AudioInputStream:file)");
            }
            return getAudioInputStream(inputStream);
        } catch (UnsupportedAudioFileException e) {
            inputStream.close();
            throw e;
        } catch (IOException e) {
            inputStream.close();
            throw e;
        }
    }

    public static AudioInputStream getAudioInputStream(URL url) throws UnsupportedAudioFileException, IOException {
        try {
            Class.forName("javazoom.spi.mpeg.sampled.file.MpegAudioFileReader");
            return new MpegAudioFileReaderWorkaround().getAudioInputStream(url, useragent);
        } catch (ClassNotFoundException cnfe) {
            throw new IllegalArgumentException("Mpeg codec not properly installed");
        }
    }

    public static AudioFileFormat getAudioFileFormat(File file) throws UnsupportedAudioFileException, IOException {
        InputStream inputStream = new BufferedInputStream(new FileInputStream(file));
        try {
            if (DEBUG == true) {
                System.err.println("Using AppletMpegSPIWorkaround to get codec (AudioFileFormat:file)");
            }
            return getAudioFileFormat(inputStream);
        } finally {
            inputStream.close();
        }
    }

    public static AudioFileFormat getAudioFileFormat(URL url) throws UnsupportedAudioFileException, IOException {
        InputStream inputStream = null;
        if (useragent != null) {
            URLConnection myCon = url.openConnection();
            myCon.setUseCaches(false);
            myCon.setDoInput(true);
            myCon.setDoOutput(true);
            myCon.setAllowUserInteraction(false);
            myCon.setRequestProperty("User-Agent", useragent);
            myCon.setRequestProperty("Accept", "*/*");
            myCon.setRequestProperty("Icy-Metadata", "1");
            myCon.setRequestProperty("Connection", "close");
            inputStream = new BufferedInputStream(myCon.getInputStream());
        } else {
            inputStream = new BufferedInputStream(url.openStream());
        }
        try {
            if (DEBUG == true) {
                System.err.println("Using AppletMpegSPIWorkaround to get codec (AudioFileFormat:url)");
            }
            return getAudioFileFormatForUrl(inputStream);
        } finally {
            inputStream.close();
        }
    }

    public static AudioFileFormat getAudioFileFormatForUrl(InputStream is) throws UnsupportedAudioFileException, IOException {
        try {
            throw new Exception();
        } catch (Exception iae) {
            if (DEBUG == true) {
                System.err.println("Using AppletMpegSPIWorkaround to get codec (AudioFileFormat)");
            }
            try {
                Class.forName("javazoom.spi.mpeg.sampled.file.MpegAudioFileReader");
                return new javazoom.spi.mpeg.sampled.file.MpegAudioFileReader().getAudioFileFormat(is, AudioSystem.NOT_SPECIFIED);
            } catch (ClassNotFoundException cnfe) {
                throw new IllegalArgumentException("Mpeg codec not properly installed");
            }
        } finally {
        }
    }

    public static AudioFileFormat getAudioFileFormat(InputStream is) throws UnsupportedAudioFileException, IOException {
        try {
            throw new Exception();
        } catch (Exception iae) {
            if (DEBUG == true) {
                System.err.println("Using AppletMpegSPIWorkaround to get codec (AudioFileFormat)");
            }
            try {
                Class.forName("javazoom.spi.mpeg.sampled.file.MpegAudioFileReader");
                is.mark(4096);
                return new javazoom.spi.mpeg.sampled.file.MpegAudioFileReader().getAudioFileFormat(is);
            } catch (ClassNotFoundException cnfe) {
                throw new IllegalArgumentException("Mpeg codec not properly installed");
            }
        } finally {
            is.reset();
        }
    }

    public static AudioInputStream getAudioInputStream(InputStream is) throws UnsupportedAudioFileException, IOException {
        try {
            throw new Exception();
        } catch (Exception iae) {
            if (DEBUG == true) {
                System.err.println("Using AppletMpegSPIWorkaround to get codec");
            }
            try {
                Class.forName("javazoom.spi.mpeg.sampled.file.MpegAudioFileReader");
                return new javazoom.spi.mpeg.sampled.file.MpegAudioFileReader().getAudioInputStream(is);
            } catch (ClassNotFoundException cnfe) {
                throw new IllegalArgumentException("Mpeg codec not properly installed");
            }
        }
    }
}
