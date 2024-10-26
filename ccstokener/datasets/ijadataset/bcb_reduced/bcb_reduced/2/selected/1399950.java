package net.rptools.chartool.ui.component;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Window;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.InvalidPropertiesFormatException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BoundedRangeModel;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;
import net.rptools.chartool.model.KnownSettingsFiles;
import net.rptools.chartool.model.property.PropertySettings;
import net.rptools.chartool.ui.LoggingEventQueue;
import net.rptools.lib.AppUtil;
import net.rptools.lib.FileUtil;
import net.rptools.lib.image.ImageUtil;
import net.rptools.lib.swing.AboutDialog;

/**
 * Useful utilities shared between classes.
 * 
 * @author jgorrell
 * @version $Revision$ $Date$ $Author$
 */
public class Utilities {

    /**
   * The version and build number for this build
   */
    private static String version;

    /**
   * The name of the file that contains the version number. It just needs the tool name
   */
    public static final String VERSION_FILE = "net/rptools/{0}/ui/version.txt";

    /**
   * The name of the file that contains the credits. It just needs the tool name
   */
    public static final String CREDITS_FILE = "net/rptools/{0}/ui/credits.html";

    /**
   * The last working directory from the file dialog.
   */
    private static File lastDirectory;

    /**
   * Frame used to show loading status, which can take awhile if the database has to be loaded.
   */
    private static JFrame progressFrame;

    /**
   * Dialog used to show loading status, which can take awhile if the database has to be loaded.
   */
    private static JDialog progressDialog;

    /**
   * Progress model used to show what has been finished.
   */
    private static BoundedRangeModel progressModel;

    /**
   * Where system game files are read.
   */
    private static final URL GAME_FILE_URL;

    /**
   * The directory containing all of the downloaded game settings files.
   */
    public static final File GAME_SETTINGS_FILE_DIR = AppUtil.getAppHome("game");

    /**
   * The directory containing all of the downloaded game settings files.
   */
    public static final File GAME_SETTINGS_CACHE_DIR = AppUtil.getAppHome("game/cache");

    /**
   * Currently downloaded game file information. 
   */
    private static Properties gameFileVersions;

    /**
   * Available game files on the server.
   */
    private static Properties gameFilesAvailable;

    /**
   * Time im milliseconds since last download.
   */
    private static long gameFilesLastDownload;

    /**
   * File filter for image files.
   */
    public static final FileFilter IMAGE_FILTER = new FileFilter() {

        @Override
        public String getDescription() {
            return "PNG, GIF, and JPEG Images (*.png, *.gif, *.jpg *.jpeg)";
        }

        @Override
        public boolean accept(File file) {
            if (file.isDirectory()) return true;
            String name = file.getName();
            if (name.endsWith(".gif") || name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg")) return true;
            return false;
        }
    };

    /**
   * Logger instance for this class.
   */
    private static final Logger LOGGER = Logger.getLogger(Utilities.class.getName());

    /**
   * Set up the game file URL to be the RPTools web site or a local url from a system property.
   * Set the property 'gameFileUrl' to be the local url.
   */
    static {
        String sUrl = "http://rptools.net/tools/inittool/gameSettings/";
        URL url = null;
        String dUrl = System.getProperty("gameFileUrl");
        if (dUrl != null && (dUrl = dUrl.trim()).length() > 0) sUrl = dUrl;
        try {
            url = new URL(sUrl);
        } catch (MalformedURLException e) {
            LOGGER.log(Level.SEVERE, "Can't set up the game file url");
            System.exit(-1);
        }
        GAME_FILE_URL = url;
    }

    /**
   * Get the integer value of a field
   * 
   * @param dialog Working for this dialog.
   * @param field Read the value from this field
   * @param name The name used in the message dialog if the value can not be parsed.
   * @param positive Set this flag if the init value has to be positive.
   * @return The value of the field or {@link Integer#MIN_VALUE} if no value was entered.
   * @throws NumberFormatException The value in the field could not be parsed. A message
   * dialog is also displayed for the field. 
   * @throws IllegalStateException The number is required to be positive but a negative number was entered.
   */
    public static int getIntValue(JDialog dialog, JTextField field, String name, boolean positive) {
        String text = field.getText();
        if (text == null || (text = text.trim()).length() == 0) return Integer.MIN_VALUE;
        try {
            int value = Integer.parseInt(text);
            if (positive && value < 0) {
                JOptionPane.showMessageDialog(dialog, "The value for " + name + " must be positive but the value " + value + " was entered", "Error!", JOptionPane.ERROR_MESSAGE);
                throw new IllegalStateException("The value for " + name + " must be posivive but the value " + value + " was entered");
            }
            return value;
        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "Bad number text: " + text, e);
            JOptionPane.showMessageDialog(dialog, "The value '" + text + "' is not a valid " + name + " value.", "Error!", JOptionPane.ERROR_MESSAGE);
            throw e;
        }
    }

