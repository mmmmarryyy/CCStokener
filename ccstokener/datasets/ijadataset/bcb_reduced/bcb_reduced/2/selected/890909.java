package evetrader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Posts the trade data to a remote server
 * @author esumner
 *
 */
public class RoutePoster implements Runnable {

    /** Stores info on what files have been sent, not worth repeating the same file :D */
    private Set sent = new HashSet();

    StringBuffer data;

    /**
	 * Causes the thread to sleep for the specified number of seconds
	 * @param seconds
	 */
    private void sleep(int seconds) {
        try {
            Thread.sleep(1000 * seconds);
        } catch (InterruptedException e) {
        }
    }

    static Map fileMap = new HashMap();

    public static void visitAllFiles(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                visitAllFiles(new File(dir, children[i]));
            }
        } else {
            fileMap.put(dir.getName(), dir);
        }
    }

    public void run() {
        while (true) {
            String path = EveTrader.getEvePath();
            Map fileMap = TradeFinder.getProcessingFiles(new File(path), null, null);
            Set entries = fileMap.keySet();
            Iterator i = entries.iterator();
            while (i.hasNext()) {
                File f = (File) fileMap.get(i.next());
                if (f.getName().endsWith(".txt")) {
                    postFile(f);
                }
                sleep(5);
            }
            sleep(60);
        }
    }

    /**
	 * Adds a parameter to the http request.
	 * @param name
	 * @param contents
	 */
    private void addParam(String name, String contents) {
        String append = "";
        try {
            append = URLEncoder.encode(name, "UTF-8") + "=" + URLEncoder.encode(contents, "UTF-8");
        } catch (UnsupportedEncodingException e) {
        }
        if (data == null) {
            data = new StringBuffer();
            data.append(append);
        } else {
            data.append("&");
            data.append(append);
        }
    }

    /**
	 * Sends the file to the server
	 * @param f
	 * @return boolean indicating if the post was sucessful
	 */
    private boolean postFile(File f) {
        try {
            if ((!EveTrader.isRemoteActive()) || sent.contains(f.getName())) {
                return false;
            }
            String urlString = EveTrader.getRemoteURL();
            data = null;
            URL url = new URL(urlString);
            URLConnection conn;
            addParam("filename", f.getName());
            addParam("modified", "" + f.lastModified());
            addParam("version", TradeFinder.version);
            addParam("time", "" + System.currentTimeMillis());
            if (TradeFinder.playerName != null) {
                addParam("player", TradeFinder.playerName);
            } else {
                addParam("player", "Unknown");
            }
            addParam("postfile", "");
            conn = url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(data.toString());
            FileReader fr = new FileReader(f);
            BufferedReader in = new BufferedReader(fr);
            String str;
            int idx = 0;
            while ((str = in.readLine()) != null) {
                if (idx != 0) {
                    wr.write(str + '\n');
                }
                idx++;
            }
            wr.flush();
            sent.add(f.getName());
            try {
                BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line;
                while ((line = rd.readLine()) != null) {
                }
                rd.close();
            } catch (Exception ex) {
            }
            wr.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
        return true;
    }
}
