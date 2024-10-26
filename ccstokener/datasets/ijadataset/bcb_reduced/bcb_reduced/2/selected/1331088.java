package eu.somatik.moviebrowser.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.swing.UIManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.flicklib.domain.MovieService;
import com.google.inject.Singleton;
import eu.somatik.moviebrowser.tools.FileTools;

/**
 * Singleton implementation for the settings
 * @author francisdb
 */
@Singleton
public class SettingsImpl implements Settings {

    private static final Logger LOGGER = LoggerFactory.getLogger(SettingsImpl.class);

    private static final String SETTINGS_DIR = ".moviebrowser";

    private static final String IMG_CACHE = "images";

    private static final String PREFERENCES = "preferences.properties";

    private static final String FOLDER_SETTINGS = "folders.lst";

    private static final String SERVICE_PREFIX = "flags.service.";

    private static final String FOLDERS_PROPERTY = "folders";

    private static final String LOOK_AND_FEEL_PROPERTY = "lookandfeel";

    private static final String PREF_SERVICE_PROPERTY = "pref.service";

    private static final String RENAME_TITLES = "renameTitles";

    private static final String SAVE_ALBUM_ART = "saveAlbumArt";

    private Map<String, String> preferences;

    /**
     *
     * @return the folders
     */
    @Override
    public final Set<String> loadFolders() {
        Map<String, String> prefs = loadPreferences();
        String folderString = prefs.get(FOLDERS_PROPERTY);
        if (folderString == null) {
            folderString = "";
        }
        String[] folderStrings = folderString.split(File.pathSeparator);
        Set<String> folders = new LinkedHashSet<String>();
        for (String folder : folderStrings) {
            folders.add(folder);
        }
        return folders;
    }

    @Override
    public void addFolder(File newFolder) {
        final Set<String> folders = loadFolders();
        if (!folders.contains(newFolder.getAbsolutePath())) {
            folders.add(newFolder.getAbsolutePath());
            saveFolders(folders);
        } else {
            LOGGER.warn("Trying to add folder that is allready in the list: " + newFolder);
        }
    }

    /**
     *
     * @param folders
     */
    @Override
    public final void saveFolders(Set<String> folders) {
        StringBuilder folderString = new StringBuilder();
        for (String folder : folders) {
            if (folder.trim().length() != 0) {
                if (folderString.length() > 0) {
                    folderString.append(File.pathSeparator);
                }
                folderString.append(folder);
            }
        }
        Map<String, String> prefs = loadPreferences();
        prefs.put(FOLDERS_PROPERTY, folderString.toString());
        savePreferences(prefs);
    }

    private File openFolderSettings() {
        File settingsDir = new File(System.getProperty("user.home"), SETTINGS_DIR);
        settingsDir.mkdirs();
        File folderSettings = new File(settingsDir, FOLDER_SETTINGS);
        if (!folderSettings.exists()) {
            try {
                LOGGER.info("First run, creating " + folderSettings.getAbsolutePath());
                boolean succes = folderSettings.createNewFile();
                if (!succes) {
                    throw new IOException("Could not create file: " + folderSettings.getAbsolutePath());
                }
            } catch (IOException ex) {
                LOGGER.error("File io error: ", ex);
            }
        }
        return folderSettings;
    }

    @Override
    public File getSettingsDir() {
        File settingsDir = new File(System.getProperty("user.home"), SETTINGS_DIR);
        if (!settingsDir.exists()) {
            settingsDir.mkdirs();
        }
        return settingsDir;
    }

    /**
     * @return the imageCacheDir
     */
    @Override
    public File getImageCacheDir() {
        File cache = new File(getSettingsDir(), IMG_CACHE);
        if (!cache.exists()) {
            cache.mkdirs();
        }
        return cache;
    }

    private Properties defaultPreferences() {
        Properties properties = new Properties();
        properties.put(LOOK_AND_FEEL_PROPERTY, UIManager.getSystemLookAndFeelClassName());
        properties.put(serviceKey("FLIXSTER"), "true");
        properties.put(serviceKey("TOMATOES"), "true");
        properties.put(serviceKey("GOOGLE"), "true");
        properties.put(serviceKey("MOVIEWEB"), "true");
        properties.put(serviceKey("IMDB"), "true");
        return properties;
    }

