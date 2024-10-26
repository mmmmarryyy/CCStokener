package com.lightminds.swingx.jxmapviewer;

import com.lightminds.map.tileserver.admin.SessionHandler;
import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import org.jdesktop.swingx.mapviewer.GeoPosition;
import org.jdesktop.swingx.mapviewer.DefaultTileFactory;
import org.jdesktop.swingx.mapviewer.Tile;
import salomap.mapcomponent.GeoCoords;
import salomap.mapcomponent.ProjCoords;
import salomap.mapcomponent.exceptions.MethodNotSupportedException;
import salomap.projections.ProjectionLambert;

/**
 *
 * @author joshy
 */
public class SwegisTileFactory extends DefaultTileFactory {

    ProjectionLambert pl = new ProjectionLambert();

    protected Map<String, Tile> tileMap = new HashMap<String, Tile>();

    private SwegisTileProviderInfo info;

    private String servingURL = "";

    private long lastTokenRenewal = 0;

    public String getServingURL() {
        return servingURL;
    }

    public void setServingURL(String servingURL) {
        this.servingURL = servingURL;
    }

    /** Creates a new instance of DefaultTileFactory */
    public SwegisTileFactory(SwegisTileProviderInfo info) {
        super(info);
        this.info = info;
    }

    /**
     * <em>IN TILES!!!</em>
     */
    @Override
    public Dimension getMapSize(int zoom) {
        return new Dimension(getInfo().getMapWidthInTilesAtZoom(zoom), ((SwegisTileProviderInfo) getInfo()).getMapHeightInTilesAtZoom(zoom));
    }

    @Override
    public GeoPosition pixelToGeo(Point2D pixelCoordinate, int zoom) {
        Point2D p = ((SwegisTileProviderInfo) getInfo()).pixelCoordToMapCoord(zoom, pixelCoordinate.getX(), pixelCoordinate.getY());
        ProjCoords pc = new ProjCoords(p.getX(), p.getY());
        GeoCoords gc = null;
        try {
            gc = pl.localToGeo(pc);
        } catch (MethodNotSupportedException ex) {
            ex.printStackTrace();
        }
        return new GeoPosition(GeoCoords.radToDec(gc.m_dLatitude), GeoCoords.radToDec(gc.m_dLongitude));
    }

    @Override
    public Point2D geoToPixel(GeoPosition geoPos, int zoom) {
        GeoCoords gc = new GeoCoords(GeoCoords.decToRad(geoPos.getLongitude()), GeoCoords.decToRad(geoPos.getLatitude()));
        ProjCoords pc = pl.geoToLocal(gc);
        return ((SwegisTileProviderInfo) getInfo()).getBitMapCoordinate(zoom, pc.getX(), pc.getY());
    }

    @Override
    public Tile getTile(int x, int y, int zoom) {
        Tile tile = super.getTile(x, y, zoom);
        if (tile.isLoaded() && tile == null || tile.getUnrecoverableError() != null) {
            renewToken();
            tile = super.getTile(x, y, zoom);
        }
        return tile;
    }

    private synchronized void renewToken() {
        if (!(System.currentTimeMillis() > (lastTokenRenewal + 10000))) return;
        lastTokenRenewal = System.currentTimeMillis();
        String token = null;
        System.out.println("loading error - refresh token");
        byte[] buff = null;
        try {
            BufferedInputStream bis = null;
            System.out.println("Calling timeout : " + getServingURL() + "?token_timeout=true");
            URL remoteurl = new URL(getServingURL() + "?token_timeout=true");
            URLConnection connection = remoteurl.openConnection();
            connection.setRequestProperty("Referer", getServingURL());
            int length = connection.getContentLength();
            InputStream in = connection.getInputStream();
            buff = new byte[length];
            int bytesRead = 0;
            while (bytesRead < length) {
                bytesRead += in.read(buff, bytesRead, in.available());
            }
            token = new String(buff);
        } catch (Exception e) {
        }
        if (token != null && !token.equals("")) {
            token = token.trim();
            this.info.setToken(token);
        } else {
            System.out.println("Token returned was null");
        }
    }
}
