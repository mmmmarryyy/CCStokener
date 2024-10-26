package com.android.gopens;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public abstract class MapRetriever {

    /**
	 * @param args
	 */
    public static Bitmap[] getMaps() throws MalformedURLException, IOException {
        return getMaps(45.647371, 5.860171, 10);
    }

    public static Bitmap[] getMaps(double lat, double lon, int zoom) throws MalformedURLException, IOException {
        int latitudeTileNumber = lat2tile(lat, zoom);
        int longitudeTileNumber = lon2tile(lon, zoom);
        Bitmap[] maps = new Bitmap[10];
        int cpt = 0;
        for (int i = -1; i < 2; i++) {
            for (int j = -1; j < 2; j++) {
                try {
                    URL url = new URL(("http://tile.openstreetmap.org/" + zoom + "/" + (longitudeTileNumber + j) + "/" + (latitudeTileNumber + i) + ".png"));
                    Bitmap bmImg = BitmapFactory.decodeStream(url.openStream());
                    maps[cpt] = bmImg;
                    cpt++;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return maps;
    }

    public static int[] getPositionOnTile(double lat, double lon, int zoom, int latTile, int lonTile) {
        int[] coord = { 0, 0 };
        double north;
        double south;
        double east;
        double west;
        north = tile2lat(latTile, zoom);
        south = tile2lat(latTile + 1, zoom);
        west = tile2lon(lonTile, zoom);
        east = tile2lon(lonTile + 1, zoom);
        coord[0] = (int) Math.round((lon - west) / (east - west) * 256);
        coord[1] = (int) Math.round((lat - north) / (south - north) * 256);
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
