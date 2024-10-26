package tristero.ntriple;

import tristero.util.*;
import java.io.*;
import java.util.*;
import java.net.*;

public class NTripleParser {

    public static String getName(String url) {
        Vector v = StringUtils.split(url, "/");
        return (String) v.get(v.size() - 1);
    }

    public static Vector parse(URL url) throws IOException {
        URLConnection conn = url.openConnection();
        InputStream in = conn.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        return parse(reader);
    }

    public static Vector parse(BufferedReader in) {
        Vector triples = new Vector();
        String s = null;
        try {
            s = in.readLine();
        } catch (IOException e) {
            e.printStackTrace();
            return triples;
        }
        while (s != null) {
            try {
                if (s.equals("")) {
                    s = in.readLine();
                    continue;
                }
                if (s.charAt(0) == '#') {
                    s = in.readLine();
                    continue;
                }
                Vector triple = parseTriple(s);
                triples.addElement(triple);
                s = in.readLine();
            } catch (Exception e) {
                e.printStackTrace();
                return triples;
            }
        }
        return triples;
    }

    public static Vector parseTriple(String s) {
        Vector triple = new Vector();
        Vector v = StringUtils.split(s, " ");
        for (int x = 0; x < 3; x++) {
            String str = (String) v.elementAt(x);
            Vector pair = new Vector();
            pair.addElement(determineType(str));
            pair.addElement(trim(str));
            if (x == 1) triple.addElement(trim(str)); else triple.addElement(pair);
        }
        return triple;
    }

    private static Integer determineType(String s) {
        char c = s.charAt(0);
        if (c == '"') return Triple.STRING; else if (c == '<') return Triple.RESOURCE; else return Triple.NODE;
    }

    public static String trim(String s) {
        if (s.charAt(0) == '_') return s.substring(2); else if (s.charAt(0) == '"') {
            s = s.substring(1, s.length() - 1);
            return URLDecoder.decode(s);
        } else {
            return s.substring(1, s.length() - 1);
        }
    }
}
