package org.opencarto.vectortile;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.logging.Logger;
import org.mapfish.geo.MfFeature;
import org.mapfish.geo.MfGeo;
import org.mapfish.geo.MfGeo.GeoType;
import org.mapfish.geo.MfGeometry;
import org.opencarto.datamodel.OpenCartoFeature;
import org.opencarto.datamodel.Rep;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;

/**
 * An agent that retrieve a tile on the web.
 * 
 * @author julien Gaffuri
 *
 */
public class VecTileLoader implements Runnable {

    private static final Logger logger = Logger.getLogger(VecTileLoader.class.getName());

    private HashSet<String> lauchedHTTPRequests;

    private VecDataSource dataSource;

    private int x, y, z;

    public VecTileLoader(HashSet<String> lauchedHTTPRequests, VecDataSource wmsSource, int x, int y, int z) {
        this.lauchedHTTPRequests = lauchedHTTPRequests;
        this.dataSource = wmsSource;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void start() {
        new Thread(this).start();
    }

    @Override
    public void run() {
        String key = getKey();
        synchronized (this.lauchedHTTPRequests) {
            if (this.lauchedHTTPRequests.contains(key)) return;
            this.lauchedHTTPRequests.add(key);
        }
        String st = this.dataSource.getTileURL(this.x, this.y, this.z);
        URL url;
        try {
            url = new URL(st);
        } catch (MalformedURLException e1) {
            logger.warning("Error in URL: " + st);
            return;
        }
        String geoJSON = "";
        try {
            InputStream is;
            if ("file".equals(url.getProtocol())) is = new FileInputStream(url.getFile()); else if ("http".equals(url.getProtocol())) is = url.openStream(); else {
                logger.warning("Impossible to load settings from " + url + ". Unsupported protocol " + url.getProtocol());
                return;
            }
            BufferedReader in = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = in.readLine()) != null) geoJSON += line;
            in.close();
        } catch (FileNotFoundException e) {
            return;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        VectorTile tile = new VectorTile(geoJSON, x, y, z);
        Geometry[] geoms = new Geometry[tile.getPieces().size()];
        int i = 0;
        for (MfGeo geo : tile.getPieces()) {
            if (geo.getGeoType() == GeoType.GEOMETRY) {
                geoms[i++] = ((MfGeometry) geo).getInternalGeometry();
            } else if (geo.getGeoType() == GeoType.FEATURE) {
                MfFeature mf = (MfFeature) geo;
                geoms[i++] = mf.getMfGeometry().getInternalGeometry();
            }
        }
        GeometryCollection gc = new GeometryFactory().createGeometryCollection(geoms);
        this.dataSource.getDataLoader().add(new OpenCartoFeature(this.dataSource.getLayer(), new Rep(gc), this.z));
        this.dataSource.getLayer().getDisplayCacheLoader().coin();
        synchronized (this.lauchedHTTPRequests) {
            this.lauchedHTTPRequests.remove(key);
        }
    }

    private String getKey() {
        return new StringBuffer().append(this.z).append(".").append(this.x).append(".").append(this.y).toString();
    }
}
