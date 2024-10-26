package com.luzan.app.map.processor;

import com.luzan.app.map.service.bean.PublishRequest;
import com.luzan.app.map.bean.MapTile;
import com.luzan.app.map.bean.NASAMapTile;
import com.luzan.app.map.bean.publik.PublicMapPost;
import com.luzan.app.map.utils.Configuration;
import com.luzan.common.processor.ProcessorTask;
import com.luzan.common.processor.ProcessorContext;
import com.luzan.common.processor.ProcessorException;
import com.luzan.common.geomap.Tile;
import com.luzan.common.geomap.LatLngRectangle;
import com.luzan.common.geomap.LatLngPoint;
import com.luzan.db.dao.GenericDAO;
import com.luzan.db.dao.DAOFactory;
import com.luzan.db.ReadOnlyTransaction;
import com.luzan.db.DatabaseException;
import com.sun.xfile.XFile;
import com.sun.xfile.XFileOutputStream;
import java.util.List;
import java.util.ArrayList;
import java.net.URL;
import java.net.URLConnection;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.hibernate.criterion.Expression;

public class MapTileChacheTask implements ProcessorTask<PublishRequest> {

    private static final Logger logger = Logger.getLogger(MapTileChacheTask.class);

    public boolean preProcess(ProcessorContext<PublishRequest> context) throws InterruptedException, ProcessorException {
        logger.info("MapTileChacheTask:preProcess");
        return true;
    }

    public byte process(ProcessorContext<PublishRequest> context) throws InterruptedException, ProcessorException {
        logger.info("MapTileChacheTask:process");
        PublishRequest req = context.getItem().getEntity();
        if (StringUtils.isEmpty(req.getBackMap())) return TaskState.STATE_TILE_CACHED;
        final PublicMapPost post;
        final GenericDAO<PublicMapPost> postDao = DAOFactory.createDAO(PublicMapPost.class);
        try {
            ReadOnlyTransaction.beginTransaction();
        } catch (DatabaseException e) {
            logger.error("error", e);
            throw new ProcessorException(e);
        }
        int numCachedTiles = 0;
        try {
            List<MapTile> backTiles = new ArrayList<MapTile>();
            post = postDao.findUniqueByCriteria(Expression.eq("guid", req.getPostGuid()));
            final LatLngRectangle bounds = new LatLngRectangle(new LatLngPoint(post.getSWLat(), post.getSWLon()), new LatLngPoint(post.getNELat(), post.getNELon()));
            final String backMapGuid = "gst";
            final XFile dstDir = new XFile(new XFile(Configuration.getInstance().getPublicMapStorage().toString()), backMapGuid);
            dstDir.mkdir();
            for (int z = Math.min(Tile.getOptimalZoom(bounds, 768), 9); z <= 17; z++) {
                final Tile tileStart = new Tile(bounds.getSouthWest().getLat(), bounds.getSouthWest().getLng(), z);
                final Tile tileEnd = new Tile(bounds.getNorthEast().getLat(), bounds.getNorthEast().getLng(), z);
                for (double y = tileEnd.getTileCoord().getY(); y <= tileStart.getTileCoord().getY(); y++) for (double x = tileStart.getTileCoord().getX(); x <= tileEnd.getTileCoord().getX(); x++) {
                    NASAMapTile tile = new NASAMapTile((int) x, (int) y, z);
                    XFile file = new XFile(dstDir, tile.toKeyString());
                    if (file.exists() && file.isFile()) continue;
                    backTiles.add(tile);
                }
            }
            try {
                for (MapTile tile : backTiles) {
                    InputStream in = null;
                    OutputStream out = null;
                    final URL url = new URL(tile.getPath());
                    try {
                        final XFile outFile = new XFile(dstDir, tile.toKeyString());
                        final URLConnection conn = url.openConnection();
                        if (conn == null || !conn.getContentType().startsWith("image")) throw new IllegalAccessException("onearth.jpl.nasa.gov service returns non-image file, " + "content-type='" + conn.getContentType() + "'");
                        in = conn.getInputStream();
                        if (in != null) {
                            out = new XFileOutputStream(outFile);
                            IOUtils.copy(in, out);
                        } else throw new IllegalStateException("opened stream is null");
                    } finally {
                        if (out != null) {
                            out.flush();
                            out.close();
                        }
                        if (in != null) in.close();
                    }
                    if (++numCachedTiles % 100 == 0) {
                        logger.info(numCachedTiles + " tiles cached");
                    }
                }
            } catch (Throwable e) {
                logger.error("map tile caching has failed: ", e);
                throw new ProcessorException(e);
            }
        } catch (ProcessorException e) {
            logger.error("map tile caching has failed: ", e);
            throw e;
        } catch (Throwable e) {
            logger.error("map tile caching has failed: ", e);
            throw new ProcessorException(e);
        } finally {
            ReadOnlyTransaction.closeTransaction();
            logger.info(numCachedTiles + " tiles cached");
        }
        return TaskState.STATE_TILE_CACHED;
    }

    public void postProcess(ProcessorContext<PublishRequest> processorContext) throws InterruptedException, ProcessorException {
        logger.info("MapTileChacheTask:postProcess");
    }
}
