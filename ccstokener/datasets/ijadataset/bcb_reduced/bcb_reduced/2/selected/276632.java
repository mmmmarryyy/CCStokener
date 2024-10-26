package org.lindenb.io;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 * This abstract class is used to get a unified way to open and re-open again a {@link java.io.InputStream }
 * from a souce (could be a File, a String, a resource or a remote URL)
 * @author lindenb
 *
 */
public abstract class InputSource {

    /** the a {@link InputSource} is a String */
    public static class STRING extends InputSource {

        private String s;

        public STRING(String s) {
            this.s = s;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(s.getBytes());
        }
    }

    /** the a {@link InputSource} is a File */
    public static class FILE extends InputSource {

        private File file;

        public FILE(File file) {
            this.file = file;
        }

        public FILE(String file) {
            this(new File(file));
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new FileInputStream(file);
        }
    }

    /** the a {@link InputSource} is an URL */
    public static class URL extends InputSource {

        private java.net.URL url;

        public URL(java.net.URL url) {
            this.url = url;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return this.url.openStream();
        }
    }

    /** the a {@link InputSource} is an Class.Resource */
    public static class RESOURCE extends InputSource {

        private Class<?> clazz;

        private String name;

        public RESOURCE(Class<?> clazz, String name) {
            this.clazz = clazz;
            this.name = name;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            InputStream in = this.clazz.getResourceAsStream(this.name);
            if (in == null) throw new IOException("Cannot read resource " + name + " from " + this.clazz);
            return in;
        }
    }

    public abstract InputStream getInputStream() throws IOException;

    /** re-open this source of data and return a java.io.Reader */
    public Reader getReader() throws IOException {
        return new InputStreamReader(getInputStream());
    }
}
