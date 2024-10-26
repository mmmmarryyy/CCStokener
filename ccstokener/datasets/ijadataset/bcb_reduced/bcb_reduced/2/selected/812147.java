package prajna.geo;

import java.io.*;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import prajna.data.GeoCoord;
import prajna.data.Location;

/**
 * Geographic entity location class using the geographic locator at <a
 * href="http://ws.geonames.org">GeoNames</a>. Alternately, the locator can
 * read a local file, avoiding the connectivity problems that geonames.org has
 * by using a single local file. When reading a single file, it generally only
 * works with a single location type. It uses an internal cache to improve
 * performance.
 * 
 * @author <a href="http://www.ganae.com/edswing">Edward Swing</a>
 */
public class GeoNamesLocator extends GeoLocator {

    private static DocumentBuilder docBuilder;

    private String geoPath = "http://ws.geonames.org";

    private boolean estimate = false;

    private boolean retry = false;

    private boolean loaded = false;

    private static HashMap<String, GeoObject> cache = new HashMap<String, GeoObject>();

    private String featClass;

    private String featClassStr;

    static {
        try {
            docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException exc) {
            exc.printStackTrace();
        }
    }

    /**
     * Create the GeoNamesLocator
     */
    public GeoNamesLocator() {
        setFeatureClass("AHP");
    }

    /**
     * Create the GeoNamesLocator with the specified path
     * 
     * @param path the path to the GeoNames file or URL
     */
    public GeoNamesLocator(String path) {
        setFeatureClass("AHP");
        geoPath = path;
    }

    /**
     * Request the geographic coordinate from the GeoNames Map server.
     * 
     * @param locName Location name.
     * @return the coordinate for the given location
     */
    @Override
    public GeoCoord getCoordinate(String locName) {
        GeoObject geoObj = getGeoObject(locName);
        return geoObj == null ? null : geoObj.getCenter();
    }

    /**
     * Get the country containing the named location.
     * 
     * @param locName the location name
     * @return the country name containing the location.
     */
    public String getCountry(String locName) {
        GeoObject geoObj = getGeoObject(locName);
        return geoObj == null ? null : geoObj.getCountry();
    }

    /**
     * Get the feature type for the location name.
     * 
     * @param locName the location name
     * @return the GeoFeatureType for the geographic feature, or null if no
     *         location matches the specified name
     */
    @Override
    public GeoFeatureType getFeatureType(String locName) {
        GeoObject geoObj = getGeoObject(locName);
        return geoObj == null ? null : geoObj.getType();
    }

