package net.ponec.pekiline.resources;

import java.awt.Image;
import java.net.URL;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import javax.swing.ImageIcon;

/**
 * Provides Images and other resources.
 * @author Pavel Ponec
 */
public class ResourceProvider {

    /** Cascade Style Sheet */
    public static final String FILE_CSS = "style.css";

    /** Localized text properties. */
    public static final String TEXT_PROPERTIES = "text.properties";

    /** A Report XSL Style Sheet */
    public static String REPORT_BASE = "ReportBase.xsl";

    public static String REPORT_BASE2 = "ReportBase2.xsl";

    /** Icon s */
    public static String LOGO = "logo.png";

    public static String LOGO_TRY = "logoTray.gif";

    public static String SORT = "sort.png";

    public static final String IMG_ADD = "_add.png";

    public static final String IMG_APPLICATION_DOUBLE = "_application_double.png";

    public static final String IMG_APPLICATION_LIST = "_application_view_list.png";

    public static final String IMG_ARROW_DOWN = "_arrow_down.png";

    public static final String IMG_ARROW_UNDO = "_arrow_undo.png";

    public static final String IMG_DELETE = "_delete2.png";

    public static final String IMG_DETAIL = "_zoom_in.png";

    public static final String IMG_HELP = "_help.png";

    public static final String IMG_LEARNING = "_learning.png";

    public static final String IMG_INFORMATION = "_information.png";

    public static final String IMG_NEXT = "_next.png";

    public static final String IMG_NEW_TEST = "_newTest.png";

    public static final String IMG_REPORT = "_report.png";

    public static final String IMG_WRENCH = "_wrench.png";

    public static final String IMG_OK = "_tick.png";

    public static final String IMG_CANCEL = "_cross.png";

    public static final String IMG_EMPTY = "_empty.png";

    public static final String IMG_HOME_PAGE = "_page_world.png";

    public static final String IMG_RE_TRANSLATE = "_re_translate.png";

    public static final String IMG_MOVE_UP = "_move_up.png";

    public static final String IMG_MOVE_DOWN = "_move_down.png";

    /**
     * Creates a new instance of ResourceProvider
     */
    public ResourceProvider() {
    }

    /** Returns a URL of resource. */
    public URL getUrl(String fileName) {
        String path = getClass().getPackage().getName().replace('.', '/');
        path = "/" + path + "/" + fileName;
        URL result = getClass().getResource(path);
        return result;
    }

    /** Returns an Icon from a file name. */
    public ImageIcon getIcon(String name, boolean hideIcon) {
        return hideIcon ? null : getIcon(name);
    }

    /** Returns an Icon from a file name. */
    public ImageIcon getIcon(String name) {
        final ImageIcon result = new ImageIcon(getUrl(name));
        return result;
    }

    /** Returns an Image from a file name. */
    public Image getImage(String name) {
        final Image result = getIcon(name).getImage();
        return result;
    }

    /** Returns Localization of the key */
    public String getText(String key, Locale locale) {
        try {
            URL url = getUrl(TEXT_PROPERTIES);
            PropertyResourceBundle bundle = new PropertyResourceBundle(url.openStream());
            return bundle.getString(key);
        } catch (Exception e) {
            return "[" + key + "]";
        }
    }
}
