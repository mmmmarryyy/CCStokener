package com.nexirius.util;

import java.io.*;

/**
 * The XFile class implements a File with additional (extra) functionality
 */
public class XFile extends File {

    boolean m_append_separator = true;

    public XFile(File dir, String name) {
        super(dir, name);
    }

    public XFile(String path) {
        super(path);
    }

    public XFile(String path, String name) {
        super(path, name);
    }

    public long length() {
        long ret = 0;
        if (isFile()) {
            try {
                RandomAccessFile r = new RandomAccessFile(this, "r");
                ret = r.length();
                r.close();
            } catch (Exception ex) {
            }
        }
        return ret;
    }

    /**
     * by default File.separator character is added to the end of
     * directory names (not for method directoryList())
     */
    public void setAppendSeparators(boolean on) {
        m_append_separator = on;
    }

    /**
     * deletes the file or the directory (recursively)
     *
     * @return true if the file is successfully deleted; false otherwise
     */
    public boolean delete() {
        boolean ret = true;
        if (isDirectory()) {
            StringVector sv = getFiles(true);
            String s;
            for (s = sv.firstItem(); s != null; s = sv.nextItem()) {
                File f = new File(this, s);
                ret = ret && f.delete();
            }
            sv = getDirectories(true);
            for (s = sv.lastItem(); s != null; s = sv.previousItem()) {
                File f = new File(this, s);
                ret = ret && f.delete();
            }
        }
        ret = ret && super.delete();
        return ret;
    }

    /**
     * get the names of all the files in the directory which are no directories (not recursive)
     */
    public String[] fileList() {
        return list(new FileOnly());
    }

    /**
     * get the names of all the subdirectories in the directory (not recursive)
     */
    public String[] directoryList() {
        return list(new DirectoryOnly());
    }

    /**
     * get files and or directories in a sorted list
     */
    private StringVector getAllFiles(boolean recursively, String insert, boolean with_directories, boolean with_files, String match_pattern) {
        StringVector ret = new StringVector();
        StringVector dirs = new StringVector();
        String s;
        if (!isDirectory()) {
            return ret;
        }
        dirs.sortInsert(directoryList());
        for (s = dirs.firstItem(); s != null; s = dirs.nextItem()) {
            String directory = insert + s;
            if (with_directories) {
                if (m_append_separator) {
                    ret.append(directory + File.separator);
                } else {
                    ret.append(directory);
                }
            }
            if (recursively) {
                XFile f = new XFile(this, s);
                ret.append(f.getAllFiles(true, directory + File.separator, with_directories, with_files, match_pattern));
            }
        }
        if (with_files) {
            StringVector files = new StringVector();
            if (match_pattern == null) {
                files.sortInsert(fileList());
            } else {
                files.sortInsert(fileList(), match_pattern);
            }
            for (s = files.firstItem(); s != null; s = files.nextItem()) {
                ret.append(insert + s);
            }
        }
        return ret;
    }

    public int writeDirectory(java.io.OutputStream out) throws Exception {
        int ret = 0;
        int i;
        String dirs[] = directoryList();
        for (i = 0; i < dirs.length; ++i) {
            XFile f = new XFile(this, dirs[i]);
            TextToken t = new TextToken(dirs[i]);
            t.writeTo(out);
            out.write('\n');
            out.write('{');
            out.write('\n');
            ret += f.writeDirectory(out);
            out.write('}');
            out.write('\n');
        }
        String files[] = fileList();
        for (i = 0; i < files.length; ++i) {
            ++ret;
            XFile f = new XFile(this, files[i]);
            TextToken t = new TextToken(files[i]);
            t.writeTo(out);
            out.write(' ');
            t = new TextToken(f.lastModified());
            t.writeTo(out);
            out.write(' ');
            t = new TextToken(f.length());
            t.writeTo(out);
            out.write('\n');
        }
        return ret;
    }

    /**
     * get files and directories in a sorted list
     */
    public StringVector getAllFiles(boolean recursively) {
        return getAllFiles(recursively, "", true, true, null);
    }

    /**
     * get files in a sorted list
     */
    public StringVector getFiles(boolean recursively) {
        return getAllFiles(recursively, "", false, true, null);
    }

    /**
     * get files in a sorted list
     */
    public StringVector getFiles(boolean recursively, String match_pattern) {
        return getAllFiles(recursively, "", false, true, match_pattern);
    }

    /**
     * get directories in a sorted list
     */
    public StringVector getDirectories(boolean recursively) {
        return getAllFiles(recursively, "", true, false, null);
    }

    /**
     * go through all directories and subdirectories recursively and call the appropriate method on XFileJob
     * @param job
     */
    public void traverseFiles(XFileJob job) {
        if (job.isInterrupted()) {
            return;
        }
        StringVector directories = getDirectories(false);
        if (job.isInterrupted()) {
            return;
        }
        for (String dir = directories.firstItem(); dir != null; dir = directories.nextItem()) {
            if (job.isInterrupted()) {
                return;
            }
            if (job.workDirectory(this, dir)) {
                XFile xDir = new XFile(getPath(), dir);
                xDir.traverseFiles(job);
            }
        }
        if (job.isInterrupted()) {
            return;
        }
        StringVector files = getFiles(false);
        if (job.isInterrupted()) {
            return;
        }
        for (String file = files.firstItem(); file != null; file = files.nextItem()) {
            if (job.isInterrupted()) {
                return;
            }
            job.work(this, file);
        }
    }

