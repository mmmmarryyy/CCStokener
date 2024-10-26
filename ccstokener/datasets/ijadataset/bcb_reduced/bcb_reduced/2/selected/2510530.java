package org.fao.fenix.map.geoserver.io;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import org.apache.log4j.Logger;
import org.fao.fenix.domain.map.geoserver.FeatureType;
import org.fao.fenix.map.geoserver.util.FeatureTypeUtils;

/**
 *
 */
public class GeoserverPublisher {

    private boolean debugEnabled = true;

    private void info(String msg) {
        if (debugEnabled) {
            Logger.getLogger(getClass()).info(msg);
        }
    }

    public GeoserverPublisher() {
    }

    public void publish(FeatureType featureType) {
        System.out.println("GeoserverPublisher::publish('" + featureType.getScopedLayerName() + "') : start");
        String dsName = featureType.getDataStore().getName();
        String putSz = FeatureTypeUtils.getFeatureTypeXML(featureType);
        try {
            URL dssUrl = new URL(featureType.getDataStore().getGeoServer().getBaseUrl() + "/rest/folders/" + dsName + "/layers/" + featureType.getLayerName() + ".xml");
            info("### Putting FT into " + dssUrl.toExternalForm() + " (" + dssUrl + ")");
            info("### Feature Type: " + putSz);
            put(dssUrl, putSz);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean put(URL url, String content) throws IOException, ProtocolException {
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setDoOutput(true);
        con.setDoInput(true);
        con.setRequestMethod("PUT");
        OutputStreamWriter outReq = new OutputStreamWriter(con.getOutputStream());
        outReq.write(content);
        outReq.flush();
        outReq.close();
        System.out.println(con.getResponseCode());
        System.out.println(con.getResponseMessage());
        if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
            InputStreamReader is = new InputStreamReader(con.getInputStream());
            String response = readIs(is);
            is.close();
            System.out.println(response);
            return true;
        } else {
            System.out.println(con.getResponseCode());
            System.out.println(con.getResponseMessage());
            return false;
        }
    }

    private static String readIs(InputStreamReader is) {
        char[] inCh = new char[1024];
        StringBuffer input = new StringBuffer();
        int r;
        try {
            while ((r = is.read(inCh)) > 0) {
                input.append(inCh, 0, r);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return input.toString();
    }

    public void unpublish(FeatureType featureType) {
        info("GeoserverPublisher::unpublish('" + featureType.getScopedLayerName() + "') : TODO");
    }

    public void createClassifiedSLD(FeatureType featureType) {
        System.out.println("GeoserverPublisher::createClassifiedSLD('" + featureType.getScopedLayerName() + "') : start");
        StringBuffer szString = new StringBuffer();
        szString.append("classMethod=quantile");
        szString.append("&property=value");
        szString.append("&classNum=10");
        szString.append("&colorRamp=red");
        try {
            URL dssUrl = new URL(featureType.getDataStore().getGeoServer().getBaseUrl() + "/rest/sldservice/" + featureType.getLayerName() + "/styles/" + featureType.getLayerName());
            info("### Putting FT into " + dssUrl.toExternalForm() + " (" + dssUrl + ")");
            info("### Feature Type: " + szString);
            HttpURLConnection con = (HttpURLConnection) dssUrl.openConnection();
            con.setDoOutput(true);
            con.setDoInput(true);
            con.setRequestMethod("PUT");
            OutputStreamWriter outReq = new OutputStreamWriter(con.getOutputStream());
            outReq.write(szString.substring(0));
            outReq.flush();
            outReq.close();
            if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
                InputStreamReader is = new InputStreamReader(con.getInputStream());
                String response = readIs(is);
                is.close();
            } else {
                System.out.println(con.getResponseCode());
                System.out.println(con.getResponseMessage());
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void createStyle(String getMapURL, String styleName, String sldBody) {
        try {
            final String gsURL = getMapURL.substring(0, getMapURL.lastIndexOf("/"));
            URL dssUrl = new URL(gsURL + "/rest/styles/" + styleName.replaceAll(":", "_"));
            if (put(dssUrl, sldBody)) {
                final String featureTypeName = styleName.substring(0, styleName.lastIndexOf("_"));
                dssUrl = new URL(gsURL + "/rest/sldservice/updateStyle/" + featureTypeName);
                put(dssUrl, "<LayerConfig><Style>" + styleName.replaceAll(":", "_") + "</Style></LayerConfig>");
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
