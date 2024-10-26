package net.assimilator.resources.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;

/**
 * Utility for getting the Attributes from a Jar File
 * <br>
 * The attributes are:<br><br>
 * "Assimilator-Build:", for example:<br><br>
 * Manifest-Version: 1.0<br>
 * Assimilator-Build: 20061225<br>
 * <br>
 *
 * @author Kevin Hartig
 * @version $Id: $
 */
public class Manifest {

    public static Attributes.Name ASSIMILATOR_BUILD = new Attributes.Name("Assimilator-Build");

    private JarInputStream jarIn = null;

    /**
     * Holds value of property manifest.
     */
    private java.util.jar.Manifest manifest;

    public Manifest(URL url) throws IOException {
        if (!url.getProtocol().equals("jar")) {
            url = new URL("jar:" + url.toExternalForm() + "!/");
        }
        JarURLConnection uc = (JarURLConnection) url.openConnection();
        setManifest(uc.getManifest());
    }

    public Manifest(InputStream in) throws IOException {
        jarIn = new JarInputStream(in);
        setManifest(jarIn.getManifest());
    }

    public Manifest(java.util.jar.Manifest manifest) {
        setManifest(manifest);
    }

    public void close() throws IOException {
        if (jarIn != null) jarIn.close();
    }

    /**
     * Get the build from a Jar file
     *
     * @return the Build from a Jar file, or null if not defined.
     */
    public String getBuild() throws IOException {
        return getMainAttribute(ASSIMILATOR_BUILD);
    }

    /**
     * Get a Main Attribute from a Jar file
     *
     * @param name the name of the main attribute entry
     * @return the value of the main attribute from a Jar file, or null if not defined.
     */
    public String getMainAttribute(String name) throws IOException {
        return getMainAttribute(new Attributes.Name(name));
    }

    /**
     * Get a Main Attribute from a Jar file
     *
     * @param name the name of the main attribute entry
     * @return the value of the main attribute from a Jar file, or null if not defined.
     */
    public String getMainAttribute(Attributes.Name name) throws IOException {
        if (manifest == null) throw new NullPointerException("there is no manifest");
        Attributes attributes = manifest.getMainAttributes();
        if (attributes == null) return null;
        return (String) attributes.get(name);
    }

    /**
     * Get an Entry from a Jar file
     *
     * @param name the name of the entry
     * @return the attributes for the entry or null if not defined
     */
    public Attributes getEntry(String name) throws IOException {
        if (manifest == null) throw new NullPointerException("there is no manifest");
        return manifest.getAttributes(name);
    }

    public Attributes getMainAttributes() throws IOException {
        if (manifest == null) throw new NullPointerException("there is no manifest");
        return (manifest.getMainAttributes());
    }

    /**
     * Getter for property manifest.
     *
     * @return Value of property manifest.
     */
    public java.util.jar.Manifest getManifest() {
        return manifest;
    }

    /**
     * Setter for property manifest.
     *
     * @param manifest New value of property manifest.
     */
    public void setManifest(java.util.jar.Manifest manifest) {
        this.manifest = manifest;
    }
}
