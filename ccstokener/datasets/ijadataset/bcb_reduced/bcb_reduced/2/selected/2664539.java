package net.sf.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import net.sf.utils.MyBufferedInputStream;
import net.sf.utils.Utils;

public class KeggmapCoord extends SwingWorker<List<Image>, String> {

    private String map_id;

    private HashMap<String, ECCoord> ecCoords;

    private Image mapImage;

    private KeggmapPanel keggmapPanel;

    private PreloadProgresser progresser;

    private static Pattern rect = Pattern.compile("area shape=rect\\tcoords=(\\d+),(\\d+),(\\d+),(\\d+).*(\\d+\\.\\d+\\.\\d+\\.\\d+)");

    private static Image noImage = createOffscreenImage("No image available");

    public KeggmapCoord() throws IOException {
        super();
        ecCoords = new HashMap<String, ECCoord>();
    }

    public KeggmapCoord(String mapId) throws IOException {
        this();
        map_id = mapId;
    }

    public boolean load(MyBufferedInputStream in) {
        String line = null;
        try {
            if (in.isNull()) return false;
            while ((line = Utils.readLine(in)) != null) {
                String[] t = line.split("\\t+");
                if (t.length < 3) continue;
                Matcher matcher;
                if ((matcher = rect.matcher(t[0])).find()) {
                    ecCoords.put(t[0], new ECCoord(t[2], matcher.group(1), matcher.group(2), matcher.group(3), matcher.group(4)));
                }
            }
            in.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean loadWeb(KeggmapPanel keggmapPanel, PreloadProgresser progresser) throws InterruptedException, InvocationTargetException {
        this.keggmapPanel = keggmapPanel;
        this.progresser = progresser;
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                execute();
            }
        });
        return true;
    }

    public boolean load(URL url) {
        String line = null;
        try {
            URLConnection yc = url.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream()));
            while ((line = in.readLine()) != null) {
                Matcher matcher;
                if ((matcher = rect.matcher(line)).find()) {
                    ecCoords.put(matcher.group(5), new ECCoord(matcher.group(5), matcher.group(1), matcher.group(2), matcher.group(3), matcher.group(4)));
                }
            }
            in.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }
        return true;
    }

    private static Image createOffscreenImage(String str) {
        BufferedImage image = new BufferedImage(640, 480, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setBackground(Color.WHITE);
        g2.setColor(Color.BLACK);
        g2.setFont(new Font("Arial", Font.PLAIN, 28));
        g2.drawString(str, 40, 30);
        return image;
    }

    public boolean loadImg(MyBufferedInputStream in) {
        try {
            if (in.isNull()) return false;
            if (mapImage == null) mapImage = ImageIO.read(in);
            in.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean loadImg(URL in) {
        try {
            if (mapImage == null) mapImage = ImageIO.read(in);
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }
        return true;
    }

    public void link(String ec_number, HashMap<String, ArrayList<KeggmapCoord>> ecTrace) {
        if (ecTrace.containsKey(ec_number)) {
            ArrayList<KeggmapCoord> coords = ecTrace.get(ec_number);
            coords.add(this);
        } else {
            ArrayList<KeggmapCoord> coords = new ArrayList<KeggmapCoord>();
            coords.add(this);
            ecTrace.put(ec_number, coords);
        }
    }

    @Override
    public String toString() {
        return map_id;
    }

    public Image getMapImage() {
        return mapImage != null ? mapImage : noImage;
    }

    public HashMap<String, ECCoord> getEcCoords() {
        return ecCoords;
    }

    @Override
    protected List<Image> doInBackground() throws Exception {
        List<Image> image = new ArrayList<Image>();
        try {
            loadImg(new URL("http://www.genome.jp/kegg/pathway/map/" + map_id + ".png"));
            load(new URL("http://www.genome.jp/kegg-bin/show_pathway?" + map_id));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        image.add(mapImage);
        return image;
    }

    @Override
    protected void done() {
        keggmapPanel.repaint();
        if (progresser != null) progresser.exit();
    }

    @Override
    protected void process(List<String> chunks) {
        super.process(chunks);
    }
}