    /**
   * Read the string from the component, strip spaces and return the value. It will also check for
   * a required value if needed.
   * 
   * @param dialog Working for this dialog.
   * @param field Read the value from this field
   * @param name The name used in the message dialog if the value is required but not set.
   * @param required Flag used to indicate that a value is required.
   * @return The text from the component or <code>null</code> if the field was empty.
   * @throws IllegalStateException The field is required but a <code>null</code> was entered.
   */
    public static String getString(JDialog dialog, JTextField field, String name, boolean required) {
        String string = field.getText();
        if (string == null || (string = string.trim()).length() == 0) return string = null;
        if (required && string == null) {
            JOptionPane.showMessageDialog(dialog, "A value for '" + name + "' is required.", "Error!", JOptionPane.ERROR_MESSAGE);
            throw new IllegalArgumentException("A value for '" + name + "' is required.");
        }
        return string;
    }

    /**
   * Position a dialog relative to the passed bounds
   * 
   * @param bounds The bounds of the parent used to place the dialog.
   * @param window The window being positioned.
   */
    public static void setWindowPosition(Rectangle bounds, Window window) {
        if (bounds == null) {
            bounds = new Rectangle();
            bounds.setSize(window.getToolkit().getScreenSize());
        }
        window.setLocation(bounds.x + (bounds.width - window.getWidth()) / 2, bounds.y + (bounds.height - window.getHeight()) / 2);
    }

    /**
   * Search for an image on the file system.
   * 
   * @param component Component requesting the search
   * @return The file selected or <code>null</code> if no
   * image file was selected.
   */
    public static File browseForImage(Component component) {
        JFileChooser chooser = getChooser(IMAGE_FILTER);
        if (chooser.showOpenDialog(component) == JFileChooser.APPROVE_OPTION) return chooser.getSelectedFile();
        return null;
    }

    /**
   * Get the shared file chooser.
   *
   * @param filter Optional filter used to control the displayed files.
   * @return Returns the shared file chooser.
   */
    public static JFileChooser getChooser(FileFilter filter) {
        JFileChooser fileChooser = new JFileChooser() {

            public void approveSelection() {
                super.approveSelection();
                lastDirectory = getCurrentDirectory();
            }
        };
        fileChooser.setCurrentDirectory(lastDirectory);
        fileChooser.setFileFilter(filter);
        return fileChooser;
    }

    /**
   * Find the trailing digits of a string
   * 
   * @param value Search this string for digits.
   * @return The index of the first non-digit char at the end of <code>value</code>.
   */
    public static int findTrailingDigits(String value) {
        int index = value.length() - 1;
        while (index >= 0 && Character.isDigit(value.charAt(index))) index--;
        return index;
    }

    /**
   * Get the version and build number
   * 
   * @return The version w/ build number.
   */
    public static String getVersion() {
        if (version == null) {
            version = "DEVELOPMENT";
            try {
                String appName = AppUtil.getAppName();
                if (appName.endsWith("Dbg")) appName = appName.substring(0, appName.length() - 3);
                String versionResource = MessageFormat.format(VERSION_FILE, new Object[] { appName });
                if (Utilities.class.getClassLoader().getResource(versionResource) != null) {
                    version = new String(FileUtil.loadResource(versionResource));
                }
            } catch (IOException ioe) {
                LOGGER.log(Level.WARNING, "Can't load version file", ioe);
                version = "CAN'T LOAD VERSION FILE";
            }
        }
        return version;
    }

    /**
   * Get the about dialog for the passed frame.
   * 
   * @param frame The frame where the dialog is displayed
   * @param image The resource name to the image.
   * @return An about dialog for the passed frame.
   */
    public static AboutDialog getAboutDialog(JFrame frame, String image) {
        String credits = "";
        String version = "";
        Image logo = null;
        try {
            String appName = AppUtil.getAppName();
            if (appName.endsWith("Dbg")) appName = appName.substring(0, appName.length() - 3);
            credits = new String(FileUtil.loadResource(MessageFormat.format(CREDITS_FILE, new Object[] { appName })));
            version = getVersion();
            credits = credits.replace("%VERSION%", version);
            credits = credits.replace("%JIDE_URL%", Utilities.class.getClassLoader().getResource("com/jidesoft/icons/jide/jide_logo_small_2.png").toExternalForm());
            logo = ImageUtil.getImage(RPIconFactory.getInstance().getDefaultResourceDirectory() + image);
        } catch (Exception ioe) {
            LOGGER.log(Level.WARNING, "Problem opening the about dialog", ioe);
        }
        AboutDialog aboutDialog = new AboutDialog(frame, logo, credits);
        aboutDialog.setSize(355, 425);
        return aboutDialog;
    }

