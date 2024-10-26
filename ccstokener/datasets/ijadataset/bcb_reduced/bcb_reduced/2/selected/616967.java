package org.p2s.core;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import org.p2s.data.Track;
import org.p2s.lib.Base64;
import org.p2s.lib.RC4;
import org.p2s.lib.XmlLoader;
import org.p2s.lib.XmlNode;

public class HypemMp3Stream {

    public static byte[] toBinArray(String hexStr) {
        byte bArray[] = new byte[hexStr.length() / 2];
        for (int i = 0; i < (hexStr.length() / 2); i++) {
            byte firstNibble = Byte.parseByte(hexStr.substring(2 * i, 2 * i + 1), 16);
            byte secondNibble = Byte.parseByte(hexStr.substring(2 * i + 1, 2 * i + 2), 16);
            int finalByte = (secondNibble) | (firstNibble << 4);
            bArray[i] = (byte) finalByte;
        }
        return bArray;
    }

    public static String decrypt(String in) throws Exception {
        String key = "a905fbc8325ddecf";
        byte src2[] = Base64.decode(in);
        byte src[] = toBinArray(new String(src2));
        RC4 rc4 = new RC4();
        rc4.setKey(key.getBytes());
        byte dest[] = new byte[src.length];
        rc4.decrypt(src, 0, dest, 0, src.length);
        return new String(dest);
    }

    public static ArrayList search(String query) throws Exception {
        ArrayList list = new ArrayList();
        String url = "http://hypem.com/playlist/search/" + query + "/xml/1/list.xspf";
        HttpURLConnection conn = (HttpURLConnection) (new URL(url)).openConnection();
        conn.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 1.1.4322; .NET CLR 2.0.50727)");
        XmlNode node = XmlLoader.load(conn.getInputStream());
        XmlNode tracks[] = node.getFirstChild("trackList").getChild("track");
        for (int i = 0; i < tracks.length; i++) {
            String location = decrypt(tracks[i].getFirstChild("location").getText());
            String annotation = tracks[i].getFirstChild("annotation").getText().replaceAll("[\r\n]", "");
            list.add(location);
            System.out.print("found in Hypem: ");
            System.out.print(annotation);
            System.out.print(", ");
            System.out.println(location);
        }
        return list;
    }

    public static String clean(String q) {
        q = q.toLowerCase();
        q = " " + q + " ";
        q = q.replaceAll(" the ", " ");
        q = q.replaceAll(" and ", " ");
        q = q.replaceAll(" in ", " ");
        q = q.replaceAll(" to ", " ");
        q = q.replaceAll(" \\([^\\)]*\\) ", " ");
        q = q.replaceAll("[^a-zA-Z0-9]", " ");
        q = q.replaceAll(" +", " ");
        return q.trim();
    }

    public static InputStream getHypemStream(String artist, String track, String filename) throws Exception {
        String query = artist + " " + track;
        query = clean(query);
        System.out.println("searching: " + query);
        query = query.replace(" ", "+");
        ArrayList list = HypemMp3Stream.search(query);
        System.out.println("found: " + list.size());
        if (list.size() == 0) return null;
        String t = (String) list.get(0);
        System.out.println("getStream: " + t);
        HttpURLConnection conn = (HttpURLConnection) (new URL(t)).openConnection();
        conn.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 1.1.4322; .NET CLR 2.0.50727)");
        return new InputStreamProxy(conn.getInputStream(), filename);
    }

    public static ArrayList search(String artist, String track, String filename) throws Exception {
        String query = artist + " " + track;
        query = clean(query);
        System.out.println("searching: " + query);
        query = query.replace(" ", "+");
        ArrayList list = search(query);
        return list;
    }
}
