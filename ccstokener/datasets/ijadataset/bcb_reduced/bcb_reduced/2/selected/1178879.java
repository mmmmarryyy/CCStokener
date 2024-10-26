package info.reflectionsofmind.connexion.tilelist;

import info.reflectionsofmind.connexion.fortress.core.common.tile.parser.TileCodeFormatException;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.imageio.ImageIO;

public class DefaultTileSource implements ITileSource {

    private final List<TileData> tiles = new ArrayList<TileData>();

    public DefaultTileSource(final URL url) throws IOException, TileCodeFormatException {
        final Properties tileList = new Properties();
        tileList.load(new InputStreamReader(url.openStream()));
        loadTiles(tileList);
    }

    private void loadTiles(Properties tileList) throws TileCodeFormatException, IOException {
        final Map<String, String> images = new HashMap<String, String>();
        final Map<String, String> codes = new HashMap<String, String>();
        final Map<String, String> points = new HashMap<String, String>();
        for (Object keyObject : tileList.keySet()) {
            final String key = (String) keyObject;
            final String value = tileList.getProperty(key);
            if (!key.contains(".")) continue;
            final String tileName = key.substring(0, key.lastIndexOf("."));
            final String keyType = key.substring(key.lastIndexOf(".") + 1);
            if ("code".equals(keyType)) codes.put(tileName, value);
            if ("image".equals(keyType)) images.put(tileName, value);
            if ("points".equals(keyType)) points.put(tileName, value);
        }
        for (String tileName : codes.keySet()) {
            if (images.get(tileName) == null) {
                continue;
            }
            if (points.get(tileName) == null) {
                continue;
            }
            final String code = codes.get(tileName);
            final BufferedImage image = ImageIO.read(Thread.currentThread().getContextClassLoader().getResourceAsStream(images.get(tileName)));
            final TileData tileData = new TileData(code, image);
            for (final String point : points.get(tileName).split(",")) {
                tileData.addSectionPoint(strToPoint2D(point));
            }
            this.tiles.add(tileData);
        }
    }

    @Override
    public List<TileData> getTiles() {
        return Collections.unmodifiableList(this.tiles);
    }

    private Point2D strToPoint2D(final String string) {
        final String[] cs = string.split(":");
        final double x = Double.parseDouble(cs[0]);
        final double y = Double.parseDouble(cs[1]);
        return new Point2D.Double(x, y);
    }
}
