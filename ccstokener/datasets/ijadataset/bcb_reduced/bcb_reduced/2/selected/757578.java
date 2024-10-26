package etric.mapmaker.output;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;
import javax.imageio.ImageIO;
import etric.mapmaker.tools.BoundingBox;
import etric.mapmaker.tools.CoordsConvert;

/**
 * This class handles the download and merging of the tiles which are used for the map.
 * @author etric
 */
public class TileMap {

    /** url where the tiles can be found */
    public static final String LAYER_MAPNIK = "http://tile.openstreetmap.org/mapnik";

    public static final String LAYER_MAPLINT = "http://dev.openstreetmap.org/~ojw/Tiles/maplint.php";

    public static final String LAYER_TATHOME = "http://dev.openstreetmap.org/~ojw/Tiles/tile.php";

    /** output format that will be used */
    public static final int OUTPUT_JPG = 0;

    public static final int OUTPUT_PNG = 1;

    public static final int OUTPUT_BOTH = 2;

    /** base URl from the tile Server **/
    private String baseURL = null;

    /** name of the rendering layer **/
    private String tileLayer = null;

    private String mapTitel = null;

    private String mapLayerShort = null;

    /** normal width of one tile in px */
    private int tileWidth = 256;

    public TileMap(String mapTitel, String layer) {
        this.mapTitel = mapTitel;
        setTileLayer(layer);
    }

    /**
	 * @param tileLayer the tileLayer to set
	 */
    public void setTileLayer(String Layer) {
        this.baseURL = Layer;
        if (Layer == LAYER_MAPNIK) {
            this.tileLayer = "mapnik";
            this.mapLayerShort = "mn";
        } else if (Layer == LAYER_MAPLINT) {
            this.tileLayer = "maplint";
            this.mapLayerShort = "ml";
        } else if (Layer == LAYER_TATHOME) {
            this.tileLayer = "tilesathome";
            this.mapLayerShort = "th";
        }
        this.mapTitel = mapTitel + "_" + mapLayerShort;
        boolean success = (new File("map/" + this.mapTitel)).mkdirs();
        if (!success) {
            System.err.println("could not create the directory: " + "map/" + this.mapTitel);
        }
    }

    public void setNewTileServer(String serverUrl, String layerName) {
        this.baseURL = serverUrl;
        this.tileLayer = layerName;
        boolean success = (new File("tiles/" + this.tileLayer)).mkdirs();
        if (!success) {
        }
    }

    /**
	 * Downloads all tiles that are needed.
	 * @param gridwidth		// width of the tile grid
	 * @param zoom			// zoomlevel of the tile grid
	 * @param xCenter		// x coordination of the center tile
	 * @param yCenter		// y coordination of the center tile
	 */
    public String getTiles(int gridWidth, int zoom, int xCenter, int yCenter) {
        if (gridWidth > 1) {
            int startTile = gridWidth / 2;
            int xCoords = xCenter - startTile;
            int yCoords = yCenter - startTile;
            int tileNumber = 0;
            for (int xrow = 0; xrow < gridWidth; ++xrow) {
                for (int yrow = 0; yrow < gridWidth; ++yrow) {
                    downloadTile(baseURL + "/" + Integer.toString(zoom) + "/" + Integer.toString(xCoords) + "/" + Integer.toString(yCoords), tileNumber);
                    tileNumber++;
                    yCoords++;
                }
                xCoords++;
                yCoords = yCenter - startTile;
            }
        } else {
            downloadTile(baseURL + "/" + Integer.toString(zoom) + "/" + Integer.toString(xCenter) + "/" + Integer.toString(yCenter), 0);
        }
        return "map/" + mapTitel;
    }

    /**
	 * queries the server, gets the tile and save it on the hard disk
	 * @param tileUrl		// URL to the tile
	 * @param tileNumber	// number of tile that we get
	 */
    private void downloadTile(String tileUrl, int tileNumber) {
        URL url = null;
        InputStream in = null;
        System.out.println("Downloading tile: " + Integer.toString(tileNumber));
        try {
            url = new URL(tileUrl + ".png");
        } catch (MalformedURLException e1) {
            System.err.println("missing tile: " + tileUrl + ".png");
            System.err.println("creating blank tile instead.");
            createBlankTile("map/" + mapTitel, Integer.toString(tileNumber));
            return;
        }
        try {
            in = new BufferedInputStream(url.openStream());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            RandomAccessFile file;
            file = new RandomAccessFile("map/" + mapTitel + "/" + Integer.toString(tileNumber) + ".png", "rw");
            byte[] buffer = new byte[4096];
            for (int read = 0; (read = in.read(buffer)) != -1; out.write(buffer, 0, read)) ;
            file.write(out.toByteArray());
            file.close();
        } catch (IOException e) {
            System.err.println("cout not write tile: " + "map/" + mapTitel + "/" + Integer.toString(tileNumber) + ".png");
            System.err.println("creating blank tile instead.");
            createBlankTile("map/" + mapTitel, Integer.toString(tileNumber));
            return;
        }
    }

