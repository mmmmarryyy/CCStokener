package de.anormalmedia.sbstutorial.store;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * The {@link AbstractStore} defines the location of the .tut file.
 * @author anormal
 */
public class AbstractStore {

    private URL url;

    private JarFile jar;

    /**
     * Sets the url to the .tut file. If the url is a jar file url, it will be extracted as a temp file.
     * @param jarurl the url to the .tut file
     */
    protected void setURL(URL jarurl) {
        this.url = jarurl;
        try {
            if (this.url.toString().toLowerCase().startsWith("jar:")) {
                File tempFile = File.createTempFile("SBSTUTORIAL", String.valueOf(System.currentTimeMillis()));
                tempFile.deleteOnExit();
                FileOutputStream fos = new FileOutputStream(tempFile);
                URLConnection uc = this.url.openConnection();
                InputStream is = uc.getInputStream();
                byte[] buf = new byte[1024];
                int i = 0;
                while ((i = is.read(buf)) != -1) {
                    fos.write(buf, 0, i);
                }
                is.close();
                fos.flush();
                fos.close();
                this.url = tempFile.toURI().toURL();
            }
            if (!url.toString().toLowerCase().startsWith("jar:")) {
                this.url = new URL("jar", "", -1, this.url.toString() + "!/");
            }
            URLConnection uc = this.url.openConnection();
            jar = ((JarURLConnection) uc).getJarFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the jar url to the .tut file
     * @return the jar url to the .tut file
     */
    public URL getUrl() {
        return url;
    }

    /**
     * Returns the stream to a resource with the given name. The name should be absolute in the jar file
     * @param resource the resource to look for
     * @return the stream to the resource or null if not found
     */
    public InputStream getResource(String resource) {
        if (resource.startsWith("/")) {
            resource = resource.substring(1);
        }
        if (jar != null) {
            JarEntry entry = jar.getJarEntry(resource);
            if (entry != null) {
                try {
                    return jar.getInputStream(entry);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
}
