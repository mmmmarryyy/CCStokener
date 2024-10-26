package objects.util;

import objects.Galaxy;
import util.Profiler;
import util.Utils;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.logging.Level;

public final class GameInfo {

    public static void saveInfo(Galaxy galaxy, boolean post) {
        String types = System.getProperty("Server.SaveInfo", "off").toLowerCase();
        if (types.isEmpty() || "off".equalsIgnoreCase(types)) return;
        Profiler.getProfiler().start("saveinfo", galaxy.getName(), galaxy.getTurn());
        try {
            for (String type : Utils.split(types)) {
                String path = "";
                if ("txt".equalsIgnoreCase(type)) path = GameInfoText.saveInfo(galaxy, post); else if ("oldxml".equalsIgnoreCase(type)) path = GameInfoOldXml.saveInfo(galaxy, post); else if ("xml".equalsIgnoreCase(type)) path = GameInfoXml.saveInfo(galaxy, post); else Galaxy.getLogger().severe("GameInfo: unknown method for saving game info" + type);
                if (path != null && !path.isEmpty()) informWebServer(path);
            }
        } catch (Exception err) {
            Galaxy.getLogger().log(Level.SEVERE, "Save game " + galaxy.getName() + " info", err);
        } finally {
            Profiler.getProfiler().stop("saveinfo");
        }
    }

    private static void informWebServer(String path) throws IOException {
        if (!Utils.parseBoolean(System.getProperty("Server.SaveInfo.InformWebServer", "no"))) return;
        URL url = new URL(System.getProperty("Server.SaveInfo.InformWebServer.URL") + URLEncoder.encode(path, "UTF-8"));
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.getInputStream().read();
        conn.disconnect();
    }
}
