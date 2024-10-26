package org.roussev.hiena.sound;

import java.net.*;
import java.io.*;
import javax.sound.sampled.*;

/**
 * From JRE/JDK 1.3.0_01 on, applets can not use provided service
 * providers. Obviously, in these later releases of the Java 2 platform
 * the service providers are only searched on the system/boot classloader
 * and NOT on the classloader of the applet.
 * Workaround found by Tritonus Team.
 */
public class AppletMpegSPIWorkaround {

    public static boolean DEBUG = false;

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
        InputStream inputStream = new BufferedInputStream(url.openStream());
        try {
            if (DEBUG == true) {
                System.err.println("Using AppletMpegSPIWorkaround to get codec (AudioInputStream:url)");
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
        InputStream inputStream = new BufferedInputStream(url.openStream());
        try {
            if (DEBUG == true) {
                System.err.println("Using AppletMpegSPIWorkaround to get codec (AudioFileFormat:url)");
            }
            return getAudioFileFormat(inputStream);
        } finally {
            inputStream.close();
        }
    }

    public static AudioFileFormat getAudioFileFormat(InputStream is) throws UnsupportedAudioFileException, IOException {
        try {
            return AudioSystem.getAudioFileFormat(is);
        } catch (Exception iae) {
            if (DEBUG == true) {
                System.err.println("Using AppletMpegSPIWorkaround to get codec (AudioFileFormat)");
            }
            try {
                Class.forName("javazoom.spi.mpeg.sampled.file.MpegAudioFileReader");
                return new javazoom.spi.mpeg.sampled.file.MpegAudioFileReader().getAudioFileFormat(is);
            } catch (ClassNotFoundException cnfe) {
                throw new IllegalArgumentException("Mpeg codec not properly installed");
            }
        }
    }

    public static AudioInputStream getAudioInputStream(InputStream is) throws UnsupportedAudioFileException, IOException {
        try {
            return AudioSystem.getAudioInputStream(is);
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
