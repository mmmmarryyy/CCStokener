package edu.psu.its.lionshare.util;

import java.util.zip.*;
import java.net.URL;
import java.net.URLConnection;
import java.io.*;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * LionShare Project - 2005
 *
 * ZipUtility.java
 *
 * Class that provides utility methods to handle zip files.
 *
 * @author Todd C. Beehler
 * Created on Mar 28, 2005
 */
public class ZipUtility {

    /**
   * Utility method that unzips the fileSrc zip file and extracts the contents
   * of the file with the sFilename parameter and copies the contents to the
   * the fileDest parameter.
   *
   * @param fileSrc - The zip file to unzip.
   * @param fileDest - The destination file to copy to.
   * @param sFilename - The name of the file to copy from.
   * @throws ZipException
   * @throws IOException
   */
    public static void unZipAndCopyFile(File fileSrc, File fileDest, String sFilename) throws ZipException, IOException {
        ZipFile zipFile = null;
        Enumeration entries = null;
        zipFile = new ZipFile(fileSrc);
        entries = zipFile.entries();
        boolean bFileFound = false;
        while (entries.hasMoreElements() && !bFileFound) {
            ZipEntry entry = (ZipEntry) entries.nextElement();
            if (entry.getName().equals(sFilename)) {
                copyInputStream(zipFile.getInputStream(entry), new BufferedOutputStream(new FileOutputStream(fileDest)));
            }
        }
        zipFile.close();
    }

    public static void unZipFileToDirectory(String sFileToUnzip, String sDirUnzip) throws IOException {
        unZipURLToDirectory(new File(sFileToUnzip).toURL(), sDirUnzip);
    }

    /**
   * Unzips a zip file represented by the sFileToZip absolute path
   * to the directory represented by the sDirUnzip path.
   *
   * Note: See examples below.
   *
   * @param sFileToUnzip The absolute path of the zip file to unzip
   *                     Example: C:/myZipFiles/ziptounzip.zip
   * @param sDirUnzip  The absolute file path to unzip the file to.
   *                   Example: C:/unZipDir
   * @throws java.io.IOException
   */
    public static void unZipURLToDirectory(java.net.URL url, String sDirUnzip) throws IOException {
        FileOutputStream fw = null;
        BufferedWriter bw = null;
        File dir = new File(sDirUnzip);
        if (!dir.exists()) {
            dir.mkdir();
        }
        URLConnection connection = url.openConnection();
        ZipInputStream input = new ZipInputStream(connection.getInputStream());
        ZipEntry zipEntry = input.getNextEntry();
        while (zipEntry != null) {
            File file = new File(sDirUnzip + "/" + zipEntry.toString());
            if (zipEntry.isDirectory()) {
                file.mkdir();
            } else {
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                while (true) {
                    byte[] bytes = new byte[4096];
                    int read = input.read(bytes);
                    if (read == -1) {
                        break;
                    }
                    bout.write(bytes, 0, read);
                }
                CheckedInputStream cis = new CheckedInputStream(new ByteArrayInputStream(bout.toByteArray()), new CRC32());
                fw = new FileOutputStream(file);
                while (true) {
                    byte[] bytes = new byte[4096];
                    int read = cis.read(bytes);
                    if (read < 0) {
                        break;
                    }
                    fw.write(bytes, 0, read);
                }
                fw.close();
            }
            zipEntry = input.getNextEntry();
        }
        input.close();
    }

    /**
   * Checks to see if a certain zip entry is in the zip file.
   *
   * Note: See examples below.
   *
   * @param fileZip The zip file to check to see whether it
   *                contains the zip entry.
   *                ziptocheck.zip
   * @param sEntryToFind  The relative path of the zip entry to find.
   *                      Example: imsmetadata.xml
   * @throws java.io.IOException
   */
    public static boolean entryExists(File fileZip, String sEntryToFind) throws IOException {
        ZipFile zipFile = null;
        zipFile = new ZipFile(fileZip);
        Enumeration enumEntries = zipFile.entries();
        boolean bFileFound = false;
        ZipEntry zipEntry = null;
        while (enumEntries.hasMoreElements() && !bFileFound) {
            try {
                zipEntry = (ZipEntry) enumEntries.nextElement();
            } catch (InternalError error) {
            } finally {
                if (zipEntry != null) {
                    if (zipEntry.getName().equals(sEntryToFind)) {
                        bFileFound = true;
                    }
                }
            }
        }
        zipFile.close();
        return bFileFound;
    }

    /**
   * Copies the in stream to the out stream.
   *
   * @param in The stream to copy.
   * @param out The stream to copy to.
   * @throws java.io.IOException
   */
    private static void copyInputStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int len;
        while ((len = in.read(buffer)) >= 0) {
            out.write(buffer, 0, len);
        }
        in.close();
        out.close();
    }
}
