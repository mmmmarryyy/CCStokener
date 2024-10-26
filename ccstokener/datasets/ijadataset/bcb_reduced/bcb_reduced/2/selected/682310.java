package android.gopens;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public abstract class MapRetriever {

    public static final int WINDOWS_X = 320;

    public static final int WINDOWS_Y = 480;

    private static List<int[]> cache = new ArrayList<int[]>();

    /**
	 * @param args
	 */
    public static Map getMaps() throws MalformedURLException, IOException {
        return getMaps(45.647371, 5.860171, 10);
    }

    public static Map getMaps(double lat, double lon, int zoom) throws MalformedURLException, IOException {
        System.out.println("Get map : " + lat + "    " + lon);
        int latitudeTileNumber = lat2tile(lat, zoom);
        int longitudeTileNumber = lon2tile(lon, zoom);
        int[] tileCoord = { 0, 0 };
        int[] coord = { 0, 0 };
        Bitmap[][] maps = new Bitmap[3][3];
        for (int i = 0; i <= 2; i++) {
            for (int j = 0; j <= 2; j++) {
                try {
                    tileCoord[0] = longitudeTileNumber + j - 1;
                    tileCoord[1] = latitudeTileNumber + i - 1;
                    URL url = new URL(("http://tile.openstreetmap.org/" + zoom + "/" + (longitudeTileNumber + j - 1) + "/" + (latitudeTileNumber + i - 1) + ".png"));
                    Bitmap bmImg = BitmapFactory.decodeStream(url.openStream());
                    cache.add(tileCoord);
                    maps[j][i] = bmImg;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            coord = getPositionOnTile(lat, lon, zoom);
        }
        return new Map(maps, coord);
    }

    public static Map getMaps(double lat, double lon, int zoom, int xoffset, int yoffset) throws MalformedURLException, IOException {
        double zoomOffsetPerPixel = 360.0 * Math.pow(0.5, (double) zoom);
        double lon_update = lon + (xoffset * zoomOffsetPerPixel);
        double lat_update = lat + (yoffset * zoomOffsetPerPixel);
        return getMaps(lat_update, lon_update, zoom);
    }

    public static int[] getPositionOnTile(double lat, double lon, int zoom) {
        int[] coord = { 0, 0 };
        double north;
        double south;
        double east;
        double west;
        int latTile = lat2tile(lat, zoom);
        int lonTile = lon2tile(lon, zoom);
        north = tile2lat(latTile, zoom);
        south = tile2lat(latTile + 1, zoom);
        west = tile2lon(lonTile, zoom);
        east = tile2lon(lonTile + 1, zoom);
        coord[0] = (int) (256 + Math.round((lon - west) / (east - west) * 256)) - WINDOWS_X / 2;
        coord[1] = (int) (256 + Math.round((lat - north) / (south - north) * 256)) - WINDOWS_Y / 2;
        return coord;
    }

    private static int lon2tile(double lon, int zoom) {
        return (int) Math.floor((lon + 180) / 360 * (1 << zoom));
    }

    private static int lat2tile(final double lat, int zoom) {
        return (int) Math.floor((1 - Math.log(Math.tan(lat * Math.PI / 180) + 1 / Math.cos(lat * Math.PI / 180)) / Math.PI) / 2 * (1 << zoom));
    }

    private static double tile2lon(int x, int z) {
        return x / Math.pow(2.0, z) * 360.0 - 180;
    }

    private static double tile2lat(int y, int z) {
        double n = Math.PI - (2.0 * Math.PI * y) / Math.pow(2.0, z);
        return Math.toDegrees(Math.atan(Math.sinh(n)));
    }
}
