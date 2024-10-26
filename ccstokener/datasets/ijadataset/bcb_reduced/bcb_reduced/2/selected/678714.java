package net.sourceforge.mapcraft.editor;

import net.sourceforge.mapcraft.map.*;
import net.sourceforge.mapcraft.map.Map;
import net.sourceforge.mapcraft.map.elements.Area;
import net.sourceforge.mapcraft.map.elements.Path;
import net.sourceforge.mapcraft.map.elements.Terrain;
import net.sourceforge.mapcraft.map.elements.Thing;
import net.sourceforge.mapcraft.map.interfaces.ITileSet;
import net.sourceforge.mapcraft.utils.ImageUtils;
import net.sourceforge.mapcraft.MapCraft;
import javax.swing.*;
import java.awt.*;
import java.awt.image.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.awt.geom.Line2D;
import java.awt.geom.GeneralPath;

/**
 * A component which displays a Map as part of a GUI.
 * The map data is held in the Map object. Should be
 * extended in order to give editing functions (for
 * instance).
 *
 * @author  Samuel Penn (sam@bifrost.demon.co.uk)
 * @version $Revision: 1.81 $
 */
public class MapViewer extends JPanel {

    protected Map map;

    protected int x_offset;

    protected int y_offset;

    protected String tileSize = "medium";

    protected int tileXSize = 30;

    protected int tileYSize = 35;

    protected int tileYOffset = 20;

    protected int iconWidth = 30;

    protected int iconHeight = 35;

    protected Toolkit toolkit = null;

    protected Image[] icons = null;

    protected String imagePath = "images";

    protected IconSet iconSet = null;

    protected IconSet thingSet = null;

    protected IconSet featureSet = null;

    protected IconSet effectSet = null;

    private boolean showGrid = true;

    private boolean showLargeGrid = true;

    private boolean showThings = true;

    private boolean showFeatures = true;

    private boolean showCoasts = true;

    private boolean showRivers = true;

    private boolean showRoads = true;

    private boolean showAreas = true;

    private static final short HIGHLIGHT_ICON = 0;

    protected Properties properties;

    protected ViewProperties views[];

    protected int view = 0;

    protected MapCraft application;

    protected ITileSet currentSet = null;

    protected void log(int level, String message) {
        System.out.println(message);
    }

    protected void debug(String message) {
        log(4, message);
    }

    protected void info(String message) {
        log(3, message);
    }

    protected void warn(String message) {
        log(2, message);
    }

    protected void error(String message) {
        log(1, message);
    }

    protected void fatal(String message) {
        log(0, message);
    }

    /**
     * Image filter which turns a rectangle into a hexagon.
     * Filters on an image, forcing any pixels outside the hexagonal region
     * to be transparent. The visible hexagon will be in to the top left
     * hand corner of the image, which often results in the bottom part of
     * a rectangular image being blank.
     * 
     * @author Samuel Penn.
     */
    protected class HexFilter extends RGBImageFilter {

        private int width;

        private int height;

        private int h;

        private double SQRTHREE;

        public HexFilter(int width, int height) {
            this.width = width;
            this.height = height;
            canFilterIndexColorModel = true;
            SQRTHREE = Math.sqrt(3);
            h = (int) ((SQRTHREE / 4) * width + 0.5);
        }

        public int filterRGB(int x, int y, int rgb) {
            int dx = x;
            int dy = y;
            if (y > h * 2) {
                return 0;
            }
            if (x > width / 2) {
                dx = width - x;
            }
            if (y > h) {
                dy = (h * 2) - y;
            }
            int a = (int) (SQRTHREE * dx + 0.5);
            if ((h - dy) > a) {
                rgb = 0;
            }
            return rgb;
        }
    }

    /**
     * Inner class which holds properties of each view on the image.
     * A view is a scaling, so generally a map will have several view
     * scales, each with a different set of icons.
     */
    protected class ViewProperties {

        private Properties properties;

        private String viewName;

        private String viewShape;

        private String path;

        private int iconHeight;

        private int iconWidth;

        private int tileHeight;

        private int tileWidth;

        private int tileOffset;

        private int fontSizeSmall;

        private int fontSizeMedium;

        private int fontSizeLarge;

        private int fontSizeHuge;

        public static final int USMALL = 0;

        public static final int XXSMALL = 0;

        public static final int XSMALL = 0;

        public static final int SMALL = 1;

