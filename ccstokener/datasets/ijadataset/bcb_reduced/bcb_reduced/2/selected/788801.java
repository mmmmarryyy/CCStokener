package open.gps.gopens.retriever;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import open.gps.gopens.R;
import open.gps.gopens.retrievers.utils.TileNumber;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;

/**
 * @author Lucas Miller
 * @author Aurelien Lanoiselier
 * @author Cyrille Mortier
 * @author Camille Tardy
 */
public class TilesRetrieverImpl extends Observable implements TilesRetriever {

    private Context ctx;

    private TileCacheManager cacheManager;

    private ExecutorService threadPool = Executors.newFixedThreadPool(20);

    private Bitmap loading;

    /**
	 * Downloads and stores the requested tiles
	 * 
	 * @param ctx
	 *            Application context
	 */
    public TilesRetrieverImpl(Context ctx) {
        this.ctx = ctx;
        this.cacheManager = new TileCacheManager(ctx, ctx.getResources().getInteger(R.integer.MEMORY_CACHE_SIZE));
        loading = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.loading);
    }

    /**
	 * Get the map tiles corresponding a gps coordinates and a zoom
	 * 
	 * @param tn
	 *            Coordinates of the selected tile.
	 * @return Map A Map object containing the tiles and the offset to have the
	 *         point at the center of the screen
	 * @throws IOException
	 *             if something get wrong when storing the tiles.
	 */
    public Bitmap getTile(TileNumber tn) throws IOException {
        if (tn == null) {
            throw new IOException("Null tileNumber ");
        } else if (tn.getX() > 0 && tn.getY() > 0 && tn.getZoom() > 0) {
            Bitmap cachedTile = this.cacheManager.get(tn);
            if (cachedTile != null) {
                return cachedTile;
            } else {
                if (!cacheManager.checkIfDownloading(tn)) {
                    threadPool.submit(new DownloadingThread(this));
                    cacheManager.addToDownloadStack(tn);
                }
                return loading;
            }
        }
        return loading;
    }

    /**
	 * Retrieve the bitmap map from the cache if it exists.
	 * 
	 * @param tn
	 *            The TileNumber.
	 * @return Bitmap if the file exist, null otherwise.
	 */
    public synchronized Bitmap getMapFromSdCache(TileNumber tn) {
        int x = tn.getX();
        int y = tn.getY();
        int zoom = tn.getZoom();
        File sdCardPath = Environment.getExternalStorageDirectory();
        String pathName = sdCardPath.getAbsolutePath() + ctx.getResources().getString(open.gps.gopens.R.string.CACHE_PATH) + zoom + "/" + x + "/" + y + ".png";
        Log.i("testDebug", "pathName ::" + pathName);
        File img = new File(pathName);
        if (img.exists()) {
            return BitmapFactory.decodeFile(pathName);
        } else {
            return null;
        }
    }

    /**
	 * Downloads a tile if this tile is not present in the SD card cache.
	 * 
	 * @param tn
	 *            The tile number of the desired tile
	 */
    public synchronized void downloadTile(TileNumber tn) {
        try {
            Bitmap tile = getMapFromSdCache(tn);
            if (tile == null) {
                URL url = new URL("http://tile.openstreetmap.org/" + tn.getZoom() + "/" + tn.getX() + "/" + tn.getY() + ".png");
                tile = BitmapFactory.decodeStream(url.openStream());
                File sdCardPath = Environment.getExternalStorageDirectory();
                Log.d(ctx.getResources().getString(open.gps.gopens.R.string.TEST), "Path to SD :: " + sdCardPath.getAbsolutePath());
                File dir = new File(sdCardPath + ctx.getResources().getString(open.gps.gopens.R.string.CACHE_PATH) + tn.getZoom() + "/" + tn.getX() + "/");
                dir.mkdirs();
                File imgFile = new File(dir.getAbsolutePath() + "/" + tn.getY() + ".png");
                imgFile.createNewFile();
                FileOutputStream fOut = new FileOutputStream(imgFile);
                tile.compress(Bitmap.CompressFormat.PNG, 100, fOut);
            }
            cacheManager.put(tn.toString(), tile);
            setChanged();
            notifyObservers();
            Log.d("OBS", "OBS : Notify");
        } catch (MalformedURLException e) {
            Log.e("Error", e.getMessage());
        } catch (IOException e) {
            Log.e("Error", e.getMessage());
        }
    }

    /**
	 * Returns a tile number from the download stack
	 * 
	 * @return The number of the tile to download
	 */
    public synchronized TileNumber getTileToDownload() {
        return cacheManager.getTileToDownload();
    }

    /**
	 * Get a tile stored in the cache
	 * 
	 * @param tn
	 *            The tile number of the desired tile
	 * @return The bitmap of the tile if its present, null otherwise
	 */
    public Bitmap getTileFromMemoryCache(TileNumber tn) {
        if (tn == null) {
            return null;
        }
        return cacheManager.get(tn);
    }

    /**
	 * Clears the cache
	 */
    public void clearCache() {
        this.cacheManager.clear();
    }

    /**
	 * The retriever is observed by the model
	 */
    @Override
    public synchronized void addObserver(Observer observer) {
        super.addObserver(observer);
    }
}
