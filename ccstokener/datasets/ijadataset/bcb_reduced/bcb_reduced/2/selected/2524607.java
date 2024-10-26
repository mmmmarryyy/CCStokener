package gleam.executive.util;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.net.*;
import java.util.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * File utilities class.
 */
public final class FileUtil {

    private static Log log = LogFactory.getLog(FileUtil.class);

    static final String ROOT_DIR = new StringBuffer(System.getProperty("user.home")).append("/safe/temp").toString();

    /**
   * Returns current working folder string.
   * 
   * @return current woking folder, if it does not exist - create it
   */
    public static String getWorkingDir() {
        File tmpDir = getWorkingDirFile();
        if (!tmpDir.exists() || !tmpDir.isDirectory()) {
            tmpDir.mkdirs();
        }
        return tmpDir.getPath();
    }

    /**
   * Returns current working folder.
   * 
   * @return File for current woking folder
   */
    public static File getWorkingDirFile() {
        return new File(ROOT_DIR);
    }

    /**
   * Returns file size.
   * 
   * @param fileName
   *          file name
   * 
   * @return file size
   */
    public static long getFileSize(String fileName) {
        return new File(fileName).length();
    }

    /**
   * Returns file size.
   * 
   * @param file
   *          file
   * 
   * @return file size
   */
    public static long getFileSize(File file) {
        return file.length();
    }

    /**
   * Checks if two files are poiniting to the same file.
   * 
   * @param file1
   *          first file
   * @param file2
   *          second file
   * 
   * @return true if files are pointing to the same file
   */
    public static boolean equals(String file1, String file2) {
        return equals(new File(file1), new File(file2));
    }

    /**
   * Checks if two files are poiniting to the same file.
   * 
   * @param file1
   *          first file
   * @param file2
   *          second file
   * 
   * @return true if files are pointing to the same file
   */
    public static boolean equals(File file1, File file2) {
        try {
            file1 = file1.getCanonicalFile();
            file2 = file2.getCanonicalFile();
        } catch (IOException e) {
            return false;
        }
        return file1.equals(file2);
    }

    /**
   * Creates all folders.
   * 
   * @param dirs
   *          dirs
   * 
   * @return true if creating was successful, false otherwise
   */
    public static boolean mkdirs(String dirs) {
        return new File(dirs).mkdirs();
    }

    /**
   * Creates all folders.
   * 
   * @param dirs
   *          dirs
   * 
   * @return true if creating was successful, false otherwise
   */
    public static boolean mkdirs(File dirs) {
        return dirs.mkdirs();
    }

    /**
   * Creates a folder.
   * 
   * @param dir
   *          dir
   * 
   * @return true if creating was successful, false otherwise
   */
    public static boolean mkdir(String dir) {
        return new File(dir).mkdir();
    }

    /**
   * Creates a folders.
   * 
   * @param dir
   *          dir
   * 
   * @return true if creating was successful, false otherwise
   */
    public static boolean mkdir(File dir) {
        return dir.mkdir();
    }

    /**
   * Buffer size (32KB) for file manipulation methods.
   */
    public static final int FILE_BUFFER_SIZE = 32 * 1024;

    /**
   * Copies and overwrites a file to another file or folder.
   * 
   * @param fileIn
   *          input file
   * @param fileOut
   *          output file
   * 
   * @return true if operation was successful, false otherwise
   * @see #copy(File, File, int, boolean)
   */
    public static boolean copy(String fileIn, String fileOut) throws IOException {
        return copy(new File(fileIn), new File(fileOut), FILE_BUFFER_SIZE, true);
    }

    /**
   * Copies a file to another file or folder.
   * 
   * @param fileIn
   *          input file
   * @param fileOut
   *          output file
   * 
   * @return true if operation was successful, false otherwise
   * @see #copy(File, File, int, boolean)
   */
    public static boolean copySafe(String fileIn, String fileOut) throws IOException {
        return copy(new File(fileIn), new File(fileOut), FILE_BUFFER_SIZE, false);
    }

    /**
   * Copies and overwrites a file to another file or folder with specified
   * buffer size.
   * 
   * @param fileIn
   *          input file
   * @param fileOut
   *          output file
   * @param bufsize
   *          size of the buffer used for copying
   * 
   * @return true if operation was successful, false otherwise
   * @see #copy(File, File, int, boolean)
   */
    public static boolean copy(String fileIn, String fileOut, int bufsize) throws IOException {
        return copy(new File(fileIn), new File(fileOut), bufsize, true);
    }