        public static final int MEDIUM = 2;

        public static final int LARGE = 3;

        public static final int XLARGE = 4;

        public static final int XXLARGE = 4;

        public static final int PALETTE = 2;

        public static final int EDITICON = 3;

        /**
         * Get the integer value of the name view property.
         * Return the default value if the properties aren't
         * set up, or the named value does not exist.
         *
         * @param name      Name of property to retrieve.
         * @param dflt      Default value if property not found.
         *
         * @return          Value of property found, or dflt.
         */
        private int getIntProperty(String name, int dflt) {
            int value = dflt;
            try {
                String s = properties.getProperty(name, "" + dflt);
                value = Integer.parseInt(s);
            } catch (Exception e) {
            }
            return value;
        }

        public ViewProperties(String basePath, String baseFile) throws Exception {
            FileInputStream input = null;
            String file = basePath + "/" + baseFile + ".properties";
            properties = new Properties();
            try {
                URL url = MapViewer.class.getResource(file);
                properties.load(url.openStream());
                viewName = (String) properties.get("view.name");
                viewShape = (String) properties.get("view.shape");
                path = basePath + "/" + (String) properties.get("icon.path");
                iconHeight = getIntProperty("icon.height", 96);
                iconWidth = getIntProperty("icon.width", 96);
                fontSizeSmall = getIntProperty("font.small.size", 10);
                fontSizeMedium = getIntProperty("font.medium.size", 12);
                fontSizeLarge = getIntProperty("font.large.size", 16);
                fontSizeHuge = getIntProperty("font.huge.size", 20);
                if (viewShape.equals("Hexagonal")) {
                    tileHeight = (int) (Math.sqrt(3.0) / 2.0 * iconWidth);
                    tileWidth = (int) (iconWidth * 3.0 / 4.0);
                    tileOffset = (int) (tileHeight / 2.0);
                } else {
                    tileHeight = iconHeight;
                    tileWidth = iconWidth;
                    tileOffset = 0;
                }
            } catch (Exception e) {
                error("Cannot load properties from file [" + file + "]");
                throw e;
            }
        }

        public String toString() {
            return viewName + " (" + iconWidth + "x" + iconHeight + ")";
        }

        public String getPath() {
            return path;
        }

        /**
         * The name of the view, normally a descriptive name for the
         * size of the icons, such as 'Small' or 'Medium'.
         */
        public String getName() {
            return viewName;
        }

        /**
         * The shape used by the icons, either 'Square' or 'Hexagonal'.
         */
        public String getShape() {
            return viewShape;
        }

        public boolean isHexagonal() {
            return viewShape.equals("Hexagonal");
        }

        public boolean isSquare() {
            return viewShape.equals("Square");
        }

        /**
         * The actual physical height of each icon, in pixels.
         */
        public int getIconHeight() {
            return iconHeight;
        }

        /**
         * The actual physical width of each icon, in pixels.
         */
        public int getIconWidth() {
            return iconWidth;
        }

        /**
         * The height of a tile. For square maps, this is identical to the
         * icon height. For hexagonal maps, it may be different.
         */
        public int getTileHeight() {
            return tileHeight;
        }

        /**
         * The width of a tile. For square maps, this is identical to the
         * icon width. For hexagonal maps, it is smaller, since columns of
         * hexagons will overlap each other.
         */
        public int getTileWidth() {
            return tileWidth;
        }

        /**
         * Vertical offset of odd numbered columns. Zero for square maps,
         * positive (downwards offset) for hexagonal maps. Normally equal
         * to about half the height of a hexagonal tile. Note that the
         * first column is zero, and hence even.
         */
        public int getTileOffset() {
            return tileOffset;
        }

        public int getSmallFont() {
            return fontSizeSmall;
        }

        public int getMediumFont() {
            return fontSizeMedium;
        }

        public int getLargeFont() {
            return fontSizeLarge;
        }

        public int getHugeFont() {
            return fontSizeHuge;
        }
    }

    /**
     * Basic constructor. Creates an empty map.
     */
    public MapViewer() {
        super(true);
        toolkit = Toolkit.getDefaultToolkit();
    }

    public void setApplication(MapCraft mapcraft) {
        application = mapcraft;
    }

    private void prepareImage(Image image) {
        int count = 3;
        prepareImage(image, -1, -1, this);
        return;
    }