    /**
   * Delete the file. This will delete all of the children files if the passed file is a directory.
   * 
   * @param file File to be deleted
   * @param subDir Should sub directories be deleted?
   * @return The value <code>true</code> when the file and all of its subfiles were deleted.
   */
    public static boolean deleteFiles(File file, boolean subDir) {
        if (file.isFile()) return file.delete();
        File[] files = file.listFiles();
        for (File file2 : files) {
            if (file2.isDirectory() && !subDir) continue;
            boolean success = deleteFiles(file2, subDir);
            if (!success) return false;
        }
        return subDir ? file.delete() : true;
    }

    /**
   * Delete the files on exit. This will delete all of the children files if the passed file is a directory.
   * 
   * @param file File to be deleted
   * @param subDir Should sub directories be deleted?
   */
    public static void deleteFilesOnExit(File file, boolean subDir) {
        file.deleteOnExit();
        if (file.isFile()) return;
        File[] files = file.listFiles();
        for (File file2 : files) {
            if (file2.isDirectory() && !subDir) continue;
            deleteFilesOnExit(file2, subDir);
        }
    }

    /**
   * Create a quick progress frame w/o decorations
   * 
   * @param count Number of items to put in the progress model.
   * @param image Image displayed on the frame.
   * @return The progress dialog
   */
    public static JFrame createProgressFrame(int count, String image) {
        progressModel = new DefaultBoundedRangeModel(0, 0, 0, count);
        progressFrame = new JFrame();
        progressFrame.setUndecorated(true);
        JLabel label = new JLabel(RPIconFactory.getInstance().get(ImageStorageType.createDescriptor(image)));
        progressFrame.add(label, BorderLayout.CENTER);
        if (count > 0) {
            JProgressBar bar = new JProgressBar(progressModel);
            progressFrame.add(bar, BorderLayout.SOUTH);
        }
        progressFrame.pack();
        Utilities.setWindowPosition(null, progressFrame);
        return progressFrame;
    }

    /**
   * Create a quick progress dialog w/o decorations and then run a task in the background to 
   * 
   * @param frame The frame that owns this dialog
   * @param count Number of items to put in the progress model.
   * @param image Image displayed on the frame.
   * @param doInBackground Code to be executed in the background thread.
   * @param done Code to be executed in the EDT after the background thread has finished.
   */
    public static void createProgressDialog(RPFrame frame, int count, String image, final Runnable doInBackground, final Runnable done) {
        progressModel = new DefaultBoundedRangeModel(0, 0, 0, count);
        ProgressWorker worker = new ProgressWorker(frame, doInBackground, done);
        progressDialog = new JDialog(frame);
        progressDialog.setUndecorated(true);
        JLabel label = new JLabel(RPIconFactory.getInstance().get(ImageStorageType.createDescriptor(image)));
        progressDialog.add(label, BorderLayout.CENTER);
        if (count > 0) {
            JProgressBar bar = new JProgressBar(worker.dialogModel);
            progressDialog.add(bar, BorderLayout.SOUTH);
        }
        progressDialog.pack();
        Utilities.setWindowPosition(frame == null ? null : frame.getBounds(), progressDialog);
        progressDialog.setVisible(true);
        progressDialog.toFront();
        worker.execute();
    }

    /**
   * Release the progress frame when finished.
   */
    public static void releaseProgressFrame() {
        progressFrame = null;
        progressDialog = null;
        progressModel = null;
    }

    /**
   * Get the current progress frame.
   * 
   * @return The current progress frame.
   */
    public static JFrame getProgressFrame() {
        return progressFrame;
    }

    /**
   * Increment the progress model by one.
   * @param amount The amount to increment this model
   */
    public static void incrementProgressModel(int amount) {
        if (progressModel != null) {
            synchronized (progressModel) {
                int val = progressModel.getValue();
                if (val + amount >= progressModel.getMaximum()) LOGGER.log(Level.INFO, "Trying to incrment past maximum value");
                progressModel.setValue(val + amount);
            }
        }
    }

    /**
   * Finish the progress model.
   */
    public static void finishProgressModel() {
        if (progressModel != null) {
            synchronized (progressModel) {
                int val = progressModel.getMaximum();
                if (val != progressModel.getValue() + 1) LOGGER.log(Level.INFO, "Progress dialog skipping to the end from " + progressModel.getValue() + " to " + val);
                progressModel.setValue(val);
            }
        }
    }

