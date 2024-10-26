import java.awt.*;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.zip.*;

/**
 * This class does various read/write file I/O and other file related 
 * functions.
 * <PRE>
 * <U><B>List of Methods</B></U>
 * FileIO() - Constructor, set some defaults.
 * initFileIO() - Reset class variables for zip and random-access files
 * promptFileName() - pop up prompt for file name
 * mapPathFileSeparators() - map '/' or '\' to current file separ. for OS 
 * readRandomAccessData() - read random access data from file.
 * readRandomAccessLine() - read random access line from file with readLine.
 * closeRandomAccessFile() - close random access file if was opened.
 * fileExists() - test if file path exists and is readable
 * readFileAsString() - read file from disk and return data as String
 * writeStringToFile() - write string data to disk file.
 * getFilesInDir() - get list of files in directory w/specified file ext.
 * getSubDirsInDir() - get list of sub directories in directory.
 * makeDirectory() - make a directory if it does not exist. 
 * makePathSubDirs() - make a directory tree if it doesn't exist for path
 * deleteFile() - Delete specific file specified with full path.
 * copyFile() - binary copy of one file or URL to a local file
 * copyDirectoryTree() - recursively copy directory tree.
 * readStringFromURL() - read a String data from URL
 * readBytesFromURL() - read binary data from URL 
 * readFileZipEntries() - read zip file to a Vector of ZipEntry entry files
 * getZipEntryName() - get the name of the ZipEntry item
 * getZipEntryData() - get the data from a ZipEntry item
 * getAllZipEntryNames() - get the names for all ZipEntry elements
 * getAllZipEntryData() - get the data for all ZipEntry elements
 * getAllZipEntries() - get the name and data for all zip entries
 * readAllZipEntries() - get array of name and data for each zip entry
 * writeZipFile() - zip the inputDir to a zipOutputFile.
 * zipDir() - zip a directory into the zip output stream.
 * getDirOfFile() - get the directory from a file path
 * getTopLevelBackLink() - get the "../" Web backlink for current level.
 * cvtRelDirToHyphenStr() - get the "../" Web backlink for current level.
 * getLineSeparatorFromString() - get String line-term.: "\r", "\n" or "\r\n"
 * getLineSeparatorFromFile() - get file line-terminator: "\r", "\n" or "\r\n"
 * getDirPathByDirBrowser() - set the output path by directory browser
 * testFile() - test if file exists and is readable.
 * testDir() - test if file exists and is a readable directory.
 * </PRE>
 * <P> 
 * This code is available at the HTMLtools project on SourceForge at
 * <A HREF="http://htmltools.sourceforge.net/">http://htmltools.sourceforge.net/</A>
 * under the "Common Public License Version 1.0" 
 * <A HREF="http://www.opensource.org/licenses/cpl1.0.php">
 * http://www.opensource.org/licenses/cpl1.0.php</A>.<P></P>
 * <P>
 * It was derived and refactored from the open source
 * MAExplorer (http://maexplorer.sourceforge.net/), and
 * Open2Dprot (http://Open2Dprot.sourceforge.net/) Table modules.
 * <P>
 * $Date: 2009/07/3 11:45:56 $ $Revision: 1.28 $
 * <BR> 
 * Copyright 2008, 2009 by Peter Lemkin 
 * E-Mail: lemkin@users.sourceforge.net 
 * <A HREF="http://lemkingroup.com/">http://lemkingroup.com/</A>
 * <BR>
 */
public class FileIO {

    /** error message log it not empty */
    public String errMsgLog;

    /** error message log it not empty. In case the function returning
   * a FileTable instance is null, we can still get at the error msg. */
    public static String lastErrMsgLog = "";

    /** ZipFile instance that contains the decoders and other things 
   * we need for unpacking the zip file.
   */
    public ZipFile zipFile;

    /** Random access file handle if used */
    public RandomAccessFile raf;

    /** Random access filename if used */
    public String rafFileName;

    /** Random access file buffer if used */
    public byte rafBuffer[];

    /** Last random access seek pointer */
    public long rafLastSeekPtr;

    /**
   * FileIO() - generic Constructor, set some defaults to 0.
   */
    public FileIO() {
        initFileIO();
    }

    /**
   * initFileIO() - Reset class variables for zip and random-access files   
   */
    public void initFileIO() {
        errMsgLog = "";
        zipFile = null;
        raf = null;
        rafFileName = null;
        rafBuffer = null;
        rafLastSeekPtr = 0;
    }

