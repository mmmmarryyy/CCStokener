package com.volantis.mcs.googlemaps;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import com.volantis.synergetics.log.LogDispatcher;
import com.volantis.mcs.localization.LocalizationFactory;

public class OperationHelper {

    /**
     * The logger used by this class.
     */
    private static final LogDispatcher logger = LocalizationFactory.createLogger(OperationHelper.class);

    /**
     * Geocoder host.
     */
    private static String HOST = "http://maps.google.com/maps/geo?";

    /**
     * Key for geocoder API. 
     */
    private static String KEY = "ABQIAAAApcwDXZe7X6BilUwJbUBpaxRBOd3Ksdikbh80-kvHC15zaVzJFBRl08lXxta6N-wmA4oBk5U21engqA";

    private static OperationHelper instance = null;

    /**
     * For coordinates calculation. 
     */
    private GoogleCalculator calc;

    private OperationHelper() {
        this.calc = GoogleCalculatorExtended.getInstance();
    }

    public static OperationHelper getInstance() {
        if (null == instance) {
            instance = new OperationHelper();
        }
        return instance;
    }

    /**
     * Perform search in map mode. 
     * 
     * @param q
     * @return
     */
    public String performSearchMap(String q, Integer zoom) throws GeocoderException {
        GImage gImage;
        GPoint gPoint;
        int zoomInt = (zoom == null) ? GoogleCalculator.INITIAL_ZOOM : zoom.intValue();
        GLatLng gLatLng = doSearch(q);
        if (gLatLng != null) {
            gPoint = this.calc.fromLatLngToPixel(gLatLng, zoomInt);
        } else {
            zoomInt = GoogleCalculator.WORLD_ZOOM;
            gPoint = this.calc.fromLatLngToPixel(GoogleCalculator.WORLD_LAT_LNG, zoomInt);
        }
        gImage = this.calc.fromGPixelToGImage(gPoint);
        return getMainMapImagesList(gImage.getImgX(), gImage.getImgY(), zoomInt);
    }

    /**
     *Perform search in satellite mode. If no location found default world map with lang,lat (0,0)
     * is returned. 
     * 
     * @param q
     * @param mode
     * @return
     */
    public String performSearchPhoto(String q, Integer zoom) throws GeocoderException {
        GGeoString geoString;
        GPoint gPoint;
        int zoomInt = (zoom == null) ? GoogleCalculator.INITIAL_ZOOM : zoom.intValue();
        GLatLng gLatLng = doSearch(q);
        if (gLatLng != null) {
            gPoint = this.calc.fromLatLngToPixel(gLatLng, zoomInt);
        } else {
            zoomInt = GoogleCalculator.WORLD_ZOOM;
            gPoint = this.calc.fromLatLngToPixel(GoogleCalculator.WORLD_LAT_LNG, zoomInt);
        }
        GImage gImage = this.calc.fromGPixelToGImage(gPoint);
        gImage.setZoom(zoomInt);
        geoString = this.calc.fromGImageToGeoString(gImage);
        return getMainSatImagesList(geoString);
    }

    /**
     * Perform search operation, contact geocoder server, get response, parse it
     * and return lattitude and longitude for queried location. 
     * 
     * @param q
     * @return
     * @throws GeocoderException
     */
    private GLatLng doSearch(String q) throws GeocoderException {
        GLatLng gLatLng = null;
        String result = doRequest(q, HOST, KEY);
        int colorIndex = result.indexOf("coordinates");
        if (colorIndex != -1) {
            colorIndex += "coordinates".length();
            int start = result.indexOf('[', colorIndex);
            int stop = result.indexOf(']', colorIndex);
            String tab = result.substring(start + 1, stop);
            int separator = tab.indexOf(",");
            double lng = new Double(tab.substring(0, separator - 1)).doubleValue();
            double lat = new Double(tab.substring(separator + 1, tab.indexOf(",", separator + 1))).doubleValue();
            gLatLng = new GLatLng(lat, lng);
        } else {
            logger.warn("map-location-not-found", q);
        }
        return gLatLng;
    }

