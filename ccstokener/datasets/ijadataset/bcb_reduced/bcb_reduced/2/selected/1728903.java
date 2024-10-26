package br.net.woodstock.rockframework.utils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

public abstract class FileUtils {

    private static final char UNIX_FILE_SEPARATOR = '/';

    private static final char WINDOWS_FILE_SEPARATOR = '\\';

    private static final char EXTENSION_SEPARATOR = '.';

    private FileUtils() {
    }

    public static File createTempFile(final String name) {
        File tmpDir = new File(SystemUtils.getProperty(SystemUtils.JAVA_IO_TMPDIR_PROPERTY));
        File file = new File(tmpDir, name);
        return file;
    }

    public static String getName(final String src) {
        if (ConditionUtils.isEmpty(src)) {
            return null;
        }
        if (src.indexOf(FileUtils.UNIX_FILE_SEPARATOR) != -1) {
            int index = src.lastIndexOf(FileUtils.UNIX_FILE_SEPARATOR);
            String fileName = src.substring(index + 1);
            return fileName;
        } else if (src.indexOf(FileUtils.WINDOWS_FILE_SEPARATOR) != -1) {
            int index = src.lastIndexOf(FileUtils.WINDOWS_FILE_SEPARATOR);
            String fileName = src.substring(index + 1);
            return fileName;
        }
        return src;
    }

    public static String getName(final File file) {
        if (file == null) {
            return null;
        }
        return file.getName();
    }

    public static String getName(final URL url) {
        if (url == null) {
            return null;
        }
        String path = url.getPath();
        return FileUtils.getName(path);
    }

    public static String getExtension(final String src) {
        if (ConditionUtils.isEmpty(src)) {
            return null;
        }
        String fileName = FileUtils.getName(src);
        if ((fileName != null) && (fileName.indexOf(FileUtils.EXTENSION_SEPARATOR) != -1)) {
            return fileName.substring(fileName.lastIndexOf(FileUtils.EXTENSION_SEPARATOR) + 1);
        }
        return null;
    }

    public static String getExtension(final File file) {
        if (file == null) {
            return null;
        }
        String fileName = file.getName();
        if (fileName.indexOf(FileUtils.EXTENSION_SEPARATOR) != -1) {
            return fileName.substring(fileName.lastIndexOf(FileUtils.EXTENSION_SEPARATOR) + 1);
        }
        return null;
    }

    public static String getParentPath(final File file) {
        if (file == null) {
            return null;
        }
        return file.getParent();
    }

    public static String getParentPath(final String src) {
        if (ConditionUtils.isEmpty(src)) {
            return null;
        }
        if (src.indexOf(FileUtils.UNIX_FILE_SEPARATOR) != -1) {
            int index = src.lastIndexOf(FileUtils.UNIX_FILE_SEPARATOR);
            String path = src.substring(0, index);
            return path;
        } else if (src.indexOf(FileUtils.WINDOWS_FILE_SEPARATOR) != -1) {
            int index = src.lastIndexOf(FileUtils.WINDOWS_FILE_SEPARATOR);
            String path = src.substring(0, index);
            return path;
        }
        return src;
    }

    public static String getPath(final File file) {
        if (file == null) {
            return null;
        }
        return file.getAbsolutePath();
    }

    public static String getPath(final String src) {
        if (ConditionUtils.isEmpty(src)) {
            return null;
        }
        return src;
    }

    public static int getSize(final File file) throws IOException {
        if (file == null) {
            return -1;
        }
        URLConnection con = file.toURI().toURL().openConnection();
        return con.getContentLength();
    }

    public static int getSize(final URL url) throws IOException {
        if (url == null) {
            return -1;
        }
        URLConnection con = url.openConnection();
        return con.getContentLength();
    }

    public static String getType(final File file) throws IOException {
        if (file == null) {
            return null;
        }
        URLConnection con = file.toURI().toURL().openConnection();
        return con.getContentType();
    }

    public static String getType(final URL url) throws IOException {
        if (url == null) {
            return null;
        }
        URLConnection con = url.openConnection();
        return con.getContentType();
    }

    public static String getTypeByExtension(final String extension) {
        return MimeUtils.getMimeType(extension);
    }

    public static String getExtensionByType(final String mimeType) {
        return MimeUtils.getExtension(mimeType);
    }
}
