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
public class AppletVorbisSPIWorkaround {

    public static boolean DEBUG = false;

    public static String useragent = null;

    public static AudioInputStream getAudioInputStream(AudioFormat targetFormat, AudioInputStream sourceStream) {
        try {
            return AudioSystem.getAudioInputStream(targetFormat, sourceStream);
        } catch (IllegalArgumentException iae) {
            if (DEBUG == true) {
                System.err.println("Using AppletVorbisSPIWorkaround to get codec");
            }
            try {
                Class.forName("org.tritonus.sampled.convert.jorbis.JorbisFormatConversionProvider");
                return new org.tritonus.sampled.convert.jorbis.JorbisFormatConversionProvider().getAudioInputStream(targetFormat, sourceStream);
            } catch (ClassNotFoundException cnfe) {
                throw new IllegalArgumentException("Vorbis codec not properly installed");
            }
        }
    }

    public static AudioFileFormat getAudioFileFormat(File file) throws UnsupportedAudioFileException, IOException {
        InputStream inputStream = new BufferedInputStream(new FileInputStream(file));
        try {
            if (DEBUG == true) {
                System.err.println("Using AppletVorbisSPIWorkaround to get codec (AudioFileFormat:file)");
            }
            return getAudioFileFormat(inputStream);
        } finally {
            inputStream.close();
        }
    }

    public static AudioInputStream getAudioInputStream(File file) throws UnsupportedAudioFileException, IOException {
        InputStream inputStream = new BufferedInputStream(new FileInputStream(file));
        try {
            if (DEBUG == true) {
                System.err.println("Using AppletVorbisSPIWorkaround to get codec (AudioInputStream:file)");
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
                System.err.println("Using AppletVorbisSPIWorkaround to get codec (AudioInputStream:url)");
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
                System.err.println("Using AppletVorbisSPIWorkaround to get codec AudioFileFormat(url)");
            }
            return getAudioFileFormat(inputStream);
        } finally {
            inputStream.close();
        }
    }

    public static AudioFileFormat getAudioFileFormat(InputStream is) throws UnsupportedAudioFileException, IOException {
        try {
            throw new Exception();
        } catch (Exception iae) {
            if (DEBUG == true) {
                System.err.println("Using AppletVorbisSPIWorkaround to get codec");
            }
            try {
                is.mark(4096);
                Class.forName("org.tritonus.sampled.file.jorbis.JorbisAudioFileReader");
                return new org.tritonus.sampled.file.jorbis.JorbisAudioFileReader().getAudioFileFormat(is);
            } catch (ClassNotFoundException cnfe) {
                throw new IllegalArgumentException("Vorbis codec not properly installed");
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
                System.err.println("Using AppleVorbisSPIWorkaround to get codec");
            }
            try {
                Class.forName("org.tritonus.sampled.file.jorbis.JorbisAudioFileReader");
                return new org.tritonus.sampled.file.jorbis.JorbisAudioFileReader().getAudioInputStream(is);
            } catch (ClassNotFoundException cnfe) {
                throw new IllegalArgumentException("Vorbis codec not properly installed:" + cnfe.getMessage());
            }
        }
    }
}
