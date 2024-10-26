package mp3.services;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import mp3.extras.Config;
import mp3.extras.Utilidades;
import mp3.pluginsSupport.Plugin;

/**
 *
 * @author user
 */
public class Reporter implements Service {

    private HashMap<String, Counter> map;

    private ReporterProxy proxy = new ReporterProxy();

    Reporter() {
        map = new HashMap<String, Counter>(21);
        map.put("open", new Counter());
        map.put("m2", new Counter());
        map.put("cach", new Counter());
        map.put("mp3ext", new Counter());
        map.put("editor1", new Counter());
        map.put("editor2", new Counter());
        map.put("m3u", new Counter());
        map.put("pls", new Counter());
        map.put("mmlf", new Counter());
        map.put("mmlfc", new Counter());
        map.put("coms", new Counter());
        map.put("dplay", new Counter());
        map.put("cplay", new Counter());
        map.put("win", new Counter());
        map.put("lin", new Counter());
        map.put("mac", new Counter());
        map.put("others", new Counter());
        map.put("cli", new Counter());
        map.put("normal", new Counter());
        map.put("server", new Counter());
        map.put("nodef", new Counter());
    }

    public void addPluginCount(Plugin plug) {
        String plugName = "plugin--" + plug.getMenuPath() + "--" + Double.toString(plug.getVersion());
        if (!map.containsKey(plugName)) {
            map.put(plugName, new Counter());
        }
        map.get(plugName).add();
    }

    public boolean addCount(String key) {
        Counter c = map.get(key);
        if (c == null) return false; else {
            c.add();
            return true;
        }
    }

    private void clearCount() {
        for (Counter c : map.values()) {
            c.clear();
        }
    }

    public boolean sendData() {
        boolean nosend;
        String id = null;
        try {
            nosend = Boolean.parseBoolean(Config.getConfig().get("NoSendReports"));
        } catch (NullPointerException ex) {
            nosend = Config.defaultNoSendReports;
        } catch (IOException ex) {
            nosend = Config.defaultNoSendReports;
        }
        if (!nosend) id = Utilidades.getId(true); else {
            Logger.getLogger(Reporter.class.getName()).config("Reporter will not send data");
        }
        if (id == null) return true; else {
            boolean ok = proxy.sendData(map);
            clearCount();
            return ok;
        }
    }

    @Override
    public String getServiceName() {
        return this.getClass().getName();
    }

    private class ReporterProxy {

        public boolean sendData(HashMap<String, Counter> m) {
            try {
                String data = parseData(m);
                if (sendDataToServer(data)) {
                    String[] moreData = getDataFromFile();
                    if (moreData != null) {
                        for (String s : moreData) {
                            if (!sendDataToServer(s)) {
                                if (!sendDataToFile(s)) {
                                    return false;
                                }
                            }
                        }
                        return true;
                    } else {
                        return true;
                    }
                } else {
                    Logger.getLogger(Reporter.class.getName()).warning("Server seems unreachable so we will try to store data in a file");
                    if (sendDataToFile(data)) return true; else return false;
                }
            } catch (IOException ex) {
                Logger.getLogger(Reporter.class.getName()).log(Level.SEVERE, null, ex);
                return false;
            }
        }

        private boolean sendDataToServer(String dataParsed) throws IOException {
            URL url = null;
            HttpURLConnection http = null;
            try {
                url = new URL("http://jmusicmanager.sourceforge.net/jmmm_db_process.php");
                http = (HttpURLConnection) url.openConnection();
                http.setRequestMethod("POST");
                http.setUseCaches(false);
                http.setDoOutput(true);
                http.setDoInput(true);
                http.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                http.setRequestProperty("Content-Length", Integer.toString(dataParsed.getBytes().length));
                DataOutputStream wr = new DataOutputStream(http.getOutputStream());
                wr.writeBytes(dataParsed);
                wr.flush();
                wr.close();
            } catch (IOException ex) {
                Logger.getLogger(Reporter.class.getName()).log(Level.SEVERE, null, ex);
                http.disconnect();
                return false;
            }
            if (http != null) {
                BufferedReader bf = new BufferedReader(new InputStreamReader(http.getInputStream()));
                String s = bf.readLine();
                bf.close();
                if (s != null && (s.startsWith("inserted") || s.startsWith("updated"))) {
                    http.disconnect();
                    return true;
                } else {
                    http.disconnect();
                    return false;
                }
            } else {
                http.disconnect();
                return false;
            }
        }

        private boolean sendDataToFile(String dataParsed) {
            try {
                FileWriter fw = new FileWriter(Utilidades.getExecutionFolder() + File.separatorChar + "dprox", true);
                fw.append(dataParsed);
                fw.append(System.getProperty("line.separator"));
                fw.flush();
                fw.close();
                return true;
            } catch (IOException ex) {
                Logger.getLogger(Reporter.class.getName()).log(Level.SEVERE, null, ex);
                return false;
            }
        }

        private String[] getDataFromFile() {
            ArrayList<String> lines = new ArrayList<String>();
            File f = new File(Utilidades.getExecutionFolder() + File.separatorChar + "dprox");
            if (!f.canRead()) return null;
            try {
                FileReader fr = new FileReader(f);
                BufferedReader bf = new BufferedReader(fr);
                String line = bf.readLine();
                while (line != null) {
                    lines.add(line);
                    line = bf.readLine();
                }
                bf.close();
                fr.close();
                if (!f.delete()) Logger.getLogger(Reporter.class.getName()).severe("dprox file hasn´t been deleted");
                return lines.toArray(new String[lines.size()]);
            } catch (IOException ex) {
                Logger.getLogger(Reporter.class.getName()).log(Level.SEVERE, null, ex);
                return null;
            }
        }

        /**
         * We need to add some data to the hash map
         * @param m map to be parsed
         * @return 
         */
        private String parseData(HashMap<String, Counter> m) throws UnsupportedEncodingException {
            StringBuilder sb = new StringBuilder(200);
            sb.append("id=");
            sb.append(URLEncoder.encode(Utilidades.getId(true), "UTF-8"));
            sb.append("&ver=");
            sb.append(URLEncoder.encode(Utilidades.actualVersion, "UTF-8"));
            int i = 0;
            for (String key : m.keySet()) {
                if (!key.startsWith("plugin--")) {
                    sb.append("&").append(key).append("=");
                    sb.append(URLEncoder.encode(m.get(key).getString(), "UTF-8"));
                } else {
                    String[] splitted = key.split("--");
                    sb.append("&pluginName").append(i).append("=");
                    sb.append(URLEncoder.encode(splitted[1], "UTF-8"));
                    sb.append("&pluginVersion").append(i).append("=");
                    sb.append(URLEncoder.encode(splitted[2], "UTF-8"));
                    sb.append("&pluginCount").append(i).append("=");
                    sb.append(URLEncoder.encode(m.get(key).getString(), "UTF-8"));
                    i++;
                }
            }
            return sb.toString();
        }
    }

    private class Counter {

        private int count = 0;

        public void add() {
            count++;
        }

        public void substract() {
            count--;
        }

        public int get() {
            return count;
        }

        public String getString() {
            return Integer.toString(count);
        }

        public void clear() {
            count = 0;
        }
    }
}
