package edumips64.utils;

import java.util.*;
import java.io.*;
import java.net.URL;

public class CurrentLocale {

    static HashMap<String, String> en, it;

    static {
        en = new HashMap<String, String>();
        it = new HashMap<String, String>();
        try {
            loadMessages("en", en);
            loadMessages("it", it);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void loadMessages(String filename, Map<String, String> map) throws FileNotFoundException, IOException {
        String line;
        URL url = CurrentLocale.class.getResource("MessagesBundle_" + filename + ".properties");
        InputStreamReader isr = new InputStreamReader(url.openStream());
        BufferedReader br = new BufferedReader(isr);
        while ((line = br.readLine()) != null) {
            String[] l = line.split("=", 2);
            map.put(l[0].trim(), l[1].trim());
        }
        br.close();
        isr.close();
    }

    public static void setLanguage(String language) {
        Config.set("language", language);
    }

    public static String getString(String key) {
        return Config.get("language").equals("it") ? it.get(key) : en.get(key);
    }

    public static boolean isSelected(String lan) {
        return (Config.get("language").equals(lan));
    }
}
