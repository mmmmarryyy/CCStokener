package tools.updater;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import application.Neki;

/**
 * @author kreed
 *
 */
public class VersionTool {

    static final String URI = "http://neki.sourceforge.net/nekiversion.txt";

    /**
	 * 	 Check if new Updates are available
	 *  @return
	 *  -1 :	localVersion is newer than webversion (shouldn't happen ;-)
	 *   0 : 	localVersion and webversion are identic
	 *   1 :	localVersion is older than webversion - an update is recommended
	 * 
	 */
    public static int checkForNewVersion() {
        int result = 0;
        String webVersion = VersionTool.downloadWebVersionString(VersionTool.URI);
        webVersion = webVersion.trim();
        System.out.println(":: VersionTool - checkForNewVersion from web: [" + webVersion + "]");
        result = VersionTool.compareVersions(webVersion);
        return result;
    }

    /**
	 * Downloads the nekiversion string from given location
	 * @param address
	 * @return
	 */
    public static String downloadWebVersionString(String address) {
        StringBuffer stb = new StringBuffer();
        URLConnection conn = null;
        InputStream in = null;
        try {
            URL url = new URL(address);
            conn = url.openConnection();
            in = conn.getInputStream();
            byte[] buffer = new byte[1024];
            int numRead;
            long numWritten = 0;
            while ((numRead = in.read(buffer)) != -1) {
                for (int i = 0; i < numRead; i++) {
                    stb.append((char) buffer[i]);
                }
                numWritten += numRead;
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            return stb.toString();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ioe) {
            }
        }
        return stb.toString();
    }

    /**
	 * Returns the state of a given Version
	 * States : 
	 * 	-1 :	localVersion is newer than webversion (shouldn't happen ;-)
	 *   0 : 	localVersion and webversion are identic
	 *   1 :	localVersion is older than webversion - an update is recommended
	 * @param webversion
	 * @return -1 zero or 1
	 */
    public static int compareVersions(String webVersion) {
        String localVersion = Neki.VERSION;
        int locFirst = getFirstPart(localVersion);
        int webFirst = getFirstPart(webVersion);
        if (webFirst == locFirst) {
            int locLast = getLastPart(localVersion);
            int webLast = getLastPart(webVersion);
            if (locLast == webLast) {
                return 0;
            } else if (webLast > locLast) {
                return 1;
            } else {
                return -1;
            }
        } else if (webFirst > locFirst) {
            return 1;
        } else {
            return -1;
        }
    }

    /**
	 * Utility for Version Parsing
	 * @param ver
	 * @return
	 */
    private static int getFirstPart(String ver) {
        int idx = ver.indexOf('.');
        int number = 0;
        try {
            number = Integer.parseInt(ver.substring(0, idx));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return number;
    }

    /**
	 * Utility for Version Parsing
	 * @param ver
	 * @return
	 */
    private static int getLastPart(String ver) {
        int idx = ver.indexOf('.');
        int number = 0;
        try {
            number = Integer.parseInt(ver.substring(idx + 1, ver.length()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return number;
    }

    /**
	 * @param args
	 */
    public static void main(String[] args) {
        System.out.println(VersionTool.checkForNewVersion());
    }
}
