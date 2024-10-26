package com.showdown.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;
import org.dom4j.Document;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.eclipse.swt.program.Program;
import com.showdown.api.ITorrentInfo;
import com.showdown.api.impl.OperatingSystem;
import com.showdown.api.impl.ShowDownManager;
import com.showdown.log.ShowDownLog;
import com.showdown.resource.Messages;
import com.showdown.resource.PropertiesLoader;
import com.showdown.settings.SettingsItemEnum;

/**
 * Utility class for working with files
 * @author Mat DeLong
 */
public final class FileUtil {

    /**
    * Opens the given URL in an external browser
    * @param url the URL to try to open
    * @return true if successful, false otherwise
    */
    public static boolean openURL(URL url) {
        if (url != null) {
            return openURL(url.toString());
        }
        return false;
    }

    /**
    * Opens the given URL in an external browser
    * @param url the URL to try to open
    * @return true if successful, false otherwise
    */
    public static boolean openURL(String url) {
        if (url != null) {
            try {
                return Program.launch(url);
            } catch (Exception ex) {
            }
        }
        return false;
    }

    /**
    * Returns the default directory for ShowDown related things
    * @return the default ShowDown directory
    */
    public static String getDefaultAppDir() {
        StringBuffer sb = new StringBuffer();
        sb.append(System.getProperty("user.home"));
        sb.append(File.separatorChar);
        sb.append(ShowDownManager.INSTANCE.getApplicationName());
        return sb.toString();
    }

    /**
    * Returns true if the given directory contains the specified file. This is a recursive
    * method, which navigates up the parent tree for the file, looking for the directory.
    * @param directory the directory to check
    * @param file the file to check
    * @return true if the file exists under the given directory, false otherwise
    */
    public static boolean containsFile(File directory, File file) {
        if (directory == null || !directory.exists() || !directory.isDirectory() || file == null) {
            return false;
        }
        if (file.isDirectory() && directory.equals(file)) {
            return true;
        }
        return containsFile(directory, file.getParentFile());
    }