    /**
   * promptFileName() - pop up prompt for file name
   * @param promptDir default dir
   * @param msg message to display in file dialog
   * @return full file path if successful else null
   * @see Frame
   * @see FileDialog
   * @see FileDialog#setDirectory
   * @see FileDialog#setVisible
   * @see FileDialog#getFile
   * @see FileDialog#getDirectory
   */
    public String promptFileName(String promptDir, String msg) {
        Frame fdFrame = new Frame("FileDialog");
        FileDialog fd = new FileDialog(fdFrame, msg, FileDialog.LOAD);
        if (promptDir != null) fd.setDirectory(promptDir);
        fd.setVisible(true);
        String newFile = fd.getFile();
        if (newFile == null) return (null);
        promptDir = fd.getDirectory();
        String fullPath = promptDir + newFile;
        return (fullPath);
    }

    /**
   * mapPathFileSeparators() - map '/' or '\' to current file separators for OS
   * @param str to map
   * @return mapped string
   */
    public static String mapPathFileSeparators(String str) {
        String fileSep = System.getProperty("file.separator");
        char fsCh = (fileSep.equals("/")) ? '/' : '\\';
        if (str == null) return (null);
        String sR = "";
        char ch, sBuf[] = str.toCharArray();
        int sSize = str.length(), sCtr = 0;
        while (sCtr < sSize) {
            ch = sBuf[sCtr++];
            if (fsCh == '/' && ch == '\\') ch = '/'; else if (fsCh == '\\' && ch == '/') ch = '\\';
            sR += ("" + ch);
        }
        return (sR);
    }

    /**
   * readRandomAccessLine() - read random access line from file with readLine.
   * Open the file if not previously opened. 
   * Read line until "\n", "\r", or "\r\n".
   * @param raf RandomAccessFile to read
   * @param startBytePtr to start reading
   * @return string read from the file. If any errors, return null.
   * @see #closeRandomAccessFile
   * @see RandomAccessFile
   */
    public String readRandomAccessLine(String fileName, long startBytePtr) {
        try {
            if (raf != null && !rafFileName.equals(fileName)) {
                raf.close();
            }
            if (raf == null && fileName != null) {
                rafFileName = fileName;
                raf = new RandomAccessFile(rafFileName, "r");
            }
            rafLastSeekPtr = startBytePtr;
            raf.seek(rafLastSeekPtr);
            String rawData = raf.readLine();
            return (rawData);
        } catch (IOException e) {
            return (null);
        }
    }

    /**
   * readRandomAccessData() - read random access data from file.
   * Open the file if not previously opened. Read data
   * from startBytePtr to endBytePtr.
   * @param raf RandomAccessFile to read
   * @param startBytePtr to start reading
   * @param endBytePtr to stop reading.
   * @return string read from the file. If any errors, return null.
   * @see #closeRandomAccessFile
   * @see RandomAccessFile
   */
    public String readRandomAccessData(String fileName, long startBytePtr, long endBytePtr) {
        int nBytesRead = 0, bytesToRead = (int) (endBytePtr - startBytePtr + 1);
        try {
            if (raf != null && !rafFileName.equals(fileName)) {
                raf.close();
            }
            if (raf == null && fileName != null) {
                rafFileName = fileName;
                raf = new RandomAccessFile(rafFileName, "r");
            }
            rafLastSeekPtr = startBytePtr;
            raf.seek(rafLastSeekPtr);
            if (rafBuffer == null || rafBuffer.length <= bytesToRead) rafBuffer = new byte[bytesToRead];
            nBytesRead = raf.read(rafBuffer, 0, bytesToRead);
            String rawData = new String(rafBuffer, 0, nBytesRead);
            return (rawData);
        } catch (IOException e) {
            return (null);
        }
    }

    /**
   * closeRandomAccessFile() - close random access file if was opened.
   * It also clears class variables (raf, rafName, rafBuffer, rafLastSeekPtr).
   * @return string read from the file. If any errors, return null.
   * @see #readRandomAccessLine
   * @see RandomAccessFile
   */
    public boolean closeRandomAccessFile() {
        try {
            if (raf != null) raf.close();
            raf = null;
            rafFileName = null;
            rafBuffer = null;
            return (true);
        } catch (IOException e) {
            raf = null;
            rafFileName = null;
            rafBuffer = null;
            rafLastSeekPtr = 0;
            return (false);
        }
    }

    /**
   * fileExists() - test if file path exists and is readable
   * @param filePath file name to test
   * @return true if file exists and is readable
   * @see File
   * @see #readFileAsString
   */
    public boolean fileExists(String filePath) {
        if (filePath == null) return (false);
        File f;
        try {
            f = new File(filePath);
            if (!f.exists()) return (false);
            if (!f.canRead()) return (false);
            return (true);
        } catch (SecurityException e) {
            return (false);
        }
    }

