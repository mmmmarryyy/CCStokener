package commonapp.gui;

import common.log.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

/**
   Reads a version properties file from a JAR or the file system.
*/
public class VersionData {

    /** Version property prefix. */
    private static String ourPrefix = null;

    /**
     Loads the version property data from a file in the deployment jar or
     from the file system.

     @param theResourceName the system resource name for the version
     data file in the JAR or null.

     @param theVersionFile the version data file in the file system.

     @return the number of properties loaded.
  */
    public static int load(String theResourceName, File theVersionFile) {
        Properties props = new Properties();
        URL url = null;
        if (theResourceName != null) {
            url = ClassLoader.getSystemResource(theResourceName);
            if (url != null) {
                try {
                    InputStream is = url.openConnection().getInputStream();
                    props.load(is);
                    is.close();
                } catch (Exception e) {
                    Log.main.stackTrace("", e);
                }
            }
        }
        if ((props.size() == 0) && (theVersionFile != null) && theVersionFile.exists()) {
            try {
                FileInputStream in = new FileInputStream(theVersionFile);
                props.load(in);
                in.close();
            } catch (Exception e) {
                Log.main.stackTrace("", e);
            }
        }
        if (props.size() == 0) {
            Log.main.println(Log.FAULT, "no version data located, url=" + url + " file=" + theVersionFile);
        } else {
            System.getProperties().putAll(props);
        }
        return props.size();
    }

    /**
     Sets the system version prefix.

     @param thePrefix the system version prefix, e.g., "ihsdm." for IHSDM
     or "sa." for SafetyAnalyst.
  */
    public static void setPrefix(String thePrefix) {
        ourPrefix = thePrefix;
    }

    /**
     Gets the specified version title.

     @param theVersionMnemonic the version entity mnemonic, e.g., "sa",
     "cpm", "tam", etc.

     @return the title of the version entity or an empty string if the
     entity is unknown.
  */
    public static String title(String theVersionMnemonic) {
        return get(theVersionMnemonic + ".title");
    }

    /**
     Gets the specified version number.

     @param theVersionMnemonic the version entity mnemonic, e.g., "sa",
     "cpm", "tam", etc.

     @return the version number of the version entity or an empty string if
     the entity is unknown.
  */
    public static String number(String theVersionMnemonic) {
        return get(theVersionMnemonic + ".num");
    }

    /**
     Gets the specified version date.

     @param theVersionMnemonic the version entity mnemonic, e.g., "sa",
     "cpm", "tam", etc.

     @return the version date of the version entity or an empty string if
     the entity is unknown.
  */
    public static String date(String theVersionMnemonic) {
        return get(theVersionMnemonic + ".date");
    }

    /**
     Gets the value of a version entity.

     @param theID 5he version ID, e.g., "sa.num", "cpm.title", "tam.date", etc.

     @return the version value of the version entity or an empty string if
     the entity is unknown.
  */
    public static String get(String theID) {
        String prop = "v." + theID;
        if (ourPrefix != null) {
            prop = ourPrefix + prop;
        }
        String value = System.getProperty(prop);
        return (value != null) ? value : "";
    }
}
