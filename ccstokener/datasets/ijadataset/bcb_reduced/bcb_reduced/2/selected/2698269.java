package de.byteholder.geoclipse.map;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import de.byteholder.geoclipse.logging.StatusUtil;
import de.byteholder.geoclipse.map.event.TileEventId;

/**
 * This class loads the tile images. The run method is called from the executer in the thread queue.
 */
class TileImageLoader implements Runnable {

    private final TileFactoryImpl fTileFactoryImpl;

    /**
	 * @param defaultTileFactory
	 */
    TileImageLoader(final TileFactoryImpl defaultTileFactory) {
        fTileFactoryImpl = defaultTileFactory;
    }

    private void finalizeTile(final Tile tile, final boolean isNotifyObserver) {
        fTileFactoryImpl.getTileCache().add(tile.getTileKey(), tile);
        if (tile.isLoadingError() == false) {
            fTileFactoryImpl.getLoadingTiles().remove(tile.getTileKey());
        }
        tile.setLoading(false);
        if (isNotifyObserver) {
            tile.notifyImageObservers();
        }
        fTileFactoryImpl.fireTileEvent(TileEventId.TILE_END_LOADING, tile);
    }

    /**
	 * load tile from a url
	 */
    private void loadTileImage(final Tile tile) {
        String loadingError = null;
        Tile parentTile = null;
        boolean isNotifyObserver = true;
        boolean isParentFinal = false;
        try {
            fTileFactoryImpl.fireTileEvent(TileEventId.TILE_START_LOADING, tile);
            boolean isSaveImage = false;
            final TileImageCache tileImageCache = fTileFactoryImpl.getTileImageCache();
            final TileFactory tileFactory = tile.getTileFactory();
            final TileFactoryInfo factoryInfo = tileFactory.getInfo();
            ImageData[] tileImageData = tileImageCache.getOfflineTileImageData(tile);
            if (tileImageData == null) {
                isSaveImage = true;
                InputStream inputStream = null;
                try {
                    final ITileLoader tileLoader = factoryInfo.getTileLoader();
                    if (tileLoader instanceof ITileLoader) {
                        try {
                            inputStream = tileLoader.getTileImageStream(tile);
                        } catch (final Exception e) {
                            loadingError = e.getMessage();
                            StatusUtil.logStatus(loadingError, e);
                            throw e;
                        }
                    } else {
                        final URL url;
                        try {
                            url = fTileFactoryImpl.getURL(tile);
                        } catch (final Exception e) {
                            loadingError = e.getMessage();
                            throw e;
                        }
                        try {
                            inputStream = url.openStream();
                        } catch (final UnknownHostException e) {
                            loadingError = "Map image cannot be loaded from:\n" + tile.getUrl() + "\n\nUnknownHostException: " + e.getMessage();
                            StatusUtil.logStatus(loadingError, e);
                            throw e;
                        } catch (final FileNotFoundException e) {
                            loadingError = "Map image cannot be loaded from:\n" + tile.getUrl() + "\n\nFileNotFoundException: " + e.getMessage();
                            throw e;
                        } catch (final Exception e) {
                            loadingError = "Map image cannot be loaded from:\n" + tile.getUrl() + "\n" + e.getMessage();
                            StatusUtil.logStatus(loadingError, e);
                            throw e;
                        }
                    }
                    tileImageData = new ImageLoader().load(inputStream);
                } catch (final Exception e) {
                    try {
                        if (inputStream != null) {
                            inputStream.close();
                        }
                    } catch (final IOException e1) {
                        StatusUtil.logStatus(e.getMessage(), e);
                    }
                    fTileFactoryImpl.fireTileEvent(TileEventId.TILE_ERROR_LOADING, tile);
                }
            }
            boolean isCreateImage = true;
            Tile imageTile = tile;
            String imageTileKey = tile.getTileKey();
            if (tileImageData == null) {
                tile.setLoadingError(loadingError == null ? "Loaded image data is empty" : loadingError);
                isCreateImage = false;
            }
            if (tile.isChildTile()) {
                isNotifyObserver = false;
                if (tileImageData != null && isSaveImage) {
                    tileImageCache.saveOfflineImage(tile, tileImageData);
                }
                parentTile = tile.getParentTile();
                final ParentImageStatus parentImageStatus = tile.setChildImageData(tileImageData);
                if (parentImageStatus == null) {
                    parentTile.setLoadingError("Parent image cannot be created");
                } else {
                    if (parentImageStatus.isImageFinal) {
                        parentTile.setLoadingError(parentImageStatus.childLoadingError);
                        tileImageData = parentImageStatus.tileImageData;
                        imageTile = parentTile;
                        imageTileKey = parentTile.getTileKey();
                        isParentFinal = true;
                        isCreateImage = true;
                        isSaveImage = parentImageStatus.isSaveImage;
                    } else {
                        isCreateImage = false;
                    }
                }
            }
            if (isCreateImage) {
                final Image tileImage = tileImageCache.createImage(tileImageData, imageTile, imageTileKey, isSaveImage);
                if (imageTile.setMapImage(tileImage) == false) {
                    tile.setLoadingError("Image is invalid");
                }
            }
        } catch (final Exception e) {
            StatusUtil.logStatus("Exception occured when loading tile images", e);
        } finally {
            finalizeTile(tile, isNotifyObserver);
            if (isParentFinal) {
                finalizeTile(parentTile, true);
            }
        }
    }