    @Override
    public Map<String, String> loadPreferences() {
        if (preferences == null) {
            Properties props = defaultPreferences();
            File prefsFile = new File(getSettingsDir(), PREFERENCES);
            InputStream is = null;
            try {
                if (prefsFile.exists()) {
                    is = new FileInputStream(prefsFile);
                    props.load(is);
                }
            } catch (IOException ex) {
                LOGGER.error("Could not load preferences to " + prefsFile.getAbsolutePath(), ex);
            } catch (SecurityException ex) {
                LOGGER.error("Could not load preferences to " + prefsFile.getAbsolutePath(), ex);
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException ex) {
                        LOGGER.error("Could not load outputstream for" + prefsFile.getAbsolutePath(), ex);
                    }
                }
            }
            preferences = new HashMap<String, String>();
            for (Map.Entry<Object, Object> entry : props.entrySet()) {
                preferences.put((String) entry.getKey(), (String) entry.getValue());
            }
        }
        return preferences;
    }

    @Override
    public void savePreferences(Map<String, String> preferences) {
        Properties props = new Properties();
        for (Map.Entry<String, String> entry : preferences.entrySet()) {
            props.setProperty(entry.getKey(), entry.getValue());
        }
        File prefsFile = new File(getSettingsDir(), PREFERENCES);
        FileTools.storePropeties(props, prefsFile);
    }

    @Override
    public boolean isDebugMode() {
        boolean debug = false;
        Map<String, String> prefs = loadPreferences();
        if (prefs.containsKey("debug") && "true".equals(prefs.get("debug"))) {
            LOGGER.info("Starting in DEBUG mode!");
            debug = true;
        }
        return debug;
    }

    @Override
    public String getApplicationVersion() {
        String version = null;
        InputStream is = null;
        try {
            String pom = "META-INF/maven/org.somatik/moviebrowser/pom.properties";
            URL resource = SettingsImpl.class.getClassLoader().getResource(pom);
            if (resource == null) {
                throw new IOException("Could not load pom properties: " + pom);
            }
            is = resource.openStream();
            Properties props = new Properties();
            props.load(is);
            version = props.getProperty("version");
        } catch (IOException ex) {
            LOGGER.warn(ex.getMessage());
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ex) {
                    LOGGER.error("Could not close InputStream", ex);
                }
            }
        }
        return version;
    }

    @Override
    public String getLatestApplicationVersion() {
        String latestVersion = null;
        String latestVersionInfoURL = "http://movie-browser.googlecode.com/svn/site/latest";
        LOGGER.info("Checking latest version info from: " + latestVersionInfoURL);
        BufferedReader in = null;
        try {
            LOGGER.info("Fetcing latest version info from: " + latestVersionInfoURL);
            URL url = new URL(latestVersionInfoURL);
            in = new BufferedReader(new InputStreamReader(url.openStream()));
            String str;
            while ((str = in.readLine()) != null) {
                latestVersion = str;
            }
        } catch (Exception ex) {
            LOGGER.error("Error fetching latest version info from: " + latestVersionInfoURL, ex);
        } finally {
            try {
                in.close();
            } catch (Exception ex) {
                LOGGER.error("Could not close inputstream", ex);
            }
        }
        return latestVersion;
    }

    @Override
    public void setRenameTitles(boolean value) {
        Map<String, String> prefs = loadPreferences();
        prefs.put(RENAME_TITLES, Boolean.valueOf(value).toString());
        savePreferences(prefs);
    }

    @Override
    public boolean getRenameTitles() {
        Map<String, String> prefs = loadPreferences();
        String value = prefs.get(RENAME_TITLES);
        return Boolean.valueOf(value);
    }

    @Override
    public void setSaveAlbumArt(boolean value) {
        Map<String, String> prefs = loadPreferences();
        prefs.put(SAVE_ALBUM_ART, Boolean.valueOf(value).toString());
        savePreferences(prefs);
    }

    @Override
    public boolean getSaveAlbumArt() {
        Map<String, String> prefs = loadPreferences();
        String value = prefs.get(SAVE_ALBUM_ART);
        return Boolean.valueOf(value);
    }

    @Override
    public String getLookAndFeelClassName() {
        Map<String, String> prefs = loadPreferences();
        return prefs.get(LOOK_AND_FEEL_PROPERTY);
    }

    @Override
    public void setLookAndFeelClassName(String className) {
        Map<String, String> prefs = loadPreferences();
        prefs.put(LOOK_AND_FEEL_PROPERTY, className);
        savePreferences(prefs);
    }

    @Override
    public boolean isServiceEnabled(String service, boolean defaultValue) {
        Map<String, String> prefs = loadPreferences();
        String value = prefs.get(serviceKey(service));
        if (value == null) {
            return defaultValue;
        } else {
            return Boolean.valueOf(value);
        }
    }

    @Override
    public void setServiceEnabled(String service, boolean value) {
        Map<String, String> prefs = loadPreferences();
        prefs.put(serviceKey(service), Boolean.toString(value));
        savePreferences(prefs);
    }

    @Override
    public List<MovieService> getEnabledServices() {
        List<MovieService> services = new ArrayList<MovieService>();
        Map<String, String> prefs = loadPreferences();
        for (MovieService service : MovieService.values()) {
            String value = prefs.get(serviceKey(service.getId()));
            if (value != null && Boolean.valueOf(value) == Boolean.TRUE) {
                services.add(service);
            }
        }
        return services;
    }

    private String getPreferredServiceName() {
        Map<String, String> prefs = loadPreferences();
        return prefs.get(PREF_SERVICE_PROPERTY);
    }

    @Override
    public MovieService getPreferredService() {
        MovieService service = MovieService.valueOf("IMDB");
        try {
            String name = getPreferredServiceName();
            if (name != null) {
                service = MovieService.valueOf(name);
            }
        } catch (IllegalArgumentException e) {
            LOGGER.error("problem looking up preferred service", e);
        }
        return service;
    }

    @Override
    public void setPreferredService(MovieService service) {
        Map<String, String> prefs = loadPreferences();
        prefs.put(PREF_SERVICE_PROPERTY, service.getId());
        savePreferences(prefs);
    }

    private String serviceKey(final String movieServiceId) {
        return SERVICE_PREFIX + movieServiceId.toLowerCase();
    }
}
