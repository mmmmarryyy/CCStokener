package org.oclc.da.utils;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.Vector;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import org.oclc.da.exceptions.*;
import org.oclc.da.logging.Logger;

/**
 * This class contains basic utility methods that serve as an extension to 
 * the core Java IO package.
 * @author JCG
 *
 */
public class IOUtils {

    /** Invalid characters for file names, windows and linux combined. */
    private static final String INVALID_CHARS = "*|\\:\"<>?/";

    /** File permission modification command. */
    private static final String CHMOD_CMD = "chmod u[+operatior+][+perm+] [+file+]";

    /** Delete directory command with dir pathname substitution string. */
    private static final String DELETE_DIR_CMD = "rm -rf [+dirpath+]";

    private static Logger logger = Logger.newInstance();

    /** Close the specified input stream. If any exception occurs, an error
     *  will be logged, but no exception will be thrown.
     *  @param  inputStream   An input stream to close. Passing null will result
     *                        in no action.
     */
    public static void closeInputStream(InputStream inputStream) {
        closeIOObject(inputStream);
    }

    /** Close the specified reader. If any exception occurs, an error
     *  will be logged, but no exception will be thrown.
     *  @param  reader  A reader to close. 
     *                  Passing null will result in no action.
     */
    public static void closeReader(Reader reader) {
        closeIOObject(reader);
    }

    /** Close the specified output stream. If any exception occurs, an error
     *  will be logged, but no exception will be thrown.
     *  @param  outputStream  An output stream to close. If null is passed,
     *                        no action occurs.
     */
    public static void closeOutputStream(OutputStream outputStream) {
        closeIOObject(outputStream);
    }

    /** Close the specified writer. If any exception occurs, an error
     *  will be logged, but no exception will be thrown.
     *  @param  writer  A writer to close. 
     *                  Passing null will result in no action.
     */
    public static void closeWriter(Writer writer) {
        closeIOObject(writer);
    }

    /** Close the object specified. If any exception occurs, an error
     *  will be logged, but no exception will be thrown.
     *  Currently supported objects:
     *      <code>InputStream</code>
     *      <code>OutputStream</code>
     *      <code>Reader</code>
     *      <code>Writer</code>
     *      <code>Socket</code>
     *  @param  ioObject  Any supported IO object.
     */
    public static void closeIOObject(Object ioObject) {
        try {
            if (ioObject == null) {
                return;
            } else if (ioObject instanceof InputStream) {
                ((InputStream) ioObject).close();
            } else if (ioObject instanceof OutputStream) {
                ((OutputStream) ioObject).close();
            } else if (ioObject instanceof Reader) {
                ((Reader) ioObject).close();
            } else if (ioObject instanceof Writer) {
                ((Writer) ioObject).close();
            } else if (ioObject instanceof Socket) {
                ((Socket) ioObject).close();
                ;
            }
        } catch (IOException e) {
            logger.log(DAExceptionCodes.CLOSING_ERROR, Logger.WARN, IOUtils.class, "closeIOObject", "closing " + ioObject.getClass(), e);
        }
    }