    /**
   * readFileAsString() - read file from disk and return data as String
   * @param fileName file name to read data
   * @return data as String, null if failed
   * @see File
   * @see #readFileAsString
   */
    public String readFileAsString(String fileName) {
        String sR;
        if (fileName == null) return (null);
        File f;
        try {
            f = new File(fileName);
            sR = readFileAsString(f);
            return (sR);
        } catch (SecurityException e) {
            errMsgLog += "FT-RFFD secur.Excep.[" + fileName + "] " + e;
            lastErrMsgLog = errMsgLog;
        }
        return (null);
    }

    /**
   * readFileAsString() - Will read file from disk & returns as String
   * @param f File to read
   * @return data read from the file, else null if failed.
   * @see RandomAccessFile
   * @see RandomAccessFile#canRead
   * @see RandomAccessFile#exists
   * @see RandomAccessFile#readFully
   * @see RandomAccessFile#close
   * @see System#runFinalization
   * @see UtilCM#mapCRLForCR2LF
   */
    public String readFileAsString(File f) {
        String sB, sR;
        RandomAccessFile rin = null;
        byte dataB[] = null;
        int size;
        try {
            if (!f.canRead()) {
                errMsgLog += "FT-RFFD Can't read '" + f.getName() + "'";
                lastErrMsgLog = errMsgLog;
                return (null);
            }
            if (!f.exists()) {
                errMsgLog += "FT-RFFD File not found [" + f.getName() + "]";
                lastErrMsgLog = errMsgLog;
                return (null);
            }
            rin = new RandomAccessFile(f, "r");
            size = (int) f.length();
            dataB = new byte[size];
            rin.readFully(dataB);
            rin.close();
            f = null;
            System.runFinalization();
            System.gc();
            sB = new String(dataB);
            sR = UtilCM.mapCRLForCR2LF(sB);
            dataB = null;
            System.runFinalization();
            System.gc();
            return (sR);
        } catch (SecurityException e) {
            errMsgLog += "FT-RFFD secur.Excep.[" + f.getName() + "] " + e;
            lastErrMsgLog = errMsgLog;
            return (null);
        } catch (FileNotFoundException e) {
            errMsgLog += "FT-RFFD FileNotFoundExcep.[" + f.getName() + "] " + e;
            lastErrMsgLog = errMsgLog;
        } catch (IOException e) {
            errMsgLog += "FT-RFFD IOExcep.[" + f.getName() + "] " + e;
            lastErrMsgLog = errMsgLog;
        }
        return (null);
    }

    /**
   * writeStringToFile() - write string data to disk file.
   * @param fileName is name of the file to write
   * @param data to write to the file
   * @return true if successful false if failed
   * @see File
   * @see File#canWrite
   * @see FileWriter
   * @see FileWriter#write
   * @see FileWriter#close
   * 
   */
    public boolean writeStringToFile(String fileName, String data) {
        if (data == null) return (false);
        File f;
        FileWriter out = null;
        char dataBuf[];
        int size = data.length();
        try {
            f = new File(fileName);
            out = new FileWriter(f);
            if (!f.canWrite()) return (false);
            dataBuf = new char[size];
            for (int i = 0; i < size; i++) dataBuf[i] = data.charAt(i);
            out.write(dataBuf, 0, size);
            out.close();
            return (true);
        } catch (SecurityException e) {
            errMsgLog += "FT-WSTF SecurityException [" + fileName + "] " + e;
            lastErrMsgLog = errMsgLog;
        } catch (FileNotFoundException e) {
            errMsgLog += "FT-WSTF FileNotFoundException [" + fileName + "] " + e;
            lastErrMsgLog = errMsgLog;
        } catch (IOException e) {
            errMsgLog += "FT-WSTF IOException [" + fileName + "] " + e;
            lastErrMsgLog = errMsgLog;
        }
        return (false);
    }