    /**
     * Request geocoder host to get location for queried place. 
     * 
     * @param q
     * @param host
     * @param key
     * @return
     * @throws GeocoderException
     */
    private String doRequest(String q, String host, String key) throws GeocoderException {
        URL url;
        URLConnection conn;
        String query = "";
        StringBuffer buffer = new StringBuffer();
        try {
            query = "q=" + URLEncoder.encode(q, "UTF-8") + "&output=js&key=" + URLEncoder.encode(key, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            logger.error("widget-unsupported-encoding", query, e);
            throw new GeocoderException(e);
        }
        try {
            url = new URL(host + query);
            conn = url.openConnection();
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = rd.readLine()) != null) {
                buffer.append(line);
            }
            rd.close();
        } catch (MalformedURLException e) {
            logger.error("widget-geocoder-malformed-url", host + query, e);
            throw new GeocoderException(e);
        } catch (IOException e) {
            logger.error("widget-geocoder-io-exception", host + query, e);
            throw new GeocoderException(e);
        }
        return buffer.toString();
    }

    /**
     * Return satellite images for zoom in operation
     * @param t
     * @param direction
     * @return
     */
    public String getSatZoomIn(String t, int offx, int offy) {
        GGeoString gString = new GGeoString(t, offx, offy);
        GGeoString zoomInString = this.calc.getZoomInGeoString(gString);
        return getMainSatImagesList(zoomInString);
    }

    /**
     * Return satellite images for zoom out operation
     * @param t
     * @param direction
     * @return
     */
    public String getSatZoomOut(String t, int offx, int offy) {
        GGeoString gString = new GGeoString(t, offx, offy);
        GGeoString zoomOutString = this.calc.getZoomOutGeoString(gString);
        return getMainSatImagesList(zoomOutString);
    }

    /**
     * Return map images after zoom in operation
     * @param x
     * @param y
     * @param currentZoom
     * @return
     */
    public String getMapZoomIn(int x, int y, int currentZoom) {
        int newX = x << 1;
        int newY = y << 1;
        int newZoom = currentZoom - 1;
        return getMainMapImagesList(newX, newY, newZoom);
    }

    /**
     * Return map images after zoom out operation
     * @param x
     * @param y
     * @param currentZoom
     * @return
     */
    public String getMapZoomOut(int x, int y, int currentZoom) {
        int newX = x >> 1;
        int newY = y >> 1;
        int newZoom = currentZoom + 1;
        return getMainMapImagesList(newX, newY, newZoom);
    }

    /**
     * Return images for given direction
     * @param x
     * @param y
     * @param zoom
     * @param direction
     * @return
     */
    public String getMapImagesList(int x, int y, int zoom, String direction) {
        GImage centerImage = new GImage(x, y, zoom);
        GImage[] newImages = null;
        ;
        newImages = this.calc.getMapImages(centerImage, (int[][]) this.calc.shiftMap.get(direction));
        return "{ zoom : " + zoom + ", imgList : " + this.calc.mapImageListToString(newImages) + "}";
    }

    /**
     * Wrapper for getPhotoImagesList(String dir,GGeoString geoString);
     * @param direction
     * @param t
     * @param offx
     * @param offy
     * @return
     */
    public String getPhotoImagesList(String direction, String t, int offx, int offy) {
        GGeoString geoString = new GGeoString(t, offx, offy);
        return this.getPhotomagesList(direction, geoString);
    }

    /**
     * Return satellites for given direction
     * @param t
     * @param direction
     * @return
     */
    public String getPhotomagesList(String direction, GGeoString geoString) {
        GImage centerImage = this.calc.fromGeoStringToGImage(geoString);
        GImage[] imageList = this.calc.getMapImages(centerImage, (int[][]) this.calc.shiftMap.get(direction));
        GGeoString[] photoList = new GGeoString[imageList.length];
        for (int i = 0; i < imageList.length; i++) {
            photoList[i] = this.calc.fromGImageToGeoString(imageList[i]);
        }
        return "{ zoom : " + geoString.getZoom() + ", imgList : " + this.calc.gGeoStringListToString(photoList) + "}";
    }

    /**
     * Get map images list for given satellite coordinate.
     *  
     * @param t satellite coordinate
     * @param area
     * @return
     */
    public String getMapImagesListFromTxt(String t, int offx, int offy) {
        GGeoString geoString = new GGeoString(t, offx, offy);
        GImage gImage = this.calc.fromGeoStringToGImage(geoString);
        int zoom = this.calc.fromTxtZoomToCoordZoom(geoString.getZoom());
        return getMainMapImagesList(gImage.getImgX(), gImage.getImgY(), zoom);
    }

    /**
     * Get sattelites images list fro given map coordinates. 
     * 
     * @param x
     * @param y
     * @param zoom
     * @param area
     * @return
     */
    public String getPhotoImagesListFromCoord(int x, int y, int zoom) {
        GGeoString gString = this.calc.fromGImageToGeoString(new GImage(x, y, zoom));
        return getMainSatImagesList(gString);
    }

    /**
     * Get main images list for map mode. 
     * 
     * @param x
     * @param y
     * @param zoom
     * @return
     */
    private String getMainMapImagesList(int x, int y, int zoom) {
        return getMapImagesList(x, y, zoom, GoogleCalculator.MAIN_IMAGES);
    }

    /**
     * Get background images list for map mode. 
     * 
     * @param x
     * @param y
     * @param zoom
     * @return
     */
    public String getBgMapImagesList(int x, int y, int zoom) {
        return getMapImagesList(x, y, zoom, GoogleCalculator.BG_IMAGES);
    }

    /**
     * Get main images list for sattelite mode. 
     * @param t
     * @return
     */
    private String getMainSatImagesList(GGeoString t) {
        return getPhotomagesList(GoogleCalculator.MAIN_IMAGES, t);
    }

    public String getBgPhotoImagesList(String t, int offx, int offy) {
        GGeoString geoString = new GGeoString(t, offx, offy);
        return this.getBgPhotoImagesList(geoString);
    }

    /**
     * Get background images list for sattellite mode. 
     *  
     * @param t
     * @return
     */
    public String getBgPhotoImagesList(GGeoString t) {
        return getPhotomagesList(GoogleCalculator.BG_IMAGES, t);
    }
}