    /**
   * Read a game settings file from the RPTools web site.
   * 
   * @param file File to be read
   * @return The bytes for the file.
   */
    private static byte[] readGameFile(String file) {
        URL url = null;
        try {
            url = new URL(GAME_FILE_URL, file);
            return FileUtil.getBytes(url.openStream());
        } catch (MalformedURLException e) {
            LOGGER.log(Level.WARNING, "Bad URL: " + url);
            throw new IllegalArgumentException("Bad URL: " + url, e);
        } catch (FileNotFoundException e) {
            LOGGER.log(Level.WARNING, "File not found: " + url);
            throw new IllegalArgumentException("File not found: " + url, e);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Could not copy file: " + url);
            throw new IllegalArgumentException("Could not copy file: " + url, e);
        }
    }

    /**
   * Return the property data that describes all of the remote game files available.
   * 
   * @return The property data from the RPTools site.
   */
    public static Properties getRemoteGameInfo() {
        if (gameFilesAvailable == null || System.currentTimeMillis() - gameFilesLastDownload > 60000) {
            gameFilesAvailable = new Properties();
            InputStream is = null;
            try {
                is = new ByteArrayInputStream(readGameFile("version.properties"));
                gameFilesAvailable.load(is);
                gameFilesLastDownload = System.currentTimeMillis();
            } catch (IllegalArgumentException e) {
                LOGGER.log(Level.WARNING, "Error opening version file datastream.", e);
                JOptionPane.showMessageDialog(null, e.getMessage() + " No remote game settings" + " files will be available.", "Error!", JOptionPane.ERROR_MESSAGE);
                gameFilesAvailable.clear();
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Error reading version file datastream.", e);
                JOptionPane.showMessageDialog(null, "Unable to read the version properties file from the RPTools site. No remote game settings" + " files will be available.", "Error!", JOptionPane.ERROR_MESSAGE);
                gameFilesAvailable.clear();
            } finally {
                try {
                    if (is != null) is.close();
                } catch (IOException e) {
                }
            }
        }
        return gameFilesAvailable;
    }

    /**
   * Return the property data that describes all of the locally downloaded game files.
   * 
   * @return The property data from this machine.
   */
    public static Properties getLocalGameInfo() {
        File localVersionFile = new File(Utilities.GAME_SETTINGS_FILE_DIR, "version.properties");
        if (gameFileVersions == null) {
            gameFileVersions = new Properties();
            if (localVersionFile.exists()) {
                try {
                    gameFileVersions.load(new BufferedReader(new FileReader(localVersionFile)));
                } catch (FileNotFoundException e) {
                    LOGGER.log(Level.WARNING, "Couldn't find the file after I checked for it?", e);
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Error reading the version file, all data being ignored.", e);
                    FileUtil.delete(Utilities.GAME_SETTINGS_FILE_DIR);
                    Utilities.GAME_SETTINGS_FILE_DIR.mkdirs();
                    localVersionFile.delete();
                    gameFileVersions.clear();
                }
            }
        }
        return gameFileVersions;
    }

    /**
   * Check to see if the passed file is a valid game file and if it requires an updated.
   * 
   * @param file Find the version of this file.
   * @return The value {@link Boolean#TRUE} if no update is needed of {@link Boolean#FALSE} if an 
   * update is needed. The <code>null</code> value is returned if the file is not a remote game file.
   */
    public static Boolean checkGameFile(File file) {
        if (file == null || !file.getParentFile().equals(Utilities.GAME_SETTINGS_FILE_DIR)) return null;
        String game = file.getName();
        int index = game.lastIndexOf('.');
        if (index < 0) return null;
        game = game.substring(0, index);
        String remoteVersion = Utilities.getRemoteGameInfo().getProperty(game + ".version");
        if (remoteVersion == null) return null;
        return checkGameFile(game, false) ? Boolean.TRUE : Boolean.FALSE;
    }