    /**
   * getFilesInDir() - get list of files in directory with specified file 
   * extension
   * @param dir directory to read
   * @param ext is the file extensions (e.g. ".txt") to match if not 
   *        null, else accept all files
   * @return list of files, null if fail or there are no files
   */
    public String[] getFilesInDir(String dir, String ext) {
        if (dir == null) return (null);
        String dirList[] = null;
        try {
            File f = new File(dir);
            if (f.isDirectory()) {
                dirList = f.list();
                if (dirList == null || dirList.length == 0) return (null);
                if (ext == null) return (dirList);
                int nAll = dirList.length, nExt = 0;
                String tmp[] = new String[nAll];
                for (int i = 0; i < nAll; i++) if (dirList[i].endsWith(ext)) tmp[nExt++] = dirList[i];
                if (nExt == 0) return (null);
                dirList = new String[nExt];
                for (int j = 0; j < nExt; j++) dirList[j] = tmp[j];
            }
        } catch (Exception ef) {
            return (null);
        }
        return (dirList);
    }

    /**
   * getSubDirsInDir() - get list of sub directories in directory.
   * This ignores files which are not sub directories.
   * @param dir directory to read
   * @return list of files, null if fail or there are no files
   */
    public String[] getSubDirsInDir(String dir) {
        if (dir == null) return (null);
        String dirList[] = null, fileSep = System.getProperty("file.separator");
        try {
            String subDirList[] = null;
            File f = new File(dir);
            if (f.isDirectory()) {
                dirList = f.list();
                if (dirList == null || dirList.length == 0) return (null);
                int nAll = dirList.length, nSubDir = 0;
                subDirList = new String[nAll];
                for (int i = 0; i < nAll; i++) {
                    String testFile = dir + fileSep + dirList[i];
                    File fsd = new File(testFile);
                    if (fsd.isDirectory()) subDirList[nSubDir++] = dirList[i];
                }
                dirList = new String[nSubDir];
                for (int i = 0; i < nSubDir; i++) dirList[i] = subDirList[i];
            }
        } catch (Exception ef) {
            return (null);
        }
        return (dirList);
    }

    /**
   * makeDirectory() - make a directory tree if it doesn't exist if 
   * allowMkdirsFlag using mkdirs(). Otherwise, just report whether the
   * directory tree exists.
   * @param dirName to check and possibly create. It may have multiple 
   *               subdirectories
   * @param allowMkdirsFlag to allow mkdirs() if directories not found
   * @return true if succeed in either finding the directory or
   *         in creating it, false if I/O error or a directory was NOT
   *         found and allowMkdirsFlag was NOT set.
   */
    public boolean makeDirectory(String dirName, boolean allowMkdirsFlag) {
        try {
            File fp = new File(dirName);
            if (fp.isDirectory()) return (true);
            if (allowMkdirsFlag) {
                if (!fp.mkdirs()) return (false);
            } else return (false);
            if (!fp.isDirectory()) return (false); else return (true);
        } catch (Exception e) {
            errMsgLog += "Can't make directory [" + dirName + "]\n";
            lastErrMsgLog = errMsgLog;
            return (false);
        }
    }

    /**
   * makePathSubDirs() - make a directory tree if it doesn't exist for path
   * the file path. Strip off the file at the end by finding the last 
   * fileSeparator, then do makeDirectory().
   * @param filePath to create the sub directories along the path
   * @return true if succeed 
   */
    public boolean makePathSubDirs(String filePath) {
        String filePath2 = mapPathFileSeparators(filePath), path = filePath2, fileSep = System.getProperty("file.separator");
        int lastIdx = filePath2.lastIndexOf(fileSep);
        if (lastIdx == -1) return (true);
        path = filePath2.substring(0, lastIdx);
        boolean ok = makeDirectory(path, true);
        return (ok);
    }

    /**
   * deleteFile() - Delete specific file specified with full path.
   * @param fullFileName of file to delete
   */
    public boolean deleteFile(String fullFileName) {
        try {
            File f = new File(fullFileName);
            if (f != null && f.exists()) f.delete();
        } catch (Exception e) {
            errMsgLog += "Can't delete file [" + fullFileName + "]\n";
            lastErrMsgLog = errMsgLog;
            return (false);
        }
        return (true);
    }

    /**
   * copyFile() - binary copy of one file or URL to a local file
   * @param srcName is either a full path local file name or
   *        a http:// prefixed URL string of the source file.
   * @param dstName is the full path of the local destination file name
   * @return true if succeed.
   */
    public boolean copyFile(String srcName, String dstName) {
        try {
            FileOutputStream dstFOS = new FileOutputStream(new File(dstName));
            FileInputStream srcFIS = null;
            int bufSize = 20000, nBytesRead = 0, nBytesWritten = 0;
            byte buf[] = new byte[bufSize];
            boolean isURL = (srcName.startsWith("http://"));
            if (isURL) {
                URL url = new URL(srcName);
                InputStream urlIS = url.openStream();
                while (true) {
                    nBytesRead = urlIS.read(buf);
                    if (nBytesRead == -1) break; else {
                        dstFOS.write(buf, 0, nBytesRead);
                        nBytesWritten += nBytesRead;
                    }
                }
                dstFOS.close();
            } else {
                srcFIS = new FileInputStream(new File(srcName));
                while (true) {
                    nBytesRead = srcFIS.read(buf);
                    if (nBytesRead == -1) break; else {
                        dstFOS.write(buf, 0, nBytesRead);
                        nBytesWritten += nBytesRead;
                    }
                }
                srcFIS.close();
                dstFOS.close();
            }
        } catch (Exception e1) {
            errMsgLog += "Can't copy '" + srcName + "' to '" + dstName + "'  " + e1 + "\n";
            lastErrMsgLog = errMsgLog;
            return (false);
        }
        return (true);
    }