    /**
   * Copies a file to another file or folder with specified buffer size.
   * 
   * @param fileIn
   *          input file
   * @param fileOut
   *          output file
   * @param bufsize
   *          size of the buffer used for copying
   * 
   * @return true if operation was successful, false otherwise
   * @see #copy(File, File, int, boolean)
   */
    public static boolean copySafe(String fileIn, String fileOut, int bufsize) throws IOException {
        return copy(new File(fileIn), new File(fileOut), bufsize, false);
    }

    /**
   * Copies and overwrites a file to another file or folder.
   * 
   * @param fileIn
   *          input file
   * @param fileOut
   *          output file
   * 
   * @return true if operation was successful, false otherwise
   * @see #copy(File, File, int, boolean)
   */
    public static boolean copy(File fileIn, File fileOut) throws IOException {
        return copy(fileIn, fileOut, FILE_BUFFER_SIZE, true);
    }

    /**
   * Copies a file to another file or folder.
   * 
   * @param fileIn
   *          input file
   * @param fileOut
   *          output file
   * 
   * @return true if operation was successful, false otherwise
   * @see #copy(File, File, int, boolean)
   */
    public static boolean copySafe(File fileIn, File fileOut) throws IOException {
        return copy(fileIn, fileOut, FILE_BUFFER_SIZE, false);
    }

    /**
   * Copies and overwrites a file to another file or folder with specified
   * buffer size.
   * 
   * @param fileIn
   *          input file
   * @param fileOut
   *          output file
   * @param bufsize
   *          size of the buffer used for copying
   * 
   * @return true if operation was successful, false otherwise
   * @see #copy(File, File, int, boolean)
   */
    public static boolean copy(File fileIn, File fileOut, int bufsize) throws IOException {
        return copy(fileIn, fileOut, bufsize, true);
    }

    /**
   * Copies a file to another file or folder with specified buffer size.
   * 
   * @param fileIn
   *          input file
   * @param fileOut
   *          output file
   * @param bufsize
   *          size of the buffer used for copying
   * 
   * @return true if operation was successful, false otherwise
   * @see #copy(File, File, int, boolean)
   */
    public static boolean copySafe(File fileIn, File fileOut, int bufsize) throws IOException {
        return copy(fileIn, fileOut, bufsize, false);
    }

    /**
   * Copies a file to another file or folder with specified buffer size and
   * overwrite flag.
   * 
   * @param fileIn
   *          input file
   * @param fileOut
   *          output file
   * @param bufsize
   *          size of the buffer used for copying
   * @param overwrite
   *          should existing destination be overwritten
   * 
   * @return true if operation was successful, false otherwise
   * @see #copy(File, File, int, boolean)
   */
    public static boolean copy(String fileIn, String fileOut, int bufsize, boolean overwrite) throws IOException {
        return copy(new File(fileIn), new File(fileOut), bufsize, overwrite);
    }

    /**
   * Copies a file to another file or folder with specified buffer size and
   * overwrite flag. If source doesn't exist, operation fails. If source isn't
   * file, operation fails. Destination may be a folder, in that case file will
   * be copied in destination folder. If overwrite is on and source file points
   * to the same file as edstination, operation is successful.
   * 
   * @param fileIn
   *          input file
   * @param fileOut
   *          output file
   * @param bufsize
   *          size of the buffer used for copying
   * @param overwrite
   *          should existing destination be overwritten
   * 
   * @return true if operation was successful, false otherwise
   */
    public static boolean copy(File fileIn, File fileOut, int bufsize, boolean overwrite) throws IOException {
        if (fileIn.exists() == false) {
            return false;
        }
        if (fileIn.isFile() == false) {
            return false;
        }
        if (fileOut.isDirectory() == true) {
            fileOut = new File(fileOut.getPath() + "/" + fileIn.getName());
        }
        if (overwrite == false) {
            if (fileOut.exists() == true) {
                return false;
            }
        } else {
            if (fileOut.exists()) {
                try {
                    if (fileIn.getCanonicalFile().equals(fileOut.getCanonicalFile()) == true) {
                        return true;
                    }
                } catch (IOException ioex) {
                    return false;
                }
            }
        }
        return copyFile(fileIn, fileOut, bufsize);
    }

