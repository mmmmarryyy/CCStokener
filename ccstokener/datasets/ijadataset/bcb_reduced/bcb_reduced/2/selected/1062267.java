package org.spark.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.WeakHashMap;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileUtils {

    private static Logger LOG = LoggerFactory.getLogger(FileUtils.class);

    public static String getFileText(String _fileName) {
        try {
            InputStream input = getFileInputStream(_fileName);
            String content = IOUtils.toString(input);
            IOUtils.closeQuietly(input);
            return content;
        } catch (Throwable err) {
            LOG.error(_fileName, err);
            return "";
        }
    }

    public static String getFileText(URL _url) {
        try {
            InputStream input = _url.openStream();
            String content = IOUtils.toString(input);
            IOUtils.closeQuietly(input);
            return content;
        } catch (Exception err) {
            LOG.error(_url.toString(), err);
            return "";
        }
    }

    public static String getFileText(File _file) {
        try {
            InputStream input = getFileInputStream(_file);
            String content = IOUtils.toString(input);
            IOUtils.closeQuietly(input);
            return content;
        } catch (Throwable err) {
            LOG.error(_file.getAbsolutePath(), err);
            return "";
        }
    }

    public static String getFileText(URL _url, String _encoding) {
        try {
            InputStream input = _url.openStream();
            String content = IOUtils.toString(input, _encoding);
            IOUtils.closeQuietly(input);
            return content;
        } catch (Throwable err) {
            LOG.error(_url.toString(), err);
            return "";
        }
    }

    public static String getFileText(String _fileName, String _encoding) {
        try {
            InputStream input = getFileInputStream(_fileName);
            String content = IOUtils.toString(input, _encoding);
            IOUtils.closeQuietly(input);
            return content;
        } catch (Throwable err) {
            LOG.error(_fileName, err);
            return "";
        }
    }

    public static String getFileText(File _file, String _encoding) {
        try {
            InputStream input = getFileInputStream(_file);
            String content = IOUtils.toString(input, _encoding);
            IOUtils.closeQuietly(input);
            return content;
        } catch (Throwable err) {
            LOG.error(_file.getAbsolutePath(), err);
            return "";
        }
    }

    public static byte[] getFileBytes(String _fileName) {
        return getFileByteStream(_fileName).toByteArray();
    }

    public static byte[] getFileBytes(File _file) {
        return getFileByteStream(_file).toByteArray();
    }

    public static byte[] getFileBytes(URL _url) {
        try {
            InputStream input = _url.openStream();
            byte[] content = IOUtils.toByteArray(input);
            IOUtils.closeQuietly(input);
            return content;
        } catch (Exception err) {
            throw new RuntimeException(err);
        }
    }

    public static java.io.InputStream getFileInputStream(String _fileName) {
        try {
            return new java.io.FileInputStream(_fileName);
        } catch (Exception err) {
            throw new RuntimeException(err);
        }
    }

    public static java.io.InputStream getFileInputStream(URL _url) {
        try {
            return _url.openStream();
        } catch (Exception err) {
            throw new RuntimeException(err);
        }
    }

    public static java.io.InputStream getFileInputStream(File _file) {
        try {
            return new java.io.FileInputStream(_file);
        } catch (Exception err) {
            throw new RuntimeException(err);
        }
    }

    public static java.io.ByteArrayOutputStream getFileByteStream(File _file) {
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        java.io.FileInputStream file = null;
        try {
            file = new java.io.FileInputStream(_file);
            int iReadCount = 0;
            byte[] temp = new byte[1024];
            do {
                iReadCount = file.read(temp);
                if (iReadCount > 0) buffer.write(temp, 0, iReadCount);
            } while (iReadCount > 0);
        } catch (Exception err) {
            LOG.error(_file.getAbsolutePath(), err);
        } finally {
            try {
                if (file != null) file.close();
            } catch (Exception err) {
            }
        }
        return buffer;
    }

    public static java.io.ByteArrayOutputStream getFileByteStream(URL _url) {
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        try {
            InputStream input = _url.openStream();
            IOUtils.copy(input, buffer);
            IOUtils.closeQuietly(input);
        } catch (Exception err) {
            throw new RuntimeException(err);
        }
        return buffer;
    }

    public static java.io.ByteArrayOutputStream getFileByteStream(String _fileName) {
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        java.io.FileInputStream file = null;
        try {
            file = new java.io.FileInputStream(_fileName);
            int iReadCount = 0;
            byte[] temp = new byte[1024];
            do {
                iReadCount = file.read(temp);
                if (iReadCount > 0) buffer.write(temp, 0, iReadCount);
            } while (iReadCount > 0);
        } catch (Exception err) {
            LOG.error(_fileName, err);
        } finally {
            try {
                if (file != null) file.close();
            } catch (Exception err) {
            }
        }
        return buffer;
    }

    public static boolean writeFileText(String _fileName, String _strContent, boolean _isAppend) {
        java.io.FileWriter writer = null;
        try {
            writer = new java.io.FileWriter(_fileName, _isAppend);
            writer.write(_strContent);
            writer.flush();
            return true;
        } catch (Exception err) {
            LOG.error(_fileName, err);
            return false;
        } finally {
            try {
                if (writer != null) writer.close();
            } catch (Exception err) {
            }
        }
    }

    public static boolean writeFileText(String _fileName, String _content) {
        return writeFileText(_fileName, _content, false);
    }

    public static boolean appendFileText(String _fileName, String _content) {
        return writeFileText(_fileName, _content, true);
    }

    public static boolean writeFileText(String _fileName, String _content, String _encoding, boolean _isAppend) {
        java.io.OutputStreamWriter writer = null;
        try {
            java.io.FileOutputStream stream = new java.io.FileOutputStream(_fileName, _isAppend);
            writer = new java.io.OutputStreamWriter(stream, _encoding);
            writer.write(_content);
            writer.flush();
            return true;
        } catch (Exception err) {
            LOG.error(_fileName, err);
            return false;
        } finally {
            try {
                if (writer != null) writer.close();
            } catch (Exception err) {
            }
        }
    }

    public static boolean writeFileText(String _fileName, String _strContent, String _strEncoding) {
        return writeFileText(_fileName, _strContent, _strEncoding, false);
    }

    public static boolean appendFileText(String _fileName, String _strContent, String _strEncoding) {
        return writeFileText(_fileName, _strContent, _strEncoding, true);
    }

    public static boolean writeFileBytes(String _fileName, byte[] _content, boolean _isAppend) {
        java.io.FileOutputStream writer = null;
        try {
            writer = new java.io.FileOutputStream(_fileName, _isAppend);
            writer.write(_content);
            writer.flush();
            return true;
        } catch (Exception err) {
            LOG.error(_fileName, err);
            return false;
        } finally {
            try {
                if (writer != null) writer.close();
            } catch (Exception err) {
            }
        }
    }

    public static boolean writeFileBytes(String _fileName, byte[] _content) {
        return writeFileBytes(_fileName, _content, false);
    }

    public static boolean appendFileBytes(String _fileName, byte[] _content) {
        return writeFileBytes(_fileName, _content, true);
    }

    public static boolean deleteFile(String _fileName) {
        try {
            java.io.File file = new java.io.File(_fileName);
            return file.delete();
        } catch (Exception err) {
            LOG.error(_fileName, err);
            return false;
        }
    }

    public static boolean renameFile(String _fileName, String _newFileName) {
        try {
            java.io.File file = new java.io.File(_fileName);
            java.io.File newFile = new java.io.File(_newFileName);
            return file.renameTo(newFile);
        } catch (Exception err) {
            LOG.error(_fileName, err);
            return false;
        }
    }

    public static boolean creatDir(String _dirName) {
        try {
            java.io.File file = new java.io.File(_dirName);
            if (file.exists() && file.isDirectory()) return true;
            return file.mkdir();
        } catch (Exception err) {
            LOG.error(_dirName, err);
            return false;
        }
    }

    public static String[] getChildren(String _dirName) {
        try {
            java.io.File file = new java.io.File(_dirName);
            if (!(file.exists() && file.isDirectory())) return null;
            return file.list();
        } catch (Exception err) {
            LOG.error(_dirName, err);
            return null;
        }
    }

    public static void copyDirectory(String sourceDirName, String destinationDirName) {
        copyDirectory(new File(sourceDirName), new File(destinationDirName));
    }

    public static void copyDirectory(File source, File destination) {
        if (source.exists() && source.isDirectory()) {
            if (!destination.exists()) {
                destination.mkdirs();
            }
            File[] fileArray = source.listFiles();
            for (int i = 0; i < fileArray.length; i++) {
                if (fileArray[i].isDirectory()) {
                    copyDirectory(fileArray[i], new File(destination.getPath() + File.separator + fileArray[i].getName()));
                } else {
                    copyFile(fileArray[i], new File(destination.getPath() + File.separator + fileArray[i].getName()));
                }
            }
        }
    }

    public static void copyFile(String sourceFileName, String destinationFileName) {
        copyFile(new File(sourceFileName), new File(destinationFileName));
    }

    public static void copyFile(File source, File destination) {
        if (!source.exists()) {
            return;
        }
        if ((destination.getParentFile() != null) && (!destination.getParentFile().exists())) {
            destination.getParentFile().mkdirs();
        }
        try {
            FileChannel srcChannel = new FileInputStream(source).getChannel();
            FileChannel dstChannel = new FileOutputStream(destination).getChannel();
            dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
            srcChannel.close();
            dstChannel.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public static void deltree(String directory) {
        deltree(new File(directory));
    }

    public static void deltree(File directory) {
        if (directory.exists() && directory.isDirectory()) {
            File[] fileArray = directory.listFiles();
            for (int i = 0; i < fileArray.length; i++) {
                if (fileArray[i].isDirectory()) {
                    deltree(fileArray[i]);
                } else {
                    fileArray[i].delete();
                }
            }
            directory.delete();
        }
    }

    public static String getPath(String fullFileName) {
        int pos = fullFileName.lastIndexOf("/");
        if (pos == -1) {
            pos = fullFileName.lastIndexOf("\\");
        }
        String shortFileName = fullFileName.substring(0, pos);
        if (shortFileName == null || shortFileName.trim().length() <= 0) return "/";
        return shortFileName;
    }

    public static String getShortFileName(String fullFileName) {
        int pos = fullFileName.lastIndexOf("/");
        if (pos == -1) {
            pos = fullFileName.lastIndexOf("\\");
        }
        String shortFileName = fullFileName.substring(pos + 1, fullFileName.length());
        return shortFileName;
    }

    public static boolean exists(String fileName) {
        File file = new File(fileName);
        return file.exists();
    }

    public static String[] listDirs(String fileName) {
        return listDirs(new File(fileName));
    }

    public static String[] listDirs(File file) {
        List<String> dirs = new ArrayList<String>();
        File[] fileArray = file.listFiles();
        for (int i = 0; i < fileArray.length; i++) {
            if (fileArray[i].isDirectory()) {
                dirs.add(fileArray[i].getName());
            }
        }
        return (String[]) dirs.toArray(new String[0]);
    }

    public static String[] listFiles(String fileName) {
        return listFiles(new File(fileName));
    }

    public static String[] listFiles(File file) {
        List<String> files = new ArrayList<String>();
        File[] fileArray = file.listFiles();
        for (int i = 0; i < fileArray.length; i++) {
            if (fileArray[i].isFile()) {
                files.add(fileArray[i].getName());
            }
        }
        return (String[]) files.toArray(new String[0]);
    }

    public static void mkdirs(String pathName) {
        File file = new File(pathName);
        if (file.exists()) return;
        file.mkdirs();
    }

    public static boolean move(String sourceFileName, String destinationFileName) {
        return move(new File(sourceFileName), new File(destinationFileName));
    }

    public static boolean move(File source, File destination) {
        if (!source.exists()) {
            return false;
        }
        destination.delete();
        return source.renameTo(destination);
    }

    public static String replaceSeparator(String fileName) {
        return StringUtils.replace(fileName, '\\', "/");
    }

    public static List<String> toList(Reader reader) {
        List<String> list = new ArrayList<String>();
        try {
            BufferedReader br = new BufferedReader(reader);
            String line = null;
            while ((line = br.readLine()) != null) list.add(line);
            br.close();
        } catch (IOException ioe) {
        }
        return list;
    }

    public static List<String> toList(String fileName) {
        try {
            return toList(new FileReader(fileName));
        } catch (IOException ioe) {
            return new ArrayList<String>();
        }
    }

    public static Properties toProperties(FileInputStream fis) {
        Properties props = new Properties();
        try {
            props.load(fis);
        } catch (IOException ioe) {
        }
        return props;
    }

    public static Properties toProperties(String fileName) {
        try {
            return toProperties(new FileInputStream(fileName));
        } catch (IOException ioe) {
            return new Properties();
        }
    }

    private static Thread monitorThread;

    @SuppressWarnings("unchecked")
    private static Map monitorMap;

    public static long MONITOR_DELAY_TIME = 1000 * 60;

    static class FileMonitorObject {

        long lastModified;

        String filePath;

        boolean exists;

        boolean isExists() {
            return exists;
        }

        void setExists(boolean exists) {
            this.exists = exists;
        }

        String getFilePath() {
            return filePath;
        }

        void setFilePath(String filePath) {
            this.filePath = filePath;
        }

        long getLastModified() {
            return lastModified;
        }

        void setLastModified(long lastModified) {
            this.lastModified = lastModified;
        }
    }

    @SuppressWarnings("unchecked")
    public static synchronized void monitor(File file, FileChangedAware observer) {
        if (observer == null) throw new IllegalArgumentException("Observer parameter can not be null.");
        if (monitorThread == null) {
            monitorMap = Collections.synchronizedMap(new WeakHashMap());
            monitorThread = new Thread(new Runnable() {

                public void run() {
                    while (true) {
                        Iterator iter = monitorMap.entrySet().iterator();
                        while (iter.hasNext()) {
                            Map.Entry entry = (Map.Entry) iter.next();
                            Map fileObjMap = (Map) entry.getValue();
                            synchronized (fileObjMap) {
                                Iterator iterFileObj = fileObjMap.values().iterator();
                                while (iterFileObj.hasNext()) {
                                    FileMonitorObject obj = (FileMonitorObject) iterFileObj.next();
                                    File file = new File(obj.getFilePath());
                                    boolean exists = file.exists();
                                    try {
                                        if (exists && !obj.isExists()) ((FileChangedAware) entry.getKey()).fileChanged(obj.getFilePath(), 1); else if (!exists && obj.isExists()) ((FileChangedAware) entry.getKey()).fileChanged(obj.getFilePath(), 2); else {
                                            if (obj.getLastModified() != file.lastModified()) ((FileChangedAware) entry.getKey()).fileChanged(obj.getFilePath(), 0);
                                        }
                                    } catch (Throwable err) {
                                        err.printStackTrace();
                                    }
                                    obj.setExists(file.exists());
                                    obj.setLastModified(file.lastModified());
                                }
                            }
                        }
                        try {
                            Thread.sleep(MONITOR_DELAY_TIME);
                        } catch (InterruptedException e) {
                        }
                    }
                }
            });
            monitorThread.setDaemon(true);
            monitorThread.setPriority(Thread.MIN_PRIORITY);
            monitorThread.start();
        }
        Map fileObjMap = (Map) monitorMap.get(observer);
        if (fileObjMap == null) {
            fileObjMap = new HashMap();
            monitorMap.put(observer, fileObjMap);
        }
        if (fileObjMap.containsKey(file.getPath())) return;
        FileMonitorObject obj = new FileMonitorObject();
        obj.setFilePath(file.getPath());
        obj.setExists(file.exists());
        obj.setLastModified(file.lastModified());
        synchronized (fileObjMap) {
            fileObjMap.put(file.getPath(), obj);
        }
    }

    public interface FileChangedAware {

        public void fileChanged(String file, int action);
    }
}
