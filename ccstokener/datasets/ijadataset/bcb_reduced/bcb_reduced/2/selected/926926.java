package net.rptools.common.util;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.channels.FileChannel;

/**
 */
public class FileUtil {

    public static byte[] loadFile(File file) throws IOException {
        return getBytes(new FileInputStream(file));
    }

    public static byte[] loadResource(String resource) throws IOException {
        return getBytes(FileUtil.class.getClassLoader().getResourceAsStream(resource));
    }

    public static void saveResource(String resource, File destDir) throws IOException {
        int index = resource.lastIndexOf('/');
        String filename = index >= 0 ? resource.substring(index + 1) : resource;
        saveResource(resource, destDir, filename);
    }

    public static void saveResource(String resource, File destDir, String filename) throws IOException {
        File outFilename = new File(destDir + File.separator + filename);
        InputStream inStream = FileUtil.class.getClassLoader().getResourceAsStream(resource);
        OutputStream outStream = new BufferedOutputStream(new FileOutputStream(outFilename));
        int data = 0;
        while ((data = inStream.read()) != -1) {
            outStream.write(data);
        }
        outStream.close();
    }

    public static byte[] getBytes(URL url) throws IOException {
        return getBytes(url.openConnection().getInputStream());
    }

    private static byte[] getBytes(InputStream inStream) throws IOException {
        if (inStream == null) {
            throw new IllegalArgumentException("Input stream cannot be null");
        }
        ByteArrayOutputStream outStream = new ByteArrayOutputStream(10 * 1024);
        byte[] b = new byte[1024];
        while (true) {
            int read = inStream.read(b);
            if (read == 0 || read == -1) {
                break;
            }
            outStream.write(b, 0, read);
        }
        return outStream.toByteArray();
    }

    public static void copyFile(File sourceFile, File destFile) throws IOException {
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
