package de.mpii.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import de.mpii.Config;

public class Uio {

    private static String _read(BufferedReader in) throws Exception {
        StringBuilder sbuilder = new StringBuilder();
        String line = null;
        while ((line = in.readLine()) != null) {
            sbuilder.append(line);
            sbuilder.append('\n');
        }
        return sbuilder.toString();
    }

    public static String read(File f) throws Exception {
        return _read(new BufferedReader(new FileReader(f)));
    }

    public static String read(InputStream x) throws Exception {
        return _read(new BufferedReader(new InputStreamReader(x)));
    }

    public static String read(String file) throws Exception {
        return read(new File(file));
    }

    public static String read(URL url) throws Exception {
        String filename = Integer.toString(url.toString().hashCode());
        boolean cached = false;
        File dir = new File(Config.CACHE_PATH);
        for (File file : dir.listFiles()) {
            if (!file.isFile()) continue;
            if (file.getName().equals(filename)) {
                filename = file.getName();
                cached = true;
                break;
            }
        }
        File file = new File(Config.CACHE_PATH, filename);
        if (Config.USE_CACHE && cached) return read(file);
        System.out.println(">> CACHE HIT FAILED.");
        InputStream in = null;
        try {
            in = url.openStream();
        } catch (Exception e) {
            System.out.println(">> OPEN STREAM FAILED: " + url.toString());
            return null;
        }
        String content = read(in);
        save(file, content);
        return content;
    }

    public static void save(File file, String content) throws Exception {
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(file)));
        out.print(content);
        out.flush();
        out.close();
    }
}
