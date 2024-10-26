package com.luzan.app.map.tool;

import org.apache.log4j.Logger;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.io.IOUtils;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.List;
import java.util.ArrayList;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import com.luzan.app.map.bean.MapTile;
import com.luzan.app.map.bean.NASAMapTile;
import com.luzan.app.map.utils.Configuration;
import com.luzan.common.geomap.LatLngRectangle;
import com.luzan.common.geomap.LatLngPoint;
import com.luzan.common.geomap.Tile;
import com.sun.xfile.XFile;
import com.sun.xfile.XFileOutputStream;

/**
 * NASACrawler
 *
 * @author Alexander Bondar
 */
public class NASACrawler {

    private static final Logger logger = Logger.getLogger(NASACrawler.class);

    protected String cfg;

    private Double south;

    private Double west;

    private Double north;

    private Double east;

    private Long sleep = 3000L;

    public void setSleep(String sleep) {
        this.sleep = Long.parseLong(sleep);
    }

    public void setCfg(String cfg) {
        this.cfg = cfg;
    }

    public void setSouth(String south) {
        this.south = Double.parseDouble(south);
    }

    public void setWest(String west) {
        this.west = Double.parseDouble(west);
    }

    public void setNorth(String north) {
        this.north = Double.parseDouble(north);
    }

    public void setEast(String east) {
        this.east = Double.parseDouble(east);
    }

    private void doIt() throws Throwable {
        int numCachedTiles = 0;
        try {
            List<MapTile> backTiles = new ArrayList<MapTile>();
            final LatLngRectangle bounds = new LatLngRectangle(new LatLngPoint(south, west), new LatLngPoint(north, east));
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
            logger.info(backTiles.size() + " tiles to cache");
            for (MapTile tile : backTiles) {
                InputStream in = null;
                OutputStream out = null;
                final URL url = new URL(tile.getPath());
                try {
                    int i = 4;
                    while (--i > 0) {
                        final XFile outFile = new XFile(dstDir, tile.toKeyString());
                        final URLConnection conn = url.openConnection();
                        if (conn == null || !conn.getContentType().startsWith("image")) {
                            logger.error("onearth.jpl.nasa.gov service returns non-image file, " + "content-type='" + conn.getContentType() + "'");
                            Thread.sleep(1000L * (long) Math.pow(2, 8 - i));
                            continue;
                        }
                        in = conn.getInputStream();
                        if (in != null) {
                            out = new XFileOutputStream(outFile);
                            IOUtils.copy(in, out);
                            break;
                        } else throw new IllegalStateException("opened stream is null");
                    }
                } finally {
                    if (out != null) {
                        out.flush();
                        out.close();
                    }
                    if (in != null) in.close();
                }
                if (++numCachedTiles % 10 == 0) {
                    logger.info(numCachedTiles + " tiles cached");
                    Thread.sleep(sleep);
                }
            }
        } catch (Throwable e) {
            logger.error("map tile caching has failed: ", e);
            throw e;
        }
    }

    public static void main(String args[]) {
        NASACrawler proc = new NASACrawler();
        String allArgs = StringUtils.join(args, ' ');
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(NASACrawler.class, Object.class);
            for (PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
                Pattern p = Pattern.compile("-" + pd.getName() + "\\s*([\\S]*)", Pattern.CASE_INSENSITIVE);
                final Matcher m = p.matcher(allArgs);
                if (m.find()) {
                    pd.getWriteMethod().invoke(proc, m.group(1));
                }
            }
            Configuration.getInstance().load(proc.cfg);
            proc.doIt();
        } catch (Throwable e) {
            logger.error("error", e);
            System.out.println(e.getMessage());
            try {
                BeanInfo beanInfo = Introspector.getBeanInfo(NASACrawler.class);
                System.out.println("Options:");
                for (PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
                    System.out.println("-" + pd.getName());
                }
            } catch (Throwable t) {
                System.out.print("Internal error");
            }
        }
    }
}
