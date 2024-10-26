package com.safi.asterisk.handler.classloader;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import org.apache.log4j.Logger;
import com.safi.asterisk.handler.classloader.exception.JclException;

/**
 * JarResources reads jar files and loads the class content/bytes in a HashMap
 * 
 * @author Kamran Zafar
 * 
 */
public class JarResources {

    protected Map<String, byte[]> jarEntryContents;

    static Logger logger = Logger.getLogger(JarResources.class);

    /**
	 * @throws IOException
	 */
    public JarResources() {
        jarEntryContents = new HashMap<String, byte[]>();
    }

    /**
	 * @param name
	 * @return byte[]
	 */
    public byte[] getResource(String name) {
        return jarEntryContents.get(name);
    }

    /**
	 * @return Map
	 */
    public Map<String, byte[]> getResources() {
        return jarEntryContents;
    }

    /**
	 * Reads the specified jar file
	 * 
	 * @param jarFile
	 * @throws IOException
	 * @throws JclException
	 */
    public void loadJar(String jarFile) throws IOException, JclException {
        FileInputStream fis = new FileInputStream(jarFile);
        loadJar(fis);
        fis.close();
    }

    /**
	 * Reads the jar file from a specified URL
	 * 
	 * @param url
	 * @throws IOException
	 * @throws JclException
	 */
    public void loadJar(URL url) throws IOException, JclException {
        InputStream in = url.openStream();
        loadJar(in);
        in.close();
    }

    /**
	 * Load the jar contents from InputStream
	 * 
	 * @throws IOException
	 * @throws JclException
	 */
    public void loadJar(InputStream jarStream) throws IOException, JclException {
        java.io.BufferedInputStream bis = null;
        java.util.jar.JarInputStream jis = null;
        try {
            bis = new java.io.BufferedInputStream(jarStream);
            jis = new java.util.jar.JarInputStream(bis);
            JarEntry jarEntry = null;
            while ((jarEntry = jis.getNextJarEntry()) != null) {
                logger.debug(dump(jarEntry));
                if (jarEntry.isDirectory()) {
                    continue;
                }
                if (jarEntryContents.containsKey(jarEntry.getName())) {
                    System.err.println("we aredddy jarmon " + jarEntry.getName());
                    if (!Configuration.supressCollisionException()) throw new JclException("Class/Resource " + jarEntry.getName() + " already loaded"); else {
                        logger.debug("Class/Resource " + jarEntry.getName() + " already loaded; ignoring entry...");
                        continue;
                    }
                }
                logger.debug("Entry Name: " + jarEntry.getName() + "," + "Entry Size: " + jarEntry.getSize());
                byte[] b = new byte[2048];
                java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
                int len = 0;
                while ((len = jis.read(b)) > 0) {
                    out.write(b, 0, len);
                }
                jarEntryContents.put(jarEntry.getName(), out.toByteArray());
                logger.debug(jarEntry.getName() + ", size=" + out.size() + ",csize=" + jarEntry.getCompressedSize());
                out.close();
            }
        } catch (NullPointerException e) {
            logger.debug("Done loading.");
        } finally {
            jis.close();
            bis.close();
        }
    }

    /**
	 * For debugging
	 * 
	 * @param je
	 * @return String
	 */
    private String dump(JarEntry je) {
        StringBuffer sb = new StringBuffer();
        if (je.isDirectory()) {
            sb.append("d ");
        } else {
            sb.append("f ");
        }
        if (je.getMethod() == JarEntry.STORED) {
            sb.append("stored   ");
        } else {
            sb.append("defalted ");
        }
        sb.append(je.getName());
        sb.append("\t");
        sb.append("" + je.getSize());
        if (je.getMethod() == JarEntry.DEFLATED) {
            sb.append("/" + je.getCompressedSize());
        }
        return (sb.toString());
    }
}
