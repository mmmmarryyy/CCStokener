package server.actions;

import objects.Galaxy;
import objects.Race;
import server.IAction;
import server.OGSserver;
import util.BASE64;
import util.Utils;
import util.schedule.Schedule;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

/**
 * Author: serhiy
 * Created on Jan 26, 2009, 7:56:59 PM
 */
public class RLStart implements IAction {

    private static final int TIMEOUT = 60000;

    protected String serverID;

    private String[] gameTypePrefixes;

    private String[] gameTypeNames;

    @Override
    public void exec(String name, Galaxy galaxy) {
        serverID = OGSserver.getProperty("Racelist.Server");
        gameTypePrefixes = Utils.split(OGSserver.getProperty("Racelist.GameTypes", ""));
        gameTypeNames = new String[gameTypePrefixes.length];
        for (int i = 0; i < gameTypePrefixes.length; i++) {
            String[] strings = gameTypePrefixes[i].split(":", 2);
            gameTypePrefixes[i] = strings[0];
            if (strings.length > 1) gameTypeNames[i] = strings[1]; else gameTypeNames[i] = strings[0];
        }
        String charset = OGSserver.getProperty("Racelist.Charset", "UTF-8");
        try {
            URL url = new URL(OGSserver.getProperty("Action." + name + ".URL"));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            String userInfo = url.getUserInfo();
            if (userInfo == null) {
                String user = OGSserver.getProperty("Racelist.User");
                if (user != null) {
                    userInfo = user;
                    String password = OGSserver.getProperty("Racelist.Password");
                    if (password != null) userInfo += ':' + password;
                }
            }
            if (userInfo != null) conn.setRequestProperty("Authorization", "Basic " + new String(BASE64.encode(userInfo.getBytes())));
            conn.setConnectTimeout(TIMEOUT);
            conn.setRequestMethod("POST");
            conn.setUseCaches(false);
            conn.setAllowUserInteraction(false);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=" + charset);
            Map<String, String> params = new HashMap<String, String>();
            fillForm(params, galaxy);
            OutputStream out = conn.getOutputStream();
            boolean first = true;
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (!first) out.write('&'); else first = false;
                out.write(URLEncoder.encode(entry.getKey(), charset).getBytes("US-ASCII"));
                out.write('=');
                out.write(URLEncoder.encode(entry.getValue(), charset).getBytes("US-ASCII"));
            }
            out.flush();
            out.close();
            conn.connect();
            conn.getResponseCode();
            Galaxy.getLogger().fine("Request result: " + conn.getResponseMessage());
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                Galaxy.getLogger().log(Level.WARNING, "Can't update RaceList. Response: " + conn.getResponseCode() + ' ' + conn.getResponseMessage());
            }
            conn.disconnect();
        } catch (IOException err) {
            Galaxy.getLogger().log(Level.WARNING, "Can't update RaceList", err);
        }
    }

    protected void fillForm(Map<String, String> params, Galaxy galaxy) {
        params.put("form", "rf_start");
        params.put("server", serverID);
        params.put("game", galaxy.getName());
        params.put("day", new SimpleDateFormat("dd", Locale.ROOT).format(galaxy.saveDate));
        params.put("month", new SimpleDateFormat("MM", Locale.ROOT).format(galaxy.saveDate));
        params.put("year", new SimpleDateFormat("yyyy", Locale.ROOT).format(galaxy.saveDate));
        String gameType = "unknown";
        for (int i = 0; i < gameTypePrefixes.length; i++) if (galaxy.getName().startsWith(gameTypePrefixes[i])) {
            gameType = gameTypeNames[i];
            break;
        }
        params.put("type", gameType);
        params.put("newtype", "YES");
        params.put("options", "");
        params.put("size", String.valueOf((int) galaxy.getGeometry().getSize()));
        params.put("planets", String.valueOf(galaxy.getPlanets().length));
        Schedule sch = Schedule.getSchedule(galaxy.getGameDir());
        int[] limits = sch.getScheduleLimits();
        params.put("tpw", limits.length > 0 ? String.valueOf(sch.getScheduleItemsFrom(limits[limits.length - 1]).size()) : "");
        StringBuilder buffer = new StringBuilder();
        for (Race race : galaxy.getRaces()) {
            if (race.getTeam() != null) buffer.append(race.getTeam().getName()).append('~');
            buffer.append(race.getName()).append('\n');
        }
        params.put("racelist", buffer.toString());
    }
}
