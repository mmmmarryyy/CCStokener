package net.sourceforge.geeboss.util;

import net.sourceforge.geeboss.model.settings.GlobalSettings;
import net.sourceforge.geeboss.model.settings.SettingsFactory;
import net.sourceforge.geeboss.view.MainView;
import net.sourceforge.geeboss.view.MessageBoxFactory;
import net.sourceforge.geeboss.view.i18n.I18nUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.FileChannel;

/**
 * Utility methods for files
 * @author <a href="mailto:fborry@free.fr">Frederic BORRY</a>
 */
public class FileUtil {

    /** FileShop constant: Operation was aborted */
    public static final int OP_ABORTED = -1;

    /** FileShop constant: Operation failed */
    public static final int OP_FAILED = 1;

    /** FileShop constant: Operation was successfull */
    public static final int OP_SUCCESSFULL = 0;

    /** This utility class constructor is hidden */
    private FileUtil() {
    }

    /**
     * Copy one stream to another
     * @param inputStream the source stream
     * @param outputStream the destination stream
     * @throws IOException In case of an IO Error
     */
    public static void copyStreams(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte buffer[] = new byte[0xffff];
        int nbytes;
        while ((nbytes = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, nbytes);
        }
    }

    /**
     * Copy one file to another
     * @param inputStream the source stream
     * @param outputStream the destination stream
     * @throws IOException In case of an IO Error
     */
    public static void copy(FileInputStream inputStream, FileOutputStream outputStream) throws IOException {
        FileChannel input = inputStream.getChannel();
        FileChannel output = outputStream.getChannel();
        input.transferTo(0, input.size(), output);
    }

    /**
     * Copy one file to another
     * @param src The path of the sourcefile
     * @param dest The path of the destinationfile
     * @throws IOException In case of an IO Error
     */
    public static void copy(String src, String dest) throws IOException {
        copy(new FileInputStream(src), new FileOutputStream(dest));
    }

    /**
     * Read a file into a String using ISO-8859-1 encoding
     * @param file the file to read
     * @return the String with the file content
     */
    public static String fileToString(File file) throws FileNotFoundException, IOException {
        return fileToString(file, "ISO-8859-1");
    }

    /**
     * Read a file into a String
     * @param file the file to read
     * @return the String with the file content
     */
    public static String fileToString(File file, String encoding) throws FileNotFoundException, IOException {
        StringWriter iWriter;
        try {
            InputStream iInStream = new FileInputStream(file);
            Reader iStreamReader = new InputStreamReader(iInStream, encoding);
            iWriter = new StringWriter();
            char[] iBuffer = new char[1024];
            int niCharRed = iStreamReader.read(iBuffer);
            while (niCharRed >= 0) {
                iWriter.write(iBuffer, 0, niCharRed);
                niCharRed = iStreamReader.read(iBuffer);
            }
            iStreamReader.close();
            iWriter.close();
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Unsupported encoding '" + encoding + "'.", e);
        }
        return iWriter.toString();
    }

    /**
     * Copys java.exe.manifest and javaw.exe.manifest to the java.home/bin/ directory, if not yet copyd. These files enable WinXP Look & Feel on Windows XP operating systems.
     * 
     * @param toPath
     *            Path to copy to
     * @return true if the manifest files have been copied
     */
    public static boolean copyManifestFiles(String toPath) {
        boolean copyStatus = false;
        if (!isMediumWriteable(new File(toPath + "temp.tmp"))) return copyStatus;
        if (!new File(toPath + "java.exe").exists() && !new File(toPath + "javaw.exe").exists()) return copyStatus;
        if (!new File(toPath + "java.exe.manifest").exists()) {
            File java_exe_manifest = new File(toPath + "java.exe.manifest");
            try {
                copyStreams(FileUtil.class.getResourceAsStream("/net/sourceforge/geeboss/resources/javaw.exe.manifest"), new FileOutputStream(java_exe_manifest));
                copyStatus = true;
            } catch (FileNotFoundException e) {
                return false;
            } catch (IOException e) {
                return false;
            }
        }
        if (!new File(toPath + "javaw.exe.manifest").exists()) {
            File javaw_exe_manifest = new File(toPath + "javaw.exe.manifest");
            try {
                copyStreams(FileUtil.class.getResourceAsStream("/net/sourceforge/geeboss/resources/javaw.exe.manifest"), new FileOutputStream(javaw_exe_manifest));
                copyStatus = true;
            } catch (FileNotFoundException e) {
                return false;
            } catch (IOException e) {
                return false;
            }
        }
        return copyStatus;
    }

    /**
     * Check if the given Path points to an existing file.
     * 
     * @param path
     *            The path to check.
     * @return boolean true if the File exists
     */
    public static boolean exists(String path) {
        if (!StringUtil.isset(path)) return false;
        return new File(path).exists();
    }

