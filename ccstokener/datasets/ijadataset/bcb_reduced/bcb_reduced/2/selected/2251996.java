package org.slasoi.businessManager.common.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import org.apache.log4j.Logger;

/**
 * Utility Class for loading resources from bundle, either
 * inside a jar or from the filesystem
 */
public final class FileLoader {

    private static final Logger logger = Logger.getLogger(FileLoader.class.getName());

    private FileLoader() {
    }

    /**
     * Loads the content of a file into a String
     * 
     * @param fileName The name of file to load
     * @return The content of a file as a byte array
     * @throws IOException 
     * @throws Exception
     */
    public static byte[] loadFileBytes(String fileName) throws Exception {
        InputStream is = null;
        byte[] buffer = new byte[1024];
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int n = 0;
        try {
            URL url = checkJarFile(fileName);
            if (url != null) {
                is = url.openStream();
                while (-1 != (n = is.read(buffer))) {
                    baos.write(buffer, 0, n);
                }
                return baos.toByteArray();
            }
            File file = checkFsFile(fileName);
            if (file != null) {
                is = new FileInputStream(file);
                while (-1 != (n = is.read(buffer))) {
                    baos.write(buffer, 0, n);
                }
                return baos.toByteArray();
            }
        } catch (IOException e) {
            throw new Exception(e);
        } finally {
            if (is != null) {
                is.close();
                is = null;
            }
        }
        throw new Exception(fileName + "NOT found");
    }

    /**
	 * Loads the content of a file into a String
	 * 
	 * @param fileName The name of fiel to load
	 * @return The content of a file as a String
	 * @throws IOException 
	 * @throws Exception
	 */
    public static String loadFileContent(String fileName) throws Exception {
        StringBuilder buf = null;
        InputStream is = null;
        int i = 0;
        try {
            URL url = checkJarFile(fileName);
            if (url != null) {
                is = url.openStream();
                buf = new StringBuilder();
                while (is.available() != 0 && (i = is.read()) != -1) {
                    buf.append((char) i);
                }
                return buf.toString();
            }
            File file = checkFsFile(fileName);
            if (file != null) {
                is = new FileInputStream(file);
                buf = new StringBuilder();
                while (is.available() != 0 && (i = is.read()) != -1) {
                    buf.append((char) i);
                }
                return buf.toString();
            }
        } catch (IOException e) {
            throw new Exception(e);
        } finally {
            if (is != null) {
                is.close();
                is = null;
            }
        }
        throw new Exception(fileName + "NOT found");
    }

    /**
	 * Checks the existence of file inside a jar
	 * 
	 * @param fileName The name of file to load
	 * @return The URL with the file location
	 */
    public static URL checkJarFile(String fileName) {
        URL url = FileLoader.class.getClassLoader().getResource(fileName);
        if (url == null) {
            return null;
        } else {
            return url;
        }
    }

    /**
	 * Checks the existence of file in the filesystem, relative to the current execution dir
	 * 
	 * @param fileName The name of file to load
	 * @return The file found 
	 */
    public static File checkFsFile(String fileName) {
        File file = new File(System.getProperty("app.dir") + System.getProperty("file.separator") + fileName);
        if (!file.exists()) {
            return null;
        } else {
            return file;
        }
    }

    /**
	 * Loads the properties from a <b>properties</b> file
	 * 
	 * @param fileName The name of file to load
	 * @return The content of the file as a <b>properties</b> object
	 */
    public static Properties loadPropertiesFile(String fileName) throws Exception {
        Properties properties = null;
        URL url = checkJarFile(fileName);
        File file = checkFsFile(fileName);
        try {
            if (url != null) {
                properties = new Properties();
                properties.load(url.openStream());
            } else if (file != null) {
                properties = new Properties();
                properties.load(new FileInputStream(file));
            } else {
                logger.info("properties file: " + fileName + " NOT found");
                throw new Exception(fileName + " NOT Found");
            }
        } catch (IOException ioe) {
            throw new Exception("IOException loading " + fileName + "\t " + ioe);
        }
        logger.info(fileName + " file LOADED");
        return properties;
    }
}
