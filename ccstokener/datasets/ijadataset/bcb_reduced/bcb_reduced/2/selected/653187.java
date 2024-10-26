package org.chemicalcovers.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.channels.FileChannel;

/**
 * Usefull methods to work with files
 * @author beaujean
 *
 */
public final class FileUtilities {

    private static int BUFSIZE = 4096;

    public static String getExtension(String filename) {
        String ext = null;
        int i = filename.lastIndexOf('.');
        if (i > 0 && i < filename.length() - 1) {
            ext = filename.substring(i + 1).toLowerCase();
        }
        return ext;
    }

    /**
	 * Copy file from source to target
	 * @param source
	 * @param target
	 * @throws Exception
	 */
    public static void copyFile(String source, String target) throws Exception {
        copyFile(new File(source), new File(target));
    }

    public static void copyFile(File source, File target) throws Exception {
        copyFile(new FileInputStream(source), new FileOutputStream(target));
    }

    public static void copyFile(FileInputStream source, FileOutputStream target) throws Exception {
        FileChannel inChannel = source.getChannel();
        FileChannel outChannel = target.getChannel();
        try {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } catch (IOException e) {
            throw e;
        } finally {
            if (inChannel != null) inChannel.close();
            if (outChannel != null) outChannel.close();
        }
    }

    public static void copyFile(InputStream source, String target) throws Exception {
        InputStreamReader reader = new InputStreamReader(source);
        FileWriter writer = new FileWriter(target);
        try {
            int c = -1;
            while ((c = reader.read()) != -1) writer.write(c);
        } catch (IOException e) {
            throw e;
        } finally {
            reader.close();
            writer.close();
        }
    }

    public static void downloadFile(URL url, String filename) throws IOException {
        InputStream stream = url.openConnection().getInputStream();
        byte[] buffer = new byte[BUFSIZE];
        FileOutputStream fout = new FileOutputStream(filename);
        for (int bytesRead = 0; bytesRead >= 0; bytesRead = stream.read(buffer, 0, BUFSIZE)) fout.write(buffer, 0, bytesRead);
        fout.close();
    }

    private FileUtilities() {
    }
}
