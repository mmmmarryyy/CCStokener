package treditor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TrUtils {

    public static void main(String... args) throws Exception {
        main2();
    }

    public static void main2() throws Exception {
        BufferedWriter out = new BufferedWriter(new FileWriter("F:/translations/AFA-fr-en.txt"));
        Map<String, String> afaFr = putAllInMap(new LinkedHashMap<String, String>(), new File("F:/translations/AFA-fr.txt"));
        Map<String, String> afaEn = putAllInMap(new HashMap<String, String>(), new File("F:/translations/AFA-en.txt"));
        Map<String, String> afaDescrFr = putAllInMap(new HashMap<String, String>(), new File("F:/translations/AFA-Descr-fr.txt"));
        StringBuilder lineStrb = new StringBuilder(2048);
        out.write("Source\tFran�ais\tEnglish\n");
        for (Map.Entry<String, String> entry : afaFr.entrySet()) {
            String id = entry.getKey();
            String fr = entry.getValue();
            String en = afaEn.get(id);
            String descr = afaDescrFr.get(id);
            if (descr == null) {
                descr = "[]";
            }
            lineStrb.setLength(0);
            lineStrb.append("AFA\t").append(fr).append("\t").append(en).append("\t").append(descr).append("\n");
            out.write(lineStrb.toString());
        }
        out.close();
    }

    public static void main3() throws Exception {
        BufferedWriter out = new BufferedWriter(new FileWriter("F:/translations/AFA-Descr-fr.txt"));
        Map<String, String> afaFr = putAllInMap(new LinkedHashMap<String, String>(), new File("F:/translations/AFA-fr.txt"));
        StringBuilder pageStrb = new StringBuilder(2048);
        StringBuilder lineStrb = new StringBuilder(2048);
        char[] cbuf = new char[512];
        Pattern tagPattern = Pattern.compile("<[^<>]+>");
        Pattern nlPattern = Pattern.compile("\\r?\\n");
        for (Map.Entry<String, String> entry : afaFr.entrySet()) {
            String id = entry.getKey();
            String paddedId = id;
            if (id.length() == 1) {
                paddedId = "000" + id;
            } else if (id.length() == 2) {
                paddedId = "00" + id;
            } else if (id.length() == 3) {
                paddedId = "0" + id;
            }
            URL url = new URL("http://www.vbv.ch/upload/Lexikon/e_" + paddedId + "_f.htm");
            pageStrb.setLength(0);
            readUrl(url, pageStrb, cbuf);
            int i = pageStrb.indexOf("<BR><BR>") + 8;
            int j = pageStrb.indexOf("</body>", i);
            String text = pageStrb.substring(i + 1, j - 2);
            text = tagPattern.matcher(text).replaceAll("");
            text = nlPattern.matcher(text).replaceAll(" ");
            lineStrb.setLength(0);
            lineStrb.append(id).append("=").append(text).append("\n");
            out.write(lineStrb.toString());
        }
        out.close();
    }

    public static void readUrl(URL url, StringBuilder strb, char[] cbuf) throws IOException {
        Reader in = new InputStreamReader(url.openStream());
        int rlen;
        while ((rlen = in.read(cbuf)) != -1) {
            strb.append(cbuf, 0, rlen);
        }
        in.close();
    }

    public static void save(File fileOut, Map<String, String> map) throws IOException {
        BufferedWriter out = new BufferedWriter(new FileWriter(fileOut));
        for (Map.Entry<String, String> entry : map.entrySet()) {
            out.write(entry.getKey());
            out.write('=');
            out.write(entry.getValue());
            out.write('\n');
        }
        out.close();
    }

    public static <T extends Map<String, String>> T putAllInMap(T map, Set<String> keys, String defaultValue) {
        for (String key : keys) {
            map.put(key, defaultValue);
        }
        return map;
    }

    public static Map<String, String> getCrossMap(Map<String, String> crossMap, Map<String, String> map1, Map<String, String> map2) {
        for (Map.Entry<String, String> entry1 : map1.entrySet()) {
            crossMap.put(entry1.getValue(), map2.get(entry1.getKey()));
        }
        return crossMap;
    }

    public static <T extends Map<String, String>> T putAllInMap(T map, File fileIn) throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(fileIn));
        String line;
        while ((line = in.readLine()) != null) {
            int i = line.indexOf('=');
            if (i != -1) {
                map.put(line.substring(0, i), line.substring(i + 1));
            }
        }
        in.close();
        return map;
    }

    public static Map<String, String> getReverseMap(Map<String, String> reverseMap, Map<String, String> map) {
        for (Map.Entry<String, String> entry : map.entrySet()) {
            reverseMap.put(entry.getValue(), entry.getKey());
        }
        return reverseMap;
    }

    public static List<String[]> parseTabDelimitedText(File fileIn, int nbOfColumns) throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(fileIn));
        List<String[]> list = new ArrayList<String[]>(300);
        String colPattern = "([^\\t]*)";
        StringBuilder strb = new StringBuilder(nbOfColumns * (colPattern.length() + 2));
        for (int i = 0; i < nbOfColumns; i++) {
            if (i > 0) {
                strb.append("\\t");
            }
            strb.append(colPattern);
        }
        Pattern pattern = Pattern.compile(strb.toString());
        String line;
        while ((line = in.readLine()) != null) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.matches()) {
                String[] cols = new String[nbOfColumns];
                for (int i = 0; i < nbOfColumns; i++) {
                    cols[i] = matcher.group(i + 1);
                }
                list.add(cols);
            } else if (!line.isEmpty()) {
                System.err.println(line);
            }
        }
        in.close();
        return list;
    }

    public static Set<String> addAllValuesToSet(Set<String> set, List<String[]> table, int... colIndexes) {
        for (String[] row : table) {
            for (int i = 0; i < colIndexes.length; i++) {
                set.add(row[colIndexes[i]]);
            }
        }
        return set;
    }
}
