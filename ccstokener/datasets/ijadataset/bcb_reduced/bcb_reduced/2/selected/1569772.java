package de.hirte.schachanalyse.connector;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import de.hirte.schachanalyse.model.Player;
import de.hirte.schachanalyse.model.feld.Farbe;
import de.hirte.schachanalyse.model.feld.Spielfeld;
import de.hirte.schachanalyse.model.figuren.Bauer;
import de.hirte.schachanalyse.model.figuren.Dame;
import de.hirte.schachanalyse.model.figuren.Figur;
import de.hirte.schachanalyse.model.figuren.Koenig;
import de.hirte.schachanalyse.model.figuren.Laeufer;
import de.hirte.schachanalyse.model.figuren.Pferd;
import de.hirte.schachanalyse.model.figuren.Turm;
import de.hirte.schachanalyse.model.moves.ConcreteMove;

public class SchachArenaConnector {

    private static HashMap<String, Figur> db = new HashMap<String, Figur>();

    static {
        SchachArenaConnector.db.put("pics_fig/tsb.gif", new Bauer(Farbe.SCHWARZ));
        SchachArenaConnector.db.put("pics_fig/tst.gif", new Turm(Farbe.SCHWARZ));
        SchachArenaConnector.db.put("pics_fig/tsp.gif", new Pferd(Farbe.SCHWARZ));
        SchachArenaConnector.db.put("pics_fig/tsl.gif", new Laeufer(Farbe.SCHWARZ));
        SchachArenaConnector.db.put("pics_fig/tsk.gif", new Koenig(Farbe.SCHWARZ));
        SchachArenaConnector.db.put("pics_fig/tsd.gif", new Dame(Farbe.SCHWARZ));
        SchachArenaConnector.db.put("pics_fig/twb.gif", new Bauer(Farbe.WEISS));
        SchachArenaConnector.db.put("pics_fig/twt.gif", new Turm(Farbe.WEISS));
        SchachArenaConnector.db.put("pics_fig/twp.gif", new Pferd(Farbe.WEISS));
        SchachArenaConnector.db.put("pics_fig/twl.gif", new Laeufer(Farbe.WEISS));
        SchachArenaConnector.db.put("pics_fig/twk.gif", new Koenig(Farbe.WEISS));
        SchachArenaConnector.db.put("pics_fig/twd.gif", new Dame(Farbe.WEISS));
    }

    public SchachArenaConnector() {
    }

