package org.jjsip.tools;

import java.awt.Image;
import java.awt.Toolkit;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.swing.ImageIcon;

/** Collection of static methods for handling files and jar archives. */
public class Archive {

    /** The base path */
    public static String BASE_PATH = (new File("")).getAbsolutePath();

    /** Gets the complete url of a file included within a jar archive. */
    public static URL getJarURL(String jar_archive, String file_name) {
        if (jar_archive == null || file_name == null) return null;
        String url = "jar:file:" + BASE_PATH + "/" + jar_archive + "!/" + file_name;
        try {
            return new URL(url);
        } catch (java.net.MalformedURLException e) {
            System.err.println("ERROR: malformed url " + url);
            return null;
        }
    }

    /** Gets the complete url of a file. */
    public static URL getFileURL(String file_name) {
        if (file_name == null) return null;
        String url = "file:" + BASE_PATH + "/" + file_name;
        try {
            return new URL("file:" + BASE_PATH + "/" + file_name);
        } catch (java.net.MalformedURLException e) {
            System.err.println("ERROR: malformed url " + url);
            return null;
        }
    }

    /** Gets an Image from file */
    public static Image getImage(String file_name) {
        if (file_name == null) return null;
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Image image = null;
        file_name = BASE_PATH + "/" + file_name;
        if ((new File(file_name)).canRead()) {
            image = toolkit.getImage(file_name);
            for (int i = 0; i < 4 && image.getWidth(null) < 0; i++) try {
                Thread.sleep(80);
            } catch (Exception e) {
            }
        }
        return image;
    }

    /** Gets an Image from a URL. */
    public static Image getImage(URL url) {
        if (url == null) return null;
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Image image = null;
        try {
            url.openConnection().connect();
            image = toolkit.getImage(url);
            for (int i = 0; i < 4 && image.getWidth(null) < 0; i++) try {
                Thread.sleep(80);
            } catch (Exception e) {
            }
        } catch (java.io.IOException e) {
            System.err.println("ERROR: can't read the file " + url.toString());
        }
        return image;
    }

    /** Gets an ImageIcon from file */
    public static ImageIcon getImageIcon(String file_name) {
        file_name = BASE_PATH + "/" + file_name;
        return new ImageIcon(file_name);
    }

    /** Gets an ImageIcon from an URL */
    public static ImageIcon getImageIcon(URL url) {
        if (url == null) return null;
        ImageIcon icon = null;
        try {
            url.openConnection().connect();
            icon = new ImageIcon(url);
        } catch (java.io.IOException e) {
            System.err.println("ERROR: can't read the file " + url.toString());
        }
        return icon;
    }

    /** Gets an InputStream from an URL */
    public static InputStream getInputStream(URL url) {
        if (url == null) return null;
        InputStream in = null;
        try {
            in = (url).openStream();
        } catch (java.io.IOException e) {
            System.err.println("ERROR: can't read the file " + url.toString());
        }
        return in;
    }

    /** Gets an AudioInputStream from an URL */
    public static AudioInputStream getAudioInputStream(URL url) throws javax.sound.sampled.UnsupportedAudioFileException {
        if (url == null) return null;
        AudioInputStream in = null;
        try {
            in = AudioSystem.getAudioInputStream(url);
        } catch (java.io.IOException e) {
            System.err.println("ERROR: can't read the file " + url.toString());
        }
        return in;
    }
}
