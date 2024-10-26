package uk.org.skeet.jbench;

import java.io.*;
import java.util.*;
import java.net.*;

/**
 * Generic resource for reading. Resources are originally created using
 * getResource, which uses the classloader if the resource can be
 * found that way or a File otherwise. A resource can be created
 * relative to another one, in which case only the original type
 * of resource is used (eg creating a resource relative to a file
 * will always create another file resource).
 */
public abstract class Resource {

    public abstract InputStream getStream() throws IOException;

    public abstract Resource getRelativeResource(String name) throws ConfigurationException;

    public static Resource getResource(String name) {
        URL u = Resource.class.getClassLoader().getResource(name);
        if (u != null) return new URLResource(u); else return new FileResource(new File(name));
    }

    private static class URLResource extends Resource {

        private URL url;

        URLResource(URL url) {
            this.url = url;
        }

        public InputStream getStream() throws IOException {
            return url.openStream();
        }

        public Resource getRelativeResource(String name) throws ConfigurationException {
            try {
                return new URLResource(new URL(url, name));
            } catch (MalformedURLException e) {
                throw new ConfigurationException("Invalid relative name " + name);
            }
        }

        public String toString() {
            return url.toString();
        }
    }

    private static class FileResource extends Resource {

        private File file;

        FileResource(File file) {
            this.file = file;
        }

        public InputStream getStream() throws IOException {
            return new FileInputStream(file);
        }

        public Resource getRelativeResource(String name) throws ConfigurationException {
            return new FileResource(new File(file, name));
        }

        public String toString() {
            return file.toString();
        }
    }
}