    /**
    * Reads the text from a URL
    * @param url The URL to read from
    * @return the text contained at the given url, or empty string
    */
    public static String getURLText(String url) {
        StringBuffer sb = new StringBuffer();
        BufferedReader in = null;
        try {
            URL fileURL = new URL(url);
            URLConnection conn = makeURLConnection(fileURL, 15000);
            conn.setRequestProperty("User-agent", "Mozilla/4.0");
            in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                sb.append(inputLine);
                sb.append("\n");
            }
        } catch (Exception ex) {
            ShowDownLog.getInstance().logError(ex.getLocalizedMessage(), ex);
        } finally {
            close(in);
        }
        return sb.toString();
    }

    /**
    * Close the given stream
    * @param closeable the stream to close
    */
    public static void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception ex) {
            }
        }
    }

    /**
    * Returns a URLConnection made to the given URL, using the specified timeout
    * @param url the URL to create a connection to
    * @param timeout the timeout length, in milliseconds
    * @return the connection, or null
    */
    public static URLConnection makeURLConnection(URL url, int timeout) {
        URLConnection conn = null;
        try {
            conn = url.openConnection();
            conn.setConnectTimeout(timeout);
            conn.setReadTimeout(timeout);
        } catch (Exception ex) {
        }
        return conn;
    }

    /**
    * Downloads the torrent file referenced from the {@link ITorrentInfo} and returns
    * the local File reference to the downloaded torrent.
    * @param torrentInfo The torrent info object to get the name and download location from
    * @param directory The directory to download into
    * 
    * @return returns the file location of the downloaded torrent, or null if it couldn't download
    */
    public static File downloadTorrent(ITorrentInfo torrentInfo, File directory) {
        if (torrentInfo == null) {
            return null;
        }
        InputStream in = null;
        BufferedInputStream bis = null;
        FileOutputStream bos = null;
        File outputFile = null;
        try {
            String name = torrentInfo.getName().trim();
            directory.mkdirs();
            outputFile = new File(directory, name + ".torrent");
            int copy = 1;
            while (outputFile.exists()) {
                outputFile = new File(directory, name + " (" + (copy++) + ").torrent");
            }
            URLConnection conn = makeURLConnection(torrentInfo.getURL(), ShowDownManager.INSTANCE.getSettings().getInteger(SettingsItemEnum.TIMEOUT));
            int length = conn.getContentLength();
            in = conn.getInputStream();
            if (length == -1) {
                length = 250000;
            }
            bis = new BufferedInputStream(in);
            bos = new FileOutputStream(outputFile);
            byte[] buff = new byte[length];
            int bytesRead;
            while ((bytesRead = bis.read(buff, 0, buff.length)) != -1) {
                bos.write(buff, 0, bytesRead);
            }
        } catch (Exception e) {
        } finally {
            close(in);
            close(bis);
            close(bos);
        }
        return outputFile;
    }

    /**
    * Writes the given text into the given file
    * @param file The file to write to
    * @param text The text to write
    */
    public static void writeToFile(File file, String text) {
        if (!file.exists() || !file.isDirectory()) {
            BufferedWriter out = null;
            try {
                FileWriter fstream = new FileWriter(file);
                out = new BufferedWriter(fstream);
                out.write(text);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                close(out);
            }
        }
    }

    /**
    * Returns the default language properties file for ShowDown.
    * 
    * @return the message properties file
    */
    public static Properties getDefaultLanguageProperties() {
        Properties properties = null;
        InputStream in = null;
        try {
            String jarPath = "/com/showdown/resource/messages.properties";
            in = Messages.class.getResourceAsStream(jarPath);
            if (in != null) {
                properties = PropertiesLoader.loadProperties(in);
            }
        } catch (Exception ex) {
            ShowDownLog.getInstance().logError(ex.getLocalizedMessage(), ex);
        } finally {
            FileUtil.close(in);
        }
        return properties;
    }

    /**
    * Returns the language properties file for ShowDown. It checks if a parameter such as: "-nl=en"
    * is passed as a command line argument, and if it is, it looks for file: "nl/messages_%nl.properties"
    * relative to the root directory. If no nl property is specified, or if the file for the given nl 
    * property doesn't exist, the default (English) bundle is used.
    * 
    * @return the message properties file
    */
    public static Properties getLanguageProperties() {
        Properties properties = null;
        String language = ShowDownManager.INSTANCE.getSDParams().getLanguage();
        InputStream in = null;
        try {
            boolean loadDefault = true;
            if (language != null && language.length() > 0) {
                File file = new File("nl/messages_" + language + ".properties");
                loadDefault = !file.exists();
                if (!loadDefault) {
                    in = new FileInputStream(file);
                }
            }
            if (loadDefault) {
                return getDefaultLanguageProperties();
            }
            if (in != null) {
                properties = PropertiesLoader.loadProperties(in);
            }
        } catch (Exception ex) {
            ShowDownLog.getInstance().logError(ex.getLocalizedMessage(), ex);
        } finally {
            FileUtil.close(in);
        }
        return properties;
    }

    /**
    * Deletes a file or directory and any files or directories under it
    * @param path the file or directory to delete
    * @return true if the delete was successful, false otherwise
    */
    public static boolean deleteFile(File path) {
        if (path.exists() && path.isDirectory()) {
            File[] files = path.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    deleteFile(files[i]);
                } else {
                    files[i].delete();
                }
            }
        }
        return path.delete();
    }

    /**
    * Copies a file to the specified file
    * @param in the file to copy
    * @param out the file to copy into
    */
    public static boolean copyFile(File in, File out) {
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            fis = new FileInputStream(in);
            fos = new FileOutputStream(out);
            byte[] buf = new byte[1024];
            int i = 0;
            while ((i = fis.read(buf)) != -1) {
                fos.write(buf, 0, i);
            }
        } catch (Exception ex) {
            ShowDownLog.getInstance().logError(ex.getLocalizedMessage(), ex);
            return false;
        } finally {
            close(fis);
            close(fos);
        }
        return true;
    }

    /**
    * Loads properties from a properties file and returns
    * @param propFile the properties file to load
    * @return the in-memory properties representation
    */
    public static Properties loadProperties(File propFile) {
        Properties props = new Properties();
        if (propFile != null && propFile.exists() && !propFile.isDirectory()) {
            FileInputStream in = null;
            try {
                in = new FileInputStream(propFile);
                props.load(in);
            } catch (Exception ex) {
                ShowDownLog.getInstance().logError(ex.getLocalizedMessage(), ex);
            } finally {
                close(in);
            }
        }
        return props;
    }

    /**
    * Stores the given properties to disk
    * @param props the properties to store
    * @param propFile the file to store the properties in
    * @return true if successful, false otherwise
    */
    public static boolean storeProperties(Properties props, File propFile) {
        if (props != null && propFile != null && !propFile.isDirectory()) {
            FileOutputStream out = null;
            try {
                out = new FileOutputStream(propFile);
                props.store(out, null);
            } catch (Exception ex) {
                ShowDownLog.getInstance().logError(ex.getLocalizedMessage(), ex);
                return false;
            } finally {
                close(out);
            }
        }
        return true;
    }

    /**
    * Downloads the file located at the given URL into the file specified
    * @param externalFile the URL of the file to download
    * @param destinationFile the destination file
    * @return true if successful, false otherwise.
    */
    public static boolean downloadFile(URL externalFile, File destinationFile) {
        if (externalFile == null || destinationFile == null || (destinationFile.exists() && destinationFile.isDirectory())) {
            return false;
        }
        if (!destinationFile.exists() && !destinationFile.getParentFile().exists()) {
            if (!destinationFile.getParentFile().mkdirs()) {
                return false;
            }
        }
        URLConnection urlConn = makeURLConnection(externalFile, 2000);
        if (urlConn == null) {
            return false;
        }
        InputStream fis = null;
        FileOutputStream fos = null;
        try {
            fis = urlConn.getInputStream();
            fos = new FileOutputStream(destinationFile);
            byte[] buf = new byte[1024];
            int i = 0;
            while ((i = fis.read(buf)) != -1) {
                fos.write(buf, 0, i);
            }
        } catch (Exception ex) {
            ShowDownLog.getInstance().logError(ex.getLocalizedMessage(), ex);
            return false;
        } finally {
            close(fis);
            close(fos);
        }
        return true;
    }

    /**
    * Make the parent directories for the given file
    * @param file the file to create parent directories for
    * @return true if successful, false otherwise
    */
    public static boolean createFileDirectories(File file) {
        if (file != null && !file.exists()) {
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
        }
        return file != null;
    }

    /**
    * Write the given XML document to the specified file
    * @param document The XML document to write
    * @param outputFile The file to write into
    * @return true if successful, false otherwise
    */
    public static boolean writeXMLFile(Document document, File outputFile) {
        if (document != null && createFileDirectories(outputFile)) {
            XMLWriter writer = null;
            try {
                OutputFormat format = OutputFormat.createPrettyPrint();
                writer = new XMLWriter(new FileWriter(outputFile), format);
                writer.write(document);
            } catch (Exception ex) {
                ShowDownLog.getInstance().logError(ex.getLocalizedMessage(), ex);
                return false;
            } finally {
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (IOException e) {
                    }
                }
            }
            return true;
        }
        return false;
    }

    /**
    * Returns the arguments, based on current OS, for executing the specified file
    * @param fileToExecute the file to execute
    * @return the command line arguments to use for executing the file
    */
    public static String[] getExecuteArguments(File fileToExecute) {
        OperatingSystem os = ShowDownManager.INSTANCE.getOS();
        String[] args;
        if (os == OperatingSystem.WINDOWS) {
            args = new String[] { "cmd", "/C", "\"\"" + fileToExecute.getAbsolutePath() + "\"" };
        } else if (os == OperatingSystem.MAC) {
            args = new String[] { "open", fileToExecute.getAbsolutePath() };
        } else if (os == OperatingSystem.LINUX) {
            args = new String[] { "gnome-open", fileToExecute.getAbsolutePath() };
        } else {
            args = null;
        }
        return args;
    }

    /**
    * Executes the given file, waiting 5 seconds for it to complete.
    * @param fileToExecute the file to execute.
    */
    public static void executeFile(File fileToExecute) {
        if (fileToExecute != null && fileToExecute.exists() && !fileToExecute.isDirectory()) {
            Long killTime = ShowDownManager.INSTANCE.getSettings().getLong(SettingsItemEnum.KILL_TORRENT_PROCESS);
            executeArguments(getExecuteArguments(fileToExecute), fileToExecute.getParentFile(), null, killTime);
        }
    }

    /**
    * Executes the given file, waiting 5 seconds for it to complete.
    * @param fileToExecute the file to execute.
    * @param envProps array of env settings in format name=value, or null to inherit from current process.
    */
    public static void executeFile(File fileToExecute, String[] envProps) {
        if (fileToExecute != null && fileToExecute.exists() && !fileToExecute.isDirectory()) {
            Long killTime = ShowDownManager.INSTANCE.getSettings().getLong(SettingsItemEnum.KILL_TORRENT_PROCESS);
            executeArguments(getExecuteArguments(fileToExecute), fileToExecute.getParentFile(), envProps, killTime);
        }
    }

    /**
    * Executes the given arguments and waits the given length of time (in milliseconds)
    * for the process to complete before killing it.
    * @param arg the argument string to execute.
    * @param execDir the directory to execute the argument in
    * @param waitMS the milliseconds to wait before killing the process.
    */
    public static void executeArgument(String arg, File execDir, long waitMS) {
        if (arg != null) {
            try {
                Process process = Runtime.getRuntime().exec(arg, new String[] {}, execDir);
                killProcessLater(process, waitMS);
            } catch (IOException e) {
                ShowDownLog.getInstance().logError(e.getLocalizedMessage(), e);
            }
        }
    }

    /**
    * Executes the given arguments and waits the given length of time (in milliseconds)
    * for the process to complete before killing it.
    * @param args the arguments to execute.
    * @param execDir the directory to execute the arguments in
    * @param envProps array of env settings in format name=value, or null to inherit from current process.
    * @param waitMS the milliseconds to wait before killing the process.
    */
    public static void executeArguments(String[] args, File execDir, String[] envProps, long waitMS) {
        if (args != null) {
            try {
                Process process = Runtime.getRuntime().exec(args, envProps, execDir);
                killProcessLater(process, waitMS);
            } catch (IOException e) {
                ShowDownLog.getInstance().logError(e.getLocalizedMessage(), e);
            }
        }
    }

    private static void killProcessLater(Process process, long waitMS) {
        if (process != null) {
            Worker worker = new Worker(process);
            worker.start();
            try {
                worker.join(waitMS);
            } catch (InterruptedException ex) {
                worker.interrupt();
                Thread.currentThread().interrupt();
            } finally {
                process.destroy();
            }
        }
    }

    /**
    * Worker thread which simply waits for the given process to finish.
    */
    public static class Worker extends Thread {

        private final Process process;

        /**
       * Constructor which provides the process to wait for
       * @param process the process to wait for
       */
        public Worker(Process process) {
            this.process = process;
        }

        /**
       * {@inheritDoc}
       */
        public void run() {
            try {
                process.waitFor();
            } catch (InterruptedException ignore) {
                return;
            }
        }
    }
}
