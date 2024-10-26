package org.maestroframework.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class FileUtils {

    public static String getExtension(String filename) {
        String extension = "";
        if (filename.contains("\\.")) extension = filename.substring(filename.lastIndexOf("\\."));
        return extension;
    }

    public static boolean isExtension(String filename, String[] extensions) {
        for (String ext : extensions) {
            if (filename.endsWith(ext)) return true;
        }
        return false;
    }

    public static List<String> readListFile(URL url) {
        List<String> names = new ArrayList<String>();
        if (url != null) {
            InputStream in = null;
            try {
                in = url.openStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                String line = "";
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("#")) {
                        names.add(line);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                try {
                    if (in != null) in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return names;
    }
}
