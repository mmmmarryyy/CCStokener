package prajna.geo;

import java.awt.GridLayout;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import javax.swing.*;
import prajna.data.GeoCoord;
import prajna.data.Location;

/**
 * Simple Google Map display utility. This class pops up a Google Map centered
 * on the specified location. It loads the cells surrounding the center, so a
 * total of 9 Google map cells are displayed in a 3x3 grid.
 * 
 * @author <a href="http://www.ganae.com/edswing">Edward Swing</a>
 */
public class GoogleMapPopup extends JFrame {

    private static final long serialVersionUID = 8614872990781195271L;

    private static String key = "ABQIAAAAv_t34sZlMJhW7HagRAJj9RRiRPk7b96qT_AcyZAUnbnn80mx9RRCpKGIqR-hgpZPKNQ-2h8B9lw8gQ";

    private String googleUrlStr = "http://maps.google.com/maps/geo?output=csv&q=";

    private double lat;

    private double lon;

    private double projLat;

    private Location loc;

    private JPanel pane = new JPanel(new GridLayout(3, 3));

    private int dispLevel;

    private boolean locValid = false;

    /**
     * Create a new map popup, displaying the specified location
     * 
     * @param location the location to display
     */
    public GoogleMapPopup(Location location) {
        loc = location;
        setupGui();
    }

    /**
     * Create a new map popup, displaying the named location
     * 
     * @param locName the location name
     */
    public GoogleMapPopup(String locName) {
        loc = new Location(locName);
        setupGui();
    }

    /**
     * Determine whether Google can identify the location, and set the map to
     * the specified coordinates if the location is valid
     * 
     * @return the default map level for the desired location, or -1 if the map
     *         location is not found
     */
    private int findLocation() {
        int level = -1;
        GeoCoord center = loc.getCenter();
        if (center == null) {
            String locName = loc.getName();
            String urlString = googleUrlStr + locName.replaceAll("\\s+", "+") + "&key=" + key;
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(new URL(urlString).openStream()));
                String line = null;
                if ((line = reader.readLine()) != null) {
                    String[] values = line.split(",");
                    int status = Integer.parseInt(values[0]);
                    if (status == 200) {
                        level = Integer.parseInt(values[1]);
                        lat = Double.parseDouble(values[2]);
                        lon = Double.parseDouble(values[3]);
                        double latRads = lat * Math.PI / 180;
                        loc.setCenter(new GeoCoord(lat, lon));
                        double yTmp = Math.log(Math.tan(Math.PI / 4 + 0.5 * Math.abs(latRads)));
                        projLat = yTmp * 180 / Math.PI;
                        level = 10 - level + (int) (Math.abs(lat / 30));
                        double radAtLat = Math.cos(latRads) * 2 * Math.PI;
                        int gridSize = 131072 >> level;
                        loc.setRadius(radAtLat / gridSize);
                        locValid = true;
                    }
                }
                reader.close();
            } catch (IOException exc) {
                throw new RuntimeException("Cannot parse stream from " + urlString, exc);
            }
        } else {
            lat = center.getLatitude();
            lon = center.getLongitude();
            double yTmp = Math.log(Math.tan(Math.PI / 4 + 0.5 * Math.abs(center.getLatRadians())));
            projLat = yTmp * 180 / Math.PI;
            double radius = loc.getRadius();
            if (radius == 0) {
                loc.setRadius(1000);
            }
            double radAtLat = Math.cos(center.getLatRadians()) * 2 * Math.PI;
            double cells = radAtLat / radius;
            double log2 = Math.log(cells) / Math.log(2.0);
            level = 17 - (int) log2;
        }
        return level;
    }

    /**
     * Get the Google map image cell for the specified coordinate and level.
     * The Google map is in a square grid, with 2^(17-level) cells on each
     * side. Each successive level is a 2x zoom factor. The X coordinate and Y
     * coordinate start in the upper left corner
     * 
     * @param x the X coordinate for the image cell
     * @param y the Y coordinate for the image cell
     * @param lvl the map level
     * @return A JLabel containing the image
     */
    private JLabel getImage(int x, int y, int lvl) {
        JLabel lbl = new JLabel();
        if (y > -1) {
            try {
                URL url = new URL("http://mt0.google.com/mt?n=404&v=w2.25&x=" + x + "&y=" + y + "&zoom=" + lvl);
                ImageIcon icn = new ImageIcon(url);
                lbl = new JLabel(icn);
            } catch (Exception exc) {
                Image img = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
                lbl = new JLabel(new ImageIcon(img));
            }
        }
        return lbl;
    }

    /**
     * Get the currently displayed map level
     * 
     * @return the displayed map level
     */
    public int getMapLevel() {
        return dispLevel;
    }

    /**
     * Pop up the map display
     */
    public void popupMap() {
        int level = findLocation();
        if (level > 0) {
            popupMap(level);
        }
    }

    /**
     * Pop up the map display at the specified level.
     * 
     * @param mapLevel the map level to display
     */
    public void popupMap(int mapLevel) {
        if (!locValid) {
            findLocation();
        }
        if (locValid) {
            dispLevel = mapLevel;
            int gridSize = 131072 >> mapLevel;
            double scaledLat = (180 - projLat) / 360;
            double scaledLon = (lon + 180) / 360;
            double xGrid = scaledLon * gridSize;
            double yGrid = scaledLat * gridSize;
            int xCel = (int) xGrid;
            int yCel = (int) yGrid;
            for (int y = yCel - 1; y < yCel + 2; y++) {
                for (int x = xCel - 1; x < xCel + 2; x++) {
                    int xTru = x;
                    if (x < 0) {
                        xTru += gridSize;
                    } else if (x > gridSize) {
                        xTru -= gridSize;
                    }
                    pane.add(getImage(xTru, y, mapLevel));
                }
            }
            pack();
            setSize(400, 400);
            setVisible(true);
        }
    }

    /**
     * Set the location currently displayed by this popup map
     * 
     * @param location the location to display
     */
    public void setLocation(Location location) {
        loc = location;
        locValid = false;
    }

    /**
     * Set up GUI components
     */
    private void setupGui() {
        add(new JScrollPane(pane));
    }

    /**
     * Set the Google Map API key
     * 
     * @param keyString the Google Map API key
     */
    public static void setKey(String keyString) {
        key = keyString;
    }
}
