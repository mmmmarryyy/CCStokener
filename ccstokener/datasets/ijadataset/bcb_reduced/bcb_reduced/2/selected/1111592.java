package swingextras.license;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import swingextras.icons.IconManager;

/**
 *
 * @author Joao Leal
 */
public class Licenses {

    private static final Logger logger = Logger.getLogger(Licenses.class.getName());

    /**
     * Commonly used licenses
     */
    public static enum LICENSES {

        /** GPL v3.0 */
        GPL3_0("GPL v3.0", "GNU General Public License", "Version 3, 29 June 2007", "gpl-3.0.txt", "notice-gpl-3.0.txt", "gpl-3.0.png"), /** GPL 2.0 */
        GPL2_0("GPL 2.0", "GNU General Public License", "Version 2, June 1991", "gpl-2.0.txt", "notice-gpl-2.0.txt", "gpl-2.0.png"), /** GPL 2.0 */
        LGPL3_0("LGPL 3.0", "GNU Lesser General Public License", "Version 3, 29 June 2007", "lgpl-3.0.txt", "notice-lgpl-3.0.txt", "lgpl-3.0.png"), /** LGPL 2.1 */
        LGPL2_1("LGPL 2.1", "GNU Lesser General Public License", "Version 2.1, February 1999", "lgpl-2.1.txt", "notice-lgpl-2.1.txt", "lgpl-2.1.png"), /** Apache v2.0 */
        APACHE2_0("Apache v2.0", "Apache License", "Version 2.0, January 2004", "apache-2.0.txt", "notice-apache-2.0.txt", "apache-2.0.png"), /** BSD */
        BSD("BSD", "Berkeley Software Distribution License", null, "bsd.txt", "notice-bsd.txt", "bsd.png"), /** MIT */
        MIT("MIT", "Massachusetts Institute of Technology License", null, "mit.txt", "notice-mit.txt", "mit.png");

        private String shortName;

        private String fullName;

        private String version;

        private String fulltextfile;

        private String noticetextfile;

        private String iconName;

        LICENSES(String shortName, String fullName, String version, String fulltextfile, String noticetextfile, String iconName) {
            this.shortName = shortName;
            this.fullName = fullName;
            this.version = version;
            this.fulltextfile = fulltextfile;
            this.noticetextfile = noticetextfile;
            this.iconName = iconName;
        }

        /**
         * Returns a short name used to identify this license
         * @return a short name used to identify this license
         */
        public String getShortName() {
            return shortName;
        }

        /**
         * Returns the full license name
         * @return the full license name
         */
        public String getFullName() {
            return fullName;
        }

        /**
         * Returns the version of this license
         * @return the license version
         */
        public String getVersion() {
            return version;
        }

        /**
         *
         * @param description A short description of the program
         * @param copywriteOwner The copywrite's owner
         * @param copywriteYear The copywrite's year
         * @return the license
         */
        public License getNotice(String description, String copywriteOwner, String copywriteYear) {
            String text = getNoticeText(description, copywriteOwner, copywriteYear);
            return new License(fullName, version, shortName, text, getIcon());
        }

        /**
         * Creates a license
         * @return the license
         */
        public License getLicense() {
            return new License(fullName, version, shortName, getLicenseText(), getIcon());
        }

        /**
         * Returns the custom license icon, if it exist, otherwise it returns
         * the default icon
         * @return an icon
         */
        public ImageIcon getIcon() {
            URL url = null;
            if (iconName != null) {
                url = Licenses.class.getResource(iconLocation + iconName);
            }
            if (url != null) {
                return new ImageIcon(url);
            } else {
                return IconManager.getIcon("32x32/license.png");
            }
        }

        /**
         * Creates a license text
         * @param description A short description of the program
         * @param copywriteOwner The copywrite's owner
         * @param copywriteYear The copywrite's year
         * @return returns the generated license text
         */
        public String getNoticeText(String description, String copywriteOwner, String copywriteYear) {
            URL licenseURL = Licenses.class.getResource(location + noticetextfile);
            return Licenses.getNoticeText(licenseURL, description, copywriteOwner, copywriteYear);
        }

        /**
         * Fetches the license text
         * @return the license text
         */
        public String getLicenseText() {
            return getText(Licenses.class.getResource(location + fulltextfile));
        }
    }

    ;

    private static final String location = "/swingextras/license/";

    private static final String iconLocation = "/swingextras/license/icons/";

    /**
     * Fetches text from a text file
     * @param url the location of the text file
     * @return the text contained in the text file
     */
    public static String getText(URL url) {
        String text = null;
        try {
            InputStream stream = url.openStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(stream));
            String inputLine;
            text = "";
            while ((inputLine = in.readLine()) != null) {
                text += inputLine + "\n";
            }
            in.close();
        } catch (MalformedURLException ex) {
            logger.warning(ex.getMessage());
            return null;
        } catch (IOException ex) {
            logger.warning(ex.getMessage());
            return null;
        }
        return text;
    }

    /**
     * Creates a license notice text using a template stored in a text file
     * @param license The license code number
     * @param description A short description of the program
     * @param copywriteOwner The copywrite's owner
     * @param copywriteYear The copywrite's year
     * @return the generated notice text
     */
    public static String getNoticeText(LICENSES license, String description, String copywriteOwner, String copywriteYear) {
        return getNoticeText((URL) null, description, copywriteOwner, copywriteYear);
    }

    /**
     * Creates a license text using a template stored in a text file
     * @param licenseURL The URL to the license text/content
     * @param description A short description of the program
     * @param copywriteOwner The copywrite's owner
     * @param copywriteYear The copywrite's year
     * @return the generated license text
     */
    public static String getNoticeText(URL licenseURL, String description, String copywriteOwner, String copywriteYear) {
        String license = getText(licenseURL);
        if (description == null) {
            description = "";
        }
        license = license.replaceAll("<description>", description);
        if (copywriteOwner == null) {
            copywriteOwner = "";
        }
        license = license.replaceAll("<name of author>", copywriteOwner);
        if (copywriteYear == null) {
            copywriteYear = "";
        }
        license = license.replaceAll("<year>", copywriteYear);
        return license;
    }
}
