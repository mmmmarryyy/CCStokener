package net.sourceforge.minigolf.provider;

import java.awt.Image;
import java.awt.Toolkit;
import java.net.URL;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.io.IOException;
import java.util.jar.JarFile;
import java.util.jar.Attributes;
import net.sourceforge.minigolf.exceptions.MinigolfException;

/**
 * This class is the engine to "unpack" und split course-JARs into their pieces.
 * That is, extract the CourseProvider and visual Images for rendering the
 * course out of them.
 */
public class CourseUnpacker {

    /** This is the URL to get the file from */
    private final URL url;

    /** This is the JARFile we are to unpack. */
    private JarFile jar;

    /**
  * Create a unpacker for the given JAR-source.
  * @param u	The URL to load from.
  */
    public CourseUnpacker(URL u) {
        url = u;
        jar = null;
        try {
            JarURLConnection con = (JarURLConnection) url.openConnection();
            jar = con.getJarFile();
        } catch (IOException e) {
            MinigolfException.throwToDisplay(e);
        }
    }

    /**
  * Get the meter-&get;pixel mapping factor desired.
  * @return The mapping-factor as specified in the Manifest.
  */
    public float getMeterPixelMapping() {
        return Float.parseFloat(getManifestEntry("Meters2Pixels"));
    }

    /**
  * Get the Ball-radius specified in the Manifest.
  * @return The Ball radius to use.
  */
    public double getBallRadius() {
        return Double.parseDouble(getManifestEntry("Ball-Radius"));
    }

    /**
  * Extract the basic image to display.
  * @return The course's basic image.
  */
    public Image getBaseImage() {
        URL imageUrl = null;
        try {
            imageUrl = new URL(url, getManifestEntry("Base-Image"));
        } catch (MalformedURLException e) {
            MinigolfException.throwToDisplay(e);
        }
        return Toolkit.getDefaultToolkit().getImage(imageUrl);
    }

    /**
  * Extract the CourseProvider.
  * @return The CourseProvider to get the course.
  * @throws MalformedCourseException if no provider class is found.
  */
    public CourseProvider getCourseProvider() throws MalformedCourseException {
        try {
            String cml = getManifestEntry("Course-CML");
            if (cml != null) return new net.sourceforge.minigolf.provider.cml.Parser(jar.getInputStream(jar.getEntry(cml)));
        } catch (IOException e) {
            e.printStackTrace();
        }
        String className = getManifestEntry("Course-Provider");
        Class c = null;
        try {
            c = Class.forName(className);
        } catch (ClassNotFoundException e) {
            try {
                JARClassLoader cl = new JARClassLoader(jar);
                c = cl.findClass(className);
            } catch (ClassNotFoundException ex) {
                throw new NoProviderClassFoundException(className);
            }
        }
        Object ret = null;
        try {
            ret = c.newInstance();
        } catch (InstantiationException e) {
            MinigolfException.throwToDisplay(e);
        } catch (IllegalAccessException e) {
            MinigolfException.throwToDisplay(e);
        }
        return (CourseProvider) ret;
    }

    /**
  * Get a Manifest-Entry-String-value by its String name.
  * @param key	The key to look for in the Manifest.
  * @return The associated value.
  */
    private String getManifestEntry(String key) {
        try {
            Attributes attr = jar.getManifest().getMainAttributes();
            return (String) attr.get(new Attributes.Name(key));
        } catch (IOException e) {
            MinigolfException.throwToDisplay(e);
        }
        return null;
    }
}
