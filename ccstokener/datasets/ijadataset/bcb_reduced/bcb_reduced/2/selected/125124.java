package JavaGo;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

public class SgfFile implements Constants {

    private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(SgfFile.class);

    SgfFile(Game game1, URL url, JavaGO javago1) {
        javago = javago1;
        game = game1;
        int i = 0;
        if (i > 0) log.debug("SgfFile.SgfFile()");
        try {
            InputStream inputstream = url.openStream();
            DataInputStream datainputstream = new DataInputStream(inputstream);
            String s;
            while ((s = datainputstream.readLine()) != null) {
                if (i > 1) log.debug("SgfFile.SgfFile : " + s);
                i_start = 0;
                for (int j = 0; j < s.length(); j++) {
                    char c = s.charAt(j);
                    if (i > 2) log.debug("SgfFile.SgfFile : " + c);
                    if (Character.isUpperCase(c) && !inside) {
                        n++;
                        if (n == 1) c1 = c; else if (n == 2) c2 = c;
                    } else if (c == '[') {
                        inside = true;
                        i_start = j + 1;
                    } else if (c == ']') {
                        String s1;
                        if (c1 == 0) s1 = ""; else if (c2 == 0) s1 = "" + c1; else s1 = "" + c1 + c2;
                        param = param + s.substring(i_start, j);
                        sgfCommand(s1, param);
                        init();
                    } else if (c == ';') init();
                }
                if (i_start != -1) param = param + s.substring(i_start, s.length());
            }
            game.setNode(-1);
            datainputstream.close();
            inputstream.close();
            return;
        } catch (MalformedURLException malformedurlexception) {
            javago.appendTextln("Bad URL file : " + url);
            log.error("MalformedURLException: " + malformedurlexception);
            return;
        } catch (IOException ioexception) {
            javago.appendTextln("Problem reading URL file : " + url);
            log.error("IOException: " + ioexception);
            return;
        }
    }

    public void init() {
        inside = false;
        n = 0;
        c1 = '\0';
        c2 = '\0';
        i_start = -1;
        last = "";
        param = "";
    }

    public void sgfCommand(String s, String s1) {
        if (s.equals("B") || s.equals("W")) {
            if (s1.equals("tt")) {
                game.actionPass();
                return;
            }
            if (s.equals(last)) game.actionPass();
            int i = "abcdefghijklmnopqrs".indexOf(s1.charAt(0));
            int j = "abcdefghijklmnopqrs".indexOf(s1.charAt(1));
            game.actionMove(i, j);
            last = s;
            return;
        }
        if (s.equals("HA")) {
            Integer integer = new Integer(s1);
            int k = integer.intValue();
            game.setHandicaps(k);
            game.addInfo("Handicap " + k);
            return;
        }
        if (s.equals("C")) {
            game.addInfo("Comment : " + s1);
            return;
        }
        if (!s.equals("WL") && !s.equals("BL") && !s.equals("")) game.addInfo(s + " : " + s1);
    }

    char c1;

    char c2;

    int n;

    boolean inside;

    int i_start;

    String last;

    Game game;

    String param;

    JavaGO javago;
}
