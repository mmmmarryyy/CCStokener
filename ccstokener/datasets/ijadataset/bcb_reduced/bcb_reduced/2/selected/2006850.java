package net.sourceforge.rcontrol.model;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;

public final class Constants {

    public static final String TITLE = "RCONtrol";

    public static final String SHORTDESC = "HL/HL2 RCON Manager";

    public static final String VERSION = "0.1";

    public static final String SUBVERSION = "alpha CVS";

    public static final boolean PUBLIC_RELEASE = false;

    public static final boolean PUBLIC_WINBINARY = false;

    public static final String DEFAULT_LANGUAGE = "en";

    public static final String CFG_SUBFOLDER = TITLE;

    public static final String CFG_SERVERLIST_FILENAME = CFG_SUBFOLDER + File.separator + "settings.bin";

    public static final String SCRIPTS_SUBFOLDER = "scripts";

    public static final String LOG_SUBFOLDER = "logs";

    public static final String I18NFOLDER = "i18n";

    public static final String GAME_CSS = "Counter-Strike: Source";

    public static final int SERVER_TIMEOUT = 2000;

    public static final String OCEAN_THEME_CLASS_NAME = "javax.swing.plaf.metal.OceanTheme";

    public static final int RCON_TIMEOUT_RETRIES = 3;

    public static final int SERVER_DEFAULTPORT = 27015;

    public static final boolean SERVER_NOTIFY = true;

    public static final String OS_WINXP = "Windows XP";

    public static final int MSG_ERR = 0;

    public static final int MSG_INFO = 1;

    public static final int MSG_DEBUG = 2;

    public static final int MSG_TRACE = 3;

    /**
	 * @deprecated
	 * @return external IP
	 */
    public static String getIP() {
        String sUrl = "http://www.paecker.net/fileadmin/getip.php";
        StringBuffer sb = new StringBuffer(1024);
        try {
            URL url = new URL(sUrl);
            InputStream is = url.openStream();
            DataInputStream data = new DataInputStream(new BufferedInputStream(is));
            String line;
            while ((line = data.readLine()) != null) sb.append(line);
        } catch (Exception e) {
        }
        return sb.toString();
    }

    public static String getLocalIP() {
        String retVal = null;
        try {
            retVal = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return retVal;
    }
}
