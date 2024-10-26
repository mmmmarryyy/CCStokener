package com.loribel.commons.google.maps;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import com.loribel.commons.util.STools;

/**
 * Tools to use GoogleMaps API to geocode.
 * 
 * @author Gregory Borelli
 */
public class GB_GeocodingTools {

    private static String googleMapKey = null;

    private static String URL_GOOGLEMAP = "http://maps.google.com/maps/geo?q={1}&output=xml&key={2}";

    /**
     * Call googleMap and returns KML.
     * If googleMapKey==null, throws nullPointerExecption 
     */
    public static String getKml(String a_adresse) throws IOException {
        if (a_adresse == null) {
            return null;
        }
        if (googleMapKey == null) {
            throw new NullPointerException("googleMapKey must not be null");
        }
        String l_adresse = a_adresse.replaceAll(" ", "+");
        l_adresse = STools.replace(l_adresse, "\n", ",");
        l_adresse = STools.replace(l_adresse, "\r", "");
        String l_url = STools.replace(URL_GOOGLEMAP, new String[] { l_adresse, googleMapKey });
        String retour = loadUrlToString(l_url);
        return retour;
    }

    /**
     * TODO Comprendre pourquoi GB_HttpTools.loadUrlToString ne marche pas.
     */
    private static String loadUrlToString(String a_url) throws IOException {
        URL l_url1 = new URL(a_url);
        BufferedReader br = new BufferedReader(new InputStreamReader(l_url1.openStream()));
        String l_content = "";
        String l_ligne = null;
        l_content = br.readLine();
        while ((l_ligne = br.readLine()) != null) {
            l_content += AA.SL + l_ligne;
        }
        return l_content;
    }

    public static void setGoogleMapKey(String a_key) {
        googleMapKey = a_key;
    }
}
