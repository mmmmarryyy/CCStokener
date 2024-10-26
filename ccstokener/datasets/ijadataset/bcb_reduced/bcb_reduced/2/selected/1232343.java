package net.warl0ck.mjjp;

import java.io.*;
import java.net.*;
import java.util.Vector;
import javax.swing.JOptionPane;
import net.warl0ck.mjjp.MJTypes.*;

public class MJLoader {

    private Vector<String> file = new Vector<String>();

    private Vector<String> nfo = new Vector<String>();

    private String locale;

    public MJLoader(String adr) {
        try {
            URL url = new URL(adr);
            String ln;
            BufferedReader lnr = new BufferedReader(new InputStreamReader(url.openStream()));
            while (true) {
                ln = lnr.readLine();
                if (ln == null) break;
                ln = ln.trim();
                if (!ln.substring(0, 1).equals("#")) file.add(ln);
            }
            lnr.close();
        } catch (MalformedURLException e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Read error", JOptionPane.ERROR_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Read error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public MJFile parseFile(MJParams params) {
        MJFile f = new MJFile(params);
        setLoc(params.getLoc());
        for (String line : file) {
            String[] l = line.split(":");
            switch(l.length) {
                case 2:
                    if (l[0].equalsIgnoreCase("Info")) {
                        if (l.length > 1) nfo.add(l[1]); else nfo.add("");
                        f.setInfo(nfo);
                    }
                    break;
                case 3:
                    if (l[0].equalsIgnoreCase("Place")) {
                        f.addName(l[1], l[2]);
                    }
                    if (l[0].equalsIgnoreCase("Hand")) {
                        MJHand hand = new MJHand(getLoc());
                        hand.setWind(l[1]);
                        if (l[2].length() > 0) {
                            String[] tails = l[2].split("\\s+");
                            for (String tail : tails) hand.add(new MJTail(tail, getLoc()));
                            hand.sort();
                            f.setHand(hand);
                        }
                    }
                    if (l[0].equalsIgnoreCase("Open")) {
                        MJHand open = new MJHand(getLoc());
                        open.setWind(l[1]);
                        if (l[2].length() > 0) {
                            String[] tails = l[2].split("\\s+");
                            for (String tail : tails) open.add(new MJTail(tail, getLoc()));
                            f.setOpen(open);
                        }
                    }
                    if (l[0].equalsIgnoreCase("Discard")) {
                        MJDiscard disc = new MJDiscard();
                        disc.setWind(l[1]);
                        if (l[2].length() > 0) {
                            String[] tails = l[2].split("\\s+");
                            for (String tail : tails) disc.add(new MJTail(tail, getLoc()));
                            f.setDiscard(disc);
                        }
                    }
                    if (l[0].equalsIgnoreCase("Flower")) {
                        MJHand flower = new MJHand(getLoc());
                        flower.setWind(l[1]);
                        if (l[2].length() > 0) {
                            String[] tails = l[2].split("\\s+");
                            for (String tail : tails) flower.add(new MJTail(tail, getLoc()));
                            f.setFlower(flower);
                        }
                    }
                    break;
                case 4:
                    if (l[0].equalsIgnoreCase("Game")) {
                        MJLog log = new MJLog();
                        if (l[3].length() > 0) {
                            log.setWind(l[1]);
                            log.setAction(l[2]);
                            log.setTail(l[3]);
                        }
                        f.addLog(log);
                    }
                    break;
                case 5:
                    if (l[0].equalsIgnoreCase("Game")) {
                        MJLog log = new MJLog();
                        if (l[3].length() > 0) {
                            log.setWind(l[1]);
                            log.setAction(l[2]);
                            log.setTail(l[3]);
                            log.setAdv(l[4]);
                        }
                        f.addLog(log);
                    }
                    break;
                case 6:
                case 7:
                case 8:
                    if (l[0].equalsIgnoreCase("Game")) {
                        MJLog log = new MJLog();
                        if (l[3].length() > 0) {
                            log.setWind(l[1]);
                            log.setAction(l[2]);
                            log.setTail(l[3]);
                            log.setAdv(l[4]);
                            log.setSuit(l[5]);
                            if (l.length > 6) {
                                log.setHand(l[6]);
                                if (l.length == 8) log.setClosed(true); else log.setClosed(false);
                            }
                        }
                        f.addLog(log);
                    }
                    break;
                default:
                    break;
            }
        }
        return f;
    }

    public void setLoc(String locale) {
        this.locale = locale;
    }

    public String getLoc() {
        return locale;
    }
}