    /**
     * Convert all data within the input stream specified into a string.
     * 
     * @param inputStream the input stream
     * 
     * @return the string
     * 
     * @throws DAException the DA exception
     */
    public static String inputStreamToString(InputStream inputStream) throws DAException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        transferStream(inputStream, buffer);
        byte[] bytes = buffer.toByteArray();
        return new String(bytes);
    }

    /**
     * Write the entire input stream to the output stream.
     * 
     * @param inputStream the input stream
     * @param outputStream the output stream
     * 
     * @return the long
     * 
     * @throws DAException on error. Exception code will be
     * <code>ERROR_READING</code> if read error.
     * <code>ERROR_WRITING</code> if write error.
     */
    public static long transferStream(InputStream inputStream, OutputStream outputStream) throws DAException {
        byte[] bytes = new byte[4096];
        long totalBytes = 0;
        try {
            int bytesRead = 0;
            while ((bytesRead = inputStream.read(bytes)) != -1) {
                try {
                    outputStream.write(bytes, 0, bytesRead);
                } catch (IOException e) {
                    logger.log(DAExceptionCodes.ERROR_WRITING, Logger.WARN, IOUtils.class, "transferStream", null, e);
                    DAException ex = new DAException(DAExceptionCodes.ERROR_WRITING, new String[] { "output stream" });
                    throw ex;
                }
                totalBytes += bytesRead;
            }
        } catch (IOException e) {
            logger.log(DAExceptionCodes.ERROR_READING, Logger.WARN, IOUtils.class, "transferStream", null, e);
            DAException ex = new DAException(DAExceptionCodes.ERROR_READING, new String[] { "input stream" });
            throw ex;
        }
        return totalBytes;
    }

    /** Transfer the contents of the URL specified to the destination file.
     * <p>
     *  @param url  A URL to obtain content from.
     *  @param dest A destination file to .
     *  @return The content type of the URL specified.
     *  @throws DAException on error. Exception code will be
     *          <code>ERROR_READING</code> if read error.
     *          <code>ERROR_WRITING</code> if write error.
     */
    public static String transferURL(URL url, File dest) throws DAException {
        URLConnection conn = null;
        String contentType = null;
        FileOutputStream fileOut = null;
        InputStream input = null;
        try {
            conn = url.openConnection();
            contentType = conn.getContentType();
            contentType = (contentType != null) ? contentType : ContentType.CONTENT_TYPE_UNKNOWN;
            input = conn.getInputStream();
            fileOut = createOutputStream(dest);
            transferStream(input, fileOut);
        } catch (Exception e) {
            logger.log(DAExceptionCodes.ERROR_READING, Logger.WARN, IOUtils.class, "transferURL", null, e);
            DAException ex = new DAException(DAExceptionCodes.ERROR_READING, new String[] { url.toString() });
            throw ex;
        } finally {
            closeIOObject(input);
            closeIOObject(fileOut);
        }
        return contentType;
    }

    /**
     * Read the lines of the text file specified into a vector.
     * <p>
     * 
     * @param textFile the text file
     * 
     * @return An array of strings containing the lines of the file.
     * 
     * @throws DAException the DA exception
     */
    public static String[] readLines(File textFile) throws DAException {
        return readLines(textFile, null);
    }

    /**
     * Read the lines of the text file specified into a vector.
     * Return only lines that contain one of the strings specified.
     * <p>
     * 
     * @param textFile A text file to read.
     * @param search   An array of strings to search for.
     * Lines containing ANY of the strings specified
     * will be returned. A <code>null</code> will
     * result in all lines being returned.
     * 
     * @return An array of strings containing the matching lines of the file.
     * 
     * @throws DAException the DA exception
     */
    public static String[] readLines(File textFile, String[] search) throws DAException {
        FileInputStream fileIn = null;
        InputStreamReader reader = null;
        BufferedReader buffer = null;
        Vector<String> lines = new Vector<String>();
        try {
            fileIn = new FileInputStream(textFile);
            reader = new InputStreamReader(fileIn);
            buffer = new BufferedReader(reader);
            String line = null;
            while ((line = buffer.readLine()) != null) {
                boolean found = (search == null);
                for (int index = 0; (!found && (index < search.length)); index++) {
                    found = (found || (line.indexOf(search[index]) != -1));
                }
                if (found) {
                    lines.add(line);
                }
            }
            String[] result = new String[lines.size()];
            lines.toArray(result);
            return result;
        } catch (IOException e) {
            logger.log(DAExceptionCodes.IO_ERROR, Logger.WARN, IOUtils.class, "readLines", null, e);
            DAException ex = new DAException(DAExceptionCodes.IO_ERROR, new String[] { textFile.toString() });
            throw ex;
        } finally {
            closeIOObject(buffer);
            closeIOObject(reader);
            closeIOObject(fileIn);
        }
    }

    /**
     * Del dir.
     * 
     * @param dir the dir
     * 
     * @throws DAException the DA exception
     */
    public static void delDir(File dir) throws DAException {
        System.out.println("\ndelDir dir to delete is " + dir.getPath());
        if (dir.isDirectory()) {
            StringReplacer repl = new StringReplacer(DELETE_DIR_CMD);
            String cmd = repl.replace(new String[] { dir.getPath() });
            ApplicationRunner runner = new ApplicationRunner(cmd);
            runner.runChecked();
        }
    }

    /**
     * Delete the file or directory specified.
     * <p>
     * 
     * @param fileDir  A file or directory to delete.
     * 
     * @throws DAException the DA exception
     */
    public static void deleteFile(File fileDir) throws DAException {
        if (!fileDir.delete()) {
            DAException ex = new DAException(DAExceptionCodes.ERROR_DELETING, new String[] { fileDir.toString() });
            logger.log(IOUtils.class, "deleteFile", null, ex);
            throw ex;
        }
    }

    /**
     * Create the file specified.
     * <p>
     * 
     * @param file A file to create.
     * 
     * @throws DAException the DA exception
     */
    public static void createFile(File file) throws DAException {
        boolean success = false;
        try {
            success = file.createNewFile();
        } catch (IOException e) {
            logger.log(DAExceptionCodes.IO_ERROR, Logger.ERROR, IOUtils.class, "createFile", null, e);
        }
        if (!success) {
            DAException ex = new DAException(DAExceptionCodes.ERROR_CREATING, new String[] { file.toString() });
            logger.log(IOUtils.class, "createFile", null, ex);
            throw ex;
        }
    }

    /** Make a file or directory name safe for the file system.
     * This method will replace invalid characters with underscores. 
     * <p>
     * @param name  File name to make safe.
     * @return The name with invalid characters replaces with underscores.
     */
    public static String makeSafeName(String name) {
        return (StringUtils.removeInvalids(name, INVALID_CHARS));
    }

    /**
     * Set or unset a file permission for the file owner.
     * <p>
     * 
     * @param file  The file to set/unset the permission for.
     * @param perms The list of permissions to set or unset.
     * "r" for read. "w" for write. "x" for execute.
     * These can be concatenated together.
     * @param add   If <code>true</code> it means add the permission.
     * <code>false</code> means to delete the permission.
     * 
     * @throws DAException the DA exception
     */
    public static void setFilePermissions(File file, String perms, boolean add) throws DAException {
        String oper = (add ? "+" : "-");
        String[] params = { oper, perms, file.toString() };
        StringReplacer replacer = new StringReplacer(CHMOD_CMD);
        String command = replacer.replace(params);
        ApplicationRunner runner = new ApplicationRunner(command);
        runner.runChecked();
    }

    /**
     * Determine if the search pattern exists in the file specified.
     * Currently does not handle regular expressions.
     * <p>
     * 
     * @param file      A text file to search.
     * @param search    A search phrase to look for in the file.
     * 
     * @return <code>true</code> if the search phrase was found.
     * <code>false</code> otherwise.
     * 
     * @throws DAException the DA exception
     */
    public static boolean searchFile(File file, String search) throws DAException {
        return (searchFileForLine(file, search) != null);
    }

    /**
     * Find a line in the file specified containing the search string specified.
     * Currently does not handle regular expressions.
     * <p>
     * 
     * @param file      A text file to search.
     * @param search    A search phrase to look for in the file.
     * 
     * @return A line containing the search string specified or
     * <code>null</code> if not found.
     * 
     * @throws DAException the DA exception
     */
    public static String searchFileForLine(File file, String search) throws DAException {
        TextFileReader tr = null;
        if (file.exists()) {
            try {
                tr = new TextFileReader(file);
                boolean finished = false;
                String line = null;
                while (!finished) {
                    line = tr.readLine();
                    finished = (line == null);
                    if (!finished) {
                        if (line.indexOf(search) != -1) {
                            return line;
                        }
                    }
                }
            } finally {
                if (tr != null) {
                    tr.close();
                }
            }
        }
        return null;
    }

    /**
     * Create an output stream for the file specified.
     * <p>
     * 
     * @param file  A file or directory to delete.
     * 
     * @return the file output stream
     * 
     * @throws DAException on error. Exception code will be
     * <code>ERROR_WRITING</code>.
     */
    private static FileOutputStream createOutputStream(File file) throws DAException {
        try {
            return (new FileOutputStream(file));
        } catch (IOException e) {
            DAException ex = new DAException(DAExceptionCodes.ERROR_WRITING, new String[] { file.toString() });
            logger.log(IOUtils.class, "createFileOut", null, ex);
            throw ex;
        }
    }
}