    private GeoObject getGeoFromFile(String locName) {
        GeoObject geoObj = null;
        try {
            TreeSet<GeoObject> recs = new TreeSet<GeoObject>();
            BufferedReader reader = new BufferedReader(new FileReader(geoPath));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] blocks = line.split("\\t");
                if (featClass.contains(blocks[6]) && (blocks[1].equalsIgnoreCase(locName) || blocks[2].equalsIgnoreCase(locName))) {
                    GeoObject obj = new GeoObject(blocks[1]);
                    double lat = Double.parseDouble(blocks[4]);
                    double lon = Double.parseDouble(blocks[5]);
                    obj.setCenter(new GeoCoord(lat, lon));
                    String[] alts = blocks[3].split(",");
                    for (String alt : alts) {
                        obj.addAltName(alt.trim());
                    }
                    obj.setFeatureType(getTypeForCode(blocks[7]));
                    obj.setCountry(CountryCoder.getCountry(blocks[8]));
                    if (blocks[14].length() > 0) {
                        obj.setPopulation(Long.parseLong(blocks[14]));
                    }
                    recs.add(obj);
                }
            }
            if (recs.size() > 0) {
                geoObj = recs.iterator().next();
            }
            reader.close();
        } catch (IOException exc) {
            exc.printStackTrace();
        }
        return geoObj;
    }

    /**
     * Get the geographic information from a web service. This method expects
     * the service to match the GeoNames API.
     * 
     * @param locName the location name
     * @return the geographic object
     */
    private GeoObject getGeoFromUrl(String locName) {
        GeoObject geoObj = null;
        String urlString = geoPath + "/search?maxRows=1&isNameRequired=true&q=" + locName.trim().replaceAll("\\s+", "+") + featClassStr;
        try {
            InputStream inStream = new URL(urlString).openStream();
            Document doc = docBuilder.parse(inStream);
            NodeList totalNote = doc.getElementsByTagName("totalResultsCount");
            int cnt = 0;
            while (totalNote.getLength() == 0 && retry && cnt < 10) {
                Thread.sleep(1000);
                totalNote = doc.getElementsByTagName("totalResultsCount");
                cnt++;
            }
            if (totalNote.getLength() > 0) {
                String totalStr = totalNote.item(0).getTextContent();
                int totalCnt = Integer.parseInt(totalStr);
                NodeList geoNodes = doc.getElementsByTagName("geoname");
                if (geoNodes != null && geoNodes.getLength() > 0) {
                    Element geoElem = (Element) geoNodes.item(0);
                    int pop = 0;
                    for (int i = 0; i < geoNodes.getLength(); i++) {
                        Element tstElem = (Element) geoNodes.item(i);
                        Element nameElem = (Element) tstElem.getElementsByTagName("name").item(0);
                        String nameStr = nameElem.getTextContent().trim();
                        Element popElem = (Element) tstElem.getElementsByTagName("population").item(0);
                        String popStr = popElem.getTextContent().trim();
                        if (popStr != null && popStr.length() > 0 && nameStr.contains(locName)) {
                            int tstPop = Integer.parseInt(popStr);
                            if (tstPop > pop) {
                                geoElem = tstElem;
                                pop = tstPop;
                            }
                        }
                    }
                    Element nameElem = (Element) geoElem.getElementsByTagName("name").item(0);
                    Element typeElem = (Element) geoElem.getElementsByTagName("fcode").item(0);
                    Element latElem = (Element) geoElem.getElementsByTagName("lat").item(0);
                    Element lonElem = (Element) geoElem.getElementsByTagName("lng").item(0);
                    Element cntryElem = (Element) geoElem.getElementsByTagName("countryName").item(0);
                    String nameStr = nameElem.getTextContent().trim();
                    String typeStr = typeElem.getTextContent().trim();
                    GeoFeatureType type = getTypeForCode(typeStr);
                    if (totalCnt < 200 || type.equals(GeoFeatureType.COUNTRY) || type.equals(GeoFeatureType.OTHER)) {
                        String latStr = latElem.getTextContent().trim();
                        String lonStr = lonElem.getTextContent().trim();
                        String cntryStr = cntryElem.getTextContent().trim();
                        double lat = Double.parseDouble(latStr);
                        double lon = Double.parseDouble(lonStr);
                        geoObj = new GeoObject(nameStr);
                        geoObj.setCenter(new GeoCoord(lat, lon));
                        geoObj.addAltName(locName);
                        geoObj.addAltName(nameStr);
                        geoObj.setFeatureType(type);
                        if (cntryStr != null) {
                            geoObj.setCountry(cntryStr);
                            if (type.equals(GeoFeatureType.COUNTRY)) {
                                geoObj.addAltName(cntryStr);
                            }
                        }
                    }
                }
            }
            inStream.close();
        } catch (Exception exc) {
            System.err.println("GeoNamesLocator: Error when retrieving " + urlString + ": Service not available.");
        }
        return geoObj;
    }

    /**
     * Get the geographic object for the given location name. This method first
     * checks the cache for the location name. If it is not found, the location
     * name is looked up using the Geonames web service
     * 
     * @param locName the location name
     * @return the geographic object representing the information about this
     *         location name
     */
    public GeoObject getGeoObject(String locName) {
        GeoObject geoObj = cache.get(locName);
        if (geoObj == null && !cache.containsKey(locName) && locName.length() > 2) {
            if (geoPath.startsWith("http:")) {
                geoObj = getGeoFromUrl(locName);
            } else if (!loaded) {
                geoObj = getGeoFromFile(locName);
            }
            if (geoObj != null) {
                cache.put(locName, geoObj);
                cache.put(geoObj.getName(), geoObj);
                for (String alt : geoObj.getAltNames()) {
                    cache.put(alt, geoObj);
                }
            }
        }
        return geoObj;
    }

    /**
     * Get the geonames path. This will either return the file being read by
     * this locator, or the URL that it is reading from.
     * 
     * @return the geonames path
     */
    public String getGeoPath() {
        return geoPath;
    }

    /**
     * Get the real name known by GeoNames for the location name.
     * 
     * @param locName the location name
     * @return the name GeoNames uses for the specified name, or null if no
     *         location matches the specified name
     */
    public String getRealName(String locName) {
        GeoObject geoObj = getGeoObject(locName);
        return geoObj == null ? null : geoObj.getName();
    }

    /**
     * Get the feature type for a particular feature code. The GeoNames feature
     * codes are detailed at http://www.geonames.org/export/codes.html
     * 
     * @param code the GeoNames feature code
     * @return a GeoFeatureType
     */
    private GeoFeatureType getTypeForCode(String code) {
        GeoFeatureType type = GeoFeatureType.OTHER;
        if (code.startsWith("PCL")) {
            type = GeoFeatureType.COUNTRY;
        } else if (code.startsWith("ADM")) {
            type = GeoFeatureType.PROVINCE;
        } else if (code.startsWith("PPL")) {
            type = GeoFeatureType.CITY;
        } else if (code.equals("CONT")) {
            type = GeoFeatureType.CONTINENT;
        } else if (code.equals("OCN")) {
            type = GeoFeatureType.OCEAN;
        } else if (code.equals("SEA")) {
            type = GeoFeatureType.SEA;
        } else if (code.startsWith("STM")) {
            type = GeoFeatureType.RIVER;
        } else if (code.startsWith("LK")) {
            type = GeoFeatureType.LAKE;
        }
        return type;
    }

    /**
     * Return whether the locator allows estimation
     * 
     * @return whether the locator allows estimation
     */
    @Override
    public boolean isEstimateFlag() {
        return estimate;
    }

    /**
     * Return whether the locator allows retrying if the servers are busy
     * 
     * @return whether the locator allows retrying
     */
    public boolean isRetryFlag() {
        return retry;
    }

    /**
     * Loads the entire file into the cache. This has no effect if the GeoPath
     * points to a URL.
     */
    public void loadCache() {
        if (!geoPath.startsWith("http:")) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(geoPath));
                String line;
                int cnt = 0;
                while ((line = reader.readLine()) != null) {
                    String[] blocks = line.split("\\t");
                    if (featClass.contains(blocks[6])) {
                        GeoObject obj = new GeoObject(blocks[1]);
                        double lat = Double.parseDouble(blocks[4]);
                        double lon = Double.parseDouble(blocks[5]);
                        obj.setCenter(new GeoCoord(lat, lon));
                        String[] alts = blocks[3].split(",");
                        for (String alt : alts) {
                            obj.addAltName(alt.trim());
                        }
                        obj.setFeatureType(getTypeForCode(blocks[7]));
                        obj.setCountry(CountryCoder.getCountry(blocks[8]));
                        if (blocks[14].length() > 0) {
                            obj.setPopulation(Long.parseLong(blocks[14]));
                        }
                        GeoObject other = cache.get(obj.getName());
                        if (other == null || obj.compareTo(other) < 0) {
                            cache.put(obj.getName(), obj);
                        }
                        for (String alt : obj.getAltNames()) {
                            other = cache.get(alt);
                            if (other == null || obj.compareTo(other) < 0) {
                                cache.put(alt, obj);
                            }
                        }
                        cnt++;
                    }
                }
                reader.close();
                loaded = true;
            } catch (IOException exc) {
                System.err.println("GeoNamesLocator: Error when retrieving " + geoPath + ": Service not available.");
            }
        }
    }

    /**
     * Look up the shape associated with the specified Location
     * 
     * @param loc the specified location object
     * @return The geographic shape representing the specified location
     */
    @Override
    public GeoShape lookup(Location loc) {
        GeoShape shape = null;
        if (loc.getCenter() == null && loc.getName().length() > 0) {
            shape = lookup(loc.getName());
        } else {
            shape = new GeoMarker(loc.getCenter());
        }
        return shape;
    }

    /**
     * Look up the shape associated with the specified Location by name
     * 
     * @param locName the specified location name
     * @return The geographic shape representing the specified location
     */
    @Override
    public GeoShape lookup(String locName) {
        GeoObject geoObj = getGeoObject(locName);
        return geoObj == null ? null : new GeoMarker(geoObj.getCenter());
    }

    /**
     * Look up the shape associated with the specified Location by name and
     * type
     * 
     * @param locName the specified location name
     * @param type the type of location the name represents
     * @return The geographic shape representing the specified location
     */
    @Override
    public GeoShape lookup(String locName, String type) {
        return lookup(locName);
    }

    /**
     * Set the flag indicating whether to allow estimation within the locator.
     * 
     * @param estimateFlag whether the locator allows estimation
     */
    @Override
    public void setEstimateFlag(boolean estimateFlag) {
        estimate = estimateFlag;
    }

    /**
     * Set the feature classes this locator will use. The argument should be
     * composed of letters indicating which feature classes this locator will
     * use. Each code is a single letter, and the codes are concatenated
     * together. Letters correspond to those described in the geonames web
     * service, at http://www.geonames.org/export/codes.html. By default, the
     * feature classes are AHP, indicating Countries and Regions (A), Bodies of
     * Water (H), and Population Centers (P).
     * 
     * @param featureClasses A string containing single letter codes.
     */
    public void setFeatureClass(String featureClasses) {
        if (featureClasses == null) {
            featClassStr = "";
        } else {
            featClass = featureClasses.toUpperCase();
            StringBuffer featBuf = new StringBuffer();
            for (int i = 0; i < featureClasses.length(); i++) {
                char feat = featClass.charAt(i);
                if (Character.isLetter(feat)) {
                    featBuf.append("&featureClass=" + feat);
                }
            }
            featClassStr = featBuf.toString();
        }
    }

    /**
     * Set the base GeoNames path. If not set, the default is
     * http://ws.geonames.org. If the path is set to a local file, then the
     * data is read from the local file instead of checking the URL.
     * 
     * @param path the new base for the geonames URL, or the local geoFile.
     */
    public void setGeoPath(String path) {
        geoPath = path;
    }

    /**
     * Set the flag indicating whether to allow retrying when the servers are
     * too busy.
     * 
     * @param retryFlag whether the locator allows retrying
     */
    public void setRetryFlag(boolean retryFlag) {
        retry = retryFlag;
    }

    /**
     * Get the cache. This method returns the current cache. This method is
     * available for developers who want to subclass this locator.
     * 
     * @return the cache
     */
    protected static Map<String, GeoObject> getCache() {
        return cache;
    }
}
