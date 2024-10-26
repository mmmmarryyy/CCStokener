package org.thirdstreet.utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * General file utilities for our blogger tool
 * @author bramlej
 */
public final class FileUtils {

    private static final Log logger = LogFactory.getLog(FileUtils.class);

    /**
	 * Constructor - declared private as all access is via static methods
	 */
    private FileUtils() {
        super();
    }

    /**
	 * Writes the given content to the given file
	 * @param filename The name of the file we are writing
	 * @param content The content for the file
	 */
    public static void writeFile(String filename, String content) {
        try {
            org.apache.commons.io.FileUtils.writeStringToFile(new File(filename), content);
        } catch (Exception e) {
            logger.error("Failed writing file " + filename, e);
            throw new RuntimeException("Failed writing file " + filename, e);
        }
    }

    /**
	 * Reads the given file to a string
	 * 
	 * @param filename The name of the file we are reading
	 * @return String The file contents
	 */
    public static String readFile(String filename) {
        String result = null;
        try {
            File file = new File(filename);
            if (!file.exists()) {
                logger.warn("File " + filename + " does not exist!");
            } else {
                result = org.apache.commons.io.FileUtils.readFileToString(file);
            }
        } catch (Exception e) {
            logger.error("Failed to read file " + filename, e);
            throw new RuntimeException("Failed to read file " + filename, e);
        }
        return result;
    }

    /**
	 * Returns a flag indicating if the file exists
	 * @param filename the name of the file we are checking
	 * @return boolean Returns true if the file exists, false otherwise
	 */
    public static boolean fileExists(String filename) {
        File file = new File(filename);
        return file.exists();
    }

    /**
	 * Verifies the directory exists, if it doesn't it will create it
	 * @param directory The directory we are checking
	 */
    public static void verifyDirectory(String directory) {
        if (directory != null) {
            File f = new File(directory);
            if (!f.exists()) {
                f.mkdirs();
            }
        }
    }

    /**
	 * Downloads an image to a file
	 * @param imageUrl The image url
	 * @param filename The filename to use for the image
	 * @return boolean Returns true if the file was downloaded
	 * false otherwise
	 */
    public static boolean downloadImage(String imageUrl, String filename) {
        OutputStream out = null;
        URLConnection conn = null;
        InputStream in = null;
        boolean deleteFile = false;
        try {
            logger.debug("Attempting to download image from url " + imageUrl);
            URL url = new URL(imageUrl);
            out = new BufferedOutputStream(new FileOutputStream(filename));
            conn = url.openConnection();
            in = conn.getInputStream();
            byte[] buffer = new byte[1024];
            int numRead;
            long numWritten = 0;
            while ((numRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, numRead);
                numWritten += numRead;
            }
            logger.debug("Downloaded image to file " + filename);
        } catch (Exception e) {
            deleteFile = true;
            logger.error("Failed to download image " + imageUrl, e);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException ioe) {
                ;
            }
            if (deleteFile) {
                logger.debug("Failed downloading image - removing file");
                File f = new File(filename);
                if (f.exists()) {
                    f.delete();
                    logger.debug("Image file " + filename + " deleted");
                }
            }
        }
        return !deleteFile;
    }

    /**
	 * Determines if the given file name is valid
	 * @param name The name of the file we are checking
	 * @return boolean Returns true if the file is valid,
	 * false otherwise
	 */
    public static boolean isValid(String name) {
        boolean valid = true;
        try {
            File f = new File(name);
            if (!f.exists()) {
                if (!f.createNewFile()) {
                    valid = false;
                } else {
                    f.delete();
                }
            }
        } catch (Exception e) {
            logger.error("File " + name + " is not valid", e);
            valid = false;
        }
        return valid;
    }
}