    /**
     * Read in all icons for a particular set. Size information is obtained
     * from the current view profile. If this is a hexagonal map, then
     * ensure that each icon is filtered through the Square->Hex filter.
     */
    protected IconSet readIcons(String name, TerrainSet set) {
        IconSet iconSet = new IconSet(name);
        Iterator iter = set.iterator();
        HexFilter filter = new HexFilter(views[view].getIconWidth(), views[view].getIconHeight());
        if (iter == null || !iter.hasNext()) {
            warn("No iterator");
            return null;
        }
        iconSet = new IconSet(map.getName());
        while (iter.hasNext()) {
            Terrain t = (Terrain) iter.next();
            if (t != null) {
                int id = t.getId();
                Image icon = null;
                Image scaled = null;
                int x = views[view].getIconWidth();
                int y = views[view].getIconHeight();
                if (name.equals("things")) {
                    x *= 1.5;
                    y *= 1.5;
                }
                try {
                    if (t.getImagePath().startsWith("#")) {
                        ImageUtils iu = new ImageUtils();
                        icon = iu.createImage(x, y, t.getImagePath());
                    } else {
                        String path = views[view].getPath() + "/" + t.getImagePath();
                        URL url = MapViewer.class.getResource(path);
                        icon = toolkit.getImage(url);
                    }
                    if (set.isAnySize()) {
                        while (x == -1 || y == -1) {
                            prepareImage(icon);
                            x = icon.getWidth(this);
                            y = icon.getHeight(this);
                        }
                        x = (x * views[view].getIconWidth()) / 96;
                        y = (y * views[view].getIconHeight()) / 96;
                    }
                    scaled = icon.getScaledInstance(x, y, Image.SCALE_SMOOTH);
                    if (views[view].isHexagonal() && !set.isAnySize()) {
                        int h = (int) ((Math.sqrt(3) / 2) * x);
                        filter = new HexFilter(x, h);
                        icon = createImage(new FilteredImageSource(scaled.getSource(), filter));
                        iconSet.add(id, icon);
                        prepareImage(icon);
                    } else {
                        iconSet.add(id, scaled);
                        prepareImage(scaled);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        iconSet.prepareImages(this);
        return iconSet;
    }

    protected IconSet readEffectIcons() {
        IconSet iconSet = new IconSet("effects");
        HexFilter filter = new HexFilter(views[view].getIconWidth(), views[view].getIconHeight());
        String[] icons = { "highlight" };
        for (int i = 0; i < icons.length; i++) {
            String name = icons[i] + ".png";
            String path = views[view].getPath() + "/effects/" + name;
            URL url = MapViewer.class.getResource(path);
            Image icon = toolkit.getImage(url);
            Image scaled = null;
            int x = -1, y = -1;
            if (icon == null) {
                warn("Null highlight image icon found");
            }
            x = views[view].getIconWidth();
            y = views[view].getIconHeight();
            scaled = icon.getScaledInstance(x, y, Image.SCALE_SMOOTH);
            if (views[view].isHexagonal()) {
                int h = (int) ((Math.sqrt(3) / 2) * x);
                filter = new HexFilter(x, h);
                icon = createImage(new FilteredImageSource(scaled.getSource(), filter));
                iconSet.add((short) i, icon);
                prepareImage(icon);
            } else {
                iconSet.add((short) i, scaled);
                prepareImage(scaled);
            }
        }
        iconSet.prepareImages(this);
        return iconSet;
    }

    protected void readAllIcons() {
        iconSet = readIcons("terrain", map.getTerrainSet());
        thingSet = readIcons("things", map.getThingSet());
        featureSet = readIcons("features", map.getFeatureSet());
        effectSet = readEffectIcons();
    }

    private ViewProperties getNewViewProperties(String path, String zoom) {
        try {
            return new ViewProperties(path, zoom);
        } catch (Exception e) {
            return (ViewProperties) null;
        }
    }

    /**
     * Constructor to load a map from a file and display it.
     */
    public MapViewer(Properties properties, String filename) {
        super(true);
        toolkit = Toolkit.getDefaultToolkit();
        this.properties = properties;
        String path = properties.getProperty("path.images", "images");
        try {
            map = new Map(filename);
            map.setCurrentSet("root");
            currentSet = map.getTileSet(0);
            path = path + "/" + map.getImageDir();
            views = new ViewProperties[7];
            views[0] = getNewViewProperties(path, "xsmall");
            views[1] = getNewViewProperties(path, "small");
            views[2] = getNewViewProperties(path, "medium");
            views[3] = getNewViewProperties(path, "large");
            views[4] = getNewViewProperties(path, "xlarge");
            setView(2);
            System.out.println("MapViewer: readAllIcons");
            readAllIcons();
        } catch (MapException e) {
            e.printStackTrace();
        }
        System.out.println("MapViewer: Finished");
    }

    public void setImagePath(String path) {
        this.imagePath = path;
    }

    public void loadMap(String filename) {
        try {
            map = new Map(filename);
            TerrainSet set = map.getTerrainSet();
            iconSet = readIcons("terrain", set);
            thingSet = readIcons("things", map.getThingSet());
            map.setCurrentSet("root");
            currentSet = map.getTileSet(0);
            int realWidth, realHeight;
            realWidth = tileXSize * (map.getTileSet(0).getMapWidth() + 1);
            realHeight = tileYSize * (map.getTileSet(0).getMapHeight() + 1);
            setPreferredSize(new Dimension(realWidth, realHeight));
            repaint();
        } catch (MapException e) {
            e.printStackTrace();
        }
    }

    public void zoomIn() {
        if (view < ViewProperties.XXLARGE) {
            setView(view + 1);
        }
    }

    public void zoomOut() {
        if (view > 0) {
            setView(view - 1);
        }
    }

    public void setView(int view) {
        if (views[view] == null) {
            return;
        }
        this.view = view;
        tileXSize = views[view].getTileWidth();
        tileYSize = views[view].getTileHeight();
        tileYOffset = views[view].getTileOffset();
        iconWidth = views[view].getIconWidth();
        iconHeight = views[view].getIconHeight();
        int realWidth, realHeight;
        realWidth = tileXSize * (currentSet.getMapWidth() + 1);
        realHeight = tileYSize * (currentSet.getMapHeight() + 1);
        setPreferredSize(new Dimension(realWidth, realHeight));
        readAllIcons();
        repaint();
    }

    /**
     * Is the map currently displaying the tile grid?
     */
    public boolean isShowGrid() {
        return showGrid;
    }

    /**
     * Set whether the map should show the tile grid.
     * Force a redraw of the map to update the display.
     */
    public void setShowGrid(boolean show) {
        showGrid = show;
        paintComponent();
    }

    /**
     * Is the map currently displaying the large helper grid?
     */
    public boolean isShowLargeGrid() {
        return showLargeGrid;
    }

    /**
     * Set whether the map should show the helper grid.
     * Force a redraw of the map to update the display.
     */
    public void setShowLargeGrid(boolean show) {
        showLargeGrid = show;
        paintComponent();
    }

    /**
     * Is the map currently displaying things?
     */
    public boolean isShowThings() {
        return showThings;
    }

    /**
     * Set whether the map should show things.
     * Force a redraw of the map to update the display.
     */
    public void setShowThings(boolean show) {
        showThings = show;
        paintComponent();
    }

    /**
     * Set whether the map should show area borders.
     */
    public void setShowAreas(boolean show) {
        showAreas = show;
        paintComponent();
    }

    /**
     * Is the map displaying area borders?
     */
    public boolean isShowAreas() {
        return showAreas;
    }

    /**
     * Is the map currently displaying hills?
     */
    public boolean isShowFeatures() {
        return showFeatures;
    }

    /**
     * Set whether the map should show hills.
     * Force a redraw of the map to update the display.
     */
    public void setShowFeatures(boolean show) {
        showFeatures = show;
        paintComponent();
    }

    public boolean isShowCoasts() {
        return showCoasts;
    }

    public void setShowCoasts(boolean show) {
        showCoasts = show;
        paintComponent();
    }

    public boolean isShowRivers() {
        return showRivers;
    }

    public void setShowRivers(boolean show) {
        showRivers = show;
        paintComponent();
    }

    public boolean isShowRoads() {
        return showRoads;
    }

    public void setShowRoads(boolean show) {
        showRoads = show;
        paintComponent();
    }

    public void drawPaths() {
        drawPaths((Graphics2D) this.getGraphics());
    }

    public void drawPaths(Graphics2D g) {
        int i = 0, e = 0;
        Path[] paths = currentSet.getPaths();
        for (i = 0; i < paths.length; i++) {
            Path path = paths[i];
            if (path.isRoad() && !showRoads) {
                continue;
            } else if (path.isRiver() && !showRivers) {
                continue;
            }
            Shape shape = path.getGraphicsShape(g, tileXSize, tileYSize, tileYOffset, iconWidth, iconHeight);
            g.setStroke(new BasicStroke(path.getWidth()));
            if (path.isHighlighted()) {
                switch(path.getType()) {
                    case Path.RIVER:
                        g.setColor(new Color(60, 60, 255));
                        break;
                    case Path.ROAD:
                        g.setColor(new Color(50, 50, 50));
                        break;
                }
            } else {
                switch(path.getType()) {
                    case Path.RIVER:
                        g.setColor(new Color(184, 253, 253));
                        break;
                    case Path.ROAD:
                        g.setColor(new Color(150, 150, 150));
                        break;
                }
            }
            g.draw(shape);
        }
    }

    public void drawLargeGrid(Graphics2D g) {
    }

    public void paintComponent() {
        Graphics2D g = (Graphics2D) this.getGraphics();
        if (isDisplayable()) {
            if (g == null || !(g instanceof Graphics2D)) {
                System.out.println("ERROR: paintComponent: Invalid graphics object");
                return;
            }
            paintComponent(g);
        }
    }

    /**
     * Paint the entire map component. No attempt at cropping is performed,
     * the entire map is redrawn each time even if not all of it can be
     * seen. At some point this needs to be fixed.
     */
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        int x = 0, y = 0;
        int xp, yp, ypp;
        if (map == null) {
            return;
        }
        if (g == null) {
            System.out.println("ERROR: paintComponent: Graphics object is null");
            return;
        }
        if (!(g instanceof Graphics2D)) {
            System.out.println("ERROR: paintComponent: Do not have Graphics2D");
        }
        int startX = 0;
        int startY = 0;
        int endX = currentSet.getMapWidth();
        int endY = currentSet.getMapHeight();
        Rectangle clip = g.getClipBounds();
        if (clip != null) {
            startX = (int) (clip.getX() / tileXSize) - 1;
            endX = (int) ((clip.getX() + clip.getWidth()) / tileXSize) + 1;
            startY = (int) (clip.getY() / tileYSize) - 1;
            endY = (int) ((clip.getY() + clip.getHeight()) / tileYSize) + 1;
        } else {
            startX = startY = 0;
            endX = currentSet.getMapWidth();
            endY = currentSet.getMapHeight();
        }
        if (startX < 0) startX = 0;
        if (startY < 0) startY = 0;
        if (endX > currentSet.getMapWidth()) {
            endX = currentSet.getMapWidth();
        }
        if (endY > currentSet.getMapHeight()) {
            endY = currentSet.getMapHeight();
        }
        try {
            for (y = startY; y < endY; y++) {
                for (x = startX; x < endX; x++) {
                    paintTile(x, y, g);
                }
            }
            if (showRivers || showRoads) {
                drawPaths((Graphics2D) g);
            }
            if (showThings) {
                Thing[] things = currentSet.getThings();
                for (int i = 0; i < things.length; i++) {
                    Thing thing = things[i];
                    if (thing.getX() < (100 * startX) || thing.getX() > (100 * endX)) {
                        continue;
                    }
                    if (thing.getY() < (100 * startY) || thing.getY() > (100 * endY)) {
                        continue;
                    }
                    paintThing(thing, g);
                }
            }
            if (showLargeGrid) {
                int x1, x2, y1, y2;
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(Color.BLACK);
                g2.setStroke(new BasicStroke(1));
                y1 = 0;
                y2 = endY * tileYSize;
                for (x = 0; x < endX; x += 6) {
                    x1 = x2 = (int) (x * tileXSize + (iconWidth * 0.5));
                    GeneralPath gp = new GeneralPath();
                    Line2D line = new Line2D.Float(x1, y1, x2, y2);
                    gp.append(line, true);
                    g2.draw(gp);
                }
                x1 = 0;
                x2 = endX * tileXSize;
                for (y = 0; y < endY; y += 5) {
                    y1 = y2 = (int) (y * tileYSize);
                    GeneralPath gp = new GeneralPath();
                    Line2D line = new Line2D.Float(x1, y1, x2, y2);
                    gp.append(line, true);
                    g2.draw(gp);
                }
            }
        } catch (Exception e) {
            System.out.println("Failed to paint tile " + x + ", " + y);
        }
    }

    /**
     * Paint a single tile on the map. Tile is referenced by its
     * coordinate. Tile is painted to the graphics object of the
     * MapViewer component.
     */
    public void paintTile(int x, int y) {
        Graphics2D g = (Graphics2D) this.getGraphics();
        if (g == null) {
            System.out.println("ERROR: paintTile: Cannot obtain graphics object");
            return;
        }
        paintTile(x, y, g);
    }

    /**
     * Return a point representing the top left corner of thhis tile.
     * For hexagonal maps, this is non-trivial to work out.
     */
    private Point getPosition(int x, int y) {
        int px = 0;
        int py = 0;
        if (map.getTileShape() == Map.SQUARE) {
            px = x * tileXSize;
            py = y * tileYSize;
        } else {
            px = x * tileXSize;
            py = y * tileYSize;
            if (x % 2 == 1) {
                py += tileYOffset;
            }
        }
        return new Point(px, py);
    }

    /**
     * Paint a single tile on the map. Tile is referenced by its
     * coordinate. Tile is painted to the supplied graphics object.
     */
    public void paintTile(int x, int y, Graphics g) {
        int xp, yp;
        Graphics2D g2 = (Graphics2D) g;
        if (g2 == null) {
            System.out.println("g2 is NULL");
            System.exit(0);
        }
        if (map.getTileShape() == Map.SQUARE) {
            xp = x * tileXSize;
            yp = y * tileYSize;
        } else {
            xp = x * tileXSize;
            yp = y * tileYSize + ((x % 2 == 0) ? 0 : tileYOffset);
        }
        try {
            int t = currentSet.getTerrain(x, y).getId();
            Image icon = iconSet.getIcon(t);
            double r = 0.0;
            if (icon == null) {
                System.out.println("ERROR: Icon is null for icon " + t);
                return;
            }
            r = Math.toRadians(currentSet.getTerrainRotation(x, y));
            if (r != 0) {
                g2.rotate(r, xp + iconWidth / 2, yp + tileYSize / 2);
                g2.drawImage(icon, xp, yp, this);
                g2.rotate(-r, xp + iconWidth / 2, yp + tileYSize / 2);
            } else {
                g2.drawImage(icon, xp, yp, this);
            }
            if (currentSet.getFeature(x, y) != null) {
                r = Math.toRadians(currentSet.getFeatureRotation(x, y));
                icon = featureSet.getIcon(currentSet.getFeature(x, y).getId());
                g2.rotate(r, xp + tileXSize / 2, yp + tileYSize / 2);
                g2.drawImage(icon, xp, yp, this);
                g2.rotate(-r, xp + tileXSize / 2, yp + tileYSize / 2);
            }
            if (showAreas && map.getTileShape() == Map.HEXAGONAL) {
                int area = 0;
                if (currentSet.getArea(x, y) != null) {
                    area = currentSet.getArea(x, y).getId();
                }
                int parent;
                int x1, y1, x2, y2;
                Area pa = map.getAreaParent(x, y);
                if (pa != null) {
                    parent = pa.getId();
                } else {
                    parent = -1;
                }
                g2.setColor(Color.RED);
                g2.setStroke(new BasicStroke((float) (0.5 * view)));
                if (y > 0) {
                    Area aa = currentSet.getArea(x, y - 1);
                    int n = 0;
                    if (aa != null) n = aa.getId();
                    if (area != n) {
                        if (parent == n || parent == map.getAreaParentId(n)) {
                            debug("Parent area match!");
                            g2.setColor(Color.ORANGE);
                        }
                        Point p = getPosition(x, y);
                        x1 = (int) p.getX() + (int) (iconWidth * 0.3);
                        x2 = (int) p.getX() + (int) (iconWidth * 0.7);
                        y1 = y2 = (int) p.getY();
                        GeneralPath gp = new GeneralPath();
                        Line2D line = new Line2D.Float(x1, y1, x2, y2);
                        gp.append(line, true);
                        g2.draw(gp);
                    }
                }
                g2.setColor(Color.RED);
                if (x > 0 && y > 0) {
                    int n = 0;
                    if (x % 2 == 0) {
                        Area aa = currentSet.getArea(x - 1, y - 1);
                        if (aa != null) n = aa.getId();
                    } else {
                        Area aa = currentSet.getArea(x - 1, y);
                        if (aa != null) n = aa.getId();
                    }
                    if (area != n) {
                        Point p = getPosition(x, y);
                        x1 = (int) p.getX();
                        x2 = (int) p.getX() + (int) (iconWidth * 0.3);
                        y1 = (int) p.getY() + (int) (tileYSize * 0.5);
                        y2 = (int) p.getY();
                        GeneralPath gp = new GeneralPath();
                        Line2D line = new Line2D.Float(x1, y1, x2, y2);
                        gp.append(line, true);
                        g2.draw(gp);
                    }
                }
                if (x > 0 && y < currentSet.getMapHeight()) {
                    int n = 0;
                    if (x % 2 == 0) {
                        Area aa = currentSet.getArea(x - 1, y);
                        if (aa != null) n = aa.getId();
                    } else if (y + 1 < currentSet.getMapHeight()) {
                        Area aa = currentSet.getArea(x - 1, y + 1);
                        if (aa != null) n = aa.getId();
                    } else {
                        n = area;
                    }
                    if (area != n) {
                        Point p = getPosition(x, y);
                        x1 = (int) p.getX();
                        x2 = (int) p.getX() + (int) (iconWidth * 0.3);
                        y1 = (int) p.getY() + (int) (tileYSize * 0.5);
                        y2 = (int) p.getY() + tileYSize;
                        GeneralPath gp = new GeneralPath();
                        Line2D line = new Line2D.Float(x1, y1, x2, y2);
                        gp.append(line, true);
                        g2.draw(gp);
                    }
                }
            }
            if (currentSet.isHighlighted(x, y)) {
                icon = effectSet.getIcon(HIGHLIGHT_ICON);
                if (icon != null) {
                    g2.drawImage(icon, xp, yp, this);
                } else {
                    warn("Highlight icon is null");
                }
            }
        } catch (Exception e) {
            System.out.println("Failed to paint tile " + x + ", " + y + " (" + e + ")");
            e.printStackTrace();
        }
    }

    public void paintThing(Thing thing, Graphics g) {
        int x = 0, y = 0;
        int fontSize = 16;
        int fontStyle = Font.PLAIN;
        Image icon = thingSet.getIcon(thing.getType());
        Font font = null;
        Graphics2D g2 = (Graphics2D) g;
        switch(thing.getImportance()) {
            case Thing.LOW:
                if (view < 2) return;
                break;
            case Thing.NORMAL:
            case Thing.HIGH:
                break;
        }
        x = thing.getX() * tileXSize / 100 - tileXSize / 2;
        y = thing.getY() * tileYSize / 100 - tileYSize / 2;
        double r = 0.0;
        r = Math.toRadians(thing.getRotation());
        g2.rotate(r, x + tileXSize / 2, y + tileYSize / 2);
        g2.drawImage(icon, x, y, this);
        g2.rotate(-r, x + tileXSize / 2, y + tileYSize / 2);
        if (map.getType() != Map.LOCAL) {
            switch(thing.getFontSize()) {
                case Thing.SMALL:
                    fontSize = views[view].getSmallFont();
                    break;
                case Thing.MEDIUM:
                    fontSize = views[view].getMediumFont();
                    break;
                case Thing.LARGE:
                    fontSize = views[view].getLargeFont();
                    fontStyle = Font.BOLD;
                    break;
                case Thing.HUGE:
                    fontSize = views[view].getHugeFont();
                    fontStyle = Font.BOLD;
                    break;
            }
            font = new Font("Helvetica", fontStyle, fontSize);
            g.setColor(Color.BLACK);
            g.setFont(font);
            g.drawString(thing.getName(), x, y);
        }
    }

    public boolean crop(int x, int y, int w, int h) {
        map.crop(x, y, w, h);
        paintComponent();
        return true;
    }

    public boolean cropToArea(String area, int margin) {
        Area a = map.getAreaByName(area);
        return cropToArea(a, margin);
    }

    public boolean cropToArea(Area area, int margin) {
        map.cropToArea(area, margin);
        setView(view);
        return true;
    }

    public boolean cropToThing(String thing, int radius) {
        map.cropToThing(thing, (short) radius);
        setView(view);
        return true;
    }

    public boolean rescale(int newScale) {
        map.rescale(newScale);
        setView(view);
        return true;
    }

    /**
     * Main method for use when testing.
     */
    public static void main(String args[]) {
    }
}
