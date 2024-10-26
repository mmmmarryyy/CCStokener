package net.sf.mustang.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import net.sf.mustang.K;

public class FileTool {

    static final int BUFFER_SIZE = 16384;

    public static void forceClose(java.io.InputStream inp) {
        if (inp != null) {
            try {
                inp.close();
            } catch (IOException ignore) {
            }
        }
        return;
    }

    public static void forceClose(java.io.OutputStream out) {
        if (out != null) {
            try {
                out.close();
            } catch (IOException ignore) {
            }
        }
        return;
    }

    public static void createParent(File dest) {
        File destParent;
        if ((destParent = dest.getParentFile()) != null && !destParent.exists()) destParent.mkdirs();
    }

    public static void copyFile(String sourceFile, String destFile) throws IOException {
        copyFile(new File(sourceFile), new File(destFile));
    }

    public static void copyFile(File source, File dest) throws IOException {
        if (source.isDirectory()) {
            throw new IOException("Source filename " + source.getName() + " is a directory");
        }
        if (source.exists() && !source.canRead()) {
            throw new IOException("Read access on " + source.getName() + " denied");
        }
        java.io.InputStream in = new FileInputStream(source);
        copyFile(in, dest);
    }

    public static void copyFile(InputStream in, File dest) throws IOException {
        byte[] buf = new byte[BUFFER_SIZE];
        int n;
        if (dest.exists() && !dest.canWrite()) {
            throw new IOException("Cannot write on " + dest.getName());
        }
        if (dest.isDirectory()) {
            throw new IOException("Destination filename " + dest.getName() + " is a directory");
        }
        createParent(dest);
        java.io.OutputStream out = new FileOutputStream(dest);
        while ((n = in.read(buf)) != -1) {
            out.write(buf, 0, n);
        }
        forceClose(in);
        forceClose(out);
    }

