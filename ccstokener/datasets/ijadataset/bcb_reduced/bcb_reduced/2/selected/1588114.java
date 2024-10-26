package com.sbmoon.util;

import java.io.*;
import java.net.*;
import java.util.*;

public class FileUtil {

    public static byte[] readBytes(File file) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            InputStream is = new BufferedInputStream(new FileInputStream(file));
            int count = 0;
            byte[] buf = new byte[16384];
            while ((count = is.read(buf)) != -1) {
                if (count > 0) {
                    baos.write(buf, 0, count);
                }
            }
            is.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return baos.toByteArray();
    }

    public static char[] readCharList(File file, String encoding) throws Exception {
        FileInputStream fis = new FileInputStream(file);
        InputStreamReader reader = new InputStreamReader(fis, encoding);
        return readCharList(reader);
    }

    public static char[] readCharList(Reader in) throws Exception {
        return readCharArrayWriter(in).toCharArray();
    }

    public static CharArrayWriter readCharArrayWriter(Reader reader) throws Exception {
        CharArrayWriter caw = new CharArrayWriter();
        int count = 0;
        char[] buf = new char[16384];
        while ((count = reader.read(buf)) != -1) {
            if (count > 0) {
                caw.write(buf, 0, count);
            }
        }
        reader.close();
        return caw;
    }

    public static void writeBytes(File file, byte[] data) {
        try {
            OutputStream os = new BufferedOutputStream(new FileOutputStream(file));
            os.write(data);
            os.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void writeChars(File file, char[] data) {
        try {
            Writer os = new BufferedWriter(new FileWriter(file));
            os.write(data);
            os.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int getFileLength(final File file) {
        if (file == null) {
            return 0;
        }
        try {
            final URL url = new URL("file:///" + file.getCanonicalPath());
            if (url == null) {
                return 0;
            }
            final URLConnection uc = url.openConnection();
            if (uc == null) {
                return 0;
            }
            return uc.getContentLength();
        } catch (IOException e) {
            return 0;
        }
    }

    public static String getFileTailName(File file) {
        String filename = file.getName();
        int dotIdx = filename.indexOf(".");
        if (dotIdx > -1) {
            return filename.substring(dotIdx);
        } else {
            return "";
        }
    }

    public static void removeFilesUnderDir(final File dir) {
        if (!dir.exists()) {
            return;
        }
        File[] files = dir.listFiles();
        File file;
        for (int i = 0, len = files.length; i < len; i++) {
            file = files[i];
            if (file.isDirectory()) {
                removeFilesUnderDir(file);
                file.delete();
            } else {
                file.delete();
            }
        }
    }

    public static BufferedReader getBufferedReader(File file) throws FileNotFoundException {
        return new BufferedReader(new InputStreamReader(new FileInputStream(file)));
    }

    public static boolean isImgFile(String name) {
        name = name.toUpperCase();
        String[] imgExt = IMG_EXT;
        for (int i = 0, len = imgExt.length; i < len; i++) {
            if (name.endsWith(imgExt[i])) {
                return true;
            }
        }
        return false;
    }

    static final String[] IMG_EXT = { "GIF", "JPG", "JPEG", "PNG", "BMP" };
}
