package preferences;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Properties;
import javax.swing.JOptionPane;

/**
 *
 * @author EddyM
 */
public class Preferences {

    private String nanoDropDataLocation;

    private String blueFuseDataLocation;

    private static Properties properties;

    private static final String NANODROP_FILES_LOCATION = "ND_FILES_LOCATION";

    private static final String BLUEFUSE_FILES_LOCATION = "BF_FILES_LOCATION";

    private static final String PROCESSED_DNA_ORGANIZATION_LOCATION = "PROC_DNA_ORG_LOCATION";

    private static final String PROCESSED_NOTES_LOCATION = "PROC_NOTES_LOCATION";

    private static final String DEFAULT_NANODROP_FILES_LOCATION = "C:\\Documents and Settings\\EddyM\\My Documents\\Cyto Backup";

    private static final String DEFAULT_BLUEFUSE_FILES_LOCATION = "E:\\GG_Data\\Arrays";

    private static final String DEFAULT_PROCESSED_DNA_ORGANIZATION_LOCATION = "C:\\Documents and Settings\\EddyM\\Desktop\\DNA Organization\\Processed DNA.xls";

    private static final String DEFAULT_PROCESSED_NOTES_LOCATION = "C:\\Documents and Settings\\EddyM\\Desktop\\DNA Organization\\1MB array data.xls";

    private static final String DEFAULT_PREFERENCES_RESOURCE_LOCATION = "prefs.xml";

    private static final String DEFAULT_PREFERENCES_FILE_PATH = "C:\\nddprefs.xml";

    static {
        loadStartupPreferences();
    }

    public static void loadStartupPreferences() {
        URL url = Preferences.class.getClassLoader().getResource(DEFAULT_PREFERENCES_RESOURCE_LOCATION);
        try {
            InputStream is = new BufferedInputStream(url.openStream());
            Properties readProps = read(is);
            setProperties(readProps);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, "Couldn't find the darned input file");
        }
    }

    public static void write(OutputStream out, Properties toWrite) {
        try {
            toWrite.storeToXML(out, "XML Preferences");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, "Error saving properties.  Using default properties...");
        }
    }

    public static Properties read(InputStream in) {
        Properties result = new Properties();
        try {
            result.loadFromXML(in);
            setProperties(result);
        } catch (IOException ex) {
            int yesOrNo = JOptionPane.showConfirmDialog(null, "Error loading properties.  Using default properties?", "Load Default Properties", JOptionPane.YES_NO_OPTION);
            if (yesOrNo == JOptionPane.YES_OPTION) {
                result = getAndUseDefaultProperties();
            }
        }
        return result;
    }

    public static Properties getAndUseDefaultProperties() {
        setProperties(new Properties());
        setNanoDropDataLocation(DEFAULT_NANODROP_FILES_LOCATION);
        setBlueFuseDataLocation(DEFAULT_BLUEFUSE_FILES_LOCATION);
        setProcessedDNAOrganizationInfoLocation(DEFAULT_PROCESSED_DNA_ORGANIZATION_LOCATION);
        setProcessedNotesLocation(DEFAULT_PROCESSED_NOTES_LOCATION);
        return getProperties();
    }

    public static void writeToDefaultLocation() throws IOException {
        File f = new File(DEFAULT_PREFERENCES_FILE_PATH);
        f.delete();
        f.createNewFile();
        write(new BufferedOutputStream(new FileOutputStream(f)), getProperties());
    }

    public static void readPreferencesFromDefaultLocation() {
        try {
            File f = new File(DEFAULT_PREFERENCES_FILE_PATH);
            InputStream is = new BufferedInputStream(new FileInputStream(f));
            Properties temp = new Properties();
            temp.load(is);
            setProperties(temp);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static String getNanoDropDataLocation() {
        return getProperties().getProperty(NANODROP_FILES_LOCATION);
    }

    public static void setNanoDropDataLocation(String nanoDropDataLocation) {
        getProperties().setProperty(NANODROP_FILES_LOCATION, nanoDropDataLocation);
    }

    public static String getBlueFuseDataLocation() {
        return "C:\\BlueFuseFiles";
    }

    public static void setBlueFuseDataLocation(String blueFuseDataLocation) {
        getProperties().setProperty(BLUEFUSE_FILES_LOCATION, blueFuseDataLocation);
    }

    public static String getProcessedDNAOrgnizationInfoLocation() {
        return getProperties().getProperty(PROCESSED_DNA_ORGANIZATION_LOCATION);
    }

    public static void setProcessedDNAOrganizationInfoLocation(String processedDNAOrganizationInfoLocation) {
        getProperties().setProperty(PROCESSED_DNA_ORGANIZATION_LOCATION, processedDNAOrganizationInfoLocation);
    }

    public static String getProcessedNotesLocation() {
        return getProperties().getProperty(PROCESSED_NOTES_LOCATION);
    }

    public static void setProcessedNotesLocation(String processedNotesLocation) {
        getProperties().setProperty(PROCESSED_NOTES_LOCATION, processedNotesLocation);
    }

    public static Properties getProperties() {
        return properties;
    }

    public static void setProperties(Properties properties2) {
        properties = properties2;
    }
}