    /**
   * Check to see if a game file is currently downloaded, check its version against those on the
   * server, and then update it as needed.
   * 
   * @param game Name of the game file to check
   * @param updateFile Flag indicating that the file will be updated automatically if the local version doesn't match 
   * the remote version
   * @return The value <code>true</code> if the versions match, or <code>false</code> if they do not.
   * @throws IllegalStateException Unable to get the version info of the game file from te server
   */
    public static boolean checkGameFile(String game, boolean updateFile) {
        File gameFile = new File(Utilities.GAME_SETTINGS_FILE_DIR, game + GameSelectionDialog.SETTINGS_FILE_TYPE);
        File localVersionFile = new File(Utilities.GAME_SETTINGS_FILE_DIR, "version.properties");
        Properties gameFileVersions = getLocalGameInfo();
        String remoteVersion = gameFilesAvailable.getProperty(game + ".version");
        assert remoteVersion != null : "No remote version for game '" + game + "'";
        Properties gameFilesAvailable = getRemoteGameInfo();
        if (gameFile.exists()) {
            try {
                String localVersion = gameFileVersions.getProperty(game + ".version");
                if (localVersion != null && localVersion.equals(remoteVersion)) return true;
            } catch (IllegalStateException e) {
                if (gameFile.exists()) return true;
                throw e;
            }
        }
        if (gameFile.exists() && !updateFile) return false;
        Writer writer = null;
        try {
            FileUtil.writeBytes(gameFile, readGameFile(gameFile.getName()));
            gameFileVersions.setProperty(game + ".version", remoteVersion);
            gameFileVersions.setProperty(game + ".name", gameFilesAvailable.getProperty(game + ".name"));
            String games = gameFileVersions.getProperty("games");
            if (games == null || !games.contains(game)) gameFileVersions.setProperty("games", games == null ? game : games + "," + game);
            writer = new BufferedWriter(new FileWriter(localVersionFile));
            gameFileVersions.store(writer, "Version properties for each of the locally available game files");
            return true;
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error reading game file from server.", e);
            if (gameFile.exists()) return false;
            throw new IllegalStateException("Unable to access the game files from RPTools.");
        } finally {
            if (writer != null) try {
                writer.close();
            } catch (IOException e) {
            }
        }
    }

    /**
   * Copy all of the version specific files over, backing up any changed ones.
   * 
   * @param defaultFiles The default system files that are loaded on each new version of the app.
   * @param resourceDirectory The base directory for the file names in the class path. The file names
   * in <code>defaultFiles</code> are added to this path.
   * @param noVersionClear This runnable is executed to clear state when the version.xml file doesn't exist
   * @param ksf Access to all of the known settings files.
   * @param info Initialization information set by this method.
   * @return The list of files that were loaded or <code>null</code> if there wasn't a version change.
   */
    public static List<File> updateVersionSpecificFiles(String[] defaultFiles, String resourceDirectory, Runnable noVersionClear, KnownSettingsFiles ksf, InitializationInformation info) {
        InputStream pStream = null;
        InputStream rStream = null;
        OutputStream fStream = null;
        StringBuilder error;
        StringBuilder backup;
        File pFile = null;
        List<File> loadedFiles = null;
        try {
            pFile = new File(AppUtil.getAppHome(), "version.xml");
            Properties props = new Properties();
            if (pFile.exists()) {
                pStream = new BufferedInputStream(new FileInputStream(pFile));
                props.loadFromXML(pStream);
                pStream.close();
            } else if (noVersionClear != null) {
                noVersionClear.run();
            }
            incrementProgressModel(1);
            String oldVersion = props.getProperty("version");
            String newVersion = Utilities.getVersion();
            if (newVersion.equals(oldVersion)) {
                incrementProgressModel(defaultFiles.length);
                return null;
            }
            props.setProperty("version", newVersion);
            loadedFiles = new ArrayList<File>();
            error = new StringBuilder();
            backup = new StringBuilder();
            for (String fName : defaultFiles) {
                File file = new File(AppUtil.getAppHome(), fName);
                if (file.exists()) {
                    long lastModified = file.lastModified();
                    String pLastModified = props.getProperty(fName);
                    if (pLastModified == null || Long.parseLong(pLastModified) != lastModified) {
                        String renameFilename = fName.substring(0, fName.lastIndexOf('.') + 1) + oldVersion + fName.substring(fName.lastIndexOf('.'));
                        File renameFile = new File(AppUtil.getAppHome(), renameFilename);
                        StringBuilder message = backup;
                        File messageFile = renameFile;
                        if (!file.renameTo(renameFile)) {
                            message = error;
                            messageFile = file;
                        }
                        if (message.length() > 0) message.append("\n");
                        message.append("   ");
                        message.append(messageFile);
                    } else {
                        if (!file.delete()) {
                            if (error.length() > 0) error.append("\n");
                            error.append("   ");
                            error.append(file);
                        }
                    }
                }
                if (!file.exists()) {
                    file.getParentFile().mkdirs();
                    rStream = new BufferedInputStream(Utilities.class.getClassLoader().getResourceAsStream(resourceDirectory + fName));
                    fStream = new BufferedOutputStream(new FileOutputStream(file));
                    FileUtil.copy(rStream, fStream);
                    rStream.close();
                    fStream.close();
                    props.setProperty(fName, Long.toString(file.lastModified()));
                    loadedFiles.add(file);
                }
                incrementProgressModel(1);
            }
            fStream = new BufferedOutputStream(new FileOutputStream(pFile));
            props.storeToXML(fStream, newVersion);
        } catch (FileNotFoundException e) {
            LOGGER.log(Level.WARNING, "File not found", e);
            JOptionPane.showMessageDialog(null, "Unable to open the version properties file even though it exists. You will " + "need to delete the " + AppUtil.getAppHome().getAbsolutePath() + " directory to be able to start the tool", "Error!", JOptionPane.ERROR_MESSAGE);
            throw new IllegalStateException("Unable to open the version properties file even though it exists.", e);
        } catch (InvalidPropertiesFormatException e) {
            LOGGER.log(Level.WARNING, "Bad properties format", e);
            JOptionPane.showMessageDialog(null, "The version properties file is corrupt. You will " + "need to delete the " + pFile.getAbsolutePath() + " file to be able to start the tool", "Error!", JOptionPane.ERROR_MESSAGE);
            throw new IllegalStateException("The version properties file is corrupt.", e);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Problem copying files", e);
            JOptionPane.showMessageDialog(null, "Problem copying default rptool files. You will " + "need to delete the " + AppUtil.getAppHome().getAbsolutePath() + " directory to be able to start the tool", "Error!", JOptionPane.ERROR_MESSAGE);
            throw new IllegalStateException("Problem copying default rptool files.", e);
        } finally {
            try {
                if (pStream != null) pStream.close();
            } catch (IOException e) {
            }
            try {
                if (rStream != null) rStream.close();
            } catch (IOException e) {
            }
            try {
                if (fStream != null) fStream.close();
            } catch (IOException e) {
            }
        }
        StringBuilder sb = new StringBuilder();
        String messageTitle = "Warning!";
        int messageType = JOptionPane.INFORMATION_MESSAGE;
        if (backup.length() > 0) {
            sb.append("Some of the default files used by this tool have been modified, but newer versions need to be loaded." + "\n Backups have been made of these files:\n");
            sb.append(backup);
        }
        if (error.length() > 0) {
            if (sb.length() > 0) sb.append("\n\n");
            sb.append("Unable to load newer versions of some of the default files used by this tool because old files can " + "not be delete or backed up.\nPlease check the write permission on these files or delete them and try again.");
            sb.append(error);
            messageTitle = "Error!";
            messageType = JOptionPane.ERROR_MESSAGE;
        }
        if (sb.length() > 0) JOptionPane.showMessageDialog(null, sb.toString(), messageTitle, messageType);
        if (loadedFiles != null && !loadedFiles.isEmpty()) {
            for (File file : loadedFiles) {
                if (file.getName().endsWith(GameSelectionDialog.SETTINGS_FILE_TYPE)) ksf.addKnownSettingsFile(file);
            }
        }
        info.install = loadedFiles != null ? loadedFiles.contains(info.settingsFile) : info.install;
        incrementProgressModel(1);
        return loadedFiles;
    }

