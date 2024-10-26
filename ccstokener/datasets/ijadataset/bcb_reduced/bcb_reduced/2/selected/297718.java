package net.sf.intltyper.lib;

import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.net.*;

/**
 * A list of Unicode characters and their Unicode names.
 */
public class UnicodeList {

    private List chars = null;

    /**
     * Creates a new UnicodeList from an URL.
     */
    protected UnicodeList(URL url) {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new GZIPInputStream(url.openStream())));
            String line;
            line = br.readLine();
            chars = new ArrayList();
            while ((line = br.readLine()) != null) {
                String[] parts = GUIHelper.split(line, ";");
                if (parts[0].length() >= 5) continue;
                if (parts.length < 2 || parts[0].length() != 4) {
                    System.out.println("Strange line: " + line);
                } else {
                    if (parts.length > 10 && parts[1].equals("<control>")) {
                        parts[1] = parts[1] + ": " + parts[10];
                    }
                    try {
                        Integer.parseInt(parts[0], 16);
                        chars.add(parts[0] + parts[1]);
                    } catch (NumberFormatException ex) {
                        System.out.println("No number: " + line);
                    }
                }
            }
            br.close();
        } catch (FileNotFoundException ex) {
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Returns the unicode name of a character
     */
    public String getName(char c) {
        if (chars == null) return "No unicode database found.";
        String name = "0000" + Integer.toHexString(c).toUpperCase();
        name = name.substring(name.length() - 4);
        for (Iterator it = chars.iterator(); it.hasNext(); ) {
            String data = (String) it.next();
            if (data.substring(0, 4).equals(name)) {
                return data.substring(4);
            }
        }
        return "Not found.";
    }

    /**
     * Searches for unicode names containing specific words.
     */
    public String search(String words) {
        if (chars == null) return "No unicode database found.";
        String[] matches = GUIHelper.split(words.toUpperCase(), " ");
        for (int i = 0; i < matches.length; i++) {
            if (matches[i].startsWith("*")) {
                matches[i] = matches[i].substring(1);
            } else {
                matches[i] = " " + matches[i];
            }
            if (matches[i].endsWith("*")) {
                matches[i] = matches[i].substring(0, matches[i].length() - 1);
            } else {
                matches[i] = matches[i] + " ";
            }
        }
        StringBuffer sb = new StringBuffer();
        outer: for (Iterator it = chars.iterator(); it.hasNext(); ) {
            String data = (String) it.next();
            String tosearch = " " + data.substring(4) + " ";
            for (int i = 0; i < matches.length; i++) {
                if (tosearch.indexOf(matches[i]) == -1) continue outer;
            }
            sb.append((char) Integer.parseInt(data.substring(0, 4), 16));
            if (sb.length() > 128) break;
        }
        return sb.toString();
    }
}
