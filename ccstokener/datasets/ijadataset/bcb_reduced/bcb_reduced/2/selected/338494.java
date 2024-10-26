package JavaGo;

import java.awt.Button;
import java.awt.Choice;
import java.awt.Event;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.TextField;
import java.io.DataInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

public class ControlGame extends Panel implements Constants {

    private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ControlGame.class);

    TextArea text;

    Frame frame_sgf;

    boolean frame_on;

    boolean control_new;

    boolean control_load;

    private URL url_dir;

    private String name_of_dir_games;

    private Button new_game;

    private Button load;

    private Button save;

    private Button refresh;

    public Button pass;

    private Button full;

    private Button floating;

    private Button first;

    private Button prev;

    private Button next;

    private Button last;

    private Button to_n;

    private TextField n;

    private Choice url_list;

    private Choice size_list;

    private Choice komi_list;

    private Choice hand_list;

    private JavaGO javago;

    ControlGame(JavaGO javago1) {
        javago = javago1;
        controlNew();
        controlMain();
        controlLoad();
        control_new = false;
        control_load = false;
        init();
        setLayout(new FlowLayout(0, 4, 0));
    }

    public void init() {
        removeAll();
        if (control_new) init_new(); else if (control_load) init_load(); else init_main();
        validate();
        text = new TextArea();
        frame_sgf = new Frame();
        frame_sgf.resize(600, 250);
        frame_sgf.setLayout(new FlowLayout(0, 0, 0));
        frame_sgf.add(text);
        frame_on = false;
    }

    public void init_main() {
        add(pass);
    }

    public void init_new() {
        add(new_game);
        add(load);
        add(save);
        add(size_list);
        add(hand_list);
        add(komi_list);
    }

    public void init_load() {
        add(new_game);
        add(load);
        add(save);
        add(url_list);
    }

    public void paint(Graphics g) {
        JavaGO.drawBackground(g, size(), this);
    }

    public void setNodeField(int i) {
        n.setText("" + i);
    }

    public void controlNew() {
        size_list = new Choice();
        komi_list = new Choice();
        hand_list = new Choice();
        size_list.addItem("size");
        size_list.addItem("19");
        size_list.addItem("18");
        size_list.addItem("17");
        size_list.addItem("16");
        size_list.addItem("15");
        size_list.addItem("14");
        size_list.addItem("13");
        size_list.addItem("12");
        size_list.addItem("11");
        size_list.addItem("10");
        size_list.addItem("9");
        komi_list.addItem("komi");
        komi_list.addItem("9.5");
        komi_list.addItem("8.5");
        komi_list.addItem("7.5");
        komi_list.addItem("6.5");
        komi_list.addItem("5.5");
        komi_list.addItem("4.5");
        komi_list.addItem("3.5");
        komi_list.addItem("2.5");
        komi_list.addItem("1.5");
        komi_list.addItem("0.5");
        komi_list.addItem("-0.5");
        komi_list.addItem("-1.5");
        komi_list.addItem("-2.5");
        komi_list.addItem("-3.5");
        komi_list.addItem("-4.5");
        komi_list.addItem("-5.5");
        komi_list.addItem("-6.5");
        komi_list.addItem("-7.5");
        komi_list.addItem("-8.5");
        komi_list.addItem("-9.5");
        hand_list.addItem("hand");
        hand_list.addItem("2");
        hand_list.addItem("3");
        hand_list.addItem("4");
        hand_list.addItem("5");
        hand_list.addItem("6");
        hand_list.addItem("7");
        hand_list.addItem("8");
        hand_list.addItem("9");
        hand_list.addItem("10");
        hand_list.addItem("11");
        hand_list.addItem("12");
        hand_list.addItem("13");
        hand_list.addItem("14");
        hand_list.addItem("15");
        hand_list.addItem("16");
        hand_list.addItem("17");
        size_list.select("size");
        hand_list.select("hand");
        komi_list.select("komi");
        new_game = new Button("New");
        load = new Button("Load");
        save = new Button("Save");
    }

    public void controlMain() {
        refresh = new Button("Refresh");
        pass = new Button("Pass");
        full = new Button("x");
        floating = new Button("o");
        first = new Button("|<");
        prev = new Button("<");
        next = new Button(">");
        last = new Button(">|");
        to_n = new Button(">#");
        n = new TextField(2);
    }

    public void controlLoad() {
        url_list = new Choice();
        String s = javago.getCodeBase().toString();
        int i = s.lastIndexOf('/');
        if (i >= 0) s = s.substring(0, i + 1);
        name_of_dir_games = s + "GOGames/";
        url_list.addItem(name_of_dir_games);
        readURL();
        url_list.addItem("http://www.mygale.org/~al62/GOGames/");
        url_list.addItem("http://stekt.oulu.fi/~suopanki/go/games/");
        url_list.addItem("file://C|/");
        url_list.addItem("file://D|/");
        url_list.addItem("file://E|/");
    }

    public boolean action(Event event, Object obj) {
        if (event.target == new_game) {
            control_load = false;
            if (control_new) {
                javago.appendTextln("New game");
                javago.newGame();
            }
            control_new = !control_new;
            init();
        } else if (event.target == load) {
            control_new = false;
            if (control_load) {
                URL url = readURL();
                if (url != null) {
                    if (!url.toString().endsWith("/")) {
                        javago.appendTextln("Load new game : " + url + " ...");
                        javago.newGame();
                        new SgfFile(javago.game, url, javago);
                        javago.appendText(" ok");
                        control_load = false;
                    }
                } else {
                    controlLoad();
                }
            } else {
                control_load = true;
            }
            init();
        } else if (event.target == save) actionSave(); else if (event.target == full) javago.fullWindow(); else if (event.target == floating) javago.floatWindow(); else if (event.target == refresh) javago.goban.repaintAll(); else if (event.target == pass) {
            javago.goban.actionPass();
        } else if (event.target == first) javago.goban.setNode(-1); else if (event.target == prev) javago.goban.setNode(-3); else if (event.target == next) javago.goban.setNode(-2); else if (event.target == last) javago.goban.setNode(-4); else if (event.target == to_n) try {
            String s = n.getText();
            Integer integer = new Integer(s);
            int i = integer.intValue();
            javago.goban.setNode(integer.intValue());
        } catch (NumberFormatException _ex) {
        }
        return true;
    }

    public void actionSave() {
        if (frame_on) {
            frame_sgf.hide();
        } else {
            String s = new String("(\n;GaMe[1] VieW[] SiZe[19] Comment[ Created by JavaGO ] ; \n");
            s = s + javago.game.SGF();
            s = s + " \n)\n";
            text.setText(s);
            frame_sgf.show();
        }
        frame_on = !frame_on;
    }

    public URL readURL() {
        String s = url_list.getSelectedItem();
        URL url;
        try {
            url = new URL(s);
            if (s.endsWith("/")) {
                url_dir = url;
                if (!setDir()) {
                    javago.appendTextln("Bad URL directory : " + url);
                    url = null;
                }
            } else {
                javago.appendTextln("Read game : " + url);
            }
        } catch (MalformedURLException malformedurlexception) {
            log.error("ControlGame.readURL:MalformedURLException" + malformedurlexception);
            url = null;
        }
        return url;
    }

    public boolean setDir() {
        javago.appendTextln("List directory : " + url_dir);
        String s1 = "";
        boolean flag = true;
        String s3;
        if (url_dir != null) s3 = url_dir.toString(); else s3 = "";
        url_list = new Choice();
        try {
            InputStream inputstream = url_dir.openStream();
            DataInputStream datainputstream = new DataInputStream(inputstream);
            String s;
            while ((s = datainputstream.readLine()) != null) {
                int i = 0;
                String s4 = s;
                while (i != -1) {
                    i = s4.indexOf("href");
                    if (i == -1) {
                        i = s4.indexOf("HREF");
                        if (i == -1) continue;
                    }
                    s4 = s4.substring(i + 5);
                    int j = s4.indexOf('"');
                    s4 = s4.substring(j + 1);
                    int k = s4.indexOf('"');
                    String s5 = s4.substring(0, k);
                    s4 = s4.substring(k + 1);
                    j = s5.indexOf("%7C");
                    char c = s3.charAt(7);
                    if (c != '|' && c != ':') c = '|';
                    if (j != -1) s5 = s5.substring(0, j) + c + s5.substring(j + 3);
                    j = s5.indexOf("%20");
                    if (j != -1) s5 = s5.substring(0, j) + " " + s5.substring(j + 3);
                    String s2 = s5;
                    if (s2.startsWith("/")) url_list.addItem(s3.substring(0, 5) + s2); else if (s2.startsWith("http") || s2.startsWith("file")) url_list.addItem(s2); else url_list.addItem(s3 + s2);
                    javago.appendText(".");
                }
            }
            javago.appendText(" ok");
            datainputstream.close();
            inputstream.close();
            url_list.select(0);
        } catch (Exception exception) {
            log.error(exception);
            flag = false;
        }
        url_list.addItem(name_of_dir_games);
        return flag;
    }

    public int gameSize() {
        String s = size_list.getSelectedItem();
        int i;
        if (s == "size" || control_load) {
            i = 19;
        } else {
            Integer integer = new Integer(s);
            i = integer.intValue();
        }
        return i;
    }

    public int Handicap() {
        String s = hand_list.getSelectedItem();
        int i;
        if (s == "hand") {
            i = 0;
        } else {
            Integer integer = new Integer(s);
            i = integer.intValue();
        }
        return i;
    }

    public double Komi() {
        String s = komi_list.getSelectedItem();
        double d;
        if (s == "komi") {
            if (Handicap() > 1) d = 0.5D; else d = 5.5D;
        } else {
            Double double1 = new Double(s);
            d = double1.doubleValue();
        }
        return d;
    }
}