    /**
   * Resolve file names that point to the application home directory.
   * 
   * @param file File being resolved.
   * @return File with the $APP_HOME replaced by the proper path.
   */
    public static File resolveFilename(File file) {
        if (file != null && file.getAbsolutePath().contains("$APP_HOME")) {
            String fName = file.getAbsolutePath();
            int index = fName.indexOf("$APP_HOME");
            fName = AppUtil.getAppHome().getAbsolutePath() + fName.substring(index + "$APP_HOME".length());
            return new File(fName);
        }
        return file;
    }

    /**
   * Resolve file names that point to the application home directory.
   * 
   * @param file File being resolved.
   * @return File with the $APP_HOME replaced by the proper path.
   */
    public static String resolveFilename(String file) {
        if (file != null && file.contains("$APP_HOME")) {
            int index = file.indexOf("$APP_HOME");
            file = file.substring(0, index) + AppUtil.getAppHome().getAbsolutePath() + file.substring(index + "$APP_HOME".length());
        }
        return file;
    }

    /**
   * Show the browser.
   * 
   * @param c Component needing the browser displayed
   * @param uri Url to be displayed in the browser.
   */
    public static void showBrowser(Component c, String uri) {
        try {
            Desktop.getDesktop().browse(new URI(uri));
            return;
        } catch (IOException e1) {
            LOGGER.log(Level.WARNING, "Unable to start browser.", e1);
        } catch (URISyntaxException e1) {
            LOGGER.log(Level.WARNING, "Bad URI.", e1);
        }
        JOptionPane.showMessageDialog(c, "Unable to start the browser. URL can be viewed here:\n   " + uri);
    }

    /**
   * Capitalize the first letters in each word of a title.
   * 
   * @param titleString The string to be capitalized.
   * @return A string builder with the title properly capitalized
   */
    public static StringBuilder titleCase(String titleString) {
        StringBuilder title = new StringBuilder(titleString);
        boolean lastCharWhitespace = true;
        for (int i = 0; i < title.length(); i++) {
            char c = title.charAt(i);
            if (lastCharWhitespace && !Character.isWhitespace(c)) {
                if (!Character.isUpperCase(c)) title.replace(i, i, Character.toString(c));
                lastCharWhitespace = false;
            } else if (Character.isWhitespace(c)) {
                lastCharWhitespace = true;
            }
        }
        return title;
    }

