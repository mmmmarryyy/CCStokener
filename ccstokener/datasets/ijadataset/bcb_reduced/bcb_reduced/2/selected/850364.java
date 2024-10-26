package com.framedobjects.dashwell.utils.webservice;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.internal.Library;
import wsl.fw.exception.MdnException;
import wsl.fw.util.Config;

/**
 * This class contains some static methods used from several classes.
 * Theses methods are "helper" methods and support the main classes.
 * 
 */
public class WebServiceFileHelper {

    /** 
   * Extracts a file from the Jar file.
   * 
   * @param jarname the name of the Jar file.
   * @param filename the name of the file.
   * @param override overrides the file even if it exists.
   * @return File the file from the Jar file.
   * @throws IOException
   * @throws MdnException if file was not found or could not extracted.
   */
    public static File getFile(String jarname, String filename, boolean override) throws MdnException {
        JarFile jarfile = null;
        ZipEntry entry = null;
        if (jarname == null || filename == null || jarname.equals("") || filename.equals("")) throw new MdnException(Config.getProp("error.FileNotFoundException"), new FileNotFoundException());
        File outputFile = new File(filename);
        if (outputFile.exists() == true && override == false) return outputFile;
        File jarFile = new File(jarname);
        if (!jarFile.exists()) throw new MdnException(Config.getProp("error.FileNotFoundException"), new FileNotFoundException(jarname));
        try {
            jarfile = new JarFile(jarFile);
            entry = jarfile.getEntry(filename);
            if (entry == null) throw new MdnException(Config.getProp("error.FileNotFoundException"), new ZipException(filename));
            outputFile = writeFile(jarfile.getInputStream(entry), filename);
        } catch (IOException e) {
            try {
                jarfile.close();
            } catch (IOException e2) {
            }
            throw new MdnException(Config.getProp("error.IOException"), e);
        }
        return outputFile;
    }

    /**
   * Writes a file from an input stream.
   * 
   * @param is the input stream to write.
   * @param filename name of the file.
   * @return the written file.
   * @throws MdnException
   */
    private static File writeFile(InputStream is, String filename) throws MdnException {
        OutputStream os = null;
        File outputFile = new File(filename);
        File parentFile = outputFile.getParentFile();
        if (parentFile != null) if (!parentFile.exists()) parentFile.mkdirs();
        try {
            os = new FileOutputStream(outputFile);
            byte[] buffer = new byte[4096];
            int cnt = is.read(buffer);
            while (cnt > 0) {
                os.write(buffer, 0, cnt);
                cnt = is.read(buffer);
            }
        } catch (IOException e) {
            throw new MdnException("error.IOException", e);
        } finally {
            try {
                if (os != null) os.close();
                if (is != null) is.close();
            } catch (IOException e) {
            }
        }
        return outputFile;
    }

    /**
   * Extracts a file from the Jar file.
   * 
   * @param filename the name of the file.
   * @param override overrides the file even if it exists.
   * @return the file from the Jar file.
   * @throws MdnException if file was not found or could not extracted.
   */
    public static File getFile(String filename, boolean override) throws MdnException {
        InputStream is = null;
        OutputStream os = null;
        if (filename == null) throw new MdnException("error.FileNotFoundException", new FileNotFoundException());
        File outputFile = new File(filename);
        if (outputFile.exists() && override == false) return outputFile;
        URL url = Thread.currentThread().getContextClassLoader().getResource(filename);
        if (url == null) throw new MdnException("error.FileNotFoundException", new FileNotFoundException(filename));
        try {
            outputFile = writeFile(url.openStream(), filename);
        } catch (IOException e) {
            throw new MdnException("error.IOException", e);
        }
        return outputFile;
    }

    /**
   * SWT needs OS dependent libraries to create native widgets (windows, buttons
   * etc.). This method extracts these libraries from the Jar file where
   * the application is running from.
   * 
   * @throws MdnException if extracting of libraries failed.
   */
    public static void preprocessSWT() throws MdnException {
        try {
            Library.loadLibrary("swt");
        } catch (IllegalArgumentException ignore) {
        } catch (Throwable e) {
            String filename = e.getMessage().replaceAll("no ([-_a-zA-Z0-9]+) in java.library.path", "$1");
            String os = System.getProperty("os.name");
            if (os.startsWith("Windows")) {
                filename += ".dll";
                getFile(filename, false);
            } else if (os.startsWith("Linux")) {
                filename += ".so";
                if (filename.startsWith("swt-gtk-")) {
                    filename = filename.replaceAll("swt-gtk-", "libswt-gtk-");
                    getFile(filename, false);
                    filename = filename.replaceAll("libswt-gtk-", "libswt-pi-gtk-");
                    getFile(filename, false);
                }
            }
        }
    }

    /**
   * Returns a {@link org.eclipse.swt.graphics.Color} object from a RGB value in hexadecimal
   * format.
   * 
   * @param hex the RGB value in hexadecimal format.
   * @return a {@link org.eclipse.swt.graphics.Color} object.
   */
    public static Color getColor(String hex) {
        int r, g, b;
        try {
            r = Integer.parseInt(hex.substring(0, 2).trim(), 16);
            g = Integer.parseInt(hex.substring(2, 4).trim(), 16);
            b = Integer.parseInt(hex.substring(4, 6).trim(), 16);
        } catch (NumberFormatException e) {
            return SWTResourceManager.getColor(255, 255, 255);
        } catch (StringIndexOutOfBoundsException e) {
            return SWTResourceManager.getColor(0, 0, 0);
        }
        return SWTResourceManager.getColor(r, g, b);
    }
}