    /**
   * copyDirectoryTree() - recursively copy directory tree. 
   * It does not preserve the files creation dates.
   * Derived from code on
   * http://www.roseindia.net/java/example/java/io/copyDirectory.shtml 
   * @param srcPath
   * @param dstPath
   * @return true if succeed, false if error and message in errMsgLog
   */
    public boolean copyDirectoryTree(String srcPath, String dstPath) {
        File fSrcPath = new File(srcPath), fDstPath = new File(dstPath);
        return (copyDirectoryTree(fSrcPath, fDstPath));
    }

    /**
   * copyDirectoryTree() - recursively copy directory tree. 
   * It does not preserve the files creation dates.
   * Derived from code on
   * http://www.roseindia.net/java/example/java/io/CopyDirectory.shtml 
   * @param srcPath
   * @param dstPath
   * @return true if succeed, false if error and message in errMsgLog
   */
    public boolean copyDirectoryTree(File srcPath, File dstPath) {
        try {
            if (srcPath.isDirectory()) {
                if (!dstPath.exists()) dstPath.mkdir();
                String files[] = srcPath.list();
                for (int i = 0; i < files.length; i++) copyDirectoryTree(new File(srcPath, files[i]), new File(dstPath, files[i]));
            } else {
                if (!srcPath.exists()) {
                    errMsgLog += "copyDirectoryTree I/O error from '" + srcPath + "' does not exist.\n";
                    lastErrMsgLog = errMsgLog;
                    return (false);
                } else {
                    InputStream in = new FileInputStream(srcPath);
                    OutputStream out = new FileOutputStream(dstPath);
                    byte[] buf = new byte[10240];
                    int len;
                    while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
                    in.close();
                    out.close();
                }
            }
            return (true);
        } catch (Exception e) {
            errMsgLog += "copyDirectoryTree I/O error from '" + srcPath.getName() + "' to '" + dstPath.getName() + "\n  " + e + "\n";
            lastErrMsgLog = errMsgLog;
            return (false);
        }
    }

    /**
   * readStringFromURL() - read a String data from URL 
   * @param srcName is either a full path local file name or 
   *        a http:// prefixed URL string of the source file.
   * @param optUpdateMsg (opt) will display message in logMsg() and 
   *        increasing ... in logMsg(). One '.' for every 10K bytes read.
   *        This only is used when reading a URL. Set to null if not used.
   * @return a String if succeed, else null.
   */
    public String readStringFromURL(String srcName, String optUpdateMsg) {
        byte bData[] = readBytesFromURL(srcName, optUpdateMsg);
        if (bData == null || bData.length == 0) return (null);
        String sR = new String(bData);
        return (sR);
    }

    /**
   * readBytesFromURL() - read binary data from URL 
   * @param srcName is either a full path local file name or 
   *        a http:// prefixed URL string of the source file.
   * @param optUpdateMsg (opt) will display message in logMsg() and 
   *        increasing ... in logMsg(). One '.' for every 10K bytes read.
   *        This only is used when reading a URL. Set to null if not used.
   * @return a byte[] if succeed, else null.
   */
    public byte[] readBytesFromURL(String srcName, String optUpdateMsg) {
        if (!srcName.startsWith("http://")) return (null);
        int bufSize = 20000, nBytesRead = 0, nBytesWritten = 0, oByteSize = bufSize;
        byte buf[] = null, oBuf[] = null;
        try {
            buf = new byte[bufSize];
            oBuf = new byte[bufSize];
            URL url = new URL(srcName);
            InputStream urlIS = url.openStream();
            while (true) {
                nBytesRead = urlIS.read(buf);
                if (nBytesRead == -1) break; else {
                    if (nBytesRead + nBytesWritten > oByteSize) {
                        byte tmp[] = new byte[oByteSize + bufSize];
                        for (int i = 0; i < nBytesWritten; i++) tmp[i] = oBuf[i];
                        oBuf = tmp;
                        oByteSize += bufSize;
                    }
                    for (int i = 0; i < nBytesRead; i++) oBuf[nBytesWritten++] = buf[i];
                    nBytesWritten += nBytesRead;
                }
            }
            byte tmp[] = new byte[nBytesWritten];
            for (int i = 0; i < nBytesWritten; i++) tmp[i] = oBuf[i];
            oBuf = tmp;
        } catch (Exception e1) {
            errMsgLog += "Can't read URL '" + srcName + "'\n";
            lastErrMsgLog = errMsgLog;
            return (null);
        }
        return (oBuf);
    }

