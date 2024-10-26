package utils;

import java.io.*;
import java.nio.channels.FileChannel;

/**
 *
 * @author Bastian Hinterleitner
 */
public abstract class SimpleFile {

    /**
     * downloads a file from an internet url to a destination file
     * @param url address of the file you want to download
     * @param dest path to the file you want to save it as
     * @return returns whether successful
     * @throws IOException java.io.BufferedInputStream and java.io.FileOutputStream
     */
    public static boolean download(String url, String dest) throws IOException {
        try {
            java.io.BufferedInputStream in = new java.io.BufferedInputStream(new java.net.URL(url).openStream());
            java.io.FileOutputStream fos = new java.io.FileOutputStream(dest);
            int oneChar = 0;
            while ((oneChar = in.read()) >= 0) {
                fos.write(oneChar);
            }
            in.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     *reads a textfile and returns the content as a String
     * @param url path to the textfile
     * @return String containing the content of the file
     */
    public static String read(String url) {
        String result = "";
        File file = new File(url);
        FileInputStream fis = null;
        BufferedInputStream bis = null;
        DataInputStream dis = null;
        try {
            fis = new FileInputStream(file);
            bis = new BufferedInputStream(fis);
            dis = new DataInputStream(bis);
            BufferedReader d = new BufferedReader(new InputStreamReader(dis));
            while (d.ready()) {
                result += d.readLine() + "\n";
            }
            fis.close();
            bis.close();
            dis.close();
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        }
        return result;
    }

    /**
     *saves a textfile
     * @param str the String the textfail will contain
     * @param url the Path to the file you want to save it to
     * @return returns whether successful
     */
    public static boolean save(String str, String url) {
        try {
            FileWriter fstream = new FileWriter(url);
            BufferedWriter out = new BufferedWriter(fstream);
            out.write(str);
            out.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     *Copies a file to a given location
     * @param sourceFile the File you want to copy
     * @param destFile the File you want it to be copied to
     * @throws IOException FileInputStream and FileOutputStream
     */
    public static void copy(File sourceFile, File destFile) throws IOException {
        if (!destFile.exists()) {
            destFile.createNewFile();
        }
        FileChannel source = null;
        FileChannel destination = null;
        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, source.size());
        } finally {
            if (source != null) {
                source.close();
            }
            if (destination != null) {
                destination.close();
            }
        }
    }
}
