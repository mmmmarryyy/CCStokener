package de.guidoludwig.jtrade.install;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Properties;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;
import de.guidoludwig.jtrade.ErrorMessage;

/**
 * Properties used by the application
 * 
 * @author <a href="mailto:jtrade@gigabss.de">Guido Ludwig</a>
 * @version $Revision: 1.9 $
 */
public class JTradeProperties {

    public static final JTradeProperties INSTANCE = new JTradeProperties();

    public static final String DATA_DIRECTORY = "data.directory";

    public static final String HTML_OUTPUT_DIRECTORY = "export.html.directory";

    public static final String HTML_OVERVIEW_FILE = "export.html.filename.overview";

    public static final String HTML_XSL_OVERVIEW = "export.xsl.html.overview";

    public static final String HTML_XSL_SHOW_DETAIL = "export.xsl.html.show.detail";

    public static final String TXT_XSL_SHOW_INFO_FILE = "export.xsl.txt.show.infoFile";

    public static final String SQL_ALL_FILE = "export.sql.filename.all";

    public static final String SQL_SPLIT = "export.sql.split";

    public static final String PRINT_ARTWORK_FONT_MAIN = "print.artwork.font.main";

    public static final String PRINT_ARTWORK_FONT_SETLIST = "print.artwork.font.setlist";

    public static final String HTML_PAGE_TITLE = "export.html.pageTitle";

    public static final String USER_EMAIL = "user.email";

    public static final String START_MAXIMIZED = "ui.start.maximized";

    public static final String DATE_FORMAT_DISPLAY = "dateFormat.display";

    public static final String DATE_FORMAT_EDIT = "dateFormat.edit";

    public static final String DEFAULT_SHOW_EXPORT_DIR = "export.directory";

    public static final String COLOR_TOOLBAR_BACKGROUND = "color.toolbar.background";

    public static final String COLOR_TOOLBAR_MIRROR = "color.toolbar.background.mirror";

    private Properties defaultProperties;

    private Properties userProperties;

    /**
     * avoid instantiation
     */
    private JTradeProperties() {
        defaultProperties = new Properties();
        userProperties = new Properties();
        File userFile = new File(System.getProperty("user.dir") + File.separator + "jtrade.properties");
        if (userFile.exists()) {
            try {
                userProperties.load(new FileInputStream(userFile));
            } catch (FileNotFoundException e) {
                ErrorMessage.handle(e);
            } catch (IOException e) {
                ErrorMessage.handle(e);
            }
        }
        URL url = ClassLoader.getSystemResource("resources/jtrade.properties");
        if (url == null) {
            ErrorMessage.handle(new NullPointerException("URL for resources/jtrade.properties not found"));
        } else {
            try {
                defaultProperties.load(url.openStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public String getProperty(String key) {
        String userValue = userProperties.getProperty(key);
        return (userValue == null ? defaultProperties.getProperty(key) : userValue);
    }

    public boolean isUserSpecific(String key) {
        return userProperties.getProperty(key) != null;
    }

    public String getHome() {
        return System.getProperty("user.dir");
    }

    public boolean startMaximized() {
        String mx = getProperty(START_MAXIMIZED);
        boolean rv = Boolean.valueOf(mx).booleanValue();
        return rv;
    }

    public DateFormat getEditDateFormat() {
        return getDateFormat(getProperty(DATE_FORMAT_EDIT), DateFormat.SHORT);
    }

    public DateFormat getDisplayDateFormat() {
        return getDateFormat(getProperty(DATE_FORMAT_DISPLAY), DateFormat.DEFAULT);
    }

    private DateFormat getDateFormat(String pattern, int def) {
        if (pattern == null) {
            return DateFormat.getDateInstance(def);
        }
        return new SimpleDateFormat(pattern, Locale.getDefault());
    }

    public int getSQLSplit() {
        String split = getProperty(JTradeProperties.SQL_SPLIT);
        if (StringUtils.isBlank(split)) {
            return 0;
        }
        try {
            return Integer.valueOf(split).intValue();
        } catch (Exception e) {
            return 0;
        }
    }

    public File getDefaultExportDirectory() {
        String dir = getProperty(JTradeProperties.DEFAULT_SHOW_EXPORT_DIR);
        if (StringUtils.isBlank(dir)) {
            return new File("export");
        }
        File file = new File(dir);
        if (!file.exists() | !file.isDirectory()) {
            return new File("export");
        }
        return file;
    }

    public Color getToolBarBackground() {
        String color = getProperty(JTradeProperties.COLOR_TOOLBAR_BACKGROUND);
        if (StringUtils.isBlank(color)) {
            return Color.DARK_GRAY;
        }
        try {
            return Color.decode("0x" + color);
        } catch (NumberFormatException e) {
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info("Color " + color + " could not be parsed!");
            return Color.DARK_GRAY;
        }
    }

    public Color getToolBarMirror() {
        String color = getProperty(JTradeProperties.COLOR_TOOLBAR_MIRROR);
        if (StringUtils.isBlank(color)) {
            return getToolBarBackground();
        }
        try {
            return Color.decode("0x" + color);
        } catch (NumberFormatException e) {
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info("Color " + color + " could not be parsed!");
            return getToolBarBackground();
        }
    }
}