    /**
   * readFileZipEntries() - read zip file to a Vector of ZipEntry entry files
   * but no directory items.
   * @param zipFileName
   * @return a Vector of zip file ZipEntry file entries, else null if failed.
   */
    public Vector readFileZipEntries(String zipFileName) {
        Vector zipEntries = new Vector();
        try {
            this.zipFile = new ZipFile(zipFileName);
            for (Enumeration e = zipFile.entries(); e.hasMoreElements(); ) {
                ZipEntry zEntry = (ZipEntry) e.nextElement();
                String name = zEntry.getName();
                if (!name.endsWith("/") && !name.endsWith("\\")) zipEntries.addElement(zEntry);
            }
        } catch (Exception e) {
            errMsgLog += "Can't zip file '" + zipFileName + "'\n";
            lastErrMsgLog = errMsgLog;
            return (null);
        }
        return (zipEntries);
    }

    /**
   * getZipEntryName() - get the name of the ZipEntry item
   * @param zipEntry data
   * @return zip entry name, else null if failed.
   */
    public String getZipEntryName(ZipEntry zipEntry) {
        if (zipEntry == null) return (null);
        String zipEntryName = zipEntry.getName();
        return (zipEntryName);
    }

    /**
   * getZipEntryData() - get the data from a ZipEntry item
   * @param zipEntry data
   * @return data of ZipEntry entry, else null if failed.
   */
    public String getZipEntryData(ZipEntry zipEntry) {
        String zipData = zipEntry.toString();
        return (zipData);
    }

    /**
   * getAllZipEntryNames() - get the names for all ZipEntry elements
   * @param zipEntryList (ZipEntry) to analyze
   * @return array of zip file names, else null if failed.
   */
    public String[] getAllZipEntryNames(Vector zipEntryList) {
        int nEntries = zipEntryList.size();
        String name, dataList[] = new String[nEntries];
        for (int i = 0; i < nEntries; i++) {
            ZipEntry ze = (ZipEntry) zipEntryList.elementAt(i);
            name = ze.getName();
            dataList[i] = name;
        }
        return (dataList);
    }

    /**
   * getAllZipEntryData() - get the data for all ZipEntry elements
   * @param zipEntryList (ZipEntry) to analyze
   * @return array of zip file names, else null if failed.
   */
    public String[] getAllZipEntryData(Vector zipEntryList) {
        int nEntries = zipEntryList.size();
        String data, dataList[] = new String[nEntries];
        for (int i = 0; i < nEntries; i++) {
            ZipEntry ze = (ZipEntry) zipEntryList.elementAt(i);
            data = ze.toString();
            dataList[i] = data;
        }
        return (dataList);
    }

    /**
   * getAllZipEntries() - get the name and data for all zip entries
   * The returned array is of form 
   * <PRE>
   *    dataList[0:nEntries-1][0:1]
   * where:
   *    dataList[*][0]   = zip file names
   *    dataList[*][1]   = unzipped data
   * </PRE>
   * @param zipEntryList (ZipEntry) to analyze
   * @return array of unzipped data , else null if failed.
   */
    public String[][] getAllZipEntries(ZipFile zf, Vector zipEntryList) {
        int nEntries = zipEntryList.size();
        String data = null, name, dataList[][] = new String[nEntries][];
        try {
            for (int i = 0; i < nEntries; i++) {
                ZipEntry ze = (ZipEntry) zipEntryList.elementAt(i);
                name = ze.getName();
                byte buf[] = new byte[(int) ze.getSize()];
                InputStream is = zf.getInputStream(ze);
                int len = 0, off = 0;
                while (off < buf.length && (len = is.read(buf, off, buf.length - off)) >= 0) off += len;
                data = new String(buf);
                dataList[i] = new String[2];
                dataList[i][0] = name;
                dataList[i][1] = data;
            }
        } catch (Exception e) {
            return (null);
        }
        return (dataList);
    }

