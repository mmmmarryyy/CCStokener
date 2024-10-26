package com.dreamfabric.jsidplay;

import com.dreamfabric.jac64.*;
import com.dreamfabric.c64utils.*;
import java.applet.Applet;
import java.awt.*;
import java.awt.event.*;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.zip.*;
import javax.swing.*;

/**
 *
 *
 * @author  Joakim Eriksson (joakime@sics.se)
 * @version $Revision: 1.3 $, $Date: 2006/04/08 10:06:02 $
 */
public class JSIDPlay extends Applet implements ActionListener, JSIDListener {

    public static final boolean DEBUG = false;

    public static final boolean AUTODEBUG = false;

    public static final int PSID = 1;

    public static final int RSID = 2;

    public static final String VERSION = "0.76";

    public static final String JSID = "JSIDPlay " + VERSION + ": ";

    public static final String JSID_POST = "                         ";

    public static final int NONE = 0;

    public static final int VIEW_INTERNALS = 1;

    public static final int VIEW_CONTROLS = 2;

    public static final int VIEW_SPECTRUM = 4;

    public static final String DEFAULT_STATUS = JSID + " -= www.jac64.com =-" + JSID_POST;

    private int repaint;

    private long lastUpdate;

    boolean noapplet = false;

    boolean gui = true;

    private int viewMode = VIEW_INTERNALS | VIEW_CONTROLS | VIEW_SPECTRUM;

    String sidName = "sids/City.sid";

    String hvscBase = "";

    FileDialog fileDialog;

    JFrame window;

    JButton nextSong;

    JPanel panel;

    int currentSong = 0;

    int nxtSong;

    int volume = 100;

    C64Scroller statusLabel;

    C64Scroller c64Scroller;

    int effect = 0;

    int scrollSize = 30;

    int fadeTime = -1;

    int fadeVol = 0;

    AudioDriver audioDriver;

    private JSIDPlayer player = new JSIDPlayer() {

        public void setSongInfo(String songinfo) {
            System.out.println("Song info:" + songinfo);
            c64Scroller.setText("                                               " + " song: " + songinfo + "       " + songCount + " songs     " + " JSIDPlay " + VERSION + " by Joakim Eriksson.......        " + " more info at www.jac64.com                    ");
        }

        public void setStatus(String text) {
            if (statusLabel != null) {
                statusLabel.setText("JSIDPLAY: " + text + JSID_POST);
            } else {
                System.out.println("Status: " + text);
            }
        }
    };

    private boolean enableTimers;

    private SIDCanvas[] sidCanvas = new SIDCanvas[3];

    private PSIDCanvas psidCanvas;

    private SIDMixerDisplay mixerDisplay;

    private JPanel sidPanel;

    public String getParameter(String param) {
        if (noapplet) {
            return "";
        }
        return super.getParameter(param);
    }

    private URL getResource(String urls) {
        URL url = this.getClass().getResource(urls);
        if (url == null) try {
            url = new URL(getCodeBase().toString() + urls);
        } catch (Exception e) {
        }
        return url;
    }