    /**
   * Copies one file to another withut any checkings.
   * 
   * @param fileIn
   *          source
   * @param fileOut
   *          destination
   * 
   * @return true if operation was successful, false otherwise
   */
    public static boolean copyFile(String fileIn, String fileOut) throws IOException {
        return copyFile(new File(fileIn), new File(fileOut), FILE_BUFFER_SIZE);
    }

    /**
   * Copies one file to another withut any checkings.
   * 
   * @param fileIn
   *          source
   * @param fileOut
   *          destination
   * 
   * @return true if operation was successful, false otherwise
   */
    public static boolean copyFile(File fileIn, File fileOut) throws IOException {
        return copyFile(fileIn, fileOut, FILE_BUFFER_SIZE);
    }

    /**
   * Copies one file to another withut any checkings.
   * 
   * @param fileIn
   *          source
   * @param fileOut
   *          destination
   * @param bufsize
   *          buffer size
   * 
   * @return true if operation was successful, false otherwise
   */
    public static boolean copyFile(String fileIn, String fileOut, int bufsize) throws IOException {
        return copyFile(new File(fileIn), new File(fileOut), bufsize);
    }

    /**
   * Copies one file to another withut any checkings.
   * 
   * @param fileIn
   *          source
   * @param fileOut
   *          destination
   * @param bufsize
   *          buffer size
   * 
   * @return true if operation was successful, false otherwise
   */
    public static boolean copyFile(File fileIn, File fileOut, int bufsize) throws IOException {
        FileInputStream in = null;
        FileOutputStream out = null;
        boolean result = false;
        try {
            in = new FileInputStream(fileIn);
            out = new FileOutputStream(fileOut);
            copyPipe(in, out, bufsize);
            result = true;
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ioex) {
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ioex) {
                }
            }
        }
        return result;
    }

    public static boolean move(String fileNameIn, String fileNameOut) {
        return move(new File(fileNameIn), new File(fileNameOut), true);
    }

    public static boolean moveSafe(String fileNameIn, String fileNameOut) {
        return move(new File(fileNameIn), new File(fileNameOut), false);
    }

    public static boolean move(File fileIn, File fileOut) {
        return move(fileIn, fileOut, true);
    }

    public static boolean moveSafe(File fileIn, File fileOut) {
        return move(fileIn, fileOut, false);
    }

    /**
   * Moves one file to another file or folder with overwrite flag.
   * 
   * @param fileNameIn
   * @param fileNameOut
   * @param overwrite
   *          overwrite flag
   * 
   * @return true if successful, false otherwise
   * @see #move(File, File, boolean)
   */
    public static boolean move(String fileNameIn, String fileNameOut, boolean overwrite) {
        return move(new File(fileNameIn), new File(fileNameOut), overwrite);
    }

    /**
   * Moves one file to another file or folder with overwrite flag. If source
   * doesn't exist, oepration fails. If source is not file, oeration fails. If
   * destionation is folder, file will be moved to a file with the same name in
   * that folder.
   * 
   * @param fileIn
   *          source
   * @param fileOut
   *          destination
   * @param overwrite
   *          overwrite flag
   * 
   * @return true if successful, false otherwise
   */
    public static boolean move(File fileIn, File fileOut, boolean overwrite) {
        if (fileIn.exists() == false) {
            return false;
        }
        if (fileIn.isFile() == false) {
            return false;
        }
        if (fileOut.isDirectory() == true) {
            fileOut = new File(fileOut.getPath() + "/" + fileIn.getName());
        }
        if (overwrite == false) {
            if (fileOut.exists() == true) {
                return false;
            }
        } else {
            if (fileOut.exists()) {
                try {
                    if (fileIn.getCanonicalFile().equals(fileOut.getCanonicalFile()) == true) {
                        return true;
                    } else {
                        fileOut.delete();
                    }
                } catch (IOException ioex) {
                    return false;
                }
            }
        }
        return fileIn.renameTo(fileOut);
    }

    /**
   * Moves (renames) a file without any check.
   * 
   * @param src
   *          source file
   * @param dest
   *          destination file
   * 
   * @return true if sucess, false otherwise
   */
    public static boolean moveFile(String src, String dest) {
        return new File(src).renameTo(new File(dest));
    }

    /**
   * Moves (renames) a file without any check.
   * 
   * @param src
   *          source file
   * @param dest
   *          destination file
   * 
   * @return true if sucess, false otherwise
   */
    public static boolean moveFile(File src, File dest) {
        return src.renameTo(dest);
    }

    public static boolean moveDir(String fileIn, String fileOut) {
        return moveDir(new File(fileIn), new File(fileOut));
    }

    /**
   * Moves (renames) one folder to another. If source doesn't exist, operation
   * fails. If source equals to destination, operaiton is successful. If source
   * isn't a folder, operation fails. If destinatione exist, operation fails.
   * 
   * @param fileIn
   *          source folder
   * @param fileOut
   *          destination folder
   * 
   * @return true if success, false otherwise
   */
    public static boolean moveDir(File fileIn, File fileOut) {
        if (fileIn.exists() == false) {
            return false;
        }
        if (fileIn.isDirectory() == false) {
            return false;
        }
        if (fileOut.exists() == true) {
            try {
                if (fileIn.getCanonicalFile().equals(fileOut.getCanonicalFile()) == true) {
                    return true;
                } else {
                    return false;
                }
            } catch (IOException ioex) {
                return false;
            }
        }
        return fileIn.renameTo(fileOut);
    }

    public static boolean copyDir(String srcDir, String dstDir) throws IOException {
        return copyDir(new File(srcDir), new File(dstDir));
    }

    /**
   * Copies all files under source folder to destination. If destinatio does not
   * exist, it will be created.
   * 
   * @param srcDir
   *          source
   * @param dstDir
   *          destination
   * 
   * @return true if success, false otherwise
   */
    public static boolean copyDir(File srcDir, File dstDir) throws IOException {
        if (srcDir.isDirectory()) {
            log.debug(srcDir.getAbsolutePath() + " is directory");
            if (!dstDir.exists()) {
                log.debug(dstDir.getAbsolutePath() + " does not exist, so create it");
                if (!dstDir.mkdirs()) {
                    throw new IOException("Can't create " + dstDir.getName());
                }
            }
            String[] files = srcDir.list();
            log.debug("Copy " + files.length + " files.");
            for (int i = 0; i < files.length; i++) {
                if (copyDir(new File(srcDir, files[i]), new File(dstDir, files[i])) == false) {
                    return false;
                }
            }
            return true;
        }
        log.debug(srcDir.getAbsolutePath() + " is NOT directory");
        return copyFile(srcDir, dstDir);
    }

    /**
   * Deletes files or empty folders, identical to File.delete().
   * 
   * @param fileName
   *          name of file to delete
   */
    public static boolean delete(String fileName) {
        return delete(new File(fileName));
    }

    /**
   * Deletes files or empty folders, identical to File.delete().
   * 
   * @param fileIn
   *          file to delete
   */
    public static boolean delete(File fileIn) {
        return fileIn.delete();
    }

    /**
   * Deletes files and/or complete tree recursively.
   * 
   * @param pathName
   *          folder name to delete
   */
    public static boolean deleteDir(String pathName) {
        return deleteDir(new File(pathName));
    }

    /**
   * Deletes files with name=fileName and located in folderId.
   * 
   * @param pathName
   *          folder name where are files that need to be deleted
   */
    public static boolean deleteDocs(String folderId, String[] docNames) {
        boolean deleted = true;
        String name = null;
        if (docNames != null && (docNames.length > 0)) {
            for (int i = 0; i < docNames.length; i++) {
                name = docNames[i];
                if (name != null && !"".equals(name)) {
                    deleted = FileUtil.deleteDir(new StringBuffer(folderId).append("/").append(name).toString());
                    if (deleted == false) break;
                }
            }
        }
        return deleted;
    }

    /**
   * Deletes files and/or complete tree recursively. Returns true if all
   * deletions were successful. If a deletion fails, the method stops attempting
   * to delete and returns false.
   * 
   * @param path
   *          folder to delete
   * 
   * @return <code>true</code> if success, <code>false</code> otherwise
   */
    public static boolean deleteDir(File path) {
        if (path.isDirectory()) {
            File[] files = path.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (deleteDir(files[i]) == false) {
                    return false;
                }
            }
        }
        return path.delete();
    }

    /**
   * Buffer size (32KB) for file string methods.
   */
    public static final int STRING_BUFFER_SIZE = 32 * 1024;

    /**
   * Reads file's content into a String. Implicitly assumes that the file is in
   * the default encoding.
   * 
   * @param fileName
   *          name of the file to read from
   * 
   * @return string with file content or null
   * @exception IOException
   */
    public static String readString(String fileName) throws IOException {
        return readString(new File(fileName), STRING_BUFFER_SIZE);
    }

    /**
   * Reads file's content into a String. Implicitly assumes that the file is in
   * the default encoding.
   * 
   * @param fileName
   *          name of the file to read from
   * @param bufferSize
   *          buffer size
   * 
   * @return string with file content or null
   * @exception IOException
   */
    public static String readString(String fileName, int bufferSize) throws IOException {
        return readString(new File(fileName), bufferSize);
    }

    /**
   * Reads file's content into a String. Implicitly assumes that the file is in
   * the default encoding.
   * 
   * @param file
   *          file to read
   * 
   * @return string with file content or null
   * @exception IOException
   */
    public static String readString(File file) throws IOException {
        return readString(file, STRING_BUFFER_SIZE);
    }

    /**
   * Reads file's content into a String. Implicitly assumes that the file is in
   * the default encoding.
   * 
   * @param file
   *          file to read
   * @param bufferSize
   *          buffer size
   * 
   * @return string with file content or null
   * @exception IOException
   */
    public static String readString(File file, int bufferSize) throws IOException {
        long fileLen = file.length();
        if (fileLen <= 0L) {
            if (file.exists() == true) {
                return "";
            }
            return null;
        }
        if (fileLen > Integer.MAX_VALUE) {
            throw new IOException("File too big for loading into a String!");
        }
        FileReader fr = null;
        BufferedReader brin = null;
        char[] buf = null;
        try {
            fr = new FileReader(file);
            brin = new BufferedReader(fr, bufferSize);
            int length = (int) fileLen;
            buf = new char[length];
            brin.read(buf, 0, length);
        } finally {
            if (brin != null) {
                brin.close();
                fr = null;
            }
            if (fr != null) {
                fr.close();
            }
        }
        return new String(buf);
    }

    /**
   * This method get content of a file from file system where id is the absolute
   * location of file on fs. If file with specified id doesn't exist, new file
   * is created.
   * 
   * @param id
   * @param bufferSize
   * @return
   */
    public static String getContents(String id, int bufferSize) {
        StringBuffer contents = new StringBuffer();
        File aFile = new File(id);
        BufferedReader input = null;
        try {
            input = new BufferedReader(new FileReader(aFile));
            String line = null;
            while ((line = input.readLine()) != null) {
                contents.append(line);
                contents.append(System.getProperty("line.separator"));
            }
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (input != null) {
                    input.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return contents.toString();
    }

    /**
   * Writes string to a file. Implicitly assumes that the file will be written
   * the default encoding.
   * 
   * @param fileName
   *          name of the destination file
   * @param s
   *          source string
   * 
   * @exception IOException
   */
    public static void writeString(String fileName, String s) throws IOException {
        writeString(new File(fileName), s, STRING_BUFFER_SIZE);
    }

    /**
   * Writes string to a file. Implicitly assumes that the file will be written
   * the default encoding.
   * 
   * @param fileName
   *          name of the destination file
   * @param s
   *          source string
   * @param bufferSize
   *          buffer size
   * 
   * @exception IOException
   */
    public static void writeString(String fileName, String s, int bufferSize) throws IOException {
        writeString(new File(fileName), s, bufferSize);
    }

    /**
   * Writes string to a file. Implicitly assumes that the file will be written
   * the default encoding.
   * 
   * @param file
   *          destination file
   * @param s
   *          source string
   * 
   * @exception IOException
   */
    public static void writeString(File file, String s) throws IOException {
        writeString(file, s, STRING_BUFFER_SIZE);
    }

    public static void writeString(File file, String s, int bufferSize) throws IOException {
        writeString(file, s, bufferSize, false);
    }

    public static void writeString(File file, String s, boolean append) throws IOException {
        writeString(file, s, STRING_BUFFER_SIZE, append);
    }

    /**
   * Writes string to a file. Implicitly assumes that the file will be written
   * the default encoding.
   * 
   * @param file
   *          destination file
   * @param s
   *          source string
   * @param bufferSize
   *          buffer size
   * 
   * @exception IOException
   */
    public static void writeString(File file, String s, int bufferSize, boolean append) throws IOException {
        FileWriter fw = null;
        BufferedWriter out = null;
        if (s == null) {
            return;
        }
        try {
            fw = new FileWriter(file, append);
            out = new BufferedWriter(fw, bufferSize);
            out.write(s);
        } finally {
            if (out != null) {
                out.close();
                fw = null;
            }
            if (fw != null) {
                fw.close();
            }
        }
    }

    /**
   * Reads file's content into a String.
   * 
   * @param fileName
   *          source file name
   * @param encoding
   *          java encoding string
   * 
   * @return string with file content or null
   * @exception IOException
   */
    public static String readString(String fileName, String encoding) throws IOException {
        return readString(new File(fileName), STRING_BUFFER_SIZE, encoding);
    }

    /**
   * Reads file's content into a String.
   * 
   * @param fileName
   *          source file name
   * @param bufferSize
   *          buffer size
   * @param encoding
   *          java encoding string
   * 
   * @return string with file content or null
   * @exception IOException
   */
    public static String readString(String fileName, int bufferSize, String encoding) throws IOException {
        return readString(new File(fileName), bufferSize, encoding);
    }

    /**
   * Reads file's content into a String.
   * 
   * @param file
   *          source file
   * @param encoding
   *          java encoding string
   * 
   * @return string with file content or null
   * @exception IOException
   */
    public static String readString(File file, String encoding) throws IOException {
        return readString(file, STRING_BUFFER_SIZE, encoding);
    }

    /**
   * Reads file's content into a String. This is a bit different implementation
   * than other readString() method, since the number of characters in the file
   * is not known. This currently only affest the value of the maximum file
   * size.
   * 
   * @param file
   *          source file
   * @param bufferSize
   *          buffer size
   * @param encoding
   *          java encoding string
   * 
   * @return string with file content or null
   * @exception IOException
   */
    public static String readString(File file, int bufferSize, String encoding) throws IOException {
        long fileLen = file.length();
        if (fileLen <= 0L) {
            if (file.exists() == true) {
                return "";
            }
            return null;
        }
        if (fileLen > Integer.MAX_VALUE) {
            throw new IOException("File too big for loading into a String!");
        }
        FileInputStream fis = null;
        InputStreamReader isr = null;
        BufferedReader brin = null;
        int length = (int) fileLen;
        char[] buf = null;
        int realSize = 0;
        try {
            fis = new FileInputStream(file);
            isr = new InputStreamReader(fis, encoding);
            brin = new BufferedReader(isr, bufferSize);
            buf = new char[length];
            int c;
            while ((c = brin.read()) != -1) {
                buf[realSize] = (char) c;
                realSize++;
            }
        } finally {
            if (brin != null) {
                brin.close();
                isr = null;
                fis = null;
            }
            if (isr != null) {
                isr.close();
                fis = null;
            }
            if (fis != null) {
                fis.close();
            }
        }
        return new String(buf, 0, realSize);
    }

    /**
   * Writes string to a file.
   * 
   * @param fileName
   *          destination file name
   * @param s
   *          source string
   * @param encoding
   *          java encoding string
   * 
   * @exception IOException
   */
    public static void writeString(String fileName, String s, String encoding) throws IOException {
        writeString(new File(fileName), s, STRING_BUFFER_SIZE, encoding);
    }

    /**
   * Writes string to a file.
   * 
   * @param fileName
   *          destination file name
   * @param s
   *          source string
   * @param bufferSize
   *          buffer size
   * @param encoding
   *          java encoding string
   * 
   * @exception IOException
   */
    public static void writeString(String fileName, String s, int bufferSize, String encoding) throws IOException {
        writeString(new File(fileName), s, bufferSize, encoding);
    }

    /**
   * Writes string to a file.
   * 
   * @param file
   *          destination file
   * @param s
   *          source string
   * @param encoding
   *          java encoding string
   * 
   * @exception IOException
   */
    public static void writeString(File file, String s, String encoding) throws IOException {
        writeString(file, s, STRING_BUFFER_SIZE, encoding);
    }

    /**
   * Writes string to a file.
   * 
   * @param file
   *          destination file
   * @param s
   *          source string
   * @param bufferSize
   *          buffer size
   * @param encoding
   *          java encoding string
   * 
   * @exception IOException
   */
    public static void writeString(File file, String s, int bufferSize, String encoding) throws IOException {
        if (s == null) {
            return;
        }
        FileOutputStream fos = null;
        OutputStreamWriter osw = null;
        BufferedWriter out = null;
        try {
            fos = new FileOutputStream(file);
            osw = new OutputStreamWriter(fos, encoding);
            out = new BufferedWriter(osw, bufferSize);
            out.write(s);
        } finally {
            if (out != null) {
                out.close();
                osw = null;
                fos = null;
            }
            if (osw != null) {
                osw.close();
                fos = null;
            }
            if (fos != null) {
                fos.close();
            }
        }
    }

    /**
   * Buffer size (32KB) for object serialization methods.
   */
    public static final int OBJECT_BUFFER_SIZE = 32 * 1024;

    /**
   * Writes serializable object to a file. Existing file will be overwritten.
   * 
   * @param f
   *          name of the destination file
   * @param o
   *          object to write
   * 
   * @exception IOException
   */
    public static void writeObject(String f, Object o) throws IOException {
        writeObject(f, o, OBJECT_BUFFER_SIZE);
    }

    /**
   * Writes serializable object to a file with specified buffer size. Existing
   * file will be overwritten.
   * 
   * @param f
   *          name of the destination file
   * @param o
   *          object to write
   * @param bufferSize
   *          buffer size used for writing
   * 
   * @exception IOException
   */
    public static void writeObject(String f, Object o, int bufferSize) throws IOException {
        FileOutputStream fos = null;
        BufferedOutputStream bos = null;
        ObjectOutputStream oos = null;
        try {
            fos = new FileOutputStream(f);
            bos = new BufferedOutputStream(fos, bufferSize);
            oos = new ObjectOutputStream(bos);
            oos.writeObject(o);
        } finally {
            if (oos != null) {
                oos.close();
                bos = null;
                fos = null;
            }
            if (bos != null) {
                bos.close();
                fos = null;
            }
            if (fos != null) {
                fos.close();
            }
        }
    }

    /**
   * Reads seralized object from the file.
   * 
   * @param f
   *          name of the source file
   * 
   * @return serialized object from the file.
   * @exception IOException
   */
    public static Object readObject(String f) throws IOException, ClassNotFoundException, FileNotFoundException {
        return readObject(f, OBJECT_BUFFER_SIZE);
    }

    /**
   * Reads seralized object from the file with specified buffer size
   * 
   * @param f
   *          name of the source file
   * @param bufferSize
   *          size of buffer used for reading
   * 
   * @return serialized object from the file.
   * @exception IOException
   * @exception ClassNotFoundException
   * @exception FileNotFoundException
   */
    public static Object readObject(String f, int bufferSize) throws IOException, ClassNotFoundException, FileNotFoundException {
        Object result = null;
        FileInputStream fis = null;
        BufferedInputStream bis = null;
        ObjectInputStream ois = null;
        try {
            fis = new FileInputStream(f);
            bis = new BufferedInputStream(fis, bufferSize);
            ois = new ObjectInputStream(bis);
            result = ois.readObject();
        } finally {
            if (ois != null) {
                ois.close();
                bis = null;
                fis = null;
            }
            if (bis != null) {
                bis.close();
                fis = null;
            }
            if (fis != null) {
                fis.close();
            }
        }
        return result;
    }

    /**
   * Reads file content as byte array.
   * 
   * @param s
   *          file name
   * 
   * @return file content
   * @exception IOException
   */
    public static final byte[] readBytes(String s) throws IOException {
        return readBytes(new File(s));
    }

    /**
   * Reads file content as byte array.
   * 
   * @param file
   *          file to read
   * 
   * @return file content
   * @exception IOException
   */
    public static final byte[] readBytes(File file) throws IOException {
        FileInputStream fileinputstream = new FileInputStream(file);
        long l = file.length();
        if (l > Integer.MAX_VALUE) {
            throw new IOException("File too big for loading into a byte array!");
        }
        byte byteArray[] = new byte[(int) l];
        int i = 0;
        for (int j = 0; (i < byteArray.length) && (j = fileinputstream.read(byteArray, i, byteArray.length - i)) >= 0; i += j) ;
        if (i < byteArray.length) {
            throw new IOException("Could not completely read the file " + file.getName());
        }
        fileinputstream.close();
        return byteArray;
    }

    public static void writeBytes(String filename, byte[] source) throws IOException {
        if (source == null) {
            return;
        }
        writeBytes(new File(filename), source, 0, source.length);
    }

    public static void writeBytes(File file, byte[] source) throws IOException {
        if (source == null) {
            return;
        }
        writeBytes(file, source, 0, source.length);
    }

    public static void writeBytes(String filename, byte[] source, int offset, int len) throws IOException {
        writeBytes(new File(filename), source, offset, len);
    }

    public static void writeBytes(File file, byte[] source, int offset, int len) throws IOException {
        if (len < 0) {
            throw new IOException("File size is negative!");
        }
        if (offset + len > source.length) {
            len = source.length - offset;
        }
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            fos.write(source, offset, len);
        } finally {
            if (fos != null) {
                fos.close();
            }
        }
        return;
    }

    public static boolean redirectInputStream(InputStream in, String path) throws IOException {
        FileOutputStream out = null;
        boolean result = false;
        try {
            out = new FileOutputStream(path);
            copyPipe(in, out, FILE_BUFFER_SIZE);
            result = true;
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ioex) {
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ioex) {
                }
            }
        }
        return result;
    }

    public static void copyPipe(InputStream in, OutputStream out, int bufSizeHint) throws IOException {
        int read = -1;
        byte[] buf = new byte[bufSizeHint];
        while ((read = in.read(buf, 0, bufSizeHint)) >= 0) {
            out.write(buf, 0, read);
        }
        out.flush();
    }

    /**
   * Checks whether file exists.
   * 
   * @param filePath
   * @return boolean
   */
    public static boolean fileExists(String filePath) {
        File f = new File(filePath);
        return (f.exists() && f.isFile());
    }

    /**
   * Checks whether directory exists.
   * 
   * @param filePath
   * @return boolean
   */
    public static boolean folderExists(String dirPath) {
        File f = new File(dirPath);
        return (f.exists() && f.isDirectory());
    }

    /**
   * Reads content from URL to a string.
   * 
   * @param url
   *          URL
   * 
   * @exception IOException
   */
    public static String readString(URL url) throws IOException {
        BufferedReader in = null;
        StringBuffer buf = new StringBuffer("");
        try {
            URLConnection urlConnection = url.openConnection();
            in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                buf.append(inputLine);
            }
        } finally {
            if (in != null) {
                in.close();
            }
        }
        return buf.toString();
    }

    private static final String[] supportedExtensions = { "html", "htm", "txt", "doc", "xls", "pdf", "owl", "nt", "xls", "yam" };

    /**
   * Reads content from URL to a string.
   * 
   * @param file
   *          File
   * @return true if file has supported exyension, false otherwise.
   */
    public static boolean hasSupportedExtension(String fileName) {
        boolean result = false;
        if (fileName != null) {
            int index = fileName.lastIndexOf(".");
            if (index != -1) {
                String ext = fileName.substring(index + 1);
                Arrays.sort(supportedExtensions);
                if (Arrays.binarySearch(supportedExtensions, ext) >= 0) result = true;
            }
        }
        System.out.println("result " + result);
        return result;
    }

    /**
   * Changes the extension of the file
   * 
   * @param fileName
   *          File name
   * @param newExtension
   *          new Extension
   * @return File name with new extension
   */
    public static String changeExtension(String fileName, String newExtension) {
        String newFileName = "";
        if (fileName != null) {
            int index = fileName.lastIndexOf(".");
            if (index != -1) {
                String name = fileName.substring(0, index);
                newFileName = name + "." + newExtension;
            }
        }
        return newFileName;
    }

    /**
   * list file names from folder given as String parameter
   */
    public static List listFiles(String folder) throws Exception {
        List documentNames = new ArrayList();
        if (folder == null || folder.equals("")) throw new Exception("The file doesn't exist!");
        File dir = new File(folder);
        String[] files = dir.list();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                String filePath = new StringBuffer(folder).append("/").append(files[i]).toString();
                if (new File(filePath).isFile()) documentNames.add(files[i]);
            }
        }
        return documentNames;
    }

    /**
   * list folder names from folder given as String parameter
   */
    public static Collection listFolders(String folder) throws Exception {
        Collection folderNames = new ArrayList();
        if (folder == null || folder.equals("")) throw new Exception("The folder doesn't exist!");
        File dir = new File(folder);
        String[] files = dir.list();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                String filePath = new StringBuffer(folder).append("/").append(files[i]).toString();
                if (new File(filePath).isDirectory()) folderNames.add(files[i]);
            }
        }
        return folderNames;
    }

    /**
   * Creates file if it doesn't exist, return false if it does.
   * 
   * @param filename
   * @return true if File did not exist and was created, returns false if file
   *         already exists
   */
    public static boolean createFile(String filename) throws IOException {
        boolean success = false;
        try {
            File file = new File(filename);
            createFile(file);
        } catch (IOException e) {
            throw e;
        }
        return success;
    }

    /**
   * Creates file if it doesn't exist, return false if it does.
   * 
   * @param file
   * @return true if File did not exist and was created, returns false if file
   *         already exists
   */
    public static boolean createFile(File file) throws IOException {
        boolean success = false;
        try {
            success = file.createNewFile();
        } catch (IOException e) {
            throw e;
        }
        return success;
    }
}