    /**
	 * creates a blank tile which is used in the case the download fails.
	 * @param path			// path where to store the tile
	 * @param tilenumber	// number of the missing tile
	 */
    private void createBlankTile(String path, String tilenumber) {
        BufferedImage blankTile = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
        Graphics2D map = blankTile.createGraphics();
        map.setColor(Color.WHITE);
        map.setBackground(Color.WHITE);
        map.drawRect(0, 0, 256, 256);
        try {
            ImageIO.write(blankTile, "png", new File(path + "/" + tilenumber + ".png"));
        } catch (Exception e) {
            System.err.println("could not create blank tile: " + e.getMessage());
        }
    }

    /**
	 * Merge the downloaded tiles together.
	 * The result will be one big map file with gridwidth*256 height and width as jpeg
	 * @param gridWidth		// width of the used tilegrid
	 */
    public void mergeTiles(int gridWidth, int format) {
        int mapSize = tileWidth * gridWidth;
        BufferedImage mergedImage = new BufferedImage(mapSize, mapSize, BufferedImage.TYPE_INT_RGB);
        Graphics2D map = mergedImage.createGraphics();
        Vector<BufferedImage> tiles = new Vector<BufferedImage>();
        for (int i = 0; i < (gridWidth * gridWidth); i++) {
            try {
                tiles.addElement(ImageIO.read(new File("map/" + mapTitel + "/" + i + ".png")));
            } catch (IOException e1) {
                System.err.println("missing file: " + "map/" + mapTitel + "/" + i + ".png");
            }
        }
        int tileNumber = 0;
        for (int xrow = 0; xrow < gridWidth; xrow++) {
            for (int yrow = 0; yrow < gridWidth; yrow++) {
                map.drawImage(tiles.get(tileNumber), tileWidth * xrow, tileWidth * yrow, 256, 256, Color.WHITE, null);
                tileNumber++;
            }
        }
        try {
            if (format == 0) {
                ImageIO.write(mergedImage, "jpeg", new File("map/" + mapTitel + "/" + mapTitel + ".jpg"));
            } else if (format == 1) {
                ImageIO.write(mergedImage, "png", new File("map/" + mapTitel + "/" + mapTitel + ".png"));
            } else if (format == 2) {
                ImageIO.write(mergedImage, "jpeg", new File("map/" + mapTitel + "/" + mapTitel + ".jpg"));
                ImageIO.write(mergedImage, "png", new File("map/" + mapTitel + "/" + mapTitel + ".png"));
            }
        } catch (Exception e) {
            System.err.println("Error merging: " + e.getMessage());
        }
        mergedImage.flush();
        removeTiles(tiles);
        tiles.clear();
    }

    private void removeTiles(Vector<BufferedImage> tiles) {
        File tmp = null;
        tiles.size();
        for (int i = 0; i < tiles.size(); i++) {
            tmp = new File("map/" + mapTitel + "/" + i + ".png");
            tmp.delete();
        }
    }

    public void writeMapInfos(BoundingBox bb) {
        File outputFile = new File("map/" + mapTitel + "/" + mapTitel + "_info.txt");
        PrintWriter bw = null;
        try {
            bw = new PrintWriter(new FileWriter(outputFile));
        } catch (IOException e1) {
            System.err.println("can't create output file.");
        }
        double lantitude = bb.getCenterLat();
        double longitude = bb.getCenterLong();
        bw.println("Center Long: " + CoordsConvert.convToDegMinSec(longitude) + "E");
        bw.println("Center Lat:  " + CoordsConvert.convToDegMinSec(lantitude) + "N");
        bw.println("Distance: " + bb.boxWidth());
        bw.close();
    }

    /**
	 * @return the mapTitel
	 */
    public String getMapTitel() {
        return mapTitel;
    }
}