    /**
	 * paint tile based on SRTM data
	 */
    private void paintTileImage(final Tile tile, final ITilePainter tilePainter) {
        fTileFactoryImpl.fireTileEvent(TileEventId.SRTM_PAINTING_START, tile);
        try {
            final Display display = Display.getDefault();
            final RGB[][] rgbData = tilePainter.drawTile(tile);
            display.asyncExec(new Runnable() {

                public void run() {
                    final ImageData[] paintedImageData = new ImageData[1];
                    final int tileSize = rgbData[0].length;
                    final Image paintedImage = new Image(display, tileSize, tileSize);
                    final HashMap<Integer, Color> usedMapColors = new HashMap<Integer, Color>();
                    final GC gc = new GC(paintedImage);
                    {
                        for (int drawX = 0; drawX < rgbData.length; drawX++) {
                            final RGB[] rgbX = rgbData[drawX];
                            for (int drawY = 0; drawY < rgbX.length; drawY++) {
                                final RGB rgb = rgbX[drawY];
                                Color mapColor = usedMapColors.get(rgb.hashCode());
                                if (mapColor == null) {
                                    mapColor = new Color(display, rgb);
                                    usedMapColors.put(rgb.hashCode(), mapColor);
                                }
                                gc.setForeground(mapColor);
                                gc.drawPoint(drawX, drawY);
                            }
                        }
                    }
                    gc.dispose();
                    paintedImageData[0] = paintedImage.getImageData();
                    paintedImage.dispose();
                    for (final Color color : usedMapColors.values()) {
                        color.dispose();
                    }
                    if (paintedImageData == null) {
                        tile.setLoadingError("Painting data is invalid");
                    } else {
                        final String tileKey = tile.getTileKey();
                        final Image tileImage = fTileFactoryImpl.getTileImageCache().createImage(paintedImageData, tile, tileKey, true);
                        if (tile.setMapImage(tileImage)) {
                            fTileFactoryImpl.getLoadingTiles().remove(tileKey);
                        } else {
                            tile.setLoadingError("Painted image is invalid");
                        }
                    }
                    tile.setLoading(false);
                    fTileFactoryImpl.fireTileEvent(TileEventId.SRTM_PAINTING_END, tile);
                }
            });
        } catch (final Exception e) {
            tile.setLoadingError("Painting error occured: " + e.getMessage());
            tile.setLoading(false);
            fTileFactoryImpl.fireTileEvent(TileEventId.SRTM_PAINTING_ERROR, tile);
            StatusUtil.logStatus(e.getMessage(), e);
        }
    }

    public void run() {
        final Tile tile = fTileFactoryImpl.getTileWaitingQueue().pollLast();
        if (tile == null) {
            return;
        }
        final boolean isChildTile = tile.isChildTile();
        final boolean isParentTile = fTileFactoryImpl instanceof ITileChildrenCreator && isChildTile == false;
        if (isParentTile) {
            if (tile.isLoadingError()) {
                finalizeTile(tile, true);
            } else if (tile.hasOfflimeImage()) {
                loadTileImage(tile);
            }
        } else {
            final ITilePainter tilePainter = tile.getTileFactory().getInfo().getTilePainter();
            if (tilePainter != null) {
                paintTileImage(tile, tilePainter);
            } else {
                loadTileImage(tile);
            }
        }
    }
}
