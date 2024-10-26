package regnumhelper.gui.map;

import regnumhelper.*;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.TimerTask;
import java.util.Timer;
import javax.imageio.ImageIO;

/**
 *
 * @author  Niels
 */
public class DrawFortStatusPanel extends GlasspanePanel {

    public static final int UNKNOWN = 0;

    public static final int ALSIUS = 1;

    public static final int IGNIS = 2;

    public static final int SYRTIS = 3;

    int[] status = new int[9];

    Timer refreshtimer = new Timer();

    boolean enabledDisplay = true;

    BufferedImage alsiusFlag = null;

    BufferedImage ignisFlag = null;

    BufferedImage syrtisFlag = null;

    BufferedImage unknownFlag = null;

    /** Creates new form DrawFortStatusPanel */
    public DrawFortStatusPanel() {
        initComponents();
        this.setOpaque(false);
        try {
            alsiusFlag = ImageIO.read(Main.class.getResource("images/alsius.png"));
            ignisFlag = ImageIO.read(Main.class.getResource("images/ignis.png"));
            syrtisFlag = ImageIO.read(Main.class.getResource("images/syrtis.png"));
            unknownFlag = ImageIO.read(Main.class.getResource("images/none.png"));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void setSettings(Settings settings, MapLocator mapLocator) {
        super.setSettings(settings, mapLocator);
        if (settings != null) {
            stopRefreshTimer();
            setServer(settings.getPlayingServer());
        } else {
            stopRefreshTimer();
        }
    }

    private void stopRefreshTimer() {
        if (refreshtimer != null) refreshtimer.cancel();
    }

    private void startRefreshTimer() {
        if (refreshtimer != null) refreshtimer.cancel();
        TimerTask tt = new TimerTask() {

            public void run() {
                refreshStatus();
            }
        };
        refreshtimer = new Timer();
        refreshtimer.schedule(tt, 500, settings.getFortStatusRefreshRate());
        this.repaint();
    }

    public void setEnabledDisplay(boolean enabled) {
        this.enabledDisplay = enabled;
        refreshStatus();
        this.repaint();
    }

    private Settings.Server server = Settings.Server.RA;

    public void setServer(Settings.Server server) {
        this.server = server;
        users = "calculating";
        stopRefreshTimer();
        startRefreshTimer();
    }

    private String getServerFortURL() {
        String url = "";
        switch(server) {
            case RA:
                url = "http://www.regnumonlinegame.com/ranking/index.php?l=5&ref=gmg&realm=0&opt=1&world=ra";
                break;
            case MUSPELL:
                url = "http://www.regnumonlinegame.com/ranking/index.php?l=5&ref=gmg&realm=0&opt=1&world=muspell";
                break;
            case NIFLHEIM:
                url = "http://www.regnumonlinegame.com/ranking/index.php?l=5&ref=gmg&realm=0&opt=1&world=niflheim";
                break;
            case HORUS:
                url = "http://www.regnumonline.com.ar/ranking/index.php?l=1&realm=0&opt=1&world=horus";
                break;
        }
        return url;
    }

    String users = "";

    public void refreshStatus() {
        if (!enabledDisplay) return;
        try {
            String url = getServerFortURL();
            BufferedReader reader = new BufferedReader(new InputStreamReader(new URL(url).openStream()));
            String data = null;
            int counter = 0;
            while ((data = reader.readLine()) != null && counter < 9) {
                status[counter] = UNKNOWN;
                if (data.matches(".*_alsius.gif.*")) {
                    status[counter] = ALSIUS;
                    counter++;
                }
                if (data.matches(".*_syrtis.gif.*")) {
                    status[counter] = SYRTIS;
                    counter++;
                }
                if (data.matches(".*_ignis.gif.*")) {
                    status[counter] = IGNIS;
                    counter++;
                }
            }
        } catch (Exception exc) {
            for (int i = 0; i < status.length; i++) status[i] = UNKNOWN;
        }
    }

    public void paintComponent(Graphics g) {
        if (!enabledDisplay) return;
        if (settings != null) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setColor(Color.red);
            g2d.setFont(g2d.getFont().deriveFont(Font.BOLD, 18));
            String serverinfo = server.name();
            g2d.drawString(serverinfo, this.getVisibleRect().x + 5, this.getVisibleRect().y + 20);
            g2d.scale(scale, scale);
            for (int i = 0; i < status.length; i++) {
                Point p = null;
                switch(i) {
                    case 0:
                        p = mapLocator.getMap().getCastleAlsiusFlagPositionOnMap();
                        break;
                    case 1:
                        p = mapLocator.getMap().getFortAggersborgFlagPositionOnMap();
                        break;
                    case 2:
                        p = mapLocator.getMap().getFortTrelleborgFlagPositionOnMap();
                        break;
                    case 3:
                        p = mapLocator.getMap().getCastleIgnisFlagPositionOnMap();
                        break;
                    case 4:
                        p = mapLocator.getMap().getFortMenirahFlagPositionOnMap();
                        break;
                    case 5:
                        p = mapLocator.getMap().getFortSamalFlagPositionOnMap();
                        break;
                    case 6:
                        p = mapLocator.getMap().getCastleSyrtisFlagPositionOnMap();
                        break;
                    case 7:
                        p = mapLocator.getMap().getFortAlgarosFlagPositionOnMap();
                        break;
                    case 8:
                        p = mapLocator.getMap().getFortHerbredFlagPositionOnMap();
                        break;
                }
                if (p != null && p.x >= 0 && p.y >= 0) {
                    paintFlag(g2d, i, p.x, p.y);
                }
            }
        }
    }

    private double scale = 1.0;

    public void setScale(double scale) {
        this.scale = scale;
        this.repaint();
    }

    private void paintFlag(Graphics2D g2d, int statusI, int x, int y) {
        if (alsiusFlag != null && ignisFlag != null && syrtisFlag != null) {
            BufferedImage flag = unknownFlag;
            x = (int) ((x - 1));
            y = (int) ((y - 20));
            switch(status[statusI]) {
                case UNKNOWN:
                    flag = unknownFlag;
                    break;
                case ALSIUS:
                    flag = alsiusFlag;
                    break;
                case IGNIS:
                    flag = ignisFlag;
                    break;
                case SYRTIS:
                    flag = syrtisFlag;
                    break;
            }
            g2d.drawImage(flag, x, y, this);
        }
    }

    private void initComponents() {
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGap(0, 400, Short.MAX_VALUE));
        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGap(0, 300, Short.MAX_VALUE));
    }
}