    public void openWindow() {
        if (noapplet) {
            window = new JFrame();
        }
        if (viewMode != NONE) {
            sidPanel = new JPanel(new GridLayout(0, 1));
            sidPanel.setOpaque(false);
        }
        if (window != null) {
            panel = new JPanel(new GridLayout(0, 4, 0, 0));
            window.getContentPane().add(panel, BorderLayout.NORTH);
            if (sidPanel != null) window.getContentPane().add(sidPanel, BorderLayout.CENTER);
        } else {
            setLayout(new BorderLayout());
            panel = new JPanel(new GridLayout(0, 1, 0, 0));
            add(panel, BorderLayout.NORTH);
            if (sidPanel != null) add(sidPanel, BorderLayout.CENTER);
        }
        if ((viewMode & VIEW_INTERNALS) != 0) {
            if (noapplet) sidPanel.add(c64Scroller);
        }
        if ((viewMode & VIEW_CONTROLS) != 0) {
        }
        if (noapplet) {
            JButton butt = new JButton("Load SID");
            butt.addActionListener(this);
            panel.add(butt, 0);
            butt = new JButton("Init SID");
            butt.addActionListener(this);
            panel.add(butt, 0);
            nextSong = new JButton("Next Song");
            nextSong.addActionListener(this);
            panel.add(nextSong, 0);
            butt = new JButton("Reset Player");
            butt.addActionListener(this);
            panel.add(butt, 0);
            butt = new JButton("Pause Player");
            butt.addActionListener(this);
            panel.add(butt, 0);
            butt = new JButton("Effects on");
            effect = 0;
            JButton b2 = new JButton("DEBUG");
            b2.addActionListener(this);
            panel.add(b2, 0);
            b2 = new JButton("AUTO DEBUG ON");
            b2.addActionListener(this);
            panel.add(b2, 0);
            b2 = new JButton("INFO");
            b2.addActionListener(this);
            panel.add(b2, 0);
        } else {
            panel.setFont(new Font("Monospaced", Font.PLAIN, 10));
            panel.add(statusLabel);
            statusLabel.setText(DEFAULT_STATUS);
            panel.add(c64Scroller);
            c64Scroller.setText("                                      JSIDPlay " + VERSION + " by Joakim Eriksson, 2004 - 2007 " + JSID_POST);
            if ((viewMode & VIEW_SPECTRUM) != 0) {
                add(mixerDisplay, BorderLayout.SOUTH);
            }
        }
        if (window != null) {
            window.setSize(380, 450);
            window.show();
        }
        screenRefresh();
    }

    public void reset() {
        player.reset();
    }

    public void noapplet() {
        noapplet = true;
    }

    public void actionPerformed(ActionEvent ae) {
        String cmd = ae.getActionCommand();
        if (cmd.startsWith("Load")) {
            if (fileDialog == null) fileDialog = new FileDialog(window, "Select File to Load");
            fileDialog.show();
            String name = fileDialog.getDirectory() + fileDialog.getFile();
            readSIDFromFile(name);
            player.playSID();
        } else if (cmd.startsWith("Init")) {
            player.initSID();
        } else if (cmd.startsWith("Reset")) {
            System.out.println("Resetting");
            player.reset();
        } else if (cmd.startsWith("Pause")) {
            JButton b = (JButton) ae.getSource();
            b.setText("Continue...");
            player.sids.setPause(true);
        } else if (cmd.startsWith("Continue")) {
            JButton b = (JButton) ae.getSource();
            b.setText("Pause Player");
            player.sids.setPause(false);
        }
        if (cmd.startsWith("Effects")) {
            JButton b = (JButton) ae.getSource();
        } else if (cmd.startsWith("DEBUG")) {
            player.sids.dumpStatus();
            player.imon.setLevel(10);
            player.imon.setEnabled(!player.imon.isEnabled());
        } else if (cmd.startsWith("AUTO DEBUG")) {
            player.setAutoDebug();
        } else if (cmd.startsWith("Next Song")) {
            player.nextSong();
        } else if (cmd.startsWith("INFO")) {
            player.sids.dumpStatus();
        }
    }

    public void pause() {
        player.setPause(true);
        player.setStatus("*** Paused ***");
    }

    public void play() {
        player.setPause(false);
        if (player.songCount() > 0) {
            player.setStatus("song: " + (player.getSong() + 1) + "/" + player.songCount());
        } else {
            player.setStatus(DEFAULT_STATUS);
        }
    }

    public void nextSong() {
        player.nextSong();
    }

    public void previousSong() {
        player.previousSong();
    }

    public void setEffect(int effect) {
        player.setEffect(effect);
    }

