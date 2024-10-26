package com.qasystems.qstudio.java.classloader;

import java.net.*;
import java.io.*;
import com.qasystems.debug.DebugWriter;

public class ClassPathInfo {

    private final ClassLoader classLoader;

    public ClassPathInfo(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public ClassPathInfo() {
        this(ClassLoader.getSystemClassLoader());
    }

    /**
   * validates classpath, throws an LinkageError if invalid
   * classpath was specified
   */
    public void validateClassPath() {
        try {
            URL[] urls = ((URLClassLoader) classLoader).getURLs();
            for (int i = 0; i < urls.length; i++) {
                try {
                    urls[i].openStream();
                    new DebugWriter().writeMessage(urls[i].getFile() + "\n");
                } catch (IllegalArgumentException iae) {
                    throw new LinkageError("malformed class path url:\n " + urls[i]);
                } catch (IOException ioe) {
                    throw new LinkageError("invalid class path url:\n " + urls[i]);
                }
            }
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("The current VM's System classloader is not a " + "subclass of java.net.URLClassLoader");
        }
    }

    public static void main(String[] args) {
        ClassPathInfo info = new ClassPathInfo();
        info.validateClassPath();
    }
}
