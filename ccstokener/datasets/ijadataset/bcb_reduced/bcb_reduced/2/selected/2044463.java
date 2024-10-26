package data;

import java.awt.Color;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import presentation.WirelessRedstoneInstaller;

public class RepositoryHandler {

    public String rootUrl;

    public String url;

    public RepositoryHandler(String url) {
        this.rootUrl = url;
        this.url = url;
    }

    public void setVersion(String path) {
        if (path.contains(";")) {
            String[] pathData = path.split(";");
            path = pathData[1];
        }
        url = rootUrl + "/" + path;
    }

    public boolean testConnection() {
        return getFile(rootUrl + "/" + "index.html") != null;
    }

    public List<String> getModFileList(Mod mod) {
        byte[] data = getFile(url + "/" + mod.name + ".list");
        String[] file = new String(data).split("\\\n");
        List<String> out = new ArrayList<String>();
        for (String s : file) {
            out.add(s);
        }
        return out;
    }

    public String[] getVersionList() {
        byte[] data = getFile(rootUrl + "/version.list");
        return new String(data).split("\\\n");
    }

    public Map<String, Mod> getModList() {
        byte[] data = getFile(url + "/mods.list");
        String[] file = new String(data).split("\\\n");
        Map<String, Mod> out = new TreeMap<String, Mod>();
        for (String s : file) {
            String[] modData = new String(s).split(";");
            if (modData.length != 3) continue;
            Mod mod = new Mod(modData[0], modData[1], Integer.parseInt(modData[2]));
            out.put(mod.name, mod);
        }
        return fillModDependencies(out);
    }

    private Map<String, Mod> fillModDependencies(Map<String, Mod> mods) {
        for (String modName : mods.keySet()) {
            byte[] data = getFile(url + "/" + modName + ".dependencies");
            String[] file = new String(data).split("\\\n");
            for (String s : file) {
                String[] modData = new String(s).split(";");
                if (modData.length != 2) continue;
                mods.get(modName).addDependency(mods.get(modData[0]), Integer.parseInt(modData[1]));
            }
        }
        return mods;
    }

    public byte[] getFile(String file, boolean try2) {
        URLConnection urlConn;
        URL url;
        try {
            url = new URL(file);
            urlConn = url.openConnection();
            urlConn.setDoInput(true);
            urlConn.setUseCaches(false);
            int contentLength = urlConn.getContentLength();
            InputStream raw = urlConn.getInputStream();
            BufferedInputStream in = new BufferedInputStream(raw);
            byte[] data = new byte[contentLength];
            int bytesRead = 0;
            int offset = 0;
            while (offset < contentLength) {
                bytesRead = in.read(data, offset, data.length - offset);
                if (bytesRead == -1) break;
                offset += bytesRead;
            }
            in.close();
            return data;
        } catch (Exception e) {
            e.printStackTrace();
            if (try2) {
                JOptionPane.showMessageDialog(null, "Unable to get file from server " + file + "!", "ERROR", JOptionPane.ERROR_MESSAGE);
            } else {
                System.out.println("Try fetching again!");
                byte[] trie2 = getFile(file, true);
                return trie2;
            }
        }
        return null;
    }

    public byte[] getFile(String file) {
        return getFile(file, false);
    }

    public boolean testConnection(JLabel label) {
        boolean b = testConnection();
        if (!b) {
            label.setForeground(Color.RED);
            label.setText("Unable to connect to server on port 80!");
        } else {
            label.setForeground(Color.BLACK);
        }
        return b;
    }
}
