package de.internnetz.eaf.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedList;
import java.util.List;
import de.internnetz.eaf.i18n.Messages;

/**
 * Provides methods to write data into files.
 */
public class FileTools {

    private static final String CHARACTER_ENCODING = "UTF-8";

    /**
	 * Takes every object from the list and appends the objects toString()
	 * method output to the file located at the given path.
	 * 
	 * @param list
	 *            a list of objects
	 * @param absolutePath
	 *            the path to the file
	 * @throws IOException
	 */
    public static void appendToFile(List<Object> list, String absolutePath) throws IOException {
        File f = new File(absolutePath);
        FileTools.appendToFile(list.toArray(), f);
    }

    /**
	 * Takes every object from the array and appends the objects toString()
	 * method output to to given file.
	 * 
	 * @param data
	 *            an array of objects
	 * @param file
	 *            a reference to the file
	 * @throws IOException
	 */
    public static void appendToFile(Object[] data, File file) throws IOException {
        if (!file.exists()) {
            File parentDir = file.getAbsoluteFile().getParentFile();
            if (!parentDir.exists()) {
                FileTools.createTargetDir(parentDir.getAbsolutePath());
            }
            file.createNewFile();
        }
        if (data == null) {
            return;
        }
        try {
            PrintWriter w = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file, true), CHARACTER_ENCODING));
            for (int n = 0; n < data.length; n++) {
                if (data[n] != null) {
                    w.write(data[n].toString());
                } else {
                    w.write("\n");
                }
            }
            w.flush();
            w.close();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Takes every object from the array and appends the objects toString()
	 * method output to the file located at the given path.
	 * 
	 * @param data
	 *            an array of objects
	 * @param absolutePath
	 *            the path to the file
	 * @throws IOException
	 */
    public static void appendToFile(Object[] data, String absolutePath) throws IOException {
        File f = new File(absolutePath);
        FileTools.appendToFile(data, f);
    }

    public static LinkedList<File> collectAllFiles(File startFrom, String filename, boolean isRecursive) {
        LinkedList<File> res = new LinkedList<File>();
        if (!startFrom.exists()) {
            throw new RuntimeException(Messages.getFormatedString("FileTools.2", new Object[] { startFrom.getAbsolutePath() }));
        }
        if (!startFrom.isDirectory()) {
            throw new RuntimeException(Messages.getFormatedString("FileTools.5", new Object[] { startFrom.getAbsolutePath() }));
        }
        String[] files = startFrom.list();
        if (files != null) {
            for (String s : files) {
                File f = new File(startFrom.getAbsolutePath() + File.separator + s);
                if (f.isDirectory()) {
                    if (isRecursive) {
                        res.addAll(collectAllFiles(f, filename, isRecursive));
                    } else {
                        res.add(f);
                    }
                } else if (filename != null) {
                    if (f.getName().toUpperCase().startsWith(filename.toUpperCase())) {
                        res.add(f);
                    }
                } else {
                    res.add(f);
                }
            }
        }
        return res;
    }

    public static File createNewFile(String filename) {
        File f = new File(filename);
        try {
            if (f.exists()) {
                if (f.isFile()) {
                    f.createNewFile();
                } else {
                    throw new RuntimeException(Messages.getString("FileTools.8"));
                }
            } else {
                f.createNewFile();
            }
            return f;
        } catch (IOException e) {
            throw new RuntimeException(Messages.getFormatedString("FileTools.9", new Object[] { filename, e.getMessage() }));
        }
    }

    public static void createTargetDir(String filename) {
        File dir = new File(filename);
        if (dir.exists()) {
            if (dir.isDirectory()) {
                return;
            } else {
                throw new RuntimeException(Messages.getString("FileTools.12"));
            }
        } else {
            dir.mkdirs();
        }
    }

    public static void download(String address) {
        String filename = getFilenameFromURL(address);
        if (filename != null) {
            download(address, filename);
        }
    }

    public static void download(String address, String localFileName) {
        OutputStream out = null;
        URLConnection conn = null;
        InputStream in = null;
        try {
            URL url = new URL(address);
            out = new BufferedOutputStream(new FileOutputStream(localFileName));
            conn = url.openConnection();
            in = conn.getInputStream();
            byte[] buffer = new byte[1024];
            int numRead;
            long numWritten = 0;
            while ((numRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, numRead);
                numWritten += numRead;
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException ioe) {
            }
        }
    }

    public static String getFilenameFromURL(String address) {
        int lastSlashIndex = address.lastIndexOf('/');
        if (lastSlashIndex >= 0 && lastSlashIndex < address.length() - 1) {
            return address.substring(lastSlashIndex + 1);
        } else {
            System.err.println(Messages.getFormatedString("FileTools.13", new Object[] { address }));
            return null;
        }
    }

    /**
	 * Takes every object from the list and writes the objects toString() method
	 * output to the file located at the given path. NOTE: if the file already
	 * exists it will be overwritten.
	 * 
	 * @param list
	 *            a list of objects
	 * @param absolutePath
	 *            the path to the file
	 * @throws IOException
	 */
    public static void writeToFile(List<Object> list, String absolutePath) throws IOException {
        File f = new File(absolutePath);
        FileTools.writeToFile(list.toArray(), f);
    }

    /**
	 * Takes every object from the array and writes the objects toString()
	 * method output to to given file. NOTE: if the file already exists it will
	 * be overwritten.
	 * 
	 * @param data
	 *            an array of objects
	 * @param file
	 *            a reference to the file
	 * @throws IOException
	 */
    public static void writeToFile(Object[] data, File file) throws IOException {
        if (file.exists()) {
            file.delete();
        }
        FileTools.appendToFile(data, file);
    }

    /**
	 * Takes every object from the array and writes the objects toString()
	 * method output to the file located at the given path. NOTE: if the file
	 * already exists it will be overwritten.
	 * 
	 * @param data
	 *            an array of objects
	 * @param absolutePath
	 *            the path to the file
	 * @throws IOException
	 */
    public static File writeToFile(Object[] data, String absolutePath) throws IOException {
        File f = new File(absolutePath);
        FileTools.writeToFile(data, f);
        return f;
    }

    public static void writeToFile(InputStream is, File out) throws IOException {
        if (is.markSupported()) {
            is.reset();
        }
        FileWriter w = new FileWriter(out);
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line = br.readLine();
        while (line != null) {
            w.write(line + "\n");
            line = br.readLine();
        }
        w.close();
        br.close();
    }

    public static void writeBytesToFile(InputStream is, File out) throws IOException {
        InputStream in = new BufferedInputStream(is);
        ByteArrayOutputStream of = new ByteArrayOutputStream();
        RandomAccessFile file = new RandomAccessFile(out, "rw");
        byte[] buffer = new byte[4096];
        for (int read = 0; (read = in.read(buffer)) != -1; of.write(buffer, 0, read)) ;
        file.write(of.toByteArray());
        file.close();
    }

    public static String convertInputStreamToString(InputStream is) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        StringBuilder res = new StringBuilder();
        String line = null;
        while ((line = br.readLine()) != null) {
            res.append(line);
            res.append("\n");
        }
        return res.toString();
    }
}