    public static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int n;
        while ((n = in.read(buffer)) != -1) {
            out.write(buffer, 0, n);
        }
        forceClose(in);
        forceClose(out);
    }

    public static void copyFile2Dir(String sourceFile, String destDir) throws IOException {
        copyFile2Dir(sourceFile, new File(destDir));
    }

    public static void copyFile2Dir(File source, String destDir) throws IOException {
        copyFile2Dir(source, new File(destDir));
    }

    public static void copyFile2Dir(String sourceFile, File dest) throws IOException {
        copyFile2Dir(new File(sourceFile), dest);
    }

    public static void copyFile2Dir(File source, File dest) throws IOException {
        if (dest.exists() && !dest.isDirectory()) {
            throw new IOException("Target " + dest.getName() + " is not a directory");
        }
        copyFile(source, new File(dest, source.getName()));
    }

    public static void deleteFile(String fileName) throws IOException {
        deleteFile(new File(fileName));
    }

    public static void deleteFile(File file) throws IOException {
        if (!file.delete()) {
            throw new IOException("Cannot delete file " + file.getPath());
        }
    }

    public static void deleteDirTree(String rootName) throws IOException {
        deleteDirTree(new File(rootName));
    }

    public static void deleteDirTree(File root) throws IOException {
        if (!root.exists()) return;
        emptyDirectory(root);
        if (!root.delete()) {
            throw new IOException("Cannot delete directory " + root.getPath());
        }
    }

    public static void emptyDirectory(String directory) throws IOException {
        emptyDirectory(new File(directory));
    }

    public static void emptyDirectory(File directory) throws IOException {
        File[] list = directory.listFiles();
        if (list == null) {
            throw new IOException("Cannot empty directory " + directory.getPath());
        }
        for (int i = 0; i < list.length; i++) {
            if (list[i].isFile()) deleteFile(list[i]); else deleteDirTree(list[i]);
        }
    }

    public static void moveFile(String sourcePath, String destPath) throws IOException {
        moveFile(new File(sourcePath), new File(destPath));
    }

    public static void moveFile(File from, File to) throws IOException {
        if (!from.exists() || from.getCanonicalFile().compareTo(to.getCanonicalFile()) == 0) return;
        if (to.exists()) to.delete();
        if (to.exists()) throw new IOException("cannot move file '" + from + "' to '" + to + "'");
        if (!from.renameTo(to)) {
            copyFile(from, to);
            if (!to.exists()) throw new IOException("cannot move file '" + from + "' to '" + to + "'");
            from.delete();
        }
    }

    public static void moveFile2Dir(String fromFile, String destDir) throws IOException {
        moveFile2Dir(new File(fromFile), new File(destDir));
    }

    public static void moveFile2Dir(File from, File dest) throws IOException {
        if (dest.exists() && !dest.isDirectory()) {
            throw new IOException("Target " + dest.getName() + " is not a directory");
        }
        moveFile(from, new File(dest, from.getName()));
    }

    public static void moveFile2Dir(File from, String dest) throws IOException {
        moveFile2Dir(from, new File(dest));
    }

    public static String loadTextFile(String filename) throws IOException {
        return loadTextFile(filename, null);
    }

    public static String loadTextFile(String filename, String encoding) throws IOException {
        return loadTextFile(new File(filename), encoding);
    }

    public static String loadTextFile(File file) throws IOException {
        return loadTextFile(file, null);
    }

    public static String loadTextFile(URL url) throws IOException {
        return loadTextFile(url.openStream());
    }

    public static String loadTextFile(InputStream in) throws IOException {
        return loadTextFile(in, null);
    }

    public static String loadTextFile(File file, String encoding) throws IOException {
        FileInputStream istream = new FileInputStream(file);
        return loadTextFile(istream, encoding);
    }

    public static String loadTextFile(InputStream in, String encoding) throws IOException {
        java.io.Reader ir;
        if (encoding == null) ir = new java.io.InputStreamReader(in); else ir = new java.io.InputStreamReader(in, encoding);
        int len = 0;
        char buffer[] = new char[BUFFER_SIZE];
        try {
            StringBuffer sb = new StringBuffer((int) in.available());
            while ((len = ir.read(buffer, 0, BUFFER_SIZE)) != -1) {
                sb.append(buffer, 0, len);
            }
            return sb.toString();
        } finally {
            forceClose(in);
        }
    }

    public static String loadTextResource(Class c, String path) throws IOException {
        InputStream istream = c.getResourceAsStream(path);
        java.io.Reader ir = new java.io.InputStreamReader(istream);
        int len = 0;
        char buffer[] = new char[BUFFER_SIZE];
        try {
            StringBuffer sb = new StringBuffer((int) istream.available());
            while ((len = ir.read(buffer, 0, BUFFER_SIZE)) != -1) {
                sb.append(buffer, 0, len);
            }
            return sb.toString();
        } finally {
            forceClose(istream);
        }
    }

    public static byte[] loadBinaryFile(String filename) throws IOException {
        return loadBinaryFile(new File(filename));
    }

    public static byte[] loadBinaryFile(File filename) throws IOException {
        FileInputStream istream = new FileInputStream(filename);
        return loadBinaryFile(istream);
    }

    public static byte[] loadBinaryFile(URL url) throws IOException {
        return loadBinaryFile(url.openStream());
    }

    public static byte[] loadBinaryFile(InputStream in) throws IOException {
        try {
            int inBytes = in.available();
            byte[] inBuf = new byte[inBytes];
            in.read(inBuf, 0, inBytes);
            return inBuf;
        } finally {
            forceClose(in);
        }
    }

    public static byte[] loadBinaryResource(Class c, String path) throws IOException {
        InputStream istream = c.getResourceAsStream(path);
        try {
            int inBytes = istream.available();
            byte[] inBuf = new byte[inBytes];
            istream.read(inBuf, 0, inBytes);
            return inBuf;
        } finally {
            forceClose(istream);
        }
    }

    public static void saveTextFile(String filename, String content, boolean append) throws IOException {
        saveTextFile(new File(filename), content, append);
    }

    public static void saveTextFile(File file, String content, boolean append) throws IOException {
        saveTextFile(file, content, null, append);
    }

    public static void saveTextFile(File file, String content, String encoding, boolean append) throws IOException {
        saveTextFile(file.getPath(), content, encoding, append);
    }

    public static void saveTextFile(String filename, String content, String encoding, boolean append) throws IOException {
        FileOutputStream fos = new FileOutputStream(filename, append);
        java.io.Writer ow;
        if (encoding == null) ow = new java.io.OutputStreamWriter(fos); else ow = new java.io.OutputStreamWriter(fos, encoding);
        try {
            ow.write(content);
            ow.flush();
        } finally {
            forceClose(fos);
        }
        return;
    }

    public static void saveBinaryFile(File file, byte[] content, boolean append) throws IOException {
        saveBinaryFile(file.getPath(), content, append);
    }

    public static void saveBinaryFile(String filename, byte[] content, boolean append) throws IOException {
        FileOutputStream fos = new FileOutputStream(filename, append);
        try {
            fos.write(content);
            fos.flush();
        } finally {
            forceClose(fos);
        }
        return;
    }

    public static String normalizeFile(String filePath) {
        if (K.EMPTY.equals(filePath)) return filePath;
        String retVal = filePath.replace('\\', '/');
        while (retVal.indexOf("//") >= 0) retVal = retVal.replace("//", "/");
        if (retVal.charAt(retVal.length() - 1) == '/') retVal = retVal.substring(0, retVal.length() - 1);
        return retVal;
    }

    public static String normalizeDir(String dirPath) {
        return normalizeFile(dirPath).concat(K.SLASH);
    }

    public static String composeDirFile(String dirPath, String filePath) {
        return normalizeFile(normalizeDir(dirPath).concat(filePath));
    }
}