    /**
   * Invoke a runnable on the event dispatch thread and wait for it to finish
   * execution.
   * 
   * @param runnable Code to run on the EDT.
   */
    public static void invokeAndWait(Runnable runnable) {
        if (!SwingUtilities.isEventDispatchThread()) {
            try {
                SwingUtilities.invokeAndWait(runnable);
                LOGGER.log(Level.FINER, "Calling invokeAndWait", new RuntimeException("here"));
            } catch (InterruptedException e) {
                LOGGER.log(Level.WARNING, "invokeAndWait was interrupted.", e);
            } catch (InvocationTargetException e) {
                LOGGER.log(Level.WARNING, "invokeAndWait method threw an exception.", e);
            }
        } else {
            runnable.run();
        }
    }

    /**
   * Initialize the game settings by either loading the currently active set or installing a new one.
   * 
   * @param initInfo Information used to determine what initializations need to be performed.
   * @param settings Property settings used to load and install the files.
   * @param prefs The known settings files are updated if errors occur.
   */
    public static void initializeGameSettings(InitializationInformation initInfo, PropertySettings settings, KnownSettingsFiles prefs) {
        try {
            if (initInfo.isInstall()) {
                settings.installGameSettings(initInfo.getSettingsFile());
                if (prefs.getLoadedSourceFiles() != null) {
                    for (File source : prefs.getLoadedSourceFiles()) settings.installGameSource(source);
                }
                prefs.setDefaultSettingsFile(initInfo.getSettingsFile());
            } else {
                settings.loadGameSettings(initInfo.getSettingsFile());
                if (prefs.getLoadedSourceFiles() != null) {
                    for (File source : prefs.getLoadedSourceFiles()) settings.loadGameSource(source);
                }
            }
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "Unable to load the settings: " + initInfo.getSettingsFile().getAbsolutePath(), e);
            String message = "Unable to load the current settings file or one of the source files.";
            prefs.removeKnownSettingsFile(initInfo.getSettingsFile());
            prefs.setLoadedSourceFiles(null);
            prefs.setDefaultSettingsFile(null);
            if (initInfo.getSettingsFile().equals(initInfo.getDefaultSettingsFile())) {
                message += "\nNo settings have been loaded.";
            } else {
                try {
                    settings.installGameSettings(initInfo.getDefaultSettingsFile());
                    prefs.setDefaultSettingsFile(initInfo.getDefaultSettingsFile());
                    message += "\nThe default settings have been loaded instead.";
                } catch (RuntimeException e1) {
                    LOGGER.log(Level.WARNING, "Problem installing default settings: " + initInfo.getDefaultSettingsFile().getAbsolutePath(), e1);
                    message += "\nThe default settings could not be loaded either so no settings have been loaded.";
                    prefs.removeKnownSettingsFile(initInfo.getDefaultSettingsFile());
                }
            }
            JOptionPane.showMessageDialog(Utilities.getProgressFrame(), message, "Error!", JOptionPane.ERROR_MESSAGE);
        }
        incrementProgressModel(1);
    }

    /**
   * Perform common application initialization tasks.
   * 
   * @param ksf Access to all of the known settings files.
   * @param info Initialization information set by this method.
   * @param defaultIcons The default icons for this application.
   * @param defaultResourceDirectory The default resource directory.
   */
    public static void initializeApplication(KnownSettingsFiles ksf, InitializationInformation info, String[] defaultIcons, String defaultResourceDirectory) {
        LoggingEventQueue.configureJide();
        RPIconFactory.getInstance().setDefaultIconNames(defaultIcons);
        RPIconFactory.getInstance().setDefaultResourceDirectory(defaultResourceDirectory);
        info.defaultSettingsFile = new File(AppUtil.getAppHome(), "game/default.rpgame");
        info.settingsFile = ksf.getDefaultSettingsFile();
        info.allExist = info.settingsFile != null && info.settingsFile.exists();
        if (!info.allExist) ksf.removeKnownSettingsFile(info.settingsFile);
        for (File source : ksf.getLoadedSourceFiles()) {
            info.allExist &= source.exists();
            if (!source.exists()) ksf.removeKnownSettingsFile(source);
        }
        if (!info.allExist) {
            info.settingsFile = null;
            ksf.setDefaultSettingsFile(null);
            ksf.setLoadedSourceFiles(null);
        }
        info.install = info.install || info.settingsFile == null;
        if (info.settingsFile == null) {
            info.settingsFile = info.defaultSettingsFile;
            info.install = true;
            ksf.setLoadedSourceFiles(null);
        }
    }

    /**
   * This class contains the initialization data needed by applications
   * 
   * @author Jay
   */
    public static class InitializationInformation {

        /**
     * Did all required settings files exist?
     */
        private boolean allExist;

        /**
     * Default settings file used when no other settings are available.
     */
        private File defaultSettingsFile;

        /**
     * Flag that indicates that the files will be installed.
     */
        private boolean install = false;

        /**
     * The name of the settings file being loaded.
     */
        private File settingsFile;

        /** @return Getter for defaultSettingsFile */
        public File getDefaultSettingsFile() {
            return defaultSettingsFile;
        }

        /** @param aDefaultSettingsFile Setter for defaultSettingsFile */
        public void setDefaultSettingsFile(File aDefaultSettingsFile) {
            defaultSettingsFile = aDefaultSettingsFile;
        }

        /** @return Getter for install */
        public boolean isInstall() {
            return install;
        }

        /** @param aInstall Setter for install */
        public void setInstall(boolean aInstall) {
            install = aInstall;
        }

        /** @return Getter for settingsFile */
        public File getSettingsFile() {
            return settingsFile;
        }

        /** @param aSettingsFile Setter for settingsFile */
        public void setSettingsFile(File aSettingsFile) {
            settingsFile = aSettingsFile;
        }

        /** @return Getter for allExist */
        public boolean isAllExist() {
            return allExist;
        }

        /** @param aAllExist Setter for allExist */
        public void setAllExist(boolean aAllExist) {
            allExist = aAllExist;
        }
    }

    /**
   * A worker that takes updates made to the {@link Utilities#progressModel} and then converts the value into the 
   * {@link SwingWorker} progress property. Which then updates a local progress model that can be used to display
   * progress to the user. Done this way since a lot of the underlying code already sets the 
   * {@link Utilities#progressModel} which can't be modified if it isn't on the EDT if it is in a component. 
   * 
   * @author Jay
   */
    private static class ProgressWorker extends SwingWorker<Object, Object> implements ChangeListener, PropertyChangeListener {

        /**
     * Code ran in the background
     */
        private Runnable doInBackground;

        /**
     * Code ran in the EDT after the background code has finished
     */
        private Runnable done;

        /**
     * Frame set busy while the work is being done.
     */
        private RPFrame frame;

        /**
     * Model used to actually display the progress to the user.
     */
        private BoundedRangeModel dialogModel = new DefaultBoundedRangeModel(0, 0, 0, 100);

        /**
     * Set up this worker and attach all the listeners
     * 
     * @param aFrame The frame being set busy.
     * @param background The code to be ran in the background.
     * @param whenDone Code to be executed in the EDT after the background thread has finished.
     */
        public ProgressWorker(RPFrame aFrame, Runnable background, Runnable whenDone) {
            doInBackground = background;
            done = whenDone;
            Utilities.progressModel.addChangeListener(this);
            addPropertyChangeListener(this);
            frame = aFrame;
            if (frame != null) frame.startBusy();
        }

        /**
     * Execute the runnable which will update {@link Utilities#progressModel}.
     *  
     * @see javax.swing.SwingWorker#doInBackground()
     */
        @Override
        protected Object doInBackground() throws Exception {
            doInBackground.run();
            return null;
        }

        /**
     * Hide the dialog, end busy, and cleanup
     * 
     * @see javax.swing.SwingWorker#done()
     */
        @Override
        protected void done() {
            try {
                get();
                if (done != null) done.run();
            } catch (InterruptedException e) {
                LOGGER.log(Level.WARNING, "Interruption of progress worker ignored as it shouldn't happen.", e);
            } catch (ExecutionException e) {
                LOGGER.log(Level.WARNING, "Exception occurred in background thread.", e);
                JOptionPane.showMessageDialog(frame, e.getMessage(), "Error!", JOptionPane.ERROR_MESSAGE);
            } finally {
                progressDialog.setVisible(false);
                if (frame != null) frame.endBusy();
                releaseProgressFrame();
            }
        }

        /**
     * Handle a change in {@link Utilities#progressModel} by setting this worker thread's progress indicator.
     *  
     * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
     */
        public void stateChanged(ChangeEvent aE) {
            synchronized (Utilities.progressModel) {
                double percent = ((double) progressModel.getValue() / (double) progressModel.getMaximum()) * 100D;
                if (progressModel.getValue() == progressModel.getMaximum()) percent = 100;
                setProgress((int) Math.round(percent));
            }
        }

        /**
     * Called when the {@link SwingWorker} progress property is changed. It updates the displayed
     * model.
     * 
     * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
     */
        public void propertyChange(PropertyChangeEvent aEvt) {
            if (aEvt.getPropertyName().equals("progress")) {
                dialogModel.setValue(getProgress());
            }
        }
    }
}