    public static void main(String[] argv) {
        try {
            System.out.println(SchachArenaConnector.getLatestedMatches("bossiq"));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public static List<URL> getLatestedMatches(String user) throws MalformedURLException {
        String baseUrl = "http://www.schacharena.de/new/";
        URL url = new URL("http://www.schacharena.de/new/spielerstatistik.php?name=" + user);
        StringBuffer sb = null;
        try {
            sb = SchachArenaConnector.downloadHTTPPage(url);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        String pattern = "verlauf_db_new\\.php\\?name=\\w+&gedreht=[01]&nr=\\d+";
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(sb.toString());
        ArrayList<URL> ret = new ArrayList<URL>(10);
        while (m.find()) {
            ret.add(new URL(baseUrl + m.group()));
        }
        return ret;
    }

    /**
	 * @return
	 */
    public Spielfeld getSpielfeld(URL url) {
        Spielfeld f = null;
        try {
            StringBuffer sb = SchachArenaConnector.downloadHTTPPage(url);
            String[] moves = SchachArenaConnector.parseMoves(sb);
            List<ConcreteMove> moveList = SchachArenaConnector.readJSFields(moves);
            f = new Spielfeld();
            f.setupNewGame(new Player(Farbe.WEISS, "P1"), new Player(Farbe.SCHWARZ, "P2"));
            for (ConcreteMove cm : moveList) {
                f.move(cm);
            }
        } catch (Exception e) {
            e.printStackTrace();
            f = new Spielfeld();
        }
        return f;
    }

    private static StringBuffer downloadHTTPPage(URL url) throws Exception {
        URLConnection con = url.openConnection();
        con.setReadTimeout(0);
        StringBuffer sb = new StringBuffer();
        BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String line = null;
        while (null != (line = br.readLine())) {
            sb.append(line);
        }
        br.close();
        return sb;
    }

    private static String[] parseMoves(StringBuffer sb) {
        String regex = "fld\\[[0-9]+\\]=\"[A-Ha-h][1-8]\";fig\\[[0-9]+\\]=\"[ws0][btlsdk0]\";zug\\[[0-9]+\\]=[0-9]+;";
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(sb.toString());
        ArrayList<String> ret = new ArrayList<String>(100);
        while (m.find()) {
            ret.add(m.group());
        }
        return ret.toArray(new String[0]);
    }

    private static String[][] readFields(String[] imgs) {
        String[][] ret = new String[8][8];
        String regBild = "pics_fig/t[ws0][btlskd0]\\.gif";
        String regId = "id=\"[A-Ha-h][1-8]\"";
        Pattern patBild = Pattern.compile(regBild);
        Pattern patId = Pattern.compile(regId);
        for (String s : imgs) {
            String src = null;
            String id = null;
            char col = 0;
            char row = 0;
            Matcher m1 = patBild.matcher(s);
            if (m1.find()) {
                src = m1.group();
            }
            Matcher m2 = patId.matcher(s);
            if (m2.find()) {
                id = m2.group();
                col = id.charAt(4);
                row = id.charAt(5);
            }
            int iRow = SchachArenaConnector.getRowFromChar(row);
            int iCol = SchachArenaConnector.getColFromChar(col);
            ret[iRow][iCol] = src;
        }
        return ret;
    }

    private static List<ConcreteMove> readJSFields(String[] jsRows) {
        List<ConcreteMove> ret = null;
        int cntMoves = (jsRows.length / 2);
        if ((jsRows.length % 2) != 0) return new ArrayList<ConcreteMove>();
        ret = new ArrayList<ConcreteMove>(cntMoves);
        String regPos = "fld\\[[0-9]+\\]=\"[A-Ha-h][1-8]\"";
        Pattern patPos = Pattern.compile(regPos);
        for (int i = 0; i < cntMoves; i++) {
            String posVon = "", posBis = "";
            Matcher m = patPos.matcher(jsRows[i * 2]);
            if (m.find()) posVon = m.group();
            m = patPos.matcher(jsRows[(i * 2) + 1]);
            if (m.find()) posBis = m.group();
            int colFrom = SchachArenaConnector.getColFromChar(posVon.charAt(posVon.length() - 3));
            int rowFrom = SchachArenaConnector.getRowFromChar(posVon.charAt(posVon.length() - 2));
            int colTo = SchachArenaConnector.getColFromChar(posBis.charAt(posBis.length() - 3));
            int rowTo = SchachArenaConnector.getRowFromChar(posBis.charAt(posBis.length() - 2));
            ret.add(new ConcreteMove(colFrom, rowFrom, colTo, rowTo, null));
        }
        return ret;
    }

    private static Spielfeld buildField(String[][] f) {
        Spielfeld board = new Spielfeld();
        for (int row = 0; row < Spielfeld.MAX_ROWS; row++) {
            for (int col = 0; col < Spielfeld.MAX_COLS; col++) {
                board.set(row, col, SchachArenaConnector.db.get(f[row][col]));
            }
        }
        return board;
    }

    private static int getColFromChar(char c) {
        switch(c) {
            case 'A':
                return 0;
            case 'B':
                return 1;
            case 'C':
                return 2;
            case 'D':
                return 3;
            case 'E':
                return 4;
            case 'F':
                return 5;
            case 'G':
                return 6;
            case 'H':
                return 7;
        }
        return -1;
    }

    private static int getRowFromChar(char c) {
        switch(c) {
            case '1':
                return 0;
            case '2':
                return 1;
            case '3':
                return 2;
            case '4':
                return 3;
            case '5':
                return 4;
            case '6':
                return 5;
            case '7':
                return 6;
            case '8':
                return 7;
        }
        return -1;
    }
}