    /**
     * create a copy of the given file
     *
     * @param update if true, only copy to target file if it does not exist or if the
     *               target file is older than the source file.
     */
    public boolean createFrom(File source, boolean update) throws Exception {
        if (update && exists()) {
            if (lastModified() >= source.lastModified()) {
                return false;
            }
        }
        if (source.getAbsolutePath().equals(getAbsolutePath())) {
            throw new Exception("Can't copy'" + getAbsolutePath() + "' to itself");
        }
        if (!source.exists()) {
            throw new Exception(source.getPath() + " does not exist");
        }
        InputStream fs = new FileInputStream(source);
        createFrom(fs);
        return true;
    }

    public void createFrom(InputStream fs) throws IOException {
        OutputStream ft = null;
        int bufferSize = 512;
        byte b[] = new byte[bufferSize];
        int number;
        fs = new BufferedInputStream(fs, bufferSize);
        ft = new BufferedOutputStream(new FileOutputStream(this), bufferSize);
        while ((number = fs.read(b)) > 0) {
            ft.write(b, 0, number);
        }
        fs.close();
        ft.close();
    }

    public BufferedInputStream getBufferedInputStream() throws Exception {
        if (exists()) {
            return new BufferedInputStream(new FileInputStream(this));
        }
        String urlname = getPath().replace(File.separatorChar, '/');
        if (urlname.startsWith("./")) {
            urlname = urlname.substring(2);
        }
        InputStream in = ClassLoader.getSystemResourceAsStream(urlname);
        if (in == null) {
            java.net.URL url = getClass().getClassLoader().getResource(urlname);
            if (url != null) {
                in = url.openStream();
            }
        }
        if (in == null) {
            throw new IOException("Can't access resource: " + urlname);
        }
        return new BufferedInputStream(in);
    }

    /**
     * opens the file, reads all lines (ending with \n ignoring \r)
     * and returns them in a StringVector
     */
    public StringVector getTextLines() throws Exception {
        StringVector ret = new StringVector();
        getTextLines(0, -1, ret);
        return ret;
    }

    /**
     * opens the file, reads a maximum number of lines (ending with \n ignoring \r)
     * and returns them in a StringVector
     *
     * @param beginAtByte firstz byte to read
     * @param maxLines maximum number of lines to read (-1 indicates all lines)
     * @param lines this list is filled (append) with the lines from the file
     * @return the current byte position at the end of reading or -1 if the end of file has been reached
     * @throws Exception
     */
    public long getTextLines(long beginAtByte, int maxLines, StringVector lines) throws Exception {
        BufferedInputStream in = getBufferedInputStream();
        StringBuffer buf = new StringBuffer();
        int c;
        boolean cont = true;
        long ret = beginAtByte;
        if (beginAtByte > 0) {
            in.skip(beginAtByte);
        }
        while (cont) {
            c = in.read();
            ++ret;
            switch(c) {
                case '\r':
                    break;
                case '\n':
                    lines.append(buf.toString());
                    buf = new StringBuffer();
                    if (maxLines > 0 && lines.size() >= maxLines) {
                        cont = false;
                    }
                    break;
                case -1:
                    cont = false;
                    ret = -1;
                    break;
                default:
                    buf.append((char) c);
            }
        }
        if (buf.length() > 0) {
            lines.append(buf.toString());
        }
        in.close();
        return ret;
    }

    /**
     * opens the file, writes the bytes of the string and closes the file
     */
    public void writeText(String s) throws Exception {
        FileOutputStream ft = new FileOutputStream(this);
        ft.write(s.getBytes());
        ft.close();
    }

    public void writeTextLines(StringVector sv) throws Exception {
        writeTextLines(sv, false);
    }

    /**
     * opens the file, writes the bytes of the strings (plus a '\n') and closes the file
     */
    public void writeTextLines(StringVector sv, boolean dosFormat) throws Exception {
        FileOutputStream ft = new FileOutputStream(this);
        for (String s = sv.firstItem(); s != null; s = sv.nextItem()) {
            ft.write(s.getBytes());
            if (dosFormat) {
                ft.write((int) '\r');
            }
            ft.write((int) '\n');
        }
        ft.close();
    }

    public byte[] getBytes() throws Exception {
        BufferedInputStream in = getBufferedInputStream();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int c = in.read();
        while (c >= 0) {
            out.write((byte) c);
            c = in.read();
        }
        in.close();
        out.close();
        return out.toByteArray();
    }

    public static void main(String argv[]) throws Exception {
        (new XFile("C:\\marcel\\projects\\src\\jshell")).writeDirectory(System.out);
    }

    private static class FileOnly implements FilenameFilter {

        public boolean accept(File dir, String name) {
            File f = new File(dir, name);
            return !f.isDirectory();
        }
    }

    private static class DirectoryOnly implements FilenameFilter {

        public boolean accept(File dir, String name) {
            File f = new File(dir, name);
            return f.isDirectory();
        }
    }
}