    /**
     * Open a FileDialog for the user to choose where to export the "user.xml"
     * 
     * @return boolean true if the user has not canceld the dialog
     * @throws IOException
     *             If an IO Error occurs
     */
    public static boolean exportUserSettings() throws IOException {
        String fileName = getFilePath(new String[] { "*.xml", "*.*" }, "user.xml", SWT.SAVE, null);
        if (fileName != null) {
            if (!new File(fileName).exists() || overwrite()) copy(GlobalSettings.GEEBOSS_SETTINGS_FILE, fileName);
        }
        return (fileName != null);
    }

    /**
     * Read the contents from an InputStream into a String.
     * 
     * @param inS
     *            Any InputStream (e.g. from an URL)
     * @return String A new String from the byte array
     */
    public static String getContent(InputStream inS) {
        String line;
        String content = "";
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(inS));
            while ((line = in.readLine()) != null) content += line + "\n";
            in.close();
        } catch (IOException e) {
            MainView.mLogger.log("getContent()", e);
            return "";
        } finally {
            try {
                if (inS != null) inS.close();
            } catch (IOException e) {
                MainView.mLogger.log("getContent()", e);
            }
        }
        return content;
    }

    /**
     * Read the contents from an URL into a String.
     * 
     * @param url
     *            Any valid URL that is online
     * @return String A new String from the contents of the URL
     * @throws IOException
     *             If an error occurs
     */
    public static String getContent(URL url) throws IOException {
        URLConnection urlConnection = url.openConnection();
        return getContent(urlConnection.getInputStream());
    }

    /**
     * Get a path to a file from the user
     * 
     * @param style
     *            Either SWT.OPEN or SWT.SAVE
     * @return String The file path
     */
    public static String getFilePath(int style) {
        return getFilePath(null, null, style, null);
    }

    /**
     * Get a path to a file from the user
     * 
     * @param style
     *            Either SWT.OPEN or SWT.SAVE
     * @param path
     *            If not NULL browser into given directory
     * @return String The file path
     */
    public static String getFilePath(int style, String path) {
        return getFilePath(null, null, style, path);
    }

    /**
     * Get the FilePath from the user
     * 
     * @param formats
     *            Preselected formats
     * @param style
     *            The style of the FontDialog
     * @return String FilePath
     */
    public static String getFilePath(String[] formats, int style) {
        return getFilePath(formats, null, style, null);
    }

    /**
     * Get a path to a file from the user
     * 
     * @param formats
     *            List of selectable file extensions
     * @param fileName
     *            The preselected filename for the dialog
     * @param style
     *            Either SWT.OPEN or SWT.SAVE
     * @param path
     *            If not NULL browser into given directory
     * @return String The file path
     */
    public static String getFilePath(String[] formats, String fileName, int style, String path) {
        FileDialog fileDialog = new FileDialog(MainView.mShell, style);
        if (formats != null) fileDialog.setFilterExtensions(formats);
        if (path != null) fileDialog.setFilterPath(path);
        if (fileName != null) fileDialog.setFileName(fileName);
        return fileDialog.open();
    }

    /**
     * Get the FilePath from the user to save the file
     * 
     * @param fileName
     *            Preselected filename
     * @param format
     *            Format of the file to save
     * @return String Save path
     */
    public static String getSavePath(String fileName, String format) {
        String selectedFileName = getFilePath(new String[] { "*." + format, "*.*" }, fileName, SWT.SAVE, null);
        if (selectedFileName != null) {
            if (!new File(selectedFileName).exists() || overwrite()) {
                return selectedFileName;
            }
        }
        return null;
    }

    /**
     * Open a FileDialog for the user to choose from which file the settings should be imported
     * 
     * @return int Either OP_SUCCESSFULL, OP_FAILED or OP_ABORTED as result of the operation
     * @throws IOException
     *             If an IO Error occurs
     */
    public static int importUserSettings() throws IOException {
        String fileName = getFilePath(new String[] { "*.xml", "*.*" }, null, SWT.OPEN, null);
        if (fileName == null) return OP_ABORTED;
        if (SettingsFactory.isValidUserXML(fileName)) {
            copy(fileName, GlobalSettings.GEEBOSS_SETTINGS_FILE);
            return OP_SUCCESSFULL;
        }
        return OP_FAILED;
    }

    /**
     * Test if the medium from where Geeboss is executed is writeable
     * @param file A file to write
     * @return true if the medium is writeable
     */
    public static boolean isMediumWriteable(File file) {
        try {
            file.createNewFile();
            file.delete();
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    /**
     * Open dialog to ask if the file should be overwritten
     * 
     * @return true if file should be overwritten
     */
    private static boolean overwrite() {
        return MessageBoxFactory.showMessage(MainView.mShell, SWT.ICON_WARNING | SWT.YES | SWT.NO, I18nUtil.getI18nString("messagebox.title.attention"), I18nUtil.getI18nString("messagebox.file.exists")) == SWT.YES;
    }
}
