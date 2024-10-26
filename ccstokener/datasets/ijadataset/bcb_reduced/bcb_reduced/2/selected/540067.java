package org.openfrag.OpenCDS.core.util;

import org.openfrag.OpenCDS.core.exceptions.InvalidActionException;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.net.*;

/**
 * The size checker class can convert file sizes on your harddrive, or on the
 *  internet.
 *
 * @author  Lars 'Levia' Wesselius
*/
public class SizeChecker {

    /**
     * Compares two file sizes.
     *
     * @param   fileOne The first file.
     * @param   fileTwo The second file.
     * @return  True whether the file sizes are the same, false if not.
    */
    public static boolean compareSize(File fileOne, File fileTwo) {
        return fileOne.length() == fileTwo.length();
    }

    /**
     * Compares two file sizes.
     *
     * @param   netFile The file on the web to compare.
     * @param   file    The file to compare with.
     * @return  True if the file sizes are the same, false when not.
    */
    public static boolean compareSize(String netFile, File file) throws InvalidActionException {
        try {
            int fileSize = getNetFileSize(netFile);
            if (fileSize == file.length()) {
                return true;
            } else {
                return false;
            }
        } catch (InvalidActionException e) {
            throw new InvalidActionException(e.getMessage());
        }
    }

    /**
     * Compare two file sizes
     *
     * @param   netFileOne  A file on the web to compare.
     * @param   netFileTwo  A file on the web to compare.
     * @return  True if the two files have the same size, false if not.
    */
    public static boolean compareSize(String netFileOne, String netFileTwo) {
        try {
            int fileSizeOne = getNetFileSize(netFileOne);
            int fileSizeTwo = getNetFileSize(netFileTwo);
            if (fileSizeOne == fileSizeTwo) {
                return true;
            } else {
                return false;
            }
        } catch (InvalidActionException e) {
            throw new InvalidActionException(e.getMessage());
        }
    }

    /**
     * Get net file size.
     *
     * @param   netFile The files size to get on the web.
     * @return  A int containing the bytes, otherwise 0.
    */
    public static int getNetFileSize(String netFile) throws InvalidActionException {
        URL url;
        URLConnection conn;
        int size;
        try {
            url = new URL(netFile);
            conn = url.openConnection();
            size = conn.getContentLength();
            conn.getInputStream().close();
            if (size < 0) {
                throw new InvalidActionException("Could not determine file size.");
            } else {
                return size;
            }
        } catch (Exception e) {
            throw new InvalidActionException(e.getMessage());
        }
    }
}
