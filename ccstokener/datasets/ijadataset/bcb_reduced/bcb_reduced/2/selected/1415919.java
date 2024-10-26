package com.enjoyireland.hiking;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.osmdroid.DefaultResourceProxyImpl;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import uk.me.jstott.jcoord.LatLng;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.location.Location;

/**
 * @author Rónan Mac an tSaoir
 */
public class MapUtils extends DefaultResourceProxyImpl {

    private static Context mContext = null;

    public MapUtils(final Context pContext) {
        super(pContext);
        mContext = pContext;
    }

    @Override
    public String getString(final string resID) {
        try {
            final int res = R.string.class.getDeclaredField(resID.name()).getInt(null);
            return mContext.getString(res);
        } catch (final Exception e) {
            return super.getString(resID);
        }
    }

    @Override
    public Bitmap getBitmap(final bitmap resID) {
        try {
            final int res = R.drawable.class.getDeclaredField(resID.name()).getInt(null);
            return BitmapFactory.decodeResource(mContext.getResources(), res);
        } catch (final Exception e) {
            return super.getBitmap(resID);
        }
    }

    @Override
    public Drawable getDrawable(final bitmap resID) {
        try {
            final int res = R.drawable.class.getDeclaredField(resID.name()).getInt(null);
            return mContext.getResources().getDrawable(res);
        } catch (final Exception e) {
            return super.getDrawable(resID);
        }
    }

    /**
	 * Utility method to download GPX file from specified URL
	 * 
	 * @param gpxURL
	 * 		GPX URL to download
	 */
    public static void getGPX(String gpxURL, String fName) {
        try {
            URL url = new URL(gpxURL);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setDoOutput(true);
            urlConnection.connect();
            File storage = mContext.getExternalFilesDir(null);
            File file = new File(storage, fName);
            FileOutputStream os = new FileOutputStream(file);
            InputStream is = urlConnection.getInputStream();
            byte[] buffer = new byte[1024];
            int bufferLength = 0;
            while ((bufferLength = is.read(buffer)) > 0) {
                os.write(buffer, 0, bufferLength);
            }
            os.close();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Utility method to parse Lat/Lng co-ordinates from GPX file
	 * 
	 * @param gpxFile
	 * 		GPX file (xml structure for GPS co-ordinates)
	 * 
	 */
    public static List<Location> getGPXPoints(InputStream gpxFile) {
        List<Location> points = null;
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = dbFactory.newDocumentBuilder();
            InputStream is = gpxFile;
            Document dom = builder.parse(is);
            Element root = dom.getDocumentElement();
            NodeList nodes = root.getElementsByTagName("trkpt");
            points = new ArrayList<Location>();
            for (int i = 0; i < nodes.getLength(); i++) {
                Node item = nodes.item(i);
                NamedNodeMap attrs = item.getAttributes();
                NodeList props = item.getChildNodes();
                Location pt = new Location("GPXParser");
                pt.setLatitude(Double.parseDouble(attrs.getNamedItem("lat").getTextContent()));
                pt.setLongitude(Double.parseDouble(attrs.getNamedItem("lon").getTextContent()));
                for (int j = 0; j < props.getLength(); j++) {
                    Node item2 = props.item(j);
                    String name = item2.getNodeName();
                    if (!name.equalsIgnoreCase("time")) continue;
                    try {
                        pt.setTime((new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").parse(item2.getFirstChild().getNodeValue())).getTime());
                    } catch (ParseException ex) {
                        ex.printStackTrace();
                    }
                }
                for (int k = 0; k < props.getLength(); k++) {
                    Node item3 = props.item(k);
                    String name = item3.getNodeName();
                    if (!name.equalsIgnoreCase("ele")) continue;
                    pt.setAltitude(Double.parseDouble(item3.getFirstChild().getNodeValue()));
                }
                points.add(pt);
            }
            is.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }
        return points;
    }

    /**
	 * Utility method to calculate distance between two LatLng sets
	 *  Details: http://www.movable-type.co.uk/scripts/latlong.html
	 * 	
	 *  Note: LatLng is converted Irish Grid Reference, using jstott's jcoord library
	 * 
	 */
    public static double getDistance(LatLng ll1, LatLng ll2) {
        double lat1 = ll1.getLatitude();
        double lat2 = ll2.getLatitude();
        double lng1 = ll1.getLongitude();
        double lng2 = ll2.getLongitude();
        double radius = 6371;
        double deltaLat = Math.toRadians(lat2 - lat1);
        double deltaLng = Math.toRadians(lng2 - lng1);
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);
        double aVal = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) + Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2) * Math.cos(lat1) * Math.cos(lat2);
        double cVal = 2 * Math.atan2(Math.sqrt(aVal), Math.sqrt(1 - aVal));
        double distance = radius * cVal;
        return distance;
    }
}