    public void setVolume(int v) {
        if (v > 100) v = 100;
        if (v < 0) v = 0;
        audioDriver.setMasterVolume(volume = v);
        player.setStatus("Volume set to: " + v);
    }

    public void incVolume() {
        setVolume(volume + 10);
    }

    public void decVolume() {
        setVolume(volume - 10);
    }

    public void init() {
        audioDriver = new AudioDriverSE();
        audioDriver.init(44000, 22000);
        audioDriver.setMasterVolume(100);
        player.init(this, new SELoader(), audioDriver);
        if (!noapplet) {
            try {
                String param;
                viewMode = 0;
                if ((param = getParameter("viewmode")) != null) {
                    if ("all".equals(param)) {
                        viewMode = VIEW_CONTROLS | VIEW_INTERNALS;
                    } else if ("controls".equals(param)) {
                        viewMode = VIEW_CONTROLS;
                    } else if ("status".equals(param)) {
                        viewMode = VIEW_INTERNALS;
                    } else if ("spectrum".equals(param)) {
                        viewMode = VIEW_SPECTRUM;
                        mixerDisplay = new SIDMixerDisplay();
                    }
                }
            } catch (Exception e) {
                viewMode = VIEW_INTERNALS | VIEW_CONTROLS;
            }
        }
        String chs = getParameter("scroll-size");
        try {
            if (chs != null) {
                scrollSize = Integer.parseInt(chs);
            }
        } catch (Exception e) {
        }
        statusLabel = new C64Scroller(scrollSize);
        c64Scroller = new C64Scroller(scrollSize);
        String col = getParameter("foreground-color");
        if (col != null || col.length() > 1) {
            try {
                int cval = Integer.parseInt(col, 16);
                c64Scroller.text.setColor(1, 0xff000000 | cval);
                statusLabel.text.setColor(1, 0xff000000 | cval);
                if (mixerDisplay != null) mixerDisplay.setForeground(new Color(cval));
                panel.setForeground(new Color(cval));
                setForeground(new Color(cval));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        col = getParameter("background-color");
        if (col != null || col.length() > 1) {
            try {
                int cval = Integer.parseInt(col, 16);
                c64Scroller.text.setColor(0, 0xff000000 | cval);
                statusLabel.text.setColor(0, 0xff000000 | cval);
                if (mixerDisplay != null) mixerDisplay.setBackground(new Color(cval));
                panel.setBackground(new Color(cval));
                setBackground(new Color(cval));
            } catch (Exception e) {
            }
        }
        if (gui) {
            openWindow();
        }
    }

    public void setPlayTime(int seconds) {
        fadeTime = seconds;
    }

    public void start() {
        player.start();
        hvscBase = getParameter("hvscBase");
        String p = "";
        if ((p = getParameter("playsid")) != null && p != "") {
            final String param = p;
            new Thread(new Runnable() {

                public void run() {
                    int i = 0;
                    while (!player.sids.isReady()) {
                        player.setStatus("Warming up " + ((i++ & 1) == 1 ? "_" : " "));
                        try {
                            Thread.currentThread().sleep(100);
                        } catch (Exception ee) {
                            ee.printStackTrace();
                        }
                    }
                    int songNo = -1;
                    try {
                        songNo = Integer.parseInt(getParameter("playsong"));
                    } catch (Exception e) {
                    }
                    playSIDFromURL(param);
                    if (songNo != -1) {
                        player.playSong(songNo);
                    }
                }
            }).start();
        }
    }

    public void stop() {
        player.stop();
    }

    public boolean readSIDFromFile(String name) {
        player.reset();
        try {
            System.out.println("Loading SID " + name);
            FileInputStream reader = new FileInputStream(name);
            return player.readSID(reader);
        } catch (Exception e) {
            System.out.println("Error while opening file " + name + "  " + e);
        }
        return false;
    }

    public void playSIDFromURL(String name) {
        player.reset();
        player.setStatus("Loading song: " + name);
        URL url;
        try {
            if (name.startsWith("http")) {
                url = new URL(name);
            } else {
                url = getResource(name);
            }
            if (player.readSID(url.openConnection().getInputStream())) {
                player.playSID();
            }
        } catch (IOException ioe) {
            System.out.println("Could not load: ");
            ioe.printStackTrace();
            player.setStatus("Could not load SID: " + ioe.getMessage());
        }
    }

    public void playSIDFromHVSC(String name) {
        player.reset();
        player.setStatus("Loading song: " + name);
        URL url;
        try {
            if (name.startsWith("/")) {
                name = name.substring(1);
            }
            url = getResource(hvscBase + name);
            if (player.readSID(url.openConnection().getInputStream())) {
                player.playSID();
            }
        } catch (IOException ioe) {
            System.out.println("Could not load: ");
            ioe.printStackTrace();
            player.setStatus("Could not load SID: " + ioe.getMessage());
        }
    }

    public void sidUpdate() {
        JSIDChipemu sids = player.sids;
    }

    public void screenRefresh() {
        long diff = System.currentTimeMillis() - lastUpdate;
        if (diff < 10) return;
        lastUpdate = System.currentTimeMillis();
        JSIDChipemu sids = player.sids;
        repaint++;
        if ((viewMode & VIEW_INTERNALS) != 0) {
            if ((repaint % 4) == 0) {
                sidPanel.repaint();
            }
        }
        if (mixerDisplay != null && (repaint % 4) == 0) {
            mixerDisplay.repaint();
        }
        if ((repaint % 50) == 0) {
            if (player.songCount() > 0) {
                String text = "song " + (player.getSong() + 1) + "/" + player.songCount() + " ";
                while (text.length() < (scrollSize - 15)) text += " ";
                text += player.getPlayTime();
                player.setStatus(text);
            }
        }
        c64Scroller.updatePos();
        c64Scroller.repaint();
        System.out.println("PlayTime:" + player.getPlayTime());
        if (fadeTime > 0 && player.getPlaySec() > fadeTime) {
            fadeVol = volume;
            fadeTime = 0;
        }
        if (fadeVol > 0) {
            System.out.println("Time for fade... " + fadeVol);
            fadeVol = fadeVol - 1;
            player.sids.audioDriver.setMasterVolume(fadeVol);
            if (fadeVol == 0) {
                player.setPause(true);
            }
        }
    }

    public class C64Scroller extends JComponent {

        C64TextRenderer text;

        Image image;

        long lastTime;

        long currentTime;

        int width;

        int pos = 0;

        int speed = 10;

        int size;

        String textStr = "                                              ";

        public C64Scroller(int size) {
            this.size = size;
            text = new C64TextRenderer(player.memory, size * 2, 8);
            text.renderText("just a simple test text...");
            image = text.getImage();
            width = size * 8;
            speed = 10;
            setPreferredSize(new Dimension(size * 8, 8));
            updateText();
        }

        public void setText(String text) {
            textStr = text.toLowerCase();
            pos = 0;
            updateText();
            repaint();
        }

        private void updateText() {
            String txt = "";
            for (int i = 0, n = size * 2; i < n; i++) {
                txt += textStr.charAt((pos + i) % textStr.length());
            }
            text.renderText(txt);
            pos += size;
        }

        public void updatePos() {
            currentTime += speed;
        }

        public void paint(Graphics g) {
            int w = getWidth();
            int delta = (int) (currentTime - lastTime);
            int xpos = (delta * w) / 2000;
            if (xpos > w) {
                lastTime = currentTime;
                xpos -= w;
                updateText();
            }
            g.drawImage(image, -xpos, 0, w * 2, getHeight(), null);
        }
    }

    public static void main(String[] args) {
        JSIDPlay sp = new JSIDPlay();
        sp.noapplet();
        sp.init();
        sp.start();
    }
}