    /**
   * readAllZipEntries() - get array of name and data for each zip entry
   * The returned array is of form 
   * <PRE>
   *    dataList[0:nEntries-1][0:1]
   * where:
   *    dataList[*][0]   = zip file names
   *    dataList[*][1]   = unzipped data
   * </PRE>
   * @param zipEntryName to read
   * @return array of unzipped data, else null if failed.
   */
    public String[][] readAllZipEntries(String zipFileName) {
        String dataList[][] = null;
        Vector zipEntryList = readFileZipEntries(zipFileName);
        if (zipEntryList == null) return (null); else dataList = getAllZipEntries(zipFile, zipEntryList);
        return (dataList);
    }

    /**
   * writeZipFile() - zip the inputDir to a zipOutputFile.
   * [TODO] debug subfolder path issue where it saves the zip
   * directory in the complete path rather than the directory
   * tree at the end of the path. Can't use writeZipFile() until
   * fix this.<P>
   * Code derived from http://www.devx.com/tips/Tip/14049 and
   * discussion in the example (pg 1922-1924) in
   * Chan, Lee, Kramer "The Java Class Libraries, 2nd ed., 
   * Vol 1, Addison-Wesley, 1998.
   * @param inputDirPath - path of directory to zip. Use the lowest 
   *                       directory in the tree as the name 
   *                       so it unzips it into that directory.
   * @param zipOutputFile - zip output file
   * @return true if succeed, error in errMsgLog
   */
    public boolean writeZipFile(String inputDirPath, String zipOutputFile) {
        try {
            FileOutputStream fos = new FileOutputStream(zipOutputFile);
            ZipOutputStream zos = new ZipOutputStream(fos);
            File f = new File(inputDirPath);
            if (!zipDir(inputDirPath, inputDirPath, zos)) {
                errMsgLog += "Problem zipping directory '" + inputDirPath + "' zipDir() failed.";
                lastErrMsgLog = errMsgLog;
                return (false);
            }
            zos.close();
            return (true);
        } catch (Exception e) {
            errMsgLog += "Problem zipping directory '" + inputDirPath + "' writeZipFile() failed.";
            lastErrMsgLog = errMsgLog;
            return (false);
        }
    }

    /**
   * zipDir() - zip a directory into the zip output stream.
   * <P>
   * Code derived from http://www.devx.com/tips/Tip/14049 and
   * discussion in the example (pg 1922-1924) in
   * Chan, Lee, Kramer "The Java Class Libraries, 2nd ed., 
   * Vol 1, Addison-Wesley, 1998.
   * @param inputDirPath - path of directory to zip.
   * @param dir2zip - bottom directory name to zip
   * @param zos - ZipOutputStream
   * @return true if succeed, error in errMsgLog
   */
    public boolean zipDir(String inputDirPath, String dir2zip, ZipOutputStream zos) {
        try {
            File zipDir = new File(inputDirPath);
            String dirList[] = zipDir.list();
            byte readBuffer[] = new byte[2156];
            int bytesIn = 0;
            for (int i = 0; i < dirList.length; i++) {
                File f = new File(zipDir, dirList[i]);
                if (f.isDirectory()) {
                    String fileDirPath = f.getPath();
                    zipDir(fileDirPath, fileDirPath, zos);
                    continue;
                }
                FileInputStream fis = new FileInputStream(f);
                String filePath = f.getPath();
                ZipEntry anEntry = new ZipEntry(filePath);
                zos.putNextEntry(anEntry);
                while ((bytesIn = fis.read(readBuffer)) != -1) zos.write(readBuffer, 0, bytesIn);
                fis.close();
            }
            return (true);
        } catch (Exception e) {
            errMsgLog += "Problem zipping to zip file '" + dir2zip + "' in zipDir() failed.";
            lastErrMsgLog = errMsgLog;
            return (false);
        }
    }

    /**
   * getDirOfFile() - get the directory from a file path
   * @param inputDirPath - file path
   * @return path of file
   */
    public String getDirOfFile(String inputDirPath) {
        if (inputDirPath == null) return (null);
        String inputDir = mapPathFileSeparators(inputDirPath), fileSep = "" + File.separatorChar;
        int idx = inputDir.lastIndexOf(fileSep);
        if (idx != -1) inputDir = inputDir.substring(0, idx);
        return (inputDir);
    }

