package org.probatron.officeotron;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.log4j.Logger;
import org.probatron.officeotron.sessionstorage.ValidationSession;
import org.xml.sax.Attributes;

public class Utils {

    static Logger logger = Logger.getLogger(Utils.class);

    private static final int READ_BUFFER_SIZE = 32768;

    public static final int CLOSE_NONE = 0x0000;

    public static final int CLOSE_IN = 0x0001;

    public static final int CLOSE_OUT = 0x0010;

    public static String getQAtt(Attributes atts, String uri, String name) {
        for (int i = 0; i < atts.getLength(); i++) {
            if (atts.getURI(i).equals(uri) && atts.getLocalName(i).equals(name)) {
                return atts.getValue(i);
            }
        }
        return null;
    }

    /**
     * Reads all of an InputStream content into a byte array, and closes that InputStream.
     * 
     * @param in
     *            the InputStream to be read
     * @return byte[] its content
     */
    public static byte[] getBytesToEndOfStream(InputStream in, boolean closeSteam) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        transferBytesToEndOfStream(in, byteStream, (closeSteam ? (CLOSE_IN | CLOSE_OUT) : CLOSE_OUT));
        byte[] ba = byteStream.toByteArray();
        return ba;
    }

    /**
     * GETs the content at a URL and returns it as a byte array.
     * 
     * @param url
     *            - the URL
     * @return a byte array
     */
    public static byte[] derefUrl(URL url) {
        byte[] ba = null;
        InputStream is = null;
        try {
            URLConnection conn = url.openConnection();
            conn.connect();
            is = conn.getInputStream();
            ba = Utils.getBytesToEndOfStream(is, true);
        } catch (IOException e) {
            logger.warn(e.getMessage());
            return null;
        }
        return ba;
    }

    /**
     * Writes the bytes in <tt>ba</tt> to the file named <tt>fn</tt>, creating it if necessary.
     * 
     * @param ba
     *            the byte array to be written
     * @param fn
     *            the filename of the file to be written to
     * @throws IOException
     */
    public static void writeBytesToFile(byte[] ba, String fn) throws IOException {
        File f = new File(fn);
        f.createNewFile();
        FileOutputStream fos = null;
        ByteArrayInputStream bis = null;
        try {
            fos = new FileOutputStream(f);
            bis = new ByteArrayInputStream(ba);
            transferBytesToEndOfStream(bis, fos, CLOSE_IN | CLOSE_OUT);
            System.gc();
        } catch (FileNotFoundException e) {
            throw new RuntimeException("File not found when writing: ", e);
        }
    }

    public static void streamFromFile(String fn, OutputStream os, boolean closeStream) throws IOException {
        File f = new File(fn);
        try {
            FileInputStream fis = new FileInputStream(f);
            int flags = CLOSE_IN;
            if (closeStream) {
                flags |= CLOSE_OUT;
            }
            Utils.transferBytesToEndOfStream(fis, os, flags);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("File not found when reading: ", e);
        }
    }

    public static long streamToFile(InputStream is, String fn, boolean closeStream) throws IOException {
        File f = new File(fn);
        f.createNewFile();
        try {
            FileOutputStream fos = new FileOutputStream(f);
            int flags = closeStream ? (CLOSE_IN | CLOSE_OUT) : CLOSE_OUT;
            return Utils.transferBytesToEndOfStream(is, fos, flags);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("File not found when writing: ", e);
        }
    }

    public static ValidationSession autoCreateValidationSession(Submission sub, ReportFactory reportFactory) {
        ValidationSession vs = null;
        String fn = new File(sub.getCandidateFile()).toURI().toASCIIString();
        logger.debug("auto-detecting package at: " + fn);
        File manifest = sub.getEntryFile("/META-INF/manifest.xml");
        if (manifest.canRead()) {
            logger.info("Auto detected ODF package");
            vs = new ODFValidationSession(sub.uuid, sub.getOptionMap(), reportFactory);
        } else {
            File rels = sub.getEntryFile("/_rels/.rels");
            if (rels.canRead()) {
                logger.info("Auto detected OOXML package");
                vs = new OOXMLValidationSession(sub.uuid, reportFactory);
            }
        }
        return vs;
    }

    /**
     * Reads all of an InputStream content into an OutputStream, via a buffer.
     * 
     * @param in
     *            the InputStream to be read
     * @return int number of bytes read
     */
    public static long transferBytesToEndOfStream(InputStream in, OutputStream out, int closeFlags) throws IOException {
        byte[] buf = new byte[READ_BUFFER_SIZE + 1];
        long written = 0;
        int count;
        while ((count = in.read(buf)) != -1) {
            out.write(buf, 0, count);
            written += count;
        }
        if ((closeFlags & CLOSE_IN) != 0) {
            streamClose(in);
        }
        if ((closeFlags & CLOSE_OUT) != 0) {
            streamClose(out);
        }
        buf = null;
        return written;
    }

    public static void streamClose(InputStream is) {
        try {
            if (is != null) is.close();
        } catch (Exception e) {
        }
    }

    public static void streamClose(OutputStream os) {
        try {
            if (os != null) {
                os.flush();
                os.close();
            }
        } catch (Exception e) {
        }
    }

    public static void unzipArchive(File archive, File outputDir) {
        try {
            ZipFile zipfile = new ZipFile(archive);
            for (Enumeration<? extends ZipEntry> e = zipfile.entries(); e.hasMoreElements(); ) {
                ZipEntry entry = e.nextElement();
                unzipEntry(zipfile, entry, outputDir);
            }
        } catch (Exception e) {
            logger.error("Error while extracting file " + archive, e);
        }
    }

    private static void unzipEntry(ZipFile zipfile, ZipEntry entry, File outputDir) throws IOException {
        if (entry.isDirectory()) {
            new File(outputDir, entry.getName()).mkdirs();
            return;
        }
        File outputFile = new File(outputDir, entry.getName());
        if (!outputFile.getParentFile().exists()) {
            outputFile.getParentFile().mkdirs();
        }
        BufferedInputStream inputStream = new BufferedInputStream(zipfile.getInputStream(entry));
        BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(outputFile));
        try {
            transferBytesToEndOfStream(inputStream, outputStream, CLOSE_NONE);
        } finally {
            outputStream.close();
            inputStream.close();
        }
    }

    public static boolean deleteDir(File path) {
        logger.info("Deleting folder " + path);
        if (path.exists()) {
            File[] files = path.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    deleteDir(files[i]);
                } else {
                    files[i].delete();
                    System.gc();
                }
            }
        }
        boolean ret = path.delete();
        return ret;
    }
}