    /**
   * getTopLevelBackLink() - get the "../" Web backlink for current level.
   * @param relDir - the relative relDir from the top level of Web site.
   *                 Note the relDir must end in a directory not in a 
   *                 specific file.
   * @return String with back link list of "../../" etc., else ""
   */
    public static String getTopLevelBackLink(String relDir) {
        String sR = "", fileSep = System.getProperty("file.separator");
        char fileSepChar = fileSep.charAt(0);
        if (relDir == null) return (sR);
        String sN = mapPathFileSeparators(relDir);
        for (int i = 1; i < sN.length(); i++) if (sN.charAt(i) == fileSepChar && sN.charAt(i - 1) != fileSepChar) sR += "../";
        return (sR);
    }

    /**
   * cvtRelDirToHyphenStr() - get the "../" Web backlink for current level.
   * @param relDir - the relative relDir from the top level of Web site.
   *                 Note the relDir must end in a directory not in a 
   *                 specific file.
   * @return String with back link list of "../../" etc., else ""
   */
    public static String cvtRelDirToHyphenStr(String relDir) {
        String sR = "", fileSep = System.getProperty("file.separator");
        char fileSepChar = fileSep.charAt(0);
        if (relDir == null) return (sR);
        String sN = mapPathFileSeparators(relDir);
        sR = UtilCM.replaceSubstrInString(sN, fileSep, "-");
        return (sR);
    }

    /**
   * getLineSeparatorFromString() - get String line-term: "\r", "\n" or "\r\n".
   * @param str used to determine the line-terminator.
   * @return "\r", "\n" or "\r\n" if succeed, null if not found or error.
   */
    public String getLineSeparatorFromString(String str) {
        int i = 0, lth = str.length(), ch = -1, ch2 = -2;
        String terminator = null;
        while (i < lth) {
            ch = str.charAt(i++);
            if (ch == '\n') {
                terminator = "\n";
                break;
            }
            if (ch == '\r') {
                if (i < lth - 1) ch2 = str.charAt(i++); else ch2 = -1;
                if (ch2 == '\n') {
                    terminator = "\r\n";
                    break;
                } else {
                    terminator = "\r";
                    break;
                }
            }
        }
        return (terminator);
    }

    /**
   * getLineSeparatorFromFile() - get file line-terminator: "\r", "\n" or
   * "\r\n". Read enough of the file to determine the line terminator.
   * @param inputFile to read to determine the line-terminator.
   * @return "\r", "\n" or "\r\n" if succeed, null if not found or error.
   */
    public String getLineSeparatorFromFile(String inputFile) {
        try {
            FileReader fr = new FileReader(inputFile);
            BufferedReader in = new BufferedReader(fr);
            int ch = -1, ch2 = -2;
            String terminator = null;
            while ((ch = in.read()) != -1) {
                if (ch == '\n') {
                    terminator = "\n";
                    break;
                }
                if (ch == '\r') {
                    ch2 = in.read();
                    if (ch2 == '\n') {
                        terminator = "\r\n";
                        break;
                    } else {
                        terminator = "\r";
                        break;
                    }
                }
            }
            in.close();
            return (terminator);
        } catch (IOException e) {
            return (null);
        }
    }

    /**
   * getDirPathByDirBrowser() - set the output path by directory browser
   * @param defaultDir if not null
   * @param sPrompt to use if not null
   * @return directory if not null.
   */
    public static String getDirPathByDirBrowser(String defaultDir, String sPrompt) {
        Frame f = new Frame();
        if (sPrompt == null) sPrompt = "Select the Project Folder";
        FileDialog fdO = new FileDialog(f, sPrompt);
        if (defaultDir != null) fdO.setDirectory(defaultDir);
        fdO.setMode(FileDialog.SAVE);
        fdO.setFile(sPrompt + " - then press 'Save'");
        fdO.setVisible(true);
        String useDir = fdO.getDirectory();
        return (useDir);
    }

    /**
   * testFile() - test if file exists and is readable. 
   * @param filePath to test
   * @return true if file exists and is readable
   */
    public boolean testFile(String filePath) {
        boolean flag = false;
        try {
            File fd = new File(filePath);
            flag = (fd.isFile() && fd.exists() && fd.canRead());
        } catch (Exception e) {
            flag = false;
        }
        return (flag);
    }

    /**
   * testDir() - test if file exists and is a readable directory.  
   * @param dirPath to test
   * @return true if directory exists and is readable
   */
    public boolean testDir(String dirPath) {
        boolean flag = false;
        try {
            File fd = new File(dirPath);
            flag = (fd.isDirectory() && fd.exists() && fd.canRead());
        } catch (Exception e) {
            flag = false;
        }
        return (flag);
    }
}
